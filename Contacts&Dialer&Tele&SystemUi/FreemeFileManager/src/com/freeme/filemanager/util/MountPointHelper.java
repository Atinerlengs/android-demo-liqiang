/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.FileManagerLog;

import android.content.Context;
import android.drm.DrmManagerClient;
import android.os.Environment;
import android.os.Handler;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;


public final class MountPointHelper {   
    private static final String TAG = "MountPointHelper";

    public static final String HOME = "Home";
    public static final String SEPARATOR = "/";
    public static final String ROOT = "storage";
    public static final String ROOT_PATH = "/storage";

    private static final String DOCUMENT = "Document";
    private static final String DOWNLOAD = "Download";
    private static final String MUSIC = "Music";
    private static final String PHOTO = "Photo";
    private static final String RECEIVED = "Received File";
    private static final String VIDEO = "Video";

    private static MountPointHelper sInstance = null;
    protected DrmManagerClient mDrmManagerClient = null;

    private Context mContext = null;
    private ArrayList<FileInfo> mMountedFileInfos = new ArrayList<FileInfo>();
    private StorageManager mStorageManager = null;
    private StorageVolume[] mVolumeList = null;

    private ArrayList<String> mSystemFolderPaths = new ArrayList<String>(0);
    private HashMap<String, Integer> mSystemFolderIcons = new HashMap<String, Integer>();

    private MountPointHelper() {
    }

    public static MountPointHelper getInstance() {
        if (sInstance == null) {
            sInstance = new MountPointHelper();
        }
        return sInstance;
    }

    public void init(Context context, DrmManagerClient drmManagerClient) {
        mContext = context;
        mDrmManagerClient = drmManagerClient;
        mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        updateMountedPointList();
    }

    public void updateMountedPointList() {
        mVolumeList = mStorageManager.getVolumeList();

        if (mVolumeList != null) {
            mMountedFileInfos.clear();
            for (StorageVolume volume : mVolumeList) {
                if (volume != null && isPointMounted(volume.getPath())) {
                    try {
                        File file = new File(volume.getPath());
                        FileInfo info = new FileInfo(mContext, file, mDrmManagerClient);
                        info.setFileDescription(volume.getDescription(mContext));
                        mMountedFileInfos.add(info);

                        FileManagerLog.i(TAG, "MountPoint:" + info.getFileDescription() + ", path="
                                + info.getFilePath());
                    } catch (IllegalArgumentException e) {
                        FileManagerLog.e(TAG, "updateMountedPointList error: "
                                + volume.getDescription(mContext));
                    }
                }
            }
        }

        updateSystemFolderPath();
    }

    public void updateSystemFolderPath() {
        if (OptionsUtil.isOp02Enabled()) {
            final String defaultPath = getDefaultPath();
            mSystemFolderPaths.clear();
            mSystemFolderPaths.add(defaultPath + SEPARATOR + DOCUMENT);
            mSystemFolderPaths.add(defaultPath + SEPARATOR + DOWNLOAD);
            mSystemFolderPaths.add(defaultPath + SEPARATOR + MUSIC);
            mSystemFolderPaths.add(defaultPath + SEPARATOR + PHOTO);
            mSystemFolderPaths.add(defaultPath + SEPARATOR + RECEIVED);
            mSystemFolderPaths.add(defaultPath + SEPARATOR + VIDEO);

            mSystemFolderIcons.clear();
            mSystemFolderIcons.put(defaultPath + SEPARATOR + DOCUMENT, R.drawable.fm_document_folder);
            mSystemFolderIcons.put(defaultPath + SEPARATOR + DOWNLOAD, R.drawable.fm_download_folder);
            mSystemFolderIcons.put(defaultPath + SEPARATOR + MUSIC, R.drawable.fm_music_folder);
            mSystemFolderIcons.put(defaultPath + SEPARATOR + PHOTO, R.drawable.fm_photo_folder);
            mSystemFolderIcons.put(defaultPath + SEPARATOR + RECEIVED, R.drawable.fm_received_folder);
            mSystemFolderIcons.put(defaultPath + SEPARATOR + VIDEO, R.drawable.fm_video_folder);

            createSystemFolder();
        }
    }

    public String getMountPointDescription(String fileName) {
        String description = null;
        if (mVolumeList != null) {
            for (StorageVolume volume : mVolumeList) {
                if (volume != null && volume.getPath().endsWith(fileName)) {
                    description = volume.getDescription(mContext);
                    break;
                }
            }
        }

        return description == null ? fileName : description;
    }

    public String getRootPath(Context context, String filePath) {
        if (mMountedFileInfos != null) {
            final int size = mMountedFileInfos.size();
            for (FileInfo mountedPoint : mMountedFileInfos) {
                final String mountPath = mountedPoint.getFilePath();
                if ((filePath + "/").startsWith(mountPath + "/")
                        || (filePath + "/").equals(mountPath + "/")) {
                    return mountPath;
                }
            }
        }
        return null;
    }

    private void createSystemFolder() {
        new Handler().post(new Runnable() {

            @Override
            public void run() {
                for (String path : mSystemFolderPaths) {
                    File dir = new File(path);
                    if (!dir.exists()) {
                        FileManagerLog.d(TAG, "mkdir path = " + path + "start");
                        boolean ret = dir.mkdirs();
                        FileManagerLog.d(TAG, "mkdir path = " + path + "end ret = " + ret);
                    }
                }
            }
        });
    }

    public static boolean isRootPath(String path) {
        return path.equals(ROOT_PATH);
    }

    public List<FileInfo> getMountPointFileInfo() {
        return mMountedFileInfos;
    }

    public boolean isFileRootMount(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            final String mountPointPath = getRealMountPointPath(filePath);
            return isPointMounted(mountPointPath);
        }
        return false;
    }

    protected boolean isPointMounted(String mountPointPath) {
        if (!TextUtils.isEmpty(mountPointPath)) {
            String state = mStorageManager.getVolumeState(mountPointPath);
            FileManagerLog.d(TAG, "isMounted = " + Environment.MEDIA_MOUNTED.equals(state)
                    + " state=" + state + "filePath:" + mountPointPath);
            return Environment.MEDIA_MOUNTED.equals(state);
        }
        return false;
    }

    public String getDefaultPath() {
        //return StorageManager.getDefaultPath();
        return "";
    }

    public boolean isMountPointPath(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            for (FileInfo mountPoint : mMountedFileInfos) {
                if ((filePath + SEPARATOR).equals(mountPoint.getFilePath() + SEPARATOR)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getRealMountPointPath(String path) {
        for (FileInfo mountPoint : mMountedFileInfos) {
            if ((path + SEPARATOR).startsWith(mountPoint.getFilePath() + SEPARATOR)) {
                FileManagerLog.d(TAG, "getRealMountPointPath = " + mountPoint.getFilePath());
                return mountPoint.getFilePath();
            }
        }
        FileManagerLog.d(TAG, "getRealMountPointPath = \"\" ");
        return "";
    }

    public boolean isSystemFolder(FileInfo fileInfo) {
        if (fileInfo.isDirectory()) {
            String filePath = fileInfo.getFilePath();
            for (String path : mSystemFolderPaths) {
                if (filePath.equalsIgnoreCase(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the file in path {@link SDCARD2_PATH}
     * @param fileInfo
     * @return
     */
    public boolean isExternalFile(FileInfo fileInfo) {
        if (fileInfo != null && mVolumeList != null) {
            final String mountPath = getRealMountPointPath(fileInfo.getFilePath());
            for (StorageVolume volume : mVolumeList) {
                if (volume != null && mountPath.equals(volume.getPath())) {
                    return volume.isRemovable();
                }
            }
        }
        return false;
    }
}