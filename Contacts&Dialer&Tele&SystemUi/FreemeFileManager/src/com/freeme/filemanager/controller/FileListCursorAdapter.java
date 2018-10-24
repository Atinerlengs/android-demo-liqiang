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
package com.freeme.filemanager.controller;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.FileListItem;
import com.freeme.filemanager.view.Settings;

public class FileListCursorAdapter extends CursorAdapter {

    private final LayoutInflater mFactory;

    private FileViewInteractionHub mFileViewInteractionHub;

    private FileIconHelper mFileIcon;

    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();

    private Context mContext;

    private FileSortHelper mSortHelper;

    private int mResourceId;

    public FileListCursorAdapter(Context context, Cursor cursor, FileViewInteractionHub f, FileIconHelper fileIcon) {
        super(context, cursor, false);
        mFactory = LayoutInflater.from(context);
        mFileViewInteractionHub = f;
        mFileIcon = fileIcon;
        mContext = context;
        mResourceId = -1;
    }

    public FileListCursorAdapter(Context context, Cursor cursor, int resId, FileViewInteractionHub f, FileIconHelper fileIcon) {
        super(context, cursor, false);
        mFactory = LayoutInflater.from(context);
        mFileViewInteractionHub = f;
        mFileIcon = fileIcon;
        mContext = context;
        mResourceId = resId;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        FileListItem listItem = (FileListItem) view;
        FileInfo fileInfo = getFileItem(cursor.getPosition());
        if (fileInfo == null) {
            // file is not existing, create a fake info
            fileInfo = new FileInfo();
            fileInfo.dbId = cursor.getLong(FileCategoryHelper.COLUMN_ID);
            fileInfo.filePath = cursor.getString(FileCategoryHelper.COLUMN_PATH);
            fileInfo.fileName = Util.getNameFromFilepath(fileInfo.filePath);
            fileInfo.fileSize = cursor.getLong(FileCategoryHelper.COLUMN_SIZE);
            fileInfo.ModifiedDate = cursor.getLong(FileCategoryHelper.COLUMN_DATE);
        }

        listItem.bind(mContext, fileInfo, mFileViewInteractionHub, mFileIcon);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (mResourceId == -1) {
            mResourceId = R.layout.layout_file_list_item;
        }
        return mFactory.inflate(mResourceId, parent, false);
    }


    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        mFileNameList.clear();

        getAllFiles();
    }

    public void setSortHelper(FileSortHelper sortHelper) {
        mSortHelper = sortHelper;
    }

    public Collection<FileInfo> getAllFiles() {
        if (mFileNameList.size() == getCount())
            return mFileNameList;

        Cursor cursor = getCursor();
        mFileNameList.clear();
        if (cursor.moveToFirst()) {
            do {
                FileInfo fileInfo = getFileInfo(cursor);
                if (fileInfo != null) {
                    if (Settings.instance().getShowDotAndHiddenFiles()) {
                        mFileNameList.add(fileInfo);
                    }else {
                    if (!fileInfo.isHidden) {
                        mFileNameList.add(fileInfo);
                    }
                    }
                }
            } while (cursor.moveToNext());
        }
        //sort by type
        if (mSortHelper != null) {
            Collections.sort(mFileNameList, mSortHelper.getComparator());
        }
        return mFileNameList;
    }

    public FileInfo getFileItem(int pos) {
        if (mFileNameList.size() > pos)
            return mFileNameList.get(pos);

        Cursor cursor = (Cursor) getItem(pos);
        FileInfo fileInfo = getFileInfo(cursor);
        if (fileInfo == null)
            return null;

        fileInfo.dbId = cursor.getLong(FileCategoryHelper.COLUMN_ID);
        mFileNameList.add(fileInfo);
        return fileInfo;
    }

    private FileInfo getFileInfo(Cursor cursor) {
        return (cursor == null || cursor.getCount() == 0) ? null : Util.GetFileInfo(cursor.getString(FileCategoryHelper.COLUMN_PATH));
    }
}
