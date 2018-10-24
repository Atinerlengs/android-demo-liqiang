package com.freeme.systemui.power;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Slog;

public final class FreemePowerUiReceiver extends BroadcastReceiver {
    private static final String TAG = "FreemePowerUiReceiver";

    private static final int SOUND_POWER_CONNECT = 0;
    private static final int SOUND_POWER_FULL    = 1;

    private final Handler mHandler = new Handler();

    private int mBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;

    private Context mContext;

    private SoundPool mSounds;

    boolean mIgnoreFirstPowerEvent = false;

    private int mPwrFullSoundId;
    private int mUsbconSoundId;

    public FreemePowerUiReceiver(Context context) {
        mContext = context;

        final ContentResolver resolver = mContext.getContentResolver();

        mSounds = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        String powerFullSoundPath = Settings.Global.getString(resolver,
                com.freeme.provider.FreemeSettings.Global.FREEME_SOUND_POWER_FULL);
        if ( powerFullSoundPath != null ) {
            mPwrFullSoundId = mSounds.load(powerFullSoundPath, 1);
        }
        String powerConnSoundPath = Settings.Global.getString(resolver,
                com.freeme.provider.FreemeSettings.Global.FREEME_SOUND_POWER_CONNECT);
        if ( powerConnSoundPath != null ) {
            mUsbconSoundId = mSounds.load(powerConnSoundPath, 1);
        }
    }

    public void init() {
        // Register for Intent broadcasts for...
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        //filter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        mContext.registerReceiver(this, filter, null, mHandler);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int oldBatteryStatus = mBatteryStatus;
            mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            final boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1) != 0;

            if (mBatteryStatus == BatteryManager.BATTERY_STATUS_FULL &&
                    mBatteryStatus != oldBatteryStatus) {
                playPowerNotificationSound(SOUND_POWER_FULL);
            }
            // change mIgnoreFirstPowerEvent
            if (mIgnoreFirstPowerEvent && plugged) {
                mIgnoreFirstPowerEvent = false;
            }
        } else if (Intent.ACTION_POWER_CONNECTED.equals(action)
                || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            if (mIgnoreFirstPowerEvent) {
                mIgnoreFirstPowerEvent = false;
            } else {
                playPowerNotificationSound(SOUND_POWER_CONNECT);
            }
        } else {
            Slog.w(TAG, "unknown intent: " + intent);
        }
    }

    private void playPowerNotificationSound(int selection) {
        final boolean enabled = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CHARGING_SOUNDS_ENABLED, 1) != 0;
        if (enabled) {
            switch (selection) {
                case SOUND_POWER_CONNECT:
                    // POWER_NOTIFICATIONS_RINGTONE
                    if (mUsbconSoundId != 0) {
                        mSounds.play(mUsbconSoundId,
                                1.0f, 1.0f, 1/*priortiy*/, 0/*loop*/, 1.0f/*rate*/);
                    }
                    break;
                case SOUND_POWER_FULL:
                    if (mPwrFullSoundId != 0) {
                        mSounds.play(mPwrFullSoundId,
                                1.0f, 1.0f, 1/*priortiy*/, 0/*loop*/, 1.0f/*rate*/);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
