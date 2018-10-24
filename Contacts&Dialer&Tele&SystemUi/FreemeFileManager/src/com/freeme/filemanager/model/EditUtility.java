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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileInfoComparator;
import com.freeme.filemanager.util.MountPointHelper;
import com.freeme.filemanager.util.OptionsUtil;
import com.freeme.filemanager.util.ScannerClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class EditUtility {
    private static final String TAG = "EditUtility";
    private static final String UNIT_KB = "KB";
    private static final String UNIT_MB = "MB";
    private static final String UNIT_GB = "GB";
    private static final String UNIT_TB = "TB";

    // operation constant for navigation view
    public static final int CREATE_FOLDER = 0;
    public static final int EDIT = 1;
    public static final int SORTY_BY = 2;

    // operation constant for edit view
    public static final int COPY = 0;
    public static final int DELETE = 1;
    public static final int CUT = 2;
    public static final int SHARE = 3;
    public static final int PASTE = 4;
    public static final int RENAME = 5;
    public static final int DETAILS = 6;
    public static final int PROTECTION_INFO = 7;
    public static final int NO_OPERATION = 8;

    public static final String EXT_DRM_CONTENT = "dcf";
    public static final String UNRECOGNIZED_FILE_MIME_TYPE = "application/zip";
    public static final String ACTION_DELETE = "com.mediatek.filemanager.ACTION_DELETE";
    public static final int FILENAME_MAX_LENGTH = 255;
    private static final int IO_BUFFER_LENGTH = 256 * 1024;
    private static final int OPERATION_COMPLETE_PERCENTAGE = 100;
    private static final int COPY_COMPLETE_PERCENTAGE = 90;
    private static final int DELETE_COMPLETE_PERCENTAGE = 10;

    private static int sLastOperation = NO_OPERATION;
    private static boolean sShowProgressDialog = false;

    private static byte[] mIOBuffer = null;

    private static final EditUtility INSTANCE = new EditUtility();

    /**
     * private constructor here, It is a singleton class.
     */
    private EditUtility() {
    }

    /**
     * The EditUtility is a singleton class, this static method can be used to obtain the unique
     * instance of the class.
     *
     * @return The unique instance of EditUtility.
     */
    public static EditUtility getInstance() {
        return INSTANCE;
    }

    /**
     * This method creates a new folder
     *
     * @param name the name of the folder to be created
     * @return true if the folder is created successfully, false otherwise
     */
    public static boolean createFolder(Context context, String toCreateDirPath) {
        boolean result = false;
        String dirPath = toCreateDirPath;

        FileManagerLog.d(TAG, "Create a new folder");
        try {
            dirPath = dirPath.trim();
            File dir = new File(dirPath);

            FileManagerLog.d(TAG, "The folder to be created exist: " + dir.exists());
            if (!dir.exists()) {
                result = dir.mkdirs();
                if (!result) {
                    showToast(context, R.string.msg_create_fail);
                }
            } else {
                showToast(context, R.string.msg_change_name);
            }
        } catch (Exception e) {
            FileManagerLog.e(TAG, "Failed to create a folder", e);
            showToast(context, R.string.msg_create_fail);
        }
        return result;
    }

    /**
     * This method is used to judge whether a fileName is legal. It is designed just to judge the
     * filename length, that can't be zero and can't too long either.
     *
     * @param fileName Filename string to be judged.
     * @return If the length is in the range of 0 ~ DIALOG_FILENAME_MAX_SIZE, return true, otherwise
     * return false.
     */
    public static boolean isValidName(Context context, String fileName) {
        String msg = null;

        if (fileName.trim().length() == 0) {
            showToast(context, R.string.invalid_empty_name);
            return false;
        } else {
            if (fileName.getBytes().length > FILENAME_MAX_LENGTH) {
                // show a toast notification if the folder name is too long
                showToast(context, R.string.invalid_file_name);
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * This method copies a list of files to the clipboard.
     *
     * @param context   the context of FileManagerOperationActivity
     * @param copyFiles a list of files to be copied
     */
    public static void copy(Context context, final List<FileInfo> copyFileInfos) {
        Clipboard.setContents(null);
        ArrayList<File> files = new ArrayList<File>();
        for (FileInfo fileInfo : copyFileInfos) {
            files.add(fileInfo.getFile());
        }
        Clipboard.setContents(files);
    }

    public static boolean isDrmFile(File file) {
        if (OptionsUtil.isDrmSupported()) {
            String extension = getFileExtension(file.getName());
            if (extension != null && extension.equalsIgnoreCase(EXT_DRM_CONTENT)) {
                return true; // all drm files cannot be copied
            }
        }
        return false;
    }

    /**
     * This method generates a new suffix if a name conflict occurs, ex: paste a file named
     * "stars.txt", the target file name would be "stars(1).txt"
     *
     * @param conflictFile the conflict file
     * @return a new name for the conflict file
     */
    public static String autoGenerateName(File conflictFile) {
        int prevMax = 0;
        int newMax = 0;
        int leftBracketIndex = 0;
        int rightBracketIndex = 0;
        String tmp = null;
        String numeric = null;
        String fileName = null;
        File dir = null;
        File[] files = null;
        String parentDir = conflictFile.getParent();
        String conflictName = conflictFile.getName();

        if (parentDir != null) {
            dir = new File(parentDir);
            files = dir.listFiles();
        }

        if (conflictFile.isDirectory()) {
            // check if source folder already contains "(x)", e.g. /sdcard/starsDir(3)
            if (conflictName.endsWith(")")) {
                leftBracketIndex = conflictName.lastIndexOf("(");
                if (leftBracketIndex != -1) {
                    numeric = conflictName.substring(leftBracketIndex + 1,
                            conflictName.length() - 1);
                    if (numeric.matches("[0-9]+")) {
                        FileManagerLog.d(TAG, "Conflict folder name already contains (): "
                                + conflictName + "thread id: " + Thread.currentThread().getId());
                        newMax = findSuffixNumber(conflictName, prevMax);
                        prevMax = newMax;
                        conflictName = conflictName.substring(0, leftBracketIndex);
                    }
                }
            }

            if (files != null) {
                for (File file : files) {
                    fileName = file.getName();
                    if (fileName.endsWith(")")) {
                        leftBracketIndex = fileName.lastIndexOf("(");
                        if (leftBracketIndex != -1) {
                            tmp = fileName.substring(0, leftBracketIndex);
                            if (tmp.equalsIgnoreCase(conflictName)) {
                                numeric = fileName.substring(leftBracketIndex + 1, fileName
                                        .length() - 1);
                                if (numeric.matches("[0-9]+")) {
                                    FileManagerLog.d(TAG, "File name contains () match: "
                                            + fileName + "thread id: "
                                            + Thread.currentThread().getId());
                                    newMax = findSuffixNumber(fileName, prevMax);
                                    prevMax = newMax;
                                }
                            }
                        }
                    }
                }
            }
            return parentDir + "/" + conflictName + "(" + Integer.toString(newMax + 1) + ")";
        } else {
            // check if source file already contains "(x)", e.g. /sdcard/stars(3).jpg
            String ext = "";
            int extIndex = conflictName.lastIndexOf(".");
            if (extIndex == -1) {
                extIndex = conflictName.length(); // this file has no extension
            } else {
                ext = conflictName.substring(extIndex);
            }

            String prefix = conflictName.substring(0, extIndex);
            if (prefix.endsWith(")")) {
                leftBracketIndex = prefix.lastIndexOf("(");
                if (leftBracketIndex != -1) {
                    numeric = prefix.substring(leftBracketIndex + 1, prefix.length() - 1);
                    if (numeric.matches("[0-9]+")) {
                        FileManagerLog.d(TAG, "Conflict file name already contains (): "
                                + conflictName + "thread id: " + Thread.currentThread().getId());
                        newMax = findSuffixNumber(conflictName, prevMax);
                        prevMax = newMax;
                        prefix = prefix.substring(0, leftBracketIndex);
                    }
                }
            }

            if (files != null) {
                for (File file : files) {
                    fileName = file.getName();
                    if (fileName.endsWith(")" + ext)) {
                        leftBracketIndex = fileName.lastIndexOf("(");
                        rightBracketIndex = fileName.lastIndexOf(")");
                        if (leftBracketIndex != -1) {
                            tmp = fileName.substring(0, leftBracketIndex);
                            if (tmp.equalsIgnoreCase(prefix)) {
                                numeric = fileName.substring(leftBracketIndex + 1,
                                        rightBracketIndex);
                                if (numeric.matches("[0-9]+")) {
                                    FileManagerLog.d(TAG, "file name contains () match: "
                                            + fileName + "thread id: "
                                            + Thread.currentThread().getId());
                                    newMax = findSuffixNumber(fileName, prevMax);
                                    prevMax = newMax;
                                }
                            }
                        }
                    }
                }
            }
            return parentDir + "/" + prefix + "(" + Integer.toString(newMax + 1) + ")" + ext;
        }
    }

    /**
     * This method finds the current max number of suffix for a conflict file ex: there are
     * A(1).txt, A(2).txt, then the max number of suffix is 2
     *
     * @param fileName the conflict file
     * @param maxVal   the old max number of suffix
     * @return the new max number of suffix
     */
    private static int findSuffixNumber(String fileName, int maxVal) {
        int val = 0;
        int leftBracket = fileName.lastIndexOf("(");
        int rightBracket = fileName.lastIndexOf(")");

        String s = fileName.substring(leftBracket + 1, rightBracket);

        try {
            val = Integer.parseInt(s);
            if (val > maxVal) {
                return val;
            }
        } catch (NumberFormatException e) {
            FileManagerLog.e(TAG, "Fn-findSuffixNumber(): " + e.toString());
        }
        return maxVal;
    }

    /**
     * This method gets the total size (bytes) of a file/folder (recursively to the deepest level)
     *
     * @param root the file/folder
     * @return the size
     */
    public static long getContentSize(File root) {
        // Note that this function is written in iteration instead of recursion.
        // This is because recursion may cause stack overflow if folders are very deep.
        long size = root.length();

        if (root.isDirectory()) {
            ArrayList<File> folderArrayList = new ArrayList<File>();
            folderArrayList.add(root);
            while (!folderArrayList.isEmpty()) {
                File folder = folderArrayList.get(0);
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            folderArrayList.add(file);
                        }
                        size += file.length();
                    }
                }
                folderArrayList.remove(0);
            }
            folderArrayList = null; // release reference
        }

        return size;
    }

    /**
     * This method gets total free space in the sdcard
     *
     * @param path the path of target directory that to be got free space
     * @return total free space
     */
    public static long getFreeSpace(String path) {
        long freeSpace = 0;
        try {
            // if lost sdcard, it may caused IllegalArgumentException to create a StatFs Object.
            StatFs stat = new StatFs(path);
            freeSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
        }
        return freeSpace;
    }

    /**
     * This method performs the cut operation, i.e. save a list of files to the clipboard. filter
     * the file that can not be cut
     *
     * @param context     the context of FileManagerOperationActivity
     * @param cutFiles    a list of files to be cut
     * @param grayOutItem the gray out item list should be updated when the cut files list is put in
     *                    clipboard
     */
    public static void cut(Context context, List<FileInfo> cutFileInfos) {
        Clipboard.setContents(null);
        ArrayList<File> cutFiles = new ArrayList<File>();
        for (FileInfo fileInfo : cutFileInfos) {
            File file = fileInfo.getFile();
            fileInfo.setCut(true);
            cutFiles.add(file);
        }
        Clipboard.setContents(cutFiles);
    }

    public static int getDeviceNumber(Context context, String filePath) {
        StorageManager storageManager = (StorageManager) context
                .getSystemService(Context.STORAGE_SERVICE);
        String[] storagePathList = storageManager.getVolumePaths();
        if (null != storagePathList) {
            for (int i = 0; i < storagePathList.length; i++) {
                if ((filePath + "/").startsWith(storagePathList[i] + "/")
                        || (filePath + "/").equals(storagePathList[i] + "/")) {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * This method notifies the updates of sdcard content
     *
     * @param context the context of FileManagerOperationActivity
     * @param action  broadcast action
     * @param file    the changed file to be notified about
     */
    public static void notifyUpdates(Context context, String action, File file) {
        FileManagerLog.d(TAG, "Broadcasting action: " + action);

        if ("com.mediatek.filemanager.ACTION_DELETE".equals(action)) {
            // specific solution for music application
            Uri data = Uri.fromFile(file);
            context.sendBroadcast(new Intent(action, data));
        } else {
            if (file != null) {
                final String mountPath = MountPointHelper.getInstance().getRealMountPointPath(
                        file.getPath());
                if (mountPath != null) {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
                            .parse("file://" + mountPath)));
                }
            }
        }
    }

    /**
     * This method converts a size to a string
     *
     * @param size the size of a file
     * @return the string represents the size
     */
    public static String sizeToString(long size) {
        String unit = UNIT_KB;
        double sizeDouble = (double) size / (double) 1024;
        if (sizeDouble > 1024) {
            sizeDouble = sizeDouble / (double) 1024;
            unit = UNIT_MB;
        }
        if (sizeDouble > 1024) {
            sizeDouble = sizeDouble / (double) 1024;
            unit = UNIT_GB;
        }
        if (sizeDouble > 1024) {
            sizeDouble = sizeDouble / (double) 1024;
            unit = UNIT_TB;
        }

        // Add 0.005 for rounding-off.
        long sizeInt = (long) ((sizeDouble + 0.005) * 100.0); // strict to two
        // decimal places
        double formatedSize = ((double) sizeInt) / 100.0;

        if (formatedSize == 0) {
            return "0" + " " + unit;
        } else {
            return Double.toString(formatedSize) + " " + unit;
        }
    }

    /**
     * This method gets the permission information of a file/folder
     *
     * @param res  used to get resources from resources file
     * @param file the file
     * @return the permission of the file (in form: drw)
     */
    public static String getPermission(Resources res, File file) {
        String permission = "";

        permission = permission.concat(res.getString(R.string.readable) + ": ");
        if (file.canRead()) {
            permission = permission.concat(res.getString(R.string.yes));
        } else {
            permission = permission.concat(res.getString(R.string.no));
        }

        permission = permission.concat("\n" + res.getString(R.string.writable) + ": ");
        if (file.canWrite()) {
            permission = permission.concat(res.getString(R.string.yes));
        } else {
            permission = permission.concat(res.getString(R.string.no));
        }

        permission = permission.concat("\n" + res.getString(R.string.executable) + ": ");
        if (file.canExecute()) {
            permission = permission.concat(res.getString(R.string.yes));
        } else {
            permission = permission.concat(res.getString(R.string.no));
        }
        return permission;
    }

    /**
     * This method gets the extension of a file
     *
     * @param fileName the name of the file
     * @return the extension of the file
     */
    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        String extension = null;

        if ((lastDot > 0) && (lastDot < fileName.length() - 1)) {
            extension = fileName.substring(lastDot + 1).toLowerCase();
        }
        return extension;
    }

    /**
     * This method gets the mime type based on the extension of a file
     *
     * @param file the target file
     * @return the mime type of the file/folder
     */
    public static String getMimeTypeForFile(Context context, File file) {
        FileManagerLog.d(TAG, "getMimeTypeForFile");
        if (file == null) {
            return null;
        }
        String mimeType;
        String fileName = file.getName();
        String extension = getFileExtension(fileName);

        if (extension == null) {
            return "unknown_ext_null_mimeType";
        }

        if (OptionsUtil.isDrmSupported() && extension.equalsIgnoreCase(EXT_DRM_CONTENT)) {
            return "application/vnd.oma.drm.content";
        }

        //mimeType = MediaFile.getMimeTypeBySuffix(fileName);
        mimeType = null;
        if (mimeType == null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (mimeType == null) {
            return "unknown_ext_mimeType";
        }

        // special solution for checking 3gpp original mimetype
        // 3gpp extension could be video/3gpp or audio/3gpp
        if (mimeType.equalsIgnoreCase("video/3gpp") || mimeType.equalsIgnoreCase("video/3gpp2")) {
            FileManagerLog.d(TAG, "getMimeTypeForFile, a 3gpp or 3g2 file");
            ContentResolver resolver;
            if (context == null) {
                resolver = null;
            } else {
                resolver = context.getContentResolver();
            }
            return get3gppOriginalMimetype(resolver, file);
        }
        return mimeType;
    }

    /**
     * The file whose extension is 3gpp may be a audio file or a video file, so its mimeType should
     * get by querying.
     *
     * @param file the file for querying
     * @return the mimeType of the 3gpp file
     */
    private static String get3gppOriginalMimetype(ContentResolver resolver, File file) {
        FileManagerLog.d(TAG, "get3gppOriginalMimetype");
        String mimeType = "video/3gpp";

        if (resolver == null) {
            return "video/3gpp";
        }

        while (!ScannerClient.getInstance().waitForScanningCompleted()) ;

        // since 3gpp could be video or audio type,
        // we need to check audio and video content provider to find out its
        // real mimetype
        Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns.MIME_TYPE}, MediaStore.MediaColumns.DATA
                        + "=?", new String[]{file.getPath()}, null);

        try {
            if (cursor == null) {
                FileManagerLog.d(TAG, "get3gppOriginalMimetype " + "cursor is null");
            }
            if (cursor != null && cursor.moveToFirst()) {
                FileManagerLog.d(TAG, "get3gppOriginalMimetype " + "cursor is not null");
                mimeType = getCursorString(cursor, "mime_type");

                if (mimeType != null) {
                    FileManagerLog.d(TAG, "Found " + file.getPath()
                            + " in: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mimetype: "
                            + mimeType);
                    return mimeType;
                } else {
                    FileManagerLog.d(TAG, "get3gppOriginalMimetype " + "get mime null from media");
                    return "video/3gpp";
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (mimeType == null) {
                return "unknown_3pgg_mimeType";
            } else {
                return mimeType;
            }
        }
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        if (cursor == null)
            return null;
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getString(index) : null;
    }
    public static int getIconIdForDirectory(Context context, FileInfo fileInfo) {
        if (!fileInfo.getFile().isDirectory()) {
            FileManagerLog.e(TAG, "getIconIdForDirectory, Illegal argument: not dir");
            return 0;
        }

        String fileName = fileInfo.getFile().getName();
        return R.drawable.folder;

    }

    /**
     * This method gets the drawable id based on the mimetype
     *
     * @param mimeType the mimeType of a file/folder
     * @return the drawable icon id based on the mimetype
     */
    public static int getDrawableId(String mimeType) {
        if (mimeType.startsWith("application/vnd.android.package-archive")) {
            return R.drawable.fm_apk;
        } else if (mimeType.startsWith("application/zip")) {
            return R.drawable.fm_zip;
        } else if (mimeType.startsWith("application/ogg")) {
            return R.drawable.fm_audio;
        } else if (mimeType.startsWith("audio/")) {
            return R.drawable.fm_audio;
        } else if (mimeType.startsWith("image/")) {
            return R.drawable.fm_picture;
        } else if (mimeType.startsWith("text/")) {
            return R.drawable.fm_doc;
            //*/added by tyd carl,20120704,[tyd00429785],icons display
        } else if (mimeType.startsWith("application/msword")) {
            return R.drawable.fm_doc;
        } else if (mimeType.startsWith("application/mspowerpoint")) {
            return R.drawable.fm_doc;
        } else if (mimeType.startsWith("application/vnd.ms-excel")) {
            return R.drawable.fm_doc;
            //*/
        } else if (mimeType.startsWith("video/")) {
            return R.drawable.fm_video;
        } else {
            return R.drawable.fm_unknown;
        }
    }

    /**
     * This method gets the mime type from multiple files (order to return: image->video->other)
     *
     * @param drmManagerClient for get some services from DrmManagerClient
     * @param currentDirPath   the current directory
     * @param files            a list of files
     * @return the mime type of the multiple files
     */

    public static String getShareMultipleMimeType(Context context,
                                                  DrmManagerClient drmManagerClient, String currentDirPath, List<String> files) {
        String mimeType = null;
        String path = null;

        for (String s : files) {
            mimeType = getMimeTypeForFile(context, new File(currentDirPath + "/" + s));
            FileManagerLog.d(TAG, "Get multiple files mimetype: " + mimeType);

            if (OptionsUtil.isDrmSupported()) {
                if (mimeType.equalsIgnoreCase("application/vnd.oma.drm.content")) {
                    path = currentDirPath + "/" + s;
                    mimeType = drmManagerClient.getOriginalMimeType(path);
                }
            }
            if (null != mimeType) {
                if (mimeType.startsWith("image/")) {
                    break;
                } else if (mimeType.startsWith("video/")) {
                    break;
                }
            }
        }

        if (mimeType == null || mimeType.startsWith("unknown")) {
            mimeType = UNRECOGNIZED_FILE_MIME_TYPE;
        }
        FileManagerLog.d(TAG, "Multiple files' mimetype is " + mimeType);
        return mimeType;
    }

    /**
     * This method gets the category icon id for a category folder
     *
     * @param fileName the name of the category folder
     * @return the category icon id
     */
    private static int getCategoryFolderIcon(String fileName) {
        FileManagerLog.d(TAG, "getcategoryFolderIcon, fileName: " + fileName);
        if (fileName.equalsIgnoreCase("Document")) {
            return R.drawable.fm_document_folder;
        } else if (fileName.equalsIgnoreCase("Download")) {
            return R.drawable.fm_download_folder;
        } else if (fileName.equalsIgnoreCase("Music")) {
            return R.drawable.fm_music_folder;
        } else if (fileName.equalsIgnoreCase("Photo")) {
            return R.drawable.fm_photo_folder;
        } else if (fileName.equalsIgnoreCase("Received File")) {
            return R.drawable.fm_received_folder;
        } else if (fileName.equalsIgnoreCase("Video")) {
            return R.drawable.fm_video_folder;
        } else {
            return R.drawable.fm_folder;
        }
    }

    /**
     * This method sets the last operation
     *
     * @param op the last operation performed
     */
    public static void setLastOperation(int op) {
        FileManagerLog.d(TAG, "set last operation: " + op);
        sLastOperation = op;
    }

    /**
     * This method gets the last operation
     *
     * @return the last operation
     */
    public static int getLastOperation() {
        return sLastOperation;
    }

    public static Bitmap createSDCardIcon(Resources res, Bitmap defIcon) {
        Bitmap temp = BitmapFactory.decodeResource(res, R.drawable.fm_file_location_icon);
        int offx = temp.getWidth() / 4;
        int width = offx + defIcon.getWidth();
        int height = defIcon.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        c.drawBitmap(defIcon, offx, 0, null);
        c.drawBitmap(temp, 0, 0, null);
        c.save(Canvas.ALL_SAVE_FLAG);
        c.restore();
        return bitmap;
    }

    private static Toast sToast = null;

    public static void showToast(Context context, String msg) {
        if (sToast == null) {
            sToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        } else {
            sToast.setText(msg);
        }
        sToast.show();
    }

    public static void showToast(Context context, int msgId) {
        if (sToast == null) {
            sToast = Toast.makeText(context, msgId, Toast.LENGTH_SHORT);
        } else {
            sToast.setText(msgId);
        }
        sToast.show();
    }
}
