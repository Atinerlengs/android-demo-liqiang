# 概述

- 创建时间: 2016-08-11
- 创建作者: goprife@gmail.com
- 系统版本：ubuntu14.04 64bit

编译Android代码最权威的文档是[Android项目官方文档](https://source.android.com/source/requirements.html)。

AOSP项目代码下载需要翻墙，国内用户可以通过[清华AOSP镜像](https://mirrors.tuna.tsinghua.edu.cn/help/AOSP/)获取Android代码。

详细的步骤上面两个链接里都有说明，本文以具体编译Nexus 6的Android 6.0 rom为例子，简要描述如何下载代码并编译运行。

## 准备工作



1. 安装必要命令

```bash
sudo apt-get install git-core gnupg flex bison gperf build-essential \
zip curl zlib1g-dev gcc-multilib g++-multilib libc6-dev-i386 \
lib32ncurses5-dev x11proto-core-dev libx11-dev lib32z-dev ccache \
libgl1-mesa-dev libxml2-utils xsltproc unzip
```

2. 安装jdk

根据官方说明，android5.0到android6.0使用jdk7，正在开发中AOSP主分支（以及未来的7.0）使用jdk8，官方推荐使用openjdk。

ubuntu14.04系统使用软件仓库自带的openjdk7，直接安装即可使用

```bash
sudo apt-get install openjdk-7-jdk
```

安装完毕后不需做任何配置。

3. 下载repo命令

```
mkdir ~/bin
PATH=~/bin:$PATH
curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
chmod a+x ~/bin/repo
```

4. 配置adb

进入Nexus 6手机的系统设置，打开`关于手机(about phone)`，连续点击6次`build number`，可以在`Settings`页面打开隐藏的`Devleoper options`，使能`USB debugging`项。

如果/etc/udev/rules.d/51-android.rules不存在，则执行如下命令创建

```bash
$ wget -S -O - http://source.android.com/source/51-android.rules | sed "s/<username>/$USER/" | sudo tee >/dev/null /etc/udev/rules.d/51-android.rules
```

将手机通过USB链接PC

```bash
$ dmesg
...
[46872.551186] usb 3-10.3: Product: AOSP on Shamu
[46872.551191] usb 3-10.3: Manufacturer: motorola
[46872.551194] usb 3-10.3: SerialNumber: ZX1G228T9B
[47197.456666] usb 3-10.3: USB disconnect, device number 7
[51110.320069] kvm [13540]: vcpu0 disabled perfctr wrmsr: 0xc1 data 0xffff
[60186.938097] perf samples too long (2508 > 2500), lowering kernel.perf_event_max_sample_rate to 50000
[61322.877350] usb 3-10.3: new high-speed USB device number 8 using xhci_hcd
[61322.895302] usb 3-10.3: New USB device found, idVendor=18d1, idProduct=4ee7
[61322.895311] usb 3-10.3: New USB device strings: Mfr=1, Product=2, SerialNumber=3
[61322.895316] usb 3-10.3: Product: AOSP on Shamu
[61322.895321] usb 3-10.3: Manufacturer: motorola
[61322.895324] usb 3-10.3: SerialNumber: ZX1G228T9B
```

修改51-android.rules

```bash
$ sudo vim /etc/udev/rules.d/51-android.rules
```

根据**idVendor=18d1, idProduct=4ee7**添加一行，然后重新加载配置文件

```bash
$ sudo udevadm control --reload-rules
```

然后将手机断开USB后重新链接，此时使用`adb shell`应能正确执行了。

## 下载代码

Android项目开发至今，推出了多个里程碑版本，每个里程碑版本分配了唯一TAG和分支名，查看[Android分支列表](https://source.android.com/source/build-numbers.html#source-code-tags-and-builds)，还可以看到各个分支所支持的设备列表。

以Nexus6为例子，如果选择`android-6.0.1-r9`分支，并使用清华镜像仓库，那么下载命令为

```
repo init -u https://aosp.tuna.tsinghua.edu.cn/platform/manifest -b android-6.0.1-r9
```

该命令执行完毕后会在当前目录下生成`.repo`目录，继续执行如下命令开始真正下载Android代码。

```
repo sync
```

**说明：为了加快速度，清华镜像网站的使用说明中提到，由于Android项目代码量巨大，推荐使用每月的离线包下载后增量更新。强烈建议各位使用这种方式，可以大大缩短代码更新时间。**

## 编译Nexus 6项目

### 下载驱动程序
编译Nexus 6，除了下载AOSP代码外，还需要下载驱动程序。

- [驱动程序](https://developers.google.com/android/nexus/drivers)

从上面的驱动程序网站下载相应分支版本的驱动程序。对于`android-6.0.1-r9`，需要下载下面三个文件。

Nexus 6 (Mobile) binaries for Android 6.0.1 (MMB29S)||||
---|---|---|---
Hardware Component | Company | Download Link | SHA-256 Checksum
NFC, Bluetooth, Wifi |  Broadcom | [link]()| 4de94399dba548c41eebf63dbf6c3caedf6745ba032223c434eea45833ec1501
Media, Audio, Thermal, Touch Screen, Sensors | Motorola|[Link]()|	f964dea317d30a25253fa3d4e3eedfe7ae1f7dab5c3d0cb6cc8d102003ec5163
GPS, Audio, Camera, Gestures, Graphics, DRM, Video, Sensors	|Qualcomm|[Link]()|	612d1921dd903cfa4870918e7c3b497c2349cdce2f3aee692d2c47d1358a4740

下载完毕后放置到Android代码目录下，执行如下命令解压驱动程序。

```
$ chmod a+x *.sh
$ ./extract-moto-shamu.sh
...根据提示输入 I ACCEPT后回车

$ ./extract-broadcom-shamu.sh
...根据提示输入 I ACCEPT后回车

$ ./extract-qcom-shamu.sh
...根据提示输入 I ACCEPT后回车
```

三个脚本解压完毕后会生成vendor目录，其中存放项目需要的BSP。

### 编译系统

进入Android代码目录下，执行如下命令刷机

```bash
$ . build/envsetup.sh

$ lunch

You're building on Linux

Lunch menu... pick a combo:
     1. aosp_arm-eng
     2. aosp_arm64-eng
     3. aosp_mips-eng
     4. aosp_mips64-eng
     5. aosp_x86-eng
     6. aosp_x86_64-eng
     7. aosp_deb-userdebug
     8. aosp_flo-userdebug
     9. full_fugu-userdebug
     10. aosp_fugu-userdebug
     11. mini_emulator_arm64-userdebug
     12. m_e_arm-userdebug
     13. mini_emulator_mips-userdebug
     14. mini_emulator_x86_64-userdebug
     15. mini_emulator_x86-userdebug
     16. aosp_flounder-userdebug
     17. aosp_angler-userdebug
     18. aosp_bullhead-userdebug
     19. aosp_hammerhead-userdebug
     20. aosp_hammerhead_fp-userdebug
     21. aosp_shamu-userdebug

Which would you like? [aosp_arm-eng] 这里选择21，因为Nexus6手机项目代号为shamu，请注意该序号可能不同，根据实际情况选择。

$ make -j12
该开始编译，笔者机器为i7+16G内存，开12个进程并行编译，读者根据自己实际情况修改并行数目。
```

编译过程大概需要数小时，笔者机器上大概50分钟左右。编译完成后会在`out/target/product/shamu/`下生成多个image文件，并可以使用`fastboot`命令单独刷机。

- boot.img
    - `fastboot flash boot boot.img`
- system.img
    - `fastboot flash system system.img`
- userdata.img
    - `fastboot flash userdata userdata.img`
- cache.img
    - `fastboot flash cache cache.img`
- recovery.img
    - `fastboot flash recovery recovery.img`

### 刷机

使用android sdk中的fastboot命令刷机，具体步骤是：

1. 重启进入bootloader

使用adb命令启动进入bootloader，或者关机后按住音量下和电源键开机进入

```bash
$ adb reboot bootloader
```

2. 解锁oem

```bash
$ fastboot oem unlock
```

此时根据提示操作解锁。

3. 刷机

进入Android代码目录下，执行如下命令刷机

```bash
$ fastboot -w flashall
```

### 其他问题

如果遇到如下校验失败，根据提示修改。

## 参考资料
- [维基百科Android词条](https://zh.wikipedia.org/wiki/Android)，查阅各个里程碑版本发布时间
- [Android项目官方网站](https://source.android.com/source/requirements.html)，下载/编译Android代码
- [Android分支列表](https://source.android.com/source/build-numbers.html#source-code-tags-and-builds)，查看Android分支版本
- [清华AOSP项目镜像网站](https://mirrors.tuna.tsinghua.edu.cn/help/AOSP/)，国内Android代码镜像网站
- [Android官方ROM网站](https://developers.google.com/android/nexus/images)，Android Nexus系列手机官方ROM
- [Android开发者网站](https://developers.google.com)，Android应用开发官方网站
- [Android Issues](https://code.google.com/p/android/issues/list)
