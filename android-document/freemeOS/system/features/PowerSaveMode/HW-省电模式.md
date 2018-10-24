[TOC]

# 省电模式
---

功能说明：限制后台活动，关闭邮件自动同步和系统提示音，并减弱视觉效果

---


需要调研的方向：

1. 省电原理（简单流程，具体做了哪些操作）
2. 涉及的模块
3. side effect
4. 需要数据支持


## 省电原理

```
    private OnCheckedChangeListener mSaveModeCheckListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (PowerManagerFragment.this.mSaveModeChecked != isChecked) {
                String[] strArr = new String[2];
                strArr[0] = HsmStatConst.PARAM_OP;
                strArr[1] = isChecked ? "1" : "0";
                HsmStat.statE((int) Events.E_POWER_POWERMODE_SWITCH_STATUS, HsmStatConst.constructJsonParams(strArr));
                String statParam1;
                if (isChecked) {
                    PowerNotificationUtils.showPowerModeQuitNotification(PowerManagerFragment.this.mAppContext);
                    PowerModeControl.getInstance(PowerManagerFragment.this.mAppContext).changePowerMode(4);
                    statParam1 = HsmStatConst.constructJsonParams(HsmStatConst.PARAM_OP, "1");
                    HsmStat.statE((int) Events.E_POWER_POWERMODE_SELECT, statParam1);
                } else {
                    PowerModeControl.getInstance(PowerManagerFragment.this.mAppContext).changePowerMode(1);
                    PowerNotificationUtils.cancleLowBatterySaveModeNotification(PowerManagerFragment.this.mAppContext);
                    PowerNotificationUtils.canclePowerModeOpenNotification(PowerManagerFragment.this.mAppContext);
                    statParam1 = HsmStatConst.constructJsonParams(HsmStatConst.PARAM_OP, "0");
                    HsmStat.statE((int) Events.E_POWER_POWERMODE_SELECT, statParam1);
                }
                PowerManagerFragment.this.mSaveModeChecked = isChecked;
            }
        }
    };
```

### 三个方面解析
####  发送对应的通知状态

> PowerNotificationUtils.showPowerModeQuitNotification(PowerManagerFragment.this.mAppContext);

#### 改变当前模式

```
    private void handlePowerModeSwitch(int powerModeNum) {
        if (powerModeNum == 1) {//  关闭省电模式
            if (getPercentStatusEnterSaveMode()) {
                System.putInt(this.mContext.getContentResolver(), DB_BATTERY_PERCENT_SWITCH, 0);
            }
            setConnect(this.mContext, "normal_level");
            wirtePowerMode(1, 2);
            HwLog.i(TAG, "handlePowerModeSwitch to SmartMode, settings db SmartModeStatus= " + powerModeNum + " ,broadcast genieValue= " + 2);
        } else if (powerModeNum == 4) { // 开启省电模式
            recordBatteryPercentStatusForSaveMode();
            System.putInt(this.mContext.getContentResolver(), DB_BATTERY_PERCENT_SWITCH, 1);
            setConnect(this.mContext, "normal_level");
            wirtePowerMode(4, 1);
            HwLog.i(TAG, "handlePowerModeSwitch to SaveMode, settings db SmartModeStatus= " + powerModeNum + " ,broadcast genieValue= " + 1);
        }
    }
```

主要看wirtePowerMode(..., ...)方法，发广播

```
 CHANGE_MODE_ACTION="huawei.intent.action.POWER_MODE_CHANGED_ACTION";

    public void wirtePowerMode(int mSaveMode, int genieValue) {
        if (readSaveMode() == mSaveMode) {
            HwLog.i(TAG, "the current powerMode is same with change mode, do nothing.");
            return;
        }
        System.putIntForUser(this.mContext.getContentResolver(), ApplicationConstant.SMART_MODE_STATUS, mSaveMode, 0);
        Intent intent = new Intent(CHANGE_MODE_ACTION);
        intent.putExtra("state", genieValue);
        this.mContext.sendBroadcast(intent);
    }
```

## 涉及的模块

通过查看哪些模块接受了这个广播再继续追究到省电的细节方面

huawei.intent.action.POWER_MODE_CHANGED_ACTION

### 模块1：CPUPowerMode.java

```
                if (CPUPowerMode.ACTION_POWER_MODE_CHANGE.equals(action)) {
                    int powerMode = intent.getIntExtra("state", 0);
                    if (powerMode == 1) { // 省电模式
                        CPUPowerMode.this.mPowerMode = 2;
                        CPUPowerMode.this.mCPUFreqInteractive.notifyToChangeFreq(CPUFeature.MSG_RESET_FREQUENCY, CPUPowerMode.CHANGE_FREQUENCY_DELAYED, CPUPowerMode.this.mPowerMode);
                        CPUPowerMode.mIsSaveMode.set(true);
                    } else if (powerMode == 2) { //NO 
                        CPUPowerMode.this.mPowerMode = 1;
                        CPUPowerMode.this.mCPUFreqInteractive.notifyToChangeFreq(CPUFeature.MSG_SET_FREQUENCY, CPUPowerMode.CHANGE_FREQUENCY_DELAYED, CPUPowerMode.this.mPowerMode);
                        CPUPowerMode.mIsSaveMode.set(false);
                    }
                }
```

通知Cpu 改变频率和save mode

调频

```
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CPUFeature.MSG_SET_FREQUENCY /*112*/:
                    CPUFreqInteractive.this.setFrequency();
                    return;
                case CPUFeature.MSG_RESET_FREQUENCY /*113*/:
                    CPUFreqInteractive.this.resetFrequency();
                    return;
                default:
                    AwareLog.w(CPUFreqInteractive.TAG, "handleMessage default msg what = " + msg.what);
                    return;
            }


    private void setFrequency() {
        long time = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(CPUFeature.MSG_SET_FREQUENCY);
        int resCode = this.mCPUFeatureInstance.sendPacket(buffer);
        if (resCode != 1) {
            AwareLog.e(TAG, "setFrequency sendPacket failed, send error code:" + resCode);
        }
        CpuDumpRadar.getInstance().insertDumpInfo(time, "setFrequency()", "set cpu frequency", CpuDumpRadar.STATISTICS_CHG_FREQ_POLICY);
    }
```

两种方式（setfreq   + resetfreq）

```
    private void resetFrequency() {
        long time = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate(8);//申请堆空间缓存数据
        buffer.putInt(CPUFeature.MSG_RESET_FREQUENCY);
        buffer.putInt(this.mPowerMode); //mPowerMode = 2
        int resCode = this.mCPUFeatureInstance.sendPacket(buffer);
        if (resCode != 1) {
            AwareLog.e(TAG, "resetFrequency sendPacket failed, send error code:" + resCode);
        }
        CpuDumpRadar.getInstance().insertDumpInfo(time, "resetFrequency()", "reset cpu frequency", CpuDumpRadar.STATISTICS_RESET_FREQ_POLICY);
    }
```

通过socket ByteBuffer

CPUFeature.java

```
    public synchronized int sendPacket(ByteBuffer buffer) {
        if (buffer == null) {
            return -1;
        }
        int retry = 2;
        do {
            if (IAwaredConnection.getInstance().sendPacket(buffer.array(), 0, buffer.position())) {
                return 1;
            }
            retry--;
        } while (retry > 0);
        return -2;
    }
```

IAwaredConnection.java

```
    public synchronized boolean sendPacket(byte[] msg, int offset, int count) {
        if (msg != null && offset >= 0 && count > 0) {
            if (offset <= msg.length - count) {
                if (createImpl()) {
                    try {
                        this.outStream.write(msg, offset, count);
                        this.outStream.flush();
                        return true;
                    } catch (IOException e) {
                        AwareLog.e(TAG, "Failed to write output stream, IOException");
                        destroyImpl();
                        return false;
                    }
                }
                AwareLog.e(TAG, "Failed to create connection");
                return false;
            }
        }
        AwareLog.e(TAG, "Parameter check failed");
        return false;
    }
```

cpu 模块的影响主要是cpu的freq  修改，再往下就是看底层节点对写下的值处理的原则

### 模块2：PowerModeReciever.java

```
    public void onReceive(Context context, Intent intent) {
        int powerState = 2;
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("huawei.intent.action.POWER_MODE_CHANGED_ACTION")) {
                    powerState = intent.getIntExtra("state", 2);
                }
                if (powerState == 1) {
                    this.mSettingsPreferenceFragment.applyLowPowerMode(true);
                } else if (powerState == 2) {
                    this.mSettingsPreferenceFragment.applyLowPowerMode(false);
                }
            }
        }
    }
```

applyLowPowerMode 这个是mSettingsPreferenceFragment的方法，主要是改变设置开关的状态

NotificationAndStatusSettings.java

如  DisplaySettings.java(休眠时间：30s，亮度减低)

```
    public void applyLowPowerMode(boolean isLowPowerMode) {
        super.applyLowPowerMode(isLowPowerMode);
        if (isLowPowerMode) {
            this.mScreenTimeoutPreference.setEnabled(false);
        } else if (!this.mScreenTimeoutPreference.isDisabledByAdmin()) {
            this.mScreenTimeoutPreference.setEnabled(true);
        }
    }
```

### 模块3：MsgReciever.java

```
        } else if ("huawei.intent.action.POWER_MODE_CHANGED_ACTION".equals(action)) {
            evtId = 208;
```

直接调用到ThermalStateManager.java

```
    private void powerModeChanged(int newMode) {
        if (newMode < 1 || newMode > 4) {
            Log.e("HwThermalStateManager", "invalid new mode:" + newMode);
        } else if (this.mPowerMode != newMode) {
            Log.i("HwThermalStateManager", "power mode:" + this.mPowerMode + " -> " + newMode);
            if (newMode != 1) { //2
                notifyUsePowerSaveThermalPolicy(false);
            } else {//1
                notifyUsePowerSaveThermalPolicy(true);
            }
            this.mPowerMode = newMode;
        } else {
            Log.i("HwThermalStateManager", "power mode:" + this.mPowerMode + " same, do nothing ");
        }
    }


     private void handleThermalPolicyChange(boolean usePowerSaveThermalConf) {
        synchronized (this.mLock) {
            String str;
            String str2 = "HwThermalStateManager";
            if (usePowerSaveThermalConf) {
                str = "Switch to power save thermal configure";
            } else {
                str = "Switch to default thermal configure";
            }
            Log.w(str2, str);
            clearCurrentPolicy();
            clearCachedConf();
            this.mCameraClear = false;
            loadThermalConf(usePowerSaveThermalConf);
        }
    }
```

重新load 各种配置，不深究


至此可以看到主要影响如上三个方面


但是对邮件系统同步，系统提示音的，后台活动 在huawei代码中没找到
