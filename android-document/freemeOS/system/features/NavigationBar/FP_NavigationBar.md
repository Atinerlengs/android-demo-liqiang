##### 一、模式构建

###### 1、FrontFinger
###### 宏：FREEME_FINGERPRINT_FRONT_SUPPORT

###### 2、Trikey(可交换物理按键)
###### 宏：FREEME_FINGERPRINT_FRONT_SUPPORT_TRIKEY

- -1 (Single Home)
- 0（left: Back, right: Recent)
- 1 (left: Recent, right: Back)

###### 3、Slide

##### 二、设置存储

- FREEME_NAVIGATIONBAR_IS_MIN - ==是否最小化==
- FREEME_ENABLE_NAVBAR - ==是否使能导航栏==
- FREEME_HIDE_VIRTUAL_KEY/FREEME_NAVIGATIONBAR_CAN_MIN - ==是否可以隐藏导航栏==
- FREEME_SWAP_KEY_POSITION - ==Trikey模式下按键位置==
- FREEME_VIRTUAL_KEY_TYPE/FREEME_NAVIGATIONBAR_TYPE - ==导航栏类型，0（左键返回）、1（右键返回）==

##### 三、可选设置页面

- FreemeNavigationSettings - 有前置指纹，不是TriKey设备
- FreemeNaviTrikeySettings - 有前置指纹，是TriKey设备
- FreemeVirtualKeySettings - 无前置指纹，可滑动

##### 四、指纹按键事件添加

1. KEYCODE_FINGERPRINT_UP = 280;
1. KEYCODE_FINGERPRINT_DOWN = 281;
1. KEYCODE_FINGERPRINT_LEFT = 282;
1. KEYCODE_FINGERPRINT_RIGHT = 283;
1. KEYCODE_FINGERPRINT_SINGLE_TAP = 284;
1. KEYCODE_FINGERPRINT_DOUBLE_TAP = 285;
1. KEYCODE_FINGERPRINT_LONGPRESS = 286;

##### 五、通用指纹业务支持

1. DefaultInspector
1. CameraInspector
1. CollapsePanelsInspector
1. GalleryInspector
1. InCallInspector
1. LauncherInspector
1. StartHomeInspector
1. StopAlarmInspector
1. SingleTapInspector
1. DoubleTapInspector
1. LongPressOnScreenOffInspector
1. FingerprintDemoInspector

##### 六、前置指纹业务支持

1. SystemUIBackInspector - KEYCODE_FINGERPRINT_SINGLE_TAP
1. SystemUIHomeInspector - KEYCODE_FINGERPRINT_LONGPRESS
1. SystemUIRecentInspector - KEYCODE_FINGERPRINT_LEFT/RIGHT/UP/DOWN
1. FingerPrintHomeUpInspector - ==KEYCODE_FINGERPRINT_HOME_UP==

##### 七、指纹类别

1. FingerprintNavigation
1. FingerpressNavigation - 传统的按压式指纹

##### 八、变色逻辑

1. 背景变色逻辑
2. 图标变色逻辑

##### 九、Freeme Style in Java

1. FreemeuiStyle(==freemeuistyle = 1==)
1. LuncherStyle(==freemeuistyle = -1==)
1. TouchExplorationEnabled(==freemestyle = -2==)
1. FreemeuiLightStyle(==freemeuilightstyle = 1==)
