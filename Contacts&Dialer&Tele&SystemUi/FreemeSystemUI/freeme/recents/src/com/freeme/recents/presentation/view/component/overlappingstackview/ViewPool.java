package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.content.Context;

import java.util.Iterator;
import java.util.LinkedList;

public class ViewPool<V, T> {

    /* An interface to the consumer of a view pool */
    public interface ViewPoolConsumer<V, T> {
        V createView(Context context);

        void prepareViewToEnterPool(V v);

        void prepareViewToLeavePool(V v, T prepareData, boolean isNewView);

        boolean hasPreferredData(V v, T preferredData);
    }

    Context mContext;
    ViewPoolConsumer<V, T> mViewCreator;
    LinkedList<V> mPool = new LinkedList<V>();

    /**
     * Initializes the pool with a fixed predetermined pool size
     */
    public ViewPool(Context context, ViewPoolConsumer<V, T> viewCreator) {
        mContext = context;
        mViewCreator = viewCreator;
    }

    /**
     * Returns a view into the pool
     */
    void returnViewToPool(V v) {
        mViewCreator.prepareViewToEnterPool(v);
        mPool.push(v);
    }

    /**
     * Gets a view from the pool and prepares it
     */
    V pickUpViewFromPool(T preferredData, T prepareData) {
        V v = null;
        boolean isNewView = false;
        if (mPool.isEmpty()) {
            v = mViewCreator.createView(mContext);
            isNewView = true;
        } else {
            // Try and find a preferred view
            Iterator<V> iter = mPool.iterator();
            while (iter.hasNext()) {
                V vpv = iter.next();
                if (mViewCreator.hasPreferredData(vpv, preferredData)) {
                    v = vpv;
                    iter.remove();
                    break;
                }
            }
            // Otherwise, just grab the first view
            if (v == null) {
                v = mPool.pop();
            }
        }
        mViewCreator.prepareViewToLeavePool(v, prepareData, isNewView);
        return v;
    }

    /**
     * Returns an iterator to the list of the views in the pool.
     */
    Iterator<V> poolViewIterator() {
        if (mPool != null) {
            return mPool.iterator();
        }
        return null;
    }
}
