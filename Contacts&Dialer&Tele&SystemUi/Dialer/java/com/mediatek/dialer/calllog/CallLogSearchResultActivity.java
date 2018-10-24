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
package com.mediatek.dialer.calllog;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.R;
import com.android.dialer.app.calllog.CallLogFragment;
import com.android.dialer.app.calllog.CallLogAsyncTaskUtil;
import com.android.dialer.app.calllog.CallLogAsyncTaskUtil.CallLogAsyncTaskListener;
import com.android.dialer.calldetails.CallDetailsActivity;
import com.android.dialer.calldetails.CallDetailsEntries;
import com.android.dialer.calldetails.CallDetailsEntries.CallDetailsEntry;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.common.Assert;
import com.android.dialer.database.CallLogQueryHandler;
import com.android.dialer.dialercontact.DialerContact;
import com.android.dialer.util.TransactionSafeActivity;

import com.mediatek.dialer.util.DialerConstants;
import com.mediatek.dialer.util.DialerFeatureOptions;

/**
 * M: [Dialer Global Search] Displays a list of call log entries.
 */
public class CallLogSearchResultActivity extends TransactionSafeActivity {
    private static final String TAG = "CallLogSearchResultActivity";
    private static final int MENU_ITEM_DELETE_ALL = 1;

    private ViewGroup mSearchResult;
    private TextView mSearchResultFor;
    private TextView mSearchResultFound;
    private String mData;
    private CallLogFragment mCallLogFragment;
    private Context mContext;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!DialerFeatureOptions.DIALER_GLOBAL_SEARCH) {
            finish();
        }

        // View action, start CallDetailActivity view CallLog.
        final Intent intent = this.getIntent();
        /// M: Put start activity operation in a function
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            viewCallDetail(intent);
        }

        setContentView(R.layout.mtk_call_log_search_activity);
        configureActionBar();

        mSearchResult = (LinearLayout) findViewById(R.id.calllog_search_result);
        mSearchResultFor = (TextView) findViewById(R.id.calllog_search_results_for);
        mSearchResultFound = (TextView) findViewById(R.id.calllog_search_results_found);

        mSearchResult.setVisibility(View.VISIBLE);
        mSearchResultFor.setText(Html.fromHtml(getString(R.string.search_results_for, "<b>" + mData
                + "</b>")));
        mSearchResultFound.setText(getString(R.string.search_results_searching));

        mContext = this;
        mCallLogFragment = new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL, true);
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.calllog_list_fragment, mCallLogFragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onResume() {
        super.onResume();
        /** M: When query data changed, update the search result's display. @{ */
        String tempQueryData = this.getIntent().getStringExtra(SearchManager.USER_QUERY);
        if (mData == null || !mData.equals(tempQueryData)) {
            Log.d(TAG, "[Dialer Global Search] QueryData changed, the query data is "
                    + tempQueryData);
            mData = tempQueryData;
            mCallLogFragment.setQueryData(mData);
            mCallLogFragment.fetchCalls();
        }
        /** @} */
        mSearchResultFor.setText(Html.fromHtml(getString(R.string.search_results_for, "<b>" + mData
                + "</b>")));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
        /**
         * Add for the issue: when the activity in the search result state, and
         * the activtiy is not finished, then click an item in the QSB to view a
         * call log, the interface is not call detail activty. @{
         */
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            viewCallDetail(this.getIntent());
        }
        /** @} */
    }

    private void configureActionBar() {
        Log.d(TAG, "configureActionBar()");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar
                    .setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ITEM_DELETE_ALL, 0, R.string.recentCalls_delete).setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean enable = getAdapterCount() > 0;
        menu.findItem(MENU_ITEM_DELETE_ALL).setEnabled(enable);
        menu.findItem(MENU_ITEM_DELETE_ALL).setVisible(enable);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE_ALL:
            final Intent intent = new Intent(this, CallLogMultipleDeleteActivity.class);
            intent.putExtra(SearchManager.USER_QUERY, mData);
            intent.putExtra(DialerConstants.BUNDLE_GLOBAL_SEARCH, true);
            this.startActivity(intent);
            return true;
        case android.R.id.home:
            finish();
            return true;
        default:
            break;

        }
        return super.onOptionsItemSelected(item);
    }

    private String getQuantityText(int count, int zeroResourceId, int pluralResourceId) {
        if (count == 0) {
            return getResources().getString(zeroResourceId);
        } else {
            String format = getResources().getQuantityText(pluralResourceId, count).toString();
            return String.format(format, count);
        }
    }

    public void updateSearchResult(int count) {
        Log.d(TAG, "[Dialer Global Search] updateSearchResult: " + count);
        String text = getQuantityText(count, R.string.listFoundAllCalllogZero,
                R.plurals.searchFoundCalllogs);
        mSearchResultFound.setText(text);
    }

    private int getAdapterCount() {
        if (mCallLogFragment != null) {
            return mCallLogFragment.getItemCount();
        } else {
            return 0;
        }
    }

    private void viewCallDetail(Intent intent) {
        getCallDetails(intent);
        Log.d(TAG, "View CallLog, start CallDetailActivity to view ");
    }

    private DialerContact buildContact(PhoneCallDetails detail) {
        DialerContact.Builder  contact = DialerContact.newBuilder();
        contact.setPhotoId(detail.photoId);
        if (detail.photoUri != null) {
          contact.setPhotoUri(detail.photoUri.toString());
        }
        if (detail.contactUri != null) {
          contact.setContactUri(detail.contactUri.toString());
        }
        CharSequence nameOrNumber = detail.getPreferredName();
        /* second line of contact view. */
        if (nameOrNumber != null) {
          contact.setDisplayNumber(detail.displayNumber);
        }
        if (TextUtils.isEmpty(nameOrNumber)) {
          nameOrNumber = detail.displayNumber;
        }
        if (nameOrNumber != null) {
          contact.setNameOrNumber(nameOrNumber.toString());
        }
        contact.setContactType(detail.contactType);
        contact.setNumber(detail.number.toString());
        /* phone number type (e.g. mobile) in second line of contact view */
        contact.setNumberLabel((String)
            Phone.getTypeLabel(getResources(), detail.numberType, detail.numberLabel));
        return contact.build();
    }

    private static CallDetailsEntries createCallDetailsEntries(PhoneCallDetails detail) {
        Assert.isMainThread();
        CallDetailsEntries.Builder entries = CallDetailsEntries.newBuilder();
        CallDetailsEntry.Builder entry = CallDetailsEntry.newBuilder();
        entry.setCallId(detail.callId)
            .setCallType(detail.callTypes.length > 0 ? detail.callTypes[0] : Calls.MISSED_TYPE)
            .setDataUsage(detail.dataUsage == null ? 0L : detail.dataUsage)
            .setDate(detail.date)
            .setDuration(detail.duration)
            .setFeatures(detail.features);
        entries.addEntries(entry.build());
        return entries.build();
      }

    public void getCallDetails(Intent intent) {
        CallLogAsyncTaskUtil.getCallDetails(this,
                new Uri[]{ intent.getData() }, mCallLogAsyncTaskListener);
    }

    private CallLogAsyncTaskListener mCallLogAsyncTaskListener = new CallLogAsyncTaskListener() {
        @Override
        public void onDeleteVoicemail() {
            Log.d(TAG, "Voicemail log delete");
            finish();
        }

        @Override
        public void onGetCallDetails(PhoneCallDetails[] details) {
            if (details == null || mContext == null) {
                // Somewhere went wrong: we're going to bail out.
                Log.d(TAG, "Somewhere went wrong: no call log");
                finish();
                return;
            }
            // All calls are from the same number and same contact, so pick the first detail.
            CallDetailsEntries callDetailsEntries = createCallDetailsEntries(details[0]);
            DialerContact contact = buildContact(details[0]);
            Intent intent = CallDetailsActivity.newInstance(mContext, callDetailsEntries,
                contact, false);
            mContext.startActivity(intent);
            finish();
        }

        //*/ freeme.liqiang, 20180204, calllog details
        @Override
        public void onDeleteCall() {

        }
        //*/
    };
}
