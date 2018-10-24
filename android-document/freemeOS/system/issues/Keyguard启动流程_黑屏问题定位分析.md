[TOC]

# 概述

本文主要搞清楚KeyguardScrim的启动、显示、销毁。

# PhoneWindowManager.systemReady

```
public void systemReady() {
    //创建 KeyguardServiceDelegate
    mKeyguardDelegate = new KeyguardServiceDelegate(mContext,
            this::onKeyguardShowingStateChanged);
    mKeyguardDelegate.onSystemReady();

    readCameraLensCoverState();
    updateUiMode();
    boolean bindKeyguardNow;

    //注意，这段代码使用mLock同步
    synchronized (mLock) {
        updateOrientationListenerLp();
        mSystemReady = true;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateSettings();
            }
        });

        // 首次执行时，mDeferBindKeyguard为false
        bindKeyguardNow = mDeferBindKeyguard;
        if (bindKeyguardNow) {
            // systemBooted ran but wasn't able to bind to the Keyguard, we'll do it now.
            mDeferBindKeyguard = false;
        }
    }

    //首次执行时，条件不满足，不执行bindService
    if (bindKeyguardNow) {
        mKeyguardDelegate.bindService(mContext);
        mKeyguardDelegate.onBootCompleted();
    }
    mSystemGestures.systemReady();
    mImmersiveModeConfirmation.systemReady();
}
```

因此上面代码逻辑仅执行：

1. `KeyguardServiceDelegate` 对象创建
2. `mKeyguardDelegate.onSystemReady()`

## KeyguardServiceDelegate构造

```
public KeyguardServiceDelegate(Context context,
        OnShowingStateChangedCallback showingStateChangedCallback) {
    mContext = context;
    mScrimHandler = UiThread.getHandler();
    mShowingStateChangedCallback = showingStateChangedCallback;
    mScrim = createScrim(context, mScrimHandler);
}
```

### KeyguardServiceDelegate.createScrim

```
    private static View createScrim(Context context, Handler handler) {
        final View view = new View(context);

        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
                ;

        final int stretch = ViewGroup.LayoutParams.MATCH_PARENT;
        final int type = WindowManager.LayoutParams.TYPE_KEYGUARD_SCRIM;
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                stretch, stretch, type, flags, PixelFormat.TRANSLUCENT);
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        lp.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_FAKE_HARDWARE_ACCELERATED;
        // 这里设置窗口名
        lp.setTitle("KeyguardScrim");
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        // Disable pretty much everything in statusbar until keyguard comes back and we know
        // the state of the world.
        view.setSystemUiVisibility(View.STATUS_BAR_DISABLE_HOME
                | View.STATUS_BAR_DISABLE_BACK
                | View.STATUS_BAR_DISABLE_RECENT
                | View.STATUS_BAR_DISABLE_EXPAND
                | View.STATUS_BAR_DISABLE_SEARCH);
        handler.post(new Runnable() {
            @Override
            public void run() {
                wm.addView(view, lp);
            }
        });
        return view;
    }
```

## KeyguardServiceDelegate.onSystemReady

```
public void onSystemReady() {
    if (mKeyguardService != null) {
        mKeyguardService.onSystemReady();
    } else {
        //执行这里
        mKeyguardState.systemIsReady = true;
    }
}
```

此时 `mKeyguardService` 为空。只有执行过bindService之后 `mKeyguardService` 才非空。因此只是简单的设置状态。


# WMS.enableScreenAfterBoot()

```
public void enableScreenAfterBoot() {
    synchronized(mWindowMap) {
        if (DEBUG_BOOT) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            Slog.i(TAG_WM, "enableScreenAfterBoot: mDisplayEnabled=" + mDisplayEnabled
                    + " mForceDisplayEnabled=" + mForceDisplayEnabled
                    + " mShowingBootMessages=" + mShowingBootMessages
                    + " mSystemBooted=" + mSystemBooted, here);
        }
        if (mSystemBooted) {
            return;
        }
        mSystemBooted = true;
        hideBootMessagesLocked();
        // If the screen still doesn't come up after 30 seconds, give
        // up and turn it on.
        mH.sendEmptyMessageDelayed(H.BOOT_TIMEOUT, 30*1000);
    }

    //这里调用PhoneWindowManager.systemBooted
    mPolicy.systemBooted();

    performEnableScreen();
}
```

## PhoneWindowManager.systemBooted

```
@Override
public void systemBooted() {
    boolean bindKeyguardNow = false;
    synchronized (mLock) {
        // Time to bind Keyguard; take care to only bind it once, either here if ready or
        // in systemReady if not.
        if (mKeyguardDelegate != null) {
            // 此时走到这里来
            bindKeyguardNow = true;
        } else {
            // 待分析，如果没有走到这里会怎么样？
            // Because mKeyguardDelegate is null, we know that the synchronized block in
            // systemReady didn't run yet and setting this will actually have an effect.
            mDeferBindKeyguard = true;
        }
    }
    if (bindKeyguardNow) {
        // 走到这里
        mKeyguardDelegate.bindService(mContext);
        mKeyguardDelegate.onBootCompleted();
    }
    synchronized (mLock) {
        mSystemBooted = true;
    }
    startedWakingUp();
    screenTurningOn(null);
    screenTurnedOn();
}
```

### KeyguardServiceDelegate.bindService

```
public void bindService(Context context) {
    Intent intent = new Intent();
    final Resources resources = context.getApplicationContext().getResources();

    // 1）读取frameworks/base/core/res/res/values/config.xml中的字符串
    //  "com.android.systemui/com.android.systemui.keyguard.KeyguardService"
    final ComponentName keyguardComponent = ComponentName.unflattenFromString(
            resources.getString(com.android.internal.R.string.config_keyguardComponent));
    intent.addFlags(Intent.FLAG_DEBUG_TRIAGED_MISSING);
    intent.setComponent(keyguardComponent);

    // 2）这里启动KeyguardService
    if (!context.bindServiceAsUser(intent, mKeyguardConnection,
            Context.BIND_AUTO_CREATE, mScrimHandler, UserHandle.SYSTEM)) {
        Log.v(TAG, "*** Keyguard: can't bind to " + keyguardComponent);
        mKeyguardState.showing = false;
        mKeyguardState.showingAndNotOccluded = false;
        mKeyguardState.secure = false;
        synchronized (mKeyguardState) {
            // TODO: Fix synchronisation model in this class. The other state in this class
            // is at least self-healing but a race condition here can lead to the scrim being
            // stuck on keyguard-less devices.
            mKeyguardState.deviceHasKeyguard = false;
            hideScrim();
        }
    } else {
        // 走到这里
        if (DEBUG) Log.v(TAG, "*** Keyguard started");
    }
}
```

疑问：`bindServiceAsUser`这个方法是同步还是异步？

此时Android系统去SystemUI中启动真正的KeyguardService了。这段后面再讲。

### KeyguardServiceDelegate.onBootCompleted

```
public void onBootCompleted() {
    //此时mKeyguardService还未空，mKeyguardService要等到前面的SystemUI启动之后，前面的connected成功之后
    if (mKeyguardService != null) {
        mKeyguardService.onBootCompleted();
    }
    mKeyguardState.bootCompleted = true;
}
```

## PhoneWindowManager.screenTurningOn(null);

这个函数在下文连接KeyguardService服务时介绍，这里不赘述，简单来说，这个函数会显示`KeyguardScrim`窗口。

## PhoneWindowManager.screenTurnedOn();


# 连接KeyguardService

## 尚未连接(Service启动中)

如果此时SystemUI进程的Keyguard还未启动，而此时又收到了DisplayPowerController的screenTurningOn回调，执行逻辑如下。

### PhoneWindowManager.screenTurningOn 

```
// Called on the DisplayManager's DisplayPowerController thread.
@Override
public void screenTurningOn(final ScreenOnListener screenOnListener) {
    if (DEBUG_WAKEUP) Slog.i(TAG, "Screen turning on...");

    updateScreenOffSleepToken(false);
    synchronized (mLock) {
        mScreenOnEarly = true;
        mScreenOnFully = false;
        mKeyguardDrawComplete = false;
        mWindowManagerDrawComplete = false;
        mScreenOnListener = screenOnListener;
        // 条件满足，进入 if 分支
        if (mKeyguardDelegate != null) {
            mHandler.removeMessages(MSG_KEYGUARD_DRAWN_TIMEOUT);
            // 这里设置了一个1S的绘制keyguard的超时消息
            mHandler.sendEmptyMessageDelayed(MSG_KEYGUARD_DRAWN_TIMEOUT, 1000);
            // 执行
            mKeyguardDelegate.onScreenTurningOn(mKeyguardDrawnCallback);
        } else {
            if (DEBUG_WAKEUP) Slog.d(TAG,
                    "null mKeyguardDelegate: setting mKeyguardDrawComplete.");
            finishKeyguardDrawn();
        }
    }
}
```

TODO：DisplayPowerController的回调流程

#### KeyguardServiceDelegate.onScreenTurningOn

```
public void onScreenTurningOn(final DrawnListener drawnListener) {
    if (mKeyguardService != null) {
        if (DEBUG) Log.v(TAG, "onScreenTurnedOn(showListener = " + drawnListener + ")");
        mKeyguardService.onScreenTurningOn(new KeyguardShowDelegate(drawnListener));
    } else {
        // android M/N的低端机上，由于硬件性能太差，基本100%会走到这里
        // try again when we establish a connection
        Slog.w(TAG, "onScreenTurningOn(): no keyguard service!");
        // This shouldn't happen, but if it does, show the scrim immediately and
        // invoke the listener's callback after the service actually connects.
        mDrawnListenerWhenConnect = drawnListener;

        //显示KeyguardScrim窗口
        showScrim();
    }
    mKeyguardState.screenState = SCREEN_STATE_TURNING_ON;
}
```

### 总结

如果SystemUI中的`KeyguardService`启动太慢，并且由于某些原因执行了上面这段逻辑，那么就会显示`KeyguardScrim`窗口。这个窗口存在的意义就是显示壁纸。

## 连接成功

### mKeyguardConnection.onServiceConnected

KeyguardService启动之后，这个Service会真正connected成功。

```java
private final ServiceConnection mKeyguardConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (DEBUG) Log.v(TAG, "*** Keyguard connected (yay!)");
        到这里说明SystemUI已经真正启动起来了！
        mKeyguardService = new KeyguardServiceWrapper(mContext,
                IKeyguardService.Stub.asInterface(service), mShowingStateChangedCallback);

        //这里肯定是true
        if (mKeyguardState.systemIsReady) {
            // If the system is ready, it means keyguard crashed and restarted.
            mKeyguardService.onSystemReady();
            if (mKeyguardState.currentUser != UserHandle.USER_NULL) {
                // There has been a user switch earlier
                mKeyguardService.setCurrentUser(mKeyguardState.currentUser);
            }
            // This is used to hide the scrim once keyguard displays.
            if (mKeyguardState.interactiveState == INTERACTIVE_STATE_AWAKE) {
                mKeyguardService.onStartedWakingUp();
            }
            if (mKeyguardState.screenState == SCREEN_STATE_ON
                    || mKeyguardState.screenState == SCREEN_STATE_TURNING_ON) {
                mKeyguardService.onScreenTurningOn(
                        new KeyguardShowDelegate(mDrawnListenerWhenConnect));
            }
            if (mKeyguardState.screenState == SCREEN_STATE_ON) {
                mKeyguardService.onScreenTurnedOn();
            }
            mDrawnListenerWhenConnect = null;
        }

        //此时 bootCompleted为真
        if (mKeyguardState.bootCompleted) {
            mKeyguardService.onBootCompleted();
        }
        if (mKeyguardState.occluded) {
            mKeyguardService.setOccluded(mKeyguardState.occluded, false /* animate */);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (DEBUG) Log.v(TAG, "*** Keyguard disconnected (boo!)");
        mKeyguardService = null;
    }
};
```

#### KeyguardServiceWrapper构造


# KeyguardService启动流程

- frameworks/base/packages/SystemUI/src/com/android/systemui/keyguard/KeyguardService.java


# Keyguard一条执行流程分析（黑屏问题）

设置密码/图案锁屏，在锁屏界面点击通话进入紧急联系人界面，点击返回，直接回到锁屏界面。这个过程的执行流程如何?

## WMS.relayoutWindow（略，参见深入理解Android卷III)

## PhoneWindowManager.finishPostLayoutPolicyLw

```
public int finishPostLayoutPolicyLw() {
    ...
      } else {
        mWinDismissingKeyguard = null;
        mSecureDismissingKeyguard = false;
        mKeyguardHidden = false;
        if (setKeyguardOccludedLw(false)) {
            changes |= FINISH_LAYOUT_REDO_LAYOUT
                    | FINISH_LAYOUT_REDO_CONFIG
                    | FINISH_LAYOUT_REDO_WALLPAPER;
        }
    }
}
```

### PhoneWindowManager.setKeyguardOccludedLw

```
private boolean setKeyguardOccludedLw(boolean isOccluded) {
    boolean wasOccluded = mKeyguardOccluded;
    boolean showing = mKeyguardDelegate.isShowing();
    if (wasOccluded && !isOccluded && showing) {
        // 走这里
        mKeyguardOccluded = false;
        mKeyguardDelegate.setOccluded(false, true /* animate */);
        mStatusBar.getAttrs().privateFlags |= PRIVATE_FLAG_KEYGUARD;
        if (!mKeyguardDelegate.hasLockscreenWallpaper()) {
            mStatusBar.getAttrs().flags |= FLAG_SHOW_WALLPAPER;
        }
        Animation anim = AnimationUtils.loadAnimation(mContext,
                com.android.internal.R.anim.wallpaper_open_exit);
        mWindowManagerFuncs.overridePlayingAppAnimationsLw(anim);
        return true;
    } else if (!wasOccluded && isOccluded && showing) {
        mKeyguardOccluded = true;
        mKeyguardDelegate.setOccluded(true, false /* animate */);
        mStatusBar.getAttrs().privateFlags &= ~PRIVATE_FLAG_KEYGUARD;
        mStatusBar.getAttrs().flags &= ~FLAG_SHOW_WALLPAPER;
        return true;
    } else {
        return false;
    }
}
```

#### KeyguardServiceDelegate.setOccluded

```
public void setOccluded(boolean isOccluded, boolean animate) {
    if (mKeyguardService != null) {
        if (DEBUG) Log.v(TAG, "setOccluded(" + isOccluded + ") animate=" + animate);
        mKeyguardService.setOccluded(isOccluded, animate);
    }
    mKeyguardState.occluded = isOccluded;
}
```

##### KeyguardServiceWrapper.setOccluded

```
    @Override // Binder interface
    public void setOccluded(boolean isOccluded, boolean animate) {
        try {
            mService.setOccluded(isOccluded, animate);
        } catch (RemoteException e) {
            Slog.w(TAG , "Remote Exception", e);
        }
    }
```

###### IKeyguardService.aidl->IKeyguardService.setOccluded

```
oneway interface IKeyguardService {
    /**
     * Sets the Keyguard as occluded when a window dismisses the Keyguard with flag
     * FLAG_SHOW_ON_LOCK_SCREEN.
     *
     * @param isOccluded Whether the Keyguard is occluded by another window.
     * @param animate Whether to play an animation for the state change.
     */
    void setOccluded(boolean isOccluded, boolean animate);
...
}
```

注意这个 `oneway` 关键字，表示调用 `IKeyguardService.setOccluded` 时不等函数再BN端执行完毕就立刻返回。这是为了避免系统被keyguard的实现端（SystemUI）阻塞。

## KeyguardService.setOccluded

```
@Override // Binder interface
public void setOccluded(boolean isOccluded, boolean animate) {
    Trace.beginSection("KeyguardService.mBinder#setOccluded");
    checkPermission();
    mKeyguardViewMediator.setOccluded(isOccluded, animate);
    Trace.endSection();
}
```

### KeyguardViewMediator.setOccluded

```
public void setOccluded(boolean isOccluded, boolean animate) {
    Trace.beginSection("KeyguardViewMediator#setOccluded");
    if (DEBUG) Log.d(TAG, "setOccluded " + isOccluded);
    mHandler.removeMessages(SET_OCCLUDED);
    Message msg = mHandler.obtainMessage(SET_OCCLUDED, isOccluded ? 1 : 0, animate ? 1 : 0);
    mHandler.sendMessage(msg);
    Trace.endSection();
}

...
    case SET_OCCLUDED:
        Trace.beginSection("KeyguardViewMediator#handleMessage SET_OCCLUDED");
        handleSetOccluded(msg.arg1 != 0, msg.arg2 != 0);
        Trace.endSection();
        break;
...

```

## KeyguardViewMediator.handleSetOccluded

```
private void handleSetOccluded(boolean isOccluded, boolean animate) {
    Trace.beginSection("KeyguardViewMediator#handleSetOccluded");
    synchronized (KeyguardViewMediator.this) {
        if (mHiding && isOccluded) {
            // We're in the process of going away but WindowManager wants to show a
            // SHOW_WHEN_LOCKED activity instead.
            startKeyguardExitAnimation(0, 0);
        }

        if (mOccluded != isOccluded) {
            mOccluded = isOccluded;
            mStatusBarKeyguardViewManager.setOccluded(isOccluded, animate);
            // 重点分析
            updateActivityLockScreenState();
            adjustStatusBarLocked();
        }
    }
    Trace.endSection();
}
```

### StatusBarKeyguardViewManager.setOccluded

```
public void setOccluded(boolean occluded, boolean animate) {
    if (occluded && !mOccluded && mShowing) {
        if (mPhoneStatusBar.isInLaunchTransition()) {
            mOccluded = true;
            mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(null /* beforeFading */,
                    new Runnable() {
                        @Override
                        public void run() {
                            mStatusBarWindowManager.setKeyguardOccluded(mOccluded);
                            reset();
                        }
                    });
            return;
        }
    }
    mOccluded = occluded;
    mPhoneStatusBar.updateMediaMetaData(false, animate && !occluded);
    mStatusBarWindowManager.setKeyguardOccluded(occluded);
    reset();
    if (animate && !occluded) {
        mPhoneStatusBar.animateKeyguardUnoccluding();
    }
}
```

#### PhoneStatusBar.updateMediaMetaData

该函数注释`Refresh or remove lockscreen artwork from media metadata or the lockscreen wallpaper.`。

#### StatusBarWindowManager.setKeyguardOccluded

```
public void setKeyguardOccluded(boolean occluded) {
    mCurrentState.keyguardOccluded = occluded;
    apply(mCurrentState);
}
```

apply函数更新窗口参数，并调用

```
private void apply(State state) {
    applyKeyguardFlags(state);
    applyForceStatusBarVisibleFlag(state);
    applyFocusableFlag(state);
    adjustScreenOrientation(state);
    applyHeight(state);
    applyUserActivityTimeout(state);
    applyInputFeatures(state);
    applyFitsSystemWindows(state);
    applyModalFlag(state);
    applyBrightness(state);
    applyHasTopUi(state);
    if (mLp.copyFrom(mLpChanged) != 0) {
        mWindowManager.updateViewLayout(mStatusBarView, mLp);
    }
    if (mHasTopUi != mHasTopUiChanged) {
        try {
            mActivityManager.setHasTopUi(mHasTopUiChanged);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to call setHasTopUi", e);
        }
        mHasTopUi = mHasTopUiChanged;
    }
}
```

回调 `WMS.updateViewLayout`

存疑，这个函数最终调用到哪里去了呢？？如何把这个函数存储的信息更新给WMS呢？WMS中没有 `updateViewLayout` API。

思考：修改 `WindowManager.LayoutParams` 中的参数，需要WMS执行relayout。因为LP参数中可以配置窗口位置，以及状态栏、导航栏的可见性，这显然需要窗口重新布局。

#### StatusBarKeyguardViewManager.reset

```
public void reset() {
    if (mShowing) {
        if (mOccluded) {
            mPhoneStatusBar.hideKeyguard();
            mPhoneStatusBar.stopWaitingForKeyguardExit();
            mBouncer.hide(false /* destroyView */);
        } else {
            // 调用到这里
            showBouncerOrKeyguard();
        }
        KeyguardUpdateMonitor.getInstance(mContext).sendKeyguardReset();
        updateStates();
    }
}
```

##### StatusBarKeyguardViewManager.showBouncerOrKeyguard

```
protected void showBouncerOrKeyguard() {
    if (mBouncer.needsFullscreenBouncer()) {

        // The keyguard might be showing (already). So we need to hide it.
        mPhoneStatusBar.hideKeyguard();
        mBouncer.show(true /* resetSecuritySelection */);
    } else {
        // 调用到这里
        mPhoneStatusBar.showKeyguard();
        mBouncer.hide(false /* destroyView */);
        mBouncer.prepare();
    }
}
```

##### PhoneStatusBar.showKeyguard

```
public void showKeyguard() {
    if (mLaunchTransitionFadingAway) {
        mNotificationPanel.animate().cancel();
        onLaunchTransitionFadingEnded();
    }
    mHandler.removeMessages(MSG_LAUNCH_TRANSITION_TIMEOUT);
    if (mUserSwitcherController != null && mUserSwitcherController.useFullscreenUserSwitcher()) {
        setBarState(StatusBarState.FULLSCREEN_USER_SWITCHER);
    } else {
        // 调用这里
        setBarState(StatusBarState.KEYGUARD);
    }

    //更新keyguard状态信息，重点分析
    updateKeyguardState(false /* goingToFullShade */, false /* fromShadeLocked */);
    if (!mDeviceInteractive) {

        // If the screen is off already, we need to disable touch events because these might
        // collapse the panel after we expanded it, and thus we would end up with a blank
        // Keyguard.
        mNotificationPanel.setTouchDisabled(true);
    }
    if (mState == StatusBarState.KEYGUARD) {
        instantExpandNotificationsPanel();
    } else if (mState == StatusBarState.FULLSCREEN_USER_SWITCHER) {
        instantCollapseNotificationPanel();
    }
    mLeaveOpenOnKeyguardHide = false;
    if (mDraggedDownRow != null) {
        mDraggedDownRow.setUserLocked(false);
        mDraggedDownRow.notifyHeightChanged(false  /* needsAnimation */);
        mDraggedDownRow = null;
    }
    mPendingRemoteInputView = null;
    mAssistManager.onLockscreenShown();
}
```

###### PhoneStatusBar.setBarState

```
    public void setBarState(int state) {
        // If we're visible and switched to SHADE_LOCKED (the user dragged
        // down on the lockscreen), clear notification LED, vibration,
        // ringing.
        // Other transitions are covered in handleVisibleToUserChanged().
        if (state != mState && mVisible && (state == StatusBarState.SHADE_LOCKED
                || (state == StatusBarState.SHADE && isGoingToNotificationShade()))) {
            clearNotificationEffects();
        }
        if (state == StatusBarState.KEYGUARD) {
            removeRemoteInputEntriesKeptUntilCollapsed();
            maybeEscalateHeadsUp();
        }
        mState = state;
        mGroupManager.setStatusBarState(state);
        mFalsingManager.setStatusBarState(state);
        mStatusBarWindowManager.setStatusBarState(state);
        updateReportRejectedTouchVisibility();
        // 下面函数有Trace打印
        updateDozing();
    }
```

###### PhoneStatusBar.updateKeyguardState

```
protected void updateKeyguardState(boolean goingToFullShade, boolean fromShadeLocked) {
    Trace.beginSection("PhoneStatusBar#updateKeyguardState");
    // 1.
    if (mState == StatusBarState.KEYGUARD) {
        // 调用到这里
        mKeyguardIndicationController.setVisible(true);
        mNotificationPanel.resetViews();
        if (mKeyguardUserSwitcher != null) {
            mKeyguardUserSwitcher.setKeyguard(true, fromShadeLocked);
        }
        mStatusBarView.removePendingHideExpandedRunnables();
    } else {
        mKeyguardIndicationController.setVisible(false);
        if (mKeyguardUserSwitcher != null) {
            mKeyguardUserSwitcher.setKeyguard(false,
                    goingToFullShade ||
                    mState == StatusBarState.SHADE_LOCKED ||
                    fromShadeLocked);
        }
    }
    if (mState == StatusBarState.KEYGUARD || mState == StatusBarState.SHADE_LOCKED) {
        mScrimController.setKeyguardShowing(true);
    } else {
        mScrimController.setKeyguardShowing(false);
    }
    mIconPolicy.notifyKeyguardShowingChanged();
    mNotificationPanel.setBarState(mState, mKeyguardFadingAway, goingToFullShade);
    //2. 这里打印
    updateDozingState();
    updatePublicMode();
    updateStackScrollerState(goingToFullShade, fromShadeLocked);
    updateNotifications();
    checkBarModes();
    //3. 这里再次打印
    updateMediaMetaData(false, mState != StatusBarState.KEYGUARD);
    mKeyguardMonitor.notifyKeyguardState(mStatusBarKeyguardViewManager.isShowing(),
            mStatusBarKeyguardViewManager.isSecure(),
            mStatusBarKeyguardViewManager.isOccluded());
    //4
    Trace.endSection();
}
```

从trace.html上来看

- 1-4：20.675ms
- 1-2: 17ms
- 2-3: 1.674ms
- 3  : 0.015ms
- 3-4: 1.436ms

### KeyguardViewMediator.updateActivityLockScreenState

```
private void updateActivityLockScreenState() {
    Trace.beginSection("KeyguardViewMediator#updateActivityLockScreenState");
    try {
        ActivityManagerNative.getDefault().setLockScreenShown(mShowing, mOccluded);
    } catch (RemoteException e) {
    }
    Trace.endSection();
}
```

时间为26.022ms


# AMS.setLockScreenShown

```
public void setLockScreenShown(boolean showing, boolean occluded) {
    if (checkCallingPermission(android.Manifest.permission.DEVICE_POWER)
            != PackageManager.PERMISSION_GRANTED) {
        throw new SecurityException("Requires permission "
                + android.Manifest.permission.DEVICE_POWER);
    }

    synchronized(this) {
        long ident = Binder.clearCallingIdentity();
        try {
            // 这行打印日志，此时 showing=true, occluded=false
            if (DEBUG_LOCKSCREEN) logLockScreen(" showing=" + showing + " occluded=" + occluded);

            // 此时mLockScreenShown为LOCK_SCREEN_SHOWN
            mLockScreenShown = (showing && !occluded) ? LOCK_SCREEN_SHOWN : LOCK_SCREEN_HIDDEN;
            // 条件不满足
            if (showing && occluded) {
                // The lock screen is currently showing, but is occluded by a window that can
                // show on top of the lock screen. In this can we want to dismiss the docked
                // stack since it will be complicated/risky to try to put the activity on top
                // of the lock screen in the right fullscreen configuration.
                mStackSupervisor.moveTasksToFullscreenStackLocked(DOCKED_STACK_ID,
                        mStackSupervisor.mFocusedStack.getStackId() == DOCKED_STACK_ID);
            }

            updateSleepIfNeededLocked();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
```

打印日志

```
02-17 23:00:34.653   851  1385 D ActivityManager: com.android.server.am.ActivityManagerService.setLockScreenShown:11876 android.app.ActivityManagerNative.onTransact:1532 : showing=true occluded=false mLockScreenShown=LOCK_SCREEN_HIDDEN mWakefulness=Awake mSleeping=false
```

## AMS.updateSleepIfNeededLocked

```
void updateSleepIfNeededLocked() {
    if (mSleeping && !shouldSleepLocked()) {
        mSleeping = false;
        startTimeTrackingFocusedActivityLocked();
        mTopProcessState = ActivityManager.PROCESS_STATE_TOP;
        mStackSupervisor.comeOutOfSleepIfNeededLocked();
        updateOomAdjLocked();
    } else if (!mSleeping && shouldSleepLocked()) {
        // 进入这个分支执行
        mSleeping = true;
        if (mCurAppTimeTracker != null) {
            mCurAppTimeTracker.stop();
        }
        mTopProcessState = ActivityManager.PROCESS_STATE_TOP_SLEEPING;
        // 调用到这里
        mStackSupervisor.goingToSleepLocked();
        updateOomAdjLocked();

        // Initialize the wake times of all processes.
        checkExcessivePowerUsageLocked(false);
        mHandler.removeMessages(CHECK_EXCESSIVE_WAKE_LOCKS_MSG);
        Message nmsg = mHandler.obtainMessage(CHECK_EXCESSIVE_WAKE_LOCKS_MSG);
        mHandler.sendMessageDelayed(nmsg, POWER_CHECK_DELAY);
    }
}
```

### ActivityStackSupervisor.goingToSleepLocked

```
void goingToSleepLocked() {
    scheduleSleepTimeout();
    if (!mGoingToSleep.isHeld()) {
        mGoingToSleep.acquire();
        if (mLaunchingActivity.isHeld()) {
            if (VALIDATE_WAKE_LOCK_CALLER && Binder.getCallingUid() != Process.myUid()) {
                throw new IllegalStateException("Calling must be system uid");
            }
            mLaunchingActivity.release();
            mService.mHandler.removeMessages(LAUNCH_TIMEOUT_MSG);
        }
    }

    //执行这里
    checkReadyForSleepLocked();
}
```

#### ActivityStackSupervisor.checkReadyForSleepLocked

```
void checkReadyForSleepLocked() {
    if (!mService.isSleepingOrShuttingDownLocked()) {
        // Do not care.
        return;
    }

    if (!mSleepTimeout) {
        boolean dontSleep = false;
        for (int displayNdx = mActivityDisplays.size() - 1; displayNdx >= 0; --displayNdx) {
            final ArrayList<ActivityStack> stacks = mActivityDisplays.valueAt(displayNdx).mStacks;
            for (int stackNdx = stacks.size() - 1; stackNdx >= 0; --stackNdx) {
                // 调用这里
                dontSleep |= stacks.get(stackNdx).checkReadyForSleepLocked();
            }
        }

        if (mStoppingActivities.size() > 0) {
            // Still need to tell some activities to stop; can't sleep yet.
            if (DEBUG_PAUSE) Slog.v(TAG_PAUSE, "Sleep still need to stop "
                    + mStoppingActivities.size() + " activities");
            scheduleIdleLocked();
            dontSleep = true;
        }

        if (mGoingToSleepActivities.size() > 0) {
            // Still need to tell some activities to sleep; can't sleep yet.
            if (DEBUG_PAUSE) Slog.v(TAG_PAUSE, "Sleep still need to sleep "
                    + mGoingToSleepActivities.size() + " activities");
            dontSleep = true;
        }

        if (dontSleep) {
            // 从这里返回
            return;
        }
    }

    // Send launch end powerhint before going sleep
    mService.mActivityStarter.sendPowerHintForLaunchEndIfNeeded();

    for (int displayNdx = mActivityDisplays.size() - 1; displayNdx >= 0; --displayNdx) {
        final ArrayList<ActivityStack> stacks = mActivityDisplays.valueAt(displayNdx).mStacks;
        for (int stackNdx = stacks.size() - 1; stackNdx >= 0; --stackNdx) {
            stacks.get(stackNdx).goToSleep();
        }
    }

    removeSleepTimeouts();

    if (mGoingToSleep.isHeld()) {
        mGoingToSleep.release();
    }
    if (mService.mShuttingDown) {
        mService.notifyAll();
    }
}
```

##### ActivityStack.checkReadyForSleepLocked

```
boolean checkReadyForSleepLocked() {
    if (mResumedActivity != null) {
        // 条件满足，进入这里执行
        // Still have something resumed; can't sleep until it is paused.
        if (DEBUG_PAUSE) Slog.v(TAG_PAUSE, "Sleep needs to pause " + mResumedActivity);
        if (DEBUG_USER_LEAVING) Slog.v(TAG_USER_LEAVING,
                "Sleep => pause with userLeaving=false");
        startPausingLocked(false, true, null, false);
        // 从这里返回
        return true;
    }
    if (mPausingActivity != null) {
        // Still waiting for something to pause; can't sleep yet.
        if (DEBUG_PAUSE) Slog.v(TAG_PAUSE, "Sleep still waiting to pause " + mPausingActivity);
        return true;
    }

    if (hasVisibleBehindActivity()) {
        // Stop visible behind activity before going to sleep.
        final ActivityRecord r = getVisibleBehindActivity();
        mStackSupervisor.mStoppingActivities.add(r);
        if (DEBUG_STATES) Slog.v(TAG_STATES, "Sleep still waiting to stop visible behind " + r);
        return true;
    }

    return false;
}
```

# 遗留问题

1. SystemUI里，设置StatusBar为全屏以绘制keyguard的代码在哪里？
2. `PhoneStatusBar.showKeyguard`在SystemUI的主线程中调用，如果该函数被Block住，那么为什么StartusBar窗口的LP参数如何更新给WMS？
