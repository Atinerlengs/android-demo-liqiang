package com.freeme.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.service.quicksettings.Tile;

import com.android.internal.logging.nano.MetricsProto;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class FreemeScreenRecorder extends QSTileImpl<QSTile.BooleanState> {
    private static final String TAG = "FreemeScreenRecorder";

    private static final String ACTION_SCREEN_RECORDER_STATE = "com.freeme.systemui.action.SCREEN_RECORDER_STATE";
    private static boolean mSwitch = false;

    private Handler mHandler = new Handler();

    public FreemeScreenRecorder(QSHost host) {
        super(host);

        IntentFilter filter =
                new IntentFilter(ACTION_SCREEN_RECORDER_STATE);
        mContext.registerReceiver(new ScreenRecorderReceiver(), filter);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {}

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.freeme_screen_recorder);
    }

    @Override
    protected void handleClick() {
        mHost.collapsePanels();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startScreenRecorder();
            }
        }, 250);
    }

    private void startScreenRecorder() {
        Intent intent = new Intent("android.intent.action.ScreenRecorder");
        intent.setPackage("com.freeme.screenrecorder");
        mContext.startService(intent);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.freeme_screen_recorder);
        state.value = mSwitch;
        state.state = mSwitch ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.icon = ResourceIcon.get(R.drawable.freeme_ic_qs_screenrecorder);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.QS_PANEL;
    }

    public class ScreenRecorderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_SCREEN_RECORDER_STATE.equals(intent.getAction())) {
                mSwitch = intent.getBooleanExtra("isStart", false);
                refreshState();
            }
        }
    }

}