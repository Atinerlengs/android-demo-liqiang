package com.mediatek.dialer.compat;

import android.content.Context;
import com.mediatek.provider.MtkContactsContract.Aas;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.dialer.util.PermissionsUtil;

import com.mediatek.provider.MtkContactsContract;

/**
 * Compatibility utility class about ContactsContract.
 */
public class ContactsCompat {
  /**
   * Compatibility utility class about android.provider.ContactsContract.RawContacts
   */
  public static class RawContactsCompat {
    //refer to RawContacts.INDICATE_PHONE_SIM
    public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";
    //refer to RawContacts.INDEX_IN_SIM
    public static final String INDEX_IN_SIM = "index_in_sim";
    //refer to RawContacts.IS_SDN_CONTACT
    public static final String IS_SDN_CONTACT = "is_sdn_contact";
  }

  /**
   * Compatibility utility class about android.provider.ContactsContract.RawContacts
   */
  public static class PhoneLookupCompat {
    //refer to PhoneLookup.INDICATE_PHONE_SIM
    public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";
    //refer to PhoneLookup.IS_SDN_CONTACT
    public static final String INDEX_IN_SIM = "index_in_sim";
    //refer to PhoneLookup.IS_SDN_CONTACT
    public static final String IS_SDN_CONTACT = "is_sdn_contact";
  }

  /**
   * Compatibility utility class about ContactsContract.CommonDataKinds.Phone.
   */
  public static class PhoneCompat {
    private static final String PHONE_CLASS =
            "com.mediatek.provider.MtkContactsContract$CommonDataKinds$Phone";
    private static final String GET_TYPE_LABEL_METHOD = "getTypeLabel";

    public static CharSequence getTypeLabel(Context context, int labelType, CharSequence label) {
        CharSequence res = "";
        if (DialerCompatExUtils.isMethodAvailable(PHONE_CLASS, GET_TYPE_LABEL_METHOD, Context.class,
                int.class, CharSequence.class)) {
            if (labelType == Aas.PHONE_TYPE_AAS && !TextUtils.isEmpty(label)
                    && !PermissionsUtil.hasContactsReadPermissions(context)) {
                return "";
            }
            ///M: Using new API for AAS phone number label lookup.
            res = MtkContactsContract.CommonDataKinds.Phone.getTypeLabel(context, labelType, label);
        } else {
            res = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    context.getResources(), labelType, label);
        }
        return res;
    }
  }

  /**
   * Compatibility utility class android.provider.ContactsContract.CommonDataKinds.ImsCall.
   */
  public static class ImsCallCompat {
    /**
     * refer to ContactsContract.CommonDataKinds.ImsCall.CONTENT_ITEM_TYPE.
     * MIME type used when storing this in data table.
     * @internal
     */
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/ims";
  }
}
