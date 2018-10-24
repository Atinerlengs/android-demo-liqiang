package com.example.freeme.apis.tab;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;

import com.freeme.support.design.widget.FreemeTabLayout;

import com.example.freeme.apis.R;

public class ScrollingTabTest extends FragmentActivity {

    private static final String[] TAB_TITLES = { "主题", "混搭", "我的", "世界时钟" };

    private ViewPager mViewPager;
    private FreemeTabLayout mScrollingTab;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling_tab);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(TabFragmentHelper.newInstance(getSupportFragmentManager(), TAB_TITLES));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            FreemeTabLayout tabs = (FreemeTabLayout) View.inflate(this, R.layout.layout_scrolling_tab_component, null);
            tabs.setupWithViewPager(mViewPager);
            ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            actionBar.setCustomView(tabs, lp);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mScrollingTab = (FreemeTabLayout) findViewById(R.id.tabs);
        mScrollingTab.setupWithViewPager(mViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_show_never, menu);
        return true;
    }
}
