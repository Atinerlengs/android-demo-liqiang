package com.freeme.recents.recentslib.data.repository;

import android.graphics.drawable.Drawable;

import com.freeme.recents.recentslib.data.model.Task;

import java.util.List;

public abstract interface Repository
{
  public abstract List<Task> getTasks();

  public abstract Drawable getTaskThumbnail(Task paramTask);

  public abstract Drawable getTaskIcon(Task paramTask);

  public abstract void trimMemory(int paramInt);

  public abstract void removeAllTasks(int[] paramArrayOfInt);

  public abstract void removeTask(Task paramTask);

  public abstract void removeAllTasks(Task[] taskArray);
}
