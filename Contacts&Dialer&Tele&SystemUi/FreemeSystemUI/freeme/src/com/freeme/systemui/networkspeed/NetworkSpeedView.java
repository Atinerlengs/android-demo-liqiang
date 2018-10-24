package com.freeme.systemui.networkspeed;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.freeme.systemui.tint.TintTextView;
import com.android.systemui.R;

public class NetworkSpeedView extends LinearLayout implements NetworkSpeedManagerEx.Callback {

    private static final String TAG = NetworkSpeedView.class.getSimpleName();

    private static final boolean DEBUG = false;

    private boolean mShowInKeyguard = true;

    private TintTextView mNetSpeedText;

    private NetworkSpeedManagerEx mNetworkSpeedManagerEx = new NetworkSpeedManagerEx();

    public NetworkSpeedView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        mNetSpeedText = (TintTextView) findViewById(R.id.speed);
        super.onFinishInflate();
    }

    @Override
    protected void onAttachedToWindow() {
        if (mNetworkSpeedManagerEx != null) {
            mNetworkSpeedManagerEx.init(getContext(), this);
        }
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mNetworkSpeedManagerEx != null) {
            mNetworkSpeedManagerEx.unRegister();
            mNetworkSpeedManagerEx = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void updateSpeed(String speed) {
        if (mShowInKeyguard && speed != null && mNetSpeedText != null) {
            mNetSpeedText.setText(speed);
        }

        if (DEBUG) {
            Log.i("NetworkSpeedView", "/update(), speed=" + speed
                    + " parent class:" + getParent().getClass());
        }
    }

    @Override
    public void updateVisibility(boolean show) {
        setVisibility(mShowInKeyguard && show ? VISIBLE : GONE);
    }

    public void setShowInKeyguard(boolean show) {
        mShowInKeyguard = show;

        if (mNetworkSpeedManagerEx != null) {
            mNetworkSpeedManagerEx.unRegister();
            mNetworkSpeedManagerEx = null;
        }
    }
}
