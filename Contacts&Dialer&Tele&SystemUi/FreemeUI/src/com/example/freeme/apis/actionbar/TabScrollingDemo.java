package com.example.freeme.apis.actionbar;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import com.example.freeme.apis.R;

public class TabScrollingDemo
        extends FragmentActivity
        implements ActionBar.TabListener
{
    private ActionBar mActionBar;
    private Fragment mFragment1 = new Fragment();
    private Fragment mFragment2 = new Fragment();
    private Fragment mFragment3 = new Fragment();
    private ViewPager mViewPager;
    private ViewPagerAdapter mViewPagerAdapter;

    private void setUpActionBar()
    {
        this.mActionBar = getActionBar();
        this.mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        this.mActionBar.setDisplayShowTitleEnabled(false);
        this.mActionBar.setDisplayShowHomeEnabled(false);
    }

    private void setUpTabs()
    {
        int i = 0;
        while (i < this.mViewPagerAdapter.getCount())
        {
            this.mActionBar.addTab(this.mActionBar.newTab().setText(this.mViewPagerAdapter.getPageTitle(i)).setTabListener(this));
            i += 1;
        }
    }

    private void setUpViewPager()
    {
        this.mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        this.mViewPager = ((ViewPager)findViewById(R.id.pager));
        this.mViewPager.setAdapter(this.mViewPagerAdapter);
    }

    public void onCreate(Bundle paramBundle)
    {
        super.onCreate(paramBundle);
        setContentView(R.layout.scrollingtab);
        setUpActionBar();
        setUpViewPager();
        setUpTabs();
        this.mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
        {
            public void onPageScrollStateChanged(int paramAnonymousInt)
            {
                switch (paramAnonymousInt)
                {
                }
            }

            public void onPageSelected(int paramAnonymousInt)
            {
                TabScrollingDemo.this.mActionBar.setSelectedNavigationItem(paramAnonymousInt);
            }
        });
    }

    protected void onDestroy()
    {
        super.onDestroy();
    }

    public void onTabReselected(ActionBar.Tab paramTab, FragmentTransaction paramFragmentTransaction) {}

    public void onTabSelected(ActionBar.Tab paramTab, FragmentTransaction paramFragmentTransaction) {}

    public void onTabUnselected(ActionBar.Tab paramTab, FragmentTransaction paramFragmentTransaction) {}

    public class ViewPagerAdapter
            extends FragmentPagerAdapter
    {
        public ViewPagerAdapter(FragmentManager paramFragmentManager)
        {
            super(paramFragmentManager);
        }

        public int getCount()
        {
            return 3;
        }

        public Fragment getItem(int paramInt)
        {
            switch (paramInt)
            {
                default:
                    throw new IllegalStateException("No fragment at position " + paramInt);
                    //return TabScrollingDemo.this.mFragment3;
                case 0:
                    return TabScrollingDemo.this.mFragment1;
                case 1:
                    return TabScrollingDemo.this.mFragment2;
                case 2:
                    return TabScrollingDemo.this.mFragment3;

            }

        }

        public CharSequence getPageTitle(int paramInt)
        {
            switch (paramInt)
            {
                default:
                    return "3";
                case 0:
                    return "1";
                case 1:
                    return "2";
                case 2:
                    return "3";
            }
           // return "3";
        }
    }
}
