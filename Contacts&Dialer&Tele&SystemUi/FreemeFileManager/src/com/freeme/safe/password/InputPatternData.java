package com.freeme.safe.password;

import android.content.Context;
import android.content.Intent;

import com.freeme.filemanager.R;
import com.freeme.safe.utils.SafeConstants;

public class InputPatternData {
    static final int STATE_HAS_NOT_FINGER_PRINT    = 0;
    static final int STATE_ENABLE_OK_FINGER_PRINT  = 1;
    static final int STATE_ENABLE_NOT_FINGER_PRINT = 2;

    private static final int FAILED_TIMES = FingerPrintActivity.PASSWORD_FAILED_TIMES;

    private String mTwiceDrawPattern;
    private String mDrawPattern;
    private boolean mFingerFailedFlag;
    private boolean mInputEnable;
    private boolean mIsNewPassword;
    private boolean mUnlockProtectedAppFlag;
    private int mFailedTimes;
    private int mCurrentFailedTimes;
    private int mFingerPrintState;

    private static InputPatternData mInputPasswordTipData;
    private Stage mTipStage = Stage.InputPattern;

    public enum Stage {
        InputPattern(R.string.pattern_controller_input_pattern, R.string.secure_enter_password_sub_tip2, -1, -1),
        InputOldPattern(R.string.lockpattern_old_passcode, R.string.secure_enter_password_sub_tip2, -1, -1),
        InputNewPattern(R.string.lockpattern_new_passcode, R.string.secure_enter_password_sub_tip2, -1, -1),
        InputNewFailPattern(R.string.lockpattern_new_passcode, R.string.lock_pattern_repeated, -1, -1),
        InputPatternEntry(R.string.pattern_controller_input_pattern, R.string.secure_enter_password_sub_tip2, -1, -1),
        InputPatternFail(R.string.pattern_controller_input_pattern, -1, R.string.password_controller_password_wrong, -1),
        InputPatternFailFingerEnable(R.string.secure_enter_password_tip7, -1, R.string.password_controller_password_wrong, -1),
        InputPatternFailFingerUnable(R.string.pattern_controller_input_pattern, R.string.secure_enter_pattern_tip5, -1, -1),
        InputFinger(R.string.secure_enter_password_tip7, R.string.secure_enter_password_sub_tip2, -1, -1),
        InputFailAfterFingerFailed(R.string.secure_enter_password_tip7, -1, R.string.password_controller_password_wrong, -1),
        InputFingerFail(R.string.secure_enter_password_tip3, -1, -1, -1),
        InputFingerFailGood(R.string.try_again_fingerprint_or_draw_pattern, R.string.no_matching_fingerprint, -1, -1),
        InputFingerFailStains(R.string.try_again_fingerprint_or_draw_pattern, R.string.wipe_any_stains_from_the_fingerprint_button, -1, -1),
        InputFingerFailCovers(R.string.try_again_fingerprint_or_draw_pattern, R.string.ensure_that_your_finger_covers_the_fingerprint_button, -1, -1),
        InputFingerFiveFail(R.string.pattern_controller_input_pattern, R.string.secure_enter_pattern_tip5, -1, -1),
        InputFingerUnable(R.string.pattern_controller_input_pattern, R.string.secure_enter_pattern_tip5, -1, -1),
        InputFingerFailTimeout(R.string.pattern_controller_input_pattern, -1, R.string.keyguard_fingerprint_password_too_many_failed_attempts_countdown, -1),
        InputFingerFailTryAgain(R.string.keyguard_fingerprint_pattern_again_enter_code, -1, R.string.password_controller_password_wrong, -1),
        InputUnable(-1, -1, -1, R.string.failed_attempts_five_times_countdown),
        NewPatternProgress(R.string.pattern_controller_pattern_recording_inprogress, -1, -1, -1),
        NewPatternNeedFourPoint(R.string.pattern_controller_input_pattern, R.string.pattern_controller_pattern_too_short, -1, -1),
        NewPatternFirstSucess(R.string.pattern_controller_pattern_recorded, -1, -1, -1),
        NewPatternFirstSucessPress(R.string.pattern_controller_input_new_pattern, -1, -1, -1),
        NewPatternTwice(R.string.pattern_confirm_twice_tip, -1, -1, -1),
        NewPatternConfirmWrong(R.string.pattern_controller_input_pattern, R.string.password_controller_pattern_confirm_error, -1, -1),
        ModifyPatternConfirmWrong(R.string.lockpattern_new_passcode, R.string.password_controller_pattern_confirm_error, -1, -1),
        NewPatternTwiceNeedFourPoint(R.string.pattern_controller_input_pattern, R.string.pattern_controller_pattern_too_short, -1, -1);

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

    private InputPatternData(Intent intent) {
        setUnlockProtectedAppFlag(intent);
    }

    public static synchronized InputPatternData getInstance(Context context, Intent intent) {
        InputPatternData inputPatternData;
        synchronized (InputPatternData.class) {
            if (mInputPasswordTipData == null && context != null) {
                mInputPasswordTipData = new InputPatternData(intent);
            }
            inputPatternData = mInputPasswordTipData;
        }
        return inputPatternData;
    }

    private void setTipStage(Stage stage) {
        mTipStage = stage;
    }

    public Stage getTipStage() {
        return mTipStage;
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

    String getStageFingerFailedTimeoutTip(Context context, long waitTime) {
        if (mTipStage.mFailMessage == -1) {
            return "";
        }
        return context.getResources().getString(mTipStage.mFailMessage, waitTime);
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
                    setTipStage(Stage.InputPatternFail);
                    break;
                case STATE_ENABLE_OK_FINGER_PRINT:
                    setTipStage(Stage.InputPatternFailFingerEnable);
                    break;
                case STATE_ENABLE_NOT_FINGER_PRINT:
                    setTipStage(Stage.InputPatternFailFingerUnable);
                    break;
                default:
                    break;
            }
        }
    }

    void setInputTipStageAndState(int mode) {
        if (mode == FingerPrintActivity.MODE_ADD_NEW) {
            setTipStage(Stage.InputPattern);
        } else {
            setTipStage(Stage.InputPatternEntry);
        }
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputTipOldState() {
        setTipStage(Stage.InputOldPattern);
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputTipNewState() {
        setTipStage(Stage.InputNewPattern);
        setFingerPrintState(STATE_HAS_NOT_FINGER_PRINT);
    }

    void setInputTipNewFailState() {
        setTipStage(Stage.InputNewFailPattern);
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
        if (mFailedTimes == 0) {
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

    void setNewPatternProgress() {
        mTipStage = Stage.NewPatternProgress;
    }

    void setNewPatternNeedFourPoint() {
        mTipStage = Stage.NewPatternNeedFourPoint;
    }

    void setNewPatternFirstSucess() {
        mTipStage = Stage.NewPatternFirstSucess;
    }

    void setNewPatternFirstSucessPress() {
        mTipStage = Stage.NewPatternFirstSucessPress;
    }

    void setNewPatternTwice() {
        mTipStage = Stage.NewPatternTwice;
    }

    void setNewPatternConfirmWrong() {
        mTipStage = Stage.NewPatternConfirmWrong;
    }

    void setModifyPatternConfirmWrong() {
        mTipStage = Stage.ModifyPatternConfirmWrong;
    }

    void setNewPatternTwiceNeedFourPoint() {
        mTipStage = Stage.NewPatternTwiceNeedFourPoint;
    }

    void setNewPatternOrNot(boolean actionNew) {
        mIsNewPassword = actionNew;
    }

    boolean isNewPattern() {
        return mIsNewPassword;
    }

    void setDrawPattern(String Pattern) {
        mDrawPattern = Pattern;
    }

    String getDrawPattern() {
        return mDrawPattern;
    }

    void setTwiceDrawPattern(String Pattern) {
        mTwiceDrawPattern = Pattern;
    }

    String getTwiceDrawPattern() {
        return mTwiceDrawPattern;
    }

    void setFingerFailedFlag(boolean finger) {
        mFingerFailedFlag = finger;
    }

    private boolean getFingerFailedFlag() {
        return mFingerFailedFlag;
    }

    void setInputEnable(boolean able) {
        mInputEnable = able;
    }

    public boolean getInputEnable() {
        return mInputEnable;
    }

    private void setUnlockProtectedAppFlag(Intent intent) {
        if (intent != null) {
            boolean first = intent.getBooleanExtra(SafeConstants.IS_FIRST_SET, false);
            if (intent.getBooleanExtra(SafeConstants.IS_MODIFY_PASSWORD, false) || first) {
                mUnlockProtectedAppFlag = false;
                return;
            } else {
                mUnlockProtectedAppFlag = true;
                return;
            }
        }
        mUnlockProtectedAppFlag = false;
    }

    private boolean getUnlockProtectedAppFlag() {
        return mUnlockProtectedAppFlag;
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

    void setFingerFailedCountDown(Context context, boolean save, long time) {
        if (save) {
            InputPasswordManager.saveFingerFailedCountTimes(context, time);
            InputPasswordManager.saveFingerFailedCurrentTimes(context, System.currentTimeMillis());
            return;
        }
        InputPasswordManager.saveFingerFailedCountTimes(context, 0);
        InputPasswordManager.saveFingerFailedCurrentTimes(context, 0);
    }

    void saveFailedTimes(Context context) {
        InputPasswordManager.setInputFailedTimes(context, mFailedTimes);
    }

    int getFailedTimes(Context context) {
        int failedTimes = InputPasswordManager.getInputFailedTimes(context);
        mCurrentFailedTimes = failedTimes;
        return failedTimes;
    }

    void onDestory(Context context) {
        setFingerFailedFlag(false);
        InputPasswordManager.setInputFailedTimes(context, mFailedTimes);
        if (mInputPasswordTipData != null) {
            mInputPasswordTipData = null;
        }
    }
}
