package com.freeme.dialer.app;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.android.dialer.R;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.common.LogUtil;

public class FreemeTabBarController {

    private static final String KEY_IS_SLID_UP = "key_tab_bar_is_slid_up";
    private static final String KEY_IS_FADED_OUT = "key_tab_bar_is_faded_out";
    private static final String KEY_IS_EXPANDED = "key_tab_bar_is_expanded";

    private ActivityUi mActivityUi;

    private boolean mIsTabBarSlidUp;
    private boolean mStateChangedDuringFadingIn = false;
    private boolean mStateChangedDuringFadingOut = false;

    private final AnimUtils.AnimationCallback mFadeOutCallback =
            new AnimUtils.AnimationCallback() {
                @Override
                public void onAnimationEnd() {
                    if (!mStateChangedDuringFadingOut) {
                        slideTabBar(true /* slideUp */, false /* animate */);
                    } else {
                        LogUtil.i("mFadeOutCallback.end", "mStateChanged During FadingOut");
                    }
                }

                @Override
                public void onAnimationCancel() {
                    if (!mStateChangedDuringFadingOut) {
                        slideTabBar(true /* slideUp */, false /* animate */);
                    }
                }
            };

    private final AnimUtils.AnimationCallback mFadeInCallback =
            new AnimUtils.AnimationCallback() {
                @Override
                public void onAnimationEnd() {
                    if (!mStateChangedDuringFadingIn) {
                        slideTabBar(false /* slideUp */, false /* animate */);
                    } else {
                        LogUtil.i("mFadeInCallback.end", "mStateChanged During FadingIn");
                    }
                }

                @Override
                public void onAnimationCancel() {
                    if (!mStateChangedDuringFadingIn) {
                        slideTabBar(false /* slideUp */, false /* animate */);
                    }
                }
            };
    private ValueAnimator mAnimator;

    public FreemeTabBarController(ActivityUi activityUi, View tabView, View searchView) {
        mActivityUi = activityUi;

        initSearchViews(tabView, searchView);
    }

    /**
     * @return Whether or not the tab bar is currently showing (both slid down and visible)
     */
    public boolean isTabBarShowing() {
        return !mIsTabBarSlidUp && !mIsFadedOut;
    }

    /**
     * Called when search UI has been exited for some reason.
     */
    public void onSearchUiExited() {
        LogUtil.d(
                "onSearchUIExited",
                "isExpanded: %b, isFadedOut %b",
                mIsExpanded,
                mIsFadedOut);
        if (mIsExpanded) {
            collapse(true /* animate */);
        }
        if (mIsFadedOut) {
            fadeIn();
        }

        slideTabBar(false /* slideUp */, false /* animate */);
    }

    /**
     * Called to indicate that the user is trying to hide the dialpad. Should be called before any
     * state changes have actually occurred.
     */
    public void onDialpadDown() {
        LogUtil.d(
                "onDialpadDown",
                "isInSearchUi: %b, hasSearchQuery: %b, isFadedOut: %b, isExpanded: %b",
                mActivityUi.isInSearchUi(),
                mActivityUi.hasSearchQuery(),
                mIsFadedOut,
                mIsExpanded);
        if (mActivityUi.isInSearchUi()) {
            if (mActivityUi.hasSearchQuery()) {
                if (mIsFadedOut) {
                    setVisible(true);
                }
                if (!mIsExpanded) {
                    expand(false /* animate */, false /* requestFocus */);
                }
                slideTabBar(false /* slideUp */, true /* animate */);
            } else {
                mStateChangedDuringFadingIn = false;
                fadeIn(mFadeInCallback);
            }
        }
    }

    /**
     * Called to indicate that the user is trying to show the dialpad. Should be called before any
     * state changes have actually occurred.
     */
    public void onDialpadUp() {
        LogUtil.d("onDialpadUp", "isInSearchUi " + mActivityUi.isInSearchUi());
        if (mActivityUi.isInSearchUi()) {
            slideTabBar(true /* slideUp */, true /* animate */);
        } else {
            // From the lists fragment
            fadeOut(mFadeOutCallback);
            mStateChangedDuringFadingOut = false;
        }
    }

    public void slideTabBar(boolean slideUp, boolean animate) {
        LogUtil.d("slideTabBar", "up: %b, animate: %b", slideUp, animate);

        mStateChangedDuringFadingIn = true;
        mStateChangedDuringFadingOut = true;
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
            mAnimator.removeAllUpdateListeners();
        }
        if (animate) {
            mAnimator = slideUp ? ValueAnimator.ofFloat(0, 1) : ValueAnimator.ofFloat(1, 0);
            mAnimator.addUpdateListener(
                    animation -> {
                        final float value = (float) animation.getAnimatedValue();
                        setHideOffset((int) (mActivityUi.getTabBarHeight() * value));
                    });
            mAnimator.start();
        } else {
            setHideOffset(slideUp ? mActivityUi.getTabBarHeight() : 0);
        }
        mIsTabBarSlidUp = slideUp;
    }

    public void setAlpha(float alphaValue) {
        mSearchView.animate().alpha(alphaValue).start();
    }

    private void setHideOffset(int offset) {
//        mActivityUi.setTabBarHideOffset(offset);
    }

    /**
     * Saves the current state of the tab bar into a provided {@link Bundle}
     */
    public void saveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IS_SLID_UP, mIsTabBarSlidUp);
        outState.putBoolean(KEY_IS_FADED_OUT, mIsFadedOut);
        outState.putBoolean(KEY_IS_EXPANDED, mIsExpanded);
    }

    /**
     * Restores the tab bar state from a provided {@link Bundle}.
     */
    public void restoreInstanceState(Bundle inState) {
        mIsTabBarSlidUp = inState.getBoolean(KEY_IS_SLID_UP);

        final boolean isSearchBoxFadedOut = inState.getBoolean(KEY_IS_FADED_OUT);
        if (isSearchBoxFadedOut) {
            if (!mIsFadedOut) {
                setVisible(false);
            }
        } else if (mIsFadedOut) {
            setVisible(true);
        }

        final boolean isSearchBoxExpanded = inState.getBoolean(KEY_IS_EXPANDED);
        if (isSearchBoxExpanded) {
            if (!mIsExpanded) {
                expand(false, false);
            }
        } else if (mIsExpanded) {
            collapse(false);
        }
    }

    public interface ActivityUi {

        boolean isInSearchUi();

        boolean hasSearchQuery();

        int getTabBarHeight();
    }

    // @{ ========================= Search View ==============================
    private static final float EXPAND_MARGIN_FRACTION_START = 0.8f;
    private static final int ANIMATION_DURATION = 200;

    private View mTabView;
    private View mSearchView;
    private EditText mSearchEditorView;
    private View mBackButtonView;
    private View mClearButtonView;

    protected boolean mIsExpanded = false;
    protected boolean mIsFadedOut = false;

    private float mCollapsedElevation;

    private void initSearchViews(View tabView, View searchView) {
        mTabView = tabView;
        mSearchView = searchView;
        mSearchEditorView = mSearchView.findViewById(R.id.search_view);
        mBackButtonView = mSearchView.findViewById(R.id.search_back_button);
        mClearButtonView = mSearchView.findViewById(R.id.search_close_button);

        mSearchEditorView.setTextDirection(View.TEXT_DIRECTION_LTR);
        mSearchEditorView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

        mCollapsedElevation = mSearchView.getElevation();
    }

    private void expand(boolean animate, boolean requestFocus) {
        updateVisibility(true);

        if (animate) {
            AnimUtils.crossFadeViews(mSearchView, mTabView, ANIMATION_DURATION);
            mAnimator = ValueAnimator.ofFloat(EXPAND_MARGIN_FRACTION_START, 0f);
            prepareAnimator();
        } else {
            mSearchView.setVisibility(View.VISIBLE);
            mSearchView.setAlpha(1);
            mTabView.setVisibility(View.GONE);
        }

        // Set 9-patch background. This owns the padding, so we need to restore the original values.
        mSearchView.setBackgroundResource(R.drawable.search_shadow);
        mSearchView.setElevation(0);

        if (requestFocus) {
            mSearchEditorView.requestFocus();
        }
        mIsExpanded = true;
    }

    private void collapse(boolean animate) {
        updateVisibility(false);

        if (animate) {
            AnimUtils.crossFadeViews(mTabView, mSearchView, ANIMATION_DURATION);
            mAnimator = ValueAnimator.ofFloat(0f, 1f);
            prepareAnimator();
        } else {
            mTabView.setVisibility(View.VISIBLE);
            mTabView.setAlpha(1);
            mSearchView.setVisibility(View.GONE);
        }

        mIsExpanded = false;
        mSearchView.setElevation(mCollapsedElevation);
        mSearchView.setBackgroundResource(R.drawable.rounded_corner);
    }

    private void updateVisibility(boolean isExpand) {
        int visibility = isExpand ? View.VISIBLE : View.GONE;
        mBackButtonView.setVisibility(visibility);
        if (TextUtils.isEmpty(mSearchEditorView.getText())) {
            mClearButtonView.setVisibility(View.GONE);
        } else {
            mClearButtonView.setVisibility(visibility);
        }
    }

    private void prepareAnimator() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }

        mAnimator.setDuration(ANIMATION_DURATION);
        mAnimator.start();
    }

    private void setVisible(boolean visible) {
        if (visible) {
            setAlpha(1);
            mSearchView.setVisibility(View.VISIBLE);
            mIsFadedOut = false;
        } else {
            setAlpha(0);
            mSearchView.setVisibility(View.GONE);
            mIsFadedOut = true;
        }
    }

    private void fadeOut(AnimUtils.AnimationCallback callback) {
        AnimUtils.fadeOut(mSearchView, ANIMATION_DURATION, callback);
        mIsFadedOut = true;
    }

    private void fadeIn() {
        AnimUtils.fadeIn(mSearchView, ANIMATION_DURATION);
        mIsFadedOut = false;
    }

    private void fadeIn(AnimUtils.AnimationCallback callback) {
        AnimUtils.fadeIn(mSearchView, ANIMATION_DURATION, AnimUtils.NO_DELAY, callback);
        mIsFadedOut = false;
    }

    public void setSearchQuery(String query){
        mSearchEditorView.setText(query);
    }
    // @} ========================= Search View ==============================
}
