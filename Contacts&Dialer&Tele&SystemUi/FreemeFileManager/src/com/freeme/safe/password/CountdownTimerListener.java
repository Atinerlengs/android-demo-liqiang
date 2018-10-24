package com.freeme.safe.password;

public interface CountdownTimerListener {
    void onTimerFinish();
    void onTimerTick(long time);
}
