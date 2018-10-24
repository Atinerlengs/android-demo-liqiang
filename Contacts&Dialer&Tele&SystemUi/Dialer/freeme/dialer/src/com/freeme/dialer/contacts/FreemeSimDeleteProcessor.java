package com.freeme.dialer.contacts;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SubscriptionManager;

import com.freeme.contacts.common.utils.FreemeLogUtils;

public class FreemeSimDeleteProcessor extends FreemeSimProcessorBase {
    private final static String TAG = "FreemeSimDeleteProcessor";

    private static Listener mListener = null;

    private Uri mSimUri = null;
    private Uri mLocalContactUri = null;
    private int mSimIndex = -1;
    private Context mContext;
    private Intent mIntent;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    public final static String SIM_INDEX = "sim_index";
    public final static String LOCAL_CONTACT_URI = "local_contact_uri";
    public static final String SERVICE_SUBSCRIPTION_KEY = "subscription_key";
    public static final String SERVICE_WORK_TYPE = "work_type";
    public static final int SERVICE_WORK_DELETE = 2;

    public interface Listener {
        public void onSIMDeleteFailed();

        public void onSIMDeleteCompleted();
    }

    public static void registerListener(Listener listener) {
        if (listener instanceof FreemeContactDeletionInteraction) {
            FreemeLogUtils.i(TAG, "[registerListener]listener added to SIMDeleteProcessor:" + listener);
            mListener = listener;
        }
    }

    public static void unregisterListener(Listener listener) {
        FreemeLogUtils.i(TAG, "[unregisterListener]removed from SIMDeleteProcessor: " + listener);
        mListener = null;
    }

    public FreemeSimDeleteProcessor(Context context, int subId, Intent intent,
                                    FreemeSimProcessorManager.ProcessorCompleteListener listener) {
        super(intent, listener);
        FreemeLogUtils.i(TAG, "[FreemeSimDeleteProcessor]new...");
        mContext = context;
        mSubId = subId;
        mIntent = intent;
    }

    @Override
    public int getType() {
        return FreemeSimServiceUtils.SERVICE_WORK_DELETE;
    }

    @Override
    public void doWork() {
        if (isCancelled()) {
            FreemeLogUtils.w(TAG, "[dowork]cancel remove work. Thread id = "
                    + Thread.currentThread().getId());
            return;
        }
        mSimUri = mIntent.getData();
        mSimIndex = mIntent.getIntExtra(SIM_INDEX, -1);
        mLocalContactUri = mIntent.getParcelableExtra(LOCAL_CONTACT_URI);
        if (mContext.getContentResolver().delete(mSimUri, "index = " + mSimIndex, null) <= 0) {
            FreemeLogUtils.i(TAG, "[doWork] Delete SIM contact failed");
            if (mListener != null) {
                mListener.onSIMDeleteFailed();
            }
        } else {
            FreemeLogUtils.i(TAG, "[doWork] Delete SIM contact successfully");
            mContext.startService(FreemeContactDeleteService.createDeleteContactIntent(mContext,
                    mLocalContactUri));
            if (mListener != null) {
                mListener.onSIMDeleteCompleted();
            }
        }
    }

    public static boolean doDeleteSimContact(Context context, Uri contactUri, Uri simUri,
                                             int simIndex, int subId, Fragment fragment) {
        FreemeLogUtils.i(TAG, "[doDeleteSimContact]simUri: " + simUri + ",simIndex = " + simIndex
                + ",subId = " + subId);
        if (simUri != null && fragment.isAdded()) {
            Intent intent = new Intent(context, FreemeSimProcessorService.class);
            intent.setData(simUri);
            intent.putExtra(SIM_INDEX, simIndex);
            intent.putExtra(SERVICE_SUBSCRIPTION_KEY, subId);
            intent.putExtra(SERVICE_WORK_TYPE, SERVICE_WORK_DELETE);
            intent.putExtra(LOCAL_CONTACT_URI, contactUri);
            context.startService(intent);
            return true;
        }
        return false;
    }
}
