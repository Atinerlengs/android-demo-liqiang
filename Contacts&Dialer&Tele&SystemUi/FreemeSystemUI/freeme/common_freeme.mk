# Freeme for SystemUI

LOCAL_RESOURCE_DIR += \
    $(LOCAL_PATH)/freeme/res \
    $(LOCAL_PATH)/freeme/recents/res

LOCAL_SRC_FILES += \
    $(call all-java-files-under, freeme/src) \
    $(call all-java-files-under, freeme/recents/src)

LOCAL_FULL_LIBS_MANIFEST_FILES += \
    $(LOCAL_PATH)/freeme/recents/AndroidManifest.xml \
    $(LOCAL_PATH)/freeme/AndroidManifest.xml