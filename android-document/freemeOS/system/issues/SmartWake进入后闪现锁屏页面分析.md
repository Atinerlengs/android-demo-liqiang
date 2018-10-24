### 现象

- 进行任意Smart wake动作，唤醒后先闪锁屏界面，再出现Smart Wake动画

### 原因分析

#### 1.1 Log分析

- Exception

```
- 04-10 17:27:39.659  1205  1303 I WindowManager: All windows ready for display!
- 04-10 17:27:39.753  1205  2176 V WindowManager: performShowLocked: mDrawState=HAS_DRAWN in Window{f46fa3 u0 SmartWake}
- 04-10 17:27:39.773  1205  1303 D WindowManager: finishScreenTurningOn: mAwake=true, mScreenOnEarly=true, mScreenOnFully=false, mKeyguardDrawComplete=true, mWindowManagerDrawComplete=true
- 04-10 17:27:39.780  1205  1376 D LocalDisplayAdapter: setDisplayBrightness(id=0, brightness=255)
- 04-10 17:27:39.863  1205  1309 I WindowManager:   SURFACE SHOW (performLayout): SmartWake
```

- Normal

```
- 04-10 17:13:19.282  1205  1303 I WindowManager: All windows ready for display!
- 04-10 17:13:19.373  1205  2061 V WindowManager: performShowLocked: mDrawState=HAS_DRAWN in Window{9498090 u0 SmartWake}
- 04-10 17:13:19.396  1205  1309 I WindowManager:   SURFACE SHOW (performLayout): SmartWake
- 04-10 17:13:19.438  1205  1303 D WindowManager: finishScreenTurningOn: mAwake=true, mScreenOnEarly=true, mScreenOnFully=false, mKeyguardDrawComplete=true, mWindowManagerDrawComplete=true
- 04-10 17:13:19.443  1205  1376 D LocalDisplayAdapter: setDisplayBrightness(id=0, brightness=255)
```

- 很明显，setDisplayBrightness为亮屏动作，如果发生在SURFACE SHOW之前，则会出现异常，而若发生在SURFACE SHOW之后，则正常

- 为什么会产生这种时序问题？继续往下看

#### 1.2 依赖关系

- 从LOG中我们发现，这几个动作分别发生在不同的线程，如果没有依赖关系，则时序将是混乱的
- 那么是怎么依赖的呢？请看下面的执行序

DisplayPowerController.java

```
private final class DisplayControllerHandler extends Handler {
    ……
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_POWER_STATE:
                updatePowerState();
                break;
                ……
        }
    }
}

private void updatePowerState() {
    ……
    animateScreenStateChange(state, performScreenOffTransition);
    ……
}

private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
    if (!setScreenState(Display.STATE_ON)) {
            return; // screen on blocked
        }
    }
}

private boolean setScreenState(int state) {
    mWindowManagerPolicy.screenTurningOn(mPendingScreenOnUnblocker);
}
```

PhoneWindowManager.java

```
// Called on the DisplayManager's DisplayPowerController thread.
@Override
public void screenTurningOn(final ScreenOnListener screenOnListener) {
    ……
    synchronized (mLock) {
        ……
        if (mKeyguardDelegate != null) {
            mHandler.removeMessages(MSG_KEYGUARD_DRAWN_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MSG_KEYGUARD_DRAWN_TIMEOUT, 1000);
            mKeyguardDelegate.onScreenTurningOn(mKeyguardDrawnCallback);
        } else {
            finishKeyguardDrawn();
        }
    }
}

final DrawnListener mKeyguardDrawnCallback = new DrawnListener() {
    @Override
    public void onDrawn() {
        mHandler.sendEmptyMessage(MSG_KEYGUARD_DRAWN_COMPLETE);
    }
};

case MSG_KEYGUARD_DRAWN_COMPLETE:
    finishKeyguardDrawn();
    break;

private void finishKeyguardDrawn() {
    ……
    // ... eventually calls finishWindowsDrawn which will finalize our screen turn on
    // as well as enabling the orientation change logic/sensor.
    mWindowManagerInternal.waitForAllWindowsDrawn(mWindowManagerDrawCallback,
            WAITING_FOR_DRAWN_TIMEOUT);
}

final Runnable mWindowManagerDrawCallback = new Runnable() {
    @Override
    public void run() {
        if (DEBUG_WAKEUP) Slog.i(TAG, "All windows ready for display!");
        mHandler.sendEmptyMessage(MSG_WINDOW_MANAGER_DRAWN_COMPLETE);
    }
};

case MSG_WINDOW_MANAGER_DRAWN_COMPLETE:
    if (DEBUG_WAKEUP) Slog.w(TAG, "Setting mWindowManagerDrawComplete");
    finishWindowsDrawn();
    break;

private void finishWindowsDrawn() {
    ……

    finishScreenTurningOn();
}

private void finishScreenTurningOn() {
    ……

    if (listener != null) {
        listener.onScreenOn();
    }
    ……
}
```

通过代码，我们发现，DisplayPowerController收到唤醒消息之后，会先交由PhoneWindowManager进行策略处理，PhoneWindowManager收到亮屏消息之后，先分发给锁屏，等待锁屏处理完毕后，通过waitForAllWindowsDrawn进行等待，直到所有窗口画完，才会调用onScreenOn进行点亮

也就是说，会有两个等待

- 等待锁屏
- 等待所有窗口画完

如何等待所有窗口画完？

WindowManagerService.java

```
@Override
public void waitForAllWindowsDrawn(Runnable callback, long timeout) {
    boolean allWindowsDrawn = false;
    synchronized (mWindowMap) {
        mWaitingForDrawnCallback = callback;
        final WindowList windows = getDefaultWindowListLocked();
        for (int winNdx = windows.size() - 1; winNdx >= 0; --winNdx) {
            final WindowState win = windows.get(winNdx);
            final boolean isForceHiding = mPolicy.isForceHiding(win.mAttrs);
            if (win.isVisibleLw()
                    && (win.mAppToken != null || isForceHiding)) {
                win.mWinAnimator.mDrawState = DRAW_PENDING;
                // Force add to mResizingWindows.
                win.mLastContentInsets.set(-1, -1, -1, -1);
                mWaitingForDrawn.add(win);

                // No need to wait for the windows below Keyguard.
                if (isForceHiding) {
                    break;
                }
            }
        }
        mWindowPlacerLocked.requestTraversal();
        mH.removeMessages(H.WAITING_FOR_DRAWN_TIMEOUT);
        if (mWaitingForDrawn.isEmpty()) {
            allWindowsDrawn = true;
        } else {
            mH.sendEmptyMessageDelayed(H.WAITING_FOR_DRAWN_TIMEOUT, timeout);
            checkDrawnWindowsLocked();
        }
    }
    if (allWindowsDrawn) {
        callback.run();
    }
}
```

从如上代码可以看出，如果要加入等待列表，需满足如下条件

==if (win.isVisibleLw() && (win.mAppToken != null || isForceHiding))==

很不幸，SmartWake的窗口是Free Window，并没有APP Token，而且也不属于ForceHiding的范畴，所以永远都不会加入到等待列表中，换句话说，系统并不关心你画完或者不画完

PhoneWindowManager.java

```
@Override
public boolean isForceHiding(WindowManager.LayoutParams attrs) {
    return (attrs.privateFlags & PRIVATE_FLAG_KEYGUARD) != 0 ||
    /// M: [ALPS01939364][ALPS01948669] Fix app window is hidden even when Keyguard is occluded
        (isKeyguardHostWindow(attrs) && isKeyguardShowingAndNotOccluded()) ||
        (attrs.type == TYPE_KEYGUARD_SCRIM);
}
```

这也就是此问题的原因之一，亮屏对SmartWake是否画完并没有任何依赖

#### 1.3 下面讲另外一个原因，SURFACE SHOW

从1.1的Log我们发现，SURFACE SHOW位于亮屏之前，则不闪屏，反之，就闪屏了，即SURFACE SHOW也是一个关键因素

根据1.2，我们看到，当前系统设计的思路上，只关心了窗口是否画完，并没有关心窗口是否Show出来，抽象点说，只关心Surface有没有画好，并不管SURFACE有没有抬起来

那么，SURFACE SHOW发生在何时？

WindowAnimator.java

```
WindowAnimator(final WindowManagerService service) {
    mService = service;
    mContext = service.mContext;
    mPolicy = service.mPolicy;
    mWindowPlacerLocked = service.mWindowPlacerLocked;

    mAnimationFrameCallback = new Choreographer.FrameCallback() {
        public void doFrame(long frameTimeNs) {
            synchronized (mService.mWindowMap) {
                mService.mAnimationScheduled = false;
                /// M: add systrace
                Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "wmAnimate");
                animateLocked(frameTimeNs);
                Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
            }
        }
    };
}

/** Locked on mService.mWindowMap. */
private void animateLocked(long frameTimeNs) {
    ……
    final WindowList windows = mService.getWindowListLocked(displayId);
    final int N = windows.size();
    for (int j = 0; j < N; j++) {
        windows.get(j).mWinAnimator.prepareSurfaceLocked(true);
    }
    ……
}
```

WindowStateAnimator.java

```
void prepareSurfaceLocked(final boolean recoveringMemory) {
    ……
    if (prepared && mLastHidden && mDrawState == HAS_DRAWN) {
        if (showSurfaceRobustlyLocked()) {
            ……
        } else {
            w.mOrientationChanging = false;
        }
    }
    ……
}

private boolean showSurfaceRobustlyLocked() {
        ……
        boolean shown = mSurfaceController.showRobustlyInTransaction();
        if (!shown)
            return false;
        ……
    }

```

WindowSurfaceController.java

```
boolean showRobustlyInTransaction() {
    if (SHOW_TRANSACTIONS) logSurface(
            "SHOW (performLayout)", null);
    ……
    return updateVisibility();
}

private boolean updateVisibility() {
    if (mHiddenForCrop || mHiddenForOtherReasons) {
        if (mSurfaceShown) {
            hideSurface();
        }
        return false;
    } else {
        if (!mSurfaceShown) {
            return showSurface();
        } else {
            return true;
        }
    }
}
private boolean showSurface() {
    try {
        mSurfaceShown = true;
        mSurfaceControl.show();
        return true;
    } catch (RuntimeException e) {
        Slog.w(TAG, "Failure showing surface " + mSurfaceControl + " in " + this, e);
    }
    ……
}
```

动画系统比较复杂，代码省略了几万字，从上述代码中，只说明一个问题，动画线程开始走，并最终会掉SURFACEControl把这层SURFACE SHOW出来

最后看下动画开始的时间点

WindowSurfacePlacer.java

```
// "Something has changed!  Let's make it correct now."
private void performSurfacePlacementInner(boolean recoveringMemory) {
    …
    mService.scheduleAnimationLocked();
    ……
}
```

WindowManagerService.java

```
/** Note that Locked in this case is on mLayoutToAnim */
void scheduleAnimationLocked() {
    if (!mAnimationScheduled) {
        mAnimationScheduled = true;
        mChoreographer.postFrameCallback(mAnimator.mAnimationFrameCallback);
    }
}
```

艾玛，画完之后，抛了个消息就跑了，那动画在什么时候发生，怎么保证呢？

并没有

SURFACE SHOW什么时候完成，并没有办法确定，这也就是原因之二

### 解决方式

#### 1.1 使亮屏依赖Smart wake绘制完成

```
--- a/base/services/core/java/com/android/server/policy/PhoneWindowManager.java
+++ b/base/services/core/java/com/android/server/policy/PhoneWindowManager.java
@@ -2676,6 +2676,10 @@ public class PhoneWindowManager implements WindowManagerPolicy {
         return (attrs.privateFlags & PRIVATE_FLAG_KEYGUARD) != 0 ||
         /// M: [ALPS01939364][ALPS01948669] Fix app window is hidden even when Keyguard is occluded
             (isKeyguardHostWindow(attrs) && isKeyguardShowingAndNotOccluded()) ||
+            //*/ freeme.zhiwei.zhang, 20170407. SmartWake.
+            // bugfix: flash keyguard before smartwake window show.
+            (FreemeOption.FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT && attrs.getTitle().equals("SmartWake")) ||
+            //*/
             (attrs.type == TYPE_KEYGUARD_SCRIM);
     }
```

#### 1.2 使亮屏依赖Smart wake被抬起来

```
diff --git a/base/services/core/java/com/android/server/wm/WindowManagerService.java b/base/services/core/java/com/android/server/wm/WindowManagerService.java
index d3b7707..ae9d7a0 100644
--- a/base/services/core/java/com/android/server/wm/WindowManagerService.java
+++ b/base/services/core/java/com/android/server/wm/WindowManagerService.java
@@ -9832,6 +9832,15 @@ public class WindowManagerService extends IWindowManager.Stub
             } else if (win.hasDrawnLw()) {
                 // Window is now drawn (and shown).
                 if (DEBUG_SCREEN_ON) Slog.d(TAG_WM, "Window drawn win=" + win);
+                //*/ freeme.zhiwei.zhang, 20170407. SmartWake.
+                // bugfix: flash keyguard before smartwake window show.
+                if (com.freeme.util.FreemeOption.FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT &&
+                        win.mAttrs.getTitle().equals("SmartWake")) {
+                    if (win.mWinAnimator.getShown()) {
+                        mWaitingForDrawn.remove(win);
+                    }
+                } else
+                //*/
                 mWaitingForDrawn.remove(win);
             }
         }
```

#### 1.3 重新审视Smart Wake实现

目前Freeme Smart Wake的频繁Add和Remove Window的行为在未发现其他问题时，暂时不做变动

#### 1.4 提交
Change-Id: I27afd4110357655a83ca40f4974696e53fc0cf7e

### 总结

- 该问题牵涉到PMS, WMS和AMS的内容，添加基于锁屏之上，且显示优先于锁屏的窗口，需注意亮屏逻辑控制

