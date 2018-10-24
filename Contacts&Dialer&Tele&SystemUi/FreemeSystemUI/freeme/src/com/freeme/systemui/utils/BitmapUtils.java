package com.freeme.systemui.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;

public class BitmapUtils {

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        return getRoundedCornerBitmap(bitmap, 0, 0, w, h, w / 2, h / 2);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int x, int y,
                                                int w, int h, int rx, int ry) {
        Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect srcRect = new Rect(x, y, x + w, y + h);
        final Rect destRect = new Rect(0, 0, w, h);
        final RectF rectF = new RectF(destRect);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, rx, ry, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, srcRect, destRect, paint);

        return output;
    }

    public static Bitmap getRoundedCornerBitmapFromByte(byte[] data) {
        if (data != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bmp != null) {
                return getRoundedCornerBitmap(bmp, 0, 0,
                        bmp.getWidth(), bmp.getHeight(),
                        bmp.getWidth() / 2, bmp.getHeight() / 2);
            }
        }
        return null;
    }

    public static Bitmap getRoundedCornerBitmapFromResouce(Resources res, int res_id) {
        try {
            Bitmap bmp = BitmapFactory.decodeResource(res, res_id);
            if (bmp != null) {
                return getRoundedCornerBitmap(bmp, 0, 0,
                        bmp.getWidth(), bmp.getHeight(),
                        bmp.getWidth() / 2, bmp.getHeight() / 2);
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable != null) {
            return drawableToBitmap(drawable, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
        }
        return null;
    }

    public static Bitmap getBitmapFromDrawableRes(Resources res, int drawableResId, int color) {
        Drawable drawable = res.getDrawable(drawableResId, null);
        if (drawable != null) {
            drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawable) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }
}