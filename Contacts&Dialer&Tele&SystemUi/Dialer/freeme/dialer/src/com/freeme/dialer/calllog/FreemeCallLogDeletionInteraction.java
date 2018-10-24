package com.freeme.dialer.calllog;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;

import com.android.dialer.app.R;
import com.android.dialer.database.CallLogQueryHandler;
import com.freeme.contacts.common.utils.FreemeLogUtils;

import java.util.HashMap;

public class FreemeCallLogDeletionInteraction extends Fragment
        implements CallLogQueryHandler.Listener {

    public interface MultiCallLogDeleteListener {
        public void onDeletionFinished();
    }

    private static final String TAG = "FreemeCallLogDeletionInteraction";
    private static final String FRAGMENT_TAG = "DELETE_CALL_LOG";

    private HashMap<Long, long[]> mSelectedCallIds;
    private AlertDialog mDialog;
    private ProgressDialog mProgressDialog;
    private CallLogQueryHandler mCallLogQueryHandler;
    private MultiCallLogDeleteListener mListener;
    private boolean mIsActive = false;

    public static FreemeCallLogDeletionInteraction start(Fragment hostFragment,
                                                         HashMap<Long, long[]> selectedCallIds,
                                                         MultiCallLogDeleteListener listener) {
        final FragmentManager fragmentManager = hostFragment.getFragmentManager();
        FreemeCallLogDeletionInteraction fragment =
                (FreemeCallLogDeletionInteraction) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            FreemeLogUtils.i(TAG, "[start] new...");
            fragment = new FreemeCallLogDeletionInteraction();
            fragment.setCallLogIds(selectedCallIds);
            fragment.setMultiCallLogDeleteListener(listener);
            fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG)
                    .commitAllowingStateLoss();
        } else {
            fragment.setCallLogIds(selectedCallIds);
            fragment.setMultiCallLogDeleteListener(listener);

            fragment.showDialog();
        }
        return fragment;
    }

    public void setCallLogIds(HashMap<Long, long[]> selectedCallIds) {
        FreemeLogUtils.i(TAG, "[setContactIds]");
        mSelectedCallIds = selectedCallIds;
        mIsActive = true;
    }

    public void setMultiCallLogDeleteListener(MultiCallLogDeleteListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        showDialog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mIsActive = false;
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.setOnDismissListener(null);
            mDialog.dismiss();
            mDialog = null;
        }
    }

    public void showDialog() {
        if (!mIsActive) {
            return;
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.deleteCallLogConfirmation_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.deleteCallLogConfirmation_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok,
                        (DialogInterface dialog, int which) -> {
                            deleteSelectedCallItems();
                        })
                .setOnDismissListener((DialogInterface dialog) -> {
                    mIsActive = false;
                })
                .create();
        mDialog.show();
    }

    private void deleteSelectedCallItems() {
        if (mSelectedCallIds.size() > 0) {
            mProgressDialog = ProgressDialog.show(getActivity(), "",
                    getString(com.android.dialer.R.string.deleting_call_log));
        }
        if (mCallLogQueryHandler == null) {
            Context context = getContext();
            mCallLogQueryHandler = new CallLogQueryHandler(context, context.getContentResolver(),
                    this);
        }

        StringBuilder where = new StringBuilder("_id in ");
        where.append("(");
        if (mSelectedCallIds.size() > 0) {
            boolean isFirst = true;
            for (long rawId : mSelectedCallIds.keySet()) {
                for (long callLogId : mSelectedCallIds.get(rawId)) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        where.append(",");
                    }
                    where.append("\'");
                    where.append(callLogId);
                    where.append("\'");
                }
            }
        } else {
            where.append(-1);
        }
        where.append(")");

        mCallLogQueryHandler.deleteSpecifiedCalls(where.toString());
    }

    @Override
    public void onVoicemailStatusFetched(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    public void onVoicemailUnreadCountFetched(Cursor cursor) {

    }

    @Override
    public void onMissedCallsUnreadCountFetched(Cursor cursor) {

    }

    @Override
    public boolean onCallsFetched(Cursor combinedCursor) {
        return false;
    }

    @Override
    public void onCallsDeleted() {
        if (null != mProgressDialog) {
            mProgressDialog.dismiss();
        }
        if (mListener != null) {
            mListener.onDeletionFinished();
        }
    }

    private static final String KEY_CALL_IDS = "callIds";

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_CALL_IDS, mSelectedCallIds);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mSelectedCallIds = (HashMap<Long, long[]>) savedInstanceState.getSerializable(KEY_CALL_IDS);
        }
    }
}
