[TOC]

# 综述

gerrit默认不支持删除项目功能。官方提供了delete-project插件，安装该插件后，项目的general页面底部`Project Commands`字段下方新增`Delete`按钮，点击即可删除项目。

经测试，该插件可以在删除gerrit上项目、以及服务器上的git仓库，同时还会将gerrit上的所有提交记录删掉。

# 安装依赖工具

## 安装ant

http://ant.apache.org/bindownload.cgi

## 安装buck

```
$ git clone https://gerrit.googlesource.com/buck
$ cd buck
$ ant
```

PS. buck仓库较大（.git仓库近500M），下载时间很长。

笔者机器上16秒即编译完成，生成的buck位于buck/bin/下。

# 编译gerrit的delete-project插件

## 下载代码

**下载gerrit代码**

因为笔者公司配置的gerrit版本是v2.12.2。因此这里选择取出该版本代码。

```
$ cd /opt/projects
$ git clone https://gerrit.googlesource.com/gerrit
$ cd gerrit
$ git checkout v2.12.2
```

**下载delete-project插件代码**

```
$ cd plugins
$ git clone https://gerrit.googlesource.com/plugins/delete-project
$ cd delete-project
$ git checkout -b 2.12 remotes/origin/stable-2.12
```

## 编译delete-project插件

```
$ /opt/projects/gerrit/buck/bin/buck build plugins/delete-project:delete-project
```

1. Failed to read NDK version

```
BUILD FAILED: Failed to read NDK version from /home/prife/workplace/aosp/android-ndk-r12b
```

解决方法

```
$ echo "r12b (64-bit)" /home/prife/workplace/aosp/android-ndk-r12b/RELEASE.TXT
```

2. ndk错误

```
Not using buckd because watchman isn't installed.
[2016-12-29 16:49:00.265][error][command:98df922e-ab27-4d21-abc9-a3f1a3328a8a][tid:01][com.facebook.buck.cli.Main] Uncaught exception at top level
java.lang.IllegalStateException: /home/prife/workplace/aosp/android-ndk-r12b/toolchains/arm-linux-androideabi-4.8/prebuilt/linux-x86_64/arm-linux-androideabi/bin/as
        at com.google.common.base.Preconditions.checkState(Preconditions.java:173)
        at com.facebook.buck.android.NdkCxxPlatforms.getGccToolPath(NdkCxxPlatforms.java:548)
        at com.facebook.buck.android.NdkCxxPlatforms.getGccTool(NdkCxxPlatforms.java:560)
        at com.facebook.buck.android.NdkCxxPlatforms.build(NdkCxxPlatforms.java:315)
        at com.facebook.buck.android.NdkCxxPlatforms.getPlatforms(NdkCxxPlatforms.java:133)
        at com.facebook.buck.android.NdkCxxPlatforms.getPlatforms(NdkCxxPlatforms.java:102)
        at com.facebook.buck.rules.KnownBuildRuleTypes.createBuilder(KnownBuildRuleTypes.java:312)
        at com.facebook.buck.rules.KnownBuildRuleTypes.createInstance(KnownBuildRuleTypes.java:210)
        at com.facebook.buck.rules.KnownBuildRuleTypesFactory.create(KnownBuildRuleTypesFactory.java:49)
        at com.facebook.buck.rules.Cell.<init>(Cell.java:106)
        at com.facebook.buck.cli.Main.runMainWithExitCode(Main.java:653)
        at com.facebook.buck.cli.Main.tryRunMainWithExitCode(Main.java:1232)
        at com.facebook.buck.cli.Main.runMainThenExit(Main.java:1293)
        at com.facebook.buck.cli.Main.main(Main.java:1311)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:497)
        at com.facebook.buck.cli.bootstrapper.ClassLoaderBootstrapper.main(ClassLoaderBootstrapper.java:62)
```

解决方法是在gerrit目录下，编辑.buckconfig文件，最后添加

```
[ndk]
    gcc_version = 4.9

```

修复错误后，大概4分钟后编译完毕，生成的插件`delete-project.jar`位于`buck-out/gen/plugins/delete-project/`

## 安装插件

安装gerrit插件的方法有多种

### 手动安装方法
1. Download the delete-project.jar from the latest-ok or any other build of Gerrit.
2. Copy it to gerrit/plugins directory.
3. Restart Gerrit.
4. Optional Star this issue if you find it crazy that we are forced to build the plugins ourselves.

参考：http://stackoverflow.com/questions/21254291/how-to-install-delete-project-plugin-in-gerrit

### gerrit ssh命令行在线安装插件

暂时未尝试，参考文献[3]

# 参考文献

- [1] 编译buck：https://buckbuild.com/setup/install.html
- [2] buck运行出错: https://github.com/facebook/buck/issues/701
- [3] Delete projects in Gerrit：http://sychen.logdown.com/posts/2014/12/28/delete-project-in-gerrit

# 安装gitiles

注意，gitiles/src/main/resources/+Documentation有编译和配置文档，可参考。

```
$ cd plugins
$ git clone -c http.proxy=socks5://127.0.0.1:1080 https://gerrit.googlesource.com/plugins/gitiles
$ cd gitiles
$ git checkout -b stable-2.12 remotes/origin/stable-2.12
```

编译

```
$ cd ../
$ /opt/projects/gerrit/buck/bin/buck build plugins/gitiles:gitiles

编译完毕后，gitiles.jar位于buck-out/gen/plugins/gitiles/gitiles/目录下。
```

安装

```
ssh -p 29418 zhuzhongkai@10.20.40.19 gerrit plugin install -n xxxx.jar - < xxxx.jar
```

经测试，该方法会出现“fatal: remote installation is disabled”

因此最终还是登陆服务器，手动拷贝jar包安装。

安装完毕后，重启gerrit服务器，未做任何配置即可在gerrit中正常使用。

访问gerrit.googlesource.com需要翻墙，参考：https://segmentfault.com/q/1010000000118837

# 安装importer

```
仓库地址：https://gerrit.googlesource.com/plugins/importer/
```

编译、安装方法与上面相同。

复制仓库，假定有个仓库`droi/test/test1`，要创建一个完全相同的仓库`droi/test/test2`，使用如下命令

```
$ ssh -p 29418 zhuzhongkai@10.20.40.19 importer copy-project droi/test/test1 droi/test/test2
```

一个完整的示例如下。

```
$ ssh -p 29418 zhuzhongkai@10.20.40.19 importer copy-project freemeos/mt6750/ALPS-MP-N0.MP7-V1_DROI6755_66_N/pcb/freeme freemeos/common/freeme
Check source project:   100% (1/1)
Set parent project:     100% (1/1)
Check preconditions:    100% (1/1)
Open repository:        100% (1/1)
Persist parameters:     100% (1/1)
Configure repository:   100% (1/1)
Fetch project:          100% (1/1)
Configure project:      100% (1/1)
Replay Changes:         55
Created Changes: 55
```

拷贝完成后，原来的项目就可以删除了。使用这种方式，即可对已经创建成功的项目重命名。

另外，gerrit网站的projects | List Imports页面，可以看到当前执行复制的项目。
