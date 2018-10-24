package com.freeme.safe.encryption.task;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.safe.utils.FilenameUtils;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;

public class ViewTask {

    private static final String TAG = "ViewTask";

    public String openSelectedFile(Context context, String path) {
        try {
            if (TextUtils.isEmpty(path) || !new File(path).exists()) {
                return null;
            }
            mkTempsFolder();
            String desPath = SafeConstants.TEMPS_FILE_PATH + File.separator + FilenameUtils.getBaseName(path);
            Runtime.getRuntime().exec("chmod 775 " + SafeConstants.PRIVATE_FILE_PATH);
            Runtime.getRuntime().exec("chmod 775 " + SafeConstants.TEMPS_FILE_PATH);
            Runtime.getRuntime().exec("chmod 774 " + path);
            Runtime.getRuntime().exec("ln -s " + path + " " + desPath);
            return desPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void restorePermittion() {
        try {
            if (SafeUtils.DEBUG) {
                Log.d(TAG, "restorePermittion");
            }
            Runtime.getRuntime().exec("chmod 750 " + SafeConstants.PRIVATE_FILE_PATH);
            File file = new File(SafeConstants.TEMPS_FILE_PATH);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mkTempsFolder() {
        File file = new File(SafeConstants.TEMPS_FILE_PATH);
        if (!file.exists()) {
            file.mkdirs();
        } else if (file.isFile()) {
            if (file.delete()) {
                file.mkdirs();
            }
        }
    }
}
