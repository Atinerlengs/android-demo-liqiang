package com.freeme.filemanager.activity.cleanup.special;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.controller.FileListAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.CleanupUtil;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.freeme.filemanager.FMIntent.EXTRA_WECHAT_TRASH_TYPE;

public class WeChatPreviewActivity extends BaseActivity implements IFileInteractionListener {

    private Context mContext;
    private FileViewInteractionHub mFileViewInteractionHub;
    private FileIconHelper mFileIconHelper;
    private FileListAdapter mAdapter;

    private ListView mPreviewListView;
    private LinearLayout mEmptyView;
    private TextView mEmptyTv;
    private Button mCleanUpBtn;
    private ArrayList<FileInfo> mPreviewList = new ArrayList<FileInfo>();
    private String[] mCurrentScanPath;
    private int mCategory;

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        setContentView(R.layout.activity_wechat_special_preview);

        mContext = this.getContext();
        mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_DEFAULT, null);
        mFileViewInteractionHub.setMode(FileViewInteractionHub.Mode.Pick);
        mFileIconHelper = new FileIconHelper(this);
        init();

        mPreviewListView = (ListView) findViewById(R.id.file_path_list);
        mAdapter = new FileListAdapter(this, R.layout.layout_file_list_item,
                mPreviewList, mFileViewInteractionHub, mFileIconHelper);
        mPreviewListView.setAdapter(mAdapter);

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
        return path;
    }

    @Override
    public String getRealPath(String displayPath) {
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
        if (pos < 0 || pos > mPreviewList.size() - 1)
            return null;
        return mPreviewList.get(pos);
    }

    @Override
    public void sortCurrentList(final FileSortHelper sort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(mPreviewList, sort.getComparator());
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public Collection<FileInfo> getAllFiles() {
        return mPreviewList;
    }

    @Override
    public void addSingleFile(final FileInfo file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreviewList.add(file);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void deleteSingleFile(final FileInfo file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPreviewList.remove(file);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        String defaultPath = Util.MEMORY_DIR;
        String sdCardPath = Util.SD_DIR;
        int scanPathSize = mCurrentScanPath.length;
        int pathListSize = scanPathSize * 2;
        String[] allPathArray = new String[pathListSize];

        List<String> pathFilter = new ArrayList<>();
        for(int i = 0; i < pathListSize; i++) {
            if (i < scanPathSize) {
                allPathArray[i] = defaultPath + mCurrentScanPath[i];
            } else {
                allPathArray[i] = sdCardPath + mCurrentScanPath[i - scanPathSize];
            }
            if (allPathArray[i].contains("*")) {
                List<String> filtered = CleanupUtil.FilterPath(allPathArray[i]);
                if (filtered != null && filtered.size() > 0) {
                    pathFilter.addAll(filtered);
                }
            } else {
                pathFilter.add(allPathArray[i]);
            }
        }

        mPreviewList.clear();
        LoadListTask LoadData = new LoadListTask(mCategory, pathFilter, mPreviewList);
        LoadData.execute();

        return true;
    }

    @Override
    public void onRefreshMenu(boolean visible) {

    }

    @Override
    public int getItemCount() {
        return mPreviewList.size();
    }

    @Override
    public void hideVolumesList() {

    }

    private void init() {
        mFileViewInteractionHub.mCurrentPath = Util.getDefaultPath();
        mEmptyView = (LinearLayout) findViewById(R.id.empty_view);
        mEmptyTv = (TextView) findViewById(R.id.empty_tv);
        mCleanUpBtn = (Button) findViewById(R.id.cleanup_button);
        showEmptyView(false, 0);

        Intent intent = this.getIntent();
        mCategory = intent.getIntExtra(EXTRA_WECHAT_TRASH_TYPE, CleanupUtil.WECHAT_CHAT_MEDIA);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Util.setBackTitle(actionBar, getString(R.string.wechat_special));

        switch (mCategory) {
            case CleanupUtil.WECHAT_CHAT_MEDIA:
                actionBar.setTitle(R.string.clear_wechat_chat_img_title);
                mCurrentScanPath = mContext.getResources().getStringArray(R.array.wechat_scan_path_chat_media);
                break;
            case CleanupUtil.WECHAT_DOWNLOADS:
                actionBar.setTitle(R.string.clear_wechat_download_file);
                mCurrentScanPath = mContext.getResources().getStringArray(R.array.wechat_scan_path_download);
                break;
            case CleanupUtil.WECHAT_SAVED_MEDIA:
                actionBar.setTitle(R.string.clear_wechat_saved_img_title);
                mCurrentScanPath = mContext.getResources().getStringArray(R.array.wechat_scan_path_saved_media);
                break;
            default:
                break;

        }
    }

    private void showEmptyView(boolean show, int position) {
        if (show) {
            switch (position) {
                case CleanupUtil.WECHAT_CHAT_MEDIA:
                    mEmptyTv.setText(R.string.clear_wechat_not_found_chat_img);
                    break;
                case CleanupUtil.WECHAT_DOWNLOADS:
                    mEmptyTv.setText(R.string.clear_wechat_not_found_download);
                    break;
                case CleanupUtil.WECHAT_SAVED_MEDIA:
                    mEmptyTv.setText(R.string.clear_wechat_not_found_saved_img);
                    break;
                default:
                    mEmptyTv.setText(R.string.no_file);
                    break;

            }
            mEmptyView.setVisibility(View.VISIBLE);
            mCleanUpBtn.setVisibility(View.GONE);

        } else {
            mEmptyView.setVisibility(View.GONE);
            mCleanUpBtn.setVisibility(View.VISIBLE);
        }
    }

    private class LoadListTask extends AsyncTask<Void, Integer, ArrayList<FileInfo>> {

        private List<String> mPathList;
        private final int PATH_DEPTH = 2;
        private int mPathLists;
        private int mPosition;
        private ArrayList<FileInfo> mFileList;
        private ArrayList<FileInfo> mPathAllFiles = new  ArrayList<FileInfo>();
        FavoriteDatabaseHelper mDatabaseHelper;

        public LoadListTask(int position, List<String> pathfile, ArrayList<FileInfo> list) {
            mPathList = pathfile;
            mPosition = position;
            mFileList = list;
            mDatabaseHelper = FavoriteDatabaseHelper.getInstance();
        }

        @Override
        protected void onPreExecute() {
            mFileList.clear();
            super.onPreExecute();
        }

        @Override
        protected ArrayList<FileInfo> doInBackground(Void... voids) {
            ArrayList<FileInfo> fileLists = new  ArrayList<FileInfo>();
            switch (mPosition) {
                case CleanupUtil.WECHAT_CHAT_MEDIA:
                    fileLists = getChatLists();
                    break;
                case CleanupUtil.WECHAT_DOWNLOADS:
                    fileLists = getDownloadLists();
                    break;
                case CleanupUtil.WECHAT_SAVED_MEDIA:
                    fileLists = getSavedLists();
                    break;
                default:
                    break;
            }
            return fileLists;
        }

        @Override
        protected void onPostExecute(ArrayList<FileInfo> lastList) {
            if (mFileList.isEmpty() && !lastList.isEmpty()) {
                FileInfo[] tradingArray = new FileInfo[lastList.size()];
                Collections.addAll(mFileList, lastList.toArray(tradingArray));
            }
            mAdapter.notifyDataSetChanged();

            if (mFileList.isEmpty()) {
                showEmptyView(true, mPosition);
            }
            super.onPostExecute(lastList);
        }

        private ArrayList<FileInfo> getChatLists() {
            mPathAllFiles.clear();
            for (int i = 0; i < mPathList.size(); i++) {
                File pathFile = new File(mPathList.get(i));
                if (!pathFile.exists()) {
                    continue;
                }
                get2DepthFiles(pathFile);
            }
            return mPathAllFiles;
        }

        private ArrayList<FileInfo> getDownloadLists() {
            ArrayList<FileInfo> downloadLists = new  ArrayList<FileInfo>();
            for (int i = 0; i < mPathList.size(); i++) {
                File pathFile = new File(mPathList.get(i));
                if (!pathFile.exists()) {
                    continue;
                }

                for(String child : pathFile.list()) {
                    FileInfo childInfo = Util.GetFileInfo(pathFile + "/" + child);
                    if (childInfo != null && !childInfo.IsDir) {
                        downloadLists.add(childInfo);
                    }
                }
            }
            return downloadLists;
        }

        private ArrayList<FileInfo> getSavedLists() {
            ArrayList<FileInfo> savedLists = new  ArrayList<FileInfo>();
            for (int i = 0; i < mPathList.size(); i++) {
                File pathFile = new File(mPathList.get(i));
                if (!pathFile.exists()) {
                    continue;
                }

                for(String child : pathFile.list()) {
                    String absolutePath = pathFile + "/" + child;
                    FileInfo childInfo = Util.GetFileInfo(absolutePath);
                    if (childInfo != null && !childInfo.IsDir) {
                        String fileMimeType = EditUtility.getMimeTypeForFile(mContext, new File(absolutePath));
                        if (isImageVideo(fileMimeType)) {
                            savedLists.add(childInfo);
                        }
                    }
                }
            }
            return savedLists;
        }

        private ArrayList<FileInfo> get2DepthFiles(File pathFile) {
            for(String child : pathFile.list()) {
                String absolutePath = pathFile + "/" + child;
                FileInfo childInfo = Util.GetFileInfo(absolutePath);
                if (childInfo == null) {
                    continue;
                } else if (childInfo.IsDir) {
                    if (mPathLists == 0) {
                        mPathLists = PATH_DEPTH;
                        continue;
                    } else {
                        mPathLists--;
                        get2DepthFiles(new File(absolutePath));
                    }
                } else {
                    mPathLists = PATH_DEPTH;
                    String fileMimeType = EditUtility.getMimeTypeForFile(mContext, new File(absolutePath));
                    if (isImageVideo(fileMimeType)) {
                        mPathAllFiles.add(childInfo);
                    }
                }
            }
            return mPathAllFiles;
        }

        private boolean isImageVideo(String mimetype) {
            if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
                return true;
            } else {
                return false;
            }
        }
    }
}
