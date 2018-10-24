# PowerGuru 可配置项

##ro.freeme.powerguru

>配置 对齐方案是否 开启（true / false）

## config_powerguru_alarm_defInterval = 5

>默认对齐时间是 5 分钟为间隔 (该数据是长时间实用中得到的数据值)
>例如：对齐时间是12:05 , 12：10.......以此类推，新的alarm 会被对齐到最近的一个后一个5 分钟的整数倍（如12:04 会被对齐到12:05）
>
>现在是代码中写死了，**可以考虑是否需要配置prop属性值**
>


## ALARM_SCEDULE_DETECT_DURATION_DEFAULT = 60 * MILLIS_PER_MINUTE //60 minuteis

> 默认的自学习时间段，如判断 60分钟内如果某alarm 的设置同样的action 达到5 次，即认为该应用可能是频繁请求alarm服务，造成功耗问题，需要把该应用对应的Action 等相关信息添加到对齐列表中

* 注：请同如下两个属性一同理解

## config_powerguru_alarm_repeatInterval_level1 = 15; //15 minutes

>DEFAULT_ALARM_DETECT_DURATION  /DEFAULT_ALARM_REPEAT_INTERVAL_MINS_LEVEL1 = 4 ,如果1小时内一个应用设置同一个Action的alarm超过4 次就添加到对齐列表中

## config_powerguru_alarm_repeatInterval_level2 = 10; //10 minutes

>如果该重复Alarm 的repeatInterval 小于 10，即添加到对齐列表中。


















