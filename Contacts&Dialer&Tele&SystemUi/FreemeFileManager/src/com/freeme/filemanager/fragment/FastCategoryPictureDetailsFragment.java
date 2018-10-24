package com.freeme.filemanager.fragment;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.ContentObserver;
import android.net.Uri;
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
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.freeme.filemanager.FMApplication;
import com.freeme.filemanager.FMApplication.SDCardChangeListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.controller.FileListAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.FileViewInteractionHub.Mode;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class FastCategoryPictureDetailsFragment extends BaseCategoryFragment implements
        IFileInteractionListener, IBackPressedListener, SDCardChangeListener {
    private static final String LOG_TAG = "FileCategoryActivity";
    private static final String ROOT_DIR = "/mnt";
    private static final int MSG_FILE_CHANGED_TIMER = 100;
    private final int TYPE_NOTIFY_REFRESH_HIDEFILE = 1;
    public FileViewInteractionHub mFileViewInteractionHub;
    private FileListAdapter mAdapter;
    private FileCategoryHelper mFileCagetoryHelper;
    private FileIconHelper mFileIconHelper;
    private ScannerReceiver mScannerReceiver;
    private FileExplorerTabActivity mActivity;
    private View mRootView;
    private boolean mConfigurationChanged = false;
    private FMApplication mApplication;

    private boolean isFirst = true;
    HashMap<String, String> mFolderListsBack = new HashMap<String, String>();
    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();
    ProgressDialog loadDialog;
    private ContentObserver mDatabaseListener;

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

    public class ScannerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mActivity = (FileExplorerTabActivity) getActivity();
        mRootView = inflater.inflate(R.layout.layout_fast_category_file_explorer, null,
                false);
        mRootView.setPadding(0, mActivity.getResources().getDimensionPixelSize(R.dimen.main_page_padding_top), 0, 0);

        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        mFileCagetoryHelper.setCurCategory(FileCategory.Picture);
        mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_PICFOLDER, SortMethod.name);
        mFileViewInteractionHub.setMode(Mode.View);
        mFileIconHelper = new FileIconHelper(mActivity);

        mAdapter = new FileListAdapter(mActivity, R.layout.layout_list_item_picture_folder,
                mFileNameList, mFileViewInteractionHub, mFileIconHelper);

        ListView fileListView = (ListView) mRootView
                .findViewById(R.id.file_path_list);
        fileListView.setDivider(mActivity.getDrawable(R.drawable.list_item_divider_picfolder));
        fileListView.setAdapter(mAdapter);

        mApplication = (FMApplication) mActivity.getApplication();
        setHasOptionsMenu(true);

        init();

        mFileViewInteractionHub.setRootPath(ROOT_DIR);
        LinearLayout searchLayout = (LinearLayout) mRootView.findViewById(R.id.search_layout);
        searchLayout.setVisibility(View.GONE);

        mActivity.getContentResolver().registerContentObserver(MediaStore.Images.Media.getContentUri("external"),
                true, mDatabaseListener);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFileViewInteractionHub != null && !mFileViewInteractionHub.isInSelection()) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    private void init() {
        loadDialog = new ProgressDialog(mRootView.getContext());
        registerScannerReceiver();

        Handler mHand = new Handler();
        mDatabaseListener = new ContentObserver(mHand) {
            @Override
            public boolean deliverSelfNotifications() {
                return super.deliverSelfNotifications();
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                isFirst =true;
                super.onChange(selfChange, uri);
            }

            @Override
            public void onChange(boolean selfChange) {
                isFirst =true;
                super.onChange(selfChange);
            }
        };

        mFileViewInteractionHub.mCurrentPath = ROOT_DIR;
    }

    private void registerScannerReceiver() {
        mScannerReceiver = new ScannerReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(1000);
        intentFilter.addAction(GlobalConsts.BROADCAST_REFRESH);
        intentFilter.addDataScheme("file");
        mActivity.registerReceiver(mScannerReceiver, intentFilter);
        mApplication.addSDCardChangeListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mFileViewInteractionHub.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mFileViewInteractionHub.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onBack() {
        if (mFileViewInteractionHub == null) {
            return false;
        }
        return mFileViewInteractionHub.onBackPressed();
    }

    @Override
    public View getViewById(int id) {
        return mRootView.findViewById(id);
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
                isFirst = true;
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
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }

    @Override
    public boolean shouldHideMenu(int menu) {
        return (menu == GlobalConsts.MENU_NEW_FOLDER
                || menu == GlobalConsts.MENU_SEARCH
                || menu == GlobalConsts.MENU_COMPRESS
                || menu == GlobalConsts.MENU_PASTE
                || menu == GlobalConsts.MENU_SHOWHIDE);
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

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    @Override
    public FileInfo getItem(int pos) {
        if (pos < 0 || pos > mFileNameList.size() - 1)
            return null;
        return mFileNameList.get(pos);
    }

    @Override
    public void sortCurrentList(final FileSortHelper sort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(mFileNameList, sort.getComparator());
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public Collection<FileInfo> getAllFiles() {
        return mFileNameList;
    }

    @Override
    public void addSingleFile(final FileInfo file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileNameList.add(file);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public void deleteSingleFile(final FileInfo file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileNameList.remove(file);
                showEmptyView(mFileNameList.size() == 0);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        if(TextUtils.isEmpty(path)){
            return false;
        }

        final FileCategory curCategory = mFileCagetoryHelper.getCurCategory();
        final ArrayList<FileInfo> fileList = mFileNameList;

        if (isFirst) {
            isFirst = false;
            LoadlistDataTask listData = new LoadlistDataTask(curCategory, fileList, sort);
            listData.execute();
        }
        return true;
    }

    class LoadlistDataTask extends AsyncTask<Void,Integer,ArrayList<FileInfo>> {
        private Context context;
        FileCategory mCategory;
        ArrayList<FileInfo> fileList;
        FileSortHelper msort;
        int pos;

        public LoadlistDataTask(FileCategory curCategory, ArrayList<FileInfo> fileLists, FileSortHelper sort) {
            mCategory = curCategory;
            fileList = fileLists;
            msort = sort;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadDialog.setMessage(mActivity.getResources().getString(R.string.load_data));
            if (!mActivity.isFinishing()) {
                loadDialog.show();
                loadDialog.setCancelable(false);
            }
            fileList.clear();
            mFolderListsBack.clear();
        }

        @Override
        protected ArrayList<FileInfo> doInBackground(Void... params) {
            ArrayList<FileInfo> fileLists = new ArrayList<FileInfo>();

            Cursor cursor = mFileCagetoryHelper.query(mCategory, FileSortHelper.SortMethod.name);

            if (cursor.moveToFirst()) {
                int photoIDIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int photoPathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int bucketDisplayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int bucketIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);

                do {
                    if (cursor.getString(photoPathIndex).substring(
                            cursor.getString(photoPathIndex).lastIndexOf("/") + 1,
                            cursor.getString(photoPathIndex).lastIndexOf("."))
                            .replaceAll(" ", "").length() <= 0) {
                        Log.v(LOG_TAG, "Abnormal picture:" + cursor.getString(photoPathIndex));
                    } else {
                        FileInfo info = new FileInfo();

                        String _id = cursor.getString(photoIDIndex);
                        String path = cursor.getString(photoPathIndex);
                        String bucketName = cursor.getString(bucketDisplayNameIndex);
                        String bucketId = cursor.getString(bucketIdIndex);
                        String bucket = mFolderListsBack.get(bucketId);

                        if (bucket == null) {
                            mFolderListsBack.put(bucketId, bucketName);

                            info.fileName = bucketName;
                            info.bucketId = bucketId;

                            Cursor mCursor = mFileCagetoryHelper.query(mCategory, bucketId);
                            info.Count = mCursor.getCount();
                            info.IsDir = true;
                            mCursor.moveToFirst();
                            info.filePath = mCursor.getString(photoPathIndex);
                            fileLists.add(info);
                        }
                    }
                } while (cursor.moveToNext());
            }

            return fileLists;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList<FileInfo> lastFileList) {
            if(fileList.isEmpty()){
                for(int i = 0; i < lastFileList.size(); i++){
                    fileList.add(lastFileList.get(i));
                }
            }

            fileList = lastFileList;
            if (loadDialog != null && loadDialog.isShowing()) {
                loadDialog.cancel();
            }
            mAdapter.notifyDataSetChanged();

            showEmptyView(fileList.size() == 0);
            mFileViewInteractionHub.setRefresh(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScannerReceiver != null) {
            mActivity.unregisterReceiver(mScannerReceiver);
        }
        mActivity.getContentResolver().unregisterContentObserver(mDatabaseListener);

        if (mFileViewInteractionHub != null) {
            // keep the progressDialog's lifecycle is consistent with mActivity
            mFileViewInteractionHub.DismissProgressDialog();
            // to cancel the doing works for this context is destroyed
            mFileViewInteractionHub.onOperationButtonCancel();
        }
        if (mApplication != null) {
            mApplication.removeSDCardChangeListener(this);
        }
        if (loadDialog != null && loadDialog.isShowing()) {
            loadDialog.dismiss();
        }
    }

    @Override
    public void onRefreshMenu(boolean visible) {

    }

    @Override
    public int getItemCount() {
        return mFileNameList.size();
    }

    @Override
    public void hideVolumesList() {

    }

    @Override
    public void finish() {
        if (mRootView != null) {
            mActivity.finish();
        } else {
            return;
        }
    }
    @Override
    public void onMountStateChange(int flag) {
        notifyFileChanged();
    }

    private void showUI() {
        mFileViewInteractionHub.refreshFileList();
        mActivity.invalidateOptionsMenu();
    }

    // process file changed notification, using a timer to avoid frequent
    // refreshing due to batch changing on file system
    synchronized public void notifyFileChanged() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                timer = null;
                Message message = new Message();
                message.what = MSG_FILE_CHANGED_TIMER;
                handler.sendMessage(message);
            }

        }, 100);

    }
}
