/*
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2011. All rights reserved.
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

package com.mediatek.contacts.simcontact;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.Manifest.permission;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.compat.CompatUtils;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkSubscriptionInfo;
import com.mediatek.internal.telephony.MtkSubscriptionManager;

import java.util.List;

public class SubInfoUtils {
    private static final String TAG = "SubInfoUtils";
    public static final String ICC_PROVIDER_PBR_URI = "content://icc/pbr/subId";
    public static final String ICC_PROVIDER_ADN_URI = "content://icc/adn/subId";
    private static final boolean MTK_GEMINI_SUPPORT =
            TelephonyManager.getDefault().getSimCount() > 1;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
        // "Contacts" group. Without this permission, the Contacts app is useless.
        permission.READ_CONTACTS,
        // "Phone" group. This is only used in a few places such as QuickContactActivity and
        // ImportExportDialogFragment. We could work around missing this permission with a bit
        // of work.
        permission.READ_CALL_LOG,
        /// M: The basic permissions of Contacts. If not have, Contacts can't be used. @{
        permission.READ_PHONE_STATE,
        permission.WRITE_CONTACTS,
        permission.CALL_PHONE,
        permission.GET_ACCOUNTS
        /// @}
    };

    public static int[] getActiveSubscriptionIdList() {
        Context context = GlobalEnv.getApplicationContext();
        // If has no basic permission of Phone, it shouldn't call getActiveSubscriptionIdList.
        if (PermissionsUtil.hasPermission(context, REQUIRED_PERMISSIONS)) {
            return SubscriptionManager.from(context).getActiveSubscriptionIdList();
        } else {
            Log.w(TAG, "getActiveSubscriptionIdList has no basic permissions!");
            return null;
        }
    }

    public static boolean iconTintChange(int iconTint, int subId) {
        Log.d(TAG, "[iconTintChange] iconTint = " + iconTint + ",subId = " + subId);
        boolean isChanged = true;
        List<SubscriptionInfo> activeList = getActivatedSubInfoList();
        if (activeList == null) {
            isChanged = false;
            return isChanged;
        }
        // TODO:: Check here,it may cause performance poor than L0
        for (SubscriptionInfo subInfo : activeList) {
            if (subInfo.getSubscriptionId() == subId && iconTint == subInfo.getIconTint()) {
                isChanged = false;
                break;
            }
        }
        return isChanged;
    }

    public static int getColorUsingSubId(int subId) {
        if (!checkSubscriber(subId)) {
            return -1;
        }
        SubscriptionInfo subscriptionInfo = getSubInfoUsingSubId(subId);
        return subscriptionInfo == null ? -1 : subscriptionInfo.getIconTint();
    }

    public static boolean checkSubscriber(int subId) {
        if (subId < 1) {
            Log.w(TAG, "[checkSubscriber], invalid subId: " + subId);
            return false;
        }
        return true;
    }

    /// M: Add for ALPS03507042. @{
    public static MtkSubscriptionInfo getSubscriptionInfo(int subId) {
        Context context = GlobalEnv.getApplicationContext();
        // If has no basic permission of Phone, it shouldn't call getActiveSubscriptionIdList.
        if (PermissionsUtil.hasPermission(context, REQUIRED_PERMISSIONS)) {
            return MtkSubscriptionManager.getSubInfo(context.getPackageName(),subId);
        } else {
            Log.w(TAG, "getActiveSubscriptionIdList has no basic permissions!");
            return null;
        }
    }
    /// @}


    public static Bitmap getIconBitmap(int subId) {
        if (!checkSubscriber(subId)) {
            return null;
        }
        /// M: Add for ALPS03507042. @{
        MtkSubscriptionInfo info = getSubscriptionInfo(subId);
        return info == null ? null : info.createIconBitmap(GlobalEnv
                    .getApplicationContext(), -1, MTK_GEMINI_SUPPORT);

       // SubscriptionInfo subscriptionInfo = getSubInfoUsingSubId(subId);
       // return subscriptionInfo == null ? null : subscriptionInfo.createIconBitmap(GlobalEnv
       // .getApplicationContext());
        /// @}
    }

    public static Uri getIccProviderUri(int subId) {
        if (!checkSubscriber(subId)) {
            return null;
        }
        if (isUsimOrCsimType(subId)) {
            return ContentUris.withAppendedId(Uri.parse(ICC_PROVIDER_PBR_URI), subId);
        } else {
            return ContentUris.withAppendedId(Uri.parse(ICC_PROVIDER_ADN_URI), subId);
        }
    }

    /*/ freeme.zhaozehong, 20180224. for freemeOS, UI redesign
    private static List<SubscriptionInfo> getActivatedSubInfoList() {
    /*/
    public static List<SubscriptionInfo> getActivatedSubInfoList() {
    //*/
      Context context = GlobalEnv.getApplicationContext();
      // If has no basic permission of Phone, it shouldn't call getActiveSubscriptionInfoList.
      if (PermissionsUtil.hasPermission(context, REQUIRED_PERMISSIONS)) {
          return SubscriptionManager.from(context).getActiveSubscriptionInfoList();
      } else {
          Log.w(TAG, "getActivatedSubInfoList has no basic permissions!");
          return null;
      }
    }

    private static SubscriptionInfo getSubInfoUsingSubId(int subId) {
      if (!checkSubscriber(subId)) {
          return null;
      }
      List<SubscriptionInfo> subscriptionInfoList = getActivatedSubInfoList();
      if (subscriptionInfoList != null && subscriptionInfoList.size() > 0) {
          for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
              if (subscriptionInfo.getSubscriptionId() == subId) {
                  return subscriptionInfo;
              }
          }
      }
      return null;
    }

    //copy from SimCardUtils
    public interface SimType {
      public static final String SIM_TYPE_USIM_TAG = "USIM";
      public static final String SIM_TYPE_SIM_TAG = "SIM";
      public static final String SIM_TYPE_RUIM_TAG = "RUIM";
      public static final String SIM_TYPE_CSIM_TAG = "CSIM";

      public static final int SIM_TYPE_SIM = 0;
      public static final int SIM_TYPE_USIM = 1;
      public static final int SIM_TYPE_RUIM = 2;
      public static final int SIM_TYPE_CSIM = 3;
      public static final int SIM_TYPE_UNKNOWN = -1;
    }

    private static final String SIM_KEY_WITHSLOT_IS_USIM = "isSimUsimType";
    private static final String IMTKTELEPHONYEX = "com.mediatek.internal.telephony.IMtkTelephonyEx";
    /**
     * [Gemini+] check whether a slot is insert a usim or csim card
     *
     * @param subId
     * @return true if it is usim or csim card
     */
    private static boolean isUsimOrCsimType(int subId) {
        boolean isUsimOrCsim = false;
        if (!CompatUtils.isClassAvailable(IMTKTELEPHONYEX)) {
            Log.d(TAG, "[isUsimOrCsimType] IMtkTelephonyEx not available");
            return isUsimOrCsim;
        }
        final IMtkTelephonyEx iTel = IMtkTelephonyEx.Stub.asInterface(ServiceManager
                .getService("phoneEx"));
        if (iTel == null) {
            Log.w(TAG, "[isUsimOrCsimType]iTel == null");
            return isUsimOrCsim;
        }
        try {
            if (SimType.SIM_TYPE_USIM_TAG.equals(iTel.getIccCardType(subId))
                    || SimType.SIM_TYPE_CSIM_TAG.equals(iTel.getIccCardType(subId))) {
                isUsimOrCsim = true;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "[isUsimOrCsimType]catch exception:");
            e.printStackTrace();
        }
        Log.d(TAG, "[isUsimOrCsimType]subId:" + subId + ",isUsimOrCsim:" + isUsimOrCsim);
        return isUsimOrCsim;
    }

    //*/ freeme.zhaozehong, 20180224. for freemeOS, UI redesign
    public static int getInvalidSubId() {
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    public static int getInvalidSlotId() {
        return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    }

    public static int getActivatedSubInfoCount() {
        Context context = GlobalEnv.getApplicationContext();
        return SubscriptionManager.from(context).getActiveSubscriptionInfoCount();
    }

    public static int getSlotIdUsingSubId(int subId) {
        if (!checkSubscriber(subId)) {
            return -1;
        }
        SubscriptionInfo info = getSubInfoUsingSubId(subId);
        return info == null ? getInvalidSlotId() : info.getSimSlotIndex();
    }

    public static final String MTK_PHONE_BOOK_SERVICE_NAME = "mtksimphonebook";

    public static String getMtkPhoneBookServiceName() {
        return MTK_PHONE_BOOK_SERVICE_NAME;
    }
    //*/
}
