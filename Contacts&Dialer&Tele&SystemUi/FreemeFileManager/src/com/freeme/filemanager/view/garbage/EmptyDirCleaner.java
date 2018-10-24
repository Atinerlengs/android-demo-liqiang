package com.freeme.filemanager.view.garbage;

import java.io.File;
import java.util.LinkedList;

import android.util.Log;

public class EmptyDirCleaner {
    private static final int MAX_PATH_SEGMENTS = 6;
    private static final String TAG = "EmptyDirCleaner";
    private boolean mDelete;
    private int mDeleteCount = 0;
    private long mDeleteSize = 0L;
    private boolean mDetected;
    private File mDir = null;
    private int mEmptyDirCount = 0;
    private boolean mRootDelete;
    private long mSizeCount = 0L;
    LinkedList<File> emptyDirList = new LinkedList<File>();

    public EmptyDirCleaner() {
    }

    public EmptyDirCleaner (File file, boolean delete, boolean rootDelete) {
        this.mDir = file;
        this.mDelete = delete;
        this.mRootDelete = rootDelete;
        this.mDetected = false;
        detect(this.mDir, this.mDelete, this.mRootDelete);
    }

    private void detect (File file, boolean delete, boolean rootDelete) {
        if((file == null) || (!file.exists())){
            return;
        }
        if ((file.getAbsolutePath().split(File.separator).length < MAX_PATH_SEGMENTS) && (file.isDirectory())){
            File[] listFiles = file.listFiles();
            if (listFiles == null || listFiles.length==0) {
                return;
            }
            for (int i =0; i<listFiles.length; i++) {
                File childFile = listFiles[i];
                // if the childFile is a directory
                if (childFile.exists() && childFile.isDirectory()) {
                    File[] childListFiles = childFile.listFiles();
                    // if the childFile is a empty dir
                    if (childListFiles!=null && childListFiles.length == 0) {
                        long length = childFile.length();
                        if (!delete) {
                            emptyDirList.add(childFile);
                            mEmptyDirCount += 1;
                            mSizeCount += length;
                        } else {
                            if (childFile.delete()) {
                                mDeleteCount += 1;
                                mDeleteSize += length;
                                mEmptyDirCount -= 1;
                                mSizeCount -= length;
                            }
                        }
                    } else {
                        // if the childFile has child files, then to detect it
                        detect(childFile, delete, rootDelete);
                    }
                }
            }
        }
        
    }      

    public int emptyDeleteCount() {
        return this.mDeleteCount;
    }

    public long emptyDeleteSize() {
        return this.mDeleteSize;
    }

    public int emptyDirCount() {
        return this.mEmptyDirCount;
    }

    public long sizeCount() {
        return this.mSizeCount;
    }
}