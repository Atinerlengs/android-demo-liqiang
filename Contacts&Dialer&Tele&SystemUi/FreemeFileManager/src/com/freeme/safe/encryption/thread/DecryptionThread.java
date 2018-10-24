package com.freeme.safe.encryption.thread;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.model.EncryptionFileInfo;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;
import java.util.ArrayList;
import javax.crypto.Cipher;

public class DecryptionThread extends Thread {
    private static final String TAG = "DecryptionThread";

    public interface OnDecryptionCompleteListener {
        void onDecryptionComplete();
    }

    private String mTargetPath;

    private boolean mStop;
    private boolean mIsReplaced;

    private int mTaskCount;

    private ArrayList mTaskList;

    private Dialog mDialog;
    private AlertDialog mReplacedDialog;
    private ProgressDialog mDecryptionDialog;

    private Handler mHandler;
    private Activity mActivity;

    private OnDecryptionCompleteListener mListener;

    private IEncryptionService mService;

    private final Object mLock = new Object();

    public DecryptionThread(Activity activity, Dialog dialog, IEncryptionService service) {
        mActivity = activity;
        mService = service;
        mHandler = new Handler();
        mDialog = dialog;
        createDecryptionDialog(mActivity);
        createReplacedDialog(mActivity);
    }

    @Override
    public void run() {
        innerRun();
    }

    private void createDecryptionDialog(Activity activity) {
        mDecryptionDialog = new ProgressDialog(activity);
        mDecryptionDialog.setTitle(R.string.decryption_menu);
        mDecryptionDialog.setMessage(activity.getString(R.string.dencryp_progress_text));
        mDecryptionDialog.setIndeterminate(true);
        mDecryptionDialog.setCancelable(true);
        mDecryptionDialog.setCanceledOnTouchOutside(false);
        mDecryptionDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                stopThread();
            }
        });
        mDecryptionDialog.setButton(activity.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                stopThread();
            }
        });
    }

    private void createReplacedDialog(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setNeutralButton(R.string.replace_yes, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                mIsReplaced = true;
                synchronized (mLock) {
                    mLock.notifyAll();
                }
                dialogInterface.cancel();
            }
        })
                .setNegativeButton(R.string.cancel, null);

        Util.setFreemeDialogOption(builder, Util.FREEME_DIALOG_OPTION_BOTTOM);
        mReplacedDialog = builder.create();
    }

    public boolean addDecryptionTask(ArrayList<EncryptionFileInfo> list, String targetPath) {
        if (list == null || list.size() == 0 || targetPath == null || !new File(targetPath).exists()) {
            return false;
        }
        mTaskList = (ArrayList) list.clone();
        mTaskCount = list.size();
        mTargetPath = targetPath;
        return true;
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

    private void innerRun() {
        showDialog();
        int i = 0;
        File encrypFile = null;
        Cipher cipher = null;
        String targetRootPath = SafeUtils.fetchRootPathByFile(mTargetPath);
        String password = SafeUtils.getPasswordWithoutEncryption(mActivity);
        if (password == null) {
            mStop = true;
            Log.e(TAG, "password do not exist");
        }
        dissmissDialog();
        showDecryptionDialog();

        boolean internalState = false;
        boolean newFileExit = false;
        while (!interrupted() && !mStop && i < mTaskCount) {
            EncryptionFileInfo info = (EncryptionFileInfo) mTaskList.get(i);
            boolean hasException = false;
            File targetFile = null;
            String rootPath = info.getRootPath();
            String encrypName = info.getEncryptionName();
            String originalPath = info.getOriginalPath();
            long originalSize = info.getOriginalFileSize();
            if (encrypName != null) {
                if (!SafeUtils.isStorageEnable(info.getOriginalFileType(), originalSize, targetRootPath)) {
                    if (!doStorageNotEnough(targetRootPath)) {
                        mStop = true;
                        break;
                    }
                }
                if (cipher == null) {
                    cipher = SafeUtils.initAESCipher(SafeConstants.ENCRYPTION_KEY + password, Cipher.DECRYPT_MODE);
                }
                String originalName = SafeUtils.getOriginalFileName(originalPath);
                if (originalName != null) {
                    targetFile = new File(mTargetPath + File.separator + originalName);
                    if (targetFile.exists()) {
                        doTargetFileExist(targetFile.getAbsolutePath());
                    }
                }
                File fileWrapper = new File(SafeUtils.getEncryptionPath(info.getEncryptionName()));
                String internalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                if (fileWrapper.exists()) {
                    newFileExit = true;
                    internalState = !(targetRootPath == null || !targetRootPath.equals(internalPath));
                }
                if (newFileExit) {
                    encrypFile = fileWrapper;
                } else if (!TextUtils.isEmpty(SafeUtils.getEncryptionPath(rootPath, encrypName))) {
                    encrypFile = new File(SafeUtils.getEncryptionPath(rootPath, encrypName));
                }
                if (SafeUtils.DEBUG) {
                    Log.d(TAG, "newFileExit = " + newFileExit + ",internalState = " + internalState);
                }
                if (encrypFile == null || !encrypFile.exists()) {
                    encrypOldFile(info, rootPath, encrypName, originalPath, password, targetRootPath);
                } else {
                    if (!newFileExit) {
                        SafeUtils.decryptFile(encrypFile, cipher);
                    }
                    if (originalName != null) {
                        if (!targetFile.exists() || mIsReplaced) {
                            if (newFileExit) {
                                hasException = decryptionNewFile(info, rootPath, encrypName, originalPath,
                                        password, targetRootPath);
                            } else if (SafeUtils.isDirectlyEncryp(info.getOriginalFileType())) {
                                renameTo(encrypFile, rootPath, targetFile);
                            }
                        }
                        if (!(mStop || hasException)) {
                            deleteDatabase(encrypName);
                        }
                    }
                }
            }
            i++;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mStop) {
                    mDecryptionDialog.dismiss();
                } else if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                }
                if (mListener != null) {
                    mListener.onDecryptionComplete();
                }
            }
        });
    }

    private boolean doStorageNotEnough(String targetRootPath) {
        if (SafeUtils.DEBUG) {
            Log.v(TAG, "doStorageNotEnough rootPath = " + targetRootPath);
        }
        if (mDecryptionDialog != null && targetRootPath != null) {
            String message = null;
            if (targetRootPath.equalsIgnoreCase(Util.MEMORY_DIR)) {
                message = mActivity.getString(R.string.disk_space_not_enough,
                        mActivity.getString(R.string.storage_internal),
                        mActivity.getString(R.string.unable_to_dencryp));
            } else if (targetRootPath.equalsIgnoreCase(Util.SD_DIR)) {
                message = mActivity.getString(R.string.disk_space_not_enough,
                        mActivity.getString(R.string.storage_external),
                        mActivity.getString(R.string.unable_to_dencryp));
            }
            if (message != null) {
                Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mDecryptionDialog != null && mDecryptionDialog.isShowing()) {
                            mDecryptionDialog.dismiss();
                        }
                        if (mDialog != null && mDialog.isShowing()) {
                            mDialog.dismiss();
                        }
                    }
                });
                return false;
            }
        }
        return true;
    }

    private void encrypOldFile(EncryptionFileInfo info, String rootPath, String encrypName,
                               String originalPath, String password, String targetRootPath) {
        boolean hasException = false;
        if (!mStop) {
            hasException = decryptionNewFile(info, rootPath, encrypName, originalPath,
                    password, targetRootPath);
        }
        if (!hasException && !mStop) {
            String originalName = SafeUtils.getOriginalFileName(originalPath);
            String ext = null;
            if (originalPath.lastIndexOf(".") > 0) {
                ext = originalPath.substring(originalPath.lastIndexOf("."));
            }
            File file = new File(SafeUtils.getEncryptionPath(rootPath, encrypName) + ext);
            if (file.exists()) {
                boolean renameTo = file.renameTo(new File(mTargetPath + File.separator + originalName));
                if (renameTo) {
                    deleteDatabase(encrypName);
                }
            } else {
                deleteDatabase(encrypName);
            }
        }
    }

    private boolean decryptionNewFile(EncryptionFileInfo info, String rootPath, String encrypName,
                                      String originalPath, String password, String targetRootPath) {
        try {
            if (mService != null) {
                return !mService.decryptionTask(rootPath, targetRootPath, mTargetPath, encrypName,
                        originalPath, info.getOriginalFileType(), password, mIsReplaced);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    private void renameTo(File encrypFile, String rootPath, File targetFile) {
        if (!mStop) {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "mTargetPath = " + mTargetPath + ",rootPath = " + rootPath);
            }
            if (mTargetPath.startsWith(rootPath)) {
                encrypFile.renameTo(targetFile);
            } else if (copyFile(encrypFile, targetFile) && !mStop) {
                encrypFile.delete();
            }
        }
    }

    private boolean deleteDatabase(String encryptionName) {
        if (SafeUtils.DEBUG) {
            Log.v(TAG, "deleteDatabase encryptionName = " + encryptionName);
        }
        if (encryptionName != null) {
            String selection = EncryptionColumns.ENCRYPTION_NAME + "=?";
            if (mActivity.getContentResolver().delete(EncryptionColumns.FILE_URI,
                    selection, new String[]{encryptionName}) > 0) {
                return true;
            }
        }
        return false;
    }

    private void showDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDialog != null && !mDialog.isShowing()) {
                    mDialog.show();
                }
            }
        });
    }

    private void showDecryptionDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mDecryptionDialog != null && !mDecryptionDialog.isShowing()) {
                    mDecryptionDialog.show();
                }
            }
        });
    }

    private void doTargetFileExist(final String targetPath) {
        if (SafeUtils.DEBUG) {
            Log.v(TAG, "doTargetFileExist targetPath = " + targetPath);
        }
        if (mReplacedDialog != null && targetPath != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mDialog != null && mDialog.isShowing()) {
                        mDialog.dismiss();
                    }

                    if (mDecryptionDialog != null && mDecryptionDialog.isShowing()) {
                        mDecryptionDialog.dismiss();
                    }

                    if (mActivity != null && !mActivity.isFinishing()
                            && mReplacedDialog != null && !mReplacedDialog.isShowing()) {
                        mReplacedDialog.setTitle(mActivity.getString(R.string.replace_file_text, targetPath));
                        mReplacedDialog.show();
                    }
                }
            });
            synchronized (mLock) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (SafeUtils.DEBUG) {
                Log.v(TAG, "doTargetFileExist mIsReplaced = " + mIsReplaced);
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mReplacedDialog != null && mReplacedDialog.isShowing()) {
                        mReplacedDialog.dismiss();
                    }
                    if (mActivity != null && !mActivity.isFinishing()) {
                        if (mDecryptionDialog != null && !mDecryptionDialog.isShowing()) {
                            mDecryptionDialog.show();
                        }
                    }
                }
            });
        }
    }

    private boolean copyFile(File sourceFile, File targetFile) {
        String copyResult = Util.copyFile(sourceFile.getAbsolutePath(), targetFile.getAbsolutePath());
        return copyResult != null;
    }

    public void setOnDecryptionCompleteListener(OnDecryptionCompleteListener listener) {
        mListener = listener;
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
}
