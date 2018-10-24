package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.graphics.Rect;

import com.freeme.recents.presentation.view.component.overlappingstackview.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class LayoutAlgorithm<T> {

    // These are all going to change
    static final float StackPeekMinScale = 0.95f; // The min scale of the last card in the peek area

    // A report of the visibility state of the stack
    public class VisibilityReport {
        public int numVisibleChilds;
        public int numVisibleThumbnails;

        /**
         * Package level ctor
         */
        VisibilityReport(int childs, int thumbnails) {
            numVisibleChilds = childs;
            numVisibleThumbnails = thumbnails;
        }
    }

    Config mConfig;
    boolean mIsmultiWindowMode;

    // The various rects that define the stack view
    public Rect mViewRect = new Rect();
    Rect mStackVisibleRect = new Rect();
    Rect mStackRect = new Rect();
    Rect mChildRect = new Rect();

    // The min/max scroll progress
    float mMinScrollP;
    float mMaxScrollP;
    float mInitialScrollP;
    int mBetweenAffiliationOffset;
    HashMap<T, Float> mChildProgressMap = new HashMap<T, Float>();

    // Log function
    static final float XScale = 1.75f;  // The large the XScale, the longer the flat area of the curve
    static final float LogBase = 4000;
    static final int PrecisionSteps = 250;
    static float[] xp;
    static float[] px;

    public LayoutAlgorithm(Config config, boolean ismultiwindowmode) {
        mConfig = config;
        mIsmultiWindowMode = ismultiwindowmode;
        // Precompute the path
        initializeCurve();
    }

    /**
     * Computes the stack and child rects
     */
    public void computeRects(int windowWidth, int windowHeight, Rect childStackBounds) {
        // Compute the stack rects
        mViewRect.set(0, 0, windowWidth, windowHeight);
        mStackRect.set(childStackBounds);
        mStackVisibleRect.set(childStackBounds);
        mStackVisibleRect.bottom = mViewRect.bottom;
        // Compute the child rect
        int width = mIsmultiWindowMode ? mConfig.childThumbnailMultiWindowModeWidth : mConfig.childThumbnailWidth;
        int childThumbnailHeight = mIsmultiWindowMode ? mConfig.childThumbnailMultiWindowModeHeigh : mConfig.childThumbnailHeight;
        int height = childThumbnailHeight + mConfig.childHeaderHeight;

        /*/ freeme.gouzhouping, 20180403. Recents.
        mChildRect.set(0, top, width, top + height);
        /*/
        int top = mStackRect.top + (mStackRect.height() - height) / 2;
        int bottom = mStackRect.height() - mConfig.childMarginBot;
        if (mIsmultiWindowMode) {
            mChildRect.set(0, top, width, top + height);
        } else {
            mChildRect.set(0, bottom - height, width, bottom);
        }
        //*/

        // Update the affiliation offsets
        float visibleChildPct = 0.9f;
        mBetweenAffiliationOffset = (int) (visibleChildPct * mChildRect.width());
    }

    /**
     * Computes the minimum and maximum scroll progress values.  This method may be called before
     * the Configuration is set, so we need to pass in the alt-tab state.
     */
    void computeMinMaxScroll(ArrayList<T> data, boolean launchedWithAltTab,
                             boolean launchedFromHome) {
        // Clear the progress map
        mChildProgressMap.clear();

        // Return early if we have no childs
        if (data.isEmpty()) {
            mMinScrollP = mMaxScrollP = 0;
            return;
        }

        int childWidth = mChildRect.width();
        float pAtRightOfStackRect = screenXToCurveProgress(mStackVisibleRect.right);
        float pBetweenAffiliateOffset = pAtRightOfStackRect -
                screenXToCurveProgress(mStackVisibleRect.right - mBetweenAffiliationOffset);

        // Update the child offsets
        float pAtFrontMostCardLeft = 0.6f;
        int childCount = data.size();
        for (int i = 0; i < childCount; i++) {
            mChildProgressMap.put(data.get(i), pAtFrontMostCardLeft);

            if (i < (childCount - 1)) {
                // Increment the peek height
                float pPeek = pBetweenAffiliateOffset;
                pAtFrontMostCardLeft += pPeek;
            }
        }

        float offset = screenXToCurveProgress((mStackVisibleRect.right - childWidth)/2);
        float scale = curveProgressToScale(offset);
        mMaxScrollP = pAtFrontMostCardLeft - screenXToCurveProgress((mStackVisibleRect.right - (int) (childWidth * scale))/2);
        mMinScrollP = data.size() == 1 ? Math.max(mMaxScrollP, 0f) : 0f;
        mInitialScrollP = data.size() == 1 ? mMaxScrollP : pAtFrontMostCardLeft - 0.825f;
    }

    /**
     * Computes the maximum number of visible childs and thumbnails.  Requires that
     * computeMinMaxScroll() is called first.
     */
    public VisibilityReport computeStackVisibilityReport(ArrayList<T> data) {
        if (data.size() <= 1) {
            return new VisibilityReport(1, 1);
        }

        // Walk backwards in the child stack and count the number of childs and visible thumbnails
        int numVisibleChilds = 1;
        int numVisibleThumbnails = 1;

        float progress;
        for (int i = data.size() - 1; i >= 0; i--) {
            progress = mChildProgressMap.get(data.get(i)) - mInitialScrollP;
            if (progress < 0) {
                break;
            }
            numVisibleChilds++;
            numVisibleThumbnails++;
        }
        return new VisibilityReport(numVisibleChilds, numVisibleThumbnails);
    }

    /**
     * Update/get the transform
     */
    public ChildViewTransform getStackTransform(T key, float stackScroll,
                                                ChildViewTransform transformOut,
                                                ChildViewTransform prevTransform) {
        // Return early if we have an invalid index
        if (!mChildProgressMap.containsKey(key)) {
            transformOut.reset();
            return transformOut;
        }
        return getStackTransform(mChildProgressMap.get(key), stackScroll, transformOut,
                prevTransform);
    }

    /**
     * Update/get the transform
     */
    public ChildViewTransform getStackTransform(float childProgress, float stackScroll,
                                                ChildViewTransform transformOut,
                                                ChildViewTransform prevTransform) {
        float pChildRelative = childProgress - stackScroll;
        float pBounded = Math.max(0, Math.min(pChildRelative, 1f));
        // If the child top is outside of the bounds below the screen, then immediately reset it
        if (pChildRelative > 1f) {
            transformOut.reset();
            transformOut.rect.set(mChildRect);
            return transformOut;
        }
        // The check for the top is trickier, since we want to show the next child if it is at all
        // visible, even if p < 0.
        if (pChildRelative < 0f) {
            if (prevTransform != null && Float.compare(prevTransform.p, 0f) <= 0) {
                transformOut.reset();
                transformOut.rect.set(mChildRect);
                return transformOut;
            }
        }
        float scale = curveProgressToScale(pBounded);
        int scaleXOffset = (int) (((1f - scale) * mChildRect.width()) / 2);
        int minZ = mConfig.childViewTranslationZMinPx;
        int maxZ = mConfig.childViewTranslationZMaxPx;
        transformOut.scale = scale;
        transformOut.translationX = curveProgressToScreenX(pBounded) - mStackVisibleRect.left -
                scaleXOffset;
        transformOut.translationZ = Math.max(minZ, minZ + (pBounded * (maxZ - minZ)));
        transformOut.rect.set(mChildRect);
        transformOut.rect.offset(0, transformOut.translationX);
        Utils.scaleRectAboutCenter(transformOut.rect, transformOut.scale);
        transformOut.visible = true;
        transformOut.p = pChildRelative;
        return transformOut;
    }

    /**
     * Returns the scroll to such child top = 1f;
     */
    public float getStackScrollForChild(T key) {
        if (!mChildProgressMap.containsKey(key)) return 0f;
        return mChildProgressMap.get(key);
    }

    /**
     * Initializes the curve.
     */
    public static void initializeCurve() {
        if (xp != null && px != null) return;
        xp = new float[PrecisionSteps + 1];
        px = new float[PrecisionSteps + 1];

        // Approximate f(x)
        float[] fx = new float[PrecisionSteps + 1];
        float step = 1f / PrecisionSteps;
        float x = 0;
        for (int xStep = 0; xStep <= PrecisionSteps; xStep++) {
            fx[xStep] = logFunc(x);
            x += step;
        }
        // Calculate the arc length for x:1->0
        float pLength = 0;
        float[] dx = new float[PrecisionSteps + 1];
        dx[0] = 0;
        for (int xStep = 1; xStep < PrecisionSteps; xStep++) {
            dx[xStep] = (float) Math.sqrt(Math.pow(fx[xStep] - fx[xStep - 1], 2) + Math.pow(step, 2));
            pLength += dx[xStep];
        }
        // Approximate p(x), a function of cumulative progress with x, normalized to 0..1
        float p = 0;
        px[0] = 0f;
        px[PrecisionSteps] = 1f;
        for (int xStep = 1; xStep <= PrecisionSteps; xStep++) {
            p += Math.abs(dx[xStep] / pLength);
            px[xStep] = p;
        }
        // Given p(x), calculate the inverse function x(p). This assumes that x(p) is also a valid
        // function.
        int xStep = 0;
        p = 0;
        xp[0] = 0f;
        xp[PrecisionSteps] = 1f;
        for (int pStep = 0; pStep < PrecisionSteps; pStep++) {
            // Walk forward in px and find the x where px <= p && p < px+1
            while (xStep < PrecisionSteps) {
                if (px[xStep] > p) break;
                xStep++;
            }
            // Now, px[xStep-1] <= p < px[xStep]
            if (xStep == 0) {
                xp[pStep] = 0;
            } else {
                // Find x such that proportionally, x is correct
                float fraction = (p - px[xStep - 1]) / (px[xStep] - px[xStep - 1]);
                x = (xStep - 1 + fraction) * step;
                xp[pStep] = x;
            }
            p += step;
        }
    }

    /**
     * Reverses and scales out x.
     */
    static float reverse(float x) {
        return (-x * XScale) + 1;
    }

    /**
     * The log function describing the curve.
     */
    static float logFunc(float x) {
        return 1f - (float) (Math.pow(LogBase, reverse(x))) / (LogBase);
    }

    /**
     * Converts from the progress along the curve to a screen coordinate.
     */
    int curveProgressToScreenX(float p) {
        if (p < 0 || p > 1) return mStackVisibleRect.left + (int) (p * mStackVisibleRect.width());
        float pIndex = p * PrecisionSteps;
        int pFloorIndex = (int) Math.floor(pIndex);
        int pCeilIndex = (int) Math.ceil(pIndex);
        float xFraction = 0;
        if (pFloorIndex < PrecisionSteps && (pCeilIndex != pFloorIndex)) {
            float pFraction = (pIndex - pFloorIndex) / (pCeilIndex - pFloorIndex);
            xFraction = (xp[pCeilIndex] - xp[pFloorIndex]) * pFraction;
        }
        float x = xp[pFloorIndex] + xFraction;
        return mStackVisibleRect.left + (int) (x * mStackVisibleRect.width());
    }

    /**
     * Converts from the progress along the curve to a scale.
     */
    float curveProgressToScale(float p) {
        if (p < 0) return StackPeekMinScale;
        if (p > 1) return 1f;
        float scaleRange = (1f - StackPeekMinScale);
        float scale = StackPeekMinScale + (p * scaleRange);
        return scale;
    }

    /**
     * Converts from a screen coordinate to the progress along the curve.
     */
    float screenXToCurveProgress(int screenX) {
        float x = (float) (screenX - mStackVisibleRect.left) / mStackVisibleRect.width();
        if (x < 0 || x > 1) return x;
        float xIndex = x * PrecisionSteps;
        int xFloorIndex = (int) Math.floor(xIndex);
        int xCeilIndex = (int) Math.ceil(xIndex);
        float pFraction = 0;
        if (xFloorIndex < PrecisionSteps && (xCeilIndex != xFloorIndex)) {
            float xFraction = (xIndex - xFloorIndex) / (xCeilIndex - xFloorIndex);
            pFraction = (px[xCeilIndex] - px[xFloorIndex]) * xFraction;
        }
        return px[xFloorIndex] + pFraction;
    }
}
