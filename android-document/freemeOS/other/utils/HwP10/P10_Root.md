[TOC]

# Root华为P10

## 背景知识
1. 手机连接电源 电源键 + 音量下键 进入Fastboot模式
2. 手机连接电源 电源键 + 音量上键 进入eRecovery模式
3. 手机不连接电源 电源键 + 音量上键 进入Recovery模式

注： eRecovery模式 可以对手机进行完整的恢复  (比如刷了第三方的Recovery 或者 刷了boot.img)

## Step 1 : 手机解锁

### 注册华为账号
自行去官网注册登录

### 安装手机驱动
电脑上由手机驱动可以无视

驱动下载

地址：http://huawei-nova2-shuajibao.shuajizhijia.net/shuaji/17710.html

### 申请唯一解锁码

- **到华为解锁网站申请解锁码**

  地址：http://www.emui.com/plugin/hwdownload/download

- **获取解锁码**

  选择"EMUI5.0之前，EMUI5.1之后 "，然后按要求输入对应的 产品型号，序列号...点击提交,
获取到对应的解锁码
### 进行解锁
- **用fastboot方式解锁**

```
fastboot oem unlock 解锁码
```

- **查看是否解锁成功**

```
fastboot oem get-bootinfo
```

如果显示'''Bootloader Lock State: UNLOCKED'''，表示手机已经解锁，可以进行刷机操作

---

## Step 2 : 修改boot.img

### 解压boot.img

- **找到p10的boot.img**

    有道云droi/utils/HwP10目录下vtr-al00-b172-boot.img
    百度云：https://pan.baidu.com/s/1mhHdE5M

- **准备解压打包工具mkboot**

   https://github.com/xiaolu/mkbootimg_tools

- **解压**

```
./mkboot boot.img boot
```

### 修改debuggale值

- **/ramdisk/default.prop**

  修改ro.debuggable的值为1，表示能debug调试

```
diff --git a/ramdisk/default.prop b/ramdisk/default.prop
index 2c908e8..029fc77 100644
--- a/ramdisk/default.prop
+++ b/ramdisk/default.prop
@@ -6,7 +6,7 @@ ro.secure=1
 security.perf_harden=1
 ro.adb.secure=1
 ro.allow.mock.location=0
-ro.debuggable=0
+ro.debuggable=1
 ro.zygote=zygote64_32
 pm.dexopt.first-boot=interpret-only
 pm.dexopt.boot=verify-profile
diff --git a/ramdisk/init.rc b/ramdisk/init.rc
index 7ca66e7..728eb0b 100755
```

### 添加su权限

- **/ramdisk/init.rc**

  添加几个权限

```
diff --git a/ramdisk/init.rc b/ramdisk/init.rc
index 7ca66e7..728eb0b 100755
--- a/ramdisk/init.rc
+++ b/ramdisk/init.rc
@@ -40,7 +40,10 @@ on early-init

 on init
     sysclktz 0
-
+    chmod 0755 /su/bin/daemonsu
+    chmod 0755 /su/bin/sukernel
+    chmod 0755 /su/bin/su
+    chmod 0755 /su/bin/supolicy_wrapped
     copy /proc/cmdline /dev/urandom
     copy /default.prop /dev/urandom
```

### 打包boot.img

```
./mkboot boot boot.img
```

### 刷入手机

```
fastboot flash boot boot.img
fastboot reboot
```


### 解决内存不足的现象


关机状态，按（音量上键 + 电源键 ），手机开机一两秒，松开电源键按住音量上键，
进入华为的Recovery模式，恢复出厂设置，清除Cache，重启设备

## 遗留问题

1. 重新打包生成的image镜像/su/bin目录丢失x权限
2. 修改任意文件或者重新打包boog.img都会导致Settings部分模块异常(如：开发者模式 指纹 恢复出厂设置 等 崩溃)

## 参考资料
官方网站

1. https://emui.huawei.com/cn/plugin/unlock/step 华为解锁步骤

2. http://rom.7to.cn/romdetail/1024481 Rom市场
