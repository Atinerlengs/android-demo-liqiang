package com.freeme.recents.recentslib.domain.interactor;

import android.os.Handler;

import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.Repository;

import java.util.concurrent.Executor;

public class RemoveTaskUseCase extends UseCase {
    Task mTask;
    private UseCaseCallBack mUseCaseCallBack;
    private final Runnable mRunnable = new Runnable () {
        public void run() {
            if (RemoveTaskUseCase.this.mUseCaseCallBack != null)
                RemoveTaskUseCase.this.mUseCaseCallBack.onComplete ();
        }
    };

    public RemoveTaskUseCase(Repository repository, Executor executor, Handler mainHandler) {
        super (repository, executor, mainHandler);
    }

    public void execute(UseCaseCallBack useCaseCallBack) {
        this.mRepository.removeTask (this.mTask);
        this.mMainHandler.removeCallbacks (this.mRunnable);
        this.mMainHandler.postDelayed (this.mRunnable, 500L);
    }

    public void execute(UseCaseCallBack useCaseCallBack, Task task) {
        this.mUseCaseCallBack = useCaseCallBack;
        this.mTask = task;
        execute (useCaseCallBack);
    }
}


