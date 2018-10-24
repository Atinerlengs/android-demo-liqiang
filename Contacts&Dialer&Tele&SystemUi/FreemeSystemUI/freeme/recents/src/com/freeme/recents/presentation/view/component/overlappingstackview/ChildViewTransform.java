package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;

public class ChildViewTransform {
    public int startDelay = 0;
    public int translationX = 0;
    public float translationZ = 0;
    public float scale = 1f;
    public float alpha = 1f;
    public boolean visible = false;
    public Rect rect = new Rect();
    public float p = 0f;

    public ChildViewTransform() {
        // Do nothing
    }

    public ChildViewTransform(ChildViewTransform o) {
        startDelay = o.startDelay;
        translationX = o.translationX;
        translationZ = o.translationZ;
        scale = o.scale;
        alpha = o.alpha;
        visible = o.visible;
        rect.set(o.rect);
        p = o.p;
    }

    /**
     * Resets the current transform
     */
    public void reset() {
        startDelay = 0;
        translationX = 0;
        translationZ = 0;
        scale = 1f;
        alpha = 1f;
        visible = false;
        rect.setEmpty();
        p = 0f;
    }

    /**
     * Convenience functions to compare against current property values
     */
    public boolean hasAlphaChangedFrom(float v) {
        return (Float.compare(alpha, v) != 0);
    }

    public boolean hasScaleChangedFrom(float v) {
        return (Float.compare(scale, v) != 0);
    }

    public boolean hasTranslationXChangedFrom(float v) {
        return (Float.compare(translationX, v) != 0);
    }

    public boolean hasTranslationZChangedFrom(float v) {
        return (Float.compare(translationZ, v) != 0);
    }

    /**
     * Applies this transform to a view.
     */
    public void applyToChildView(View v, int duration, Interpolator interp, boolean allowLayers,
                                ValueAnimator.AnimatorUpdateListener updateCallback) {
        // Check to see if any properties have changed, and update the Child view
        if (duration > 0) {
            ViewPropertyAnimator anim = v.animate();
            boolean requiresLayers = false;

            // Animate to the final state
            if (hasTranslationXChangedFrom(v.getTranslationX())) {
                anim.translationX(translationX);
            }
            if (hasTranslationZChangedFrom(v.getTranslationZ())) {
                anim.translationZ(translationZ);
            }
            if (hasScaleChangedFrom(v.getScaleX())) {
                anim.scaleX(scale)
                        .scaleY(scale);
                requiresLayers = true;
            }
            if (hasAlphaChangedFrom(v.getAlpha())) {
                // Use layers if we animate alpha
                anim.alpha(alpha);
                requiresLayers = true;
            }
            if (requiresLayers && allowLayers) {
                anim.withLayer();
            }
            if (updateCallback != null) {
                anim.setUpdateListener(updateCallback);
            } else {
                anim.setUpdateListener(null);
            }
            anim.setStartDelay(startDelay)
                    .setDuration(duration)
                    .setInterpolator(interp)
                    .start();
        } else {
            // Set the changed properties
            if (hasTranslationXChangedFrom(v.getTranslationX())) {
                v.setTranslationX(translationX);
            }
            if (hasTranslationZChangedFrom(v.getTranslationZ())) {
                v.setTranslationZ(translationZ);
            }
            if (hasScaleChangedFrom(v.getScaleX())) {
                v.setScaleX(scale);
                v.setScaleY(scale);
            }
            if (hasAlphaChangedFrom(v.getAlpha())) {
                v.setAlpha(alpha);
            }
        }
    }

    /**
     * Reset the transform on a view.
     */
    public static void reset(View v) {
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setTranslationZ(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setAlpha(1f);
    }

    @Override
    public String toString() {
        return "ChildViewTransform delay: " + startDelay + " y: " + translationX + " z: " + translationZ +
                " scale: " + scale + " alpha: " + alpha + " visible: " + visible + " rect: " + rect +
                " p: " + p;
    }
}
