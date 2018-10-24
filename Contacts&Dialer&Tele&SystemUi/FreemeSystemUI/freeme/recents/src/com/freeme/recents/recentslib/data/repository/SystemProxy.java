package com.freeme.recents.recentslib.data.repository;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.TaskDescription;
import android.content.ComponentName;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.List;

public abstract interface SystemProxy
{
  public abstract void removeTask(int paramInt);

  public abstract void removeTask(int taskId, String pkg);

  public abstract String getTaskLable(ActivityManager.TaskDescription paramTaskDescription, ComponentName paramComponentName);

  public abstract Drawable getTaskIcon(ActivityManager.TaskDescription paramTaskDescription, ComponentName paramComponentName, int paramInt, Resources paramResources);

  public abstract Drawable getTaskThumbnail(int paramInt, Resources paramResources);

  public abstract List<ActivityManager.RecentTaskInfo> getTasks();
}

