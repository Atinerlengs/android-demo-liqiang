package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class BitmapUtils {

    public static void blur(Context context, Bitmap image, float blurRadius) {
        RenderScript renderScript = RenderScript.create(context);
        ScriptIntrinsicBlur blurScript =
                ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        Allocation allocationIn = Allocation.createFromBitmap(renderScript, image);
        Allocation allocationOut = Allocation.createFromBitmap(renderScript, image);
        blurScript.setInput(allocationIn);
        blurScript.forEach(allocationOut);
//        blurScript.setRadius(blurRadius);
        allocationOut.copyTo(image);
        blurScript.destroy();
        allocationIn.destroy();
        allocationOut.destroy();
    }
}
