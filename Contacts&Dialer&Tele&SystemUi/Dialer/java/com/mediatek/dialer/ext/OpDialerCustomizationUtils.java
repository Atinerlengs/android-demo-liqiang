package com.mediatek.dialer.ext;

import android.content.Context;

import com.android.dialer.common.LogUtil;
import com.android.dialer.compat.CompatUtils;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader.OperatorFactoryInfo;

import java.util.ArrayList;
import java.util.List;

public class OpDialerCustomizationUtils {
    private static final String PLUGIN_CLASS =
            "com.mediatek.common.util.OperatorCustomizationFactoryLoader";

    // list every operator's factory path and name.
    private static final List<OperatorFactoryInfo> sOperatorFactoryInfoList
            = new ArrayList<OperatorFactoryInfo>();

    private static final String TAG = "OpDialerCustomizationUtils";

    static OpDialerCustomizationFactoryBase sFactory = null;

    static {
      if (supportPlugin()) {
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP03Dialer.apk",
            "com.mediatek.op03.dialer.Op03DialerCustomizationFactory", "com.mediatek.op03.dialer",
            "OP03"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP07Dialer.apk",
            "com.mediatek.op07.dialer.Op07DialerCustomizationFactory", "com.mediatek.op07.dialer",
            "OP07"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP08Dialer.apk",
            "com.mediatek.op08.dialer.Op08DialerCustomizationFactory", "com.mediatek.op08.dialer",
            "OP08"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP18Dialer.apk",
            "com.mediatek.op18.dialer.Op18DialerCustomizationFactory", "com.mediatek.op18.dialer",
            "OP18"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP01Dialer.apk",
            "com.mediatek.op01.dialer.Op01DialerCustomizationFactory", "com.mediatek.op01.dialer",
            "OP01"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP02Dialer.apk",
            "com.mediatek.op02.dialer.Op02DialerCustomizationFactory", "com.mediatek.op02.dialer",
            "OP02"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP09Dialer.apk",
            "com.mediatek.op09.dialer.Op09DialerCustomizationFactory", "com.mediatek.op09.dialer",
            "OP09", "SEGDEFAULT"));
        sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP09ClibDialer.apk",
            "com.mediatek.op09clib.dialer.Op09ClibDialerCustomizationFactory",
            "com.mediatek.op09clib.dialer", "OP09", "SEGC"));
      }
    }

    public static synchronized OpDialerCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {
            if (supportPlugin()) {
                sFactory = (OpDialerCustomizationFactoryBase) OperatorCustomizationFactoryLoader
                        .loadFactory(context, sOperatorFactoryInfoList);
            }
            LogUtil.d(TAG, "try to load op factory, sFactory=" + sFactory);
            if (sFactory == null) {
                sFactory = new OpDialerCustomizationFactoryBase();
            }
        }
        return sFactory;
    }

  public static boolean supportPlugin() {
    return CompatUtils.isClassAvailable(PLUGIN_CLASS);
  }
}