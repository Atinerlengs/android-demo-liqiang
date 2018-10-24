/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.incallui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.ColorUtils;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.compat.ActivityCompat;
import com.android.dialer.configprovider.ConfigProviderBindings;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.ScreenEvent;
import com.android.incallui.answer.bindings.AnswerBindings;
import com.android.incallui.answer.protocol.AnswerScreen;
import com.android.incallui.answer.protocol.AnswerScreenDelegate;
import com.android.incallui.answer.protocol.AnswerScreenDelegateFactory;
import com.android.incallui.answerproximitysensor.PseudoScreenState;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.DialerCall.State;
import com.android.incallui.disconnectdialog.DisconnectMessage;
import com.android.incallui.incall.bindings.InCallBindings;
import com.android.incallui.incall.protocol.InCallButtonUiDelegate;
import com.android.incallui.incall.protocol.InCallButtonUiDelegateFactory;
import com.android.incallui.incall.protocol.InCallScreen;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.InCallScreenDelegateFactory;
import com.android.incallui.video.bindings.VideoBindings;
import com.android.incallui.video.protocol.VideoCallScreen;
import com.android.incallui.video.protocol.VideoCallScreenDelegate;
import com.android.incallui.video.protocol.VideoCallScreenDelegateFactory;
import com.mediatek.incallui.utils.InCallUtils;

//*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.incallui.incall.impl.InCallFragment;
import com.freeme.incallui.answer.bindings.FreemeAnswerBindings;
import com.freeme.incallui.FreemeInCallActivityCommon;
import com.freeme.incallui.video.impl.FreemeVideoCallFragment;
//*/

/** Version of {@link InCallActivity} that shows the new UI */
public class InCallActivity extends TransactionSafeFragmentActivity
    implements AnswerScreenDelegateFactory,
        InCallScreenDelegateFactory,
        InCallButtonUiDelegateFactory,
        VideoCallScreenDelegateFactory,
        PseudoScreenState.StateChangedListener {

  private static final String TAG_IN_CALL_SCREEN = "tag_in_call_screen";
  private static final String TAG_ANSWER_SCREEN = "tag_answer_screen";
  private static final String TAG_VIDEO_CALL_SCREEN = "tag_video_call_screen";

  private static final String DID_SHOW_ANSWER_SCREEN_KEY = "did_show_answer_screen";
  private static final String DID_SHOW_IN_CALL_SCREEN_KEY = "did_show_in_call_screen";
  private static final String DID_SHOW_VIDEO_CALL_SCREEN_KEY = "did_show_video_call_screen";

  private static final String CONFIG_ANSWER_AND_RELEASE_ENABLED = "answer_and_release_enabled";

  /*/ freeme.zhaozehong, 20180228. for freemeOS, UI redesign
  private final InCallActivityCommon common;
  /*/
  private final FreemeInCallActivityCommon common;
  //*/
  private boolean didShowAnswerScreen;
  private boolean didShowInCallScreen;
  private boolean didShowVideoCallScreen;
  private int[] backgroundDrawableColors;
  private GradientDrawable backgroundDrawable;
  private boolean isVisible;
  private View pseudoBlackScreenOverlay;
  private boolean touchDownWhenPseudoScreenOff;
  private boolean isInShowMainInCallFragment;
  private boolean needDismissPendingDialogs;

  /*/ freeme.zhaozehong, 20180228. for freemeOS, UI redesign
  public InCallActivity() {
    common = new InCallActivityCommon(this);
  }
  /*/
  public InCallActivity() {
    common = new FreemeInCallActivityCommon(this);
  }
  //*/

  public static Intent getIntent(
      Context context, boolean showDialpad, boolean newOutgoingCall, boolean isForFullScreen) {
    Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setClass(context, InCallActivity.class);
    /*/ freeme.zhaozehong, 20180228. for freemeOS, UI redesign
    InCallActivityCommon.setIntentExtras(intent, showDialpad, newOutgoingCall, isForFullScreen);
    /*/
    FreemeInCallActivityCommon.setIntentExtras(intent, showDialpad, newOutgoingCall,
            isForFullScreen);
    //*/
    return intent;
  }

  @Override
  protected void onResumeFragments() {
    super.onResumeFragments();
    if (needDismissPendingDialogs) {
      dismissPendingDialogs();
    }
  }

  @Override
  protected void onCreate(Bundle icicle) {
    LogUtil.i("InCallActivity.onCreate", "");
    super.onCreate(icicle);

    //*/ freeme.zhaozehong, 20180418. for freemeOS, reset incall background
    common.showBackground();
    //*/

    if (icicle != null) {
      didShowAnswerScreen = icicle.getBoolean(DID_SHOW_ANSWER_SCREEN_KEY);
      didShowInCallScreen = icicle.getBoolean(DID_SHOW_IN_CALL_SCREEN_KEY);
      didShowVideoCallScreen = icicle.getBoolean(DID_SHOW_VIDEO_CALL_SCREEN_KEY);
    }

    common.onCreate(icicle);

    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

    pseudoBlackScreenOverlay = findViewById(R.id.psuedo_black_screen_overlay);
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    LogUtil.i("InCallActivity.onSaveInstanceState", "");
    common.onSaveInstanceState(out);
    out.putBoolean(DID_SHOW_ANSWER_SCREEN_KEY, didShowAnswerScreen);
    out.putBoolean(DID_SHOW_IN_CALL_SCREEN_KEY, didShowInCallScreen);
    out.putBoolean(DID_SHOW_VIDEO_CALL_SCREEN_KEY, didShowVideoCallScreen);
    super.onSaveInstanceState(out);
    isVisible = false;
  }

  @Override
  protected void onStart() {
    LogUtil.i("InCallActivity.onStart", "");
    super.onStart();
    isVisible = true;
    showMainInCallFragment();
    common.onStart();
    if (ActivityCompat.isInMultiWindowMode(this)
        && !getResources().getBoolean(R.bool.incall_dialpad_allowed)) {
      // Hide the dialpad because there may not be enough room
      showDialpadFragment(false, false);
    }
  }

  @Override
  protected void onResume() {
    LogUtil.i("InCallActivity.onResume", "");
    super.onResume();
    common.onResume();
    PseudoScreenState pseudoScreenState = InCallPresenter.getInstance().getPseudoScreenState();
    pseudoScreenState.addListener(this);
    onPseudoScreenStateChanged(pseudoScreenState.isOn());
  }

  /** onPause is guaranteed to be called when the InCallActivity goes in the background. */
  @Override
  protected void onPause() {
    LogUtil.i("InCallActivity.onPause", "");
    super.onPause();
    common.onPause();
    InCallPresenter.getInstance().getPseudoScreenState().removeListener(this);
  }

  @Override
  protected void onStop() {
    LogUtil.i("InCallActivity.onStop", "");
    super.onStop();
    common.onStop();
    isVisible = false;
  }

  @Override
  protected void onDestroy() {
    LogUtil.i("InCallActivity.onDestroy", "");
    super.onDestroy();
    common.onDestroy();
  }

  @Override
  public void finish() {
    if (shouldCloseActivityOnFinish()) {
      // When user select incall ui from recents after the call is disconnected, it tries to launch
      // a new InCallActivity but InCallPresenter is already teared down at this point, which causes
      // crash.
      // By calling finishAndRemoveTask() instead of finish() the task associated with
      // InCallActivity is cleared completely. So system won't try to create a new InCallActivity in
      // this case.
      //
      // Calling finish won't clear the task and normally when an activity finishes it shouldn't
      // clear the task since there could be parent activity in the same task that's still alive.
      // But InCallActivity is special since it's singleInstance which means it's root activity and
      // only instance of activity in the task. So it should be safe to also remove task when
      // finishing.
      // It's also necessary in the sense of it's excluded from recents. So whenever the activity
      // finishes, the task should also be removed since it doesn't make sense to go back to it in
      // anyway anymore.
      super.finishAndRemoveTask();
    }
  }

  private boolean shouldCloseActivityOnFinish() {
    if (!isVisible()) {
      LogUtil.i(
          "InCallActivity.shouldCloseActivityOnFinish",
          "allowing activity to be closed because it's not visible");
      return true;
    }

    if (common.hasPendingDialogs()) {
      LogUtil.i(
          "InCallActivity.shouldCloseActivityOnFinish", "dialog is visible, not closing activity");
      return false;
    }

    AnswerScreen answerScreen = getAnswerScreen();
    if (answerScreen != null && answerScreen.hasPendingDialogs()) {
      LogUtil.i(
          "InCallActivity.shouldCloseActivityOnFinish",
          "answer screen dialog is visible, not closing activity");
      return false;
    }

    LogUtil.i(
        "InCallActivity.shouldCloseActivityOnFinish",
        "activity is visible and has no dialogs, allowing activity to close");
    return true;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    LogUtil.i("InCallActivity.onNewIntent", "");

    // If the screen is off, we need to make sure it gets turned on for incoming calls.
    // This normally works just fine thanks to FLAG_TURN_SCREEN_ON but that only works
    // when the activity is first created. Therefore, to ensure the screen is turned on
    // for the call waiting case, we recreate() the current activity. There should be no jank from
    // this since the screen is already off and will remain so until our new activity is up.
    /// M: If the screen is off in call waiting case, we need finish activity then start again. @{
    //if (!isVisible()
       /// M: ALPS03431103 fix can't add select phone account call because of incallactivity will
       ///disconnect the call before recreat. @{
       //&& CallList.getInstance().getIncomingCall() != null ) {
       ///@}
      //common.onNewIntent(intent, true /* isRecreating */);
      //LogUtil.i("InCallActivity.onNewIntent", "Restarting InCallActivity to force screen on.");
      //recreate();
    //} else {
    common.onNewIntent(intent, false /* isRecreating */);
    //}
    ///@}
  }

  @Override
  public void onBackPressed() {
    LogUtil.i("InCallActivity.onBackPressed", "");
    if (!common.onBackPressed(didShowInCallScreen || didShowVideoCallScreen)) {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    LogUtil.i("InCallActivity.onOptionsItemSelected", "item: " + item);
    if (item.getItemId() == android.R.id.home) {
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    return common.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return common.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
  }

  public boolean isInCallScreenAnimating() {
    return false;
  }

  public void showConferenceFragment(boolean show) {
    if (show) {
      startActivity(new Intent(this, ManageConferenceActivity.class));
    }
  }

  public boolean showDialpadFragment(boolean show, boolean animate) {
    boolean didChange = common.showDialpadFragment(show, animate);
    if (didChange) {
      // Note:  onInCallScreenDialpadVisibilityChange is called here to ensure that the dialpad FAB
      // repositions itself.
      /// M: support show dialpad for video call too. @{
      if (didShowInCallScreen) {
        getInCallScreen().onInCallScreenDialpadVisibilityChange(show);
        //*/ freeme.zhaozehong, 20180302. for freemeOS, UI redesign
        Fragment fragment = getInCallScreen().getInCallScreenFragment();
        if (fragment instanceof InCallFragment) {
          ((InCallFragment) fragment).performShowOrHideCallButtonPager(!show, animate);
          ((InCallFragment) fragment).performShowOrHideCallerInfo(show, !show, animate);
        }
        //*/
      } else if (didShowVideoCallScreen) {
        getVideoCallScreen().onVideoCallScreenDialpadVisibilityChange(show);
        //*/ freeme.zhaozehong, 20180302. for freemeOS, UI redesign
        Fragment fragment = getVideoCallScreen().getVideoCallScreenFragment();
        if (fragment instanceof FreemeVideoCallFragment) {
          ((FreemeVideoCallFragment) fragment).performShowOrHideOtherInfo(!show);
        }
        //*/
      }
      /// @}
    }
    return didChange;
  }

  public boolean isDialpadVisible() {
    return common.isDialpadVisible();
  }

  public void onForegroundCallChanged(DialerCall newForegroundCall) {
    common.updateTaskDescription();
    if (didShowAnswerScreen && newForegroundCall != null) {
      if (newForegroundCall.getState() == State.DISCONNECTED
          || newForegroundCall.getState() == State.IDLE) {
        LogUtil.i(
            "InCallActivity.onForegroundCallChanged",
            "rejecting incoming call, not updating " + "window background color");
      }
    } else {
      LogUtil.v("InCallActivity.onForegroundCallChanged", "resetting background color");
      updateWindowBackgroundColor(0);
      /// M: ALPS03673734, update dialpad color @{
      /*/ freeme.zhaozehong, 20180228. for freemeOS, UI redesign
      if (common.isDialpadVisible()) {
        common.getDialpadFragment().updateColors();
      }
      //*/
      /// @}
    }
  }

  public void updateWindowBackgroundColor(@FloatRange(from = -1f, to = 1.0f) float progress) {
    /*/ freeme.zhaozehong, 20180417. for freemeOS, reset incall background
    ThemeColorManager themeColorManager = InCallPresenter.getInstance().getThemeColorManager();
    @ColorInt int top;
    @ColorInt int middle;
    @ColorInt int bottom;
    @ColorInt int gray = 0x66000000;

    if (ActivityCompat.isInMultiWindowMode(this)) {
      top = themeColorManager.getBackgroundColorSolid();
      middle = themeColorManager.getBackgroundColorSolid();
      bottom = themeColorManager.getBackgroundColorSolid();
    } else {
      top = themeColorManager.getBackgroundColorTop();
      middle = themeColorManager.getBackgroundColorMiddle();
      bottom = themeColorManager.getBackgroundColorBottom();
    }

    if (progress < 0) {
      float correctedProgress = Math.abs(progress);
      top = ColorUtils.blendARGB(top, gray, correctedProgress);
      middle = ColorUtils.blendARGB(middle, gray, correctedProgress);
      bottom = ColorUtils.blendARGB(bottom, gray, correctedProgress);
    }

    boolean backgroundDirty = false;
    if (backgroundDrawable == null) {
      backgroundDrawableColors = new int[] {top, middle, bottom};
      backgroundDrawable = new GradientDrawable(Orientation.TOP_BOTTOM, backgroundDrawableColors);
      backgroundDirty = true;
    } else {
      if (backgroundDrawableColors[0] != top) {
        backgroundDrawableColors[0] = top;
        backgroundDirty = true;
      }
      if (backgroundDrawableColors[1] != middle) {
        backgroundDrawableColors[1] = middle;
        backgroundDirty = true;
      }
      if (backgroundDrawableColors[2] != bottom) {
        backgroundDrawableColors[2] = bottom;
        backgroundDirty = true;
      }
      if (backgroundDirty) {
        backgroundDrawable.setColors(backgroundDrawableColors);
      }
    }

    if (backgroundDirty) {
      getWindow().setBackgroundDrawable(backgroundDrawable);
    }
    /*/
    common.showBackground();
    //*/
  }

  public boolean isVisible() {
    return isVisible;
  }

  public boolean getCallCardFragmentVisible() {
    return didShowInCallScreen || didShowVideoCallScreen;
  }

  public void dismissKeyguard(boolean dismiss) {
    common.dismissKeyguard(dismiss);
  }

  public void showPostCharWaitDialog(String callId, String chars) {
    common.showPostCharWaitDialog(callId, chars);
  }

  public void maybeShowErrorDialogOnDisconnect(DisconnectMessage disconnectMessage) {
    common.maybeShowErrorDialogOnDisconnect(disconnectMessage);
  }

  public void dismissPendingDialogs() {
    if (isVisible) {
      LogUtil.i("InCallActivity.dismissPendingDialogs", "");
      common.dismissPendingDialogs();
      AnswerScreen answerScreen = getAnswerScreen();
      if (answerScreen != null) {
        answerScreen.dismissPendingDialogs();
      }
      needDismissPendingDialogs = false;
    } else {
      // The activity is not visible and onSaveInstanceState may have been called so defer the
      // dismissing action.
      LogUtil.i(
          "InCallActivity.dismissPendingDialogs", "defer actions since activity is not visible");
      needDismissPendingDialogs = true;
    }
  }

  private void enableInCallOrientationEventListener(boolean enable) {
    common.enableInCallOrientationEventListener(enable);
  }

  public void setExcludeFromRecents(boolean exclude) {
    common.setExcludeFromRecents(exclude);
  }

  @Nullable
  public FragmentManager getDialpadFragmentManager() {
    /// M: supoort show dialpad for video call. @{
    InCallScreen inCallScreen = getInCallScreen();
    VideoCallScreen videoCallScreen = getVideoCallScreen();
    if (didShowInCallScreen && inCallScreen != null) {
      return inCallScreen.getInCallScreenFragment().getChildFragmentManager();
    } else if (didShowVideoCallScreen && videoCallScreen != null){
      return videoCallScreen.getVideoCallScreenFragment().getChildFragmentManager();
    }
    /// @}
    return null;
  }

  public int getDialpadContainerId() {
    /// M: supoort show dialpad for video call. @{
    if (didShowInCallScreen) {
      return getInCallScreen().getAnswerAndDialpadContainerResourceId();
    } else {
      return getVideoCallScreen().getDialpadContainerResourceId();
    }
    /// @}
  }

  @Override
  public AnswerScreenDelegate newAnswerScreenDelegate(AnswerScreen answerScreen) {
    DialerCall call = CallList.getInstance().getCallById(answerScreen.getCallId());
    if (call == null) {
      // This is a work around for a bug where we attempt to create a new delegate after the call
      // has already been removed. An example of when this can happen is:
      // 1. incoming video call in landscape mode
      // 2. remote party hangs up
      // 3. activity switches from landscape to portrait
      // At step #3 the answer fragment will try to create a new answer delegate but the call won't
      // exist. In this case we'll simply return a stub delegate that does nothing. This is ok
      // because this new state is transient and the activity will be destroyed soon.
      LogUtil.i("InCallActivity.onPrimaryCallStateChanged", "call doesn't exist, using stub");
      return new AnswerScreenPresenterStub();
    } else {
      return new AnswerScreenPresenter(
          this, answerScreen, CallList.getInstance().getCallById(answerScreen.getCallId()));
    }
  }

  @Override
  public InCallScreenDelegate newInCallScreenDelegate() {
    return new CallCardPresenter(this);
  }

  @Override
  public InCallButtonUiDelegate newInCallButtonUiDelegate() {
    return new CallButtonPresenter(this);
  }

  @Override
  public VideoCallScreenDelegate newVideoCallScreenDelegate(VideoCallScreen videoCallScreen) {
    DialerCall dialerCall = CallList.getInstance().getCallById(videoCallScreen.getCallId());
    if (dialerCall != null && dialerCall.getVideoTech().shouldUseSurfaceView()) {
      return dialerCall.getVideoTech().createVideoCallScreenDelegate(this, videoCallScreen);
    }
    return new VideoCallPresenter();
  }

  public void onPrimaryCallStateChanged() {
    LogUtil.d("InCallActivity.onPrimaryCallStateChanged", "");
    showMainInCallFragment();
  }

  public void onWiFiToLteHandover(DialerCall call) {
    common.showWifiToLteHandoverToast(call);
  }

  public void onHandoverToWifiFailed(DialerCall call) {
    common.showWifiFailedDialog(call);
  }

  public void onInternationalCallOnWifi(@NonNull DialerCall call) {
    LogUtil.enterBlock("InCallActivity.onInternationalCallOnWifi");
    common.showInternationalCallOnWifiDialog(call);
  }

  public void setAllowOrientationChange(boolean allowOrientationChange) {
    if (!allowOrientationChange) {
      setRequestedOrientation(InCallOrientationEventListener.ACTIVITY_PREFERENCE_DISALLOW_ROTATION);
    } else {
      setRequestedOrientation(InCallOrientationEventListener.ACTIVITY_PREFERENCE_ALLOW_ROTATION);
    }
    enableInCallOrientationEventListener(allowOrientationChange);
  }

  public void hideMainInCallFragment() {
    LogUtil.i("InCallActivity.hideMainInCallFragment", "");
    if (didShowInCallScreen || didShowVideoCallScreen) {
      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
      hideInCallScreenFragment(transaction);
      hideVideoCallScreenFragment(transaction);
      transaction.commitAllowingStateLoss();
      getSupportFragmentManager().executePendingTransactions();
    }
  }

  private void showMainInCallFragment() {
    // If the activity's onStart method hasn't been called yet then defer doing any work.
    if (!isVisible) {
      LogUtil.i("InCallActivity.showMainInCallFragment", "not visible yet/anymore");
      return;
    }

    // Don't let this be reentrant.
    if (isInShowMainInCallFragment) {
      LogUtil.i("InCallActivity.showMainInCallFragment", "already in method, bailing");
      return;
    }

    isInShowMainInCallFragment = true;
    ShouldShowUiResult shouldShowAnswerUi = getShouldShowAnswerUi();
    ShouldShowUiResult shouldShowVideoUi = getShouldShowVideoUi();
    LogUtil.d(
        "InCallActivity.showMainInCallFragment",
        "shouldShowAnswerUi: %b, shouldShowVideoUi: %b, "
            + "didShowAnswerScreen: %b, didShowInCallScreen: %b, didShowVideoCallScreen: %b",
        shouldShowAnswerUi.shouldShow,
        shouldShowVideoUi.shouldShow,
        didShowAnswerScreen,
        didShowInCallScreen,
        didShowVideoCallScreen);
    /// M:[ALPS03482828] modify allow orientation conditions.incallactivity and videocallpresenter
    ///have conflicts on allow orientation.keep incallactivity and videocallpresenter have same
    ///conditions. @{
    /// Google original code:@{
    // Only video call ui allows orientation change.
    //setAllowOrientationChange(shouldShowVideoUi.shouldShow);
    ///@}
    setAllowOrientationChange(isAllowOrientation(shouldShowAnswerUi,shouldShowVideoUi));
    ///@}
    /// M:ALPS03538860 clear rotation when disable incallOrientationEventListner.@{
    common.checkResetOrientation();
    ///@}
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    boolean didChangeInCall;
    boolean didChangeVideo;
    boolean didChangeAnswer;
    if (shouldShowAnswerUi.shouldShow) {
      didChangeInCall = hideInCallScreenFragment(transaction);
      didChangeVideo = hideVideoCallScreenFragment(transaction);
      didChangeAnswer = showAnswerScreenFragment(transaction, shouldShowAnswerUi.call);
    } else if (shouldShowVideoUi.shouldShow) {
      didChangeInCall = hideInCallScreenFragment(transaction);
      didChangeVideo = showVideoCallScreenFragment(transaction, shouldShowVideoUi.call);
      didChangeAnswer = hideAnswerScreenFragment(transaction);
    /// M: Hide incall screen when exist waiting account call. @{
    } else if (CallList.getInstance().getWaitingForAccountCall() != null) {
      didChangeInCall = hideInCallScreenFragment(transaction);
      didChangeVideo = hideVideoCallScreenFragment(transaction);
      didChangeAnswer = hideAnswerScreenFragment(transaction);
    /// @}
    } else {
      didChangeInCall = showInCallScreenFragment(transaction);
      didChangeVideo = hideVideoCallScreenFragment(transaction);
      didChangeAnswer = hideAnswerScreenFragment(transaction);
    }

    //*/ freeme.zhaozehong, 20180319. for freemeOS, UI redesign
    if(!common.isAccountSelected()){
      finish();
      return;
    }
    //*/

    if (didChangeInCall || didChangeVideo || didChangeAnswer) {
      transaction.commitNow();
      Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    }
    isInShowMainInCallFragment = false;
  }

  private ShouldShowUiResult getShouldShowAnswerUi() {
    DialerCall call = CallList.getInstance().getIncomingCall();
    if (call != null) {
      LogUtil.d("InCallActivity.getShouldShowAnswerUi", "found incoming call");
      return new ShouldShowUiResult(true, call);
    }

    call = CallList.getInstance().getVideoUpgradeRequestCall();
    if (call != null) {
      LogUtil.i("InCallActivity.getShouldShowAnswerUi", "found video upgrade request");
      return new ShouldShowUiResult(true, call);
    }

    // Check if we're showing the answer screen and the call is disconnected. If this condition is
    // true then we won't switch from the answer UI to the in call UI. This prevents flicker when
    // the user rejects an incoming call.
    call = CallList.getInstance().getFirstCall();
    if (call == null) {
      call = CallList.getInstance().getBackgroundCall();
    }
    if (didShowAnswerScreen && (call == null || call.getState() == State.DISCONNECTED)) {
      LogUtil.i("InCallActivity.getShouldShowAnswerUi", "found disconnecting incoming call");
      return new ShouldShowUiResult(true, call);
    }

    return new ShouldShowUiResult(false, null);
  }

  private static ShouldShowUiResult getShouldShowVideoUi() {
    DialerCall call = CallList.getInstance().getFirstCall();
    if (call == null) {
      LogUtil.i("InCallActivity.getShouldShowVideoUi", "null call");
      return new ShouldShowUiResult(false, null);
    }

    if (call.isVideoCall()) {
      LogUtil.d("InCallActivity.getShouldShowVideoUi", "found video call");
      return new ShouldShowUiResult(true, call);
    }

    if (call.hasSentVideoUpgradeRequest()) {
      LogUtil.i("InCallActivity.getShouldShowVideoUi", "upgrading to video");
      return new ShouldShowUiResult(true, call);
    }

    return new ShouldShowUiResult(false, null);
  }

  private boolean showAnswerScreenFragment(FragmentTransaction transaction, DialerCall call) {
    // When rejecting a call the active call can become null in which case we should continue
    // showing the answer screen.
    if (didShowAnswerScreen && call == null) {
      return false;
    }

    Assert.checkArgument(call != null, "didShowAnswerScreen was false but call was still null");

    boolean isVideoUpgradeRequest = call.hasReceivedVideoUpgradeRequest();

    // Check if we're already showing an answer screen for this call.
    if (didShowAnswerScreen) {
      AnswerScreen answerScreen = getAnswerScreen();
      if (answerScreen.getCallId().equals(call.getId())
          && answerScreen.isVideoCall() == call.isVideoCall()
          && answerScreen.isVideoUpgradeRequest() == isVideoUpgradeRequest
          && !answerScreen.isActionTimeout()) {
        LogUtil.d(
            "InCallActivity.showAnswerScreenFragment",
            "answer fragment exists for same call and has NOT been accepted/rejected/timed out");
        return false;
      }
      if (answerScreen.isActionTimeout()) {
        LogUtil.i(
            "InCallActivity.showAnswerScreenFragment",
            "answer fragment exists but has been accepted/rejected and timed out");
      } else {
        LogUtil.i(
            "InCallActivity.showAnswerScreenFragment",
            "answer fragment exists but arguments do not match");
      }
      hideAnswerScreenFragment(transaction);
    }

    // Show a new answer screen.
    /*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
    AnswerScreen answerScreen =
        AnswerBindings.createAnswerScreen(
            call.getId(),
            call.isVideoCall(),
            isVideoUpgradeRequest,
            call.getVideoTech().isSelfManagedCamera(),
            shouldAllowAnswerAndRelease(call),
            CallList.getInstance().getBackgroundCall() != null);
    /*/
    AnswerScreen answerScreen =
            FreemeAnswerBindings.createAnswerScreen(
                    call.getId(),
                    call.isVideoCall(),
                    isVideoUpgradeRequest,
                    call.getVideoTech().isSelfManagedCamera(),
                    shouldAllowAnswerAndRelease(call),
                    CallList.getInstance().getBackgroundCall() != null);
    //*/
    transaction.add(R.id.main, answerScreen.getAnswerScreenFragment(), TAG_ANSWER_SCREEN);

    Logger.get(this).logScreenView(ScreenEvent.Type.INCOMING_CALL, this);
    didShowAnswerScreen = true;
    return true;
  }

  private boolean shouldAllowAnswerAndRelease(DialerCall call) {
    if (CallList.getInstance().getActiveCall() == null) {
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "no active call");
      return false;
    }
    /// M:ALPS03415796.When CT insert slot 0 and CMCC insert slot 1,TelephonyManager.getphoneType()
    ///will alway get CDMA.the cdma Call don't support the operation of AnswerAndRelease.
    ///so when there are 1A1W in CMCC card, the operation of AnswerAndRelease isn't support.
    ///that's no right.Use the right function instead of TelephonyManager.getphoneType(). @{
    ///Google original code:
    /*if (getSystemService(TelephonyManager.class).getPhoneType()
        == TelephonyManager.PHONE_TYPE_CDMA) {*/
    if (InCallUtils.isCdmaCall(call) == true) {
    ///@}
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "PHONE_TYPE_CDMA not supported");
      return false;
    }
    //// M:[ALPS03469461] fix show wrong answer button when active call and waitting call come from
    ////different phone account,answer screen shounldn't show answer and hold ongoing call. @{
    if (InCallUtils.isCdmaCall(CallList.getInstance().getActiveCall())) {
    ///@}
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "active call and waiting call is"
              + " different phone account");
      return false;
    }
    ///@}

    if (call.isVideoCall() || call.hasReceivedVideoUpgradeRequest()) {
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "video call");
      return false;
    }
    if (!ConfigProviderBindings.get(this).getBoolean(CONFIG_ANSWER_AND_RELEASE_ENABLED, true)) {
      LogUtil.i("InCallActivity.shouldAllowAnswerAndRelease", "disabled by config");
      return false;
    }

    return true;
  }

  private boolean hideAnswerScreenFragment(FragmentTransaction transaction) {
    if (!didShowAnswerScreen) {
      return false;
    }
    AnswerScreen answerScreen = getAnswerScreen();
    if (answerScreen != null) {
      transaction.remove(answerScreen.getAnswerScreenFragment());
    }

    didShowAnswerScreen = false;
    return true;
  }

  private boolean showInCallScreenFragment(FragmentTransaction transaction) {
    if (didShowInCallScreen) {
      return false;
    }
    //// M:ALPS03535093 Shall not remove old screen, otherwise mut state may be lost. @{
    //// InCallScreen inCallScreen = InCallBindings.createInCallScreen();
    //// transaction.add(R.id.main, inCallScreen.getInCallScreenFragment(), TAG_IN_CALL_SCREEN);
    InCallScreen inCallScreen = getInCallScreen();
    if (inCallScreen == null) {
      inCallScreen = InCallBindings.createInCallScreen();
      transaction.add(R.id.main, inCallScreen.getInCallScreenFragment(), TAG_IN_CALL_SCREEN);
    } else {
      transaction.show(inCallScreen.getInCallScreenFragment());
    }
    ///@}
    Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    didShowInCallScreen = true;
    return true;
  }

  private boolean hideInCallScreenFragment(FragmentTransaction transaction) {
    if (!didShowInCallScreen) {
      return false;
    }
    InCallScreen inCallScreen = getInCallScreen();
    if (inCallScreen != null) {
      //// M:ALPS03535093 Shall not remove old screen, otherwise mut state may be lost. @{
      //// transaction.remove(inCallScreen.getInCallScreenFragment());
      transaction.hide(inCallScreen.getInCallScreenFragment());
      /// @}
    }
    didShowInCallScreen = false;
    return true;
  }

  private boolean showVideoCallScreenFragment(FragmentTransaction transaction, DialerCall call) {
    if (didShowVideoCallScreen) {
      VideoCallScreen videoCallScreen = getVideoCallScreen();
      if (videoCallScreen.getCallId().equals(call.getId())) {
        return false;
      }
      LogUtil.i(
          "InCallActivity.showVideoCallScreenFragment",
          "video call fragment exists but arguments do not match");
      hideVideoCallScreenFragment(transaction);
    }

    LogUtil.i("InCallActivity.showVideoCallScreenFragment", "call: %s", call);

    VideoCallScreen videoCallScreen =
        VideoBindings.createVideoCallScreen(
            call.getId(), call.getVideoTech().shouldUseSurfaceView());
    transaction.add(R.id.main, videoCallScreen.getVideoCallScreenFragment(), TAG_VIDEO_CALL_SCREEN);

    Logger.get(this).logScreenView(ScreenEvent.Type.INCALL, this);
    didShowVideoCallScreen = true;
    return true;
  }

  private boolean hideVideoCallScreenFragment(FragmentTransaction transaction) {
    if (!didShowVideoCallScreen) {
      return false;
    }
    VideoCallScreen videoCallScreen = getVideoCallScreen();
    if (videoCallScreen != null) {
      transaction.remove(videoCallScreen.getVideoCallScreenFragment());
    }
    didShowVideoCallScreen = false;
    return true;
  }

  AnswerScreen getAnswerScreen() {
    return (AnswerScreen) getSupportFragmentManager().findFragmentByTag(TAG_ANSWER_SCREEN);
  }

  InCallScreen getInCallScreen() {
    return (InCallScreen) getSupportFragmentManager().findFragmentByTag(TAG_IN_CALL_SCREEN);
  }

  VideoCallScreen getVideoCallScreen() {
    return (VideoCallScreen) getSupportFragmentManager().findFragmentByTag(TAG_VIDEO_CALL_SCREEN);
  }

  @Override
  public void onPseudoScreenStateChanged(boolean isOn) {
    LogUtil.i("InCallActivity.onPseudoScreenStateChanged", "isOn: " + isOn);
    pseudoBlackScreenOverlay.setVisibility(isOn ? View.GONE : View.VISIBLE);
  }

  /**
   * For some touch related issue, turning off the screen can be faked by drawing a black view over
   * the activity. All touch events started when the screen is "off" is rejected.
   *
   * @see PseudoScreenState
   */
  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    // Reject any gesture that started when the screen is in the fake off state.
    if (touchDownWhenPseudoScreenOff) {
      if (event.getAction() == MotionEvent.ACTION_UP) {
        touchDownWhenPseudoScreenOff = false;
      }
      return true;
    }
    // Reject all touch event when the screen is in the fake off state.
    if (!InCallPresenter.getInstance().getPseudoScreenState().isOn()) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        touchDownWhenPseudoScreenOff = true;
        LogUtil.i("InCallActivity.dispatchTouchEvent", "touchDownWhenPseudoScreenOff");
      }
      return true;
    }
    return super.dispatchTouchEvent(event);
  }

  private static class ShouldShowUiResult {
    public final boolean shouldShow;
    public final DialerCall call;

    ShouldShowUiResult(boolean shouldShow, DialerCall call) {
      this.shouldShow = shouldShow;
      this.call = call;
    }
  }

  /// M:[ALPS03482828] modify allow orientation conditions.incallactivity and videocallpresenter
  ///have conflicts on allow orientation.keep incallactivity and videocallpresenter have same
  ///conditions. in the following case,it will allow orientation:
  ///1.active video call 2.send or recevie upgrade request .@{
  private boolean isAllowOrientation(ShouldShowUiResult answerUi,ShouldShowUiResult videoUi) {
   DialerCall mcall;
   if(answerUi.shouldShow) {
     mcall = answerUi.call;
     if (mcall != null && mcall.hasReceivedVideoUpgradeRequest()) {
       LogUtil.d("InCallActivity.isAllowOrientation ","is true");
       return true;
     }
   } else if (videoUi.shouldShow) {
     mcall = videoUi.call;
     if (mcall != null && mcall.isVideoCall() && mcall.getState() != DialerCall.State.ACTIVE) {
       return false;
     } else {
       LogUtil.d("InCallActivity.isAllowOrientation ","is true");
       return true;
     }
   }
   return false;
  }
  ///@}

  //*/ freeme.zhaozehong, 20180303. for freemeOS, UI redesign
  private final String KEY_RECORD_NUMBER = "KEY_RECORD_NUMBER";
  private String mNumber;
  private String mRecordNumber;

  public void setNumber(String number) {
    this.mNumber = number;
  }

  public void setRecordNumber(String recordNumber) {
    this.mRecordNumber = recordNumber;
  }

  public void setIsNumberRecord(boolean isRecord) {
    if (common != null) {
      common.setIsNumberRecord(isRecord, isRecord ? mRecordNumber : mNumber);
    }
  }

  public void recordNumerToDialpad() {
    if (TextUtils.isEmpty(mRecordNumber)) return;
    Intent recordIntent = new Intent(Intent.ACTION_DIAL, getCallUri(mRecordNumber));
    recordIntent.putExtra(KEY_RECORD_NUMBER, true);
    recordIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
    startActivity(recordIntent);
  }

  private Uri getCallUri(String number) {
    if (PhoneNumberUtils.isUriNumber(number)) {
      return Uri.fromParts("sip", number, null);
    }
    return Uri.fromParts("tel", number, null);
  }
  //*/
  //*/ freeme.zhaozehong, 20180424. for freemeOS, UI redesign
  public static Intent getIntentPreStart(Context context, boolean showDialpad,
                                         boolean newOutgoingCall, boolean isForFullScreen) {
    if (InCallPresenter.getInstance().getActivity() != null) {
      return null;
    }
    Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setClass(context, InCallActivity.class);
    FreemeInCallActivityCommon.setIntentExtras(intent, showDialpad, newOutgoingCall,
            isForFullScreen);
    return intent;
  }
  //*/

    //*/ freeme.zhangjunhe,20180919 save incall state for floating call icon
    public void setInCallState(InCallPresenter.InCallState state) {
        if (common != null) {
            common.setInCallState(state);
        }
    }
    //*/
}
