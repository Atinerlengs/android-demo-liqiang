# Freeme IMEI 说明文档

### 问题描述
通过获取系统的IMEI号，做服务器的过滤升级（指定IMEI号才可升级），现获取的IMEI不正确

### 现象

- v9c/q5c 通过TelephonyManager 的getDeviceId接口，获取的为MEID号，而非IMEI号
- 通过暗码 ```*#06#``` 查看IMEI MEID 号，显示全网通三个号码
- 通过设置 - 关于手机 - 状态 - IMEI信息，只看到俩个信息（默认卡槽一 MEID 卡槽二 IMEI号）

### 相关模块
- Package Settings
- packages Dialer
- frameworks/opt/telephony/

### 分析流程

- 通过系统接口获取IMEI时，默认情况如下：

```java
// Demo Code
TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
String imsi = tm.getSubscriberId();
String imei = tm.getDeviceId();

mTextView.setText("imei = " + imei + "\n" + "imsi = " + imsi + " \n ");
// 结果 imei为 MEID号码

//而实际的在 TelephonyManager 中 getDeviceId() 实现如下
public String getDeviceId() {
  try {
    ITelephony telephony = getITelephony();
    if (telephony == null)
      return null;
    return telephony.getDeviceId(mContext.getOpPackageName());
  } catch (RemoteException ex) {
    return null;
  } catch (NullPointerException ex) {
    return null;
  }
}
@newApi 23
public String getDeviceId(int slotId) {
  // FIXME this assumes phoneId == slotId
  try {
    IPhoneSubInfo info = getSubscriberInfo();
    if (info == null)
      return null;
    return info.getDeviceIdForPhone(slotId);
  } catch (RemoteException ex) {
    return null;
  } catch (NullPointerException ex) {
    return null;
  }
}
```

- 此时如果该设备为双卡，默认的[getDeviceId()](https://developer.android.com/reference/android/telephony/TelephonyManager.html)如果不传参，则获取的是卡槽一的deviceid （在API 23中可以指定获取卡槽一或者卡槽二，从而获取默认的deviceID）

- 在mtk 平台中卡槽一在不插卡的情况下，默认显示为MEID 即CDMA phone id ，只有插入SIM卡后，才会动态的根据sim卡类型显示deviceID （移动卡、联通卡对应GSM 的IMEI号），如下：

![image](http://note.youdao.com/yws/res/139/WEBRESOURCE87b2f826a42bf1f86d510562d27a3480)
![image](http://note.youdao.com/yws/res/137/WEBRESOURCE8b2e4915d1a6db023c92a700c2d9f627)

对比通过暗码为何能读取三个deviceID ，查看代码如下：

```java
packages/apps/Dialer/src/com/android/dialer/SpecialCharSequenceMgr.java
private static final String[] RIL_IMEI_SIM = {
  "ril.imei.sim1",
  "ril.imei.sim2"
}

String imei = SystemProperties.get(RIL_IMEI_SIM[slot]);
deviceIds.add(TextUtils.isEmpty(imei) ? "imei_invalid" : imei);
if (icCdmaLteDcSupport()) {
  String meid = SystemProperties.get("ril.cdma.meid");
  deviceIds.add(TextUtils.isEmpty(meid) ? "meid_invalid" : meid);
}

// 而RIL_IMEI_SIM字符串数组的赋值在framework/opt/telephone中

private static final String[] RIL_IMEI_SIM = {
  "ril.imei.sim1",
  "ril.imei.sim2"
};

case EVENT_GET_IMEI_DONE:
ar = (AsyncResult)msg.obj;

if (ar.exception != null) {
  Rlog.d(LOG_TAG, "Null IMEI!!");
  setDeviceIdAbnormal(1);
  break;
}
mImei = (String)ar.result;
Rlog.d(LOG_TAG, "IMEI: " + mImei);
int index = mPhoneId;
if (index == 10) {
  index = 0;
} else if (index == 11) {
  index = 1;
}
SystemProperties.set(RIL_IMEI_SIM[index], mImei);
//*/
try {
  Long.parseLong(mImei);
  setDeviceIdAbnormal(0);
} catch (NumberFormatException e) {
  setDeviceIdAbnormal(1);
  Rlog.d(LOG_TAG, "Invalid format IMEI!!");
}
break;
```

分析代码发现，其通过一个字符串数组保存俩个卡槽自带的IMEI号，然后通过暗码显示（该方案由mtk提供，详情请见[C2K双卡项目通话界面输入“*#06#”显示MEID/IMEI的客制化](https://onlinesso.mediatek.com/Pages/FAQ.aspx?List=SW&FAQID=FAQ15598)）

### 总结

- **通过系统API获取IMEI号，受限于默认卡槽（卡槽一）的类型，而指定卡槽的新API(getDeviceId(int slotId) )需要API23以上才能使用，默认卡槽类型在MTK平台中不插卡时限定为CDMA**
- **建议将MEID号加入默认的后台服务器下载白名单**
