package com.freeme.contacts.group;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentUris;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;

import com.android.contacts.R;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.activities.TransactionSafeActivity;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;
import com.freeme.contacts.common.utils.FreemeLogUtils;

public class FreemeGroupBrowseActivity extends TransactionSafeActivity implements
        SelectAccountDialogFragment.Listener,
        OnGroupListItemClickListener {

    private static final String TAG = "FreemeGroupBrowseActivity";
    private static final String TAG_SECOND_LEVEL = "second-level";

    public static final String FRAGMENT_TAG_GROUP_LIST = "FRAGMENT_TAG_GROUP_LIST";
    public static final String FRAGMENT_TAG_GROUP_MEMBER_LIST = "FRAGMENT_TAG_GROUP_MEMBER_LIST";

    private FreemeGroupListFragment mGroupListFragment;
    private FreemeGroupMembersListFragment mGroupMemberListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }

        setUpActionBar();

        getWindow().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);

        setContentView(R.layout.freeme_group_browse_activity);

        showOrHideFragment(true, FRAGMENT_TAG_GROUP_LIST);
    }

    private void setUpActionBar() {
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mGroupMemberListFragment != null && mGroupMemberListFragment.getAdapter().isDisplayingCheckBoxes()) {
            mGroupMemberListFragment.clearCheckBoxes();
            mGroupMemberListFragment.displayCheckBoxes(false);
            mGroupMemberListFragment.setSelectionMode(false);
        } else if (mGroupListFragment != null && mGroupListFragment.getAdapter().ismDispalyCheckBox()) {
            mGroupListFragment.getAdapter().setmDispalyCheckBox(false);
            mGroupListFragment.clearCheckBoxSelect();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof FreemeGroupListFragment) {
            mGroupListFragment = (FreemeGroupListFragment) fragment;
        } else if (fragment instanceof FreemeGroupMembersListFragment) {
            mGroupMemberListFragment = (FreemeGroupMembersListFragment) fragment;
        }
    }

    private Fragment createNewFragmentForTag(String tag) {
        if (FRAGMENT_TAG_GROUP_LIST.equals(tag)) {
            mGroupListFragment = new FreemeGroupListFragment();
            return mGroupListFragment;
        } else if (FRAGMENT_TAG_GROUP_MEMBER_LIST.equals(tag)) {
            mGroupMemberListFragment = FreemeGroupMembersListFragment.newInstance(mGroupUri,
                    mGroupId, getResources().getString(R.string.freeme_my_contacts_group));
            return mGroupMemberListFragment;
        }
        throw new IllegalStateException("Unexpected fragment: " + tag);
    }

    public void showOrHideFragment(boolean show, String tag) {
        final FragmentManager fm = getFragmentManager();
        if (fm == null) {
            FreemeLogUtils.w(TAG, "Fragment manager is null for : " + tag);
            return;
        }

        Fragment fragment = fm.findFragmentByTag(tag);
        if (!show && fragment == null) {
            // Nothing to show, so bail early.
            return;
        }

        final FragmentTransaction transaction = fm.beginTransaction();
        if (show) {
            if (fragment == null) {
                fragment = createNewFragmentForTag(tag);
                transaction.replace(R.id.main, fragment, tag);
                if (!FRAGMENT_TAG_GROUP_LIST.equals(tag)) {
                    transaction.addToBackStack(TAG_SECOND_LEVEL);
                }
            } else {
                transaction.show(fragment);
            }
        } else {
            transaction.hide(fragment);
        }

        transaction.commitAllowingStateLoss();
        fm.executePendingTransactions();
    }

    private static final String TAG_SELECT_ACCOUNT_DIALOG = "selectAccountDialog";
    private static final String TAG_GROUP_NAME_EDIT_DIALOG = "groupNameEditDialog";

    public void selectAccount() {
        SelectAccountDialogFragment.show(getFragmentManager(),
                R.string.dialog_new_group_account,
                AccountTypeManager.AccountFilter.GROUPS_WRITABLE,
                /* extraArgs */ null,
                TAG_SELECT_ACCOUNT_DIALOG);
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        FreemeGroupNameEditDialogFragment dialog = FreemeGroupNameEditDialogFragment.newInstanceForCreation(
                this, account,
                GroupUtil.ACTION_CREATE_GROUP);
        dialog.show(true);
    }

    @Override
    public void onAccountSelectorCancelled() {

    }

    private Uri mGroupUri;
    private long mGroupId;

    @Override
    public void onGroupItemClick(long groupId) {
        mGroupId = groupId;
        mGroupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId);

        final FragmentManager fm = getFragmentManager();
        if (fm == null) {
            FreemeLogUtils.w(TAG, "Fragment manager is null for : " + FRAGMENT_TAG_GROUP_MEMBER_LIST);
            return;
        }
        Fragment fragment = fm.findFragmentByTag(FRAGMENT_TAG_GROUP_MEMBER_LIST);
        if (fragment == null) {
            showOrHideFragment(true, FRAGMENT_TAG_GROUP_MEMBER_LIST);
        } else {
            if (fragment instanceof FreemeGroupMembersListFragment) {
                ((FreemeGroupMembersListFragment) fragment).updateExistingGroupFragment(
                        mGroupUri, GroupUtil.ACTION_SWITCH_GROUP);
            }
        }
    }
}
