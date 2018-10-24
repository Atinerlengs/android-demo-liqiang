package com.freeme.incallui.video.impl;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.telecom.CallAudioState;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.dialer.common.LogUtil;
import com.android.dialer.compat.ActivityCompat;
import com.android.dialer.util.PermissionsUtil;
import com.android.incallui.audioroute.AudioRouteSelectorDialogFragment;
import com.android.incallui.incall.impl.CheckableLabeledButton;
import com.android.incallui.incall.protocol.InCallButtonIds;
import com.android.incallui.incall.protocol.InCallButtonIdsExtension;
import com.android.incallui.incall.protocol.InCallButtonUi;
import com.android.incallui.incall.protocol.InCallButtonUiDelegate;
import com.android.incallui.incall.protocol.InCallButtonUiDelegateFactory;
import com.android.incallui.incall.protocol.InCallScreen;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.InCallScreenDelegateFactory;
import com.android.incallui.incall.protocol.PrimaryCallState;
import com.android.incallui.incall.protocol.PrimaryInfo;
import com.android.incallui.incall.protocol.SecondaryInfo;
import com.android.incallui.video.impl.R;
import com.android.incallui.video.impl.SwitchOnHoldCallController;
import com.android.incallui.video.protocol.VideoCallScreen;
import com.android.incallui.video.protocol.VideoCallScreenDelegate;
import com.android.incallui.video.protocol.VideoCallScreenDelegateFactory;
import com.android.incallui.videosurface.bindings.VideoSurfaceBindings;
import com.android.incallui.videosurface.protocol.VideoSurfaceTexture;
import com.android.incallui.videotech.utils.VideoUtils;
import com.freeme.incallui.contactgrid.FreemeContactGridManager;
import com.freeme.incallui.incall.hold.FreemeOnHoldFragment;
import com.freeme.incallui.incall.protocol.FreemeOnHoldInfo;
import com.freeme.incallui.video.FreemeVideoDebugInfo;
import com.mediatek.incallui.plugin.ExtensionManager;

import java.util.LinkedHashMap;

public class FreemeVideoCallFragment extends Fragment
        implements InCallScreen,
        InCallButtonUi,
        VideoCallScreen,
        OnClickListener,
        CheckableLabeledButton.OnCheckedChangeListener,
        AudioRouteSelectorDialogFragment.AudioRouteSelectorPresenter,
        View.OnSystemUiVisibilityChangeListener {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String ARG_CALL_ID = "call_id";

    @VisibleForTesting
    static final float BLUR_PREVIEW_RADIUS = 0f;
    @VisibleForTesting
    static final float BLUR_PREVIEW_SCALE_FACTOR = 1.0f;
    private static final float BLUR_REMOTE_RADIUS = 0f;
    private static final float BLUR_REMOTE_SCALE_FACTOR = 0.25f;
    private static final float ASPECT_RATIO_MATCH_THRESHOLD = 0.2f;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1;
    private static final long CAMERA_PERMISSION_DIALOG_DELAY_IN_MILLIS = 2000L;
    private static final long VIDEO_OFF_VIEW_FADE_OUT_DELAY_IN_MILLIS = 2000L;

    private final ViewOutlineProvider circleOutlineProvider =
            new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int x = view.getWidth() / 2;
                    int y = view.getHeight() / 2;
                    int radius = Math.min(x, y);
                    outline.setOval(x - radius, y - radius, x + radius, y + radius);
                }
            };

    private InCallScreenDelegate inCallScreenDelegate;
    private VideoCallScreenDelegate videoCallScreenDelegate;
    private InCallButtonUiDelegate inCallButtonUiDelegate;
    private View endCallButton;
    private CheckableLabeledButton speakerButton;
    private FreemeSpeakerButtonController speakerButtonController;
    private CheckableLabeledButton muteButton;
    private CheckableLabeledButton cameraOffButton;
    private CheckableLabeledButton swapCameraButton;
    private View switchOnHoldButton;
    private View onHoldContainer;
    private SwitchOnHoldCallController switchOnHoldCallController;
    private TextView remoteVideoOff;
    private ImageView remoteOffBlurredImageView;
    private View previewOffOverlay;
    private View controls;
    private View controlsContainer;
    private TextureView previewTextureView;
    private TextureView remoteTextureView;
    private View greenScreenBackgroundView;
    private View fullscreenBackgroundView;
    private boolean shouldShowRemote;
    private boolean shouldShowPreview;
    private boolean isInFullscreenMode;
    private boolean isInGreenScreenMode;
    private boolean hasInitializedScreenModes;
    private boolean isRemotelyHeld;
    /// M: ALPS03506468 Record hide preview and hide previewoffView @{
    private boolean mHidePreview = false;
    /// @}
    private FreemeContactGridManager contactGridManager;
    private SecondaryInfo savedSecondaryInfo;
    private final Runnable cameraPermissionDialogRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (videoCallScreenDelegate.shouldShowCameraPermissionToast()) {
                        LogUtil.i("VideoCallFragment.cameraPermissionDialogRunnable", "showing dialog");
                        checkCameraPermission();
                    }
                }
            };

    public static FreemeVideoCallFragment newInstance(String callId) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_CALL_ID, Assert.isNotNull(callId));

        FreemeVideoCallFragment instance = new FreemeVideoCallFragment();
        instance.setArguments(bundle);
        return instance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i("VideoCallFragment.onCreate", null);

        inCallButtonUiDelegate =
                FragmentUtils.getParent(this, InCallButtonUiDelegateFactory.class)
                        .newInCallButtonUiDelegate();
        if (savedInstanceState != null) {
            inCallButtonUiDelegate.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogUtil.i("VideoCallFragment.onRequestPermissionsResult", "Camera permission granted.");
                videoCallScreenDelegate.onCameraPermissionGranted();
            } else {
                LogUtil.i("VideoCallFragment.onRequestPermissionsResult", "Camera permission denied.");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        LogUtil.i("VideoCallFragment.onCreateView", null);

        View view = layoutInflater.inflate(
                isLandscape() ? R.layout.freeme_frag_videocall_land
                        : R.layout.freeme_frag_videocall,
                viewGroup, false);
        contactGridManager = new FreemeContactGridManager(view, null,
                0, false);

        boolean isInMultiWindowMode = ActivityCompat.isInMultiWindowMode(getActivity());
        controls = view.findViewById(R.id.videocall_video_controls);
        controls.setVisibility(
                isInMultiWindowMode ? View.GONE : View.VISIBLE);
        controlsContainer = view.findViewById(R.id.videocall_video_controls_container);
        mFreemeBottomBtnContainer = view.findViewById(R.id.freeme_bottom_button_layout);
        speakerButton = view.findViewById(R.id.videocall_speaker_button);
        speakerButton.setIsHideLabel(true);
        muteButton = view.findViewById(R.id.videocall_mute_button);
        muteButton.setOnCheckedChangeListener(this);
        cameraOffButton = view.findViewById(R.id.videocall_mute_video);
        cameraOffButton.setOnCheckedChangeListener(this);
        previewOffOverlay = view.findViewById(R.id.videocall_video_preview_off_overlay);
        swapCameraButton = view.findViewById(R.id.switch_camera_button);
        swapCameraButton.setOnClickListener(this);
        switchOnHoldButton = view.findViewById(R.id.videocall_switch_on_hold);
        onHoldContainer = view.findViewById(R.id.videocall_on_hold_banner);
        remoteVideoOff = view.findViewById(R.id.videocall_remote_video_off);
        remoteVideoOff.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        remoteOffBlurredImageView =
                view.findViewById(R.id.videocall_remote_off_blurred_image_view);
        endCallButton = view.findViewById(R.id.videocall_end_call);
        endCallButton.setOnClickListener(this);
        previewTextureView = view.findViewById(R.id.videocall_video_preview);
        previewOffOverlay.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkCameraPermission();
                    }
                });
        remoteTextureView = view.findViewById(R.id.videocall_video_remote);
        greenScreenBackgroundView = view.findViewById(R.id.videocall_green_screen_background);
        fullscreenBackgroundView = view.findViewById(R.id.videocall_fullscreen_background);

        remoteTextureView.addOnLayoutChangeListener(
                new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom) {
                        LogUtil.i("VideoCallFragment.onLayoutChange", "remoteTextureView layout changed");
                        updateRemoteVideoScaling();
                        updateRemoteOffView();
                    }
                });

        previewTextureView.addOnLayoutChangeListener(
                new OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(
                            View v,
                            int left,
                            int top,
                            int right,
                            int bottom,
                            int oldLeft,
                            int oldTop,
                            int oldRight,
                            int oldBottom) {
                        LogUtil.i("VideoCallFragment.onLayoutChange", "previewTextureView layout changed");
                        updatePreviewVideoScaling();
                        updatePreviewOffView();

                        /// M: ALPS03519689, preview does not update in non-full screen after rotation @{
                        if (!hasInitializedScreenModes) {
                            updateFullscreenAndGreenScreenMode(isInFullscreenMode, isInGreenScreenMode);
                        }
                        /// @}

                        /// M:ALPS03502688 Scrollview's button will cover with local video, after you answer the
                        ///vilte call request from notification.the rootcause is nevigation of height can't get
                        ///before view start measue layout.so view can't set right padding to layout. So when
                        ///onGloalLayout callback,AP tringer view set padding by onApplyWindowInsets.@{
                        if (getView().isAttachedToWindow() &&
                                !isInMultiWindowMode) {
                            controlsContainer.onApplyWindowInsets(getView().getRootWindowInsets());
                        }
                        ///@}
                    }
                });

        /// M: init MediaTek feature's view.
        initView(view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle bundle) {
        super.onViewCreated(view, bundle);
        LogUtil.i("VideoCallFragment.onViewCreated", null);

        inCallScreenDelegate =
                FragmentUtils.getParentUnsafe(this, InCallScreenDelegateFactory.class)
                        .newInCallScreenDelegate();
        videoCallScreenDelegate =
                FragmentUtils.getParentUnsafe(this, VideoCallScreenDelegateFactory.class)
                        .newVideoCallScreenDelegate(this);

        speakerButtonController =
                new FreemeSpeakerButtonController(speakerButton, inCallButtonUiDelegate, videoCallScreenDelegate);
        switchOnHoldCallController =
                new SwitchOnHoldCallController(
                        switchOnHoldButton, onHoldContainer, inCallScreenDelegate, videoCallScreenDelegate);

        videoCallScreenDelegate.initVideoCallScreenDelegate(getContext(), this);

        inCallScreenDelegate.onInCallScreenDelegateInit(this);
        inCallScreenDelegate.onInCallScreenReady();
        inCallButtonUiDelegate.onInCallButtonUiReady(this);

        view.setOnSystemUiVisibilityChangeListener(this);
        /// M:[ALPS03556861] delete clear listener after videocallfragemnt onstop.Merge video conference
        ///success after video screen change to background, the incallui don't set surface to VTService
        ///in this case,the VTService will get stuck.@{
        videoCallScreenDelegate.onVideoCallScreenUiReady();
        ///@}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /// M: [ALPS03863711] ignore greenscreen or fullscreen update in background. @{
        mIsVisible = false;
        /// @}
        inCallButtonUiDelegate.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtil.i("VideoCallFragment.onDestroyView", null);
        /// M: save mute state in incallpresenter incase of losing state due to
        //  destroy of videocall fragment. @{
        inCallButtonUiDelegate.saveAutoMuteState();
        /// @}
        inCallButtonUiDelegate.onInCallButtonUiUnready();
        inCallScreenDelegate.onInCallScreenUnready();
        /// M:[ALPS03556861] delete clear listener after videocallfragemnt onstop.Merge video conference
        ///success after video screen change to background, the incallui don't set surface to VTService
        ///in this case,the VTService will get stuck.@{
        videoCallScreenDelegate.onVideoCallScreenUiUnready();
        ///@}
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (savedSecondaryInfo != null) {
            setSecondary(savedSecondaryInfo);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtil.i("VideoCallFragment.onStart", null);
        /// M: [ALPS03863711] ignore greenscreen or fullscreen update in background. @{
        mIsVisible = true;
        /// @}
        onVideoScreenStart();
    }

    @Override
    public void onVideoScreenStart() {
        /// M: [ALPS03855504] update greenscreen or fullscreen when screen resume @{
        videoCallScreenDelegate.updateFullscreenAndGreenScreenModeWhenUIStart();
        /// @}
        inCallButtonUiDelegate.refreshMuteState();
        /// M:[ALPS03556861] delete clear listener after videocallfragemnt onstop.Merge video conference
        ///success after video screen change to background, the incallui don't set surface to VTService
        ///in this case,the VTService will get stuck.@{
        /// google original code:
        //videoCallScreenDelegate.onVideoCallScreenUiReady();
        ///@}
        getView().postDelayed(cameraPermissionDialogRunnable, CAMERA_PERMISSION_DIALOG_DELAY_IN_MILLIS);
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.i("VideoCallFragment.onResume", null);
        inCallScreenDelegate.onInCallScreenResumed();
        /// M: refresh mute state and align with voice call.
        inCallButtonUiDelegate.refreshMuteState();

        /** M: add FreemeVideoDebugInfo feature. @{ */
        if (FreemeVideoDebugInfo.isFeatureOn() && mVideoDebugInfo == null) {
            mVideoDebugInfo = new FreemeVideoDebugInfo();
            LogUtil.i("VideoCallFragment.onResume",
                    "[checkPackageLoss]EngineerMode for package loss is on,set param");
            mVideoDebugInfo.setFragment(this);
            View parent = (View) previewTextureView.getParent();
            if (!(parent instanceof RelativeLayout)) {
                LogUtil.i("VideoCallFragment.onResume", "[checkPackageLoss]not a RelativeLayout:");
                return;
            }
            mVideoDebugInfo.setParent((RelativeLayout) parent);
        }
        /** @}*/
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtil.i("VideoCallFragment.onPause", null);
        inCallScreenDelegate.onInCallScreenPaused();
        /// M:[ALPS03556861] delete clear listener after videocallfragemnt onstop.Merge video conference
        ///success after video screen change to background, the incallui don't set surface to VTService
        ///in this case,the VTService will get stuck.@{
        videoCallScreenDelegate.cancelAutoFullScreen();
        ///@}
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.i("VideoCallFragment.onStop", null);
        /// M: [ALPS03863711] ignore greenscreen or fullscreen update in background. @{
        mIsVisible = false;
        /// @}
        onVideoScreenStop();
    }

    @Override
    public void onVideoScreenStop() {
        getView().removeCallbacks(cameraPermissionDialogRunnable);
        /// M:[ALPS03556861] delete clear listener after videocallfragemnt onstop.Merge video conference
        ///success after video screen change to background, the incallui don't set surface to VTService
        ///in this case,the VTService will get stuck.@{
        /// google original code:
        //videoCallScreenDelegate.onVideoCallScreenUiUnready();
        ///@}
    }

    private void exitFullscreenMode() {
        LogUtil.i("VideoCallFragment.exitFullscreenMode", null);

        if (!getView().isAttachedToWindow()) {
            /// M: ALPS03519689, preview does not update in non-full screen after rotation @{
            hasInitializedScreenModes = false;
            /// @}
            LogUtil.i("VideoCallFragment.exitFullscreenMode", "not attached");
            return;
        }

        showSystemUI();

        LinearOutSlowInInterpolator linearOutSlowInInterpolator = new LinearOutSlowInInterpolator();

        // Animate the controls to the shown state.
        if (!mDialpadButton.isChecked()) { // checked means dialpad is shown
            controls.animate()
                    .translationX(0)
                    .translationY(0)
                    .setInterpolator(linearOutSlowInInterpolator)
                    .alpha(1)
                    .start();
        }

        View contactGridView = contactGridManager.getContainerView();
        // Animate contact grid to the shown state.
        contactGridView
                .animate()
                .translationX(0)
                .translationY(0)
                .setInterpolator(linearOutSlowInInterpolator)
                .alpha(1)
                .withStartAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (mFreemeCallInfo != null) {
                                    showOrHideVideoCallInfo(mFreemeCallInfo.size() > 1);
                                }
                            }
                        });

        mFreemeBottomBtnContainer
                .animate()
                .translationX(0)
                .translationY(0)
                .setInterpolator(linearOutSlowInInterpolator)
                .alpha(1)
                .withStartAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                mFreemeBottomBtnContainer.setVisibility(View.VISIBLE);
                            }
                        })
                .start();

        updateOverlayBackground();
        /// M: show scroll view too.
        mVideoScrollView.setAlpha(1);
    }

    private void showSystemUI() {
        View view = getView();
        if (view != null) {
            // Code is more expressive with all flags present, even though some may be combined
            // noinspection PointlessBitwiseExpression
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    /**
     * Set view flags to hide the system UI. System UI will return on any touch event
     */
    private void hideSystemUI() {
        View view = getView();
        if (view != null) {
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private Point getControlsOffsetEndHidden(View controls) {
        return new Point(0, getOffsetBottom(controls));
    }

    private Point getContactGridOffsetEndHidden(View view) {
        return new Point(0, getOffsetTop(view));
    }

    private Point getEndCallOffsetEndHidden(View endCallButton) {
        return new Point(0, ((MarginLayoutParams) endCallButton.getLayoutParams()).bottomMargin);
    }

    private int getOffsetTop(View view) {
        return -(view.getHeight() + ((MarginLayoutParams) view.getLayoutParams()).topMargin);
    }

    private int getOffsetBottom(View view) {
        return view.getHeight() + ((MarginLayoutParams) view.getLayoutParams()).bottomMargin;
    }

    private void enterFullscreenMode() {
        LogUtil.i("VideoCallFragment.enterFullscreenMode", null);

        hideSystemUI();

        Interpolator fastOutLinearInInterpolator = new FastOutLinearInInterpolator();

        // Animate controls to the hidden state.
        Point offset = getControlsOffsetEndHidden(controls);
        controls
                .animate()
                .translationX(offset.x)
                .translationY(offset.y)
                .setInterpolator(fastOutLinearInInterpolator)
                .alpha(0)
                .start();

        View contactGridView = contactGridManager.getContainerView();
        // Animate contact grid to the hidden state.
        offset = getContactGridOffsetEndHidden(contactGridView);
        contactGridView
                .animate()
                .translationX(offset.x)
                .translationY(offset.y)
                .setInterpolator(fastOutLinearInInterpolator)
                .alpha(0);

        offset = getEndCallOffsetEndHidden(mFreemeBottomBtnContainer);
        // Use a fast out interpolator to quickly fade out the button. This is important because the
        // button can't draw under the navigation bar which means that it'll look weird if it just
        // abruptly disappears when it reaches the edge of the naivgation bar.
        mFreemeBottomBtnContainer
                .animate()
                .translationX(offset.x)
                .translationY(offset.y)
                .setInterpolator(fastOutLinearInInterpolator)
                .alpha(0)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                mFreemeBottomBtnContainer.setVisibility(View.INVISIBLE);
                            }
                        })
                .setInterpolator(new FastOutLinearInInterpolator())
                .start();

        updateOverlayBackground();
        /// M: hide scroll view too.
        mVideoScrollView.setAlpha(0);
    }

    @Override
    public void onClick(View v) {
        if (v == endCallButton) {
            LogUtil.i("VideoCallFragment.onClick", "end call button clicked");
            inCallButtonUiDelegate.onEndCallClicked();
            videoCallScreenDelegate.resetAutoFullscreenTimer();
        } else if (v == swapCameraButton) {
            inCallButtonUiDelegate.toggleCameraClicked();
            videoCallScreenDelegate.resetAutoFullscreenTimer();
            /** M: Handle ext features @{*/
        } else {
            handleClick(v);
        }
        /** @}*/
    }

    @Override
    public void onCheckedChanged(CheckableLabeledButton button, boolean isChecked) {
        if (button == cameraOffButton) {
            if (!isChecked && !VideoUtils.hasCameraPermissionAndShownPrivacyToast(getContext())) {
                LogUtil.i("VideoCallFragment.onCheckedChanged", "show camera permission dialog");
                checkCameraPermission();
            } else {
                inCallButtonUiDelegate.pauseVideoClicked(isChecked);
                videoCallScreenDelegate.resetAutoFullscreenTimer();
            }
        } else if (button == muteButton) {
            inCallButtonUiDelegate.muteClicked(isChecked, true /* clickedByUser */);
            videoCallScreenDelegate.resetAutoFullscreenTimer();
            /** M: Handle ext features @{*/
        } else {
            handleCheckedChanged(button, isChecked);
        }
        /** @}*/
    }

    @Override
    public void showVideoViews(
            boolean shouldShowPreview, boolean shouldShowRemote, boolean isRemotelyHeld) {
        LogUtil.i(
                "VideoCallFragment.showVideoViews",
                "showPreview: %b, shouldShowRemote: %b",
                shouldShowPreview,
                shouldShowRemote);

        videoCallScreenDelegate.getLocalVideoSurfaceTexture().attachToTextureView(previewTextureView);
        videoCallScreenDelegate.getRemoteVideoSurfaceTexture().attachToTextureView(remoteTextureView);

        if (this.shouldShowRemote != shouldShowRemote
                /// M: ALPS03515553, call onhold on remote screen @{
                || this.isRemotelyHeld != isRemotelyHeld) {
            /// @}

            this.shouldShowRemote = shouldShowRemote;
            this.isRemotelyHeld = isRemotelyHeld;
            updateRemoteOffView();
        }
        if (this.shouldShowPreview != shouldShowPreview) {
            this.shouldShowPreview = shouldShowPreview;
            updatePreviewOffView();
        }
    }

    @Override
    public void onLocalVideoDimensionsChanged() {
        LogUtil.i("VideoCallFragment.onLocalVideoDimensionsChanged", null);
        updatePreviewVideoScaling();
    }

    @Override
    public void onLocalVideoOrientationChanged() {
        LogUtil.i("VideoCallFragment.onLocalVideoOrientationChanged", null);
        updatePreviewVideoScaling();
    }

    /**
     * Called when the remote video's dimensions change.
     */
    @Override
    public void onRemoteVideoDimensionsChanged() {
        LogUtil.i("VideoCallFragment.onRemoteVideoDimensionsChanged", null);
        updateRemoteVideoScaling();
    }

    @Override
    public void updateFullscreenAndGreenScreenMode(
            boolean shouldShowFullscreen, boolean shouldShowGreenScreen) {
        LogUtil.d(
                "VideoCallFragment.updateFullscreenAndGreenScreenMode",
                "shouldShowFullscreen: %b, shouldShowGreenScreen: %b",
                shouldShowFullscreen,
                shouldShowGreenScreen);

        if (getActivity() == null) {
            LogUtil.i("VideoCallFragment.updateFullscreenAndGreenScreenMode", "not attached to activity");
            return;
        }

        // Check if anything is actually going to change. The first time this function is called we
        // force a change by checking the hasInitializedScreenModes flag. We also force both fullscreen
        // and green screen modes to update even if only one has changed. That's because they both
        // depend on each other.
        if (hasInitializedScreenModes
                && shouldShowGreenScreen == isInGreenScreenMode
                && shouldShowFullscreen == isInFullscreenMode) {
            LogUtil.d(
                    "VideoCallFragment.updateFullscreenAndGreenScreenMode", "no change to screen modes");
            return;
        }

        /// M: ALPS03855504 ignore update greenscreen or fullscreen in background. @{
        if (!mIsVisible) {
            LogUtil.d(
                    "VideoCallFragment.updateFullscreenAndGreenScreenMode", "ignore update in background");
            return;
        }
        ///@}

        hasInitializedScreenModes = true;
        isInGreenScreenMode = shouldShowGreenScreen;
        isInFullscreenMode = shouldShowFullscreen;

        if (getView().isAttachedToWindow() && !ActivityCompat.isInMultiWindowMode(getActivity())) {
            controlsContainer.onApplyWindowInsets(getView().getRootWindowInsets());
        }
        if (shouldShowGreenScreen) {
            enterGreenScreenMode();
        /// M:[ALPS03837665] add local and peer swap video fuction for support CMCC and CT do video
        /// call's quality test. @{
        } else if (!videoCallScreenDelegate.isPreviewLarge()){
            exitGreenScreenMode();
        /// @}
        }
        if (shouldShowFullscreen) {
            enterFullscreenMode();
        } else {
            exitFullscreenMode();
        }

        FreemeOnHoldFragment onHoldFragment = ((FreemeOnHoldFragment) getChildFragmentManager()
                .findFragmentById(R.id.videocall_on_hold_banner));
        if (onHoldFragment != null) {
            onHoldFragment.setPadTopInset(!isInFullscreenMode);
        }
    }

    @Override
    public Fragment getVideoCallScreenFragment() {
        return this;
    }

    @Override
    @NonNull
    public String getCallId() {
        return Assert.isNotNull(getArguments().getString(ARG_CALL_ID));
    }

    @Override
    public void showButton(@InCallButtonIds int buttonId, boolean show) {
        LogUtil.v(
                "VideoCallFragment.showButton",
                "buttonId: %s, show: %b",
                InCallButtonIdsExtension.toString(buttonId),
                show);
        if (buttonId == InCallButtonIds.BUTTON_AUDIO) {
            speakerButtonController.setEnabled(show);
        } else if (buttonId == InCallButtonIds.BUTTON_MUTE) {
            muteButton.setEnabled(show);
        } else if (buttonId == InCallButtonIds.BUTTON_PAUSE_VIDEO) {
            /// M : hide pause @{
            cameraOffButton.setVisibility(show ? View.VISIBLE : View.GONE);
            /// @}
            cameraOffButton.setEnabled(show);
        } else if (buttonId == InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY) {
            switchOnHoldCallController.setVisible(show);
        } else if (buttonId == InCallButtonIds.BUTTON_SWITCH_CAMERA) {
            swapCameraButton.setEnabled(show);
        /** M: Handle ext features @{*/
        } else {
            showExtButtons(buttonId, show);
        }
        /** @}*/
    }

    @Override
    public void enableButton(@InCallButtonIds int buttonId, boolean enable) {
        LogUtil.v(
                "VideoCallFragment.setEnabled",
                "buttonId: %s, enable: %b",
                InCallButtonIdsExtension.toString(buttonId),
                enable);
        if (buttonId == InCallButtonIds.BUTTON_AUDIO) {
            speakerButtonController.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_MUTE) {
            muteButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_PAUSE_VIDEO) {
            cameraOffButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY) {
            switchOnHoldCallController.setEnabled(enable);
            /** M: Handle ext features @{*/
        } else {
            enableExtButton(buttonId, enable);
        }
        /** @}*/
    }

    @Override
    public void setEnabled(boolean enabled) {
        LogUtil.v("VideoCallFragment.setEnabled", "enabled: " + enabled);
        speakerButtonController.setEnabled(enabled);
        muteButton.setEnabled(enabled);
        cameraOffButton.setEnabled(enabled);
        switchOnHoldCallController.setEnabled(enabled);
        /** M: Handle ext features @{*/
        enableExtButtons(enabled);
        /** @}*/
    }

    @Override
    public void setHold(boolean value) {
        LogUtil.d("VideoCallFragment.setHold", "value: " + value);
        /// M: ALPS03729639. update the hold call button state based on call hold state. @{
        mHoldCallButton.setChecked(value);
        /// @}
    }

    @Override
    public void setCameraSwitched(boolean isBackFacingCamera) {
        LogUtil.i("VideoCallFragment.setCameraSwitched", "isBackFacingCamera: " + isBackFacingCamera);
    }

    @Override
    public void setVideoPaused(boolean isPaused) {
        LogUtil.d("VideoCallFragment.setVideoPaused", "isPaused: " + isPaused);
        cameraOffButton.setChecked(isPaused);
    }

    @Override
    public void setAudioState(CallAudioState audioState) {
        LogUtil.i("VideoCallFragment.setAudioState", "audioState: " + audioState);
        speakerButtonController.setAudioState(audioState);
        muteButton.setChecked(audioState.isMuted());
    }

    @Override
    public void updateButtonStates() {
        LogUtil.d("VideoCallFragment.updateButtonState", null);
        speakerButtonController.updateButtonState();
        switchOnHoldCallController.updateButtonState();
    }

    @Override
    public void updateInCallButtonUiColors() {
    }

    @Override
    public Fragment getInCallButtonUiFragment() {
        return this;
    }

    @Override
    public void showAudioRouteSelector() {
        LogUtil.i("VideoCallFragment.showAudioRouteSelector", null);
        AudioRouteSelectorDialogFragment.newInstance(inCallButtonUiDelegate.getCurrentAudioState())
                .show(getChildFragmentManager(), null);
    }

    @Override
    public void onAudioRouteSelected(int audioRoute) {
        LogUtil.i("VideoCallFragment.onAudioRouteSelected", "audioRoute: " + audioRoute);
        inCallButtonUiDelegate.setAudioRoute(audioRoute);
    }

    @Override
    public void onAudioRouteSelectorDismiss() {
    }

    @Override
    public void setPrimary(@NonNull PrimaryInfo primaryInfo) {
        LogUtil.d("VideoCallFragment.setPrimary", primaryInfo.toString());
        contactGridManager.setPrimary(primaryInfo);

        if (!TextUtils.isEmpty(primaryInfo.mId)) {
            FreemeOnHoldInfo info = new FreemeOnHoldInfo(primaryInfo.mId, primaryInfo.name, primaryInfo.number,
                    primaryInfo.nameIsNumber, primaryInfo.label, primaryInfo.contactInfoLookupKey,
                    primaryInfo.mIsConferenceCall);
            if (mFreemeCallInfo.containsKey(primaryInfo.mId)) {
                mFreemeCallInfo.get(primaryInfo.mId).copyData(info);
            } else {
                if (primaryInfo.mIsConferenceCall) {
                    mConferenceCallId = primaryInfo.mId;
                    mFreemeCallInfo.clear();
                } else {
                    if (mFreemeCallInfo.size() > 1 && !TextUtils.isEmpty(mConferenceCallId)) {
                        mFreemeCallInfo.remove(mConferenceCallId);
                    }
                }
                mFreemeCallInfo.put(primaryInfo.mId, info);
            }
        }
    }

    @Override
    public void setSecondary(@NonNull SecondaryInfo secondaryInfo) {
        LogUtil.d("VideoCallFragment.setSecondary", secondaryInfo.toString());
        if (!isAdded()) {
            savedSecondaryInfo = secondaryInfo;
            return;
        }
        savedSecondaryInfo = null;
        switchOnHoldCallController.setSecondaryInfo(secondaryInfo);
        updateButtonStates();

        if (!TextUtils.isEmpty(secondaryInfo.id)) {
            FreemeOnHoldInfo info = new FreemeOnHoldInfo(secondaryInfo.id, secondaryInfo.name,
                    secondaryInfo.number, secondaryInfo.nameIsNumber, secondaryInfo.label,
                    secondaryInfo.lookupKey, secondaryInfo.isConference);
            if (mFreemeCallInfo.containsKey(secondaryInfo.id)) {
                mFreemeCallInfo.get(secondaryInfo.id).copyData(info);
            } else {
                if (!TextUtils.isEmpty(mConferenceCallId)) {
                    mFreemeCallInfo.remove(mConferenceCallId);
                }
                mFreemeCallInfo.put(secondaryInfo.id, info);
            }
        }

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        Fragment oldBanner = getChildFragmentManager().findFragmentById(R.id.videocall_on_hold_banner);
        if (secondaryInfo.shouldShow) {
            if (oldBanner != null && oldBanner instanceof FreemeOnHoldFragment) {
                ((FreemeOnHoldFragment) oldBanner).updateCallerInfo(mFreemeCallInfo);
                ((FreemeOnHoldFragment) oldBanner).setPadTopInset(!isInFullscreenMode);
            } else {
                FreemeOnHoldFragment fragment = FreemeOnHoldFragment.newInstance(mFreemeCallInfo);
                transaction.replace(R.id.videocall_on_hold_banner, fragment);
                fragment.setPadTopInset(!isInFullscreenMode);
            }
        } else {
            if (oldBanner != null) {
                transaction.remove(oldBanner);
            }
        }
        transaction.setCustomAnimations(R.anim.abc_slide_in_top, R.anim.abc_slide_out_top);
        transaction.commitAllowingStateLoss();
    }

    @Override
    public void setCallState(@NonNull PrimaryCallState primaryCallState) {
        LogUtil.d("VideoCallFragment.setCallState", primaryCallState.toString());
        contactGridManager.setCallState(primaryCallState);

        if (mFreemeCallInfo.containsKey(primaryCallState.mId)) {
            FreemeOnHoldInfo info = mFreemeCallInfo.get(primaryCallState.mId);
            info.connectTimeMillis = primaryCallState.connectTimeMillis;
            info.state = primaryCallState.state;
        }

        if (isAdded()) { // fragment maybe not been attached
            Fragment oldBanner = getChildFragmentManager().findFragmentById(R.id.videocall_on_hold_banner);
            if (oldBanner != null && oldBanner instanceof FreemeOnHoldFragment) {
                ((FreemeOnHoldFragment) oldBanner).updateCallerInfo(mFreemeCallInfo);
            }
        }
    }

    @Override
    public void setEndCallButtonEnabled(boolean enabled, boolean animate) {
        LogUtil.d("VideoCallFragment.setEndCallButtonEnabled", "enabled: " + enabled);
    }

    @Override
    public void showManageConferenceCallButton(boolean visible) {
        LogUtil.d("VideoCallFragment.showManageConferenceCallButton", "visible: " + visible);
        /// M: Video Conference. @{
        mVideoConferenceButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        mVideoConferenceButton.setEnabled(visible);
        /// @}
    }

    @Override
    public boolean isManageConferenceVisible() {
        LogUtil.d("VideoCallFragment.isManageConferenceVisible", null);
        /// M: Video Conference. @{
        return mVideoConferenceButton.getVisibility() == View.VISIBLE;
        /// @}
    }

    @Override
    public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        contactGridManager.dispatchPopulateAccessibilityEvent(event);
    }

    @Override
    public void showNoteSentToast() {
        LogUtil.i("VideoCallFragment.showNoteSentToast", null);
    }

    @Override
    public void updateInCallScreenColors() {
        LogUtil.d("VideoCallFragment.updateColors", null);
    }

    @Override
    public void onInCallScreenDialpadVisibilityChange(boolean isShowing) {
        LogUtil.i("VideoCallFragment.onInCallScreenDialpadVisibilityChange", null);
    }

    @Override
    public int getAnswerAndDialpadContainerResourceId() {
        return 0;
    }

    @Override
    public Fragment getInCallScreenFragment() {
        return this;
    }

    @Override
    public boolean isShowingLocationUi() {
        return false;
    }

    @Override
    public void showLocationUi(Fragment locationUi) {
        LogUtil.e("VideoCallFragment.showLocationUi", "Emergency video calling not supported");
        // Do nothing
    }

    private void updatePreviewVideoScaling() {
        if (previewTextureView.getWidth() == 0 || previewTextureView.getHeight() == 0) {
            LogUtil.i("VideoCallFragment.updatePreviewVideoScaling", "view layout hasn't finished yet");
            return;
        }
        VideoSurfaceTexture localVideoSurfaceTexture =
                videoCallScreenDelegate.getLocalVideoSurfaceTexture();
        Point cameraDimensions = localVideoSurfaceTexture.getSurfaceDimensions();
        if (cameraDimensions == null) {
            LogUtil.i(
                    "VideoCallFragment.updatePreviewVideoScaling", "camera dimensions haven't been set");
            return;
        }
        if (isLandscape()) {
            /// M:[ALPS03837665] add local and peer swap video fuction for support CMCC and CT do video
            /// call's quality test. @{
            if (videoCallScreenDelegate.isPreviewLarge()) {
                VideoSurfaceBindings.scaleVideoMaintainingAspectRatioWithOrientation(
                        previewTextureView,
                        cameraDimensions.x,
                        cameraDimensions.y,
                        videoCallScreenDelegate.getDeviceOrientation());
            } else {
            /// @}
            VideoSurfaceBindings.scaleVideoAndFillView(
                    previewTextureView,
                    cameraDimensions.x,
                    cameraDimensions.y,
                    videoCallScreenDelegate.getDeviceOrientation());
            }
        } else {

            /// M: ALPS03646689, switch x y value to get correct scale while not in landscape
            /// but with 90 degree. @{
            ///google original code:
            int rotationDegree = videoCallScreenDelegate.getDeviceOrientation();
            int yDimension = cameraDimensions.y;
            int xDimension = cameraDimensions.x;
            Context context = getContext();
            if ((rotationDegree == 90 || rotationDegree == 270) &&
                    context != null &&
                    isOrientationLocked(context)) {
                xDimension = cameraDimensions.y;
                yDimension = cameraDimensions.x;
                LogUtil.d("VideoCallFragment.updatePreviewVideoScaling", "switch camera dimensions");
            }
            /// M:[ALPS03837665] add local and peer swap video fuction for support CMCC and CT do video
            /// call's quality test. @{
            if (videoCallScreenDelegate.isPreviewLarge()) {
                VideoSurfaceBindings.scaleVideoMaintainingAspectRatioWithOrientation(
                        previewTextureView,
                        yDimension,
                        xDimension,
                        rotationDegree);
            } else {
            /// @}
            VideoSurfaceBindings.scaleVideoAndFillView(
                    previewTextureView,
                    yDimension,
                    xDimension,
                    rotationDegree);
            /// @}
            }
        }
    }

    private void updateRemoteVideoScaling() {
        VideoSurfaceTexture remoteVideoSurfaceTexture =
                videoCallScreenDelegate.getRemoteVideoSurfaceTexture();
        Point videoSize = remoteVideoSurfaceTexture.getSourceVideoDimensions();
        if (videoSize == null) {
            LogUtil.i("VideoCallFragment.updateRemoteVideoScaling", "video size is null");
            return;
        }
        if (remoteTextureView.getWidth() == 0 || remoteTextureView.getHeight() == 0) {
            LogUtil.i("VideoCallFragment.updateRemoteVideoScaling", "view layout hasn't finished yet");
            return;
        }

        // If the video and display aspect ratio's are close then scale video to fill display
        float videoAspectRatio = ((float) videoSize.x) / videoSize.y;
        float displayAspectRatio =
                ((float) remoteTextureView.getWidth()) / remoteTextureView.getHeight();
        float delta = Math.abs(videoAspectRatio - displayAspectRatio);
        float sum = videoAspectRatio + displayAspectRatio;
        if ((delta / sum < ASPECT_RATIO_MATCH_THRESHOLD &&
                /// M: cmcc requires peer video can't be cropped by plugin to implement. @{
                ExtensionManager.getVilteAutoTestHelperExt().isAllowVideoCropped())
                /// @}
                /// M:[ALPS03837665] add local and peer swap video fuction for support CMCC and CT do
                /// video call's quality test. @{
                || videoCallScreenDelegate.isPreviewLarge()) {
                ///@}
            VideoSurfaceBindings.scaleVideoAndFillView(remoteTextureView, videoSize.x, videoSize.y, 0);
        } else {
            VideoSurfaceBindings.scaleVideoMaintainingAspectRatio(
                    remoteTextureView, videoSize.x, videoSize.y);
        }
    }

    private boolean isLandscape() {
        /// M: Fixed no point JE issue @{
        if (getActivity() == null) {
            return false;
        }
        /// @}
        // Choose orientation based on display orientation, not window orientation
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
    }

    private void enterGreenScreenMode() {
        LogUtil.i("VideoCallFragment.enterGreenScreenMode", null);
        updateOverlayBackground();
        contactGridManager.setIsMiddleRowVisible(true);

        /// M: ALPS03659894, update screen after request fail @{
        previewTextureView.setTranslationY(0);
        previewOffOverlay.setTranslationY(0);
        /// @}
    }

    private void exitGreenScreenMode() {
        LogUtil.i("VideoCallFragment.exitGreenScreenMode", null);
        updateOverlayBackground();
        contactGridManager.setIsMiddleRowVisible(true);
    }

    private void updatePreviewOffView() {
        LogUtil.enterBlock("VideoCallFragment.updatePreviewOffView");

        // Always hide the preview off and remote off views in green screen mode.
        /// M: ALPS03506468 Record hide preview and hide previewoffView @{
        /// boolean previewEnabled = isInGreenScreenMode || shouldShowPreview;
        boolean previewEnabled = isInGreenScreenMode || shouldShowPreview || mHidePreview;
        /// @}
        previewOffOverlay.setVisibility(previewEnabled ? View.GONE : View.VISIBLE);
    }

    private void updateRemoteOffView() {
        LogUtil.enterBlock("VideoCallFragment.updateRemoteOffView");
        boolean remoteEnabled = isInGreenScreenMode || shouldShowRemote;
        boolean isResumed = remoteEnabled && !isRemotelyHeld;
        if (isResumed) {
            boolean wasRemoteVideoOff =
                    TextUtils.equals(
                            remoteVideoOff.getText(),
                            remoteVideoOff.getResources().getString(R.string.videocall_remote_video_off));
            // The text needs to be updated and hidden after enough delay in order to be announced by
            // talkback.
            remoteVideoOff.setText(
                    wasRemoteVideoOff
                            ? R.string.videocall_remote_video_on
                            : R.string.videocall_remotely_resumed);
            remoteVideoOff.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            remoteVideoOff.setVisibility(View.GONE);
                        }
                    },
                    VIDEO_OFF_VIEW_FADE_OUT_DELAY_IN_MILLIS);
        } else {
            remoteVideoOff.setText(
                    isRemotelyHeld ? R.string.videocall_remotely_held : R.string.videocall_remote_video_off);
            remoteVideoOff.setVisibility(View.VISIBLE);
        }
        updateBlurredImageView(
                remoteTextureView,
                remoteOffBlurredImageView,
                shouldShowRemote,
                BLUR_REMOTE_RADIUS,
                BLUR_REMOTE_SCALE_FACTOR);
    }

    @VisibleForTesting
    void updateBlurredImageView(
            TextureView textureView,
            ImageView blurredImageView,
            boolean isVideoEnabled,
            float blurRadius,
            float scaleFactor) {
        Context context = getContext();

        if (isVideoEnabled || context == null) {
            blurredImageView.setImageBitmap(null);
            blurredImageView.setVisibility(View.GONE);
            return;
        }

        long startTimeMillis = SystemClock.elapsedRealtime();
        int width = Math.round(textureView.getWidth() * scaleFactor);
        int height = Math.round(textureView.getHeight() * scaleFactor);

        LogUtil.i("VideoCallFragment.updateBlurredImageView", "width: %d, height: %d", width, height);

        // This call takes less than 10 milliseconds.
        Bitmap bitmap = textureView.getBitmap(width, height);

        if (bitmap == null) {
            blurredImageView.setImageBitmap(null);
            blurredImageView.setVisibility(View.GONE);
            return;
        }

        // TODO: When the view is first displayed after a rotation the bitmap is empty
        // and thus this blur has no effect.
        // This call can take 100 milliseconds.
        blur(getContext(), bitmap, blurRadius);

        // TODO: Figure out why only have to apply the transform in landscape mode
        if (width > height) {
            bitmap =
                    Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.getWidth(),
                            bitmap.getHeight(),
                            textureView.getTransform(null),
                            true);
        }

        blurredImageView.setImageBitmap(bitmap);
        blurredImageView.setVisibility(View.VISIBLE);

        LogUtil.i(
                "VideoCallFragment.updateBlurredImageView",
                "took %d millis",
                (SystemClock.elapsedRealtime() - startTimeMillis));
    }

    private void updateOverlayBackground() {
        if (isInGreenScreenMode) {
            // We want to darken the preview view to make text and buttons readable. The fullscreen
            // background is below the preview view so use the green screen background instead.
            animateSetVisibility(greenScreenBackgroundView, View.VISIBLE);
            animateSetVisibility(fullscreenBackgroundView, View.GONE);
        } else if (!isInFullscreenMode) {
            // We want to darken the remote view to make text and buttons readable. The green screen
            // background is above the preview view so it would darken the preview too. Use the fullscreen
            // background instead.
            animateSetVisibility(greenScreenBackgroundView, View.GONE);
            animateSetVisibility(fullscreenBackgroundView, View.VISIBLE);
        } else {
            animateSetVisibility(greenScreenBackgroundView, View.GONE);
            animateSetVisibility(fullscreenBackgroundView, View.GONE);
        }
    }

    private void animateSetVisibility(final View view, final int visibility) {
        /// M: ALPS03460593, Sometimes when user click quick, will get incorrect result to
        /// get the visibility of the view. Because we use animate to modify the visibility
        /// of view. Should record latest visibility of fullscreen. @{
        LogUtil.d("VideoCallFragment.animateSetVisibility", "" + visibility);
        if ((view.getVisibility() == visibility &&
                view != fullscreenBackgroundView) ||
                (view == fullscreenBackgroundView &&
                        mFullscreenLatestVisibility == visibility)) {
            return;
        }
        /// @}

        int startAlpha;
        int endAlpha;
        if (visibility == View.GONE) {
            startAlpha = 1;
            endAlpha = 0;
        } else if (visibility == View.VISIBLE) {
            startAlpha = 0;
            endAlpha = 1;
        } else {
            Assert.fail();
            return;
        }

        /// M: ALPS03460593, Sometimes when user click quick, will get incorrect result to
        /// get the visibility of the view. Because we use animate to modify the visibility
        /// of view. Should record latest visibility of fullscreen. @{
        if (view == fullscreenBackgroundView) {
            LogUtil.d("VideoCallFragment.animateSetVisibility", "" + visibility);
            mFullscreenLatestVisibility = visibility;
        }
        /// @}

        view.setAlpha(startAlpha);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(endAlpha)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {
                                view.setVisibility(visibility);
                            }
                        })
                .start();
    }

    public static void blur(Context context, Bitmap image, float blurRadius) {
        RenderScript renderScript = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript =
                ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        Allocation allocationIn = Allocation.createFromBitmap(renderScript, image);
        Allocation allocationOut = Allocation.createFromBitmap(renderScript, image);
        blurScript.setInput(allocationIn);
        blurScript.forEach(allocationOut);
        allocationOut.copyTo(image);
        blurScript.destroy();
        allocationIn.destroy();
        allocationOut.destroy();
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        /// M: ALPS03449886 fix don't enter full screen when click quickly.add check
        ///View.SYSTEM_UI_FLAG_FULLSCREEN to judge whether should set full screen false or not
        ///when callback of onSystemUiVisibilityChange. @{
        ///Google original code:
        ///boolean navBarVisible = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
        LogUtil.i("VideoCallFragment.onSystemUiVisibilityChange", "visible:" + visibility);
        int flag = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        boolean navBarVisible = (visibility & flag) == 0;
        ///@}
        videoCallScreenDelegate.onSystemUiVisibilityChange(navBarVisible);
    }

    private void checkCameraPermission() {
        // Checks if user has consent of camera permission and the permission is granted.
        // If camera permission is revoked, shows system permission dialog.
        // If camera permission is granted but user doesn't have consent of camera permission
        // (which means it's first time making video call), shows custom dialog instead. This
        // will only be shown to user once.
        if (!VideoUtils.hasCameraPermissionAndShownPrivacyToast(getContext())) {
            videoCallScreenDelegate.onCameraPermissionDialogShown();
            if (!VideoUtils.hasCameraPermission(getContext())) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                PermissionsUtil.showCameraPermissionToast(getContext());
                videoCallScreenDelegate.onCameraPermissionGranted();
            }
        }
    }

    /// M: -------------- Mediatek Feature-------------
    private CheckableLabeledButton mDowngradeAudioButton = null;
    private View mVideoScrollView = null;
    private CheckableLabeledButton mHidePrviewButton = null;
    private CheckableLabeledButton mAddCallButton = null;
    private CheckableLabeledButton mMergeCallButton = null;
    private CheckableLabeledButton mVideoConferenceButton = null;
    private CheckableLabeledButton mCancelUpgradeButton = null;
    private CheckableLabeledButton mEctButton = null;
    private CheckableLabeledButton mBlindEctButton = null;
    private CheckableLabeledButton mDialpadButton = null;
    private CheckableLabeledButton mHoldCallButton = null;
    /// M: ALPS03916818, using different value to make sure animateSetVisibility will
    /// always be called after videocallfragment create. @{
    private int mFullscreenLatestVisibility = View.INVISIBLE;
    /// @}
    private boolean mIsVisible = false;

    private void initView(View view) {
        mDowngradeAudioButton = view.findViewById(R.id.videocall_downgrade_audio);
        mDowngradeAudioButton.setOnClickListener(this);

        mVideoScrollView =  view.findViewById(R.id.videocall_video_controls_scrollview);
        mVideoScrollView.setVisibility(
                ActivityCompat.isInMultiWindowMode(getActivity()) ? View.GONE : View.VISIBLE);
        mHidePrviewButton = view.findViewById(R.id.hide_preview);
        mHidePrviewButton.setOnCheckedChangeListener(this);

        mAddCallButton = view.findViewById(R.id.add_call);
        mAddCallButton.setOnClickListener(this);

        mMergeCallButton = view.findViewById(R.id.merge_call);
        mMergeCallButton.setOnClickListener(this);

        mVideoConferenceButton = view.findViewById(R.id.manage_video_conference);
        mVideoConferenceButton.setOnClickListener(this);

        mCancelUpgradeButton = view.findViewById(R.id.cancel_upgrade);
        mCancelUpgradeButton.setOnClickListener(this);

        mDialpadButton = view.findViewById(R.id.video_dialpad);
        mDialpadButton.setIsHideLabel(true);
        mDialpadButton.setOnCheckedChangeListener(this);

        mHoldCallButton = view.findViewById(R.id.hold_call);
        mHoldCallButton.setOnCheckedChangeListener(this);

        mEctButton = view.findViewById(R.id.transfer_call);
        mEctButton.setOnClickListener(this);

        mBlindEctButton = view.findViewById(R.id.blind_transfer_call);
        mBlindEctButton.setOnClickListener(this);

        mFreemeRecordNumber = view.findViewById(R.id.freeme_record_number);
        mFreemeRecordNumber.setOnCheckedChangeListener(this);

        mFreemeTopInfoConatiner = view.findViewById(R.id.freeme_top_info_conatiner);
    }

    private void handleCheckedChanged(CheckableLabeledButton button,
                                      boolean isChecked) {
        LogUtil.i("VideoCallFragment.handleCheckedChanged", "button: %s, checked: %b",
                button, isChecked);
        if (button == mHidePrviewButton) {
            inCallButtonUiDelegate.hidePreviewClicked(isChecked);
            videoCallScreenDelegate.resetAutoFullscreenTimer();
            mHidePrviewButton.setChecked(isChecked);
        } else if (button == mDialpadButton) {
            inCallButtonUiDelegate.showDialpadClicked(isChecked);
            videoCallScreenDelegate.resetAutoFullscreenTimer();
            mDialpadButton.setChecked(isChecked);
            inCallButtonUiDelegate.setIsNumberRecord(false);
        } else if (button == mFreemeRecordNumber) {
            inCallButtonUiDelegate.showDialpadClicked(isChecked);
            videoCallScreenDelegate.resetAutoFullscreenTimer();
            mFreemeRecordNumber.setChecked(isChecked);
            inCallButtonUiDelegate.setIsNumberRecord(true);
        } else if (button == mHoldCallButton) {
            inCallButtonUiDelegate.holdClicked(isChecked);
            videoCallScreenDelegate.resetAutoFullscreenTimer();
            mHoldCallButton.setChecked(isChecked);
        }
    }

    private void handleClick(View v) {
        if (v == mDowngradeAudioButton) {
            LogUtil.i("VideoCallFragment.onClick", "downgrade audio button clicked");
            inCallButtonUiDelegate.changeToAudioClicked();
        } else if (v == mAddCallButton) {
            LogUtil.i("VideoCallFragment.onClick", "add call button clicked");
            inCallButtonUiDelegate.addCallClicked();
        } else if (v == mMergeCallButton) {
            LogUtil.i("VideoCallFragment.onClick", "merge call button clicked");
            inCallButtonUiDelegate.mergeClicked();
        } else if (v == mVideoConferenceButton) {
            LogUtil.i("VideoCallFragment.onClick", "manage conference button clicked");
            inCallScreenDelegate.onManageConferenceClicked();
        } else if (v == mCancelUpgradeButton) {
            LogUtil.i("VideoCallFragment.onClick", "cancel ugrade button clicked");
            inCallButtonUiDelegate.cancelUpgradeClicked();
        } else if (v == mEctButton) {
            LogUtil.i("VideoCallFragment.onClick", "ect button clicked");
            inCallButtonUiDelegate.onConsultativeEctClicked();
        } else if (v == mBlindEctButton) {
            LogUtil.i("VideoCallFragment.onClick", "blind ect button clicked");
            inCallButtonUiDelegate.onBlindOrAssuredEctClicked();
        }
        videoCallScreenDelegate.resetAutoFullscreenTimer();
    }

    private void enableExtButton(@InCallButtonIds int buttonId, boolean enable) {
        if (buttonId == InCallButtonIds.BUTTON_DOWNGRADE_TO_AUDIO) {
            mDowngradeAudioButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_HIDE_PREVIEW) {
            mHidePrviewButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_ADD_CALL) {
            mAddCallButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_MERGE) {
            mMergeCallButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_MANAGE_VIDEO_CONFERENCE) {
            mVideoConferenceButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_CANCEL_UPGRADE) {
            mCancelUpgradeButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_DIALPAD) {
            mDialpadButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_HOLD) {
            mHoldCallButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_ECT) {
            mEctButton.setEnabled(enable);
        } else if (buttonId == InCallButtonIds.BUTTON_BLIND_ECT) {
            mBlindEctButton.setEnabled(enable);
        }
    }

    private void showExtButtons(@InCallButtonIds int buttonId, boolean show) {
        if (buttonId == InCallButtonIds.BUTTON_ADD_CALL) {
            mAddCallButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_MERGE) {
            mMergeCallButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_MANAGE_VIDEO_CONFERENCE) {
            mVideoConferenceButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_CANCEL_UPGRADE) {
            mCancelUpgradeButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_DIALPAD) {
            mDialpadButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_HOLD) {
            mHoldCallButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_ECT) {
            mEctButton.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (buttonId == InCallButtonIds.BUTTON_BLIND_ECT) {
            mBlindEctButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void enableExtButtons(boolean enable) {
        mDowngradeAudioButton.setEnabled(enable);
        mHidePrviewButton.setEnabled(enable);
        mAddCallButton.setEnabled(enable);
        mMergeCallButton.setEnabled(enable);
        mVideoConferenceButton.setEnabled(enable);
        mCancelUpgradeButton.setEnabled(enable);
        mDialpadButton.setEnabled(enable);
        mHoldCallButton.setEnabled(enable);
        mEctButton.setEnabled(enable);
        mBlindEctButton.setEnabled(enable);
    }

    @Override
    public void hidePreview(boolean hide) {
        if (previewTextureView == null) {
            return;
        }
        mHidePreview = hide;
        LogUtil.i("VideoCallFragment.hidePreview", "hide: " + hide);
        previewTextureView.setVisibility(hide ? View.GONE : View.VISIBLE);
        /// M: update state @{
        if (hide) {
            updatePreviewVideoScaling();
            updatePreviewOffView();
        }
        mHidePrviewButton.setChecked(hide);
        /// @}
    }

    private FreemeVideoDebugInfo mVideoDebugInfo = null;

    @Override
    public void updateDeclineTimer() {
    }

    @Override
    public void updateVideoDebugInfo(long dataUsage) {
        if (mVideoDebugInfo != null && FreemeVideoDebugInfo.isFeatureOn()) {
            mVideoDebugInfo.onCallDataUsageChange(dataUsage);
        }
    }

    /// M: [Voice Record]
    @Override
    public void updateRecordStateUi(boolean isRecording) {
        // do nothing.
    }

    @Override
    public int getDialpadContainerResourceId() {
        return R.id.incall_dialpad_container;
    }

    @Override
    public void onVideoCallScreenDialpadVisibilityChange(boolean isShowing) {
        mDialpadButton.setChecked(isShowing);
        mFreemeRecordNumber.setChecked(isShowing);
    }

    /// M: ALPS03646689, switch x y value to get correct scale while not in landscape
    /// but with 90 degree. @{
    private boolean isOrientationLocked(Context cnx) {
        return Settings.System.getInt(cnx.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 1) != 1;
    }
    /// @}

    /// M:[ALPS03837665] add local and peer swap video fuction for support CMCC and CT do video
    /// call's quality test. @{
    @Override
    public void switchLocalAndPeer(boolean shouldShowLargePreview) {
        LogUtil.i("VideoCallFragment.switchLocalAndPeer",
                "shouldShowLargePreview: %b", shouldShowLargePreview);
        if (getActivity() == null) {
            LogUtil.i("VideoCallFragment.switchLocalAndPeer", "not attached to activity");
            return;
        }
        if (shouldShowLargePreview) {
            enterLargeLocalMode();
            enterSmalPeerMode();
        } else {
            enterLargePeerMode();
            exitGreenScreenMode();
        }
    }

    private void enterLargeLocalMode() {
        LogUtil.i("VideoCallFragment.enterLargeLocalMode", null);
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        previewTextureView.setLayoutParams(params);
        previewTextureView.setTranslationX(0);
        previewTextureView.setTranslationY(0);
        previewTextureView.setOutlineProvider(null);
    }

    private void enterLargePeerMode() {
        LogUtil.i("VideoCallFragment.enterLargePeerMode", null);
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        remoteTextureView.setLayoutParams(params);
        remoteTextureView.setOutlineProvider(null);
    }

    private void enterSmalPeerMode() {
        LogUtil.i("VideoCallFragment.enterSmalPeerMode", null);
        Resources resources = getResources();
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        (int) resources.getDimension(R.dimen.videocall_preview_width),
                        (int) resources.getDimension(R.dimen.videocall_preview_height));
        params.setMargins(
                0, 0, 0, (int) resources.getDimension(R.dimen.videocall_preview_margin_bottom));
        if (isLandscape()) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.setMarginEnd((int) resources.getDimension(R.dimen.videocall_preview_margin_end));
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            params.setMarginStart((int) resources.getDimension(R.dimen.videocall_preview_margin_start));
        }
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        remoteTextureView.setLayoutParams(params);
        remoteTextureView.setOutlineProvider(circleOutlineProvider);
        remoteTextureView.setClipToOutline(true);
    }
    /// @}

    private View mFreemeBottomBtnContainer;
    private CheckableLabeledButton mFreemeRecordNumber;
    private View mFreemeTopInfoConatiner;

    public void performShowOrHideOtherInfo(boolean show) {
        if (!show) {
            Interpolator interpolator = new FastOutLinearInInterpolator();
            // Animate controls to the hidden state.
            Point offset = getControlsOffsetEndHidden(controls);
            controls.animate()
                    .translationX(offset.x)
                    .translationY(offset.y)
                    .setInterpolator(interpolator)
                    .alpha(0)
                    .start();

            // Animate contact grid to the hidden state.
            offset = getContactGridOffsetEndHidden(mFreemeTopInfoConatiner);
            mFreemeTopInfoConatiner
                    .animate()
                    .translationX(offset.x)
                    .translationY(offset.y)
                    .setInterpolator(interpolator)
                    .alpha(0);

            mVideoScrollView.setAlpha(0);
        } else {
            LinearOutSlowInInterpolator interpolator = new LinearOutSlowInInterpolator();

            // Animate the controls to the shown state.
            controls.animate()
                    .translationX(0)
                    .translationY(0)
                    .setInterpolator(interpolator)
                    .alpha(1)
                    .start();

            // Animate contact grid to the shown state.
            mFreemeTopInfoConatiner
                    .animate()
                    .translationX(0)
                    .translationY(0)
                    .setInterpolator(interpolator)
                    .alpha(1)
                    .withStartAction(
                            new Runnable() {
                                @Override
                                public void run() {
                                    mFreemeTopInfoConatiner.setVisibility(View.VISIBLE);
                                }
                            });
            mVideoScrollView.setAlpha(1);
        }
    }

    private LinkedHashMap<String, FreemeOnHoldInfo> mFreemeCallInfo = new LinkedHashMap<>();
    private String mConferenceCallId;

    private void showOrHideVideoCallInfo(boolean hide) {
        if (hide) {
            contactGridManager.hide();
        } else {
            contactGridManager.show();
        }
    }
}
// LINT.ThenChange(//depot/google3/third_party/java_src/android_app/dialer/java/com/android/incallui/video/impl/SurfaceViewVideoCallFragment.java)
