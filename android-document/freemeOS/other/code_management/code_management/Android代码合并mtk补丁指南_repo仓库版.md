# repo项目patch合并操作指南（mtk repo仓库版）

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

PS. 如使shell为`zsh`，请执行`hash -r`，当前终端即可直接执行`repopatch.sh`

## 代码管理概述

MTK不再提供补丁包，而是使用repo方式发布代码，新的patch直接推送到其搭建的repo服务器。我们需要持续维护一套MTK的repo仓库。当mtk有更新patch时，只要使用repo sync更新即可。

MTK的repo仓库分拆了1100多个git子仓库，我司内部将其合并为30个左右的git仓库（基本按照android源代码的第一级目录分拆管理）。之所以这么处理，原因是：

1. 仓库拉取时间极长。验证发现，在我司gerrit服务器上使用MTK同样的代码仓库，即部署1100个git子仓库，从gerrit服务器拉取一套代码的时间极大增加，最长长达3小时。
2. 代码提交繁琐。由于子git分拆过细，导致驱动组合并代码时，都需要新创建git仓库。操作非常不便。

## 1. mtk版本合并patch

### patch原理

当mtk有patch更新时：

1. 首先将mtk repo源码树更新到最新。此时mtk repo代码树将包含patch代码。
2. 下载一份对应的freemeos-mtk分支的repo源码树
3. 将mtk repo源代码树的.repo目录移动到其他位置保存，注意这一步很重要，因为重新下载一套mtk repo需要40个小时以上，因此需要妥善保存好。同时将mtk repo仓库目录下的所有.git仓库删除
4. 将freemeos源码树下的.repo目录、以及所有子目录下的.git移动到原mtk repo源码树对应各个目录下。

经过上述操作，此时`mtk`源代码树就实际上变身为`freemeos`的代码树，同时工作目录中含有了`mtk`源码树的最新patch。利用repo命令将所有存在改动的git仓库做本地提交并上传我司服务器即可。

注意：根据上面的操作，patch合并完成后，两棵代码树都会被破坏。鉴于mtk repo的重要性，需要将其复原。这个在下文专门一节说明。

### 拉取mtk repo代码

请拉取代码树，具体命令请参考mtk提供的文档。

如果已有mtk仓库，可使用如下命令更新。

```
$ repo sync -c -f -j8 --no-clone-bundle
```

#### 更新仓库

### 拉取freemeos repo仓库mtk分支

为了减少干扰，请使用完全干净的源码树执行patch合并操作。

以`droi6757_n1`为例

```
$ mkdir droi6757_n1
$ cd droi6757_n1
$ repo init --no-repo-verify -u ssh://yourname@10.20.40.19:29418/freemeos/manifest -m ALPS-MP-N1.MP5-V1.61_DROI_TK6757_66_N1/mtk.xml
$ repo sync
$ repo start --all mtk
```

如果已经有这样的一棵树，请更新这棵树，保证代码最新。

```
$ repo sync
```

### 合并patch之step1 仓库变身

开始执行真正的动作之前，先说明笔者代码目录结构，如下：

```
~/work/mtk_git
├── droi_57_N1， droi6757 n1代码 mtk分支，下文可能简称为freemeos代码树
└── mtk_57_N1， mtk repo n1代码，下文可能简称为mtk代码树
```

创建`patch临时工作目录`用于合并patch，笔者使用`pwork`，你可以根据自己喜好修改。

```
$ cd mtk_57_N1
$ mkdir ../pwork
```

除非明确说明，本文之后所有命令均在`mtk_57_N1`路径上执行。请读者知悉。


#### 创建`project.skip`文件

mtk提供的repo仓库还包含了一些无关代码，为了减少代码体积，在代码部署到gerrit时将他们删除了。可以创建`project.skip`文件，在接下来的仓库变身时自动删除这些目录。命令如下：

```
$ echo "./device/asus/
./device/google/
./device/htc/
./device/huawei/
./device/intel/
./device/lge/
./device/linaro/
./device/moto/
./prebuilts/android-emulator/
./prebuilts/qemu-kernel/" > project.skip
```

#### repopatch.sh git_mtk_move

在合并patch过程中，会将mtk代码树下的.repo仓库移动到`patch临时工作目录`下，并重命名为`back_dot_repo`，请务必保证该工作目录下没有`back_dot_repo`，否则会报错。

接下来执行仓库变身操作，这是合并patch的最重要一步，子命令为`git_mtk_move`，之后还有五个参数，如下所示

```
$ repopatch.sh git_mtk_move \
.repo/manifest.xml \
. \
../droi_57_N1/.repo/manifests/ALPS-MP-N1.MP5-V1.61_DROI_TK6757_66_N1/_common.xml \
../droi_57_N1 \
../pwork
```

这五个参数的含义如下：

1. `.repo/manifest.xml`，指向mtk仓库的manifests.xml
2. `.`，指当前mtk代码树路径
3. `../droi_57_N1/.repo/manifests/ALPS-MP-N1.MP5-V1.61_DROI_TK6757_66_N1/_common.xml`，指向freemeos仓库的`manifests.xml`
4. `../droi_57_N1`，指向freemeos代码树路径
5. `../pwork`，指向patch临时工作目录

其中参数1-2用于在mtk代码树路径中生成辅助的配置文件、参数3-4用于在droi代码树目录下生成辅助的配置文件。

本命令执行以下工作：

1. 分别解析两个repo仓库的xml文件，生成辅助文件，其中记录各个子git仓库路径和
2. 将mtk代码树下的.repo仓库移动到`patch临时工作目录`下，并重命名为`back_dot_repo`，删除mtk代码树下所有.git目录
3. 移动freemeos代码树下.repo和所有.git目录到mtk代码树对应目录下
4. 如果mtk代码树目录下存在`project.skip`，则将文件中的所有子目录删除。

请务必仔细阅读本命令的打印日志，确认该命令正确执行。

如果你在执行上面命令时看到以下输出，那么请手动删除这些目录。

```
should you remove the following projects under your-mtk-git-directory?
```

请检查是否创建了`project.skip`文件。

该步骤执行成功后，接下需要本地提交patch。

### 合并patch之step2 本地提交

请提供本次patch的名称，由于MTK不再提供补丁包，也就没有正式的补丁名称。假如本次更新的是P6补丁，那么将patch名称设计为：

```
For_Droi6757_n1_mp5_n1-V1.61_P6
```

请执行

```
$ repopatch.sh git_mtk_commit For_Droi6757_n1_mp5_n1-V1.61_P6 ../pwork
```

该命令子命令为`git_mtk_commit`，随后带两个参数：

1. `patch-message`，即本次patch的全称，该信息将会作为git commit的提交信息。
2. `../pwork`，patch临时工作目录

该命令执行时间较长，大概在10分钟～30分钟之间，这段时间可以处理其他事情。

执行完毕后，`patch临时工作目录`（本例中即pwork目录）下会生成

```
- changelog/: 用于存放patch合并信息文件，
- back_dot_repo/：原mtk源码树下的.repo目录
```

至此，patch已经在本地合并完成。接下来将修改推送到服务器。

### 上传

上传代码到服务器

```
$ repo upload
```

### gerrit上合并提交

打开gerrit网页，http://10.20.40.19:8080，登陆，确认代码提交成功，将代码review并submit到仓库中。

### 合并patch之step3 复原仓库

本次补丁合并完成后，需要将仓库复原，以方便以后patch合并，命令如下

```
$ repopatch.sh git_mtk_restore . ../droi_57_N1 ../pwork
```

该命令子命令为`git_mtk_restore`，随后带三个参数：

1. `.`，指向mtk代码树路径
2. `../droi_57_N1`，指向freemeos代码树路径
2. `../pwork`，patch临时工作目录

该命令执行完毕后，代码树复原。 

freemos代码树的使命已经完成，由于我司内部重新下载freemeos代码树极快，请自行决定。

## 3. 将patch合并到pcb分支

### 拉取pcb仓库

拉取freemeos pcb分支或pcb_oversea仓库。

```
$ repo start --all pcb_oversea
```

### 从changelog文件中合并patch

例如，合并P6补丁，命令如下，修改`pwork`为你的`patch临时工作目录`

```
$ repopatch.sh droi ../pwork/changelogs/For_Droi6757_n1_mp5_n1-V1.61_P6.txt
loop patched projects...
pick 0a8e656a13740a8e15b79a4635ea95c7200cee03 from device/droi
picked!
pick c923c744872e5dd667696de82c1b0ce7764e8b24 from frameworks
picked!
create change log file
log: create For_Droi6757_n1_mp5_n1-V1.61_P6.txt
```

如果没有任何冲突，效果如上面所示。

如果运气不好，个别仓库合并失败，合并脚本会继续执行。

```
$ repopatch.sh droi ../pwork/changelogs/For_Droi6757_n1_mp5_n1-V1.61_P7.txt
loop patched projects...
pick f60a45baf19182f79123778a2ba09ab7042fec96 from bionic
picked!
pick 849b7b693aaf0b0753f98da8857c0a4d72bd0b2e from bootable/recovery
error: could not apply 849b7b6... [patch/apply] For_Droi6757_n1_mp5_n1-V1.61_P8
hint: after resolving the conflicts, mark the corrected paths
hint: with 'git add <paths>' or 'git rm <paths>'
Recorded preimage for 'mt_recovery.cpp'
error: pick conflict, please fix it later, now we just skip
pick b0287947170328802040e6b6e6482b404c44004b from device/droi
picked!
...
create change log file
log: create pwork/changelogs_new/For_Droi6757_n1_mp5_n1-V1.61_P8.txt
```

冲突的仓库会红色高亮日志打印出来，请手动修复冲突：进入该仓库，执行`git mergetool`执行三方合并。

```
$ git config --global merge.tool meld
$ git mergetool
```

手动处理完冲突之后，执行

```
$ git add -u
$ git commit -m "[patch/apply] For_Droi6757_n1_mp5_n1-V1.61_P8"
```

如果有冲突，修复冲突之后，请更新变更日志，没有冲突请跳过此步。

```
$ repopatch.sh logupdate pwork/changelogs_new/For_Droi6757_n1_mp5_n1-V1.61_P8.txt
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
$ repo abandon pcb_oversea
```