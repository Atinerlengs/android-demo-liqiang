package com.freeme.dialer.app.dialpad;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.provider.CallLog;
import android.provider.Contacts;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.contacts.common.dialog.CallSubjectDialog;
import com.android.contacts.common.util.StopWatch;
import com.android.dialer.app.DialtactsActivity;
import com.android.dialer.app.R;
import com.android.dialer.app.SpecialCharSequenceMgr;
import com.android.dialer.app.calllog.CallLogAsync;
import com.android.dialer.app.dialpad.DialpadFragment;
import com.android.dialer.app.dialpad.PseudoEmergencyAnimator;
import com.android.dialer.app.dialpad.UnicodeDialerKeyListener;
import com.android.dialer.callintent.CallInitiationType;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.common.LogUtil;
import com.android.dialer.dialpadview.DialpadKeyButton;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.telecom.TelecomUtil;
import com.android.dialer.util.CallUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PermissionsUtil;
import com.android.internal.telephony.TelephonyIntents;
import com.freeme.dialer.app.FreemeDialtactsActivity;
import com.freeme.dialer.dialpadview.FreemeDialpadView;
import com.freeme.dialer.utils.FreemeDialerUtils;
import com.freeme.dialer.utils.FreemeDialerFeatureOptions;
import com.freeme.dialer.speeddial.FreemeSpeedDialController;

import com.mediatek.dialer.compat.DialerCompatEx;
import com.mediatek.dialer.ext.DialpadExtensionAction;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.util.DialerVolteUtils;
import com.mediatek.telephony.MtkTelephonyManagerEx;

import java.util.HashSet;
import java.util.List;

public class FreemeDialpadFragment extends Fragment
        implements View.OnClickListener,
        View.OnLongClickListener,
        View.OnKeyListener,
        AdapterView.OnItemClickListener,
        TextWatcher,
        PopupMenu.OnMenuItemClickListener,
        DialpadKeyButton.OnPressedListener,
        DialpadExtensionAction {
    private static final String TAG = "FreemeDialpadFragment";
    private static final boolean DEBUG = DialtactsActivity.DEBUG;
    private static final String EMPTY_NUMBER = "";
    private static final char PAUSE = ',';
    private static final char WAIT = ';';
    /**
     * The length of DTMF tones in milliseconds
     */
    private static final int TONE_LENGTH_MS = 150;

    private static final int TONE_LENGTH_INFINITE = -1;
    /**
     * The DTMF tone volume relative to other sounds in the stream
     */
    private static final int TONE_RELATIVE_VOLUME = 80;
    /**
     * Stream type used to play the DTMF tones off call, and mapped to the volume control keys
     */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;
    /**
     * Identifier for the "Add Call" intent extra.
     */
    private static final String ADD_CALL_MODE_KEY = "add_call_mode";
    /**
     * Identifier for intent extra for sending an empty Flash message for CDMA networks. This message
     * is used by the network to simulate a press/depress of the "hookswitch" of a landline phone. Aka
     * "empty flash".
     * <p>
     * <p>TODO: Using an intent extra to tell the phone to send this flash is a temporary measure. To
     * be replaced with an Telephony/TelecomManager call in the future. TODO: Keep in sync with the
     * string defined in OutgoingCallBroadcaster.java in Phone app until this is replaced with the
     * Telephony/Telecom API.
     */
    private static final String EXTRA_SEND_EMPTY_FLASH = "com.android.phone.extra.SEND_EMPTY_FLASH";

    private static final String PREF_DIGITS_FILLED_BY_INTENT = "pref_digits_filled_by_intent";
    private final Object mToneGeneratorLock = new Object();
    /**
     * Set of dialpad keys that are currently being pressed
     */
    private final HashSet<View> mPressedDialpadKeys = new HashSet<View>(12);
    // Last number dialed, retrieved asynchronously from the call DB
    // in onCreate. This number is displayed when the user hits the
    // send key and cleared in onPause.
    private final CallLogAsync mCallLog = new CallLogAsync();
    private DialpadFragment.OnDialpadQueryChangedListener mDialpadQueryListener;
    private FreemeDialpadView mDialpadView;
    private EditText mDigits;
    private int mDialpadSlideInDuration;
    /**
     * Remembers if we need to clear digits field when the screen is completely gone.
     */
    private boolean mClearDigitsOnStop;

    private View mDelete;
    private ToneGenerator mToneGenerator;
    private View mSpacer;
    private ListView mDialpadChooser;
    private DialpadChooserAdapter mDialpadChooserAdapter;
    private View mDialActionLayout;
    private ViewGroup mDigitsContainer;
    /**
     * Regular expression prohibiting manual phone call. Can be empty, which means "no rule".
     */
    private String mProhibitedPhoneNumberRegexp;

    private PseudoEmergencyAnimator mPseudoEmergencyAnimator;
    private String mLastNumberDialed = EMPTY_NUMBER;

    // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;
    private String mCurrentCountryIso;
    private CallStateReceiver mCallStateReceiver;
    private boolean mWasEmptyBeforeTextChange;
    /**
     * This field is set to true while processing an incoming DIAL intent, in order to make sure that
     * SpecialCharSequenceMgr actions can be triggered by user input but *not* by a tel: URI passed by
     * some other app. It will be set to false when all digits are cleared.
     */
    private boolean mDigitsFilledByIntent;

    private boolean mStartedFromNewIntent = false;
    private boolean mFirstLaunch = false;
    private boolean mAnimate = false;

    /// M: SOS implementation, to check for SOS support
    private boolean mIsSupportSOS = FreemeDialerFeatureOptions.isMtkSosQuickDialSupport();

    private boolean isLand;

    private final String KEY_RECORD_NUMBER = "KEY_RECORD_NUMBER";

    /**
     * Determines whether an add call operation is requested.
     *
     * @param intent The intent.
     * @return {@literal true} if add call operation was requested. {@literal false} otherwise.
     */
    public static boolean isAddCallMode(Intent intent) {
        if (intent == null) {
            return false;
        }
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            // see if we are "adding a call" from the InCallScreen; false by default.
            return intent.getBooleanExtra(ADD_CALL_MODE_KEY, false);
        } else {
            return false;
        }
    }

    /**
     * Format the provided string of digits into one that represents a properly formatted phone
     * number.
     *
     * @param dialString       String of characters to format
     * @param normalizedNumber the E164 format number whose country code is used if the given
     *                         phoneNumber doesn't have the country code.
     * @param countryIso       The country code representing the format to use if the provided normalized
     *                         number is null or invalid.
     * @return the provided string of digits as a formatted phone number, retaining any post-dial
     * portion of the string.
     */
    @VisibleForTesting
    static String getFormattedDigits(String dialString, String normalizedNumber, String countryIso) {
        String number = PhoneNumberUtils.extractNetworkPortion(dialString);
        // Also retrieve the post dial portion of the provided data, so that the entire dial
        // string can be reconstituted later.
        final String postDial = PhoneNumberUtils.extractPostDialPortion(dialString);

        if (TextUtils.isEmpty(number)) {
            return postDial;
        }

        number = PhoneNumberUtils.formatNumber(number, normalizedNumber, countryIso);

        if (TextUtils.isEmpty(postDial)) {
            return number;
        }

        return number.concat(postDial);
    }

    /**
     * Returns true of the newDigit parameter can be added at the current selection point, otherwise
     * returns false. Only prevents input of WAIT and PAUSE digits at an unsupported position. Fails
     * early if start == -1 or start is larger than end.
     */
    @VisibleForTesting
  /* package */ static boolean canAddDigit(CharSequence digits, int start, int end, char newDigit) {
        if (newDigit != WAIT && newDigit != PAUSE) {
            throw new IllegalArgumentException(
                    "Should not be called for anything other than PAUSE & WAIT");
        }

        // False if no selection, or selection is reversed (end < start)
        if (start == -1 || end < start) {
            return false;
        }

        // unsupported selection-out-of-bounds state
        if (start > digits.length() || end > digits.length()) {
            return false;
        }

        // Special digit cannot be the first digit
        if (start == 0) {
            return false;
        }

        if (newDigit == WAIT) {
            // preceding char is ';' (WAIT)
            if (digits.charAt(start - 1) == WAIT) {
                return false;
            }

            // next char is ';' (WAIT)
            if ((digits.length() > end) && (digits.charAt(end) == WAIT)) {
                return false;
            }
        }

        return true;
    }

    private TelephonyManager getTelephonyManager() {
        return (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mWasEmptyBeforeTextChange = TextUtils.isEmpty(s);
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
        boolean isEmpty = TextUtils.isEmpty(input);
        if (mWasEmptyBeforeTextChange != isEmpty) {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.invalidateOptionsMenu();
                updateMenuOverflowButton(mWasEmptyBeforeTextChange);
            }
        }
        if (!isLand && getActivity() != null && !getActivity().isDestroyed()
                && !getActivity().isInMultiWindowMode()) {
            boolean added = mDigitsContainer.getChildCount() > 0;
            if (!isEmpty && !added) {
                mDigitsContainer.addView(mDialpadView.getDigitsViews());
            } else if (isEmpty && added) {
                mDigitsContainer.removeAllViews();
            }
        }

        // DTMF Tones do not need to be played here any longer -
        // the DTMF dialer handles that functionality now.
    }

    @Override
    public void afterTextChanged(Editable input) {
        /// M: avoid NPE if this callback is called after activity finished @{
        if (getActivity() == null) {
            return;
        }
        /// @}
        // When DTMF dialpad buttons are being pressed, we delay SpecialCharSequenceMgr sequence,
        // since some of SpecialCharSequenceMgr's behavior is too abrupt for the "touch-down"
        // behavior.
        if (!mDigitsFilledByIntent
                && SpecialCharSequenceMgr.handleChars(getActivity(), input.toString(), mDigits)) {
            // A special sequence was entered, clear the digits
            mDigits.getText().clear();
        }

        if (isDigitsEmpty()) {
            mDigitsFilledByIntent = false;
            mDigits.setCursorVisible(false);
        }

        if (mDialpadQueryListener != null) {
            mDialpadQueryListener.onDialpadQueryChanged(mDigits.getText().toString());
        }

        showGeoLocation();

        updateDeleteButtonEnabledState();
    }

    @Override
    public void onCreate(Bundle state) {
        Trace.beginSection(TAG + " onCreate");
        super.onCreate(state);

        mFirstLaunch = state == null;

        isLand = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        mCurrentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());

        mProhibitedPhoneNumberRegexp =
                getResources().getString(R.string.config_prohibited_phone_number_regexp);

        if (state != null) {
            mDigitsFilledByIntent = state.getBoolean(PREF_DIGITS_FILLED_BY_INTENT);
        }

        mDialpadSlideInDuration = getResources().getInteger(R.integer.dialpad_slide_in_duration);

        if (mCallStateReceiver == null) {
            IntentFilter callStateIntentFilter =
                    new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            mCallStateReceiver = new CallStateReceiver();
            getActivity().registerReceiver(mCallStateReceiver, callStateIntentFilter);
        }

        /// M: for Plug-in @{
        ExtensionManager.getDialPadExtension().onCreate(
                getActivity().getApplicationContext(), this, this);
        /// @}

        Trace.endSection();
    }

    /**
     * M: for plug-in, init customer view.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Trace.beginSection(TAG + " onViewCreated init plugin");
        ExtensionManager.getDialPadExtension().onViewCreated(getActivity(), view);
        Trace.endSection();

        showDialButton();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        Trace.beginSection(TAG + " onCreateView");
        Trace.beginSection(TAG + " inflate view");
        final View fragmentView = inflater.inflate(R.layout.freeme_dialpad_fragment,
                container, false);
        Trace.endSection();
        Trace.beginSection(TAG + " buildLayer");
        fragmentView.buildLayer();
        Trace.endSection();

        Trace.beginSection(TAG + " setup views");

        ///M: WFC @{
        mContext = getActivity();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelecomManager.ACTION_PHONE_ACCOUNT_REGISTERED);
        filter.addAction(TelecomManager.ACTION_PHONE_ACCOUNT_UNREGISTERED);
        mContext.registerReceiver(mReceiver, filter);
        ///@}

        /// M: for plug-in @{
        Trace.beginSection(TAG + " init plugin view");
        ExtensionManager.getDialPadExtension().onCreateView(inflater, container,
                savedState, fragmentView);
        Trace.endSection();
        /// @}

        boolean isInMultiWindowMode = getActivity().isInMultiWindowMode();
        mDigitsContainer = fragmentView.findViewById(R.id.digits_container);
        mDialpadView = fragmentView.findViewById(R.id.dialpad_view);
        mDialpadView.setCanDigitsBeEdited(true);
        mDigits = mDialpadView.getDigits();
        mDigits.setKeyListener(UnicodeDialerKeyListener.INSTANCE);
        mDigits.setOnClickListener(this);
        mDigits.setOnKeyListener(this);
        mDigits.setOnLongClickListener(this);
        mDigits.addTextChangedListener(this);
        mDigits.setElegantTextHeight(false);

        PhoneNumberFormattingTextWatcher watcher = new PhoneNumberFormattingTextWatcher(
                GeoUtil.getCurrentCountryIso(getActivity()));
        mDigits.addTextChangedListener(watcher);

        // Check for the presence of the keypad
        View oneButton = fragmentView.findViewById(R.id.one);
        if (oneButton != null) {
            configureKeypadListeners(fragmentView);
        }

        mDelete = mDialpadView.getDeleteButton();

        if (mDelete != null) {
            mDelete.setOnClickListener(this);
            mDelete.setOnLongClickListener(this);
        }

        mSpacer = fragmentView.findViewById(R.id.spacer);
        mSpacer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isDigitsEmpty()) {
                    if (getActivity() != null) {
                        return ((DialpadFragment.HostInterface) getActivity())
                                .onDialpadSpacerTouchWithEmptyQuery();
                    }
                    return true;
                }
                return false;
            }
        });

        if (isLand) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSpacer.getLayoutParams();
            if (isInMultiWindowMode) {
                params.weight = 1;
            } else {
                params.weight = 2;
            }
            mSpacer.setLayoutParams(params);
        }

        if (isLand || isInMultiWindowMode) {
            mDigitsContainer.addView(mDialpadView.getDigitsViews());
        } else if (mDigits.getText().length() == 0) {
            mDigitsContainer.removeAllViews();
        }
        mDigits.setText(mSearchQuery);
        mDigits.setCursorVisible(false);

        // Set up the "dialpad chooser" UI; see showDialpadChooser().
        mDialpadChooser = fragmentView.findViewById(R.id.dialpadChooser);
        mDialpadChooser.setOnItemClickListener(this);

        mDialActionLayout = fragmentView.findViewById(R.id.dial_action_layout);

        /// M: Fix CR ALPS01863413. Update text field view for ADN query.
        SpecialCharSequenceMgr.updateTextFieldView(mDigits);

        registerReceiverFreeme();

        Trace.endSection();
        Trace.endSection();
        return fragmentView;
    }

    private boolean isLayoutReady() {
        return mDigits != null;
    }

    @VisibleForTesting
    public EditText getDigitsWidget() {
        return mDigits;
    }

    /**
     * @return true when {@link #mDigits} is actually filled by the Intent.
     */
    private boolean fillDigitsIfNecessary(Intent intent) {
        // Only fills digits from an intent if it is a new intent.
        // Otherwise falls back to the previously used number.
        if (!mFirstLaunch && !mStartedFromNewIntent) {
            return false;
        }

        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                if (PhoneAccount.SCHEME_TEL.equals(uri.getScheme())) {
                    // Put the requested number into the input area
                    String data = uri.getSchemeSpecificPart();
                    // Remember it is filled via Intent.
                    mDigitsFilledByIntent = true;
                    final String converted =
                            PhoneNumberUtils.convertKeypadLettersToDigits(
                                    PhoneNumberUtils.replaceUnicodeDigits(data));
                    setFormattedDigits(converted, null);
                    // clear phone number, it from incall record
                    if (intent.hasExtra(KEY_RECORD_NUMBER)) {
                        intent.setData(null);
                    }
                    return true;
                } else {
                    if (!PermissionsUtil.hasContactsReadPermissions(getActivity())) {
                        return false;
                    }
                    String type = intent.getType();
                    if (Contacts.People.CONTENT_ITEM_TYPE.equals(type)
                            || Contacts.Phones.CONTENT_ITEM_TYPE.equals(type)) {
                        // Query the phone number
                        Cursor c = getActivity().getContentResolver()
                                .query(intent.getData(),
                                        new String[]{Contacts.PhonesColumns.NUMBER, Contacts.PhonesColumns.NUMBER_KEY},
                                        null,
                                        null,
                                        null);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    // Remember it is filled via Intent.
                                    mDigitsFilledByIntent = true;
                                    // Put the number into the input area
                                    setFormattedDigits(c.getString(0), c.getString(1));
                                    return true;
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks the given Intent and changes dialpad's UI state. For example, if the Intent requires the
     * screen to enter "Add Call" mode, this method will show correct UI for the mode.
     */
    private void configureScreenFromIntent(Activity parent) {
        // If we were not invoked with a DIAL intent,
        if (!(parent instanceof FreemeDialtactsActivity)) {
            setStartedFromNewIntent(false);
            return;
        }
        // See if we were invoked with a DIAL intent. If we were, fill in the appropriate
        // digits in the dialer field.
        Intent intent = parent.getIntent();

        if (!isLayoutReady()) {
            // This happens typically when parent's Activity#onNewIntent() is called while
            // Fragment#onCreateView() isn't called yet, and thus we cannot configure Views at
            // this point. onViewCreate() should call this method after preparing layouts, so
            // just ignore this call now.
            LogUtil.i(
                    "FreemeDialpadFragment.configureScreenFromIntent",
                    "Screen configuration is requested before onCreateView() is called. Ignored");
            return;
        }

        boolean needToShowDialpadChooser = false;

        // Be sure *not* to show the dialpad chooser if this is an
        // explicit "Add call" action, though.
        final boolean isAddCallMode = isAddCallMode(intent);
        if (!isAddCallMode) {

            // Don't show the chooser when called via onNewIntent() and phone number is present.
            // i.e. User clicks a telephone link from gmail for example.
            // In this case, we want to show the dialpad with the phone number.
            final boolean digitsFilled = fillDigitsIfNecessary(intent);
            if (!(mStartedFromNewIntent && digitsFilled)) {

                final String action = intent.getAction();
                if (Intent.ACTION_DIAL.equals(action)
                        || Intent.ACTION_VIEW.equals(action)
                        || Intent.ACTION_MAIN.equals(action)) {
                    // If there's already an active call, bring up an intermediate UI to
                    // make the user confirm what they really want to do.
                    if (isPhoneInUse()) {
                        /*
                        needToShowDialpadChooser = true;
                        */
                        needToShowDialpadChooser = false; // hide dialpad chooser
                    }
                }
            }
        }
        showDialpadChooser(needToShowDialpadChooser);
        setStartedFromNewIntent(false);
    }

    public void setStartedFromNewIntent(boolean value) {
        mStartedFromNewIntent = value;
    }

    public void clearCallRateInformation() {
        setCallRateInformation(null, null);
    }

    public void setCallRateInformation(String countryName, String displayRate) {
        mDialpadView.setCallRateInformation(countryName, displayRate);
    }

    /**
     * Sets formatted digits to digits field.
     */
    private void setFormattedDigits(String data, String normalizedNumber) {
        final String formatted = getFormattedDigits(data, normalizedNumber, mCurrentCountryIso);
        if (!TextUtils.isEmpty(formatted)) {
            Editable digits = mDigits.getText();
            digits.replace(0, digits.length(), formatted);
            // for some reason this isn't getting called in the digits.replace call above..
            // but in any case, this will make sure the background drawable looks right
            afterTextChanged(digits);
        }
    }

    private void configureKeypadListeners(View fragmentView) {
        final int[] buttonIds =
                new int[]{
                        R.id.one,
                        R.id.two,
                        R.id.three,
                        R.id.four,
                        R.id.five,
                        R.id.six,
                        R.id.seven,
                        R.id.eight,
                        R.id.nine,
                        R.id.star,
                        R.id.zero,
                        R.id.pound
                };

        DialpadKeyButton dialpadKey;

        for (int i = 0; i < buttonIds.length; i++) {
            dialpadKey = fragmentView.findViewById(buttonIds[i]);
            dialpadKey.setOnPressedListener(this);
            dialpadKey.setOnLongClickListener(this);
        }

        // Long-pressing one button will initiate Voicemail.
        final DialpadKeyButton one = fragmentView.findViewById(R.id.one);
        one.setOnLongClickListener(this);

        // Long-pressing zero button will enter '+' instead.
        final DialpadKeyButton zero = fragmentView.findViewById(R.id.zero);
        zero.setOnLongClickListener(this);

        // Long-pressing star button will enter ',' instead.
        final DialpadKeyButton star = fragmentView.findViewById(R.id.star);
        star.setOnLongClickListener(this);

        // Long-pressing pound button will enter ';' instead.
        final DialpadKeyButton pound = fragmentView.findViewById(R.id.pound);
        pound.setOnLongClickListener(this);

        /// M: SOS Implementation, Long-pressing nine button will dial ECC @{
        LogUtil.d(TAG, "SOS quick dial support support:" + mIsSupportSOS);
        if (FreemeDialerFeatureOptions.isMtkSosQuickDialSupport()) {
            final DialpadKeyButton nine = fragmentView.findViewById(R.id.nine);
            nine.setOnLongClickListener(this);
        }
        /// @}
    }

    @Override
    public void onStart() {
        LogUtil.d("FreemeDialpadFragment.onStart", "first launch: %b", mFirstLaunch);
        Trace.beginSection(TAG + " onStart");
        super.onStart();
        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        final long start = System.currentTimeMillis();
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                } catch (RuntimeException e) {
                    LogUtil.e(
                            "FreemeDialpadFragment.onStart",
                            "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }
        final long total = System.currentTimeMillis() - start;
        if (total > 50) {
            LogUtil.i("FreemeDialpadFragment.onStart", "Time for ToneGenerator creation: " + total);
        }
        Trace.endSection();
    }

    @Override
    public void onResume() {
        LogUtil.d("FreemeDialpadFragment.onResume", "");
        Trace.beginSection(TAG + " onResume");
        super.onResume();

        /// M: [VoLTE ConfCall] initialize value about conference call capability.
        mVolteConfCallEnabled = supportOneKeyConference(getActivity());
        LogUtil.d(TAG, "onResume mVolteConfCallEnabled = " + mVolteConfCallEnabled);

        final FreemeDialtactsActivity activity = (FreemeDialtactsActivity) getActivity();
        mDialpadQueryListener = activity;

        final StopWatch stopWatch = StopWatch.start("Dialpad.onResume");

        // Query the last dialed number. Do it first because hitting
        // the DB is 'slow'. This call is asynchronous.
        queryLastOutgoingCall();

        stopWatch.lap("qloc");

        final ContentResolver contentResolver = activity.getContentResolver();

        /// M: [ALPS01858019] add listener to observer CallLog changes
        if (PermissionsUtil.hasPhonePermissions(getContext())) {
            contentResolver.registerContentObserver(CallLog.CONTENT_URI, true, mCallLogObserver);
        } else {
            LogUtil.w(TAG, "can not register CallLog observer without permission.");
        }

        // retrieve the DTMF tone play back setting.
        mDTMFToneEnabled =
                Settings.System.getInt(contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        stopWatch.lap("dtwd");

        stopWatch.lap("hptc");

        mPressedDialpadKeys.clear();

        configureScreenFromIntent(getActivity());

        stopWatch.lap("fdin");

        if (!isPhoneInUse()) {
            // A sanity-check: the "dialpad chooser" UI should not be visible if the phone is idle.
            showDialpadChooser(false);
        }

        ///M: WFC @{
        updateWfcUI();
        ///@}
        stopWatch.lap("hnt");

        updateDeleteButtonEnabledState();

        stopWatch.lap("bes");

        stopWatch.stopAndLog(TAG, 50);

        if (mFirstLaunch) {
            // The onHiddenChanged callback does not get called the first time the fragment is
            // attached, so call it ourselves here.
            onHiddenChanged(false);
        }

        showDialButton();

        mFirstLaunch = false;
        Trace.endSection();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Make sure we don't leave this activity with a tone still playing.
        stopTone();
        mPressedDialpadKeys.clear();

        // TODO: I wonder if we should not check if the AsyncTask that
        // lookup the last dialed number has completed.
        mLastNumberDialed = EMPTY_NUMBER; // Since we are going to query again, free stale number.

        SpecialCharSequenceMgr.cleanup();
        /// M: [ALPS01858019] add unregister the call log observer.
        getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
    }

    @Override
    public void onStop() {
        super.onStop();

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }

        if (mClearDigitsOnStop) {
            mClearDigitsOnStop = false;
            clearDialpad();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PREF_DIGITS_FILLED_BY_INTENT, mDigitsFilledByIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPseudoEmergencyAnimator != null) {
            mPseudoEmergencyAnimator.destroy();
            mPseudoEmergencyAnimator = null;
        }
        getActivity().unregisterReceiver(mCallStateReceiver);
        /// M: for plug-in. @{
        ExtensionManager.getDialPadExtension().onDestroy();
        /// @}
    }

    private void keyPressed(int keyCode) {
        if (getView() == null || getView().getTranslationY() != 0) {
            return;
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
                playTone(ToneGenerator.TONE_DTMF_1, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_2:
                playTone(ToneGenerator.TONE_DTMF_2, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_3:
                playTone(ToneGenerator.TONE_DTMF_3, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_4:
                playTone(ToneGenerator.TONE_DTMF_4, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_5:
                playTone(ToneGenerator.TONE_DTMF_5, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_6:
                playTone(ToneGenerator.TONE_DTMF_6, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_7:
                playTone(ToneGenerator.TONE_DTMF_7, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_8:
                playTone(ToneGenerator.TONE_DTMF_8, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_9:
                playTone(ToneGenerator.TONE_DTMF_9, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_0:
                playTone(ToneGenerator.TONE_DTMF_0, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_POUND:
                playTone(ToneGenerator.TONE_DTMF_P, TONE_LENGTH_INFINITE);
                break;
            case KeyEvent.KEYCODE_STAR:
                playTone(ToneGenerator.TONE_DTMF_S, TONE_LENGTH_INFINITE);
                break;
            default:
                break;
        }

        getView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        mDigits.onKeyDown(keyCode, event);

        // if mDigits is no focus, the onClick method cannot be executed to display the cursor
        if (!mDigits.isFocused()) {
            mDigits.requestFocus();
        }

        // If the cursor is at the end of the text we hide it.
        final int length = mDigits.length();
        if (length == mDigits.getSelectionStart() && length == mDigits.getSelectionEnd()) {
            mDigits.setCursorVisible(false);
        }

        if (mPressedDialpadKeys.isEmpty()) {
            stopTone();
        }
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
        if (view.getId() == R.id.digits) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                handleDialButtonPressed();
                return true;
            }
        }
        return false;
    }

    /**
     * When a key is pressed, we start playing DTMF tone, do vibration, and enter the digit
     * immediately. When a key is released, we stop the tone. Note that the "key press" event will be
     * delivered by the system with certain amount of delay, it won't be synced with user's actual
     * "touch-down" behavior.
     */
    @Override
    public void onPressed(View view, boolean pressed) {
        if (DEBUG) {
            LogUtil.d("FreemeDialpadFragment.onPressed", "view: " + view + ", pressed: " + pressed);
        }
        if (pressed) {
            int resId = view.getId();
            if (resId == R.id.one) {
                keyPressed(KeyEvent.KEYCODE_1);
            } else if (resId == R.id.two) {
                keyPressed(KeyEvent.KEYCODE_2);
            } else if (resId == R.id.three) {
                keyPressed(KeyEvent.KEYCODE_3);
            } else if (resId == R.id.four) {
                keyPressed(KeyEvent.KEYCODE_4);
            } else if (resId == R.id.five) {
                keyPressed(KeyEvent.KEYCODE_5);
            } else if (resId == R.id.six) {
                keyPressed(KeyEvent.KEYCODE_6);
            } else if (resId == R.id.seven) {
                keyPressed(KeyEvent.KEYCODE_7);
            } else if (resId == R.id.eight) {
                keyPressed(KeyEvent.KEYCODE_8);
            } else if (resId == R.id.nine) {
                keyPressed(KeyEvent.KEYCODE_9);
            } else if (resId == R.id.zero) {
                keyPressed(KeyEvent.KEYCODE_0);
            } else if (resId == R.id.pound) {
                keyPressed(KeyEvent.KEYCODE_POUND);
            } else if (resId == R.id.star) {
                keyPressed(KeyEvent.KEYCODE_STAR);
            } else {
                LogUtil.e(
                        "FreemeDialpadFragment.onPressed", "Unexpected onTouch(ACTION_DOWN) event from: " + view);
            }
            mPressedDialpadKeys.add(view);
        } else {
            mPressedDialpadKeys.remove(view);
            if (mPressedDialpadKeys.isEmpty()) {
                stopTone();
            }
        }
    }

    @Override
    public void onClick(View view) {
        int resId = view.getId();
        if (resId == R.id.dialpad_floating_action_button) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            handleDialButtonPressed();
        } else if (resId == R.id.deleteButton) {
            keyPressed(KeyEvent.KEYCODE_DEL);
        } else if (resId == R.id.digits) {
            if (!isDigitsEmpty()) {
                mDigits.setCursorVisible(true);
            }
        } else {
            LogUtil.w("FreemeDialpadFragment.onClick", "Unexpected event from: " + view);
            return;
        }
    }

    private final static String DEFAULT_DIALER_PAKAGE_NAME = "com.android.dialer";
    @Override
    public boolean onLongClick(View view) {
        final Editable digits = mDigits.getText();
        final int id = view.getId();
        if (id == R.id.deleteButton) {
            digits.clear();
            return true;
        } else if (id == R.id.one) {
            if (isDigitsEmpty() || TextUtils.equals(mDigits.getText(), "1")) {

                // We'll try to initiate voicemail and thus we want to remove irrelevant string.
                removePreviousDigitIfPossible('1');

                List<PhoneAccountHandle> subscriptionAccountHandles =
                        PhoneAccountUtils.getSubscriptionPhoneAccounts(getActivity());
                boolean hasUserSelectedDefault =
                        subscriptionAccountHandles.contains(
                                TelecomUtil.getDefaultOutgoingPhoneAccount(
                                        getActivity(), PhoneAccount.SCHEME_VOICEMAIL));
                boolean needsAccountDisambiguation =
                        subscriptionAccountHandles.size() > 1 && !hasUserSelectedDefault;

                if (needsAccountDisambiguation || isVoicemailAvailable()) {
                    // On a multi-SIM phone, if the user has not selected a default
                    // subscription, initiate a call to voicemail so they can select an account
                    // from the "Call with" dialog.
                    callVoicemail();
                } else if (getActivity() != null) {
                    // Voicemail is unavailable maybe because Airplane mode is turned on.
                    // Check the current status and show the most appropriate error message.
                    final boolean isAirplaneModeOn = Settings.System.getInt(
                            getActivity().getContentResolver(),
                            Settings.System.AIRPLANE_MODE_ON, 0) != 0;
                    if (isAirplaneModeOn) {
                        DialogFragment dialogFragment = DialpadFragment.ErrorDialogFragment
                                .newInstance(R.string.dialog_voicemail_airplane_mode_message);
                        dialogFragment.show(getFragmentManager(), "voicemail_request_during_airplane_mode");
                    } else {
                        DialogFragment dialogFragment = DialpadFragment.ErrorDialogFragment
                                .newInstance(R.string.freeme_dialog_voicemail_not_ready_message);
                        dialogFragment.show(getFragmentManager(), "voicemail_not_ready");
                    }
                }
                return true;
            }
            return false;
        } else if (id == R.id.zero) {
            if (mPressedDialpadKeys.contains(view)) {
                // If the zero key is currently pressed, then the long press occurred by touch
                // (and not via other means like certain accessibility input methods).
                // Remove the '0' that was input when the key was first pressed.
                longPressReplace('0', "+");
            }
            stopTone();
            mPressedDialpadKeys.remove(view);
            return true;
        } else if (id == R.id.nine) {
            ///M: SOS implementation, long press 9 dials ECC @{
            if (mIsSupportSOS) {
                LogUtil.d(TAG, "Nine button long pressed, initiate ECC call");
                final Intent intent = new CallIntentBuilder("112",
                        CallInitiationType.Type.DIALPAD).build();
                DialerUtils.startActivityWithErrorToast(getActivity(), intent);
                hideAndClearDialpad(false);
                return true;
            }
            /// @}
        } else if (id == R.id.digits) {
            mDigits.setCursorVisible(true);
            return false;
        } else if (id == R.id.star) {
            if (mPressedDialpadKeys.contains(view)) {
                // If the star key is currently pressed, then the long press occurred by touch
                // (and not via other means like certain accessibility input methods).
                // Remove the '*' that was input when the key was first pressed.
                longPressReplace('*', ",");
            }
            stopTone();
            mPressedDialpadKeys.remove(view);
            return true;
        } else if (id == R.id.pound) {
            if (mPressedDialpadKeys.contains(view)) {
                // If the pound key is currently pressed, then the long press occurred by touch
                // (and not via other means like certain accessibility input methods).
                // Remove the ';' that was input when the key was first pressed.
                longPressReplace('#', ";");
            }
            stopTone();
            mPressedDialpadKeys.remove(view);
            return true;
        }

        int key = 0;
        switch (id) {
            case R.id.two: {
                key = 2;
                break;
            }
            case R.id.three: {
                key = 3;
                break;
            }
            case R.id.four: {
                key = 4;
                break;
            }
            case R.id.five: {
                key = 5;
                break;
            }
            case R.id.six: {
                key = 6;
                break;
            }
            case R.id.seven: {
                key = 7;
                break;
            }
            case R.id.eight: {
                key = 8;
                break;
            }
            case R.id.nine: {
                key = 9;
                break;
            }
        }
        if (key > 1 && key < 10 && mDigits.getText().length() <= 1) {
            FreemeSpeedDialController.getInstance().handleKeyLongProcess(getActivity(), key);
            clearDialpad();
            return true;
        }
        return false;
    }

    /**
     * Remove the digit just before the current position of the cursor, iff the following conditions
     * are true: 1) The cursor is not positioned at index 0. 2) The digit before the current cursor
     * position matches the current digit.
     *
     * @param digit to remove from the digits view.
     */
    private void removePreviousDigitIfPossible(char digit) {
        final int currentPosition = mDigits.getSelectionStart();
        if (currentPosition > 0 && digit == mDigits.getText().charAt(currentPosition - 1)) {
            mDigits.setSelection(currentPosition);
            mDigits.getText().delete(currentPosition - 1, currentPosition);
        }
    }

    public void callVoicemail() {
        DialerUtils.startActivityWithErrorToast(getActivity(),
                new CallIntentBuilder(CallUtil.getVoicemailUri(),
                        CallInitiationType.Type.DIALPAD).build());
        hideAndClearDialpad(false);
    }

    private void hideAndClearDialpad(boolean animate) {
        mGeoHandler.removeMessages(HANDLER_WAHT_GEO);
        mGeoHandler.postDelayed(() -> {
            ((FreemeDialtactsActivity) getActivity())
                    .hideDialpadFragment(animate, true);
        }, 300);
    }

    /**
     * In most cases, when the dial button is pressed, there is a number in digits area. Pack it in
     * the intent, start the outgoing call broadcast as a separate task and finish this activity.
     * <p>
     * <p>When there is no digit and the phone is CDMA and off hook, we're sending a blank flash for
     * CDMA. CDMA networks use Flash messages when special processing needs to be done, mainly for
     * 3-way or call waiting scenarios. Presumably, here we're in a special 3-way scenario where the
     * network needs a blank flash before being able to add the new participant. (This is not the case
     * with all 3-way calls, just certain CDMA infrastructures.)
     * <p>
     * <p>Otherwise, there is no digit, display the last dialed number. Don't finish since the user
     * may want to edit it. The user needs to press the dial button again, to dial it (general case
     * described above).
     */
    private void handleDialButtonPressed() {
        ///M: Fix for ALPS03581591, if Dialpad is not shown, FAB
        // cannot be pressed @{
        if (getActivity() instanceof FreemeDialtactsActivity
                && !((FreemeDialtactsActivity) getActivity()).isDialpadShown()) {
            LogUtil.i(
                    "FreemeDialpadFragment.handleDialButtonPressed",
                    "Dialpad is not shown, return !");
            return;
        }
        // @}
        if (isDigitsEmpty()) { // No number entered.
            // No real call made, so treat it as a click
            PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
            handleDialButtonClickWithEmptyDigits();
        } else {
            final String number = mDigits.getText().toString();

            // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
            // test equipment.
            // TODO: clean it up.
            if (number != null
                    && !TextUtils.isEmpty(mProhibitedPhoneNumberRegexp)
                    && number.matches(mProhibitedPhoneNumberRegexp)) {
                PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
                LogUtil.i(
                        "FreemeDialpadFragment.handleDialButtonPressed",
                        "The phone number is prohibited explicitly by a rule.");
                if (getActivity() != null) {
                    DialogFragment dialogFragment =
                            DialpadFragment.ErrorDialogFragment.newInstance(R.string.dialog_phone_call_prohibited_message);
                    dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
                }

                // Clear the digits just in case.
                clearDialpad();
            } else {
                final Intent intent =
                        new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD).build();
                DialerUtils.startActivityWithErrorToast(getActivity(), intent);
                clearDialpad();
            }
        }
    }

    public void clearDialpad() {
        if (mDigits != null) {
            mDigits.getText().clear();
        }
    }

    public void handleDialButtonClickWithEmptyDigits() {
        /// M:refactor CDMA phone is in call check
        if (/*phoneIsCdma() && isPhoneInUse()*/isCdmaInCall()) {
            // TODO: Move this logic into services/Telephony
            //
            // This is really CDMA specific. On GSM is it possible
            // to be off hook and wanted to add a 3rd party using
            // the redial feature.
            startActivity(newFlashIntent());
        } else {
            if (!TextUtils.isEmpty(mLastNumberDialed)) {
                // Dialpad will be filled with last called number,
                // but we don't want to record it as user action
                PerformanceReport.setIgnoreActionOnce(UiAction.Type.TEXT_CHANGE_WITH_INPUT);

                // Recall the last number dialed.
                mDigits.setText(mLastNumberDialed);

                // ...and move the cursor to the end of the digits string,
                // so you'll be able to delete digits using the Delete
                // button (just as if you had typed the number manually.)
                //
                // Note we use mDigits.getText().length() here, not
                // mLastNumberDialed.length(), since the EditText widget now
                // contains a *formatted* version of mLastNumberDialed (due to
                // mTextWatcher) and its length may have changed.
                mDigits.setSelection(mDigits.getText().length());
            } else {
                // There's no "last number dialed" or the
                // background query is still running. There's
                // nothing useful for the Dial button to do in
                // this case.  Note: with a soft dial button, this
                // can never happens since the dial button is
                // disabled under these conditons.
                playTone(ToneGenerator.TONE_PROP_NACK);
            }
        }
    }

    /**
     * Plays the specified tone for TONE_LENGTH_MS milliseconds.
     */
    private void playTone(int tone) {
        playTone(tone, TONE_LENGTH_MS);
    }

    /**
     * Play the specified tone for the specified milliseconds
     * <p>
     * <p>The tone is played locally, using the audio stream for phone calls. Tones are played only if
     * the "Audible touch tones" user preference is checked, and are NOT played if the device is in
     * silent mode.
     * <p>
     * <p>The tone length can be -1, meaning "keep playing the tone." If the caller does so, it should
     * call stopTone() afterward.
     *
     * @param tone       a tone code from {@link ToneGenerator}
     * @param durationMs tone length.
     */
    private void playTone(int tone, int durationMs) {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager =
                (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
                || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                LogUtil.w("FreemeDialpadFragment.playTone", "mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, durationMs);
        }
    }

    /**
     * Stop the tone if it is played.
     */
    private void stopTone() {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                LogUtil.w("FreemeDialpadFragment.stopTone", "mToneGenerator == null");
                return;
            }
            mToneGenerator.stopTone();
        }
    }

    /**
     * Brings up the "dialpad chooser" UI in place of the usual Dialer elements (the textfield/button
     * and the dialpad underneath).
     * <p>
     * <p>We show this UI if the user brings up the Dialer while a call is already in progress, since
     * there's a good chance we got here accidentally (and the user really wanted the in-call dialpad
     * instead). So in this situation we display an intermediate UI that lets the user explicitly
     * choose between the in-call dialpad ("Use touch tone keypad") and the regular Dialer ("Add
     * call"). (Or, the option "Return to call in progress" just goes back to the in-call UI with no
     * dialpad at all.)
     *
     * @param enabled If true, show the "dialpad chooser" instead of the regular Dialer UI
     */
    private void showDialpadChooser(boolean enabled) {
        if (getActivity() == null) {
            return;
        }
        // Check if onCreateView() is already called by checking one of View objects.
        if (!isLayoutReady()) {
            return;
        }

        if (enabled) {
            LogUtil.i("FreemeDialpadFragment.showDialpadChooser", "Showing dialpad chooser!");
            if (mDialpadView != null) {
                mDialpadView.setVisibility(View.GONE);
            }

            if (mDialActionLayout != null) {
                mDialActionLayout.setVisibility(View.GONE);
            }

            mDialpadChooser.setVisibility(View.VISIBLE);

            // Instantiate the DialpadChooserAdapter and hook it up to the
            // ListView.  We do this only once.
            if (mDialpadChooserAdapter == null) {
                mDialpadChooserAdapter = new DialpadChooserAdapter(getActivity());
            }
            mDialpadChooser.setAdapter(mDialpadChooserAdapter);
        } else {
            LogUtil.i("FreemeDialpadFragment.showDialpadChooser", "Displaying normal Dialer UI.");
            if (mDialpadView != null) {
                mDialpadView.setVisibility(View.VISIBLE);
            } else {
                mDigits.setVisibility(View.VISIBLE);
            }

            if (mDialActionLayout != null) {
                mDialActionLayout.setVisibility(View.VISIBLE);
            }

            mDialpadChooser.setVisibility(View.GONE);
        }

        /// M: for plug-in @{
        ExtensionManager.getDialPadExtension().showDialpadChooser(enabled);
        /// @}
    }

    /**
     * @return true if we're currently showing the "dialpad chooser" UI.
     */
    private boolean isDialpadChooserVisible() {
        return mDialpadChooser.getVisibility() == View.VISIBLE;
    }

    /**
     * Handle clicks from the dialpad chooser.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        DialpadChooserAdapter.ChoiceItem item =
                (DialpadChooserAdapter.ChoiceItem) parent.getItemAtPosition(position);
        int itemId = item.id;
        if (itemId == DialpadChooserAdapter.DIALPAD_CHOICE_USE_DTMF_DIALPAD) {
            // Fire off an intent to go back to the in-call UI
            // with the dialpad visible.
            returnToInCallScreen(true);
        } else if (itemId == DialpadChooserAdapter.DIALPAD_CHOICE_RETURN_TO_CALL) {
            // Fire off an intent to go back to the in-call UI
            // (with the dialpad hidden).
            returnToInCallScreen(false);
        } else if (itemId == DialpadChooserAdapter.DIALPAD_CHOICE_ADD_NEW_CALL) {
            // Ok, guess the user really did want to be here (in the
            // regular Dialer) after all.  Bring back the normal Dialer UI.
            showDialpadChooser(false);
        } else {
            LogUtil.w("FreemeDialpadFragment.onItemClick", "Unexpected itemId: " + itemId);
        }
    }

    /**
     * Returns to the in-call UI (where there's presumably a call in progress) in response to the user
     * selecting "use touch tone keypad" or "return to call" from the dialpad chooser.
     */
    private void returnToInCallScreen(boolean showDialpad) {
        TelecomUtil.showInCallScreen(getActivity(), showDialpad);

        // Finally, finish() ourselves so that we don't stay on the
        // activity stack.
        // Note that we do this whether or not the showCallScreenWithDialpad()
        // call above had any effect or not!  (That call is a no-op if the
        // phone is idle, which can happen if the current call ends while
        // the dialpad chooser is up.  In this case we can't show the
        // InCallScreen, and there's no point staying here in the Dialer,
        // so we just take the user back where he came from...)
        getActivity().finish();
    }

    /**
     * @return true if the phone is "in use", meaning that at least one line is active (ie. off hook
     * or ringing or dialing, or on hold).
     */
    private boolean isPhoneInUse() {
        final Context context = getActivity();
        if (context != null) {
            return TelecomUtil.isInCall(context);
        }
        return false;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int resId = item.getItemId();
        if (resId == R.id.menu_2s_pause) {
            updateDialString(PAUSE);
            return true;
        } else if (resId == R.id.menu_add_wait) {
            updateDialString(WAIT);
            return true;
            /** M: [VoLTE ConfCall] handle conference call menu. @{ */
        } else if (resId == R.id.menu_volte_conf_call) {
            Activity activity = getActivity();
            if (activity != null) {
                DialerVolteUtils.handleMenuVolteConfCall(activity);
            }
            return true;
            /** @} */
        } else if (resId == R.id.menu_call_with_note) {
            CallSubjectDialog.start(getActivity(), mDigits.getText().toString());
            hideAndClearDialpad(false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the dial string (mDigits) after inserting a Pause character (,) or Wait character (;).
     */
    private void updateDialString(char newDigit) {
        if (newDigit != WAIT && newDigit != PAUSE) {
            throw new IllegalArgumentException("Not expected for anything other than PAUSE & WAIT");
        }

        int selectionStart;
        int selectionEnd;

        // SpannableStringBuilder editable_text = new SpannableStringBuilder(mDigits.getText());
        int anchor = mDigits.getSelectionStart();
        int point = mDigits.getSelectionEnd();

        selectionStart = Math.min(anchor, point);
        selectionEnd = Math.max(anchor, point);

        if (selectionStart == -1) {
            selectionStart = selectionEnd = mDigits.length();
        }

        Editable digits = mDigits.getText();

        if (canAddDigit(digits, selectionStart, selectionEnd, newDigit)) {
            digits.replace(selectionStart, selectionEnd, Character.toString(newDigit));

            if (selectionStart != selectionEnd) {
                // Unselect: back to a regular cursor, just pass the character inserted.
                mDigits.setSelection(selectionStart + 1);
            }
        }
    }

    /**
     * Update the enabledness of the "Dial" and "Backspace" buttons if applicable.
     */
    private void updateDeleteButtonEnabledState() {
        if (getActivity() == null) {
            return;
        }
        final boolean digitsNotEmpty = !isDigitsEmpty();
        mDelete.setEnabled(digitsNotEmpty);
    }

    /**
     * Handle transitions for the menu button depending on the state of the digits edit text.
     * Transition out when going from digits to no digits and transition in when the first digit is
     * pressed.
     *
     * @param transitionIn True if transitioning in, False if transitioning out
     */
    private void updateMenuOverflowButton(boolean transitionIn) {
        /** M: Fix for issue ALPS03475682 @{ */
        mVolteConfCallEnabled = supportOneKeyConference(getActivity());
        LogUtil.d("updateMenuOverflowButton", "mVolteConfCallEnabled = " + mVolteConfCallEnabled);
        /** @} */
        /** M: [VoLTE ConfCall] Always show overflow menu button for conf call. @{ */
        //if (mVolteConfCallEnabled) {
        //  return;
        //}
        /** @} */
    }

    /**
     * Check if voicemail is enabled/accessible.
     *
     * @return true if voicemail is enabled and accessible. Note that this can be false "temporarily"
     * after the app boot.
     */
    private boolean isVoicemailAvailable() {
        try {
            PhoneAccountHandle defaultUserSelectedAccount =
                    TelecomUtil.getDefaultOutgoingPhoneAccount(getActivity(), PhoneAccount.SCHEME_VOICEMAIL);
            if (defaultUserSelectedAccount == null) {
                // In a single-SIM phone, there is no default outgoing phone account selected by
                // the user, so just call TelephonyManager#getVoicemailNumber directly.
                return !TextUtils.isEmpty(getTelephonyManager().getVoiceMailNumber());
            } else {
                return !TextUtils.isEmpty(
                        TelecomUtil.getVoicemailNumber(getActivity(), defaultUserSelectedAccount));
            }
        } catch (SecurityException se) {
            // Possibly no READ_PHONE_STATE privilege.
            LogUtil.w(
                    "FreemeDialpadFragment.isVoicemailAvailable",
                    "SecurityException is thrown. Maybe privilege isn't sufficient.");
        }
        return false;
    }

    /**
     * @return true if the widget with the phone number digits is empty.
     */
    private boolean isDigitsEmpty() {
        return mDigits.length() == 0;
    }

    /**
     * Starts the asyn query to get the last dialed/outgoing number. When the background query
     * finishes, mLastNumberDialed is set to the last dialed number or an empty string if none exists
     * yet.
     */
    private void queryLastOutgoingCall() {
        mLastNumberDialed = EMPTY_NUMBER;
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        CallLogAsync.GetLastOutgoingCallArgs lastCallArgs =
                new CallLogAsync.GetLastOutgoingCallArgs(
                        getActivity(),
                        new CallLogAsync.OnLastOutgoingCallComplete() {
                            @Override
                            public void lastOutgoingCall(String number) {
                                // TODO: Filter out emergency numbers if
                                // the carrier does not want redial for
                                // these.
                                // If the fragment has already been detached since the last time
                                // we called queryLastOutgoingCall in onResume there is no point
                                // doing anything here.
                                if (getActivity() == null) {
                                    return;
                                }
                                mLastNumberDialed = number;
                                updateDeleteButtonEnabledState();
                            }
                        });
        mCallLog.getLastOutgoingCall(lastCallArgs);
    }

    private Intent newFlashIntent() {
        Intent intent = new CallIntentBuilder(EMPTY_NUMBER, CallInitiationType.Type.DIALPAD).build();
        intent.putExtra(EXTRA_SEND_EMPTY_FLASH, true);
        return intent;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        LogUtil.d(TAG, "onHiddenChanged hidden = " + hidden);
        final FreemeDialtactsActivity activity = (FreemeDialtactsActivity) getActivity();
        if (activity == null || getView() == null) {
            return;
        }
        final FreemeDialpadView dialpadView = getView().findViewById(R.id.dialpad_view);
        if (!hidden && !isDialpadChooserVisible()) {
            if (mAnimate) {
                dialpadView.animateShow();
            }
            /// M: [VoLTE ConfCall] initialize value about conference call capability. @{
            mVolteConfCallEnabled = supportOneKeyConference(getActivity());
            LogUtil.d(TAG, "onHiddenChanged false mVolteConfCallEnabled = " + mVolteConfCallEnabled);

            if (mDialActionLayout != null) {
                mDialActionLayout.setVisibility(View.VISIBLE);
            }

            /// M: for Plug-in @{
            ExtensionManager.getDialPadExtension().onHiddenChanged(
                    true, mAnimate ? mDialpadSlideInDuration : 0);
            /// @}

            activity.onDialpadShown();
            mDigits.requestFocus();
            updateWfcUI();
        }

        /// M: Need to check if floatingActionButton is null. because in CT
        // project, OP09 plugin will modify Dialpad layout and floatingActionButton
        // will be null in that case. @{
        if (hidden && mDialActionLayout != null) {
            mDialActionLayout.setVisibility(View.GONE);
        }

        /// M: for Plug-in @{
        if (hidden && mAnimate) {
            ExtensionManager.getDialPadExtension().onHiddenChanged(false, 0);
        }
        /// @}

    }

    public boolean getAnimate() {
        return mAnimate;
    }

    public void setAnimate(boolean value) {
        mAnimate = value;
    }

    public void setYFraction(float yFraction) {
        ((DialpadFragment.DialpadSlidingRelativeLayout) getView()).setYFraction(yFraction);
    }

    public int getDialpadHeight() {
        if (mDialpadView == null) {
            return 0;
        }
        return mDialpadView.getHeight();
    }

    public void process_quote_emergency_unquote(String query) {
        if (PseudoEmergencyAnimator.PSEUDO_EMERGENCY_NUMBER.equals(query)) {
            if (mPseudoEmergencyAnimator == null) {
                mPseudoEmergencyAnimator =
                        new PseudoEmergencyAnimator(
                                new PseudoEmergencyAnimator.ViewProvider() {
                                    @Override
                                    public View getView() {
                                        return FreemeDialpadFragment.this.getView();
                                    }
                                });
            }
            mPseudoEmergencyAnimator.start();
        } else {
            if (mPseudoEmergencyAnimator != null) {
                mPseudoEmergencyAnimator.end();
            }
        }
    }

    /**
     * Simple list adapter, binding to an icon + text label for each item in the "dialpad chooser"
     * list.
     */
    private static class DialpadChooserAdapter extends BaseAdapter {

        // IDs for the possible "choices":
        static final int DIALPAD_CHOICE_USE_DTMF_DIALPAD = 101;
        static final int DIALPAD_CHOICE_RETURN_TO_CALL = 102;
        static final int DIALPAD_CHOICE_ADD_NEW_CALL = 103;
        private static final int NUM_ITEMS = 3;
        private LayoutInflater mInflater;
        private DialpadChooserAdapter.ChoiceItem[] mChoiceItems = new DialpadChooserAdapter.ChoiceItem[NUM_ITEMS];

        public DialpadChooserAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);

            // Initialize the possible choices.
            // TODO: could this be specified entirely in XML?

            // - "Use touch tone keypad"
            mChoiceItems[0] =
                    new DialpadChooserAdapter.ChoiceItem(
                            context.getString(R.string.dialer_useDtmfDialpad),
                            BitmapFactory.decodeResource(
                                    context.getResources(), R.drawable.ic_dialer_fork_tt_keypad),
                            DIALPAD_CHOICE_USE_DTMF_DIALPAD);

            // - "Return to call in progress"
            mChoiceItems[1] =
                    new DialpadChooserAdapter.ChoiceItem(
                            context.getString(R.string.dialer_returnToInCallScreen),
                            BitmapFactory.decodeResource(
                                    context.getResources(), R.drawable.ic_dialer_fork_current_call),
                            DIALPAD_CHOICE_RETURN_TO_CALL);

            // - "Add call"
            mChoiceItems[2] =
                    new DialpadChooserAdapter.ChoiceItem(
                            context.getString(R.string.dialer_addAnotherCall),
                            BitmapFactory.decodeResource(
                                    context.getResources(), R.drawable.ic_dialer_fork_add_call),
                            DIALPAD_CHOICE_ADD_NEW_CALL);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        /**
         * Return the ChoiceItem for a given position.
         */
        @Override
        public Object getItem(int position) {
            return mChoiceItems[position];
        }

        /**
         * Return a unique ID for each possible choice.
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a view for each row.
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // When convertView is non-null, we can reuse it (there's no need
            // to reinflate it.)
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.dialpad_chooser_list_item, null);
            }

            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(mChoiceItems[position].text);

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageBitmap(mChoiceItems[position].icon);

            return convertView;
        }

        // Simple struct for a single "choice" item.
        static class ChoiceItem {

            String text;
            Bitmap icon;
            int id;

            public ChoiceItem(String s, Bitmap b, int i) {
                text = s;
                icon = b;
                id = i;
            }
        }
    }

    private class CallStateReceiver extends BroadcastReceiver {

        /**
         * Receive call state changes so that we can take down the "dialpad chooser" if the phone
         * becomes idle while the chooser UI is visible.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if ((TextUtils.equals(state, TelephonyManager.EXTRA_STATE_IDLE)
                    || TextUtils.equals(state, TelephonyManager.EXTRA_STATE_OFFHOOK))
                    && isDialpadChooserVisible()) {
                // Note there's a race condition in the UI here: the
                // dialpad chooser could conceivably disappear (on its
                // own) at the exact moment the user was trying to select
                // one of the choices, which would be confusing.  (But at
                // least that's better than leaving the dialpad chooser
                // onscreen, but useless...)
                showDialpadChooser(false);
            }
        }
    }

    /// M: Mediatek start.
    /**
     * M: [ALPS01858019] add listener observer CallLog changes. @{
     */
    private ContentObserver mCallLogObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (FreemeDialpadFragment.this.isAdded()) {
                LogUtil.d(TAG, "Observered the CallLog changes. queryLastOutgoingCall");
                queryLastOutgoingCall();
            }
        }

        ;
    };
    /** @} */

    /**
     * M: add for check CDMA phone is in call or not. @{
     */
    private boolean isCdmaInCall() {
        for (int subId : SubscriptionManager.from(mContext).getActiveSubscriptionIdList()) {
            if ((TelephonyManager.from(mContext).getCallState(subId) != TelephonyManager.CALL_STATE_IDLE)
                    && (TelephonyManager.from(mContext).getCurrentPhoneType(subId)
                    == TelephonyManager.PHONE_TYPE_CDMA)) {
                LogUtil.d(TAG, "Cdma In Call");
                return true;
            }
        }
        return false;
    }
    /** @} */

    /**
     * M: [VoLTE ConfCall] indicated phone account has volte conference capability.
     */
    private boolean mVolteConfCallEnabled = false;

    /**
     * M: [VoLTE ConfCall] Checking whether the volte conference is supported or not.
     *
     * @param context
     * @return ture if volte conference is supported
     */
    private boolean supportOneKeyConference(Context context) {
        // We have to requery contacts numbers from provider now.
        // Which requires contacts permissions.
        final boolean hasContactsPermission = PermissionsUtil.hasContactsReadPermissions(context);
        return DialerVolteUtils.isVolteConfCallEnable(context) && hasContactsPermission;
    }

    ///M: WFC @{
    private static final String SCHEME_TEL = PhoneAccount.SCHEME_TEL;
    private Context mContext;

    /* *
      * Update the dialer icon based on WFC is registered or not.
      *
      */
    private void updateWfcUI() {
        final View floatingActionButton = (ImageButton) getView().findViewById(
                R.id.dialpad_floating_action_button);
        if (floatingActionButton != null && DialerCompatEx.isWfcCompat()) {
            ImageView dialIcon = (ImageView) floatingActionButton;
            PhoneAccountHandle defaultAccountHandle = TelecomUtil.getDefaultOutgoingPhoneAccount(
                    getActivity(), SCHEME_TEL);
            LogUtil.d(TAG, "[WFC] defaultAccountHandle: " + defaultAccountHandle);
            if (defaultAccountHandle != null) {
                PhoneAccount phoneAccount = TelecomUtil
                        .getPhoneAccount(getActivity(), defaultAccountHandle);
                LogUtil.d(TAG, "[WFC] Phone Account: " + phoneAccount);
                if (phoneAccount != null) {
                    TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
                    int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
                    boolean wfcCapability = MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(subId);
                    LogUtil.d(TAG, "[WFC] WFC Capability: " + wfcCapability + " (subId= " + subId + ")");
                    if (wfcCapability) {
                        dialIcon.setImageDrawable(getResources().getDrawable(R.drawable.mtk_fab_ic_wfc));
                        LogUtil.d(TAG, "[WFC] Dial Icon is changed to WFC dial icon");
                    } else {
                        dialIcon.setImageDrawable(getResources().getDrawable(
                                R.drawable.quantum_ic_call_white_24));
                    }
                } else {
                    dialIcon.setImageDrawable(getResources().getDrawable(R.drawable.quantum_ic_call_white_24));
                }
            } else {
                dialIcon.setImageDrawable(getResources().getDrawable(R.drawable.quantum_ic_call_white_24));
            }
        }
    }

    /**
     * M: add for plug-in.
     */
    @Override
    public void doCallOptionHandle(Intent intent) {
        DialerUtils.startActivityWithErrorToast(getActivity(), intent);
        hideAndClearDialpad(false);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelecomManager.ACTION_PHONE_ACCOUNT_REGISTERED.equals(action)
                    || TelecomManager.ACTION_PHONE_ACCOUNT_UNREGISTERED.equals(action)) {
                LogUtil.i(TAG, "[WFC] Intent recived is " + intent.getAction());
                updateWfcUI();
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mContext.unregisterReceiver(mReceiver);
        unregisterReceiverFreeme();
    }
    ///@}
    /// M: Mediatek end.

    private View sim1;
    private TextView sim1Text;
    private View sim2;
    private TextView sim2Text;

    private void showDialButton() {
        List<SubscriptionInfo> list = SubscriptionManager.from(getContext())
                .getActiveSubscriptionInfoList();
        boolean isSingleOrNoneSim = list == null || list.size() <= 1;
        if (isSingleOrNoneSim) {
            initCallBtn1(true, null);
            if (sim2 != null) {
                sim2.setVisibility(View.GONE);
            }
        } else {
            CharSequence[] simNames = new CharSequence[list.size()];
            for (SubscriptionInfo info : list) {
                simNames[info.getSimSlotIndex()] = info.getDisplayName();
            }
            initCallBtn1(false, simNames[0]);
            initCallBtn2(simNames[1]);
        }
    }

    private static final long INTERVAL = 200;

    private void initCallBtn1(final boolean isSingleSim, CharSequence simName) {
        if (sim1 == null) {
            sim1 = getView().findViewById(R.id.sim_btn_layout);
            sim1Text = sim1.findViewById(R.id.sim_text);
        }
        sim1.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (isSingleSim) {
                handleDialButtonPressed();
            } else {
                handleDialButtonPressed(0);
            }
            DialerUtils.disableViewClickableInDuration(sim1, INTERVAL);
        });
        sim1Text.setText(simName);
        int left = isSingleSim ? R.drawable.freeme_dial_action_btn_sim_icon :
                R.drawable.freeme_dial_action_btn_sim1_icon;
        sim1Text.setCompoundDrawablesWithIntrinsicBounds(left, 0, 0, 0);
    }

    private void initCallBtn2(CharSequence simName) {
        if (sim2 == null) {
            ViewStub stub = getView().findViewById(R.id.sim2_btn_layout);
            sim2 = stub.inflate();
            sim2Text = sim2.findViewById(R.id.sim_text);
        }
        sim2.setVisibility(View.VISIBLE);
        sim2.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            handleDialButtonPressed(1);
            DialerUtils.disableViewClickableInDuration(sim2, INTERVAL);
        });
        sim2Text.setText(simName);
        sim2Text.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.freeme_dial_action_btn_sim2_icon, 0, 0, 0);
    }

    private void handleDialButtonPressed(int simIdx) {
        ///M: Fix for ALPS03581591, if Dialpad is not shown, FAB
        // cannot be pressed @{
        if (getActivity() instanceof FreemeDialtactsActivity
                && !((FreemeDialtactsActivity) getActivity()).isDialpadShown()) {
            LogUtil.i(
                    "FreemeDialpadFragment.handleDialButtonPressed",
                    "Dialpad is not shown, return !");
            return;
        }
        // @}
        if (isDigitsEmpty()) { // No number entered.
            // No real call made, so treat it as a click
            PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
            handleDialButtonClickWithEmptyDigits();
        } else {
            final String number = mDigits.getText().toString();

            // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
            // test equipment.
            // TODO: clean it up.
            if (number != null
                    && !TextUtils.isEmpty(mProhibitedPhoneNumberRegexp)
                    && number.matches(mProhibitedPhoneNumberRegexp)) {
                PerformanceReport.recordClick(UiAction.Type.PRESS_CALL_BUTTON_WITHOUT_CALLING);
                LogUtil.i(
                        "FreemeDialpadFragment.handleDialButtonPressed",
                        "The phone number is prohibited explicitly by a rule.");
                if (getActivity() != null) {
                    DialogFragment dialogFragment =
                            DialpadFragment.ErrorDialogFragment.newInstance(R.string.dialog_phone_call_prohibited_message);
                    dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
                }

                // Clear the digits just in case.
                clearDialpad();
            } else {
                final Intent intent = new CallIntentBuilder(number, CallInitiationType.Type.DIALPAD)
                        .setPhoneAccountHandle(FreemeDialerUtils.getPhoneAccountHandleBySlot(getActivity(), simIdx))
                        .build();
                DialerUtils.startActivityWithErrorToast(getActivity(), intent);
                clearDialpad();
            }
        }
    }

    private final long MAX_INTERVALS_TIME = 200;
    private long mRequestTime = 0;
    private final int HANDLER_WAHT_GEO = 1;

    private void showGeoLocation() {
        long current = System.currentTimeMillis();
        if (current - mRequestTime < MAX_INTERVALS_TIME) {
            mGeoHandler.removeMessages(HANDLER_WAHT_GEO);
        }
        mRequestTime = current;
        mGeoHandler.sendEmptyMessageDelayed(HANDLER_WAHT_GEO, MAX_INTERVALS_TIME);
    }

    private Handler mGeoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (getContext() == null) {
                removeMessages(HANDLER_WAHT_GEO);
                return;
            }
            String geo = GeoUtil.getGeocodedLocationFor(getContext(),
                    mDigits.getText().toString());
            mDialpadView.setGeoLocation(geo);
            removeMessages(HANDLER_WAHT_GEO);
        }
    };

    private SimStateReceiver mSimStateReceiver;

    private class SimStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            showDialButton();
        }
    }

    private void registerReceiverFreeme() {
        mSimStateReceiver = new SimStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        mContext.registerReceiver(mSimStateReceiver, filter);
    }

    private void unregisterReceiverFreeme() {
        mContext.unregisterReceiver(mSimStateReceiver);
    }

    private void longPressReplace(char src, String des) {
        final int currentPosition = mDigits.getSelectionStart();
        if (currentPosition > 0 && src == mDigits.getText().charAt(currentPosition - 1)) {
            mDigits.setSelection(currentPosition);
            CharSequence s = mDigits.getText().replace(currentPosition - 1, currentPosition, des);
            mDigits.setText(s);
            mDigits.setSelection(mDigits.getText().length());
        }
    }

    private String mSearchQuery;

    public void setSearchQuery(String query) {
        this.mSearchQuery = query;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final FreemeDialtactsActivity activity = (FreemeDialtactsActivity) getActivity();
        mDialpadQueryListener = activity;

        if (mDialpadQueryListener != null) {
            if (!TextUtils.isEmpty(mSearchQuery)) {
                mDialpadQueryListener.onDialpadQueryChanged(mSearchQuery);
            } else {
                mDialpadQueryListener.onDialpadQueryChanged(mDigits.getText().toString());
            }
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        if (mDigitsContainer != null && mDigits != null) {
            if (!isLand && !isInMultiWindowMode && mDigits.getText().length() == 0) {
                mDigitsContainer.removeAllViews();
            }
        }
    }
}
