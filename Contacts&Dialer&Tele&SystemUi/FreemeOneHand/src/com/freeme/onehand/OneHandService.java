package com.freeme.onehand;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class OneHandService extends Service {
    private static final String TAG = "OneHandService";
    private static final boolean DBG = OneHandConstants.DEBUG;

    static final String EXTRA_FORCE_HIDE = "ForceHide";
    static final String EXTRA_STARTBY_HOMEKEY = "StartByHomeKey";

    private Context mContext;
    private Handler mHandler;
    private final Binder mBinder = new Binder();
    private Configuration mLastConfig;

    private OneHandController mController;
    private OneHandChangeObserver mChangeObserver;
    private OneHandUtils mUtils;
    private OneHandWindowInfo mWinInfo;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() mController=" + mController);

        mContext = this;
        mHandler = new Handler();
        mLastConfig = new Configuration(getResources().getConfiguration());

        if (!DBG) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e(TAG, "uncaughtException() t=" + t, e);
                    stopSelf();
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean portrait = mLastConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        boolean home = false;
        boolean hide = false;
        String action = "";
        if (intent != null) {
            action = intent.getAction();
            hide = intent.getBooleanExtra(EXTRA_FORCE_HIDE, false);
            home = intent.getBooleanExtra(EXTRA_STARTBY_HOMEKEY, false);
        }
        if (DBG) {
            Log.d(TAG, (new StringBuilder(86))
                    .append("onStartCommand() ")
                    .append("action=").append(action).append(' ')
                    .append("portrait=").append(portrait).append(' ')
                    .append("hide=").append(hide).append(' ')
                    .append("home=").append(home)
                    .toString());
        }

        if (OneHandConstants.ACTION_ONEHAND_SERVICE_SCREEN_OFF.equals(action) && mController != null) {
            if (DBG) {
                Log.d(TAG, "ONEHAND_SERVICE_SCREEN_OFF() ");
            }
            if (mWinInfo.mOffsetY > 0 || mController.isBGVisible()) {
                mHandler.postDelayed(new Runnable() {
                            public void run() {
                                mController.returnFullScreen(false, null);
                            }
                        },
                        mController.isAnimationRunning() ? 300 : 0);
            }
            return START_STICKY;
        } else if (hide) {
            Log.d(TAG, "Kill OneHand service");
            if (mController != null) {
                mController.returnFullScreen(false, null);
            }
            stopSelf();
            return START_NOT_STICKY;
        } else {
            if (home && !portrait) {
                Toast.makeText(mContext, R.string.rotate_help_message, Toast.LENGTH_LONG)
                        .show();
            }
            if (mController != null) {
                if (home && portrait) {
                    if (mWinInfo.mOffsetY == 0) {
                        mController.startReduceScreenAnimation();
                    } else {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mController.returnFullScreen(true, null);
                            }
                        }, 100);
                    }
                }
                return START_STICKY;
            }

            super.onStartCommand(intent, flags, startId);

            manageProcessForeground(true);
            if (portrait) {
                initialize();
            }
            return START_STICKY;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DBG) {
            Log.d(TAG, "onConfigurationChanged() orientation=" + newConfig.orientation);
        }
        if (mController == null) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                initialize();
            }
        } else {
            if (mLastConfig.orientation != newConfig.orientation) {
                mController.onOrientationChanged(newConfig);
            }
            if ((!mLastConfig.locale.equals(newConfig.locale))
                    /*FIXME || (mLastConfig.FlipFont != newConfig.FlipFont) */) {
                mController.onFontLocaleChanged();
            }
        }
        mLastConfig.setTo(newConfig);
    }

    @Override
    public void onLowMemory() {
        if (DBG) {
            Log.d(TAG, "onLowMemory()");
        }
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if (DBG) {
            Log.d(TAG, "onTrimMemory() level=" + level);
        }
        super.onTrimMemory(level);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() mController=" + mController);
        if (mController != null) {
            mController.onDestroy();
        }
        if (mChangeObserver != null) {
            mChangeObserver.unregister();
        }
        Settings.System.putIntForUser(getContentResolver(),
                OneHandConstants.ONEHAND_RUNNING, 0,
                UserHandle.USER_CURRENT);

        Process.killProcess(Process.myPid());
    }

    private void initialize() {
        if (DBG) {
            Log.d(TAG, "initialize() mWinInfo=" + mWinInfo + ", mController =" + mController);
        }
        if (mWinInfo != null) {
            Log.d(TAG, "skip initialze() mWinInfo != null");
            return;
        }

        mWinInfo = OneHandWindowInfo.getInstance();
        mWinInfo.init(mContext);

        mUtils = OneHandUtils.getInstance();
        mUtils.init(mContext);

        mController = new OneHandController(mContext);
        mChangeObserver = new OneHandChangeObserver(mContext, mController);
    }

    public void manageProcessForeground(boolean isForeground) {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am != null && mBinder != null) {
            try {
                am.setProcessImportant(mBinder, Process.myPid(), isForeground, TAG);
            } catch (RemoteException e) {
                Log.w(TAG, "manageProcessForeground", e);
            }
        }
    }
}
