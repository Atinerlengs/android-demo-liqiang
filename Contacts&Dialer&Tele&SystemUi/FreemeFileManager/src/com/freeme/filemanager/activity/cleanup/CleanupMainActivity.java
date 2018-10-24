package com.freeme.filemanager.activity.cleanup;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.controller.CustomViewPager;
import com.freeme.filemanager.controller.IBackHandledInterface;
import com.freeme.filemanager.controller.TabsAdapter;
import com.freeme.filemanager.fragment.BaseFragment;
import com.freeme.filemanager.fragment.cleanup.DeepCleanFragment;
import com.freeme.filemanager.fragment.cleanup.SuggestCleanFragment;
import com.freeme.filemanager.util.Util;
import com.freeme.support.design.widget.FreemeTabLayout;

import java.util.ArrayList;

public class CleanupMainActivity extends BaseActivity implements IBackHandledInterface {
    public CustomViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cleanup_main_pager);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        initViewPager();
    }

    private void initViewPager() {
        mViewPager = (CustomViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mViewPager, new ArrayList<String>() {{
            add(getString(R.string.garbage_child_summary_default));
            add(getString(R.string.deep_clean));
        }});

        mTabsAdapter.addTab(null, SuggestCleanFragment.class, null);
        mTabsAdapter.addTab(null, DeepCleanFragment.class, null);
        mViewPager.setAdapter(mTabsAdapter);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            FreemeTabLayout tabs = (FreemeTabLayout) View.inflate(this, R.layout.layout_scrolling_tab_component, null);
            tabs.setupWithViewPager(mViewPager);
            ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.MATCH_PARENT,
                    ActionBar.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER_HORIZONTAL);
            lp.setMarginStart(getResources().getDimensionPixelSize(R.dimen.edit_adapter_img_width));
            actionBar.setCustomView(tabs, lp);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            Util.setBackTitle(actionBar, null);
        }
    }
    @Override
    protected void onResume() {
        if (this.getIntent() != null && Intent.ACTION_MAIN.equals(this.getIntent().getAction())) {
            NotificationManager nm = (NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE);
            nm.cancelAll();
            mViewPager.setCurrentItem(0);
        }
        super.onResume();
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment) {

    }
}
