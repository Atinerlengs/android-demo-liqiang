package com.freeme.contacts.group;

import android.accounts.Account;
import android.annotation.StringRes;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupListLoader;
import com.android.contacts.R;
import com.android.contacts.activities.TransactionSafeActivity;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.contacts.common.widgets.FreemeEmptyContentView;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FreemeGroupListFragment extends Fragment
        implements OnGroupListItemClickListener, OnGroupListItemLongClickListener {

    private static final int LOADER_GROUPS = 1;

    private LinearLayoutManager mManager;
    private FreemeGroupListAdapter mAdapter;
    private OnGroupListItemClickListener mListener;

    private RecyclerView mRecyclerView;
    private FreemeEmptyContentView mEmptyView;

    protected Menu mOptionMenu = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.freeme_group_list_fragment,
                null, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mEmptyView = view.findViewById(R.id.empty_list_view);
        mEmptyView.setImage(R.drawable.freeme_empty_icon_groups);
        mEmptyView.setDescription(R.string.freeme_groups_empty_tips);

        mAdapter = new FreemeGroupListAdapter(this);
        mManager = new LinearLayoutManager(getContext());
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.freeme_my_contacts_group));
            FreemeActionBarUtil.setBackTitle(actionBar, getSubTitle(getActivity().getIntent()));
        }
        mRecyclerView.setLayoutManager(mManager);
        mRecyclerView.setAdapter(mAdapter);

        setHasOptionsMenu(true);

        return view;
    }

    private long mGroupId;
    private List<GroupListItem> groupListItems;
    private int mSubId = SubInfoUtils.getInvalidSubId();

    private void deleteGroup() {
        if (!SimGroupUtils.checkServiceState(true, mSubId, getActivity())) {
            return;
        }
        String message = getActivity().getString(R.string.freeme_delete_group_dialog);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mSubId > 0) {
                                    getActivity().startService(SimGroupUtils.createMultipleGroupDeletionIntentForIcc(
                                            getActivity(), groupSelectCounts,
                                            mSubId, null));

                                } else {
                                    getActivity().startService(ContactSaveService.createGroupBatchDeletionIntent(
                                            getActivity(), groupSelectCounts));
                                }
                                groupSelectCounts.clear();
                                mAdapter.setmDispalyCheckBox(false);
                                getActivity().invalidateOptionsMenu();
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    public void sendToGroup(Map<Long, FreemeGroupMemberLoader.ContactDataHelperClass> contactMap,
                            List<String> itemList, String sendScheme, String title) {
        if (contactMap == null || contactMap.size() == 0
                || itemList == null || itemList.size() == 0) return;

        long[] ids = new long[contactMap.size()];
        int index = 0;
        // Start picker if a contact does not have a default
        for (FreemeGroupMemberLoader.ContactDataHelperClass i : contactMap.values()) {
            ids[index++] = i.getContactId();
            if (!i.hasDefaultItem()) {
                // Build list of default selected item ids
                final List<Long> defaultSelection = new ArrayList<>();
                for (FreemeGroupMemberLoader.ContactDataHelperClass j : contactMap.values()) {
                    final String selectionItemId = j.getDefaultSelectionItemId();
                    if (selectionItemId != null) {
                        defaultSelection.add(Long.parseLong(selectionItemId));
                    }
                }
                final long[] defaultSelectionArray = Longs.toArray(defaultSelection);
                startActivity(GroupUtil.createSendToSelectionPickerIntent(getContext(), ids,
                        defaultSelectionArray, sendScheme, title));
                return;
            }
        }

        final String itemsString = TextUtils.join(",", itemList);
        GroupUtil.startSendToSelectionActivity(this, itemsString, sendScheme, title);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnGroupListItemClickListener) {
            mListener = (OnGroupListItemClickListener) context;
        }
    }

    @Override
    public void onGroupItemClick(long groupId) {
        if (mListener != null && !mAdapter.ismDispalyCheckBox()) {
            mListener.onGroupItemClick(groupId);
        } else {
            setGroupItemCheck(groupId);
        }
    }

    public FreemeGroupListAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onGroupItemLongClick(long groupId) {
        setGroupItemCheck(groupId);
    }

    private List<Long> groupSelectCounts = new ArrayList<>();


    public void setGroupItemCheck(long groupId) {
        getActivity().invalidateOptionsMenu();
        if (groupSelectCounts.contains(groupId)) {
            groupSelectCounts.remove(groupId);
        } else {
            groupSelectCounts.add(groupId);
        }

        if (groupSelectCounts.size() == mAdapter.getItemCount()) {
            updateMenuTitle(mOptionMenu, R.id.freeme_menu_select_all, R.string.menu_select_none);
        } else {
            updateMenuTitle(mOptionMenu, R.id.freeme_menu_select_all, R.string.menu_select_all);
        }
        getActivity().invalidateOptionsMenu();
        mAdapter.setGroupItemCheck(groupSelectCounts);
        mAdapter.notifyDataSetChanged();
    }

    public void clearCheckBoxSelect() {
        groupSelectCounts.clear();
        getActivity().invalidateOptionsMenu();
        mAdapter.notifyDataSetChanged();
    }

    public void updateCheckBoxState(boolean checked) {
        int position = 0;
        final int count = mAdapter.getItemCount();
        long contactId = -1;
        if (checked) {
            for (; position < count; position++) {
                contactId = mAdapter.getItem(position).getGroupId();
                if (!groupSelectCounts.contains(contactId)) {
                    groupSelectCounts.add(contactId);
                }
            }
        } else {
            groupSelectCounts.clear();
        }
        mAdapter.setGroupItemCheck(groupSelectCounts);
        mAdapter.notifyDataSetChanged();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.freeme_my_contacts_group);
        getLoaderManager().initLoader(LOADER_GROUPS, null, mGroupListLoaderListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.freeme_group_list, menu);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mOptionMenu = menu;

        boolean isSelectedMode = mAdapter.ismDispalyCheckBox();
        int count = groupSelectCounts.size();

        setVisible(menu, R.id.freeme_menu_add_group, !isSelectedMode, true);
        setVisible(menu, R.id.freeme_menu_group_rename, isSelectedMode, count == 1);
        setVisible(menu, R.id.freeme_menu_send_message, isSelectedMode, count > 0);
        setVisible(menu, R.id.freeme_menu_group_delete, isSelectedMode, count > 0);
        setVisible(menu, R.id.freeme_menu_select_all, isSelectedMode, true);

        int title;
        if (count == getAdapter().getItemCount()) {
            title = R.string.menu_select_none;
        } else {
            title = R.string.menu_select_all;
        }
        updateMenuTitle(menu, R.id.freeme_menu_select_all, title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.freeme_menu_add_group:
                Activity activity = getActivity();
                if (activity instanceof TransactionSafeActivity) {
                    if (((TransactionSafeActivity) activity).isSafeToCommitTransactions()) {
                        onCreateGroupMenuItemClicked();
                    }
                } else {
                    onCreateGroupMenuItemClicked();
                }
                break;
            case R.id.freeme_menu_group_rename:
                mGroupId = groupSelectCounts.get(0);
                int count = mAdapter.getItemCount();
                for (int i = 0; i < count; i++) {
                    GroupListItem data = mAdapter.getItem(i);
                    if (mGroupId == data.getGroupId()) {
                        FreemeGroupNameEditDialogFragment dailog = FreemeGroupNameEditDialogFragment.newInstanceForUpdate(
                                getActivity(), new AccountWithDataSet(data.getAccountName(),
                                        data.getAccountType(), data.getDataSet()),
                                GroupUtil.ACTION_UPDATE_GROUP, mGroupId,
                                data.getTitle());
                        dailog.show(false);
                        break;
                    }
                }
                break;
            case R.id.freeme_menu_send_message:
                FreemeGroupMemberLoader groupMemberLoader = new FreemeGroupMemberLoader(this,
                        new FreemeGroupMemberLoader.GroupMemberListLoadCallBack() {
                            @Override
                            public void onLoadFinish(Map<Long, FreemeGroupMemberLoader.ContactDataHelperClass> contactMap,
                                                     List<String> itemList) {
                                sendToGroup(contactMap, itemList, ContactsUtils.SCHEME_SMSTO,
                                        getString(R.string.menu_sendMessageOption));
                            }
                        });
                long[] array = groupSelectCounts.stream().mapToLong(t -> t.longValue()).toArray();
                groupMemberLoader.loaderData(array, ContactsUtils.SCHEME_SMSTO);
                break;
            case R.id.freeme_menu_group_delete:
                deleteGroup();
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.freeme_menu_select_all:
                if (groupSelectCounts.size() == mAdapter.getItemCount()) {
                    updateMenuTitle(item, R.string.menu_select_all);
                    updateCheckBoxState(false);
                } else {
                    updateMenuTitle(item, R.string.menu_select_none);
                    updateCheckBoxState(true);
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
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

    private void onCreateGroupMenuItemClicked() {
        // Select the account to create the group
        final Bundle extras = getActivity().getIntent().getExtras();
        final Account account = extras == null ? null :
                (Account) extras.getParcelable(ContactsContract.Intents.Insert.EXTRA_ACCOUNT);
        if (account == null) {
            selectAccountForNewGroup();
        } else {
            Activity activity = getActivity();
            if (activity instanceof FreemeGroupBrowseActivity) {
                final String dataSet = extras == null
                        ? null : extras.getString(ContactsContract.Intents.Insert.EXTRA_DATA_SET);
                final AccountWithDataSet accountWithDataSet = new AccountWithDataSet(
                        account.name, account.type, dataSet);
                ((FreemeGroupBrowseActivity) activity).onAccountChosen(
                        accountWithDataSet, /* extraArgs */ null);
            }
        }
    }

    private void selectAccountForNewGroup() {
        // This should never block because the DrawerFragment loads the accounts and the
        // "Create Label" item only exists when that loading finishes
        final List<AccountInfo> accounts = Futures.getUnchecked(AccountTypeManager.getInstance(getContext())
                .filterAccountsAsync(AccountTypeManager.AccountFilter.GROUPS_WRITABLE));

        if (accounts.isEmpty()) {
            // We shouldn't present the add group button if there are no writable accounts
            // but check it since it's possible we are started with an Intent.
            Toast.makeText(getContext(), R.string.groupCreateFailedToast,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Activity activity = getActivity();
        if (activity instanceof FreemeGroupBrowseActivity) {
            // If there is a single writable account, use it w/o showing a dialog.
            if (accounts.size() == 1) {
                ((FreemeGroupBrowseActivity) activity).onAccountChosen(
                        accounts.get(0).getAccount(), /* extraArgs */ null);
                return;
            } else {
                for (AccountInfo ai : accounts) {
                    if (AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE.equals(ai.getAccount().type)) {
                        ((FreemeGroupBrowseActivity) activity).onAccountChosen(
                                ai.getAccount(), null);
                        return;
                    }
                }
                Toast.makeText(getContext(), R.string.groupCreateFailedToast,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mGroupListLoaderListener =
            new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public CursorLoader onCreateLoader(int id, Bundle args) {
                    return new GroupListLoader(getContext(), true);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    if (data == null || data.getCount() <= 0) {
                        mRecyclerView.setVisibility(View.GONE);
                        mEmptyView.setVisibility(View.VISIBLE);
                        return;
                    }
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                    groupListItems = new ArrayList<>();
                    ///M:[Google Issue][ALPS03432998]Must move cursur to -1 before moveToNext()@{
                    data.moveToPosition(-1);
                    /// @{
                    for (int i = 0; i < data.getCount(); i++) {
                        if (data.moveToNext()) {
                            groupListItems.add(GroupUtil.getGroupListItem(data, i));
                        }
                    }

                    notifyIfReady(groupListItems);
                }

                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private void notifyIfReady(List<GroupListItem> groupListItems) {
        final Iterator<GroupListItem> iterator = groupListItems.iterator();
        while (iterator.hasNext()) {
            final GroupListItem groupListItem = iterator.next();
            if (GroupUtil.isEmptyFFCGroup(groupListItem)) {
                iterator.remove();
            }
        }

        mAdapter.setGroupListItems(groupListItems);
        mAdapter.notifyDataSetChanged();

        groupListItems.clear();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private String getSubTitle(Intent intent) {
        String title = null;
        if (intent != null) {
            title = intent.getStringExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT);
        }
        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.freeme_navigate_title_from_contacts);
        }

        return title;
    }
}
