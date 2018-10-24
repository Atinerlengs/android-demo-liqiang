package com.freeme.dialer.contacts;

import android.app.IntentService;
import android.app.Notification;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;

import com.android.dialer.R;
import com.android.dialer.util.PermissionsUtil;
import com.freeme.contacts.common.utils.FreemeLogUtils;
import com.freeme.contacts.common.utils.FreemeToast;
import com.freeme.dialer.contacts.list.service.FreemeMultiChoiceService;

import static android.Manifest.permission.WRITE_CONTACTS;

public class FreemeContactDeleteService extends IntentService {
    private final static String TAG = "FreemeContactDeleteService";

    public static final String ACTION_DELETE_CONTACT = "delete";
    public static final String ACTION_DELETE_MULTIPLE_CONTACTS = "deleteMultipleContacts";
    public static final String EXTRA_CONTACT_URI = "contactUri";
    public static final String EXTRA_CONTACT_IDS = "contactIds";
    public static final String EXTRA_DISPLAY_NAME_ARRAY = "extraDisplayNameArray";
    public static final String BROADCAST_SERVICE_STATE_CHANGED = "serviceStateChanged";

    private Handler mMainHandler;

    public FreemeContactDeleteService() {
        super(TAG);
        setIntentRedelivery(true);
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ///M:[ALPS03438257]Change to foregrond to avoid be killed in lowmemory project. @{
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            FreemeLogUtils.d(TAG, "onCreate()");
            Notification notification = new Notification.Builder(this).build();
            notification.flags |= 0x10000000;
            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            FreemeLogUtils.d(TAG, "onDestroy()");
            stopForeground(true);
        }
    }
    /// @}

    private void notifyStateChanged() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(BROADCAST_SERVICE_STATE_CHANGED));
    }

    @Override
    public Object getSystemService(String name) {
        Object service = super.getSystemService(name);
        if (service != null) {
            return service;
        }

        return getApplicationContext().getSystemService(name);
    }

    // Parent classes Javadoc says not to override this method but we're doing it just to update
    // our state which should be OK since we're still doing the work in onHandleIntent
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notifyStateChanged();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            FreemeLogUtils.d(TAG, "onHandleIntent: could not handle null intent");
            return;
        }
        if (!PermissionsUtil.hasPermission(this, WRITE_CONTACTS)) {
            FreemeLogUtils.w(TAG, "No WRITE_CONTACTS permission, unable to write to CP2");
            // TODO: add more specific error string such as "Turn on Contacts
            // permission to update your contacts"
            showToast(R.string.freeme_contact_edit_permission_denied);
            return;
        }

        // Call an appropriate method. If we're sure it affects how incoming phone calls are
        // handled, then notify the fact to in-call screen.
        String action = intent.getAction();
        FreemeLogUtils.d(TAG, "[onHandleIntent] action = " + action);
        if (ACTION_DELETE_MULTIPLE_CONTACTS.equals(action)) {
            deleteMultipleContacts(intent);
        } else if (ACTION_DELETE_CONTACT.equals(action)) {
            deleteContact(intent);
        }

        notifyStateChanged();
    }


    /**
     * Creates an intent that can be sent to this service to delete a contact.
     */
    public static Intent createDeleteContactIntent(Context context, Uri contactUri) {
        Intent serviceIntent = new Intent(context, FreemeContactDeleteService.class);
        serviceIntent.setAction(FreemeContactDeleteService.ACTION_DELETE_CONTACT);
        serviceIntent.putExtra(FreemeContactDeleteService.EXTRA_CONTACT_URI, contactUri);
        return serviceIntent;
    }

    /**
     * Creates an intent that can be sent to this service to delete multiple contacts.
     */
    public static Intent createDeleteMultipleContactsIntent(Context context,
                                                            long[] contactIds,
                                                            final String[] names) {
        Intent serviceIntent = new Intent(context, FreemeContactDeleteService.class);
        serviceIntent.setAction(FreemeContactDeleteService.ACTION_DELETE_MULTIPLE_CONTACTS);
        serviceIntent.putExtra(FreemeContactDeleteService.EXTRA_CONTACT_IDS, contactIds);
        serviceIntent.putExtra(FreemeContactDeleteService.EXTRA_DISPLAY_NAME_ARRAY, names);
        return serviceIntent;
    }

    private void deleteContact(Intent intent) {
        Uri contactUri = intent.getParcelableExtra(EXTRA_CONTACT_URI);
        if (contactUri == null) {
            FreemeLogUtils.e(TAG, "Invalid arguments for deleteContact request");
            return;
        }

        getContentResolver().delete(contactUri, null, null);

        FreemeMultiChoiceService.STATUS = FreemeMultiChoiceService.STATUS_IDLE;
    }

    private void deleteMultipleContacts(Intent intent) {
        FreemeLogUtils.d(TAG, "[deleteMultipleContacts] ...");
        final long[] contactIds = intent.getLongArrayExtra(EXTRA_CONTACT_IDS);
        if (contactIds == null) {
            FreemeLogUtils.e(TAG, "Invalid arguments for deleteMultipleContacts request");
            return;
        }
        for (long contactId : contactIds) {
            final Uri contactUri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI, contactId);
            getContentResolver().delete(contactUri, null, null);
        }

        FreemeMultiChoiceService.STATUS = FreemeMultiChoiceService.STATUS_IDLE;

        final String[] names = intent.getStringArrayExtra(
                FreemeContactDeleteService.EXTRA_DISPLAY_NAME_ARRAY);
        final String deleteToastMessage;
        if (contactIds.length != names.length || names.length == 0) {
            deleteToastMessage = getResources().getQuantityString(
                    R.plurals.freeme_contacts_deleted_toast, contactIds.length);
        } else if (names.length == 1) {
            deleteToastMessage = getResources().getString(
                    R.string.freeme_contacts_deleted_one_named_toast, names);
        } else if (names.length == 2) {
            deleteToastMessage = getResources().getString(
                    R.string.freeme_contacts_deleted_two_named_toast, names);
        } else {
            deleteToastMessage = getResources().getString(
                    R.string.freeme_contacts_deleted_many_named_toast, names);
        }

        showToast(deleteToastMessage);
    }

    private void showToast(final int message) {
        mMainHandler.post(() -> {
            FreemeToast.toast(getApplicationContext(), message);
        });
    }

    private void showToast(final String message) {
        mMainHandler.post(() -> {
            FreemeToast.toast(getApplicationContext(), message);
        });
    }
}
