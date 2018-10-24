package com.freeme.dialer.app.list;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Trace;
import android.preference.PreferenceManager;
import android.provider.VoicemailContract;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.android.contacts.common.list.ViewPagerTabs;
import com.android.dialer.app.R;
import com.android.dialer.app.calllog.CallLogNotificationsService;
import com.android.dialer.app.list.DialerViewPager;
import com.android.dialer.app.list.DialtactsPagerAdapter;
import com.android.dialer.app.list.OldSpeedDialFragment;
import com.android.dialer.app.list.RemoveView;
import com.android.dialer.app.voicemail.error.VoicemailStatusCorruptionHandler;
import com.android.dialer.common.LogUtil;
import com.android.dialer.database.CallLogQueryHandler;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.ScreenEvent;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.speeddial.SpeedDialFragment;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.voicemailstatus.VisualVoicemailEnabledChecker;
import com.android.dialer.voicemailstatus.VoicemailStatusHelper;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.dialer.app.FreemeDialtactsActivity;
import com.freeme.dialer.app.FreemeTabBarController;
import com.freeme.dialer.app.calllog.FreemeCallLogFragment;
import com.freeme.dialer.contactsfragment.FreemeContactsFragment;
import com.freeme.dialer.utils.FreemeEntranceRequst;
import com.mediatek.dialer.ext.ExtensionManager;

import java.util.ArrayList;

import static com.android.dialer.app.list.DialtactsPagerAdapter.TAB_COUNT_WITH_VOICEMAIL;
import static com.android.dialer.app.list.DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS;
import static com.android.dialer.app.list.DialtactsPagerAdapter.TAB_INDEX_HISTORY;
import static com.android.dialer.app.list.DialtactsPagerAdapter.TAB_INDEX_SPEED_DIAL;
import static com.android.dialer.app.list.DialtactsPagerAdapter.TAB_INDEX_VOICEMAIL;

public class FreemeListsFragment extends Fragment implements ViewPager.OnPageChangeListener,
        FreemeTabBarController.ActivityUi,
        CallLogQueryHandler.Listener {

    private static final String TAG = "FreemeListsFragment";

    private DialerViewPager mViewPager;
    private ViewPagerTabs mViewPagerTabs;
    private DialtactsPagerAdapter mAdapter;
    private RemoveView mRemoveView;
    private View mRemoveViewContent;
    private Fragment mCurrentPage;
    private SharedPreferences mPrefs;
    private boolean mHasFetchedVoicemailStatus;
    private boolean mShowVoicemailTabAfterVoicemailStatusIsFetched;
    private VoicemailStatusHelper mVoicemailStatusHelper;
    private final ArrayList<ViewPager.OnPageChangeListener> mOnPageChangeListeners = new ArrayList<>();
    /**
     * The position of the currently selected tab.
     */
    private int mTabIndex = TAB_INDEX_HISTORY;

    private boolean mPaused;
    private CallLogQueryHandler mCallLogQueryHandler;

    private UiAction.Type[] actionTypeList;

    /*
    private FreemeTabBarController mTabBarController;
    */

    private int mActionBarHeight;

    private final ContentObserver mVoicemailStatusObserver =
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    mCallLogQueryHandler.fetchVoicemailStatus();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.d("FreemeListsFragment.onCreate", null);
        Trace.beginSection(TAG + " onCreate");
        super.onCreate(savedInstanceState);
        mVoicemailStatusHelper = new VoicemailStatusHelper();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Trace.endSection();
    }

    @Override
    public void onResume() {
        LogUtil.d("FreemeListsFragment.onResume", null);
        Trace.beginSection(TAG + " onResume");
        super.onResume();

        mPaused = false;

        if (getUserVisibleHint()) {
            sendScreenViewForCurrentPosition();
        }

        // Fetch voicemail status to determine if we should show the voicemail tab.
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity(),
                getActivity().getContentResolver(), this);
        mCallLogQueryHandler.fetchVoicemailStatus();
        mCallLogQueryHandler.fetchMissedCallsUnreadCount();
        Trace.endSection();
        mCurrentPage = mAdapter.getItem(mViewPager.getCurrentItem());
        if (mCurrentPage instanceof FreemeCallLogFragment) {
            ((FreemeCallLogFragment) mCurrentPage).onVisible();
        }
    }

    @Override
    public void onPause() {
        LogUtil.d("FreemeListsFragment.onPause", null);
        if (mCurrentPage instanceof FreemeCallLogFragment) {
            ((FreemeCallLogFragment) mCurrentPage).onNotVisible();
        }
        super.onPause();

        mPaused = true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewPager.removeOnPageChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogUtil.d("FreemeListsFragment.onCreateView", null);
        Trace.beginSection(TAG + " onCreateView");
        Trace.beginSection(TAG + " inflate view");
        final View parentView = inflater.inflate(R.layout.freeme_lists_fragment,
                container, false);
        Trace.endSection();
        Trace.beginSection(TAG + " setup views");

        actionTypeList = new UiAction.Type[TAB_COUNT_WITH_VOICEMAIL];
        actionTypeList[TAB_INDEX_SPEED_DIAL] = UiAction.Type.CHANGE_TAB_TO_FAVORITE;
        actionTypeList[TAB_INDEX_HISTORY] = UiAction.Type.CHANGE_TAB_TO_CALL_LOG;
        actionTypeList[TAB_INDEX_ALL_CONTACTS] = UiAction.Type.CHANGE_TAB_TO_CONTACTS;
        actionTypeList[TAB_INDEX_VOICEMAIL] = UiAction.Type.CHANGE_TAB_TO_VOICEMAIL;

        String[] tabTitles = new String[TAB_COUNT_WITH_VOICEMAIL];
        tabTitles[TAB_INDEX_SPEED_DIAL] = getResources().getString(R.string.contactsFavoritesLabel);
        tabTitles[TAB_INDEX_HISTORY] = getResources().getString(R.string.freeme_tab_call_label);
        tabTitles[TAB_INDEX_ALL_CONTACTS] = getResources().getString(R.string.tab_all_contacts);
        tabTitles[TAB_INDEX_VOICEMAIL] = getResources().getString(R.string.tab_voicemail);

        int[] tabIcons = new int[TAB_COUNT_WITH_VOICEMAIL];
        tabIcons[TAB_INDEX_SPEED_DIAL] = R.drawable.quantum_ic_grade_white_24;
        tabIcons[TAB_INDEX_HISTORY] = R.drawable.quantum_ic_schedule_white_24;
        tabIcons[TAB_INDEX_ALL_CONTACTS] = R.drawable.quantum_ic_people_white_24;
        tabIcons[TAB_INDEX_VOICEMAIL] = R.drawable.quantum_ic_voicemail_white_24;

        mActionBarHeight = parentView.getContext().getResources().getDimensionPixelSize(
                R.dimen.freeme_tab_bar_height);
        mUnreadCounts = new int[tabIcons.length];
        mViewPager = parentView.findViewById(R.id.lists_pager);
        mAdapter = new DialtactsPagerAdapter(getContext(), getChildFragmentManager(), tabTitles,
                mPrefs.getBoolean(VisualVoicemailEnabledChecker.PREF_KEY_HAS_ACTIVE_VOICEMAIL_PROVIDER,
                        false));
        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(this);
        mViewPager.setOffscreenPageLimit(4);

        /*
        mTabBarController = new FreemeTabBarController(this, mFreemeTabs,
                parentView.findViewById(R.id.search_box_expanded));
        if (savedInstanceState != null) {
            mTabBarController.restoreInstanceState(savedInstanceState);
        }
        */

        Activity activity = getActivity();
        if (activity instanceof FreemeDialtactsActivity) {
            FreemeDialtactsActivity fdActivity = (FreemeDialtactsActivity) activity;
            FreemeEntranceRequst feq = fdActivity.getFreemeEntriceRequst();
            if (feq != null) {
                if (!feq.isRecreatedInstance()) {
                    int code = fdActivity.getFreemeEntriceRequst().getEntranceCode();
                    if (code == FreemeEntranceRequst.ENTRANCE_CONTACTS) {
                        showTab(TAB_INDEX_ALL_CONTACTS);
                    } else {
                        showTab(TAB_INDEX_HISTORY);
                    }
                }
            }
        }

        mViewPagerTabs = parentView.findViewById(R.id.lists_pager_header);
        /*
        mViewPagerTabs.configureTabIcons(tabIcons);
        */
        mViewPagerTabs.setViewPager(mViewPager);
        addOnPageChangeListener(mViewPagerTabs);

        if (PermissionsUtil.hasReadVoicemailPermissions(getContext())
                && PermissionsUtil.hasAddVoicemailPermissions(getContext())) {
            activity.getContentResolver().registerContentObserver(
                    VoicemailContract.Status.CONTENT_URI, true,
                    mVoicemailStatusObserver);
        } else {
            LogUtil.w("FreemeListsFragment.onCreateView", "no voicemail read permissions");
        }

        /// M: [For Customization of default TAB opened when start dialer] @{
        ExtensionManager.getDialPadExtension().customizeDefaultTAB(this);
        /// @}

        Trace.endSection();
        Trace.endSection();
        return parentView;
    }

    @Override
    public void onDestroy() {
        getActivity().getContentResolver().unregisterContentObserver(mVoicemailStatusObserver);
        super.onDestroy();
    }

    public void addOnPageChangeListener(ViewPager.OnPageChangeListener onPageChangeListener) {
        if (!mOnPageChangeListeners.contains(onPageChangeListener)) {
            mOnPageChangeListeners.add(onPageChangeListener);
        }
    }

    /**
     * Shows the tab with the specified index. If the voicemail tab index is specified, but the
     * voicemail status hasn't been fetched, it will show the speed dial tab and try to show the
     * voicemail tab after the voicemail status has been fetched.
     */
    public void showTab(int index) {
        if (index == TAB_INDEX_VOICEMAIL) {
            if (mAdapter.hasActiveVoicemailProvider()) {
                mViewPager.setCurrentItem(mAdapter.getRtlPosition(TAB_INDEX_VOICEMAIL));
            } else if (!mHasFetchedVoicemailStatus) {
                // Try to show the voicemail tab after the voicemail status returns.
                mShowVoicemailTabAfterVoicemailStatusIsFetched = true;
            }
        } else if (index < getTabCount()) {
            mViewPager.setCurrentItem(mAdapter.getRtlPosition(index));
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mTabIndex = mAdapter.getRtlPosition(position);

        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);

        final int count = mOnPageChangeListeners.size();
        for (int i = 0; i < count; i++) {
            mOnPageChangeListeners.get(i).onPageScrolled(position, positionOffset, positionOffsetPixels);
        }
    }

    @Override
    public void onPageSelected(int position) {
        PerformanceReport.recordClick(actionTypeList[position]);

        LogUtil.i("FreemeListsFragment.onPageSelected", "position: %d", position);
        mTabIndex = mAdapter.getRtlPosition(position);

        // Show the tab which has been selected instead.
        mShowVoicemailTabAfterVoicemailStatusIsFetched = false;

        if (mCurrentPage instanceof FreemeCallLogFragment) {
            ((FreemeCallLogFragment) mCurrentPage).onNotVisible();
        }
        mCurrentPage = mAdapter.getItem(position);
        if (mCurrentPage instanceof FreemeCallLogFragment) {
            ((FreemeCallLogFragment) mCurrentPage).onVisible();
        }

        final int count = mOnPageChangeListeners.size();
        for (int i = 0; i < count; i++) {
            mOnPageChangeListeners.get(i).onPageSelected(position);
        }
        sendScreenViewForCurrentPosition();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        final int count = mOnPageChangeListeners.size();
        for (int i = 0; i < count; i++) {
            mOnPageChangeListeners.get(i).onPageScrollStateChanged(state);
        }
    }

    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        mHasFetchedVoicemailStatus = true;

        if (getActivity() == null || mPaused) {
            return;
        }

        VoicemailStatusCorruptionHandler.maybeFixVoicemailStatus(
                getContext(), statusCursor, VoicemailStatusCorruptionHandler.Source.Activity);

        // Update hasActiveVoicemailProvider, which controls the number of tabs displayed.
        boolean hasActiveVoicemailProvider =
                mVoicemailStatusHelper.getNumberActivityVoicemailSources(statusCursor) > 0;
        if (hasActiveVoicemailProvider != mAdapter.hasActiveVoicemailProvider()) {
            mAdapter.setHasActiveVoicemailProvider(hasActiveVoicemailProvider);
            mAdapter.notifyDataSetChanged();

            if (hasActiveVoicemailProvider) {
                Logger.get(getContext()).logImpression(DialerImpression.Type.VVM_TAB_VISIBLE);
                mViewPagerTabs.updateTab(TAB_INDEX_VOICEMAIL);
            } else {
                mViewPagerTabs.removeTab(TAB_INDEX_VOICEMAIL);
                mAdapter.removeVoicemailFragment(getChildFragmentManager());
            }

            mPrefs.edit()
                    .putBoolean(VisualVoicemailEnabledChecker.PREF_KEY_HAS_ACTIVE_VOICEMAIL_PROVIDER,
                            hasActiveVoicemailProvider)
                    .apply();
        }

        if (hasActiveVoicemailProvider) {
            mCallLogQueryHandler.fetchVoicemailUnreadCount();
        }

        if (mAdapter.hasActiveVoicemailProvider() && mShowVoicemailTabAfterVoicemailStatusIsFetched) {
            mShowVoicemailTabAfterVoicemailStatusIsFetched = false;
            showTab(TAB_INDEX_VOICEMAIL);
        }
    }

    @Override
    public void onVoicemailUnreadCountFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing() || cursor == null) {
            return;
        }

        int count = 0;
        try {
            count = cursor.getCount();
        } finally {
            cursor.close();
        }

        mViewPagerTabs.setUnreadCount(count, TAB_INDEX_VOICEMAIL);
        mViewPagerTabs.updateTab(TAB_INDEX_VOICEMAIL);
    }

    @Override
    public void onMissedCallsUnreadCountFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing() || cursor == null) {
            return;
        }

        int count = 0;
        try {
            count = cursor.getCount();
        } finally {
            cursor.close();
        }

        mViewPagerTabs.setUnreadCount(count, TAB_INDEX_HISTORY);
        mViewPagerTabs.updateTab(TAB_INDEX_HISTORY);
    }

    @Override
    public boolean onCallsFetched(Cursor statusCursor) {
        // Return false; did not take ownership of cursor
        return false;
    }

    public int getCurrentTabIndex() {
        return mTabIndex;
    }

    /**
     * External method to update unread count because the unread count changes when the user expands a
     * voicemail in the call log or when the user expands an unread call in the call history tab.
     */
    public void updateTabUnreadCounts() {
        if (mCallLogQueryHandler != null) {
            mCallLogQueryHandler.fetchMissedCallsUnreadCount();
            if (mAdapter.hasActiveVoicemailProvider()) {
                mCallLogQueryHandler.fetchVoicemailUnreadCount();
            }
        }
    }

    /**
     * External method to mark all missed calls as read.
     */
    public void markMissedCallsAsReadAndRemoveNotifications() {
        if (mCallLogQueryHandler != null) {
            mCallLogQueryHandler.markMissedCallsAsRead();
            CallLogNotificationsService.cancelAllMissedCalls(getContext());
        }
    }

    public void showRemoveView(boolean show) {
        initRemoveView();
        mRemoveViewContent.setVisibility(show ? View.VISIBLE : View.GONE);
        mRemoveView.setAlpha(show ? 0 : 1);
        mRemoveView.animate().alpha(show ? 1 : 0).start();
    }

    public void showMultiSelectRemoveView(boolean show) {
        mViewPagerTabs.setVisibility(show ? View.GONE : View.VISIBLE);
        mViewPager.setEnableSwipingPages(!show);
    }

    public boolean hasFrequents() {
        Fragment page = mAdapter.getItem(mAdapter.getRtlPosition(TAB_INDEX_SPEED_DIAL));
        return page instanceof OldSpeedDialFragment
                ? ((OldSpeedDialFragment) page).hasFrequents()
                : ((SpeedDialFragment) page).hasFrequents();
    }

    public RemoveView getRemoveView() {
        return mRemoveView;
    }

    public int getTabCount() {
        return mAdapter.getCount();
    }

    public void sendScreenViewForCurrentPosition() {
        if (!isResumed()) {
            return;
        }

        ScreenEvent.Type screenType;
        switch (getCurrentTabIndex()) {
            case TAB_INDEX_SPEED_DIAL:
                screenType = ScreenEvent.Type.SPEED_DIAL;
                break;
            case TAB_INDEX_HISTORY:
                screenType = ScreenEvent.Type.CALL_LOG;
                break;
            case TAB_INDEX_ALL_CONTACTS:
                screenType = ScreenEvent.Type.ALL_CONTACTS;
                break;
            case TAB_INDEX_VOICEMAIL:
                screenType = ScreenEvent.Type.VOICEMAIL_LOG;
                break;
            default:
                return;
        }
        Logger.get(getActivity()).logScreenView(screenType, getActivity());
    }

    /// M: [Multi-Delete] For CallLog delete @{
    @Override
    public void onCallsDeleted() {
        // Do nothing
    }
    /// @}

    private int[] mUnreadCounts;

    private void setUnreadCount(int count, int position) {
        if (mUnreadCounts == null || position >= mUnreadCounts.length) {
            return;
        }
        mUnreadCounts[position] = count;
    }

    /*
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mTabBarController.saveInstanceState(outState);
    }
    */

    public void onDialpadUp() {
        /*
        mTabBarController.onDialpadUp();
        */
        mAdapter.onDialpadUp();
    }

    public void onDialpadDown() {
        /*
        mTabBarController.onDialpadDown();
        */
        mAdapter.onDialpadDown();
    }

    @Override
    public boolean isInSearchUi() {
        return ((FreemeDialtactsActivity) getActivity()).isInSearchUi();
    }

    @Override
    public boolean hasSearchQuery() {
        return ((FreemeDialtactsActivity) getActivity()).hasSearchQuery();
    }

    @Override
    public int getTabBarHeight() {
        return mActionBarHeight;
    }

    public boolean isTabBarShowing() {
        /*
        return mTabBarController.isTabBarShowing();
        */
        return true;
    }

    public void setSearchQuery(String query) {
        /*
        mTabBarController.setSearchQuery(query);
        */
    }

    public DialerViewPager getViewPager() {
        return mViewPager;
    }

    public boolean isNeedExitMultiMode() {
        mCurrentPage = mAdapter.getItem(mViewPager.getCurrentItem());
        if (mCurrentPage instanceof FreemeCallLogFragment) {
            return ((FreemeCallLogFragment) mCurrentPage).isNeedExitMultiMode();
        } else if (mCurrentPage instanceof FreemeContactsFragment) {
            return ((FreemeContactsFragment) mCurrentPage).isNeedExitMultiMode();
        } else {
            return false;
        }
    }

    public void onAllSelected() {
        if (mAdapter != null) {
            mAdapter.onAllSelected(getCurrentTabIndex());
        }
    }

    private void initRemoveView() {
        if (mRemoveView != null) {
            return;
        }
        ViewStub stub = getView().findViewById(R.id.remove_view_stub);
        if (stub != null) {
            View view = stub.inflate();
            if (view instanceof RemoveView) {
                mRemoveView = (RemoveView) view;
                mRemoveViewContent = mRemoveView.findViewById(R.id.remove_view_content);
            }
        }
    }

    private View mActionBarView;
    private TextView mTitleView;
    private TextView mSelectView;
    private TextView mHomeView;

    public void showOrHideToolbar(boolean show) {
        if (mActionBarView == null) {
            ViewStub stub = getView().findViewById(R.id.actionbar_stub);
            mActionBarView = stub.inflate();

            mTitleView = mActionBarView.findViewById(R.id.title);
            mSelectView = mActionBarView.findViewById(R.id.select);
            mHomeView = mActionBarView.findViewById(R.id.home);

            ActionBarListener listener = new ActionBarListener();
            mActionBarView.setOnTouchListener(listener);
            mHomeView.setOnClickListener(listener);
            mSelectView.setOnClickListener(listener);

            Drawable drawable = getResources().getDrawable(R.drawable.freeme_back_img_selector);
            drawable.setAutoMirrored(true);
            mHomeView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable,
                    null, null, null);
        }

        if (mActionBarView != null) {
            mActionBarView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void updateActionbarTitle(String title) {
        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    public void updateActionBarMenuText(int title) {
        if (mSelectView != null) {
            mSelectView.setText(title);
        }
    }

    class ActionBarListener implements View.OnClickListener, View.OnTouchListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.select:
                    onAllSelected();
                    break;
                case R.id.home:
                    isNeedExitMultiMode();
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    }

    public boolean isSpecifiedPage(int pageIndex) {
        boolean ret = mTabIndex == pageIndex;
        if (!ret && FreemeLogUtils.DEBUG) {
            FreemeLogUtils.i(TAG, "current tab is: " + getCurrentTabName());
        }
        return ret;
    }

    private String getCurrentTabName() {
        String tab = "UNKNOWN";
        switch (mTabIndex) {
            case TAB_INDEX_ALL_CONTACTS:
                tab = "TAB_INDEX_ALL_CONTACTS";
                break;
            case TAB_INDEX_HISTORY:
                tab = "TAB_INDEX_HISTORY";
                break;
            case TAB_INDEX_SPEED_DIAL:
                tab = "TAB_INDEX_SPEED_DIAL";
                break;
            case TAB_INDEX_VOICEMAIL:
                tab = "TAB_INDEX_VOICEMAIL";
                break;
            default:
                break;
        }
        return tab;
    }

    public boolean inSearchContactorMode() {
        return isSpecifiedPage(TAB_INDEX_ALL_CONTACTS)
                && mAdapter != null && mAdapter.inSearchContactorMode();
    }

    public void clearSearchContactorFocus() {
        if (mAdapter != null) {
            mAdapter.clearSearchContactorFocus();
        }
    }

    public void updateSearchUI(boolean isInSearch) {
        mViewPagerTabs.setVisibility(isInSearch ? View.GONE : View.VISIBLE);
    }

    public void setEnableSwipingPages(boolean enabled){
        mViewPager.setEnableSwipingPages(enabled);
    }

    public void updateCalllogInfo() {
        if (mAdapter != null) {
            mAdapter.updateCalllogInfo();
        }
    }
}