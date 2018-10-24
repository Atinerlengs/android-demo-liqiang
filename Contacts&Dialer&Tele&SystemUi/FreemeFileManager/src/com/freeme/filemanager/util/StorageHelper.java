package com.freeme.filemanager.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.util.Util.SDCardInfo;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class StorageHelper {
    private static final String LOG_TAG = "StorageHelper";
    private static Context mContext;
    private static StorageHelper storageHelper;
    private String mCurrentMountPoint;
    private StorageManager mStorageManager;

    private StorageHelper(Context paramContext) {
        this.mStorageManager = ((StorageManager) paramContext
                .getSystemService("storage"));
        //*/add by mingjun on 2015-12-31 for storage path
        Util.setMountedStorageBySubPath(mContext, this.mStorageManager);
    }

    public static StorageHelper getInstance(Context paramContext) {
        if (storageHelper == null) {
            storageHelper = new StorageHelper(paramContext);
            mContext = paramContext;
        }
        return storageHelper;
    }

    public void release() {
        mContext = null;
        storageHelper = null;
    }

    public long destVolumeFreeSpace(String paramString) {
        return getStorageInfoForVolume(Util.getMountedStorageBySubPath(
                mContext, paramString)).free;
    }

    public String getCurrentVolume() {
        return this.mCurrentMountPoint;
    }

    public int getMountedVolumeCount() {
        int count = 0;
        StorageVolume[] storageVolumes = this.mStorageManager.getVolumeList();
        for (int j = 0; j < storageVolumes.length; ++j) {
            StorageVolume storageVolume = storageVolumes[j];
            if (isVolumeMounted(storageVolume.getPath())) {
                count = count + 1;
            }
        }
        return count;
    }

    public ArrayList<StorageVolume> getMountedVolumeList() {
        StorageVolume[] storageVolumes = this.mStorageManager.getVolumeList();
        ArrayList<StorageVolume> mountVolumeList = new ArrayList<StorageVolume>();
        int i = storageVolumes.length;
        Log.i("liuhaoran2", "storageVolumes------------" + i);
        for (int j = 0; j < i; ++j) {
            StorageVolume localStorageVolume = storageVolumes[j];
            Log.i("liuhaoran2", "localStorageVolume = " + localStorageVolume.toString());
            if (!isVolumeMounted(localStorageVolume.getPath()))
                continue;
            mountVolumeList.add(localStorageVolume);
            Log.i("liuhaoran2", "mountVolumeList==== " + mountVolumeList.size());
        }
        return mountVolumeList;
    }

    public ArrayList<StorageVolume> getSortedMountVolumeList() {
        StorageVolume[] storageVolumes = this.mStorageManager.getVolumeList();
        ArrayList<StorageVolume> mountVolumeList = new ArrayList<StorageVolume>();
        int i = storageVolumes.length;
        for (int j = 0; j < i; ++j) {
            StorageVolume storageVolume = storageVolumes[j];
            //TODO
            if (!isVolumeMounted(storageVolume.getPath()))
                continue;
            mountVolumeList.add(storageVolume);
        }
        if (mountVolumeList.size() == 2) {
            StorageVolume storageVolume1 = mountVolumeList.get(0);
            StorageVolume storageVolume2 = mountVolumeList.get(1);
            if (!mContext.getString(R.string.storage_phone).equals(storageVolume1.getDescription(mContext))) {
                mountVolumeList.clear();
                mountVolumeList.add(storageVolume2);
                mountVolumeList.add(storageVolume1);
            }
        } else if (mountVolumeList.size() == 3) {
            //modified by TYD mingjun for storage switch
            ArrayList<StorageVolume> newMountVolumeList = new ArrayList<StorageVolume>();
            newMountVolumeList.add(mountVolumeList.get(0));
            newMountVolumeList.add(mountVolumeList.get(1));
            newMountVolumeList.add(mountVolumeList.get(2));
            for (StorageVolume storageVolume : mountVolumeList) {
                if (mContext.getString(R.string.storage_phone).equals(storageVolume.getDescription(mContext))) {
//                  newMountVolumeList.set(0, storageVolume);
                } else if (mContext.getString(R.string.storage_sd_card).equals(storageVolume.getDescription(mContext))) {
//                  newMountVolumeList.set(1, storageVolume);
                } else {
//                  newMountVolumeList.set(2, storageVolume);
                }
            }
            //end
            return newMountVolumeList;
        }

        return mountVolumeList;
    }

    public StorageVolume getPrimaryStorageVolume() {
        ArrayList<StorageVolume> mountVolumeList = getMountedVolumeList();
        int i = mountVolumeList.size();
        StorageVolume storageVolume = null;

        if (i > 0) {
            for (int j = 0; j < i; j++) {
                storageVolume = (StorageVolume) mountVolumeList.get(j);
                if (isVolumeMounted(storageVolume.getPath()) && mContext.getString(R.string.storage_phone).equals(((StorageVolume) mountVolumeList.get(j)).getDescription(mContext))) {
                    storageVolume = (StorageVolume) mountVolumeList.get(j);
                    return storageVolume;
                }
            }
        }
        return storageVolume;
    }

    public StorageVolume getLatestMountedVolume() {
        ArrayList<StorageVolume> mountVolumeList = getSortedMountVolumeList();
        if (mountVolumeList == null || mountVolumeList.size() == 0) {
            return null;
        }
        return mountVolumeList.get(mountVolumeList.size() - 1);
    }

    public MountedStorageInfo getMountedStorageInfo() {
        MountedStorageInfo mountedInfo = new MountedStorageInfo();
        StorageVolume[] arrayOfStorageVolume = this.mStorageManager.getVolumeList();
        for (int i = 0; i < arrayOfStorageVolume.length; ++i) {
            String str = arrayOfStorageVolume[i].getPath();
            MountedStorageInfo volumInfo = getStorageInfoForVolume(Util.getMountedStorageBySubPath(mContext, str));
            if (volumInfo == null)
                continue;
            mountedInfo.free += volumInfo.free;
            mountedInfo.total += volumInfo.total;
        }
        if (mountedInfo.total == 0L)
            mountedInfo = null;
        return mountedInfo;
    }

    public MountedStorageInfo getStorageInfoForVolume(StorageVolume storageVolume) {
        MountedStorageInfo storageInfo = new MountedStorageInfo();
        if ((storageVolume == null) || (storageVolume.getPath() == null))
            return null;
        String str = storageVolume.getPath();
        if (isVolumeMounted(str)) {
            try {
                StatFs localStatFs = new StatFs(str);
                long l1 = localStatFs.getBlockCount();
                long l2 = localStatFs.getBlockSize();
                long l3 = localStatFs.getAvailableBlocks();
                storageInfo.total = (l1 * l2);
                storageInfo.free = (l3 * l2);
                return storageInfo;
            } catch (IllegalArgumentException localIllegalArgumentException) {
                Log.e("StorageHelper", "statfs failed",
                        localIllegalArgumentException);
            }
        }
        return null;
    }

    public boolean isCurrentVolumeMounted() {
        return isVolumeMounted(this.mCurrentMountPoint);
    }

    //*/ freeme.liuhaoran , 20160722 , judge SD state
    public String getStorageState(String path) {
        try {

            Method getVolumeStateMethod = StorageManager.class.getMethod("getVolumeState", new Class[]{String.class});
            String state = (String) getVolumeStateMethod.invoke(this.mStorageManager, path);
            Log.i("liuhaoran", "state = " + state);
            return state;
        } catch (Exception e) {
        }
        return null;
    }

    public boolean isVolumeMounted(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.L) {
            if (this.mStorageManager != null) {
                if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState(new File(path)))) {
                    return false;
                }
            }
        } else {
            if (!Environment.MEDIA_MOUNTED.equals(getStorageState(path))) {
                return false;
            }
        }
        //*/

        Util.UsbStorageInfo usbStorageInfo = null;
        SDCardInfo sdCardInfo = null;
        MemoryCardInfo memoryCardInfo = null;

        if (!TextUtils.isEmpty(Util.SD_DIR) && path.startsWith(Util.SD_DIR)) {
            sdCardInfo = Util.getSDCardInfo();
            if (sdCardInfo != null && sdCardInfo.total != sdCardInfo.free) {
                return true;
            }
        }
        if (!TextUtils.isEmpty(Util.USBOTG_DIR) && path.startsWith(Util.USBOTG_DIR)) {
            usbStorageInfo = Util.getUsbStorageInfo();
            if (usbStorageInfo != null && usbStorageInfo.total > 0 && usbStorageInfo.free != usbStorageInfo.total) {
                return true;
            }
        }
        if (path.startsWith(Util.getDefaultPath())) {
            memoryCardInfo = Util.getMemoryCardInfo();
            if (memoryCardInfo != null && memoryCardInfo.total > 0) {
                return true;
            }
        }
        return false;
        //*/
    }

    public void setCurrentMountPoint(String paramString) {
        StorageVolume[] arrayOfStorageVolume = this.mStorageManager.getVolumeList();
        int i = arrayOfStorageVolume.length;
        for (int j = 0; j < i; j++) {
            if (!arrayOfStorageVolume[j].getPath().equals(paramString)) {
                continue;
            }
            this.mCurrentMountPoint = paramString;
        }
    }

    public static class MountedStorageInfo {
        public long free;
        public long total;
    }
}
