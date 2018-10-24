/* Copyright Statement:
*
* This software/firmware and related documentation ("MediaTek Software") are
* protected under relevant copyright laws. The information contained herein
* is confidential and proprietary to MediaTek Inc. and/or its licensors.
* Without the prior written permission of MediaTek inc. and/or its licensors,
* any reproduction, modification, use or disclosure of MediaTek Software,
* and information contained herein, in whole or in part, shall be strictly prohibited.
*
* MediaTek Inc. (C) 2014. All rights reserved.
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
*/
package com.mediatek.dialer.calllog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.dialer.R;
import com.android.dialer.app.calllog.CallLogAdapter;
import com.android.dialer.app.calllog.CallLogAdapter.CallFetcher;
import com.android.dialer.app.calllog.CallLogAdapter.MultiSelectRemoveView;
import com.android.dialer.app.calllog.CallLogAdapter.OnActionModeStateChangedListener;
import com.android.dialer.app.calllog.calllogcache.CallLogCache;
import com.android.dialer.app.calllog.CallLogGroupBuilder;
import com.android.dialer.app.calllog.CallLogListItemViewHolder;
import com.android.dialer.app.calllog.IntentProvider;
import com.android.dialer.app.contactinfo.ContactInfoCache;
import com.android.dialer.blocking.FilteredNumberAsyncQueryHandler;
import com.android.dialer.calldetails.CallDetailsEntryViewHolder;
import com.android.dialer.calldetails.CallDetailsFooterViewHolder;
import com.android.dialer.calldetails.CallDetailsHeaderViewHolder;
import com.android.dialer.calldetails.CallDetailsEntries;
import com.android.dialer.calldetails.CallDetailsEntries.CallDetailsEntry;
import com.android.dialer.calllogutils.CallTypeHelper;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.dialercontact.DialerContact;
import com.android.dialer.lightbringer.LightbringerComponent;
import com.android.dialer.phonenumbercache.CallLogQuery;
import com.android.dialer.util.DialerUtils;
import com.mediatek.dialer.util.DialerVolteUtils;
import com.mediatek.provider.MtkCallLog.ConferenceCalls;

import java.util.ArrayList;

/**
 * M: [VoLTE ConfCall] The Volte Conference call member list adapter
 */
public class ConfCallMemberListAdapter extends CallLogAdapter {
    private final static String TAG = "ConfCallMemberListAdapter";
    private final static int VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER = 50;
    private final static int VIEW_TYPE_CALL_HISTORY_LIST_ITEM = 51;
    private final static int VIEW_TYPE_CALL_DETAIL_HEADER = 52;

    //private CallDetailHistoryAdapter mCallDetailHistoryAdapter;
    private PhoneCallDetails[] mConferenceCallDetails;
    private CallDetailsEntry mCallDetailsEntry;
    private ArrayList<String> mConfNumbers;
    private Context mContext;
    private final DialerContact contact;
    private final CallTypeHelper mCallTypeHelper;

    public ConfCallMemberListAdapter(Activity context, DialerContact contact,
        ContactInfoCache contactInfoCache) {
        super(context, /*alertContainer*/null,
            new CallFetcher() {
                @Override
                public void fetchCalls() {
                  // Do nothings
                }
            },
            /*MultiSelectRemoveView*/ null,
            /*OnActionModeStateChangedListener*/ null,
            new CallLogCache(context),
            contactInfoCache, /*voicemailPlaybackPresenter*/ null,
            new FilteredNumberAsyncQueryHandler(context),
            ACTIVITY_TYPE_CALL_LOG);
        mContext = context;
        this.contact = contact;
        mCallTypeHelper = new CallTypeHelper(context.getResources());
        setIsConfCallMemberList(true);
    }

    public void setConferenceCallDetails(PhoneCallDetails[] callDetails) {
        mConferenceCallDetails = callDetails;
        mConfNumbers = new ArrayList<String>();
        for (int i = 0; i < callDetails.length; i++) {
            if (!TextUtils.isEmpty(callDetails[i].number)) {
                mConfNumbers.add(callDetails[i].number.toString());
            }
        }
        mCallDetailsEntry = generateConfCallDetailsEntry(callDetails);
    }

    private CallDetailsEntry generateConfCallDetailsEntry(PhoneCallDetails[] details) {
        LogUtil.d(TAG, "generateConfCallDetailsEntry");
        CallDetailsEntry.Builder entry =
            CallDetailsEntry.newBuilder();
        if (details == null || details.length < 1) {
            return entry.build();
        }
        long minDate = details[0].date;
        long conferenceDuration = 0l;
        Long sumDataUsage = null;
        for (PhoneCallDetails detail : details) {
            if (minDate > detail.date) {
                minDate = detail.date;
            }
            if (null != detail.dataUsage) {
                if (sumDataUsage == null) {
                    sumDataUsage = 0L;
                }
                sumDataUsage += detail.dataUsage;
            }
        }

      LogUtil.d(TAG, "generateConferenceCallDetails begine get duration");
        Cursor cursor = mContext.getContentResolver().query(
                ConferenceCalls.CONTENT_URI,
                        new String[] { ConferenceCalls.CONFERENCE_DURATION },
                                ConferenceCalls._ID + "=?",
                                new String[] { String.valueOf(details[0].conferenceId)},
                                        null);
        if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                conferenceDuration = cursor.getLong(cursor.getColumnIndexOrThrow(
                ConferenceCalls.CONFERENCE_DURATION));
                LogUtil.d(TAG, "generateConferenceCallDetails conferenceDuration:"
                          + conferenceDuration);
        }
        if (cursor != null) {
                cursor.close();
        }
        LogUtil.d(TAG, "generateConferenceCallDetails get duration end ");
        entry.setCallId(details[0].conferenceId);
        entry.setCallType(details[0].callTypes[0]);
        if (sumDataUsage != null) {
            entry.setDataUsage(sumDataUsage);
        }
        entry.setDate(minDate);
        entry.setDuration(conferenceDuration);
        entry.setFeatures(details[0].features);

        return entry.build();
    }

    @Override
    protected void addGroups(Cursor cursor) {
        if (cursor.getCount() == 0) {
            return;
        }
        // Clear any previous day grouping information.
        clearDayGroups();
        // Reset cursor to start before the first row
        cursor.moveToPosition(-1);
        // Create an individual group for each calllog
        while (cursor.moveToNext()) {
            addGroup(cursor.getPosition(), 1);
            setDayGroup(cursor.getLong(CallLogQuery.ID),
                    CallLogGroupBuilder.DAY_GROUP_TODAY);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        // If there was no calllog items, do not shown headers
        if (super.getItemCount() == 0) {
            return 0;
        }
        // Add conference call detail header, history list header and item views
        return super.getItemCount() + 3;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_CALL_DETAIL_HEADER;
        } else if (position == getItemCount() - 1) {
            return VIEW_TYPE_CALL_HISTORY_LIST_ITEM;
        } else if (position == getItemCount() - 2) {
            return VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER;
        }
        return super.getItemViewType(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_CALL_DETAIL_HEADER) {
          return new CallDetailsHeaderViewHolder(
              inflater.inflate(R.layout.contact_container, parent, false));
        } else if (viewType == VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER) {
            return CallHistoryViewHolder.createHeader(mContext, parent);
        } else if (viewType == VIEW_TYPE_CALL_HISTORY_LIST_ITEM) {
          return new CallDetailsEntryViewHolder(
              inflater.inflate(R.layout.call_details_entry, parent, false));
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    protected void bindCallLogListViewHolder(ViewHolder viewHolder, int position) {
        Log.d(TAG, "bindCallLogListViewHolder(), viewHolder = " + viewHolder
                + " position = " + position);
        // Conference call detail header, history list header and item views
        if (getItemViewType(position) == VIEW_TYPE_CALL_DETAIL_HEADER
                && mConferenceCallDetails != null) {
          ((CallDetailsHeaderViewHolder) viewHolder).updateContactInfo(contact, mConfNumbers);
            return;
        } else if (getItemViewType(position) == VIEW_TYPE_CALL_HISTORY_LIST_ITEM_HEADER) {
            return;
        } else if (getItemViewType(position) == VIEW_TYPE_CALL_HISTORY_LIST_ITEM) {
            CallDetailsEntryViewHolder holder = (CallDetailsEntryViewHolder) viewHolder;
            holder.setCallDetails(
                contact.getNumber(),
                mCallDetailsEntry,
                mCallTypeHelper,
                false);
            return;
        } else {
            // The first position is call detail header
            position = position - 1;
            //bind Member List
            super.bindCallLogListViewHolder(viewHolder, position);
        }
    }


    @Override
    protected void render(CallLogListItemViewHolder views, PhoneCallDetails details, long rowId) {
        // Conference member list title
        views.dayGroupHeaderText = mContext.getString(R.string.conf_call_member_list);
        super.render(views, details, rowId);
    }

    // Call history list item view holder
    static class CallHistoryViewHolder extends RecyclerView.ViewHolder {
        public View view;

        private CallHistoryViewHolder(final Context context, View view) {
            super(view);
            this.view = view;
        }

        public static CallHistoryViewHolder createHeader(Context context, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.call_detail_history_header, parent, false);
            return new CallHistoryViewHolder(context, view);
        }
    }
}
