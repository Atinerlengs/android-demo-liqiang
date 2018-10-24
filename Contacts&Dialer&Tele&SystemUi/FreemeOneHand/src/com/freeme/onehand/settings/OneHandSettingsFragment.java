package com.freeme.onehand.settings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.view.ContextThemeWrapper;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.preference.RadioButtonPreference;

import com.freeme.onehand.OneHandConstants;
import com.freeme.onehand.R;
import com.freeme.onehand.settings.widget.WrapContentHeightViewPager;
import com.freeme.policy.FreemeNavigation;
import com.freeme.provider.FreemeSettings;
import com.freeme.util.FreemeOption;

public final class OneHandSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, RadioButtonPreference.OnClickListener {

    static final boolean HAS_TRIPLY_HOMEKEY = Utils.HAS_TRIPLY_HOMEKEY;
    static final boolean HAS_NAVIGATION_BAR = Utils.hasNavigationBar();

    private static final String PREF_SWITCHER = "switcher";

    private static final String PREF_CATEGORY_SHOW_HARDKEYS = "show_keys";
    private static final String PREF_SWITCH_SHOW_HARDKEYS = "switch_show_hard_keys";

    private static final String PREF_CATEGORY_WAKEUP_TYPE = "wakeup_type";
    static final String PREF_GESTURE_TYPE = "gesture_type";
    static final String PREF_BUTTON_TYPE = "button_type";

    private SwitchPreference mPrefSwitcher;

    private PreferenceCategory mPrefCategoryShowKeys;
    private SwitchPreference mPrefShowHardKeysOnScreen;

    private PreferenceCategory mPrefCategoryWakeType;
    private RadioButtonPreference mPrefGeatureType;
    private RadioButtonPreference mPrefButtonType;

    private WrapContentHeightViewPager mViewPager;
    private OneHandViewPagerAdapter mViewPagerAdapter;
    private LinearLayout mPointArea;

    private AlertDialog mExclusiveDialog;

    private final ContentObserver mOneHandObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            final boolean enabled = Settings.System.getInt(getContentResolver(),
                    OneHandConstants.ONEHAND_ENABLED, 0) != 0;

            mPrefSwitcher.setChecked(enabled);

            PreferenceScreen screen = getPreferenceScreen();
            if (enabled) {
                if (mPrefCategoryShowKeys != null
                        && screen.findPreference(PREF_CATEGORY_SHOW_HARDKEYS) == null) {
                    screen.addPreference(mPrefCategoryShowKeys);
                }
                if (mPrefCategoryWakeType != null
                        && screen.findPreference(PREF_CATEGORY_WAKEUP_TYPE) == null) {
                    screen.addPreference(mPrefCategoryWakeType);
                }
            } else {
                if (mPrefCategoryShowKeys != null) {
                    screen.removePreference(mPrefCategoryShowKeys);
                }
                if (mPrefCategoryWakeType != null) {
                    screen.removePreference(mPrefCategoryWakeType);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.onehand_settings_preference);

        mPrefSwitcher = (SwitchPreference) findPreference(PREF_SWITCHER);
        mPrefSwitcher.setOnPreferenceChangeListener(this);

        mPrefCategoryWakeType = (PreferenceCategory) findPreference(PREF_CATEGORY_WAKEUP_TYPE);
        if (mPrefCategoryWakeType != null) {
            if (HAS_TRIPLY_HOMEKEY) {
                mPrefGeatureType = (RadioButtonPreference) mPrefCategoryWakeType.findPreference(PREF_GESTURE_TYPE);
                mPrefButtonType = (RadioButtonPreference) mPrefCategoryWakeType.findPreference(PREF_BUTTON_TYPE);
                mPrefGeatureType.setOnClickListener(this);
                mPrefButtonType.setOnClickListener(this);
            } else {
                getPreferenceScreen().removePreference(mPrefCategoryWakeType);
                mPrefCategoryWakeType = null;
            }
        }

        mPrefCategoryShowKeys = (PreferenceCategory) findPreference(PREF_CATEGORY_SHOW_HARDKEYS);
        if (mPrefCategoryShowKeys != null) {
            if (HAS_NAVIGATION_BAR) {
                getPreferenceScreen().removePreference(mPrefCategoryShowKeys);
                mPrefCategoryShowKeys = null;
            } else {
                mPrefShowHardKeysOnScreen = (SwitchPreference) mPrefCategoryShowKeys.findPreference(PREF_SWITCH_SHOW_HARDKEYS);
                mPrefShowHardKeysOnScreen.setOnPreferenceChangeListener(this);
                if (mPrefButtonType != null) {
                    mPrefButtonType.setSummary(R.string.onehand_settings_using_button_summary_hardkey);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        View guideView = inflater.inflate(R.layout.guide, null);
        mViewPager = (WrapContentHeightViewPager) guideView.findViewById(R.id.pager);
        mViewPagerAdapter = new OneHandViewPagerAdapter(getActivity());
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setOffscreenPageLimit(mViewPagerAdapter.getCount());
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                changePagerIndicator(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mPointArea = (LinearLayout) guideView.findViewById(R.id.point_area);
        final int pointCount = mViewPagerAdapter.getCount();
        for (int i = 0; i < pointCount; i++) {
            final int current = i;
            ImageView point = (ImageView) inflater.inflate(R.layout.pager_indicator_circle, null);
            point.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mViewPager.setCurrentItem(current);
                }
            });
            if (i == 0) {
                point.setImageResource(R.drawable.circle_on);
            }
            mPointArea.addView(point);
        }
        if (mPointArea.getChildCount() == 1) {
            mPointArea.setVisibility(View.GONE);
        }
        if (Utils.isRTL(getActivity())) {
            mViewPager.setCurrentItem(pointCount);
        }
        getListView().addFooterView(guideView, null, true);
        getListView().setItemsCanFocus(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mOneHandObserver.onChange(true);
        final int wakeupType = Settings.System.getInt(getContentResolver(),
                OneHandConstants.ONEHAND_WAKEUP_TYPE, 0);
        updateRadioButtons((wakeupType == 0) ? mPrefGeatureType : mPrefButtonType,
                false);
        mViewPager.setCurrentItem(wakeupType);

        getContentResolver().registerContentObserver(Settings.System.getUriFor(
                OneHandConstants.ONEHAND_ENABLED), false, mOneHandObserver);
    }

    @Override
    public void onPause() {
        getContentResolver().unregisterContentObserver(mOneHandObserver);

        if (mExclusiveDialog != null && mExclusiveDialog.isShowing()) {
            mExclusiveDialog.dismiss();
            mExclusiveDialog = null;
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mViewPager != null) {
            int position = mViewPager.getCurrentItem();
            if (position > 0) {
                mViewPager.setAdapter(mViewPagerAdapter);
                mViewPager.setOffscreenPageLimit(mViewPagerAdapter.getCount());
                mViewPager.setCurrentItem(position);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int position = mViewPagerAdapter.getItemPosition(preference.getKey());
        if (position >= 0) {
            mViewPager.setCurrentItem(position);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPrefSwitcher) {
            final boolean isChecked = (boolean) newValue;
            if (!isChecked) {
                Settings.System.putInt(getContentResolver(),
                        OneHandConstants.ONEHAND_ENABLED, 0);
            } else if (allowedReduceSize()) {
                Settings.System.putInt(getContentResolver(),
                        OneHandConstants.ONEHAND_ENABLED, 1);
            } else {
                popupExclusiveReduceSize();
            }
        } else if (preference == mPrefShowHardKeysOnScreen) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(),
                    OneHandConstants.ONEHAND_SHOW_HARD_KEYS, value ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference emiter) {
        updateRadioButtons(emiter, true);
    }

    private void updateRadioButtons(RadioButtonPreference emiter, boolean persist) {
        if (!HAS_TRIPLY_HOMEKEY) {
            return;
        }
        if (emiter == mPrefGeatureType) {
            mPrefButtonType.setChecked(false);
            mPrefGeatureType.setChecked(true);
            if (persist) {
                Settings.System.putInt(getContentResolver(),
                        OneHandConstants.ONEHAND_WAKEUP_TYPE, 0);
            }
        } else if (emiter == mPrefButtonType) {
            mPrefButtonType.setChecked(true);
            mPrefGeatureType.setChecked(false);
            if (persist) {
                Settings.System.putInt(getContentResolver(),
                        OneHandConstants.ONEHAND_WAKEUP_TYPE, 1);
            }
        }
    }

    private boolean allowedReduceSize() {
        Context context = getContext();
        boolean talkbackEnabled = Utils.isGoogleTalkBackEnabled(context);
        boolean magnificationGesturesEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) != 0;
        boolean magnificationNavbarEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) != 0;
        boolean autoClickPointerEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0) != 0;
        boolean navigationBarCollapsableEnabled = Settings.System.getInt(getContentResolver(),
                FreemeSettings.System.FREEME_NAVIGATION_BAR_COLLAPSABLE, 0) != 0;
        boolean navigationSwipeUpEnabled = Settings.System.getInt(getContentResolver(),
                FreemeSettings.System.FREEME_NAVIGATION_BAR_MODE, 0)
                == FreemeNavigation.Mode.SWIPEUP;
        boolean onestepEnabled = Settings.System.getInt(getContentResolver(),
                FreemeSettings.System.FREEME_ONESTEP_MODE, 1) != 0;
        return ! ( talkbackEnabled
                || magnificationGesturesEnabled
                || magnificationNavbarEnabled
                || autoClickPointerEnabled
                || navigationBarCollapsableEnabled
                || navigationSwipeUpEnabled
                || onestepEnabled
                );
    }

    private void popupExclusiveReduceSize() {
        StringBuilder message = new StringBuilder();
        if (Utils.hasPackage(getActivity(), "com.google.android.marvin.talkback")) {
            if (Utils.isRTL(getActivity())) {
                message.append("‏ ");
            }
            message.append("• ")
                    .append(getText(R.string.direct_access_actions_talkback_title))
                    .append("\n");
        }

        message.append("• ")
                .append(getText(R.string.accessibility_screen_magnification_title))
                .append("\n")
                .append("• ")
                .append(getText(R.string.accessibility_autoclick_preference_title));

        if (FreemeOption.Navigation.supports(
                FreemeOption.Navigation.FREEME_NAVIGATION_COLLAPSABLE)) {
            message.append("\n")
                    .append("• ")
                    .append(getText(R.string.navigation_key_virtual_hide_title));
        }
        if (FreemeOption.Navigation.supports(
                FreemeOption.Navigation.FREEME_NAVIGATION_SWIPEUP)) {
            message.append("\n")
                    .append("• ")
                    .append(getText(R.string.navigation_swipe_up_gesture_title));
        }
        if (FreemeOption.FREEME_ONESTEP_SUPPORT) {
            message.append("\n")
                    .append("• ")
                    .append(getText(R.string.onestep_title));
        }

        ContextThemeWrapper context = new ContextThemeWrapper(getActivity(),
                com.freeme.internal.R.style.Theme_Freeme_Light_Dialog_Alert);
        View dialogView = View.inflate(context, R.layout.accessibility_exclusive_popup, null);
        ((TextView) dialogView.findViewById(R.id.dialog_desc_string))
                .setText(R.string.onehand_settings_dialog_text);
        ((TextView) dialogView.findViewById(R.id.dialog_list_desc_string))
                .setText(message.toString());

        mExclusiveDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.onehand_settings_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Context context = getContext();
                        final ContentResolver resolver = context.getContentResolver();
                        Utils.turnOffGoogleTalkBack(context);
                        Settings.Secure.putInt(resolver,
                                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0);
                        Settings.Secure.putInt(resolver,
                                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0);
                        Settings.Secure.putInt(resolver,
                                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0);

                        if (FreemeOption.FREEME_ONESTEP_SUPPORT) {
                            Settings.System.putInt(resolver,
                                    FreemeSettings.System.FREEME_ONESTEP_MODE, 0);
                        }

                        if (FreemeOption.Navigation.supports(
                                FreemeOption.Navigation.FREEME_NAVIGATION_COLLAPSABLE)) {
                            Settings.System.putInt(resolver,
                                    FreemeSettings.System.FREEME_NAVIGATION_BAR_COLLAPSABLE, 0);
                            Settings.System.putInt(resolver,
                                    FreemeSettings.System.FREEME_NAVIGATION_BAR_MODE,
                                    FreemeNavigation.Mode.COLLAPSABLE);
                            showNavigationBar();
                        } else if (FreemeOption.Navigation.supports(
                                FreemeOption.Navigation.FREEME_NAVIGATION_FINGERPRINT)) {
                            Settings.System.putInt(resolver,
                                    FreemeSettings.System.FREEME_NAVIGATION_BAR_MODE,
                                    FreemeNavigation.Mode.FINGERPRINT);
                        } else if (FreemeOption.Navigation.supports(
                                FreemeOption.Navigation.FREEME_NAVIGATION_SWIPEUP)) {
                            Toast.makeText(context, R.string.onehand_settings_prompt_text,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Settings.System.putInt(resolver,
                                OneHandConstants.ONEHAND_ENABLED, 1);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(getContentResolver(),
                                OneHandConstants.ONEHAND_ENABLED, 0);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mPrefSwitcher.setChecked(Settings.System.getInt(getContentResolver(),
                                OneHandConstants.ONEHAND_ENABLED, 0) != 0);
                    }
                })
                .show();
    }

    private void changePagerIndicator(int position) {
        final LinearLayout group = mPointArea;
        final int count = mPointArea.getChildCount();
        for (int i = 0; i < count; i++) {
            ImageView image = (ImageView) group.getChildAt(i);
            image.setImageResource((i == position)
                    ? R.drawable.circle_on : R.drawable.circle_off);
        }
    }

    private void showNavigationBar() {
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.updateNavigationBar(false);
        } catch (RemoteException e) {
        }
    }
}
