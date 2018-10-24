[TOC]

## 问题描述

使用80N wf8公版执行cts启动时间测试时，测试失败，cts控制台的日志如下：

```
$ ./cts-tradefed
Android Compatibility Test Suite 7.0_r9 (3943095)
05-09 21:33:14 I/DeviceManager: Detected new device TSS8TW8LAEI7E699
cts-tf >
cts-tf >
cts-tf > run cts -m CtsAppSecurityHostTestCases  -t android.appsecurity.cts.DirectBootHostTest#testDirectBootNone

05-09 21:33:16 I/TestInvocation: Starting invocation for 'cts' on build '3943095' cts -m CtsAppSecurityHostTestCases -t android.appsecurity.cts.DirectBootHostTest#testDirectBootNone Compatibility Test Suite 3943095 CTS 1494336796137 /home/prife/sharedir/projects/cts/android-cts/tools/./../.. 7.0_r9 cts https://androidpartner.googleapis.com/v1/dynamicconfig/suites/CTS/modules/{module}/version/{version}?key=AIzaSyAbwX5JRlmsLeygY2WWihpIJPXFLueOQ3U 2017.05.09_21.33.16 on device TSS8TW8LAEI7E699
05-09 21:33:16 I/ResultReporter: Initializing result directory
05-09 21:33:16 I/ResultReporter: Results Directory: /home/prife/sharedir/projects/cts/android-cts/tools/./../../android-cts/results/2017.05.09_21.33.16
05-09 21:33:16 I/ResultReporter: Created log dir /home/prife/sharedir/projects/cts/android-cts/tools/./../../android-cts/logs/2017.05.09_21.33.16
05-09 21:33:36 I/SettingsPreparer: Setting verifier_verify_adb_installs to value 0
05-09 21:33:38 I/ApkPreconditionCheck: Instrumenting package com.android.preconditions.cts:
05-09 21:33:53 I/ApkPreconditionCheck: Target preparation successful
05-09 21:33:58 E/WifiCheck: Device has no network connection, no ssid provided, some modules of CTS require an active network connection
05-09 21:33:59 I/DeviceInfoCollector: Instrumenting package com.android.compatibility.common.deviceinfo:
05-09 21:34:32 I/DeviceInfoCollector: Target preparation successful
05-09 21:34:32 W/PropertyCheck: Expected "user" but found "userdebug" for property: ro.build.type
05-09 21:34:33 I/ModuleRepo: TSS8TW8LAEI7E699 running 1 modules, expected to complete in 20m 0s
05-09 21:34:33 I/CompatibilityTest: Starting 1 module on TSS8TW8LAEI7E699
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.AppSecurityTests
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.ScopedDirectoryAccessTest
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.UsesLibraryHostTest
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.ExternalStorageHostTest
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.PkgInstallSignatureVerificationTest
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.AdoptableHostTest
05-09 21:34:33 W/DeviceTestCase: No tests found in android.appsecurity.cts.KeySetHostTest
05-09 21:34:33 I/ConsoleReporter: [TSS8TW8LAEI7E699] Starting armeabi-v7a CtsAppSecurityHostTestCases with 1 test
05-09 21:38:25 W/AndroidNativeDevice: AdbCommandRejectedException (device 'TSS8TW8LAEI7E699' not found) when attempting shell getprop sys.boot_completed on device TSS8TW8LAEI7E699
05-09 21:39:34 I/ConsoleReporter: [1/1 armeabi-v7a CtsAppSecurityHostTestCases TSS8TW8LAEI7E699] android.appsecurity.cts.DirectBootHostTest#testDirectBootNone fail: java.lang.AssertionError: on-device tests failed:
com.android.cts.encryptionapp.EncryptionAppTest#testVerifyUnlockedAndDismiss:
java.lang.AssertionError: Failed to find /data/user_de/0/com.android.cts.splitapp/files/8.android.intent.action.BOOT_COMPLETED
at com.android.cts.encryptionapp.EncryptionAppTest.awaitBroadcast(EncryptionAppTest.java:409)
at com.android.cts.encryptionapp.EncryptionAppTest.assertUnlocked(EncryptionAppTest.java:294)
at com.android.cts.encryptionapp.EncryptionAppTest.testVerifyUnlockedAndDismiss(EncryptionAppTest.java:210)
at java.lang.reflect.Method.invoke(Native Method)
at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:220)
at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:205)
at junit.framework.TestCase.runBare(TestCase.java:134)
at junit.framework.TestResult$1.protect(TestResult.java:115)
at android.support.test.internal.runner.junit3.AndroidTestResult.runProtected(AndroidTestResult.java:77)
at junit.framework.TestResult.run(TestResult.java:118)
at android.support.test.internal.runner.junit3.AndroidTestResult.run(AndroidTestResult.java:55)
at junit.framework.TestCase.run(TestCase.java:124)
at android.support.test.internal.runner.junit3.NonLeakyTestSuite$NonLeakyTest.run(NonLeakyTestSuite.java:63)
at junit.framework.TestSuite.runTest(TestSuite.java:243)
at junit.framework.TestSuite.run(TestSuite.java:238)
at android.support.test.internal.runner.junit3.DelegatingTestSuite.run(DelegatingTestSuite.java:103)
at android.support.test.internal.runner.junit3.AndroidTestSuite.run(AndroidTestSuite.java:68)
at android.support.test.internal.runner.junit3.JUnit38ClassRunner.run(JUnit38ClassRunner.java:103)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
at android.support.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:272)
at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1932)

    at android.appsecurity.cts.Utils.runDeviceTests(Utils.java:102)
    at android.appsecurity.cts.DirectBootHostTest.runDeviceTests(DirectBootHostTest.java:206)
    at android.appsecurity.cts.DirectBootHostTest.doDirectBootTest(DirectBootHostTest.java:166)
    at android.appsecurity.cts.DirectBootHostTest.testDirectBootNone(DirectBootHostTest.java:127)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke(Method.java:497)
    at junit.framework.TestCase.runTest(TestCase.java:168)
    at junit.framework.TestCase.runBare(TestCase.java:134)
    at com.android.tradefed.testtype.DeviceTestResult$1.protect(DeviceTestResult.java:81)
    at com.android.tradefed.testtype.DeviceTestResult.runProtected(DeviceTestResult.java:56)
    at com.android.tradefed.testtype.DeviceTestResult.run(DeviceTestResult.java:85)
    at junit.framework.TestCase.run(TestCase.java:124)
    at com.android.tradefed.testtype.DeviceTestCase.run(DeviceTestCase.java:180)
    at com.android.tradefed.testtype.JUnitRunUtil.runTest(JUnitRunUtil.java:55)
    at com.android.tradefed.testtype.JUnitRunUtil.runTest(JUnitRunUtil.java:38)
    at com.android.tradefed.testtype.DeviceTestCase.run(DeviceTestCase.java:144)
    at com.android.tradefed.testtype.HostTest.run(HostTest.java:253)
    at com.android.compatibility.common.tradefed.testtype.ModuleDef.run(ModuleDef.java:247)
    at com.android.compatibility.common.tradefed.testtype.CompatibilityTest.run(CompatibilityTest.java:428)
    at com.android.tradefed.invoker.TestInvocation.runTests(TestInvocation.java:716)
    at com.android.tradefed.invoker.TestInvocation.prepareAndRun(TestInvocation.java:491)
    at com.android.tradefed.invoker.TestInvocation.performInvocation(TestInvocation.java:386)
    at com.android.tradefed.invoker.TestInvocation.invoke(TestInvocation.java:166)
    at com.android.tradefed.command.CommandScheduler$InvocationThread.run(CommandScheduler.java:471)

05-09 21:39:34 I/ConsoleReporter: [TSS8TW8LAEI7E699] armeabi-v7a CtsAppSecurityHostTestCases completed in 5m 0s. 0 passed, 1 failed, 0 not executed
05-09 21:39:34 W/DeviceTestCase: No tests found in android.appsecurity.cts.PrivilegedUpdateTests
05-09 21:39:34 W/DeviceTestCase: No tests found in android.appsecurity.cts.PermissionsHostTest
05-09 21:39:34 W/DeviceTestCase: No tests found in android.appsecurity.cts.DocumentsTest
05-09 21:39:34 W/DeviceTestCase: No tests found in android.appsecurity.cts.SplitTests
05-09 21:39:34 W/CompatibilityTest: Inaccurate runtime hint for armeabi-v7a CtsAppSecurityHostTestCases, expected 20m 0s was 5m 0s
05-09 21:39:36 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:39:43 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:39:50 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:39:58 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:40:05 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:40:12 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:40:19 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:40:26 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:40:33 I/MonitoringUtils: Connectivity check failed, retrying in 5000ms
05-09 21:40:38 W/CompatibilityTest: System status checker [com.android.compatibility.common.tradefed.targetprep.NetworkConnectivityChecker] failed with message: failed network connectivity check
05-09 21:40:38 W/CompatibilityTest: There are failed system status checkers: [com.android.compatibility.common.tradefed.targetprep.NetworkConnectivityChecker] capturing a bugreport
05-09 21:41:33 I/ResultReporter: Saved logs for bugreport-checker-post-module-CtsAppSecurityHostTestCases in /home/prife/sharedir/projects/cts/android-cts/tools/./../../android-cts/logs/2017.05.09_21.33.16/bugreport-checker-post-module-CtsAppSecurityHostTestCases_2686884682507685938.zip
05-09 21:41:33 I/ResultReporter: Saved logs for device_logcat in /home/prife/sharedir/projects/cts/android-cts/tools/./../../android-cts/logs/2017.05.09_21.33.16/device_logcat_3929520033359652433.zip
05-09 21:41:33 I/ResultReporter: Saved logs for host_log in /home/prife/sharedir/projects/cts/android-cts/tools/./../../android-cts/logs/2017.05.09_21.33.16/host_log_5411306482923611799.zip
05-09 21:41:33 I/ResultReporter: Invocation finished in 8m 17s. PASSED: 0, FAILED: 1, MODULES: 1 of 1
05-09 21:41:34 I/ResultReporter: Test Result: /home/prife/sharedir/projects/cts/android-cts/results/2017.05.09_21.33.16/test_result_failures.html
05-09 21:41:34 I/ResultReporter: Full Result: /home/prife/sharedir/projects/cts/android-cts/results/2017.05.09_21.33.16.zip
```

- 已知37平台上此项测试可以通过。

## cts环境搭建

1.下载安装包

```
ftp://192.168.0.6/CTS_资料/r9/android-cts-7.0_r9-linux_x86-arm.zip
或者
\\192.168.3.127\sharedir\projects\cts\android-cts-7.0_r9-linux_x86-arm.zip
```

2.安装

解压上面的包。又cts环境依赖aapt命令，因此请将aapt添加到系统PATH变量中去

```
ln -s /home/prife/Android/Sdk/build-tools/24.0.2/aapt ~/bin/aapt
```

## 运行

```
$ cd android-cts/tools
$ ./cts-tradefed
Android Compatibility Test Suite 7.0_r9 (3943095)
05-09 21:33:14 I/DeviceManager: Detected new device TSS8TW8LAEI7E699
cts-tf >
```

此时连接手机，并关闭锁屏。然后执行

```
cts-tf > run cts -m CtsAppSecurityHostTestCases  -t android.appsecurity.cts.DirectBootHostTest#testDirectBootNone
```

等待cts自动执行，此时手机会自动设置pin码锁屏，并自动重启，然后再去掉锁屏，再重启，整个测试结束，整个过程大约5分钟。

# 分析进展

- 金川说80N 4月17号80N测试cts通过，但是这边测试，最新版本、4月18日、4月1日，等多个版本都不通过。
- cts测试的日志会保存在android-cts/logs目录下，每执行一次测试命令，就会生成一个目录，含有测试抓取的日志。

另外，上面的cts测试的原理是安装cts测试应用，这些应用的代码也可以看到，源码应与使用的cts工具的版本一致，本例中使用的是`android-cts-7.0_r9`，那么在aosp中取出该分支的方法如下：

```
cd aosp/cts
git pull
git checkout android-cts-7.0_r9 -b refs/tags/android-cts-7.0_r9
```

然后上面的关键日志如下

```
05-09 21:39:34 I/ConsoleReporter: [1/1 armeabi-v7a CtsAppSecurityHostTestCases TSS8TW8LAEI7E699] android.appsecurity.cts.DirectBootHostTest#testDirectBootNone fail: java.lang.AssertionError: on-device tests failed:
com.android.cts.encryptionapp.EncryptionAppTest#testVerifyUnlockedAndDismiss:
java.lang.AssertionError: Failed to find /data/user_de/0/com.android.cts.splitapp/files/8.android.intent.action.BOOT_COMPLETED
at com.android.cts.encryptionapp.EncryptionAppTest.awaitBroadcast(EncryptionAppTest.java:409)
at com.android.cts.encryptionapp.EncryptionAppTest.assertUnlocked(EncryptionAppTest.java:294)
at com.android.cts.encryptionapp.EncryptionAppTest.testVerifyUnlockedAndDismiss(EncryptionAppTest.java:210)
```

根据打印的log查看代码如下：

```
cts/hostsidetests/appsecurity/test-apps/EncryptionApp/src/com/android/cts/encryptionapp/EncryptionAppTest.java
```

```
public void assertUnlocked() throws Exception {
    awaitBroadcast(Intent.ACTION_LOCKED_BOOT_COMPLETED);
    awaitBroadcast(Intent.ACTION_BOOT_COMPLETED);
}

private void awaitBroadcast(String action) throws Exception {
    final Context otherContext = mDe.createPackageContext(OTHER_PKG, 0)
            .createDeviceProtectedStorageContext();
    final File probe = new File(otherContext.getFilesDir(),
            getBootCount() + "." + action);
    for (int i = 0; i < 60; i++) {
        Log.d(TAG, "Waiting for " + probe + "...");
        if (probe.exists()) {
            return;
        }
        SystemClock.sleep(1000);
    }
    throw new AssertionError("Failed to find " + probe);
}
```

测试失败的原因是由于ACTION_BOOT_COMPLETED广播对应的标记文件未生成，而此文件是由`splitapp` 生成

```
cts/hostsidetests/appsecurity/test-apps/SplitApp/src/com/android/cts/splitapp/BaseBootReceiver.java
```

```
    public void onReceive(Context context, Intent intent) {
        try {
            context = context.createDeviceProtectedStorageContext();
            final File probe = new File(context.getFilesDir(),
                    getBootCount(context) + "." + intent.getAction());
            Log.d(TAG, "Touching probe " + probe);
            probe.createNewFile();
            exposeFile(probe);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
```

查看开机过程的log中关于开机广播信息

```
05-10 23:17:24.270   990  1029 V ActivityManager: broadcast BOOT_COMPLETED intent
05-10 23:17:24.278   990  1029 V ActivityManager: Broadcast: Intent { act=android.intent.action.LOCKED_BOOT_COMPLETED flg=0x9000010 (has extras) } ordered=true userid=0 callerApp=null
05-10 23:17:24.281   990  1029 V ActivityManager: Enqueing broadcast: android.intent.action.LOCKED_BOOT_COMPLETED replacePending=false
05-10 23:17:24.281   990  1029 I ActivityManager: Broadcast intent Intent { act=android.intent.action.LOCKED_BOOT_COMPLETED flg=0x9000010 (has extras) } on background queue
05-10 23:17:24.281   990  1029 V ActivityManager: Enqueueing ordered broadcast BroadcastRecord{3bb7e4e u0 android.intent.action.LOCKED_BOOT_COMPLETED}: prev had 0
05-10 23:17:24.282   990  1029 I ActivityManager: Enqueueing broadcast android.intent.action.LOCKED_BOOT_COMPLETED
05-10 23:17:25.215   990  1248 D ActivityManager: BroadcastRecord{3bb7e4e u0 android.intent.action.LOCKED_BOOT_COMPLETED}, spend: 332, android.os.BinderProxy@767503, ActivityInfo{355315a com.android.talkback.BootReceiver}
05-10 23:17:25.989   990  1263 D ActivityManager: BroadcastRecord{3bb7e4e u0 android.intent.action.LOCKED_BOOT_COMPLETED}, spend: 748, android.os.BinderProxy@dc49d4f, ActivityInfo{de2ba57 com.android.deskclock.AlarmInitReceiver}
05-10 23:17:26.363   990  1000 D ActivityManager: BroadcastRecord{3bb7e4e u0 android.intent.action.LOCKED_BOOT_COMPLETED}, spend: 328, android.os.BinderProxy@c8640e7, ActivityInfo{15a9f59 com.mediatek.gallery3d.util.BootCompletedReceiver}
05-10 23:17:27.186   990  1021 V ActivityManager: Broadcast: Intent { act=android.intent.action.BOOT_COMPLETED flg=0x9000010 (has extras) } ordered=true userid=0 callerApp=null
05-10 23:17:27.206   990  1021 V ActivityManager: Enqueing broadcast: android.intent.action.BOOT_COMPLETED replacePending=false
05-10 23:17:27.206   990  1021 I ActivityManager: Broadcast intent Intent { act=android.intent.action.BOOT_COMPLETED flg=0x9000010 (has extras) } on background queue
05-10 23:17:27.206   990  1021 V ActivityManager: Enqueueing ordered broadcast BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}: prev had 16
05-10 23:17:27.206   990  1021 I ActivityManager: Enqueueing broadcast android.intent.action.BOOT_COMPLETED
05-10 23:17:42.020   990  1000 D ActivityManager: BroadcastRecord{3bb7e4e u0 android.intent.action.LOCKED_BOOT_COMPLETED}, spend: 208, android.os.BinderProxy@8e7423c, ActivityInfo{ad7b5d3 com.android.cts.splitapp.LockedBootReceiver}
05-10 23:18:00.843   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 244, android.os.BinderProxy@8d36567, ActivityInfo{2489d5a com.freeme.sc.network.monitor.commuincate.NWM_DownLoadProfileSetting}
05-10 23:18:00.864   990   990 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 20, android.app.LoadedApk$ReceiverDispatcher$InnerReceiver@6178110, BroadcastFilter{343a60e u0 ReceiverList{3b50409 990 system/1000/u0
05-10 23:18:00.988   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 119, android.os.BinderProxy@82d1af5, BroadcastFilter{99d86fb u0 ReceiverList{7e9508a 1107 com.android.systemui/10026/u0 remote:82d1af5}}
05-10 23:18:00.991   990  1689 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 2, android.os.BinderProxy@5965260, BroadcastFilter{80737d5 u0 ReceiverList{45a8319 1107 com.android.systemui/10026/u0 remote:5965260}}
05-10 23:18:00.995   990   990 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 4, android.app.ActivityThread$ApplicationThread@c33110a, ActivityInfo{97b8d75 com.android.server.BootReceiver}
05-10 23:18:01.010   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 14, android.os.BinderProxy@d7933d6, ActivityInfo{9621798 com.android.systemui.BootReceiver}
05-10 23:18:02.641   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 1630, android.os.BinderProxy@b27c829, ActivityInfo{7ea7657 com.stools.util.cp.WorkReceiver}
05-10 23:18:17.136   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 153, android.os.BinderProxy@2d42b70, ActivityInfo{bd314db com.freeme.ota.app.UpdateReceiver}
05-10 23:18:17.158   990  1562 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 20, android.os.BinderProxy@3e6cc42, ActivityInfo{bb8d28d com.smjkb.tlik.lvk.SmjKbReceiver}
05-10 23:18:17.596   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 436, android.os.BinderProxy@4c3d66, ActivityInfo{a299190 com.android.dialer.calllog.CallLogReceiver}
05-10 23:18:18.249   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 16, android.os.BinderProxy@b74cb7e, ActivityInfo{5c354f8 com.android.phone.OtaStartupReceiver}
05-10 23:18:18.252   990   990 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 2, android.app.LoadedApk$ReceiverDispatcher$InnerReceiver@5ddd222, BroadcastFilter{9c3ba70 u0 ReceiverList{dc504b3 990 system/1000/u0 
05-10 23:18:18.260   990   990 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 7, android.app.LoadedApk$ReceiverDispatcher$InnerReceiver@f8e9bac, BroadcastFilter{9d7680a u0 ReceiverList{62ee075 990 system/1000/u0 local:f8e9bac}}05-10 23:18:18.288   990   990 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 27, android.app.LoadedApk$ReceiverDispatcher$InnerReceiver@e4981a6, BroadcastFilter{e7c1232 u-1 ReceiverList{730f7e7 990 
05-10 23:18:18.294   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 5, android.os.BinderProxy@245f7c8, BroadcastFilter{62d0786 u0 ReceiverList{5f9a061 1235 com.android.phone/1001/u0 remote:245f7c8}}
05-10 23:18:18.623   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 302, android.os.BinderProxy@b25ed41, ActivityInfo{355315a com.android.talkback.BootReceiver}
05-10 23:18:18.872   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 243, android.os.BinderProxy@d70ec46, ActivityInfo{a4a2f8c com.mediatek.mtklogger.framework.LogReceiver}
05-10 23:18:19.494   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 618, android.os.BinderProxy@49fe3c9, ActivityInfo{72f2f6 com.uc.base.push.core.PushProxyReceiver}
05-10 23:18:34.525   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 49, android.os.BinderProxy@49fe3c9, ActivityInfo{ccb9865 com.taobao.accs.EventReceiver05-10 23:18:49.719   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 207, android.os.BinderProxy@7f5958e, ActivityInfo{8464742 com.android.calendar.alerts.AlertReceiver}
05-10 23:19:05.038   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 285, android.os.BinderProxy@d46238f, ActivityInfo{de2ba57 com.android.deskclock.AlarmInitReceiver}

05-10 23:19:05.176   990  1248 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 136, android.os.BinderProxy@d7d6e95, ActivityInfo{e76cdd9 com.android.managedprovisioning.BootReminder}
05-10 23:19:05.411   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 235, android.os.BinderProxy@3135f11, ActivityInfo{257abaa com.android.mms.transaction.MmsSystemEventReceiver}

05-10 23:19:05.552   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 140, android.os.BinderProxy@3135f11, ActivityInfo{cabc367 com.android.mms.transaction.SmsSystemEventReceiver}
05-10 23:19:20.485   990  1248 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 38, android.os.BinderProxy@3135f11, ActivityInfo{9965457 com.android.mms.transaction.SmsReceiver}

05-10 23:19:20.672   990  1562 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 4, android.os.BinderProxy@3135f11, ActivityInfo{2b8ad62 com.mediatek.mwi.MwiReceiver}
05-10 23:19:20.687   990  1689 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 14, android.os.BinderProxy@b74cb7e, ActivityInfo{941def3 com.android.services.telephony.sip.SipBroadcastReceiver}
05-10 23:19:20.696   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 9, android.os.BinderProxy@b74cb7e, ActivityInfo{5ec27b0 com.android.phone.vvm.omtp.OmtpBootCompletedReceiver}
05-10 23:19:20.710   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 13, android.os.BinderProxy@b74cb7e, ActivityInfo{bc4b629 com.mediatek.settings.cdma.LteDataOnlySwitchReceiver}
05-10 23:19:20.729   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 17, android.os.BinderProxy@e40edc, ActivityInfo{ef83dae com.android.providers.calendar.CalendarReceiver}
05-10 23:19:21.020   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 291, android.os.BinderProxy@b7f0147, ActivityInfo{3be1534 com.android.providers.downloads.DownloadReceiver}
05-10 23:19:21.042   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 21, android.os.BinderProxy@b7f0147, ActivityInfo{4b67c0b com.android.providers.media.MediaScannerReceiver}
05-10 23:19:22.184   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 34, android.os.BinderProxy@b7f0147, ActivityInfo{2efea1a com.android.providers.media.MtpReceiver}
05-10 23:19:22.192   990   990 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 7, android.app.ActivityThread$ApplicationThread@c33110a, ActivityInfo{260a5d4 com.mediatek.telecom.BootCompletedBroadcastReceiver}
05-10 23:19:22.386   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 194, android.os.BinderProxy@e33fb1, ActivityInfo{1385c1f com.mediatek.settings.RestoreRotationReceiver}
05-10 23:19:22.614   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 226, android.os.BinderProxy@63acd07, ActivityInfo{f5ac92b com.android.ssassist.statistics.AllReceiver}
05-10 23:19:22.656   990  1263 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 43, android.os.BinderProxy@b74cb7e, ActivityInfo{a84029c com.android.stk.BootCompletedReceiver}
05-10 23:19:23.911   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 1252, android.os.BinderProxy@b8e4e73, ActivityInfo{64b05f7 com.google.android.finsky.receivers.BootCompletedReceiver}
05-10 23:19:24.271   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 154, android.os.BinderProxy@8f9579a, ActivityInfo{48b9445 d.f.core.BluetoothReceiver}
05-10 23:19:24.457   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 184, android.os.BinderProxy@b27c829, ActivityInfo{e833ff2 com.dianxinos.powermanager.PowerMgrReceiver}


05-10 23:19:25.088   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 630, android.os.BinderProxy@9ff6216, ActivityInfo{d181abb com.cootek.presentation.service.PresentationServiceReceiver}
05-10 23:19:39.714   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 31, android.os.BinderProxy@31d2e5a, ActivityInfo{b918e05 com.freeme.launcher.StartupReceiver}

05-10 23:19:41.099   990  1689 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 1382, android.os.BinderProxy@799c814, ActivityInfo{b28588b com.google.android.apps.gmm.navigation.service.detection.StartDetectionReceiver}
05-10 23:19:41.126   990  1263 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 26, android.os.BinderProxy@799c814, ActivityInfo{a555180 com.google.android.apps.gmm.ugc.ataplace.StartAtAPlaceReceiver}
05-10 23:19:41.186   990  1248 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 60, android.os.BinderProxy@799c814, ActivityInfo{8b0c75f com.google.android.apps.gmm.offline.StartAutoUpdatesCheckingReceiver}
05-10 23:19:42.180   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 993, android.os.BinderProxy@7964357, ActivityInfo{7cde20a com.google.android.apps.photos.camerashortcut.CameraShortcutBroadcastReceiver}
05-10 23:19:42.666   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 65, android.os.BinderProxy@7964357, ActivityInfo{cd8b047 com.google.android.apps.photos.jobscheduler.PhotosJobReschedulerBroadcastReceiver}
05-10 23:19:42.762   990  1689 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 95, android.os.BinderProxy@7964357, ActivityInfo{a947409 com.google.android.apps.photos.mediamonitor.DeviceBootAppUpgradeBroadcastReceiver}
05-10 23:19:42.874   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 112, android.os.BinderProxy@7964357, ActivityInfo{bdfa8d4 com.google.android.libraries.social.notifications.impl.BootCompletedReceiver}
05-10 23:19:43.539   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 622, android.os.BinderProxy@a47f770, ActivityInfo{8a8ef04 com.google.android.apps.tachyon.BootReceiver}
05-10 23:19:43.791   990  1248 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 251, android.os.BinderProxy@3b3a5cd, ActivityInfo{e098191 com.google.android.configupdater.CertPin.CertPinUpdateRequestReceiver}
05-10 23:19:43.815   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 23, android.os.BinderProxy@3b3a5cd, ActivityInfo{84b780b com.google.android.configupdater.IntentFirewall.IntentFirewallUpdateRequestReceiver}
05-10 23:19:43.847   990  1263 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 32, android.os.BinderProxy@3b3a5cd, ActivityInfo{a37a6e8 com.google.android.configupdater.SmsShortCodes.SmsShortCodesUpdateRequestReceiver}
05-10 23:19:43.859   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 11, android.os.BinderProxy@3b3a5cd, ActivityInfo{dfcf101 com.google.android.configupdater.ApnDb.ApnDbUpdateRequestReceiver}
05-10 23:19:43.874   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 15, android.os.BinderProxy@3b3a5cd, ActivityInfo{de71ba6 com.google.android.configupdater.TzData.TzDataUpdateRequestReceiver}
05-10 23:19:43.907   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 33, android.os.BinderProxy@3b3a5cd, ActivityInfo{2e8dd83 com.google.android.configupdater.SELinux.SELinuxUpdateRequestReceiver}
05-10 23:19:43.933   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 26, android.os.BinderProxy@3b3a5cd, ActivityInfo{dd7372c com.google.android.configupdater.CarrierProvisioningUrls.CarrierProvisioningUrlsUpdateRequestReceiver}
05-10 23:19:44.889   990  1263 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 955, android.os.BinderProxy@8231c71, ActivityInfo{f7a15f5 com.google.android.gm.GoogleMailDeviceStartupReceiver}
05-10 23:19:44.975   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 31, android.os.BinderProxy@8231c71, ActivityInfo{36db2d5 com.google.android.gm.MailIntentReceiver}
05-10 23:19:45.053   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 17, android.os.BinderProxy@8231c71, ActivityInfo{829ccf9 com.android.email.service.EmailBroadcastReceiver}
05-10 23:19:45.096   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 16, android.os.BinderProxy@8231c71, ActivityInfo{cdf764a com.google.android.gm.utils.ExchangeUpgradeReceiver}
05-10 23:19:45.117   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 19, android.os.BinderProxy@8231c71, ActivityInfo{86c19bb com.android.email.task.notification.TaskReminderReceiver}
05-10 23:19:45.170   990  1562 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 41, android.os.BinderProxy@ab89697, ActivityInfo{e032231 com.google.android.gms.chimera.GmsIntentOperationService$PersistentTrustedReceiver}
05-10 23:19:45.233   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 62, android.os.BinderProxy@ab89697, ActivityInfo{db6b06d com.google.android.gms.auth.setup.notification.PersistentNotificationBroadcastReceiver}
05-10 23:19:45.419   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 185, android.os.BinderProxy@75e48ee, ActivityInfo{7f7f0f0 com.google.android.gms.games.chimera.GamesSystemBroadcastReceiverProxy}
05-10 23:19:52.386   990  1689 V ActivityManager: Broadcast: Intent { act=com.google.android.googlequicksearchbox.interactor.BOOT_COMPLETED flg=0x10 pkg=com.google.android.googlequicksearchbox } ordered=false userid=0 callerApp=ProcessRecord{36a05fa 2848:com.google.android.googlequicksearchbox:search/u0a28}
05-10 23:19:52.387   990  1689 V ActivityManager: Enqueing broadcast: com.google.android.googlequicksearchbox.interactor.BOOT_COMPLETED replacePending=false
05-10 23:19:52.396   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 6976, android.os.BinderProxy@86ff9a1, ActivityInfo{371c18f com.google.android.apps.gsa.search.core.StartUpReceiver}
05-10 23:19:52.489   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 36, android.os.BinderProxy@86ff9a1, ActivityInfo{d347c21 com.google.android.apps.gsa.googlequicksearchbox.GelStubAppWatcher}
05-10 23:19:52.520   990  1263 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 30, android.os.BinderProxy@584ea1e, ActivityInfo{2c782a0 com.google.android.apps.inputmethod.libs.framework.core.LauncherIconVisibilityInitializer}
05-10 23:19:52.722   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 201, android.os.BinderProxy@b5b2793, ActivityInfo{70cd01b com.google.android.onetimeinitializer.OneTimeInitializerReceiver}
05-10 23:19:53.619   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 865, android.os.BinderProxy@9d3cf7e, ActivityInfo{9efc232 com.google.android.partnersetup.GooglePartnerSetup}
05-10 23:19:54.217   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 23, android.os.BinderProxy@9d3cf7e, ActivityInfo{fdb7b06 com.google.android.partnersetup.RlzPingBroadcastReceiver}
05-10 23:19:54.689   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 352, android.os.BinderProxy@75b4eb7, ActivityInfo{39d8fdb com.google.android.syncadapters.contacts.ContactsSyncAdapterBroadcastReceiver}
05-10 23:19:54.834   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 38, android.os.BinderProxy@8d36567, ActivityInfo{ca2c945 com.freeme.sc.network.monitor.receiver.NWM_Receiver}
05-10 23:19:54.976   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 143, android.os.BinderProxy@52753a7, ActivityInfo{75e25cb com.mediatek.atci.service.AtciIntentReceiver}
05-10 23:19:55.151   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 171, android.os.BinderProxy@939109f, ActivityInfo{5cbf0f2 com.mediatek.batterywarning.BatteryWarningReceiver}
05-10 23:19:55.306   990  1689 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 154, android.os.BinderProxy@d367497, ActivityInfo{a56b5ec com.mediatek.connectivity.CdsInfoReceiver}
05-10 23:19:55.519   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 213, android.os.BinderProxy@465d6f0, ActivityInfo{b7fd684 com.mediatek.engineermode.boot.EmBootupReceiver}
05-10 23:19:55.650   990  1689 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 130, android.os.BinderProxy@b524e25, ActivityInfo{479d469 com.mediatek.omacp.message.OmacpReceiver}
05-10 23:19:55.859   990  1563 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 163, android.os.BinderProxy@b47e8b4, ActivityInfo{1a99408 com.mediatek.providers.drm.BootCompletedReceiver}
05-10 23:19:55.984   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 125, android.os.BinderProxy@ed40f9b, ActivityInfo{1d59f9e com.mediatek.schpwronoff.AlarmInitReceiver}
05-10 23:19:56.260   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 205, android.os.BinderProxy@cf72c4e, ActivityInfo{df7f202 com.mediatek.simprocessor.BootCmpReceiver}
05-10 23:19:56.481   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 200, android.os.BinderProxy@b9fb668, ActivityInfo{b402e7c com.mediatek.thermalmanager.ServiceStarter}
05-10 23:19:56.651   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 169, android.os.BinderProxy@d4dfb2, ActivityInfo{9048a81 com.ptns.da.notification.NotizycationReceiverhi}
05-10 23:19:56.962   990  1001 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 310, android.os.BinderProxy@43ff45f, ActivityInfo{c77ab03 com.baidu.simeji.common.push.WakeupBroadcastReceiver}
05-10 23:19:57.762   990  1562 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 29, android.os.BinderProxy@3e6cc42, ActivityInfo{aef6974 com.baidu.simeji.util.ExternalSignalReceiver}
05-10 23:19:57.778   990  1000 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 14, android.os.BinderProxy@3e6cc42, ActivityInfo{dc96e9d com.baidu.simeji.alive.AliveReceiver}
05-10 23:19:57.926   990  1562 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 146, android.os.BinderProxy@58f0e3f, ActivityInfo{15ee6e3 com.android.cts.splitapp.BootReceiver}
```

可以看到有几个广播之间间隔很久（15s）

```
05-10 23:18:02.641   990  1548 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 1630, android.os.BinderProxy@b27c829, ActivityInfo{7ea7657 com.stools.util.cp.WorkReceiver}
05-10 23:18:17.136   990  1549 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 153, android.os.BinderProxy@2d42b70, ActivityInfo{bd314db com.freeme.ota.app.UpdateReceiver}

05-10 23:18:19.494   990  1121 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 618, android.os.BinderProxy@49fe3c9, ActivityInfo{72f2f6 com.uc.base.push.core.PushProxyReceiver}
05-10 23:18:34.525   990  1265 D ActivityManager: BroadcastRecord{8994db4 u0 android.intent.action.BOOT_COMPLETED}, spend: 49, android.os.BinderProxy@49fe3c9, ActivityInfo{ccb9865 com.taobao.accs.EventReceiver}
...
```

对比37平台，广播之间delay不超过500ms，抓取更多delay时产生的log如下：

```
05-10 23:59:34.497   985  1011 D ActivityManager: BroadcastRecord{59103f5 u0 android.intent.action.BOOT_COMPLETED}, spend: 118, android.os.BinderProxy@5cd6b9e, ActivityInfo{69dda7c com.android.mms.transaction.SmsSystemEventReceiver}
05-10 23:59:34.497  2265  2265 V ActivityThread: SVC-Creating service CreateServiceData{token=android.os.BinderProxy@415c145 className=com.android.mms.transaction.NoneService packageName=com.android.mms intent=null}
05-10 23:59:34.497   985  1011 D BroadcastQueue: BDC-mStartingBackground size = 1 mStartingBackground = [ServiceRecord{1afff05 u0 com.android.mms/.transaction.NoneService}] mMaxStartingBackground = 1
05-10 23:59:34.497   985  1011 I BroadcastQueue: Delay finish: com.android.mms/.transaction.SmsSystemEventReceiver
05-10 23:59:34.498  2265  2265 D NoneService: onCreate
05-10 23:59:34.499  2265  2265 D ActivityThread: SVC-Calling onStartCommand: com.android.mms.transaction.NoneService@e84809a, flags=0, startId=1
... ...
05-10 23:59:49.407   985  1014 I ActivityManager: Waited long enough for: ServiceRecord{1afff05 u0 com.android.mms/.transaction.NoneService}
05-10 23:59:49.407   985  1014 I BroadcastQueue: Resuming delayed broadcast
05-10 23:59:49.408   985  1014 V BroadcastQueue: processNextBroadcast [background]: 0 broadcasts, 28 ordered broadcasts
05-10 23:59:49.408   985  1014 D BroadcastQueue: BroadcastRecord{59103f5 u0 android.intent.action.BOOT_COMPLETED}, #26 ActivityInfo{2b68681 com.android.mms.transaction.SmsReceiver}
```

通过log发现，广播处理完后，进程并没有直接进行下一次广播，而是等待mStartingBackground中service创建，而此service不知为何一直未执行完成，最终导致ActivityManager等待超时，强行执行下一次广播。

查看代码

```
frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
```

```java
    public void finishReceiver(IBinder who, int resultCode, String resultData,
            Bundle resultExtras, boolean resultAbort, int flags) {

      	... ...
        try {
            boolean doNext = false;
            BroadcastRecord r;

            synchronized(this) {
                BroadcastQueue queue = (flags & Intent.FLAG_RECEIVER_FOREGROUND) != 0
                        ? mFgBroadcastQueue : mBgBroadcastQueue;
                r = queue.getMatchingOrderedReceiver(who);
                if (r != null) {
                    /// M: broadcast log enhancement @{
                    if (!IS_USER_BUILD || DEBUG_BROADCAST) {
                        Slog.d(TAG_BROADCAST, r
                            + ", spend: " + (SystemClock.uptimeMillis() - r.receiverTime)
                            + (r.receiver != null ? ", " + r.receiver : "")
                            + (r.curFilter != null ? ", " + r.curFilter : "")
                            + (r.curReceiver != null ? ", " + r.curReceiver : ""));
                    }
                    /// @}

                    doNext = r.queue.finishReceiverLocked(r, resultCode,
                        resultData, resultExtras, resultAbort, true);
                }
            }

            /// donext为true时，才会处理下一个receiver
            if (doNext) {
                r.queue.processNextBroadcast(false);
            }
            trimApplications();
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }
```

```frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java```

```java
	public boolean finishReceiverLocked(BroadcastRecord r, int resultCode,
            String resultData, Bundle resultExtras, boolean resultAbort, boolean waitForServices) {
		... ...

        if (waitForServices && r.curComponent != null && r.queue.mDelayBehindServices
                && r.queue.mOrderedBroadcasts.size() > 0
                && r.queue.mOrderedBroadcasts.get(0) == r) {
            ActivityInfo nextReceiver;
            if (r.nextReceiver < r.receivers.size()) {
                Object obj = r.receivers.get(r.nextReceiver);
                nextReceiver = (obj instanceof ActivityInfo) ? (ActivityInfo)obj : null;
            } else {
                nextReceiver = null;
            }
            // Don't do this if the next receive is in the same process as the current one.
            if (receiver == null || nextReceiver == null
                    || receiver.applicationInfo.uid != nextReceiver.applicationInfo.uid
                    || !receiver.processName.equals(nextReceiver.processName)) {

              	///  判断当前是否存在后台服务
                if (mService.mServices.hasBackgroundServices(r.userId)) {
                    /// M: broadcast log enhancement @{
                    ActiveServices.ServiceMap smap = mService.mServices.mServiceMap.get(r.userId);
                    if (smap != null) {
                        Slog.d(TAG_BROADCAST, "BDC-mStartingBackground size = "
                            + smap.mStartingBackground.size()
                            + " mStartingBackground = "
                            + smap.mStartingBackground + " mMaxStartingBackground = "
                            + mService.mServices.mMaxStartingBackground);
                    }
                    /// @}

                    Slog.i(TAG, "Delay finish: " + r.curComponent.flattenToShortString());
                    r.state = BroadcastRecord.WAITING_SERVICES;
                    /// 有后台服务则暂停处理下一个receiver，并得等待service完成
                    return false;
                }
            }
        }
		... ...
    }
```

```hasBackgroundServices()``` 函数实现于

```/frameworks/base/services/core/java/com/android/server/am/ActiveServices.java```

```java
    /// 判断当前callingUser的后台服务时是否大于最大的后台服务数
    boolean hasBackgroundServices(int callingUser) {
        ServiceMap smap = mServiceMap.get(callingUser);
        return smap != null ? smap.mStartingBackground.size() >= mMaxStartingBackground : false;
    }

    public ActiveServices(ActivityManagerService service) {
        mAm = service;
        int maxBg = 0;
        try {
            /// 80平台 ro.config.max_starting_bg 属性为空
            maxBg = Integer.parseInt(SystemProperties.get("ro.config.max_starting_bg", "0"));
        } catch(RuntimeException e) {
        }
        mMaxStartingBackground = maxBg > 0
                ? maxBg : ActivityManager.isLowRamDeviceStatic() ? 1 : 8; 
    }
```

在平台低内存时，系统配置最大后台可启动服务数为1，而对比log发现部分应用此时后台服务数正好为1个，所以receiver状态被置为`BroadcastRecord.WAITING_SERVICES` ， 最终等待15s而超时

## 解决方法

建议CTS测试版本将属性`ro.config.max_starting_bg` 属性配置成数值`4`或以上

High Mem settings

Due to too much problems with using 'ro.config.force_highgfx=true' in conjunction with low_mem device setting i decided to remove this property. Instead i've added and used three other properties to get our Nexus S running in 'high mem' mode without loosing too much performance. To change to high mem mode i recommend to set/change the follwing properties in system/build.prop:

ro.config.low_ram=false
ro.config.max_recent_tasks=10
ro.config.max_starting_bg=2

https://forum.xda-developers.com/nexus-s/development/rom-crespo-build-11-30-04-2014-t2567919
