package com.freeme.safe.encryption.service;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

interface IEncryptionService {
    void deleteTask(String source);

    boolean decryptionTask(String rootPath, String targetRootPath, String targetPath, String encrypName,
     String originalPath, int originalType, String password, boolean replaced);

    void encryptionTask(String source, int imageType, boolean scanState);

    Bitmap getBitmap(String source, String password);

    void savePasswordAndMode(int mode, String password);

    void setStopEncryption(boolean state);

    String viewTask(String source);
}
