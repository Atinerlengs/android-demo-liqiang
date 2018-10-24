package com.freeme.applock;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;

import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;

import com.android.internal.widget.LockPatternUtils;
import com.freeme.applock.settings.LogUtil;

public class AppLockSpassUnlockThread extends Thread {
    private static final String TAG = "AppLockSpassUlThread";
    private static final boolean DEBUG = true;
    private final int MSG_FINGERPRINT_IDENTIFY_START = 1116;
    private final int MSG_FINGERPRINT_IDENTIFY_STOP = 1117;
    private final int MSG_FINGERPRINT_SENSOR_START = 1119;
    private final int MSG_FINGERPRINT_SENSOR_STOP = 1120;
    private final int MSG_FINGERPRINT_SENSOR_ERROR = 1121;
    private final int MSG_FINGERPRINT_SENSOR_FAILURE = 1122;
    private final int MSG_FINGERPRINT_RESPONDING_ERROR = 1123;
    private final int MSG_DESTROY_FINGERPRINT_THREAD = 1124;
    private final int MSG_FINGERPRINT_DATABASE_ERROR = 1125;
    private final int MSG_FINGERPRINT_DATABASE_DELETED = 1126;
    private final int MSG_FINGERPRINT_SENSOR_SUCCEED = 1127;
    private final int MSG_FINGERPRINT_SENSOR_HELP = 1128;
    private final int SCREEN_TIMEOUT = 30000;

    private boolean mBroadcastRegistered;
    private Context mContext;
    private long mExpiredTime;
    private FingerprintManager mFpm;
    private boolean mIsActive;
    private boolean mIsRegisteredClient;
    private volatile boolean mIsRunning;
    private LockPatternUtils mLockPatternUtils;
    private PowerManager mPowerManager;
    private int mQualityMessage;
    private SpassCallback mSpassCallback;
    private Handler mSpassThreadHandler;
    private IBinder mToken;
    private long[] mVibraterPattern = new long[]{0, 10, 60, 15};
    private Vibrator mVibraterService;

    public interface SpassCallback {
        void dismissLock();

        int getFailAttemps();

        void reportFailAttemps();

        void showBackupPassword();

        void showErrorImage(int i);

        void showErrorMessage(String str);

        void showErrorPopup(int i);
    }

    /*/ freeme.zhongkai.zhu. 20171027. fingerprint
    private IFingerprintClient mFingerprintClient = new Stub() {
        public void onFingerprintEvent(FingerprintEvent evt) throws RemoteException {
            FingerprintEvent event = evt;
            if (evt == null) {
                LogUtil.e(TAG, "Invalid Event");
            } else if (mSpassThreadHandler != null) {
                mSpassThreadHandler.sendMessage(Message.obtain(mSpassThreadHandler, evt.eventId, evt));
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.samsung.android.fingerprint.action.SERVICE_STARTED".equals(intent.getAction()) && mSpassThreadHandler != null) {
                mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_START);
            }
        }
    };

    public AppLockSpassUnlockThread(Context context) {
        super(TAG);
        mContext = context;
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mVibraterService = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mFpm = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        if (mFpm == null) {
            LogUtil.d(TAG, "FingerPrintManager is not possilbe");
        } else {
            mFpm.request(1, 0);
        }
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    public void setSpassCallback(SpassCallback callback) {
        mSpassCallback = callback;
    }

    @Override
    public void run() {
        Looper.prepare();
        mSpassThreadHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                FingerprintEvent event;
                String errorMessage;
                switch (msg.what) {
                    case 11:
                        LogUtil.d(TAG, "handleMessage : EVENT_IDENTIFY_READY");
                        break;
                    case 12:
                        LogUtil.d(TAG, "handleMessage : EVENT_IDENTIFY_STARTED");
                        break;
                    case 13:
                        event = (FingerprintEvent) msg.obj;
                        if (event.eventResult == 0 || event.eventResult != -1) {
                            LogUtil.d(TAG, "handleMessage : RESULT_SUCCESS");
                            handleUnlock();
                            break;
                        }
                        LogUtil.d(TAG, "handleMessage : RESULT_FAILED eventStatus = " + event.eventStatus);
                        if (event.eventStatus == 4) {
                            //empty
                        } else if (event.eventStatus == 7) {
                            if (mSpassThreadHandler != null) {
                                mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_ERROR);
                                break;
                            }
                        } else if (event.eventStatus == 121) {
                             if (mSpassThreadHandler != null) {
                                mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_FAILURE);
                                break;
                            }
                        } else if (event.eventStatus != 8) {
                            if (event.eventStatus == 11) {
                                errorMessage = event.getImageQualityFeedback();
                                mQualityMessage = event.getImageQuality();
                                if (mSpassCallback != null) {
                                    mSpassCallback.showErrorMessage(errorMessage);
                                    mSpassCallback.showErrorImage(mQualityMessage);
                                    mSpassCallback.reportFailAttemps();
                                }
                            }
                            if (event.eventStatus != 122) {
                                errorMessage = event.getImageQualityFeedback();
                                if (mSpassCallback != null) {
                                    mSpassCallback.showErrorMessage(errorMessage);
                                    mSpassCallback.showErrorImage(mQualityMessage);
                                }
                            } else if (mSpassThreadHandler != null) {
                                mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_DATABASE_ERROR);
                            }

                            if (mVibraterService != null) {
                                mVibraterService.vibrate(mVibraterPattern, -1);
                                break;
                            }
                        }
                        handleStartIdentify();
                        break;
                    case 14:
                        LogUtil.d(TAG, "handleMessage : EVENT_IDENTIFY_STATUS  ");
                        event = (FingerprintEvent) msg.obj;
                        if (event.eventStatus == 20) {
                            errorMessage = event.getImageQualityFeedback();
                            mQualityMessage = event.getImageQuality();
                            if (mSpassCallback != null) {
                                mSpassCallback.showErrorMessage(errorMessage);
                                mSpassCallback.showErrorImage(mQualityMessage);
                                break;
                            }
                        }
                        break;
                    case 16:
                        LogUtil.d(TAG, "handleMessage : EVENT_IDENTIFY_COMPLETED  ");
                        handleStartIdentify();
                        break;
                    case 1011:
                        LogUtil.d(TAG, "handleMessage : EVENT_FINGER_REMOVED");
                        break;
                    case MSG_FINGERPRINT_IDENTIFY_START:
                        handleStartIdentify();
                        break;
                    case MSG_FINGERPRINT_IDENTIFY_STOP:
                        handleStopIdentify();
                        break;
                    case MSG_FINGERPRINT_SENSOR_START:
                        handleStartFingerPrintSensor();
                        break;
                    case MSG_FINGERPRINT_SENSOR_STOP:
                        unregisterClient();
                        break;
                    case MSG_FINGERPRINT_SENSOR_ERROR:
                        handleSensorError();
                        break;
                    case MSG_FINGERPRINT_SENSOR_FAILURE:
                        handleSensorFailure();
                        break;
                    case MSG_FINGERPRINT_RESPONDING_ERROR:
                        handleRespondingError();
                        break;
                    case MSG_DESTROY_FINGERPRINT_THREAD:
                        handleDestoryFingerPrintThread();
                        break;
                    case MSG_FINGERPRINT_DATABASE_ERROR:
                        handleFingerPrintDataBaseError();
                        break;
                    case MSG_FINGERPRINT_DATABASE_DELETED:
                        handleFingerPrintDataBaseDeleted();
                        break;
                    default:
                        LogUtil.e(TAG, "Unhandled message = " + msg.what);
                        return;
                }
            }
        };
        Looper.loop();
    }

    protected void removeAllMsgs() {
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_IDENTIFY_START);
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_IDENTIFY_STOP);
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_START);
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_STOP);
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_ERROR);
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_FAILURE);
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_RESPONDING_ERROR);
            mSpassThreadHandler.removeMessages(MSG_DESTROY_FINGERPRINT_THREAD);
        }
    }

    public void startIdentify() {
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_IDENTIFY_START);
        }
    }

    public void stopIdentify() {
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_IDENTIFY_STOP);
        }
    }

    public void stopSensor() {
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(13);
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_STOP);
        }
    }

    void handleStartFingerPrintSensor() {
        if (mPowerManager == null || mPowerManager.isScreenOn()) {
            LogUtil.d(TAG, "handleStartFingerPrintSensor  ");
            if (!mIsRegisteredClient) {
                LogUtil.d(TAG, "start( mIsRegisteredClient=" + mIsRegisteredClient + " isRunning()=" + mIsRunning + " )");
                try {
                    Intent intent = new Intent();
                    intent.setClassName("com.samsung.android.fingerprint.service", "com.samsung.android.fingerprint.service.FingerprintServiceStarter");
                    mContext.startService(intent);
                } catch (Exception e) {
                    LogUtil.d(TAG, "Failed to call FingerprintServiceStarter");
                }
                mExpiredTime = System.currentTimeMillis() + SCREEN_TIMEOUT;
                mIsRegisteredClient = registerClient();
                mIsRunning = DEBUG;
                if (mIsRegisteredClient) {
                    unregisterBroadcastReceiver();
                    if (mSpassThreadHandler != null) {
                        mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_IDENTIFY_START);
                    }
                } else {
                    registerBroadcastReceiver();
                }
            } else if (mIsRunning) {
                LogUtil.d(TAG, "fingerprint is already running.");
            } else {
                if (mSpassThreadHandler != null) {
                    mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_IDENTIFY_START);
                }
                mIsRunning = DEBUG;
                LogUtil.d(TAG, "sensor is already running.");
            }
            return;
        }
        handleStopIdentify();
    }

    private void handleStartIdentify() {
        LogUtil.d(TAG, "handleStartIdentify( mIsRegisteredClient = " + mIsRegisteredClient + ")");
        if (mSpassCallback != null) {
            int attemps = mSpassCallback.getFailAttemps();
            if (mIsRegisteredClient && mPowerManager.isScreenOn() && mFpm != null && mToken != null) {
                if (mFpm.isSensorReady()) {
                    int currentUserId = ActivityManager.getCurrentUser();
                    LogUtil.d(TAG, "handleStartIdentify currentUserId = " + currentUserId);
                    int result = mFpm.identifyForMultiUser(mToken, currentUserId, null);
                    if (result == 0) {
                        LogUtil.d(TAG, "identify OK");
                        return;
                    } else if (result == -2) {
                        LogUtil.d(TAG, "identify: RESULT_IN_PROGRESS");
                        unregisterClient();
                        if (mSpassThreadHandler != null) {
                            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_START);
                            return;
                        }
                        return;
                    } else if (result == -3) {
                        LogUtil.d(TAG, "identify: RESULT_INVALID_TOKEN");
                        unregisterClient();
                        if (mSpassThreadHandler != null) {
                            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_START);
                            return;
                        }
                        return;
                    } else if (result == -4) {
                        LogUtil.d(TAG, "identify: RESULT_DATABASE_FAILURE");
                        if (mSpassThreadHandler != null) {
                            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_DATABASE_ERROR);
                            return;
                        }
                        return;
                    } else if (result == -5) {
                        LogUtil.d(TAG, "identify:RESULT_NO_REGISTERED_FINGER");
                        if (mSpassThreadHandler != null) {
                            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_DATABASE_DELETED);
                            return;
                        }
                        return;
                    } else {
                        LogUtil.e(TAG, "identify request failed.");
                        if (mSpassThreadHandler != null) {
                            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_RESPONDING_ERROR);
                            return;
                        }
                        return;
                    }
                }
                LogUtil.e(TAG, "handleStartIdentify is called but isSensorReady is false");
                if (mSpassThreadHandler != null) {
                    mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_ERROR);
                }
            }
        }
    }

    public void handleStopIdentify() {
        LogUtil.d(TAG, "handleStopIdentify()");
        if (mFpm != null && mToken != null) {
            mFpm.cancel(mToken);
        }
        mIsRunning = false;
    }

    public void handleRespondingError() {
        LogUtil.d(TAG, "handleRespondingError()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_RESPONDING_ERROR);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_not_responding_error_message);
        }
        unregisterClient();
    }

    public void handleFingerPrintDataBaseError() {
        LogUtil.d(TAG, "handleFingerPrintDataBaseError()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_DATABASE_ERROR);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_database_error_message);
        }
        unregisterClient();
    }

    public void handleFingerPrintDataBaseDeleted() {
        LogUtil.d(TAG, "handleFingerPrintDataBaseDeleted()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_DATABASE_DELETED);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_database_deleted_message);
        }
        unregisterClient();
    }

    public void handleSensorError() {
        LogUtil.d(TAG, "handleSensorError()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_ERROR);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_sensor_error_message);
        }
        unregisterClient();
    }

    public void handleSensorFailure() {
        LogUtil.d(TAG, "handleSensorFailure()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_FAILURE);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_not_responding_error_message);
        }
    }

    private boolean registerClient() {
        boolean z = DEBUG;
        if (mFpm == null) {
            mFpm = FingerprintManager.getInstance(mContext, 1);
        }
        if (mFpm == null) {
            LogUtil.d(TAG, "registerClient() : FingerPrintManager is not possilbe");
            return false;
        }
        FingerprintClientSpecBuilder builder = new FingerprintClientSpecBuilder("system");
        builder.demandExtraEvent(DEBUG);
        builder.useManualTimeout(DEBUG);
        builder.setPrivilegedAttr(-2147483645);
        mToken = mFpm.registerClient(mFingerprintClient, builder.build());
        LogUtil.d(TAG, "registerClient() mToken = " + mToken);
        return mToken != null;
    }

    public void unregisterClient() {
        if (mFpm != null && mToken != null) {
            mFpm.unregisterClient(mToken);
            LogUtil.d(TAG, "unregisterClient() mToken = " + mToken);
            mToken = null;
            mIsRegisteredClient = false;
            mIsRunning = false;
        }
    }

    private void registerBroadcastReceiver() {
        if (!mBroadcastRegistered) {
            LogUtil.d(TAG, "registerBroadcastReceiver");
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.samsung.android.fingerprint.action.SERVICE_STARTED");
            mContext.registerReceiver(mBroadcastReceiver, filter);
            mBroadcastRegistered = DEBUG;
        }
    }

    private void unregisterBroadcastReceiver() {
        if (mBroadcastRegistered) {
            LogUtil.d(TAG, "unregisterBroadcastReceiver");
            mContext.unregisterReceiver(mBroadcastReceiver);
            mBroadcastRegistered = false;
        }
    }

    public void onPause() {
        LogUtil.d(TAG, "onPause()");
        synchronized (this) {
            if (mSpassThreadHandler != null) {
                mSpassThreadHandler.removeMessages(13);
                mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_STOP);
            }
        }
    }

    public void onResume() {
        LogUtil.d(TAG, "onResume()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_START);
        }
    }

    public void cleanUp() {
        LogUtil.d(TAG, "cleanUp()");
        unregisterClient();
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_DESTROY_FINGERPRINT_THREAD);
        }
    }

    public void reset() {
        LogUtil.d(TAG, "reset()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_START);
        }
    }

    private void handleUnlock() {
        LogUtil.d(TAG, "handleUnlock()");
        if (mSpassCallback != null) {
            boostCpuClock();
            mSpassCallback.dismissLock();
        }
        cleanUp();
    }

    private void handleReportFailedAttempts() {
        LogUtil.d(TAG, "handleReportFailedAttempts()");
    }

    private void handleDestoryFingerPrintThread() {
        LogUtil.d(TAG, "handleDestoryFingerPrintThread()");
        synchronized (this) {
            removeAllMsgs();
            Looper.myLooper().quit();
            if (mContext != null) {
                mContext = null;
            }
            if (mSpassCallback != null) {
                mSpassCallback = null;
            }
        }
    }
    /*/
    public AppLockSpassUnlockThread(Context context) {
        super(TAG);
        mFpm = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        mVibraterService = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void run() {
        Looper.prepare();
        mSpassThreadHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_FINGERPRINT_SENSOR_ERROR:
                        handleSensorError();
                        break;
                    case MSG_FINGERPRINT_SENSOR_SUCCEED:
                        handleUnlock();
                        break;
                    case MSG_FINGERPRINT_SENSOR_FAILURE:
                        handleSensorFailure();
                        break;
                    case MSG_FINGERPRINT_SENSOR_HELP: {
                        handleSensorHelp((String) msg.obj);
                    }
                    default:
                        LogUtil.e(TAG, "Unhandled message = " + msg.what);
                        break;
                }
            }
        };
        Looper.loop();
    }

    private CancellationSignal mFingerprintCancelSignal;
    private FingerprintManager.AuthenticationCallback mAuthenticationCallback
            = new FingerprintManager.AuthenticationCallback() {
        @Override
        public void onAuthenticationFailed() {
            handleFingerprintAuthFailed();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            handleFingerprintAuthenticated(result.getUserId());
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            handleFingerprintHelp(helpMsgId, helpString.toString());
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            handleFingerprintError(errMsgId, errString.toString());
        }

        @Override
        public void onAuthenticationAcquired(int acquireInfo) {
            handleFingerprintAcquired(acquireInfo);
        }
    };

    private void handleFingerprintAuthFailed() {
        LogUtil.d(TAG, "handleFingerprintAuthFailed");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_ERROR);
        }
    }

    private void handleFingerprintAuthenticated(int userId) {
        LogUtil.d(TAG, "handleFingerprintAuthenticated, userId=" + userId);
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_SUCCEED);
        }
    }

    private void handleFingerprintHelp(int msgId, String helpString) {
        LogUtil.d(TAG, "handleFingerprintHelp, msgid=" + msgId + " helpString=" + helpString);
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendMessage(
                    Message.obtain(mSpassThreadHandler, MSG_FINGERPRINT_SENSOR_HELP, helpString));
        }
    }

    private void handleFingerprintError(int msgId, String errString) {
        LogUtil.d(TAG, "handleFingerprintError, msgid=" + msgId + " errString=" + errString);
        // FingerprintManager.FINGERPRINT_ERROR_CANCELED
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendMessage(
                    Message.obtain(mSpassThreadHandler, MSG_FINGERPRINT_SENSOR_FAILURE, errString));
        }
    }

    private void handleFingerprintAcquired(int acquireInfo) {
        LogUtil.d(TAG, "handleFingerprintAcquired, acquireInfo=" + acquireInfo);
//        if (acquireInfo != FingerprintManager.FINGERPRINT_ACQUIRED_GOOD) {
//            return;
//        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void handleSensorError() {
        LogUtil.d(TAG, "handleSensorError()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_ERROR);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_sensor_error_message);
        }
        //TODO, cancel fingerprint?
    }

    private void handleUnlock() {
        LogUtil.d(TAG, "handleUnlock()");
        if (mSpassCallback != null) {
            mSpassCallback.dismissLock();
        }
        cleanUp();
    }

    private void handleSensorFailure() {
        LogUtil.d(TAG, "handleSensorFailure()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_FAILURE);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorPopup(R.string.applock_finger_print_not_responding_error_message);
        }
    }

    private void handleSensorHelp(String helpString) {
        LogUtil.d(TAG, "handleSensorHelp()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.removeMessages(MSG_FINGERPRINT_SENSOR_HELP);
        }
        if (mSpassCallback != null) {
            mSpassCallback.showErrorMessage(helpString);
        }
    }

    private void startListeningForFingerprint() {
        int userId = ActivityManager.getCurrentUser();
        if (isUnlockWithFingerprintPossible(userId)) {
            if (mFingerprintCancelSignal != null) {
                mFingerprintCancelSignal.cancel();
            }
            mFingerprintCancelSignal = new CancellationSignal();
            mFpm.authenticate(null, mFingerprintCancelSignal, 0, mAuthenticationCallback, null, userId);
        }
    }

    public void onResume() {
        LogUtil.d(TAG, "onResume()");
        if (mSpassThreadHandler != null) {
            mSpassThreadHandler.sendEmptyMessage(MSG_FINGERPRINT_SENSOR_START);
        }
        startListeningForFingerprint();
    }

    public void reset() {
    }

    public void onPause() {
    }

    public void stopSensor() {
    }

    public void cleanUp() {
    }

    public void setSpassCallback(SpassCallback callback) {
        mSpassCallback = callback;
    }

    private boolean isUnlockWithFingerprintPossible(int userId) {
        return mFpm != null && mFpm.isHardwareDetected() && !isFingerprintDisabled(userId)
                && mFpm.getEnrolledFingerprints(userId).size() > 0;
    }

    private boolean isFingerprintDisabled(int userId) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && (dpm.getKeyguardDisabledFeatures(null, userId)
                & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0
                /* || isSimPinSecure() */;
    }
    //*/
}
