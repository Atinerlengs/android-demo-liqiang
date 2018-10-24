package com.freeme.safe.password;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.freeme.safe.encryption.service.EncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService.Stub;
import com.freeme.filemanager.R;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;
import com.freeme.safe.utils.StaticHandler;
import com.freeme.safe.view.PrivateSpaceActivity;

public class FingerPrintActivity extends Activity {

    protected static final String TAG = "FingerPrintActivity";

    public static final int PASSWORD_FAILED_TIMES = 5;

    protected static final int MODE_ADD_NEW = 10;
    protected static final int MODE_NEW_CONFIRM = 11;
    protected static final int MODE_CHECK = 30;
    protected static final int FIVE_FAILED_COUNT_TIME = 30000;

    protected boolean mFingerCheckFail;
    protected boolean mFingerprintEnabled;
    protected boolean mIsFirstSet;
    protected boolean mIsModifyPassword;
    protected boolean mIsNeedOpenSafe;
    protected int mCurMode = -1;

    private Vibrator mVibrator;

    protected FingerHandler mHandler = new FingerHandler(this);
    protected static class FingerHandler extends StaticHandler<FingerPrintActivity> {
        FingerHandler(FingerPrintActivity a) {
            super(a);
        }

        @Override
        protected void handleMessage(Message msg, FingerPrintActivity t) {
        }
    }

    protected IEncryptionService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "onServiceConnected");
            }
            mService = Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        bindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
    }

    private void bindService() {
        if (mConnection != null) {
            Intent intent = new Intent(this, EncryptionService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    protected void setTitle(TextView textview) {
        if (mIsFirstSet) {
            textview.setText(R.string.lock_title_settings);
        } else if (mIsModifyPassword) {
            textview.setText(R.string.lock_title_change);
        } else {
            textview.setText(R.string.enter_password);
        }
    }

    protected boolean isFingerprintUnlockEnabled() {
        return false;
    }

    protected void openSafe() {
        Intent intent = new Intent();
        intent.setClass(this, PrivateSpaceActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(SafeConstants.IS_FIRST_SET, mIsFirstSet);
        startActivity(intent);
    }

    protected void doHapticKeyLongClick() {
        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
        if (mVibrator != null) {
            mVibrator.vibrate(new long[]{0, 300}, -1);
        }
    }

    protected void savePasswordAndMode(int mode, String password) {
        try {
            if (mService != null) {
                mService.savePasswordAndMode(mode, password);
                //System.putInt(getContentResolver(), SafeConstants.ENCRYPTION_STATE, 1);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected int getFailedAttempts() {
        return 0;
    }

    private long getLockoutAttemptDeadline() {
        return 0;
    }

    protected long getLockoutAttemptCountDownTime() {
        if (SafeUtils.isNeededSdk()) {
            return getLockoutAttemptDeadline() - SystemClock.elapsedRealtime();
        }
        return 0;
    }

    protected void invalidateTipStage() {
    }
}
