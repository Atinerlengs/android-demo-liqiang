package com.freeme.filemanager.fragment.cleanup;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.cleanup.CleanupMainActivity;
import com.freeme.filemanager.activity.cleanup.GarbageCleaningActivity;
import com.freeme.filemanager.controller.GarbageExpandAdapter;
import com.freeme.filemanager.fragment.BaseFragment;
import com.freeme.filemanager.util.AsyncGarbageCleanupHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.garbage.CleanListItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.freeme.filemanager.FMIntent.EXTRA_CHOSE_CLEAN_LIST;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_CLEAN;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TOTAL;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_USE;

public class SuggestCleanFragment extends BaseFragment implements
        ExpandableListView.OnGroupClickListener,
        ExpandableListView.OnChildClickListener,
        AsyncGarbageCleanupHelper.GarbageCleanupStatesListener {

    private View mRootView;
    private CleanupMainActivity mActivity;

    private Handler mHandler;
    private AsyncGarbageCleanupHelper mAsyncGarbageCleanupHelper;
    private GarbageExpandAdapter mAdapter;

    private ExpandableListView mExpandableListView;
    private ProgressBar mUsedProgress;
    private Button mCleanUpBtn;
    private TextView mCleanableView;
    private TextView mCleanableSelect;
    private TextView mScanPathTxt;

    private Map<Integer, List<AsyncGarbageCleanupHelper.GarbageItem>> mChildData;
    private long[] mGroupCleanableSize;
    private String[] mGroupItemName;
    private int[] mDefaultState;
    private boolean mCleanupState;
    private boolean mFinishState;
    private long mCleanableSize;
    private long mSelectedSize;
    private long mTotalSize;
    private long mUsedSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_garbage_cleanup, null,
                false);
        mActivity = (CleanupMainActivity)getActivity();
        mCleanableView = (TextView)mRootView.findViewById(R.id.cleanable_size_txt);
        mCleanableSelect = (TextView)mRootView.findViewById(R.id.cleanable_select_txt);
        mUsedProgress = (ProgressBar)mRootView.findViewById(R.id.used_progress);
        mScanPathTxt = (TextView)mRootView.findViewById(R.id.scan_path_txt);

        initView();

        mHandler = new Handler(mActivity.getMainLooper());
        mAsyncGarbageCleanupHelper = new AsyncGarbageCleanupHelper(mActivity);

        mGroupItemName = getResources().getStringArray(R.array.group_item_names);
        mGroupCleanableSize = new long[mGroupItemName.length];
        mChildData = new HashMap<Integer, List<AsyncGarbageCleanupHelper.GarbageItem>>();

        mAdapter = new GarbageExpandAdapter(mActivity, mGroupItemName, mChildData);
        mAdapter.setOnUpdateButtonStateListener(new GarbageExpandAdapter.OnUpdateButtonStateListener() {
            @Override
            public void onUpdate() {
                updateButtonState();
            }

            public void onUpdate(long selectsize) {
                mSelectedSize = selectsize;
                updateButtonState();
            }
        });
        mExpandableListView = (ExpandableListView)mRootView.findViewById(R.id.expande_list);
        mExpandableListView.setGroupIndicator(null);
        mExpandableListView.setOnGroupClickListener(this);
        mExpandableListView.setOnChildClickListener(this);
        mExpandableListView.setAdapter(mAdapter);
        mExpandableListView.setSelected(false);

        mCleanUpBtn = (Button)mRootView.findViewById(R.id.cleanup_button);
        mCleanUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                peformClick();
                mActivity.finish();
            }
        });

        // first enter auto perform click
        peformClick();
        return mRootView;
    }

    @Override
    public void onDestroyView(){
        mAsyncGarbageCleanupHelper.destroyAysntask();
        super.onDestroyView();
    }

    @Override
    public boolean onGroupClick(ExpandableListView expandableListView,
                                View view, int groupPosition, long id) {
        if ((mAdapter.getGroupItemProgressState(groupPosition) == 0) || (mFinishState)) {
            return false;
        }
        /*if (groupPosition < mAdapter.getGroupCount() - 1) {
            mAdapter.markGroupItem(groupPosition);
            updateButtonState();
            return true;
        }*/
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView paramExpandableListView,
                                View paramView,
                                int groupPosition,
                                int childPosition,
                                long id) {

        if ((mAdapter.getGroupItemProgressState(groupPosition) == 0) || (mFinishState)) {
            return false;
        }
        long changeSize = mAdapter.markChildItem(groupPosition, childPosition);
        mSelectedSize = mSelectedSize + changeSize;
        updateButtonState();
        return true;
    }

    @Override
    public void onScanGarbageFinish(final Map<Integer, List<AsyncGarbageCleanupHelper.GarbageItem>> mChildMap) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.setChildData(mChildMap);

            }
        });
    }

    @Override
    public void onFinish(final int position, final long fileSize, final int fileCount) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.setGroupItemProgress(position, 8);
                mAdapter.setGroupData(position, fileSize);
                mGroupCleanableSize[position] = fileSize;
            }
        });
    }

    @Override
    public void onUpdateUI(final int state) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    return;
                }
                switch (state) {
                    case AsyncGarbageCleanupHelper.STATE_START_SCAN:
                        mCleanUpBtn.setEnabled(false);
                        mExpandableListView.setEnabled(false);
                        mCleanUpBtn.setText(getResources().getString(R.string.garbage_scanning));
                        mScanPathTxt.setText(getResources().getString(R.string.garbage_scanning));
                        mAdapter.setAllGroupItemProgress(0);
                        break;

                    case AsyncGarbageCleanupHelper.STATE_SCAN_FINISH:
                        mCleanUpBtn.setEnabled(true);
                        mExpandableListView.setEnabled(true);
                        mScanPathTxt.setVisibility(View.GONE);
                        mCleanUpBtn.setText(getResources().getString(R.string.start_clean));
                        int stateSize = mDefaultState.length;
                        for (int i = 0; i < mGroupItemName.length; i++) {
                            mCleanableSize = mCleanableSize + mGroupCleanableSize[i];
                            if (i < stateSize && mDefaultState[i] == 1) {
                                mSelectedSize = mSelectedSize + mGroupCleanableSize[i];
                            }
                        }
                        mCleanableView.setText(Util.convertStorage(mCleanableSize));
                        mCleanableSelect.setText(getString(R.string.trashes_selected, Util.convertStorage(mSelectedSize)));

                        setProgress(mUsedProgress, mUsedSize, mCleanableSize);
                        break;

                    case AsyncGarbageCleanupHelper.STATE_START_CLEANUP:
                        mCleanUpBtn.setEnabled(false);
                        mExpandableListView.setEnabled(false);
                        mCleanUpBtn.setText(getResources().getString(R.string.garbage_cleaning));
                        ArrayList localArrayList = mAdapter.getGroupMarkItem();
                        if (mAdapter != null) {
                            mCleanupState = true;
                            mAdapter.setCleanupState(mCleanupState);
                        }
                        for (int i = 0; i < localArrayList.size(); i++) {
                            mAdapter.setGroupItemProgress((Integer) localArrayList.get(i), 0);
                            mAdapter.notifyDataSetChanged();
                        }
                        break;

                    case AsyncGarbageCleanupHelper.STATE_CLEANUP_FINISH:
                        if (mAsyncGarbageCleanupHelper != null) {
                            long cleanSize = mAsyncGarbageCleanupHelper.getTotalDeletedFileSize();
                            if (cleanSize > 0) {
                                String emptyDir = Util.convertStorage(mAsyncGarbageCleanupHelper.getTotalDeletedFileSize());
                                if (emptyDir.equals("64.0 KB") || emptyDir.equals("32.0 KB")) {
                                    Toast.makeText(mActivity, getResources().getString(R.string.no_garbage_result), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(mActivity, getResources().getString(R.string.garbage_clean_result)
                                                    + Util.convertStorage(mAsyncGarbageCleanupHelper.getTotalDeletedFileSize()),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(mActivity, getResources().getString(R.string.no_garbage_result), Toast.LENGTH_SHORT).show();
                            }
                            mAsyncGarbageCleanupHelper.stopRunning();
                            mAsyncGarbageCleanupHelper.resetDeletedParam();
                        }
                        mCleanUpBtn.setEnabled(true);
                        mExpandableListView.setEnabled(true);
                        mCleanUpBtn.setText(getResources().getString(R.string.garbage_clean_finish));
                        if (mAdapter != null) {
                            mFinishState = true;
                            mAdapter.setFinishState(mFinishState);
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void updateCleanProgress(int progress) {

    }

    private void initView() {
        TextView usedTxt = (TextView)mRootView.findViewById(R.id.storage_txt0);
        TextView totalTxt = (TextView)mRootView.findViewById(R.id.storage_txt1);
        Intent intent = mActivity.getIntent();
        mUsedSize = intent.getLongExtra(EXTRA_STORAGE_USE, 0);
        mTotalSize = intent.getLongExtra(EXTRA_STORAGE_TOTAL, 0);
        long cleanSize = intent.getLongExtra(EXTRA_STORAGE_CLEAN, 0);
        String cleanString = Util.convertStorage(cleanSize);
        mCleanableView.setText(cleanString);
        mCleanableSelect.setText(getString(R.string.trashes_selected, cleanString));
        usedTxt.setText(Util.convertStorage(mUsedSize));
        totalTxt.setText(Util.convertStorage(mTotalSize));

        mDefaultState = getResources().getIntArray(R.array.clean_item_select_default);

        setProgress(mUsedProgress, mUsedSize, cleanSize);
    }

    private void setProgress(ProgressBar progressBar, long usedSize, long cleanSize) {
        if (mTotalSize == 0) {
            progressBar.setSecondaryProgress(0);
            progressBar.setProgress(0);
            return;
        }

        int p = (int)(usedSize * 100 / mTotalSize);
        int sp = (int)(cleanSize * 100 / mTotalSize);
        if (sp == 0 && cleanSize > 0) {
            sp = 1;
        }
        progressBar.setSecondaryProgress(p);
        progressBar.setProgress(p - sp);
    }

    private void peformClick() {
        if (mAsyncGarbageCleanupHelper == null
                || mAsyncGarbageCleanupHelper.getState() == AsyncGarbageCleanupHelper.STATE_START_SCAN) {
            return;
        }

        ArrayList groupMarkList = new ArrayList(mAdapter.getGroupMarkItem());

        if (groupMarkList != null && !groupMarkList.isEmpty()) {
            mAsyncGarbageCleanupHelper.setActionOperate(groupMarkList);
        }

        List<AsyncGarbageCleanupHelper.GarbageItem>[] mListCleanupItem = new ArrayList[mGroupItemName.length];

        for (int i = 0; i < mGroupItemName.length; i++) {
            List<AsyncGarbageCleanupHelper.GarbageItem> itemList = mAdapter.getChildSelectedItems(i);
            mListCleanupItem[i] = itemList;
        }

        mAsyncGarbageCleanupHelper.setGarbageCleanupItem(mListCleanupItem);

        mAsyncGarbageCleanupHelper.setGarbageCleanupStatesListener(this);

        if (mCleanUpBtn.getText().equals(getResources().getString(R.string.start_clean))) {
            Intent intent = new Intent(mActivity, GarbageCleaningActivity.class);
            List<CleanListItem> checkList = new ArrayList<CleanListItem>();
            for(int i = 0; i < mGroupItemName.length; i++) {
                int GroupItemCnt = mListCleanupItem[i].size();
                List<AsyncGarbageCleanupHelper.GarbageItem> GroupItemList = mListCleanupItem[i];
                for (int j = 0; j < GroupItemCnt; j++) {
                    AsyncGarbageCleanupHelper.GarbageItem garbageItem = GroupItemList.get(j);
                    CleanListItem item0 = new CleanListItem(garbageItem, i);
                    checkList.add(item0);
                }
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable(EXTRA_CHOSE_CLEAN_LIST, (Serializable)checkList);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        mAsyncGarbageCleanupHelper.cleanUp();
    }

    private void updateButtonState() {
        mCleanableSelect.setText(getString(R.string.trashes_selected, Util.convertStorage(mSelectedSize)));
        if (this.mAdapter.getChildMarkItem() > 0) {
            if (!mCleanUpBtn.getText().equals(getResources().getString(R.string.garbage_scanning))) {
                mCleanUpBtn.setEnabled(true);
            }
            return;
        }
        mCleanUpBtn.setEnabled(false);
    }
}