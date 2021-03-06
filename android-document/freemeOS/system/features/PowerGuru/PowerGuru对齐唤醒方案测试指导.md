PowerGuru对齐唤醒方案测试指导
======================

## 测试主题：
静止待机下验证对齐唤醒方案是否可行，和降功耗效果

## 背景说明：
该方案对用户安装的第三方应用进行对齐唤醒调整，该方案对功耗的优化效果会在安装多个第三方应用，且第三方唤醒越频繁功耗问题越明显。

> 该方案的最后结果通过如下两个方面的数据反映 :
>
>1. 同等时间测试之后的**平均**电流值
>2. 对比电流图，查看波峰的频率

## 操作步骤：
>

### 1. 对比测试,准备两台测试机，一台关闭对齐功能，一台开启对齐功能

> 开启/关闭方法如下

>####  1. 开启

>    * adb root
>	 * adb remount
>	 * adb shell
>    * setprop persist.freeme.powerguru.off 0
>	 * getprop | grep persist.freeme.powerguru.off //确认修改成功
>		[persist.freeme.powerguru.off]: [0]
>	 * reboot  //重启手机

>#### 2. 关闭
>    * adb root
>	 * adb remount
>	 * adb shell
>    * setprop persist.freeme.powerguru.off 1
>	 * getprop | grep persist.freeme.powerguru.off //确认修改成功
>		[persist.freeme.powerguru.off]: [1]
>	 * reboot  //重启手机
>

### 2. *GMS版本* 手机连接 **翻墙** WIFI，确保WIFI信号及网络OK，或者使用 **VPN** 连接； *非GMS版本* ，使用移动数据网络，确保手机信号在一个相对稳定的环境下

### 3. 安装 QQ / 微信 / 支付宝 / 腾讯应用宝 / 新浪微博 / 淘宝 等应用

> 上述应用必须安装，其他应用可选择安装，但是必须保持两个对比手机一样的测试环境

### 4. 登录 QQ ,微信,支付宝，腾讯应用宝，新浪微博，淘宝等第三方应用，保持所有的第三方应用是登录状态

### 5. 两台手机同时连接电流仪器。（至此要保持两个测试对比机器前提条件一致）

> 请保持两个小时以上的测试时间

### 6.power key 灭屏静止待机测试。待电流从亮屏稳定下来之后，开始记录电流

> 请保持输入的电压一致，和其他的前置条件一致


### 7.测试三次、测试时间 2小时(测试时间越长，电流数据显示的效果)

> 后面两次测试同上述步骤,保存数据，填写表格。

## 保存数据 ：
### 1.取每次的平均值 ，记录表格如下

|     次数      | 开启对齐      | 关闭对齐  | 测试时间 |
| ------------- | ------------- | -----     |   ---    |
| ------------- | ------------- | -----     |   ---    |
| ------------- | ------------- | -----     |   ---    |
| ------------- | ------------- | -----     |   ---    |
| 平均电流(mA)  | ------------- | -----     |   ---    |



### 2. 保存两个测试的 **平均** 电流图
