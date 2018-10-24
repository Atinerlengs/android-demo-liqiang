/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, softwareateCre
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.freeme.contacts.group;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.model.account.AccountWithDataSet;
import com.google.common.base.Strings;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Edits the name of a group.
 */
public final class FreemeGroupNameEditDialogFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "GroupNameEditDialogFragment";

    private static final String ARG_IS_INSERT = "isInsert";
    private static final String ARG_GROUP_NAME = "groupName";
    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_CALLBACK_ACTION = "callbackAction";
    private static final String ARG_GROUP_ID = "groupId";

    private static final long NO_GROUP_ID = -1;


    /**
     * Callbacks for hosts of the {@link FreemeGroupNameEditDialogFragment}.
     */
    public interface Listener {
        void onGroupNameEditCancelled();

        void onGroupNameEditCompleted(String name);

        public static final Listener None = new Listener() {
            @Override
            public void onGroupNameEditCancelled() {
            }

            @Override
            public void onGroupNameEditCompleted(String name) {
            }
        };
    }

    private boolean mIsInsert;
    private String mGroupName;
    private long mGroupId;
    private Listener mListener;
    private AccountWithDataSet mAccount;

    private static Activity mActivity;
    private static final String FRAGMENT_TAG = "renameDeleteContact";

    private EditText mGroupNameEditText;
    private TextView mGroupNameTextCounter;
    private Set<String> mExistingGroups = Collections.emptySet();

    /// M: [Sim Group] sub id for sim group @{
    private int mSubId = SubInfoUtils.getInvalidSubId();
    private AlertDialog mAlertDialog;

    ///M:[ALPS03468759]
    public int getSubId() {
        return mSubId;
    }

    public static FreemeGroupNameEditDialogFragment newInstanceForCreation(
            Activity activity, AccountWithDataSet account, String callbackAction) {
        return newInstance(activity, account, callbackAction, NO_GROUP_ID, null);
    }

    public static FreemeGroupNameEditDialogFragment newInstanceForUpdate(
            Activity activity, AccountWithDataSet account, String callbackAction, long groupId, String groupName) {
        return newInstance(activity, account, callbackAction, groupId, groupName);
    }

    private static FreemeGroupNameEditDialogFragment newInstance(
            Activity activity, AccountWithDataSet account, String callbackAction, long groupId, String groupName) {
        if (account == null || account.name == null || account.type == null) {
            throw new IllegalArgumentException("Invalid account");
        }
        mActivity = activity;
        if (activity.isDestroyed()) {
            return null;
        }
        mActivity = activity;
        FragmentManager fragmentManager = activity.getFragmentManager();
        FreemeGroupNameEditDialogFragment fragment = new FreemeGroupNameEditDialogFragment();
        fragmentManager.beginTransaction().add(fragment, FRAGMENT_TAG)
                .commitAllowingStateLoss();

        final boolean isInsert = groupId == NO_GROUP_ID;
        final Bundle args = new Bundle();
        args.putBoolean(ARG_IS_INSERT, isInsert);
        args.putLong(ARG_GROUP_ID, groupId);
        args.putString(ARG_GROUP_NAME, groupName);
        args.putParcelable(ARG_ACCOUNT, account);
        args.putString(ARG_CALLBACK_ACTION, callbackAction);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (savedInstanceState == null) {
            mGroupName = args.getString(ARG_GROUP_NAME);
        } else {
            mGroupName = savedInstanceState.getString(ARG_GROUP_NAME);
        }

        mGroupId = args.getLong(ARG_GROUP_ID, NO_GROUP_ID);
        mIsInsert = args.getBoolean(ARG_IS_INSERT, true);
        mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        mSubId = AccountTypeUtils.getSubIdBySimAccountName(getActivity(), mAccount.name);
        getLoaderManager().initLoader(0, null, this);
    }

    public void show(boolean createNew) {
        mIsInsert = createNew;
        final int max_text_count = mActivity.getResources().getInteger(R.integer.group_name_max_length);

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setTitle(createNew
                        ? R.string.freeme_group_name_dialog_insert_title
                        : R.string.freeme_group_name_dialog_update_title)
                .setView(R.layout.freeme_group_name_edit_dialog)
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideInputMethod();
                        getListener().onGroupNameEditCancelled();
                        mAlertDialog.dismiss();
                    }
                })
                .setPositiveButton(android.R.string.ok, null);

        mAlertDialog = builder.create();
        mAlertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        /// @}
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ///M:[Google Issue][ALPS03468617] onShow must after added. @{
                if (!isAdded()) {
                    Log.e(TAG, "[onShow] Fragment not added yet !!!");
                    try {
                        mAlertDialog.dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Error dismissing group name edit dialog", e);
                    }
                    return;
                }
                /// @}
                mGroupNameEditText = (EditText) mAlertDialog.findViewById(android.R.id.text1);
                mGroupNameTextCounter = (TextView) mAlertDialog.findViewById(R.id.freeme_group_name_text_counter);
                mGroupNameTextCounter.setText(getString(R.string.freeme_group_name_text_limit, 0, max_text_count));
                if (!TextUtils.isEmpty(mGroupName)) {
                    mGroupNameEditText.setText(mGroupName);
                    // Guard against already created group names that are longer than the max
                    final int maxLength = getResources().getInteger(
                            R.integer.group_name_max_length);
                    mGroupNameEditText.setSelection(
                            mGroupName.length() > maxLength ? maxLength : mGroupName.length());
                }
                showInputMethod(mGroupNameEditText);

                final Button createButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                createButton.setEnabled(!TextUtils.isEmpty(getGroupName()));

                // Override the click listener to prevent dismissal if creating a duplicate group.
                createButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        maybePersistCurrentGroupName(v);
                    }
                });
                mGroupNameEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s != null && s.length() > 0 && s.toString().trim().length() == 0) {
                            mGroupNameEditText.setText("");
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        int len = mGroupNameEditText.getText().length();
                        createButton.setEnabled(len > 0);
                        mGroupNameTextCounter.setText(getString(R.string.freeme_group_name_text_limit, len, max_text_count));
                    }
                });
            }
        });
        mAlertDialog.show();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.setOnDismissListener(null);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }


    /**
     * Sets the listener for the rename
     * <p>
     * Setting a listener on a fragment is error prone since it will be lost if the fragment
     * is recreated. This exists because it is used from a view class (GroupMembersView) which
     * needs to modify it's state when this fragment updates the name.
     *
     * @param listener the listener. can be null
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    private boolean hasNameChanged() {
        final String name = Strings.nullToEmpty(getGroupName());
        final String originalName = getArguments().getString(ARG_GROUP_NAME);
        return (mIsInsert && !name.isEmpty()) || !name.equals(originalName);
    }

    private void maybePersistCurrentGroupName(View button) {
        Log.d(TAG, "[maybePersistCurrentGroupName] mIsInsert is " + mIsInsert);
        /// M: [Sim Group] [Google Issue]ALPS00542175 @{
        if (!SimGroupUtils.checkServiceState(true, mSubId, getActivity())) {
            return;
        }
        /// @}
        if (!hasNameChanged()) {
            mAlertDialog.dismiss();
            return;
        }
        final String name = getGroupName();
        // Note we don't check if the loader finished populating mExistingGroups. It's not the
        // end of the world if the user ends up with a duplicate group and in practice it should
        // never really happen (the query should complete much sooner than the user can edit the
        // label)
        if (mExistingGroups.contains(name)) {
            Toast.makeText(mActivity, R.string.groupExistsErrorMessage, Toast.LENGTH_SHORT).show();
            button.setEnabled(false);
            return;
        }
        final String callbackAction = getArguments().getString(ARG_CALLBACK_ACTION);
        final Intent serviceIntent;
        Log.d(TAG, "[maybePersistCurrentGroupName]name=" + name + ", mSubId=" + mSubId);
        /// M: [Sim Group] @{
        if (mSubId > 0) {
            if (mIsInsert) {
                serviceIntent = SimGroupUtils.createNewGroupIntentForIcc(getActivity(),
                        mAccount, name, null, getActivity().getClass(), callbackAction,
                        null, mSubId);
            } else {
                serviceIntent = SimGroupUtils.createGroupRenameIntentForIcc(getActivity(),
                        mGroupId, name, getActivity().getClass(), callbackAction,
                        getArguments().getString(ARG_GROUP_NAME), mSubId);
            }
        } else { /// @}
            if (mIsInsert) {
                serviceIntent = ContactSaveService.createNewGroupIntent(getActivity(), mAccount,
                        name, null, getActivity().getClass(), callbackAction);
            } else {
                serviceIntent = ContactSaveService.createGroupRenameIntent(getActivity(), mGroupId,
                        name, getActivity().getClass(), callbackAction);
            }
        }
        ContactSaveService.startService(getActivity(), serviceIntent);
        getListener().onGroupNameEditCompleted(mGroupName);
        mAlertDialog.dismiss();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_GROUP_NAME, getGroupName());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Only a single loader so id is ignored.
        return new CursorLoader(getActivity(), Groups.CONTENT_SUMMARY_URI,
                new String[]{Groups.TITLE, Groups.SYSTEM_ID, Groups.ACCOUNT_TYPE,
                        Groups.SUMMARY_COUNT, Groups.GROUP_IS_READ_ONLY},
                getSelection(), getSelectionArgs(), null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mExistingGroups = new HashSet<>();
        final GroupUtil.GroupsProjection projection = new GroupUtil.GroupsProjection(data);
        /// M:[Google Issue][ALPS03459189] need reset cursor before use it @{
        data.moveToPosition(-1);
        /// @}
        while (data.moveToNext()) {
            final String title = projection.getTitle(data);
            // Empty system groups aren't shown in the nav drawer so it would be confusing to tell
            // the user that they already exist. Instead we allow them to create a duplicate
            // group in this case. This is how the web handles this case as well (it creates a
            // new non-system group if a new group with a title that matches a system group is
            // create).
            if (projection.isEmptyFFCGroup(data)) {
                continue;
            }
            mExistingGroups.add(title);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, /* flags */ 0);
        }
    }

    private void hideInputMethod() {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && mGroupNameEditText != null) {
            imm.hideSoftInputFromWindow(mGroupNameEditText.getWindowToken(), /* flags */ 0);
        }
    }

    private Listener getListener() {
        if (mListener != null) {
            return mListener;
        } else if (getActivity() instanceof Listener) {
            return (Listener) getActivity();
        } else {
            return Listener.None;
        }
    }

    private String getGroupName() {
        return mGroupNameEditText == null || mGroupNameEditText.getText() == null
                ? null : mGroupNameEditText.getText().toString();
    }

    private String getSelection() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Groups.ACCOUNT_NAME).append("=? AND ")
                .append(Groups.ACCOUNT_TYPE).append("=? AND ")
                .append(Groups.DELETED).append("=?");
        if (mAccount.dataSet != null) {
            builder.append(" AND ").append(Groups.DATA_SET).append("=?");
        }
        return builder.toString();
    }

    private String[] getSelectionArgs() {
        final int len = mAccount.dataSet == null ? 3 : 4;
        final String[] args = new String[len];
        args[0] = mAccount.name;
        args[1] = mAccount.type;
        args[2] = "0"; // Not deleted
        if (mAccount.dataSet != null) {
            args[3] = mAccount.dataSet;
        }
        return args;
    }
}
