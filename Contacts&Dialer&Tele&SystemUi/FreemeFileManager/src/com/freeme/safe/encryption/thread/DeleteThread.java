package com.freeme.safe.encryption.thread;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;

import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.filemanager.R;
import com.freeme.safe.dialog.DialogFactory;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.model.EncryptionFileInfo;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;
import java.util.ArrayList;

public class DeleteThread extends Thread implements OnKeyListener {
    private static final String TAG = "DeleteThread";

    public interface OnDeleteCompleteListener {
        void onDeleteComplete();
    }

    private boolean mStop;
    private int mTaskCount;

    private ArrayList<EncryptionFileInfo> mTaskList;

    private Dialog mProgressDialog;

    private Context mContext;
    private Handler mHandler;

    private OnDeleteCompleteListener mListener;

    private IEncryptionService mService;

    public DeleteThread(Context context, IEncryptionService service) {
        mContext = context;
        mHandler = new Handler();
        mService = service;
        mProgressDialog = DialogFactory.getProgressDialog(mContext, context.getString(R.string.operation_deleting), this);
    }

    @Override
    public void run() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.show();
                }
            }
        });
        int i = 0;
        while (!interrupted() && !mStop && i < mTaskCount) {
            EncryptionFileInfo info = mTaskList.get(i);
            String rootPath = info.getRootPath();
            String encryptionName = info.getEncryptionName();
            try {
                if (mService != null) {
                    mService.deleteTask(encryptionName);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (!(rootPath == null || encryptionName == null)) {
                File encrypFile = new File(SafeUtils.getEncryptionPath(encryptionName));
                if (!encrypFile.exists()) {
                    encrypFile = new File(SafeUtils.getEncryptionPath(rootPath, encryptionName));
                }
                if (deleteFile(encrypFile)) {
                    deleteFile(SafeUtils.getThumbFile(encrypFile));
                    deleteDatabaseRecord(encryptionName);
                }
            }
            i++;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
                if (mListener != null) {
                    mListener.onDeleteComplete();
                }
            }
        });
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent keyEvent) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {
            return false;
        }
        if (SafeUtils.DEBUG) {
            Log.v(TAG, "cancel delete");
        }
        stopDeleteTask();
        return true;
    }

    public void setOnDeleteCompleteListener(OnDeleteCompleteListener listener) {
        mListener = listener;
    }

    public boolean addDeleteTask(ArrayList<EncryptionFileInfo> list) {
        if (list == null || list.size() == 0) {
            return false;
        }
        mTaskList = list;
        mTaskCount = list.size();
        mStop = false;
        return true;
    }

    public void stopDeleteTask() {
        if (isAlive()) {
            mStop = true;
        }
    }

    private boolean deleteFile(File file) {
        return file == null || !file.exists() || file.delete();
    }

    private void deleteDatabaseRecord(String encryptionName) {
        String[] selectionArgs = new String[]{encryptionName};
        String selection = EncryptionColumns.ENCRYPTION_NAME + "=?";
        mContext.getContentResolver().delete(EncryptionColumns.FILE_URI,
                selection, selectionArgs);
    }
}