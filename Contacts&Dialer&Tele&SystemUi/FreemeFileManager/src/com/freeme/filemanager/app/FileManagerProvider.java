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

package com.freeme.filemanager.app;

import java.io.File;

import com.freeme.filemanager.controller.FileInfoComparator;
import com.freeme.filemanager.model.FileManagerLog;
import com.freeme.filemanager.util.StorageHelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

//import com.mediatek.featureoption.FeatureOption;


public class FileManagerProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse("content://com.freeme.filemanager.provider");

    // columns
    public static final String ID = "_id";
    public static final String CATEGORY = "category";
    public static final String PATH = "path";
    public static final String PATH_SEPERATOR = "/";

    // operators
    protected static final int CU = 1;
    protected static final int OTHER = 2;

    // Auto assort category folder
    protected static final String DOCUMENT_CATEGORY = "Document";
    protected static final String DOWNLOAD_CATEGORY = "Download";
    protected static final String MUSIC_CATEGORY = "Music";
    protected static final String PHOTO_CATEGORY = "Photo";
    protected static final String RECEIVED_CATEGORY = "Received File";
    protected static final String VIDEO_CATEGORY = "Video";

    public static final String DEFAULT_MOUNTPOINT_PATH = "/mnt/sdcard";
    // category folders for CU; must be a sorted array for comparator use
    static final String[] CATEGORY_CU = { DOCUMENT_CATEGORY, DOWNLOAD_CATEGORY, MUSIC_CATEGORY,
            PHOTO_CATEGORY, RECEIVED_CATEGORY, VIDEO_CATEGORY };

    private static String mDefaultCategoryRootPath = null;
    //*/ Modified by TYD Biantao, 20120709. Support special media path on external sd. 
    public static final String AUTHORITY = "com.freeme.filemanager.provider";
    public static final String EXTERNAL = "external";
    public static final Uri EXTERNAL_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + EXTERNAL);
    private static final int EXTERNAL_SD = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, EXTERNAL, EXTERNAL_SD);
    }
   
    private String[] mExternalPath;
    private static String sdPath;
    //*/

    String[] mCategory;
    String[] mPath;

    private StorageManager mStorageManager;
    //int mOp = 0;
    private static final String TAG = "FileManagerProvider";

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    private void checkCategoryPath(String defaultStoragePath) {
        mDefaultCategoryRootPath = defaultStoragePath;
        mPath = new String[mCategory.length];

        for (int i = 0; i < mPath.length; i++) {
            mPath[i] = mDefaultCategoryRootPath + PATH_SEPERATOR + mCategory[i];
        }
    }
    
    public FileManagerProvider(Context mContext){
        this.mStorageManager = ((StorageManager)mContext.getSystemService("storage"));
        StorageVolume storageVolume = null;
        StorageVolume[] sv = this.mStorageManager.getVolumeList();
        Log.i(TAG, "sv=" + sv);
        if (sv.length > 1) {
            // sv[0] is internal storage, sv[1] is sdcard, and sv[2] is the usb otg
            storageVolume = sv[1];
            sdPath = storageVolume.getPath();
        }
        Log.i(TAG, "storageVolume=" + storageVolume);
      }
      protected static final String EXTERNAL_DOCUMENT_CATEGORY_PATH = sdPath + "/Document";
      protected static final String EXTERNAL_DOWNLOAD_CATEGORY_PATH = sdPath + "/Download";
      protected static final String EXTERNAL_MUSIC_CATEGORY_PATH = sdPath + "/Music";
      protected static final String EXTERNAL_PHOTO_CATEGORY_PATH = sdPath + "/Photo";
      protected static final String EXTERNAL_RECEIVED_CATEGORY_PATH = sdPath + "/Received File";
      protected static final String EXTERNAL_VIDEO_CATEGORY_PATH = sdPath + "/Video";
      private static final String[] EXTERNAL_PATH_CU = {
                                      EXTERNAL_DOCUMENT_CATEGORY_PATH,
                                      EXTERNAL_DOWNLOAD_CATEGORY_PATH,
                                      EXTERNAL_MUSIC_CATEGORY_PATH,
                                      EXTERNAL_PHOTO_CATEGORY_PATH,
                                      EXTERNAL_RECEIVED_CATEGORY_PATH,
                                      EXTERNAL_VIDEO_CATEGORY_PATH,
                                  };
      
    @Override
    public boolean onCreate() {
        if (
            /*/ Modified by TYD Biantao, 20120703. Enable the special path selection.
            SystemProperties.get("ro.operator.optr").equals("OP02")
            /*/
            true
            //*/
                ) {   //CU
            FileManagerLog.d(TAG, "onCreate: CU operator");
            //mOp = CU;
            mCategory = CATEGORY_CU;
            //*/  Modified by TYD Biantao, 20120709. Support special media path on external sd. 
            mExternalPath = EXTERNAL_PATH_CU;
            checkCategoryPath(DEFAULT_MOUNTPOINT_PATH);
            //*/
        } else { // general or CMCC
            FileManagerLog.d(TAG, "onCreate: other operator");
            //mOp = OTHER;
            mCategory = null;
            mPath = null;
        }
        return true;
    }

    /**
     * This method returns the query category folder paths
     * @param uri the uri for the content to be retrieved
     * @param projection a list of which columns to return; can pass null
     * @param selection a filter declaring which rows to return
     * @param selectionArgs for future use
     * @param sortOrder for future use
     * @return a cursor object contains the results or null
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        //FileManagerLog.d(TAG, "Query operator: " + mOp + "; selection: " + selection);
        if (mCategory == null) {
            return null;
        }
        final MatrixCursor c = new MatrixCursor(new String[] { ID, CATEGORY, PATH });

        /*/ Modified by TYD Biantao, 20120709. Support special media path on external sd. 
        StorageManager storageManager = (StorageManager) getContext().getSystemService(
                Context.STORAGE_SERVICE);
        String defaultStoragePath = storageManager.getDefaultPath();
        FileManagerLog.d(TAG, "StorageManager, DefaultPath: " + defaultStoragePath);

        if (mDefaultCategoryRootPath == null
                || !mDefaultCategoryRootPath.equalsIgnoreCase(defaultStoragePath)) {
            // make sure the all category folder exists in the SDcard;
            // other app might trigger this function before
            checkCategoryPath(defaultStoragePath);
        }
        /*/
        switch (sUriMatcher.match(uri)) {
            case EXTERNAL_SD: {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    return null;
                }
                if (selection == null){
                    for (int i = 0; i < mCategory.length; i++){
                        File target = new File(mExternalPath[i]);
                        if (!target.exists()) {
                            target.mkdir();
                        }
                        c.addRow(new Object[] { Integer.valueOf(i), mCategory[i], mExternalPath[i]} );
                    }
                } else {
                    int index = FileInfoComparator.binarySearch(mCategory, selection);
                    if (index >= 0){
                        File target = new File(mExternalPath[index]);
                        if(!target.exists()){
                            target.mkdir();
                        }
                        c.addRow(new Object[] {Integer.valueOf(index), mCategory[index], mExternalPath[index]});
                    } else {
                        return null;
                    }
                }
                return c;
            }
            default: break;
        }
        //*/

        if (selection == null) {
            // all category data
            for (int i = 0; i < mCategory.length; i++) {
                c.addRow(new Object[] { Integer.valueOf(i), mCategory[i], mPath[i] });
            }
        } else {
            // only selected category data
            int index = FileInfoComparator.binarySearch(mCategory, selection);
            FileManagerLog.d(TAG, "Binary search on category list: " + index);

            if (index >= 0) {
                c.addRow(new Object[] { Integer.valueOf(index), mCategory[index], mPath[index] });
            } else {
                return null;
            }
        }
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
