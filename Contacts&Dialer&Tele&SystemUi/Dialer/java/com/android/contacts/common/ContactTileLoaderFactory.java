/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.contacts.common;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.contacts.common.preference.ContactsPreferences;

import com.mediatek.provider.MtkContactsContract.ContactsColumns;
/**
 * Used to create {@link CursorLoader} which finds contacts information from the strequents table.
 *
 * <p>Only returns contacts with phone numbers.
 */
public final class ContactTileLoaderFactory {

    private static final String TAG = "ContactTileLoaderFactory";
    private static final int DISPLAY_NAME = 1;
    private static final int DISPLAY_ORDER_PRIMARY = 1;
    private static final int DISPLAY_ORDER_ALTERNATIVE = 2;
    private static final int SORT_ORDER_PRIMARY = 1;
    private static final int SORT_ORDER_ALTERNATIVE = 2;
    /**
     * The _ID field returned for strequent items actually contains data._id instead of contacts._id
     * because the query is performed on the data table. In order to obtain the contact id for
     * strequent items, use Phone.contact_id instead.
     */
    @VisibleForTesting
    public static final String[] COLUMNS_PHONE_ONLY =
        new String[] {
            Contacts._ID,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.STARRED,
            Contacts.PHOTO_URI,
            Contacts.LOOKUP_KEY,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.IS_SUPER_PRIMARY,
            Contacts.PINNED,
            Phone.CONTACT_ID,
            Contacts.DISPLAY_NAME_ALTERNATIVE,
            // M: add for contacts extensions
            ContactsColumns.INDICATE_PHONE_SIM,
        };

    public static CursorLoader createStrequentPhoneOnlyLoader(Context context) {
        /*/ freeme.zhaozehong, 20180328. for freemeOS, only show stared contacts
        Uri uri =
            Contacts.CONTENT_STREQUENT_URI
                .buildUpon()
                .appendQueryParameter(ContactsContract.STREQUENT_PHONE_ONLY, "true")
                .build();
        /*/
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_STREQUENT_URI, "stared")
                .buildUpon()
                .appendQueryParameter(ContactsContract.STREQUENT_PHONE_ONLY, "true")
                .build();
        //*/

        /** M: Bug Fix for CR ALPS00319593 @{ */
        CursorLoader cursorLoader;
        if (isMtkPhoneBookSupport()) {
            cursorLoader = new CursorLoader(context, uri, COLUMNS_PHONE_ONLY,
                    ContactsColumns.INDICATE_PHONE_SIM + "=-1 ",
                    null, null);
        } else {
            cursorLoader = new CursorLoader(context, uri, COLUMNS_PHONE_ONLY,
                    null, null, null);
        }
        fixSortOrderByPreference(cursorLoader, DISPLAY_NAME, context);
        return cursorLoader;
        /** @} */
    }

    /** M: Bug Fix for CR ALPS00319593 @{ */
    private static boolean isMtkPhoneBookSupport() {
        try {
            Class<?> iccPhoneBook = Class.forName(
                  "com.mediatek.internal.telephony.phb.IMtkIccPhoneBook");
            iccPhoneBook.getDeclaredMethod("getUsimAasList", int.class);
            iccPhoneBook.getDeclaredMethod("getUsimGroups", int.class);
            iccPhoneBook.getDeclaredMethod("hasSne", int.class);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Android phonebook class not found!");
            return false;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "MTK phonebook api not found!");
            return false;
        }
        return true;
    }

    private static void fixSortOrderByPreference(CursorLoader cursorLoader, int displayNameIndex,
            Context context) {
        String[] project = cursorLoader.getProjection();
        if (project == null || project.length < displayNameIndex) {
            Log.d(TAG, "[fixSortByPreference] project is null or not right:" + project);
            return;
        }

        ContactsPreferences preferences = new ContactsPreferences(context);

        // for display name sort order
        int displayNameSortOrder = preferences.getDisplayOrder();
        switch (displayNameSortOrder) {
            case DISPLAY_ORDER_PRIMARY:
                project[displayNameIndex] = Contacts.DISPLAY_NAME_PRIMARY;
                break;
            case DISPLAY_ORDER_ALTERNATIVE:
                project[displayNameIndex] = Contacts.DISPLAY_NAME_ALTERNATIVE;
                break;
            default:
                Log.w(TAG, "[fixSortByPreference] displayNameSortOrder is error:"
                    + displayNameSortOrder);
        }

        // for contacts sort order
        int contactsSoryOrder = preferences.getSortOrder();
        String order = cursorLoader.getSortOrder();
        if (order != null) {
            Log.w(TAG, "[fixSortByPreference] The CursorLoader already has sort order:"
                + order);
            return;
        }
        Log.i(TAG, "[fixSortByPreference]displayNameSortOrder:" + displayNameSortOrder
                + ",contactsSoryOrder = " + contactsSoryOrder);
        switch (contactsSoryOrder) {
            case SORT_ORDER_PRIMARY:
                cursorLoader.setSortOrder(Contacts.SORT_KEY_PRIMARY);
                break;
            case SORT_ORDER_ALTERNATIVE:
                cursorLoader.setSortOrder(Contacts.SORT_KEY_ALTERNATIVE);
                break;
            default:
                Log.w(TAG, "[fixSortByPreference] Contacts SortOrder is error:"
                    + contactsSoryOrder);
        }
    }
    /** @} */
}
