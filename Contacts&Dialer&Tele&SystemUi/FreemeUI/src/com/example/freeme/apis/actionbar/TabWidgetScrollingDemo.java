package com.example.freeme.apis.actionbar;

import java.util.Random;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TabWidget;
import android.widget.TextView;

import com.example.freeme.apis.R;


public class TabWidgetScrollingDemo extends FragmentActivity
{
    private static final String TAG = "AndroidDemos.SlideTabs3";

    private ViewPager mViewPager;

    private PagerAdapter mPagerAdapter;

    private TabWidget mTabWidget;

    private String[] addresses = { "first", "second", "third" ,"four"};

    private Button[] mBtnTabs = new Button[addresses.length];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_slidetabs3);

        mTabWidget = (TabWidget) findViewById(R.id.tabWidget1);
       // mTabWidget.setStripEnabled(true);
        mBtnTabs[0] = new Button(this);
        mBtnTabs[0].setFocusable(true);
        mBtnTabs[0].setText(addresses[0]);
        mBtnTabs[0].setBackground(null);
        //mBtnTabs[0].setTextColor(0xFF000000);
        mTabWidget.addView(mBtnTabs[0]);
        /*
         * Listener必须在mTabWidget.addView()之后再加入，用于覆盖默认的Listener，
         * mTabWidget.addView()中默认的Listener没有NullPointer检测。
         */
        mBtnTabs[0].setOnClickListener(mTabClickListener);

        mBtnTabs[1] = new Button(this);
        mBtnTabs[1].setFocusable(true);
        mBtnTabs[1].setText(addresses[1]);
        mBtnTabs[1].setTextColor(0xFF000000);
        mBtnTabs[1].setBackground(null);
        mTabWidget.addView(mBtnTabs[1]);
        mBtnTabs[1].setOnClickListener(mTabClickListener);

        mBtnTabs[2] = new Button(this);
        mBtnTabs[2].setFocusable(true);
        mBtnTabs[2].setText(addresses[2]);
        //mBtnTabs[2].setTextColor(0xFF000000);
        mBtnTabs[2].setBackground(null);
        mTabWidget.addView(mBtnTabs[2]);
        mBtnTabs[2].setOnClickListener(mTabClickListener);

        mBtnTabs[3] = new Button(this);
        mBtnTabs[3].setFocusable(true);
        mBtnTabs[3].setText(addresses[3]);
        //mBtnTabs[2].setTextColor(0xFF000000);
        mBtnTabs[3].setBackground(null);
        mTabWidget.addView(mBtnTabs[3]);
        mBtnTabs[3].setOnClickListener(mTabClickListener);


        mViewPager = (ViewPager) findViewById(R.id.viewPager1);
        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);

        mTabWidget.setCurrentTab(0);
//        mTabWidget.setShowDividers(2);
        //mTabWidget.setDividerDrawable(getResources().getDrawable((R.drawable.divider_vertical_dark_opaque)));
    }

    private OnClickListener mTabClickListener = new OnClickListener() {

        @Override
        public void onClick(View v)
        {
            if (v == mBtnTabs[0])
            {
                mViewPager.setCurrentItem(0);
            } else if (v == mBtnTabs[1])
            {
                mViewPager.setCurrentItem(1);

            } else if (v == mBtnTabs[2])
            {
                mViewPager.setCurrentItem(2);

            }else if (v == mBtnTabs[3])
            {
                mViewPager.setCurrentItem(3);

            }
        }
    };

    private OnPageChangeListener mPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int arg0)
        {
            mTabWidget.setCurrentTab(arg0);
            /*switch (arg0)
            {
                default:
                    //mBtnTabs[2].setBackground(null);;
                case 0:
                    mBtnTabs[0].setBackgroundColor(0xff29bd68);
                    mBtnTabs[1].setBackgroundColor(0xffffffff);
                    mBtnTabs[2].setBackgroundColor(0xffffffff);
                case 1:
                    mBtnTabs[0].setBackgroundColor(0xffffffff);
                    mBtnTabs[1].setBackgroundColor(0xff29bd68);
                    mBtnTabs[2].setBackgroundColor(0xffffffff);
                case 2:
                    mBtnTabs[2].setBackgroundColor(0xffffffff);
                    mBtnTabs[0].setBackgroundColor(0xffffffff);
                    mBtnTabs[1].setBackgroundColor(0xff29bd68);
            }*/



        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2)
        {

        }

        @Override
        public void onPageScrollStateChanged(int arg0)
        {

        }
    };

    private class MyPagerAdapter extends FragmentStatePagerAdapter
    {

        public MyPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            return MyFragment.create(addresses[position]);
        }

        @Override
        public int getCount()
        {
            return addresses.length;
        }

    }

    public static class MyFragment extends Fragment
    {
        public static MyFragment create(String address)
        {
            MyFragment f = new MyFragment();
            Bundle b = new Bundle();
            b.putString("address", address);
            f.setArguments(b);
            return f;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            Random r = new Random(System.currentTimeMillis());

            Bundle b = getArguments();

            View v = inflater.inflate(R.layout.fragment_viewpager1_layout1, null);
            v.setBackgroundColor(r.nextInt() >> 8 | 0xFF << 24);

            TextView txvAddress = (TextView) v.findViewById(R.id.textView1);
            txvAddress.setTextColor(r.nextInt() >> 8 | 0xFF << 24);
            txvAddress.setBackgroundColor(r.nextInt() >> 8 | 0xFF << 24);

            txvAddress.setText(b.getString("address", ""));
            return v;
        }

    }
}