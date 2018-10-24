package com.freeme.recents.recentslib.data.model;

import android.app.ActivityManager;
import android.app.ActivityManager.TaskDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;

public class Task {

    public final int taskId;
    public final int stackId;
    public final Intent intent;
    public final int userId;
    public ActivityManager.TaskDescription taskDescription;
    public long lastActiveTime;
    private Drawable thumbnail;
    private Drawable icon;
    private CharSequence label;
    private boolean loaded;
    private boolean isHome;
    private boolean isLocked;
    private Object holder;

    public boolean isLocked() {
        return this.isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    public Task(int taskId, int stackId, Intent intent, ActivityManager.TaskDescription description, int userId, long lastActiveTime) {
        this.intent = intent;
        this.taskId = taskId;
        this.stackId = stackId;
        this.taskDescription = description;
        this.userId = userId;
        this.lastActiveTime = lastActiveTime;
    }

    public Task() {
        this.intent = null;
        this.taskId = -1;
        this.stackId = -1;
        this.taskDescription = null;
        this.userId = -10000;
        this.lastActiveTime = -1L;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public boolean isNull() {
        return this.taskDescription == null;
    }

    public CharSequence getLabel() {
        return this.label;
    }

    public void setLabel(CharSequence label) {
        this.label = label;
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public void setThumbnail(Drawable thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Drawable getThumbnail() {
        return this.thumbnail;
    }

    public boolean isHome() {
        return this.isHome;
    }

    public void setHome(boolean home) {
        this.isHome = home;
    }

    public String toStringKey() {
        return "Task.Key: " + this.taskId + ", " + "u: " + this.userId + ", " + this.intent
                .getComponent ().getPackageName ();
    }

    public Object getHolder() {
        return this.holder;
    }

    public void setHolder(Object holder) {
        this.holder = holder;
    }

    //*/ freeme.gouzhouping, 20180404. Recents, white list.
    public String getPackageName() {
        return intent.getComponent().getPackageName();
    }
    //*/
}


