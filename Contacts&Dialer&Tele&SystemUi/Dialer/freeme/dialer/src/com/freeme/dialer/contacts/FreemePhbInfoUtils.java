package com.freeme.dialer.contacts;

import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telephony.SubscriptionInfo;

import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;

import java.util.HashMap;
import java.util.List;

public class FreemePhbInfoUtils {

    private static final String TAG = "FreemePhbInfoUtils";

    private static final int DEFAULT_SUBINFO_COUNT = 0;
    private static HashMap<Integer, PhbInfoWrapper> mActiveUsimPhbInfoMap = null;

    private FreemePhbInfoUtils() {
    }

    private final static class PhbInfoWrapper {
        private int mSubId = SubInfoUtils.getInvalidSubId();
        private int mUsimGroupMaxNameLength;
        private int mUsimGroupCount;
        private int mUsimAnrCount;
        private int mUsimEmailCount;
        // add for Aas&Sne
        private int mUsimAasCount;
        private int mUsimAasMaxNameLength;
        private int mUsimSneMaxNameLength;
        private boolean mHasSne;
        private boolean mInitialized;
        private static final int INFO_NOT_READY = -1;

        public PhbInfoWrapper(int subId) {
            mSubId = subId;
            resetPhbInfo();
            if (FreemeContactDeletionInteraction.isMtkPhoneBookSupport()) {
                refreshPhbInfo();
            }
        }

        private void resetPhbInfo() {
            mUsimGroupMaxNameLength = INFO_NOT_READY;
            mUsimGroupCount = INFO_NOT_READY;
            mUsimAnrCount = INFO_NOT_READY;
            mUsimEmailCount = INFO_NOT_READY;
            mInitialized = false;
            // add for Aas&Sne
            mUsimAasCount = INFO_NOT_READY;
            mUsimAasMaxNameLength = INFO_NOT_READY;
            mUsimSneMaxNameLength = INFO_NOT_READY;
            mHasSne = false;
        }

        private void refreshPhbInfo() {
            FreemeLogUtils.i(TAG, "[refreshPhbInfo]refreshing phb info for subId: " + mSubId);
            if (!FreemeSimCardUtils.isPhoneBookReady(mSubId)) {
                FreemeLogUtils.e(TAG, "[refreshPhbInfo]phb not ready, refresh aborted. slot: " + mSubId);
                mInitialized = false;
                return;
            }
            // /TODO: currently, Usim or Csim is necessary for phb infos.
            if (!FreemeSimCardUtils.isUsimOrCsimType(mSubId)) {
                FreemeLogUtils.i(TAG, "[refreshPhbInfo]not usim phb, nothing to refresh, keep default "
                        + ", subId: " + mSubId);
                mInitialized = true;
                return;
            }

            if (!mInitialized) {
                new FreemePhbInfoUtils.GetSimInfoTask(this).execute(mSubId);
            }
        }

        private int getUsimAnrCount() {
            if (!mInitialized) {
                refreshPhbInfo();
            }
            FreemeLogUtils.d(TAG, "[getUsimAnrCount] subId = " + mSubId
                    + ", count = " + mUsimAnrCount);
            return mUsimAnrCount;
        }
    }

    private static final class GetSimInfoTask extends AsyncTask<Integer, Void, PhbInfoWrapper> {
        private PhbInfoWrapper mPhbInfoWrapper;
        private static final long QUERY_TOTAL_TIME = 2 * 60 * 1000;
        private static final long QUERY_INTERVAL = 5 * 1000;

        public GetSimInfoTask(PhbInfoWrapper phbInfoWrapper) {
            mPhbInfoWrapper = phbInfoWrapper;
        }

        @Override
        protected PhbInfoWrapper doInBackground(Integer... params) {
            final int subId = params[0];
            FreemeLogUtils.d(TAG, "[GetSimInfoTask] subId = " + subId);
            String serviceName = SubInfoUtils.getMtkPhoneBookServiceName();
            try {
                final IMtkIccPhoneBook iIccPhb = IMtkIccPhoneBook.Stub.asInterface(ServiceManager
                        .getService(serviceName));
                if (iIccPhb == null) {
                    FreemeLogUtils.e(TAG, "[GetSimInfoTask] IIccPhoneBook is null!");
                    mPhbInfoWrapper.mInitialized = false;
                    return null;
                }
                ///M: [ALPS03737363] do while to query until all phb info ready for caching
                ///them ASAP. And then Editor can show up all sim data kind earlier.
                long start = SystemClock.elapsedRealtime();
                do {
                    mPhbInfoWrapper.mUsimGroupMaxNameLength = iIccPhb.getUsimGrpMaxNameLen(subId);
                    mPhbInfoWrapper.mUsimGroupCount = iIccPhb.getUsimGrpMaxCount(subId);
                    mPhbInfoWrapper.mUsimAnrCount = iIccPhb.getAnrCount(subId);
                    mPhbInfoWrapper.mUsimEmailCount = iIccPhb.getEmailCount(subId);
                    mPhbInfoWrapper.mHasSne = iIccPhb.hasSne(subId);
                    mPhbInfoWrapper.mUsimAasCount = iIccPhb.getUsimAasMaxCount(subId);
                    mPhbInfoWrapper.mUsimAasMaxNameLength = iIccPhb.getUsimAasMaxNameLen(subId);
                    mPhbInfoWrapper.mUsimSneMaxNameLength = iIccPhb.getSneRecordLen(subId);
                    if (PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimGroupMaxNameLength
                            || PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimGroupCount
                            || PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimAnrCount
                            || PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimEmailCount
                            || PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimAasCount
                            || PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimAasMaxNameLength
                            || PhbInfoWrapper.INFO_NOT_READY == mPhbInfoWrapper.mUsimSneMaxNameLength) {
                        mPhbInfoWrapper.mInitialized = false;
                        FreemeLogUtils.d(TAG, "[GetSimInfoTask] Initialize = false. Not all info ready,"
                                + "still need query next time");
                        try {
                            Thread.sleep(QUERY_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mPhbInfoWrapper.mInitialized = true;
                        FreemeLogUtils.d(TAG, "[GetSimInfoTask] Initialize = true");
                    }
                } while (!mPhbInfoWrapper.mInitialized
                        && (SystemClock.elapsedRealtime() - start) <= QUERY_TOTAL_TIME);
            } catch (RemoteException e) {
                FreemeLogUtils.e(TAG, "[GetSimInfoTask]Exception happened when refreshing phb info");
                e.printStackTrace();
                mPhbInfoWrapper.mInitialized = false;
                return null;
            }

            FreemeLogUtils.i(TAG, "[GetSimInfoTask]refreshing done,UsimGroupMaxNameLenght = "
                    + mPhbInfoWrapper.mUsimGroupMaxNameLength
                    + ", UsimGroupMaxCount = " + mPhbInfoWrapper.mUsimGroupCount
                    + ", UsimAnrCount = " + mPhbInfoWrapper.mUsimAnrCount
                    + ", UsimEmailCount = " + mPhbInfoWrapper.mUsimEmailCount
                    + ", mHasSne = " + mPhbInfoWrapper.mHasSne
                    + ", mUsimAasMaxCount = " + mPhbInfoWrapper.mUsimAasCount
                    + ", mUsimAasMaxNameLength = " + mPhbInfoWrapper.mUsimAasMaxNameLength
                    + ", mUsimSneMaxNameLength = " + mPhbInfoWrapper.mUsimSneMaxNameLength);
            return mPhbInfoWrapper;
        }
    }

    public static HashMap<Integer, PhbInfoWrapper> getActiveUsimPhbInfoMap() {
        if (mActiveUsimPhbInfoMap == null) {
            mActiveUsimPhbInfoMap = new HashMap<Integer, PhbInfoWrapper>();
            List<SubscriptionInfo> subscriptionInfoList = SubInfoUtils.getActivatedSubInfoList();
            FreemeLogUtils.d(TAG, "[getActiveUsimPhbInfoMap] subscriptionInfoList: " + subscriptionInfoList);
            if (subscriptionInfoList != null && subscriptionInfoList.size() > 0) {
                for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                    mActiveUsimPhbInfoMap.put(subscriptionInfo.getSubscriptionId(),
                            new PhbInfoWrapper(subscriptionInfo.getSubscriptionId()));
                }
            }
        }
        return mActiveUsimPhbInfoMap;
    }

    public static int getUsimAnrCount(int subId) {
        PhbInfoWrapper usimPhbInfo = getActiveUsimPhbInfoMap().get(subId);
        if (null == usimPhbInfo) {
            return DEFAULT_SUBINFO_COUNT;
        }
        int count = usimPhbInfo.getUsimAnrCount();
        if (PhbInfoWrapper.INFO_NOT_READY == count) {
            return DEFAULT_SUBINFO_COUNT;
        }
        return count;
    }
}
