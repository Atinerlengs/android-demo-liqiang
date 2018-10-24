package com.freeme.recents.presentation.event.ui;

import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.recentslib.data.model.Task;

public class TaskDismissedEvent extends FreemeEventBus.AnimatedEvent {

    public final Task task;

    public TaskDismissedEvent(Task task) {
        super();
        this.task = task;
    }
}
