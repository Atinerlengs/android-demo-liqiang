package com.mediatek.dialer.compat;

public class CallLogCompat {
  /**
   * Compatibility utility class about android.provider.CallLog.Calls
   */
  public static class CallsCompat {
    /**
     * refer to CallLog.Calls.CONFERENCE_CALL_ID
     * save conference call id of a call log in a conference call
     * @hide
     */
    public static final String CONFERENCE_CALL_ID = "conference_call_id";

    /**
     * refer to CallLog.Calls.CACHED_INDICATE_PHONE_SIM
     * An opaque value that indicate contact store location. "-1", indicates
     * phone contacts, others, indicate sim id of a sim contact
     * @hide
     */
    public static final String CACHED_INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";

    /**
     * refer to CallLog.Calls.CACHED_IS_SDN_CONTACT
     * For SIM contact's flag, SDN's contacts value is 1, ADN's contacts value
     * is 0 card.
     * @hide
     */
    public static final String CACHED_IS_SDN_CONTACT = "is_sdn_contact";

    /**
     * refer to CallLog.Calls.SORT_DATE
     * the projection of calls date or conference call date
     * @hide
     */
    public static final String SORT_DATE = "sort_date";
  }
}
