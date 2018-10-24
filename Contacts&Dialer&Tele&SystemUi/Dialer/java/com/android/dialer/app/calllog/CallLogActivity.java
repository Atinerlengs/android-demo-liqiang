/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.dialer.app.calllog;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.android.contacts.common.list.ViewPagerTabs;
import com.android.dialer.app.DialtactsActivity;
import com.android.dialer.app.R;
import com.android.dialer.calllogutils.CallTypeIconsView;
import com.android.dialer.app.calllog.ClearCallLogDialog.Listener;
import com.android.dialer.calldetails.CallDetailsActivity;
import com.android.dialer.database.CallLogQueryHandler;
import com.android.dialer.enrichedcall.EnrichedCallComponent;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.ScreenEvent;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.postcall.PostCall;
import com.android.dialer.util.TransactionSafeActivity;
import com.android.dialer.util.ViewUtil;
import com.mediatek.dialer.calllog.CallLogMultipleDeleteActivity;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.ext.ICallLogAction;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.provider.MtkCallLog;
//*/ freeme.zhaozehong, 20180126. for freemeOS, UI redesign
import com.freeme.dialer.app.FreemeDialtactsActivity;
//*/

/** Activity for viewing call history. */
public class CallLogActivity extends TransactionSafeActivity
    implements ViewPager.OnPageChangeListener, Listener
    , /*M:*/ICallLogAction {

  private static final int TAB_INDEX_ALL = 0;
  /// M: [CallLog Incoming and Outgoing Filter] @{
  private static final boolean TAB_INCOMING_OUTGOING_ENABLE = DialerFeatureOptions
          .isCallLogIOFilterEnabled();
  private static final int TAB_INDEX_INCOMING = 1;
  private static final int TAB_INDEX_OUTGOING = 2;
  private static final int TAB_INDEX_MISSED = TAB_INCOMING_OUTGOING_ENABLE ? 3 : 1;
    private static final int TAB_INDEX_COUNT = TAB_INCOMING_OUTGOING_ENABLE ? 4 :
        /// M: for Plug-in @{
        ExtensionManager.getCallLogExtension().getTabIndexCount();
        ///@}

  private CallLogFragment mIncomingCallsFragment;
  private CallLogFragment mOutgoingCallsFragment;
  /// @}

  ///M: For plug-in to add two new fragments
  public CallLogFragment mVoiceCallsFragment;
  public CallLogFragment mVideoCallsFragment;
  /// @}

  private ViewPager mViewPager;
  private ViewPagerTabs mViewPagerTabs;
  private ViewPagerAdapter mViewPagerAdapter;
  private CallLogFragment mAllCallsFragment;
  ///M:keep mMissedCallsFragment
  private CallLogFragment mMissedCallsFragment;
  /// M: changed from String to CharSequence
  private CharSequence[] mTabTitles;
  private boolean mIsResumed;
  ///M: For Dialer Call pull feature @{
  public static final int INCOMING_PULLED_AWAY_TYPE = 10;
  public static final int OUTGOING_PULLED_AWAY_TYPE = 11;
  public static final int DECLINED_EXTERNAL_TYPE = 12;
  ///@}

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /// M: for Plug-in
    /// manage the hashmap @{
    ExtensionManager.getCallLogExtension().onCreate(this, savedInstanceState);
    /// @}

    /// M: [CallLog Incoming and Outgoing Filter] @{
    if (TAB_INCOMING_OUTGOING_ENABLE) {
        setContentView(R.layout.mtk_call_log_activity);
    } else {
        setContentView(R.layout.call_log_activity);
    }
    /// @}
    getWindow().setBackgroundDrawable(null);

    final ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayShowHomeEnabled(true);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowTitleEnabled(true);
    actionBar.setElevation(0);

    int startingTab = TAB_INDEX_ALL;
    final Intent intent = getIntent();
    if (intent != null) {
      final int callType = intent.getIntExtra(CallLog.Calls.EXTRA_CALL_TYPE_FILTER, -1);
      if (callType == CallLog.Calls.MISSED_TYPE) {
        startingTab = TAB_INDEX_MISSED;
      }
    }

    mTabTitles = new CharSequence[TAB_INDEX_COUNT];
    mTabTitles[0] = getString(R.string.call_log_all_title);
    mTabTitles[1] = getString(R.string.call_log_missed_title);

    /// M: [CallLog Incoming and Outgoing Filter] @{
    if (TAB_INCOMING_OUTGOING_ENABLE) {
        initTabIcons();
    }
    /// @}
    mViewPager = (ViewPager) findViewById(R.id.call_log_pager);

    mViewPagerAdapter = new ViewPagerAdapter(getFragmentManager());
    mViewPager.setAdapter(mViewPagerAdapter);
    /// M: [CallLog Incoming and Outgoing Filter]
    /// Also increase page limit when calllog incoming/outgoing enabled @{
    if (TAB_INCOMING_OUTGOING_ENABLE) {
        mViewPager.setOffscreenPageLimit(3);
    } else {
        mViewPager.setOffscreenPageLimit(1);
    }
    /// @}

    /// M: for Plug-in to init call logs tabs
    ExtensionManager.getCallLogExtension().initCallLogTab(mTabTitles, mViewPager);
    /// @}

    mViewPager.setOnPageChangeListener(this);

    mViewPagerTabs = (ViewPagerTabs) findViewById(R.id.viewpager_header);

    mViewPagerTabs.setViewPager(mViewPager);
    mViewPager.setCurrentItem(startingTab);

    /** M: Fix CR ALPS03452486. Save and restore the fragments. @{ */
    restoreFragments(savedInstanceState);
    /** @} */

  }

  @Override
  protected void onResume() {
    // Some calls may not be recorded (eg. from quick contact),
    // so we should restart recording after these calls. (Recorded call is stopped)
    PostCall.restartPerformanceRecordingIfARecentCallExist(this);
    if (!PerformanceReport.isRecording()) {
      PerformanceReport.startRecording();
    }

    mIsResumed = true;
    super.onResume();
    sendScreenViewForChildFragment();
    /// M: for Op01 Plug-in reset the reject mode flag @{
    ExtensionManager.getCallLogExtension().resetRejectMode(this);
    /// @}
  }

  @Override
  protected void onPause() {
    mIsResumed = false;
    super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.call_log_options, menu);
    /// M: for Plug-in @{
    ExtensionManager.getCallLogExtension()
            .createCallLogMenu(this, menu, mViewPagerTabs, this);
    /// @}
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    final MenuItem itemDeleteAll = menu.findItem(R.id.delete_all);
    if (mAllCallsFragment != null && itemDeleteAll != null) {
      // If onPrepareOptionsMenu is called before fragments are loaded, don't do anything.
      final CallLogAdapter adapter = mAllCallsFragment.getAdapter();
      /** M: Fix CR ALPS01884065. The isEmpty() be overrided with loading state of data.
       *  Here, it should not care about the loading state. So, use getCount() to check
       *  is the adapter really empty. @{ */
      itemDeleteAll.setVisible(adapter != null && adapter.getItemCount() > 0);
    }
    /// M: [Multi-Delete] for CallLog multiple delete @{
    final MenuItem itemDelete = menu.findItem(R.id.delete);
    CallLogFragment fragment = getCurrentCallLogFragment();

    /// M: for plug-in set the current call log fragment@{
    ExtensionManager.getCallLogExtension().setCurrentCallLogFragment(this,
                            getRtlPosition(mViewPager.getCurrentItem()), fragment);
    ///@}
    if (fragment != null && itemDelete != null) {
      final CallLogAdapter adapter = fragment.getAdapter();
      itemDelete.setVisible(DialerFeatureOptions.MULTI_DELETE && adapter != null
          && adapter.getItemCount() > 0);
    }
    ///@}

    /// M: for plug-in create the CallLogFilter menu@{
    ExtensionManager.getCallLogExtension().prepareCallLogMenu(this,
            menu, fragment, itemDeleteAll,
            fragment != null ? fragment.getAdapter().getItemCount() : 0);
    ///@}
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (!isSafeToCommitTransactions()) {
      return true;
    }

    if (item.getItemId() == android.R.id.home) {
      /// M: for Plug-in @{
      if (ExtensionManager.getCallLogExtension().onHomeButtonClick(this,
        mViewPagerAdapter, item)) {
        return true;
      }
      /// @}
      PerformanceReport.recordClick(UiAction.Type.CLOSE_CALL_HISTORY_WITH_CANCEL_BUTTON);
      /*/ freeme.zhaozehong, 20180125. for freemeOS, UI redesign
      final Intent intent = new Intent(this, DialtactsActivity.class);
      /*/
      final Intent intent = new Intent(this, FreemeDialtactsActivity.class);
      //*/
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      return true;
    } else if (item.getItemId() == R.id.delete_all) {
      ClearCallLogDialog.show(getFragmentManager(), this);
      return true;
    /// M: [Multi-Delete] for CallLog multiple delete @{
    } else if (item.getItemId() == R.id.delete) {
      if (DialerFeatureOptions.MULTI_DELETE) {
        final Intent delIntent = new Intent(this, CallLogMultipleDeleteActivity.class);
        delIntent.putExtra(CallLogQueryHandler.CALL_LOG_TYPE_FILTER, getCurrentCallLogFilteType());
        /// M: for Plug-in @{
        ExtensionManager.getCallLogExtension().onDeleteButtonClick(
                              getRtlPosition(mViewPager.getCurrentItem()), delIntent);
        /// @}
        startActivity(delIntent);
      }
      return true;
    }
    ///@}
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    mViewPagerTabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
  }

  @Override
  public void onPageSelected(int position) {
    /// M: for Plug-in @{
    ExtensionManager.getCallLogExtension().setPosition(position);
    /// @}

    if (mIsResumed) {
      sendScreenViewForChildFragment();
    }
    mViewPagerTabs.onPageSelected(position);
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    mViewPagerTabs.onPageScrollStateChanged(state);
  }

  private void sendScreenViewForChildFragment() {
    Logger.get(this).logScreenView(ScreenEvent.Type.CALL_LOG_FILTER, this);
  }

  private int getRtlPosition(int position) {
    if (ViewUtil.isRtl()) {
      return mViewPagerAdapter.getCount() - 1 - position;
    }
    return position;
  }

  @Override
  public void callHistoryDeleted() {
    if (EnrichedCallComponent.get(this).getEnrichedCallManager().hasStoredData()) {
      Snackbar.make(
              findViewById(R.id.calllog_frame), getString(R.string.multiple_ec_data_deleted), 5_000)
          .show();
    }
  }

  @Override
  public void onBackPressed() {
    /// M: for Plug-in @{
    ExtensionManager.getCallLogExtension().onBackPressed(this,
      mViewPagerAdapter, this);
    /// @}
    PerformanceReport.recordClick(UiAction.Type.PRESS_ANDROID_BACK_BUTTON);
    super.onBackPressed();
  }

  /** Adapter for the view pager. */
  public class ViewPagerAdapter extends FragmentPagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public long getItemId(int position) {
      return getRtlPosition(position);
    }

    @Override
    public Fragment getItem(int position) {
      /* Original code
      switch (getRtlPosition(position)) {
        case TAB_INDEX_ALL:
          return new CallLogFragment(
              CallLogQueryHandler.CALL_TYPE_ALL, true /* isCallLogActivity /);
        case TAB_INDEX_MISSED:
          return new CallLogFragment(Calls.MISSED_TYPE, true /* isCallLogActivity /);
        default:
          throw new IllegalStateException("No fragment at position " + position);
      */

      ///M: Plug-in to get the Item @{
      Fragment fragment = null;
      if ((fragment = ExtensionManager.getCallLogExtension()
                                      .getCallLogFragmentItem(position)) != null) {
          return fragment;
      }
      ///@}

      /** M: [CallLog Incoming and Outgoing Filter] @{ */
      position = getRtlPosition(position);
      if (position == TAB_INDEX_ALL) {
          return new CallLogFragment(CallLogQueryHandler.CALL_TYPE_ALL, true);
      } else if (position == TAB_INDEX_MISSED) {
          return new CallLogFragment(Calls.MISSED_TYPE, true);
      }else if (position == TAB_INDEX_INCOMING) {
          return new CallLogFragment(Calls.INCOMING_TYPE, true);
      } else if (position == TAB_INDEX_OUTGOING) {
          return new CallLogFragment(Calls.OUTGOING_TYPE, true);
      }
      throw new IllegalStateException("No fragment at position " + position);
      /** @} */
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      /// M: for Plug-in @{
      position = ExtensionManager.getCallLogExtension().getPosition(position);
      /// @}

      final CallLogFragment fragment = (CallLogFragment) super.instantiateItem(container, position);
      /* Original code
      if (position == TAB_INDEX_ALL) {
          mAllCallsFragment = fragment;
      }
      */
      /** M: [CallLog Incoming and Outgoing Filter] @{ */
      if (position == TAB_INDEX_ALL) {
          mAllCallsFragment = fragment;
      } else if (position == TAB_INDEX_MISSED) {
          mMissedCallsFragment = fragment;
      } else if (position == TAB_INDEX_INCOMING) {
          mIncomingCallsFragment = fragment;
      } else if (position == TAB_INDEX_OUTGOING) {
          mOutgoingCallsFragment = fragment;
      }
      ///M: Plug-in call to Instantiate Item @{
      ExtensionManager.getCallLogExtension().instantiateCallLogFragmentItem(
                            CallLogActivity.this , position, fragment);
      ///@}
      /** @} */
      return fragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return mTabTitles[position];
    }

    @Override
    public int getCount() {
      /// M: for Plug-in @{
      int count = ExtensionManager.getCallLogExtension()
            .getTabCount(CallLogActivity.this, TAB_INDEX_COUNT);
      /// @}
      return count;
    }

    /// M: ALPS03479182. for Plug-in.
    //  FragmentManagerImpl may get exception in some cases, need to override finishUpdate
    //  fuction to catch the exception. @{
    @Override
    public void finishUpdate(ViewGroup container) {
      try {
        super.finishUpdate(container);
      } catch (NullPointerException nullpointerException) {
        Log.d(TAG, "Catch the NullPointerException in FragmentPagerAdapter");
      }
    }
    /// @}
  }

  /// M: Mediatek start.
  /**
   * M: [Multi-Delete] Get the current displaying call log fragment. @{
   */
  public CallLogFragment getCurrentCallLogFragment() {
    int position = mViewPager.getCurrentItem();
    position = getRtlPosition(position);
    /** M: For OP01 plugin @{ */
    position = ExtensionManager.getCallLogExtension().getPosition(position);
    /** @} */
    if (position == TAB_INDEX_ALL) {
      return mAllCallsFragment;
    } else if (position == TAB_INDEX_MISSED) {
      return mMissedCallsFragment;
    } else if (position == TAB_INDEX_INCOMING) {
      return mIncomingCallsFragment;
    } else if (position == TAB_INDEX_OUTGOING) {
      return mOutgoingCallsFragment;
    }
    return null;
  }

  public int getCurrentCallLogFilteType() {
    int position = mViewPager.getCurrentItem();
    position = getRtlPosition(position);
    /** M: For OP01 plugin @{ */
    if (ExtensionManager.getCallLogExtension().isAutoRejectMode()) {
        return MtkCallLog.Calls.AUTO_REJECT_TYPE;
    }
    /** @} */
    if (position == TAB_INDEX_ALL) {
      return CallLogQueryHandler.CALL_TYPE_ALL;
    } else if (position == TAB_INDEX_MISSED) {
      return Calls.MISSED_TYPE;
    } else if (position == TAB_INDEX_INCOMING) {
      return Calls.INCOMING_TYPE;
    } else if (position == TAB_INDEX_OUTGOING) {
      return Calls.OUTGOING_TYPE;
    }
    return CallLogQueryHandler.CALL_TYPE_ALL;
  }
  /** @} */
  /// M: Mediatek end.

  /** M: [CallLog Incoming and Outgoing Filter] @{ */
  private static final String TAG = "CallLogActivity";
  private static final String FRAGMENT_TAG_ALL = "fragment_tag_all";
  private static final String FRAGMENT_TAG_MISSED = "fragment_tag_missed";
  private String mAllCallsFragmentTag = null;
  private String mMissedCallsFragmentTag = null;
  private static final String FRAGMENT_TAG_INCOMING = "fragment_tag_incoming";
  private static final String FRAGMENT_TAG_OUTGOING = "fragment_tag_outgoing";
  private String mIncomingCallsFragmentTag = null;
  private String mOutgoingCallsFragmentTag = null;

  private void restoreFragments(Bundle savedInstanceState) {
      Log.d(TAG, "restoreFragments savedInstanceState= " + savedInstanceState);
      if (savedInstanceState != null) {
          mAllCallsFragmentTag = savedInstanceState.getString(FRAGMENT_TAG_ALL, null);
          mMissedCallsFragmentTag = savedInstanceState.getString(FRAGMENT_TAG_MISSED, null);
      }
      if (mAllCallsFragment == null && mAllCallsFragmentTag != null) {
          mAllCallsFragment = (CallLogFragment) getFragmentManager()
                  .findFragmentByTag(mAllCallsFragmentTag);
          Log.d(TAG, "onResume findFragment all ~ " + mAllCallsFragment);
      }
      if (mMissedCallsFragment == null && mMissedCallsFragmentTag != null) {
          mMissedCallsFragment = (CallLogFragment) getFragmentManager()
                  .findFragmentByTag(mMissedCallsFragmentTag);
          Log.d(TAG, "onResume findFragment missed ~ " + mMissedCallsFragment);
      }
      if (TAB_INCOMING_OUTGOING_ENABLE) {
          if (savedInstanceState != null) {
              mIncomingCallsFragmentTag = savedInstanceState
                      .getString(FRAGMENT_TAG_INCOMING, null);
              mOutgoingCallsFragmentTag = savedInstanceState
                      .getString(FRAGMENT_TAG_OUTGOING, null);
          }
          if (mIncomingCallsFragment == null && mIncomingCallsFragmentTag != null) {
              mIncomingCallsFragment = (CallLogFragment) getFragmentManager()
                      .findFragmentByTag(mIncomingCallsFragmentTag);
              Log.d(TAG, "onResume findFragment incoming ~ " + mIncomingCallsFragment);
          }
          if (mOutgoingCallsFragment == null && mOutgoingCallsFragmentTag != null) {
              mOutgoingCallsFragment = (CallLogFragment) getFragmentManager()
                      .findFragmentByTag(mOutgoingCallsFragmentTag);
              Log.d(TAG, "onResume findFragment outgoing ~ " + mOutgoingCallsFragment);
          }
      }
      /// M: for plug-in @{
      ExtensionManager.getCallLogExtension().restoreFragments(this,
              mViewPagerAdapter, mViewPagerTabs);
      /// @}
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      if (mAllCallsFragment != null) {
          outState.putString(FRAGMENT_TAG_ALL, mAllCallsFragment.getTag());
      }
      if (mMissedCallsFragment != null) {
          outState.putString(FRAGMENT_TAG_MISSED, mMissedCallsFragment.getTag());
      }

      if (TAB_INCOMING_OUTGOING_ENABLE) {
          if (mIncomingCallsFragment != null) {
              outState.putString(FRAGMENT_TAG_INCOMING, mIncomingCallsFragment.getTag());
          }
          if (mOutgoingCallsFragment != null) {
              outState.putString(FRAGMENT_TAG_OUTGOING, mOutgoingCallsFragment.getTag());
          }
      }
      /// M: for plug-in @{
      ExtensionManager.getCallLogExtension().onSaveInstanceState(this, outState);
      /// @}

  }

  private void initTabIcons() {
      CallTypeIconsView.Resources resources = new CallTypeIconsView.Resources(this, false);
      mTabTitles[1] = createSpannableString(resources.incoming);
      mTabTitles[2] = createSpannableString(resources.outgoing);
      mTabTitles[3] = createSpannableString(resources.missed);
  }

  private SpannableString createSpannableString(Drawable drawable) {
      //Enlarge the icon by 1.5 times
      drawable.setBounds(0, 0, (drawable.getIntrinsicWidth() * 3) / 2,
              (drawable.getIntrinsicHeight() * 3) / 2);
      SpannableString sp = new SpannableString("i");
      ImageSpan iconsp = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
      sp.setSpan(iconsp, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
      return sp;
  }
  /** @} */

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == DialtactsActivity.ACTIVITY_REQUEST_CODE_CALL_DETAILS) {
      if (resultCode == RESULT_OK
          && data != null
          && data.getBooleanExtra(CallDetailsActivity.EXTRA_HAS_ENRICHED_CALL_DATA, false)) {
        String number = data.getStringExtra(CallDetailsActivity.EXTRA_PHONE_NUMBER);
        Snackbar.make(findViewById(R.id.calllog_frame), getString(R.string.ec_data_deleted), 5_000)
            .setAction(
                R.string.view_conversation,
                v -> startActivity(IntentProvider.getSendSmsIntentProvider(number).getIntent(this)))
            .setActionTextColor(getResources().getColor(R.color.dialer_snackbar_action_text_color))
            .show();
      }
    }

    /// M: for Plug-in CallLogFilter handle the result @{
    ExtensionManager.getCallLogExtension().onActivityResult(
            requestCode, resultCode, data);
    /// @}

    super.onActivityResult(requestCode, resultCode, data);
  }

  /// M: for Plug-in @{
  @Override
  public void processBackPressed() {
      super.onBackPressed();
  }

  @Override
  public void updateCallLogScreen() {
      mViewPagerAdapter.notifyDataSetChanged();
      mViewPagerTabs.setViewPager(mViewPager);
      mViewPager.setAdapter(mViewPagerAdapter);

      if (mAllCallsFragment != null) {
          mAllCallsFragment.forceToRefreshData();
          if (!mAllCallsFragment.isVisible() && !mAllCallsFragment.isAdded()) {
              final FragmentTransaction ftAll = getFragmentManager().beginTransaction();
              ftAll.remove(mAllCallsFragment);
              ftAll.commit();
          }
      }

      if (mMissedCallsFragment != null) {
          mMissedCallsFragment.forceToRefreshData();
          if (!mMissedCallsFragment.isVisible() && !mMissedCallsFragment.isAdded()) {
              final FragmentTransaction ftMiss = getFragmentManager().beginTransaction();
              ftMiss.remove(mMissedCallsFragment);
              ftMiss.commit();
          }
      }

      /// M: Add account select for plugin. @{
      if (mIncomingCallsFragment != null) {
          mIncomingCallsFragment.forceToRefreshData();
      }

      if (mOutgoingCallsFragment != null) {
          mOutgoingCallsFragment.forceToRefreshData();
      }
      /// @}
  }

  /// M: Add account select for plugin. @{
  @Override
  public boolean isActivityResumed() {
      return mIsResumed;
  }
  /// @}

  @Override
  protected void onDestroy() {
      /// M: for Plug-in
      /// manage the hashmap
      ExtensionManager.getCallLogExtension().onDestroy(this);
      /// @}
      super.onDestroy();
  }
  /// @}
}
