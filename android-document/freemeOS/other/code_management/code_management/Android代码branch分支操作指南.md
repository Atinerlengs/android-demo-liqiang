# Android代码branch分支操作指南

[TOC]

## 概述

本文讲述如果开发branch分支。

本文以70M项目为例讲解，如何从`driver`分支branch出`pcb_oversea`分支的具体方法。

## 获取权限

本文操作需要操作人员具有相关权限，属于`gerrit`的代码管理组（`cm`组）。

如果你不具有`cm`组权限，请具有`cm`权限的人帮忙将你添加进入该组。添加方法是打开`gerrit`，在`People` 进入`List Groups`，点击`cm`，将自己的名字加入。

## 拉取代码

首先拉取该项目`driver`分支代码，注意修改`zhuzhongkai`为你的名字。

```
$ mkdir 6570m
$ repo init --no-repo-verify -u ssh://zhuzhongkai@10.20.40.19:29418/freemeos/manifest -m ALPS-MP-M0.MP23-V1.32.3_DROI6570_CTLC_M/driver.xml
$ repo sync
```

## 创建分支

创建本地`pcb_oversea`分支

```
$ repo start --all pcb_oversea
```

将本地`pcb_oversea`分支推送到gerrit服务器上

```
$ repo forall -pc git push origin pcb_oversea
```

执行完毕后前往gerit网页上，进入相关仓库，查看branches页面，是否有`pcb_oversea`分支。

## 提交manifest

```
$ cd .repo
$ cd ALPS-MP-N0.MP1-V1.0.2_DROI6737M_65_N
$ cp driver.xml pcb_oversea.xml
```

编辑该文件，将其中的`revision`字段的`driver`修改为`pcb_oversea`

```
@@ -4,7 +4,7 @@
   <remote  name="origin"
            fetch=".."
            review="http://10.20.40.19:8080/" />
-  <default revision="driver"
+  <default revision="pcb_oversea"
            remote="origin"
            sync-j="4" />
```

请检查其他项目外项目的`revision`字段可能也需要相应修改。对于特殊仓库，如`freemeos/common/freeme`仓库，配置使用自己的`m-driver`分支。

```
  <project path="vendor/freeme" name="freemeos/common/freeme" revision="m-driver" >
    <linkfile src="build/tools/build" dest="mk" />
    <linkfile src="build/tools/build" dest="publish" />
    <linkfile src="build/tools/build" dest="stepup" />
    <linkfile src="build/tools/build" dest="otadiff" />
  </project>
```

该仓库属于属于公共仓库，如果不确定该项目的修改，请联系该项目owner（卞涛）了解该仓库如何配置`revision`。

接下来提交代码

```
$ git add pcb_oversea.xml
$ git commit -m "[mainfest] branch pcb_oversea for ALPS-MP-M0.MP23-V1.32.3_DROI6570_CTLC_M"
$ git push origin HEAD:refs/for/master
```

然后请Leader将review该提交。

## 邮件通知

提交被review通过后，就可以通知相关team执行后续开发了。一般来说，代码管理人员需要撰写邮件通知相关人等。