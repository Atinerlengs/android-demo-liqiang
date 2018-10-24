[TOC]
# 方案介绍

## 前言
   在众多导致高功耗的因素中，wakeup 类型（如下表格介绍，接下来章节中使用“wakeup类型”）的 alarm 事件是一个常见的因素。

   在遇到功耗问题的时候，我们首先对这类功耗源进行数据收集和分析，基于数据分析的结果，制定相应的优化策略。


type | 参数
---|---
0 | AlarmManager.ELAPSED_REALTIME_WAKEUP
2 | AlarmManager.RTC_WAKEUP

表1-1

## 思路简介
   - 每次 alarm 事件被触发的时候，开始记录数据并保存
   - 挑出 wakeup 类型的 alarm 事件，进行分析
   - 根据 alarm 事件触发的平均时间长短，分为如下三个等级

alarm事件的平均触发时间间隔（单位：s） | 等级
---|---
小于 360 | warning
小于 300 | serious
小于 60  | worst

表1-2

  - 对于特殊alarm事件（如：action = syncmanager.SYNC_ALARM）进行分析，判断是否是造成高功耗，如果是，对其进行功耗优化



# 具体实现

参考文件：
> vendor/freeme/frameworks/base/services-export/core/java/com/freeme/server/FreemeAlarmWakeupDetection.java

## 数据收集：

**1. 首先，所有 alarm 事件触发之后，记录其数据并保存**

**2. 其次，通过自学习算法计算出，某一时间段的 alarm 事件的平均触发时间，保存相关信息到列表中**

**3. 接着，根据 alarm 事件的平均触发时间长短，分为三个级别（如表1-2）**

**4. 最后，上述所有数据写到 log 文件中，保存在 data 分区下**

如下是代码实现部分（供参考）

每次在 alarm 服务被触发的时候，都会调用 AlarmManagerServices.java 中的 alarmTriggerFrequentDetection（）方法。

该方法重要事项： 通过自学习算法得到，某一时间段内，第一次触发 alarm 事件到第 n 次触发的平均触发时间差，满足一定阈值就添加到记录列表中

```java

    private boolean alarmWakeupDetection(BroadcastStats bs, long nowELAPSED, long lastTimeWakeup) {
        boolean detected = false;
        if (nowELAPSED - lastTimeWakeup >= 1200000) {
            resetState(bs, nowELAPSED, 0, true, false);
            return false;
        }
        int numAllWakeup = bs.numAllWakeup - bs.numWakeupWhenScreenoff;
        Slog.w(ATAG, "alarmWakeupDetection: numAllWakeup="+numAllWakeup );

        if (numAllWakeup <= 0 /*|| numAllWakeup % 5 != 0*/) {
            return false;
        }
        long totalCheckTime = (bs.lastTimeWakeup - bs.wakeupCountStartTime) / 1000;
        Slog.w(ATAG, "alarmWakeupDetection: totalCheckTime="+totalCheckTime );
        if (totalCheckTime <= 0) {
            return false;
        }
        long numSecondsPerWakeup = totalCheckTime / ((long) numAllWakeup);
        Slog.w(ATAG, "alarmWakeupDetection: numSecondsPerWakeup="+numSecondsPerWakeup +" " +
                "totalCheckTime+"+totalCheckTime);
        if (numSecondsPerWakeup <= thresholdSeriousPerWakeup) {
            mWakeLockCheck.acquire(500);
            Message msg = mHandlerTimeout.obtainMessage();
            msg.what = MSG_CHECK_ALARM_WAKEUP;//通过消息机制，开始调用alarmWakeupHandle(...)
            Bundle data = new Bundle();
            data.putInt("uid", bs.mUid);
            data.putString("pkg", bs.mPackageName);
            data.putLong("totalCheckTime", totalCheckTime);
            data.putLong("numSecondsPerWakeup", numSecondsPerWakeup);
            msg.setData(data);
            mHandlerTimeout.sendMessage(msg);
            detected = true;
        }
        return detected;
    }

```

通过 MSG_CHECK_ALARM_WAKEUP 消息处理机制，调用方法 alarmWakeupHandle() 。

该方法作用是：把已经通过自学习算法，且满足条件的alarm 添加到记录列表中

```java

    private void alarmWakeupHandle(Bundle data) {
        Slog.w(TAG, "alarmWakeupHandle: mScreenOn=" + mScreenOn);

        if (!mScreenOn && data != null) {
            String pkgName = data.getString(KEY_PKG);
            int uid = data.getInt(KEY_UID);
            long totalCheckTime = data.getLong(KEY_TOTAL_CHECK_TIME);
            long numSecondsPerWakeup = data.getLong(KEY_NUMSECONDS_PER_WAKEUP);
            if (pkgName != null) {
                synchronized (mLock) {
                    ArrayMap<String, BroadcastStats> uidStats = (ArrayMap) mBroadcastStats.get(uid);
                    if (uidStats == null) {
                        Slog.w(TAG, "alarmWakeupHandle: uidStats == null");
                        return;
                    }
                    BroadcastStats bs = (BroadcastStats) uidStats.get(pkgName);
                    if (bs == null) {
                        Slog.w(TAG, "alarmWakeupHandle: bs == null");
                        return;
                    }
                    ArrayList<AlarmIntentRecord> alarmIntentRecordList = new ArrayList();
                    for (int is = 0; is < bs.filterStats.size(); is++) {
                        FilterStats fs = (FilterStats) bs.filterStats.valueAt(is);
                        int fsNumWakeup = fs.numWakeup - fs.numWakeupWhenScreenoff;
                        Slog.w(TAG, "alarmWakeupHandle: fsNumWakeup=" + fsNumWakeup);
                        if (fsNumWakeup > 0) {
                            alarmIntentRecordList.add(new AlarmIntentRecord(fs.mTag, fsNumWakeup, true));
                            if (!isSyncAlarmWakeupFrequent && fsNumWakeup >= 5 && bs.mUid == 1000 && fs.mTag != null && fs.mTag.endsWith("syncmanager.SYNC_ALARM") && totalCheckTime / ((long) fsNumWakeup) <= mThresholdSeriousPerWakeup) {
                                isSyncAlarmWakeupFrequent = true;//这里特别对syncmanager.SYNC_ALARM做了学习计算，如果超过5次且平均触发的时间间隔小于300s，则认为是高耗电源，下一次就需要改变该alarm的type，详情参看下一节“功耗优化方面”
                                if (ADBG) {
                                    Tracer.w(TAG, "alarmWakeupHandle: isSyncAlarmWakeupFrequent " +
                                            "set true!");
                                }
                            }
                        }
                    }
                    Slog.w(TAG, "alarmWakeupHandle: alarmWakeupHandle end!");
                    Collections.sort(alarmIntentRecordList, mComparatorIntent);
                    AlarmWakeupRecord record = addReportList(bs.mUid, numSecondsPerWakeup,
                            totalCheckTime, pkgName, alarmIntentRecordList, true, true, bs
                                    .numCanceledWakeup);//添加到列表中，这里包括update和add 两个操作
                }
            }
        }
    }

```

及时更新并添加 alarm 数据到 mReportList 列表中

```java

    private AlarmWakeupRecord addReportList(int uid, long wakeupInterval, long totalCheckTime, String pkgName, ArrayList<AlarmIntentRecord> alarmIntentRecordList, boolean notRestrictApp, boolean isWakeup, int numCanceledWakeup) {
        if (pkgName == null) {
            return null;
        }
        AlarmWakeupRecord alarmWakeupRecord = getFrequentAlarm(pkgName);
        if (alarmWakeupRecord != null) {//update操作
            if (ADBG) Slog.w(TAG, "addReportList: update");
            alarmWakeupRecord.update(wakeupInterval, totalCheckTime, alarmIntentRecordList, isWakeup, numCanceledWakeup);
        } else {//作为一个new的alarm 添加到列表中
            if (ADBG) Slog.w(TAG, "addReportList: new");
            alarmWakeupRecord = new AlarmWakeupRecord(uid, wakeupInterval, totalCheckTime, pkgName, alarmIntentRecordList, notRestrictApp, isWakeup, numCanceledWakeup);
            mReportList.add(alarmWakeupRecord);
        }
        return alarmWakeupRecord;
    }

```

所有的符合条件的 wakeup 类型 alarm 事件都会被记录在 mReportList 列表中。

但是在 screen on 的时候，会调用 mReportList.clear() 方法 ，该列表会被清除，这样会在下次 screen off 的时候重新记录数据。

## 功耗优化：

 **1. 对 action = syncmanager.SYNC_ALARM 的 alarm 事件进行自学习分析**

 **2. 判断 alarm 事件的平均触发时间是否小于300s。如果是，在接下来处理 alarm 事件的时候，对其进行功耗优化**

 **3. 对其优化方案：把该alarm事件的类型从 wakeup 转为非 wakeup ，从而达到降功耗的效果**

 优化前 alarm 事件类型（wakeup类） | 优化后alarm事件类型（非wakeup类）
---|---
 AlarmManager.RTC_WAKEUP | AlarmManager.RTC
AlarmManager.ELAPSED_REALTIME_WAKEUP | AlarmManager.ELAPSED_REALTIME





如下是代码实现部分（供参考）

 对系统级别的 alarm （action= syncmanager.SYNC_ALARM) （用于定时同步账号信息）做了学习判断，如果两个 action 的 alarm 平均触发时间间隔小于300s，即认为是需要降功耗处理，会在接下来对该其做降功耗处理

```java
    public int SyncAlarmHandle(int type, PendingIntent pi) {
        if (mScreenOn || (type != AlarmManager.RTC_WAKEUP && type != AlarmManager.ELAPSED_REALTIME_WAKEUP)) {
            return type;
        }
        // 通过自学习一段时间，发现是高功耗action，这里的isSyncAlarmWakeupFrequent = true
        if (isSyncAlarmWakeupFrequent && pi.getCreatorUid() == 1000) {
            String tag = pi.getTag("");
            if (tag != null && tag.endsWith("syncmanager.SYNC_ALARM")) {
                if (type == AlarmManager.RTC_WAKEUP) {
                    type = AlarmManager.RTC;// 把原来的RTC_WAKEUP 改成 RTC ,非唤醒类
                } else if (type == AlarmManager.ELAPSED_REALTIME_WAKEUP) {
                    type = AlarmManager.ELAPSED_REALTIME;//转成ELAPSED_REALTIME 非唤醒类
                }
            }
        }
        return type;
    }
```

# 方案小结

1. 主要通过自学习算法，记录每次 screen off 到 screen on 之间，wakeup 类型的 alarm 事件的触发情况。在下一次 screen on 的时候，通过匹配算法，计算同一个 pkg 、同一类型的 alarm 事件的平均触发时间间隔，并分成三个等级，保存数据。

2.  (Freeme添加的优化项) 一般 alarm 导致高功耗问题，出现在灭屏之后。该方案记录的是：在每次 screen off 到下次 screen on 之间触发的所有 wakeup 类型的 alarm 事件，同时对这些数据做初步的分析并输出，便于开发人员分析功耗问题。

# 后续可优化/扩展点
## 添加黑白名单列表机制（评估是否需要添加）

- 黑名单 （功耗优化方面）：对黑名单中的 pkg ，当出现 type 为 wakeup 的时候改为非 wakeup 类型，省去自学习的过程和时间
- 白名单 （数据收集方面）：专门收集特定 pkgname 的 alarm ，用于特殊场景的 alarm 分析