LOCAL_PATH:= $(call my-dir)

# Build the Telecom service.
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-telecom-common
LOCAL_JAVA_LIBRARIES += voip-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-base
# Plugin need OperatorCustomizationFactoryLoader
LOCAL_JAVA_LIBRARIES += mediatek-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_STATIC_JAVA_LIBRARIES := com.mediatek.server.telecom.ext

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-proto-files-under, proto)
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res_ext

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTOC_FLAGS := --proto_path=$(LOCAL_PATH)/proto/
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := optional_field_style=accessors

# @{ freeme.zhaozehong, 20180115. freeme source
include packages/apps/PhoneCommon/freeme/common/freeme_res.mk
include vendor/mediatek/proprietary/packages/services/Telecomm/freeme/freeme.mk
# @}

LOCAL_PACKAGE_NAME := MtkTelecom
LOCAL_OVERRIDES_PACKAGES := Telecom

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

# Build the test package.
include $(call all-makefiles-under,$(LOCAL_PATH))
