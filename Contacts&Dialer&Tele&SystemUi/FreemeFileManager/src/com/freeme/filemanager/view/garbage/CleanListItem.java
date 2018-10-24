package com.freeme.filemanager.view.garbage;

import com.freeme.filemanager.util.AsyncGarbageCleanupHelper.GarbageItem;
import com.freeme.filemanager.util.Util;

import java.io.Serializable;

public class CleanListItem implements Serializable {
    private String mPath;
    private String mRootPath;
    private String mItemName;
    private long mItemSize;
    private int mItemArraryId;

    public CleanListItem(GarbageItem garbageItem, int arraryId) {
        this.mItemName = garbageItem.appName;
        this.mItemSize = garbageItem.size;
        this.mPath = garbageItem.path;
        this.mRootPath = garbageItem.rootPath;
        this.mItemArraryId = arraryId;
    }

    public String getName() {
        return mItemName;
    }
    public void setName(String name) {
        this.mItemName = name;
    }

    public long getSize() {
        return mItemSize;
    }
    public void setSize(long size) {
        this.mItemSize = size;
    }

    public String getPath() {
        return mPath;
    }
    public void setPath(String path) {
        this.mPath = path;
    }

    public String getRootPath() {
        return mRootPath;
    }
    public void setRootPath(String rootpath) {
        this.mRootPath = rootpath;
    }

    public int getArraryId() {
        return mItemArraryId;
    }
    public void setArraryId(int arraryId) {
        this.mItemArraryId = arraryId;
    }
}
