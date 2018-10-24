package com.freeme.filemanager.view;

/**
 * Created by chenming on 17-6-29.
 */

public class StorageCategoryItem {
    private String mTitle;
    private long mTotal;
    private long mFree;

    public StorageCategoryItem(String title, long total, long free) {
        mTitle = title;
        mTotal = total;
        mFree = free;
    }
    public StorageCategoryItem(String title) {
        mTitle = title;
        mTotal = 0;
        mFree = 0;
    }

    public long getTotal() {
        return mTotal;
    }

    public String getTitle() {
        return mTitle;
    }

    public long getFree() {
        return mFree;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setTotal(long total) {
        mTotal = total;
    }

    public void setFree(long free) {
        mFree = free;
    }
}
