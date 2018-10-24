package com.freeme.dialer.contacts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A base processor class. One instance processes vCard one import/export request (imports a given
 * vCard or exports a vCard). Expected to be used with {@link ExecutorService}.
 * <p>
 * This instance starts itself with {@link #run()} method, and can be cancelled with
 * {@link #cancel(boolean)}. Users can check the processor's status using {@link #isCancelled()}
 * and {@link #isDone()} asynchronously.
 * <p>
 * {@link #get()} and {@link #get(long, TimeUnit)}, which are form {@link Future}, aren't
 * supported and {@link UnsupportedOperationException} will be just thrown when they are called.
 */
public abstract class FreemeProcessorBase implements RunnableFuture<Object> {

    public abstract int getType();

    @Override
    public abstract void run();

    /**
     * Cancels this operation.
     *
     * @param mayInterruptIfRunning ignored. When this method is called, the instance
     *                              stops processing and finish itself even if the thread is running.
     * @see Future#cancel(boolean)
     */
    @Override
    public abstract boolean cancel(boolean mayInterruptIfRunning);

    @Override
    public abstract boolean isCancelled();

    @Override
    public abstract boolean isDone();

    /**
     * Just throws {@link UnsupportedOperationException}.
     */
    @Override
    public final Object get() {
        throw new UnsupportedOperationException();
    }

    /**
     * Just throws {@link UnsupportedOperationException}.
     */
    @Override
    public final Object get(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }
}
