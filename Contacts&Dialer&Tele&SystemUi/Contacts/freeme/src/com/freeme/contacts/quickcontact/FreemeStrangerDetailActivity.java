package com.freeme.contacts.quickcontact;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Trace;
import android.provider.ContactsContract;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.contacts.CallUtil;
import com.android.contacts.R;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.interactions.CallLogInteractionsLoader;
import com.android.contacts.interactions.ContactInteraction;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.logging.Logger;
import com.android.contacts.logging.QuickContactEvent;
import com.android.contacts.quickcontact.ExpandingEntryCardView;
import com.android.contacts.widget.MultiShrinkScroller;
import com.freeme.actionbar.app.FreemeActionBarUtil;
import com.freeme.contacts.common.utils.FreemeIntentUtils;
import com.freeme.phone.common.accessibility.FreemeCallAccessibility;
import com.mediatek.contacts.util.Log;
import com.freeme.contacts.common.utils.FreemeBlockUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FreemeStrangerDetailActivity extends ContactsActivity {
    private static final String TAG = "FreemeStrangerDetailActivity";
    private final static int REQUEST_CODE = 1;
    private static final int NO_PHONE_TYPE = -1;

    private ImageView mStrangerIcon;
    private TextView mStrangerAdress;
    private TextView mStrangerNumberText;
    private RelativeLayout mStrangerView;
    private FreemeSettingEntryCardView mFreemeStrangerSettingsCard;
    private ExpandingEntryCardView mRecentCard;
    private MultiShrinkScroller mScroller;
    private ArrayList<String> mNumberList = new ArrayList<>();
    private AsyncTask<Void, Void, Void> mRecentDataTask;
    private String mStrangerNumber;
    private String mStrangerLocation;
    private String mStrangerTitle;
    private boolean mIsBlocked;

    private Bundle phonesExtraBundle;
    private final static int LOADER_CALL_LOG_ID = 3;
    private static final String KEY_LOADER_EXTRA_PHONES =
            FreemeStrangerDetailActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_PHONES";
    private static final int[] mRecentLoaderIds = new int[]{
            LOADER_CALL_LOG_ID};
    private Map<Integer, List<ContactInteraction>> mRecentLoaderResults =
            new ConcurrentHashMap<>(4, 0.9f, 1);
    private static final int MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN = 5;
    private String mReferrer;
    private int mContactType;
    private boolean mShouldLog;
    private int mCallType;
    private boolean mIsVoicemailNumber;
    private static final int CALL_TYPE_ALL = -1;
    private static final String CALL_TYPE= "call_type";
    private static final String STRANGER_NUMBER= "stranger_number";
    private static final String STRANGER_LOCATION= "stranger_location";
    private static final String STRANGER_TITLE = "KEY_ACTIVITY_TITLE";
    private static final String DETAIL_CALL_IS_VOICEMAIL = "detail_call_is_voicemail";

    final View.OnClickListener mEntryClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FreemeIntentUtils.startViewCallLog(mNumberList, FreemeStrangerDetailActivity.this, mCallType);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }

        setContentView(R.layout.freeme_stranger_detail_activity);

        Intent intent = getIntent();
        mStrangerNumber = intent.getStringExtra(STRANGER_NUMBER);
        mStrangerLocation = intent.getStringExtra(STRANGER_LOCATION);
        mStrangerTitle = intent.getStringExtra(STRANGER_TITLE);
        mNumberList.add(mStrangerNumber);
        mCallType = intent.getIntExtra(CALL_TYPE, CALL_TYPE_ALL);
        mIsVoicemailNumber = intent.getBooleanExtra(DETAIL_CALL_IS_VOICEMAIL, false);

        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            if (!TextUtils.isEmpty(mStrangerTitle)) {
                actionbar.setTitle(mStrangerTitle);
            }
            FreemeActionBarUtil.setBackTitle(actionbar, getSubTitle(intent));
        }

        initHeaderViews();
        initFreemeSettings();
        if (mIsVoicemailNumber) {
            LetterTileDrawable drawable = new LetterTileDrawable(getResources());
            drawable.setContactType(LetterTileDrawable.TYPE_VOICEMAIL);
            drawable.setIsCircular(true);
            mStrangerIcon.setImageDrawable(drawable);
            mStrangerNumberText.setText(R.string.freeme_type_voicemail);
            mStrangerAdress.setText(mStrangerNumber);
        }
        mRecentCard = findViewById(R.id.recent_card);
        mRecentCard.setOnClickListener(mEntryClickHandler);
        mRecentCard.setTitle(getResources().getString(R.string.recent_card_title));
        mShouldLog = true;
        mReferrer = getCallingPackage();
        if (mReferrer == null && CompatUtils.isLollipopMr1Compatible() && getReferrer() != null) {
            mReferrer = getReferrer().getAuthority();
        }
        mContactType = QuickContactEvent.ContactType.UNKNOWN_TYPE;
        startLoaderData();
        initAccessibility();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getLoaderManager().restartLoader(
                LOADER_CALL_LOG_ID,
                phonesExtraBundle,
                mLoaderInteractionsCallbacks);
    }

    private void startLoaderData() {
        phonesExtraBundle = new Bundle();
        String[] phoneNumbers = null;
        phoneNumbers = new String[mNumberList.size()];
        for (int i = 0; i < mNumberList.size(); ++i) {
            phoneNumbers[i] = mStrangerNumber;
        }
        phonesExtraBundle.putStringArray(KEY_LOADER_EXTRA_PHONES, phoneNumbers);
        getLoaderManager().initLoader(
                LOADER_CALL_LOG_ID,
                phonesExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();
    }

    private final LoaderManager.LoaderCallbacks<List<ContactInteraction>> mLoaderInteractionsCallbacks =
            new LoaderManager.LoaderCallbacks<List<ContactInteraction>>() {

                @Override
                public Loader<List<ContactInteraction>> onCreateLoader(int id, Bundle args) {
                    Loader<List<ContactInteraction>> loader = null;
                    switch (id) {
                        case LOADER_CALL_LOG_ID:
                            loader = new CallLogInteractionsLoader(
                                    FreemeStrangerDetailActivity.this,
                                    args.getStringArray(KEY_LOADER_EXTRA_PHONES),
                                    null,
                                    MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN, mCallType);
                    }
                    return loader;
                }

                @Override
                public void onLoadFinished(Loader<List<ContactInteraction>> loader,
                                           List<ContactInteraction> data) {
                    mRecentLoaderResults.put(loader.getId(), data);

                    if (isAllRecentDataLoaded()) {
                        Log.d(TAG, "all recent data loaded");
                        bindRecentData();
                    }
                }

                @Override
                public void onLoaderReset(Loader<List<ContactInteraction>> loader) {
                    mRecentLoaderResults.remove(loader.getId());
                }
            };

    private boolean isAllRecentDataLoaded() {
        return mRecentLoaderResults.size() == mRecentLoaderIds.length;
    }

    private void bindRecentData() {
        final List<ContactInteraction> allInteractions = new ArrayList<>();
        final List<List<ExpandingEntryCardView.Entry>> interactionsWrapper = new ArrayList<>();

        // Serialize mRecentLoaderResults into a single list. This should be done on the main
        // thread to avoid races against mRecentLoaderResults edits.
        for (List<ContactInteraction> loaderInteractions : mRecentLoaderResults.values()) {
            allInteractions.addAll(loaderInteractions);
        }

        mRecentDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.d(TAG, "[bindRecentData] doInBackground(). " + mRecentDataTask);
                Trace.beginSection("sort recent loader results");

                // Sort the interactions by most recent
                Collections.sort(allInteractions, new Comparator<ContactInteraction>() {
                    @Override
                    public int compare(ContactInteraction a, ContactInteraction b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return 1;
                        }
                        if (b == null) {
                            return -1;
                        }
                        if (a.getInteractionDate() > b.getInteractionDate()) {
                            return -1;
                        }
                        if (a.getInteractionDate() == b.getInteractionDate()) {
                            return 0;
                        }
                        return 1;
                    }
                });

                Trace.endSection();
                Trace.beginSection("contactInteractionsToEntries");

                // Wrap each interaction in its own list so that an icon is displayed for each entry
                for (ExpandingEntryCardView.Entry contactInteraction : contactInteractionsToEntries(allInteractions)) {
                    List<ExpandingEntryCardView.Entry> entryListWrapper = new ArrayList<>(1);
                    entryListWrapper.add(contactInteraction);
                    interactionsWrapper.add(entryListWrapper);
                }

                Trace.endSection();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Log.d(TAG, "[bindRecentData] onPostExecute(). " + mRecentDataTask
                        + ", allInteractions.size() = " + allInteractions.size());
                Trace.beginSection("initialize recents card");

                if (allInteractions.size() > 0) {
                    mRecentCard.recentInitialize(interactionsWrapper,
                    /* numInitialVisibleEntries = */ allInteractions.size(),
                    /* isExpanded = */ mRecentCard.isExpanded(), /* isAlwaysExpanded = */ false,
                            null, mScroller, mCallType);
                    if (mRecentCard.getVisibility() == View.GONE && mShouldLog) {
                        Logger.logQuickContactEvent(mReferrer, mContactType, QuickContactEvent.CardType.RECENT,
                                QuickContactEvent.ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
                    }
                    mRecentCard.setVisibility(View.VISIBLE);
                } else {
                    mRecentCard.setVisibility(View.GONE);
                }
                mStrangerView.setVisibility(View.VISIBLE);
                if (!mIsVoicemailNumber) {
                    mFreemeStrangerSettingsCard.setVisibility(View.VISIBLE);
                }
                Trace.endSection();
                mRecentDataTask = null;
            }
        };

        Log.d(TAG, "[bindRecentData] mRecentDataTask.execute(). " + mRecentDataTask);
        mRecentDataTask.execute();
    }

    private List<ExpandingEntryCardView.Entry> contactInteractionsToEntries(List<ContactInteraction> interactions) {
        final List<ExpandingEntryCardView.Entry> entries = new ArrayList<>();
        for (ContactInteraction interaction : interactions) {
            if (interaction == null) {
                continue;
            }
            entries.add(new ExpandingEntryCardView.Entry(/* id = */ -1,
                    /* mainIcon */ null,
                    interaction.getViewCallType(this),
                    interaction.getMissed(this),
                    /* subHeaderIcon */ null,
                    interaction.getViewFooter(this),
                    /* textIcon */ null,
                    /* M: [SIM IND] add sim indicator @{ */
                    interaction.getSimIcon(this),
                    interaction.getSimName(this),
                    /* @} */
                    interaction.getContentDescription(this),
                    /*intent*/ null,
                    /* alternateIcon = */ null,
                    /* alternateIntent = */ null,
                    /* alternateContentDescription = */ null,
                    /* shouldApplyColor = */ true,
                    /* isEditable = */ false,
                    /* EntryContextMenuInfo = */ null,
                    /* thirdIcon = */ null,
                    /* thirdIntent = */ null,
                    /* thirdContentDescription = */ null,
                    /* thirdAction = */ ExpandingEntryCardView.Entry.ACTION_NONE,
                    /* thirdActionExtras = */ null,
                    /* iconResourceId*/ 0));
        }
        return entries;
    }

    private void initHeaderViews() {
        mStrangerView = findViewById(R.id.stranger_header_view);
        mStrangerIcon = findViewById(R.id.stranger_header_icon);
        mStrangerNumberText = findViewById(R.id.stranger_number);
        mStrangerAdress = findViewById(R.id.stranger_address);

        mStrangerNumberText.setText(mStrangerNumber);
        mStrangerAdress.setText(mStrangerLocation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (data != null && data.getBooleanExtra("save", false)) {
                finish();
            } else {
                //do nothing
            }
        }
        if (requestCode == 2) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private static void populateContactIntent(
            Intent intent, CharSequence name, CharSequence phoneNumber, int phoneNumberType) {
        if (phoneNumber != null) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
        }
        if (name != null) {
            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
        }
        if (phoneNumberType != NO_PHONE_TYPE) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, phoneNumberType);
        }
    }

    public static Intent getNewContactIntent() {
        return new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
    }

    public static Intent getAddToExistingContactIntent() {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        return intent;
    }

    public static Intent getNewContactIntent(
            CharSequence name, CharSequence phoneNumber, int phoneNumberType) {
        Intent intent = getNewContactIntent();
        populateContactIntent(intent, name, phoneNumber, phoneNumberType);
        return intent;
    }

    public static Intent getAddToExistingContactIntent(
            CharSequence name, CharSequence phoneNumber, int phoneNumberType) {
        Intent intent = getAddToExistingContactIntent();
        populateContactIntent(intent, name, phoneNumber, phoneNumberType);
        return intent;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFreemeStrangerSettingsCard.displayOrHideEntryView(CARD_ENTRY_VIDEO_CALL);
        startAccessibility();
    }

    private static final int CARD_ENTRY_CALL = -2;
    private static final int CARD_ENTRY_SEND_MESSAGE = -3;
    private static final int CARD_ENTRY_NEW_CONTACT = -4;
    private static final int CARD_ENTRY_EXIST_CONTACT = -5;
    private static final int CARD_ENTRY_BLOCK = -6;
    private static final int CARD_ENTRY_MARK = -7;
    private static final int CARD_ENTRY_VIDEO_CALL = -8;

    private void initFreemeSettings() {
        mFreemeStrangerSettingsCard = (FreemeSettingEntryCardView) findViewById(R.id.freeme_stranger_settings);
        final LinkedHashMap<Integer, FreemeSettingEntryCardView.SettingsEntry> strangerSettingsEntries = new LinkedHashMap<>();

        //call
        final FreemeSettingEntryCardView.SettingsEntry callEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_CALL, getString(R.string.freeme_stranger_call), null, mCallListener);
        strangerSettingsEntries.put(CARD_ENTRY_CALL,callEntry);

        //send message
        final FreemeSettingEntryCardView.SettingsEntry sendMessageEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_SEND_MESSAGE, getString(R.string.freeme_stranger_send_message), null, mSendMessageListener);
        strangerSettingsEntries.put(CARD_ENTRY_SEND_MESSAGE,sendMessageEntry);

        //video call
        final FreemeSettingEntryCardView.SettingsEntry videoCallEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_VIDEO_CALL, getString(R.string.freeme_stranger_video_call), null, mVideoCallListener);
        strangerSettingsEntries.put(CARD_ENTRY_VIDEO_CALL,videoCallEntry);

        //new cotnact
        final FreemeSettingEntryCardView.SettingsEntry createCotnactEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_NEW_CONTACT, getString(R.string.freeme_stranger_create_contact), null, mCreateContactListener);
        strangerSettingsEntries.put(CARD_ENTRY_NEW_CONTACT,createCotnactEntry);

        //add to already exist cotnact
        final FreemeSettingEntryCardView.SettingsEntry addToExistContactEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_EXIST_CONTACT, getString(R.string.freeme_stranger_add_to_exist_contact), null, mAddToExistContactListener);
        strangerSettingsEntries.put(CARD_ENTRY_EXIST_CONTACT,addToExistContactEntry);

        //block
        mIsBlocked = FreemeBlockUtil.isBlockedNumber(this, mStrangerNumber);
        final FreemeSettingEntryCardView.SettingsEntry blockedThisNumberEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_BLOCK, getString(mIsBlocked ? R.string.remove_from_blacklsit : R.string.add_to_blacklsit), null, mBlockedListener);
        strangerSettingsEntries.put(CARD_ENTRY_BLOCK,blockedThisNumberEntry);

        /*/ mark number
        final FreemeSettingEntryCardView.SettingsEntry markNumberEntry = new FreemeSettingEntryCardView.SettingsEntry(
                CARD_ENTRY_MARK, getString(R.string.freeme_stranger_mark_number), null, mMarkNumberListener);
        strangerSettingsEntries.add(markNumberEntry);
        //*/

        if (!mIsVoicemailNumber && strangerSettingsEntries.size() > 0) {
            mFreemeStrangerSettingsCard.initialize(strangerSettingsEntries);
        } else {
            mFreemeStrangerSettingsCard.setVisibility(View.GONE);
        }
    }

    Intent callIntent;
    private View.OnClickListener mCallListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            callIntent = CallUtil.getCallIntent(mStrangerNumber);
            callIntent.putExtra(EXTRA_ACTION_TYPE, QuickContactEvent.ActionType.CALL);
            startActivity(callIntent);
        }
    };

    Intent sendIntent;
    public static final String EXTRA_ACTION_TYPE = "action_type";
    private View.OnClickListener mSendMessageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendIntent = new Intent(Intent.ACTION_SENDTO,
                    Uri.fromParts(ContactsUtils.SCHEME_SMSTO, mStrangerNumber, null));
            sendIntent.putExtra(EXTRA_ACTION_TYPE, QuickContactEvent.ActionType.SMS);
            startActivity(sendIntent);
        }
    };

    private static final String CALL_ORIGIN_STRANGER_ACTIVITY =
            "com.freeme.contacts.quickcontact.FreemeStrangerDetailActivity";
    private View.OnClickListener mVideoCallListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = CallUtil.getVideoCallIntent(mStrangerNumber,
                    CALL_ORIGIN_STRANGER_ACTIVITY);
            intent.putExtra(EXTRA_ACTION_TYPE, QuickContactEvent.ActionType.VIDEOCALL);
            startActivity(intent);
        }
    };

    private View.OnClickListener mCreateContactListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            {
                Intent intent = getNewContactIntent(
                        null /* name */,
                        mStrangerNumber,
                        NO_PHONE_TYPE);
                try {
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException e) {

                }
            }
        }
    };

    private View.OnClickListener mAddToExistContactListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = getAddToExistingContactIntent(
                    null /* name */,
                    mStrangerNumber,
                    NO_PHONE_TYPE);
            try {
                startActivityForResult(intent, 2);
            } catch (ActivityNotFoundException e) {

            }
        }
    };

    private View.OnClickListener mBlockedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showBlackListOperateTipsDialog();
        }
    };

    private View.OnClickListener mMarkNumberListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private void showBlackListOperateTipsDialog() {
        mIsBlocked = FreemeBlockUtil.isBlockedNumber(FreemeStrangerDetailActivity.this, mStrangerNumber);
        AlertDialog.Builder build = new AlertDialog.Builder(FreemeStrangerDetailActivity.this);
        build.setNegativeButton(android.R.string.cancel, null);
        build.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int position) {
                if (mIsBlocked) {
                    FreemeBlockUtil.unBlockNumber(FreemeStrangerDetailActivity.this, mStrangerNumber);
                    mIsBlocked = false;
                } else {
                    FreemeBlockUtil.blockNumber(FreemeStrangerDetailActivity.this, mStrangerNumber, null);
                    mIsBlocked = true;
                }
                mFreemeStrangerSettingsCard.updateEntryView(CARD_ENTRY_BLOCK,
                        getString(mIsBlocked ? R.string.remove_from_blacklsit : R.string.add_to_blacklsit),
                        null);
            }
        });
        int max = Math.min(mNumberList.size(), 3);
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < max; i++) {
            buffer.append(mNumberList.get(i));
            if (i != max - 1) {
                buffer.append("、");
            }
        }
        String number;
        if (mNumberList.size() > max) {
            number = getString(R.string.black_list_msg_more, buffer.toString());
        } else {
            number = buffer.toString();
        }

        if (mIsBlocked) {
            build.setTitle(R.string.black_list_remove_title);
            build.setMessage(getString(R.string.black_list_remove_tips, number));
        } else {
            build.setTitle(R.string.black_list_add_title);
            build.setMessage(getString(R.string.black_list_add_tips, number));
        }
        build.show();
    }

    private String getSubTitle(Intent intent) {
        String title = null;
        if (intent != null) {
            title = intent.getStringExtra(FreemeActionBarUtil.EXTRA_NAVIGATE_UP_TITLE_TEXT);
        }
        if (TextUtils.isEmpty(title)) {
            title = getString(R.string.freeme_navigate_title_from_phone);
        }

        return title;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAccessibility();
    }

    private FreemeCallAccessibility mSmartDialAccessibility;

    private void initAccessibility() {
        mSmartDialAccessibility = new FreemeCallAccessibility(this,
                FreemeCallAccessibility.TYPE_SMART_DIAL,
                new FreemeCallAccessibility.IFreemeSmartAction() {
                    @Override
                    public void smartAnswer() {
                    }

                    @Override
                    public void smartDial() {
                        FreemeStrangerDetailActivity.this.smartDial();
                    }
                });
    }

    private void startAccessibility() {
        if (mSmartDialAccessibility != null) {
            mSmartDialAccessibility.start();
        }
    }

    private void stopAccessibility() {
        if (mSmartDialAccessibility != null) {
            mSmartDialAccessibility.stop();
        }
    }

    private void smartDial() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
            if (TextUtils.isEmpty(mStrangerNumber)) {
                return;
            }
            if (mSmartDialAccessibility != null) {
                mSmartDialAccessibility.vibrator();
            }
            Intent intent = CallUtil.getCallIntent(mStrangerNumber);
            intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,
                    getDefaultSmartDialAccount());
            startActivity(intent);
        }
    }

    private PhoneAccountHandle getDefaultSmartDialAccount() {
        PhoneAccountHandle defaultPhoneAccountHandle = null;
        final TelecomManager telecomManager = TelecomManager.from(this);
        final List<PhoneAccountHandle> phoneAccountsList = telecomManager.getCallCapablePhoneAccounts();
        if (phoneAccountsList.size() == 1) {
            defaultPhoneAccountHandle = phoneAccountsList.get(0);
        } else if (phoneAccountsList.size() > 1) {
            defaultPhoneAccountHandle = telecomManager.getUserSelectedOutgoingPhoneAccount();
            if (defaultPhoneAccountHandle == null) {
                defaultPhoneAccountHandle = phoneAccountsList.get(0);
            }
        }
        return defaultPhoneAccountHandle;
    }
}
