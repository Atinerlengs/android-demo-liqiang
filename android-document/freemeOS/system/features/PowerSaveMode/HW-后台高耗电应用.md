[TOC]

# 后台高耗电应用

---

功能说明：列出所有正在运行的后台高耗电应用

会列出如下：阻止系统休眠，频繁定位，频繁唤醒系统

---

需要调研的方向：

- 核心：监控后台应用耗电的算法。
- 界面上相对比较独立化
- 一键结束功能

##  核心：监控后台应用耗电的算法

一堆类 之间调用，看的想吐。这能参考大概的设计逻辑，然后我们据此自己开发

目前知道监控项有：cpu 使用时间，对gps的调用，唤醒调用

这部分可以参考 “应用”---“电池” 中有接口。




## “一键结束”功

```
        if (!packageName.isEmpty()) {
            SysCoreUtils.forceStopPackageAndSyncSaving(this.mContext.getApplicationContext(), packageName);
        }


    public static void forceStopPackageAndSyncSaving(Context context, List<String> packages) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        for (String pkg : packages) {
            HwLog.d(TAG, "Force stop package: " + pkg);
            activityManager.forceStopPackage(pkg);
            ProviderWrapper.updateWakeupNumDBSingle(context, pkg);
        }
    }
```

核心就是这个。





