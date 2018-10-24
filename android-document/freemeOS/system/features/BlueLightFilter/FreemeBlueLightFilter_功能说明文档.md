# 蓝光过滤

## 一、目标定义

用户使用过程中，屏幕颜色无法调整，导致在查看屏幕或阅读的过程中，易造成用户的用眼疲劳。蓝光过滤能根据时间自动调整屏幕使其白天明亮，夜间温和（过滤对眼睛有害的蓝光部分），进而增强用户使用体验。

## 二、功能说明

蓝光过滤能根据设定的时间，在开机的情况下自动打开或者关闭功能。目前蓝光过滤一共提供以下三种模式，方便用户自定义需求。

- 日落日出模式：根据`GPS` 定位，自动校准开始及结束时间，譬如调整前（晚上7点~早上7点），调整后（晚上6点半到早上6点半），更好的贴近用户，增强体验。

- 自定义时间模式：用户自定义开始及结束时间，因为考虑到部分用户可能需要更早的进入或者退出蓝光过滤模式。

- 强制打开模式：用户强制打开蓝光过滤模式，不考虑时间因素

> 注：
>
> - 日落日出需要定位功能（GPS）打开，否则无法进行定位校准时间，默认时间为（晚上7点~早上7点）
> - 日落日出及自定义时间模式优先级大于强制打开模式（即：日落日出模式打开后，手动进入强制打开模式，此时仍然受限于日出日落时间，即到达结束时间，模式依然自动关闭）
> - 强制打开模式下，重启后需要用户重新打开

## 三、实现细节

蓝光过滤在`android7.1`后系统自带，只是没有亮度调节功能，所以在7.1.1上只是添加调节功能。而`android7.0`上自己实现

- `android 7.0`

  - 功能实现代码：`FreemeOS/common/apps/FreemeBluelightFilter`

  - 配置文件：`freeme_bluelight_filter.properties` 相关配置如下：

    ```
    # 手动调度开关
    # - 0, off
    # - 1, on
    config.bluefilter.enable=0
    # 自动调度开关
    # - 0, off
    # - 1, on
    config.bluefilter.scheduled=1
    # 自动调度类型（用户自定义时间或者日落日出时间）
    # - 0, off
    # - 1, user scheduled
    # - 2, auto scheduled [ need System def_location_providers_allowed != "" or 0 ]
    config.bluefilter.type=2
    # User scheduled Timer start time
    config.bluefilter.time.on=1140
    # User scheduled Timer end time
    config.bluefilter.time.off=420
    # Auto scheduled Timer start time
    config.bluefilter.automatic.time.on=1140
    # Auto scheduled Timer end time
    config.bluefilter.automatic.time.off=420
    # 默认蓝光值
    config.bluefilter.opacity=200
    ```

- `android 7.1`

  ```xml
  核心实现代码
  frameworks/base
              |--core/java/com/android/internal/app/NightDisplayController.java
  				（控制类，包括监听设置的变化）
              |--services/core/java/com/android/server/display/NightDisplayService.java 
  				（实现服务，随系统一起启动，常驻system_server中）
              |--packages/SystemUI/src/com/android/systemui/qs/tiles/NightDisplayTile.java
  			        （Qs中tile实现）

  packages/apps/Settings/src/com/android/settings/display/NightDisplaySettings.java 
  （设置入口）

  配置开关：
  frameworks/base/core/res/res/values/config.xml
  其中 config_nightDisplayAvailable （系统中总的控制宏，包括service注册及systemui tile 和settings入口）
  ```
