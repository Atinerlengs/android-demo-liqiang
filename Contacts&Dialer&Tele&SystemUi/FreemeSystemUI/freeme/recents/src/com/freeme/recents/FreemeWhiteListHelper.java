package com.freeme.recents;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;

import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.freeme.util.FreemeFeature;

public class FreemeWhiteListHelper {
    private static final String TAG = "FreemeWhiteListHelper";

    private static FreemeWhiteListHelper sFreemeWhiteListHelper;

    private Resources mRes;
    private PackageManager mPm;
    private InputMethodManager mInputManager;
    private ContentResolver mContentResolver;
    private AccessibilityManager mAccessibilityManager;

    private ArrayList<String> mAltasWhiteList = new ArrayList();
    private ArrayList<String> mAccessibilityServiceList;
    private ArrayList<String> mLauncherNames;
    private ArrayList<String> mWidgetNames;
    private ArrayList<String> mInputMethodList = new ArrayList();
    private boolean mNeedReloadWidgetData = true;
    private boolean mNeedReloadLauncherFlag = true;

    private FreemeWhiteListHelper(Context context) {
        Context appContext = context.getApplicationContext();
        mRes = appContext.getResources();
        mPm = appContext.getPackageManager();
        mInputManager = (InputMethodManager)appContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mContentResolver = appContext.getContentResolver();
        mAccessibilityManager = AccessibilityManager.getInstance(appContext);

        loadAltasWhiteList();
    }

    public static FreemeWhiteListHelper getInstance(Context context) {
        if (sFreemeWhiteListHelper == null) {
            sFreemeWhiteListHelper = new FreemeWhiteListHelper(context);
        }
        return sFreemeWhiteListHelper;
    }

    public boolean isNeedForceStop(String pkg) {
        return !(isSystemPackage(pkg)
                    || mAltasWhiteList.contains(pkg)
                    || isLauncher(pkg)
                    || isAccessibilityNames(pkg)
                    || isWidgetNames(pkg)
                    || isInputMethodApp(pkg)
        );
    }

    public boolean isAccessibilityNames(String packageName) {
        if (mAccessibilityServiceList == null) {
            getAllAccessibility();
        }
        return mAccessibilityServiceList.contains(packageName);
    }

    public void getAllAccessibility() {
        if (mAccessibilityServiceList == null) {
            mAccessibilityServiceList = new ArrayList();
        }
        mAccessibilityServiceList.clear();
        try {
            for (AccessibilityServiceInfo info : mAccessibilityManager.getInstalledAccessibilityServiceList()) {
                if (!(info == null || info.getResolveInfo() == null || info.getResolveInfo().serviceInfo == null)) {
                    String pkgName = info.getResolveInfo().serviceInfo.packageName;
                    if (pkgName != null) {
                        mAccessibilityServiceList.add(pkgName);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "getAllServiceList Exception:" + e);
        }
    }

    public boolean isLauncher(String packageName) {
        if (mLauncherNames == null || mNeedReloadLauncherFlag) {
            mNeedReloadLauncherFlag = false;
            getAllLauncherPackagesNames();
        }
        return mLauncherNames.contains(packageName);
    }

    public void getAllLauncherPackagesNames() {
        ArrayList<String> names = new ArrayList();
        for (ResolveInfo res : mPm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY)) {
            names.add(res.activityInfo.packageName);
        }
        if (mLauncherNames != null) {
            mLauncherNames.clear();
        }
        mLauncherNames = names;
    }

    public void loadInputMethods() {
        for (InputMethodInfo info : mInputManager.getInputMethodList()) {
            String packageName = info.getPackageName();
            if (!mInputMethodList.contains(packageName)) {
                mInputMethodList.add(packageName);
            }
        }
    }

    public void checkInputMethodApp(String packageName) {
        boolean isInputMethodApp = false;
        try {
            ServiceInfo[] sInfo = mPm.getPackageInfo(packageName, PackageManager.FLAG_PERMISSION_POLICY_FIXED).services;
            if (sInfo != null) {
                for (ServiceInfo serviceInfo : sInfo) {
                    if (serviceInfo.permission != null && serviceInfo.permission.equals(Manifest.permission.BIND_INPUT_METHOD)) {
                        isInputMethodApp = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "checkInputMethodApp " + e);
        }
        if (isInputMethodApp && mInputMethodList != null) {
            mInputMethodList.add(packageName);
        }
    }

    public boolean isInputMethodApp(String packageName) {
        return (mInputMethodList != null) && mInputMethodList.contains(packageName);
    }

    public boolean isWidgetNames(String packageName) {
        if (mNeedReloadWidgetData) {
            mNeedReloadWidgetData = false;
            getWidgetPackageNames();
        }
        return mWidgetNames.contains(packageName);
    }

    private void getWidgetPackageNames() {
        Cursor c = null;
        mWidgetNames = new ArrayList<String>();
        try {
            c = mContentResolver.query(Uri.parse("content://com.freeme.launcher.settings/favorites"),
                    new String[]{"appWidgetProvider"}, null, null, null);
            if (c != null) {
                while(c.moveToNext()) {
                    String data = c.getString(c.getColumnIndex("appWidgetProvider"));
                    if (data != null) {
                        mWidgetNames.add(ComponentName.unflattenFromString(data).getPackageName());
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "getWidgetPackageNames error:" + ex.toString());
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void reloadData() {
        mNeedReloadWidgetData = true;
        mNeedReloadLauncherFlag = true;
        loadAltasWhiteList();
    }

    private void loadAltasWhiteList() {
        mAltasWhiteList.clear();
        List<String> whitelist = getWhitelistFromConfigCenter();
        if (whitelist == null || whitelist.size() == 0) {
            whitelist = getWhitelistFromResource();
        }

        if (whitelist != null && whitelist.size() > 0) {
            mAltasWhiteList.addAll(whitelist);
        }
    }

    private List<String> getWhitelistFromConfigCenter() {
        Cursor c = null;
        try {
            c = mContentResolver.query(
                    Uri.parse("content://com.freeme.config/autoStartWhitelistAtlas"),
                    new String[] { "c_pkg_name" }, null, null, null);
            if (c != null) {
                List<String> whitelist = new ArrayList<>();
                while (c.moveToNext()) {
                    int columnIndex = c.getColumnIndex("c_pkg_name");
                    if (!c.isNull(columnIndex)) {
                        String pkgName = c.getString(columnIndex);
                        whitelist.add(pkgName);
                    }
                }
                return whitelist;
            }
        } catch (Exception e) {
            Log.w(TAG, "Access ConfigCenter failed.", e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return Collections.emptyList();
    }

    private List<String> getWhitelistFromResource() {
        final int resid = mRes.getIdentifier("config_atlas_preset_whitelistApps",
                "array", "android");
        return resid == 0 ? null : Arrays.asList(mRes.getStringArray(resid));
    }

    public static boolean hasAltas() {
        return FreemeFeature.isSystemSupported("sys.context.atlas");
    }

    public static boolean isSystemPackage(String packageName) {
        return !TextUtils.isEmpty(packageName) && (
                    packageName.equals("android") ||
                    packageName.equals("system") ||
                    packageName.startsWith("com.android") ||
                    packageName.startsWith("android") ||
                    packageName.startsWith("com.qualcomm") ||
                    packageName.startsWith("com.qti") ||
                    packageName.startsWith("com.quicinc") ||
                    packageName.startsWith("com.mediatek") ||
                    packageName.startsWith("com.mtk") ||
                    packageName.startsWith("plugin.sprd") ||
                    packageName.startsWith("com.sprd") ||
                    packageName.startsWith("com.spreadtrum") ||
                    packageName.startsWith("com.google") ||
                    packageName.startsWith("com.freeme")
        );
    }
}
