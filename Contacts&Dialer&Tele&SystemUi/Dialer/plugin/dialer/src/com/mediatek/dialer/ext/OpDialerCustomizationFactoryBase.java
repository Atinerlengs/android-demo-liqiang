package com.mediatek.dialer.ext;

public class OpDialerCustomizationFactoryBase {
    public ICallDetailExtension makeCallDetailExt() {
        return new DefaultCallDetailExtension();
    }

    public ICallLogExtension makeCallLogExt() {
        return new DefaultCallLogExtension();
    }

    public IDialerSearchExtension makeDialerSearchExt() {
        return new DefaultDialerSearchExtension();
    }

    public IDialPadExtension makeDialPadExt() {
        return new DefaultDialPadExtension();
    }

    public IRCSeCallLogExtension makeRCSeCallLogExt() {
        return new DefaultRCSeCallLogExtension();
    }

    public IDialerUtilsExtension makeDialerUtilsExt() {
        return new DefaultDialerUtilsExtension();
    }
}
