package com.freeme.applock;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings.Global;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freeme.internal.app.AppLockPolicy;
import com.android.internal.widget.LockPatternUtils;
import com.freeme.applock.AppLockSpassUnlockThread.SpassCallback;
import com.freeme.applock.settings.LogUtil;

public class AppLockSpassConfirmActivity extends AppLockConfirmActivity implements SpassCallback {
    private static final String TAG = "AppLockSpassCfmAty";
    private static final boolean DEBUG = true;
    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;
    private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";

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
    private final String PREF_ATTEMPT_DEADLINE = AppLockUtils.PREF_ATTEMPT_DEADLINE;
    private AnimationDrawable aniDrawable;
    private boolean isSupportMobileKeyboard;
    protected RelativeLayout mAnimationBox;
    private AppLockSpassUnlockThread mAppLockFPThread;
    private TextView mBackupPasswordButton;
    protected View mBouncerEMA;
    private Drawable mBouncerFrame;
    private Context mContext;
    private CountDownTimer mCountdownTimer;
    protected boolean mEMAPress;
    private View mEcaView;
    private FingerprintManager mFingerprintManager;
    private TextView mFingerprintStatusText;
    boolean mHasWindowFocus = DEBUG;
    private CountDownTimer mHelpTextCountdownTimer;
    private ImageView mInfoImage;
    private LockPatternUtils mLockPatternUtils;
    private boolean mMobileKeyboard;
    private int mNumWrongConfirmAttempts;
    private long mResumedTimeMillis = System.currentTimeMillis();
    private TextView mStatusText;
    private boolean mUseBlackTextOnWhiteWallpaper;
    private ContentObserver mWhiteWallpaperObserver;

    private Handler mHandler = new Handler(Looper.myLooper(), null, DEBUG) {
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
                case DISMISS_LOCK /*17*/:
                    handleDismissLock();
                    break;
                case SHOW_FINGERPRINT_ERROR_IMAGE /*19*/:
                    handleShowErrorImage((Integer) msg.obj);
                    break;
                case MSG_LOCK_OUT /*20*/:
                    handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_lock_spass_identify);
        isAppVerifying();
        mContext = this;
        mLockPatternUtils = new LockPatternUtils(mContext);
        mAppLockFPThread = new AppLockSpassUnlockThread(mContext);
        mAppLockFPThread.start();
        mUseBlackTextOnWhiteWallpaper = Global.getInt(mContext.getContentResolver(), "white_lockscreen_wallpaper", 0) == 1;
        initFingerprintManager();
        mBackupPasswordButton = (TextView) findViewById(R.id.text_backup_password);
        if (mBackupPasswordButton != null) {
            mBackupPasswordButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSpassPasswordActivity();
                }
            });
        }
        mFingerprintStatusText = (TextView) findViewById(R.id.applock_fingerprint_identify_info_text);
        mAnimationBox = (RelativeLayout) findViewById(R.id.identify_animation_box);
        mInfoImage = (ImageView) findViewById(R.id.identify_error_image);
        mAppLockFPThread.setSpassCallback(this);
    }

    protected void setAnimationLayout() {
        if (mAnimationBox != null) {
            mAnimationBox.setVisibility(View.VISIBLE);
        }
        if (mInfoImage != null) {
            mInfoImage.setImageResource(R.drawable.icon_default);
        }
        aniDrawable = new AnimationDrawable();
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
            }
            return;
        }
        LogUtil.d(TAG, "onWindowFocusChanged - loosing focus");
        if (mAppLockFPThread != null) {
            mAppLockFPThread.stopSensor();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume()");
        doOnResume();
    }

    private void doOnResume() {
        mNumWrongConfirmAttempts = AppLockUtils.getFailedUnlockAttempts();
        long deadline = AppLockUtils.getRemaingTimeToUnlock();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            if (mAppLockFPThread != null) {
                mAppLockFPThread.onResume();
            }
            mHandler.removeMessages(SHOW_FINGERPRINT_INSTRUCTIONS);
            mHandler.sendEmptyMessageDelayed(SHOW_FINGERPRINT_INSTRUCTIONS, 200);
            setAnimationLayout();
        }
        mResumedTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void onPause() {
        LogUtil.d(TAG, "onPause()");
        doOnPause();
        super.onPause();
    }

    private void doOnPause() {
        mHandler.removeMessages(SHOW_FINGERPRINT_INSTRUCTIONS);
        if (mAppLockFPThread != null) {
            mAppLockFPThread.onPause();
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
    public void requestOnPauseAciton() {
        doOnPause();
        super.requestOnPauseAciton();
    }

    @Override
    public void requestOnResumeAciton() {
        super.requestOnResumeAciton();
        doOnResume();
    }

    @Override
    protected void onDestroy() {
        if (mAppLockFPThread != null) {
            mAppLockFPThread.cleanUp();
            mAppLockFPThread = null;
        }
        super.onDestroy();
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
        if (mFingerprintManager == null) {
            mFingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        }
        if (mFingerprintManager == null || !mFingerprintManager.hasEnrolledFingerprints()) {
            showSpassPasswordActivity();
        }
    }

    private void setFingerprintStatusText(String string) {
        if (mFingerprintStatusText != null) {
            mFingerprintStatusText.setText(string);
            mFingerprintStatusText.announceForAccessibility(string);
        }
    }

    @Override
    protected void updateHelpText(boolean isFaild, String helpString) {
        if (isFaild) {
            handleShowFingerPrintInstruction(0);
        } else {
            setFingerprintStatusText(helpString);
        }
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
            mErrorDialog = new Builder(mContext, 5).setTitle(null).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showSpassPasswordActivity();
                }
            }).setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    showSpassPasswordActivity();
                }
            }).create();
            mErrorDialog.getWindow().setType(2009);
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

    private void handleShowBackupPasswordButton() {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (mBackupPasswordButton != null && mBackupPasswordButton.getVisibility() != 0 && powerManager.isScreenOn()) {
            LogUtil.d(TAG, "fade in backupView.");
            resetErrorMessage();
            mBackupPasswordButton.setVisibility(View.VISIBLE);
        }
    }

    private void handleShowBackupPassword() {
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_MESSAGE);
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_POPUP);
        resetErrorMessage();
        showSpassPasswordActivity();
    }

    private boolean isPreparedBackupPasswordButton() {
        return mBackupPasswordButton != null && mCountdownTimer == null && mBackupPasswordButton.getVisibility() != View.VISIBLE;
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
                doOnResume();
                onCountDownFinished();
            }
        }.start();
    }

    private void resetErrorMessage() {
        setFingerprintStatusText("");
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
            case 65536:
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

    private void showSpassPasswordActivity() {
        Intent intent = new Intent();
        if (mIntent != null) {
            intent.putExtra(AppLockPolicy.LOCKED_PACKAGE_INTENT, mIntent);
        }
        intent.putExtra(AppLockPolicy.LOCKED_PACKAGE_NAME, mPkgName);
        intent.putExtra(AppLockPolicy.LAUNCH_FROM_RESUME, mIsFromResume);
        intent.putExtra(AppLockPolicy.LAUNCH_FROM_SETTINGS, mIsStartFromAppLockSettings);
        String fingprintAction = getIntent().getAction();
        if (AppLockPolicy.ACTION_CHECK_APPLOCK_FINGERPRINT_PATTERN.equals(fingprintAction)) {
            intent.setClass(mContext, AppLockPatternConfirmActivity.class);
        } else if (AppLockPolicy.ACTION_CHECK_APPLOCK_FINGERPRINT_PINCODE.equals(fingprintAction)) {
            intent.setClass(mContext, AppLockPinConfirmActivity.class);
        } else if (AppLockPolicy.ACTION_CHECK_APPLOCK_FINGERPRINT_PASSWORD.equals(fingprintAction)) {
            intent.setClass(mContext, AppLockPasswordConfirmActivity.class);
        } else {
            intent.setClass(mContext, AppLockSpassPasswordConfirmActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void dismissLock() {
        LogUtil.d(TAG, "dismissLock()");
        mHandler.removeMessages(DISMISS_LOCK);
        mHandler.sendMessage(mHandler.obtainMessage(DISMISS_LOCK));
    }

    @Override
    public int getFailAttemps() {
        return AppLockUtils.getFailedUnlockAttempts();
    }

    @Override
    public void reportFailAttemps() {
        AppLockUtils.reportFailedUnlockAttempts();
        if (AppLockUtils.getFailedUnlockAttempts() % 5 == 0) {
            AppLockUtils.setRemaingTimeToUnlock();
            mHandler.sendEmptyMessage(MSG_LOCK_OUT);
        }
    }

    @Override
    public void showBackupPassword() {
        mHandler.removeMessages(SHOW_BACKUP_PASSWORD);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_BACKUP_PASSWORD));
    }

    @Override
    public void showErrorImage(int quality) {
        LogUtil.d(TAG, "showErrorMessage( quality = " + quality + " )");
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_IMAGE);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_IMAGE, quality));
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        LogUtil.d(TAG, "showErrorMessage( errorMessage = " + errorMessage + " )");
        mHandler.removeMessages(11);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_MESSAGE, errorMessage));
    }

    @Override
    public void showErrorPopup(int resid) {
        LogUtil.d(TAG, "showErrorPopup( resid = " + resid + " )");
        mHandler.removeMessages(SHOW_FINGERPRINT_ERROR_POPUP);
        mHandler.sendMessage(mHandler.obtainMessage(SHOW_FINGERPRINT_ERROR_POPUP, resid, 0));
    }
}
