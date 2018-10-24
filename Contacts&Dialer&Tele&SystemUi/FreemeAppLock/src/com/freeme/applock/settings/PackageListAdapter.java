package com.freeme.applock.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.freeme.applock.R;
import com.freeme.internal.app.AppLockPolicy;

public class PackageListAdapter extends BaseAdapter {
    private static final String TAG = PackageListAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;
    Context mContext;
    Handler mHandler;
    LayoutInflater mInflater;
    private boolean mMasterSwitch;
    ArrayList<AppInfo> mAppList = new ArrayList<>();
    PackageInfoUtil mPackageInfoUtil = PackageInfoUtil.getInstance();

    private static final int[] DRAWABLEIDS = {
            com.freeme.internal.R.drawable.listitem_backgroud_head_freeme,
            com.freeme.internal.R.drawable.listitem_backgroud_middle_freeme,
            com.freeme.internal.R.drawable.listitem_backgroud_tail_freeme,
            com.freeme.internal.R.drawable.listitem_backgroud_full_freeme
    };

    private class OnSwitchCheckedChangeListener implements OnCheckedChangeListener {
        private ViewHolder mHolder;

        public OnSwitchCheckedChangeListener(int pos, ViewHolder holder) {
            mHolder = holder;
        }

        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            int appId = (Integer) view.getTag();
            String pkgName =  mAppList.get(appId).packageName;
            String storedName = mPackageInfoUtil.getStoredName(mAppList.get(appId));
            if (mHolder.mLockSwitch.isChecked()) {
                mPackageInfoUtil.storePackageState(mContext, pkgName);
            } else {
                mPackageInfoUtil.removePackageState(mContext, pkgName);
                mPackageInfoUtil.removeFolderInfo(mContext, storedName);
            }

            HashMap<String, List<String>> relatedPackageMap = AppLockPolicy.getAppLockedRelatedPackageMap();
            if (relatedPackageMap.containsKey(pkgName)) {
                for (String relatedPackage : relatedPackageMap.get(pkgName)) {
                    if (mHolder.mLockSwitch.isChecked()) {
                        mPackageInfoUtil.storePackageState(mContext, relatedPackage);
                    } else {
                        mPackageInfoUtil.removePackageState(mContext, relatedPackage);
                    }
                }
            }

            mHandler.sendMessage(mHandler.obtainMessage(1));
        }
    }

    private class PackageListClickListener implements OnClickListener {
        private ViewHolder mHolder;

        public PackageListClickListener(int pos, ViewHolder holder) {
            mHolder = holder;
        }

        @Override
        public void onClick(View view) {
            mHolder.mLockSwitch.setChecked(!mHolder.mLockSwitch.isChecked());
        }
    }

    private class ViewHolder {
        TextView mFolderTextView;
        ImageView mImageView;
        Switch mLockSwitch;
        TextView mNameView;
    }

    public PackageListAdapter(Context context, Handler handler) {
        mContext = context;
        bindData();
        mHandler = handler;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mAppList.size();
    }

    @Override
    public Object getItem(int position) {
        return mAppList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.applock_package_list_items, parent, false);
            holder = buildViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        bindViewData(convertView, holder, position);
        setListener(convertView, holder, position);

        // rounded section
        int groupSize = getCount();
        if (groupSize == 1) {
            convertView.setBackgroundResource(DRAWABLEIDS[3]);
        } else if (position == groupSize - 1) {
            convertView.setBackgroundResource(DRAWABLEIDS[2]);
        } else if (position == 0) {
            convertView.setBackgroundResource(DRAWABLEIDS[0]);
        } else {
            convertView.setBackgroundResource(DRAWABLEIDS[1]);
        }

        return convertView;
    }

    private ViewHolder buildViewHolder(View convertView) {
        ViewHolder holder = new ViewHolder();
        holder.mNameView = (TextView) convertView.findViewById(R.id.list_tx_appname);
        holder.mFolderTextView = (TextView) convertView.findViewById(R.id.list_tx_summary);
        holder.mLockSwitch = (Switch) convertView.findViewById(R.id.list_btn_lock);
        holder.mImageView = (ImageView) convertView.findViewById(R.id.list_iv_icon);
        return holder;
    }

    public void bindData() {
        mAppList = mPackageInfoUtil.getAppList();
    }

    private void bindViewData(View convertView, ViewHolder holder, int position) {
        holder.mNameView.setText(mAppList.get(position).appName);
        holder.mImageView.setImageDrawable(mAppList.get(position).appIcon);
        bindLockedData(convertView, holder, position);
    }

    private void bindLockedData(View convertView, ViewHolder holder, int position) {
        Context context = mContext;
        mPackageInfoUtil.getClass();
        SharedPreferences sp = context.getSharedPreferences(
                PackageInfoUtil.PREF_PACKAGE_ONLY, 0);
        String pkgName = mAppList.get(position).packageName;
        String storedName = mPackageInfoUtil.getStoredName(mAppList.get(position));
        int locked = sp.getInt(pkgName, 0);
        holder.mLockSwitch.setOnCheckedChangeListener(null);
        if (locked == 1) {
            holder.mLockSwitch.setChecked(true);
            mAppList.get(position).locked = 1;
            if (mPackageInfoUtil.isFolderLocked(mContext, storedName)) {
                holder.mFolderTextView.setVisibility(View.VISIBLE);
                String folderInfo = mPackageInfoUtil.getFolderInfo(mContext, storedName);
                if (folderInfo.equals("")) {
                    holder.mFolderTextView.setText(mContext.getResources().getString(R.string.applock_locked_folder_default));
                } else {
                    holder.mFolderTextView.setText(mContext.getResources().getString(R.string.applock_locked_folder, new Object[]{folderInfo}));
                }
            } else {
                holder.mFolderTextView.setVisibility(View.GONE);
            }
        } else {
            holder.mLockSwitch.setChecked(false);
            mAppList.get(position).locked = 0;
            holder.mFolderTextView.setVisibility(View.GONE);
        }
        holder.mLockSwitch.setTag(position);
        holder.mFolderTextView.setTag(position);
        mMasterSwitch = mPackageInfoUtil.getMasterValue(mContext);
        holder.mNameView.setEnabled(mMasterSwitch);
        holder.mLockSwitch.setEnabled(mMasterSwitch);
        convertView.setEnabled(mMasterSwitch);
    }

    private void setListener(View convertView, ViewHolder holder, int position) {
        convertView.setOnClickListener(new PackageListClickListener(position, holder));
        holder.mLockSwitch.setOnCheckedChangeListener(new OnSwitchCheckedChangeListener(position, holder));
    }
}
