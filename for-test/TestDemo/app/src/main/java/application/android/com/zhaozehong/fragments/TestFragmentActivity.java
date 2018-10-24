package application.android.com.zhaozehong.fragments;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import application.android.com.zhaozehong.demoapplication.R;
import application.android.com.zhaozehong.utils.BitmapUtils;

public class TestFragmentActivity extends FragmentActivity {

    private static final String TAG_IN_CALL_SCREEN = "tag_in_call_screen";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.incall_fragment);

        getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        WallpaperManager wm = WallpaperManager.getInstance(this);
        Drawable drawable = wm.getDrawable();
        if (drawable != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            Bitmap bitmap = drawableToBitmap(drawable);
            int width = bitmap.getWidth();
            int x = 0;
            int w = width;
            if (width > screenWidth) {
                w = screenWidth;
            }
            bitmap = Bitmap.createBitmap(bitmap, x, 0, w, bitmap.getHeight());
            BitmapUtils.blur(this, bitmap, 0);
            Drawable[] layers = new Drawable[2];
            layers[0] = new BitmapDrawable(bitmap);
            layers[1] = new ColorDrawable(0x99000000);
            LayerDrawable layerDrawable = new LayerDrawable(layers);
            getWindow().setBackgroundDrawable(layerDrawable);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.main, new InCallFragment(), TAG_IN_CALL_SCREEN);
        transaction.commitNow();
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
