# 编译Android7.0代码

[TOC]

## ubuntu14.04 安装openjdk8

Android7.0代码依赖openjdk8。以下内容参考自Android官网。

1. [openjdk-8-jre-headless_8u45-b14-1_amd64.deb](http://archive.ubuntu.com/ubuntu/pool/universe/o/openjdk-8/openjdk-8-jre-headless_8u45-b14-1_amd64.deb)

SHA256 0f5aba8db39088283b51e00054813063173a4d8809f70033976f83e214ab56c0

2. [openjdk-8-jre_8u45-b14-1_amd64.deb](http://archive.ubuntu.com/ubuntu/pool/universe/o/openjdk-8/openjdk-8-jre_8u45-b14-1_amd64.deb)

SHA256 9ef76c4562d39432b69baf6c18f199707c5c56a5b4566847df908b7d74e15849

3. [openjdk-8-jdk_8u45-b14-1_amd64.deb](http://archive.ubuntu.com/ubuntu/pool/universe/o/openjdk-8/openjdk-8-jdk_8u45-b14-1_amd64.deb)

SHA256 6e47215cf6205aa829e6a0a64985075bd29d1f428a4006a80c9db371c2fc3c4c

```
$ sha256sum {downloaded.deb file}
```

对比hash值确保下载的包完整，然后执行如下命令安装。

```
$ sudo dpkg -i openjdk-8-jre-headless_8u45-b14-1_amd64.deb openjdk-8-jre_8u45-b14-1_amd64.deb openjdk-8-jdk_8u45-b14-1_amd64.deb
```

上述命令在14.04上会安装失败，提示缺少部分依赖库，执行如下命令修复依赖并完成jdk8安装。

```
$ sudo apt-get -f install
```

## ubuntu 14.04安装编译依赖

```
$ sudo apt-get install git-core gnupg flex bison gperf build-essential \
  zip curl zlib1g-dev gcc-multilib g++-multilib libc6-dev-i386 \
  lib32ncurses5-dev x11proto-core-dev libx11-dev lib32z-dev ccache \
  libgl1-mesa-dev libxml2-utils xsltproc unzip
```

参考：https://source.android.com/source/initializing

## ubuntu 12.04安装jdk8

参考： http://ubuntuhandbook.org/index.php/2015/01/install-openjdk-8-ubuntu-14-04-12-04-lts/

```
sudo add-apt-repository ppa:openjdk-r/ppa
sudo apt-get update
sudo apt-get install openjdk-8-jdk
```

## 切换jdk版本

```
$ sudo update-alternatives --config java
$ sudo update-alternatives --config javac
```

根据提示配置为java 8。

**AOSP 代码中添加自动识别jdk安装路径的代码，支持jdk7和jdk8同时存在。只要正确安装jdk, 可自动编译Android6.0（需要jdk7）与Android7.0（需要jdk8）**

```
sudo apt-get install openjdk-7-jdk
```
同时，请不要添加`JAVA_HOME`等环境变量, 即请确保`echo $JAVA_HOME`输出为空, `JAVA_HOME`配置的可能位置如下，将他们全部删掉

```
/etc/environment
/etc/profile
/etc/bash.bashrc
~/.profile
~/.bashrc
```

## 编译

```
$ . build/envsetup.sh
$ lunch
选择合适版本

$ make -j10 开10个线程编译。
```
