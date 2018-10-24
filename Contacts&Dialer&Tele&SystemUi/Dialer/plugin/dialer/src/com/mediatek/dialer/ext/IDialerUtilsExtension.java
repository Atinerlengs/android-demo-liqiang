package com.mediatek.dialer.ext;

public interface IDialerUtilsExtension {
    /**
     * for op01.
     * plug-in we do not use google default blocked number features
     * @return false in op01
     */
    public boolean shouldUseBlockedNumberFeature();
}
