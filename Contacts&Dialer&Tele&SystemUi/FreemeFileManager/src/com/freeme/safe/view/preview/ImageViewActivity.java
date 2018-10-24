package com.freeme.safe.view.preview;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.freeme.filemanager.R;
import com.freeme.safe.utils.SafeConstants;

public class ImageViewActivity extends BasePreviewActivity implements View.OnClickListener {

    private boolean mShowing;
    private RelativeLayout mActionBarView;

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        Intent intent = getIntent();
        String filePath;
        if (intent != null) {
            filePath = intent.getStringExtra(SafeConstants.SAFE_FILE_PATH);
        } else {
            return;
        }

        PreviewUtils.setFullScreen(this);
        mActionBarView = findViewById(R.id.action_bar_view);
        mActionBarView.setPadding(0, PreviewUtils.getStatusBarHeight(this), 0, 0);
        ImageView backBtn = findViewById(R.id.ic_back);
        backBtn.setOnClickListener(this);
        ImageView imageView = findViewById(R.id.image_view);
        imageView.setOnClickListener(this);

        File file = new File(filePath);
        Uri imageUri = Uri.fromFile(file);
        if (file.exists() && file.length() > 0) {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        hide();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ic_back:
                super.onBackPressed();
                break;
            case R.id.image_view: {
                if (mShowing) {
                    hide();
                } else {
                    show(PreviewUtils.TIME_OUT);
                }
                break;
            }
        }
    }

    private void show(int timeout) {
        mShowing = true;
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mActionBarView.setVisibility(View.VISIBLE);

        if (timeout != 0) {
            mActionBarView.removeCallbacks(mFadeOut);
            mActionBarView.postDelayed(mFadeOut, timeout);
        }
    }

    private void hide() {
        mActionBarView.setVisibility(View.GONE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mShowing = false;
    }
}
