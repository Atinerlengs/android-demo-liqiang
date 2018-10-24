# repo项目patch合并操作指南

[TOC]

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

## 1. patch文件备份

### 首次创建仓库

注意修改yourname为你的名字，有两处。

```
$ git clone ssh://yourname@10.20.40.19:29418/freemeos/mt6750/ALPS-MP-N0.MP7-V1_DROI6755_66_N/patch && scp -p -P 29418 yourname@10.20.40.19:hooks/commit-msg patch/.git/hooks/
```

### 提交

示例如下。

```
$ cd your-patch-git-dir
$ git pull
$ cp the-patch-zip-files-from-mtk ./
$ git add the-patch-zip-files-from-mtk
$ git commit -m "[patch/backup] ALPS-MP-N0.MP7-V1_DROI6755_66_N: modem p3-p5, ALPS p3"
$ git push origin HEAD:refs/for/master
```

## 2. mtk版本合并patch

为了减少干扰，请使用完全干净的源码树执行patch合并操作。

### 拉取代码

首次合并patch，请拉取代码树，示例代码如下

```
$ mkdir alsp_50n_mtk
$ cd alsp_50n_mtk
$ repo init --no-repo-verify -u ssh://yourname@10.20.40.19:29418/freemeos/manifest -m ALPS-MP-N0.MP7-V1_DROI6755_66_N/mtk.xml
$ repo sync
$ repo start --all mtk
```

如果已经有这样的一棵树，请更新这棵树，保证代码最新。

```
$ repo sync
```

### 合并patch

创建新的本地分支`mtk`用于执行本次的`mtk`合并

```
$ repo start --all `mtk`
```

创建`patch临时工作目录`用于合并patch，笔者在`pwork`，请根据自己喜好修改。

```
$ mkdir pwork
```

执行patch合并操作

```
$ cd your-mtk-repo-code # 请根据实际情况修改
$ repopatch.sh mtk ../patch/ALPS03108431\(For_droi6755_66_n_alps-mp-n0.mp7-V1_P7\).tar.gz pwork
tar patch files... 
override patch files... 
loop modified projects...
patch device/droi
patch frameworks
patch packages
patch system
patch vendor
create change log file
log: create P7_ALPS03108431.txt
```

执行完毕后，`patch临时工作目录`下会生成

```
- changelog/: 用于存放patch合并信息文件，
- P7_ALPS03108431/: 本patch包解压目录，类似目录可能有多个
```

如果还有其他补丁包，请重复使用上述命令。

### 补丁较多

如果补丁较多，重复输入命令枯燥无趣，可以使用`gen.py`自动生成补丁合并脚本。将`gen.py`也链接到`～/bin`目录下（方法参见本文开头）。然后执行

```
$ gen.py ../patch tempdir > run.sh
$ cat run.sh
#!/bin/bash
repopatch.sh mtk "../patch/ALPS03039046(For_droi6755_66_n_alps-mp-n0.mp7-V1_P1).tar.gz" tempdir
repopatch.sh mtk "../patch/ALPS03076119(For_droi6755_66_n_alps-mp-n0.mp7-V1_P2).tar.gz" tempdir
repopatch.sh mtk "../patch/ALPS03108339(For_droi6755_66_n_alps-mp-n0.mp7-V1_P3).tar.gz" tempdir
...
repopatch.sh mtk "../patch/ALPS03144425(For_droi6755_66_n_alps-mp-n0.mp7-V1_P24).tar.gz" tempdir
repopatch.sh mtk "../patch/ALPS03144423(For_droi6755_66_n_alps-mp-n0.mp7-V1_P25).tar.gz" tempdir
```

编辑run.sh，删除已经合并的补丁行。然后运行

```
$ bash run.sh
```

### 检查是否有遗漏（可选步骤，可跳过）

查看当前的改动，添加`-o`选项，如果有未被任何git仓库跟踪的文件也列出来（例如.repo目录目录下新增文件或目录）

```
$ repo status -o
```

如果有新增文件或目录（概率很小，暂时还没遇到过），那么需要具体分析，请联系本作者。

再次确认是否遗漏文件，命令

```
$ repo forall -c git status --ignored
```

如果有，那么手动进入该目录下，执行`git add -f .`，`git commit --amend` 后直接保存退出vim窗口。

**说明**：`git add`配合`-f`参数，可以强制添加所有文件（即使是在.gitignore被忽略的文件）。

### 上传

上传代码到服务器

```
$ repo upload
```

注意，当一次合并较多patch时，可能会出现如下警告，这是repo监测到个别仓库提交较多发出的提醒，直接输入`yes`即可

```
ATTENTION: One or more branches has an unusually high number of commits.
YOU PROBABLY DO NOT MEAN TO DO THIS. (Did you rebase across branches?)
If you are sure you intend to do this, type 'yes': yes
```

### gerrit上合并提交

打开gerrit网页，http://10.20.40.19:8080，登陆，确认代码提交成功。

**注意**

合并一个patch后，提交到gerrit上，可以看到，该patch会分拆成多个提交，包括：

1. 一个或多个源代码目录git提交，提交信息固定为：`[patch/apply] ALPS03614759(For_droi6755_66_n_alps-mp-n0.mp7-V1_P83)`
2. 一个变更日志提交，该提交总是提交到device_mediatek仓库，提交信息为：`[patch/log] add P83_ALPS03614759.txt`

这两部分构成了一个patch提交。如果一次合并多个patch之后执行repo upload提交，在gerrit上以git仓库的上传顺序提交，请merge patch的leader务必按照patch顺序合并，以P83为例子，将P83的所有提交全部merge，并保证最后merge `[patch/log] add P83_ALPS03614759.txt`提交。然后再merge P84、P85...。

请务必按照此顺序合并提交，因为如果后续发现patch有bug需要回退提交测试，需要保证个patch被完整回退。我们开发的repo roll回退脚本依赖该顺序。

## 3. 将patch合并到pcb分支

### 修改manifests

为了减少干扰，请重新拉取一套`pcb_oversea`仓库执行patch合并操作。

创建新的本地分支`pcb_oversea`用于执行本次的patch合并

```
$ repo start --all pcb
```

### 从changelog文件中合并patch

例如，合并P7补丁，命令如下，修改`pwork`为你刚才在mtk仓库合并patch时指定的`patch临时工作目录`，

```
$ cd your-pcb-repo-code # 请根据实际情况修改
$ repopatch.sh droi 刚才mtk仓库合并patch时生成的pwork目录/changelogs/P7_ALPS03108431.txt
loop patched projects...
pick 0a8e656a13740a8e15b79a4635ea95c7200cee03 from device/droi
picked!
pick c923c744872e5dd667696de82c1b0ce7764e8b24 from frameworks
picked!
create change log file
log: create P7_ALPS03108431.txt
```

如果没有任何冲突，效果如上面所示。

如果运气不好，个别仓库合并失败，合并脚本会继续执行，示例如下：

```
$ repopatch.sh droi pwork/changelogs/P8_ALPS03128418.txt
loop patched projects...
pick f60a45baf19182f79123778a2ba09ab7042fec96 from bionic
picked!
pick 849b7b693aaf0b0753f98da8857c0a4d72bd0b2e from bootable/recovery
error: could not apply 849b7b6... [patch/apply] ALPS03128418(For_droi6755_66_n_alps-mp-n0.mp7-V1_P8)
hint: after resolving the conflicts, mark the corrected paths
hint: with 'git add <paths>' or 'git rm <paths>'
Recorded preimage for 'mt_recovery.cpp'
error: pick conflict, please fix it later, now we just skip
pick b0287947170328802040e6b6e6482b404c44004b from device/droi
picked!
...
create change log file
log: create pwork/changelogs_new/P8_ALPS03128418.txt
```

冲突的仓库会以红色高亮显示。请手动修复冲突：进入该仓库，执行`git mergetool`执行三方合并。

```
$ git config --global merge.tool meld
$ git mergetool
```

手动处理完冲突之后，执行

```
$ git add -u
$ git commit -m "[patch/apply] ALPS03128418(For_droi6755_66_n_alps-mp-n0.mp7-V1_P8)"
```

修复冲突之后，请更新变更日志，该命令会自动完成并更日志的本地git提交。注意，下面命令中，pwork后跟`changelogs_new`，而非`changelogs`

```
$ cd your-pcb-repo-code # 请根据实际情况修改
$ repopatch.sh logupdate 刚才mtk仓库合并patch时生成的pwork目录/changelogs_new/P8_ALPS03128418.txt
```

如果还有其他补丁包，请重复使用上述命令。

### 上传

上传代码到服务器

```
$ repo upload
```

### 代码合并

在gerrit上将代码合并到仓库中。

### 本地分支清理

删除本地分支

```
$ repo abandon mtk
$ repo abandon pcb_oversea
```