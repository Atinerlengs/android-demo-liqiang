package com.freeme.recents.presentation.view.component.overlappingstackview;

import android.animation.ValueAnimator;
import android.graphics.Rect;

import com.freeme.recents.presentation.view.component.overlappingstackview.utils.ReferenceCountedTrigger;

public class ViewAnimation {

    /* The animation context for a child view animation into Recents */
    public static class ChildViewEnterContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        public ReferenceCountedTrigger postAnimationTrigger;
        // An update listener to notify as the enter animation progresses (used for the home transition)
        public ValueAnimator.AnimatorUpdateListener updateListener;

        // These following properties are updated for each child view we start the enter animation on

        // Whether or not the current child occludes the launch target
        boolean currentChildOccludesLaunchTarget;
        // The child rect for the current stack
        Rect currentChildRect;
        // The transform of the current child view
        public ChildViewTransform currentChildTransform;
        // The view index of the current child view
        public int currentStackViewIndex;
        // The total number of child views
        public int currentStackViewCount;

        public ChildViewEnterContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

    /* The animation context for a child view animation out of Recents */
    public static class ChildViewExitContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        public ReferenceCountedTrigger postAnimationTrigger;

        // The translationY to apply to a ChildView to move it off the bottom of the child stack
        public int offscreenTranslationY;

        public ChildViewExitContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

}
