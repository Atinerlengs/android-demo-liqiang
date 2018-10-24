package com.freeme.incallui.video.impl;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.telecom.CallAudioState;
import android.view.View;

import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.incallui.incall.impl.CheckableLabeledButton;
import com.android.incallui.incall.impl.CheckableLabeledButton.OnCheckedChangeListener;
import com.android.incallui.incall.protocol.InCallButtonUiDelegate;
import com.android.incallui.speakerbuttonlogic.SpeakerButtonInfo;
import com.android.incallui.video.impl.R;
import com.android.incallui.video.protocol.VideoCallScreenDelegate;

public class FreemeSpeakerButtonController
        implements OnCheckedChangeListener, View.OnClickListener {

    @NonNull
    private final InCallButtonUiDelegate inCallButtonUiDelegate;
    @NonNull
    private final VideoCallScreenDelegate videoCallScreenDelegate;

    @NonNull
    private CheckableLabeledButton button;

    @DrawableRes
    private int icon = R.drawable.quantum_ic_volume_up_white_36;

    private boolean isChecked;
    private boolean checkable;
    private boolean isEnabled;
    private CharSequence contentDescription;

    public FreemeSpeakerButtonController(
            @NonNull CheckableLabeledButton button,
            @NonNull InCallButtonUiDelegate inCallButtonUiDelegate,
            @NonNull VideoCallScreenDelegate videoCallScreenDelegate) {
        this.inCallButtonUiDelegate = Assert.isNotNull(inCallButtonUiDelegate);
        this.videoCallScreenDelegate = Assert.isNotNull(videoCallScreenDelegate);
        this.button = Assert.isNotNull(button);
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void updateButtonState() {
        button.setIsHideLabel(true);
        button.setVisibility(View.VISIBLE);
        button.setEnabled(isEnabled);
        button.setChecked(isChecked);
        button.setOnClickListener(checkable ? null : this);
        button.setOnCheckedChangeListener(checkable ? this : null);
        button.setIconDrawable(icon);
        button.setContentDescription(contentDescription);
    }

    public void setAudioState(CallAudioState audioState) {
        LogUtil.i("FreemeSpeakerButtonController.setSupportedAudio", "audioState: "
                + audioState);
        SpeakerButtonInfo info = new SpeakerButtonInfo(audioState,
                SpeakerButtonInfo.IconSize.SIZE_36_DP);

        checkable = info.checkable;
        isChecked = info.isChecked;
        icon = info.icon;
        @StringRes int contentDescriptionResId = info.contentDescription;
        contentDescription = button.getContext().getText(contentDescriptionResId);
        updateButtonState();
    }

    @Override
    public void onCheckedChanged(CheckableLabeledButton button, boolean isChecked) {
        LogUtil.i("FreemeSpeakerButtonController.onCheckedChanged", null);
        inCallButtonUiDelegate.toggleSpeakerphone();
        videoCallScreenDelegate.resetAutoFullscreenTimer();
    }

    @Override
    public void onClick(View view) {
        LogUtil.i("FreemeSpeakerButtonController.onClick", null);
        inCallButtonUiDelegate.showAudioRouteSelector();
        videoCallScreenDelegate.resetAutoFullscreenTimer();
    }
}
