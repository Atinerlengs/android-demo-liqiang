# Freeme Recovery 调试要点

[TOC]

### 相关命令

OTA制作命令： `make otapackage`

命令所在文件 `build/core/Makefile` 相关代码如下：

```shell
#生成各*.img（system.img、boot.img等)命令

#压缩过程如下
... ...
1784     @# Zip everything up, preserving symlinks
1785     $(hide) (cd $(zip_root) && zip -qry ../$(notdir $@) .)
1786     @# Run fs_config on all the system, vendor, boot ramdisk,
1787     @# and recovery ramdisk files in the zip, and save the output
1788     $(hide) zipinfo -1 $@ | awk 'BEGIN { FS="SYSTEM/" } /^SYSTEM\// {print "system/" $$2}' | $(HOST_OUT_EXECUTABLES)/fs_config -C -D $(TARGET_OUT) -S $(SELINUX_FC) > $(zip_root)/META/filesystem_config.txt
1789     $(hide) zipinfo -1 $@ | awk 'BEGIN { FS="VENDOR/" } /^VENDOR\// {print "vendor/" $$2}' | $(HOST_OUT_EXECUTABLES)/fs_config -C -D $(TARGET_OUT) -S $(SELINUX_FC) > $(zip_root)/META/vendor_filesystem_config.txt
1790     $(hide) zipinfo -1 $@ | awk 'BEGIN { FS="BOOT/RAMDISK/" } /^BOOT\/RAMDISK\// {print $$2}' | $(HOST_OUT_EXECUTABLES)/fs_config -C -D $(TARGET_OUT) -S $(SELINUX_FC) > $(zip_root)/META/boot_filesystem_config.txt
1791     $(hide) zipinfo -1 $@ | awk 'BEGIN { FS="RECOVERY/RAMDISK/" } /^RECOVERY\/RAMDISK\// {print $$2}' | $(HOST_OUT_EXECUTABLES)/fs_config -C -D $(TARGET_OUT) -S $(SELINUX_FC) > $(zip_root)/META/recovery_filesystem_config.txt
... ...

1802 .PHONY: target-files-package
1803 target-files-package: $(BUILT_TARGET_FILES_PACKAGE)
```

```shell
# 打包各*.img并生成OTA.zip 命令
1814 # -----------------------------------------------------------------
1815 # OTA update package
1817 ... ...
1821 name := $(name)-ota-$(FILE_NAME_TAG)
1822
1823 INTERNAL_OTA_PACKAGE_TARGET := $(PRODUCT_OUT)/$(name).zip
1824
1825 $(INTERNAL_OTA_PACKAGE_TARGET): KEY_CERT_PAIR := $(DEFAULT_KEY_CERT_PAIR)
1826
1827 $(INTERNAL_OTA_PACKAGE_TARGET): $(BUILT_TARGET_FILES_PACKAGE) $(DISTTOOLS)
1828     @echo "Package OTA: $@"
1829     $(hide) PATH=$(foreach p,$(INTERNAL_USERIMAGES_BINARY_PATHS),$(p):)$$PATH MKBOOTIMG=$(MKBOOTIMG) \
1830        ./build/tools/releasetools/ota_from_target_files -v \
1831        $(if $(filter true,$(TARGET_USERIMAGES_USE_UBIFS)),-g,--block) \
1832        -p $(HOST_OUT) \
1833        -k $(KEY_CERT_PAIR) \
1834        -s ./device/mediatek/build/releasetools/mt_ota_from_target_files \
1835        $(if $(OEM_OTA_CONFIG), -o $(OEM_OTA_CONFIG)) \
1836        $(BUILT_TARGET_FILES_PACKAGE) $@

1838 .PHONY: otapackage
1839 otapackage: $(INTERNAL_OTA_PACKAGE_TARGET)
```


 `build/tools/releasetools/ota_from_target_files.py` （生成最终ota包脚本）


### 保留现场

在执行 `make otapackage` 后，如编译出错，tmp文件一般会被删除，要 **保留临时文件** 需进行以下操作：

```python
# build/tools/releasetools/ota_from_target_files.py

1876 if __name__ == '__main__':
1877   try:
1878     common.CloseInheritedPipes()
1879     main(sys.argv[1:])
1880   except common.ExternalError as e:
1881     print
1882     print "   ERROR: %s" % (e,)
1883     print
1884     sys.exit(1)
1885   finally:
1886     common.Cleanup()
```

将1886行的代码注释掉即可 => `#common.Cleanup()`，修改文件后，重复之前出错的步骤，即可获取错误现场

### 挂载镜像文件

挂载命令： `sudo mount -o loop -t ext4 system.img.ext4 /mnt`

由于电脑系统存在加密软件，进行此操作须先卸载加密软件，不然无法进行挂载（错误如下）

```shell
$ sudo mount -o loop -t ext4 system.img.ext4 /mnt
mount: wrong fs type, bad option, bad superblock on /dev/loop0,
       missing codepage or helper program, or other error
       In some cases useful info is found in syslog - try
       dmesg | tail  or so

FAIL: 32
```
