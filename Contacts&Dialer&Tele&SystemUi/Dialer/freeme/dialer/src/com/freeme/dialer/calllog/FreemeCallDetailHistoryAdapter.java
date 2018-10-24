package com.freeme.dialer.calllog;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.android.dialer.app.R;
import com.android.dialer.calllogutils.CallEntryFormatter;
import com.android.dialer.util.CallUtil;
import com.freeme.dialer.utils.FreemeCallTypeHelper;

public class FreemeCallDetailHistoryAdapter extends CursorAdapter {

    private final Context mContext;
    private FreemeCallTypeHelper mFreemeCallTypeHelper;
    private boolean mIsMultNumber;

    public FreemeCallDetailHistoryAdapter(Context context, boolean isMultNumber) {
        super(context, null, true);
        mContext = context;
        mIsMultNumber = /*isMultNumber*/true;
        mFreemeCallTypeHelper = new FreemeCallTypeHelper(mContext.getResources());
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;
        view = View.inflate(mContext, R.layout.freeme_calllog_details_item, null);
        MultiNumberCallLogsDetail holder = new MultiNumberCallLogsDetail(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view.getTag() instanceof CallLogsDetails) {
            ((CallLogsDetails) view.getTag()).bindView(cursor);
        }
    }

    private class MultiNumberCallLogsDetail extends CallLogsDetails {
        TextView calllog_header;
        TextView calllog_subHeader;
        TextView calllog_time;

        MultiNumberCallLogsDetail(View view) {
            calllog_header = (TextView) view.findViewById(R.id.calllog_header);
            calllog_subHeader = (TextView) view.findViewById(R.id.calllog_subHeader);
            calllog_time = (TextView) view.findViewById(R.id.calllog_time);
        }

        @Override
        void bindView(Cursor cursor) {
            int features = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.FEATURES));
            int callType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
            long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
            long duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));


            boolean isVideoCall = (features & CallLog.Calls.FEATURES_VIDEO) == CallLog.Calls.FEATURES_VIDEO
                    && CallUtil.isVideoEnabled(mContext);
            calllog_header.setText(mFreemeCallTypeHelper.getCallTypeText(callType, isVideoCall, false));
            if (duration > 0) {
                calllog_subHeader.setText(CallEntryFormatter.formatDurationAndDataUsage(mContext, duration
                        , 0));
            } else {
                calllog_subHeader.setText(mFreemeCallTypeHelper.getCallTypeText(callType));
            }
            CharSequence dateValue = DateUtils.formatDateRange(mContext, date, date,
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                            DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR);
            calllog_time.setText(dateValue);
        }
    }

    abstract class CallLogsDetails {
        abstract void bindView(Cursor cursor);
    }
}
