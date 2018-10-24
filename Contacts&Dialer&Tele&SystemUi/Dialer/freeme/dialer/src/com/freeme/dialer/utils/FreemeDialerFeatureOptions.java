package com.freeme.dialer.utils;

import android.os.SystemProperties;

public class FreemeDialerFeatureOptions {

    public static boolean isMtkSosQuickDialSupport() {
        return "1".equals(SystemProperties.get("persist.mtk_sos_quick_dial"));
    }
}
