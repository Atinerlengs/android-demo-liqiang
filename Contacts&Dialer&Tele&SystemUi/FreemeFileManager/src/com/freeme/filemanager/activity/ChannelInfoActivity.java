package com.freeme.filemanager.activity;

import java.io.IOException;
import java.io.InputStream;

import com.freeme.filemanager.activity.BaseActivity;
import com.freeme.filemanager.R;

import android.app.ActionBar;
import android.os.Bundle;
import android.widget.TextView;

public class ChannelInfoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_info);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);

        TextView channelInfo = (TextView) findViewById(R.id.channel);
        TextView customInfo = (TextView) findViewById(R.id.custom);

        channelInfo.setText(readAssetsFileString("cp"));
        customInfo.setText(readAssetsFileString("td"));
    }

    public String readAssetsFileString(String Filename) {
        String str = null;
        try {
            InputStream is = getAssets().open(Filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            str = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return str;
    }
}
