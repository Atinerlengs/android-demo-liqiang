# OTA注意事项汇总
###修改记录
| 版 次  | 修改日期       | 作 者  | 修改内容 |
| :--- | ---------- | ---- | ---- |
| V1.0 | 2016.10.11 | 陈明   | 初版   |

[TOC]

### 支持平台
平台信息可通过属性 ```ro.hardware``` 查看

- **mt6752**
- **mt6735/37**
- **mt6755/50**

> 注：如果平台不支持，则需要 **适配** ，否则系统无法进行升级

### 配置文件
可对系统升级进行部分客制化，部分配置位于

```
vendor/droi/freeme/packages/apps/FreemeOTA/freeme_ota.properties
```

```properties
# External OTA Setting Properties

log.priority = 4

#config.work.oversea = false

#install.ignore.user_will = false

#download.ignore.network_type = false

#config.unread.for_new = true
#query.timing.interval = 3
#install.remind.force.interval= 10
```

全部配置位于（不建议修改该文件）
```
vendor/droi/freeme/packages/apps/FreemeOTA/assets/ota.properties
```


- 国内外服务器配置

  - 首先推荐：通过配置 ```build/tools/buildinfo``` 中 ```ro.build.ota.product``` 的第三个宏即 ```FREEME_OTA_LANGUAGE``` 来实现，如下：

  ```sh
  echo "ro.build.ota.product=${PRODUCTMANUFACTURER}_${FREEME_PRODUCT_DEVICE}_${FREEME_OTA_LANGUAGE}_${FREEME_OTA_FLASH}"

  FREEME_OTA_LANGUAGE=zh：###默认为国内
  FREEME_OTA_LANGUAGE=非zh：###默认为海外，譬如en、zh-tw等
  ```

  - 其次可通过以下配置

  ```properties
  # - false: China inland
  # - true : Oversea or HK or Taiwan or Macao
  config.work.oversea = false
  ```

- 强制升级

  ```properties
  # Force install ignore user will:
  # - true  : Ignore user will, then will install automatic
  # - false : Care about user will
  install.ignore.user_will = false
  ```

### 查询出错
匹配 **本地版本** 信息与 **服务器上传** 信息。

- 本地版本信息
  ![](res/06.png)

- 服务器版本信息
  ![](res/07.png)

###升级出错

#####如何查看recovery log
  进入recovery 模式后，界面停止在升级logo界面
  可通过按 *电源键 +音量上键* 进入recovery菜单模式，查看出错信息
  ![](res/01.jpg)![](res/02.jpg)

#####升级包损坏 或 升级包签名错误

  ![](res/03.jpg)

- Freeme定制key位置： ```vendor/droi/freeme/build/target/product/security/testkey```
- ~~~Android 默认： ```build/target/product/security/testkey```~~


#####存储位置问题

  ![](res/04.png)
  出现该问题，需要对该平台单独适配

#####版本错误

```
  This package is for "Q5" devices; this is a ***.
  或者
  Package expects build fingerprint of *** this device has ***
```

​    出现该问题，原因为俩个版本信息不一致，需要重新制作升级包

#####Sepolicy 问题

 通过查看last_kmsg可判断Sepolicy 错误类型

 针对OTA的se权限未移植完全，对比公版以下文件，查看是否有缺失

```shell
  device/mediatek/common/sepolicy/recovery.te:# freeme.biantao, 20160525. Ota.
  device/mediatek/common/sepolicy/uncrypt.te:# freeme.biantao, 20160525. Ota.
  external/sepolicy/domain.te # freeme.biantao, 20160525. Ota.
```

##### 升级包制作过程出错

```
system partition has unexpected non-zero contents after OTA update
system partition has unexpected contents after OTA update
```

  原因可能为：

- 使用的制作升级包命令不正确，请查看”OTA包制作SOP“
- 其他

##### cache空间大小不足

详情见”OTA包制作SOP“ 中FAQ：如何配置OTA 所需cache分区大小？

##### 制作含中文资源升级包失败

Ubuntu 12.04 的zipinfo 解析内容含有中文名称的zip包，可能出现乱码，请使用 zipinfo（ubuntu14.04）覆盖系统/usr/bin/zipinfo。

详情见
![](res/09.png)

##### system空间大小不足

 **现象** ：升级完成后，开机完成时， ```Luancher,``` , ```SystemUI``` 等应用出错。提示出错log 如下：

![](res/13.png)

执行 `adb shell` 查看systemui apk 发现如下情况(SystemUI.apk => SystemUi.apk.patch)：

![](res/11.png)

初步定位OTA 升级失败，查看last_log，有以下提示：

![](res/10.png)

提示空间不足，查看system分区：

![](res/12.png)

分区只剩16m

 **总结：** 预留空间太小，导致更新部分系统核心应用出错。

 **解决方案：** 预留足够大的system分区
