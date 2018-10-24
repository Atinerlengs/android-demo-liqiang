package com.freeme.contacts.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.vcard.ExportVCardActivity;
import com.android.contacts.vcard.VCardCommonArguments;
import com.google.common.collect.Lists;
import com.mediatek.contacts.activities.ContactImportExportActivity;
import com.mediatek.contacts.list.service.MultiChoiceHandlerListener;
import com.mediatek.contacts.list.service.MultiChoiceRequest;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simservice.SimServiceUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.provider.MtkContactsContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FreemeContactsCopyUtils {

    private static final String TAG = "FreemeContactsCopyUtils";

    private IFreemePickDataCacheCallBack mCallBack;
    private CopyRequestConnection mCopyRequestConnection;
    private SendRequestHandler mRequestHandler;
    private HandlerThread mHandlerThread;

    public FreemeContactsCopyUtils(IFreemePickDataCacheCallBack callBack) {
        if (callBack == null) {
            throw new NullPointerException();
        }
        mCallBack = callBack;
    }

    public void doExportVCardToSDCard() {
        Activity activity = mCallBack.getActivity();
        StringBuilder exportSelection = new StringBuilder();
        exportSelection.append(ContactsContract.Contacts._ID + " IN " + join());

        Log.d(TAG, "[doExportVCardToSDCard] exportSelection is " + exportSelection.toString());

        Intent exportIntent = new Intent(activity, ExportVCardActivity.class);
        /* M:fix ALPS00997877 and new feature for Dialer using mtk contactimportExport.
         * The ExportVCardActivity need ARG_CALLING_ACTIVITYSo when user click on the
         * notification after export finished,it will jump to callingActivitiy(PeopleActivity
         * or DialtasActivity) which start ContactImportExportActivity @{
         */
        Log.i(TAG, "[doExportVCardToSDCard] mCallBack.getCallingActivityName() = "
                + mCallBack.getCallingActivityName());
        exportIntent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY,
                mCallBack.getCallingActivityName());
        //exportIntent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY,
        //        PeopleActivity.class.getName());
        //@}

        exportIntent.putExtra("exportselection", exportSelection.toString());
        exportIntent.putExtra("dest_path", mCallBack.getDstAccount().dataSet);

        activity.startActivityForResult(exportIntent, ContactImportExportActivity.REQUEST_CODE);
    }

    public void doCopyContacts() {
        startCopyService();

        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
            mRequestHandler = new SendRequestHandler(mCallBack.getActivity().getMainLooper());
        }

        mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                SendRequestHandler.MSG_TOAST,
                mCallBack.getActivity()
                        .getString(R.string.freeme_contact_settings_copy_contacts_tips)));

        mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                SendRequestHandler.MSG_QUERY));

    }

    private void startCopyService() {
        mCopyRequestConnection = new CopyRequestConnection();

        Log.i(TAG, "[startCopyService]Bind to MultiChoiceService.");
        // We don't want the service finishes itself just after this connection.
        Context context = mCallBack.getActivity();
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(context, MultiChoiceService.class);
        appContext.startService(intent);
        appContext.bindService(intent, mCopyRequestConnection, Context.BIND_AUTO_CREATE);
    }

    private void destroyMyself() {
        Log.d(TAG, "[destroyMyself]");
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }

        Activity activity = mCallBack.getActivity();
        if (activity != null) {
            activity.getApplicationContext().unbindService(mCopyRequestConnection);
            if (activity instanceof FreemeContactsSettingsActivity) {
                ((FreemeContactsSettingsActivity) activity).resetStep();
            } else {
                activity.finish();
            }
        }
    }

    private class CopyRequestConnection implements ServiceConnection {

        private MultiChoiceService mService;

        public boolean sendCopyRequest(final List<MultiChoiceRequest> requests) {
            Log.d(TAG, "[sendCopyRequest]Send an copy request");
            if (mService == null) {
                Log.i(TAG, "[sendCopyRequest]mService is not ready");
                return false;
            }
            mService.handleCopyRequest(requests, new MultiChoiceHandlerListener(mService,
                            mCallBack.getCallingActivityName()), mCallBack.getSrcAccount(),
                    mCallBack.getDstAccount());
            return true;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "[onServiceConnected]");
            mService = ((MultiChoiceService.MyBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "[onServiceDisconnected]");
        }
    }

    private class SendRequestHandler extends Handler {

        public static final int MSG_REQUEST = 100;
        public static final int MSG_QUERY = 200;
        public static final int MSG_TOAST = 300;
        public static final int MSG_END = 400;
        private int mRetryCount = 20;

        public SendRequestHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "[handleMessage]msg.what = " + msg.what);
            if (msg.what == MSG_REQUEST) {
                if (!mCopyRequestConnection.sendCopyRequest((List<MultiChoiceRequest>) msg.obj)) {
                    if (mRetryCount-- > 0) {
                        sendMessageDelayed(obtainMessage(msg.what, msg.obj), 500);
                    } else {
                        sendMessage(obtainMessage(MSG_END));
                    }
                } else {
                    sendMessage(obtainMessage(MSG_END));
                }
                return;
            } else if (msg.what == MSG_END) {
                destroyMyself();
                return;
            } else if (msg.what == MSG_QUERY) {
                new QueryContactsTask().execute();
                return;
            } else if (msg.what == MSG_TOAST) {
                MtkToast.toast(mCallBack.getActivity(), (String) msg.obj, Toast.LENGTH_SHORT);
                return;
            }
            super.handleMessage(msg);
        }
    }

    private String join() {
        StringBuffer buffer = new StringBuffer("(");
        long[] ids = mCallBack.getCheckedContactIds();
        if (ids != null && ids.length > 0) {
            boolean isFirst = true;
            for (long id : ids) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    buffer.append(",");
                }
                buffer.append(id);
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    private class QueryContactsTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground(Void... voids) {
            long[] ids = mCallBack.getCheckedContactIds();
            if (ids == null || ids.length <= 0) {
                return null;
            }

            final String[] _projection = new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE,
                    ContactsContract.Contacts.LOOKUP_KEY,
            };
            List<String> projectionList = Lists.newArrayList(_projection);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                /// M: Add some columns for Contacts extensions. @{
                projectionList.add(MtkContactsContract.ContactsColumns.INDICATE_PHONE_SIM);
                projectionList.add(MtkContactsContract.ContactsColumns.INDEX_IN_SIM);
                /// @}
            }
            final String[] projection = projectionList.toArray(new String[projectionList.size()]);

            return mCallBack.getActivity().getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI, projection,
                    ContactsContract.Contacts._ID + " IN " + join() + "",
                    null, null);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            List<MultiChoiceRequest> requests = constructData(cursor);

            request(requests);
        }

        private List<MultiChoiceRequest> constructData(Cursor cursor) {
            List<MultiChoiceRequest> mRequests = new ArrayList<>();
            if (cursor == null) {
                return mRequests;
            }
            if (cursor.getCount() <= 0) {
                cursor.close();
                return mRequests;
            }

            if (!cursor.moveToFirst()) {
                return mRequests;
            }

            PickListItemCache listItemCache = new PickListItemCache();
            do {
                listItemCache.add(cursor);
            } while (cursor.moveToNext());
            cursor.close();

            // Bug fix ALPS01651069, if listItemCache is empty, just return.
            if (listItemCache == null || listItemCache.isEmpty()) {
                Log.w(TAG, "[onOptionAction]listItemCache is empty,return.");
                return mRequests;
            }

            for (long id : mCallBack.getCheckedContactIds()) {
                PickListItemCache.PickListItemData item = listItemCache.getItemData(id);
                if (item != null) {
                    mRequests.add(new MultiChoiceRequest(item.contactIndicator, item.simIndex,
                            (int) id, item.displayName));
                }
            }
            return mRequests;
        }

        private void request(List<MultiChoiceRequest> requests) {
            if (requests == null || requests.size() <= 0) {
                mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                        SendRequestHandler.MSG_END));
            }

            Context context = mCallBack.getActivity();
            mDstStoreType = getStoreType(mCallBack.getDstAccount());

            // UIM
            if (mDstStoreType == DST_STORE_TYPE_SIM ||
                    mDstStoreType == DST_STORE_TYPE_USIM ||
                    mDstStoreType == DST_STORE_TYPE_RUIM ||
                    mDstStoreType == DST_STORE_TYPE_CSIM) {
                // Check Radio state
                int subId = ((AccountWithDataSetEx) mCallBack.getDstAccount()).getSubId();
                /** M: change for PHB Status Refactoring. @{ */
                if (SimCardUtils.isPhoneBookReady(subId)) {
                    boolean serviceRunning = SimServiceUtils.isServiceRunning(context, subId);
                    Log.i(TAG, "[onOptionAction]AbstractService state is running? "
                            + serviceRunning);
                    if (serviceRunning) {
                        mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                                SendRequestHandler.MSG_TOAST,
                                context.getString(R.string.notifier_fail_copy_title)));

                        mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                                SendRequestHandler.MSG_END));
                    } else {
                        mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                                SendRequestHandler.MSG_REQUEST, requests));
                    }
                    /** @} */
                } else {
                    Log.i(TAG, "[onOptionAction] isPhoneBookReady return false.");
                    mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                            SendRequestHandler.MSG_TOAST,
                            context.getString(R.string.notifier_fail_copy_title)));

                    mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                            SendRequestHandler.MSG_END));
                }
                /**@}*/
            } else {
                mRequestHandler.sendMessage(mRequestHandler.obtainMessage(
                        SendRequestHandler.MSG_REQUEST, requests));
            }
        }

        private static final int DST_STORE_TYPE_NONE = 0;
        private static final int DST_STORE_TYPE_PHONE = 1;
        private static final int DST_STORE_TYPE_SIM = 2;
        private static final int DST_STORE_TYPE_USIM = 3;
        private static final int DST_STORE_TYPE_STORAGE = 4;
        private static final int DST_STORE_TYPE_ACCOUNT = 5;
        // UIM
        private static final int DST_STORE_TYPE_RUIM = 6;
        private static final int DST_STORE_TYPE_CSIM = 7;
        private int mDstStoreType = DST_STORE_TYPE_NONE;

        private int getStoreType(AccountWithDataSet account) {
            if (account == null) {
                Log.w(TAG, "[getStoreType]account is null.");
                return DST_STORE_TYPE_NONE;
            }

            if (ContactImportExportActivity.STORAGE_ACCOUNT_TYPE.equals(account.type)) {
                return DST_STORE_TYPE_STORAGE;
            } else if (AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE.equals(account.type)) {
                return DST_STORE_TYPE_PHONE;
            } else if (AccountTypeUtils.ACCOUNT_TYPE_SIM.equals(account.type)) {
                return DST_STORE_TYPE_SIM;
            } else if (AccountTypeUtils.ACCOUNT_TYPE_USIM.equals(account.type)) {
                return DST_STORE_TYPE_USIM;
            } else if (AccountTypeUtils.ACCOUNT_TYPE_RUIM.equals(account.type)) {
                return DST_STORE_TYPE_RUIM;
            } else if (AccountTypeUtils.ACCOUNT_TYPE_CSIM.equals(account.type)) {
                return DST_STORE_TYPE_CSIM;
            }
            // / M: UIM

            return DST_STORE_TYPE_ACCOUNT;
        }
    }

    private final class PickListItemCache {

        public final class PickListItemData {
            public int contactIndicator;

            public int simIndex;

            public String displayName;

            public String lookupUri;

            public PickListItemData(int contactIndicator2, int simIndex2, String displayName2,
                                    String lookupUri2) {
                contactIndicator = contactIndicator2;
                simIndex = simIndex2;
                displayName = displayName2;
                lookupUri = lookupUri2;
            }

            @Override
            public String toString() {
                return "[PickListItemData]@" + hashCode() + " contactIndicator: "
                        + contactIndicator + ", simIndex: " + simIndex + ", displayName: "
                        + displayName + ", lookupUri: " + lookupUri;
            }

        }

        private HashMap<Long, PickListItemData> mMap = new HashMap<>();

        public void add(long id, int contactIndicator, int simIndex, String displayName,
                        String lookupUri) {
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                mMap.put(Long.valueOf(id), new PickListItemData(contactIndicator, simIndex,
                        displayName, lookupUri));
            } else {
                mMap.put(Long.valueOf(id), new PickListItemData(-1, -1,
                        displayName, lookupUri));
            }
        }

        public void add(final Cursor cursor) {
            long id = cursor.getInt(0 /* CONTACT_ID */);
            String displayName = cursor.getString(1 /* DISPLAY_NAME_ALTERNATIVE */);
            String lookupUri = cursor.getString(2 /* LOOKUP_KEY */);

            int contactIndicator = -1;
            int simIndex = -1;
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                contactIndicator = cursor.getInt(cursor.getColumnIndexOrThrow(
                        MtkContactsContract.ContactsColumns.INDICATE_PHONE_SIM));
                simIndex = cursor.getInt(cursor.getColumnIndexOrThrow(
                        MtkContactsContract.ContactsColumns.INDEX_IN_SIM));
            }
            mMap.put(Long.valueOf(id), new PickListItemData(contactIndicator, simIndex,
                    displayName, lookupUri));
        }

        // Clear the cache data
        public void clear() {
            mMap.clear();
        }

        // The cache is empty or not
        public boolean isEmpty() {
            return mMap.isEmpty();
        }

        public int getCacheSize() {
            return mMap.size();
        }

        public PickListItemData getItemData(long id) {
            return mMap.get(Long.valueOf(id));
        }
    }

    public interface IFreemePickDataCacheCallBack {

        Activity getActivity();

        AccountWithDataSet getSrcAccount();

        AccountWithDataSet getDstAccount();

        long[] getCheckedContactIds();

        String getCallingActivityName();
    }
}
