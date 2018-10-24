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

package com.android.incallui.incall.impl;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.telecom.CallAudioState;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.dialer.common.LogUtil;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.multimedia.MultimediaData;
import com.android.dialer.widget.LockableViewPager;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;
import com.android.incallui.audioroute.AudioRouteSelectorDialogFragment;
import com.android.incallui.audioroute.AudioRouteSelectorDialogFragment.AudioRouteSelectorPresenter;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.contactgrid.ContactGridManager;
import com.android.incallui.hold.OnHoldFragment;
import com.android.incallui.incall.impl.ButtonController.SpeakerButtonController;
import com.android.incallui.incall.impl.InCallButtonGridFragment.OnButtonGridCreatedListener;
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

import java.util.ArrayList;
import java.util.List;

//*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.util.ViewUtil;
import com.freeme.incallui.contactgrid.FreemeContactGridManager;
import com.android.incallui.InCallActivity;
import com.freeme.incallui.incall.hold.FreemeConferenceFragment;
import com.freeme.incallui.incall.hold.FreemeOnHoldFragment;
import com.freeme.incallui.incall.impl.FreemeButtonChooserFactory;
import com.freeme.incallui.incall.impl.FreemeInCallButtonGridFragment;
import com.freeme.incallui.incall.protocol.FreemeOnHoldInfo;
import java.util.LinkedHashMap;
//*/

/** Fragment that shows UI for an ongoing voice call. */
public class InCallFragment extends Fragment
    implements InCallScreen,
        InCallButtonUi,
        OnClickListener,
        AudioRouteSelectorPresenter,
        /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
        OnButtonGridCreatedListener {
        /*/
        FreemeInCallButtonGridFragment.OnButtonGridCreatedListener {
        //*/

  private List<ButtonController> buttonControllers = new ArrayList<>();
  private View endCallButton;
  private InCallPaginator paginator;
  private LockableViewPager pager;
  private InCallPagerAdapter adapter;
  /*/ freeme.zhaozehong, 20180227. for freemeOS, UI redesign
  private ContactGridManager contactGridManager;
  /*/
  private FreemeContactGridManager contactGridManager;
  //*/
  private InCallScreenDelegate inCallScreenDelegate;
  private InCallButtonUiDelegate inCallButtonUiDelegate;
  /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
  private InCallButtonGridFragment inCallButtonGridFragment;
  /*/
  private FreemeInCallButtonGridFragment inCallButtonGridFragment;
  //*/
  @Nullable private ButtonChooser buttonChooser;
  private SecondaryInfo savedSecondaryInfo;
  private int voiceNetworkType;
  private int phoneType;
  private boolean stateRestored;

  // Add animation to educate users. If a call has enriched calling attachments then we'll
  // initially show the attachment page. After a delay seconds we'll animate to the button grid.
  private final Handler handler = new Handler();
  private final Runnable pagerRunnable =
      new Runnable() {
        @Override
        public void run() {
          pager.setCurrentItem(adapter.getButtonGridPosition());
        }
      };

  private static boolean isSupportedButton(@InCallButtonIds int id) {
    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    if (true) {
      return id == InCallButtonIds.BUTTON_MUTE
              || id == InCallButtonIds.BUTTON_AUDIO
              || id == InCallButtonIds.BUTTON_DIALPAD
              || id == InCallButtonIds.BUTTON_HOLD
              || id == InCallButtonIds.BUTTON_SWAP
              || id == InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO
              || id == InCallButtonIds.BUTTON_ADD_CALL
              || id == InCallButtonIds.BUTTON_MERGE
              || id == InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD
              || id == InCallButtonIds.BUTTON_RECORD_NUMBER;
    }
    //*/
    return id == InCallButtonIds.BUTTON_AUDIO
        || id == InCallButtonIds.BUTTON_MUTE
        || id == InCallButtonIds.BUTTON_DIALPAD
        || id == InCallButtonIds.BUTTON_HOLD
        || id == InCallButtonIds.BUTTON_SWAP
        || id == InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO
        || id == InCallButtonIds.BUTTON_ADD_CALL
        || id == InCallButtonIds.BUTTON_MERGE
        || id == InCallButtonIds.BUTTON_MANAGE_VOICE_CONFERENCE
        /// M: [Voice Record]
        || id == InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD
        /// M: [Hang Up]
        || id == InCallButtonIds.BUTTON_HANG_UP_ALL
        || id == InCallButtonIds.BUTTON_HANG_UP_HOLD
        /// M: [ECT(blind)]
        || id == InCallButtonIds.BUTTON_ECT
        || id == InCallButtonIds.BUTTON_BLIND_ECT
        || id == InCallButtonIds.BUTTON_DEVICE_SWITCH;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (savedSecondaryInfo != null) {
      setSecondary(savedSecondaryInfo);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    inCallButtonUiDelegate =
        FragmentUtils.getParent(this, InCallButtonUiDelegateFactory.class)
            .newInCallButtonUiDelegate();
    if (savedInstanceState != null) {
      inCallButtonUiDelegate.onRestoreInstanceState(savedInstanceState);
      stateRestored = true;
    }
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater layoutInflater,
      @Nullable ViewGroup viewGroup,
      @Nullable Bundle bundle) {
    LogUtil.i("InCallFragment.onCreateView", null);
    /*/ freeme.zhaozehong, 20180227. for freemeOS, UI redesign
    final View view = layoutInflater.inflate(R.layout.frag_incall_voice, viewGroup, false);
    contactGridManager =
        new ContactGridManager(
            view,
            (ImageView) view.findViewById(R.id.contactgrid_avatar),
            getResources().getDimensionPixelSize(R.dimen.incall_avatar_size),
            true /* showAnonymousAvatar * /);
    /*/
    final View view = layoutInflater.inflate(R.layout.freeme_frag_incall_voice, viewGroup, false);
    contactGridManager = new FreemeContactGridManager(view,
            view.findViewById(R.id.contactgrid_avatar),
            getResources().getDimensionPixelSize(R.dimen.incall_avatar_size),
            true);
    //*/

    paginator = (InCallPaginator) view.findViewById(R.id.incall_paginator);
    pager = (LockableViewPager) view.findViewById(R.id.incall_pager);
    pager.setOnTouchListener(
        (v, event) -> {
          handler.removeCallbacks(pagerRunnable);
          return false;
        });

    endCallButton = view.findViewById(R.id.incall_end_call);
    endCallButton.setOnClickListener(this);
    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    dialpadButton = view.findViewById(R.id.freeme_incall_dialpad);
    dialpadButton.setIsHideLabel(true);
    speakerButton = view.findViewById(R.id.freeme_incall_speaker);
    speakerButton.setIsHideLabel(true);
    //*/

    /// M: ALPS03836283, Increase call performace.
    /// Using API in DialerCall to get network type and phone type. @{
    DialerCall call = CallList.getInstance().getFirstCall();
    if (call != null) {
      voiceNetworkType = call.getVoiceNetworkTypeForCall();
      if (voiceNetworkType != TelephonyManager.NETWORK_TYPE_UNKNOWN) {
        phoneType = call.getPhoneTypeByNetworkType(voiceNetworkType);
        return view;
      }
    }
    /// @}

    if (ContextCompat.checkSelfPermission(getContext(), permission.READ_PHONE_STATE)
        != PackageManager.PERMISSION_GRANTED) {
      voiceNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    } else {

      voiceNetworkType =
          VERSION.SDK_INT >= VERSION_CODES.N
              ? getContext().getSystemService(TelephonyManager.class).getVoiceNetworkType()
              : TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }
    phoneType = getContext().getSystemService(TelephonyManager.class).getPhoneType();
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    inCallButtonUiDelegate.refreshMuteState();
    inCallScreenDelegate.onInCallScreenResumed();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle bundle) {
    LogUtil.i("InCallFragment.onViewCreated", null);
    super.onViewCreated(view, bundle);
    /// M: [Voice Record] Add for recording. @{
    initVoiceRecorderIcon(view);
    /// @}
    inCallScreenDelegate =
        FragmentUtils.getParent(this, InCallScreenDelegateFactory.class).newInCallScreenDelegate();
    Assert.isNotNull(inCallScreenDelegate);

    buttonControllers.add(new ButtonController.MuteButtonController(inCallButtonUiDelegate));
    buttonControllers.add(new ButtonController.SpeakerButtonController(inCallButtonUiDelegate));
    buttonControllers.add(new ButtonController.DialpadButtonController(inCallButtonUiDelegate));
    buttonControllers.add(new ButtonController.HoldButtonController(inCallButtonUiDelegate));
    buttonControllers.add(new ButtonController.AddCallButtonController(inCallButtonUiDelegate));
    buttonControllers.add(new ButtonController.SwapButtonController(inCallButtonUiDelegate));
    buttonControllers.add(new ButtonController.MergeButtonController(inCallButtonUiDelegate));
    buttonControllers.add(
        new ButtonController.UpgradeToVideoButtonController(inCallButtonUiDelegate));
    /// M: [Voice Record] @{
    buttonControllers.add(
            new ButtonController.SwitchVoiceRecordButtonController(inCallButtonUiDelegate));
    /// @}
    /// M: [ECT(blind)] @{
    buttonControllers.add(
            new ButtonController.ECTButtonController(inCallButtonUiDelegate));
    buttonControllers.add(
            new ButtonController.BlindECTButtonController(inCallButtonUiDelegate));
    /// @}
    /// M: [Hang Up] @{
    buttonControllers.add(
            new ButtonController.HangupAllButtonController(inCallScreenDelegate));
    buttonControllers.add(
            new ButtonController.HangupHoldButtonController(inCallScreenDelegate));
    /// @}
    buttonControllers.add(
        new ButtonController.ManageConferenceButtonController(inCallScreenDelegate));
    buttonControllers.add(
        new ButtonController.SwitchToSecondaryButtonController(inCallScreenDelegate));
  /// M: [Device Switch] @{
    buttonControllers.add(
            new ButtonController.DeviceSwitchButtonController(inCallButtonUiDelegate));
    /// @}

    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    buttonControllers.add(
            new ButtonController.NumberRecordButtonController(inCallButtonUiDelegate));
    //*/
    inCallScreenDelegate.onInCallScreenDelegateInit(this);
    inCallScreenDelegate.onInCallScreenReady();
  }

  @Override
  public void onPause() {
    super.onPause();
    inCallScreenDelegate.onInCallScreenPaused();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    inCallScreenDelegate.onInCallScreenUnready();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    inCallButtonUiDelegate.onSaveInstanceState(outState);
  }

  @Override
  public void onClick(View view) {
    if (view == endCallButton) {
      LogUtil.i("InCallFragment.onClick", "end call button clicked");
      inCallScreenDelegate.onEndCallClicked();
    } else {
      LogUtil.e("InCallFragment.onClick", "unknown view: " + view);
      Assert.fail();
    }
  }

  @Override
  public void setPrimary(@NonNull PrimaryInfo primaryInfo) {
    LogUtil.d("InCallFragment.setPrimary", primaryInfo.toString());
    setAdapterMedia(primaryInfo.multimediaData);
    contactGridManager.setPrimary(primaryInfo);

    //*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
    if (!TextUtils.isEmpty(primaryInfo.mId)) {
        FreemeOnHoldInfo info = new FreemeOnHoldInfo(primaryInfo.mId, primaryInfo.name, primaryInfo.number,
                primaryInfo.nameIsNumber, primaryInfo.label, primaryInfo.contactInfoLookupKey,
                primaryInfo.mIsConferenceCall);
        info.primary = true;
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
        //*/ freeme.zhangjunhe, 20180717. for multi call display two primary call
        for (FreemeOnHoldInfo holdInfo : mFreemeCallInfo.values()) {
            if (holdInfo.id != primaryInfo.mId) {
                holdInfo.primary = false;
            }
        }
        //*/
    }
    //*/

    //*/ freeme.liqiang, 20180313. for FreemeOS redesign conference
    showConferenceCall(mFreemeCallInfo.size() == 1
            && primaryInfo.mIsConferenceCall);
    //*/

    if (primaryInfo.shouldShowLocation) {
      // Hide the avatar to make room for location
      /*/ freeme.zhaozehong, 20180303. for freemeOS, UI redesign
      contactGridManager.setAvatarHidden(true);
      //*/

      // Need to widen the contact grid to fit location information
      View contactGridView = getView().findViewById(R.id.incall_contact_grid);
      ViewGroup.LayoutParams params = contactGridView.getLayoutParams();
      if (params instanceof ViewGroup.MarginLayoutParams) {
        ((ViewGroup.MarginLayoutParams) params).setMarginStart(0);
        ((ViewGroup.MarginLayoutParams) params).setMarginEnd(0);
      }
      contactGridView.setLayoutParams(params);

      // Need to let the dialpad move up a little further when location info is being shown
      View dialpadView = getView().findViewById(R.id.incall_dialpad_container);
      params = dialpadView.getLayoutParams();
      if (params instanceof RelativeLayout.LayoutParams) {
        ((RelativeLayout.LayoutParams) params).removeRule(RelativeLayout.BELOW);
      }
      dialpadView.setLayoutParams(params);
    }
  }

  private void setAdapterMedia(MultimediaData multimediaData) {
    if (adapter == null) {
      adapter = new InCallPagerAdapter(getChildFragmentManager(), multimediaData);
      pager.setAdapter(adapter);
    } else {
      adapter.setAttachments(multimediaData);
    }

    if (adapter.getCount() > 1 && getResources().getInteger(R.integer.incall_num_rows) > 1) {
      paginator.setVisibility(View.VISIBLE);
      paginator.setupWithViewPager(pager);
      pager.setSwipingLocked(false);
      if (!stateRestored) {
        handler.postDelayed(pagerRunnable, 4_000);
      } else {
        pager.setCurrentItem(adapter.getButtonGridPosition(), false /* animateScroll */);
      }
    } else {
      paginator.setVisibility(View.GONE);
    }
  }

  @Override
  public void setSecondary(@NonNull SecondaryInfo secondaryInfo) {
    LogUtil.d("InCallFragment.setSecondary", secondaryInfo.toString());
    /// M: Need inset secondary view after sim account dialog show @{
    if (mNeedInsetSecondaryView) {
      getActivity().getWindow().getDecorView().requestApplyInsets();
      mNeedInsetSecondaryView = false;
      LogUtil.i("InCallFragment.setSecondary", "requestApplyInsets");
    }
    /// @}
    /// M: Enable switch to secondary button when primary call active @{
    if (secondaryInfo.shouldShow) {
      mNeedSetSecondaryButton = true;
    } else {
      mNeedSetSecondaryButton = false;
      //*/ freeme.zhaozehong, 20180507. for freemeOS, UI redesign
      boolean isNeedUpdateBtnStatus = false;
      //*/
      /// M: ALPS03836283, Increase call performace.
      /// Do not update Button state every time. @{
      boolean isAllowed = getButtonController(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY)
                            .isAllowed();
      if (isAllowed) {
        getButtonController(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY)
            .setEnabled(secondaryInfo.shouldShow);
        getButtonController(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY)
            .setAllowed(secondaryInfo.shouldShow);
        /*/ freeme.zhaozehong, 20180507. for freemeOS, UI redesign
        updateButtonStates();
        /*/
        isNeedUpdateBtnStatus = true;
        //*/
      }
      /// @}

      //*/ freeme.zhaozehong, 20180507. for freemeOS, UI redesign
      // 1. do not show BUTTON_HOLD during multiple calls.
      // 2. BUTTON_HOLD and BUTTON_SWITCH_TO_SECONDARY cannot be displayed at the same time.
      boolean isHoldAllowed = getButtonController(InCallButtonIds.BUTTON_HOLD).isAllowed();
      boolean afterHoldStatus = !secondaryInfo.shouldShow;
      if (isHoldAllowed != afterHoldStatus) {
          getButtonController(InCallButtonIds.BUTTON_HOLD).setAllowed(afterHoldStatus);
          isNeedUpdateBtnStatus = true;
      }

      if (isNeedUpdateBtnStatus) {
          updateButtonStates();
      }
      //*/
    }
    /// @}

    if (!isAdded()) {
      savedSecondaryInfo = secondaryInfo;
      return;
    }
    savedSecondaryInfo = null;
    //*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
    if (!TextUtils.isEmpty(secondaryInfo.id)) {
        FreemeOnHoldInfo info = new FreemeOnHoldInfo(secondaryInfo.id, secondaryInfo.name,
                secondaryInfo.number, secondaryInfo.nameIsNumber, secondaryInfo.label,
                secondaryInfo.lookupKey, secondaryInfo.isConference);
        info.primary = false;
        if (mFreemeCallInfo.containsKey(secondaryInfo.id)) {
            mFreemeCallInfo.get(secondaryInfo.id).copyData(info);
        } else {
            if (!TextUtils.isEmpty(mConferenceCallId)) {
                mFreemeCallInfo.remove(mConferenceCallId);
            }
            mFreemeCallInfo.put(secondaryInfo.id, info);
        }
    }
    //*/
    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
    Fragment oldBanner = getChildFragmentManager().findFragmentById(R.id.incall_on_hold_banner);
    if (secondaryInfo.shouldShow) {
      /*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
      transaction.replace(R.id.incall_on_hold_banner, OnHoldFragment.newInstance(secondaryInfo));
      /*/
      if (oldBanner != null && oldBanner instanceof FreemeOnHoldFragment) {
        ((FreemeOnHoldFragment) oldBanner).updateCallerInfo(mFreemeCallInfo);
      } else {
        FreemeOnHoldFragment fragment = FreemeOnHoldFragment.newInstance(mFreemeCallInfo);
        transaction.replace(R.id.incall_on_hold_banner, fragment);
      }
      showOrHidePrimaryCallInfo(true, false, false);
      //*/
    } else {
      //*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
      if (!mIsConference) {
        removeOtherCallInfo();
        boolean isDialpadShown = ((InCallActivity) getActivity()).isDialpadVisible();
        showOrHidePrimaryCallInfo(false, !isDialpadShown, false);
      }
      //*/
      /*/ freeme.liqiang, 20180313. for freemeOS redesign conference
      if (oldBanner != null) {
      /*/
      if (oldBanner != null && oldBanner instanceof FreemeOnHoldFragment) {
      //*/
        transaction.remove(oldBanner);
      }
    }
    transaction.setCustomAnimations(R.anim.abc_slide_in_top, R.anim.abc_slide_out_top);
    transaction.commitAllowingStateLoss();
  }

  @Override
  public void setCallState(@NonNull PrimaryCallState primaryCallState) {
    LogUtil.i("InCallFragment.setCallState", primaryCallState.toString() +
              ", Phonetype:" + phoneType);
    contactGridManager.setCallState(primaryCallState);
    //*/ freeme.zhaozehong, 20180305. for freemeOS, UI redesign
    if (mFreemeCallInfo.containsKey(primaryCallState.mId)) {
      FreemeOnHoldInfo info = mFreemeCallInfo.get(primaryCallState.mId);
      info.connectTimeMillis = primaryCallState.connectTimeMillis;
      info.state = primaryCallState.state;
      info.isConference = primaryCallState.mIsConferenceCall;
    }
    if (isAdded()) { // fragment maybe not been attached
      Fragment oldBanner = getChildFragmentManager().findFragmentById(R.id.incall_on_hold_banner);
      if (oldBanner != null && oldBanner instanceof FreemeOnHoldFragment) {
        ((FreemeOnHoldFragment) oldBanner).updateCallerInfo(mFreemeCallInfo);
      } else if (oldBanner != null && oldBanner instanceof FreemeConferenceFragment) {
        ((FreemeConferenceFragment) oldBanner).updateChronometer(primaryCallState.connectTimeMillis);
      }
    }
    //*/
    //*/ freeme.zhaozehong, 20180523. for freemeOS, update Conference Call UI
    showConferenceCall(mFreemeCallInfo.size() == 1
            && primaryCallState.mIsConferenceCall);
    //*/
    /// M: Get voice network type and phone type according to call account @{
    updateInfoForButtonChooser(primaryCallState.state);
    /// @}
    /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    buttonChooser =
        ButtonChooserFactory.newButtonChooser(voiceNetworkType, primaryCallState.isWifi, phoneType);
    /*/
    buttonChooser = FreemeButtonChooserFactory.newButtonChooser(voiceNetworkType,
            primaryCallState.isWifi, phoneType);
    //*/
    /// M: Enable switch to secondary button when primary call active @{
    setSwtichToSecondaryButton(primaryCallState.state);
    /// @}

    /// M: ALPS03809577. Improve volte mo call performance. @{
    if (!DialerCall.State.isConnecting(primaryCallState.state)) {
      updateButtonStates();
    }
    /// @}
  }

  @Override
  public void setEndCallButtonEnabled(boolean enabled, boolean animate) {
    if (endCallButton != null) {
      endCallButton.setEnabled(enabled);
    }
  }

  @Override
  public void showManageConferenceCallButton(boolean visible) {
    /// M: ALPS03809577. Improve volte mo call performance. @{
    LogUtil.d("InCallFragment.showManageConferenceCallButton", "visible = " + visible);
    if (isManageConferenceVisible() != visible) {
      getButtonController(InCallButtonIds.BUTTON_MANAGE_VOICE_CONFERENCE).setAllowed(visible);
      getButtonController(InCallButtonIds.BUTTON_MANAGE_VOICE_CONFERENCE).setEnabled(visible);
      updateButtonStates();
    }
    /// @}
  }

  @Override
  public boolean isManageConferenceVisible() {
    return getButtonController(InCallButtonIds.BUTTON_MANAGE_VOICE_CONFERENCE).isAllowed();
  }

  @Override
  public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
    contactGridManager.dispatchPopulateAccessibilityEvent(event);
  }

  @Override
  public void showNoteSentToast() {
    LogUtil.i("InCallFragment.showNoteSentToast", null);
    Toast.makeText(getContext(), R.string.incall_note_sent, Toast.LENGTH_LONG).show();
  }

  @Override
  public void updateInCallScreenColors() {}

  @Override
  public void onInCallScreenDialpadVisibilityChange(boolean isShowing) {
    LogUtil.i("InCallFragment.onInCallScreenDialpadVisibilityChange", "isShowing: " + isShowing);
    // Take note that the dialpad button isShowing
    getButtonController(InCallButtonIds.BUTTON_DIALPAD).setChecked(isShowing);

    // This check is needed because there is a race condition where we attempt to update
    // ButtonGridFragment before it is ready, so we check whether it is ready first and once it is
    // ready, #onButtonGridCreated will mark the dialpad button as isShowing.
    if (inCallButtonGridFragment != null) {
      // Update the Android Button's state to isShowing.
      inCallButtonGridFragment.onInCallScreenDialpadVisibilityChange(isShowing);
    }
  }

  @Override
  public int getAnswerAndDialpadContainerResourceId() {
    return R.id.incall_dialpad_container;
  }

  @Override
  public Fragment getInCallScreenFragment() {
    return this;
  }

  @Override
  public void showButton(@InCallButtonIds int buttonId, boolean show) {
    LogUtil.v(
        "InCallFragment.showButton",
        "buttionId: %s, show: %b",
        InCallButtonIdsExtension.toString(buttonId),
        show);
    if (isSupportedButton(buttonId)) {
      getButtonController(buttonId).setAllowed(show);
      if (buttonId == InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO && show) {
        Logger.get(getContext())
            .logImpression(DialerImpression.Type.UPGRADE_TO_VIDEO_CALL_BUTTON_SHOWN);
      }
    }
  }

  @Override
  public void enableButton(@InCallButtonIds int buttonId, boolean enable) {
    LogUtil.v(
        "InCallFragment.enableButton",
        "buttonId: %s, enable: %b",
        InCallButtonIdsExtension.toString(buttonId),
        enable);
    if (isSupportedButton(buttonId)) {
      getButtonController(buttonId).setEnabled(enable);
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    LogUtil.v("InCallFragment.setEnabled", "enabled: " + enabled);
    for (ButtonController buttonController : buttonControllers) {
      buttonController.setEnabled(enabled);
    }
  }

  @Override
  public void setHold(boolean value) {
    getButtonController(InCallButtonIds.BUTTON_HOLD).setChecked(value);
  }

  @Override
  public void setCameraSwitched(boolean isBackFacingCamera) {}

  @Override
  public void setVideoPaused(boolean isPaused) {}

  @Override
  public void setAudioState(CallAudioState audioState) {
    LogUtil.i("InCallFragment.setAudioState", "audioState: " + audioState);
    ((SpeakerButtonController) getButtonController(InCallButtonIds.BUTTON_AUDIO))
        .setAudioState(audioState);
    getButtonController(InCallButtonIds.BUTTON_MUTE).setChecked(audioState.isMuted());
  }

  @Override
  public void updateButtonStates() {
    // When the incall screen is ready, this method is called from #setSecondary, even though the
    // incall button ui is not ready yet. This method is called again once the incall button ui is
    // ready though, so this operation is safe and will be executed asap.
    if (inCallButtonGridFragment == null) {
      return;
    }
    int numVisibleButtons =
        inCallButtonGridFragment.updateButtonStates(
            buttonControllers, buttonChooser, voiceNetworkType, phoneType);

    int visibility = numVisibleButtons == 0 ? View.GONE : View.VISIBLE;
    pager.setVisibility(visibility);
    if (adapter != null
        && adapter.getCount() > 1
        && getResources().getInteger(R.integer.incall_num_rows) > 1) {
      paginator.setVisibility(View.VISIBLE);
      pager.setSwipingLocked(false);
    } else {
      paginator.setVisibility(View.GONE);
      if (adapter != null) {
        pager.setSwipingLocked(true);
        pager.setCurrentItem(adapter.getButtonGridPosition());
      }
    }
  }

  @Override
  public void updateInCallButtonUiColors() {}

  @Override
  public Fragment getInCallButtonUiFragment() {
    return this;
  }

  @Override
  public void showAudioRouteSelector() {
    AudioRouteSelectorDialogFragment.newInstance(inCallButtonUiDelegate.getCurrentAudioState())
        .show(getChildFragmentManager(), null);
  }

  @Override
  public void onAudioRouteSelected(int audioRoute) {
    inCallButtonUiDelegate.setAudioRoute(audioRoute);
  }

  @Override
  public void onAudioRouteSelectorDismiss() {}

  @NonNull
  @Override
  public ButtonController getButtonController(@InCallButtonIds int id) {
    for (ButtonController buttonController : buttonControllers) {
      if (buttonController.getInCallButtonId() == id) {
        return buttonController;
      }
    }
    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    LogUtil.i("InCallFragment.getButtonController", "button id = "+id);
    //*/
    Assert.fail();
    return null;
  }

  /*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
  @Override
  public void onButtonGridCreated(InCallButtonGridFragment inCallButtonGridFragment) {
    LogUtil.i("InCallFragment.onButtonGridCreated", "InCallUiReady");
    this.inCallButtonGridFragment = inCallButtonGridFragment;
    inCallButtonUiDelegate.onInCallButtonUiReady(this);
    updateButtonStates();
  }
  /*/
  @Override
  public void onButtonGridCreated(FreemeInCallButtonGridFragment inCallButtonGridFragment) {
    LogUtil.i("InCallFragment.onButtonGridCreated", "InCallUiReady");
    this.inCallButtonGridFragment = inCallButtonGridFragment;
    inCallButtonUiDelegate.onInCallButtonUiReady(this);
    if (speakerButton != null) {
      getButtonController(InCallButtonIds.BUTTON_AUDIO).setButton(speakerButton);
    }
    if (dialpadButton != null) {
      getButtonController(InCallButtonIds.BUTTON_DIALPAD).setButton(dialpadButton);
    }
    updateButtonStates();
  }
  //*/

  @Override
  public void onButtonGridDestroyed() {
    LogUtil.i("InCallFragment.onButtonGridCreated", "InCallUiUnready");
    inCallButtonUiDelegate.onInCallButtonUiUnready();
    this.inCallButtonGridFragment = null;
  }

  @Override
  public boolean isShowingLocationUi() {
    Fragment fragment = getLocationFragment();
    return fragment != null && fragment.isVisible();
  }

  @Override
  public void showLocationUi(@Nullable Fragment locationUi) {
    boolean isVisible = isShowingLocationUi();
    if (locationUi != null && !isVisible) {
      // Show the location fragment.
      getChildFragmentManager()
          .beginTransaction()
          .replace(R.id.incall_location_holder, locationUi)
          .commitAllowingStateLoss();
    } else if (locationUi == null && isVisible) {
      // Hide the location fragment
      getChildFragmentManager()
          .beginTransaction()
          .remove(getLocationFragment())
          .commitAllowingStateLoss();
    }
  }

  @Override
  public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
    super.onMultiWindowModeChanged(isInMultiWindowMode);
    if (isInMultiWindowMode == isShowingLocationUi()) {
      LogUtil.i("InCallFragment.onMultiWindowModeChanged", "hide = " + isInMultiWindowMode);
      // Need to show or hide location
      showLocationUi(isInMultiWindowMode ? null : getLocationFragment());
    }
  }

  private Fragment getLocationFragment() {
    return getChildFragmentManager().findFragmentById(R.id.incall_location_holder);
  }

  /// M: ----------------------------MediaTek feature --------------------------
  private boolean mNeedUpdateButtonInfo = true;
  private boolean mNeedSetSecondaryButton = false;
  private boolean mNeedInsetSecondaryView = true;
  private int mCallProperties = 0;

  private void updateInfoForButtonChooser(int state) {
    DialerCall call = CallList.getInstance().getFirstCallWithState(state);
    if (call == null) {
      LogUtil.i("InCallFragment.updateInfoForButtonChooser", "no call, state: " + state);
      return;
    }
    if (call.getDetails() == null) {
      LogUtil.i("InCallFragment.updateInfoForButtonChooser", "no call details, state: " + state);
      return;
    }
    LogUtil.d("InCallFragment.updateInfoForButtonChooser", String.valueOf(call));
    /// M: ALPS03567937. Do not get network type and phone type in connecting status
    // dut to performance. @{
    if (call != null && !DialerCall.State.isConnecting(state)) {
      voiceNetworkType = call.getVoiceNetworkTypeForCall();
      phoneType = call.getPhoneTypeByNetworkType(voiceNetworkType);
    }
    /// @}
  }

  private void setSwtichToSecondaryButton(int state) {
    if (mNeedSetSecondaryButton && state == DialerCall.State.ACTIVE) {
      LogUtil.i("InCallFragment.setSwtichToSecondaryButton", "setAllowed");
      getButtonController(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY).setAllowed(true);
      //*/ freeme.zhaozehong, 20180306. for freemeOS, UI redesign
      getButtonController(InCallButtonIds.BUTTON_HOLD).setAllowed(false);
      //*/
      mNeedSetSecondaryButton = false;
    }
  }

  @Override
  public void updateDeclineTimer() {
  }

  /**
   * M: [Voice Record]
   */
  @Override
  public void updateRecordStateUi(boolean isRecording) {
      Log.d(this, "[updateRecordStateUi]... " + isRecording);

      DialerCall ringCall = null;
      int ringCallState = DialerCall.State.INVALID;
      ringCall = CallList.getInstance().getIncomingCall();
      if (null != ringCall) {
          ringCallState = ringCall.getState();
      }
      if (isRecording && (ringCallState != DialerCall.State.INCOMING)
              && (ringCallState != DialerCall.State.CALL_WAITING)) {
          updateVoiceRecordIcon(true);
      } else {
          updateVoiceRecordIcon(false);
      }
      getButtonController(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD).setChecked(isRecording);
  }

  /// M: [Voice Record] recording indication icon @{
  private ImageView mVoiceRecorderIcon;

  private void initVoiceRecorderIcon(View view) {
      /*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
      mVoiceRecorderIcon = (ImageView) view.findViewById(R.id.voiceRecorderIcon);
      mVoiceRecorderIcon.setImageResource(R.drawable.voice_record_indicator);
      mVoiceRecorderIcon.setVisibility(View.INVISIBLE);
      //*/
  }

  public void updateVoiceRecordIcon(boolean show) {
      /*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
      mVoiceRecorderIcon.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
      AnimationDrawable ad = (AnimationDrawable) mVoiceRecorderIcon.getDrawable();
      if (ad != null) {
          if (show && !ad.isRunning()) {
              ad.start();
          } else if (!show && ad.isRunning()) {
              ad.stop();
          }
      }
      //*/
  }
  /// @}

    //*/ freeme.zhaozehong, 20180301. for freemeOS, UI redesign
    private CheckableLabeledButton dialpadButton;
    private CheckableLabeledButton speakerButton;

    private Animation callbuttonSlideInAnimation;
    private Animation callbuttonSlideOutAnimation;

    private void initCallButtonAnimate() {
        boolean isLandscape = getContext().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        boolean isRtl = ViewUtil.isRtl();

        if (isLandscape) {
            callbuttonSlideInAnimation =
                    AnimationUtils.loadAnimation(getContext(),
                            isRtl ? R.anim.freeme_incall_button_slide_in_left : R.anim.freeme_incall_button_slide_in_right);
            callbuttonSlideOutAnimation =
                    AnimationUtils.loadAnimation(getContext(),
                            isRtl ? R.anim.freeme_incall_button_slide_out_left : R.anim.freeme_incall_button_slide_out_right);
        } else {
            callbuttonSlideInAnimation =
                    AnimationUtils.loadAnimation(getContext(), R.anim.freeme_incall_button_slide_in_bottom);
            callbuttonSlideOutAnimation =
                    AnimationUtils.loadAnimation(getContext(), R.anim.freeme_incall_button_slide_out_bottom);
        }

        callbuttonSlideInAnimation.setInterpolator(AnimUtils.EASE_IN);
        callbuttonSlideOutAnimation.setInterpolator(AnimUtils.EASE_OUT);
        callbuttonSlideOutAnimation.setFillAfter(true);
    }

    public void performShowOrHideCallButtonPager(boolean show, boolean animate) {
        if (pager == null) {
            return;
        }
        View view = pager.findViewById(R.id.grid);
        if (view == null) {// view maybe not added
            return;
        }
        if (animate) {
            if (callbuttonSlideInAnimation == null || callbuttonSlideOutAnimation == null) {
                initCallButtonAnimate();
            }
            view.setVisibility(View.VISIBLE);
            view.startAnimation(show ? callbuttonSlideInAnimation : callbuttonSlideOutAnimation);
        } else {
            if (show) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }
    }

    public void performShowOrHideCallerInfo(boolean isDialpadShown, boolean show, boolean animate) {
        contactGridManager.performShowOrHideCallerInfo(mFreemeCallInfo.size() > 1 || mIsConference,
                isDialpadShown, show, animate);
    }

    private LinkedHashMap<String, FreemeOnHoldInfo> mFreemeCallInfo = new LinkedHashMap<>();

    private void removeOtherCallInfo(){
        FreemeOnHoldInfo primary = null;
        for (FreemeOnHoldInfo info : mFreemeCallInfo.values()) {
            if (info.primary) {
                primary = info;
                break;
            }
        }
        mFreemeCallInfo.clear();
        if (primary != null) {
            mFreemeCallInfo.put(primary.id, primary);
        }
    }

    private void showOrHidePrimaryCallInfo ( boolean isMultiCall, boolean show, boolean animate){
        boolean isDialpadShown = ((InCallActivity) getActivity()).isDialpadVisible();
        contactGridManager.performShowOrHideCallerInfo(isMultiCall, isDialpadShown, show, animate);
    }

    boolean mIsConference;
    private void showConferenceCall(boolean isShowCallConference) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        Fragment oldBanner = getChildFragmentManager().findFragmentById(R.id.incall_on_hold_banner);
        boolean isAllowed = getButtonController(InCallButtonIds.BUTTON_MANAGE_VOICE_CONFERENCE).isAllowed();
        if (isAllowed && isShowCallConference) {
            if (oldBanner == null || !(oldBanner instanceof FreemeConferenceFragment)) {
                if (oldBanner != null) {
                    transaction.remove(oldBanner);
                }
                FreemeConferenceFragment fragment = FreemeConferenceFragment.newInstance();
                transaction.replace(R.id.incall_on_hold_banner, fragment);
            }
            mIsConference = true;
            showOrHidePrimaryCallInfo(true, false, false);
        } else {
            if (oldBanner != null && oldBanner instanceof FreemeConferenceFragment) {
                mIsConference = false;
                transaction.remove(oldBanner);
            }
        }
        transaction.setCustomAnimations(R.anim.abc_slide_in_top, R.anim.abc_slide_out_top);
        transaction.commitAllowingStateLoss();
    }
    //*/

    //*/ freeme.zhaozehong, 20180704. for freemeOS, save Conference Call Id
    private String mConferenceCallId;
    //*/

    //*/ freeme.zhangjunhe,20180913. for call record
    @Override
    public void onDestroy() {
        super.onDestroy();
        ButtonController controller = getButtonController(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD);
        if (controller instanceof ButtonController.SwitchVoiceRecordButtonController) {
            ((ButtonController.SwitchVoiceRecordButtonController) controller).showRecordTime(0);
        }
    }
    //*/
}
