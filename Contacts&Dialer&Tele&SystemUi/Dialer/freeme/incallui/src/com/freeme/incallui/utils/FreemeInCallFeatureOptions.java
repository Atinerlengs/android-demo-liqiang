package com.freeme.incallui.utils;

import android.content.Context;
import android.os.ServiceManager;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;

public class FreemeInCallFeatureOptions {

    private FreemeGameModeUtils mGameUtils;

    public FreemeInCallFeatureOptions(Context context) {
        mGameUtils = new FreemeGameModeUtils(context);
    }

    /**
     * force display full screen activity for incoming call
     *
     * @return true(default value): full screen, false: display handsup notification if it is possible
     */
    public boolean forceFullScreenForIncomingCall() {
        if (ignore()) {
            return false;
        }

        // add conditions to control whether you need to force display full screen
        if (mGameUtils.isGameModeActive()) {
            return false;
        }

        return true;
    }

    private boolean ignore() {
        return isDreaming();
    }

    private boolean isDreaming() {
        boolean isDreaming = false;
        try {
            IDreamManager mDreamManager = IDreamManager.Stub.asInterface(
                    ServiceManager.getService(DreamService.DREAM_SERVICE));
            if (mDreamManager != null) {
                isDreaming = mDreamManager.isDreaming();
            }
        } catch (Exception e) {
        }
        return isDreaming;
    }
}
