package com.freeme.recents.recentslib.data.repository;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import com.freeme.recents.SystemServicesProxy;
import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.RecentsRepositoryImpl;
import com.freeme.recents.recentslib.data.repository.SystemProxy;

import java.util.List;

public class RecentsRepository
        implements Repository {
    private static final String TAG = "RecentsRepository";
    private static RecentsRepository sInstance;
    private final RecentsRepositoryImpl mImpl;
    private int mMaxThumbnailCacheSize;
    private int mMaxIconCacheSize;
    private int mNumVisibleTasksLoaded;
    private final LruCache<String, Drawable> mThumbnailCache;
    private final LruCache<String, Drawable> mIconCache;
    private static final boolean RECENTS_MOCK = false;

    private RecentsRepository(Context context, SystemProxy systemProxy) {
        this.mMaxThumbnailCacheSize = 10;
        this.mMaxIconCacheSize = 20;
        this.mNumVisibleTasksLoaded = 4;
        this.mIconCache = new LruCache (this.mMaxIconCacheSize);
        this.mThumbnailCache = new LruCache (this.mMaxThumbnailCacheSize);

        this.mImpl = new RecentsRepositoryImpl (context, systemProxy);
    }

    public static RecentsRepository getInstance(Context context, SystemProxy systemProxy) {
        if (sInstance == null) {
            sInstance = new RecentsRepository (context, systemProxy);
        }

        return sInstance;
    }

    private String getCacheKey(Task task) {
        return "last:" + task.lastActiveTime + "," + task.toStringKey ();
    }

    public List<Task> getTasks() {
        return this.mImpl.getTasks ();
    }

    public Drawable getTaskThumbnail(Task task) {
        /*/ freeme.gouzhouping, 20180331. Recents. blank view.
        Drawable drawable = (Drawable) this.mThumbnailCache.get (getCacheKey (task));
        if (drawable == null) {
            drawable = this.mImpl.getTaskThumbnail (task.taskId);
            this.mThumbnailCache.put (getCacheKey (task), drawable);
        }

        return drawable;
        /*/
        return mImpl.getTaskThumbnail (task.taskId);
        //*/
    }

    public Drawable getTaskIcon(Task task) {
        Drawable drawable = (Drawable) this.mIconCache.get (getCacheKey (task));
        if (drawable == null) {
            drawable = this.mImpl.getTaskIcon (task);
            this.mIconCache.put (getCacheKey (task), drawable);
        }

        return drawable;
    }

    public void trimMemory(int level) {
        switch (level) {
            case 20:
                this.mThumbnailCache.trimToSize (Math.max (this.mNumVisibleTasksLoaded, this.mMaxThumbnailCacheSize / 2));

                this.mIconCache.trimToSize (Math.max (this.mNumVisibleTasksLoaded, this.mMaxIconCacheSize / 2));

                break;
            case 5:
            case 40:
                this.mThumbnailCache.trimToSize (Math.max (1, this.mMaxThumbnailCacheSize / 2));
                this.mIconCache.trimToSize (Math.max (1, this.mMaxIconCacheSize / 2));
                break;
            case 10:
            case 60:
                this.mThumbnailCache.trimToSize (Math.max (1, this.mMaxThumbnailCacheSize / 4));
                this.mIconCache.trimToSize (Math.max (1, this.mMaxIconCacheSize / 4));
                break;
            case 15:
            case 80:
                this.mThumbnailCache.evictAll ();
                this.mIconCache.evictAll ();
                break;
        }
    }

    public void removeAllTasks(int[] taskIds) {
        this.mThumbnailCache.evictAll ();
        this.mIconCache.evictAll ();
        this.mImpl.removeAllTasks (taskIds);
    }

    public void removeAllTasks(Task[] tasks) {
        mThumbnailCache.evictAll();
        mIconCache.evictAll();
        mImpl.removeAllTasks(tasks);
    }

    public void removeTask(Task task) {
        mThumbnailCache.remove(getCacheKey (task));
        mIconCache.remove(getCacheKey(task));
        mImpl.removeTask(task.taskId, task.getPackageName());
    }
}

