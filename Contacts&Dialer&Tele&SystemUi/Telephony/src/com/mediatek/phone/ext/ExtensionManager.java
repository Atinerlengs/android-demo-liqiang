/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.phone.ext;


import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public final class ExtensionManager {

    private static final String LOG_TAG = "ExtensionManager";
    private static Context sContext;

    private static IAccessibilitySettingsExt sAccessibilitySettingsExt;
    private static ICallFeaturesSettingExt sCallFeaturesSettingExt;
    private static ICallForwardExt sCallForwardExt;
    private static IPhoneMiscExt sPhoneMiscExt;
    private static IMobileNetworkSettingsExt sMobileNetworkSettingsExt;
    private static INetworkSettingExt sNetworkSettingExt;
    private static IMmiCodeExt sMmiCodeExt;
    private static IEmergencyDialerExt sEmergencyDialerExt;
    private static ISsRoamingServiceExt sSsRoamingServiceExt;
    private static IDisconnectCauseExt sDisconnectCauseExt;
    private static IIncomingCallExt sIncomingCallExt;
    private static ITelephonyConnectionServiceExt sTelephonyConnectionServiceExt;
    private static IDigitsUtilExt sDigitsUtilExt;
    private static IGttInfoExt sGttInfoExt;
    private static IRttUtilExt sRttUtilExt;

    private ExtensionManager() {
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    public static void init(Application application) {
        sContext = application.getApplicationContext();
    }

    public static IAccessibilitySettingsExt getAccessibilitySettingsExt() {
        if (sAccessibilitySettingsExt == null) {
            synchronized (IAccessibilitySettingsExt.class) {
                if (sAccessibilitySettingsExt == null) {
                    sAccessibilitySettingsExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeAccessibilitySettingsExt();
                    log("[getAccessibilitySettingsExt]create ext instance: " +
                        sAccessibilitySettingsExt);
                }
            }
        }
        return sAccessibilitySettingsExt;
    }

    public static ICallFeaturesSettingExt getCallFeaturesSettingExt() {
        if (sCallFeaturesSettingExt == null) {
            synchronized (ICallFeaturesSettingExt.class) {
                if (sCallFeaturesSettingExt == null) {
                    sCallFeaturesSettingExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeCallFeaturesSettingExt();
                    log("[getCallFeaturesSettingExt]create ext instance: " +
                                 sCallFeaturesSettingExt);
                }
            }
        }
        return sCallFeaturesSettingExt;
    }


    public static ICallForwardExt getCallForwardExt() {
        if (sCallForwardExt == null) {
            synchronized (ICallForwardExt.class) {
                if (sCallForwardExt == null) {
                    sCallForwardExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeCallForwardExt();
                    log("[getCallForwardExt] create ext instance: " + sCallForwardExt);
                }
            }
        }
        return sCallForwardExt;
    }

    public static IPhoneMiscExt getPhoneMiscExt() {
        if (sPhoneMiscExt == null) {
            synchronized (IPhoneMiscExt.class) {
                if (sPhoneMiscExt == null) {
                    sPhoneMiscExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makePhoneMiscExt();
                    log("[getPhoneMiscExt] create ext instance: " + sPhoneMiscExt);
                }
            }
        }
        return sPhoneMiscExt;
    }

    public static IMobileNetworkSettingsExt getMobileNetworkSettingsExt() {
        if (sMobileNetworkSettingsExt == null) {
            synchronized (IMobileNetworkSettingsExt.class) {
                if (sMobileNetworkSettingsExt == null) {
                    sMobileNetworkSettingsExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeMobileNetworkSettingsExt();
                    log("[sMobileNetworkSettingsExt] create ext instance: "
                            + sMobileNetworkSettingsExt);
                }
            }
        }
        return sMobileNetworkSettingsExt;
    }

    public static INetworkSettingExt getNetworkSettingExt() {
        if (sNetworkSettingExt == null) {
            synchronized (INetworkSettingExt.class) {
                if (sNetworkSettingExt == null) {
                    sNetworkSettingExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeNetworkSettingExt();
                    log("[sNetworkSettingExt] create ext instance: "
                            + sNetworkSettingExt);
                }
            }
        }
        return sNetworkSettingExt;
    }

    public static IMmiCodeExt getMmiCodeExt() {
        if (sMmiCodeExt == null) {
            synchronized (IMmiCodeExt.class) {
                if (sMmiCodeExt == null) {
                    sMmiCodeExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeMmiCodeExt();
                    log("[getMmiCodeExt]create ext instance: " + sMmiCodeExt);
                }
            }
        }
        return sMmiCodeExt;
    }

    public static IEmergencyDialerExt getEmergencyDialerExt() {
        if (sEmergencyDialerExt == null) {
            synchronized (IEmergencyDialerExt.class) {
                if (sEmergencyDialerExt == null) {
                    sEmergencyDialerExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeEmergencyDialerExt();
                    log("[sEmergencyDialerExt] create ext instance: "
                            + sEmergencyDialerExt);
                }
            }
        }
        return sEmergencyDialerExt;
    }

    public static ISsRoamingServiceExt getSsRoamingServiceExt() {
        if (sSsRoamingServiceExt == null) {
            synchronized (ISsRoamingServiceExt.class) {
                if (sSsRoamingServiceExt == null) {
                    sSsRoamingServiceExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeSsRoamingServiceExt();
                    log("[sSsRoamingServiceExt] create ext instance: "
                            + sSsRoamingServiceExt);
                }
            }
        }
        return sSsRoamingServiceExt;
    }

    public static IDisconnectCauseExt getDisconnectCauseExt() {
        if (sDisconnectCauseExt == null) {
            synchronized (IDisconnectCauseExt.class) {
                if (sDisconnectCauseExt == null) {
                    sDisconnectCauseExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeDisconnectCauseExt();
                    log("[sDisconnectCauseExt] create ext instance: "
                            + sDisconnectCauseExt);
                }
            }
        }
        return sDisconnectCauseExt;
    }

    public static IIncomingCallExt getIncomingCallExt() {
        if (sIncomingCallExt == null) {
            synchronized (IIncomingCallExt.class) {
                if (sIncomingCallExt == null) {
                    sIncomingCallExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeIncomingCallExt();
                    log("[sIncomingCallExt] create ext instance: "
                            + sIncomingCallExt);
                }
            }
        }
        return sIncomingCallExt;
    }

    public static ITelephonyConnectionServiceExt getTelephonyConnectionServiceExt() {
        if (sTelephonyConnectionServiceExt == null) {
            synchronized (ITelephonyConnectionServiceExt.class) {
                if (sTelephonyConnectionServiceExt == null) {
                    sTelephonyConnectionServiceExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeTelephonyConnectionServiceExt();
                    log("[sTelephonyConnectionServiceExt] create ext instance: "
                            + sTelephonyConnectionServiceExt);
                }
            }
        }
        return sTelephonyConnectionServiceExt;
    }

    public static IDigitsUtilExt getDigitsUtilExt() {
        if (sDigitsUtilExt == null) {
            synchronized (IDigitsUtilExt.class) {
                if (sDigitsUtilExt == null) {
                    sDigitsUtilExt = OpPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeDigitsUtilExt();
                    log("[sDigitsUtilExt] create ext instance: "
                            + sDigitsUtilExt);
                }
            }
        }
        return sDigitsUtilExt;
    }

    public static IGttInfoExt getGttInfoExt() {
        if (sGttInfoExt == null) {
            synchronized (IGttInfoExt.class) {
                if (sGttInfoExt == null) {
                    sGttInfoExt = CommonPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeGttInfoExt();
                    log("[sGttInfoExt] create ext instance: "
                            + sGttInfoExt);
                }
            }
        }
        return sGttInfoExt;
    }

    public static IRttUtilExt getRttUtilExt() {
        if (sRttUtilExt == null) {
            synchronized (IRttUtilExt.class) {
                if (sRttUtilExt == null) {
                    sRttUtilExt = CommonPhoneCustomizationUtils.getOpFactory(sContext)
                            .makeRttUtilExt();
                    log("[sRttUtilExt] create ext instance: " + sRttUtilExt);
                }
            }
        }
        return sRttUtilExt;
    }

    public static void initPhoneHelper() {
        try {
            Class<?> cls = Class.forName("cn.richinfo.dm.CtmApplication");
            Method method = cls.getMethod("getInstance", Application.class);
            method.invoke(null, (Application) sContext);
        } catch (ClassNotFoundException |
                 NoSuchMethodException |
                 IllegalAccessException |
                 InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
