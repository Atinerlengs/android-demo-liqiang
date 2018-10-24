package com.freeme.recents.presentation.event.activity;

import com.freeme.recents.presentation.event.FreemeEventBus;

public class FreemeHideRecentsEvent extends FreemeEventBus.Event {

    public final boolean anim;
    public boolean force;

    public FreemeHideRecentsEvent(boolean anim) {
        this.anim = anim;
    }
}
