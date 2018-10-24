package com.mediatek.server.telecom.ext;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader.OperatorFactoryInfo;

public class CommonTelecomCustomizationUtils {
    private static final String TAG = "CommonTelecomCustomizationUtils";

    private static final List<OperatorFactoryInfo> sOperatorFactoryInfoList
            = new ArrayList<OperatorFactoryInfo>();
    private static CommonTelecomCustomizationFactoryBase sFactory;
    static {
        if (isOpFactoryLoaderAvailable()) {
            sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo(
                        "OPTelecomCommon.apk",
                        "com.mediatek.op.telecom.CommonTelecomCustomizationFactory",
                        "com.mediatek.op.telecom",
                        null));
        }
    };

    public static synchronized CommonTelecomCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null && isOpFactoryLoaderAvailable()) {
            sFactory = (CommonTelecomCustomizationFactoryBase)
                OperatorCustomizationFactoryLoader.loadFactory(context, sOperatorFactoryInfoList);
        }
        if (sFactory == null) {
            Log.i(TAG, "return default CommonTelecomCustomizationFactoryBase");
            sFactory = new CommonTelecomCustomizationFactoryBase();
        }
        return sFactory;
    }

    private static boolean isOpFactoryLoaderAvailable() {
        try {
            Class.forName("com.mediatek.common.util.OperatorCustomizationFactoryLoader");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
