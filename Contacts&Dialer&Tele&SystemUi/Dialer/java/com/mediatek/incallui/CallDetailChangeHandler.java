package com.mediatek.incallui;

import android.telecom.VideoProfile;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.android.incallui.call.DialerCall;
import com.mediatek.incallui.volte.InCallUIVolteUtils;

public class CallDetailChangeHandler {

    private static final String LOG_TAG = "CallDetailChangeHandler";
    private static CallDetailChangeHandler sInstance = new CallDetailChangeHandler();

    CallDetailChangeHandler() {
    }

    public static CallDetailChangeHandler getInstance() {
        return sInstance;
    }

    public static abstract class CallDetailChangeListener {
        public void onPhoneNumberChanged(DialerCall call) {}
        public void onVolteMarkedEccChanged(DialerCall call) {}
    }

    private List<CallDetailChangeListener> mCallDetailChangeListeners
            = new ArrayList<CallDetailChangeListener>();

    public void addCallDetailChangeListener(CallDetailChangeListener listener) {
        if (!mCallDetailChangeListeners.contains(listener)) {
            mCallDetailChangeListeners.add(listener);
        }
    }

    public void removeCallDetailChangeListener(CallDetailChangeListener listener) {
        if (mCallDetailChangeListeners.contains(listener)) {
            mCallDetailChangeListeners.remove(listener);
        }
    }

    public void onCallDetailChanged(DialerCall call, android.telecom.Call.Details oldDetails,
            android.telecom.Call.Details newDetails) {
        log("handleDetailsChanged()...");
        if (InCallUIVolteUtils.isVolteMarkedEccChanged(oldDetails, newDetails)) {
            for (CallDetailChangeListener listener : mCallDetailChangeListeners) {
                listener.onVolteMarkedEccChanged(call);
            }
        }
        if (InCallUIVolteUtils.isPhoneNumberChanged(oldDetails, newDetails)) {
            for (CallDetailChangeListener listener : mCallDetailChangeListeners) {
                listener.onPhoneNumberChanged(call);
            }
        }
        // Reset video pause state if call no longer remains video call.
        if (VideoProfile.isVideo(oldDetails.getVideoState())
                && !VideoProfile.isVideo(newDetails.getVideoState())) {
            call.setVideoPauseState(false);
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
