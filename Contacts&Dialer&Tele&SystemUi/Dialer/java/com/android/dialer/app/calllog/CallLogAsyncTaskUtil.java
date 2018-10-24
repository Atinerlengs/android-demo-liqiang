/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.dialer.app.calllog;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION_CODES;
import android.provider.CallLog;
import android.provider.VoicemailContract.Voicemails;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import com.android.dialer.common.LogUtil;
import com.android.contacts.common.ContactPhotoManager;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.common.concurrent.AsyncTaskExecutor;
import com.android.dialer.common.concurrent.AsyncTaskExecutors;
import com.android.dialer.common.LogUtil;
import com.android.dialer.compat.CompatUtils;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.phonenumbercache.ContactInfo;
import com.android.dialer.phonenumbercache.ContactInfoHelper;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.util.PermissionsUtil;
import com.android.voicemail.VoicemailClient;
import java.util.ArrayList;
import java.util.Arrays;

//*/ freeme.liqiang, 20180204, calllog details
import android.util.Log;
import com.android.dialer.phonenumbercache.CallLogQuery;
import com.android.dialer.telecom.TelecomUtil;
import com.mediatek.dialer.util.DialerFeatureOptions;
//*/

@TargetApi(VERSION_CODES.M)
public class CallLogAsyncTaskUtil {

  private static final String TAG = "CallLogAsyncTaskUtil";
  private static AsyncTaskExecutor sAsyncTaskExecutor;

  private static void initTaskExecutor() {
    sAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
  }

  public static void markVoicemailAsRead(
      @NonNull final Context context, @NonNull final Uri voicemailUri) {
    LogUtil.enterBlock("CallLogAsyncTaskUtil.markVoicemailAsRead, voicemailUri: " + voicemailUri);
    if (sAsyncTaskExecutor == null) {
      initTaskExecutor();
    }

    LogUtil.d("ConfCallLogAsyncTaskUtil.markVoicemailAsRead", "AsyncTaskExecutor.submit.");
    sAsyncTaskExecutor.submit(
        Tasks.MARK_VOICEMAIL_READ,
        new AsyncTask<Void, Void, Void>() {
          @Override
          public Void doInBackground(Void... params) {
            ContentValues values = new ContentValues();
            values.put(Voicemails.IS_READ, true);
            // "External" changes to the database will be automatically marked as dirty, but this
            // voicemail might be from dialer so it need to be marked manually.
            values.put(Voicemails.DIRTY, 1);
            if (context
                    .getContentResolver()
                    .update(voicemailUri, values, Voicemails.IS_READ + " = 0", null)
                > 0) {
              uploadVoicemailLocalChangesToServer(context);
            }

            CallLogNotificationsService.markAllNewVoicemailsAsOld(context);
            return null;
          }
        });
  }

  public static void deleteVoicemail(
      @NonNull final Context context,
      final Uri voicemailUri,
      @Nullable final CallLogAsyncTaskListener callLogAsyncTaskListener) {
    if (sAsyncTaskExecutor == null) {
      initTaskExecutor();
    }

    LogUtil.d("ConfCallLogAsyncTaskUtil.deleteVoicemail", "AsyncTaskExecutor.submit.");
    sAsyncTaskExecutor.submit(
        Tasks.DELETE_VOICEMAIL,
        new AsyncTask<Void, Void, Void>() {
          @Override
          public Void doInBackground(Void... params) {
            deleteVoicemailSynchronous(context, voicemailUri);
            return null;
          }

          @Override
          public void onPostExecute(Void result) {
            if (callLogAsyncTaskListener != null) {
              callLogAsyncTaskListener.onDeleteVoicemail();
            }
          }
        });
  }

  public static void deleteVoicemailSynchronous(Context context, Uri voicemailUri) {
    ContentValues values = new ContentValues();
    values.put(Voicemails.DELETED, "1");
    context.getContentResolver().update(voicemailUri, values, null, null);
    // TODO(b/35440541): check which source package is changed. Don't need
    // to upload changes on foreign voicemails, they will get a PROVIDER_CHANGED
    uploadVoicemailLocalChangesToServer(context);
  }

  public static void markCallAsRead(@NonNull final Context context, @NonNull final long[] callIds) {
    if (!PermissionsUtil.hasPhonePermissions(context)
        || !PermissionsUtil.hasCallLogWritePermissions(context)) {
      return;
    }
    if (sAsyncTaskExecutor == null) {
      initTaskExecutor();
    }

    LogUtil.d("ConfCallLogAsyncTaskUtil.markCallAsRead", "AsyncTaskExecutor.submit.");
    sAsyncTaskExecutor.submit(
        Tasks.MARK_CALL_READ,
        new AsyncTask<Void, Void, Void>() {
          @Override
          public Void doInBackground(Void... params) {

            StringBuilder where = new StringBuilder();
            where.append(CallLog.Calls.TYPE).append(" = ").append(CallLog.Calls.MISSED_TYPE);
            where.append(" AND ");

            Long[] callIdLongs = new Long[callIds.length];
            for (int i = 0; i < callIds.length; i++) {
              callIdLongs[i] = callIds[i];
            }
            where
                .append(CallLog.Calls._ID)
                .append(" IN (" + TextUtils.join(",", callIdLongs) + ")");

            ContentValues values = new ContentValues(1);
            values.put(CallLog.Calls.IS_READ, "1");
            context
                .getContentResolver()
                .update(CallLog.Calls.CONTENT_URI, values, where.toString(), null);
            return null;
          }
        });
  }

  /** The enumeration of {@link AsyncTask} objects used in this class. */
  public enum Tasks {
    DELETE_VOICEMAIL,
    DELETE_CALL,
    MARK_VOICEMAIL_READ,
    MARK_CALL_READ,
    GET_CALL_DETAILS,
    UPDATE_DURATION,
  }

  public interface CallLogAsyncTaskListener {
    void onDeleteVoicemail();
    /// M: [Dialer Global Search]
    void onGetCallDetails(PhoneCallDetails[] details);
      //*/ freeme.liqiang, 20180204, calllog details
      void onDeleteCall();
      //*/
  }

  private static void uploadVoicemailLocalChangesToServer(Context context) {
    Intent intent = new Intent(VoicemailClient.ACTION_UPLOAD);
    intent.setPackage(context.getPackageName());
    context.sendBroadcast(intent);
  }

  /// M: [Dialer Global Search] @{
  public static void getCallDetails(
      final Context context,
      final Uri[] callUris,
      final CallLogAsyncTaskListener callLogAsyncTaskListener) {
    if (sAsyncTaskExecutor == null) {
      initTaskExecutor();
    }
    LogUtil.d("ConfCallLogAsyncTaskUtil.getCallDetails", "AsyncTaskExecutor.submit.");
    sAsyncTaskExecutor.submit(Tasks.GET_CALL_DETAILS,
        new AsyncTask<Void, Void, PhoneCallDetails[]>() {
          @Override
          public PhoneCallDetails[] doInBackground(Void... params) {
            // TODO: All calls correspond to the same person, so make a single lookup.
            final int numCalls = callUris.length;
            PhoneCallDetails[] details = new PhoneCallDetails[numCalls];
            try {
              for (int index = 0; index < numCalls; ++index) {
                details[index] =
                    getPhoneCallDetailsForUri(context, callUris[index]);
              }
              return details;
            } catch (IllegalArgumentException e) {
              // Something went wrong reading in our primary data.
              LogUtil.w(TAG, "Invalid URI starting call details", e);
              return null;
            }
          }

          @Override
          public void onPostExecute(PhoneCallDetails[] phoneCallDetails) {
            if (callLogAsyncTaskListener != null) {
              callLogAsyncTaskListener.onGetCallDetails(phoneCallDetails);
            }
          }
        });
  }

  /**
   * Return the phone call details for a given call log URI.
   */
  private static PhoneCallDetails getPhoneCallDetailsForUri(Context context, Uri callUri) {
    Cursor cursor = context.getContentResolver().query(
        callUri, CallDetailQuery.CALL_LOG_PROJECTION, null, null, null);

    try {
      if (cursor == null || !cursor.moveToFirst()) {
        throw new IllegalArgumentException("Cannot find content: " + callUri);
      }

      // Read call log.
      final String countryIso = cursor.getString(CallDetailQuery.COUNTRY_ISO_COLUMN_INDEX);
      final String number = cursor.getString(CallDetailQuery.NUMBER_COLUMN_INDEX);
      final String postDialDigits = CompatUtils.isNCompatible()
          ? cursor.getString(CallDetailQuery.POST_DIAL_DIGITS) : "";
      final String viaNumber = CompatUtils.isNCompatible() ?
          cursor.getString(CallDetailQuery.VIA_NUMBER) : "";
      final int numberPresentation =
          cursor.getInt(CallDetailQuery.NUMBER_PRESENTATION_COLUMN_INDEX);

      final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
          cursor.getString(CallDetailQuery.ACCOUNT_COMPONENT_NAME),
          cursor.getString(CallDetailQuery.ACCOUNT_ID));

      // If this is not a regular number, there is no point in looking it up in the contacts.
      ContactInfoHelper contactInfoHelper =
          new ContactInfoHelper(context, GeoUtil.getCurrentCountryIso(context));
      boolean isVoicemail = PhoneNumberHelper.isVoicemailNumber(context, accountHandle, number);
      boolean shouldLookupNumber =
          PhoneNumberHelper.canPlaceCallsTo(number, numberPresentation) && !isVoicemail;
      ContactInfo info = ContactInfo.EMPTY;

      if (shouldLookupNumber) {
        ContactInfo lookupInfo = contactInfoHelper
            .lookupNumber(/*M: should lookup entire number with post digits*/
                 number + postDialDigits, countryIso);
        info = lookupInfo != null ? lookupInfo : ContactInfo.EMPTY;
      }

      PhoneCallDetails details = new PhoneCallDetails(number,
          numberPresentation, postDialDigits);

      details.viaNumber = viaNumber;
      details.accountHandle = accountHandle;
      details.contactUri = info.lookupUri;
      details.namePrimary = info.name;
      details.nameAlternative = info.nameAlternative;
      details.numberType = info.type;
      details.numberLabel = info.label;
      details.photoUri = info.photoUri;
      details.photoId = info.photoId;
      details.sourceType = info.sourceType;
      details.objectId = info.objectId;

      details.callTypes = new int[] {
          cursor.getInt(CallDetailQuery.CALL_TYPE_COLUMN_INDEX)
      };
      details.callId = cursor.getLong(CallDetailQuery.ID_COLUMN_INDEX);
      details.date = cursor.getLong(CallDetailQuery.DATE_COLUMN_INDEX);
      details.duration = cursor.getLong(CallDetailQuery.DURATION_COLUMN_INDEX);
      details.features = cursor.getInt(CallDetailQuery.FEATURES);
      details.geocode = cursor.getString(CallDetailQuery.GEOCODED_LOCATION_COLUMN_INDEX);
      details.transcription = cursor.getString(CallDetailQuery.TRANSCRIPTION_COLUMN_INDEX);

      details.countryIso = !TextUtils.isEmpty(countryIso) ? countryIso
          : GeoUtil.getCurrentCountryIso(context);

      if (!cursor.isNull(CallDetailQuery.DATA_USAGE)) {
          details.dataUsage = cursor.getLong(CallDetailQuery.DATA_USAGE);
      }

      int contactType = ContactPhotoManager.TYPE_DEFAULT;
      if (isVoicemail) {
        contactType = ContactPhotoManager.TYPE_VOICEMAIL;
      } else if (contactInfoHelper.isBusiness(info.sourceType)) {
        contactType = ContactPhotoManager.TYPE_BUSINESS;
      } else if (numberPresentation == TelecomManager.PRESENTATION_RESTRICTED) {
        contactType = ContactPhotoManager.TYPE_GENERIC_AVATAR;
      }
      details.updateDisplayNumber(context, null, false);
        return details;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  private static final class CallDetailQuery {

    private static final String[] CALL_LOG_PROJECTION_INTERNAL = new String[] {
      CallLog.Calls._ID,
      CallLog.Calls.DATE,
      CallLog.Calls.DURATION,
      CallLog.Calls.NUMBER,
      CallLog.Calls.TYPE,
      CallLog.Calls.COUNTRY_ISO,
      CallLog.Calls.GEOCODED_LOCATION,
      CallLog.Calls.NUMBER_PRESENTATION,
      CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME,
      CallLog.Calls.PHONE_ACCOUNT_ID,
      CallLog.Calls.FEATURES,
      CallLog.Calls.DATA_USAGE,
      CallLog.Calls.TRANSCRIPTION
    };
    public static final String[] CALL_LOG_PROJECTION;

    static final int ID_COLUMN_INDEX = 0;
    static final int DATE_COLUMN_INDEX = 1;
    static final int DURATION_COLUMN_INDEX = 2;
    static final int NUMBER_COLUMN_INDEX = 3;
    static final int CALL_TYPE_COLUMN_INDEX = 4;
    static final int COUNTRY_ISO_COLUMN_INDEX = 5;
    static final int GEOCODED_LOCATION_COLUMN_INDEX = 6;
    static final int NUMBER_PRESENTATION_COLUMN_INDEX = 7;
    static final int ACCOUNT_COMPONENT_NAME = 8;
    static final int ACCOUNT_ID = 9;
    static final int FEATURES = 10;
    static final int DATA_USAGE = 11;
    static final int TRANSCRIPTION_COLUMN_INDEX = 12;
    static final int POST_DIAL_DIGITS = 13;
    static final int VIA_NUMBER = 14;

    static {
      ArrayList<String> projectionList = new ArrayList<>();
      projectionList.addAll(Arrays.asList(CALL_LOG_PROJECTION_INTERNAL));
      if (CompatUtils.isNCompatible()) {
        projectionList.add(CallLog.Calls.POST_DIAL_DIGITS);
        projectionList.add(CallLog.Calls.VIA_NUMBER);
      }
      projectionList.trimToSize();
      CALL_LOG_PROJECTION = projectionList.toArray(new String[projectionList.size()]);
    }
  }
    /// @}

    //*/ freeme.liqiang, 20180204, calllog details
    public static void deleteCalls(Context context, String selection, DeleteType type) {
        deleteCalls(context, selection, type, null);
    }

    public static void deleteCalls(
            final Context context,
            final String selection,
            final DeleteType type,
            final CallLogAsyncTaskListener callLogAsyncTaskListener) {
        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }

        sAsyncTaskExecutor.submit(Tasks.DELETE_CALL, new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                StringBuffer buffer = new StringBuffer();
                if (type == DeleteType.CALL_NUMBERS) {
                    buffer.append(CallLog.Calls.NUMBER);
                } else {
                    buffer.append(CallLog.Calls._ID);
                }
                buffer.append(" in (")
                      .append(selection)
                      .append(")");
                context.getContentResolver().delete(
                        TelecomUtil.getCallLogUri(context),
                        buffer.toString(), null);
                return null;
            }

            @Override
            public void onPostExecute(Void result) {
                if (callLogAsyncTaskListener != null) {
                    callLogAsyncTaskListener.onDeleteCall();
                }
            }
        });
    }

    public enum DeleteType {
        CALL_NUMBERS,
        CALL_IDS
    }
    //*/
}
