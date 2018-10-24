package com.mediatek.incallui;

import android.os.Trace;

import com.android.dialer.common.LogUtil;

/**
 * M: log trace for performance profile.
 * enable trace via adb command:
 * adb shell setprop debug.atrace.tags.enableflags 0x1000
 * adb shell atrace --poke_services
 */
public class InCallTrace {
    private static final String LOG_TAG = "InCallTrace";
    private static final long TRACE_TAG = Trace.TRACE_TAG_PERF;

    /**
     * begin the performance trace.
     *
     * @param tag the tag shows in the report.
     */
    public static void begin(String tag) {
        if (Trace.isTagEnabled(TRACE_TAG)) {
            LogUtil.i(LOG_TAG, "[begin]" + tag);
            Trace.traceBegin(TRACE_TAG, tag);
        }
    }

    /**
     * end the performance trace.
     *
     * @param tag the tag shows in the report.
     */
    public static void end(String tag) {
        if (Trace.isTagEnabled(TRACE_TAG)) {
            Trace.traceEnd(TRACE_TAG);
            LogUtil.i(LOG_TAG, "[end]" + tag);
        }
    }
}
