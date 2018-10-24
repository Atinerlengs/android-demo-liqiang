package com.freeme.recents.presentation.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.android.systemui.R;
import com.freeme.recents.presentation.view.component.DonutProgress;

public class RecentsAnimationHelper {

    private static final String TAG = "RecentsAnimationHelper";
    private static final int MEMORY_ANIMATION_DURATION = 300;
    private final Context mContext;

    public RecentsAnimationHelper(Context context) {
        mContext = context;
    }

    public void startMemoryInfoProgressAnimation(final int progress,
                                                 final DonutProgress donutProgress) {
        final ValueAnimator backAnimator = new ValueAnimator().ofInt(progress, 0);
        final ValueAnimator forthAnimator = new ValueAnimator().ofInt(0, progress);
        backAnimator.setDuration(MEMORY_ANIMATION_DURATION);
        forthAnimator.setDuration(MEMORY_ANIMATION_DURATION);
        backAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                donutProgress.setProgress((int) valueAnimator.getAnimatedValue());
            }
        });
        backAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                forthAnimator.start();
            }
        });
        forthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                donutProgress.setProgress((int) valueAnimator.getAnimatedValue());
            }
        });
        forthAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {

            }
        });
        backAnimator.start();
    }

}
