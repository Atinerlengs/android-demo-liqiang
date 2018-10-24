package com.freeme.safe.view;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.utils.SafeConstants;

public class EncryptionFileBrowserActivity extends BaseActivity {

    private int mCurrentFileType;
    private EncryptionFileBrowserFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_file_browser);
        initView();
        createFragment();
    }

    private void initView() {
        mCurrentFileType = getIntent().getIntExtra(SafeConstants.SELECTED_SAFE_FILE_TYPE, -1);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            Util.setBackTitle(actionBar, getString(R.string.encryption_file));
            int titleId = 0;
            switch (mCurrentFileType) {
                case MediaFile.AUDIO_TYPE:
                    titleId = R.string.category_music;
                    break;
                case MediaFile.VIDEO_TYPE:
                    titleId = R.string.category_video;
                    break;
                case MediaFile.IMAGE_TYPE:
                    titleId = R.string.category_picture;
                    break;
                case MediaFile.DOC_TYPE:
                    titleId = R.string.category_document;
                    break;
                case MediaFile.OTHER_TYPE:
                    titleId = R.string.category_other;
                    break;
            }
            if (titleId != 0) {
                actionBar.setTitle(titleId);
            }
        }
    }

    private void createFragment() {
        if (mFragment == null) {
            mFragment = new EncryptionFileBrowserFragment();
        }
        Bundle bundle = new Bundle();
        bundle.putInt(SafeConstants.SELECTED_SAFE_FILE_TYPE, mCurrentFileType);
        mFragment.setArguments(bundle);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.main_frame, mFragment);
        ft.show(mFragment);
        ft.commit();
    }
}