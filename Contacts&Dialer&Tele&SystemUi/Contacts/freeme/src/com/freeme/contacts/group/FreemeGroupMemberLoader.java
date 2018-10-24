package com.freeme.contacts.group;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.group.GroupMembersFragment;
import com.android.contacts.group.GroupUtil;
import com.freeme.contacts.common.utils.FreemeToast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreemeGroupMemberLoader implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static int LOADER_ID_LOAD_GROUP_MENBER = 0x1112;
    private final static String KEY_GRRUP_ID_ARRAY = "KEY_GRRUP_ID_ARRAY";
    private final static String KEY_GRRUP_SEND_SCHEME = "KEY_GRRUP_SEND_SCHEME";

    private final Uri uri;
    private final String selection;
    private Fragment mFragment;
    private final String[] selectionArgs = new String[1];
    private GroupMemberListLoadCallBack mCallBack;
    private String mScheme;

    public FreemeGroupMemberLoader(Fragment fragment, GroupMemberListLoadCallBack callBack) {
        mFragment = fragment;
        mCallBack = callBack;
        uri = ContactsContract.Data.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(ContactsContract.Directory.DEFAULT))
                .appendQueryParameter(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build();

        selection = ContactsContract.Data.MIMETYPE + " = ? " + " AND "
                + ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " in ";
        selectionArgs[0] = ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE;
    }

    public void loaderData(long[] groupIdArr, String scheme) {
        if (groupIdArr != null && groupIdArr.length > 0) {
            Bundle args = new Bundle();
            args.putLongArray(KEY_GRRUP_ID_ARRAY, groupIdArr);
            args.putString(KEY_GRRUP_SEND_SCHEME, scheme);
            mFragment.getLoaderManager().restartLoader(LOADER_ID_LOAD_GROUP_MENBER, args, this);
        }
    }

    @Override
    public CursorLoader onCreateLoader(int id, Bundle args) {
        StringBuffer stringBuffer = new StringBuffer(selection);
        stringBuffer.append("(");
        if (args != null) {
            long[] groupIdArr = args.getLongArray(KEY_GRRUP_ID_ARRAY);
            for (int i = 0; i < groupIdArr.length; i++) {
                if (i > 0) {
                    stringBuffer.append(",");
                }
                stringBuffer.append(groupIdArr[i]);
            }
        }
        stringBuffer.append(")");

        mScheme = args.getString(KEY_GRRUP_SEND_SCHEME);

        String[] clus = new String[]{ContactsContract.Data.CONTACT_ID};

        return new CursorLoader(mFragment.getContext(), uri, clus, stringBuffer.toString(),
                selectionArgs, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            return;
        }
        long[] ids = new long[cursor.getCount()];
        int idx = 0;
        while (cursor.moveToNext()) {
            final long contactId = cursor.getLong(0);
            ids[idx++] = contactId;
        }

        boolean isSendMail = ContactsUtils.SCHEME_MAILTO.equals(mScheme);
        final String sIds = GroupUtil.convertArrayToString(ids);
        final String select = (ContactsUtils.SCHEME_MAILTO.equals(mScheme)
                ? GroupMembersFragment.Query.EMAIL_SELECTION
                : GroupMembersFragment.Query.PHONE_SELECTION)
                + " AND " + ContactsContract.Data.CONTACT_ID + " IN (" + sIds + ")";
        final ContentResolver contentResolver = mFragment.getContext().getContentResolver();
        final Cursor c = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                isSendMail ? GroupMembersFragment.Query.EMAIL_PROJECTION
                        : GroupMembersFragment.Query.PHONE_PROJECTION,
                select, null, null);

        if (c == null) {
            return;
        }

        final Map<Long, ContactDataHelperClass> contactMap = new HashMap<>();
        final List<String> itemList = new ArrayList<>();
        while (c.moveToNext()) {
            final long contactId = c.getLong(GroupMembersFragment.Query.CONTACT_ID);
            final String itemId = c.getString(GroupMembersFragment.Query.ITEM_ID);
            final boolean isPrimary = c.getInt(GroupMembersFragment.Query.PRIMARY) != 0;
            final int timesUsed = c.getInt(GroupMembersFragment.Query.TIMES_USED);
            final String data = c.getString(GroupMembersFragment.Query.DATA1);
            if (!TextUtils.isEmpty(data)) {
                final ContactDataHelperClass contact;
                if (!contactMap.containsKey(contactId)) {
                    contact = new ContactDataHelperClass();
                    contactMap.put(contactId, contact);
                } else {
                    contact = contactMap.get(contactId);
                }
                contact.addItem(contactId, itemId, timesUsed, isPrimary);
                itemList.add(data);
            }
        }

        if (itemList.size() == 0 || contactMap.size() < ids.length) {
            FreemeToast.toast(mFragment.getContext(), isSendMail
                    ? mFragment.getString(R.string.groupSomeContactsNoEmailsToast)
                    : mFragment.getString(R.string.groupSomeContactsNoPhonesToast));
        }

        if (mCallBack != null) {
            mCallBack.onLoadFinish(contactMap, itemList);
            mCallBack = null;
        }

        mFragment.getLoaderManager().destroyLoader(LOADER_ID_LOAD_GROUP_MENBER);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public interface GroupMemberListLoadCallBack {
        public void onLoadFinish(Map<Long, ContactDataHelperClass> contactMap, List<String> itemList);
    }

    public class ContactDataHelperClass {

        private List<String> items = new ArrayList<>();
        private long contactId;
        private String mostUsedItemId = null;
        private int mostUsedTimes;
        private String primaryItemId = null;

        public void addItem(long contactId, String item, int timesUsed, boolean primaryFlag) {
            this.contactId = contactId;
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

        public long getContactId() {
            return contactId;
        }
    }
}
