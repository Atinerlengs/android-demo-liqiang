package com.freeme.systemui.tint;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;

import java.util.Observable;
import java.util.Observer;

public class TintTextView extends TextView implements DarkIconDispatcher.DarkReceiver {

    private boolean mIsReverse = true;

    public TintTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TintTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TintTextView(Context context) {
        super(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        if (mIsReverse) {
            setTextColor(DarkIconDispatcher.getTint(area, this, tint));
        }
    }

    public void setIsResever(boolean reverse) {
        mIsReverse = reverse;
    }
}
