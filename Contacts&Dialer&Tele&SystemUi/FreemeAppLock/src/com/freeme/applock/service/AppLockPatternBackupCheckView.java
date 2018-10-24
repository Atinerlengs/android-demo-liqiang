package com.freeme.applock.service;

import android.content.Context;
import android.os.UserHandle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils.AppLockType;
import com.freeme.applock.AppLockUtils;
import com.freeme.applock.R;
import com.freeme.applock.settings.LogUtil;

public class AppLockPatternBackupCheckView extends AppLockPasswordBaseCheckView {
    public static final String TAG = "AppLockPatternBCW";

    public AppLockPatternBackupCheckView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mLayoutInflater.inflate(R.layout.app_lock_pin_white, this);
        mPasswordText = (TextView) findViewById(R.id.typingverifyTitleText);
        mPasswordText.setText(R.string.pattern_backuppin);
        mEtPassword = (EditText) findViewById(R.id.typingverifyEditText);
        mEtPassword.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD | InputType.TYPE_CLASS_NUMBER);
        mEtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mEtPassword.requestFocus();
        initialTablet();
    }

    public AppLockPatternBackupCheckView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AppLockPatternBackupCheckView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppLockPatternBackupCheckView(Context context) {
        this(context, null);
    }

    protected void verifyPassword() {
        String entry = mEtPassword.getText().toString();
        if (entry.length() != 0) {
            if (mLockPatternUtils == null || !mLockPatternUtils.checkAppLockPassword(entry,
                    AppLockType.BackupPin, UserHandle.myUserId())) {
                if (AppLockUtils.getFailedBackupAttempts() == 5) {
                    resetFaildAttempts();
                }
                LogUtil.i(TAG, "Verify Backup PIN fail");
                AppLockUtils.reportFailedBackupAttempts();
                if (AppLockUtils.getFailedBackupAttempts() >= 5) {
                    AppLockUtils.setRemaingTimeToUnlock();
                    handleAttemptLockout(AppLockUtils.getRemaingTimeToUnlock());
                } else {
                    updateStage(Stage.NeedToUnlockWrong);
                }
            } else {
                LogUtil.i(TAG, "Verify Backup PIN success");
                verifySuccess();
            }
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
                mPasswordText.setText(R.string.pattern_backuppin);
                break;
            case NeedToUnlockWrong:
                mPasswordText.setText(R.string.incorrect_backup_pin);
                mEtPassword.setText("");
                break;
            default:
                break;
        }
    }
}
