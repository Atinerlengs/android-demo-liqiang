/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.preference.ContactsPreferences;

import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.aassne.SimAasEditor;
import com.mediatek.contacts.aassne.SimAasSneUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.provider.MtkContactsContract;
import com.mediatek.provider.MtkContactsContract.Aas;

import java.util.ArrayList;
import java.util.List;

//*/freeme.zhangjunjian, 20180122. redesign contact editors
import android.provider.ContactsContract;
//*/
/**
 * Custom view for an entire section of data as segmented by
 * {@link DataKind} around a {@link Data#MIMETYPE}. This view shows a
 * section header and a trigger for adding new {@link Data} rows.
 */
public class KindSectionView extends LinearLayout {
    private static final String TAG = "KindSectionView";

    /**
     * Marks a name as super primary when it is changed.
     *
     * This is for the case when two or more raw contacts with names are joined where neither is
     * marked as super primary.
     */
    private static final class StructuredNameEditorListener implements Editor.EditorListener {

        private final ValuesDelta mValuesDelta;
        private final long mRawContactId;
        private final RawContactEditorView.Listener mListener;

        public StructuredNameEditorListener(ValuesDelta valuesDelta, long rawContactId,
                RawContactEditorView.Listener listener) {
            mValuesDelta = valuesDelta;
            mRawContactId = rawContactId;
            mListener = listener;
        }

        @Override
        public void onRequest(int request) {
            if (request == Editor.EditorListener.FIELD_CHANGED) {
                mValuesDelta.setSuperPrimary(true);
                if (mListener != null) {
                    mListener.onNameFieldChanged(mRawContactId, mValuesDelta);
                }
            } else if (request == Editor.EditorListener.FIELD_TURNED_EMPTY) {
                mValuesDelta.setSuperPrimary(false);
            }
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            editor.clearAllFields();
        }
    }

    /**
     * Clears fields when deletes are requested (on phonetic and nickename fields);
     * does not change the number of editors.
     */
    private static final class OtherNameKindEditorListener implements Editor.EditorListener {

        @Override
        public void onRequest(int request) {
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            editor.clearAllFields();
        }
    }

    /**
     * Updates empty fields when fields are deleted or turns empty.
     * Whether a new empty editor is added is controlled by {@link #setShowOneEmptyEditor} and
     * {@link #setHideWhenEmpty}.
     */
    private class NonNameEditorListener implements Editor.EditorListener {

        @Override
        public void onRequest(int request) {
            // If a field has become empty or non-empty, then check if another row
            // can be added dynamically.
            /*/ freeme.liqiang, 20180409. refresh menu item when modify kindSectionView
            if (request == FIELD_TURNED_EMPTY || request == FIELD_TURNED_NON_EMPTY) {
            /*/
            if (request == FIELD_TURNED_EMPTY || request == FIELD_TURNED_NON_EMPTY
                    || request == FIELD_CHANGED) {
            //*/
                /*/freeme.zhangjunjian, 20180118. redesign contact editors
                updateEmptyEditors(/ * shouldAnimate = * / true);
                //*/
                //*/ freeme.liqiang, 20180403. refresh menu item when edit contacts
                if (mListener != null) {
                    mListener.onNonNameEditorChanged();
                }
                //*/
            }
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            /* M: [Sim Contact Flow][AAS] when delete AAS editor, just clear its content.
             * Original code: @{
            if (mShowOneEmptyEditor && mEditors.getChildCount() == 1) {
             * @}
             * New code: @{ */
            /*/freeme.zhangjunjian, 20180118. redesign contact editors
             if ((mShowOneEmptyEditor && mEditors.getChildCount() == 1)
                    || GlobalEnv.getSimAasEditor().updateView(
                            mKindSectionData.getRawContactDelta(),
                            null, null, SimAasEditor.VIEW_UPDATE_DELETE_EDITOR)) {
            /*/
            if (GlobalEnv.getSimAasEditor().updateView(
                            mKindSectionData.getRawContactDelta(),
                            null, null, SimAasEditor.VIEW_UPDATE_DELETE_EDITOR)) {
            //*/
             /* @} */
                // If there is only 1 editor in the section, then don't allow the user to
                // delete it.  Just clear the fields in the editor.
                editor.clearAllFields();
            } else {
                editor.deleteEditor();
                //*/freeme.zhangjunjian, 20180207. redesign contact editors
                if (mEditors.getChildCount() > 1) {
                    mAddField.setBackgroundResource(R.drawable.freeme_content_bottom_bg_selector);
                } else {
                    mAddField.setBackgroundResource(R.drawable.freeme_content_full_bg_selector);
                }
                //*/
            }
        }
    }

    private class EventEditorListener extends NonNameEditorListener {

        @Override
        public void onRequest(int request) {
            super.onRequest(request);
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            if (editor instanceof EventFieldEditorView){
                final EventFieldEditorView delView = (EventFieldEditorView) editor;
                if (delView.isBirthdayType() && mEditors.getChildCount() > 1) {
                    final EventFieldEditorView bottomView = (EventFieldEditorView) mEditors
                            .getChildAt(mEditors.getChildCount() - 1);
                    bottomView.restoreBirthday();
                }
            }
            super.onDeleteRequested(editor);
        }
    }

    private KindSectionData mKindSectionData;
    private ViewIdGenerator mViewIdGenerator;
    private RawContactEditorView.Listener mListener;

    private boolean mIsUserProfile;
    private boolean mShowOneEmptyEditor = false;
    private boolean mHideIfEmpty = true;

    private LayoutInflater mLayoutInflater;
    private ViewGroup mEditors;
    /*/freeme.zhangjunjian, 20180117. redesign contact editor
    private ImageView mIcon;
    //*/
    //*/freeme.zhangjunjian, 20180207. redesign contact editors
    private TextView mAddFieldBtn;
    private boolean mNewFieldNext;
    private LinearLayout mAddField;
    private String mimeType;
    private String mTitle;
    private int mSubid = -1;
    //*/

    public KindSectionView(Context context) {
        this(context, /* attrs =*/ null);
    }

    public KindSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEditors != null) {
            int childCount = mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                mEditors.getChildAt(i).setEnabled(enabled);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mLayoutInflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mEditors = (ViewGroup) findViewById(R.id.kind_editors);
        /*/freeme.zhangjunjian, 20180117. redesign contact editor
        mIcon = (ImageView) findViewById(R.id.kind_icon);
        //*/
        //*/freeme.zhangjunjian, 20180207. redesign contact editors
        mAddFieldBtn = (TextView) findViewById(R.id.add_field_button);
        mAddField = (LinearLayout) findViewById(R.id.freeme_add_field);
        mAddField.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addEmptyItem(true);
            }
        });
        //*/
    }

    public void setIsUserProfile(boolean isUserProfile) {
        mIsUserProfile = isUserProfile;
    }

    /**
     * @param showOneEmptyEditor If true, we will always show one empty editor, otherwise an empty
     *         editor will not be shown until the user enters a value.  Note, this does not apply
     *         to name editors since those are always displayed.
     */
    public void setShowOneEmptyEditor(boolean showOneEmptyEditor) {
        mShowOneEmptyEditor = showOneEmptyEditor;
    }

    /**
     * @param hideWhenEmpty If true, the entire section will be hidden if all inputs are empty,
     *         otherwise one empty input will always be displayed.  Note, this does not apply
     *         to name editors since those are always displayed.
     */
    public void setHideWhenEmpty(boolean hideWhenEmpty) {
        mHideIfEmpty = hideWhenEmpty;
    }

    /** Binds the given group data to every {@link GroupMembershipView}. */
    public void setGroupMetaData(Cursor cursor) {
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            final View view = mEditors.getChildAt(i);
            if (view instanceof GroupMembershipView) {
                ((GroupMembershipView) view).setGroupMetaData(cursor);
            }
        }
    }

    /**
     * Whether this is a name kind section view and all name fields (structured, phonetic,
     * and nicknames) are empty.
     */
    public boolean isEmptyName() {
        if (!StructuredName.CONTENT_ITEM_TYPE.equals(mKindSectionData.getMimeType())) {
            return false;
        }
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            final View view = mEditors.getChildAt(i);
            if (view instanceof Editor) {
                final Editor editor = (Editor) view;
                if (!editor.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public StructuredNameEditorView getNameEditorView() {
        if (!StructuredName.CONTENT_ITEM_TYPE.equals(mKindSectionData.getMimeType())
            || mEditors.getChildCount() == 0) {
            return null;
        }
        return (StructuredNameEditorView) mEditors.getChildAt(0);
    }

    /**
     * Binds views for the given {@link KindSectionData}.
     *
     * We create a structured name and phonetic name editor for each {@link DataKind} with a
     * {@link StructuredName#CONTENT_ITEM_TYPE} mime type.  The number and order of editors are
     * rendered as they are given to {@link #setState}.
     *
     * Empty name editors are never added and at least one structured name editor is always
     * displayed, even if it is empty.
     */
    public void setState(KindSectionData kindSectionData,
            ViewIdGenerator viewIdGenerator, RawContactEditorView.Listener listener) {
        Log.d(TAG, "[setState] beg");
        mKindSectionData = kindSectionData;
        mViewIdGenerator = viewIdGenerator;
        mListener = listener;

        // Set the icon using the DataKind
        final DataKind dataKind = mKindSectionData.getDataKind();
        /*/freeme.zhangjunjian, 20180117. redesign contact editor
        if (dataKind != null) {
            mIcon.setImageDrawable(EditorUiUtils.getMimeTypeDrawable(getContext(),
                    dataKind.mimeType));
            if (mIcon.getDrawable() != null) {
                mIcon.setContentDescription(dataKind.titleRes == -1 || dataKind.titleRes == 0
                        ? "" : getResources().getString(dataKind.titleRes));
            }
        }
        /*/
        final String mimeType = mKindSectionData.getMimeType();

        if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)
                || GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)
                || Nickname.CONTENT_ITEM_TYPE.equals(mimeType)
                || ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
            setVisibility(GONE);
        } else {
            mTitle = (dataKind.titleRes == -1 || dataKind.titleRes == 0)
                    ? ""
                    : getResources().getString(dataKind.titleRes);
            String label = getResources().getString(R.string.freeme_add_new_entry_for_section);
            label = String.format(label, mTitle);
            mAddFieldBtn.setText(label);
        }
        //*/

        rebuildFromState();

        //*/ freeme.liqiang, 20180702. add empty item when click from phone or email card
        if (mimeType.equals(mEntryType)) {
            addEmptyItem(false);
        }
        //*/

        /*/freeme.zhangjunjian, 20180124. redesign contact editors
        updateEmptyEditors(/* shouldAnimate = * / false);
        //*/
        Log.d(TAG, "[setState] end");
    }

    private void rebuildFromState() {
        Log.d(TAG, "[rebuildFromState] beg");
        mEditors.removeAllViews();

        final String mimeType = mKindSectionData.getMimeType();
        if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
            addNameEditorViews(mKindSectionData.getAccountType(),
                    mKindSectionData.getRawContactDelta());
        /*/freeme.zhangjunjian, 20180129. redesign contact editors
        } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
            addGroupEditorView(mKindSectionData.getRawContactDelta(),
                    mKindSectionData.getDataKind());
        //*/
        } else {
            final Editor.EditorListener editorListener;
            if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                editorListener = new OtherNameKindEditorListener();
            } else if (Event.CONTENT_ITEM_TYPE.equals(mimeType)) {
                editorListener = new EventEditorListener();
            } else {
                editorListener = new NonNameEditorListener();
            }
            final List<ValuesDelta> valuesDeltas = mKindSectionData.getVisibleValuesDeltas();
            for (int i = 0; i < valuesDeltas.size(); i++ ) {
                addNonNameEditorView(mKindSectionData.getRawContactDelta(),
                        mKindSectionData.getDataKind(), valuesDeltas.get(i), editorListener);
            }
            //*/freeme.zhangjunjian, 20180130. redesign contact editors
            if (mEditors.getChildCount() > 0) {
                mEditors.setVisibility(VISIBLE);
            }
            //*/
        }
        Log.d(TAG, "[rebuildFromState] end");
    }

    private void addNameEditorViews(AccountType accountType, RawContactDelta rawContactDelta) {
        final boolean readOnly = !accountType.areContactsWritable();
        final ValuesDelta nameValuesDelta = rawContactDelta
                .getSuperPrimaryEntry(StructuredName.CONTENT_ITEM_TYPE);
        Log.d(TAG, "[addNameEditorViews] nameValuesDelta=" + nameValuesDelta
                + ", readOnly = " + readOnly);

        if (readOnly) {
            final View nameView = mLayoutInflater.inflate(
                    R.layout.structured_name_readonly_editor_view, mEditors,
                    /* attachToRoot =*/ false);

            // Display name
            ((TextView) nameView.findViewById(R.id.display_name))
                    .setText(nameValuesDelta.getDisplayName());

            // Account type info
            final LinearLayout accountTypeLayout = (LinearLayout)
                    nameView.findViewById(R.id.account_type);
            accountTypeLayout.setVisibility(View.VISIBLE);
            ((ImageView) accountTypeLayout.findViewById(R.id.account_type_icon))
                    .setImageDrawable(accountType.getDisplayIcon(getContext()));
            ((TextView) accountTypeLayout.findViewById(R.id.account_type_name))
                    .setText(accountType.getDisplayLabel(getContext()));

            mEditors.addView(nameView);
            return;
        }

        /*/freeme.zhangjunjian, 20180117. redesign contact editor
        // Structured name
        final StructuredNameEditorView nameView = (StructuredNameEditorView) mLayoutInflater
                .inflate(R.layout.structured_name_editor_view, mEditors, / * attachToRoot =* / false);
        if (!mIsUserProfile) {
            // Don't set super primary for the me contact
            nameView.setEditorListener(new StructuredNameEditorListener(
                    nameValuesDelta, rawContactDelta.getRawContactId(), mListener));
        }
        nameView.setDeletable(false);
        nameView.setValues(accountType.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_NAME),
                nameValuesDelta, rawContactDelta, / * readOnly =* / false, mViewIdGenerator);

        // Correct start margin since there is a second icon in the structured name layout
        nameView.findViewById(R.id.kind_icon).setVisibility(View.GONE);
        mEditors.addView(nameView);
        //*/

        // Phonetic name
        final DataKind phoneticNameKind = accountType
                .getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME);
        // The account type doesn't support phonetic name.
        if (phoneticNameKind == null) return;

        final TextFieldsEditorView phoneticNameView = (TextFieldsEditorView) mLayoutInflater
                .inflate(R.layout.text_fields_editor_view, mEditors, /* attachToRoot =*/ false);
        phoneticNameView.setEditorListener(new OtherNameKindEditorListener());
        phoneticNameView.setDeletable(false);
        phoneticNameView.setValues(
                accountType.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME),
                nameValuesDelta, rawContactDelta, /* readOnly =*/ false, mViewIdGenerator);

        // Fix the start margin for phonetic name views
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 0);
        phoneticNameView.setLayoutParams(layoutParams);
        mEditors.addView(phoneticNameView);
        // Display of phonetic name fields is controlled from settings preferences.
        mHideIfEmpty = new ContactsPreferences(getContext()).shouldHidePhoneticNamesIfEmpty();
    }

    private void addGroupEditorView(RawContactDelta rawContactDelta, DataKind dataKind) {
        Log.d(TAG, "[addGroupEditorView] beg");
        final GroupMembershipView view = (GroupMembershipView) mLayoutInflater.inflate(
                R.layout.item_group_membership, mEditors, /* attachToRoot =*/ false);
        view.setKind(dataKind);
        view.setEnabled(isEnabled());
        view.setState(rawContactDelta);

        // Correct start margin since there is a second icon in the group layout
        /*/freeme.zhangjunjian, 20180117. redesign contact editor
        view.findViewById(R.id.kind_icon).setVisibility(View.GONE);
        /*/
        if (mNewFieldNext) {
            view.requestFocus();
        }
        if (!mIsUserProfile) {
            view.setEnabled(true);
        }
        mEditors.setVisibility(VISIBLE);
        //*/
        mEditors.addView(view);
        Log.d(TAG, "[addGroupEditorView] end");
    }

    private View addNonNameEditorView(RawContactDelta rawContactDelta, DataKind dataKind,
            ValuesDelta valuesDelta, Editor.EditorListener editorListener) {
        // Inflate the layout
        //*/freeme.zhangjunjian, 20180130. redesign contact editors
        boolean isGroupMember = GroupMembership.CONTENT_ITEM_TYPE.equals(
                mKindSectionData.getMimeType());
        if (isGroupMember) {
            return null;
        }
        //*/
        final View view = mLayoutInflater.inflate(
                EditorUiUtils.getLayoutResourceId(dataKind.mimeType), mEditors, false);
        view.setEnabled(isEnabled());
        if (view instanceof Editor) {
            final Editor editor = (Editor) view;
            editor.setDeletable(true);
            editor.setEditorListener(editorListener);
            editor.setValues(dataKind, valuesDelta, rawContactDelta, !dataKind.editable,
                    mViewIdGenerator);
        }
        mEditors.addView(view);

        return view;
    }

    /**
     * Updates the editors being displayed to the user removing extra empty
     * {@link Editor}s, so there is only max 1 empty {@link Editor} view at a time.
     * If there is only 1 empty editor and {@link #setHideWhenEmpty} was set to true,
     * then the entire section is hidden.
     */
    public void updateEmptyEditors(boolean shouldAnimate) {
        final boolean isNameKindSection = StructuredName.CONTENT_ITEM_TYPE.equals(
                mKindSectionData.getMimeType());
        final boolean isGroupKindSection = GroupMembership.CONTENT_ITEM_TYPE.equals(
                mKindSectionData.getMimeType());
        Log.d(TAG, "[updateEmptyEditors] isNameKindSection = " + isNameKindSection
                + ", isGroupKindSection = " + isGroupKindSection
                + ", mHideIfEmpty = " + mHideIfEmpty);
        if (isNameKindSection) {
            // The name kind section is always visible
            setVisibility(VISIBLE);
            updateEmptyNameEditors(shouldAnimate);
            //*/freeme.zhangjunjian, 20180122. redesign contact editors
            mAddField.setVisibility(GONE);
            //*/
        } else if (isGroupKindSection) {
            // Check whether metadata has been bound for all group views
            for (int i = 0; i < mEditors.getChildCount(); i++) {
                final View view = mEditors.getChildAt(i);
                if (view instanceof GroupMembershipView) {
                    final GroupMembershipView groupView = (GroupMembershipView) view;
                    /// M: [Google Issue] Fix CR ALPS02813651.
                    /// add/edit profile shouldn't contain group view. @{
                    if (!groupView.wasGroupMetaDataBound()
                            || !groupView.accountHasGroups() || mIsUserProfile) {
                    /// @}
                        setVisibility(GONE);
                        return;
                    }
                }
            }
            // Check that the user has selected to display all fields
            if (mHideIfEmpty) {
                setVisibility(GONE);
                return;
            }
            setVisibility(VISIBLE);

            // We don't check the emptiness of the group views
        } else {
            // Determine if the entire kind section should be visible
            final int editorCount = mEditors.getChildCount();
            final List<View> emptyEditors = getEmptyEditors();
            if (editorCount == emptyEditors.size() && mHideIfEmpty) {
                setVisibility(GONE);
                return;
            }
            setVisibility(VISIBLE);

            updateEmptyNonNameEditors(shouldAnimate);
        }
    }

    private void updateEmptyNameEditors(boolean shouldAnimate) {
        boolean isEmptyNameEditorVisible = false;

        for (int i = 0; i < mEditors.getChildCount(); i++) {
            final View view = mEditors.getChildAt(i);
            if (view instanceof Editor) {
                final Editor editor = (Editor) view;
                if (view instanceof StructuredNameEditorView) {
                    // We always show one empty structured name view
                    /*/freeme.zhangjunjian, 20180122. redesign contact editors
                    if (editor.isEmpty()) {
                        if (isEmptyNameEditorVisible) {
                            // If we're already showing an empty editor then hide any other empties
                            if (mHideIfEmpty) {
                                view.setVisibility(View.GONE);
                            }
                        } else {
                            isEmptyNameEditorVisible = true;
                        }
                    } else {
                        showView(view, shouldAnimate);
                        isEmptyNameEditorVisible = true;
                    }
                    //*/
                } else {
                    // Since we can't add phonetic names and nicknames, just show or hide them
                    if (mHideIfEmpty && editor.isEmpty()) {
                        hideView(view);
                    } else {
                        showView(view, /* shouldAnimate =*/ false); // Animation here causes jank
                        /// M: [Sim Contact Flow] ALPS02703604.
                        /// no need show PhonicName fields for iccAccount @{
                        Log.d(TAG, "[updateEmptyNameEditors] is dismiss Phonetic Name fields : " +
                                mIsIccAccount);
                        if (mIsIccAccount) {
                            view.setVisibility(View.GONE);
                        }
                        /// @}
                    }
                }
            } else {
                // For read only names, only show them if we're not hiding empty views
                if (mHideIfEmpty) {
                    hideView(view);
                } else {
                    showView(view, shouldAnimate);
                }
            }
        }
    }

    private void updateEmptyNonNameEditors(boolean shouldAnimate) {
        // Prune excess empty editors
        final List<View> emptyEditors = getEmptyEditors();

        /* M: [Sim Contact Flow][AAS] sim contact support phone count
         * Original code: @{
        if (emptyEditors.size() > 1) {
         * @}
         * New code: @{ */
        int max = 1;
        if (mKindSectionData.getMimeType() != null) {
            max = GlobalEnv.getSimAasEditor().getMaxEmptyEditors(
                    mKindSectionData.getRawContactDelta(),
                    mKindSectionData.getMimeType());
            String accountType = mKindSectionData.getRawContactDelta().getAccountType();
            if (AccountTypeUtils.isUsimOrCsim(accountType)
                    && AccountTypeUtils.isPhoneNumType(mKindSectionData.getMimeType())) {
                mKindSectionData.getDataKind().typeOverallMax = max;
                Log.d(TAG, "[updateEmptyNonNameEditors] set aas typeOverallMax = max = " + max);
            }
        }
        Log.d(TAG, "[updateEmptyNonNameEditors] max =" + max + " emptyEditors.size()="
                + emptyEditors.size() + ", mEditors=" + mEditors.getChildCount());
        /*/freeme.zhangjunjian, 20180127. redesign contact editors
        if (emptyEditors.size() > max) {
        /* @} * /
            // If there is more than 1 empty editor, then remove it from the list of editors.
            int deleted = 0;
            for (int i = 0; i < emptyEditors.size(); i++) {
                final View view = emptyEditors.get(i);
                // If no child {@link View}s are being focused on within this {@link View}, then
                // remove this empty editor. We can assume that at least one empty editor has
                // focus. One way to get two empty editors is by deleting characters from a
                // non-empty editor, in which case this editor has focus.  Another way is if
                // there is more values delta so we must also count number of editors deleted.
                if (view.findFocus() == null) {
                    deleteView(view, shouldAnimate);
                    deleted++;
                    if (deleted == emptyEditors.size() - 1) break;
                }
            }
            return;
        }
        //*/
        // Determine if we should add a new empty editor
        final DataKind dataKind = mKindSectionData.getDataKind();
        final RawContactDelta rawContactDelta = mKindSectionData.getRawContactDelta();
        if (dataKind == null // There is nothing we can do.
                // We have already reached the maximum number of editors, don't add any more.
                || !RawContactModifier.canInsert(rawContactDelta, dataKind)
                // We have already reached the maximum number of empty editors, don't add any more.
                /*/freeme.zhangjunjian, 20180127. redesign contact editors
                || emptyEditors.size() == 1) {
                /*/
                ) {
                //*/
            return;
        }
        // Add a new empty editor
        if (mShowOneEmptyEditor) {
            final String mimeType = mKindSectionData.getMimeType();
            if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType) && mEditors.getChildCount() > 0) {
                return;
            }
            final ValuesDelta values = RawContactModifier.insertChild(rawContactDelta, dataKind);
            /// M: [ALPS03309591] check if convert to AAS@{
            if (values != null && values.containsKey("data2")) {
                if (values.getAsInteger("data2", -1) == Aas.PHONE_TYPE_AAS) {
                    Log.d(TAG, "[updateEmptyNonNameEditors] Convert to Aas:" + values.toString());
                    values.put(MtkContactsContract.DataColumns.IS_ADDITIONAL_NUMBER, 1);
                }
            }
            /// @}
            final Editor.EditorListener editorListener = Event.CONTENT_ITEM_TYPE.equals(mimeType)
                    ? new EventEditorListener() : new NonNameEditorListener();
            final View view = addNonNameEditorView(rawContactDelta, dataKind, values,
                    editorListener);
            showView(view, shouldAnimate);
        }
    }

    private void hideView(View view) {
        view.setVisibility(View.GONE);
    }

    private void deleteView(View view, boolean shouldAnimate) {
        if (shouldAnimate) {
            final Editor editor = (Editor) view;
            editor.deleteEditor();
        } else {
            mEditors.removeView(view);
        }
    }

    private void showView(View view, boolean shouldAnimate) {
        if (shouldAnimate) {
            view.setVisibility(View.GONE);
            EditorAnimator.getInstance().showFieldFooter(view);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    private List<View> getEmptyEditors() {
        final List<View> emptyEditors = new ArrayList<>();
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            final View view = mEditors.getChildAt(i);
            if (view instanceof Editor && ((Editor) view).isEmpty()) {
                emptyEditors.add(view);
            }
        }
        return emptyEditors;
    }

    /// M: ALPS02703604. no need show PhoneticName fields for iccAccount @{
    private boolean mIsIccAccount = false;

    public void setIsCurrentIccAccount(boolean isIccAccount) {
        mIsIccAccount = isIccAccount;
    }
    /// @}

    //*/freeme.zhangjunjian, 20180207. redesign contact editors
    public void setMimeType(String setMimeType) {
        mimeType = setMimeType;
    }

    public void setSubId(int subid) {
        mSubid = subid;
        mAddField.setVisibility(mSubid == -1 ? VISIBLE : GONE);
    }
    //*/

    //*/ freeme.liqiang, 20180702. add empty item when click from phone or email card
    private String mEntryType;

    public void setEntryType(String entryType) {
        mEntryType = entryType;
    }

    private void addEmptyItem(boolean shouldAnimate) {
        mNewFieldNext = true;
        mEditors.setVisibility(VISIBLE);
        mAddField.setBackgroundResource(R.drawable.freeme_content_bottom_bg_selector);
        updateEmptyEditors(shouldAnimate);
        int count = mEditors.getChildCount();
        mEditors.getChildAt(count - 1).requestFocus();
    }
    //*/
}
