package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

public class ViewTouchHandler implements SwipeHelper.Callback {

    static final int INACTIVE_POINTER_ID = -1;

    Config mConfig;
    OverlappingStackView mOverlappingStackView;
    StackViewScroller mStackViewScroller;
    VelocityTracker mVelocityTracker;

    boolean mIsScrolling;

    float mInitialP;
    float mLastP;
    float mTotalPMotion;
    int mInitialMotionX, mInitialMotionY;
    int mLastMotionX, mLastMotionY;
    int mActivePointerId = INACTIVE_POINTER_ID;
    ChildView mActiveChildView = null;

    int mMinimumVelocity;
    int mMaximumVelocity;
    // The scroll touch slop is used to calculate when we start scrolling
    int mScrollTouchSlop;
    // The page touch slop is used to calculate when we start swiping
    float mPagingTouchSlop;

    SwipeHelper mSwipeHelper;
    boolean mInterceptedBySwipeHelper;

    public static final int StackMinOverscrollRange = 32;
    public static final int StackMaxOverscrollRange = 128;

    public ViewTouchHandler(Context context, OverlappingStackView dv,
                            Config config, StackViewScroller stackViewScroller) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mScrollTouchSlop = configuration.getScaledTouchSlop();
        mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
        mOverlappingStackView = dv;
        mStackViewScroller = stackViewScroller;
        mConfig = config;

        float densityScale = context.getResources().getDisplayMetrics().density;
        mSwipeHelper = new SwipeHelper(SwipeHelper.Y, this,
                densityScale, mPagingTouchSlop);
        mSwipeHelper.setMinAlpha(1f);
    }

    /**
     * Velocity tracker helpers
     */
    void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * Returns the view at the specified coordinates
     */
    ChildView findViewAtPoint(int x, int y) {
        int childCount = mOverlappingStackView.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            ChildView cv = (ChildView) mOverlappingStackView.getChildAt(i);
            if (cv.getVisibility() == View.VISIBLE) {
                if (mOverlappingStackView.isTransformedTouchPointInView(x, y, cv)) {
                    return cv;
                }
            }
        }
        return null;
    }

    /**
     * Constructs a simulated motion event for the current stack scroll.
     */
    MotionEvent createMotionEventForStackScroll(MotionEvent ev) {
        MotionEvent pev = MotionEvent.obtainNoHistory(ev);
        pev.setLocation(mStackViewScroller.progressToScrollRange(mStackViewScroller.getStackScroll()), 0);
        return pev;
    }

    /**
     * Touch preprocessing for handling below
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Return early if we have no children
        boolean hasChildren = (mOverlappingStackView.getChildCount() > 0);
        if (!hasChildren) {
            return false;
        }

        // Pass through to swipe helper if we are swiping
        mInterceptedBySwipeHelper = mSwipeHelper.onInterceptTouchEvent(ev);
        if (mInterceptedBySwipeHelper) {
            return true;
        }

        boolean wasScrolling = mStackViewScroller.isScrolling() ||
                (mStackViewScroller.mScrollAnimator != null && mStackViewScroller.mScrollAnimator.isRunning());
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                // Save the touch down info
                mInitialMotionX = mLastMotionX = (int) ev.getX();
                mInitialMotionY = mLastMotionY = (int) ev.getY();
                mInitialP = mLastP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(mLastMotionX);
                mActivePointerId = ev.getPointerId(0);
                mActiveChildView = findViewAtPoint(mLastMotionX, mLastMotionY);
                // Stop the current scroll if it is still flinging
                mStackViewScroller.stopScroller();
                mStackViewScroller.stopBoundScrollAnimation();
                // Initialize the velocity tracker
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INACTIVE_POINTER_ID) break;

                // Initialize the velocity tracker if necessary
                initVelocityTrackerIfNotExists();
                mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));

                int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                int y = (int) ev.getY(activePointerIndex);
                int x = (int) ev.getX(activePointerIndex);
                if (Math.abs(x - mInitialMotionX) > mScrollTouchSlop) {
                    // Save the touch move info
                    mIsScrolling = true;
                    // Disallow parents from intercepting touch events
                    final ViewParent parent = mOverlappingStackView.getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                mLastMotionX = x;
                mLastMotionY = y;
                mLastP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(mLastMotionX);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                // Animate the scroll back if we've cancelled
                mStackViewScroller.animateBoundScroll();
                // Reset the drag state and the velocity tracker
                mIsScrolling = false;
                mActivePointerId = INACTIVE_POINTER_ID;
                mActiveChildView = null;
                mTotalPMotion = 0;
                recycleVelocityTracker();
                break;
            }
        }

        return wasScrolling || mIsScrolling;
    }

    /**
     * Handles touch events once we have intercepted them
     */
    public boolean onTouchEvent(MotionEvent ev) {
        // Short circuit if we have no children
        boolean hasChildren = (mOverlappingStackView.getChildCount() > 0);
        if (!hasChildren) {
            return false;
        }

        // Pass through to swipe helper if we are swiping
        if (mInterceptedBySwipeHelper && mSwipeHelper.onTouchEvent(ev)) {
            return true;
        }

        // Update the velocity tracker
        initVelocityTrackerIfNotExists();

        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                // Save the touch down info
                mInitialMotionX = mLastMotionX = (int) ev.getX();
                mInitialMotionY = mLastMotionY = (int) ev.getY();
                mInitialP = mLastP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(mLastMotionX);
                mActivePointerId = ev.getPointerId(0);
                mActiveChildView = findViewAtPoint(mLastMotionX, mLastMotionY);
                // Stop the current scroll if it is still flinging
                mStackViewScroller.stopScroller();
                mStackViewScroller.stopBoundScrollAnimation();
                // Initialize the velocity tracker
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));
                // Disallow parents from intercepting touch events
                final ViewParent parent = mOverlappingStackView.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mActivePointerId = ev.getPointerId(index);
                mLastMotionX = (int) ev.getX(index);
                mLastMotionY = (int) ev.getY(index);
                mLastP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(mLastMotionX);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INACTIVE_POINTER_ID) break;

                mVelocityTracker.addMovement(createMotionEventForStackScroll(ev));

                int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                int x = (int) ev.getX(activePointerIndex);
                int y = (int) ev.getY(activePointerIndex);
                int xTotal = Math.abs(x - mInitialMotionX);
                float curP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(x);
                float deltaP = mLastP - curP;
                if (!mIsScrolling) {
                    if (xTotal > mScrollTouchSlop) {
                        mIsScrolling = true;
                        // Disallow parents from intercepting touch events
                        final ViewParent parent = mOverlappingStackView.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                }
                if (mIsScrolling) {
                    float curStackScroll = mStackViewScroller.getStackScroll();
                    float overScrollAmount = mStackViewScroller.getScrollAmountOutOfBounds(curStackScroll + deltaP);
                    if (Float.compare(overScrollAmount, 0f) != 0) {
                        // Bound the overscroll to a fixed amount, and inversely scale the y-movement
                        // relative to how close we are to the max overscroll
                        float maxOverScroll = mConfig.childStackOverscrollPct;
                        deltaP *= (1f - (Math.min(maxOverScroll, overScrollAmount)
                                / maxOverScroll));
                    }
                    mStackViewScroller.setStackScroll(curStackScroll + deltaP);
                }
                mLastMotionX = x;
                mLastMotionY = y;
                mLastP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(mLastMotionX);
                mTotalPMotion += Math.abs(deltaP);
                break;
            }
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocity = (int) mVelocityTracker.getXVelocity(mActivePointerId);
                if (mIsScrolling && (Math.abs(velocity) > mMinimumVelocity)) {
                    float overscrollRangePct = Math.abs((float) velocity / mMaximumVelocity);
                    int overscrollRange = (int) (Math.min(1f, overscrollRangePct) *
                            (StackMaxOverscrollRange - StackMinOverscrollRange));
                    mStackViewScroller.mScroller.fling(mStackViewScroller.progressToScrollRange(mStackViewScroller.getStackScroll()),
                            0,
                            velocity, 0,
                            mStackViewScroller.progressToScrollRange(mOverlappingStackView.getStackAlgorithm().mMinScrollP),
                            mStackViewScroller.progressToScrollRange(mOverlappingStackView.getStackAlgorithm().mMaxScrollP),
                            0, 0,
                            0, StackMinOverscrollRange + overscrollRange);
                    // Invalidate to kick off computeScroll
                    mOverlappingStackView.invalidate();
                } else if (mStackViewScroller.isScrollOutOfBounds()) {
                    // Animate the scroll back into bounds
                    mStackViewScroller.animateBoundScroll();
                }

                mActivePointerId = INACTIVE_POINTER_ID;
                mIsScrolling = false;
                mTotalPMotion = 0;
                recycleVelocityTracker();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // Select a new active pointer id and reset the motion state
                    final int newPointerIndex = (pointerIndex == 0) ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    mLastMotionX = (int) ev.getX(newPointerIndex);
                    mLastMotionY = (int) ev.getY(newPointerIndex);
                    mLastP = mOverlappingStackView.getStackAlgorithm().screenXToCurveProgress(mLastMotionX);
                    mVelocityTracker.clear();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                if (mStackViewScroller.isScrollOutOfBounds()) {
                    // Animate the scroll back into bounds
                    mStackViewScroller.animateBoundScroll();
                }
                mActivePointerId = INACTIVE_POINTER_ID;
                mIsScrolling = false;
                mTotalPMotion = 0;
                recycleVelocityTracker();
                break;
            }
        }
        return true;
    }

    /**
     * Handles generic motion events
     */
    public boolean onGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & InputDevice.SOURCE_CLASS_POINTER) ==
                InputDevice.SOURCE_CLASS_POINTER) {
            int action = ev.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_SCROLL:
                    // Find the front most child and scroll the next child to the front
                    float vScroll = ev.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    if (vScroll > 0) {
                        if (mOverlappingStackView.ensureFocusedChild()) {
                            mOverlappingStackView.focusNextChild(true, false);
                        }
                    } else {
                        if (mOverlappingStackView.ensureFocusedChild()) {
                            mOverlappingStackView.focusNextChild(false, false);
                        }
                    }
                    return true;
            }
        }
        return false;
    }

    /**
     * * SwipeHelper Implementation ***
     */

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return findViewAtPoint((int) ev.getX(), (int) ev.getY());
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public void onBeginDrag(View v) {
        ChildView cv = (ChildView) v;
        // Disallow touch events from this child view
        cv.setTouchEnabled(false);
        // Disallow parents from intercepting touch events
        final ViewParent parent = mOverlappingStackView.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public void onSwipeChanged(View v, float delta) {
        // Do nothing
    }

    @Override
    public void onChildDismissed(View v) {
        ChildView cv = (ChildView) v;
        // Re-enable touch events from this child view
        cv.setTouchEnabled(true);
        // Remove the child view from the stack
        mOverlappingStackView.onChildViewDismissed(cv);
    }

    @Override
    public void onSnapBackCompleted(View v) {
        ChildView cv = (ChildView) v;
        // Re-enable touch events from this child view
        cv.setTouchEnabled(true);
    }

    @Override
    public void onDragCancelled(View v) {
        // Do nothing
    }

    @Override
    public void toggleLockStatus(View v) {
        ChildView cv = (ChildView) v;
        cv.toggleLockStatus();
    }
}
