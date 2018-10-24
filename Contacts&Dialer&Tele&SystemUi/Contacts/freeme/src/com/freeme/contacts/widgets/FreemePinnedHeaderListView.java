package com.freeme.contacts.widgets;

import android.content.Context;
import android.util.AttributeSet;

import com.android.contacts.list.PinnedHeaderListView;

/**
 * fast scroll status maybe lead to ListView stuck.
 * override method "setFastScrollEnabled" and "setFastScrollAlwaysVisible" to block the state change.
 * if stuck happened, use the solution to avoid.
 *
 * this is a workaround (temporary).
 */
public class FreemePinnedHeaderListView extends PinnedHeaderListView {

    public FreemePinnedHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setFastScrollEnabled(boolean enabled) {
        // ignore
    }

    @Override
    public void setFastScrollAlwaysVisible(boolean alwaysShow) {
        // ignore
    }
}
