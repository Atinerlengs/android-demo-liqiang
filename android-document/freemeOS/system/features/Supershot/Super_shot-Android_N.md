## 一、功能定义
超级截屏为我们自研应用，含长截屏、趣味截屏等扩展功能，可对截屏进行裁剪和编辑

## 二、概要描述
框架层判断超级截屏开关是否开启，若开启，将使用超级截屏取代系统自带截屏

同时，为了功能的可配置性，我们添加了设置存储，以便用户能方便的开关此功能

## 三、实现清单

###### 1、存储声明
###### keyword: "Supershot"
- [FreemeSettings.java]

vendor\freeme\frameworks\base\core-export\java\com\freeme\provider

###### 2、默认值设置
###### keyword: "Supershot"
- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

###### 3、默认值读取
###### keyword: "Supershot"
- [FreemeProvidersUtils.java]

frameworks\base\packages\SettingsProvider\src\com\freeme\providers\settings

###### 4、设置类，根据宏开关，控制是否显示该设置项
###### keyword: "Supershot"
- [FreemeIntelligenceAssistant.java]

packages\apps\Settings\src\com\freeme\settings\intelligence

###### 5、事件监测，并调用超级截屏
###### keyword: "SuperShot"
- [PhoneWindowManager.java]

frameworks\base\services\core\java\com\android\server\policy

###### 6、为Java世界提供宏开关，在宏关闭的状态下，原则上不影响原生代码的任何行为

```
Public Static final boolean FREEME_SUPER_SHOT = getBoolean("ro.freeme.super_shot");
```

- [FreemeOption.java]

Vendor/freeme/frameworks/base/core-export/java/com/freeme/util

###### 7、配置开关，决定是否开启该功能，请注意两个需要同时开启

```
FREEME_SUPER_SHOT_SUPPORT=yes
```

- [ProjectConfig.mk]

device/droi/common

###### 8、读取ProjectConfig.mk中的值，写入build.prop中，开发调试时，可手动修改build.prop中的相应ro值来开启和关闭功能

```
ifeq ($(strip $(FREEME_SUPER_SHOT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.super_shot=1
endif
```

- [device.mk]

device/droi/common

## 四、项目配置
###### ==1、项目配置时，请根据需要需要开启，yes为开启，no为关闭，请在相应的项目配置文件中override该属性==

```
FREEME_SUPER_SHOT_SUPPORT=yes
```

- [ProjectConfig.mk]

device/droi/project_name

###### ==2、默认值设置，配置时，请在相应的项目文件夹中重写该文件==
###### keyword: "SuperShot"
- 开关配置，false为关闭，true为开启

```
<integer name="def_supershot_mode_default" translatable="false">true</integer>
```

- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

## 五、总结
- 该功能牵涉的改动文件比较多，移植时请注意遗漏
