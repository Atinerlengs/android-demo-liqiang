package com.freeme.safe.password;

import android.content.Context;

import com.freeme.filemanager.R;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import javax.crypto.Cipher;

public class InputPasswordData {

    static final int STATE_HAS_NOT_FINGER_PRINT    = 0;
    static final int STATE_ENABLE_OK_FINGER_PRINT  = 1;
    static final int STATE_ENABLE_NOT_FINGER_PRINT = 2;

    static final int PASSWORD_MAX_LEN = 16;
    static final int PASSWORD_MIN_LEN = 4;
    static final int PASSWORD_NUM_LEN = 4;

    static final int PASSWORD_SIMPLE  = 1;
    static final int PASSWORD_COMPLEX = 5;

    private static final int FAILED_TIMES = FingerPrintActivity.PASSWORD_FAILED_TIMES;

    private static InputPasswordData mInputPasswordTipData;

    private String mFirstSetPassword;
    private String[] mInputSimplePassword;

    private boolean mInputEnable;
    private boolean mIsNewPassword;
    private boolean mFingerFailedFlag;

    private int mCurrentFailedTimes;
    private int mFailedTimes;
    private int mFingerPrintState;

    private Stage mTipStage = Stage.InputPassword;
    public enum Stage {
        InputPassword(R.string.input_password, -1, -1, -1),
        InputOldPassword(R.string.lockpassword_old_passcode, -1, -1, -1),
        InputNewPassword(R.string.lockpassword_new_passcode, -1, -1, -1),
        InputNewPasswordFail(R.string.lockpassword_new_passcode, R.string.lock_password_repeated, -1, -1),
        InputPasswordFail(R.string.input_password, -1, R.string.password_controller_password_wrong, -1),
        InputPasswordFailFingerEnable(R.string.secure_enter_password_tip6, -1, R.string.password_controller_password_wrong, -1),
        InputPasswordFailFingerUnable(R.string.input_password, R.string.secure_enter_password_tip5, -1, -1),
        InputFinger(R.string.secure_enter_password_tip6, -1, -1, -1),
        InputFingerFail(R.string.secure_enter_password_tip3, -1, -1, -1),
        InputFingerFailGood(R.string.try_again_fingerprint_or_password, R.string.no_matching_fingerprint, -1, -1),
        InputFingerFailStains(R.string.try_again_fingerprint_or_password, R.string.wipe_any_stains_from_the_fingerprint_button, -1, -1),
        InputFingerFailCovers(R.string.try_again_fingerprint_or_password, R.string.ensure_that_your_finger_covers_the_fingerprint_button, -1, -1),
        InputFingerFiveFail(R.string.input_password, R.string.secure_enter_password_tip5, -1, -1),
        InputFingerUnable(R.string.input_password, R.string.secure_enter_password_tip5, -1, -1),
        InputFingerFailTimeout(R.string.input_password, -1, R.string.keyguard_fingerprint_password_too_many_failed_attempts_countdown, -1),
        InputFingerFailTryAgain(R.string.keyguard_fingerprint_password_again_enter_code, -1, R.string.password_controller_password_wrong, -1),
        InputUnable(-1, -1, -1, R.string.failed_attempts_five_times_countdown),
        InputFailAfterFingerFailed(R.string.secure_enter_password_tip6, -1, R.string.password_controller_password_wrong, -1),
        NewComplexLengthShort(-1, -1, R.string.lockpassword_password_requires_alpha, -1),
        NewComplexLengthLong(R.string.input_password, R.string.lockpassword_password_too_long, -1, -1),
        ModifyComplexLengthLong(R.string.lockpassword_new_passcode, R.string.lockpassword_password_too_long, -1, -1),
        NewComplexIllegalCharacters(R.string.lockpassword_illegal_character, -1, -1, -1),
        NewComplexLettersShort(-1, -1, R.string.lockpassword_password_requires_alpha, -1),
        NewComplexFirstNormal(R.string.lockpassword_password_normal_next, -1, -1, -1),
        NewConfirmPassword(R.string.confirm_alert_dialog_new_password, -1, -1, -1),
        ModifyConfirmPasswordWrong(R.string.lockpassword_new_passcode, R.string.password_controller_password_confirm_error, -1, -1),
        NewConfirmPasswordWrong(R.string.input_password, R.string.password_controller_password_confirm_error, -1, -1);

        public final int mFailMessage;
        public final int mFiveFailed;
        public final int mHeaderMessage;
        public final int mSubMessage;

        Stage(int headerMessage, int subMessage, int failMessage, int fiveFailed) {
            mHeaderMessage = headerMessage;
            mSubMessage = subMessage;
            mFailMessage = failMessage;
            mFiveFailed = fiveFailed;
        }
    }

    private InputPasswordData(int type) {
        initSimplePassword(type);
    }

    public static synchronized InputPasswordData getInstance(Context context, int passwordType) {
        InputPasswordData inputPasswordData;
        synchronized (InputPasswordData.class) {
            if (mInputPasswordTipData == null && context != null) {
                mInputPasswordTipData = new InputPasswordData(passwordType);
            }
            inputPasswordData = mInputPasswordTipData;
        }
        return inputPasswordData;
    }

    private void setTipStage(Stage stage) {
        mTipStage = stage;
    }

    String getStageHeaderTip(Context context) {
        if (mTipStage.mHeaderMessage != -1) {
            return context.getResources().getString(mTipStage.mHeaderMessage);
        }
        return "";
    }

    String getStageSubTip(Context context) {
        if (mTipStage.mSubMessage != -1) {
            return context.getResources().getString(mTipStage.mSubMessage);
        }
        return "";
    }

    String getStageFailedTip(Context context, int times) {
        if (mTipStage.mFailMessage == -1) {
            return "";
        }
        return context.getResources().getString(mTipStage.mFailMessage, times);
    }

    String getStageFiveFailedTip(Context context, long waitTime) {
        if (mTipStage.mFiveFailed == -1) {
            return "";
        }
        return context.getResources().getString(mTipStage.mFiveFailed, waitTime);
    }

    private int getFingerPrintState() {
        return mFingerPrintState;
    }

    void setFingerPrintState(int fingerPrintState) {
        mFingerPrintState = fingerPrintState;
    }

    void setInputFailTipStage(boolean inFiveInputTimes) {
        if (!inFiveInputTimes) {
            setTipStage(Stage.InputUnable);
        } else if (getFingerFailedFlag()) {
            setTipStage(Stage.InputFailAfterFingerFailed);
        } else {
            switch (getFingerPrintState()) {
                case STATE_HAS_NOT_FINGER_PRINT:
                    setTipStage(Stage.InputPasswordFail);
                    break;
                case STATE_ENABLE_OK_FINGER_PRINT:
                    setTipStage(Stage.InputPasswordFailFingerEnable);
                    break;
                case STATE_ENABLE_NOT_FINGER_PRINT:
                    setTipStage(Stage.InputPasswordFailFingerUnable);
                    break;
                default:
                    break;
            }
        }
    }

    void setAreInputTipStage() {
        if (!isNewPassword()) {
            switch (getFingerPrintState()) {
                case STATE_HAS_NOT_FINGER_PRINT:
                    setTipStage(Stage.InputPassword);
                    break;
                case STATE_ENABLE_OK_FINGER_PRINT:
                    setTipStage(Stage.InputFinger);
                    break;
                case STATE_ENABLE_NOT_FINGER_PRINT:
                    setTipStage(Stage.InputFingerUnable);
                    break;
                default:
                    break;
            }
        } else if (getFirstPassword() != null) {
            setTipStage(Stage.NewConfirmPassword);
        }
    }

    void setInputTipStageAndState() {
        setTipStage(Stage.InputPassword);
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputTipOldState() {
        setTipStage(Stage.InputOldPassword);
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputTipNewState() {
        setTipStage(Stage.InputNewPassword);
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputTipNewFailState() {
        setTipStage(Stage.InputNewPasswordFail);
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputFingerFailedTimeout() {
        setTipStage(Stage.InputFingerFailTimeout);
    }

    void updateTipFailTimes(Context context) {
        if (mFailedTimes == 0) {
            mFailedTimes = InputPasswordManager.getInputFailedTimes(context) + 1;
        } else {
            mFailedTimes++;
        }
    }

    void updateFailedTimesTipStage(Context context, boolean printEnabled) {
        if (mFailedTimes >= FAILED_TIMES) {
            setInputFailTipStage(false);
            InputPasswordManager.setFingerPrintTimes(context, FAILED_TIMES);
        } else if (!printEnabled || mFailedTimes <= mCurrentFailedTimes || !getUnlockProtectedAppFlag() || getFingerPrintState() == 2) {
            setInputFailTipStage(true);
        } else {
            setTipStage(Stage.InputFingerFailTryAgain);
        }
    }

    void updateTipStageAndFailTimes(Context context) {
        if (mFailedTimes <= 0) {
            mFailedTimes = InputPasswordManager.getInputFailedTimes(context) + 1;
        } else {
            mFailedTimes++;
        }
        if (mFailedTimes >= FAILED_TIMES) {
            setInputFailTipStage(false);
            InputPasswordManager.setFingerPrintTimes(context, FAILED_TIMES);
        } else if (mFailedTimes <= mCurrentFailedTimes || !getUnlockProtectedAppFlag()) {
            setInputFailTipStage(true);
        } else {
            setTipStage(Stage.InputFingerFailTryAgain);
        }
    }

    int getFailedTimes() {
        return mFailedTimes;
    }

    void setFailedTimes(int times) {
        mFailedTimes = times;
    }

    void setLengthLongStage() {
        setTipStage(Stage.NewComplexLengthLong);
    }

    void setLengthLongModifyStage() {
        setTipStage(Stage.ModifyComplexLengthLong);
    }

    boolean standardNewComplexStage(String password) {
        int letters = 0;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c < ' ' || c > '') {
                setTipStage(Stage.NewComplexIllegalCharacters);
                return false;
            }
            if (c < '0' || c > '9') {
                if (c >= 'A' && c <= 'Z') {
                    letters++;
                } else if (c >= 'a' && c <= 'z') {
                    letters++;
                }
            }
        }
        int mPasswordMinLetters = 1;
        if (letters >= mPasswordMinLetters) {
            return true;
        }
        setTipStage(Stage.NewComplexLettersShort);
        return false;
    }

    void complexShortStage() {
        setTipStage(Stage.NewComplexLengthShort);
    }

    void secondConfirmStage() {
        setTipStage(Stage.NewConfirmPassword);
    }

    void setNewConfirmPasswordWrong() {
        mTipStage = Stage.NewConfirmPasswordWrong;
    }

    void setModifyConfirmPasswordWrong() {
        mTipStage = Stage.ModifyConfirmPasswordWrong;
    }

    void setNewPasswordOrNot(boolean actionNew) {
        mIsNewPassword = actionNew;
    }

    boolean isNewPassword() {
        return mIsNewPassword;
    }

    void setFirstPassword(String password) {
        mFirstSetPassword = password;
    }

    String getFirstPassword() {
        return mFirstSetPassword;
    }

    void setFingerFailedFlag(boolean finger) {
        mFingerFailedFlag = finger;
    }

    private boolean getFingerFailedFlag() {
        return mFingerFailedFlag;
    }

    boolean getPasswordFromDatabase(Context context, String input) {
        Cipher cipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY, Cipher.ENCRYPT_MODE);
        String encryptionInput = SafeUtils.encrypString(input, cipher);
        String passwordPath = SafeUtils.getSafeFilePath(SafeConstants.SAFE_ROOT_PATH, SafeConstants.LOCK_PASSWORD_PATH);
        String password = SafeUtils.getPassword(context, passwordPath);
        return encryptionInput != null && encryptionInput.equals(password);
    }

    private void initSimplePassword(int type) {
        if (type == 1) {
            mInputSimplePassword = new String[]{"", "", "", ""};
        }
    }

    void clearInputPasswordData() {
        int length = mInputSimplePassword.length;
        for (int i = 0; i < length; i++) {
            mInputSimplePassword[i] = "";
        }
    }

    void setInputEnable(boolean able) {
        mInputEnable = able;
    }

    public boolean getInputEnable() {
        return mInputEnable;
    }

    private boolean getUnlockProtectedAppFlag() {
        return false;
    }

    void setFiveFailedData(Context context, boolean save, long time) {
        if (save) {
            InputPasswordManager.saveRestCountTime(context, time);
            InputPasswordManager.saveFiveTimesFailTime(context, System.currentTimeMillis());
            return;
        }
        InputPasswordManager.saveFiveTimesFailTime(context, 0);
        InputPasswordManager.saveRestCountTime(context, 0);
    }

    void saveFailedTimes(Context context) {
        InputPasswordManager.setInputFailedTimes(context, mFailedTimes);
    }

    int getFailedTimes(Context context) {
        int failedTimes = InputPasswordManager.getInputFailedTimes(context);
        mCurrentFailedTimes = failedTimes;
        return failedTimes;
    }

    void onDestroy(Context context) {
        setFingerFailedFlag(false);
        InputPasswordManager.setInputFailedTimes(context, mFailedTimes);
        if (mInputPasswordTipData != null) {
            mInputPasswordTipData = null;
        }
    }
}