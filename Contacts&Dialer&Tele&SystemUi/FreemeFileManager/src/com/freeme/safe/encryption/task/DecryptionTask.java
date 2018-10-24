package com.freeme.safe.encryption.task;

import android.content.Context;
import android.util.Log;

import com.freeme.filemanager.util.Util;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.crypto.Cipher;

public class DecryptionTask {

    private static final String TAG = "DecryptionTask";

    private static final int BUFFER_SIZE = 1 << 20;

    private String mTargetPath;

    public boolean singleDecryptionTask(Context context, String rootPath, String targetRootPath,
                                        String targetPath, String encrypName, String originalPath,
                                        int originalType, String password, boolean replaced) {
        boolean success = false;
        boolean newFileExit = false;
        boolean internalState = false;
        Cipher cipher;
        mTargetPath = targetPath;
        if (encrypName != null) {
            File encrypFile;
            cipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY + password, Cipher.DECRYPT_MODE);
            String newPath = SafeUtils.getEncryptionPath(encrypName);
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "newPath = " + newPath);
            }
            File newEncrypFile = new File(newPath);
            String internalPath = Util.MEMORY_DIR;
            if (newEncrypFile.exists()) {
                newFileExit = true;
                internalState = !(targetRootPath == null || !targetRootPath.equals(internalPath));
            }
            if (newFileExit) {
                encrypFile = newEncrypFile;
            } else {
                encrypFile = new File(SafeUtils.getEncryptionPath(rootPath, encrypName));
            }
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "newFileExit = " + newFileExit + ",internalState = " + internalState);
            }
            if (encrypFile.exists()) {
                if (!newFileExit) {
                    SafeUtils.decryptFile(encrypFile, cipher);
                }
                String originalName = SafeUtils.getOriginalFileName(originalPath);
                if (originalName != null) {
                    File targetFile;
                    targetPath = targetPath + File.separator + originalName;
                    String newTargetPath = SafeConstants.NEW_INTERNAL_PATH_START
                            + targetPath.substring(internalPath.length(), targetPath.length());
                    if (internalState) {
                        targetFile = new File(newTargetPath);
                    } else {
                        targetFile = new File(targetPath);
                    }
                    if (!targetFile.exists() || replaced) {
                        if (!newFileExit) {
                            success = false;
                        } else if (internalState) {
                            success = renameTo(encrypFile, targetRootPath, targetFile);
                        } else {
                            success = !copyFiles(encrypFile, targetFile);
                        }
                        if (success) {
                            File thumbFile = SafeUtils.getThumbFile(encrypFile);
                            if (thumbFile.exists()) {
                                if (thumbFile.delete()) {
                                    deleteDatabase(context, encrypName);
                                }
                            }
                            SafeUtils.scanAllFile(context, new String[]{targetPath}, null);
                        }
                    }
                }
            } else {
                deleteDatabase(context, encrypName);
            }
        }
        return success;
    }

    private boolean deleteDatabase(Context context, String encryptionName) {
        if (SafeUtils.DEBUG) {
            Log.d(TAG, "deleteDatabase encryptionName = " + encryptionName);
        }
        if (encryptionName != null) {
            String where = EncryptionColumns.ENCRYPTION_NAME + "?=";
            int deletes = context.getContentResolver().delete(EncryptionColumns.FILE_URI,
                    where, new String[]{encryptionName});
            if (deletes > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean renameTo(File encrypFile, String rootPath, File targetFile) {
        if (SafeUtils.DEBUG) {
            Log.d(TAG, "mTargetPath = " + mTargetPath + ",rootPath = " + rootPath);
        }
        if (mTargetPath.startsWith(rootPath)) {
            return encrypFile.renameTo(targetFile);
        }
        boolean success = copyFile(encrypFile, targetFile);
        if (success) {
            if (encrypFile.delete()) {
                return true;
            }
        }
        return false;
    }

    private boolean copyFile(File srcFile, File destFile) {
        if (!srcFile.exists() || srcFile.isDirectory()) {
            Log.i(TAG, "copyFile: file not exist or is directory, " + srcFile);
            return false;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(srcFile);
            if (!destFile.exists()) {
                if (!destFile.mkdirs()) {
                    return false;
                }
            }

            String destPath;
            int i = 1;
            while (destFile.exists()) {
                String destName = getNameFromFilename(srcFile.getName()) + " "
                        + (i++) + "."
                        + getExtFromFilename(srcFile.getName());
                destPath = makePath(destFile.getAbsolutePath(), destName);
                destFile = new File(destPath);
            }

            if (!destFile.createNewFile()) {
                return false;
            }

            fo = new FileOutputStream(destFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = fi.read(buffer, 0, BUFFER_SIZE)) != -1) {
                fo.write(buffer, 0, read);
                fo.flush();
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "copyFile: file not found, " + srcFile);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "copyFile: " + e.toString());
        } finally {
            try {
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        }
        return "";
    }

    private static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    private static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator)) {
            return path1 + path2;
        }
        return path1 + File.separator + path2;
    }

    private boolean copyFiles(File encrypFile, File targetFile) {
        boolean hasException = false;
        try {
            SafeUtils.copyFileRandomAccess(encrypFile, targetFile);
        } catch (Exception e) {
            e.printStackTrace();
            hasException = true;
            Log.e(TAG, "Exception decompress error path = " + encrypFile.getAbsolutePath());
        }
        if (!hasException) {
            try {
                hasException = !encrypFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return hasException;
    }
}
