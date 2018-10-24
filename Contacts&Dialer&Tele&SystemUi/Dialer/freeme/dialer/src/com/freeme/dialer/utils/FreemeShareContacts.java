package com.freeme.dialer.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import com.android.dialer.R;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.contacts.common.utils.FreemeToast;

import java.util.ArrayList;
import java.util.List;

public class FreemeShareContacts {

    private static final String TAG = "FreemeContactsFragment";
    private static final int MULTI_CHOICE_MAX_COUNT_FOR_SHARE = 1000;
    private static final int MAX_DATA_SIZE = 124 * 1024;
    private static final String ARG_CALLING_ACTIVITY = "CALLING_ACTIVITY";

    private Context mContext;

    public FreemeShareContacts(Context context) {
        mContext = context;
    }

    public void doShare(ArrayList<Uri> uris, String callingActivity) {
        if (uris == null || uris.size() == 0) {
            FreemeLogUtils.w(TAG, "[doShareVisibleContacts]error, return,idArrayUriLookUp = " + uris);
            return;
        }
        final int selectedCount = uris.size();
        if (selectedCount == 0) {
            FreemeLogUtils.w(TAG, "[doShare]selectedCount = 0");
            FreemeToast.toast(mContext, R.string.multichoice_no_select_alert);
            return;
        } else if (selectedCount > MULTI_CHOICE_MAX_COUNT_FOR_SHARE) {
            FreemeLogUtils.w(TAG, "[doShare]selectedCount > getMultiChoiceLimitCount, selectedCount = " + selectedCount);
            String msg = mContext.getString(R.string.share_contacts_limit, MULTI_CHOICE_MAX_COUNT_FOR_SHARE);
            FreemeToast.toast(mContext, msg);
            return;
        }

        doShareVisibleContacts(uris, callingActivity);
    }

    private boolean doShareVisibleContacts(ArrayList<Uri> uris, String callingActivity) {
        FreemeLogUtils.d(TAG, "[shareSelectedContacts]...");
        final StringBuilder uriListBuilder = new StringBuilder();
        for (Uri lookupUri : uris) {
            if (lookupUri == null) {
                continue;
            }
            final List<String> pathSegments = lookupUri.getPathSegments();
            if (pathSegments.size() < 2) {
                continue;
            }
            final String lookupKey = pathSegments.get(pathSegments.size() - 2);
            if (uriListBuilder.length() > 0) {
                uriListBuilder.append(':');
            }
            uriListBuilder.append(Uri.encode(lookupKey));
        }
        int dataSize = uriListBuilder.length();
        if (dataSize == 0) {
            return false;
        }
        final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_MULTI_VCARD_URI,
                Uri.encode(uriListBuilder.toString()));
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(ARG_CALLING_ACTIVITY, callingActivity);
        try {
            final CharSequence chooseTitle = mContext.getText(R.string.title_share_via);
            final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

            FreemeLogUtils.i(TAG, "[doShareVisibleContacts] dataSize : " + dataSize);
            if (dataSize < MAX_DATA_SIZE) {
                mContext.startActivity(chooseIntent);
                return true;
            } else {
                FreemeToast.toast(mContext, R.string.share_too_large);
            }
        } catch (final ActivityNotFoundException ex) {
            FreemeLogUtils.w(TAG, "[doShareVisibleContacts]ActivityNotFoundException = " + ex);
            FreemeToast.toast(mContext, R.string.share_error);
        }
        return false;
    }
}
