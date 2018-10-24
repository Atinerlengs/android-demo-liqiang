package com.freeme.dialer.contacts;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.dialer.contacts.list.service.FreemeMultiChoiceHandlerListener;
import com.freeme.dialer.contacts.list.service.FreemeMultiChoiceRequest;
import com.freeme.dialer.contacts.list.service.FreemeMultiChoiceService;
import com.mediatek.contacts.simcontact.SubInfoUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FreemeDeleteProcessor extends FreemeProcessorBase {
    private static final String TAG = "FreemeDeleteProcessor";

    private final FreemeMultiChoiceService mService;
    private final ContentResolver mResolver;
    private final List<FreemeMultiChoiceRequest> mRequests;
    private final int mJobId;
    private final FreemeMultiChoiceHandlerListener mListener;

    private PowerManager.WakeLock mWakeLock;

    private volatile boolean mIsCanceled;
    private volatile boolean mIsDone;
    private volatile boolean mIsRunning;

    private static final int MAX_OP_COUNT_IN_ONE_BATCH = 100;

    // change max count and max count in one batch for special operator
    private static final int MAX_COUNT = 1551;
    private static final int MAX_COUNT_IN_ONE_BATCH = 50;

    public FreemeDeleteProcessor(final FreemeMultiChoiceService service,
                                 final FreemeMultiChoiceHandlerListener listener,
                                 final List<FreemeMultiChoiceRequest> requests,
                                 final int jobId) {
        FreemeLogUtils.i(TAG, "[FreemeDeleteProcessor]new.");
        mService = service;
        mResolver = mService.getContentResolver();
        mListener = listener;

        mRequests = requests;
        mJobId = jobId;

        final PowerManager powerManager = (PowerManager) mService.getApplicationContext()
                .getSystemService("power");
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        FreemeLogUtils.i(TAG, "[cancel]mIsDone = " + mIsDone + ",mIsCanceled = " + mIsCanceled
                + ",mIsRunning = " + mIsRunning);
        if (mIsDone || mIsCanceled) {
            return false;
        }

        mIsCanceled = true;
        if (!mIsRunning) {
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(FreemeMultiChoiceService.TYPE_DELETE, mJobId, -1, -1, -1);
        } else {
            /*
             * Bug Fix by Mediatek Begin. Original Android's code: xxx CR ID:
             * ALPS00249590 Descriptions:
             */
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceling(FreemeMultiChoiceService.TYPE_DELETE, mJobId);
            /*
             * Bug Fix by Mediatek End.
             */
        }

        return true;
    }

    @Override
    public int getType() {
        return FreemeMultiChoiceService.TYPE_DELETE;
    }

    @Override
    public synchronized boolean isCancelled() {
        return mIsCanceled;
    }

    @Override
    public synchronized boolean isDone() {
        return mIsDone;
    }

    @Override
    public void run() {
        FreemeLogUtils.i(TAG, "[run].");
        try {
            mIsRunning = true;
            mWakeLock.acquire();
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            runInternal();
        } finally {
            synchronized (this) {
                mIsDone = true;
            }
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private void runInternal() {
        if (isCancelled()) {
            FreemeLogUtils.i(TAG, "[runInternal]Canceled before actually handling");
            return;
        }

        boolean succeessful = true;
        int totalItems = mRequests.size();
        int successfulItems = 0;
        int currentCount = 0;
        int iBatchDel = MAX_OP_COUNT_IN_ONE_BATCH;
        if (totalItems > MAX_COUNT) {
            iBatchDel = MAX_COUNT_IN_ONE_BATCH;
            FreemeLogUtils.i(TAG, "[runInternal]iBatchDel = " + iBatchDel);
        }
        long startTime = System.currentTimeMillis();
        final ArrayList<ArrayList<Long>> contactOpIdsList = new ArrayList<>();
        final ArrayList<Long> contactIdsList = new ArrayList<Long>();
        int times = 0;
        boolean simServiceStarted = false;

        int subId = -1;
        HashMap<Integer, Uri> delSimUriMap = new HashMap<Integer, Uri>();
        for (FreemeMultiChoiceRequest request : mRequests) {
            if (mIsCanceled) {
                FreemeLogUtils.d(TAG, "[runInternal] run: mCanceled = true, break looper");
                break;
            }
            currentCount++;

            mListener.onProcessed(FreemeMultiChoiceService.TYPE_DELETE, mJobId, currentCount, totalItems,
                    request.mContactName);
            FreemeLogUtils.d(TAG, "[runInternal]Indicator: " + request.mIndicator);
            // delete contacts from sim card
            if (request.mIndicator > 0) {
                subId = request.mIndicator;
                if (!isReadyForDelete(subId)) {
                    FreemeLogUtils.d(TAG, "[runInternal] run: isReadyForDelete(" + subId + ") = false");
                    succeessful = false;
                    continue;
                }

                // / M: change for SIM Service refactoring
                if (simServiceStarted || !simServiceStarted && FreemeSimServiceUtils.isServiceRunning(
                        mService.getApplicationContext(), subId)) {
                    FreemeLogUtils.d(TAG,
                            "[runInternal]sim service is running, skip all of sim contacts");
                    simServiceStarted = true;
                    succeessful = false;
                    continue;
                }

                Uri delSimUri = null;
                if (delSimUriMap.containsKey(subId)) {
                    delSimUri = delSimUriMap.get(subId);
                } else {
                    delSimUri = SubInfoUtils.getIccProviderUri(subId);
                    delSimUriMap.put(subId, delSimUri);
                }

                String where = ("index = " + request.mSimIndex);

                int deleteCount = mResolver.delete(delSimUri, where, null);
                if (deleteCount <= 0) {
                    FreemeLogUtils.d(TAG, "[runInternal] run: delete the sim contact failed");
                    succeessful = false;
                } else {
                    successfulItems++;
                    contactIdsList.add(request.mContactId);
                }
            } else {
                successfulItems++;
                contactIdsList.add(request.mContactId);
            }

            // delete contacts from database
            if (contactIdsList.size() >= iBatchDel) {
                contactOpIdsList.add(new ArrayList<>(contactIdsList));
                actualBatchDelete(contactIdsList);
                FreemeLogUtils.i(TAG, "[runInternal]the " + (++times) + ",iBatchDel = " + iBatchDel);
                contactIdsList.clear();
                if ((totalItems - currentCount) <= MAX_COUNT) {
                    iBatchDel = MAX_OP_COUNT_IN_ONE_BATCH;
                }
            }
        }

        if (contactIdsList.size() > 0) {
            contactOpIdsList.add(new ArrayList<>(contactIdsList));
            actualBatchDelete(contactIdsList);
            contactIdsList.clear();
        }

        FreemeLogUtils.d(TAG, "[runInternal]totaltime: " + (System.currentTimeMillis() - startTime));

        if (mIsCanceled) {
            FreemeLogUtils.d(TAG, "[runInternal]run: mCanceled = true, return");
            succeessful = false;
            mService.handleFinishNotification(mJobId, false);
            mListener.onCanceled(FreemeMultiChoiceService.TYPE_DELETE, mJobId, totalItems,
                    successfulItems, totalItems - successfulItems);
            return;
        }
        if (succeessful) {
            mListener.onFinished(FreemeMultiChoiceService.TYPE_DELETE, mJobId, totalItems);
            FreemeMultiChoiceService.STATUS = FreemeMultiChoiceService.STATUS_IDLE;
            for (ArrayList<Long> ids : contactOpIdsList) {
                actualBatchDelete(ids, true);
            }
            contactOpIdsList.clear();
        } else {
            mListener.onFailed(FreemeMultiChoiceService.TYPE_DELETE, mJobId, totalItems, successfulItems,
                    totalItems - successfulItems);
        }
        mService.handleFinishNotification(mJobId, succeessful);
    }

    private int actualBatchDelete(ArrayList<Long> contactsIds) {
        return actualBatchDelete(contactsIds, false);
    }

    private int actualBatchDelete(ArrayList<Long> contactsIds, boolean isRealDelete) {
        FreemeLogUtils.d(TAG, "[actualBatchDelete]isRealDelete: " + isRealDelete);
        if (contactsIds == null || contactsIds.size() == 0) {
            FreemeLogUtils.w(TAG, "[actualBatchDelete]input error,contactsIds = " + contactsIds);
            return 0;
        }

        final ArrayList<String> whereArgs = new ArrayList<>();
        for (long contactId : contactsIds) {
            whereArgs.add(String.valueOf(contactId));
        }

        int deleteCount = mResolver.delete(ContactsContract.Contacts.CONTENT_URI.buildUpon()
                        .appendQueryParameter("batch", "true")
                        .appendQueryParameter("isFreeme", "true")
                        .appendQueryParameter("isRealDelete", isRealDelete ? "true" : "false")
                        .build(),
                null, whereArgs.toArray(new String[0]));
        FreemeLogUtils.d(TAG, "[actualBatchDelete]deleteCount:" + deleteCount + " Contacts");
        return deleteCount;
    }

    private boolean isReadyForDelete(int subId) {
        return FreemeSimCardUtils.isSimStateIdle(mService.getApplicationContext(), subId);
    }
}
