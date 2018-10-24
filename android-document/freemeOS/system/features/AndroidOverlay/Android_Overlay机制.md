# Android Overlay机制及AAPT简介

[TOC]

### 简介

Android Overlay 即编译时动态替换资源，包括资源覆盖与新增

### 覆盖资源

- 平台资源Overlay

  定义以下属性于`device/droi/<platform>/<project>/device.mk`中

  - 平台Overlay初始化属性`DEVICE_PACKAGE_OVERLAYS := `
  - 项目Overlay初始化属性`PRODUCT_PACKAGE_OVERLAYS := `


- Apk Overlay

  初始化目录`LOCAL_RESOURCE_DIR :=`于`Android.mk`

以上三个标签依次覆盖规则如下：

```sh
# 从左到右进行覆盖
# PRODUCT_PACKAGE_OVERLAYS => DEVICE_PACKAGE_OVERLAYS => LOCAL_RESOURCE_DIR

# 实现代码 build/core/package_internal.mk (102)
package_resource_overlays := $(strip \
    $(wildcard $(foreach dir, $(PRODUCT_PACKAGE_OVERLAYS), \
      $(addprefix $(dir)/, $(LOCAL_RESOURCE_DIR)))) \
    $(wildcard $(foreach dir, $(DEVICE_PACKAGE_OVERLAYS), \
      $(addprefix $(dir)/, $(LOCAL_RESOURCE_DIR)))))

LOCAL_RESOURCE_DIR := $(package_resource_overlays) $(LOCAL_RESOURCE_DIR)
```

> 注：以上配置，在默认情况下，不能新增资源，只能覆盖原有的资源

### 新增资源

方法有如下俩种：

1. 给新增的资源添加`<add-resource>`字段，如：

   `<add-resource type="string" name="test_add_res">Test Add Resource</add-resource>`

2. 如不想给每个资源手动添加`add-resource`字段，可利用`aapt`的`--auto-add-overlay`参数 ，如：

   在项目的`Android.mk`添加如属性`LOCAL_AAPT_FLAGS += --auto-add-overlay`

### AAPT编译过程

**推荐查看 [浅谈Android的aapt资源编译过程 - jlins - 博客园](http://www.cnblogs.com/dyllove98/archive/2013/06/19/3144950.html) ，其中包含aapt如何编译打包APK**

AAPT接收参数后，启动编译（以`framework-res`为例）

```
out/host/linux-x86/bin/aapt package
	-u
	-x
	--private-symbols com.android.internal
	-z
	-M frameworks/base/core/res/AndroidManifest.xml
	-S device/sample/overlays/location/frameworks/base/core/res/res
	-S vendor/partner_gms/products/gms_overlay/frameworks/base/core/res/res
	-S device/droi/v9h62_gb/overlay/frameworks/base/core/res/res
	-S device/mediatek/common/overlay/navbar/frameworks/base/core/res/res
	-S frameworks/base/core/res/res
	-A frameworks/base/core/res/assets
	--min-sdk-version 24
	--target-sdk-version 24
	--product default
	--version-code 24
	--version-name 7.0
	--skip-symbols-without-default-localization
	-F out/target/common/obj/APPS/framework-res_intermediates/package-export.apk
```

而AAPT的主要参数解释如下（详细参数解释见[ Android自动打包工具aapt详解](http://blog.csdn.net/xiangzhihong8/article/details/53607539)）：

```ini
Usage:
 aapt l[ist] [-v] [-a] file.{zip,jar,apk}
   List contents of Zip-compatible archive.
 aapt d[ump] [--values] [--include-meta-data] WHAT file.{apk} [asset [asset ...]]
   ...
 aapt p[ackage] [-d][-f][-m][-u][-v][-x][-z][-M AndroidManifest.xml] \
        ...
        [--utf16] [--auto-add-overlay] \
        ...
        [-S resource-sources [-S resource-sources ...]] \
        [-F apk-file] [-J R-file-dir] \
        ...
   Package the android resources.  It will read assets and resources that are
   supplied with the -M -A -S or raw-files-dir arguments.  The -J -P -F and -R
   options control which files are output.
...
 Modifiers:
   ...
   # 特别说明下，这就是的include的base set，比如android.jar
   -I  add an existing package to base include set
   ...
   # overlay通过-S指定，可以指定多个目录，优先使用先匹配目录资源
   -S  directory in which to find resources.  Multiple directories will be scanned
       and the first match found (left to right) will take precedence.
   ...
   # 自动添加overlays包里的资源
   --auto-add-overlay
       Automatically add resources that are only in overlays.
   ...
```

### 参考

[浅谈Android的aapt资源编译过程 - jlins - 博客园](http://www.cnblogs.com/dyllove98/archive/2013/06/19/3144950.html)

[AndroidStudio Add App Resources](http://blog.zhaiyifan.cn/2016/02/18/android-resource-overlay/)

[编译时替换资源 - Android重叠包与资源合并一见](http://blog.zhaiyifan.cn/2016/02/18/android-resource-overlay/)

[Android Resources Overlay Mechanism](http://lib.csdn.net/article/android/4985)

[Android自动打包工具aapt详解](http://blog.csdn.net/xiangzhihong8/article/details/53607539)
