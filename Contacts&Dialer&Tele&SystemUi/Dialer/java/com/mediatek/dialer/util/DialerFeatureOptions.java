package com.mediatek.dialer.util;

import com.mediatek.dialer.compat.DialerCompatEx;

import android.os.SystemProperties;

public class DialerFeatureOptions {

    // [IP Dial] IP call prefix.
    public static final boolean IP_PREFIX = false;
    // [Union Query] this feature will make a union query on Calls table and data view
    // while query the call log. So that the query result would contain contacts info.
    // and no need to query contacts info again in CallLogAdapter. It improve the call
    // log performance.
    public static final boolean CALL_LOG_UNION_QUERY = false;

    // [Multi-Delete] Support delete the multi-selected call logs
    public static final boolean MULTI_DELETE = true;

    // [Suggested Account] if true, support feature "Suggested Account",
    // otherwise not support.
    public static final boolean MTK_SUPPORT_SUGGESTED_ACCOUNT = false;

    // [Dialer Global Search] Support search call log from quick search box.
    public static final boolean DIALER_GLOBAL_SEARCH = true;

    // For dynamic control the test case
    public static boolean sIsRunTestCase = false;

    /**
     * [CallLog Incoming and Outgoing Filter]
     * Whether the callLog incoming and outgoing filter is enabled or not.
     * @return true if the callLog incoming and outgoing filter feature is enabled.
     */
    public static boolean isCallLogIOFilterEnabled() {
        String operatorSpec = SystemProperties.get("persist.operator.optr", "");
        String operatorSeg = SystemProperties.get("persist.operator.seg", "");
        boolean isCtOperatorA = operatorSpec.equals("OP09") && operatorSeg.equals("SEGDEFAULT");
        boolean isCtOperatorC = operatorSpec.equals("OP09") && operatorSeg.equals("SEGC");
        // Return true on OP09 or OP02 mode
        if (operatorSpec.equals("OP02") || isCtOperatorA || isCtOperatorC) {
            return true;
        }
        return false;
    }

    /**
     * [MTK Dialer Search] whether DialerSearch feature enabled on this device
     * @return ture if allowed to enable
     */
    public static boolean isDialerSearchEnabled() {
        return true;
    }

    public static final boolean MTK_IMS_SUPPORT = SystemProperties.get(
            "persist.mtk_ims_support").equals("1");
    public static final boolean MTK_VOLTE_SUPPORT = SystemProperties.get(
            "persist.mtk_volte_support").equals("1");
    //[VoLTE ConfCall] Whether the VoLTE enhanced conference call (Launch
    //conference call directly from dialer) supported.
    public static final boolean MTK_ENHANCE_VOLTE_CONF_CALL = true;
    // Add for auto testing
    public static boolean sEnableVolteConfForTest = false;
    public static void setVolteConfForTest(boolean enable) {
        sEnableVolteConfForTest = enable;
    }

    /**
     * [VoLTE ConfCall] Whether the VoLTE enhanced conference call (Launch
     * conference call directly from dialer) supported.
     *
     * @return true if the VoLTE enhanced conference call supported
     */
    public static boolean isVolteEnhancedConfCallSupport() {
        if (sEnableVolteConfForTest) {
            return true;
        } else {
            return MTK_ENHANCE_VOLTE_CONF_CALL && MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT
                    && DialerCompatEx.isVolteEnhancedConfCallCompat();
        }
    }

    //[VoLTE ConfCallLog] Whether the VoLTE conference calLog supported.
    public static final boolean MTK_VOLTE_CONFERENCE_CALLLOG = true;
    /**
     * [VoLTE ConfCallLog] Whether the conference calLog supported.
     *
     * @return true if the conference calLog supported
     */
    public static boolean isVolteConfCallLogSupport() {
        return MTK_VOLTE_CONFERENCE_CALLLOG
                && DialerCompatEx.isConferenceCallLogCompat();
    }
//
//    /**
//     * [IMS Call] Whether the IMS call supported
//     * @return true if the IMS call supported
//     */
//    public static boolean isImsCallSupport() {
//        return MTK_IMS_SUPPORT && MTK_VOLTE_SUPPORT;
//    }

//    /**
//     * Whether Android one project enable
//     * @return true if is Android one project
//     */
//    public static boolean isA1ProjectEnabled() {
//        return SystemProperties.get("ro.mtk_a1_feature").equals("1");
//    }

    /**
     * Whether the Light cust support is supported
     * @return true if the Light cust supported
     */
    public static boolean isOpLightCustSupport() {
        return SystemProperties.get("ro.cmcc_light_cust_support").equals("1") &&
                SystemProperties.get("ro.mtk_c2k_support").equals("1");
    }

    private static final boolean SIM_CONTACTS_FEATURE_OPTION = true;
    /**
     * [MTK SIM Contacts feature] Whether the SIM contact indicator support supported
     * @return true if the SIM contact indicator supported
     */
    public static boolean isSimContactsSupport() {
        return SIM_CONTACTS_FEATURE_OPTION && DialerCompatEx.isSimContactsCompat();
    }

    //*/ freeme.zhaozehong, 20180320. for freemeOS, UI redesign
    public static boolean isCdmaSupport() {
        return "1".equals(SystemProperties.get("ro.mtk_c2k_support"));
    }
    //*/
}
