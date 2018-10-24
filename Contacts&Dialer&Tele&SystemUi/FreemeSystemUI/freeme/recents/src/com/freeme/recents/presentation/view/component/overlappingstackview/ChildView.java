package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.ui.TaskSnapshotChangedEvent;

import com.freeme.recents.RecentsUtils;
import com.freeme.recents.presentation.view.component.overlappingstackview.utils.ReferenceCountedTrigger;
import com.freeme.recents.presentation.view.component.overlappingstackview.utils.Utils;
import com.freeme.recents.recentslib.data.model.Task;

public class ChildView<T> extends FrameLayout implements
        View.OnClickListener, View.OnLongClickListener {

    private View mPinIcon;
    private View mLockIcon;

    interface ChildViewCallbacks<T> {

        void onChildViewClicked(ChildView<T> cv, T key, boolean isPin);

        void onChildViewDismissed(ChildView<T> cv);

        void onChildViewFocusChanged(ChildView<T> cv, boolean focused);

        void onChildViewLockStatusChanged(ChildView<T> cv, T key);

        void onChildViewLongClicked(ChildView<T> cv, T key);

        //*/ freeme.gouzhouping, 20180327. Recents, blank view.
        void updateTaskFromTaskList(T key);
        //*/
    }

    Config mConfig;

    float mChildProgress;
    ObjectAnimator mChildProgressAnimator;
    float mMaxDimScale;
    int mDimAlpha;
    AccelerateInterpolator mDimInterpolator = new AccelerateInterpolator(1f);

    T mKey;
    boolean mChildDataLoaded;
    boolean mIsFocused;
    boolean mFocusAnimationsEnabled;

    //*/ freeme.gouzhouping, 20180327. Recents, blank view.
    boolean mIsInMultiWindowMode;
    //*/

    View mContent;
    ChildViewThumbnail mThumbnailView;
    private ImageView mIconView;
    private TextView mLabelView;
    ChildViewCallbacks<T> mCb;

    // Optimizations
    ValueAnimator.AnimatorUpdateListener mUpdateDimListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setChildProgress((Float) animation.getAnimatedValue());
                }
            };


    public ChildView(Context context) {
        this(context, null);
    }

    public ChildView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChildView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ChildView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mConfig = Config.getInstance();
        mMaxDimScale = mConfig.childStackMaxDim / 255f;
        setChildProgress(getChildProgress());
        setDim(getDim());
        //*/ freeme.gouzhouping, 20180327. Recents, blank view.
        EventBus.getDefault().register(this, RecentsUtils.EVENT_BUS_PRIORITY);
        //*/
    }

    /**
     * Set callback
     */
    void setCallbacks(ChildViewCallbacks cb) {
        mCb = cb;
    }

    /**
     * Resets this ChildView for reuse.
     */
    void reset() {
        resetViewProperties();
        setCallbacks(null);
    }

    /**
     * Gets the child
     */
    T getAttachedKey() {
        return mKey;
    }

    @Override
    protected void onFinishInflate() {
        // Bind the views
        mContent = findViewById(R.id.child_view_content);
        mThumbnailView = (ChildViewThumbnail) findViewById(R.id.child_view_thumbnail);
        mIconView = (ImageView) findViewById(R.id.child_view_icon);
        mLabelView = (TextView) findViewById(R.id.child_view_label);
        mPinIcon = findViewById(R.id.child_view_pin);
        mPinIcon.setVisibility(mConfig.isScreenPinEnable ? View.VISIBLE : View.INVISIBLE);
        mPinIcon.setOnClickListener(this);
        mLockIcon = findViewById(R.id.child_view_lock);
    }

    /**
     * Synchronizes this view's properties with the child's transform
     */
    void updateViewPropertiesToChildTransform(ChildViewTransform toTransform, int duration) {
        updateViewPropertiesToChildTransform(toTransform, duration, null);
    }

    void updateViewPropertiesToChildTransform(ChildViewTransform toTransform, int duration,
                                              ValueAnimator.AnimatorUpdateListener updateCallback) {
        // Apply the transform
        toTransform.applyToChildView(this, duration, mConfig.fastOutSlowInInterpolator, false, updateCallback);

        // Update the child progress
        Utils.cancelAnimationWithoutCallbacks(mChildProgressAnimator);
        if (duration <= 0) {
            setChildProgress(toTransform.p);
        } else {
            mChildProgressAnimator = ObjectAnimator.ofFloat(this, "childProgress", toTransform.p);
            mChildProgressAnimator.setDuration(duration);
            mChildProgressAnimator.addUpdateListener(mUpdateDimListener);
            mChildProgressAnimator.start();
        }
    }

    /**
     * Resets this view's properties
     */
    void resetViewProperties() {
        setDim(0);
        setLayerType(View.LAYER_TYPE_NONE, null);
        ChildViewTransform.reset(this);
    }

    /**
     * Prepares this child view for the enter-recents animations.  This is called earlier in the
     * first layout because the actual animation into recents may take a long time.
     */
    void prepareEnterRecentsAnimation(boolean isChildViewLaunchTargetChild,
                                      boolean occludesLaunchTarget, int offscreenX) {
        int initialDim = getDim();
        if (mConfig.launchedHasConfigurationChanged) {
            // Just load the views as-is
        } else if (mConfig.launchedFromAppWithThumbnail) {
            if (isChildViewLaunchTargetChild) {
                // Set the dim to 0 so we can animate it in
                initialDim = 0;
            } else if (occludesLaunchTarget) {
                // Move the child view off screen (below) so we can animate it in
                setTranslationX(offscreenX);
            }

        } else if (mConfig.launchedFromHome) {
            // Move the child view off screen (below) so we can animate it in
            setTranslationX(offscreenX);
            setTranslationZ(0);
            setScaleX(1f);
            setScaleY(1f);
        }
        // Apply the current dim
        setDim(initialDim);
        // Prepare the thumbnail view alpha
        mThumbnailView.prepareEnterRecentsAnimation(isChildViewLaunchTargetChild);
    }

    /**
     * Animates this child view as it enters recents
     */
    void startEnterRecentsAnimation(final ViewAnimation.ChildViewEnterContext ctx) {
        final ChildViewTransform transform = ctx.currentChildTransform;
        int startDelay = 0;
        if (mConfig.launchedFromHome) {
            // Animate the childs up
            int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex - 1);
            int delay = mConfig.transitionEnterFromHomeDelay +
                    frontIndex * mConfig.childViewEnterFromHomeStaggerDelay;

            setScaleX(transform.scale);
            setScaleY(transform.scale);
            animate().translationZ(transform.translationZ);
            animate()
                    .translationX(transform.translationX)
                    .setStartDelay(delay)
                    .setUpdateListener(ctx.updateListener)
                    .setInterpolator(mConfig.quintOutInterpolator)
                    .setDuration(mConfig.childViewEnterFromHomeDuration +
                            frontIndex * mConfig.childViewEnterFromHomeStaggerDelay)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            // Decrement the post animation trigger
                            ctx.postAnimationTrigger.decrement();
                        }
                    })
                    .start();
            ctx.postAnimationTrigger.increment();
            startDelay = delay;
        }

        // Enable the focus animations from this point onwards so that they aren't affected by the
        // window transitions
        postDelayed(new Runnable() {
            @Override
            public void run() {
                enableFocusAnimations();
            }
        }, startDelay);
    }

    /**
     * Animates this child view as it leaves recents by pressing home.
     */
    void startExitToHomeAnimation(ViewAnimation.ChildViewExitContext ctx) {
        animate()
                .translationY(ctx.offscreenTranslationY)
                .setStartDelay(0)
                .setUpdateListener(null)
                .setInterpolator(mConfig.fastOutLinearInInterpolator)
                .setDuration(mConfig.childViewExitToHomeDuration)
                .withEndAction(ctx.postAnimationTrigger.decrementAsRunnable())
                .start();
        ctx.postAnimationTrigger.increment();
    }

    /**
     * Animates this child view as it exits recents
     */
    void startLaunchChildAnimation(final Runnable postAnimRunnable, boolean isLaunchingChild,
                                  boolean occludesLaunchTarget, boolean lockToChild) {
        if (isLaunchingChild) {
            // Animate the thumbnail alpha back into full opacity for the window animation out
            mThumbnailView.startLaunchChildAnimation(postAnimRunnable);

            // Animate the dim
            if (mDimAlpha > 0) {
                ObjectAnimator anim = ObjectAnimator.ofInt(this, "dim", 0);
                anim.setDuration(mConfig.childViewExitToAppDuration);
                anim.setInterpolator(mConfig.fastOutLinearInInterpolator);
                anim.start();
            }
        } else {
            // If this is another view in the child grouping and is in front of the launch child,
            // animate it away first
            if (occludesLaunchTarget) {
                animate().alpha(0f)
                        .translationY(getTranslationY())
                        .setStartDelay(0)
                        .setUpdateListener(null)
                        .setInterpolator(mConfig.fastOutLinearInInterpolator)
                        .setDuration(mConfig.childViewExitToAppDuration)
                        .start();
            }
        }
    }

    /**
     * Animates the deletion of this child view
     */
    void startDeleteChildAnimation(final Runnable r) {

        animate().translationY(-mConfig.childViewRemoveAnimTranslationYPx)
                .alpha(0f)
                .setStartDelay(0)
                .setUpdateListener(null)
                .setInterpolator(mConfig.fastOutSlowInInterpolator)
                .setDuration(mConfig.childViewRemoveAnimDuration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        // We just throw this into a runnable because starting a view property
                        // animation using layers can cause inconsisten results if we try and
                        // update the layers while the animation is running.  In some cases,
                        // the runnabled passed in may start an animation which also uses layers
                        // so we defer all this by posting this.
                        r.run();
                    }
                })
                .start();
    }

    /**
     * Dismisses this child.
     */
    void dismissChild(final ReferenceCountedTrigger trigger) {
        // Animate out the view and call the callback
        final ChildView<T> cv = this;
        startDeleteChildAnimation(new Runnable() {
            @Override
            public void run() {
                if (mCb != null) {
                    trigger.decrement();
                }
            }
        });
    }

    /**
     * Sets the current child progress.
     */
    public void setChildProgress(float p) {
        mChildProgress = p;
        updateDimFromChildProgress();
    }

    /**
     * Returns the current child progress.
     */
    public float getChildProgress() {
        return mChildProgress;
    }

    /**
     * Returns the current dim.
     */
    public void setDim(int dim) {
        mDimAlpha = dim;
        // Defer setting hardware layers if we have not yet measured, or there is no dim to draw
        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            float dimAlpha = 1 - mDimAlpha / 255.0f;
            float exponent = (dimAlpha - 0.7f > 0 ? dimAlpha - 0.7f : 0) / 0.05f;
            mLabelView.setAlpha(dimAlpha * 0.1f * (float) (Math.pow(2, exponent)));
            //mIconView.setAlpha(dimAlpha * 0.5f * (float) (Math.pow(2, exponent)));
            if (dimAlpha < 0.5f) {
                float d = dimAlpha + 0.5f;
                mThumbnailView.setAlpha(d);
            } else {
                mThumbnailView.setAlpha(1.0f);
            }
        }
    }

    /**
     * Returns the current dim.
     */
    public int getDim() {
        return mDimAlpha;
    }

    /**
     * Compute the dim as a function of the scale of this view.
     */
    int getDimFromChildProgress() {
        float dim = mMaxDimScale * mDimInterpolator.getInterpolation(1f - mChildProgress);
        return (int) (dim * 255);
    }

    /**
     * Update the dim as a function of the scale of this view.
     */
    void updateDimFromChildProgress() {
        setDim(getDimFromChildProgress());
    }

    /**** View focus state ****/

    /**
     * Sets the focused child explicitly. We need a separate flag because requestFocus() won't happen
     * if the view is not currently visible, or we are in touch state (where we still want to keep
     * track of focus).
     */
    public void setFocusedChild(boolean animateFocusedState) {
        mIsFocused = true;
        // Update the thumbnail alpha with the focus
        mThumbnailView.onFocusChanged(true);
        // Call the callback
        if (mCb != null) {
            mCb.onChildViewFocusChanged(this, true);
        }
        // Workaround, we don't always want it focusable in touch mode, but we want the first child
        // to be focused after the enter-recents animation, which can be triggered from either touch
        // or keyboard
        setFocusableInTouchMode(true);
        requestFocus();
        setFocusableInTouchMode(false);
        invalidate();
    }

    /**
     * Unsets the focused child explicitly.
     */
    void unsetFocusedChild() {
        mIsFocused = false;

        // Update the thumbnail alpha with the focus
        mThumbnailView.onFocusChanged(false);
        // Call the callback
        if (mCb != null) {
            mCb.onChildViewFocusChanged(this, false);
        }
        invalidate();
    }

    /**
     * Updates the explicitly focused state when the view focus changes.
     */
    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            unsetFocusedChild();
        }
    }

    /**
     * Returns whether we have explicitly been focused.
     */
    public boolean isFocusedChild() {
        return mIsFocused || isFocused();
    }

    /**
     * Enables all focus animations.
     */
    void enableFocusAnimations() {
        boolean wasFocusAnimationsEnabled = mFocusAnimationsEnabled;
        mFocusAnimationsEnabled = true;
    }

    /**** ChildCallbacks Implementation ****/

    /**
     * Binds this child view to the child
     */
    public void onChildBound(T key) {
        mKey = key;
    }

    private boolean isBound() {
        return mKey != null;
    }

    /**
     * Binds this child view to the child
     */
    public void onChildUnbound() {
        mKey = null;
    }

    public Bitmap getThumbnail() {
        if (mThumbnailView != null) {
            return mThumbnailView.getThumbnail();
        }

        return null;
    }

    public void onDataLoaded(T key, Bitmap thumbnail, Drawable headerIcon,
                             CharSequence headerTitle, boolean isLocked, boolean ismultiwindowmode) {
        /*/ freeme.gouzhouping, 20180327. Recents, blank view.
        if (!isBound() || !mKey.equals(key)) return;
        /*/
        if (!isBound() || !mKey.equals(key)) {
            return;
        }
        //*/

        if (mThumbnailView != null) {
            // Bind each of the views to the new child data
            mThumbnailView.rebindToChild(thumbnail, ismultiwindowmode, ((Task)mKey).getPackageName());
        }
        //*/ freeme.gouzhouping, 20180327. Recents, blank view.
        mIsInMultiWindowMode = ismultiwindowmode;
        //*/
        mIconView.setImageDrawable(headerIcon);
        mLabelView.setText(headerTitle);
        mLockIcon.setVisibility(isLocked ? View.VISIBLE : View.INVISIBLE);
        mChildDataLoaded = true;
    }

    public void onDataUnloaded() {
        if (mThumbnailView != null) {
            // Unbind each of the views from the child data and remove the child callback
            mThumbnailView.unbindFromChild();
        }
        mChildDataLoaded = false;
        //*/ freeme.gouzhouping, 20180327. Recents, blank view.
        EventBus.getDefault().unregister(this);
        //*/
    }

    public void toggleLockStatus() {
        mCb.onChildViewLockStatusChanged(this, this.getAttachedKey());
    }

    /**
     * Enables/disables handling touch on this child view.
     */
    public void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    /**
     * * View.OnClickListener Implementation ***
     */

    @Override
    public void onClick(final View v) {
        final ChildView<T> cv = this;
        if (mCb != null) {
            if (v == mPinIcon) {
                mCb.onChildViewClicked(cv, cv.getAttachedKey(), true);
            } else {
                mCb.onChildViewClicked(cv, cv.getAttachedKey(), false);
            }
        }
    }

    /**
     * * View.OnLongClickListener Implementation ***
     */

    @Override
    public boolean onLongClick(View v) {
        final ChildView<T> cv = this;
        mCb.onChildViewLongClicked(cv, cv.getAttachedKey());
        return false;
    }

    //*/ freeme.gouzhouping, 20180327. Recents, blank view.
    public final void onBusEvent(TaskSnapshotChangedEvent event) {
        if (mKey == null
                || event.taskId != ((Task) mKey).taskId
                || event.thumbnailData == null
                || event.thumbnailData.thumbnail == null) {
            return;
        }
        ((Task) mKey).setThumbnail(new BitmapDrawable(event.thumbnailData.thumbnail));
        if (mCb != null) {
            mCb.updateTaskFromTaskList(mKey);
        }
        mThumbnailView.rebindToChild(event.thumbnailData.thumbnail, mIsInMultiWindowMode, ((Task)mKey).getPackageName());
    }
    //*/
}
