package com.freeme.applock;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.freeme.applock.settings.LogUtil;

public class AppLockSpassPasswordConfirmActivity extends AppLockConfirmActivity {
    public static final String TAG = "AppLockSpassPwdCfmAty";
    public static final long FAILED_ATTEMPT_TIMEOUT_MS = 30000;
    private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts_passwd";
    private static final String PASSWORD_INPUT_TEXT = "password_input_text";
    private static final InputFilter[] blockInputFilter = new InputFilter[]{new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            return "";
        }
    }};
    private static final InputFilter[] normalInputFilter = new InputFilter[0];
    private final String PREF_ATTEMPT_DEADLINE = "pref_attempt_deadline_passwd";
    private Context mContext;
    private CountDownTimer mCountdownTimer;
    private EditText mEtPassword;
    private int mNumWrongConfirmAttempts;
    private TextView mPasswordText;

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAppVerifying();
        mContext = this;
        setContentView(R.layout.app_lock_typingverify_white);
        if (savedInstanceState != null) {
            mNumWrongConfirmAttempts = savedInstanceState.getInt(KEY_NUM_WRONG_ATTEMPTS);
        }
        mPasswordText = (TextView) findViewById(R.id.typingverifyTitleText);
        if (AppLockUtils.getFailedUnlockAttempts() >= 5) {
            mPasswordText.setText(R.string.spass_password_to_use_fingerprint);
        } else {
            mPasswordText.setText(R.string.spass_backup_password_guide);
        }
        mEtPassword = (EditText) findViewById(R.id.typingverifyEditText);
        mEtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT);
        mEtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mEtPassword.requestFocus();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        mEtPassword.setText(savedState.getCharSequence(PASSWORD_INPUT_TEXT));
        mEtPassword.setSelection(mEtPassword.length());
        super.onRestoreInstanceState(savedState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
        outState.putCharSequence(PASSWORD_INPUT_TEXT, mEtPassword.getText());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "destroy() : " + this);
        mLockPatternUtils = null;
        mContext = null;
        super.onDestroy();
    }

    protected void verifyPassword() {
        if (mEtPassword.getText().toString().length() != 0) {
            LogUtil.i(TAG, "Verify password success");
            verifySuccess();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        long deadline = AppLockUtils.getRemaingTimeToUnlock();
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        }
        if (mEtPassword != null) {
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
            mEtPassword.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
                        return false;
                    }
                    verifyPassword();
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
        updateStage(Stage.LockedOut);
        if (mCountdownTimer == null) {
            mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if (secondsCountdown == 1) {
                        mPasswordText.setText(getString(R.string.lockpattern_too_many_failed_confirmation_attempt_footer));
                        return;
                    }
                    mPasswordText.setText(getString(R.string.lockpattern_too_many_failed_confirmation_attempts_footer,
                            secondsCountdown));
                }

                @Override
                public void onFinish() {
                    AppLockUtils.resetFailedBackupAttempts();
                    updateStage(Stage.NeedToUnlock);
                    mEtPassword.setFilters(AppLockSpassPasswordConfirmActivity.normalInputFilter);
                    mCountdownTimer = null;
                    onCountDownFinished();
                }
            }.start();
        }
    }

    private void updateStage(Stage stage) {
        switch (stage) {
            case LockedOut:
                mEtPassword.setText("");
                mEtPassword.setFilters(blockInputFilter);
                break;
            case NeedToUnlock:
                mPasswordText.setText(R.string.spass_backup_password_guide);
                break;
            case NeedToUnlockWrong:
                mPasswordText.setText(R.string.spass_backup_wrong_password);
                mEtPassword.setText("");
                break;
            default:
                break;
        }
    }
}
