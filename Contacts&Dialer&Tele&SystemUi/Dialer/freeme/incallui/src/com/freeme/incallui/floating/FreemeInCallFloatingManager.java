package com.freeme.incallui.floating;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import com.android.incallui.R;

class FreemeInCallFloatingManager {

    private FreemeInCallFloatingView mFloatingView;
    private WindowManager mWindowManager;
    private Context mContext;

    FreemeInCallFloatingManager(Context context) {
        mContext = context;
        mWindowManager = FreemeInCallFloatingUtils.getWindowManager(context);
    }

    void createView() {
        if (mFloatingView == null) {
            Resources res = mContext.getResources();

            SharedPreferences spf = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor editor = spf.edit();
            DisplayMetrics dm = res.getDisplayMetrics();

            int floatingBtnSize = res.getDimensionPixelSize(
                    R.dimen.freeme_incall_floating_view_max_size);

            int floatingBtnXPoint = spf.getInt(
                    FreemeInCallFloatingUtils.PRFERENCE_KEY_FLOATING_BUTTON_X_POINT, -1);
            if (floatingBtnXPoint == -1) {
                floatingBtnXPoint = dm.widthPixels / 3;
                editor.putInt(FreemeInCallFloatingUtils.PRFERENCE_KEY_FLOATING_BUTTON_X_POINT,
                        floatingBtnXPoint);
            }
            int floatingBtnYPoint = spf.getInt(
                    FreemeInCallFloatingUtils.PRFERENCE_KEY_FLOATING_BUTTON_Y_POINT, -1);
            if (floatingBtnYPoint == -1) {
                floatingBtnYPoint = dm.heightPixels / 2 - 75;
                editor.putInt(FreemeInCallFloatingUtils.PRFERENCE_KEY_FLOATING_BUTTON_Y_POINT,
                        floatingBtnYPoint);
            }
            editor.commit();

            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            params.format = PixelFormat.RGBA_8888;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            params.gravity = Gravity.LEFT | Gravity.TOP;
            params.x = floatingBtnXPoint;
            params.y = floatingBtnYPoint;
            params.width = floatingBtnSize;
            params.height = floatingBtnSize;

            mFloatingView = new FreemeInCallFloatingView(mContext);
            mFloatingView.setFreemeInCallFloatingManager(this);
            mFloatingView.setParams(params);

            mWindowManager.addView(mFloatingView, params);
        }
    }

    void setVisibility(boolean visibility) {
        if (mFloatingView != null) {
            mFloatingView.setVisibility(visibility);
        }
    }

    void removeView() {
        if (mFloatingView != null) {
            mWindowManager.removeView(mFloatingView);
            mFloatingView = null;
        }
    }

    void updateView(WindowManager.LayoutParams params) {
        if (mFloatingView != null) {
            mWindowManager.updateViewLayout(mFloatingView, params);
        }
    }
}
