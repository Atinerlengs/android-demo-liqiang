package com.freeme.recents.presentation.view.activity;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.freeme.recents.RecentsUtils;
import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.presentation.event.activity.DismissRecentsToHomeAnimationStarted;
import com.freeme.recents.presentation.event.activity.FreemeHideRecentsEvent;
import com.freeme.recents.presentation.event.activity.LaunchTaskEvent;
import com.freeme.recents.presentation.event.activity.ToggleRecentsEvent;
import com.freeme.recents.presentation.view.fragment.RecentsOverlappingFragment;
import com.freeme.recents.presentation.view.fragment.RecentsMultiWindowFragment;
import com.freeme.recents.SystemServicesProxy;

public class RecentsActivity extends BaseActivity {

    private static final String TAG = "RecentsActivity";
    private Fragment mRecentsFragment;
    private FinishRecentsRunnable mFinishLaunchHomeRunnable;
    private Handler mHandler = new Handler();
    private boolean mIsInMultiWindowMode;
    private boolean mInstanceStateSaved;
    private boolean mVisible;
    private SystemUIApplication msystemuiApplication;

    class FinishRecentsRunnable implements Runnable {

        Intent mLaunchIntent;
        ActivityOptions mLaunchOpts;

        public FinishRecentsRunnable(Intent launchIntent, ActivityOptions opts) {
            mLaunchOpts = opts;
            mLaunchIntent = launchIntent;
        }

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mLaunchOpts == null) {
                        mLaunchOpts = ActivityOptions.makeCustomAnimation(RecentsActivity.this,
                                R.anim.recents_to_launcher_enter, R.anim.recents_to_launcher_exit);
                    }
                    try {
                        startActivityAsUser(mLaunchIntent, mLaunchOpts.toBundle(), UserHandle.CURRENT);
                    } catch (Exception e) {
                        Log.e(TAG, "fail to launch home", e);
                    }
                }
            });
        }
    }

    final BroadcastReceiver mSystemBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                // When the screen turns off, dismiss Recents to Home
                SystemServicesProxy ssp = SystemServicesProxy.getInstance(context);
                if (ssp.isRecentsActivityVisible()) {
                    mFinishLaunchHomeRunnable.run();
                }
            }
        }
    };

    private class StartScreenPinningRunnableRunnable implements Runnable {

        private int taskId = -1;

        @Override
        public void run() {
            msystemuiApplication.getSystemServices().proxyAction(RecentsUtils.FREEME_SHOW_SCREEN_PIN_REQUEST, taskId);
        }
    }

    private StartScreenPinningRunnableRunnable mStartScreenPinningRunnable
            = new StartScreenPinningRunnableRunnable();

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mInstanceStateSaved = true;
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        boolean visible = msystemuiApplication.getSystemServices().isRecentsActivityVisible();
        if (!isInMultiWindowMode && visible) {
            replaceRecentsFragment();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(ev.getPointerCount() > 1) {
            Log.d(TAG, "Multitouch detected!");
            ev.setAction(MotionEvent.ACTION_CANCEL);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.freeme_recents);
        msystemuiApplication = (SystemUIApplication) getApplication();
        mIsInMultiWindowMode = isInMultiWindowMode();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mFinishLaunchHomeRunnable = new FinishRecentsRunnable(intent,
                ActivityOptions.makeCustomAnimation(this, R.anim.recents_to_launcher_enter,
                        R.anim.recents_to_launcher_exit));

        // Register the broadcast receiver to handle messages when the screen is turned off
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mSystemBroadcastReceiver, filter);
        if (savedInstanceState == null) {
            replaceRecentsFragment();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged (newConfig);
        if (isInMultiWindowMode() != mIsInMultiWindowMode) {
            replaceRecentsFragment();
            mIsInMultiWindowMode = isInMultiWindowMode();
        } else if( isInMultiWindowMode()) {
            replaceRecentsFragment();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mSystemBroadcastReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVisible = true;
        //*/ freeme.gouzhouping, 20180629. bugfix#40506 app doesn't show when it has applock.
        if (isInMultiWindowMode()) {
            replaceRecentsFragment();
        }
        //*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!isInMultiWindowMode()) replaceRecentsFragment();
    }

    @Override
    public void onBackPressed() {
        if (!mVisible) return;

        FreemeEventBus.getDefault().send(new ToggleRecentsEvent());
        mVisible = false;
    }

    private void replaceRecentsFragment() {
        FragmentTransaction fragmentTransaction = this.getFragmentManager().beginTransaction();
        mRecentsFragment = createRecentsFragment();
        fragmentTransaction.replace(R.id.fragmentContainer, mRecentsFragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private Fragment createRecentsFragment() {
        /*/ freeme.gouzhouping, 20180224. FreemeAppTheme, recents view.
        if (msystemuiApplication.getSystemServices().isNapp()) {
            if (isInMultiWindowMode()) {
                return new RecentsMultiWindowFragment();
            }
        }
        //*/
        return new RecentsOverlappingFragment();
    }

    /**** EventBus events ****/
    public final void onBusEvent(final LaunchTaskEvent event) {
        final View view = event.view;
        /*/freeme.lishoubo, 20180307. FreemeAppTheme, recents view.
        final ActivityOptions opts;

        if (view != null) {
            opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0,
                    view.getWidth(), view.getHeight());
        } else {
            opts = ActivityOptions.makeCustomAnimation(this,
                R.anim.recents_launch_next_task_target,
                R.anim.recents_launch_next_task_source);
        }
        /*/
        final ActivityOptions opts = null;
        //*/
        if (msystemuiApplication.getSystemServices().startActivityFromRecents(this, event.task,
                event.task.getLabel(), opts)) {
            if (event.screenPinningRequested) {
                mStartScreenPinningRunnable.taskId = event.task.taskId;
                mStartScreenPinningRunnable.run();
            }
        }
    }

    public final void onBusEvent(FreemeHideRecentsEvent event) {
        boolean anim = event.anim;
        boolean force = event.force;

        if (!mVisible && !force) return;

        mVisible = false;
        if (anim) {
            DismissRecentsToHomeAnimationStarted dismissEvent =
                    new DismissRecentsToHomeAnimationStarted();
            dismissEvent.addPostAnimationCallback(mFinishLaunchHomeRunnable);
            FreemeEventBus.getDefault().send(dismissEvent);
        } else {
            mFinishLaunchHomeRunnable.run();
        }
    }
}
