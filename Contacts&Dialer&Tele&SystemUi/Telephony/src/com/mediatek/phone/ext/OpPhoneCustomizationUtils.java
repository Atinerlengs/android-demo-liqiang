package com.mediatek.phone.ext;

import android.content.Context;

import com.mediatek.common.util.OperatorCustomizationFactoryLoader;
import com.mediatek.common.util.OperatorCustomizationFactoryLoader.OperatorFactoryInfo;

import java.util.ArrayList;
import java.util.List;

public class OpPhoneCustomizationUtils {

    // list every operator's factory path and name.
    private static final List<OperatorFactoryInfo> sOperatorFactoryInfoList
            = new ArrayList<OperatorFactoryInfo>();

    static OpPhoneCustomizationFactoryBase sFactory = null;

    static {
        /*sOperatorFactoryInfoList.add(new OperatorFactoryInfo("OP01Dialer.apk",
                "com.mediatek.op01.dialer.Op01DialerCustomizationFactory", null, "OP01"));*/
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP01TeleService.apk", //apk name
                "com.mediatek.op01.phone.plugin.Op01PhoneCustomizationFactory", //factory class name
                "com.mediatek.op01.phone.plugin", //apk's package name
                "OP01")); //operator id
                          //seg,OP09 has two customizations, need to set operator segment
       sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP02TeleService.apk",    // apk name
                         "com.mediatek.phone.op02.plugin.Op02PhoneCustomizationFactory",
                                                                           // factory class
                         "com.mediatek.phone.op02.plugin",                 // apk's package
                         "OP02"                                            // operator id
                                                                           // operator segment
                     ));
       sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP03TeleService.apk",    // apk name
                         "com.mediatek.op03.phone.Op03PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op03.phone",                 // apk's package
                         "OP03"                                            // operator id
                                                                           // operator segment
                     ));
       sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP05TeleService.apk",    // apk name
                         "com.mediatek.op05.phone.Op05PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op05.phone",                 // apk's package
                         "OP05"                                            // operator id
                                                                           // operator segment
                )
       );
       sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP06TeleService.apk",    // apk name
                         "com.mediatek.op06.phone.Op06PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op06.phone",                 // apk's package
                         "OP06"                                            // operator id
                                                                           // operator segment
                )
       );
       sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP07TeleService.apk",    // apk name
                         "com.mediatek.op07.phone.OP07PhoneCustomizationFactory", // factory class
                         "com.mediatek.op07.phone",                            // apk's package name
                         "OP07",                                           // operator id
                         "SEGDEFAULT"// operator segment
                     ));
       sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP08TeleService.apk",    // apk name
                         "com.mediatek.op08.phone.Op08PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op08.phone",                        // apk's package
                         "OP08"                                            // operator id
                                                                           // operator segment
                )
        );
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP09TeleService.apk",    // apk name
                         "com.mediatek.phone.op09.plugin.Op09PhoneCustomizationFactory",
                                                                              // factory class
                         "com.mediatek.phone.op09.plugin",                    // apk's package name
                         "OP09",                                              // operator id
                         "SEGDEFAULT"                                         // operator segment
                     ));
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP09ClibTeleService.apk",    // apk name
                         "com.mediatek.phone.op09Clib.plugin.Op09ClibPhoneCustomizationFactory",
                                                                              // factory class name
                         "com.mediatek.phone.op09Clib.plugin",                // apk's package name
                         "OP09",                                              // operator id
                         "SEGC"                                         // operator segment
                     ));
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP11TeleService.apk",    // apk name
                         "com.mediatek.op11.phone.Op11PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op11.phone",                 // apk's package
                         "OP11"                                            // operator id
                                                                           // operator segment
                )
        );
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP12TeleService.apk",    // apk name
                         "com.mediatek.op12.phone.Op12PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op12.phone",                 // apk's package
                         "OP12"                                            // operator id
                                                                           // operator segment
                )
        );
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP15TeleService.apk",    // apk name
                         "com.mediatek.op15.phone.Op15PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op15.phone",                 // apk's package
                         "OP15"                                            // operator id
                                                                           // operator segment
                )
        );
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP16TeleService.apk",    // apk name
                         "com.mediatek.op16.phone.Op16PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op16.phone",                 // apk's package
                         "OP16"                                            // operator id
                                                                           // operator segment
                )
        );
        sOperatorFactoryInfoList.add(
                new OperatorFactoryInfo("OP18TeleService.apk",    // apk name
                         "com.mediatek.op18.phone.Op18PhoneCustomizationFactory",
                                                                           // factory class name
                         "com.mediatek.op18.phone",                 // apk's package
                         "OP18"                                            // operator id
                                                                           // operator segment
                )
        );
    }

    public static synchronized OpPhoneCustomizationFactoryBase getOpFactory(Context context) {
        if (sFactory == null) {

            sFactory = (OpPhoneCustomizationFactoryBase) OperatorCustomizationFactoryLoader
                        .loadFactory(context, sOperatorFactoryInfoList);
            if (sFactory == null) {
                sFactory = new OpPhoneCustomizationFactoryBase();
            }
        }
        return sFactory;
    }
}
