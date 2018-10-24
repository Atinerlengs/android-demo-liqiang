package com.freeme.systemui.blur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class WindowBlur {

    public interface OnBlurObserver {
        Bitmap getBaseBitmap();

        void onBlurFinish(Bitmap bitmap);
    }

    private static final String TAG = "WindowBlur";

    private int mBlurRadius = 25;

    private int[] mLastFingerprint;
    private OnBlurObserver mOnBlurObserver;
    private BitmapThread mThread;

    private Context mContext;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (1 == msg.what) {
                mThread = null;
                Bitmap blurBitmap = (Bitmap) msg.obj;
                msg.obj = null;
                if (mOnBlurObserver != null) {
                    mOnBlurObserver.onBlurFinish(blurBitmap);
                }
            }
        }
    };

    private class BitmapThread extends Thread {
        private Bitmap mScreenBitmap;

        @Override
        public void run() {
            Bitmap screenShot = null;
            if (true) { //FIXME: PerfAdjust.supportBlurBackgound()
                try {
                    if (mOnBlurObserver != null) {
                        screenShot = mOnBlurObserver.getBaseBitmap();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } catch (Error err) {
                    Log.e(TAG, "startWindowBlur  Error er = " + err.getMessage());
                }
                if (screenShot == null) {
                    Log.e(TAG, "start screen shot fail,notify caller");
                    notifyBlurResult(null);
                    return;
                }
                mScreenBitmap = screenShot;
                if (isInterrupted()) {
                    mScreenBitmap.recycle();
                    return;
                }
                if (!(mScreenBitmap.isMutable() && mScreenBitmap.getConfig() == Config.ARGB_8888)) {
                    Bitmap tmp = mScreenBitmap.copy(Config.ARGB_8888, true);
                    mScreenBitmap.recycle();
                    mScreenBitmap = tmp;
                }
                if (compareFingerprint(mScreenBitmap)) {
                    mScreenBitmap.recycle();
                    return;
                }
                new BlurUtils().blurImage(mContext, mScreenBitmap, mScreenBitmap, mBlurRadius);
                Bitmap bitmap = null;
                if (isInterrupted()) {
                    if (mScreenBitmap != null) {
                        mScreenBitmap.recycle();
                    }
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    return;
                }
                if (true && mScreenBitmap != null) { //FIXME: PerfAdjust.supportScreenRotation()
                    Matrix matrix = new Matrix();
                    matrix.postScale(0.5f, 0.5f);
                    mScreenBitmap = Bitmap.createBitmap(mScreenBitmap, 0, 0,
                            mScreenBitmap.getWidth(), mScreenBitmap.getHeight(), matrix, true);
                }
                notifyBlurResult(mScreenBitmap);
                return;
            }
            Log.i(TAG, "not support blur background, notify null");
            notifyBlurResult(null);
        }
    }


    public WindowBlur(Context context) {
        mContext = context.getApplicationContext();
    }

    public void setOnBlurObserver(OnBlurObserver observer) {
        mOnBlurObserver = observer;
    }

    public void setBlurRadius(int blurRadius) {
        mBlurRadius = blurRadius;
    }

    public void start() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
        mThread = new BitmapThread();
        try {
            mThread.start();
        } catch (Exception e) {
            Log.e(TAG, "start::occur exception=" + e);
            notifyBlurResult(null);
        } catch (Error e2) {
            Log.e(TAG, "start::occur error=" + e2);
            notifyBlurResult(null);
        }
    }

    private boolean compareFingerprint(Bitmap bitmap) {
        if (bitmap == null) {
            Log.d(TAG, "bitmap = null");
            return false;
        }
        try {
            int dstWidth = bitmap.getWidth();
            int dstHeight = bitmap.getHeight();
            while (dstWidth * dstHeight >= 2304) {
                dstWidth >>= 1;
                dstHeight >>= 1;
            }
            int[] fingerPrint = new int[(dstWidth * dstHeight)];
            Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false)
                    .getPixels(fingerPrint, 0, dstWidth, 0, 0, dstWidth, dstHeight);
            if (mLastFingerprint == null || mLastFingerprint.length != fingerPrint.length) {
                mLastFingerprint = fingerPrint;
                Log.d(TAG, "fingerprint equals.");
                return false;
            }
            fallPixelColor(fingerPrint);
            int similitudeRate = 0;
            for (int i = 0; i < mLastFingerprint.length; i++) {
                if (mLastFingerprint[i] != fingerPrint[i]) {
                    similitudeRate++;
                }
            }
            Log.d(TAG, "similitudeRate = " + similitudeRate);
            if (similitudeRate < 10) {
                return true;
            }
            mLastFingerprint = fingerPrint;
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } catch (Error er) {
            er.printStackTrace();
            return false;
        }
    }

    private final void fallPixelColor(int[] fingerPrint) {
        for (int i = 0; i < fingerPrint.length; i++) {
            fingerPrint[i] = Color.rgb(
                    Color.red(fingerPrint[i]) >> 2,
                    Color.green(fingerPrint[i]) >> 2,
                    Color.blue(fingerPrint[i]) >> 2);
        }
    }

    private final void notifyBlurResult(Bitmap bitmap) {
        Log.d(TAG, "notifyBlurResult bitmap = " + bitmap);
        Message msg = Message.obtain();
        msg.obj = bitmap;
        msg.what = 1;
        mHandler.sendMessage(msg);
    }
}
