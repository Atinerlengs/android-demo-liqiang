package com.freeme.recents.presentation.presenter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.freeme.provider.FreemeSettings;
import com.freeme.recents.FreemeWhiteListHelper;
import com.freeme.recents.RecentsUtils;
import com.freeme.recents.recentslib.data.executor.JobExecutor;
import com.freeme.recents.recentslib.data.repository.Repository;
import com.freeme.recents.recentslib.domain.interactor.LoadTasksUseCase;
import com.freeme.recents.recentslib.domain.interactor.LoadThumbnailUseCase;
import com.freeme.recents.recentslib.domain.interactor.LoadThumbnailsUseCase;
import com.freeme.recents.recentslib.domain.interactor.RemoveAllTasksUseCase;
import com.freeme.recents.recentslib.domain.interactor.RemoveTaskUseCase;
import com.freeme.recents.recentslib.domain.interactor.TrimMemoryUseCase;
import com.freeme.recents.recentslib.domain.interactor.UseCase;
import com.freeme.recents.recentslib.domain.interactor.UseCaseCallBack;
import com.freeme.recents.recentslib.data.model.Task;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class RecentsPresenter implements Presenter {

    private static final String TAG = "RecentsPresenter";
    private final Context mContext;
    private final Repository mRepository;
    private final JobExecutor mExecutor;
    private final Handler mMainHandler;
    private RecentsView mRecentsView;
    private final UseCase<List<Task>> mLoadTasksUseCase;
    private final RemoveTaskUseCase mRemoveTaskUseCase;
    private UseCase<Task> mLoadThumbnaislUseCase;
    private final LinkedBlockingQueue<Task> tasksWaitingForThumbnails =
            new LinkedBlockingQueue<>();
    private SharedPreferences mSharedPreferences;
    protected long mLastOperationTime;
    private final static int MIN_OPERATION_DELAY_MS = 1000;
    public static final int RECENTS_MAX_TASK_NUM = 4;
    private boolean initialized;
    private SystemUIApplication msystemuiApplication;
    private boolean mShowToastAfterClean ;
    public interface RecentsView {
        void onTaskLoaded(Task task);
        void onThumbnailLoaded(Task task);
        void render(int type);
        void dismissRecentsToHome();
        void dismissRecentsToLaunchTargetTaskOrHome();
        void showMemoryToast();
        void launchTask(Task task, View view, boolean screenPinningEnabled);
        void startApplicationDetailsActivity(Task task);
        void splitTask(Task task);
    }

    public RecentsPresenter(Context context) {
        mContext = context;
        msystemuiApplication =
                (SystemUIApplication) ((Activity) mContext).getApplication();
        mShowToastAfterClean = mContext.getResources().getBoolean(R.bool.show_toast_after_clean_memory);
        mRepository = msystemuiApplication.getRepository();
        mExecutor = msystemuiApplication.getExecutor();
        mMainHandler = msystemuiApplication.getMainHandler();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mLoadThumbnaislUseCase = new LoadThumbnailsUseCase(RECENTS_MAX_TASK_NUM,
                tasksWaitingForThumbnails, mRepository, mExecutor,
                mMainHandler);
        mLoadTasksUseCase = new LoadTasksUseCase(mRepository,
                mExecutor, mMainHandler);
        mRemoveTaskUseCase =
                new RemoveTaskUseCase(mRepository, mExecutor, mMainHandler);
        //*/ freeme.gouzhouping, 20180408. Recents, white list.
        loadRecentsLockList();
        //*/
    }

    /**** Presenter Implementation ****/
    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;

        mLoadTasksUseCase.execute(new UseCaseCallBack<List<Task>>() {
            @Override
            public void onNext(List<Task> next) {
                if (next != null) {
                    tasksWaitingForThumbnails.clear();
                    while (true) {
                        try {
                            for (Task task : next) {
                                tasksWaitingForThumbnails.put(task);
                            }
                            break;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                while (true) {
                    try {
                        tasksWaitingForThumbnails.put(new Task());
                        break;
                    } catch (InterruptedException e) {
                    }
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });

        mLoadThumbnaislUseCase.execute(new UseCaseCallBack<Task>() {
            @Override
            public void onNext(Task next) {
                if (getTaskLockStatus(next)) {
                    next.setLocked(true);
                }
                mRecentsView.onTaskLoaded(next);
            }

            @Override
            public void onComplete() {
                mRecentsView.render(RecentsUtils.RECENTS_UPDATE_INDICATOR);
                mRecentsView.render(RecentsUtils.RECENTS_UPDATE_MEMORY);
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    public void preloadTasks() {
        if (initialized) {
            return;
        }

        initialized = true;

        mLoadTasksUseCase.execute(new UseCaseCallBack<List<Task>>() {
            @Override
            public void onNext(List<Task> next) {
                if (next != null) {
                    tasksWaitingForThumbnails.clear();
                    while (true) {
                        try {
                            for (Task task : next) {
                                tasksWaitingForThumbnails.put(task);
                                task.setLocked(getTaskLockStatus(task));
                                mRecentsView.onTaskLoaded(task);
                            }
                            break;
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                while (true) {
                    try {
                        tasksWaitingForThumbnails.put(new Task());
                        break;
                    } catch (InterruptedException e) {
                    }
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });

        FreemeWhiteListHelper.getInstance(mContext).loadInputMethods();

        mLoadThumbnaislUseCase.execute(new UseCaseCallBack<Task>() {
            @Override
            public void onNext(Task next) {
                FreemeWhiteListHelper.getInstance(mContext).checkInputMethodApp(next.getPackageName());
            }

            @Override
            public void onComplete() {
                mRecentsView.render(RecentsUtils.RECENTS_UPDATE_INDICATOR);
                mRecentsView.render(RecentsUtils.RECENTS_UPDATE_MEMORY);
                FreemeWhiteListHelper.getInstance(mContext).reloadData();
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    @Override
    public void resume() {
    }

    @Override
    public void pause() {

    }

    @Override
    public void destroy() {

    }

    public void setView(RecentsView view) {
        mRecentsView = view;
    }

    public void removeAllTasks(Task[] tasks) {
        new RemoveAllTasksUseCase(tasks, mRepository, mExecutor, mMainHandler)
            .execute(new UseCaseCallBack() {
            @Override
            public void onNext(Object next) {

            }

            @Override
            public void onComplete() {
                if (mShowToastAfterClean) {
                    mRecentsView.showMemoryToast();
                }
                mRecentsView.render(RecentsUtils.RECENTS_UPDATE_MEMORY);
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    public void removeTask(Task task) {
        mRemoveTaskUseCase.execute(new UseCaseCallBack() {
            @Override
            public void onNext(Object next) {

            }

            @Override
            public void onComplete() {
                //*/ freeme.gouzhouping, 20180404. Recents, white list.
                if (task.isLocked()) {
                    mRecentsLockList.remove(task.getPackageName());
                }
                saveRecentsLockList(mRecentsLockList);
                //*/
                mRecentsView.render(RecentsUtils.RECENTS_UPDATE_MEMORY);
            }

            @Override
            public void onError(Exception e) {

            }
        }, task);
    }

    public void switchTaskLockStatus(Task task) {
        boolean isLocked = task.isLocked();
        task.setLocked(isLocked ? false : true);
        /*/ freeme.gouzhouping, 20180404. Recents, white list.
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        if (isLocked) {
            editor.remove(task.toStringKey());
        } else {
            editor.putInt(task.toStringKey(), task.taskId);
        }
        editor.apply();
        /*/
        if (isLocked) {
            mRecentsLockList.remove(task.getPackageName());
        } else {
            mRecentsLockList.add(task.getPackageName());
        }
        saveRecentsLockList(mRecentsLockList);
        //*/
    }

    public boolean getTaskLockStatus(Task task) {
        //*/ freeme.gouzhouping, 20180404. Recents, white list.
        loadRecentsLockList();
        return hasLocked(task);
        /*/
        return mSharedPreferences.contains(task.toStringKey());
        //*/
    }

    public void loadTaskData(Task task) {
        UseCase<Task> loadThumbnailUseCase
                = new LoadThumbnailUseCase(task, mRepository, mExecutor, mMainHandler);
        loadThumbnailUseCase.execute(new UseCaseCallBack<Task>() {
            @Override
            public void onNext(Task next) {
                mRecentsView.onThumbnailLoaded(next);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    public void onTrimMemory(int level) {
        TrimMemoryUseCase trimMemoryUseCase = new TrimMemoryUseCase(level,
                mRepository, mExecutor, mMainHandler);
        trimMemoryUseCase.execute(null);
    }

    public void onEmptyHomeClicked() {
        mRecentsView.dismissRecentsToHome();
    }


    public void onDismissAllButtonClicked() {
        long elapsedTime = SystemClock.elapsedRealtime() - mLastOperationTime;

        if (elapsedTime < MIN_OPERATION_DELAY_MS) {
            return;
        }

        mLastOperationTime = SystemClock.elapsedRealtime();
        mRecentsView.render(RecentsUtils.RECENTS_UPDATE_DISMISS_ALL_TASKS);
    }

    public void onTaskClicked(Task task, View view, boolean screenPinningEnabled) {
        mRecentsView.launchTask(task, view, screenPinningEnabled);
    }

    public void onTaskSplitClicked(Task task) {
        mRecentsView.splitTask(task);
    }

    public void onTaskLongClicked(Task task) {
        mRecentsView.startApplicationDetailsActivity(task);
    }

    //*/ freeme.gouzhouping, 20180408. Recents, white list.
    private ArraySet<String> mRecentsLockList = new ArraySet<>();
    private static final String DIVIDER = ":";

    private void loadRecentsLockList() {
        String locklist = Settings.System.getString(mContext.getContentResolver(),
                FreemeSettings.System.FREEME_SYSUI_RECENT_LOCKED_TASKS);
        if (TextUtils.isEmpty(locklist)) {
            return;
        }

        String[] tempLockList = TextUtils.split(locklist, DIVIDER);
        for (String str : tempLockList) {
            if (!TextUtils.isEmpty(str)) {
                mRecentsLockList.add(str);
            }
        }
    }

    private boolean hasLocked(Task task) {
        return mRecentsLockList.contains(task.getPackageName());
    }

    private void saveRecentsLockList(ArraySet<String> recents) {
        ArraySet<String> re = new ArraySet<>(recents);
        String[] locklist = re.toArray(new String[re.size()]);
        String value = TextUtils.join(DIVIDER, locklist);
        Settings.System.putString(mContext.getContentResolver(),
                FreemeSettings.System.FREEME_SYSUI_RECENT_LOCKED_TASKS, value);
    }
    //*/

}
