package com.freeme.applock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.ServiceManager;
import android.util.Log;
import android.view.IWindowManager.Stub;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import com.android.internal.widget.LockPatternUtils;
import com.freeme.applock.settings.LogUtil;

public class AppLockConfirmTestActivity extends Activity {
    private static final String TAG = "AppLockConfirmTest";
    private Context mContext;
    protected LockPatternUtils mLockPatternUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        mContext = this;
        findViewById(R.id.bt_2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, AppLockPinConfirmActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, 2);
            }
        });
        findViewById(R.id.bt_3).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, AppLockPatternBackupPinConfirmActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, 3);
            }
        });
        findViewById(R.id.bt_4).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, AppLockPasswordConfirmActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, 4);
            }
        });
        findViewById(R.id.bt_5).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, AppLockPatternConfirmActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, 5);
            }
        });
    }

    protected int getScreenOrientation() {
        return getResources().getConfiguration().orientation;
    }

    protected boolean isPortrait() {
        return getScreenOrientation() == Configuration.ORIENTATION_PORTRAIT;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        LogUtil.d(TAG, "onWindowFocusChanged: " + hasFocus);
        if (!hasFocus) {
            hideVirtualKeypad();
        }
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onStop() {
        LogUtil.d(TAG, "onStop");
        super.onStop();
    }

    protected void hideVirtualKeypad() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = getCurrentFocus();
        if (v != null && inputManager != null) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    protected void verifySuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LogUtil.i(TAG, "requestCode:" + requestCode + ", received - resultCode:" + resultCode + " Activity.RESULT_OK = " + -1);
        switch (requestCode) {
            case 1:
                if (resultCode <= 0) {
                    // empty
                }
                break;
            case 2:
            case 3:
                if (resultCode == 0) {
                    // empty
                }
                break;
            case 4:
                if (resultCode != -1) {
                    // empty
                }
                break;
            default:
                break;
        }
    }
}
