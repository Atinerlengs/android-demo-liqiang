package com.freeme.onehand;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;

public class OneHandUtils {
    private static final String TAG = "OneHandUtils";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private static OneHandUtils mSingleton = new OneHandUtils();
    static OneHandUtils getInstance() {
        return mSingleton;
    }

    private static final int MAX_PREVIEW_SIZE = 1024;
    private static final int WALLPAPAER_RESIZE_WIDTH = 1024;

    private PointerCoords[] gSharedTempPointerCoords
            = PointerCoords.createArray(10);
    private PointerProperties[] gSharedTempPointerProperties
            = PointerProperties.createArray(10);

    private Context mContext;
    private long mDownTime;

    private WindowManager mWindowManager;
    private IWindowManager mWindowManagerService;
    private Vibrator mVibrator;
    private TelephonyManager mTelephonyManager;

    private Point mScreenSize = new Point();
    private BitmapDrawable mWallpaperImage;
    private OneHandWindowInfo mWinInfo;

    private class GetWallpaperTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            mWallpaperImage = new BitmapDrawable(mContext.getResources(),
                    getBlurBitmap(mContext, getWallpaperBitmap(), 10));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (DBG) {
                Log.d(TAG, "onPostExecute() isValid=" + isValidBGImage()
                        + ",mWallpaperImage=" + mWallpaperImage);
            }
            mGetWallpaperImageTask = null;
        }
    }
    private GetWallpaperTask mGetWallpaperImageTask;

    private OneHandUtils() {
    }

    void init(Context context) {
        mContext = context.getApplicationContext();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowManagerService = IWindowManager.Stub.asInterface(ServiceManager
                .getService(Context.WINDOW_SERVICE));

        mWinInfo = OneHandWindowInfo.getInstance();

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        boolean hasNavigationBar = false;
        try {
            hasNavigationBar = mWindowManagerService.hasNavigationBar();
        } catch (RemoteException ignored) {
        }
        mWinInfo.setNavigationBarSupportState(hasNavigationBar);
    }

    Bitmap makeBackgroundBmp(Drawable drawable) {
        int height = 0;
        int width = 0;
        if (drawable != null) {
            height = drawable.getIntrinsicHeight();
            width = drawable.getIntrinsicWidth();
        }
        if (drawable == null || height <= 0 || width <= 0) {
            if (DBG) {
                Log.d(TAG, (new StringBuilder(64))
                        .append("makeBackgroundBmp() ")
                        .append("height=").append(height).append(", ")
                        .append("width=").append(width).append(", ")
                        .append("height=").append(height).append(", ")
                        .append("drawable=null")
                        .toString());
            }
            Bitmap bmp = Bitmap.createBitmap(MAX_PREVIEW_SIZE, MAX_PREVIEW_SIZE,
                    Bitmap.Config.ARGB_8888);
            (new Canvas(bmp)).drawColor(Color.BLACK);
            return bmp;
        }

        Bitmap croppedBmp;
        float scale = WALLPAPAER_RESIZE_WIDTH / (float) Math.max(height, width);
        Bitmap bmp = Bitmap.createBitmap((int) (width * scale), (int) (height * scale),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        height = canvas.getHeight();
        width = canvas.getWidth();
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);

        mWindowManager.getDefaultDisplay().getRealSize(mScreenSize);
        int viewWidth = mScreenSize.x;
        int viewHeight = mScreenSize.y;
        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("makeBackgroundBmp() ")
                    .append("width=").append(width).append(", ")
                    .append("height=").append(height).append(", ")
                    .append("viewWidth=").append(viewWidth).append(", ")
                    .append("viewHeight=").append(viewHeight)
                    .toString());
        }

        if ((viewWidth / (float) viewHeight) <= (width / (float) height)) {
            int scaledViewWidth = (int) (viewWidth / (viewHeight / (float) height));
            int offsetX = Math.round((width - scaledViewWidth) / 2.0f);
            if (offsetX < 0) {
                offsetX = 0;
            }
            if (width < scaledViewWidth) {
                scaledViewWidth = width;
            }
            croppedBmp = Bitmap.createBitmap(bmp, offsetX, 0, scaledViewWidth, height);
            if (DBG) {
                Log.d(TAG, (new StringBuilder(64))
                        .append("makeBackgroundBmp() croppedBmp ")
                        .append("offsetX=").append(offsetX).append(", ")
                        .append("scaledViewWidth=").append(scaledViewWidth).append(", ")
                        .append("height=").append(height)
                        .toString());
            }
        } else {
            int scaledViewHeight = (int) (viewHeight / (viewWidth / (float) width));
            int offsetY = Math.round((height - scaledViewHeight) / 2.0f);
            if (offsetY < 0) {
                offsetY = 0;
            }
            if (height < scaledViewHeight) {
                scaledViewHeight = height;
            }
            croppedBmp = Bitmap.createBitmap(bmp, 0, offsetY, width, scaledViewHeight);
            if (DBG) {
                Log.d(TAG, (new StringBuilder(64))
                        .append("makeBackgroundBmp() cropedBmp ")
                        .append("offsetY=").append(offsetY).append(", ")
                        .append("width=").append(width).append(", ")
                        .append("scaledViewHeight=").append(scaledViewHeight)
                        .toString());
            }
        }
        bmp.recycle();
        return croppedBmp;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        return config != null ? config : Bitmap.Config.ARGB_8888;
    }

    Bitmap resizeBitmapByScale(Bitmap src, float scale, boolean recycle) {
        int width = Math.round(src.getWidth() * scale);
        int height = Math.round(src.getHeight() * scale);
        if (width == src.getWidth() && height == src.getHeight()) {
            return src;
        }

        ColorMatrix cm = new ColorMatrix(new float[] {
                0.7f, 0,    0,    0,
                0.7f, 0,    0.7f, 0,
                0,    0.7f, 0,    0,
                0.7f, 0,    0.7f, 0,
                0,    0,    1,    0
        });
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(src));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        if (recycle) {
            src.recycle();
        }
        return target;
    }

    Bitmap getBlurBitmap(Context context, Bitmap src, int radius) {
        if (src == null || src.isRecycled()) {
            Log.d(TAG, "getBlurBitmap() invalid bitmap.");
            src = makeBackgroundBmp(null);
        }
        if (src == null || src.isRecycled()) {
            return null;
        }

        Bitmap bitmap = resizeBitmapByScale(src,
                128f / Math.max(src.getWidth(), src.getHeight()),
                false);
        try {
            RenderScript rs = RenderScript.create(context);
            Allocation input = Allocation.createFromBitmap(rs, bitmap,
                    MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
            Allocation output = Allocation.createTyped(rs, input.getType());
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            script.setRadius(radius);
            script.setInput(input);
            script.forEach(output);
            output.copyTo(bitmap);
            rs.destroy();
        } catch (Exception e) {
            Log.w(TAG, "getBlurBitmap", e);
        }

        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("getBlurBitmap() ")
                    .append("orig=(").append(src.getWidth()).append(",")
                    .append(src.getHeight()).append(") ")
                    .append("blur=(").append(bitmap.getWidth()).append(",")
                    .append(bitmap.getHeight()).append(")")
                    .toString());
        }
        return bitmap;
    }

    Bitmap getWallpaperBitmap() {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
        WallpaperInfo liveWallpaper = wallpaperManager.getWallpaperInfo();
        if (liveWallpaper != null) {
            return makeBackgroundBmp(liveWallpaper
                    .loadThumbnail(mContext.getPackageManager()));
        } else {
            return wallpaperManager.getBitmap();
        }
    }

    boolean isValidBGImage() {
        return mWallpaperImage != null && mWallpaperImage.getBitmap() != null;
    }

    BitmapDrawable getWallpaperImage() {
        if (DBG) {
            Log.d(TAG, "getBGImage() isValidBGImage()=" + isValidBGImage());
        }
        return mWallpaperImage;
    }

    void startWallpaperImageTask() {
        Log.d(TAG, "startWallpaperImageTask() mGetWallpaperImageTask="
                + mGetWallpaperImageTask);
        if (mGetWallpaperImageTask == null) {
            mWallpaperImage = null;
            mGetWallpaperImageTask = new GetWallpaperTask();
            mGetWallpaperImageTask.execute();
        }
    }

    void addWindow(View view, LayoutParams lp) {
        mWindowManager.addView(view, lp);
    }

    void removeWindow(View view) {
        try {
            mWindowManager.removeViewImmediate(view);
        } catch (Exception e) {
            Log.d(TAG, "Exception inside removeView() ");
        }
    }

    void trimMemory() {
        WindowManagerGlobal.getInstance().trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
    }

    void playShortHaptic() {
        if (DBG) {
            Log.d(TAG, "playShortHaptic() callers=" + Debug.getCallers(7));
        }
        mVibrator.vibrate(50);
    }

    void sendKeyEvent(int keyCode, int action, int flags) {
        long when = SystemClock.uptimeMillis();
        if (action == 0) {
            mDownTime = when;
        }
        InputManager.getInstance().injectInputEvent(new KeyEvent(
                        mDownTime, when, action, keyCode,
                        (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0,
                        0, -1, 0,
                        flags | KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                        InputDevice.SOURCE_KEYBOARD),
                0);
    }

    private PointerCoords[] getTempPointerCoordsWithMinSize(int size) {
        int oldSize = gSharedTempPointerCoords != null ? gSharedTempPointerCoords.length : 0;
        if (oldSize < size) {
            PointerCoords[] oldTempPointerCoords = gSharedTempPointerCoords;
            gSharedTempPointerCoords = new PointerCoords[size];
            if (oldTempPointerCoords != null) {
                System.arraycopy(oldTempPointerCoords, 0,
                        gSharedTempPointerCoords, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            gSharedTempPointerCoords[i] = new PointerCoords();
        }
        return gSharedTempPointerCoords;
    }

    boolean isCallRinging() {
        boolean isRinging = mTelephonyManager != null && mTelephonyManager.isRinging();
        if (DBG) {
            Log.d(TAG, "isCallRinging() " + isRinging);
        }
        return isRinging;
    }

    private PointerProperties[] getTempPointerPropertiesWithMinSize(int size) {
        int oldSize = gSharedTempPointerProperties != null ? gSharedTempPointerProperties.length : 0;
        if (oldSize < size) {
            PointerProperties[] oldTempPointerProperties = gSharedTempPointerProperties;
            gSharedTempPointerProperties = new PointerProperties[size];
            if (oldTempPointerProperties != null) {
                System.arraycopy(oldTempPointerProperties, 0,
                        gSharedTempPointerProperties, 0, oldSize);
            }
        }
        for (int i = oldSize; i < size; i++) {
            gSharedTempPointerProperties[i] = new PointerProperties();
        }
        return gSharedTempPointerProperties;
    }

    void byPassRawEvent(MotionEvent event) {
        final int pointerCount = event.getPointerCount();
        PointerCoords[] pointerCoords = getTempPointerCoordsWithMinSize(pointerCount);
        PointerProperties[] pointerProperties = getTempPointerPropertiesWithMinSize(pointerCount);
        for (int i = 0; i < pointerCount; i++) {
            event.getPointerCoords(i, pointerCoords[i]);
            pointerCoords[i].y += mWinInfo.getTouchableWindowOffset();

            event.getPointerProperties(i, pointerProperties[i]);
        }

        InputManager.getInstance().injectInputEvent(MotionEvent.obtain(
                event.getDownTime(), event.getEventTime(), event.getAction(),
                pointerCount, pointerProperties, pointerCoords,
                0, 0, 1, 1,
                event.getDeviceId(), 0, event.getSource(),
                event.getFlags() | OneHandConstants.AMOTION_EVENT_FLAG_PREDISPATCH),
                0);
    }
}
