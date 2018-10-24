package com.freeme.safe.password;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.safe.utils.LockPatternUtils;
import com.freeme.safe.view.LockPatternView;
import com.freeme.safe.view.LockPatternView.DisplayMode;
import com.freeme.safe.view.LockPatternView.Cell;
import com.freeme.safe.view.LockPatternView.OnPatternListener;
import com.freeme.filemanager.R;
import com.freeme.safe.encryption.thread.ModifyPasswordThread;
import com.freeme.safe.encryption.thread.ModifyPasswordThread.OnModifyPasswordListener;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.util.List;

import javax.crypto.Cipher;

import static com.freeme.safe.password.InputPatternData.STATE_ENABLE_NOT_FINGER_PRINT;

public class UnlockPatternActivity extends FingerPrintActivity implements OnPatternListener, OnClickListener {

    private static final String TAG = "PatternActivity";

    private Context mContext;
    private FiveFailedInputTimer mFingerFailedInputTimer;
    private FiveFailedInputTimer mFiveFailedInputTimer;
    private InputPatternData mInputPatternData;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private PasswordDialog mPasswordDialog;
    private HomeBroadcastReceiver mHomeBroadcastReceiver;

    private TextView mOtherEncryptionMethods;
    private TextView mTextTitle;
    private TextView mTextFail;
    private TextView mTextFailWait;
    private TextView mTextHead;
    private TextView mTextSub;
    private Button mButtonCancel;
    private Button mButtonNext;

    private Intent mIntent;
    private String mAction;
    private String mPassword;
    private boolean mEntryModifyMode;
    private boolean mIsNeedResult;
    private boolean mIsResetPassword;

    private Runnable mClearPatternRunnable = new Runnable() {
        @Override
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_password_pattern);
        mIntent = getIntent();
        mAction = mIntent.getAction();

        instanceInputPatternData();
        getPatternActionType();
        judgeActionAndSetMode();
        initInputUI();
        setLockPatternView();
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
    public boolean onNavigateUp() {
        finish();
        return true;
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
            case R.id.forget_view: {
                mIsResetPassword = true;
                finish();
                break;
            }
            case R.id.cancel_btn: {
                finish();
                break;
            }
            case R.id.next_btn: {
                mButtonNext.setEnabled(false);
                enableLockPatternView(true);
                handleConfirmation(mCurMode);
                break;
            }
        }
    }

    @Override
    public void onPatternStart() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        if (mInputPatternData.isNewPattern()) {
            mInputPatternData.setNewPatternProgress();
            invalidateTipText();
        }
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
        if (pattern.size() < 4) {
            drawPatternShort();
        } else if (mInputPatternData.isNewPattern()) {
            checkNewPattern(pattern);
        } else {
            checkUnlockPattern(pattern);
        }
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
                        if (mode != -1) {
                            if (mInputPatternData != null) {
                                mInputPatternData.setFailedTimes(0);
                            }
                            if (mFiveFailedInputTimer != null) {
                                mFiveFailedInputTimer.setCountRestTime(0);
                                cancelTimerAndSetFailedData();
                            }
                            if (mode == SafeConstants.LOCK_MODE_PATTERN) {
                                retryModeAddNew();
                            } else if (mode == SafeConstants.LOCK_MODE_PASSWORD) {
                                intent = new Intent(this, UnlockPasswordActivity.class);
                                intent.setAction(SafeConstants.NEW_APP_PROTECT_PASSWORD);
                                intent.putExtra(SafeConstants.IS_RESET_PASSWORD, true);
                                intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
                                startActivityForResult(intent, SafeConstants.REQUEST_RESET_PASSWORD);
                            } else if (mode == SafeConstants.LOCK_MODE_COMPLEX) {
                                intent = new Intent(this, UnlockComplexActivity.class);
                                intent.setAction(SafeConstants.NEW_APP_PROTECT_COMPLEX);
                                intent.putExtra(SafeConstants.IS_RESET_PASSWORD, true);
                                intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
                                startActivityForResult(intent, SafeConstants.REQUEST_RESET_COMPLEX);
                            }
                        }
                    }
                    break;
                }
                case SafeConstants.REQUEST_RESET_PASSWORD:
                case SafeConstants.REQUEST_RESET_COMPLEX: {
                    if (data != null) {
                        if (mInputPatternData != null) {
                            mInputPatternData.setFailedTimes(0);
                        }
                        if (mFiveFailedInputTimer != null) {
                            mFiveFailedInputTimer.setCountRestTime(0);
                            cancelTimerAndSetFailedData();
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
    protected void onDestroy() {
        clearInputPatternData();
        cancelTimerAndSetFailedData();
        cancelFingerFaiedTimer();
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

    private void instanceInputPatternData() {
        mInputPatternData = InputPatternData.getInstance(this, mIntent);
    }

    private void clearInputPatternData() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mInputPatternData.onDestory(this);
    }

    private void getPatternActionType() {
        if (mAction != null && mAction.equals(SafeConstants.NEW_APP_PROTECT_PATTERN)) {
            mInputPatternData.setNewPatternOrNot(true);
        } else {
            mInputPatternData.setNewPatternOrNot(false);
        }
    }

    private void initInputUI() {
        mPasswordDialog = new PasswordDialog();
        mPasswordDialog.createPasswordDialog(this, SafeConstants.LOCK_MODE_PATTERN);
        mTextTitle = findViewById(R.id.password_title);
        mTextHead = findViewById(R.id.head_tip);
        mTextSub = findViewById(R.id.sub_tip);
        mTextFail = findViewById(R.id.fail_tip);
        mTextFailWait = findViewById(R.id.fail_wait_tip);
        mLockPatternView = findViewById(R.id.lock_pattern);
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
        mButtonNext.setVisibility(mCurMode == MODE_CHECK ? View.GONE : View.VISIBLE);
    }

    private void setLockPatternView() {
        mLockPatternUtils = new LockPatternUtils(this);
        mLockPatternView.enableInput();
        if (mAction != null && SafeConstants.APP_UNLOCK_PATTERN_ACTIVITY.equals(mAction)) {
            mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(UserHandle.myUserId()));
        }
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
        mLockPatternView.setDisplayMode(DisplayMode.Correct);
        mLockPatternView.setOnPatternListener(this);
    }

    private void updateUiToMode(int mode) {
        switch (mode) {
            case MODE_ADD_NEW: {
                mLockPatternView.clearPattern();
                mLockPatternView.enableInput();
                break;
            }
            default:
                break;
        }
    }

    private void retryModeAddNew() {
        mCurMode = MODE_ADD_NEW;
        mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        mInputPatternData.setInputEnable(true);
        clearInputPatternData();
        mInputPatternData.setNewPatternOrNot(true);
        updateUiToMode(mCurMode);
        mInputPatternData.setDrawPattern(null);
        mTextFail.setVisibility(View.GONE);
        mTextFailWait.setVisibility(View.GONE);
        mTextHead.setVisibility(View.VISIBLE);
        mTextHead.setText(R.string.pattern_controller_input_pattern);
    }

    private void initTipStage() {
        mFingerprintEnabled = isFingerprintUnlockEnabled();
        int failedTimes = mInputPatternData.getFailedTimes(this);
        long restTime = InputPasswordManager.getRestCountTimeToInput(this);
        long restFingerTimes = getLockoutAttemptCountDownTime();
        if (restTime > 0 && !mIsNeedOpenSafe) {
            mInputPatternData.setInputFailTipStage(false);
            setInputState(false, -1);
            startCountdownTimer(restTime);
        } else if (!mFingerprintEnabled || restFingerTimes <= 0 || mCurMode != MODE_CHECK || mIsModifyPassword) {
            if (!mIsModifyPassword) {
                if (SafeUtils.isNeededSdk() && InputPasswordManager.getFingerFailedCountState(this)) {
                    resumeSeviceFlagAndRecoverFinger();
                }
                mInputPatternData.setInputTipStageAndState(mCurMode);
            } else if (mCurMode == MODE_ADD_NEW) {
                mInputPatternData.setInputTipNewState();
            } else {
                mInputPatternData.setInputTipOldState();
            }
            setInputState(true, -1);
            if (PASSWORD_FAILED_TIMES <= failedTimes) {
                mInputPatternData.setFailedTimes(0);
                mInputPatternData.saveFailedTimes(this);
            }
        } else {
            setInputState(true, -1);
            mInputPatternData.setInputFingerFailedTimeout();
            startFingerCountdownTimer(restFingerTimes);
        }
    }

    private void invalidateTipText() {
        mTextHead.setText(mInputPatternData.getStageHeaderTip(this));
        mTextSub.setText(mInputPatternData.getStageSubTip(this));
        String failedTip = mInputPatternData.getStageFailedTip(this, PASSWORD_FAILED_TIMES - mInputPatternData.getFailedTimes());
        mTextFail.setText(failedTip);
        if ("".equals(failedTip)) {
            mTextFail.setVisibility(View.GONE);
        } else {
            mTextFail.setVisibility(View.VISIBLE);
        }
        if (!"".equals(mInputPatternData.getStageHeaderTip(this))) {
            mTextFailWait.setText("");
        }
    }

    private void setInputState(boolean keyboardEnable, int failedTimes) {
        setPatternEnable(keyboardEnable);
        if (failedTimes == 0) {
            mInputPatternData.setFailedTimes(failedTimes);
        }
    }

    private void setPatternEnable(boolean enable) {
        if (enable) {
            mLockPatternView.enableInput();
        } else {
            mLockPatternView.disableInput();
        }
        mInputPatternData.setInputEnable(enable);
    }

    private void setInputTipStageAndState() {
        mInputPatternData.setInputTipStageAndState(mCurMode);
    }

    private void inputPasswordFailChecked() {
        if (SafeUtils.isNeededSdk()) {
            mInputPatternData.updateTipFailTimes(this);
            if (!(mFingerCheckFail && mInputPatternData.getFailedTimes() < PASSWORD_FAILED_TIMES && mFingerprintEnabled)) {
                mInputPatternData.updateFailedTimesTipStage(this, mFingerprintEnabled);
                invalidateTipText();
            }
        } else {
            mInputPatternData.updateTipStageAndFailTimes(this);
            invalidateTipText();
        }
        if (mInputPatternData.getFailedTimes() >= PASSWORD_FAILED_TIMES) {
            mLockPatternView.clearPattern();
            setInputState(false, -1);
            startCountdownTimer(FIVE_FAILED_COUNT_TIME);
            return;
        }
        postClearPatternRunnable();
    }

    private void startFingerCountdownTimer(long time) {
        mFingerCheckFail = true;
        InputPasswordManager.setFingerFailedCountState(mContext, true);
        if (mFingerFailedInputTimer != null) {
            mFingerFailedInputTimer.cancel();
            mFingerFailedInputTimer = null;
        }
        mFingerFailedInputTimer = (FiveFailedInputTimer) new FiveFailedInputTimer(time, 1000).start();
        mFingerFailedInputTimer.setCountdownTimerListener(new CountdownTimerListener() {
            @Override
            public void onTimerTick(long time) {
                mTextSub.setVisibility(View.GONE);
                mTextFail.setVisibility(View.VISIBLE);
                mInputPatternData.setInputFingerFailedTimeout();
                mTextFail.setText(mInputPatternData.getStageFingerFailedTimeoutTip(mContext, time / 1000));
            }

            @Override
            public void onTimerFinish() {
                mHandler.sendEmptyMessageDelayed(0, 300);
            }
        });
    }

    private void startCountdownTimer(long time) {
        if (mFiveFailedInputTimer != null) {
            mFiveFailedInputTimer.cancel();
            mFiveFailedInputTimer = null;
        }
        if (mFingerFailedInputTimer != null) {
            mFingerFailedInputTimer.cancel();
            mFingerFailedInputTimer = null;
        }
        mFingerCheckFail = false;
        InputPasswordManager.setFingerFailedCountState(mContext, false);
        mFiveFailedInputTimer = (FiveFailedInputTimer) new FiveFailedInputTimer(time, 1000).start();
        mFiveFailedInputTimer.setCountdownTimerListener(new CountdownTimerListener() {
            @Override
            public void onTimerTick(long time) {
                mTextFailWait.setText(mInputPatternData.getStageFiveFailedTip(mContext, time / 1000));
            }

            @Override
            public void onTimerFinish() {
                mFingerCheckFail = true;
                recoverFingerFailedFlag();
                mTextSub.setVisibility(View.VISIBLE);
                setInputTipStageAndState();
                invalidateTipText();
                setInputState(true, 0);
                cancelTimerAndSetFailedData();
                mInputPatternData.saveFailedTimes(mContext);
            }
        });
    }

    private void confirmPassword() {
        if (isPasswordRight(mInputPatternData.getDrawPattern())) {
            mFingerCheckFail = false;
            resumeSeviceFlagAndRecoverFinger();
            mInputPatternData.setFailedTimes(0);
            clearInputPatternData();
            if (!(mIsModifyPassword || mIsNeedResult)) {
                openSafe();
            }
            Intent intent = new Intent();
            if (mIsNeedResult) {
                intent.putExtra(SafeConstants.PASSWORD, mInputPatternData.getDrawPattern());
            }
            intent.putExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, SafeConstants.LOCK_MODE_PATTERN);
            intent.putExtra(SafeConstants.HEADER_TIP, mTextHead.getText());
            intent.putExtra(SafeConstants.SUB_TIP, mTextSub.getText());
            setResult(RESULT_OK, intent);
            finish();
            return;
        }
        inputPasswordFailChecked();
        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
    }

    private void postClearPatternRunnable() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, 2000);
    }

    private void resumeSeviceFlagAndRecoverFinger() {
        InputPasswordManager.setFingerPrintTimes(getApplicationContext(), 0);
    }

    private void saveSelect() {
        setPasswordMode(SafeConstants.LOCK_MODE_PATTERN);
    }

    protected void setPasswordMode(int mode) {
        String lockModePath = SafeUtils.getSafeFilePath(SafeConstants.SAFE_ROOT_PATH, SafeConstants.LOCK_MODE_PATH);
        SafeUtils.saveLockMode(this, mode, lockModePath);
    }

    private boolean isPasswordRight(String input) {
        Cipher cipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY, Cipher.ENCRYPT_MODE);
        String encryptionInput = SafeUtils.encrypString(input, cipher);
        String lockPsPath = SafeUtils.getSafeFilePath(SafeConstants.SAFE_ROOT_PATH, SafeConstants.LOCK_PASSWORD_PATH);
        String password = SafeUtils.getPassword(this, lockPsPath);
        return encryptionInput != null && encryptionInput.equals(password);
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
                    SafeUtils.savePassword(mContext, password,
                            SafeUtils.getSafeFilePath(path, SafeConstants.LOCK_PASSWORD_PATH));
                    savePasswordAndMode(SafeConstants.LOCK_MODE_PATTERN, password);
                    Intent intent = new Intent();
                    if (mIsResetPassword) {
                        if (mIsNeedResult) {
                            intent.putExtra(SafeConstants.PASSWORD, password);
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

    private void saveChosenPatternAndFinish() {
        String input = mInputPatternData.getDrawPattern();
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
        mPassword = input;
        mLockPatternUtils.setVisiblePatternEnabled(true, UserHandle.myUserId());
        if (mIsModifyPassword || mIsResetPassword) {
            encrypWithNewPassword(input, safeFilePath);
            return;
        }
        SafeUtils.savePassword(this, input,
                SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_PASSWORD_PATH));
        savePasswordAndMode(SafeConstants.LOCK_MODE_PATTERN, input);
        if (!mIsNeedResult) {
            openSafe();
        }
        setResult(RESULT_OK);
        finish();
    }

    private void judgeActionAndSetMode() {
        mIsNeedResult = mIntent.getBooleanExtra(SafeConstants.IS_NEED_RESULT, false);
        mIsFirstSet = mIntent.getBooleanExtra(SafeConstants.IS_FIRST_SET, false);
        mIsResetPassword = mIntent.getBooleanExtra(SafeConstants.IS_RESET_PASSWORD, false);
        mIsModifyPassword = mIntent.getBooleanExtra(SafeConstants.IS_MODIFY_PASSWORD, false);
        mIsNeedOpenSafe = mIntent.getBooleanExtra(SafeConstants.IS_NEED_OPEN_SAFE, false);
        mEntryModifyMode = mIntent.getBooleanExtra(SafeConstants.MODIFY_PASSWORD, false);

        if (mAction != null) {
            switch (mAction) {
                case SafeConstants.NEW_APP_PROTECT_PATTERN:
                    mCurMode = MODE_ADD_NEW;
                    break;
                case SafeConstants.APP_UNLOCK_PATTERN_ACTIVITY:
                    mCurMode = MODE_CHECK;
                    break;
                default:
                    break;
            }
        }
    }

    private void handleConfirmation(int mode) {
        switch (mode) {
            case MODE_ADD_NEW: {
                if (isPasswordRight(mInputPatternData.getDrawPattern())) {
                    mCurMode = MODE_ADD_NEW;
                    mInputPatternData.setDrawPattern(null);
                    mInputPatternData.setInputEnable(true);
                    clearInputPatternData();
                    mInputPatternData.setNewPatternOrNot(true);
                    updateUiToMode(mCurMode);
                    mInputPatternData.setInputTipNewFailState();
                    invalidateTipText();
                    mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    break;
                }
                mInputPatternData.setFailedTimes(0);
                mInputPatternData.setNewPatternFirstSucessPress();
                invalidateTipText();
                mCurMode = MODE_NEW_CONFIRM;
                mOtherEncryptionMethods.setVisibility(View.GONE);
                break;
            }
            case MODE_NEW_CONFIRM: {
                String string = mInputPatternData.getTwiceDrawPattern();
                invalidateTipText();
                if (string == null || !mInputPatternData.getDrawPattern().equals(string)) {
                    postClearPatternRunnable();
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                }
                mInputPatternData.setFailedTimes(0);
                clearInputPatternData();
                saveSelect();
                saveChosenPatternAndFinish();
                break;
            }
            case MODE_CHECK: {
                mInputPatternData.setFailedTimes(0);
                clearInputPatternData();
                if (isPasswordRight(mInputPatternData.getDrawPattern())) {
                    resumeSeviceFlagAndRecoverFinger();
                    if (!(mIsModifyPassword || mIsNeedResult)) {
                        openSafe();
                    }
                    Intent intent = new Intent();
                    if (mIsNeedResult) {
                        intent.putExtra(SafeConstants.PASSWORD, mInputPatternData.getDrawPattern());
                    }
                    intent.putExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, SafeConstants.LOCK_MODE_PATTERN);
                    intent.putExtra(SafeConstants.HEADER_TIP, mTextHead.getText());
                    intent.putExtra(SafeConstants.SUB_TIP, mTextSub.getText());
                    setResult(RESULT_OK, intent);
                    finish();
                    break;
                }
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                postClearPatternRunnable();
                break;
            }
            default:
                break;
        }
    }

    private void drawPatternShort() {
        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
        if (!mFingerCheckFail) {
            if (mInputPatternData.isNewPattern()) {
                if (mInputPatternData.getDrawPattern() == null) {
                    mInputPatternData.setNewPatternNeedFourPoint();
                } else {
                    mInputPatternData.setNewPatternTwiceNeedFourPoint();
                }
            }
            invalidateTipText();
        }
        postClearPatternRunnable();
    }

    private void checkNewPattern(List<Cell> pattern) {
        if (mInputPatternData.getDrawPattern() == null) {
            firstDrawPattern(pattern);
        } else {
            secondDrawPattern(pattern);
        }
    }

    private void firstDrawPattern(List<Cell> pattern) {
        mInputPatternData.setDrawPattern(patternToString(pattern));
        mInputPatternData.setNewPatternFirstSucess();
        invalidateTipText();
        enableLockPatternView(false);
        if (mCurMode == MODE_ADD_NEW) {
            if (isPasswordRight(mInputPatternData.getDrawPattern())) {
                mCurMode = MODE_ADD_NEW;
                mInputPatternData.setDrawPattern(null);
                mInputPatternData.setInputEnable(true);
                clearInputPatternData();
                mInputPatternData.setNewPatternOrNot(true);
                updateUiToMode(mCurMode);
                mInputPatternData.setInputTipNewFailState();
                invalidateTipText();
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                postClearPatternRunnable();
            } else {
                mButtonNext.setText(R.string.password_next);
                mButtonNext.setEnabled(true);
            }
        }
    }

    private void secondDrawPattern(List<Cell> pattern) {
        mInputPatternData.setTwiceDrawPattern(patternToString(pattern));
        if (mInputPatternData.getDrawPattern().equals(mInputPatternData.getTwiceDrawPattern())) {
            secondDrawPatternSuccess();
        } else {
            secondDrawPatternFailed();
        }
    }

    private void secondDrawPatternSuccess() {
        mButtonNext.setText(R.string.completed);
        mButtonNext.setEnabled(true);
        mInputPatternData.setNewPatternTwice();
        enableLockPatternView(false);
        invalidateTipText();
    }

    private void secondDrawPatternFailed() {
        mButtonNext.setText(R.string.password_next);
        mButtonNext.setEnabled(false);
        mInputPatternData.setDrawPattern(null);
        mCurMode = MODE_ADD_NEW;
        mOtherEncryptionMethods.setVisibility(View.VISIBLE);
        mInputPatternData.setInputEnable(true);
        clearInputPatternData();
        mInputPatternData.setNewPatternOrNot(true);
        updateUiToMode(mCurMode);
        if (mIsModifyPassword) {
            mInputPatternData.setModifyPatternConfirmWrong();
        } else {
            mInputPatternData.setNewPatternConfirmWrong();
        }
        invalidateTipText();
        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
        postClearPatternRunnable();
    }

    private void enableLockPatternView(boolean able) {
        if (able) {
            mLockPatternView.clearPattern();
            mLockPatternView.enableInput();
            return;
        }
        mLockPatternView.disableInput();
    }

    private void checkUnlockPattern(List<Cell> pattern) {
        mInputPatternData.setDrawPattern(patternToString(pattern));
        confirmPassword();
    }

    private void recoverFingerFailedFlag() {
        mInputPatternData.setFingerFailedFlag(false);
        mInputPatternData.setFingerPrintState(STATE_ENABLE_NOT_FINGER_PRINT);
    }

    private void cancelTimerAndSetFailedData() {
        if (mFiveFailedInputTimer != null) {
            mInputPatternData.setFiveFailedData(this, true,
                    mFiveFailedInputTimer.getCountRestTime());
            mFiveFailedInputTimer.cancel();
            mFiveFailedInputTimer = null;
        }
    }

    private void cancelFingerFaiedTimer() {
        if (mFingerFailedInputTimer != null) {
            mInputPatternData.setFingerFailedCountDown(this, true,
                    mFingerFailedInputTimer.getCountRestTime());
            mFingerFailedInputTimer.cancel();
            mFingerFailedInputTimer = null;
        }
    }

    private String patternToString(List<Cell> pattern) {
        if (pattern == null) {
            return "";
        }
        int patternSize = pattern.size();
        byte[] res = new byte[patternSize];
        for (int i = 0; i < patternSize; i++) {
            Cell cell = pattern.get(i);
            res[i] = (byte) ((cell.getRow() * 3) + cell.getColumn());
        }
        return new String(res);
    }
}
