package com.freeme.applock.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.internal.app.AppLockPolicy;
import com.freeme.applock.R;
import com.freeme.provider.FreemeSettings;

public class AppLockSettingsActivity extends Activity {
    private static final String TAG = AppLockSettingsActivity.class.getSimpleName();

    private Boolean isFromCStyle;
    Context mContext;
    private boolean mHasVerified;
    private AppLockListFragment mListFragment;

    private static final String KEY_HAS_VERIFIED = "has_verified";


    public static class AppLockListFragment extends ListFragment implements OnClickListener {
        private AppLockSettingsActivity mActivity;
        private PackageListAdapter mAdapter;
        Context mContext;
        View mHeadView;
        TextView mLockTypeSummary;
        TextView mLockTypeTitle;
        LinearLayout mLockTypeView;
        TextView mAppCategoryText;
        TextView mAppLockDescription;
        private FingerprintManager mFingerprintManager;

        private boolean mIsMasterOn;
        private Switch mMasterSwitch;
        private TextView mMasterText;
        private LinearLayout mSwitchBar;
        private boolean mIsDBChanged;
        private PackageInfoUtil mPackageInfoUtil;

        private final ContentObserver mAppLockTypeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                LogUtil.i(TAG, "onChange() LockType");
                updateLockTypeView();
            }
        };

        private final Handler mFragmentHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case AppLockUtil.UPDATE_LIST_VIEW:
                    case AppLockUtil.STATES_PACKAGE_REMOVED:
                        if (mAdapter != null) {
                            mAdapter.bindData();
                        }
                        break;
                    case AppLockUtil.UPDATE_LOCK_TYPE:
                        updateLockTypeSummary();
                        if (mAdapter != null) {
                            mAdapter.bindData();
                        }
                        break;
                }
                updateView();
            }
        };


        private final ContentObserver mLockedAppsDBObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                LogUtil.i(TAG, "mLockedAppsDBObserver DB changed: mIsMasterOn = " + mIsMasterOn);
                mIsDBChanged = mIsMasterOn;
            }
        };

        OnClickListener mSwitchBarClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                setMasterSwitchView(!mMasterSwitch.isChecked());
            }
        };

        public OnCheckedChangeListener mSwitchChangeListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LogUtil.i(TAG, "onCheckedChanged : isChecked = " + isChecked);
                int lockType = AppLockUtil.getLockType(mContext);
                if (isChecked) {
                    if (lockType == AppLockPolicy.LOCK_TYPE_NONE) {
                        callLockType(AppLockUtil.REQUEST_CODE_SET_LOCK);
                    } else {
                        mPackageInfoUtil.setMasterValue(mContext, true);
                        mIsMasterOn = true;
                    }
                    mMasterText.setText(R.string.switch_on_text);
                    mFragmentHandler.sendMessage(
                            mFragmentHandler.obtainMessage(AppLockUtil.UPDATE_LIST_VIEW));
                } else if (lockType != AppLockPolicy.LOCK_TYPE_NONE) {
                    popupConfirmDialog();
                } else {
                    mPackageInfoUtil.setMasterValue(mContext, false);
                    mMasterText.setText(R.string.switch_off_text);
                }
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = getActivity();
            mAdapter = new PackageListAdapter(getActivity(), mFragmentHandler);
            mFingerprintManager = (FingerprintManager) mContext.getSystemService(
                    Context.FINGERPRINT_SERVICE);
            setListAdapter(mAdapter);
            mPackageInfoUtil = PackageInfoUtil.getInstance();
            mPackageInfoUtil.getLauncherApps(getActivity().getApplicationContext());
            mIsMasterOn = mPackageInfoUtil.getMasterValue(mContext);
            if (mIsMasterOn) {
                loadDB();
                mIsDBChanged = false;
            }
            getActivity().getContentResolver().registerContentObserver(
                    Secure.getUriFor(PackageInfoUtil.LOCKED_PACKAGE),true, mLockedAppsDBObserver);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if (mHeadView == null) {
                mHeadView = LayoutInflater.from(getActivity()).inflate(
                        R.layout.applock_detail_layout, null);
                mLockTypeView = (LinearLayout) mHeadView.findViewById(R.id.applock_type_container);
                mLockTypeTitle = (TextView) mHeadView.findViewById(R.id.applock_locktype_title);
                mLockTypeSummary = (TextView) mHeadView.findViewById(R.id.applock_locktype_summary);
                mAppCategoryText = (TextView) mHeadView.findViewById(R.id.applock_app_category);
                mAppLockDescription = (TextView) mHeadView.findViewById(R.id.headview_description);
                mSwitchBar = (LinearLayout) mHeadView.findViewById(R.id.switch_bar);
                mMasterSwitch = (Switch) mHeadView.findViewById(R.id.switch_widget);
                mMasterText = (TextView) mHeadView.findViewById(R.id.switch_text);
            }

            View view = inflater.inflate(R.layout.applock_lock_settings_layout, container, false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().addHeaderView(mHeadView, null, true);
            getListView().setHeaderDividersEnabled(false);
            getListView().setItemsCanFocus(true);
            getListView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mLockTypeView.setClickable(true);
            mLockTypeView.setOnClickListener(this);
            mAppCategoryText.setText(getActivity().getResources().getString(
                    R.string.applock_app_category));
            AppLockUtil.setMaxFontScale(mContext, mMasterText);
            setMasterSwitchView(mIsMasterOn);
            if (isShopDemo()) {
                mMasterSwitch.setEnabled(false);
                mSwitchBar.setEnabled(false);
            } else {
                mMasterSwitch.setOnCheckedChangeListener(mSwitchChangeListener);
                mSwitchBar.setOnClickListener(mSwitchBarClickListener);
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mActivity = (AppLockSettingsActivity) context;
        }

        private void updateView() {
            updateCountViewState();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mIsMasterOn = mPackageInfoUtil.getMasterValue(mContext);
            mPackageInfoUtil.sortList();
            if (mIsMasterOn && mIsDBChanged) {
                loadDB();
                mFragmentHandler.sendMessage(mFragmentHandler.obtainMessage(
                        AppLockUtil.LOCK_STATE_CHANGE));
            }
            mIsDBChanged = false;
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                    PackageInfoUtil.MARK_PREF_NAME, MODE_PRIVATE);
            boolean remove_mark = sharedPreferences.getBoolean(
                    PackageInfoUtil.PACKAGE_REMOVE, false);
            boolean add_mark = sharedPreferences.getBoolean(PackageInfoUtil.PACKAGE_ADD, false);
            if (remove_mark || add_mark) {
                mPackageInfoUtil.getLauncherApps(mContext);
                mFragmentHandler.sendMessage(mFragmentHandler.obtainMessage(
                        AppLockUtil.STATES_PACKAGE_REMOVED));
                Editor editor = sharedPreferences.edit();
                editor.putBoolean(PackageInfoUtil.PACKAGE_REMOVE, false);
                editor.putBoolean(PackageInfoUtil.PACKAGE_ADD, false);
                editor.apply();
            }
            if (AppLockUtil.getLockType(mContext) == AppLockPolicy.LOCK_TYPE_NONE) {
                mMasterSwitch.setChecked(false);
            }
            mActivity.getContentResolver().registerContentObserver(
                    Secure.getUriFor(FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE),
                    true, mAppLockTypeObserver);
            mActivity.getContentResolver().registerContentObserver(
                    Secure.getUriFor(FreemeSettings.Secure.FREEME_APPLOCK_ENABLED),
                    true, mAppLockTypeObserver);
            updateView();
            updateLockTypeView();
        }

        @Override
        public void onPause() {
            super.onPause();
            mPackageInfoUtil.updateDB(mContext, false, null);
            mPackageInfoUtil.sendStatusBroadcast(mContext, null,
                    AppLockUtil.APPLOCKED_STATUS_ACTION);
            getActivity().getContentResolver().unregisterContentObserver(mAppLockTypeObserver);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getActivity().getContentResolver().unregisterContentObserver(mLockedAppsDBObserver);
        }

        public void updateLockTypeView() {
            boolean state = Secure.getInt(mActivity.getContentResolver(),
                    FreemeSettings.Secure.FREEME_APPLOCK_ENABLED, 0) == 1;
            mLockTypeView.setEnabled(state);
            mLockTypeTitle.setEnabled(state);
            mLockTypeSummary.setEnabled(state);
            updateLockTypeSummary();
        }

        private int checkLockType() {
            boolean hasEnrolledFingers = mFingerprintManager.isHardwareDetected()
                    && mFingerprintManager.hasEnrolledFingerprints();
            int lockType = AppLockUtil.getLockType(getActivity());
            if (AppLockUtil.isFingerPrint(lockType)) {
                if (!hasEnrolledFingers) {
                    lockType -= AppLockPolicy.LOCK_TYPE_FINGERPRINT;
                }
            }
            return lockType;
        }

        private String getBasicLockTypeSummary(int lockType) {
            switch (lockType) {
                case AppLockPolicy.LOCK_TYPE_PATTERN:
                    return mContext.getResources().getString(R.string.unlock_set_unlock_pattern_title);
                case AppLockPolicy.LOCK_TYPE_PIN:
                    return mContext.getResources().getString(R.string.unlock_set_unlock_pin_title);
                case AppLockPolicy.LOCK_TYPE_PASSWORD:
                    return mContext.getResources().getString(R.string.unlock_set_unlock_password_title);
                default:
                    return null;
            }
        }

        public void updateLockTypeSummary() {
            switch (checkLockType()) {
                case AppLockPolicy.LOCK_TYPE_NONE:
                    mLockTypeSummary.setText(R.string.applock_type_no_selected);
                    break;
                case AppLockPolicy.LOCK_TYPE_PATTERN:
                    mLockTypeSummary.setText(R.string.unlock_set_unlock_pattern_title);
                    break;
                case AppLockPolicy.LOCK_TYPE_PIN:
                    mLockTypeSummary.setText(R.string.unlock_set_unlock_pin_title);
                    break;
                case AppLockPolicy.LOCK_TYPE_PASSWORD:
                    mLockTypeSummary.setText(R.string.unlock_set_unlock_password_title);
                    break;
                case AppLockPolicy.LOCK_TYPE_FINGERPRINT:
                case AppLockPolicy.LOCK_TYPE_FINGERPRINT_PATTERN:
                case AppLockPolicy.LOCK_TYPE_FINGERPRINT_PIN:
                case AppLockPolicy.LOCK_TYPE_FINGERPRINT_PASSWORD:
                    mLockTypeSummary.setText(AppLockUtil.StringCat(
                            getBasicLockTypeSummary(AppLockUtil.getBasicLockType(getActivity())),
                            mContext.getResources().getString(R.string.applock_type_fingerprint)));
                    break;
                default:
                    break;
            }
        }

        public void updateCountNumberCategory(int countNumber) {
            if (countNumber > 0) {
                mAppCategoryText.setText(mContext.getResources().getString(
                        R.string.applock_app_category_with_num,
                        new Object[]{Integer.valueOf(countNumber)}));
            } else {
                mAppCategoryText.setText(mContext.getResources().getString(
                        R.string.applock_app_category));
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.applock_type_container:
                    callLockType(AppLockUtil.REQUEST_CODE_SET_LOCK);
                    break;
            }
        }

        private void loadDB() {
            mPackageInfoUtil.loadDBState(mContext);
            mPackageInfoUtil.loadFolderInfo(mContext);
        }

        private void updateCountViewState() {
            updateCountNumberCategory(mPackageInfoUtil.countLockedApps(mContext));
        }

        public class DialogOnKeyListenner implements OnKeyListener {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                    mMasterSwitch.setChecked(true);
                }
                return false;
            }
        }

        public void popupConfirmDialog() {
            AlertDialog comfirmDialog = new Builder(getActivity())
                    .setTitle(R.string.applock_disable_popup_title)
                    .setMessage(R.string.applock_disable_popup_body)
                    .setPositiveButton(R.string.applock_disable_popup_opt_keep,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPackageInfoUtil.setMasterValue(mContext, false);
                            mMasterText.setText(R.string.switch_off_text);
                            mFragmentHandler.sendMessage(mFragmentHandler.obtainMessage(
                                    AppLockUtil.UPDATE_LIST_VIEW));
                            mIsMasterOn = false;
                        }
                    }).setNegativeButton(R.string.applock_disable_popup_opt_disgard,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mPackageInfoUtil.setMasterValue(mContext, false);
                            mMasterText.setText(R.string.switch_off_text);
                            mIsMasterOn = false;
                            removeLocktype();
                        }
                    }).setNeutralButton(R.string.applock_disable_popup_opt_cancel,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mMasterSwitch.setChecked(true);
                        }
                    }).create();
            comfirmDialog.setCanceledOnTouchOutside(false);
            comfirmDialog.setOnKeyListener(new DialogOnKeyListenner());
            comfirmDialog.show();
        }

        public void setMasterSwitchView(Boolean isMasterOn) {
            if (isMasterOn) {
                mMasterSwitch.setChecked(true);
                mMasterText.setText(R.string.switch_on_text);
            } else {
                mMasterSwitch.setChecked(false);
                mMasterText.setText(R.string.switch_off_text);
            }
        }

        private boolean isShopDemo() {
            return Secure.getInt(getActivity().getContentResolver(), "shopdemo", 0) == 1;
        }

        public void removeLocktype() {
            Secure.putInt(getActivity().getContentResolver(),
                    FreemeSettings.Secure.FREEME_APPLOCK_LOCK_TYPE,
                    AppLockPolicy.LOCK_TYPE_NONE);
            mFragmentHandler.sendMessage(mFragmentHandler.obtainMessage(
                    AppLockUtil.UPDATE_LOCK_TYPE));
        }

        private void callLockType(int requestCode) {
            Bundle args = new Bundle();
            args.putInt("firstStart", requestCode);
            Intent intent = new Intent(getActivity(), AppLockTypeActivity.class);
            intent.putExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT,
                    getActivity().getTitle().toString());
            startActivity(intent);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FreemeActionBarUtil.setNavigateTitle(this,getIntent());
        mContext = getApplicationContext();
        if (savedInstanceState != null) {
            mHasVerified = savedInstanceState.getBoolean(KEY_HAS_VERIFIED);
            LogUtil.i(TAG, "savedInstanceState != null: mHasVerified=" + mHasVerified);
        }
        if (AppLockUtil.getLockType(mContext) != AppLockPolicy.LOCK_TYPE_NONE && !mHasVerified) {
            startVerifyActivity();
        }
        initView();
        isFromCStyle = getIntent().getBooleanExtra("is_from_cstyle", false);
    }

    public void initView() {
        setContentView(R.layout.applock_main_layout);
        mListFragment = (AppLockListFragment) getFragmentManager().findFragmentById(R.id.list_fragement);
    }


    public void startVerifyActivity() {
        String action = ((ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE)).getAppLockedCheckAction();
        if (action != null) {
            Intent intent = new Intent(action);
            intent.putExtra(AppLockPolicy.LAUNCH_FROM_SETTINGS, true);
            try {
                startActivityForResult(intent, AppLockUtil.REQUEST_CODE_VERIFY_LOCK);
            } catch (ActivityNotFoundException e) {
                LogUtil.d(TAG, "REQUEST_CODE_VERIFY_LOCK Activity not found!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_HAS_VERIFIED, mHasVerified);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (isFromCStyle && mListFragment.mIsMasterOn) {
            setResult(RESULT_OK);
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case AppLockUtil.REQUEST_CODE_SET_LOCK /*10001*/:
                if (resultCode == RESULT_OK) {
                    LogUtil.i(TAG, "REQUEST_CODE_SET_LOCK RESULT_OK");
                } else {
                    finish();
                }
                break;
            case AppLockUtil.REQUEST_CODE_VERIFY_LOCK /*10002*/:
                if (resultCode == RESULT_OK) {
                    mHasVerified = true;
                    LogUtil.i(TAG, "REQUEST_CODE_VERIFY_LOCK RESULT_OK");
                } else {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
