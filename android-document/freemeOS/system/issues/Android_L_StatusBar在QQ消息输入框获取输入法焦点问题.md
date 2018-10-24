# StatusBar 输入法bug 分析
### 问题
在密码锁屏界面，如来qq消息则出现qq弹框，此时下拉通知栏并点击任意按钮/通知，出现密码解锁界面，此时输入法界面不能弹出

### 测试用例
利用github上已有的测试用例，测试该问题。
[仿QQ5.0Android版 在锁屏桌面显示消息框](http://cloay.com/blog/2014/08/25/fang-qq5-dot-0androidban-zai-suo-ping-zhuo-mian-xian-shi-xiao-xi-kuang/)

### 分析流程

1. 根据上报的现象，复现并确认所有Android L手机均有该类BUG
2. 观察到点击密码输入框时，背景中出现输入法阴影，判断输入法已经弹出
3. 通过dumpsys window windows 查看当前输入法layer等级，其与qq弹框layer一致
4. 查看密码锁屏界面(KeyguardPasswordView.java)如何调出输入法。

```java
@Override
public void onResume(final int reason) {
    super.onResume(reason);

    // Wait a bit to focus the field so the focusable flag on the window is already set then.
    post(new Runnable() {
        @Override
        public void run() {
            if (isShown()) {
                mPasswordEntry.requestFocus();
                Log.d(TAG, "reason = " + reason +
                    ", mShowImeAtScreenOn = " + mShowImeAtScreenOn) ;
                if (reason != KeyguardSecurityView.SCREEN_ON || mShowImeAtScreenOn) {
                    Log.d(TAG, "onResume() - call showSoftInput()") ;
                    mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }
    });
}
```

查看InputMethodManager.java

```java
public boolean showSoftInput(View view, int flags, ResultReceiver resultReceiver) {
    checkFocus();
    if (DEBUG) Log.d(TAG, "ap request show soft input.", new Exception());
    synchronized (mH) {
        if (mServedView != view && (mServedView == null
                || !mServedView.checkInputConnectionProxy(view))) {
            return false;
        }

        try {
            if (DEBUG) Log.d(TAG, "Soft input will be shown");
            return mService.showSoftInput(mClient, flags, resultReceiver);
        } catch (RemoteException e) {
        }

        return false;
    }
}
```

关键在于 mClient 对象是什么，发现其在 onWindowFocus 发生变化

```java
/**
 * Called by ViewAncestor when its window gets input focus.
 * @hide
 */
public void onWindowFocus(View rootView, View focusedView, int softInputMode,
        boolean first, int windowFlags) {
    boolean forceNewFocus = false;
    synchronized (mH) {
        /*if (DEBUG) */Log.v(TAG, "onWindowFocus: " + focusedView
                + " softInputMode=" + softInputMode
                + " first=" + first + " flags=#"
                + Integer.toHexString(windowFlags));
        if (mHasBeenInactive) {
            if (DEBUG) Log.v(TAG, "Has been inactive!  Starting fresh");
            mHasBeenInactive = false;
            forceNewFocus = true;
        }
        focusInLocked(focusedView != null ? focusedView : rootView);
    }
}
```

查看 log 如下：

```java
Log.v(TAG, "onWindowFocus: " + focusedView
                + " softInputMode=" + softInputMode
                + " first=" + first + " flags=#"
                + Integer.toHexString(windowFlags));
```

- 在点击后，出现PasswordEntry ，但是onWindowFocus 并未变调用，此时输入法的mcilent 对象任然是qq弹框，所以其出现在qq弹框上面，而不是密码解锁界面
- 而正常情况下该函数被正常调用，新的focusedView变化为statusbar，输入法正常显示
调查onWindowFocus（）函数的调用栈情况如下：

```java
imm.onWindowFocus(mView, mView.findFocus(),                                 ViewRootImpl.java
case MSG_WINDOW_FOCUS_CHANGED: {                                            ViewRootImpl.java
public void windowFocusChanged() {                                          ViewRootImpl.java
public void reportFocusChangedSerialized()                                  WindowState.java
 case REPORT_FOCUS_CHANGE: {                                            WindowManagerService.java
```

结合以上调用情况，可以看出系统整体焦点变化的一个调用情况，通过打log发现 ViewRootImpl.java  中在下拉通知栏时，focus确实发生了变化，但是最终调用到 ViewRootImpl.java 时，并没有调用onWindowFocus函数。查看接受到消息后，ViewRootImpl的 处理

```java
mLastWasImTarget = WindowManager.LayoutParams
        .mayUseInputMethod(mWindowAttributes.flags);

InputMethodManager imm = InputMethodManager.peekInstance();
if (imm != null && mLastWasImTarget && !isInLocalFocusMode()) {
    imm.onPreWindowFocus(mView, hasWindowFocus);
}
if (mView != null) {
    mAttachInfo.mKeyDispatchState.reset();
    mView.dispatchWindowFocusChanged(hasWindowFocus);
    mAttachInfo.mTreeObserver.dispatchOnWindowFocusChange(hasWindowFocus);
}

if (DEBUG_IMF) {
    Log.v(TAG, "Handle MSG_WINDOW_FOCUS_CHANGED: hasWindowFocus = "
            + hasWindowFocus + ", mLastWasImTarget = " + mLastWasImTarget
            + ", softInputMode = #"
            + Integer.toHexString(mWindowAttributes.softInputMode)
            + ", window flags = #"
            + Integer.toHexString(mWindowAttributes.flags)
            + ", mView = " + mView + ", this = " + this);
}

// Note: must be done after the focus change callbacks,
// so all of the view state is set up correctly.
if (hasWindowFocus) {
    if (imm != null && mLastWasImTarget && !isInLocalFocusMode()) {
        imm.onPostWindowFocus(mView, mView.findFocus(),
                mWindowAttributes.softInputMode,
                !mHasHadWindowFocus, mWindowAttributes.flags);
    }
    // Clear the forward bit.  We can just do this directly, since
    // the window manager doesn't care about it.
    mWindowAttributes.softInputMode &=
            ~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
    ((WindowManager.LayoutParams)mView.getLayoutParams())
            .softInputMode &=
                ~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION;
    mHasHadWindowFocus = true;
}
```

onPostWindowFocus 的调用受三个变量控制 imm != null && mLastWasImTarget && !isInLocalFocusMode()
通过log发现 mLastWasImTarget此时false。而

```java
 mLastWasImTarget = WindowManager.LayoutParams
                            .mayUseInputMethod(mWindowAttributes.flags);
```

所以查看 WindowManager 的mayUseInputMethod 函数

```java
public static boolean mayUseInputMethod(int flags) {
    switch (flags&(FLAG_NOT_FOCUSABLE|FLAG_ALT_FOCUSABLE_IM)) {
        case 0:
        case FLAG_NOT_FOCUSABLE|FLAG_ALT_FOCUSABLE_IM:
            return true;
    }
    return false;
}
```

关键在于FLAG_NOT_FOCUSABLE|FLAG_ALT_FOCUSABLE_IM 属性
查看Systemui中关于这俩个属性的定义，发现在StatusBarWindowManager.java 中

```
private void applyFocusableFlag(State state) {
    if (state.isKeyguardShowingAndNotOccluded() && state.keyguardNeedsInput
            && state.bouncerShowing) {
        mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
    } else if (state.isKeyguardShowingAndNotOccluded() || state.statusBarFocusable) {
        mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLpChanged.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
    } else {
        mLpChanged.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
    }
}
```

可以看到isKeyguardShowingAndNotOccluded 受qq弹框或闹钟影响，对比M上现有改动：

```diff
     private void applyFocusableFlag(State state) {
-        if (state.isKeyguardShowingAndNotOccluded() && state.keyguardNeedsInput
+        if (state.keyguardShowing && state.keyguardNeedsInput
                 && state.bouncerShowing) {
             mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
             mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
```

替换后，验证输入法正常弹出，但是输入框未被顶起
验证AS上5.1虚拟机也存在同样问题
在Android源码下查看StatusBarWindowManager.java 的修改提交记录，发现有以下提交

```
commit aa8061448ec5a0e3cef9685f4186fc94e09eb78e
Author: Jorim Jaggi <jjaggi@google.com>
Date:   Wed May 20 18:04:16 2015 -0700

    Fix status bar window IME flags & layout

    When bouncer was showing, but keyguard was occluded, staus bar
    window couldn't receive input, and thus the IME window was placed
    below the status bar window. In addition to that, fix the layout when
    IME is showing up on the bouncer screen.

    Bug: 19969474
.../statusbar/phone/StatusBarWindowManager.java       |  4 ++--
.../systemui/statusbar/phone/StatusBarWindowView.java |  5 ++++-
.../com/android/server/policy/PhoneWindowManager.java | 19 ++++++++++++++-----
```

查看该次提交所修改文件，并何入patch，验证有效
合入patch如下：

```diff
commit aa8061448ec5a0e3cef9685f4186fc94e09eb78e
Author: Jorim Jaggi <jjaggi@google.com>
Date:   Wed May 20 18:04:16 2015 -0700

    Fix status bar window IME flags & layout

    When bouncer was showing, but keyguard was occluded, staus bar
    window couldn't receive input, and thus the IME window was placed
    below the status bar window. In addition to that, fix the layout when
    IME is showing up on the bouncer screen.

    Bug: 19969474
    Change-Id: I38d21647801b57608d49c3f525d4840e6ba58296
---
 .../statusbar/phone/StatusBarWindowManager.java       |  4 ++--
 .../systemui/statusbar/phone/StatusBarWindowView.java |  5 ++++-
 .../com/android/server/policy/PhoneWindowManager.java | 19 ++++++++++++++-----
 3 files changed, 20 insertions(+), 8 deletions(-)

diff --git a/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowManager.java b/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowManager.java
index 4f1c652..de42643 100644
--- a/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowManager.java
+++ b/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowManager.java
@@ -115,8 +115,8 @@ public class StatusBarWindowManager {

     private void applyFocusableFlag(State state) {
         boolean panelFocusable = state.statusBarFocusable && state.panelExpanded;
-        if (state.isKeyguardShowingAndNotOccluded() && state.keyguardNeedsInput
-                && state.bouncerShowing || BaseStatusBar.ENABLE_REMOTE_INPUT && panelFocusable) {
+        if (state.keyguardShowing && state.keyguardNeedsInput && state.bouncerShowing
+                || BaseStatusBar.ENABLE_REMOTE_INPUT && panelFocusable) {
             mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
             mLpChanged.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
         } else if (state.isKeyguardShowingAndNotOccluded() || panelFocusable) {
diff --git a/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowView.java b/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowView.java
index a96f4e9..3b91751 100644
--- a/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowView.java
+++ b/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarWindowView.java
@@ -67,8 +67,10 @@ public class StatusBarWindowView extends FrameLayout {
                     || insets.top != getPaddingTop()
                     || insets.right != getPaddingRight()
                     || insets.bottom != getPaddingBottom();
+
+            // Drop top inset, apply right and left inset and pass through bottom inset.
             if (changed) {
-                setPadding(insets.left, insets.top, insets.right, 0);
+                setPadding(insets.left, 0, insets.right, 0);
             }
             insets.left = 0;
             insets.top = 0;
@@ -81,6 +83,7 @@ public class StatusBarWindowView extends FrameLayout {
             if (changed) {
                 setPadding(0, 0, 0, 0);
             }
+            insets.top = 0;
         }
         return false;
     }
diff --git a/services/core/java/com/android/server/policy/PhoneWindowManager.java b/services/core/java/com/android/server/policy/PhoneWindowManager.java
index 17368aa..06b30b6 100644
--- a/services/core/java/com/android/server/policy/PhoneWindowManager.java
+++ b/services/core/java/com/android/server/policy/PhoneWindowManager.java
@@ -3642,15 +3642,24 @@ public class PhoneWindowManager implements WindowManagerPolicy {
         }
     }

+    private boolean canReceiveInput(WindowState win) {
+        boolean notFocusable =
+                (win.getAttrs().flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0;
+        boolean altFocusableIm =
+                (win.getAttrs().flags & WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) != 0;
+        boolean notFocusableForIm = notFocusable ^ altFocusableIm;
+        return !notFocusableForIm;
+    }
+
     /** {@inheritDoc} */
     @Override
     public void layoutWindowLw(WindowState win, WindowState attached) {
-        // we've already done the status bar
-        final WindowManager.LayoutParams attrs = win.getAttrs();
-        if ((win == mStatusBar && (attrs.privateFlags & PRIVATE_FLAG_KEYGUARD) == 0) ||
-                win == mNavigationBar) {
+        // We've already done the navigation bar and status bar. If the status bar can receive
+        // input, we need to layout it again to accomodate for the IME window.
+        if ((win == mStatusBar && !canReceiveInput(win)) || win == mNavigationBar) {
             return;
         }
+        final WindowManager.LayoutParams attrs = win.getAttrs();
         final boolean isDefaultDisplay = win.isDefaultDisplay();
         final boolean needsToOffsetInputMethodTarget = isDefaultDisplay &&
                 (win == mLastInputMethodTargetWindow && mLastInputMethodWindow != null);
@@ -3717,7 +3726,7 @@ public class PhoneWindowManager implements WindowManagerPolicy {
                     + mUnrestrictedScreenHeight;
             cf.bottom = vf.bottom = mStableBottom;
             cf.top = vf.top = mStableTop;
-        } else if (win == mStatusBar && (attrs.privateFlags & PRIVATE_FLAG_KEYGUARD) != 0) {
+        } else if (win == mStatusBar) {
             pf.left = df.left = of.left = mUnrestrictedScreenLeft;
             pf.top = df.top = of.top = mUnrestrictedScreenTop;
             pf.right = df.right = of.right = mUnrestrictedScreenWidth + mUnrestrictedScreenLeft;

```
