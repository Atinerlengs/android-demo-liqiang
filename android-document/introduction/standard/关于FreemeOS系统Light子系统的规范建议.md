# 关于FreemeOS系统Light子系统的规范建议

[TOC]

### Android现状

##### Android 7.0 以前

App中只要调用 `Notification.Builder.setLights(int argb, int onMs, int offMs);` 

即可使各自的Notification带有light效果；关于该方法的说明：

- `Parameter0(argb)`：三色灯颜色，可以用RGB格式的数字表示，比如红色（`0xff0000`）、蓝色（`0x0000ff`）、紫色（`0xff00ff`）；


- `Parameter0(onMs)` ：灯亮时间，单位毫秒；


- `Parameter0(offMs)`：灯灭时间，单位毫秒；

通过以上仅有的三个参数，可以发现我们需要的功能绝大多数皆可满足了。改色？没问题。闪烁？也没问题。

##### Android 7.0 以后

**新增限制**：如果通知中不包含***振动或者提示音***，则默认不进行闪烁（即使配置`setLights`），因为系统认为没有振动或者声音的通知无需打扰用户即不点亮信号灯。

增加默认振动或声音方法如下：

```
notification.defaults |= Notification.DEFAULT_SOUND;
notification.defaults |= Notification.DEFAULT_VIBRATE;
```

### 平台适配

按照Google原生Android设计，其实只需要按照标准做就OK了：

驱动/内核层配置：

  参考 vendor/mediatek/proprietary/hardware/liblights/lights.c MTK的默认配置

```
/* RED LED */
char const*const RED_LED_FILE = "/sys/class/leds/red/brightness";
char const*const RED_TRIGGER_FILE = "/sys/class/leds/red/trigger";
char const*const RED_DELAY_ON_FILE = "/sys/class/leds/red/delay_on";
char const*const RED_DELAY_OFF_FILE = "/sys/class/leds/red/delay_off";

/* GREEN LED */
char const*const GREEN_LED_FILE = "/sys/class/leds/green/brightness";
char const*const GREEN_TRIGGER_FILE = "/sys/class/leds/green/trigger";
char const*const GREEN_DELAY_ON_FILE = "/sys/class/leds/green/delay_on";
char const*const GREEN_DELAY_OFF_FILE = "/sys/class/leds/green/delay_off";

/* BLUE LED */
char const*const BLUE_LED_FILE = "/sys/class/leds/blue/brightness";
char const*const BLUE_TRIGGER_FILE = "/sys/class/leds/blue/trigger";
char const*const BLUE_DELAY_ON_FILE = "/sys/class/leds/blue/delay_on";
char const*const BLUE_DELAY_OFF_FILE = "/sys/class/leds/blue/delay_off";
```

  需要针对**点亮/闪烁**完成以上kernel接口。

> 备注：trigger 接口须支持 timer属性（默认支持），测试方法见 [MT6323/MT6322 ISINK闪烁时间配置](https://onlinesso.mediatek.com/FAQ#/SW/FAQ11519)

### 客制化选项

android 系统提供以下接口配置闪烁周期及灯颜色等：

```
frameworks/base/core/res/res/values/config.xml
	<!-- 默认提示灯颜色. 默认值：白色 -->
    <color name="config_defaultNotificationColor">#ffffffff</color>

    <!-- 默认呼吸灯- 亮时间 -->
    <integer name="config_defaultNotificationLedOn">500</integer>

    <!-- 默认呼吸灯- 灭时间 -->
    <integer name="config_defaultNotificationLedOff">2000</integer>

    <!-- 默认低电量下显示颜色. 红色 -->
    <integer name="config_notificationsBatteryLowARGB">0xFFFF0000</integer>

     <!-- 默认充电过程中显示颜色. 黄色 -->
    <integer name="config_notificationsBatteryMediumARGB">0xFFFFFF00</integer>

    <!-- 默认充满电显示颜色. 绿色 -->
    <integer name="config_notificationsBatteryFullARGB">0xFF00FF00</integer>

    <!-- 默认低电量下闪烁-亮时间 -->
    <integer name="config_notificationsBatteryLedOn">125</integer>

    <!-- 默认低电量下闪烁-灭时间 -->
    <integer name="config_notificationsBatteryLedOff">2875</integer>

    <!-- 设置中呼吸灯设置入口 -->
    <bool name="config_intrusiveNotificationLed">true</bool>

frameworks/base/packages/SettingsProvider/res/values/defaults.xml
	<!-- 通知呼吸灯开关 -->
	<bool name="def_notification_pulse">true</bool>
```