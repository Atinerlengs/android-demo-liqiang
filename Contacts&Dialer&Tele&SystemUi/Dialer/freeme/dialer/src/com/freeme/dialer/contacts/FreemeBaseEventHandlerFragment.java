package com.freeme.dialer.contacts;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.PhoneConstants;
import com.freeme.contacts.common.utils.FreemeLogUtils;

public class FreemeBaseEventHandlerFragment extends Fragment implements FreemeGeneralEventHandler.Listener {
    private static String TAG = "BaseEventHanleFragment";
    private static final int DEFAULT_NO_USE_SUBID = -1;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        FreemeLogUtils.i(TAG, "[onCreate]");
        FreemeGeneralEventHandler.getInstance(getContext()).register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FreemeLogUtils.i(TAG, "[onDestroy]");
        FreemeGeneralEventHandler.getInstance(getContext()).unRegister(this);
    }

    @Override
    public void onReceiveEvent(String eventType, Intent extraData) {
        int subId = getSubId();
        int stateChangeSubId = extraData.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                FreemeGeneralEventHandler.ERROR_SUB_ID);
        FreemeLogUtils.i(TAG,
                "[onReceiveEvent] eventType: " + eventType + ", extraData: " + extraData.toString()
                        + ",subId: " + subId + ",stateChangeSubId: " + stateChangeSubId);
        if (FreemeGeneralEventHandler.EventType.PHB_STATE_CHANGE_EVENT.equals(eventType)
                && SubscriptionManager.isValidSubscriptionId(subId)
                && SubscriptionManager.isValidSubscriptionId(stateChangeSubId)
                && (subId == stateChangeSubId)) {
            FreemeLogUtils.i(TAG, "[onReceiveEvent] phb state change,default action: getActivity finish!");
            getActivity().finish();
        }
    }

    protected int getSubId() {
        return DEFAULT_NO_USE_SUBID;
    }
}
