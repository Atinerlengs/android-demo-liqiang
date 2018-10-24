package com.freeme.applock.service;

import android.content.Context;
import android.content.res.Configuration;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.R;
import com.freeme.applock.settings.AppLockUtil;
import com.freeme.applock.settings.LogUtil;

public abstract class AppLockPasswordBaseCheckView extends AppLockCheckBaseView {
    public static final String TAG = "AppLockPBCV";

    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;
    protected static final InputFilter[] blockInputFilter = new InputFilter[]{ new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            return "";
        }
    }};
    private static final InputFilter[] normalInputFilter = new InputFilter[0];
    protected CountDownTimer mCountdownTimer;
    protected EditText mEtPassword;
    protected Handler mHandler;
    protected TextView mPasswordText;
    private Runnable mSIPRunnable;

    protected enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    abstract void updateStage(Stage stage);

    protected abstract void verifyPassword();

    public AppLockPasswordBaseCheckView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHandler = new Handler(mContext.getMainLooper());
    }

    public AppLockPasswordBaseCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppLockPasswordBaseCheckView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLockPasswordBaseCheckView(Context context) {
        this(context, null);
    }

    protected void initialTablet() {
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        mCallback.onSavePSWForRotation(mEtPassword.getText().toString());
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        long deadline = AppLockUtils.getRemaingTimeToUnlock();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        }
        if (mEtPassword != null) {
            mEtPassword.setText(mEditTextPSW);
            mEtPassword.setSelection(mEtPassword.length());
            mEtPassword.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    LogUtil.i(TAG, "onEditorAction, actionID =" + actionId);
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        verifyPassword();
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        verifyPassword();
                    } else if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        verifyPassword();
                    }
                    return true;
                }
            });
            mEtPassword.setOnKeyListener(this);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
            return super.onKey(v, keyCode, event);
        }
        verifyPassword();
        return true;
    }

    @Override
    protected void onDestroy() {
        if (mSIPRunnable != null && mHandler != null) {
            mHandler.removeCallbacks(mSIPRunnable);
        }

        AppLockUtil.hideKeyboard(mEtPassword);

        super.onDestroy();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (mSIPRunnable != null && mHandler != null) {
                mHandler.removeCallbacks(mSIPRunnable);
            }
            mSIPRunnable = new Runnable() {
                @Override
                public void run() {
                    AppLockUtil.showKeyboard(mEtPassword);
                }
            };
            if (mHandler != null) {
                mHandler.postDelayed(mSIPRunnable, 50);
            }
        }
    }

    @Override
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        super.handleAttemptLockout(elapsedRealtimeDeadline);
        updateStage(Stage.LockedOut);
        if (mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if (secondsCountdown == 1) {
                        mPasswordText.setText(mContext.getString(R.string.lockpattern_too_many_failed_confirmation_attempt_footer));
                        return;
                    }
                    mPasswordText.setText(mContext.getString(R.string.lockpattern_too_many_failed_confirmation_attempts_footer,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    resetFaildAttempts();
                    updateStage(Stage.NeedToUnlock);
                    mEtPassword.setFilters(AppLockPasswordBaseCheckView.normalInputFilter);
                    mCountdownTimer = null;
                    onCountDownFinished();
                }
            }.start();
        }
    }

    protected void resetFaildAttempts() {
        AppLockUtils.resetFailedUnlockNBackupAttempts();
    }
}
