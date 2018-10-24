package com.freeme.safe.view.preview;

import java.io.File;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.freeme.filemanager.R;
import com.freeme.safe.utils.SafeConstants;

public class AudioPlayActivity extends BasePreviewActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {

    private static final long UPDATE_PROGRESS_INTERVAL = 1000;

    private Handler mHandler = new Handler();
    private int mDuration;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mNameText;
    private ImageView mPlayView;
    private SeekBar mSeekBar;
    private Intent mIntent;
    private MediaPlayer mMediaPlayer;

    private Runnable mProgressCallback = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer.isPlaying()) {
                float percent = (float)mMediaPlayer.getCurrentPosition() / (float)mMediaPlayer.getDuration();
                int progress = (int) (mSeekBar.getMax() * percent);
                updateProgressTextWithDuration(mMediaPlayer.getCurrentPosition());
                if (progress >= 0 && progress <= mSeekBar.getMax()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mSeekBar.setProgress(progress, true);
                    } else {
                        mSeekBar.setProgress(progress);
                    }
                    mHandler.postDelayed(this, UPDATE_PROGRESS_INTERVAL);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_audio);

        mIntent = getIntent();
        if (mIntent == null) {
            return;
        }

        mNameText = findViewById(R.id.audio_name);
        mCurrentTime = findViewById(R.id.current_time);
        mTotalTime = findViewById(R.id.total_time);

        mSeekBar = findViewById(R.id.progress);
        mSeekBar.setOnSeekBarChangeListener(this);
        mPlayView = findViewById(R.id.btn_play);
        mPlayView.setOnClickListener(this);
        LinearLayout btnStop = findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(this);

        initMediaPlayer();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mHandler.removeCallbacks(mProgressCallback);
            mHandler.post(mProgressCallback);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_play: {
                if (mMediaPlayer == null) return;
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    mPlayView.setImageResource(R.drawable.ic_play);
                    mHandler.removeCallbacks(mProgressCallback);
                } else {
                    mMediaPlayer.start();
                    mPlayView.setImageResource(R.drawable.ic_pause);
                    mHandler.removeCallbacks(mProgressCallback);
                    mHandler.post(mProgressCallback);
                }
                break;
            }
            case R.id.btn_stop: {
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                    }
                    mMediaPlayer.reset();
                }
                finish();
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            updateProgressTextWithProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHandler.removeCallbacks(mProgressCallback);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mMediaPlayer.seekTo(getCurrentDuration(seekBar.getProgress()));
        if (mMediaPlayer.isPlaying()) {
            mHandler.removeCallbacks(mProgressCallback);
            mHandler.post(mProgressCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.removeCallbacks(mProgressCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }
    }

    private void initMediaPlayer() {
        if (mIntent.hasExtra(Intent.EXTRA_TITLE)) {
            mNameText.setText(mIntent.getStringExtra(Intent.EXTRA_TITLE));
        } else {
            mNameText.setVisibility(View.INVISIBLE);
        }

        String filePath = mIntent.getStringExtra(SafeConstants.SAFE_FILE_PATH);
        if (null == filePath) {
            finish();
            return;
        }

        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    finish();
                }
            });

            File file = new File(filePath);
            mMediaPlayer.setDataSource(file.getPath());
            mMediaPlayer.setLooping(false);
            mMediaPlayer.prepare();
            mDuration = mMediaPlayer.getDuration();
            mSeekBar.setProgress(0);
            updateProgressTextWithProgress(0);
            mMediaPlayer.seekTo(0);
            mTotalTime.setText(PreviewUtils.makeTimeString(this, mDuration));
            mMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateProgressTextWithProgress(int progress) {
        int targetDuration = getCurrentDuration(progress);
        mCurrentTime.setText(PreviewUtils.makeTimeString(this, targetDuration));
    }

    private void updateProgressTextWithDuration(int duration) {
        mCurrentTime.setText(PreviewUtils.makeTimeString(this, duration));
    }

    private int getCurrentDuration(int progress) {
        return (int) (mDuration * ((float) progress / mSeekBar.getMax()));
    }
}
