# 添加新项目操作指南

[TOC]

## 概述

本文以案例方式演示如何在gerrit上创建project。

本文演示为AndroidN 6580项目海外版本添加`wf5`主板。

## 获取权限

本文操作需要操作人员具有相关权限，属于`gerrit`的代码管理组（`cm`组）。

如果你不具有`cm`组权限，请具有`cm`权限的人帮忙将你添加进入该组。添加方法是打开`gerrit`，在`People` 进入`List Groups`，点击`cm`，将自己的名字加入。

## 1. gerrit上创建仓库

登陆gerrit，打开project 页面，进入`Create New Project`，

- Project Name对应的编辑框中输入： `freemeos/mt6580/ALPS-MP-N0.MP2-V1_DROI6580_WE_N/pcb/droi/wf5`
- Rights Inherit From编辑框中输入：`privilege/cp`
- 勾选`Create initial empty commit`

## 2. 创建分支

通过`List`页面进入刚刚创建的`wf5`项目。由于该仓库刚刚创建完毕，仅有`master`分支。

如果此`wf5`是为用于海外版本出货，那么需要创建`pcb_oversea`分支，进入branches页面，操作如下

- Branch Name： 输入`pcb_oversea`
- Initial Revision: 输入`master`

点击`Create branch`按钮即创建了新分支。

## 3. 更新manifest

进入manifest仓库（每个拉取的repo项目的主目录下的.repo/manifest目录），添加

```
$ cd 80N
$ cd .repo/manifests
$ 编辑ALPS-MP-N0.MP2-V1_DROI6580_WE_N/pcb_oversea.xml，针对wf5添加一行。编辑完成后保存退出

$ git add ALPS-MP-N0.MP2-V1_DROI6580_WE_N/pcb_oversea.xml
$ git commit -m "[manifest] add wf5 for 80N"
$ git push origin HEAD:refs/for/master
```

打开gerrit，review代码，检查无误后，合并进入仓库。

## 4. 向wf5中提交代码

更新80项目repo仓库

```
$ cd 80N
$ repo sync
```

此时可以看到刚才创建的wf5项目空仓库已经被拉取下来了，向该git仓库中提交代码即可。