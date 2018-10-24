[toc]
# 行为变更
## 针对所有 API 级别的应用
这些行为变更适用于 在 Android 8.0 平台上运行的 所有应用，无论这些应用是针对哪个 API 级别构建。所有开发者都应查看这些变更，并修改其应用以正确支持这些变更（如果适用）。

### 后台执行限制
当您的应用进入已缓存状态时，如果没有活动的组件，系统将解除应用具有的所有唤醒锁。
### Android 后台位置限制
为节约电池电量、保持良好的用户体验和确保系统健康运行，在运行 Android 8.0 的设备上使用后台应用时，降低了后台应用接收位置更新的频率。此行为变更会影响包括 Google Play 服务在内的所有接收位置更新的应用。
### 应用快捷键

- com.android.launcher.action.INSTALL_SHORTCUT 广播不再会对您的应用有任何影响，因为它现在是私有的隐式广播。
- 现在，ACTION_CREATE_SHORTCUT Intent 可以创建可使用 ShortcutManager 类进行管理的应用快捷方式。
- 现在，ACTION_CREATE_SHORTCUT Intent 可以创建可使用 ShortcutManager 类进行管理的应用快捷方式。
- 旧版快捷方式仍然保留了它们在旧版 Android 中的功能，但您必须在应用中手动将它们转换成应用快捷方式。

### 语言区域和国际化
Android 7.0（API 级别 24）引入能指定默认类别语言区域的概念，但是某些 API 在本应使用默认 DISPLAY 类别语言区域时，仍然使用不带参数的通用 Locale.getDefault() 函数。现在，在 Android 8.0 中，以下函数使用 Locale.getDefault(Category.DISPLAY) 来代替 Locale.getDefault()

### 提醒窗口
应用针对的是 Android 8.0，则应用会使用 TYPE_APPLICATION_OVERLAY 窗口类型来显示提醒窗口。
### 输入和导航
在 Android 8.0 中，我们又再次使用键盘作为导航输入设备，从而为基于箭头键和 Tab 键的导航构建了一种更可靠并且可预测的模型。
### 网页表单自动填充
对于安装到运行 Android 8.0 的设备上的应用，与 WebView 对象相关的一些函数发生了变化
### 无障碍功能
无障碍服务可识别应用的 TextView 对象内部的所有 ClickableSpan 实例。
### 网络连接和 HTTP(S) 连接

### 蓝牙
Android 8.0 对 ScanRecord.getBytes() 函数检索的数据长度做出了变更
### wifi连接

- 稳定性和可靠性改进。
- 更加直观的界面。
- 一个合并的 WLAN 首选项菜单。
- 当附近存在优质的已保存网络时在兼容设备上自动激活 WLAN。

### 安全性
Android8.0不在支持SSLv3等安全性相关
### 隐私性

- 只要签署密钥相同（并且应用未在 OTA 之前安装到某个版本的 O），ANDROID_ID 的值在软件包卸载或重新安装时就不会发生变化。
- 对于在 OTA 之前安装到某个版本 Android 8.0（API 级别 26）的应用，除非在 OTA 后卸载并重新安装，否则 ANDROID_ID 的值将保持不变。要在 OTA 后在卸载期间保留值，开发者可以使用密钥/值备份关联旧值和新值。
- 即使系统更新导致软件包签署密钥发生变化，ANDROID_ID 的值也不会变化。
- 对于安装在运行 Android 8.0 的设备上的应用，ANDROID_ID 的值现在将根据应用签署密钥和用户确定作用域。

### 记录未捕获的异常
如果某个应用安装的 Thread.UncaughtExceptionHandler 未移交给默认的 Thread.UncaughtExceptionHandler，则当出现未捕获的异常时，系统不会终止应用。从 Android 8.0 开始，在此情况下系统将记录异常堆栈跟踪情况；在之前的平台版本中，系统不会记录异常堆栈跟踪情况。
### 联系人提供程序使用情况统计方法的变更
在之前版本的 Android 中，联系人提供程序组件允许开发者获取每个联系人的使用情况数据。此使用情况数据揭示了与某个联系人相关联的每个电子邮件地址和每个电话号码的信息，包括与该联系人联系的次数以及上次联系该联系人的时间。请求 READ_CONTACTS 权限的应用可以读取此数据。

如果应用请求 READ_CONTACTS 权限，它们仍可以读取此数据。从 Android 8.0 开始，使用情况数据查询会返回近似值，而不是精确值。不过，Android 系统内部仍然会保留精确值，因此，此变更不会影响 auto-complete API。
### 集合的处理
现在，AbstractCollection.removeAll() 和 AbstractCollection.retainAll() 始终引发 NullPointerException；之前，当集合为空时不会引发 NullPointerException。
### Android 企业版

- 新增多种行为，帮助应用支持完全托管设备中的工作资料。
- 变更系统更新处理、应用验证和身份验证方式，以提高设备和系统的完整性。
- 改进用户在配置、通知、“最近使用的应用”屏幕和 Always on VPN 方面的体验。

## 针对 Android 8.0 的应用

### 提醒窗口

使用 SYSTEM_ALERT_WINDOW 权限的应用无法再使用以下窗口类型来在其他应用和系统窗口上方显示提醒窗口
### 内容变更通知
Android 8.0 更改了 ContentResolver.notifyChange() 和 registerContentObserver(Uri, boolean, ContentObserver) 在针对 Android 8.0 的应用中的行为方式。
### 视图焦点
可点击的 View 对象现在默认也可以成为焦点。
### 安全性
如果您的应用的网络安全性配置选择退出对明文流量的支持，那么您的应用的 WebView 对象无法通过 HTTP 访问网站。每个 WebView 对象必须转而使用 HTTPS。
### 帐号访问和可检测性
除非身份验证器拥有用户帐号或用户授予访问权限，否则，应用将无法再访问用户帐号
### 隐私性

- 系统属性 net.dns1、net.dns2、net.dns3 和 net.dns4 不再可用，此项变更可加强平台的隐私性。
- Build.SERIAL 已弃用。
- LauncherApps API 不再允许工作资料应用获取有关主个人资料的信息。
- 要获取 DNS 服务器之类的网络连接信息，具有 ACCESS_NETWORK_STATE 权限的应用可以注册 NetworkRequest 或 NetworkCallback 对象。

### 权限
在 Android 8.0 之前，如果应用在运行时请求权限并且被授予该权限，系统会错误地将属于同一权限组并且在清单中注册的其他权限也一起授予应用。

对于针对 Android 8.0 的应用，此行为已被纠正。系统只会授予应用明确请求的权限。然而，一旦用户为应用授予某个权限，则所有后续对该权限组中权限的请求都将被自动批准。
### 媒体

- 在界面操作组件中处理媒体按钮未发生变化：前台操作组件在处理媒体按钮时仍然优先。
- 如果前台操作组件不处理媒体按钮，系统会将媒体按钮路由到最近在本地播放音频的应用。在确定哪些应用接收媒体按钮事件时，不再考虑活动状态、标志和媒体会话的播放状态。即使在应用调用 setActive(false) 后，媒体会话仍然可以接收媒体按钮事件。
- 如果应用的媒体会话已经释放，系统会将媒体按钮事件发送到应用的 MediaButtonReceiver（如果有）。
- 对于任何其他情况，系统都会舍弃媒体按钮事件。与其开始播放错误的应用，不如不播放任何东西。

### 原生库
在针对 Android 8.0 的应用中，如果原生库包含任何可写且可执行的加载代码段，则不会再加载原生库。倘若某些应用的原生库包含不正确的加载代码段，则此变更可能会导致这些应用停止工作。这是一种安全加强措施。
### 集合的处理
在 Android 8.0 中，Collections.sort() 是在 List.sort() 的基础上实现的。在 Android 7.x（API 级别 24 和 25）中，则恰恰相反。在过去，List.sort() 的默认实现会调用 Collections.sort()。
### 类加载行为
检查确保类加载器在加载新类时不会违反运行时假设条件。

## 参考文献
[官方超详细具体Android 8.0 行为变更](https://developer.android.com/about/versions/oreo/android-8.0-changes.html?hl=zh-cn#atap)
