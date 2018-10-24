package com.mediatek.dialer.calllog;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog.Calls;
import android.text.TextUtils;
import android.telecom.PhoneAccountHandle;

import com.android.dialer.app.calllog.CallLogAsyncTaskUtil.Tasks;
import com.android.dialer.calldetails.CallDetailsEntries.CallDetailsEntry;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.common.concurrent.AsyncTaskExecutor;
import com.android.dialer.common.concurrent.AsyncTaskExecutors;
import com.android.dialer.common.LogUtil;
import com.android.dialer.compat.CompatUtils;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.phonenumbercache.CallLogQuery;
import com.android.dialer.phonenumbercache.ContactInfo;
import com.android.dialer.phonenumbercache.ContactInfoHelper;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.telecom.TelecomUtil;
import com.mediatek.dialer.util.DialerFeatureOptions;

import java.util.List;

/// M: [VoLTE ConfCallLog] For volte conference callLog
public class ConfCallLogAsyncTaskUtil {
    private static final String TAG = "ConfCallLogAsyncTaskUtil";
    private static AsyncTaskExecutor sAsyncTaskExecutor;

    private static void initTaskExecutor() {
        sAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
    }
    /**
     * M: Create a phone call detail includes contact information, copy from
     * getPhoneCallDetailsForUri.
     * @param context
     * @param cursor
     * @return phone call detail with contact information
     */
    public static PhoneCallDetails createConferenceCallDetails(Context context, Cursor cursor) {
        // Read call log.
        final String countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
        final String number = cursor.getString(CallLogQuery.NUMBER);
        final int numberPresentation = cursor.getInt(CallLogQuery.NUMBER_PRESENTATION);
        final String postDialDigits = CompatUtils.isNCompatible() ? cursor
                .getString(CallLogQuery.POST_DIAL_DIGITS) : "";

        final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
                cursor.getString(CallLogQuery.ACCOUNT_COMPONENT_NAME),
                cursor.getString(CallLogQuery.ACCOUNT_ID));

        // If this is not a regular number, there is no point in looking it up
        // in the contacts.
        ContactInfoHelper contactInfoHelper = new ContactInfoHelper(context,
                GeoUtil.getCurrentCountryIso(context));
        boolean isVoicemail = PhoneNumberHelper.isVoicemailNumber(context, accountHandle, number);
        boolean shouldLookupNumber = PhoneNumberHelper.canPlaceCallsTo(number, numberPresentation)
                && !isVoicemail;
        ContactInfo info = ContactInfo.EMPTY;
        if (shouldLookupNumber) {
            ContactInfo lookupInfo = contactInfoHelper.lookupNumber(number, countryIso);
            info = lookupInfo != null ? lookupInfo : ContactInfo.EMPTY;
        }

        PhoneCallDetails details = new PhoneCallDetails(number, numberPresentation, postDialDigits);

        details.updateDisplayNumber(context, info.formattedNumber, isVoicemail);
        details.accountHandle = accountHandle;
        details.contactUri = info.lookupUri;
        details.namePrimary = info.name;
        details.nameAlternative = info.nameAlternative;
        details.numberType = info.type;
        details.numberLabel = info.label;
        details.photoUri = info.photoUri;
        details.sourceType = info.sourceType;
        details.objectId = info.objectId;

        details.callTypes = new int[]{cursor.getInt(CallLogQuery.CALL_TYPE)};
        details.date = cursor.getLong(CallLogQuery.DATE);
        details.duration = cursor.getLong(CallLogQuery.DURATION);
        details.features = cursor.getInt(CallLogQuery.FEATURES);
        details.geocode = cursor.getString(CallLogQuery.GEOCODED_LOCATION);
        details.transcription = cursor.getString(CallLogQuery.TRANSCRIPTION);

        details.countryIso = !TextUtils.isEmpty(countryIso) ? countryIso : GeoUtil
                .getCurrentCountryIso(context);

        if (!cursor.isNull(CallLogQuery.DATA_USAGE)) {
            details.dataUsage = cursor.getLong(CallLogQuery.DATA_USAGE);
        }

        // / M: [VoLTE ConfCallLog] Is it conference child callLog
        if (DialerFeatureOptions.isVolteConfCallLogSupport()) {
            details.conferenceId = cursor.getLong(CallLogQuery.CONFERENCE_CALL_ID);
        }

        return details;
    }

    public interface ConfCallLogAsyncTaskListener {
        public void onGetConfCallDetails(Cursor cursor, PhoneCallDetails[] details);
    }

    public static void getConferenceCallDetails(final Context context,
            final List<CallDetailsEntry> callLogEntries,
            final ConfCallLogAsyncTaskListener confCallLogAsyncTaskListener) {
        if (callLogEntries == null || callLogEntries.size() < 1) {
            return;
        }
        final StringBuilder selection = new StringBuilder();
        selection.append("calls." + Calls._ID);
        selection.append(" IN (");
        selection.append(callLogEntries.get(0).getCallId());
        for (int i = 1; i < callLogEntries.size(); i++) {
            selection.append(",");
            selection.append(callLogEntries.get(i).getCallId());
        }
        selection.append(")");
        LogUtil.d(TAG, "getConferenceCallDetails callLogIds " + selection.toString());

        if (sAsyncTaskExecutor == null) {
            initTaskExecutor();
        }
        LogUtil.d("ConfCallLogAsyncTaskUtil.getConferenceCallDetails", "AsyncTaskExecutor.submit.");
        sAsyncTaskExecutor.submit(Tasks.GET_CALL_DETAILS, new ConferenceDetailsAsyncTask(context,
                selection.toString(), confCallLogAsyncTaskListener));
    }

    private static class ConferenceDetailsAsyncTask extends AsyncTask<Void, Void, Cursor> {
        private Context mContext;
        private String mSelection;
        private ConfCallLogAsyncTaskListener mListener;
        private PhoneCallDetails[] mDetails;

        public ConferenceDetailsAsyncTask(Context context, String selection,
                ConfCallLogAsyncTaskListener listener) {
            super();
            mContext = context;
            mSelection = selection;
            mListener = listener;
        }

        @Override
        public Cursor doInBackground(Void... params) {
            try {
                Uri queryUri = TelecomUtil.getCallLogUri(mContext);

                Cursor cursor = mContext.getContentResolver().query(queryUri,
                        CallLogQuery.getProjection(), mSelection, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    mDetails = new PhoneCallDetails[cursor.getCount()];
                    cursor.moveToFirst();
                    for (int i = 0; i < cursor.getCount(); i++) {
                        mDetails[i] = ConfCallLogAsyncTaskUtil.createConferenceCallDetails(
                                mContext, cursor);
                        cursor.moveToNext();
                    }
                }
                return cursor;
            } catch (IllegalArgumentException e) {
                // Something went wrong reading in our primary data.
                LogUtil.w(TAG, "Invalid URI starting conf call details", e);
                return null;
            }
        }

        @Override
        public void onPostExecute(Cursor cursor) {
            if (mListener != null) {
                mListener.onGetConfCallDetails(cursor, mDetails);
            }
        }
    }
}
