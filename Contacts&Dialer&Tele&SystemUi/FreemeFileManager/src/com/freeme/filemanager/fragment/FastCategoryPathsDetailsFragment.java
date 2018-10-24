package com.freeme.filemanager.fragment;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.freeme.filemanager.FMApplication;
import com.freeme.filemanager.FMApplication.SDCardChangeListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.activity.SearchActivity;
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
import com.freeme.filemanager.view.SeparateMenuLayout;
import com.freeme.filemanager.view.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_TAG;

public class FastCategoryPathsDetailsFragment extends BaseCategoryFragment implements
        IFileInteractionListener, IBackPressedListener, SDCardChangeListener {
    private static final String LOG_TAG = "FastCategoryPathsDetailsFragment";
    private static final String ROOT_DIR = "/mnt";
    private static final int MSG_FILE_CHANGED_TIMER = 100;
    private final int TYPE_NOTIFY_REFRESH_HIDEFILE = 1;
    private FileExplorerTabActivity mActivity;
    private View mRootView;
    private Context mContext;
    public FileViewInteractionHub mFileViewInteractionHub;
    private FileCategoryHelper mFileCagetoryHelper;
    private FileIconHelper mFileIconHelper;
    private FileSortHelper mFileSortHelper;
    private SortMethod mSort;
    private FileCategory category;
    private ArrayList<FileInfo> mFileNameList = new ArrayList<FileInfo>();
    private final ArrayList<FileInfo> fileList = mFileNameList;
    private ArrayList<FileInfo> mPathFileList = new ArrayList<FileInfo>();
    private ListView mFileListView;
    private FileListAdapter mAdapter;
    private SeparateMenuLayout mFormatMenu;
    private SeparateMenuLayout mSortMenu;
    private ScannerReceiver mScannerReceiver;
    private FMApplication mApplication;
    ProgressDialog loadDialog;
    String[] allPathArray;

    private boolean isFirst=true;
    private int pathListSize;

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
        mApplication = (FMApplication) mActivity.getApplication();

        mRootView = inflater.inflate(R.layout.layout_fast_category_qqwechat_explorer, null,
                false);
        mContext = mRootView.getContext();
        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        mFileSortHelper = new FileSortHelper(SortMethod.name);
        mSort = mFileSortHelper.getSortMethod();
        mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_DETAILS, mSort);
        mFileViewInteractionHub.setMode(Mode.View);
        mFileIconHelper = new FileIconHelper(mActivity);

        mFileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
        mAdapter = new FileListAdapter(mActivity, R.layout.layout_file_list_item,
                mFileNameList, mFileViewInteractionHub, mFileIconHelper);
        mFileListView.setAdapter(mAdapter);
        setHasOptionsMenu(true);
        init();
        category = (FileCategoryHelper.FileCategory) getArguments().getSerializable(EXTRA_CATEGORY_TAG);

        LinearLayout searchLayout = (LinearLayout) mRootView.findViewById(R.id.search_layout);
        searchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, SearchActivity.class);
                intent.putExtra("category_selection", getSelectionString(category));
                startActivity(intent);
            }
        });
        addPopupMenuView();
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFileViewInteractionHub != null && !mFileViewInteractionHub.isInSelection()) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void init() {
        loadDialog = new ProgressDialog(mContext);
        registerScannerReceiver();
        mFileViewInteractionHub.mCurrentPath = ROOT_DIR;
    }

    private void addPopupMenuView() {
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

    private String getSelectionString(FileCategory fc) {
        String selections = "";
        if (allPathArray != null) {
            for (int i = 0; i < allPathArray.length; i++) {
                selections = selections + "_data LIKE '" + allPathArray[i] + "%'";
                if (i < allPathArray.length - 1) {
                    selections = selections + " or ";
                }
            }
        }
        return selections;
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mFileCagetoryHelper.getCurCategory() != FileCategoryHelper.FileCategory.Favorite) {
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
        if (mFileCagetoryHelper.getCurCategory() != FileCategoryHelper.FileCategory.Favorite) {
            mFileViewInteractionHub.onPrepareOptionsMenu(menu);
        }
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
                || menu == GlobalConsts.MENU_COMPRESS || menu == GlobalConsts.MENU_PASTE);
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

    @Override
    public void deleteSingleFile(final FileInfo file) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFileNameList.remove(file);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        String defaultPath = Util.MEMORY_DIR;
        String sdCardPath = Util.SD_DIR;
        String[] pathArray;
        if (category == FileCategory.QQ) {
            pathArray = mActivity.getResources().getStringArray(R.array.qq_path_list);
        } else if (category == FileCategory.WeChat) {
            pathArray = mActivity.getResources().getStringArray(R.array.wechat_path_list);
        } else {
            pathArray = null;
        }
        if(pathArray == null){
            return false;
        }

        pathListSize = pathArray.length * 2;
        File[] file = new File[pathListSize];
        allPathArray = new String[pathListSize];

        for(int i = 0; i < pathListSize; i++) {
            if (i < pathArray.length) {
                allPathArray[i] = defaultPath + "/" + pathArray[i];
            } else {
                allPathArray[i] = sdCardPath + "/" + pathArray[i - pathArray.length];
            }
            file[i] = new File(allPathArray[i]);
        }

        fileList.clear();

        if (isFirst) {
            isFirst = false;
            LoadlistDataTask listData = new LoadlistDataTask(file, fileList, sort);
            listData.execute();
        } else {
            for (int i = 0; i < file.length; i++) {
                if (!file[i].exists()) {
                    continue;
                } else {
                    File[] listFiles = file[i].listFiles(mFileCagetoryHelper.getFilter());
                    if (listFiles == null || listFiles.length == 0) {
                        continue;
                    }
                    mPathFileList.clear();
                    mPathFileList = getAllFileInfo(listFiles);
                    fileList.addAll(mPathFileList);
                }
            }
            sortCurrentList(sort);
            showEmptyView(fileList.size() == 0);
        }
        return true;
    }

    private ArrayList<FileInfo>  getAllFileInfo (File[] listFiles) {
        String absolutePath="";
        if (listFiles != null) {
            for(File child : listFiles) {
                absolutePath = child.getAbsolutePath();
                if (child.isDirectory()) {
                    if (Util.isNormalFile(absolutePath)
                            && Util.shouldShowFile(absolutePath)) {
                        getAllFileInfo(child.listFiles());
                    } else {
                        continue;
                    }
                } else {
                    if (Util.isNormalFile(absolutePath)
                            && Util.shouldShowFile(absolutePath)) {

                        FileInfo lFileInfo = Util.GetFileInfo(child,
                                mFileCagetoryHelper.getFilter(), Settings
                                        .instance().getShowDotAndHiddenFiles());
                        if (lFileInfo != null && !lFileInfo.fileName.equals("droi")) {
                            mPathFileList.add(lFileInfo);
                            Util.scanAllFile(mContext, new String[]{absolutePath});
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return mPathFileList;
    }

    class LoadlistDataTask extends AsyncTask<Void,Integer,ArrayList<FileInfo>> {
        File[] datafileList = new File[pathListSize];
        ArrayList<FileInfo> fileList;

        FileSortHelper msort;

        public LoadlistDataTask(File[] file, ArrayList<FileInfo> fileLists, FileSortHelper sort) {
            datafileList = file;
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
            sortCurrentList(msort);
        }
        @Override
        protected ArrayList<FileInfo> doInBackground(Void... params) {
            ArrayList<FileInfo> fileListsBack = new  ArrayList<FileInfo>();
            for (int i = 0; i < datafileList.length; i++) {
                if (!datafileList[i].exists()) {
                    continue;
                }

                File[] listFiles = datafileList[i].listFiles(mFileCagetoryHelper.getFilter());
                mPathFileList.clear();
                mPathFileList = getAllFileInfo(listFiles);
                if (mPathFileList.size() > 0) {
                    fileListsBack.addAll(mPathFileList);
                }
            }
            return fileListsBack;
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
            sortCurrentList(msort);
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
