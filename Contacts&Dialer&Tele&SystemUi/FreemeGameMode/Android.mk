LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := FreemeGameMode

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    $(call all-java-files-under, settings_common/src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/settings_common/res

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.settings

LOCAL_CERTIFICATE := platform

# disabled nosystem
LOCAL_PROGUARD_ENABLED := nosystem
LOCAL_PROGUARD_FLAGS := -ignorewarnings -include build/core/proguard_basic_keeps.flags
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)