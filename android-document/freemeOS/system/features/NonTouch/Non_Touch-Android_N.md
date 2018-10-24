## 一、功能定义
浮空操作即当手掌从手机顶部上方（3cm-5cm）处拂过时，调用相应的功能，包括隔空解锁、隔空操作相册、隔空切换待机界面、隔空操作视频、隔空操作音乐、隔空操作来电（静音或者免提）

## 二、概要描述
应用中根据需求，直接注册SensorManager，监听Sensor.Type_GESTURE，并根据回调调用相应功能

同时，为了功能的可配置性，我们添加了设置存储，以便用户能方便的开关此功能

## 三、实现清单

###### 1、设置类，用于用户浮空操作的使能

- [NonTouchOperationSettings.java]

packages\apps\Settings\src\com\freeme\settings\motion

###### 2、设置类，根据功能开关决定是否移除或添加该设置项
###### keyword: "non-touch operation"

- [FreemeIntelligenceAssistant.java]

packages\apps\Settings\src\com\freeme\settings

###### 3、UI描述，对该设置Preference进行声明
###### keyword: "non-touch operation"

- [freeme_settings_intelligence_assistant.xml]
- [freeme_non_touch_operation.xml]

packages\apps\Settings\res-freeme\xml

###### 4、多语言

- [strings_freeme.xml]

packages\apps\Settings\res-freeme\values

packages\apps\Settings\res-freeme\values-zh-rCN

packages\apps\Settings\res-freeme\values-zh-rTW

###### 5、存储声明，包含四个子功能的使能和十四个模拟按键功能的行为描述
###### keyword: "non-touch operation"

- [FreemeSettings.java]

vendor\freeme\frameworks\base\core-export\java\com\freeme\provider

###### 6、默认值设置
###### Keyword: "NavigationBar Show/Hide"

- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\values

###### 7、默认值读取
###### keyword: "NavigationBar Show/Hide"

- [FreemeProvidersUtils.java]

frameworks\base\packages\SettingsProvider\src\com\freeme\providers\settings

###### 8、 为Java世界提供宏开关，在宏关闭的状态下，原则上不影响原生代码的任何行为

```
Public Static final boolean FREEME_NON_TOUCH_OPEARTION_SUPPORT = getBoolean("ro.freeme.non_touch_operation");
```

- [FreemeOption.java]

Vendor/freeme/frameworks/base/core-export/java/com/freeme/util

###### 9、配置开关，决定是否开启该功能

```
FREEME_NON_TOUCH_OPEARTION=yes
```
- [ProjectConfig.mk]

device/droi/common

###### 10、读取ProjectConfig.mk中的值，写入build.prop中，开发调试时，可手动修改build.prop中的相应ro值来开启和关闭功能

```
ifeq ($(strip $(FREEME_NON_TOUCH_OPEARTION)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.non_touch_operation=1
endif
```

- [device.mk]

device/droi/common

## 四、项目配置
###### ==1、项目配置时，请根据需要需要开启，yes为开启，no为关闭，请在相应的项目配置文件中override该属性==

```
FREEME_NON_TOUCH_OPEARTION=yes
```
- [ProjectConfig.mk]

device/droi/project_name

###### ==2、默认值设置，配置时，请在相应的项目文件夹中重写该文件==
###### keyword: "NavigationBar Show/Hide"

- 0 表示所有开关都是关闭，如若要设置某个子项开启，需要将对应的位 置为1，请注意位运算
- 例如设置为4（100），则表示将FREEME_GESTURE_LOCKSCR_UNLOCK开启

```
public static final int FREEME_GESTURE_LOCKSCR_UNLOCK = 1 << 2;
```

```
<bool name="def_non_touch_operation_setting" translatable="false">0</bool>
```

- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

## 五、总结

- 该功能牵涉的改动文件比较多，移植时请注意遗漏
- ==默认值配置方式与其他不同，请特别注意子项设置使用移位运算，不明白请查阅相关基础知识==
