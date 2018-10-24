/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.keyguard;

import static com.android.keyguard.LatencyTracker.ACTION_CHECK_CREDENTIAL;
import static com.android.keyguard.LatencyTracker.ACTION_CHECK_CREDENTIAL_UNLOCKED;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;

import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.freeme.keyguard.FingerprintHintView;
import com.freeme.systemui.utils.AnimUtils;

/**
 * Base class for PIN and password unlock screens.
 */
public abstract class KeyguardAbsKeyInputView extends LinearLayout
        implements KeyguardSecurityView, EmergencyButton.EmergencyButtonCallback {
    protected KeyguardSecurityCallback mCallback;
    protected LockPatternUtils mLockPatternUtils;
    protected AsyncTask<?, ?, ?> mPendingLockCheck;
    protected SecurityMessageDisplay mSecurityMessageDisplay;
    protected View mEcaView;
    protected boolean mEnableHaptics;
    private boolean mDismissing;
    private CountDownTimer mCountdownTimer = null;

    // To avoid accidental lockout due to events while the device in in the pocket, ignore
    // any passwords with length less than or equal to this length.
    protected static final int MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT = 3;

    public KeyguardAbsKeyInputView(Context context) {
        this(context, null);
    }

    public KeyguardAbsKeyInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
        mEnableHaptics = mLockPatternUtils.isTactileFeedbackEnabled();
    }

    @Override
    public void reset() {
        // start fresh
        mDismissing = false;
        resetPasswordText(false /* animate */, false /* announce */);
        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline(
                KeyguardUpdateMonitor.getCurrentUser());
        if (shouldLockout(deadline)) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }

    // Allow subclasses to override this behavior
    protected boolean shouldLockout(long deadline) {
        return deadline != 0;
    }

    protected abstract int getPasswordTextViewId();
    protected abstract void resetState();

    @Override
    protected void onFinishInflate() {
        mLockPatternUtils = new LockPatternUtils(mContext);
        mSecurityMessageDisplay = KeyguardMessageArea.findSecurityMessageDisplay(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);

        EmergencyButton button = findViewById(R.id.emergency_call_button);
        if (button != null) {
            button.setCallback(this);
        }

        //*/ freeme.gouzhouping, 20180125. FreemeAppTheme, keyguard.
        mPasswordLength = getLockPasswordLength();
        //*/

        //*/ freeme.gouzhouping, 20180205. FreemeAppTheme, fp.
        mFingerprintHintView = FingerprintHintView.findFingerprintHintView(this);
        //*/
    }

    @Override
    public void onEmergencyButtonClickedWhenInCall() {
        mCallback.reset();
    }

    /*
     * Override this if you have a different string for "wrong password"
     *
     * Note that PIN/PUK have their own implementation of verifyPasswordAndUnlock and so don't need this
     */
    protected int getWrongPasswordStringId() {
        return R.string.kg_wrong_password;
    }

    protected void verifyPasswordAndUnlock() {
        if (mDismissing) return; // already verified but haven't been dismissed; don't do it again.

        final String entry = getPasswordText();
        setPasswordEntryInputEnabled(false);
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
        }

        final int userId = KeyguardUpdateMonitor.getCurrentUser();
        if (entry.length() <= MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT) {
            // to avoid accidental lockout, only count attempts that are long enough to be a
            // real password. This may require some tweaking.
            setPasswordEntryInputEnabled(true);
            onPasswordChecked(userId, false /* matched */, 0, false /* not valid - too short */);
            return;
        }

        if (LatencyTracker.isEnabled(mContext)) {
            LatencyTracker.getInstance(mContext).onActionStart(ACTION_CHECK_CREDENTIAL);
            LatencyTracker.getInstance(mContext).onActionStart(ACTION_CHECK_CREDENTIAL_UNLOCKED);
        }
        mPendingLockCheck = LockPatternChecker.checkPassword(
                mLockPatternUtils,
                entry,
                userId,
                new LockPatternChecker.OnCheckCallback() {

                    @Override
                    public void onEarlyMatched() {
                        if (LatencyTracker.isEnabled(mContext)) {
                            LatencyTracker.getInstance(mContext).onActionEnd(
                                    ACTION_CHECK_CREDENTIAL);
                        }
                        onPasswordChecked(userId, true /* matched */, 0 /* timeoutMs */,
                                true /* isValidPassword */);
                    }

                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        if (LatencyTracker.isEnabled(mContext)) {
                            LatencyTracker.getInstance(mContext).onActionEnd(
                                    ACTION_CHECK_CREDENTIAL_UNLOCKED);
                        }
                        setPasswordEntryInputEnabled(true);
                        mPendingLockCheck = null;
                        if (!matched) {
                            onPasswordChecked(userId, false /* matched */, timeoutMs,
                                    true /* isValidPassword */);
                        }
                    }

                    @Override
                    public void onCancelled() {
                        // We already got dismissed with the early matched callback, so we cancelled
                        // the check. However, we still need to note down the latency.
                        if (LatencyTracker.isEnabled(mContext)) {
                            LatencyTracker.getInstance(mContext).onActionEnd(
                                    ACTION_CHECK_CREDENTIAL_UNLOCKED);
                        }
                    }
                });
    }

    private void onPasswordChecked(int userId, boolean matched, int timeoutMs,
            boolean isValidPassword) {
        boolean dismissKeyguard = KeyguardUpdateMonitor.getCurrentUser() == userId;
        if (matched) {
            mCallback.reportUnlockAttempt(userId, true, 0);
            if (dismissKeyguard) {
                mDismissing = true;
                mCallback.dismiss(true, userId);
            }
        } else {
            if (isValidPassword) {
                mCallback.reportUnlockAttempt(userId, false, timeoutMs);
                if (timeoutMs > 0) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            userId, timeoutMs);
                    handleAttemptLockout(deadline);
                    //*/ freeme.gouzhouping, 20180205. FreemeAppTheme, fp.
                    mFingerprintHintView.hideFpHint();
                    //*/
                }
            }
            if (timeoutMs == 0) {
                mSecurityMessageDisplay.setMessage(getWrongPasswordStringId());
            }
        }
        resetPasswordText(true /* animate */, !matched /* announce deletion if no match */);
    }

    protected abstract void resetPasswordText(boolean animate, boolean announce);
    protected abstract String getPasswordText();
    protected abstract void setPasswordEntryEnabled(boolean enabled);
    protected abstract void setPasswordEntryInputEnabled(boolean enabled);

    // Prevent user from using the PIN/Password entry until scheduled deadline.
    protected void handleAttemptLockout(long elapsedRealtimeDeadline) {
        setPasswordEntryEnabled(false);
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long secondsInFuture = (long) Math.ceil(
                (elapsedRealtimeDeadline - elapsedRealtime) / 1000.0);
        mCountdownTimer = new CountDownTimer(secondsInFuture * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) Math.round(millisUntilFinished / 1000.0);
                mSecurityMessageDisplay.formatMessage(
                        R.string.kg_too_many_failed_attempts_countdown, secondsRemaining);
            }

            @Override
            public void onFinish() {
                /*/ freeme.gouzhouping, 20180205. FreemeAppTheme fp.
                mSecurityMessageDisplay.setMessage("", false);
                /*/
                mSecurityMessageDisplay.setMessage(getPromtReasonStringRes(PROMPT_REASON_TIMEOUT));
                //*/
                resetState();
            }
        }.start();
    }

    protected void onUserInput() {
        if (mCallback != null) {
            mCallback.userActivity();
        }
        //*/ freeme.gouzhouping, 20180125. FreemeAppTheme, keyguard.
        resetKgMessage();
        /*/
        mSecurityMessageDisplay.setMessage("");
        //*/
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        onUserInput();
        return false;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
            mPendingLockCheck = null;
        }
    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void showPromptReason(int reason) {
        if (reason != PROMPT_REASON_NONE) {
            int promtReasonStringRes = getPromtReasonStringRes(reason);
            if (promtReasonStringRes != 0) {
                mSecurityMessageDisplay.setMessage(promtReasonStringRes);
            }
        }
    }

    @Override
    public void showMessage(String message, int color) {
        mSecurityMessageDisplay.setNextMessageColor(color);
        mSecurityMessageDisplay.setMessage(message);
    }

    protected abstract int getPromtReasonStringRes(int reason);

    // Cause a VIRTUAL_KEY vibration
    public void doHapticKeyClick() {
        if (mEnableHaptics) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                    | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    //*/ freeme.gouzhouping, 20180125. FreemeAppTheme, keyguard.
    protected abstract void resetKgMessage();
    //*/

    //*/ freeme.gouzhouping, 20180125. FreemeAppTheme, auto unlock.
    public  static final boolean AUTO_UNLOCK_DEBUG = true;
    private static final String TAG = "KeyguardAbsKeyInputView";

    protected static final int UNLOCK_TO_MASTER  = 10;
    protected static final int UNLOCK_TO_VISITOR = 11;

    private int mPasswordLength = -1;
    private int mPasswordLengthVisitor = -1;
    private String mVisitorModeState = null;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            unlockPasswordForAuto(msg.arg1);
        }
    };

    public void verifyPasswordAuto() {
        if (AUTO_UNLOCK_DEBUG) {
            Log.d(TAG, "verifyPasswordAuto");
        }
        if (AntiTheftManager.getInstance(null, null, null).isAntiTheftLocked()) {
            Log.d(TAG, "in anti theft mode,so we can't auto unlock!");
            return;
        }
        String entry = getPasswordText();
        if (!TextUtils.isEmpty(entry)) {
            if (mPasswordLength != -1 && entry.length() == mPasswordLength) {
                verifyPasswordAndUnlock();
            }
        }
    }

    protected int verifyPasswordNotUnlock() {
        int result = -1;
        String entry = getPasswordText();
        boolean check =false;

        try {
            check =mLockPatternUtils.checkPassword(entry,KeyguardUpdateMonitor.getCurrentUser());
        } catch (Exception e) {
        }
        if (check) {
            result = UNLOCK_TO_MASTER;
        }
        return result;
    }

    protected void unlockPasswordForAuto(int resultcode) {
        int userId = KeyguardUpdateMonitor.getCurrentUser();
        if (resultcode == UNLOCK_TO_MASTER) {
            mCallback.reportUnlockAttempt(userId, true, 0);
            mCallback.dismiss(true, userId);
        }
        resetPasswordText(true /* animate */, false);
    }

    public int getLockPasswordLength() {
        String temp = mLockPatternUtils.getPasswordLength(LockPatternUtils.PASSWORD_KEY_LENGTH,
                KeyguardUpdateMonitor.getCurrentUser());
        if (temp == null || temp.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(temp);
    }
    //*/

    //*/ freeme.gouzhouping, 20180205. FreemeAppTheme, fp.
    private FingerprintHintView mFingerprintHintView;
    protected abstract boolean getPasswordEntryEnabledState();

    public void showHintView() {
        if (mFingerprintHintView != null && getPasswordEntryEnabledState()) {
            mFingerprintHintView.showFpHint();
        }
    }

    public void hideHintView() {
        if (mFingerprintHintView != null) {
            mFingerprintHintView.hideFpHint();
        }
    }
    //*/

    //*/ freeme.gouzhouping, 20180919. keyguard animation.
    protected boolean appearAnimation(Runnable endRunnable) {
        return AnimUtils.startEnterSecurityViewAnimation(this, endRunnable);
    }

    protected boolean disappearAnimation(Runnable endRunnable) {
        return AnimUtils.startExitSecurityViewAnimation(this, endRunnable);
    }
    //*/
}

