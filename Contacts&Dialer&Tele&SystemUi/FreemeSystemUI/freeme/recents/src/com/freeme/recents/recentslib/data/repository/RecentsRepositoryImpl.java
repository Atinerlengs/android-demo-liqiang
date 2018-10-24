package com.freeme.recents.recentslib.data.repository;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.SystemProxy;

import java.util.ArrayList;
import java.util.List;

public class RecentsRepositoryImpl {
    static final String TAG = "RecentsRepositoryImpl";
    protected final Context mContext;
    private final BitmapDrawable mDefaultThumbnail;
    private final BitmapDrawable mDefaultIcon;
    private final SystemProxy mSystemProxy;

    public RecentsRepositoryImpl(Context context, SystemProxy systemProxy) {
        this.mContext = context;
        this.mSystemProxy = systemProxy;
        Bitmap icon = Bitmap.createBitmap (1, 1, Bitmap.Config.ALPHA_8);
        icon.eraseColor (0);
        Bitmap thumbnail = Bitmap.createBitmap (9, 16, Bitmap.Config.ARGB_8888);
        thumbnail.setHasAlpha (false);
        thumbnail.eraseColor (-1);
        this.mDefaultIcon = new BitmapDrawable (context.getResources (), icon);
        this.mDefaultThumbnail = new BitmapDrawable (context.getResources (), thumbnail);
    }

    public List<Task> getTasks() {
        List<RecentTaskInfo> infos = mSystemProxy.getTasks ();
        ArrayList tasks = new ArrayList ();
        for (RecentTaskInfo info : infos) {
            Task task = new Task (info.persistentId, info.stackId, info.baseIntent, info.taskDescription, info.userId, info.lastActiveTime);
            task.setLabel (getTaskLable (task));
            if (info.stackId == 0) {
                task.setHome (true);
            }
            tasks.add (task);
        }
        return tasks;
    }

    public Drawable getTaskThumbnail(int taskId) {
        Drawable drawable = this.mSystemProxy.getTaskThumbnail (taskId, this.mContext.getResources ());
        if (drawable != null) {
            return drawable;
        }
        return this.mDefaultThumbnail;
    }

    public Drawable getTaskIcon(Task task) {
        Drawable drawable = this.mSystemProxy.getTaskIcon (task.taskDescription, task.intent.getComponent (), task.userId, this.mContext
                .getResources ());

        if (drawable != null) {
            return drawable;
        }
        return this.mDefaultIcon;
    }

    public void removeAllTasks(int[] taskIds) {
        for (int taskId : taskIds)
            this.mSystemProxy.removeTask (taskId);
    }

    public void removeTask(int taskId) {
        this.mSystemProxy.removeTask (taskId);
    }

    private String getTaskLable(Task task) {
        return this.mSystemProxy
                .getTaskLable (task.taskDescription, task.intent
                        .getComponent ());
    }

    public void removeAllTasks(Task[] tasks) {
        for (Task task : tasks) {
            mSystemProxy.removeTask(task.taskId, task.getPackageName());
        }
    }

    public void removeTask(int taskId, String pkg) {
        mSystemProxy.removeTask(taskId, pkg);
    }
}

