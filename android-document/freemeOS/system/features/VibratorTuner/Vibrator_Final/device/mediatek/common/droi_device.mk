################################# Board Configs ################################

# [full_v9c81q_ada_gmo -> v9c81q_ada_gmo]
FREEME_PRODUCT_PROJECT := $(subst full_,,$(TARGET_PRODUCT))
# [v9c81q_ada_gmo -> v9c81q]
FREEME_PUBLIC_PROJECT := $(firstword $(subst _, ,$(FREEME_PRODUCT_PROJECT)))
# [hd720, etc]
FREEME_LCD_RESOLUTION := $(BOOT_LOGO)

# Freeme BootAnimation
$(call inherit-product-if-exists, vendor/droi/freeme/frameworks/base/data/bootanimation/bootanimation.mk)
# Freeme Fonts
$(call inherit-product-if-exists, vendor/droi/freeme/frameworks/base/data/fonts/fonts.mk)
# Freeme Sounds
$(call inherit-product-if-exists, vendor/droi/freeme/frameworks/base/data/sounds/sounds.mk)
# Freeme Presets
$(call inherit-product-if-exists, vendor/droi/freeme/frameworks/base/data/presets/presets.mk)
# Freeme Prebuilts
$(call inherit-product-if-exists, vendor/droi/freeme/frameworks/base/data/prebuilts/prebuilts.mk)

# Freeme default certificate key
PRODUCT_DEFAULT_DEV_CERTIFICATE := vendor/droi/freeme/build/target/product/security/testkey

# Enable dex-preoptimization to speed up first boot sequence
ifeq ($(strip $(FREEME_USER_WITH_DEXPREOPT)), yes)
ifeq ($(TARGET_BUILD_VARIANT),user)
    WITH_DEXPREOPT := true
    WITH_DEXPREOPT_PIC := true
    DONT_DEXPREOPT_PREBUILTS := false
endif
endif

# Freeme common device init files
PRODUCT_PACKAGES += \
    init.freeme.rc

# Freeme Audible
PRODUCT_PROPERTY_OVERRIDES += \
    ro.config.notification_sound=$(FREEME_CONFIG_AUDIBLE_NOTIFICATION) \
    ro.config.alarm_alert=$(FREEME_CONFIG_AUDIBLE_ALARM) \
    ro.config.ringtone=$(FREEME_CONFIG_AUDIBLE_RINGTONE) \
    ro.config.message_sound=$(FREEME_CONFIG_AUDIBLE_MESSAGE)

# Freeme framework base modules
PRODUCT_PACKAGES += \
    freeme-framework
PRODUCT_BOOT_JARS += \
    freeme-framework

# Overlay
DEVICE_PACKAGE_OVERLAYS += device/mediatek/common/overlay/freeme


############################ Application & Features ############################


# @{ freeme.biantao, 20160604. For FreemeFW. ###################################

# -- DriverBoard

PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=$(FREEME_DEVICE_LCD_DENSITY) \
    ro.sf.lcd_width=$(LCM_WIDTH) \
    ro.sf.lcd_height=$(LCM_HEIGHT)

PRODUCT_PACKAGES += NativeMisc

ifeq ($(strip $(FREEME_FACTORYMODE_SUPPORT)), yes)
  PRODUCT_PACKAGES += FreemeFactoryMode
  ifeq ($(strip $(FREEME_FACTORYMODE_BCAMERA)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_bcamera=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_FCAMERA)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_fcamera=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_MSENSOR)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_msensor=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_GSENSOR)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_gsensor=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_LSENSOR)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_lsensor=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_GYROSCOPE)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_gyoscope=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_MHL)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_mhl=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_GPS)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_gps=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_ATLIGHT)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_atlight=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_DSENSOR)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.freeme.hw_sensor_proximity=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_WIFI)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_wifi=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_LED)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_led=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_HSENSOR)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_hsensor=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_HIFI)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_hifi=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_DUALMIC)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_dualmic=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_INFRARED)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_infrared=1
  endif
  ifeq ($(strip $(FREEME_IMEI_WRITE_SUPPORT)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_imei_write_support=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_AUDIO_DUALSPEAKER)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_audio_dualspeaker=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_STROBE_BACK)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_strobe_back=1
  endif
  ifeq ($(strip $(FREEME_FACTORYMODE_STROBE_FRONT)), yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.fo_fmode_strobe_front=1
  endif
  ifneq ($(strip $(FREEME_FACTORYMODE_KEYBOARD_EXT)),)
    PRODUCT_PROPERTY_OVERRIDES += ro.freeme.hw_keyboard_ext=$(FREEME_FACTORYMODE_KEYBOARD_EXT)
  endif
endif

ifneq ($(strip $(FREEME_FINGERPRINT_SUPPORT)),)
  PRODUCT_PACKAGES += \
    fingerprintd \
    init.freeme.fp.rc

  PRODUCT_PROPERTY_OVERRIDES += \
    ro.hardware.fingerprint=$(FREEME_FINGERPRINT_SUPPORT) \
    ro.freeme.fingerprint_custom=1

  ifeq ($(strip $(FREEME_FINGERPRINT_FRONT)),yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.freeme.fingerprint_front=1
  endif
  ifeq ($(strip $(FREEME_FINGERPRINT_TEST_PASSWORD)),yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.freeme.fingerprint_test_pwd=1
  endif

  $(call inherit-product-if-exists, hardware/droi/fingerprint/$(FREEME_FINGERPRINT_SUPPORT)/fingerprint.mk)
endif

ifneq ($(filter yes android,$(FREEME_NAVIGATIONBAR_MIN)),)
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=0
  ifeq ($(strip $(FREEME_NAVIGATIONBAR_MIN)),yes)
    PRODUCT_PROPERTY_OVERRIDES += ro.freeme.navigationbar_min=1
  endif
else
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=1
endif

# -- Applications/Features

ifneq ($(FREEME_DEFAULT_LAUNCHER),)
  PRODUCT_PROPERTY_OVERRIDES += persist.sys.default.launcher=$(FREEME_DEFAULT_LAUNCHER)
endif
ifneq ($(FREEME_DEFAULT_LOCKSCREEN),)
  PRODUCT_PROPERTY_OVERRIDES += ro.sys.default.lockscreen=$(FREEME_DEFAULT_LOCKSCREEN)
endif
ifneq ($(FREEME_DEFAULT_FONT),)
  PRODUCT_PROPERTY_OVERRIDES += persist.sys.custom.font=$(FREEME_DEFAULT_FONT)
endif
ifneq ($(FREEME_DEFAULT_FONT_SCALE),)
  PRODUCT_PROPERTY_OVERRIDES += persist.sys.custom.font_scale=$(FREEME_DEFAULT_FONT_SCALE)
endif

ifneq ($(TARGET_BUILD_VARIANT),user)
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.ams.aee_dialog=true \
    persist.sys.ams.anr_dialog=true
endif

ifeq ($(strip $(FREEME_VISITOR_MODE_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeVisitorMode
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_visitor_mode_support=1
endif

ifeq ($(strip $(FREEME_MOTION_SETTINGS)),yes)
PRODUCT_PACKAGES += FreemeMotionRecognition
endif
ifeq ($(strip $(FREEME_NON_TOUCH_OPERATION_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_non_touch_operation=1
endif

ifeq ($(strip $(FREEME_OTA_SUPPORT)),yes)
  PRODUCT_PACKAGES += FreemeOTA
  PRODUCT_PROPERTY_OVERRIDES += \
    ro.build.ota.product=$(strip $(FREEME_PRODUCT_MANUFACTURER))_$(strip $(FREEME_PRODUCT_DEVICE))_$(strip $(FREEME_OTA_LANGUAGE))_$(strip $(FREEME_OTA_FLASH))
endif

ifeq ($(strip $(FREEME_TP_GLOVE_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.tp_glove_support=1
endif

ifeq ($(strip $(FREEME_FONT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.droi_mmi_theme_font=1
endif
ifeq ($(strip $(FREEME_ICON)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.droi_mmi_theme_icon=1
endif

ifeq ($(strip $(FREEME_SAHRED_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeShared
PRODUCT_PROPERTY_OVERRIDES += ro.droi_freeme_shared=1
endif

# -- SystemUI & Keyguard

ifeq ($(strip $(FREEME_LOCKSCREEN_SUPPORT)), yes)
PRODUCT_PACKAGES += \
    FreemeLockscreenWindmill \
    FreemeLockscreenBlinds \
    FreemeLockscreenConch \
    FreemeLockscreenCD \
    FreemeLockscreenIphone \
    FreemeLockscreenPendantCard \
    FreemeLockscreenButterfly \
    FreemeLockscreenMIUI \
    FreemeLockscreenBookmark \
    FreemeLockscreenClassic \
    FreemeLockscreenClover \
    FreemeLockscreenExpand \
    FreemeLockscreenWeather \
    FreemeLockscreenQuick \
    FreemeLockscreenHalo\
    FreemeLockscreenMood\
    FreemeLockscreenHexagram
endif

ifeq ($(strip $(FREEME_SCREEN_GESTURE_WAKEUP_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.screen_gesture_wakeup=1
endif

ifeq ($(strip $(FREEME_SCREEN_DOUBLETAP_WAKEUP_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.screendoubletapwakeup=1
endif

ifeq ($(strip $(FREEME_HOME_DOUBLETAP_WAKEUP_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.home_doubletap_wakeup=1
endif

ifeq ($(strip $(FREEME_SMART_LEATHERCASE_MODE_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_smart_leathercase_mode=1
endif

ifeq ($(strip $(FREEME_TOUCH_PROTECT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.touch_protect=1
endif

# -- System Misc

ifeq ($(strip $(MTK_MASS_STORAGE)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.mtk_mass_storage=1
endif

ifeq ($(strip $(FREEME_EXTREME_LK_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_extreme_lk_support=1
endif

ifeq ($(strip $(FREEME_EXTREME_LK_TRIM)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.extreme_lk_trim=1
PRODUCT_PROPERTY_OVERRIDES += persist.sys.display_tasks=$(strip $(FREEME_EXTREME_LK_DISPLAY_TASKS))
endif

# Auto generate wifi mac address
ifeq ($(strip $(FREEME_AUTO_MAC_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.auto_generate_mac=1
endif

# Vibrator Tuner
ifeq ($(strip $(FREEME_VIBRATOR_TUNER_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.vib_tuner_support=1
endif

# @} ###########################################################################


# @{ freeme wanbao.zhang & swang, 20160604. For FreemeSA. ######################

PRODUCT_PACKAGES += SystemTest
PRODUCT_PACKAGES += StatisticalData

PRODUCT_PACKAGES += SecurityService

ifeq ($(strip $(FREEME_PERMISSION_CONTROL_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.fo_sc_permission_control=1
endif

ifeq ($(strip $(FREEME_SECURITY_PERMISSION_PREMIUM)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.fo_sc_permission_permium=1
endif

ifeq ($(strip $(FREEME_SECURITY_HI_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_hi=1
endif

ifeq ($(strip $(FREEME_SECURITY_CENTER_APK)), yes)
PRODUCT_PACKAGES += \
    CleanTask \
    Security
endif

ifeq ($(strip $(FREEME_SECURITY_STATIC_UNINSTALL)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_sc_static_uninstall=1
endif

ifeq ($(strip $(FREEME_SECURITY_NOTIFI_MANAGER)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_notifi=1
endif

ifeq ($(strip $(FREEME_SECURITY_SHUTDOWN_CLEAN_PROCESS)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_sc_shutdown_clear=1
endif

ifeq ($(strip $(FREEME_SECURITY_BPM_MANAGER)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_bpm=1
endif

ifeq ($(strip $(FREEME_SECURITY_NUMBER_MARKS)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_number_markers=1
endif

ifeq ($(strip $(FREEME_SECURITY_AUTO_CLEAN_TASK)), no)
  PRODUCT_PROPERTY_OVERRIDES += persist.sys.ct_screen_onoff=0
  PRODUCT_PROPERTY_OVERRIDES += persist.sys.ct_lowmem_onoff=0
endif


ifeq ($(strip $(FREEME_SECURITY_DATACOLLECT_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_security_datacollect=1
endif

# == Freeme.swang 2015-06-19, for security function
ifeq ($(strip $(FREEME_SECURITY_GET_APPLIST)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_getapplist=1
endif
ifeq ($(strip $(FREEME_SECURITY_GET_WIFILIST)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_getwifilist=1
endif

ifeq ($(strip $(FREEME_SECURITY_DEFAULT_APP)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_defapp=1
endif
ifeq ($(strip $(FREEME_SECURITY_INSTALL_APP)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_minstall=1
endif
ifeq ($(strip $(FREEME_SECURITY_RECEIVER_MANAGER)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_mreceiver=1
endif
ifeq ($(strip $(FREEME_SECURITY_FLOAT_MANAGER)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_mfloat=1
endif
ifeq ($(strip $(FREEME_SECURITY_HIDE_APP)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_hideapp=1
endif

ifeq ($(strip $(FREEME_SECURITY_TMS)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_tms=1
endif

ifeq ($(strip $(FREEME_SECURITY_TMS_FS)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_tms_fs=1
endif

ifeq ($(strip $(FREEME_SECURITY_TMS_TASK)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_tms_task=1
endif

ifeq ($(strip $(FREEME_SECURITY_TMS_DISABLE)),yes)
  PRODUCT_PROPERTY_OVERRIDES += ro.fo_security_tms_disable=1
endif

# @} ###########################################################################


# @{ freeme.xiaocui, 20160531. For FreemeAP. ###################################
ifeq ($(strip $(FREEME_DESKCLOCK_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeDeskClock
endif

ifeq ($(strip $(FREEME_WEATHER_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeWeather
endif

ifeq ($(strip $(FREEME_WEATHER_WIDGET_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeWeatherWidget
endif

ifeq ($(strip $(FREEME_HEALTHCENTER_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeHealthCenter
endif

ifeq ($(strip $(FREEME_CALCULATOR_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeCalculator
endif

ifeq ($(strip $(FREEME_FLOATTASK_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeFloatTask
PRODUCT_PROPERTY_OVERRIDES += ro.freeme_floattask=1
endif

ifeq ($(strip $(FREEME_COMPASS_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeCompass
endif

ifeq ($(strip $(FREEME_CALENDAR_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeCalendar
endif

ifeq ($(strip $(FREEME_CRONTAB_SUPPORT)), yes)
PRODUCT_PACKAGES += FreemeCrontab
endif

ifeq ($(strip $(FREEME_MULTISIM_RINGTONE_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_freeme_multi_sim=1
endif

ifeq ($(strip $(FREEME_LEGALNOTICES_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeLegalNotices
PRODUCT_PROPERTY_OVERRIDES += ro.freeme_legalnotices=1
endif

# @} ###########################################################################


# @{ freeme.Greg, 20160601. For FreemeLite. ####################################
ifeq ($(strip $(FREEME_FASHION_LAUNCHER_SUPPORT)),yes)
  PRODUCT_PACKAGES += FreemeHome
endif

ifeq ($(strip $(FREEME_PRIVATE_WIDGET_NEWS_PAGE_SUPPORT)),yes)
  PRODUCT_PACKAGES += FreemePrivateNewsPage
endif

ifeq ($(strip $(FREEME_THEME_CLUB_SUPPORT)),yes)
  PRODUCT_PACKAGES += FreemeThemeClub
  PRODUCT_PACKAGES += FreemeThemeClubCore
endif

ifeq ($(strip $(FREEME_BIGLAUNCHER_APP)),yes)
  PRODUCT_PACKAGES += FreemeBigLauncher
  PRODUCT_PACKAGES += theme-biglauncher
endif

ifeq ($(strip $(FREEME_THEMES_RESOURCES)),yes)
  PRODUCT_PACKAGES += JustYoung
  PRODUCT_PACKAGES += theme_forwoman
  PRODUCT_PACKAGES += theme_xuji
  PRODUCT_PACKAGES += theme_zhujue
  PRODUCT_PACKAGES += theme_xinjing
  PRODUCT_PACKAGES += Euro2016
endif

ifeq ($(strip $(FREEME_SUPPER_POWER_SAVE_SUPPORT)),yes)
  PRODUCT_PACKAGES += FreemeSuperPowerSave
  PRODUCT_PROPERTY_OVERRIDES += ro.freeme.superpower_enable = 1
endif

ifeq ($(strip $(FREEME_FESTIVALWALLPAPER_APP)),yes)
  PRODUCT_PACKAGES += FreemeFestivalWallpaper
endif

ifeq ($(strip $(FREEME_LOCK_NOW_APP)),yes)
PRODUCT_PACKAGES += FreemeLockNow
endif

ifeq ($(strip $(FREEME_SHAKE_OPEN_APP_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_shake_open_app_support=1
endif
# @} ###########################################################################


# @{ freeme.John, 20160603. For FreemeSS. ######################################
ifeq ($(strip $(FREEME_LUCKY_MONEY_APP)),yes)
PRODUCT_PACKAGES += FreemeLuckyMoney
endif

ifeq ($(strip $(FREEME_PUBLIC_INFO)),yes)
PRODUCT_PACKAGES += FreemePublicInfo
endif

ifeq ($(strip $(FREEME_OOBE_APP)),yes)
PRODUCT_PACKAGES += OOBE
endif

ifeq ($(strip $(FREEME_CSFB_FAKE_DUAL_SIGNAL)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_csfb_fake_dual_signal=1
endif

ifeq ($(strip $(FREEME_SIGNAL_DESCENT_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_signal_descent_support=1
endif

ifeq ($(strip $(FREEME_MMI_DUAL_SIGNAL_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_mmi_dual_signal_support=1
endif

ifeq ($(strip $(FREEME_SMART_SMS_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.droi_smart_sms_support=1
endif

ifeq ($(strip $(FREEME_FULLSCREEN_INCOMING_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.tyd_fullscreen_incoming=1
endif

ifeq ($(strip $(FREEME_DEFAULT_WRITE_STORAGE_SUPPORT)), yes)
PRODUCT_PROPERTY_OVERRIDES += ro.default_write_storage=1
endif

ifeq ($(strip $(FREEME_YELLOWPAPE_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeYellowPage
endif

ifeq ($(strip $(FREEME_SMART_DIAL_SUPPORT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.smart_dial_answer = 1
endif

ifeq ($(strip $(FREEME_CHANNELINFO_APP)),yes)
PRODUCT_PACKAGES += FreemeChannelInfo
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.channel_info_support=1
endif

# weixin anr start
#PRODUCT_PROPERTY_OVERRIDES += ro.mtk.dex2oat_white_list=com.tencent.mm:

ifeq ($(strip $(FREEME_WIFI_ENHANCEMENT)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.wifi_enhancement=1
endif

## Fake System
ifeq ($(strip $(FREEME_FAKE_ROM_RAM)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.fake_rom_ram = 1
endif
ifeq ($(strip $(FREEME_FAKE_PRODUCT_MODEL)),yes)
PRODUCT_PROPERTY_OVERRIDES += ro.freeme.fake_product_model=1
endif

# @} ###########################################################################


# @{ freeme.KimiWu, 20160602. For FreemeMM. ####################################
ifeq ($(strip $(FREEME_MM_CAMERA)),yes)
PRODUCT_PACKAGES += FreemeCamera
# camera interal plugins.
ifeq ($(strip $(FREEME_MM_CAMERA_CHILDMODE)),yes)
PRODUCT_PACKAGES += CameraChildrenMode
endif
ifeq ($(strip $(FREEME_MM_CAMERA_LARGEMODE)),yes)
PRODUCT_PACKAGES += CameraLargeMode
endif
ifeq ($(strip $(FREEME_MM_CAMERA_POSEMODE)),yes)
PRODUCT_PACKAGES += CameraPoseMode
endif
ifeq ($(strip $(FREEME_MM_CAMERA_WATERMARKMODE)),yes)
PRODUCT_PACKAGES += CameraWatermarkMode
endif
endif

ifeq ($(strip $(FREEME_MM_GALLERY_SUPPORT)), yes)
PRODUCT_PACKAGES += FreemeGallery
endif

ifeq ($(strip $(FREEME_MM_MUSIC_APP)),yes)
PRODUCT_PACKAGES += FreemeMusic
endif

ifeq ($(strip $(FREEME_MM_FILE_MANAGER_APP)),yes)
PRODUCT_PACKAGES += FreemeFileManager
endif

ifeq ($(strip $(FREEME_MM_VIDEO_APP)),yes)
PRODUCT_PACKAGES += FreemeVideo
endif

ifeq ($(strip $(FREEME_MM_VOICE_ASSISTANT)),yes)
PRODUCT_PACKAGES += FreemeVoiceAssistant
endif

ifeq ($(strip $(FREEME_MM_FMRADIO_APP)),yes)
PRODUCT_PACKAGES += FreemeFmRadio
endif

ifeq ($(strip $(FREEME_MM_SOUNDRECORDER_SUPPORT)),yes)
PRODUCT_PACKAGES += FreemeSoundRecorder
endif

ifeq ($(strip $(FREEME_MM_SUPERSHOT_SUPPORT)),yes)
PRODUCT_PACKAGES += \
    FreemeSuperShot \
    FreemeScreenRecorder
endif

ifeq ($(strip $(FREEME_MM_DESKTOP_CAMERA)),yes)
PRODUCT_PACKAGES += FreemePrivateMoodAlbum
endif

ifeq ($(strip $(FREEME_MM_DESKTOP_MUSIC)),yes)
PRODUCT_PACKAGES += FreemePrivateMusicPage
endif

# @} ###########################################################################


# @{ Preset Packages ###########################################################
ifneq ($(strip $(FREEME_PRODUCT_FOR_FACTORY)),yes)

# ---- Internal Apps
PRODUCT_PACKAGES += Market
PRODUCT_PACKAGES += DroiCloud
PRODUCT_PACKAGES += DroiAccount

ifeq ($(strip $(FREEME_SERVICE_APP)),yes)
PRODUCT_PACKAGES += FreemeService
endif

ifeq ($(strip $(FREEME_OPERATION_MANUAL)),yes)
PRODUCT_PACKAGES += FreemeOperationManual
endif

# ---- Themes ----
ifeq ($(strip $(FREEME_THEME_CHINAAGE)),yes)
PRODUCT_PACKAGES += theme_CHINAAGE
endif
ifeq ($(strip $(FREEME_THEME_HEAVEN)),yes)
PRODUCT_PACKAGES += theme_HEAVEN
endif
ifeq ($(strip $(FREEME_THEME_MYWORLD)),yes)
PRODUCT_PACKAGES += theme_MyWorld
endif
ifeq ($(strip $(FREEME_THEME_PINKO)),yes)
PRODUCT_PACKAGES += theme_pinko
endif
ifeq ($(strip $(FREEME_THEME_PRINCE)),yes)
PRODUCT_PACKAGES += theme_prince
endif
ifeq ($(strip $(FREEME_THEME_SPECIFICS)),yes)
PRODUCT_PACKAGES += theme_specifics
endif
ifeq ($(strip $(FREEME_THEME_VISION)),yes)
PRODUCT_PACKAGES += theme_VISION
endif

ifeq ($(strip $(FREEME_THEME_YYIN)),yes)
PRODUCT_PACKAGES += theme_yyin
endif
ifeq ($(strip $(FREEME_THEME_GENTLEMAN)),yes)
PRODUCT_PACKAGES += theme_gentleman
endif
ifeq ($(strip $(FREEME_THEME_PRINCESSDREAMS)),yes)
PRODUCT_PACKAGES += theme_PrincessDreams
endif
ifeq ($(strip $(FREEME_THEME_SIMPLEOS)),yes)
PRODUCT_PACKAGES += theme_SIMPLE_OS
endif
# ---- 3rd Apps ----

PRODUCT_PACKAGES += SogouIME

PRODUCT_PACKAGES += NewsArticle

# tecent apks
PRODUCT_PACKAGES += qq
PRODUCT_PACKAGES += qqbrowser
PRODUCT_PACKAGES += qqlive
PRODUCT_PACKAGES += tencentmobilemanager
PRODUCT_PACKAGES += tencentnews
PRODUCT_PACKAGES += weixin

# baidu search 5.0
PRODUCT_PACKAGES += BaiduSearch
PRODUCT_PACKAGES += BaiduMap
PRODUCT_PACKAGES += aiqiyiVideo


PRODUCT_PACKAGES += ReadingJoy
PRODUCT_PACKAGES += TencentReading

ifeq ($(strip $(FREEME_3RD_BAOFENGVIDEO)),yes)
PRODUCT_PACKAGES += BaoFengVideo
endif
ifeq ($(strip $(FREEME_3RD_KUWOPLAYE)),yes)
PRODUCT_PACKAGES += KuWoPlayer
endif
ifeq ($(strip $(FREEME_3RD_TAOBAO)),yes)
PRODUCT_PACKAGES += TaoBao
endif
ifeq ($(strip $(FREEME_3RD_AIMEITUAN)),yes)
PRODUCT_PACKAGES += aimeituan
endif
ifeq ($(strip $(FREEME_3RD_ZYCP)),yes)
PRODUCT_PACKAGES += zycp
endif
ifeq ($(strip $(FREEME_3RD_DAZHDIANPING)),yes)
PRODUCT_PACKAGES += DaZhdianping
endif

# MultipleAccounts
PRODUCT_PACKAGES += MultipleAccounts

endif
## @} ##########################################################################
