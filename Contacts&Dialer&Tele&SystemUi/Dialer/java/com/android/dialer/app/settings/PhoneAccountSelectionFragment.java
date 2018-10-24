/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.dialer.app.settings;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.VisibleForTesting;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import java.util.List;
//*/ freeme.liqiang, 20180321. start voicemailsettingsfragment
import android.app.FragmentTransaction;
import android.preference.PreferenceCategory;
import com.android.voicemail.impl.settings.VoicemailSettingsFragment;
import com.freeme.preference.FreemeJumpPreference;
//*/

/**
 * Preference screen that lists SIM phone accounts to select from, and forwards the selected account
 * to {@link #PARAM_TARGET_FRAGMENT}. Can only be used in a {@link PreferenceActivity}
 */
public class PhoneAccountSelectionFragment extends PreferenceFragment {

  /** The {@link PreferenceFragment} to launch after the account is selected. */
  public static final String PARAM_TARGET_FRAGMENT = "target_fragment";

  /**
   * The arguments bundle to pass to the {@link #PARAM_TARGET_FRAGMENT}
   *
   * @see Fragment#getArguments()
   */
  public static final String PARAM_ARGUMENTS = "arguments";

  /**
   * The key to insert the selected {@link PhoneAccountHandle} to bundle in {@link #PARAM_ARGUMENTS}
   */
  public static final String PARAM_PHONE_ACCOUNT_HANDLE_KEY = "phone_account_handle_key";

  /**
   * The title of the {@link #PARAM_TARGET_FRAGMENT} once it is launched with {@link
   * PreferenceActivity#startWithFragment(String, Bundle, Fragment, int)}, as a string resource ID.
   */
  public static final String PARAM_TARGET_TITLE_RES = "target_title_res";

  private String targetFragment;
  private Bundle arguments;
  private String phoneAccountHandleKey;
  private int titleRes;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    targetFragment = getArguments().getString(PARAM_TARGET_FRAGMENT);
    arguments = new Bundle();
    arguments.putAll(getArguments().getBundle(PARAM_ARGUMENTS));
    phoneAccountHandleKey = getArguments().getString(PARAM_PHONE_ACCOUNT_HANDLE_KEY);
    titleRes = getArguments().getInt(PARAM_TARGET_TITLE_RES, 0);
  }

  /*/ freeme.liqiang, 20180321. use FreemeJumpPreference
  final class AccountPreference extends Preference {
  /*/
  final class AccountPreference extends FreemeJumpPreference {
  //*/
    private final PhoneAccountHandle phoneAccountHandle;

    public AccountPreference(
        Context context, PhoneAccountHandle phoneAccountHandle, PhoneAccount phoneAccount) {
      super(context);
      this.phoneAccountHandle = phoneAccountHandle;
      setTitle(phoneAccount.getLabel());
      /*/ freeme.liqiang, 20180321. remove summary
      setSummary(phoneAccount.getShortDescription());
      /*/
      Icon icon = phoneAccount.getIcon();
      if (icon != null) {
        setIcon(icon.loadDrawable(context));
      }
    }

    @VisibleForTesting
    void click() {
      onClick();
    }

    @Override
    protected void onClick() {
      super.onClick();
      /*/ freeme.liqiang, 20180321. start voicemailsettingsfragment
      PreferenceActivity preferenceActivity = (PreferenceActivity) getActivity();
      arguments.putParcelable(phoneAccountHandleKey, phoneAccountHandle);
      preferenceActivity.startWithFragment(targetFragment, arguments, null, 0, titleRes, 0);
      /*/
      VoicemailSettingsFragment newFragment = new VoicemailSettingsFragment();
      arguments.putParcelable(phoneAccountHandleKey, phoneAccountHandle);
      newFragment.setArguments(arguments);
      FragmentTransaction transaction = getFragmentManager().beginTransaction();
      transaction.replace(android.R.id.content, newFragment);
      transaction.addToBackStack(null);
      transaction.commit();
      //*/
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
    PreferenceScreen screen = getPreferenceScreen();

    TelecomManager telecomManager = getContext().getSystemService(TelecomManager.class);

    List<PhoneAccountHandle> accountHandles = telecomManager.getCallCapablePhoneAccounts();

    Context context = getActivity();
    //*/ freeme.liqiang, 20180321. add preferenceCategory
    PreferenceCategory preferenceCategory = new PreferenceCategory(context);
    screen.addPreference(preferenceCategory);
    //*/
    for (PhoneAccountHandle handle : accountHandles) {
      PhoneAccount account = telecomManager.getPhoneAccount(handle);
      if (account != null) {
        final boolean isSimAccount =
            0 != (account.getCapabilities() & PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION);
        if (isSimAccount) {
          /*/ freeme.liqiang, 20180321. add preferenceCategory
          screen.addPreference(new AccountPreference(context, handle, account));
          /*/
          preferenceCategory.addPreference(new AccountPreference(context, handle, account));
          //*/
        }
      }
    }
  }
}
