package com.freeme.safe.password;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Interpolator;

class InputPasswordManager {

    private static final String PREFERENCES_FINGER = "fingerprint_preferences";

    private static final String TIMES_FINGER = "fingerprint_times";
    private static final String TIMES_INPUT = "input_fail_key";

    private static final String FAIL_KEY_FINGER_TIME = "finger_failed_times_key";
    private static final String FAIL_KEY_FIVE_TIME = "five_fail_time_key";
    private static final String FAIL_KEY_WAIT_TIME = "fail_wait_time_key";

    private static final String FAIL_KEY_FINGER_COUNT_TIME  = "finger_failed_count_time_key";
    private static final String FAIL_KEY_FINGER_COUNT_STATE = "finger_failed_count_state_key";

    private static final double PI = 3.141592653589793d;

    static long getRestCountTimeToInput(Context context) {
        long lastTime = getFiveTimesFailTime(context);
        if (lastTime < 1) {
            return 0;
        }
        long restTime = getRestCountTime(context);
        if (restTime < 1) {
            return 0;
        }
        long tempTime = restTime - (System.currentTimeMillis() - lastTime);
        if (tempTime >= restTime) {
            return 0;
        }
        if (tempTime >= restTime || tempTime <= 0) {
            return 0;
        }
        return tempTime;
    }

    static ValueAnimator failEditScaleAnimation(final View view) {
        Interpolator mScaleInterpolator = new Interpolator() {
            @Override
            public float getInterpolation(float input) {
                if (((double) input) <= 0.25d || ((double) input) >= 0.75d) {
                    return (float) Math.tan(((double) input) * PI);
                }
                return (float) (1.0d / Math.tan(((double) input) * PI));
            }
        };
        ObjectAnimator scalesAnim = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.3f);
        scalesAnim.setInterpolator(mScaleInterpolator);
        scalesAnim.setDuration(500);
        scalesAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setScaleX((Float) animation.getAnimatedValue());
            }
        });
        return scalesAnim;
    }

    static void setFingerPrintTimes(Context context, int times) {
        Editor edit = context.getSharedPreferences(PREFERENCES_FINGER, Context.MODE_PRIVATE).edit();
        edit.putInt(TIMES_FINGER, times);
        edit.apply();
    }

    static int getInputFailedTimes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(TIMES_INPUT, 0);
    }

    static void setInputFailedTimes(Context context, int times) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putInt(TIMES_INPUT, times);
        edit.apply();
    }

    private static long getFiveTimesFailTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(FAIL_KEY_FIVE_TIME, 0);
    }

    static void saveFiveTimesFailTime(Context context, long time) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(FAIL_KEY_FIVE_TIME, time);
        edit.apply();
    }

    private static long getRestCountTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(FAIL_KEY_WAIT_TIME, 0);
    }

    static void saveRestCountTime(Context context, long time) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(FAIL_KEY_WAIT_TIME, time);
        edit.apply();
    }

    static void saveFingerFailedCurrentTimes(Context context, long time) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(FAIL_KEY_FINGER_TIME, time);
        edit.apply();
    }

    static void saveFingerFailedCountTimes(Context context, long time) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(FAIL_KEY_FINGER_COUNT_TIME, time);
        edit.apply();
    }

    static void setFingerFailedCountState(Context context, boolean state) {
        Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putBoolean(FAIL_KEY_FINGER_COUNT_STATE, state);
        edit.apply();
    }

    static boolean getFingerFailedCountState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(FAIL_KEY_FINGER_COUNT_STATE, false);
    }
}
