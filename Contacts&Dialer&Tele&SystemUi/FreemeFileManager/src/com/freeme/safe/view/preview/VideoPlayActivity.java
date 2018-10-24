package com.freeme.safe.view.preview;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.freeme.filemanager.R;
import com.freeme.safe.utils.SafeConstants;

public class VideoPlayActivity extends BasePreviewActivity implements View.OnClickListener {

    private boolean mShowing;
    private RelativeLayout mActionBarView;
    private TextView mVideoName;
    private Intent mIntent;

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);

        mIntent = getIntent();
        if (mIntent == null) {
            return;
        }

        PreviewUtils.setFullScreen(this);
        mActionBarView = findViewById(R.id.action_bar_view);
        mActionBarView.setPadding(0, PreviewUtils.getStatusBarHeight(this), 0, 0);
        ImageView backBtn = findViewById(R.id.ic_back);
        backBtn.setOnClickListener(this);
        mVideoName = findViewById(R.id.video_name);

        loadView(mIntent.getStringExtra(SafeConstants.SAFE_FILE_PATH));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ic_back:
                super.onBackPressed();
                break;
            case R.id.video_view: {
                if (mShowing) {
                    hide();
                } else {
                    show(PreviewUtils.TIME_OUT);
                }
                break;
            }
        }
    }

    private void loadView(String path) {
        if (mIntent.hasExtra(Intent.EXTRA_TITLE)) {
            mVideoName.setText(mIntent.getStringExtra(Intent.EXTRA_TITLE));
        }
        if (path != null) {
            VideoView videoView = findViewById(R.id.video_view);
            videoView.setOnClickListener(this);
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    finish();
                }
            });

            videoView.setVideoPath(path);
            MediaController mediaController = new MediaController(this);
            videoView.setMediaController(mediaController);
            videoView.requestFocus();
            videoView.start();
        }
        hide();
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
