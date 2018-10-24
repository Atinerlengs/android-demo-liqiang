[TOC]

# 反编译脚本集`fox`

反编译`Android framework/app`需要使用多个逆向工具，为了简化使用、方便维护和部署，模仿git命令，编写了一组脚本，这就是fox的由来。

## 原理概述

### 格式

1. apk
2. dex
3. odex
4. oat
5. art

关于dex/odex/oat格式的一些必要知识点：

1. dex格式是google专为其dalvik虚拟机设计的格式。
2. dex由dalvik虚拟机解释执行，出于性能考虑，google又开发了odex格式，该格式针对dex格式做了少许优化，引入了一些特殊指令（这些指令在标准dex格式中是非法的）。
3. Android L之后，google开了一款新型虚拟机Android Runtime（简称为ART）取代dalvik，同时引入OAT技术，可以将dex/odex转换为native代码（ELF格式），从而大大加快加载速度。并引入了两种新的格式，分别是oat、art。其中oat是一种elf变种格式，出于兼容考虑，oat文件中还内嵌一份odex格式代码，可使用第三方工具（如dextra)取出；art格式则是运行时数据，供ART虚拟机使用
4. Android N ROM中的dex、odex、oat文件本质上都是oat格式。也就是说，单纯以后缀名区分实际格式并不准确。请使用`file`命令查看真正格式。

### image格式

1. ramdisk格式

boot.img和recovery.img使用该格式。这是一种只读格式，常用作启动分区。

```
$ file boot.img
boot.img: Android bootimg, kernel (0x8000), ramdisk (0x2000000), page size: 4096, cmdline (console=null androidboot.hardware=qcom user_debug=31 msm_rtb.fi)
```

解压/生成该格式image可使用`https://github.com/xiaolu/mkbootimg_tools`工具

2. sparse image格式

AOSP编译完成后，out目录下生成的system.img/userdata.img/cache.img等文件都为sparse格式，这是一种简单的压缩格式。因为ext4格式未经压缩，通常会含有大量的0字符填充，浪费存储空间。

在笔者的macOS上，使用`file`识别：

```
$ file system.img
system.img: Android sparse image, version: 1.0, Total of 1113600 4096-byte output blocks in 5509 input chunks
```

ubuntu 14.04上，使用`file`识别：

```
$ file system.img
system.img: data
```

说明：由于ubuntu 14.04中的`file`包太老，还没有增加对android sparse格式支持，附录提供了升级`file`的方法。

解压缩/生成该格式，可使用AOSP编译生成的simg2img命令将其转换为标准的ext4格式，位于`out/host/<linux/darwin/win>-x86/bin/`目录下：

```
$ simg2img userdata.img userdata.img.ext4
```

反之，可使用img2simg将ext4格式image转换为sparse格式

```
$ img2simg userdata.img.ext4 userdata.img
```

3. ext4格式

```
$ file system.img
system.img: Linux rev 1.0 ext4 filesystem data, UUID=57f8f4bc-abf4-655f-bf67-946fc0f9f25b (extents) (large files)
```

这种格式可使用mount直接挂载，如下命令将`system.img`挂载到`/mnt`目录下，之后访问`/mnt`目录即可查看（或修改）`system.img`内容。

```
$ sudo mount -o loop -t ext4 system.img /mnt
```

注意：在配置了亿赛通加密的机器上，请将待挂载的image拷贝在/tmp目录下执行，否则挂载失败。

## 安装

```
$ cd ~
$ git clone https://github.com/prife/fox.git
$ mkdir -p ~/bin; cd ~/bin
$ ln -fs ~/fox/fox
```

说明：一般Linux系统（如ubuntu）默认将`~/bin`加入`PATH`中，如果其他系统，如macOS，请手动将其加入`PATH`，并确保`fox`命令可以被正确执行。

首次运行`fox`之前，请执行`install`子命令，完成必要安装。

```
$ fox install
```

## 更新

因为`fox`可能存在bug，更新会比较迅速，请及时执行`update`子命令。确保使用最新代码。

```
$ fox update
```

## 逆向Android framework

逆向android framework需要/system/framework目录。可以使用两种方式得到：

- 连接待逆向手机，使用adb pull /system/framework得到
- 从网络下载手机rom，解压得到system.img，转换为ext4格式，取出其中framework目录。

使用`decrom`命令的`all`参数一键逆向framework

```
$ fox decrom all your-rom-directory
```

注意，`your-rom-directory`必须为`framework`目录的父目录，即该目录下必须有`framework`目录。

这里从手机中pull出rom并逆向的方式为例，整个过程步骤如下：

1. 连接手机，使能ADB调试

2. pull出代码

```
    $ mkdir decompile-projects; cd decompile-projects
    $ mkdir rom; cd rom
    $ adb pull /system/framework
    $ adb pull /system/priv-app
    $ adb pull /system/app
```

3. 执行逆向

```
    $ cd ../
    $ mkdir decompile; cd decompile
    $ fox decrom app ../rom
```

接下来生成`AndroidStuido`工程

## 逆向app

### 逆向代码

使用`decrom`命令的app参数一键逆向app

```
$ fox decrom app your-rom-directory your-rom-app-directory
```

例如，逆向systemui，假定rom目录下有framework、priv-app、app三个目录。

```
$ fox decrom app rom rom/priv-app/SystemUI
```

即可完成对SystemUI.apk的逆向，逆向完成后会自动生成SystemUI目录。

jadx支持fallback模式，可以生成后缀为jadx的文件，风格类似于smali，可读性更好。命令如下：

1. `fox decrom all-fallback your-rom-directory`
2. `fox decrom app-fallback your-rom-directory your-rom-app-directory`

### 逆向资源

`decrom`命令内部使用jadx逆向app，逆向出来的资源效果并不好。这时候可以使用apktool逆向资源。经过对比测试，在未保存退出的情况下，apktool逆向的app资源可以直接使用。

更多内容可参考apktool官方文档：https://ibotpeaches.github.io/Apktool/documentation/

**注意：华为手机自带的apk，apktools无法逆向**

### 修复代码

逆向出来的java代码，会出现很多常量，如下：

1. intent action常量

```
action.equals("android.intent.action.SCREEN_OFF")
```

2. context service常量

```
this.mFingerprintManager = (FingerprintManager) getApplicationContext().getSystemService("fingerprint");
```

3. intent flags常量

```
intent.setFlags(872415232);
```

4. LayoutParams常量

```
if ((lp.flags & 4194304) != 0 || (lp.flags & 524288) != 0) {
```

5. view visibility常量

```
this.mAnimationBox.setVisibility(0);
```

6. 其他

`fixsrc`命令可以将java代码中的这些常量替换为标准写法。用法如下：

```
$ fox fixsrc a file1.java file2.java ....
```

批量修复某个目录下所有java代码，可使用：

```
$ find your-java-src -name "*.java" | xargs fox fixsrc a
```

除此之外，`fixsrc`命令还支持其他参数，分别是：

1. `fox fixsrc f_i <intent-flag-number>`，`intent-flag-number`支持10/16进制方式，输入`fox fixsrc f_i 10`查看效果
2. `fox fixsrc f_lp <layoutparams-flag-number>`，同理，不再赘述

# 常见反编译工具汇总

- jadx
- jeb
- apktool
- apkdb，http://idoog.me/?p=2933
- baksmali/smali
- dextra，http://newandroidbook.com/tools/dextra.html
- vdexExtractor
- SVADeodexerForArt，https://forum.xda-developers.com/galaxy-s5/general/tool-deodex-tool-android-l-t2972025

## JEB

```
\\prifepc\sharedir\software\jeb2.2.7.tar.gz
```

说明：

- https://github.com/pnfsoftware/jeb2-plugin-oat/releases

## jadx

```
\\prifepc\sharedir\software\jadx-0.6.1.zip
```

官方下载：github搜索jadx

使用jadx反编译Android7.0会出现错误，拷贝`AOSP/out/host/linux-x86/framework/dx.jar`，覆盖`jadx/lib/dx-1.10.jar`即可。

反编译较大包时，jadx会卡住，请在jadx脚本开头中加入如下代码：

```
JAVA_OPTS="-server -Xms1024m -Xmx8192m -XX:PermSize=256m -XX:MaxPermSize=1024m"
```

基本用法是：

```
jadx --show-bad-code xxx.apk/xxx.dex
```

如果逆向被混淆的应用，建议添加`--deobf`参数。

更多用法参考其自带help

## OTA提取dex

### dextra（<= Android N1)

官网：http://newandroidbook.com/tools/dextra.html

用法：

```
dextra.ELF64 -dextract xxx.odex
```

更多用法参考其自带说明

### vdexExtractor（Android O)

官网：https://github.com/anestisb/vdexExtractor

## dex转smali

baksmali

地址：https://github.com/JesusFreke/smali

# 附录

## 升级`file`命令

ubuntu 14.04自带的file版本为5.14，无法识别很多新的文件类型，如Android的sparse格式，升级方法如下。

1. 下载file5.28 deb包，https://launchpad.net/ubuntu/zesty/amd64/file/1:5.28-2ubuntu1
2. 下载libmagic1 5.28 deb包，https://launchpad.net/ubuntu/zesty/amd64/libmagic1/1:5.28-2ubuntu1

sudo dpkg -i 分别安装`libmagic1`与`file`即可。

完整命令如下：

```
$ wget http://launchpadlibrarian.net/273183617/libmagic1_5.28-2ubuntu1_amd64.deb
$ wget http://launchpadlibrarian.net/273183615/file_5.28-2ubuntu1_amd64.deb
$ sudo dpkg -i libmagic1_5.28-2ubuntu1_amd64.deb
$ sudo dpkg -i file_5.28-2ubuntu1_amd64.deb
```
