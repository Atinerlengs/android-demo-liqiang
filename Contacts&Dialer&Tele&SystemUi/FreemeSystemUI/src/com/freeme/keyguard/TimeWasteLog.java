package com.freeme.keyguard;

import android.util.Log;

import com.freeme.util.FreemeFeature;

public class TimeWasteLog {
    public static final String FP_LOG_TAG = "FREEME_FP_WAKE";

    public static final boolean DEBUG_TIME_WASTE
            = FreemeFeature.getLocalBoolean("debug.fp.wake_log", true);

    private static long FINGER_ACQUIRE_DOWN_TIME = 0;
    private static long FINGER_SUCCESS_TIME      = 0;
    private static long WAKE_UP_OVER_TIME        = 0;
    private static long WAKE_UP_TIME             = 0;

    public static void markFingerAcquireDownTime() {
        FINGER_ACQUIRE_DOWN_TIME = System.currentTimeMillis();
        Log.d(FP_LOG_TAG, "markFingerAcquireDownTime " + FINGER_ACQUIRE_DOWN_TIME);
    }

    public static final void markFingerSuccessTime() {
        FINGER_SUCCESS_TIME = System.currentTimeMillis();
        Log.d(FP_LOG_TAG, "markFingerSuccessTime " + FINGER_SUCCESS_TIME);
        if (FINGER_ACQUIRE_DOWN_TIME > 0) {
            Log.d(FP_LOG_TAG, "wasteTime from finger down to finger success: " + (FINGER_SUCCESS_TIME - FINGER_ACQUIRE_DOWN_TIME));
        }
    }

    public static final void markWakeUpTime() {
        WAKE_UP_TIME = System.currentTimeMillis();
        Log.d(FP_LOG_TAG, "markWakeUpTime: " + WAKE_UP_TIME);
    }

    public static final void markWakeUpOverTime() {
        WAKE_UP_OVER_TIME = System.currentTimeMillis();
        Log.d(FP_LOG_TAG, "markWakeUpOverTime: " + WAKE_UP_OVER_TIME);
        if (WAKE_UP_TIME > 0) {
            Log.d(FP_LOG_TAG, "wasteTime from wakeup to wakeUpOver: " + (WAKE_UP_OVER_TIME - WAKE_UP_TIME));
        }
    }

    public static final void markTotalTimeFromFingerToUnlock() {
        if (FINGER_ACQUIRE_DOWN_TIME > 0) {
            Log.d(FP_LOG_TAG, "wasteTime from finger down to unlock: " + (System.currentTimeMillis() - FINGER_ACQUIRE_DOWN_TIME));
        }
    }

    public static void reset() {
        FINGER_ACQUIRE_DOWN_TIME = 0;
        FINGER_SUCCESS_TIME = 0;
        WAKE_UP_TIME = 0;
        WAKE_UP_OVER_TIME = 0;
    }
}
