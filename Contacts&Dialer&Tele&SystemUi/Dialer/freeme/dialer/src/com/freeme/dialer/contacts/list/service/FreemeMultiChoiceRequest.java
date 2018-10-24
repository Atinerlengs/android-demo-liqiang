package com.freeme.dialer.contacts.list.service;

import android.accounts.Account;

public class FreemeMultiChoiceRequest {

    // Using to identify this item phone or SIM
    public int mIndicator;

    // Index in SIM for SIM contacts
    public int mSimIndex;

    // Contacts Id in the database
    public long mContactId;

    // Contacts display name
    public String mContactName;

    // Account source
    public Account mAccountSrc;

    // Account destination
    public Account mAccountDst;

    // Target account
    public Account mTargetAccount;

    public FreemeMultiChoiceRequest(int indicator,
                                    int simIndex,
                                    long contactId,
                                    String displayName) {
        mIndicator = indicator;
        mSimIndex = simIndex;
        mContactId = contactId;
        mContactName = displayName;
    }

    public FreemeMultiChoiceRequest(int indicator,
                                    int simIndex,
                                    long contactId,
                                    String displayName,
                                    Account targetAccount) {
        mIndicator = indicator;
        mSimIndex = simIndex;
        mContactId = contactId;
        mContactName = displayName;
        mTargetAccount = targetAccount;
    }

    public FreemeMultiChoiceRequest(int indicator,
                                    int simIndex,
                                    long contactId,
                                    String displayName,
                                    Account source,
                                    Account destination) {
        mIndicator = indicator;
        mSimIndex = simIndex;
        mContactId = contactId;
        mContactName = displayName;
        mAccountSrc = source;
        mAccountDst = destination;
    }
}
