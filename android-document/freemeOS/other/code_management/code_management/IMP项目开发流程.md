# 概述

本文描述IMP项目开发流程。一个IMP项目涉及多方，各自的角色分别是：

1. 第三方供应商，绘制PCB、并提供驱动，下文简称Customer
2. Freeme事业部/Freeme IMP，负责对接供应商，下文简称FreemeIMP
3. Freeme事业部/Freeme DEV，负责开发Freeme公版，下文简称FreemeDEV

本文主要介绍在IMP项目实施过程中，FreemeDEV组涉及的相关工作。

# IMP项目开发流程

## 阶段一、释放初版代码

该阶段，FreemeDEV组基于MTK（未来可能包括其他平台，如展讯、高通）某项目代码，整理后发布代码给IMP组。

### 1. 整理初版代码

使用repo命令获取某项目mtk分支代码，确保该项目已经合并最新的补丁。命令通常是：

**获取代码**

```
$ mkdir 50N
$ cd 50N
$ repo init --no-repo-verify -u ssh://zhuzhongkai@10.20.40.19:29418/FreemeOS/manifest -m ALPS-MP-N0.MP7-V1_DROI6755_66_N/mtk.xml 
$ repo sync
```

**创建分支**

创建droi_init分支

```
$ repo start --all droi_init
$ repo forall -pc git push origin droi_init
```

注意第二条命令需要操作者创建分支的权限，权限说明请参考`Android代码branch分支操作指南.md`

**清理代码**

根据项目需要清理代码，主要工作包括：

- 移除内部使用的build脚本
- 调整源代码目录结构
- 一些freeme客制化改动还原

保证使用Android原生命令可编译通过

**创建manifests文件**

然在`FreemeOS/manifest`仓库的相关项目目录下创建droi_init.xml，正确配置后提交。

### 2. 释放初版代码给`IMP`

整理完代码之后，编写邮件，通知IMP组通过droi_init.xml获取代码。

### 3. 释放初版代码给Customer

当前，通过将代码推送到我们的github企业账号上来释放给客户代码。由IMP组完成。这部分流程的具体操作、相关脚本工具、注意事项、操作文档等由他们负责。

## 阶段二、客户开发

在这一阶段，客户完成以下工作：

1. 绘制手机电路板、制作手机硬件
2. 驱动程序开发，测试手机驱动版本工作正常

在这个过程中，由IMP组对接客户需求。

客户完成手机原型开发以及驱动板调通之后，就可以进入下一阶段了。

## 阶段三、国内版封版

该阶段操作通过branch特殊分支，隔离IMP内部维护客户分支与FreemeDEV内部公版开发分支。

在阶段一中，我们提到，释放给客户的代码需要移除FreemeOS build脚本并调整源码目录结构。换言之，客户拿到的代码结构基本与Android原生类似，这跟FreemeOS项目代码结构差别较大。

FreemeOS源码树可以分为以下几个部分

1. 项目独有仓库，如build、framework、packages等等，即Android源代码主目录下第一级目录除去驱动目录以外的其他目录。
2. 驱动仓库
    - kernel
    - vendor
    - device/droi
    - device/mediatek
3. FreemeOS common仓库
    - vendor/freeme
    - vendor/freeme/packages/apps/FreemeSettings
    - vendor/freeme/packages/apps/FreemeSystemUI
    - vendor/freeme/packages/apps/FreemeMms
    - vendor/freeme/packages/apps/FreemeDeskClock
    - 其他app使用release分支
4. 其他仓库
    - vendor/freeme/packages/apps/freemelite
    - vendor/freeme/packages/apps/security
    - vendor/partner_gms
    - vendor/freeme/packages/3rd-apps

在branch量产分支（production分支）时，针对这四部分区别如下：

1. 项目独有仓库，从fremeeos内部开发分支branch，分支名为`production`即可
2. 驱动仓库
    - kernel、vendor仓库从droi_init分支branch，分支名为`production`
    - device/droi、device/mediatek可从内部的customer_driver分支上branch，分支名为`production`
3. FreemeOS common仓库，从fremeeos内部开发分支branch，分支名需要为`项目-production`
4. 其他仓库，不branch，使用各自内部开发分支

由于各个子仓库的差异性，不建议参考`Android代码branch分支操作指南`统一branch，这里提供一个简单的分支脚本，名为branchhelper.sh，源码见附录，供定制使用

以6750项目创建量产分支为例，命令如下

```
$ branchhelper.sh b production
$ branchhelper.sh ba 6750-production
```

驱动涉及的四个仓库由手动操作分支并推送。分别以`vendor`和`device/droi`

```
$ cd vendor
$ git checkout -b production origin/droi_init
$ git push origin production

$ cd ../device/droi
$ git checkout -b production origin/custormer_driver
$ git push origin production
```

PS. vendor的量产分支需要从freemeos内部开发分支合并少许提交才能正确编译。

分支创建完毕之后，在`FreemeOS/manifest`仓库的相关项目目录下创建producntion.xml，正确配置后提交。编写邮件通知IMP组基于此分支合并驱动代码，该分支后续由他们维护。

可参考：FreemeOS/manifest仓库下的ALPS-MP-N0.MP7-V1_DROI6755_66_N/production.xml配置

## 阶段四、FreemeOS合并驱动

这一阶段，首先由IMP组同事从客户那里获取驱动，并以FreemeOS项目源代码结构上合并驱动，提交到production.xml描述的各个分支上，并完成验证。

# 附录

## branchhelper.sh源代码

```
#!/bin/bash

aosp_projects=(
./build
./tools
./dalvik
./platform_testing
./development
./bionic
./abi
./toolchain
./hardware
./device
./system
./sdk
./libnativehelper
./art
./docs
./packages
./frameworks
./developers
./prebuilts
./cts
./bootable/recovery
./libcore
./pdk
./ndk
./external
)

isolated_apps=(
vendor/freeme/packages/apps/FreemeSettings
vendor/freeme/packages/apps/FreemeSystemUI
vendor/freeme/packages/apps/FreemeMms
vendor/freeme/packages/apps/FreemeDeskClock
)

function push_branch() {
    local name=$1
    local HERE=$(pwd)
    for i in ${aosp_projects[*]}; do
        echo "branch $i: $name"
        cd $i
        git checkout -b $name
        git push origin $name
        cd $HERE
    done
}

function push_apps_branch() {
    local name=$1
    local HERE=$(pwd)
    for i in ${isolated_apps[*]}; do
        echo "branch $i: $name"
        cd $i
        git checkout -b $name
        git push origin $name
        cd $HERE
    done
}

action=$1
shift

case $action in
    b|branch)
        push_branch $1
        ;;
    ba|branch_app)
        push_apps_branch $1
        ;;
    *)
        echo "error: unsupport action!"
        ;;
esac
```