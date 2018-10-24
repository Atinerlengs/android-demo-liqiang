package com.freeme.recents.recentslib.domain.interactor;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.Repository;
import com.freeme.recents.recentslib.domain.interactor.UseCase;
import com.freeme.recents.recentslib.domain.interactor.UseCaseCallBack;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class LoadThumbnailsUseCase extends UseCase<Task> {
    private final LinkedBlockingQueue<Task> mTasksWaitingForThumbnails;
    private final int mMaxVisibleTaskNum;

    public LoadThumbnailsUseCase(int maxVisibleTaskNum, LinkedBlockingQueue<Task> tasksWaitingForThumbnails, Repository repository, Executor executor, Handler mainHandler) {
        super (repository, executor, mainHandler);
        this.mTasksWaitingForThumbnails = tasksWaitingForThumbnails;
        this.mMaxVisibleTaskNum = maxVisibleTaskNum;
    }

    public void execute(final UseCaseCallBack<Task> useCaseCallBack) {
        this.mExecutor.execute (new Runnable () {
            public void run() {
                int num = 0;
                while (true)
                    try {
                        final Task task = (Task)mTasksWaitingForThumbnails.take();
                        if (task.isNull ()) {
                            break;
                        }
                        if ((num < mMaxVisibleTaskNum) || (task.isHome())) {
                            Drawable thumbnail = mRepository.getTaskThumbnail(task);
                            task.setThumbnail (thumbnail);
                            Drawable icon = mRepository.getTaskIcon(task);
                            task.setIcon (icon);
                            task.setLoaded (true);
                        }
                        if (!task.isHome ()) num++;
                        mMainHandler.post (new Runnable () {
                            public void run() {
                                useCaseCallBack.onNext(task);
                            }
                        });
                    } catch (InterruptedException localInterruptedException) {
                    }
                mMainHandler.post (new Runnable () {
                    public void run() {
                        useCaseCallBack.onComplete ();
                    }
                });
            }
        });
    }
}

