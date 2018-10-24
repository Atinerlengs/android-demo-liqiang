package com.mediatek.incallui.wfc;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.widget.Toast;

import com.android.incallui.Log;
import com.android.incallui.R;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;

import com.mediatek.incallui.plugin.ExtensionManager;
import com.mediatek.wfo.IMwiService;
import com.mediatek.wfo.IWifiOffloadService;
import com.mediatek.wfo.MwisConstants;
import com.mediatek.wfo.WifiOffloadManager;

import mediatek.telecom.MtkCall;

/**
 * RoveOutReceiver.
 */
public class RoveOutReceiver extends WifiOffloadManager.Listener {
    private static final String TAG = "RoveOutReceiver";
    private Context mContext;
    private Message mMsg = null;
    private static final int COUNT_TIMES = 3;
    private static final int EVENT_RESET_TIMEOUT = 1;
    private static final int CALL_ROVE_OUT_TIMER = 1800000;
    private IWifiOffloadService mWfoService = null;

    /**
     * Constructor.
     * @param context context
     */
    public RoveOutReceiver(Context context) {
        mContext = context;
        IBinder b = ServiceManager.getService(WifiOffloadManager.WFO_SERVICE);
        Log.d(TAG, "RoveOutReceiver constructor " + b);
        if (b != null) {
            mWfoService = IWifiOffloadService.Stub.asInterface(b);
        } else {
            b = ServiceManager.getService(MwisConstants.MWI_SERVICE);
            try {
                if (b != null) {
                    mWfoService = IMwiService.Stub.asInterface(b)
                            .getWfcHandlerInterface();
                } else {
                    Log.d(TAG, "No MwiService exist");
                }
            } catch (RemoteException e) {
                Log.d(TAG, "can't get MwiService");
            }
        }
        Log.d(TAG, "mWfoService is" + mWfoService);
    }

    /**
     * register RoveOutReceiver for handover events.
     */
    public void register() {
        if (mWfoService != null) {
            try {
                Log.d(TAG, "RoveOutReceiver register mWfoService");
                mWfoService.registerForHandoverEvent(this);
            } catch (RemoteException e) {
                Log.i(TAG, "RemoteException RoveOutReceiver()");
            }
        }
    }

    /**
     * unregister RoveOutReceiver.
     */
    public void unregister() {
        if (mWfoService != null) {
            try {
                Log.d(TAG, "RoveOutReceiver unregister mWfoService ");
                mWfoService.unregisterForHandoverEvent(this);
            } catch (RemoteException e) {
                Log.i(TAG, "RemoteException RoveOutReceiver()");
            }
            WfcDialogActivity.sCount = 0;
            if (mMsg != null) {
                mHandler.removeMessages(mMsg.what);
            }
        }
    }

    @Override
    public void onHandover(int simIdx, int stage, int ratType) {
        Log.d(TAG, "onHandover stage: " + stage + "ratType : " + ratType);
        ExtensionManager.getInCallExt().showHandoverNotification(mHandler, stage, ratType);
        checkForVideoOverWifi(mHandler, stage, ratType);
    }

    @Override
    public void onRoveOut(int simIdx, boolean roveOut, int rssi) {
        Log.d(TAG, "onRoveOut: " + roveOut);
        DialerCall call = CallList.getInstance().getActiveOrBackgroundCall();
        if (roveOut) {
            if ((call != null && call.hasProperty(android.telecom.Call.Details.PROPERTY_WIFI))
                    && (WfcDialogActivity.sCount < COUNT_TIMES)
                    && !WfcDialogActivity.sIsShowing) {
                final Intent intent1 = new Intent(mContext, WfcDialogActivity.class);
                intent1.putExtra(WfcDialogActivity.SHOW_WFC_ROVE_OUT_POPUP, true);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent1);
                if (WfcDialogActivity.sCount == 0) {
                    mMsg = mHandler.obtainMessage(EVENT_RESET_TIMEOUT);
                    mHandler.removeMessages(mMsg.what);
                    mHandler.sendMessageDelayed(mMsg, CALL_ROVE_OUT_TIMER);
                    Log.i(TAG, "WfcSignalReceiver sendMessageDelayed ");
                }
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESET_TIMEOUT:
                    Log.i(TAG, "WfcSignalReceiver EVENT_RESET_TIMEOUT ");
                    WfcDialogActivity.sCount = 0;
                    break;
                default:
                    Log.i(TAG, "Message not expected: ");
                    break;
            }
        }
    };

    /**
     * M: check if video call over wifi is allowed or not.
     * if not allowed then show error toast to user and
     * convert video call to voice call on handover from LTE to WIFI
     *
     * @param handler handler
     * @param stage handover stage
     * @param ratType handover ratType
     */
    public void checkForVideoOverWifi(Handler handler, int stage, int ratType) {
        final DialerCall call = CallList.getInstance().getActiveOrBackgroundCall();
        if (call == null) {
            return;
        }
        if ((call.getVideoFeatures() == null) || call.isVideoCall() == false) {
            return;
        }
        boolean isVideoOverWifiDisabled =
            call.getVideoFeatures().disableVideoCallOverWifi();
        Log.d(TAG, "[WFC]checkForVideoOverWifi isVideoOverWifiDisabled = "
            + isVideoOverWifiDisabled);
        if (!isVideoOverWifiDisabled) {
            return;
        }
        if (call != null && !call.hasProperty(MtkCall.MtkDetails.MTK_PROPERTY_VOLTE)) {
            if (stage == WifiOffloadManager.HANDOVER_START &&
                    ratType == WifiOffloadManager.RAN_TYPE_WIFI) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, mContext.getResources().getString(
                                R.string.video_over_wifi_not_available),
                                Toast.LENGTH_SHORT).show();
                        call.getVideoTech().downgradeToAudio();
                    }
                });
            }
        }
    }
}
