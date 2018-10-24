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

import android.app.ActionBar;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.MediaScanner;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import com.freeme.filemanager.R;
import com.freeme.filemanager.custom.FeatureOption;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.view.FavoriteItem;
import com.freeme.filemanager.view.Settings;
import com.freeme.utils.ReflectUtils;
import com.freeme.utils.ReflectUtils.ReflAgent;

import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

public class Util {
    private static String ANDROID_SECURE = "/mnt/sdcard/.android_secure";

    public static String USBOTG_DIR = "/mnt/usbotg";

    public static long SYSTEM_RESERVED_STORAGE_SIZE = 30 * 1024 * 1024;//30MB

    private static final String LOG_TAG = "Util";
    public static final int BUFFER_SIZE = 1024 * 1024;
    public static final int MIN_CLICK_DELAY_TIME = 1000;//time delay for click respond
    public static final int GARBAGE_ITEMS = 5;
    public static final int FREEME_DIALOG_OPTION_BOTTOM = 1;

    //FastCategoryDetailsFragment, FastCategoryPathsDetailsFragment
    public static final int PAGE_DEFAULT      = -1;
    public static final int PAGE_DETAILS      = 0;
    public static final int PAGE_EXPLORER     = 1; //FileExplorerViewFragment
    public static final int PAGE_STORAGE      = 2; //StorageFileListActivity
    public static final int PAGE_PICFOLDER    = 3; //FastCategoryPictureDetailsFragment
    public static final int PAGE_ENCRYPT      = 4; //EncryptionFileBrowserActivity

    private static DateFormat  date_format = DateFormat.getDateInstance();

    private static DateFormat  time_format = DateFormat.getTimeInstance();

    public static String MEMORY_DIR = Environment.getExternalStorageDirectory().getPath();

    public static String SD_DIR = "/storage/sdcard1";

    private static File sdFile= new File(SD_DIR);

    public static int USBOTG_DEFAULT_SIZE = 742903808;
    public static String mFileExplorerTabActivityTilte;

    private static Context mContext;

    private static StorageVolume storageVolume1;
    private static String PATH = "/storage/emulated/0";
    private static String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static ArrayList<StorageVolume> mountVolumeList;
    private static StorageVolume[] storageVolumes;
    private static StorageVolume storageVolume = null;
    public static boolean isSDCardReady() {
            return sdFile.equals(Environment.MEDIA_MOUNTED);
    }

    private static ReflAgent reflAgent;

    // if path1 contains path2
    public static boolean containsPath(String path1, String path2) {
        String path = path2;
        while (path != null) {
            if (path.equalsIgnoreCase(path1))
                return true;

            if (path.equals(GlobalConsts.ROOT_PATH))
                break;
            path = new File(path).getParent();
        }

        return false;
    }

    public static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator))
            return path1 + path2;

        return path1 + File.separator + path2;
    }

    public static void setBackTitle(ActionBar actionBar, CharSequence title) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // Android O1
            try {
                reflAgent = ReflectUtils.ReflAgent.getClass("com.freeme.actionbar.app.FreemeActionBarUtil");
                reflAgent.callStatic("setBackTitle",
                        new Class[]{ActionBar.class, CharSequence.class},//Integer.TYPE
                        new Object[]{actionBar, title});
            } catch (Exception e) {
                Log.v(LOG_TAG, e.toString());
            }
        }
    }

    public static void setFreemeDialogOption(Object target, int dialogtype) {
        try {
            ReflectUtils.callObjectMethod(target, "setFreemeDialogOption", new Class[]{Integer.TYPE}, new Object[]{dialogtype});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static boolean isOneStepMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // Android O1
            Object target;
            Object result = null;
            try {
                reflAgent = ReflectUtils.ReflAgent.getClass("com.freeme.internal.app.FreemeOneStepManager");
                target = reflAgent.callStatic("from",
                        new Class[]{Context.class},
                        new Object[]{context}).objectResult();
                result = ReflectUtils.callObjectMethod(target, "isOneStepMode",
                        new Class[]{},
                        new Object[]{});
            } catch (Exception e) {
                Log.v(LOG_TAG, e.toString());
                return false;
            }
            return result != null && (boolean) result;
        }
        return false;
    }

    public static void dragFile(View view, Context context, File file, String mimeType, int iconId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // Android O1
            try {
                reflAgent = ReflectUtils.ReflAgent.getClass("com.freeme.util.FreemeOneStepDragUtils");
                reflAgent.callStatic("dragFile",
                        new Class[]{View.class, Context.class, File.class, String.class, Integer.TYPE},
                        new Object[]{view, context, file, mimeType, iconId});
            } catch (Exception e) {
                Log.v(LOG_TAG, e.toString());
            }
        }
    }

    public static void dragMultipleFile(View view, Context context,
                                        File[] files, String[] mimeTypes, Bundle extras, int iconId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // Android O1
            try {
                reflAgent = ReflectUtils.ReflAgent.getClass("com.freeme.util.FreemeOneStepDragUtils");
                reflAgent.callStatic("dragMultipleFile",
                        new Class[]{View.class, Context.class, File[].class, String[].class, Bundle.class, Integer.TYPE},
                        new Object[]{view, context, files, mimeTypes, extras, iconId});
            } catch (Exception e) {
                Log.v(LOG_TAG, e.toString());
            }
        }
    }

    public static void dragImage(View view, Context context, Bitmap content, File file, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // Android O1
            try {
                reflAgent = ReflectUtils.ReflAgent.getClass("com.freeme.util.FreemeOneStepDragUtils");
                reflAgent.callStatic("dragImage",
                        new Class[]{View.class, Context.class, Bitmap.class, File.class, String.class},
                        new Object[]{view, context, content, file, mimeType});
            } catch (Exception e) {
                Log.v(LOG_TAG, e.toString());
            }
        }
    }

    public static void dragMultipleImage(View view, Context context,
                                         int index, File[] files, String[] mimeTypes, int showAnimDelay) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // Android O1
            try {
                reflAgent = ReflectUtils.ReflAgent.getClass("com.freeme.util.FreemeOneStepDragUtils");
                reflAgent.callStatic("dragMultipleImage",
                        new Class[]{View.class, Context.class, Integer.TYPE, File[].class, String[].class, Integer.TYPE},
                        new Object[]{view, context, index, files, mimeTypes, showAnimDelay});
            } catch (Exception e) {
                Log.v(LOG_TAG, e.toString());
            }
        }
    }

    public static boolean isNormalFile(String fullName) {
        return !fullName.equals(ANDROID_SECURE);
    }

    public static FileInfo GetFileInfo(String filePath) {
        File lFile = new File(filePath);
        if (!lFile.exists())
            return null;

        FileInfo lFileInfo = new FileInfo();
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = Util.getNameFromFilepath(filePath);
        lFileInfo.ModifiedDate = lFile.lastModified();
        String str2 = date_format.format(lFileInfo.ModifiedDate);
        String str3 = time_format.format(lFileInfo.ModifiedDate);
        lFileInfo.fileFormatDateTime = str2 + " " + str3;
        lFileInfo.IsDir = lFile.isDirectory();
        if (!lFile.isDirectory()) {
            lFileInfo.mFileMimeType = Util.getFileMimeType(filePath);
        } else {
            lFileInfo.mFileMimeType = null;
        }
        lFileInfo.filePath = filePath;
        lFileInfo.fileSize = lFile.length();
        return lFileInfo;
    }

    public static FileInfo GetFileInfo(File f, FilenameFilter filter, boolean showHidden) {
        FileInfo lFileInfo = new FileInfo();
        String filePath = f.getPath();
        File lFile = new File(filePath);
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = f.getName();
        lFileInfo.ModifiedDate = lFile.lastModified();
        String str2 = date_format.format(lFileInfo.ModifiedDate);
        String str3 = time_format.format(lFileInfo.ModifiedDate);
        lFileInfo.fileFormatDateTime = str2 + " " + str3;
        lFileInfo.IsDir = lFile.isDirectory();
        if (!lFile.isDirectory()) {
            lFileInfo.mFileMimeType = Util.getFileMimeType(filePath);
        } else {
            lFileInfo.mFileMimeType = null;
        }
        lFileInfo.filePath = filePath;
        if (lFileInfo.IsDir) {
            int lCount = 0;
            File[] files = lFile.listFiles(filter);

            // null means we cannot access this dir
            if (files == null) {
                return null;
            }

            for (File child : files) {
                if ((!child.isHidden() || showHidden)
                        && Util.isNormalFile(child.getAbsolutePath())) {
                    lCount++;
                }
            }
            lFileInfo.Count = lCount;

        } else {

            lFileInfo.fileSize = lFile.length();

        }
        return lFileInfo;
    }

    public static Drawable getApkIcon(Context context, String apkPath) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch (OutOfMemoryError e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
        return null;
    }

    //adapter storage path
    public static void setMountedStorageBySubPath(Context paramContext, StorageManager mStorageManager) {
        storageVolumes = mStorageManager.getVolumeList();
        mountVolumeList = new ArrayList<StorageVolume>();
        int i = storageVolumes.length;
        for (int j = 0; j < i; ++j) {
            storageVolume1 = storageVolumes[j];
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (storageVolume1.getStorageId() != 0 && !storageVolume1.getState().equals(Environment.MEDIA_REMOVED)) {
                    mountVolumeList.add(storageVolume1);
                }
            }else {
                if (storageVolume1.getStorageId() != 0) {
                    mountVolumeList.add(storageVolume1);
                }
            }
        }

        for (StorageVolume sv : storageVolumes) {
            if (sv.isRemovable() && isUSBOTG(sv.getId())) {
                USBOTG_DIR = sv.getPath();
            } else if (sv.isRemovable()) {
                SD_DIR = sv.getPath();
            }
        }
    }

    public static boolean isUSBOTG(String diskID) {
        if (diskID != null) {
            // for usb otg, the disk id same as disk:8:x
            String[] idSplit = diskID.split(":");
            if (idSplit != null && idSplit.length == 2) {
                if (idSplit[1].startsWith("8,")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static StorageVolume getMountedStorageBySubPath(Context paramContext, String volumPath) {
        if (!TextUtils.isEmpty(volumPath)) {
            Iterator localIterator = StorageHelper.getInstance(paramContext).getMountedVolumeList().iterator();
            StorageVolume storageVolume;
            while (localIterator.hasNext()) {
                storageVolume = (StorageVolume) localIterator.next();
                if (volumPath.startsWith(storageVolume.getPath()))
                    return storageVolume;
            }
        }
        return null;
    }

    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    public static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        }
        return "";
    }

    public static String getPathFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(0, pos);
        }
        return "";
    }

    public static String getNameFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(pos + 1);
        }
        return "";
    }

    public static String getFileMimeType(String filePath) {
        String mimeType;
        int dotPosition = filePath.lastIndexOf('.');
        if (dotPosition == -1)
            return "*/*";

        String ext = filePath.substring(dotPosition + 1, filePath.length()).toLowerCase();
        if (MimeUtils.is3GPPExtension(ext)) {
            mimeType = EditUtility.getMimeTypeForFile(mContext, new File(filePath));
        } else {
            mimeType = MimeUtils.guessMimeTypeFromExtension(ext);
        }
        return mimeType != null ? mimeType : "*/*";
    }

    // return new file path if successful, or return null
    public static String copyFile(String src, String dest) {
        File file = new File(src);
        if (!file.exists() || file.isDirectory()) {
            Log.v(LOG_TAG, "copyFile: file not exist or is directory, " + src);
            return null;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(file);
            File destPlace = new File(dest);
            if (!destPlace.exists()) {
                if (!destPlace.mkdirs())
                    return null;
            }

            String destPath = Util.makePath(dest, file.getName());
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                String destName = Util.getNameFromFilename(file.getName()) + " " + i++ + "."
                        + Util.getExtFromFilename(file.getName());
                destPath = Util.makePath(dest, destName);
                destFile = new File(destPath);
            }

            if (!destFile.createNewFile())
                return null;

            fo = new FileOutputStream(destFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = fi.read(buffer, 0, BUFFER_SIZE)) != -1) {
                fo.write(buffer, 0, read);
                fo.flush();
            }
            return destPath;
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "copyFile: file not found, " + src);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(LOG_TAG, "copyFile: " + e.toString());
        } finally {
            try {
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    // does not include sd card folder
    private static String[] SysFileDirs = new String[] {
        "miren_browser/imagecaches"
    };

    public static boolean shouldShowFile(String path) {
        return shouldShowFile(new File(path));
    }

    public static boolean shouldShowFile(File file) {
        boolean show = Settings.instance().getShowDotAndHiddenFiles();
        if (show)
            return true;

        if (file.isHidden())
            return false;

        if (file.getName().startsWith("."))
            return false;

        String sdFolder = getSdDirectory();
        if(sdFolder == null){
            return false;
        }
        for (String s : SysFileDirs) {
            if (file.getPath().startsWith(makePath(sdFolder, s)))
                return false;
        }

        return true;
    }

    public static ArrayList<FavoriteItem> getDefaultFavorites(Context context) {
        ArrayList<FavoriteItem> list = new ArrayList<FavoriteItem>();
        return list;
    }

    public static boolean setText(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    public static boolean setText(View view, int id, int text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    // comma separated number
    public static String convertNumber(long number) {
        return String.format("%,d", number);
    }

    // storage, G M K B
    public static String convertStorage(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }

    public static String getMemoryDirectory() {
        if(isSdcardExist()){
            return Environment.getExternalStorageDirectory().getPath();
        }else {
            return SD_DIR;
        }
    }

    public static String getSdDirectory() {
        if(isSdcardExist()){
            return SD_DIR;
        }else {
            return "/mnt";
        }
    }

    public static boolean isSdcardExist() {
        if(sdFile.equals(android.os.Environment.MEDIA_MOUNTED)){
            return true;
        }else {
            return false;
        }
    }

    public static class SDCardInfo {
        public long total;
        public long free;
    }

    public static SDCardInfo getSDCardInfo() {
        if (!TextUtils.isEmpty(SD_DIR)) {
            sdFile= new File(SD_DIR);

            File pathFile = sdFile;
            try {
                SDCardInfo info = new SDCardInfo();
                info.total = pathFile.getTotalSpace();
                info.free = pathFile.getFreeSpace();
                if (info.total == 0 && info.free == 0) {
                    return null;
                }

                return info;
            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
        return null;
    }

    public static class MemoryCardInfo {
        public long total;
        public long free;
    }

    public static MemoryCardInfo getMemoryCardInfo() {
        String path = getDefaultPath();
        try {
            MemoryCardInfo info = new MemoryCardInfo();
            File pathInfo = new File(path);
            info.total = getFormatTotalSpace(pathInfo.getTotalSpace());
            info.free = pathInfo.getFreeSpace();
            return info;
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.toString());
        }

        return null;
    }

    // freeme.chenming, 20170608. Format total space
    public static long getFormatTotalSpace(long totalSpace) {
        final long TOTAL_SPACE_8G = 8L << 30;
        final long TOTAL_SPACE_16G = 2 * TOTAL_SPACE_8G;
        final long TOTAL_SPACE_32G = 2 * TOTAL_SPACE_16G;
        final long TOTAL_SPACE_64G = 2 * TOTAL_SPACE_32G;
        final long TOTAL_SPACE_128G = 2 * TOTAL_SPACE_64G;

        if (totalSpace <= TOTAL_SPACE_8G) {
            return TOTAL_SPACE_8G;
        } else if (totalSpace <=  TOTAL_SPACE_16G) {
            return TOTAL_SPACE_16G;
        } else if (totalSpace <= TOTAL_SPACE_32G) {
            return TOTAL_SPACE_32G;
        } else if (totalSpace <= TOTAL_SPACE_64G) {
            return TOTAL_SPACE_64G;
        } else if (totalSpace <= TOTAL_SPACE_128G) {
            return TOTAL_SPACE_128G;
        }
        return totalSpace;
    }

    public static String getDefaultPath() {
        if (PATH.equals(defaultPath)) {
            return defaultPath;
        } else if (mountVolumeList.size() == 1) {
            return mountVolumeList.get(0).getPath();
        } else if (mountVolumeList.size() >= 2) {
            for (StorageVolume sv : mountVolumeList) {
                if (sv.isPrimary() && !sv.isRemovable()) {
                    return sv.getPath();
                }
            }
        }
        return defaultPath;
    }

    public static String getDefaultState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.L) {
            return Environment.getExternalStorageState(new File(getDefaultPath()));
        } else {
            return Environment.getStorageState(new File(getDefaultPath()));
        }
    }

    public static class UsbStorageInfo {
        public long total;
        public long free;
    }

    public static UsbStorageInfo getUsbStorageInfo() {
        if (!TextUtils.isEmpty(USBOTG_DIR)) {
            File pathFile = new File(USBOTG_DIR);
            if (!pathFile.exists()) {
                return null;
            }
            UsbStorageInfo info = new UsbStorageInfo();
            info.total = pathFile.getTotalSpace();
            info.free = pathFile.getFreeSpace();
            if (info.total == 0 && info.free == 0) {
                return null;
            }

            return info;
        }

        return null;
    }

    private static DateFormat dateFormat = null;

    private static DateFormat timeFormat = null;

    private static void getDateFormat(Context context){
        if(dateFormat == null){
            dateFormat = android.text.format.DateFormat.getDateFormat(context);
        }else {
            return;
        }
    }

    private static void getTimeFormat(Context context){
        if(timeFormat == null){
            timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        }else {
            return;
        }
    }

    public static String formatDateString(Context context, long time) {
        getDateFormat(context);
        getTimeFormat(context);
        Date date = new Date(time);
        return dateFormat.format(date) + " " + timeFormat.format(date);
    }

    public static void updateActionModeTitle(ActionMode mode, Context context, int selectedNum) {
        if (mode != null) {
            if (selectedNum == 0 || selectedNum == 1) {
                mode.setTitle((context.getString(R.string.multi_select_title, selectedNum)).toString().replace("s", ""));
            } else {
                mode.setTitle(context.getString(R.string.multi_select_title, selectedNum));
            }
        }
    }

    public static HashSet<String> sDocMimeTypesSet = new HashSet<String>() {
        {
            add("text/plain");
            add("text/html");
            add("application/pdf");
            add("application/msword");
            add("application/vnd.ms-excel");
            add("application/vnd.ms-powerpoint");
            add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            add("application/vnd.openxmlformats-officedocument.wordprocessingml.template");
            add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        }
    };

    public static HashSet<String> sZipFileMimeTypeSet = new HashSet<String>() {
        {
            add("application/zip");
        }
    };

    public static HashSet<String> sApkFileMimeTypeSet = new HashSet<String>() {
        {
            add("application/vnd.android.package-archive");
        }
    };

    public static int CATEGORY_TAB_INDEX = 0;
    public static int SDCARD_TAB_INDEX = 1;

    public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static boolean isInSameVolume(String path1, String path2) {
        Log.i(LOG_TAG, "path1: "+path1+", path2: "+path2);
        if ((TextUtils.isEmpty(path1))|| (TextUtils.isEmpty(path2))){
            return false;
        }

        if(FeatureOption.MTK_MULTI_STORAGE_SUPPORT){
            if(path1.startsWith(MEMORY_DIR)){
                if(path2.startsWith(MEMORY_DIR)){
                    return true;
                }
            }else if(path1.startsWith(SD_DIR)){
                if(path2.startsWith(SD_DIR)){
                    return true;
                }
            }else if(path1.startsWith(USBOTG_DIR)){
                if(path2.startsWith(USBOTG_DIR)){
                    return true;
                }
            }
        }else{
            if(path1.startsWith(MEMORY_DIR)){
                if(path2.startsWith(MEMORY_DIR)){
                    return true;
                }
            }else if(path1.startsWith(USBOTG_DIR)){
                if(path2.startsWith(USBOTG_DIR)){
                    return true;
                }
            }
        }
        return false;
    }

    public static String getFormatedFileName(String fileName){
        String formatedName = "";
        if(TextUtils.isEmpty(fileName)){
            return formatedName;
        }
        if(fileName.lastIndexOf(".") == -1){
            formatedName = fileName;
        }else{
            formatedName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        return formatedName;
    }

    public static long getCurMemoryFreeSize(String currentPath){
        long freeSize = 0;
        SDCardInfo sdCardInfo = Util.getSDCardInfo();

        MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();

        if(sdCardInfo!=null){
            if(currentPath.startsWith(MEMORY_DIR)){
                freeSize = memoryCardInfo.free;
            }else if(currentPath.startsWith(SD_DIR)){
                freeSize = sdCardInfo.free;
            }else if(currentPath.startsWith(USBOTG_DIR)){
                freeSize = usbStorageInfo.free;
            }
        }else {
            if(currentPath.startsWith(MEMORY_DIR)){
                freeSize = memoryCardInfo.free;
            }else if(currentPath.startsWith(USBOTG_DIR)){
                freeSize = usbStorageInfo.free;
            }
        }
        return freeSize;
    }

    //define this method for get a relative path of the given params
    public static String getRelativePathAtVolume(String volmePath, String folderPath) {
        String relativePath = null;
        if ((!TextUtils.isEmpty(folderPath)) && (!TextUtils.isEmpty(volmePath)) && (folderPath.indexOf(volmePath) >= 0)){
            relativePath = folderPath.substring(volmePath.length());
        }
        return relativePath;
    }

    public static void scanDirectories(Context context, String[] directories, String volumeName) {
        if (volumeName == null) {
            volumeName ="external";
        }
        MediaScanner scanner = new MediaScanner(context, volumeName);
        scanner.scanDirectories(directories);
    }

    public static void scanAllFile(Context context, String[] files) {
        if (files == null) {
            files = new String[]{MEMORY_DIR};
        }
        scanAllFile(context, files, null);
    }

    public static void scanAllFile(Context context, String[] files,
                                   MediaScannerConnection.OnScanCompletedListener callback) {
        if (context != null) {
            if (files == null) {
                return;
            }
            for (String path: files) {
                MediaScannerConnection.scanFile(context, new String[]{path}, null, callback);
            }
        }
    }
}
