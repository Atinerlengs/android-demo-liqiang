# 项目创建production分支流程

[TOC]

`production`分支用于`FreemeIMP`进行项目分发

# mtk平台

### Step1 - 分支创建

子仓库创建分支`production`（以下仓库除外）

- `kernel及vendor` 使用mtk原始分支
- vendor/freeme 仓库确认要求后，建立production分支
- 源码内置系统应用须单独创建分支（应用systemui、settings、mms、Alarm，分支名（6739-freeme-7.1.1_prod））

### Step2 - repo分支创建

创建production.xml（移除客户项目，[示例](http://10.20.40.17:8080/#/c/37854/)）

提交后，并拉去代码验证

### Step3 - 封包

vendor/freeme/仓库下部分内容封包

- native misc  （[示例](http://10.20.40.17:8080/#/c/37870/)）
- powerguru（[示例](http://10.20.40.17:8080/#/c/37864/)）
- FreemeBadgeProvider （[示例](http://10.20.40.17:8080/#/c/37865/)）
- FreemeYellowPageLite & FreemeSalesCode ([示例](http://10.20.40.17:8080/#/c/37866/))

### Step4 - vendor合入freeme改动

vendor仓库合入freeme的改动（

- 基础设施（[示例](http://10.20.40.17:8080/#/c/37880/)）
- 修改部分mtk应用为freeme theme（[示例1](http://10.20.40.17:8080/#/c/37881/)）（[示例2](http://10.20.40.17:8080/#/c/37882/)）

### Step5 - 移除客户项目

移除客户项目的相关文件（device/droi目录下）

-  project & project.ini （[示例](http://10.20.40.17:8080/#/c/37908/)）
-  fingerprint （[示例](http://10.20.40.17:8080/#/c/37912/)）

### Step6 - 安全

通知安全进行相关修改

### Step7 - 编译

拉去代码，修复编译问题