[TOC]

# 功耗源
> 手机系统的功耗主要来源如下两个方面（硬件、软件），这里主要介绍软件方面的功耗，并且主要从软件方面来做功耗优化，当然软件的优化也会掺杂对硬件设备使用的优化

## 硬件方面

* 射频
* wifi
* 蓝牙
* 屏幕
* 信号
* CPU动态调频


## 软件方面

* 定时Alarm wakeup类的唤醒源


* 系统/应用持有的wakelock唤醒锁


* 系统/应用造成的Exception


# 分析过程

一般我们会结合log（main.log system.log kernel.log） + 电流图 + bugreport.txt 分析找出功耗源，然后据此提出降功耗办法

## 采集哪些数据

### MTK log

> *#6803# 开启MTk log 中的 mobile log

### 电流图
> 使用Power minitor 工具抓取avg/max/min等所需要的电流图，使用的时候建议
选一个整点时间（如：12:00：00）去点击run ，开始采集数据。这个方便和mtk log的时间点对应起来。（在Power Tools的客户端的底部有数据的开始时间）

### bugreport.txt
Android为了方便开发人员分析整个系统平台和某个app在运行一段时间之内的所有信息，专门开发了bugreport工具。这个工具使用起来十分简单，只要在终端执行（linux或者win）：
即可生成bugreport文件。但是有一个问题是，这个生成的文件有的时候异常庞大，能够达到15M+,想一想对于一个txt文本格式的文件内容长度达到了15M+是一个什么概念，如果使用文本工具打开查看将是一个噩梦。因此google针对android 5.0（api 21）以上的系统开发了一个叫做battery historian的分析工具，这个工具就是用来解析这个txt文本文件，然后使用web图形的形式展现出来，这样出来的效果更加人性化，更加可读  
参考：https://www.2cto.com/kf/201607/528696.html

> 在采集bugreport.txt之前需要重置相关状态:
>
>* adb shell dumpsys batterystats --enable full-wake-history
>* adb shell dumpsys batterystats --reset
>* adb reboot，不然的话操作如下的命令抓取的数据是好几天的堆积在一起，不利于分析
>* adb bugreport > bugreport.txt
> 上传生成的bugreport信息压缩包至分析地址 https://bathist.ef.lc/

当然还有另一种方法：

>https://github.com/google/battery-historian
>通过github下载battery-historian，使用其中的脚本
>python historian.py -a bugreport.txt > battery.html
>上面的historian.py脚本是python写的，所以需要python环境，然后从github上下载这个脚本。上面两条命令执行成功后，会在目录下发现两个文件 
bugreport.txt和battery.html 使用Chrome 打开battery.html

采集到的数据如图：
https://github.com/google/battery-historian/blob/master/screenshots/timeline.png


## 如何找出功耗源

### wakeup类的alarm功耗源
这类的alarm会把手机从asleep的状态中唤醒（Doze模式除外，需要特殊Flag才能唤醒）

这个通过system.log中找关键log
搜索关键log    “==sending alarm==
举例如下：

```
Line 868: 12-24 21:10:00.054031   860  1250 V AlarmManager: sending alarm Alarm{adf3920 ==type 2== when 24372545 android} success

Line 879: 12-24 21:10:00.069781   860  1250 V AlarmManager: sending alarm Alarm{33342d9 type 2 when 24534545 ==android} success==

Line 879: 12-24 21:10:00.069781   860  1250 V AlarmManager: sending alarm Alarm{33342d9 type 0 when 24534545 ==com.android.phone} success==

Line 882: 12-24 21:10:00.085849   860  1250 V AlarmManager: sending alarm Alarm{bb2e6eb type 3 when 24352399 com.tencent.mm} success

Line 898: 12-24 21:10:00.105410   860  1250 V AlarmManager: sending alarm Alarm{807c49e type 0 when 1514120765455 com.ximalaya.ting.android} success

Line 907: 12-24 21:10:00.110535   860  1250 V AlarmManager: sending alarm Alarm{96b0b7f ==type 0== when 1514120760092 cmccwm.mobilemusic} success begin
```

可以找到某个时间点，==type = 0 、type = 2== 是wakeup类的唤醒alarm。这类alarm会把系统从深睡眠中唤醒，如果同时对应到电流图中，会在该时间点有个对应的波峰出现。


* 注意

>这里会发现来自于系统进程的alarm ==android== ， ==com.android.phone== 等
很好奇，这里我们没法确认这个alarm是来自于哪。
这类是系统的行为，可以通过在AlarmManangerServices.java中添加log来详细确认
>
>Slog.v(TAG,"sending alarm"+alarm.operation.getIntent().getAction() +"success"); 这里需要判断operation.getIntent() 是否为空，小心导致NullpointException

#### 建议解决方案

> * 使用“对齐唤醒”方案，减少等差时间段内的唤醒次数，统一到某一个时间窗口唤醒，这样也可能有一个潜在的副作用（在低端机器上，积累到一起触发的alarm可能会造成持续的波峰，这个需要针对对应项目数据验证）



### 系统/应用持有的wakelock唤醒锁长时间没有释放

在手机处于被wakeup的情况下，一些系统、应用操作需要持有一个wakelock唤醒锁。等执行操作结束之后，才释放锁。这也会观察到一段时间的波峰。

这个信息在bugreport中观测到的比较明显，Battery Historian的纵坐标的Userspace wakelock中看到某一个wakelock 的持有时间

然后结合log中的关键信息

```
12-24 21:10:00.051211   860   860 D PowerManagerService: acquireWakeLockInternal: lock=145490586, flags=0x1, ==tag="SyncManagerHandleSyncAlarm"==, ws=null, uid=1000, pid=860

12-24 21:10:00.051790   860   860 D PowerManagerNotifier: onWakeLockAcquired: flags=1, tag="SyncManagerHandleSyncAlarm", packageName=android, ownerUid=1000, ownerPid=860, workSource=null

12-24 21:10:00.175798   860   888 D PowerManagerService: releaseWakeLockInternal: lock=145490586 [SyncManagerHandleSyncAlarm], flags=0x0, total_time=124ms

12-24 21:10:00.175922   860   888 D PowerManagerNotifier: onWakeLockReleased: flags=1, tag="SyncManagerHandleSyncAlarm", packageName=android, ownerUid=1000, ownerPid=860, workSource=null
```

代码中搜索“SyncManagerHandleSyncAlarm” 就能找到这个唤醒锁的使用者，如何使用的。

#### 建议解决方案
> * Freeme7.0上的项目基本都带有“对齐唤醒”功能，但是因为这类的WakeLock,有些都是系统进程持有的，所以要结合实际功能评估持有锁之后是否有耗时操作
> * “浙江移动后台一直持有wakelock的方案”  
360提供一个patch，持有锁一定时间就释放，patch 如下：

```
diff --git a/base/core/java/android/os/PowerManager.java b/base/core/java/android/os/PowerManager.java
index c61b2fc..71a721c 100644
--- a/base/core/java/android/os/PowerManager.java
+++ b/base/core/java/android/os/PowerManager.java
@@ -1108,9 +1108,32 @@ public final class PowerManager {
         public void acquire() {
             synchronized (mToken) {
                 acquireLocked();
+                /* 360OS add, begin */
+                /* A temporary solution for special package, it's bad somehow */
+                if(mPackageName != null && mPackageName.equals("com.example.businesshall")){
+                    mHandler.postDelayed(mDefReleaser, 2 * 60 * 1000);
+                }
+                /* 360OS add, end */
             }
         }

+        /* 360OS add, begin */
+        private final Runnable mDefReleaser = new Runnable() {
+            public void run() {
+                synchronized (mToken) {
+                    if (mHeld) {
+                        Trace.asyncTraceEnd(Trace.TRACE_TAG_POWER, mTraceName, 0);
+                        try {
+                            Log.v(TAG, "force release wakelock for GMS!");
+                            mService.releaseWakeLock(mToken, 0);
+                        } catch (RemoteException e) {
+                        }
+                        mHeld = false;
+                    }
+                }
+            }
+        };
+        /* 360OS add, end */
         /**
          * Acquires the wake lock with a timeout.
          * <p>
@@ -1124,6 +1147,14 @@ public final class PowerManager {
         public void acquire(long timeout) {
             synchronized (mToken) {
                 acquireLocked();
+                /* 360OS add, begin */
+                /* A temporary solution for special package, it's bad somehow */
+                if(mPackageName != null && mPackageName.equals("com.example.businesshall")){
+                    if(timeout >= 5 * 60 * 1000){
+                        timeout = 5 * 60 * 1000;
+                    }
+                }
+                /* 360OS add, end */
                 mHandler.postDelayed(mReleaser, timeout);
             }
         }
--
1.9.1

```


### 系统/应用造成的Exception
这个在log中都可以找到，想办法解决此类Exception导致的高功耗

#### 建议解决方案

> * 正面解决此类的Exception
> * 如果是合入“对齐唤醒”的版本，可侧面考虑是不是对齐唤醒间接到值的socket 通信超时的异常。

### apk的通信唤醒
main log中找如下关键log

```
06-23 20:33:20.014895 1528 1762 D Posix : [==Posix_connect== Debug]Process com.zhuoyi.market :7892
```

#### 建议解决方案

> 这类的涉及到某个应用的通信的需求，可交由每个应用去解决。


### JobScheduler耗电

Android 5.0 使用了JobScheduler来计划任务执行，设计的目的也是为了降功耗。
但是不排除某些流氓应用使用JobInfo.setPeriodic（long mins）去设置重复Job任务，这样也会造成高功耗。我们可以很容易的在Batery-Historian中找JobScheduler的坐标，结合log中的关键信息

#### 建议解决方案

> JobScheduler的设计初衷就是为了降功耗，但是不排除应用恶性使用api，这里我们的优化方向，针对具体的JobInfo的要求设计，同时也把重复的Jobinf对齐到“对齐方案”的整点时间。（PowerGuru2.0的优化方向）


# 补充
MTK FAQ 也提供一些特殊功耗点的查找和分析方法

[FAQ07421] 待机时，怎么看AP每次wake up起来的时长

https://onlinesso.mediatek.com/FAQ#/SW/FAQ07421

