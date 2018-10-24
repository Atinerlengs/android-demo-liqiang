package com.freeme.safe.password;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.freeme.safe.encryption.thread.ModifyPasswordThread;
import com.freeme.safe.encryption.thread.ModifyPasswordThread.OnModifyPasswordListener;
import com.freeme.filemanager.R;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import static com.freeme.safe.password.InputPasswordData.PASSWORD_COMPLEX;
import static com.freeme.safe.password.InputPasswordData.PASSWORD_MAX_LEN;
import static com.freeme.safe.password.InputPasswordData.PASSWORD_MIN_LEN;
import static com.freeme.safe.password.InputPasswordData.PASSWORD_SIMPLE;
import static com.freeme.safe.password.InputPasswordData.STATE_HAS_NOT_FINGER_PRINT;

public class UnlockComplexActivity extends FingerPrintActivity implements TextWatcher,
        OnEditorActionListener, AnimatorListener, OnClickListener {
    private static final String TAG = "ComplexActivity";

    private Context mContext;
    private AnimatorSet mCheckFailedAnimatorSet;
    private InputPasswordData mInputPasswordData;
    private FiveFailedInputTimer mFingerFailedInputTimer;
    private FiveFailedInputTimer mFiveFailedInputTimer;
    private PasswordDialog mPasswordDialog;
    private HomeBroadcastReceiver mHomeBroadcastReceiver;

    private EditText mPasswordEntry;
    private TextView mOtherEncryptionMethods;
    private TextView mTextFail;
    private TextView mTextFailWait;
    private TextView mTextTitle;
    private TextView mTextHead;
    private TextView mTextSub;
    private Button mButtonCancel;
    private Button mButtonNext;

    private String mInputData;
    private String mPassword;
    private String mInput;
    private boolean mEntryModifyMode;
    private boolean mIsNeedResult;
    private boolean mIsResetPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_password_complex);
        instanceInputPasswordData();
        getInputComplexActionType();
        judgeActionAndSetMode();
        initViews();
        initTipStage();
        invalidateTipText();
        registerHomeBroadcastReceiver();

        setTitle(mTextTitle);
    }

    @Override
    protected void onResume() {
        invalidateTipStage();
        super.onResume();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable editable) {
        mInputData = mPasswordEntry.getText().toString();
        comfirmButtonEnable();
        if (limitLengthInput(editable)) {
            inputCheckPassword(editable);
        }
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
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
            case R.id.forget_view:
                break;
            case R.id.cancel_btn:
                finish();
                break;
            case R.id.next_btn:
                menubarLeftClickLogic();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Intent intent;
            switch (requestCode) {
                case SafeConstants.REQUEST_ENTRY_SAFE: {
                    intent = new Intent();
                    intent.putExtra(SafeConstants.PASSWORD, mPassword);
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
                }
                case SafeConstants.REQUEST_RESET_PASSCODE: {
                    if (data != null) {
                        int mode = data.getIntExtra(SafeConstants.SAFE_LOCK_MODE_KEY, -1);
                        if (mode != SafeConstants.LOCK_MODE_DEFAULT) {
                            if (mInputPasswordData != null) {
                                mInputPasswordData.setFailedTimes(0);
                            }
                            if (mFiveFailedInputTimer != null) {
                                mFiveFailedInputTimer.setCountRestTime(0);
                                cancelTimerAndSetFailedDate();
                                cancelAnimatorSet();
                            }
                            if (mode == SafeConstants.LOCK_MODE_COMPLEX) {
                                if (mInputPasswordData != null) {
                                    mInputPasswordData.setInputEnable(true);
                                    mInputPasswordData.setNewPasswordOrNot(true);
                                }
                                clearInputPasswordData();
                                mTextFail.setVisibility(View.GONE);
                                mTextFailWait.setVisibility(View.GONE);
                                mTextHead.setVisibility(View.VISIBLE);
                                mTextHead.setText(R.string.input_password);
                                updateUIToNewMode();
                                initButtonBar();
                            } else if (mode == SafeConstants.LOCK_MODE_PASSWORD) {
                                intent = new Intent(this, UnlockPasswordActivity.class);
                                intent.setAction(SafeConstants.NEW_APP_PROTECT_PASSWORD);
                                intent.putExtra(SafeConstants.IS_RESET_PASSWORD, true);
                                intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
                                startActivityForResult(intent, SafeConstants.REQUEST_RESET_PASSWORD);
                            } else if (mode == SafeConstants.LOCK_MODE_PATTERN) {
                                intent = new Intent(this, UnlockPatternActivity.class);
                                intent.setAction(SafeConstants.NEW_APP_PROTECT_PATTERN);
                                intent.putExtra(SafeConstants.IS_RESET_PASSWORD, true);
                                intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
                                startActivityForResult(intent, SafeConstants.REQUEST_RESET_PATTERN);
                            }
                        }
                    }
                    break;
                }
                case SafeConstants.REQUEST_RESET_PATTERN:
                case SafeConstants.REQUEST_RESET_PASSWORD: {
                    if (data != null) {
                        if (mInputPasswordData != null) {
                            mInputPasswordData.setFailedTimes(0);
                        }
                        if (mFiveFailedInputTimer != null) {
                            mFiveFailedInputTimer.setCountRestTime(0);
                            cancelTimerAndSetFailedDate();
                            cancelAnimatorSet();
                        }
                        if (!TextUtils.isEmpty(data.getStringExtra(SafeConstants.PASSWORD))) {
                            setResult(RESULT_OK, data);
                        }
                        finish();
                    }
                    break;
                }
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED && mIsNeedResult) {
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void invalidateTipStage() {
        boolean state = isFingerprintUnlockEnabled();
        boolean fingerState = false;
        if (SafeUtils.isNeededSdk() && getFailedAttempts() <= 0 && mFingerCheckFail) {
            fingerState = true;
        }
        if (mFingerprintEnabled != isFingerprintUnlockEnabled()) {
            mFingerprintEnabled = state;
            initTipStage();
            invalidateTipText();
        } else if (fingerState) {
            if (mFingerFailedInputTimer != null) {
                mFingerFailedInputTimer.cancel();
                mFingerFailedInputTimer = null;
            }
            mFingerCheckFail = true;
            mTextSub.setVisibility(View.VISIBLE);
            initTipStage();
            invalidateTipText();
        }
    }

    @Override
    protected void onDestroy() {
        clearInputPasswordData();
        cancelTimerAndSetFailedDate();
        cancelAnimatorSet();
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

    private void judgeActionAndSetMode() {
        Intent intent = getIntent();
        mIsModifyPassword = intent.getBooleanExtra(SafeConstants.IS_MODIFY_PASSWORD, false);
        mEntryModifyMode = intent.getBooleanExtra(SafeConstants.MODIFY_PASSWORD, false);
        mIsNeedResult = intent.getBooleanExtra(SafeConstants.IS_NEED_RESULT, false);
        mIsFirstSet = intent.getBooleanExtra(SafeConstants.IS_FIRST_SET, false);
        mIsResetPassword = intent.getBooleanExtra(SafeConstants.IS_RESET_PASSWORD, false);
        mIsNeedOpenSafe = intent.getBooleanExtra(SafeConstants.IS_NEED_OPEN_SAFE, false);
        String action = intent.getAction();
        if (SafeConstants.NEW_APP_PROTECT_COMPLEX.equals(action)) {
            mCurMode = MODE_ADD_NEW;
        } else if (SafeConstants.APP_UNLOCK_COMPLEX_ACTIVITY.equals(action)) {
            mCurMode = MODE_CHECK;
        }
    }

    private void instanceInputPasswordData() {
        mInputPasswordData = InputPasswordData.getInstance(this, PASSWORD_COMPLEX);
    }

    private void clearInputPasswordData() {
        mInputPasswordData.onDestroy(this);
    }

    private void getInputComplexActionType() {
        if (SafeConstants.NEW_APP_PROTECT_COMPLEX.equals(getIntent().getAction())) {
            mInputPasswordData.setNewPasswordOrNot(true);
        } else {
            mInputPasswordData.setNewPasswordOrNot(false);
        }
    }

    private void initButtonBar() {
        if (mInputPasswordData.isNewPassword()) {
            mButtonNext.setEnabled(false);
        } else if (mCurMode == MODE_CHECK) {
            mButtonNext.setVisibility(View.GONE);
        }
    }

    private void updateUIToNewMode() {
        mCurMode = MODE_ADD_NEW;
        setKeyboardEnable(true);
        clearPasswordEntry();
    }

    private void initViews() {
        mPasswordDialog = new PasswordDialog();
        mPasswordDialog.createPasswordDialog(this, SafeConstants.LOCK_MODE_COMPLEX);
        mTextTitle = findViewById(R.id.password_title);
        mTextHead = findViewById(R.id.head_tip);
        mTextSub = findViewById(R.id.sub_tip);
        mTextFail = findViewById(R.id.fail_tip);
        mTextFailWait = findViewById(R.id.fail_wait_tip);
        mPasswordEntry = findViewById(R.id.password_entry);
        mPasswordEntry.addTextChangedListener(this);
        mPasswordEntry.setOnEditorActionListener(this);
        mOtherEncryptionMethods = findViewById(R.id.other_encryption_methods);
        mOtherEncryptionMethods.setOnClickListener(this);
        if (mIsFirstSet || mEntryModifyMode) {
            mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        } else {
            mOtherEncryptionMethods.setVisibility(View.GONE);
        }

        mButtonCancel = findViewById(R.id.cancel_btn);
        mButtonCancel.setOnClickListener(this);
        mButtonNext = findViewById(R.id.next_btn);
        mButtonNext.setOnClickListener(this);

        if (mIsModifyPassword && mIsNeedOpenSafe) {
            mInputPasswordData.setInputEnable(true);
            clearInputPasswordData();
            mTextFail.setVisibility(View.GONE);
            mTextFailWait.setVisibility(View.GONE);
            mTextHead.setVisibility(View.VISIBLE);
            mTextHead.setText(R.string.input_password);
            updateUIToNewMode();
        }
    }

    private void initTipStage() {
        mFingerprintEnabled = isFingerprintUnlockEnabled();
        int failedTimes = mInputPasswordData.getFailedTimes(this);
        long restTime = InputPasswordManager.getRestCountTimeToInput(this);
        long restFingerTimes = getLockoutAttemptCountDownTime();
        if (restTime > 0 && !mIsNeedOpenSafe) {
            mInputPasswordData.setInputFailTipStage(false);
            startCountdownTimer(restTime);
            setInputState(false, -1);
        } else if (!mFingerprintEnabled || restFingerTimes <= 0 || mCurMode != MODE_CHECK || mIsModifyPassword) {
            if (!mIsModifyPassword) {
                if (SafeUtils.isNeededSdk() && InputPasswordManager.getFingerFailedCountState(this)) {
                    resumeServiceFlagAndRecoverFinger();
                }
                mInputPasswordData.setInputTipStageAndState();
            } else if (mCurMode == MODE_ADD_NEW) {
                mInputPasswordData.setInputTipNewState();
            } else {
                mInputPasswordData.setInputTipOldState();
            }
            setInputState(true, -1);
            if (PASSWORD_FAILED_TIMES <= failedTimes) {
                mInputPasswordData.setFailedTimes(0);
                mInputPasswordData.saveFailedTimes(this);
            }
        }
    }

    private void invalidateTipText() {
        mTextHead.setText(mInputPasswordData.getStageHeaderTip(this));
        mTextSub.setText(mInputPasswordData.getStageSubTip(this));
        String failedTip = mInputPasswordData.getStageFailedTip(this, PASSWORD_FAILED_TIMES - mInputPasswordData.getFailedTimes());
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

    private void updateTextFail(Editable editable) {
        if (editable != null && editable.length() == 1) {
            mInputPasswordData.setAreInputTipStage();
            invalidateTipText();
        }
    }

    private void startCountdownTimer(long time) {
        if (mFiveFailedInputTimer != null) {
            mFiveFailedInputTimer.cancel();
            mFiveFailedInputTimer = null;
        }
        mFingerCheckFail = false;
        InputPasswordManager.setFingerFailedCountState(mContext, false);
        mFiveFailedInputTimer = (FiveFailedInputTimer) new FiveFailedInputTimer(time, 1000).start();
        mFiveFailedInputTimer.setCountdownTimerListener(new CountdownTimerListener() {
            @Override
            public void onTimerTick(long time) {
                mTextFailWait.setVisibility(View.VISIBLE);
                mTextFailWait.setText(mInputPasswordData.getStageFiveFailedTip(mContext, time / 1000));
            }

            @Override
            public void onTimerFinish() {
                mFingerCheckFail = true;
                recoverFingerFailedFlag();
                mTextSub.setVisibility(View.VISIBLE);
                mTextFailWait.setVisibility(View.GONE);
                setInputTipStageAndState();
                invalidateTipText();
                cancelTimerAndSetFailedDate();
                setInputState(true, 0);
                mInputPasswordData.saveFailedTimes(mContext);
            }
        });
    }

    private void cancelTimerAndSetFailedDate() {
        if (mFiveFailedInputTimer != null) {
            mInputPasswordData.setFiveFailedData(this, true, mFiveFailedInputTimer.getCountRestTime());
            mFiveFailedInputTimer.cancel();
            mFiveFailedInputTimer = null;
        }
    }

    private void clearPasswordEntry() {
        mPasswordEntry.getText().clear();
    }

    private void setInputTipStageAndState() {
        mInputPasswordData.setInputTipStageAndState();
    }

    private void setInputState(boolean keyboardEnable, int failedTimes) {
        setKeyboardEnable(keyboardEnable);
        if (failedTimes >= 0) {
            mInputPasswordData.setFailedTimes(failedTimes);
        }
    }

    private void setKeyboardEnable(boolean enabled) {
        mPasswordEntry.setEnabled(enabled);
        if (enabled) {
            mPasswordEntry.requestFocus();
        } else {
            mPasswordEntry.clearFocus();
        }
        mPasswordEntry.setFocusable(enabled);
        mPasswordEntry.setFocusableInTouchMode(enabled);
        mInputPasswordData.setInputEnable(enabled);
    }

    private void resumeServiceFlagAndRecoverFinger() {
        InputPasswordManager.setFingerPrintTimes(getApplicationContext(), 0);
    }

    private void checkFailedAnimation() {
        cancelAnimatorSet();
        createAnimatorSet();
        mCheckFailedAnimatorSet.start();
    }

    private void createAnimatorSet() {
        mCheckFailedAnimatorSet = new AnimatorSet();
        mCheckFailedAnimatorSet.playTogether(InputPasswordManager.failEditScaleAnimation(mPasswordEntry));
        mCheckFailedAnimatorSet.addListener(this);
    }

    private void cancelAnimatorSet() {
        if (mCheckFailedAnimatorSet != null) {
            mCheckFailedAnimatorSet.cancel();
            mCheckFailedAnimatorSet = null;
        }
    }

    private void saveSelect() {
        setPasswordMode(SafeConstants.LOCK_MODE_COMPLEX);
    }

    protected void setPasswordMode(int mode) {
        String lockModePath = SafeUtils.getSafeFilePath(SafeConstants.SAFE_ROOT_PATH, SafeConstants.LOCK_MODE_PATH);
        SafeUtils.saveLockMode(this, mode, lockModePath);
    }

    private void encrypWithNewPassword(final String password, final String path) {
        ModifyPasswordThread modifyPasswordThread = new ModifyPasswordThread(this);
        modifyPasswordThread.setNewPassword(password);
        modifyPasswordThread.setOnModifyPasswordListener(new OnModifyPasswordListener() {
            @Override
            public void onModifyComplete(boolean success) {
                if (success) {
                    if (SafeUtils.DEBUG) {
                        Log.v(TAG, "modify password success");
                    }
                    SafeUtils.savePassword(mContext, password, SafeUtils.getSafeFilePath(path, SafeConstants.LOCK_PASSWORD_PATH));
                    savePasswordAndMode(SafeConstants.LOCK_MODE_COMPLEX, password);
                    Intent intent = new Intent();
                    if (mIsResetPassword) {
                        if (mIsNeedResult) {
                            intent.putExtra(SafeConstants.PASSWORD, mPassword);
                        } else {
                            openSafe();
                        }
                    } else if (mIsNeedOpenSafe) {
                        InputPasswordManager.saveFiveTimesFailTime(mContext, 0);
                        InputPasswordManager.saveRestCountTime(mContext, 0);
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

    private void savePasswordAndFinish(String input) {
        mPassword = input;
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
        if (mIsModifyPassword || mIsResetPassword) {
            encrypWithNewPassword(input, safeFilePath);
            return;
        }
        SafeUtils.savePassword(this, input, SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_PASSWORD_PATH));
        savePasswordAndMode(SafeConstants.LOCK_MODE_COMPLEX, input);
        if (mIsFirstSet) {
            if (!mIsNeedResult) {
                openSafe();
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showTipsDialog() {
        AlertDialog dialog = new Builder(this).setTitle(R.string.password_dialog_message).setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                updateUIToNewMode();
            }
        }).setNegativeButton(R.string.password_dialog_new, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                updateUIToNewMode();
            }
        }).setPositiveButton(R.string.password_dialog_use, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                confirmPassword(mInput);
            }
        }).create();
        dialog.show();
    }

    private void handleConfirmation(int mode, String password) {
        switch (mode) {
            case MODE_ADD_NEW: {
                mInput = password;
                if (mInputPasswordData.getPasswordFromDatabase(this, mInput)) {
                    mInputPasswordData.setInputTipNewFailState();
                    mInputPasswordData.setInputEnable(true);
                    clearInputPasswordData();
                    mInputPasswordData.setNewPasswordOrNot(true);
                    updateUIToNewMode();
                    invalidateTipText();
                } else if (SafeUtils.checkLegal(mInput, 8)) {
                    confirmPassword(password);
                    setNetButton(true);
                    mOtherEncryptionMethods.setVisibility(View.GONE);
                } else {
                    showTipsDialog();
                }
                break;
            }
            case MODE_NEW_CONFIRM: {
                if (mInputPasswordData.getFirstPassword().equals(password)) {
                    mInputPasswordData.setFailedTimes(0);
                    clearInputPasswordData();
                    saveSelect();
                    savePasswordAndFinish(mInputPasswordData.getFirstPassword());
                    break;
                }
                setNetButton(false);
                updateUIToNewMode();
                if (mIsModifyPassword) {
                    mInputPasswordData.setModifyConfirmPasswordWrong();
                } else {
                    mInputPasswordData.setNewConfirmPasswordWrong();
                }
                mTextHead.setText(R.string.input_password);
                mInputPasswordData.setInputEnable(true);
                clearInputPasswordData();
                mInputPasswordData.setNewPasswordOrNot(true);
                invalidateTipText();
                break;
            }
            case MODE_CHECK: {
                if (mInputPasswordData.getPasswordFromDatabase(this, mInputData)) {
                    mFingerCheckFail = false;
                    resumeServiceFlagAndRecoverFinger();
                    mInputPasswordData.setFailedTimes(0);
                    clearInputPasswordData();
                    if (!(mIsModifyPassword || mIsNeedResult)) {
                        openSafe();
                    }
                    Intent intent = new Intent();
                    if (mIsNeedResult) {
                        intent.putExtra(SafeConstants.PASSWORD, mInputData);
                    }
                    intent.putExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, SafeConstants.LOCK_MODE_COMPLEX);
                    intent.putExtra(SafeConstants.HEADER_TIP, mTextHead.getText());
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
                }
                inputPasswordFailChecked();
                checkFailedAnimation();
                break;
            }
            default:
                break;
        }
    }

    private void setNetButton(boolean complete) {
        if (complete) {
            mButtonNext.setText(R.string.completed);
        } else {
            mButtonNext.setText(R.string.password_next);
        }
    }

    private void confirmPassword(String password) {
        firstEditorAction(password);
        mInputPasswordData.secondConfirmStage();
    }

    private void inputPasswordFailChecked() {
        if (SafeUtils.isNeededSdk()) {
            mInputPasswordData.updateTipFailTimes(this);
            if (!(mFingerCheckFail && mInputPasswordData.getFailedTimes() < PASSWORD_FAILED_TIMES && mFingerprintEnabled)) {
                mInputPasswordData.updateFailedTimesTipStage(this, mFingerprintEnabled);
                invalidateTipText();
            }
        } else {
            mInputPasswordData.updateTipStageAndFailTimes(this);
            invalidateTipText();
        }
        clearPasswordEntry();
        if (mInputPasswordData.getFailedTimes() >= PASSWORD_FAILED_TIMES) {
            setInputState(false, -1);
            startCountdownTimer(FIVE_FAILED_COUNT_TIME);
        }
        doHapticKeyLongClick();
    }

    private void menubarLeftClickLogic() {
        handleConfirmation(mCurMode, mInputData);
        comfirmButtonEnable();
    }

    private void comfirmButtonEnable() {
        int length = mInputData.length();
        if (length < PASSWORD_MIN_LEN || length > PASSWORD_MAX_LEN) {
            mButtonNext.setEnabled(false);
        } else {
            mButtonNext.setEnabled(true);
        }
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == 0 || actionId == 6 || actionId == 5) {
            int length = mInputData.length();
            if (length >= PASSWORD_MIN_LEN && length <= PASSWORD_MAX_LEN) {
                handleConfirmation(mCurMode, mInputData);
            }
        }
        return true;
    }

    private void firstEditorAction(String password) {
        int length = password.length();
        if (length >= PASSWORD_SIMPLE && length < PASSWORD_MIN_LEN) {
            mInputPasswordData.complexShortStage();
            mCurMode = MODE_ADD_NEW;
            mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        } else if (length < PASSWORD_MIN_LEN) {
            mCurMode = MODE_ADD_NEW;
            mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        } else if (mInputPasswordData.standardNewComplexStage(password)) {
            mCurMode = MODE_NEW_CONFIRM;
            mOtherEncryptionMethods.setVisibility(View.GONE);
            mInputPasswordData.setFirstPassword(password);
            mInputPasswordData.secondConfirmStage();
        } else {
            mCurMode = MODE_ADD_NEW;
            mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        }
        invalidateTipText();
        clearPasswordEntry();
    }

    private void inputCheckPassword(Editable editable) {
        updateTextFail(editable);
        checkUnlockPassword(editable);
    }

    private void checkUnlockPassword(Editable editable) {
        int length = editable.length();
        if (length >= PASSWORD_MIN_LEN && length <= PASSWORD_MAX_LEN
                && mInputPasswordData.getPasswordFromDatabase(this, mInputData)
                && mCurMode == MODE_CHECK) {
            if (mInputPasswordData.getPasswordFromDatabase(this, mInputData)) {
                resumeServiceFlagAndRecoverFinger();
                mInputPasswordData.setFailedTimes(0);
                clearInputPasswordData();
                if (!(mIsModifyPassword || mIsNeedResult)) {
                    openSafe();
                }
                Intent intent = new Intent();
                if (mIsNeedResult) {
                    intent.putExtra(SafeConstants.PASSWORD, mInputData);
                }
                intent.putExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, SafeConstants.LOCK_MODE_COMPLEX);
                intent.putExtra(SafeConstants.HEADER_TIP, mTextHead.getText());
                setResult(RESULT_OK, intent);
                finish();
                return;
            }
            inputPasswordFailChecked();
            checkFailedAnimation();
        }
    }

    private boolean limitLengthInput(Editable editable) {
        int length = editable.length();
        if (length >= PASSWORD_MIN_LEN && length <= PASSWORD_MAX_LEN) {
            return true;
        }
        if (length > PASSWORD_MAX_LEN) {
            if (mCurMode == MODE_ADD_NEW) {
                if (mIsModifyPassword) {
                    mInputPasswordData.setLengthLongModifyStage();
                } else {
                    mInputPasswordData.setLengthLongStage();
                }
                invalidateTipText();
            }
            int selEndIndex = Selection.getSelectionEnd(editable);
            if (selEndIndex > 0) {
                editable.delete(selEndIndex - 1, selEndIndex);
            }
        }
        return false;
    }

    private void recoverFingerFailedFlag() {
        mInputPasswordData.setFingerFailedFlag(false);
        mInputPasswordData.setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }
}
