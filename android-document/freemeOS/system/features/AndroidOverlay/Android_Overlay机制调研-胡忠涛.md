#### 一. 概述：Android overlay 机制允许在不修改packages中apk的情况下，来自定义framework和package中的资源文件，实现资源的定制。

#### 二.实现方法：

1. 在build目录下package_internal.mk中定义了package_resource_overlays![image](https://note.youdao.com/yws/api/personal/file/WEBfc42abdad333d8827b5a8b64e12ee5bf?method=getImage&cstk=6qpeztF4)其中 PRODUCT_PACKAGE_OVERLAYS 和 DEVICE_PACKAGE_OVERLAYS 都可以定义Overlay目录。
2. 以freemeos7_dev中Settings.apk为例，简述Overlay的实现，我们以mk中定义的$(TOPDIR)表示项目根目录。
3. Settings 目录路径为：$(TOPDIR)packages/apps/Settings/res
4. 在项目根目录新建overlay目录，创建和Settings目录同样的目录结构：$(TOPDIR)overlay/packages/apps/Settings/res
5. 把overlay目录添加到相应的设备mk中，比如在device/droi/q5c61q_gs_gmo/device.mk中添加一行命令：DEVICE_PACKAGE_OVERLAYS += $(TOPDIR)overlay
6. 在overlay/packages/apps/Settings/res中新建values-zh-rCN目录，并增加strings.xml，内容如图片所示![image](https://note.youdao.com/yws/api/personal/file/WEB62cada78b7f0d661aa3cd79b48cda5ff?method=getImage&cstk=6qpeztF4)其中 “wifi_settings_title“和“display_settings”是覆盖Settings目录下面的，其他的都是新增的string资源字段。
7. 执行mm -j8，编译完成adb push 到 /system/priv-app/Settings/目录下,再次打开“设置”，可以看到overlay目录资源已经生效![image](https://note.youdao.com/yws/api/personal/file/WEB976bbb36a884c0eaa719bc0b7a236d0c?method=getImage&cstk=6qpeztF4)
8. 对于其他的资源文件，在overlay/packages/apps/Settings/res中都相应增加，如图：![image](https://note.youdao.com/yws/api/personal/file/WEBea2a396f3d167012f28d4660eb8fae20?method=getImage&cstk=6qpeztF4)编译之后，相应的资源文件被overlay目录下面的资源文件所替换。

#### 三.新增的overlay资源文件：

1. 在Settings中/res/xml/dashboard_categories.xml文件中引用pingfan_text新增的overlay资源文件![image](https://note.youdao.com/yws/api/personal/file/WEBee0bd1b92a080ef5744126b3bd1b162f?method=getImage&cstk=6qpeztF4)执行编译mm -j8 和 push操作之后，再次打开“设置”应用，发现新的overlay资源文件可以引用。![image](https://note.youdao.com/yws/api/personal/file/WEBabe88c6c126df50d654f74e6e9758c84?method=getImage&cstk=6qpeztF4)
2. 在代码中动态引用新增的资源文件，在SettingsActivity.java中onCreate()方法中引用pingfan_text，代码为 String str = getString(R.string.pingfan_text)，但是编译的时候会出现ERROR：pingfan_text cannot be resolved or is not a field，原因是新增的overlay资源文件的ID没有被编译到R.java中去。
#### 四.framework中overlay新增资源的引用
1. 在overlay目录下面新建framework相应的文件夹：frameworks/base/core/res/res，并增加 string.xml文件，内容如下：![image](https://note.youdao.com/yws/api/personal/file/WEB0e70a9c76b7c879b25501d71c8008ed3?method=getImage&cstk=6qpeztF4)
2. 在$(TOPDIR)frameworks/base/core/res目录下执行mm -j8 和 push 操作
3. Settings中引用overlay目录新增的framework资源文件framework_overlay字段![image](https://note.youdao.com/yws/api/personal/file/WEBf84ce2548f520ff002e458b265dce4f0?method=getImage&cstk=6qpeztF4)
4. 再次编译Settings.apk，push 到 /system/priv-app/Settings目录下，验证了framework overlay中新增的资源文件可以引用![image](https://note.youdao.com/yws/api/personal/file/WEB746c733fac56e0b925f56c960a83f589?method=getImage&cstk=6qpeztF4)

#### 五.关于 --auto-add-overlay
Android打包工具aapt的使用帮助，其中有一项  --auto-add-overlay （Automatically add resources that are only in overlays.），但是在package_internal.mk中添加选项：LOCAL_AAPT_FLAGS += --auto-add-overlay，结果还是不能在代码中动态引用.

#### 六.目前结论
Android overlay机制可以新增资源文件，并且可以用xml方式引用，但是不能通过代码方式引用。

