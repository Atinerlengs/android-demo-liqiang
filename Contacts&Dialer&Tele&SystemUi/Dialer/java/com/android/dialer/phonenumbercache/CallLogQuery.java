/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.phonenumbercache;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mediatek.dialer.compat.CallLogCompat.CallsCompat;
import com.mediatek.dialer.util.DialerFeatureOptions;

/** The query for the call log table. */
public final class CallLogQuery {

  public static final int ID = 0;
  public static final int NUMBER = 1;
  public static final int DATE = 2;
  public static final int DURATION = 3;
  public static final int CALL_TYPE = 4;
  public static final int COUNTRY_ISO = 5;
  public static final int VOICEMAIL_URI = 6;
  public static final int GEOCODED_LOCATION = 7;
  public static final int CACHED_NAME = 8;
  public static final int CACHED_NUMBER_TYPE = 9;
  public static final int CACHED_NUMBER_LABEL = 10;
  public static final int CACHED_LOOKUP_URI = 11;
  public static final int CACHED_MATCHED_NUMBER = 12;
  public static final int CACHED_NORMALIZED_NUMBER = 13;
  public static final int CACHED_PHOTO_ID = 14;
  public static final int CACHED_FORMATTED_NUMBER = 15;
  public static final int IS_READ = 16;
  public static final int NUMBER_PRESENTATION = 17;
  public static final int ACCOUNT_COMPONENT_NAME = 18;
  public static final int ACCOUNT_ID = 19;
  public static final int FEATURES = 20;
  public static final int DATA_USAGE = 21;
  public static final int TRANSCRIPTION = 22;
  public static final int CACHED_PHOTO_URI = 23;

  @RequiresApi(VERSION_CODES.N)
  public static final int POST_DIAL_DIGITS = 24;

  @RequiresApi(VERSION_CODES.N)
  public static final int VIA_NUMBER = 25;

  /// M: [VoLTE ConfCallLog] For Volte conference call calllog
  public static int CONFERENCE_CALL_ID = -1;

  private static final String[] PROJECTION_M =
      new String[] {
        Calls._ID, // 0
        Calls.NUMBER, // 1
        Calls.DATE, // 2
        Calls.DURATION, // 3
        Calls.TYPE, // 4
        Calls.COUNTRY_ISO, // 5
        Calls.VOICEMAIL_URI, // 6
        Calls.GEOCODED_LOCATION, // 7
        Calls.CACHED_NAME, // 8
        Calls.CACHED_NUMBER_TYPE, // 9
        Calls.CACHED_NUMBER_LABEL, // 10
        Calls.CACHED_LOOKUP_URI, // 11
        Calls.CACHED_MATCHED_NUMBER, // 12
        Calls.CACHED_NORMALIZED_NUMBER, // 13
        Calls.CACHED_PHOTO_ID, // 14
        Calls.CACHED_FORMATTED_NUMBER, // 15
        Calls.IS_READ, // 16
        Calls.NUMBER_PRESENTATION, // 17
        Calls.PHONE_ACCOUNT_COMPONENT_NAME, // 18
        Calls.PHONE_ACCOUNT_ID, // 19
        Calls.FEATURES, // 20
        Calls.DATA_USAGE, // 21
        Calls.TRANSCRIPTION, // 22
        Calls.CACHED_PHOTO_URI, // 23
      };

  /// M: @{
  private static final String[] PROJECTION_M2;
  static {
    List<String> projectionList = new ArrayList<>(Arrays.asList(PROJECTION_M));
    /// M:[MTK SIM Contacts feature] @{
    //if (DialerFeatureOptions.isSimContactsSupport()) {
      projectionList.add(CallsCompat.CACHED_INDICATE_PHONE_SIM);
      //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
      CACHED_INDICATE_PHONE_SIM = projectionList.size() - 1;
      //*/
      projectionList.add(CallsCompat.CACHED_IS_SDN_CONTACT);
    //}
    /// @}
    /// M: [VoLTE ConfCallLog] For Volte conference call calllog. @{
    //if (DialerFeatureOptions.isVolteConfCallLogSupport()) {
      projectionList.add(CallsCompat.CONFERENCE_CALL_ID);
      CONFERENCE_CALL_ID = projectionList.size() - 1;
    //}
    /// @}
    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    projectionList.add(Calls.IS_FREEME_YELLOW_PAGE);
    FREEME_YELLOW_FLAG_IDX = projectionList.size() - 1;
    projectionList.add(Calls.FREEME_NUMBER_MARK);
    FREEME_CALL_MARK_IDX = projectionList.size() - 1;
    //*/
    PROJECTION_M2 = projectionList.toArray(new String[projectionList.size()]);
  }
  /// @}

  private static final String[] PROJECTION_N;

  static {
    List<String> projectionList = new ArrayList<>(Arrays.asList(PROJECTION_M));
    projectionList.add(CallLog.Calls.POST_DIAL_DIGITS);
    projectionList.add(CallLog.Calls.VIA_NUMBER);
    /// M:[MTK SIM Contacts feature] @{
    //if (DialerFeatureOptions.isSimContactsSupport()) {
      projectionList.add(CallsCompat.CACHED_INDICATE_PHONE_SIM);
      //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
      CACHED_INDICATE_PHONE_SIM = projectionList.size() - 1;
      //*/
      projectionList.add(CallsCompat.CACHED_IS_SDN_CONTACT);
    //}
    /// @}
    /// M: [VoLTE ConfCallLog] For Volte conference call calllog. @{
    //if (DialerFeatureOptions.isVolteConfCallLogSupport()) {
      projectionList.add(CallsCompat.CONFERENCE_CALL_ID);
      CONFERENCE_CALL_ID = projectionList.size() - 1;
    //}
    /// @}
    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    projectionList.add(Calls.IS_FREEME_YELLOW_PAGE);
    FREEME_YELLOW_FLAG_IDX = projectionList.size() - 1;
    projectionList.add(Calls.FREEME_NUMBER_MARK);
    FREEME_CALL_MARK_IDX = projectionList.size() - 1;
    //*/
    PROJECTION_N = projectionList.toArray(new String[projectionList.size()]);
  }

  @NonNull
  public static String[] getProjection() {
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      return PROJECTION_N;
    }
    return PROJECTION_M2;
  }

    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    public static int CACHED_INDICATE_PHONE_SIM = -1;
    public static int FREEME_YELLOW_FLAG_IDX = -1;
    public static int FREEME_CALL_MARK_IDX = -1;
    //*/
}