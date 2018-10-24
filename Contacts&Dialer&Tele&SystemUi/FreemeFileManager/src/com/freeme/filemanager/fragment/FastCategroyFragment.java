package com.freeme.filemanager.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freeme.filemanager.FMApplication;
import com.freeme.filemanager.FMApplication.SDCardChangeListener;
import com.freeme.filemanager.FMIntent;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.activity.cleanup.CleanupMainActivity;
import com.freeme.filemanager.activity.StorageCategoryActivity;
import com.freeme.filemanager.controller.CategoryItemAdapter;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.AsyncGarbageCleanupHelper;
import com.freeme.filemanager.util.FavoriteDatabaseHelper.FavoriteDatabaseListener;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileCategoryHelper.CategoryInfo;
import com.freeme.filemanager.util.FileCategoryHelper.FileCategory;
import com.freeme.filemanager.util.PermissionUtil;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.util.Util.SDCardInfo;
import com.freeme.filemanager.view.FavoriteList;
import com.freeme.filemanager.view.FileCategoryItem;
import com.freeme.filemanager.view.ListItemTextView;
import com.freeme.filemanager.view.Settings;
import com.freeme.filemanager.view.memoryprogress.MemoryProgressLayout;
import com.freeme.safe.password.UnlockComplexActivity;
import com.freeme.safe.password.UnlockPasswordActivity;
import com.freeme.safe.password.UnlockPatternActivity;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.security.KeyStore.getApplicationContext;
import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_TAG;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TOTAL;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_USE;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_CLEAN;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_FREE;
import static com.freeme.filemanager.FMIntent.EXTRA_STORAGE_TYPE;

public class FastCategroyFragment extends BaseFragment implements
        FavoriteDatabaseListener, IBackPressedListener, SDCardChangeListener,
        OnItemClickListener {

    private static final String LOG_TAG = "FastCategroyFragment";

    public static final String BACKSTACK_TAG = "tag_fastcategory_fragment";

    private static final int SET_CATEGORY_INFO = 1;
    private static final int MSG_INIT = 2;
    private static final int MSG_UPDATE_TXT = 3;

    private static int[] icons = new int[]{
            R.drawable.category_icon_music,
            R.drawable.category_icon_video,
            R.drawable.category_icon_picture,
            R.drawable.category_icon_document,
            R.drawable.category_icon_apk,
            R.drawable.category_icon_zip,
            R.drawable.category_icon_favorite,
            R.drawable.category_icon_qq,
            R.drawable.category_icon_wechat
    };

    private static int[] mTexts = new int[]{
            R.string.category_music,
            R.string.category_video,
            R.string.category_picture,
            R.string.category_document,
            R.string.category_apk,
            R.string.category_zip,
            R.string.category_favorite,
            R.string.category_qq,
            R.string.category_wechat
    };

    private static FileCategory[] categories = {
            FileCategory.Music,
            FileCategory.Video,
            FileCategory.Picture,
            FileCategory.Doc,
            FileCategory.Apk,
            FileCategory.Zip,
            FileCategory.Favorite,
            FileCategory.QQ,
            FileCategory.WeChat
    };
    private static List<FileCategoryItem> sCategoryInfoItems = new ArrayList<FileCategoryItem>();

    static {
        for (int i = 0; i < mTexts.length; i++) {
            FileCategoryItem item = new FileCategoryItem();
            item.textStringId = mTexts[i];
            item.iconId = icons[i];
            item.category = categories[i];
            sCategoryInfoItems.add(item);
        }
    }

    private long mMemoryCardFree;
    private long mMemoryCardTotal;
    private long mSdcardFree;
    private long mSdCardTotal;
    private long mUsbStorageFree;
    private long mUsbStorageTotal;
    private long mMemoryFree = 0;
    private long mMemoryTotal = 0;
    private long mStorageClean;

    private View mRootView;
    private FileExplorerTabActivity mActivity;
    private FMApplication mApplication;
    private FileCategoryHelper mFileCagetoryHelper;
    private AsyncTask<Void, Void, Void> mRefreshCategoryInfoTask;
    private CategoryItemAdapter mCategoryItemAdapter;
    private FavoriteList mFavoriteList;
    private long mPathItemSize;
    private boolean mIsInit;
    private ProgressBar mStorageProgress;
    private TextView mStorageSizeTxt;
    private NotificationManager mNotificationManager;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
                case SET_CATEGORY_INFO:
                    setCategoryInfo();
                    break;
                case MSG_INIT:
                    mIsInit = true;
                    intInfo();
                    break;
                case MSG_UPDATE_TXT:
                    TextView canCleanTrash = mRootView.findViewById(R.id.can_clean_trash);
                    //canCleanTrash.setVisibility(View.VISIBLE);
                    canCleanTrash.setText(Util.convertStorage(mStorageClean)
                            + mActivity.getString(R.string.storage_cleanable));
                    break;
                default:
                    break;
            }
        }
    };

    private  View.OnClickListener layoutClick = new View.OnClickListener(){
        public void onClick(View v) {
            Fragment fragment = null;
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Intent info;

            switch (v.getId()) {
                case R.id.all_storage:
                    fragment = new FileExplorerViewFragment();
                    break;
                case R.id.storage_space: {
                    info = new Intent(mActivity, StorageCategoryActivity.class);
                    info.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    info.putExtra(EXTRA_STORAGE_TOTAL, mMemoryCardTotal);
                    info.putExtra(EXTRA_STORAGE_FREE, mMemoryCardFree);
                    info.putExtra(EXTRA_STORAGE_USE, mMemoryCardTotal - mMemoryCardFree);
                    info.putExtra(EXTRA_STORAGE_TYPE, GlobalConsts.IS_MEMORY_CARD);
                    startActivity(info);
                    return;
                }
                case R.id.trash_clean: {
                    info = new Intent(mActivity, CleanupMainActivity.class);
                    info.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    info.putExtra(EXTRA_STORAGE_TOTAL, mMemoryTotal);
                    info.putExtra(EXTRA_STORAGE_FREE, mMemoryFree);
                    info.putExtra(EXTRA_STORAGE_USE, mMemoryTotal - mMemoryFree);
                    info.putExtra(EXTRA_STORAGE_CLEAN, mStorageClean);
                    startActivity(info);
                    return;
                }
                case R.id.ftp_connect:
                    fragment = new ServerControlFragment();
                    break;
                case R.id.encryption_file: {
                    onActionSafe();
                    return;
                }
                default:
                    break;
            }

            if (fragment != null) {
                mActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
                Util.setBackTitle(mActivity.getActionBar(), getString(R.string.app_name));
                mActivity.getActionBar().setTitle(R.string.tab_file);
                ft.replace(R.id.fragment_container, fragment);
                ft.addToBackStack(BACKSTACK_TAG);
                ft.commitAllowingStateLoss();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mIsInit = false;
        mActivity = (FileExplorerTabActivity) getActivity();

        mRootView = inflater
                .inflate(R.layout.fragment_fast_category, null);
        init();

        mFavoriteList = new FavoriteList(mActivity, this);

        mApplication = (FMApplication) mActivity.getApplication();
        mApplication.addSDCardChangeListener(this);
        mFileCagetoryHelper = new FileCategoryHelper(mActivity);

        mCategoryItemAdapter = new CategoryItemAdapter(mActivity, sCategoryInfoItems);
        GridView categoryInfo = mRootView.findViewById(R.id.category_info);
        categoryInfo.setOnItemClickListener(this);
        categoryInfo.setFocusable(false);
        categoryInfo.setAdapter(mCategoryItemAdapter);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        scanGarbageItems();
        refreshCategoryInfo();
    }

    private static final long GARBAGE_SIZE_THRESHOLD = 256 * 1024 * 1024;
    private long mGarbageSize;
    private boolean mCleanFinished;

    private void scanGarbageItems() {
        mGarbageSize = 0;
        int size = Util.GARBAGE_ITEMS;
        ArrayList<Integer> list = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }

        final int[] defaultState = getResources().getIntArray(R.array.clean_item_select_default);

        AsyncGarbageCleanupHelper cleanHelper = new AsyncGarbageCleanupHelper(getApplicationContext());
        cleanHelper.setState(AsyncGarbageCleanupHelper.STATE_START_SCAN);
        cleanHelper.setActionOperate(list);
        cleanHelper.setGarbageCleanupStatesListener(new AsyncGarbageCleanupHelper.GarbageCleanupStatesListener() {
            @Override
            public void onUpdateUI(int i) {
            }

            @Override
            public void updateCleanProgress(int progress) {
            }

            @Override
            public void onFinish(int i, long l, int j) {
                if (i < defaultState.length && defaultState[i] == 1) {
                    mGarbageSize += l;
                }
                if (mCleanFinished) {
                    mStorageClean = mGarbageSize;
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_TXT, 0);
                    if (GARBAGE_SIZE_THRESHOLD <= mGarbageSize) {
                        showClearNotification();

                        // reset
                        mGarbageSize = 0;
                        mCleanFinished = false;
                    }
                }
            }

            @Override
            public void onScanGarbageFinish(Map<Integer, List<AsyncGarbageCleanupHelper.GarbageItem>> mChildMap) {
                mCleanFinished = true;
            }
        });
        cleanHelper.cleanUp();
    }

    public void showClearNotification() {
        Intent contentIntent = new Intent(mActivity,
                FileExplorerTabActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        contentIntent.putExtra(EXTRA_STORAGE_TOTAL, mMemoryTotal);
        contentIntent.putExtra(EXTRA_STORAGE_FREE, mMemoryFree);
        contentIntent.putExtra(EXTRA_STORAGE_USE, mMemoryTotal - mMemoryFree);
        contentIntent.putExtra(EXTRA_STORAGE_CLEAN, mStorageClean);
        PendingIntent contentPIntent = PendingIntent.getActivity(
                mActivity, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent buttonIntent = new Intent(Intent.ACTION_MAIN);
        buttonIntent.setClass(mActivity, CleanupMainActivity.class);
        PendingIntent cleanIntent = PendingIntent.getActivity(mActivity,
                1, buttonIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Builder builder = new Notification.Builder(mActivity)
                .setContentIntent(contentPIntent)
                .setContentTitle(mActivity.getString(R.string.notificaiton_clear_title))
                .setContentText(mActivity.getString(R.string.notificaiton_clear_info))
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(false)
                .setColor(mActivity.getResources().getColor(R.color.app_theme_color_accent))
                .addAction(R.drawable.new_ic_menu_clear,
                        mActivity.getString(R.string.notificaiton_clear_btn_text), cleanIntent)
                .setSmallIcon(R.drawable.notification_small_clean);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotiManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE);
            final String channelId = mActivity.getString(R.string.file_clean);
            if (mNotiManager.getNotificationChannel(channelId) == null) {
                final String channelName = channelId;
                NotificationChannel mChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
                mNotiManager.createNotificationChannel(mChannel);
            }
            builder.setChannelId(channelId);
        }

        Notification notify = builder.build();
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(200, notify);
    }

    private void init() {
        if (!mIsInit) {
            mHandler.sendEmptyMessageDelayed(MSG_INIT, 0);
        }
    }
    private void intInfo() {
        RelativeLayout allStorageRl = mRootView.findViewById(R.id.all_storage);
        allStorageRl.setOnClickListener(layoutClick);
        LinearLayout storageSpaceRl = mRootView.findViewById(R.id.storage_space);
        storageSpaceRl.setOnClickListener(layoutClick);
        RelativeLayout trashCleanRl = mRootView.findViewById(R.id.trash_clean);
        trashCleanRl.setOnClickListener(layoutClick);
        ListItemTextView ftpConnectRl = mRootView.findViewById(R.id.ftp_connect);
        ftpConnectRl.setOnClickListener(layoutClick);
        ListItemTextView safeFileRl = mRootView.findViewById(R.id.encryption_file);
        safeFileRl.setOnClickListener(layoutClick);
        mStorageProgress = mRootView.findViewById(R.id.phoneRectview);
        mStorageSizeTxt = mRootView.findViewById(R.id.phoneVolume);
        mNotificationManager = (NotificationManager) mActivity.getSystemService(mActivity.NOTIFICATION_SERVICE);

        if (mActivity.getIntent() != null && FMIntent.ACTION_CLEAR.equals(mActivity.getIntent().getAction())) {
            mNotificationManager.cancelAll();
        }
        initDeviceInfo();
    }

    @Override
    public void pagerUserVisible() {
        super.pagerUserVisible();
        refreshCategoryInfo();
    }

    @Override
    public void onMountStateChange(int flag) {
        refreshCategoryInfo();
    }

    @Override
    public boolean onBack() {
        FragmentManager mFragmentManager = getFragmentManager();
        if (mFragmentManager == null) {
            return false;
        }

        int back_stack_cnt = mFragmentManager.getBackStackEntryCount();
        if (back_stack_cnt == 0) {
            return false;
        } else {
            mFragmentManager.popBackStack();
            String back_tag = mFragmentManager.getBackStackEntryAt(back_stack_cnt -1).getName();

            Activity activity = (FileExplorerTabActivity) getActivity();
            if (back_tag.equals(BACKSTACK_TAG)) {
                activity.getActionBar().setTitle(R.string.app_name);
                activity.getActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                activity.getActionBar().setTitle(R.string.category_picture);
                Util.setBackTitle(activity.getActionBar(), getString(R.string.app_name));
            }
            return true;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        onCategoryItemClick(parent, view, position, id);
    }

    @Override
    public void onFavoriteDatabaseChanged() {
        //do nothing
    }

    public void onCategoryItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileCategoryItem selectItem = sCategoryInfoItems.get(position);
        Fragment fragment;

        switch (selectItem.category) {
            case QQ:
            case WeChat:
                fragment = new FastCategoryPathsDetailsFragment();
                break;
            case Picture:
                fragment = new FastCategoryPictureDetailsFragment();
                break;
            default:
                fragment = new FastCategoryDetailsFragment();
                break;
        }

        mActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
        Util.setBackTitle(mActivity.getActionBar(), getString(R.string.app_name));
        mFileCagetoryHelper.setCurCategory(selectItem.category);
        mActivity.getActionBar().setTitle(mFileCagetoryHelper.getCurCategoryNameResId());

        FragmentManager fm = getFragmentManager();
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRA_CATEGORY_TAG, selectItem.category);
        fragment.setArguments(bundle);
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment);
        ft.addToBackStack(BACKSTACK_TAG);
        ft.commitAllowingStateLoss();
    }

    private void onDataChanged() {
        mHandler.sendEmptyMessage(SET_CATEGORY_INFO);
    }

    private void initDeviceInfo() {
        mMemoryTotal = 0;
        mMemoryFree = 0;

        final MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        if (memoryCardInfo != null) {
            mMemoryCardTotal = memoryCardInfo.total;
            mMemoryCardFree = memoryCardInfo.free;
            mMemoryTotal = mMemoryTotal + mMemoryCardTotal;
            mMemoryFree = mMemoryFree + mMemoryCardFree;
        }

        final SDCardInfo sdCardInfo = Util.getSDCardInfo();
        if (sdCardInfo != null && sdCardInfo.total >= sdCardInfo.free) {
            mSdCardTotal = sdCardInfo.total;
            mSdcardFree = sdCardInfo.free;
            mMemoryTotal = mMemoryTotal + mSdCardTotal;
            mMemoryFree = mMemoryFree + mSdcardFree;
        }

        final Util.UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();
        if (usbStorageInfo != null && usbStorageInfo.total >= usbStorageInfo.free) {
            mUsbStorageTotal = usbStorageInfo.total;
            mUsbStorageFree = usbStorageInfo.free;
            mMemoryTotal = mMemoryTotal + mUsbStorageTotal;
            mMemoryFree = mMemoryFree + mUsbStorageFree;
        }

        MemoryProgressLayout progressLayout = mRootView.findViewById(R.id.phnoe_memory_progress_bar_layout);
        progressLayout.setVisibility(View.VISIBLE);
        updateProgress(mMemoryFree, mMemoryTotal);
    }

    private void refreshStorageDeviceInfo() {
        if (isAdded()) {
            mMemoryTotal = 0;
            mMemoryFree = 0;
            final MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
            if (memoryCardInfo != null) {
                mMemoryTotal = mMemoryTotal + memoryCardInfo.total;
                mMemoryFree = mMemoryFree + memoryCardInfo.free;
            }

            final SDCardInfo sdCardInfo = Util.getSDCardInfo();
            if (sdCardInfo != null && sdCardInfo.total != sdCardInfo.free) {
                mMemoryTotal = mMemoryTotal + sdCardInfo.total;
                mMemoryFree = mMemoryFree + sdCardInfo.free;
            }

            final Util.UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();
            if (usbStorageInfo != null && usbStorageInfo.total != usbStorageInfo.free) {
                mMemoryTotal = mMemoryTotal + usbStorageInfo.total;
                mMemoryFree = mMemoryFree + usbStorageInfo.free;
            }

            updateProgress(mMemoryFree, mMemoryTotal);
        }
    }

    private void updateProgress(long free, long total) {
        mStorageSizeTxt.setText(getString(R.string.storage_available, Util.convertStorage(free)));
        mStorageProgress.setProgress((int)(free*100/total));
    }

    private void refreshCategoryInfo() {
        if (!PermissionUtil.hasStoragePermissions(mActivity.getApplicationContext())) {
            return;
        }

        mRefreshCategoryInfoTask = new AsyncTask<Void, Void, Void>() {

            protected void onPostExecute(Void paramVoid) {
                onDataChanged();
            }

            @Override
            protected Void doInBackground(Void... params) {
                mFileCagetoryHelper.refreshCategoryInfo(
                        GlobalConsts.IS_CATEGORY_FRAGMENT, false);
                mFavoriteList.initList();
                return null;
            }
        };
        mRefreshCategoryInfoTask.execute();
    }

    private void setCategoryInfo() {
        StorageHelper.MountedStorageInfo mountedStorageInfo = StorageHelper
                .getInstance(mActivity).getMountedStorageInfo();
        refreshStorageDeviceInfo();
        if (mountedStorageInfo != null) {
            if (FileCategoryHelper.sCategories != null) {

                for (FileCategoryItem fc : sCategoryInfoItems) {
                    if (fc.category.equals(FileCategory.Favorite)) {
                        fc.count = mFavoriteList.getCount();
                        continue;
                    } else if (fc.category.equals(FileCategory.QQ)
                            || fc.category.equals(FileCategory.WeChat)) {
                        fc.count = getAllFolderSize(fc.category);
                        continue;
                    }

                    CategoryInfo categoryInfo = mFileCagetoryHelper
                            .getCategoryInfos().get(fc.category);
                    if (categoryInfo != null) {
                        if (fc.count != categoryInfo.count) {
                            fc.count = categoryInfo.count;
                        }
                    }
                }

                mCategoryItemAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Get the files num under QQ or wechat
     * @param category FileCategory
     * @return long
     */
    private long getAllFolderSize(FileCategory category){
        if (isAdded()) {
            String defaultPath = Util.MEMORY_DIR;
            String sdCardPath = Util.SD_DIR;
            String[] pathArray;
            if (category == FileCategory.QQ) {
                pathArray = getResources().getStringArray(R.array.qq_path_list);
            } else if (category == FileCategory.WeChat) {
                pathArray = getResources().getStringArray(R.array.wechat_path_list);
            } else {
                pathArray = null;
            }
            if(pathArray == null){
                return 0;
            }

            long total_size = 0;

            int pathListSize = pathArray.length * 2;
            File[] file = new File[pathListSize];
            for(int i = 0; i < pathListSize; i++) {
                if (i < pathListSize/2) {
                    file[i] = new File(defaultPath + "/" + pathArray[i]);
                } else {
                    file[i] = new File(sdCardPath + "/" + pathArray[i - pathListSize/2]);
                }
            }

            for (int i = 0; i < pathListSize; i ++) {
                mPathItemSize = 0;
                total_size = total_size + getFolderSize(file[i]);
            }
            return total_size;
        }
        else return 0;
    }

    private long getFolderSize (File file) {
        String absolutePath="";
        if (!file.exists()) {
            return 0;
        }

        try {
            File[] fileList = file.listFiles();
            int listLen = fileList.length;
            for (int i = 0; i < listLen; i++) {
                absolutePath = fileList[i].getAbsolutePath();
                if (fileList[i].isDirectory()) {
                    if (Util.isNormalFile(absolutePath)
                            && Util.shouldShowFile(absolutePath)) {
                        getFolderSize(fileList[i]);
                    } else {
                        continue;
                    }
                } else {
                    if (Util.isNormalFile(absolutePath)
                            && Util.shouldShowFile(absolutePath)) {
                        FileInfo lFileInfo = Util.GetFileInfo(fileList[i],
                                mFileCagetoryHelper.getFilter(), Settings
                                        .instance().getShowDotAndHiddenFiles());
                        if (lFileInfo != null && !lFileInfo.fileName.equals("droi")) {
                            mPathItemSize++;
                        }
                    } else {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mPathItemSize;
    }

    public void onActionSafe() {
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
        String unlockModePath = SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_MODE_PATH);
        String passWordPath = SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_PASSWORD_PATH);
        int unlockMode = SafeUtils.getLockMode(mActivity, unlockModePath);
        String password = SafeUtils.getPassword(mActivity, passWordPath);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        if (unlockMode == SafeConstants.LOCK_MODE_DEFAULT || password == null) {
            intent.setClass(mActivity, UnlockPasswordActivity.class);
            intent.setAction(SafeConstants.NEW_APP_PROTECT_PASSWORD);
            intent.putExtra(SafeConstants.IS_FIRST_SET, true);
        } else if (unlockMode == SafeConstants.LOCK_MODE_PATTERN) {
            intent.setAction(SafeConstants.APP_UNLOCK_PATTERN_ACTIVITY);
            intent.setClass(mActivity, UnlockPatternActivity.class);
        } else if (unlockMode == SafeConstants.LOCK_MODE_PASSWORD) {
            intent.setAction(SafeConstants.APP_UNLOCK_PASSWORD_ACTIVITY);
            intent.setClass(mActivity, UnlockPasswordActivity.class);
        } else if (unlockMode == SafeConstants.LOCK_MODE_COMPLEX) {
            intent.setAction(SafeConstants.APP_UNLOCK_COMPLEX_ACTIVITY);
            intent.setClass(mActivity, UnlockComplexActivity.class);
        }
        startActivity(intent);
    }
}
