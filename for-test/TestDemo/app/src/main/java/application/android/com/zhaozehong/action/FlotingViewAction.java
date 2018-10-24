package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

import application.android.com.zhaozehong.demoapplication.R;

public class FlotingViewAction extends Action {

    private WindowManager wm;
    private ImageView mFloatingView;

    public FlotingViewAction(Activity activity) {
        super(activity);
        wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public String getName() {
        return "FlotingView";
    }

    @Override
    public void doAction() {
//        int mFloatingBtnSize = 200;
//        DisplayMetrics dm = mActivity.getResources().getDisplayMetrics();
//        int mFloatingBtnXPoint = dm.widthPixels / 3;
//        int mFloatingBtnYPoint = dm.heightPixels / 2 - 75;
//
//        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
//        mWindowParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
//
//        mWindowParams.format = PixelFormat.RGBA_8888;
//        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//        mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
//        mWindowParams.x = mFloatingBtnXPoint;
//        mWindowParams.y = mFloatingBtnYPoint;
//        mWindowParams.width = mFloatingBtnSize;
//        mWindowParams.height = mFloatingBtnSize;
//
//        if (mFloatingView == null) {
//            mFloatingView = new ImageView(mActivity);
//            mFloatingView.setImageResource(R.drawable.ic_launcher_background);
//        }
//
//        wm.addView(mFloatingView, mWindowParams);
        Intent service = new Intent("com.freeme.intent.action.GAMEMODE_TOOL");
        service.setPackage("com.freeme.game");
        mActivity.startService(service);

    }

    @Override
    public boolean onBackPress() {
        if (removeView()) {
            return true;
        }
        return super.onBackPress();
    }

    @Override
    public void onDestroy() {
        removeView();
    }

    private boolean removeView() {
        if (mFloatingView != null) {
            wm.removeView(mFloatingView);
            mFloatingView = null;
            return true;
        }
        return false;
    }
}
