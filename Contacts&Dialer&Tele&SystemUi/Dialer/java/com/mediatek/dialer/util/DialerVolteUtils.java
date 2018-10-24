package com.mediatek.dialer.util;

import java.util.ArrayList;
import java.util.List;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.android.dialer.R;
import com.android.dialer.callintent.CallInitiationType;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.common.LogUtil;
import com.android.dialer.util.CallUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PermissionsUtil;
import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.dialer.compat.TelecomCompat;
import com.mediatek.dialer.compat.TelecomCompat.PhoneAccountCompat;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import mediatek.telephony.MtkCarrierConfigManager;
import mediatek.telecom.MtkTelecomManager;
/**
 * M: [VoLTE ConfCall] A util class for supporting the VOLTE features
 */
public class DialerVolteUtils {
    private static final String TAG = "VolteUtils";

    public static final int ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS = 101;

    /**
     * [VoLTE ConfCall] Launch the contacts choice activity to pick participants.
     */
    public static void handleMenuVolteConfCall(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(ContactsIntent.LIST.ACTION_PICK_MULTI_PHONEANDIMSANDSIPCONTACTS);
        intent.setType(Phone.CONTENT_TYPE);
        intent.putExtra(ContactsIntent.CONFERENCE_CALL_LIMIT_NUMBER,
                ContactsIntent.CONFERENCE_CALL_LIMITES);
        DialerUtils.startActivityForResultWithErrorToast(activity, intent,
                ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS);
    }

    /**
     * [VoLTE ConfCall] Launch volte conference call according the picked contacts.
     */
    public static void launchVolteConfCall(Activity activity, Intent data) {
        final long[] dataIds = data.getLongArrayExtra(
                ContactsIntent.CONFERENCE_CALL_RESULT_INTENT_EXTRANAME);

        // Add contacts permission check
        if (!PermissionsUtil.hasContactsReadPermissions(activity)) {
            Toast.makeText(activity, R.string.missing_required_permission,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (dataIds == null || dataIds.length <= 0) {
            LogUtil.d(TAG, "Volte conf call, the selected contacts is empty");
            return;
        }
        new LaunchVolteConfCallTask(activity).execute(dataIds);
    }

    private static class LaunchVolteConfCallTask extends
            AsyncTask<long[], Void, ArrayList<String>> {

        Activity mActivity;
        LaunchVolteConfCallTask(Activity activity) {
            mActivity = activity;
        }
        @Override
        protected ArrayList<String> doInBackground(long[]... arg0) {
            return getPhoneNumberByDataIds(mActivity, arg0[0]);
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (mActivity.isFinishing()) {
                LogUtil.d(TAG, "Volte conf call, Activity has finished");
                return;
            }
            if (result.size() <= 0) {
                LogUtil.d(TAG, "Volte conf call, No phone numbers");
                return;
            }
            //Intent confCallIntent = new CallIntentBuilder(result.get(0),
            //    CallInitiationType.Type.DIALPAD).build();
            //confCallIntent.putExtra(TelecomCompat.EXTRA_VOLTE_CONF_CALL_DIAL, true);
            //confCallIntent.putStringArrayListExtra(
            //    TelecomCompat.EXTRA_VOLTE_CONF_CALL_NUMBERS, result);
            /// M: Fix for ALPS03647027 @{
            Intent confCallIntent =
              MtkTelecomManager.createConferenceInvitationIntent(mActivity.getApplicationContext());
            Uri uri = CallUtil.getCallUri(result.get(0));
            confCallIntent.setData(uri);
            confCallIntent.putExtra(TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                VideoProfile.STATE_AUDIO_ONLY);

            Bundle extras = new Bundle();
            extras.putLong("android.telecom.extra.CALL_CREATED_TIME_MILLIS",
                           SystemClock.elapsedRealtime());
            confCallIntent.putExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
            /// @}
            confCallIntent.putStringArrayListExtra(
                TelecomCompat.EXTRA_VOLTE_CONF_CALL_NUMBERS, result);
            DialerUtils.startActivityWithErrorToast(mActivity, confCallIntent);
        }
    }

    private static ArrayList<String> getPhoneNumberByDataIds(
            Context context, long[] dataIds) {
        ArrayList<String> phoneNumbers = new ArrayList<String>();
        if (dataIds == null || dataIds.length <= 0) {
            return phoneNumbers;
        }
        StringBuilder selection = new StringBuilder();
        selection.append(Data._ID);
        selection.append(" IN (");
        selection.append(dataIds[0]);
        for (int i = 1; i < dataIds.length; i++) {
            selection.append(",");
            selection.append(dataIds[i]);
        }
        selection.append(")");
        LogUtil.d(TAG, "getPhoneNumberByDataIds dataIds " + selection.toString());
        Cursor c = null;
        try {
            c = context.getContentResolver().query(Data.CONTENT_URI,
                    new String[]{Data._ID, Data.DATA1},
                    selection.toString(), null, null);
            if (c == null) {
                return phoneNumbers;
            }
            while (c.moveToNext()) {
                LogUtil.d(TAG, "getPhoneNumberByDataIds got"
                        + " _ID=" + c.getInt(0)
                        + ", NUMBER=" + LogUtil.sanitizePhoneNumber(c.getString(1)));
                phoneNumbers.add(c.getString(1));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return phoneNumbers;
    }

    /**
     * Returns whether the VoLTE conference call enabled.
     * @param context the context
     * @return true if the VOLTE is supported and has Volte phone account
     */
    public static boolean isVolteConfCallEnable(Context context) {
        if (!DialerFeatureOptions.isVolteEnhancedConfCallSupport() || context == null
            ///M:configManager.getConfigForSubId need READ_PHONE_STATE permission
            || !PermissionsUtil.hasPermission(context, permission.READ_PHONE_STATE)) {
            return false;
        }
        final TelecomManager telecomManager = (TelecomManager) context
                .getSystemService(Context.TELECOM_SERVICE);
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        CarrierConfigManager configManager = (CarrierConfigManager) context
            .getSystemService(Context.CARRIER_CONFIG_SERVICE);

        List<PhoneAccount> phoneAccouts = telecomManager.getAllPhoneAccounts();
        for (PhoneAccount phoneAccount : phoneAccouts) {
            int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
            boolean isVolteEnabled = MtkTelephonyManagerEx.getDefault().isVolteEnabled(subId);
            PersistableBundle bundle = configManager.getConfigForSubId(subId);
            if (isVolteEnabled
                && bundle != null
                && bundle.getBoolean(
                  MtkCarrierConfigManager.MTK_KEY_VOLTE_CONFERENCE_ENHANCED_ENABLE_BOOL)) {
                return true;
            }
        }
        return false;
    }
}
