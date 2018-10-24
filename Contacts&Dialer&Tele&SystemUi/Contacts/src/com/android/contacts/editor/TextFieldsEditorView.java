/*
* Copyright (C) 2014 MediaTek Inc.
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

package com.android.contacts.editor;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.text.style.TtsSpan;
import android.util.AttributeSet;
//import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType.EditField;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.PhoneNumberFormatter;

import com.mediatek.contacts.aassne.SimAasEditor;
import com.mediatek.contacts.editor.ContactEditorUtilsEx;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.Log;
//*/ freeme.zhangjunjian, 20180129. redesign contact editor
import android.content.res.Configuration;
import android.widget.TextView;
//*/
//*/ freeme.liqiang, 20180529. create new account when switch storage path
import com.android.contacts.util.NameConverter;
import com.mediatek.contacts.util.AccountTypeUtils;
//*/

/**
 * Simple editor that handles labels and any {@link EditField} defined for the
 * entry. Uses {@link ValuesDelta} to read any existing {@link RawContact} values,
 * and to correctly write any changes values.
 */
public class TextFieldsEditorView extends LabeledEditorView {
    private static final String TAG = TextFieldsEditorView.class.getSimpleName();

    private EditText[] mFieldEditTexts = null;
    private ViewGroup mFields = null;
    protected View mExpansionViewContainer;
    protected ImageView mExpansionView;
    protected String mCollapseButtonDescription;
    protected String mExpandButtonDescription;
    protected String mCollapsedAnnouncement;
    protected String mExpandedAnnouncement;
    /*/ freeme.zhaozehong, 20180408. for freemeOS, UI redesign
    private boolean mHideOptional = true;
    /*/
    private boolean mHideOptional = false;
    //*/
    private boolean mHasShortAndLongForms;
    private int mMinFieldHeight;
    private int mPreviousViewHeight;
    private int mHintTextColorUnfocused;

    public TextFieldsEditorView(Context context) {
        super(context);
    }

    public TextFieldsEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextFieldsEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        Log.d(TAG, "[onFinishInflate] beg");
        super.onFinishInflate();

        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        /*/freeme.zhangjunjian, 20180206. redesign contact editors
        mMinFieldHeight = getContext().getResources().getDimensionPixelSize(
                R.dimen.editor_min_line_item_height);
        /*/
        mMinFieldHeight = getContext().getResources().getDimensionPixelSize(
                R.dimen.freeme_editor_min_line_item_height);
        //*/
        mFields = (ViewGroup) findViewById(R.id.editors);
        mHintTextColorUnfocused = getResources().getColor(R.color.editor_disabled_text_color);
        mExpansionView = (ImageView) findViewById(R.id.expansion_view);
        mCollapseButtonDescription = getResources()
                .getString(R.string.collapse_fields_description);
        mCollapsedAnnouncement = getResources()
                .getString(R.string.announce_collapsed_fields);
        mExpandButtonDescription = getResources()
                .getString(R.string.expand_fields_description);
        mExpandedAnnouncement = getResources()
                .getString(R.string.announce_expanded_fields);

        mExpansionViewContainer = findViewById(R.id.expansion_view_container);
        if (mExpansionViewContainer != null) {
            mExpansionViewContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "[mExpansionViewContainer] onClick");
                    mPreviousViewHeight = mFields.getHeight();

                    // Save focus
                    final View focusedChild = findFocus();
                    final int focusedViewId = focusedChild == null ? -1 : focusedChild.getId();

                    /// M: [Google Issue] ALPS00809436 @{
                    InputMethodManager imm = (InputMethodManager) getContext().
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && v != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    /// @}

                    // Reconfigure GUI
                    mHideOptional = !mHideOptional;
                    onOptionalFieldVisibilityChange();
                    rebuildValues();

                    // Restore focus
                    View newFocusView = findViewById(focusedViewId);
                    if (newFocusView == null || newFocusView.getVisibility() == GONE) {
                        // find first visible child
                        newFocusView = TextFieldsEditorView.this;
                    }
                    newFocusView.requestFocus();

                    EditorAnimator.getInstance().slideAndFadeIn(mFields, mPreviousViewHeight);
                    announceForAccessibility(mHideOptional ?
                            mCollapsedAnnouncement : mExpandedAnnouncement);
                }
            });
        }
        Log.d(TAG, "[onFinishInflate] end");
    }

    @Override
    public void editNewlyAddedField() {
        // Some editors may have multiple fields (eg: first-name/last-name), but since the user
        // has not selected a particular one, it is reasonable to simply pick the first.
        final View editor = mFields.getChildAt(0);

        // Show the soft-keyboard.
        InputMethodManager imm =
                (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (mFieldEditTexts != null) {
            for (int index = 0; index < mFieldEditTexts.length; index++) {
                mFieldEditTexts[index].setEnabled(!isReadOnly() && enabled);
            }
        }
        if (mExpansionView != null) {
            mExpansionView.setEnabled(!isReadOnly() && enabled);
        }
    }

    private OnFocusChangeListener mTextFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (getEditorListener() != null) {
                getEditorListener().onRequest(EditorListener.EDITOR_FOCUS_CHANGED);
            }
            // Rebuild the label spinner using the new colors.
            rebuildLabel();
            //*/ freeme.zhangyalun, 20180502. Edit contacts, click add phone, do not pop up keyboard
            InputMethodManager inputMethodManager = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            int mInputHeightType = inputMethodManager.getInputMethodWindowVisibleHeight();
            if (hasFocus && mInputHeightType == 0) {
                inputMethodManager.showSoftInput(v, 0);
            }
            //*/
        }
    };

    /**
     * Creates or removes the type/label button. Doesn't do anything if already correctly configured
     */
    /*/freeme.zhangjunjian, 20180122. redesign contact editors
    private void setupExpansionView(boolean shouldExist, boolean collapsed) {
    /*/
    public void setupExpansionView(boolean shouldExist, boolean collapsed) {
    //*/
        final Drawable expandIcon = getContext().getDrawable(collapsed
                ? R.drawable.quantum_ic_expand_more_vd_theme_24
                : R.drawable.quantum_ic_expand_less_vd_theme_24);
        mExpansionView.setImageDrawable(expandIcon);
        mExpansionView.setContentDescription(collapsed ? mExpandButtonDescription
                : mCollapseButtonDescription);
        Log.d(TAG, "[setupExpansionView] shouldExist =" + shouldExist);
        /*/freeme.zhangjunjian, 20180202. redesign contact editors
        mExpansionViewContainer.setVisibility(shouldExist ? View.VISIBLE : View.INVISIBLE);
        /*/
        mExpansionViewContainer.setVisibility(View.GONE);
        mShouldExistExpansion = shouldExist;
        //*/
    }

    @Override
    protected void requestFocusForFirstEditField() {
        if (mFieldEditTexts != null && mFieldEditTexts.length != 0) {
            EditText firstField = null;
            boolean anyFieldHasFocus = false;
            for (EditText editText : mFieldEditTexts) {
                if (firstField == null && editText.getVisibility() == View.VISIBLE) {
                    firstField = editText;
                }
                if (editText.hasFocus()) {
                    anyFieldHasFocus = true;
                    break;
                }
            }
            if (!anyFieldHasFocus && firstField != null) {
                firstField.requestFocus();
            }
        }
    }

    public void setValue(int field, String value) {
        mFieldEditTexts[field].setText(value);
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly,
            ViewIdGenerator vig) {
        /// M: [Sim Contact Flow][ALPS01893634][ALPS01929324]
        /// For Icc card, it's nickName is null, so the kind is empty. @{
        if (kind == null || kind.fieldList == null) {
            return;
        }
        /// @}

        super.setValues(kind, entry, state, readOnly, vig);
        // Remove edit texts that we currently have
        if (mFieldEditTexts != null) {
            for (EditText fieldEditText : mFieldEditTexts) {
                mFields.removeView(fieldEditText);
            }
        }
        boolean hidePossible = false;

        int fieldCount = kind.fieldList == null ? 0 : kind.fieldList.size();
        Log.d(TAG, "[setValues] loop kind.fieldList, fieldCount=" + fieldCount);
        mFieldEditTexts = new EditText[fieldCount];
        /* M: [Google Issue]ALPS03260782, 1/3 @{*/
        boolean showDeleteButton = false;
        /* @} */
        //*/ Freeme.zhangjunjian. 20180117, redesign contact editors.
        boolean hasNonEmptyField = false;
        //*/
        for (int index = 0; index < fieldCount; index++) {
            final EditField field = kind.fieldList.get(index);
            Log.d(TAG, "[setValues] index=" + index + ", field=" + field);
            final EditText fieldView = new EditText(getContext());
            //*/ Freeme.zhangjunjian. 20180117, redesign contact editors.
            // remove padding
            fieldView.setPadding(0, 0, 0, 0);
            fieldView.setBackground(null);
            //*/
            fieldView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            fieldView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(R.dimen.editor_form_text_size));
            fieldView.setHintTextColor(mHintTextColorUnfocused);
            mFieldEditTexts[index] = fieldView;
            fieldView.setId(vig.getId(state, kind, entry, index));
            if (field.titleRes > 0) {
                fieldView.setHint(field.titleRes);
            }
            /// M: [Sim Contact Flow][AAS] @{
            if (Phone.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
                GlobalEnv.getSimAasEditor().updateView(state, fieldView, entry,
                        SimAasEditor.VIEW_UPDATE_HINT);
            }
            /// @}

            int inputType = field.inputType;
            fieldView.setInputType(inputType);
            if (inputType == InputType.TYPE_CLASS_PHONE) {
                /// M:op01 will add its own listener to filter phone number.
                ExtensionManager.getInstance().getOp01Extension().setViewKeyListener(fieldView);

                PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(
                        getContext(), fieldView,
                        /* formatAfterWatcherSet =*/ state.isContactInsert());
                fieldView.setTextDirection(View.TEXT_DIRECTION_LTR);
                //*/ freeme.zhangyalun, 20180504. New contact, number cannot enter symbols and spaces
                fieldView.setKeyListener(InputKeyListener.getInstance());
                //*/
            }
            fieldView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            // Set either a minimum line requirement or a minimum height (because {@link TextView}
            // only takes one or the other at a single time).
            if (field.minLines > 1) {
                fieldView.setMinLines(field.minLines);
            } else {
                // This needs to be called after setInputType. Otherwise, calling setInputType
                // will unset this value.
                fieldView.setMinHeight(mMinFieldHeight);
            }

            // Show the "next" button in IME to navigate between text fields
            // TODO: Still need to properly navigate to/from sections without text fields,
            // See Bug: 5713510
            fieldView.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_FULLSCREEN);

            // Read current value from state
            final String column = field.column;
            /*/ freeme.liqiang, 20180529. create new account when switch storage path
            final String value = entry.getAsString(column);
            /*/
            String value = entry.getAsString(column);

            if (value == null && AccountTypeUtils.ACCOUNT_TYPE_LOCAL_PHONE == state.getAccountType()
                    && ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE == entry.getMimetype()) {
                String familyName = entry.getAsString(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
                String middleName = entry.getAsString(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME);
                String givenName = entry.getAsString(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);

                value = NameConverter.buildPhoneticName(familyName, middleName, givenName);
            }
            //*/
            if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
                fieldView.setText(PhoneNumberUtilsCompat.createTtsSpannable(value));
            } else {
                /// M: [Google Issue] ALPS00244669. max length that EditField can input. 1/2 @{
                fieldView.setFilters(new InputFilter[] { new InputFilter.LengthFilter(
                        ContactEditorUtilsEx.getFieldEditorLengthLimit(inputType)) });
                /// @}
                fieldView.setText(value);
            }
            Log.d(TAG, "[setValues] fieldView.setText()=" + fieldView.getText().toString());

            // Show the delete button if we have a non-empty value
            /* M: [Google Issue]ALPS03260782, 2/3, show the delete button
             * if there is at least one non-empty value on all fieldView.
             * Original code: @{
            setDeleteButtonVisible(!TextUtils.isEmpty(value));
             * @}
             * New code: @{ */
            if (!TextUtils.isEmpty(value) && !showDeleteButton) {
                showDeleteButton = true;
            }
            /* @} */

            // Prepare listener for writing changes
            fieldView.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    // Trigger event for newly changed value
                    onFieldChanged(column, s.toString());
                    //M: [RCS] OP01 RCS will listen phone number text change.@{
                    ExtensionManager.getInstance().getRcsExtension().
                            setTextChangedListener(state, fieldView, inputType, s.toString());
                    /** @} */
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(
                            getKind().mimeType) || !(s instanceof Spannable)) {
                        return;
                    }
                    final Spannable spannable = (Spannable) s;
                    final TtsSpan[] spans = spannable.getSpans(0, s.length(), TtsSpan.class);
                    for (int i = 0; i < spans.length; i++) {
                        spannable.removeSpan(spans[i]);
                    }
                    PhoneNumberUtilsCompat.addTtsSpan(spannable, 0, s.length());
                }
            });

            fieldView.setEnabled(isEnabled() && !readOnly);
            fieldView.setOnFocusChangeListener(mTextFocusChangeListener);

            if (field.shortForm) {
                hidePossible = true;
                mHasShortAndLongForms = true;
                fieldView.setVisibility(mHideOptional ? View.VISIBLE : View.GONE);
            } else if (field.longForm) {
                hidePossible = true;
                mHasShortAndLongForms = true;
                fieldView.setVisibility(mHideOptional ? View.GONE : View.VISIBLE);
            } else {
                // Hide field when empty and optional value
                final boolean couldHide = (!ContactsUtils.isGraphic(value) && field.optional);
                final boolean willHide = (mHideOptional && couldHide);
                fieldView.setVisibility(willHide ? View.GONE : View.VISIBLE);
                hidePossible = hidePossible || couldHide;
                //*/ Freeme.zhangjunjian. 20180117, redesign contact editors.
                // postal-address or organization and other fields which more then one, should be expandable.
                if (!willHide && fieldCount > 1) {
                    hidePossible = true;
                    fieldView.setVisibility(index == 0 ? VISIBLE
                            : (mHideOptional ? GONE : VISIBLE));
                    // postal-address or organization and other fields which more then one,
                    // while has non-empty field, then we should show expansion view instead of delete button.
                    if (!hasNonEmptyField && !TextUtils.isEmpty(value)) {
                        hasNonEmptyField = true;
                    }
                }
                //*/
            }

            mFields.addView(fieldView);
        }
        /* M: [Google Issue]ALPS03260782, 3/3 @{ */
        setDeleteButtonVisible(showDeleteButton);
        /* @} */

        //*/ Freeme.zhangjunjian. 20180117, redesign contact editors.
        if (hasNonEmptyField) {
            setDeleteButtonVisible(false);
        }
        //*/
        if (mExpansionView != null) {
            // When hiding fields, place expandable
            setupExpansionView(hidePossible, mHideOptional);
            mExpansionView.setEnabled(!readOnly && isEnabled());
        }
        updateEmptiness();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < mFields.getChildCount(); i++) {
            EditText editText = (EditText) mFields.getChildAt(i);
            if (!TextUtils.isEmpty(editText.getText())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the editor is currently configured to show optional fields.
     */
    public boolean areOptionalFieldsVisible() {
        return !mHideOptional;
    }

    public boolean hasShortAndLongForms() {
        return mHasShortAndLongForms;
    }

    /**
     * Populates the bound rectangle with the bounds of the last editor field inside this view.
     */
    public void acquireEditorBounds(Rect bounds) {
        if (mFieldEditTexts != null) {
            for (int i = mFieldEditTexts.length; --i >= 0;) {
                EditText editText = mFieldEditTexts[i];
                if (editText.getVisibility() == View.VISIBLE) {
                    bounds.set(editText.getLeft(), editText.getTop(), editText.getRight(),
                            editText.getBottom());
                    return;
                }
            }
        }
    }

    /**
     * Saves the visibility of the child EditTexts, and mHideOptional.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.mHideOptional = mHideOptional;

        final int numChildren = mFieldEditTexts == null ? 0 : mFieldEditTexts.length;
        ss.mVisibilities = new int[numChildren];
        for (int i = 0; i < numChildren; i++) {
            ss.mVisibilities[i] = mFieldEditTexts[i].getVisibility();
        }
        /// M: [Google Issue] ALPS03416992. 4/5 @{
        ss.mSelfVisibility = this.getVisibility();
        /// @}

        return ss;
    }

    /**
     * Restores the visibility of the child EditTexts, and mHideOptional.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mHideOptional = ss.mHideOptional;

        int numChildren = Math.min(mFieldEditTexts == null ? 0 : mFieldEditTexts.length,
                ss.mVisibilities == null ? 0 : ss.mVisibilities.length);
        for (int i = 0; i < numChildren; i++) {
            mFieldEditTexts[i].setVisibility(ss.mVisibilities[i]);
        }
        rebuildValues();
        /// M: [Google Issue] ALPS03416992. 5/5 @{
        this.setVisibility(ss.mSelfVisibility);
        /// @}
    }

    private static class SavedState extends BaseSavedState {
        public boolean mHideOptional;
        public int[] mVisibilities;
        /// M: [Google Issue] ALPS03416992. Keep TextFieldsEditorView's
        /// self visibility when configuration change. 1/5 @{
        public int mSelfVisibility;
        /// @}

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mVisibilities = new int[in.readInt()];
            in.readIntArray(mVisibilities);
            /// M: [Google Issue] Bug fix ALPS00564820. 1/2 @{
            mHideOptional = in.readInt() == 1 ? true : false;
            /// @}
            /// M: [Google Issue] ALPS03416992. 2/5 @{
            mSelfVisibility = in.readInt();
            /// @}
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mVisibilities.length);
            out.writeIntArray(mVisibilities);
            /// M: Bug fix ALPS00564820. 2/2 @{
            out.writeInt(mHideOptional ? 1 : 0);
            /// @}
            /// M: [Google Issue] ALPS03416992. 3/5 @{
            out.writeInt(mSelfVisibility);
            /// @}
        }

        @SuppressWarnings({"unused", "hiding" })
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    public void clearAllFields() {
        if (mFieldEditTexts != null) {
            for (EditText fieldEditText : mFieldEditTexts) {
                // Update UI (which will trigger a state change through the {@link TextWatcher})
                fieldEditText.setText("");
            }
        }
    }

    //*/ Freeme.zhangjunjian. 20180117, redesign contact editors.
    private boolean mShouldExistExpansion;
    @Override
    public void onFieldChanged(String column, String value) {
        super.onFieldChanged(column, value);
        // display one of the delete button and expansion button.
        if (mExpansionViewContainer != null) {
            mExpansionViewContainer.setVisibility(GONE);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // setup expansion view while configure(orientation) changed
        if (mShouldExistExpansion) {
            setupExpansionView(mShouldExistExpansion, mHideOptional);
        }
    }
    //*/

    //*/ freeme.zhangyalun, 20180504. New contact, number cannot enter symbols and spaces
    private static class InputKeyListener extends NumberKeyListener {
        private static InputKeyListener sKeyListener;
        public static final char[] CHARACTERS = new char[]{'0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9', '+'};

        @Override
        protected char[] getAcceptedChars() {
            return CHARACTERS;
        }

        public static InputKeyListener getInstance() {
            if (sKeyListener == null) {
                sKeyListener = new InputKeyListener();
            }
            return sKeyListener;
        }

        public int getInputType() {
            return InputType.TYPE_CLASS_PHONE;
        }
    }
    //*/
}
