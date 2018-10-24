package com.freeme.game.floatingui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.WindowManager;

class GmFloatingViewHelper implements ViewManager {

    private final List<View> mViewLists;
    private WindowManager mWindowManager;

    GmFloatingViewHelper(WindowManager wm) {
        mWindowManager = wm;
        mViewLists = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams params) {
        if (view == null || params == null) {
            return;
        }
        synchronized (mViewLists) {
            if (!mViewLists.contains(view)) {
                mWindowManager.addView(view, params);
                mViewLists.add(view);
            }
        }
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (view == null || params == null) {
            return;
        }

        synchronized (mViewLists) {
            if (mViewLists.contains(view)) {
                mWindowManager.updateViewLayout(view, params);
            }
        }
    }

    @Override
    public void removeView(View view) {
        if (view == null) {
            return;
        }

        synchronized (mViewLists) {
            if (mViewLists.contains(view)) {
                mWindowManager.removeView(view);
                mViewLists.remove(view);
            }
        }
    }

    void removeAllView() {
        synchronized (mViewLists) {
            if (mViewLists.size() > 0) {
                for (View v : mViewLists) {
                    mWindowManager.removeView(v);
                }
                mViewLists.clear();
            }
        }
    }
}
