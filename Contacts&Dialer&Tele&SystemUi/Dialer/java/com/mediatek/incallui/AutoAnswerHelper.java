package com.mediatek.incallui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;

import com.android.dialer.common.LogUtil;
import com.android.incallui.call.CallList;
import com.android.incallui.call.DialerCall;
import com.android.incallui.InCallPresenter;
import com.android.incallui.StatusBarNotifier;
import com.android.incallui.Log;

/**
 * M: for EngineerMode testing.
 */
public class AutoAnswerHelper implements InCallPresenter.IncomingCallListener {

    private static final long AUTO_ANSWER_DELAY_MILLIS = 5 * 1000;
    private Context mContext;
    private Handler mAutoAnswerHandler = new AutoAnswerHandler();

    /**
     * Constructor.
     * @param context the context is required for the application context.
     */
    public AutoAnswerHelper(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onIncomingCall(InCallPresenter.InCallState oldState,
                               InCallPresenter.InCallState newState, DialerCall call) {
        if (isEnabled()) {
            log("would answer the call in a few seconds: " + call.getId());
            Message msg = mAutoAnswerHandler.obtainMessage(0, call);
            mAutoAnswerHandler.sendMessageDelayed(msg, AUTO_ANSWER_DELAY_MILLIS);
        }
    }

    /**
     * The property persist.auto_answer_incoming_call has 2 users,
     * one is InCallUI, the other is the EngineerMode.
     * @return true if auto answer enabled.
     */
    private boolean isEnabled() {
        return SystemProperties.getInt("persist.auto_answer", -1) > 0;
    }

    /**
     * M: Handler to delay auto answer.
     */
    private class AutoAnswerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            DialerCall call = (DialerCall) msg.obj;
            if (call != null && DialerCall.State.isIncomingOrWaiting(call.getState())) {
                log("answer incoming call as: " + call.getVideoState());
                answerIncomingCall(mContext, call.getVideoState());
            }
        }
    }

    private static void log(String msg) {
        Log.i(AutoAnswerHelper.class.getSimpleName(), msg);
    }

    private void answerIncomingCall(Context context, int videoState) {
      CallList callList = InCallPresenter.getInstance().getCallList();
      if (callList == null) {
        StatusBarNotifier.clearAllCallNotifications(context);
        LogUtil.e("NotificationBroadcastReceiver.answerIncomingCall", "call list is empty");
      } else {
        DialerCall call = callList.getIncomingCall();
        if (call != null) {
          /// M: [log optimize]
          Log.op(call, Log.CcOpAction.ANSWER, "answer via AutoAnswer: "
                  + videoState + ",showInCall with CallId:" + call.getId());
          call.answer(videoState);
          InCallPresenter.getInstance()
              .showInCall(false /* showDialpad */, false /* newOutgoingCall */);
        }
      }
    }
}
