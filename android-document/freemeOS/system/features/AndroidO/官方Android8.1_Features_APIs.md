[toc]
# Features and APIs
### 神经网络API
为设备上的机器学习框架（如TensorFlow Lite - Google的移动平台ML库）以及Caffe2等提供了加速的计算和推理。
### 通知
应用程序每秒只能发送一个通知警报，而不管您收到的通知数量是多少。超过这个速度的警报声音不会排队并丢失。此更改不会影响通知行为的其他方面，通知消息仍按预期发布。
### 改善低内存设备的表现

将两个新的硬件特征常量FEATURE_RAM_LOW和FEATURE_RAM_NORMAL添加到程序包管理器。这些常量允许您将应用程序的分发和APK分割为正常或低RAM设备。
使Play商店能够突出显示特别适合给定设备功能的应用程序，从而促进更好的用户体验。

### 自动填充框架更新
对自动填充框架进行了一些改进，您可以将其纳入到您的应用程序中。

### EditText update
EditText.getText() 方法返回 Editable，之前是返回CharSequence.这个改变是向下兼容的，Editable 实现了 CharSequence

### 程序化安全浏览操作

1.您可以控制您的应用是否将已知威胁报告给安全浏览。

2.您可以让自己的应用程序自动执行特定操作（例如回到安全状态），每次遇到安全浏览会将其归类为已知威胁的网址时。

### 视频缩略图提取器

这MediaMetadataRetriever类有一个新的方法 getScaledFrameAtTime(), 缩放成适合具有给定宽度和高度的矩形，从视频生成缩略图图像很有用。

### 共享内存API
这是一个新的api，这个类允许你创建，映射和管理一个匿名的SharedMemory实例。您将内存保护设置为SharedMemory对象以进行读取和/或写入，并且由于SharedMemory对象是Parcelable，因此可以通过AIDL轻松地将其传递给另一个进程。

### 彩色壁纸API
允许您的动态壁纸为System UI提供颜色信息

### 指纹更新
FingerprintManager 介绍了如下的error codes

FINGERPRINT_ERROR_LOCKOUT_PERMANENT - 用户尝试了太多次指纹解锁

FINGERPRINT_ERROR_VENDOR - 指纹识别发生错误

### 加密更新
加密使用了新的算法
（GCM，AES，DESEDE，HMACMD5，HMACSHA1，HMACSHA224...）

### 参考文献
1. [官方Android开发者博客](https://android-developers.googleblog.com/2017/11/final-preview-of-android-81-now.html)

2. [官方API27与26的差异](https://developer.android.com/sdk/api_diff/27/changes.html)
