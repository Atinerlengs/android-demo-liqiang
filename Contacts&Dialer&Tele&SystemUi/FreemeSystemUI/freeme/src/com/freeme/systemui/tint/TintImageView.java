package com.freeme.systemui.tint;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.RemotableViewMethod;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.tuner.TunerService;

import java.util.Observable;
import java.util.Observer;

public class TintImageView extends ImageView implements DarkIconDispatcher.DarkReceiver {
    private static final String TAG = "TintImageView";
    public static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    protected boolean mIsResever = true;
    private int mTintColor = Color.WHITE;

    public TintImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TintImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TintImageView(Context context) {
        super(context);
    }

    public TintImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onAttachedToWindow() {
        setTint();
        super.onAttachedToWindow();
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver((DarkIconDispatcher.DarkReceiver)this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver((DarkIconDispatcher.DarkReceiver)this);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable != null) {
            drawable.mutate();
            setTint();
        }
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mTintColor = tint;
        setTint();
    }

    @Override
    @RemotableViewMethod
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        if (getDrawable() != null) {
            getDrawable().mutate();
            setTint();
        }
    }

    public void setTint() {
        if (getVisibility() == VISIBLE) {
            Drawable drawable = getDrawable();
            if (drawable != null) {
                setTint(drawable);
            }
        }
    }

    protected void setTint(Drawable drawable) {
        if (drawable != null) {
            if (mIsResever) {
                int color = mTintColor;
                if (Color.WHITE != color) {
                    drawable.setTintList(null);
                    drawable.setTint(color);
                } else {
                    drawable.setTintList(null);
                    drawable.setTint(color);
                }
                invalidate();
            }
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == VISIBLE) {
            setTint();
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    @Override
    public void setImageTintList(ColorStateList tint) {
        if (DEBUG) {
            Log.d(TAG, "setImageTintList:" + tint + " " + this);
        }
        if (mIsResever) {
            super.setImageTintList(tint);
        }
    }

    public void setIsResever(boolean isResever) {
        if (DEBUG) {
            Log.d(TAG, "setIsResever:" + isResever + " " + this);
        }
        mIsResever = isResever;
        if (!isResever && getDrawable() != null) {
            getDrawable().setTintList(null);
        }
    }
}
