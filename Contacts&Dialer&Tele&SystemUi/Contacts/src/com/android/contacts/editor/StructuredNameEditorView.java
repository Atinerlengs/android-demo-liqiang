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

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.android.contacts.R;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.dataitem.StructuredNameDataItem;
import com.android.contacts.util.NameConverter;

import com.mediatek.contacts.editor.ContactEditorUtilsEx;
import com.mediatek.contacts.util.Log;

import java.util.Map;

/**
 * A dedicated editor for structured name.
 */
public class StructuredNameEditorView extends TextFieldsEditorView {

    private static final String TAG = "StructuredNameEditorView";
    private StructuredNameDataItem mSnapshot;
    private boolean mChanged;

    public StructuredNameEditorView(Context context) {
        super(context);
    }

    public StructuredNameEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StructuredNameEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final Resources res = getResources();
        mCollapseButtonDescription = res
                .getString(R.string.collapse_name_fields_description);
        mExpandButtonDescription = res
                .getString(R.string.expand_name_fields_description);
        //*/freeme.zhangjunjian, 20180130. redesign contact editors
        mExpansionViewContainer.setVisibility(GONE);
        mDelete.setVisibility(View.GONE);
        //*/
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly,
            ViewIdGenerator vig) {
        Log.d(TAG, "[setValues] DataKind=" + kind
              + ", entry=" + entry + ", state=" + state);
        super.setValues(kind, entry, state, readOnly, vig);
        /*/ freeme.liqiang, 20180502. fix contacts stopped running
        if (mSnapshot == null) {
        /*/
        if (mSnapshot == null && entry != null) {
        //*/
            mSnapshot = (StructuredNameDataItem) DataItem.createFrom(
                    new ContentValues(getValues().getCompleteValues()));
            mChanged = entry.isInsert();
        } else {
            mChanged = false;
        }
        updateEmptiness();
        // Right alien with rest of the editors. As this view has an extra expand/collapse view on
        // the right, we need to free the space from deleteContainer
        /*/freeme.zhangjunjian, 20180205. redesign contact editors
        mDeleteContainer.setVisibility(View.GONE);
        /*/
        mDelete.setVisibility(View.GONE);
        //*/
    }

    @Override
    public void onFieldChanged(String column, String value) {
        Log.d(TAG, "[onFieldChanged] beg, column=" + column + ", value=" + value);
        if (!isFieldChanged(column, value)) {
            return;
        }

        // First save the new value for the column.
        saveValue(column, value);
        mChanged = true;

        //*/ freeme.linqingwei, 20180409. redesign contacts editor for 8.1 style.
        // Next make sure the display name and the structured name are synced
        rebuildStructuredName(getValues());
        //*/

        // Then notify the listener.
        notifyEditorListener();
    }

    /**
     * Returns the display name currently displayed in the editor.
     */
    public String getDisplayName() {
        return NameConverter.structuredNameToDisplayName(getContext(),
                getValues().getCompleteValues());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.mChanged = mChanged;
        //*/ freeme.linqingwei, 20180322. redesign contact account editor.
        // in case of JE.
        if (mSnapshot != null) {
            state.mSnapshot = mSnapshot.getContentValues();
        }
        /*/
        state.mSnapshot = mSnapshot.getContentValues();
        //*/
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.mSuperState);

        mChanged = ss.mChanged;
        /* M: [Google Issue]ALPS00477285
         * Original code: @{
        mSnapshot = (StructuredNameDataItem) DataItem.createFrom(ss.mSnapshot);
         * @}
         * New code: @{ */
        /*/ freeme.liqiang, 20180502. fix contacts stopped running
        mSnapshot = ContactEditorUtilsEx.restoreStructuredNameDataItem(ss.mSnapshot);
        /*/
        if (ss.mSnapshot != null) {
            mSnapshot = ContactEditorUtilsEx.restoreStructuredNameDataItem(ss.mSnapshot);
        }
        //*/
        /* @} */
    }

    private static class SavedState implements Parcelable {
        public boolean mChanged;
        public ContentValues mSnapshot;
        public Parcelable mSuperState;

        SavedState(Parcelable superState) {
            mSuperState = superState;
        }

        private SavedState(Parcel in) {
            ClassLoader loader = getClass().getClassLoader();
            mSuperState = in.readParcelable(loader);

            mChanged = in.readInt() != 0;
            mSnapshot = in.readParcelable(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(mSuperState, 0);

            out.writeInt(mChanged ? 1 : 0);
            out.writeParcelable(mSnapshot, 0);
        }

        @SuppressWarnings({"unused"})
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

        @Override
        public int describeContents() {
            return 0;
        }
    }

    //*/ freeme.linqingwei, 20180409. redesign contacts editor for 8.1 style.
    private void rebuildStructuredName(ValuesDelta values) {
        String displayName = values.getDisplayName();
        Map<String, String> structuredNameMap = NameConverter.displayNameToStructuredName(
                getContext(), displayName);
        for (String field : structuredNameMap.keySet()) {
            values.put(field, structuredNameMap.get(field));
        }
    }
    //*/
}
