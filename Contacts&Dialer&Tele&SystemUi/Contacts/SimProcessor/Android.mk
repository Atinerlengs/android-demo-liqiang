LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

ext_dir := ../ext
src_dirs := src $(ext_dir)/src
LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
# {@ freeme.zhaozehong, 20180328. add freeme resource
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/freeme/res
# @}

LOCAL_PACKAGE_NAME := MtkSimProcessor
LOCAL_CERTIFICATE := platform
LOCAL_APK_LIBRARIES += Contacts
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := SimProcessor

LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common mediatek-telephony-base
LOCAL_JAVA_LIBRARIES += mediatek-common

LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
