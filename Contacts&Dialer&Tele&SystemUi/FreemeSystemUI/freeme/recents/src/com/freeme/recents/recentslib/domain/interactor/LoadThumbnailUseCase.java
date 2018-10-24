package com.freeme.recents.recentslib.domain.interactor;

import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.Repository;

import java.util.concurrent.Executor;

public class LoadThumbnailUseCase extends UseCase<Task> {
    private final Task mTask;

    public LoadThumbnailUseCase(Task task, Repository repository, Executor executor, Handler mainHandler) {
        super (repository, executor, mainHandler);
        this.mTask = task;
    }

    public void execute(final UseCaseCallBack<Task> useCaseCallBack) {
        mExecutor.execute (new Runnable () {
            public void run() {
                Drawable thumbnail = mRepository.getTaskThumbnail(mTask);
                mTask.setThumbnail (thumbnail);
                Drawable icon = mRepository.getTaskIcon (mTask);
                mTask.setIcon (icon);
                mTask.setLoaded (true);
                mMainHandler.post (new Runnable () {
                    public void run() {
                        useCaseCallBack.onNext (mTask);
                    }
                });
                mMainHandler.post (new Runnable () {
                    public void run() {
                        useCaseCallBack.onComplete ();
                    }
                });
            }
        });
    }
}

