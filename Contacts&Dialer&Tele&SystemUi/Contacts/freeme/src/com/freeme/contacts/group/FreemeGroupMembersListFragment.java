package com.freeme.contacts.group;

import android.annotation.StringRes;
import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.editor.EditorUiUtils;
import com.android.contacts.group.GroupMembersAdapter;
import com.android.contacts.group.GroupMembersAdapter.GroupMembersQuery;
import com.android.contacts.group.GroupMembersFragment;
import com.android.contacts.group.GroupMetaData;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.group.UpdateGroupMembersAsyncTask;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsSectionIndexer;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.logging.ListEvent;
import com.android.contacts.logging.Logger;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.contacts.common.utils.FreemeIntentUtils;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.contacts.common.widgets.FreemeEmptyContentView;
import com.google.common.primitives.Longs;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.group.UpdateSimGroupMembersAsyncTask;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.ProgressHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FreemeGroupMembersListFragment extends MultiSelectContactsListFragment<GroupMembersAdapter>
        implements ContactSaveService.DeleteEndListener,
        FreemeEmptyContentView.OnEmptyViewActionButtonClickedListener {

    private static final String TAG = "FreemeGroupMembers";

    private static final String KEY_IS_EDIT_MODE = "editMode";
    private static final String KEY_GROUP_URI = "groupUri";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_GROUP_METADATA = "groupMetadata";
    /// M: [Sim Group][ALPS03477180] 1/3 @{
    private static final String KEY_SUB_ID = "subId";
    /// @}

    private static final String ARG_GROUP_URI = "groupUri";
    private static final String ARG_GROUP_ID = "groupId";

    private static final int LOADER_GROUP_METADATA = 100;
    private static final int MSG_FAIL_TO_LOAD = 1;
    private static final int RESULT_GROUP_ADD_MEMBER = 100;
    private static final String EXTRA_KEY_SELECTED_MODE = "navBar.selectionMode";

    /// M: [Sim Group] sub id for sim group @{
    private int mSubId = SubInfoUtils.getInvalidSubId();
    /// @}

    /**
     * Filters out duplicate contacts.
     */
    private class FilterCursorWrapper extends CursorWrapper {

        private int[] mIndex;
        private int mCount = 0;
        private int mPos = 0;

        private FilterCursorWrapper(Cursor cursor) {
            super(cursor);

            mCount = super.getCount();
            mIndex = new int[mCount];

            final List<Integer> indicesToFilter = new ArrayList<>();

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Group members CursorWrapper start: " + mCount);
            }

            final Bundle bundle = cursor.getExtras();
            final String sections[] = bundle.getStringArray(Contacts
                    .EXTRA_ADDRESS_BOOK_INDEX_TITLES);
            final int counts[] = bundle.getIntArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
            final ContactsSectionIndexer indexer = (sections == null || counts == null)
                    ? null : new ContactsSectionIndexer(sections, counts);

            mGroupMemberContactIds.clear();
            for (int i = 0; i < mCount; i++) {
                super.moveToPosition(i);
                final String contactId = getString(GroupMembersQuery.CONTACT_ID);
                if (!mGroupMemberContactIds.contains(contactId)) {
                    mIndex[mPos++] = i;
                    mGroupMemberContactIds.add(contactId);
                } else {
                    indicesToFilter.add(i);
                }
            }

            if (indexer != null && GroupUtil.needTrimming(mCount, counts, indexer.getPositions())) {
                GroupUtil.updateBundle(bundle, indexer, indicesToFilter, sections, counts);
            }

            mCount = mPos;
            mPos = 0;
            super.moveToFirst();

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Group members CursorWrapper end: " + mCount);
            }
        }

        @Override
        public boolean move(int offset) {
            return moveToPosition(mPos + offset);
        }

        @Override
        public boolean moveToNext() {
            return moveToPosition(mPos + 1);
        }

        @Override
        public boolean moveToPrevious() {
            return moveToPosition(mPos - 1);
        }

        @Override
        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return moveToPosition(mCount - 1);
        }

        @Override
        public boolean moveToPosition(int position) {
            if (position >= mCount) {
                mPos = mCount;
                return false;
            } else if (position < 0) {
                mPos = -1;
                return false;
            }
            mPos = mIndex[position];
            return super.moveToPosition(mPos);
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public int getPosition() {
            return mPos;
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetaDataCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupMetaDataLoader(mActivity, mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor == null || cursor.isClosed() || !cursor.moveToNext()) {
                Log.e(TAG, "Failed to load group metadata for " + mGroupUri);
                Toast.makeText(getContext(), R.string.groupLoadErrorToast, Toast.LENGTH_SHORT)
                        .show();
                mHandler.sendEmptyMessage(MSG_FAIL_TO_LOAD);
                return;
            }
            mGroupMetaData = new GroupMetaData(mActivity, cursor);
            mCustomRingtone = mGroupMetaData.ringtone;
            setActionBarTitle(mGroupMetaData.groupName);
            mGroupId = mGroupMetaData.groupId;
            /// M: [Sim Group] @{
            mSubId = AccountTypeUtils.getSubIdBySimAccountName(mActivity,
                    mGroupMetaData.accountName);
            FreemeLogUtils.d(TAG, "[onLoadFinished] mSubId is " + mSubId);
            /// @}
            onGroupMetadataLoaded();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    /**
     * M:[Google issue][ALPS03411014]This is to disable {@link #onOptionsItemSelected}
     * when we trying to stop the activity/fragment. 1/4 @{
     */
    private boolean mDisableOptionItemSelected;
    /**
     * @}
     */

    private FreemeGroupBrowseActivity mActivity;

    private Uri mGroupUri;
    private long mGroupId;

    private boolean mIsEditMode;

    private GroupMetaData mGroupMetaData;

    private Set<String> mGroupMemberContactIds = new HashSet();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_FAIL_TO_LOAD) {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }
        }
    };

    public static FreemeGroupMembersListFragment newInstance(Uri groupUri, long groupId, String navigateTitle) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_GROUP_URI, groupUri);
        args.putLong(ARG_GROUP_ID, groupId);
        args.putString(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, navigateTitle);

        final FreemeGroupMembersListFragment fragment = new FreemeGroupMembersListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public FreemeGroupMembersListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(false);
        setHasOptionsMenu(true);
        setListType(ListEvent.ListType.GROUP);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mGroupMetaData == null) {
            // Hide menu options until metadata is fully loaded
            Log.e(TAG, "[onCreateOptionsMenu]mGroupMetaData == null !!!");
            return;
        }
        inflater.inflate(R.menu.freeme_group_member_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mOptionMenu = menu;
        final boolean isSelectionMode = isSelectionMode();
        final boolean isGroupReadOnly = mGroupMetaData != null && mGroupMetaData.readOnly;
        final boolean isGroupEditable = !isGroupReadOnly && mGroupMetaData != null
                && mGroupMetaData.editable;
        boolean isEnable = mSelectedCount > 0;

        setVisible(menu, R.id.menu_add, !isSelectionMode && !isGroupEmpty(), true);
        setVisible(menu, R.id.menu_multi_send_email, isSelectionMode, isEnable);
        setVisible(menu, R.id.menu_multi_send_message, isSelectionMode, isEnable);
        setVisible(menu, R.id.menu_remove_from_group, isSelectionMode && isGroupEditable, isEnable);
        setVisible(menu, R.id.freeme_group_item_ring,
                !isSelectionMode && isGroupEditable && !isGroupEmpty(), true);
        setVisible(menu, R.id.freeme_menu_select_all, isSelectionMode, true);

        int title;
        if (mSelectedCount == getAdapter().getCount()) {
            title = R.string.menu_select_none;
        } else {
            title = R.string.menu_select_all;
        }
        updateMenuTitle(menu, R.id.freeme_menu_select_all, title);
    }

    private boolean isGroupEmpty() {
        return getAdapter() != null && getAdapter().isEmpty();
    }

    private void setVisible(Menu menu, int id, boolean visible, boolean enable) {
        final MenuItem menuItem = menu.findItem(id);
        if (menuItem != null) {
            menuItem.setVisible(visible);
            menuItem.setEnabled(enable);
        }
    }

    private void updateMenuTitle(Menu menu, int id, @StringRes int title) {
        final MenuItem menuItem = menu.findItem(id);
        if (menuItem != null) {
            menuItem.setTitle(title);
        }
    }

    private void updateMenuTitle(MenuItem menuItem, @StringRes int title) {
        if (menuItem != null) {
            menuItem.setTitle(title);
        }
    }

    /**
     * Helper class for managing data related to contacts and emails/phone numbers.
     */
    private class ContactDataHelperClass {

        private List<String> items = new ArrayList<>();
        private String mostUsedItemId = null;
        private int mostUsedTimes;
        private String primaryItemId = null;

        public void addItem(String item, int timesUsed, boolean primaryFlag) {
            if (mostUsedItemId == null || timesUsed > mostUsedTimes) {
                mostUsedItemId = item;
                mostUsedTimes = timesUsed;
            }
            if (primaryFlag) {
                primaryItemId = item;
            }
            items.add(item);
        }

        public boolean hasDefaultItem() {
            return primaryItemId != null || items.size() == 1;
        }

        public String getDefaultSelectionItemId() {
            return primaryItemId != null
                    ? primaryItemId
                    : mostUsedItemId;
        }
    }

    public void sendToGroup(long[] ids, String sendScheme, String title) {
        if (ids == null || ids.length == 0) return;

        // Get emails or phone numbers
        // contactMap <contact_id, contact_data>
        final Map<String, ContactDataHelperClass> contactMap = new HashMap<>();
        // itemList <item_data>
        final List<String> itemList = new ArrayList<>();
        final String sIds = GroupUtil.convertArrayToString(ids);
        final String select = (ContactsUtils.SCHEME_MAILTO.equals(sendScheme)
                ? GroupMembersFragment.Query.EMAIL_SELECTION
                : GroupMembersFragment.Query.PHONE_SELECTION)
                + " AND " + ContactsContract.Data.CONTACT_ID + " IN (" + sIds + ")";
        final ContentResolver contentResolver = getContext().getContentResolver();
        final Cursor cursor = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                ContactsUtils.SCHEME_MAILTO.equals(sendScheme)
                        ? GroupMembersFragment.Query.EMAIL_PROJECTION
                        : GroupMembersFragment.Query.PHONE_PROJECTION,
                select, null, null);

        if (cursor == null) {
            return;
        }

        try {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                final String contactId = cursor.getString(GroupMembersFragment.Query.CONTACT_ID);
                final String itemId = cursor.getString(GroupMembersFragment.Query.ITEM_ID);
                final boolean isPrimary = cursor.getInt(GroupMembersFragment.Query.PRIMARY) != 0;
                final int timesUsed = cursor.getInt(GroupMembersFragment.Query.TIMES_USED);
                final String data = cursor.getString(GroupMembersFragment.Query.DATA1);

                if (!TextUtils.isEmpty(data)) {
                    final ContactDataHelperClass contact;
                    if (!contactMap.containsKey(contactId)) {
                        contact = new ContactDataHelperClass();
                        contactMap.put(contactId, contact);
                    } else {
                        contact = contactMap.get(contactId);
                    }
                    contact.addItem(itemId, timesUsed, isPrimary);
                    itemList.add(data);
                }
            }
        } finally {
            cursor.close();
        }

        // Start picker if a contact does not have a default
        for (ContactDataHelperClass i : contactMap.values()) {
            if (!i.hasDefaultItem()) {
                // Build list of default selected item ids
                final List<Long> defaultSelection = new ArrayList<>();
                for (ContactDataHelperClass j : contactMap.values()) {
                    final String selectionItemId = j.getDefaultSelectionItemId();
                    if (selectionItemId != null) {
                        defaultSelection.add(Long.parseLong(selectionItemId));
                    }
                }
                final long[] defaultSelectionArray = Longs.toArray(defaultSelection);
                startSendToSelectionPickerActivity(ids, defaultSelectionArray, sendScheme, title);
                return;
            }
        }

        if (itemList.size() == 0 || contactMap.size() < ids.length) {
            Toast.makeText(getContext(), ContactsUtils.SCHEME_MAILTO.equals(sendScheme)
                            ? getString(R.string.groupSomeContactsNoEmailsToast)
                            : getString(R.string.groupSomeContactsNoPhonesToast),
                    Toast.LENGTH_LONG).show();
        }

        if (itemList.size() == 0) {
            return;
        }

        final String itemsString = TextUtils.join(",", itemList);
        GroupUtil.startSendToSelectionActivity(this, itemsString, sendScheme, title);
    }

    private void startSendToSelectionPickerActivity(long[] ids, long[] defaultSelection,
                                                    String sendScheme, String title) {
        startActivity(GroupUtil.createSendToSelectionPickerIntent(getContext(), ids,
                defaultSelection, sendScheme, title));
    }

    private void startGroupAddMemberActivity() {
        ///M: [Sim Group] add subId @{
        startActivityForResult(GroupUtil.createPickMemberIntent(getContext(), mGroupMetaData,
                getMemberContactIds(), mSubId, true), RESULT_GROUP_ADD_MEMBER);
        /// @}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "[onOptionsItemSelected] item = " + item.getTitle() +
                ", mDisableOptionItemSelected = " + mDisableOptionItemSelected);
        /// M:[Google issue][ALPS03411014] 2/4 @{
        if (mDisableOptionItemSelected || mGroupMetaData == null) {
            return false;
        }
        /// @}
        final int id = item.getItemId();
        if (id == R.id.menu_add) {
            startGroupAddMemberActivity();
        } else if (id == R.id.menu_multi_send_email) {
            final long[] ids = isSelectionMode()
                    ? getAdapter().getSelectedContactIdsArray()
                    : GroupUtil.convertStringSetToLongArray(mGroupMemberContactIds);
            sendToGroup(ids, ContactsUtils.SCHEME_MAILTO,
                    getString(R.string.menu_sendEmailOption));
        } else if (id == R.id.menu_multi_send_message) {
            final long[] ids = isSelectionMode()
                    ? getAdapter().getSelectedContactIdsArray()
                    : GroupUtil.convertStringSetToLongArray(mGroupMemberContactIds);
            sendToGroup(ids, ContactsUtils.SCHEME_SMSTO,
                    getString(R.string.menu_sendMessageOption));
        } else if (id == R.id.menu_remove_from_group) {
            removeSelectedContacts();
        } else if (id == R.id.freeme_group_item_ring) {
            freemeGroupRing();
        } else if (id == R.id.freeme_menu_select_all) {
            if (mSelectedCount == getAdapter().getCount()) {
                updateCheckBoxState(false);
                updateMenuTitle(item, R.string.menu_select_all);
                setSelectionCount(0);
            } else {
                updateCheckBoxState(true);
                setSelectionCount(getAdapter().getCount());
                updateMenuTitle(item, R.string.menu_select_none);
            }
            getAdapter().notifyDataSetChanged();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void removeSelectedContacts() {
        /// M: [Sim Group] [Google Issue]ALPS00542175 @{
        if (!SimGroupUtils.checkServiceState(true, mSubId, mActivity)) {
            return;
        }
        /// @}
        final long[] contactIds = getAdapter().getSelectedContactIdsArray();
        /// M: [Sim Group] @{
        if (mSubId > 0) {
            new UpdateSimGroupMembersAsyncTask(UpdateGroupMembersAsyncTask.TYPE_REMOVE,
                    getContext(), contactIds, mGroupMetaData.groupId, mGroupMetaData.accountName,
                    mGroupMetaData.accountType, mGroupMetaData.dataSet,
                    mGroupMetaData.groupName, mSubId).execute();
        } else {
            /// @}
            new UpdateGroupMembersAsyncTask(UpdateGroupMembersAsyncTask.TYPE_REMOVE,
                    getContext(), contactIds, mGroupMetaData.groupId, mGroupMetaData.accountName,
                    mGroupMetaData.accountType, mGroupMetaData.dataSet).execute();
        }
        setSelectionMode(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_RINGTONE && data != null) {
            final Uri pickedUri = data.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            mCustomRingtone = EditorUiUtils.getRingtoneStringFromUri(pickedUri, CURRENT_API_VERSION);
            long[] contactIds = GroupUtil.convertStringSetToLongArray(mGroupMemberContactIds);
            Intent intentGroupContactUri = ContactSaveService.createGroupMultiSetRingtone(
                    getActivity(), contactIds, mCustomRingtone);
            getActivity().startService(intentGroupContactUri);
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Groups.GROUP_RING, mCustomRingtone);
            final Uri groupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, mGroupId);
            getContext().getContentResolver().update(groupUri, values, null, null);
            return;
        }

        if (resultCode != Activity.RESULT_OK || data == null
                || requestCode != RESULT_GROUP_ADD_MEMBER) {
            return;
        }
        /// M: [Sim Group] [Google Issue]ALPS00542175 @{
        if (!SimGroupUtils.checkServiceState(true, mSubId, mActivity)) {
            return;
        }
        /// @}
        long[] contactIds = data.getLongArrayExtra(
                UiIntentActions.TARGET_CONTACT_IDS_EXTRA_KEY);
        if (contactIds == null) {
            final long contactId = data.getLongExtra(
                    UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, -1);
            if (contactId > -1) {
                contactIds = new long[1];
                contactIds[0] = contactId;
            }
        }
        /// M: [Sim Group] @{
        if (mSubId > 0) {
            new UpdateSimGroupMembersAsyncTask(UpdateGroupMembersAsyncTask.TYPE_ADD,
                    getContext(), contactIds, mGroupMetaData.groupId, mGroupMetaData.accountName,
                    mGroupMetaData.accountType, mGroupMetaData.dataSet,
                    mGroupMetaData.groupName, mSubId).execute();
        } else {
            /// @}
            new UpdateGroupMembersAsyncTask(UpdateGroupMembersAsyncTask.TYPE_ADD,
                    getContext(), contactIds, mGroupMetaData.groupId, mGroupMetaData.accountName,
                    mGroupMetaData.accountType, mGroupMetaData.dataSet).execute();
        }

        if (mCustomRingtone != null) {
            Intent intentGroupContactUri = ContactSaveService.createGroupMultiSetRingtone(
                    getActivity(), contactIds, mCustomRingtone);
            getActivity().startService(intentGroupContactUri);
        }
    }

    private final OnCheckBoxListActionListener mCheckBoxListener = new OnCheckBoxListActionListener() {
        @Override
        public void onStartDisplayingCheckBoxes() {
            setSelectionMode(true);
        }

        @Override
        public void onSelectedContactIdsChanged() {
            if (!getAdapter().isDisplayingCheckBoxes()) {
                setActionBarTitle(mGroupMetaData.groupName);
            } else {
                setSelectionCount(getSelectedContactIds().size());
            }
        }

        @Override
        public void onStopDisplayingCheckBoxes() {
            setSelectionMode(false);
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (FreemeGroupBrowseActivity) getActivity();
        final ContactsRequest contactsRequest = new ContactsRequest();
        contactsRequest.setActionCode(ContactsRequest.ACTION_GROUP);
        if (mGroupMetaData != null) {
            mActivity.setTitle(mGroupMetaData.groupName);
            if (mGroupMetaData.editable) {
                setCheckBoxListListener(mCheckBoxListener);
            }
        }
        mSelectionMode = false;
        if (savedInstanceState != null) {
            mSelectionMode = savedInstanceState.getBoolean(EXTRA_KEY_SELECTED_MODE);
            setSelectionMode(mSelectionMode, true);
        }
    }

    public void displayDeleteButtons(boolean displayDeleteButtons) {
        getAdapter().setDisplayDeleteButtons(displayDeleteButtons);
    }

    public ArrayList<String> getMemberContactIds() {
        return new ArrayList<>(mGroupMemberContactIds);
    }

    public int getMemberCount() {
        return mGroupMemberContactIds.size();
    }

    public boolean isEditMode() {
        return mIsEditMode;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (savedState == null) {
            mGroupUri = getArguments().getParcelable(ARG_GROUP_URI);
            mGroupId = getArguments().getLong(ARG_GROUP_ID);
        } else {
            mIsEditMode = savedState.getBoolean(KEY_IS_EDIT_MODE);
            mGroupUri = savedState.getParcelable(KEY_GROUP_URI);
            mGroupMetaData = savedState.getParcelable(KEY_GROUP_METADATA);
            if (mGroupMetaData != null) {
                mGroupId = mGroupMetaData.groupId;
            } else {
                mGroupId = savedState.getLong(KEY_GROUP_ID);
            }
            mSubId = savedState.getInt(KEY_SUB_ID);
            mCustomRingtone = savedState.getString(FREEME_GROUP_RING, null);
        }
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            setNavigateTitle(actionBar, getArguments().getString(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT));
        }
        Log.d(TAG, "[onCreate] mGroupUri is " + mGroupUri + ", mSubId is " + mSubId);
        maybeAttachCheckBoxListener();
        ContactSaveService.setDeleteEndListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        /// M:[Google issue][ALPS03411014] 3/4 @{
        mDisableOptionItemSelected = false;
        /// @}
    }

    @Override
    protected void startLoading() {
        if (mGroupMetaData == null || !mGroupMetaData.isValid()) {
            getLoaderManager().restartLoader(LOADER_GROUP_METADATA,
                    null, mGroupMetaDataCallbacks);
        } else {
            onGroupMetadataLoaded();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            // Wait until contacts are loaded before showing the scrollbar
            setVisibleScrollbarEnabled(true);

            final FilterCursorWrapper cursorWrapper = new FilterCursorWrapper(data);
            bindMembersCount(cursorWrapper.getCount());
            super.onLoadFinished(loader, cursorWrapper);
            // Update state of menu items (e.g. "Remove contacts") based on number of group members.
            mActivity.invalidateOptionsMenu();
            ///M:[ALPS03608123] exit edit mode here @{
            if (!isInactive() && isEditMode()) {
                //If we're deleting the last group member, exit edit mode
                exitEditMode();
            }
            ///@}
        }
    }

    private void bindMembersCount(int memberCount) {
        final View emptyGroupView = getView().findViewById(R.id.empty_list_view);
        if (memberCount > 0 && mGroupMetaData != null) {
            emptyGroupView.setVisibility(View.GONE);
        } else {
            emptyGroupView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "[onSaveInstanceState] mSubId = " + mSubId);
        /// M:[Google issue][ALPS03411014] 4/4 @{
        mDisableOptionItemSelected = true;
        /// @}
        outState.putBoolean(KEY_IS_EDIT_MODE, mIsEditMode);
        outState.putParcelable(KEY_GROUP_URI, mGroupUri);
        outState.putLong(KEY_GROUP_ID, mGroupId);
        outState.putParcelable(KEY_GROUP_METADATA, mGroupMetaData);
        /// M: [Sim Group][ALPS03477180] 2/3 @{
        outState.putInt(KEY_SUB_ID, mSubId);
        /// @}
        outState.putBoolean(EXTRA_KEY_SELECTED_MODE, mSelectionMode);
    }

    private void onGroupMetadataLoaded() {
        FreemeLogUtils.i(TAG, "Loaded " + mGroupMetaData);

        maybeAttachCheckBoxListener();

        mActivity.setTitle(mGroupMetaData.groupName);
        mActivity.invalidateOptionsMenu();

        // Start loading the group members
        super.startLoading();
    }

    private void maybeAttachCheckBoxListener() {
        // Don't attach the multi select check box listener if we can't edit the group
        if (mGroupMetaData != null && mGroupMetaData.editable) {
            setCheckBoxListListener(mCheckBoxListener);
        }
    }

    @Override
    protected GroupMembersAdapter createListAdapter() {
        final GroupMembersAdapter adapter = new GroupMembersAdapter(getContext());
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        adapter.setDeleteContactListener(new DeletionListener());
        return adapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        getAdapter().setGroupId(mGroupId);
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.freeme_contact_list_content, /* root */ null);
        FreemeEmptyContentView emptyGroupView = new FreemeEmptyContentView(getActivity());
        emptyGroupView.setImage(R.drawable.freeme_empty_icon_contacts);
        emptyGroupView.setDescription(R.string.freeme_groups_no_members_tips);
        emptyGroupView.setActionLabel(R.string.freeme_groups_no_members_action_tips);
        emptyGroupView.setId(R.id.empty_list_view);
        emptyGroupView.setActionClickedListener(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        emptyGroupView.setLayoutParams(params);

        FrameLayout contactListLayout = view.findViewById(R.id.contact_list);
        contactListLayout.addView(emptyGroupView);

        setContactsCountCallBack(new ISelectCountCallBack() {
            public void onShowCount(int count) {
                mSelectedCount = count;
                getActivity().invalidateOptionsMenu();
            }
        });
        return view;
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        getListView().setDividerHeight(1);
    }

    private static final int REQUEST_CODE_PICK_RINGTONE = 4;
    private String mCustomRingtone;
    private String FREEME_GROUP_RING = "freemeGroupRing";
    private static final int CURRENT_API_VERSION = android.os.Build.VERSION.SDK_INT;

    private void freemeGroupRing() {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        // Allow user to pick 'Default'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        // Show only ringtones
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        // Allow the user to pick a silent ringtone
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        if (mGroupMetaData != null) {
            mCustomRingtone = mGroupMetaData.ringtone;
        }

        final Uri ringtoneUri = EditorUiUtils.getRingtoneUriFromString(mCustomRingtone,
                CURRENT_API_VERSION);

        // Put checkmark next to the current ringtone for this contact
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);

        // Launch!
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(getActivity(), R.string.missing_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onItemClick(int position, long id) {
        final Uri uri = getAdapter().getContactUri(position);
        if (uri == null) {
            return;
        }
        if (getAdapter().isDisplayingCheckBoxes()) {
            super.onItemClick(position, id);
            if (getAdapter().getSelectedContactIds().size() == getAdapter().getCount()) {
                updateMenuTitle(mOptionMenu, R.id.freeme_menu_select_all, R.string.menu_select_none);
            } else {
                updateMenuTitle(mOptionMenu, R.id.freeme_menu_select_all, R.string.menu_select_all);
            }
            return;
        }
        final int count = getAdapter().getCount();
        Logger.logListEvent(ListEvent.ActionType.CLICK, ListEvent.ListType.GROUP, count,
                /* clickedIndex */ position, /* numSelected */ 0);
        FreemeIntentUtils.startQuickContact(mActivity, uri, mGroupMetaData.groupName);
    }

    @Override
    protected boolean onItemLongClick(int position, long id) {
        if (mActivity != null && mIsEditMode) {
            return true;
        }
        return super.onItemLongClick(position, id);
    }


    private final class DeletionListener implements MultiSelectEntryContactListAdapter.DeleteContactListener {

        @Override
        public void onContactDeleteClicked(int position) {
            /// M: [Sim Group] [Google Issue]ALPS00542175 @{
            if (!SimGroupUtils.checkServiceState(true, mSubId, mActivity)) {
                return;
            }
            /// @}
            /**
             * M:[Google Issue][ALPS03485650] catch null pointer exception for timing issue @[
             * ori: final long contactId = getAdapter().getContactId(position);
             * new: @{ */
            long contactId;
            try {
                contactId = getAdapter().getContactId(position);
            } catch (Exception e) {
                Log.e(TAG, "[onContactDeleteClicked]exception:" + e.toString());
                return;
            }
            /** @} */
            final long[] contactIds = new long[1];
            contactIds[0] = contactId;
            /// M: [Sim Group] @{
            if (mSubId > 0) {
                new UpdateSimGroupMembersAsyncTask(UpdateGroupMembersAsyncTask.TYPE_REMOVE,
                        getContext(), contactIds, mGroupMetaData.groupId,
                        mGroupMetaData.accountName, mGroupMetaData.accountType,
                        mGroupMetaData.dataSet, mGroupMetaData.groupName, mSubId).execute();
            } else {
                /// @}
                new UpdateGroupMembersAsyncTask(UpdateGroupMembersAsyncTask.TYPE_REMOVE,
                        getContext(), contactIds, mGroupMetaData.groupId,
                        mGroupMetaData.accountName, mGroupMetaData.accountType,
                        mGroupMetaData.dataSet).execute();
            }
        }
    }

    /**
     * Return true if the fragment is not yet added, being removed, or detached.
     */
    public boolean isInactive() {
        return !isAdded() || isRemoving() || isDetached();
    }

    @Override
    public void onDestroy() {
        /// M: [Sim Group][Google Issue]ALPS00463033 @{
        ContactSaveService.removeDeleteEndListener(this);
        /// @}
        /// M: [Sim Group]ALPS03607814, ensure to dismiss dialog @{
        mProgressHandler.dismissDialog(getFragmentManager());
        /// @}
        super.onDestroy();
    }

    public void updateExistingGroupFragment(Uri newGroupUri, String action) {
        if (!GroupUtil.ACTION_REMOVE_FROM_GROUP.equals(action)) {
            mGroupUri = newGroupUri;
            mGroupMetaData = null; // Clear mGroupMetaData to trigger a new load.
            reloadData();
            if (mActivity != null) {
                mActivity.invalidateOptionsMenu();
            }
        }
    }

    public void exitEditMode() {
        mIsEditMode = false;
        setSelectionMode(false);
        displayDeleteButtons(false);
    }

    private ProgressHandler mProgressHandler = new ProgressHandler();

    @Override
    public void onDeleteEnd() {
        mProgressHandler.dismissDialog(getFragmentManager());
        //delete group done, then safely switch to AllContacts on UI thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mActivity.showOrHideFragment(true,
                        FreemeGroupBrowseActivity.FRAGMENT_TAG_GROUP_LIST);
            }
        });
    }

    @Override
    public void onDeleteStart() {
        mProgressHandler.showDialog(getFragmentManager());
    }

    private boolean isSelectionMode() {
        return mSelectionMode;
    }

    private boolean mSelectionMode;

    public void setSelectionMode(boolean selectionMode) {
        setSelectionMode(selectionMode, false);
    }

    private void setSelectionMode(boolean selectionMode, boolean forceUpdate) {
        if (forceUpdate || mSelectionMode != selectionMode) {
            mSelectionMode = selectionMode;
            updateSelectedItemsView();
            displayCheckBoxes(mSelectionMode);
            getActivity().invalidateOptionsMenu();
            if (!mSelectionMode) {
                setActionBarTitle(mGroupMetaData.groupName);
            }
        }
    }

    private void setActionBarTitle(String title) {
        mActivity.getActionBar().setTitle(title);
    }

    private int mSelectedCount;

    private void setSelectionCount(int count) {
        mSelectedCount = count;
        mActivity.getActionBar().setTitle(getString(R.string.freeme_selected_count, count));
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onEmptyViewActionButtonClicked() {
        startGroupAddMemberActivity();
    }

    private void setNavigateTitle(ActionBar actionBar, String title) {
        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.freeme_my_contacts_group);
        }
        if (actionBar != null) {
            FreemeActionBarUtil.setBackTitle(actionBar, title);
        }
    }
}
