## 一、功能描述
智能唤醒由三个子功能组成，分别为手势唤醒、双击屏幕唤醒、双击HOME键唤醒或灭屏，前两者依赖TP传感支持，后者依赖HOME键上的传感芯片支持，除唤醒之外，手势唤醒子功能，还包含了音乐控制，应用启动，呼叫，短信等额外业务设置
## 二、概要描述
虽然系统源生支持灭屏时的触摸唤醒逻辑，但是受限于底层实现，框架层并没有将TP传感事件集成于Sensor系统中，而是接收输入子系统模拟的按键事件，在PhoneWindowManager中不影响该按键事件继续分发的前提下，进行特殊过滤，嵌入相应行为逻辑

同时，为了功能的可配置性，我们添加了设置存储，以便用户能方便的开关此功能
## 三、详细实现
### 1、Feature开关

```
FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT=yes
FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT=yes
FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT=no

ifeq ($(strip $(FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.screen_gesture_wakeup=1
endif
ifeq ($(strip $(FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.screendoubletapwakeup=1
endif
ifeq ($(strip $(FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.home_doubletap_wakeup=1
endif
```

### 2、存储
#### A、子功能

```
/** @hide */public static final String FREEME_HOME_DOUBLETAP_POWEROFF_ENABLED   = "freeme_home_doubletap_poweroff_enabled";
/** @hide */public static final String FREEME_HOME_DOUBLETAP_WAKEUP_ENABLED     = "freeme_home_doubletap_wakeup_enabled";
/** @hide */public static final String FREEME_SCREEN_DOUBLETAP_WAKEUP_ENABLED   = "freeme_screen_doubletap_wakeup_enabled";
/** @hide */public static final String FREEME_SCREEN_GESTURE_WAKEUP_ENABLED     = "freeme_screen_gesture_wakeup_enabled";
```

#### B、模拟按键功能

```
/** @hide */public static final String FREEME_SCREEN_WAKEUP_W_ENABLED       = "freeme_screen_wakeup_w_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_O_ENABLED       = "freeme_screen_wakeup_o_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_E_ENABLED       = "freeme_screen_wakeup_e_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_C_ENABLED       = "freeme_screen_wakeup_c_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_M_ENABLED       = "freeme_screen_wakeup_m_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_V_ENABLED       = "freeme_screen_wakeup_v_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_S_ENABLED       = "freeme_screen_wakeup_s_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_Z_ENABLED       = "freeme_screen_wakeup_z_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_UP_ENABLED      = "freeme_screen_wakeup_up_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_DWON_ENABLED    = "freeme_screen_wakeup_down_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_LEFT_ENABLED    = "freeme_screen_wakeup_left_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_RIGHT_ENABLED   = "freeme_screen_wakeup_right_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_RARROW_ENABLED  = "freeme_screen_wakeup_rarrow_enabled";
/** @hide */public static final String FREEME_SCREEN_WAKEUP_TARROW_ENABLED  = "freeme_screen_wakeup_tarrow_enabled";
```

#### C、模拟按键行为

```
/** @hide */public static final String FREEME_SCREEN_ACTION_W_SETTING       = "freeme_screen_action_w_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_O_SETTING       = "freeme_screen_action_o_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_E_SETTING       = "freeme_screen_action_e_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_C_SETTING       = "freeme_screen_action_c_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_M_SETTING       = "freeme_screen_action_m_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_V_SETTING       = "freeme_screen_action_v_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_S_SETTING       = "freeme_screen_action_s_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_Z_SETTING       = "freeme_screen_action_z_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_UP_SETTING      = "freeme_screen_action_up_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_DOWN_SETTING    = "freeme_screen_action_down_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_LEFT_SETTING    = "freeme_screen_action_left_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_RIGHT_SETTING   = "freeme_screen_action_right_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_RARROW_SETTING  = "freeme_screen_action_rarrow_setting";
/** @hide */public static final String FREEME_SCREEN_ACTION_TARROW_SETTING  = "freeme_screen_action_tarrow_setting";
```

### 3、功能实现
#### A、驱动节点
/sys/class/syna/gesenable，任意一个Feature打开，节点为enable，否则为disable，PhoneWindowManager中SystemReady时打开节点

```
if ((FeatureOption.FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT ||
    FeatureOption.FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT ||
    FeatureOption.FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT) && mFreemeSmartWake != null) {
    mFreemeSmartWake.updateWakeUpDeviceNodeStatus(true);
}
```

#### B、事件模拟
驱动模拟按键事件上报，在PhonewindowManager中过滤处理

```
//*/ freeme.zhiwei.zhang, 20160909. SmartWake.
if (mFreemeSmartWake != null &&
    mFreemeSmartWake.interceptKeyBeforeQueueing(event, policyFlags, mAwake)) {
    isWakeKey = true; // need wake up screen.
}
//*/
```

#### C、存储更新
SettingsObserver监听

```
private class SettingsObserver extends ContentObserver {
    SettingsObserver(Handler handler) {
        super(handler);
    }

    void observe() {
        if (FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT) {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.FREEME_SCREEN_GESTURE_WAKEUP_ENABLED), false, this);
        }
        if (FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT) {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.FREEME_SCREEN_DOUBLETAP_WAKEUP_ENABLED), false, this);
        }
        if (FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT) {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.FREEME_HOME_DOUBLETAP_WAKEUP_ENABLED), false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.FREEME_HOME_DOUBLETAP_POWEROFF_ENABLED), false, this);
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        mScreenGestureWakeUpEnabled = FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT &&
                Settings.System.getInt(mResolver, Settings.System.FREEME_SCREEN_GESTURE_WAKEUP_ENABLED, 0) == 1;
        mScreenDoubleTapWakeUpEnabled = FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT &&
                Settings.System.getInt(mResolver, Settings.System.FREEME_SCREEN_DOUBLETAP_WAKEUP_ENABLED, 0) == 1;
        mHomeDoubleTapWakeupEnabled = FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT &&
                Settings.System.getInt(mResolver, Settings.System.FREEME_HOME_DOUBLETAP_WAKEUP_ENABLED, 0) == 1;
        mHomeDoubleTapPoweroffEnabled = FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT &&
                Settings.System.getInt(mResolver, Settings.System.FREEME_HOME_DOUBLETAP_POWEROFF_ENABLED, 0) == 1;

        boolean anyFeatureEnabled = mScreenGestureWakeUpEnabled
                || mScreenDoubleTapWakeUpEnabled
                || mHomeDoubleTapWakeupEnabled
                || mHomeDoubleTapPoweroffEnabled;
        if (mAnySmartWakeUpEnabled != anyFeatureEnabled) {
            mAnySmartWakeUpEnabled = anyFeatureEnabled;
            writeEnableToSysFile(SMART_WAKE_SWITCH_PATH, anyFeatureEnabled);
        }
    }
}
```

#### D、播放动画
PhoneWindowManager中finishScreenTurningOn时，开始播放动画

```
//*/ freeme.zhiwei.zhang, 20160909. SmartWake.
if (mFreemeSmartWake != null) {
    mFreemeSmartWake.startSmartViewAnimation();
}
//*/
```

#### E、唤醒
复用系统按键唤醒逻辑

复用PhoneWindowManager中BrocastWakeLock，用于分发给UI Handler
#### F、震动
媒体按键时复用系统虚拟键模拟反馈逻辑，非媒体按键时，在100ms延迟后，复用系统虚拟键模拟反馈逻辑，因为受SystemUI影响，亮屏时，会清掉所有震动事件，所以起了100ms延迟来规避

```
Message msg = mHandler.obtainMessage(MSG_SMART_DO_VIBRATE);
mHandler.sendMessageDelayed(msg, VIBRATE_DELAY_TIME);// system ui would cancel all vibrate while visible, so we add a delay.
```

#### G、设置
目前的代码，Double Tap逻辑统一写在Setting中，手势相关唤醒统一写在Vendor下的motionrecognition包中
#### H、动画
待续

## 四、总结
### 1、关键点
此功能实现关键点在于和电源管理系统的耦合及输入子系统的影响，是否会影响到这两者，还有待测试和考量，后续会持续对这两点进行维护和优化
### 2、代码可移植性
目前功能代码量较大，且散落仍然比较严重，当前版本已经将重复实现和部分冗余代码清理，后续还需要进行缩减和优化

##### --- 持续更新中

## 附（当前版本关联java文件）
##### vendor\droi\freeme\packages\apps\FreemeMotionRecognition\src\com\freeme\motionrecognition
SmartWakeGestureSettings.java

SmartWakeMusicSettings.java

SmartWakeSelectSettings.java

SmartWakeStartupAppList.java

CustomPreference.java

CustomSwitchPreference.java

##### vendor\droi\freeme\frameworks\base\services\core-export\java\com\freeme\server下
FreemeSmartWake.java
##### packages\apps\Settings\src\com\android\settings\accessibility
SmartWakeDoubleTapSettings.java
GestureOperate.java
##### frameworks\base\services\core\java\com\android\server\policy
PhoneWindowManager.java
##### frameworks\base\core\java\android\provider
Settings.java
