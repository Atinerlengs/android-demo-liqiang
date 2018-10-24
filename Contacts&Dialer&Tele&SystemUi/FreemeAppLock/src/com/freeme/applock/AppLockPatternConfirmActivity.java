package com.freeme.applock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.AppLockType;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import com.freeme.applock.settings.AppLockUtil;
import com.freeme.applock.settings.LogUtil;
import com.freeme.internal.app.AppLockPolicy;

import java.util.List;

public class AppLockPatternConfirmActivity extends AppLockConfirmActivity {
    private static final String TAG = "AppLockPatternCfmAty";
    public static final String HEADER_TEXT = "com.android.settings.applock.AppLockPattern.header";
    public static final String HEADER_WRONG_TEXT = "com.android.settings.applock.AppLockPattern.header_wrong";
    private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";

    private ImageButton imgbtn;
    private ImageView imgview;
    private int isPatternimage;
    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };
    private Context mContext;
    private CountDownTimer mCountdownTimer;
    private CharSequence mHeaderText;
    private CharSequence mHeaderWrongText;
    private TextView mInfoTextView;
    private boolean mIsPatternVisible = true;
    boolean mIsVerificationMode = false;
    private LockPatternView mLockPatternView;
    private int mNumWrongConfirmAttempts;
    private OnPatternListener mSecretLockPatternListener = new OnPatternListener() {
        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
        }

        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
        }

        @Override
        public void onPatternCellAdded(List<Cell> list) {
        }

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            if (mLockPatternUtils.checkAppLockPassword(LockPatternUtils.patternToString(pattern),
                    AppLockType.Pattern, UserHandle.myUserId())) {
                try {
                    LogUtil.i(TAG, "pattern verification is successful");
                    verifySuccess();
                    return;
                } catch (Exception e) {
                    LogUtil.d(TAG, "Exception, mode change is fail" + e);
                    return;
                }
            }
            if (pattern.size() < 4 || AppLockUtils.getFailedUnlockAttempts() < 5) {
                if (pattern.size() >= 4) {
                    AppLockUtils.reportFailedUnlockAttempts();
                }

                updateStage(Stage.NeedToUnlockWrong);
                postClearPatternRunnable();
                return;
            }
            AppLockUtils.setRemaingTimeToUnlock();
            handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
        }
    };

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAppVerifying();
        mContext = this;
        mIsPatternVisible = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(AppLockUtil.KEY_SECURE_LOCK_SETTINGS_PATTERN_VISIBLE_SWITCH, true);
        super.setTheme(R.style.PatternUnlockTheme);
        setContentView(R.layout.app_lock_pattern_white);
        imgbtn = (ImageButton) findViewById(R.id.patternimageButtonPW);
        if (imgbtn != null) {
            imgbtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent();
                    if (mIntent != null) {
                        intent.putExtra(AppLockPolicy.LOCKED_PACKAGE_INTENT, mIntent);
                    }
                    intent.putExtra(AppLockPolicy.LOCKED_PACKAGE_NAME, mPkgName);
                    intent.putExtra(AppLockPolicy.LAUNCH_FROM_RESUME, mIsFromResume);
                    intent.putExtra(AppLockPolicy.LAUNCH_FROM_SETTINGS, mIsStartFromAppLockSettings);
                    intent.setClass(mContext, AppLockPatternBackupPinConfirmActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                    startActivity(intent);
                    finish();
                }
            });
            imgbtn.setVisibility(View.GONE);
        }
        if (getIntent() != null) {
            mIsVerificationMode = true;
        }
        InitPatternview(savedInstanceState);
        if (imgbtn != null) {
            imgbtn.setVisibility(View.GONE);
        }
        isPatternimage = getResources().getIdentifier("patternimageViewPW", "drawable", getPackageName());
        if (isPatternimage != 0) {
            mInfoTextView.setRight(14);
        }
        if (imgview != null) {
            imgview.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mLockPatternUtils.savedAppLockPasswordExists(AppLockType.Pattern, UserHandle.myUserId())) {
            setResult(-1);
            finish();
        }
        long deadline = AppLockUtils.getRemaingTimeToUnlock();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else if (mLockPatternView != null && !mLockPatternView.isEnabled()) {
            AppLockUtils.resetFailedUnlockNBackupAttempts();
            updateStage(Stage.NeedToUnlock);
            mLockPatternView.clearPattern();
        }
    }

    private void InitPatternview(Bundle savedInstanceState) {
        LogUtil.i(TAG, "InitPatternview()");
        mLockPatternView = (LockPatternView) findViewById(R.id.secretPattern);
        mInfoTextView = (TextView) findViewById(R.id.patternTitleText);
        Intent intent = getIntent();
        if (intent != null) {
            mHeaderText = intent.getCharSequenceExtra(HEADER_TEXT);
            mHeaderWrongText = intent.getCharSequenceExtra(HEADER_WRONG_TEXT);
        }
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
        mLockPatternView.setOnPatternListener(mSecretLockPatternListener);
        if (!mIsPatternVisible) {
            mLockPatternView.setInStealthMode(true);
        }
        updateStage(Stage.NeedToUnlock);
    }

    @Override
    protected void onDestroy() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
        super.onDestroy();
    }

    @Override
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        super.handleAttemptLockout(elapsedRealtimeDeadline);
        imgbtn = (ImageButton) findViewById(R.id.patternimageButtonPW);
        if (AppLockUtils.isSupportPatternBackupPin()) {
            imgbtn.setVisibility(View.VISIBLE);
        } else {
            imgbtn.setVisibility(View.GONE);
        }
        updateStage(Stage.LockedOut);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if (secondsCountdown == 1) {
                        mInfoTextView.setText(getString(R.string.lockpattern_too_many_failed_confirmation_attempt_footer));
                        return;
                    }
                    mInfoTextView.setText(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts_footer, secondsCountdown));
                }

                @Override
                public void onFinish() {
                    AppLockUtils.resetFailedUnlockNBackupAttempts();
                    updateStage(Stage.NeedToUnlock);
                    mCountdownTimer = null;
                    if (isPatternimage != 0) {
                        imgview.setVisibility(View.GONE);
                    }
                    onCountDownFinished();
                }
            }.start();
        }
    }

    private void postClearPatternRunnable() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, 2000);
    }

    private void setHelpText(TextView helpTextView) {
        if (helpTextView != null) {
            if (mHeaderText != null) {
                helpTextView.setText(mHeaderText);
            } else if (mIsShouldStartBiometrics) {
                helpTextView.setText(R.string.applock_biometrics_pattern_instructions);
            } else if (mIsShouldStartIris) {
                helpTextView.setText(R.string.applock_iris_pattern_instructions);
            } else if (mIsShouldStartFingerprint) {
                helpTextView.setText(R.string.applock_fingerprints_pattern_instructions);
            } else if (mIsRequestToLock) {
                helpTextView.setText(R.string.applock_pattern_help_text_lock);
            } else {
                helpTextView.setText(R.string.applock_pattern_help_text);
            }
        }
    }

    @Override
    protected void updateHelpText(boolean isFailed, String helpString) {
        if (isFailed) {
            setHelpText(mInfoTextView);
        } else {
            mInfoTextView.setText(helpString);
        }
    }

    private void updateStage(Stage stage) {
        switch (stage) {
            case LockedOut:
                mLockPatternView.clearPattern();
                mLockPatternView.setEnabled(false);
                break;
            case NeedToUnlock:
                setHelpText(mInfoTextView);
                mLockPatternView.setEnabled(true);
                mLockPatternView.enableInput();
                break;
            case NeedToUnlockWrong:
                if (mHeaderWrongText != null) {
                    mInfoTextView.setText(mHeaderWrongText);
                } else {
                    mInfoTextView.setText(R.string.pattern_help_text_try_again);
                }
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                mLockPatternView.setEnabled(true);
                mLockPatternView.enableInput();
                break;
            default:
                break;
        }
    }
}
