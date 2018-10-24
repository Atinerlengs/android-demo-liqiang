package com.mediatek.incallui.video;

import android.os.SystemClock;

import android.telecom.VideoProfile;

import com.android.dialer.common.LogUtil;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.CallList;
import com.android.incallui.videotech.VideoTech;
import com.android.incallui.videotech.utils.SessionModificationState;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;
import com.mediatek.incallui.CallTimer;
import com.mediatek.incallui.plugin.ExtensionManager;

//import com.mediatek.incallui.ext.ExtensionManager;

/**
 * M: [Video Call] A helper to downgrade video call if necessary.
 * Especially downgrade when UI in background or quit.
 */
public class VideoSessionController implements InCallPresenter.InCallStateListener,
        InCallPresenter.IncomingCallListener {
    private static final boolean DEBUG = true;
    private static final int DEFAULT_COUNT_DOWN_SECONDS = 20;
    ///M:add for upgrade recevied timeout
    private static final int COUNT_DOWN_SECONDS_FOR_RECEVICE_UPGRADE_WITH_PRECONDITION = 15;
    private static final long MILLIS_PER_SECOND = 1000;
    private static VideoSessionController sInstance;
    private InCallPresenter mInCallPresenter;
    private DialerCall mPrimaryCall;
    private AutoDeclineTimer mAutoDeclineTimer = new AutoDeclineTimer();

    //M:the event to start timer of cancel upgrade
    public static final int SESSION_EVENT_NOTIFY_START_TIMER_20S = 1013;

    private VideoSessionController() {
        // do nothing
    }

    /**
     * M: get the VideoSessionController instance.
     * @return the instance.
     */
    public static VideoSessionController getInstance() {
        if (sInstance == null) {
            sInstance = new VideoSessionController();
        }
        return sInstance;
    }

    /**
     * M: setup when InCallPresenter setUp.
     * @param inCallPresenter the InCallPresenter instance.
     */
    public void setUp(InCallPresenter inCallPresenter) {
        logd("setUp");
        if (inCallPresenter == null) {
          return;
        }
        mInCallPresenter = inCallPresenter;
        mInCallPresenter.addListener(this);
        mInCallPresenter.addIncomingCallListener(this);
    }

    /**
     * M: tearDown when InCallPresenter tearDown.
     */
    public void tearDown() {
        logd("tearDown...");
        mInCallPresenter.removeListener(this);
        mInCallPresenter.removeIncomingCallListener(this);

        clear();
    }

    /**
     * M: get the countdown second number.
     * @return countdown number.
     */
    public long getAutoDeclineCountdownSeconds() {
        return mAutoDeclineTimer.getAutoDeclineCountdown();
    }

    @Override
    public void onStateChange(InCallPresenter.InCallState oldState,
                              InCallPresenter.InCallState newState, CallList callList) {
        DialerCall call;
        if (newState == InCallPresenter.InCallState.INCOMING) {
            call = callList.getIncomingCall();
        } else if (newState == InCallPresenter.InCallState.WAITING_FOR_ACCOUNT) {
            call = callList.getWaitingForAccountCall();
        } else if (newState == InCallPresenter.InCallState.PENDING_OUTGOING) {
            call = callList.getPendingOutgoingCall();
        } else if (newState == InCallPresenter.InCallState.OUTGOING) {
            call = callList.getOutgoingCall();
        } else {
            call = callList.getActiveOrBackgroundCall();
        }

        if (!DialerCall.areSame(call, mPrimaryCall)) {
            onPrimaryCallChanged(call);
        }
    }

    @Override
    public void onIncomingCall(InCallPresenter.InCallState oldState,
                               InCallPresenter.InCallState newState, DialerCall call) {
        if (!DialerCall.areSame(call, mPrimaryCall)) {
            onPrimaryCallChanged(call);
        }
    }

    /**
     * M: When upgrade request received, start timing.
     * @param call the call upgrading.
     */
    public void startTimingForAutoDecline(DialerCall call) {
        logi("[startTimingForAutoDecline] for call: " + getId(call));
        if (!DialerCall.areSame(call, mPrimaryCall)) {
            Log.e(this, "[startTimingForAutoDecline]Abnormal case for a non-primary call " +
                    "receiving upgrade request.");
            onPrimaryCallChanged(call);
        }
        if (call.getVideoFeatures().isSupportAutoDeclineUpgradeRequest()) {
            mAutoDeclineTimer.startTiming();
        }
    }

    /**
     * M: stop timing when the request accepted or declined.
     */
    public void stopTiming() {
        mAutoDeclineTimer.stopTiming();
    }

    private void onPrimaryCallChanged(DialerCall call) {
        logi("[onPrimaryCallChanged] " + getId(mPrimaryCall) + " -> " + getId(call));
        if (call != null && mPrimaryCall != null && mPrimaryCall.getVideoTech().
            getSessionModificationState()
                == SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            /**
             * force decline upgrade request if primary call changed.
             */
            mInCallPresenter.declineUpgradeRequest(mInCallPresenter.getContext());
        }
        mPrimaryCall = call;
    }

    private void clear() {
        mInCallPresenter = null;
        // when mInCallPresenter is null ,eg peer disconnect call,
        // local should stop timer.
        stopTiming();
    }

    private void logd(String msg) {
        if (DEBUG) {
            Log.d(this, msg);
        }
    }

    private void logw(String msg) {
        if (DEBUG) {
            Log.w(this, msg);
        }
    }

    private void logi(String msg) {
        Log.i(this, msg);
    }

    private static String getId(DialerCall call) {
        return call == null ? "null" : call.getId();
    }

    public void onDowngradeToAudio(DialerCall call) {
        logd("[onDowngradeToAudio]for callId: " + getId(call));
        if (call == null) {
            logw("onDowngradeToAudio the current call is nul");
            return;
        }
        //reset hide preview flag
        call.setHidePreview(false);

         //show message when downgrade to voice
        // move this toast to Framework layer to show for cover more cases of video call
        //downgrade on OP01 project.ex:SRVCC,Change to voice call in call dailing,etc. @{
        if (ExtensionManager.getVideoCallExt().showToastForDowngrade()) {
          InCallPresenter.getInstance().showMessage(
              com.android.incallui.R.string.video_call_downgrade_to_voice_call);
        } else {
          logd("[onDowngradeToAudio] not show downgrade toast in here");
        }
        /// @}
        ExtensionManager.getInCallExt().maybeDismissBatteryDialog();
    }

    /**
     * M: Timer to countdown.
     */
    private class AutoDeclineTimer {
        private int mCountdownSeconds = DEFAULT_COUNT_DOWN_SECONDS;
        private CallTimer mTimer;
        private long mTimingStartMillis;
        private long mRemainSecondsBeforeDecline = -1;

        AutoDeclineTimer() {
            mTimer = new CallTimer(new Runnable() {
                @Override
                public void run() {
                    updateCountdown();
                }
            });
        }

        public void startTiming() {
            //TODO: customer might need some other value for this.
            ///M: change timer value for AT&T
            mCountdownSeconds = ExtensionManager.getVideoCallExt().getDeclineTimer();
            /// M: CMCC upgrade with precondition needs differenct timer.the timer of UE that sends
            /// upgrade request is 20 seconds.the timer of UE that receives upgrade request is 15
            ///seconds. @{
            if (mPrimaryCall != null && mPrimaryCall.getVideoTech().
                    getSessionModificationState() ==
                    SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE
                    && mPrimaryCall.getVideoFeatures().supportsCancelUpgradeVideo()) {
                mCountdownSeconds = COUNT_DOWN_SECONDS_FOR_RECEVICE_UPGRADE_WITH_PRECONDITION;
            }
            ///@}
            mRemainSecondsBeforeDecline = mCountdownSeconds;
            mTimingStartMillis = SystemClock.uptimeMillis();
            mTimer.start(MILLIS_PER_SECOND);
        }

        public long getAutoDeclineCountdown() {
            return mRemainSecondsBeforeDecline;
        }

        public void stopTiming() {
            mTimer.cancel();
            mRemainSecondsBeforeDecline = -1;
        }

        private void updateCountdown() {
            long currentMillis = SystemClock.uptimeMillis();
            long elapsedSeconds = (currentMillis - mTimingStartMillis) / MILLIS_PER_SECOND;
            if (elapsedSeconds > mCountdownSeconds) {
                if(mInCallPresenter == null) {
                    logd("[updateCountdown]mInCallPresenter is null return");
                    return;
                }

                //M: When call is in cancel progress, the timeout requires send cancel
                /// upgrade request out. @{
                if (mPrimaryCall != null && mPrimaryCall.getVideoTech().
                        getSessionModificationState() ==
                        SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE ) {
                     mInCallPresenter.cancelUpgradeRequest(mInCallPresenter.getContext());
               ///@}
                } else {
                mInCallPresenter.declineUpgradeRequest(mInCallPresenter.getContext());
                }
            } else {
                mRemainSecondsBeforeDecline = mCountdownSeconds - elapsedSeconds;
                /// M: When call is in cancel progress, it doesn't need to update UI. @{
                if (mPrimaryCall != null &&
                        mPrimaryCall.getVideoTech().getSessionModificationState() !=
                        SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
                    logd("[updateCountdown]it didn't need show updateUI" );
                    return;
                }
                ///@}
                updateRelatedUi();
            }
        }

        private void updateRelatedUi() {
            logd("[updateRelatedUi]remain seconds: " + mRemainSecondsBeforeDecline);
            if(mInCallPresenter == null) {
                logd("[updateRelatedUi]mInCallPresenter is null return");
                return;
            }
            mInCallPresenter.onAutoDeclineCountdownChanged();
        }
    }
}
