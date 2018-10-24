package com.freeme.dialer.app.calllog;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.dialer.animation.AnimUtils;
import com.android.dialer.app.Bindings;
import com.android.dialer.app.R;
import com.android.dialer.app.calllog.CallLogAdapter;
import com.android.dialer.app.calllog.CallLogFragment.HostInterface;
import com.android.dialer.app.calllog.CallLogModalAlertManager;
import com.android.dialer.app.calllog.calllogcache.CallLogCache;
import com.android.dialer.app.contactinfo.ContactInfoCache;
import com.android.dialer.app.contactinfo.ExpirableCacheHeadlessFragment;
import com.android.dialer.app.list.DialtactsPagerAdapter;
import com.android.dialer.app.list.DragDropController;
import com.android.dialer.app.voicemail.VoicemailPlaybackPresenter;
import com.android.dialer.blocking.FilteredNumberAsyncQueryHandler;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.database.CallLogQueryHandler;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.logging.UiAction;
import com.android.dialer.oem.CequintCallerIdManager;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.phonenumbercache.ContactInfoHelper;
import com.android.dialer.postcall.PostCall;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.util.ViewUtil;
import com.freeme.contacts.common.utils.FreemeBottomSelectedController;
import com.freeme.contacts.common.widgets.FreemeBottomSelectedView;
import com.freeme.contacts.common.widgets.FreemeEmptyContentView;
import com.freeme.dialer.app.FreemeDialtactsActivity;
import com.freeme.dialer.app.dialpad.FreemeDialpadUtils;
import com.freeme.dialer.app.dialpad.IFreemeCallLogOperatorInterface;
import com.freeme.dialer.app.list.FreemeListsFragment;
import com.freeme.dialer.callback.IFreemeMultiSelectCallBack;
import com.freeme.dialer.calllog.FreemeCallLogDeletionInteraction;
import com.mediatek.dialer.calllog.CallLogSearchResultActivity;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.util.DialerConstants;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.VvmUtils;

import java.util.Arrays;

public class FreemeCallLogFragment extends Fragment
        implements CallLogQueryHandler.Listener,
        CallLogAdapter.CallFetcher,
        CallLogAdapter.MultiSelectRemoveView,
        FreemeEmptyContentView.OnEmptyViewActionButtonClickedListener,
        FragmentCompat.OnRequestPermissionsResultCallback,
        CallLogModalAlertManager.Listener,
        View.OnClickListener {
    private static final String KEY_FILTER_TYPE = "filter_type";
    private static final String KEY_LOG_LIMIT = "log_limit";
    private static final String KEY_DATE_LIMIT = "date_limit";
    private static final String KEY_IS_CALL_LOG_ACTIVITY = "is_call_log_activity";
    private static final String KEY_HAS_READ_CALL_LOG_PERMISSION = "has_read_call_log_permission";
    private static final String KEY_REFRESH_DATA_REQUIRED = "refresh_data_required";
    private static final String KEY_SELECT_ALL_MODE = "select_all_mode_checked";
    private static final String KEY_IS_MULTI_SELECT_MODE = "key_is_multi_select_mode";

    // No limit specified for the number of logs to show; use the CallLogQueryHandler's default.
    private static final int NO_LOG_LIMIT = -1;
    // No date-based filtering.
    private static final int NO_DATE_LIMIT = 0;

    /**
     * M: request full group permissions instead of READ_CALL_LOG,
     * Because MTK changed the group permissions granting logic.
     */
    private static final String[] READ_CALL_LOG = PermissionsUtil.PHONE_FULL_GROUP;
    private static final int PHONE_PERMISSIONS_REQUEST_CODE = 1;

    private static final int EVENT_UPDATE_DISPLAY = 1;

    private static final long MILLIS_IN_MINUTE = 60 * 1000;
    private final Handler mHandler = new Handler();
    // See issue 6363009
    private final ContentObserver mCallLogObserver = new CustomContentObserver();
    private final ContentObserver mContactsObserver = new CustomContentObserver();
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private CallLogAdapter mAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;
    private boolean mScrollToTop;
    private FreemeEmptyContentView mEmptyListView;
    private ContactInfoCache mContactInfoCache;
    private final ContactInfoCache.OnContactInfoChangedListener mOnContactInfoChangedListener =
            new ContactInfoCache.OnContactInfoChangedListener() {
                @Override
                public void onContactInfoChanged() {
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                }
            };
    private boolean mRefreshDataRequired;
    private boolean mHasReadCallLogPermission;
    // Exactly same variable is in Fragment as a package private.
    private boolean mMenuVisible = true;
    // Default to all calls.
    private int mCallTypeFilter = CallLogQueryHandler.CALL_TYPE_ALL;
    // Log limit - if no limit is specified, then the default in {@link CallLogQueryHandler}
    // will be used.
    private int mLogLimit = NO_LOG_LIMIT;
    // Date limit (in millis since epoch) - when non-zero, only calls which occurred on or after
    // the date filter are included.  If zero, no date-based filtering occurs.
    private long mDateLimit = NO_DATE_LIMIT;
    /*
     * True if this instance of the FreemeCallLogFragment shown in the CallLogActivity.
     */
    private boolean mIsCallLogActivity = false;
    private boolean selectAllMode;
    private final Handler mDisplayUpdateHandler =
            new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case EVENT_UPDATE_DISPLAY:
                            refreshData();
                            rescheduleDisplayUpdate();
                            break;
                        default:
                            throw Assert.createAssertionFailException("Invalid message: " + msg);
                    }
                }
            };
    protected CallLogModalAlertManager mModalAlertManager;
    private ViewGroup mModalAlertView;
    private boolean isEmpty;

    private boolean mIsMultiSelectMode;
    private boolean mIsLandscape;

    public FreemeCallLogFragment() {
        this(CallLogQueryHandler.CALL_TYPE_ALL, NO_LOG_LIMIT);
    }

    public FreemeCallLogFragment(int filterType) {
        this(filterType, NO_LOG_LIMIT);
    }

    public FreemeCallLogFragment(int filterType, boolean isCallLogActivity) {
        this(filterType, NO_LOG_LIMIT);
        mIsCallLogActivity = isCallLogActivity;
    }

    public FreemeCallLogFragment(int filterType, int logLimit) {
        this(filterType, logLimit, NO_DATE_LIMIT);
    }

    /**
     * Creates a call log fragment, filtering to include only calls of the desired type, occurring
     * after the specified date.
     *
     * @param filterType type of calls to include.
     * @param dateLimit  limits results to calls occurring on or after the specified date.
     */
    public FreemeCallLogFragment(int filterType, long dateLimit) {
        this(filterType, NO_LOG_LIMIT, dateLimit);
    }

    /**
     * Creates a call log fragment, filtering to include only calls of the desired type, occurring
     * after the specified date. Also provides a means to limit the number of results returned.
     *
     * @param filterType type of calls to include.
     * @param logLimit   limits the number of results to return.
     * @param dateLimit  limits results to calls occurring on or after the specified date.
     */
    public FreemeCallLogFragment(int filterType, int logLimit, long dateLimit) {
        mCallTypeFilter = filterType;
        mLogLimit = logLimit;
        mDateLimit = dateLimit;

        mFreemeDialpadUtils = new FreemeDialpadUtils(this);
    }

    @Override
    public void onCreate(Bundle state) {
        LogUtil.d("FreemeCallLogFragment.onCreate", toString());
        super.onCreate(state);
        mRefreshDataRequired = true;
        if (state != null) {
            mCallTypeFilter = state.getInt(KEY_FILTER_TYPE, mCallTypeFilter);
            mLogLimit = state.getInt(KEY_LOG_LIMIT, mLogLimit);
            mDateLimit = state.getLong(KEY_DATE_LIMIT, mDateLimit);
            mIsCallLogActivity = state.getBoolean(KEY_IS_CALL_LOG_ACTIVITY, mIsCallLogActivity);
            mHasReadCallLogPermission = state.getBoolean(KEY_HAS_READ_CALL_LOG_PERMISSION, false);
            mRefreshDataRequired = state.getBoolean(KEY_REFRESH_DATA_REQUIRED, mRefreshDataRequired);
            selectAllMode = state.getBoolean(KEY_SELECT_ALL_MODE, false);

            mIsMultiSelectMode = state.getBoolean(KEY_IS_MULTI_SELECT_MODE, false);
        }

        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();
        mCallLogQueryHandler = new CallLogQueryHandler(activity, resolver, this, mLogLimit);

        if (PermissionsUtil.hasCallLogReadPermissions(getContext())) {
            resolver.registerContentObserver(CallLog.CONTENT_URI, true, mCallLogObserver);
        } else {
            LogUtil.w("FreemeCallLogFragment.onCreate", "call log permission not available");
        }
        if (PermissionsUtil.hasContactsReadPermissions(getContext())) {
            resolver.registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI, true, mContactsObserver);
        } else {
            LogUtil.w("FreemeCallLogFragment.onCreate", "contacts permission not available.");
        }
        setHasOptionsMenu(true);
    }

    /**
     * Called by the CallLogQueryHandler when the list of calls has been fetched or updated.
     */
    @Override
    public boolean onCallsFetched(Cursor cursor) {
        if (getActivity() == null || getActivity().isFinishing()) {
            // Return false; we did not take ownership of the cursor
            return false;
        }
        mAdapter.invalidatePositions();
        mAdapter.setLoading(false);
        mAdapter.changeCursor(cursor);
        // This will update the state of the "Clear call log" menu item.
        getActivity().invalidateOptionsMenu();

        boolean isDialpadShown = mCalllogOpreator.isDialpadShown();

        if (cursor != null && cursor.getCount() > 0) {
            if (mCallTypeFilter == CallLogQueryHandler.CALL_TYPE_ALL) {
                isEmpty = false;
            }
            if (mEmptyListView.getVisibility() != View.VISIBLE) {
                moveEmptyViewViaAnimation(isDialpadShown);
            }
            mEmptyListView.clearAnimation();
            mEmptyListView.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mEmptyListView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
            updateEmptyMessage(mCallTypeFilter);
            moveEmptyViewViaAnimation(isDialpadShown);
        }
        if (mScrollToTop) {
            // The smooth-scroll animation happens over a fixed time period.
            // As a result, if it scrolls through a large portion of the list,
            // each frame will jump so far from the previous one that the user
            // will not experience the illusion of downward motion.  Instead,
            // if we're not already near the top of the list, we instantly jump
            // near the top, and animate from there.
            if (mLayoutManager.findFirstVisibleItemPosition() > 5) {
                // TODO: Jump to near the top, then begin smooth scroll.
                mRecyclerView.smoothScrollToPosition(0);
            }
            // Workaround for framework issue: the smooth-scroll doesn't
            // occur if setSelection() is called immediately before.
            mHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (getActivity() == null || getActivity().isFinishing()) {
                                return;
                            }
                            mRecyclerView.smoothScrollToPosition(0);
                        }
                    });

            mScrollToTop = false;
        }

        /** M:  [Dialer Global Search] notify search activity update search result. @{*/
        updateSearchResultIfNeed(cursor);
        /** @}*/

        mAdapter.setMultiSelected(mAdapter.isMultiSelected());
        return true;
    }

    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
    }

    @Override
    public void onVoicemailUnreadCountFetched(Cursor cursor) {
    }

    @Override
    public void onMissedCallsUnreadCountFetched(Cursor cursor) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mIsLandscape = getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE;
        View view = inflater.inflate(R.layout.freeme_call_log_fragment, container, false);
        setupView(view);
        if (mAdapter != null && savedState != null) {
            mAdapter.onRestoreInstanceState(savedState);
        }
        return view;
    }

    protected void setupView(View view) {
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        PerformanceReport.logOnScrollStateChange(mRecyclerView);
        mEmptyListView = view.findViewById(R.id.empty_list_view);
        mEmptyListView.setActionLabel(FreemeEmptyContentView.NO_LABEL);
        mEmptyListView.setImage(R.drawable.freeme_empty_icon_calllogs);
        mEmptyListView.setActionClickedListener(this);
        mModalAlertView = view.findViewById(R.id.modal_message_container);
        mModalAlertManager = new CallLogModalAlertManager(LayoutInflater.from(getContext()), mModalAlertView, this);

        mCallLogFilterView = view.findViewById(R.id.call_log_filter_layout);
        if (mCallTypeFilter == CallLog.Calls.VOICEMAIL_TYPE) {
            mCallLogFilterView.setVisibility(View.GONE);
        }
        mAllCallLogs = view.findViewById(R.id.call_logs_all);
        mAllCallLogs.setSelected(mCallTypeFilter == CallLogQueryHandler.CALL_TYPE_ALL);
        mAllCallLogs.setOnClickListener(this);
        mMissedCallLogs = view.findViewById(R.id.call_logs_missed);
        mMissedCallLogs.setOnClickListener(this);
        mMissedCallLogs.setSelected(mCallTypeFilter == CallLog.Calls.MISSED_TYPE);

        FreemeBottomSelectedView bottomContainer = view.findViewById(R.id.bottom_container);
        mBottomSelectedController = new FreemeBottomSelectedController(bottomContainer);

        mParentLayout = view.findViewById(R.id.freeme_call_log_fragment_layout);
        mParentLayout.setOnDragListener(new LayoutOnDragListener());

        FloatingActionButton floatingActionButton = view.findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(this);
        mFloatingActionButtonController = new FloatingActionButtonController(getActivity(),
                floatingActionButton);

        mFreemeDialpadUtils.init(mFloatingActionButtonController, mParentLayout);

        mFloatingActionButtonController.setVisible(false);
        mCalllogOpreator.showDialpadFragment(false);
    }

    protected void setupData() {
        int activityType =
                mIsCallLogActivity
                        ? CallLogAdapter.ACTIVITY_TYPE_CALL_LOG
                        : CallLogAdapter.ACTIVITY_TYPE_DIALTACTS;
        String currentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());

        mContactInfoCache = new ContactInfoCache(
                ExpirableCacheHeadlessFragment.attach((AppCompatActivity) getActivity())
                        .getRetainedCache(),
                new ContactInfoHelper(getActivity(), currentCountryIso),
                mOnContactInfoChangedListener);
        mAdapter = Bindings.getLegacy(getActivity()).newCallLogAdapter(
                getActivity(),
                mRecyclerView,
                this,
                this,
                activityType == CallLogAdapter.ACTIVITY_TYPE_DIALTACTS
                        ? (CallLogAdapter.OnActionModeStateChangedListener) getActivity()
                        : null,
                new CallLogCache(getActivity()),
                mContactInfoCache,
                getVoicemailPlaybackPresenter(),
                new FilteredNumberAsyncQueryHandler(getActivity()),
                activityType);
        mAdapter.setFreemeMultiSelectCallBack(new IFreemeMultiSelectCallBack() {
            @Override
            public void isInMulitMode(boolean isMultiMode) {
                Activity activity = getActivity();
                if (activity instanceof FreemeDialtactsActivity) {
                    ((FreemeDialtactsActivity) activity).setMultiSelectMode(isMultiMode,
                            DialtactsPagerAdapter.TAB_INDEX_HISTORY);
                }
                if (isMultiMode) {
                    mBottomSelectedController.showActions(mActionNames, mActionCodes, mCallBack);
                } else {
                    mBottomSelectedController.hideActions();
                }
                if (mCallTypeFilter != CallLog.Calls.VOICEMAIL_TYPE) {
                    mCallLogFilterView.setVisibility(isMultiMode ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onSelectedCount(int count) {
                Activity activity = getActivity();
                if (activity instanceof FreemeDialtactsActivity) {
                    ((FreemeDialtactsActivity) activity).updateActionBarTitle(count,
                            DialtactsPagerAdapter.TAB_INDEX_HISTORY);
                    ((FreemeDialtactsActivity) activity).setIsAllCalllogsSelected(
                            count > 0 && count == mAdapter.getItemCount());
                }
                mBottomSelectedController.updateActionEnabled(count > 0, ACTION_CODE_DELETE);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        fetchCalls();
    }

    @Nullable
    protected VoicemailPlaybackPresenter getVoicemailPlaybackPresenter() {
        return null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupData();
        mAdapter.onRestoreInstanceState(savedInstanceState);

        ((FreemeDialtactsActivity) getActivity())
                .setFreemeDialpadOperator(mCalllogOpreator);

        if (savedInstanceState != null) {
            if (mFreemeDialpadUtils != null) {
                mFreemeDialpadUtils.setSearchQuery(
                        savedInstanceState.getString(KEY_SEARCH_QUERY));
                mFreemeDialpadUtils.setInRegularSearch(
                        savedInstanceState.getBoolean(KEY_IN_REGULAR_SEARCH_UI));
                mFreemeDialpadUtils.setInDialpadSearch(
                        savedInstanceState.getBoolean(KEY_IN_DIALPAD_SEARCH_UI));
                mFreemeDialpadUtils.setShowDialpadOnResume(
                        savedInstanceState.getBoolean(KEY_IS_DIALPAD_SHOWN));
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateEmptyMessage(mCallTypeFilter);

        /// M: Add account select for plugin. @{
        ExtensionManager.getCallLogExtension().onViewCreated(this, view);
        /// @}
    }

    @Override
    public void onResume() {
        LogUtil.d("FreemeCallLogFragment.onResume", toString());
        super.onResume();
        final boolean hasReadCallLogPermission =
                PermissionsUtil.hasPermission(getActivity(), READ_CALL_LOG);
        if (!mHasReadCallLogPermission && hasReadCallLogPermission) {
            // We didn't have the permission before, and now we do. Force a refresh of the call log.
            // Note that this code path always happens on a fresh start, but mRefreshDataRequired
            // is already true in that case anyway.
            mRefreshDataRequired = true;
            updateEmptyMessage(mCallTypeFilter);
        }

        mAdapter.setCallTypeFilter(mCallTypeFilter);

        mHasReadCallLogPermission = hasReadCallLogPermission;

        /*
         * Always clear the filtered numbers cache since users could have blocked/unblocked numbers
         * from the settings page
         */
        mAdapter.clearFilteredNumbersCache();
        refreshData();
        mAdapter.onResume();

        rescheduleDisplayUpdate();
    }

    @Override
    public void onPause() {
        LogUtil.d("FreemeCallLogFragment.onPause", toString());
        cancelDisplayUpdate();
        mAdapter.onPause();
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        CequintCallerIdManager cequintCallerIdManager = null;
        if (CequintCallerIdManager.isCequintCallerIdEnabled(getContext())) {
            cequintCallerIdManager = CequintCallerIdManager.createInstanceForCallLog();
        }
        mContactInfoCache.setCequintCallerIdManager(cequintCallerIdManager);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter != null) {
            mAdapter.onStop();
        }
        mContactInfoCache.stop();
    }

    @Override
    public void onDestroy() {
        LogUtil.d("FreemeCallLogFragment.onDestroy", toString());
        if (mAdapter != null) {
            mAdapter.changeCursor(null);
        }

        getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
        getActivity().getContentResolver().unregisterContentObserver(mContactsObserver);

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_FILTER_TYPE, mCallTypeFilter);
        outState.putInt(KEY_LOG_LIMIT, mLogLimit);
        outState.putLong(KEY_DATE_LIMIT, mDateLimit);
        outState.putBoolean(KEY_IS_CALL_LOG_ACTIVITY, mIsCallLogActivity);
        outState.putBoolean(KEY_HAS_READ_CALL_LOG_PERMISSION, mHasReadCallLogPermission);
        outState.putBoolean(KEY_REFRESH_DATA_REQUIRED, mRefreshDataRequired);
        outState.putBoolean(KEY_SELECT_ALL_MODE, selectAllMode);
        if (mAdapter != null) {
            mAdapter.onSaveInstanceState(outState);
            outState.putBoolean(KEY_IS_MULTI_SELECT_MODE, mAdapter.isMultiSelected());
        }

        if (mFreemeDialpadUtils != null) {
            outState.putString(KEY_SEARCH_QUERY, mFreemeDialpadUtils.getSearchQuery());
            outState.putBoolean(KEY_IN_REGULAR_SEARCH_UI, mFreemeDialpadUtils.isInRegularSearch());
            outState.putBoolean(KEY_IN_DIALPAD_SEARCH_UI, mFreemeDialpadUtils.isInDialpadSearch());
            outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mFreemeDialpadUtils.isDialpadShown());
        }
    }

    @Override
    public void fetchCalls() {
        /** M: [Dialer Global Search] Displays a list of call log entries @{ */
        if (isQueryMode()) {
            startSearchCalls(mQueryData);
        } else {
            /** @} */
            mCallLogQueryHandler.fetchCalls(mCallTypeFilter, mDateLimit);
            if (!mIsCallLogActivity) {
                if (getParentFragment() instanceof FreemeListsFragment) {
                    ((FreemeListsFragment) getParentFragment()).updateTabUnreadCounts();
                }
            }
        }
    }

    private void updateEmptyMessage(int filterType) {
        final Context context = getActivity();
        if (context == null) {
            return;
        }

        if (!PermissionsUtil.hasPermission(context, READ_CALL_LOG)) {
            mEmptyListView.setDescription(R.string.permission_no_calllog);
            mEmptyListView.setActionLabel(R.string.permission_single_turn_on);
            return;
        }
        mEmptyListView.setActionLabel(FreemeEmptyContentView.NO_LABEL);

        ///M: For plugin to get the empty message Id @{
        filterType = ExtensionManager.getCallLogExtension().getFilterType(filterType);
        ///@}

        int icon = R.drawable.freeme_empty_icon_calllogs;
        final int messageId;
        switch (filterType) {
            case CallLog.Calls.MISSED_TYPE:
                messageId = R.string.freeme_call_log_missed_empty;
                icon = R.drawable.freeme_empty_icon_missed_calllogs;
                break;
            case CallLog.Calls.VOICEMAIL_TYPE:
                messageId = R.string.freeme_call_log_voicemail_empty;
                break;
            case CallLogQueryHandler.CALL_TYPE_ALL:
                /** M: [Dialer Global Search] Search mode with customer empty string. */
                messageId = isQueryMode() ? R.string.noMatchingCalllogs
                        : R.string.call_log_all_empty;
                /** @} */
                break;
            /** M: [CallLog Incoming and Outgoing Filter] @{ */
            case CallLog.Calls.INCOMING_TYPE:
                messageId = R.string.call_log_all_empty;
                break;
            case CallLog.Calls.OUTGOING_TYPE:
                messageId = R.string.call_log_all_empty;
                break;
            /** @} */
            default:
                throw new IllegalArgumentException(
                        "Unexpected filter type in FreemeCallLogFragment: " + filterType);
        }
        mEmptyListView.setDescription(messageId);
        mEmptyListView.setImage(icon);
    }

    public CallLogAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (menuVisible && isResumed()) {
                refreshData();
            }
        }
    }

    /**
     * Requests updates to the data to be shown.
     */
    private void refreshData() {
        // Prevent unnecessary refresh.
        if (mRefreshDataRequired) {
            // Mark all entries in the contact info cache as out of date, so they will be looked up
            // again once being shown.
            mContactInfoCache.invalidate();
            mAdapter.setLoading(true);

            fetchCalls();
            mCallLogQueryHandler.fetchVoicemailStatus();
            mCallLogQueryHandler.fetchMissedCallsUnreadCount();
            mRefreshDataRequired = false;
        } else {
            // Refresh the display of the existing data to update the timestamp text descriptions.
            mAdapter.notifyDataSetChanged();
        }

        /// M: Add account select for plugin. @{
        ExtensionManager.getCallLogExtension().updateNotice(this);
        /// @}
    }

    @Override
    public void onEmptyViewActionButtonClicked() {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        String[] deniedPermissions =
                PermissionsUtil.getPermissionsCurrentlyDenied(
                        getContext(), PermissionsUtil.allPhoneGroupPermissionsUsedInDialer);
        if (deniedPermissions.length > 0) {
            LogUtil.i(
                    "FreemeCallLogFragment.onEmptyViewActionButtonClicked",
                    "Requesting permissions: " + Arrays.toString(deniedPermissions));
            FragmentCompat.requestPermissions(this, deniedPermissions, PHONE_PERMISSIONS_REQUEST_CODE);
        } else if (!mIsCallLogActivity) {
            // Show dialpad if we are not in the call log activity.
            ((HostInterface) activity).showDialpad();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PHONE_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                // Force a refresh of the data since we were missing the permission before this.
                mRefreshDataRequired = true;
            }
        }
    }

    /**
     * Schedules an update to the relative call times (X mins ago).
     */
    private void rescheduleDisplayUpdate() {
        if (!mDisplayUpdateHandler.hasMessages(EVENT_UPDATE_DISPLAY)) {
            long time = System.currentTimeMillis();
            // This value allows us to change the display relatively close to when the time changes
            // from one minute to the next.
            long millisUtilNextMinute = MILLIS_IN_MINUTE - (time % MILLIS_IN_MINUTE);
            mDisplayUpdateHandler.sendEmptyMessageDelayed(EVENT_UPDATE_DISPLAY, millisUtilNextMinute);
        }
    }

    /**
     * Cancels any pending update requests to update the relative call times (X mins ago).
     */
    private void cancelDisplayUpdate() {
        mDisplayUpdateHandler.removeMessages(EVENT_UPDATE_DISPLAY);
    }

    @CallSuper
    public void onVisible() {
        LogUtil.enterBlock("FreemeCallLogFragment.onPageSelected");
    }

    @CallSuper
    public void onNotVisible() {
        LogUtil.enterBlock("FreemeCallLogFragment.onPageUnselected");
    }

    @Override
    public void onShowModalAlert(boolean show) {
        LogUtil.d(
                "FreemeCallLogFragment.onShowModalAlert",
                "show: %b, fragment: %s, isVisible: %b",
                show,
                this,
                getUserVisibleHint());
        getAdapter().notifyDataSetChanged();
        HostInterface hostInterface = (HostInterface) getActivity();
        if (show) {
            mRecyclerView.setVisibility(View.GONE);
            mModalAlertView.setVisibility(View.VISIBLE);
            if (hostInterface != null && getUserVisibleHint()) {
                hostInterface.enableFloatingButton(false);
            }
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mModalAlertView.setVisibility(View.GONE);
            if (hostInterface != null && getUserVisibleHint()) {
                hostInterface.enableFloatingButton(true);
            }
        }
    }

    @Override
    public void showMultiSelectRemoveView(boolean show) {
        if (getParentFragment() instanceof FreemeListsFragment) {
            ((FreemeListsFragment) getParentFragment()).showMultiSelectRemoveView(show);
        }
    }

    @Override
    public void setSelectAllModeToFalse() {
        selectAllMode = false;
    }

    @Override
    public void tapSelectAll() {
        LogUtil.i("FreemeCallLogFragment.tapSelectAll", "imitating select all");
        selectAllMode = true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.call_logs_all:
                updateCallLogsFilter(CallLogQueryHandler.CALL_TYPE_ALL);
                break;
            case R.id.call_logs_missed:
                updateCallLogsFilter(CallLog.Calls.MISSED_TYPE);
                break;
            case R.id.floating_action_button:
                if (!mCalllogOpreator.isDialpadShown()) {
                    PerformanceReport.recordClick(UiAction.Type.OPEN_DIALPAD);
                    mCalllogOpreator.setInCallDialpadUp(false);
                    mCalllogOpreator.showDialpadFragment(true);
                    PostCall.closePrompt();
                }
                break;
            default:
                break;
        }
    }

    protected class CustomContentObserver extends ContentObserver {

        public CustomContentObserver() {
            super(mHandler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mRefreshDataRequired = true;
        }
    }

    /// M: [Multi-Delete] For CallLog delete @{
    @Override
    public void onCallsDeleted() {
        // Do nothing
    }
    /// @}

    /**
     * M: [Dialer Global Search] Displays a list of call log entries.
     * CallLogSearch activity reused FreemeCallLogFragment.  @{
     */
    // Default null, while in search mode it is not null.
    private String mQueryData = null;

    /**
     * Use it to inject search data.
     * This is the entrance of call log search mode.
     *
     * @param query
     */
    public void setQueryData(String query) {
        mQueryData = query;
        mAdapter.setQueryString(query);
    }

    private void startSearchCalls(String query) {
        Uri uri = Uri.withAppendedPath(DialerConstants.CALLLOG_SEARCH_URI_BASE, query);
        /// support search Voicemail calllog
        uri = VvmUtils.buildVvmAllowedUri(uri);
        mCallLogQueryHandler.fetchSearchCalls(uri);
    }

    private boolean isQueryMode() {
        return !TextUtils.isEmpty(mQueryData) && DialerFeatureOptions.DIALER_GLOBAL_SEARCH;
    }

    private void updateSearchResultIfNeed(Cursor result) {
        if (isQueryMode() && getActivity() instanceof CallLogSearchResultActivity) {
            int count = result != null ? result.getCount() : 0;
            ((CallLogSearchResultActivity) getActivity()).updateSearchResult(count);
        }
    }

    public int getItemCount() {
        return mAdapter.getItemCount();
    }
    /** @} */

    /**
     * M : force refresh calllog data
     */
    public void forceToRefreshData() {
        mRefreshDataRequired = true;
        // / M: for ALPS01683374
        // refreshData only when FreemeCallLogFragment is in foreground
        if (isResumed()) {
            refreshData();
            // refreshData would cause ContactInfoCache.invalidate
            // and cache thread starting would be stopped seldom.
            // we have to call adapter onResume again to start cache thread.
            mAdapter.onResume();
        }
    }
    ///@}

    private View mCallLogFilterView;
    private TextView mAllCallLogs;
    private TextView mMissedCallLogs;

    private void updateCallLogsFilter(int filter) {
        if (filter != mCallTypeFilter) {
            mCallTypeFilter = filter;
            mAdapter.setCallTypeFilter(mCallTypeFilter);
            boolean isAll = filter == CallLogQueryHandler.CALL_TYPE_ALL;
            mAllCallLogs.setSelected(isAll);
            mMissedCallLogs.setSelected(!isAll);
            fetchCalls();
        }
    }

    public boolean isNeedExitMultiMode() {
        if (mAdapter != null && mAdapter.isMultiSelected()) {
            mAdapter.setMultiSelected(false);
            mAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    private FreemeBottomSelectedController mBottomSelectedController;
    private static final int ACTION_CODE_DELETE = 0x100;
    private static final int[] mActionNames = new int[]{R.string.recentCalls_delete};
    private static final int[] mActionCodes = new int[]{ACTION_CODE_DELETE};

    private FreemeBottomSelectedView.IFreemeBottomActionCallBack mCallBack = (int actionCode) -> {
        switch (actionCode) {
            case ACTION_CODE_DELETE:
                FreemeCallLogDeletionInteraction.start(this,
                        mAdapter.getSelectedCallIds(), () -> {
                            mAdapter.setMultiSelected(false);
                        });
                break;
            default:
                break;
        }
    };

    public void onAllSelected() {
        mAdapter.allSelecteOrNot();
    }

    private ViewPropertyAnimator mTranslateYAnimator;
    private int mTranslateY = Integer.MIN_VALUE;

    private void initEmptyViewAnim() {
        if (mTranslateY == Integer.MIN_VALUE || mTranslateYAnimator == null) {
            int viewH;
            ViewParent parent = mEmptyListView.getParent();
            if (parent instanceof ViewGroup) {
                ViewGroup view = (ViewGroup) parent;
                viewH = view.getMeasuredHeight();
            } else {
                viewH = getActivity().getResources().getDisplayMetrics().heightPixels;
            }
            int[] loc = new int[2];
            mEmptyListView.getLocationInWindow(loc);
            mTranslateY = (viewH - mEmptyListView.getMeasuredHeight()) / 2 - loc[1];

            mTranslateYAnimator = mEmptyListView.animate()
                    .setInterpolator(AnimUtils.EASE_OUT_EASE_IN)
                    .setDuration(300);
        }
    }

    private boolean mIsEmptyViewNoMoved = true; // dialpad is shown
    public void moveEmptyViewViaAnimation(boolean isDialpadShown) {
        moveEmptyViewViaAnimation(isDialpadShown, false);
    }

    public void moveEmptyViewViaAnimation(boolean isDialpadShown, boolean force) {
        if (mIsLandscape || (getActivity() != null && getActivity().isInMultiWindowMode())
                || mEmptyListView == null) {
            return;
        }
        if (mEmptyListView.getVisibility() != View.VISIBLE
                && mRecyclerView != null && mRecyclerView.getAdapter().getItemCount() <= 0) {
            mEmptyListView.setVisibility(View.VISIBLE);
        }
        if (mEmptyListView.getVisibility() != View.VISIBLE) {
            return;
        }
        if (force || (mIsEmptyViewNoMoved != isDialpadShown)) {
            mIsEmptyViewNoMoved = isDialpadShown;
            initEmptyViewAnim();
            mTranslateYAnimator.translationY(isDialpadShown ? 0 : mTranslateY).start();
        }
    }

    private static final String KEY_IN_REGULAR_SEARCH_UI = "in_regular_search_ui";
    private static final String KEY_IN_DIALPAD_SEARCH_UI = "in_dialpad_search_ui";
    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_IS_DIALPAD_SHOWN = "is_dialpad_shown";

    private FloatingActionButtonController mFloatingActionButtonController;
    private FreemeDialpadUtils mFreemeDialpadUtils;
    private DragDropController mDragDropController;

    private FrameLayout mParentLayout;

    /**
     * Listener that listens to drag events and sends their x and y coordinates to a {@link
     * DragDropController}.
     */
    private class LayoutOnDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION && mDragDropController != null) {
                mDragDropController.handleDragHovered(v, (int) event.getX(), (int) event.getY());
            }
            return true;
        }
    }

    private boolean isNotNull(Object object) {
        return object != null;
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        if (isNotNull(mFreemeDialpadUtils)){
            mFreemeDialpadUtils.onAttachFragment(childFragment);
        }
    }

    public void updateSearchUI(boolean isInSearch) {
        if (isInSearch) {
            mRecyclerView.setVisibility(View.GONE);
            if (mRecyclerView.getAdapter().getItemCount() <= 0) {
                mEmptyListView.setVisibility(View.INVISIBLE);
            }
        } else {
            if (mRecyclerView.getAdapter().getItemCount() > 0) {
                mRecyclerView.setVisibility(View.VISIBLE);
            } else if (mEmptyListView.getVisibility() != View.VISIBLE) {
                mEmptyListView.setVisibility(View.VISIBLE);
            }
        }
        mCallLogFilterView.setVisibility(isInSearch ? View.GONE : View.VISIBLE);
    }

    private IFreemeCallLogOperatorInterface mCalllogOpreator
            = new IFreemeCallLogOperatorInterface() {

        @Override
        public boolean isDialpadVisible() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.isDialpadVisible();
        }

        @Override
        public boolean onBackPress() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.onBackPress();
        }

        @Override
        public boolean isInSearchUi() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.isInSearchUi();
        }

        @Override
        public void exitSearchUi() {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.exitSearchUi();
            }
        }

        @Override
        public boolean hasSearchQuery() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.hasSearchQuery();
        }

        @Override
        public void onDialpadQueryChanged(String query, String normalizedQuery) {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.onDialpadQueryChanged(query, normalizedQuery);
            }
        }

        @Override
        public void showDialpadFragment(boolean animate) {
            if ((mAdapter == null && !mIsMultiSelectMode)
                    || (mAdapter != null && !mAdapter.isMultiSelected())) {
                if (isNotNull(mFreemeDialpadUtils)) {
                    mFreemeDialpadUtils.showDialpadFragment(animate);
                }
            }
        }

        @Override
        public void setStartedFromNewIntent(boolean value) {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.setStartedFromNewIntent(value);
            }
        }

        @Override
        public int getDialpadHeight() {
            int h = 0;
            if (isNotNull(mFreemeDialpadUtils)) {
                h = mFreemeDialpadUtils.getDialpadHeight();
            }
            return h;
        }

        @Override
        public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.hideDialpadFragment(animate, clearDialpad);
            }
        }

        @Override
        public boolean isDialpadShown() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.isDialpadShown();
        }

        @Override
        public void onDialpadShown() {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.onDialpadShown();
            }
        }

        @Override
        public void clearDialpad() {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.clearDialpad();
            }
        }

        @Override
        public void enableFloatingButton(boolean enabled) {
            LogUtil.i("FreemeDialtactsActivity.enableFloatingButton", "enable: %b", enabled);
            // Floating button shouldn't be enabled when dialpad is shown.
            if (!isDialpadShown() || !enabled) {
                LogUtil.i("FreemeDialtactsActivity.enableFloatingButton", "enter : FAB setVisible->" + enabled);
                mFloatingActionButtonController.setVisible(enabled);
            }
        }

        @Override
        public void setFloatingButtonVisible(boolean visible) {
            mFloatingActionButtonController.setVisible(visible);
        }

        @Override
        public void setInCallDialpadUp(boolean inCallDialpadUp) {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.setInCallDialpadUp(inCallDialpadUp);
            }
        }

        @Override
        public boolean isInCallDialpadUp() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.isInCallDialpadUp();
        }

        @Override
        public void setShowDialpadOnResume(boolean showDialpadOnResume) {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.setShowDialpadOnResume(showDialpadOnResume);
            }
        }

        @Override
        public boolean isShowDialpadOnResume() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.isShowDialpadOnResume();
        }

        @Override
        public void setClearSearchOnPause(boolean clearSearchOnPause) {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.setClearSearchOnPause(clearSearchOnPause);
            }
        }

        @Override
        public boolean isClearSearchOnPause() {
            return isNotNull(mFreemeDialpadUtils) && mFreemeDialpadUtils.isClearSearchOnPause();
        }

        @Override
        public void setDragDropController(DragDropController dragController) {
            mDragDropController = dragController;
        }

        @Override
        public void onResume() {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.onResume();
            }
        }

        @Override
        public void onPause() {
            if (isNotNull(mFreemeDialpadUtils)) {
                mFreemeDialpadUtils.onPause();
            }
        }

        @Override
        public String getSearchQuery() {
            String query = null;
            if (isNotNull(mFreemeDialpadUtils)) {
                query = mFreemeDialpadUtils.getSearchQuery();
            }
            return query;
        }
    };

    public void updateCalllogInfo() {
        refreshData();
        if (mAdapter != null) {
            mAdapter.onResume();
        }
    }
}
