package application.android.com.zhaozehong.floating;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

import application.android.com.zhaozehong.demoapplication.R;

public class FreemeInCallFloatingService extends Service {

    private ImageView img;
    private WindowManager wm;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = getResources().getDisplayMetrics();

        WindowManager.LayoutParams mWmParams = new WindowManager.LayoutParams();
        mWmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        mWmParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWmParams.width = 1;
        mWmParams.height = 1;
        mWmParams.x = (dm.widthPixels - mWmParams.width) / 2;
        mWmParams.y = (dm.heightPixels - mWmParams.height) / 2;
        mWmParams.format = PixelFormat.RGBA_8888;

        img = new ImageView(this);
        img.setImageResource(R.drawable.ic_launcher_foreground);

        wm.addView(img, mWmParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wm.removeView(img);
    }

    public static void start(Context context) {
        if (context != null) {
            context.startService(new Intent(context, FreemeInCallFloatingService.class));
        }
    }

    public static void stop(Context context) {
        if (context != null) {
            context.stopService(new Intent(context, FreemeInCallFloatingService.class));
        }
    }
}

