package com.example.freeme.apis.actionbar;

import android.app.ActionBar;
import android.app.TabActivity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.TabWidget;

import com.example.freeme.apis.R;

public class TabScrollingDemo1
        extends TabActivity {
    private TabHost tabhost;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scrollingtab1);

        //从TabActivity上面获取放置Tab的TabHost
        tabhost = getTabHost();
        tabhost.addTab(tabhost
                //创建新标签one
                .newTabSpec("one")
                //设置标签标题
                .setIndicator("红色")
                //设置该标签的布局内容
                .setContent(R.id.widget_layout_red));
        tabhost.addTab(tabhost.newTabSpec("two").setIndicator("黄色").setContent(R.id.widget_layout_yellow));
    }


}