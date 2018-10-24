## 一、功能定义
三指截屏即当用户在任何界面使用三根手指一起往下滑动时，进行截屏或者超级截屏，相对于使用组合键来截屏的方式，更加方便快捷

## 二、概要描述
根据DecorView中触摸事件中手指个数、位移和方向的计算，若满足三指截屏条件，则分发给PhoneWindowManager，由PhoneWindowManager接管后会调用截屏或者超级截屏接口

同时，为了功能的可配置性，我们添加了设置存储，以便用户能方便的开关此功能

## 三、实现清单

###### 1、嵌入代码监听触摸行为
###### keyword: "Three finger snapshot"
- [DecorView.java]

frameworks\base\core\java\com\android\internal\policy

###### 2、触摸行为的计算和分发
- [FreemeInterceptHandler.java]

vendor\freeme\frameworks\core-export\java\com\freeme\supershot

###### 3、存储声明
###### keyword: "Three finger snapshot"
- [FreemeSettings.java]

vendor\freeme\frameworks\base\core-export\java\com\freeme\provider

###### 4、默认值设置
###### keyword: "Three finger snapshot"
- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

###### 5、默认值读取
###### keyword: "Three finger snapshot"
- [FreemeProvidersUtils.java]

frameworks\base\packages\SettingsProvider\src\com\freeme\providers\settings

###### 6、设置类，根据宏开关，控制是否显示该设置项
###### keyword: "Three finger snapshot"
- [FreemeGestureOperate.java]

packages\apps\Settings\src\com\freeme\settings\intelligence

###### 7、UI描述，对该设置Preference进行声明
###### keyword: "Three finger snapshot"

- [freeme_gesture_operate.xml]
- [freeme_settings_intelligence_assistant.xml]

packages\apps\Settings\res-freeme\xml

###### 8、多语言
- [strings_freeme.xml]

packages\apps\Settings\res-freeme\values

packages\apps\Settings\res-freeme\values-zh-rCN

packages\apps\Settings\res-freeme\values-zh-rTW

###### 9、添加控制接口, 设置和获取当前是否处理三指截屏模式
```
void setSwipeActionUseThreeFingerMode(boolean enable);
boolean isSwipeActionUseThreeFingersEnable();
```
###### keyword: "Three finger snapshot"

- [IWindowManager.aidl]

frameworks\base\core\java\android\view

###### 10、接口实现
###### keyword: "Three finger snapshot"
- [WindowManagerService.java]

frameworks\base\services\core\java\com\android\server\wm

###### 11、事件监测，并调用截屏或者超级截屏
###### keyword: "Three finger snapshot"
- [PhoneWindowManager.java]

frameworks\base\services\core\java\com\android\server\policy

###### 12、为Java世界提供宏开关，在宏关闭的状态下，原则上不影响原生代码的任何行为

```
Public Static final boolean FREEME_THREE_POINTER_TAKE_SCREEN_SHOT = getBoolean("ro.freeme.three_p_screen_shot");
```

- [FreemeOption.java]

Vendor/freeme/frameworks/base/core-export/java/com/freeme/util

###### 13、配置开关，决定是否开启该功能

```
FREEME_THREE_POINTER_TAKE_SCREEN_SHOT=yes
```

- [ProjectConfig.mk]

device/droi/common

###### 14、读取ProjectConfig.mk中的值，写入build.prop中，开发调试时，可手动修改build.prop中的相应ro值来开启和关闭功能

```
ifeq ($(strip $(FREEME_THREE_POINTER_TAKE_SCREEN_SHOT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.three_p_screen_shot=1
endif
```

- [device.mk]

device/droi/common

## 四、项目配置
###### ==1、项目配置时，请根据需要需要开启，yes为开启，no为关闭，请在相应的项目配置文件中override该属性==

```
FREEME_THREE_POINTER_TAKE_SCREEN_SHOT=yes
```

- [ProjectConfig.mk]

device/droi/project_name

###### ==2、唤醒行为默认值设置，配置时，请在相应的项目文件夹中重写该文件==
###### keyword: "Three finger snapshot"

- 开关配置，false为关闭，true为开启

```
<integer name="def_three_pointer_take_screen_shot" translatable="false">true</integer>
```

- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

## 五、总结
- 该功能牵涉的改动文件比较多，移植时请注意遗漏
