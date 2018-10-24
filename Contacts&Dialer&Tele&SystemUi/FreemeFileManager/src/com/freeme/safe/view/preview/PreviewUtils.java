package com.freeme.safe.view.preview;

import java.util.Formatter;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.freeme.filemanager.R;

class PreviewUtils {

    static final int TIME_OUT = 3000;

    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static Object[] sTimeArgs = new Object[5];

    static String makeTimeString(Context context, int duration) {
        final int MS_TO_S    = 1000;
        final int S_TO_HOUR  = 3600;
        final int TIME_CYCLE = 60;

        duration /= MS_TO_S;
        sTimeArgs[0] = duration / S_TO_HOUR;
        sTimeArgs[1] = duration / TIME_CYCLE;
        sTimeArgs[2] = (duration / TIME_CYCLE) % TIME_CYCLE;
        sTimeArgs[3] = duration;
        sTimeArgs[4] = duration % TIME_CYCLE;

        String durationformat = context.getString(
                (int)sTimeArgs[0] == 0 ? R.string.durationformatshort : R.string.durationformatlong);
        sFormatBuilder.setLength(0);

        return sFormatter.format(Locale.getDefault(), durationformat, sTimeArgs).toString();
    }

    static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 0;
    }

    static void setFullScreen(Activity activity) {
        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.BLACK);
    }
}
