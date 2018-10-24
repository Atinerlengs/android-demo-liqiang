package com.mediatek.incallui.video;

import android.content.Context;
import android.os.PersistableBundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.dialer.common.LogUtil;
import com.android.incallui.call.DialerCall;
import com.android.incallui.call.CallList;
import com.android.incallui.InCallPresenter;

import java.util.Objects;

import mediatek.telephony.MtkCarrierConfigManager;

/**
 * M: management for video call features.
 */
public class VideoFeatures {
    private final DialerCall mCall;
    private CarrierConfigManager mCarrierConfigManager;
    /**
     * M: [video call]for management of video call features.
     *
     * @param call the call associate with current VideoFeatures instance.
     */
    public VideoFeatures(DialerCall call) {
        mCall = call;
    }

    /**
     * M: whether this call supports rotation.
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsRotation() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports downgrade.
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsDowngrade() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports answer as voice.
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsAnswerAsVoice() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports pause (turn off camera).
     * make sure this is a video call before checking this feature.
     *
     * @return
     */
    public boolean supportsPauseVideo() {
        return !isCsCall()
                && isContainCarrierConfig(
                    MtkCarrierConfigManager.MTK_KEY_ALLOW_ONE_WAY_VIDEO_BOOL);
    }

    /**
     * M: whether this video call supports reject call by SMS .
     * make sure this is a video call before checking this feature.
     *
     * @return true if support.
     */
    public boolean supportsRejectVideoCallBySms() {
        return !isCsCall()
                && isContainCarrierConfig(
                    MtkCarrierConfigManager.MTK_KEY_ALLOW_ONE_VIDEO_CALL_ONLY_BOOL);
    }

    /**
     * M: whether this call supports cancel upgrade .
     * make sure this is a video call before checking this feature.
     *
     * @return true if support
     */
    public boolean supportsCancelUpgradeVideo() {
        return !isCsCall()
                && isContainCarrierConfig(
                    MtkCarrierConfigManager.MTK_KEY_ALLOW_CANCEL_VIDEO_UPGRADE_BOOL);
    }

    /// M: Add for video feature change due to camera error @{
    private boolean mCameraErrorHappened = false;
    public void setCameraErrorHappened(boolean happen) {
        mCameraErrorHappened = happen;
    }
    /// @}

    /**
     * M: check the current call can upgrade to video call or not
     *
     * @return false if without subject call, active and hold call with the same account which
     * belongs to some operator, else depends on whether it is not a CS Call.
     */
    public boolean canUpgradeToVideoCall() {
        if (mCall == null) {
            return false;
        }

        if (isContainCarrierConfig(MtkCarrierConfigManager.
            MTK_KEY_ALLOW_ONE_VIDEO_CALL_ONLY_BOOL)) {
            //FIXME: support vilte capability when multi calls exist with different accounts.
            if (CallList.getInstance().getBackgroundCall() == null
                    || !Objects.equals(mCall.getTelecomCall().getDetails().getAccountHandle(),
                    CallList.getInstance().getBackgroundCall().getTelecomCall().getDetails()
                            .getAccountHandle())) {
                return !isCsCall() && !mCameraErrorHappened;
            } else {
                // return false if Active and Hold Calls from the same account
                return false;
            }
        }

        return !isCsCall() && !mCameraErrorHappened;
    }

    /**
     * M: Check whether it's ims(video) call or 3gvt call
     */
    public boolean isImsCall() {
        return !isCsCall();
    }

    /**
     * M: whether this call supports hold.
     * make sure this is a video call before checking this feature.
     *
     * @return
     */
    public boolean supportsHold() {
        return !isCsCall();
    }

    public boolean supportsHidePreview() {
        return !isCsCall();
    }

    private boolean isCsCall() {
        return !mCall.hasProperty(mediatek.telecom.MtkCall.MtkDetails.MTK_PROPERTY_VOLTE);
    }

  /**
   * TODO: This is bind call progress may effect the UI performance.
   * cache it ? but how to update it?
   * @param key
   * @return
   */
  public boolean isContainCarrierConfig(String key) {
    int subId = mCall.getCallSubId();
    if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
      LogUtil.d("VideoFeatures.isContainCarrierConfig", "Invaild subId");
      return false;
    }

    Context context = InCallPresenter.getInstance().getContext();
    if (mCarrierConfigManager == null) {
      mCarrierConfigManager = (CarrierConfigManager) context
          .getSystemService(Context.CARRIER_CONFIG_SERVICE);
    }

    PersistableBundle bundle = mCarrierConfigManager.getConfigForSubId(subId);
    if (bundle != null
        && bundle
            .getBoolean(key)) {
      return true;
    }
    return false;
  }

  /**
   * AOSP default not show Dialpd for video call.
   * Less effect to the AOSP UI change, add it for some special OP.
   * @return
   */
  public boolean supportShowVideoDialpad() {
    return isContainCarrierConfig(MtkCarrierConfigManager.MTK_KEY_VT_DIALPAD_SUPPORT_BOOL);
  }

  /**
   * M: whether video call over wifi is disabled or not.
   * @return true if video call over wifi is disabled, false otherwise
   */
  public boolean disableVideoCallOverWifi() {
    return isContainCarrierConfig(MtkCarrierConfigManager.MTK_KEY_DISABLE_VT_OVER_WIFI_BOOL);
  }

  /**
   * M: check if key to support auto decline of upgrade request is supported or not.
   *
   * @return true if supported, false otherwise
   */
  public boolean isSupportAutoDeclineUpgradeRequest() {
    return isContainCarrierConfig(
        MtkCarrierConfigManager.MTK_KEY_IMS_SUPPORT_AUTO_DECLINE_UPGRADE_REQUEST_BOOL);
  }

  /**
   * M: check if key to support pause video without SIP message is supported or not.
   *
   * @return true if supported, false otherwise
   */
  public boolean isSupportPauseVideoWithoutSipMessage() {
    return isContainCarrierConfig(
        MtkCarrierConfigManager.MTK_KEY_IMS_NO_SIP_MESSAGE_ON_PAUSE_VIDEO_BOOL);
  }
}
