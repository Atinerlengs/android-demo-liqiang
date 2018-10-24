package com.freeme.applock.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.provider.Settings.System;

import com.freeme.applock.R;
import com.freeme.provider.FreemeSettings;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import static android.content.Context.MODE_PRIVATE;

public class PackageInfoUtil {
    public static final String TAG = PackageInfoUtil.class.getSimpleName();
    public static final String APPLOCK_STATUS_CHANGED_PERMISSION = "com.freeme.applock.permission.STATUSCHANGED";
    public static final String LOCKED_CLASSES = FreemeSettings.Secure.FREEME_APPLOCK_LOCKED_APPS_CLASSES;
    public static final String LOCKED_PACKAGE = FreemeSettings.Secure.FREEME_APPLOCK_LOCKED_APPS_PACKAGES;
    public static final String EMPTY_FOLDER_NAME = "";
    public static final String LOCKED_FOLDERS = "smartmanager_locked_apps_folders";
    public static final String PACKAGE_ADD = "package_add";
    public static final String PACKAGE_REMOVE  = "package_remove";
    public static final String PREF_NAME = "com.freeme.applock.appInfo";
    public static final String PREF_PACKAGE_ONLY = "com.freeme.applock.pakcgeInfo";
    public static final String MARK_PREF_NAME = "com.freeme.applock.mark";

    private static final boolean DEBUG = true;
    private static final boolean DEBUG_MORE = false;

    private ArrayList<AppInfo> appList = new ArrayList<>();
    private StringBuilder mLockedClasses = new StringBuilder();
    private StringBuilder mLockedPackages = new StringBuilder();
    private HashMap<String, ArrayList<String>> mMappingList = new HashMap<>();
    private HashMap<String, String> mFolderList = new HashMap<>();
    private ArrayList<String> mWhiteList = new ArrayList<>();
    private int mAppCount;
    private int mLockedAppCount;

    enum BoardcastType {
        MASTER_ENABLE,
        FOLDER_CHANGE,
        LOCKED_APP_STATUS
    }

    private static class PackageUtilHolder {
        private static final PackageInfoUtil INSTANCE = new PackageInfoUtil();

        private PackageUtilHolder() {
        }
    }

    enum State {
        ADD,
        REMOVE
    }

    public static PackageInfoUtil getInstance() {
        return PackageUtilHolder.INSTANCE;
    }

    private PackageInfoUtil() {
    }

    public ArrayList<AppInfo> getLauncherApps(Context context) {
        appList.clear();
        mMappingList.clear();
        mWhiteList = getWhiteList(context);
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 0)) {
            ActivityInfo activityInfo = info.activityInfo;
            AppInfo tmpInfo = new AppInfo();
            tmpInfo.appName = activityInfo.loadLabel(packageManager).toString();
            tmpInfo.appIcon = activityInfo.loadIcon(packageManager);
            tmpInfo.packageName = activityInfo.packageName;
            tmpInfo.mainActivity = activityInfo.name;
            tmpInfo.locked = isPkgLocked(context, tmpInfo.packageName) ? 1 : 0;
            if (!isWhiteList(tmpInfo.packageName)) {
                appList.add(tmpInfo);
                ArrayList<String> mainClasses = new ArrayList<>();
                if (mMappingList.containsKey(tmpInfo.packageName)) {
                    mainClasses = mMappingList.get(tmpInfo.packageName);
                }
                mainClasses.add(tmpInfo.mainActivity);
                mMappingList.put(tmpInfo.packageName, mainClasses);
            }
        }
        LogUtil.i(TAG, "appList.size = " + appList.size());
        return appList;
    }

    public void getAppsInfo(Context context) {
        mMappingList.clear();
        mWhiteList = getWhiteList(context);
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : packageManager.queryIntentActivities(intent, 0)) {
            ActivityInfo activityInfo = info.activityInfo;
            String packageName = activityInfo.packageName;
            if (!isWhiteList(packageName)) {
                String className = activityInfo.name;
                ArrayList<String> mainClasses = new ArrayList<>();
                if (mMappingList.containsKey(packageName)) {
                    mainClasses = mMappingList.get(packageName);
                }
                mainClasses.add(className);
                mMappingList.put(packageName, mainClasses);
            }
        }
    }

    public boolean isWhiteList(String pkgName) {
        for (String whitePkg : mWhiteList) {
            if (pkgName.equals(whitePkg)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<AppInfo> getAppList() {
        sortList();
        return appList;
    }

    public void sortList() {
        final Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(appList, new Comparator<AppInfo>() {
            public int compare(AppInfo lhs, AppInfo rhs) {
                int ret = lhs.locked - rhs.locked;
                if (ret != 0) {
                    return ret > 0 ? -1 : 1;
                } else {
                    ret = collator.compare(lhs.appName, rhs.appName);
                    if (ret != 0) {
                        return ret > 0 ? 1 : -1;
                    } else {
                        return 0;
                    }
                }
            }
        });
    }

    public void saveLockState(Context context, Boolean isRemovePackage, String removePackage) {
        mLockedPackages.delete(0, mLockedPackages.length());
        mLockedClasses.delete(0, mLockedClasses.length());
        mLockedAppCount = 0;
        Map<String, Integer> keys = (Map<String, Integer>)getSharedPreferences(context, PREF_PACKAGE_ONLY).getAll();
        boolean masterSwitch = getMasterValue(context);
        for (Entry<String, Integer> entry : keys.entrySet()) {
            if (entry.getValue() == 1) {
                String packageInfo = entry.getKey();
                if (isRemovePackage && packageInfo.equals(removePackage)) {
                    removePackageState(context, removePackage);
                } else if (masterSwitch) {
                    mLockedPackages.append(packageInfo);
                    mLockedPackages.append(",");
                    if (mMappingList.containsKey(packageInfo)) {
                        for (String c : mMappingList.get(packageInfo)) {
                            mLockedClasses.append(c);
                            mLockedClasses.append(",");
                            mLockedAppCount++;
                        }
                    }
                }
            }
        }
        if (mLockedPackages.length() != 0) {
            mLockedPackages.delete(mLockedPackages.length() - 1, mLockedPackages.length());
        }
        if (mLockedClasses.length() != 0) {
            mLockedClasses.delete(mLockedClasses.length() - 1, mLockedClasses.length());
        }
    }

    public void saveDBValue(Context context) {
        Secure.putString(context.getContentResolver(), LOCKED_PACKAGE, mLockedPackages.toString());
        Secure.putString(context.getContentResolver(), LOCKED_CLASSES, mLockedClasses.toString());
        String getlockedPackages = Secure.getString(context.getContentResolver(), LOCKED_PACKAGE);
        LogUtil.i(TAG, "saveDBValue: LOCKED_PACKAGE=" + getlockedPackages
                + "\n LOCKED_CLASSES=" + Secure.getString(context.getContentResolver(), LOCKED_CLASSES));
        saveLockedAppsCount(context, getLockedAppsCount());
    }

    public int countLockedApps(Context context) {
        Map<String, ?> keys = getSharedPreferences(context, PREF_NAME).getAll();
        mLockedAppCount = 0;
        if (getMasterValue(context)) {
            for (Entry<String, ?> entry : keys.entrySet()) {
                if (((Integer) entry.getValue()) == 1) {
                    mLockedAppCount++;
                }
            }
        }
        return mLockedAppCount;
    }

    public void updateDB(Context context, Boolean isRemovePackage, String removePackage) {
        if (isRemovePackage) {
            saveLockState(context, true, removePackage);
        } else {
            saveLockState(context, false, null);
        }
        saveDBValue(context);
    }

    public void loadDBState(Context context) {
        String lockedPackageString = Secure.getString(context.getContentResolver(), LOCKED_PACKAGE);
        if (lockedPackageString != null) {
            String[] lockedPackages = lockedPackageString.split(",");
            LogUtil.i(TAG, "loadDBState: getlockedPackages=" + Arrays.toString(lockedPackages));
            Editor editor1 = getEditor(context, PREF_PACKAGE_ONLY);
            editor1.clear();
            editor1.apply();
            Editor editor2 = getEditor(context, PREF_NAME);
            editor2.clear();
            editor2.apply();
            for (String packageInfo : lockedPackages) {
                if (!isWhiteList(packageInfo)) {
                    storePackageState(context, packageInfo);
                }
            }
        }
    }

    public void updateDBForRemove(Context context, String packageInfo) {
        getAppsInfo(context);
        String lockedPackageString = Secure.getString(context.getContentResolver(), LOCKED_PACKAGE);
        if (lockedPackageString == null) {
            LogUtil.i(TAG, "no app was locked");
            return;
        }
        ArrayList<String> lockedPackageList = new ArrayList<>(Arrays.asList(lockedPackageString.split(",")));
        int locked_num = 0;
        if (lockedPackageList.contains(packageInfo)) {
            lockedPackageList.remove(packageInfo);
            StringBuilder storedPackage = new StringBuilder();
            StringBuilder storedClass = new StringBuilder();
            if (lockedPackageList.size() > 0) {
                for (String str : lockedPackageList) {
                    storedPackage.append(str);
                    storedPackage.append(",");
                    if (mMappingList.containsKey(str)) {
                        for (String c : mMappingList.get(str)) {
                            storedClass.append(c);
                            storedClass.append(",");
                            locked_num++;
                        }
                    }
                }
                storedPackage.delete(storedPackage.length() - 1, storedPackage.length());
                storedClass.delete(storedClass.length() - 1, storedClass.length());
            }
            Secure.putString(context.getContentResolver(), LOCKED_PACKAGE, storedPackage.toString());
            Secure.putString(context.getContentResolver(), LOCKED_CLASSES, storedClass.toString());
            LogUtil.i(TAG, "updateDBForRemove: LOCKED_PACKAGE=" + storedPackage.toString()
                    + "\n LOCKED_CLASSES=" + storedClass.toString());
            saveLockedAppsCount(context, locked_num);
        }
    }

    public void storePackageState(Context context, String pkgName) {
        Editor editor = getEditor(context, PREF_PACKAGE_ONLY);
        editor.putInt(pkgName, 1);
        editor.apply();
        getSamePkgInfo(context, pkgName, State.ADD);
    }

    public void removePackageState(Context context, String pkgName) {
        Editor editor = getEditor(context, PREF_PACKAGE_ONLY);
        editor.remove(pkgName);
        editor.apply();
        getSamePkgInfo(context, pkgName, State.REMOVE);
    }

    public void getSamePkgInfo(Context context, String pkgName, State states) {
        Editor editor = getSharedPreferences(context, PREF_NAME).edit();
        StringBuilder storedName = new StringBuilder();
        if (mMappingList.containsKey(pkgName)) {
            for (String c :  mMappingList.get(pkgName)) {
                storedName.delete(0, storedName.length());
                storedName.append(pkgName);
                storedName.append(",");
                storedName.append(c);
                switch (states) {
                    case REMOVE:
                        editor.remove(storedName.toString());
                        break;
                    default:
                        editor.putInt(storedName.toString(), 1);
                        break;
                }
            }
        }
        editor.apply();
    }

    public String getStoredName(AppInfo appInfo) {
        StringBuilder result = new StringBuilder();
        result.append(appInfo.packageName);
        result.append(",");
        result.append(appInfo.mainActivity);
        return result.toString();
    }

    public ArrayList<String> getWhiteList(Context context) {
        return new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.applock_white_list_pkg)));
    }

    public SharedPreferences getSharedPreferences(Context context, String pref) {
        return context.getSharedPreferences(pref, MODE_PRIVATE);
    }

    public Editor getEditor(Context context, String pref) {
        return context.getSharedPreferences(pref, MODE_PRIVATE).edit();
    }

    public int getAppsCount() {
        mAppCount = appList.size();
        return mAppCount;
    }

    public int getLockedAppsCount() {
        return mLockedAppCount;
    }

    public void saveLockedAppsCount(Context context, int number) {
        System.putInt(context.getContentResolver(), "locked_app_count", number);
        LogUtil.i(TAG, "app locked_num = " + number);
    }

    public void updatePackageChanged(Context context, int status, Intent intent) {
        String packageInfo = intent.getData().toString().split(":")[1];
        if (status == AppLockUtil.STATES_PACKAGE_REMOVED) {
            updateDBForRemove(context, packageInfo);
            mark(context, PACKAGE_REMOVE, DEBUG);
        } else if (status == AppLockUtil.STATES_PACKAGE_ADD) {
            mark(context, PACKAGE_ADD, DEBUG);
        }
    }

    public void updateDBStatus(Context context, String lastLockedPackage) {
        Secure.putString(context.getContentResolver(), LOCKED_PACKAGE, lastLockedPackage);
    }

    public void mark(Context context, String markTag, boolean value) {
        Editor editor = context.getSharedPreferences(MARK_PREF_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(markTag, value);
        editor.apply();
    }

    public boolean getMarkValue(Context context, String markTag, boolean defaultValue) {
        return getSharedPreferences(context, MARK_PREF_NAME).getBoolean(markTag, defaultValue);
    }

    public void lockAll(Context context) {
        Editor editor1 = getEditor(context, PREF_NAME);
        Editor editor2 = getEditor(context, PREF_PACKAGE_ONLY);
        for (AppInfo appInfo : appList) {
            editor1.putInt(getStoredName(appInfo), 1);
            editor2.putInt(appInfo.packageName, 1);
        }
        editor1.commit();
        editor2.commit();
    }

    public void unlockAll(Context context) {
        Editor editor1 = getEditor(context, PREF_NAME);
        editor1.clear();
        editor1.commit();
        Editor editor2 = getEditor(context, PREF_PACKAGE_ONLY);
        editor2.clear();
        editor2.commit();
    }

    public boolean getMasterValue(Context context) {
        return Secure.getInt(context.getContentResolver(), FreemeSettings.Secure.FREEME_APPLOCK_ENABLED, 0) == 1;
    }

    public void setMasterValue(Context context, boolean value) {
        ContentResolver contentResolver = context.getContentResolver();
        String str = FreemeSettings.Secure.FREEME_APPLOCK_ENABLED;
        Secure.putInt(contentResolver, str, value ? 1 : 0);
        Editor editor = context.getSharedPreferences(MARK_PREF_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(AppLockUtil.MASTER_SWITCH, value);
        editor.apply();
        sendMsgToLauncher(context, value, BoardcastType.MASTER_ENABLE);
    }

    public void loadFolderInfo(Context context) {
        mFolderList.clear();
        String folderInfo1 = System.getString(context.getContentResolver(), LOCKED_FOLDERS);
        LogUtil.i(TAG, "folderInfo = " + folderInfo1);
        if (folderInfo1 != null && folderInfo1.length() > 0) {
            for (String str : folderInfo1.split(";")) {
                String[] folder = str.split("-");
                if (folder.length == 1) {
                    mFolderList.put(folder[0], "");
                } else {
                    mFolderList.put(folder[0], folder[1]);
                }
            }
        }
    }

    public void sendMsgToLauncher(Context context, Object subjectInfo, BoardcastType type) {
        Bundle bundle = new Bundle();
        switch (type) {
            case FOLDER_CHANGE:
                bundle.putString("android.intent.extra.SUBJECT", (String) subjectInfo);
                sendStatusBroadcast(context, bundle, AppLockUtil.FOLDERLOCK_ACTION);
                break;
            case MASTER_ENABLE:
                bundle.putBoolean("android.intent.extra.SUBJECT", ((Boolean) subjectInfo));
                sendStatusBroadcast(context, bundle, AppLockUtil.APPLOCK_ENABLE_ACTION);
                break;
            default:
                break;
        }
    }

    public void sendStatusBroadcast(Context context, Bundle bundle, String destination) {
        /*/ freeme.yangzhengguang, 20180626. remove unused code
        LogUtil.i(TAG, "sendStatusBroadcast: " + destination);
        Intent intent = new Intent(destination);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        context.sendBroadcast(intent, APPLOCK_STATUS_CHANGED_PERMISSION);
        //*/
    }

    public boolean isPkgLocked(Context context, String pkgName) {
        return getSharedPreferences(context, PREF_PACKAGE_ONLY).getInt(pkgName, MODE_PRIVATE) == 1;
    }

    public String getFolderInfo(Context context, String storedName) {
        return mFolderList.get(storedName);
    }

    public void removeFolderInfo(Context context, String storedName) {
        if (mFolderList.containsKey(storedName)) {
            mFolderList.remove(storedName);
            sendMsgToLauncher(context, storedName, BoardcastType.FOLDER_CHANGE);
        }
    }

    public boolean isFolderLocked(Context context, String storedName) {
        return mFolderList.containsKey(storedName);
    }
}
