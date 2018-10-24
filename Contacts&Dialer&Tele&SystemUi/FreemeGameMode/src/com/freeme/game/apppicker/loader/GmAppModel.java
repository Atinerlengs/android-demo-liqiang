package com.freeme.game.apppicker.loader;

import android.graphics.drawable.Drawable;

public class GmAppModel {

    private String mTitle;
    private String mPkgName;
    private String mAppName;
    private boolean mIsSelected;
    private Drawable mAppIcon;

    GmAppModel(String pkgName, String appName, boolean isSelected) {
        mPkgName = pkgName;
        mAppName = appName;
        mIsSelected = isSelected;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getPkgName() {
        return mPkgName;
    }

    public void setPkgName(String pkgName) {
        mPkgName = pkgName;
    }

    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String appName) {
        mAppName = appName;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        mIsSelected = isSelected;
    }

    public Drawable getAppIcon() {
        return mAppIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        mAppIcon = appIcon;
    }
}
