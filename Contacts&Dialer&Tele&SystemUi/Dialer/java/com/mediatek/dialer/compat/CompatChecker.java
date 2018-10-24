package com.mediatek.dialer.compat;

import com.android.dialer.util.PermissionsUtil;
import com.mediatek.dialer.compat.CallLogCompat.CallsCompat;
import com.mediatek.dialer.compat.ContactsCompat.RawContactsCompat;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;

/**
 * [portable]Utility class to check whether the columns really existed in db.
 * only need run one time.
 */
public class CompatChecker {
    private static final String TAG = CompatChecker.class.getSimpleName();
    private static CompatChecker sSingleton;
    private Context mContext;

    protected CompatChecker(Context context) {
        mContext = context;
    }

    /**
     * get the singleton instance of CompatChecker.
     */
    public static synchronized CompatChecker getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton = new CompatChecker(context.getApplicationContext());
        }
        return sSingleton;
    }

    /**
     * start the database columns check in the background.
     */
    public void startCheckerThread() {
        if (PermissionsUtil.hasContactsReadPermissions(mContext)) {
            new SimContactAsyncTask().execute();
        }
        if (PermissionsUtil.hasPhonePermissions(mContext)) {
            new CallsAsyncTask().execute();
        }
    }

    private class SimContactAsyncTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object... arg0) {
            checkSimContacts();
            return null;
        }
    }

    private class CallsAsyncTask extends AsyncTask {
        @Override
        protected Object doInBackground(Object... arg0) {
            checkConfCallLog();
            return null;
        }
    }

    private void checkSimContacts() {
        Cursor cursor = null;
        try {
            String[] projection = new String[] { RawContactsCompat.INDICATE_PHONE_SIM };
            cursor = mContext.getContentResolver().query(Contacts.CONTENT_URI, projection,
                    Contacts._ID + "=1", null, null);
            // if no exception means it supports INDICATE_PHONE_SIM
            DialerCompatEx.setSimContactsCompat(true);
        } catch (IllegalArgumentException e) {
            // if exception means it not support INDICATE_PHONE_SIM
            DialerCompatEx.setSimContactsCompat(false);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void checkConfCallLog() {
        Cursor callCursor = null;
        try {
            Uri uri = Uri.withAppendedPath(Calls.CONTENT_URI, "1");
            callCursor = mContext.getContentResolver().query(uri,
                    new String[] { CallsCompat.CONFERENCE_CALL_ID }, null, null, null);
            // if no exception means it supports the columns
            DialerCompatEx.setConferenceCallLogCompat(true);
        } catch (IllegalArgumentException e) {
            DialerCompatEx.setConferenceCallLogCompat(false);
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
    }
}
