package com.freeme.recents;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.util.ArrayList;

public final class RecentsUtils {

    public final static int EVENT_BUS_PRIORITY = 1;
    public final static int FREEME_SHOW_SCREEN_PIN_REQUEST = 1;
    public final static int FREEME_RECENTS_DRAWN = 3;
    public final static int RECENTS_UPDATE_NORMAL = 0;
    public final static int RECENTS_UPDATE_INDICATOR = 1;
    public final static int RECENTS_UPDATE_MEMORY = 2;
    public final static int RECENTS_UPDATE_DISMISS_ALL_TASKS = 3;

    private RecentsUtils() {
    }

    public static float dp2px(Resources resources, float dp) {
        final float scale = resources.getDisplayMetrics().density;
        return  dp * scale + 0.5f;
    }

    public static float sp2px(Resources resources, float sp){
        final float scale = resources.getDisplayMetrics().scaledDensity;
        return sp * scale;
    }

    public static Bitmap BitmapRotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        tmpBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(),
                tmpBitmap.getHeight(), matrix,true);
        return tmpBitmap;
    }
}
