# 1.测试命令

###### 进入android-cts/tools, 执行./cts-tradefed
###### 单项测试需要的命令为：  run cts -c android.view.cts.WindowTest -m testDecorView
###### 整包测试： run cts -c android.view.cts.WindowTest

# 2.CTS资料

CTS认证详细资料请参照CTS文档 , 资料以及GMS认证工具ftp路径如下：
ftp://192.168.0.6/CTS资料/

# 3.CTS认证的手机环境配置：

##### 1)下载测试版本
##### 2)写IMEI号（须用SN工具写），确保在设置-->关于手机-->状态-->IMEI信息中显示正确（开机手写也可以）
##### 3)插入SIM卡（3G或4G卡）
##### 4)Setting-->Language and input -->Language -->English(US)
##### 5)关于手机-->版本号，打开开发者模式
##### 6)设置-->开发人员选项-->勾选：不锁定屏幕、USB调试、允许模拟位置
##### 7)安装CTS media 1.2媒体包
###### a.通过adb连接被测试设备（linux中，File System/home/share/CTS/android-cts-media-1.2）
###### b.  chmod 544 copy_media.sh
###### c.  ./copy_media.sh
##### 8)安装CtsDeviceAdmin.apk
###### （linux中，在CTS测试工具包中的如下路径：File System/home/share/CTS/android-cts-6.0_r8-linux_x86-arm/android-cts/repository/testcases/ ）adb install -r CtsDeviceAdmin.apk
##### 9)打开Wifi链接到有效的路由器（DNS：8.8.8.8或8.8.4.4）(WIFI名：CP4，密码：20160711)
##### 10)设置-->显示—>勾上自动旋转屏幕，确保屏幕超时时间为30分钟
##### 11)设置—>安全—>屏幕锁定方式-->无
##### 12)设置>安全>设备管理员>项目名为“android.deviceadmin.cts….”
######  共有3个选项，前面两项要手动勾选，最后一个不用选
##### 13)设置>安全>取消勾选“未知源”选项
##### 14)设置-- SIM管理--打开数据连接（第一次测试，不用打开数据链接）

# 4.当前Pcb最新代码上，整包测试通过

# 5.PS

##### 1.手机需要为USER版本
