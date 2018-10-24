package com.freeme.filemanager.activity.cleanup.largefiles;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.controller.FileListCursorAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.Util;

import java.util.Collection;

import static com.freeme.filemanager.FMIntent.EXTRA_LARGE_FILES_TYPE;

public class LarFilesPreviewActivity extends BaseActivity implements IFileInteractionListener {

    private static final String ROOT_DIR = "/mnt";

    private Context mContext;
    private FileViewInteractionHub mFileViewInteractionHub;
    private FileCategoryHelper mFileCategoryHelper;
    private FileIconHelper mFileIconHelper;
    private FileListCursorAdapter mAdapter;

    private LinearLayout mEmptyView;
    private TextView mEmptyTv;
    private Button mCleanUpBtn;
    private FileCategory mCategory;
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_wechat_special_preview);

        mContext = this;
        mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_DEFAULT, null);
        mFileViewInteractionHub.setMode(FileViewInteractionHub.Mode.Pick);
        mFileIconHelper = new FileIconHelper(this);
        mFileCategoryHelper = new FileCategoryHelper(this);
        init();

        ListView previewListView = findViewById(R.id.file_path_list);
        mAdapter = new FileListCursorAdapter(mContext, null,
                mFileViewInteractionHub, mFileIconHelper);
        previewListView.setAdapter(mAdapter);

        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public View getViewById(int id) {
        return this.findViewById(id);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public FragmentManager getFragmentM() {
        return null;
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                showEmptyView(mAdapter.getCount() == 0, mCategory);
            }
        });
    }

    @Override
    public void onPick(FileInfo f) {
    }

    @Override
    public boolean shouldShowOperationPane() {
        return true;
    }

    @Override
    public boolean onOperation(int id) {
        return false;
    }

    @Override
    public String getDisplayPath(String path) {
        return this.getString(R.string.tab_category)
                + path.substring(ROOT_DIR.length());
    }

    @Override
    public String getRealPath(String displayPath) {
        if (!TextUtils.isEmpty(displayPath)) {
            if (displayPath.equals(this.getString(R.string.tab_category))) {
                return ROOT_DIR;
            }
        }
        return displayPath;
    }

    @Override
    public boolean shouldHideMenu(int menu) {
        return false;
    }

    @Override
    public void showPathGalleryNavbar(boolean show) {

    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    @Override
    public FileInfo getItem(int pos) {
        return mAdapter.getFileItem(pos);
    }

    @Override
    public void sortCurrentList(final FileSortHelper sort) {
        refreshList();
    }

    @Override
    public Collection<FileInfo> getAllFiles() {
        return mAdapter.getAllFiles();
    }

    @Override
    public void addSingleFile(final FileInfo file) {
        refreshList();
    }

    @Override
    public void deleteSingleFile(final FileInfo file) {
        refreshList();
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        RefreshFileListTask refreshFileListTask = new RefreshFileListTask(mCategory);
        refreshFileListTask.execute();
        return true;
    }

    @Override
    public void onRefreshMenu(boolean visible) {

    }

    @Override
    public int getItemCount() {
        return mAdapter.getCount();
    }

    @Override
    public void hideVolumesList() {

    }

    private void init() {
        mFileViewInteractionHub.mCurrentPath = Util.getDefaultPath();
        mEmptyView = findViewById(R.id.empty_view);
        mEmptyTv = findViewById(R.id.empty_tv);
        mCleanUpBtn = findViewById(R.id.cleanup_button);
        Intent intent = this.getIntent();
        mCategory = (FileCategory)intent.getSerializableExtra(EXTRA_LARGE_FILES_TYPE);

        showEmptyView(false, mCategory);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            Util.setBackTitle(actionBar, getString(R.string.clear_large_files));
            switch (mCategory) {
                case Video:
                    actionBar.setTitle(R.string.category_video);
                    break;
                case Picture:
                    actionBar.setTitle(R.string.category_picture);
                    break;
                case Music:
                    actionBar.setTitle(R.string.category_music);
                    break;
                case Doc:
                    actionBar.setTitle(R.string.category_document);
                    break;
                case Apk:
                    actionBar.setTitle(R.string.category_apk);
                    break;
                default:
                    actionBar.setTitle(R.string.category_other);
                    break;
            }
        }
    }

    private void refreshList() {
        mFileViewInteractionHub.refreshFileList();
    }

    private void showEmptyView(boolean show, FileCategory fc) {
        if (show) {
            mEmptyTv.setText(R.string.no_file);
            mEmptyView.setVisibility(View.VISIBLE);
            mCleanUpBtn.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mCleanUpBtn.setVisibility(View.VISIBLE);
        }
    }

    private class RefreshFileListTask extends AsyncTask<Void, Void, Cursor> {
        private FileCategory mCurCategory;

        private RefreshFileListTask(FileCategory curCategory) {
            mCurCategory = curCategory;
        }

        @Override
        protected void onPreExecute() {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(mContext);
                mProgressDialog.setTitle(getString(R.string.operation_load));
                mProgressDialog.setMessage(getString(R.string.operation_loading));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setCancelable(false);
            }
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            Cursor cursor;
            cursor = mFileCategoryHelper.query(mCurCategory, mContext.getResources().getInteger(R.integer.large_file_minimum_size));
            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor != null) {
                showEmptyView(cursor.getCount() == 0, mCurCategory);
                mAdapter.changeCursor(cursor);
            }

            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            mFileViewInteractionHub.setRefresh(true);
        }
    }
}
