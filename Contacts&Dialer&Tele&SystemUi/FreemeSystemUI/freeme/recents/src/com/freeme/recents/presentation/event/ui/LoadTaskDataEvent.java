package com.freeme.recents.presentation.event.ui;

import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.recentslib.data.model.Task;

public class LoadTaskDataEvent extends FreemeEventBus.Event {

    public final Task task;

    public LoadTaskDataEvent(Task task) {
        this.task = task;
    }
}
