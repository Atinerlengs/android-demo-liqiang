/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.freeme.filemanager.model;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.drm.DrmManagerClient;
import android.text.TextUtils;

import com.freeme.filemanager.util.MimeUtils;
import com.freeme.filemanager.util.ScannerClient;
import com.freeme.filemanager.util.OptionsUtil;

public class FileInfo {
    private static String TAG = "FileInfo";
    private static final int NOT_DRM_FILE = -1;
    private static final int INVALID_DRM_ACTION_ID = -2;

    private final Context mContext;
    private final DrmManagerClient mDrmManagerClient;
    public File mFile;

    public String fileName;
    public String bucketId;

    public String filePath;
    public String fileFormatDateTime;
    public String mFileMimeType;

    public long fileSize;

    public boolean IsDir;

    public int Count;

    public int mFileIconResId;

    public long ModifiedDate;

    //*/ Added by tyd wulianghuan 2013-12-12, the folder's owner(App name)
    public String owner = null;
    //*/

    public boolean Selected;

    public boolean canRead;

    public boolean canWrite;

    public boolean isHidden;

    public long dbId;

    private long mFileLastModifiedTime = -1;
    private String mFileOrigMimeType = null;
    private String mFileName = null;
    public String mFilePath = null;
    private String mFileSizeStr = null;
    private long mFileSize = -1;
    private boolean mIsDir = false;
    private int mFileDrmActionId = INVALID_DRM_ACTION_ID;
    /** local description for mounted point */
    private String mDescription = null;
    /** Used in Adapter to indicate whether the file is selected */
    private boolean mChecked = false;
    /** Used in Adapter to indicate whether the file is cut */
    private boolean mIsCut = false;

    public FileInfo(){

        mContext = null;
        mDrmManagerClient = null;
    }
    public FileInfo(Context context, File file, DrmManagerClient drmManagerClient)
          throws IllegalArgumentException{
        if(file == null){
            throw new IllegalArgumentException();
        }
        mFile = file;
        mContext = context;
        mDrmManagerClient = drmManagerClient;

        mFileLastModifiedTime = mFile.lastModified();
        mFileName = mFile.getName();
        mFileSize = mFile.length();
        mIsDir = mFile.isDirectory();
    }

    public void setFileDescription(String description) {
        mDescription = description;
    }

    public String getFileDescription() {
        if (mDescription != null) {
            return mDescription;
        }
        return mFileName;
    }

    public String getFileName() {
        return mFileName;
    }

    public int getFileIconResId() {
        if (mFileIconResId == 0) {
            if (isDirectory()) {
                // get different icons for mount points and other directories
                mFileIconResId = EditUtility.getIconIdForDirectory(mContext, this);
            } else {
                if (mFileMimeType == null) {
                    mFileMimeType = getFileMimeType();
                }

                if (OptionsUtil.isDrmSupported()
                        && mFileMimeType.startsWith("application/vnd.oma.drm")) {
                    mFileOrigMimeType = getFileOriginalMimeType();
                }

                if (mFileOrigMimeType == null) {
                    mFileIconResId = EditUtility.getDrawableId(mFileMimeType);
                } else {
                    mFileIconResId = EditUtility.getDrawableId(mFileOrigMimeType);
                }
            }
        }
        return mFileIconResId;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public String getFileSizeStr() {
        if(mFileSizeStr == null){
            mFileSizeStr = EditUtility.sizeToString(mFileSize);
        }
        return mFileSizeStr;
    }

    public boolean isDirectory() {
        return mIsDir;
    }

    public int getFileDrmActionId() {
        if (mFileDrmActionId != INVALID_DRM_ACTION_ID) {
            return mFileDrmActionId;
        } else {
            if (OptionsUtil.isDrmSupported() && !isDirectory()) {
                if(mFileMimeType == null){
                    mFileMimeType = getFileMimeType();
                }
                if (mFileMimeType.startsWith("application/vnd.oma.drm")) {
                    if(mFileOrigMimeType == null){
                        mFileOrigMimeType = getFileOriginalMimeType();
                    }
                    if(mFileOrigMimeType != null){
                    //modify by mingjun
                     //   mFileDrmActionId = DrmUtils.getAction(mFileOrigMimeType);
                    } else {
                        mFileDrmActionId = NOT_DRM_FILE;
                    }
                } else {
                    mFileDrmActionId = NOT_DRM_FILE;
                }
            } else {
                mFileDrmActionId = NOT_DRM_FILE;
            }
            return mFileDrmActionId;
        }
    }

    public String getFileMimeType() {
        if (mFileMimeType == null && !isDirectory()) {
            int dotPosition = filePath.lastIndexOf('.');
            if (dotPosition == -1) {
                return "*/*";
            }

            String ext = filePath.substring(dotPosition + 1, filePath.length()).toLowerCase();
            if (MimeUtils.is3GPPExtension(ext)) {
                mFileMimeType = EditUtility.getMimeTypeForFile(mContext, new File(filePath));
            } else {
                mFileMimeType = MimeUtils.guessMimeTypeFromExtension(ext);
            }
            return mFileMimeType != null ? mFileMimeType : "*/*";
        }
        return mFileMimeType;
    }

    public String getFileOriginalMimeType() {
        if (OptionsUtil.isDrmSupported() && !isDirectory() && TextUtils.isEmpty(mFileOrigMimeType)) {
            if (mFileMimeType == null) {
                mFileMimeType = getFileMimeType();
            }
            if (mFileMimeType.startsWith("application/vnd.oma.drm")) {
                if (mFilePath == null) {
                    mFilePath = getFilePath();
                }

                if (TextUtils.isEmpty(mFileOrigMimeType)) {
                    mFileOrigMimeType = mDrmManagerClient.getOriginalMimeType(mFilePath);
                }
            } else {
                mFileOrigMimeType = null;
            }
            FileManagerLog.d(TAG, "Drm file original mimetype: " + mFileOrigMimeType);
        }
        return mFileOrigMimeType;
    }

    public long getFileLastModifiedTime() {
        return mFileLastModifiedTime;
    }

    public boolean isDrmFile(){
        if (isDirectory()) {
            return false;
        } else {
            if (mFileMimeType == null) {
                mFileMimeType = getFileMimeType();
            }
            if (mFileMimeType != null && mFileMimeType.startsWith("application/vnd.oma.drm")) {
                return true;
            } else {
                return false;
            }
        }
    }

    public String getFilePath(){
        if(mFilePath == null){
            mFilePath = mFile.getAbsolutePath();
        }
        return mFilePath;
    }

    public File getFile(){
        return mFile;
    }

    /**
     * This method renames a file
     @param  context     the context of FileManagerOperationActiviy, for notify others
     @param  newP        the new file path
     @return             true if rename succeeds, false otherwise
     */
    public boolean rename(Context context, String newP, ScannerClient scannerClient) {
        String newPath = newP;
        FileManagerLog.d(TAG, "Rename to new file: " + newP);
        boolean result = false;
        try {
            newPath = newPath.trim();
            File newFile = new File(newPath);

            if(newFile.exists()){
                result = false;
            } else {
                result = mFile.renameTo(newFile);
                FileManagerLog.d(TAG, "The result of renaming a file/folder: " + result);
                if (result) {
                    scannerClient.scanPath(newFile.getAbsolutePath());
                    mFile = newFile;
                    mFileLastModifiedTime = mFile.lastModified();
                    mFileName = mFile.getName();

                    mFileMimeType = null;
                    mFileOrigMimeType = null;
                    mFilePath = null;
                    mFileIconResId = 0;
                    mFileDrmActionId = INVALID_DRM_ACTION_ID;
                    EditUtility.notifyUpdates(mContext, Intent.ACTION_MEDIA_MOUNTED, mFile);
                }
            }
        } catch (Exception e) {
            FileManagerLog.e(TAG, "Failed to rename a file/folder", e);
        }
        return result;
    }

    public void setChecked(boolean flag) {
        mChecked = flag;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setCut(boolean flag) {
        mIsCut = flag;
    }

    public boolean isCut() {
        return mIsCut;
    }
}
