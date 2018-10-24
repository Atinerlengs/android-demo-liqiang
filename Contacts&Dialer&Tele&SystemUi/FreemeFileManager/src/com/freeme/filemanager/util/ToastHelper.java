package com.freeme.filemanager.util;

import android.content.Context;
import android.os.Handler;

public abstract class ToastHelper {

    public static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;

    private static android.widget.Toast toast;
    private static Handler handler = new Handler();


    private static Runnable run = new Runnable() {
        public void run() {
            toast.cancel();
        }
    };

    public static void toast(Context ctx, CharSequence msg, int duration) {
        handler.removeCallbacks(run);
        switch (duration) {
            case LENGTH_SHORT:
                duration = 500;
                break;
            case LENGTH_LONG:
                duration = 3000;
                break;
            default:
                break;
        }
        if (null != toast) {
            toast.setText(msg);
        } else {
            toast = android.widget.Toast.makeText(ctx, msg, duration);
        }
        handler.postDelayed(run, duration);
        toast.show();
    }

    public static void show(Context ctx, CharSequence msg, int duration)
            throws NullPointerException {
        if (null == ctx) {
            throw new NullPointerException("The ctx is null!");
        }
        if (0 > duration) {
            duration = LENGTH_SHORT;
        }
        toast(ctx, msg, duration);
    }

    public static void show(Context ctx, int resId, int duration)
            throws NullPointerException {
        if (null == ctx) {
            throw new NullPointerException("The ctx is null!");
        }
        if (0 > duration) {
            duration = LENGTH_SHORT;
        }
        toast(ctx, ctx.getResources().getString(resId), duration);
    }

}
