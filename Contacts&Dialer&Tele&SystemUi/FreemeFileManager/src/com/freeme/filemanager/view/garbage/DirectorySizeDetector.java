package com.freeme.filemanager.view.garbage;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class DirectorySizeDetector {
    private long mByteCount=0;
    private boolean mDetected;
    private String mDirName;
    private List<String> mDirQueue;

    public DirectorySizeDetector(String path) {
        this.mDirName = path;
        this.mByteCount = 0;
        this.mDetected = false;
    }

    private void detect() {
        mDirQueue = new LinkedList();
        mDirQueue.add(mDirName);
        while (mDirQueue.size() > 0) {
            File file = new File((String) mDirQueue.remove(0));
            if (!file.exists()) {
                continue;
            }
            if (!file.isDirectory()) {
                mByteCount += file.length();
                
            } // modify by droi heqianqian on 20151224
            else if (file.listFiles() != null&&file.listFiles().length>0)
            {
                for (File listFile : file.listFiles()) {
                    if(listFile!=null){
                    mDirQueue.add(listFile.getAbsolutePath());
                }}
            }
        }
    }

    public long getSize() {
        if (!mDetected) {
            detect();
            mDetected = true;
        }
        return mByteCount;
    }
}
