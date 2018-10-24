package com.freeme.recents.presentation.view.adapter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.freeme.recents.presentation.view.fragment.RecentsMultiWindowFragment.FreemePackageInfo;

import java.util.List;

public class GridViewAdapter extends BaseAdapter {

    private List<FreemePackageInfo> mListFreemePackageInfo;
    private Context mContext;

    public GridViewAdapter (List<FreemePackageInfo> ListFreemePackageInfo,Context Context){
        mListFreemePackageInfo = ListFreemePackageInfo;
        mContext = Context;
    }

    @Override
    public int getCount() {
        return mListFreemePackageInfo.size ();
    }

    @Override
    public Object getItem(int position) {
        return mListFreemePackageInfo.get (position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.griditem_layout, null);
        } else {
            view = convertView;
        }
        TextView mTextView = view.findViewById (R.id.title);
        ImageView mImageView = view.findViewById (R.id.image);
        mTextView.setText (mListFreemePackageInfo.get(position).label);
        mImageView.setImageDrawable (mListFreemePackageInfo.get(position).icon);
        final ComponentName componentName = mListFreemePackageInfo.get(position).componentName;
        view.setOnClickListener (new View.OnClickListener (
        ) {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent ();
                intent.setComponent (componentName);
                mContext.startActivity (intent);
            }
        });
        return view;
    }
}
