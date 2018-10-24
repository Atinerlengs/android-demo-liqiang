package com.freeme.systemui.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

public class SimCardMethod {
    public static boolean hasIccCard(TelephonyManager telePhonyManager, Context context) {
        if (telePhonyManager == null) {
            telePhonyManager = TelephonyManager.from(context);
        }
        if (isMulityCard(context) && (isCardPresent(telePhonyManager, 0) || isCardPresent(telePhonyManager, 1))) {
            return true;
        }
        return telePhonyManager.hasIccCard();
    }

    public static boolean isCardPresent(TelephonyManager telePhonyManager, int slot) {
        int slotState = telePhonyManager.getSimState(slot);
        return (slotState == 2 || slotState == 3 || slotState == 4 || slotState == 5);
    }

    public static boolean isMulityCard(Context context) {
        return TelephonyManager.from(context).isMultiSimEnabled();
    }
}
