LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.freeme.systemui.ext
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += mediatek-common

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SRC_FILES += \
         ../src/com/mediatek/systemui/statusbar/util/FeatureOptions.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/IconIdWrapper.java \


include $(BUILD_STATIC_JAVA_LIBRARY)
