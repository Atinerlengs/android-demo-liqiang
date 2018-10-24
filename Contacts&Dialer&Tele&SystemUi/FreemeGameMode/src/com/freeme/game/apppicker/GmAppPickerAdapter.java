package com.freeme.game.apppicker;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.game.R;
import com.freeme.game.apppicker.loader.GmAppModel;

public class GmAppPickerAdapter extends BaseAdapter {

    private static class ViewHolder {
        TextView mTitle;
        ImageView mAppIcon;
        TextView mAppName;
        CheckBox mCheckBox;
    }

    private List<GmAppModel> mAppList;
    private List<Boolean> mSelectedList = new ArrayList<>();
    private LayoutInflater mInflater;

    GmAppPickerAdapter(Context context, List<GmAppModel> list) {
        mAppList = list;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mAppList.size();
    }

    @Override
    public GmAppModel getItem(int position) {
        return mAppList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.gm_list_item_apps, null);

            holder.mTitle = (TextView) view.findViewById(R.id.title);
            holder.mAppIcon = (ImageView) view.findViewById(R.id.app_icon);
            holder.mAppName = (TextView) view.findViewById(R.id.app_name);
            holder.mCheckBox = (CheckBox) view.findViewById(R.id.check_box);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        GmAppModel appModel = getItem(position);
        String title = appModel.getTitle();
        if (TextUtils.isEmpty(title)) {
            holder.mTitle.setVisibility(View.GONE);
        } else {
            holder.mTitle.setVisibility(View.VISIBLE);
            holder.mTitle.setText(title);
        }
        holder.mAppIcon.setImageDrawable(appModel.getAppIcon());
        holder.mAppName.setText(appModel.getAppName());
        holder.mCheckBox.setChecked(isSelected(position));

        return view;
    }

    public void setData(List<GmAppModel> list) {
        mAppList = list;
        mSelectedList.clear();
        for (GmAppModel app : mAppList) {
            mSelectedList.add(app.isSelected());
        }
        notifyDataSetChanged();
    }

    public void setSelected(int position, boolean selected) {
        if (validPosition(position)) {
            mSelectedList.set(position, selected);
            notifyDataSetChanged();
        }
    }

    public boolean isSelected(int position) {
        return validPosition(position) ? mSelectedList.get(position) : false;
    }

    private boolean validPosition(int pos) {
        return pos >= 0 && pos < mSelectedList.size();
    }
}
