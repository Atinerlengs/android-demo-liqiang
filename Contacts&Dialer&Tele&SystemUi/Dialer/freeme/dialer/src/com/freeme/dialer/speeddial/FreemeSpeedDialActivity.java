package com.freeme.dialer.speeddial;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.method.DialerKeyListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.dialer.R;

import java.util.ArrayList;

import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.dialer.speeddial.provider.FreemeSpeedDial;
import com.freeme.dialer.utils.FreemeDialerFeatureOptions;

public class FreemeSpeedDialActivity extends Activity implements View.OnClickListener,
        DialogInterface.OnClickListener, DialogInterface.OnShowListener {

    public static final String PREF_NAME = "speed_dial";
    private static final int QUERY_TOKEN = 47;
    private static final int VIEW_EDGE = 30;
    private static final int SPEED_DIAL_DIALOG_ADD = 1;

    private String TAG = "FreemeSpeedDialActivity";

    private SharedPreferences mPref;
    private int mQueryTimes;
    private ListView mListView;
    private SpeedDialQueryHandler mQueryHandler;
    private MatrixCursor mMatrixCursor;
    private SimpleCursorAdapter mSimpleCursorAdapter;

    private boolean mIsWaitingActivityResult;

    private int mAddPosition = -1;
    private int mRemovePosition = -1;

    static final int SPEED_DIAL_MIN = 2;
    static final int SPEED_DIAL_MAX = 9;
    private static final int LIST_CAPACITY = 9;
    private static final int VOICEMAIL_POSITION = 0;
    private static final int EMERGENCECALL_POSITION = 8;

    private ProgressDialog mProgressDialog = null;
    private static final int WAIT_CURSOR_START = 1000;
    private static final long WAIT_CURSOR_DELAY_TIME = 500;

    private boolean mActivityDestroyed = false;
    /// M: SOS implementation, to check for SOS support
    private boolean mIsSupportSOS = FreemeDialerFeatureOptions.isMtkSosQuickDialSupport();

    private SimReceiver mSimReceiver;
    private static final String ACTION_PHB_LOAD_FINISHED = "com.android.contacts.ACTION_PHB_LOAD_FINISHED";
    private static final String PACKAGENAME_OF_VOICEMAIL = "com.android.dialer";
    private static final String ACTIVITYNAME_OF_VOICEMAIL = "com.freeme.dialer.settings.FreemePhoneAccountAndVoicemailActivity";
    private static final String PACKAGENAME_OF_CONTACTSLECT = "com.android.contacts";
    private static final String ACTIVITYNAME_OF_CONTACTSLECT = "com.android.contacts.activities.ContactSelectionActivity";

    private static final int NUMBER_EMPTY_INDEX_COLOR = 0xff808080;
    private static final int NUMBER_NONEMPTY_INDEX_COLOR = 0xff333333;
    private static final int NUMBER_EMPTY_NAME_COLOR = 0xffcccccc;
    private static final int NUMBER_NONEMPTY_NAME_COLOR = 0xff333333;

    // For SharePreference
    private String[] mPrefNumState = new String[10];

    //For adapter
    public static final String[] DATA_FROM = {
            PhoneLookup._ID,
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.TYPE,
            PhoneLookup.NUMBER,
            PhoneLookup.HAS_PHONE_NUMBER
    };

    public static final int[] ID_TO = {
            R.id.sd_index,
            R.id.sd_name,
            R.id.sd_label,
            R.id.sd_number,
            R.id.sd_remove,
    };

    //For query
    static final String[] QUERY_PROJECTION = {
            PhoneLookup._ID, // 0
            PhoneLookup.DISPLAY_NAME, // 1
            PhoneLookup.TYPE, // 2
            PhoneLookup.NUMBER, // 3
            PhoneLookup.HAS_PHONE_NUMBER, // 4
            PhoneLookup.LABEL, // 5
    };

    static final int QUERY_DISPLAY_NAME_INDEX = 1;
    static final int QUERY_LABEL_INDEX = 2;
    static final int QUERY_NUMBER_INDEX = 3;
    static final int QUERY_HAS_PHONE_NUMBER = 4;
    static final int QUERY_CUSTOM_LABEL_INDEX = 5;

    static final int BIND_ID_INDEX = 0;
    static final int BIND_DISPLAY_NAME_INDEX = 1;
    static final int BIND_LABEL_INDEX = 2;
    static final int BIND_NUMBER_INDEX = 3;

    private static int REQUEST_CODE_PICK_CONTACT = 1;

    private ArrayList<QueueItem> mToastQueue = new ArrayList<QueueItem>();

    private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    public FreemeSpeedDialActivity() {
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        if (position == VOICEMAIL_POSITION) {
            Intent intent = new Intent();
            intent.setClassName(PACKAGENAME_OF_VOICEMAIL, ACTIVITYNAME_OF_VOICEMAIL);
            startActivity(intent);
            return;
        }
        if ((position == EMERGENCECALL_POSITION && mIsSupportSOS) || !TextUtils.isEmpty(mPrefNumState[position + 1])) {
            return;
        }

        mAddPosition = position;
        //showDialog(SPEED_DIAL_DIALOG_ADD);
        addContactToSpeedDial();
        return;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIsWaitingActivityResult = false;
        if (requestCode != REQUEST_CODE_PICK_CONTACT || RESULT_OK != resultCode || data == null) {
            return;
        }

        String index = data.getData().getLastPathSegment();
        String number = "";
        Cursor cursor = this.getContentResolver().query(Data.CONTENT_URI, new String[]{
                Data._ID, Data.DATA1
        }, Data._ID + " = " + index, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            number = cursor.getString(1);
        } else {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        cursor.close();

        if (findKeyByNumber(number) > -1) {
            mMatrixCursor.moveToPosition(-1);
            Toast.makeText(this, getString(R.string.freeme_speeddial_reselect_number), Toast.LENGTH_LONG).show();
            return;
        } else {
            getPrefStatus();
            mPrefNumState[mAddPosition + 1] = number;
            SharedPreferences.Editor editor = mPref.edit();
            editor.putString(String.valueOf(mAddPosition + 1), mPrefNumState[mAddPosition + 1]);
            editor.apply();

            updateSpeedDial(mAddPosition + 1, number);

            enQueueItem(mAddPosition);
        }
    }

    private int findKeyByNumber(String number) {
        for (int i = SPEED_DIAL_MIN; i < SPEED_DIAL_MAX + 1; i++) {
            if (shouldCollapse(this, number, mPrefNumState[i])) {
                return i;
            }
        }
        return -1;
    }

    public static final boolean shouldCollapse(Context context, CharSequence data1, CharSequence data2) {
        if (data1 == data2) {
            return true;
        }

        if (data1 == null || data2 == null) {
            return false;
        }

        if (TextUtils.equals(data1, data2)) {
            return true;
        }

        String[] dataParts1 = data1.toString().split(String.valueOf(PhoneNumberUtils.WAIT));
        String[] dataParts2 = data2.toString().split(String.valueOf(PhoneNumberUtils.WAIT));
        if (dataParts1.length != dataParts2.length) {
            return false;
        }

        for (int i = 0; i < dataParts1.length; i++) {
            if (!PhoneNumberUtils.compare(context, dataParts1[i], dataParts2[i])) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.freeme_speed_dial_listview);
        //mListView = getListView();
        mListView = (ListView) findViewById(R.id.freeme_speeddial_listview);
        mSimpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.freeme_speed_dial_list_item, null,
                DATA_FROM, ID_TO);
        mSimpleCursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(final View view, Cursor cursor, int columnIndex) {
                boolean isEmpty = TextUtils.isEmpty(cursor.getString(BIND_NUMBER_INDEX));
                int viewId = view.getId();
                if (viewId == R.id.sd_label) {
                    view.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                } else if (viewId == R.id.sd_number) {
                    view.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                } else if (viewId == R.id.sd_name) {
                    if (isEmpty) {
                        ((TextView) view).setTextColor(NUMBER_EMPTY_NAME_COLOR);
                    } else {
                        ((TextView) view).setTextColor(NUMBER_NONEMPTY_NAME_COLOR);
                    }

                } else if (viewId == R.id.sd_index) {
                    if (!isEmpty) {
                        ((TextView) view).setTextColor(NUMBER_NONEMPTY_INDEX_COLOR);
                    } else {
                        ((TextView) view).setTextColor(NUMBER_EMPTY_INDEX_COLOR);
                    }

                } else if (viewId == R.id.sd_remove) {
                    view.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    view.setOnClickListener(FreemeSpeedDialActivity.this);
                    if (!isEmpty) {
                        final View parent = (View) view.getParent();
                        parent.post(new Runnable() {
                            public void run() {
                                final Rect r = new Rect();
                                view.getHitRect(r);
                                r.top -= VIEW_EDGE;
                                r.bottom += VIEW_EDGE;
                                r.left -= VIEW_EDGE;
                                r.right += VIEW_EDGE;
                                parent.setTouchDelegate(new TouchDelegate(r, view));
                            }
                        });
                    } else {
                        final View parent = (View) view.getParent();
                        parent.post(new Runnable() {
                            public void run() {
                                parent.setTouchDelegate(null);
                            }
                        });
                    }
                    return true;
                }
                return false;
            }
        });

        mQueryHandler = new SpeedDialQueryHandler(this);
        mListView.setAdapter(mSimpleCursorAdapter);
        mListView.setOnItemClickListener(mOnClickListener);

        mSimReceiver = new SimReceiver(this);
        mSimReceiver.register();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
            FreemeActionBarUtil.setBackTitle(actionBar, getSubTitle(getIntent()));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPrefStatus();
        startQuery();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        dismissProgressIndication();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSimReceiver.unregister();
        if (mMatrixCursor != null) {
            mMatrixCursor.close();
        }
        mActivityDestroyed = true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (SPEED_DIAL_DIALOG_ADD == id) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.freeme_speeddial_call_speed_dial);
            builder.setPositiveButton(R.string.freeme_speeddial_sd_add, this);
            builder.setNegativeButton(android.R.string.cancel, this);
            builder.setView(View.inflate(this, R.layout.freeme_mtk_speed_dial_input_dialog, null));
            Dialog dialog = builder.create();
            dialog.setOnShowListener(this);
            return dialog;
        }
        return null;
    }

    private void getPrefStatus() {
        mPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        for (int i = SPEED_DIAL_MIN; i < SPEED_DIAL_MAX + 1; ++i) {
            mPrefNumState[i] = mPref.getString(String.valueOf(i), "");
        }
    }

    private void initMatrixCursor() {
        mMatrixCursor = new MatrixCursor(DATA_FROM, LIST_CAPACITY);
        mMatrixCursor.addRow(new String[]{
                String.valueOf(VOICEMAIL_POSITION + 1), getResources().getString(R.string.voicemail), "", "", ""
        });
        mQueryTimes = SPEED_DIAL_MIN;
    }

    private void startQuery() {
        mDialogHandler.sendMessageDelayed(mDialogHandler.obtainMessage(WAIT_CURSOR_START),
                WAIT_CURSOR_DELAY_TIME);

        initMatrixCursor();
        goOnQuery();
    }

    private void goOnQuery() {

        int end;
        for (end = mQueryTimes; end < SPEED_DIAL_MAX + 1 && TextUtils.isEmpty(mPrefNumState[end]); ++end) {
            populateMatrixCursorEmpty(this, mMatrixCursor, end, "");
        }
        if (end > SPEED_DIAL_MAX) {
            mSimpleCursorAdapter.changeCursor(mMatrixCursor);
            mSimpleCursorAdapter.notifyDataSetChanged();

            processQueue();
            updatePreferences();

            mDialogHandler.removeMessages(WAIT_CURSOR_START);
            dismissProgressIndication();
        } else {

            QueryInfo info = new QueryInfo();
            mQueryTimes = end;
            info.mQueryIndex = mQueryTimes;

            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri
                    .encode(mPrefNumState[mQueryTimes]));
            mQueryHandler.startQuery(QUERY_TOKEN, null, uri, QUERY_PROJECTION, null, null, null);
        }
    }

    private void populateMatrixCursorEmpty(Context cnx, MatrixCursor cursor, int id, String number) {
        if (TextUtils.isEmpty(number)) {
            if (id == (EMERGENCECALL_POSITION + 1) && mIsSupportSOS) {
                cursor.addRow(new String[]{
                        String.valueOf(EMERGENCECALL_POSITION + 1),
                        cnx.getResources().getString(R.string.freeme_speeddial_emergency_call), "", "", "-1"});
            } else {
                cursor.addRow(new String[]{
                        String.valueOf(id),
                        cnx.getResources().getString(R.string.freeme_speeddial_tap_to_add), "", "", "-1"});
            }
        } else {
            cursor.addRow(new String[]{
                    String.valueOf(id),
                    number, "", number, "-1"});
        }
    }

    //Need update lable && simcard icon
    private void populateMatrixCursorRow(int row, Cursor cursor) {
        cursor.moveToFirst();
        String name = cursor.getString(QUERY_DISPLAY_NAME_INDEX);
        int type = cursor.getInt(QUERY_LABEL_INDEX);
        String label = "";
        if (type == 0) {
            label = cursor.getString(QUERY_CUSTOM_LABEL_INDEX);
        } else {
            label = (String) CommonDataKinds.Phone.getTypeLabel(getResources(), type, null);
        }
        String number = cursor.getString(QUERY_NUMBER_INDEX);
        int simId = -1;
        if (!cursor.isNull(QUERY_HAS_PHONE_NUMBER)) {
            simId = cursor.getInt(QUERY_HAS_PHONE_NUMBER);
        }

        if (simId > 0) {
            //photoId = SimContactPhotoUtils.getSimContactPhotoId(simId, false);
        }

        if (TextUtils.isEmpty(number)) {
            populateMatrixCursorEmpty(this, mMatrixCursor, row, "");
            mPrefNumState[row] = mPref.getString(String.valueOf(row), "");
            updateSpeedDial(row, "");
            return;
        }
        mMatrixCursor.addRow(new String[]{
                String.valueOf(row), name, label,
                number, String.valueOf(simId)});
    }

    private void showEditDialog(int clickposition) {
        String[] items = new String[3];
        Resources mResource = this.getResources();
        items[0] = mResource.getString(R.string.freeme_speeddial_edit_speed_dial);
        items[1] = mResource.getString(R.string.freeme_speeddial_delete_number_setting);
        items[2] = mResource.getString(R.string.freeme_speeddial_modify_setted_number);
        FreemeBottomDialog bottomDialog = FreemeBottomDialog.newInstance(items);
        bottomDialog.show(getFragmentManager(), items[2]);
        bottomDialog.setListener(new FreemeBottomDialog.OnClickListener() {
            @Override
            public void onClick(int position) {
                switch (position) {
                    case 1:
                        confirmRemovePosition(clickposition + mListView.getFirstVisiblePosition());
                        break;
                    case 2:
                        mAddPosition = clickposition;
                        addContactToSpeedDial();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void onClick(View v) {
        if (v.getId() == R.id.sd_remove) {
            for (int i = 0, size = mListView.getCount(); i < size; i++) {
                if (mListView.getChildAt(i) == v.getParent()) {
                    showEditDialog(i);
                    return;
                }
            }
        } else if (v.getId() == R.id.contacts) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            ComponentName component = new ComponentName(PACKAGENAME_OF_CONTACTSLECT, ACTIVITYNAME_OF_CONTACTSLECT);
            intent.setComponent(component);
            intent.setType(Phone.CONTENT_ITEM_TYPE);
            startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);

            dismissDialog(SPEED_DIAL_DIALOG_ADD);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            EditText editText = (EditText) ((AlertDialog) dialog).findViewById(R.id.number);
            final String number = editText.getText().toString();
            if (TextUtils.isEmpty(number)) {
                return;
            }

            if (-1 == findKeyByNumber(number)) {
                mPrefNumState[mAddPosition + 1] = number;

                SharedPreferences.Editor editor = mPref.edit();
                editor.putString(String.valueOf(mAddPosition + 1), number);
                editor.commit();

                updateSpeedDial(mAddPosition + 1, number);

                startQuery();

                enQueueItem(mAddPosition);
            } else {
                Toast.makeText(this, getString(R.string.freeme_speeddial_reselect_number), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onShow(DialogInterface dialog) {
        EditText editText = (EditText) ((AlertDialog) dialog).findViewById(R.id.number);
        if (!TextUtils.isEmpty(mPrefNumState[mAddPosition + 1])) {
            editText.setText(mPrefNumState[mAddPosition + 1]);
            editText.setSelection(mPrefNumState[mAddPosition + 1].length());
        } else {
            editText.setText("");
        }

        editText.setKeyListener(InputKeyListener.getInstance());
        ImageView imageView = (ImageView) ((AlertDialog) dialog).findViewById(R.id.contacts);
        imageView.setOnClickListener(this);
    }


    public void confirmRemovePosition(int position) {
        if (position < SPEED_DIAL_MIN - 1 && position > SPEED_DIAL_MAX) {
            return;
        }

        Cursor c = (Cursor) mSimpleCursorAdapter.getItem(position);
        if (c == null) {
            return;
        }

        mRemovePosition = position;
        actuallyRemove();
    }

    private void updatePreferences() {
        SharedPreferences.Editor editor = mPref.edit();
        for (int i = SPEED_DIAL_MIN, count = SPEED_DIAL_MAX + 1; i < count; ++i) {
            editor.putString(String.valueOf(i), mPrefNumState[i]);
        }
        editor.apply();
    }

    private void actuallyRemove() {
        mPrefNumState[mRemovePosition + 1] = "";
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(String.valueOf(mRemovePosition + 1), mPrefNumState[mRemovePosition + 1]);
        editor.apply();

        startQuery();

        updateSpeedDial(mRemovePosition + 1, "");
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mAddPosition = state.getInt("add_position", -1);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mAddPosition != -1) {
            outState.putInt("add_position", mAddPosition);
        }
        super.onSaveInstanceState(outState);
    }

    private void enQueueItem(int index) {
        mToastQueue.add(new QueueItem(index));
    }

    private void processQueue() {
        if (mToastQueue != null) {
            for (QueueItem item : mToastQueue) {
                item.run();
            }
            mToastQueue.clear();
        }
    }

    private Handler mDialogHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WAIT_CURSOR_START:
                    showProgressIndication();
                    break;
                default:
                    break;
            }
        }
    };

    private void showProgressIndication() {
        if (mActivityDestroyed) {
            return;
        }

        dismissProgressIndication(); // Clean up any prior progress indication

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(this.getResources().getString(R.string.contact_list_loading));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void dismissProgressIndication() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            try {
                mProgressDialog.dismiss(); // safe even if already dismissed
            } catch (Exception e) {
                Log.i(TAG, "dismiss exception: " + e);
            }
            mProgressDialog = null;
        }
    }


    private class SpeedDialQueryHandler extends AsyncQueryHandler {
        SpeedDialQueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            if (cookie instanceof QueryInfo) {
                int index = ((QueryInfo) cookie).mQueryIndex;
                if (index != mQueryTimes) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    return;
                }
            }

            if (mQueryTimes < SPEED_DIAL_MAX + 1 && cursor != null && cursor.getCount() > 0) {
                populateMatrixCursorRow(mQueryTimes, cursor);
            } else if (mQueryTimes < SPEED_DIAL_MAX + 1) {
                populateMatrixCursorEmpty(FreemeSpeedDialActivity.this, mMatrixCursor, mQueryTimes, mPrefNumState[mQueryTimes]);
            }

            if (cursor != null) {
                cursor.close();
            }

            if (mQueryTimes < SPEED_DIAL_MAX) {
                mQueryTimes++;
                goOnQuery();
            } else {
                mSimpleCursorAdapter.changeCursor(mMatrixCursor);
                mSimpleCursorAdapter.notifyDataSetChanged();

                processQueue();
                updatePreferences();

                mDialogHandler.removeMessages(WAIT_CURSOR_START);
                dismissProgressIndication();
            }
        }
    }

    private void updateSpeedDial(int id, String number) {
        SpeedDialArgs args = new SpeedDialArgs(FreemeSpeedDialActivity.this, id, number,
                FreemeSpeedDial.Numbers.CONTENT_URI);
        new UpdateSpeedDialTask().execute(args);
    }

    private class SpeedDialArgs {
        public SpeedDialArgs(Context context, int id, String number, Uri uri) {
            this.mContext = context;
            this.mIndex = id;
            this.mNumber = number;
            this.mUri = uri;
        }

        public Uri mUri;
        public Context mContext;
        public int mIndex;
        public String mNumber;
    }

    private class UpdateSpeedDialTask extends AsyncTask<SpeedDialArgs, Void, Void> {
        @Override
        protected Void doInBackground(SpeedDialArgs... argList) {
            int count = argList.length;
            for (int i = 0; i < count; i++) {
                SpeedDialArgs arg = argList[i];
                Context context = arg.mContext;
                ContentValues value = new ContentValues();
                value.put(FreemeSpeedDial.Numbers.NUMBER, arg.mNumber);
                context.getContentResolver().update(arg.mUri, value, FreemeSpeedDial.Numbers._ID + " = " + arg.mIndex, null);
            }
            return null;
        }
    }

    private static class InputKeyListener extends DialerKeyListener {
        private static InputKeyListener sKeyListener;
        public static final char[] CHARACTERS = new char[]{'0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', '+', '*', '#', ',', ';'};

        @Override
        protected char[] getAcceptedChars() {
            return CHARACTERS;
        }

        public static InputKeyListener getInstance() {
            if (sKeyListener == null) {
                sKeyListener = new InputKeyListener();
            }
            return sKeyListener;
        }
    }

    private class QueueItem {
        private int index;
        private Runnable runnable;

        QueueItem(int id) {
            this.index = id;
            this.initialize();
        }

        private void initialize() {
            this.runnable = new Runnable() {
                @Override
                public void run() {

                    mMatrixCursor.moveToPosition(QueueItem.this.index);
                    if (QueueItem.this.index < SPEED_DIAL_MIN - 1) {
                        return;
                    }

                    CharSequence name = mMatrixCursor.getString(BIND_DISPLAY_NAME_INDEX);
                    CharSequence label = mMatrixCursor.getString(BIND_LABEL_INDEX);

                    CharSequence fullInfo;
                    if (TextUtils.isEmpty(label)) {
                        fullInfo = getString(R.string.freeme_speeddial_added2, name, String.valueOf(QueueItem.this.index + 1));
                    } else {
                        fullInfo = getString(R.string.freeme_speeddial_added, name, label, String.valueOf(QueueItem.this.index + 1));
                    }
                    Toast.makeText(FreemeSpeedDialActivity.this, fullInfo, Toast.LENGTH_LONG).show();
                }
            };
        }

        public void run() {
            this.runnable.run();
        }
    }

    private class QueryInfo {
        int mQueryIndex;
    }

    private class SimReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUi();
        }

        private void refreshUi() {
            mRefreshHandler.sendEmptyMessage(0);
        }

        public void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_PHB_LOAD_FINISHED);
            mContext.registerReceiver(this, filter);
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }

        public SimReceiver(Context context) {
            mContext = context;
        }

        private Context mContext;
        private Handler mRefreshHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                initMatrixCursor();
                goOnQuery();
            }
        };
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

    private void addContactToSpeedDial() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        ComponentName component = new ComponentName(PACKAGENAME_OF_CONTACTSLECT, ACTIVITYNAME_OF_CONTACTSLECT);
        intent.setComponent(component);
        intent.setType(Phone.CONTENT_ITEM_TYPE);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }
}
