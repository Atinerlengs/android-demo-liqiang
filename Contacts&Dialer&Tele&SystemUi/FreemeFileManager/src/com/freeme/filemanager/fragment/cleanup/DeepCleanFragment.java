package com.freeme.filemanager.fragment.cleanup;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.activity.cleanup.CleanupMainActivity;
import com.freeme.filemanager.activity.cleanup.GarbageCleaningActivity;
import com.freeme.filemanager.activity.cleanup.largefiles.LargeFilesActivity;
import com.freeme.filemanager.activity.cleanup.special.WeChatSpecialActivity;
import com.freeme.filemanager.fragment.BaseFragment;
import com.freeme.filemanager.util.CleanupUtil;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.ListItemTextView;

import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TOTAL;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_USE;

public class DeepCleanFragment extends BaseFragment {

    private View mRootView;
    private BaseActivity mActivity;

    private long mTotalSize;
    private long mFreeSize;
    private long mUsedSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_garbage_deep_clean, null,
                false);

        if (getArguments() != null) {
            mActivity = (GarbageCleaningActivity)getActivity();
            mActivity.getActionBar().setTitle(R.string.deep_clean);
        } else {
            mActivity = (CleanupMainActivity)getActivity();
        }
        ListItemTextView weChatSpecial = (ListItemTextView)mRootView.findViewById(R.id.wechat_special);
        weChatSpecial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new  Intent(mActivity, WeChatSpecialActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        ListItemTextView clearLargeFiles = (ListItemTextView)mRootView.findViewById(R.id.clear_large_files);
        clearLargeFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new  Intent(mActivity, LargeFilesActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        initView();
        return mRootView;
    }

    private void initView() {
        TextView usedPercentTv = (TextView)mRootView.findViewById(R.id.used_percent_txt);
        TextView sufficientTv = (TextView)mRootView.findViewById(R.id.memory_is_sufficient);
        TextView storageFreeTv = (TextView)mRootView.findViewById(R.id.storage_free);
        ProgressBar progressBar = (ProgressBar)mRootView.findViewById(R.id.used_progress);

        Intent intent = mActivity.getIntent();
        mUsedSize = intent.getLongExtra(EXTRA_STORAGE_USE, 0);
        mTotalSize = intent.getLongExtra(EXTRA_STORAGE_TOTAL, 0);

        if (mTotalSize == 0) {
            refreshStorageDeviceInfo();
        }

        int usedPercent = (int) (mTotalSize == 0 ? 0 : mUsedSize * 100 / mTotalSize);
        usedPercentTv.setText(getString(R.string.storage_used_percent, usedPercent));
        if (usedPercent < CleanupUtil.MEMORY_THRESHOLD) {
            sufficientTv.setText(R.string.sufficient_memory);
        } else {
            sufficientTv.setText(R.string.insufficient_memory);
        }
        progressBar.setProgress(usedPercent);
        storageFreeTv.setText(getString(R.string.storage_free, Util.convertStorage(mTotalSize - mUsedSize)));
    }

    private void refreshStorageDeviceInfo() {
        final Util.MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        if (memoryCardInfo != null) {
            mTotalSize = mTotalSize + memoryCardInfo.total;
            mFreeSize = mFreeSize + memoryCardInfo.free;
        }

        final Util.SDCardInfo sdCardInfo = Util.getSDCardInfo();
        if (sdCardInfo != null && sdCardInfo.total != sdCardInfo.free) {
            mTotalSize = mTotalSize + sdCardInfo.total;
            mFreeSize = mFreeSize + sdCardInfo.free;
        }

        final Util.UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();
        if (usbStorageInfo != null && usbStorageInfo.total != usbStorageInfo.free) {
            mTotalSize = mTotalSize + usbStorageInfo.total;
            mFreeSize = mFreeSize + usbStorageInfo.free;
        }
        mUsedSize = mTotalSize - mFreeSize;
    }
}