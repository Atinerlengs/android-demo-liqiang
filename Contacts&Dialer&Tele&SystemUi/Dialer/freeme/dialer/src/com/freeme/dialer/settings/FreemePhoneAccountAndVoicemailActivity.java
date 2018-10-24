package com.freeme.dialer.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import com.android.dialer.app.R;
import com.android.dialer.app.settings.PhoneAccountSelectionFragment;
import com.android.dialer.common.LogUtil;
import com.android.voicemail.VoicemailClient;
import com.android.voicemail.VoicemailComponent;
import com.android.voicemail.impl.settings.VoicemailSettingsFragment;
import com.freeme.actionbar.app.FreemeActionBarUtil;

public class FreemePhoneAccountAndVoicemailActivity extends PreferenceActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if ( actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            setNavigateTitle(actionBar,getIntent());
        }
        init();
    }

    private void init() {
        if (!isPrimaryUser()) {
            return;
        }
        Fragment fragment;
        String voicemailSettingsFragment =
                VoicemailComponent.get(this).getVoicemailClient().getSettingsFragment();
        if (voicemailSettingsFragment == null) {
            LogUtil.i(
                    "DialerSettingsActivity.addVoicemailSettings",
                    "VoicemailClient does not provide settings");
            return;
        }
        PhoneAccountHandle soleAccount = getSoleSimAccount();
        if (soleAccount == null) {
            fragment = new PhoneAccountSelectionFragment();
            Bundle bundle = new Bundle();
            bundle.putString(
                    PhoneAccountSelectionFragment.PARAM_TARGET_FRAGMENT, voicemailSettingsFragment);
            bundle.putString(
                    PhoneAccountSelectionFragment.PARAM_PHONE_ACCOUNT_HANDLE_KEY,
                    VoicemailClient.PARAM_PHONE_ACCOUNT_HANDLE);
            bundle.putBundle(PhoneAccountSelectionFragment.PARAM_ARGUMENTS, new Bundle());
            bundle.putInt(
                    PhoneAccountSelectionFragment.PARAM_TARGET_TITLE_RES, R.string.voicemail_settings_label);
            fragment.setArguments(bundle);
        } else {
            fragment = new VoicemailSettingsFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable(VoicemailClient.PARAM_PHONE_ACCOUNT_HANDLE, soleAccount);
            fragment.setArguments(bundle);
        }
        getFragmentManager().beginTransaction().replace(
                android.R.id.content, fragment).commit();
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return VoicemailSettingsFragment.class.getName().equals(fragmentName);
    }

    @Nullable
    private PhoneAccountHandle getSoleSimAccount() {
        TelecomManager telecomManager = getSystemService(TelecomManager.class);
        PhoneAccountHandle result = null;
        for (PhoneAccountHandle phoneAccountHandle : telecomManager.getCallCapablePhoneAccounts()) {
            PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                LogUtil.i(
                        "DialerSettingsActivity.getSoleSimAccount", phoneAccountHandle + " is a SIM account");
                if (result != null) {
                    return null;
                }
                result = phoneAccountHandle;
            }
        }
        return result;
    }

    private boolean isPrimaryUser() {
        return getSystemService(UserManager.class).isSystemUser();
    }

    private String getSubTitle(Intent intent) {
        String title = null;
        if (intent != null) {
            title = intent.getStringExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT);
        }
        return title;
    }

    private void setNavigateTitle(ActionBar actionBar, Intent intent) {
        if (actionBar != null) {
            FreemeActionBarUtil.setBackTitle(actionBar, getSubTitle(intent));
        }
    }
}
