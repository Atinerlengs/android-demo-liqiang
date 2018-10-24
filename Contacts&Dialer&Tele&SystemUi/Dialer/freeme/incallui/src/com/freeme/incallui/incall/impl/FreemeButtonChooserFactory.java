package com.freeme.incallui.incall.impl;

import android.support.v4.util.ArrayMap;
import android.telephony.TelephonyManager;

import com.android.incallui.incall.impl.ButtonChooser;
import com.android.incallui.incall.impl.MappedButtonConfig;
import com.android.incallui.incall.protocol.InCallButtonIds;

import java.util.Map;

/**
 * Creates {@link ButtonChooser} objects, based on the current network and phone type.
 */
public class FreemeButtonChooserFactory {
    /**
     * Creates the appropriate {@link ButtonChooser} based on the given information.
     *
     * @param voiceNetworkType the result of a call to {@link TelephonyManager#getVoiceNetworkType()}.
     * @param isWiFi           {@code true} if the call is made over WiFi, {@code false} otherwise.
     * @param phoneType        the result of a call to {@link TelephonyManager#getPhoneType()}.
     * @return the ButtonChooser.
     */
    public static ButtonChooser newButtonChooser(
            int voiceNetworkType, boolean isWiFi, int phoneType) {
        if (voiceNetworkType == TelephonyManager.NETWORK_TYPE_LTE || isWiFi) {
            return newImsAndWiFiButtonChooser();
        }

        if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            return newCdmaButtonChooser();
        }

        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            return newGsmButtonChooser();
        }

        return newImsAndWiFiButtonChooser();
    }

    private static ButtonChooser newImsAndWiFiButtonChooser() {
        Map<Integer, MappedButtonConfig.MappingInfo> mapping = createCommonMapping();
        mapping.put(InCallButtonIds.BUTTON_HOLD,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(0).build());
        mapping.put(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(1).build());
        mapping.put(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO,
                MappedButtonConfig.MappingInfo.builder(3).build());

        return new ButtonChooser(new MappedButtonConfig(mapping));
    }

    private static ButtonChooser newCdmaButtonChooser() {
        Map<Integer, MappedButtonConfig.MappingInfo> mapping = createCommonMapping();
        /// M: Support CDMA hold call for DSDA project. @{
        mapping.put(InCallButtonIds.BUTTON_HOLD,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(0).build());
        /// @
        mapping.put(InCallButtonIds.BUTTON_SWAP,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(1).build());
        mapping.put(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(Integer.MAX_VALUE)
                        .setMutuallyExclusiveButton(InCallButtonIds.BUTTON_SWAP).build());
        mapping.put(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO,
                MappedButtonConfig.MappingInfo.builder(3).build());

        return new ButtonChooser(new MappedButtonConfig(mapping));
    }

    private static ButtonChooser newGsmButtonChooser() {
        Map<Integer, MappedButtonConfig.MappingInfo> mapping = createCommonMapping();

        mapping.put(InCallButtonIds.BUTTON_HOLD,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(0).build());
        mapping.put(InCallButtonIds.BUTTON_SWITCH_TO_SECONDARY,
                MappedButtonConfig.MappingInfo.builder(1).setSlotOrder(1).build());
        mapping.put(InCallButtonIds.BUTTON_UPGRADE_TO_VIDEO,
                MappedButtonConfig.MappingInfo.builder(3).build());
        return new ButtonChooser(new MappedButtonConfig(mapping));
    }

    private static Map<Integer, MappedButtonConfig.MappingInfo> createCommonMapping() {
        Map<Integer, MappedButtonConfig.MappingInfo> mapping = new ArrayMap<>();
        mapping.put(InCallButtonIds.BUTTON_ADD_CALL,
                MappedButtonConfig.MappingInfo.builder(2).setSlotOrder(0).build());
        mapping.put(InCallButtonIds.BUTTON_MERGE,
                MappedButtonConfig.MappingInfo.builder(2).setSlotOrder(1).build());
        mapping.put(InCallButtonIds.BUTTON_MUTE,
                MappedButtonConfig.MappingInfo.builder(4).build());
        mapping.put(InCallButtonIds.BUTTON_RECORD_NUMBER,
                MappedButtonConfig.MappingInfo.builder(5).build());

        /// M: Extend button mapping @{
        addMtkCommonMapping(mapping);
        /// @}
        return mapping;
    }

    /**
     * M: Extend the button mapping
     *
     * @param mapping
     */
    private static void addMtkCommonMapping(Map<Integer, MappedButtonConfig.MappingInfo> mapping) {
        // [Voice Record]
        mapping.put(InCallButtonIds.BUTTON_SWITCH_VOICE_RECORD,
                MappedButtonConfig.MappingInfo.builder(0).build());
    }
}
