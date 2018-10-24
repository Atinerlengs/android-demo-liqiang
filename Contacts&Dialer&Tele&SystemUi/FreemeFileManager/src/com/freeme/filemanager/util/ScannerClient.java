package com.freeme.filemanager.util;


import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import com.freeme.filemanager.model.FileManagerLog;

import java.util.ArrayList;

public final class ScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {

    private static final String TAG = "ScannerClient";

    private ArrayList<String> mPaths = new ArrayList<String>();
    private MediaScannerConnection mScannerConnection = null;
    private int mScanningFileNumber = 0;
    private long mScanFilesWatingTimeStart = 0;
    private Object mLock = new Object();

    private static ScannerClient sInstance = null;

    /**
     * Please call the method init(Context ) before useing it.
     * @return ScannerClient ScannerClient Object
     */
    public static ScannerClient getInstance() {
        if (sInstance == null) {
            sInstance = new ScannerClient();
        }
        return sInstance;
    }

    private ScannerClient() {
    }

    public void init(Context context) {
        if (mScannerConnection != null && mScannerConnection.isConnected()) {
            mScannerConnection.disconnect();
            mScannerConnection = null;
        }
        mScannerConnection = new MediaScannerConnection(context, this);
    }

    public void scanPath(String path) {
        FileManagerLog.i(TAG, "scanPath() thread id: " + Thread.currentThread().getId());
        synchronized (mLock) {
            mScanningFileNumber++;
            mScanFilesWatingTimeStart = System.currentTimeMillis();
            if (mScannerConnection.isConnected()) {
                mScannerConnection.scanFile(path, null);
            } else {
                mPaths.add(path);
                mScannerConnection.connect();
            }
        }
    }

    public void connect() {
        if (!mScannerConnection.isConnected()) {
            mScannerConnection.connect();
        }
    }

    public void disconnect() {
        synchronized (mLock) {
            mScanningFileNumber = 0;
            mPaths.clear();
            mScanFilesWatingTimeStart = 0;
            mScannerConnection.disconnect();
        }
    }

    @Override
    public void onMediaScannerConnected() {
        FileManagerLog.i(TAG, "onMediaScannerConnected(), thread id: "
                + Thread.currentThread().getId());
        synchronized (mLock) {
            if (!mPaths.isEmpty()) {
                for (String path : mPaths) {
                    mScannerConnection.scanFile(path, null);
                }
                mPaths.clear();
            }
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        FileManagerLog.i(TAG, "onScanCompleted() thread: " + Thread.currentThread().getId());
        FileManagerLog.i(TAG, "path=" + path + ", uri: " + uri.toString());
        synchronized (mLock) {
            mScanningFileNumber--;
        }
    }

    public boolean waitForScanningCompleted() {
        FileManagerLog.i(TAG, "waitForScanningCompleted() :" + Thread.currentThread().getId());
        FileManagerLog.i(TAG, "mScanningFileNumber: " + mScanningFileNumber);
        if (mScanningFileNumber == 0) {
            return true;
        }

        if (System.currentTimeMillis() - mScanFilesWatingTimeStart >= 3000) {
            FileManagerLog.i(TAG, "Query MediaStore waiting overtime: "
                    + (System.currentTimeMillis() - mScanFilesWatingTimeStart));
            return true;
        }
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
