# 部署AOSP代码到Gerrit服务器操作指南

[TOC]

## 概述

本文以案例方式演示如何将mtk提供的Android项目代码部署到`gerrit`上。

以AndroidN 6737M上线为例。步骤如下。

## 获取权限

本文操作需要操作人员具有相关权限，属于`gerrit`的代码管理组（`cm`组）。

如果你不具有`cm`组权限，请具有`cm`权限的人帮忙将你添加进入该组。添加方法是打开`gerrit`，在`People` 进入`List Groups`，点击`cm`，将自己的名字加入。

## 安装/更新脚本

本文操作需要使用脚本工具，请确认获取了最新的脚本/文档仓库，并正确配置`PATH`。

### 首次安装

1. 克隆脚本/文档仓库

    修改zhuzhongkai为你的名字，注意有两处。

        $ cd ~
        $ git clone ssh://zhuzhongkai@gitlab.droi.com:29418/freemeos/common/documents freeme-documents && scp -p -P 29418 zhuzhongkai@gitlab.droi.com:hooks/commit-msg  freeme-documents/.git/hooks/

2. 添加脚本目录到`PATH`中

        $ gedit ~/.profile

    在最后添加一行

        PATH="$HOME/freeme-documents/scripts:$PATH"

    保存退出。

    测试下配置是否正确，打开终端，执行

        $ . ~/.profile
        $ repopatch.sh
        fail: unknown subcommand! ; 出现该提示，则表明配置成功

        repopatch.sh: command not found； 出现该提示，则表明配置失败，请根据上述步骤排查。

    配置成功后，请注销用户并重新登陆（或者重启计算机）重新加载.profile。

    PS. 如使shell为`zsh`，请执行`hash -r`，当前终端即可直接执行`repopatch.sh`

### 非首次安装

因为文档仓库经常更新，建议每次使用时前更新仓库，方法如下

$ cd ~/freeme-documents
$ git pull

## 代码准备

将MTK提供的代码拷贝到工作目录中。笔者目录如下

```
$ cd ~/workplace/freemeos/droi6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N
$ ls -lh
total 12G
-rw-rw-r--  1 prife prife 1000M 11月  4 23:35 ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N_INHOUSE.tar.gz00
...(省略类似文件)...
-rw-rw-r--  1 prife prife  894M 11月  4 23:37 ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N_INHOUSE.tar.gz11
-rw-rw-r--  1 prife prife  145M 11月  5 09:38 ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N_KERNEL.tar.gz
-rw-rw-r--  1 prife prife  1.2K 11月  5 09:35 ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N.md5
-rw-rw-r--  1 prife prife   11M 11月  4 20:40 DROI6737M_65_N(C2K_SVLTE_V2)(C2K_SVLTE_OM_V2)_SIXTH.CBP.MD.MP2.V63.5.tar.gz
drwxrwxr-x  2 prife prife  4.0K  2月  9 11:32 Patch
-rw-rw-r--  1 prife prife   222  2月  9 11:34 readme_to_rename.txt
-rw-rw-r--  1 prife prife  185K 11月  5 09:36 ReleaseNote_for_MT6737_alps-mp-n0.mp1.xlsx
-rw-rw-r--  1 prife prife  227M 11月  4 20:40 X.tar.gz
```

打开`ReleaseNote_for_MT6737_alps-mp-n0.mp1.xlsx`，根据Readme标签页的`Steps for building project`选项操作解压代码，步骤如下。

```
$ tar xf ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N_KERNEL.tar.gz 
$ cat ALPS-MP-N0.MP2-V1_DROI6580_WE_N_INHOUSE.tar.gz* | tar xf - 
```
执行完毕后，生成`alps`目录，也就是AOSP项目代码（含kernel，不含modem）。

## 准备manifests文件

如果本机没有`manifest`的`git`仓库，那么请`clone`该仓库。注意修改`zhuzhongkai`为你的名字。

```
$ cd ~/workplace/freemeos
$ git clone ssh://zhuzhongkai@10.20.40.19:29418/freemeos/manifest && scp -p -P 29418 zhuzhongkai@10.20.40.19:hooks/commit-msg manifest/.git/hooks/
$ tree manifest
manifest
├── ALPS-MP-N0.MP2-V1_DROI6580_WE_N
│   ├── _common.xml
│   ├── driver.xml
│   ├── modem_mtk.xml
│   ├── modem_pcb.xml
│   └── mtk.xml
├── ALPS-MP-N0.MP7-V1_DROI6755_66_N
│   ├── _common.xml
│   ├── driver.xml
│   ├── modem_mtk.xml
│   ├── modem_pcb.xml
│   ├── mtk.xml
│   └── pcb_oversea.xml
└── test.xml
```

可见gerrit上已经上线的AndroidN项目包括`6780`、`6755`，讲`6580`项目目录复制为`6737`目录，命令如下。

```
creategit.sh clone mt6580 ALPS-MP-N0.MP2-V1_DROI6580_WE_N mt6737 ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N
```

## 构造repo工程

首先命令行切换目录到`ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N`项目源代码目录（解压后），命令如下:

```
$ cd ~/workplace/freemeos/droi6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N
```

### 0. 生成辅助文件

```
creategit.sh s0 ~/workplace/freemeos/manifest/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/_common.xml
```

该命令对比`_common.xml`和当前源代码目录结构，其输出类似如下效果：

```
diff xml with workplace start...
####  check extra git projects in mainifest.xml
 warning: < trusty > is not existed

####  check extra directories in  /home/prife/workplace/freemeos/droi6755/pcb
 warning: < kernel-4.4 > has no git project
diff xml with workplace over
```

上面的含义表示：当前源代码目录比_common.xml中多了个`kernel-4.4`，少了个`trusty`，请继续编辑`_common.xml`文件，删除多出的目录项，补足缺少的目录项。
然后继续执行上面命令直到输出为如下效果：

```
diff xml with workplace start...
####  check extra git projects in mainifest.xml

####  check extra directories in  /home/prife/workplace/freemeos/droi6755/pcb
diff xml with workplace over
```

执行完成之后，在该目录下生成三个文件：droi.xml，project.list，project.gerrit，是后续工作的必要文件。

### 1. 创建本地git仓库并添加代码

```
creategit.sh s1
```

根据manifest.xml中执行的本地git仓库路径创建本地git仓库，并做本地提交。

本步执行总时间大概20分钟（机械硬盘）。

本步执行完毕之后，会打印如下提示，请检查是否存在嵌套git仓库。当前仓库git分拆方案中，`device/`下的`droi/`、`mediatek/`都是独立的`git`仓库。

```
Now, you may fix the git repo which has sub git repos, such as "device/", run:
      git rm --cached <your-sub-git-dirs>;
      echo "<your-sub-git-dirs>" >> .gitignore
      git add .gitignore
      git commit -m "[freeme] ignore xxx"
```

根据提示执行以下命令

```
$ cd device
$ git rm --cached droi mediatek
$ echo "/droi/" >> .gitignore
$ echo "/mediatek/" >> .gitignore
$ git add .gitignore
$ git commit -m "[freemeos/bringup] ignore droi/ mediatek/"
$ cd ../
```

### 2. 在gerrit服务器上创建projects

```
creategit.sh s2
```

本步执行完毕之后，打开gerrit服务器，进入`Porjects | list`页面，查看刚才创建的仓库，刚才创建的仓库全部属于`mt6737`项目，因此在`Filter`编辑框输入mt6737过滤查看。务必确保仓库路径创建正常。

本步执行总时间不到一分钟。

### 3. 推送代码到服务器

本步骤执行时间较长，请执行如下命令配置git压缩并发进程数以加快速度。笔者机器是四核八线程，配置`6`，请根据自己的机器实际情况配置（16核32线程，配置`8`后效果提升明显）。

```
git config --global pack.threads 6
```

```
creategit.sh s3
```

这一步将本地git仓库代码推送到gerrit服务器上。

本步总执行时间在1~2小时左右（机械硬盘）。本次执行完毕后注意观察全部输出日志，检查是否有异常语句，具体参见本节最后。

### 4. 在`gerrit`服务器上`review`提交

```
creategit.sh s4
```

本步执行时间大概2～3分钟。执行完完毕后检查命令输出确保无错误。并检查gerrit网站的All | Merged页面的提交记录，确保没有遗漏的提交。

至此仓库创建主要工作已经完成。注意此时服务器上还只有`master`分支。

## 测试验证

流程如下：

1. 使用`repo`拉取代码验证编译验证。
2. 如果有patch，合并patch，然后编译验证
2. 创建mtk分支，并提供manifest的mtk.xml
3. 创建driver分支，并提供manifest的driver.xml

下面具体介绍

### 提交manifest

```
$ cd ~/workplace/freemeos/manifest
$ cd ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N
$ vim mtk.xml 
将mtk分支修改为master分支后保存（因为此时mtk分支还未创建）
$ git add _common.xml mtk.xml
$ git commit -m "[mainfest] add ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N"
$ git push origin HEAD:refs/for/master
```

### 拉取代码

注意修改`zhuzhongkai`为你的名字。

```
$ mkdir 6737-n
$ repo init --no-repo-verify -u ssh://zhuzhongkai@10.20.40.19:29418/freemeos/manifest -m ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/mtk.xml  
$ repo sync
$ repo start --all master
```

### 执行编译

```
$ . build/envsetup.sh
$ lunch full_droi6735_36g_n-userdebug
$ make -j10
```

正常情况下，项目即可编译通过。否则要排查错误（是否创建git仓库漏提交文件、是否有些git push到gerrit上失败、是否有遗漏的review等等）

如果编译通过，那么继续下一步。

### 合并patch

如果有patch，此时合并patch，方法参见相关文档。合并patch后继续执行编译并验证。

### 创建mtk分支

创建本地mtk分支

```
$ repo start --all mtk
```

将本地mtk分支推送到gerrit服务器上

```
$ repo forall -pc git push origin mtk
```

执行完毕后前往gerit网页上，进入相关仓库，查看branches页面，是否有mtk分支。

进入manifests仓库修改`mtk.xml`，将master修改为`mtk`，并提交。具体方法参考本节开头**提交manifest**。

### 创建driver分支

方法与创建mtk分支同理。创建完毕后注意在manifest仓库提交`driver.xml`。

## 发布代码

创建项目目录，并合并必要代码。测试完毕后即可发布邮件，请驱动组开始工作。

# 异常处理

实际运行中`s3`步可能出现以下错误信息，这是由于`external/`，`prebuilts/`，`vendor/`三个仓库较大导致的。

从实际测试来看，虽然提示失败，但是其实已经推送到gerrit服务器上了。后续的`s4`步骤依然可以review通过，review后可以在gerrit网页的All | Merged页面看到全部记录。

```
remote: error: internal error while processing changes
To ssh://10.20.40.19:29418/freemeos/mt6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/pcb/external.git
 ! [remote rejected] HEAD -> refs/for/master (internal error)
error: failed to push some refs to 'ssh://zhuzhongkai@10.20.40.19:29418/freemeos/mt6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/pcb/external.git'
...

remote: error: internal error while processing changes
To ssh://10.20.40.19:29418/freemeos/mt6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/pcb/prebuilts.git
 ! [remote rejected] HEAD -> refs/for/master (internal error)
error: failed to push some refs to 'ssh://zhuzhongkai@10.20.40.19:29418/freemeos/mt6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/pcb/prebuilts.git'

...

remote: error: internal error while processing changes
To ssh://10.20.40.19:29418/freemeos/mt6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/pcb/vendor.git
 ! [remote rejected] HEAD -> refs/for/master (internal error)
error: failed to push some refs to 'ssh://zhuzhongkai@10.20.40.19:29418/freemeos/mt6737/ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N/pcb/vendor.git'
##### push gerrit projects over ###
```
