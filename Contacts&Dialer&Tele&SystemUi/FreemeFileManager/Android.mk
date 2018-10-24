LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_PACKAGE_NAME := FreemeFileManager

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    src/com/freeme/safe/encryption/service/IEncryptionService.aidl

LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-v13 \
    freeme-support-design
LOCAL_MODULE_TAGS := optional

# mtk
LOCAL_OVERRIDES_PACKAGES := FileManager
# sprd
LOCAL_OVERRIDES_PACKAGES += FileExplorer FileExplorerDRMAddon

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
