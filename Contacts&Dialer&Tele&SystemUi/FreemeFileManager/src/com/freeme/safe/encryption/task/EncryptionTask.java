package com.freeme.safe.encryption.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;

public class EncryptionTask {

    private static final String TAG = "EncryptionTask";

    private boolean mStop;
    private String mPassword;
    private Cipher mCipher;

    private List<String> mPaths = new ArrayList<>();

    public void setStop(boolean state) {
        mStop = state;
    }

    public void encryptionSingleFile(Context context, int imageType, String source, boolean scanState) {
        boolean internalState;
        SafeUtils.mkPrivateFolder();
        boolean encrySuss = false;
        ContentValues values = new ContentValues();
        values.clear();
        if (mPassword == null) {
            mPassword = SafeUtils.getPasswordWithoutEncryption(context);
        }
        if (mCipher == null) {
            mCipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY + mPassword, Cipher.ENCRYPT_MODE);
        }
        String newPath = null;
        String internalPath = Util.MEMORY_DIR;
        String rootPath = SafeUtils.fetchRootPathByFile(source);
        if (internalPath == null || rootPath == null || !internalPath.equals(rootPath)) {
            internalState = false;
        } else {
            newPath = SafeConstants.NEW_INTERNAL_PATH_START + source.substring(internalPath.length(), source.length());
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "newPath = " + newPath);
            }
            internalState = true;
        }
        File file = new File(source);
        int selectedCount = detectCurrFileCount(file);
        int fileType = MediaFile.getMediaType(source);
        values.put(EncryptionColumns.ORIGINAL_PATH, source);
        values.put(EncryptionColumns.ORIGINAL_SIZE, file.length());
        values.put(EncryptionColumns.ORIGINAL_TYPE, fileType);
        values.put(EncryptionColumns.ORIGINAL_COUNT, selectedCount);
        values.put(EncryptionColumns.ROOT_PATH, rootPath);
        if (imageType == -1) {
            if (MediaFile.isAudioFileType(fileType)) {
                imageType = MediaFile.AUDIO_TYPE;
            } else if (MediaFile.isVideoFileType(fileType)) {
                imageType = MediaFile.VIDEO_TYPE;
            } else if (MediaFile.isImageFileType(fileType)) {
                imageType = MediaFile.IMAGE_TYPE;
            } else if (MediaFile.isDocFileType(fileType)) {
                imageType = MediaFile.DOC_TYPE;
            } else {
                imageType = MediaFile.OTHER_TYPE;
            }
        }

        values.put(EncryptionColumns.MEDIA_TYPE, imageType);
        String encryptionName = SafeUtils.getMD5(source);
        String encryptionPath = SafeUtils.getEncryptionPath(encryptionName);
        if (encryptionPath != null) {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "encryptionPath = " + encryptionPath);
            }
            File encryptionFile = new File(encryptionPath);
            if (encryptionFile.exists()) {
                String tempPath = fetchEncryptionFileName(encryptionPath);
                if (!tempPath.equals(encryptionPath)) {
                    encryptionPath = tempPath;
                    encryptionFile = new File(encryptionPath);
                }
                encryptionName = encryptionFile.getName();
            }
            values.put(EncryptionColumns.ENCRYPTION_NAME, encryptionName);
            if (!internalState) {
                try {
                    SafeUtils.copyFileRandomAccess(file, encryptionFile);
                    encrySuss = true;
                } catch (Exception e) {
                    encrySuss = false;
                    e.printStackTrace();
                }
                if (!mStop && encrySuss) {
                    file.delete();
                } else if (encryptionFile.exists()) {
                    encryptionFile.delete();
                }
            } else if (!mStop && !TextUtils.isEmpty(newPath)) {
                handeImageFile(imageType, source);
                if (new File(newPath).renameTo(encryptionFile)) {
                    encrySuss = true;
                } else {
                    try {
                        SafeUtils.copyFileRandomAccess(file, encryptionFile);
                        encrySuss = true;
                    } catch (Exception e) {
                        encrySuss = false;
                        e.printStackTrace();
                    }
                    if (mStop || !encrySuss) {
                        if (encryptionFile.exists()) {
                            encryptionFile.delete();
                        }
                    } else {
                        file.delete();
                    }
                }
            }
            if (!mStop && encrySuss) {
                context.getContentResolver().insert(EncryptionColumns.FILE_URI, values);
                if (!scanState) {
                    deleteFileRecord(context, source);
                }
            }
        }
    }

    private void deleteFileRecord(Context context, String filePath){
        if (filePath != null){
            ContentResolver contentResolver = context.getContentResolver();
            Uri localUri = MediaStore.Files.getContentUri("external");
            contentResolver.delete(localUri, "_data=?", new String[]{filePath});
        }
    }

    private void handeImageFile(int imageType, String source) {
        if (imageType == MediaFile.IMAGE_TYPE && mPaths != null) {
            mPaths.clear();
            mPaths.add(source);
        }
    }

    private int detectCurrFileCount(File file) {
        int count = 0;
        if (file == null || !file.exists()) {
            return 0;
        }
        if (!file.isDirectory()) {
            return 1;
        }
        File[] files = file.listFiles();
        if (files == null || files.length <= 0) {
            return 0;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                count += detectCurrFileCount(f);
            } else {
                count++;
            }
        }
        return count;
    }

    private String fetchEncryptionFileName(String encryptionPath) {
        String tempPath = null;
        int i = 1;
        while (i < SafeConstants.SAME_NAME_MAX) {
            tempPath = encryptionPath + i;
            if (!new File(tempPath).exists()) {
                break;
            }
            i++;
        }
        if (i == SafeConstants.SAME_NAME_MAX) {
            Log.v(TAG, "the same file name too much");
        }
        return tempPath;
    }
}
