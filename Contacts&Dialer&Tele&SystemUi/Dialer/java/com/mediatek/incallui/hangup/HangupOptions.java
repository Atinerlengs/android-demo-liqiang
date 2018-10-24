package com.mediatek.incallui.hangup;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Options for hangup type. */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        HangupOptions.HANGUP_ALL,
        HangupOptions.HANGUP_HOLD,
})

public @interface HangupOptions {
    int HANGUP_ALL = 0x00000001;
    int HANGUP_HOLD = HANGUP_ALL << 1;
}