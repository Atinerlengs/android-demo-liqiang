/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2017. All rights reserved.
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

package com.mediatek.phone;

import android.Manifest.permission;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.carrier.CarrierIdentifier;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.RadioAccessFamily;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;

import com.android.phone.LocationAccessPolicy;
import com.android.phone.PhoneGlobals;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.IccRecords;
// MTK-START: SIM
import com.android.phone.PhoneInterfaceManager;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.mediatek.internal.telephony.uicc.MtkIccCardProxy;
import com.mediatek.internal.telephony.uicc.MtkUiccCard;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import com.mediatek.internal.telephony.uicc.MtkUiccController;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.IccCard;
// MTK-END
import com.mediatek.internal.telephony.dataconnection.MtkDcTracker;

import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MmsConfigInfo;
import com.mediatek.internal.telephony.MmsIcpInfo;
import com.mediatek.internal.telephony.MtkPhoneConstants;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils.EccEntry;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkRILConstants;
import com.mediatek.internal.telephony.PseudoCellInfo;
import com.mediatek.internal.telephony.phb.CsimPhbUtil;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.selfactivation.ISelfActivation;
import com.mediatek.internal.telephony.uicc.IccFileAdapter;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;
import com.mediatek.internal.telephony.uicc.MtkRuimRecords;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of the IMtkTelephonyEx interface.
 */
public class MtkPhoneInterfaceManagerEx extends IMtkTelephonyEx.Stub {
    protected static final String LOG_TAG = "MtkPhoneIntfMgrEx";
    protected static final boolean DBG = true;//(PhoneGlobals.DBG_LEVEL >= 2);
    protected static final boolean DBG_LOC = false;
    protected static final boolean DBG_MERGE = false;

    // MTK-START: SIM
    private static final int CMD_EXCHANGE_SIM_IO_EX = 102;
    private static final int EVENT_EXCHANGE_SIM_IO_EX_DONE = 103;
    private static final int CMD_GET_ATR = 104;
    private static final int EVENT_GET_ATR_DONE = 105;
    private static final int CMD_LOAD_EF_TRANSPARENT = 108;
    private static final int EVENT_LOAD_EF_TRANSPARENT_DONE = 109;
    private static final int CMD_LOAD_EF_LINEARFIXEDALL = 110;
    private static final int EVENT_LOAD_EF_LINEARFIXEDALL_DONE = 111;
    private static final int CMD_INVOKE_OEM_RIL_REQUEST_RAW = 112;
    private static final int EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE = 113;

    private static final int COMMAND_READ_BINARY = 0xb0;
    private static final int COMMAND_READ_RECORD = 0xb2;
    // MTK-END

    // M: [LTE][Low Power][UL traffic shaping] @{
    private static final int CMD_SET_LTE_ACCESS_STRATUM_STATE = 35;
    private static final int EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE = 36;
    private static final int CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE = 37;
    private static final int EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE = 38;
    // M: [LTE][Low Power][UL traffic shaping] @}

    /** The singleton instance. */
    private static MtkPhoneInterfaceManagerEx sInstance;

    // Query SIM phonebook Adn stroage info thread
    private QueryAdnInfoThread mAdnInfoThread = null;

    private PhoneGlobals mApp;
    private Phone mPhone;
    private CallManager mCM;
    private UserManager mUserManager;
    private AppOpsManager mAppOps;
    protected MainThreadHandler mMainThreadHandler;
    private SubscriptionController mSubscriptionController;
    private SharedPreferences mTelephonySharedPreferences;

    /// M: CC: ECC is in progress
    private boolean mIsEccInProgress = false;
    private boolean mIsLastEccIms;

    // MTK-START: SIM GBA
    // SIM authenthication thread
    private SimAuth mSimAuthThread = null;
    // MTK-END
    // MTK-START: SIM
    private static final String[] PROPERTY_RIL_TEST_SIM = {
         "gsm.sim.ril.testsim",
         "gsm.sim.ril.testsim.2",
         "gsm.sim.ril.testsim.3",
         "gsm.sim.ril.testsim.4",
     };

    /**
     * The property is used to get supported card type of each SIM card in the slot.
     * @hide
     */
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE  = {
        "gsm.ril.fulluicctype",
        "gsm.ril.fulluicctype.2",
        "gsm.ril.fulluicctype.3",
        "gsm.ril.fulluicctype.4",
    };
    // MTK-END

    // [SIM-C2K] @{
    /**
     * The property shows uim imsi that is only for cdma card.
     */
    private static final String[] PROPERTY_UIM_SUBSCRIBER_ID = {
        "ril.uim.subscriberid.1",
        "ril.uim.subscriberid.2",
        "ril.uim.subscriberid.3",
        "ril.uim.subscriberid.4",
    };

    /**
     * The property is used to check if the card is cdma 3G dual mode card in the slot.
     * @hide
     */
     private static final String[] PROPERTY_RIL_CT3G = {
         "gsm.ril.ct3g",
         "gsm.ril.ct3g.2",
         "gsm.ril.ct3g.3",
         "gsm.ril.ct3g.4",
     };

    /**
     * The property record the card's ICC ID.
     */
    private String[] PROPERTY_ICCID_SIM = {
        "ril.iccid.sim1",
        "ril.iccid.sim2",
        "ril.iccid.sim3",
        "ril.iccid.sim4",
    };
    // [SIM-C2K] @}

    // Exit ECBM mode
    private static final int EVENT_EXIT_ECBM_MODE_REQ = 44;

    // [OMH] @{
    private static IccFileAdapter[] sIccFileAdapter = null;
    private String[] mOmhOperators = null;
    private ArrayList<EccEntry> mUserCustomizedEccList = new ArrayList<EccEntry>();
    // [OMH] @}

    // RX Test EVENT
    private static final int EVENT_SET_RX_TEST_CONFIG = 42;
    private static final int EVENT_GET_RX_TEST_RESULT = 43;

    /**
     * The property is used to control the log level of TAGs which used for M log.
     * @hide
     */
     private static final String[] PROPERTY_M_LOG_TAG = {
         "persist.log.tag.DCT",
         "persist.log.tag.MtkDCT",
         "persist.log.tag.RIL-DATA",
         "persist.log.tag.C2K_RIL-DATA",
         "persist.log.tag.GsmCdmaPhone",
         "persist.log.tag.SSDecisonMaker",
         "persist.log.tag.GsmMmiCode",
         "persist.log.tag.RpSsController",
         "persist.log.tag.RIL-SS",
         "persist.log.tag.RILMD2-SS",
         "persist.log.tag.CapaSwitch",
         "persist.log.tag.DSSelector",
         "persist.log.tag.DSSExt",
         "persist.log.tag.Op01DSSExt",
         "persist.log.tag.Op02DSSExt",
         "persist.log.tag.Op09DSSExt",
         "persist.log.tag.Op18DSSExt",
         "persist.log.tag.DSSelectorUtil",
         "persist.log.tag.Op01SimSwitch",
         "persist.log.tag.Op02SimSwitch",
         "persist.log.tag.Op18SimSwitch",
         "persist.log.tag.DcFcMgr",
         "persist.log.tag.DC-1",
         "persist.log.tag.DC-2",
         "persist.log.tag.RetryManager",
         "persist.log.tag.IccProvider",
         "persist.log.tag.IccPhoneBookIM",
         "persist.log.tag.AdnRecordCache",
         "persist.log.tag.AdnRecordLoader",
         "persist.log.tag.AdnRecord",
         "persist.log.tag.RIL-PHB",
         "persist.log.tag.MtkIccProvider",
         "persist.log.tag.MtkIccPHBIM",
         "persist.log.tag.MtkAdnRecord",
         "persist.log.tag.MtkRecordLoader",
         "persist.log.tag.RpPhbController",
         "persist.log.tag.RmcPhbReq",
         "persist.log.tag.RmcPhbUrc",
         "persist.log.tag.RtcPhb",
         "persist.log.tag.RIL-SMS",
         "persist.log.tag.DupSmsFilterExt",
         "persist.log.tag.VT",
         "persist.log.tag.ImsVTProvider",
         "persist.log.tag.IccCardProxy",
         "persist.log.tag.IsimFileHandler",
         "persist.log.tag.IsimRecords",
         "persist.log.tag.SIMRecords",
         "persist.log.tag.SpnOverride",
         "persist.log.tag.UiccCard",
         "persist.log.tag.UiccController",
         "persist.log.tag.RIL-SIM",
         "persist.log.tag.CountryDetector",
         "persist.log.tag.DataDispatcher",
         "persist.log.tag.ImsService",
         "persist.log.tag.IMS_RILA",
         "persist.log.tag.IMSRILRequest",
         "persist.log.tag.ImsManager",
         "persist.log.tag.ImsApp",
         "persist.log.tag.ImsBaseCommands",
         "persist.log.tag.MtkImsManager",
         "persist.log.tag.MtkImsService",
         "persist.log.tag.RP_IMS",
         "persist.log.tag.RtcIms",
         "persist.log.tag.RmcImsCtlUrcHdl",
         "persist.log.tag.RmcImsCtlReqHdl",
         "persist.log.tag.ImsCall",
         "persist.log.tag.ImsPhone",
         "persist.log.tag.ImsPhoneCall",
         "persist.log.tag.ImsPhoneBase",
         "persist.log.tag.ImsCallSession",
         "persist.log.tag.ImsCallProfile",
         "persist.log.tag.ImsEcbm",
         "persist.log.tag.ImsEcbmProxy",
         "persist.log.tag.OperatorUtils",
         "persist.log.tag.WfoApp",
         "persist.log.tag.GbaApp",
         "persist.log.tag.GbaBsfProcedure",
         "persist.log.tag.GbaBsfResponse",
         "persist.log.tag.GbaDebugParam",
         "persist.log.tag.GbaService",
         "persist.log.tag.SresResponse",
         "persist.log.tag.ImsUtService",
         "persist.log.tag.SimservType",
         "persist.log.tag.SimservsTest",
         "persist.log.tag.ImsUt",
         "persist.log.tag.SSDecisonMaker",
         "persist.log.tag.SuppSrvConfig",
         "persist.log.tag.ECCCallHelper",
         "persist.log.tag.GsmConnection",
         "persist.log.tag.TelephonyConf",
         "persist.log.tag.TeleConfCtrler",
         "persist.log.tag.TelephonyConn",
         "persist.log.tag.TeleConnService",
         "persist.log.tag.ECCRetryHandler",
         "persist.log.tag.ECCNumUtils",
         "persist.log.tag.ECCRuleHandler",
         "persist.log.tag.SuppMsgMgr",
         "persist.log.tag.ECCSwitchPhone",
         "persist.log.tag.GsmCdmaConn",
         "persist.log.tag.GsmCdmaPhone",
         "persist.log.tag.Phone",
         "persist.log.tag.RIL-CC",
         "persist.log.tag.RpCallControl",
         "persist.log.tag.RpAudioControl",
         "persist.log.tag.GsmCallTkrHlpr",
         "persist.log.tag.MtkPhoneNotifr",
         "persist.log.tag.MtkGsmCdmaConn",
         "persist.log.tag.RadioManager",
         "persist.log.tag.RIL_Mux",
         "persist.log.tag.RIL-OEM",
         "persist.log.tag.RIL",
         "persist.log.tag.RIL_UIM_SOCKET",
         "persist.log.tag.RILD",
         "persist.log.tag.RIL-RP",
         "persist.log.tag.RfxMessage",
         "persist.log.tag.RfxDebugInfo",
         "persist.log.tag.RfxTimer",
         "persist.log.tag.RfxObject",
         "persist.log.tag.SlotQueueEntry",
         "persist.log.tag.RfxAction",
         "persist.log.tag.RFX",
         "persist.log.tag.RpRadioMessage",
         "persist.log.tag.RpModemMessage",
         "persist.log.tag.PhoneFactory",
         "persist.log.tag.ProxyController",
         "persist.log.tag.SpnOverride",
         "persist.log.tag.RfxDefDestUtils",
         "persist.log.tag.RfxSM",
         "persist.log.tag.RfxSocketSM",
         "persist.log.tag.RfxDT",
         "persist.log.tag.RpCdmaOemCtrl",
         "persist.log.tag.RpRadioCtrl",
         "persist.log.tag.RpMDCtrl",
         "persist.log.tag.RpCdmaRadioCtrl",
         "persist.log.tag.RpFOUtils",
         "persist.log.tag.C2K_RIL-SIM",
         "persist.log.tag.MtkGsmCdmaPhone",
         "persist.log.tag.MtkRILJ",
         "persist.log.tag.MtkRadioInd",
         "persist.log.tag.MtkRadioResp",
         "persist.log.tag.ExternalSimMgr",
         "persist.log.tag.VsimAdaptor",
         "persist.log.tag.MGsmSMSDisp",
         "persist.log.tag.MSimSmsIStatus",
         "persist.log.tag.MSmsStorageMtr",
         "persist.log.tag.MSmsUsageMtr",
         "persist.log.tag.Mtk_RIL_ImsSms",
         "persist.log.tag.MtkConSmsFwk",
         "persist.log.tag.MtkCsimFH",
         "persist.log.tag.MtkDupSmsFilter",
         "persist.log.tag.MtkIccSmsIntMgr",
         "persist.log.tag.MtkIsimFH",
         "persist.log.tag.MtkRuimFH",
         "persist.log.tag.MtkSIMFH",
         "persist.log.tag.MtkSIMRecords",
         "persist.log.tag.MtkSmsCbHeader",
         "persist.log.tag.MtkSmsManager",
         "persist.log.tag.MtkSmsMessage",
         "persist.log.tag.MtkSpnOverride",
         "persist.log.tag.MtkIccCardProxy",
         "persist.log.tag.MtkUiccCard",
         "persist.log.tag.MtkUiccCardApp",
         "persist.log.tag.MtkUiccCtrl",
         "persist.log.tag.MtkUsimFH",
         "persist.log.tag.RpRilClientCtrl",
         "persist.log.tag.RilMalClient",
         "persist.log.tag.RpSimController",
         "persist.log.tag.MtkSubCtrl",
         "persist.log.tag.RP_DAC",
         "persist.log.tag.NetAgentService",
         "persist.log.tag.NetLnkEventHdlr",
         "persist.log.tag.RmcDcCommon",
         "persist.log.tag.RmcDcDefault",
         "persist.log.tag.RtcDC",
         "persist.log.tag.RilClient",
         "persist.log.tag.RmcCommSimReq",
         "persist.log.tag.RmcCommSimOpReq",
         "persist.log.tag.RtcRadioCont",
         "persist.log.tag.MtkRetryManager",
         "persist.log.tag.RmcDcPdnManager",
         "persist.log.tag.RmcDcReqHandler",
         "persist.log.tag.RmcDcUtility",
         "persist.log.tag.RfxIdToMsgId",
         "persist.log.tag.RfxOpUtils",
         "persist.log.tag.RfxMclMessenger",
         "persist.log.tag.RfxRilAdapter",
         "persist.log.tag.RfxFragEnc",
         "persist.log.tag.RfxStatusMgr",
         "persist.log.tag.RmcRadioReq",
         "persist.log.tag.RmcCapa",
         "persist.log.tag.RtcCapa",
         "persist.log.tag.RpMalController",
         "persist.log.tag.WORLDMODE",
         "persist.log.tag.RtcWp",
         "persist.log.tag.RmcWp",
         "persist.log.tag.RmcOpRadioReq",
         "persist.log.tag.RP_DC",
         "persist.log.tag.RfxRilUtils",
         "persist.log.tag.RtcNwCtrl",
         "persist.log.tag.RmcCdmaSimUrc",
         "persist.log.tag.MtkPhoneNumberUtils",
     };

     private static final String[] PROPERTY_V_LOG_TAG = {
         "persist.log.tag.NetworkStats",
         "persist.log.tag.NetworkPolicy",
         "persist.log.tag.RTC_DAC",
         "persist.log.tag.MTKSST",
         "persist.log.tag.RmcNwHdlr",
         "persist.log.tag.RmcNwReqHdlr",
         "persist.log.tag.RmcRatSwHdlr",
         "persist.log.tag.RtcRatSwCtrl"
     };

     private static final String[] PROPERTY_M_LOG_TAG_COMMON_RIL = {
         "persist.log.tag.AT",
         "persist.log.tag.RILMUXD",
         "persist.log.tag.RILC-MTK",
         "persist.log.tag.RILC",
         "persist.log.tag.RfxMainThread",
         "persist.log.tag.RfxRoot",
         "persist.log.tag.RfxRilAdapter",
         "persist.log.tag.RfxController",
         "persist.log.tag.RILC-RP",
         "persist.log.tag.RfxTransUtils",
         "persist.log.tag.RfxMclDisThread",
         "persist.log.tag.RfxCloneMgr",
         "persist.log.tag.RfxHandlerMgr",
         "persist.log.tag.RfxIdToStr",
         "persist.log.tag.RfxDisThread",
         "persist.log.tag.RfxMclStatusMgr",
         "persist.log.tag.RIL-Fusion",
         "persist.log.tag.RfxContFactory",
         "persist.log.tag.RfxChannelMgr"
     };

    private static final String[] ICCRECORD_PROPERTY_ICCID = {
        "ril.iccid.sim1",
        "ril.iccid.sim2",
        "ril.iccid.sim3",
        "ril.iccid.sim4",
    };
     // MTK-START: SIM
     /**
      * A request object to use for transmitting data to an ICC.
      */
     public static final class MtkIccAPDUArgument {
         public int channel, cla, command, p1, p2, p3;
         public String data;
         public String pathId;
         public String pin2;
         public int slotId;
         public int family;

         public MtkIccAPDUArgument(int channel, int cla, int command,
                 int p1, int p2, int p3, String data) {
             //super(channel, cla, command, p1, p2, p3, data);
             int slot = SubscriptionManager
                     .getSlotIndex(SubscriptionManager.getDefaultSubscriptionId());
             if (DBG) log("MtkIccAPDUArgument, default slot " + slot);
             this.channel = channel;
             this.cla = cla;
             this.command = command;
             this.p1 = p1;
             this.p2 = p2;
             this.p3 = p3;
             this.pathId = null;
             this.data = data;
             this.pin2 = null;
             this.slotId = slot;
         }

         public MtkIccAPDUArgument(int slotId, int channel, int cla, int command,
                 int p1, int p2, int p3, String pathId) {
             //super(channel, cla, command, p1, p2, p3, null);
             this.channel = channel;
             this.cla = cla;
             this.command = command;
             this.p1 = p1;
             this.p2 = p2;
             this.p3 = p3;
             this.pathId = pathId;
             this.data = null;
             this.pin2 = null;
             this.slotId = slotId;
         }

         public MtkIccAPDUArgument(int slotId, int family, int channel, int cla,
                 int command, int p1, int p2, int p3, String pathId) {
             //super(channel, cla, command, p1, p2, p3, null);
             this.channel = channel;
             this.cla = cla;
             this.command = command;
             this.p1 = p1;
             this.p2 = p2;
             this.p3 = p3;
             this.pathId = pathId;
             this.data = null;
             this.pin2 = null;
             this.slotId = slotId;
             this.family = family;
         }

         public MtkIccAPDUArgument(int channel, int cla, int command,
                 int p1, int p2, int p3, String data, String pathId, String pin2) {
             //super(channel, cla, command, p1, p2, p3, data);
             int slot = SubscriptionManager
                     .getSlotIndex(SubscriptionManager.getDefaultSubscriptionId());
             this.channel = channel;
             this.cla = cla;
             this.command = command;
             this.p1 = p1;
             this.p2 = p2;
             this.p3 = p3;
             this.pathId = pathId;
             this.data = data;
             this.pin2 = pin2;
             this.slotId = slotId;
         }
     }
     // MTK-END
    /**
     * A request object for use with {@link MainThreadHandler}. Requesters should wait() on the
     * request after sending. The main thread will notify the request when it is complete.
     */
    private static final class MainThreadRequest {
        /** The argument to use for the request */
        public Object argument;
        /** The result of the request that is run on the main thread */
        public Object result;
        // The subscriber id that this request applies to. Defaults to
        // SubscriptionManager.INVALID_SUBSCRIPTION_ID
        public Integer subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        public MainThreadRequest(Object argument) {
            this.argument = argument;
        }

        public MainThreadRequest(Object argument, Integer subId) {
            this.argument = argument;
            if (subId != null) {
                this.subId = subId;
            }
        }
    }

    /**
     * Class to listens for Emergency Callback Mode state change intents
     */
    private class EcmExitReceiver extends BroadcastReceiver {
        private int mSubId;
        private MainThreadRequest mRequest;

        /**
         * Create a broadcast receiver to listen ECBM exit.
         * @param subId the subscription ID
         * @param request the request to th main thread
         */
        public EcmExitReceiver(int subId, MainThreadRequest request) {
            mSubId = subId;
            mRequest = request;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                if (intent.getBooleanExtra(PhoneConstants.PHONE_IN_ECM_STATE, false) == false) {
                    if (intent.getExtras().getInt(PhoneConstants.SUBSCRIPTION_KEY) == mSubId) {
                        mRequest.result = new EcmExitResult(this);
                        synchronized (mRequest) {
                            mRequest.notifyAll();
                        }
                    }
                }
            }
        }

    }

    /**
     * Class to record the result to exiting ECBM mode.
     */
    private class EcmExitResult {
        private EcmExitReceiver mReceiver;
        /**
         * Create a ECBM exit result object.
         * @param receiver the broadcast receiver
         */
        public EcmExitResult(EcmExitReceiver receiver) {
            mReceiver = receiver;
        }

        /**
         * Get the broadcast receiver instance.
         * @return the broadcast receiver
         */
        public EcmExitReceiver getReceiver() {
            return mReceiver;
        }
    }

    /**
     * A handler that processes messages on the main thread in the phone process. Since many
     * of the Phone calls are not thread safe this is needed to shuttle the requests from the
     * inbound binder threads to the main thread in the phone process.  The Binder thread
     * may provide a {@link MainThreadRequest} object in the msg.obj field that they are waiting
     * on, which will be notified when the operation completes and will contain the result of the
     * request.
     *
     * <p>If a MainThreadRequest object is provided in the msg.obj field,
     * note that request.result must be set to something non-null for the calling thread to
     * unblock.
     */
    public class MainThreadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MainThreadRequest request;
            Message onCompleted;
            AsyncResult ar;
            UiccCard uiccCard;
            MtkIccAPDUArgument iccArgument;
            IccFileHandler fh;

            log("MainThreadHandler.handleMessage : " + msg.what);
            switch (msg.what) {
              // MTK-START: SIM
              case CMD_LOAD_EF_TRANSPARENT:
                  request = (MainThreadRequest) msg.obj;
                  iccArgument = (MtkIccAPDUArgument) request.argument;
                  log("CMD_LOAD_EF_TRANSPARENT: slot " + iccArgument.slotId);

                  fh = UiccController.getInstance().getIccFileHandler(
                          iccArgument.slotId, iccArgument.family);
                  if (fh == null) {
                      loge("loadEFTransparent: No UICC");
                      request.result = new AsyncResult(null, null, null);
                      synchronized (request) {
                          request.notifyAll();
                      }
                  } else {
                      onCompleted = obtainMessage(EVENT_LOAD_EF_TRANSPARENT_DONE,
                              request);
                      fh.loadEFTransparent(iccArgument.cla, iccArgument.pathId,
                              onCompleted);
                  }
                  break;

              case EVENT_LOAD_EF_TRANSPARENT_DONE:
                  log("EVENT_LOAD_EF_TRANSPARENT_DONE");
                  ar = (AsyncResult) msg.obj;
                  request = (MainThreadRequest) ar.userObj;
                  if (ar.exception == null && ar.result != null) {
                      request.result = new AsyncResult(null, (byte [])ar.result, null);
                  } else {
                      request.result = new AsyncResult(null, null, null);
                  }
                  synchronized (request) {
                      request.notifyAll();
                  }
                  break;

              case CMD_LOAD_EF_LINEARFIXEDALL:
                  request = (MainThreadRequest) msg.obj;
                  iccArgument = (MtkIccAPDUArgument) request.argument;
                  log("CMD_LOAD_EF_LINEARFIXEDALL: slot " + iccArgument.slotId);

                  fh = UiccController.getInstance().getIccFileHandler(
                          iccArgument.slotId, iccArgument.family);
                  if (fh == null) {
                      loge("loadEFLinearFixedAll: No UICC");
                      request.result = new AsyncResult(null, null, null);
                      synchronized (request) {
                          request.notifyAll();
                      }
                  } else {
                      onCompleted = obtainMessage(EVENT_LOAD_EF_LINEARFIXEDALL_DONE,
                              request);
                      fh.loadEFLinearFixedAll(iccArgument.cla, iccArgument.pathId,
                              onCompleted);
                  }
                  break;

              case EVENT_LOAD_EF_LINEARFIXEDALL_DONE:
                  log("EVENT_LOAD_EF_LINEARFIXEDALL_DONE");
                  ar = (AsyncResult) msg.obj;
                  request = (MainThreadRequest) ar.userObj;
                  if (ar.exception == null && ar.result != null) {
                      request.result = new AsyncResult(null, (ArrayList<byte[]>)ar.result, null);
                  } else {
                      request.result = new AsyncResult(null, null, null);
                  }
                  synchronized (request) {
                      request.notifyAll();
                  }
                  break;

              case CMD_EXCHANGE_SIM_IO_EX:
                  request = (MainThreadRequest) msg.obj;
                  iccArgument = (MtkIccAPDUArgument) request.argument;
                  uiccCard = getUiccCardFromRequest(request);
                  if (uiccCard == null) {
                      loge("iccExchangeSimIOExUsingSlot: No UICC");
                      request.result = new IccIoResult(0x6F, 0, (byte[]) null);
                      synchronized (request) {
                          request.notifyAll();
                      }
                  } else {
                      onCompleted = obtainMessage(EVENT_EXCHANGE_SIM_IO_EX_DONE,
                              request);
                      ((MtkUiccCard)uiccCard).iccExchangeSimIOEx(iccArgument.cla, /* cla is fileID here! */
                              iccArgument.command, iccArgument.p1, iccArgument.p2, iccArgument.p3,
                              iccArgument.pathId, iccArgument.data,
                              iccArgument.pin2, onCompleted);
                  }
                  break;

             case EVENT_EXCHANGE_SIM_IO_EX_DONE:
                  ar = (AsyncResult) msg.obj;
                  request = (MainThreadRequest) ar.userObj;

                  log("EVENT_EXCHANGE_SIM_IO_EX_DONE");
                  if (ar.exception == null && ar.result != null) {
                      request.result = ar.result;
                  } else {
                      request.result = new IccIoResult(0x6f, 0, (byte[]) null);
                      if (ar.result == null) {
                          loge("iccExchangeSimIOExUsingSlot: Empty response");
                      } else if (ar.exception != null && ar.exception instanceof CommandException) {
                          loge("iccExchangeSimIOExUsingSlot: CommandException: " +
                                  ar.exception);
                      } else {
                          loge("iccExchangeSimIOExUsingSlot: Unknown exception");
                      }
                  }

                  synchronized (request) {
                      request.notifyAll();
                  }
                  break;

              case CMD_GET_ATR:
                  request = (MainThreadRequest) msg.obj;
                  iccArgument = (MtkIccAPDUArgument) request.argument;
                  uiccCard = getUiccCardFromRequest(request);

                  if (uiccCard == null) {
                      loge("get ATR: No UICC");
                      request.result = "";
                      synchronized (request) {
                          request.notifyAll();
                      }
                  } else {
                      onCompleted = obtainMessage(EVENT_GET_ATR_DONE, request);
                      ((MtkUiccCard)uiccCard).iccGetAtr(onCompleted);
                  }
                  break;

              case EVENT_GET_ATR_DONE:
                  ar = (AsyncResult) msg.obj;
                  request = (MainThreadRequest) ar.userObj;
                  if (ar.exception == null) {
                      log("EVENT_GET_ATR_DONE, no exception");
                      request.result = ar.result;
                  } else {
                      loge("EVENT_GET_ATR_DONE, exception happens");
                      request.result = "";
                  }
                  synchronized (request) {
                      request.notifyAll();
                  }
                  break;
              // M: [LTE][Low Power][UL traffic shaping] @{
              case CMD_SET_LTE_ACCESS_STRATUM_STATE:
                  request = (MainThreadRequest) msg.obj;
                  boolean enabled = ((Boolean) request.argument).booleanValue();
                  if (DBG) {
                      log("CMD_SET_LTE_ACCESS_STRATUM_STATE: enabled " + enabled
                              + "subId" + request.subId);
                  }
                  mPhone = getPhone(request.subId);
                  if (mPhone == null) {
                      loge("setLteAccessStratumReport: No MainPhone");
                      request.result = new Boolean(false);
                      synchronized (request) {
                          request.notifyAll();
                      }
                  } else {
                      MtkDcTracker dcTracker = (MtkDcTracker) mPhone.mDcTracker;
                      onCompleted = obtainMessage(EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE,
                              request);
                      dcTracker.onSetLteAccessStratumReport((Boolean) enabled, onCompleted);
                  }
                  break;

              case EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE:
                  if (DBG) log("EVENT_SET_LTE_ACCESS_STRATUM_STATE_DONE");
                  handleNullReturnEvent(msg, "setLteAccessStratumReport");
                  break;

              case CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE:
                  request = (MainThreadRequest) msg.obj;
                  int state = ((Integer) request.argument).intValue();
                  if (DBG) {
                      log("CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE: state " + state
                              + "subId " + request.subId);
                  }
                  mPhone = getPhone(request.subId);
                  if (mPhone == null) {
                      loge("setLteUplinkDataTransfer: No MainPhone");
                      request.result = new Boolean(false);
                      synchronized (request) {
                          request.notifyAll();
                      }
                  } else {
                      MtkDcTracker dcTracker = (MtkDcTracker) mPhone.mDcTracker;
                      onCompleted = obtainMessage(EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE,
                              request);
                      dcTracker.onSetLteUplinkDataTransfer((Integer) state, onCompleted);
                  }
                  break;

              case EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE:
                  if (DBG) log("EVENT_SET_LTE_UPLINK_DATA_TRANSFER_STATE_DONE");
                  handleNullReturnEvent(msg, "setLteUplinkDataTransfer");
                  break;
              // M: [LTE][Low Power][UL traffic shaping] @}
              // MTK-END

                case EVENT_SET_RX_TEST_CONFIG:
                case EVENT_GET_RX_TEST_RESULT:
                    if (DBG) log("handle RX_TEST");
                    ar = (AsyncResult) msg.obj;
                    RxTestObject rt = (RxTestObject) ar.userObj;
                    synchronized(rt.lockObj) {
                        if (ar.exception != null) {
                            log("RX_TEST: error ret null, e=" + ar.exception);
                            rt.result = null;
                        } else {
                            rt.result = (int[]) ar.result;
                        }
                        rt.lockObj.notify();
                        if (DBG) log("RX_TEST notify result");
                    }
                    break;

              case EVENT_EXIT_ECBM_MODE_REQ:
                  request = (MainThreadRequest) msg.obj;
                  Integer subId = (Integer) request.argument;
                  if (getPhone(subId).isInEcm()) {
                      IntentFilter filter = new IntentFilter();
                      filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
                      EcmExitReceiver receiver = new EcmExitReceiver(subId, request);
                      log("Exit ECBM mode receiver " + receiver);
                      mApp.registerReceiver(receiver, filter);
                      getPhone(subId).exitEmergencyCallbackMode();
                  } else {
                      request.result = new EcmExitResult(null);
                      synchronized (request) {
                          request.notifyAll();
                      }
                  }
                  break;

                case CMD_INVOKE_OEM_RIL_REQUEST_RAW:
                    request = (MainThreadRequest)msg.obj;
                    onCompleted = obtainMessage(EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE, request);
                    ((MtkGsmCdmaPhone)mPhone).invokeOemRilRequestRaw((byte[])request.argument,
                            onCompleted);
                    break;

                case EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE:
                    ar = (AsyncResult)msg.obj;
                    request = (MainThreadRequest)ar.userObj;
                    request.result = ar;
                    synchronized (request) {
                        request.notifyAll();
                    }
                    break;

                default:
                    Log.w(LOG_TAG, "MainThreadHandler: unexpected message code: " + msg.what);
                    break;
            }
        }

        private void handleNullReturnEvent(Message msg, String command) {
            AsyncResult ar = (AsyncResult) msg.obj;
            MainThreadRequest request = (MainThreadRequest) ar.userObj;
            if (ar.exception == null) {
                request.result = true;
            } else {
                request.result = false;
                if (ar.exception instanceof CommandException) {
                    loge(command + ": CommandException: " + ar.exception);
                } else {
                    loge(command + ": Unknown exception");
                }
            }
            synchronized (request) {
                request.notifyAll();
            }
        }
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see #sendRequestAsync
     */
    private Object sendRequest(int command, Object argument) {
        return sendRequest(command, argument, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    /**
     * Posts the specified command to be executed on the main thread,
     * waits for the request to complete, and returns the result.
     * @see #sendRequestAsync
     */
    private Object sendRequest(int command, Object argument, Integer subId) {
        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }

        MainThreadRequest request = new MainThreadRequest(argument, subId);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request) {
            while (request.result == null) {
                try {
                    request.wait();
                } catch (InterruptedException e) {
                    // Do nothing, go back and wait until the request is complete
                }
            }
        }
        return request.result;
    }

    /**
     * Asynchronous ("fire and forget") version of sendRequest():
     * Posts the specified command to be executed on the main thread, and
     * returns immediately.
     * @see #sendRequest
     */
    private void sendRequestAsync(int command) {
        mMainThreadHandler.sendEmptyMessage(command);
    }

    /**
     * Same as {@link #sendRequestAsync(int)} except it takes an argument.
     * @see {@link #sendRequest(int,Object)}
     */
    private void sendRequestAsync(int command, Object argument) {
        MainThreadRequest request = new MainThreadRequest(argument);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();
    }

    /**
     * Initialize the singleton PhoneInterfaceManager instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     */
    public static MtkPhoneInterfaceManagerEx init(PhoneGlobals app, Phone phone) {
        synchronized (MtkPhoneInterfaceManagerEx.class) {
            if (sInstance == null) {
                sInstance = new MtkPhoneInterfaceManagerEx(app, phone);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    protected MtkPhoneInterfaceManagerEx(PhoneGlobals app, Phone phone) {
        mApp = app;
        mPhone = phone;
        //mCM = PhoneGlobals.getInstance().mCM;
        mUserManager = (UserManager) app.getSystemService(Context.USER_SERVICE);
        mAppOps = (AppOpsManager)app.getSystemService(Context.APP_OPS_SERVICE);
        mMainThreadHandler = new MainThreadHandler();
        mTelephonySharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(mPhone.getContext());
        mSubscriptionController = SubscriptionController.getInstance();

        // Init customized ECC
        updateUserCustomizedEccList(getUserCustomizedEccList());

        publish();

        // [OMH] init
        omhInit();
    }

    private void publish() {
        if (DBG) log("publish: " + this);

        ServiceManager.addService("phoneEx", this);
    }

    private Phone getPhoneFromRequest(MainThreadRequest request) {
        return (request.subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                ? mPhone : getPhone(request.subId);
    }

    // returns phone associated with the subId.
    private static Phone getPhone(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        return PhoneFactory.getPhone(
                ((phoneId < 0) ? SubscriptionManager.DEFAULT_PHONE_INDEX : phoneId));
    }

    protected static void log(String msg) {
        Log.e(LOG_TAG, msg);
    }

    protected static void logv(String msg) {
        Log.e(LOG_TAG, msg);
    }

    protected static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }

    /**
     * Enable or disable Telephony and connectivity debug log
     * @param enable true: enable log, false: disable log
     */
    @Override
    public void setTelLog(boolean enable) {
        if (DBG) log("setTelLog enable = " + enable);
        if (SystemProperties.getInt("persist.log.tag.tel_log_ctrl", 0) != 1) {
            return;
        }
        if (enable) {
            for (String telLogTag : PROPERTY_M_LOG_TAG_COMMON_RIL) {
                SystemProperties.set(telLogTag, "D");
            }
            for (String telLogTag : PROPERTY_M_LOG_TAG) {
                SystemProperties.set(telLogTag, "D");
            }
            for (String telLogTag : PROPERTY_V_LOG_TAG) {
                SystemProperties.set(telLogTag, "V");
            }
        } else {
            // Userdebug/user load: allow log level I
            for (String telLogTag : PROPERTY_M_LOG_TAG_COMMON_RIL) {
                SystemProperties.set(telLogTag, "I");
            }
            if (!SystemProperties.get("ro.build.type").equals("eng")) {
                for (String telLogTag : PROPERTY_M_LOG_TAG) {
                    SystemProperties.set(telLogTag, "I");
                }
                for (String telLogTag : PROPERTY_V_LOG_TAG) {
                    SystemProperties.set(telLogTag, "I");
                }
            }
        }
    }

    // MTK-START: SIM ME LOCK
    private class UnlockSim extends Thread {

        /* Query network lock start */

        // Verify network lock result.
        public static final int VERIFY_RESULT_PASS = 0;
        public static final int VERIFY_INCORRECT_PASSWORD = 1;
        public static final int VERIFY_RESULT_EXCEPTION = 2;

        // Total network lock count.
        public static final int NETWORK_LOCK_TOTAL_COUNT = 5;
        public static final String QUERY_SIMME_LOCK_RESULT =
                "com.mediatek.phone.QUERY_SIMME_LOCK_RESULT";
        public static final String SIMME_LOCK_LEFT_COUNT =
                "com.mediatek.phone.SIMME_LOCK_LEFT_COUNT";

        /* Query network lock end */

        private MtkIccCardProxy mSimCard = null;
        private boolean mDone = false;
        private boolean mResult = false;

        // For replies from SimCard interface
        private Handler mHandler;

        private static final int QUERY_NETWORK_STATUS_COMPLETE = 100;
        private static final int SET_NETWORK_LOCK_COMPLETE = 101;

        private int mVerifyResult = -1;
        private int mSIMMELockRetryCount = -1;

        public UnlockSim(IccCard simCard) {
            if (simCard instanceof MtkIccCardProxy) {
                mSimCard = (MtkIccCardProxy)simCard;
            } else {
                log("UnlockSim: Not MtkIccCardProxy instance.");
            }
        }
        @Override
        public void run() {
            Looper.prepare();
            synchronized (UnlockSim.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case QUERY_NETWORK_STATUS_COMPLETE:
                                synchronized (UnlockSim.this) {
                                    int [] LockState = (int []) ar.result;
                                    if (ar.exception != null) { //Query exception occurs
                                        log("Query network lock fail");
                                        mResult = false;
                                    } else {
                                        mSIMMELockRetryCount = LockState[2];
                                        log("[SIMQUERY] Category = " + LockState[0]
                                            + " ,Network status =" + LockState[1]
                                            + " ,Retry count = " + LockState[2]);

                                         mResult = true;
                                    }
                                    mDone = true;
                                    UnlockSim.this.notifyAll();
                                }
                                break;
                            case SET_NETWORK_LOCK_COMPLETE:
                                log("SUPPLY_NETWORK_LOCK_COMPLETE");
                                synchronized (UnlockSim.this) {
                                    if ((ar.exception != null) &&
                                           (ar.exception instanceof CommandException)) {
                                        log("ar.exception " + ar.exception);
                                        if (((CommandException) ar.exception).getCommandError()
                                            == CommandException.Error.PASSWORD_INCORRECT) {
                                            mVerifyResult = VERIFY_INCORRECT_PASSWORD;
                                       } else {
                                            mVerifyResult = VERIFY_RESULT_EXCEPTION;
                                       }
                                    } else {
                                        mVerifyResult = VERIFY_RESULT_PASS;
                                    }
                                    mDone = true;
                                    UnlockSim.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                UnlockSim.this.notifyAll();
            }
            Looper.loop();
        }

        synchronized Bundle queryNetworkLock(int category) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            log("Enter queryNetworkLock");
            Message callback = Message.obtain(mHandler, QUERY_NETWORK_STATUS_COMPLETE);
            mSimCard.queryIccNetworkLock(category, callback);

            while (!mDone) {
                try {
                    log("wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            try {
                mHandler.getLooper().quit();
                if (mHandler.getLooper().getThread() != null) {
                    mHandler.getLooper().getThread().interrupt();
                }
            } catch (NullPointerException ne) {
                loge("queryNetworkLock Null looper");
                ne.printStackTrace();
            }

            Bundle bundle = new Bundle();
            bundle.putBoolean(QUERY_SIMME_LOCK_RESULT, mResult);
            bundle.putInt(SIMME_LOCK_LEFT_COUNT, mSIMMELockRetryCount);

            log("done");
            return bundle;
        }

        synchronized int supplyNetworkLock(String strPasswd) {

            while (mHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            log("Enter supplyNetworkLock");
            Message callback = Message.obtain(mHandler, SET_NETWORK_LOCK_COMPLETE);
            mSimCard.supplyNetworkDepersonalization(strPasswd, callback);

            while (!mDone) {
                try {
                    log("wait for done");
                    wait();
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                }
            }
            try {
                mHandler.getLooper().quit();
                if (mHandler.getLooper().getThread() != null) {
                    mHandler.getLooper().getThread().interrupt();
                }
            } catch (NullPointerException ne) {
                loge("supplyNetworkLock Null looper");
                ne.printStackTrace();
            }

            log("done");
            return mVerifyResult;
        }
    }

    public Bundle queryNetworkLock(int subId, int category) {
        final UnlockSim queryNetworkLockState;

        log("queryNetworkLock");

        queryNetworkLockState = new UnlockSim(getPhone(subId).getIccCard());
        // MTK-START: should confirm with keygurad can handle null Bundle.
        if (queryNetworkLockState.mSimCard == null) {
            return null;
        }
        // MTK-END
        queryNetworkLockState.start();

        return queryNetworkLockState.queryNetworkLock(category);
    }

    public int supplyNetworkDepersonalization(int subId, String strPasswd) {
        final UnlockSim supplyNetworkLock;

        log("supplyNetworkDepersonalization");

        supplyNetworkLock = new UnlockSim(getPhone(subId).getIccCard());
        supplyNetworkLock.start();

        return supplyNetworkLock.supplyNetworkLock(strPasswd);
    }
    // MTK-END

    // MTK-START: CMCC DUAL SIM DEPENDENCY LOCK
    /**
     * Modem SML change feature.
     * This function will query the SIM state of the given slot. And broadcast
     * ACTION_UNLOCK_SIM_LOCK if the SIM state is in network lock.
     *
     * @param subId: Indicate which sub to query
     * @param needIntent: The caller can deside to broadcast ACTION_UNLOCK_SIM_LOCK or not
     *                    in this time, because some APs will receive this intent (eg. Keyguard).
     *                    That can avoid this intent to effect other AP.
     */
    public void repollIccStateForNetworkLock(int subId, boolean needIntent) {
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            ((MtkIccCardProxy)getPhone(subId).getIccCard()).
                    repollIccStateForModemSmlChangeFeatrue(needIntent);
        } else {
            log("Not Support in Single SIM.");
        }
    }
    // MTK-END

    /// M: [Network][C2K] Add isInHomeNetwork interface. @{
    @Override
    public boolean isInHomeNetwork(int subId) {
         final int phoneId = SubscriptionManager.getPhoneId(subId);
         boolean isInHomeNetwork = false;
         final Phone phone = PhoneFactory.getPhone(phoneId);
         if (phone != null) {
             ServiceState serviceState = phone.getServiceState();
             if (serviceState != null) {
                 isInHomeNetwork = inSameCountry(phoneId, serviceState.getVoiceOperatorNumeric());
             }
         }
         log("isInHomeNetwork, subId=" + subId + " ,phoneId=" + phoneId
                 + " ,isInHomeNetwork=" + isInHomeNetwork);
         return isInHomeNetwork;
     }

     /**
      * Check ISO country by MCC to see if phone is roaming in same registered country.
      *
      * @param phoneId for which phone inSameCountry is returned
      * @param operatorNumeric registered operator numeric
      * @return true if in same country.
      */
     private static final boolean inSameCountry(int phoneId, String operatorNumeric) {
         if (TextUtils.isEmpty(operatorNumeric) || (operatorNumeric.length() < 5)
                 || (!TextUtils.isDigitsOnly(operatorNumeric))) {
             // Not a valid network
             log("inSameCountry, Not a valid network"
                     + ", phoneId=" + phoneId + ", operatorNumeric=" + operatorNumeric);
             return true;
         }

         final String homeNumeric = getHomeOperatorNumeric(phoneId);
         if (TextUtils.isEmpty(homeNumeric) || (homeNumeric.length() < 5)
                 || (!TextUtils.isDigitsOnly(homeNumeric))) {
             // Not a valid SIM MCC
             log("inSameCountry, Not a valid SIM MCC"
                     + ", phoneId=" + phoneId + ", homeNumeric=" + homeNumeric);
             return true;
         }

         boolean inSameCountry = true;
         final String networkMCC = operatorNumeric.substring(0, 3);
         final String homeMCC = homeNumeric.substring(0, 3);
         final String networkCountry = MccTable.countryCodeForMcc(Integer.parseInt(networkMCC));
         final String homeCountry = MccTable.countryCodeForMcc(Integer.parseInt(homeMCC));
         log("inSameCountry, phoneId=" + phoneId
                 + ", homeMCC=" + homeMCC
                 + ", networkMCC=" + networkMCC
                 + ", homeCountry=" + homeCountry
                 + ", networkCountry=" + networkCountry);
         if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
             // Not a valid country
             return true;
         }
         inSameCountry = homeCountry.equals(networkCountry);
         if (inSameCountry) {
             return inSameCountry;
         }
         // special same country cases
         if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
             inSameCountry = true;
         } else if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
             inSameCountry = true;
         } else if ("cn".equals(homeCountry) && "mo".equals(networkCountry)) {
             inSameCountry = true;
         }

         log("inSameCountry, phoneId=" + phoneId + ", inSameCountry=" + inSameCountry);
         return inSameCountry;
     }

     /**
      * Returns the Service Provider Name (SPN).
      *
      * @param phoneId for which Home Operator Numeric is returned
      * @return the Service Provider Name (SPN)
      */
     private static final String getHomeOperatorNumeric(int phoneId) {
         String numeric = TelephonyManager.getDefault().getSimOperatorNumericForPhone(phoneId);
         if (TextUtils.isEmpty(numeric)) {
             numeric = SystemProperties.get("ro.cdma.home.operator.numeric", "");
         }

         // For CT 3G special case
         MtkTelephonyManagerEx telEx = MtkTelephonyManagerEx.getDefault();
         boolean isCt3gDualMode = telEx.isCt3gDualMode(phoneId);
         if (isCt3gDualMode && "20404".equals(numeric)) {
             numeric = "46003";
         }

         log("getHomeOperatorNumeric, phoneId=" + phoneId + ", numeric=" + numeric);
         return numeric;
     }
     /// @}

    @Override
    public Bundle getCellLocationUsingSlotId(int slotId) {
        enforceFineOrCoarseLocationPermission("getCellLocationUsingSlotId");

        if (DBG_LOC) log("getCellLocationUsingSlotId: is active user");
        Bundle data = new Bundle();
        int subId = getSubIdBySlot(slotId);
        Phone phone = getPhone(subId);
        if (phone == null) {
            return null;
        }

        phone.getCellLocation(null).fillInNotifierBundle(data);
        return data;
    }

    private void enforceFineOrCoarseLocationPermission(String message) {
        try {
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION, null);
        } catch (SecurityException e) {
            // If we have ACCESS_FINE_LOCATION permission, skip the check for ACCESS_COARSE_LOCATION
            // A failure should throw the SecurityException from ACCESS_COARSE_LOCATION since this
            // is the weaker precondition
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION, message);
        }
    }

    private static boolean checkIfCallerIsSelfOrForegroundUser() {
        boolean ok;

        boolean self = Binder.getCallingUid() == Process.myUid();
        if (!self) {
            // Get the caller's user id then clear the calling identity
            // which will be restored in the finally clause.
            int callingUser = UserHandle.getCallingUserId();
            long ident = Binder.clearCallingIdentity();

            try {
                // With calling identity cleared the current user is the foreground user.
                int foregroundUser = ActivityManager.getCurrentUser();
                ok = (foregroundUser == callingUser);
                if (DBG_LOC) {
                    log("checkIfCallerIsSelfOrForegoundUser: foregroundUser=" + foregroundUser
                            + " callingUser=" + callingUser + " ok=" + ok);
                }
            } catch (NullPointerException ex) {
                if (DBG_LOC) {
                    loge("checkIfCallerIsSelfOrForegoundUser: Exception ex=" + ex);
                }
                ok = false;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            if (DBG_LOC) {
                log("checkIfCallerIsSelfOrForegoundUser: is self");
            }
            ok = true;
        }
        if (DBG_LOC) {
            log("checkIfCallerIsSelfOrForegoundUser: ret=" + ok);
        }
        return ok;
    }

    // MTK-START: SIM
    // Added by M start
    private int getSubIdBySlot(int slot) {
        int [] subId = SubscriptionManager.getSubId(slot);
        if (subId == null || subId.length == 0) {
            return getDefaultSubscription();
        }
        if (DBG) log("getSubIdBySlot, simId " + slot + "subId " + subId[0]);
        return subId[0];
    }

    @Override
    public String getIccAtr(int subId) {
        enforceModifyPermissionOrCarrierPrivilege(subId);

        if (DBG) log("> getIccAtr " + ", subId = " + subId);
        String response = (String) sendRequest(CMD_GET_ATR, null, subId);
        if (DBG) log("< getIccAtr: " + response);
        return response;
    }

    @Override
    public byte[] iccExchangeSimIOEx(int subId, int fileID, int command,
            int p1, int p2, int p3, String filePath, String data, String pin2) {
        enforceModifyPermissionOrCarrierPrivilege(subId);

        if (DBG) log("Exchange SIM_IO Ex " + fileID + ":" + command + " " +
                 p1 + " " + p2 + " " + p3 + ":" + filePath + ", " + data + ", " + pin2 +
                 ", subId = " + subId);

        IccIoResult response =
                (IccIoResult) sendRequest(CMD_EXCHANGE_SIM_IO_EX,
                        new MtkIccAPDUArgument(-1, fileID, command,
                        p1, p2, p3, data, filePath, pin2), subId);

        if (DBG) log("Exchange SIM_IO Ex [R]" + response);
        byte[] result = null; int length = 2;
        if (response.payload != null) {
            length = 2 + response.payload.length;
            result = new byte[length];
            System.arraycopy(response.payload, 0, result, 0, response.payload.length);
        } else result = new byte[length];

        if (DBG) log("Exchange SIM_IO Ex [L] " + length);
        result[length - 1] = (byte) response.sw2;
        result[length - 2] = (byte) response.sw1;
        return result;
    }

    @Override
    /**
     * Returns the response APDU for a command APDU sent through SIM_IO.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_PHONE_STATE MODIFY_PHONE_STATE}
     * Or the calling app has carrier privileges. @see #hasCarrierPrivileges.
     *
     * @param slotId
     * @param family
     * @param fileID
     * @param filePath
     * @return The APDU response
     */
    public byte[] loadEFTransparent(int slotId, int family, int fileID, String filePath) {
        enforceModifyPermissionOrCarrierPrivilege(getSubIdBySlot(slotId));

        if (DBG) {
            log("loadEFTransparent slot " + slotId + " " + family + " " + fileID + ":" + filePath);
        }

        AsyncResult ar = (AsyncResult)  sendRequest(CMD_LOAD_EF_TRANSPARENT,
                new MtkIccAPDUArgument(slotId, family, -1, fileID, COMMAND_READ_BINARY, 0, 0, 0,
                filePath));
        byte[] response = (byte[])ar.result;
        if (DBG) {
            log("loadEFTransparent " + response);
        }
        return response;
    }

    @Override
    /**
     * Returns the response APDU for a command APDU sent through SIM_IO.
     *
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#MODIFY_PHONE_STATE MODIFY_PHONE_STATE}
     * Or the calling app has carrier privileges. @see #hasCarrierPrivileges.
     *
     * @param slotId
     * @param family
     * @param fileID
     * @param filePath
     * @return The APDU response
     */
    public List<String> loadEFLinearFixedAll(int slotId, int family, int fileID,
            String filePath) {
        enforceModifyPermissionOrCarrierPrivilege(getSubIdBySlot(slotId));

        if (DBG) {
            log("loadEFLinearFixedAll slot " + slotId + " " + family + " " + fileID + ":"
                    + filePath);
        }
        AsyncResult ar = (AsyncResult) sendRequest(CMD_LOAD_EF_LINEARFIXEDALL,
                new MtkIccAPDUArgument(slotId, family, -1, fileID, COMMAND_READ_RECORD, 0, 0, 0,
                filePath));
        ArrayList<byte[]> result = (ArrayList<byte[]>) ar.result;
        if (result == null) {
            log("loadEFLinearFixedAll return null");
            return null;
        }
        List<String> response = new ArrayList<String>();
        for (int i = 0 ; i < result.size(); i++) {
            if (result.get(i) == null) {
                continue;
            }
            String res = IccUtils.bytesToHexString(result.get(i));
            response.add(res);
        }
        if (DBG) {
            log("loadEFLinearFixedAll " + response);
        }
        return response;
    }

    public String getIccCardType(int subId) {
        if (DBG) log("getIccCardType  subId=" + subId);

        Phone phone = getPhone(subId);
        if (phone == null) {
            if (DBG) log("getIccCardType(): phone is null");
            return "";
        }
        return ((MtkIccCardProxy)phone.getIccCard()).getIccCardType();
    }

    public boolean isAppTypeSupported(int slotId, int appType) {
        if (DBG) log("isAppTypeSupported  slotId=" + slotId);

        UiccCard uiccCard = UiccController.getInstance().getUiccCard(slotId);
        if (uiccCard == null) {
            if (DBG) log("isAppTypeSupported(): uiccCard is null");
            return false;
        }

        return ((uiccCard.getApplicationByType(appType) == null) ?  false : true);
    }

    public boolean isTestIccCard(int slotId) {
        String mTestCard = null;

        mTestCard = SystemProperties.get(PROPERTY_RIL_TEST_SIM[slotId], "");
        if (DBG) log("isTestIccCard(): slot id =" + slotId + ", iccType = " + mTestCard);
        return (mTestCard != null && mTestCard.equals("1"));
    }

    /**
     * Get icc app family by slot id.
     * @param slotId slot id
     * @return the family type
     * @hide
     */
    public int getIccAppFamily(int slotId) {
        int iccType = MtkTelephonyManagerEx.APP_FAM_NONE;
        int phoneCount = TelephonyManager.getDefault().getSimCount();
        if (slotId < 0 || slotId >= phoneCount) {
            log("getIccAppFamily, invalid slotId:" + slotId);
            return iccType;
        }

        String uiccType = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId]);
        String appType[] = uiccType.split(",");
        int fullType = MtkTelephonyManagerEx.CARD_TYPE_NONE;
        for (int i = 0; i < appType.length; i++) {
            if ("USIM".equals(appType[i])) {
                fullType = fullType | MtkTelephonyManagerEx.CARD_TYPE_USIM;
            } else if ("SIM".equals(appType[i])) {
                fullType = fullType | MtkTelephonyManagerEx.CARD_TYPE_SIM;
            } else if ("CSIM".equals(appType[i])) {
                fullType = fullType | MtkTelephonyManagerEx.CARD_TYPE_CSIM;
            } else if ("RUIM".equals(appType[i])) {
                fullType = fullType | MtkTelephonyManagerEx.CARD_TYPE_RUIM;
            }
        }

        if (fullType == MtkTelephonyManagerEx.CARD_TYPE_NONE) {
            iccType = MtkTelephonyManagerEx.APP_FAM_NONE;
        } else if ((fullType & MtkTelephonyManagerEx.CARD_TYPE_CSIM) != 0
                && (fullType & MtkTelephonyManagerEx.CARD_TYPE_USIM) != 0) {
            iccType = MtkTelephonyManagerEx.APP_FAM_3GPP2 | MtkTelephonyManagerEx.APP_FAM_3GPP;
        } else if ((fullType & MtkTelephonyManagerEx.CARD_TYPE_CSIM) != 0
                || (fullType & MtkTelephonyManagerEx.CARD_TYPE_RUIM) != 0) {
            iccType = MtkTelephonyManagerEx.APP_FAM_3GPP2;
        } else {
            iccType = MtkTelephonyManagerEx.APP_FAM_3GPP;

            // Uim dual mode sim, may switch to SIM type for use
            if (fullType == MtkTelephonyManagerEx.CARD_TYPE_SIM) {
                String uimDualMode = SystemProperties.get(PROPERTY_RIL_CT3G[slotId]);
                if ("1".equals(uimDualMode)) {
                    iccType = MtkTelephonyManagerEx.APP_FAM_3GPP2;
                }
            }

        }
        log("getIccAppFamily, " + "uiccType[" + slotId + "] = "
                    + uiccType + "fullType = " + fullType + " iccType = " + iccType);
        return iccType;
    }

    /**
     * Make sure either system app or the caller has carrier privilege.
     *
     * @throws SecurityException if the caller does not have the required permission/privilege
     */
    private void enforceModifyPermissionOrCarrierPrivilege(int subId) {
        int permission = mApp.checkCallingOrSelfPermission(
                android.Manifest.permission.MODIFY_PHONE_STATE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        log("No modify permission, check carrier privilege next.");
        enforceCarrierPrivilege(subId);
    }

    /**
     * Make sure the caller has the READ_PRIVILEGED_PHONE_STATE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforcePrivilegedPhoneStatePermission() {
        mApp.enforceCallingOrSelfPermission(
                android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE, null);
    }

    /**
     * Make sure the caller has carrier privilege.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceCarrierPrivilege(int subId) {
        if (getCarrierPrivilegeStatus(subId) !=
                    TelephonyManager.CARRIER_PRIVILEGE_STATUS_HAS_ACCESS) {
            loge("No Carrier Privilege.");
            throw new SecurityException("No Carrier Privilege.");
        }
    }

    private int getCarrierPrivilegeStatus(int subId) {
        final Phone phone = getPhone(subId);
        if (phone == null) {
            loge("getCarrierPrivilegeStatus: Invalid subId");
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_NO_ACCESS;
        }
        UiccCard card = UiccController.getInstance().getUiccCard(phone.getPhoneId());
        if (card == null) {
            loge("getCarrierPrivilegeStatus: No UICC");
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
        return card.getCarrierPrivilegeStatusForCurrentTransaction(
                phone.getContext().getPackageManager());
    }

    private UiccCard getUiccCardFromRequest(MainThreadRequest request) {
        Phone phone = getPhoneFromRequest(request);
        return phone == null ? null :
                UiccController.getInstance().getUiccCard(phone.getPhoneId());
    }

    private int getDefaultSubscription() {
        return mSubscriptionController.getDefaultSubId();
    }

    // MTK-START: MVNO
    public String getMvnoMatchType(int subId) {
        String type = ((MtkGsmCdmaPhone)getPhone(subId)).getMvnoMatchType();
        if (DBG) log("getMvnoMatchType sub = " + subId + " ,type = " + type);
        return type;
    }

    public String getMvnoPattern(int subId, String type) {
        String pattern = ((MtkGsmCdmaPhone)getPhone(subId)).getMvnoPattern(type);
        if (DBG) log("getMvnoPattern sub = " + subId + " ,pattern = " + pattern);
        return pattern;
    }
    // MTK-END

    /**
     * Query if the radio is turned off by user.
     *
     * @param subId inidicated subscription
     *
     * @return true radio is turned off by user.
     *         false radio isn't turned off by user.
     *
     */
    public boolean isRadioOffBySimManagement(int subId) {
        boolean result = true;
        try {
            Context otherAppsContext = mApp.createPackageContext(
                    "com.android.phone", Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences mIccidPreference =
                    otherAppsContext.getSharedPreferences("RADIO_STATUS", 0);

            if (SubscriptionController.getInstance() != null) {
                int mSlotId = SubscriptionController.getInstance().getPhoneId(subId);

                if (mSlotId < 0 || mSlotId >= TelephonyManager.getDefault().getPhoneCount()) {
                    log("[isRadioOffBySimManagement]mSlotId: " + mSlotId);
                        return false;
                }

                String mIccId = SystemProperties.get(ICCRECORD_PROPERTY_ICCID[mSlotId], "");
                if ((mIccId != null) && (mIccidPreference != null)) {
                    log("[isRadioOffBySimManagement]SharedPreferences: "
                            + mIccidPreference.getAll().size() + ", IccId: " + mIccId);
                    result = mIccidPreference.contains(mIccId);
                }
            }

            log("[isRadioOffBySimManagement]result: " + result);
        } catch (NameNotFoundException e) {
            log("Fail to create com.android.phone createPackageContext");
        }
        return result;
    }
    // MTK-END

    /**
    * Return true if the FDN of the ICC card is enabled
    */
    public boolean isFdnEnabled(int subId) {
        log("isFdnEnabled subId=" + subId);

        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            loge("Error subId: " + subId);
            return false;
        }

        /* We will rollback the temporary solution after SubscriptionManager merge to L1 */
        Phone phone = getPhone(subId);
        if (phone != null && phone.getIccCard() != null) {
            return phone.getIccCard().getIccFdnAvailable() && phone.getIccCard().getIccFdnEnabled();
        } else {
            return false;
        }
    }

    private boolean canReadPhoneState(String callingPackage, String message) {
        try {
            mApp.enforceCallingOrSelfPermission(
                    android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE, message);

            // SKIP checking for run-time permission since caller or self has PRIVILEDGED permission
            return true;
        } catch (SecurityException e) {
            mApp.enforceCallingOrSelfPermission(android.Manifest.permission.READ_PHONE_STATE,
                    message);
        }

        if (mAppOps.noteOp(AppOpsManager.OP_READ_PHONE_STATE, Binder.getCallingUid(),
                callingPackage) != AppOpsManager.MODE_ALLOWED) {
            return false;
        }

        return true;
    }

    // [SIM-C2K] @{
    /**
     * Get uim imsi by sub id.
     * @param callingPackage The package get UIM subscriber id.
     * @param subId subscriber id
     * @return uim imsi
     */
    public String getUimSubscriberId(String callingPackage, int subId) {
        if (!canReadPhoneState(callingPackage, "getUimSubscriberId")) {
            log("getUimImsiBySubId: permission denied");
            return null;
        }

        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= PROPERTY_UIM_SUBSCRIBER_ID.length) {
            log("getUimImsiBySubId:invalid phoneId " + phoneId);
            return null;
        }

        return SystemProperties.get(PROPERTY_UIM_SUBSCRIBER_ID[phoneId], "");
    }

    /**
     * Get IccId by slotId.
     * @param callingPackage The package get SIM serial number.
     * @param slotId int
     * @return Iccid
     */
    public String getSimSerialNumber(String callingPackage, int slotId) {
        if (!canReadPhoneState(callingPackage, "getSimSerialNumber")) {
            log("getSimSerialNumber: permission denied");
            return null;
        }
        return SystemProperties.get(PROPERTY_ICCID_SIM[slotId], "");
    }
    // [SIM-C2K] @}

    // [OMH] @{
    @Override
    public boolean isOmhEnable(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        Phone phone = getPhone(subId);
        if (phone == null || phone.getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA) {
            return false;
        }

        String operator = TelephonyManager.getDefault()
                .getSimOperatorNumeric(subId);
        if (DBG_LOC) {
            log("isOmhEnable: the subId = " + subId + " operator name = " + operator);
        }

        if (operator == null || mOmhOperators == null) {
            return false;
        }

        if ("1".equals(SystemProperties.get("persist.radio.omh.debug", "0"))) {
            // Add for debug OMH using non-OMH card
            if (DBG_LOC) {
                log("isOmhEnable: debug enable");
            }
            return true;
        }

        for (String s : mOmhOperators) {
            if (DBG_LOC) {
                log("isOmhEnable: operator = " + s);
            }
            if (operator.equals(s)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isOmhCard(int subId) {
        if (!isOmhEnable(subId)) {
            return false;
        }
        return getIccFileAdapterBySubId(subId).isOmhCard();
    }

    @Override
    public MmsIcpInfo getMmsIcpInfo(int subId) {
        Object object = getIccFileAdapterBySubId(subId)
                .getMmsIcpInfo();
        if (object instanceof MmsIcpInfo) {
            return (MmsIcpInfo) object;
        }
        return null;
    }

    @Override
    public MmsConfigInfo getMmsConfigInfo(int subId) {
        Object object = getIccFileAdapterBySubId(subId)
                .getMmsConfigInfo();
        if (object instanceof MmsConfigInfo) {
            return (MmsConfigInfo) object;
        }
        return null;
    }

    @Override
    public Bundle getUserCustomizedEccList() {
        Bundle result = null;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mApp);
        int count = sp.getInt("ecc_count", 0);
        if (count > 0) {
            ArrayList<String> names = new ArrayList<String>();
            ArrayList<String> numbers = new ArrayList<String>();
            for (int i = 0; i < count; i++) {
                names.add(sp.getString("ecc_name" + i, ""));
                numbers.add(sp.getString("ecc_number" + i, ""));
            }
            result = new Bundle();
            result.putStringArrayList("names", names);
            result.putStringArrayList("numbers", numbers);
        }
        return result;
    }

    @Override
    public boolean updateUserCustomizedEccList(Bundle bundle) {
        mUserCustomizedEccList.clear();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mApp);
        Editor editor = sp.edit();
        int count = sp.getInt("ecc_count", 0);
        editor.putInt("ecc_count", 0);
        for (int i = 0; i < count; i++) {
            editor.remove("ecc_name" + i);
            editor.remove("ecc_number" + i);
        }
        if (bundle != null) {
            ArrayList<String> names = bundle.getStringArrayList("names");
            ArrayList<String> numbers = bundle.getStringArrayList("numbers");
            if (names != null && numbers != null && names.size() == numbers.size()) {
                editor.putInt("ecc_count", names.size());
                for (int i = 0; i < names.size(); i++) {
                    EccEntry entry = new EccEntry(names.get(i), numbers.get(i));
                    mUserCustomizedEccList.add(entry);
                    editor.putString("ecc_name" + i, names.get(i));
                    editor.putString("ecc_number" + i, numbers.get(i));
                }
            }
        }
        editor.commit();
        log("[updateUserCustomizedEccList] mUserCustomizedEccList: " + mUserCustomizedEccList);
        return true;
    }

    @Override
    public boolean isUserCustomizedEcc(String number) {
        if (number == null || mUserCustomizedEccList.size() == 0) {
            log("[isUserCustomizedEcc] No number or no customized number!");
            return false;
        }

        boolean isAirplaneModeOn = Settings.Global.getInt(mApp.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) > 0;
        if ("1".equals(SystemProperties.get("ro.mtk_flight_mode_power_off_md"))
                && isAirplaneModeOn) {
            log("[isUserCustomizedEcc] airplane mode on, return false!");
            return false;
        }

        int subIdCdma = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        int cdmaSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        TelephonyManager tm = TelephonyManager.getDefault();
        int simCount = tm.getSimCount();
        int tmpSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        boolean isCdmaSimInsert = false;
        for (int i = 0; i < simCount; i++) {
            int[] subIds = SubscriptionManager.getSubId(i);
            if (subIds != null && subIds.length > 0) {
                tmpSubId = subIds[0];
            }
            if (tm.getCurrentPhoneType(tmpSubId) == PhoneConstants.PHONE_TYPE_CDMA) {
                subIdCdma = tmpSubId;
                cdmaSlotId = i;
                break;
            }
        }
        if (subIdCdma != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            isCdmaSimInsert = tm.hasIccCard(cdmaSlotId);
        }

        // return false if no OMH card
        if (!isCdmaSimInsert || !isOmhCard(subIdCdma)) {
            log("[isUserCustomizedEcc] no OMH card return false, isCdmaSimInsert: "
                    + isCdmaSimInsert);
            return false;
        }

        log("[isUserCustomizedEcc] mUserCustomizedEccList: " + mUserCustomizedEccList);

        String numberPlus = null;
        String ecc = null;
        for (EccEntry entry : mUserCustomizedEccList) {
            ecc = entry.getEcc();
            numberPlus = ecc + "+";
            if (ecc.equals(number) || numberPlus.equals(number)) {
                return true;
            }
        }
        return false;
    }

    private void omhInit() {
        //Init opeartors list that support OMH.
        Context context = mPhone.getContext();
        mOmhOperators = context.getResources().getStringArray(
                com.mediatek.internal.R.array.operator_support_omh_list);
        //Create for OMH feature
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        sIccFileAdapter = new IccFileAdapter[numPhones];
        for (int i = 0; i < numPhones; i++) {
            Phone simPhone  =  PhoneFactory.getPhone(i);
            sIccFileAdapter[i] = new IccFileAdapter(mApp, simPhone);
        }
    }

    private IccFileAdapter getIccFileAdapterBySubId(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        return sIccFileAdapter[phoneId];
    }

    @Override
    public int[] getCallForwardingFc(int type, int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        int[] cf = null;
        switch (type) {
            case 1:
                cf = sIccFileAdapter[phoneId].getFcsForApp(3, 22, subId);
                break;
            case 2:
                cf = sIccFileAdapter[phoneId].getFcsForApp(3, 7, subId);
                break;
            case 3:
                cf = sIccFileAdapter[phoneId].getFcsForApp(8, 12, subId);
                break;
            case 4:
                cf = sIccFileAdapter[phoneId].getFcsForApp(13, 17, subId);
                break;
            case 5:
                cf = sIccFileAdapter[phoneId].getFcsForApp(18, 22, subId);
                break;
            default:
                log("getCallForwardingFc, invalid code.");
                break;
        }
        return cf;
    }

    @Override
    public int[] getCallWaitingFc(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        int[] cf = sIccFileAdapter[phoneId].getFcsForApp(23, 25, subId);
        return cf;
    }

    @Override
    public int[] getDonotDisturbFc(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        int[] cf = sIccFileAdapter[phoneId].getFcsForApp(30, 31, subId);
        return cf;
    }

    @Override
    public int[] getVMRetrieveFc(int subId) {
        log("getVMRetrieveFC");
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return null;
        }
        int[] cf = sIccFileAdapter[phoneId].getFcsForApp(38, 38, subId);
        return cf;
    }

    @Override
    public int getBcsmsCfgFromRuim(int subId, int userCategory, int userPriority) {
        log("getBcsmsCfgFromRuim");
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return -1;
        }
        int ret = sIccFileAdapter[phoneId].getBcsmsCfgFromRuim(userCategory, userPriority);
        return ret;
    }

    @Override
    public void saveMessageIdToCard(int subId) {
        log("getNextMessageId");
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return;
        }
        sIccFileAdapter[phoneId].saveMessageIdToCard();
    }

    @Override
    public int getWapMsgId(int subId) {
        log("getWapMsgId");
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return -1;
        }
        int ret = sIccFileAdapter[phoneId].getWapMsgId();
        return ret;
    }
    // [OMH] @}

    /**
     * Set phone radio type and access technology.
     *
     * @param rafs an RadioAccessFamily array to indicate all phone's
     *        new radio access family. The length of RadioAccessFamily
     *        must equal to phone count.
     * @return true if start setPhoneRat successfully.
     */
    @Override
    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        boolean ret = true;
        try {
            ((MtkProxyController)ProxyController.getInstance()).setRadioCapability(rafs);
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "setRadioCapability: Runtime Exception");
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }
    /**
     * Check if under capability switching.
     *
     * @return true if switching
     */
    public boolean isCapabilitySwitching() {
        return ((MtkProxyController)ProxyController.getInstance()).isCapabilitySwitching();
    }

    /**
     * Get main capability phone id.
     * @return The phone id with highest capability.
     */
    public int getMainCapabilityPhoneId() {
        return RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
    }

    /**
     * Get IMS registration state by given sub-id.
     * @param subId The subId for query
     * @return true if IMS is registered, or false
     * @hide
     */
    public boolean isImsRegistered(int subId) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            return false;
        }
        boolean result = phone.isImsRegistered();
        if (DBG) {
            log("isImsRegistered(" + subId + ")=" + result);
        }
        return result;
    }

    /**
     * Get Volte registration state by given sub-id.
     * @param subId The subId for query
     * @return true if volte is registered, or false
     * @hide
     */
    public boolean isVolteEnabled(int subId) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            return false;
        }
        boolean result = phone.isVolteEnabled();
        if (DBG) {
            log("isVolteEnabled=(" + subId + ")=" + result);
        }
        return result;
    }

    /**
     * Get WFC registration state by given sub-id.
     * @param subId The subId for query
     * @return true if wfc is registered, or false
     * @hide
     */
    public boolean isWifiCallingEnabled(int subId) {
        Phone phone = getPhone(subId);
        if (phone == null) {
            return false;
        }
        boolean result = phone.isWifiCallingEnabled();
        if (DBG) {
            log("isWifiCallingEnabled(" + subId + ")=" + result);
        }
        return result;
    }

    // MTK-START: SIM GBA / AUTH
    /**
     * Helper thread to turn async call to {@link #SimAuthentication} into
     * a synchronous one.
     */
    private static class SimAuth extends Thread {
        private Phone mTargetPhone;
        private boolean mDone = false;
        private IccIoResult mResponse = null;

        // For replies from SimCard interface
        private Handler mHandler;

        // For async handler to identify request type
        private static final int SIM_AUTH_GENERAL_COMPLETE = 300;

        public SimAuth(Phone phone) {
            mTargetPhone = phone;
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (SimAuth.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        switch (msg.what) {
                            case SIM_AUTH_GENERAL_COMPLETE:
                                log("SIM_AUTH_GENERAL_COMPLETE");
                                synchronized (SimAuth.this) {
                                    if (ar.exception != null) {
                                        log("SIM Auth Fail");
                                        mResponse = (IccIoResult) (ar.result);
                                    } else {
                                        mResponse = (IccIoResult) (ar.result);
                                    }
                                    log("SIM_AUTH_GENERAL_COMPLETE result is " + mResponse);
                                    mDone = true;
                                    SimAuth.this.notifyAll();
                                }
                                break;
                        }
                    }
                };
                SimAuth.this.notifyAll();
            }
            Looper.loop();
        }

        byte[] doGeneralSimAuth(int slotId, int family, int mode, int tag,
                String strRand, String strAutn) {
           synchronized (SimAuth.this) {
                while (mHandler == null) {
                    try {
                        SimAuth.this.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                mDone = false;
                mResponse = null;

                Message callback = Message.obtain(mHandler, SIM_AUTH_GENERAL_COMPLETE);
                int sessionId = ((MtkUiccController)UiccController.getInstance())
                        .getIccApplicationChannel(
                        slotId, family);
                log("family = " + family + ", sessionId = " + sessionId);

                int[] subId = SubscriptionManager.getSubId(slotId);
                if (subId == null) {
                    log("slotId = " + slotId + ", subId is invalid.");
                    return null;
                } else {
                    ((MtkGsmCdmaPhone)getPhone(subId[0])).doGeneralSimAuthentication
                            (sessionId, mode, tag, strRand, strAutn, callback);
                }

                while (!mDone) {
                    try {
                        log("wait for done");
                        SimAuth.this.wait();
                    } catch (InterruptedException e) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                }
                int len = 0;
                byte[] result = null;

                if (mResponse != null) {
                    // 2 bytes for sw1 and sw2
                    len = 2 + ((mResponse.payload == null) ? 0 : mResponse.payload.length);
                    result = new byte[len];

                    if (mResponse.payload != null) {
                        System.arraycopy(mResponse.payload, 0, result, 0, mResponse.payload.length);
                    }

                    result[len - 1] = (byte) mResponse.sw2;
                    result[len - 2] = (byte) mResponse.sw1;

                    // TODO: Should use IccUtils.bytesToHexString to print log info.
                    //for (int i = 0; i < len ; i++) {
                    //    log("Result = " + result[i]);
                    //}
                    //log("Result = " + new String(result));
                } else {
                    log("mResponse is null.");
                }

                log("done");
                return result;
            }
        }
    }
    // MTK-END
    // MTK-START: SIM GBA
    /**
     * Request to run AKA authenitcation on UICC card by indicated family.
     *
     * @param slotId indicated sim id
     * @param family indiacted family category
     *        UiccController.APP_FAM_3GPP =  1; //SIM/USIM
     *        UiccController.APP_FAM_3GPP2 = 2; //RUIM/CSIM
     *        UiccController.APP_FAM_IMS   = 3; //ISIM
     * @param byteRand random challenge in byte array
     * @param byteAutn authenication token in byte array
     *
     * @return reponse paramenters/data from UICC
     *
     */
    public byte[] simAkaAuthentication(int slotId, int family, byte[] byteRand, byte[] byteAutn) {
        enforcePrivilegedPhoneStatePermission();

        String strRand = "";
        String strAutn = "";
        log("simAkaAuthentication session is " + family + " simId " + slotId);

        if (byteRand != null && byteRand.length > 0) {
            strRand = IccUtils.bytesToHexString(byteRand).substring(0, byteRand.length * 2);
        }

        if (byteAutn != null && byteAutn.length > 0) {
            strAutn = IccUtils.bytesToHexString(byteAutn).substring(0, byteAutn.length * 2);
        }
        log("simAkaAuthentication Randlen " + strRand.length() + " strRand is "
                + strRand + ", AutnLen " + strAutn.length() + " strAutn " + strAutn);
        String akaData = Integer.toHexString(strRand.length()) + strRand +
                Integer.toHexString(strAutn.length()) + strAutn;
        if (DBG) {
            log("akaData: " + akaData);
        }


        int subId = getSubIdBySlot(slotId);
        int appType = PhoneConstants.APPTYPE_UNKNOWN;
        switch (family) {
            case 1:
                appType = PhoneConstants.APPTYPE_USIM;
                break;
            case 2:
                appType = PhoneConstants.APPTYPE_CSIM;
                break;
            case 3:
                appType = PhoneConstants.APPTYPE_ISIM;
                break;
        }
        if (appType == PhoneConstants.APPTYPE_UNKNOWN) {
            return null;
        } else {
            Context context = mPhone.getContext();
            String responseData = TelephonyManager.from(context).getIccAuthentication(
                       subId, appType, TelephonyManager.AUTHTYPE_EAP_SIM, akaData);
            return IccUtils.hexStringToBytes(responseData);
        }
    }

    /**
     * Request to run GBA authenitcation (Bootstrapping Mode)on UICC card
     * by indicated family.
     *
     * @param slotId indicated sim id
     * @param family indiacted family category
     *        UiccController.APP_FAM_3GPP =  1; //SIM/USIM
     *        UiccController.APP_FAM_3GPP2 = 2; //RUIM/CSIM
     *        UiccController.APP_FAM_IMS   = 3; //ISIM
     * @param byteRand random challenge in byte array
     * @param byteAutn authenication token in byte array
     *
     * @return reponse paramenters/data from UICC
     *
     */
    public byte[] simGbaAuthBootStrapMode(int slotId, int family, byte[] byteRand, byte[] byteAutn) {
        enforcePrivilegedPhoneStatePermission();

        if (mSimAuthThread == null) {
            log("simGbaAuthBootStrapMode new thread");
            mSimAuthThread = new SimAuth(mPhone);
            mSimAuthThread.start();
        } else {
            log("simGbaAuthBootStrapMode thread has been created.");
        }

        String strRand = "";
        String strAutn = "";
        log("simGbaAuthBootStrapMode session is " + family + " simId " + slotId);

        if (byteRand != null && byteRand.length > 0) {
            strRand = IccUtils.bytesToHexString(byteRand).substring(0, byteRand.length * 2);
        }

        if (byteAutn != null && byteAutn.length > 0) {
            strAutn = IccUtils.bytesToHexString(byteAutn).substring(0, byteAutn.length * 2);
        }
        log("simGbaAuthBootStrapMode strRand is " + strRand + " strAutn " + strAutn);

        return mSimAuthThread.doGeneralSimAuth(slotId, family, 1, 0xDD, strRand, strAutn);
    }

    /**
     * Request to run GBA authenitcation (NAF Derivation Mode)on UICC card
     * by indicated family.
     *
     * @param slotId indicated sim id
     * @param family indiacted family category
     *        UiccController.APP_FAM_3GPP =  1; //SIM/USIM
     *        UiccController.APP_FAM_3GPP2 = 2; //RUIM/CSIM
     *        UiccController.APP_FAM_IMS   = 3; //ISIM
     * @param byteNafId network application function id in byte array
     * @param byteImpi IMS private user identity in byte array
     *
     * @return reponse paramenters/data from UICC
     *
     */
    public byte[] simGbaAuthNafMode(int slotId, int family, byte[] byteNafId, byte[] byteImpi) {
        enforcePrivilegedPhoneStatePermission();

        if (mSimAuthThread == null) {
            log("simGbaAuthNafMode new thread");
            mSimAuthThread = new SimAuth(mPhone);
            mSimAuthThread.start();
        } else {
            log("simGbaAuthNafMode thread has been created.");
        }

        String strNafId = "";
        String strImpi = "";
        log("simGbaAuthNafMode session is " + family + " simId " + slotId);

        if (byteNafId != null && byteNafId.length > 0) {
            strNafId = IccUtils.bytesToHexString(byteNafId).substring(0, byteNafId.length * 2);
        }

        /* ISIM GBA NAF mode parameter should be NAF_ID.
         * USIM GAB NAF mode parameter should be NAF_ID + IMPI
         * If getIccApplicationChannel got 0, mean that ISIM not support */
        if (((MtkUiccController)UiccController.getInstance()).
                getIccApplicationChannel(slotId, family) == 0) {
            log("simGbaAuthNafMode ISIM not support.");
            if (byteImpi != null && byteImpi.length > 0) {
                strImpi = IccUtils.bytesToHexString(byteImpi).substring(0, byteImpi.length * 2);
            }
        }
        log("simGbaAuthNafMode NAF ID is " + strNafId + " IMPI " + strImpi);

        return mSimAuthThread.doGeneralSimAuth(slotId, family, 1, 0xDE, strNafId, strImpi);
    }
    // MTK-END

    // M: [LTE][Low Power][UL traffic shaping] @{
    public boolean setLteAccessStratumReport(boolean enabled) {
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager
                .getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null || phoneId != dataPhoneId) {
            loge("setLteAccessStratumReport incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) {
                    loge("setLteAccessStratumReport: MainPhoneId and dataPhoneId aren't the same");
                }
            }
            return false;
        }
        if (DBG) log("setLteAccessStratumReport: enabled = " + enabled);
        Boolean success = (Boolean) sendRequest(CMD_SET_LTE_ACCESS_STRATUM_STATE,
                new Boolean(enabled), new Integer(phoneId));
        if (DBG) log("setLteAccessStratumReport: success = " + success);
        return success;

    }

    public boolean setLteUplinkDataTransfer(boolean isOn, int timeMillis) {
        int state = 1;
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager
                .getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null || phoneId != dataPhoneId) {
            loge("setLteUplinkDataTransfer incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) {
                    loge("setLteUplinkDataTransfer: MainPhoneId and dataPhoneId aren't the same");
                }
            }
            return false;
        }
        if (DBG) {
            log("setLteUplinkDataTransfer: isOn = " + isOn
                    + ", Tclose timer = " + (timeMillis/1000));
        }
        if (!isOn) state = (timeMillis/1000) << 16 | 0;
        Boolean success = (Boolean) sendRequest(CMD_SET_LTE_UPLINK_DATA_TRANSFER_STATE,
                new Integer(state), new Integer(phoneId));
        if (DBG) log("setLteUplinkDataTransfer: success = " + success);
        return success;
    }

    public String getLteAccessStratumState() {
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager
                .getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        String state = MtkPhoneConstants.LTE_ACCESS_STRATUM_STATE_UNKNOWN;
        if (phone == null || phoneId != dataPhoneId) {
            loge("getLteAccessStratumState incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) {
                    loge("getLteAccessStratumState: MainPhoneId and dataPhoneId aren't the same");
                }
            }
        } else {
            MtkDcTracker dcTracker = (MtkDcTracker) phone.mDcTracker;
            state = dcTracker.getLteAccessStratumState();
        }
        if (DBG) log("getLteAccessStratumState: " + state);
        return state;
    }

    public boolean isSharedDefaultApn() {
        int phoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int dataPhoneId = SubscriptionManager
                .getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        Phone phone = PhoneFactory.getPhone(phoneId);
        boolean isSharedDefaultApn = false;
        if (phone == null || phoneId != dataPhoneId) {
            loge("isSharedDefaultApn incorrect parameter [getMainPhoneId = "
                    + RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()
                    + ", dataPhoneId = " + dataPhoneId + "]");
            if (phoneId != dataPhoneId) {
                if (DBG) loge("isSharedDefaultApn: MainPhoneId and dataPhoneId aren't the same");
            }
        } else {
            MtkDcTracker dcTracker = (MtkDcTracker) phone.mDcTracker;
            isSharedDefaultApn = dcTracker.isSharedDefaultApn();
        }
        if (DBG) log("isSharedDefaultApn: " + isSharedDefaultApn);
        return isSharedDefaultApn;
    }
    // M: [LTE][Low Power][UL traffic shaping] @}

    // PHB START
    /**
     * This function is used to get SIM phonebook storage information
     * by sim id.
     *
     * @param simId Indicate which sim(slot) to query
     * @return int[] which incated the storage info
     *         int[0]; // # of remaining entries
     *         int[1]; // # of total entries
     *         int[2]; // # max length of number
     *         int[3]; // # max length of alpha id
     *
     */
    public int[] getAdnStorageInfo(int subId) {
        Log.d(LOG_TAG, "getAdnStorageInfo " + subId);

        if (SubscriptionManager.isValidSubscriptionId(subId) == true) {
            if (mAdnInfoThread == null) {
                Log.d(LOG_TAG, "getAdnStorageInfo new thread ");
                mAdnInfoThread  = new QueryAdnInfoThread(subId);
                mAdnInfoThread.start();
            } else {
                mAdnInfoThread.setSubId(subId);
                Log.d(LOG_TAG, "getAdnStorageInfo old thread ");
            }
            return mAdnInfoThread.GetAdnStorageInfo();
        } else {
            Log.d(LOG_TAG, "getAdnStorageInfo subId is invalid.");
            int[] recordSize;
            recordSize = new int[4];
            recordSize[0] = 0; // # of remaining entries
            recordSize[1] = 0; // # of total entries
            recordSize[2] = 0; // # max length of number
            recordSize[3] = 0; // # max length of alpha id
            return recordSize;
        }
    }

    private static class QueryAdnInfoThread extends Thread {

        private int mSubId;
        private boolean mDone = false;
        private int[] recordSize;

        private Handler mHandler;

        // For async handler to identify request type
        private static final int EVENT_QUERY_PHB_ADN_INFO = 100;

        public QueryAdnInfoThread(int subId) {
            mSubId = subId;
        }
        public void setSubId(int subId) {
            synchronized (QueryAdnInfoThread.this) {
                mSubId = subId;
                mDone = false;
            }
        }

        @Override
        public void run() {
            Looper.prepare();
            synchronized (QueryAdnInfoThread.this) {
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        AsyncResult ar = (AsyncResult) msg.obj;

                        switch (msg.what) {
                            case EVENT_QUERY_PHB_ADN_INFO:
                                Log.d(LOG_TAG, "EVENT_QUERY_PHB_ADN_INFO");
                                synchronized (QueryAdnInfoThread.this) {
                                    mDone = true;
                                    int[] info = (int[]) (ar.result);
                                    if (info != null && info.length == 4) {
                                        recordSize = new int[4];
                                        recordSize[0] = info[0]; // # of remaining entries
                                        recordSize[1] = info[1]; // # of total entries
                                        recordSize[2] = info[2]; // # max length of number
                                        recordSize[3] = info[3]; // # max length of alpha id
                                        Log.d(LOG_TAG, "recordSize[0]=" + recordSize[0] +
                                                ",recordSize[1]=" + recordSize[1] +
                                                ",recordSize[2]=" + recordSize[2] +
                                                ",recordSize[3]=" + recordSize[3]);
                                    }
                                    else {
                                        recordSize = new int[4];
                                        recordSize[0] = 0; // # of remaining entries
                                        recordSize[1] = 0; // # of total entries
                                        recordSize[2] = 0; // # max length of number
                                        recordSize[3] = 0; // # max length of alpha id
                                    }
                                    QueryAdnInfoThread.this.notifyAll();

                                }
                                break;
                            }
                      }
                };
                QueryAdnInfoThread.this.notifyAll();
            }
            Looper.loop();
        }

        public int[] GetAdnStorageInfo() {
            synchronized (QueryAdnInfoThread.this) {
                while (mHandler == null) {
                    try {
                        QueryAdnInfoThread.this.wait();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                Message response = Message.obtain(mHandler, EVENT_QUERY_PHB_ADN_INFO);

                ((MtkGsmCdmaPhone)getPhone(mSubId)).queryPhbStorageInfo(MtkRILConstants.PHB_ADN, response);

                while (!mDone) {
                    try {
                        Log.d(LOG_TAG, "wait for done");
                        QueryAdnInfoThread.this.wait();
                    } catch (InterruptedException e) {
                        // Restore the interrupted status
                        Thread.currentThread().interrupt();
                    }
                }
                Log.d(LOG_TAG, "done");
                return recordSize;
            }
        }
    }

   /**
    * This function is used to check if the SIM phonebook is ready
    * by sub id.
    *
    * @param subId Indicate which sim(slot) to query
    * @return true if phone book is ready.
    *
    */
    public boolean isPhbReady(int subId) {
        int phoneId = SubscriptionManager.getPhoneId(subId);
        int slotId = SubscriptionManager.getSlotIndex(subId);
        boolean phbReady = false;

        if (SubscriptionManager.isValidSlotIndex(slotId) == true) {
            Phone phone = PhoneFactory.getPhone(phoneId);
            if (phone != null) {
                IccRecords iccRecords = phone.getIccRecords();
                if (iccRecords != null) {
                    if (iccRecords instanceof MtkSIMRecords) {
                        phbReady = ((MtkSIMRecords) iccRecords).isPhbReady();
                    } else if (iccRecords instanceof MtkRuimRecords) {
                        phbReady = ((MtkRuimRecords) iccRecords).isPhbReady();
                    }
                }
            }
        }

        return phbReady;
    }
    // PHB END

    private class RxTestObject {
        int result[] = null;
        Object lockObj = new Object();
    }

    public int[] setRxTestConfig(int phoneId, int config) {
        MtkGsmCdmaPhone phone = (MtkGsmCdmaPhone)PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            if (phone.mMtkCi != null) {
                RxTestObject RxTest = new RxTestObject();
                synchronized(RxTest.lockObj) {
                    phone.setRxTestConfig(config, mMainThreadHandler.obtainMessage(
                            EVENT_SET_RX_TEST_CONFIG, RxTest));
                    try {
                        RxTest.lockObj.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized(RxTest.lockObj) {
                    if (RxTest.result != null) {
                        if (DBG) log("setRxTestConfig return: " + RxTest.result);
                        return RxTest.result;
                    } else {
                        if (DBG) log("setRxTestConfig return: null");
                        return null;
                    }
                }
            } else {
                if (DBG) log("setRxTestConfig phone.mMtkCi = null");
            }
        } else {
            if (DBG) log("setRxTestConfig phone = null");
        }
        return null;
    }

    public int[] getRxTestResult(int phoneId) {
        MtkGsmCdmaPhone phone = (MtkGsmCdmaPhone)PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            if (phone.mMtkCi != null) {
                RxTestObject RxTest = new RxTestObject();
                synchronized(RxTest.lockObj) {
                    phone.getRxTestResult(mMainThreadHandler.obtainMessage(
                            EVENT_GET_RX_TEST_RESULT, RxTest));
                    try {
                        RxTest.lockObj.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                synchronized(RxTest.lockObj) {
                    if (RxTest.result != null) {
                        if (DBG) log("getRxTestResult return: " + RxTest.result);
                        return RxTest.result;
                    } else {
                        if (DBG) log("getRxTestResult return: null");
                        return null;
                    }
                }
            } else {
                if (DBG) log("getRxTestResult mMtkCi.mCi = null");
            }
        } else {
            if (DBG) log("getRxTestResult phone = null");
        }
        return null;
    }

    // M: [VzW] PCO based Self Activation @{
    public int selfActivationAction(int action, Bundle param, int subId) {
        // Default success
        int retVal = 0;
        ISelfActivation instance =
                ((MtkGsmCdmaPhone)getPhone(subId)).getSelfActivationInstance();
        if (instance != null) {
            retVal = instance.selfActivationAction(action, param);
        } else {
            if (DBG) log("null SelfActivation instance");
        }
        if (DBG) log("selfActivationAction: action = " +
                action + " subId = " + subId + " retVal = " + retVal);
        return retVal;
    }

    public int getSelfActivateState(int subId) {
        // Default value is STATE_NONE
        int retVal = 0;
        ISelfActivation instance =
                ((MtkGsmCdmaPhone)getPhone(subId)).getSelfActivationInstance();
        if (instance != null) {
            retVal = instance.getSelfActivateState();
        } else {
            if (DBG) log("null SelfActivation instance");
        }
        if (DBG) log("getSelfActivateState: subId = " + subId + " retVal = " + retVal);
        return retVal;
    }

    public int getPCO520State(int subId) {
        // Default value is FIVETOZERO_NONE
        int retVal = 0;
        ISelfActivation instance =
                ((MtkGsmCdmaPhone)getPhone(subId)).getSelfActivationInstance();
        if (instance != null) {
            retVal = instance.getPCO520State();
        } else {
            if (DBG) log("null SelfActivation instance");
        }
        if (DBG) log("getPCO520State: subId = " + subId + " retVal = " + retVal);
        return retVal;
    }
    // @}

    /// M: CC: ECC is in progress @{
    @Override
    public void setEccInProgress(boolean state) {
        mIsEccInProgress = state;
        log("setEccInProgress, mIsEccInProgress:" + mIsEccInProgress);
    }

    @Override
    public boolean isEccInProgress() {
        log("isEccInProgress, mIsEccInProgress:" + mIsEccInProgress);
        return mIsEccInProgress;
    }
    /// @}

    @Override
    public boolean exitEmergencyCallbackMode(int subId) {
        log("exitEmergencyCallbackMode, subId: " + subId);

        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            log("no corresponding phone id");
            return false;
        }
        EcmExitResult result = (EcmExitResult) sendRequest(EVENT_EXIT_ECBM_MODE_REQ, subId);
        if (result.getReceiver() != null) {
            log("unregisterReceiver " + result.getReceiver());
            mApp.unregisterReceiver(result.getReceiver());
        }
        return true;
    }

    @Override
    public void setApcModeUsingSlotId(int slotId, int mode,
                        boolean reportOn, int reportInterval) {
        log("setApcModeUsingSlotId, slotId:" + slotId + ", mode:" + mode +
             ", reportOn:" + reportOn + ", reportInterval:" + reportInterval);
        int subId = getSubIdBySlot(slotId);
        final Phone phone = getPhone(subId);
        if (phone != null) {
            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                ((MtkGsmCdmaPhone)phone).setApcMode(mode, reportOn, reportInterval);
            } else {
                log("setApcModeUsingSlotId: phone type is abnormal");
            }
        } else {
            log("setApcModeUsingSlotId, phone or subId: is null");
        }
    }

    @Override
    public PseudoCellInfo getApcInfoUsingSlotId(int slotId) {
        log("getApcInfoUsingSlotId, slotId:" + slotId);
        PseudoCellInfo info = null;
        int subId = getSubIdBySlot(slotId);
        final Phone phone = getPhone(subId);
        if (phone != null) {
            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                info = ((MtkGsmCdmaPhone)phone).getApcInfo();
            } else {
                log("getApcInfoUsingSlotId: phone type is abnormal");
            }
        } else {
            log("getApcInfoUsingSlotId, phone or subId: is null");
        }
        return info;
    }

    /**
     * Get CDMA subscription active status  by subId.
     * @param subId subId
     * @return active status. 1 is active, 0 is deactive
     */
    public int getCdmaSubscriptionActStatus(int subId) {
        int actStatus = 0;
        Phone p = getPhone(subId);
        if (p != null){
            if (DBG) {
                log("getCdmaSubscriptionActStatus, phone type " + p.getPhoneType());
            }
            if (p.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                actStatus = ((MtkGsmCdmaPhone)p).getCdmaSubscriptionActStatus();
            }
        } else {
            log("fail to getCdmaSubscriptionActStatus due to phone is null");
        }
        return actStatus;
    }

    public void setIsLastEccIms(boolean val) {
        if (DBG) log("setIsLastEccIms(): " + val);
        mIsLastEccIms = val;
    }

    public boolean getIsLastEccIms() {
        if (DBG) log("getIsLastEccIms(): " + mIsLastEccIms);
        return mIsLastEccIms;
    }

    public void setWifiEnabled(int pheonId, String ifName, int isEnabled) {
        log("[setWifiEnabled] pheonId:" + pheonId + ", ifName:" + ifName
                + ", isEnabled:" + isEnabled);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setWifiEnabled(ifName, isEnabled, null);
        } else {
            log("[setWifiEnabled] phone = null");
        }
    }

    public void setWifiFlightModeEnabled(int pheonId, String ifName, int isWifiEnabled,
            int isFlightModeOn) {
        log("[setWifiFlightModeEnabled] pheonId:" + pheonId + ", ifName:" + ifName
                + ", isWifiEnabled:" + isWifiEnabled + ", isFlightModeOn:" + isFlightModeOn);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setWifiFlightModeEnabled(ifName, isWifiEnabled,
                isFlightModeOn, null);
        } else {
            log("[setWifiFlightModeEnabled] phone = null");
        }
    }

    public void setWifiAssociated(int pheonId, String ifName,
            boolean associated, String ssid, String apMac) {
        log("[setWifiAssociated] pheonId:" + pheonId + ", ifName:" + ifName
                + ", associated:" + associated + ", ssid:" + ssid + ", apMac:" + apMac);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setWifiAssociated(ifName, associated, ssid, apMac, null);
        } else {
            log("[setWifiAssociated] phone = null");
        }
    }

    public void setWifiSignalLevel(int pheonId, int rssi, int snr) {
        log("[setWifiSignalLevel] pheonId:" + pheonId + ", rssi:" + rssi + ", snr:" + snr);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setWifiSignalLevel(rssi, snr, null);
        } else {
            log("[setWifiSignalLevel] phone = null");
        }
    }

    public void setWifiIpAddress(int pheonId, String ifName, String ipv4Addr,
            String ipv6Addr) {
        log("[setWifiIpAddress] pheonId:" + pheonId + ", ifName:" + ifName
                + ", ipv4Addr:" + ipv4Addr + ", ipv6Addr:" + ipv6Addr);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setWifiIpAddress(ifName, ipv4Addr, ipv6Addr, null);
        } else {
            log("[setWifiIpAddress] phone = null");
        }
    }

    public void setLocationInfo(int pheonId, String accountId, String broadcastFlag,
            String latitude, String longitude, String accuracy, String method, String city,
            String state, String zip, String countryCode) {
        log("[setLocationInfo] pheonId:" + pheonId);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setLocationInfo(accountId, broadcastFlag, latitude,
                    longitude, accuracy, method, city, state, zip, countryCode, null);
        } else {
            log("[setLocationInfo] phone = null");
        }
    }

    public void setLocationInfoWlanMac(int pheonId, String accountId, String broadcastFlag,
            String latitude, String longitude, String accuracy, String method, String city,
            String state, String zip, String countryCode, String ueWlanMac) {
        log("[setLocationInfoWifiMac] pheonId:" + pheonId);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setLocationInfoWlanMac(accountId, broadcastFlag, latitude,
                    longitude, accuracy, method, city, state, zip, countryCode, ueWlanMac, null);
        } else {
            log("[setLocationInfoWifiMac] phone = null");
        }
    }

    public void setEmergencyAddressId(int pheonId, String aid) {
        log("[setEmergencyAddressId] pheonId:" + pheonId + ", aid:" + aid);
        final Phone phone = PhoneFactory.getPhone(pheonId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setEmergencyAddressId(aid, null);
        } else {
            log("[setEmergencyAddressId] phone = null");
        }
    }

    @Override
    public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) {
        enforceModifyPermission();

        int returnValue = 0;
        try {
            AsyncResult result = (AsyncResult)sendRequest(CMD_INVOKE_OEM_RIL_REQUEST_RAW, oemReq);
            if(result.exception == null) {
                if (result.result != null) {
                    byte[] responseData = (byte[])(result.result);
                    if(responseData.length > oemResp.length) {
                        Log.w(LOG_TAG, "Buffer to copy response too small: Response length is " +
                                responseData.length +  "bytes. Buffer Size is " +
                                oemResp.length + "bytes.");
                    }
                    System.arraycopy(responseData, 0, oemResp, 0, responseData.length);
                    returnValue = responseData.length;
                }
            } else {
                CommandException ex = (CommandException) result.exception;
                returnValue = ex.getCommandError().ordinal();
                if(returnValue > 0) returnValue *= -1;
            }
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "sendOemRilRequestRaw: Runtime Exception");
            returnValue = (CommandException.Error.GENERIC_FAILURE.ordinal());
            if(returnValue > 0) returnValue *= -1;
        }

        return returnValue;
    }

    /**
     * Make sure the caller has the MODIFY_PHONE_STATE permission.
     *
     * @throws SecurityException if the caller does not have the required permission
     */
    private void enforceModifyPermission() {
        mApp.enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE, null);
    }

    public void setNattKeepAliveStatus(int phoneId, String ifName, boolean enable,
            String srcIp, int srcPort,
            String dstIp, int dstPort) {
        log("[setNattKeepAliveStatus] pheonId:" + phoneId);
        final Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setNattKeepAliveStatus(ifName, enable,
                srcIp, srcPort, dstIp, dstPort, null);
        } else {
            log("[setNattKeepAliveStatus] phone = null");
        }
    }

    public void setWifiPingResult(int phoneId, int rat, int latency, int pktloss) {
        log("[setWifiPingResult] phoneId:" + phoneId);
        final Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            ((MtkGsmCdmaPhone) phone).setWifiPingResult(rat, latency, pktloss, null);
        } else {
            log("[setWifiPingResult] phone = null");
        }
    }

    /*
     * For CDMA system UI display requirement. Check whether in CS call.
     */
    @Override
    public boolean isInCsCall(int phoneId) {
        log("[isInCsCall] phoneId:" + phoneId);
        final Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && phone.getCallTracker() != null) {
            return phone.getCallTracker().getState() != PhoneConstants.State.IDLE;
        }
        return false;
    }

    @Override
    public List<CellInfo> getAllCellInfo(int slotId, String callingPackage) {
        Phone phone = PhoneFactory.getPhone(slotId);
        if (phone == null) {
            return null;
        }
        if (!LocationAccessPolicy.canAccessCellLocation(phone.getContext(),
                callingPackage, Binder.getCallingUid(), "getAllCellInfo")) {
            return null;
        }

        if (DBG_LOC) {
            log("getAllCellInfo: is active user");
        }

        WorkSource workSource = getWorkSource(Binder.getCallingUid(), phone.getContext());
        return phone.getAllCellInfo(workSource);
    }

    private final WorkSource getWorkSource(int uid, Context context) {
        if (context == null) {
            return null;
        }

        final String packageName = context.getPackageManager().getNameForUid(uid);
        return new WorkSource(uid, packageName);
    }

    /*
     * Get current located PLMN from service state tracker
     */
    public String getLocatedPlmn(int phoneId) {
        String plmn = null;
        MtkGsmCdmaPhone phone = (MtkGsmCdmaPhone)PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            plmn = phone.getLocatedPlmn();
        }
        return plmn;
    }
}

