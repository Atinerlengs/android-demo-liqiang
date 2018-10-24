package com.freeme.dialer.contactsfragment;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.provider.MtkContactsContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class FreemeContactsCursorLoader extends CursorLoader {

    public static final int CONTACT_ID = 0;
    public static final int CONTACT_DISPLAY_NAME = 1;
    public static final int CONTACT_PHOTO_ID = 2;
    public static final int CONTACT_PHOTO_URI = 3;
    public static final int CONTACT_LOOKUP_KEY = 4;

    /// M:[portable][MTK SIM Contacts feature] @{
    public static final int CONTACT_INDICATE_PHONE_SIM = 5;
    public static final int CONTACT_IS_SDN_CONTACT = 6;

    public static String[] CONTACTS_PROJECTION_DISPLAY_NAME_PRIMARY =
            new String[]{
                    ContactsContract.Contacts._ID, // 0
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, // 1
                    ContactsContract.Contacts.PHOTO_ID, // 2
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI, // 3
                    ContactsContract.Contacts.LOOKUP_KEY, // 4
            };

    /// M:[portable][MTK SIM Contacts feature] @{
    static {
        List<String> projectionList = new ArrayList<>(
                Arrays.asList(CONTACTS_PROJECTION_DISPLAY_NAME_PRIMARY));
        if (DialerFeatureOptions.isSimContactsSupport()) {
            projectionList.add(MtkContactsContract.ContactsColumns.INDICATE_PHONE_SIM);   //5
            projectionList.add(MtkContactsContract.ContactsColumns.IS_SDN_CONTACT);   //6
        }
        projectionList.add(ContactsContract.RawContacts.ACCOUNT_TYPE);
        CONTACTS_PROJECTION_DISPLAY_NAME_PRIMARY = projectionList
                .toArray(new String[projectionList.size()]);
    }
    /// @}

    public static /*final*/ String[] CONTACTS_PROJECTION_DISPLAY_NAME_ALTERNATIVE =
            new String[]{
                    ContactsContract.Contacts._ID, // 0
                    ContactsContract.Contacts.DISPLAY_NAME_ALTERNATIVE, // 1
                    ContactsContract.Contacts.PHOTO_ID, // 2
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI, // 3
                    ContactsContract.Contacts.LOOKUP_KEY, // 4
            };

    /// M:[portable][MTK SIM Contacts feature] @{
    static {
        List<String> projectionList = new ArrayList<>(
                Arrays.asList(CONTACTS_PROJECTION_DISPLAY_NAME_ALTERNATIVE));
        if (DialerFeatureOptions.isSimContactsSupport()) {
            projectionList.add(MtkContactsContract.ContactsColumns.INDICATE_PHONE_SIM);   //5
            projectionList.add(MtkContactsContract.ContactsColumns.IS_SDN_CONTACT);   //6
        }
        projectionList.add(ContactsContract.RawContacts.ACCOUNT_TYPE);
        CONTACTS_PROJECTION_DISPLAY_NAME_ALTERNATIVE = projectionList
                .toArray(new String[projectionList.size()]);
    }
    /// @}


    private FreemeContactsCursorLoader(Context context, String[] contactProjection, String sortKey, String searchStr) {
        super(context, getUri(searchStr),
                contactProjection,
                contactProjection[CONTACT_DISPLAY_NAME] + " IS NOT NULL AND "
                        + ContactsContract.RawContacts.DELETED + " != 1",
                null,
                sortKey + " ASC");
    }

    private static final Uri ENTERPRISE_CONTENT_FILTER_URI =
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, "filter_enterprise");

    private static Uri getUri(String searchStr) {
        Uri.Builder builder;
        if (TextUtils.isEmpty(searchStr)) {
            builder = ContactsContract.Contacts.CONTENT_URI
                    .buildUpon();
        } else {
            builder = ENTERPRISE_CONTENT_FILTER_URI
                    .buildUpon()
                    .appendPath(searchStr)
                    .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                            String.valueOf(ContactsContract.Directory.DEFAULT))
                    .appendQueryParameter(ContactsContract.SearchSnippets.DEFERRED_SNIPPETING_KEY, "1");
        }
        return builder.appendQueryParameter(ContactsContract.Contacts.EXTRA_ADDRESS_BOOK_INDEX, "true")
                .build();
    }

    public static FreemeContactsCursorLoader createInstance(boolean displayOrderPrimary,
                                                            Context context, String sortKey,
                                                            String searchStr) {
        if (displayOrderPrimary) {
            return new FreemeContactsCursorLoader(context, CONTACTS_PROJECTION_DISPLAY_NAME_PRIMARY,
                    sortKey, searchStr);
        } else {
            return new FreemeContactsCursorLoader(context, CONTACTS_PROJECTION_DISPLAY_NAME_ALTERNATIVE,
                    sortKey, searchStr);
        }
    }
}
