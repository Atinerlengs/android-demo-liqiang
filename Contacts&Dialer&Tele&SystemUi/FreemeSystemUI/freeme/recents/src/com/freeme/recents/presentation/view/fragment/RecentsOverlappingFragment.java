package com.freeme.recents.presentation.view.fragment;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.Recents;

import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.presentation.event.activity.DismissRecentsToHomeAnimationStarted;
import com.freeme.recents.presentation.event.activity.FreemeHideRecentsEvent;
import com.freeme.recents.presentation.event.activity.LaunchFrontTaskEvent;
import com.freeme.recents.presentation.event.activity.LaunchTaskEvent;
import com.freeme.recents.presentation.event.activity.ToggleRecentsEvent;
import com.freeme.recents.presentation.event.ui.DismissAllTasksEvent;
import com.freeme.recents.presentation.event.ui.LoadTaskDataEvent;
import com.freeme.recents.presentation.event.ui.TaskDismissedEvent;
import com.freeme.recents.presentation.view.component.overlappingstackview.ChildView;
import com.freeme.recents.presentation.view.component.overlappingstackview.OverlappingStackView;
import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.presentation.presenter.RecentsPresenter;
import com.freeme.recents.presentation.view.RecentsAnimationHelper;
import com.freeme.recents.presentation.view.adapter.RecentsItemOverlappingAdapter;
import com.freeme.recents.presentation.view.component.DonutProgress;
import com.freeme.recents.RecentsUtils;
import com.freeme.recents.SystemServicesProxy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RecentsOverlappingFragment extends BaseFragment implements RecentsPresenter.RecentsView, OverlappingStackView.Callback<Task> {

    private static final String TAG = "RecentsOverlappingFragment";
    private Handler mHandler = new RecentsOverlappingFragment.RecentsHandler();
    private RecentsPresenter mPresenter;
    private Activity mActivity;
    private TextView mEmptyHomeImage;
    private TextView mMemoryAvailText;
    private TextView mMemoryTotalText;
    private DonutProgress mMemoryProgressBar;
    private long mAvailBefore;
    private View mTrashCan;
    private View mEmptyHomeContainer;
    private View mRecentPanel;
    private View mMemoryLayout;
    private View mRecentsContainer;
    private View mMemoryContainer;
    private RecentsAnimationHelper mRecentsAnimationHelper;
    private RecentsItemOverlappingAdapter mAdapter;
    private OverlappingStackView<Task> mOverlappingView;
    private SystemUIApplication msystemuiApplication;
    private SystemServicesProxy mSystemServicesProxy;
    private int mPreviousTaskId;

    final class RecentsHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case RecentsUtils.RECENTS_UPDATE_INDICATOR:
                    update(type);
                    break;
                case RecentsUtils.RECENTS_UPDATE_MEMORY:
                    update(type);
                    break;
                case RecentsUtils.RECENTS_UPDATE_NORMAL:
                    update(type);
                    break;
                case RecentsUtils.RECENTS_UPDATE_DISMISS_ALL_TASKS:
                    FreemeEventBus.getDefault().send(new DismissAllTasksEvent());
                    break;
            }
        }
    }

    private final ViewTreeObserver.OnPreDrawListener mRecentsDrawnEventListener =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRecentPanel.getViewTreeObserver().removeOnPreDrawListener(this);
                    mSystemServicesProxy.proxyAction(RecentsUtils.FREEME_RECENTS_DRAWN);
                    return true;
                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            mActivity = (Activity) context;
            mPresenter = new RecentsPresenter(mActivity);
            mPresenter.setView(this);
            msystemuiApplication = (SystemUIApplication) ((Activity) context).getApplication();
        }
        //*/ freeme.gouzhouping, 20180511. Recents.
        EventBus.getDefault().register(this);
        //*/
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mPresenter.onTrimMemory(level);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        mRecentsAnimationHelper = new RecentsAnimationHelper(mActivity);
        mSystemServicesProxy = msystemuiApplication.getSystemServices();
        mRecentPanel = inflater.inflate(R.layout.recents_overlapping_panel, container, false);
        mRecentPanel.getViewTreeObserver().addOnPreDrawListener(mRecentsDrawnEventListener);
        mRecentPanel.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        mEmptyHomeContainer = mRecentPanel.findViewById(R.id.recents_empty_home_container);
        mEmptyHomeImage = (TextView) mRecentPanel.findViewById(R.id.recents_empty);
        mEmptyHomeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.onEmptyHomeClicked();
            }
        });

        mRecentsContainer = mRecentPanel.findViewById(R.id.recents_overlapping_container);
        mMemoryContainer = mRecentPanel.findViewById(R.id.recents_memory_container);
        setupOverlappingView();

        mTrashCan = mRecentPanel.findViewById(R.id.recents_memory_trash_can);
        mTrashCan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.onDismissAllButtonClicked();
            }
        });
        mMemoryLayout = mRecentPanel.findViewById(R.id.recents_memory_layout);
        // make memory layout auto adapt to navgation bar
        autoAdaptMemoryLayout();

        mMemoryAvailText = (TextView) mRecentPanel.findViewById(R.id.recents_memory_avail);
        mMemoryTotalText = (TextView) mRecentPanel.findViewById(R.id.recents_memory_total);
        mMemoryProgressBar = (DonutProgress) mRecentPanel
                .findViewById(R.id.recents_memory_progressbar);
        return mRecentPanel;
    }

    @Override public void onDestroyView() {
        mRecentPanel.getViewTreeObserver().removeOnPreDrawListener(mRecentsDrawnEventListener);
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        //*/ freeme.gouzhouping, 20180511. Recents.
        EventBus.getDefault().unregister(this);
        //*/
    }

    private void autoAdaptMemoryLayout() {
        Resources res = getResources();
        if (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) return;

        boolean hasSoftKey = res.getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            hasSoftKey = false;
        } else if ("0".equals(navBarOverride)) {
            hasSoftKey = true;
        }
        if (hasSoftKey) {
            if (mMemoryLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                int navHeight = res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mMemoryLayout.getLayoutParams();
                params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin + navHeight);
                mMemoryLayout.setLayoutParams(params);
            }
        }
    }

    private void setupOverlappingView() {
        Resources res = getResources();
        LayoutParams layoutparams = (LayoutParams) mRecentsContainer.getLayoutParams ();
        if (getActivity().isInMultiWindowMode() && res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutparams.topMargin = res.getDimensionPixelSize(R.dimen.recents_overlappingStackView_portrait_multwindow_marginTop);
        } else {
            if (getActivity().isInMultiWindowMode()) {
                layoutparams.topMargin = res.getDimensionPixelSize(R.dimen.recents_overlappingStackView_land_multwindow_marginTop);
            }
            /*/ freeme.gouzhouping, 20180403. Recents.
            else if (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                layoutparams.topMargin = res.getDimensionPixelSize(R.dimen.recents_overlappingStackView_portrait_marginTop);
            } else {
                layoutparams.topMargin = res.getDimensionPixelSize(R.dimen.recents_overlappingStackView_land_marginTop);
            }
            //*/

        }
        mRecentsContainer.setLayoutParams(layoutparams);
        boolean screenPinningEnabled = mSystemServicesProxy.getSystemSetting(getContext(),
                Settings.System.LOCK_TO_APP_ENABLED) != 0;
        mMemoryContainer.setVisibility(mActivity.isInMultiWindowMode() ? View.GONE : View.VISIBLE);
        mOverlappingView = (OverlappingStackView<Task>) mRecentPanel.findViewById(R.id.overlappingview);
        mAdapter = new RecentsItemOverlappingAdapter(mOverlappingView);
        mPresenter.preloadTasks();
        if (Recents.getConfiguration() != null
                && Recents.getConfiguration().getLaunchState().launchedFromApp
                && mSystemServicesProxy.getTasks().size() > 0) {
            mPreviousTaskId = mSystemServicesProxy.getTasks().get(0).persistentId;
        }
        mOverlappingView.initialize(this, screenPinningEnabled);
        mOverlappingView.showOverlapping(mActivity);
    }

    private void startApplicationDetailsActivity(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        mActivity.startActivityAsUser(intent, null, UserHandle.CURRENT);
    }

    public void update(int type) {
        if (mAdapter.getCount() != 0 || type == RecentsUtils.RECENTS_UPDATE_MEMORY) {
            if (mAdapter.getCount() != 0) {
                mRecentsContainer.setVisibility(View.VISIBLE);
                mEmptyHomeContainer.setVisibility(View.GONE);
            }
            switch (type) {
                case RecentsUtils.RECENTS_UPDATE_INDICATOR:
                    break;
                case RecentsUtils.RECENTS_UPDATE_MEMORY:
                    updateMemoryInfo();
                    break;
                case RecentsUtils.RECENTS_UPDATE_NORMAL:
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        } else {
            mRecentsContainer.setVisibility(View.GONE);
            mEmptyHomeContainer.setVisibility(View.VISIBLE);
        }
    }

    public void updateMemoryInfo() {
        if (isVisible()) {
            ActivityManager.MemoryInfo info = mSystemServicesProxy.getMemoryInfo();
            long avail = info.availMem;
            long total = info.totalMem;
            float progress = (float) (total-avail) / (float) total * 100;
            String availStr = Formatter.formatFileSize(mActivity, avail);
            String totalStr = Formatter.formatFileSize(mActivity, total);
            mMemoryAvailText.setText(getString(R.string.memory_avail, availStr));
            mMemoryTotalText.setText(totalStr);
            mRecentsAnimationHelper.startMemoryInfoProgressAnimation((int) progress, mMemoryProgressBar);
        }
    }

    /**** RecentsPresenter.RecentsView Implementation ****/
    @Override
    public void onTaskLoaded(Task task) {
        if (task.isHome()) {
//            mEmptyHomeImage.setImageDrawable(task.getThumbnail());
        } else {
            mAdapter.addTask(task);
        }
    }

    @Override
    public void render(int type) {
        mHandler.sendEmptyMessage(type);
    }

    @Override
    public void onThumbnailLoaded(Task task) {
        mAdapter.updateThumbnail(task, mActivity.isInMultiWindowMode());
    }

    @Override
    public void showMemoryToast() {
        ActivityManager.MemoryInfo info = mSystemServicesProxy.getMemoryInfo();
        long availBefore = mAvailBefore;
        long availNow = info.availMem;
        if (availNow > availBefore) {
            long released = availNow - availBefore;
            String releasedStr = Formatter.formatFileSize(mActivity, released);
            String availStr = Formatter.formatFileSize(mActivity, availNow);
            Toast toast = Toast.makeText(mActivity,
                    mActivity.getString(R.string.recent_app_clean_finished_toast, releasedStr, availStr), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    @Override
    public void dismissRecentsToHome() {
        if (mSystemServicesProxy.isNapp()) {
            if (mActivity.isInMultiWindowMode()) {
                FreemeEventBus.getDefault().send(new FreemeHideRecentsEvent(mEmptyHomeContainer.getVisibility() == View.GONE));
            } else {
                mActivity.finish();
            }
        } else {
            mActivity.finish();
        }
    }

    @Override
    public void dismissRecentsToLaunchTargetTaskOrHome() {
        if (mSystemServicesProxy.isNapp()) {
            if (mActivity.isInMultiWindowMode()) {
                FreemeEventBus.getDefault().send(new FreemeHideRecentsEvent(mEmptyHomeContainer.getVisibility() == View.GONE));
            } else {
                mActivity.finish();
            }
        } else {
            mActivity.finish();
        }
    }

    @Override
    public void launchTask(Task task, View view, boolean screenPinningEnabled) {
        FreemeEventBus.getDefault().send(new LaunchTaskEvent(task, view, screenPinningEnabled));
    }

    @Override
    public void startApplicationDetailsActivity(Task task) {
        startApplicationDetailsActivity(task.intent.getComponent().getPackageName());
    }

    @Override
    public void splitTask(Task task) {

    }

    /**** OverlappingStackView.Callback<Task> Implementation ****/
    @Override
    public ArrayList<Task> getData() {
        return mAdapter.getData();
    }

    @Override
    public void loadViewData(WeakReference<ChildView<Task>> cv, Task item) {
        mAdapter.loadViewData(cv, item, mActivity.isInMultiWindowMode());
    }

    @Override
    public void unloadViewData(Task item) {
        mAdapter.unloadViewData(item);
    }

    @Override
    public void onViewDismissed(Task item) {
        mAdapter.onViewDismissed(item);
        FreemeEventBus.getDefault().send(new TaskDismissedEvent(item));
    }

    @Override
    public void onItemClick(WeakReference<ChildView<Task>> cv, Task item, boolean isScreenPin) {
        if (cv !=null && cv.get() != null) {
            mPresenter.onTaskClicked(item, cv.get(), isScreenPin);
        }
    }

    @Override
    public void onItemLongClick(Task item) {
        mPresenter.onTaskLongClicked(item);
    }

    @Override
    public void toggleLockStatus(Task item) {
        mPresenter.switchTaskLockStatus(item);
        mAdapter.updateThumbnail(item, mActivity.isInMultiWindowMode());
    }

    @Override
    public boolean getItemLockStatus(Task item) {
        return item.isLocked();
    }

    @Override
    public void onNoViewsToOverlapping() {

    }
    /**** EventBus events ****/
    public final void onBusEvent(DismissAllTasksEvent event) {
        ActivityManager.MemoryInfo info = mSystemServicesProxy.getMemoryInfo();
        mAvailBefore = info.availMem;
        mOverlappingView.dismissAll(new Runnable() {
            @Override
            public void run() {
                Task[] tasks = mAdapter.getRemoveTasks();
                mAdapter.removeAllTasks();
                render(RecentsUtils.RECENTS_UPDATE_NORMAL);
                mPresenter.removeAllTasks(tasks);
                //*/ freeme.gouzhouping, 20180224. FreemeAppTheme, recents view.
                dismissRecentsToHome();
                //*/
            }
        });
    }

    public final void onBusEvent(TaskDismissedEvent event) {
        mPresenter.removeTask(event.task);
        /*/ freeme.gouzhouping, 20180514. Recents, user actively remove tasks will back to launcher surface.
        if (mAdapter.getCount() == 0) render(RecentsUtils.RECENTS_UPDATE_NORMAL);
        /*/
        if (mAdapter.getCount() == 0) {
            render(RecentsUtils.RECENTS_UPDATE_NORMAL);
            dismissRecentsToHome();
        }
        //*/
    }

    public final void onBusEvent(final LoadTaskDataEvent event) {
        mPresenter.loadTaskData(event.task);
    }

    public final void onBusEvent(ToggleRecentsEvent event) {
        /*/ freeme.gouzhouping, 20180521. Recents. press back to top task or launcher
        dismissRecentsToLaunchTargetTaskOrHome();
        /*/
        if (Recents.getConfiguration().getLaunchState().launchedFromApp
                && (mSystemServicesProxy.getTasks().size() > 0)) {
            if (mPreviousTaskId == mSystemServicesProxy.getTasks().get(0).persistentId) {
                FreemeEventBus.getDefault().post(new LaunchFrontTaskEvent());
            } else {
                dismissRecentsToLaunchTargetTaskOrHome();
            }
        } else {
            dismissRecentsToLaunchTargetTaskOrHome();
        }
        //*/
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        event.getAnimationTrigger().flushLastDecrementRunnables();
    }

    //*/ freeme.gouzhouping, 20180327. Recents, blank view.
    @Override
    public void updateTaskFromTaskList(Task item) {
        mAdapter.updateTaskFromTaskList(item);
    }
    //*/

    //*/ freeme.gouzhouping, 20180511. Recents.
    public final void onBusEvent(LaunchNextTaskRequestEvent event) {
        if ((mSystemServicesProxy.getTasks().size() > 0)) {
            FreemeEventBus.getDefault().post(new LaunchFrontTaskEvent());
        } else {
            dismissRecentsToLaunchTargetTaskOrHome();
        }
    }
    //*/
}
