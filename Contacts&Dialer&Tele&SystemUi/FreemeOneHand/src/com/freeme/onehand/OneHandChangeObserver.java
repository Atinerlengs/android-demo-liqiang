package com.freeme.onehand;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

final class OneHandChangeObserver {
    private static final String TAG = "OneHandChangeObserver";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private final Context mContext;
    private Handler mHandler;

    private OneHandController mController;
    private OneHandWindowInfo mWinInfo;

    private final BroadcastReceiver mScreenOnOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DBG) {
                Log.d(TAG, "onReceive: " + action);
            }

            if (mSensorManager != null && mProximitySensor != null) {
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    mSensorManager.registerListener(mSensorEventListener, mProximitySensor,
                            SensorManager.SENSOR_DELAY_NORMAL);
                    mController.screenTurnedOn();
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    mWinInfo.setGestureByProximitySensor(false);
                    mSensorManager.unregisterListener(mSensorEventListener);
                }
            }
        }
    };

    private final Runnable mWallpaperChangeRunnable = new Runnable() {
        @Override
        public void run() {
            mController.updateBackgroundImage();
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public final void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DBG) {
                Log.d(TAG, "onReceive: " + action);
            }

            if (Intent.ACTION_DREAMING_STARTED.equals(action)) {
                closeAllWindowSafe(false);
            } else if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                mController.forceStopService();
            } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
                closeAllWindowSafe(false);
            } else if (Intent.ACTION_WALLPAPER_CHANGED.equals(action)) {
                if (mController != null) {
                    mHandler.removeCallbacks(mWallpaperChangeRunnable);
                    mHandler.postDelayed(mWallpaperChangeRunnable, 3000);
                }
            } else if ("com.freeme.intent.action.EMERGENCY_STATE_CHANGED".equals(action)) {
                int reason = intent.getIntExtra("reason", 0);
                Log.d(TAG, "mEmergencyModeReceiver. reason=" + reason);
                if (reason == 2) {
                    closeAllWindowSafe(true);
                }
            }
        }
    };

    private final SensorManager mSensorManager;
    private final Sensor mProximitySensor;
    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            int value = (int) event.values[0];
            if (DBG) {
                Log.d(TAG, "onSensorChanged() value=" + value);
            }
            if (value > 0) {
                mWinInfo.setGestureByProximitySensor(true);
                mSensorManager.unregisterListener(mSensorEventListener);
            }
        }
    };

    private final class OneHandContentObserver extends ContentObserver {
        private final Uri URI_SHOW_HARD_KEYS;
        private final Uri URI_WAKEUP_TYPE;

        private final ContentResolver mResolver;

        public OneHandContentObserver(Context context, Handler handler) {
            super(handler);
            mResolver = context.getContentResolver();

            URI_SHOW_HARD_KEYS = Settings.System.getUriFor(OneHandConstants.ONEHAND_SHOW_HARD_KEYS);
            URI_WAKEUP_TYPE = Settings.System.getUriFor(OneHandConstants.ONEHAND_WAKEUP_TYPE);
        }

        void register() {
            mResolver.registerContentObserver(URI_SHOW_HARD_KEYS, false,
                    this, UserHandle.USER_CURRENT);
            mResolver.registerContentObserver(URI_WAKEUP_TYPE, false,
                    this, UserHandle.USER_CURRENT);
        }

        void unregister() {
            mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (DBG) {
                Log.d(TAG, "onChange() selfChange=" + selfChange + ", uri=" + uri);
            }

            if (URI_SHOW_HARD_KEYS.equals(uri)) {
                mController.onSoftkeyModeChanged(Settings.System.getIntForUser(mResolver,
                        OneHandConstants.ONEHAND_SHOW_HARD_KEYS, 0,
                        UserHandle.USER_CURRENT));
            } else if (URI_WAKEUP_TYPE.equals(uri)) {
                mController.onTriggerTypeChanged(Settings.System.getIntForUser(mResolver,
                        OneHandConstants.ONEHAND_WAKEUP_TYPE, 0,
                        UserHandle.USER_CURRENT));
            }
        }
    }
    private final OneHandContentObserver mContentObserver;

    public OneHandChangeObserver(Context context, OneHandController controller) {
        mContext = context;
        mHandler = new Handler();

        mController = controller;
        mWinInfo = OneHandWindowInfo.getInstance();

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        mContentObserver = new OneHandContentObserver(context, mHandler);
        mContentObserver.onChange(true, mContentObserver.URI_SHOW_HARD_KEYS);
        mContentObserver.onChange(true, mContentObserver.URI_WAKEUP_TYPE);

        register();
    }

    private void register() {
        mContentObserver.register();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenOnOffReceiver, intentFilter);

        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_DREAMING_STARTED);
        intentFilter.addAction(Intent.ACTION_USER_SWITCHED);
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        intentFilter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
        intentFilter.addAction("com.freeme.intent.action.EMERGENCY_STATE_CHANGED");
        mContext.registerReceiver(mIntentReceiver, intentFilter);
    }

    public void unregister() {
        mContentObserver.unregister();
        mContext.unregisterReceiver(mIntentReceiver);
        mContext.unregisterReceiver(mScreenOnOffReceiver);
    }

    private void closeAllWindowSafe(boolean animation) {
        if (mController != null) {
            mController.returnFullScreen(!mController.isAnimationRunning() && animation,
                    null);
        }
    }
}
