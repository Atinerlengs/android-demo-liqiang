package com.freeme.game.settings;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import com.freeme.game.R;
import com.freeme.game.apppicker.GmAppPickerActivity;
import com.freeme.game.apppicker.loader.GmAppLoader;
import com.freeme.game.database.GmDatabaseConstant;
import com.freeme.game.receiver.GmAppInstallReceiver;
import com.freeme.game.utils.GmSettingsUtils;
import com.freeme.game.apppicker.loader.GmAppModel;
import com.freeme.game.widgets.GmPreference;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.util.FreemeOption;

public class GmSettingsActivity extends SettingsActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, GmSettingsFragment.class.getName());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
        return modIntent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.gm_label);
        setTitle(msg);
    }

    public static class GmSettingsFragment extends SettingsPreferenceFragment implements
            Preference.OnPreferenceClickListener,
            Preference.OnPreferenceChangeListener,
            GmAppInstallReceiver.IAppChangedCallBack {

        private final String KEY_GAME_MODE_PREF = "game_mode_pref";

        private final String KEY_GM_TOOL_CATEGORY = "gm_tool_category";
        private final String KEY_GM_TOOL_PREF = "gm_tool_pref";

        private final String KEY_GM_SETTINGS_CATEGORY = "gm_settings_category";
        private final String KEY_GM_ANSWER_CALL_PREF = "gm_answer_call_pref";
        private final String KEY_GM_BLOCK_NOTIFICATIONS_PREF = "gm_block_notifications_pref";
        private final String KEY_GM_LOCK_KEYS_PREF = "gm_lock_keys_pref";
        private final String KEY_GM_BLOCK_AUTO_BRIGHTNESS_PREF = "gm_block_auto_brightness_pref";

        private final String KEY_GM_APP_LIST_CATEGORY = "gm_app_list_category";
        private final String KEY_GM_ADD_APPS_PREF = "gm_add_apps_pref";

        private PreferenceCategory mGmToolCategory;
        private SwitchPreference mGmToolPref;
        private PreferenceCategory mGmSettingsCategory;
        private SwitchPreference mAnswerCallPref;
        private SwitchPreference mBlockNotifyPref;
        private SwitchPreference mLockKeysPref;
        private SwitchPreference mBlockBrightnessPref;
        private PreferenceCategory mAppListCategory;
        private Preference mAddAppsPref;

        private TextView mFooterView;

        private GmSettingsUtils mUtils;
        private GmAppLoader mLoader;

        private List<GmAppModel> mAppList = new ArrayList<>();

        private Activity mActivity;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mActivity = getActivity();

            ActionBar actionbar = mActivity.getActionBar();
            if (actionbar != null) {
                actionbar.setDisplayHomeAsUpEnabled(true);
            }

            FreemeActionBarUtil.setNavigateTitle(mActivity, getIntent());

            addPreferencesFromResource(R.xml.gm_settings_pref);

            mUtils = new GmSettingsUtils();
            Handler handler = new AppLoadedResultHandler(Looper.myLooper());
            mLoader = new GmAppLoader(mActivity, getPackageManager(), handler, getLoaderManager());
            GmAppInstallReceiver.registerCallBack(this);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            initPref();
            initPrefVisible();
        }

        @Override
        public void onResume() {
            super.onResume();
            updateListData();
            initPrefStatus();
        }

        @Override
        public void onDestroy() {
            GmAppInstallReceiver.unregisterCallBack(this);
            super.onDestroy();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    finish();
                    return true;
                default:
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            switch (preference.getKey()) {
                case KEY_GM_ADD_APPS_PREF: {
                    Intent intent = new Intent(mActivity, GmAppPickerActivity.class);
                    intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT, mActivity.getTitle());
                    startActivity(intent);
                }
                break;
                default:
                    break;
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean isSupport = (Boolean) newValue;
            switch (preference.getKey()) {
                case KEY_GAME_MODE_PREF:
                    mUtils.turnGameModeOn(mActivity, isSupport);
                    initPrefVisible();
                    initPrefStatus();
                    break;
                case KEY_GM_TOOL_PREF:
                    mUtils.turnGameToolOn(mActivity, isSupport);
                    break;
                case KEY_GM_ANSWER_CALL_PREF:
                    mUtils.setAnswerCallViaSpeaker(mActivity, isSupport);
                    break;
                case KEY_GM_BLOCK_NOTIFICATIONS_PREF:
                    mUtils.setBlockNotification(mActivity, isSupport);
                    break;
                case KEY_GM_LOCK_KEYS_PREF:
                    mUtils.setLockKeys(mActivity, isSupport);
                    break;
                case KEY_GM_BLOCK_AUTO_BRIGHTNESS_PREF:
                    mUtils.setBlockAutoBrightness(mActivity, isSupport);
                    break;
                default:
                    break;
            }
            return true;
        }


        @Override
        public void onAppChanged() {
            updateListData();
        }

        private void initPref() {
            PreferenceGroup group = getPreferenceScreen();

            SwitchPreference gameModePref = (SwitchPreference) group.findPreference(KEY_GAME_MODE_PREF);
            gameModePref.setChecked(mUtils.isGameModeTurnedOn(mActivity));
            gameModePref.setOnPreferenceChangeListener(this);

            mGmToolCategory = (PreferenceCategory) group.findPreference(KEY_GM_TOOL_CATEGORY);
            mGmToolPref = (SwitchPreference) group.findPreference(KEY_GM_TOOL_PREF);
            mGmToolPref.setOnPreferenceChangeListener(this);

            mGmSettingsCategory = (PreferenceCategory) group.findPreference(KEY_GM_SETTINGS_CATEGORY);

            mAnswerCallPref = (SwitchPreference) group.findPreference(KEY_GM_ANSWER_CALL_PREF);
            mAnswerCallPref.setOnPreferenceChangeListener(this);

            mBlockNotifyPref = (SwitchPreference) group.findPreference(KEY_GM_BLOCK_NOTIFICATIONS_PREF);
            mBlockNotifyPref.setOnPreferenceChangeListener(this);

            mLockKeysPref = (SwitchPreference) group.findPreference(KEY_GM_LOCK_KEYS_PREF);
            mLockKeysPref.setOnPreferenceChangeListener(this);

            mBlockBrightnessPref = (SwitchPreference) group.findPreference(KEY_GM_BLOCK_AUTO_BRIGHTNESS_PREF);
            mBlockBrightnessPref.setOnPreferenceChangeListener(this);

            mAppListCategory = (PreferenceCategory) group.findPreference(KEY_GM_APP_LIST_CATEGORY);

            mAddAppsPref = group.findPreference(KEY_GM_ADD_APPS_PREF);
            mAddAppsPref.setOnPreferenceClickListener(this);

            mFooterView = new TextView(mActivity);
            mFooterView.setText(R.string.gm_function_tips);
            mFooterView.setPadding(40, 0, 40, 40);
            getListView().setFooterDividersEnabled(false);
        }

        private void initPrefVisible() {
            PreferenceGroup group = getPreferenceScreen();
            if (mUtils.isGameModeTurnedOn(mActivity)) {
                group.addPreference(mGmToolCategory);
                if (!FreemeOption.FREEME_GAMEMODE_TOOL_SUPPORT) {
                    mGmToolCategory.removePreference(mGmToolPref);
                }
                group.addPreference(mGmSettingsCategory);
                group.addPreference(mAppListCategory);
                group.addPreference(mAddAppsPref);
                getListView().addFooterView(mFooterView);
            } else {
                group.removePreference(mGmToolCategory);
                group.removePreference(mGmSettingsCategory);
                group.removePreference(mAppListCategory);
                group.removePreference(mAddAppsPref);
                getListView().removeFooterView(mFooterView);
            }
        }

        private void initPrefStatus() {
            if (mGmToolPref != null) {
                mGmToolPref.setChecked(mUtils.isGameToolTurnedOn(mActivity));
            }
            if (mAnswerCallPref != null) {
                mAnswerCallPref.setChecked(mUtils.isAnswerCallViaSpeaker(mActivity));
            }
            if (mBlockNotifyPref != null) {
                mBlockNotifyPref.setChecked(mUtils.isBlockedNotification(mActivity));
            }
            if (mLockKeysPref != null) {
                mLockKeysPref.setChecked(
                        mUtils.isLockedKeys(mActivity, KeyEvent.KEYCODE_BACK)
                                || mUtils.isLockedKeys(mActivity, KeyEvent.KEYCODE_APP_SWITCH));
            }
            if (mBlockBrightnessPref != null) {
                mBlockBrightnessPref.setChecked(mUtils.isBlockedAutoBrightness(mActivity));
            }
        }

        private void updateListData() {
            if (!mLoader.isLoading()) {
                mLoader.initData(GmAppLoader.LOAD_TYPE_SELECTED_APP);
            }
        }

        private class AppLoadedResultHandler extends Handler {
            private AppLoadedResultHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                if (mAppListCategory != null) {
                    mAppListCategory.removeAll();
                    mAppList.clear();
                    mAppList.addAll(mLoader.getAppListByType(msg.what));
                    for (GmAppModel app : mAppList) {
                        GmPreference gmPref = new GmPreference(mActivity,
                                (Preference pre) -> {
                                    mAppListCategory.removePreference(pre);

                                    Bundle bundle = new Bundle();
                                    bundle.putString(GmDatabaseConstant.Columns.COLUMN_APP_PACKAGE_NAME,
                                            app.getPkgName());
                                    bundle.putInt(GmDatabaseConstant.Columns.COLUMN_APP_SELECTED, 0);
                                    getContentResolver().call(GmDatabaseConstant.CONTENT_URI,
                                            GmDatabaseConstant.Methods.METHOD_UPDATE, null, bundle);
                                });
                        gmPref.setIcon(app.getAppIcon());
                        gmPref.setTitle(app.getAppName());
                        mAppListCategory.addPreference(gmPref);
                    }
                }
            }
        }
    }
}
