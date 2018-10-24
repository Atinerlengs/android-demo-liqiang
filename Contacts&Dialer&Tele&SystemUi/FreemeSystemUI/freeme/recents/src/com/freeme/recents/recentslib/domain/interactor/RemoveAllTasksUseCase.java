package com.freeme.recents.recentslib.domain.interactor;

import android.os.Handler;

import java.util.concurrent.Executor;

import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.Repository;

public class RemoveAllTasksUseCase extends UseCase {
    private final Task[] mTasks;

    public RemoveAllTasksUseCase(Task[] tasks, Repository repository, Executor executor, Handler mainHandler) {
        super (repository, executor, mainHandler);
        mTasks = tasks;
    }

    public void execute(final UseCaseCallBack useCaseCallBack) {
        mRepository.removeAllTasks (mTasks);
        mMainHandler.postDelayed(() -> {
            useCaseCallBack.onComplete ();
        }, mTasks.length * 500);
    }
}


