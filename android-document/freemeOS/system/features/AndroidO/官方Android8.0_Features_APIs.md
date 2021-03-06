[toc]
# Features and APIs
## 用户体验
### 通知

- **通知渠道**

允许您为要显示的每种通知类型创建用户可自定义的渠道

- **通知标志**

引入了对在应用启动器图标上显示通知标志的支持。

- **休眠**

用户可以将通知置于休眠状态，以便稍后重新显示它。

- **通知超时**

现在，使用 setTimeoutAfter() 创建通知时您可以设置超时。

- **通知清除**

系统现在可区分通知是由用户清除，还是由应用移除。

- **背景颜色**

您现在可以设置和启用通知的背景颜色。

- **消息样式**

现在，使用 MessagingStyle 类的通知可在其折叠形式中显示更多内容。

### 自动填充框架

帐号创建、登录和信用卡交易需要时间并且容易出错。在使用要求执行此类重复性任务的应用时，用户很容易遭受挫折。

Android 8.0 通过引入自动填充框架，简化了登录和信用卡表单之类表单的填写工作。在用户选择接受自动填充之后，新老应用都可使用自动填充框架。

### 画中画模式

Android 8.0 允许以画中画 (PIP) 模式启动操作组件。PIP 是一种特殊的多窗口模式，最常用于视频播放。目前，PIP 模式可用于 Android TV，而 Android 8.0 则让该功能可进一步用于其他 Android 设备。

### API 变更

Android 8.0 引入一种新的对象 PictureInPictureParams，您可以将该对象传递给 PIP 函数来指定某个 Activity 在其处于 PIP 模式时的行为。此对象还指定了各种属性，例如操作组件的首选纵横比。

现在，在添加画中画中介绍的现有 PIP 函数可用于所有 Android 设备，而不仅限于 Android TV。

### 可下载字体
允许您从提供程序应用请求字体，而无需将字体绑定到 APK 中或让 APK 下载字体。此功能可减小 APK 大小，提高应用安装成功率，使多个应用可以共享同一种字体。

### XML 中的字体
即 XML 中的字体，允许您使用字体作为资源。

### 自动调整 TextView 的大小
允许您根据 TextView 的大小自动设置文本展开或收缩的大小。

### 自适应图标
自适应图标支持视觉效果，可在不同设备型号上显示为各种不同的形状。

### 颜色管理
图像应用的 Android 开发者现在可以利用支持广色域彩色显示的新设备。要显示广色域图像，应用需要在其清单（每个操作组件）中启用一个标志，并加载具有嵌入的广域彩色配置文件（AdobeRGB、Pro Photo RGB、DCI-P3 等）的位图。

### WebView API
Android 8.0 提供多种 API，帮助您管理在应用中显示网页内容的 WebView 对象。这些 API 可增强应用的稳定性和安全性，它们包括：

- Version API
- Google SafeBrowsing API
- Termination Handle API
- Renderer Importance API

### 固定快捷方式和小部件
引入了快捷方式和微件的应用内固定功能。在您的应用中，您可以根据用户权限为支持的启动器创建固定的快捷方式和小部件。

### 最大屏幕纵横比
针对 Android 8.0 或更高版本的应用没有默认的最大纵横比。如果您的应用需要设置最大纵横比，请使用定义您的操作组件的清单文件中的 maxAspectRatio 属性。

### 多显示器支持
从 Android 8.0 开始，此平台为多显示器提供增强的支持。如果 Activity 支持多窗口模式，并且在具有多显示器的设备上运行，则用户可以将 Activity 从一个显示器移动到另一个显示器。当应用启动 Activity 时，此应用可指定 Activity 应在哪个显示器上运行。

### 统一的布局外边距和内边距
让您可以更轻松地指定 View 元素的对边使用相同外边距和内边距的情形

### 指针捕获
从 Android 8.0 开始，您的应用中的 View 可以请求指针捕获并定义一个侦听器来处理捕获的指针事件。鼠标指针在此模式下将隐藏。如果不再需要鼠标信息，该视图可以释放指针捕获。系统也可以在视图丢失焦点时（例如，当用户打开另一个应用时）释放指针捕获。

### 应用类别
允许每个应用声明其所属的类别。例如按流量消耗、电池消耗和存储消耗将应用归类。

### Android TV 启动器
添加了一种以内容为中心的全新 Android TV 主屏幕体验

### AnimatorSet
AnimatorSet API 现在支持寻道和倒播功能。
寻道功能允许您将动画的位置设置为指定的时间点处。如果您的应用包含可撤消的操作的动画，倒播功能会很有用。现在，您不必定义两组独立的动画，而只需反向播放同一组动画

### 输入和导航

- 键盘导航键区
某个操作组件使用一种复杂的视图层次结构，可考虑将多组界面元素组成一个键区，简化键盘导航这些元素的操作。
- 视图默认焦点
您可以指定在（重新）创建的操作组件继续运行并且用户按下键盘导航键（例如 Tab 键）之后应接收焦点的 View。

## 系统
### 新的 StrictMode 检测程序
添加了三个新的 StrictMode 检测程序，帮助识别应用可能出现的错误
### 缓存数据
优化了缓存数据的导航和行为。
### 内容提供程序分页
我们已更新内容提供程序以支持加载大型数据集，每次加载一页。
### 内容刷新请求
客户端可以更轻松地知道所请求的信息是否为最新信息。
### JobScheduler 改进
引入了对 JobScheduler 的多项改进。由于您通常可以使用计划作业替代现在受限的后台服务或隐式广播接收器，这些改进可以让您的应用更轻松地符合新的后台执行限制。
### 自定义数据存储
允许您为首选项提供自定义数据存储，如果您的应用将首选项存储在云或本地数据库中，或者如果首选项特定于某个设备，此功能会非常有用。
### findViewById() 签名变更
findViewById() 函数的全部实例均返回 <T extends View> T，而不是 View。
## 媒体增强功能

### VolumeShaper
新的 VolumeShaper 类。您可以用它来执行简短的自动音量转换，例如淡入、淡出和交叉淡入淡出。
### 音频焦点增强功能
音频应用通过请求和舍弃音频焦点的方式在设备上共享音频输出。应用通过启动或停止播放或者闪避音量的方式处理处于聚焦状态的变更。
### 媒体指标
新的 getMetrics() 函数将返回一个包含配置和性能信息的 PersistableBundle 对象，用一个包含属性和值的地图表示。
### MediaPlayer
 为 MediaPlayer 类添加了多种新函数。
### 音频录制器

- 音频录制器现在支持对流式传输有用的 MPEG2_TS 格式
- MediaMuxer 现在可以处理任意数量的音频和视频流，而不再仅限于一个音频曲目和/或一个视频曲目。
- MediaMuxer 还可以添加一个或多个包含用户定义的每帧信息的元数据曲目。

### 音频播放控制

- Google 智能助理的新音频使用类型
- 设备音频播放的变更
- 显式请求音频焦点

### 增强的媒体文件访问功能

- 自定义文档提供程序
- 直接文档访问
- 文档路径

## 连接

### WLAN 感知
在具有相应 WLAN 感知硬件的设备上，应用和附近设备可以通过 WLAN 进行搜索和通信，无需依赖互联网接入点。我们正在与硬件合作伙伴合作，以尽快将 WLAN 感知技术应用于设备。
### 蓝牙
增强了平台对蓝牙的支持：

- 支持 AVRCP 1.4 标准，该标准支持音乐库浏览。
- 支持蓝牙低功耗 (BLE) 5.0 标准。
- 将 Sony LDAC 编解码器集成到蓝牙堆叠中。

### 配套设备配对
在尝试通过蓝牙、BLE 和 WLAN 与配套设备配对时，Android 8.0 提供的 API 允许您自定义配对请求对话框。
## 共享
### 智能共享
了解用户的个性化分享首选项，在通过哪些应用分享各个类型的内容方面，也有着更好的把握。Android 8.0 可以根据用户的个性化首选项自动学习所有这些模式。
### 智能文本选择
当用户长按某个实体中可识别格式的单词时，系统会选中整个实体。系统识别的实体包括地址、网址、电话号码和电子邮件地址。
## 无障碍功能
### 无障碍功能按钮
可以请求在系统的导航区域显示无障碍功能按钮，该按钮让用户可从其设备上的任意位置快速激活您的服务功能。
### 独立的音量调整
允许您单独控制无障碍服务音频输出的音量，而不会影响设备上的其他声音。
### 指纹手势
沿设备的指纹传感器按特定方向滑动（上、下、左和右）。
### 字词级突出显示
可以确定TextView对象中可见字符的位置
### 标准化单端范围值

- 对于没有最小值的范围，Float.NEGATIVE_INFINITY 表示最小值。
- 对于没有最大值的范围，Float.POSITIVE_INFINITY 表示最大值

### 提示文本
 包含可用于与文本可编辑对象的提示文本进行交互的多个函数
### 连续的手势分派
指定属于同一设定手势的笔划的顺序
## 安全性与隐私
### 权限
Android 8.0 引入了多个与电话有关的新权限：

- ANSWER_PHONE_CALLS 允许您的应用通过编程方式接听呼入电话。要在您的应用中处理呼入电话，您可以使用 acceptRingingCall() 函数。
- READ_PHONE_NUMBERS 权限允许您的应用读取设备中存储的电话号码。

### 新的帐号访问和 Discovery API
Android 8.0 对应用访问用户帐号的方式引入多项改进。对于由身份验证器管理的帐号，身份验证器在决定对应用隐藏帐号还是显示帐号时可以使用自己的策略。Android 系统跟踪可以访问特定帐号的应用。
### Google Safe Browsing API
WebView 类现在添加了一个 Safe Browsing API 来增强网络浏览的安全性。
## 运行时和工具
### 平台优化
Android 8.0 为平台引入了运行时优化和其他优化，这些优化将带来多项性能改进。这些优化包括并发压缩垃圾回收、更有效的内存利用和代码区域。
它们可以加快启动时间，并为 OS 和应用带来更好的性能。
### 更新的 Java 支持
Android 8.0 添加了对更多 OpenJDK Java API 的支持：

- OpenJDK 8 中的 java.time。
- OpenJDK 7 中的 java.nio.file 和 java.lang.invoke。

### 更新的 ICU4J Android Framework API
您无需在 APK 中编译 ICU4J 库，从而减少 APK 占用空间。

| Android API 级别 | ICU 版本 | CLDR 版本 |Unicode 版本  |
|--|--|--|--|
|Android 7.0（API 级别 24），Android 7.1（API 级别 25）|56|28|8.0|
|Android 8.0|58.2|30.0.3|9.0|

## 参考文献
[官方API概览](https://developer.android.google.cn/about/versions/oreo/android-8.0.html?hl=zh-cn#rt)
