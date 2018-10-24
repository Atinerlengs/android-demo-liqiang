package com.mediatek.dialer.calllog;

import android.database.Cursor;

import com.android.dialer.compat.CompatUtils;
import com.android.dialer.phonenumbercache.CallLogQuery;
import com.android.dialer.phonenumbercache.ContactInfo;
import com.android.dialer.phonenumbercache.ContactInfoHelper;

/**
 * for lookup conference calllog name
 */
public class ConfCallLogInfo {
    public ContactInfo cachedContactInfo;
    public String number;
    public String countryIso;
    public String postDialDigits;
    public int numberPresentation;

    public ConfCallLogInfo(Cursor c) {
        cachedContactInfo = ContactInfoHelper.getContactInfo(c);
        number = c.getString(CallLogQuery.NUMBER);
        numberPresentation = c.getInt(CallLogQuery.NUMBER_PRESENTATION);
        countryIso = c.getString(CallLogQuery.COUNTRY_ISO);
        postDialDigits = CompatUtils.isNCompatible() ?
                c.getString(CallLogQuery.POST_DIAL_DIGITS) : "";
    }
}
