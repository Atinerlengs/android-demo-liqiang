package com.freeme.dialer.utils;

import android.content.Intent;

public class FreemeEntranceRequst {

    public static final String CLASS_NAME_CONTACTS = "com.freeme.dialer.app.ContactsActivity";
    private final String CLASS_NAME_DIALER = "com.freeme.dialer.app.FreemeDialtactsActivity";
    private final String CLASS_NAME_DIALER_ALIAS = "com.android.dialer.DialtactsActivity";

    public final static int ENTRANCE_NOCHANGE = 0;
    public final static int ENTRANCE_DAIL = 10;
    public final static int ENTRANCE_CONTACTS = 20;

    private int mEntranceCode = 0;
    private boolean mIsRecreatedInstance;
    private boolean mIsFromNewIntent;

    public void resolveIntent(Intent intent) {
        String className = intent.getComponent().getClassName();
        switch (className) {
            case CLASS_NAME_CONTACTS:
                mEntranceCode = ENTRANCE_CONTACTS;
                break;
            case CLASS_NAME_DIALER:
            case CLASS_NAME_DIALER_ALIAS:
                mEntranceCode = ENTRANCE_DAIL;
                break;
            default:
                break;
        }
        if (!mIsFromNewIntent && mIsRecreatedInstance) {
            mEntranceCode = ENTRANCE_NOCHANGE;
        }
    }

    public int getEntranceCode() {
        return mEntranceCode;
    }

    public void setIsRecreatedInstance(boolean isRecreatedInstance) {
        this.mIsRecreatedInstance = isRecreatedInstance;
    }

    public boolean isRecreatedInstance() {
        return mIsRecreatedInstance;
    }

    public void setIsFromNewIntent(boolean isFromNewIntent) {
        this.mIsFromNewIntent = isFromNewIntent;
    }

    public boolean isFromNewIntent() {
        return mIsFromNewIntent;
    }
}
