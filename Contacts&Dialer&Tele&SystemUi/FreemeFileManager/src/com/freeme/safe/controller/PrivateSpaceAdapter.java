package com.freeme.safe.controller;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.safe.encryption.provider.EncryptionColumns;

import static com.freeme.safe.encryption.provider.EncryptionColumns.FILE_URI;

public class PrivateSpaceAdapter extends BaseAdapter {

    private class ViewHolder {
        ImageView mIcon;
        TextView mName;
        TextView mCount;
    }

    private String[] mItemID;

    private LayoutInflater mInflater;

    private Context mContext;

    public PrivateSpaceAdapter(Context context, String[] itemId) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mItemID = itemId;
    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.layout_safe_category_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mIcon = convertView.findViewById(R.id.child_icon);
            viewHolder.mName = convertView.findViewById(R.id.child_name);
            viewHolder.mCount = convertView.findViewById(R.id.child_count);

            switch (position) {
                case MediaFile.AUDIO_TYPE:
                    viewHolder.mIcon.setImageDrawable(mContext.getDrawable(R.drawable.file_icon_audio));
                    break;
                case MediaFile.VIDEO_TYPE:
                    viewHolder.mIcon.setImageDrawable(mContext.getDrawable(R.drawable.file_icon_video));
                    break;
                case MediaFile.IMAGE_TYPE:
                    viewHolder.mIcon.setImageDrawable(mContext.getDrawable(R.drawable.file_icon_image));
                    break;
                case MediaFile.DOC_TYPE:
                    viewHolder.mIcon.setImageDrawable(mContext.getDrawable(R.drawable.file_icon_txt));
                    break;
                case MediaFile.OTHER_TYPE:
                    viewHolder.mIcon.setImageDrawable(mContext.getDrawable(R.drawable.file_icon_default));
                    break;
                default:
                    viewHolder.mIcon.setImageDrawable(mContext.getDrawable(R.drawable.file_icon_default));
                    break;
            }
            viewHolder.mName.setText(mItemID[position]);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        Cursor cursor = mContext.getContentResolver().query(FILE_URI, null,
                EncryptionColumns.MEDIA_TYPE + "=?",
                new String[]{String.valueOf(position)},
                null);
        int count;
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        } else {
            count = 0;
        }

        String countStr;
        if (count > 1) {
            countStr = count + " " + mContext.getString(R.string.child_item_count);
        } else {
            countStr = count + " " + mContext.getString(R.string.child_item_count).replace("s", "");
        }
        viewHolder.mCount.setText(countStr);

        return convertView;
    }

    @Override
    public int getCount() {
        return mItemID.length;
    }

    @Override
    public Object getItem(int position) {
        return mItemID[position];
    }
}
