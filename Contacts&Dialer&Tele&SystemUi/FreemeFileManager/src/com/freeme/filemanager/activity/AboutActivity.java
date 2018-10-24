package com.freeme.filemanager.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.ToastHelper;
import com.freeme.filemanager.util.Util;

public class AboutActivity extends BaseActivity implements OnClickListener {

    private static int sMaxClicked = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        String backTitle = Util.mFileExplorerTabActivityTilte;
        if (backTitle == null) {
            Util.setBackTitle(actionBar, getString(R.string.app_name));
        } else {
            Util.setBackTitle(actionBar, backTitle);
        }
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

        initView();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.app_icon) {
            performClick();
        }
    }

    private void initView() {
        ImageView app_icon = (ImageView) findViewById(R.id.app_icon);
        TextView app_name = (TextView) findViewById(R.id.app_name);
        TextView app_version = (TextView) findViewById(R.id.version_name);

        try {
            PackageInfo appInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            app_name.setText(appInfo.applicationInfo.loadLabel(getPackageManager()).toString());
            app_version.setText("V" + appInfo.versionName);
            app_icon.setBackgroundResource(appInfo.applicationInfo.icon);
            app_icon.setOnClickListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performClick() {
        if (sMaxClicked > 0) {
            sMaxClicked--;
            if (sMaxClicked <= 6) {
                ToastHelper.show(this,
                        getString(R.string.about_click_times)
                                + sMaxClicked + getString(R.string.about_click_intent),
                        Toast.LENGTH_SHORT);
            }
        } else if (sMaxClicked == 0) {
            Intent detailIntent = new Intent();
            detailIntent.setClassName(getApplicationContext(), ChannelInfoActivity.class.getName());
            startActivity(detailIntent);
            sMaxClicked = 15;
        }
    }
}
