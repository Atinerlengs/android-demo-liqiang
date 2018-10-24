#### Android Power 按键处理流程梳理

电源按键是系统级的按键，分析主要 **PhoneWindowManager** 的处理流程。

首先PhoneWindowManager中的dispatchUnhandledKey方法中，下面是关于该方法的部分代码:

代码目录: /frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    public KeyEvent dispatchUnhandledKey(WindowState win, KeyEvent event, int policyFlags) {
        ...
        KeyEvent fallbackEvent = null;
        if ((event.getFlags() & KeyEvent.FLAG_FALLBACK) == 0) {
            final KeyCharacterMap kcm = event.getKeyCharacterMap();
            final int keyCode = event.getKeyCode();
            final int metaState = event.getMetaState();
            final boolean initialDown = event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0;

            // Check for fallback actions specified by the key character map.
            final FallbackAction fallbackAction;
            if (initialDown) {
                fallbackAction = kcm.getFallbackAction(keyCode, metaState);
            } else {
                fallbackAction = mFallbackActions.get(keyCode);
            }

            if (fallbackAction != null) {
                if (DEBUG_INPUT) {
                    Slog.d(TAG, "Fallback: keyCode=" + fallbackAction.keyCode
                            + " metaState=" + Integer.toHexString(fallbackAction.metaState));
                }

                final int flags = event.getFlags() | KeyEvent.FLAG_FALLBACK;
                fallbackEvent = KeyEvent.obtain(
                        event.getDownTime(), event.getEventTime(),
                        event.getAction(), fallbackAction.keyCode,
                        event.getRepeatCount(), fallbackAction.metaState,
                        event.getDeviceId(), event.getScanCode(),
                        flags, event.getSource(), null);

                if (!interceptFallback(win, fallbackEvent, policyFlags)) {
                    fallbackEvent.recycle();
                    fallbackEvent = null;
                }

                if (initialDown) {
                    mFallbackActions.put(keyCode, fallbackAction);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    mFallbackActions.remove(keyCode);
                    fallbackAction.recycle();
                }
            }
        }
        ...
        return fallbackEvent;
    }
```

系统按键的主要处理流程主要实在 **interceptFallback** 方法中处理的，下面是关于 interceptFallback 方法的部分代码：

代码目录: /frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
     private boolean interceptFallback(WindowState win, KeyEvent fallbackEvent, int policyFlags) {
        int actions = interceptKeyBeforeQueueing(fallbackEvent, policyFlags);
        if ((actions & ACTION_PASS_TO_USER) != 0) {
            long delayMillis = interceptKeyBeforeDispatching(
                    win, fallbackEvent, policyFlags);
            if (delayMillis == 0) {
                return true;
            }
        }
        return false;
    }
```

查看interceptFallback方法，主要的处理逻辑是在 **interceptKeyBeforeQueueing** 方法中

代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
     public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        ...
        case KeyEvent.KEYCODE_POWER: {
            result &= ~ACTION_PASS_TO_USER;
            isWakeKey = false; // wake-up will be handled separately
            if (down) {
                interceptPowerKeyDown(event, interactive);
            } else {
                interceptPowerKeyUp(event, interactive, canceled);
            }
            break;
        }
        ...
        return result;
    }
```

电源按键的处理逻辑，之前是灭屏的，主要是在 **interceptPowerKeyDown** 方法中实现，下面是关于该方法的部分代码：

代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    private void interceptPowerKeyDown(KeyEvent event, boolean interactive) {
        ...
        // Latch power key state to detect screenshot chord.
        if (interactive && !mScreenshotChordPowerKeyTriggered
                && (event.getFlags() & KeyEvent.FLAG_FALLBACK) == 0) {
            mScreenshotChordPowerKeyTriggered = true;
            mScreenshotChordPowerKeyTime = event.getDownTime();
            interceptScreenshotChord();
            //*/ freeme,xupeng, 20160112,for freeme screen record
            //*/ freeme,Jack, 20150202, for screen record
            interceptScreenRecordChord();
            //*/
        }
        // Stop ringing or end call if configured to do so when power is pressed.
        TelecomManager telecomManager = getTelecommService();
        boolean hungUp = false;
        if (telecomManager != null) {
            if (telecomManager.isRinging()) {
                // Pressing Power while there's a ringing incoming
                // call should silence the ringer.
                telecomManager.silenceRinger();
            } else if ((mIncallPowerBehavior
                    & Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP) != 0
                    && telecomManager.isInCall() && interactive) {
                // Otherwise, if "Power button ends call" is enabled,
                // the Power button will hang up any current active call.
                hungUp = telecomManager.endCall();
            }
        }
        // If the power key has still not yet been handled, then detect short
        // press, long press, or multi press and decide what to do.
        mPowerKeyHandled = hungUp || mScreenshotChordVolumeDownKeyTriggered
                || mScreenshotChordVolumeUpKeyTriggered;
        if (!mPowerKeyHandled) {
            if (interactive) {
                // When interactive, we're already awake.
                // Wait for a long press or for the button to be released to decide what to do.
                if (hasLongPressOnPowerBehavior()) {
                    Message msg = mHandler.obtainMessage(MSG_POWER_LONG_PRESS);
                    msg.setAsynchronous(true);
                    mHandler.sendMessageDelayed(msg,
                            ViewConfiguration.get(mContext).getDeviceGlobalActionKeyTimeout());
                }
            } else { //灭屏处理
                wakeUpFromPowerKey(event.getDownTime());
                if (mSupportLongPressPowerWhenNonInteractive && hasLongPressOnPowerBehavior()) {
                    Message msg = mHandler.obtainMessage(MSG_POWER_LONG_PRESS);
                    msg.setAsynchronous(true);
                    mHandler.sendMessageDelayed(msg,
                            ViewConfiguration.get(mContext).getDeviceGlobalActionKeyTimeout());
                    mBeganFromNonInteractive = true;
                } else {
                    final int maxCount = getMaxMultiPressPowerCount();
                    if (maxCount <= 1) {
                        mPowerKeyHandled = true;
                    } else {
                        mBeganFromNonInteractive = true;
                    }
                }
            }
        }
    }
```

灭屏按键 **Down** 的处理

分析上面的Down方法，屏幕灭屏的主要逻辑处理是在 **!mPowerKeyHandled** 分支的 **else** 中，主要处理逻辑是 **wakeUpFromPowerKey** 方法

代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    private void wakeUpFromPowerKey(long eventTime) {
        wakeUp(eventTime, mAllowTheaterModeWakeFromPowerKey, "android.policy:POWER");
    }

    private boolean wakeUp(long wakeTime, boolean wakeInTheaterMode, String reason) {
        final boolean theaterModeEnabled = isTheaterModeEnabled();
        if (!wakeInTheaterMode && theaterModeEnabled) {
            return false;
        }
        if (theaterModeEnabled) {
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.THEATER_MODE_ON, 0);
        }
        mPowerManager.wakeUp(wakeTime, reason);
        return true;
    }
```

此时 **mAllowTheaterModeWakeFromPowerKey** 值为true，会将 **Settings.Global.THEATER_MODE_ON** 写入，调用 *PowerManage* 的 **wakeUp** 方法

代码目录：/frameworks/base/core/java/android/os/PowerManager.java

```
    public void wakeUp(long time, String reason) {
        try {
            mService.wakeUp(time, reason, mContext.getOpPackageName());
        } catch (RemoteException e) {
        }
    }
```

Power按键Down之前是灭屏/亮屏的处理逻辑，

代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    private void interceptPowerKeyUp(KeyEvent event, boolean interactive, boolean canceled) {
        final boolean handled = canceled || mPowerKeyHandled;
        mScreenshotChordPowerKeyTriggered = false;
        cancelPendingScreenshotChordAction();
        cancelPendingPowerKeyAction();
        if (!handled) {
            // Figure out how to handle the key now that it has been released.
            mPowerKeyPressCounter += 1;
            final int maxCount = getMaxMultiPressPowerCount();
            final long eventTime = event.getDownTime();
            if (mPowerKeyPressCounter < maxCount) {
                // This could be a multi-press.  Wait a little bit longer to confirm.
                // Continue holding the wake lock.
                Message msg = mHandler.obtainMessage(MSG_POWER_DELAYED_PRESS,
                        interactive ? 1 : 0, mPowerKeyPressCounter, eventTime);
                msg.setAsynchronous(true);
                mHandler.sendMessageDelayed(msg, ViewConfiguration.getDoubleTapTimeout());
                return;
            }
            // No other actions.  Handle it immediately.
            powerPress(eventTime, interactive, mPowerKeyPressCounter);
        }
        // Done.  Reset our state.
        finishPowerKeyPress();
    }
```

我们在之前Power按键按下，Down的情况时获得的 **mPowerKeyHandled** 值为true，则直接调用 **finishPowerKeyPress()** 方法

代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    private void finishPowerKeyPress() {
        mBeganFromNonInteractive = false;
        mPowerKeyPressCounter = 0;
        if (mPowerKeyWakeLock.isHeld()) {
            mPowerKeyWakeLock.release();
        }
    }
```

Power按键之前是亮屏的情况，短按、长按：

长按的情况，上面Down的时候 **interactive** 有调用逻辑，

代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    private void powerLongPress() {
        final int behavior = getResolvedLongPressOnPowerBehavior();
        switch (behavior) {
        case LONG_PRESS_POWER_NOTHING:
            break;
        case LONG_PRESS_POWER_GLOBAL_ACTIONS:
            mPowerKeyHandled = true;
            if (!performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false)) {
                performAuditoryFeedbackForAccessibilityIfNeed();
            }
            showGlobalActionsInternal();
            break;
        case LONG_PRESS_POWER_SHUT_OFF:
        case LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM:
            mPowerKeyHandled = true;
            performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false);
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
            mWindowManagerFuncs.shutdown(behavior == LONG_PRESS_POWER_SHUT_OFF);
            break;
        }
    }
```

长按mPowerKeyHandled = true，直接最后interceptPowerKeyUp函数就调用 **finishPowerKeyPress()方法** 了。

短按的情况，代码目录：/frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java

```
    private void powerPress(long eventTime, boolean interactive, int count) {
        if (mScreenOnEarly && !mScreenOnFully) {
            Slog.i(TAG, "Suppressed redundant power key press while "
                    + "already in the process of turning the screen on.");
            return;
        }
        if (count == 2) {
            powerMultiPressAction(eventTime, interactive, mDoublePressOnPowerBehavior);
        } else if (count == 3) {
            powerMultiPressAction(eventTime, interactive, mTriplePressOnPowerBehavior);
        } else if (interactive && !mBeganFromNonInteractive) {
            switch (mShortPressOnPowerBehavior) {
                case SHORT_PRESS_POWER_NOTHING:
                    break;
                case SHORT_PRESS_POWER_GO_TO_SLEEP:
                    mPowerManager.goToSleep(eventTime,
                            PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON, 0);
                    break;
                case SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP:
                    mPowerManager.goToSleep(eventTime,
                            PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON,
                            PowerManager.GO_TO_SLEEP_FLAG_NO_DOZE);
                    break;
                case SHORT_PRESS_POWER_REALLY_GO_TO_SLEEP_AND_GO_HOME:
                    mPowerManager.goToSleep(eventTime,
                            PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON,
                            PowerManager.GO_TO_SLEEP_FLAG_NO_DOZE);
                    launchHomeFromHotKey();
                    break;
                case SHORT_PRESS_POWER_GO_HOME:
                    launchHomeFromHotKey(true /* awakenFromDreams */, false /*respectKeyguard*/);
                    break;
            }
        }
```

最后会调用到 **PowerManager** 的 **goToSleep()** 方法。


总结：

主要是Power按键在PhoneWindowManager中的处理：

1，Power按键按下时，前面的状态是亮屏还是灭屏
2，power按键按下时，前面的状态是亮屏，又有短按和长按

