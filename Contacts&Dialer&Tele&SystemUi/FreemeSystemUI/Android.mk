LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := Freeme-SystemUI-proto

LOCAL_SRC_FILES := $(call all-proto-files-under,src)

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := optional_field_style=accessors

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := Freeme-SystemUI-tags

LOCAL_SRC_FILES := src/com/android/systemui/EventLogTags.logtags

include $(BUILD_STATIC_JAVA_LIBRARY)

# ------------------

include $(CLEAR_VARS)

LOCAL_USE_AAPT2 := true

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-Iaidl-files-under, src)

ifeq ($(strip $(MTK_VOW_SUPPORT)),yes)
    LOCAL_SRC_FILES += $(call all-java-files-under, src_ext)
endif

LOCAL_STATIC_ANDROID_LIBRARIES := \
    Freeme-SystemUIPluginLib \
    android-support-v4 \
    android-support-v7-recyclerview \
    android-support-v7-preference \
    android-support-v7-appcompat \
    android-support-v7-mediarouter \
    android-support-v7-palette \
    android-support-v14-preference \
    android-support-v17-leanback

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.freeme.systemui.ext \
    com.freeme.keyguard.ext \
    framework-protos \
    Freeme-SystemUI-tags \
    Freeme-SystemUI-proto

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += android.car
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_JAVA_LIBRARIES += mediatek-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-base
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common

LOCAL_PACKAGE_NAME := FreemeSystemUI
LOCAL_OVERRIDES_PACKAGES := SystemUI MtkSystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res-keyguard \
                      $(LOCAL_PATH)/res \
                      $(LOCAL_PATH)/res_ext \
                      $(LOCAL_PATH)/res-keyguard_ext

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_DX_FLAGS := --multi-dex
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/base/packages/SettingsLib/common.mk

LOCAL_AAPT_FLAGS := --extra-packages com.android.keyguard

# @{ freeme.gouzhouping, 20180115. Freeme.
include $(LOCAL_PATH)/freeme/common_freeme.mk

LOCAL_STATIC_ANDROID_LIBRARIES += faceprint-authenui-library
# @}
include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
