[TOC]

# 逆向oppo rom

## 1. 获取rom下载

oppo官网提供了各个机型rom下载，见：http://www.coloros.com/rom/index.html

## 2. ozip转换为zip

OPPO的官网下载的卡刷包是ozip格式，需要解密处理才能转回zip格式，才能刷入第三方recovery

由于OPPO每个型号采用了不同的解密key，所以需要对机型做适配。

使用方法：linux 64位

./OppoDecrypt *.ozip -device_name

其中device_name如果是r9s则使用r9s,如下是对应的device_name:

- oppo r9s的device_name: r9s
- oppo a77的device_name: a77
- oppo r11/r11s的device_name: r11

例如

```
$ ./OppoDecrypt R11Plus_11_OTA_0270_all_UVSE4fXRYuxD_local.ozip -r11
```

## 3. 转换system.img

```
$ sdat2img.py system.transfer.list system.new.dat system.img.ext4
```

转换完成之后，即得到ext4格式的system.img文件。

## 4. 解压缩system.img

如果在公司，部署了亿赛通加密的ubuntu机器上，请将`system.img.ext4`拷贝到/tmp目录下执行。

```
$ sudo mount -o loop -t ext4 system.img.ext4 ~/sharedir/system
```

## 5. 反编译framework/app

使用fox反编译命令一键反编译。具体用法请参考`Android反编译.md`。

# 逆向vivo rom

vivo官网没有提供rom下载，逆向所需rom请从手机中pull，或在互联网上搜索第三方泄露的rom包。

## 1. 转换system.img

```
$ sdat2img.py system.transfer.list system.new.dat system.img.ext4 
```

转换完成之后，即得到ext4格式的system.img文件。

## 2. 一键反编译

使用fox反编译命令一键反编译。具体用法请参考`Android反编译.md`。

# 附录

## 相关工具

链接: https://pan.baidu.com/s/1gfmvTEb 密码: xcq6

## 参考

- https://blog.cofface.com/archives/2541.html
- https://github.com/xpirt/sdat2img
