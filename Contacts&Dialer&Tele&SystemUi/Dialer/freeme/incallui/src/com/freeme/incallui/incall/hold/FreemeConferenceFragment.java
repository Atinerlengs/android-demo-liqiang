package com.freeme.incallui.incall.hold;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;

import com.android.dialer.common.FragmentUtils;
import com.android.incallui.hold.R;
import com.android.incallui.incall.impl.InCallFragment;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.InCallScreenDelegateFactory;

public class FreemeConferenceFragment extends Fragment {

    private boolean isTimerStarted;
    private static InCallScreenDelegate mInCallScreenDelegate;

    public static FreemeConferenceFragment newInstance() {
        return new FreemeConferenceFragment();
    }

    private Chronometer mCallConferenceTimer;
    private ImageView mConferenceDetail;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.freeme_incall_conference_banner,
                container, false);
        mCallConferenceTimer = view.findViewById(R.id.conference_contactgrid_bottom_timer);
        mConferenceDetail = view.findViewById(R.id.conference_detail);

        Fragment parent = getParentFragment();
        if (parent instanceof InCallFragment) {
            mInCallScreenDelegate = FragmentUtils.getParentUnsafe(parent,
                    InCallScreenDelegateFactory.class).newInCallScreenDelegate();
            mInCallScreenDelegate.onInCallScreenDelegateInit((InCallFragment) parent);
        }
        mConferenceDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInCallScreenDelegate();
            }
        });
        isTimerStarted = false;
        return view;
    }

    public void updateChronometer(long timer) {
        if (mCallConferenceTimer == null) {
            return;
        }
        if (timer > 0) {
            mCallConferenceTimer.setVisibility(View.VISIBLE);
            mCallConferenceTimer.setBase(timer - System.currentTimeMillis()
                    + SystemClock.elapsedRealtime());
            if (!isTimerStarted) {
                mCallConferenceTimer.start();
            }
        } else {
            mCallConferenceTimer.setVisibility(View.GONE);
            mCallConferenceTimer.stop();
            isTimerStarted = false;
        }
    }

    private void createInCallScreenDelegate() {
        if (mInCallScreenDelegate != null) {
            mInCallScreenDelegate.onManageConferenceClicked();
        }
    }
}