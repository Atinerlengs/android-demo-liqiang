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
package com.freeme.filemanager.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.WindowManager;

import com.freeme.filemanager.R;
import com.freeme.filemanager.FMIntent;
import com.freeme.filemanager.controller.IBackHandledInterface;
import com.freeme.filemanager.fragment.BaseFragment;
import com.freeme.filemanager.fragment.FastCategoryContainerFragment;
import com.freeme.filemanager.fragment.FileExplorerViewFragment;
import com.freeme.filemanager.util.PermissionUtil;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.AsyncGarbageCleanupHelper;
import com.freeme.filemanager.util.AsyncGarbageCleanupHelper.GarbageItem;
import com.freeme.filemanager.app.FTPServerService;
import com.freeme.filemanager.controller.IActionModeCtr;
import com.freeme.filemanager.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.widget.TextView;

import static com.freeme.safe.utils.SafeConstants.FROM_SAFE;

public class FileExplorerTabActivity extends BaseActivity implements IActionModeCtr, IBackHandledInterface {
    private static final String TAG = "FileExplorerTabActivity";

    private boolean isSearch;

    private ActionMode mActionMode;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor editor;

    private NotificationManager mNotificationManager;

    private BaseFragment mBackHandedFragment;
    private boolean mHasSaveInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceState.remove("android:fragments");
        }

        PermissionUtil.checkSecurityPermissions(this);
        StorageHelper.getInstance(this).setCurrentMountPoint(Environment.getExternalStorageDirectory().getPath());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_pager);

        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra(FROM_SAFE, false)) {
                Fragment fragment;
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                fragment = new FileExplorerViewFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable(FROM_SAFE, true);
                fragment.setArguments(bundle);
                ft.replace(R.id.first_page, fragment);
                ft.commitAllowingStateLoss();
        } else {
            setFastCategoryContainerFragment();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        initOthers();
    }
    private void initOthers() {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = mSharedPref.edit();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (getIntent() != null && FMIntent.ACTION_CLEAR.equals(getIntent().getAction())) {
            mNotificationManager.cancelAll();
        }
    }

    private void setFastCategoryContainerFragment() {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        FastCategoryContainerFragment mContainerFragment = new FastCategoryContainerFragment();
        transaction.replace(R.id.first_page, mContainerFragment);
        transaction.commit();
    }

    @Override
    protected void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        setIntent(paramIntent);
        Uri pathUri = paramIntent.getData();
        if ((pathUri != null) && (!TextUtils.isEmpty(pathUri.getPath()))) {
            mBackHandedFragment.setPath(pathUri.getPath());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Util.mFileExplorerTabActivityTilte = this.getActionBar().getTitle().toString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationManager.cancelAll();

        if (FTPServerService.isRunning()) {
            stopService(new Intent(this, FTPServerService.class));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment) {
        this.mBackHandedFragment = selectedFragment;
    }

    @Override
    public void onBackPressed() {
        IBackPressedListener backPressedListener = (IBackPressedListener) mBackHandedFragment;
        Log.v(TAG, "onBackPressed mHasSaveInstanceState = "+ mHasSaveInstanceState);
        if (mHasSaveInstanceState) {
            return;
        }
        if (!backPressedListener.onBack()) {
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setDisplayHomeAsUpEnabled(false);
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PermissionUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mHasSaveInstanceState = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHasSaveInstanceState = false;

        invalidateOptionsMenu();
        isSearch = getIntent().getBooleanExtra("isSearch", false);
        if (getIntent() != null && isSearch) {
            isSearch = false;
            getIntent().putExtra("isSearch", false);
        }
        //scanGarbageItems();
    }

    public interface IBackPressedListener {
        boolean onBack();
    }

    public void setActionMode(ActionMode actionMode) {
        mActionMode = actionMode;
    }

    public ActionMode getActionMode() {
        return mActionMode;
    }

}
