/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;
import com.android.dialer.blocking.FilteredNumberAsyncQueryHandler;
import com.android.incallui.audiomode.AudioModeProvider;
import com.android.incallui.call.CallList;
import com.android.incallui.call.ExternalCallList;
import com.android.incallui.call.TelecomAdapter;
import com.mediatek.incallui.plugin.ExtensionManager;
import com.mediatek.incallui.utils.InCallUtils;

/**
 * Used to receive updates about calls from the Telecom component. This service is bound to Telecom
 * while there exist calls which potentially require UI. This includes ringing (incoming), dialing
 * (outgoing), and active calls. When the last call is disconnected, Telecom will unbind to the
 * service triggering InCallActivity (via CallList) to finish soon after.
 */
public class InCallServiceImpl extends InCallService {

  private ReturnToCallController returnToCallController;

  @Override
  public void onCallAudioStateChanged(CallAudioState audioState) {
    AudioModeProvider.getInstance().onAudioStateChanged(audioState);
  }

  @Override
  public void onBringToForeground(boolean showDialpad) {
    InCallPresenter.getInstance().onBringToForeground(showDialpad);
  }

  @Override
  public void onCallAdded(Call call) {
    /**
     * M: When in upgrade progress or in requesting for VILTE call,
     * It should reject the incoming call and disconnect other calls,
     * except the emergency call.
     * @{
     */
    if ((CallList.getInstance().getVideoUpgradeRequestCall() != null ||
            CallList.getInstance().getSendingVideoUpgradeRequestCall() != null ||
            /// M: When is cancel upgrade progress,we can't add another call in calllist. @{
            CallList.getInstance().getSendingCancelUpgradeRequestCall() != null)
            ///@}
            && !isEmergency(call)) {
        if (call.getState() == Call.STATE_RINGING) {
            call.reject(false, null);
        } else {
            call.disconnect();
        }
        Log.d(this, "[Debug][CC][InCallUI][OP][Hangup][null][null]" +
        "auto disconnect call while upgrading to video");
        InCallUtils.showOutgoingFailMsg(getApplicationContext(), call);
    } else {
        InCallPresenter.getInstance().onCallAdded(call);
    }
    /** @} */
  }

  @Override
  public void onCallRemoved(Call call) {
    InCallPresenter.getInstance().onCallRemoved(call);
  }

  @Override
  public void onCanAddCallChanged(boolean canAddCall) {
    InCallPresenter.getInstance().onCanAddCallChanged(canAddCall);
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(this, "onBind");
    final Context context = getApplicationContext();
    /// M: [Plugin Host] register context @{
    ExtensionManager.registerApplicationContext(context);
    /// @}
    final ContactInfoCache contactInfoCache = ContactInfoCache.getInstance(context);
    InCallPresenter.getInstance()
        .setUp(
            context,
            CallList.getInstance(),
            new ExternalCallList(),
            new StatusBarNotifier(context, contactInfoCache),
            new ExternalCallNotifier(context, contactInfoCache),
            contactInfoCache,
            new ProximitySensor(
                context, AudioModeProvider.getInstance(), new AccelerometerListener(context)),
            new FilteredNumberAsyncQueryHandler(context));
    InCallPresenter.getInstance().onServiceBind();
    InCallPresenter.getInstance().maybeStartRevealAnimation(intent);
    TelecomAdapter.getInstance().setInCallService(this);
    if (ReturnToCallController.isEnabled(this)) {
      returnToCallController = new ReturnToCallController(this);
    }
    /// M: for pct vilte auto test on instrument @{
    ExtensionManager.getVilteAutoTestHelperExt().registerReceiver(context,
            InCallPresenter.getInstance(),TelecomAdapter.getInstance());
    /// @}

    return super.onBind(intent);
  }

  @Override
  public boolean onUnbind(Intent intent) {
    super.onUnbind(intent);

    InCallPresenter.getInstance().onServiceUnbind();
    tearDown();

    return false;
  }

  private void tearDown() {
    Log.v(this, "tearDown");
    // Tear down the InCall system
    TelecomAdapter.getInstance().clearInCallService();
    InCallPresenter.getInstance().tearDown();
    if (returnToCallController != null) {
      returnToCallController.tearDown();
      returnToCallController = null;
    }
    /// M: for pct vilte auto test on instrument @{
    ExtensionManager.getVilteAutoTestHelperExt().unregisterReceiver();
    /// @}
  }

  /// M: ---------------- MediaTek feature -------------------
  /// M: fix CR:ALPS02696713,can not dial ECC when requesting for vilte call. @{
  private boolean isEmergency(Call call) {
      Uri handle = call.getDetails().getHandle();
      return android.telephony.PhoneNumberUtils.isEmergencyNumber(
              handle == null ? "" : handle.getSchemeSpecificPart());
  }
  /// @}
}
