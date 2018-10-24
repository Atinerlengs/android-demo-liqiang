package com.freeme.dialer.calllog;

import android.app.ActionBar;
import android.content.Intent;
import android.text.TextUtils;
import android.view.Menu;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import android.widget.ListView;
import android.os.Handler;
import android.provider.CallLog;
import android.database.ContentObserver;
import android.provider.CallLog.Calls;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.dialer.app.R;
import com.android.dialer.app.calllog.CallLogAsyncTaskUtil;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.database.CallLogQueryHandler;
import com.android.dialer.telecom.TelecomUtil;
import com.android.dialer.widget.EmptyContentView;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.dialer.activities.FreemeNeedTestActivity;

public class FreemeCallLogDetailsActivity extends FreemeNeedTestActivity {

    public static final String EXTRA_CALL_LOG_TYPE_FILTER = "call_log_type_filter";
    public static final String EXTRA_CALL_LOG_IDS = "call_log_ids";
    public static final String EXTRA_PHONE_NUMBERS = "phone_numbers";

    private FreemeCallDetailHistoryAdapter mHistoryAdapter;
    private ListView mHistoryList;

    private final ContentObserver mCallLogObserver = new CustomContentObserver();

    private class CustomContentObserver extends ContentObserver {
        public CustomContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            getCallLogsData();
        }
    }

    private int mCallTypeFilter;
    private String[] mNumbers;
    private long[] mCallIds;
    private EmptyContentView mEmptyListView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNumbers = getIntent().getStringArrayExtra(EXTRA_PHONE_NUMBERS);
        if (mNumbers == null || mNumbers.length <= 0) {
            finish();
            return;
        }

        mCallTypeFilter = getIntent().getIntExtra(EXTRA_CALL_LOG_TYPE_FILTER,
                CallLogQueryHandler.CALL_TYPE_ALL);

        mCallIds = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setElevation(1);
            actionBar.setTitle(getResources().getString(R.string.callHistoryIconLabel));
            FreemeActionBarUtil.setBackTitle(actionBar, getSubTitle(getIntent()));
        }

        setContentView(R.layout.freeme_call_log_detail_activity);

        mEmptyListView = (EmptyContentView) findViewById(R.id.empty_list_view);
        mEmptyListView.setImage(R.drawable.empty_call_log);
        mEmptyListView.setDescription(R.string.call_log_all_empty);
        mHistoryList = (ListView) findViewById(R.id.history);

        mHistoryAdapter = new FreemeCallDetailHistoryAdapter(this,
                mNumbers.length > 1);
        mHistoryList.setAdapter(mHistoryAdapter);


        getContentResolver().registerContentObserver(CallLog.CONTENT_URI, true,
                mCallLogObserver);
        getCallLogsData();
    }

    private void getCallDetails() {
        if (mNumbers == null || mNumbers.length <= 0) {
            mHistoryAdapter.changeCursor(null);
            return;
        }

        StringBuffer buffer = new StringBuffer()
                .append("number in (")
                .append(getNumberString(mNumbers))
                .append(")");

        if (mCallTypeFilter != CallLogQueryHandler.CALL_TYPE_ALL) {
            buffer.append(" and type = ").append(mCallTypeFilter);
            if (mCallTypeFilter == Calls.MISSED_TYPE) {
                buffer.append(" or type = " + Calls.REJECTED_TYPE);
            }
        }
        Cursor cursor = getContentResolver().query(Calls.CONTENT_URI, null,
                buffer.toString(), null, "date desc");
        mHistoryAdapter.changeCursor(cursor);
        if (mHistoryAdapter.getCount() > 0) {
            mEmptyListView.setVisibility(View.GONE);
        } else {
            mEmptyListView.setVisibility(View.VISIBLE);
        }

        invalidateOptionsMenu();

        if (cursor != null && cursor.getCount() <= 0) {
            cursor.close();
        }

    }

    private String getNumberString(String[] numbers) {
        StringBuffer numBuffer = new StringBuffer();
        for (String num : numbers) {
            if (numBuffer.length() > 0) {
                numBuffer.append(",");
            }
            // perhaps a special character
            numBuffer.append("\'" + num + "\'");
        }
        return numBuffer.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        getCallLogsData();
    }

    private void getCallLogsData() {
        getCallDetails();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mHistoryAdapter != null) {
            mHistoryAdapter.changeCursor(null);
        }
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mCallLogObserver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        if (onFreemeOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final int MENU_ITEM_ID_CLEAR = 0x100;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_ID_CLEAR, 0, R.string.freeme_contact_delete_positive_button)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    private boolean onFreemeOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ITEM_ID_CLEAR) {
            showDialogComfirmDelete();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteMenu = menu.findItem(MENU_ITEM_ID_CLEAR);
        boolean isEnable = (mHistoryAdapter != null && mHistoryAdapter.getCount() > 0);
        deleteMenu.setEnabled(isEnable);
        return true;
    }

    private void showDialogComfirmDelete() {
        AlertDialog.Builder build = new AlertDialog.Builder(FreemeCallLogDetailsActivity.this);
        build.setMessage(R.string.deleteCallLogConfirmation_title);
        build.setNegativeButton(android.R.string.cancel, null);
        build.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int position) {
                if (mCallIds != null && mCallIds.length > 0) {
                    final StringBuilder builder = new StringBuilder();
                    for (Uri callUri : getCallLogEntryUris()) {
                        if (builder.length() != 0) {
                            builder.append(",");
                        }
                        builder.append(ContentUris.parseId(callUri));
                    }
                    CallLogAsyncTaskUtil.deleteCalls(FreemeCallLogDetailsActivity.this,
                            builder.toString(), CallLogAsyncTaskUtil.DeleteType.CALL_IDS,
                            mCallLogAsyncTaskListener);
                }
                CallLogAsyncTaskUtil.deleteCalls(FreemeCallLogDetailsActivity.this,
                        getNumberString(mNumbers), CallLogAsyncTaskUtil.DeleteType.CALL_NUMBERS,
                        mCallLogAsyncTaskListener);
            }
        });
        build.show();
    }

    private CallLogAsyncTaskUtil.CallLogAsyncTaskListener mCallLogAsyncTaskListener = new
            CallLogAsyncTaskUtil.CallLogAsyncTaskListener() {
                @Override
                public void onDeleteCall() {
                    Toast.makeText(FreemeCallLogDetailsActivity.this,
                            R.string.freeme_toast_call_delete_success, Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onDeleteVoicemail() {

                }

                @Override
                public void onGetCallDetails(PhoneCallDetails[] details) {

                }
            };

    private Uri[] getCallLogEntryUris() {
        final int numIds = mCallIds == null ? 0 : mCallIds.length;
        final Uri[] uris = new Uri[numIds];
        for (int index = 0; index < numIds; ++index) {
            uris[index] = ContentUris.withAppendedId(TelecomUtil.getCallLogUri(this), mCallIds[index]);
        }
        return uris;
    }

    private String getSubTitle(Intent intent) {
        String title = null;
        if (intent != null) {
            title = intent.getStringExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT);
        }
        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.call_details);
        }

        return title;
    }
}