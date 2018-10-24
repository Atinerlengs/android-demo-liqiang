package com.freeme.dialer.contacts.merge;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.dialer.R;
import com.freeme.contacts.common.utils.FreemeBitmapUtils;

import java.util.ArrayList;

public class FreemeMergeDialogAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Context mContext;
    FreemeContactData mFreemeContactData;
    ArrayList<String> mContactIds;
    private int defItem;

    FreemeMergeDialogAdapter(ArrayList<String> contactIds, Context context, FreemeContactData cData) {
        mFreemeContactData = cData;
        this.mContactIds = contactIds;
        this.inflater = LayoutInflater.from(context);
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return mContactIds.size();
    }

    @Override
    public Long getItem(int position) {
        return Long.parseLong(mContactIds.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setDefSelected(int position) {
        this.defItem = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.freeme_merge_dialog_adapter_item, null);

            viewHolder.avatar = convertView.findViewById(R.id.contact_avatar);
            viewHolder.displayName = convertView.findViewById(R.id.contact_displayname);
            viewHolder.nickName = convertView.findViewById(R.id.contact_nickname);
            viewHolder.selected = convertView.findViewById(R.id.contact_select);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        long key = getItem(position);
        viewHolder.avatar.setImageBitmap(convertToBimap(key));
        String displayNames = mFreemeContactData.getDisplayNames().get(key);
        String nickNames = mFreemeContactData.getNickNames().get(key);
        if (displayNames != null) {
            viewHolder.displayName.setVisibility(View.VISIBLE);
            viewHolder.displayName.setText(displayNames);
        } else {
            viewHolder.displayName.setVisibility(View.GONE);
        }

        if (nickNames != null) {
            viewHolder.nickName.setVisibility(View.VISIBLE);
            viewHolder.nickName.setText(nickNames);
        } else {
            viewHolder.nickName.setVisibility(View.GONE);
        }

        if (defItem == position) {
            viewHolder.selected.setVisibility(View.VISIBLE);
            viewHolder.selected.setImageDrawable(mContext.getDrawable(R.drawable.freeme_selected));
        } else {
            viewHolder.selected.setVisibility(View.GONE);
        }

        return convertView;
    }

    public Bitmap convertToBimap(long key) {
        Bitmap bitmap = null;
        if (mFreemeContactData.getPhotos().containsKey(key)) {
            bitmap = FreemeBitmapUtils.getRoundedCornerBitmapFromByte(mFreemeContactData.getPhotos().get(key).getPhoto());
        }
        if (bitmap == null) {
            bitmap = FreemeBitmapUtils.getBitmapFromDrawableRes(mContext.getResources(), R.drawable.freeme_list_default_stranger);
        }
        return bitmap;
    }

    static class ViewHolder {
        ImageView avatar;
        TextView displayName;
        TextView nickName;
        ImageView selected;
    }
}