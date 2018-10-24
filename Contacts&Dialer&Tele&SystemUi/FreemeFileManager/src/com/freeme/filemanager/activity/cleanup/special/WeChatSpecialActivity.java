package com.freeme.filemanager.activity.cleanup.special;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.CleanupUtil;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.ListItemTextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.freeme.filemanager.FMIntent.EXTRA_BACK_TITLE;
import static com.freeme.filemanager.FMIntent.EXTRA_WECHAT_TRASH_TYPE;

public class WeChatSpecialActivity extends BaseActivity implements View.OnClickListener {

    private Context mContext;
    private final int SECS_TO_MONS = 60 * 60 * 24 * 30;
    private List<String> mPathFilterCache = new ArrayList<>();
    private List<String> mPathFilterOlds = new ArrayList<>();
    private ListItemTextView mTempCache;
    private ListItemTextView mOldFiles;

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_wechat_special);
        mContext = this;
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = this.getIntent();
        String backTitle = intent != null ? intent.getStringExtra(EXTRA_BACK_TITLE) : null;
        if (backTitle != null) {
            Util.setBackTitle(actionBar, backTitle);
        } else {
            Util.setBackTitle(actionBar, getString(R.string.deep_clean));
        }

        mTempCache = (ListItemTextView) findViewById(R.id.temp_cache);
        mTempCache.setRightTvListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTempCache.setRightTvVisible(View.GONE);
                mTempCache.setProgressBarVisible(View.VISIBLE);
                LoadListTask cleanData_Cache = new LoadListTask(CleanupUtil.WECHAT_TEMP_CACHE, mPathFilterCache);
                cleanData_Cache.execute();
            }
        });

        mOldFiles = (ListItemTextView) findViewById(R.id.old_files);
        mOldFiles.setRightTvListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOldFiles.setRightTvVisible(View.GONE);
                mOldFiles.setProgressBarVisible(View.VISIBLE);
                LoadListTask cleanData_olds = new LoadListTask(CleanupUtil.WECHAT_THROLD_FILES, mPathFilterOlds);
                cleanData_olds.execute();
            }
        });
        ListItemTextView chatImg = (ListItemTextView) findViewById(R.id.chat_img);
        chatImg.setOnClickListener(this);
        ListItemTextView wechatDl = (ListItemTextView) findViewById(R.id.wechat_download);
        wechatDl.setOnClickListener(this);
        ListItemTextView savedImg = (ListItemTextView) findViewById(R.id.saved_image);
        savedImg.setOnClickListener(this);

        initCleanPath();
    }

    @Override
    public void onClick(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.chat_img:
                intent = new Intent(mContext, WeChatPreviewActivity.class);
                intent.putExtra(EXTRA_WECHAT_TRASH_TYPE,CleanupUtil.WECHAT_CHAT_MEDIA);
                break;
            case R.id.wechat_download:
                intent = new Intent(mContext, WeChatPreviewActivity.class);
                intent.putExtra(EXTRA_WECHAT_TRASH_TYPE,CleanupUtil.WECHAT_DOWNLOADS);
                break;
            case R.id.saved_image:
                intent = new Intent(mContext, WeChatPreviewActivity.class);
                intent.putExtra(EXTRA_WECHAT_TRASH_TYPE,CleanupUtil.WECHAT_SAVED_MEDIA);
                break;
            default:
                break;
        }
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mContext.startActivity(intent);
        }
    }

    private void initCleanPath() {
        String defaultPath = Util.MEMORY_DIR;
        String sdCardPath = Util.SD_DIR;

        String[] currentPath = mContext.getResources().getStringArray(R.array.wechat_scan_path_temp_caches);
        int scanPathSize = currentPath.length;
        int pathListSize = scanPathSize * 2;

        List<String> filtered;
        for(int i = 0; i < pathListSize; i++) {
            if (i < scanPathSize) {
                filtered = CleanupUtil.FilterPath(defaultPath
                        + currentPath[i]);
            } else {
                filtered = CleanupUtil.FilterPath(sdCardPath
                        + currentPath[i - scanPathSize]);
            }
            if (filtered != null && filtered.size() > 0) {
                mPathFilterCache.addAll(filtered);
            }
        }

        currentPath = mContext.getResources().getStringArray(R.array.wechat_scan_path_throld_files);
        scanPathSize = currentPath.length;
        pathListSize = scanPathSize * 2;
        for(int i = 0; i < pathListSize; i++) {
            if (i < scanPathSize) {
                filtered = CleanupUtil.FilterPath(defaultPath
                        + currentPath[i]);

            } else {
                filtered = CleanupUtil.FilterPath(sdCardPath
                        + currentPath[i - scanPathSize]);
            }
            if (filtered != null && filtered.size() > 0) {
                mPathFilterOlds.addAll(filtered);
            }
        }
    }

    private class LoadListTask extends AsyncTask<Void, Integer, Void> {

        private List<String> mPathList;
        private int mPosition;
        FavoriteDatabaseHelper mDatabaseHelper;
        long mCurrentTime;

        public LoadListTask(int position, List<String> pathfile) {
            mPathList = pathfile;
            mPosition = position;
            mDatabaseHelper = FavoriteDatabaseHelper.getInstance();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (mPosition) {
                case CleanupUtil.WECHAT_TEMP_CACHE:
                    cleanCache();
                    break;
                case CleanupUtil.WECHAT_THROLD_FILES:
                    mCurrentTime = System.currentTimeMillis() / 1000;
                    cleanOldFiles();
                    break;
                default:
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void paramVoid) {
            switch (mPosition) {
                case CleanupUtil.WECHAT_TEMP_CACHE:
                    mTempCache.setProgressBarVisible(View.GONE);
                    mTempCache.refreshRightTv(R.string.storage_cleaned, false);
                    break;
                case CleanupUtil.WECHAT_THROLD_FILES:
                    mOldFiles.setProgressBarVisible(View.GONE);
                    mOldFiles.refreshRightTv(R.string.storage_cleaned, false);
                    break;
                default:
                    break;
            }
            super.onPostExecute(paramVoid);
        }

        private void showDisableTextView(TextView tv) {
            tv.setText(R.string.storage_cleaned);
            tv.setTextColor(mContext.getResources().getColor(R.color.textColorSecondary));
            tv.setEnabled(false);
            tv.setVisibility(View.VISIBLE);
        }

        private ArrayList<FileInfo> cleanCache() {
            ArrayList<FileInfo> cacheLists = new  ArrayList<FileInfo>();
            for (int i = 0; i < mPathList.size(); i++) {
                File pathFile = new File(mPathList.get(i));
                if (!pathFile.exists()) {
                    continue;
                } else {
                    DeleteFile(pathFile);
                }
            }
            return cacheLists;
        }

        private ArrayList<FileInfo> cleanOldFiles() {
            ArrayList<FileInfo> cacheLists = new  ArrayList<FileInfo>();
            for (int i = 0; i < mPathList.size(); i++) {
                File pathFile = new File(mPathList.get(i));
                if (!pathFile.exists()) {
                    continue;
                } else {
                    DeleteFileOlds(pathFile);
                }
            }
            return cacheLists;
        }

        private void DeleteFile(File pathFile) {
            boolean directory = pathFile.isDirectory();
            if (directory) {
                for (File child : pathFile.listFiles()) {
                    if (Util.isNormalFile(child.getAbsolutePath())) {
                        DeleteFile(child);
                    }
                }
            }
            if (mDatabaseHelper != null) {
                String path = pathFile.getAbsolutePath();
                if (mDatabaseHelper.isFavorite(path)) {
                    mDatabaseHelper.delete(path);
                }
            }
            pathFile.delete();
        }

        private void DeleteFileOlds(File pathFile) {
            boolean directory = pathFile.isDirectory();
            if (directory) {
                for (File child : pathFile.listFiles()) {
                    if (Util.isNormalFile(child.getAbsolutePath())) {
                        DeleteFileOlds(child);
                    }
                }
            }

            if (mDatabaseHelper != null) {
                String path = pathFile.getAbsolutePath();
                if (mDatabaseHelper.isFavorite(path)) {
                    mDatabaseHelper.delete(path);
                }
            }

            long diffTime = mCurrentTime - pathFile.lastModified()/1000;
            long diffMonths = diffTime / SECS_TO_MONS;
            if (diffMonths >= CleanupUtil.MONTHS_AGO) {
                pathFile.delete();
            }
        }
    }
}
