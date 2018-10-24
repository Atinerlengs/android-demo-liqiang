package com.freeme.safe.encryption.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.freeme.safe.encryption.task.DecryptionTask;
import com.freeme.safe.encryption.task.DeleteTask;
import com.freeme.safe.encryption.task.EncryptionTask;
import com.freeme.safe.encryption.task.ViewTask;
import com.freeme.safe.utils.SafeUtils;

public class EncryptionService extends Service {
    private static final String TAG = "EncryptionService";

    private DeleteTask mDeleteTask;
    private DecryptionTask mDecryptionTask;
    private EncryptionTask mEncryptionTask;
    private ViewTask mViewTask;

    private final IEncryptionService.Stub mBinder = new IEncryptionService.Stub() {
        Context mContext = EncryptionService.this;

        @Override
        public void deleteTask(String source) throws RemoteException {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "deleteTask");
            }
            if (mDeleteTask == null) {
                mDeleteTask = new DeleteTask();
            }
            try {
                mDeleteTask.delete(mContext, source);
            } catch (Exception e) {
                throw new RemoteException(e.toString());
            }
        }

        @Override
        public boolean decryptionTask(String rootPath, String targetRootPath, String targetPath,
                                      String encrypName, String originalPath, int originalType,
                                      String password, boolean replaced) throws RemoteException {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "decryptionTask");
            }
            if (mDecryptionTask == null) {
                mDecryptionTask = new DecryptionTask();
            }
            try {
                return mDecryptionTask.singleDecryptionTask(mContext, rootPath, targetRootPath,
                        targetPath, encrypName, originalPath, originalType, password, replaced);
            } catch (Exception e) {
                throw new RemoteException(e.toString());
            }
        }

        @Override
        public void encryptionTask(String source, int imageType, boolean scanState) throws RemoteException {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "encryptionTask");
            }
            if (mEncryptionTask == null) {
                mEncryptionTask = new EncryptionTask();
            }
            try {
                mEncryptionTask.encryptionSingleFile(mContext, imageType, source, scanState);
            } catch (Exception e) {
                throw new RemoteException(e.toString());
            }
        }

        @Override
        public Bitmap getBitmap(String source, String password) throws RemoteException {
            return null;
        }

        @Override
        public void savePasswordAndMode(int mode, String password) throws RemoteException {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "savePasswordAndMode");
            }
            SafeUtils.saveLockModeAndPassword(mContext, mode, password);
        }

        @Override
        public void setStopEncryption(boolean state) throws RemoteException {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "setStopEncryption");
            }
            if (mEncryptionTask != null) {
                mEncryptionTask.setStop(state);
            }
        }

        @Override
        public String viewTask(String source) throws RemoteException {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "viewTask");
            }
            String path;
            if (mViewTask == null) {
                mViewTask = new ViewTask();
            }
            try {
                path = mViewTask.openSelectedFile(mContext, source);
            } catch (Exception e) {
                throw new RemoteException(e.toString());
            }
            return path;
        }
    };

    @Override
    public IBinder onBind(Intent t) {
        return mBinder;
    }
}