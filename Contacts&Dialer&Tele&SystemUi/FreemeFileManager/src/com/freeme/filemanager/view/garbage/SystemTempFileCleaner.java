package com.freeme.filemanager.view.garbage;

import java.io.File;

public class SystemTempFileCleaner {

    private static final String TAG = "FileExplorer.TempFileCleaner";
    private boolean mDelRootDir;
    private boolean mDelete;
    private int mFileCount;
    private File mRootDir;
    private long mSizeCount;

    public SystemTempFileCleaner(File rootDir, boolean delRootDir, boolean delete) {
        this.mRootDir = rootDir;
        this.mDelRootDir = delRootDir;
        this.mDelete = delete;
        recursiveDelete(rootDir);
    }

    private void recursiveDelete(File file) {
        if ((file.isDirectory()) && (file.listFiles() != null)) {
            File[] listFiles = file.listFiles();
            for (int i = 0; i < listFiles.length; i++) {
                recursiveDelete(listFiles[i]);
            }
        }
        long length = file.length();
        mFileCount += 1;
        mSizeCount += length;
        if (mDelete) {
            if (((mDelRootDir) && (file == mRootDir)) || (file != mRootDir)) {
                file.delete();
            }
        }
    }

    public int fileCount() {
        return this.mFileCount;
    }

    public long size() {
        return this.mSizeCount;
    }
}
