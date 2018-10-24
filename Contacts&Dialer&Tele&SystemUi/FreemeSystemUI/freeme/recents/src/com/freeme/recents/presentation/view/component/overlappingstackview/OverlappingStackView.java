package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.freeme.recents.presentation.event.FreemeEventBus;
import com.freeme.recents.presentation.event.activity.LaunchTaskEvent;
import com.freeme.recents.presentation.event.activity.LaunchFrontTaskEvent;
import com.freeme.recents.presentation.event.activity.ToggleRecentsEvent;
import com.freeme.recents.presentation.view.component.overlappingstackview.ChildView;
import com.freeme.recents.presentation.view.component.overlappingstackview.Config;
import com.freeme.recents.presentation.view.component.overlappingstackview.ChildViewTransform;
import com.freeme.recents.presentation.view.component.overlappingstackview.LayoutAlgorithm;
import com.freeme.recents.presentation.view.component.overlappingstackview.StackViewScroller;
import com.freeme.recents.presentation.view.component.overlappingstackview.utils.ReferenceCountedTrigger;
import com.freeme.recents.presentation.view.component.overlappingstackview.ViewTouchHandler;
import com.freeme.recents.presentation.view.component.overlappingstackview.ViewAnimation;
import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.RecentsUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class OverlappingStackView<T> extends FrameLayout implements
        ChildView.ChildViewCallbacks<T>,
        StackViewScroller.StackViewScrollerCallbacks,
        ViewPool.ViewPoolConsumer<ChildView<T>, T> {

    Config mConfig;
    LayoutAlgorithm<T> mLayoutAlgorithm;
    StackViewScroller mStackStackViewScroller;
    ViewTouchHandler mTouchHandler;
    ViewPool<ChildView<T>, T> mViewPool;
    ArrayList<ChildViewTransform> mCurrentChildTransforms = new ArrayList<ChildViewTransform>();
    Rect mStackBounds = new Rect();
    int mFocusedChildIndex = -1;
    int mPrevAccessibilityFocusedIndex = -1;

    // Optimizations
    int mStackViewsAnimationDuration;
    boolean mStackViewsDirty = true;
    boolean mAwaitingFirstLayout = true;
    boolean mStartEnterAnimationRequestedAfterLayout = true;
    boolean mStartEnterAnimationCompleted;
    ViewAnimation.ChildViewEnterContext mStartEnterAnimationContext;
    int[] mTmpVisibleRange = new int[2];
    Rect mTmpRect = new Rect();
    ChildViewTransform mTmpTransform = new ChildViewTransform ();
    HashMap<T, ChildView> mTmpChildViewMap = new HashMap<T, ChildView>();
    LayoutInflater mInflater;

    public OverlappingStackView(Context context) {
        this(context, null);
    }

    public OverlappingStackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlappingStackView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public OverlappingStackView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Config.reinitialize(getContext());
        mConfig = Config.getInstance();
    }

    @Override
    protected void onAttachedToWindow() {
        FreemeEventBus.getDefault().register(this, RecentsUtils.EVENT_BUS_PRIORITY);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FreemeEventBus.getDefault().unregister(this);
    }

    public void initialize(Callback<T> callback, boolean screenPinEnable) {
        mCallback = callback;
        requestLayout();

        mViewPool = new ViewPool<ChildView<T>, T>(getContext(), this);
        mInflater = LayoutInflater.from(getContext());
        if (getContext() instanceof Activity) {
            mLayoutAlgorithm = new LayoutAlgorithm<T> (mConfig, ((Activity) getContext()).isInMultiWindowMode());
        } else {
            mLayoutAlgorithm = new LayoutAlgorithm<T> (mConfig, false);
        }
        mStackStackViewScroller = new StackViewScroller (getContext(), mConfig, mLayoutAlgorithm);
        mStackStackViewScroller.setCallbacks(this);
        mTouchHandler = new ViewTouchHandler (getContext(), this, mConfig, mStackStackViewScroller);
        mConfig.isScreenPinEnable = screenPinEnable;
    }

    /**
     * Resets this StackView for reuse.
     */
    void reset() {
        // Reset the focused child
        resetFocusedChild();

        // Return all the views to the pool
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            ChildView<T> cv = (ChildView) getChildAt(i);
            mViewPool.returnViewToPool(cv);
        }

        // Mark each child view for relayout
        if (mViewPool != null) {
            Iterator<ChildView<T>> iter = mViewPool.poolViewIterator();
            if (iter != null) {
                while (iter.hasNext()) {
                    ChildView cv = iter.next();
                    cv.reset();
                }
            }
        }

        // Reset the stack state
        mStackViewsDirty = true;
        mAwaitingFirstLayout = true;
        mPrevAccessibilityFocusedIndex = -1;
        mStackStackViewScroller.reset();
    }

    /**
     * Requests that the views be synchronized with the model
     */
    void requestSynchronizeStackViewsWithModel() {
        requestSynchronizeStackViewsWithModel(0);
    }

    void requestSynchronizeStackViewsWithModel(int duration) {
        if (!mStackViewsDirty) {
            invalidate();
            mStackViewsDirty = true;
        }
        if (mAwaitingFirstLayout) {
            // Skip the animation if we are awaiting first layout
            mStackViewsAnimationDuration = 0;
        } else {
            mStackViewsAnimationDuration = Math.max(mStackViewsAnimationDuration, duration);
        }
    }

    /**
     * Finds the child view given a specific child.
     */
    public ChildView getChildViewForChild(T key) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ChildView cv = (ChildView) getChildAt(i);
            if (cv.getAttachedKey().equals(key)) {
                return cv;
            }
        }
        return null;
    }

    /**
     * Returns the stack algorithm for this child stack.
     */
    public LayoutAlgorithm getStackAlgorithm() {
        return mLayoutAlgorithm;
    }

    /**
     * Gets the stack transforms of a list of childs, and returns the visible range of childs.
     */
    private boolean updateStackTransforms(ArrayList<ChildViewTransform> childTransforms,
                                          ArrayList<T> data,
                                          float stackScroll,
                                          int[] visibleRangeOut,
                                          boolean boundTranslationsToRect) {
        int childTransformCount = childTransforms.size();
        int childCount = data.size();
        int frontMostVisibleIndex = -1;
        int backMostVisibleIndex = -1;

        // We can reuse the child transforms where possible to reduce object allocation
        if (childTransformCount < childCount) {
            // If there are less transforms than childs, then add as many transforms as necessary
            for (int i = childTransformCount; i < childCount; i++) {
                childTransforms.add(new ChildViewTransform ());
            }
        } else if (childTransformCount > childCount) {
            // If there are more transforms than childs, then just subset the transform list
            childTransforms.subList(0, childCount);
        }

        // Update the stack transforms
        ChildViewTransform prevTransform = null;
        for (int i = childCount - 1; i >= 0; i--) {
            ChildViewTransform transform = mLayoutAlgorithm.getStackTransform(data.get(i), stackScroll, childTransforms.get(i), prevTransform);
            if (transform.visible) {
                if (frontMostVisibleIndex < 0) {
                    frontMostVisibleIndex = i;
                }
                backMostVisibleIndex = i;
            } else {
                if (backMostVisibleIndex != -1) {
                    // We've reached the end of the visible range, so going down the rest of the
                    // stack, we can just reset the transforms accordingly
                    while (i >= 0) {
                        childTransforms.get(i).reset();
                        i--;
                    }
                    break;
                }
            }

            if (boundTranslationsToRect) {
                transform.translationX = Math.min(transform.translationX,
                        mLayoutAlgorithm.mViewRect.bottom);
            }
            prevTransform = transform;
        }
        if (visibleRangeOut != null) {
            visibleRangeOut[0] = frontMostVisibleIndex;
            visibleRangeOut[1] = backMostVisibleIndex;
        }
        return frontMostVisibleIndex != -1 && backMostVisibleIndex != -1;
    }

    /**
     * Synchronizes the views with the model
     */
    boolean synchronizeStackViewsWithModel() {
        if (mStackViewsDirty) {
            // Get all the child transforms
            ArrayList<T> data = mCallback.getData();
            float stackScroll = mStackStackViewScroller.getStackScroll();
            int[] visibleRange = mTmpVisibleRange;
            boolean isValidVisibleRange = updateStackTransforms(mCurrentChildTransforms,
                    data, stackScroll, visibleRange, false);

            // Return all the invisible children to the pool
            mTmpChildViewMap.clear();
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                ChildView<T> cv = (ChildView) getChildAt(i);
                T key = cv.getAttachedKey();
                int childIndex = data.indexOf(key);

                if (visibleRange[1] <= childIndex
                        && childIndex <= visibleRange[0]) {
                    mTmpChildViewMap.put(key, cv);
                } else {
                    mViewPool.returnViewToPool(cv);
                }
            }

            for (int i = visibleRange[0]; isValidVisibleRange && i >= visibleRange[1]; i--) {
                T key = data.get(i);
                ChildViewTransform transform = mCurrentChildTransforms.get(i);
                ChildView cv = mTmpChildViewMap.get(key);

                if (cv == null) {
                    // TODO Check
                    cv = mViewPool.pickUpViewFromPool(key, key);

                    if (mStackViewsAnimationDuration > 0) {
                        // For items in the list, put them in start animating them from the
                        // approriate ends of the list where they are expected to appear
                        if (Float.compare(transform.p, 0f) <= 0) {
                            mLayoutAlgorithm.getStackTransform(0f, 0f, mTmpTransform, null);
                        } else {
                            mLayoutAlgorithm.getStackTransform(1f, 0f, mTmpTransform, null);
                        }
                        cv.updateViewPropertiesToChildTransform(mTmpTransform, 0);
                    }
                }

                // Animate the child into place
                cv.updateViewPropertiesToChildTransform(mCurrentChildTransforms.get(i),
                        mStackViewsAnimationDuration, null);
            }

            prefetch(visibleRange[0] + 1, visibleRange[1] - 1);

            // Reset the request-synchronize params
            mStackViewsAnimationDuration = 0;
            mStackViewsDirty = false;
            return true;
        }
        return false;
    }

    void prefetch(int front, int back) {
        int frontPrefetchIndex = front;
        int backPrefetchIndex = back;
        ArrayList<T> data = mCallback.getData();
        int size = data.size();
        if (frontPrefetchIndex < size) {
            T key = data.get(frontPrefetchIndex);
            mCallback.loadViewData(null, key);
        }

        if (backPrefetchIndex >= 0) {
            T key = data.get(backPrefetchIndex);
            mCallback.loadViewData(null, key);
        }
    }

    /**
     * Updates the min and max virtual scroll bounds
     */
    void updateMinMaxScroll(boolean boundScrollToNewMinMax, boolean launchedWithAltTab,
                            boolean launchedFromHome) {
        // Compute the min and max scroll values
        mLayoutAlgorithm.computeMinMaxScroll(mCallback.getData(), launchedWithAltTab, launchedFromHome);

        // Debug logging
        if (boundScrollToNewMinMax) {
            mStackStackViewScroller.boundScroll();
        }
    }

    /**
     * Returns the scroller.
     */
    public StackViewScroller getScroller() {
        return mStackStackViewScroller;
    }

    /**
     * Focuses the child at the specified index in the stack
     */
    void focusChild(int childIndex, boolean scrollToNewPosition, final boolean animateFocusedState) {
        // Return early if the child is already focused
        if (childIndex == mFocusedChildIndex) return;

        ArrayList<T> data = mCallback.getData();

        if (0 <= childIndex && childIndex < data.size()) {
            mFocusedChildIndex = childIndex;

            // Focus the view if possible, otherwise, focus the view after we scroll into position
            T key = data.get(childIndex);
            ChildView cv = getChildViewForChild(key);
            Runnable postScrollRunnable = null;
            if (cv != null) {
                cv.setFocusedChild(animateFocusedState);
            } else {
                postScrollRunnable = new Runnable() {
                    @Override
                    public void run() {

                        ChildView cv = getChildViewForChild(mCallback.getData().get(mFocusedChildIndex));
                        if (cv != null) {
                            cv.setFocusedChild(animateFocusedState);
                        }
                    }
                };
            }

            // Scroll the view into position (just center it in the curve)
            if (scrollToNewPosition) {
                float newScroll = mLayoutAlgorithm.getStackScrollForChild(key) - 0.5f;
                newScroll = mStackStackViewScroller.getBoundedStackScroll(newScroll);
                mStackStackViewScroller.animateScroll(mStackStackViewScroller.getStackScroll(), newScroll, postScrollRunnable);
            } else {
                if (postScrollRunnable != null) {
                    postScrollRunnable.run();
                }
            }

        }
    }

    /**
     * Ensures that there is a child focused, if nothing is focused, then we will use the child
     * at the center of the visible stack.
     */
    public boolean ensureFocusedChild() {
        if (mFocusedChildIndex < 0) {
            // If there is no child focused, then find the child that is closes to the center
            // of the screen and use that as the currently focused child
            int x = mLayoutAlgorithm.mStackVisibleRect.centerX();
            int y = mLayoutAlgorithm.mStackVisibleRect.centerY();
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                ChildView cv = (ChildView) getChildAt(i);
                cv.getHitRect(mTmpRect);
                if (mTmpRect.contains(x, y)) {
                    mFocusedChildIndex = i;
                    break;
                }
            }
            // If we can't find the center child, then use the front most index
            if (mFocusedChildIndex < 0 && childCount > 0) {
                mFocusedChildIndex = childCount - 1;
            }
        }
        return mFocusedChildIndex >= 0;
    }

    /**
     * Focuses the next child in the stack.
     *
     * @param animateFocusedState determines whether to actually draw the highlight along with
     *                            the change in focus, as well as whether to scroll to fit the
     *                            child into view.
     */
    public void focusNextChild(boolean forward, boolean animateFocusedState) {
        // Find the next index to focus
        int numChilds = mCallback.getData().size();
        if (numChilds == 0) return;

        int direction = (forward ? -1 : 1);
        int newIndex = mFocusedChildIndex + direction;
        if (newIndex >= 0 && newIndex <= (numChilds - 1)) {
            newIndex = Math.max(0, Math.min(numChilds - 1, newIndex));
            focusChild(newIndex, true, animateFocusedState);
        }
    }

    /**
     * Resets the focused child.
     */
    void resetFocusedChild() {
        if ((0 <= mFocusedChildIndex) && (mFocusedChildIndex < mCallback.getData().size())) {
            ChildView cv = getChildViewForChild(mCallback.getData().get(mFocusedChildIndex));
            if (cv != null) {
                cv.unsetFocusedChild();
            }
        }
        mFocusedChildIndex = -1;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        int childCount = getChildCount();
        if (childCount > 0) {
            ChildView<T> backMostChild = (ChildView) getChildAt(0);
            ChildView<T> frontMostChild = (ChildView) getChildAt(childCount - 1);
            event.setFromIndex(mCallback.getData().indexOf(backMostChild.getAttachedKey()));
            event.setToIndex(mCallback.getData().indexOf(frontMostChild.getAttachedKey()));
        }
        event.setItemCount(mCallback.getData().size());
        event.setScrollY(mStackStackViewScroller.mScroller.getCurrY());
        event.setMaxScrollY(mStackStackViewScroller.progressToScrollRange(mLayoutAlgorithm.mMaxScrollP));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchHandler.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mTouchHandler.onTouchEvent(ev);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        return mTouchHandler.onGenericMotionEvent(ev);
    }

    @Override
    public void computeScroll() {
        mStackStackViewScroller.computeScroll();
        // Synchronize the views
        synchronizeStackViewsWithModel();
        // Notify accessibility
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SCROLLED);
    }

    /**
     * Computes the stack and child rects
     */
    public void computeRects(int windowWidth, int windowHeight, Rect childStackBounds,
                             boolean launchedWithAltTab, boolean launchedFromHome) {
        // Compute the rects in the stack algorithm
        mLayoutAlgorithm.computeRects(windowWidth, windowHeight, childStackBounds);

        // Update the scroll bounds
        updateMinMaxScroll(false, launchedWithAltTab, launchedFromHome);
    }

    public int getCurrentChildIndex() {
        if (getChildCount() == 0)
            return -1;

        ChildView<T> frontMostChild = (ChildView) getChildAt(getChildCount() / 2);

        if (frontMostChild != null) {
            return mCallback.getData().indexOf(frontMostChild.getAttachedKey());
        }

        return -1;
    }

    /**
     * Focuses the child at the specified index in the stack
     */
    public void scrollToChild(int childIndex) {
        if (getCurrentChildIndex() == childIndex)
            return;

        if (0 <= childIndex && childIndex < mCallback.getData().size()) {
            // Scroll the view into position (just center it in the curve)
            float newScroll = mLayoutAlgorithm.getStackScrollForChild(
                    mCallback.getData().get(childIndex)) - 0.5f;
            newScroll = mStackStackViewScroller.getBoundedStackScroll(newScroll);
            mStackStackViewScroller.setStackScroll(newScroll);
            //Alternate (animated) way
            //mStackStackViewScroller.animateScroll(mStackStackViewScroller.getStackScroll(), newScroll, null);
        }
    }

    /**
     * Computes the maximum number of visible childs and thumbnails.  Requires that
     * updateMinMaxScroll() is called first.
     */
    public LayoutAlgorithm.VisibilityReport computeStackVisibilityReport() {
        return mLayoutAlgorithm.computeStackVisibilityReport(mCallback.getData());
    }

    /**
     * This is called with the full window width and height to allow stack view children to
     * perform the full screen transition down.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        mStackBounds.set(0, 0, width, height);

        // Compute our stack/child rects
        Rect childStackBounds = new Rect(mStackBounds);
        computeRects(width, height, childStackBounds, mConfig.launchedWithAltTab,
                mConfig.launchedFromHome);

        // If this is the first layout, then scroll to the front of the stack and synchronize the
        // stack views immediately to load all the views
        if (mAwaitingFirstLayout) {
            mStackStackViewScroller.setStackScrollToInitialState();
            requestSynchronizeStackViewsWithModel();
            synchronizeStackViewsWithModel();
        }

        // Measure each of the ChildViews
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ChildView cv = (ChildView) getChildAt(i);
            if (cv.getBackground() != null) {
                cv.getBackground().getPadding(mTmpRect);
            } else {
                mTmpRect.setEmpty();
            }
            cv.measure(
                    MeasureSpec.makeMeasureSpec(
                            mLayoutAlgorithm.mChildRect.width() + mTmpRect.left + mTmpRect.right,
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(
                            mLayoutAlgorithm.mChildRect.height() + mTmpRect.top + mTmpRect.bottom,
                            MeasureSpec.EXACTLY));
        }

        setMeasuredDimension(width, height);
    }

    /**
     * This is called with the size of the space not including the top or right insets, or the
     * search bar height in portrait (but including the search bar width in landscape, since we want
     * to draw under it.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Layout each of the children
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ChildView cv = (ChildView) getChildAt(i);
            if (cv.getBackground() != null) {
                cv.getBackground().getPadding(mTmpRect);
            } else {
                mTmpRect.setEmpty();
            }
            cv.layout(mLayoutAlgorithm.mChildRect.left - mTmpRect.left,
                    mLayoutAlgorithm.mChildRect.top - mTmpRect.top,
                    mLayoutAlgorithm.mChildRect.right + mTmpRect.right,
                    mLayoutAlgorithm.mChildRect.bottom + mTmpRect.bottom);
        }

        if (mAwaitingFirstLayout) {
            mAwaitingFirstLayout = false;
            onFirstLayout();
        }
    }

    /**
     * Handler for the first layout.
     */
    void onFirstLayout() {
        int offscreenX = mLayoutAlgorithm.mViewRect.right -
                (mLayoutAlgorithm.mChildRect.left - mLayoutAlgorithm.mViewRect.left);

        int childCount = getChildCount();

        // Prepare the first view for its enter animation
        for (int i = childCount - 1; i >= 0; i--) {
            ChildView cv = (ChildView) getChildAt(i);
            // TODO: The false needs to go!
            cv.prepareEnterRecentsAnimation(i == childCount - 1, false, offscreenX);
        }

        // If the enter animation started already and we haven't completed a layout yet, do the
        // enter animation now
        if (mStartEnterAnimationRequestedAfterLayout) {
            startEnterRecentsAnimation(mStartEnterAnimationContext);
            mStartEnterAnimationRequestedAfterLayout = false;
            mStartEnterAnimationContext = null;
        }

        // When Alt-Tabbing, focus the previous child (but leave the animation until we finish the
        // enter animation).
        if (mConfig.launchedWithAltTab) {
            if (mConfig.launchedFromAppWithThumbnail) {
                focusChild(Math.max(0, mCallback.getData().size() - 2), false,
                        mConfig.launchedHasConfigurationChanged);
            } else {
                focusChild(Math.max(0, mCallback.getData().size() - 1), false,
                        mConfig.launchedHasConfigurationChanged);
            }
        }
    }

    public void showOverlapping(Context context) {
        // Try and start the enter animation (or restart it on configuration changed)
        ReferenceCountedTrigger trigger = new ReferenceCountedTrigger(context, null, null, null);
        ViewAnimation.ChildViewEnterContext ctx = new ViewAnimation.ChildViewEnterContext(trigger);

        // We have to increment/decrement the post animation trigger in case there are no children
        // to ensure that it runs
        ctx.postAnimationTrigger.increment();
        startEnterRecentsAnimation(ctx);
        ctx.postAnimationTrigger.decrement();
    }

    /**
     * Requests this child stacks to start it's enter-recents animation
     */
    public void startEnterRecentsAnimation(ViewAnimation.ChildViewEnterContext ctx) {
        // If we are still waiting to layout, then just defer until then
        if (mAwaitingFirstLayout) {
            mStartEnterAnimationRequestedAfterLayout = true;
            mStartEnterAnimationContext = ctx;
            return;
        }

        if (mCallback.getData().size() > 0) {
            int childCount = getChildCount();

            // Animate all the child views into view
            for (int i = childCount - 1; i >= 0; i--) {
                ChildView<T> cv = (ChildView) getChildAt(i);
                T key = cv.getAttachedKey();
                ctx.currentChildTransform = new ChildViewTransform ();
                ctx.currentStackViewIndex = i;
                ctx.currentStackViewCount = childCount;
                ctx.currentChildRect = mLayoutAlgorithm.mChildRect;
                // TODO: this needs to go
                ctx.currentChildOccludesLaunchTarget = false;
                mLayoutAlgorithm.getStackTransform(key, mStackStackViewScroller.getStackScroll(),
                        ctx.currentChildTransform, null);
                cv.startEnterRecentsAnimation(ctx);
            }

            // Add a runnable to the post animation ref counter to clear all the views
            ctx.postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    mStartEnterAnimationCompleted = true;
                }
            });
        }
    }

    void hideOverlapping(Context context, Runnable finishRunnable) {
        ReferenceCountedTrigger exitTrigger = new ReferenceCountedTrigger(context,
                null, finishRunnable, null);
        ViewAnimation.ChildViewExitContext exitCtx =
                new ViewAnimation.ChildViewExitContext(exitTrigger);

        exitCtx.postAnimationTrigger.increment();
        startExitToHomeAnimation(
                new ViewAnimation.ChildViewExitContext(exitTrigger));
        exitCtx.postAnimationTrigger.decrement();
    }

    /**
     * Requests this child stacks to start it's exit-recents animation.
     */
    public void startExitToHomeAnimation(ViewAnimation.ChildViewExitContext ctx) {
        // Stop any scrolling
        mStackStackViewScroller.stopScroller();
        mStackStackViewScroller.stopBoundScrollAnimation();
        // Animate all the child views out of view
        ctx.offscreenTranslationY = mLayoutAlgorithm.mViewRect.bottom -
                (mLayoutAlgorithm.mChildRect.top - mLayoutAlgorithm.mViewRect.top);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ChildView cv = (ChildView) getChildAt(i);
            cv.startExitToHomeAnimation(ctx);
        }
    }

    /**
     * Animates a child view in this stack as it launches.
     */
    public void startLaunchChildAnimation(ChildView cv, Runnable r, boolean lockToChild) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ChildView view = (ChildView) getChildAt(i);
            if (view == cv) {
                view.startLaunchChildAnimation(r, true, true, lockToChild);
            } else {
                // TODO: the false needs to go
                view.startLaunchChildAnimation(null, false, false, lockToChild);
            }
        }
    }

    /**
     * Final callback after Recents is finally hidden.
     */
    void onRecentsHidden() {
        reset();
    }

    public boolean isTransformedTouchPointInView(float x, float y, View child) {
        // TODO: confirm if this is the right approach
        if (child == null)
            return false;

        final Rect frame = new Rect();
        child.getHitRect(frame);

        return frame.contains((int) x, (int) y);
    }

    /**
     * * ViewPoolConsumer Implementation ***
     */
    @Override
    public ChildView createView(Context context) {
        return (ChildView) mInflater.inflate(R.layout.child_view, this, false);
    }

    @Override
    public void prepareViewToEnterPool(ChildView<T> cv) {
        T key = cv.getAttachedKey();

        mCallback.unloadViewData(key);
        cv.onChildUnbound();
        cv.onDataUnloaded();

        // Detach the view from the hierarchy
        detachViewFromParent(cv);

        // Reset the view properties
        cv.resetViewProperties();
    }

    @Override
    public void prepareViewToLeavePool(ChildView<T> cv, T key, boolean isNewView) {
        // It is possible for a view to be returned to the view pool before it is laid out,
        // which means that we will need to relayout the view when it is first used next.
        boolean requiresRelayout = cv.getWidth() <= 0 && !isNewView;

        // Rebind the child and request that this child's data be filled into the ChildView
        cv.onChildBound(key);

        // Load the child data
        mCallback.loadViewData(new WeakReference<ChildView<T>>(cv), key);

        // If we've finished the start animation, then ensure we always enable the focus animations
        if (mStartEnterAnimationCompleted) {
            cv.enableFocusAnimations();
        }

        // Find the index where this child should be placed in the stack
        int insertIndex = -1;
        int position = mCallback.getData().indexOf(key);
        if (position != -1) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                T otherKey = ((ChildView<T>) getChildAt(i)).getAttachedKey();
                int pos = mCallback.getData().indexOf(otherKey);
                if (position < pos) {
                    insertIndex = i;
                    break;
                }
            }
        }


        // Add/attach the view to the hierarchy
        if (isNewView) {
            addView(cv, insertIndex);
        } else {
            attachViewToParent(cv, insertIndex, cv.getLayoutParams());
            if (requiresRelayout) {
                cv.requestLayout();
            }
        }

        // Set the new state for this view, including the callbacks
        cv.setCallbacks(this);
        cv.setTouchEnabled(true);
    }

    @Override
    public boolean hasPreferredData(ChildView<T> cv, T preferredData) {
        return (cv.getAttachedKey() != null && cv.getAttachedKey().equals(preferredData));
    }

    /**
     * * OverlappingChildCallbacks Implementation ***
     */

    @Override
    public void onChildViewClicked(ChildView<T> cv, T key, boolean isPin) {
        mCallback.onItemClick(new WeakReference<ChildView<T>>(cv), key, isPin);
    }

    @Override
    public void onChildViewDismissed(ChildView<T> cv) {
        boolean childWasFocused = cv.isFocusedChild();

        T key = cv.getAttachedKey();
        int childIndex = mCallback.getData().indexOf(key);

        onStackChildRemoved(cv);

        // If the dismissed child was focused, then we should focus the new child in the same index
        if (childIndex != -1 && childWasFocused) {
            int nextChildIndex = Math.min(mCallback.getData().size() - 1, childIndex - 1);
            if (nextChildIndex >= 0) {
                ChildView nextCv = getChildViewForChild(mCallback.getData().get(nextChildIndex));
                if (nextCv != null) {
                    // Focus the next child, and only animate the visible state if we are launched
                    // from Alt-Tab
                    nextCv.setFocusedChild(mConfig.launchedWithAltTab);
                }
            }
        }
    }

    public void onStackChildRemoved(ChildView<T> removedView) {
        // Remove the view associated with this child, we can't rely on updateTransforms
        // to work here because the child is no longer in the list
        if (removedView != null) {
            T key = removedView.getAttachedKey();
            mViewPool.returnViewToPool(removedView);

            // Notify the callback that we've removed the child and it can clean up after it
            mCallback.onViewDismissed(key);
        }
    }

    public void notifyDataSetChanged() {
        // Get the stack scroll of the child to anchor to (since we are removing something, the front
        // most child will be our anchor child)
        T anchorChild = null;
        float prevAnchorChildScroll = 0;
        boolean pullStackForward = mCallback.getData().size() > 0;
        if (pullStackForward) {
            anchorChild = mCallback.getData().get(mCallback.getData().size() - 1);
            prevAnchorChildScroll = mLayoutAlgorithm.getStackScrollForChild(anchorChild);
        }

        // Update the min/max scroll and animate other child views into their new positions
        updateMinMaxScroll(true, mConfig.launchedWithAltTab, mConfig.launchedFromHome);

        // Offset the stack by as much as the anchor child would otherwise move back
        if (pullStackForward) {
            float anchorChildScroll = mLayoutAlgorithm.getStackScrollForChild(anchorChild);
            mStackStackViewScroller.setStackScroll(mStackStackViewScroller.getStackScroll() + (anchorChildScroll
                    - prevAnchorChildScroll));
            mStackStackViewScroller.boundScroll();
        }

        // Animate all the childs into place
        requestSynchronizeStackViewsWithModel(200);

        T newFrontMostChild = mCallback.getData().size() > 0 ?
                mCallback.getData().get(mCallback.getData().size() - 1)
                : null;
        // Update the new front most child
        if (newFrontMostChild != null) {
            ChildView<T> frontCv = getChildViewForChild(newFrontMostChild);
            if (frontCv != null) {
                frontCv.onChildBound(newFrontMostChild);
            }
        }

        // If there are no remaining childs
        if (mCallback.getData().size() == 0) {
            mCallback.onNoViewsToOverlapping();
        }
    }

    @Override
    public void onChildViewFocusChanged(ChildView<T> cv, boolean focused) {
        if (focused) {
            mFocusedChildIndex = mCallback.getData().indexOf(cv.getAttachedKey());
        }
    }

    @Override
    public void onChildViewLockStatusChanged(ChildView<T> cv, T key) {
        mCallback.toggleLockStatus(key);
    }

    @Override
    public void onChildViewLongClicked(ChildView<T> cv, T key) {
        mCallback.onItemLongClick(key);
    }

    /**
     * * StackViewScroller.StackViewScrollerCallbacks ***
     */

    @Override
    public void onScrollChanged(float p) {
        requestSynchronizeStackViewsWithModel();
        postInvalidateOnAnimation();
    }

    Callback<T> mCallback;

    public void dismissAll(Runnable runnable) {
        int childCount = getChildCount();
        ReferenceCountedTrigger trigger = new ReferenceCountedTrigger(getContext(), null, null, null);
        for (int i = childCount - 1; i >= 0; i--) {
            ChildView<T> cv = (ChildView) getChildAt(i);
            if (!mCallback.getItemLockStatus(cv.getAttachedKey())) {
                trigger.increment();
                cv.dismissChild(trigger);
            }
        }
        trigger.addLastDecrementRunnable(runnable);
    }

    public interface Callback<T> {
        ArrayList<T> getData();

        void loadViewData(WeakReference<ChildView<T>> cv, T item);

        void unloadViewData(T item);

        void onViewDismissed(T item);

        void onItemClick(WeakReference<ChildView<T>> cv, T item, boolean isScreenPin);

        void onItemLongClick(T item);

        void toggleLockStatus(T item);

        boolean getItemLockStatus(T item);

        void onNoViewsToOverlapping();

        //*/ freeme.gouzhouping, 20180327. Recents, blank view.
        void updateTaskFromTaskList(T item);
        //*/
    }

    public final void onBusEvent(LaunchFrontTaskEvent event) {
        if (getChildCount() > 0) {
            if (getContext () instanceof Activity) {
                if (((SystemUIApplication) ((Activity) getContext()).getApplication()).getSystemServices().getTasks().size() == 0) {
                    FreemeEventBus.getDefault().send(new ToggleRecentsEvent());
                    return;
                }
            }
            ChildView<T> frontChild = (ChildView) getChildAt(getChildCount() - 1);
            Task frontChildData =  (Task)frontChild.getAttachedKey();
            FreemeEventBus.getDefault().send(new LaunchTaskEvent(frontChildData, frontChild, false));
        }else {
            FreemeEventBus.getDefault().send(new ToggleRecentsEvent());
        }
    }

    //*/ freeme.gouzhouping, 20180327. Recents, blank view.
    @Override
    public void updateTaskFromTaskList(T key) {
        mCallback.updateTaskFromTaskList(key);
    }
    //*/
}
