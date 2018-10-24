package com.freeme.filemanager.util;

import android.app.ActivityManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.os.RemoteException;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.freeme.filemanager.R;
import com.freeme.filemanager.view.GarbageCleanupItem;
import com.freeme.filemanager.view.garbage.CleanListItem;
import com.freeme.filemanager.view.garbage.DirectorySizeDetector;
import com.freeme.filemanager.view.garbage.EmptyDirCleaner;

public class AsyncGarbageCleanupHelper {

    public interface GarbageCleanupStatesListener {
        void onScanGarbageFinish(Map<Integer, List<GarbageItem>> mChildMap);

        void onFinish(int i, long l, int j);

        void onUpdateUI(int i);

        void updateCleanProgress(int progress);
    }

    public static final int ACTION_GARBAGE_CACHE      = 0;
    public static final int ACTION_GARBAGE_MEMORY  = 1;
    public static final int ACTION_GARBAGE_APKS   = 2;
    public static final int ACTION_GARBAGE_TEMPFILE   = 3;
    public static final int ACTION_GARBAGE_APPS = 4;

    public static final int STATE_DONE           = 0;
    public static final int STATE_START_SCAN     = 1;
    public static final int STATE_SCAN_FINISH    = 2;
    public static final int STATE_START_CLEANUP  = 3;
    public static final int STATE_CLEANUP_FINISH = 4;

    private static final String LOG_TAG = AsyncGarbageCleanupHelper.class.getSimpleName();

    private int mApplicationInfoCount;
    private int mDeletedFileCount = 0;
    private int mFileCount;
    private int mState = 0;

    private long mFileSize;
    private long mDeletedFileSize = 0;

    private String mInternalPath = Util.MEMORY_DIR;
    private String mExternalPath = Util.SD_DIR;

    private boolean mRunning;

    private Map<Integer, List<GarbageItem>> mChildMap;
    private ArrayList<Integer> mActionList = new ArrayList();

    private List mCacheGarbageList;
    private List mMemoryGarbageList;
    private List mApkGarbageList;
    private List mSystemTempList;
    private List mAppGarbageList;

    private List mApplicationInfoList;

    List<GarbageItem>[] mCleanupItemList = new ArrayList[Util.GARBAGE_ITEMS];
    List<CleanListItem> mCleanupItemList0;

    private Map<String, Long> mApplicationSizeMap;

    private AtomicInteger mCacheSizeObserverCount = new AtomicInteger();
    private AtomicInteger mCacheCleanupObserverCount = new AtomicInteger();

    private GarbageCleanupItem mCacheCleanupStatusItem;

    private Context mContext;

    private GarbageCleanupStatesListener mListener;

    private Object mLock = new Object();

    private PackageManager mPackageManager;

    private final IPackageDataObserver.Stub mCacheCleanupObserver = new IPackageDataObserver.Stub() {
        @Override
        public void onRemoveCompleted(String s, boolean flag)
                throws RemoteException {
            mCacheCleanupObserverCount.addAndGet(1);
            if (flag && (mApplicationSizeMap.get(s) > 0)) {
                synchronized (mCacheCleanupStatusItem) {
                    mDeletedFileCount += 1;
                    mDeletedFileSize += mApplicationSizeMap.get(s);
                }
            }
        }
    };

    public AsyncGarbageCleanupHelper() {
    }

    public AsyncGarbageCleanupHelper(Context context) {
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        if (mChildMap == null) {
            mChildMap = new HashMap<Integer, List<GarbageItem>>();
        }
    }

    public void destroyAysntask() {
        stopRunning();
    }

    private CleanupItemInfo execute(int index, boolean delete) {
        if (!mRunning) {
            return null;
        }

        switch (index) {
            case ACTION_GARBAGE_CACHE:
                return cacheCleanup(delete);
            case ACTION_GARBAGE_MEMORY:
                return memoryGarbageCleanup(delete);
            case ACTION_GARBAGE_APKS:
                return apkGarbageCleanup(delete);
            case ACTION_GARBAGE_TEMPFILE:
                return systemTempFileCleanup(delete);
            case ACTION_GARBAGE_APPS:
                return appGarbageCleanup(delete);
            default:
                return null;
        }
    }

    /**
     * Method for clean cache files.
     *
     * @param flag
     * @return CleanupItemInfo
     */
    private CleanupItemInfo cacheCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }

        if (!flag) {
            scanCacheGarbage();
        }
        if (mCacheGarbageList != null && mCacheGarbageList.size() > 0) {
            if (flag) {
                LinkedList linkedlist = new LinkedList(mCacheGarbageList);
                List cacheGarbageList = mCleanupItemList[ACTION_GARBAGE_CACHE];
                if (cacheGarbageList != null && !cacheGarbageList.isEmpty()) {
                    Iterator iterator = cacheGarbageList.iterator();
                    while (iterator.hasNext() && mRunning) {
                        GarbageItem garbageitem = (GarbageItem) iterator.next();
                        mPackageManager.deleteApplicationCacheFiles(garbageitem.path, mCacheCleanupObserver);
                        mCacheGarbageList.remove(garbageitem);
                        if (linkedlist.remove(garbageitem)) {
                            mDeletedFileCount += 1;
                            mDeletedFileSize += garbageitem.size;
                        }
                    }
                    mCacheGarbageList.size();
                    mChildMap.put(ACTION_GARBAGE_CACHE, mCacheGarbageList);
                }
            } else {
                Iterator iterator3 = mCacheGarbageList.iterator();
                int count = 0;
                long length = 0;
                while (iterator3.hasNext() && mRunning) {
                    GarbageItem garbageitem1 = (GarbageItem) iterator3.next();
                    count += 1;
                    length += garbageitem1.size;
                }
                cleanupiteminfo = new CleanupItemInfo(count, length);
            }
        }

        return cleanupiteminfo;
    }

    private CleanupItemInfo memoryGarbageCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }
        if (!flag) {
            scanMemoryGarbage();
        }

        if (mMemoryGarbageList != null && mMemoryGarbageList.size() > 0) {
            if (flag) {
                LinkedList linkedlist = new LinkedList(mMemoryGarbageList);
                List memoryGarbageList = mCleanupItemList[ACTION_GARBAGE_MEMORY];
                if (memoryGarbageList != null && !memoryGarbageList.isEmpty()) {
                    Iterator iterator = memoryGarbageList.iterator();
                    ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                    while (iterator.hasNext() && mRunning) {
                        GarbageItem garbageitem = (GarbageItem) iterator.next();
                        am.killBackgroundProcesses(garbageitem.rootPath);
                        if (linkedlist.remove(garbageitem)) {
                            mMemoryGarbageList.remove(garbageitem);
                            mDeletedFileCount += 1;
                            mDeletedFileSize += garbageitem.size;
                        }
                    }
                    mChildMap.put(ACTION_GARBAGE_MEMORY, mMemoryGarbageList);
                }
            } else {
                Iterator iterator3 = mMemoryGarbageList.iterator();
                int count = 0;
                long length = 0;
                while (iterator3.hasNext() && mRunning) {
                    GarbageItem garbageitem1 = (GarbageItem) iterator3.next();
                    count += 1;
                    length += garbageitem1.size;
                }
                cleanupiteminfo = new CleanupItemInfo(count, length);
            }
        }
        return cleanupiteminfo;
    }

    private CleanupItemInfo apkGarbageCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }
        if (!flag) {
            scanApkGarbage();
        }
        if (mApkGarbageList != null && mApkGarbageList.size() > 0) {
            if (flag) {
                LinkedList linkedlist = new LinkedList(mApkGarbageList);
                List apkGarbageCleanupList = mCleanupItemList[ACTION_GARBAGE_APKS];
                if (apkGarbageCleanupList != null && !apkGarbageCleanupList.isEmpty()) {
                    Iterator iterator = apkGarbageCleanupList.iterator();
                    while (iterator.hasNext() && mRunning) {
                        GarbageItem garbageitem = (GarbageItem) iterator.next();
                        boolean isDeleted = DeleteFile(garbageitem.rootPath + garbageitem.path);
                        if (isDeleted == true) {
                            mApkGarbageList.remove(garbageitem);
                            long length = garbageitem.size;
                            if (linkedlist.remove(garbageitem)) {
                                mDeletedFileCount += 1;
                                mDeletedFileSize += length;
                            }
                        }
                    }
                    mChildMap.put(ACTION_GARBAGE_APKS, mApkGarbageList);
                }
            } else {
                Iterator iterator3 = mApkGarbageList.iterator();
                int count = 0;
                long length = 0;
                while (iterator3.hasNext() && mRunning) {
                    GarbageItem garbageitem1 = (GarbageItem) iterator3.next();
                    count += 1;
                    length += garbageitem1.size;
                }
                cleanupiteminfo = new CleanupItemInfo(count, length);
            }
        }
        return cleanupiteminfo;
    }

    private CleanupItemInfo systemTempFileCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }
        if (!flag) {
            scanSystemTempFiles();
        }
        if (mSystemTempList != null && mSystemTempList.size() > 0) {
            if (flag) {
                LinkedList linkedlist = new LinkedList(mSystemTempList);
                List tempFileGarbageCleanupList = mCleanupItemList[ACTION_GARBAGE_TEMPFILE];
                if (tempFileGarbageCleanupList != null && !tempFileGarbageCleanupList.isEmpty()) {
                    Iterator iterator = tempFileGarbageCleanupList.iterator();
                    while (iterator.hasNext() && mRunning) {
                        GarbageItem garbageitem = (GarbageItem) iterator.next();
                        boolean isDeleted = DeleteFile(garbageitem.rootPath + garbageitem.path);
                        if (isDeleted == true) {
                            mSystemTempList.remove(garbageitem);
                            long length = garbageitem.size;
                            if (linkedlist.remove(garbageitem)) {
                                mDeletedFileCount += 1;
                                mDeletedFileSize += length;
                            }
                        }
                    }
                    mChildMap.put(ACTION_GARBAGE_TEMPFILE, mSystemTempList);
                }
            } else {
                Iterator iterator3 = mSystemTempList.iterator();
                int count = 0;
                long length = 0;
                while (iterator3.hasNext() && mRunning) {
                    GarbageItem garbageitem1 = (GarbageItem) iterator3.next();
                    count += 1;
                    length += garbageitem1.size;
                }
                cleanupiteminfo = new CleanupItemInfo(count, length);
            }
        }
        return cleanupiteminfo;
    }

    /**
     * This method used to clean the garbage of uninstalled apps
     *
     * @param flag
     * @return
     */
    private CleanupItemInfo appGarbageCleanup(boolean flag) {
        CleanupItemInfo cleanupiteminfo = null;
        if (!mRunning) {
            return null;
        }
        if (!flag) {
            scanAppGarbage();
        }
        if (mAppGarbageList != null && mAppGarbageList.size() > 0) {
            if (flag) {
                LinkedList linkedlist = new LinkedList(mAppGarbageList);
                List appGarbageCleanupList = mCleanupItemList[ACTION_GARBAGE_APPS];
                if (appGarbageCleanupList != null && !appGarbageCleanupList.isEmpty()) {
                    Iterator iterator = appGarbageCleanupList.iterator();
                    while (iterator.hasNext() && mRunning) {
                        GarbageItem garbageitem = (GarbageItem) iterator.next();

                        boolean isDeleted = DeleteFile(garbageitem.rootPath + garbageitem.path);
                        if (isDeleted == true) {
                            mAppGarbageList.remove(garbageitem);
                            long length = garbageitem.size;
                            if (linkedlist.remove(garbageitem)) {
                                mDeletedFileCount += 1;
                                mDeletedFileSize += length;
                            }
                        }
                    }
                }
                mChildMap.put(ACTION_GARBAGE_APPS, mAppGarbageList);
            } else {
                Iterator iterator3 = mAppGarbageList.iterator();
                int count = 0;
                long length = 0;
                while (iterator3.hasNext() && mRunning) {
                    GarbageItem garbageitem1 = (GarbageItem) iterator3.next();
                    count += 1;
                    length += (new DirectorySizeDetector(garbageitem1.rootPath + garbageitem1.path)).getSize();
                }
                cleanupiteminfo = new CleanupItemInfo(count, length);
            }
        }
        mListener.onScanGarbageFinish(mChildMap);
        return cleanupiteminfo;
    }

    /**
     * Subclass of IPackageStatsObserver.Stub
     */
    private class CacheSizeObserver extends IPackageStatsObserver.Stub {
        private boolean mDelete;
        private ApplicationInfo mInfo;

        public CacheSizeObserver(ApplicationInfo applicationinfo, boolean delete) {
            super();
            this.mInfo = applicationinfo;
            this.mDelete = delete;
        }

        public void onGetStatsCompleted(PackageStats packagestats, boolean succeeded) {
            synchronized (mLock) {
                if (!mRunning) {
                    mLock.notifyAll();
                    return;
                }
            }
            mCacheSizeObserverCount.addAndGet(1);
            long fileSize = 0;
            if (succeeded) {
                mApplicationSizeMap.put(packagestats.packageName, packagestats.cacheSize + packagestats.externalCacheSize);

                String childSummary = mContext.getString(R.string.garbage_child_summary_default);
                fileSize = packagestats.cacheSize + packagestats.externalCacheSize;
                if (fileSize > 0) {
                    GarbageItem garbageItem = new GarbageItem(mInfo.loadIcon(mPackageManager),
                            mInfo.loadLabel(mPackageManager).toString(), childSummary,
                            mInfo.packageName, null, fileSize);
                    mCacheGarbageList.add(garbageItem);
                }
            }
            if (mCacheSizeObserverCount.intValue() <= mApplicationInfoCount || mDelete) {
                if (fileSize > 0) {
                    mCacheCleanupStatusItem.mFileCount += 1;
                    mCacheCleanupStatusItem.mSizeCount += fileSize;
                }
            }
            if (mCacheSizeObserverCount.intValue() == mApplicationInfoCount) {
                synchronized (mLock) {
                    mLock.notifyAll();
                }
            }

        }
    }

    private void getAppCachedStat(Context context, String packageName) {
        StorageStatsManager ssm = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
        long totalCache = 0;
        Drawable apkIcon = null;
        String apkLabel = null;
        Boolean hasCache = false;
        try {
            ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
            if (info != null) {
                final UUID uuid = mPackageManager.getApplicationInfo(packageName, 0).storageUuid;
                StorageStats storageStats = ssm.queryStatsForPackage(uuid, packageName, Process.myUserHandle());
                apkIcon = info.loadIcon(mPackageManager);
                apkLabel = info.loadLabel(mPackageManager).toString();
                if (storageStats != null) {
                    totalCache += storageStats.getCacheBytes();

                    Context opContext = context.createPackageContext(packageName,
                            Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                    File[] dir_exts = opContext.getExternalCacheDirs();
                    for (int i = 0; i < dir_exts.length; i++ ) {
                        if (dir_exts[i] != null && dir_exts[i].list() != null && dir_exts[i].list().length > 0) {
                            hasCache = true;
                        } else {
                            continue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "queryStatsForPackage error: " + e.toString());
        }

        String childSummary = mContext.getString(R.string.garbage_child_summary_default);
        GarbageItem garbageItem = new GarbageItem(apkIcon, apkLabel, childSummary,
                packageName, null, totalCache);
        if (totalCache != 0 && hasCache) {
            mApplicationSizeMap.put(packageName, totalCache);
            mCacheGarbageList.add(garbageItem);

            mCacheCleanupStatusItem.mFileCount += 1;
            mCacheCleanupStatusItem.mSizeCount += totalCache;
        }
    }

    protected boolean DeleteFile(String filePath) {
        if (!mRunning) {
            return false;
        }
        boolean isDeleted = false;
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            for (File child : file.listFiles()) {
                DeleteFile(child.getAbsolutePath());
            }
        }
        if (mRunning && file.delete()) {
            isDeleted = true;
        }
        return isDeleted;
    }

    private boolean checkApkInstall(String packageName) {
        boolean installed = false;
        try {
            mPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    private CleanupItemInfo emptyDirCleanup(File file, boolean flag) {
        EmptyDirCleaner emptydircleaner = new EmptyDirCleaner(file, flag, false);
        if (flag) {
            mDeletedFileCount += emptydircleaner.emptyDeleteCount();
            mDeletedFileSize += emptydircleaner.emptyDeleteSize();
        }
        return new CleanupItemInfo(emptydircleaner.emptyDirCount(),
                emptydircleaner.sizeCount());
    }

    private CleanupItemInfo emptyDirCleanup(boolean flag) {
        int count = 0;
        long size = 0;
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            CleanupItemInfo cleanupiteminfo = emptyDirCleanup(internalFile, flag);
            count = cleanupiteminfo.fileCount;
            size = cleanupiteminfo.fileSize;
        }

        if (!TextUtils.isEmpty(mExternalPath)) {
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                CleanupItemInfo cleanupiteminfo1 = emptyDirCleanup(externalFile, flag);
                count += cleanupiteminfo1.fileCount;
                size += cleanupiteminfo1.fileSize;
            }
        }
        return new CleanupItemInfo(count, size);
    }

    private void resetDeletedMarkParam() {
        if (mActionList != null)
            mActionList.clear();
    }

    private void scanAppGarbage() {
        if (mAppGarbageList == null) {
            mAppGarbageList = new LinkedList();
        }
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            scanAppGarbage(mInternalPath);
        }

        if (!TextUtils.isEmpty(mExternalPath)) {
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                scanAppGarbage(mExternalPath);
            }
        }
        mChildMap.put(ACTION_GARBAGE_APPS, mAppGarbageList);
    }

    /**
     * This method do work for scan app garbage
     *
     * @param volumePath
     */
    private void scanAppGarbage(String volumePath) {
        File file = new File(volumePath);
        if (!file.exists()) {
            return;
        }

        File afile[] = file.listFiles();
        if (afile != null && afile.length > 0) {
            CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(mContext);
            SQLiteDatabase db = dbHelper.openDatabase();
            try {
                ArrayList<String> whiteList = CleanupUtil.getWhiteList(mContext);
                for (int i = 0; i < afile.length && mRunning; i++) {
                    if (!afile[i].isDirectory()) {
                        continue;
                    }
                    getPackageNameByPath(db, whiteList, volumePath, afile[i].getAbsolutePath());
                }
            } finally {
                db.close();
            }
        }
    }

    private void scanMemoryGarbage() {
        if (mMemoryGarbageList == null) {
            mMemoryGarbageList = new LinkedList();
        }

        HashMap<String, GarbageItem> memoryGarbageMap = new HashMap<>();
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
        ApplicationInfo appInfo = null;

        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessList) {
            Debug.MemoryInfo[] memoryInfos = am.getProcessMemoryInfo(new int[]{appProcessInfo.pid});
            long size = memoryInfos[0].getTotalPrivateDirty() * 1024;
            String pkgName = appProcessInfo.pkgList[0];

            if (memoryGarbageMap.containsValue(pkgName)) {
                GarbageItem updateItem = memoryGarbageMap.get(pkgName);
                updateItem.size = updateItem.size + size;
            } else {
                try {
                    appInfo = mPackageManager.getApplicationInfo(pkgName, 0);
                    if (appInfo == null) {
                        continue;
                    }
                    if (inWhiteList(appInfo)) {
                        continue;
                    }

                    Drawable appIcon = appInfo.loadIcon(mPackageManager);
                    String appName = appInfo.loadLabel(mPackageManager).toString();
                    GarbageItem garbageItem = new GarbageItem(appIcon, appName,
                            mContext.getString(R.string.garbage_child_summary_default),
                            appProcessInfo.pid + "", pkgName,
                            size);
                    memoryGarbageMap.put(pkgName, garbageItem);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
        if (!memoryGarbageMap.isEmpty()) {
            mMemoryGarbageList = new ArrayList<GarbageItem> (memoryGarbageMap.values());
        }
        mChildMap.put(ACTION_GARBAGE_MEMORY, mMemoryGarbageList);
    }

    private boolean inWhiteList(ApplicationInfo appInfo) {
        return mContext.getPackageName().equals(appInfo.packageName)
                || (appInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0;
    }

    private void scanCacheGarbage() {
        mCacheCleanupStatusItem = new GarbageCleanupItem();
        mApplicationSizeMap = new HashMap();
        mApplicationInfoList = mPackageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        HashSet runningAppHashSet = new HashSet();
        List runningProcesList = ((ActivityManager) mContext.getSystemService("activity")).getRunningAppProcesses();
        if (runningProcesList != null) {
            Iterator iterator = runningProcesList.iterator();
            if (iterator != null) {
                while (iterator.hasNext() && mRunning) {
                    // get the all packages that have been loaded into the process.
                    String[] as = ((ActivityManager.RunningAppProcessInfo) iterator.next()).pkgList;
                    for (int i = 0; i < as.length; i++) {
                        runningAppHashSet.add(as[i]);
                    }
                }
            }

            mCacheSizeObserverCount.set(0);
            mCacheCleanupObserverCount.set(0);
            mApplicationInfoCount = mApplicationInfoList.size();

            if (mCacheGarbageList == null) {
                mCacheGarbageList = new LinkedList();
            }

            for (int i = 0; i < mApplicationInfoList.size(); i++) {
                ApplicationInfo applicationinfo = (ApplicationInfo) mApplicationInfoList.get(i);
                if (!runningAppHashSet.contains(applicationinfo.packageName)) {
                    String name = applicationinfo.packageName;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getAppCachedStat(mContext, name);
                    } else {
                        mPackageManager.getPackageSizeInfo(name, new CacheSizeObserver(applicationinfo, false));
                    }
                } else {
                    mCacheSizeObserverCount.addAndGet(1);
                    mCacheCleanupObserverCount.addAndGet(1);
                }
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                synchronized (mLock) {
                    try {
                        mLock.wait(20000L);
                    } catch (InterruptedException interruptedexception) {
                        interruptedexception.printStackTrace();
                    }
                }
            }
            mChildMap.put(ACTION_GARBAGE_CACHE, mCacheGarbageList);
        }
    }

    /**
     * Get garbageItems and put them into mAppGarbageList
     *
     * @param db
     * @param whiteList
     * @param volumePath
     * @param absolutePath
     */
    private void getPackageNameByPath(SQLiteDatabase db, ArrayList<String> whiteList, String volumePath, String absolutePath) {
        String folderPath = absolutePath.substring(volumePath.length());
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("select path,package_name,name from package as a,app as b on a.app_id = b._id where a.path like'"
                    + folderPath + "%'", null);
            while (cursor.moveToNext()) {
                String relativePath = cursor.getString(0);
                String packageName = cursor.getString(1);
                // if the relativePath in whiteList or the packageName is empty
                // or null, then to continue
                if (whiteList.contains(relativePath) || TextUtils.isEmpty(packageName)) {
                    continue;
                }
                // if one of packageNames is installed, then to break, for this
                // relativePath is used by an app
                if (checkApkInstall(packageName) == true) {
                    break;
                }
                long itemLength = (new DirectorySizeDetector(volumePath + cursor.getString(0))).getSize();
                GarbageItem garbageItem = new GarbageItem(cursor.getString(2), cursor.getString(0), volumePath, itemLength);
                // if the garbageItem isValidate garbage
                if (isGarbageItemExist(garbageItem)) {
                    mAppGarbageList.add(garbageItem);
                    break;
                }
            }
        } catch (Exception e) {
            Log.i(LOG_TAG, "scanAppGarbage(), query package_path table catch exception: " + e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean isGarbageItemExist(GarbageItem garbageitem) {
        if (garbageitem != null) {
            String filePath = garbageitem.rootPath + garbageitem.path;
            File file = new File(filePath);
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }

    private void scanApkGarbage() {
        if (mApkGarbageList == null) {
            mApkGarbageList = new LinkedList();
        }
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            scanApkGarbage(mInternalPath);
        }

        if (!TextUtils.isEmpty(mExternalPath)) {
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                scanApkGarbage(mExternalPath);
            }
        }

        mChildMap.put(ACTION_GARBAGE_APKS, mApkGarbageList);
    }

    private void scanApkGarbage(String volumePath) {
        File file = new File(volumePath);
        if (!file.exists()) {
            return;
        }
        File afile[] = file.listFiles();
        if (afile != null && afile.length > 0) {
            for (int i = 0; i < afile.length && mRunning; i++) {
                String filePath = afile[i].getAbsolutePath();
                if (!afile[i].isDirectory()) {
                    String ext = MimeUtils.getRealExtension(filePath);
                    if (ext.equals("apk")) {
                        getApksListByPath(filePath);
                    }
                } else {
                    scanApkGarbage(filePath);
                }
            }
        }
    }

    private void getApksListByPath(String filePath) {
        try {
            PackageInfo packageArchiveInfo = mPackageManager.getPackageArchiveInfo(filePath, PackageManager.GET_PERMISSIONS);
            if (packageArchiveInfo != null) {
                ApplicationInfo appInfo = packageArchiveInfo.applicationInfo;
                appInfo.sourceDir = filePath;
                appInfo.publicSourceDir = filePath;
                String apkLabel = appInfo.loadLabel(mPackageManager).toString();
                Drawable apkIcon = appInfo.loadIcon(mPackageManager);
                String childSummary = mContext.getString(R.string.garbage_child_summary_uninstall);
                try {
                    mPackageManager.getPackageInfo(appInfo.packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES);
                    childSummary = mContext.getString(R.string.garbage_child_summary_install);
                } catch (PackageManager.NameNotFoundException e) {
                    childSummary = mContext.getString(R.string.garbage_child_summary_uninstall);
                }

                long size = new File(filePath).length();
                GarbageItem garbageItem = new GarbageItem(apkIcon, apkLabel, childSummary, filePath, "", size);
                // if the garbageItem isValidate garbage
                if (isGarbageItemExist(garbageItem)) {
                    mApkGarbageList.add(garbageItem);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private void scanSystemTempFiles() {
        if (mSystemTempList == null) {
            mSystemTempList = new LinkedList();
        }
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            scanSystemTempFiles(mInternalPath);
        }

        if (!TextUtils.isEmpty(mExternalPath)) {
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                scanSystemTempFiles(mExternalPath);
            }
        }
        mChildMap.put(ACTION_GARBAGE_TEMPFILE, mSystemTempList);
    }

    private void scanSystemTempFiles(String volumePath) {
        File file = new File(volumePath);
        if (!file.exists()) {
            return;
        }
        ArrayList<String> tempDirs = CleanupUtil.getTempFolderList(mContext);
        if (!TextUtils.isEmpty(volumePath)) {
            for (int i = 0; i < tempDirs.size(); i++) {
                file = new File((new StringBuilder()).append(volumePath).append(tempDirs.get(i)).toString());
                if (file.exists()) {
                    long size = file.length();
                    GarbageItem garbageItem = new GarbageItem(file.getName(), tempDirs.get(i), volumePath, size);
                    mSystemTempList.add(garbageItem);
                }
            }
        }
    }

    private CleanupItemInfo thumbnailCleanup(File file, boolean flag) {
        int count = 0;
        long size = 0;
        if (file.exists()) {
            File[] afile = file.listFiles();
            if (afile != null && afile.length > 0) {
                for (int i = 0; i < afile.length; i++) {
                    File file1 = afile[i];
                    long length = file1.length();
                    count += 1;
                    size += length;
                    if (file1.delete()) {
                        mDeletedFileCount += 1;
                        mDeletedFileSize += length;
                    }
                }
            }
        }
        StringBuilder builder = new StringBuilder()
                .append("thumbnailCleanup mDeletedFileSize = ")
                .append(mDeletedFileSize).append(";length = ")
                .append(size);

        Log.v(LOG_TAG, builder.toString());
        return new CleanupItemInfo(count, size);
    }

    private CleanupItemInfo thumbnailCleanup(boolean flag) {
        int count = 0;
        long size = 0;
        File internalFile = new File(mInternalPath);
        if (internalFile.exists()) {
            CleanupItemInfo cleanupiteminfo1 = thumbnailCleanup(
                    new File((new StringBuilder()).append(mInternalPath).append("/DCIM/.thumbnails").toString()), flag);
            count = cleanupiteminfo1.fileCount;
            size = cleanupiteminfo1.fileSize;
        }

        if (!TextUtils.isEmpty(mExternalPath)) {
            File externalFile = new File(mExternalPath);
            if (externalFile.exists()) {
                CleanupItemInfo cleanupiteminfo2 = thumbnailCleanup(
                        new File((new StringBuilder()).append(mExternalPath).append("/DCIM/.thumbnails").toString()), flag);
                count += cleanupiteminfo2.fileCount;
                size += cleanupiteminfo2.fileSize;
            }
        }
        return new CleanupItemInfo(count, size);
    }

    public void cleanUp() {
        mRunning = true;
        if (mState == STATE_DONE) {
            mState = STATE_START_SCAN;
        } else if (mState == STATE_SCAN_FINISH) {
            mState = STATE_START_CLEANUP;
        }
        mListener.onUpdateUI(mState);
        new GarbageCleanupThread().start();
    }

    public int getState() {
        return mState;
    }

    public void setState(int i) {
        mState = i;
    }

    public int getTotalDeletedFileCount() {
        return mDeletedFileCount;
    }

    public long getTotalDeletedFileSize() {
        return mDeletedFileSize;
    }

    public int getTotalFileCount() {
        return mFileCount;
    }

    public long getTotalFileSize() {
        return mFileSize;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void resetDeletedParam() {
        mDeletedFileCount = 0;
        mDeletedFileSize = 0L;
    }

    public void setActionOperate(ArrayList arraylist) {
        mActionList = arraylist;
    }

    public void setGarbageCleanupItem(List<GarbageItem>[] list) {
        if (mActionList == null) {
            mActionList = new ArrayList(Util.GARBAGE_ITEMS);
            for (int i = 0; i < Util.GARBAGE_ITEMS; i++) {
                mActionList.add(i);
            }
        } else {
            for (int i = 0; i < Util.GARBAGE_ITEMS; i++) {
                if (!mActionList.contains(i)) {
                    mActionList.add(i);
                }
            }
        }
        mCleanupItemList = list;
    }

    public void setGarbageCleanupItem(List<CleanListItem> list) {
        if (mActionList == null) {
            mActionList = new ArrayList(Util.GARBAGE_ITEMS);
            for (int i = 0; i < Util.GARBAGE_ITEMS; i++) {
                mActionList.add(i);
            }
        } else {
            for (int i = 0; i < Util.GARBAGE_ITEMS; i++) {
                if (!mActionList.contains(i)) {
                    mActionList.add(i);
                }
            }
        }
        mCleanupItemList0 = list;
    }

    public void setGarbageCleanupStatesListener(
            GarbageCleanupStatesListener garbagecleanupstateslistener) {
        mListener = garbagecleanupstateslistener;
    }

    public void stopRunning() {
        mRunning = false;
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    public class GarbageCleanupThread extends Thread {
        public GarbageCleanupThread() {

        }

        public void run() {
            operateAction();
            if (mState == STATE_START_SCAN) {
                mState = STATE_SCAN_FINISH;
            }
            mListener.onUpdateUI(mState);
            mRunning = false;
        }

        private void operateAction() {
            if (mActionList != null && mActionList.size() > 0) {
                mFileCount = 0;
                mFileSize = 0;
                if (mState == STATE_START_SCAN) {
                    for (int i = 0; i < Util.GARBAGE_ITEMS; i++) {
                        CleanupItemInfo cleanupiteminfo = null;
                        for (int j = 0; j < mActionList.size(); j++) {
                            if (i == mActionList.get(j)) {
                                cleanupiteminfo = execute(mActionList.get(j), false);
                            }
                        }
                        if (cleanupiteminfo != null) {
                            mFileCount = mFileCount + cleanupiteminfo.fileCount;
                            mFileSize = mFileSize + cleanupiteminfo.fileSize;
                            mListener.onFinish(i, cleanupiteminfo.fileSize, cleanupiteminfo.fileCount);

                        } else {
                            mListener.onFinish(i, 0, 0);
                        }
                    }
                } else if (mState == STATE_START_CLEANUP) {
                    new IAsyncTask(mCleanupItemList0).execute();
                }
            }
        }
    }

    private class IAsyncTask extends AsyncTask<Void, Integer, Void> {
        List<CleanListItem> mCleanList;
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int mMax;
        int mOldProgress;
        int mNewProgress;
        long mIntervalTime;
        private IAsyncTask(List<CleanListItem> list) {
            mCleanList = list;
            mMax = list.size();
        }

        protected Void doInBackground(Void... params) {
            for (int i = 0; i < mCleanList.size(); i++) {
                switch (mCleanList.get(i).getArraryId()) {
                    case ACTION_GARBAGE_CACHE:
                        mPackageManager.deleteApplicationCacheFiles(mCleanList.get(i).getPath(), mCacheCleanupObserver);
                        break;
                    case ACTION_GARBAGE_MEMORY:
                        am.killBackgroundProcesses(mCleanList.get(i).getRootPath());
                        break;
                    case ACTION_GARBAGE_APKS:
                    case ACTION_GARBAGE_TEMPFILE:
                    case ACTION_GARBAGE_APPS:
                        mRunning = true;
                        DeleteFile(mCleanList.get(i).getRootPath() + mCleanList.get(i).getPath());
                        break;
                    default:
                        break;
                }
                mDeletedFileCount += 1;
                mDeletedFileSize += mCleanList.get(i).getSize();

                mNewProgress = mDeletedFileCount*100/mMax;
                if (mNewProgress > mOldProgress) {
                    mOldProgress = mNewProgress;
                    publishProgress(mNewProgress);
                }
            }
            return null;
        }

        protected void onCancelled() {
        }

        protected void onPostExecute(Void paramVoid) {
            mRunning = false;
            mState = STATE_CLEANUP_FINISH;
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    mListener.onUpdateUI(mState);
                }
            };
            Timer timer = new Timer();
            timer.schedule(task, 1000);
        }

        @Override
        protected void onPreExecute() {
            mRunning = true;
        }
        @Override
        protected void onProgressUpdate(Integer... args2) {
            mListener.updateCleanProgress(args2[0]);
        }
    }

    public class GarbageItem {
        public String appName;
        public Drawable appIcon;
        public String childSummary;
        public String path;
        public String rootPath;
        public long size;

        public GarbageItem(String appName, String path, String rootPath, long length) {
            super();
            this.appIcon = mContext.getDrawable(R.drawable.clean_icon_default);
            this.appName = appName;
            this.childSummary = mContext.getString(R.string.garbage_child_summary_default);
            this.path = path;
            this.rootPath = rootPath;
            this.size = length;
        }

        public GarbageItem(Drawable appIcon, String appName, String childSummary, String path, String rootPath, long length) {
            super();
            this.appIcon = appIcon;
            this.appName = appName;
            this.childSummary = childSummary;
            this.path = path;
            this.rootPath = rootPath;
            this.size = length;
        }
    }

    public class CleanupItemInfo {

        final AsyncGarbageCleanupHelper cleanHelper;
        public int fileCount;
        public long fileSize;

        public CleanupItemInfo(int count, long size) {
            super();
            cleanHelper = AsyncGarbageCleanupHelper.this;
            this.fileCount = count;
            this.fileSize = size;
        }
    }
}
