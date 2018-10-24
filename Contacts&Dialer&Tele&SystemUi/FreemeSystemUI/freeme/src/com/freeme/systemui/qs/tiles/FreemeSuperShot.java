package com.freeme.systemui.qs.tiles;

import android.content.Intent;
import android.os.Handler;
import android.service.quicksettings.Tile;

import com.android.internal.logging.nano.MetricsProto;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class FreemeSuperShot extends QSTileImpl<QSTile.BooleanState> {
    private static final String TAG = "FreemeSuperShot";

    private Handler mHandler = new Handler();

    public FreemeSuperShot(QSHost host) {
        super(host);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected void handleLongClick() {
        return ;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleSetListening(boolean listening) {}

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.freeme_super_shot);
    }

    @Override
    protected void handleClick() {
        mHost.collapsePanels();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startScreenRecorder();
            }
        }, 500);
    }

    private void startScreenRecorder() {
        Intent service = new Intent("com.freeme.supershot.MainMenu");
        service.setPackage("com.freeme.supershot");
        mContext.startService(service);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.value = false;
        state.state = Tile.STATE_INACTIVE;
        state.icon = ResourceIcon.get(R.drawable.freeme_ic_qs_supershot);
        state.label = mContext.getString(R.string.freeme_super_shot);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.QS_PANEL;
    }

}