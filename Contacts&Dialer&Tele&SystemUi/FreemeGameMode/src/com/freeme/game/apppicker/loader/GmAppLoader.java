package com.freeme.game.apppicker.loader;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.view.View;

import com.freeme.game.database.GmDatabaseConstant;
import com.freeme.game.utils.GmAppConfigManager;

public class GmAppLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final Comparator<GmAppModel> COMPARATOR = new Comparator<GmAppModel>() {

        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(GmAppModel object1, GmAppModel object2) {
            int ret = sCollator.compare(object1.getAppName(), object2.getAppName());
            if (ret != 0) {
                return ret;
            }
            ret = sCollator.compare(object1.getPkgName(), object2.getPkgName());
            if (ret != 0) {
                return ret;
            }
            return 0;
        }
    };

    public static final int LOAD_TYPE_SELECTED_APP = 1;
    public static final int LOAD_TYPE_UNSELECTED_APP = 2;

    private Context mContext;
    private PackageManager mPackageManager;
    private UserManager mUserManager;
    private GmAppConfigManager mAppConfigManager;
    private ExecutorService mThreadPool;
    private LoaderManager mLoaderManager;

    private View mLoadingContainer;

    private ArraySet<String> mSelectedApps;
    private List<GmAppModel> mSelectedAppList = new ArrayList<>();
    private List<GmAppModel> mUnSelectedAppList = new ArrayList<>();
    private Handler mHandler;

    public GmAppLoader(Context context, PackageManager pm, Handler handler,
                       LoaderManager loaderManager) {
        mContext = context;
        mPackageManager = pm;
        mHandler = handler;
        mLoaderManager = loaderManager;

        mUserManager = UserManager.get(context);
        mAppConfigManager = new GmAppConfigManager(context);
        mThreadPool = Executors.newCachedThreadPool();

        mSelectedApps = new ArraySet<>();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(mContext, GmDatabaseConstant.CONTENT_URI,
                new String[]{GmDatabaseConstant.Columns.COLUMN_APP_PACKAGE_NAME},
                GmDatabaseConstant.Columns.COLUMN_APP_SELECTED + " != 0",
                null, null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, Cursor cursor) {
        mSelectedApps.clear();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                mSelectedApps.add(cursor.getString(0));
            }
        }

        mThreadPool.execute(() -> {
            loadAppListByType(loader.getId());
            mLoading = false;
            onPostExecute();
            mHandler.sendEmptyMessage(loader.getId());
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mSelectedApps.clear();
    }

    public void setLoadingContainer(View loadingContainer) {
        mLoadingContainer = loadingContainer;
    }

    public void initData(int loadType) {
        mLoading = true;
        onPreExecute();

        mLoaderManager.restartLoader(loadType, null, this);
    }

    private boolean mLoading;
    private boolean mHasShowProgress;

    public boolean isLoading() {
        return mLoading;
    }

    private void onPreExecute() {
        mHasShowProgress = false;
        mHandler.postDelayed(mShowProgressRunnable, 300);
    }

    private void onPostExecute() {
        if (mHasShowProgress) {
            mHandler.post(mHideProgressRunnable);
        }
    }

    private Runnable mShowProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mLoading) {
                return;
            }
            mHasShowProgress = true;
            if (mLoadingContainer != null) {
                mLoadingContainer.setVisibility(View.VISIBLE);
            }
        }
    };

    private Runnable mHideProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLoadingContainer != null) {
                mLoadingContainer.setVisibility(View.GONE);
            }
        }
    };

    private void loadAppListByType(int loadType) {
        switch (loadType) {
            case LOAD_TYPE_SELECTED_APP:
                loadSelectedAppList();
                break;
            case LOAD_TYPE_UNSELECTED_APP:
                loadUnSelectedAppList();
                break;
            default:
                break;
        }
    }

    private List<ResolveInfo> getResolveInfos() {
        List<ResolveInfo> resolveInfos;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveInfos = new ArrayList<>();
        for (UserInfo user : mUserManager.getProfiles(UserHandle.myUserId())) {
            resolveInfos.addAll(mPackageManager.queryIntentActivitiesAsUser(
                    intent, 0, user.id));
        }

        return resolveInfos;
    }

    private void loadSelectedAppList() {
        mSelectedAppList.clear();

        List<ResolveInfo> infos = getResolveInfos();

        for (ResolveInfo info : infos) {
            // String activityName = info.activityInfo.name;
            String pkgName = info.activityInfo.packageName;
            String appName = (String) info.loadLabel(mPackageManager);
            if (!mAppConfigManager.packageExcludeFilter(pkgName)) {
                boolean contains = mSelectedApps.contains(pkgName);
                if (contains) {
                    GmAppModel appModel = new GmAppModel(pkgName, appName, true);
                    appModel.setAppIcon(getBadgedIcon(mPackageManager, info));
                    mSelectedAppList.add(appModel);
                }
            }
        }
        if (!mSelectedAppList.isEmpty()) {
            Collections.sort(mSelectedAppList, COMPARATOR);
        }
    }

    private void loadUnSelectedAppList() {
        mUnSelectedAppList.clear();

        List<ResolveInfo> infos = getResolveInfos();

        for (ResolveInfo info : infos) {
            // String activityName = info.activityInfo.name;
            String pkgName = info.activityInfo.packageName;
            String appName = (String) info.loadLabel(mPackageManager);
            if (!mAppConfigManager.packageExcludeFilter(pkgName)) {
                boolean contains = mSelectedApps.contains(pkgName);
                if (!contains) {
                    GmAppModel appModel = new GmAppModel(pkgName, appName, false);
                    appModel.setAppIcon(getBadgedIcon(mPackageManager, info));
                    mUnSelectedAppList.add(appModel);
                }
            }
        }
        if (!mUnSelectedAppList.isEmpty()) {
            Collections.sort(mUnSelectedAppList, COMPARATOR);
        }
    }

    private Drawable getBadgedIcon(PackageManager pm, ResolveInfo resolveInfo) {
        ApplicationInfo info = resolveInfo.activityInfo.applicationInfo;
        return pm.getUserBadgedIcon(pm.loadUnbadgedItemIcon(info, info),
                new UserHandle(UserHandle.getUserId(info.uid)));
    }


    public List<GmAppModel> getAppListByType(int loadType) {
        switch (loadType) {
            case LOAD_TYPE_SELECTED_APP:
                return mSelectedAppList;
            case LOAD_TYPE_UNSELECTED_APP:
                return mUnSelectedAppList;
            default:
                return new ArrayList<>();
        }
    }
}
