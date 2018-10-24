package com.freeme.applock.settings;

import android.util.Log;

/**
 * Created by yangzhengguang on 18-5-21.
 */

public class LogUtil {
    private static final boolean isDeBug = false;

    public static void v(String tag, String msg){
        if(isDeBug){
            Log.v(tag,msg);
        }
    }

    public static void d(String tag, String msg){
        if(isDeBug){
            Log.d(tag,msg);
        }
    }

    public static void i(String tag, String msg){
        if(isDeBug){
            Log.i(tag,msg);
        }
    }

    public static void w(String tag, String msg){
        if(isDeBug){
            Log.w(tag,msg);
        }
    }

    public static void e(String tag, String msg){
        if(isDeBug){
            Log.e(tag,msg);
        }
    }
}
