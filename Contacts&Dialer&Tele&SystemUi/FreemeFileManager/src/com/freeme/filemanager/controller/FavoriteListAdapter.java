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

import java.util.List;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.FavoriteItem;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class FavoriteListAdapter extends ArrayAdapter<FavoriteItem> {
    private Context mContext;

    private LayoutInflater mInflater;

    private FileIconHelper mFileIcon;

    public FavoriteListAdapter(Context context, int resource, List<FavoriteItem> objects, FileIconHelper fileIcon) {
        super(context, resource, objects);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFileIcon = fileIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(R.layout.layout_favorite_item, parent, false);
        }

        FavoriteItem item = getItem(position);
        FileInfo lFileInfo = item.fileInfo;
        if(lFileInfo != null){
            Util.setText(view, R.id.file_name, item.title != null ? item.title : lFileInfo.fileName);
            if (lFileInfo.ModifiedDate > 0) {
                String modifyDateTime = DateUtils.formatDateRange(mContext, lFileInfo.ModifiedDate, lFileInfo.ModifiedDate,
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR |
                        DateUtils.FORMAT_NUMERIC_DATE );
                Util.setText(view, R.id.modified_time, modifyDateTime);
                view.findViewById(R.id.modified_time).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.modified_time).setVisibility(View.GONE);
            }
            view.findViewById(R.id.modified_time).setVisibility(lFileInfo.ModifiedDate > 0 ? View.VISIBLE: View.GONE);
            if (lFileInfo.IsDir) {
                view.findViewById(R.id.slash_before_size).setVisibility(View.GONE);
                view.findViewById(R.id.file_size).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.file_size).setVisibility(View.VISIBLE);
                Util.setText(view, R.id.file_size, Util.convertStorage(lFileInfo.fileSize));
            }

            ImageView lFileImage = (ImageView) view.findViewById(R.id.file_image);
            lFileImage.setTag(lFileInfo.filePath);
            if (lFileInfo.IsDir) {
                lFileImage.setImageResource(R.drawable.folder);
                view.findViewById(R.id.dir_arrow).setVisibility(View.VISIBLE);
            } else {
                mFileIcon.setIcon(mContext, lFileInfo, lFileImage);
                view.findViewById(R.id.dir_arrow).setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

}
