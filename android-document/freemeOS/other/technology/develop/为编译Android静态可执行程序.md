# 概述

- 创建日期：2016/08/10
- 主机环境：ubuntu 14.04
- 本文静态编译arm版tree命令，可在Android系统上运行，该方法适用于其他二进制程序。

### 1. 配置编译工具链(toolchain)

首先去[linaro网站](https://www.linaro.org/downloads/)下载ARM toolchain，笔者选择的是[gcc-linaro-5.3-2016.02-x86_64_arm-linux-gnueabihf.tar.xz](https://releases.linaro.org/components/toolchain/binaries/latest-5/arm-linux-gnueabihf/gcc-linaro-5.3-2016.02-x86_64_arm-linux-gnueabihf.tar.xz)。下载后解压到任意位置。

### 2. 下载tree命令代码包

[tree项目官方网站](http://mama.indstate.edu/users/ice/tree/)，当前最新版本为[tree-1.7.0.tgz](ftp://mama.indstate.edu/linux/tree/tree-1.7.0.tgz)。

下载代码并解压。

### 3. 修改代码并编译

修改tree源代码目录下的Makefile，主要是设定CC为刚才下载的arm gcc， toolchain，其次是为编译、链接参数添加`-static`。
在笔者机器上改动如下：

```diff
 prefix = /usr

-CC=gcc
+#CC=gcc
+CC=~/software/gcc-linaro-5.3-2016.02-x86_64_arm-linux-gnueabihf/bin/arm-linux-gnueabihf-gcc

 VERSION=1.7.0
 TREE_DEST=tree
@@ -30,7 +31,8 @@ OBJS=tree.o unix.o html.o xml.o json.o hash.o color.o
 # Uncomment options below for your particular OS:

 # Linux defaults:
-CFLAGS=-ggdb -Wall -DLINUX -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64
+CFLAGS=-ggdb -Wall -DLINUX -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64 -static
+LDFLAGS=-static
 #CFLAGS=-O4 -Wall  -DLINUX -D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64
 #LDFLAGS=-s
```

修改完毕后，终端切换到该目录下，并执行`make`即可。

```bash
$ cd tree-1.7.0
$ make
```

将静态编译的tree命令push到Android系统中，可以看到运行效果。

```
root@shamu:/data/app/io.virtualapp-2 # tree
.
|-- base.apk
|-- lib
|   `-- arm
|       |-- libGodinJniHook.so
|       `-- libMyJni.so
`-- oat
    `-- arm
        `-- base.odex
```

