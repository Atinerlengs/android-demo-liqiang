/*
* Copyright (C) 2017 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.Contacts;
import android.support.v4.content.ContextCompat;
/*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
import android.support.v7.widget.Toolbar;
//*/
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.os.BuildCompat;
import android.text.TextUtils;
//import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.DynamicShortcuts;
import com.android.contacts.R;
import com.android.contacts.ShortcutIntentBuilder;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.GroupMemberPickerFragment;
import com.android.contacts.list.JoinContactListFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.MultiSelectContactsListFragment.OnCheckBoxListActionListener;
import com.android.contacts.list.MultiSelectEmailAddressesListFragment;
import com.android.contacts.list.MultiSelectPhoneNumbersListFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.logging.ListEvent;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.ViewUtil;

import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.util.ContactsSettingsUtils;
import com.mediatek.contacts.util.Log;

import java.util.ArrayList;

//*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.app.ActionBar;
//*/

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class ContactSelectionActivity extends AppCompatContactsActivity implements
        View.OnCreateContextMenuListener, ActionBarAdapter.Listener, OnClickListener,
        OnFocusChangeListener, OnCheckBoxListActionListener {
    private static final String TAG = "ContactSelection";

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final String KEY_SEARCH_MODE = "searchMode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;
    private boolean mIsSearchMode;
    private boolean mIsSearchSupported;

    private ContactsRequest mRequest;

    /*/ freeme.liqiang, 20180307. for FreemeOS single selected
    private ActionBarAdapter mActionBarAdapter;
    private Toolbar mToolbar;
    /*/
    private EditText etFreemeSearchView;
    //*/

    public ContactSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
            setupActionListener();
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        Log.i(TAG, "[onCreate]");
        RequestPermissionsActivity.startPermissionActivityIfNeeded(this);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
            mIsSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
        }

        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            Log.w(TAG, "[onCreate] mRequest is Invalid,finish activity...mRequest:"
                    + mRequest);
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        //*/ freeme.liqiang, 20180704. add multi-selected
        mShowSelectMode = getIntent().getBooleanExtra(SHOW_SELECT_MODE, false);
        //*/
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        setContentView(R.layout.contact_picker);
        /*/
        setContentView(R.layout.freeme_contact_picker);

        etFreemeSearchView = findViewById(R.id.freeme_search_edit);
        etFreemeSearchView.setHint(getString(R.string.freeme_hint_findContacts, 0));
        if (etFreemeSearchView != null) {
            etFreemeSearchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                    mListFragment.setQueryString(arg0.toString(), false);

                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                              int arg3) {
                }

                @Override
                public void afterTextChanged(Editable arg0) {
                }
            });
        }
        //*/

        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
        }

        prepareSearchViewAndActionBar(savedState);
        configureActivityTitle();
    }

    ///M: [ALPS03424453] close select all menu @{
    @Override
    protected void onDestroy() {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        Log.i(TAG, "[onDestroy] mListFragment = " + mListFragment +
                ", mActionBarAdapter=" + mActionBarAdapter);
        if (getMultiSelectListFragment() != null && mActionBarAdapter != null) {
            mActionBarAdapter.closeSelectMenu();
        }
        /*/
        super.onDestroy();
    }
    /// @}

    public boolean isSelectionMode() {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        return mActionBarAdapter.isSelectionMode();
        /*/
        return mShowSelectMode;
        //*/
    }

    public boolean isSearchMode() {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        return mActionBarAdapter.isSearchMode();
        /*/
        return mIsSearchMode;
        //*/
    }

    private void prepareSearchViewAndActionBar(Bundle savedState) {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        mToolbar = getView(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Add a shadow under the toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        mActionBarAdapter = new ActionBarAdapter(this, this, getSupportActionBar(), mToolbar,
                R.string.enter_contact_name, mListFragment);
        mActionBarAdapter.setShowHomeIcon(true);
        mActionBarAdapter.setShowHomeAsUp(true);
        mActionBarAdapter.initialize(savedState, mRequest);
        /*/
        ActionBar actionBar = getActionBar();
        if (actionBar !=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //*/

        // Postal address pickers (and legacy pickers) don't support search, so just show
        // "HomeAsUp" button and title.
        mIsSearchSupported = mRequest.getActionCode() != ContactsRequest.ACTION_PICK_POSTAL
                && mRequest.getActionCode() != ContactsRequest.ACTION_PICK_EMAILS
                && mRequest.getActionCode() != ContactsRequest.ACTION_PICK_PHONES
                && !mRequest.isLegacyCompatibilityMode();
        configureSearchMode();
    }

    private void configureSearchMode() {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        mActionBarAdapter.setSearchMode(mIsSearchMode);
        /*/
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {// Go back to previous screen, intending "cancel"
            setResult(RESULT_CANCELED);
            /// M: [Google Issue] ALPS02013610. Need add isResumed() judgement. @{
            if (isResumed()) {
                onBackPressed();
            }
            /// @}
        } else if (id == R.id.menu_search) {
            mIsSearchMode = !mIsSearchMode;
            configureSearchMode();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
        outState.putBoolean(KEY_SEARCH_MODE, mIsSearchMode);
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        if (mActionBarAdapter != null) {
            mActionBarAdapter.onSaveInstanceState(outState);
        }
        /*/
    }

    private void configureActivityTitle() {
        if (!TextUtils.isEmpty(mRequest.getActivityTitle())) {
            /*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
            getSupportActionBar().setTitle(mRequest.getActivityTitle());
            /*/
            getActionBar().setTitle(mRequest.getActivityTitle());
            //*/
            return;
        }
        int titleResId = -1;
        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                titleResId = R.string.contactInsertOrEditActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_CONTACT: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                titleResId = R.string.shortcutActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_PHONE: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_EMAIL: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_PHONES: {
                titleResId = R.string.pickerSelectContactsActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_EMAILS: {
                titleResId = R.string.pickerSelectContactsActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                titleResId = R.string.shortcutActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                titleResId = R.string.shortcutActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_POSTAL: {
                titleResId = R.string.contactPickerActivityTitle;
                break;
            }
            case ContactsRequest.ACTION_PICK_JOIN: {
                titleResId = R.string.titleJoinContactDataWith;
                break;
            }
            case ContactsRequest.ACTION_PICK_GROUP_MEMBERS: {
                titleResId = R.string.groupMemberPickerActivityTitle;
                break;
            }
        }
        if (titleResId > 0) {
            /*/ freeme.zhaozehong, 20180307. for freemeOS, UI redesign
            getSupportActionBar().setTitle(titleResId);
            /*/
            getActionBar().setTitle(titleResId);
            //*/
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        Log.d(TAG, "[configureListFragment] mActionCode=" + mActionCode);
        switch (mActionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setEditMode(true);
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                fragment.setCreateContactEnabled(!mRequest.isSearchMode());
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_DEFAULT:
            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setIncludeFavorites(mRequest.shouldIncludeFavorites());
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setCreateContactEnabled(!mRequest.isSearchMode());
                fragment.setListType(ListEvent.ListType.PICK_CONTACT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setShortcutRequested(true);
                fragment.setListType(ListEvent.ListType.PICK_CONTACT_FOR_SHORTCUT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setListType(ListEvent.ListType.PICK_PHONE);
                /* M: ALPS02316838 CallableUri's default value is false
                 * If it set to true, query uri will be Callable.CONTENT_URI. @{ */
                boolean isCallableUri = getIntent().getBooleanExtra("isCallableUri", false);
                fragment.setUseCallableUri(isCallableUri);
                /* @} */
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                mListFragment = new EmailAddressPickerFragment();
                mListFragment.setListType(ListEvent.ListType.PICK_EMAIL);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONES: {
                mListFragment = new MultiSelectPhoneNumbersListFragment();
                mListFragment.setArguments(getIntent().getExtras());
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAILS: {
                mListFragment = new MultiSelectEmailAddressesListFragment();
                mListFragment.setArguments(getIntent().getExtras());
                break;
            }
            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_CALL);
                fragment.setListType(ListEvent.ListType.PICK_CONTACT_FOR_SHORTCUT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_SENDTO);
                fragment.setListType(ListEvent.ListType.PICK_CONTACT_FOR_SHORTCUT);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();
                fragment.setListType(ListEvent.ListType.PICK_POSTAL);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_JOIN: {
                JoinContactListFragment joinFragment = new JoinContactListFragment();
                joinFragment.setTargetContactId(getTargetContactId());
                joinFragment.setListType(ListEvent.ListType.PICK_JOIN);
                mListFragment = joinFragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_GROUP_MEMBERS: {
                final String accountName = getIntent().getStringExtra(
                        UiIntentActions.GROUP_ACCOUNT_NAME);
                final String accountType = getIntent().getStringExtra(
                        UiIntentActions.GROUP_ACCOUNT_TYPE);
                final String accountDataSet = getIntent().getStringExtra(
                        UiIntentActions.GROUP_ACCOUNT_DATA_SET);
                final ArrayList<String> contactIds = getIntent().getStringArrayListExtra(
                        UiIntentActions.GROUP_CONTACT_IDS);
                ///M:[Sim Group] add subId @{
                final int subId = getIntent().getIntExtra(
                        UiIntentActions.GROUP_ACCOUNT_SUBID, -1);

                mListFragment = GroupMemberPickerFragment.newInstance(
                        accountName, accountType, accountDataSet, contactIds, subId);
                /// @}
                mListFragment.setListType(ListEvent.ListType.PICK_GROUP_MEMBERS);
                break;
            }

            default:
                throw new IllegalStateException("Invalid action code: " + mActionCode);
        }

        /* M: [Sim Contact Flow]ALPS01256880, 3/3 @{ */
        ActivitiesUtils.setPickerFragmentAccountType(this, mListFragment);
        /* @} */


        // Setting compatibility is no longer needed for PhoneNumberPickerFragment since that logic
        // has been separated into LegacyPhoneNumberPickerFragment.  But we still need to set
        // compatibility for other fragments.
        mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getFragmentManager().beginTransaction()
                .replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();

        //*/ freeme.liqiang, 20180308, for FreemeOS diaplay contacts number
        mListFragment.setContactsCountCallBack(new ContactEntryListFragment.ContactsCountCallBack() {
            @Override
            public void onShowCount(int count) {
                etFreemeSearchView.setHint(getString(R.string.freeme_hint_findContacts, count));
            }
        });
        //*/
    }

    private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest request) {
        if (mRequest.isLegacyCompatibilityMode()) {
            return new LegacyPhoneNumberPickerFragment();
        } else {
            return new PhoneNumberPickerFragment();
        }
    }

    public void setupActionListener() {
        if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                    new PhoneNumberPickerActionListener());
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                    new EmailAddressPickerActionListener());
        } else if (mListFragment instanceof MultiSelectEmailAddressesListFragment) {
            ((MultiSelectEmailAddressesListFragment) mListFragment).setCheckBoxListListener(this);
        } else if (mListFragment instanceof MultiSelectPhoneNumbersListFragment) {
            ((MultiSelectPhoneNumbersListFragment) mListFragment).setCheckBoxListListener(this);
        } else if (mListFragment instanceof JoinContactListFragment) {
            ((JoinContactListFragment) mListFragment).setOnContactPickerActionListener(
                    new JoinContactActionListener());
        } else if (mListFragment instanceof GroupMemberPickerFragment) {
            ((GroupMemberPickerFragment) mListFragment).setListener(
                    new GroupMemberPickerListener());
            getMultiSelectListFragment().setCheckBoxListListener(this);
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

    private MultiSelectContactsListFragment getMultiSelectListFragment() {
        if (mListFragment instanceof MultiSelectContactsListFragment) {
            return (MultiSelectContactsListFragment) mListFragment;
        }
        return null;
    }

    @Override
    public void onAction(int action) {
        Log.d(TAG, "[onAction]action = " + action);
        switch (action) {
            case ActionBarAdapter.Listener.Action.START_SEARCH_MODE:
                mIsSearchMode = true;
                ///M:[Google Issue][ALPS03481762]After exit select mode back to search mode,
                /// need hide check box as DefaultContactBrowseListFragment do. @{
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(false);
                }
                /// @}
                configureSearchMode();
                break;
            case ActionBarAdapter.Listener.Action.CHANGE_SEARCH_QUERY:
                /*/ freeme.liqiang, 20180307. for FreemeOS single selected
                final String queryString = mActionBarAdapter.getQueryString();
                mListFragment.setQueryString(queryString, / * delaySelection * / false);
                /*/
                break;
            case ActionBarAdapter.Listener.Action.START_SELECTION_MODE:
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(true);
                }
                invalidateOptionsMenu();
                break;
            case ActionBarAdapter.Listener.Action.STOP_SEARCH_AND_SELECTION_MODE:
                /*/ freeme.liqiang, 20180307. for FreemeOS single selected
                mListFragment.setQueryString("", / * delaySelection * / false);
                mActionBarAdapter.setSearchMode(false);
                if (getMultiSelectListFragment() != null) {
                    getMultiSelectListFragment().displayCheckBoxes(false);
                }
                /*/
                invalidateOptionsMenu();
                break;
        }
    }

    @Override
    public void onUpButtonPressed() {
        onBackPressed();
    }

    @Override
    public void onStartDisplayingCheckBoxes() {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        mActionBarAdapter.setSelectionMode(true);
        /*/
    }

    @Override
    public void onSelectedContactIdsChanged() {
        if (mListFragment instanceof MultiSelectContactsListFragment) {
            final int count = getMultiSelectListFragment().getSelectedContactIds().size();
            /*/ freeme.liqiang, 20180307. for FreemeOS single selected
            mActionBarAdapter.setSelectionCount(count);
            updateAddContactsButton(count);
            /*/

            //*/ freeme.liqiang, 20180705. add multi-selected
            if (mShowSelectMode) {
                getActionBar().setTitle(getString(R.string.freeme_delete_contact_choice,
                        String.valueOf(count)));
            }
            //*/
            // Show or hide the multi select "Done" button
            invalidateOptionsMenu();
        }
    }

    /*/ freeme.liqiang, 20180307. for FreemeOS single selected
    private void updateAddContactsButton(int count) {
        final TextView textView = (TextView) mActionBarAdapter.getSelectionContainer()
                .findViewById(R.id.add_contacts);
        if (count > 0) {
            textView.setVisibility(View.VISIBLE);
            textView.setAllCaps(true);
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final long[] contactIds =
                            getMultiSelectListFragment().getSelectedContactIdsArray();
                    returnSelectedContacts(contactIds);
                }
            });
        } else {
            textView.setVisibility(View.GONE);
        }
    }
    /*/

    @Override
    public void onStopDisplayingCheckBoxes() {
        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        mActionBarAdapter.setSelectionMode(false);
        /*/
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        @Override
        public void onCreateNewContactAction() {
            startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            startActivityAndForwardResult(EditorIntents.createEditContactIntent(
                    ContactSelectionActivity.this, contactLookupUri, /* materialPalette =*/ null,
                    /* photoId =*/ -1));
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            //*/ freeme.liqiang, 20180815. for contacts shortcut
            if (!createShortcut(intent))
            //*/
            returnPickerResult(intent);
        }
    }

    private final class PhoneNumberPickerActionListener implements
            OnPhoneNumberPickerActionListener {
        @Override
        public void onPickDataUri(Uri dataUri, boolean isVideoCall, int callInitiationType) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onPickPhoneNumber(String phoneNumber, boolean isVideoCall,
                                      int callInitiationType) {
            Log.w(TAG, "Unsupported call.");
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            //*/ freeme.liqiang, 20180815. for contacts shortcut
            if (!createShortcut(intent))
            //*/
            returnPickerResult(intent);
        }

        @Override
        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class JoinContactActionListener implements OnContactPickerActionListener {
        @Override
        public void onPickContactAction(Uri contactUri) {
            Intent intent = new Intent(null, contactUri);
            setResult(RESULT_OK, intent);
            finish();
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
        }

        @Override
        public void onCreateNewContactAction() {
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
        }
    }

    private final class GroupMemberPickerListener implements GroupMemberPickerFragment.Listener {

        @Override
        public void onGroupMemberClicked(long contactId) {
            Log.d(TAG, "[onGroupMemberClicked] contactId=" + contactId);
            final Intent intent = new Intent();
            intent.putExtra(UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, contactId);
            returnPickerResult(intent);
        }

        @Override
        public void onSelectGroupMembers() {
            /*/ freeme.liqiang, 20180307. for FreemeOS single selected
            mActionBarAdapter.setSelectionMode(true);
            /*/
            //*/ freeme.liqiang, 20180705. add multi-selected
            final long[] contactIds =
                    getMultiSelectListFragment().getSelectedContactIdsArray();
            returnSelectedContacts(contactIds);
            //*/
        }
    }

    private void returnSelectedContacts(long[] contactIds) {
        final Intent intent = new Intent();
        intent.putExtra(UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY, contactIds);
        returnPickerResult(intent);
    }

    private final class PostalAddressPickerActionListener implements
            OnPostalAddressPickerActionListener {
        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements
            OnEmailAddressPickerActionListener {
        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        try {
            ImplicitIntentsUtil.startActivityInApp(ContactSelectionActivity.this, intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "startActivity() failed: " + e);
            Toast.makeText(ContactSelectionActivity.this, R.string.missing_app,
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view.getId() == R.id.search_view) {
            if (hasFocus) {
                /*/ freeme.liqiang, 20180307. for FreemeOS single selected
                mActionBarAdapter.setFocusOnSearchView();
                /*/
            }
        }
    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.floating_action_button) {
            startCreateNewContactActivity();
        }
    }

    private long getTargetContactId() {
        Intent intent = getIntent();
        final long targetContactId = intent.getLongExtra(
                UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, -1);
        if (targetContactId == -1) {
            Log.e(TAG, "Intent " + intent.getAction() + " is missing required extra: "
                    + UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY);
            setResult(RESULT_CANCELED);
            finish();
            return -1;
        }
        return targetContactId;
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        /*/ freeme.liqiang, 20180327. always enter the details interface when save new contact
        intent.putExtra(ContactEditorActivity.
                INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
        /*/
        intent.putExtra(ContactEditorActivity.
                INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, false);
        //*/
        /// M: Add account type for handling special case for add new contactor. @{
        intent.putExtra(ContactsSettingsUtils.ACCOUNT_TYPE,
                        getIntent().getIntExtra(ContactsSettingsUtils.ACCOUNT_TYPE,
                                                ContactsSettingsUtils.ALL_TYPE_ACCOUNT));
        /// @}
        startActivityAndForwardResult(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        /*/ freeme.liqiang, 20180308. for FreemeOS remove searchview
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchItem.setVisible(!mIsSearchMode && mIsSearchSupported);

        final Drawable searchIcon = searchItem.getIcon();
        if (searchIcon != null) {
            searchIcon.mutate().setColorFilter(ContextCompat.getColor(this,
                    R.color.actionbar_icon_color), PorterDuff.Mode.SRC_ATOP);
        }
        //*/
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!isSafeToCommitTransactions()) {
            return;
        }

        /*/ freeme.liqiang, 20180307. for FreemeOS single selected
        if (isSelectionMode()) {
            mActionBarAdapter.setSelectionMode(false);
            if (getMultiSelectListFragment() != null) {
                getMultiSelectListFragment().displayCheckBoxes(false);
            }
        } else if (mIsSearchMode) {
        /*/
        if (mIsSearchMode) {
        //*/
            mIsSearchMode = false;
            configureSearchMode();
        } else {
            super.onBackPressed();
        }

        if (mIsSearchMode) {
            mIsSearchMode = false;
            configureSearchMode();
        } else {
            super.onBackPressed();
        }

    }

    //*/ freeme.liqiang, 20180704. add multi-selected
    private boolean mShowSelectMode;
    private final static String SHOW_SELECT_MODE = "show_select_mode";
    //*/

    //*/ freeme.liqiang, 20180815. for contacts shortcut
    private boolean createShortcut(Intent intent) {
        if (intent != null && BuildCompat.isAtLeastO()) {
            String shortcutId = intent.getStringExtra(ShortcutIntentBuilder.EXTRA_SHORTCUT_ID);
            if (TextUtils.isEmpty(shortcutId)) {
                return false;
            }

            long contactId = -1;
            try {
                contactId = Integer.valueOf(shortcutId);
            } catch (NumberFormatException e) {
            }

            String displayName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

            final ShortcutManager shortcutManager = (ShortcutManager)
                    getSystemService(SHORTCUT_SERVICE);
            final DynamicShortcuts shortcuts = new DynamicShortcuts(this);
            final ShortcutInfo info;
            if (contactId != -1) {
                String lookupKey = intent.getStringExtra(ShortcutIntentBuilder.EXTRA_LOOKUPKEY);
                info = shortcuts.getQuickContactShortcutInfo(contactId, lookupKey, displayName);
            } else {
                Icon icon = null;
                Parcelable bit = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
                if (bit instanceof Bitmap) {
                    icon = IconCompat.createWithAdaptiveBitmap((Bitmap) bit).toIcon();
                }
                Intent shortcutIntent = null;
                Parcelable in = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                if (in instanceof Intent) {
                    shortcutIntent = (Intent) in;
                }
                info = shortcuts.getActionShortcutInfo(shortcutId, displayName,
                        shortcutIntent, icon);
            }
            shortcutManager.requestPinShortcut(info, null);
            return true;
        }
        return false;
    }
    //*/
}
