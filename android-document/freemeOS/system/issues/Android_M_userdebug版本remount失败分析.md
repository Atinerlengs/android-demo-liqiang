# Android M userdebug版本remount失败分析

#### 现象

执行remount 命令后，system分区不能正常读写

```
adb root
adb remount
adb push framework-res.apk system/framework/
## push “成功”adb reboot
## reboot 后被push的文件还原
```

查看分区情况，发现system分区异常

```
$mount | grep system /dev/block/dm-0 /system ext4 ro,seclabel,relatime,discard,data=ordered 0 0
```

### 而正常可读写的system 分区挂载应该如下：

`/dev/block/platform/mtk-msdc.0/11230000.msdc0/by-name/system /system ext4 ro,seclabel,relatime,data=ordered 0 0`

### 问题分析

分析发现，执行`adb disable-verity`命令可解决该问题，执行命令后，分区挂载正常并可正常读写

`/dev/block/platform/mtk-msdc.0/11230000.msdc0/by-name/system /system ext4 ro,seclabel,relatime,data=ordered 0 0`

对于 `adb disable-verity` 命令，一般都是由 `adb remount` 失败提示执行，如下：

```
chenming@chenM:~$ adb remount
dm_verity is enabled on the system partition.
Use "adb disable-verity" to disable verity.
If you do not, remount may succeed, however, you will still not be able to write to these volumes.remount of system failed:
Read-only file systemremount failed
```

而该提示是只有在打开 `Android dm-verity` 功能才会出现，如集成需在init.rc中集成 **verity_update_state** 命令，如下：

```
on fs    ... ...    # Update dm-verity state and set partition.*.verified properties    verity_update_state
```

**但是在mtk项目工程中没有发现verity_update_state，因而partition.system.verified 属性没有被设置，最终没有错误提示**

MTK在fstab.in中默认打开system分区的verity功能（如下）

```
vendor/mediatek/proprietary/hardware/fstab/mt6735/fstab.in

20#ifdef __MTK_SEC_VERITY
21/dev/block/platform/mtk-msdc.0/11230000.msdc0/by-name/system/system__MTK_SYSIMG_FSTYPE ro	    wait,verify
22#else
23/dev/block/platform/mtk-msdc.0/11230000.msdc0/by-name/system/system__MTK_SYSIMG_FSTYPE   ro	    wait
```

宏在 __MTK_SEC_VERITY 定义在

```
vendor/mediatek/proprietary/hardware/fstab/mt6735/Android.mk
35ifeq (true,$(PRODUCTS.$(INTERNAL_PRODUCT).PRODUCT_SUPPORTS_VERITY))
36__CFLAGS += -D__MTK_SEC_VERITY
37endif
```

而 `PRODUCT_SUPPORTS_VERITY` 定义在 `build/target/product/verity.mk`


`PRODUCT_SUPPORTS_VERITY := true`

MTK 利用 `MTK_GMO_RAM_OPTIMIZE` 宏最终控制是否采用verity功能，如下v9c中配置

```
device/droi/v9c81q_ada_gmo/ProjectConfig.mk
MTK_GMO_RAM_OPTIMIZE = yes

device/droi/v9c/device.mk

304# setup dm-verity configs.
305PRODUCT_SYSTEM_VERITY_PARTITION := /dev/block/platform/mtk-msdc.0/11230000.msdc0/by-name/system
306ifneq (yes,$(strip $(MTK_GMO_RAM_OPTIMIZE)))
307$(call inherit-product, build/target/product/verity.mk)
308endif
```


### 结语

至此，本问题分析完毕，如以后再遇到此问题 ，请输入 `adb disable-verity` 命令即可
