package com.freeme.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;

import com.android.internal.logging.nano.MetricsProto;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class FreemeAudioProfile extends QSTileImpl<QSTile.BooleanState> {

    private static final String TAG = "FreemeAudioProfile";

    private static final int CHANGE_PROFILE = 9000;

    private final Intent kIntent = new Intent().setClassName(
            "com.android.settings","com.android.settings.Settings$SoundSettingsActivity");

    private int mAudioState = R.drawable.freeme_ic_qs_normal;
    private int mAudioString = R.string.freeme_normal;

    private AudioManager mAudioManager;

    private final BroadcastReceiver mRingerBroadcast = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            loadAudioprofile();
        }
    };

    public FreemeAudioProfile(QSHost host) {
        super(host);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        loadAudioprofile();
    }

    @Override
    protected void handleClick() {
        Message msg = mHandler.obtainMessage(CHANGE_PROFILE);
        mHandler.sendMessage(msg);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.QS_PANEL;
    }

    @Override
    public Intent getLongClickIntent() {
        return kIntent;
    }

    @Override
    protected void handleSetListening(boolean listening) {
        if (listening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
            mContext.registerReceiver(mRingerBroadcast, filter);
        } else {
            mContext.unregisterReceiver(mRingerBroadcast);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(mAudioString);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = true;
        state.label = mContext.getString(mAudioString);
        state.icon = ResourceIcon.get(mAudioState);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    private void loadAudioprofile() {
        final int mode = mAudioManager.getRingerModeInternal();
        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                mAudioState = R.drawable.freeme_ic_qs_normal;
                mAudioString = R.string.freeme_normal;
                break;
            case AudioManager.RINGER_MODE_SILENT:
                mAudioState = R.drawable.freeme_ic_qs_silent;
                mAudioString = R.string.freeme_silent;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                mAudioState = R.drawable.freeme_ic_qs_vibrate;
                mAudioString = R.string.freeme_vibrate;
                break;
            default:
                break;
        }
        refreshState();
    }

    private void getBNextProfile(int mode) {
        switch (mode) {
            case AudioManager.RINGER_MODE_NORMAL:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
                break;

            case AudioManager.RINGER_MODE_SILENT:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                break;

            case AudioManager.RINGER_MODE_VIBRATE:
                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
                break;

            default :
                break;
        }
        loadAudioprofile();
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHANGE_PROFILE:
                    getBNextProfile(mAudioManager.getRingerModeInternal());
                    break;
                default:
                    break;
            }
        }
    };

}
