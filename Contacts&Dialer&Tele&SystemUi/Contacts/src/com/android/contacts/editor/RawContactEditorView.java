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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.text.TextUtils;
import android.util.AttributeSet;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import com.android.contacts.GeoUtil;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.dataitem.CustomDataItem;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.UiClosables;

import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
//*/freeme.zhangjunjian, 20180122. redesign contact editors
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.Ringtone;
import android.widget.Toast;
import android.media.RingtoneManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
//*/

/**
 * View to display information from multiple {@link RawContactDelta}s grouped together.
 */
public class RawContactEditorView extends LinearLayout implements View.OnClickListener {

    static final String TAG = "RawContactEditorView";

    /**
     * Callbacks for hosts of {@link RawContactEditorView}s.
     */
    public interface Listener {

        /**
         * Invoked when the structured name editor field has changed.
         *
         * @param rawContactId The raw contact ID from the underlying {@link RawContactDelta}.
         * @param valuesDelta The values from the underlying {@link RawContactDelta}.
         */
        public void onNameFieldChanged(long rawContactId, ValuesDelta valuesDelta);

        /**
         * Invoked when the editor should rebind editors for a new account.
         *
         * @param oldState Old data being edited.
         * @param oldAccount Old account associated with oldState.
         * @param newAccount New account to be used.
         */
        public void onRebindEditorsForNewContact(RawContactDelta oldState,
                AccountWithDataSet oldAccount, AccountWithDataSet newAccount);

        /**
         * Invoked when no editors could be bound for the contact.
         */
        public void onBindEditorsFailed();

        /**
         * Invoked after editors have been bound for the contact.
         */
        public void onEditorsBound();

        //*/ freeme.liqiang, 20180403. refresh menu item when edit contacts
        public void onNonNameEditorChanged();

        public void onRingtoneChanged(Uri ringtotneUri);

        public void onGroupChanged();

        public void onPhotoDeleted();
        //*/
    }
    /**
     * Sorts kinds roughly the same as quick contacts; we diverge in the following ways:
     * <ol>
     *     <li>All names are together at the top.</li>
     *     <li>IM is moved up after addresses</li>
     *     <li>SIP addresses are moved to below phone numbers</li>
     *     <li>Group membership is placed at the end</li>
     * </ol>
     */
    private static final class MimeTypeComparator implements Comparator<String> {
        /*/freeme.zhangjunjian, 20180130. redesign contact editors
        private static final List<String> MIME_TYPE_ORDER = Arrays.asList(new String[] {
                StructuredName.CONTENT_ITEM_TYPE,
                Nickname.CONTENT_ITEM_TYPE,
                Organization.CONTENT_ITEM_TYPE,
                Phone.CONTENT_ITEM_TYPE,
                SipAddress.CONTENT_ITEM_TYPE,
                Email.CONTENT_ITEM_TYPE,
                StructuredPostal.CONTENT_ITEM_TYPE,
                Im.CONTENT_ITEM_TYPE,
                Website.CONTENT_ITEM_TYPE,
                Event.CONTENT_ITEM_TYPE,
                Relation.CONTENT_ITEM_TYPE,
                Note.CONTENT_ITEM_TYPE,
                GroupMembership.CONTENT_ITEM_TYPE
        });
        /*/
        private static final List<String> MIME_TYPE_ORDER = Arrays.asList(new String[] {
                StructuredName.CONTENT_ITEM_TYPE,
                Nickname.CONTENT_ITEM_TYPE,
                Phone.CONTENT_ITEM_TYPE,
                Email.CONTENT_ITEM_TYPE,
                Im.CONTENT_ITEM_TYPE,
                StructuredPostal.CONTENT_ITEM_TYPE,
                Organization.CONTENT_ITEM_TYPE,
                SipAddress.CONTENT_ITEM_TYPE,
                Website.CONTENT_ITEM_TYPE,
                Event.CONTENT_ITEM_TYPE,
                Relation.CONTENT_ITEM_TYPE,
                Note.CONTENT_ITEM_TYPE,
                GroupMembership.CONTENT_ITEM_TYPE
        });
        //*/

        @Override
        public int compare(String mimeType1, String mimeType2) {
            if (mimeType1 == mimeType2) return 0;
            if (mimeType1 == null) return -1;
            if (mimeType2 == null) return 1;

            int index1 = MIME_TYPE_ORDER.indexOf(mimeType1);
            int index2 = MIME_TYPE_ORDER.indexOf(mimeType2);

            // Fallback to alphabetical ordering of the mime type if both are not found
            if (index1 < 0 && index2 < 0) return mimeType1.compareTo(mimeType2);
            if (index1 < 0) return 1;
            if (index2 < 0) return -1;

            return index1 < index2 ? -1 : 1;
        }
    }

    public static class SavedState extends BaseSavedState {

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };

        private boolean mIsExpanded;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mIsExpanded = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mIsExpanded ? 1 : 0);
        }
    }

    private RawContactEditorView.Listener mListener;

    private AccountTypeManager mAccountTypeManager;
    private LayoutInflater mLayoutInflater;

    private ViewIdGenerator mViewIdGenerator;
    private MaterialColorMapUtils.MaterialPalette mMaterialPalette;
    private boolean mHasNewContact;
    private boolean mIsUserProfile;
    private AccountWithDataSet mPrimaryAccount;
    private List<AccountInfo> mAccounts = new ArrayList<>();
    private RawContactDeltaList mRawContactDeltas;
    private RawContactDelta mCurrentRawContactDelta;
    private long mRawContactIdToDisplayAlone = -1;
    private Map<String, KindSectionData> mKindSectionDataMap = new HashMap<>();
    private Set<String> mSortedMimetypes = new TreeSet<>(new MimeTypeComparator());

    // Account header
    private View mAccountHeaderContainer;
    private TextView mAccountHeaderPrimaryText;
    private TextView mAccountHeaderSecondaryText;
    /*/freeme.zhangjunjian, 20180116. redesign contact editors
    private ImageView mAccountHeaderIcon;
    //*/
    private ImageView mAccountHeaderExpanderIcon;

    private PhotoEditorView mPhotoView;
    private ViewGroup mKindSectionViews;
    private Map<String, KindSectionView> mKindSectionViewMap = new HashMap<>();
    /*/freeme.zhangjunjian, 20180116. redesign contact editors
    private View mMoreFields;
    //*/

    private boolean mIsExpanded;

    private Bundle mIntentExtras;

    private ValuesDelta mPhotoValuesDelta;
    //*/freeme.zhangjunjian, 20180116. redesign contact editors
    private LinearLayout mRingContact;
    private TextView mContactRing;
    private StructuredNameEditorView mName;
    private GroupMembershipView mGroup;
    private int mSubId = -1;
    private AlertDialog mDialog;
    //*/

    public RawContactEditorView(Context context) {
        super(context);
    }

    public RawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets the receiver for {@link RawContactEditorView} callbacks.
     */
    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAccountTypeManager = AccountTypeManager.getInstance(getContext());
        mLayoutInflater = (LayoutInflater)
                getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Account header
        mAccountHeaderContainer = findViewById(R.id.account_header_container);
        mAccountHeaderPrimaryText = (TextView) findViewById(R.id.account_type);
        mAccountHeaderSecondaryText = (TextView) findViewById(R.id.account_name);
        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        mAccountHeaderIcon = (ImageView) findViewById(R.id.account_type_icon);
        //*/
        mAccountHeaderExpanderIcon = (ImageView) findViewById(R.id.account_expander_icon);

        mPhotoView = (PhotoEditorView) findViewById(R.id.photo_editor);
        mKindSectionViews = (LinearLayout) findViewById(R.id.kind_section_views);
        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        mMoreFields = findViewById(R.id.more_fields);
        mMoreFields.setOnClickListener(this);
        /*/
        mRingContact = (LinearLayout) findViewById(R.id.ring_contact);
        mContactRing = (TextView) findViewById(R.id.ring);
        mName = (StructuredNameEditorView) findViewById(R.id.edit_name);
        mGroup =(GroupMembershipView) findViewById(R.id.group_membership_view);
        //*/
    }

    @Override
    public void onClick(View view) {
        /*/freeme.zhangjunjian, 20180119. redesign contact editors
        if (view.getId() == R.id.more_fields) {
            showAllFields();
        }
        //*/
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        final int childCount = mKindSectionViews.getChildCount();
        for (int i = 0; i < childCount; i++) {
            mKindSectionViews.getChildAt(i).setEnabled(enabled);
        }
        //*/freeme.zhangjunjian, 20180122. redesign contact editors
        if (mName != null) {
            mName.setEnabled(enabled);
            mName.setupExpansionView(false, false);
        }
        if (mGroup != null) {
            mGroup.setEnabled(enabled);
        }
        //*/
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Log.d(TAG, "[onSaveInstanceState] mAccountSelectorPopup = " + mAccountSelectorPopup);
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState savedState = new SavedState(superState);
        savedState.mIsExpanded = mIsExpanded;
        /*/freeme.zhangjunjian, 20180206. redesign contact editors
        / * M:[Google Issue] ALPS03746536 3/3.
         * enter/exit multi-window will callback onSaveInstanceState(). @{ * /
        UiClosables.closeQuietly(mAccountSelectorPopup);
        / * @} * /
        /*/
        UiClosables.closeDialogQuietly(mDialog);
        //*/
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mIsExpanded = savedState.mIsExpanded;
        if (mIsExpanded) {
            showAllFields();
        }
    }

    /**
     * Pass through to {@link PhotoEditorView#setListener}.
     */
    public void setPhotoListener(PhotoEditorView.Listener listener) {
        mPhotoView.setListener(listener);
    }

    public void removePhoto() {
        mPhotoValuesDelta.setFromTemplate(true);
        mPhotoValuesDelta.put(Photo.PHOTO, (byte[]) null);
        mPhotoValuesDelta.put(Photo.PHOTO_FILE_ID, (String) null);

        mPhotoView.removePhoto();
        //*/ freeme.liqiang, 20180427. refresh menu item when remove photo
        if (mListener != null) {
            mListener.onPhotoDeleted();
        }
        //*/
    }

    /**
     * Pass through to {@link PhotoEditorView#setFullSizedPhoto(Uri)}.
     */
    public void setFullSizePhoto(Uri photoUri) {
        mPhotoView.setFullSizedPhoto(photoUri);
    }

    public void updatePhoto(Uri photoUri) {
        mPhotoValuesDelta.setFromTemplate(false);
        // Unset primary for all photos
        unsetSuperPrimaryFromAllPhotos();
        // Mark the currently displayed photo as primary
        mPhotoValuesDelta.setSuperPrimary(true);

        // Even though high-res photos cannot be saved by passing them via
        // an EntityDeltaList (since they cause the Bundle size limit to be
        // exceeded), we still pass a low-res thumbnail. This simplifies
        // code all over the place, because we don't have to test whether
        // there is a change in EITHER the delta-list OR a changed photo...
        // this way, there is always a change in the delta-list.
        try {
            final byte[] bytes = EditorUiUtils.getCompressedThumbnailBitmapBytes(
                    getContext(), photoUri);
            if (bytes != null) {
                mPhotoValuesDelta.setPhoto(bytes);
            }
        } catch (FileNotFoundException e) {
            elog("Failed to get bitmap from photo Uri");
        }

        mPhotoView.setFullSizedPhoto(photoUri);
    }

    private void unsetSuperPrimaryFromAllPhotos() {
        for (int i = 0; i < mRawContactDeltas.size(); i++) {
            final RawContactDelta rawContactDelta = mRawContactDeltas.get(i);
            if (!rawContactDelta.hasMimeEntries(Photo.CONTENT_ITEM_TYPE)) {
                continue;
            }
            final List<ValuesDelta> photosDeltas =
                    mRawContactDeltas.get(i).getMimeEntries(Photo.CONTENT_ITEM_TYPE);
            if (photosDeltas == null) {
                continue;
            }
            for (int j = 0; j < photosDeltas.size(); j++) {
                photosDeltas.get(j).setSuperPrimary(false);
            }
        }
    }

    /**
     * Pass through to {@link PhotoEditorView#isWritablePhotoSet}.
     */
    public boolean isWritablePhotoSet() {
        return mPhotoView.isWritablePhotoSet();
    }

    /**
     * Get the raw contact ID for the current photo.
     */
    public long getPhotoRawContactId() {
        return mCurrentRawContactDelta == null ? - 1 : mCurrentRawContactDelta.getRawContactId();
    }

    public StructuredNameEditorView getNameEditorView() {
        /*/freeme.zhangjunjian, 20180123. redesign contact editors
        final KindSectionView nameKindSectionView = mKindSectionViewMap
                .get(StructuredName.CONTENT_ITEM_TYPE);
        return nameKindSectionView == null
                ? null : nameKindSectionView.getNameEditorView();
        /*/
        return mName == null
                ? null : mName;
        //*/
    }

    public RawContactDelta getCurrentRawContactDelta() {
        return mCurrentRawContactDelta;
    }

    /**
     * Marks the raw contact photo given as primary for the aggregate contact.
     */
    public void setPrimaryPhoto() {

        // Update values delta
        final ValuesDelta valuesDelta = mCurrentRawContactDelta
                .getSuperPrimaryEntry(Photo.CONTENT_ITEM_TYPE);
        if (valuesDelta == null) {
            Log.wtf(TAG, "setPrimaryPhoto: had no ValuesDelta for the current RawContactDelta");
            return;
        }
        valuesDelta.setFromTemplate(false);
        unsetSuperPrimaryFromAllPhotos();
        valuesDelta.setSuperPrimary(true);
    }

    public View getAggregationAnchorView() {
        final StructuredNameEditorView nameEditorView = getNameEditorView();
        return nameEditorView != null ? nameEditorView.findViewById(R.id.anchor_view) : null;
    }

    public void setGroupMetaData(Cursor groupMetaData) {
        final KindSectionView groupKindSectionView =
                mKindSectionViewMap.get(GroupMembership.CONTENT_ITEM_TYPE);
        if (groupKindSectionView == null) {
            return;
        }
        groupKindSectionView.setGroupMetaData(groupMetaData);
        //*/freeme.zhangjunjian, 20180129. redesign contact editors
        if (mGroup != null) {
            mGroup.setGroupMetaData(groupMetaData);
        }
        //*/

        if (mIsExpanded) {
            groupKindSectionView.setHideWhenEmpty(false);
            groupKindSectionView.updateEmptyEditors(/* shouldAnimate =*/ true);
        }
    }

    public void setIntentExtras(Bundle extras) {
        mIntentExtras = extras;
    }

    public void setState(RawContactDeltaList rawContactDeltas,
            MaterialColorMapUtils.MaterialPalette materialPalette, ViewIdGenerator viewIdGenerator,
            boolean hasNewContact, boolean isUserProfile, AccountWithDataSet primaryAccount,
            long rawContactIdToDisplayAlone) {
        Log.d(TAG, "[setState] beg: rawContactDeltas=" + rawContactDeltas
                + ", hasNewContact=" + hasNewContact + ", isUserProfile=" + isUserProfile
                + ", primaryAccount=" + primaryAccount + ", rawContactIdToDisplayAlone="
                + rawContactIdToDisplayAlone);
        mRawContactDeltas = rawContactDeltas;
        mRawContactIdToDisplayAlone = rawContactIdToDisplayAlone;

        mKindSectionViewMap.clear();
        mKindSectionViews.removeAllViews();
        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        mMoreFields.setVisibility(View.VISIBLE);
        /*/
        mName.setVisibility(View.VISIBLE);
        //*/

        mMaterialPalette = materialPalette;
        mViewIdGenerator = viewIdGenerator;

        mHasNewContact = hasNewContact;
        mIsUserProfile = isUserProfile;
        mPrimaryAccount = primaryAccount;
        /// M: [Google Issue] ALPS02703604.restore primary account when
        /// recreate(ex.rotate devices).@{
        restorePrimaryAccountIfHave(rawContactDeltas);
        /// @}
        if (mPrimaryAccount == null && mAccounts != null) {
            mPrimaryAccount = ContactEditorUtils.create(getContext())
                    .getOnlyOrDefaultAccount(AccountInfo.extractAccounts(mAccounts));
            Log.d(TAG, "[setState] get only or default account ");
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "state: primary " + mPrimaryAccount);
        }

        // Parse the given raw contact deltas
        if (rawContactDeltas == null || rawContactDeltas.isEmpty()) {
            elog("No raw contact deltas");
            if (mListener != null) mListener.onBindEditorsFailed();
            return;
        }
        pickRawContactDelta();
        if (mCurrentRawContactDelta == null) {
            elog("Couldn't pick a raw contact delta.");
            if (mListener != null) mListener.onBindEditorsFailed();
            return;
        }
        // Apply any intent extras now that we have selected a raw contact delta.
        applyIntentExtras();
        parseRawContactDelta();
        if (mKindSectionDataMap.isEmpty()) {
            elog("No kind section data parsed from RawContactDelta(s)");
            if (mListener != null) mListener.onBindEditorsFailed();
            return;
        }

        final KindSectionData nameSectionData =
                mKindSectionDataMap.get(StructuredName.CONTENT_ITEM_TYPE);
        // Ensure that a structured name and photo exists
        if (nameSectionData != null) {
            final RawContactDelta rawContactDelta =
                    nameSectionData.getRawContactDelta();
            RawContactModifier.ensureKindExists(
                    rawContactDelta,
                    rawContactDelta.getAccountType(mAccountTypeManager),
                    StructuredName.CONTENT_ITEM_TYPE);
            RawContactModifier.ensureKindExists(
                    rawContactDelta,
                    rawContactDelta.getAccountType(mAccountTypeManager),
                    Photo.CONTENT_ITEM_TYPE);
        }

        // Setup the view
        addPhotoView();
        setAccountInfo();
        //*/freeme.zhangjunjian, 20180119. redesign contact editors
        boolean isSim = (mSubId != -1);
        mRingContact.setVisibility(mIsUserProfile || isSim ? GONE : VISIBLE);
        addRingSelector();
        //*/
        if (isReadOnlyRawContact()) {
            // We're want to display the inputs fields for a single read only raw contact
            addReadOnlyRawContactEditorViews();
        } else {
            setupEditorNormally();
            // If we're inserting a new contact, request focus to bring up the keyboard for the
            // name field.
            if (mHasNewContact) {
                final StructuredNameEditorView name = getNameEditorView();
                if (name != null) {
                    name.requestFocusForFirstEditField();
                }
            }
        }
        if (mListener != null) mListener.onEditorsBound();
    }

    public void setAccounts(List<AccountInfo> accounts) {
        mAccounts.clear();
        mAccounts.addAll(accounts);
        // Update the account header
        setAccountInfo();
    }

    private void setupEditorNormally() {
        Log.d(TAG, "[setupEditorNormally] beg");
        addKindSectionViews();

        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        mMoreFields.setVisibility(hasMoreFields() ? View.VISIBLE : View.GONE);
        //*/

        if (mIsExpanded) showAllFields();
        Log.d(TAG, "[setupEditorNormally] end");
    }

    private boolean isReadOnlyRawContact() {
        return !mCurrentRawContactDelta.getAccountType(mAccountTypeManager).areContactsWritable();
    }

    private void pickRawContactDelta() {
        Log.d(TAG, "[pickRawContactDelta] beg");
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "parse: " + mRawContactDeltas.size() + " rawContactDelta(s)");
        }
        for (int j = 0; j < mRawContactDeltas.size(); j++) {
            final RawContactDelta rawContactDelta = mRawContactDeltas.get(j);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "parse: " + j + " rawContactDelta" + rawContactDelta);
            }
            if (rawContactDelta == null || !rawContactDelta.isVisible()) continue;
            final AccountType accountType = rawContactDelta.getAccountType(mAccountTypeManager);
            Log.d(TAG, "[pickRawContactDelta] accountType:" + accountType);
            if (accountType == null) continue;

            if (mRawContactIdToDisplayAlone > 0) {
                // Look for the raw contact if specified.
                if (rawContactDelta.getRawContactId().equals(mRawContactIdToDisplayAlone)) {
                    mCurrentRawContactDelta = rawContactDelta;
                    Log.d(TAG, "[pickRawContactDelta] end 1, mCurrentRawContactDelta"
                          +  mCurrentRawContactDelta);
                    return;
                }
            } else if (mPrimaryAccount != null
                    && mPrimaryAccount.equals(rawContactDelta.getAccountWithDataSet())) {
                // Otherwise try to find the one that matches the default.
                mCurrentRawContactDelta = rawContactDelta;
                Log.d(TAG, "[pickRawContactDelta] end 2, mCurrentRawContactDelta" +
                      mCurrentRawContactDelta);
                return;
            } else if (accountType.areContactsWritable()){
                // TODO: Find better raw contact delta
                // Just select an arbitrary writable contact.
                mCurrentRawContactDelta = rawContactDelta;
            }
        }
        Log.d(TAG, "[pickRawContactDelta] end 3, mCurrentRawContactDelta" +
              mCurrentRawContactDelta);
    }

    private void applyIntentExtras() {
        if (mIntentExtras == null || mIntentExtras.size() == 0) {
            return;
        }
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(getContext());
        final AccountType type = mCurrentRawContactDelta.getAccountType(accountTypes);

        RawContactModifier.parseExtras(getContext(), type, mCurrentRawContactDelta, mIntentExtras);
        mIntentExtras = null;
    }

    private void parseRawContactDelta() {
        mKindSectionDataMap.clear();
        mSortedMimetypes.clear();

        final AccountType accountType = mCurrentRawContactDelta.getAccountType(mAccountTypeManager);
        Log.d(TAG, "[parseRawContactDelta] to constuct mKindSectionDataMap. beg"
                + ", accountType=" + accountType);
        final List<DataKind> dataKinds = accountType.getSortedDataKinds();
        final int dataKindSize = dataKinds == null ? 0 : dataKinds.size();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "parse: " + dataKindSize + " dataKinds(s)");
        }

        for (int i = 0; i < dataKindSize; i++) {
            final DataKind dataKind = dataKinds.get(i);
            Log.d(TAG, "[parseRawContactDelta] i = " + i + ", dataKind = " + dataKind);
            // Skip null and un-editable fields.
            if (dataKind == null || !dataKind.editable) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "parse: " + i +
                            (dataKind == null ? " dropped null data kind"
                                    : " dropped uneditable mimetype: " + dataKind.mimeType));
                }
                continue;
            }
            final String mimeType = dataKind.mimeType;

            // Skip psuedo mime types
            if (DataKind.PSEUDO_MIME_TYPE_NAME.equals(mimeType) ||
                    DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(mimeType)) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "parse: " + i + " " + dataKind.mimeType + " dropped pseudo type");
                }
                continue;
            }

            // Skip custom fields
            // TODO: Handle them when we implement editing custom fields.
            if (CustomDataItem.MIMETYPE_CUSTOM_FIELD.equals(mimeType)) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "parse: " + i + " " + dataKind.mimeType + " dropped custom field");
                }
                continue;
            }

            final KindSectionData kindSectionData =
                    new KindSectionData(accountType, dataKind, mCurrentRawContactDelta);
            mKindSectionDataMap.put(mimeType, kindSectionData);
            mSortedMimetypes.add(mimeType);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "parse: " + i + " " + dataKind.mimeType + " " +
                        kindSectionData.getValuesDeltas().size() + " value(s) " +
                        kindSectionData.getNonEmptyValuesDeltas().size() + " non-empty value(s) " +
                        kindSectionData.getVisibleValuesDeltas().size() +
                        " visible value(s)");
            }
        }
        Log.d(TAG, "[parseRawContactDelta] to constuct mKindSectionDataMap. end");
    }

    private void addReadOnlyRawContactEditorViews() {
        mKindSectionViews.removeAllViews();
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(
                getContext());
        final AccountType type = mCurrentRawContactDelta.getAccountType(accountTypes);

        // Bail if invalid state or source
        if (type == null) return;

        // Make sure we have StructuredName
        RawContactModifier.ensureKindExists(
                mCurrentRawContactDelta, type, StructuredName.CONTENT_ITEM_TYPE);

        ValuesDelta primary;

        // Name
        final Context context = getContext();
        final Resources res = context.getResources();
        primary = mCurrentRawContactDelta.getPrimaryEntry(StructuredName.CONTENT_ITEM_TYPE);
        final String name = primary != null ? primary.getAsString(StructuredName.DISPLAY_NAME) :
            getContext().getString(R.string.missing_name);
        final Drawable nameDrawable = context.getDrawable(R.drawable.quantum_ic_person_vd_theme_24);
        final String nameContentDescription = res.getString(R.string.header_name_entry);
        bindData(nameDrawable, nameContentDescription, name, /* type */ null,
                /* isFirstEntry */ true);

        // Phones
        final ArrayList<ValuesDelta> phones = mCurrentRawContactDelta
                .getMimeEntries(Phone.CONTENT_ITEM_TYPE);
        final Drawable phoneDrawable = context.getDrawable(R.drawable.quantum_ic_phone_vd_theme_24);
        final String phoneContentDescription = res.getString(R.string.header_phone_entry);
        if (phones != null) {
            boolean isFirstPhoneBound = true;
            for (ValuesDelta phone : phones) {
                final String phoneNumber = phone.getPhoneNumber();
                if (TextUtils.isEmpty(phoneNumber)) {
                    continue;
                }
                final String formattedNumber = PhoneNumberUtilsCompat.formatNumber(
                        phoneNumber, phone.getPhoneNormalizedNumber(),
                        GeoUtil.getCurrentCountryIso(getContext()));
                CharSequence phoneType = null;
                if (phone.hasPhoneType()) {
                    phoneType = Phone.getTypeLabel(
                            res, phone.getPhoneType(), phone.getPhoneLabel());
                }
                bindData(phoneDrawable, phoneContentDescription, formattedNumber, phoneType,
                        isFirstPhoneBound, true);
                isFirstPhoneBound = false;
            }
        }

        // Emails
        final ArrayList<ValuesDelta> emails = mCurrentRawContactDelta
                .getMimeEntries(Email.CONTENT_ITEM_TYPE);
        final Drawable emailDrawable = context.getDrawable(R.drawable.quantum_ic_email_vd_theme_24);
        final String emailContentDescription = res.getString(R.string.header_email_entry);
        if (emails != null) {
            boolean isFirstEmailBound = true;
            for (ValuesDelta email : emails) {
                final String emailAddress = email.getEmailData();
                if (TextUtils.isEmpty(emailAddress)) {
                    continue;
                }
                CharSequence emailType = null;
                if (email.hasEmailType()) {
                    emailType = Email.getTypeLabel(
                            res, email.getEmailType(), email.getEmailLabel());
                }
                bindData(emailDrawable, emailContentDescription, emailAddress, emailType,
                        isFirstEmailBound);
                isFirstEmailBound = false;
            }
        }

        mKindSectionViews.setVisibility(mKindSectionViews.getChildCount() > 0 ? VISIBLE : GONE);
        // Hide the "More fields" link
        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        mMoreFields.setVisibility(GONE);
        //*/
    }

    private void bindData(Drawable icon, String iconContentDescription, CharSequence data,
            CharSequence type, boolean isFirstEntry) {
        bindData(icon, iconContentDescription, data, type, isFirstEntry, false);
    }

    private void bindData(Drawable icon, String iconContentDescription, CharSequence data,
            CharSequence type, boolean isFirstEntry, boolean forceLTR) {
        final View field = mLayoutInflater.inflate(R.layout.item_read_only_field, mKindSectionViews,
                /* attachToRoot */ false);
        if (isFirstEntry) {
            final ImageView imageView = (ImageView) field.findViewById(R.id.kind_icon);
            imageView.setImageDrawable(icon);
            imageView.setContentDescription(iconContentDescription);
        } else {
            final ImageView imageView = (ImageView) field.findViewById(R.id.kind_icon);
            imageView.setVisibility(View.INVISIBLE);
            imageView.setContentDescription(null);
        }
        final TextView dataView = (TextView) field.findViewById(R.id.data);
        dataView.setText(data);
        if (forceLTR) {
            dataView.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
        final TextView typeView = (TextView) field.findViewById(R.id.type);
        if (!TextUtils.isEmpty(type)) {
            typeView.setText(type);
        } else {
            typeView.setVisibility(View.GONE);
        }
        mKindSectionViews.addView(field);
    }

    private void setAccountInfo() {
        Log.d(TAG, "[setAccountInfo] beg");
        if (mCurrentRawContactDelta == null && mPrimaryAccount == null) {
            return;
        }
        final AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(getContext());
        final AccountInfo account = mCurrentRawContactDelta != null
                ? accountTypeManager.getAccountInfoForAccount(
                mCurrentRawContactDelta.getAccountWithDataSet())
                : accountTypeManager.getAccountInfoForAccount(mPrimaryAccount);

        // Accounts haven't loaded yet or we are editing.
        if (mAccounts.isEmpty()) {
            mAccounts.add(account);
        }

        // Get the account information for the primary raw contact delta
        if (isReadOnlyRawContact()) {
            final String accountType = account.getTypeLabel().toString();
            setAccountHeader(accountType,
                    getResources().getString(
                            R.string.editor_account_selector_read_only_title, accountType));
        } else {
            /* M: [Sim Contact Flow] Change USIM1 to SIM name if Icc account.
             * Original code: @{
            final String accountLabel = mIsUserProfile
                    ? EditorUiUtils.getAccountHeaderLabelForMyProfile(getContext(), account)
                    : account.getNameLabel().toString();
             * @}
             * New code: @{ */
            final String accountLabel = getAccountLabel(account);
            /* @} */
            /*/freeme.zhangjunjian, 20180123. redesign contact editors
            setAccountHeader(getResources().getString(R.string.editor_account_selector_title),
                    accountLabel);
            /*/
            setAccountHeader(getResources().getString(R.string.freeme_contact_add),
                    accountLabel);
            //*/
        }

        // If we're saving a new contact and there are multiple accounts, add the account selector.
        if (mHasNewContact && !mIsUserProfile && mAccounts.size() > 1) {
            addAccountSelector(mCurrentRawContactDelta);
        }
        Log.d(TAG, "[setAccountInfo] end");
    }

    private void setAccountHeader(String primaryText, String secondaryText) {
        mAccountHeaderPrimaryText.setText(primaryText);
        mAccountHeaderSecondaryText.setText(secondaryText);

        // Set the icon
        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        final AccountType accountType =
                mCurrentRawContactDelta.getRawContactAccountType(getContext());
        mAccountHeaderIcon.setImageDrawable(accountType.getDisplayIcon(getContext()));
        /*/

        // Set the content description
        mAccountHeaderContainer.setContentDescription(
                EditorUiUtils.getAccountInfoContentDescription(secondaryText, primaryText));
    }

    private void addAccountSelector(final RawContactDelta rawContactDelta) {
        // Add handlers for choosing another account to save to.
        mAccountHeaderExpanderIcon.setVisibility(View.VISIBLE);
        final OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                final AccountWithDataSet current = rawContactDelta.getAccountWithDataSet();
                AccountInfo.sortAccounts(current, mAccounts);
                /*/freeme.zhangjunjian, 20180207. redesign contact editors
                final ListPopupWindow popup = new ListPopupWindow(getContext(), null);
                /* M:[Google Issue] ALPS03746536 2/3. @{ * /
                mAccountSelectorPopup = popup;
                //*/
                Log.d(TAG, "[click on AccountSelector] new mAccountSelectorPopup = "
                        + mAccountSelectorPopup);
                /* @} */
                final AccountsListAdapter adapter =
                        new AccountsListAdapter(getContext(), mAccounts, current);
                /*/freeme.zhangjunjian, 20180206. redesign contact editors
                popup.setWidth(mAccountHeaderContainer.getWidth());
                popup.setAnchorView(mAccountHeaderContainer);
                popup.setAdapter(adapter);
                popup.setModal(true);
                popup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
                popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
                        UiClosables.closeQuietly(popup);
                        final AccountWithDataSet newAccount = adapter.getItem(position);
                        if (mListener != null && !mPrimaryAccount.equals(newAccount)) {
                            mIsExpanded = false;
                            mListener.onRebindEditorsForNewContact(
                                    rawContactDelta,
                                    mPrimaryAccount,
                                    newAccount);
                        }
                    }
                });
                popup.show();
                /*/
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UiClosables.closeDialogQuietly(mDialog);
                        final AccountWithDataSet newAccount = adapter.getItem(which);
                        if (mListener != null && !mPrimaryAccount.equals(newAccount)) {
                            mIsExpanded = false;
                            mListener.onRebindEditorsForNewContact(
                                    rawContactDelta,
                                    mPrimaryAccount,
                                    newAccount);
                        }
                    }
                });
                mDialog = builder.create();
                mDialog.show();
                //*/
            }
        };
        mAccountHeaderContainer.setOnClickListener(clickListener);
        // Make the expander icon clickable so that it will be announced as a button by
        // talkback
        mAccountHeaderExpanderIcon.setOnClickListener(clickListener);
    }

    private void addPhotoView() {
        Log.d(TAG, "[addPhotoView] beg");
        if (!mCurrentRawContactDelta.hasMimeEntries(Photo.CONTENT_ITEM_TYPE)) {
            wlog("No photo mimetype for this raw contact.");
            mPhotoView.setVisibility(GONE);
            return;
        } else {
            mPhotoView.setVisibility(VISIBLE);
        }

        final ValuesDelta superPrimaryDelta = mCurrentRawContactDelta
                .getSuperPrimaryEntry(Photo.CONTENT_ITEM_TYPE);
        if (superPrimaryDelta == null) {
            Log.wtf(TAG, "addPhotoView: no ValueDelta found for current RawContactDelta"
                    + "that supports a photo.");
            mPhotoView.setVisibility(GONE);
            return;
        }
        // Set the photo view
        /*/ freeme.linqingwei, 20180321. redesign contact editors.
        mPhotoView.setPalette(mMaterialPalette);
        //*/
        mPhotoView.setPhoto(superPrimaryDelta);
        if (isReadOnlyRawContact()) {
            mPhotoView.setReadOnly(true);
            return;
        }
        mPhotoView.setReadOnly(false);
        mPhotoValuesDelta = superPrimaryDelta;
        /// M: [Sim Contact Flow] ALPS02698016. sim account not support photo. 2/2 @{
        boolean isIccAccount = mCurrentRawContactDelta
                .getAccountType(mAccountTypeManager).isIccCardAccount();
        Log.d(TAG, "[addPhotoView] isIccAccount: " + isIccAccount);
        if (isIccAccount) {
            mPhotoView.setSelectPhotoEnable(false);
        } else {
            mPhotoView.setSelectPhotoEnable(true);
        }
        /// @}
        Log.d(TAG, "[addPhotoView] end");
    }

    private void addKindSectionViews() {
        Log.d(TAG, "[addKindSectionViews] beg.");
        int i = -1;

        for (String mimeType : mSortedMimetypes) {
            i++;
            // Ignore mime types that we've already handled
            if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "kind: " + i + " " + mimeType + " dropped");
                }
                continue;
            }
            //*/ freeme.linqingwei, 20180409. redesign contacts editor for 8.1 style.
            handleKindSectionViews(mimeType);
            /*/
            final KindSectionView kindSectionView;
            final KindSectionData kindSectionData = mKindSectionDataMap.get(mimeType);
            kindSectionView = inflateKindSectionView(mKindSectionViews, kindSectionData, mimeType);
            mKindSectionViews.addView(kindSectionView);

            // Keep a pointer to the KindSectionView for each mimeType
            mKindSectionViewMap.put(mimeType, kindSectionView);
            //*/
        }
        Log.d(TAG, "[addKindSectionViews] end");
    }

    private KindSectionView inflateKindSectionView(ViewGroup viewGroup,
            KindSectionData kindSectionData, String mimeType) {
        Log.d(TAG, "[inflateKindSectionView] beg, mimeType=" + mimeType
                + ", kindSectionData.mDataKind=" + kindSectionData.getDataKind()
                + ", kindSectionData.mRawContactDelta=" + kindSectionData.getRawContactDelta());
        final KindSectionView kindSectionView = (KindSectionView)
                mLayoutInflater.inflate(R.layout.item_kind_section, viewGroup,
                        /* attachToRoot =*/ false);
        kindSectionView.setIsUserProfile(mIsUserProfile);
        /*/freeme.zhangjunjian, 20180123. redesign contact editors
        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)
                || Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
        /*/
        kindSectionView.setSubId(mSubId);
        kindSectionView.setMimeType(mimeType);
        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)
                || Email.CONTENT_ITEM_TYPE.equals(mimeType)
                || StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)
                || Im.CONTENT_ITEM_TYPE.equals(mimeType)
                || Website.CONTENT_ITEM_TYPE.equals(mimeType)
                || Organization.CONTENT_ITEM_TYPE.equals(mimeType)
                ) {
            //*/
            // Phone numbers and email addresses are always displayed,
            // even if they are empty
            kindSectionView.setHideWhenEmpty(false);
        }

        // Since phone numbers and email addresses displayed even if they are empty,
        // they will be the only types you add new values to initially for new contacts
        kindSectionView.setShowOneEmptyEditor(true);

        //*/ freeme.liqiang, 20180702. add empty item when click from phone or email card
        kindSectionView.setEntryType(mEntryType);
        //*/
        kindSectionView.setState(kindSectionData, mViewIdGenerator, mListener);
        Log.d(TAG, "[inflateKindSectionView] end");
        return kindSectionView;
    }

    private void showAllFields() {
        // Stop hiding empty editors and allow the user to enter values for all kinds now
        Log.d(TAG, "[showAllFields] count: " + mKindSectionViews.getChildCount() +
                ", PrimaryAccount: " + mPrimaryAccount + ",mIsUserProfile: " + mIsUserProfile);
        for (int i = 0; i < mKindSectionViews.getChildCount(); i++) {
            final KindSectionView kindSectionView =
                    (KindSectionView) mKindSectionViews.getChildAt(i);
            kindSectionView.setHideWhenEmpty(false);
            /// M: ALPS02703604. no need show PhonicName fields for iccAccount.@{
            kindSectionView.setIsCurrentIccAccount((!mIsUserProfile) && (mPrimaryAccount != null)
                    && AccountTypeUtils.isAccountTypeIccCard(mPrimaryAccount.type));
            /// @}
            kindSectionView.updateEmptyEditors(/* shouldAnimate =*/ true);
        }
        mIsExpanded = true;

        // Hide the more fields button
        /*/freeme.zhangjunjian, 20180116. redesign contact editors
        mMoreFields.setVisibility(View.GONE);
        //*/
    }

    private boolean hasMoreFields() {
        for (KindSectionView section : mKindSectionViewMap.values()) {
            if (section.getVisibility() != View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private static void wlog(String message) {
        if (Log.isLoggable(TAG, Log.WARN)) {
            Log.w(TAG, message);
        }
    }

    private static void elog(String message) {
        Log.e(TAG, message);
    }

    /* M: [Google Issue] ALPS02703604. restore primary account when reCreate. because
     * primary account is not update when rotate device.if Profile no restore it @{
     */
    private void restorePrimaryAccountIfHave(RawContactDeltaList state) {
        Log.d(TAG, "[restorePrimaryAccountIfHave] mIsUserProfile: " + mIsUserProfile);
        if ((!mIsUserProfile) && (state != null) && (state.size() > 0) &&
                (mPrimaryAccount == null)) {
            RawContactDelta contactDelta = state.get(0);

            if (contactDelta == null) {
                Log.i(TAG, "[restorePrimaryAccountIfHave] contactDelta is null,return" );
                return;
            }
            String accountName = contactDelta.getAccountName();
            String accountType = contactDelta.getAccountType();
            String dataSet = contactDelta.getDataSet();
            Log.d(TAG, "[restorePrimaryAccountIfHave] accountName :" + accountName + ",accountType:"
                    + accountType + ",dataset: " + dataSet);
            if (accountName == null || accountType == null) {
                Log.d(TAG, "[restorePrimaryAccountIfHave] accountName or accontType is null," +
                        "return" );
                return;
            }
            if (AccountTypeUtils.isAccountTypeIccCard(accountType)) {
                mPrimaryAccount = new AccountWithDataSetEx(accountName, accountType, dataSet,
                        AccountTypeUtils.getSubIdBySimAccountName(getContext(), accountName));
            } else {
                mPrimaryAccount = new AccountWithDataSet(accountName,accountType, dataSet);
            }
            Log.d(TAG, "[restorePrimaryAccountIfHave] update PrimaryAccount success!" +
                    ",mPrimaryAccount: " + mPrimaryAccount);
            }
        }
    /* @}*/

    /* M: [Sim Contact Flow] Change USIM1 to SIM name if Icc account. @{ */
    private String getAccountLabel(AccountInfo account) {
        if (mIsUserProfile) {
            return EditorUiUtils.getAccountHeaderLabelForMyProfile(
                    getContext(), account);
        }
        String iccAccountLabel = null;
        //*/freeme.zhangjunjian, 20180206. redesign contact editors
        mSubId = -1;
        //*/
        if (AccountTypeUtils.isAccountTypeIccCard(account.getType().accountType)) {
            if (account.getAccount().name != null) {
                int subId = AccountTypeUtils.getSubIdBySimAccountName(getContext(),
                        account.getAccount().name);
                //*/freeme.zhangjunjian, 20180206. redesign contact editors
                mSubId = subId;
                //*/
                iccAccountLabel = SubInfoUtils.getDisplaynameUsingSubId(subId);
            }
        }
        return (iccAccountLabel != null ? iccAccountLabel
                : account.getNameLabel().toString());
    }
    /* @}*/

    /* M:[Google Issue] ALPS03746536 1/3.
     * Should dismiss ListPopupWindow when enter/exit multi-window,
     * otherwise it still shows up besides we lose its reference. @{ */
    private ListPopupWindow mAccountSelectorPopup = null;
    /* @}*/

    //*/freeme.zhangjunjian, 20180123. redesign contact editors
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

    private void addRingSelector() {
        final OnClickListener mRingContactListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                addContactRingSelector();
            }
        };
        mRingContact.setOnClickListener(mRingContactListener);
    }

    private String mCustomRingtone;
    public Uri mLookupUri;
    private static final int REQUEST_CODE_PICK_RINGTONE = 4;
    private static final int CURRENT_API_VERSION = android.os.Build.VERSION.SDK_INT;
    private Uri mRingtoneUri;

    private void addContactRingSelector() {

        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        // Allow user to pick 'Default'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        // Show only ringtones
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        // Allow the user to pick a silent ringtone
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        final Uri ringtoneUri = EditorUiUtils.getRingtoneUriFromString(mCustomRingtone,
                CURRENT_API_VERSION);

        // Put checkmark next to the current ringtone for this contact
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mRingtoneUri);

        // Launch!
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(mContext, R.string.missing_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_RINGTONE && data != null) {
            final Uri pickedUri = data.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            mCustomRingtone = EditorUiUtils.getRingtoneStringFromUri(pickedUri, CURRENT_API_VERSION);
            updateRingtoneName(mContext, pickedUri, 0);
            //*/ freeme.liqiang, 20180403. refresh menu item when edit contacts
            if (mListener != null) {
                mListener.onRingtoneChanged(pickedUri);
            }
            //*/
        }
    }

    public String getRingtone() {
        return mCustomRingtone;
    }

    public void updateRingtoneName(Context context, Uri ringtoneUri, int type) {
        mRingtoneUri = ringtoneUri;
        if (context == null) {
            Log.e(TAG, "Unable to update ringtone name, no context provided");
        }
        final String unknown = context.getString(com.android.internal.R.string.ringtone_unknown);
        String summary = unknown;
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(com.android.internal.R.string.ringtone_silent);
        } else {
            Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            if (ringtone != null) {
                summary = ringtone.getTitle(context);
            }
        }

        if (summary.equals(unknown)) {
            // if ringtone don't exsit, like be deleted, use default ringtone
            Ringtone ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(type));
            if (ringtone != null) {
                summary = ringtone.getTitle(context);
            }
        }
        mContactRing.setText(summary);
    }

    private void addNameEditorViews(AccountType accountType, RawContactDelta rawContactDelta) {
        final ValuesDelta nameValuesDelta = rawContactDelta
                .getSuperPrimaryEntry(StructuredName.CONTENT_ITEM_TYPE);

        /*/ freeme.liqiang, 20180417. refresh menu item
        if (!mIsUserProfile) {
            // Don't set super primary for the me contact
            mName.setEditorListener(new StructuredNameEditorListener(
                    nameValuesDelta, rawContactDelta.getRawContactId(), mListener));
        }
        /*/
        mName.setEditorListener(new StructuredNameEditorListener(
                nameValuesDelta, rawContactDelta.getRawContactId(), mListener));
        //*/
        mName.setDeletable(false);
        mName.setValues(accountType.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_NAME),
                nameValuesDelta, rawContactDelta, /* readOnly =*/ false, mViewIdGenerator);
        mName.setRight(R.id.photo_editor);
    }

    private void addGroupEditorView(RawContactDelta rawContactDelta, DataKind dataKind) {
        mGroup.setKind(dataKind);
        mGroup.setEnabled(isEnabled());
        mGroup.setState(rawContactDelta);
        //*/ freeme.liqiang, 20180403. refresh menu item when edit contacts
        mGroup.setGroupEditorListener(mListener);
        //*/
        if (!mIsUserProfile) {
            mGroup.setEnabled(true);
        }
        mGroup.setVisibility(mIsUserProfile ? GONE : VISIBLE);
    }

    private void handleKindSectionViews(final String mimeType) {
        final KindSectionData kindSectionData = mKindSectionDataMap.get(mimeType);
        final KindSectionView kindSectionView = inflateKindSectionView(mKindSectionViews,
                kindSectionData, mimeType);

        switch (mimeType) {
            case GroupMembership.CONTENT_ITEM_TYPE:
                addGroupEditorView(kindSectionData.getRawContactDelta(),
                        kindSectionData.getDataKind());
                break;
            case StructuredName.CONTENT_ITEM_TYPE:
                addNameEditorViews(kindSectionData.getAccountType(),
                        kindSectionData.getRawContactDelta());
                break;
            default:
                break;
        }

        mKindSectionViews.addView(kindSectionView);

        // Keep a pointer to the KindSectionView for each mimeType
        mKindSectionViewMap.put(mimeType, kindSectionView);
    }
    //*/

    //*/ freeme.liqiang, 20180702. add empty item when click from phone or email card
    private String mEntryType;

    public void setEntryType(String entryType){
        mEntryType = entryType;
    }
    //*/
}
