## 搭建AOSP repo镜像仓库

因为Android代码仓库较大，受限于GFW，无法直接下载Android代码，国内清华大学提供了AOSP镜像服务，本文基于清华大学AOSP镜像搭建次级镜像，供公司局域网用户获取代码。

### 参考文档

- https://mirrors.tuna.tsinghua.edu.cn/help/AOSP/

### 同步镜像仓库

假设将AOSP镜像代码放在`/home/prife/mirror_aosp`目录下。

创建工作目录

```
mkdir ~/mirror_aosp
cd ~/mirror_aosp
```

初始化repo镜像仓库，注意最后的`--mirror`参数。

```
repo init -u https://aosp.tuna.tsinghua.edu.cn/mirror/manifest --mirror
```

拉取代码

```
repo sync
```

镜像同步完成后，本机可以从该镜像获取代码，比如取出android 6.0.1 r9代码树，方法如下。

```
mkdir ~/android6.0
cd ~/android6.0
repo init -u ~/mirror_aosp/platform/manifest.git -b android-6.0.1_r9
repo sync
```

### 启动git server服务

如果想要其他局域网客户端能够通过本地镜像下载代码，则需要启动git server。

```
sudo apt-get install git-daemon
```

按照如下方式修改`/etc/sv/git-daemon/run`

```diff
@@ -3,4 +3,5 @@ exec 2>&1
 echo 'git-daemon starting.'
 exec chpst -ugitdaemon \
   "$(git --exec-path)"/git-daemon --verbose --reuseaddr \
-    --base-path=/var/lib /var/lib/git
+    --export-all
+    --base-path=/home/prife/mirror_aosp /home/prife/mirror_aosp
```

注意：`--base-path`后面跟AOSP镜像目录，根据自己的实际情况修改。

接下来重启git server，使修改生效。

```
sudo sv stop git-daemon
sudo sv start git-daemon
```

这样，局域网内其他用户可以通过`git://ip.to.mirror/`作为AOSP镜像地址下载代码了。

### 从镜像仓库同步代码

假定搭建AOSP镜像服务器地址为`192.168.0.2`，那么局域网内其他机器下载android-6.0.1_r9代码，命令如下

```
mkdir ~/android6.0
cd ~/android6.0
repo init -u git://192.168.0.2/platform/manifest.git -b android-6.0.1_r9
repo sync
```

### 遗留问题

如何从已有Android源码中创建镜像（即repo init的时候并没有使用--mirror参数）？
