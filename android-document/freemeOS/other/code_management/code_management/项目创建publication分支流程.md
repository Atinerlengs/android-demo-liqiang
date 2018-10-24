# 项目创建publication分支流程

[TOC]

`publication`分支用于客户进行项目分发，要删除所有的freeme build工具，.git等

# mtk平台

### Step1 - 分支创建

继承`production`分支（操作见《项目创建production分支流程》）创建`publication` 分支，并创建`publication.xml`，提交后拉去验证

### Step2 - 移除freeme预置资源

删除freeme相关资源

- 删除common的铃声、字体（[示例](http://10.20.40.17:8080/#/c/37954/)）
- 删除freeme common项目的ProjectConfig （[示例](http://10.20.40.17:8080/#/c/37954/1/common/ProjectConfig.base.mk)）
- 移除common device.mk中部分依赖（[示例](http://10.20.40.17:8080/#/c/37954/1/common/device.mk)）

### Step3 - mtk项目导入freeme依赖

在mtk项目中导入freeme依赖

[示例](http://10.20.40.17:8080/#/c/37954/)

```
# droi6739_36_n1/ProjectConfig.mk （接入freeme ProjectConfig)
-include device/droi/common/ProjectConfig.mk

#droi6739_36_n1/full_droi6739_36_n1.mk （接入freeme PRODUCT_LOCALES)
# @{ freeme.biantao, 20170120. FreemeBuild.
PRODUCT_LOCALES := $(FREEME_PRODUCT_LOCALES_DEFAULT) $(filter-out $(FREEME_PRODUCT_LOCALES_DEFAULT) $(FREEME_PRODUCT_LOCALES_FILTER_OUT),$(PRODUCT_LOCALES) $(FREEME_PRODUCT_LOCALES))
# @}

#droi6739_36_n1/system.prop（移除默认的虚拟键以及density)
# FREEME qemu.hw.mainkeys=0
# FREEME ro.sf.lcd_density=320
```

### Step4 - 移除freeme的签名

[示例](http://10.20.40.17:8080/#/c/37955/1/common/device.mk)

```
# device_droi / common/device.mk
# Freeme default certificate key
PRODUCT_DEFAULT_DEV_CERTIFICATE := vendor/freeme/build/target/product/security/testkey
```

### Step5 - 编译

编译mtk项目，修复编译错误

### Step6 - 外发

项目外发前必须执行一下命令，对代码树进行清理：
```
. build/envsetup.sh
publication
```

# 展讯平台