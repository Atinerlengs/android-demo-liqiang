[TOC]

# 低分辨率省电

---
功能说明： **根据需要自动降低分辨率，有助于省电**

```
    public boolean isLowResolutionSupported() {
        if ((SystemProperties.getInt("sys.aps.support", 0)) != 0) {
            return true;
        }
        return false;
    }
```

该功能开关是通过 sys.aps.support 属性来控制的

---

需要调研的三个方向：

1. 如何自动降低分辨率
2. 省电原理：降低分辨率具体是通过哪些细节来实现省电的（如：分辨率低，应用会模糊，减少绘制计算
3. 开关开启之后，影响的模块有哪些

## 如何自动降低分辨率

开关之后

```
    private OnCheckedChangeListener mLowResolutionSwitchCheckListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (PowerManagerFragment.this.mLowResolutionControlChecked != isChecked) {
                String[] strArr = new String[2];
                strArr[0] = HsmStatConst.PARAM_OP;
                strArr[1] = isChecked ? "1" : "0";
                HsmStat.statE((int) Events.E_POWER_ROG_SWITCH, HsmStatConst.constructJsonParams(strArr));
                SysCoreUtils.setLowResolutionSwitchState(PowerManagerFragment.this.mAppContext, isChecked);
                PowerManagerFragment.this.mLowResolutionControlChecked = isChecked;
                PowerManagerFragment.this.getAvaliableTime();
            }
        }
    };
```

通过HsmStat.statE ，深究进去最后是通过Proxy 通过bindle 往底层写数据
IHsmStatService.java

```
            public boolean eStat(String key, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeString(value);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }
```


## 省电原理

## 影响模块
SysCoreUtils.java中

```
    public static void setLowResolutionSwitchState(Context mContext, boolean state) {
        int i = 1;
        ContentResolver contentResolver = mContext.getContentResolver();
        String str = ApplicationConstant.LOW_RESOLUTION_SWITCH_STATUS;
        if (!state) {
            i = 0;
        }
        Global.putInt(contentResolver, str, i);
        try {
            Class<?> hwNSDImplClass = Class.forName("android.view.HwNsdImpl");
            Object hwNsdImplObject = hwNSDImplClass.getMethod("getDefault", new Class[0]).invoke(null, new Object[0]);
            hwNSDImplClass.getDeclaredMethod("setLowResolutionMode", new Class[]{Context.class, Boolean.TYPE}).invoke(hwNsdImplObject, new Object[]{mContext, Boolean.valueOf(state)});
            HwLog.i(TAG, "setLowResolutionSwitchState, state = " + state);
```

反射到HwNsdImpl.java中

```
    public void setLowResolutionMode(Context context, boolean enableLowResolutionMode) {
        int i = 0;
        Log.i("sdr", "APS: SDR: HwNsdImpl.setLowResolutionMod, enableLowResolutionMode = " + enableLowResolutionMode);
        String[] queryResultList = getQueryResultGameList(context, CONFIGTYPE_QUERYRESULTLIST);
        ActivityManager am = (ActivityManager) context.getSystemService(FreezeScreenScene.ACTIVITY_PARAM);
        int length;
        String packageName;
        if (enableLowResolutionMode) {
            String[] blackListCompatModeAppsArray = getCustAppList(context, CONFIGTYPE_BLACKLIST);
            ArrayList<String> compatModeBlackListApps = new ArrayList();
            for (String tmp : blackListCompatModeAppsArray) {
                compatModeBlackListApps.add(tmp);
            }
            length = queryResultList.length;
            while (i < length) {
                packageName = queryResultList[i];
                if (!compatModeBlackListApps.contains(packageName)) {
                    am.setPackageScreenCompatMode(packageName, 1);
                }
                i++;
            }
            return;
        }
        String[] whiteListCompatModeAppsArray = getCustAppList(context, CONFIGTYPE_WHITELIST);
        ArrayList<String> compatModeWhiteListApps = new ArrayList();
        for (String tmp2 : whiteListCompatModeAppsArray) {
            compatModeWhiteListApps.add(tmp2);
        }
        for (String packageName2 : queryResultList) {
            if (!compatModeWhiteListApps.contains(packageName2)) {
                am.setPackageScreenCompatMode(packageName2, 0);
            }
        }
    }
```

这里牵扯到==黑白名单==的问题

但是主要的是==setPackageScreenCompatMode==

ActivityManager 原生方法

设置屏幕兼容模式的，当屏幕发生变化的时候这个package的是否是兼容，是否需要更新ui，继续跟踪这个代码会发现，当设置熟悉之后，所有在监控列表中的应用都会被kill

```
    private void setPackageScreenCompatModeLocked(ApplicationInfo ai, int mode) {

            ....................
            this.mService.forceStopPackage(packageName, UserHandle.myUserId());
        }
    }
```

我们不用太过关注原生逻辑


从上述的分析来看，这个功能开关影响最直接的模块就是所有被监控的UI界面，当分辨率被降低的时候，该应用会被重新绘制，最直观的影响



## 负面影响

http://club.huawei.com/thread-11576293-1-1.html
如帖子上面说：

电池中低分辨率省电在某些软件中有显示不全的问题，目前发现qq的弹出公告等和微信的弹出菜有显示不全。

在花粉论坛里有很多这样的帖子，这种省电不是对所有的应用，是针对当前系统耗电应用，微信，qq。


问题是要糊，所有应用都糊不好吗！唯独微信是这样。边看网页。上qq，刷微信很正常吧。这楼切换，眼晴根本受不了！就像你看了一分钟杨幂，然后再看一分钟凤姐，再看一分钟芙蓉道理是一样的！

微信、京东等的选项菜单页面错位


## 综合评估


和这些视觉上的影响 vs 省电的效果 + 工作量的评估

这个省电功能，开发优先级降低
