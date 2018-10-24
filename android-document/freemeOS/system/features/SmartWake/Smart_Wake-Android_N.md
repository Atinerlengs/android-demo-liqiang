## 一、功能定义
智能唤醒由三个子功能组成，分别为手势唤醒、双击屏幕唤醒、双击HOME键唤醒或灭屏，前两者依赖TP传感支持，后者依赖HOME键上的传感芯片支持，除唤醒之外，手势唤醒子功能，还包含了音乐控制，应用启动，呼叫，短信等额外业务设置

## 二、概要描述
虽然系统源生支持灭屏时的触摸唤醒逻辑，但是受限于底层实现，框架层并没有将TP传感事件集成于Sensor系统中，而是接收输入子系统模拟的按键事件，在PhoneWindowManager中不影响该按键事件继续分发的前提下，进行特殊过滤，嵌入相应行为逻辑

同时，为了功能的可配置性，我们添加了设置存储，以便用户能方便的开关此功能

## 三、实现清单

###### ==1、主体实现类，封装了驱动使能实现、事件处理、数据监测和读取、动画控制==
- [FreemeSmartWake.java]

vendor\freeme\frameworks\base\services\core-export\java\com\freeme\server

###### 2、两个视图类，用于动画的显示
- [SmartWakeCharContainer.java]
- [SmartWakeView.java]

vendor\freeme\frameworks\base\core-export\java\com\freeme\internal\widget

###### 3、存储声明，包含四个子功能的使能和十四个模拟按键功能的行为描述
###### keyword: "SmartWake"
- [FreemeSettings.java]

vendor\freeme\frameworks\base\core-export\java\com\freeme\provider

###### 4、唤醒行为默认值设置
###### keyword: "SmartWake"
- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

###### 5、唤醒行为默认值读取
###### keyword: "SmartWake"
- [FreemeProvidersUtils.java]

frameworks\base\packages\SettingsProvider\src\com\freeme\providers\settings

###### 6、设置类，用于用户对手势唤醒行为的设置
- [SmarrtWakeGestureSettings.java]
- [SmartWakeMusicSettings.java]
- [SmartWakeSelectSettings.java]
- [SmartWakeStartupAppList.java]

packages\apps\Settings\src\com\freeme\settings\motion

###### 7、设置类，用于用户对双击唤醒行为和手势行为的使能
###### keyword: "SmartWake"
- [FreemeSmartWakeDoubleTapSettings.java]
- [FreemeGestureOperate.java]

packages\apps\Settings\src\com\freeme\settings\intelligence

###### 8、UI描述，对该设置Preference进行声明
###### keyword: "SmartWake"

- [freeme_gesture_operate.xml]
- [freeme_settings_intelligence_assistant.xml]

packages\apps\Settings\res-freeme\xml

###### 9、多语言
- [strings_freeme.xml]

packages\apps\Settings\res-freeme\values

packages\apps\Settings\res-freeme\values-zh-rCN

packages\apps\Settings\res-freeme\values-zh-rTW

###### ==10、驱动事件转接，唤醒控制==
###### keyword: "SmartWake"
- [PhoneWindowManager.java]

frameworks\base\services\core\java\com\android\server\policy

###### 11、res添加动画所需资源图片，并对其声明
###### keyword: "SmartWake"
- [symbols_freeme.xml]
- [smart_wake_c.png][smart_wake_e.png][smart_wake_m.png][smart_wake_o.png][smart_wake_s.png][smart_wake_v.png]
- [smart_wake_w.png][smart_wake_z.png][smart_wake_up.png][smart_wake_down.png][smart_wake_left.png][smart_wake_right.png][smart_wake_camera.png][smart_wake_rarrow.png][smart_wake_reversal_o.png]

frameworks\base\core\res\res

###### 12、 为Java世界提供三个宏开关，在宏关闭的状态下，原则上不影响原生代码的任何行为

```
Public Static final boolean FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT = getBoolean("ro.freeme.screen_gesture_wakeup");
Public Static final boolean FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT = getBoolean("ro.freeme.screendoubletapwakeup");
Public Static final boolean FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT = getBoolean("ro.freeme.home_doubletap_wakeup");
```

- [FreemeOption.java]

Vendor/freeme/frameworks/base/core-export/java/com/freeme/util

###### 13、配置开关，决定是否开启该功能

```
FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT=yes
FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT=yes
FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT=no
```
- [ProjectConfig.mk]

device/droi/common

###### 14、读取ProjectConfig.mk中的值，写入build.prop中，开发调试时，可手动修改build.prop中的相应ro值来开启和关闭功能

```
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

- [device.mk]

device/droi/common

## 四、项目配置
###### ==1、项目配置时，请根据需要需要开启，yes为开启，no为关闭，请在相应的项目配置文件中override该属性==

```
FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT=yes
FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT=yes
FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT=no
```
- [ProjectConfig.mk]

device/droi/project_name

###### ==2、唤醒行为默认值设置，配置时，请在相应的项目文件夹中重写该文件==
###### keyword: "SmartWake"
- 开关配置，0为关闭，1为开启
- 行为配置，使用字符串解析的方式，字段之间以‘;’隔开
- 字段1表示功能，包含：startupapp/startupcall/startupmms/mediacontrol/unlock，相应表述为：启动应用/拨打电话/打开短信/控制音乐/解锁锁屏
- 字段2用于表示显示在Summary中的字串，其中，对如下字符串有做多语言转义，appweixin/appqq/appbrowser/appcall/appmusic/musicstartpause/musicprev/musicnext，其余保持原始字串显示
- 字段3、4根据字段1的功能不同有所区别，如果为startupapp，字段3、4分别表示包名和类名；如果为startupcall，字段3为呼叫的号码；如果为startupmms和unlock，无字段3和4；如果为mediacontrol，字段3为子功能项：musicprev/musicnext/musicstartpause，表示为上一首/下一首/播放暂停；
- 如下代码段为示例

```
<integer name="def_home_doubletap_poweroff_enabled" translatable="false">0</integer>
……
<string name="def_screen_action_m_setting" translatable="flase">
    startupapp;appmusic;com.google.android.music;com.android.music.activitymanagement.TopLevelActivity</string>
……
```

- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

## 五、总结
- 该功能牵涉的改动文件比较多，移植时请注意遗漏
- 手势唤醒的子项较多，请务必验证每一项的功能正常
