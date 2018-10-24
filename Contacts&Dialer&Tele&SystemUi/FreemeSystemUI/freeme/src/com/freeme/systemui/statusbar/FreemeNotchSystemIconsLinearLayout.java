package com.freeme.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.freeme.systemui.utils.NotchUtils;

import java.util.Locale;

public class FreemeNotchSystemIconsLinearLayout extends LinearLayout {
    private static final String TAG = "FreemeNotchSystemIconsLinearLayout";
    DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    private int mPaddingEnd;
    private boolean mIsLandScape;

    public FreemeNotchSystemIconsLinearLayout(Context context) {
        super(context);
    }

    public FreemeNotchSystemIconsLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FreemeNotchSystemIconsLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mPaddingEnd = getContext().getResources().getDimensionPixelOffset(R.dimen.notch_status_bar_end_padding);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (getDisplay() != null) {
            getDisplay().getMetrics(mDisplayMetrics);
        }
        super.onConfigurationChanged(newConfig);

        mIsLandScape = (newConfig.getLayoutDirection() == Configuration.ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDisplayMetrics.widthPixels == 0 && getDisplay() != null) {
            getDisplay().getMetrics(mDisplayMetrics);
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(Math.max(mDisplayMetrics.widthPixels,
                mDisplayMetrics.heightPixels), Integer.MIN_VALUE), heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int offset;
        int i;
        super.onLayout(changed, left, top, right, bottom);
        View parent = (View) getParent();
        if (isFaAr(Locale.getDefault().getLanguage())) {
            offset = left - (parent.getLeft() + parent.getPaddingLeft());
            if (offset < 0) {
                for (i = 0; i < getChildCount(); i++) {
                    getChildAt(i).offsetLeftAndRight(-offset);
                }
            }
        } else {
            offset = right - (parent.getRight() - parent.getPaddingRight());
            if (offset > 0) {
                for (i = 0; i < getChildCount(); i++) {
                    getChildAt(i).offsetLeftAndRight(-offset);
                }
            }
        }
        if (changed) {
            Log.i(TAG, "onLayout:left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + ", offset=" + offset);
        }
        LinearLayout statusIcons = (LinearLayout) findViewById(R.id.statusIcons);
        if (statusIcons != null) {
            for (i = 0; i < statusIcons.getChildCount(); i++) {
                View child = statusIcons.getChildAt(i);
                child.setAlpha(isOverflow(child) ? 0.0f : 1.0f);
            }
        }
        View networkSpeedView = findViewById(R.id.network_speed_view);
        if (networkSpeedView != null) {
            float f;
            if (isOverflow(networkSpeedView)) {
                f = 0.0f;
            } else {
                f = 1.0f;
            }
            networkSpeedView.setAlpha(f);
        }
    }

    public boolean isOverflow(View child) {
        View parent = getSystemIcon(child);
        if (parent == null) {
            Log.i(TAG, "isOverflow failed, child=" + child);
            return false;
        }
        int[] parentLoc = new int[2];
        parent.getLocationInWindow(parentLoc);
        int[] childLoc = new int[2];
        child.getLocationInWindow(childLoc);

        if ( NotchUtils.hasNotch() && !mIsLandScape &&
                getWidth() + child.getWidth() + mPaddingEnd > getSystemAreaWidth()) {
            return true;
        }

        if (isFaAr(Locale.getDefault().getLanguage())) {
            if (childLoc[0] + child.getWidth() > parentLoc[0] + parent.getWidth()) {
                return true;
            }
        } else if (childLoc[0] < parentLoc[0]) {
            return true;
        }
        return false;
    }

    static View getSystemIcon(View child) {
        if (child == null) {
            return null;
        }
        View parent = (View) child.getParent();
        while (parent != null && !(parent instanceof FreemeNotchSystemIconsLinearLayout)) {
            parent = (View) parent.getParent();
        }
        return parent;
    }

    private static boolean isFaAr(String s) {
        return ("ar".equals(s) || "fa".equals(s) || "iw".equals(s)) ? true : "ur".equals(s);
    }

    private int getSystemAreaWidth() {
        return NotchUtils.hasNotch() ?
                (getDisplay().getWidth() - NotchUtils.getNotchWidth()) / 2 : 0;
    }
}
