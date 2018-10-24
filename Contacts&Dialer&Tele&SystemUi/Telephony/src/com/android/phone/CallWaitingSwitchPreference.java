package com.android.phone;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.phone.TimeConsumingPreferenceListener;
import com.mediatek.settings.TelephonyUtils;

import static com.android.phone.TimeConsumingPreferenceActivity.RESPONSE_ERROR;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.mediatek.phone.ext.ExtensionManager;

public class CallWaitingSwitchPreference extends SwitchPreference {
    private static final String LOG_TAG = "CallWaitingSwitchPreference";
    private final boolean DBG = true; //(PhoneGlobals.DBG_LEVEL >= 2);

    private final MyHandler mHandler = new MyHandler();
    private Phone mPhone;
    private TimeConsumingPreferenceListener mTcpListener;
    /// M: Wait for xcap server updates call forward completely @{
    private static final int DELAY_TIME = 1500;
    /// }@

    public CallWaitingSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CallWaitingSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.switchPreferenceStyle);
    }

    public CallWaitingSwitchPreference(Context context) {
        this(context, null);
    }

    public void init(
            TimeConsumingPreferenceListener listener, boolean skipReading, Phone phone) {
        mPhone = phone;
        mTcpListener = listener;

        if (!skipReading) {
            mPhone.getCallWaiting(mHandler.obtainMessage(MyHandler.MESSAGE_GET_CALL_WAITING,
                    MyHandler.MESSAGE_GET_CALL_WAITING, MyHandler.MESSAGE_GET_CALL_WAITING));
            if (mTcpListener != null) {
                mTcpListener.onStarted(this, true);
            }
        }
    }

    @Override
    protected void onClick() {
        /// Add for [VoLTE_SS] @{
        if (TelephonyUtils.shouldShowOpenMobileDataDialog(
                getContext(), mPhone.getSubId()) && !MtkImsManager.isSupportMims()) {
            TelephonyUtils.showOpenMobileDataDialog(getContext(), mPhone.getSubId());
            return;
        }
        super.onClick();
        /// @}
        mPhone.setCallWaiting(isChecked(),
                mHandler.obtainMessage(MyHandler.MESSAGE_SET_CALL_WAITING));
        if (mTcpListener != null) {
            mTcpListener.onStarted(this, false);
        }
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_CALL_WAITING = 0;
        static final int MESSAGE_SET_CALL_WAITING = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_CALL_WAITING:
                    handleGetCallWaitingResponse(msg);
                    break;
                case MESSAGE_SET_CALL_WAITING:
                    handleSetCallWaitingResponse(msg);
                    break;
            }
        }

        private void handleGetCallWaitingResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (mTcpListener != null) {
                if (msg.arg2 == MESSAGE_SET_CALL_WAITING) {
                    mTcpListener.onFinished(CallWaitingSwitchPreference.this, false);
                } else {
                    mTcpListener.onFinished(CallWaitingSwitchPreference.this, true);
                }
            }

            if (ar.exception instanceof CommandException) {
                if (DBG) {
                    Log.d(LOG_TAG, "handleGetCallWaitingResponse: CommandException=" +
                            ar.exception);
                }
                if (mTcpListener != null) {
                    mTcpListener.onException(CallWaitingSwitchPreference.this,
                            (CommandException) ar.exception);
                }
            } else if (ar.userObj instanceof Throwable || ar.exception != null) {
                // Still an error case but just not a CommandException.
                if (DBG) {
                    Log.d(LOG_TAG, "handleGetCallWaitingResponse: Exception" + ar.exception);
                }
                if (mTcpListener != null) {
                    mTcpListener.onError(CallWaitingSwitchPreference.this, RESPONSE_ERROR);
                }
            } else {
                if (DBG) {
                    Log.d(LOG_TAG, "handleGetCallWaitingResponse: CW state successfully queried.");
                }
                ///M : CRALPS02113837 @{
                ExtensionManager.getCallFeaturesSettingExt()
                        .resetImsPdnOverSSComplete(getContext(), msg.arg2);
                /// @}

                ///M: SmartCallFwd : Disable callforward (when unreachable)
                ///if smart call forwarding is active @ {
                ExtensionManager.getCallFeaturesSettingExt().disableCallFwdPref(
                        getContext(), (Object) mPhone, CallWaitingSwitchPreference.this, -1);
                /// @}

                int[] cwArray = (int[]) ar.result;
                // If cwArray[0] is = 1, then cwArray[1] must follow,
                // with the TS 27.007 service class bit vector of services
                // for which call waiting is enabled.
                try {
                    Log.d(LOG_TAG, "handleGetCallWaitingResponse cwArray[0]:cwArray[1] = "
                            + cwArray[0] + ":" + cwArray[1]);
                    if (cwArray != null) {
                    setChecked(((cwArray[0] == 1) && ((cwArray[1] & 0x01) == 0x01)));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "handleGetCallWaitingResponse: improper result: err ="
                            + e.getMessage());
                }
            }
        }

        private void handleSetCallWaitingResponse(Message msg) {
            final AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception != null) {
                if (DBG) {
                    Log.d(LOG_TAG, "handleSetCallWaitingResponse: ar.exception=" + ar.exception);
                }
                //setEnabled(false);
                /// M: reset the checkbox if set fail.
                setChecked(!isChecked());
            }
            if (DBG) {
                Log.d(LOG_TAG, "handleSetCallWaitingResponse: re get start");
            }
            /// M: modem has limitation that if query result immediately set, will
            //  not get the right result, so we need wait 1s to query. just AP workaround @{
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (DBG) {
                        Log.d(LOG_TAG, "handleSetCallWaitingResponse: re get");
            }
            if (DBG) Log.d(LOG_TAG, "handleSetCallWaitingResponse: re get");

            mPhone.getCallWaiting(obtainMessage(MESSAGE_GET_CALL_WAITING,
                    MESSAGE_SET_CALL_WAITING, MESSAGE_SET_CALL_WAITING, ar.exception));
        }
            };
            postDelayed(runnable, DELAY_TIME);
            // @}
        }
    }
}
