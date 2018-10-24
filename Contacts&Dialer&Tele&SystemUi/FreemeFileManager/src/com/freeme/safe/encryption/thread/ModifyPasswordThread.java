package com.freeme.safe.encryption.thread;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.filemanager.R;
import com.freeme.safe.dialog.DialogFactory;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;

import javax.crypto.Cipher;

import static com.freeme.safe.encryption.provider.EncryptionColumns.ENCRYPTION_NAME;
import static com.freeme.safe.encryption.provider.EncryptionColumns.ROOT_PATH;

public class ModifyPasswordThread extends Thread {
    private static final String TAG = "ModifyPasswordTask";

    public interface OnModifyPasswordListener {
        void onModifyComplete(boolean complete);
    }

    private String mNewPassword;

    private Dialog mDialog;

    private Context mContext;
    private Handler mHandler = new Handler();

    private OnModifyPasswordListener mListener;

    public ModifyPasswordThread(Context context) {
        mContext = context;
        mDialog = DialogFactory.getProgressDialog(mContext, mContext.getResources().getString(R.string.modify_password_progress_message));
    }

    @Override
    public void run() {
        if (TextUtils.isEmpty(mNewPassword)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        mListener.onModifyComplete(false);
                    }
                }
            });
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    mDialog.show();
                }
            }
        });
        boolean isSuccess = true;
        Cursor cursor;
        cursor = mContext.getContentResolver().query(EncryptionColumns.FILE_URI, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String oldPassword = null;
            Cipher oldCipher = null;
            Cipher newCipher = null;
            int count = cursor.getCount();
            for (int i = 0; i < count; i++) {
                if (oldPassword == null) {
                    String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
                    oldPassword = SafeUtils.getPassword(mContext, SafeUtils.getSafeFilePath(safeFilePath,
                            SafeConstants.LOCK_PASSWORD_PATH));
                    if (oldPassword == null) {
                        isSuccess = false;
                        break;
                    } else {
                        Cipher cipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY, Cipher.DECRYPT_MODE);
                        oldPassword = SafeUtils.dencrypString(oldPassword, cipher);
                    }
                }
                if (SafeUtils.DEBUG) {
                    Log.v(TAG, "oldPassword = " + oldPassword + "|| mNewPassword = " + mNewPassword);
                }
                if (newCipher == null) {
                    newCipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY + mNewPassword, Cipher.ENCRYPT_MODE);
                }
                if (oldCipher == null) {
                    oldCipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY + oldPassword, Cipher.DECRYPT_MODE);
                }
                String rootPath = cursor.getString(cursor.getColumnIndex(ROOT_PATH));
                String encryptionName = cursor.getString(cursor.getColumnIndex(ENCRYPTION_NAME));
                String encryptionFilePath = SafeUtils.getEncryptionPath(rootPath, encryptionName);
                if (encryptionFilePath != null) {
                    File encryptionFile = new File(encryptionFilePath);
                    if (!encryptionFile.exists()) {
                        continue;
                    } else if (SafeUtils.decryptFile(encryptionFile, oldCipher)) {
                        SafeUtils.encryptFile(encryptionFile, newCipher);
                        File thumbFile = SafeUtils.getThumbFile(encryptionFile);
                        if (thumbFile.exists()) {
                            if (SafeUtils.decryptFile(thumbFile, oldCipher)) {
                                SafeUtils.encryptFile(thumbFile, newCipher);
                            } else {
                                if (SafeUtils.DEBUG) {
                                    Log.v(TAG, "thumb file decrypt fail path = " + thumbFile.getAbsolutePath());
                                }
                            }
                        }
                    } else {
                        try {
                            if (SafeUtils.DEBUG) {
                                Log.v(TAG, "decrypt fail path = " + encryptionFilePath);
                            }
                        } catch (Throwable th) {
                            cursor.close();
                        }
                    }
                } else {
                    if (SafeUtils.DEBUG) {
                        Log.v(TAG, "rootPath = " + rootPath + "|| encryptionName = " + encryptionName);
                    }
                }
                cursor.moveToNext();
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        final boolean success = isSuccess;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                if (mListener != null) {
                    mListener.onModifyComplete(success);
                }
            }
        });
    }

    public void setNewPassword(String password) {
        mNewPassword = password;
    }

    public void setOnModifyPasswordListener(OnModifyPasswordListener listener) {
        mListener = listener;
    }
}
