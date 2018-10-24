package com.freeme.filemanager.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.view.FileCategoryItem;

import java.util.List;

public class CategoryItemAdapter extends BaseAdapter {

    private List<FileCategoryItem> mItems = null;
    private Context mContext;

    public CategoryItemAdapter(Context context, List<FileCategoryItem> items) {
        mItems = items;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.layout_fast_category_item_vertical, null);
        ImageView imageView = (ImageView) view
                .findViewById(R.id.category_icon);
        TextView tvName = (TextView) view.findViewById(R.id.category_text);
        TextView tvConunt = (TextView) view
                .findViewById(R.id.category_count_tv);
        FileCategoryItem item = mItems.get(position);
        imageView.setImageResource(item.iconId);
        tvName.setText(mItems.get(position).textStringId);
        if (position > 2) {
            view.setBackgroundResource(R.drawable.grid_item_line1_background);
        } else {
            view.setBackgroundResource(R.drawable.grid_item_line0_background);
        }

        String countStr = "";
        long count = item.count;
        if (count <= 1) {
            countStr = count + " " + mContext.getString(R.string.child_item_count).toString().replace("s", "");
        } else {
            countStr = count + " " + mContext.getString(R.string.child_item_count);
        }
        tvConunt.setText(countStr);
        view.setTag(item);
        return view;
    }
}
