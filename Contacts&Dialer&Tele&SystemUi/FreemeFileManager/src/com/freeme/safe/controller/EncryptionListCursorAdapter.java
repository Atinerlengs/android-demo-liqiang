package com.freeme.safe.controller;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.model.EncryptionFileInfo;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.util.ArrayList;
import java.util.Collection;

public class EncryptionListCursorAdapter extends CursorAdapter {

    private int mSelectionMode;
    private boolean mInActionMode;

    private ArrayList<EncryptionFileInfo> mFileNameList;

    private LayoutInflater mFactory;

    public EncryptionListCursorAdapter(Context context, Cursor c) {
        super(context, c, false);
        mFactory = LayoutInflater.from(context);
        mFileNameList = new ArrayList<>();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return mFactory.inflate(R.layout.layout_cleaning_list_item, viewGroup, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView icon = view.findViewById(R.id.child_icon);
        TextView name = view.findViewById(R.id.child_name);
        TextView size = view.findViewById(R.id.child_size);
        CheckBox checkbox = view.findViewById(R.id.file_checkbox);
        if (mInActionMode) {
            checkbox.setVisibility(View.VISIBLE);
            size.setVisibility(View.GONE);

            switch (mSelectionMode) {
                case SafeConstants.FRAGMENT_ALL_DESELECT:
                    checkbox.setChecked(false);
                    break;
                case SafeConstants.FRAGMENT_ALL_SELECT:
                    checkbox.setChecked(true);
                    break;
                default:
                    break;
            }
        } else {
            checkbox.setChecked(false);
            checkbox.setVisibility(View.GONE);
            size.setVisibility(View.VISIBLE);
        }

        int typeColumnIndex = cursor.getColumnIndex(EncryptionColumns.MEDIA_TYPE);
        int originPathColumnIndex = cursor.getColumnIndex(EncryptionColumns.ORIGINAL_PATH);
        int sizeColumnIndex = cursor.getColumnIndex(EncryptionColumns.ORIGINAL_SIZE);

        String fileName = SafeUtils.getOriginalFileName(cursor.getString(originPathColumnIndex));
        name.setText(fileName);
        size.setText(Util.convertStorage(cursor.getLong(sizeColumnIndex)));
        int mediaType = cursor.getInt(typeColumnIndex);
        switch (mediaType) {
            case MediaFile.AUDIO_TYPE:
                icon.setImageResource(R.drawable.file_icon_audio);
                break;
            case MediaFile.VIDEO_TYPE:
                icon.setImageResource(R.drawable.file_icon_video);
                break;
            case MediaFile.IMAGE_TYPE:
                icon.setImageResource(R.drawable.file_icon_image);
                break;
            case MediaFile.DOC_TYPE:
                icon.setImageResource(R.drawable.file_icon_txt);
                break;
            default:
                icon.setImageResource(R.drawable.file_icon_default);
                break;
        }
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        mFileNameList.clear();
        getAllFiles();
    }

    public EncryptionFileInfo getFileItem(int pos) {
        if (mFileNameList.size() > pos) {
            return mFileNameList.get(pos);
        }

        Cursor cursor = (Cursor) getItem(pos);
        EncryptionFileInfo fileInfo = getFileInfo(cursor);
        if (fileInfo == null) {
            return null;
        }
        mFileNameList.add(fileInfo);
        return fileInfo;
    }

    public Collection<EncryptionFileInfo> getAllFiles() {
        if (mFileNameList.size() == getCount()) {
            return mFileNameList;
        }

        Cursor cursor = getCursor();
        mFileNameList.clear();
        if (cursor.moveToFirst()) {
            do {
                EncryptionFileInfo fileInfo = getFileInfo(cursor);
                if (fileInfo != null) {
                    mFileNameList.add(fileInfo);
                }
            } while (cursor.moveToNext());
        }
        return mFileNameList;
    }

    private EncryptionFileInfo getFileInfo(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        return new EncryptionFileInfo(
                cursor.getString(cursor.getColumnIndex(EncryptionColumns.ORIGINAL_PATH)),
                cursor.getString(cursor.getColumnIndex(EncryptionColumns.ENCRYPTION_NAME)),
                cursor.getString(cursor.getColumnIndex(EncryptionColumns.ROOT_PATH)),
                cursor.getLong(cursor.getColumnIndex(EncryptionColumns.ORIGINAL_SIZE)),
                cursor.getInt(cursor.getColumnIndex(EncryptionColumns.ORIGINAL_TYPE)),
                cursor.getInt(cursor.getColumnIndex(EncryptionColumns.ORIGINAL_COUNT)),
                cursor.getInt(cursor.getColumnIndex(EncryptionColumns.MEDIA_TYPE))
        );
    }

    public void setInCheck(boolean hasActionMode) {
        mInActionMode = hasActionMode;
    }

    public void setSelectionMode(int selectionMode) {
        mSelectionMode = selectionMode;
    }
}