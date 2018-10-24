package com.freeme.applock.service;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.AppLockType;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;
import com.android.internal.widget.LockPatternView.OnPatternListener;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.R;
import com.freeme.applock.settings.AppLockUtil;
import java.util.List;
import com.freeme.applock.settings.LogUtil;

public class AppLockPatternCheckView extends AppLockCheckBaseView {
    private static final String TAG = "AppLockPatternCV";
    private static final String EXTRA_KEY_PASSWORD = "password";
    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;
    public static final String FOOTER_TEXT = "com.android.settings.applock.AppLockPattern.footer";
    public static final String FOOTER_WRONG_TEXT = "com.android.settings.applock.AppLockPattern.footer_wrong";
    public static final String HEADER_TEXT = "com.android.settings.applock.AppLockPattern.header";
    public static final String HEADER_WRONG_TEXT = "com.android.settings.applock.AppLockPattern.header_wrong";
    public static final String PACKAGE = "com.android.settings.applock";
    private static final int RIGHT_MARGIN_WITHOUT_BACKUP_PIN = 14;
    private static final int RIGHT_MARGIN_WITH_BACKUP_PIN = 86;

    private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;
    private final String PREF_ATTEMPT_DEADLINE;
    private ImageButton imgbtn;
    private ImageView imgview;
    private int isPatternimage;
    private boolean isTablet;
    private Runnable mClearPatternRunnable;
    private Context mContext;
    private CountDownTimer mCountdownTimer;
    private CharSequence mHeaderText;
    private CharSequence mHeaderWrongText;
    private TextView mInfoTextView;
    private boolean mIsPatternVisible;
    private LayoutInflater mLayoutInflater;
    private LockPatternView mLockPatternView;
    private OnPatternListener mSecretLockPatternListener;

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    public AppLockPatternCheckView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mLayoutInflater = null;
        mIsPatternVisible = true;
        isPatternimage = 0;
        PREF_ATTEMPT_DEADLINE = AppLockUtils.PREF_ATTEMPT_DEADLINE;
        isTablet = false;
        mSecretLockPatternListener = new OnPatternListener() {
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
                        LogUtil.i(AppLockPatternCheckView.TAG, "pattern verification is successful");
                        verifySuccess();
                        return;
                    } catch (Exception e) {
                        LogUtil.d(AppLockPatternCheckView.TAG, "Exception, mode change is fail" + e);
                        return;
                    }
                }
                AppLockUtils.reportFailedUnlockAttempts();
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
        mClearPatternRunnable = new Runnable() {
            @Override
            public void run() {
                mLockPatternView.clearPattern();
            }
        };
        mContext = context;
        mIsPatternVisible = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(AppLockUtil.KEY_SECURE_LOCK_SETTINGS_PATTERN_VISIBLE_SWITCH, true);
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutInflater.inflate(R.layout.app_lock_pattern_white, this);
        imgbtn = (ImageButton) findViewById(R.id.patternimageButtonPW);
        if (imgbtn != null) {
            imgbtn.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showBackupView();
                }
            });
            imgbtn.setVisibility(View.GONE);
        }
        InitPatternview();
        if (imgbtn != null) {
            imgbtn.setVisibility(View.GONE);
        }
        isPatternimage = getResources().getIdentifier("patternimageViewPW", "drawable", mContext.getPackageName());
        if (isPatternimage != 0) {
            mInfoTextView.setRight(14);
        }
        if (imgview != null) {
            imgview.setVisibility(View.GONE);
        }
    }

    public AppLockPatternCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppLockPatternCheckView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLockPatternCheckView(Context context) {
        this(context, null);
    }

    @Override
    protected void onAttachedToWindow() {
        LogUtil.d(TAG, "onAttachedToWindow");
        super.onAttachedToWindow();
        long deadline = AppLockUtils.getRemaingTimeToUnlock();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else if (mLockPatternView != null && !mLockPatternView.isEnabled()) {
            AppLockUtils.resetFailedUnlockNBackupAttempts();
            updateStage(Stage.NeedToUnlock);
            mLockPatternView.clearPattern();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        LogUtil.d(TAG, "onDetachedFromWindow");
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
        }
        super.onDetachedFromWindow();
    }

    private void InitPatternview() {
        LogUtil.i(TAG, "InitPatternview()");
        mLockPatternView = (LockPatternView) findViewById(R.id.secretPattern);
        mInfoTextView = (TextView) findViewById(R.id.patternTitleText);
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
        mLockPatternView.setOnPatternListener(mSecretLockPatternListener);
        if (!mIsPatternVisible) {
            mLockPatternView.setInStealthMode(true);
        }
        updateStage(Stage.NeedToUnlock);
    }

    private int getPxfromDp(double dp) {
        return (int) ((((double) getResources().getDisplayMetrics().density) * dp) + 0.5d);
    }

    private void changePadding(boolean lockState) {
        if (!isTablet && isPortrait()) {
            if (!lockState) {
                mInfoTextView.setPadding(0, 0, 0, 0);
            } else if (isRTL()) {
                mInfoTextView.setPadding(getPxfromDp(68.0d), 0, getPxfromDp(18.0d), 0);
            } else {
                mInfoTextView.setPadding(getPxfromDp(18.0d), 0, getPxfromDp(68.0d), 0);
            }
        }
    }

    private boolean isRTL() {
        if (getResources().getConfiguration().getLayoutDirection() == 1) {
            return true;
        }
        return false;
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
        if (isPatternimage != 0) {
            mInfoTextView.setRight(RIGHT_MARGIN_WITH_BACKUP_PIN);
        }
        updateStage(Stage.LockedOut);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if (secondsCountdown == 1) {
                        mInfoTextView.setText(mContext.getString(R.string.lockpattern_too_many_failed_confirmation_attempt_footer));
                        return;
                    }
                    mInfoTextView.setText(mContext.getString(R.string.lockpattern_too_many_failed_confirmation_attempts_footer,
                            secondsCountdown));
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
