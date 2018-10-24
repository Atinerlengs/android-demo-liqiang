[toc]
# Android O(Go Edition)

## What's New in Android Oreo by Google Developer
[What's New in Android Oreo地址连接](http://note.youdao.com/)

## 1.通知

[通知详解地址](https://developer.android.com/guide/topics/ui/notifiers/notifications.html)
- [ **通知渠道** ](https://developer.android.com/guide/topics/ui/notifiers/notifications.html#ManageChannels)

  允许你为要显示的每种通知类型创建用户自定义的渠道

- [ **通知标志** ](https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Badges)

  在应用启动器图标上显示的通知

- **休眠**

  将通知置于休眠状态

- **通知超时**

  setTimeoutAfter()

- **通知清除**

  onNotificationRemoved()

- **背景颜色**

  a. setColor() (通知设置)

  b. setColorized() (启用通知)

- **消息样式**

  MessagingStyle类

**展示图:**

<img width="280px" src="https://developer.android.com/about/versions/oreo/images/notification-long-press.png" />


**注意点：**

1.应用程序现在只能每秒发出一次通知警报声音。超过这个速度的警报声音不会排队并丢失。此更改不会影响通知行为的其他方面。

2.NotificationListenerService和 ConditionProviderService 不支持低内存的设备

**Demo效果图：**

|picture1|picture2|picture3|picture4|
|-- |-- |-- |--|
| ![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification1.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification3.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification4.png)|



[ **Demo地址** ](https://github.com/googlesamples/android-NotificationChannels)

## 2.自动填充框架

[自动填充框架详解地址](https://developer.android.com/guide/topics/text/autofill.html#benefits)

- **优点**

  a.帮助用户避免重新输入信息

  b.最大限度地减少用户输入的错误

- **先决条件**

  path:

  Settings > System > Languages & input > Advanced > Input assistance > Autofill service
- **优化应用以进行自动填充**

  a.确保数据可用

  b.提供自动填充的提示

  c.将字段标记为自动填充的重要内容

  d.强制自动填充请求

  e.确定是否启用自动填充

  f.关联网站和移动应用数据

- **支持自定义视图**

  自定义视图可以使用自动填充API来指定公开给自动填充框架的元数据。

- **在自动填充事件上使用回调**

  AutofillCallback

- **解决已知问题**

  a.检查设备是否支持自动填充并为当前用户启用

  b.调整大小的对话框不考虑自动填充

**Demo展示图：**

|picture1|picture2|picture3|picture4|
|-- |-- |--|--|
|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144637.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144651.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144729.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144736.png) |


[ **Demo地址** ](https://github.com/googlesamples/android-PictureInPicture)

## 3. 画中画模式

[画中画模式](https://developer.android.com/guide/topics/ui/picture-in-picture.html)

**简介：**

pip是主要用于视频播放的一种特殊类型的多窗口模式。Pip利用Android7.0中提供的多窗口API来提供固定的视频覆盖窗口。要将PIP添加到您的应用程序中，您需要注册支持PIP的activities，根据需要将activity切换到PIP模式，并确保UI元素处于隐藏状态，当activity处于pip模式继续播放视频。


- **声明画中画支持**

  a. android：supportsPictureInPicture

  b. android：resizeableActivity

  c. android：configChanges
- **切换您的活动的画中画**

  a. enterPictureInPictureMode()

  b. onUserLeaveHint()

- **在画中画中处理用户界面**

  a. Activity.onPictureInPictureModeChanged()

  b. Fragment.onPictureInPictureModeChanged()

  c. 遵循原则

- **在画中画中继续播放视频**

  a. isInPictureInPictureMode()

- **最佳做法**

  a. PIP适用于播放全屏视频的活动

  b. PIP模式下接收输入事件

  c. PIP模式下导致音频干扰其他应用程序


**Demo展示图：**
|picture1|picture2|picture3|
|--|--|--|
|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171211-154222.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171211-154144.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171211-154129.png)|

[ **Demo地址** ](https://github.com/googlesamples/android-PictureInPicture)

## 4.自适应TextView
[自适应TextView详解地址](https://developer.android.com/guide/topics/ui/look-and-feel/autosizing-textview.html)

- **默认**

1. android:autoSizeTextType="uniform"

- **粒度**

1. android:autoSizeStepGranularity="2sp"

- **预设尺寸**

res/values/arrays.xml:

```
<resources>
  <array name="autosize_text_sizes">
    <item>10sp</item>
    <item>12sp</item>
    <item>20sp</item>
    <item>40sp</item>
    <item>100sp</item>
  </array>
</resources>
```

**效果图：**

|picture1 |picture2 | picture3|
|-- |-- |-- |
|<img src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171205-112839.png" /> |<img src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171205-113006.png" /> |<img src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171205-113126.png" /> |

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

## 5.可下载字体
[可下载字体详解地址](https://developer.android.com/guide/topics/ui/look-and-feel/downloadable-fonts.html)

字体提供程序是一种检索字体并将其缓存在本地的应用程序，所以其他应用程序可以请求和共享字体。

![image](https://developer.android.com/guide/topics/ui/images/look-and-feel/downloadable-fonts/downloadable-fonts-process.png
)

**下载字体展示图：**
![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/font_style.png)

也通过Android Studio和Google Play服务使用可下载的字体

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

## 6.Xml中的字体

[Xml中的字体详解地址](https://developer.android.com/guide/topics/ui/look-and-feel/fonts-in-xml.html)

**简介：**

XML中的字体，可让您将字体用作资源。您可以在font文件res/font/夹中添加文件以将字体捆绑为资源。例如，要访问字体资源，请使用@font/myfont或R.font.myfont。

在java代码中:

```
Typeface typeface = getResources().getFont(R.font.dancing_script);
textView.setTypeface(typeface);
```

在xml中使用

```
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:fontFamily="@font/dancing_script"
    android:textSize="50sp"
    android:text="@string/xinsichen" />
```

效果图：
|picture1|
|--|
| <img width="300px" src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/font_xinsichen.png"  /> |

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

## 7.自适应图标

[自适应图标详解地址](https://developer.android.com/guide/practices/ui_guidelines/icon_design_adaptive.html)

**简介:**

引入了自适应启动器图标，可以在不同的设备模型中显示各种形状。

**展示图：**
|picture1|picture2|
|--|--|
| <img width="300px" src="https://developer.android.com/guide/practices/ui_guidelines/images/NB_Icon_Mask_Shapes_Ext_01.gif"  /> |<img width="300px" src="https://developer.android.com/guide/practices/ui_guidelines/images/NB_Icon_Mask_Shapes_Ext_02.gif"  />|
|自适应图标支持各种设备的不同掩码|自适应图标支持各种设备的不同掩码|

每个设备OEM都提供一个遮罩，系统随后使用该遮罩来渲染具有相同形状的所有自适应图标。自适应启动器图标也用于快捷方式，设置应用程序，共享对话框和总览屏幕。

**效果图：**
|picture1|
|--|
| <img width="300px" src="https://developer.android.com/guide/practices/ui_guidelines/images/NB_Icon_Layers_3D_03_ext.gif"  /> |
|自适应图标是使用2个图层和一个蒙版来定义的|

你可以通过定义2层来控制自适应启动器图标的外观，包括背景和前景。

在Android 7.1（API级别25）及更早版本中，启动器图标大小为48 x 48 dp。

**您现在必须使用以下准则来调整图标图层的大小：**

a.两层的尺寸必须为108 x 108 dp。

b.图标的内部72 x 72 dp出现在遮罩的视口内。

c.系统在四面各留出18dp，以产生有趣的视觉效果，如视差或脉冲。


|picture1|picture2|
|--|--|
| <img width="300px" src="https://developer.android.com/guide/practices/ui_guidelines/images/Single_Icon_Parallax_Demo_01_2x_ext.gif"  /> |<img width="300px" src="https://developer.android.com/guide/practices/ui_guidelines/images/Single_Icon_Pickup_Drop_01_2x_ext.gif"  />|
|自适应图标支持各种动态视觉效果||


[使用Image Asset Studio创建应用程序图标](https://developer.android.com/studio/write/image-asset-studio.html)

## 8.颜色管理

**简介：**

成像应用程序的Android开发人员现在可以利用具有宽色域显示的新设备。要显示宽色域图像，应用程序需要在其清单中添加flag（每个activity），并使用嵌入的宽色轮廓（AdobeRGB，Pro Photo RGB，DCI-P3等）加载位图。

## 9.WebView API

[ **WebView API详解地址** ](https://developer.android.com/guide/webapps/managing-webview.html)

- **Version API**

1. 获取应用中显示Web内容的包的相关信息

- **Google Safe Browsing API**

1. 导航到可能不安全的网站时向用户显示警告

- **Termination Handle API**

1. 处理WebView对象的渲染器进程消失的情况

- **Renderer Importance API**

1. 为WebView对象关联的呈现器进程分配优先级

  **注：** 为了保持应用程序的稳定性，您不应该更改WebView对象的渲染器优先级策略

## 10.固定快捷方式和小部件

[ **固定快捷方式和小部件** ](https://developer.android.com/guide/topics/ui/shortcuts.html#pinning)

**展示图：**
|picture1|
|--|
| <img width="300px" src="https://developer.android.com/images/guide/topics/ui/shortcuts/pinned-shortcuts.png"  /> |

**注意：**
当您尝试将快捷方式固定到受支持的启动器上时，用户会收到一个确认对话框，要求他们允许固定快捷方式。如果用户不允许快捷方式被固定，则启动程序将取消该请求。

使用 Pinned Shortcuts

**Demo效果图：**
|picture1|
|--|
| <img width="300px" src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/demo_pinned_shortcut.png" /> |

- **isRequestPinShortcutSupported()**

  验证设备的默认启动支持程序快捷启动方式

- **两种创建ShortcutInfo的方式**

  a. 如果该快捷方式已经存在，则创建一个ShortcutInfo只包含新快捷方式的ID的对象。系统会自动查找并锁定与快捷方式有关的所有其他信息。

  b.如果要固定新的快捷方式，请创建一个ShortcutInfo新快捷方式包含 ID，Intent， short label

**注意：** 如果用户不允许将快捷方式固定到启动器，则你的app不会收到callback

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/Demo)**

## 11.最大屏幕纵横比
[最大屏幕纵横比详解地址](https://developer.android.google.cn/reference/android/R.attr.html?hl=zh-cn#maxAspectRatio)

**简介：**
以Android 7.1或更低版本为目标平台，默认的最大屏幕纵横比为1.86.

针对 Android 8.0 或更高版本的应用没有默认的最大纵横比。如果您的应用需要设置最大纵横比，请使用定义您的操作组件的清单文件中的 maxAspectRatio 属性。
|机型|屏幕纵横比|上市时间|
|--|--|--|
|小米MIX|17：9|2016年10月|
|LG G6|18：9|2017年2月|
|Samsung Galaxy S8|18.5：9|2017年5月|
|Essential Phone|19：10|2017年6月|

|picture1|
|--|
|![image](http://img.blog.csdn.net/20170808150520278?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvYWhlbmNl/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)|
|适配前后的示例图|

在Galaxy S8发布之后，Android官方提供了适配方案，即提高App所支持的最大屏幕纵横比，实现很简单，在AndroidManifest.xml中可做如下配置：

```
<meta-data android:name="android.max_aspect" android:value="ratio_float"/>
```

其中ratio_float为浮点数，官方建议为2.1或更大，因为18.5：9=2.055555555……，如果日后出现纵横比更大的手机，此值将会更大。

另外如果没有上述设置，android:resizeableActivity 也为false的话，则应用所支持的最大纵横比为默认值1.86，即默认无法支持全面屏。

## 12.多显示器增强支持
[多显示器增强支持详解地址](http://note.youdao.com/)

**简介：**

如果 Activity 支持多窗口模式，并且在具有多显示器的设备上运行，则用户可以将 Activity 从一个显示器移动到另一个显示器。当应用启动 Activity 时，此应用可指定 Activity 应在哪个显示器上运行。

当用户将 Activity 从一个显示器移动到另一个显示器时，系统将调整 Activity 大小，并根据需要发起运行时变更。您的 Activity 可以自行处理配置变更，或允许系统销毁包含该 Activity 的进程，并以新的尺寸重新创建它。如需了解详细信息，请参阅[处理配置变更](https://developer.android.google.cn/guide/topics/resources/runtime-changes.html?hl=zh-cn)。

**效果图：**
|picture1|
|--|
|
![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/muti_windows.jpg)|
|huawei Mate10|

对 adb shell 进行了扩展，以支持多个显示器。shell start 命令现在可用于启动操作组件，并指定操作组件的目标显示器：

```
adb shell start <activity_name> --display <display_id>
```

**总结：**
Android 8.0 将提供更好的原生多显示器支持。如果某个应用或活动（Activity） 支持多窗口模式，并且可以在具有多个显示器的设备上运行（例如 Samsung DeX），那么用户可以在两个显示设备间自由操作和移动窗口内容。

## 13.统一的布局外边距和内边距

**简介：**

让您可以更轻松地指定 View 元素的对边使用相同外边距和内边距

- layout_marginVertical
- layout_marginHorizontal
- paddingVertical
- paddingHorizontal

**示例：**

```
<LinearLayout
    android:layout_width="wrap_content"
    android:layout_height="wrap_content">

<Button
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Hello World!"
    android:layout_marginHorizontal="50dp"
    android:layout_marginVertical="50dp"
    android:paddingHorizontal="50dp"
    android:paddingVertical="50dp"/>
</LinearLayout>
```

**效果图：**
|picture1|
|--|
| <img width="400px" src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/same_layout.png" /> |

## 14.指针捕获

[使用指针捕捉详解地址](https://developer.android.google.cn/training/gestures/movement.html?hl=zh-cn#pointer-capture)

某些应用程序（如游戏，远程桌面和虚拟化客户端）通过控制鼠标指针而获益匪浅。指针捕获是Android 8.0（API级别26）中的一项功能，稍后将通过将所有鼠标事件提供给应用程序中的焦点视图来提供此类控制。

- **请求指针捕获**

  只有在包含该视图的视图层次结构具有焦点时，您的应用中的视图才能请求指针捕获。

- **处理捕获的指针事件**

  一旦视图成功获取指针捕获，Android就开始提供鼠标事件

  a.如果您使用自定义视图，请重写onCapturedPointerEvent(MotionEvent)

  b.否则，注册一个OnCapturedPointerListener

  无论您使用自定义视图还是注册侦听器，您的视图都会收到一个 MotionEvent带有指针坐标的指针坐标，用于指定相对移动，如X / Y增量，类似于由轨迹球设备提供的坐标。您可以使用getX()和检索坐标getY()。

- **释放指针捕捉**

  调用释放指针捕获releasePointerCapture()

## 15.应用类别

**简介：**

在适当的情况下，Android 8.0 允许每个应用声明其所属的类别。这些类别用于将应用呈现给用户的用途或功能类似的应用归类在一起，例如按流量消耗、电池消耗和存储消耗将应用归类。您可以在 <application> 清单标记中设置 android:appCategory 属性，定义应用的类别。

**示例图：**
|picture1|
|--|
|
![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Categories.png)|

## 16.Android TV 启动器
[AndroidTV启动器详解地址](https://developer.android.google.cn/training/tv/discovery/recommendations-channel.html?hl=zh-cn)

**简介：**

Android TV主屏幕或简单的主屏幕提供了一个用户界面，可将推荐内容显示为频道和节目表。每一行都是一个通道。频道中包含该频道上每个可用节目的卡片

**示例：**

android电视机顶盒

[What's new for Android TV](https://www.youtube.com/watch?v=LMB9B6Z__bM&hl=zh-cn)

**展示图：**

|picture1|
|--|
|
![image](https://developer.android.google.cn/training/tv/images/home-screen-0.png)|


## 17.AnimatorSet API
[AnimatorSet API详解地址](https://developer.android.google.cn/reference/android/animation/AnimatorSet.html?hl=zh-cn)

- 寻道
- 倒播

**示例图：**

![image](http://upload-images.jianshu.io/upload_images/2062943-a276be90aeba6612.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/230)

## 18.输入和导航

[输入和导航详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn)


- **键盘导航键区**

  某个操作组件使用一种复杂的视图层次结构,可考虑将多组界面元素组成一个键区，简化键盘导航这些元素的操作。

- **视图默认焦点**

  您可以指定在创建的操作组件继续运行并且用户按下键盘导航键之后应接收焦点的 View。


**展示图：**


|picture1|
|--|
|![image](https://developer.android.google.cn/about/versions/oreo/images/keyboard-navigation-clusters.png)|
|包含 5 个键区的操作组件|

## 19.缓存数据

[缓存数据详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn)

**简述：**

现在，每个应用均获得一定的磁盘空间配额，用于存储 getCacheQuotaBytes(UUID) 返回的缓存数据。

- **为释放空间删除缓存文件**

1. 低配额被保存
2. 优先删除最旧

- **为大文件分配磁盘空间**

1. allocateBytes(FileDescriptor, long)

注： 自动清除属于其他应用的缓存文件

2. getAllocatableBytes(UUID)代替getUsableSpace()

注： 确定设备是否有足够的磁盘空间保存新数据

## 20.新的StrictMode检测程序

[新的StrictMode检测程序详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#sys)

帮助识别应用可能出现的错误：

Android8.0添加了三个新的StrictMode检测程序：

- **detectUnbufferedIo()**

**注 ：** 检测您的应用何时读取或写入未缓冲的数据，这可能极大影响性能。

- **detectContentUriWithoutPermission()**

**注：** 检测您的应用在其外部启动 Activity 时何时意外忘记向其他应用授予权限

- **detectUntaggedSockets()**

**注：** 检测您的应用何时使用网络流量


## 21.内容提供程序分页

[内容提供程序分页详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#sys)

**简述：**

我们已更新内容提供程序以支持加载大型数据集，每次加载一页。

**示例：**

一个具有大量图像的照片应用可查询要在页面中显示的数据的子集。内容提供程序返回的每个结果页面由一个 Cursor 对象表示。客户端和提供程序必须实现分页才能利用此功能。

如需了解有关内容提供程序变更的详细信息，请参阅 [ContentProvider](https://developer.android.google.cn/reference/android/content/ContentProvider.html?hl=zh-cn) 和 [ContentProviderClient](https://developer.android.google.cn/reference/android/content/ContentProviderClient.html?hl=zh-cn)。

## 22.内容刷新请求

[内容刷新请求详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#sys)

**简述：**

现在，ContentProvider 和 ContentResolver 类均包含 refresh() 函数，返回是否为true，客户端可以更轻松地知道所请求的信息是否为最新信息。


**注意：**

由于您可能通过网络不断请求数据，您应仅在有明显迹象表明内容确已过时时才从客户端调用 refresh()。执行此类内容刷新最常见的原因是响应滑动刷新手势，该手势显式请求当前界面显示最新内容。

## 23.新的后台执行限制
[新的后台执行限制详解地址](https://developer.android.google.cn/about/versions/oreo/background.html?hl=zh-cn)

**简述：**

每次在后台运行时，应用都会消耗一部分有限的设备资源，例如 RAM。 这可能会影响用户体验，如果用户正在使用占用大量资源的应用（例如玩游戏或观看视频），影响尤为明显。

为了提升用户体验，Android 8.0 对应用在后台运行时可以执行的操作施加了限制。

- 概览

用户可以在一个窗口中玩游戏，同时在另一个窗口中浏览网页，并使用第三个应用播放音乐。

同时运行的应用越多，对系统造成的负担越大。 如果还有应用或服务在后台运行，这会对系统造成更大负担，进而可能导致用户体验下降；例如，音乐应用可能会突然关闭。

为了降低发生这些问题的几率，Android 8.0 对应用在用户不与其直接交互时可以执行的操作施加了限制。

应用在两个方面受到限制：

 1.后台服务限制
 2.广播限制

- 后台服务限制
- 广播限制
- 迁移指南


## 24.新的背景位置限制
[新的背景位置限制详解地址](https://developer.android.com/about/versions/oreo/background-location-limits.html)

**简述：**

为了降低功耗，限制了后台应用程序检索用户当前位置的频率。应用程序只能每小时接收几次位置更新。


- **如果以下任一情况属实，应用程序将被视为处于前台状态：**

  a. 它具有可见的活动，不管活动是开始还是暂停。

  b. 它有一个前台服务。

  c. 另一个前台应用程序连接到该应用程序

- **前台应用行为被保留**
- **调整您的应用的位置行为**

  a. 把你的应用程序放在前台。

  b. 通过调用在应用程序中 启动前台服务startForegroundService()。

  c. 使用Geofencing API的元素（如 GeofencingApi 界面），这些元素经过优化，可最大限度地降低功耗。

- **受影响的API**

## 25.JobScheduler 改进
[JobScheduler改进详解地址](https://developer.android.google.cn/reference/android/app/job/JobScheduler.html?hl=zh-cn)

**简述：**

使用计划作业替代现在受限的后台服务或隐式广播接收器，这些改进可以让您的应用更轻松地符合新的后台执行限制。

**JobScheduler 的更新包括：**

- 工作队列与计划作业关联。

通过调用 JobInfo.Builder.setClipData() 的方式将 ClipData 与作业关联。

- 计划作业现在支持多个新的约束条件

  1）JobInfo.isRequireStorageNotLow()

  2）JobInfo.isRequireBatteryNotLow()

  3）NETWORK_TYPE_METERED


## 26.自定义数据存储
[自定义数据存储详解地址](https://developer.android.google.cn/guide/topics/ui/settings.html?hl=zh-cn#custom-data-store)

**简述：**

允许您为首选项提供自定义数据存储

**原因：**

默认情况下，Preference类将其值存储到SharedPreferences接口中，这是保持用户首选项的推荐方式。但是，如果您的应用程序将首选项存储在云或本地数据库中，或者首选项是特定于设备的，则为您的首选项提供自定义数据存储区可能会非常有用。

在运行Android 8.0（API级别26）或更高版本的设备上，您可以通过为 接口Preference的实现提供任何对象来实现此目的PreferenceDataStore。

- 实现数据存储
- 提供数据存储到首选项

## 27.findViewById()更新

**简述：**

现在 findViewById()函数的全部实例均返回<T extends View> T,而不是View。

**示例：**

```
Button btn = findViewById(R.id.btn_test);
```

**影响变化：**

如果someMethod(View)和someMethod(TextView) 均接受调用findViewId()的结果，这可能导致现有代码的返回类型不确定

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

## 28.VolumeShaper
[VolumeShaper](https://developer.android.google.cn/guide/topics/media/volumeshaper.html)

**简述：**

这是一个新的VolumeShaper类，用它来执行短暂的自动音量转换，如淡入，淡出和交叉淡入淡出。

- **VolumeShaper.Configuration**

  a. 音量曲线

  b. 插值器类型

  c. 持续时间

- **使用VolumeShaper**

  1. 创建配置 (创建一个实例VolumeShaper.Configuration)
  2. 创建一个VolumeShaper (调用createVolumeShaper())
  3. 运行VolumeShaper（shaper.apply(VolumeShaper.Operation.PLAY)）
  4. 改变曲线 (replace())
  5. 删除VolumeShaper (close())

## 29.音频焦点增强

**简述：**

音频应用通过请求和舍弃音频焦点的方式在设备上共享音频输出。

- **新AudioFocusRequest类**

  应用在处理音频焦点变化时会使用新功能：

  a.自动闪避
  (另一个应用程序请求焦点时,系统可以在不调用应用程序的onAudioFocusChange()回调的情况下进行缓存并还原卷。)

  b.延迟聚焦
  (有时系统不能授予对音频焦点的请求，因为焦点被另一个应用程序“锁定”，例如在电话呼叫期间。发生这种情况时，应用程序不应该继续播放音频，因为它没有获得焦点。）

## 30.MediaPlayer
[MediaPlayer详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#me)

**简述：**

为 MediaPlayer 类添加了多种新函数。这些函数可以从多个方面增强您的应用处理媒体播放的能力：

- 在搜索帧时进行精细控制。
- 播放受数字版权管理保护的材料的功能。
- 支持采样级加密。

## 31.MediaRecorder
[MediaRecorder详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#me)

**简述：**

- 音频录制器现在支持对流式传输的 MPEG2_TS 格式
- MediaMuxer 现在可以处理任意数量的音频和视频流，而不再仅限于一个音频曲目和/或一个视频曲目
- MediaMuxer 还可以添加一个或多个包含用户定义的每帧信息的元数据曲目。

## 32.改进媒体文件访问功能

**访问远程数据源中的大媒体文件面临一些挑战：**

- 媒体播放器需要以寻址方式访问来自文档提供程序的文件。当大媒体文件驻留在远程数据源上时，文档提供程序必须事先提取所有数据，并创建快照文件描述符。媒体播放器无法播放没有文件描述符的文件，因此在文档提供程序完成文件下载前，无法开始播放。
- 照片应用等媒体集合管理器必须通过作用域文件夹遍历一系列访问 URI 才能访问存储在外部 SD 卡上的媒体。这种访问模式会让媒体上的批量操作（例如移动、复制和删除）变得非常缓慢。
- 媒体集合管理器无法根据文档的 URI 确定其位置。这就让这些类型的应用难以允许用户选择媒体文件的保存位置。

**改进存储访问框架解决了各个挑战：**

- 自定义文档提供程序

  为远程数据源的文件创建可寻址的 **文件描述符**

- 直接文档访问

   getDocumentUri()

- 文档路径

  DocumentsContract.Path

## 33.音频播放控制

允许您查询和请求设备产生声音的方式。对音频播放的以下控制将让您的服务更轻松地仅在有利的设备条件下产生声音。

- Google 智能助理的新音频使用类型
- 设备音频播放的变更
- 显式请求音频焦点

## 34.Wifi感知
[Wifi感知](https://developer.android.google.cn/guide/topics/connectivity/wifi-aware.html)

**简介：**

新增了对 WLAN 感知的支持，此技术基于周边感知联网 (NAN) 规范。

在具有相应 WLAN 感知硬件的设备上，应用和附近设备可以通过 WLAN 进行搜索和通信，无需依赖互联网接入点。我们正在与硬件合作伙伴合作，以尽快将 WLAN 感知技术应用于设备。

## 35.蓝牙
[蓝牙详解地址](https://developer.android.com/about/versions/oreo/android-8.0.html#cs)

通过添加以下功能丰富了平台的蓝牙支持：

- 支持AVRCP 1.4标准，支持歌曲库浏览。
- 支持蓝牙低功耗（BLE）5.0标准。
- 将Sony LDAC编解码器集成到蓝牙堆栈中。

**更改变化：**

对ScanRecord.getBytes() 方法检索的数据长度进行了以下更改：

- 兼容蓝牙5的设备可能会返回超过以前最大值约60字节的数据长度。
- 如果远程设备没有提供扫描响应，则也可能返回少于60个字节。

## 36.配套设备匹对

[配套设备匹对详解地址](https://developer.android.com/guide/topics/connectivity/companion-device-pairing.html)

您可以在尝试通过蓝牙，BLE和Wi-Fi与配套设备进行配对时自定义配对请求对话框。

您可以过滤出现在配对请求对话框中的项目，例如按类型（蓝牙，BLE和Wi-Fi）或设备名称。


## 37.智能分享
[智能分享详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#sh)

**简述：**

了解用户的个性化分享首选项，在通过哪些应用分享各个类型的内容方面，也有着更好的把握。

## 38.智能文本选择

**简述：**

让应用可以帮助用户以更有意义的方式与文本交互。当用户长按某个实体中可识别格式的单词（例如某个地址或餐馆名称）时，系统会选中整个实体。用户会看到一个浮动工具栏，该工具栏包含可以处理所选文本实体的应用。

**示例：**

1. 如果系统识别出某个地址，它可以将用户导向地图应用。

## 39.无障碍功能按钮
[无障碍功能按钮详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#a11y)

**简述：**

可以请求在系统的导航区域显示无障碍功能按钮，该按钮让用户可从其设备上的任意位置快速激活您的服务功能。

## 40.独立的音量调整
[独立的音量调整详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#a11y)

**简述：**

引入了 STREAM_ACCESSIBILITY 音量类别，允许您单独控制无障碍服务音频输出的音量，而不会影响设备上的其他声音。

## 41.指纹手势

[指纹手势详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#a11y)

**简述：**

无障碍服务也可以响应替代的输入机制，即沿设备的指纹传感器按特定方向滑动（上、下、左和右）。

## 42.字词级突出显示

**简述：**

特定范围文本，Text-to-Speech API 会通知您的服务，将使用 onRangeStart() 函数开始朗读此范围的文本。

## 43.提示文本

[提示文本详解地址](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#a11y)

**简述：**

包含可用于与文本可编辑对象的提示文本进行交互的多个函数

- **isShowingHintText() 和 setShowingHintText()**

  注：分别显示和设置节点的当前文本内容是否表示节点的提示文本。

- **getHintText()**

  注：访问提示文本本身

## 44.权限

[权限详解地址](https://developer.android.com/about/versions/oreo/android-8.0.html#sp)

引入了几个与电话相关的新权限：

-  ANSWER_PHONE_CALLS （允许您的应用通过编程方式接听呼入电话）

-  READ_PHONE_NUMBERS （允许您的应用读取设备中存储的电话号码）

**注：** 这些权限均被划分为**危险**类别，属于**PHONE** 权限组。

## 45.new account access and discovery api

**简述：**

对应用如何访问用户帐户进行了一些改进。对于他们管理的帐户，身份验证人可以使用他们自己的策略来决定是否向应用程序隐藏帐户或向其披露帐户。Android系统跟踪可以访问特定帐户的应用程序。

**api更改：**

AccountManager提供了六种新方法来帮助认证者管理哪些应用可以看到一个帐户：

- setAccountVisibility(android.accounts.Account, java.lang.String, int)：设置特定用户帐户和程序包组合的可见性级别。
- getAccountVisibility(android.accounts.Account, java.lang.String)：获取特定用户帐户和程序包组合的可见性级别。
- getAccountsAndVisibilityForPackage(java.lang.String, java.lang.String)：允许认证者获得给定包裹的账户和可见度水平。
- getPackagesAndVisibilityForAccount(android.accounts.Account)：允许认证者获得给定账户的存储可见性值。
- addAccountExplicitly(android.accounts.Account, java.lang.String, android.os.Bundle, java.util.Map<java.lang.String, java.lang.Integer>)：允许认证者初始化账户的可见性值。
- addOnAccountsUpdatedListener(android.accounts.OnAccountsUpdateListener, android.os.Handler, boolean, java.lang.String[])：将OnAccountsUpdateListener侦听器添加到 AccountManager对象。只要设备上的帐户列表发生更改，系统就会调用此侦听器。

## 46.程序化安全浏览操作

**简述：**

通过使用安全浏览API，您的应用可以检测到Google已经归类为已知威胁的URL 默认情况下会 显示警告用户已知威胁的插页式广告。该屏幕允许用户选择加载URL，或返回到安全的上一页。

**自定义响应一直威胁：**

- 控制您的应用是否将已知威胁报告给安全浏览。
- 让自己的应用程序自动执行特定操作（例如回到安全状态），每次遇到安全浏览会将其归类为已知威胁的网址时。

**注意：**
为了最大限度地防范已知威胁，请等到您在调用WebView对象的loadUrl()方法之前初始化安全浏览 。

## 47.视频缩略图提取器
[视频缩略图提取器](https://developer.android.com/about/versions/oreo/android-8.1.html#sharedmemory)

**介绍：**
找到邻近给定的时间位置的帧，并返回具有相同的纵横比作为源帧的位图，缩放成以符合给定宽度和高度的矩形。

## 48.Shared Memory API

**简述：**

这个类允许你创建，映射和管理一个匿名 SharedMemory 实例

您可以将SharedMemory 对象的内存保护设置 为读取和/或写入，并且由于 SharedMemory 对象是Parcelable，因此可以通过AIDL轻松地将其传递给另一个进程。

- **什么是共享内存？**

Android共享内存是Android操作系统的一个组件，它有助于内存共享和保存。这是Linux中的设备驱动程序。

- **为什么需要共享内存？**

binder 中用来打包、传递数据的 Parcel，一般用来传递 IPC 中的小型参数和返回值。binder 目前每个进程 mmap 接收数据的内存是 1M，所以就算你不考虑效率问题用 Parcel 来传，也无法传过去。

只要超过 1M 就会报错（binder 无法分配接收空间）。所以 android 里面有一个专门用来在 IPC 中传递大型数据的东西—— Ashmem（Anonymous Shared Memroy）

- **原理概述：**

ashmem 并不像 binder 是 android 重新自己搞的一套东西，而是利用了 linux 的 tmpfs 文件系统。

tmpfs 是一种可以基于 ram 或是 swap 的高速文件系统，然后可以拿它来实现不同进程间的内存共享。

然后大致思路和流程是：

Proc A 通过 tmpfs 创建一块共享区域，得到这块区域的 fd（文件描述符）

Proc A 在 fd 上 mmap 一片内存区域到本进程用于共享数据

Proc A 通过某种方法把 fd 倒腾给 Proc B

Proc B 在接到的 fd 上同样 mmap 相同的区域到本进程

然后 A、B 在 mmap 到本进程中的内存中读、写，对方都能看到了

其实核心点就是创建一块共享区域，然后2个进程同时把这片区域 mmap 到本进程，然后读写就像本进程的内存一样。这里要解释下第3步，为什么要倒腾 fd，因为在 linux 中 fd 只是对本进程是唯一的，在 Proc A 中打开一个文件得到一个 fd，但是把这个打开的 fd 直接放到 Proc B 中，Proc B 是无法直接使用的。但是文件是唯一的，就是说一个文件（file）可以被打开多次，每打开一次就有一个 fd（文件描述符），所以对于同一个文件来说，需要某种转化，把 Proc A 中的 fd 转化成 Proc B 中的 fd。这样 Proc B 才能通过 fd mmap 同样的共享内存文件


## 49.WallpaperColors API
[WallpaperColorsAPI](https://developer.android.com/about/versions/oreo/android-8.1.html#wallpaper)

**简述：**

允许您的动态壁纸为系统UI提供颜色信息。

**使用方法：**

- 使用三种颜色创建对象
- 位图创建对象
- drawable创建一个对象


## 50.指纹更新
[指纹更新详解地址](https://developer.android.com/about/versions/oreo/android-8.1.html#fingerprint)

FingerprintManager类含有以下的错误码：

- FINGERPRINT_ERROR_LOCKOUT_PERMANENT
（用户尝试使用指纹读取器解锁设备的次数过多。）
- FINGERPRINT_ERROR_VENDOR
（发生特定于供应商的指纹识别器错误）

## 51.加密更新
[加密更新详解地址](https://developer.android.com/about/versions/oreo/android-8.1.html#nnapi)

Android 8.1已经进行了许多密码修改：

- 新的算法已经在Conscrypt中实现（GCM,AES,DESEDE等）
- Cipher.getParameters().getParameterSpec(IvParameterSpec.class)不再适用于使用GCM的算法。相反，使用 getParameterSpec(GCMParameterSpec.class)。
- 与TLS相关的许多内部的加密类被重构。
- SSLSessionIllegalArgumentException当传递一个空引用时抛出NullPointerException。
- RSA KeyFactory不再允许从字节数组中生成大于编码密钥的密钥。
- 当套接字读取被关闭的套接字中断时，Conscrypt用于从读取中返回-1。现在阅读抛出 SocketException。
- 根CA证书集已被更改，大多数删除了大量的废弃证书，同时也删除了WoSign和StartCom的根证书。


## 52.神经网络API
[神经网络API详解地址](https://developer.android.com/about/versions/oreo/android-8.1.html#nnapi)

**简述：**

Neural Networks API为设备上的机器学习框架（如TensorFlow Lite -Google的移动平台ML库以及Caffe2等）提供了加速的计算和推理。

TensorFlow Lite可与Neural Networks API 协同工作，在移动设备上高效运行 MobileNets， Inception v3和 Smart Reply等模型。


## 53.运行时和工具

- 平台优化
- 更新的Java支持
- 更新的ICU4J Android Framework API
