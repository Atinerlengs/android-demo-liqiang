# 关于预置APK的相关说明

[TOC]

## 1. 简介

**本文档介绍如何配置预置APK的`Android.mk`，另外特申明所有预置应用禁止使用`platform`签名， 特殊需求除外**

## 2. 配置应用不可卸载

#### 模板

位置： **`vendor/freeme/samples/Android.mk-sample`**

```shell
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := sample
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS := APPS
LOCAL_DEX_PREOPT := true
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_PREBUILT)
```

#### 步骤

1. 是否需要提取 `odex`？

   ```shell
       LOCAL_DEX_PREOPT := true
   ```

2. 是否放置于 `priv-app` ？

   ```shell
   LOCAL_PRIVILEGED_MODULE := true
   ```

3. 是否存在 `so`库？

   ```shell
   # 指定编译目标为 32位 或 64位
   LOCAL_MULTILIB := ###可选值 /32/64/both
   ```

   不建议提取, 如运行出错( ***FAQ：无法找到库导致应用无法启动*** )，则提取文件并添加配置

   ```shell
   # 编译32位库文件
   LOCAL_PREBUILT_JNI_LIBS_arm := $(subst $(LOCAL_PATH)/,,$(wildcard $(LOCAL_PATH)/lib/armeabi/*.so))
   # 编译64位库文件
   LOCAL_PREBUILT_JNI_LIBS_arm64 := $(subst $(LOCAL_PATH)/,,$(wildcard $(LOCAL_PATH)/lib/arm64-v8a/*.so))
   ```

## 3. 配置应用可卸载
#### 模板

位置： **`vendor/freeme/samples/Android.mk-sample`**

```shell
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := sample
LOCAL_MODULE_TAGS := optional
LOCAL_DEX_PREOPT := false
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_PATH := $(TARGET_OUT)/vendor/operator/app
LOCAL_PROPRIETARY_MODULE := true
include $(BUILD_PREBUILT)
```
#### 步骤

1. 是否存在 `so` 库？ 配置方法如上
2. 是否需要提取 `odex` ？配置方法如上
3. 如遇到应用无法安装或启动，则参考 ***FAQ***

## FAQ

1. **无法找到库导致应用无法启动**

   - 现象

     通过`adb logcat`, 可以发现以下错误信息

     ```verilog
     Process: com.skype.raider, PID: 5326
     E AndroidRuntime: java.lang.UnsatisfiedLinkError: dalvik.system.PathClassLoader[DexPathListlib] couldn't find "libSkypeAndroid.so"
     ```

   - 解决方法

     - 如应用不可卸载，则需要提取so库，将应用中的`lib`提取出来放在与应用同级目录，并在`Android.mk`中配置对应项
     - 如应用可卸载则建议配置为不可卸载

2. **V2签名导致应用无法安装**

   - 现象
     - 应用可卸载

     - Android 版本为 N 及以上

     - 使用附件的`checkV2Sign.sh` 检测为`V2`签名[(APK Signature Scheme v2官方介绍)](http://blog.bihe0832.com/android-v2-signature.html)，如：

       ```shell
       $./checkV2Sign.sh test.apk
        {"ret":0,"msg":"ok","isV2":true,"isV2OK":true} # 如字段isV2 为true,则该应用为V2签名
       ```

   - 解决方法

     在 `Android.mk` 中添加

     ```shell
     LOCAL_REPLACE_PREBUILT_APK_INSTALLED :=$(LOCAL_PATH)/$(LOCAL_MODULE).apk
     ```
