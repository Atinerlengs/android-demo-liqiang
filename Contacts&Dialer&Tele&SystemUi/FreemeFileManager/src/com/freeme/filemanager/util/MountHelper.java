package com.freeme.filemanager.util;

import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.freeme.filemanager.R;

import java.util.List;

public class MountHelper {
    private static final String LOG_TAG = "MountHelper";
    private static Context mContext;
    private static MountHelper sInstance;
    private String mVolumpath;

    private MountHelper() {

    }

    public static MountHelper getInstance(Context context)
    {
        if (sInstance == null) {
            mContext = context;
            sInstance = new MountHelper();
        }
        MountHelper localMountHelper = sInstance;
        return localMountHelper;
    }

    public void unMount(String volumpath) {
        if(TextUtils.isEmpty(volumpath)){
            return;
        }
        mVolumpath = volumpath;
        doUnmount();
    }

    private void doUnmount(){
        Log.i("doUnmount", "mVolumpath is: "+mVolumpath);
        if(mVolumpath.startsWith(Util.SD_DIR)){
            Toast.makeText(mContext, R.string.unmount_sdcard_inform_text, Toast.LENGTH_SHORT).show();
        }else if(mVolumpath.startsWith(Util.USBOTG_DIR)){
            Toast.makeText(mContext, R.string.unmount_usb_storage_inform_text, Toast.LENGTH_SHORT).show();
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    IBinder iBinder = ServiceManager.getService("mount");
                    if (iBinder != null) {
                        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
                        sm.unmount(findVolumeIdForPathOrThrow(mVolumpath));
                    }
                } catch (Exception e) {
                    if(mVolumpath.startsWith("/storage")){
                        Toast.makeText(mContext, R.string.dlg_error_unmount_sdcard_text, Toast.LENGTH_SHORT).show();
                    }else if(mVolumpath.startsWith("/mnt")){
                        Toast.makeText(mContext, R.string.dlg_error_unmount_usb_storage_text, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }.start();
    }

    private String findVolumeIdForPathOrThrow(String path) {
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        List<VolumeInfo> volumes = sm.getVolumes();
        for (VolumeInfo vol : volumes) {
            if (vol.path != null && path.startsWith(vol.path)) {
                return vol.id;
            }
        }

        return null;
    }
}
