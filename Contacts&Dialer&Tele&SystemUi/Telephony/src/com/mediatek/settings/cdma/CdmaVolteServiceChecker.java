package com.mediatek.settings.cdma;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.settings.TelephonyUtils;

import java.util.List;

/**
 * Handler class for CT Volte service update.
 */
public class CdmaVolteServiceChecker extends Handler {

    private final static String TAG = "CdmaVolteServiceChecker";
    private static final String ENHANCED_4G_MODE_ENABLED_SIM2 = "volte_vt_enabled_sim2";
    private boolean mChecking = false;
    private static CdmaVolteServiceChecker sInstance;
    private Context mContext;
    private final static int CHECK_DURATION = 120000;
    private final static int CHECK_TIME_OUT = 100;
    private Dialog mDialog;
    private static SubscriptionManager mSubscriptionManager;

    /**
     * Set singleton property for CT Volte Service.
     * @param context App or UI context.
     * @return instance for Cdma Volte Service.
     */
    public static CdmaVolteServiceChecker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CdmaVolteServiceChecker(context);
            mSubscriptionManager = SubscriptionManager.from(context);
        }
        return sInstance;
    }

    private CdmaVolteServiceChecker(Context context) {
        super(context.getMainLooper());
        mContext = context;
    }

    /**
     * Init settings for CT Volte Service.
     */
    public void init() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ENHANCED_4G_MODE_ENABLED),
                true, mContentObserver);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(ENHANCED_4G_MODE_ENABLED_SIM2),
                true, mContentObserver);
        //IntentFilter filter = new IntentFilter(MtkImsManager.ACTION_IMS_STATE_CHANGED);
        IntentFilter filter = new IntentFilter(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        //filter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        updateState();
    }

    /**
     * Handling on enable 4G state.
     */
    public void onEnable4gStateChanged() {
        Log.d(TAG, "onEnable4gStateChanged...");
        updateState();
    }

    private void updateState() {
        Log.d(TAG, "updateState, checking = " + mChecking);
        if (!mChecking && shouldShowVolteAlert()) {
            startTimeOutCheck();
        }

        if (mChecking && !shouldShowVolteAlert()) {
            stopTimeOutCheck();
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive, action = " + intent.getAction());
            updateState();
        };
    };

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange...");
            updateState();
        }
    };

    private int getMainCapabilitySubId() {
        int sub = MtkSubscriptionManager.getSubIdUsingPhoneId(
                TelephonyUtilsEx.getMainPhoneId());
        Log.d(TAG, "getMainCapabilitySubId = " + sub);
        return sub;
    }

    private boolean shouldShowVolteAlert() {
        boolean ret = false;
        int subId = getListenSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId) && TelephonyUtilsEx.isCtVolteEnabled()
                && TelephonyUtilsEx.isCt4gSim(subId)) {
            boolean isEnable4gOn = isEnable4gOn(subId);
            boolean volteOn = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(mContext,
                    SubscriptionManager.getPhoneId(subId));
            boolean imsAvailable = TelephonyUtils.isImsServiceAvailable(mContext, subId);
            boolean isRoaming = TelephonyUtilsEx.isRoaming(PhoneFactory
                    .getPhone(SubscriptionManager.getPhoneId(subId)));
            boolean isAirplaneMode = TelephonyUtilsEx.isAirPlaneMode();
            boolean isRadioOn = TelephonyUtils.isRadioOn(subId, mContext);
            boolean autoVolte = TelephonyUtilsEx.isCtAutoVolteEnabled();
            imsAvailable = (imsAvailable && isLteNetwork(subId));
            Log.d(TAG, "shouldShowVolteAlert, subId = " + subId + ", isEnable4gOn = "
                    + isEnable4gOn + ", volteOn = " + volteOn + "imsAvailable = " + imsAvailable
                    + ", isRoaming = " + isRoaming + ", isAirplaneMode" + isAirplaneMode
                    + ", autoVolte = " + autoVolte);
            ret = isEnable4gOn && volteOn && !imsAvailable && !isRoaming && !isAirplaneMode &&
                        isRadioOn && !autoVolte;
        }
        return ret;
    }

    private boolean isEnable4gOn(int subId) {
        int settingsNetworkMode = android.provider.Settings.Global.getInt(
                mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId,
                Phone.PREFERRED_NT_MODE);
        return settingsNetworkMode == Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
    }

    private void startTimeOutCheck() {
        Log.d(TAG, "startTimeOutCheck...");
        mChecking = true;
        sendMessageDelayed(obtainMessage(CHECK_TIME_OUT), CHECK_DURATION);
    }

    private void stopTimeOutCheck() {
        Log.d(TAG, "stopTimeOutCheck...");
        mChecking = false;
        removeMessages(CHECK_TIME_OUT);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        case CHECK_TIME_OUT:
            Log.d(TAG, "time out..., mchecking = " + mChecking);
            if (mChecking && shouldShowVolteAlert()) {
                showAlertDialog(getListenSubId());
            }
            break;
         default:
            break;
        }
    }

    private void showAlertDialog(int subId) {
        Log.d(TAG, "showAlertDialog...");
        if (mDialog != null && mDialog.isShowing()) {
            Log.w(TAG, "dialog showing, do nothing...");
            return;
        }

        final Context context = mContext.getApplicationContext();
        AlertDialog.Builder b = new AlertDialog.Builder(context,
                AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        b.setMessage(context.getString(
                R.string.alert_volte_no_service, PhoneUtils.getSubDisplayName(subId)));
        b.setCancelable(false);
        b.setPositiveButton(android.R.string.ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int phoneId = SubscriptionManager.getPhoneId(subId);
                Log.d(TAG, "ok clicked, phoneId = " + phoneId);
                if (phoneId != SubscriptionManager.DEFAULT_PHONE_INDEX) {
                    MtkImsManager.setEnhanced4gLteModeSetting(mContext, false,
                            phoneId);
                }
                stopTimeOutCheck();
            }
        });
        b.setNegativeButton(android.R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "cancel clicked...");
                sendMessageDelayed(obtainMessage(CHECK_TIME_OUT), CHECK_DURATION);
            }
        });
        b.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, "cancelled...");
                sendMessageDelayed(obtainMessage(CHECK_TIME_OUT), CHECK_DURATION);
            }
        });
        b.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "dismissed...");
                mDialog = null;
            }
        });
        Dialog dialog = b.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        mDialog = dialog;
    }

    private int getListenSubId() {
        int subId = getMainCapabilitySubId();
        if (mSubscriptionManager == null) {
            Log.d(TAG, "subManager mainId = " + subId);
            return subId;
        }

        if (SystemProperties.getInt("persist.mtk_mims_support", 1) == 1) {
            Log.d(TAG, "mims_support = 1, subId = " + subId);
            return subId;
        }

        if (TelephonyUtilsEx.isBothslotCt4gSim(mSubscriptionManager)) {
            Log.d(TAG, "getListenSubId mainId = " + subId);
            return subId;
        }

        List<SubscriptionInfo> infos = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (infos == null) {
            Log.d(TAG, "infos mainId = " + subId);
            return subId;
        }

        for (SubscriptionInfo info : infos) {
            int subTempId = info.getSubscriptionId();
            if (TelephonyUtilsEx.isCt4gSim(subTempId)) {
                subId = subTempId;
                break;
            }
        }

        Log.d(TAG, "getListenSubId = " + subId);
        return subId;
    }

    private boolean isLteNetwork(int subId) {
        boolean isLte = false;
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        final int dataNetworkType = TelephonyManager.getDefault().getDataNetworkType(subId);
        final int voiceNetworkType = TelephonyManager.getDefault().getVoiceNetworkType(subId);
        Log.d(TAG, "dataNetworkType = " + dataNetworkType
                + ", voiceNetworkType = " + voiceNetworkType);
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != dataNetworkType) {
            networkType = dataNetworkType;
        } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != voiceNetworkType) {
            networkType = voiceNetworkType;
        }

        if ((networkType == TelephonyManager.NETWORK_TYPE_LTE)
                || (networkType == TelephonyManager.NETWORK_TYPE_LTE_CA)) {
            isLte = true;
        }
        Log.d(TAG, "isLte = " + isLte);
        return isLte;
    }

}
