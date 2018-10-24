package com.freeme.dialer.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import com.android.internal.telephony.PhoneConstants;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.mediatek.internal.telephony.MtkTelephonyIntents;

import java.util.ArrayList;
import java.util.List;

public class FreemeGeneralEventHandler {
    private static final String TAG = "FreemeGeneralEventHandler";
    private static final String SDCARD_DATA_SCHEME = "file";
    public static final int ERROR_SUB_ID = -1000;

    public static final class SdCardState {
        public static final String SDSTEATE = "sdstate";
        public static final int SDCARD_ERROR = -1;
        public static final int SDCARD_REMOVED = 1;
        public static final int SDCARD_MOUNTED = 2;
    }

    public static final class PhbState {
        public static final String PHBREADY = "ready";
    }

    public static final class EventType {
        public static final String PHB_STATE_CHANGE_EVENT = "PhbChangeEvent";
        public static final String SD_STATE_CHANGE_EVENT = "SdStateChangeEvenet";
    }

    public interface Listener {

        /**
         * the callback to handle base event.
         *
         * @param eventType receive event type
         * @param extraData the related data in eventType
         */
        public void onReceiveEvent(String eventType, Intent extraData);
    }

    private List<FreemeGeneralEventHandler.Listener> mListeners = new ArrayList<FreemeGeneralEventHandler.Listener>();
    private Context mContext;
    private boolean mRegistered = false;
    private volatile static FreemeGeneralEventHandler uniqueInstance;

    /**
     * get the instance of the BaseEventHandler.
     * using double-checked locking for mutil-thread condition to spend
     * minimum time to get instance
     */
    public static FreemeGeneralEventHandler getInstance(Context context) {
        if (null == uniqueInstance) {
            synchronized (FreemeGeneralEventHandler.class) {
                if (null == uniqueInstance) {
                    uniqueInstance = new FreemeGeneralEventHandler(context);
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * register the listener.
     *
     * @param target the target register the listener.
     */
    public synchronized void register(FreemeGeneralEventHandler.Listener target) {
        FreemeLogUtils.i(TAG, "[register] mContext: " + mContext + ",target: " + target +
                ",mRegistered = " + mRegistered);
        if (target != null && !mListeners.contains(target)) {
            mListeners.add(target);
            FreemeLogUtils.i(TAG, "[register] currentListener: " + mListeners.toString());
        }
        if (!mRegistered) {
            registerBaseEventListener();
            mRegistered = true;
        }
    }

    /**
     * unRegister the listener.
     *
     * @param target the target unRegister the listener.
     */
    public synchronized void unRegister(FreemeGeneralEventHandler.Listener target) {
        FreemeLogUtils.i(TAG, "[unRegister]target: " + target + ",mRegistered = " + mRegistered);
        if (target != null && mListeners.contains(target)) {
            mListeners.remove(target);
        }
        if (mListeners.isEmpty() && mRegistered) {
            unRegisterBaseEventListener();
            mRegistered = false;
        }
    }

    //private constructor. only called once
    private FreemeGeneralEventHandler(Context context) {
        mContext = context.getApplicationContext();
        FreemeLogUtils.i(TAG, "[FreemeGeneralEventHandler] get App Context: " + mContext);
    }

    // for phb state change
    private BroadcastReceiver mPhbStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            boolean isPhbReady = intent.getBooleanExtra(FreemeGeneralEventHandler.PhbState.PHBREADY, false);
            int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, ERROR_SUB_ID);
            FreemeLogUtils.i(TAG, "[PhbChangeState_onReceive]action: " + action + ",subId:" + subId +
                    ",phbReady: " + isPhbReady);
            if (MtkTelephonyIntents.ACTION_PHB_STATE_CHANGED.equals(action)) {
                for (FreemeGeneralEventHandler.Listener listener : mListeners) {
                    if (listener != null) {
                        listener.onReceiveEvent(FreemeGeneralEventHandler.EventType.PHB_STATE_CHANGE_EVENT, intent);
                    }
                }
            }
        }
    };

    // for Sdcard state change
    private BroadcastReceiver mSdCardStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                FreemeLogUtils.e(TAG, "[SdCardState_onReceive] get action is null,return");
                return;
            }
            FreemeLogUtils.i(TAG, "[SdCardState_onReceive] action = " + action);
            int sdState = getSdCardMountedState(action);
            Intent extraData = new Intent();
            extraData.putExtra(FreemeGeneralEventHandler.SdCardState.SDSTEATE, sdState);
            for (FreemeGeneralEventHandler.Listener listener : mListeners) {
                if (listener != null) {
                    listener.onReceiveEvent(FreemeGeneralEventHandler.EventType.SD_STATE_CHANGE_EVENT, extraData);
                }
            }
        }
    };

    private void registerBaseEventListener() {
        FreemeLogUtils.i(TAG, "[registerBaseEventListener]");

        //register sd state change listener
        IntentFilter sdcardFilter = new IntentFilter();
        sdcardFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        sdcardFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        sdcardFilter.addDataScheme(SDCARD_DATA_SCHEME);
        mContext.registerReceiver(mSdCardStateReceiver, sdcardFilter);

        //register phb state change listener
        mContext.registerReceiver(mPhbStateListener, new IntentFilter(
                MtkTelephonyIntents.ACTION_PHB_STATE_CHANGED));
    }

    private void unRegisterBaseEventListener() {
        FreemeLogUtils.i(TAG, "[unRegisterBaseEventListener]");
        mContext.unregisterReceiver(mSdCardStateReceiver);
        mContext.unregisterReceiver(mPhbStateListener);
    }

    private int getSdCardMountedState(String action) {
        int rst = FreemeGeneralEventHandler.SdCardState.SDCARD_ERROR;
        if (TextUtils.isEmpty(action)) {
            FreemeLogUtils.e(TAG, "[getSdCardMountedState] get action is null,return");
            return rst;
        }
        if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
            rst = FreemeGeneralEventHandler.SdCardState.SDCARD_REMOVED;
        } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            rst = FreemeGeneralEventHandler.SdCardState.SDCARD_MOUNTED;
        }
        FreemeLogUtils.i(TAG, "[getSdCardMountedState] rst: " + rst);
        return rst;
    }
}
