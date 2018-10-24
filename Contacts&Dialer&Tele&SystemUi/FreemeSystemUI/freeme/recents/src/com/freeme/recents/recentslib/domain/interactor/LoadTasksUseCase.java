package com.freeme.recents.recentslib.domain.interactor;

import android.os.Handler;

import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.Repository;

import java.util.List;
import java.util.concurrent.Executor;

public class LoadTasksUseCase extends UseCase<List<Task>> {
    public LoadTasksUseCase(Repository repository, Executor executor, Handler mainHandler) {
        super (repository, executor, mainHandler);
    }

    public void execute(UseCaseCallBack<List<Task>> useCaseCallBack) {
        List list = this.mRepository.getTasks ();
        useCaseCallBack.onNext (list);
        useCaseCallBack.onComplete ();
    }
}

