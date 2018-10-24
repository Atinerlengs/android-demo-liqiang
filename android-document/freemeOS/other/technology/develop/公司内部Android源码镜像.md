# 公司内部Android源码镜像

公司运维部门搭建好了内部Android官方源码镜像仓库，服务器地址为`git://192.168.0.193`，现在使用公司的AOSP镜像拉取代码，方法如下：

## 1.  下载repo脚本

使用如下命令下载repo脚本并添加PATH路径中，如果无法下载，请使用附件repo脚本

```
$ mkdir -p ~/bin
$ PATH=~/bin:$PATH
$ curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
$ chmod a+x ~/bin/repo
```

## 2. 下载代码

如下载Android6.0.1 r9版本，如需要拉取其他版本，请参考 `https://source.android.com/source/build-numbers.html#source-code-tags-and-builds`

```
$ mkdir ~/android6.0
$ cd ~/android6.0
$ repo init -u git://192.168.0.193/platform/manifest -b android-6.0.1_r9
$ repo sync
```

另外，Android 7.0 r1版也已就绪， tag为 android-7.0.0_r1 

因为AOSP代码量极大，服务器负荷很重，请大家拉取代码时尽量避免高峰时间，以免影响公司其他服务。

其他参考：
- 清华AOSP镜像： https://mirrors.tuna.tsinghua.edu.cn/help/AOSP/ 
- 编译Android代码：https://source.android.com/source/building.html 
