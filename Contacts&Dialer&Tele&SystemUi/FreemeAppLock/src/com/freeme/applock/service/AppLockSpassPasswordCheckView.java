package com.freeme.applock.service;

import android.content.Context;
import android.os.Handler;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.R;
import com.freeme.applock.settings.LogUtil;

public class AppLockSpassPasswordCheckView extends AppLockPasswordBaseCheckView {
    public static final String TAG = "AppLockSpassPCV";

    public AppLockSpassPasswordCheckView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mLayoutInflater.inflate(R.layout.app_lock_typingverify_white, this);
        mHandler = new Handler(mContext.getMainLooper());
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
        initialTablet();
    }

    public AppLockSpassPasswordCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppLockSpassPasswordCheckView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLockSpassPasswordCheckView(Context context) {
        this(context, null);
    }

    @Override
    protected void verifyPassword() {
        if (mEtPassword.getText().toString().length() != 0) {
            LogUtil.i(TAG, "Verify password success");
            verifySuccess();
        }
    }

    @Override
    protected void resetFaildAttempts() {
        AppLockUtils.resetFailedBackupAttempts();
    }

    @Override
    void updateStage(Stage stage) {
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
