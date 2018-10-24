package com.freeme.filemanager.controller;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.StorageCategoryItem;

import java.util.HashMap;
import java.util.Map;

public class CategoryStorageItemAdapter extends BaseAdapter {

    private Map<String, StorageCategoryItem> mstorageItems = new HashMap<>();
    private Context mContext;


    public CategoryStorageItemAdapter(Context context, Map<String, StorageCategoryItem> items) {
        mstorageItems = items;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mstorageItems != null ? mstorageItems.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mstorageItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.layout_storage_category_item, null);
        TextView title = (TextView) view.findViewById(R.id.storage_category_title);

        ProgressBar availProgressBar = (ProgressBar) view.findViewById(R.id.phoneRectview);
        TextView availStorageTv = (TextView) view.findViewById(R.id.phoneVolume);

        if(mstorageItems != null) {

            StorageCategoryItem item = (StorageCategoryItem) mstorageItems.values().toArray()[position];

            String mTitle = item.getTitle();
            title.setText(mTitle);

            if(mTitle.equals(mContext.getString(R.string.all_storage))){
                availProgressBar.setVisibility(View.VISIBLE);
                availStorageTv.setVisibility(View.VISIBLE);
                availStorageTv.setText(mContext.getString(R.string.storage_available,
                        Formatter.formatFileSize(mContext, item.getFree())));
                if (item.getFree() == 0) {
                    availProgressBar.setProgress(0);
                } else {
                    int progress = (int)((item.getTotal() - item.getFree())*100/item.getTotal());
                    availProgressBar.setProgress(progress);
                }

            } else if (mTitle.equals(mContext.getString(R.string.file_clean))) {
                availProgressBar.setVisibility(View.GONE);
                availStorageTv.setVisibility(View.GONE);
            } else {
                availProgressBar.setVisibility(View.GONE);
                availStorageTv.setVisibility(View.GONE);
            }
/*
            info.setText(mContext.getString(R.string.storage_total, Util.convertStorage(item.getTotal()))
                    + "  " + mContext.getString(R.string.storage_available, Util.convertStorage(item.getFree())));*/
            view.setTag(mTitle);
        }

        return view;
    }
}
