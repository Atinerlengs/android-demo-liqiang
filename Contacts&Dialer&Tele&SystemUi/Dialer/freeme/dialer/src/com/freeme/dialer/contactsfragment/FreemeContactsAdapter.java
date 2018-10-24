package com.freeme.dialer.contactsfragment;

import android.app.LoaderManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.IntDef;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.contactsfragment.R;
import com.freeme.contacts.common.utils.FreemeToast;
import com.freeme.dialer.callback.IFreemeMultiSelectCallBack;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;

class FreemeContactsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int UNKNOWN_VIEW_TYPE = 0;
    private static final int ADD_CONTACT_VIEW_TYPE = 1;
    private static final int CONTACT_VIEW_TYPE = 2;

    /**
     * An Enum for the different row view types shown by this adapter.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UNKNOWN_VIEW_TYPE, ADD_CONTACT_VIEW_TYPE, CONTACT_VIEW_TYPE})
    @interface ContactsViewType {
    }

    private final ArrayMap<FreemeContactViewHolder, Integer> holderMap = new ArrayMap<>();
    private final Context context;
    private Cursor cursor;

    // List of contact sublist headers
    private String[] headers;

    // Number of contacts that correspond to each header in {@code headers}.
    private int[] counts;

    private LoaderManager mLoaderManager;

    private boolean mIsMultiMode;

    private boolean isSearchMode;

    FreemeContactsAdapter(Context context, LoaderManager loaderManager, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
        mLoaderManager = loaderManager;
        if (cursor != null) {
            headers = cursor.getExtras().getStringArray(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
            counts = cursor.getExtras().getIntArray(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, @ContactsViewType int viewType) {
        switch (viewType) {
            case ADD_CONTACT_VIEW_TYPE:
                return new FreemeContactsListHeaderViewHolder(mLoaderManager,
                        LayoutInflater.from(context).inflate(
                                R.layout.freeme_fragment_contacts_list_header,
                                parent,
                                false));
            case CONTACT_VIEW_TYPE:
                return new FreemeContactViewHolder(
                        LayoutInflater.from(context).inflate(
                                R.layout.freeme_contact_row,
                                parent,
                                false),
                        new ContactsItemLongClickListener(),
                        new ContactsItemSelectListener());
            case UNKNOWN_VIEW_TYPE:
            default:
                throw Assert.createIllegalStateFailException("Invalid view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof FreemeContactsListHeaderViewHolder) {
            return;
        }

        FreemeContactViewHolder contactViewHolder = (FreemeContactViewHolder) viewHolder;
        holderMap.put(contactViewHolder, position);
        // Cursor should be offset by 1 because of header row
        cursor.moveToPosition(position - (!(mIsMultiMode || isSearchMode) ? 1 : 0));

        long contactId = cursor.getLong(FreemeContactsCursorLoader.CONTACT_ID);
        String name = getDisplayName(cursor);
        String header = getHeaderString(position);
        Uri contactUri = getContactUri(cursor);
        int subId = cursor.getInt(FreemeContactsCursorLoader.CONTACT_INDICATE_PHONE_SIM);
        int account_idx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE);
        String accountType = cursor.getString(account_idx);

        contactViewHolder.setIsMultiChoiceMode(mIsMultiMode);
        contactViewHolder.setContactId(contactId);
        contactViewHolder.setIsSelected(mSelectedContactIdList.contains(contactId));

        // Always show the view holder's header if it's the first item in the list. Otherwise, compare
        // it to the previous element and only show the anchored header if the row elements fall into
        // the same sublists.
        boolean showHeader = position == 0 || !header.equals(getHeaderString(position - 1));
        contactViewHolder.bind(header, name, contactUri, showHeader, subId, accountType,
                isLastItemInGroup(position));
    }

    @Override
    public @ContactsViewType
    int getItemViewType(int position) {
        if (!mHasContactsPermissions) {
            return CONTACT_VIEW_TYPE;
        }
        return !(mIsMultiMode || isSearchMode) && position == 0 ? ADD_CONTACT_VIEW_TYPE : CONTACT_VIEW_TYPE;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder contactViewHolder) {
        super.onViewRecycled(contactViewHolder);
        if (contactViewHolder instanceof FreemeContactViewHolder) {
            holderMap.remove(contactViewHolder);
        }
    }

    public void refreshHeaders() {
        for (FreemeContactViewHolder holder : holderMap.keySet()) {
            int position = holderMap.get(holder);
            boolean showHeader = position == 0
                    || !getHeaderString(position).equals(getHeaderString(position - 1));
            int visibility = showHeader ? View.VISIBLE : View.GONE;
            holder.getHeaderView().setVisibility(visibility);
        }
    }

    @Override
    public int getItemCount() {
        if (!mHasContactsPermissions) {
            return 0;
        }
        return (cursor == null ? 0 : cursor.getCount())
                + (!(mIsMultiMode || isSearchMode) ? 1 : 0); // list header
    }

    private static String getDisplayName(Cursor cursor) {
        return cursor.getString(FreemeContactsCursorLoader.CONTACT_DISPLAY_NAME);
    }

    private static Uri getContactUri(Cursor cursor) {
        long contactId = cursor.getLong(FreemeContactsCursorLoader.CONTACT_ID);
        String lookupKey = cursor.getString(FreemeContactsCursorLoader.CONTACT_LOOKUP_KEY);
        return ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
    }

    public String getHeaderString(int position) {
        if (!(mIsMultiMode || isSearchMode)) {
            if (position == 0) {
                return "+";
            }
            position--;
        }

        int index = -1;
        int sum = 0;
        while (sum <= position) {
            ///M: TODO There is a ArrayIndexOutofBoundsException about counts[++index]. Have not found
            // the root cause now. So add a protection to avoid Exception. need to remove it after found
            // the root cause. @{
            if ((index + 1) >= counts.length) {
                int total = 0;
                for (int c : counts) {
                    total += c;
                }
                LogUtil.e("FreemeContactsAdapter.getHeaderString", "headers=" + Arrays.toString(headers) +
                        " \n counts=" + Arrays.toString(counts) + " \n SUM of counts = " + total +
                        "cursor count = " + cursor.getCount());
                if (index >= 0) {
                    return headers[index];
                } else {
                    return "";
                }
            }
            ///M: @}
            sum += counts[++index];
        }
        return headers[index];
    }

    public int getHeaderPosition(int index) {
        int init = !(mIsMultiMode || isSearchMode) ? 1 : 0;
        if (index <= 0) {
            return init;
        }
        int sum = 0;
        for (int i = 0; i < index; i++) {
            if (i < counts.length) {
                sum += counts[i];
            }
        }
        return sum + init;
    }

    public boolean isLastItemInGroup(int position) {
        if (!(mIsMultiMode || isSearchMode)) {
            if (position == 0) {
                return true;
            }
            position--;
        }

        int lastItem = getItemCount() - 1;
        if (!(mIsMultiMode || isSearchMode)) { //list header
            lastItem -= 1;
        }
        int sum = 0;
        int last_idx;
        for (int c : counts) {
            sum += c;
            last_idx = sum - 1;
            if (position == last_idx && position != lastItem) {
                return true;
            } else if (position < last_idx) {
                return false;
            }
        }
        return false;
    }

    public String[] getHeaders() {
        return headers;
    }

    private IFreemeMultiSelectCallBack mFreemeMultiSelectCallBack;
    public void setFreemeMultiSelectCallBack(IFreemeMultiSelectCallBack multiSelectCallBack) {
        this.mFreemeMultiSelectCallBack = multiSelectCallBack;
    }

    public boolean isMultiMode() {
        return mIsMultiMode;
    }

    public void setMultiMode(boolean isMultiMode) {
        mIsMultiMode = isMultiMode;
        if (mFreemeMultiSelectCallBack != null) {
            mFreemeMultiSelectCallBack.isInMulitMode(mIsMultiMode);
            mFreemeMultiSelectCallBack.onSelectedCount(0);
        }
        mSelectedContactIdList.clear();
        mSelectedContactUriList.clear();
        notifyDataSetChanged();
    }

    interface IItemLongClickListener {
        void onLongClick();
    }

    interface IItemSelectListener {
        boolean onItemSelect(long contactId, Uri contactUri);
    }

    class ContactsItemLongClickListener implements IItemLongClickListener {
        @Override
        public void onLongClick() {
            if (!mIsMultiMode) {
                setMultiMode(true);
            }
        }
    }

    class ContactsItemSelectListener implements IItemSelectListener {
        @Override
        public boolean onItemSelect(long contactId, Uri contactUri) {
            if (mSelectedContactIdList.contains(contactId)) {
                mSelectedContactIdList.remove(contactId);
                mSelectedContactUriList.remove(contactUri);
                if (mFreemeMultiSelectCallBack != null) {
                    mFreemeMultiSelectCallBack.onSelectedCount(mSelectedContactIdList.size());
                }
                return true;
            } else {
                if (mSelectedContactIdList.size() >= SELECT_MAX_LIMIT) {
                    FreemeToast.toast(context,
                            context.getString(R.string.freeme_not_add_more_recipients, SELECT_MAX_LIMIT));
                    return true;
                }
                mSelectedContactIdList.add(contactId);
                mSelectedContactUriList.add(contactUri);
                mFreemeMultiSelectCallBack.onSelectedCount(mSelectedContactIdList.size());
                return true;
            }
        }
    }

    public void allSelecteOrNot() {
        if (mIsMultiMode && cursor != null && cursor.getCount() > 0) {
            final int selectCount = mSelectedContactIdList.size();
            mSelectedContactIdList.clear();
            mSelectedContactUriList.clear();
            if (selectCount != getItemCount()) {
                cursor.moveToFirst();
                long contactId;
                String lookupKey;
                do {
                    contactId = cursor.getLong(FreemeContactsCursorLoader.CONTACT_ID);
                    lookupKey = cursor.getString(FreemeContactsCursorLoader.CONTACT_LOOKUP_KEY);
                    mSelectedContactIdList.add(contactId);
                    mSelectedContactUriList.add(ContactsContract.Contacts
                            .getLookupUri(contactId, lookupKey));
                    if (mSelectedContactIdList.size() >= SELECT_MAX_LIMIT) {
                        FreemeToast.toast(context,
                                context.getString(R.string.freeme_not_add_more_recipients, SELECT_MAX_LIMIT));
                        break;
                    }
                } while (cursor.moveToNext());
            }

            notifyDataSetChanged();

            mFreemeMultiSelectCallBack.onSelectedCount(mSelectedContactIdList.size());
        }
    }

    private static final int SELECT_MAX_LIMIT = 1000;
    private ArrayList<Long> mSelectedContactIdList = new ArrayList<>();
    private ArrayList<Uri> mSelectedContactUriList = new ArrayList<>();

    public void setSelectedList(ArrayList<Long> selectedContactIdList) {
        mSelectedContactIdList.clear();
        mSelectedContactUriList.clear();
        if (selectedContactIdList != null && cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            long contactId;
            String lookupKey;
            do {
                contactId = cursor.getLong(FreemeContactsCursorLoader.CONTACT_ID);
                if (selectedContactIdList.contains(contactId)) {
                    mSelectedContactIdList.add(contactId);
                    lookupKey = cursor.getString(FreemeContactsCursorLoader.CONTACT_LOOKUP_KEY);
                    mSelectedContactUriList.add(ContactsContract.Contacts
                            .getLookupUri(contactId, lookupKey));
                    if (mSelectedContactIdList.size() >= SELECT_MAX_LIMIT) {
                        FreemeToast.toast(context,
                                context.getString(R.string.freeme_not_add_more_recipients, SELECT_MAX_LIMIT));
                        break;
                    }
                }
            } while (cursor.moveToNext());

            notifyDataSetChanged();

            selectedContactIdList.clear();
        }
        mFreemeMultiSelectCallBack.onSelectedCount(mSelectedContactIdList.size());
    }

    public ArrayList<Long> getSelectedContactIdList() {
        return mSelectedContactIdList;
    }

    public ArrayList<Uri> getSelectedContactUriList() {
        return mSelectedContactUriList;
    }

    public void changeCursor(Cursor cursor) {
        if (cursor == this.cursor) {
            return;
        }

        this.cursor = cursor;

        if (cursor != null) {
            headers = cursor.getExtras().getStringArray(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
            counts = cursor.getExtras().getIntArray(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
        }
        notifyDataSetChanged();
    }

    public void setSearchMode(boolean searchMode) {
        isSearchMode = searchMode;
        notifyDataSetChanged();
    }

    private boolean mHasContactsPermissions;
    public void setHasContactsPermissions(boolean hasContactsPermissions) {
        mHasContactsPermissions = hasContactsPermissions;
    }

    private static final String KEY_MULIT_CHOICE_MODE = "mulit_choice_mode";

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_MULIT_CHOICE_MODE, isMultiMode());
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mIsMultiMode = savedInstanceState.getBoolean(KEY_MULIT_CHOICE_MODE);
    }
}
