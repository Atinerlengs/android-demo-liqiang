# FreemeOS驱动初版操作指南

[TOC]

## 概述

本文以案例方式演示如何从驱动分支创建第一个主板项目，以`AndroidN 6739N`为例，步骤如下。

操作之前，请先认真阅读以下文档：

- [【部署AOSP代码到Gerrit服务器操作指南】](http://10.20.40.17:8080/plugins/gitiles/freemeos/common/documents/+/refs/heads/master/code_management/Android%E4%BB%A3%E7%A0%81%E9%83%A8%E7%BD%B2gerrit%E6%8C%87%E5%8D%97.md)
- [【cp组新增项目操作指南】](http://10.20.40.17:8080/plugins/gitiles/freemeos/common/documents/+/refs/heads/master/code_management/cp%E7%BB%84%E6%96%B0%E5%A2%9E%E9%A1%B9%E7%9B%AE%E6%93%8D%E4%BD%9C%E6%8C%87%E5%8D%97.md)

## 部署代码及创建分支

如 `mtk` 代码未部署，或者驱动分支未创建，则参考文档 [【部署AOSP代码到Gerrit服务器操作指南】](http://10.20.40.17:8080/plugins/gitiles/freemeos/common/documents/+/refs/heads/master/code_management/Android%E4%BB%A3%E7%A0%81%E9%83%A8%E7%BD%B2gerrit%E6%8C%87%E5%8D%97.md)。
否则直接进行下一步。

## 创建主板项目

### 导入freeme编译框架

1. 在 `manifast` 添加 `vendor/freeme` 驱动分支（具体咨询项目负责人，国内与海外不同），并重新`repo sync`

2. 添加 `freeme ` 环境依赖

        在 build/envsetup.sh 文件最后加入以下代码：
        # @{ freeme.biantao, 20161226. FreemBuild.
        . vendor/freeme/build/envsetup.sh
        # @}

3. 执行 `source build/envsetup.sh `，成功索引情况如下

        $ source build/envsetup.sh
        ***
        including vendor/freeme/build/bash_completion/git.bash
        including vendor/freeme/build/bash_completion/repo.bash

### 新建gerrit主板工程

在 `gerrit` 新建主板工程，详细操作见[【cp组新增项目操作指南】](http://10.20.40.17:8080/plugins/gitiles/freemeos/common/documents/+/refs/heads/master/code_management/cp%E7%BB%84%E6%96%B0%E5%A2%9E%E9%A1%B9%E7%9B%AE%E6%93%8D%E4%BD%9C%E6%8C%87%E5%8D%97.md)

> 具体的主板名称要跟项目负责人确认

修改项目的 `manifast` [新增主板工程](http://10.20.40.17:8080/#/c/34294/1/ALPS-MP-N1.MP18-V2_DROI6739_66_N1/driver.xml)，并 `repo sync`

### 克隆主板

#### 老平台

确保上述文件均配置正常后，执行以下操作

        $ source build/envsetup.sh
        $ clone_project droi6739_36_n1<mtk默认主板> pu6a<目标主板>

修改`<device_droi>/common/project.ini ` 文件[新增主板](http://10.20.40.17:8080/#/c/31625/1/common/project.ini) (BOARD模块)

#### 新平台(mt6757 N1以后)

1. 从 mtk原始项目clone，需要带相对路径：`clone_project device/mediateksample/tk6757_66_n1 tc571`
2. clone项目时，`vendor/mediateksample/libs/[tk6757_66_n1|tc571|...] `这个目录需要新加仓库。

> 注：可执行 ```$ remove_project pu6a<目标主板>``` 测试移除主板功能是否正常

### 编译

使用 `aosp` 原生编译命令编译新增的主板项目
```
$ source build/envsetup.sh
$ lunch <目标主板-userdebug>
$ make -j12
```
如果编译成功则继续以下步骤，如编译报错请重新确认上俩步（克隆主板、配置主板）是否配置正确

### 上传

克隆主板后会在以下目录生成临时文件，确认后本地提交并上传

- [kernel](http://10.20.40.17:8080/#/c/34183/)
- [vendor](http://10.20.40.17:8080/#/c/34184/)
- [device_droi](http://10.20.40.17:8080/#/c/34181/)

## 驱动版合并

### [配置common](http://10.20.40.17:8080/#/c/34180/)

可从其他项目的`driver`分支中将`devices/droi/common`目录拷贝过来，须检查以下几项并做修改

- device/droi/common/project.ini （修改ARCH、PLATFORM模块）
- device/droi/common/build/envsetup.sh （路径与平台关联，需重新配置）
- device/droi/common/fingerprint/ （driver版本直接删除）
- device/droi/common/ProjectConfig.mk （从mtk项目直接合入，并加入freeme driver 的基本配置）

> 注：`devices/droi/common `目录下配置为 `freeme` 自定义的配置，包含编译apk、属性开关等）

### 配置主板

克隆完成后，执行以下操作：

- 删除 `pu6a/ProjectConfig.mk` 所有配置，一般只保留以下4句（特殊情况请项目负责人确认），如下

        # The followings are generated while "clone project"
        # Do not modify
        FREEME_PRODUCT_BOARD = pu6a
        FREEME_PRODUCT_BASED = droi6739_36_n1

- 修改 `pu6a/full_pu6a.mk` 文件，新增如下语句：

        # @{ freeme.biantao, 20170120. FreemeBuild.
        PRODUCT_LOCALES := $(FREEME_PRODUCT_LOCALES_DEFAULT) $(filter-out $(FREEME_PRODUCT_LOCALES_DEFAULT) $(FREEME_PRODUCT_LOCALES_FILTER_OUT),$(PRODUCT_LOCALES) $(FREEME_PRODUCT_LOCALES))
        # @}

### [新建具体项目](http://10.20.40.17:8080/#/c/34286/)

在 `droi/pu6a` 目录下新建具体项目（与项目负责人沟通），并创建ProjectConfig.mk

### 合并驱动版提交

主要修改以下工程

- `package` （[暗码入口](http://10.20.40.17:8080/#/c/34261/)）
- `system` （[电池信息及init.freeme.rc等](http://10.20.40.17:8080/#/c/34263/)）
- `framework`（[电池信息及window flag等](http://10.20.40.17:8080/#/c/34260/)）
- `device_mediatek` （[引入common/device.mk等](http://10.20.40.17:8080/#/c/34259/) ）
- `build` （[Freeme宏配置](http://10.20.40.17:8080/#/c/34258/)）
- `vendor` （[Freeme NvRam，](http://10.20.40.17:8080/#/c/34265/) [忽略vendor/freeme目录，蓝牙地址自动生成](http://10.20.40.17:8080/#/c/34264/)）

### 编译

执行 `./mk -d {具体项目} new `，确保编译通过，如有问题进行相关修改

### 上传

确认没有遗漏后，通知 `reviewer` 合并代码

## 邮件通知

邮件告知驱动负责人、项目负责人、`freemedev`组，`driver`版本已准备就绪
