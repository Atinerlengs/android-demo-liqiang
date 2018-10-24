package com.freeme.systemui.blur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.renderscript.Allocation;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RSInvalidStateException;
import android.renderscript.RenderScript;
import android.renderscript.RenderScript.RSMessageHandler;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

public class BlurUtils {

    private static final String TAG = "BlurUtils";

    private RenderScript mRs;

    private RSMessageHandler mRsMsgHandler = new RSMessageHandler();

    private ScriptIntrinsicBlur mScriptIntrinsic;

    public Bitmap blurImage(Context ctx, Bitmap input, Bitmap output, int radius) {
        if (ctx == null || input == null || output == null || radius <= 0 || radius > 25) {
            Log.w(TAG, "blurImage() parameter is incorrect:" + ctx + "," + input + "," + output + "," + radius);
            return null;
        }
        Context c = ctx.getApplicationContext();
        if (c == null) {
            return null;
        }
        ctx = c;
        if (mRs == null) {
            Log.e(TAG, "mRs == null and need to create!!");
            mRs = RenderScript.create(c);
            if (mRs != null) {
                mRs.setMessageHandler(mRsMsgHandler);
            } else {
                Log.e(TAG, "mRs == null still!!");
                return null;
            }
        }
        Allocation tmpIn = Allocation.createFromBitmap(mRs, input, MipmapControl.MIPMAP_NONE, 1);
        Allocation tmpOut = Allocation.createTyped(mRs, tmpIn.getType());
        if (mScriptIntrinsic == null) {
            mScriptIntrinsic = ScriptIntrinsicBlur.create(mRs, Element.U8_4(mRs));
        }
        mScriptIntrinsic.setRadius((float) radius);
        mScriptIntrinsic.setInput(tmpIn);
        mScriptIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(output);
        try {
            tmpIn.destroy();
        } catch (RSInvalidStateException e) {
            e.printStackTrace();
        }
        try {
            tmpOut.destroy();
        } catch (RSInvalidStateException e2) {
            e2.printStackTrace();
        }
        destory();
        return output;
    }

    public void destory() {
        if (mRs != null) {
            mRs.destroy();
            mRs = null;
        }
        try {
            if (mScriptIntrinsic != null) {
                mScriptIntrinsic.destroy();
            }
        } catch (RSInvalidStateException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap resizeImage(Context mContext, Bitmap bitmap) {
        if (mContext == null || bitmap == null) {
            return null;
        }
        int screenW = mContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mContext.getResources().getDisplayMetrics().heightPixels;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale = (float) Math.max(screenH / height, screenW / width);
        matrix.postScale(scale, scale);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        Log.i(TAG, "width:" + width + ",height:" + height + ",screenW:" + screenW + ",screenH:" + screenH + ",scale:" + scale);
        return resizedBitmap;
    }
}
