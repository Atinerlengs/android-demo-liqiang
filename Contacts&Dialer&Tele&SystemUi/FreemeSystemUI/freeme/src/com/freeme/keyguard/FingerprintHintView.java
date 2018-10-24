package com.freeme.keyguard;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;

public class FingerprintHintView extends RelativeLayout {
    private TextView mHintText;
    private volatile long timeoutMs = 31000;
    private static final int REFRESH = 1;
    private static final int FINISH = 2;
    private Context mContext;
    private CountDownTimer mCountDownTimer;

    public FingerprintHintView(Context context) {
        this(context, null);
    }

    public FingerprintHintView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FingerprintHintView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHintText = (TextView) findViewById(R.id.hint);
    }

    public static FingerprintHintView findFingerprintHintView(View view) {
        FingerprintHintView fingerprintHintView = (FingerprintHintView) view.findViewById(R.id.fp_hint_view);
        return fingerprintHintView;
    }

    public void showFpHint() {
        if (mCountDownTimer != null) {
            return;
        }
        handleAttemptLockout(SystemClock.elapsedRealtime() + timeoutMs);
    }

    public void hideFpHint() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        mHintText.setVisibility(View.GONE);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH:
                    int secondsRemaining = msg.arg1;
                    String hint = mContext.getString(R.string.fingerprint_lock_out_count_down);
                    mHintText.setVisibility(View.VISIBLE);
                    mHintText.setText(String.format(hint, secondsRemaining));
                    break;
                case FINISH:
                    if (mCountDownTimer != null) {
                        mCountDownTimer.cancel();
                        mCountDownTimer = null;
                    }
                    mHintText.setVisibility(View.VISIBLE);
                    mHintText.setText(mContext.getString(R.string.fingerprint_lock_out_count_down_finish));
                    break;
            }
        }
    };

    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        final long elapsedRealtime = SystemClock.elapsedRealtime();

        mCountDownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsRemaining = (int) (millisUntilFinished / 1000);
                mHandler.sendMessage(mHandler.obtainMessage(REFRESH, secondsRemaining, 0));
            }

            @Override
            public void onFinish() {
                mHandler.sendEmptyMessage(FINISH);
            }

        }.start();
    }
}
