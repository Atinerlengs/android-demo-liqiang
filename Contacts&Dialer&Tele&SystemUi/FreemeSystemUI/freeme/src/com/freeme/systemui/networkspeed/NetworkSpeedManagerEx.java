package com.freeme.systemui.networkspeed;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.R;
import com.freeme.systemui.utils.NumberLocationPercent;
import com.freeme.systemui.statusbar.FreemeStatusbarStateToolKit;
import com.freeme.systemui.utils.SystemUIThread;

import java.math.BigDecimal;
import java.util.Locale;

public class NetworkSpeedManagerEx {

    private static final String TAG = NetworkSpeedManagerEx.class.getSimpleName();

    private static final boolean DEBUG = false;

    public interface Callback {
        void updateSpeed(String str);

        void updateVisibility(boolean z);
    }

    private Callback mCallback;
    private Context mContext;
    private long mCurrentValue = 0;

    private Handler mHandler;
    private boolean mIsAirplaneMode;
    private boolean mIsDataSwitchEnabled;
    private boolean mIsFirst = true;
    private boolean mIsHasIccCard;
    private boolean mIsNetworkSpeedEnabled;
    private boolean mIsNetworkValid;
    private boolean mIsRegister = false;
    private boolean mIsStop = true;
    private boolean mIsWifiConnected;
    private long mLastValue = 0;
    private INetworkManagementService mNetworkManager = null;
    private TelephonyManager mTelephonyManager;
    private BroadcastReceiver mReceiver;

    private Runnable mRunnable = new Runnable() {
        public void run() {
            if (DEBUG) {
                Log.i(TAG, "mIsStop = " + mIsStop);
            }
            if (!mIsStop) {
                new AsyncTask<Void, Void, Long>() {
                    protected Long doInBackground(Void... params) {
                        if (DEBUG) {
                            Log.d(TAG, "doInBackground");
                        }
                        return Long.valueOf(getTetherStats());
                    }

                    protected void onPostExecute(Long result) {
                        if (DEBUG) {
                            Log.d(TAG, "onPostExecute");
                        }
                        refreshSpeed(result.longValue());
                        mHandler.postDelayed(mRunnable, 3000);
                    }
                }.execute(new Void[0]);
            }
        }
    };

    private ContentObserver mDataSwitchObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            mIsDataSwitchEnabled = isDataSwitchEnabled(mContext);
            initStateValue_CheckShowAndUpdate();

            if (DEBUG) {
                Log.i(TAG, "MOBILE_DATA is changed, mIsDataSwitchEnabled:" + mIsDataSwitchEnabled);
            }
        }
    };

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            initStateValue_CheckShowAndUpdate();

            if (DEBUG) {
                Log.i(TAG, "onChange: airplane_mode_on, oldState=" + mIsAirplaneMode);
            }
        }
    };

    private ContentObserver mSwitchObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            mIsNetworkSpeedEnabled = isShowNetworkSpeedEnabled(mContext);
            initStateValue_CheckShowAndUpdate();

            if (DEBUG) {
                Log.i(TAG, " KEY_SHOW_NETWORK_SPEED_ENABLED is changed, mIsNetworkSpeedEnabled:" + mIsNetworkSpeedEnabled);
            }
        }
    };

    //*/ TODO: SystemUIThread need init in SystemUI Application, now just init here, refactor later
    static {
        SystemUIThread.init();
    }
    //*/

    public void init(Context context, Callback callback) {
        mContext = context;
        mCallback = callback;
        if (mContext != null && callback != null) {
            mTelephonyManager = TelephonyManager.from(context);
            mNetworkManager = Stub.asInterface(ServiceManager.getService("network_management"));
            if (mNetworkManager == null) {
                Log.e(TAG, "mNetworkManager = null");
                return;
            }
            mHandler = new Handler();
            createBroadcastReceiver();
            registerBroadcast();
            initStateValue_CheckShowAndUpdate();
        }
    }

    private void initStateValue() {
        mIsNetworkSpeedEnabled = isShowNetworkSpeedEnabled(mContext);
        mIsHasIccCard = com.freeme.systemui.utils.SimCardMethod.hasIccCard(mTelephonyManager, mContext);
        mIsNetworkValid = isNetworkAvailable(mContext);
        mIsAirplaneMode = Global.getInt(mContext.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) == 1;
        mIsDataSwitchEnabled = isDataSwitchEnabled(mContext);
        mIsWifiConnected = isWifiConnected();

        if (DEBUG) {
            Log.i(TAG, "mIsNetworkSpeedEnabled = " + mIsNetworkSpeedEnabled +
                    ", mIsAirplaneMode = " + mIsAirplaneMode +
                    ", mIsWifiConnected = " + mIsWifiConnected +
                    ", mIsHasIccCard = " + mIsHasIccCard +
                    ", mIsNetworkValid = " + mIsNetworkValid +
                    ", mIsDataSwitchEnabled = " + mIsDataSwitchEnabled);
        }
    }

    private void initStateValue_CheckShowAndUpdate() {
        SystemUIThread.runAsync(new SystemUIThread.SimpleAsyncTask() {
            public boolean runInThread() {
                initStateValue();
                return true;
            }

            public void runInUI() {
                checkShowAndUpdate();
            }
        });
    }

    public boolean isShowNetworkSpeedEnabled(Context context) {
        try {
            return 1 == System.getIntForUser(context.getContentResolver(),
                    FreemeStatusbarStateToolKit.SHOW_NETWORK_SPEED_SWITCH, ActivityManager.getCurrentUser());
        } catch (SettingNotFoundException e) {
            setShowNetworkSpeedDisabled(context);
            return false;
        }
    }

    private static void setShowNetworkSpeedDisabled(Context context) {
        System.putIntForUser(context.getContentResolver(),
                FreemeStatusbarStateToolKit.SHOW_NETWORK_SPEED_SWITCH, 0, ActivityManager.getCurrentUser());
    }

    private void registerBroadcast() {
        if (!mIsRegister) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_USER_SWITCHED);
            mContext.registerReceiver(mReceiver, filter);

            mContext.getContentResolver().registerContentObserver(System.getUriFor(FreemeStatusbarStateToolKit.SHOW_NETWORK_SPEED_SWITCH), true, mSwitchObserver, -1);
            mContext.getContentResolver().registerContentObserver(Global.getUriFor(Global.MOBILE_DATA), true, mDataSwitchObserver, -1);
            mContext.getContentResolver().registerContentObserver(Global.getUriFor(Global.AIRPLANE_MODE_ON), true, mAirplaneModeObserver);

            mIsRegister = true;
        }
    }

    public void unRegister() {
        if (mIsRegister) {
            mIsRegister = false;
            if (mReceiver != null) {
                mContext.unregisterReceiver(mReceiver);
            }
            mContext.getContentResolver().unregisterContentObserver(mSwitchObserver);
            mContext.getContentResolver().unregisterContentObserver(mDataSwitchObserver);
            mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
        }
    }

    private void createBroadcastReceiver() {
        mReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, final Intent intent) {
                if (intent != null && intent.getAction() != null) {
                    SystemUIThread.runAsync(new SystemUIThread.SimpleAsyncTask() {
                        public boolean runInThread() {
                            String action = intent.getAction();
                            Log.i(TAG, "receive:" + action);
                            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                                mIsDataSwitchEnabled = isDataSwitchEnabled(mContext);
                            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                                mIsNetworkValid = isNetworkAvailable(intent) || isNetworkAvailable(mContext);
                                mIsDataSwitchEnabled = isDataSwitchEnabled(context);
                            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                                mIsWifiConnected = isWifiConnected();
                            } else if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                                mIsNetworkSpeedEnabled = isShowNetworkSpeedEnabled(mContext);
                            }
                            return true;
                        }

                        public void runInUI() {
                            checkShowAndUpdate();
                        }
                    });
                }
            }
        };
    }

    private void start() {
        if (mCallback != null) {
            mCallback.updateVisibility(true);
        }
        if (mIsStop) {
            mIsStop = false;
            if (mHandler != null) {
                mHandler.removeCallbacks(mRunnable);
                mHandler.post(mRunnable);
            }
        }
    }

    private void stop() {
        mIsStop = true;
        mIsFirst = true;
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        if (mCallback != null) {
            mCallback.updateVisibility(false);
        }
        mLastValue = 0;
    }

    private void refreshSpeed(long currentValue) {
        mCurrentValue = currentValue;
        float value = Float.parseFloat(String.valueOf(Math.abs(mCurrentValue - mLastValue))) / 3.0f;
        if (DEBUG) {
            Log.i(TAG, " value = " + value);
        }
        if (value >= 0.0f) {
            mLastValue = mCurrentValue;
            if (mIsFirst) {
                mIsFirst = false;
                value = 0.0f;
            }
            String textValue = mContext.getResources().getString(R.string.speed, new Object[]{formatFileSize(mContext, value)});
            if (DEBUG) {
                Log.i(TAG, "speed = " + textValue);
            }
            if (mCallback != null) {
                mCallback.updateSpeed(textValue);
            } else {
                Log.i(TAG, "null == mCallback , error !!!");
            }
        }
    }

    private long getTetherStats() {
        try {
            long loTotalSize = TrafficStats.getRxBytes("lo") + TrafficStats.getTxBytes("lo");
            long statsSize = (TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes()) - loTotalSize;

            if (DEBUG) {
                Log.i(TAG, "getTetherStats statsSize:" + statsSize + ", LOSize=" + loTotalSize);
            }
            return statsSize;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return 0;
        } catch (Exception e2) {
            e2.printStackTrace();
            return 0;
        }
    }

    private boolean isDataSwitchEnabled(Context context) {
        try {
            return TelephonyManager.from(context).getDataEnabled();
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isWifiConnected() {
        boolean z = false;
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return false;
        }
        boolean enabled = wifiManager.isWifiEnabled();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
        if (enabled && ipAddress != 0) {
            z = true;
        }
        return z;
    }

    private void checkShowAndUpdate() {
        if (DEBUG) {
            Log.i(TAG, "mIsNetworkSpeedEnabled = " + mIsNetworkSpeedEnabled +
                    ", mIsAirplaneMode = " + mIsAirplaneMode +
                    ", mIsWifiConnected = " + mIsWifiConnected +
                    ", mIsHasIccCard = " + mIsHasIccCard +
                    ", mIsNetworkValid = " + mIsNetworkValid +
                    ", mIsDataSwitchEnabled = " + mIsDataSwitchEnabled);
        }

        if (!mIsNetworkSpeedEnabled) {
            stop();
        } else if (!mIsNetworkValid) {
            stop();
        } else if (!mIsWifiConnected && mIsAirplaneMode) {
            stop();
        } else if (!mIsWifiConnected && !mIsHasIccCard) {
            stop();
        } else if (mIsWifiConnected || mIsDataSwitchEnabled) {
            start();
        } else {
            stop();
        }
    }


    private static final float SIZE_START = 900.0f;
    private static final float SIZE_DIVISOR = 1024.0f;

    private String formatFileSize(Context context, float number) {
        if (context == null) {
            return "";
        }
        String value;
        int newScale = 0;
        float result = number;
        int suffix = R.string.kilobyteShort;
        if (number <= SIZE_START && number > 0.0f) {
            suffix = R.string.byteShort;
        }
        if (number > SIZE_START) {
            suffix = R.string.kilobyteShort;
            result = number / SIZE_DIVISOR;
            newScale = 1;
        }
        if (result > SIZE_START) {
            suffix = R.string.megabyteShort;
            result /= SIZE_DIVISOR;
        }
        if (result > SIZE_START) {
            suffix = R.string.gigabyteShort;
            result /= SIZE_DIVISOR;
        }
        if (result > SIZE_START) {
            suffix = R.string.terabyteShort;
            result /= SIZE_DIVISOR;
        }
        if (result > SIZE_START) {
            suffix = R.string.petabyteShort;
            result /= SIZE_DIVISOR;
        }
        Locale locale = mContext.getResources().getConfiguration().locale;
        if (result == 0.0f) {
            value = getLocaleFormatString(0, locale);
        } else {
            try {
                float temp = new BigDecimal((double) result).setScale(newScale, 4).floatValue();
                if (temp % 1.0f == 0.0f) {
                    value = getLocaleFormatString((int) temp, locale);
                } else {
                    value = getLocaleFormatString(temp, locale);
                }
            } catch (Exception e) {
                e.printStackTrace();
                value = getLocaleFormatString(0, locale);
            }
        }
        return context.getResources().getString(R.string.fileSizeSuffix, new Object[]{value, context.getString(suffix)});
    }

    private String getLocaleFormatString(int value, Locale locale) {
        if (locale != null) {
            return NumberLocationPercent.getFormatnumberString(value, locale);
        }
        Log.w(TAG, "int::getLocaleFormatString::locale is null!");
        return NumberLocationPercent.getFormatnumberString(value);
    }

    private String getLocaleFormatString(float value, Locale locale) {
        if (locale != null) {
            return NumberLocationPercent.getFormatnumberString(value, locale);
        }
        Log.w(TAG, "float::getLocaleFormatString::locale is null!");
        return NumberLocationPercent.getFormatnumberString(value);
    }

    private static boolean isNetworkAvailable(Intent intent) {
        Parcelable netInfo = intent.getParcelableExtra("networkInfo");
        if (netInfo == null || !(netInfo instanceof NetworkInfo)) {
            Log.w(TAG, "isNetworkAvailable::netInfo or object type is not correct!");
            return false;
        }
        boolean isConnected = ((NetworkInfo) netInfo).isConnected();
        if (isConnected) {
            Log.i(TAG, "isNetworkAvailable::netInfo isConnected!");
        }
        return isConnected;
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            Log.w(TAG, "isNetworkAvailable::connectivity is null!");
            return false;
        }
        NetworkInfo[] info = connectivity.getAllNetworkInfo();
        if (info == null) {
            Log.w(TAG, "isNetworkAvailable::netInfo list is null!");
            return false;
        }
        int i = 0;
        while (i < info.length) {
            if (info[i] == null || !info[i].isConnected()) {
                i++;
            } else {
                Log.i(TAG, "isNetworkAvailable::info[i] isConnected!");
                return true;
            }
        }
        return false;
    }
}
