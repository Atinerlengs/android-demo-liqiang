# VibratorTuner特性开发

## 1 Overview

VibratorTuner(震动调节)旨在动态调节系统振动强度。

这篇文档的目标是规范统一Freeme Android M中关于震动调节各层次开发事项。

## 2 Integration

集成参考：

```sh
# v9c81q 的振动调节集成方法：

# 对应项目 kernel driver
- kernel-3.18/drivers/misc/mediatek/vibrator/vibrator_drv.c 

# 对应项目预编译二级制文件布置
- hardware/libhardware_legacy/
	|- include/hardware_legacy/vibrator.h
	|- vibrator/vibrator.c
	
# 修改sys文件节点权限
-  vendor/droi/freeme/prebuilt/etc/init.freeme.rc

# 修改 framework 
- frameworks/base/
	|- frameworks/base/services/java/com/android/server/VibratorService.java
	|- frameworks/base/services/jni/com_android_server_VibratorServce.cpp
	|- frameworks/base/packages/SettingsProvider/src/com/android/providers/settings/DatabaseHelper.java
	|- frameworks/base/packages/SettingsProvider/res/values/defaults.xml
	|- frameworks/base/core/java/android/provider/Settings.java

# 修改Settings
	|- packages/apps/Settings
```

## 3 Other

其他问题在开发过程中将不断完善。



