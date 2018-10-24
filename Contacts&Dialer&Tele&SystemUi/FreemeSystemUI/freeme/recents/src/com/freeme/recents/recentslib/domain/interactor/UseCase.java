package com.freeme.recents.recentslib.domain.interactor;

import android.os.Handler;

import com.freeme.recents.recentslib.data.repository.Repository;

import java.util.concurrent.Executor;

public abstract class UseCase<T> {
    protected final Executor mExecutor;
    protected final Handler mMainHandler;
    protected final Repository mRepository;

    protected UseCase(Repository repository, Executor executor, Handler mainHandler) {
        this.mExecutor = executor;
        this.mMainHandler = mainHandler;
        this.mRepository = repository;
    }

    public abstract void execute(UseCaseCallBack<T> paramUseCaseCallBack);

    public void cancel() {
        this.mMainHandler.removeCallbacks (null);
    }
}

