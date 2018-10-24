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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.model.MediaFile.MediaFileType;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.view.Settings;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FileCategoryHelper {

    public enum FileCategory {
        All,
        Music,
        Video,
        Picture,
        Doc,
        Apk,
        Zip,
        QQ,
        WeChat,
        Custom,
        Other,
        Favorite
    }

    public class CategoryInfo {
        public long count;

        public long size;
    }

    public static final int COLUMN_ID = 0;

    public static final int COLUMN_PATH = 1;

    public static final int COLUMN_SIZE = 2;

    public static final int COLUMN_DATE = 3;

    private static final String LOG_TAG = "FileCategoryHelper";
    public static HashMap<FileCategory, FilenameExtFilter> filters = new HashMap<FileCategory, FilenameExtFilter>();
    public static HashMap<FileCategory, Integer> categoryNames = new HashMap<FileCategory, Integer>();
    public static FileCategory[] sCategories = new FileCategory[]{
            FileCategory.Music,
            FileCategory.Video,
            FileCategory.Picture,
            FileCategory.Doc,
            FileCategory.Apk,
            FileCategory.Zip,
            FileCategory.QQ,
            FileCategory.WeChat,
            FileCategory.Other
    };
    private static final String APK_EXT = "apk";

    static {
        categoryNames.put(FileCategory.All, R.string.category_all);
        categoryNames.put(FileCategory.Music, R.string.category_music);
        categoryNames.put(FileCategory.Video, R.string.category_video);
        categoryNames.put(FileCategory.Picture, R.string.category_picture);
        categoryNames.put(FileCategory.Doc, R.string.category_document);
        categoryNames.put(FileCategory.Apk, R.string.category_apk);
        categoryNames.put(FileCategory.Zip, R.string.category_zip);
        categoryNames.put(FileCategory.QQ, R.string.category_qq);
        categoryNames.put(FileCategory.WeChat, R.string.category_wechat);
        categoryNames.put(FileCategory.Other, R.string.category_other);
        categoryNames.put(FileCategory.Favorite, R.string.category_favorite);
    }

    private FileCategory mCategory;
    private Context mContext;
    private HashMap<FileCategory, CategoryInfo> mCategoryInfo = new HashMap<FileCategory, CategoryInfo>();

    public FileCategoryHelper(Context context) {
        mContext = context;

        mCategory = FileCategory.All;
    }

    public static FileCategory getCategoryFromPath(String path, String mimeType) {
        if (mimeType != null) {
            if (mimeType.contains("audio")) {
                return FileCategory.Music;
            } else if (mimeType.contains("video")) {
                return FileCategory.Video;
            }
        }
        if (path == null) {
            return null;
        }
        MediaFileType type = MediaFile.getFileType(path);
        if (type != null) {
            if (MediaFile.isAudioFileType(type.fileType)) return FileCategory.Music;
            if (MediaFile.isVideoFileType(type.fileType)) return FileCategory.Video;
            if (MediaFile.isImageFileType(type.fileType)) return FileCategory.Picture;
            if (Util.sDocMimeTypesSet.contains(type.mimeType)) return FileCategory.Doc;
        }

        int dotPosition = path.lastIndexOf('.');
        if (dotPosition < 0) {
            return FileCategory.Other;
        }

        String ext = path.substring(dotPosition + 1);
        if (ext.equalsIgnoreCase(APK_EXT)) {
            return FileCategory.Apk;
        }
        return FileCategory.Other;
    }

    private static boolean matchExts(String ext, String[] exts) {
        for (String ex : exts) {
            if (ex.equalsIgnoreCase(ext))
                return true;
        }
        return false;
    }

    public FileCategory getCurCategory() {
        return mCategory;
    }

    public void setCurCategory(FileCategory c) {
        mCategory = c;
    }

    public int getCurCategoryNameResId() {
        return categoryNames.get(mCategory);
    }

    public void setCustomCategory(String[] exts) {
        mCategory = FileCategory.Custom;
        if (filters.containsKey(FileCategory.Custom)) {
            filters.remove(FileCategory.Custom);
        }

        filters.put(FileCategory.Custom, new FilenameExtFilter(exts));
    }

    public FilenameFilter getFilter() {
        return filters.get(mCategory);
    }

    public HashMap<FileCategory, CategoryInfo> getCategoryInfos() {
        return mCategoryInfo;
    }

    public CategoryInfo getCategoryInfo(FileCategory fc) {
        if (mCategoryInfo.containsKey(fc)) {
            return mCategoryInfo.get(fc);
        } else {
            CategoryInfo info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
            return info;
        }
    }

    private void setCategoryInfo(FileCategory fc, long count, long size) {
        CategoryInfo info = mCategoryInfo.get(fc);
        if (info == null) {
            info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
        }
        info.count = count;
        info.size = size;
    }


    private String buildDocSelection() {
        StringBuilder selection = new StringBuilder();
        Iterator<String> iter = Util.sDocMimeTypesSet.iterator();
        while (iter.hasNext()) {
            selection.append("(" + FileColumns.MIME_TYPE + "=='" + iter.next() + "') OR ");
        }
        return selection.substring(0, selection.lastIndexOf(")") + 1);
    }

    private String buildZipSelection() {
        StringBuilder selection = new StringBuilder();
        Iterator<String> iter = Util.sZipFileMimeTypeSet.iterator();
        while (iter.hasNext()) {
            selection.append("(" + FileColumns.MIME_TYPE + "=='" + iter.next() + "') OR ");
        }
        return selection.substring(0, selection.lastIndexOf(")") + 1);
    }

    private String buildApkSelection() {
        StringBuilder selection = new StringBuilder();
        Iterator<String> iter = Util.sApkFileMimeTypeSet.iterator();
        while (iter.hasNext()) {
            selection.append("(" + FileColumns.MIME_TYPE + "=='" + iter.next() + "') OR ");
        }
        return selection.substring(0, selection.lastIndexOf(")") + 1);
    }

    public String buildSelectionByCategory(FileCategory cat) {
        String selection = null;
        switch (cat) {
            case Doc:
                selection = buildDocSelection();
                break;
            case Apk:
                selection = "(" + buildApkSelection() + ") AND " +
                        FileColumns.DATA + " not LIKE '/data/data/com%' and " +
                        FileColumns.SIZE + " <> 0";
                break;
            case Zip:
                selection = buildZipSelection();
                break;

            case Music:
                selection = FileColumns.DISPLAY_NAME + " NOT LIKE '%hangouts_%'";
                break;
            case Other:
                selection = "(" +
                        FileColumns.MEDIA_TYPE + " not in ( 'text/plain','text/html'," +
                            "'application/zip'," +
                            "'application/pdf'," +
                            "'application/msword'," +
                            "'application/vnd.ms-excel'," +
                            "'application/vnd.ms-powerpoint'," +
                            "'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'," +
                            "'application/vnd.openxmlformats-officedocument.wordprocessingml.document'," +
                            "'application/vnd.openxmlformats-officedocument.wordprocessingml.template'," +
                            "'application/vnd.openxmlformats-officedocument.presentationml.presentation') or " +
                        FileColumns.MEDIA_TYPE + " is null) and " +
                        FileColumns.MEDIA_TYPE + " = 0 and " +
                        FileColumns.SIZE + " <> 0 and (" +
                        FileColumns.MIME_TYPE + " <> 'application/vnd.android.package-archive')";
                break;
            case Picture:
                selection = Images.ImageColumns.MIME_TYPE + " <> 'null'";
                break;
            default:
                selection = null;
        }
        return selection;
    }

    private String buildSelectionByCategoryM(FileCategory cat) {
        String selection = null;
        switch (cat) {
            case Doc:
                selection = "(" + buildDocSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.getDefaultPath() + "%'";
                break;
            case Apk:
                selection = "(" + buildApkSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.getDefaultPath() + "%' and " +
                        FileColumns.SIZE + " <> 0";
                break;
            case Zip:
                selection = "(" + buildZipSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.getDefaultPath() + "%'";
                break;
            case Picture:
                selection = Images.ImageColumns.MIME_TYPE + " <> 'null' and " +
                        FileColumns.DATA + " LIKE '" + Util.getDefaultPath() + "%'";
                break;
            default:
                selection = FileColumns.DATA + " LIKE '" + Util.getDefaultPath() + "%'";
        }
        return selection;
    }

    private String buildSelectionByCategoryS(FileCategory cat) {
        String selection = null;
        switch (cat) {
            case Doc:
                selection = "(" + buildDocSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.SD_DIR + "%'";
                break;
            case Apk:
                selection = "(" + buildApkSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.SD_DIR + "%' and " + FileColumns.SIZE + " <> 0";
                break;
            case Zip:
                selection = "(" + buildZipSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.SD_DIR + "%'";
                break;
            case Other:
                selection = "(" +
                        FileColumns.MEDIA_TYPE + " not in ( 'text/plain'," +
                            "'text/html'," +
                            "'application/zip'," +
                            "'application/pdf'," +
                            "'application/msword'," +
                            "'application/vnd.ms-excel'," +
                            "'application/vnd.ms-powerpoint'," +
                            "'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'," +
                            "'application/vnd.openxmlformats-officedocument.wordprocessingml.document'," +
                            "'application/vnd.openxmlformats-officedocument.wordprocessingml.template')  or " +
                        FileColumns.MEDIA_TYPE + " is null) and " +
                        FileColumns.MEDIA_TYPE + " = 0";
                break;
            case Picture:
                selection = Images.ImageColumns.MIME_TYPE + " <> 'null' and " +
                        FileColumns.DATA + " LIKE '" + Util.SD_DIR + "%'";
                break;
            default:
                selection = FileColumns.DATA + " LIKE '" + Util.SD_DIR + "%'";
        }
        return selection;
    }

    private String buildSelectionByCategoryO(FileCategory cat) {
        String selection = null;
        switch (cat) {
            case Doc:
                selection = "(" + buildDocSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.USBOTG_DIR + "%'";
                break;
            case Apk:
                selection = "(" + buildApkSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.USBOTG_DIR + "%' and " +
                        FileColumns.SIZE + " <> 0";
                break;
            case Zip:
                selection = "(" + buildZipSelection() + ") AND " +
                        FileColumns.DATA + " LIKE '" + Util.USBOTG_DIR + "%'";
                break;
            case Other:
                selection = "(" +
                        FileColumns.MEDIA_TYPE + " not in ( 'text/plain'," +
                        "'text/html'," +
                        "'application/zip'," +
                        "'application/pdf'," +
                        "'application/msword'," +
                        "'application/vnd.ms-excel'" +
                        ",'application/vnd.ms-powerpoint'," +
                        "'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'," +
                        "'application/vnd.openxmlformats-officedocument.wordprocessingml.document'," +
                        "'application/vnd.openxmlformats-officedocument.wordprocessingml.template')  or " +
                        FileColumns.MEDIA_TYPE + " is null) and " +
                        FileColumns.MEDIA_TYPE + " = 0";
                break;
            case Picture:
                selection = Images.ImageColumns.MIME_TYPE + " <> 'null' and " +
                        FileColumns.DATA + " LIKE '" + Util.USBOTG_DIR + "%'";
                break;
            default:
                selection = FileColumns.DATA + " LIKE '" + Util.USBOTG_DIR + "%'";
        }
        return selection;
    }

    public Uri getContentUriByCategory(FileCategory cat) {
        Uri uri;
        String volumeName = "external";
        switch (cat) {
            case Doc:
            case Apk:
            case Zip:
            case Other:
                uri = Files.getContentUri(volumeName);
                break;
            case Music:
                uri = Audio.Media.getContentUri(volumeName);
                break;
            case Video:
                uri = Video.Media.getContentUri(volumeName);
                break;
            case Picture:
                uri = Images.Media.getContentUri(volumeName);
                break;
            default:
                uri = null;
                break;
        }
        return uri;
    }

    private String buildSortOrder(SortMethod sort) {
        String sortOrder = null;
        switch (sort) {
            case name:
                sortOrder = FileColumns.DISPLAY_NAME + " asc";
                break;
            case size:
                sortOrder = FileColumns.SIZE + " asc";
                break;
            case date:
                sortOrder = FileColumns.DATE_MODIFIED + " desc";
                break;
            case type:
                sortOrder = FileColumns.MIME_TYPE + " asc, " + FileColumns.DISPLAY_NAME + " asc";
                break;
        }
        return sortOrder;
    }

    public Cursor query(FileCategory fc, String bucketid) {
        Uri uri = getContentUriByCategory(fc);

        String[] columns = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DISPLAY_NAME
        };

        if (uri != null) {
            String selection = MediaStore.Images.Media.BUCKET_ID + "=?";
            return mContext.getContentResolver().query(uri, columns, selection, new String[]{bucketid},
                    MediaStore.Images.Media.DATE_MODIFIED + " desc");
        } else {
            return null;
        }
    }

    public Cursor query(FileCategory fc, SortMethod sort) {
        Uri uri = getContentUriByCategory(fc);
        String selection = buildSelectionByCategory(fc);
        String sortOrder = buildSortOrder(sort);

        if (uri == null) {
            Log.e(LOG_TAG, "invalid uri, category:" + fc.name());
            return null;
        }

        String[] columns = new String[]{
                FileColumns._ID,
                FileColumns.DATA,
                FileColumns.SIZE,
                FileColumns.DATE_MODIFIED,
                FileColumns.DISPLAY_NAME
        };

        if (fc.equals(FileCategory.Picture)) {
            columns = new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_MODIFIED,
                    MediaStore.Images.Media.DISPLAY_NAME
            };
        }

        if (!Settings.instance().getShowDotAndHiddenFiles() && selection != null) {
            selection = buildSelectionByHide(selection);
        }
        if (selection == null && !Settings.instance().getShowDotAndHiddenFiles()) {
            selection = buildSelectionByHideNotdoc(selection);
        }

        return mContext.getContentResolver().query(uri, columns, selection, null, sortOrder);
    }

    public Cursor query(FileCategory fc, long minSize) {
        Uri uri = getContentUriByCategory(fc);
        String selection = buildSelectionByCategory(fc);

        if (uri == null) {
            Log.e(LOG_TAG, "invalid uri, category:" + fc.name());
            return null;
        }

        String[] columns = new String[]{
                FileColumns._ID,
                FileColumns.DATA,
                FileColumns.SIZE,
                FileColumns.DATE_MODIFIED,
                FileColumns.DISPLAY_NAME
        };

        if (!Settings.instance().getShowDotAndHiddenFiles() && selection != null) {
            selection = buildSelectionByHide(selection);
        }
        if (selection == null && !Settings.instance().getShowDotAndHiddenFiles()) {
            selection = buildSelectionByHideNotdoc(selection);
        }

        String selection_size = FileColumns.SIZE + ">" + minSize;
        if (selection == null) {
            selection = selection_size;
        } else {
            selection = "(" + selection + ") AND (" + selection_size +")";
        }

        return mContext.getContentResolver().query(uri, columns, selection, null,FileColumns.SIZE + " asc");
    }

    public Cursor query(FileCategory fc, SortMethod sort, int isType) {
        Uri uri = getContentUriByCategory(fc);
        String selection = "";
        if (isType == GlobalConsts.IS_CATEGORY_FRAGMENT) {
            selection = buildSelectionByCategory(fc);
        } else if (isType == GlobalConsts.IS_SD_CARD) {
            selection = buildSelectionByCategoryS(fc);
        } else if (isType == GlobalConsts.IS_USBOTG_CARD) {
            selection = buildSelectionByCategoryO(fc);
        } else {
            selection = buildSelectionByCategoryM(fc);
        }

        String sortOrder = buildSortOrder(sort);

        if (uri == null) {
            Log.e(LOG_TAG, "invalid uri, category:" + fc.name());
            return null;
        }

        String[] columns = new String[]{
                FileColumns._ID,
                FileColumns.DATA,
                FileColumns.SIZE,
                FileColumns.DATE_MODIFIED
        };
        //Hidden Files
        if (!Settings.instance().getShowDotAndHiddenFiles() && selection != null) {
            selection = buildSelectionByHide(selection);
        }
        if (selection == null && !Settings.instance().getShowDotAndHiddenFiles()) {
            selection = buildSelectionByHideNotdoc(selection);
        }
        if (fc.equals(FileCategory.Music)) {
            selection = buildSelectionByHIDEMUSIC(selection);
        }

        return mContext.getContentResolver().query(uri, columns, selection, null, sortOrder);
    }

    public void refreshCategoryInfo(int isType, boolean b) {
        // clear
        for (FileCategory fc : sCategories) {
            setCategoryInfo(fc, 0, 0);
        }

        // query database
        String volumeName = "external";

        Uri uri = Audio.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Music, uri, isType, b);

        uri = Video.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Video, uri, isType, b);

        uri = Images.Media.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Picture, uri, isType, b);


        uri = Files.getContentUri(volumeName);
        refreshMediaCategory(FileCategory.Doc, uri, isType, b);
        refreshMediaCategory(FileCategory.Apk, uri, isType, b);
        refreshMediaCategory(FileCategory.Zip, uri, isType, b);
    }

    private boolean refreshMediaCategory(FileCategory fc, Uri uri, int isType, boolean b) {
        Cursor c = null;
        String[] columns = new String[]{
                "COUNT(*)", "SUM(_size)"
        };
        if (isType == GlobalConsts.IS_CATEGORY_FRAGMENT) {
            c = mContext.getContentResolver().query(uri, columns, buildSelectionByCategory(fc), null, null);
        } else if (isType == GlobalConsts.IS_SD_CARD) {
            c = mContext.getContentResolver().query(uri, columns, buildSelectionByCategoryS(fc), null, null);
        } else if (isType == GlobalConsts.IS_USBOTG_CARD) {
            c = mContext.getContentResolver().query(uri, columns, buildSelectionByCategoryO(fc), null, null);
        } else {
            c = mContext.getContentResolver().query(uri, columns, buildSelectionByCategoryM(fc), null, null);
        }
        if (c == null) {
            Log.e(LOG_TAG, "fail to query uri:" + uri);
            return false;
        }

        if (c.moveToNext()) {
            setCategoryInfo(fc, c.getLong(0), c.getLong(1));
            Log.v(LOG_TAG, "Retrieved " + fc.name() + " info >>> count:" + c.getLong(0) + " size:" + c.getLong(1));
            c.close();
            return true;
        }

        return false;
    }

    private String buildSelectionByHide(String selection) {
        selection = "(" + selection + ") AND " + "(" + FileColumns.DISPLAY_NAME + " IS NULL OR " + FileColumns.DISPLAY_NAME + " NOT LIKE '.%'" + ")";
        return selection;
    }

    private String buildSelectionByHideNotdoc(String selection) {
        selection = FileColumns.DISPLAY_NAME + " NOT LIKE '.%'";
        return selection;
    }

    // hide the file of google .ogg
    private String buildSelectionByHIDEMUSIC(String selection) {
        selection = "(" + selection + ") AND " + (FileColumns.DISPLAY_NAME + " NOT LIKE '%hangouts_%'");
        return selection;
    }

    //Hidden Files
    public void deleteNoExistFile(String[] files) {
        String volumeName = "external";
        Uri uri = Files.getContentUri(volumeName);
        StringBuilder deleteWhere = new StringBuilder();
        deleteWhere.append(MediaStore.Files.FileColumns.DATA + " in ('-1'");
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            String fileStr = files[i];
            File file = new File(fileStr);
            if (!file.exists()) {
                list.add(fileStr);
                deleteWhere.append(",'" + fileStr + "'");
            }
        }
        if (list.size() == 0) {
            return;
        }
        deleteWhere.append(")");
        mContext.getContentResolver().delete(uri, deleteWhere.toString(), null);
    }
}
