package com.freeme.dialer.settings;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.android.contacts.common.compat.TelephonyManagerCompat;

import com.android.dialer.R;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.preference.FreemeJumpPreference;
import com.freeme.provider.FreemeSettings;
import com.freeme.util.FreemeOption;

import java.util.Iterator;
import java.util.List;

@TargetApi(24)
public class FreemeDialerSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "FreemeDialerSetting";
    //dialer assist
    private static final String KEY_DIALER_ASSIST = "dialer_assist";
    private static final String KEY_INCOMING_VIBRATION = "incoming_vibrate";
    private static final String KEY_PHONE_VIBRATE = "phone_vibrate";
    private static final String KEY_GRADIENT_RING = "gradient_ring";
    private static final String KEY_REVERSE_SILENCE = "reverse_silent";
    private static final String KEY_POCKET_MODE = "pocket_mode";
    private static final String KEY_NOISE_REDUCTION = "noise_reduction";

    private SwitchPreference mIncomingVibrate;
    private SwitchPreference mPhoneVibrate;
    private SwitchPreference mGradientRingtone;
    private SwitchPreference mReverseSilence;
    private SwitchPreference mPocketMode;
    private SwitchPreference mNoiseReduction;

    //dialer shortcut
    private static final String KEY_DIALER_SHORTCUT = "dialer_shortcut";
    private static final String KEY_SMART_DIALER = "smart_dialer";
    private static final String KEY_SMART_ANSWER = "smart_answer";
    private static final String KEY_NO_TOUCH = "no_touch";
    private static final String KEY_FREEME_SPEED_DIAL = "freeme_speed_dial";

    private SwitchPreference mSmartDialer;
    private SwitchPreference mSmartAnswer;
    private FreemeJumpPreference mNoTouch;
    private FreemeJumpPreference mFreemeSpeedDial;

    //other settings
    private static final String KEY_CALL_RELATED = "call_related";
    private static final String KEY_VOICEMAIL = "voicemail";

    private FreemeJumpPreference mCallRelated;
    private FreemeJumpPreference mVoiceMail;

    private TelephonyManager mTelephonyManager;
    private TelecomManager mTelecomManager;
    private SubscriptionManager mSubscriptionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        mTelephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelecomManager = TelecomManager.from(this);
        mSubscriptionManager = SubscriptionManager.from(this);

        com.freeme.actionbar.app.FreemeActionBarUtil.setNavigateTitle(this, getIntent());
        addPreferencesFromResource(R.xml.freeme_dialer_settings);

        initSensorSupport();
        initializeAllPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPreferenceStatus(mIncomingVibrate);
        initPreferenceStatus(mPhoneVibrate);
        initPreferenceStatus(mGradientRingtone);
        initPreferenceStatus(mReverseSilence);
        initPreferenceStatus(mPocketMode);

        initNoiseReductionPreferenceStatus();

        initPreferenceStatus(mSmartDialer);
        initPreferenceStatus(mSmartAnswer);
        initNoTouchPreferenceStatus(mNoTouch);

        initPhoneAccount(getCallingAccounts(true));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPreferenceStatus(SwitchPreference preference) {
        if (preference != null) {
            String key = getSettingsKey(preference);
            preference.setChecked(isChecked(key, 0));
            preference.setOnPreferenceChangeListener(this);
        }
    }

    private void initNoiseReductionPreferenceStatus() {
        if (mNoiseReduction != null) {
            mNoiseReduction.setChecked(isDualMicModeEnabled());
        }
    }

    private void initializeAllPreferences() {
        initializeAllAssistPreferences();
        initializeAllShortcutPreferences();
        initializeAllOtherPreferences();
    }

    private void initializeAllAssistPreferences() {
        mIncomingVibrate = (SwitchPreference) findPreference(KEY_INCOMING_VIBRATION);
        mPhoneVibrate = (SwitchPreference) findPreference(KEY_PHONE_VIBRATE);
        mGradientRingtone = (SwitchPreference) findPreference(KEY_GRADIENT_RING);
        mReverseSilence = (SwitchPreference) findPreference(KEY_REVERSE_SILENCE);
        mPocketMode = (SwitchPreference) findPreference(KEY_POCKET_MODE);
        mNoiseReduction = (SwitchPreference) findPreference(KEY_NOISE_REDUCTION);

        PreferenceCategory category = (PreferenceCategory) findPreference(KEY_DIALER_ASSIST);
        if (!mIsPSensorSupport || !mIsGSensorSupport) {
            category.removePreference(mPocketMode);
        }
        if (!mIsGSensorSupport) {
            category.removePreference(mReverseSilence);
        }
        if (isMtkDualMicSupport() && !isMTKA1Support()) {
            initNoiseReductionPreferenceStatus();
            mNoiseReduction.setOnPreferenceChangeListener(this);
        } else {
            category.removePreference(mNoiseReduction);
            mNoiseReduction = null;
        }
    }

    private void initializeAllShortcutPreferences() {
        mSmartDialer = (SwitchPreference) findPreference(KEY_SMART_DIALER);
        mSmartAnswer = (SwitchPreference) findPreference(KEY_SMART_ANSWER);
        mNoTouch = (FreemeJumpPreference) findPreference(KEY_NO_TOUCH);
        mFreemeSpeedDial = (FreemeJumpPreference) findPreference(KEY_FREEME_SPEED_DIAL);

        PreferenceCategory category = (PreferenceCategory) findPreference(KEY_DIALER_SHORTCUT);
        if (!mIsPSensorSupport || !mIsGSensorSupport) {
            category.removePreference(mSmartDialer);
            category.removePreference(mSmartAnswer);
        }
        if (!mIsGestureSensorSupport) {
            category.removePreference(mNoTouch);
        }
    }

    private void initializeAllOtherPreferences() {
        mCallRelated = (FreemeJumpPreference) findPreference(KEY_CALL_RELATED);
        mVoiceMail = (FreemeJumpPreference) findPreference(KEY_VOICEMAIL);
    }

    private boolean isChecked(String key, int def) {
        return Settings.System.getInt(getContentResolver(), key, def) == 1;
    }

    private String getSettingsKey(Preference preference) {
        if (preference == mIncomingVibrate) {
            return Settings.System.VIBRATE_WHEN_RINGING;
        } else if (preference == mPhoneVibrate) {
            return FreemeSettings.System.FREEME_PHONE_VIBRAT_KEY;
        } else if (preference == mGradientRingtone) {
            return FreemeSettings.System.FREEME_GRADIENT_RING_KEY;
        } else if (preference == mReverseSilence) {
            return FreemeSettings.System.FREEME_REVERSE_SILENT_SETTING;
        } else if (preference == mPocketMode) {
            return FreemeSettings.System.FREEME_POCKET_MODE_KEY;
        } else if (preference == mSmartDialer) {
            return FreemeSettings.System.FREEME_SMART_DIAL_KEY;
        } else if (preference == mSmartAnswer) {
            return FreemeSettings.System.FREEME_SMART_ANSWER_KEY;
        }
        throw new NullPointerException("the preference is invalid");
    }

    // DualMic mode values
    public static final String DUA_VAL_ON = "1";
    public static final String DUAL_VAL_OFF = "0";

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNoiseReduction) {
            setDualMicMode((boolean) newValue ? DUA_VAL_ON : DUAL_VAL_OFF);
        } else {
            String key = getSettingsKey(preference);
            boolean preStatus = isChecked(key, 0);
            Settings.System.putInt(getContentResolver(), key, preStatus ? 0 : 1);
        }
        return true;
    }

    private static final String MTK_DUAL_MIC_SUPPORT = "MTK_DUAL_MIC_SUPPORT";
    private static final String MTK_DUAL_MIC_SUPPORT_ON = "MTK_DUAL_MIC_SUPPORT=true";
    private final static String ONE = "1";

    public boolean isMtkDualMicSupport() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return am != null &&
                MTK_DUAL_MIC_SUPPORT_ON.equalsIgnoreCase(am.getParameters(MTK_DUAL_MIC_SUPPORT));
    }

    public static boolean isMTKA1Support() {
        return ONE.equals(SystemProperties.get("ro.mtk_a1_feature"));
    }


    ///Add for [Dual_Mic]
    private static final String DUALMIC_MODE = "Enable_Dual_Mic_Setting";
    private static final String GET_DUALMIC_MODE = "Get_Dual_Mic_Setting";
    private static final String DUALMIC_ENABLED = "Get_Dual_Mic_Setting=1";

    /**
     * get DualMic noise reduction mode.
     */
    public boolean isDualMicModeEnabled() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return am != null &&
                DUALMIC_ENABLED.equalsIgnoreCase(am.getParameters(GET_DUALMIC_MODE));
    }

    /**
     * set DualMic noise reduction mode.
     *
     * @param dualMic the value to show the user set
     */
    public void setDualMicMode(String dualMic) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setParameters(DUALMIC_MODE + "=" + dualMic);
    }

    private final static String NOT_SET = "0";
    private final static String MUTE_CALLS = "1";
    private final static String HANDS_FREE_ANSWER = "2";

    private void initNoTouchPreferenceStatus(FreemeJumpPreference preference) {
        if (preference == null) {
            return;
        }
        int resId = R.string.freeme_dialer_settings_no_touch_not_set;
        switch (isNonTouchSet()) {
            case NOT_SET:
                resId = R.string.freeme_dialer_settings_no_touch_none;
                break;
            case MUTE_CALLS:
                resId = R.string.freeme_dialer_settings_no_touch_mute_calls;
                break;
            case HANDS_FREE_ANSWER:
                resId = R.string.freeme_dialer_settings_no_touch_hands_free_answer;
                break;
            default:
                break;
        }
        preference.setStatusText1(getString(resId));
    }

    private String isNonTouchSet() {
        final ContentResolver cr = getContentResolver();
        return FreemeSettings.System.getBoolbit(cr, FreemeSettings.System.FREEME_GESTURE_SETS,
                FreemeSettings.System.FREEME_GESTURE_PHONE_HandFree_CONTROL
                        | FreemeSettings.System.FREEME_GESTURE_SETS_ENABLE, false) ? HANDS_FREE_ANSWER :
                FreemeSettings.System.getBoolbit(cr, FreemeSettings.System.FREEME_GESTURE_SETS,
                        FreemeSettings.System.FREEME_GESTURE_PHONE_CONTROL
                                | FreemeSettings.System.FREEME_GESTURE_SETS_ENABLE, false) ? MUTE_CALLS :
                        NOT_SET;
    }

    private static final String FREEME_VOICEMAIL_ACCOUNT = "com.freeme.intent.ACTION_FREEME_VOICEMAIL_ACCOUNT";
    private static final String FREEME_SPEED_DIAL = "com.freeme.intent.ACTION_FREEME_SPEEDDIAL";

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean isPrimaryUser = isPrimaryUser();

        if (preference == mCallRelated) {
            if (isPrimaryUser && TelephonyManagerCompat.getPhoneCount(mTelephonyManager) <= 1) {
                Intent callSettingsIntent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
                callSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                callSettingsIntent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, backtTitle());
                startActivity(callSettingsIntent);
            } else if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) || isPrimaryUser) {
                if (mTelecomManager != null && mTelecomManager.getCallCapablePhoneAccounts().size() <= 1) {
                    startActivityWithNone(mPhoneAccountIntent);
                } else {
                    Intent phoneAccountSettingsIntent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
                    phoneAccountSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    phoneAccountSettingsIntent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, backtTitle());
                    startActivity(phoneAccountSettingsIntent);
                }
            }
        } else if (preference == mVoiceMail) {
            Intent intent = new Intent();
            intent.setAction(FREEME_VOICEMAIL_ACCOUNT);
            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, backtTitle());
            startActivity(intent);
        } else if (preference == mFreemeSpeedDial) {
            Intent intent = new Intent();
            intent.setAction(FREEME_SPEED_DIAL);
            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, backtTitle());
            startActivity(intent);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private String backtTitle() {
        return getString(R.string.freeme_tab_call_label);
    }

    /**
     * @return Whether the current user is the primary user.
     */
    private boolean isPrimaryUser() {
        UserManager um = getSystemService(UserManager.class);
        return um != null && um.isSystemUser();
    }

    private void startActivityWithNone(Intent intent) {
        if (intent != null) {
            startActivity(intent);
        } else {
            intent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, backtTitle());
            startActivity(intent);
        }
    }

    private List<PhoneAccountHandle> getCallingAccounts(boolean includeSims) {

        List<PhoneAccountHandle> accountHandles =
                mTelecomManager.getCallCapablePhoneAccounts();
        for (Iterator<PhoneAccountHandle> i = accountHandles.iterator(); i.hasNext(); ) {
            PhoneAccountHandle handle = i.next();

            PhoneAccount account = mTelecomManager.getPhoneAccount(handle);
            if (account == null) {
                i.remove();
            } else if (!includeSims &&
                    account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                i.remove();
            }
        }
        return accountHandles;
    }

    private static final String LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT =
            "android.telecom.action.CONNECTION_SERVICE_CONFIGURE";

    private Intent buildPhoneAccountConfigureIntent(
            Context context, PhoneAccountHandle accountHandle) {
        Intent intent = buildConfigureIntent(
                context, accountHandle, TelecomManager.ACTION_CONFIGURE_PHONE_ACCOUNT);

        if (intent == null) {
            // If the new configuration didn't work, try the old configuration intent.
            intent = buildConfigureIntent(
                    context, accountHandle, LEGACY_ACTION_CONFIGURE_PHONE_ACCOUNT);
            if (intent != null) {
                Log.w(TAG, "Phone account using old configuration intent: " + accountHandle);
            }
        }
        Log.d(TAG, "get intent: " + intent);
        return intent;
    }

    private Intent mPhoneAccountIntent;

    private void initPhoneAccount(List<PhoneAccountHandle> enabledAccounts) {
        boolean isMultiSimDevice = mTelephonyManager.isMultiSimEnabled();
        for (PhoneAccountHandle handle : enabledAccounts) {
            PhoneAccount account = mTelecomManager.getPhoneAccount(handle);
            if (account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                if (isMultiSimDevice) {
                    SubscriptionInfo subInfo = mSubscriptionManager.getActiveSubscriptionInfo(
                            mTelephonyManager.getSubIdForPhoneAccount(account));
                    if (subInfo != null) {
                        mPhoneAccountIntent = new Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS);
                        mPhoneAccountIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        addExtrasToIntent(mPhoneAccountIntent, subInfo);
                    }
                }
            } else {
                mPhoneAccountIntent = buildPhoneAccountConfigureIntent(this, handle);
            }
        }
    }

    private Intent buildConfigureIntent(
            Context context, PhoneAccountHandle accountHandle, String actionStr) {
        if (accountHandle == null || accountHandle.getComponentName() == null ||
                TextUtils.isEmpty(accountHandle.getComponentName().getPackageName())) {
            return null;
        }

        // Build the settings intent.
        Intent intent = new Intent(actionStr);
        intent.setPackage(accountHandle.getComponentName().getPackageName());
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle);

        // Check to see that the phone account package can handle the setting intent.
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolutions = pm.queryIntentActivities(intent, 0);
        if (resolutions.size() == 0) {
            intent = null;  // set no intent if the package cannot handle it.
        }

        return intent;
    }

    private static final String SUBSCRIPTION_KEY = "subscription";
    // Extra on intent containing the id of a subscription.
    private static final String SUB_ID_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
    // Extra on intent containing the label of a subscription.
    private static final String SUB_LABEL_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionLabel";

    private static void addExtrasToIntent(Intent intent, SubscriptionInfo subscription) {
        if (subscription == null) {
            return;
        }

        intent.putExtra(SUB_ID_EXTRA, subscription.getSubscriptionId());
        ///M: Add for CallSettings inner activity pass subid to other activity. @{
        intent.putExtra(SUBSCRIPTION_KEY, subscription.getSubscriptionId());
        /// @}
        intent.putExtra(SUB_LABEL_EXTRA, subscription.getDisplayName().toString());
    }

    private static final String KEY_HW_SENSOR_PROXIMITY = "ro.freeme.hw_sensor_proximity";
    private static final String KEY_HW_SENSOR_ACCELEROMETER = "ro.freeme.hw_sensor_acce";
    private boolean mIsPSensorSupport;
    private boolean mIsGSensorSupport;
    private boolean mIsGestureSensorSupport;

    private void initSensorSupport() {
        mIsPSensorSupport = SystemProperties.getBoolean(KEY_HW_SENSOR_PROXIMITY, false);
        mIsGSensorSupport = SystemProperties.getBoolean(KEY_HW_SENSOR_ACCELEROMETER, false);
        mIsGestureSensorSupport = FreemeOption.NonTouch.supports(1);
    }
}
