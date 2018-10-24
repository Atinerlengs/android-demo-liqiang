package com.freeme.recents.recentslib.domain.interactor;

public abstract interface UseCaseCallBack<T>
{
  public abstract void onNext(T paramT);

  public abstract void onComplete();

  public abstract void onError(Exception paramException);
}

