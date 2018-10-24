package com.freeme.applock;

public interface AppLockViewStateCallback {
    void failVerify();

    void succeedVerify();

    void updateView();
}
