/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.incallui;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.app.Fragment;
import android.support.v4.os.UserManagerCompat;
import android.telecom.CallAudioState;
import android.telecom.VideoProfile;

import com.android.contacts.common.compat.CallCompat;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.incallui.InCallCameraManager.Listener;
import com.android.incallui.InCallPresenter.CanAddCallListener;
import com.android.incallui.InCallPresenter.InCallDetailsListener;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.InCallPresenter.IncomingCallListener;
import com.android.incallui.InCallPresenter.SessionStateListener;
import com.android.incallui.audiomode.AudioModeProvider;
import com.android.incallui.audiomode.AudioModeProvider.AudioModeListener;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.DialerCall.CameraDirection;
import com.android.incallui.call.TelecomAdapter;
import com.android.incallui.incall.protocol.InCallButtonIds;
import com.android.incallui.incall.protocol.InCallButtonUi;
import com.android.incallui.incall.protocol.InCallButtonUiDelegate;
import com.android.incallui.videotech.utils.SessionModificationState;
import com.android.incallui.videotech.utils.VideoUtils;

import com.mediatek.incallui.blindect.AddTransferNumberScreenController;
import com.mediatek.incallui.compat.InCallUiCompat;
import com.mediatek.incallui.plugin.ExtensionManager;
import com.mediatek.incallui.utils.InCallUtils;
import com.android.incallui.videotech.VideoTech;

import mediatek.telecom.MtkCall;

//*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
import com.android.incallui.incall.impl.InCallFragment;
import com.android.incallui.incall.protocol.InCallScreen;
import com.android.incallui.incall.impl.ButtonController;
//*/

/** Logic for call buttons. */
public class CallButtonPresenter
    implements InCallStateListener,
        AudioModeListener,
        IncomingCallListener,
        InCallDetailsListener,
        CanAddCallListener,
        Listener,
        InCallButtonUiDelegate,
        /// M: Update session state to refresh UI. @{
        SessionStateListener {
        /// @}

  private static final String KEY_AUTOMATICALLY_MUTED = "incall_key_automatically_muted";
  private static final String KEY_PREVIOUS_MUTE_STATE = "incall_key_previous_mute_state";

  private final Context mContext;
  private InCallButtonUi mInCallButtonUi;
  private DialerCall mCall;
  private boolean mAutomaticallyMuted = false;
  private boolean mPreviousMuteState = false;
  private boolean isInCallButtonUiReady;

  public CallButtonPresenter(Context context) {
    LogUtil.d("CallButtonPresenter.CallButtonPresenter", "mPreMuteState:" + mPreviousMuteState);
    mContext = context.getApplicationContext();
  }

  @Override
  public void onInCallButtonUiReady(InCallButtonUi ui) {
    Assert.checkState(!isInCallButtonUiReady);
    mInCallButtonUi = ui;
    AudioModeProvider.getInstance().addListener(this);

    // register for call state changes last
    final InCallPresenter inCallPresenter = InCallPresenter.getInstance();
    inCallPresenter.addListener(this);
    inCallPresenter.addIncomingCallListener(this);
    /// M: improve MO/MT performance @{
    //inCallPresenter.addDetailsListener(this);
    /// @}
    inCallPresenter.addCanAddCallListener(this);
    inCallPresenter.getInCallCameraManager().addCameraSelectionListener(this);
    /// M: Update session state to refresh UI. @{
    inCallPresenter.addSessionListener(this);
    /// @}

    // Update the buttons state immediately for the current call
    onStateChange(InCallState.NO_CALLS, inCallPresenter.getInCallState(), CallList.getInstance());
    isInCallButtonUiReady = true;
  }

  @Override
  public void onInCallButtonUiUnready() {
    Assert.checkState(isInCallButtonUiReady);
    mInCallButtonUi = null;
    InCallPresenter.getInstance().removeListener(this);
    AudioModeProvider.getInstance().removeListener(this);
    InCallPresenter.getInstance().removeIncomingCallListener(this);
    InCallPresenter.getInstance().removeDetailsListener(this);
    InCallPresenter.getInstance().getInCallCameraManager().removeCameraSelectionListener(this);
    InCallPresenter.getInstance().removeCanAddCallListener(this);
    /// M: Update session state to refresh UI. @{
    InCallPresenter.getInstance().removeSessionListener(this);
    /// @}
    isInCallButtonUiReady = false;
  }

  @Override
  public void onStateChange(InCallState oldState, InCallState newState, CallList callList) {
    if (newState == InCallState.OUTGOING) {
      mCall = callList.getOutgoingCall();
    } else if (newState == InCallState.INCALL) {
      mCall = callList.getActiveOrBackgroundCall();

      // When connected to voice mail, automatically shows the dialpad.
      // (On previous releases we showed it when in-call shows up, before waiting for
      // OUTGOING.  We may want to do that once we start showing "Voice mail" label on
      // the dialpad too.)
      if (oldState == InCallState.OUTGOING && mCall != null) {
        /// M: ALPS03567937. Using telecom API to check if is voice mail will cost 30ms once.
        // Using api isVoiceMailNumber() in dialercall. @{
        // Google code:
        // if (CallerInfoUtils.isVoiceMailNumber(mContext, mCall) && getActivity() != null) {
        if (mCall.isVoiceMailNumber() && getActivity() != null) {
        /// @}
          getActivity().showDialpadFragment(true /* show */, true /* animate */);
        }
      }
    } else if (newState == InCallState.INCOMING) {
      if (getActivity() != null) {
        getActivity().showDialpadFragment(false /* show */, true /* animate */);
      }
      mCall = callList.getIncomingCall();
    } else {
      mCall = null;
    }
    updateUi(newState, mCall);
  }

  /**
   * Updates the user interface in response to a change in the details of a call. Currently handles
   * changes to the call buttons in response to a change in the details for a call. This is
   * important to ensure changes to the active call are reflected in the available buttons.
   *
   * @param call The active call.
   * @param details The call details.
   */
  @Override
  public void onDetailsChanged(DialerCall call, android.telecom.Call.Details details) {
    // Only update if the changes are for the currently active call
    if (mInCallButtonUi != null && call != null && call.equals(mCall)) {
      updateButtonsState(call);
    }
  }

  @Override
  public void onIncomingCall(InCallState oldState, InCallState newState, DialerCall call) {
    onStateChange(oldState, newState, CallList.getInstance());
  }

  @Override
  public void onCanAddCallChanged(boolean canAddCall) {
    if (mInCallButtonUi != null && mCall != null) {
      updateButtonsState(mCall);
    }
  }

  @Override
  public void onAudioStateChanged(CallAudioState audioState) {
    if (mInCallButtonUi != null) {
      mInCallButtonUi.setAudioState(audioState);
    }
  }

  @Override
  public CallAudioState getCurrentAudioState() {
    return AudioModeProvider.getInstance().getAudioState();
  }

  @Override
  public void setAudioRoute(int route) {
    LogUtil.i(
        "CallButtonPresenter.setAudioRoute",
        "sending new audio route: " + CallAudioState.audioRouteToString(route));
    TelecomAdapter.getInstance().setAudioRoute(route);
  }

  /** Function assumes that bluetooth is not supported. */
  @Override
  public void toggleSpeakerphone() {
    // This function should not be called if bluetooth is available.
    CallAudioState audioState = getCurrentAudioState();
    if (0 != (CallAudioState.ROUTE_BLUETOOTH & audioState.getSupportedRouteMask())) {
      // It's clear the UI is wrong, so update the supported mode once again.
      LogUtil.e(
          "CallButtonPresenter", "toggling speakerphone not allowed when bluetooth supported.");
      mInCallButtonUi.setAudioState(audioState);
      return;
    }

    /// M: ALPS03664609 Fixed no pointer exception. @{
    if (mCall == null) {
      LogUtil.e("CallButtonPresenter.toggleSpeakerphone", "Call is null");
      return;
    }
    /// @}

    int newRoute;
    if (audioState.getRoute() == CallAudioState.ROUTE_SPEAKER) {
      newRoute = CallAudioState.ROUTE_WIRED_OR_EARPIECE;
      Logger.get(mContext)
          .logCallImpression(
              DialerImpression.Type.IN_CALL_SCREEN_TURN_ON_WIRED_OR_EARPIECE,
              mCall.getUniqueCallId(),
              mCall.getTimeAddedMs());
    } else {
      newRoute = CallAudioState.ROUTE_SPEAKER;
      Logger.get(mContext)
          .logCallImpression(
              DialerImpression.Type.IN_CALL_SCREEN_TURN_ON_SPEAKERPHONE,
              mCall.getUniqueCallId(),
              mCall.getTimeAddedMs());
    }

    setAudioRoute(newRoute);
  }

  @Override
  public void muteClicked(boolean checked, boolean clickedByUser) {
    LogUtil.i(
        "CallButtonPresenter", "turning on mute: %s, clicked by user: %s", checked, clickedByUser);
    if (clickedByUser) {
      Logger.get(mContext)
          .logCallImpression(
              checked
                  ? DialerImpression.Type.IN_CALL_SCREEN_TURN_ON_MUTE
                  : DialerImpression.Type.IN_CALL_SCREEN_TURN_OFF_MUTE,
              mCall.getUniqueCallId(),
              mCall.getTimeAddedMs());
    }
    TelecomAdapter.getInstance().mute(checked);
  }

  @Override
  public void holdClicked(boolean checked) {
    if (mCall == null) {
      return;
    }
    if (checked) {
      LogUtil.i("CallButtonPresenter", "putting the call on hold: " + mCall);
      mCall.hold();
    } else {
      LogUtil.i("CallButtonPresenter", "removing the call from hold: " + mCall);
      mCall.unhold();
    }
  }

  @Override
  public void swapClicked() {
    if (mCall == null) {
      return;
    }

    LogUtil.i("CallButtonPresenter", "swapping the call: " + mCall);
    TelecomAdapter.getInstance().swap(mCall.getId());
  }

  @Override
  public void mergeClicked() {
    TelecomAdapter.getInstance().merge(mCall.getId());
  }

  @Override
  public void addCallClicked() {
    /*/ freeme.zhaozehong, 20180317. fix#0027034, remove automatically mute
    // Automatically mute the current call
    mAutomaticallyMuted = true;
    mPreviousMuteState = AudioModeProvider.getInstance().getAudioState().isMuted();
    // Simulate a click on the mute button
    muteClicked(true /* checked * /, false /* clickedByUser * /);
    //*/
    TelecomAdapter.getInstance().addCall();
  }

  @Override
  public void showDialpadClicked(boolean checked) {
    LogUtil.v("CallButtonPresenter", "show dialpad " + String.valueOf(checked));
    getActivity().showDialpadFragment(checked /* show */, true /* animate */);
  }

  @Override
  public void changeToVideoClicked() {
    LogUtil.enterBlock("CallButtonPresenter.changeToVideoClicked");
    Logger.get(mContext)
        .logCallImpression(
            DialerImpression.Type.VIDEO_CALL_UPGRADE_REQUESTED,
            mCall.getUniqueCallId(),
            mCall.getTimeAddedMs());
    /// M: check if Video call over WIFI is allowed or not. @{
    if (mCall.hasProperty(android.telecom.Call.Details.PROPERTY_WIFI) &&
        mCall.getVideoFeatures().disableVideoCallOverWifi()) {
      InCallPresenter.getInstance().showMessage(
          com.android.incallui.R.string.video_over_wifi_not_available);
      return;
    }
    /// @}
    mCall.getVideoTech().upgradeToVideo();
  }

  @Override
  public void onEndCallClicked() {
    LogUtil.i("CallButtonPresenter.onEndCallClicked", "call: " + mCall);
    if (mCall != null) {
      mCall.disconnect();
    }
  }

  @Override
  public void showAudioRouteSelector() {
    if (!getActivity().isResumed()) {
      LogUtil.i("CallButtonPresenter.showAudioRouteSelector", "Activity is not resume");
      return;
    }
    mInCallButtonUi.showAudioRouteSelector();
  }

  /**
   * Switches the camera between the front-facing and back-facing camera.
   *
   * @param useFrontFacingCamera True if we should switch to using the front-facing camera, or false
   *     if we should switch to using the back-facing camera.
   */
  @Override
  public void switchCameraClicked(boolean useFrontFacingCamera) {
    updateCamera(useFrontFacingCamera);
  }

  @Override
  public void toggleCameraClicked() {
    LogUtil.i("CallButtonPresenter.toggleCameraClicked", "");
    if (mCall == null) {
      return;
    }
    Logger.get(mContext)
        .logCallImpression(
            DialerImpression.Type.IN_CALL_SCREEN_SWAP_CAMERA,
            mCall.getUniqueCallId(),
            mCall.getTimeAddedMs());
    switchCameraClicked(
        !InCallPresenter.getInstance().getInCallCameraManager().isUsingFrontFacingCamera());
  }

  /**
   * Stop or start client's video transmission.
   *
   * @param pause True if pausing the local user's video, or false if starting the local user's
   *     video.
   */
  @Override
  public void pauseVideoClicked(boolean pause) {
    LogUtil.i("CallButtonPresenter.pauseVideoClicked", "%s", pause ? "pause" : "unpause");

    Logger.get(mContext)
        .logCallImpression(
            pause
                ? DialerImpression.Type.IN_CALL_SCREEN_TURN_OFF_VIDEO
                : DialerImpression.Type.IN_CALL_SCREEN_TURN_ON_VIDEO,
            mCall.getUniqueCallId(),
            mCall.getTimeAddedMs());
    /// M: pause the video without sending sip message feature check @{
    if (pauseVideoWithoutSipMessage(pause)) {
      // video is paused by making camera as null so no need to do anything else
      return;
    }
    /// @}
    if (pause) {
      /// M: ALPS03593227 can't set camera again after pause request fail. We will remove set camera
      /// is null when send pause video request. And videocallpresenter will set correct value to
      ///vtservice accroding to video state. @{
      /// google original code:
      //mCall.getVideoTech().setCamera(null);
      ///@}
      mCall.getVideoTech().stopTransmission();
    } else {
      /// M: ALPS03593227 can't set camera again after pause request fail. We will remove set camera
      /// is null when send pause video request. And videocallpresenter will set correct value to
      ///vtservice accroding to video state. @{
      /// google original code:
      /*updateCamera(
          InCallPresenter.getInstance().getInCallCameraManager().isUsingFrontFacingCamera());*/
      ///@}
      mCall.getVideoTech().resumeTransmission();
    }

    mInCallButtonUi.setVideoPaused(pause);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_PAUSE_VIDEO, false);
  }

  private void updateCamera(boolean useFrontFacingCamera) {
    /// M: ALPS03462464 fixed no pointer exception @{
    if (mCall == null) {
      LogUtil.w("CallButtonPresenter.updateCamera", "mCall is already null");
      return;
    }
    /// @}
    InCallCameraManager cameraManager = InCallPresenter.getInstance().getInCallCameraManager();
    cameraManager.setUseFrontFacingCamera(useFrontFacingCamera);

    /// M: if video paused by turning of camera, then dont enable camera @{
    if ((mCall != null) && (mCall.getVideoPauseState() == true)) {
      LogUtil.i("CallButtonPresenter.updateCamera", " Video is paused by making camera null");
      return;
    }
    /// @}

    String cameraId = cameraManager.getActiveCameraId();
    if (cameraId != null) {
      final int cameraDir =
          cameraManager.isUsingFrontFacingCamera()
              ? CameraDirection.CAMERA_DIRECTION_FRONT_FACING
              : CameraDirection.CAMERA_DIRECTION_BACK_FACING;
      mCall.setCameraDir(cameraDir);
      mCall.getVideoTech().setCamera(cameraId);
    }
  }

  private void updateUi(InCallState state, DialerCall call) {
    LogUtil.v("CallButtonPresenter", "updating call UI for call: " + call);

    if (mInCallButtonUi == null) {
      return;
    }

    if (call != null) {
      mInCallButtonUi.updateInCallButtonUiColors();
    }

    final boolean isEnabled =
        state.isConnectingOrConnected() && !state.isIncoming() && call != null;
    mInCallButtonUi.setEnabled(isEnabled);

    if (call == null) {
      /// M: Force to show buttons to increase performance. @{
      updateButtonStateEx();
      /// @}
      return;
    }

    updateButtonsState(call);
    /// M: [DM Lock] @{
    if (InCallUtils.isDMLocked()) {
        updateInCallControlsDuringDMLocked(call);
    }
    /// @}
  }

  /**
   * Updates the buttons applicable for the UI.
   *
   * @param call The active call.
   */
  private void updateButtonsState(DialerCall call) {
    LogUtil.v("CallButtonPresenter.updateButtonsState", "");
    final boolean isVideo = call.isVideoCall();

    // Common functionality (audio, hold, etc).
    // Show either HOLD or SWAP, but not both. If neither HOLD or SWAP is available:
    //     (1) If the device normally can hold, show HOLD in a disabled state.
    //     (2) If the device doesn't have the concept of hold/swap, remove the button.
    boolean showSwap = call.can(android.telecom.Call.Details.CAPABILITY_SWAP_CONFERENCE);

    /// M: Fix ALPS03419203 hold shall has even if it is cdma call.
    /// boolean showHold =
    ///    !showSwap
    ///        && call.can(android.telecom.Call.Details.CAPABILITY_SUPPORT_HOLD)
    ///        && call.can(android.telecom.Call.Details.CAPABILITY_HOLD);
    boolean showHold =
            call.can(android.telecom.Call.Details.CAPABILITY_SUPPORT_HOLD)
            && call.can(android.telecom.Call.Details.CAPABILITY_HOLD);
    /// @}

    final boolean isCallOnHold = call.getState() == DialerCall.State.ONHOLD;

    boolean showAddCall =
        TelecomAdapter.getInstance().canAddCall() && UserManagerCompat.isUserUnlocked(mContext);
    final boolean showMerge = call.can(android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE);

    /// M: Should check canUpgradeToVideoCall. Because in CMCC NW, can not start
    // upgrade when have 1A1H calls. @{
    boolean showUpgradeToVideo = !isVideo
                                 && hasVideoCallCapabilities(call)
                                 && call.getVideoFeatures().canUpgradeToVideoCall();
    /// @}

    boolean showDowngradeToAudio = isVideo && isDowngradeToAudioSupported(call);
    final boolean showMute = call.can(android.telecom.Call.Details.CAPABILITY_MUTE);

    final boolean hasCameraPermission =
        isVideo && VideoUtils.hasCameraPermissionAndShownPrivacyToast(mContext);
    // Disabling local video doesn't seem to work when dialing. See b/30256571.
    boolean showPauseVideo =
        isVideo
            && call.getState() != DialerCall.State.DIALING
            && call.getState() != DialerCall.State.CONNECTING;
    /// M: Handle some special case, e.g. 3G Video not support hold, downgrade. @{
    boolean showDialpad = false;
    if (isVideo) {
      showHold &= call.getVideoFeatures().supportsHold();
      showDowngradeToAudio &= call.getVideoFeatures().supportsDowngrade();
      showPauseVideo &= call.getVideoFeatures().supportsPauseVideo();
      /// For video call, only show some special OP.
      showDialpad = call.getVideoFeatures().supportShowVideoDialpad();
    } else {
      showDialpad = true;
    }
    /// @}

    /// M: hide some video call buttons when dialing/incoming or held @{
    boolean currentHeldState = call.isRemotelyHeld();
    boolean isCallActive = call.getState() == DialerCall.State.ACTIVE;
    if (currentHeldState || !isCallActive) {
      showDowngradeToAudio = false;
      showUpgradeToVideo = false;
      showPauseVideo = false;
      // showHideLocalVideoBtn in updateExtButtonUI(call)
      // hide BUTTON_SWITCH_CAMERA below
      LogUtil.v("CallButtonPresenter.updateButtonsState", "call isHeld:" + currentHeldState);
    }
    /// @}

    /*
     * M: Porting N's criteria.
     * add canEnableVideoBtn flag to control showing hold button is avoid this case:
     * when there was a volte call, we can do some video action, during this action,
     * we can't show hold button. by another way if there was a voice call, it's
     * SessionModificationState always is no_request, so meet the requestment. @{
     */
    if (isSesssionProgressCall(call)) {
      showHold = false;
      showUpgradeToVideo = false;
      showAddCall = false;
      showDowngradeToAudio = false;
      showPauseVideo = false;
      showSwap = false;
      LogUtil.v("CallButtonPresenter.updateButtonsState", "isSesssionProgressCall, disable hold," +
      " upgrade, add call, downgrade, pause video and swap button.");
    }
    final boolean isCameraOff = call.getVideoState() == VideoProfile.STATE_RX_ENABLED;
    /* @} */

    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_AUDIO, true);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_SWAP, showSwap);
    /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HOLD, showHold);
    /*/
    if (!showSwap && !isVideo) {
        InCallScreen screen = getActivity().getInCallScreen();
        // InCallScreen may not have been created
        // when callbacks due to video downgrade audio
        if (screen != null) {
            Fragment fragment = screen.getInCallScreenFragment();
            if (fragment instanceof InCallFragment) {
                showSwap = ((InCallFragment) fragment)
                        .getButtonController(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY)
                        .isAllowed();
            }
        }
    }
    if (!isVideo) {
        mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HOLD, !showSwap);
    } else {
        mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HOLD, !showPauseVideo);
    }
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_HOLD, showHold);
    //*/
    mInCallButtonUi.setHold(isCallOnHold);
    /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_MUTE, showMute);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_ADD_CALL, true);
    /*/
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_MUTE, true);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_MUTE, showMute);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_ADD_CALL, !(isVideo || showMerge));
    //*/
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_ADD_CALL, showAddCall);
    /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO, showUpgradeToVideo);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_DOWNGRADE_TO_AUDIO, showDowngradeToAudio);
    mInCallButtonUi.showButton(
        InCallButtonIds.BUTTON_SWITCH_CAMERA, isVideo && hasCameraPermission
        /// M: when pause video make one way and only can receive video, we can't show
        /// switch camera button.
        /// and call is held.
        && !isSesssionProgressCall(call) && !isCameraOff && !currentHeldState);
    /*/
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO, !isVideo);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO, showUpgradeToVideo);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_DOWNGRADE_TO_AUDIO, isVideo);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_DOWNGRADE_TO_AUDIO, showDowngradeToAudio);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_SWITCH_CAMERA, isVideo);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_SWITCH_CAMERA, hasCameraPermission
            /// M: when pause video make one way and only can receive video, we can't show
            /// switch camera button.
            /// and call is held.
            && !isSesssionProgressCall(call) && !isCameraOff && !currentHeldState);
    //*/
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_PAUSE_VIDEO, showPauseVideo);
    /// M: Enable pause button together @ {
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_PAUSE_VIDEO, showPauseVideo);
    /// @}
    if (isVideo) {
      mInCallButtonUi.setVideoPaused(!call.getVideoTech().isTransmitting() || !hasCameraPermission
        /// M: Check if video was paused by disabling camera only without session modification @ {
        || (call.getVideoPauseState() == true));
        /// @}
    }
    /// M: For video only show diaplad for some special OP.
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_DIALPAD, showDialpad);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_MERGE, showMerge);

    /// M: MediaTek extension buttons.
    updateExtButtonUI(call);

    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_RECORD_NUMBER, true);
    //*/

      // freeme.zhangjunhe,,20180913. for call record
      // must before mInCallButtonUi.updateButtonStates()
      call.setRecordingStartTime();
      long time = call.getRecordingStartTime();
      Fragment fragment = mInCallButtonUi.getInCallButtonUiFragment();
      if (fragment instanceof InCallFragment) {
          ButtonController controller = ((InCallFragment) fragment)
                  .getButtonController(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD);
          if (controller instanceof ButtonController.SwitchVoiceRecordButtonController) {
              ((ButtonController.SwitchVoiceRecordButtonController) controller)
                      .showRecordTime(time);
          }
      }
      //*/

    mInCallButtonUi.updateButtonStates();
  }

  private boolean hasVideoCallCapabilities(DialerCall call) {
    return call.getVideoTech().isAvailable(mContext);
  }

  /**
   * Determines if downgrading from a video call to an audio-only call is supported. In order to
   * support downgrade to audio, the SDK version must be >= N and the call should NOT have the
   * {@link android.telecom.Call.Details#CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO}.
   *
   * @param call The call.
   * @return {@code true} if downgrading to an audio-only call from a video call is supported.
   */
  private boolean isDowngradeToAudioSupported(DialerCall call) {
    // TODO(b/33676907): If there is an RCS video share session, return true here
    return !call.can(CallCompat.Details.CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO);
  }

  @Override
  public void refreshMuteState() {
    // Restore the previous mute state
    if (mAutomaticallyMuted
        && AudioModeProvider.getInstance().getAudioState().isMuted() != mPreviousMuteState) {
      if (mInCallButtonUi == null) {
        return;
      }
      muteClicked(mPreviousMuteState, false /* clickedByUser */);
    } else {
      refreshSavedMuteState();
    }
    mAutomaticallyMuted = false;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putBoolean(KEY_AUTOMATICALLY_MUTED, mAutomaticallyMuted);
    outState.putBoolean(KEY_PREVIOUS_MUTE_STATE, mPreviousMuteState);
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    mAutomaticallyMuted =
        savedInstanceState.getBoolean(KEY_AUTOMATICALLY_MUTED, mAutomaticallyMuted);
    mPreviousMuteState = savedInstanceState.getBoolean(KEY_PREVIOUS_MUTE_STATE, mPreviousMuteState);
  }

  @Override
  public void onCameraPermissionGranted() {
    if (mCall != null) {
      updateButtonsState(mCall);
    }
  }

  @Override
  public void onActiveCameraSelectionChanged(boolean isUsingFrontFacingCamera) {
    if (mInCallButtonUi == null) {
      return;
    }
    mInCallButtonUi.setCameraSwitched(!isUsingFrontFacingCamera);
  }

  @Override
  public Context getContext() {
    return mContext;
  }

  private InCallActivity getActivity() {
    if (mInCallButtonUi != null) {
      Fragment fragment = mInCallButtonUi.getInCallButtonUiFragment();
      if (fragment != null) {
        return (InCallActivity) fragment.getActivity();
      }
    }
    return null;
  }

  /// M: ------------------ MediaTek features ---------------------
  // Downgrade to audio call.
  @Override
  public void changeToAudioClicked() {
    LogUtil.enterBlock("CallButtonPresenter.changeToAudioClicked");
    if (mCall == null) {
      LogUtil.w("CallButtonPresenter.changeToAudioClicked", "downgradeToAudio failed");
      return;
    }
    mCall.getVideoTech().downgradeToAudio();
    InCallPresenter.getInstance().showMessage(R.string.video_call_downgrade_request);
    //reset hide preview flag
    mCall.setHidePreview(false);
  }

  /**
   * M: Update session state to refresh UI.
   * @param state SessionState
   */
  @Override
  public void onSessionModificationStateChanged(int state) {
    if (mCall == null) {
        return;
    }
    updateButtonsState(mCall);
  }

  /**
   * Hide preview.
   */
  @Override
  public void hidePreviewClicked(boolean hide) {
    LogUtil.enterBlock("CallButtonPresenter.hidePreviewClicked: " + hide);
    InCallPresenter.getInstance().notifyHideLocalVideoChanged(hide);
  }

  /// M:send cancel upgrade request . @{
  public void cancelUpgradeClicked() {
    LogUtil.enterBlock("CallButtonPresenter.cancelUpgradeClicked");
    if (mCall == null) {
         LogUtil.w("CallButtonPresenter.cancelUpgradeClicked", "cancelUpgradeVideoRequest failed");
         return;
    }
    mCall.getVideoTech().cancelUpgradeVideoRequest();
  }
  ///@}

  private void updateExtButtonUI(DialerCall call) {
    final boolean isVideo = call.isVideoCall();
    boolean isCallActive = call.getState() == DialerCall.State.ACTIVE;

    // find whether it's held state, when held state can't do video operation
    boolean currentHeldState = call.isRemotelyHeld();
    final boolean hasCameraPermission =
        isVideo && VideoUtils.hasCameraPermissionAndShownPrivacyToast(mContext);

    /// hide preview. except : camera off, held call, 3G video call, without permission.
    final boolean showHideLocalVideoBtn = isVideo
        && call.getVideoFeatures().supportsHidePreview()
        && isCallActive && !currentHeldState
        && hasCameraPermission
        && !isSesssionProgressCall(call)
        && call.getVideoState() != VideoProfile.STATE_RX_ENABLED;
    /*/ freeme.zhaozehong, 20180324. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HIDE_PREVIEW, showHideLocalVideoBtn);
    /*/
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HIDE_PREVIEW, isVideo);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_HIDE_PREVIEW, showHideLocalVideoBtn);
    //*/

    /// M: [Voice Record] check if should display record @{
    final boolean isUserUnlocked = UserManagerCompat.isUserUnlocked(mContext);
    final boolean canRecordVoice = call
            .can(MtkCall.MtkDetails.MTK_CAPABILITY_CALL_RECORDING)
            && !InCallUtils.isDMLocked()
            && !isVideo
            && isUserUnlocked
            && InCallUiCompat.isMtkTelecomCompat();
    final boolean showRecording = UserHandle.myUserId()
            == UserHandle.USER_OWNER && canRecordVoice;
    /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD, showRecording);
    /*/
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD, !isVideo);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD, showRecording);
    //*/
    mInCallButtonUi.updateRecordStateUi(call.isRecording());
    /// @}

    /// M: [Hang Up] hang up all/hold calls.
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HANG_UP_ALL,
            InCallUtils.canHangupAllCalls());
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HANG_UP_HOLD,
            InCallUtils.canHangupAllHoldCalls());

    /// M: [ECT(blind)]
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_ECT, InCallUtils.canEct());
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_BLIND_ECT, InCallUtils.canBlindEct(call));

    // M : Device Switch
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_DEVICE_SWITCH,
            ExtensionManager.getInCallButtonExt().isDeviceSwitchSupported(call));

    /// M:show cancel button after upgrade request is sent @{
    final boolean showCancelUpgrade = !isVideo && isCallActive
       && call.getVideoFeatures().supportsCancelUpgradeVideo()
       &&(call.getVideoTech().getSessionModificationState()
           == SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE);

    Log.d(this, "[updateExtButtonUI] showCancelUpgrade: " + showCancelUpgrade
                   + ", session_state: " + call.getVideoTech().getSessionModificationState());
    /// M:now in real network isn't support this feature,so disable cancel button on UI.
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_CANCEL_UPGRADE, false);
    ///@}
  }

  private boolean isSesssionProgressCall(DialerCall call) {
    //we will set VideoBtn Enable except the instantaneous state
    int sessionState = call.getVideoTech().getSessionModificationState();
    boolean sessionProgress =
        sessionState == SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE
        || sessionState == SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST
        || sessionState == SessionModificationState.WAITING_FOR_RESPONSE
        /// M: for cancel upgrade @{
        || sessionState == SessionModificationState.WAITING_FOR_CANCEL_UPGRADE_RESPONSE;
        /// @}
    return sessionProgress;
  }

  /**
   * M: [DM Lock]
   * @param call
   */
  private void updateInCallControlsDuringDMLocked(DialerCall call) {
      if (mInCallButtonUi == null) {
          Log.d(this, "just return ui:" + mInCallButtonUi);
          return;
      }
      if (call == null) {
          Log.d(this, "just return call:" + call);
          return;
      }
      //*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
      final boolean isVideo = call.isVideoCall();
      boolean showPauseVideo = isVideo
              && call.getState() != DialerCall.State.DIALING
              && call.getState() != DialerCall.State.CONNECTING;
      boolean showSwap = call.can(android.telecom.Call.Details.CAPABILITY_SWAP_CONFERENCE);
      final boolean showMerge = call.can(android.telecom.Call.Details.CAPABILITY_MERGE_CONFERENCE);
      //*/
      mInCallButtonUi.setEnabled(false);
      /// M: [Voice Record] @{
      mInCallButtonUi.showButton(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD, false);
      /// @}
      mInCallButtonUi.showButton(InCallButtonIds.BUTTON_MERGE, false);
      /*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
      mInCallButtonUi.showButton(InCallButtonIds.BUTTON_ADD_CALL, true);
      /*/
      mInCallButtonUi.showButton(InCallButtonIds.BUTTON_ADD_CALL, !(isVideo || showMerge));
      //*/
      mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_ADD_CALL, false);
      final boolean canHold = call.can(android.telecom.Call.Details.CAPABILITY_HOLD);
      /*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
      mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HOLD, canHold);
      /*/
      mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HOLD, canHold && !(showSwap || showPauseVideo));
      //*/
  }

  /**
   * M: [Voice Record] Start or stop voice record.
   * @param checked True if start voice recording.
   */
  @Override
  public void toggleVoiceRecord(boolean checked) {
      LogUtil.i("CallButtonPresenter.toggleVoiceRecord", "%s", checked ? "start" : "stop");
      TelecomAdapter.getInstance().toggleVoiceRecording(checked);
  }

  /**
   * [ECT(blind)]
   */
  @Override
  public void onBlindOrAssuredEctClicked() {
    if (mCall == null) {
        return;
    }
    if (mContext != null) {
        AddTransferNumberScreenController.getInstance().showAddTransferNumberDialog(
                mContext, mCall.getId());
    }
  }

  @Override
  public void onConsultativeEctClicked() {
      final DialerCall call = CallList.getInstance().getBackgroundCall();
      if (call != null && call.can(
              mediatek.telecom.MtkCall.MtkDetails.MTK_CAPABILITY_CONSULTATIVE_ECT)) {
          TelecomAdapter.getInstance().explicitCallTransfer(
                  call.getTelecomCall().getDetails().getTelecomCallId());
      }
  }

  /**
   * [Device Switch]
   */
  @Override
  public void onDeviceSwitchClicked() {
    if (mCall == null) {
        return;
    }
    if (mContext != null) {
        ExtensionManager.getInCallButtonExt().onMenuItemClick(InCallButtonIds.BUTTON_DEVICE_SWITCH);
    }
  }

  /// M: Force to show buttons to increase performance. @{
  private void updateButtonStateEx() {
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_MUTE, true);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_AUDIO, true);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_DIALPAD, true);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_ADD_CALL, true);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_MUTE, false);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_AUDIO, false);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_DIALPAD, false);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_ADD_CALL, false);
    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_HOLD, true);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_HOLD, false);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO, true);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO, false);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD, true);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD, false);
    mInCallButtonUi.showButton(InCallButtonIds.BUTTON_RECORD_NUMBER, true);
    mInCallButtonUi.enableButton(InCallButtonIds.BUTTON_RECORD_NUMBER, true);
    mInCallButtonUi.updateRecordStateUi(false);
    //*/
  }
  /// @}

  /// M: save mute state in incallpresenter incase of losing state due to
  //  destroy of videocall fragment. @{
  @Override
  public void saveAutoMuteState() {
    if (CallList.getInstance().getWaitingForAccountCall() != null) {
      InCallPresenter.getInstance().saveMuteState(mAutomaticallyMuted, mPreviousMuteState);
      LogUtil.d("CallButtonPresenter.saveAutoMuteState", "mAutomaticallyMuted = "
          + mAutomaticallyMuted + ", mPreviousMuteState = " + mPreviousMuteState);
    }
  }

  private void refreshSavedMuteState() {
    boolean autoMute = InCallPresenter.getInstance().getAutoMuteState();
    boolean muteState = InCallPresenter.getInstance().getMuteState();

    if (autoMute
        && AudioModeProvider.getInstance().getAudioState().isMuted() != muteState){
      if (mInCallButtonUi == null) {
        return;
      }
      muteClicked(muteState, false /* clickedByUser */);
    }
    InCallPresenter.getInstance().saveMuteState(false, false);
  }
  /// @}

  /**
   * M: check and perform pause video without sending SIP message.
   *
   * @param pause Pause/Unpause state of video
   * @return true if supported, false otherwise
   */
  public boolean pauseVideoWithoutSipMessage(boolean pause) {
    if (mCall == null) {
      return false;
    }
    boolean isSupported = mCall.getVideoFeatures().isSupportPauseVideoWithoutSipMessage();
    LogUtil.d("CallButtonPresenter.pauseVideoWithoutSipMessage ", " supported:" + isSupported);
    if (!isSupported) {
      return false;
    }
    if (pause) {
      mCall.getVideoTech().setCamera(null);
      mCall.setVideoPauseState(pause);
    } else {
      InCallCameraManager cameraManager = InCallPresenter.getInstance().getInCallCameraManager();
      String cameraId = cameraManager.getActiveCameraId();
      mCall.getVideoTech().setCamera(cameraId);
      mCall.setVideoPauseState(pause);
    }
    mInCallButtonUi.setVideoPaused(pause);
    return true;
  }

  //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
  @Override
  public void setIsNumberRecord(boolean isRecord) {
    getActivity().setIsNumberRecord(isRecord);
  }
  //*/
}
