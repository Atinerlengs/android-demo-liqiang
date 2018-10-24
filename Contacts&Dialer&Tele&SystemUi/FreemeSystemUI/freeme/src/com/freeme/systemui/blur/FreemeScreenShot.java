package com.freeme.systemui.blur;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.WindowManager;

public class FreemeScreenShot {
    public static Bitmap screenShotBitmap(Context ctx, int minLayer, int maxLayer, float scale, Rect rect) {
        Bitmap bitmap;
        if (rect == null) {
            rect = new Rect();
        }
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display display = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        display.getRealMetrics(displayMetrics);
        int[] dims = new int[]{(((int) (((float) displayMetrics.widthPixels) * scale)) / 2) * 2,
                (((int) (((float) displayMetrics.heightPixels) * scale)) / 2) * 2};
        int rotation = display.getRotation();
        if (Configuration.ORIENTATION_UNDEFINED == rotation
                || Configuration.ORIENTATION_LANDSCAPE == rotation) {
            bitmap = SurfaceControl.screenshot(dims[0], dims[1 ]);
        } else {
            bitmap = rotationScreenBitmap(rect, rotation, dims, minLayer, maxLayer);
        }
        if (bitmap == null) {
            Log.e("ScreenShotHelper", "screenShotBitmap error bitmap is null");
            WallpaperManager wm = WallpaperManager.getInstance(ctx);
            if (wm != null) {
                bitmap = wm.getBitmap().copy(Config.ARGB_8888, true);
            }
            if (bitmap == null) {
                return null;
            }
        }
        bitmap.prepareToDraw();
        return bitmap;
    }

    public static Bitmap rotationScreenBitmap(Rect rect, int rotation, int[] srcDims, int minLayer, int maxLayer) {
        float degrees = convertRotationToDegrees(rotation);
        float[] dims = new float[]{(float) srcDims[0], (float) srcDims[1]};
        Matrix metrics = new Matrix();
        metrics.reset();
        metrics.preRotate(-degrees);
        metrics.mapPoints(dims);
        dims[0] = Math.abs(dims[0]);
        dims[1] = Math.abs(dims[1]);
        Bitmap bitmap = SurfaceControl.screenshot(rect, (int) dims[0], (int) dims[1], minLayer, maxLayer, false, 0);
        Bitmap ss = Bitmap.createBitmap(srcDims[0], srcDims[1], Config.ARGB_8888);
        Canvas c = new Canvas(ss);
        c.translate(((float) srcDims[0]) / 2.0f, ((float) srcDims[1]) / 2.0f);
        c.rotate(degrees);
        c.translate((-dims[0]) / 2.0f, (-dims[1]) / 2.0f);
        c.drawBitmap(bitmap, 0.0f, 0.0f, null);
        bitmap.recycle();
        bitmap = ss;
        return ss;
    }

    private static float convertRotationToDegrees(int rotation) {
        switch (rotation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return 270.0f;
            case Configuration.ORIENTATION_LANDSCAPE:
                return 180.0f;
            case Configuration.ORIENTATION_SQUARE:
                return 90.0f;
            default:
                return 0.0f;
        }
    }

    public static int converseRotation(int rotation) {
        switch (rotation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return Configuration.ORIENTATION_SQUARE;
            case Configuration.ORIENTATION_LANDSCAPE:
                return Configuration.ORIENTATION_LANDSCAPE;
            case Configuration.ORIENTATION_SQUARE:
                return Configuration.ORIENTATION_PORTRAIT;
            default:
                return Configuration.ORIENTATION_UNDEFINED;
        }
    }
}
