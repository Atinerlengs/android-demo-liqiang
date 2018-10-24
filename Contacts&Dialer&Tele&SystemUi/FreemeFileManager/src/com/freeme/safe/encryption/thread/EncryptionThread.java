package com.freeme.safe.encryption.thread;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;
import java.util.ArrayList;

public class EncryptionThread extends Thread {
    private static final String TAG = "EncryptionThread";

    public interface OnEncryptionCompleteListener {
        void onEncryptionComplete();
    }

    private String mPassword;
    private boolean mMediaScanState;
    private boolean mStop;
    private int mCurrentFileType;
    private int mTaskCount;

    private ArrayList<String> mScanList = new ArrayList();
    private ArrayList<FileInfo> mTaskList;

    private Dialog mDialog;
    private ProgressDialog mEncryptionDialog;

    private Context mContext;
    private Handler mHandler;

    private OnEncryptionCompleteListener mListener;

    private IEncryptionService mService;

    public EncryptionThread(Context context, Dialog dialog, int type, IEncryptionService service) {
        mContext = context;
        mService = service;
        mHandler = new Handler();
        mDialog = dialog;
        mCurrentFileType = type;
        createEncryptionDialog(mContext);
    }

    @Override
    public void run() {
        innerRun();
    }

    public boolean addEncryptionTask(ArrayList<FileInfo> files) {
        if (files == null || files.size() == 0) {
            return false;
        }
        if (mScanList != null) {
            mScanList.clear();
        }
        mTaskList = (ArrayList) files.clone();
        mTaskCount = mTaskList.size();
        mMediaScanState = false;
        return true;
    }

    public void setOnEncryptionCompleteListener(OnEncryptionCompleteListener listener) {
        mListener = listener;
    }

    private void createEncryptionDialog(Context context) {
        mEncryptionDialog = new ProgressDialog(context);
        mEncryptionDialog.setTitle(R.string.encryption_menu);
        mEncryptionDialog.setMessage(context.getString(R.string.encryp_progress_text));
        mEncryptionDialog.setIndeterminate(true);
        mEncryptionDialog.setCancelable(true);
        mEncryptionDialog.setCanceledOnTouchOutside(false);
        mEncryptionDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                stopThread();
            }
        });
        mEncryptionDialog.setButton(context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                stopThread();
            }
        });
    }

    private void stopThread() {
        mStop = true;
        try {
            if (mService != null) {
                mService.setStopEncryption(true);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
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
        for (File f: files) {
            if (f.isDirectory()) {
                count += detectCurrFileCount(f);
            } else {
                count++;
            }
        }
        return count;
    }

    private void innerRun() {
        showDialog();

        int i = 0;
        if (mPassword == null) {
            mPassword = SafeUtils.getPasswordWithoutEncryption(mContext);
            if (mPassword == null) {
                mStop = true;
                Log.e(TAG, "password do not exist");
            }
        }
        dissmissDialog();
        showEncryptionDialog();
        while (!interrupted() && !mStop && i < mTaskCount) {
            FileInfo fileinfo = mTaskList.get(i);
            File file = new File(fileinfo.filePath);
            if (file.exists()) {
                long fileLength = detectFile(file);
                if (fileLength < SafeConstants.ENCRYPTION_SIZE_MAX) {
                    int fileType = MediaFile.getMediaType(fileinfo.filePath);
                    if (!SafeUtils.isStorageEnable(fileType, fileLength, null)) {
                        if (!doStorageNotEnough(null)) {
                            mStop = true;
                            break;
                        }
                    }
                } else {
                    showToast(file);
                    i++;
                }
            }
            try {
                if (mService != null) {
                    mService.encryptionTask(file.getAbsolutePath(), mCurrentFileType, mMediaScanState);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            i++;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mStop) {
                    mEncryptionDialog.dismiss();
                }
                if (mListener != null) {
                    mListener.onEncryptionComplete();
                }
            }
        });
    }

    private boolean doStorageNotEnough(String rootPath) {
        return true;
    }

    private void showToast(File file) {
        String string;
        StringBuilder append = new StringBuilder().append(file.getName());
        if (file.isDirectory()) {
            string = mContext.getString(R.string.name_folder);
        } else {
            string = mContext.getString(R.string.name_file);
        }
        final String head = append.append(string).toString();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, mContext.getString(R.string.file_size_too_large,
                        new Object[]{head}), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null) {
                    mDialog.show();
                }
            }
        });
    }

    private void showEncryptionDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mEncryptionDialog != null && !mEncryptionDialog.isShowing()) {
                    mEncryptionDialog.show();
                }
            }
        });
    }

    private void dissmissDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null && mDialog.isShowing()) {
                    try {
                        mDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private long detectFile(File file) {
        long size = 0;
        if (file == null || !file.exists()) {
            return 0;
        }
        if (!file.isDirectory()) {
            return file.length();
        }
        File[] files = file.listFiles();
        if (files == null || files.length <= 0) {
            return 0;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                size += detectFile(f);
            } else {
                size += f.length();
            }
        }
        return size;
    }
}