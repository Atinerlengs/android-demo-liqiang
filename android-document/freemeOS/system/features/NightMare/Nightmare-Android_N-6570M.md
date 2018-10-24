## 一、目标定义
在设备运行一段时间后，可能存在诸如卡顿、后台运行环境糟糕、系统内存泄漏、累计清理不净的问题。那么可以 通过重置软件运行时环境（重启），以达到提升设备平均性能的目标。
同时，还要满足不易被用户发觉，并且没有轻易影响功耗的要求。

## 二、概要设计
Nightmare所重视和强调的是，在提高手机平均性能的同时，也不易被用户察觉。所以对于重启的时机和方式有较 高的要求。
根据目标定义，系统设计概括为：在手机没有被（用户）使用一段时间后，并且手机仍然没有在被（用户）使用的 情况下，重启手机；最小重启周期限制。

- 1. 没有被（用户）使用一段时间；

为什么不是夜间某个时刻？因为面向的用户群没有限定。既要适用于正常作息的人群，还要被特殊作息 习惯的人群所接受； 为什么是一段时间？这是一个简易笼统地描述用户在这段时间内，并且可能在未来一段时间内不需要使 用手机的可能性数据参数；如果需要更加精确，那么需要更多的模型参数及算法。

- 2. 没有在被（用户）使用； 如果用户正在“操作使用”，是肯定不能被重启的

- 3. 最小重启周期限制；

智能手机系统发生运行时卡顿的平均周期。既不能过长，也不能过短，倾向于偏保守的短周期； 尽量少地影响用户体验（不易被用户察觉、不影响系统及应用运行的连续性）；


## 三、实现清单

##### 1、服务实现

- [Android.mk]

    vendor/freeme/frameworks/base

- [INightmareManager.aidl]
- [INightmareManager.aidl]

    vendor/freeme/frameworks/base/core/java/com/freeme/os/

- [NightmareManagerInternal.java]
- [NightmareManagerService.java]
- [Tracer.java]

    vendor/freeme/frameworks/base/services/core-export/java/com/freeme/server/nightmare

##### 2、服务启动
###### keyword: "Nightmare"

```
if (com.freeme.util.FreemeOption.FREEME_NIGHTMARE_SUPPORT) {
    mSystemServiceManager.startService(com.freeme.server.nightmare.NightmareManagerService.class);
}
```

- [SystemServer.java]

    frameworks/base/services/java/com/android/server

##### 3、毛刺引入
###### keyword: "Nightmare"

```
//*/ freeme.biantao, 20170315. Nightmare.
com.freeme.server.nightmare.NightmareManagerInternal.onStruggle(true);
//*/
```

- [BatteryStatsService.java]

    frameworks/base/services/core/java/com/android/server/am

##### 4、SIM卡锁跳过
###### keyword: "Nightmare"

- [KeyguardSimPinPukMeView.java]

    frameworks/base/packages/Keyguard/src/com/mediatek/keyguard/Telephony

- [IccLockSettings.java]

    packages/apps/Settings/src/com/android/settingss

##### 5、UserActivity完善
###### keyword: "Nightmare"

```
//*/ freeme.biantao, 20170315. Nightmare.
if ((result & ACTION_PASS_TO_USER) == 0) {
    com.freeme.server.nightmare.NightmareManagerInternal.onUserActivity();
}
//*/
```

- [PhoneWindowManager.java]
    frameworks/base/services/core/java/com/android/server/policy/

##### 6、SEPolicy
###### keyword: "Nightmare"

- [app.te]
- [service.te]
- [service_contexts]

    device/droi/common/sepolicy

##### 7、定义宏开关

```
public static final boolean FREEME_NIGHTMARE_SUPPORT = getBoolean("ro.freeme.nightmare");
```

- [FreemeOption.java]

    vendor/freeme/frameworks/base/core-export/java/com/freeme/util

##### 8、配置宏开关

```
FREEME_NIGHTMARE_SUPPORT=no
```

- [ProjectConfig.mk]

    device/droi/common

##### 9、读取宏开关

```
ifeq ($(strip $(FREEME_NIGHTMARE_SUPPORT)),yes)
  FREEME_PRODUCT_PROPERTY_OVERRIDES += ro.freeme.nightmare=1
endif
```

- [device.mk]

    device/droi/common

## 四、项目配置
###### ==1、项目配置时，请根据需要需要开启，yes为开启，no为关闭，请在相应的项目配置文件中override该属性==

```
FREEME_NIGHTMARE_SUPPORT=no
```

- [ProjectConfig.mk]

    device/droi/project_name

###### 2、Reference
repo init ... -m ALPS-MP-M0.MP23-V1.32.3_DROI6570_CTLC_M/pcb_oversea.xml

## 五、总结

- 该功能牵涉的改动文件比较多，移植时请注意遗漏
