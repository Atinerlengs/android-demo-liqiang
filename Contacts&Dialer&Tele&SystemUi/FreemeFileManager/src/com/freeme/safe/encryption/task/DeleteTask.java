package com.freeme.safe.encryption.task;

import android.content.Context;

import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;

public class DeleteTask {
    public void delete(Context context, String encryptionName) {
        File encrypFile = new File(SafeUtils.getEncryptionPath(encryptionName));
        if (deleteFile(encrypFile)) {
            deleteFile(SafeUtils.getThumbFile(encrypFile));
            deleteDatabaseRecord(encryptionName, context);
        }
    }

    private boolean deleteFile(File file) {
        return file == null || !file.exists() || file.delete();
    }

    private void deleteDatabaseRecord(String encryptionName, Context context) {
        String[] selectionArgs = new String[]{encryptionName};
        String where = EncryptionColumns.ENCRYPTION_NAME + "=?";
        context.getContentResolver().delete(EncryptionColumns.FILE_URI, where, selectionArgs);
    }
}
