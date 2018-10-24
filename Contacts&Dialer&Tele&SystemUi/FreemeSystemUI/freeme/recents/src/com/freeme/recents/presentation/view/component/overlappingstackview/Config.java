package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.android.systemui.R;

public class Config {
    static Config sInstance;
    static int sPrevConfigurationHashCode;

    /**
     * Animations
     */
    public float animationPxMovementPerSecond;

    /**
     * Interpolators
     */
    public Interpolator fastOutSlowInInterpolator;
    public Interpolator fastOutLinearInInterpolator;
    public Interpolator linearOutSlowInInterpolator;
    public Interpolator quintOutInterpolator;

    /**
     * Child stack
     */
    public int childStackScrollDuration;
    public int childStackMaxDim;
    public float childStackOverscrollPct;

    /**
     * Transitions
     */
    public int transitionEnterFromHomeDelay;

    /**
     * Child view animation and styles
     */
    public int childViewEnterFromAppDuration;
    public int childViewEnterFromHomeDuration;
    public int childViewEnterFromHomeStaggerDelay;
    public int childViewExitToAppDuration;
    public int childViewExitToHomeDuration;
    public int childViewRemoveAnimDuration;
    public int childViewRemoveAnimTranslationYPx;
    public int childViewTranslationZMinPx;
    public int childViewTranslationZMaxPx;
    public int childViewRoundedCornerRadiusPx;
    public float childViewThumbnailAlpha;

    /**
     * Launch states
     */
    public boolean launchedWithAltTab;
    public boolean launchedFromAppWithThumbnail;
    public boolean launchedFromHome = true;
    public boolean launchedHasConfigurationChanged;

    /**
     * Misc *
     */
    public boolean isScreenPinEnable;

    /**
     * Child *
     */
    public int childThumbnailWidth;
    public int childThumbnailHeight;
    public int childThumbnailMultiWindowModeWidth;
    public int childThumbnailMultiWindowModeHeigh;
    public int childHeaderHeight;
    public int childMarginBot;

    /**
     * Private constructor
     */
    private Config(Context context) {
        // Properties that don't have to be reloaded with each configuration change can be loaded
        // here.

        // Interpolators
        fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.fast_out_slow_in);
        fastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.fast_out_linear_in);
        linearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.linear_out_slow_in);
        quintOutInterpolator = AnimationUtils.loadInterpolator(context,
                R.anim.decelerate_quint);
    }

    /**
     * Updates the configuration to the current context
     */
    public static Config reinitialize(Context context) {
        if (sInstance == null) {
            sInstance = new Config(context);
        }
        int configHashCode = context.getResources().getConfiguration().hashCode();
        if (sPrevConfigurationHashCode != configHashCode) {
            sInstance.update(context);
            sPrevConfigurationHashCode = configHashCode;
        }
        return sInstance;
    }

    /**
     * Returns the current recents configuration
     */
    public static Config getInstance() {
        return sInstance;
    }

    /**
     * Updates the state, given the specified context
     */
    void update(Context context) {
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Animations
        animationPxMovementPerSecond = res.getDimensionPixelSize(R.dimen.animation_movement_in_dps_per_second);

        // Child stack
        childStackScrollDuration = res.getInteger(R.integer.animate_overlapping_scroll_duration);
        TypedValue widthPaddingPctValue = new TypedValue();
        res.getValue(R.dimen.overlapping_width_padding_percentage, widthPaddingPctValue, true);
        TypedValue stackOverscrollPctValue = new TypedValue();
        res.getValue(R.dimen.overlapping_overscroll_percentage, stackOverscrollPctValue, true);
        childStackOverscrollPct = stackOverscrollPctValue.getFloat();
        childStackMaxDim = res.getInteger(R.integer.max_overlapping_view_dim);

        // Child thumbnail
        childThumbnailWidth = res.getDimensionPixelSize(R.dimen.child_view_thumbnail_width);
        childThumbnailHeight = res.getDimensionPixelSize(R.dimen.child_view_thumbnail_height);
        childThumbnailMultiWindowModeWidth = res.getDimensionPixelSize(R.dimen.child_view_thumbnail_multiwindowmode_width);
        childThumbnailMultiWindowModeHeigh = res.getDimensionPixelSize(R.dimen.child_view_thumbnail_multiwindowmode_height);
        childHeaderHeight = res.getDimensionPixelSize(R.dimen.child_view_application_icon_size)
                + res.getDimensionPixelSize(R.dimen.child_view_application_icon_marginBottom);

        // Transition
        transitionEnterFromHomeDelay = res.getInteger(R.integer.enter_from_home_transition_duration);

        // Child view animation and styles
        childViewEnterFromAppDuration = res.getInteger(R.integer.child_enter_from_app_duration);
        childViewEnterFromHomeDuration = res.getInteger(R.integer.child_enter_from_home_duration);
        childViewEnterFromHomeStaggerDelay = res.getInteger(R.integer.child_enter_from_home_stagger_delay);
        childViewExitToAppDuration = res.getInteger(R.integer.child_exit_to_app_duration);
        childViewExitToHomeDuration = res.getInteger(R.integer.child_exit_to_home_duration);
        childViewRemoveAnimDuration = res.getInteger(R.integer.animate_child_view_remove_duration);
        childViewRemoveAnimTranslationYPx = res.getDimensionPixelSize(R.dimen.child_view_remove_anim_translation_y);
        childViewRoundedCornerRadiusPx = res.getDimensionPixelSize(R.dimen.child_view_rounded_corners_radius);
        childViewTranslationZMinPx = res.getDimensionPixelSize(R.dimen.child_view_z_min);
        childViewTranslationZMaxPx = res.getDimensionPixelSize(R.dimen.child_view_z_max);
        TypedValue thumbnailAlphaValue = new TypedValue();
        res.getValue(R.dimen.child_view_thumbnail_alpha, thumbnailAlphaValue, true);
        childViewThumbnailAlpha = thumbnailAlphaValue.getFloat();

        childMarginBot = res.getDimensionPixelSize(R.dimen.recents_memory_label_marginTop);
    }

    /**
     * Called when the configuration has changed, and we want to reset any configuration specific
     * members.
     */
    public void updateOnConfigurationChange() {
        // Reset this flag on configuration change to ensure that we recreate new child views
        // Set this flag to indicate that the configuration has changed since Recents last launched
        launchedHasConfigurationChanged = true;
    }
}
