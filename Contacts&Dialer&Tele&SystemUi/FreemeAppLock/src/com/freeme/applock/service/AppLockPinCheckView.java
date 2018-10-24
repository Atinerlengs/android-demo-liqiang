package com.freeme.applock.service;

import android.content.Context;
import android.os.UserHandle;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils.AppLockType;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.R;
import com.freeme.applock.settings.LogUtil;

public class AppLockPinCheckView extends AppLockPasswordBaseCheckView {
    public static final String TAG = "AppLockPinCheckView";

    public AppLockPinCheckView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mLayoutInflater.inflate(R.layout.app_lock_pin_white, this);
        mPasswordText = (TextView) findViewById(R.id.typingverifyTitleText);
        setHelpText(mPasswordText);
        mEtPassword = (EditText) findViewById(R.id.typingverifyEditText);
        mEtPassword.setInputType(18);
        mEtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mEtPassword.requestFocus();
        initialTablet();
    }

    public AppLockPinCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppLockPinCheckView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLockPinCheckView(Context context) {
        this(context, null);
    }

    @Override
    protected void verifyPassword() {
        String entry = mEtPassword.getText().toString();
        if (entry.length() != 0) {
            if (mLockPatternUtils.checkAppLockPassword(entry, AppLockType.PIN, UserHandle.myUserId())) {
                LogUtil.i(TAG, "Verify PIN success");
                verifySuccess();
            } else {
                if (AppLockUtils.getFailedUnlockAttempts() == 5) {
                    resetFaildAttempts();
                }
                LogUtil.i(TAG, "Verify PIN fail");
                AppLockUtils.reportFailedUnlockAttempts();
                if (AppLockUtils.getFailedUnlockAttempts() >= 5) {
                    AppLockUtils.setRemaingTimeToUnlock();
                    handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
                } else {
                    updateStage(Stage.NeedToUnlockWrong);
                }
            }
        }
    }

    private void setHelpText(TextView helpTextView) {
        if (helpTextView != null) {
            if (mIsShouldStartBiometrics) {
                helpTextView.setText(R.string.applock_biometrics_pin_instructions);
            } else if (mIsShouldStartIris) {
                helpTextView.setText(R.string.applock_iris_pin_instructions);
            } else if (mIsShouldStartFingerprint) {
                helpTextView.setText(R.string.applock_fingerprints_pin_instructions);
            } else if (mIsRequestToLock) {
                helpTextView.setText(R.string.applock_pin_help_text_lock);
            } else {
                helpTextView.setText(R.string.applock_pin_help_text);
            }
        }
    }

    @Override
    protected void updateHelpText(boolean isFailed, String helpString) {
        if (isFailed) {
            setHelpText(mPasswordText);
        } else {
            mPasswordText.setText(helpString);
        }
    }

    @Override
    void updateStage(Stage stage) {
        switch (stage) {
            case LockedOut:
                mEtPassword.setText("");
                mEtPassword.setFilters(blockInputFilter);
                break;
            case NeedToUnlock:
                setHelpText(mPasswordText);
                break;
            case NeedToUnlockWrong:
                mPasswordText.setText(R.string.pin_help_text_try_again);
                mEtPassword.setText("");
                break;
            default:
                break;
        }
    }
}