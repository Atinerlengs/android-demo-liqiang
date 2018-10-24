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

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.os.storage.StorageVolume;

import com.freeme.filemanager.activity.FileExplorerPreferenceActivity;
import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.FMApplication;
import com.freeme.filemanager.FMApplication.SDCardChangeListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.activity.SearchActivity;
import com.freeme.filemanager.controller.ActivitiesManager;
import com.freeme.filemanager.controller.FileListAdapter;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.IFileInteractionListener;
import com.freeme.filemanager.controller.FileViewInteractionHub.Mode;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.util.MountHelper;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.Settings;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;

import static com.freeme.safe.utils.SafeConstants.DECRYPTION_PATH;
import static com.freeme.safe.utils.SafeConstants.FROM_SAFE;

public class FileExplorerViewFragment extends BaseFragment implements IFileInteractionListener, IBackPressedListener, SDCardChangeListener {

    public static final String EXT_FILTER_KEY = "ext_filter";

    private static final String LOG_TAG = "FileViewActivity";

    public static final String PICK_FOLDER = "pick_folder";

    public static final int VIEW_DELAY_LOAD = 0x101;

    private ImageView mVolumeSwitch;

    private ListView mFileListView;

    private ArrayAdapter<FileInfo> mAdapter;

    public FileViewInteractionHub mFileViewInteractionHub;

    private FileCategoryHelper mFileCagetoryHelper;

    private FileIconHelper mFileIconHelper;

    private ArrayList<FileInfo> mFileNameList = new ArrayList<>();

    private FileExplorerTabActivity mActivity;

    private View mRootView;

    private LinearLayout mGalleryNavigationBar;

    private String mVolumeDescription;

    private String mVolumePath;

    private String mTagPath;
    ProgressDialog loadDialog;
    private boolean isFirst=true;
    private FMApplication mApplication;

    private boolean mReceiverTag = false;
    private boolean mChildEmpty;

    // memorize the scroll positions of previous paths
    private ArrayList<PathScrollPositionItem> mScrollPositionList = new ArrayList<>();
    private String mPreviousPath;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(LOG_TAG, "received broadcast:" + intent.toString());
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_MEDIA_MOUNTED:
                    //get Sd listener to refresh progressBar and file list
                    notifyFileChanged(true);
                    break;
                case Intent.ACTION_MEDIA_EJECT:
                case Intent.ACTION_MEDIA_BAD_REMOVAL:
                case Intent.ACTION_MEDIA_UNMOUNTED:
                    notifyFileChanged(false);
                    break;
                case Intent.ACTION_MEDIA_SCANNER_FINISHED:
                    updateUI();
                    break;
                case GlobalConsts.BROADCAST_REFRESH:
                    if (intent.getIntExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, -1)
                            == GlobalConsts.BROADCAST_REFRESH_TABVIEW) {
                        updateUI();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private boolean mBackspaceExit;
    private boolean mFromSafe;
    private HomeBroadcastReceiver mHomeBroadcastReceiver;

    @SuppressLint("HandlerLeak")
    private Handler mFileViewHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
            case VIEW_DELAY_LOAD:
                init();
                break;
            default:
                break;
            }

        }
    };

    @Override
    public View onFragmentCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = (FileExplorerTabActivity)getActivity();
        ActionBar actionBar = mActivity.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.tab_file);

        mRootView = inflater.inflate(R.layout.layout_file_explorer_stub, container, false);

        mFileViewHandler.sendEmptyMessageDelayed(VIEW_DELAY_LOAD,350);
        return mRootView;
    }

    @Override
    public void initUserData() {
        super.initUserData();
    }

    public void init(){
        ViewStub stub = (ViewStub) mRootView.findViewById(R.id.viewContaniner);
        stub.setLayoutResource(R.layout.fragment_file_explorer);
        stub.inflate();
        ActivitiesManager.getInstance().registerActivity(
                ActivitiesManager.ACTIVITY_FILE_VIEW, mActivity);

        mFileCagetoryHelper = new FileCategoryHelper(mActivity);
        mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_EXPLORER, SortMethod.name);

        mApplication = (FMApplication) mActivity.getApplication();

        Intent intent = mActivity.getIntent();
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)
                && (action.equals(Intent.ACTION_PICK) || action
                        .equals(Intent.ACTION_GET_CONTENT))) {
            mFileViewInteractionHub.setMode(Mode.Pick);

            boolean pickFolder = intent.getBooleanExtra(PICK_FOLDER, false);
            if (!pickFolder) {
                String[] exts = intent.getStringArrayExtra(EXT_FILTER_KEY);
                if (exts != null) {
                    mFileCagetoryHelper.setCustomCategory(exts);
                }
            } else {
                mFileCagetoryHelper.setCustomCategory(new String[] {} /*
                                                                     * folder
                                                                     * only
                                                                     */);
                mRootView.findViewById(R.id.pick_operation_bar).setVisibility(
                        View.VISIBLE);

                mRootView.findViewById(R.id.button_pick_confirm)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                try {
                                    Intent intent = Intent.parseUri(
                                            mFileViewInteractionHub
                                                    .getCurrentPath(), 0);
                                    mActivity.setResult(Activity.RESULT_OK,
                                            intent);
                                    mActivity.finish();
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                mRootView.findViewById(R.id.button_pick_cancel)
                        .setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                mActivity.finish();
                            }
                        });
            }
        } else {
            mFileViewInteractionHub.setMode(Mode.View);
        }
        mVolumeSwitch = (ImageView) mRootView
                .findViewById(R.id.volume_navigator);
        updateVolumeSwitchState();
        mGalleryNavigationBar = (LinearLayout) mRootView
                .findViewById(R.id.gallery_navigation_bar);
        mVolumeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int visibility = getVolumesListVisibility();
                if (visibility == View.GONE) {
                    buildVolumesList();
                    showVolumesList(true);
                } else if (visibility == View.VISIBLE) {
                    showVolumesList(false);
                }
            }
        });
        mFileListView = (ListView) mRootView.findViewById(R.id.file_path_list);
        mFileIconHelper = new FileIconHelper(mActivity);
        mAdapter = new FileListAdapter(mActivity, R.layout.layout_file_list_item,
                mFileNameList, mFileViewInteractionHub, mFileIconHelper);

        Uri uri = intent.getData();

        initVolumeState();
        mBackspaceExit = (uri != null)
                && (TextUtils.isEmpty(action) || (!action
                        .equals(Intent.ACTION_PICK) && !action
                        .equals(Intent.ACTION_GET_CONTENT)));

        mFileListView.setAdapter(mAdapter);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(1000);

        if (!mReceiverTag) {
            mReceiverTag = true;
            intentFilter.addAction(GlobalConsts.BROADCAST_REFRESH);
            intentFilter.addDataScheme("file");
            mActivity.registerReceiver(mReceiver, intentFilter);
        }

        if (getArguments() != null) {
            mFromSafe = getArguments().getBoolean(FROM_SAFE);
            if (mFromSafe) {
                registerHomeBroadcastReceiver();
            }
        }

        mApplication.addSDCardChangeListener(this);

        setHasOptionsMenu(true);

        mRootView.addOnLayoutChangeListener(new OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(View arg0, int arg1, int arg2, int arg3,
                    int arg4, int arg5, int arg6, int arg7, int arg8) {
            }
        });
        loadDialog = new ProgressDialog(mRootView.getContext());

        LinearLayout searchLayout = (LinearLayout) mRootView.findViewById(R.id.search_layout);
        searchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, SearchActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
        ImageView newFolderView = (ImageView) mRootView.findViewById(R.id.new_folder);
        newFolderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFileViewInteractionHub.onOperationCreateFolder();
            }
        });
    }

    private void buildVolumesList(){
        LinearLayout linearyLayout = (LinearLayout) mRootView.findViewById(R.id.dropdown_navigation_list);
        linearyLayout.removeAllViews();

        ArrayList mountVolumeList = StorageHelper.getInstance(mActivity).getSortedMountVolumeList();
        if (mountVolumeList == null || mountVolumeList.size()==0){
            return;
        }

        Iterator iterator = mountVolumeList.iterator();
        while (iterator.hasNext()) {
            storageVolume = (StorageVolume) iterator.next();
            if(storageVolume.getStorageId() != 0){
                linearyLayout.addView(createStorageVolumeItem(storageVolume.getPath(), storageVolume.getDescription(mActivity)));
            }
        }
    }

    private String  internalPath = Util.getDefaultPath();

    private View createStorageVolumeItem(final String volumPath,String volumDescription) {

        View listItem = LayoutInflater.from(mActivity).inflate(R.layout.layout_storage_selected_item, null);
        View listContent = listItem.findViewById(R.id.list_item);
        ImageView img = (ImageView) listItem.findViewById(R.id.item_icon);
        TextView text = (TextView) listItem.findViewById(R.id.path_name);
        text.setText(volumDescription);
        if (storageVolume.getPath().equals(internalPath)) {
            img.setImageDrawable((getResources().getDrawable(R.drawable.new_internal)));
        }else if (storageVolume.getPath().equals(Util.SD_DIR)) {
            img.setImageDrawable((getResources().getDrawable(R.drawable.new_sd)));
        }else if (storageVolume.getPath().equals(Util.USBOTG_DIR)) {
            img.setImageDrawable((getResources().getDrawable(R.drawable.new_sd)));
        }

        ImageView unmount_btn = (ImageView) listItem.findViewById(R.id.unmount_btn);
        if(!volumPath.equals(Util.MEMORY_DIR)){
            if(mActivity.getApplicationContext().checkPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS",
                    android.os.Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED){
                unmount_btn.setVisibility(View.VISIBLE);
                unmount_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        MountHelper.getInstance(mActivity).unMount(volumPath);
                        showVolumesList(false);
                        mVolumeSwitch.setVisibility(View.GONE);
                    }
                });
            } else {
                unmount_btn.setVisibility(View.INVISIBLE);
            }
        }

        listItem.setOnClickListener(mStorageVolumeClick);
        listItem.setTag(new Pair(volumPath, volumDescription));
        return listItem;
    }

    private View.OnClickListener mStorageVolumeClick = new View.OnClickListener(){
        @Override
        public void onClick(View paramView)
        {
            showVolumesList(false);
            Pair localPair = (Pair) paramView.getTag();
            if (((String) localPair.first).equals(mVolumePath)){
                return;
            }
            mVolumePath = (String) localPair.first;
            mVolumeDescription = (String) localPair.second;
            mFileViewInteractionHub.setRootPath(mVolumePath);
            mFileViewInteractionHub.exitActionMode();
            updateUI();
        }
    };

    private int getVolumesListVisibility() {
        return this.mRootView.findViewById(R.id.dropdown_navigation).getVisibility();
    }

    private void showVolumesList(boolean show) {
        View view = mRootView.findViewById(R.id.dropdown_navigation);
        view.setVisibility(show==true? View.VISIBLE : View.GONE);
    }

    private void initVolumeState() {
        LoadDataTask loadData = new LoadDataTask();
        loadData.execute();
    }

    private void initVolumeState(StorageVolume paramStorageVolume) {
        if (paramStorageVolume == null){
            return;
        }
        this.mVolumePath = paramStorageVolume.getPath();
        this.mVolumeDescription = paramStorageVolume.getDescription(this.mActivity);

        if (paramStorageVolume.getStorageId() == 0) {
            mVolumeDescription = getString(R.string.storage_phone);
        }

        if (mFileViewInteractionHub == null){
            mFileViewInteractionHub = new FileViewInteractionHub(this, Util.PAGE_EXPLORER, SortMethod.name);
        }

        mFileViewInteractionHub.setRootPath(this.mVolumePath);
    }

     @Override
    public void fragmentShow() {
        super.fragmentShow();
    }

    @Override
    public void fragmentHint() {
        super.fragmentHint();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFileViewInteractionHub != null && !mFileViewInteractionHub.isInSelection()) {
            mFileViewInteractionHub.refreshFileList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            mActivity.unregisterReceiver(mReceiver);
        }
        if (mFromSafe) {
            unregisterHomeBroadcastReceiver();
        }

        if(mFileViewInteractionHub != null){
            mFileViewInteractionHub.DismissProgressDialog();
            mFileViewInteractionHub.unbindTaskService();
        }

        if(mApplication != null){
            mApplication.removeSDCardChangeListener(this);
        }
        if (loadDialog != null && loadDialog.isShowing()) {
            loadDialog.dismiss();
        }
    }

    private Menu optionMenu = null;
    private boolean isOperate=false;

    private StorageVolume storageVolume;

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mFileViewInteractionHub.onPrepareOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mFileViewInteractionHub.onCreateOptionsMenu(menu);
        onRefreshMenu(isOperate);
        optionMenu = menu;

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onBack() {
        if (mBackspaceExit) {
            return false;
        }

        //delete the dir of mtklog,fileManager stop run after press the back
        if (mPreviousPath != null) {
            if ((mPreviousPath.equals(Util.SD_DIR) || mPreviousPath.equals(Util.MEMORY_DIR)) && isOperate) {
                mFileViewInteractionHub.onOperationButtonCancel();
            }
        }

        return mFileViewInteractionHub != null && mFileViewInteractionHub.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class PathScrollPositionItem {
        String path;
        int pos;
        PathScrollPositionItem(String s, int p) {
            path = s;
            pos = p;
        }
    }

    @Override
    public void showPathGalleryNavbar(boolean show){
        if(mGalleryNavigationBar != null){
            mGalleryNavigationBar.setVisibility(show == true? View.VISIBLE : View.GONE);
        }
    }

    private void updateVolumeSwitchState(){
        if (mVolumeSwitch != null) {
            ArrayList<StorageVolume> mountVolumeList = StorageHelper.getInstance(mActivity).getSortedMountVolumeList();
            ArrayList<StorageVolume> volumeList = new ArrayList<StorageVolume>();
            for (int i = 0; i < mountVolumeList.size(); i++) {
                StorageVolume localStorageVolume = mountVolumeList.get(i);
                if (localStorageVolume.getStorageId() != 0) {
                    volumeList.add(localStorageVolume);
                    Log.i(LOG_TAG, "volumeList =" + volumeList.size());
                }
            }
            if (volumeList.size() > 1) {
                mVolumeSwitch.setVisibility(View.VISIBLE);
            } else {
                mVolumeSwitch.setVisibility(View.GONE);
            }
        }
    }

    private int computeScrollPosition(String path) {
        int pos = 0;
        if(mPreviousPath!=null) {
            if (path.startsWith(mPreviousPath)) {
                int firstVisiblePosition = mFileListView.getFirstVisiblePosition();
                if (mScrollPositionList.size() != 0
                        && mPreviousPath.equals(mScrollPositionList.get(mScrollPositionList.size() - 1).path)) {
                    mScrollPositionList.get(mScrollPositionList.size() - 1).pos = firstVisiblePosition;
                    pos = firstVisiblePosition;
                } else {
                    mScrollPositionList.add(new PathScrollPositionItem(mPreviousPath, firstVisiblePosition));
                }
            } else {
                int i;
                boolean isLast = false;
                for (i = 0; i < mScrollPositionList.size(); i++) {
                    if (!path.startsWith(mScrollPositionList.get(i).path)) {
                        break;
                    }
                }
                if (i > 0) {
                    pos = mScrollPositionList.get(i - 1).pos;
                }

                for (int j = mScrollPositionList.size() - 1; j >= i-1 && j>=0; j--) {
                    mScrollPositionList.remove(j);
                }
            }
        }

        mPreviousPath = path;
        return pos;
    }

    @Override
    public boolean onRefreshFileList(String path, FileSortHelper sort) {
        if (optionMenu != null) {
            mFileViewInteractionHub.onPrepareOptionsMenu(optionMenu);
        }
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        final File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        final int pos = computeScrollPosition(path);
        final ArrayList<FileInfo> fileList = mFileNameList;
        fileList.clear();

        if (isFirst) {
            isFirst=false;
            LoadlistDataTask listData = new LoadlistDataTask(file,fileList,sort,pos);
            listData.execute();
        } else {
            File[] listFiles = file.listFiles(mFileCagetoryHelper.getFilter());
              if (listFiles == null)
                  return true;

              for (File child : listFiles) {
                // do not show selected file if in move state
                if (mFileViewInteractionHub.inMoveState() && mFileViewInteractionHub.isFileSelected(child.getPath()))
                    continue;

                String absolutePath = child.getAbsolutePath();
                if (Util.isNormalFile(absolutePath) && Util.shouldShowFile(absolutePath)) {
                    FileInfo lFileInfo = Util.GetFileInfo(child,
                        mFileCagetoryHelper.getFilter(), Settings.instance().getShowDotAndHiddenFiles());
                    if (lFileInfo != null && !lFileInfo.fileName.equals("droi")) {
                        fileList.add(lFileInfo);
                    }
                }
            }

            mTagPath = file.getAbsolutePath();
            sortCurrentList(sort);
            mChildEmpty = fileList.size() == 0;
            showEmptyView(mChildEmpty);

            mFileListView.post(new Runnable() {
            @Override
            public void run() {
                mFileListView.setSelection(pos);
            }
        });
        }

        return true;
    }

    @Override
    public void onRefreshMenu(boolean operate) {
        isOperate=operate;
        if (optionMenu != null) {
            if (operate) {
                optionMenu.removeItem(16);
                optionMenu.removeItem(100);
                optionMenu.removeItem(117);
                optionMenu.removeItem(15);
                optionMenu.removeItem(102);
            }

            mFileViewInteractionHub.onPrepareOptionsMenu(optionMenu);
        }
    }
    private void updateUI() {
        showVolumesList(false);
        boolean isCurMounted = StorageHelper.getInstance(mActivity).isCurrentVolumeMounted();
        if (!isCurMounted) {
            return;
        }
        showPathGalleryNavbar(true);
        showListView(true);
        showMemoryNotAvailable(false, null);
        mFileViewInteractionHub.refreshFileList();
        mActivity.invalidateOptionsMenu();
    }

    //get Sd listener to refresh listener
    private void notifyFileChanged(Boolean flag){
        showVolumesList(false);
        updateVolumeSwitchState();
        if (flag) {
            if (isMemoryNotAvailableShow()) {
                initVolumeState();
                showPathGalleryNavbar(true);
                showListView(true);
                showMemoryNotAvailable(false, null);
            }
        } else {
            //adapter SD state
            String state = Util.getDefaultState();
            if (!state.equals(Environment.MEDIA_MOUNTED)) {
                showPathGalleryNavbar(false);
                showListView(false);
                showMemoryNotAvailable(true, mActivity.getString(R.string.storage_device_umouonted));
            } else {
                initVolumeState();
                showPathGalleryNavbar(true);
                showListView(true);
                showMemoryNotAvailable(false, null);
            }
        }
    }

    private void showEmptyView(boolean show) {
        View emptyView = mRootView.findViewById(R.id.empty_view);
        if (emptyView != null){
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            mFileViewInteractionHub.showEditMenuOrNot(!show);
        }
    }

    private void showMemoryNotAvailable(boolean show, String text) {
        LinearLayout memoryNotLayout = (LinearLayout)mRootView.findViewById(R.id.memory_not_available_page);
        TextView view = (TextView)mRootView.findViewById(R.id.memory_not_available_txt);
        if (view != null) {
            if (show) {
                showEmptyView(false);
                view.setText(text);
                memoryNotLayout.setVisibility(View.VISIBLE);
            } else {
                memoryNotLayout.setVisibility(View.GONE);
            }
        }
    }

    private boolean isMemoryNotAvailableShow() {
        LinearLayout memoryNotLayout = (LinearLayout)mRootView.findViewById(R.id.memory_not_available_page);
        return memoryNotLayout.getVisibility() == View.VISIBLE;
    }

    private void showListView(boolean show) {
        if (mFileListView != null) {
            mFileListView.setVisibility(show == true? View.VISIBLE: View.GONE);
        }
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
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onPick(FileInfo f) {
        try {
            Intent intent = Intent.parseUri(Uri.fromFile(new File(f.filePath)).toString(), 0);
            mActivity.setResult(Activity.RESULT_OK, intent);
            mActivity.finish();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
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
    public String getDisplayPath(String paramString) {
        if (paramString.startsWith(this.mVolumePath))
            paramString = this.mVolumeDescription + paramString.substring(this.mVolumePath.length());
        return paramString;
    }

    @Override
    public String getRealPath(String paramString) {
        if (paramString.startsWith(this.mVolumeDescription))
            paramString = this.mVolumePath + paramString.substring(this.mVolumeDescription.length());
        return paramString;
    }

    @Override
    public boolean shouldHideMenu(int menu) {
        return false;
    }

    public void copyFile(ArrayList<FileInfo> files) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.onOperationCopy(files);
        }
    }

    public void moveToFile(ArrayList<FileInfo> files) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.moveFileFrom(files);
        }
    }

    public interface SelectFilesCallback {
        // files equals null indicates canceled
        void selected(ArrayList<FileInfo> files);
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.startSelectFiles(callback);
        }
    }

    @Override
    public FileIconHelper getFileIconHelper() {
        return mFileIconHelper;
    }

    public boolean setPath(String location) {
        Log.i(LOG_TAG, "location:"+location);

        StorageVolume storageVolume = Util.getMountedStorageBySubPath(mActivity, location);
        if (storageVolume == null) {
            return false;
        }
        initVolumeState(storageVolume);
        if (mFileViewInteractionHub != null) {
            mFileViewInteractionHub.setCurrentPath(location);
            mFileViewInteractionHub.refreshFileList();
        }
        return true;
    }

    @Override
    public FileInfo getItem(int pos) {
        if (pos < 0 || pos > mFileNameList.size() - 1)
            return null;
        return mFileNameList.get(pos);
    }

    @SuppressWarnings("unchecked")
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
    public ArrayList<FileInfo> getAllFiles() {
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
    public int getItemCount() {
        return mFileNameList.size();
    }

    @Override
    public void runOnUiThread(Runnable r) {
        mActivity.runOnUiThread(r);
    }

    @Override
    public void hideVolumesList() {
        if (View.VISIBLE == getVolumesListVisibility()) {
            showVolumesList(false);
        }
    }

    private void registerHomeBroadcastReceiver() {
        this.mHomeBroadcastReceiver = new HomeBroadcastReceiver();
        this.mHomeBroadcastReceiver.setOnHomeBroadcastListener(new HomeBroadcastListener() {
            @Override
            public void onReceiveListener() {
                FileExplorerViewFragment.this.finish();
            }
        });
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mActivity.registerReceiver(this.mHomeBroadcastReceiver, iFilter);
    }

    private void unregisterHomeBroadcastReceiver() {
        if (this.mHomeBroadcastReceiver != null) {
            mActivity.unregisterReceiver(this.mHomeBroadcastReceiver);
            this.mHomeBroadcastReceiver = null;
        }
    }

    private class LoadDataTask extends AsyncTask<Void,Integer,StorageVolume> {

        @Override
        protected StorageVolume doInBackground(Void... params) {
            StorageVolume data = StorageHelper.getInstance(mActivity).getLatestMountedVolume();
            return data;
        }
        @Override
        protected void onPostExecute(StorageVolume integer) {
            initVolumeState(integer);

            //Deal bundle from favorite list click item
            if (getArguments() != null) {
                String local_path = getArguments().getString("current_favorite_location");
                if (local_path != null) {
                    setPath(local_path);
                } else if (mFromSafe) {
                    mFileViewInteractionHub.setFromSafe(true);
                }
            }
        }
    }

    class LoadlistDataTask extends AsyncTask<Void,Integer,ArrayList<FileInfo>>{
        private Context context;
        File datafileList;
        ArrayList<FileInfo> fileList;
        FileSortHelper msort;
        int pos;
        LoadlistDataTask(File file, ArrayList<FileInfo> fileLists, FileSortHelper sort, int mpos) {
            datafileList = file;
            fileList=fileLists;
            msort = sort;
            pos =mpos;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
               loadDialog.setMessage(mActivity.getResources().getString(R.string.load_data));
               if (!mActivity.isFinishing()) {
                   loadDialog.show();
                   loadDialog.setCancelable(false);
               }
            if (mTagPath!=null) {
                if (!mTagPath.equals(datafileList.getAbsolutePath())) {
                    fileList.clear();
                    sortCurrentList(msort);
                }
            }
        }

        @Override
        protected ArrayList<FileInfo> doInBackground(Void... params) {
                   ArrayList<FileInfo> fileLists = new  ArrayList<FileInfo>();
                File[] listFiles = datafileList.listFiles(mFileCagetoryHelper.getFilter());

            if (listFiles != null) {
                for (File child : listFiles) {
                    if (mFileViewInteractionHub.inMoveState()
                            && mFileViewInteractionHub.isFileSelected(child
                                    .getPath()))
                        continue;
                    String absolutePath = child.getAbsolutePath();
                    if (Util.isNormalFile(absolutePath)
                            && Util.shouldShowFile(absolutePath)) {
                        FileInfo lFileInfo = Util.GetFileInfo(child,
                                mFileCagetoryHelper.getFilter(), Settings
                                        .instance().getShowDotAndHiddenFiles());

                        if (lFileInfo != null && !lFileInfo.fileName.equals("droi")) {
                            fileLists.add(lFileInfo);
                        }
                    }
               }
            } else {
                this.cancel(true);
            }
            return fileLists;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected void onPostExecute(ArrayList<FileInfo> integer) {
            mTagPath = datafileList.getAbsolutePath();
            if (fileList.isEmpty() && !mChildEmpty) {
                int size = integer.size();
                for (int i = 0; i < size; i++) {
                    fileList.add(integer.get(i));
                }
                fileList = integer;
            }

            if (loadDialog != null && loadDialog.isShowing()) {
                loadDialog.cancel();
            }
            sortCurrentList(msort);
            showEmptyView(fileList.size() == 0);
            mFileListView.post(new Runnable() {
                @Override
                public void run() {
                    mFileListView.setSelection(pos);
                }
            });

            mFileViewInteractionHub.setRefresh(true);
        }
    }

    //get Sd listener to refresh progressBar
    @Override
    public void onMountStateChange(int flag) {
        if(flag == SDCardChangeListener.flag_INJECT){
            notifyFileChanged(true);
        }else{
            notifyFileChanged(false);
        }
    }

    @Override
    public void finish() {
        if (mRootView != null) {
            if (mFromSafe) {
                Intent intent = new Intent();
                if (mFileViewInteractionHub.getCurrentPath() != null) {
                    if (mFileViewInteractionHub.getDecryption()) {
                        intent.putExtra(DECRYPTION_PATH, mFileViewInteractionHub.getCurrentPath());
                    }
                    mActivity.setResult(Activity.RESULT_OK, intent);
                }
            }
            mActivity.finish();
        }
    }
}
