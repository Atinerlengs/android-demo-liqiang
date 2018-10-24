/*
* Copyright (C) 2017 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.contacts.activities;

import android.accounts.Account;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.ProviderStatus;
import android.support.annotation.LayoutRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
/*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
import android.support.v7.widget.Toolbar;
//*/
//import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.drawer.DrawerFragment;
import com.android.contacts.drawer.DrawerFragment.DrawerFragmentListener;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.group.GroupMembersFragment;
import com.android.contacts.group.GroupNameEditDialogFragment;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.list.AccountFilterActivity;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ContactListFilterController.ContactListFilterListener;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsUnavailableFragment;
import com.android.contacts.list.DefaultContactBrowseListFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.ProviderStatusWatcher;
import com.android.contacts.list.ProviderStatusWatcher.ProviderStatusListener;
import com.android.contacts.logging.Logger;
import com.android.contacts.logging.ScreenEvent.ScreenType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.preference.ContactsPreferenceActivity;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.Constants;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.SharedPreferenceUtil;
import com.android.contacts.util.SyncUtil;
import com.android.contacts.util.ViewUtil;
import com.android.contacts.widget.FloatingActionButtonController;
import com.android.contactsbind.FeatureHighlightHelper;
import com.android.contactsbind.HelpUtils;
import com.android.contactsbind.ObjectFactory;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.ContactsApplicationEx;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.model.AccountTypeManagerEx;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimGroupProcessor;
import com.mediatek.contacts.util.ContactsConstants;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.PDebug;

import com.google.common.util.concurrent.Futures;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
import android.app.Activity;
import android.widget.Toolbar;
//*/

/**
 * Displays a list to browse contacts.
 */
public class PeopleActivity extends AppCompatContactsActivity implements
        DrawerFragmentListener,
        SelectAccountDialogFragment.Listener,
        /* M: [Sim Group] add SimGroupProcessor.Listener @{ */
        SimGroupProcessor.Listener /* @} */ {

    /** Possible views of Contacts app. */
    public enum ContactsView {
        NONE,
        ALL_CONTACTS,
        ASSISTANT,
        GROUP_VIEW,
        ACCOUNT_VIEW,
    }

    private static final String TAG = "PeopleActivity";
    private static final String TAG_ALL = "contacts-all";
    private static final String TAG_UNAVAILABLE = "contacts-unavailable";
    private static final String TAG_GROUP_VIEW = "contacts-groups";
    private static final String TAG_SELECT_ACCOUNT_DIALOG = "selectAccountDialog";
    private static final String TAG_GROUP_NAME_EDIT_DIALOG = "groupNameEditDialog";

    public static final String TAG_ASSISTANT = "contacts-assistant";
    public static final String TAG_SECOND_LEVEL = "second-level";
    public static final String TAG_THIRD_LEVEL = "third-level";
    public static final String TAG_ASSISTANT_HELPER = "assistant-helper";
    public static final String TAG_DUPLICATES = "DuplicatesFragment";
    public static final String TAG_DUPLICATES_UTIL = "DuplicatesUtilFragment";

    private static final String KEY_GROUP_URI = "groupUri";
    private static final String KEY_CONTACTS_VIEW = "contactsView";
    private static final String KEY_NEW_GROUP_ACCOUNT = "newGroupAccount";

    private static final long DRAWER_CLOSE_DELAY = 300L;

    /// M: Add for ALPS02383518, when received PHB_CHANGED intent but has no
    private static final String ACTION_REFRESH_SIM_CONTACT =
            "com.android.contacts.REFRESH_SIM_CONTACT";
    ///
    private ContactsIntentResolver mIntentResolver;
    private ContactsRequest mRequest;
    private AccountTypeManager mAccountTypeManager;

    private FloatingActionButtonController mFloatingActionButtonController;
    private View mFloatingActionButtonContainer;
    private boolean wasLastFabAnimationScaleIn = false;

    private ProviderStatusWatcher mProviderStatusWatcher;
    private Integer mProviderStatus;

    private BroadcastReceiver mSaveServiceListener;

    private boolean mShouldSwitchToGroupView;

    private ContactsView mCurrentView;

    /*/ freeme.zhaozehong, 20180314. for freemeOS, UI redesign
    private CoordinatorLayout mLayoutRoot;
    /*/
    private View mLayoutRoot;
    //*/

    /**
     * Showing a list of Contacts. Also used for showing search results in search mode.
     */
    private DefaultContactBrowseListFragment mContactsListFragment;

    private GroupMembersFragment mMembersFragment;
    private Uri mGroupUri;

    /**
     * True if this activity instance is a re-created one.  i.e. set true after orientation change.
     */
    private boolean mIsRecreatedInstance;

    private boolean mShouldSwitchToAllContacts;

    /** Sequential ID assigned to each instance; used for logging */
    private final int mInstanceId;
    private static final AtomicInteger sNextInstanceId = new AtomicInteger();

    private ContactListFilterController mContactListFilterController;

    /** Navigation drawer related */
    private DrawerLayout mDrawerLayout;
    private DrawerFragment mDrawerFragment;
    private ContactsActionBarDrawerToggle mToggle;
    private Toolbar mToolbar;

    // The account the new group will be created under.
    private AccountWithDataSet mNewGroupAccount;

    private Object mStatusChangeListenerHandle;

    private final Handler mHandler = new Handler();

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            mHandler.post(new Runnable() {
                public void run() {
                    onSyncStateUpdated();
                }
            });
        }
    };

    // Update sync status for accounts in current ContactListFilter
    private void onSyncStateUpdated() {
        if (isListFragmentInSearchMode() || isListFragmentInSelectionMode()) {
            return;
        }

        final ContactListFilter filter = mContactListFilterController.getFilter();
        if (filter != null) {
            final SwipeRefreshLayout swipeRefreshLayout =
                    mContactsListFragment.getSwipeRefreshLayout();
            if (swipeRefreshLayout == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Can not load swipeRefreshLayout, swipeRefreshLayout is null");
                }
                return;
            }

            final List<AccountWithDataSet> accounts;
            if (filter.filterType == ContactListFilter.FILTER_TYPE_ACCOUNT &&
                    filter.isGoogleAccountType()) {
                accounts = Collections.singletonList(new AccountWithDataSet(filter.accountName,
                        filter.accountType, null));
            } else if (filter.shouldShowSyncState()) {
                accounts = AccountInfo.extractAccounts(
                        mAccountTypeManager.getWritableGoogleAccounts());
            } else {
                accounts = Collections.emptyList();
            }
            if (SyncUtil.isAnySyncing(accounts)) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public void showConnectionErrorMsg() {
        Snackbar.make(mLayoutRoot, R.string.connection_error_message, Snackbar.LENGTH_LONG).show();
    }

    private final ContactListFilterListener mFilterListener = new ContactListFilterListener() {
        @Override
        public void onContactListFilterChanged() {
            final ContactListFilter filter = mContactListFilterController.getFilter();
            handleFilterChangeForFragment(filter);
            handleFilterChangeForActivity(filter);
        }
    };

    private final ProviderStatusListener mProviderStatusListener = new ProviderStatusListener() {
        @Override
        public void onProviderStatusChange() {
            Log.d(TAG, "[onProviderStatusChange]");
            // TODO see if it works with drawer fragment.
            updateViewConfiguration(false);
        }
    };

    private class ContactsActionBarDrawerToggle extends ActionBarDrawerToggle {
        private boolean mMenuClickedBefore = SharedPreferenceUtil.getHamburgerMenuClickedBefore(
                PeopleActivity.this);

        /*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
        public ContactsActionBarDrawerToggle(AppCompatActivity activity, DrawerLayout drawerLayout,
                Toolbar toolbar, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes,
                    closeDrawerContentDescRes);
        }
        /*/
        public ContactsActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
                                             Toolbar toolbar, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, null, openDrawerContentDescRes,
                    closeDrawerContentDescRes);
        }
        //*/

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            if (!mMenuClickedBefore) {
                SharedPreferenceUtil.setHamburgerMenuClickedBefore(PeopleActivity.this);
                mMenuClickedBefore = true;
            }
            /// M:[ALPS03460511][ConfCall] Update Conf call state everytime @{
            if (mDrawerFragment != null) {
                mDrawerFragment.updateConfCallState();
            }
            /// @}
            drawerView.requestFocus();
            invalidateOptionsMenu();
            // Stop search and selection mode like Gmail and Keep. Otherwise, if user switches to
            // another fragment in navigation drawer, the current search/selection mode will be
            // overlaid by the action bar of the newly-created fragment.
            stopSearchAndSelection();
            updateStatusBarBackground();
        }

        private void stopSearchAndSelection() {
            final MultiSelectContactsListFragment listFragment;
            if (isAllContactsView() || isAccountView()) {
                listFragment = getListFragment();
            } else if (isGroupView()) {
                listFragment = getGroupFragment();
            } else {
                listFragment = null;
            }
            if (listFragment == null) {
                return;
            }
            final ActionBarAdapter actionBarAdapter = listFragment.getActionBarAdapter();
            if (actionBarAdapter == null) {
                return;
            }
            if (actionBarAdapter.isSearchMode()) {
                actionBarAdapter.setSearchMode(false);
            } else if (actionBarAdapter.isSelectionMode()) {
                actionBarAdapter.setSelectionMode(false);
            }
        }

        @Override
        public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            invalidateOptionsMenu();
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            super.onDrawerStateChanged(newState);
            // Set transparent status bar when drawer starts to move.
            if (newState != DrawerLayout.STATE_IDLE) {
                updateStatusBarBackground();
            }
        }
    }


    public PeopleActivity() {
        Log.w(TAG, "[PeopleActivity]new");
        mInstanceId = sNextInstanceId.getAndIncrement();
        mIntentResolver = new ContactsIntentResolver(this);
        /** M: Bug Fix for ALPS00407311 @{ */
        mProviderStatusWatcher = ProviderStatusWatcher.getInstance(ContactsApplicationEx
                .getContactsApplication());
        /** @} */
    }

    @Override
    public String toString() {
        // Shown on logcat
        return String.format("%s@%d", getClass().getSimpleName(), mInstanceId);
    }

    private boolean areContactsAvailable() {
        return (mProviderStatus != null) && mProviderStatus.equals(ProviderStatus.STATUS_NORMAL)
                ///M: add Op01 plugin @{
                || ExtensionManager.getInstance().getOp01Extension()
                .areContactAvailable(mProviderStatus);
                /// @}
    }

    @Override
    protected void onCreate(Bundle savedState) {
        Log.w(TAG,"[onCreate] savedState=" + savedState);
        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate start");
        }

        // Make sure this is *before* calling super.onCreate
        setTheme(R.style.PeopleActivityTheme);
        super.onCreate(savedState);

        /// M: [Sim Group] @{
        SimGroupProcessor.registerListener(this);
        /// @}

        mAccountTypeManager = AccountTypeManager.getInstance(this);
        mContactListFilterController = ContactListFilterController.getInstance(this);

        RequestPermissionsActivity.startPermissionActivityIfNeeded(this);

        /// M: Add for ALPS02383518, when received PHB_CHANGED intent but has no
        // READ_PHONE permission, marked NEED_REFRESH_SIM_CONTACTS as true. So refresh
        // all SIM contacts after open all permission and back to contacts at here. @{
        Log.d(TAG, "[onCreate] refresh all SIM contacts");
        Intent intent = new Intent(ACTION_REFRESH_SIM_CONTACT);
        sendBroadcast(intent);
        /// @}

        if (!processIntent(false)) {
            finish();
            Log.w(TAG, "[onCreate]can not process intent:" + getIntent());
            return;
        }

        Log.d(TAG, "[Performance test][Contacts] loading data start time: ["
                + System.currentTimeMillis() + "]");

        mContactListFilterController.checkFilterValidity(false);

        super.setContentView(R.layout.contacts_drawer_activity);

        // Set up the action bar.
        mToolbar = getView(R.id.toolbar);
        /*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
        setSupportActionBar(mToolbar);
        /*/
        setActionBar(mToolbar);
        //*/

        // Add shadow under toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        // Set up hamburger button.
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerFragment = (DrawerFragment) getFragmentManager().findFragmentById(R.id.drawer);
        mToggle = new ContactsActionBarDrawerToggle(this, mDrawerLayout, mToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(mToggle);
        // Set fallback handler for when drawer is disabled.
        mToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Set up navigation mode.
        if (savedState != null) {
            mCurrentView = ContactsView.values()[savedState.getInt(KEY_CONTACTS_VIEW)];
        } else {
            mCurrentView = ContactsView.ALL_CONTACTS;
        }

        if (savedState != null && savedState.containsKey(KEY_NEW_GROUP_ACCOUNT)) {
            mNewGroupAccount = AccountWithDataSet.unstringify(
                    savedState.getString(KEY_NEW_GROUP_ACCOUNT));
        }

        mContactListFilterController.addListener(mFilterListener);
        mProviderStatusWatcher.addListener(mProviderStatusListener);

        mIsRecreatedInstance = (savedState != null);

        if (mIsRecreatedInstance) {
            mGroupUri = savedState.getParcelable(KEY_GROUP_URI);
        }

        PDebug.Start("createViewsAndFragments");
        createViewsAndFragments();

        if (Log.isLoggable(Constants.PERFORMANCE_TAG, Log.DEBUG)) {
            Log.d(Constants.PERFORMANCE_TAG, "PeopleActivity.onCreate finish");
        }
        getWindow().setBackgroundDrawable(null);

        PDebug.End("Contacts.onCreate");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        PDebug.Start("onNewIntent");
        Log.d(TAG, "[onNewIntent] action = " + intent.getAction()
                + ", mGroupUri = " + intent.getData());
        /* M: [Sim Group] Fix ALPS03435685. If has done toast
         * in SimGroupProcessor, no need to use aosp toast again. 1/3 @{ */
        boolean doneToast = intent.getBooleanExtra(SimGroupUtils.EXTRA_DONE_TOAST, false);
        /* @} */
        final String action = intent.getAction();
        if (GroupUtil.ACTION_CREATE_GROUP.equals(action)) {
            mGroupUri = intent.getData();
            if (mGroupUri == null) {
                /* M: [Sim Group] Fix ALPS03435685. 2/3 @{ */
                if (doneToast) {
                    return;
                }
                /* @} */
                toast(R.string.groupSavedErrorToast);
                return;
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "Received group URI " + mGroupUri);
            switchView(ContactsView.GROUP_VIEW);
            /// M:[ALPS03508011]Add check valid before use mMembersFragment. @{
            if (mMembersFragment != null) {
                mMembersFragment.toastForSaveAction(action);
            }
            /// @}
            return;
        }

        if (isGroupSaveAction(action)) {
            mGroupUri = intent.getData();
            if (mGroupUri == null) {
                /* M: [Sim Group]
                 * Fix: if rename sim group that exceeds phb's limitation,
                 * group fragment UI will display wrong.
                 * Solution: if rename fail, no need popSecondLevel(). @{ */
                if (!GroupUtil.ACTION_UPDATE_GROUP.equals(action)) {
                /* @} */
                    popSecondLevel();
                }
                /* M: [Sim Group] Fix ALPS03435685. 3/3 @{ */
                if (doneToast) {
                    return;
                }
                /* @} */
                toast(R.string.groupSavedErrorToast);
                return;
            }
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "Received group URI " + mGroupUri);
            // ACTION_REMOVE_FROM_GROUP doesn't reload data, so it shouldn't cause b/32223934
            // but it's necessary to use the previous fragment since
            // GroupMembersFragment#mIsEditMode needs to be persisted between remove actions.
            if (GroupUtil.ACTION_REMOVE_FROM_GROUP.equals(action)) {
                switchToOrUpdateGroupView(action);
            } else {
                switchView(ContactsView.GROUP_VIEW);
            }
            /// M:[Google Issue][ALPS03508011]Add check valid before use mMembersFragment. @{
            if (mMembersFragment != null) {
                mMembersFragment.toastForSaveAction(action);
            }
            // @}
        }

        setIntent(intent);

        if (!processIntent(true)) {
            finish();
            Log.w(TAG, "[onNewIntent]can not process intent:" + getIntent());
            return;
        }
        Log.d(TAG, "[onNewIntent]");

        mContactListFilterController.checkFilterValidity(false);

        if (!isInSecondLevel()) {
            // Re-initialize ActionBarAdapter because {@link #onNewIntent(Intent)} doesn't invoke
            // {@link Fragment#onActivityCreated(Bundle)} where we initialize ActionBarAdapter
            // initially.
            mContactsListFragment.setParameters(/* ContactsRequest */ mRequest,
                    /* fromOnNewIntent */ true);
            mContactsListFragment.initializeActionBarAdapter(null);
        }
        ///M:[Google Issue][ALPS03477732]Need reset DrawerFragement if all contacts intent.@{
        if (mRequest != null &&
            mRequest.getActionCode() == ContactsRequest.ACTION_ALL_CONTACTS) {
            mShouldSwitchToAllContacts = true;
            Log.d(TAG, "[onNewIntent]set mShouldSwitchToAllContacts");
        }
        /// @}

        initializeFabVisibility();
        invalidateOptionsMenuIfNeeded();
        PDebug.End("onNewIntent");
    }

    private static boolean isGroupSaveAction(String action) {
        return GroupUtil.ACTION_UPDATE_GROUP.equals(action)
                || GroupUtil.ACTION_ADD_TO_GROUP.equals(action)
                || GroupUtil.ACTION_REMOVE_FROM_GROUP.equals(action);
    }

    private void toast(int resId) {
        if (resId >= 0) {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resolve the intent and initialize {@link #mRequest}, and launch another activity if redirect
     * is needed.
     *
     * @param forNewIntent set true if it's called from {@link #onNewIntent(Intent)}.
     * @return {@code true} if {@link PeopleActivity} should continue running.  {@code false}
     *         if it shouldn't, in which case the caller should finish() itself and shouldn't do
     *         farther initialization.
     */
    private boolean processIntent(boolean forNewIntent) {
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, this + " processIntent: forNewIntent=" + forNewIntent
                    + " intent=" + getIntent() + " request=" + mRequest);
        }
        if (!mRequest.isValid()) {
            Log.w(TAG, "[processIntent]request is inValid");
            setResult(RESULT_CANCELED);
            return false;
        }

        Log.d(TAG, "[processIntent]action code=" + mRequest.getActionCode());
        switch (mRequest.getActionCode()) {
            case ContactsRequest.ACTION_VIEW_CONTACT: {
                ImplicitIntentsUtil.startQuickContact(
                        this, mRequest.getContactUri(), ScreenType.UNKNOWN);
                return false;
            }
            case ContactsRequest.ACTION_INSERT_GROUP: {
                onCreateGroupMenuItemClicked();
                return true;
            }
            case ContactsRequest.ACTION_VIEW_GROUP:
            case ContactsRequest.ACTION_EDIT_GROUP: {
                mShouldSwitchToGroupView = true;
                return true;
            }
        }
        return true;
    }

    private void createViewsAndFragments() {
        Log.d(TAG,"[createViewsAndFragments]");
        PDebug.Start("createViewsAndFragments, prepare fragments");
        setContentView(R.layout.people_activity);

        final FragmentManager fragmentManager = getFragmentManager();

        setUpListFragment(fragmentManager);

        mMembersFragment = (GroupMembersFragment) fragmentManager.findFragmentByTag(TAG_GROUP_VIEW);

        // Configure floating action button
        mFloatingActionButtonContainer = findViewById(R.id.floating_action_button_container);
        final ImageButton floatingActionButton
                = (ImageButton) findViewById(R.id.floating_action_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountFilterUtil.startEditorIntent(PeopleActivity.this, getIntent(),
                        mContactListFilterController.getFilter());
            }
        });
        mFloatingActionButtonController = new FloatingActionButtonController(this,
                mFloatingActionButtonContainer, floatingActionButton);

        invalidateOptionsMenuIfNeeded();

        /*/ freeme.zhaozehong, 20180314. for freemeOS, UI redesign
        mLayoutRoot = (CoordinatorLayout) findViewById(R.id.root);
        /*/
        mLayoutRoot = findViewById(R.id.root);
        //*/

        if (mShouldSwitchToGroupView && !mIsRecreatedInstance) {
            mGroupUri = mRequest.getContactUri();
            switchToOrUpdateGroupView(GroupUtil.ACTION_SWITCH_GROUP);
            mShouldSwitchToGroupView = false;
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        final ViewGroup parent = (ViewGroup) findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    private void setUpListFragment(FragmentManager fragmentManager) {
        mContactsListFragment = (DefaultContactBrowseListFragment)
                fragmentManager.findFragmentByTag(TAG_ALL);

        if (mContactsListFragment == null) {
            mContactsListFragment = new DefaultContactBrowseListFragment();
            mContactsListFragment.setAnimateOnLoad(true);
            fragmentManager.beginTransaction()
                    .add(R.id.contacts_list_container, mContactsListFragment, TAG_ALL)
                    .commit();
            fragmentManager.executePendingTransactions();
        }

        mContactsListFragment.setContactsAvailable(areContactsAvailable());
        mContactsListFragment.setListType(mContactListFilterController.getFilterListType());
        mContactsListFragment.setParameters(/* ContactsRequest */ mRequest,
                /* fromOnNewIntent */ false);
    }

    /// M: register sim change @{
    @Override
    protected void onStart() {
        Log.i(TAG, "[onStart]mIsRecreatedInstance = " + mIsRecreatedInstance);
        AccountTypeManagerEx.registerReceiverOnSimStateAndInfoChanged(this, mBroadcastReceiver);
        super.onStart();
    }
    /// @}

    @Override
    protected void onPause() {
        Log.i(TAG, "[onPause]");
        mProviderStatusWatcher.stop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSaveServiceListener);

        ///M: [ALPS03424453][ALPS03788985] close select all menu @{
        closeMenusIfOpen(false, true);
        /// @}

        super.onPause();

        ContentResolver.removeStatusChangeListener(mStatusChangeListenerHandle);
        onSyncStateUpdated();
    }

    @Override
    public void onMultiWindowModeChanged(boolean entering) {
        initializeHomeVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.w(TAG, "[onResume]");
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            updateStatusBarBackground();
        }

        if (mShouldSwitchToAllContacts) {
            switchToAllContacts();
        }

        mProviderStatusWatcher.start();
        updateViewConfiguration(true);

        mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                        | ContentResolver.SYNC_OBSERVER_TYPE_PENDING
                        | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS,
                mSyncStatusObserver);
        onSyncStateUpdated();

        initializeFabVisibility();
        initializeHomeVisibility();

        mSaveServiceListener = new SaveServiceListener();
        LocalBroadcastManager.getInstance(this).registerReceiver(mSaveServiceListener,
                new IntentFilter(ContactSaveService.BROADCAST_GROUP_DELETED));

        Log.d(TAG, "[Performance test][Contacts] loading data end time: ["
                + System.currentTimeMillis() + "]");
        PDebug.End("Contacts.onResume");
    }

    /// M: unregister sim change @{
    @Override
    protected void onStop() {
        Log.w(TAG, "[onStop]");
        PDebug.Start("onStop");
        unregisterReceiver(mBroadcastReceiver);
        super.onStop();
        PDebug.End("onStop");
    }
    /// @

    public void updateStatusBarBackground() {
        updateStatusBarBackground(/* color */ -1);
    }

    public void updateStatusBarBackground(int color) {
        if (!CompatUtils.isLollipopCompatible()) return;
        if (color == -1) {
            mDrawerLayout.setStatusBarBackgroundColor(
                    MaterialColorMapUtils.getStatusBarColor(this));
        } else {
            mDrawerLayout.setStatusBarBackgroundColor(color);
        }
        mDrawerLayout.invalidate();
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDestroy() {
        Log.w(TAG, "[onDestroy]");
        PDebug.Start("onDestroy");
        mProviderStatusWatcher.removeListener(mProviderStatusListener);
        mContactListFilterController.removeListener(mFilterListener);
        /// M: [Sim Group] @{
        SimGroupProcessor.unregisterListener(this);
        /// @}
        super.onDestroy();
        PDebug.End("onDestroy");
    }

    private void initializeFabVisibility() {
        mFloatingActionButtonContainer.setVisibility(shouldHideFab() ? View.GONE : View.VISIBLE);
        mFloatingActionButtonController.resetIn();
        wasLastFabAnimationScaleIn = !shouldHideFab();
    }

    private void initializeHomeVisibility() {
        // Remove the navigation icon if we return to the fragment in a search or select state
        if (getToolbar() != null && (isListFragmentInSelectionMode()
                || isListFragmentInSearchMode() || isGroupsFragmentInSelectionMode()
                || isGroupsFragmentInSearchMode())) {
            getToolbar().setNavigationIcon(null);
        }
    }

    private boolean shouldHideFab() {
        if (mContactsListFragment != null && mContactsListFragment.getActionBarAdapter() == null
                || isInSecondLevel()) {
            return true;
        }
        return isListFragmentInSearchMode()
                || isListFragmentInSelectionMode();
    }

    public void showFabWithAnimation(boolean showFab) {
        if (mFloatingActionButtonContainer == null) {
            return;
        }
        if (showFab) {
            if (!wasLastFabAnimationScaleIn) {
                mFloatingActionButtonContainer.setVisibility(View.VISIBLE);
                mFloatingActionButtonController.scaleIn(0);
            }
            wasLastFabAnimationScaleIn = true;

        } else {
            if (wasLastFabAnimationScaleIn) {
                mFloatingActionButtonContainer.setVisibility(View.VISIBLE);
                mFloatingActionButtonController.scaleOut();
            }
            wasLastFabAnimationScaleIn = false;
        }
    }

    private void updateViewConfiguration(boolean forceUpdate) {
        Log.d(TAG, "[updateViewConfiguration]forceUpdate = " + forceUpdate);
        int providerStatus = mProviderStatusWatcher.getProviderStatus();
        if (!forceUpdate && (mProviderStatus != null)
                && (mProviderStatus.equals(providerStatus))) return;
        mProviderStatus = providerStatus;

        final FragmentManager fragmentManager= getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Change in CP2's provider status may not take effect immediately, see b/30566908.
        // So we need to handle the case where provider status is STATUS_EMPTY and there is
        // actually at least one real account (not "local" account) on device.
        if (shouldShowList()) {
            if (mContactsListFragment != null) {
                final Fragment unavailableFragment = fragmentManager
                        .findFragmentByTag(TAG_UNAVAILABLE);
                if (unavailableFragment != null) {
                    transaction.remove(unavailableFragment);
                }
                if (mContactsListFragment.isHidden()) {
                    transaction.show(mContactsListFragment);
                }
                mContactsListFragment.setContactsAvailable(areContactsAvailable());
                mContactsListFragment.setEnabled(true);
            }
        } else {
            // Setting up the page so that the user can still use the app
            // even without an account.
            if (mContactsListFragment != null) {
                mContactsListFragment.setEnabled(false);
            }
            final ContactsUnavailableFragment fragment = new ContactsUnavailableFragment();
            transaction.hide(mContactsListFragment);
            transaction.replace(R.id.contacts_unavailable_container, fragment, TAG_UNAVAILABLE);
            fragment.updateStatus(mProviderStatus);
        }
        ///M:[Google Issue][ALPS03416146]Add isSafeToCommitTransactions check for timging issue @{
        if (!transaction.isEmpty() && isSafeToCommitTransactions()) {
            transaction.commit();
            fragmentManager.executePendingTransactions();
        } else {
            Log.e(TAG, "[updateViewConfiguration]Igore commit. isEmpty:" + transaction.isEmpty() +
                    ", isSafe:" + isSafeToCommitTransactions());
        }
        /// @}

        invalidateOptionsMenuIfNeeded();
    }

    private boolean shouldShowList() {
        return mProviderStatus != null
                && ((mProviderStatus.equals(ProviderStatus.STATUS_EMPTY)
                && mAccountTypeManager.hasNonLocalAccount())
                || mProviderStatus.equals(ProviderStatus.STATUS_NORMAL)
                /// M:[CMCC RCS] @{
                || ExtensionManager.getInstance().getRcsExtension().isRcsServiceAvailable());
                /// @}
    }

    private void invalidateOptionsMenuIfNeeded() {
        if (mContactsListFragment != null
                && mContactsListFragment.getOptionsMenuContactsAvailable()
                != areContactsAvailable()) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // If the drawer is open, consume KEYCODE_BACK event only.
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // Should eventually go to onBackPressed().
                return super.onKeyDown(keyCode, event);
            }
            return false;
        }
        // Bring up the search UI if the user starts typing
        final int unicodeChar = event.getUnicodeChar();
        if ((unicodeChar != 0)
                // If COMBINING_ACCENT is set, it's not a unicode character.
                && ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) == 0)
                && !Character.isWhitespace(unicodeChar)) {
            if (mContactsListFragment.onKeyDown(unicodeChar)) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (!isSafeToCommitTransactions()) {
            return;
        }

        // Handle the back event in drawer first.
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer();
            return;
        }

        // Handle the back event in "second level".
        if (isGroupView()) {
            onBackPressedGroupView();
            return;
        }

        if (isAssistantView()) {
            onBackPressedAssistantView();
            return;
        }

        // If feature highlight is present, let it handle the back event before
        // mContactsListFragment.
        if (FeatureHighlightHelper.tryRemoveHighlight(this)) {
            return;
        }

        // Handle the back event in "first level" - mContactsListFragment.
        if (maybeHandleInListFragment()) {
            return;
        }

        super.onBackPressed();
    }

    private void onBackPressedGroupView() {
        ///M:[Google Issue][ALPS03440898]Fix timing issue @{
        if (mMembersFragment == null) {
            Log.d(TAG, "[onBackPressedGroupView] mMembersFragment is null !!");
            switchToAllContacts();
            return;
        }
        /// @}
        if (mMembersFragment.isEditMode()) {
            mMembersFragment.exitEditMode();
        } else if (mMembersFragment.getActionBarAdapter().isSelectionMode()) {
            mMembersFragment.getActionBarAdapter().setSelectionMode(false);
            mMembersFragment.displayCheckBoxes(false);
        } else if (mMembersFragment.getActionBarAdapter().isSearchMode()) {
            mMembersFragment.getActionBarAdapter().setSearchMode(false);
        } else {
            switchToAllContacts();
        }
    }

    private void onBackPressedAssistantView() {
        if (!isInThirdLevel()) {
            switchToAllContacts();
        } else {
            setDrawerLockMode(/* enabled */ true);
            super.onBackPressed();
        }
    }

    // Returns true if back event is handled in this method.
    private boolean maybeHandleInListFragment() {
        if (isListFragmentInSelectionMode()) {
            mContactsListFragment.getActionBarAdapter().setSelectionMode(false);
            return true;
        }

        if (isListFragmentInSearchMode()) {
            mContactsListFragment.getActionBarAdapter().setSearchMode(false);
            if (mContactsListFragment.wasSearchResultClicked()) {
                mContactsListFragment.resetSearchResultClicked();
            } else {
                Logger.logScreenView(this, ScreenType.SEARCH_EXIT);
                Logger.logSearchEvent(mContactsListFragment.createSearchState());
            }
            return true;
        }

        if (!AccountFilterUtil.isAllContactsFilter(mContactListFilterController.getFilter())
                && !mContactsListFragment.isHidden()) {
            // If mContactsListFragment is hidden, then mContactsUnavailableFragment is visible so we
            // don't need to switch to all contacts.
            switchToAllContacts();
            return true;
        }

        return false;
    }

    private boolean isListFragmentInSelectionMode() {
        return mContactsListFragment != null && mContactsListFragment.getActionBarAdapter() != null
                && mContactsListFragment.getActionBarAdapter().isSelectionMode();
    }

    private boolean isListFragmentInSearchMode() {
        return mContactsListFragment != null && mContactsListFragment.getActionBarAdapter() != null
                && mContactsListFragment.getActionBarAdapter().isSearchMode();
    }

    private boolean isGroupsFragmentInSelectionMode() {
        return mMembersFragment != null && mMembersFragment.getActionBarAdapter() != null
                && mMembersFragment.getActionBarAdapter().isSelectionMode();
    }

    private boolean isGroupsFragmentInSearchMode() {
        return mMembersFragment != null && mMembersFragment.getActionBarAdapter() != null
                && mMembersFragment.getActionBarAdapter().isSearchMode();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "[onSaveInstanceState]");
        super.onSaveInstanceState(outState);
        if (mNewGroupAccount != null) {
            outState.putString(KEY_NEW_GROUP_ACCOUNT, mNewGroupAccount.stringify());
        }
        outState.putInt(KEY_CONTACTS_VIEW, mCurrentView.ordinal());
        outState.putParcelable(KEY_GROUP_URI, mGroupUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mGroupUri = savedInstanceState.getParcelable(KEY_GROUP_URI);
    }

    private void onGroupDeleted(final Intent intent) {
        if (!ContactSaveService.canUndo(intent)) return;

        final AccessibilityManager am =
                (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        //TODO set to INDEFINITE and track user interaction to dismiss b/33208886
        final int accessibilityLength = 15000;
        final int length = am.isEnabled() ? accessibilityLength : Snackbar.LENGTH_LONG;
        final String message = getString(R.string.groupDeletedToast);

        final Snackbar snackbar = Snackbar.make(mLayoutRoot, message, length)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /// M: [Sim Group] @{
                        int subId = intent.getIntExtra(SimGroupUtils.EXTRA_SUB_ID,
                                SubInfoUtils.getInvalidSubId());
                        if (subId > 0) {
                            ContactSaveService.startService(PeopleActivity.this,
                                    SimGroupUtils.createUndoIntentForIcc(
                                            PeopleActivity.this, intent, subId));
                        } else { /// @}
                            ContactSaveService.startService(PeopleActivity.this,
                                    ContactSaveService.createUndoIntent(
                                            PeopleActivity.this, intent));
                        }
                    }
                }).setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_action_text));

        // Announce for a11y talkback
        mLayoutRoot.announceForAccessibility(message);
        mLayoutRoot.announceForAccessibility(getString(R.string.undo));

        snackbar.show();
    }

    private class SaveServiceListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ContactSaveService.BROADCAST_GROUP_DELETED:
                    onGroupDeleted(intent);
                    break;
            }
        }
    }

    private void onGroupMenuItemClicked(long groupId) {
        if (isGroupView() && mMembersFragment != null
                && mMembersFragment.isCurrentGroup(groupId)) {
            return;
        }
        mGroupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId);
        switchToOrUpdateGroupView(GroupUtil.ACTION_SWITCH_GROUP);
    }

    private void onFilterMenuItemClicked(Intent intent) {
        // We must pop second level first to "restart" mContactsListFragment before changing filter.
        if (isInSecondLevel()) {
            popSecondLevel();
            showFabWithAnimation(/* showFab */ true);
            // HACK: swap the current filter to force listeners to update because the group
            // member view no longer changes the filter. Fix for b/32223767
            final ContactListFilter current = mContactListFilterController.getFilter();
            Log.d(TAG, "[onFilterMenuItemClicked] current=" + current);
            mContactListFilterController.setContactListFilter(
                    AccountFilterUtil.createContactsFilter(this), false);
            mContactListFilterController.setContactListFilter(current, false);
        }
        mCurrentView = ContactsView.ACCOUNT_VIEW;
        AccountFilterUtil.handleAccountFilterResult(mContactListFilterController,
                AppCompatActivity.RESULT_OK, intent);
    }

    private void switchToOrUpdateGroupView(String action) {
        // If group fragment is active and visible, we simply update it.
        if (mMembersFragment != null && !mMembersFragment.isInactive()) {
            mMembersFragment.updateExistingGroupFragment(mGroupUri, action);
        } else {
            switchView(ContactsView.GROUP_VIEW);
        }
    }

    protected void launchAssistant() {
        switchView(ContactsView.ASSISTANT);
    }

    private void switchView(ContactsView contactsView) {
        mCurrentView = contactsView;

        ///M:[Google issue][ALPS03492080] ignore switchView if transaction not safe. @{
        if (!isSafeToCommitTransactions()) {
            Log.e(TAG, "[switchView]Ignore switchView. isSafe:" + isSafeToCommitTransactions());
            return;
        }
        /// @}
        final FragmentManager fragmentManager =  getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        popSecondLevel();
        if (isGroupView()) {
            mMembersFragment = GroupMembersFragment.newInstance(mGroupUri);
            transaction.replace(
                    R.id.contacts_list_container, mMembersFragment, TAG_GROUP_VIEW);
        } else if (isAssistantView()) {
            Fragment uiFragment = fragmentManager.findFragmentByTag(TAG_ASSISTANT);
            Fragment unavailableFragment = fragmentManager.findFragmentByTag(TAG_UNAVAILABLE);
            if (uiFragment == null) {
                uiFragment = ObjectFactory.getAssistantFragment();
            }
            if (unavailableFragment != null) {
                transaction.remove(unavailableFragment);
            }
            transaction.replace(R.id.contacts_list_container, uiFragment, TAG_ASSISTANT);
            resetToolBarStatusBarColor();
        }
        transaction.addToBackStack(TAG_SECOND_LEVEL);
        transaction.commit();
        fragmentManager.executePendingTransactions();

        showFabWithAnimation(/* showFab */ false);
    }

    public void switchToAllContacts() {
        popSecondLevel();
        mShouldSwitchToAllContacts = false;
        mCurrentView = ContactsView.ALL_CONTACTS;
        mDrawerFragment.setNavigationItemChecked(ContactsView.ALL_CONTACTS);
        showFabWithAnimation(/* showFab */ true);
        mContactsListFragment.scrollToTop();
        resetFilter();
        setTitle(getString(R.string.contactsList));
    }

    private void resetFilter() {
        final Intent intent = new Intent();
        final ContactListFilter filter = AccountFilterUtil.createContactsFilter(this);
        intent.putExtra(AccountFilterActivity.EXTRA_CONTACT_LIST_FILTER, filter);
        AccountFilterUtil.handleAccountFilterResult(
                mContactListFilterController, AppCompatActivity.RESULT_OK, intent);
    }

    // Reset toolbar and status bar color to Contacts theme color.
    private void resetToolBarStatusBarColor() {
        findViewById(R.id.toolbar_frame).setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary_color));
        updateStatusBarBackground(ContextCompat.getColor(this, R.color.primary_color_dark));
    }

    protected DefaultContactBrowseListFragment getListFragment() {
        return mContactsListFragment;
    }

    protected GroupMembersFragment getGroupFragment() {
        return mMembersFragment;
    }

    private void handleFilterChangeForFragment(ContactListFilter filter) {
        if (mContactsListFragment.canSetActionBar()) {
            mContactsListFragment.setFilterAndUpdateTitle(filter);
            // Scroll to top after filter is changed.
            mContactsListFragment.scrollToTop();
        }
    }

    private void handleFilterChangeForActivity(ContactListFilter filter) {
        // The filter was changed while this activity was in the background. If we're in the
        // assistant view Switch to the main contacts list when we resume to prevent
        // b/31838582 and b/31829161
        // TODO: this is a hack; we need to do some cleanup of the contact list filter stuff
        /** M:[Google Issue][ALPS03489682]After set customer filter need reset DrawerFragment.
         * ori code:
         * if (isAssistantView() && filter.isContactsFilterType()) {
         * new code: @{ */
        if ((isAssistantView()||isAccountView()) && filter.isContactsFilterType()) {
            Log.d(TAG, "[handleFilterChangeForActivity]set mShouldSwitchToAllContacts");
        /** @} */
            mShouldSwitchToAllContacts = true;
        }

        if (CompatUtils.isNCompatible()) {
            getWindow().getDecorView()
                    .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        invalidateOptionsMenu();
    }

    public void updateDrawerGroupMenu(long groupId) {
        if (mDrawerFragment != null) {
            mDrawerFragment.updateGroupMenu(groupId);
        }
    }

    public void setDrawerLockMode(boolean enabled) {
        // Prevent drawer from being opened by sliding from the start of screen.
        mDrawerLayout.setDrawerLockMode(enabled ? DrawerLayout.LOCK_MODE_UNLOCKED
                : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        // Order of these statements matter.
        // Display back button and disable drawer indicator.
        /*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
        if (enabled) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            mToggle.setDrawerIndicatorEnabled(true);
        } else {
            mToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        /*/
        if (enabled) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            mToggle.setDrawerIndicatorEnabled(true);
        } else {
            mToggle.setDrawerIndicatorEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        //*/
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
        /// M: [Google issue][ALPS03750545] mToggle auto display after onConfigurationChanged,
        /// so need reset nativagation icon after onConfigurationChanged. @{
        initializeHomeVisibility();
        /// @}
    }

    protected void onCreateGroupMenuItemClicked() {
        // Select the account to create the group
        final Bundle extras = getIntent().getExtras();
        final Account account = extras == null ? null :
                (Account) extras.getParcelable(Intents.Insert.EXTRA_ACCOUNT);
        if (account == null) {
            selectAccountForNewGroup();
        } else {
            final String dataSet = extras == null
                    ? null : extras.getString(Intents.Insert.EXTRA_DATA_SET);
            final AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
                    account.name, account.type, dataSet);
            onAccountChosen(accountWithDataSet, /* extraArgs */ null);
        }
    }

    private void selectAccountForNewGroup() {
        // This should never block because the DrawerFragment loads the accounts and the
        // "Create Label" item only exists when that loading finishes
        final List<AccountInfo> accounts = Futures.getUnchecked(AccountTypeManager.getInstance(this)
                .filterAccountsAsync(AccountTypeManager.AccountFilter.GROUPS_WRITABLE));
        if (accounts.isEmpty()) {
            // We shouldn't present the add group button if there are no writable accounts
            // but check it since it's possible we are started with an Intent.
            Toast.makeText(this, R.string.groupCreateFailedToast, Toast.LENGTH_SHORT).show();
            return;
        }
        // If there is a single writable account, use it w/o showing a dialog.
        if (accounts.size() == 1) {
            onAccountChosen(accounts.get(0).getAccount(), /* extraArgs */ null);
            return;
        }
        SelectAccountDialogFragment.show(getFragmentManager(), R.string.dialog_new_group_account,
                AccountTypeManager.AccountFilter.GROUPS_WRITABLE, /* extraArgs */ null,
                TAG_SELECT_ACCOUNT_DIALOG);
    }

    // Implementation of SelectAccountDialogFragment.Listener
    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        mNewGroupAccount = account;
        GroupNameEditDialogFragment.newInstanceForCreation(
                mNewGroupAccount, GroupUtil.ACTION_CREATE_GROUP)
                .show(getFragmentManager(), TAG_GROUP_NAME_EDIT_DIALOG);
    }

    @Override
    public void onAccountSelectorCancelled() {
    }

    // Implementation of DrawerFragmentListener
    @Override
    public void onDrawerItemClicked(){
        closeDrawer();
    }

    @Override
    public void onContactsViewSelected(ContactsView mode) {
        if (mode == ContactsView.ALL_CONTACTS) {
            switchToAllContacts();
        } else if (mode == ContactsView.ASSISTANT) {
            launchAssistant();
        } else {
            throw new IllegalStateException("Unknown view " + mode);
        }
    }

    @Override
    public void onCreateLabelButtonClicked() {
        if (isSafeToCommitTransactions()) {
            onCreateGroupMenuItemClicked();
        }
    }

    @Override
    public void onOpenSettings() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(createPreferenceIntent());
            }
        }, DRAWER_CLOSE_DELAY);
    }

    @Override
    public void onLaunchHelpFeedback() {
        HelpUtils.launchHelpAndFeedbackForMainScreen(this);
    }

    ///M: [ConfCall] @{
    @Override
    public void onConferenceCallClicked() {
        ActivitiesUtils.conferenceCall(this);
    }
    /// @}

    @Override
    public void onGroupViewSelected(GroupListItem groupListItem) {
        onGroupMenuItemClicked(groupListItem.getGroupId());
    }

    @Override
    public void onAccountViewSelected(ContactListFilter filter) {
        Log.d(TAG, "[onAccountViewSelected] filter=" + filter + ", accountType=" +
                    filter.accountType + ", accountName=" + filter.accountName);
        final Intent intent = new Intent();
        intent.putExtra(AccountFilterActivity.EXTRA_CONTACT_LIST_FILTER, filter);
        onFilterMenuItemClicked(intent);
    }

    public boolean isGroupView() {
        return mCurrentView == ContactsView.GROUP_VIEW;
    }

    protected boolean isAssistantView() {
        return mCurrentView == ContactsView.ASSISTANT;
    }

    protected boolean isAllContactsView() {
        return mCurrentView == ContactsView.ALL_CONTACTS;
    }

    protected boolean isAccountView() {
        return mCurrentView == ContactsView.ACCOUNT_VIEW;
    }

    public boolean isInSecondLevel() {
        return isGroupView() || isAssistantView();
    }

    private boolean isInThirdLevel() {
        return isLastBackStackTag(TAG_THIRD_LEVEL);
    }

    private boolean isLastBackStackTag(String tag) {
        final int count = getFragmentManager().getBackStackEntryCount();
        if (count > 0) {
            final FragmentManager.BackStackEntry last =
                    getFragmentManager().getBackStackEntryAt(count - 1);
            if (tag == null) {
                return last.getName() == null;
            }
            return tag.equals(last.getName());
        }
        return false;
    }

    private void popSecondLevel() {
        ///M:[Google Issue][ALPS03441494]Not allow popBackStackImmediate after onSaveInstance @{
        if (!isSafeToCommitTransactions()) {
            Log.e(TAG, "[popSecondLevel] ignore popSecondLevel. isSafe: false");
            return;
        }
        /// @}
        getFragmentManager().popBackStackImmediate(
                TAG_ASSISTANT_HELPER, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getFragmentManager().popBackStackImmediate(
                TAG_SECOND_LEVEL, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mMembersFragment = null;
        resetToolBarStatusBarColor();
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private Intent createPreferenceIntent() {
        final Intent intent = new Intent(this, ContactsPreferenceActivity.class);
        intent.putExtra(ContactsPreferenceActivity.EXTRA_NEW_LOCAL_PROFILE,
                ContactEditorFragment.INTENT_EXTRA_NEW_LOCAL_PROFILE);
        return intent;
    }

    /// M: [Sim Group] @{
    @Override
    public void onSimGroupCompleted(Intent callbackIntent) {
        Log.d(TAG, "[onSIMGroupCompleted]callbackIntent = " + callbackIntent);
        onNewIntent(callbackIntent);
    }
    /// @}

    /// M: [ALPS03323718]new behavior, leave selection mode when sim state change @{
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "[onReceive] Received Intent:" + intent);
            //close all option menus
            closeMenusIfOpen(true, true);
            updateViewConfiguration(true);
            ///M:[ALPS03468759]Dismiss fragment when plugout sim@{
            int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, -1);
            GroupNameEditDialogFragment fragment =
                    (GroupNameEditDialogFragment)getFragmentManager().
                        findFragmentByTag(TAG_GROUP_NAME_EDIT_DIALOG);
            if (fragment != null) {
                int groupSubId = fragment.getSubId();
                Log.i(TAG, "[onReceive] subId=" + subId + ", groupSubId=" + groupSubId);
                if (groupSubId >= 0 && groupSubId == subId) {
                    try {
                        fragment.dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing progress dialog", e);
                    }
                }
            }
            /// @}
            /// M:[ALPS03616690]Dismiss deletion fragment when plugout sim@{
            GroupDeletionDialogFragment deletionFragment =
                    (GroupDeletionDialogFragment)getFragmentManager().
                        findFragmentByTag("deleteGroup");
            if (deletionFragment != null) {
                if (subId >=0 && subId == deletionFragment.getSubId()) {
                    try {
                        deletionFragment.dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing deletion dialog", e);
                    }
                }
            }
            /// @}
        }
    };

    private void closeMenusIfOpen(boolean optionMenu, boolean selectMenu) {
        final MultiSelectContactsListFragment listFragment;
        if (isAllContactsView() || isAccountView()) {
            listFragment = getListFragment();
        } else if (isGroupView()) {
            listFragment = getGroupFragment();
        } else {
            listFragment = null;
        }
        if (listFragment != null) {
            listFragment.closeMenusIfOpen(optionMenu, selectMenu);
        }
    }
    /// @}
}
