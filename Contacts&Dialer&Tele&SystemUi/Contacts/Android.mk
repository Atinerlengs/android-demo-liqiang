LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
ifeq ($(strip $(MTK_TB_WIFI_3G_MODE)), WIFI_ONLY)
    LOCAL_MANIFEST_FILE := wifionly/AndroidManifest.xml
endif
LOCAL_MODULE_TAGS := optional

phone_common_dir := ../../../../../../packages/apps/PhoneCommon

ifeq ($(TARGET_BUILD_APPS),)
support_library_root_dir := frameworks/support
else
support_library_root_dir := prebuilts/sdk/current/support
endif

# M: add ext for Plugin interface
src_dirs := src src-bind $(phone_common_dir)/src ext
res_dirs := res $(phone_common_dir)/res
asset_dirs := assets

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs)) \
    $(support_library_root_dir)/design/res \
    $(support_library_root_dir)/transition/res \
    $(support_library_root_dir)/v7/appcompat/res \
    $(support_library_root_dir)/v7/cardview/res \
    $(support_library_root_dir)/v7/recyclerview/res
# M: add for MTK resource
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res_ext
LOCAL_ASSET_DIR := $(addprefix $(LOCAL_PATH)/, $(asset_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.contacts.common \
    --extra-packages com.android.phone.common \
    --extra-packages android.support.design \
    --extra-packages android.support.transition \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.cardview \
    --extra-packages android.support.v7.recyclerview

# M: for using framework @{
LOCAL_JAVA_LIBRARIES := telephony-common voip-common
LOCAL_JAVA_LIBRARIES += framework
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += mediatek-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-base
LOCAL_JAVA_LIBRARIES += mediatek-telecom-common
# @}

LOCAL_STATIC_JAVA_LIBRARIES := \
    android-common \
    android-support-design \
    android-support-transition \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-recyclerview \
    android-support-v7-palette \
    android-support-v4 \
    com.android.vcard.mtk \
    guava \
    libphonenumber

# @{ freeme.zhaozehong, 20180115. freeme source
include packages/apps/PhoneCommon/freeme/common/freeme.mk
include packages/apps/PhoneCommon/freeme/phone/freeme.mk
include vendor/mediatek/proprietary/packages/apps/Contacts/freeme/freeme.mk
include vendor/freeme/frameworks/support/v7/appcompat/common.mk
LOCAL_USE_AAPT2 := true
# @}

LOCAL_PACKAGE_NAME := MtkContacts
LOCAL_CERTIFICATE := shared
LOCAL_PRIVILEGED_MODULE := true
LOCAL_OVERRIDES_PACKAGES := Contacts

LOCAL_PROGUARD_FLAG_FILES := \
    proguard.flags \
    ../../../../../../frameworks/support/core-ui/proguard-rules.pro \
    ../../../../../../frameworks/support/design/proguard-rules.pro \
    ../../../../../../frameworks/support/v7/recyclerview/proguard-rules.pro

# M: mark for using hide API @{
#LOCAL_SDK_VERSION := current
# @}
LOCAL_MIN_SDK_VERSION := 21

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
