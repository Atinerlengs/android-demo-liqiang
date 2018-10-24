package com.freeme.recents.recentslib.domain.interactor;

import android.os.Handler;

import com.freeme.recents.recentslib.data.repository.Repository;

import java.util.concurrent.Executor;

public class TrimMemoryUseCase extends UseCase {
    private final int mLevel;

    public TrimMemoryUseCase(int level, Repository repository, Executor executor, Handler mainHandler) {
        super (repository, executor, mainHandler);
        this.mLevel = level;
    }

    public void execute(UseCaseCallBack useCaseCallBack) {
        this.mRepository.trimMemory (this.mLevel);
    }
}


