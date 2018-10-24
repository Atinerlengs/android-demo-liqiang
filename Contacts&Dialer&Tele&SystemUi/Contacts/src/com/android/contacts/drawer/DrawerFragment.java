/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.contacts.drawer;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.CarrierConfigManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.android.contacts.GroupListLoader;
import com.android.contacts.R;
import com.android.contacts.activities.PeopleActivity.ContactsView;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.model.account.AccountsLoader.AccountsListener;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contactsbind.ObjectFactory;

import com.mediatek.contacts.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DrawerFragment extends Fragment implements AccountsListener {

    private static final String TAG = "DrawerFragment";

    private static final int LOADER_GROUPS = 1;
    private static final int LOADER_ACCOUNTS = 2;
    private static final int LOADER_FILTERS = 3;

    private static final String KEY_CONTACTS_VIEW = "contactsView";
    private static final String KEY_SELECTED_GROUP = "selectedGroup";
    private static final String KEY_SELECTED_ACCOUNT = "selectedAccount";

    private WelcomeContentObserver mObserver;
    private ListView mDrawerListView;
    private DrawerAdapter mDrawerAdapter;
    private ContactsView mCurrentContactsView;
    private DrawerFragmentListener mListener;
    // Transparent scrim drawn at the top of the drawer fragment.
    private ScrimDrawable mScrimDrawable;

    private List<GroupListItem> mGroupListItems = new ArrayList<>();
    private boolean mGroupsLoaded;
    private boolean mAccountsLoaded;
    private boolean mHasGroupWritableAccounts;

    private final class WelcomeContentObserver extends ContentObserver {
        private WelcomeContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            mDrawerAdapter.notifyDataSetChanged();
        }
    }

    ///M:[ConfCall]CarrierConfigManager broadcast receiver @{
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                mDrawerAdapter.notifyCarrierConfChanged();
            }
        }
    };

    //[ALPS03460511] Update Conf call state for PeopleActivity
    public void updateConfCallState() {
        if (mDrawerAdapter != null) {
            mDrawerAdapter.notifyCarrierConfChanged();
        }
    }
    /// @}

    /// M: check if account/group already not exist but still selected (ex. flightmode),
    /// Change current view to ALL_CONTACTS @{
    private void checkSelectedAccountViewValidate(List<ContactListFilter> data) {
        for (ContactListFilter filter : data) {
            Log.d(TAG, "[mFiltersLoaderListener]checkSelectedAccountViewValidate:"
                    + filter.accountName);
        }
        boolean isExist = false;
        if (mDrawerAdapter.getSelectedAccount() != null &&
            mCurrentContactsView == ContactsView.ACCOUNT_VIEW) {
            for (ContactListFilter filter : data) {
                if (mDrawerAdapter.getSelectedAccount().equals(filter)) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist && (mListener != null) &&
                getActivity() != null &&
                (!getActivity().isFinishing())) {
                Log.d(TAG, "[mFiltersLoaderListener]convert to ALL_CONTACTS due to " +
                    mDrawerAdapter.getSelectedAccount().accountName + " already not exist");
                switchToAllContacts();
            }
        }
    }

    private void checkSelectedGroupViewValidate(List<GroupListItem> data) {
        for (GroupListItem item : data) {
            Log.d(TAG, "[mFiltersLoaderListener]checkSelectedGroupViewValidate:"
                    + item.getGroupId());
        }
        boolean isExist = false;
        if (mDrawerAdapter.getSelectedGroupId() >= 0 &&
            mCurrentContactsView == ContactsView.GROUP_VIEW) {
            for (GroupListItem item : data) {
                if (mDrawerAdapter.getSelectedGroupId() == item.getGroupId()) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist && (mListener != null) &&
                getActivity() != null &&
                (!getActivity().isFinishing())) {
                Log.d(TAG, "[mFiltersLoaderListener]convert to ALL_CONTACTS due to " +
                        mDrawerAdapter.getSelectedGroupId() + " already not exist");
                switchToAllContacts();
            }
        }
    }

    private void switchToAllContacts() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (!getActivity().isFinishing()) {
                    mListener.onContactsViewSelected(ContactsView.ALL_CONTACTS);
                    setNavigationItemChecked(ContactsView.ALL_CONTACTS);
                }
            }
        });
    }
    /// @}

    private final LoaderManager.LoaderCallbacks<List<ContactListFilter>> mFiltersLoaderListener =
            new LoaderManager.LoaderCallbacks<List<ContactListFilter>> () {
                @Override
                public Loader<List<ContactListFilter>> onCreateLoader(int id, Bundle args) {
                    return new AccountFilterUtil.FilterLoader(getActivity());
                }

                @Override
                public void onLoadFinished(
                        Loader<List<ContactListFilter>> loader, List<ContactListFilter> data) {
                    if (data != null) {
                        if (data == null || data.size() < 2) {
                            mDrawerAdapter.setAccounts(new ArrayList<ContactListFilter>());
                        } else {
                            mDrawerAdapter.setAccounts(data);
                        }
                    }
                    /// M: check if selected account is already not exist(ex.flightmode),
                    /// Change current view to ALL_CONTACTS @{
                    checkSelectedAccountViewValidate(data);
                    /// @}
                }

                public void onLoaderReset(Loader<List<ContactListFilter>> loader) {
                }
            };

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupListLoaderListener =
            new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public CursorLoader onCreateLoader(int id, Bundle args) {
                    return new GroupListLoader(getActivity());
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    if (data == null) {
                        return;
                    }
                    mGroupListItems.clear();
                    ///M:[Google Issue][ALPS03432998]Must move cursur to -1 before moveToNext()@{
                    data.moveToPosition(-1);
                    /// @{
                    for (int i = 0; i < data.getCount(); i++) {
                        if (data.moveToNext()) {
                            mGroupListItems.add(GroupUtil.getGroupListItem(data, i));
                        }
                    }
                    mGroupsLoaded = true;
                    notifyIfReady();
                    /// M: check if selected account is already not exist(ex.flightmode),
                    /// Change current view to ALL_CONTACTS @{
                    checkSelectedGroupViewValidate(mGroupListItems);
                    /// @}
                }

                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    public DrawerFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DrawerFragmentListener) {
            mListener = (DrawerFragmentListener) activity;
        } else {
            throw new IllegalArgumentException(
                    "Activity must implement " + DrawerFragmentListener.class.getName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View contentView = inflater.inflate(R.layout.drawer_fragment, null);
        mDrawerListView = (ListView) contentView.findViewById(R.id.list);
        mDrawerAdapter = new DrawerAdapter(getActivity());
        mDrawerAdapter.setSelectedContactsView(mCurrentContactsView);
        loadGroupsAndFilters();
        mDrawerListView.setAdapter(mDrawerAdapter);
        mDrawerListView.setOnItemClickListener(mOnDrawerItemClickListener);

        if (savedInstanceState != null) {
            final ContactsView contactsView =
                    ContactsView.values()[savedInstanceState.getInt(KEY_CONTACTS_VIEW)];
            setNavigationItemChecked(contactsView);
            final long groupId = savedInstanceState.getLong(KEY_SELECTED_GROUP);
            mDrawerAdapter.setSelectedGroupId(groupId);
            final ContactListFilter filter = savedInstanceState.getParcelable(KEY_SELECTED_ACCOUNT);
            mDrawerAdapter.setSelectedAccount(filter);
            /// M:[Google Issue][ALPS03448024] Contacts list will restore to all-contacts.
            /// So need call onAccountViewSelected again to update listFragment. @{
            if (filter != null && mListener != null && ContactsView.ACCOUNT_VIEW == contactsView) {
                Log.d(TAG, "[onCreateView]onAccountViewSelected filter=" + filter);
                mListener.onAccountViewSelected(filter);
            }
            /// @}
        } else {
            setNavigationItemChecked(ContactsView.ALL_CONTACTS);
        }

        final FrameLayout root = (FrameLayout) contentView.findViewById(R.id.drawer_fragment_root);
        root.setFitsSystemWindows(true);
        root.setOnApplyWindowInsetsListener(new WindowInsetsListener());
        root.setForegroundGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL);

        mScrimDrawable = new ScrimDrawable();
        root.setForeground(mScrimDrawable);

        ///M:[ConfCall] register for carrier config change @{
        IntentFilter filter = new IntentFilter();
        filter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        getContext().registerReceiver(mReceiver, filter);
        /// @}

        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // todo double check on the new Handler() thing
        final Uri uri = ObjectFactory.getWelcomeUri();
        if (uri != null) {
            mObserver = new WelcomeContentObserver(new Handler());
            getActivity().getContentResolver().registerContentObserver(uri, false, mObserver);
        }
        ///M:[ALPS03441067] update Conf call state every time resume @{
        mDrawerAdapter.notifyCarrierConfChanged();
        /// @}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CONTACTS_VIEW, mCurrentContactsView.ordinal());
        outState.putLong(KEY_SELECTED_GROUP, mDrawerAdapter.getSelectedGroupId());
        outState.putParcelable(KEY_SELECTED_ACCOUNT, mDrawerAdapter.getSelectedAccount());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    private void loadGroupsAndFilters() {
        Log.d(TAG, "[loadGroupsAndFilters]");
        getLoaderManager().initLoader(LOADER_FILTERS, null, mFiltersLoaderListener);
        AccountsLoader.loadAccounts(this, LOADER_ACCOUNTS,
                AccountTypeManager.AccountFilter.GROUPS_WRITABLE);
        getLoaderManager().initLoader(LOADER_GROUPS, null, mGroupListLoaderListener);
    }

    ///M:[ConfCall] Unregister for carrier config change @{
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getContext() != null) {
            getContext().unregisterReceiver(mReceiver);
        }
    }
    /// @}

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private final OnItemClickListener mOnDrawerItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            if (mListener == null) {
                return;
            }
            final int viewId = v.getId();
            if (viewId == R.id.nav_all_contacts) {
                mListener.onContactsViewSelected(ContactsView.ALL_CONTACTS);
                setNavigationItemChecked(ContactsView.ALL_CONTACTS);
            } else if (viewId == R.id.nav_assistant) {
                mListener.onContactsViewSelected(ContactsView.ASSISTANT);
                setNavigationItemChecked(ContactsView.ASSISTANT);
            } else if (viewId == R.id.nav_group) {
                final GroupListItem groupListItem = (GroupListItem) v.getTag();
                mListener.onGroupViewSelected(groupListItem);
                mDrawerAdapter.setSelectedGroupId(groupListItem.getGroupId());
                setNavigationItemChecked(ContactsView.GROUP_VIEW);
            } else if (viewId == R.id.nav_filter) {
                final ContactListFilter filter = (ContactListFilter) v.getTag();
                mListener.onAccountViewSelected(filter);
                mDrawerAdapter.setSelectedAccount(filter);
                setNavigationItemChecked(ContactsView.ACCOUNT_VIEW);
            } else if (viewId == R.id.nav_create_label) {
                mListener.onCreateLabelButtonClicked();
            } else if (viewId == R.id.nav_settings) {
                mListener.onOpenSettings();
            } else if (viewId == R.id.nav_help) {
                mListener.onLaunchHelpFeedback();
            /// M:[ConfCall] handle conference call @{
            } else if (viewId == R.id.nav_conf_call) {
                mListener.onConferenceCallClicked();
            /// @}
            } else {
                Log.e(TAG, "Unknown view:" + viewId);
                return;
            }
            mListener.onDrawerItemClicked();
        }
    };

    public void setNavigationItemChecked(ContactsView contactsView) {
        mCurrentContactsView = contactsView;
        if (mDrawerAdapter != null) {
            mDrawerAdapter.setSelectedContactsView(contactsView);
        }
    }

    public void updateGroupMenu(long groupId) {
        mDrawerAdapter.setSelectedGroupId(groupId);
        setNavigationItemChecked(ContactsView.GROUP_VIEW);
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> accounts) {
        mHasGroupWritableAccounts = !accounts.isEmpty();
        mAccountsLoaded = true;
        notifyIfReady();
    }

    private void notifyIfReady() {
        if (mAccountsLoaded && mGroupsLoaded) {
            final Iterator<GroupListItem> iterator = mGroupListItems.iterator();
            while (iterator.hasNext()) {
                final GroupListItem groupListItem = iterator.next();
                if (GroupUtil.isEmptyFFCGroup(groupListItem)) {
                    iterator.remove();
                }
            }
            mDrawerAdapter.setGroups(mGroupListItems, mHasGroupWritableAccounts);
        }
    }

    public interface DrawerFragmentListener {
        void onDrawerItemClicked();
        void onContactsViewSelected(ContactsView mode);
        void onGroupViewSelected(GroupListItem groupListItem);
        void onAccountViewSelected(ContactListFilter filter);
        void onCreateLabelButtonClicked();
        void onOpenSettings();
        void onLaunchHelpFeedback();
        ///M:[ConfCall] @{
        void onConferenceCallClicked();
        /// @}
    }

    private class WindowInsetsListener implements View.OnApplyWindowInsetsListener {
        @Override
        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
            final int insetTop = insets.getSystemWindowInsetTop();
            // set height of the scrim
            mScrimDrawable.setIntrinsicHeight(insetTop);
            return insets;
        }
    }
}
