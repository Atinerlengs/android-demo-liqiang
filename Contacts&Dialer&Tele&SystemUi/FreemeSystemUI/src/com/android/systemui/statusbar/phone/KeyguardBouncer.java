/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardSecurityView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.R;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.DejankUtils;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.keyguard.DismissCallbackRegistry;

import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;

import static com.android.keyguard.KeyguardHostView.OnDismissAction;
import static com.android.keyguard.KeyguardSecurityModel.SecurityMode;

import com.freeme.face.ui.FaceprintAuthentication;

/**
 * A class which manages the bouncer on the lockscreen.
 */
public class KeyguardBouncer {

    final static private String TAG = "KeyguardBouncer";
    private final boolean DEBUG = true;

    protected final Context mContext;
    protected final ViewMediatorCallback mCallback;
    protected final LockPatternUtils mLockPatternUtils;
    protected final ViewGroup mContainer;
    private final FalsingManager mFalsingManager;
    private final DismissCallbackRegistry mDismissCallbackRegistry;
    private final Handler mHandler;
    protected KeyguardHostView mKeyguardView;
    protected ViewGroup mRoot;
    private boolean mShowingSoon;
    private int mBouncerPromptReason;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback =
            new KeyguardUpdateMonitorCallback() {
                @Override
                public void onStrongAuthStateChanged(int userId) {
                    mBouncerPromptReason = mCallback.getBouncerPromptReason();

                    //*/ freeme,chenming. 20171218. Faceprint
                    if (!isFaceprintAllowedForUser() && mHasFaceServiceStarted) {
                        cancelFaceService();
                    }
                    //*/
                }

                //*/ freeme,chenming. 20171218. Faceprint
                @Override
                public void onFaceUnlockStateChanged(boolean running, int userId) {
                    Slog.d(TAG, "onFaceUnlockStateChanged: running = " + running + hasAuthenticateSuccess());
                    if (running){
                        updateFaceAnimState(true);
                    } else if(!running && hasAuthenticateSuccess()) {
                        notifyKeyguardAuthenticated(true);
                    }

                    mHasFaceServiceStarted = running;
                }

                @Override
                public void onKeyguardVisibilityChanged(boolean showing) {
                    Slog.d(TAG, "onKeyguardVisibilityChanged: showing = " + showing);
                    if (!showing) {
                        cancelFaceService();
                    } else if(showing && shouldListenForFace()) {
                        updateAuthenticateState(true);
                        updateFaceAnimState(true);
                    }
                }

                @Override
                public void onScreenTurnedOn() {
                    Slog.d(TAG, "onScreenTurnedOn");
                    if (KeyguardUpdateMonitor.getInstance(mContext).isKeyguardOccluded()
                            && shouldListenForFace()) {
                        updateAuthenticateState(true);
                    }
                    updateFaceAnimState(true);
                }

                @Override
                public void onStartedGoingToSleep(int why) {
                    Slog.d(TAG, "onStartedGoingToSleep");
                    updateFaceAnimState(false);
                    updateAuthenticateState(false);
                    mHasFaceServiceStarted = false;
                }
                //*/
            };
    private final Runnable mRemoveViewRunnable = this::removeView;
    private int mStatusBarHeight;

    /// M: use securityModel to check if it needs to show full screen mode
    private KeyguardSecurityModel mSecurityModel;

    public KeyguardBouncer(Context context, ViewMediatorCallback callback,
            LockPatternUtils lockPatternUtils, ViewGroup container,
            DismissCallbackRegistry dismissCallbackRegistry) {
        mContext = context;
        mCallback = callback;
        mLockPatternUtils = lockPatternUtils;
        mContainer = container;
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUpdateMonitorCallback);
        mFalsingManager = FalsingManager.getInstance(mContext);
        mDismissCallbackRegistry = dismissCallbackRegistry;
        mHandler = new Handler();
        mSecurityModel = new KeyguardSecurityModel(mContext);

        //*/ freeme,chenming. 20171218. Faceprint
        mNotificationPanel = (ViewGroup) mContainer.findViewById(R.id.notification_panel);
        //*/
    }

    ///M:
    public void show(boolean resetSecuritySelection) {
        show(resetSecuritySelection, false) ;
    }

    public void show(boolean resetSecuritySelection, boolean authenticated) {
        final int keyguardUserId = KeyguardUpdateMonitor.getCurrentUser();
        if (keyguardUserId == UserHandle.USER_SYSTEM && UserManager.isSplitSystemUser()) {
            // In split system user mode, we never unlock system user.
            return;
        }
        mFalsingManager.onBouncerShown();

        if (PowerOffAlarmManager.isAlarmBoot()) {
            Slog.d(TAG, "show() - this is alarm boot, just re-inflate.") ;
            /// M: fix ALPS01865324, we should call KeyguardPasswordView.onPause() to hide IME
            ///    before the KeyguardPasswordView is gone.
            if (mKeyguardView != null && mRoot != null) {
                Slog.d(TAG, "show() - before re-inflate, we should pause current view.") ;
                mKeyguardView.onPause();
            }
            // force to remove views.
            inflateView() ;
        } else {
            ensureView();
        }

        if (resetSecuritySelection) {
            // showPrimarySecurityScreen() updates the current security method. This is needed in
            // case we are already showing and the current security method changed.
            mKeyguardView.showPrimarySecurityScreen();
        }
        if (mRoot.getVisibility() == View.VISIBLE || mShowingSoon) {
            return;
        }

        final int activeUserId = ActivityManager.getCurrentUser();
        final boolean isSystemUser =
                UserManager.isSplitSystemUser() && activeUserId == UserHandle.USER_SYSTEM;
        final boolean allowDismissKeyguard = !isSystemUser && activeUserId == keyguardUserId;

        // If allowed, try to dismiss the Keyguard. If no security auth (password/pin/pattern) is
        // set, this will dismiss the whole Keyguard. Otherwise, show the bouncer.
        if (allowDismissKeyguard && mKeyguardView.dismiss(activeUserId)) {
            return;
        }

        // This condition may indicate an error on Android, so log it.
        if (!allowDismissKeyguard) {
            Slog.w(TAG, "User can't dismiss keyguard: " + activeUserId + " != " + keyguardUserId);
        }

        // Try to dismiss the Keyguard. If no security pattern is set, this will dismiss the whole
        // Keyguard. If we need to authenticate, show the bouncer.
        if (!mKeyguardView.dismiss(authenticated, activeUserId)) {
            if (DEBUG) {
                Slog.d(TAG, "show() - try to dismiss \"Bouncer\" directly.") ;
            }

            mShowingSoon = true;

            // Split up the work over multiple frames.
            DejankUtils.postAfterTraversal(mShowRunnable);
        }
    }

    private final Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            mRoot.setVisibility(View.VISIBLE);
            mKeyguardView.onResume();
            showPromptReason(mBouncerPromptReason);
            // We might still be collapsed and the view didn't have time to layout yet or still
            // be small, let's wait on the predraw to do the animation in that case.
            if (mKeyguardView.getHeight() != 0 && mKeyguardView.getHeight() != mStatusBarHeight) {
                mKeyguardView.startAppearAnimation();
            } else {
                mKeyguardView.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            @Override
                            public boolean onPreDraw() {
                                mKeyguardView.getViewTreeObserver().removeOnPreDrawListener(this);
                                mKeyguardView.startAppearAnimation();
                                return true;
                            }
                        });
                mKeyguardView.requestLayout();
            }
            mShowingSoon = false;
            mKeyguardView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
    };

    /**
     * Show a string explaining why the security view needs to be solved.
     *
     * @param reason a flag indicating which string should be shown, see
     *               {@link KeyguardSecurityView#PROMPT_REASON_NONE}
     *               and {@link KeyguardSecurityView#PROMPT_REASON_RESTART}
     */
    public void showPromptReason(int reason) {
        mKeyguardView.showPromptReason(reason);
    }

    public void showMessage(String message, int color) {
        mKeyguardView.showMessage(message, color);
    }

    private void cancelShowRunnable() {
        DejankUtils.removeCallbacks(mShowRunnable);
        mShowingSoon = false;
    }

    public void showWithDismissAction(OnDismissAction r, Runnable cancelAction) {
        ensureView();
        mKeyguardView.setOnDismissAction(r, cancelAction);
        show(false /* resetSecuritySelection */);
    }

    public void hide(boolean destroyView) {
        if (isShowing()) {
            mDismissCallbackRegistry.notifyDismissCancelled();
        }
        mFalsingManager.onBouncerHidden();
        cancelShowRunnable();
        if (mKeyguardView != null) {
            mKeyguardView.cancelDismissAction();
            mKeyguardView.cleanUp();
        }
        if (mRoot != null) {
            mRoot.setVisibility(View.INVISIBLE);
            if (destroyView) {

                // We have a ViewFlipper that unregisters a broadcast when being detached, which may
                // be slow because of AM lock contention during unlocking. We can delay it a bit.
                mHandler.postDelayed(mRemoveViewRunnable, 50);
            }

            //*/ freeme,chenming. 20171218. Faceprint
            updateFaceView();
            //*/
        }
    }

    /**
     * See {@link StatusBarKeyguardViewManager#startPreHideAnimation}.
     */
    public void startPreHideAnimation(Runnable runnable) {
        if (mKeyguardView != null) {
            mKeyguardView.startDisappearAnimation(runnable);
        } else if (runnable != null) {
            runnable.run();
        }
    }

    /**
     * Reset the state of the view.
     */
    public void reset() {
        cancelShowRunnable();
        inflateView();
        mFalsingManager.onBouncerHidden();
    }

    public void onScreenTurnedOff() {
        if (mKeyguardView != null && mRoot != null && mRoot.getVisibility() == View.VISIBLE) {
            mKeyguardView.onPause();
        }
    }

    public boolean isShowing() {
        return mShowingSoon || (mRoot != null && mRoot.getVisibility() == View.VISIBLE);
    }

    public void prepare() {
        boolean wasInitialized = mRoot != null;
        ensureView();
        if (wasInitialized) {
            mKeyguardView.showPrimarySecurityScreen();
        }
        mBouncerPromptReason = mCallback.getBouncerPromptReason();
    }

    protected void ensureView() {
        // Removal of the view might be deferred to reduce unlock latency,
        // in this case we need to force the removal, otherwise we'll
        // end up in an unpredictable state.
        boolean forceRemoval = mHandler.hasCallbacks(mRemoveViewRunnable);
        if (mRoot == null || forceRemoval) {
            inflateView();
        }
    }

    protected void inflateView() {
        removeView();
        mHandler.removeCallbacks(mRemoveViewRunnable);
        mRoot = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.keyguard_bouncer, null);
        mKeyguardView = mRoot.findViewById(R.id.keyguard_host_view);
        mKeyguardView.setLockPatternUtils(mLockPatternUtils);
        mKeyguardView.setViewMediatorCallback(mCallback);
        mContainer.addView(mRoot, mContainer.getChildCount());
        mStatusBarHeight = mRoot.getResources().getDimensionPixelOffset(
                com.android.systemui.R.dimen.status_bar_height);
        mRoot.setVisibility(View.INVISIBLE);

        final WindowInsets rootInsets = mRoot.getRootWindowInsets();
        if (rootInsets != null) {
            mRoot.dispatchApplyWindowInsets(rootInsets);
        }
    }

    protected void removeView() {
        //*/ freeme,chenming. 20171218. Faceprint
        removeFaceView();
        //*/

        if (mRoot != null && mRoot.getParent() == mContainer) {
            mContainer.removeView(mRoot);
            mRoot = null;
        }
    }

    public boolean onBackPressed() {
        return mKeyguardView != null && mKeyguardView.handleBackKey();
    }

    /**
     * @return True if and only if the security method should be shown before showing the
     * notifications on Keyguard, like SIM PIN/PUK.
     */
    public boolean needsFullscreenBouncer() {
        ensureView();
        /*if (mKeyguardView != null) {
            SecurityMode mode = mKeyguardView.getSecurityMode();
            return mode == SecurityMode.SimPin || mode == SecurityMode.SimPuk;
        }
        return false;*/
        SecurityMode mode = mSecurityModel.getSecurityMode(KeyguardUpdateMonitor.getCurrentUser());
        return mode == SecurityMode.SimPinPukMe1
                || mode == SecurityMode.SimPinPukMe2
                || mode == SecurityMode.SimPinPukMe3
                || mode == SecurityMode.SimPinPukMe4
                || mode == SecurityMode.AntiTheft
                || mode == SecurityMode.AlarmBoot;
    }

    /**
     * Like {@link #needsFullscreenBouncer}, but uses the currently visible security method, which
     * makes this method much faster.
     */
    public boolean isFullscreenBouncer() {
        if (mKeyguardView != null) {
            SecurityMode mode = mKeyguardView.getCurrentSecurityMode();
            return mode == SecurityMode.SimPinPukMe1
                || mode == SecurityMode.SimPinPukMe2
                || mode == SecurityMode.SimPinPukMe3
                || mode == SecurityMode.SimPinPukMe4
                || mode == SecurityMode.AntiTheft
                || mode == SecurityMode.AlarmBoot;
        }
        return false;
    }

    /**
     * WARNING: This method might cause Binder calls.
     */
    public boolean isSecure() {
        return mKeyguardView == null || mKeyguardView.getSecurityMode() != SecurityMode.None;
    }

    public boolean shouldDismissOnMenuPressed() {
        return mKeyguardView.shouldEnableMenuKey();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        ensureView();
        return mKeyguardView.interceptMediaKey(event);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        ensureView();
        mKeyguardView.finish(strongAuth, KeyguardUpdateMonitor.getCurrentUser());
    }

    //*/ freeme.gouzhouping, 20180205. FreemeAppTheme, fp.
    public int getBouncerPromptReason() {
        return mBouncerPromptReason;
    }

    public void showFingerprintHintView () {
        mKeyguardView.showFingerprintHintView();
    }

    public void hideFingerprintHintView() {
        mKeyguardView.hideFingerprintHintView();
    }
    //*/

    //*/ freeme,chenming. 20171218. Faceprint
    private FaceprintAuthentication mFaceRoot;

    private boolean mFaceHasAdded;

    private boolean mHasFaceServiceStarted;

    private boolean mHasAnimationRunning;

    private ViewGroup mNotificationPanel ;

    private void updateFaceView() {
        if (isSecure()
                && isFaceprintAllowedForUser()
                && !mFaceHasAdded
                && hasAuthenticateUI()) {
            mFaceRoot = new FaceprintAuthentication(mContext);
            mFaceRoot.setTranslationZ(15);
            mFaceRoot.setOnCancelListener(new FaceprintAuthentication.OnCancelListener() {
                @Override
                public void onCancel(View v) {
                    removeFaceView();
                }
            });
            mNotificationPanel.addView(mFaceRoot, mNotificationPanel.getChildCount());
            mFaceHasAdded = true;
            Slog.d(TAG, "updateFaceView() - add face view");

            if (mHasFaceServiceStarted) {
                updateFaceAnimState(true);
            }
        }
    }

    private void cancelFaceService() {
        if (hasAuthenticateUI()) {
            removeFaceView();
        }

        if (hasEnrolledFace()) {
            updateAuthenticateState(false);
            mHasFaceServiceStarted = false;
            Slog.d(TAG, "cancelFaceService()");
        }
    }

    private void removeFaceView() {
        if (mFaceRoot != null) {
            updateFaceAnimState(false);
            mNotificationPanel.removeView(mFaceRoot);
            mFaceHasAdded = false;
            mFaceRoot = null;

            Slog.d(TAG, "removeFaceView() - really remove views.");
        }
    }

    private boolean shouldListenForFace() {
        return isSecure()
                && isFaceprintAllowedForUser()
                && hasEnrolledFace()
                && !mHasFaceServiceStarted
                && KeyguardUpdateMonitor.getInstance(mContext).isScreenOn();
    }

    private boolean hasEnrolledFace() {
        return KeyguardUpdateMonitor.getInstance(mContext).hasEnrolledFace();
    }

    private boolean hasAuthenticateUI() {
        return hasEnrolledFace() && KeyguardUpdateMonitor.getInstance(mContext).hasAuthenticateUI();
    }

    private boolean hasAuthenticateSuccess() {
        return KeyguardUpdateMonitor.getInstance(mContext).hasAuthenticateSuccess();
    }

    private boolean isFaceprintAllowedForUser(){
        return KeyguardUpdateMonitor.getInstance(mContext).isFaceprintAllowedForUser();
    }

    private void updateAuthenticateState(boolean state) {
        KeyguardUpdateMonitor.getInstance(mContext).updateFaceService(state);
    }

    private void updateFaceAnimState(boolean running) {
        if (mFaceRoot != null) {
            Slog.d(TAG, "updateFaceAnimState() running: " + running + "mHasAnimationRunning: " + mHasAnimationRunning);
            if (running && !mHasAnimationRunning) {
                mFaceRoot.startAnimation();
            } else if (!running && mHasAnimationRunning){
                mFaceRoot.stopAnimation();
            }

            mHasAnimationRunning = running;
        }
    }
    //*/
}
