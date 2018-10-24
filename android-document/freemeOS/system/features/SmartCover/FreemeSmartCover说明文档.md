# FreemeSmartCover 说明文档

### 功能
- 开合皮套打开或关闭智能皮套显示界面
- 智能皮套现只支持一种模式（时间-灰色），后续可替换时间样式，或扩充模块（天气，音乐等），相关框架实现代码保留，后续如扩展可尝试添加。

### 相关模块

- Framework Sensor （查看当前霍尔器件状态）
- Framework PhoneWindowManager （处理霍尔键值并通知各模块）
- SystemUI/Keyguard （显示智能锁屏界面）
- Package InCallUi （显示智能锁屏-来电界面）
- Package FreemeDeskClock （显示智能锁屏-闹钟界面）

### 流程图
![image](http://note.youdao.com/yws/res/124/WEBRESOURCEa2fce8f8391044723805565b3956b2e9)

### 模块分析(```改动详情见文档末尾```)
**Framework Sensor**

```java
Modify File：
./frameworks/base/core/java/android/hardware/SensorManager.java
./frameworks/base/core/java/android/hardware/SystemSensorManager.java
./frameworks/base/core/jni/android_hardware_SensorManager.cpp
```

```java
/// 增加 isHallSensorOn() 函数，判断当前霍尔器件的状态，从而使Keyguard，Incallui，DeskCLock /// 应用prepare相应的显示界面
  class SensorManager  {
  public static boolean isHallSensorOn() {}
  }
```

**Framework PhoneWindowManager**

```java
Modify File：
frameworks/base/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java
Add File：
frameworks/base/policy/src/com/android/internal/policy/impl/DroiSmartCover.java
```

```java
///在PhoneWindowManager中做事件拦截，针对KEYCODE_ALT_LEFT 和 KEYCODE_ALT_RIGHT 发送广播
 private static final String INTENT_FOR_SMART_COVER_CLOSE  = "com.freeme.smartcover.CLOSE";
 private static final String INTENT_FOR_SMART_COVER_OPEN   = "com.freeme.smartcover.OPEN";

/// 在 DroiSmartCover 做广播分发
public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags){    
	switch (keyCode) {
    case KeyEvent.KEYCODE_ALT_LEFT:
        ///发送皮套打开广播；INTENT_FOR_SMART_COVER_OPEN
    case KeyEvent.KEYCODE_ALT_RIGHT:
        ///发送皮套关闭广播；INTENT_FOR_SMART_COVER_CLOSE
    }
}
```



**SystemUI/Keyguard**

将皮套模式加入锁屏模式中（即是一种特殊情况锁屏）

```java
     public static enum SecurityMode {
        Invalid, // NULL state
        None, // No security enabled
        Pattern, // Unlock by drawing a pattern.
        Password, // Unlock by entering an alphanumeric password
        PIN, // Strictly numeric password
        Biometric, // Unlock with a biometric key (e.g. finger print or face unlock)
        Account, // Unlock by entering an account's login and password.
        //SimPin, // Unlock by entering a sim pin.
        //SimPuk, // Unlock by entering a sim puk.
        SimPinPukMe1, // Unlock by entering a sim pin/puk/me for sim or gemini sim1.
        SimPinPukMe2, // Unlock by entering a sim pin/puk/me for sim or gemini sim2.
        SimPinPukMe3, // Unlock by entering a sim pin/puk/me for sim or gemini sim3.
        SimPinPukMe4, // Unlock by entering a sim pin/puk/me for sim or gemini sim4.
        AlarmBoot, // add for power-off alarm.
        Voice, // Unlock with voice password
        AntiTheft, // Antitheft feature
        //*/ freeme, chenming. 20160712, for tyd smart clover
        SmartClover // lock by smart clover
    }
```



当接收到 PWM 的``` CLOSE``` 广播后，根据以下条件展示皮套锁屏

- 判断是否当前为非来电状态
  - 来电状态（Ringing or Calling），则等待电话结束，显示SmartCover锁屏
  - 无来电，则进入锁屏状态
- 判断当前是否已为锁屏
  - 无锁屏，则直接dokeyguardlock 显示SmartCover锁屏界面
  - 已锁屏，则Reset当前锁屏状态 ，显示SmartCover锁屏界面

当接收到PhoneWindowManager的``` OPEN``` 广播后

- 如系统已设置图案、数字、复杂锁屏，则直接进入解锁界面
- 如系统设置滑动解锁，则直接进入桌面


```java
SystemUI
Modify File:	    
frameworks/base/packages/SystemUI/src/com/android/systemui/keyguard/KeyguardViewMediator.java
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarKeyguardViewManager.java
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/KeyguardBouncer.java

Keygguard
Modify File:
frameworks/base/packages/Keyguard/src/com/android/keyguard/KeyguardSecurityModel.java
frameworks/base/packages/Keyguard/src/com/android/keyguard/KeyguardSecurityContainer.java
frameworks/base/packages/Keyguard/src/com/android/keyguard/KeyguardHostView.java
Add File: 
frameworks/base/packages/Keyguard/res/   /// smartcover 相关资源
frameworks/base/packages/Keyguard/src/com/android/keyguard/smartcover /// smartcover界面 相关自定义View以及实现逻辑
```

**smartcover 现只支持一种模式（时间），后续可替换时间样式，同时扩充天气，音乐模块等，相关框架实现代码保留，后续如扩展，可尝试添加。**



**Package InCallUI** 

```java
private static final String INTENT_FOR_SMART_COVER_CLOSE  = "com.freeme.smartcover.CLOSE";
private static final String INTENT_FOR_SMART_COVER_OPEN   = "com.freeme.smartcover.OPEN";
```

- 正在打电话时，如接收到以上广播，则显示或退出皮套-电话界面
- 在来电响铃时，根据SensorManager.isHallSensorOn() 接口判断当前皮套状态，从而显示皮套-来电界面或皮套-电话界面 

**Package FreemeDeskClock**

```java
private static final String INTENT_FOR_SMART_COVER_CLOSE  = "com.freeme.smartcover.CLOSE";
private static final String INTENT_FOR_SMART_COVER_OPEN   = "com.freeme.smartcover.OPEN";
```

- 正在闹钟响铃时，如接收到以上广播，则显示或退出皮套-闹钟界面
- 触发闹钟时，根据SensorManager.isHallSensorOn() 接口判断当前皮套状态，从而显示皮套-闹钟界面
***



# 改动详情

**Framework Sensor**

./frameworks/base/core/java/android/hardware/SensorManager.java

```java
//*/ freeme.chenming, 20160809. for smartcover
/** @hide */
public static final int HALL_CLOSE = 0;
/** @hide */
public static final int HALL_FAR   = 1;
/** @hide */
public static boolean isHallSensorOn() {
    final int state = SystemSensorManager.nativeSensorHallState();
    return state == HALL_CLOSE;
}
//*/
```

./frameworks/base/core/java/android/hardware/SystemSensorManager.java

```java
//*/ freeme.chenming, 20160809. for smartcover
static native int nativeSensorHallState();
//*/
```

./frameworks/base/core/jni/android_hardware_SensorManager.cpp

```c++
//*/ freeme.chenming, 20160809. for smartcover
define THE_DEVICE "/sys/class/hall/state"
enum {
    HALL_CLOSE = 0,
    HALL_FAR   = 1,
};
static jint nativeHallSensorState(void) {
  	int nrd, fd;
  	char value[8];

  	fd = open(THE_DEVICE, O_RDONLY);
 	if (fd < 0) {
     	goto lbErr;
  	}
  	nrd = read(fd, value, sizeof(value));
  	close(fd);
  	if (nrd <= 0) {
      	goto lbErr;
  	}
  	return ((value[0] - '0') & HALL_FAR);
lbErr:
	ALOGE("Sensor Hall does not exist!");
	return HALL_FAR;
}
//*/
```



**Framework PhoneWindowManager**

frameworks/base/policy/src/com/android/internal/policy/impl/PhoneWindowManager.java

```java
//*/ freeme.chenming, 20160712, for tyd smart cover
private DroiSmartCover mDroiSmartCover;
//*/

public void init(Context context,
                 ...
//*/ freeme, chenming. 20160712, for tyd smart clover
if(mDroiSmartCover == null) {
  mDroiSmartCover = new DroiSmartCover(mContext);
}
//*/
                 ...
}

public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
  ...
//*/ freeme.chenming, 20160712, for tyd smart cover
if (FeatureOption.FREEME_SMARTCOVER_SUPPROT) {
   policyFlags = mDroiSmartCover.interceptKeyBeforeQueueing(event, policyFlags);
}
  ...
}
```

   frameworks/base/policy/src/com/android/internal/policy/impl/DroiSmartCover.java

```java
///核心。。。
 private static final String INTENT_FOR_SMART_COVER_CLOSE  = "com.freeme.smartcover.CLOSE";
 private static final String INTENT_FOR_SMART_COVER_OPEN   = "com.freeme.smartcover.OPEN";

public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags){    
	switch (keyCode) {
    case KeyEvent.KEYCODE_ALT_LEFT:
        ///发送皮套打开广播；
    case KeyEvent.KEYCODE_ALT_RIGHT:
        ///发送皮套关闭广播；
    }
```

**SystemUI/Keyguard**

**SystemUi**

```java
Modify File：frameworks/base/packages/SystemUI/src/com/android/systemui/keyguard/KeyguardViewMediator.java
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarKeyguardViewManager.java
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/KeyguardBouncer.java
```

frameworks/base/packages/SystemUI/src/com/android/systemui/keyguard/KeyguardViewMediator.java

```java
//*/ freeme, shanjibing. 20160712, for smart cover
private static final int RESET_FOR_SMART_COVER   = 3001;
private static final int CLOSE_FOR_SMART_COVER   = 3002;

private static final String INTENT_FOR_SMART_COVER_CLOSE  = "com.freeme.smartcover.CLOSE";
private static final String INTENT_FOR_SMART_COVER_RESET = "com.freeme.smartcover.RESET";

private void handleCloseForSmartCover() {
  TelephonyManager mTM = (TelephonyManager) 	mContext.getSystemService(Context.TELEPHONY_SERVICE);
  if (mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
    if (!isShowing()) {
      if (DEBUG) Log.i(TAG, "handleSmartCloverClose keyguard isnot Showing");
      doKeyguardLocked(null);
    } else { 
      mStatusBarKeyguardViewManager.showBouncerForHall(false);
    }
  }
}

private void handleResetForSmartCover(Bundle options) {
  synchronized (KeyguardViewMediator.this) {
    if (DEBUG) Log.d(TAG, "handleResetFromUser");
    TelephonyManager mTM = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    if (mTM.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
      mStatusBarKeyguardViewManager.showBouncerForHall(false);
    }
  }
}
//*/

public void onPhoneStateChanged(int phoneState) {
  synchronized (KeyguardViewMediator.this) {
    if ((TelephonyManager.CALL_STATE_IDLE == phoneState  // call ending
         && !mScreenOn                           // screen off
         && mExternallyEnabled) {                // not disabled by any app
      //*/ freeme, shanjibing. 20160712, for smart cover
      || (SensorManager.isHallSensorOn()
          && !isShowing()
          && TelephonyManager.CALL_STATE_IDLE == phoneState)
        //*/
        ) { 
        doKeyguardLocked(null);
      }
    }
        
 private void setupLocked() {
...
      //*/ freeme, shanjibing. 20160712, for smart cover
        filter.addAction(INTENT_FOR_SMART_COVER_CLOSE);
        filter.addAction(INTENT_FOR_SMART_COVER_RESET);
        //*/
...
 }
   private Handler mHandler = new Handler(Looper.myLooper(), null, true /*async*/) {

        /// M: Add for log message string
        private String getMessageString(Message message) {  
          ...
                //*/ freeme, shanjibing. 20160712, for smart clover
                case RESET_FOR_SMART_COVER:
                    return "RESET_FOR_SMART_COVER";
                case CLOSE_FOR_SMART_COVER:
                    return "CLOSE_FOR_SMART_COVER";
                //*/
          ...
        }
     
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                ...
                //*/ freeme, shanjibing. 20160712, for smart clover
                case RESET_FOR_SMART_COVER:
                    handleResetForSmartCover((Bundle) msg.obj);
                    break;
                case CLOSE_FOR_SMART_COVER:
                    handleCloseForSmartCover();
                    break;
                //*/
                ...
            }
        }

        
 private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
           @Override
        public void onReceive(Context context, Intent intent) {
        ...
            //*/ freeme, shanjibing. 20160712, for smart cover
            else if (INTENT_FOR_SMART_COVER_CLOSE.equals(action)) {
                Message msg = mHandler.obtainMessage(CLOSE_FOR_SMART_COVER);
                mHandler.sendMessage(msg);
            }
            else if (INTENT_FOR_SMART_COVER_RESET.equals(action)) {
                Message msg = mHandler.obtainMessage(RESET_FOR_SMART_COVER);
                mHandler.sendMessage(msg);
            }
            //*/
        ...
        }
```

frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/StatusBarKeyguardViewManager.java

```java
//*/ freeme, shanjibing. 20160712, for smart cover
public void showBouncerForHall(boolean authenticated) {
  if (mShowing) {
    mPhoneStatusBar.hideKeyguard();
    hideBouncer(true);
    mBouncer.show(true, authenticated);
  }
  updateStates();
}
```

frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/KeyguardBouncer.java

```java
public boolean needsFullscreenBouncer() {
  SecurityMode mode = mSecurityModel.getSecurityMode();
  return mode == SecurityMode.SimPinPukMe1
    || mode == SecurityMode.SimPinPukMe2
    || mode == SecurityMode.SimPinPukMe3
    || mode == SecurityMode.SimPinPukMe4
    || mode == SecurityMode.AntiTheft
    //*/ freeme, shanjibing. 20160712, for smart cover
    || mode == SecurityMode.SmartCover
    //*/
    || mode == SecurityMode.AlarmBoot;
}
public boolean isFullscreenBouncer() {
  if (mKeyguardView != null) {
    SecurityMode mode = mKeyguardView.getCurrentSecurityMode();
    return mode == SecurityMode.SimPinPukMe1
      || mode == SecurityMode.SimPinPukMe2
      || mode == SecurityMode.SimPinPukMe3
      || mode == SecurityMode.SimPinPukMe4
      || mode == SecurityMode.AntiTheft
      //*/ freeme, shanjibing. 20160712, for smart cover
      || mode == SecurityMode.SmartCover
      //*/
      || mode == SecurityMode.AlarmBoot;
  }

  return false ;
}
```

frameworks/base/packages/Keyguard/src/com/android/keyguard/KeyguardSecurityModel.java

```java
//*/ freeme, shanjibing. 20160712, for smart cover
import android.hardware.SensorManager;

public static enum SecurityMode {
  ...
    //*/ freeme, shanjibing. 20160712, for smart cover
    SmartClover, // lock by smart cover
  ...
}

public SecurityMode getSecurityMode() {
...
  //*/ freeme, shanjibing. 20160712, for smart clover
  if(SensorManager.isHallSensorOn() && !PowerOffAlarmManager.isAlarmBoot()){
    return SecurityMode.SmartClover;
  }
...
}
```

frameworks/base/packages/Keyguard/src/com/android/keyguard/KeyguardSecurityContainer.java

```java
private int getSecurityViewIdForMode(SecurityMode securityMode) {
  switch (securityMode) {
      ...
        //*/ freeme, shanjibing. 20160712, for smart cover
        case SmartClover: return R.id.smart_cover_keyguard_view;
      	//*/
      ...
  }
}
private int getLayoutIdFor(SecurityMode securityMode) {
  switch (securityMode) {
      ...
        //*/ freeme, shanjibing. 20160712, for smart cover
        case SmartClover: return R.layout.smart_cover_home_layout;
      	//*/
      ...
  }
}
```





