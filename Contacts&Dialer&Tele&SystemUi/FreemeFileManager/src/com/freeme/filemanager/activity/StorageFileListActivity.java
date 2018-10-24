package com.freeme.filemanager.activity;

import java.util.Collection;
import java.util.HashMap;

import com.freeme.filemanager.controller.FileListCursorAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.FileViewInteractionHub.Mode;
import com.freeme.filemanager.controller.IActionModeCtr;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.util.FileCategoryHelper.CategoryInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.PermissionUtil;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.FavoriteList;
import com.freeme.filemanager.view.SeparateMenuLayout;
import com.freeme.filemanager.view.Settings;
import com.freeme.filemanager.fragment.FastCategoryDetailsFragment.ViewPage;
import com.freeme.filemanager.R;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.ProgressDialog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_INFO;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TYPE;

public class StorageFileListActivity extends Activity implements
        IFileInteractionListener, IActionModeCtr{

    private FileListCursorAdapter mAdapter;
    private FileViewInteractionHub mFileViewInteractionHub;
    private FileIconHelper mFileIconHelper;
    private FileSortHelper mFileSortHelper;
    private SortMethod mSort;
    private String TAG = "StorageFileListActivity";
    private Context mContext;
    private ActionMode mActionMode;
    private FileCategoryHelper mFileCagetoryHelper;
    private static final String ROOT_DIR = "/mnt";
    private HashMap<FileCategory, Integer> categoryIndex = new HashMap<FileCategory, Integer>();
    private ViewPage curViewPage = ViewPage.Invalid;
    private ViewPage preViewPage = ViewPage.Invalid;
    public ViewPage mViewPager;
    private FavoriteList mFavoriteList;
    private boolean mNeedRefreshCategoryInfos;
    private AsyncTask<Void, Void, Cursor> mRefreshFileListTask;
    private AsyncTask<Void, Void, Object> mRefreshCategoryInfoTask;
    private boolean mConfigurationChanged = false;
    private int mStorageType;
    private ProgressDialog mProgressDialog;
    private ListView mListView;
    private MenuItem mEditMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_details);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        Util.setBackTitle(actionBar, getString(R.string.all_storage));

        mContext = StorageFileListActivity.this.getContext();
        mFileSortHelper = new FileSortHelper(SortMethod.name);
        mSort = mFileSortHelper.getSortMethod();
        mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_STORAGE, mSort);

        mFileViewInteractionHub.setMode(Mode.View);
        mFileIconHelper = new FileIconHelper(mContext);
        mAdapter = new FileListCursorAdapter(mContext, null,
                mFileViewInteractionHub, mFileIconHelper);
        mListView = (ListView) findViewById(R.id.file_path_list);
        mListView.setAdapter(mAdapter);

        setupCategoryInfo();

        Intent intent = getIntent();
        FileCategory catrgoryInfo = (FileCategory) intent.getSerializableExtra(EXTRA_CATEGORY_INFO);
        mStorageType = getIntent().getIntExtra(EXTRA_STORAGE_TYPE, 0);

        onClickFileCategory(catrgoryInfo);
        addPopupMenuView();
    }

    private void addPopupMenuView() {
        LinearLayout lseparateMenu = (LinearLayout) findViewById(R.id.separate_menu_view);
        lseparateMenu.setVisibility(View.VISIBLE);
        final SeparateMenuLayout sortMenu = (SeparateMenuLayout) findViewById(R.id.sort_by);
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

        sortMenu.setMenuTitle(sortTitle);

        sortMenu.setOnMenuItemClickListener(new SeparateMenuLayout.IpopupMenuItemClick() {
            @Override
            public void onPopupMenuClick(int itemId) {
                mFileViewInteractionHub.newPopupMenuResponse(itemId);
                sortMenu.setMenuTitle(R.string.menu_item_sort_by, itemId);
            }
        });

        SeparateMenuLayout formatMenu = (SeparateMenuLayout) findViewById(R.id.file_format);
        formatMenu.setMenuTitle(getString(R.string.menu_item_format));
        formatMenu.setOnMenuItemClickListener(new SeparateMenuLayout.IpopupMenuItemClick() {
            @Override
            public void onPopupMenuClick(int itemId) {
                mFileViewInteractionHub.newPopupMenuResponse(itemId);
            }
        });
    }
    private void onClickFileCategory(FileCategory f) {
        if (f != null) {
            onCategorySelected(f);
        }
    }

    private void onCategorySelected(FileCategory fileCategory) {
        Log.i(TAG, "FileCategory=" + fileCategory);
        if (mFileCagetoryHelper.getCurCategory() != fileCategory) {
            Log.i(TAG, "mFileCagetoryHelper=" + mFileCagetoryHelper);
            mFileCagetoryHelper.setCurCategory(fileCategory);
            mFileViewInteractionHub.refreshFileList();
        }
        mFileViewInteractionHub.setCurrentPath(mFileViewInteractionHub
                .getRootPath()
                + "/"
                + mContext.getString(mFileCagetoryHelper
                        .getCurCategoryNameResId()));
        this.getActionBar().setTitle(mContext.getString(mFileCagetoryHelper
                .getCurCategoryNameResId()));
    }

    private void setupCategoryInfo() {
        mFileCagetoryHelper = new FileCategoryHelper(mContext);
        int[] imgs = new int[] { R.drawable.category_bar_music,
                R.drawable.category_bar_video, R.drawable.category_bar_picture,
                R.drawable.category_bar_document, R.drawable.category_bar_apk,
                R.drawable.category_bar_other };

        for (int i = 0; i < FileCategoryHelper.sCategories.length; i++) {
            categoryIndex.put(FileCategoryHelper.sCategories[i], i);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.storage_details_explorer_menu, menu);
        mEditMenu = menu.findItem(R.id.menu_edit);
        mEditMenu.setVisible(mListView != null && mListView.getCount() > 0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.selectall:
            mFileViewInteractionHub.onOperationSelectAllOrCancel();
            break;
        case R.id.show_hide:
                mFileViewInteractionHub.onOperationShowSysFiles();
                item.setTitle(Settings.instance().getShowDotAndHiddenFiles() ? R.string.operation_hide_sysfile
                        : R.string.operation_show_sysfile);
            break;
        case R.id.menu_refresh:
            mFileViewInteractionHub.onOperationReferesh();
            break;
        case R.id.menu_edit:
            mFileViewInteractionHub.setSelectionMode(true);
            break;
        case android.R.id.home:
            onBackPressed();
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.unbindTaskService();
        }

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public View getViewById(int id) {
        return findViewById(id);
    }

    @Override
    public Context getContext() {
        return StorageFileListActivity.this;
    }

    @Override
    public FragmentManager getFragmentM() {
        return StorageFileListActivity.this.getFragmentManager();
    }

    @Override
    public void onDataChanged() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                boolean bool = false;
                FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
                Log.i(TAG, "curCategory=" + curCategory);
                if (curCategory == FileCategory.Other) {
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

    private void showEmptyView(boolean show) {
        View emptyView = this.findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            if (mEditMenu != null) {
                mEditMenu.setVisible(!show);
            }
        }
    }

    @Override
    public void onPick(FileInfo f) {
        // do nothing
    }

    @Override
    public boolean shouldShowOperationPane() {
        return true;
    }

    @Override
    public boolean onOperation(int id) {
        mFileViewInteractionHub.addContextMenuSelectedItem();

        return false;
    }

    @Override
    public String getDisplayPath(String path) {
        String displayPath = getString(R.string.tab_category)
                + path.substring(ROOT_DIR.length());
        return displayPath;
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
        return (menu == GlobalConsts.MENU_NEW_FOLDER
                || menu == GlobalConsts.MENU_SEARCH
                || menu == GlobalConsts.MENU_COMPRESS || menu == GlobalConsts.MENU_PASTE);
    }

    @Override
    public void showPathGalleryNavbar(boolean show) {
        showView(R.id.gallery_navigation_bar, show);
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
    public void sortCurrentList(FileSortHelper sort) {
        refreshList();
    }

    @Override
    public Collection<FileInfo> getAllFiles() {
        return mAdapter.getAllFiles();
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
    public boolean onRefreshFileList(String path, final FileSortHelper sort) {
        if (!PermissionUtil.hasStoragePermissions(this)) {
            finish();
            return false;
        }

        final FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
        if (curCategory == FileCategory.All) {
            return false;
        }

        mRefreshFileListTask = new AsyncTask<Void, Void, Cursor>() {
            @Override
            protected void onPreExecute() {

                showEmptyView(false);

                if(mProgressDialog == null) {
                    mProgressDialog = new ProgressDialog(mContext);
                    mProgressDialog.setTitle(getString(R.string.operation_load));
                    mProgressDialog.setMessage(getString(R.string.operation_loading));
                    mProgressDialog.setIndeterminate(false);
                    mProgressDialog.setCancelable(false);
                }
                mProgressDialog.show();


            }
            @Override
            protected Cursor doInBackground(Void... params) {
                mAdapter.setSortHelper(sort);
                return mFileCagetoryHelper.query(curCategory, sort.getSortMethod(), mStorageType);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (cursor != null) {
                    showEmptyView(cursor == null || cursor.getCount() == 0);
                    mAdapter.changeCursor(cursor);
                        showView(R.id.file_path_list,true);
                }
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }

                mFileViewInteractionHub.setRefresh(true);
            }

        };

        mRefreshFileListTask.execute(new Void[0]);

        return true;
    }

    public void setNeedRefreshCategoryInfos(boolean paramBoolean) {
        this.mNeedRefreshCategoryInfos = paramBoolean;
    }

    @Override
    public void onRefreshMenu(boolean visible) {
        // TODO Auto-generated method stub
    }

    @Override
    public int getItemCount() {
        return mAdapter.getCount();
    }

    @Override
    public void hideVolumesList() {
        // TODO Auto-generated method stub
    }

    private void showView(int id, boolean show) {
        View view = this.findViewById(id);
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void setActionMode(ActionMode actionMode) {
        mActionMode = actionMode;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

    private void refreshList() {
        mFileViewInteractionHub.refreshFileList();
    }
}
