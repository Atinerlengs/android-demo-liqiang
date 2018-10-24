package com.freeme.dialer.app;

import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Trace;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.telecom.PhoneAccount;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.AbsListView;

import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.dialer.app.DialtactsActivity;
import com.android.dialer.app.R;
import com.android.dialer.app.calllog.CallLogAdapter;
import com.android.dialer.app.calllog.CallLogFragment;
import com.android.dialer.app.calllog.CallLogNotificationsService;
import com.android.dialer.app.calllog.IntentProvider;
import com.android.dialer.app.dialpad.DialpadFragment;
import com.android.dialer.app.list.DialtactsPagerAdapter;
import com.android.dialer.app.list.DragDropController;
import com.android.dialer.app.list.OldSpeedDialFragment;
import com.android.dialer.app.list.OnDragDropListener;
import com.android.dialer.app.list.OnListFragmentScrolledListener;
import com.android.dialer.app.list.PhoneFavoriteSquareTileView;
import com.android.dialer.app.list.SearchFragment;
import com.android.dialer.callcomposer.CallComposerActivity;
import com.android.dialer.calldetails.CallDetailsActivity;
import com.android.dialer.callintent.CallInitiationType;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.callintent.CallSpecificAppData;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.configprovider.ConfigProviderBindings;
import com.android.dialer.database.Database;
import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.interactions.PhoneNumberInteraction;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.ScreenEvent;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.postcall.PostCall;
import com.android.dialer.proguard.UsedByReflection;
import com.android.dialer.smartdial.SmartDialNameMatcher;
import com.android.dialer.smartdial.SmartDialPrefix;
import com.android.dialer.telecom.TelecomUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PermissionsUtil;
import com.android.dialer.util.TouchPointManager;
import com.freeme.dialer.app.dialpad.FreemeDialpadFragment;
import com.freeme.dialer.app.dialpad.IFreemeCallLogOperatorInterface;
import com.freeme.dialer.app.list.FreemeListsFragment;
import com.freeme.dialer.utils.FreemeDialerUtils;
import com.freeme.dialer.utils.FreemeEntranceRequst;
import com.freeme.phone.common.accessibility.FreemeCallAccessibility;
import com.mediatek.dialer.activities.NeedTestActivity;
import com.mediatek.dialer.compat.CompatChecker;
import com.mediatek.dialer.database.DialerDatabaseHelperEx;
import com.mediatek.dialer.search.ThrottleContentObserver;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.DialerVolteUtils;

import java.util.Arrays;
import java.util.Locale;

/**
 * The dialer tab's title is 'phone', a more common name (see strings.xml).
 * M: change extend to NeedTestActivity for test case developing
 */
@UsedByReflection(value = "AndroidManifest-app.xml")
public class FreemeDialtactsActivity extends NeedTestActivity implements
        DialpadFragment.OnDialpadQueryChangedListener,
        OnListFragmentScrolledListener,
        CallLogFragment.HostInterface,
        CallLogAdapter.OnActionModeStateChangedListener,
        DialpadFragment.HostInterface,
        OldSpeedDialFragment.HostInterface,
        SearchFragment.HostInterface,
        OnDragDropListener,
        OnPhoneNumberPickerActionListener,
        ViewPager.OnPageChangeListener,
        PhoneNumberInteraction.InteractionErrorListener,
        PhoneNumberInteraction.DisambigDialogDismissedListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String ACTION_SHOW_TAB = "ACTION_SHOW_TAB";
    @VisibleForTesting
    private static final String KEY_LAST_TAB = "last_tab";
    private static final String TAG = "FreemeDialtactsActivity";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_WAS_CONFIGURATION_CHANGE = "was_configuration_change";
    private static final String TAG_FAVORITES_FRAGMENT = "favorites";
    /**
     * Just for backward compatibility. Should behave as same as {@link Intent#ACTION_DIAL}.
     */
    private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER";

    private CoordinatorLayout mParentLayout;

    /**
     * Fragment containing the speed dial list, call history list, and all contacts list.
     */
    private FreemeListsFragment mListsFragment;
    /**
     * Tracks whether onSaveInstanceState has been called. If true, no fragment transactions can be
     * commited.
     */
    private boolean mStateSaved;

    private boolean mIsRestarting;
    /**
     * True when this activity has been launched for the first time.
     */
    private boolean mFirstLaunch;

    private DialerDatabaseHelper mDialerDatabaseHelper;
    private boolean mWasConfigurationChange;

    public boolean isMultiSelectModeEnabled;

    private boolean isLastTabEnabled;

    private int mPreviouslySelectedTabIndex;

    /**
     * @param tab the TAB_INDEX_* constant in {@link FreemeListsFragment}
     * @return A intent that will open the FreemeDialtactsActivity into the specified tab. The intent for
     * each tab will be unique.
     */
    public static Intent getShowTabIntent(Context context, int tab) {
        Intent intent = new Intent(context, FreemeDialtactsActivity.class);
        intent.setAction(ACTION_SHOW_TAB);
        intent.putExtra(DialtactsActivity.EXTRA_SHOW_TAB, tab);
        intent.setData(
                new Uri.Builder()
                        .scheme("intent")
                        .authority(context.getPackageName())
                        .appendPath(TAG)
                        .appendQueryParameter(DialtactsActivity.EXTRA_SHOW_TAB, String.valueOf(tab))
                        .build());

        return intent;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.i("FreemeDialtactsActivity.onCreate", "begine");
        Trace.beginSection(TAG + " onCreate");
        super.onCreate(savedInstanceState);

        mFirstLaunch = true;
        isLastTabEnabled = ConfigProviderBindings.get(this).getBoolean("last_tab_enabled", false);

        LogUtil.i("FreemeDialtactsActivity.onCreate", "setContentView begine");
        Trace.beginSection(TAG + " setContentView");
        setContentView(R.layout.freeme_dialtacts_activity);
        Trace.endSection();
        LogUtil.i("FreemeDialtactsActivity.onCreate", "setContentView end");
        getWindow().setBackgroundDrawable(null);

        Trace.beginSection(TAG + " setup Views");

        mPreviouslySelectedTabIndex = DialtactsPagerAdapter.TAB_INDEX_HISTORY;

        // Add the favorites fragment but only if savedInstanceState is null. Otherwise the
        // fragment manager is responsible for recreating it.
        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .add(R.id.dialtacts_frame, new FreemeListsFragment(), TAG_FAVORITES_FRAGMENT)
                    .commit();
        } else {
            mFirstLaunch = savedInstanceState.getBoolean(KEY_FIRST_LAUNCH);
            mWasConfigurationChange = savedInstanceState.getBoolean(KEY_WAS_CONFIGURATION_CHANGE);
        }

        mParentLayout = findViewById(R.id.dialtacts_mainlayout);

        Trace.endSection();

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                Context context = FreemeDialtactsActivity.this;
                Trace.beginSection(TAG + " initialize smart dialing");
                /// M: [MTK Dialer Search] @{
                if (DialerFeatureOptions.isDialerSearchEnabled()) {
                    mDialerDatabaseHelperEx = Database.get(context).getDialerSearchDbHelper(context);
                    mDialerDatabaseHelperEx.startSmartDialUpdateThread();

                    // Monitor this so that we can update callLog info if dismiss an incoming call or
                    // hang up a call in dialer UI
                    if (PermissionsUtil.hasPhonePermissions(context)) {
                        mCallLogObserver.register(CallLog.Calls.CONTENT_URI);
                    } else {
                        LogUtil.w(TAG, "can not register CallLog observer without permission.");
                    }
                    // Monitor this so that we can update contact info
                    // when importing a large number of contacts
                    if (PermissionsUtil.hasContactsReadPermissions(context)) {
                        mContactsObserver.register(ContactsContract.Contacts.CONTENT_URI);
                    } else {
                        LogUtil.w(TAG, "can not register Contacts observer without permission.");
                    }
                } else {
                    /// @}
                    SmartDialPrefix.initializeNanpSettings(context);
                }
                Trace.endSection();

                initAccessibility();
            }
        });

        ///M:[portable]
        CompatChecker.getInstance(this).startCheckerThread();

        mRequst.setIsRecreatedInstance(savedInstanceState != null);
        mRequst.setIsFromNewIntent(false);
        mRequst.resolveIntent(getIntent());

        Trace.endSection();
        LogUtil.i("FreemeDialtactsActivity.onCreate", "end");
    }

    @Override
    protected void onResume() {
        LogUtil.i("FreemeDialtactsActivity.onResume", "begine");
        Trace.beginSection(TAG + " onResume");
        super.onResume();

        // Some calls may not be recorded (eg. from quick contact),
        // so we should restart recording after these calls. (Recorded call is stopped)
        PostCall.restartPerformanceRecordingIfARecentCallExist(this);
        if (!PerformanceReport.isRecording()) {
            PerformanceReport.startRecording();
        }

        ///M: if dialpad was showing when enter background, dialpad should be shown when restore. @{
        if (mCalllogOperator != null) {
            if (mCalllogOperator.isShowDialpadOnResume()) {
                mCalllogOperator.showDialpadFragment(false);
                mCalllogOperator.setShowDialpadOnResume(false);
            }
        }
        /// M @}

        mStateSaved = false;
        if (mFirstLaunch) {
            displayFragment(getIntent());
        } else if (mCalllogOperator != null && mCalllogOperator.isInCallDialpadUp()
                && !phoneIsInUse()) {
            mCalllogOperator.hideDialpadFragment(false, true);
            mCalllogOperator.setInCallDialpadUp(false);
        } else {
            PostCall.promptUserForMessageIfNecessary(this, mParentLayout);
        }

        if (mRequst.isRecreatedInstance() || mRequst.isFromNewIntent()) {
            boolean isSpecifiedTab = false;
            if (mRequst.isFromNewIntent()) {
                isSpecifiedTab = showTabFromIntent();
            }
            if (!isSpecifiedTab) {
                switch (mRequst.getEntranceCode()) {
                    case FreemeEntranceRequst.ENTRANCE_DAIL:
                        mListsFragment.showTab(DialtactsPagerAdapter.TAB_INDEX_HISTORY);
                        if (mCalllogOperator != null) {
                            mCalllogOperator.showDialpadFragment(false);
                        }
                        break;
                    case FreemeEntranceRequst.ENTRANCE_CONTACTS:
                        mListsFragment.showTab(DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS);
                        if (mCalllogOperator != null) {
                            mCalllogOperator.showDialpadFragment(false);
                        }
                        break;
                }
            }
            mRequst.setIsFromNewIntent(false);
            mRequst.setIsRecreatedInstance(false);
        }

        if (mIsRestarting) {
            // This is only called when the activity goes from resumed -> paused -> resumed, so it
            // will not cause an extra view to be sent out on rotation
            if (mCalllogOperator != null && mCalllogOperator.isDialpadShown()) {
                Logger.get(this).logScreenView(ScreenEvent.Type.DIALPAD, this);
            }
            mIsRestarting = false;
        }

        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                Context context = FreemeDialtactsActivity.this;
                /// M: [MTK Dialer Search] @{
                if (!DialerFeatureOptions.isDialerSearchEnabled() && !mWasConfigurationChange) {
                    if (mDialerDatabaseHelper == null) {
                        mDialerDatabaseHelper = Database.get(context).getDatabaseHelper(context);
                    }
                    mDialerDatabaseHelper.startSmartDialUpdateThread();
                } else if (DialerFeatureOptions.isDialerSearchEnabled()) {
                    // If Observer not registered because of permission denied in onCreate(), need to check
                    // permission here and register when permission is ok.
                    if (!mCallLogObserver.isRegistered()) {
                        if (PermissionsUtil.hasPhonePermissions(context)) {
                            mDialerDatabaseHelperEx.startCallLogUpdateThread();
                            mCallLogObserver.register(CallLog.Calls.CONTENT_URI);
                        } else {
                            LogUtil.w(TAG, "can not register CallLog observer without permission on Resume");
                        }
                    }
                    if (!mContactsObserver.isRegistered()) {
                        // Monitor this so that we can update contact info
                        // when importing a large number of contacts
                        if (PermissionsUtil.hasContactsReadPermissions(context)) {
                            mDialerDatabaseHelperEx.startContactUpdateThread();
                            mContactsObserver.register(ContactsContract.Contacts.CONTENT_URI);
                        } else {
                            LogUtil.w(TAG, "can not register Contacts observer without permission onResume");
                        }
                    }
                }
                /// @}

                startAccessibility();
            }
        });

        if (mFirstLaunch) {
            // Only process the Intent the first time onResume() is called after receiving it
            if (CallLog.Calls.CONTENT_TYPE.equals(getIntent().getType())) {
                // Externally specified extras take precedence to EXTRA_SHOW_TAB, which is only
                // used internally.
                final Bundle extras = getIntent().getExtras();
                if (extras != null && extras.getInt(CallLog.Calls.EXTRA_CALL_TYPE_FILTER) == CallLog.Calls.VOICEMAIL_TYPE) {
                    mListsFragment.showTab(DialtactsPagerAdapter.TAB_INDEX_VOICEMAIL);
                    Logger.get(this).logImpression(DialerImpression.Type.VVM_NOTIFICATION_CLICKED);
                } else {
                    mListsFragment.showTab(DialtactsPagerAdapter.TAB_INDEX_HISTORY);
                }
            } else if (getIntent().hasExtra(DialtactsActivity.EXTRA_SHOW_TAB)) {
                showTabFromIntent();
            }

            if (getIntent().getBooleanExtra(DialtactsActivity.EXTRA_CLEAR_NEW_VOICEMAILS, false)) {
                LogUtil.i("FreemeDialtactsActivity.onResume", "clearing all new voicemails");
                CallLogNotificationsService.markAllNewVoicemailsAsOld(this);
            }
        }

        mFirstLaunch = false;

        if (mCalllogOperator != null) {
            mCalllogOperator.onResume();
        }

        Trace.endSection();
        LogUtil.i("FreemeDialtactsActivity.onResume", "end");
    }

    private boolean showTabFromIntent(){
        if (getIntent().hasExtra(DialtactsActivity.EXTRA_SHOW_TAB)) {
            int index = getIntent().getIntExtra(DialtactsActivity.EXTRA_SHOW_TAB,
                    DialtactsPagerAdapter.TAB_INDEX_HISTORY);
            if (index < mListsFragment.getTabCount()) {
                // Hide dialpad since this is an explicit intent to show a specific tab, which is coming
                // from missed call or voicemail notification.
                if (mCalllogOperator != null) {
                    mCalllogOperator.hideDialpadFragment(false, false);
                    mCalllogOperator.exitSearchUi();
                }
                mListsFragment.showTab(index);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onRestart() {
        LogUtil.i("FreemeDialtactsActivity.onRestart", "begine");
        super.onRestart();
        mIsRestarting = true;
        LogUtil.i("FreemeDialtactsActivity.onRestart", "end");
        startAccessibility();
    }

    @Override
    protected void onPause() {
        /// M: dismiss miss call when exit dialer
        if (!isChangingConfigurations()
                && !getSystemService(KeyguardManager.class).isKeyguardLocked()) {
            updateMissedCalls();
        }
        ///@}

        if (mCalllogOperator != null) {
            mCalllogOperator.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        DialerUtils.getDefaultSharedPreferenceForDeviceProtectedStorageContext(this)
                .edit()
                .putInt(KEY_LAST_TAB, mListsFragment.getCurrentTabIndex())
                .apply();
        stopAccessibility();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FIRST_LAUNCH, mFirstLaunch);
        outState.putBoolean(KEY_WAS_CONFIGURATION_CHANGE, isChangingConfigurations());
        mStateSaved = true;
    }

    @Override
    public void onAttachFragment(final Fragment fragment) {
        if (fragment instanceof FreemeListsFragment) {
            mListsFragment = (FreemeListsFragment) fragment;
            mListsFragment.addOnPageChangeListener(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.i(
                "FreemeDialtactsActivity.onActivityResult",
                "requestCode:%d, resultCode:%d",
                requestCode,
                resultCode);
        if (requestCode == DialtactsActivity.ACTIVITY_REQUEST_CODE_CALL_COMPOSE) {
            if (resultCode == RESULT_FIRST_USER) {
                LogUtil.i(
                        "FreemeDialtactsActivity.onActivityResult", "returned from call composer, error occurred");
                String message =
                        getString(
                                R.string.call_composer_connection_failed,
                                data.getStringExtra(CallComposerActivity.KEY_CONTACT_NAME));
                Snackbar.make(mParentLayout, message, Snackbar.LENGTH_LONG).show();
            } else {
                LogUtil.i("FreemeDialtactsActivity.onActivityResult", "returned from call composer, no error");
            }
        } else if (requestCode == DialtactsActivity.ACTIVITY_REQUEST_CODE_CALL_DETAILS) {
            if (resultCode == RESULT_OK
                    && data != null
                    && data.getBooleanExtra(CallDetailsActivity.EXTRA_HAS_ENRICHED_CALL_DATA, false)) {
                String number = data.getStringExtra(CallDetailsActivity.EXTRA_PHONE_NUMBER);
                int snackbarDurationMillis = 5_000;
                Snackbar.make(mParentLayout, getString(R.string.ec_data_deleted), snackbarDurationMillis)
                        .setAction(
                                R.string.view_conversation,
                                v -> startActivity(IntentProvider.getSendSmsIntentProvider(number).getIntent(this)))
                        .setActionTextColor(getResources().getColor(R.color.dialer_snackbar_action_text_color))
                        .show();
            }
        }
        /** M: [VoLTE ConfCall] Handle the volte conference call. @{ */
        else if (requestCode == DialerVolteUtils.ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS) {
            if (resultCode == RESULT_OK) {
                DialerVolteUtils.launchVolteConfCall(this, data);
            } else {
                Log.d(TAG, "No contacts picked, Volte conference call cancelled.");
            }
        }
        /** @} */
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Update the number of unread voicemails (potentially other tabs) displayed next to the tab icon.
     */
    public void updateTabUnreadCounts() {
        mListsFragment.updateTabUnreadCounts();
    }

    /**
     * Callback from child FreemeDialpadFragment when the dialpad is shown.
     */
    public void onDialpadShown() {
        if (mCalllogOperator != null) {
            mCalllogOperator.onDialpadShown();
        }
    }

    /**
     * Initiates animations and other visual updates to hide the dialpad. The fragment is hidden in a
     * callback after the hide animation ends.
     *
     * @see #commitDialpadFragmentHide
     */
    public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
        if (mCalllogOperator != null) {
            mCalllogOperator.hideDialpadFragment(animate, clearDialpad);
        }
    }

    /**
     * Returns true if the intent is due to hitting the green send key (hardware call button:
     * KEYCODE_CALL) while in a call.
     *
     * @param intent the intent that launched this activity
     * @return true if the intent is due to hitting the green send key while in a call
     */
    private boolean isSendKeyWhileInCall(Intent intent) {
        // If there is a call in progress and the user launched the dialer by hitting the call
        // button, go straight to the in-call screen.
        final boolean callKey = Intent.ACTION_CALL_BUTTON.equals(intent.getAction());

        // When KEYCODE_CALL event is handled it dispatches an intent with the ACTION_CALL_BUTTON.
        // Besides of checking the intent action, we must check if the phone is really during a
        // call in order to decide whether to ignore the event or continue to display the activity.
        if (callKey && phoneIsInUse()) {
            TelecomUtil.showInCallScreen(this, false);
            return true;
        }

        return false;
    }

    /**
     * Sets the current tab based on the intent's request type
     *
     * @param intent Intent that contains information about which tab should be selected
     */
    private void displayFragment(Intent intent) {
        // If we got here by hitting send and we're in call forward along to the in-call activity
        if (isSendKeyWhileInCall(intent)) {
            finish();
            return;
        }

        final boolean showDialpadChooser =
                !ACTION_SHOW_TAB.equals(intent.getAction())
                        && phoneIsInUse()
                        && !FreemeDialpadFragment.isAddCallMode(intent);
        if (showDialpadChooser || (intent.getData() != null && isDialIntent(intent))) {
            if (mListsFragment != null) {
                mListsFragment.showTab(DialtactsPagerAdapter.TAB_INDEX_HISTORY);
            }
            if (mCalllogOperator != null) {
                mCalllogOperator.showDialpadFragment(false);
                mCalllogOperator.setStartedFromNewIntent(true);
                if (showDialpadChooser && !mCalllogOperator.isDialpadVisible()) {
                    mCalllogOperator.setInCallDialpadUp(true);
                }
            }
        } else if (isLastTabEnabled) {
            @DialtactsPagerAdapter.TabIndex
            int tabIndex =
                    DialerUtils.getDefaultSharedPreferenceForDeviceProtectedStorageContext(this)
                            .getInt(KEY_LAST_TAB, DialtactsPagerAdapter.TAB_INDEX_SPEED_DIAL);
            // If voicemail tab is saved and its availability changes, we still move to the voicemail tab
            // but it is quickly removed and shown the contacts tab.
            if (mListsFragment != null) {
                mListsFragment.showTab(tabIndex);
                PerformanceReport.setStartingTabIndex(tabIndex);
            } else {
                PerformanceReport.setStartingTabIndex(DialtactsPagerAdapter.TAB_INDEX_SPEED_DIAL);
            }
        }
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        setIntent(newIntent);

        if (mRequst != null && !mRequst.isRecreatedInstance()) {
            mRequst.setIsFromNewIntent(true);
            mRequst.resolveIntent(getIntent());
        }

        mFirstLaunch = true;

        mStateSaved = false;

        displayFragment(newIntent);

        invalidateOptionsMenu();

        if (mRequst.isRecreatedInstance() || mRequst.isFromNewIntent()) {
            int code = mRequst.getEntranceCode();
            if ((code == FreemeEntranceRequst.ENTRANCE_DAIL
                    && mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS))
                    || (code == FreemeEntranceRequst.ENTRANCE_CONTACTS
                    && mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY))) {
                mListsFragment.isNeedExitMultiMode();
            }
            if (code == FreemeEntranceRequst.ENTRANCE_CONTACTS) {
                if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                        && mCalllogOperator != null) {
                    mCalllogOperator.exitSearchUi();
                    mCalllogOperator.showDialpadFragment(false);
                }
            }
        }
    }

    /**
     * Returns true if the given intent contains a phone number to populate the dialer with
     */
    private boolean isDialIntent(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || ACTION_TOUCH_DIALER.equals(action)) {
            return true;
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            final Uri data = intent.getData();
            if (data != null && PhoneAccount.SCHEME_TEL.equals(data.getScheme())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        PerformanceReport.recordClick(UiAction.Type.PRESS_ANDROID_BACK_BUTTON);

        if (mListsFragment != null && mListsFragment.isNeedExitMultiMode()) {
            return;
        }

        if (mStateSaved) {
            return;
        }

        if (mListsFragment != null) {
            if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                    && mCalllogOperator != null) {
                if (mCalllogOperator.onBackPress()) {
                    return;
                }
                if (mCalllogOperator.isInSearchUi()) {
                    mCalllogOperator.exitSearchUi();
                    DialerUtils.hideInputMethod(mParentLayout);
                    return;
                }
            } else if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS)) {
                if (mListsFragment.inSearchContactorMode()) {
                    mListsFragment.clearSearchContactorFocus();
                    return;
                }
                mListsFragment.clearSearchContactorFocus();
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onDialpadQueryChanged(String query) {

        final String normalizedQuery =
                SmartDialNameMatcher.normalizeNumber(query,
                        /* M: [MTK Dialer Search] use mtk enhance dialpad map */
                        DialerFeatureOptions.isDialerSearchEnabled() ?
                                SmartDialNameMatcher.SMART_DIALPAD_MAP
                                : SmartDialNameMatcher.LATIN_SMART_DIAL_MAP);

        boolean isNeedEnterSearch = !TextUtils.isEmpty(normalizedQuery);
        mListsFragment.setSearchQuery(normalizedQuery);
        mListsFragment.updateSearchUI(isNeedEnterSearch);

        if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                && mCalllogOperator != null) {
            mCalllogOperator.onDialpadQueryChanged(query, normalizedQuery);
        }
    }

    @Override
    public boolean onDialpadSpacerTouchWithEmptyQuery() {
        PerformanceReport.recordClick(UiAction.Type.CLOSE_DIALPAD);
        if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                && mCalllogOperator != null) {
            mCalllogOperator.hideDialpadFragment(true, true);
        }
        return true;
    }

    @Override
    public void onListFragmentScrollStateChange(int scrollState) {
        PerformanceReport.recordScrollStateChange(scrollState);
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                    && mCalllogOperator != null) {
                mCalllogOperator.hideDialpadFragment(true, false);
            }
            DialerUtils.hideInputMethod(mParentLayout);
        }
    }

    @Override
    public void onListFragmentScroll(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // TODO: No-op for now. This should eventually show/hide the actionBar based on
        // interactions with the ListsFragments.
    }

    private boolean phoneIsInUse() {
        return TelecomUtil.isInCall(this);
    }

    /**
     * Called when the user has long-pressed a contact tile to start a drag operation.
     */
    @Override
    public void onDragStarted(int x, int y, PhoneFavoriteSquareTileView view) {
        mListsFragment.showRemoveView(true);
    }

    @Override
    public void onDragHovered(int x, int y, PhoneFavoriteSquareTileView view) {
    }

    /**
     * Called when the user has released a contact tile after long-pressing it.
     */
    @Override
    public void onDragFinished(int x, int y) {
        mListsFragment.showRemoveView(false);
    }

    @Override
    public void onDroppedOnRemove() {
    }

    /**
     * Allows the SpeedDialFragment to attach the drag controller to mRemoveViewContainer once it has
     * been attached to the activity.
     */
    @Override
    public void setDragDropController(DragDropController dragController) {
        if (mCalllogOperator != null) {
            mCalllogOperator.setDragDropController(dragController);
        }
        mListsFragment.getRemoveView().setDragDropController(dragController);
    }

    /**
     * Implemented to satisfy {@link OldSpeedDialFragment.HostInterface}
     */
    @Override
    public void showAllContactsTab() {
        if (mListsFragment != null) {
            mListsFragment.showTab(DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS);
        }
    }

    /**
     * Implemented to satisfy {@link CallLogFragment.HostInterface}
     */
    @Override
    public void showDialpad() {
        if (mCalllogOperator != null) {
            mCalllogOperator.showDialpadFragment(true);
        }
    }

    @Override
    public void enableFloatingButton(boolean enabled) {
        if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                && mCalllogOperator != null) {
            mCalllogOperator.enableFloatingButton(enabled);
        }
    }

    @Override
    public void onPickDataUri(
            Uri dataUri, boolean isVideoCall, CallSpecificAppData callSpecificAppData) {
        if (mCalllogOperator != null){
            mCalllogOperator.setClearSearchOnPause(true);
        }
        PhoneNumberInteraction.startInteractionForPhoneCall(
                FreemeDialtactsActivity.this, dataUri, isVideoCall, callSpecificAppData);
    }

    @Override
    public void onPickPhoneNumber(
            String phoneNumber, boolean isVideoCall, CallSpecificAppData callSpecificAppData) {
        if (phoneNumber == null) {
            // Invalid phone number, but let the call go through so that InCallUI can show
            // an error message.
            phoneNumber = "";
        }

        Intent intent = new CallIntentBuilder(phoneNumber, callSpecificAppData)
                .setIsVideoCall(isVideoCall).build();
        DialerUtils.startActivityWithErrorToast(this, intent);
        if (mCalllogOperator != null){
            mCalllogOperator.setClearSearchOnPause(true);
        }
    }

    @Override
    public void onHomeInActionBarSelected() {
        if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                && mCalllogOperator != null) {
            mCalllogOperator.exitSearchUi();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        updateMissedCalls();
        int tabIndex = mListsFragment.getCurrentTabIndex();
        mPreviouslySelectedTabIndex = tabIndex;
        LogUtil.i("FreemeDialtactsActivity.onPageSelected", "position: %d", position);

        boolean isEnable = true;
        if (tabIndex == DialtactsPagerAdapter.TAB_INDEX_HISTORY) {
            isEnable = !mIsMultiSelectMode;
            if (mCalllogOperator != null) {
                isEnable = isEnable && !isDialpadShown() && !isInSearchUi();
            }
        } else if (tabIndex == DialtactsPagerAdapter.TAB_INDEX_ALL_CONTACTS) {
            isEnable = !mIsMultiSelectMode;
        }
        mListsFragment.setEnableSwipingPages(isEnable);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public boolean isActionBarShowing() {
        return mListsFragment != null ? mListsFragment.isTabBarShowing() : false;
    }

    @Override
    public boolean isDialpadShown() {
        return mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                && mCalllogOperator != null && mCalllogOperator.isDialpadShown();
    }

    @Override
    public int getDialpadHeight() {
        if (mCalllogOperator != null) {
            return mCalllogOperator.getDialpadHeight();
        }
        return 0;
    }

    @Override
    public int getActionBarHeight() {
        return 0 /*mActionBarHeight*/;
    }

    private void updateMissedCalls() {
        if (mPreviouslySelectedTabIndex == DialtactsPagerAdapter.TAB_INDEX_HISTORY) {
            mListsFragment.markMissedCallsAsReadAndRemoveNotifications();
        }
    }

    @Override
    public void onDisambigDialogDismissed() {
        // Don't do anything; the app will remain open with favorites tiles displayed.
    }

    @Override
    public void interactionError(@PhoneNumberInteraction.InteractionErrorCode int interactionErrorCode) {
        switch (interactionErrorCode) {
            case PhoneNumberInteraction.InteractionErrorCode.USER_LEAVING_ACTIVITY:
                // This is expected to happen if the user exits the activity before the interaction occurs.
                return;
            case PhoneNumberInteraction.InteractionErrorCode.CONTACT_NOT_FOUND:
            case PhoneNumberInteraction.InteractionErrorCode.CONTACT_HAS_NO_NUMBER:
            case PhoneNumberInteraction.InteractionErrorCode.OTHER_ERROR:
            default:
                // All other error codes are unexpected. For example, it should be impossible to start an
                // interaction with an invalid contact from the Dialtacts activity.
                Assert.fail("PhoneNumberInteraction error: " + interactionErrorCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        // This should never happen; it should be impossible to start an interaction without the
        // contacts permission from the Dialtacts activity.
        Assert.fail(
                String.format(
                        Locale.US,
                        "Permissions requested unexpectedly: %d/%s/%s",
                        requestCode,
                        Arrays.toString(permissions),
                        Arrays.toString(grantResults)));
    }

    @Override
    public void onActionModeStateChanged(boolean isEnabled) {
        isMultiSelectModeEnabled = isEnabled;
    }

    @Override
    public boolean isActionModeStateEnabled() {
        return isMultiSelectModeEnabled;
    }

    /// M: Mediatek start.
    /// M: [MTK Dialer Search] @{
    /**
     * Dialer search database helper.
     */
    private DialerDatabaseHelperEx mDialerDatabaseHelperEx;
    private final Handler mHandler = new Handler();
    private final ThrottleContentObserver mContactsObserver = new ThrottleContentObserver(mHandler,
            this, new Runnable() {
        @Override
        public void run() {
            DialerDatabaseHelperEx dbHelper = Database.get(getApplicationContext())
                    .getDialerSearchDbHelper(getApplicationContext());
            dbHelper.startContactUpdateThread();
        }
    }, "ContactsObserver");
    private final ThrottleContentObserver mCallLogObserver = new ThrottleContentObserver(mHandler,
            this, new Runnable() {
        @Override
        public void run() {
            DialerDatabaseHelperEx dbHelper = Database.get(getApplicationContext())
                    .getDialerSearchDbHelper(getApplicationContext());
            dbHelper.startCallLogUpdateThread();
        }
    }, "CallLogObserver");

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            mCallLogObserver.unregister();
            mContactsObserver.unregister();
        }
        super.onDestroy();
    }
    /// @}
    /// M: Mediatek end.

    private FreemeEntranceRequst mRequst = new FreemeEntranceRequst();

    public FreemeEntranceRequst getFreemeEntriceRequst() {
        return mRequst;
    }

    private boolean mIsMultiSelectMode;
    public void setMultiSelectMode(boolean isMultiSelectMode, int page) {
        if (page == mListsFragment.getViewPager().getCurrentItem()) {
            mIsMultiSelectMode = isMultiSelectMode;
            if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                    && mCalllogOperator != null) {
                if (isMultiSelectMode) {
                    mCalllogOperator.setFloatingButtonVisible(false);
                } else {
                    mCalllogOperator.setFloatingButtonVisible(!mCalllogOperator.isDialpadVisible());
                }

                mListsFragment.getViewPager().setEnableSwipingPages(!isMultiSelectMode
                        && !isInSearchUi() && !isDialpadShown());
            } else {
                mListsFragment.getViewPager().setEnableSwipingPages(!isMultiSelectMode);
            }

            if (mListsFragment != null) {
                mListsFragment.showOrHideToolbar(isMultiSelectMode);
            }
        }
    }

    public void updateActionBarTitle(int count, int page) {
        if (page == mListsFragment.getViewPager().getCurrentItem()) {
            mListsFragment.updateActionbarTitle(getString(R.string.freeme_selected_count, count));
        }
    }

    public void updateActionBarMenuText() {
        if (mListsFragment != null) {
            int title;
            if (mIsAllCalllogsSelected || mIsAllContactsSelected) {
                title = R.string.menu_select_none;
            } else {
                title = R.string.menu_select_all;
            }
            mListsFragment.updateActionBarMenuText(title);
        }
    }

    private boolean mIsAllCalllogsSelected;

    public void setIsAllCalllogsSelected(boolean isAllCalllogsSelected) {
        mIsAllCalllogsSelected = isAllCalllogsSelected;
        updateActionBarMenuText();
    }

    private boolean mIsAllContactsSelected;

    public void setIsAllContactsSelected(boolean isAllContactsSelected) {
        mIsAllContactsSelected = isAllContactsSelected;
        updateActionBarMenuText();
    }

    private FreemeCallAccessibility mSmartDialAccessibility;

    private void initAccessibility() {
        mSmartDialAccessibility = new FreemeCallAccessibility(this,
                FreemeCallAccessibility.TYPE_SMART_DIAL,
                new FreemeCallAccessibility.IFreemeSmartAction() {
                    @Override
                    public void smartAnswer() {
                    }

                    @Override
                    public void smartDial() {
                        outGoingCallBySpecifiedSim();
                    }
                });
    }

    public void outGoingCallBySpecifiedSim() {
        if (mCalllogOperator == null || !mCalllogOperator.hasSearchQuery()) {
            return;
        }
        String mNumber = mCalllogOperator.getSearchQuery();
        Intent intent = new CallIntentBuilder(mNumber, CallInitiationType.Type.DIALPAD)
                .setPhoneAccountHandle(FreemeDialerUtils.getDefaultSmartDialAccount(this))
                .build();
        DialerUtils.startActivityWithErrorToast(this, intent);
        if (mSmartDialAccessibility != null) {
            mSmartDialAccessibility.vibrator();
            mSmartDialAccessibility.stop();
        }
        if (mListsFragment.isSpecifiedPage(DialtactsPagerAdapter.TAB_INDEX_HISTORY)
                && mCalllogOperator != null) {
            mCalllogOperator.clearDialpad();
        }
    }

    private void startAccessibility() {
        if (mSmartDialAccessibility != null) {
            mSmartDialAccessibility.start();
        }
    }

    private void stopAccessibility() {
        if (mSmartDialAccessibility != null) {
            mSmartDialAccessibility.stop();
        }
    }

    private IFreemeCallLogOperatorInterface mCalllogOperator;
    public void setFreemeDialpadOperator(IFreemeCallLogOperatorInterface operator) {
        this.mCalllogOperator = operator;
    }

    public boolean isInSearchUi() {
        return mCalllogOperator != null && mCalllogOperator.isInSearchUi();
    }

    public boolean hasSearchQuery() {
        return mCalllogOperator != null && mCalllogOperator.hasSearchQuery();
    }
}
