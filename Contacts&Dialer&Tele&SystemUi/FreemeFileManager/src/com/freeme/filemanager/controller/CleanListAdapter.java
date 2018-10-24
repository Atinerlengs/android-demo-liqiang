package com.freeme.filemanager.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.garbage.CleanListItem;

import java.util.List;

public class CleanListAdapter extends ArrayAdapter<CleanListItem> {
    private LayoutInflater mInflater;
    private Context mContext;
    private int mResourceId;
    private List<CleanListItem> mCleanList;

    public CleanListAdapter(Context context, int resource, List<CleanListItem> objects) {
        super(context, resource, objects);
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mResourceId = resource;
        mCleanList = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        ViewHolder viewHolder;
        if (convertView != null) {
            view = convertView;
            viewHolder = (ViewHolder)view.getTag();
        } else {
            view = mInflater.inflate(mResourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mIcon = (ImageView)view.findViewById(R.id.child_icon);
            viewHolder.mName = (TextView)view.findViewById(R.id.child_name);
            viewHolder.mSize = (TextView)view.findViewById(R.id.child_size);
            view.setTag(viewHolder);
        }
        viewHolder.mName.setText(mCleanList.get(position).getName());
        viewHolder.mSize.setText(Util.convertStorage(mCleanList.get(position).getSize()));
        return view;
    }

    class ViewHolder {
        TextView mName;
        TextView mSize;
        ImageView mIcon;
    }
}
