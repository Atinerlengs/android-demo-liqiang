package com.freeme.filemanager.activity.cleanup.largefiles;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.ListItemTextView;

import static com.freeme.filemanager.FMIntent.EXTRA_BACK_TITLE;
import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_TAG;
import static com.freeme.filemanager.FMIntent.EXTRA_LARGE_FILES_TYPE;

public class LargeFilesActivity extends BaseActivity implements View.OnClickListener {

    private Context mContext;
    private FileCategoryHelper mFileCagetoryHelper;
    private FileCategory mCategory;

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_large_files);
        mContext = this;
        mFileCagetoryHelper = new FileCategoryHelper(this);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = this.getIntent();
        String backTitle = intent != null ? intent.getStringExtra(EXTRA_BACK_TITLE) : null;
        if (backTitle != null) {
            Util.setBackTitle(actionBar, backTitle);
        } else {
            Util.setBackTitle(actionBar, getString(R.string.deep_clean));
        }

        ((ListItemTextView) findViewById(R.id.large_video)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.large_image)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.large_music)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.large_doc)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.large_apk)).setOnClickListener(this);
        ((ListItemTextView) findViewById(R.id.large_other)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = null;
        Bundle bundle = new Bundle();
        switch (view.getId()) {
            case R.id.large_video: {
                intent = new Intent(mContext, LarFilesPreviewActivity.class);
                mCategory = FileCategory.Video;
            }
                break;
            case R.id.large_image:
                intent = new Intent(mContext, LarFilesPreviewActivity.class);
                mCategory = FileCategory.Picture;
                break;
            case R.id.large_music:
                intent = new Intent(mContext, LarFilesPreviewActivity.class);
                mCategory = FileCategory.Music;
                break;
            case R.id.large_doc:
                intent = new Intent(mContext, LarFilesPreviewActivity.class);
                mCategory = FileCategory.Doc;
                break;
            case R.id.large_apk:
                intent = new Intent(mContext, LarFilesPreviewActivity.class);
                mCategory = FileCategory.Apk;
                break;
            case R.id.large_other:
                intent = new Intent(mContext, LarFilesPreviewActivity.class);
                mCategory = FileCategory.Other;
                break;
            default:
                break;
        }
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            bundle.putSerializable(EXTRA_LARGE_FILES_TYPE, mCategory);
            //intent.putExtra(EXTRA_LARGE_FILES_TYPE, mCategory.toString());
            intent.putExtras(bundle);
            mContext.startActivity(intent);
        }
    }
}
