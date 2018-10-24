package com.freeme.dialer.contacts.merge;

import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.dialer.R;
import com.freeme.contacts.common.utils.FreemeToast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FreemeContactsMerge {

    private final String[] PROJECTION = new String[]{
            Data.CONTACT_ID,
            Data.DISPLAY_NAME,
            Data.DATA1,
            Data.MIMETYPE,
            Phone.TYPE,
            Phone.LABEL,
            Photo.PHOTO,
            RawContacts.ACCOUNT_TYPE
    };
    private final int CONTACT_ID = 0;
    private final int DISPLAY_NAME = 1;
    private final int DATA1 = 2;
    private final int MIMETYPE = 3;
    private final int TYPE = 4;
    private final int LABEL = 5;
    private final int PHOTO_DATA = 6;

    private final String ACCOUNT_NAME_LOCAL_PHONE = "Phone";
    private final String ACCOUNT_TYPE_LOCAL_PHONE = "Local Phone Account";
    private final Handler mHandler = new Handler();

    public void doMerge(Context context, ArrayList<String> contactIds) {
        ConstructContactAsyncTask mergeAsyncTask = new ConstructContactAsyncTask(context, contactIds);
        mergeAsyncTask.execute();
    }

    private void showToast(final Context context, final int msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                FreemeToast.toast(context, context.getResources().getString(msg));
            }
        });
    }

    class ConstructContactAsyncTask extends AsyncTask<Void, Integer, FreemeContactData> {

        Context mContext;
        ArrayList<String> mContactIds;

        ConstructContactAsyncTask(Context context, ArrayList<String> contactIds) {
            this.mContext = context;
            this.mContactIds = contactIds;
        }

        @Override
        protected FreemeContactData doInBackground(Void... voids) {
            FreemeContactData cData = null;

            Cursor cursor = null;
            if (mContactIds != null) {
                int size = mContactIds.size();
                if (size > 1 && size <= 10) {
                    String idStr = TextUtils.join(",", mContactIds);
                    try {
                        String selection = RawContacts.CONTACT_ID + " IN ( " + idStr + " )"
                                + " AND "
                                + RawContacts.ACCOUNT_TYPE + " IS NOT NULL "
                                + " AND "
                                + RawContacts.ACCOUNT_TYPE + " IS NOT '" + ACCOUNT_TYPE_LOCAL_PHONE + "'";
                        cursor = mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                                PROJECTION, selection, null, null);

                        if (cursor != null && cursor.getCount() > 0) {
                            showToast(mContext, R.string.freeme_menu_merge_contacts_only_phone_toast);
                            return cData;
                        }
                        cursor.close();

                        selection = RawContacts.CONTACT_ID + " IN ( " + idStr + " )" + " AND ("
                                + RawContacts.ACCOUNT_TYPE + " IS NULL OR "
                                + RawContacts.ACCOUNT_TYPE + " = '" + ACCOUNT_TYPE_LOCAL_PHONE + "')";
                        cursor = mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                                PROJECTION, selection, null, RawContacts.DISPLAY_NAME_PRIMARY);

                        if (cursor != null && cursor.getCount() > 1) {
                            // if not deleted, the contact is only merged.
                            cData = constructContactsData(cursor);
                        } else {
                            showToast(mContext, R.string.freeme_menu_merge_contacts_least_two);
                        }
                    } catch (Exception e) {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                }
            }
            return cData;
        }

        Set<String> mDisplayNames;
        Set<String> mNickNames;

        private FreemeContactData constructContactsData(Cursor cursor) {
            mDisplayNames = new HashSet<>();
            mNickNames = new HashSet<>();
            FreemeContactData cData = new FreemeContactData();
            cData.setAccountName(ACCOUNT_NAME_LOCAL_PHONE);
            cData.setAccountType(ACCOUNT_TYPE_LOCAL_PHONE);
            while (cursor.moveToNext()) {
                String mimeType = cursor.getString(MIMETYPE);
                long contactId = cursor.getLong(CONTACT_ID);
                if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String displayName = cursor.getString(DISPLAY_NAME);
                    cData.getDisplayNames().put(contactId, displayName);
                    cData.setDisplayName(displayName);
                    mDisplayNames.add(displayName);
                } else if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String phone = cursor.getString(DATA1);
                    if (phone != null) {
                        phone = phone.replaceAll("-", "")
                                .replaceAll(" ", "");
                        if (!TextUtils.isEmpty(phone)) {
                            FreemeContactData.Phone data = new FreemeContactData.Phone(
                                    phone, cursor.getString(TYPE), cursor.getString(LABEL));
                            cData.getPhones().put(phone, data);
                        }
                    }
                } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String email = cursor.getString(DATA1);
                    if (!TextUtils.isEmpty(email)) {
                        FreemeContactData.Email data = new FreemeContactData.Email(
                                email, cursor.getString(TYPE));
                        cData.getEmails().put(email, data);
                    }
                } else if (Im.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String im = cursor.getString(DATA1);
                    if (!TextUtils.isEmpty(im)) {
                        FreemeContactData.IM data = new FreemeContactData.IM(
                                im, cursor.getString(TYPE));
                        cData.getIms().put(im, data);
                    }
                } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String address = cursor.getString(DATA1);
                    if (!TextUtils.isEmpty(address)) {
                        FreemeContactData.Address data = new FreemeContactData.Address(
                                address, cursor.getString(TYPE));
                        cData.getAddresses().put(address, data);
                    }
                } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String note = cursor.getString(DATA1);
                    if (!TextUtils.isEmpty(note)) {
                        FreemeContactData.Note data = new FreemeContactData.Note(note);
                        cData.getNotes().put(note, data);
                    }
                } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String company = cursor.getString(DATA1);
                    if (!TextUtils.isEmpty(company)) {
                        FreemeContactData.Company data = new FreemeContactData.Company(company,
                                cursor.getString(TYPE),
                                "");
                        cData.getCompanies().put(company, data);
                    }
                } else if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    byte[] photo = cursor.getBlob(PHOTO_DATA);
                    FreemeContactData.Photo data = new FreemeContactData.Photo(photo, contactId);
                    cData.getPhotos().put(contactId, data);
                    cData.setPhoto(photo);
                } else if (mimeType.equals(Website.CONTENT_ITEM_TYPE)) {
                    String website = cursor.getString(DATA1);
                    if (!TextUtils.isEmpty(website)) {
                        FreemeContactData.Website data = new FreemeContactData.Website(website);
                        cData.getWebsites().put(website, data);
                    }
                } else if (Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    String nickName = cursor.getString(DATA1);
                    cData.getNickNames().put(contactId, nickName);
                    cData.setNickName(nickName);
                    mNickNames.add(nickName);
                }
            }
            return cData;
        }

        @Override
        protected void onPostExecute(FreemeContactData freemeContactData) {
            if (freemeContactData == null) {
                return;
            }
            if (mDisplayNames.size() > 1 || mNickNames.size() > 1) {
                showDialog(mContext, freemeContactData, mContactIds);
            } else {
                MergeAsyncTask mergeAsyncTask = new MergeAsyncTask(mContext, false, mContactIds);
                mergeAsyncTask.execute(freemeContactData);
            }
            super.onPostExecute(freemeContactData);
        }
    }

    class MergeAsyncTask extends AsyncTask<FreemeContactData, Void, Void> {
        private Context mContext;
        private boolean mUpdate;
        private ArrayList<String> mContactIds;

        MergeAsyncTask(Context context, boolean update, ArrayList<String> contactIds) {
            this.mContext = context;
            this.mUpdate = update;
            this.mContactIds = contactIds;
        }

        @Override
        protected Void doInBackground(FreemeContactData... freemeContactData) {
            deleteAlreadyMergedContacts(mContactIds, mContext);
            merge(mContext, freemeContactData[0], mUpdate);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            FreemeToast.toast(mContext, mContext.getResources().getString(R.string.freeme_menu_merge_contacts_successful));
            super.onPostExecute(aVoid);
        }
    }

    private void showDialog(Context context, FreemeContactData freemeContactData, ArrayList<String> contactIds) {
        FreemeMergeDialogAdapter freemeMergeDialogAdapter = new FreemeMergeDialogAdapter(contactIds, context, freemeContactData);
        freemeMergeDialogAdapter.setDefSelected(0);
        Long defaultItem = Long.valueOf(contactIds.get(0));
        freemeContactData.setNickName(freemeContactData.getNickNames().get(defaultItem));
        freemeContactData.setDisplayName(freemeContactData.getDisplayNames().get(defaultItem));
        if (freemeContactData.getPhotos().containsKey(defaultItem)) {
            freemeContactData.setPhoto(freemeContactData.getPhotos().get(defaultItem).getPhoto());
        }

        LinearLayout linearLayoutMain = new LinearLayout(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayoutMain.setLayoutParams(new ViewGroup.LayoutParams(layoutParams));
        ListView listView = new ListView(context);
        listView.setFadingEdgeLength(0);
        listView.setAdapter(freemeMergeDialogAdapter);
        linearLayoutMain.addView(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                freemeMergeDialogAdapter.setDefSelected(position);
                long key = (Long) parent.getAdapter().getItem(position);
                freemeContactData.setNickName(freemeContactData.getNickNames().get(key));
                freemeContactData.setDisplayName(freemeContactData.getDisplayNames().get(key));
                if (freemeContactData.getPhotos().containsKey(key)) {
                    freemeContactData.setPhoto(freemeContactData.getPhotos().get(key).getPhoto());
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.freeme_menu_merge_contacts_dialog_title));
        builder.setView(linearLayoutMain);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int position) {
                MergeAsyncTask mergeAsyncTask = new MergeAsyncTask(context, false, contactIds);
                mergeAsyncTask.execute(freemeContactData);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private long merge(Context context, FreemeContactData data, boolean update) {
        return update ? update(context, data) : insert(context, data);
    }

    private void deleteAlreadyMergedContacts(ArrayList<String> contactIds, Context context) {
        for (String id : contactIds) {
            final Uri contactUri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI, Long.valueOf(id));
            context.getContentResolver().delete(contactUri,
                    null, null);
        }
    }

    private long insert(Context context, FreemeContactData data) {
        int rawContactId = -1;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation
                .newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, data.getAccountType())
                .withValue(RawContacts.ACCOUNT_NAME, data.getAccountName())
                .build());

        // Name
        if (!TextUtils.isEmpty(data.getDisplayName())) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(StructuredName.GIVEN_NAME, data.getDisplayName())
                    .build());
        }

        // NickName
        if (!TextUtils.isEmpty(data.getNickName())) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
                    .withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
                    .withValue(Nickname.NAME, data.getNickName()).build());
        }

        // Photo
        if (data.getPhoto() != null && data.getPhoto().length > 0) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    .withValue(Photo.PHOTO, data.getPhoto())
                    .build());
        }

        // Phone number
        for (FreemeContactData.Phone phone : data.getPhones().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, phone.getContent())
                    .withValue(Phone.TYPE, phone.getType())
                    .withValue(Phone.LABEL, phone.getLabel())
                    .build());
        }

        // Email
        for (FreemeContactData.Email email : data.getEmails().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                    .withValue(Email.DATA, email.getContent())
                    .withValue(Email.TYPE, email.getType())
                    .build());
        }

        // IM
        for (FreemeContactData.IM im : data.getIms().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE)
                    .withValue(Im.PROTOCOL, im.getType())
                    .withValue(Im.DATA, im.getContent())
                    .build());
        }

        // Address
        for (FreemeContactData.Address address : data.getAddresses().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                    .withValue(StructuredPostal.FORMATTED_ADDRESS, address.getContent())
                    .withValue(StructuredPostal.TYPE, address.getType())
                    .build());
        }

        // Birthday
        for (FreemeContactData.Birthday birthday : data.getBirthdaies().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                    .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
                    .withValue(Event.START_DATE, birthday.getContent())
                    .build());
        }

        // Note
        for (FreemeContactData.Note note : data.getNotes().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, note.getContent())
                    .build());
        }

        // Company
        for (FreemeContactData.Company company : data.getCompanies().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                    .withValue(Organization.TYPE, company.getType())
                    .withValue(Organization.COMPANY, company.getContent())
                    .withValue(Organization.TITLE, company.getPosition())
                    .build());
        }

        // Web
        for (FreemeContactData.Website website : data.getWebsites().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE)
                    .withValue(Website.URL, website.getContent())
                    .build());
        }

        // GroupMembership
        for (FreemeContactData.GroupMembership group : data.getGroups().values()) {
            ops.add(ContentProviderOperation
                    .newInsert(Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                    .withValue(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
                    .withValue(GroupMembership.GROUP_ROW_ID, group.getType())
                    .build());
        }

        if (ops.size() > 0) {
            try {
                ContentProviderResult[] results = context.getContentResolver()
                        .applyBatch(ContactsContract.AUTHORITY, ops);
                if (results.length > 0) {
                    rawContactId = (int) ContentUris.parseId(results[0].uri);
                }
            } catch (Exception e) {
            }

        }
        return rawContactId;
    }

    private long update(Context context, FreemeContactData data) {
        final long rawContactId = data.getId();
        final Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        final Uri contactUri = Uri.withAppendedPath(rawContactUri,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY);

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();

        // Name
        values.clear();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        values.put(StructuredName.GIVEN_NAME, data.getDisplayName());
        resolver.insert(Data.CONTENT_URI, values);

        // Nickname
        values.clear();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Nickname.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
        values.put(Nickname.TYPE, Nickname.TYPE_DEFAULT);
        values.put(Nickname.NAME, data.getNickName());
        resolver.insert(contactUri, values);

        // Photo
        if (data.getPhoto() != null && data.getPhoto().length > 0) {
            values.clear();
            values.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
            values.put(Photo.PHOTO, data.getPhoto());
            resolver.insert(contactUri, values);
        }

        // Phone number
        for (FreemeContactData.Phone phone : data.getPhones().values()) {
            values.clear();
            values.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            values.put(Phone.TYPE, phone.getType());
            values.put(Phone.IS_SUPER_PRIMARY, 1);
            values.put(Phone.NUMBER, phone.getContent());
            values.put(Phone.LABEL, phone.getLabel());
            values.put(Phone.TYPE, phone.getType());
            resolver.insert(contactUri, values);
        }

        // Email
        for (FreemeContactData.Email email : data.getEmails().values()) {
            values.clear();
            values.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            values.put(Email.TYPE, email.getType());
            values.put(Email.DATA, email.getContent());
            resolver.insert(contactUri, values);
        }

        // IM
        for (FreemeContactData.IM im : data.getIms().values()) {
            values.clear();
            values.put(Im.MIMETYPE, Im.CONTENT_ITEM_TYPE);
            values.put(Im.PROTOCOL, im.getType());
            values.put(Im.DATA, im.getContent());
            resolver.insert(contactUri, values);
        }

        // Address
        for (FreemeContactData.Address address : data.getAddresses().values()) {
            values.clear();
            values.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
            values.put(StructuredPostal.TYPE, address.getType());
            values.put(StructuredPostal.FORMATTED_ADDRESS, address.getContent());
            resolver.insert(contactUri, values);
        }

        // Note
        for (FreemeContactData.Note note : data.getNotes().values()) {
            values.clear();
            values.put(Note.MIMETYPE, Note.CONTENT_ITEM_TYPE);
            values.put(Note.NOTE, note.getContent());
            resolver.insert(contactUri, values);
        }

        // Birthday
        for (FreemeContactData.Birthday birthday : data.getBirthdaies().values()) {
            values.clear();
            values.put(Event.MIMETYPE, Event.CONTENT_ITEM_TYPE);
            values.put(Event.TYPE, birthday.getType());
            values.put(Event.START_DATE, birthday.getContent());
            resolver.insert(contactUri, values);
        }

        // Company
        for (FreemeContactData.Company company : data.getCompanies().values()) {
            values.clear();
            values.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
            values.put(Organization.TYPE, company.getType());
            values.put(Organization.DATA, company.getContent());
            values.put(Organization.TITLE, company.getPosition());
            resolver.insert(contactUri, values);
        }

        // Website
        for (FreemeContactData.Website website : data.getWebsites().values()) {
            values.clear();
            values.put(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE);
            values.put(Website.URL, website.getContent());
            resolver.insert(contactUri, values);
        }

        // GroupMembership
        for (FreemeContactData.GroupMembership group : data.getGroups().values()) {
            values.clear();
            values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
            values.put(GroupMembership.GROUP_ROW_ID, group.getType());
            resolver.insert(contactUri, values);
        }

        return rawContactId;
    }
}