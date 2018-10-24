LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PACKAGE_NAME := FreemeOneHand

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    $(call all-java-files-under, settings_common/src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/settings_common/res

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.settings

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_ENABLED := nosystem
LOCAL_PROGUARD_FLAGS := -ignorewarnings -include build/core/proguard_basic_keeps.flags
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)

include $(call all-makefiles-under, $(LOCAL_PATH))
