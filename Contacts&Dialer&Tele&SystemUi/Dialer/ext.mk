# Mediatek add ext res and libs.
EXT_RES_DIRS += \
    ext/incallui/res \
    ext/dialer/res \
    ext/contactscommon/res \
    java/com/mediatek/dialer/search/res \
    java/com/mediatek/dialer/calllog/res \
    java/com/mediatek/incallui/volte/res \
    java/com/mediatek/incallui/dsda/res \
    java/com/mediatek/incallui/blindect/res

# M: Vilte project not support multi-window @{
$(info Vilte $(MTK_VILTE_SUPPORT), 3GVT $(MTK_VT3G324M_SUPPORT))
ifeq (yes, $(filter yes, $(strip $(MTK_VILTE_SUPPORT)) $(strip $(MTK_VT3G324M_SUPPORT))))
EXT_RES_DIRS += ext/incallui/vt_config/disable
$(info disable multi-window for InCallUi $(res_dirs))
else
EXT_RES_DIRS += ext/incallui/vt_config/enable
$(info enabl multi-window for InCallUi $(res_dirs))
endif
# @}

# M: [InCallUI]needed by AddMemberEditView who extends MTKRecipientEditTextView
# M: [InCallUI]FIXME: should replace this with google default RecipientEditTextView
LOCAL_RESOURCE_DIR += \
    $(addprefix $(LOCAL_PATH)/, $(EXT_RES_DIRS)) \
   vendor/mediatek/proprietary/frameworks/ex/chips/res

EXT_SRC_DIRS := \
    java/com/mediatek
LOCAL_SRC_FILES += $(call all-java-files-under, $(EXT_SRC_DIRS))

LOCAL_JAVA_LIBRARIES += mediatek-telecom-common mediatek-common mediatek-telephony-base\
    ims-common \


LOCAL_STATIC_JAVA_LIBRARIES += libchips com.mediatek.incallui.ext.vendor \
    com.mediatek.dialer.ext.vendor \
    com.mediatek.tatf.common\
    wfo-common \
    android-common-chips \

EXT_DIALER_MANIFEST_FILES += \
    java/com/mediatek/dialer/calllog/AndroidManifest.xml \
    java/com/mediatek/dialer/sos/AndroidManifest.xml \
    java/com/mediatek/incallui/volte/AndroidManifest.xml \
    java/com/mediatek/incallui/dsda/AndroidManifest.xml \
    java/com/mediatek/incallui/blindect/AndroidManifest.xml \
    java/com/mediatek/incallui/tatf/AndroidManifest.xml\
    java/com/mediatek/incallui/wfc/AndroidManifest.xml \

LOCAL_FULL_LIBS_MANIFEST_FILES += \
    $(addprefix $(LOCAL_PATH)/, $(EXT_DIALER_MANIFEST_FILES))

LOCAL_AAPT_FLAGS += \
    --extra-packages com.mediatek.incallui.hangupallhold \
    --extra-packages com.android.mtkex.chips \
    --extra-packages com.mediatek.incallui.blindect

#add extproguard.flags for test case
LOCAL_PROGUARD_FLAG_FILES += extproguard.flags
