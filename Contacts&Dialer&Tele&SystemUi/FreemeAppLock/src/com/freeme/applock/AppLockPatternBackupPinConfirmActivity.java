package com.freeme.applock;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.widget.LockPatternUtils.AppLockType;

import com.freeme.applock.settings.LogUtil;

public class AppLockPatternBackupPinConfirmActivity extends AppLockConfirmActivity {
    public static final String TAG = "AppLockPtnBpPinCfmAty";

    private static final InputFilter[] blockInputFilter = new InputFilter[]{new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            return "";
        }
    }};
    private static final InputFilter[] normalInputFilter = new InputFilter[0];
    private final String BACKPIN_INPUT_TEXT = "backpin_input_text";
    private long lock_out_time;
    private Context mContext;
    private CountDownTimer mCountdownTimer;
    private EditText mEtPassword;
    private TextView mPasswordText;

    @Override
    public void onCreate(Bundle savedInstanceStates) {
        super.onCreate(savedInstanceStates);
        mContext = this;
        setContentView(R.layout.app_lock_pin_white);
        mPasswordText = (TextView) findViewById(R.id.typingverifyTitleText);
        mPasswordText.setText(R.string.pattern_backuppin);
        mEtPassword = (EditText) findViewById(R.id.typingverifyEditText);
        mEtPassword.setInputType((InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_CLASS_NUMBER));
        mEtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mEtPassword.requestFocus();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        mEtPassword.setText(savedState.getCharSequence(BACKPIN_INPUT_TEXT));
        mEtPassword.setSelection(mEtPassword.length());
        super.onRestoreInstanceState(savedState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(BACKPIN_INPUT_TEXT, mEtPassword.getText());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        LogUtil.i(TAG, "destroy() : " + this);
        mLockPatternUtils = null;
        mContext = null;
        super.onDestroy();
    }

    protected void verifyPatternBackupPin() {
        String entry = mEtPassword.getText().toString();
        if (entry.length() != 0) {
            if (mLockPatternUtils == null || !mLockPatternUtils.checkAppLockPassword(entry,
                    AppLockType.BackupPin, UserHandle.myUserId())) {
                LogUtil.i(TAG, "Verify Backup PIN fail");
                AppLockUtils.reportFailedBackupAttempts();
                if (AppLockUtils.getFailedBackupAttempts() >= 5) {
                    AppLockUtils.setRemaingTimeToUnlock();
                    handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
                } else {
                    mPasswordText = (TextView) findViewById(R.id.typingverifyTitleText);
                    mPasswordText.setText(mContext.getResources().getString(R.string.incorrect_backup_pin));
                    mEtPassword = (EditText) findViewById(R.id.typingverifyEditText);
                    mEtPassword.setText("");
                }
            } else {
                LogUtil.i(TAG, "Verify Backup PIN success");
                verifySuccess();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        lock_out_time = AppLockUtils.getRemaingTimeToUnlock();
        if (lock_out_time != 0) {
            handleAttemptLockout(lock_out_time);
        }
        if (mEtPassword != null) {
            mEtPassword.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    LogUtil.i(TAG, "onEditorAction, actionID =" + actionId);
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        verifyPatternBackupPin();
                    } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        verifyPatternBackupPin();
                    }
                    return true;
                }
            });
            mEtPassword.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
                        return false;
                    }
                    verifyPatternBackupPin();
                    return true;
                }
            });
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (hasFocus && mEtPassword != null && inputManager != null) {
            mEtPassword.requestFocus();
            inputManager.showSoftInput(mEtPassword, 0);
        }
    }

    @Override
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        super.handleAttemptLockout(elapsedRealtimeDeadline);
        mEtPassword.setText("");
        mEtPassword.setFilters(blockInputFilter);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if (secondsCountdown == 1) {
                        mPasswordText.setText(getString(R.string.lockpattern_too_many_failed_confirmation_attempt_footer));
                        return;
                    }
                    mPasswordText.setText(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts_footer, secondsCountdown));
                }

                @Override
                public void onFinish() {
                    mPasswordText.setText(R.string.pattern_backuppin);
                    mEtPassword.setFilters(AppLockPatternBackupPinConfirmActivity.normalInputFilter);
                    AppLockUtils.resetFailedUnlockNBackupAttempts();
                    AppLockUtils.resetRemaingTimeToUnlock();
                    mCountdownTimer = null;
                    onCountDownFinished();
                }
            }.start();
        }
    }
}
