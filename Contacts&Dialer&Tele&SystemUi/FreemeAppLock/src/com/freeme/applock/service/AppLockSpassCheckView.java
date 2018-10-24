package com.freeme.applock.service;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings.Global;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.freeme.applock.AppLockSpassUnlockThread;
import com.freeme.applock.AppLockSpassUnlockThread.SpassCallback;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.R;
import com.freeme.applock.settings.LogUtil;

public class AppLockSpassCheckView extends AppLockCheckBaseView implements SpassCallback {
    static final String TAG = "AppLockSpassCheckView";
    private static final boolean DEBUG = true;

    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;
    private static final int SHOW_FINGERPRINT_INSTRUCTIONS = 10;
    private static final int SHOW_FINGERPRINT_ERROR_MESSAGE = 11;
    private static final int SHOW_FINGERPRINT_ERROR_POPUP = 12;
    private static final int SHOW_BACKUP_PASSWORD = 13;
    private static final int SHOW_AUTO_WIPE_POPUP = 16;
    private static final int DISMISS_LOCK = 17;
    private static final int SHOW_INSTRUCTIONS_BY_TOUCH = 18;
    private static final int SHOW_FINGERPRINT_ERROR_IMAGE = 19;
    private static final int MSG_LOCK_OUT = 20;
    private static AlertDialog mErrorDialog;
    protected RelativeLayout mAnimationBox;
    private AppLockSpassUnlockThread mAppLockFPThread;
    private TextView mBackupPasswordButton;
    protected View mBouncerEMA;
    private CountDownTimer mCountdownTimer;
    protected boolean mEMAPress;
    private FingerprintManager mFingerprintManager;
    private TextView mFingerprintStatusText;
    private Handler mHandler;
    boolean mHasWindowFocus;
    private ImageView mInfoImage;
    private int mNumWrongConfirmAttempts;
    private boolean mUseBlackTextOnWhiteWallpaper;

    public AppLockSpassCheckView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHandler = new Handler(Looper.myLooper(), null, DEBUG) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SHOW_FINGERPRINT_INSTRUCTIONS:
                        handleShowFingerPrintInstruction(msg.arg1);
                        break;
                    case SHOW_FINGERPRINT_ERROR_MESSAGE:
                        handleShowErrorMessage((String) msg.obj);
                        break;
                    case SHOW_FINGERPRINT_ERROR_POPUP:
                        handleShowErrorPopup(msg.arg1);
                        break;
                    case SHOW_BACKUP_PASSWORD:
                        handleShowBackupPassword();
                        break;
                    case DISMISS_LOCK:
                        handleDismissLock();
                        break;
                    case SHOW_FINGERPRINT_ERROR_IMAGE:
                        handleShowErrorImage(((Integer) msg.obj));
                        break;
                    case MSG_LOCK_OUT:
                        handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
                        break;
                    default:
                        break;
                }
            }
        };
        mHasWindowFocus = DEBUG;
        mLayoutInflater.inflate(R.layout.app_lock_spass_identify, this);
        mAppLockFPThread = new AppLockSpassUnlockThread(mContext);
        mAppLockFPThread.start();
        mUseBlackTextOnWhiteWallpaper = Global.getInt(mContext.getContentResolver(), "white_lockscreen_wallpaper", 0) == 1;
        initFingerprintManager();
        mBackupPasswordButton = (TextView) findViewById(R.id.text_backup_password);
        if (mBackupPasswordButton != null) {
            mBackupPasswordButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSpassPasswordView();
                }
            });
        }
        mFingerprintStatusText = (TextView) findViewById(R.id.applock_fingerprint_identify_info_text);
        mAnimationBox = (RelativeLayout) findViewById(R.id.identify_animation_box);
        mInfoImage = (ImageView) findViewById(R.id.identify_error_image);
        mAppLockFPThread.setSpassCallback(this);
    }

    public AppLockSpassCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppLockSpassCheckView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLockSpassCheckView(Context context) {
        this(context, null);
    }

    protected void setAnimationLayout() {
        if (mAnimationBox != null) {
            mAnimationBox.setVisibility(View.VISIBLE);
        }
        if (mInfoImage != null) {
            mInfoImage.setImageResource(R.drawable.icon_default);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        LogUtil.d(TAG, "onWindowFocusChanged(): hasWindowFocus=" + hasWindowFocus);
        mHasWindowFocus = hasWindowFocus;
        if (mHasWindowFocus) {
            LogUtil.d(TAG, "onWindowFocusChanged - get focus");
            if (mAppLockFPThread != null && AppLockUtils.getRemaingTimeToUnlock() == 0) {
                mAppLockFPThread.onResume();
                return;
            }
            return;
        }
        LogUtil.d(TAG, "onWindowFocusChanged - loosing focus");
        if (mAppLockFPThread != null) {
            mAppLockFPThread.stopSensor();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNumWrongConfirmAttempts = AppLockUtils.getFailedUnlockAttempts();
        long deadline = AppLockUtils.getRemaingTimeToUnlock();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
            return;
        }
        if (mAppLockFPThread != null) {
            mAppLockFPThread.onResume();
        }
        mHandler.removeMessages(SHOW_FINGERPRINT_INSTRUCTIONS);
        mHandler.sendEmptyMessageDelayed(SHOW_FINGERPRINT_INSTRUCTIONS, 200);
        setAnimationLayout();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(SHOW_FINGERPRINT_INSTRUCTIONS);
        if (mAppLockFPThread != null) {
            mAppLockFPThread.cleanUp();
            mAppLockFPThread = null;
        }
        resetErrorMessage();
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
        if (mErrorDialog != null && mErrorDialog.isShowing()) {
            mErrorDialog.dismiss();
        }
        mErrorDialog = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void cleanUp() {
        LogUtil.d(TAG, "cleanUp()");
        if (mAppLockFPThread != null) {
            mAppLockFPThread.cleanUp();
            mAppLockFPThread = null;
        }
        if (mErrorDialog != null && mErrorDialog.isShowing()) {
            mErrorDialog.dismiss();
        }
        mErrorDialog = null;
    }

    private void initFingerprintManager() {
        /*/ freeme.zhongkai.zhu. 20171027. applock
        if (mFingerprintManager == null) {
            mFingerprintManager = FingerprintManager.getInstance(mContext, 1);
        }
        //*/
    }

    private void setFingerprintStatusText(String string) {
        if (mFingerprintStatusText != null) {
            mFingerprintStatusText.setText(string);
            mFingerprintStatusText.announceForAccessibility(string);
        }
    }

    @Override
    protected void onFinishInflate() {
        LogUtil.d(TAG, "onFinishInflate");
        super.onFinishInflate();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        LogUtil.d(TAG, "onLayout");
    }

    public void dismissLock() {
        LogUtil.d(TAG, "dismissLock()");
        mHandler.removeMessages(DISMISS_LOCK);
        mHandler.sendMessage(mHandler.obtainMessage(DISMISS_LOCK));
    }

    public void showErrorMessage(String errorMessage) {
        LogUtil.d(TAG, "showErrorMessage( errorMessage = " + errorMessage + " )");
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_MESSAGE);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_MESSAGE, errorMessage));
    }

    public void showErrorPopup(int resid) {
        LogUtil.d(TAG, "showErrorPopup( resid = " + resid + " )");
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_POPUP);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_POPUP, resid, 0));
    }

    public void showBackupPassword() {
        mHandler.removeMessages(SHOW_BACKUP_PASSWORD);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_BACKUP_PASSWORD));
    }

    public void reportFailAttemps() {
        AppLockUtils.reportFailedUnlockAttempts();
        if (AppLockUtils.getFailedUnlockAttempts() % 5 == 0) {
            AppLockUtils.setRemaingTimeToUnlock();
            mHandler.sendEmptyMessage(MSG_LOCK_OUT);
        }
    }

    public int getFailAttemps() {
        return AppLockUtils.getFailedUnlockAttempts();
    }

    private void handleShowErrorMessage(String errorMessage) {
        LogUtil.d(TAG, "handleShowErrorMessage( errorMessage = " + errorMessage + " )");
        setFingerprintStatusText(errorMessage);
    }

    private void handleDismissLock() {
        try {
            LogUtil.i(TAG, "fingerprint verification is successful");
            verifySuccess();
        } catch (Exception e) {
            LogUtil.d(TAG, "Exception, fingerprint failed :" + e);
        }
    }

    private void handleShowErrorPopup(int resid) {
        LogUtil.d(TAG, "handleShowErrorPopup( resid = " + resid + " )");
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_MESSAGE);
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_POPUP);
        resetErrorMessage();
        if (mErrorDialog == null) {
            mErrorDialog = new Builder(mContext, 5)
                    .setTitle(null).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mAppLockFPThread != null) {
                                mAppLockFPThread.cleanUp();
                                mAppLockFPThread = null;
                            }
                            showSpassPasswordView();
                        }
                    }).setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (mAppLockFPThread != null) {
                                mAppLockFPThread.cleanUp();
                                mAppLockFPThread = null;
                            }
                            showSpassPasswordView();
                        }
                    }).create();
            mErrorDialog.getWindow().setType(
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (mErrorDialog.isShowing()) {
            LogUtil.d(TAG, "handleShowErrorPopup( resid = " + resid + ", Dialog is already showing. )");
            return;
        }
        mErrorDialog.setMessage(mContext.getString(resid));
        mErrorDialog.show();
    }

    private void handleShowFingerPrintInstruction(int arg) {
        if (arg == SHOW_INSTRUCTIONS_BY_TOUCH && isPreparedBackupPasswordButton()) {
            mBackupPasswordButton.setVisibility(View.VISIBLE);
        } else if (isPreparedBackupPasswordButton()) {
            mBackupPasswordButton.setVisibility(View.VISIBLE);
        }
        if (mCountdownTimer == null) {
            LogUtil.d(TAG, "handleShowFingerPrintInstruction( show finger pirnt instructions )");
            String helpText = mContext.getResources().getString(R.string.applock_fingerprints_instructions);
            if (mIsRequestToLock) {
                helpText = mContext.getResources().getString(R.string.applock_fingerprints_instructions_lock);
            }
            setFingerprintStatusText(helpText);
        }
    }

    private void handleShowBackupPassword() {
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_MESSAGE);
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_POPUP);
        resetErrorMessage();
        showSpassPasswordView();
    }

    private boolean isPreparedBackupPasswordButton() {
        return mBackupPasswordButton != null && mCountdownTimer == null
                && mBackupPasswordButton.getVisibility() != View.VISIBLE && DEBUG;
    }

    @Override
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        super.handleAttemptLockout(elapsedRealtimeDeadline);
        if (mAnimationBox != null) {
            mAnimationBox.setVisibility(View.VISIBLE);
        }
        if (mInfoImage != null) {
            mInfoImage.setImageResource(R.drawable.icon_default);
            mInfoImage.setVisibility(View.VISIBLE);
        }
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
        if (mAppLockFPThread != null) {
            mAppLockFPThread.stopSensor();
        }
        mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (secondsRemaining > 1) {
                    setFingerprintStatusText(mContext.getResources().getString(R.string.applock_too_many_failed_attempts_countdown,
                            secondsRemaining));
                    return;
                }
                setFingerprintStatusText(mContext.getResources().getString(R.string.applock_too_many_failed_attempt_countdown));
            }

            @Override
            public void onFinish() {
                AppLockUtils.resetFailedUnlockNBackupAttempts();
                mCountdownTimer = null;
                if (mAppLockFPThread != null) {
                    mAppLockFPThread.reset();
                }
                if (mAnimationBox != null) {
                    mAnimationBox.setVisibility(View.VISIBLE);
                }
                if (mInfoImage != null) {
                    mInfoImage.setImageResource(R.drawable.icon_default);
                    mInfoImage.setVisibility(View.VISIBLE);
                }
                String helpText = mContext.getResources().getString(R.string.applock_fingerprints_instructions);
                if (mIsRequestToLock) {
                    helpText = mContext.getResources().getString(R.string.applock_fingerprints_instructions_lock);
                }
                setFingerprintStatusText(helpText);
                onCountDownFinished();
            }
        }.start();
    }

    @Override
    protected void updateHelpText(boolean isFaild, String helpString) {
        if (isFaild) {
            handleShowFingerPrintInstruction(0);
        } else {
            setFingerprintStatusText(helpString);
        }
    }

    private void resetErrorMessage() {
        setFingerprintStatusText("");
    }

    @Override
    public void showErrorImage(int quality) {
        LogUtil.d(TAG, "showErrorMessage( quality = " + quality + " )");
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_IMAGE);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_IMAGE, quality));
    }

    private void handleShowErrorImage(int quality) {
        if (mInfoImage != null) {
            mInfoImage.setVisibility(View.VISIBLE);
            mInfoImage.setImageResource(getImageQualityRsrcId(quality));
        }
    }

    private int getImageQualityRsrcId(int quality) {
        switch (quality) {
            /*/ freeme.zhongkai.zhu. 20171027. fingerprint
            case 0:
                return R.drawable.error_nomatch;
            case 2:
            case  65536:
            case 524288:
                return R.drawable.error_long;
            case 512:
                return R.drawable.fingerprint_popup_water_on_seasor;
            case 4096:
                return R.drawable.error_whole;
            case 16777216:
                return R.drawable.error_wet;
            case 805306368:
                return R.drawable.error_position;
            //*/
            default:
                return R.drawable.error_default;
        }
    }

    private void showSpassPasswordView() {
        LogUtil.d(TAG, "show backup pass here");
        showBackupView();
    }
}
