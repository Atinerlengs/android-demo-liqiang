package com.freeme.safe.password;

import android.os.CountDownTimer;

public class FiveFailedInputTimer extends CountDownTimer {
    private CountdownTimerListener mTimerListener;
    private long mCountRestTime;

    FiveFailedInputTimer(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }

    @Override
    public void onFinish() {
        mTimerListener.onTimerFinish();
        setCountRestTime(0);
    }

    @Override
    public void onTick(long millisUntilFinished) {
        mTimerListener.onTimerTick(millisUntilFinished);
        setCountRestTime(millisUntilFinished);
    }

    void setCountdownTimerListener(CountdownTimerListener listener) {
        mTimerListener = listener;
    }

    long getCountRestTime() {
        return mCountRestTime;
    }

    void setCountRestTime(long time) {
        mCountRestTime = time;
    }
}
