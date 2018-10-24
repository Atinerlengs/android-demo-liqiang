package com.freeme.incallui.answer.impl;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.dialer.common.LogUtil;
import com.android.dialer.common.MathUtil;
import com.android.dialer.compat.ActivityCompat;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.multimedia.MultimediaData;
import com.android.dialer.util.ViewUtil;
import com.android.incallui.InCallPresenter;
import com.android.incallui.answer.impl.AffordanceHolderLayout;
import com.android.incallui.answer.impl.AnswerVideoCallScreen;
import com.android.incallui.answer.impl.CreateCustomSmsDialogFragment;
import com.android.incallui.answer.impl.R;
import com.android.incallui.answer.impl.SelfManagedAnswerVideoCallScreen;
import com.android.incallui.answer.impl.SmsBottomSheetFragment;
import com.android.incallui.answer.impl.answermethod.AnswerMethodHolder;
import com.android.incallui.answer.impl.utils.Interpolators;
import com.android.incallui.answer.protocol.AnswerScreen;
import com.android.incallui.answer.protocol.AnswerScreenDelegate;
import com.android.incallui.answer.protocol.AnswerScreenDelegateFactory;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.incall.protocol.InCallScreen;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.InCallScreenDelegateFactory;
import com.android.incallui.incall.protocol.PrimaryCallState;
import com.android.incallui.incall.protocol.PrimaryInfo;
import com.android.incallui.incall.protocol.SecondaryInfo;
import com.android.incallui.maps.MapsComponent;
import com.android.incallui.sessiondata.AvatarPresenter;
import com.android.incallui.sessiondata.MultimediaFragment;
import com.android.incallui.util.AccessibilityUtil;
import com.android.incallui.video.protocol.VideoCallScreen;
import com.android.incallui.videotech.utils.VideoUtils;
import com.freeme.incallui.contactgrid.FreemeContactGridManager;
import com.freeme.incallui.widgets.FreemeCustomAnswerView;
import com.mediatek.incallui.dsda.DsdaCallController;
import com.mediatek.incallui.dsda.DsdaFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FreemeAnswerFragment extends Fragment
        implements AnswerScreen,
        InCallScreen,
        SmsBottomSheetFragment.SmsSheetHolder,
        FreemeAnswerBootSheetFragment.AnswerMenuSheetHolder,
        FreemeCustomAnswerView.IControllerCallBack,
        CreateCustomSmsDialogFragment.CreateCustomSmsHolder,
        AnswerMethodHolder,
        View.OnClickListener,
        MultimediaFragment.Holder {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String ARG_CALL_ID = "call_id";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String ARG_IS_VIDEO_CALL = "is_video_call";

    static final String ARG_ALLOW_ANSWER_AND_RELEASE = "allow_answer_and_release";

    static final String ARG_HAS_CALL_ON_HOLD = "has_call_on_hold";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String ARG_IS_VIDEO_UPGRADE_REQUEST = "is_video_upgrade_request";

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String ARG_IS_SELF_MANAGED_CAMERA = "is_self_managed_camera";

    private static final String STATE_HAS_ANIMATED_ENTRY = "hasAnimated";

    private static final int HINT_SECONDARY_SHOW_DURATION_MILLIS = 5000;
    private static final float ANIMATE_LERP_PROGRESS = 0.5f;
    private static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;
    private static final int STATUS_BAR_DISABLE_HOME = 0x00200000;
    private static final int STATUS_BAR_DISABLE_BACK = 0x00400000;

    private static void fadeToward(View view, float newAlpha) {
        view.setAlpha(MathUtil.lerp(view.getAlpha(), newAlpha, ANIMATE_LERP_PROGRESS));
    }

    private static void scaleToward(View view, float newScale) {
        view.setScaleX(MathUtil.lerp(view.getScaleX(), newScale, ANIMATE_LERP_PROGRESS));
        view.setScaleY(MathUtil.lerp(view.getScaleY(), newScale, ANIMATE_LERP_PROGRESS));
    }

    private AnswerScreenDelegate answerScreenDelegate;
    private InCallScreenDelegate inCallScreenDelegate;

    private View importanceBadge;
    private AffordanceHolderLayout affordanceHolderLayout;
    // Use these flags to prevent user from clicking accept/reject buttons multiple times.
    // We use separate flags because in some rare cases accepting a call may fail to join the room,
    // and then user is stuck in the incoming call view until it times out. Two flags at least give
    // the user a chance to get out of the CallActivity.
    private boolean buttonAcceptClicked;
    private boolean buttonRejectClicked;
    private boolean hasAnimatedEntry;
    private PrimaryInfo primaryInfo = PrimaryInfo.createEmptyPrimaryInfo();
    private PrimaryCallState primaryCallState;
    private ArrayList<CharSequence> textResponses;
    private SmsBottomSheetFragment textResponsesFragment;
    private CreateCustomSmsDialogFragment createCustomSmsDialogFragment;
    private FreemeContactGridManager contactGridManager;
    private VideoCallScreen answerVideoCallScreen;
    private Handler handler = new Handler(Looper.getMainLooper());

    public enum SecondaryBehavior {
        ANSWER_AUDIO(R.string.call_incoming_answer) {
            @Override
            public void performAction(FreemeAnswerFragment fragment) {
                fragment.answerFromMethod();
            }
        },

        ANSWER_VIDEO_CALL(R.string.freeme_incall_answer_video_call) {
            @Override
            public void performAction(FreemeAnswerFragment fragment) {
                fragment.answerFromMethod();
            }
        },

        ANSWER_VIDEO_AS_AUDIO(R.string.freeme_incall_answer_video_call_as_audio) {
            @Override
            public void performAction(FreemeAnswerFragment fragment) {
                fragment.acceptCallByUser(true /* answerVideoAsAudio */);
            }
        },

        ANSWER_AND_RELEASE(R.string.a11y_incoming_call_answer_and_release) {
            @Override
            public void performAction(FreemeAnswerFragment fragment) {
                fragment.performAnswerAndRelease();
            }
        },

        CANCEL(R.string.text_cancel) {
            @Override
            public void performAction(FreemeAnswerFragment fragment) {}
        };

        @StringRes
        public final int contentDescription;

        SecondaryBehavior(@StringRes int contentDescription) {
            this.contentDescription = contentDescription;
        }

        public abstract void performAction(FreemeAnswerFragment fragment);
    }

    private void performAnswerAndRelease() {
        answerScreenDelegate.onAnswerAndReleaseCall();
        buttonAcceptClicked = true;
    }

    public static FreemeAnswerFragment newInstance(
            String callId,
            boolean isVideoCall,
            boolean isVideoUpgradeRequest,
            boolean isSelfManagedCamera,
            boolean allowAnswerAndRelease,
            boolean hasCallOnHold) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_CALL_ID, Assert.isNotNull(callId));
        bundle.putBoolean(ARG_IS_VIDEO_CALL, isVideoCall);
        bundle.putBoolean(ARG_IS_VIDEO_UPGRADE_REQUEST, isVideoUpgradeRequest);
        bundle.putBoolean(ARG_IS_SELF_MANAGED_CAMERA, isSelfManagedCamera);
        bundle.putBoolean(ARG_ALLOW_ANSWER_AND_RELEASE, allowAnswerAndRelease);
        bundle.putBoolean(ARG_HAS_CALL_ON_HOLD, hasCallOnHold);

        FreemeAnswerFragment instance = new FreemeAnswerFragment();
        instance.setArguments(bundle);
        return instance;
    }

    @Override
    public boolean isActionTimeout() {
        return (buttonAcceptClicked || buttonRejectClicked) && answerScreenDelegate.isActionTimeout();
    }

    @Override
    @NonNull
    public String getCallId() {
        return Assert.isNotNull(getArguments().getString(ARG_CALL_ID));
    }

    @Override
    public boolean isVideoUpgradeRequest() {
        return getArguments().getBoolean(ARG_IS_VIDEO_UPGRADE_REQUEST);
    }

    @Override
    public void setTextResponses(List<String> textResponses) {
        if (isVideoCall() || isVideoUpgradeRequest()) {
            LogUtil.i("FreemeAnswerFragment.setTextResponses", "no-op for video calls");
        } else if (textResponses == null) {
            LogUtil.i("FreemeAnswerFragment.setTextResponses", "no text responses, hiding secondary button");
            this.textResponses = null;
        } else if (ActivityCompat.isInMultiWindowMode(getActivity())) {
            LogUtil.i("FreemeAnswerFragment.setTextResponses", "in multiwindow, hiding secondary button");
            this.textResponses = null;
        } else {
            LogUtil.i("FreemeAnswerFragment.setTextResponses", "textResponses.size: " + textResponses.size());
            this.textResponses = new ArrayList<>(textResponses);
        }

        /// M: Add for reject video call by SMS.
        setTextResponsesForThirdButton(textResponses);
    }

    private void initSecondaryButton() {
        /**
         * M: [Video call]3G video call can't answer as voice, nor reject via SMS. @{
         */
        if (is3GVideoCall()) {
            mSmsBtn.setClickable(false);
            mSmsBtn.setEnabled(false);
            return;
        }
        /** @} */

        if (isVideoCall() && !supportsRejectVideoCallBySms()) {
            mSmsBtn.setClickable(false);
            mSmsBtn.setEnabled(false);
        }
    }

    @Override
    public boolean allowAnswerAndRelease() {
        return getArguments().getBoolean(ARG_ALLOW_ANSWER_AND_RELEASE);
    }

    private boolean hasCallOnHold() {
        return getArguments().getBoolean(ARG_HAS_CALL_ON_HOLD);
    }

    @Override
    public boolean hasPendingDialogs() {
        boolean hasPendingDialogs =
                textResponsesFragment != null || createCustomSmsDialogFragment != null;
        LogUtil.i("FreemeAnswerFragment.hasPendingDialogs", "" + hasPendingDialogs);
        return hasPendingDialogs;
    }

    @Override
    public void dismissPendingDialogs() {
        LogUtil.i("FreemeAnswerFragment.dismissPendingDialogs", null);
        if (textResponsesFragment != null) {
            textResponsesFragment.dismiss();
            textResponsesFragment = null;
        }

        if (createCustomSmsDialogFragment != null) {
            createCustomSmsDialogFragment.dismiss();
            createCustomSmsDialogFragment = null;
        }
    }

    @Override
    public boolean isShowingLocationUi() {
        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.incall_location_holder);
        return fragment != null && fragment.isVisible();
    }

    @Override
    public void showLocationUi(@Nullable Fragment locationUi) {
        boolean isShowing = isShowingLocationUi();
        if (!isShowing && locationUi != null) {
            // Show the location fragment.
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.incall_location_holder, locationUi)
                    .commitAllowingStateLoss();
        } else if (isShowing && locationUi == null) {
            // Hide the location fragment
            Fragment fragment = getChildFragmentManager().findFragmentById(R.id.incall_location_holder);
            getChildFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    @Override
    public Fragment getAnswerScreenFragment() {
        return this;
    }

    @Override
    public void setPrimary(PrimaryInfo primaryInfo) {
        LogUtil.i("FreemeAnswerFragment.setPrimary", primaryInfo.toString());
        this.primaryInfo = primaryInfo;
        updatePrimaryUI();
        updateImportanceBadgeVisibility();
    }

    private void updatePrimaryUI() {
        if (getView() == null) {
            return;
        }
        contactGridManager.setPrimary(primaryInfo);
        updateDataFragment();
    }

    private void updateDataFragment() {
        if (!isAdded()) {
            return;
        }
        LogUtil.enterBlock("FreemeAnswerFragment.updateDataFragment");
        Fragment current = getChildFragmentManager().findFragmentById(R.id.incall_data_container);
        Fragment newFragment = null;

        MultimediaData multimediaData = getSessionData();
        if (multimediaData != null
                && (!TextUtils.isEmpty(multimediaData.getText())
                || (multimediaData.getImageUri() != null)
                || (multimediaData.getLocation() != null && canShowMap()))) {
            // Need message fragment
            String subject = multimediaData.getText();
            Uri imageUri = multimediaData.getImageUri();
            Location location = multimediaData.getLocation();
            if (!(current instanceof MultimediaFragment)
                    || !Objects.equals(((MultimediaFragment) current).getSubject(), subject)
                    || !Objects.equals(((MultimediaFragment) current).getImageUri(), imageUri)
                    || !Objects.equals(((MultimediaFragment) current).getLocation(), location)) {
                LogUtil.i("FreemeAnswerFragment.updateDataFragment", "Replacing multimedia fragment");
                // Needs replacement
                newFragment =
                        MultimediaFragment.newInstance(
                                multimediaData,
                                false /* isInteractive */,
                                !primaryInfo.isSpam /* showAvatar */,
                                primaryInfo.isSpam);
            }
        } else {
            // Needs empty
            if (current != null) {
                LogUtil.i("FreemeAnswerFragment.updateDataFragment", "Removing current fragment");
                getChildFragmentManager().beginTransaction().remove(current).commitNow();
            }
        }

        if (newFragment != null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.incall_data_container, newFragment)
                    .commitNow();
        }
    }

    private boolean canShowMap() {
        return MapsComponent.get(getContext()).getMaps().isAvailable();
    }

    @Override
    public void updateAvatar(AvatarPresenter avatarContainer) {
        contactGridManager.setAvatarImageView(
                avatarContainer.getAvatarImageView(),
                avatarContainer.getAvatarSize(),
                avatarContainer.shouldShowAnonymousAvatar());
    }

    @Override
    public void setSecondary(@NonNull SecondaryInfo secondaryInfo) {
        LogUtil.i("FreemeAnswerFragment.setSecondary", secondaryInfo.toString());
        /// M: DSDA show secondary incoming call in secondary info. @{
        if (!isAdded() || mDsdaCallController == null) {
            mSavedSecondaryInfo = secondaryInfo;
            LogUtil.i("FreemeAnswerFragment.setSecondary", "UI not ready, skip it.");
            return;
        }

        /// M: ALPS03836283, Increase call performace.
        /// Do not update secondary info if there is no IncomingCall. @{
        Fragment oldBanner = getChildFragmentManager().findFragmentById(R.id.incall_dsda_banner);
        CallList calllist = InCallPresenter.getInstance().getCallList();
        if (calllist == null || (calllist.getSecondaryIncomingCall() == null && oldBanner == null)) {
            LogUtil.d("AnswerFragment.setSecondary", "Not having secondary incoming call, skip it.");
            return;
        }
        /// @}

        mSavedSecondaryInfo = null;
        mDsdaCallController.setSecondaryInfo(secondaryInfo);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (secondaryInfo.shouldShow) {
            transaction.replace(R.id.incall_dsda_banner, DsdaFragment.newInstance(secondaryInfo));
            LogUtil.i("FreemeAnswerFragment.setSecondary", "show....");
        } else {
            if (oldBanner != null) {
                transaction.remove(oldBanner);
                LogUtil.i("FreemeAnswerFragment.setSecondary", "hide....");
            }
        }
        transaction.setCustomAnimations(R.anim.abc_slide_in_top, R.anim.abc_slide_out_top);
        transaction.commitAllowingStateLoss();
        /// @}
    }

    @Override
    public void setCallState(@NonNull PrimaryCallState primaryCallState) {
        LogUtil.i("FreemeAnswerFragment.setCallState", primaryCallState.toString());
        this.primaryCallState = primaryCallState;
        contactGridManager.setCallState(primaryCallState);
        /// M: Update answer and release button. @{
        updateAnswerAndReleaseButton();
        /// @}
    }

    @Override
    public void setEndCallButtonEnabled(boolean enabled, boolean animate) {
    }

    @Override
    public void showManageConferenceCallButton(boolean visible) {
    }

    @Override
    public boolean isManageConferenceVisible() {
        return false;
    }

    @Override
    public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        contactGridManager.dispatchPopulateAccessibilityEvent(event);
        // Add prompt of how to accept/decline call with swipe gesture.
        if (AccessibilityUtil.isTouchExplorationEnabled(getContext())) {
            event
                    .getText()
                    .add(getResources().getString(R.string.a11y_incoming_call_swipe_gesture_prompt));
        }
    }

    @Override
    public void showNoteSentToast() {
    }

    @Override
    public void updateInCallScreenColors() {
    }

    @Override
    public void onInCallScreenDialpadVisibilityChange(boolean isShowing) {
    }

    @Override
    public int getAnswerAndDialpadContainerResourceId() {
        throw Assert.createUnsupportedOperationFailException();
    }

    @Override
    public Fragment getInCallScreenFragment() {
        return this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        Assert.checkState(arguments.containsKey(ARG_CALL_ID));
        Assert.checkState(arguments.containsKey(ARG_IS_VIDEO_CALL));
        Assert.checkState(arguments.containsKey(ARG_IS_VIDEO_UPGRADE_REQUEST));

        buttonAcceptClicked = false;
        buttonRejectClicked = false;

        View view = inflater.inflate(R.layout.freeme_fragment_incoming_call, container, false);
        mAnswerControlContainer = view.findViewById(R.id.freeme_answer_control_container);
        mGlowpad = view.findViewById(R.id.freeme_glow_pad_view);
        mMuteBtn = view.findViewById(R.id.freeme_mute_btn);
        mSmsBtn = view.findViewById(R.id.freeme_sms_btn);
        mAnswerControllerSwitcher = view.findViewById(R.id.freeme_incall_controller_switcher);
        mDeclineBtnUnlock = view.findViewById(R.id.freeme_decline_btn);
        mAnswerBtnUnlock = view.findViewById(R.id.freeme_answer_btn);

        PowerManager pm = (PowerManager)getContext()
                .getSystemService(Context.POWER_SERVICE);
        KeyguardManager km = (KeyguardManager)getContext()
                .getSystemService(Context.KEYGUARD_SERVICE);
        boolean lock = !pm.isScreenOn()||km.inKeyguardRestrictedInputMode();
        mAnswerControllerSwitcher.setDisplayedChild(
                lock ? LOCKED_CONTROL_VIEW : UNLOCKED_CONTROL_VIEW);
        mMuteBtn.setOnClickListener(this);
        mSmsBtn.setOnClickListener(this);
        mDeclineBtnUnlock.setOnClickListener(this);
        mAnswerBtnUnlock.setOnClickListener(this);
        mGlowpad.setControllerCallBack(this);

        affordanceHolderLayout = (AffordanceHolderLayout) view.findViewById(R.id.incoming_container);

        importanceBadge = view.findViewById(R.id.incall_important_call_badge);
        importanceBadge
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                int leftRightPadding = importanceBadge.getHeight() / 2;
                                importanceBadge.setPadding(
                                        leftRightPadding,
                                        importanceBadge.getPaddingTop(),
                                        leftRightPadding,
                                        importanceBadge.getPaddingBottom());
                                /// M: ALPS03711469 Remove the listener so we don't continually re-layout. @{
                                ViewTreeObserver observer = importanceBadge.getViewTreeObserver();
                                if (observer.isAlive()) {
                                    observer.removeOnGlobalLayoutListener(this);
                                }
                                /// @}
                            }
                        });
        updateImportanceBadgeVisibility();

        contactGridManager = new FreemeContactGridManager(view,
                view.findViewById(R.id.contactgrid_avatar),
                getResources().getDimensionPixelSize(R.dimen.incall_avatar_size),
                true);

        answerScreenDelegate =
                FragmentUtils.getParentUnsafe(this, AnswerScreenDelegateFactory.class)
                        .newAnswerScreenDelegate(this);

        initSecondaryButton();

        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (!ActivityCompat.isInMultiWindowMode(getActivity())
                && (getActivity().checkSelfPermission(Manifest.permission.STATUS_BAR)
                == PackageManager.PERMISSION_GRANTED)) {
            LogUtil.i("FreemeAnswerFragment.onCreateView", "STATUS_BAR permission granted, disabling nav bar");
            // These flags will suppress the alert that the activity is in full view mode
            // during an incoming call on a fresh system/factory reset of the app
            flags |= STATUS_BAR_DISABLE_BACK | STATUS_BAR_DISABLE_HOME | STATUS_BAR_DISABLE_RECENT;

            ///M: ALPS03563079 refresh bounce animation again when go back to answerfragment.
            // Disable fitsSystemWindows property as well. @{
            View frameView = view.findViewById(R.id.incoming_frame_layout);
            if (frameView != null) {
                frameView.setFitsSystemWindows(false);
            }
            /// @}
        }
        view.setSystemUiVisibility(flags);
        if (isVideoCall() || isVideoUpgradeRequest()) {
            if (VideoUtils.hasCameraPermissionAndShownPrivacyToast(getContext())) {
                if (isSelfManagedCamera()) {
                    answerVideoCallScreen = new SelfManagedAnswerVideoCallScreen(getCallId(), this, view);
                } else {
                    answerVideoCallScreen = new AnswerVideoCallScreen(getCallId(), this, view);
                }
            } else {
                view.findViewById(R.id.videocall_video_off).setVisibility(View.VISIBLE);
            }
        }

        /// M: DSDA. show secondary call info. @{
        mDsdaContainer = view.findViewById(R.id.incall_dsda_banner);
        mDsdaContainer.setClickable(true);
        /// @}
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        FragmentUtils.checkParent(this, InCallScreenDelegateFactory.class);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        createInCallScreenDelegate();
        updateUI();

        if (savedInstanceState == null || !savedInstanceState.getBoolean(STATE_HAS_ANIMATED_ENTRY)) {
            ViewUtil.doOnGlobalLayout(view, this::animateEntry);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.i("FreemeAnswerFragment.onResume", null);
        inCallScreenDelegate.onInCallScreenResumed();
        /// M: DSDA. @{
        if (mSavedSecondaryInfo != null) {
            setSecondary(mSavedSecondaryInfo);
        }
        /// @}

        ///M: ALPS03563079 refresh bounce animation again when go back to answerfragment. @{
        refreshBounceAnimation();
        /// @}
        if (!buttonAcceptClicked && !buttonRejectClicked && mGlowpad != null) {
            mGlowpad.reset();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtil.i("FreemeAnswerFragment.onStart", null);

        updateUI();
        if (answerVideoCallScreen != null) {
            answerVideoCallScreen.onVideoScreenStart();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.i("FreemeAnswerFragment.onStop", null);

        if (answerVideoCallScreen != null) {
            answerVideoCallScreen.onVideoScreenStop();
        }

        ///M: ALPS03563079 refresh bounce animation again when go back to answerfragment. @{
        mRefreshAnimation = true;
        /// @}
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtil.i("FreemeAnswerFragment.onPause", null);
        inCallScreenDelegate.onInCallScreenPaused();
    }

    @Override
    public void onDestroyView() {
        LogUtil.i("FreemeAnswerFragment.onDestroyView", null);
        if (mGlowpad != null) {
            mGlowpad.stopAnimation();
            mGlowpad = null;
        }
        if (answerVideoCallScreen != null) {
            answerVideoCallScreen = null;
        }
        super.onDestroyView();
        inCallScreenDelegate.onInCallScreenUnready();
        answerScreenDelegate.onAnswerScreenUnready();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(STATE_HAS_ANIMATED_ENTRY, hasAnimatedEntry);
    }

    private void updateUI() {
        if (getView() == null) {
            return;
        }

        if (primaryInfo != null) {
            updatePrimaryUI();
        }
        if (primaryCallState != null) {
            contactGridManager.setCallState(primaryCallState);
        }

        restoreBackgroundMaskColor();
    }

    @Override
    public boolean isVideoCall() {
        return getArguments().getBoolean(ARG_IS_VIDEO_CALL);
    }

    public boolean isSelfManagedCamera() {
        return getArguments().getBoolean(ARG_IS_SELF_MANAGED_CAMERA);
    }

    @Override
    public void onAnswerProgressUpdate(@FloatRange(from = -1f, to = 1f) float answerProgress) {
        // Don't fade the window background for call waiting or video upgrades. Fading the background
        // shows the system wallpaper which looks bad because on reject we switch to another call.
        if (primaryCallState.state == DialerCall.State.INCOMING && !isVideoCall()) {
            answerScreenDelegate.updateWindowBackgroundColor(answerProgress);
        }

        // Fade and scale contact name and video call text
        float startDelay = .25f;
        // Header progress is zero over positiveAdjustedProgress = [0, startDelay],
        // linearly increases over (startDelay, 1] until reaching 1 when positiveAdjustedProgress = 1
        float headerProgress = Math.max(0, (Math.abs(answerProgress) - 1) / (1 - startDelay) + 1);
        fadeToward(contactGridManager.getContainerView(), 1 - headerProgress);
        scaleToward(contactGridManager.getContainerView(), MathUtil.lerp(1f, .75f, headerProgress));

        if (Math.abs(answerProgress) >= .0001) {
            affordanceHolderLayout.animateHideLeftRightIcon();
        }
    }

    @Override
    public void answerFromMethod() {
        acceptCallByUser(false /* answerVideoAsAudio */);
    }

    @Override
    public void rejectFromMethod() {
        rejectCall();
    }

    @Override
    public void resetAnswerProgress() {
        affordanceHolderLayout.reset(true);
        restoreBackgroundMaskColor();
    }

    private void animateEntry(@NonNull View rootView) {
        if (!isAdded()) {
            LogUtil.i(
                    "FreemeAnswerFragment.animateEntry",
                    "Not currently added to Activity. Will not start entry animation.");
            return;
        }
        contactGridManager.getContainerView().setAlpha(0f);
        Animator alpha =
                ObjectAnimator.ofFloat(contactGridManager.getContainerView(), View.ALPHA, 0, 1);
        Animator topRow = createTranslation(rootView.findViewById(R.id.contactgrid_top_row));
        Animator contactName = createTranslation(rootView.findViewById(R.id.contactgrid_contact_name));
        Animator bottomRow = createTranslation(rootView.findViewById(R.id.contactgrid_bottom_row));
        Animator important = createTranslation(importanceBadge);
        Animator dataContainer = createTranslation(rootView.findViewById(R.id.incall_data_container));

        AnimatorSet animatorSet = new AnimatorSet();
        AnimatorSet.Builder builder = animatorSet.play(alpha);
        builder.with(topRow).with(contactName).with(bottomRow).with(important).with(dataContainer);
        if (isShowingLocationUi()) {
            builder.with(createTranslation(rootView.findViewById(R.id.incall_location_holder)));
        }
        animatorSet.setDuration(
                rootView.getResources().getInteger(R.integer.answer_animate_entry_millis));
        animatorSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hasAnimatedEntry = true;
                    }
                });
        animatorSet.start();
    }

    private ObjectAnimator createTranslation(View view) {
        float translationY = view.getTop() * 0.5f;
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, translationY, 0);
        animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        return animator;
    }

    private void acceptCallByUser(boolean answerVideoAsAudio) {
        LogUtil.i("FreemeAnswerFragment.acceptCallByUser", answerVideoAsAudio ? " answerVideoAsAudio" : "");
        if (!buttonAcceptClicked) {
            answerScreenDelegate.onAnswer(answerVideoAsAudio);
            buttonAcceptClicked = true;
        }
    }

    private void rejectCall() {
        LogUtil.i("FreemeAnswerFragment.rejectCall", null);
        if (!buttonRejectClicked) {
            Context context = getContext();
            if (context == null) {
                LogUtil.w(
                        "FreemeAnswerFragment.rejectCall",
                        "Null context when rejecting call. Logger call was skipped");
            } else {
                Logger.get(context)
                        .logImpression(DialerImpression.Type.REJECT_INCOMING_CALL_FROM_ANSWER_SCREEN);
            }
            buttonRejectClicked = true;
            answerScreenDelegate.onReject();
        }
    }

    private void restoreBackgroundMaskColor() {
        answerScreenDelegate.updateWindowBackgroundColor(0);
    }

    private void showMessageMenu() {
        LogUtil.i("FreemeAnswerFragment.showMessageMenu", "Show sms menu.");
        /// M: check Host exsit or not before show pop menu @{
        if (!isAdded() || getChildFragmentManager().isDestroyed()) {
            LogUtil.w(
                    "FreemeAnswerFragment.showMessageMenu",
                    "Host is not exsit when show sms menu. skipped");
            return;
        }
        /// @}

        textResponsesFragment = SmsBottomSheetFragment.newInstance(textResponses);
        textResponsesFragment.show(getChildFragmentManager(), null);

        mAnswerControlContainer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void smsSelected(@Nullable CharSequence text) {
        LogUtil.i("FreemeAnswerFragment.smsSelected", null);
        textResponsesFragment = null;

        if (text == null) {
            createCustomSmsDialogFragment = CreateCustomSmsDialogFragment.newInstance();
            createCustomSmsDialogFragment.show(getChildFragmentManager(), null);
            return;
        }

        if (primaryCallState != null && canRejectCallWithSms()) {
            rejectCall();
            answerScreenDelegate.onRejectCallWithMessage(text.toString());
        }
    }

    @Override
    public void smsDismissed() {
        LogUtil.i("FreemeAnswerFragment.smsDismissed", null);
        textResponsesFragment = null;
        answerScreenDelegate.onDismissDialog();

        mAnswerControlContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void customSmsCreated(@NonNull CharSequence text) {
        LogUtil.i("FreemeAnswerFragment.customSmsCreated", null);
        createCustomSmsDialogFragment = null;
        if (primaryCallState != null && canRejectCallWithSms()) {
            rejectCall();
            answerScreenDelegate.onRejectCallWithMessage(text.toString());
        }
    }

    @Override
    public void customSmsDismissed() {
        LogUtil.i("FreemeAnswerFragment.customSmsDismissed", null);
        createCustomSmsDialogFragment = null;
        answerScreenDelegate.onDismissDialog();
    }

    private boolean canRejectCallWithSms() {
        return primaryCallState != null
                && !(primaryCallState.state == DialerCall.State.DISCONNECTED
                || primaryCallState.state == DialerCall.State.DISCONNECTING
                || primaryCallState.state == DialerCall.State.IDLE);
    }

    private void createInCallScreenDelegate() {
        inCallScreenDelegate =
                FragmentUtils.getParentUnsafe(this, InCallScreenDelegateFactory.class)
                        .newInCallScreenDelegate();
        Assert.isNotNull(inCallScreenDelegate);
        inCallScreenDelegate.onInCallScreenDelegateInit(this);
        inCallScreenDelegate.onInCallScreenReady();

        /// M: Support DSDA.
        mDsdaCallController =
                new DsdaCallController(mDsdaContainer, inCallScreenDelegate);
    }

    private void updateImportanceBadgeVisibility() {
        if (!isAdded() || getView() == null) {
            return;
        }

        if (!getResources().getBoolean(R.bool.answer_important_call_allowed) || primaryInfo.isSpam) {
            importanceBadge.setVisibility(View.GONE);
            return;
        }

        MultimediaData multimediaData = getSessionData();
        boolean showImportant = multimediaData != null && multimediaData.isImportant();
        TransitionManager.beginDelayedTransition((ViewGroup) importanceBadge.getParent());
        // TODO (keyboardr): Change this back to being View.INVISIBLE once mocks are available to
        // properly handle smaller screens
        importanceBadge.setVisibility(showImportant ? View.VISIBLE : View.GONE);
    }

    @Nullable
    private MultimediaData getSessionData() {
        if (primaryInfo == null) {
            return null;
        }
        if (isVideoUpgradeRequest()) {
            return null;
        }
        return primaryInfo.multimediaData;
    }

    /**
     * Shows the Avatar image if available.
     */
    public static class AvatarFragment extends Fragment implements AvatarPresenter {

        private ImageView avatarImageView;

        @Nullable
        @Override
        public View onCreateView(
                LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
            return layoutInflater.inflate(R.layout.fragment_avatar, viewGroup, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle bundle) {
            super.onViewCreated(view, bundle);
            avatarImageView = ((ImageView) view.findViewById(R.id.contactgrid_avatar));
            FragmentUtils.getParentUnsafe(this, MultimediaFragment.Holder.class).updateAvatar(this);
        }

        @NonNull
        @Override
        public ImageView getAvatarImageView() {
            return avatarImageView;
        }

        @Override
        public int getAvatarSize() {
            return getResources().getDimensionPixelSize(R.dimen.answer_avatar_size);
        }

        @Override
        public boolean shouldShowAnonymousAvatar() {
            return false;
        }
    }

    /// M: ------------------------------- MediaTek feature ---------------------------
    private SecondaryInfo mSavedSecondaryInfo;
    private View mDsdaContainer;
    private DsdaCallController mDsdaCallController;

    ///M: ALPS03563079 refresh bounce animation again when go back to answerfragment. @{
    private boolean mRefreshAnimation = false;
    /// @}

    /// Add for 3G video call.
    private boolean supportsAnswerAsVoice() {
        String callId = getArguments().getString(ARG_CALL_ID);
        CallList calllist = InCallPresenter.getInstance().getCallList();
        if (TextUtils.isEmpty(callId) || calllist == null) {
            /// default true
            return true;
        }
        DialerCall call = calllist.getCallById(callId);
        if (call == null) {
            return true;
        }
        return call.getVideoFeatures().supportsAnswerAsVoice();
    }

    private boolean is3GVideoCall() {
        return isVideoCall() && !supportsAnswerAsVoice();
    }

    /// Add for reject video call by SMS.
    private boolean supportsRejectVideoCallBySms() {
        String callId = getArguments().getString(ARG_CALL_ID);
        CallList calllist = InCallPresenter.getInstance().getCallList();
        if (TextUtils.isEmpty(callId) || calllist == null) {
            /// default false
            LogUtil.w("supportsRejectVideoCallBySms", "no callId or calllist");
            return false;
        }
        DialerCall call = calllist.getCallById(callId);
        if (call == null) {
            LogUtil.w("supportsRejectVideoCallBySms", "no DialerCall");
            return false;
        }
        return call.getVideoFeatures().supportsRejectVideoCallBySms();
    }

    private void setTextResponsesForThirdButton(List<String> textResponses) {
        if (isVideoCall() && supportsRejectVideoCallBySms()) {
            if (textResponses == null) {
                LogUtil.i("setTextResponses", "no text responses, hiding third button");
                this.textResponses = null;
            } else if (ActivityCompat.isInMultiWindowMode(getActivity())) {
                LogUtil.i("setTextResponses", "in multiwindow, hiding third button");
                this.textResponses = null;
            } else {
                LogUtil.i("setTextResponses", "textResponses.size: " + textResponses.size());
                this.textResponses = new ArrayList<>(textResponses);
            }
        }
    }

    /// M: Update answer and release button. @{
    private void updateAnswerAndReleaseButton() {
        if (allowAnswerAndRelease() && CallList.getInstance().getActiveCall() == null) {
            LogUtil.i("FreemeAnswerFragment.updateAnswerAndReleaseButton", "hide....");
            getArguments().putBoolean(ARG_ALLOW_ANSWER_AND_RELEASE, false);
        } else if (hasCallOnHold() && CallList.getInstance().getBackgroundCall() == null) {
            LogUtil.i("FreemeAnswerFragment.updateAnswerAndReleaseButton", "no hold call");
            getArguments().putBoolean(ARG_HAS_CALL_ON_HOLD, false);
        }
    }
    /// @}

    /**
     * M: show timer for video upgrade request.
     */
    @Override
    public void updateDeclineTimer() {
        contactGridManager.updateDeclineTimer();
    }

    ///M: ALPS03563079 refresh bounce animation again when go back to answerfragment. @{
    private void refreshBounceAnimation() {
        if (!mRefreshAnimation) {
            return;
        }

        mRefreshAnimation = false;
    }
    /// @}

    private final static int LOCKED_CONTROL_VIEW = 0;
    private final static int UNLOCKED_CONTROL_VIEW = 1;
    private FreemeCustomAnswerView mGlowpad;
    private TextView mMuteBtn;
    private TextView mSmsBtn;
    private ViewAnimator mAnswerControllerSwitcher;
    private ImageView mDeclineBtnUnlock;
    private ImageView mAnswerBtnUnlock;
    private FreemeAnswerBootSheetFragment mAnswerCallFragment;
    private View mAnswerControlContainer;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.freeme_mute_btn:
                TelecomManager tm = (TelecomManager)
                        getActivity().getSystemService(Context.TELECOM_SERVICE);
                tm.silenceRinger();
                mMuteBtn.setSelected(true);
                break;
            case R.id.freeme_sms_btn:
                showMessageMenu();
                break;
            case R.id.freeme_decline_btn:
                rejectCall();
                break;
            case R.id.freeme_answer_btn:
                answerCall();
                break;
            default:
                break;
        }
    }

    private void answerCall() {
        boolean isSupportDownGradeAudio = false;
        boolean isSupportAnswerAndRelease = allowAnswerAndRelease();
        if (isVideoCall()) {
            isSupportDownGradeAudio = !isVideoUpgradeRequest();
            if (supportsRejectVideoCallBySms()) {
                isSupportAnswerAndRelease = false;
            }
        }

        if (isSupportDownGradeAudio || isSupportAnswerAndRelease) {
            mAnswerCallFragment = FreemeAnswerBootSheetFragment.newInstance(
                    isSupportDownGradeAudio, isSupportAnswerAndRelease);
            mAnswerCallFragment.show(getChildFragmentManager(), null);

            mAnswerControlContainer.setVisibility(View.INVISIBLE);
        } else {
            answerFromMethod();
        }
    }

    @Override
    public void onAnswerMenuDismissed() {
        mAnswerControlContainer.setVisibility(View.VISIBLE);
        if (!buttonAcceptClicked && !buttonRejectClicked && mGlowpad != null) {
            mGlowpad.reset();
        }
    }

    @Override
    public void onAnswer() {
        answerCall();
    }

    @Override
    public void onDecline() {
        rejectCall();
    }
}
