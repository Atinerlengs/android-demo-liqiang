package com.mediatek.dialer.compat;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

/**
 * Compatibility utility class about TelecomManager.
 */
public class TelecomCompat {
  //For [VoLTE ConfCall] @{
  /**
   * refer to mediatek.telecom.MtkTelecomManager in O
   * Optional extra for {@link android.content.Intent#ACTION_CALL} and
   * {@link android.content.Intent#ACTION_CALL_PRIVILEGED} containing a phone
   * number {@link ArrayList} that used to launch the volte conference call.
   * The phone number in the list may be normal phone number, sip phone
   * address or IMS call phone number. This extra takes effect only when the
   * {@link #EXTRA_START_VOLTE_CONFERENCE} is true.
   * @hide
   */
  public static final String EXTRA_VOLTE_CONFERENCE_NUMBERS_O =
          "mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS";

  /**
   * refer to mediatek.telecom.MtkTelecomManager in O
   * Optional extra for {@link android.content.Intent#ACTION_CALL} and
   * {@link android.content.Intent#ACTION_CALL_PRIVILEGED} containing an
   * boolean value that determines if it should launch a volte conference
   * call.
   * @hide
   */
  public static final String EXTRA_START_VOLTE_CONFERENCE_O =
          "mediatek.telecom.extra.EXTRA_START_VOLTE_CONFERENCE";

  /**
   * refer to com.mediatek.telecom.TelecomManagerEx in N
   * Optional extra for {@link android.content.Intent#ACTION_CALL} and
   * {@link android.content.Intent#ACTION_CALL_PRIVILEGED} containing a phone
   * number {@link ArrayList} that used to launch the volte conference call.
   * The phone number in the list may be normal phone number, sip phone
   * address or IMS call phone number. This extra takes effect only when the
   * {@link #EXTRA_VOLTE_CONF_CALL_DIAL} is true.
   * @hide
   */
  public static final String EXTRA_VOLTE_CONF_CALL_NUMBERS_N =
          "com.mediatek.volte.ConfCallNumbers";
  /**
   * refer to com.mediatek.telecom.TelecomManagerEx in N
   * Optional extra for {@link android.content.Intent#ACTION_CALL} and
   * {@link android.content.Intent#ACTION_CALL_PRIVILEGED} containing an
   * boolean value that determines if it should launch a volte conference
   * call.
   * @hide
   */
  public static final String EXTRA_VOLTE_CONF_CALL_DIAL_N = "com.mediatek.volte.ConfCallDial";
  public static final String EXTRA_VOLTE_CONF_CALL_NUMBERS;
  public static final String EXTRA_VOLTE_CONF_CALL_DIAL;
  static {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      EXTRA_VOLTE_CONF_CALL_NUMBERS = EXTRA_VOLTE_CONFERENCE_NUMBERS_O;
      EXTRA_VOLTE_CONF_CALL_DIAL = EXTRA_START_VOLTE_CONFERENCE_O;
    } else {
      EXTRA_VOLTE_CONF_CALL_NUMBERS = EXTRA_VOLTE_CONF_CALL_NUMBERS_N;
      EXTRA_VOLTE_CONF_CALL_DIAL = EXTRA_VOLTE_CONF_CALL_DIAL_N;
    }
  }

  public class PhoneAccountCompat {
    private static final int CUSTOM_CAPABILITY_BASE = 0x8000;
    /**
     * TODO:wait PhoneAccount
     * M: refer to android.telecom.PhoneAccount.CAPABILITY_VOLTE_CONFERENCE_ENHANCED in N
     * Flag indicating that this {@code PhoneAccount} is capable of placing a volte conference
     * call at a time. This flag will be set only when the IMS service camped on the IMS
     * server and and the following features are available on the Network:
     * 1. Launch a conference with multiple participants at a time
     * 2. TBD
     * <p>
     * See {@link #getCapabilities()}
     * @hide
     */
    public static final int CAPABILITY_VOLTE_CONFERENCE_ENHANCED = CUSTOM_CAPABILITY_BASE << 2;
  }
  //@}
}
