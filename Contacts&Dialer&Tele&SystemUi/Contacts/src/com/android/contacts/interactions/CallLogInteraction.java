/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.interactions;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telecom.PhoneAccountHandle;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.TextDirectionHeuristics;

import com.android.contacts.GeoUtil;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.util.BitmapUtil;
import com.android.contacts.util.ContactDisplayUtils;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.quickcontact.PhoneAccountUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;

import com.freeme.contacts.common.utils.FreemeDateTimeUtils;

/**
 * Represents a call log event interaction, wrapping the columns in
 * {@link android.provider.CallLog.Calls}.
 *
 * This class does not return log entries related to voicemail or SIP calls. Additionally,
 * this class ignores number presentation. Number presentation affects how to identify phone
 * numbers. Since, we already know the identity of the phone number owner we can ignore number
 * presentation.
 *
 * As a result of ignoring voicemail and number presentation, we don't need to worry about API
 * version.
 */
public class CallLogInteraction implements ContactInteraction {

    private static final String URI_TARGET_PREFIX = "tel:";
    private static final int CALL_LOG_ICON_RES = R.drawable.quantum_ic_phone_vd_theme_24;
    private static final int CALL_ARROW_ICON_RES = R.drawable.ic_call_arrow;
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();

    /* M: [SIM IND] add sim indicator @{ */
    private static final String TAG = "CallLogInteraction";
    private static final int SIM_ICON_RES = R.drawable.sim_indicator_orange_small;
    private PhoneAccountHandle mPhoneAccountHandle ;
    /* @ } */

    private ContentValues mValues;

    public CallLogInteraction(ContentValues values) {
        mValues = values;
        /* M: [SIM IND] add sim indicator @{ */
        initPhoneAccount();
        /* @ } */
    }

    @Override
    public Intent getIntent() {
        String number = getNumber();
        return number == null ? null : new Intent(Intent.ACTION_CALL).setData(
                Uri.parse(URI_TARGET_PREFIX + number));
    }

    @Override
    public String getViewHeader(Context context) {
        String number = mValues.getAsString(Calls.NUMBER);
        if (number != null) {
            number = PhoneNumberUtilsCompat.formatNumber(number,
                    PhoneNumberUtilsCompat.normalizeNumber(number),
                    GeoUtil.getCurrentCountryIso(context));
            return sBidiFormatter.unicodeWrap(number, TextDirectionHeuristics.LTR);
        }
        return null;
    }

    @Override
    public long getInteractionDate() {
        Long date = getDate();
        return date == null ? -1 : date;
    }

    @Override
    public String getViewBody(Context context) {
        Integer numberType = getCachedNumberType();
        if (numberType == null) {
            return null;
        }
        return Phone.getTypeLabel(context.getResources(), getCachedNumberType(),
                getCachedNumberLabel()).toString();
    }

    @Override
    public String getViewFooter(Context context) {
        final Long date = getDate();
        if (date != null) {
            final StringBuilder callDetail = new StringBuilder();
            /*/ freeme.liqiang, 20180129, stranger call details
            callDetail.append(ContactInteractionUtil.formatDateStringFromTimestamp(date, context));
            final Long duration = getDuration();
            if (duration != null) {
                callDetail.append("\n");
                callDetail.append(ContactInteractionUtil.formatDuration(duration, context));
            }
            /*/
            callDetail.append(FreemeDateTimeUtils.formatDateStringFromTimestamp(date, context));
            //*/
            return callDetail.toString();
        }
        return null;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getResources().getDrawable(CALL_LOG_ICON_RES);
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        Drawable callArrow = null;
        Resources res = context.getResources();
        Integer type = getType();
        if (type == null) {
            return null;
        }
        switch (type) {
            case Calls.INCOMING_TYPE:
                callArrow = res.getDrawable(CALL_ARROW_ICON_RES);
                callArrow.mutate().setColorFilter(res.getColor(R.color.call_arrow_green),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.MISSED_TYPE:
                callArrow = res.getDrawable(CALL_ARROW_ICON_RES);
                callArrow.mutate().setColorFilter(res.getColor(R.color.call_arrow_red),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.OUTGOING_TYPE:
                callArrow = BitmapUtil.getRotatedDrawable(res, CALL_ARROW_ICON_RES, 180f);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green),
                        PorterDuff.Mode.MULTIPLY);
                break;
        }
        /// M: @{
        callArrow = ExtensionManager.getInstance().getOp01Extension().getArrowIcon(type, callArrow);
        /// @}
        return callArrow;
    }

    /* M: [SIM IND] add sim indicator @{ */
    private void initPhoneAccount() {
        String accountName = mValues.getAsString(Calls.PHONE_ACCOUNT_COMPONENT_NAME);
        String accountId = mValues.getAsString(Calls.PHONE_ACCOUNT_ID);
        Log.d(TAG, "[initPhoneAccount] accountName: " + accountName + ",accountId: " + accountId);
        mPhoneAccountHandle = PhoneAccountUtils.getAccount(accountName, accountId);
    }

    public Drawable getSimIcon(Context context) {
        Drawable accountIcon = PhoneAccountUtils.getAccountIcon(context, mPhoneAccountHandle);
        Log.d(TAG, "[getSimIcon] account icon: " + accountIcon);
        return accountIcon;
    }

    public String getSimName(Context context) {
        String accountName = PhoneAccountUtils.getAccountLabel(context, mPhoneAccountHandle);
        Log.d(TAG, "[getSimName] accountName: " + accountName);
        return accountName;
    }
    /* @ } */

    public String getCachedName() {
        return mValues.getAsString(Calls.CACHED_NAME);
    }

    public String getCachedNumberLabel() {
        return mValues.getAsString(Calls.CACHED_NUMBER_LABEL);
    }

    public Integer getCachedNumberType() {
        return mValues.getAsInteger(Calls.CACHED_NUMBER_TYPE);
    }

    public Long getDate() {
        return mValues.getAsLong(Calls.DATE);
    }

    public Long getDuration() {
        return mValues.getAsLong(Calls.DURATION);
    }

    public Boolean getIsRead() {
        return mValues.getAsBoolean(Calls.IS_READ);
    }

    public Integer getLimitParamKey() {
        return mValues.getAsInteger(Calls.LIMIT_PARAM_KEY);
    }

    public Boolean getNew() {
        return mValues.getAsBoolean(Calls.NEW);
    }

    public String getNumber() {
        final String number = mValues.getAsString(Calls.NUMBER);
        return number == null ? null :
            sBidiFormatter.unicodeWrap(number, TextDirectionHeuristics.LTR);
    }

    public Integer getNumberPresentation() {
        return mValues.getAsInteger(Calls.NUMBER_PRESENTATION);
    }

    public Integer getOffsetParamKey() {
        return mValues.getAsInteger(Calls.OFFSET_PARAM_KEY);
    }

    public Integer getType() {
        return mValues.getAsInteger(Calls.TYPE);
    }

    @Override
    public Spannable getContentDescription(Context context) {
        final String phoneNumber = getViewHeader(context);
        final String contentDescription = context.getResources().getString(
                R.string.content_description_recent_call,
                getCallTypeString(context), phoneNumber, getViewFooter(context));
        return ContactDisplayUtils.getTelephoneTtsSpannable(contentDescription, phoneNumber);
    }

    private String getCallTypeString(Context context) {
        String callType = "";
        Resources res = context.getResources();
        Integer type = getType();
        if (type == null) {
            return callType;
        }
        switch (type) {
            case Calls.INCOMING_TYPE:
                callType = res.getString(R.string.content_description_recent_call_type_incoming);
                break;
            case Calls.MISSED_TYPE:
                callType = res.getString(R.string.content_description_recent_call_type_missed);
                break;
            case Calls.OUTGOING_TYPE:
                callType = res.getString(R.string.content_description_recent_call_type_outgoing);
                break;
        }
        return callType;
    }

    @Override
    public int getIconResourceId() {
        return CALL_LOG_ICON_RES;
    }

    //*/ freeme.liqiang, 20180129, stranger call details
    @Override
    public String getViewCallType(Context context) {
        Resources res = context.getResources();
        Integer type = getType();
        String callType = res.getString(R.string.freeme_recent_call_type_incoming);
        if (type == null) {
            return callType;
        }
        switch (type) {
            case Calls.INCOMING_TYPE:
                callType = res.getString(R.string.freeme_recent_call_type_incoming);
                break;
            case Calls.MISSED_TYPE:
                callType = res.getString(R.string.freeme_recent_call_type_incoming);
                break;
            case Calls.OUTGOING_TYPE:
                callType = res.getString(R.string.freeme_recent_call_type_outgoing);
                break;
        }
        return callType;
    }

    @Override
    public String getMissed(Context context) {
        Resources res = context.getResources();
        Integer type = getType();
        final Long duration = getDuration();
        String recentSubheader = res.getString(R.string.freeme_recent_call_type_missing);
        switch (type) {
            case Calls.INCOMING_TYPE:
                recentSubheader = String.valueOf(FreemeDateTimeUtils.formatDurationAndDataUsage(context, duration, 0));
                break;
            case Calls.OUTGOING_TYPE:
                if (duration > 0) {
                    recentSubheader = String.valueOf(FreemeDateTimeUtils.formatDurationAndDataUsage(context, duration, 0));
                } else {
                    recentSubheader = res.getString(R.string.freeme_recent_call_type_unconnected);
                }
                break;
            case Calls.MISSED_TYPE:
                recentSubheader = res.getString(R.string.freeme_recent_call_type_missing);
                break;
            case Calls.REJECTED_TYPE:
                recentSubheader = res.getString(R.string.freeme_recent_call_type_rejected);
                break;
        }
        return recentSubheader;
    }
    //*/
}
