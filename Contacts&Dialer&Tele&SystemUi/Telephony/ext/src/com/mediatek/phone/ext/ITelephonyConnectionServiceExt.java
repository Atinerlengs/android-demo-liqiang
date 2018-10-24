package com.mediatek.phone.ext;

import android.util.Log;
import java.util.ArrayList;

/**
 * Telephony connection service extension plugin for op09.
*/
public interface ITelephonyConnectionServiceExt {

    /**
     * Customize strings which contains 'SIM', replace 'SIM' by 'UIM' etc.
     * @param stringList string list
     * @param slotId slot id
     * @return new string list
     */
    public ArrayList<String> customizeSimDisplayString(ArrayList<String> stringList, int slotId);
}
