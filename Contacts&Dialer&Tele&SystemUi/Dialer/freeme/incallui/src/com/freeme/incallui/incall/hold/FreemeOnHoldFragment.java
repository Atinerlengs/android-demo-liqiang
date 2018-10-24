package com.freeme.incallui.incall.hold;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.incallui.call.DialerCall;
import com.android.incallui.hold.R;
import com.android.incallui.incall.impl.InCallFragment;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.InCallScreenDelegateFactory;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.incallui.incall.protocol.FreemeOnHoldInfo;
import com.mediatek.incallui.plugin.ExtensionManager;

import java.util.LinkedHashMap;
import java.util.Locale;

public class FreemeOnHoldFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "FreemeOnHoldFragment";
    private final static String KEY_ARGS = "args";

    private boolean padTopInset = true;
    private int topInset;

    private static InCallScreenDelegate mInCallScreenDelegate;

    private View[] containerArr = new View[2];
    private TextView[] callerNameArr = new TextView[2];
    private TextView[] callStatusArr = new TextView[2];
    private Chronometer[] callTimerArr = new Chronometer[2];
    private ImageView[] confManagerArr = new ImageView[2];

    private LinkedHashMap<String, FreemeOnHoldInfo> mCallInfo;

    public static FreemeOnHoldFragment newInstance(LinkedHashMap<String, FreemeOnHoldInfo> infoData) {
        FreemeLogUtils.i(TAG, "newInstance......");
        FreemeOnHoldFragment fragment = new FreemeOnHoldFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_ARGS, infoData);
        fragment.setArguments(args);
        return new FreemeOnHoldFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup,
                             @Nullable Bundle bundle) {
        final View view = layoutInflater.inflate(R.layout.freeme_incall_multi_call_fragment,
                viewGroup, false);

        containerArr[0] = view.findViewById(R.id.freeme_first_call_info);
        callerNameArr[0] = containerArr[0].findViewById(R.id.freeme_contact_name);
        callStatusArr[0] = containerArr[0].findViewById(R.id.freeme_call_status);
        callTimerArr[0] = containerArr[0].findViewById(R.id.freeme_timer);
        confManagerArr[0] = containerArr[0].findViewById(R.id.conference_detail);

        containerArr[1] = view.findViewById(R.id.freeme_second_call_info);
        callerNameArr[1] = containerArr[1].findViewById(R.id.freeme_contact_name);
        callStatusArr[1] = containerArr[1].findViewById(R.id.freeme_call_status);
        callTimerArr[1] = containerArr[1].findViewById(R.id.freeme_timer);
        confManagerArr[1] = containerArr[1].findViewById(R.id.conference_detail);

        if (bundle != null) {
            LinkedHashMap<String, FreemeOnHoldInfo> data =
                    (LinkedHashMap<String, FreemeOnHoldInfo>) bundle.getSerializable(KEY_ARGS);
            updateCallerInfo(data);
        }

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                topInset = v.getRootWindowInsets().getSystemWindowInsetTop();
                applyInset();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
            }
        });

        Fragment parent = getParentFragment();
        if (parent instanceof InCallFragment) {
            mInCallScreenDelegate = FragmentUtils.getParentUnsafe(parent,
                    InCallScreenDelegateFactory.class).newInCallScreenDelegate();
            mInCallScreenDelegate.onInCallScreenDelegateInit((InCallFragment) parent);
        }
        confManagerArr[0].setOnClickListener(this);
        confManagerArr[1].setOnClickListener(this);

        /// M: add for OP09 plugin. @{
        ExtensionManager.getCallCardExt().onHoldViewCreated(view);
        /// @}
        return view;
    }

    public void updateCallerInfo(LinkedHashMap<String, FreemeOnHoldInfo> infoData) {
        mCallInfo = Assert.isNotNull(infoData);
        if (callerNameArr[0] == null || mCallInfo == null) {
            return;
        }
        int idx = 0;
        for (FreemeOnHoldInfo data : mCallInfo.values()) {
            showCallInfo(idx, data);
            idx++;
        }
    }

    private void showCallInfo(int idx, FreemeOnHoldInfo info) {
        if (idx < 0 || idx >= callerNameArr.length || info == null) {
            FreemeLogUtils.i(TAG, "idx is out of bounds or FreemeOnHoldInfo is null");
            return;
        }

        if (info.isConference) {
            info.number = getString(R.string.conference_call_name);
            confManagerArr[idx].setVisibility(View.VISIBLE);
        } else {
            confManagerArr[idx].setVisibility(View.GONE);
        }
        callerNameArr[idx].setText(TextUtils.isEmpty(info.lookupKey)
                ? info.number : info.name);

        final int color = getContext().getColor(R.color.freeme_incall_multi_call_banner_bg);
        int ctaRes = getCtaSpecificOnHoldResId(callStatusArr[idx].getResources(), info.state);

        if (FreemeLogUtils.DEBUG) {
            FreemeLogUtils.i(TAG, "idx: " + idx
                    + ", callId: " + info.id
                    + ", lookupKey: " + info.lookupKey
                    + ", number: " + info.number
                    + ", name: " + info.name
                    + ", state: " + info.state
                    + ", connectTimeMillis: " + info.connectTimeMillis
                    + ", ctaRes: " + ((ctaRes > 0) ? callStatusArr[idx].getResources().getString(ctaRes) : "-1"));
        }

        if (ctaRes > 0) {
            callStatusArr[idx].setVisibility(View.VISIBLE);
            callStatusArr[idx].setText(ctaRes);
            callTimerArr[idx].setVisibility(View.GONE);
            if (info.state == DialerCall.State.CONNECTING
                    || info.state == DialerCall.State.DIALING) {
                containerArr[idx].setBackgroundColor(color);
                callerNameArr[idx].setTextAppearance(R.style.Dialer_Incall_TextAppearance_MultiCall_Name);
                callStatusArr[idx].setTextAppearance(R.style.Dialer_Incall_TextAppearance_MultiCall_Status);
            } else {
                confManagerArr[idx].setVisibility(View.GONE);
                containerArr[idx].setBackgroundColor(Color.TRANSPARENT);
                callerNameArr[idx].setTextAppearance(R.style.Dialer_Incall_TextAppearance_MultiCall_Name_Hold);
                callStatusArr[idx].setTextAppearance(R.style.Dialer_Incall_TextAppearance_MultiCall_Status_Hold);
            }
        } else if (info.connectTimeMillis > 0) {
            callStatusArr[idx].setVisibility(View.GONE);
            callTimerArr[idx].setVisibility(View.VISIBLE);
            callTimerArr[idx].setBase(info.connectTimeMillis
                    - System.currentTimeMillis()
                    + SystemClock.elapsedRealtime());
            callTimerArr[idx].start();
            containerArr[idx].setBackgroundColor(color);
            callerNameArr[idx].setTextAppearance(R.style.Dialer_Incall_TextAppearance_MultiCall_Name);
            callStatusArr[idx].setTextAppearance(R.style.Dialer_Incall_TextAppearance_MultiCall_Status);
        }
    }

    public void setPadTopInset(boolean padTopInset) {
        this.padTopInset = padTopInset;
        applyInset();
    }

    private void applyInset() {
        if (getView() == null) {
            return;
        }

        int newPadding = padTopInset ? topInset : 0;
        if (newPadding != getView().getPaddingTop()) {
            TransitionManager.beginDelayedTransition(((ViewGroup) getView().getParent()));
            getView().setPadding(0, newPadding, 0, 0);
        }
    }

    /**
     * M: [CTA]CTA required that in Simplified Chinese, the text label of the secondary/tertiary
     * call should be changed to another string rather than google default.
     *
     * @return the right resId CTS required.
     */
    private int getCtaSpecificOnHoldResId(Resources res, int status) {
        Locale currentLocale = res.getConfiguration().locale;
        if (status == DialerCall.State.CONNECTING || status == DialerCall.State.DIALING) {
            return R.string.incall_connecting;
        } else if (status == DialerCall.State.ONHOLD) {
            if (Locale.SIMPLIFIED_CHINESE.getCountry().equals(currentLocale.getCountry())
                    && Locale.SIMPLIFIED_CHINESE.getLanguage().equals(currentLocale.getLanguage())) {
                return R.string.onHold_cta;
            }
        }
        return -1;
    }

    @Override
    public void onClick(View v) {
        if (mInCallScreenDelegate != null) {
            mInCallScreenDelegate.onManageConferenceClicked();
        }
    }
}
