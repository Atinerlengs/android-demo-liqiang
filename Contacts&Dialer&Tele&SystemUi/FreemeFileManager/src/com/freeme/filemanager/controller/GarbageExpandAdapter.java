package com.freeme.filemanager.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.AsyncGarbageCleanupHelper.GarbageItem;
import com.freeme.filemanager.util.Util;

public class GarbageExpandAdapter extends BaseExpandableListAdapter implements
        CompoundButton.OnCheckedChangeListener {

    private Context mContext;

    private boolean mFinishState;
    private boolean mCleanupState;

    private String mNoFolderString = null;
    private String mNoGarbageString = null;

    private int[] mState;
    private long[] mGroupItemSize = null;
    private String[] mGroupItemName = null;

    private List<GarbageItem>[] mSelectedArray = null;
    private List<GarbageItem>[] mListData;
    private long[] mSelectedSize;

    private HashMap<Integer, Boolean> mGroupMark = new HashMap();

    private LayoutInflater mInflater;

    private OnUpdateButtonStateListener mListener;
    private Map<Integer, List<GarbageItem>> mChildMap;

    public GarbageExpandAdapter(Context context) {
        mContext = context;
        mInflater = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        mGroupItemName = context.getResources().getStringArray(R.array.group_item_names);

        mGroupItemSize = new long[mGroupItemName.length];
        mState = new int[mGroupItemName.length];

        mNoFolderString = mContext.getResources().getString(R.string.no_folder);
        mNoGarbageString = mContext.getResources().getString(R.string.no_garbage);

        int[] defaultState = context.getResources().getIntArray(R.array.clean_item_select_default);
        int stateSize = defaultState.length;

        for (int i = 0; i < mGroupItemSize.length; ++i) {
            if (i < stateSize) {
                mGroupMark.put(i, defaultState[i] == 1);
            } else {
                mGroupMark.put(i, true);
            }
        }
    }

    public GarbageExpandAdapter(Context context, String[] groupItemName, Map<Integer, List<GarbageItem>> listMap) {
        mContext = context;
        mInflater = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE));

        mGroupItemName = groupItemName;

        mGroupItemSize = new long[mGroupItemName.length];
        mState = new int[mGroupItemName.length];

        mNoFolderString = mContext.getResources().getString(R.string.no_folder);
        mNoGarbageString = mContext.getResources().getString(R.string.no_garbage);
        this.mChildMap = listMap;

        mSelectedArray = new ArrayList[mGroupItemName.length];
        mListData = new List[mGroupItemName.length];
        mSelectedSize = new long[mGroupItemName.length];

        for (int i = 0; i < mGroupItemName.length; i++) {
            mSelectedArray[i] = new ArrayList<GarbageItem>();
            mListData[i] = new ArrayList<GarbageItem>();
        }

        int[] defaultState = context.getResources().getIntArray(R.array.clean_item_select_default);
        int stateSize = defaultState.length;

        for (int i = 0; i < mGroupItemSize.length; ++i) {
            if (i < stateSize) {
                mGroupMark.put(i, defaultState[i] == 1);
            } else {
                mGroupMark.put(i, true);
            }
        }
    }

    @Override
    public GarbageItem getChild(int groupPosition, int childPosition) {
        return mChildMap.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup paramViewGroup) {
        GroupViewHolder groupViewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.layout_cleanup_item, null);
            groupViewHolder = findGroupView(convertView);
            convertView.setTag(groupViewHolder);
        } else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }
        groupViewHolder.mTitle.setText(mGroupItemName[groupPosition]);
        groupViewHolder.mCheckBox.setTag(groupPosition);
        groupViewHolder.mSizeInfo.setText(Util.convertStorage(mSelectedSize[groupPosition]));

        groupViewHolder.mCheckBox.setClickable(true);
        groupViewHolder.mCheckBox.setOnCheckedChangeListener(this);

        groupViewHolder.mIndicatorView.setBackgroundDrawable(mContext
                .getResources().getDrawable(isExpanded ? R.drawable.btn_open_background : R.drawable.btn_close_background));

        if (mGroupMark == null) {
            groupViewHolder.mCheckBox.setChecked(false);
        } else {
            groupViewHolder.mCheckBox.setChecked(mGroupMark.get(groupPosition));
        }

        if (mState[groupPosition] == 0) {
            groupViewHolder.mCheckBox.setVisibility(View.GONE);
            groupViewHolder.mProgressBar.setVisibility(View.VISIBLE);
        } else {
            if (mCleanupState) {
                groupViewHolder.mCheckBox.setClickable(false);
            }
            groupViewHolder.mCheckBox.setVisibility(View.VISIBLE);
            groupViewHolder.mProgressBar.setVisibility(View.GONE);
        }
        // if garbage clean up work done
        if (mFinishState) {
            groupViewHolder.mCheckBox.setVisibility(View.GONE);
            groupViewHolder.mProgressBar.setVisibility(View.GONE);
            if (groupViewHolder.mCheckBox.isChecked()) {
                groupViewHolder.mFinishView.setVisibility(View.VISIBLE);
            }
        }

        int groupSize = getGroupCount();
        if (groupSize == 1) {
            convertView.setBackgroundResource(R.drawable.listitem_backgroud_full_freeme);
        } else if (groupPosition == groupSize - 1) {
            if (!isExpanded) {
                convertView.setBackgroundResource(R.drawable.listitem_backgroud_tail_freeme);
            } else if (getChildrenCount(groupPosition) > 0) {
                convertView.setBackgroundResource(R.drawable.listitem_backgroud_middle_freeme);
            }
            convertView.setBackgroundResource(isExpanded ? R.drawable.listitem_backgroud_middle_freeme
                    : R.drawable.listitem_backgroud_tail_freeme);
        } else if (groupPosition == 0) {
            convertView.setBackgroundResource(R.drawable.listitem_backgroud_head_freeme);
        } else {
            convertView.setBackgroundResource(R.drawable.listitem_backgroud_middle_freeme);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean paramBoolean, View convertView, ViewGroup paramViewGroup) {
        if (mChildMap.get(groupPosition) == null) {
            return null;
        }

        ChildViewHolder childViewHolder;
        GarbageItem garbageItem = null;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.layout_expand_list_item, null);
            childViewHolder = findChildView(convertView);
        } else {
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }

        garbageItem = mChildMap.get(groupPosition).get(childPosition);
        childViewHolder.mCheckBox.setClickable(false);

        if (garbageItem != null) {
            if (mSelectedArray[groupPosition] != null && mSelectedArray[groupPosition].contains(garbageItem)) {
                childViewHolder.mCheckBox.setChecked(true);
            } else {
                childViewHolder.mCheckBox.setChecked(false);
            }
            childViewHolder.mAppName.setText(garbageItem.appName);
            childViewHolder.mAppIcon.setImageDrawable(garbageItem.appIcon);
            childViewHolder.mChildSummary.setText(garbageItem.childSummary);

            childViewHolder.mSizeInfo.setText(Util.convertStorage(garbageItem.size));
        }

        if (mState[groupPosition] == 0) {
            childViewHolder.mCheckBox.setVisibility(View.GONE);
            childViewHolder.mProgressBar.setVisibility(View.VISIBLE);
        }

        if (this.mFinishState) {
            childViewHolder.mCheckBox.setVisibility(View.GONE);
            childViewHolder.mProgressBar.setVisibility(View.GONE);
        }
        if ((groupPosition == getGroupCount() - 1)
                && (childPosition == getChildrenCount(groupPosition) - 1)) {
            convertView.setBackgroundResource(R.drawable.listitem_backgroud_tail_freeme);
        } else {
            convertView.setBackgroundResource(R.drawable.listitem_backgroud_middle_freeme);
        }
        return convertView;
    }

    /*
     * GroupItem checkbox click
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int groupPosition = (Integer) buttonView.getTag();
        if (isChecked) {
            if (mListData[groupPosition] != null && mListData[groupPosition].size() > 0) {
                for (int i = 0; i < mListData[groupPosition].size(); i++) {
                    if (!mSelectedArray[groupPosition].contains(mListData[groupPosition].get(i))) {
                        mSelectedSize[groupPosition] = mSelectedSize[groupPosition] + mListData[groupPosition].get(i).size;
                    }
                }
                mSelectedArray[groupPosition].clear();
                mSelectedArray[groupPosition].addAll(mListData[groupPosition]);
            }
        } else if (getChildSelectedItems(groupPosition).size() == getChildrenCount(groupPosition)) {
            getChildSelectedItems(groupPosition).clear();
            mSelectedSize[groupPosition] = 0;
        }

        long selectSize = 0;
        for (int j = 0; j < mSelectedSize.length; j++) {
            selectSize = selectSize + mSelectedSize[j];
        }

        if (mGroupMark != null) {
            boolean contained = mGroupMark.containsKey(groupPosition);
            if (contained) {
                mGroupMark.put(groupPosition, isChecked);
            }
        }
        mListener.onUpdate(selectSize);
        notifyDataSetChanged();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (mChildMap.get(groupPosition) != null) {
            return mChildMap.get(groupPosition).size();
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (mGroupItemName != null) {
            return mGroupItemName[groupPosition];
        }
        return null;
    }

    @Override
    public int getGroupCount() {
        if (mGroupItemName != null) {
            return mGroupItemName.length;
        }
        return 0;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    private ChildViewHolder findChildView(View paramView) {
        ChildViewHolder childViewHolder = new ChildViewHolder();
        childViewHolder.mAppIcon = (ImageView) paramView.findViewById(R.id.child_icon);
        childViewHolder.mAppName = ((TextView) paramView.findViewById(R.id.child_name));
        childViewHolder.mChildSummary = ((TextView) paramView.findViewById(R.id.child_summary));
        childViewHolder.mSizeInfo = ((TextView) paramView.findViewById(R.id.child_size));
        childViewHolder.mCheckBox = ((CheckBox) paramView.findViewById(R.id.child_picker));
        childViewHolder.mProgressBar = ((ProgressBar) paramView.findViewById(R.id.child_progress));
        childViewHolder.mCheckBox.setClickable(false);
        paramView.setTag(childViewHolder);
        return childViewHolder;
    }

    private GroupViewHolder findGroupView(View paramView) {
        GroupViewHolder groupViewHolder = new GroupViewHolder();
        groupViewHolder.mIndicatorView = ((ImageView) paramView.findViewById(R.id.group_image));
        groupViewHolder.mTitle = ((TextView) paramView.findViewById(R.id.group_name));
        groupViewHolder.mSizeInfo = ((TextView) paramView.findViewById(R.id.group_size));
        groupViewHolder.mCheckBox = ((CheckBox) paramView.findViewById(R.id.group_picker));
        groupViewHolder.mProgressBar = ((ProgressBar) paramView.findViewById(R.id.group_progress));
        groupViewHolder.mFinishView = ((ImageView) paramView.findViewById(R.id.finish_view));
        paramView.setTag(groupViewHolder);
        return groupViewHolder;
    }

    public void cancelAllMark() {
        for (int i =0; i < mGroupItemName.length; i++) {
            mSelectedArray[i].clear();
        }
        if (mGroupMark != null) {
            for (int i = 0; i < mGroupItemSize.length; i++) {
                mGroupMark.put(i, false);
            }
        }
        notifyDataSetChanged();
    }

    public int getChildMarkItem() {
        for (int i =0; i < mGroupItemName.length; i++) {
            if (mSelectedArray[i] != null && mSelectedArray[i].size() > 0) {
                return mSelectedArray[i].size();
            }
        }
        return 0;
    }

    public List<GarbageItem> getChildSelectedItems(int groupPosition) {
        if (mGroupMark != null
            && mGroupMark.containsKey(groupPosition)
            && mGroupMark.get(groupPosition)
            && mListData[groupPosition] != null
            && mListData[groupPosition].size() > 0) {

            mSelectedArray[groupPosition].clear();
            mSelectedArray[groupPosition].addAll(mListData[groupPosition]);
        }

        return mSelectedArray[groupPosition];
    }

    public int getGroupItemProgressState(int paramInt) {
        return mState[paramInt];
    }

    public void markGroupItem(int groupPosition) {
        if (mGroupMark == null) {
            return;
        }
        if (mGroupMark.containsKey(groupPosition)) {
            boolean checked = mGroupMark.get(groupPosition);
            mGroupMark.put(groupPosition, !checked);
        }
        mListener.onUpdate();
        notifyDataSetChanged();
    }

    public ArrayList getGroupMarkItem() {
        ArrayList arrayList = new ArrayList();
        if (mGroupMark != null) {
            for (Entry<Integer, Boolean> entry : mGroupMark.entrySet()) {
                if (entry.getValue() == true) {
                    arrayList.add(entry.getKey());
                }
            }
        }
        return arrayList;
    }

    public boolean hasStableIds() {
        return false;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    //child item click
    public long markChildItem(int groupPosition, int childPosition) {
        if ((mGroupMark == null)
           || (groupPosition >= mGroupItemSize.length)
           || (mChildMap.get(groupPosition) == null)
           || (childPosition >= mChildMap.get(groupPosition).size())) {
            return 0;
        }

        long swapSize;
        GarbageItem garbageItem = (GarbageItem) mChildMap.get(groupPosition).get(childPosition);
        if (mSelectedArray[groupPosition].contains(garbageItem)) {
            int location = mSelectedArray[groupPosition].indexOf(garbageItem);
            mSelectedArray[groupPosition].remove(location);
            swapSize = - garbageItem.size;
        } else {
            mSelectedArray[groupPosition].add(garbageItem);
            swapSize = garbageItem.size;
        }
        mSelectedSize[groupPosition] = mSelectedSize[groupPosition] + swapSize;

        if (mGroupMark != null) {
            if (mSelectedArray[groupPosition].size() == mChildMap.get(groupPosition).size()) {
                mGroupMark.put(groupPosition, true);
            } else {
                mGroupMark.put(groupPosition, false);
            }
        }

        notifyDataSetChanged();
        return swapSize;
    }

    public void setAllGroupItemProgress(int paramInt) {
        for (int i = 0; i < mGroupItemName.length; ++i) {
            mState[i] = paramInt;
        }
        notifyDataSetChanged();
    }

    public void setChildData(Map<Integer, List<GarbageItem>> mChildMap) {
        this.mChildMap = mChildMap;
        for (int i = 0; i < mGroupItemName.length; i++) {
            this.mListData[i] = mChildMap.get(i);
            this.mSelectedArray[i].clear();
            if (mListData[i] != null && mListData[i].size() > 0) {
                for (GarbageItem garbageItem : mListData[i]) {
                    this.mSelectedArray[i].add(garbageItem);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setFinishState(boolean paramBoolean) {
        mFinishState = paramBoolean;
        notifyDataSetChanged();
    }

    public void setCleanupState(boolean paramBoolean) {
        mCleanupState = paramBoolean;
        notifyDataSetChanged();
    }

    public void setGroupData(int position, long size) {
        if (position >= mGroupItemSize.length) {
            return;
        }
        mGroupItemSize[position] = size;
        mSelectedSize[position] = size;
        notifyDataSetChanged();
    }

    public void setGroupItemProgress(int position, int progress) {
        mState[position] = progress;
    }

    public void setOnUpdateButtonStateListener(OnUpdateButtonStateListener onUpdateButtonStateListener) {
        this.mListener = onUpdateButtonStateListener;
    }

    public interface OnUpdateButtonStateListener {
        void onUpdate();
        void onUpdate(long selectSize);
    }

    private class ChildViewHolder {
        ProgressBar mProgressBar;
        CheckBox mCheckBox;
        TextView mSizeInfo;
        TextView mAppName;
        TextView mChildSummary;
        ImageView mAppIcon;

        private ChildViewHolder() {
        }
    }

    private class GroupViewHolder {
        ProgressBar mProgressBar;
        CheckBox mCheckBox;
        ImageView mFinishView;
        ImageView mIndicatorView;
        TextView mSizeInfo;
        TextView mTitle;

        private GroupViewHolder() {
        }
    }
}