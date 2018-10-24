/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 *
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.fragment;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.freeme.filemanager.FMApplication;
import com.freeme.filemanager.FMApplication.SDCardChangeListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.activity.SearchActivity;
import com.freeme.filemanager.controller.FileListCursorAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.FileViewInteractionHub.Mode;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FavoriteDatabaseHelper.FavoriteDatabaseListener;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileCategoryHelper.CategoryInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.FavoriteList;
import com.freeme.filemanager.view.SeparateMenuLayout;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import static com.freeme.filemanager.FMIntent.EXTRA_BUCKETID_INFO;
import static com.freeme.filemanager.FMIntent.EXTRA_BUCKET_NAME;
import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_TAG;

public class FastCategoryDetailsFragment extends BaseCategoryFragment implements
        IFileInteractionListener, FavoriteDatabaseListener,
        IBackPressedListener, SDCardChangeListener {
    private static final String LOG_TAG = "FileCategoryActivity";
    private static final String ROOT_DIR = "/mnt";
    private static final int MSG_FILE_CHANGED_TIMER = 100;
    private final int TYPE_NOTIFY_REFRESH_HIDEFILE = 1;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 100;

    public FileViewInteractionHub mFileViewInteractionHub;
    private FileListCursorAdapter mAdapter;
    private FileCategoryHelper mFileCagetoryHelper;
    private FileIconHelper mFileIconHelper;
    private FileSortHelper mFileSortHelp;
    private SortMethod mSort;
    private ScannerReceiver mScannerReceiver;
    private FavoriteList mFavoriteList;
    private ViewPage mCurViewPage = ViewPage.Invalid;
    private ViewPage mPreViewPage = ViewPage.Invalid;
    private FileExplorerTabActivity mActivity;
    private View mRootView;
    private LinearLayout mExternalStorageBlock;
    private boolean mConfigurationChanged = false;
    private boolean mNeedRefreshCategoryInfos;
    private boolean mNeedUpdateOnTabSelected;

    private boolean mIsRefreshTimerCanceled = false;
    private Timer mRefreshFileListTimer;
    private boolean mIsPause = false;
    private FMApplication mApplication;
    private ProgressDialog mProgressDialog;
    private boolean mIsFirstRefreshDatabase = true;
    private Timer mClickWaitTimer;
    private SeparateMenuLayout mFormatMenu;
    private SeparateMenuLayout mSortMenu;
    private FileCategory mCategory;
    private ListView mFileListView;
    private GridView mGridView;
    private String mBucketId;

    private Handler refreshFileListHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TYPE_NOTIFY_REFRESH_HIDEFILE:
                    showUI();
                    break;
            }
            super.handleMessage(msg);
        }

    };
    private Timer timer;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FILE_CHANGED_TIMER:
                    showUI();
                    break;
            }
            super.handleMessage(msg);
        }

    };

    public void setConfigurationChanged(boolean changed) {
        mConfigurationChanged = changed;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mActivity = (FileExplorerTabActivity) getActivity();
        mRootView = inflater.inflate(R.layout.layout_fast_category_file_explorer, null,
                false);

        mFileIconHelper = new FileIconHelper(mActivity);
        mFavoriteList = new FavoriteList(mActivity,
                (ListView) mRootView.findViewById(R.id.favorite_list), this,
                mFileIconHelper);

        mFileListView = (ListView) mRootView
                .findViewById(R.id.file_path_list);

        mGridView = (GridView) mRootView
                .findViewById(R.id.file_folder_grid);

        mExternalStorageBlock = (LinearLayout) mRootView
                .findViewById(R.id.UsbStorage_block);
        mApplication = (FMApplication) mActivity.getApplication();

        setHasOptionsMenu(true);
        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        registerScannerReceiver();

        mCategory = (FileCategory) getArguments().getSerializable(EXTRA_CATEGORY_TAG);
        LinearLayout searchLayout = (LinearLayout) mRootView.findViewById(R.id.search_layout);
        searchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, SearchActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setData(mFileCagetoryHelper.getContentUriByCategory(mCategory));
                intent.putExtra("category_selection", mFileCagetoryHelper.buildSelectionByCategory(mCategory));
                startActivity(intent);
            }
        });

        if (mCategory.equals(FileCategory.Picture)) {
            mFileSortHelp = new FileSortHelper(SortMethod.date);
            mSort = mFileSortHelp.getSortMethod();
            mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_DETAILS, mSort);
            mAdapter = new FileListCursorAdapter(mActivity, null, R.layout.layout_file_grid_item,
                    mFileViewInteractionHub, mFileIconHelper);
            mGridView.setAdapter(mAdapter);
            mRootView.setPadding(0, 0, 0, 0);
            searchLayout.setVisibility(View.GONE);
            mBucketId = getArguments().getString(EXTRA_BUCKETID_INFO);
            String bucket_name = getArguments().getString(EXTRA_BUCKET_NAME);
            mActivity.getActionBar().setTitle(bucket_name);
            Util.setBackTitle(mActivity.getActionBar(), getString(R.string.category_picture));
        } else {
            mFileSortHelp = new FileSortHelper(SortMethod.name);
            mSort = mFileSortHelp.getSortMethod();
            mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_DETAILS, mSort);
            mAdapter = new FileListCursorAdapter(mActivity, null,
                    mFileViewInteractionHub, mFileIconHelper);
            mFileListView.setAdapter(mAdapter);
            searchLayout.setVisibility(View.VISIBLE);
        }
        mFileViewInteractionHub.setMode(Mode.View);
        mFileViewInteractionHub.setRootPath(ROOT_DIR);
        addPopupMenuView();
        onCategorySelected(mCategory);

        return mRootView;
    }

    private void addPopupMenuView() {
        LinearLayout lseparateMenu = (LinearLayout) mRootView.findViewById(R.id.separate_menu_view);
        lseparateMenu.setVisibility(View.VISIBLE);
        mSortMenu = (SeparateMenuLayout) mRootView.findViewById(R.id.sort_by);
        String sortTitle = getString(R.string.menu_item_sort);

        switch (mSort) {
            case name:
                sortTitle = getString(R.string.menu_item_sort_by,
                        getString(R.string.menu_item_sort_name));
                break;
            case size:
                sortTitle = getString(R.string.menu_item_sort_by,
                        getString(R.string.menu_item_sort_size));
                break;
            case date:
                sortTitle = getString(R.string.menu_item_sort_by,
                        getString(R.string.menu_item_sort_date));
                break;
            default:
                break;
        }

        mSortMenu.setMenuTitle(sortTitle);

        mSortMenu.setOnMenuItemClickListener(new SeparateMenuLayout.IpopupMenuItemClick() {
            @Override
            public void onPopupMenuClick(int itemId) {
                mFileViewInteractionHub.newPopupMenuResponse(itemId);
                mSortMenu.setMenuTitle(R.string.menu_item_sort_by, itemId);
            }
        });

        mFormatMenu = (SeparateMenuLayout) mRootView.findViewById(R.id.file_format);
        mFormatMenu.setMenuTitle(getString(R.string.menu_item_format));
        mFormatMenu.setOnMenuItemClickListener(new SeparateMenuLayout.IpopupMenuItemClick() {
            @Override
            public void onPopupMenuClick(int itemId) {
                mFileViewInteractionHub.newPopupMenuResponse(itemId);
            }
        });
    }

    private void registerScannerReceiver() {
        mScannerReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(1000);
        intentFilter.addAction(GlobalConsts.BROADCAST_REFRESH);
        intentFilter.addDataScheme("file");
        mActivity.registerReceiver(mScannerReceiver, intentFilter);

        //get Sd listener
        mApplication.addSDCardChangeListener(this);
    }

    private void showPage(ViewPage page) {
        if (mCurViewPage == page)
            return;

        mCurViewPage = page;

        showView(R.id.file_path_list, false);
        showView(R.id.file_folder_grid, false);
        showView(R.id.gallery_navigation_bar, false);
        showView(R.id.sd_not_available_page, false);
        mFavoriteList.show(false);

        switch (page) {
            case Favorite:
                showView(R.id.gallery_navigation_bar, false);
                mFavoriteList.update();
                mFavoriteList.show(true);
                showEmptyView(mFavoriteList.getCount() == 0);
                break;
/*            case Category:
                showView(R.id.gallery_navigation_bar, true);
                break;*/
        }
    }

    @Override
    public void showPathGalleryNavbar(boolean show) {
        showView(R.id.gallery_navigation_bar, show);
    }

    private void showEmptyView(boolean show) {
        View emptyView = mRootView.findViewById(R.id.category_empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            mFileViewInteractionHub.showEditMenuOrNot(!show);
        }
    }

    private void showView(int id, boolean show) {
        View view = mRootView.findViewById(id);
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void setTextView(int id, String t) {
        TextView text = (TextView) mRootView.findViewById(id);
        text.setText(t);
        text.setTextSize(14);
    }

    private void onCategorySelected(FileCategory fileCategory) {
        if (mFileCagetoryHelper.getCurCategory() != fileCategory) {
            mFileCagetoryHelper.setCurCategory(fileCategory);
            mFileViewInteractionHub.refreshFileList();
        }
        Log.i(LOG_TAG, "onCategorySelected = " + fileCategory);
        if (fileCategory == FileCategory.Favorite) {
            showPage(ViewPage.Favorite);
        } else {
            showPage(ViewPage.Category);
        }

        mFileViewInteractionHub.setCurrentPath(mFileViewInteractionHub
                .getRootPath()
                + "/"
                + mActivity.getString(mFileCagetoryHelper
                .getCurCategoryNameResId()));
    }

    @Override
    public boolean onBack() {
        if (mFileViewInteractionHub == null) {
            return false;
        }
        return mFileViewInteractionHub.onBackPressed();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mFileCagetoryHelper.getCurCategory() != FileCategory.Favorite) {
            mFileViewInteractionHub.onCreateOptionsMenu(menu);
        } else {
            menu.clear();
            LinearLayout separateMenu = (LinearLayout) mRootView.findViewById(R.id.separate_menu_view);
            if (separateMenu != null) {
                separateMenu.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mFileCagetoryHelper.getCurCategory() != FileCategory.Favorite) {
            mFileViewInteractionHub.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onRefreshFileList(String path,
                                     final FileSortHelper fileSortHelper) {
        setNeedRefreshCategoryInfos(true);
        // refresh favoritelist
        //mFavoriteList.initList();
        final FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
        if (curCategory == FileCategory.Favorite
                || curCategory == FileCategory.All) {
            return false;
        }

        RefreshFileListTask refreshFileListTask = new RefreshFileListTask(fileSortHelper, curCategory);
        refreshFileListTask.execute();

        return true;
    }

    @Override
    public View getViewById(int id) {
        return mRootView.findViewById(id);
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public FragmentManager getFragmentM() {
        return mActivity.getFragmentManager();
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean bool = false;
                FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
                if (curCategory == FileCategory.Favorite) {
                    return;
                }
                CategoryInfo categoryInfo = mFileCagetoryHelper
                        .getCategoryInfos().get(
                                mFileCagetoryHelper.getCurCategory());
                mAdapter.notifyDataSetChanged();

                if ((categoryInfo == null)
                        || (mAdapter.getCount() != categoryInfo.count)) {
                    bool = false;
                } else if (mAdapter.getCount() == 0) {
                    bool = true;
                }
                showEmptyView(bool);
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
        mFileViewInteractionHub.addContextMenuSelectedItem();
        switch (id) {
            case GlobalConsts.OPERATION_UP_LEVEL:
                setHasOptionsMenu(false);
                getFragmentManager().popBackStack();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public String getDisplayPath(String path) {
        String displayPath = mActivity.getString(R.string.tab_category)
                + path.substring(ROOT_DIR.length());
        return displayPath;
    }

    @Override
    public String getRealPath(String displayPath) {
        if (!TextUtils.isEmpty(displayPath)) {
            if (displayPath.equals(mActivity.getString(R.string.tab_category))) {
                return ROOT_DIR;
            }
        }
        return displayPath;
    }

    @Override
    public boolean shouldHideMenu(int menu) {
        return (menu == GlobalConsts.MENU_NEW_FOLDER
                || menu == GlobalConsts.MENU_SEARCH
                || menu == GlobalConsts.MENU_COMPRESS || menu == GlobalConsts.MENU_PASTE);
    }

    @Override
    public void addSingleFile(FileInfo file) {
        refreshList();
    }

    @Override
    public void deleteSingleFile(FileInfo file) {
        refreshList();
    }

    @Override
    public Collection<FileInfo> getAllFiles() {
        return mAdapter.getAllFiles();
    }

    @Override
    public FileInfo getItem(int pos) {
        return mAdapter.getFileItem(pos);
    }

    @Override
    public int getItemCount() {
        return mAdapter.getCount();
    }

    @Override
    public void sortCurrentList(FileSortHelper sort) {
        refreshList();
    }

    private void refreshList() {
        mFileViewInteractionHub.refreshFileList();
    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    private void showUI() {
        if (mPreViewPage != ViewPage.Invalid) {
            showPage(mPreViewPage);
            mPreViewPage = ViewPage.Invalid;
        }
        mFileViewInteractionHub.refreshFileList();

        mActivity.invalidateOptionsMenu();
    }

    public void setNeedRefreshCategoryInfos(boolean paramBoolean) {
        this.mNeedRefreshCategoryInfos = paramBoolean;
    }

    // process file changed notification, using a timer to avoid frequent
    // refreshing due to batch changing on file system
    synchronized public void notifyFileChanged(boolean flag) {
        final boolean mFlag = flag;
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                timer = null;
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putBoolean("flag", mFlag);
                message.setData(bundle);
                message.what = MSG_FILE_CHANGED_TIMER;
                handler.sendMessage(message);
            }

        }, 100);

    }

    // update the count of favorite
    @Override
    public void onFavoriteDatabaseChanged() {
        mFileViewInteractionHub.notifyRefreshViewInfo();
        if (mCurViewPage == ViewPage.Favorite) {
            showEmptyView(mFavoriteList.getCount() == 0);
        }
    }

    @Override
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mFileViewInteractionHub.isInSelection()) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    @Override
    public void pagerUserHide() {
        super.pagerUserHide();

        if (mClickWaitTimer != null) {
            mClickWaitTimer.cancel();
        }
        if (mRefreshFileListTimer != null) {
            mIsRefreshTimerCanceled = true;
            mRefreshFileListTimer.cancel();
        }

    }

    @Override
    public void pagerUserVisible() {
        super.pagerUserVisible();
        mIsRefreshTimerCanceled = false;
        showUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScannerReceiver != null) {
            mActivity.unregisterReceiver(mScannerReceiver);
        }

        if (mFileViewInteractionHub != null) {
            // keep the progressDialog's lifecycle is consistent with mActivity
            mFileViewInteractionHub.DismissProgressDialog();
            // to cancel the doing works for this context is destroyed
            mFileViewInteractionHub.onOperationButtonCancel();
            mFileViewInteractionHub.unbindTaskService();
        }
        if (mApplication != null) {
            mApplication.removeSDCardChangeListener(this);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void hideVolumesList() {

    }

    @Override
    public void onRefreshMenu(boolean path) {

    }

    //get Sd listener to refresh progressBar
    @Override
    public void onMountStateChange(int flag) {
        if (flag == SDCardChangeListener.flag_INJECT) {
            notifyFileChanged(true);
        } else {
            notifyFileChanged(false);
        }
    }

    @Override
    public void finish() {
        if (mRootView != null) {
            mActivity.finish();
        } else {
            return;
        }
    }

    public enum ViewPage {
        Home, Favorite, Category, Invalid
    }

    private class RefreshFileListTask extends AsyncTask<Void, Void, Cursor> {
        private FileSortHelper mFileSortHelper;
        private FileCategory mCurCategory;

        private RefreshFileListTask(FileSortHelper fileSortHelper, FileCategory curCategory) {
            mFileSortHelper = fileSortHelper;
            mCurCategory = curCategory;
        }

        @Override
        protected void onPreExecute() {

            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(mActivity);
                mProgressDialog.setTitle(getString(R.string.operation_load));
                mProgressDialog.setMessage(getString(R.string.operation_loading));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setCancelable(false);
            }

            if (mCurViewPage != ViewPage.Home) {
                mProgressDialog.show();
            }

        }

        @Override
        protected Cursor doInBackground(Void... params) {
            mAdapter.setSortHelper(mFileSortHelper);

            Cursor cursor;
            cursor = mCurCategory.equals(FileCategory.Picture) ? mFileCagetoryHelper.query(mCurCategory, mBucketId)
                    : mFileCagetoryHelper.query(mCurCategory, mFileSortHelper.getSortMethod());

            if (cursor != null) {
                String[] fils = new String[cursor.getCount()];
                if (mIsFirstRefreshDatabase && cursor.moveToFirst()) {
                    cursor.moveToFirst();
                    int i = 0;
                    do {
                        fils[i] = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                        i++;
                    } while (cursor.moveToNext());

                    mIsFirstRefreshDatabase = false;
                    mFileCagetoryHelper.deleteNoExistFile(fils);
                }
            }

            cursor = mCurCategory.equals(FileCategory.Picture) ? mFileCagetoryHelper.query(mCurCategory, mBucketId)
                    : mFileCagetoryHelper.query(mCurCategory, mFileSortHelper.getSortMethod());

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor cursor) {

            if (cursor != null) {
                showEmptyView(cursor.getCount() == 0);
                mAdapter.changeCursor(cursor);
                if (mCurViewPage != ViewPage.Home) {
                    if (mCurCategory.equals(FileCategory.Picture)) {
                        showView(R.id.file_folder_grid, true);
                    } else {
                        showView(R.id.file_path_list, true);
                    }
                    mCurViewPage = ViewPage.Home;
                }
            }

            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            mFileViewInteractionHub.setRefresh(true);
        }
    }

    public class ScannerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG,
                    "FilecategoryACtivity, ScannerReceiver onReceive(), intent:  "
                            + intent);
            String action = intent.getAction();
            if (action.equals(GlobalConsts.BROADCAST_REFRESH)) {
                if (intent.getIntExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, -1)
                        == GlobalConsts.BROADCAST_REFRESH_TABCATEGORY) {

                    refreshFileListHandler.sendEmptyMessageDelayed(TYPE_NOTIFY_REFRESH_HIDEFILE, 110);
                    showUI();
                }
            }
        }
    }
}
