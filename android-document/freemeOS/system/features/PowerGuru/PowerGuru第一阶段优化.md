## PowerGuru第一阶段优化

## 代码中问题修复

1.整编之后无法开机，是因为PowerGuruServices.java中

```
            /**
            * getIntent need android.permission.GET_INTENT_SENDER_INTENT
            * so clearCallingIdentity first
            */
            long ident = Binder.clearCallingIdentity();
            try {
                if (operation != null) {
                    packageName = operation.getTargetPackage();
                    Intent intent = operation.getIntent(); //原来这里有GET_INTENT_SENDER_INTENT 权限问题
                    if (intent != null) {
                        ComponentName cn = intent.getComponent();
                        if (cn != null) {
                            className = cn.getClassName();
                        }

                        action = intent.getAction();
                    }
                }

            } finally {
                Binder.restoreCallingIdentity(ident);
            }
```


## 算法优化建议

### 1.对于 **mPowerGuruSavedSchedules** 列表 优化建议

目前策略是关机不保存已经学习的列表。建议在代码中 在每次学习之后不同步保存到 mPowerGuruSavedSchedules列表中。但是关于mPowerGuruSavedSchedules的借口建议保存，已备后期有此类需求

```
                    if (!isSavedHeartbeatAlarm(alarm)) {
                        //keep the mPowerGuruSavedSchedules empty forever,
                        //mPowerGuruSavedSchedules.add(alarm);
```

### 2.对 **GMS APP** 处理的算法优化建议

对于来着GMS APP的alarm，我们设计的处理原则：

> 1. 如果是中国网络（大陆地区），只有 **VPN和数据链接**  **同时** 连接的时候，才把GMS APP 添加到后续的学习列表中，or 直接cancel 来着GMS 的alarm。 这样设计的好处在于：如果在中国区，客户手机如果有GMS应用，就可以生了很多学习的步骤。

> 2. 如果是国外网络，所有的GMS APP 走第三方 App的自学习流程

基于上述原则，目前问题：对中国网络情况下（没有 **VPN和数据链接**  **同时** 连接的时候），我们直接添加到心跳列表中了

```
    private boolean processGMSAlarm(AlarmInfoInternal alarm) {
        /**check if in china network */
        if (isChinaNetworkOperator() && !(mVpnNetworkConnected && mDataNetworkConnected)) {
            Tracer.v(TAG, "in china network, and vpn and wifi are both not connected!! do not applay this GMS alarm");
            alarm.available = false;

            //add to heartbeat alarm ist
            //addToHeartbeatAlarmList(alarm, false, false);//why add to heartbeat list directly

            mGmsAppsAlarmAllAdjusted = true;
            return true;
        }

        mGmsAppsAlarmAllAdjusted = false;
        return false;
    }
```

### 3.对于isAlarmMatched 的改进建议

有两个isAlarmMatched 方法，isAlarmMatched()

```
    private boolean isAlarmMatched(AlarmInfoInternal a1, AlarmInfoInternal a2, boolean isRepeat) {
        if (a1 == null || a2 == null) return false;

        if (a1.packageName != null && a1.packageName.equals(a2.packageName) &&
                ((a1.action != null && a1.action.equals(a2.action)) || (a1.action == null && a2.action == null)) &&
                ((a1.className != null && a1.className.equals(a2.className)) || (a1.className == null && a2.className == null)) &&
            (a1.type == a2.type) &&  ( a1.repeatInterval == a2.repeatInterval) //添加type ， repeatInterva的判断
                ) { //check packageName && action && component && type

             //删除
            /*if (isRepeat) { //also check interval
                if ((a1.repeatInterval != 0) && (a2.repeatInterval != 0))
                    return true;
                else
                    return false;
            }*/

            return true;
        }

        return false;
    }



    private boolean isAlarmMatched(AlarmInfo a1, AlarmInfoInternal a2, boolean isRepeat) {
        if (a1 == null || a2 == null) return false;

        if (a1.packageName != null && a1.packageName.equals(a2.packageName) &&
                ((a1.action != null && a1.action.equals(a2.action)) || (a1.action == null && a2.action == null)) &&
                ((a1.className != null && a1.className.equals(a2.className)) || (a1.className == null && a2.className == null)) &&
            (a1.type == a2.type) // 添加type 判断
                ) { //check packageName && action && component && type

            //删除
            /*if (isRepeat) { //also check interval
                if (a2.repeatInterval != 0)
                    return true;
                else
                    return false;
            }*/

            return true;
        }
```


## LOG 优化建议

###1.PowerGuruHelper 添加Tracer log , powergure.log文件中。
