package com.freeme.contacts.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.model.account.AccountsLoader;
import com.android.contacts.model.account.FallbackAccountType;
import com.android.contacts.util.AccountFilterUtil;
import com.android.contacts.util.AccountSelectionUtil;
import com.android.contacts.vcard.VCardCommonArguments;
import com.freeme.preference.FreemeJumpPreference;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.list.ContactsIntentResolverEx;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.widget.ImportExportItem;
import com.mediatek.storage.StorageManagerEx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FreemeContactsSettingsActivity extends PreferenceActivity
        implements AccountsLoader.AccountsListener,
        FreemeContactsCopyUtils.IFreemePickDataCacheCallBack {

    private static final String TAG = "FreemeContactsSettingsActivity";
    private AccountWithDataSetEx mCheckedAccount1 = null;
    private AccountWithDataSetEx mCheckedAccount2 = null;

    private List<AccountWithDataSetEx> mAccounts = null;
    private LinkedHashMap<String, ListViewItemObject> mLinkedItemObject = new LinkedHashMap<>();

    public static final String KEY_COPY_CONTACT = "copy_contact";
    public static final String STORAGE_ACCOUNT_TYPE = "_STORAGE_ACCOUNT";

    private static final int SELECTION_VIEW_STEP_NONE = 0;
    private static final int SELECTION_VIEW_STEP_ONE = 1;
    private static final int SELECTION_VIEW_STEP_TWO = 2;
    public static final int REQUEST_CODE = 11111;
    public static final int RESULT_CODE = 11112;

    private String mCallingActivityName = null;
    private String mKey;

    private int mShowingStep = SELECTION_VIEW_STEP_NONE;
    private PreferenceCategory mCopyContact;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }

        com.freeme.actionbar.app.FreemeActionBarUtil.setNavigateTitle(this, getIntent());
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(TAG, "[onCreate] callingActivity has no putExtra");
            finish();
            return;
        } else {
            mCallingActivityName = extras.getString(
                    VCardCommonArguments.ARG_CALLING_ACTIVITY, null);
            if (mCallingActivityName == null) {
                Log.e(TAG, "[onCreate] callingActivity = null and return");
                finish();
                return;
            }
        }
        addPreferencesFromResource(R.xml.freeme_contacts_settings);
        mCopyUtils = new FreemeContactsCopyUtils(this);
        mAccountTypes = AccountTypeManager.getInstance(this);
        AccountsLoader.loadAccounts(this, 0, AccountTypeManager.writableFilter());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "[onActivityResult]requestCode:" + requestCode + ",resultCode:"
                + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FreemeContactsSettingsActivity.REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (data != null) {
                        mCheckedContactIds = data.getLongArrayExtra("checkedIds");
                        setShowingStep(SELECTION_VIEW_STEP_TWO);
                        mIsFirstEntry = false;
                        updateUi();
                    }
                    break;
                case 200://NO CHECKED CONTACTS
                    break;
                case FreemeContactsSettingsActivity.RESULT_CODE:
                    resetStep();
                    return;
                default:
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mShowingStep > SELECTION_VIEW_STEP_ONE) {
            onBackAction();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        mIsFinished = true;
        super.onDestroy();
        Log.i(TAG, "[onDestroy]");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAccountsLoaded(List<AccountInfo> data) {

        final List<AccountWithDataSetEx> accounts = extractAccountsEx(data);
        // /check whether the Activity's status still ok
        if (isActivityFinished()) {
            Log.w(TAG, "[onLoadFinished]isActivityFinished is true,return.");
            return;
        }
        if (accounts == null) { // Just in case...
            Log.e(TAG, "[onLoadFinished]data is null,return.");
            return;
        }
        Log.d(TAG, "[onLoadFinished]data = " + accounts);
        if (mAccounts == null) {
            mAccounts = accounts;
            // Add all of storages accounts
            mAccounts.addAll(getStorageAccounts());
            // If the accounts size is less than one item, we should not
            // show this view for user to import or export operations.
            if (mAccounts.size() <= 1) {
                Log.i(TAG, "[onLoadFinished]mAccounts.size = " + mAccounts.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                R.string.xport_error_one_account, Toast.LENGTH_SHORT).show();
                    }
                });
                finish();
            }
            Log.i(TAG, "[onLoadFinished]mAccounts.size() = " + mAccounts.size() + ",mAccounts:"
                    + mAccounts + ",mShowingStep =" + mShowingStep);
            if (mShowingStep == SELECTION_VIEW_STEP_NONE) {
                setShowingStep(SELECTION_VIEW_STEP_ONE);
            } else {
                setShowingStep(mShowingStep);
            }
            setCheckedAccount(mKey);
            updateUi();
        }
    }

    private FreemeJumpPreference mFreemeJumpPreference;

    private void updateUi() {
        mCopyContact = (PreferenceCategory) findPreference(KEY_COPY_CONTACT);
        mCopyContact.removeAll();
        for (ListViewItemObject checkedAccount : mLinkedItemObject.values()) {
            mFreemeJumpPreference = new FreemeJumpPreference(this);
            mFreemeJumpPreference.setKey(
                    checkedAccount.mAccount.type + checkedAccount.mAccount.getSubId());

            if (mShowingStep == SELECTION_VIEW_STEP_ONE) {
                if (checkedAccount.slotId() != 0) {
                    mFreemeJumpPreference.setTitle(
                            getString(R.string.freeme_contacts_settings_copy_contacts_from_sim,
                                    checkedAccount.slotId()));
                } else {
                    mFreemeJumpPreference.setTitle(
                            getString(R.string.freeme_contacts_settings_copy_contacts_from,
                                    checkedAccount.getTitle()));
                }
            } else {
                if (checkedAccount.slotId() != 0) {
                    mFreemeJumpPreference.setTitle(
                            getString(R.string.freeme_contacts_settings_copy_contacts_to_sim,
                                    checkedAccount.slotId()));
                } else {
                    mFreemeJumpPreference.setTitle(
                            getString(R.string.freeme_contacts_settings_copy_contacts_to,
                                    checkedAccount.getTitle()));
                }
            }

            mCopyContact.addPreference(mFreemeJumpPreference);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        mKey = preference.getKey();
        if (mLinkedItemObject.get(mKey) != null) {
            onNextAction(mKey);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean mIsFirstEntry = true;

    private void onNextAction(String key) {
        Log.d(TAG, "[onNextAction] mShowingStep = " + mShowingStep);
        setCheckedAccount(mKey);
        if (mShowingStep == SELECTION_VIEW_STEP_ONE) {
            if (!isStorageAccount(mCheckedAccount1)) {
                doPickContacts();
                return;
            }
        }
        if (mShowingStep >= SELECTION_VIEW_STEP_TWO) {
            doImportExport();
            return;
        }
        setShowingStep(SELECTION_VIEW_STEP_TWO);
        if (mIsFirstEntry || (mCheckedAccount1 == null && mCheckedAccount2 == null)) {
            mKey = null;
        } else {
            mKey = key;
        }
        mIsFirstEntry = false;
        updateUi();
    }

    private void onBackAction() {
        setShowingStep(SELECTION_VIEW_STEP_ONE);
        setCheckedAccount(mKey);
        updateUi();
    }

    private void setCheckedAccount(String key) {
        if (mLinkedItemObject.size() == 0) {
            Log.e(TAG, "[setCheckedAccount]mLinkedItemObject.size() == 0");
            finish();
            return;
        }
        if (key == null) {
            return;
        }
        if (mShowingStep == SELECTION_VIEW_STEP_ONE) {
            mCheckedAccount1 = mLinkedItemObject.get(key).mAccount;
        } else if (mShowingStep == SELECTION_VIEW_STEP_TWO) {
            mCheckedAccount2 = mLinkedItemObject.get(key).mAccount;
        }
        Log.d(TAG, "[setCheckedAccount]mCheckedAccount1 = " + mCheckedAccount1
                + ",mCheckedAccount2 =" + mCheckedAccount2 + ",key = " + key);
    }

    private void setShowingStep(int showingStep) {
        mShowingStep = showingStep;
        mLinkedItemObject.clear();

        Log.d(TAG, "[setShowingStep]mShowingStep = " + mShowingStep);
        if (mShowingStep == SELECTION_VIEW_STEP_ONE) {
            for (AccountWithDataSetEx account : mAccounts) {
                /// For MTK multiuser in 3gdatasms @{
                if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
                    int userId = UserHandle.myUserId();
                    Log.d(TAG, "[setShowingStep]MTK_ONLY_OWNER_SIM_SUPPORT is true,userId : "
                            + userId);
                    if (userId != UserHandle.USER_OWNER) {
                        AccountType accountType = getAccountType(
                                account.type, account.dataSet, true);
                        if (accountType.isIccCardAccount()) {
                            Log.d(TAG, "[setShowingStep]isIccCardAccount,accountType: "
                                    + accountType);
                            continue;
                        }
                    }
                }
                /// @}
                mLinkedItemObject.put(account.type + account.getSubId(),
                        new ListViewItemObject(account, this));
            }
        } else if (mShowingStep == SELECTION_VIEW_STEP_TWO) {
            for (AccountWithDataSetEx account : mAccounts) {
                if (!mCheckedAccount1.equals(account)) {
                    /*
                     * It is not allowed for the importing from Storage -> SIM
                     * or USIM and from SIM or USIM -> Storage and also is not
                     * for importing from Storage -> Storage
                     */
                    AccountType accountType = getAccountType(account.type, account.dataSet,
                            true);
                    AccountType checkedAccountType = getAccountType(
                            mCheckedAccount1.type, mCheckedAccount1.dataSet, true);
                    Log.d(TAG, "[setShowingStep]accountType: " + accountType +
                            ", checkedAccountType: " + checkedAccountType);
                    if ((isStorageAccount(mCheckedAccount1) && accountType.isIccCardAccount()) ||
                            (checkedAccountType.isIccCardAccount() && isStorageAccount(account)) ||
                            (isStorageAccount(mCheckedAccount1) && isStorageAccount(account))) {
                        continue;
                    }

                    /// For MTK multiuser in 3gdatasms @{
                    if (ContactsSystemProperties.MTK_OWNER_SIM_SUPPORT) {
                        int userId = UserHandle.myUserId();
                        if (userId != UserHandle.USER_OWNER) {
                            if (accountType.isIccCardAccount()) {
                                continue;
                            }
                        }
                    }
                    /// @}
                    mLinkedItemObject.put(account.type + account.getSubId(),
                            new ListViewItemObject(account, this));
                }
            }
        }
    }

    public List<AccountWithDataSetEx> getStorageAccounts() {
        List<AccountWithDataSetEx> storageAccounts = new ArrayList<AccountWithDataSetEx>();
        StorageManager storageManager = (StorageManager) getApplicationContext()
                .getSystemService(STORAGE_SERVICE);
        if (null == storageManager) {
            Log.w(TAG, "[getStorageAccounts]storageManager is null!");
            return storageAccounts;
        }

        if (ContactsPortableUtils.MTK_STORAGE_SUPPORT) {
            try {
                String defaultStoragePath = StorageManagerEx.getDefaultPath();
                if (!storageManager.getVolumeState(defaultStoragePath).equals(
                        Environment.MEDIA_MOUNTED)) {
                    Log.w(TAG, "[getStorageAccounts]State is  not MEDIA_MOUNTED!");
                    return storageAccounts;
                }
            } catch (Exception e) {
                Log.e(TAG, "StorageManagerEx.getDefaultPath native exception!");
                e.printStackTrace();
            }
        }

        // change for ALPS02390380, different user can use different storage, so change the API
        // to user related API.
        StorageVolume volumes[] = StorageManager.getVolumeList(UserHandle.myUserId(),
                StorageManager.FLAG_FOR_WRITE);
        if (volumes != null) {
            Log.d(TAG, "[getStorageAccounts]volumes are: " + volumes);
            for (StorageVolume volume : volumes) {
                String path = volume.getPath();
                ///[ALPS03465894]Add check for Sdcard inject.
                String state = Environment.getExternalStorageState(volume.getPathFile());
                Log.d(TAG, "[getStorageAccounts]path:" + path + ", state=" + state);
                if (!state.equals(Environment.MEDIA_MOUNTED)) {
                    continue;
                }
                storageAccounts.add(new AccountWithDataSetEx(volume.getDescription(this),
                        STORAGE_ACCOUNT_TYPE, path));
            }
        }
        return storageAccounts;
    }

    private List<AccountWithDataSetEx> extractAccountsEx(List<AccountInfo> data) {

        List<AccountWithDataSet> accounts = AccountInfo.extractAccounts(data);
        List<AccountWithDataSetEx> accountsEx = new ArrayList<AccountWithDataSetEx>();

        for (AccountWithDataSet account : accounts) {
            AccountType accountType = getAccountType(account.type, account.dataSet,
                    false);
            Log.d(TAG, "[loadAccountFilters]account.type = " + account.type
                    + ",account.name =" + account.name);
            if (accountType.isExtension() && !account.hasData(this)) {
                Log.d(TAG, "[loadAccountFilters]continue.");
                // Hide extensions with no raw_contacts.
                continue;
            }
            int subId = SubInfoUtils.getInvalidSubId();
            if (account instanceof AccountWithDataSetEx) {
                subId = ((AccountWithDataSetEx) account).getSubId();
            }
            Log.d(TAG, "[loadAccountFilters]subId = " + subId);
            accountsEx.add(new AccountWithDataSetEx(account.name, account.type, subId));
        }

        return accountsEx;

    }

    private AccountType getAccountType(String type, String dataSet, boolean supportStorage) {
        AccountType accountType = AccountTypeManager.
                getInstance(this).getAccountType(type, dataSet);
        if (null == accountType && supportStorage) {
            if (STORAGE_ACCOUNT_TYPE.equalsIgnoreCase(type)) {
                accountType = new FallbackAccountType(this);
            }
        }
        return accountType;
    }

    private boolean mIsFinished = false;

    private boolean isActivityFinished() {
        return mIsFinished;
    }

    AccountTypeManager mAccountTypes;

    private class ListViewItemObject {
        public AccountWithDataSetEx mAccount;
        public ImportExportItem mView;
        Context mContext;

        public ListViewItemObject(AccountWithDataSetEx account, Context context) {
            mAccount = account;
            mContext = context;
        }

        public String getName() {
            if (mAccount == null) {
                Log.w(TAG, "[getName]mAccount is null!");
                return "null";
            } else {
                String displayName = null;
                displayName = AccountFilterUtil.getAccountDisplayNameByAccount(mAccount.type,
                        mAccount.name);
                Log.d(TAG, "[getName]type : " + mAccount.type + ",name:" + mAccount.name
                        + ",displayName:" + displayName);
                if (TextUtils.isEmpty(displayName)) {
                    if (AccountWithDataSetEx.isLocalPhone(mAccount.type)) {
                        return getString(R.string.account_phone_only);
                    }
                    return mAccount.name;
                } else {
                    return displayName;
                }
            }
        }

        public String getTitle() {
            final AccountType accountType = mAccountTypes.getAccountType(mAccount.type,
                    mAccount.dataSet);
            String type = getName();
            final int subId = mAccount.getSubId();
            Log.d(TAG, "[getView]dataSet: " + mAccount.dataSet + ",subId: " + subId);
            if (accountType != null && accountType.isIccCardAccount()) {
                type = (String) accountType.getDisplayLabel(mContext);
            } else if (accountType != null) {
            }
            return type;
        }

        public int slotId() {
            return SubInfoUtils.getSlotIdUsingSubId(mAccount.getSubId()) + 1;
        }
    }

    public void doImportExport() {
        Log.i(TAG, "[doImportExport]...");

        if (AccountTypeUtils.isAccountTypeIccCard(mCheckedAccount1.type)) {
            // UIM
            int subId = ((AccountWithDataSetEx) mCheckedAccount1).getSubId();
            /** change for PHB Status Refactoring @{ */
            if (!SimCardUtils.isPhoneBookReady(subId)) {
                Toast.makeText(this, R.string.icc_phone_book_invalid, Toast.LENGTH_LONG).show();
                finish();
                Log.i(TAG, "[doImportExport] phb is not ready.");
            } else {
                handleImportExportAction();
            }
            /** @} */
        } else {
            handleImportExportAction();
        }
    }

    private void handleImportExportAction() {
        Log.d(TAG, "[handleImportExportAction]...");
        if (isStorageAccountUnAvaliable()) {
            return;
        }

        if (isStorageAccount(mCheckedAccount1)) { // import from SDCard
            if (mCheckedAccount2 != null) {
                AccountSelectionUtil.doImportFromSdCard(this, mCheckedAccount1.dataSet,
                        mCheckedAccount2);
            }
        } else {
            if (isStorageAccount(mCheckedAccount2)) { // export to SDCard
                if (isSDCardFull(mCheckedAccount2.dataSet)) { // SD card is full
                    Log.i(TAG, "[handleImportExportAction] isSDCardFull");
                    new AlertDialog.Builder(this)
                            .setMessage(R.string.storage_full)
                            .setTitle(R.string.storage_full)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    }).show();
                    return;
                }

                mCopyUtils.doExportVCardToSDCard();
            } else { // account to account
                mCopyUtils.doCopyContacts();
            }
        }
    }

    private static boolean isStorageAccount(final AccountWithDataSetEx account) {
        if (account != null) {
            return STORAGE_ACCOUNT_TYPE.equalsIgnoreCase(account.type);
        }
        return false;
    }

    private boolean checkSDCardAvaliable(final String path) {
        if (TextUtils.isEmpty(path)) {
            Log.w(TAG, "[checkSDCardAvaliable]path is null!");
            return false;
        }
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        if (null == storageManager) {
            Log.i(TAG, "[checkSDCardAvaliable] story manager is null");
            return false;
        }
        String storageState = storageManager.getVolumeState(path);
        Log.d(TAG, "[checkSDCardAvaliable]path = " + path + ",storageState = " + storageState);
        return storageState.equals(Environment.MEDIA_MOUNTED);
    }

    private boolean isSDCardFull(final String path) {
        if (TextUtils.isEmpty(path)) {
            Log.w(TAG, "[isSDCardFull]path is null!");
            return false;
        }
        Log.d(TAG, "[isSDCardFull] storage path is " + path);
        if (checkSDCardAvaliable(path)) {
            StatFs sf = null;
            try {
                sf = new StatFs(path);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "[isSDCardFull]catch exception:");
                e.printStackTrace();
                return false;
            }
            long availCount = sf.getAvailableBlocks();
            return !(availCount > 0);
        }

        return true;
    }


    private FreemeContactsCopyUtils mCopyUtils;

    private void doPickContacts() {
        Log.d(TAG, "[handleImportExportAction]...");
        if (isStorageAccountUnAvaliable()) {
            return;
        }

        if (isStorageAccount(mCheckedAccount1)) { // import from SDCard
            if (mCheckedAccount2 != null) {
                AccountSelectionUtil.doImportFromSdCard(this, mCheckedAccount1.dataSet,
                        mCheckedAccount2);
            }
        } else {
            Intent intent = new Intent(this,
                    com.mediatek.contacts.list.ContactListMultiChoiceActivity.class)
                    .setAction(ContactsIntent.LIST.ACTION_PICK_MULTI_CONTACTS)
                    .putExtra("request_type",
                            ContactsIntentResolverEx.REQ_TYPE_IMPORT_EXPORT_PICKER)
                    .putExtra("fromaccount", mCheckedAccount1)
                    .putExtra("toaccount", mCheckedAccount2)
                    .putExtra("do_immediately", false)
                    .putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY, mCallingActivityName);
            startActivityForResult(intent, FreemeContactsSettingsActivity.REQUEST_CODE);
        }
    }

    private boolean isStorageAccountUnAvaliable() {
        boolean isUnAvaliable = false;
        if (isStorageAccountUnAvaliable(mCheckedAccount1)
                || isStorageAccountUnAvaliable(mCheckedAccount2)) {
            new AlertDialog.Builder(this).setMessage(R.string.no_sdcard_message)
                    .setTitle(R.string.no_sdcard_title)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .show();
            isUnAvaliable = true;
        }
        return isUnAvaliable;
    }

    private boolean isStorageAccountUnAvaliable(final AccountWithDataSetEx account) {
        return isStorageAccount(account) && !checkSDCardAvaliable(account.dataSet);
    }

    private long[] mCheckedContactIds;

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public AccountWithDataSet getSrcAccount() {
        return mCheckedAccount1;
    }

    @Override
    public AccountWithDataSet getDstAccount() {
        return mCheckedAccount2;
    }

    @Override
    public long[] getCheckedContactIds() {
        if (mCheckedContactIds != null) {
            return mCheckedContactIds;
        } else {
            return new long[0];
        }
    }

    @Override
    public String getCallingActivityName() {
        return mCallingActivityName;
    }

    public void resetStep() {
        setShowingStep(SELECTION_VIEW_STEP_ONE);
        mKey = null;
        setCheckedAccount(mKey);
        updateUi();
    }
}