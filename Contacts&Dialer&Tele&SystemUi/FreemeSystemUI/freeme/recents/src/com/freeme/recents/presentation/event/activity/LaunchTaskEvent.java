package com.freeme.recents.presentation.event.activity;

import android.view.View;

import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.recentslib.data.model.Task;

public class LaunchTaskEvent extends FreemeEventBus.Event {

    public final Task task;
    public final View view;
    public final boolean screenPinningRequested;

    public LaunchTaskEvent(Task task, View view, boolean screenPinningRequested) {
        this.task = task;
        this.view = view;
        this.screenPinningRequested = screenPinningRequested;
    }
}
