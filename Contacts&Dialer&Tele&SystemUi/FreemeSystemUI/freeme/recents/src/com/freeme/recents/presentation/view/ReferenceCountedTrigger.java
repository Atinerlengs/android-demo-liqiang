package com.freeme.recents.presentation.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import java.util.ArrayList;

/**
 * A ref counted trigger that does some logic when the count is first incremented, or last
 * decremented.  Not thread safe as it's not currently needed.
 */
public class ReferenceCountedTrigger {

    int mCount;
    ArrayList<Runnable> mFirstIncRunnables = new ArrayList<>();
    ArrayList<Runnable> mLastDecRunnables = new ArrayList<>();
    Runnable mErrorRunnable;

    public ReferenceCountedTrigger() {
        this(null, null, null);
    }

    public ReferenceCountedTrigger(Runnable firstIncRunnable, Runnable lastDecRunnable,
                                   Runnable errorRunanable) {
        if (firstIncRunnable != null) mFirstIncRunnables.add(firstIncRunnable);
        if (lastDecRunnable != null) mLastDecRunnables.add(lastDecRunnable);
        mErrorRunnable = errorRunanable;
    }

    /** Increments the ref count */
    public void increment() {
        if (mCount == 0 && !mFirstIncRunnables.isEmpty()) {
            int numRunnables = mFirstIncRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mFirstIncRunnables.get(i).run();
            }
        }
        mCount++;
    }

    /** Adds a runnable to the last-decrement runnables list. */
    public void addLastDecrementRunnable(Runnable r) {
        mLastDecRunnables.add(r);
    }

    /** Decrements the ref count */
    public void decrement() {
        mCount--;
        if (mCount == 0) {
            flushLastDecrementRunnables();
        } else if (mCount < 0) {
            if (mErrorRunnable != null) {
                mErrorRunnable.run();
            } else {
                throw new RuntimeException("Invalid ref count");
            }
        }
    }

    /**
     * Runs and clears all the last-decrement runnables now.
     */
    public void flushLastDecrementRunnables() {
        if (!mLastDecRunnables.isEmpty()) {
            int numRunnables = mLastDecRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mLastDecRunnables.get(i).run();
            }
        }
        mLastDecRunnables.clear();
    }

    public void removeLastDecrementRunnables() {
        mLastDecRunnables.clear();
    }

    /** Returns the current ref count */
    public int getCount() {
        return mCount;
    }
}
