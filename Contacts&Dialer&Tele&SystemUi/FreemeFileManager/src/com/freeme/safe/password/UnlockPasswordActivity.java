package com.freeme.safe.password;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.filemanager.R;
import com.freeme.safe.encryption.thread.ModifyPasswordThread;
import com.freeme.safe.encryption.thread.ModifyPasswordThread.OnModifyPasswordListener;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import static com.freeme.safe.password.InputPasswordData.PASSWORD_NUM_LEN;

public class UnlockPasswordActivity extends FingerPrintActivity implements TextWatcher, OnClickListener {

    private Context mContext;
    private PasswordDialog mPasswordDialog;
    private InputPasswordData mInputPasswordData;
    private FiveFailedInputTimer mFiveFailedInputTimer;
    private HomeBroadcastReceiver mHomeBroadcastReceiver;

    private EditText mInputEdit;
    private TextView mTextTitle;
    private TextView mTextHead;
    private TextView mTextSub;
    private TextView mOtherEncryptionMethods;
    private TextView mTextFail;
    private TextView mTextFailWait;
    private Button mButtonCancel;
    private Button mButtonNext;

    private String mInputData;
    private String mPassword;
    private boolean mIsNeedResult;
    private boolean mEntryModifyMode;
    private boolean mIsResetPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_password_4numbers);
        instanceInputPasswordData();
        getInputPasswordActionType();
        initInputUI();
        judgeActionAndSetMode();
        initTipStage();
        invalidateTipText();
        registerHomeBroadcastReceiver();
        setTitle(mTextTitle);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (editable.length() == PASSWORD_NUM_LEN) {
            mInputData = mInputEdit.getText().toString();
            confirmInputPassword();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.other_encryption_methods: {
                if (mPasswordDialog != null) {
                    mPasswordDialog.showDialog();
                    mPasswordDialog.setState(mIsFirstSet, mEntryModifyMode, mIsNeedOpenSafe, mIsNeedResult);
                }
                break;
            }
            case R.id.cancel_btn:
                finish();
                break;
            case R.id.next_btn:
                handleConfirmation(mCurMode);
                break;
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        clearInputPasswordData();
        cancelTimerAndSetFailedData();
        unregisterHomeBroadcastReceiver();
        super.onDestroy();
    }

    private void registerHomeBroadcastReceiver() {
        mHomeBroadcastReceiver = new HomeBroadcastReceiver();
        mHomeBroadcastReceiver.setOnHomeBroadcastListener(new HomeBroadcastListener() {
            @Override
            public void onReceiveListener() {
                finish();
            }
        });
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeBroadcastReceiver, iFilter);
    }

    private void unregisterHomeBroadcastReceiver() {
        if (mHomeBroadcastReceiver != null) {
            unregisterReceiver(mHomeBroadcastReceiver);
            mHomeBroadcastReceiver = null;
        }
    }

    private void instanceInputPasswordData() {
        mInputPasswordData = InputPasswordData.getInstance(this, SafeConstants.LOCK_MODE_PASSWORD);
    }

    private void getInputPasswordActionType() {
        mInputPasswordData.setNewPasswordOrNot(mIsModifyPassword);
    }

    private void initInputUI() {
        mTextTitle = findViewById(R.id.password_title);
        mTextHead = findViewById(R.id.head_tip);
        mTextSub = findViewById(R.id.sub_tip);
        mTextFail = findViewById(R.id.fail_tip);
        mTextFailWait = findViewById(R.id.fail_wait_tip);
        mInputEdit = findViewById(R.id.password_input);
        mInputEdit.addTextChangedListener(this);
        mOtherEncryptionMethods = findViewById(R.id.other_encryption_methods);
        mOtherEncryptionMethods.setOnClickListener(this);
        mPasswordDialog = new PasswordDialog();
        mPasswordDialog.createPasswordDialog(this, SafeConstants.LOCK_MODE_PASSWORD);
        mInputEdit.setEnabled(true);
        mInputPasswordData.setFailedTimes(-1);

        mButtonCancel = findViewById(R.id.cancel_btn);
        mButtonCancel.setOnClickListener(this);
        mButtonNext = findViewById(R.id.next_btn);
        mButtonNext.setOnClickListener(this);
    }

    private void judgeActionAndSetMode() {
        Intent intent = getIntent();
        mIsModifyPassword = intent.getBooleanExtra(SafeConstants.IS_MODIFY_PASSWORD, false);
        mIsNeedResult = intent.getBooleanExtra(SafeConstants.IS_NEED_RESULT, false);
        mEntryModifyMode = intent.getBooleanExtra(SafeConstants.MODIFY_PASSWORD, false);
        mIsFirstSet = intent.getBooleanExtra(SafeConstants.IS_FIRST_SET, false);
        mIsResetPassword = intent.getBooleanExtra(SafeConstants.IS_RESET_PASSWORD, false);
        mIsNeedOpenSafe = intent.getBooleanExtra(SafeConstants.IS_NEED_OPEN_SAFE, false);

        if (mIsFirstSet || mEntryModifyMode) {
            mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        } else {
            mOtherEncryptionMethods.setVisibility(View.GONE);
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (action.equals(SafeConstants.NEW_APP_PROTECT_PASSWORD)) {
            mCurMode = MODE_ADD_NEW;
        } else if (action.equals(SafeConstants.APP_UNLOCK_PASSWORD_ACTIVITY)) {
            mCurMode = MODE_CHECK;
        }
    }

    private void confirmInputPassword() {
        handleConfirmation(mCurMode);
    }

    private void confirmPassword(String input) {
        mInputPasswordData.setFirstPassword(input);
        mInputEdit.getText().clear();
        mInputPasswordData.secondConfirmStage();
        invalidateTipText();
        mCurMode = MODE_NEW_CONFIRM;
        mOtherEncryptionMethods.setVisibility(View.GONE);
    }

    private void handleConfirmation(int mode) {
        switch (mode) {
            case MODE_ADD_NEW: {
                if (mInputPasswordData.getPasswordFromDatabase(this, mInputData)) {
                    mInputPasswordData.setInputTipNewFailState();
                    mInputPasswordData.setInputEnable(true);
                    clearInputPasswordData();
                    updateUIToNewMode();
                    invalidateTipText();
                } else if (SafeUtils.checkLegal(mInputData, PASSWORD_NUM_LEN)) {
                    confirmPassword(mInputData);
                    mOtherEncryptionMethods.setVisibility(View.GONE);
                } else {
                    showTipsDialog();
                }
                break;
            }
            case MODE_NEW_CONFIRM: {
                if (mInputPasswordData.getFirstPassword().equals(mInputData)) {
                    mInputPasswordData.setFailedTimes(0);
                    clearInputPasswordData();
                    saveSelect();
                    savePasswordAndFinish(mInputPasswordData.getFirstPassword());
                    break;
                }
                if (mIsModifyPassword) {
                    mInputPasswordData.setModifyConfirmPasswordWrong();
                } else {
                    mInputPasswordData.setNewConfirmPasswordWrong();
                }
                mInputPasswordData.setInputEnable(true);
                mTextFailWait.setVisibility(View.GONE);
                mTextHead.setVisibility(View.VISIBLE);
                updateUIToNewMode();
                invalidateTipText();
                break;
            }
            case MODE_CHECK: {
                String password = mInputData;
                if (mInputPasswordData.getPasswordFromDatabase(this, password)) {
                    mInputPasswordData.setFailedTimes(0);
                    clearInputPasswordData();
                    if (!(mIsModifyPassword || mIsNeedResult)) {
                        openSafe();
                    }
                    Intent intent = new Intent();
                    if (mIsNeedResult) {
                        intent.putExtra(SafeConstants.PASSWORD, password);
                    }
                    intent.putExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, SafeConstants.LOCK_MODE_PASSWORD);
                    intent.putExtra(SafeConstants.HEADER_TIP, mTextHead.getText());
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
                }

                inputPasswordFailedCheck();
                mInputEdit.getText().clear();
                if (mInputPasswordData.getFailedTimes() >= PASSWORD_FAILED_TIMES) {
                    mInputEdit.setEnabled(false);
                }
                break;
            }
            default:
                break;
        }
    }

    private void inputPasswordFailedCheck() {
        mInputPasswordData.updateTipStageAndFailTimes(this);
        invalidateTipText();
        mInputPasswordData.clearInputPasswordData();
        if (mInputPasswordData.getFailedTimes() >= PASSWORD_FAILED_TIMES) {
            startCountdownTimer(FIVE_FAILED_COUNT_TIME);
        }
    }

    private void saveSelect() {
        setPasswordMode(SafeConstants.LOCK_MODE_PASSWORD);
    }

    private void savePasswordAndFinish(String input) {
        mPassword = input;
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
        if (mIsModifyPassword || mIsResetPassword) {
            encrypWithNewPassword(input, safeFilePath);
            return;
        }
        SafeUtils.savePassword(this, input,
                SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_PASSWORD_PATH));
        SafeUtils.saveLockModeAndPassword(this, SafeConstants.LOCK_MODE_PASSWORD, input);
        if (mIsFirstSet) {
            if (!mIsNeedResult) {
                openSafe();
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showTipsDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.password_dialog_message)
                .setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                updateUIToNewMode();
            }
        }).setNegativeButton(R.string.password_dialog_new, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                updateUIToNewMode();
            }
        }).setPositiveButton(R.string.password_dialog_use, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                confirmPassword(mInputData);
            }
        }).create();
        dialog.show();
    }

    protected void setPasswordMode(int mode) {
        SafeUtils.saveLockMode(this, mode,
                SafeUtils.getSafeFilePath(SafeConstants.SAFE_ROOT_PATH, SafeConstants.LOCK_MODE_PATH));
    }

    private void encrypWithNewPassword(final String password, final String path) {
        ModifyPasswordThread modifyPasswordThread = new ModifyPasswordThread(this);
        modifyPasswordThread.setNewPassword(password);
        modifyPasswordThread.setOnModifyPasswordListener(new OnModifyPasswordListener() {
            @Override
            public void onModifyComplete(boolean success) {
                if (success) {
                    SafeUtils.savePassword(getApplicationContext(), password,
                            SafeUtils.getSafeFilePath(path, SafeConstants.LOCK_PASSWORD_PATH));
                    savePasswordAndMode(SafeConstants.LOCK_MODE_PASSWORD, password);
                    Intent intent = new Intent();
                    if (mIsResetPassword) {
                        if (mIsNeedResult) {
                            intent.putExtra(SafeConstants.PASSWORD, mPassword);
                        } else {
                            openSafe();
                        }
                    } else if (mIsNeedOpenSafe) {
                        InputPasswordManager.saveFiveTimesFailTime(getApplicationContext(), 0);
                        InputPasswordManager.saveRestCountTime(getApplicationContext(), 0);
                    }
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                }
                Toast.makeText(mContext, R.string.reset_password_fail_tips, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        modifyPasswordThread.start();
    }

    private void clearInputPasswordData() {
        mInputEdit.getText().clear();
        mInputPasswordData.onDestroy(this);
    }

    private void updateUIToNewMode() {
        mCurMode = MODE_ADD_NEW;
        clearInputPasswordData();
        mOtherEncryptionMethods.setVisibility(View.VISIBLE);
    }

    private void invalidateTipText() {
        mTextHead.setText(mInputPasswordData.getStageHeaderTip(this));
        mTextSub.setText(mInputPasswordData.getStageSubTip(this));
        String failedTip = mInputPasswordData.getStageFailedTip(this,
                PASSWORD_FAILED_TIMES - mInputPasswordData.getFailedTimes());
        mTextFail.setText(failedTip);
        if ("".equals(failedTip)) {
            mTextFail.setVisibility(View.GONE);
        } else {
            mTextFail.setVisibility(View.VISIBLE);
        }
        if (!"".equals(mInputPasswordData.getStageHeaderTip(this))) {
            mTextFailWait.setText("");
        }
    }

    private void initTipStage() {
        int failedTimes = mInputPasswordData.getFailedTimes(this);
        long restTime = InputPasswordManager.getRestCountTimeToInput(this);
        long restFingerTimes = getLockoutAttemptCountDownTime();
        if (restTime > 0 && !mIsNeedOpenSafe) {
            mInputPasswordData.setInputFailTipStage(false);
            mInputEdit.setEnabled(false);
            startCountdownTimer(restTime);
        } else if (restFingerTimes <= 0 || mCurMode != MODE_CHECK || mIsModifyPassword) {
            mInputEdit.setEnabled(true);
            if (!mIsModifyPassword) {
                mInputPasswordData.setInputTipStageAndState();
            } else if (mCurMode == MODE_ADD_NEW) {
                mInputPasswordData.setInputTipNewState();
            } else {
                mInputPasswordData.setInputTipOldState();
            }
            if (PASSWORD_FAILED_TIMES <= failedTimes) {
                mInputPasswordData.setFailedTimes(0);
                mInputPasswordData.saveFailedTimes(this);
            }
        } else {
            mInputEdit.setEnabled(false);
            mInputPasswordData.setInputFingerFailedTimeout();
        }
    }

    private void startCountdownTimer(long time) {
        if (mFiveFailedInputTimer != null) {
            mFiveFailedInputTimer.cancel();
            mFiveFailedInputTimer = null;
        }

        mFiveFailedInputTimer = (FiveFailedInputTimer) new FiveFailedInputTimer(time, 1000).start();
        mFiveFailedInputTimer.setCountdownTimerListener(new CountdownTimerListener() {
            @Override
            public void onTimerTick(long time) {
                mTextFailWait.setVisibility(View.VISIBLE);
                mTextFailWait.setText(mInputPasswordData.getStageFiveFailedTip(mContext, time / 1000));
            }

            @Override
            public void onTimerFinish() {
                mInputEdit.setEnabled(true);
                mTextSub.setVisibility(View.VISIBLE);
                mTextFailWait.setVisibility(View.GONE);
                setInputTipStageAndState();
                invalidateTipText();
                mInputPasswordData.setFailedTimes(0);
                cancelTimerAndSetFailedData();
                mInputPasswordData.saveFailedTimes(mContext);
            }
        });
        mInputEdit.setEnabled(false);
    }

    private void setInputTipStageAndState() {
        mInputPasswordData.setInputTipStageAndState();
    }

    private void cancelTimerAndSetFailedData() {
        if (mFiveFailedInputTimer != null) {
            mInputPasswordData.setFiveFailedData(this, true, mFiveFailedInputTimer.getCountRestTime());
            mFiveFailedInputTimer.cancel();
            mFiveFailedInputTimer = null;
        }
    }
}
