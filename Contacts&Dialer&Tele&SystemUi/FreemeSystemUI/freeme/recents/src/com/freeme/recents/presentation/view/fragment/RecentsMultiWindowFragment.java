package com.freeme.recents.presentation.view.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.GridView;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.freeme.recents.RecentsUtils;
import com.freeme.recents.presentation.view.adapter.GridViewAdapter;

import java.util.ArrayList;
import java.util.List;

public class RecentsMultiWindowFragment extends BaseFragment {
    private PackageManager mPackageManager;
    private List<ResolveInfo> mListAllApps;
    private List<FreemePackageInfo> mListFreemePackageInfo;
    private View mRecentPanel;
    private SystemUIApplication msystemuiApplication;
    @Override
    public void onAttach(Context context) {
        super.onAttach (context);
        msystemuiApplication = (SystemUIApplication) ((Activity) context).getApplication();
    }

    private final ViewTreeObserver.OnPreDrawListener mRecentsDrawnEventListener =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRecentPanel.getViewTreeObserver().removeOnPreDrawListener(this);
                    msystemuiApplication.getSystemServices().proxyAction(RecentsUtils.FREEME_RECENTS_DRAWN);
                    return true;
                }
            };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRecentPanel = inflater.inflate (R.layout.recents_multiwindow_panel,container,false);
        mRecentPanel.getViewTreeObserver().addOnPreDrawListener(mRecentsDrawnEventListener);
        Intent mIntent = new Intent(Intent.ACTION_MAIN, null);
        mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mPackageManager = getContext ().getPackageManager ();
        mListAllApps = mPackageManager.queryIntentActivities(mIntent, 0);
        mListFreemePackageInfo = new ArrayList<FreemePackageInfo> ();
        for (int i = 0; i < mListAllApps.size (); i++){
            if (ActivityInfo.isResizeableMode(mListAllApps.get(i).activityInfo.resizeMode)){
                FreemePackageInfo freemepackageinfo = new FreemePackageInfo();
                freemepackageinfo.label = (String) mListAllApps.get(i).loadLabel (mPackageManager);
                freemepackageinfo.icon = mListAllApps.get(i).loadIcon (mPackageManager);
                freemepackageinfo.componentName = mListAllApps.get(i).getComponentInfo().getComponentName();
                mListFreemePackageInfo.add (freemepackageinfo);
            }
        }
        GridView mGridView = mRecentPanel.findViewById (R.id.gridview);
        GridViewAdapter mGridViewAdapter = new GridViewAdapter(mListFreemePackageInfo,getContext ());
        mGridView.setAdapter (mGridViewAdapter);
        return  mRecentPanel;
    }

    @Override
    public void onDestroyView() {
        mRecentPanel.getViewTreeObserver().removeOnPreDrawListener(mRecentsDrawnEventListener);
        super.onDestroyView ();
    }

    public class FreemePackageInfo {
        public ComponentName componentName;
        public String label;
        public Drawable icon;
    }
}

