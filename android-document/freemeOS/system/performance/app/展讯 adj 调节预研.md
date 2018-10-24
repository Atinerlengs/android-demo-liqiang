[TOC]


# 展讯 Adj调节 - 预研

## 平台
sp7731 - Android 8.1

## 相关文件及目录
```
frameworks/.../ActivityManagerService.java （adj调节入口及部分公开api）
vendor/sprd/.../performance/PerformanceManagerService.java (性能优化服务，包含adj、ram、startingwindow等)
vendor/sprd/.../performance/ProcessPrioAjustment.java （Adj调节核心实现）
vendor/sprd/.../am/ActivityManagerServiceEx.java （AMS部分桩函数）
```
## 方案简述
1. 根据应用的使用频率动态调整起Adj
2. 根据当前的内存使用情况，调整需要特定应用的Adj

## 实现过程
阶段一：拦截单独app相关adj调节入口，根据策略返回对应的处理结果（增加特定应用的Adj）

``` java
//frameworks/.../ActivityManagerService.java
    private final boolean updateOomAdjLocked(ProcessRecord app, int cachedAdj,
            ProcessRecord TOP_APP, boolean doingAll, long now) {
        if (app.thread == null) {
            return false;
        }

        computeOomAdjLocked(app, cachedAdj, TOP_APP, doingAll, now);
        ProcessInfo info = createProcessInfo(app);
        int tunningResult = processLmkAdjTunningIfneeded(info, mLastMemoryLevel, now, SystemClock.elapsedRealtime());
        if (tunningResult == ProcessInfo.PROCESS_LMK_ADJ_TUNNING_INCREASE) {
            return true;
        }
        return applyOomAdjLocked(app, doingAll, now, SystemClock.elapsedRealtime());
    }

```

根据 processLmkAdjTunningIfneeded 函数返回值判断需不需增加app的adj

``` java
// vendor/sprd/.../PerformanceManagerService.java
    public int processLmkAdjTunningIfneeded(ProcessInfo app, int memLvl, long now, long nowElapsed) {
        if (mProcessPrioAjustment == null) {
            return ProcessInfo.PROCESS_LMK_ADJ_TUNNING_NONEED;
        }
        if (mProcessPrioAjustment.increaseAdjIfNeeded(app, memLvl, now, nowElapsed)) {
            return ProcessInfo.PROCESS_LMK_ADJ_TUNNING_INCREASE;
        } else if (mProcessPrioAjustment.doAdjDropIfNeeded(app, memLvl, now, nowElapsed)) {
            return ProcessInfo.PROCESS_LMK_ADJ_TUNNING_DECREASE;
        } else {
            return ProcessInfo.PROCESS_LMK_ADJ_TUNNING_NONEED;
        }
    }
```

如果想返回 `PROCESS_LMK_ADJ_TUNNING_INCREASE` 则必须 `increaseAdjIfNeeded` 返回`true`

``` java
// vendor/sprd/.../ProcessPrioAjustment.java
    // increase hot apps while adj > HOT_APP_ADJ, let it stay in memory
    // increase recent apps while adj > CACHED, let it stay in memory for a while.
    public  boolean increaseAdjIfNeeded(ProcessInfo app, int memFactor, long now, long nowElapsed) {
        // system 应用不做调节
        if (app != null && (app.flags & ApplicationInfo.FLAG_SYSTEM)!= 0) {
            return false;
        }
        // 特殊应用（cts）不做调节
        if (app != null && app.packageName != null && isSpecialPackage(app.packageName)) {
            return false;
        }

        ... ...
        // 如果为 HotApp 且当前adj 大于 HOT_APP_ADJ，调整其为 HOT_APP_ADJ
        if (app.curAdj > HOT_APP_ADJ && isProcessHotApp(app)) {
            ... ...
            app.adjTunned = true;
            app.tunnedAdj = HOT_APP_ADJ;
            ... ...
            // 调整完成，重新apply到ams中对应的app中
            mPerformanceManager.applyOomAdjByProcessInfo(app, now, nowElapsed);
            return true;
        // 如果为近期刚使用过的应用且当前 adj 大于 CACHED_APP_MIN_ADJ，调整其为 RECENT_FOCUS_APP_ADJ
        } else if (app.curAdj >= ProcessInfo.CACHED_APP_MIN_ADJ && isPackageRecentUsedForAWhile(app, memFactor)) {
            ... ...
            app.adjTunned = true;
            app.tunnedAdj = RECENT_FOCUS_APP_ADJ;
            ... ...
            // 调整完成，重新apply到ams中对应的app中
            mPerformanceManager.applyOomAdjByProcessInfo(app, now, nowElapsed);
            return true;
        // 如果为关联启动的应用且当前 adj 大于 CACHED_APP_MIN_ADJ，调整其为 RELEVANCE_APP_ADJ
        } else if (app.curAdj >= ProcessInfo.CACHED_APP_MIN_ADJ && isPackageRelevance(app) &&
            app.processName.equals(app.packageName)) {
            ... ...
            app.adjTunned = true;
            app.tunnedAdj = RELEVANCE_APP_ADJ;
            ... ...
            // 调整完成，重新apply到ams中对应的app中
            mPerformanceManager.applyOomAdjByProcessInfo(app, now, nowElapsed);
            return true;
        }
        return false;
    }

```

阶段二：拦截系统全局Adj调节接口 （减低特定应用的Adj）

``` java
//frameworks/..*./ActivityManagerService.java
    final void updateOomAdjLocked() {
        ... ...
        ArrayList<ProcessInfo> needTunning = new ArrayList<>();
        for (int i=N-1; i>=0; i--) {
                ... ...
                //add for performance begin
                ProcessInfo info = createProcessInfo(app);
                int tunningResult = processLmkAdjTunningIfneeded(info, mLastMemoryLevel, now, nowElapsed);
                switch (tunningResult) {
                    case ProcessInfo.PROCESS_LMK_ADJ_TUNNING_INCREASE:
                        break; // 增加adj，已处理
                    case ProcessInfo.PROCESS_LMK_ADJ_TUNNING_DECREASE:
                        needTunning.add(info); // 减低adj的添加到调整列表
                        break;
                    case ProcessInfo.PROCESS_LMK_ADJ_TUNNING_NONEED:
                        applyOomAdjLocked(app, true, now, nowElapsed);// 系统默认接口
                        break;
                    default:
                        break;
                }
                //add for performance end
                ... ...
        }
        ... ...

        memFactor = tunningLowPrioProcessesLocked(needTunning, memFactor);// 根据不同的内存使用情况，进行对应的adj调节
    }

```

来看下返回值为 `PROCESS_LMK_ADJ_TUNNING_DECREASE` 的策略

``` java
// vendor/sprd/.../ProcessPrioAjustment.java
    public  boolean doAdjDropIfNeeded(ProcessInfo app, int memFactor, long now, long nowElapsed) {
        if (mDropAdjEnabled == false || memFactor < ProcessStats.ADJ_MEM_FACTOR_MODERATE) {
            return false;
        }
        // system apps always go first
        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return false;
        }
        // pass special pkgs
        if (app != null && app.packageName != null && isSpecialPackage(app.packageName)) {
            return false;
        }
        // if app holds a wakelock, let it(or them ) go, may bg playing music or bg download sth.
        if (mPerformanceManager.isProcessHeldWakeLock(app.uid)) {
            return false;
        }
        if (!isProcessAdjCare(app.curAdj)) {
            return false;
        }
        if (!isPackageBeenNotUsedForLongTime(app, memFactor)) {
            return false;
        }
        if (isPackageMayBusyWithSth(app, memFactor)) {
            return false;
        }
        // 非hotapp
        if (isProcessHotApp(app)) {
            return false;
        }
        // 非近期刚使用的应用
        if (isPackageRecentUsedForAWhile(app, memFactor)) {
            return false;
        }
        if (DEBUG_PRIOADJ) {
            Slog.e(TAG, "doAdjDropIfNeeded:"+app);
        }
        return true;
    }

```

最后来看下sprd是如何根据不同的内存使用量进行调节的

``` java

    public void doLowPrioAppsTunningInner(int memFactor, ArrayList<ProcessInfo> lruProcesses, long now, long nowElapsed) {
        //reduce nums of 'persist' Apps while under mem pressure.
        if (lruProcesses.size() == 0) {
            return;
        }

        // 根据最近使用情况进行排序
        Collections.sort(lruProcesses, new Comparator<ProcessInfo>() {
            @Override
            public int compare(ProcessInfo lhs, ProcessInfo rhs) {
                // TODO:special case is the "child process" like com.douniwan:push,now we use package launchCount.
                if (getPackageLaunchCount(lhs.packageName) == getPackageLaunchCount(rhs.packageName)) {
                    long lus = getPackageLastUseTime(lhs.packageName);
                    long rus = getPackageLastUseTime(rhs.packageName);
                    if (lus == rus) {
                        return 0;
                    } else {
                        return lus > rus ? -1 : 1;
                    }
                }
                return getPackageLaunchCount(lhs.packageName) > getPackageLaunchCount(rhs.packageName) ? -1 : 1;
            }
        });
        for (AdjDropParams parms : mAdjDropParams) {
            parms.restCount();
        }

        // 调节每一个应用的adj
        for (ProcessInfo app : lruProcesses) {
            dropAdj(app, memFactor, now, nowElapsed);
        }
    }

    private void dropAdj(ProcessInfo app, int memFactor, long now, long nowElapsed) {
        int index = adjToIndex(app.curAdj);
        if (index == ADJ_DROP_INVALID) {
            Slog.w(TAG, "sth bad happend...");
            return;
        }
        AdjDropParams parms = mAdjDropParams[index];
        app.adjTunned = false;
        // 整体内存使用达到制定次数后，开始调节
        if (parms.numbCurrent < parms.numbLimit && parms.pssCurrent + app.lastPss < parms.pssLimit) {
            //ok,let it be
            parms.numbCurrent++;
            parms.pssCurrent += app.lastPss;
        } else {
            //oh,full... let it down..
            if (app.curAdj != parms.dropTo[memFactor]) {
                if (DEBUG_PRIOADJ) {
                    Slog.d(TAG, app.processName + "pid" + app.pid + " adj:" + app.curAdj + "  drop to " + parms.dropTo[memFactor] +
                            " current pss = " + parms.pssCurrent + ", numb = " + parms.numbCurrent + "current memPresure = " +
                            memFactor + "launch cout = " + getPackageLaunchCount(app.packageName));
                }
                app.adjTunned = true;
                app.tunnedAdj = parms.dropTo[memFactor];
                //dropSchedGroupLocked(app);
            }
        }
        mPerformanceManager.applyOomAdjByProcessInfo(app, now, nowElapsed);
    }

```

请注意 `dropAdj` 函数中 `adjToIndex` 其只返回需要调节的应用类型，因为展讯目前只配置部分特定的应用的调节策略，如下

``` java

    private int adjToIndex(int adj) {
        switch (adj) {
            case ProcessInfo.VISIBLE_APP_ADJ:
            case ProcessInfo.PERCEPTIBLE_APP_ADJ:
                return ADJ_DROP_PERCEPTIBAL; // 核心或者前台应用
            case ProcessInfo.SERVICE_ADJ:
                return ADJ_DROP_SERVICEA; // 普通service
            case ProcessInfo.SERVICE_B_ADJ:
                return ADJ_DROP_SERVICEB; // 普通service
            default:
                return ADJ_DROP_INVALID; //不做调整
        }
    }

    private AdjDropParams[] mAdjDropParams = new AdjDropParams[]{
            //for 核心或者前台应用
            new AdjDropParams(ProcessInfo.PERCEPTIBLE_APP_ADJ,
                    new int[]{ProcessInfo.PERCEPTIBLE_APP_ADJ,
                            ProcessInfo.PERCEPTIBLE_APP_ADJ,
                            ProcessInfo.SERVICE_ADJ,
                            ProcessInfo.SERVICE_ADJ},
                    PERCEPTIBLE_PSS_LIMIT, // 内存限制：100M
                     PERCEPTIBLE_NUM_LIMIT),
            //for serviceA
            new AdjDropParams(ProcessInfo.SERVICE_ADJ,
                    new int[]{ProcessInfo.SERVICE_ADJ,
                            ProcessInfo.SERVICE_ADJ,
                            ProcessInfo.SERVICE_B_ADJ,
                            ProcessInfo.CACHED_APP_MIN_ADJ},
                    SERVICE_PSS_LIMIT, // 内存限制：100M
                     SERVICE_NUM_LIMIT),
            //for serviceB
            new AdjDropParams(ProcessInfo.SERVICE_B_ADJ,
                    new int[]{ProcessInfo.SERVICE_B_ADJ,
                            ProcessInfo.SERVICE_B_ADJ,
                            ProcessInfo.CACHED_APP_MIN_ADJ,
                            CACHED_APP_MID_ADJ},
                    SERVICEB_PSS_LIMIT, // 内存限制：200M
                     SERVICEB_NUM_LIMIT)
    };
```

其中 `AdjDropParams` 的第二个参数int数组及对应于不同内存情况的调节值

``` java
// framework/.../ProcessStats.java
    public static final int ADJ_MEM_FACTOR_NORMAL   = 0;
    public static final int ADJ_MEM_FACTOR_MODERATE = 1;
    public static final int ADJ_MEM_FACTOR_LOW      = 2;
    public static final int ADJ_MEM_FACTOR_CRITICAL = 3;

```

最终通过 `mPerformanceManager.applyOomAdjByProcessInfo(app, now, nowElapsed);` 写回`AMS`中
