/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.dialer.notification;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.BuildCompat;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Utilities for working with voicemail channels. */
@TargetApi(VERSION_CODES.O)
/* package */ final class VoicemailChannelUtils {
  private static final String GLOBAL_VOICEMAIL_CHANNEL_ID = "phone_voicemail";
  private static final String PER_ACCOUNT_VOICEMAIL_CHANNEL_ID_PREFIX = "phone_voicemail_account_";

  static Set<String> getAllChannelIds(@NonNull Context context) {
    Assert.checkArgument(BuildCompat.isAtLeastO());
    Assert.isNotNull(context);

    Set<String> result = new ArraySet<>();
    if (isSingleSimDevice(context)) {
      result.add(GLOBAL_VOICEMAIL_CHANNEL_ID);
    } else {
      for (PhoneAccountHandle handle : getAllEligableAccounts(context)) {
        result.add(getChannelIdForAccount(handle));
      }
    }
    return result;
  }

  static void createAllChannels(@NonNull Context context) {
    Assert.checkArgument(BuildCompat.isAtLeastO());
    Assert.isNotNull(context);

    if (isSingleSimDevice(context)) {
      createGlobalVoicemailChannel(context);
    } else {
      for (PhoneAccountHandle handle : getAllEligableAccounts(context)) {
        createVoicemailChannelForAccount(context, handle);
      }
    }
  }

  @NonNull
  static String getChannelId(@NonNull Context context, @Nullable PhoneAccountHandle handle) {
    Assert.checkArgument(BuildCompat.isAtLeastO());
    Assert.isNotNull(context);

    // Most devices we deal with have a single SIM slot. No need to distinguish between phone
    // accounts.
    if (isSingleSimDevice(context)) {
      return GLOBAL_VOICEMAIL_CHANNEL_ID;
    }

    // We can get a null phone account at random points (modem reboot, etc...). Gracefully degrade
    // by using the default channel.
    if (handle == null) {
      LogUtil.i(
          "VoicemailChannelUtils.getChannelId",
          "no phone account on a multi-SIM device, using default channel");
      return NotificationChannelId.DEFAULT;
    }

    // Voicemail notifications should always be associated with a SIM based phone account.
    if (!isChannelAllowedForAccount(context, handle)) {
      LogUtil.i(
          "VoicemailChannelUtils.getChannelId",
          "phone account is not for a SIM, using default channel");
      return NotificationChannelId.DEFAULT;
    }

    // Now we're in the multi-SIM case.
    String channelId = getChannelIdForAccount(handle);
    if (!doesChannelExist(context, channelId)) {
      LogUtil.i(
          "VoicemailChannelUtils.getChannelId",
          "voicemail channel not found for phone account (possible SIM swap?), creating a new one");
      createVoicemailChannelForAccount(context, handle);
    }
    return channelId;
  }

  private static boolean doesChannelExist(@NonNull Context context, @NonNull String channelId) {
    return context.getSystemService(NotificationManager.class).getNotificationChannel(channelId)
        != null;
  }

  private static String getChannelIdForAccount(@NonNull PhoneAccountHandle handle) {
    Assert.isNotNull(handle);
    return PER_ACCOUNT_VOICEMAIL_CHANNEL_ID_PREFIX + ":" + handle.getId();
  }

  /**
   * Creates a voicemail channel but doesn't associate it with a SIM. For devices with only one SIM
   * slot this is ideal because there won't be duplication in the settings UI.
   */
  private static void createGlobalVoicemailChannel(@NonNull Context context) {
    NotificationChannel channel = newChannel(context, GLOBAL_VOICEMAIL_CHANNEL_ID, null);

    TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
    PhoneAccountHandle handle =
        telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL);
    if (handle == null) {
      LogUtil.i(
          "VoicemailChannelUtils.createGlobalVoicemailChannel",
          "phone account is null, not migrating sound settings");
    } else if (!isChannelAllowedForAccount(context, handle)) {
      LogUtil.i(
          "VoicemailChannelUtils.createGlobalVoicemailChannel",
          "phone account is not eligable, not migrating sound settings");
    } else {
      migrateVoicemailSoundSettings(context, channel, handle);
    }
    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
  }

  private static List<PhoneAccountHandle> getAllEligableAccounts(@NonNull Context context) {
    List<PhoneAccountHandle> handles = new ArrayList<>();
    TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
    for (PhoneAccountHandle handle : telecomManager.getCallCapablePhoneAccounts()) {
      if (isChannelAllowedForAccount(context, handle)) {
        handles.add(handle);
      }
    }
    return handles;
  }

  private static void createVoicemailChannelForAccount(
      @NonNull Context context, @NonNull PhoneAccountHandle handle) {
    PhoneAccount phoneAccount =
        context.getSystemService(TelecomManager.class).getPhoneAccount(handle);
    /// M: fix NPE: 3608264 @{
    NotificationChannel channel;
    if (phoneAccount == null) {
        channel =
                newChannel(context, getChannelIdForAccount(handle), null);
    } else {
    /// @}
        channel =
                newChannel(context, getChannelIdForAccount(handle), phoneAccount.getLabel());
    }
    migrateVoicemailSoundSettings(context, channel, handle);
    context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
  }

  private static void migrateVoicemailSoundSettings(
      @NonNull Context context,
      @NonNull NotificationChannel channel,
      @NonNull PhoneAccountHandle handle) {
    TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
    channel.enableVibration(telephonyManager.isVoicemailVibrationEnabled(handle));
    channel.setSound(
        telephonyManager.getVoicemailRingtoneUri(handle),
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
  }

  private static boolean isChannelAllowedForAccount(
      @NonNull Context context, @NonNull PhoneAccountHandle handle) {
    PhoneAccount phoneAccount =
        context.getSystemService(TelecomManager.class).getPhoneAccount(handle);
    if (phoneAccount == null) {
      return false;
    }
    if (!phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
      return false;
    }
    return true;
  }

  private static NotificationChannel newChannel(
      @NonNull Context context, @NonNull String channelId, @Nullable CharSequence nameSuffix) {
    CharSequence name = context.getText(R.string.notification_channel_voicemail);
    // TODO(sail): Use a string resource template after v10.
    if (!TextUtils.isEmpty(nameSuffix)) {
      name = TextUtils.concat(name, ": ", nameSuffix);
    }

    NotificationChannel channel =
        new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT);
    channel.setShowBadge(true);
    channel.enableLights(true);
    channel.enableVibration(true);
    channel.setSound(
        Settings.System.DEFAULT_NOTIFICATION_URI,
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
    return channel;
  }

  private static boolean isSingleSimDevice(@NonNull Context context) {
    return context.getSystemService(TelephonyManager.class).getPhoneCount() <= 1;
  }

  private VoicemailChannelUtils() {}
}