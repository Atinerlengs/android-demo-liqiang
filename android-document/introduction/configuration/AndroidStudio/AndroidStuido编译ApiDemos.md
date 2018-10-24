# Android 中的测试代码

- development/samples/
- developers/samples/android
- https://developer.android.com/samples/index.html

# 使用Android Studio编译ApiDemos.apk

AOSP项目提供了一个测试程序：ApiDemos。该项目编译使用了Android Framework的源码，因此默认需要使用Android命令编译，本文描述如何使用Android Studio导入该工程编译。


## 1. 导入工程

打开AS，选择File|New | Import Project...，浏览Android代码找到development/samples/ApiDemos，之后弹出的对话框一路Next即可。AS会自动将为该项目创建为Gradle配置文件。

### preference_switch错误

导入完毕后，AS会提示preference_switch必须为xm文件。在preference_switch文件的标签页上右击，弹出菜单中选择Rename File，添加.xml后缀名。

### com.google.android.mms 包不存在错误

com.google.android.mms包位于Android系统代码中，编译过Android系统代码后，会生成out/target/common/obj/JAVA_LIBRARIES/telephony-common_intermediates/classes.jar文件。

在AS左侧projec窗口选择Project，在app目录上右击选择New| Directory，创建lib目录，将classes.jar拷贝到该目录下。

点击AS左侧project窗口的ApiDemos，右击选择`Open Module Settings`（或者按下F4），在弹出对话框中，点击左侧modules下app项目，打开Dependencies标签页，点击+号，添加File Dependencies后，选择刚才拷贝的classes.jar。

确定关闭对话框。继续编译，该错误即消失。

### android.support.v4包不存在

同上，在Dependencies标签页，点击+号，添加library Dependencies，选择com.android.support:support-v4:23.4.0，确定关闭对话框。

### Manifest merger failed : uses-sdk:minSdkVersion 1 cannot be smaller than version 4 declared in library

在AS左侧project窗口，选择project模式，打开在app目录下的build.gradle文件中，添加如下两行。

    defaultConfig {
        minSdkVersion 4
        targetSdkVersion 22
        ...
    }

## 调试运行

