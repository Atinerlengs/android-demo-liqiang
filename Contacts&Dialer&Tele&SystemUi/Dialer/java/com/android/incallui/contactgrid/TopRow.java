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

package com.android.incallui.contactgrid;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import com.android.dialer.common.Assert;
import com.android.incallui.call.DialerCall.State;
import com.android.incallui.incall.protocol.PrimaryCallState;
import com.android.incallui.incall.protocol.PrimaryInfo;
import com.android.incallui.videotech.utils.SessionModificationState;
import com.android.incallui.videotech.utils.VideoUtils;
import com.mediatek.incallui.volte.InCallUIVolteUtils;

/**
 * Gets the content of the top row. For example:
 *
 * <ul>
 *   <li>Captain Holt ON HOLD
 *   <li>Calling...
 *   <li>[Wi-Fi icon] Calling via Starbucks Wi-Fi
 *   <li>[Wi-Fi icon] Starbucks Wi-Fi
 *   <li>Call from
 * </ul>
 */
public class TopRow {

  /** Content of the top row. */
  public static class Info {

    @Nullable public final CharSequence label;
    @Nullable public final Drawable icon;
    public final boolean labelIsSingleLine;

    public Info(@Nullable CharSequence label, @Nullable Drawable icon, boolean labelIsSingleLine) {
      this.label = label;
      this.icon = icon;
      this.labelIsSingleLine = labelIsSingleLine;
    }
  }

  private TopRow() {}

  public static Info getInfo(Context context, PrimaryCallState state, PrimaryInfo primaryInfo) {
    CharSequence label = null;
    Drawable icon = state.connectionIcon;
    boolean labelIsSingleLine = true;

    if (state.isWifi && icon == null) {
      icon = context.getDrawable(R.drawable.quantum_ic_network_wifi_vd_theme_24);
    }

    if (state.state == State.INCOMING || state.state == State.CALL_WAITING) {
      // Call from
      // [Wi-Fi icon] Video call from
      // Hey Jake, pick up!
      if (!TextUtils.isEmpty(state.callSubject)) {
        label = state.callSubject;
        labelIsSingleLine = false;
      /// M: [VoLTE conference]incoming volte conference @{
      } else if (InCallUIVolteUtils.isIncomingVolteConferenceCall()) {
        label = context.getString(R.string.card_title_incoming_conference);
      /// @}
      } else {
        label = getLabelForIncoming(context, state);
        /*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
        // Show phone number if it's not displayed in name (center row) or location field (bottom
        // row).
        if (shouldShowNumber(primaryInfo)) {
          label = TextUtils.concat(label, " ", spanDisplayNumber(primaryInfo.number));
        }
        //*/
      }
    } else if (VideoUtils.hasSentVideoUpgradeRequest(state.sessionModificationState)
        || VideoUtils.hasReceivedVideoUpgradeRequest(state.sessionModificationState)) {
      label = getLabelForVideoRequest(context, state);
    } else if (state.state == State.PULLING) {
      label = context.getString(R.string.incall_transferring);
    } else if (state.state == State.DIALING || state.state == State.CONNECTING) {
      // [Wi-Fi icon] Calling via Google Guest
      // Calling...
      label = getLabelForDialing(context, state);
    } else if (state.state == State.ACTIVE && state.isRemotelyHeld) {
      label = context.getString(R.string.incall_remotely_held);
    } else if (state.state == State.ACTIVE && shouldShowNumber(primaryInfo)) {
      /*/ freeme.zhaozehong, 20180309. for freemeOS, UI redesign
      label = spanDisplayNumber(primaryInfo.number);
      //*/
    //*/ freeme.zhaozehong, 20180303. for freemeOS, UI redesign
    } else if (state.state == State.DISCONNECTING
            || state.state == State.DISCONNECTED
            || state.state == State.IDLE) {
      label = context.getString(R.string.incall_call_ended);
    //*/
    } else {
      // Video calling...
      // [Wi-Fi icon] Starbucks Wi-Fi
      label = getConnectionLabel(state);
    }

    return new Info(label, icon, labelIsSingleLine);
  }

  private static CharSequence spanDisplayNumber(String displayNumber) {
    return PhoneNumberUtils.createTtsSpannable(
        BidiFormatter.getInstance().unicodeWrap(displayNumber, TextDirectionHeuristics.LTR));
  }

  private static boolean shouldShowNumber(PrimaryInfo primaryInfo) {
    if (primaryInfo.nameIsNumber) {
      return false;
    }
    if (primaryInfo.location == null) {
      return false;
    }
    if (TextUtils.isEmpty(primaryInfo.number)) {
      return false;
    }
    return true;
  }

  private static CharSequence getLabelForIncoming(Context context, PrimaryCallState state) {
    if (state.isVideoCall) {
      /// M: CTA show phone account for incoming video call.@{
      if (!TextUtils.isEmpty(state.connectionLabel) && !state.isWifi) {
        return context.getString(R.string.contact_grid_incoming_video_via_template,
               state.connectionLabel);
      } else {
        return getLabelForIncomingVideo(context, state.sessionModificationState, state.isWifi);
      }
      /// @}
    } else if (state.isWifi && !TextUtils.isEmpty(state.connectionLabel)) {
      return state.connectionLabel;
    } else if (isAccount(state)) {
      return context.getString(R.string.contact_grid_incoming_via_template, state.connectionLabel);
    /*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
    } else if (state.isWorkCall) {
      return context.getString(R.string.contact_grid_incoming_work_call);
    } else {
      return context.getString(R.string.contact_grid_incoming_voice_call);
    }
    /*/
    } else if (state.isWorkCall) {
      return context.getString(R.string.notification_incoming_work_call);
    } else {
      return context.getString(R.string.notification_incoming_call);
    }
    //*/
  }

  private static CharSequence getLabelForIncomingVideo(
      Context context, @SessionModificationState int sessionModificationState, boolean isWifi) {
    /*/ freeme.zhaozehong, 20180308. for freemeOS, UI redesign
    if (sessionModificationState == SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
      if (isWifi) {
        return context.getString(R.string.contact_grid_incoming_wifi_video_request);
      } else {
        return context.getString(R.string.contact_grid_incoming_video_request);
      }
    } else {
      if (isWifi) {
        return context.getString(R.string.contact_grid_incoming_wifi_video_call);
      } else {
        return context.getString(R.string.contact_grid_incoming_video_call);
      }
    }
    /*/
    if (sessionModificationState == SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
      if (isWifi) {
        return context.getString(R.string.freeme_incall_incoming_video_upgrade_from_wifi);
      } else {
        return context.getString(R.string.freeme_incall_incoming_video_upgrade);
      }
    } else {
      if (isWifi) {
        return context.getString(R.string.freeme_incall_incoming_video_from_wifi);
      } else {
        return context.getString(R.string.freeme_incall_incoming_video);
      }
    }
    //*/
  }

  private static CharSequence getLabelForDialing(Context context, PrimaryCallState state) {
    if (!TextUtils.isEmpty(state.connectionLabel) && !state.isWifi) {
      return context.getString(R.string.incall_calling_via_template, state.connectionLabel);
    } else {
      if (state.isVideoCall) {
        if (state.isWifi) {
          return context.getString(R.string.incall_wifi_video_call_requesting);
        } else {
          return context.getString(R.string.incall_video_call_requesting);
        }
      }
      return context.getString(R.string.incall_connecting);
    }
  }

  private static CharSequence getConnectionLabel(PrimaryCallState state) {
    if (!TextUtils.isEmpty(state.connectionLabel)
        && (isAccount(state) || state.isWifi || state.isConference)) {
      // We normally don't show a "call state label" at all when active
      // (but we can use the call state label to display the provider name).
      return state.connectionLabel;
    } else {
      return null;
    }
  }

  private static CharSequence getLabelForVideoRequest(Context context, PrimaryCallState state) {
    switch (state.sessionModificationState) {
      case SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE:
        return context.getString(R.string.incall_video_call_requesting);
      case SessionModificationState.REQUEST_FAILED:
      case SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_FAILED:
        return context.getString(R.string.incall_video_call_request_failed);
      case SessionModificationState.REQUEST_REJECTED:
        return context.getString(R.string.incall_video_call_request_rejected);
      case SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_TIMED_OUT:
        return context.getString(R.string.incall_video_call_request_timed_out);
      case SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST:
        return getLabelForIncomingVideo(context, state.sessionModificationState, state.isWifi);
      /// M: Cancel upgrade. @{
      case SessionModificationState.WAITING_FOR_CANCEL_UPGRADE_RESPONSE:
        return context.getString(R.string.card_title_cancel_upgrade_requesting);
      /// @}
      case SessionModificationState.NO_REQUEST:
      default:
        Assert.fail();
        return null;
    }
  }

  private static boolean isAccount(PrimaryCallState state) {
    return !TextUtils.isEmpty(state.connectionLabel) && TextUtils.isEmpty(state.gatewayNumber);
  }
}
