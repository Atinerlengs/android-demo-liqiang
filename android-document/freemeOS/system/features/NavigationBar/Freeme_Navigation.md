## 一、功能描述

Freeme Navigation整合了虚拟导航栏和屏幕外导航键，用户可折叠虚拟导航栏，亦可使用指纹作为屏幕外导航键替代导航栏功能，不仅外观简洁，更提升屏幕阅读空间

##### 可折叠虚拟导航栏

点击折叠按钮，可将虚拟导航栏隐藏，从屏幕外底边或侧边划入屏幕，可显示虚拟导航栏

##### 屏幕外导航键

指纹键作为导航键使用，支持导航键的设备可以使用导航键轻松完成基本的导航操作，可实现

1. 轻触
2. 长按
3. 双击
4. 上下左右滑动

具体业务逻辑视项目需求而异，包括但不限于

1. 轻触返回上一级界面
2. 长按返回主屏幕
3. 接听电话
4. 拍摄照片
5. 左右滑动浏览照片
6. 调出最近应用
7. 停止闹钟

##### 切换
支持导航键的设备出厂默认关闭了导航栏，若要开启导航栏，请打开设置，点击导航键 > 屏幕内虚拟导航栏。

## 二、概要描述

作为System Bar之一，Navigation Bar由SystemUI负责排版及管理，由WMS进行窗体排布

应用开发者可使用SYSTEM_UI_FLAG_HIDE_NAVIGATION，SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION控制其位置和显隐性，也可与SYSTEM_UI_FLAG_IMMERSIVE、SYSTEM_UI_FLAG_IMMERSIVE_STICKY进行组合，实现沉浸式效果，但区别折叠功能，沉浸式效果只发生在当前窗体视图生命周期中的的隐藏，而非系统全局行为

在不可影响系统本身对Navigation Bar相关的FLAG支持的前提下，我们在SystemUI侧对其显隐逻辑进行修改，并在WMS侧窗体布局矩阵进行修正，以达到全局折叠效果

同时，我们添加了设置存储以方便用户配置


## 三、详细实现

### 1.存储

##### A、FREEME_NAVIGATION_BAR_ENABLED
标识虚拟导航栏是否使能，默认可用

##### B、FREEME_NAVIGATION_BAR_STYLE
标识虚拟导航栏UI显示风格，可调整按键显示的位置，默认为Google原生风格，返回键位于右边

##### C、FREEME_NAVIGATION_BAR_COLLAPSABLE
标识虚拟导航栏是否可隐藏，默认可折叠

##### D、FREEME_NAVIGATION_BAR_COLLAPSED
标识虚拟导航栏是否已隐藏，默认未隐藏

### 2.交互

#### A、WMS.updateNavigationBar(boolean minNav)

用途：调用WMS重新开始窗口布局

参数：minNav - 是否折叠

说明：只会重新布局，不会对Navigation Bar自身显隐逻辑进行改变

#### B、ACTION_NAVIGATIONBAR_STATUS_CHANGED

用途：调用PhoneStatusBar进行Navigation Bar的显隐改变

参数：minNavigationBar - 是否折叠

说明：系统应用可以通过发送Intent来实现最小化，但不建议这么做

#### C、WindowManager.PRIVATE_FLAG_HIDE_NAVI_BAR

用途：系统应用窗口设置这个private flag 可实现对Navigation Bar的显隐改变

### 3.宏开关

##### FeatureOption.FREEME_NAVIGATION

全局宏，对应system.prop的ro.freeme.navigation，配置情况如下

- 0 - Google自带导航栏
- 1 - 可折叠虚拟导航栏，不带指纹导航键
- 2 - 仅支持指纹导航键，无虚拟导航栏
- 3 - 带可折叠虚拟导航栏， 且支持指纹导航键

##### FeatureOption.FREEME_HW_MENUKEY_ACTAS

全局宏，对应system.prop的ro.freeme.hw_menukey_actas

- 打开时，左实体按键为短按Menu，长按根据Home键配置行为而定，若Home配置为Recent，则左实体按键长按为VoiceAssist
- 关闭时，左实体按键为短按Recent，长按menu，Home键此时只能配置为Recent
- 注意，该宏依赖FREEME_NAVIGATION非0

### 4.布局修正

#### A、Display大小修正

NonDecorDisplayWidth & NonDecorDisplayHeight
作为displayInfo的来源，决定了应用的显示区域大小，在Navigation bar最小化时，应该将相应值还原为fullwidth/fullheight大小

#### B、Stable区域修正

Stable用于描述屏幕排除状态栏和导航栏之后的区域，本不受状态栏和导航栏可见性影响，但是，Navigation bar的最小化与应用FLAG隐藏不同，最小化之后实际已让Stable区域从系统属性上做了延长，
所以，我们对其进行了校正

#### C、Dock区域修正

Dock用于描述可用来放置停靠窗口的区域，比如半屏窗口，输入法窗口等，Navigation bar的最小化后，停靠窗口随即往下延伸，所以，这个矩阵也做了校正

#### D、System区域修正

与Dock唯一的不同，System区域认为在状态栏和导航栏淡入淡出的过程中，已经是不可见的，而
Dock认为，过程中仍是可见的，除此之外，System与Dock保持着同样的大小，所以，我们也改了这个矩阵

#### E、输入法修正

如C中所说，我们同时把输入法的布局矩阵修改为提供停靠窗口的Dock区域

### 5.动画

WMS在navigation bar进入和消失时，会主动添加淡出动画，但是动画速度比较慢于后面窗体重绘的速度，表象上比较别扭，所以，我们取消了这个动画

### 6.手势

源生的system gesture是为了状态栏的拖动而设计的，有效区域遵循status_bar_height，复用性并不好，我们模仿system gesture，专门为Navigation bar 重写了一个手势监听，以导航栏1/5的高度为有效区域，降低误触的概率

同时，由于触摸事件分发机制，在Navgation bar上拉时，会把点击事件留给当前正显示的窗口，比如，输入法显示时，导航栏上拉，很大的概率会在输入法键盘上输入一个数字，故我们在ViewRoot dispatch事件之前，加了一层过滤，如果当前正处于导航栏上滑的状态中，则消费掉这个事件，不继续分发给其余的窗口

### 7.特殊窗体

#### A、锁屏/密码解锁

根据产品定义，密码解锁界面不显示导航栏，故我们进行了特殊处理，认为这个时候navVisble为false

#### B、防误触界面

防误触要求全屏覆盖在屏幕上，故我们也进行了特殊处理

### 8.Menu键兼容

为了满足带硬件按键的机器和无硬件按键的机器的按键行为一致性，特做了Menu键的适配，
如3中所述，左边按键可根据宏开关FREEME_HW_MENUKEY_ACTAS来配置为Menu还是Recent，配合Home键自带的配置项，便可做到软硬键的兼容

InputReader中，我们新加了一个虚拟的硬件按键KEYCODE_RECENT，如果当前配置开关为False，我们会把KEYCODE_MENU转成KEYCODE_RECENT上发

### 9.处理显隐行为对Configration的影响

有些设备配置可能会在运行时发生变化（例如屏幕方向、高度、键盘可用性及语言）。 发生这种变化时，Android 会重启正在运行的 Activity（先后调用 onDestroy() 和 onCreate()）。重启行为旨在通过利用与新设备配置匹配的备用资源自动重新加载您的应用，来帮助它适应新配置。而导航栏的显隐恰恰导致了窗体高度的变化，应用会走重新加载流程，我们不希望这样，所以特意对NavigationBar显隐而导致的窗口大小变化作了过滤，不加入Configration变化的范畴之内

### 10.指纹导航键

指纹导航键，检测指纹事件上报，对不同业务逻辑进行封装

## 四、遗留问题

1.搜狗输入法和Google输入法行为不一致，需要进行适配

## 五、实现清单
#### Framework

###### 1、启动FreemeInputManagerService和FreemeStatusBarManagerService
###### Keyword: "Navigation."

- [SystemServer.java]

frameworks/base/services/java/com/android/server

###### 2、创建FreemePhoneWindow
###### Keyword: "Navigation."

- [Activity.java]
- [Dialog.java]

frameworks/base/core/java/android/app/

###### 3、默认值设置及读取
###### Keyword: "Navigation."

- [defaults_freeme.xml]
- [FreemeProvidersUtils.java]

frameworks\base\packages\SettingsProvider\res\values

frameworks\base\packages\SettingsProvider\src\com\freeme\providers\settings

###### 4、配置长按Back键功能
###### Keyword: "Navigation."

- [config.xml]

frameworks/base/core/res/res/values

###### 5、过滤因为导航栏折叠而引起的Configration Changes
###### Keyword: "Navigation."

- [ActivityManagerService.java]
- [ActivityStack.java]

frameworks/base/services/core/java/com/android/server/am

###### 6、IWindowManager接口声明及实现
###### Keyword: "Navigation."

```
void updateNavigationBar(boolean minNav);
```

- [IWindowManager.aidl]
- [WindowManagerService.java]

frameworks/base/core/java/android/view

frameworks/base/services/core/java/com/android/server/wm

###### 7、WindowManagerPolicy接口声明及实现
###### Keyword: "Navigation."

```
public interface WindowState {
    ……
    boolean canCarryColors();

    void setCanCarryColors(boolean canCarryColors);
}

public interface WindowManagerFuncs {
    ……
    public void reevaluateStatusBarSize();
}
……
public void updateNavigationBar(boolean minNaviBar);
public void updateSystemUiColorLw(WindowState windowState);
```

- [WindowManagerPolicy.java]
- [WindowState.java]

frameworks/base/core/java/android/view

frameworks/base/services/core/java/com/android/server/wm

###### 8、优化从底往上的部分触摸，减少误触概率
###### Keyword: "Navigation."

- [ViewRootImpl.java]

frameworks/base/core/java/android/view

###### 9、添加private Flag，配置了该属性的Window折叠导航栏，添加三个Window attr，用于状态栏和导航栏颜色控制
###### Keyword: "Navigation."

```
public static final int PRIVATE_FLAG_MIN_NAVI_BAR = 0x00080000;
public static final int FLAG_EX_FREEMELIGHTSTYLE = 0x00000100;
public static final int COLOR_CHANGED = 0x80000000;
public int isFreemeStyle;
public int statusBarColor;
public int navigationBarColor;
```

- [WindowManager.java]

frameworks/base/core/java/android/view

###### 10、窗体逻辑控制
###### Keyword: "Navigation."

- [PhoneWindowManager.java]

frameworks/base/services/core/java/com/android/server/policy

###### 11、添加按键转义，将Menu键转为Recent键
###### Keyword: "Navigation - RecentKey."

- [InputReader.cpp]
- [InputReader.h]

frameworks/native/services/Inputflinger

###### 12、添加KEYCODE_FINGERPRINT_*
###### Keyword: "Navigation."

- [attrs.xml]
- [fpsensor.kl]
- [keycodes.h]
- [InputEventLabels.h]
- [KeyEvent.java]

frameworks/base/core/res/res/values

frameworks/base/data/keyboards

frameworks/native/include/android

frameworks/native/include/input

frameworks/base/core/java/android/view

###### 13、updateColorViewInt时强制关闭部分窗口动画
###### Keyword: "Navigation."

- [DecorView.java]

frameworks/base/core/java/com/android/internal/policy/

###### 14、设置状态栏和导航栏颜色时，主动刷新图标颜色
###### Keyword: "Navigation."

```
setStatusBarColor(int color) {
    ……
    updateLayoutParamsColor();
    ……
}
setNavigationBarColor(int color) {
    ……
    updateLayoutParamsColor();
    ……
}
```

- [PhoneWindow.java]

frameworks/base/core/java/com/android/internal/policy/

###### 15、将filterInputEvent()/interceptKeyBeforeDispatching()/dispatchUnhandledKey()暴露给子类处理
###### Keyword: "Navigation."

- [InputManagerService.java]

frameworks/base/services/core/java/com/android/server/input/

###### 16、辅助修改

- [ActiveServices.java]
- [ActivityRecord.java]
- [StatusBarManagerService.java]

frameworks/base/

##### SystemUI

###### 17、添加freeme-framework库引用
###### Keyword: "Navigation."

- [Android.mk]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/tests

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI

###### 18、按键位置配置及修改
###### Keyword: "Navigation."

- [config.xml]
- [NavigationBarInflaterView.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res/values

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/phone

###### 19、按键位置及可见性修改
###### Keyword: "Navigation."

- [NavigationBarView.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/phone

###### 20、按键位置及可见性修改
###### Keyword: "Navigation."

- [PhoneStatusBar.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/phone

###### 21、按键位置及可见性修改
###### Keyword: "Navigation."

- [StatusBarKeyguardViewManager.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/phone

###### 22、使用FreemeKeyButtonView
###### Keyword: "Navigation."

- [back.xml]
- [home.xml]
- [menu_ime.xml]
- [recent_apps.xml]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res/layout

###### 23、修改导航栏图标颜色
###### Keyword: "Navigation."

- [color.xml]
- [freeme_min_bar.xml]
- [ic_sysbar_min_bar.png]
- [freeme_colors.xml]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res/values

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res_freeme/drawable-hdpi

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res_freeme/drawable-xhdpi

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res_freeme/layout

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/res_freeme/values

###### 24、创建FreemeCommandQueue
###### Keyword: "Navigation."

- [BaseStatusBar.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar

###### 25、添加背景颜色设置方法
###### Keyword: "Navigation."

- [PhoneStatusBarTransitions.java]
- [NavigationBarTransitions.java]
- [FreemePhoneStatusBarTransitions.java]
- [FreemeNavigationBarTransitions.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/phone

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/freeme/systemui/statusbar

###### 26、根据Style设置KeybuttonView颜色
###### Keyword: "Navigation."

- [KeyButtonRipple.java]
- [FreemeKeyButtonView.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/policy

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/freeme/systemui/statusbar/policy

###### 27、添加UPDATE_BARBG_COLOR实现
###### Keyword: "Navigation."

- [FreemeCommandQueue.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/freeme/systemui/statusbar

###### 28、图标和文本Tint
###### Keyword: "Navigation."

- [TintImageView.java]
- [TintManager.java]
- [TintTextView.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/freeme/systemui/tint

###### 29、辅助修改
###### Keyword: "Navigation."

- [BarTransitions.java]
- [PhoneStatusBarView.java]
- [KeyButtonView.java]

vendor/freeme/packages/apps/FreemeSystemUI/SystemUI/src/com/android/systemui/statusbar/phone

#### Setting

###### 30、入口
###### Keyword: "Navigation."

- [SettingsActivity.java]

vendor/freeme/packages/apps/FreemeSettings/src/com/android/settings

###### 31、UI描述，Preference声明
###### Keyword: "Navigation."

- [freeme_navigation_settings.xml]
- [list_picker_dialog.xml]
- [simple_list_item_icon_single_choice.xml]

vendor/freeme/packages/apps/FreemeSettings/res_freeme/xml

vendor/freeme/packages/apps/FreemeSettings/res_freeme/layout

###### 32、设置类
###### Keyword: "Navigation."

- [FreemeNavigationSettingsFragment.java]

vendor/freeme/packages/apps/FreemeSettings/src/com/freeme/settings/navigation

###### 33、多语言

- [strings_freeme.xml]
- [arrays_freeme.xml]
- [attrs.xml]

vendor/freeme/packages/apps/FreemeSettings/res_freeme/values/

vendor/freeme/packages/apps/FreemeSettings\res-freeme\values-zh-rCN

vendor/freeme/packages/apps/FreemeSettings\res-freeme\values-zh-rTW

###### 34、资源图片

- [navigation_physical_key_type_1.png]
- [navigation_physical_key_type_1.png]
- [navigation_physical_key_type_1.png]
- [ic_settings_navigation_bar.xml]

packages\apps\Settings\res-freeme\drawable-xhdpi

packages\apps\Settings\res-freeme\drawable

#### freeme

默认全移植，如下只简单说明部分文件

###### 35、存储声明
###### keyword: "Navigation."

```
public static final String FREEME_NAVIGATION_BAR_ENABLED = "navigationbar_enabled";
public static final String FREEME_NAVIGATION_BAR_COLLAPSABLE = "navigationbar_collapsable";
public static final String FREEME_NAVIGATION_BAR_COLLAPSED = "navigationbar_collapsed";
public static final String FREEME_NAVIGATION_BAR_STYLE = "navigationbar_style";
public static boolean isNavigationBarEnabled(ContentResolver resolver) {
    if (FreemeOption.Navigation.supports(
            FreemeOption.Navigation.FREEME_NAVIGATION_FINGERPRINT | FreemeOption.Navigation.FREEME_NAVIGATION_COLLAPSABLE)) {
        return Settings.System.getInt(resolver, FREEME_NAVIGATION_BAR_ENABLED, 0) == 1;
    }
    return true;
}
```


- [FreemeSettings.java]

vendor\freeme\frameworks\base\core-export\java\com\freeme\provider

###### 36、为Java世界提供宏开关，在宏关闭的状态下，原则上不影响原生代码的任何行为

```
private static final int FREEME_NAVIGATION = getInt("ro.freeme.navigation");
public static final class Navigation {
    public static final int FREEME_NAVIGATION_COLLAPSABLE   = 1 << 0; // 1
    public static final int FREEME_NAVIGATION_FINGERPRINT   = 1 << 1; // 2

    public static boolean supports(int op) {
        return (FREEME_NAVIGATION & op) != 0;
    }
}
```

- [FreemeOption.java]

Vendor/freeme/frameworks/base/core-export/java/com/freeme/util

#### device

###### 37、配置开关，决定是否开启该功能

```
FREEME_NAVIGATION=fingerprint
```

- [ProjectConfig.mk]

device/droi/common

###### 38、读取ProjectConfig.mk中的值，写入build.prop中，开发调试时，可手动修改build.prop中的相应ro值来开启和关闭功能

```
ifeq ($(strip $(FREEME_NAVIGATION)),android)
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=0
else ifeq ($(strip $(FREEME_NAVIGATION)),collapsable)
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=0 \
    ro.freeme.navigation=1
else ifeq ($(strip $(FREEME_NAVIGATION)),fingerprint)
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=1 \
    ro.freeme.navigation=2
else ifeq ($(strip $(FREEME_NAVIGATION)),combination)
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=0 \
    ro.freeme.navigation=3
else
  PRODUCT_PROPERTY_OVERRIDES += qemu.hw.mainkeys=1
endif
```

- [device.mk]

device/droi/common

## 六、项目配置
###### ==1、项目配置时，请根据需要需要开启，请在相应的项目配置文件中override该属性==

- FREEME_NAVIGATION 可选配置如下：

1. android - Google自带导航栏
1. collapsable - 可折叠虚拟导航栏，不带指纹导航键
1. fingerprint - 仅支持指纹导航键，无虚拟导航栏
1. combination - 带可折叠虚拟导航栏， 且支持指纹导航键

- FREEME_HW_MENUKEY_ACTAS 表示硬件按键是使用MENU还是RECENT，yes表示使用MENU，反之表示使用RECENT

```
FREEME_NAVIGATION=fingerprint
FREEME_HW_MENUKEY_ACTAS=no
```

- [ProjectConfig.mk]

device/droi/project_name

###### ==2、默认值设置，配置时，请在相应的项目文件夹中重写该文件==
###### keyword: "Navigation."

- def_navigation_enabled - 是否使能导航栏
- def_navigation_collapsable - 是否可折叠导航栏
- def_navigation_collapsed - 是否导航栏已折叠
- def_navigation_style - 导航栏风格，左键返回为0，右键返回为1

```
<bool name="def_navigation_enabled" translatable="false">true</bool>
<bool name="def_navigation_collapsable" translatable="false">true</bool>
<bool name="def_navigation_collapsed" translatable="false">false</bool>
<integer name="def_navigation_style" translatable="false">1</bool>
```

- [defaults_freeme.xml]

frameworks\base\packages\SettingsProvider\res\value

## 七、指纹配置

#### 1、指纹厂商

请指纹厂商上报指纹按键时，按照轻触、双击、长按、上下左右滑动这七个键上报给驱动

#### 2、驱动上报

驱动请按照frameworks/base/data/keyboards/fpsensor.kl中定义的键值来上报

1. key 188 FINGERPRINT_UP
2. key 189 FINGERPRINT_DOWN
3. key 190 FINGERPRINT_LEFT
4. key 191 FINGERPRINT_RIGHT
5. key 192 FINGERPRINT_SINGLE_TAP
6. key 193 FINGERPRINT_DOUBLE_TAP
7. key 194 FINGERPRINT_LONGPRESS

## 八、总结

- 该功能实现关键点在于WMS对窗体的重新布局和系统原有属性的兼容，后续会对这部分进行重点维护和优化
- 该功能牵涉的改动文件比较多，移植时请注意遗漏
- 任何疑问和见解，欢迎大家随时提出讨论，感谢
