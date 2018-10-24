[TOC]


基于android8.1版本

## 修改记录

| 版本 | 修改日期 | 作者 | 修改内容 |
| :---| ----------| ---- | ---- |
| v1.0 | 2018.05.16 | 李秋月 | 初版 |
| v2.0 | 2018.06.10 | 李秋月 | 更新 |
| v3.0 | 2018.06.20 | 李秋月 | 更新 |

## 前言
在开始讲解蓝牙模块之前，先叨扰两句， **Android O1** 设置模块架构改变很大，蓝牙里面的各个 **preference** 的显示， **google** 都是通过类似 **BluetoothDeviceNamePreferenceController** 、 **BluetoothPairingPreferenceController** 等一系列 **Controller** 来统一控制。另外，之前的 **addPreference()** 方法依旧有效。

## 正文

蓝牙的界面入口是 **BluetoothSettings.java** 类，所加载的布局文件为 **bluetooth_settings.xml** 。

首先明确几个蓝牙概念：

- **1、蓝牙的开启和关闭**
- **2、蓝牙的重命名**
- **3、蓝牙的检测性和检测时间**
- **4、加载已经配对的蓝牙设备**
- **5、扫描附近可用的蓝牙设备**
- **6、与设备配对、连接、通信**

### 蓝牙布局实现

```
public class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable {
   ......此处省略代码

    @Override
    protected int getPreferenceScreenResId() {
        //主界面的xml文件
        return R.xml.bluetooth_settings;
    }

   ......此处省略代码

}
```

### 1、蓝牙的开启和关闭

首先，在 **BluetoothSettings.java** 中通过 **getSwitchBar()** 得到 **SwitchBar** 控件，再将控件传给 **SwitchBarController** ，然后将 **SwitchBarController** 开关控件传给 **BluetoothEnabler** 。

```
final SettingsActivity activity = (SettingsActivity) getActivity();
mSwitchBar = activity.getSwitchBar();

mBluetoothEnabler = new BluetoothEnabler(activity, new SwitchBarController(mSwitchBar),
                mMetricsFeatureProvider, Utils.getLocalBtManager(activity),
                MetricsEvent.ACTION_BLUETOOTH_TOGGLE);
```

进到 **BluetoothEnabler** 类里，在构造方法里，会通过 **getSwitch()** 得到 **SwitchBar** 控件。

```
public BluetoothEnabler(Context context, SwitchWidgetController switchWidget,MetricsFeatureProvider metricsFeatureProvider, LocalBluetoothManager manager,int metricsEvent, RestrictionUtils restrictionUtils) {
    mContext = context;
    mMetricsFeatureProvider = metricsFeatureProvider;
    mSwitchWidget = switchWidget;
    //得到SwitchBar控件
    mSwitch = mSwitchWidget.getSwitch();
    mSwitchWidget.setListener(this);
    mValidListener = false;
    mMetricsEvent = metricsEvent;

    ......
}
```

**SwitchBar** 控件是如何传递，关联是在哪里？其中 **SwitchBarController** 继承自 **SwitchWidgetController**, **SwitchBarController** 重写了父类的  **getSwitch()** 方法。

```
public class SwitchBarController extends SwitchWidgetController implements SwitchBar.OnSwitchChangeListener {
    ......此处省略代码

    public SwitchBarController(SwitchBar switchBar) {
        mSwitchBar = switchBar;
    }

    ......此处省略代码

    @Override
    //由BluetoothEnabler构造方法里SwitchWidgetController.getSwitch()调用此方法
    public Switch getSwitch() {
        return mSwitchBar.getSwitch();
    }

}
```

开关是如何控制蓝牙的打开和关闭的？下面我们还是进入到 **BluetoothEnabler** 类中查看。首先定义蓝牙状态改变的广播接接收器，在 **resume()** 里注册广播监听器，为开关设置监听器。当开关开启或者关闭，对本地蓝牙进行状态更新，在 **onSwitchToggled()** 方法里进行定义。

```
/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public final class BluetoothEnabler implements SwitchWidgetController.OnSwitchChangeListener {

......此处省略代码

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            //定义蓝牙状态改变的接收器
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.d(TAG, "BluetoothAdapter state changed to" + state);
            //根据不同的蓝牙状态，处理相应的事件
            handleStateChanged(state);
        }
    };

......此处省略代码

    public void resume(Context context) {
        if (mContext != context) {
            mContext = context;
        }

        final boolean restricted = maybeEnforceRestrictions();

        if (mLocalAdapter == null) {
            //如果蓝牙适配器为空，则将resume方法返回，不进行resume方法中的剩余操作 
            mSwitchWidget.setEnabled(false);
            return;
        }

        // Bluetooth state is not sticky, so set it manually
        if (!restricted) {
            //必须手动的去监听蓝牙状态的改变，根据本地蓝牙适配器获取到此时蓝牙的状态，对switch进行设置
            handleStateChanged(mLocalAdapter.getBluetoothState());
        }

        //为开关设置监听器
        mSwitchWidget.startListening();
        //注册蓝牙状态改变的广播
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mValidListener = true;
    }

......此处省略代码

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitchWidget.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                /// M: receive bt status changed broadcast, set mUpdateStatusOnly true @{
                mUpdateStatusOnly = true;
                Log.d(TAG, "Begin update status: set mUpdateStatusOnly to true");
                /// @}
                //当蓝牙状态打开时，设置开关状态为开
                setChecked(true);
                mSwitchWidget.setEnabled(true);
                /// M: after set the switch checked status, set mUpdateStatusOnly false @{
                mUpdateStatusOnly = false;
                Log.d(TAG, "End update status: set mUpdateStatusOnly to false");
                /// @}
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitchWidget.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                /// M: receive bt status changed broadcast, set mUpdateStatusOnly true @{
                mUpdateStatusOnly = true;
                Log.d(TAG, "Begin update status: set mUpdateStatusOnly to true");
                /// @}
                //当蓝牙状态关闭时，设置开关状态为关
                setChecked(false);
                mSwitchWidget.setEnabled(true);
                /// M: after set the switch checked status, set mUpdateStatusOnly false @{
                mUpdateStatusOnly = false;
                Log.d(TAG, "End update status: set mUpdateStatusOnly to false");
                /// @}
                break;
            default:
                setChecked(false);
                mSwitchWidget.setEnabled(true);
        }
    }

......此处省略代码

    private void setChecked(boolean isChecked) {
        //获取当前switch的状态
        final boolean currentState =
                (mSwitchWidget.getSwitch() != null) && mSwitchWidget.getSwitch().isChecked();
        //如果当前的按钮状态和传进来的状态不一致，则设置为isChecked
        if (isChecked != currentState) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitchWidget.stopListening();
            }
            mSwitchWidget.setChecked(isChecked);
            if (mValidListener) {
                mSwitchWidget.startListening();
            }
        }
    }

......此处省略代码

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (maybeEnforceRestrictions()) {
            return true;
        }
        Log.d(TAG, "onSwitchChanged to " + isChecked);
        // Show toast message if Bluetooth is not allowed in airplane mode
        if (isChecked &&
                !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            mSwitch.setChecked(false);
            return false;
        }

        mMetricsFeatureProvider.action(mContext, mMetricsEvent, isChecked);

        Log.d(TAG, "mUpdateStatusOnly is " + mUpdateStatusOnly);
        /// M: if receive bt status changed broadcast, do not need enable/disable bt.
        if (mLocalAdapter != null && !mUpdateStatusOnly) {
            //当switch开关状态发生改变时，对系统本地蓝牙状态进行设置，当对本地蓝牙
            //设置成功时，返回true,否则返回false。
            boolean status = mLocalAdapter.setBluetoothEnabled(isChecked);
            // If we cannot toggle it ON then reset the UI assets:
            // a) The switch should be OFF but it should still be togglable (enabled = True)
            // b) The switch bar should have OFF text.
            //当本地蓝牙状态设置失败时，switch关闭且可用的状态，同时summary也要更新
            if (isChecked && !status) {
                mSwitch.setChecked(false);
                mSwitch.setEnabled(true);
                mSwitchWidget.updateTitle(false);
                return false;
            }
        }
        //当switch状态进行改变时，让其不可点击
        mSwitchWidget.setEnabled(false);
        return true;
    }

......此处省略代码

}
```

对本地蓝牙状态改变，调用 **LocalBluetoothAdapter** 的 **setBluetoothEnabled()** 方法，下面我们进到此方法里查看。实则是调用的 **BluetoothAdapter** 的 **enable()** 和 **disable()** 方法进行本地蓝牙的状态更新。

```
public class LocalBluetoothAdapter {

......此处省略代码

    public boolean setBluetoothEnabled(boolean enabled) {
        //根据switch的enable来开启或者关闭蓝牙，success返回执行结果
        boolean success = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (success) {
            //如果系统蓝牙开启或者关闭操作成功，将状态更新
            setBluetoothStateInt(enabled
                ? BluetoothAdapter.STATE_TURNING_ON
                : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
            if (Utils.V) {
                Log.v(TAG, "setBluetoothEnabled call, manager didn't return " +
                        "success for enabled: " + enabled);
            }

            //如果系统蓝牙没有开启或者关闭成功，则将蓝牙状态进行更新保存为当前系统蓝牙的状态
            syncBluetoothState();
        }
        return success;
    }

......此处省略代码

}
```

蓝牙的初始状态默认值对应的是 **def_bluetooth_on** ，通过蓝牙服务 **BluetoothManagerService** 保存起来。一系列的开启关闭方法也是在 **BluetoothManagerService** 类里，有兴趣可以自行查询。

### 2、蓝牙的重命名
在 **BluetoothDeviceRenamePreferenceController** 类，通过 **updateDeviceName( )** 方法更新 **preference** 的 **summary** ，在 **handlePreferenceTreeClick( )** 处理点击事件，
发送蓝牙名称改变的广播，与此同时，弹出一个 **dialog** ，给用户改名。
其中改蓝牙名称的广播接收器注册在它的父类 **BluetoothDeviceNamePreferenceController** 里。

```
public class BluetoothDeviceRenamePreferenceController extends
        BluetoothDeviceNamePreferenceController {
......此处省略代码

    @Override
    protected void updateDeviceName(final Preference preference, final String deviceName) {
        preference.setSummary(deviceName);
    }

......此处省略代码

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (PREF_KEY.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext,
                    MetricsProto.MetricsEvent.ACTION_BLUETOOTH_RENAME);
            LocalDeviceNameDialogFragment.newInstance()
                    .show(mFragment.getFragmentManager(), LocalDeviceNameDialogFragment.TAG);
            return true;
        }

        return false;
    }

......此处省略代码
}
```

其中， **updateDeviceName( )** 重写了父类 **BluetoothDeviceNamePreferenceController** 里的方法。

```
public class BluetoothDeviceNamePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {

......此处省略代码

    @Override
    public void updateState(Preference preference) {
        //由子类重写，来更新preference的蓝牙设备名称
        updateDeviceName(preference, mLocalAdapter.getName());
    }

......此处省略代码

    /**
     * Receiver that listens to {@link BluetoothAdapter#ACTION_LOCAL_NAME_CHANGED} and updates the
     * device name if possible
     */
    @VisibleForTesting
    //注册监听蓝牙名称改变的广播，从而实时更新蓝牙名称
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (TextUtils.equals(action, BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
                if (mPreference != null && mLocalAdapter != null && mLocalAdapter.isEnabled()) {
                    updateDeviceName(mPreference, mLocalAdapter.getName());
                }
            }
        }
    };

......此处省略代码

}
```

**updateState( )** 方法重写了父类 **AbstractPreferenceController** 方法。
这里着重说明一下， **AbstractPreferenceController** 类是所有 **controller** 的父类，很多 **preference** 的事件都是在这个抽象类里定义的。当 **preference** 的状态改变时，就可以调用 **updateState( )** 方法，里面涉及到系统的一系列的回调，这里就不展开讲解了。

```
/**
 * A controller that manages event for preference.
 */
 //所有Controller类的基类
public abstract class AbstractPreferenceController {

......此处省略代码

    /**
   * Updates the current status of preference (summary, switch state, etc)
   */
   public void updateState(Preference preference) {

  }

......此处省略代码

}
```

那么打开了一个对话框，蓝牙名称改变具体是怎么实现的呢？首先从用户看到到的对话框开始说起，这个对话框的类是 **LocalDeviceNameDialogFragment.java** 我们进去里看一下。发现，拿到 **setDeviceName( )** 里的蓝牙设备名称参数，再调用 **LocalBluetoothAdapter** 类的 **setName( )** 方法重新设置的蓝牙的名称。

```
/** Provides a dialog for changing the advertised name of the local bluetooth adapter. */
public class LocalDeviceNameDialogFragment extends BluetoothNameDialogFragment {

......此处省略代码

    @Override
    protected void setDeviceName(String deviceName) {
        mLocalAdapter.setName(deviceName);
    }

......此处省略代码

}
```

其中， **setDeviceName( )** 方法是重写了父类 **BluetoothNameDialogFragment** 里的方法，这个父类做的事情就有意思了，监听 **EditText** 的编辑行为，实时更新蓝牙的名称，调用 **setDeviceName( )** 方法，把字符串传给此方法的参数，给 **LocalDeviceNameDialogFragment** 类里更改蓝牙名称的参数使用。下面我们进到父类里看一下。

```
/**
 * Dialog fragment for renaming a Bluetooth device.
 */
abstract class BluetoothNameDialogFragment extends InstrumentedDialogFragment
        implements TextWatcher {

......此处省略代码

    /**
     * Set the device to the given name.
     * @param deviceName the name to use
     */
    abstract protected void setDeviceName(String deviceName);

......此处省略代码

    private View createDialogView(String deviceName) {
        final LayoutInflater layoutInflater = (LayoutInflater)getActivity()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //蓝牙重命名对话框的布局
        View view = layoutInflater.inflate(R.layout.dialog_edittext, null);
        mDeviceNameView = (EditText) view.findViewById(R.id.edittext);
        mDeviceNameView.setFilters(new InputFilter[] {
                new Utf8ByteLengthFilter(BLUETOOTH_NAME_MAX_LENGTH_BYTES)
        });
        //一打开对话框，显示的是初始的蓝牙名称
        mDeviceNameView.setText(deviceName);    // set initial value before adding listener
        if (!TextUtils.isEmpty(deviceName)) {
            //编辑框中光标的位置移到末尾
            mDeviceNameView.setSelection(deviceName.length());
        }
        mDeviceNameView.addTextChangedListener(this);
        //实时监听用户的输入
        mDeviceNameView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //把编辑框中的名称作为参数传给setDeviceName()方法中
                    setDeviceName(v.getText().toString());
                    mAlertDialog.dismiss();
                    return true;    // action handled
                } else {
                    return false;   // not handled
                }
            }
        });
        return view;
    }

......此处省略代码

}
```

### 3、蓝牙的检测性和检测时间

这个功能的实现，依赖于 **BluetoothAdapter** 和 **BluetoothDiscoverableEnabler** 类，下面讲解的属性都将在这2个类中进行定义，首先明确几个蓝牙检测性的概念：

- **SCAN_MODE_NONE:**
int值，大小为20，无法扫描其它设备，且不能被其它设备检测到


- **SCAN_MODE_CONNECTABLE:**     int值，大小为21，能够扫描其它设备，且只能被已配对的设备扫描到


- **SCAN_MODE_CONNECTABLE_DISCOVERABLE:**  int值，大小为23，能够扫描其它设备，且能被其它设备扫描到


上述扫描模式，搭配这个 **ACTION_SCAN_MODE_CHANGED**  **“action”** 使用。

- **DISCOVERABLE_TIMEOUT_TWO_MINUTES** = 120
- **DISCOVERABLE_TIMEOUT_FIVE_MINUTES** = 300
- **DISCOVERABLE_TIMEOUT_ONE_HOUR** = 3600
- **DISCOVERABLE_TIMEOUT_NEVER** = 0

系统定义了2min、5min、1h和永不设置时间这四种可选时间。进到 **BluetoothDiscoverableEnabler** 类中定义了 **setDiscoverableTimeout( )**
用来设置被其它设备扫描的时间。首先在 **resume()** 里注册广播监听本地蓝牙扫描模式的改变。

```
final class BluetoothDiscoverableEnabler implements Preference.OnPreferenceClickListener {

......此处省略代码

    // Bluetooth advanced settings screen was replaced with action bar items.
    // Use the same preference key for discoverable timeout as the old ListPreference.
    private static final String KEY_DISCOVERABLE_TIMEOUT = "bt_discoverable_timeout";

    private static final String VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES = "twomin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES = "fivemin";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR = "onehour";
    private static final String VALUE_DISCOVERABLE_TIMEOUT_NEVER = "never";

    //注册蓝牙扫描模式改变的广播
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                        BluetoothAdapter.ERROR);
                if (mode != BluetoothAdapter.ERROR) {
                    //进行蓝牙模式改变的处理
                    handleModeChanged(mode);
                }
            }
        }
    };

......此处省略代码

    public void resume(Context context) {

    ...

        //注册蓝牙扫描模式改变的广播
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

    ...

    }

......此处省略代码

    //设置蓝牙扫描时间
    void setDiscoverableTimeout(int index) {
        String timeoutValue;
        switch (index) {
            case 0:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_TWO_MINUTES;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES;
                break;

            case 1:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
                break;

            case 2:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_ONE_HOUR;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR;
                break;

            case 3:
                mTimeoutSecs = DISCOVERABLE_TIMEOUT_NEVER;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_NEVER;
                break;

            default:
                mTimeoutSecs = DEFAULT_DISCOVERABLE_TIMEOUT;
                timeoutValue = VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES;
                break;
        }

    }
    //将蓝牙扫描的时间写到SharedPreferences里面
    mSharedPreferences.edit().putString(KEY_DISCOVERABLE_TIMEOUT, timeoutValue).apply();
    setEnabled(true);   // enable discovery and reset timer

......此处省略代码

}
```

值得一提的是，源码中 **setDiscoverableTimeout()** 方法的定义有些小bug，我这里已经是修正版，跟源码会有些出入，但基本逻辑思想没有变。

这里通过 **"KEY_DISCOVERABLE_TIMEOUT"** **key** 值，也就是 **“bt_discoverable_timeout”** 存储到 **SharedPreferences** 里。用的时候，再通过 **getDiscoverableTimeout()** 方法获取，下面我们进到这个方法中看一下。

```
private int getDiscoverableTimeout() {
        if (mTimeoutSecs != -1) {
            return mTimeoutSecs;
        }

        int timeout = SystemProperties.getInt(SYSTEM_PROPERTY_DISCOVERABLE_TIMEOUT, -1);
        if (timeout < 0) {
            String timeoutValue = mSharedPreferences.getString(KEY_DISCOVERABLE_TIMEOUT,
                    VALUE_DISCOVERABLE_TIMEOUT_TWO_MINUTES);

            if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_NEVER)) {
                timeout = DISCOVERABLE_TIMEOUT_NEVER;
            } else if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_ONE_HOUR)) {
                timeout = DISCOVERABLE_TIMEOUT_ONE_HOUR;
            } else if (timeoutValue.equals(VALUE_DISCOVERABLE_TIMEOUT_FIVE_MINUTES)) {
                timeout = DISCOVERABLE_TIMEOUT_FIVE_MINUTES;
            } else {
                timeout = DISCOVERABLE_TIMEOUT_TWO_MINUTES;
            }
        }
        mTimeoutSecs = timeout;
        return timeout;
    }
```

在 **getDiscoverableTimeout()** 中，不难发现，就是通过之前 **setDiscoverableTimeout()** 方法中存储进去的 **key** 值，然后取出来的值经过 **case** 判断一下，返回检测时间值。

检测性是怎么设定的呢？在 **BluetoothSettings.java** 的 **updateContent()** 中， 当蓝牙开启时，会传入一个 **switchPreference** 给 **BluetoothDiscoverableEnabler** 的构造方法中。

```
public class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable {

......此处省略代码

    mDiscoverableEnabler = new BluetoothDiscoverableEnabler(mLocalAdapter,
                                mDiscoverablePreference);

......此处省略代码

}
```

具体怎么实现蓝牙检测性的开启和关闭，下面我们具体进入到 **BluetoothDiscoverableEnabler** 类中一探究竟。在 **BluetoothDiscoverableEnabler** 构造方法中将刚刚传入的 **preference** 赋值给 **mDiscoverablePreference** 成员变量。且对 **mDiscoverablePreference** 进行监听。实际操作是在 **setEnabled()** 方法中。当为 **true** 时，通过本地蓝牙 **mLocalAdapter** 设置扫描模式为 **BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE** ，当为 **fasle** 时，设置蓝牙的扫描模式为 **BluetoothAdapter.SCAN_MODE_CONNECTABLE** 。

```
final class BluetoothDiscoverableEnabler implements Preference.OnPreferenceClickListener {

······此处省略代码

    BluetoothDiscoverableEnabler(LocalBluetoothAdapter adapter,
            Preference discoveryPreference) {
        mUiHandler = new Handler();
        mLocalAdapter = adapter;
        //将 discoveryPreference赋值给成员变量 mDiscoveryPreference
        mDiscoveryPreference = discoveryPreference;
        mSharedPreferences = discoveryPreference.getSharedPreferences();
        discoveryPreference.setPersistent(false);
    }

······此处省略代码

    //对 mDiscoveryPreference实行监听
    mDiscoveryPreference.setOnPreferenceClickListener(this);

······此处省略代码

    public boolean onPreferenceClick(Preference preference) {
        // toggle discoverability
        mDiscoverable = !mDiscoverable;
        //对蓝牙可检测性改变真正的处理在setEnabled()方法里
        setEnabled(mDiscoverable);
        return true;
    }

······此处省略代码

    private void setEnabled(boolean enable) {
        if (enable) {
            int timeout = getDiscoverableTimeout();
            long endTimestamp = System.currentTimeMillis() + timeout * 1000L;
            LocalBluetoothPreferences.persistDiscoverableEndTimestamp(mContext, endTimestamp);

            //当为true时，设置蓝牙扫描模式为BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeout);
            updateCountdownSummary();

            Log.d(TAG, "setEnabled(): enabled = " + enable + "timeout = " + timeout);

            if (timeout > 0) {
                BluetoothDiscoverableTimeoutReceiver.setDiscoverableAlarm(mContext, endTimestamp);
            } else {
                BluetoothDiscoverableTimeoutReceiver.cancelDiscoverableAlarm(mContext);
            }

        } else {
            //当为false时，设置蓝牙的扫描模式为BluetoothAdapter.SCAN_MODE_CONNECTABLE
            mLocalAdapter.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE);
            BluetoothDiscoverableTimeoutReceiver.cancelDiscoverableAlarm(mContext);
        }
    }

······此处省略代码
}
```


### 4、加载已经配对的蓝牙设备

当本地蓝牙打开的时候，由 **LocalBluetoothAdapter** 调用 **LocalBluetoothProfileManager** 里的 **setBluetoothStateOn()** 方法。在此方法中，会调用 **BluetoothEventManager** 的 **readPairedDevices()** 方法，复制已配对设备列表，并更新到屏幕上。

```
/**
 * LocalBluetoothProfileManager provides access to the LocalBluetoothProfile
 * objects for the available Bluetooth profiles.
 */
public class LocalBluetoothProfileManager {

......此处省略代码

    //当本地蓝牙打开的时候由 LocalBluetoothAdapter 调用此方法
    void setBluetoothStateOn() {
        ParcelUuid[] uuids = mLocalAdapter.getUuids();
        if (uuids != null) {
            updateLocalProfiles(uuids);
        }
        //复制已配对设备列表，并更新到屏幕上
        mEventManager.readPairedDevices();
    }

......此处省略代码

}
```

接下来我们进到 **BluetoothEventManager** 类里具体看一下 **readPairedDevices()** 方法，在此方法中，调用底层代码获取可用设备列表并进行缓存。

```
/**
 * BluetoothEventManager receives broadcasts and callbacks from the Bluetooth
 * API and dispatches the event on the UI thread to the right class in the
 * Settings.
 */
public class BluetoothEventManager {

......此处省略代码

    boolean readPairedDevices() {
        //mLocalAdapter 是将 BluetoothAdapter 映射到本地，其内部代码不再书写，获取到已配对设备
        Set<BluetoothDevice> bondedDevices = mLocalAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return false;
        }
        boolean deviceAdded = false;
        for (BluetoothDevice device : bondedDevices) {
            //这一步调用的是设备缓存列表的管理类 CachedBluetoothDeviceManager 中的方法 findDevice()
            //用于检查缓存列表中是否已经存在该 device ，若存在就将 device 返回，若不存在就返回 null
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                //如果缓存列表中没有该设备就调用管理类 CachedBluetoothDeviceManager 中的 addDevice()
                //将设备添加到缓存列表中
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                //调用 BluetoothCallback 接口的 onDeviceAdded() 方法把设备添加进去
                dispatchDeviceAdded(cachedDevice);
                deviceAdded = true;
            }
        }
        return deviceAdded;
    }

......此处省略代码

    //调用 BluetoothCallback 接口的 onDeviceAdded() 方法把设备添加进去
    void dispatchDeviceAdded(CachedBluetoothDevice cachedDevice) {
        synchronized (mCallbacks) {
            for (BluetoothCallback callback : mCallbacks) {
                callback.onDeviceAdded(cachedDevice);
            }
        }
    }

......此处省略代码

}
```

**readPairedDevices()**  ：该方法在两个地方调用：

- 1、当本地蓝牙 **BluetoothAdapter** 开启后调用
- 2、就是当远程设备 **BluetoothDevice** 的状态发生改变时调用。


1、下面我们进到 **LocalBluetoothAdapter** 类里查看当蓝牙开启时， **readPairedDevices()** 的相关调用。

```
public class LocalBluetoothAdapter {

    public synchronized int getBluetoothState() {
        //总是更新 state，以防止在 pause 的时候改变状态
        syncBluetoothState();
        return mState;
    }

......此处省略代码

    //当蓝牙状态改变时，返回 true，否则返回 false
    //且当蓝牙打开时，更新一下已配对设备
    boolean syncBluetoothState() {
        int currentState = mAdapter.getState();
        if (currentState != mState) {
            //当蓝牙打开时，更新一下已配对设备
            setBluetoothStateInt(mAdapter.getState());
            return true;
        }
        return false;
    }

......此处省略代码

    //判断当前蓝牙是否打开，打开时，更新一下已配对设备
    synchronized void setBluetoothStateInt(int state) {
        mState = state;
        if (state == BluetoothAdapter.STATE_ON) {
            // if mProfileManager hasn't been constructed yet, it will
            // get the adapter UUIDs in its constructor when it is.
            if (mProfileManager != null) {
                //当蓝牙打开时，调用 LocalBluetoothProfileManager 里的
                // setBluetoothStateOn() 方法，复制已配对设备列表，并更新到屏幕上
                //主要是由 setBluetoothStateOn() 方法里调用 readPairedDevices()
                mProfileManager.setBluetoothStateOn();
            }
        }
    }

......此处省略代码

    //设置蓝牙的开关状态，都在 LocalBluetoothAdapter 定义，前面已经叙述
    public boolean setBluetoothEnabled(boolean enabled) {
        boolean success = enabled
                ? mAdapter.enable()
                : mAdapter.disable();

        if (success) {
            //判断当前蓝牙是否打开，打开时，更新一下已配对设备
            setBluetoothStateInt(enabled
                ? BluetoothAdapter.STATE_TURNING_ON
                : BluetoothAdapter.STATE_TURNING_OFF);
        } else {
            if (Utils.V) {
                Log.v(TAG, "setBluetoothEnabled call, manager didn't return " +
                        "success for enabled: " + enabled);
            }
            //更新一下蓝牙状态，再重新更新一下已配对设备
            syncBluetoothState();
        }
        return success;
    }

......此处省略代码

}
```

2、当远程设备发生改变时会发送 **ACTION_BOND_STATE_CHANGED** 的广播，在注册的 **handler** 中调用 **readPairedDevices()** 方法读取配对设备。监听广播的代码在 **BluetoothEventManager.java** 中。

```
public class BluetoothEventManager {

......此处省略代码

    //定义 mHandlerMap map集合
    private final Map<String, Handler> mHandlerMap;

......此处省略代码

    //构造方法
    BluetoothEventManager(LocalBluetoothAdapter adapter,
            CachedBluetoothDeviceManager deviceManager, Context context) {
        mLocalAdapter = adapter;
        mDeviceManager = deviceManager;
        mAdapterIntentFilter = new IntentFilter();
        mProfileIntentFilter = new IntentFilter();
        mHandlerMap = new HashMap<String, Handler>();

        ...

        // 加入“正在配对”的 action
        addHandler(BluetoothDevice.ACTION_BOND_STATE_CHANGED, new BondStateChangedHandler());

        ...

        mContext.registerReceiver(mBroadcastReceiver, mAdapterIntentFilter, null, mReceiverHandler);
    }

......此处省略代码

    private void addHandler(String action, Handler handler) {
        mHandlerMap.put(action, handler);
        mAdapterIntentFilter.addAction(action);
    }

......此处省略代码

    // 定义广播接收器，接收所有广播，过滤出想要的 action
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            //通过 mHandlerMap 里的 action 得到具体的 Handler
            //再通过泛型转化为父类 Handler
            Handler handler = mHandlerMap.get(action);
            //当 handler 不为空时，调用 onReceive() 方法，传参
            if (handler != null) {
                handler.onReceive(context, intent, device);
            }
        }
    };

......此处省略代码

    //定义的 Handler 接口
    interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice device);
    }

......此处省略代码

    //接收蓝牙配对状态改变的广播，做相应的处理
    private class BondStateChangedHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            if (device == null) {
                Log.e(TAG, "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                               BluetoothDevice.ERROR);
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w(TAG, "CachedBluetoothDevice for device " + device +
                        " not found, calling readPairedDevices().");
                //调用 readPairedDevices() 方法，添加蓝牙设备到已配对列表
                //成功添加，返回 true；否则，返回 false
                if (readPairedDevices()) {
                    cachedDevice = mDeviceManager.findDevice(device);
                }

                if (cachedDevice == null) {
                    Log.w(TAG, "Got bonding state changed for " + device +
                            ", but we have no record of that device.");

                    cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);ss
                    dispatchDeviceAdded(cachedDevice);
                }
            }

            synchronized (mCallbacks) {
                for (BluetoothCallback callback : mCallbacks) {
                    callback.onDeviceBondStateChanged(cachedDevice, bondState);
                }
            }
            cachedDevice.onBondingStateChanged(bondState);

            if (bondState == BluetoothDevice.BOND_NONE) {
                int reason = intent.getIntExtra(BluetoothDevice.EXTRA_REASON,
                        BluetoothDevice.ERROR);

                showUnbondMessage(context, cachedDevice.getName(), reason);
            }
        }

......此处省略代码

}
```

在 **BluetoothSettings** 类中，通过方法 **addDeviceCategory()** 方法加载已配对列表，此方法继承自父类 **DeviceListPreferenceFragment** ， **mPairedDevicesCategory** 变量用来存放已配对设备的容器 **CategoryPreference** 。

```
public class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable, Preference.OnPreferenceClickListener {

    //加载已配对列表
    addDeviceCategory(mPairedDevicesCategory,
                        R.string.bluetooth_preference_paired_devices,
                        BluetoothDeviceFilter.BONDED_DEVICE_FILTER, true);

}
```

进到 **DeviceListPreferenceFragment** 父类中查看 **addDeviceCategory()** 方法。

```
public abstract class DeviceListPreferenceFragment extends
        RestrictedDashboardFragment implements BluetoothCallback {

......此处省略代码

    public void addDeviceCategory(PreferenceGroup preferenceGroup, int titleId,
            BluetoothDeviceFilter.Filter filter, boolean addCachedDevices) {
        //SettingsPreferenceFragment父类中的方法
        cacheRemoveAllPrefs(preferenceGroup);
        //为preferenceCategory设置标题
        preferenceGroup.setTitle(titleId);
        //将preferenceGroup赋值给成员变量mDeviceListGroup，后面会在里面add设备
        mDeviceListGroup = preferenceGroup;
        //设置过滤器，赋值给 mFilter
        setFilter(filter);
        if (addCachedDevices) {
            addCachedDevices();
        }
        preferenceGroup.setEnabled(true);
        removeCachedPrefs(preferenceGroup);
    }

......此处省略代码

    final void setFilter(BluetoothDeviceFilter.Filter filter) {
        mFilter = filter;
    }

......此处省略代码

    void addCachedDevices() {
        //用于获取到缓存列表的复制
        Collection<CachedBluetoothDevice> cachedDevices =
                mLocalManager.getCachedDeviceManager().getCachedDevicesCopy();
        for (CachedBluetoothDevice cachedDevice : cachedDevices) {
            //添加已配对设备
            onDeviceAdded(cachedDevice);
        }
    }

......此处省略代码

    @Override
    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        Log.d(TAG, "onDeviceAdded, Device name is " + cachedDevice.getName());
        //假如列表里不存在当前设备，则添加。
        if (mDevicePreferenceMap.get(cachedDevice) != null) {
            Log.d(TAG, "Device name " + cachedDevice.getName() + " already have preference");
            return;
        }

        // Prevent updates while the list shows one of the state messages
        if (mLocalAdapter.getBluetoothState() != BluetoothAdapter.STATE_ON) return;

        //调用 BluetoothDeviceFilter类里的 match方法进行过滤
        if (mFilter.matches(cachedDevice.getDevice())) {
            Log.d(TAG, "Device name " + cachedDevice.getName() + " create new preference");
            //过滤之后，将已配对设备添加到列表中
            createDevicePreference(cachedDevice);
        }
    }

......此处省略代码

    //真正添加设备的动作在此方法中
    void createDevicePreference(CachedBluetoothDevice cachedDevice) {
        if (mDeviceListGroup == null) {
            Log.w(TAG, "Trying to create a device preference before the list group/category "
                    + "exists!");
            return;
        }

        String key = cachedDevice.getDevice().getAddress();
        BluetoothDevicePreference preference = (BluetoothDevicePreference) getCachedPreference(key);

        if (preference == null) {
            preference = new BluetoothDevicePreference(getPrefContext(), cachedDevice, this);
            preference.setKey(key);
            //往传进来的 preferenceCategory里添加设备，即之前被赋值的成员变量 mDeviceListGroup
            mDeviceListGroup.addPreference(preference);
        } else {
            // Tell the preference it is being re-used in case there is new info in the
            // cached device.
            preference.rebind();
        }

        //在子类中重写此方法，为每个 preference项添加点击监听事件
        initDevicePreference(preference);
        //将 CachedBluetoothDevice设备作为 key值，BluetoothDevicePreference作为value值，存储在弱引用的 mDevicePreferenceMap成员变量。
        //它本质是 WeakHashMap，后面增加设备，删除设备，都会用到这个变量。
        mDevicePreferenceMap.put(cachedDevice, preference);
    }

......此处省略代码

}
```

**SettingsPreferenceFragment** 类主要是进行远程蓝牙设备的移除操作。对preferenceGroup整体的管理，诸如preference的增删改查操作。由 **DeviceListPreferenceFragment** 类的 **addDeviceCategory** 的方法调用 **cacheRemoveAllPrefs()** 方法。

```
public abstract class SettingsPreferenceFragment extends InstrumentedPreferenceFragment
        implements DialogCreatable {

......此处省略代码

    //在 DeviceListPreferenceFragment类的 addDeviceCategory方法中调用此方法
    //主要是把设备存进  mPreferenceCache成员变量里，以便于后面对这个变量操作，进行删除设备的操作
    protected void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<String, Preference>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
            }
        }
    }

......此处省略代码

    //对 mPreferenceCache成员变量进行遍历，从而删除设备
    protected void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

......此处省略代码

}
```

 关于 **matches()** 方法可以查看 **BluetoothDeviceFilter** 文件，位于 **SettingsLib** 下，不同的过滤器对应于不同的内部类，这些内部类实现了内部接口的  **matches()** 方法，对 **BluetoothDevice** 的配对状态进行匹配，比如，过滤已经配对的蓝牙设备过滤器对应的内部类如下。

```
public final class BluetoothDeviceFilter {

    /** Filter that matches only bonded devices. */
    private static final class BondedDeviceFilter implements Filter {
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() == BluetoothDevice.BOND_BONDED;
        }
    }

}
```



**CachedBluetoothDeviceManager** 类主要是对远程蓝牙设备的管理，进行添加，移除。

```
/**
 * CachedBluetoothDeviceManager manages the set of remote Bluetooth devices.
 */
public class CachedBluetoothDeviceManager {

    public synchronized Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        //对成员变量 mCachedDevices <CachedBluetoothDevice> 进行复制
        return new ArrayList<CachedBluetoothDevice>(mCachedDevices);
    }

......此处省略代码

    public CachedBluetoothDevice addDevice(LocalBluetoothAdapter adapter,
            LocalBluetoothProfileManager profileManager,
            BluetoothDevice device) {
        CachedBluetoothDevice newDevice = new CachedBluetoothDevice(mContext, adapter,
            profileManager, device);
        synchronized (mCachedDevices) {
            //成员变量 mCachedDevices由这里进行操作添加设备
            mCachedDevices.add(newDevice);
            mBtManager.getEventManager().dispatchDeviceAdded(newDevice);
        }
        return newDevice;
    }

}
```

这里讲解一下 **CachedBluetoothDeviceManager** 类中的 **addDevice()** 方法。构造一个 **CachedBluetoothDevice** 对象 **newDevice** ，将此对象添加到 **mCachedDevices** 列表里，且返回这个新的 **newDevice** 对象。


### 5、扫描附近可用的蓝牙设备

附近可用设备和已配对设备加载原理相同，其中 **mAvailableDevicesCategory** 表示附近可用设备。

其中：

- 已配对设备设置的过滤器为 **BluetoothDeviceFilter.BONDED_DEVICE_FILTER**

- 附近可用设备设置的过滤器为 **BluetoothDeviceFilter.UNBONEDE_DEVICE_FILTER**


```
public class BluetoothSettings extends DeviceListPreferenceFragment implements Indexable, Preference.OnPreferenceClickListener {

    //加载附近可用蓝牙设备列表
    addDeviceCategory(mAvailableDevicesCategory,
                        R.string.bluetooth_preference_found_devices,
                        BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER, mInitialScanStarted);

}
```

引用的是 **BluetoothDeviceFilter** 中的内部类 **UnbondedDeviceFilter** 实现 **Filter** 接口。

```
public final class BluetoothDeviceFilter {


    /** Filter that matches only unbonded devices. */
    //匹配未连接的蓝牙设备
    private static final class UnbondedDeviceFilter implements Filter {
        public boolean matches(BluetoothDevice device) {
            return device.getBondState() != BluetoothDevice.BOND_BONDED;
        }
    }

}
```


### 6、与设备配对、连接、通信

首先蓝牙的事件处理会在 **BluetoothEvenManager** 类里面，在该类的构造方法里注册了许多 **action** ，用来监听蓝牙的相关变化。下面我们简单说一下有哪些广播：

- BluetoothAdpater.ACTION_STATE_CHANGED ：本机蓝牙状态发生了改变
- BluetoothAdpater.ACTION_DISCOVERY_STARTED：开始扫描
- BluetoothAdpater.ACTION_DISCOVERY_FINISHED：扫描结束
- BluetoothDevice.ACTION_FOUND：发现远程蓝牙设备
- BluetoothDevice.ACTION_DISAPPEARED：远程设备消失
- BluetoothDevice.ACTION_NAME_CHANGED：远程设备蓝牙名称改变
- BluetoothDevice.ACTION_BOND_STATE_CHANGED：远程设备连接状态改变
- BluetoothDevice.ACTION_PAIRING_CANCLE：远程设备取消配对
- BluetoothDevice.ACTION_CLASS_CHANGED：远程设备的蓝牙类已经改变
- BluetoothDevice.ACTION_UUID：
- BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED：远程设备的电量改变

#### a、扫描附近可用设备

在扫描开始之前，会先判断蓝牙是否开启。如果开启，则判断是否正在扫描，如果正在扫描，则不做处理。如果正在播放音乐，也不做处理，除非强制开启扫描。下面我们进到 **LocalBluetoothAdapter** 类里查看。

```
public class LocalBluetoothAdapter {

......此处省略代码

    public void startScanning(boolean force) {
        // Only start if we're not already scanning
        if (!mAdapter.isDiscovering()) {
            if (!force) {
                // Don't scan more than frequently than SCAN_EXPIRATION_MS,
                // unless forced
                //除非强制，否则设置扫描的时间间隔不超过 5min
                if (mLastScan + SCAN_EXPIRATION_MS > System.currentTimeMillis()) {
                    return;
                }

                // If we are playing music, don't scan unless forced.
                //根据蓝牙协议，除了强制，否则播放音乐的时候不扫描
                A2dpProfile a2dp = mProfileManager.getA2dpProfile();
                if (a2dp != null && a2dp.isA2dpPlaying()) {
                    return;
                }
                A2dpSinkProfile a2dpSink = mProfileManager.getA2dpSinkProfile();
                if ((a2dpSink != null) && (a2dpSink.isA2dpPlaying())){
                    return;
                }
            }

            if (mAdapter.startDiscovery()) {
                mLastScan = System.currentTimeMillis();
            }
        }
    }

......此处省略代码

}
```

在扫描开始的时候，会发送广播；扫描结束的时候，也会发送广播。由 **BluetoothEvenManager** 里的 **Handler** 处理。

不同的是：

- 开始扫描时， **mStarted** 为 **true**
- 扫描结束时， **mStarted** 为 **false**

```
public class BluetoothEventManager {

......此处省略代码

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;
        //开始扫描时传入的为true
        ScanningStateChangedHandler(boolean started) {
            mStarted = started;
        }
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            synchronized (mCallbacks) {
                for (BluetoothCallback callback : mCallbacks) {
                    //调用DeviceListPreferenceFragment.java中的方法显示扫描指示progress
                    callback.onScanningStateChanged(mStarted);
                }
            }
            //首先更新缓存列表，然后对显示列表进行排序更新显示。
            //排序规则代码在CachedBluetoothDevice.java中
            mDeviceManager.onScanningStateChanged(mStarted);
        }
    }

......此处省略代码

}

```

在搜索过程中发现设备会发送广播，程序会在广播处理代码中对缓存列表以及显示列表进行更新。

```
public class BluetoothEventManager {

......此处省略代码

    private class DeviceFoundHandler implements Handler {
        public void onReceive(Context context, Intent intent,
                BluetoothDevice device) {
            //获取到蓝牙的信号强度，默认为Short类型的最小值-2的15次方
            short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            //获取到远程设备的类型
            BluetoothClass btClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            //获取到远程设备的name
            String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            // TODO Pick up UUID. They should be available for 2.1 devices.
            // Skip for now, there's a bluez problem and we are not getting uuids even for 2.1.
            //获取到远程设备后检测是否在缓存列表中，若有就返回设备，若没有返回 null
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                //将设备添加到缓存列表中
                cachedDevice = mDeviceManager.addDevice(mLocalAdapter, mProfileManager, device);
                Log.d(TAG, "DeviceFoundHandler created new CachedBluetoothDevice: "
                        + cachedDevice);
            }
            cachedDevice.setRssi(rssi);
            cachedDevice.setBtClass(btClass);
            cachedDevice.setNewName(name);
            cachedDevice.setJustDiscovered(true);
        }
    }

......此处省略代码

}
```

#### b、蓝牙设备配对

设备列表中包括已配对设备、未配对设备、已连接设备等，当点击 **preference** 时会首先判断处于哪个状态，然后去进行下一个状态。如果没有配对，就进行配对

配对程序如下：在进行配对时首先检查远程设备是否正在配对，如果是，就返回true；如果没有在配对,就先将本机的蓝牙配对状态设为 **true**，表示正在配对，紧接着停止蓝牙的扫描操作，与远程设备进行配对，配对成功后进行自动连接

因为是点击 **preference** 进行配对连接，所以点击事件写在 **BluetoothDevicePreference** 里的 **onClicked()** 里。

```
public final class BluetoothDevicePreference extends GearPreference implements
        CachedBluetoothDevice.Callback {

......此处省略代码

    void onClicked() {
        Context context = getContext();
        int bondState = mCachedDevice.getBondState();

        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(context).getMetricsFeatureProvider();

        //判断当前的状态如果已连接，则断开连接
        if (mCachedDevice.isConnected()) {
            metricsFeatureProvider.action(context,
                    MetricsEvent.ACTION_SETTINGS_BLUETOOTH_DISCONNECT);
            Log.d(TAG, mCachedDevice.getName() + " askDisconnect");
            askDisconnect();
        //如果已配对，则连接
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            metricsFeatureProvider.action(context,
                    MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT);
            Log.d(TAG, mCachedDevice.getName() + " connect");
            mCachedDevice.connect(true);
        //如果未配对，则配对
        } else if (bondState == BluetoothDevice.BOND_NONE) {
            metricsFeatureProvider.action(context,
                    MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR);
            Log.d(TAG, mCachedDevice.getName() + " pair");
            if (!mCachedDevice.hasHumanReadableName()) {
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR_DEVICES_WITHOUT_NAMES);
            }
            pair();
        }
    }

......此处省略代码

    private void pair() {
        //调用 CachedBluetoothDevice 里的 startPairing 方法，配对成功返回 true
        if (!mCachedDevice.startPairing()) {
            Utils.showError(getContext(), mCachedDevice.getName(),
                    R.string.bluetooth_pairing_error_message);
        }
    }

......此处省略代码

}
```

配对之前进行判断，是否进行扫描，如果是，则停止扫描。通过底层服务进行配对，成功返回 **true**。

```
public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {

......此处省略代码

    public boolean startPairing() {
        // Pairing is unreliable while scanning, so cancel discovery
        //如果正在扫描，则停止扫描
        if (mLocalAdapter.isDiscovering()) {
            mLocalAdapter.cancelDiscovery();
        }

        //配对成功返回 true
        if (!mDevice.createBond()) {
            return false;
        }

        return true;
    }

......此处省略代码

}
```

接下来我们进到 **BluetoothDevice**  的 **createBond** 方法里查看，通过底层服务进行配对，成功返回 **true**。

```
public final class BluetoothDevice implements Parcelable {

......此处省略代码

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADMIN)
    public boolean createBond() {
        final IBluetooth service = sService;
        if (service == null) {
            Log.e(TAG, "BT not enabled. Cannot create bond to Remote Device");
            return false;
        }
        try {
            Log.i(TAG, "createBond() for device " + getAddress()
                    + " called by pid: " + Process.myPid()
                    + " tid: " + Process.myTid());
            return service.createBond(this, TRANSPORT_AUTO);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

......此处省略代码

}
```

#### c、蓝牙设备连接

在进行连接之前，先判断是否配对了；如果没有，则返回先配对；如果配对了，则进行连接。下面我们进到 **CachedBluetoothDevice** 里的 **

```
public class CachedBluetoothDevice implements Comparable<CachedBluetoothDevice> {

......此处省略代码
    public void connect(boolean connectAllProfiles) {
        if (!ensurePaired()) {
            return;
        }

        mConnectAttempted = SystemClock.elapsedRealtime();
        connectWithoutResettingTimer(connectAllProfiles);
    }

......此处省略代码

    private void connectWithoutResettingTimer(boolean connectAllProfiles) {
        //本机蓝牙与远程设备通信的配置规范，如果没有配置文件则不能进行通信
        //配置规范指定所使用的蓝牙通信协议，用户界面格式等等
        if (mProfiles.isEmpty()) {
            Log.d(TAG, "No profiles. Maybe we will connect later");
            return;
        }

        // Reset the only-show-one-error-dialog tracking variable
        //当我们去连接多个设备发生错误时我们只想显示一个错误对话框
        mIsConnectingErrorPossible = true;

        int preferredProfiles = 0;
        for (LocalBluetoothProfile profile : mProfiles) {
            if (connectAllProfiles ? profile.isConnectable() : profile.isAutoConnectable()) {
                if (profile.isPreferred(mDevice)) {
                    ++preferredProfiles;
                    //连接设备
                    connectInt(profile);
                }
            }
        }
        if (DEBUG) Log.d(TAG, "Preferred profiles = " + preferredProfiles);

        if (preferredProfiles == 0) {
            connectAutoConnectableProfiles();
        }
    }

    //确保配对，如果没有配对，则进行配对
    private boolean ensurePaired() {
        if (getBondState() == BluetoothDevice.BOND_NONE) {
            startPairing();
            return false;
        } else {
            return true;
        }
    }

    //连接设备
    synchronized void connectInt(LocalBluetoothProfile profile) {
        if (!ensurePaired()) {
            return;
        }
        //底层去连接，连接成功返回 true
        if (profile.connect(mDevice)) {
            if (Utils.D) {
                Log.d(TAG, "Command sent successfully:CONNECT " + describe(profile));
            }
            return;
        }
        Log.i(TAG, "Failed to connect " + profile.toString() + " to " + mName);
    }

......此处省略代码

}
```

至此，蓝牙大部分业务逻辑介绍完毕。

### 7、蓝牙常用知识整理

#### a、常用方法

- 获取本地蓝牙适配器：BluetoothAdapter.getDefaultAdapter()；

- 开启蓝牙：BluetoothAdapter----enable()

- 关闭蓝牙：BluetoothAdapter----disable()

- 重命名蓝牙：BluetoothAdapter----setName()

- 获取蓝牙名称：BluetoothAdapter----getName()

- 开启可检测性：BluetoothAdapter----setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE，timeout) //当 timeout 设为 0 时表示永不超时

- 获取蓝牙状态：BluetoothAdapter----getState()

- 获取蓝牙所支持的uuid数组：BluetoothAdapter----getUuids()

- 获取已配对设备：BluetoothAdapter----getBoneDevices()

- 开启扫描：BluetoothAdapter----startDiscovery()

- 停止扫描：BluetoothAdapter----cancelDiscovery()

- 判断是否正在扫描：BluetoothAdapter----isDiscovery()

- 扫描低功耗BLE蓝牙设备：BluetoothAdapter----startLeScan(mLeScanCallBack)

- 停止对BLE设备的扫描：BluetoothAdapter----stopLeScan(mLeScanCallBack)

#### b、常用类

- **BluetoothSettings**
蓝牙界面的显示布局 fragment，只有布局相关，会对本机蓝牙的名字，可检测性进行实时更新，所有的点击事件的处理都在别处

- **DeviceListPreferenceFragment**
远程设备列表的显示的更新，包括已配对列表和附近可用设备列表

- **BluetoothDevicePreference** 列表中每个设备的 title、summary、icon的修改，包括设备的点击事件

- **CachedBluetoothDevice** 管理远程设备，配对、连接

- **LocalBluetoothManager** 提供了本地蓝牙 API 的简化接口。

- **LocalBluetoothAdapter** 提供了设置 app 和本地蓝牙 BluetoothAdapter 功能调用的接口，特别是 adapter 自身状态的改变。

- **BluetoothAdapter** 代表本地蓝牙适配器，能够开启设备扫描、请求已配对设备列表、实例化已知的 mac 地址、监听从其它设备发来的连接请求，扫描 LE 设备。其中各种 action 、蓝牙模式以及其它有关的其它的蓝牙行为都定义在此类中。

- **BluetoothDevice** 代表远程蓝牙设备，创建各种设备的连接以及名字、地址和连接的状态的请求。

- **BluetoothEvenManager**
对设备的状态进行监听并处理，在该类的构造方法中注册了许多的监听器，监听蓝牙相关的变化，比如蓝牙状态改变 ACTION_STATE_CHANGED 等等。程序中已经为这些广播注册了监听器，当接收到广播后作出相应动作，对列表就行修改。首先是对缓存列表进行更改，然后再对显示列表进行更改。

