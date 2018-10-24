package com.freeme.server.telecom.settings;

import android.annotation.Nullable;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.provider.ContactsContract;

import java.util.ArrayList;

public class FreemeBlockNumberTaskFragment extends Fragment {
    @Nullable
    private BlockNumberTask mTask;
    @Nullable
    Listener mListener;

    private static final Uri PHONES_WITH_PRESENCE_URI = ContactsContract.Data.CONTENT_URI;

    private static final int PHONE_NUMBER_COLUMN = 0;
    private static final int CONTACT_NAME_COLUMN = 1;

    private static final String[] CALLER_ID_PROJECTION = new String[]{
            ContactsContract.CommonDataKinds.Phone.NUMBER,                   // 0
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME              // 1
    };

    /**
     * Task to block a number.
     */
    private class BlockNumberTask extends AsyncTask<ArrayList, Void, Boolean> {
        private String blockedName;
        private String blockedNumber;

        @Override
        protected Boolean doInBackground(ArrayList... params) {
            if (params.length == 0) {
                return null;
            }
            ArrayList<Object> list = params[0];
            long[] ids = (long[]) list.get(0);
            StringBuilder idSetBuilder = new StringBuilder();
            boolean first = true;
            for (long id : ids) {
                if (first) {
                    first = false;
                    idSetBuilder.append(id);
                } else {
                    idSetBuilder.append(',').append(id);
                }
            }
            Cursor cursor = null;
            ContentResolver contentResolver = getContext().getContentResolver();
            if (idSetBuilder.length() > 0) {
                final String whereClause = ContactsContract.CommonDataKinds.Phone._ID + " IN (" + idSetBuilder.toString() + ")";
                cursor = contentResolver.query(
                        PHONES_WITH_PRESENCE_URI, CALLER_ID_PROJECTION, whereClause, null, null);
            }
            if (cursor == null) {
                return false;
            }
            try {
                while (cursor.moveToNext()) {
                    blockedName = cursor.getString(CONTACT_NAME_COLUMN);
                    blockedNumber = cursor.getString(PHONE_NUMBER_COLUMN);
                    if (BlockedNumberContract.isBlocked(getContext(), blockedNumber)) {
                        continue;
                    } else {
                        ContentValues newValues = new ContentValues();
                        newValues.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                                blockedNumber);
                        newValues.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NAME,
                                blockedName);
                        contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                                newValues);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mTask = null;
            if (mListener != null) {
                mListener.onBlocked(blockedNumber, result /* alreadyBlocked */);
            }
            mListener = null;
        }
    }

    public interface Listener {
        void onBlocked(String number, boolean alreadyBlocked);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        if (mTask != null) {
            mTask.cancel(true /* mayInterruptIfRunning */);
        }
        super.onDestroy();
    }

    /**
     * Runs an async task to write the number to the blocked numbers provider if it does not already
     * exist.
     * <p>
     * Triggers {@link Listener#onBlocked(String, boolean)} when task finishes to show proper UI.
     */
    public void blockIfNotAlreadyBlocked(ArrayList<Object> ids, Listener listener) {
        mListener = listener;
        mTask = new BlockNumberTask();
        mTask.execute(ids);
    }
}
