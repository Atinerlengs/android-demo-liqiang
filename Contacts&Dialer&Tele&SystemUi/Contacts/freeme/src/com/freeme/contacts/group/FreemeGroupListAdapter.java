package com.freeme.contacts.group;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.group.GroupListItem;
import com.android.contacts.R;

import java.util.ArrayList;
import java.util.List;

class FreemeGroupListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private OnGroupListItemClickListener mListener;
    private OnGroupListItemLongClickListener mLongListener;
    private List<GroupListItem> mGroupListItems = new ArrayList<>();

    FreemeGroupListAdapter(FreemeGroupListFragment fragment) {
        mContext = fragment.getContext();
        if (fragment instanceof OnGroupListItemClickListener) {
            mListener = fragment;
        }
        if (fragment instanceof OnGroupListItemLongClickListener) {
            mLongListener = fragment;
        }

    }

    public void setGroupListItems(List<GroupListItem> groupListItems) {
        mGroupListItems.clear();
        mGroupListItems.addAll(groupListItems);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GroupListViewHolder(LayoutInflater.from(mContext).inflate(
                R.layout.freeme_group_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        GroupListViewHolder groupListViewHolder = (GroupListViewHolder) holder;
        groupListViewHolder.mGroupCheckBox.setVisibility(mDispalyCheckBox ? View.VISIBLE : View.GONE);
        groupListViewHolder.mGroupCheckBox.setChecked(groupItemSelectCounts.contains(getItem(position).getGroupId()));
        groupListViewHolder.mGroupListRightIcon.setVisibility(!mDispalyCheckBox ? View.VISIBLE : View.GONE);
        groupListViewHolder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mGroupListItems.size();
    }

    public GroupListItem getItem(int position) {
        if (mGroupListItems == null || position >= mGroupListItems.size()) {
            return null;
        }
        return mGroupListItems.get(position);
    }

    public boolean mDispalyCheckBox;
    public boolean ismDispalyCheckBox() {
        return mDispalyCheckBox;
    }

    public void setmDispalyCheckBox(boolean mDispalyCheckBox) {
        this.mDispalyCheckBox = mDispalyCheckBox;
    }

    private List<Long> groupItemSelectCounts = new ArrayList<>();

    public void setGroupItemCheck(List<Long> groupItemSelect) {
        groupItemSelectCounts = groupItemSelect;
    }

    class GroupListViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private TextView mGroupName;
        private TextView mGroupMemberCount;
        private LinearLayout mGroupListItem;
        private int mPosition;
        public CheckBox mGroupCheckBox;
        public ImageView mGroupListRightIcon;

        public GroupListViewHolder(View view) {
            super(view);
            mGroupName = view.findViewById(R.id.group_name);
            mGroupMemberCount = view.findViewById(R.id.group_member_count);
            mGroupListItem = view.findViewById(R.id.group_list_item);
            mGroupListRightIcon = view.findViewById(R.id.group_list_item_right_icon);
            mGroupCheckBox = view.findViewById(R.id.group_list_item_checkbox);
            mGroupListItem.setOnClickListener(this);
            mGroupListItem.setOnLongClickListener(this);
        }

        public void bind(int position) {
            mPosition = position;
            GroupListItem groupItem = getItem(position);
            String name = groupItem.getTitle();
            mGroupName.setText(name);
            mGroupMemberCount.setText(mContext.getString(
                    R.string.freeme_group_member_count, groupItem.getMemberCount()));
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onGroupItemClick(getItem(mPosition).getGroupId());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mLongListener != null) {
                mLongListener.onGroupItemLongClick(getItem(mPosition).getGroupId());
            }
            mDispalyCheckBox = true;
            notifyDataSetChanged();
            return true;
        }
    }
}
