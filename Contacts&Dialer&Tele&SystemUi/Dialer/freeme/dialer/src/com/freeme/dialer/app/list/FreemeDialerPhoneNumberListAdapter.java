package com.freeme.dialer.app.list;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.format.TextHighlighter;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.PhoneNumberListAdapter;
import com.android.contacts.common.util.ContactDisplayUtils;
import com.android.dialer.app.R;
import com.android.dialer.app.calllog.calllogcache.CallLogCache;
import com.android.dialer.app.list.RegularSearchListAdapter;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.calllogutils.PhoneNumberDisplayUtil;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.util.CallUtil;
import com.freeme.contacts.common.utils.FreemeIntentUtils;
import com.freeme.contacts.common.utils.FreemeYellowPageUtils;
import com.mediatek.dialer.compat.ContactsCompat;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.search.DialerSearchHelper;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.DialerSearchUtils;

import java.util.ArrayList;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class FreemeDialerPhoneNumberListAdapter extends PhoneNumberListAdapter {
    public static final int SHORTCUT_INVALID = -1;
    public static final int SHORTCUT_DIRECT_CALL = 0;
    public static final int SHORTCUT_CREATE_NEW_CONTACT = 1;
    public static final int SHORTCUT_ADD_TO_EXISTING_CONTACT = 2;
    public static final int SHORTCUT_SEND_SMS_MESSAGE = 3;
    public static final int SHORTCUT_MAKE_VIDEO_CALL = 4;
    public static final int SHORTCUT_BLOCK_NUMBER = 5;
    public static final int SHORTCUT_COUNT = 6;

    private final boolean[] mShortcutEnabled = new boolean[SHORTCUT_COUNT];
    private final BidiFormatter mBidiFormatter = BidiFormatter.getInstance();
    private final boolean mVideoCallingEnabled;
    private final String mCountryIso;

    private String mFormattedQueryString;

    public FreemeDialerPhoneNumberListAdapter(Context context) {
        super(context);

        setSectionHeaderDisplayEnabled(false);
        setPinnedPartitionHeadersEnabled(false);

        mCountryIso = GeoUtil.getCurrentCountryIso(context);

        /// M: [MTK Dialer Search] @{
        mPhoneNumberUtils = new CallLogCache(context);
        if (DialerFeatureOptions.isDialerSearchEnabled()) {
            initResources(context);
        }
        /// @}
        mVideoCallingEnabled = CallUtil.isVideoEnabled(context);
    }

    @Override
    public int getCount() {
        return super.getCount() + getShortcutCount();
    }

    /**
     * @return The number of enabled shortcuts. Ranges from 0 to a maximum of SHORTCUT_COUNT
     */
    public int getShortcutCount() {
        int count = 0;
        for (int i = 0; i < mShortcutEnabled.length; i++) {
            if (mShortcutEnabled[i]) {
                count++;
            }
        }
        return count;
    }

    public void disableAllShortcuts() {
        for (int i = 0; i < mShortcutEnabled.length; i++) {
            mShortcutEnabled[i] = false;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isFreemeYellowPageTitle(position)) {
            return VIEW_TYPE_CALL_YELLOW_TITLE;
        }
        final int shortcut = getShortcutTypeFromPosition(position);
        if (shortcut >= 0) {
            // shortcutPos should always range from 1 to SHORTCUT_COUNT
            return super.getViewTypeCount() + shortcut;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getViewTypeCount() {
        // Number of item view types in the super implementation + 2 for the 2 new shortcuts
        return super.getViewTypeCount() + SHORTCUT_COUNT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isFreemeYellowPageTitle(position)) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.freeme_list_item_header, null);

                TextView header = convertView.findViewById(R.id.header);
                header.setText(R.string.freeme_yellow_page_label);
            }

            return convertView;
        }
        final int shortcutType = getShortcutTypeFromPosition(position);
        if (shortcutType >= 0) {
            if (convertView != null) {
                assignShortcutToView((ContactListItemView) convertView, shortcutType);
                return convertView;
            } else {
                final ContactListItemView v =
                        new ContactListItemView(getContext(), null, mIsVideoEnabled);
                assignShortcutToView(v, shortcutType);
                return v;
            }
        } else {
            return super.getView(position, convertView, parent);
        }
    }

    /** M: original newView:
     @Override protected ContactListItemView newView(
     Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
     final ContactListItemView view = super.newView(context, partition, cursor, position, parent);

     view.setSupportVideoCallIcon(mIsImsVideoEnabled);
     return view;
     }*/

    /**
     * @param position The position of the item
     * @return The enabled shortcut type matching the given position if the item is a shortcut, -1
     * otherwise
     */
    public int getShortcutTypeFromPosition(int position) {
        int shortcutCount = position - super.getCount();
        if (shortcutCount >= 0) {
            // Iterate through the array of shortcuts, looking only for shortcuts where
            // mShortcutEnabled[i] is true
            for (int i = 0; shortcutCount >= 0 && i < mShortcutEnabled.length; i++) {
                if (mShortcutEnabled[i]) {
                    shortcutCount--;
                    if (shortcutCount < 0) {
                        return i;
                    }
                }
            }
            throw new IllegalArgumentException(
                    "Invalid position - greater than cursor count " + " but not a shortcut.");
        }
        return SHORTCUT_INVALID;
    }

    @Override
    public boolean isEmpty() {
        return getShortcutCount() == 0 && super.isEmpty();
    }

    @Override
    public boolean isEnabled(int position) {
        if (isFreemeYellowPageTitle(position)) {
            return false;
        }
        final int shortcutType = getShortcutTypeFromPosition(position);
        if (shortcutType >= 0) {
            return true;
        } else {
            return super.isEnabled(position);
        }
    }

    private void assignShortcutToView(ContactListItemView v, int shortcutType) {
        final CharSequence text;
        final Resources resources = getContext().getResources();
        final String number = getFormattedQueryString();
        switch (shortcutType) {
            case SHORTCUT_DIRECT_CALL:
                text = ContactDisplayUtils.getTtsSpannedPhoneNumber(
                        resources,
                        R.string.search_shortcut_call_number,
                        mBidiFormatter.unicodeWrap(number, TextDirectionHeuristics.LTR));
                break;
            case SHORTCUT_CREATE_NEW_CONTACT:
                text = resources.getString(R.string.search_shortcut_create_new_contact);
                break;
            case SHORTCUT_ADD_TO_EXISTING_CONTACT:
                text = resources.getString(R.string.search_shortcut_add_to_contact);
                break;
            case SHORTCUT_SEND_SMS_MESSAGE:
                text = resources.getString(R.string.search_shortcut_send_sms_message);
                break;
            case SHORTCUT_MAKE_VIDEO_CALL:
                text = resources.getString(R.string.search_shortcut_make_video_call);
                break;
            case SHORTCUT_BLOCK_NUMBER:
                text = resources.getString(R.string.search_shortcut_block_number);
                break;
            default:
                throw new IllegalArgumentException("Invalid shortcut type");
        }
        v.setDisplayName(text);
        v.setAdjustSelectionBoundsEnabled(false);
        v.setBackgroundResource(R.drawable.freeme_list_item_press_selector);
        v.getNameTextView().setTextColor(getContext().getColor(R.color.freeme_color_accent));

        /// M: for Plug-in to customize the view. @{
        ExtensionManager.getDialPadExtension()
                .customizeDialerOptions(v, shortcutType, number);
        ///@}

    }

    /**
     * @return True if the shortcut state (disabled vs enabled) was changed by this operation
     */
    public boolean setShortcutEnabled(int shortcutType, boolean visible) {
        final boolean changed = mShortcutEnabled[shortcutType] != visible;
        mShortcutEnabled[shortcutType] = visible;
        return changed;
    }

    public String getFormattedQueryString() {
        return mFormattedQueryString;
    }

    @Override
    public void setQueryString(String queryString) {
        mFormattedQueryString =
                PhoneNumberUtils.formatNumber(PhoneNumberUtils.normalizeNumber(queryString), mCountryIso);
        super.setQueryString(queryString);
        /// M: For query presence after search info changed.
        ExtensionManager.getDialPadExtension().onSetQueryString(queryString);
    }

    /// M: Mediatek start.
    /// M: [MTK Dialer Search] @{
    private final String TAG = this.getClass().getSimpleName();

    private final int VIEW_TYPE_UNKNOWN = -1;
    private final int VIEW_TYPE_CONTACT = 0;
    private final int VIEW_TYPE_CALL_LOG = 1;

    private final int NUMBER_TYPE_NORMAL = 0;
    private final int NUMBER_TYPE_UNKNOWN = 1;
    private final int NUMBER_TYPE_VOICEMAIL = 2;
    private final int NUMBER_TYPE_PRIVATE = 3;
    private final int NUMBER_TYPE_PAYPHONE = 4;
    private final int NUMBER_TYPE_EMERGENCY = 5;

    private final int DS_MATCHED_DATA_INIT_POS = 3;
    private final int DS_MATCHED_DATA_DIVIDER = 3;

    public final int NAME_LOOKUP_ID_INDEX = 0;
    public final int CONTACT_ID_INDEX = 1;
    public final int DATA_ID_INDEX = 2;
    public final int CALL_LOG_DATE_INDEX = 3;
    public final int CALL_LOG_ID_INDEX = 4;
    public final int CALL_TYPE_INDEX = 5;
    public final int CALL_GEOCODED_LOCATION_INDEX = 6;
    public final int PHONE_ACCOUNT_ID_INDEX = 7;
    public final int PHONE_ACCOUNT_COMPONENT_NAME_INDEX = 8;
    public final int PRESENTATION_INDEX = 9;
    public final int INDICATE_PHONE_SIM_INDEX = 10;
    public final int CONTACT_STARRED_INDEX = 11;
    public final int PHOTO_ID_INDEX = 12;
    public final int SEARCH_PHONE_TYPE_INDEX = 13;
    public final int SEARCH_PHONE_LABEL_INDEX = 14;
    public final int NAME_INDEX = 15;
    public final int SEARCH_PHONE_NUMBER_INDEX = 16;
    public final int CONTACT_NAME_LOOKUP_INDEX = 17;
    public final int IS_SDN_CONTACT = 18;
    public final int DS_MATCHED_DATA_OFFSETS = 19;
    public final int DS_MATCHED_NAME_OFFSETS = 20;

    private ContactPhotoManager mContactPhotoManager;
    private final CallLogCache mPhoneNumberUtils;
    private PhoneNumberDisplayUtil mPhoneNumberHelper;

    private String mUnknownNumber;
    private String mPrivateNumber;
    private String mPayphoneNumber;

    private String mVoiceMail;

    private TextHighlighter mTextHighlighter;

    /**
     * M: bind view for mediatek's search UI.
     *
     * @see com.android.contacts.common.list.PhoneNumberListAdapter
     * #bindView(android.view.View, int, android.database.Cursor, int)
     */
    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        if (this instanceof RegularSearchListAdapter || !DialerFeatureOptions.isDialerSearchEnabled()) {
            super.bindView(itemView, partition, cursor, position);
            return;
        }

        final int viewType = getViewType(cursor);
        switch (viewType) {
            case VIEW_TYPE_CONTACT:
                bindContactView(itemView, getContext(), cursor);
                break;
            case VIEW_TYPE_CALL_LOG:
                bindCallLogView(itemView, getContext(), cursor);
                break;
            case VIEW_TYPE_CALL_YELLOW:
                bindFreemeYellowView(itemView, getContext(), cursor);
                break;
            default:
                break;
        }
    }

    /**
     * M: create item view for this feature
     *
     * @see com.android.contacts.common.list.PhoneNumberListAdapter
     * #newView(android.content.Context, int, android.database.Cursor, int, android.view.ViewGroup)
     */
    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
                           ViewGroup parent) {
        if (this instanceof RegularSearchListAdapter || !DialerFeatureOptions.isDialerSearchEnabled()) {
            final ContactListItemView view = (ContactListItemView) super.newView(context, partition,
                    cursor, position, parent);

            /// M: [N Conflict Change] N add Video Call Icon feature
            //  And add plugin to disable video icon in OP01. @{
            view.setSupportVideoCallIcon(ExtensionManager
                    .getDialPadExtension().isSupportVideoCallIcon(mVideoCallingEnabled));
            /// @}
            return view;
        }

        View view = View.inflate(context, R.layout.freeme_dialer_search_item_view, null);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.name = view.findViewById(R.id.name);
        viewHolder.labelAndNumber = view.findViewById(R.id.labelAndNumber);
        viewHolder.address = view.findViewById(R.id.address);
        viewHolder.detail = view.findViewById(R.id.detail);
        viewHolder.callMarkStub = view.findViewById(R.id.call_mark_stub);
        view.setTag(viewHolder);
        return view;
    }

    /**
     * M: init UI resources
     *
     * @param context
     */
    private void initResources(Context context) {
        mContactPhotoManager = ContactPhotoManager.getInstance(context);
        mPhoneNumberHelper = new PhoneNumberDisplayUtil();

        mVoiceMail = context.getResources().getString(R.string.voicemail);
        mPrivateNumber = context.getResources().getString(R.string.private_num_non_verizon);
        mPayphoneNumber = context.getResources().getString(R.string.payphone);
        mUnknownNumber = context.getResources().getString(R.string.unknown);
    }

    /**
     * M: calculate view's type from cursor
     *
     * @param cursor
     * @return type number
     */
    private int getViewType(Cursor cursor) {
        String label = cursor.getString(SEARCH_PHONE_LABEL_INDEX);
        if (FreemeYellowPageUtils.FREEME_YELLOWPAGE_TITLE_LABEL.equals(label)) {
            return VIEW_TYPE_CALL_YELLOW_TITLE;
        } else if (FreemeYellowPageUtils.FREEME_YELLOWPAGE_DATA_LABEL.equals(label)) {
            return VIEW_TYPE_CALL_YELLOW;
        }
        int retval = VIEW_TYPE_UNKNOWN;
        final int contactId = cursor.getInt(CONTACT_ID_INDEX);
        final int callLogId = cursor.getInt(CALL_LOG_ID_INDEX);

        Log.d(TAG, "getViewType: contactId: " + contactId + " ,callLogId: " + callLogId);

        if (contactId > 0) {
            retval = VIEW_TYPE_CONTACT;
        } else if (callLogId > 0) {
            retval = VIEW_TYPE_CALL_LOG;
        }

        return retval;
    }

    /**
     * M: bind contact view from cursor data
     *
     * @param view
     * @param context
     * @param cursor
     */
    private void bindContactView(View view, Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
        if (viewHolder.callMark != null) {
            viewHolder.callMark.setVisibility(View.GONE);
        }

        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        String formatNumber = numberLeftToRight(number);
        if (formatNumber == null) {
            formatNumber = number;
        }

        final int presentation = cursor.getInt(PRESENTATION_INDEX);
        final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
                cursor.getString(PHONE_ACCOUNT_COMPONENT_NAME_INDEX),
                cursor.getString(PHONE_ACCOUNT_ID_INDEX));

        final int numberType = getNumberType(accountHandle, number, presentation);

        final int labelType = cursor.getInt(SEARCH_PHONE_TYPE_INDEX);
        CharSequence label = cursor.getString(SEARCH_PHONE_LABEL_INDEX);
        int subId = cursor.getInt(INDICATE_PHONE_SIM_INDEX);
        // Get type label only if it will not be "Custom" because of an empty label.
        // So IMS contacts search item don't show lable as "Custom".
        if (!(labelType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM && TextUtils.isEmpty(label))) {
            /// M: Using new API for AAS phone number label lookup
            label = ContactsCompat.PhoneCompat.getTypeLabel(mContext, labelType, label);
        }
        final CharSequence displayName = cursor.getString(NAME_INDEX);

        Uri contactUri = getContactUri(cursor);
        Log.d(TAG, "bindContactView, contactUri: " + contactUri);

        if (isSpecialNumber(numberType)) {
            if (numberType == NUMBER_TYPE_VOICEMAIL) {
                viewHolder.name.setText(mVoiceMail);

                viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphen(highlight, formatNumber, number);
                    viewHolder.labelAndNumber.setText(style);
                } else {
                    viewHolder.labelAndNumber.setText(formatNumber);
                }
            } else {
                final String convert = specialNumberToString(numberType);
                viewHolder.name.setText(convert);
            }
        } else {
            // empty name ?
            if (!TextUtils.isEmpty(displayName)) {
                // highlight name
                String highlight = getNameHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightString(highlight, displayName);
                    viewHolder.name.setText(style);
                    if (isRegularSearch(cursor)) {
                        viewHolder.name.setText(highlightName(highlight, displayName));
                    }
                } else {
                    viewHolder.name.setText(displayName);
                }
                // highlight number
                if (!TextUtils.isEmpty(formatNumber)) {
                    highlight = getNumberHighlight(cursor);
                    if (!TextUtils.isEmpty(highlight)) {
                        SpannableStringBuilder style = highlightHyphen(highlight, formatNumber, number);
                        setLabelAndNumber(viewHolder.labelAndNumber, label, style);
                    } else {
                        setLabelAndNumber(viewHolder.labelAndNumber, label, new SpannableStringBuilder(
                                formatNumber));
                    }
                } else {
                    viewHolder.labelAndNumber.setVisibility(View.GONE);
                }
            } else {
                viewHolder.labelAndNumber.setVisibility(View.GONE);

                // highlight number and set number to name text view
                if (!TextUtils.isEmpty(formatNumber)) {
                    final String highlight = getNumberHighlight(cursor);
                    if (!TextUtils.isEmpty(highlight)) {
                        SpannableStringBuilder style = highlightHyphen(highlight, formatNumber, number);
                        viewHolder.name.setText(style);
                    } else {
                        viewHolder.name.setText(formatNumber);
                    }
                } else {
                    viewHolder.name.setVisibility(View.GONE);
                }
            }
        }

        String geoCode = GeoUtil.getGeocodedLocationFor(context, number);
        viewHolder.address.setText(geoCode);

        viewHolder.detail.setVisibility(View.VISIBLE);
        viewHolder.detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FreemeIntentUtils.showContactDetailsFromCallloglist(mContext, contactUri,
                        true, number);
            }
        });

        /// M: add for plug-in @{
        ExtensionManager.getDialerSearchExtension()
                .removeCallAccountForDialerSearch(context, view);
        /// @}

    }

    /**
     * M: Bind call log view by cursor data
     *
     * @param view
     * @param context
     * @param cursor
     */
    private void bindCallLogView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        String formattedNumber = numberLeftToRight(number);
        if (TextUtils.isEmpty(formattedNumber)) {
            formattedNumber = number;
        }

        final int presentation = cursor.getInt(PRESENTATION_INDEX);
        final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
                cursor.getString(PHONE_ACCOUNT_COMPONENT_NAME_INDEX),
                cursor.getString(PHONE_ACCOUNT_ID_INDEX));

        final int numberType = getNumberType(accountHandle, number, presentation);

        final String geocode = cursor.getString(CALL_GEOCODED_LOCATION_INDEX);
        viewHolder.labelAndNumber.setText(geocode);

        viewHolder.address.setText(R.string.freeme_strange_number);

        if (isSpecialNumber(numberType)) {
            if (numberType == NUMBER_TYPE_VOICEMAIL) {
                viewHolder.name.setText(mVoiceMail);
                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphen(highlight, formattedNumber, number);
                    viewHolder.labelAndNumber.setText(style);
                } else {
                    viewHolder.labelAndNumber.setText(formattedNumber);
                }
            } else {
                final String convert = specialNumberToString(numberType);
                viewHolder.name.setText(convert);
            }
        } else {
            if (!TextUtils.isEmpty(formattedNumber)) {
                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphen(highlight, formattedNumber, number);
                    viewHolder.name.setText(style);
                } else {
                    viewHolder.name.setText(formattedNumber);
                }
            }
        }

        if (viewHolder.callMark == null && viewHolder.callMarkStub != null) {
            View v = viewHolder.callMarkStub.inflate();
            if (v instanceof TextView) {
                viewHolder.callMark = (TextView) v;
            }
        }
        if (viewHolder.callMark != null) {
            viewHolder.callMark.setVisibility(View.VISIBLE);
            viewHolder.callMark.setText(cursor.getString(SEARCH_PHONE_LABEL_INDEX));
        }

        viewHolder.detail.setVisibility(View.VISIBLE);
        viewHolder.detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FreemeIntentUtils.startStrangerActivityWithErrorToast(mContext, number, geocode);
            }
        });

        /// M: add for plug-in @{
        ExtensionManager.getDialerSearchExtension()
                .setCallAccountForDialerSearch(context, view, accountHandle);
        /// @}
    }

    private int getNumberType(PhoneAccountHandle accountHandle, CharSequence number,
                              int presentation) {
        int type = NUMBER_TYPE_NORMAL;
        if (presentation == CallLog.Calls.PRESENTATION_UNKNOWN) {
            type = NUMBER_TYPE_UNKNOWN;
        } else if (presentation == CallLog.Calls.PRESENTATION_RESTRICTED) {
            type = NUMBER_TYPE_PRIVATE;
        } else if (presentation == CallLog.Calls.PRESENTATION_PAYPHONE) {
            type = NUMBER_TYPE_PAYPHONE;
        } else if (mPhoneNumberUtils.isVoicemailNumber(accountHandle, number)) {
            type = NUMBER_TYPE_VOICEMAIL;
        }
        if (PhoneNumberHelper.isLegacyUnknownNumbers(number)) {
            type = NUMBER_TYPE_UNKNOWN;
        }
        return type;
    }

    private Uri getContactUri(Cursor cursor) {
        final String lookup = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
        final int contactId = cursor.getInt(CONTACT_ID_INDEX);
        return ContactsContract.Contacts.getLookupUri(contactId, lookup);
    }

    private boolean isSpecialNumber(int type) {
        return type != NUMBER_TYPE_NORMAL;
    }

    /**
     * M: highlight search result string
     *
     * @param highlight
     * @param target
     * @return
     */
    private SpannableStringBuilder highlightString(String highlight, CharSequence target) {
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        int length = highlight.length();
        final int styleLength = style.length();
        int start = -1;
        int end = -1;
        for (int i = DS_MATCHED_DATA_INIT_POS; i + 1 < length; i += DS_MATCHED_DATA_DIVIDER) {
            start = (int) highlight.charAt(i);
            end = (int) highlight.charAt(i + 1) + 1;
            /// M: If highlight area is invalid, just skip it.
            if (start > styleLength || end > styleLength || start > end) {
                Log.d(TAG, "highlightString, start: " + start + " ,end: " + end + " ,styleLength: "
                        + styleLength);
                break;
            }
            style.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }

    /**
     * M: highlight searched result name
     *
     * @param highlight
     * @param target
     * @return
     */
    private CharSequence highlightName(String highlight, CharSequence target) {
        String highlightedPrefix = getUpperCaseQueryString();
        if (highlightedPrefix != null) {
            mTextHighlighter = new TextHighlighter(Typeface.BOLD);
            target = mTextHighlighter.applyPrefixHighlight(target, highlightedPrefix);
        }
        return target;
    }

    /**
     * M: highlight search result hyphen
     *
     * @param highlight
     * @param target
     * @param origin
     * @return
     */
    private SpannableStringBuilder highlightHyphen(String highlight, String target, String origin) {
        if (target == null) {
            Log.w(TAG, "highlightHyphen target is null");
            return null;
        }
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        ArrayList<Integer> numberHighlightOffset = DialerSearchUtils.adjustHighlitePositionForHyphen(
                target, highlight.substring(DS_MATCHED_DATA_INIT_POS), origin);
        if (numberHighlightOffset != null && numberHighlightOffset.size() > 1) {
            int highLightColor = mContext.getResources().getColor(R.color.freeme_color_accent);
            style.setSpan(new ForegroundColorSpan(highLightColor), numberHighlightOffset.get(0),
                    numberHighlightOffset.get(1) + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }

    private String getNameHighlight(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearchHelper.DialerSearch.MATCHED_NAME_OFFSET);
        return index != -1 ? cursor.getString(index) : null;
    }

    private boolean isRegularSearch(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearchHelper.DialerSearch.MATCHED_DATA_OFFSET);
        String regularSearch = (index != -1 ? cursor.getString(index) : null);
        Log.d(TAG, "" + regularSearch);

        return Boolean.valueOf(regularSearch);
    }

    private String getNumberHighlight(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearchHelper.DialerSearch.MATCHED_DATA_OFFSET);
        return index != -1 ? cursor.getString(index) : null;
    }

    /**
     * M: set label and number to view
     *
     * @param view
     * @param label
     * @param number
     */
    private void setLabelAndNumber(TextView view, CharSequence label,
                                   SpannableStringBuilder number) {
        view.setText(number);
    }

    private String specialNumberToString(int type) {
        switch (type) {
            case NUMBER_TYPE_UNKNOWN:
                return mUnknownNumber;
            case NUMBER_TYPE_PRIVATE:
                return mPrivateNumber;
            case NUMBER_TYPE_PAYPHONE:
                return mPayphoneNumber;
            default:
                break;
        }
        return null;
    }

    private class ViewHolder {
        public TextView name;
        public TextView labelAndNumber;
        public TextView address;
        public ImageView detail;
        public ViewStub callMarkStub;
        public TextView callMark;
    }

    /**
     * M: Fix ALPS01398152, Support RTL display for Arabic/Hebrew/Urdu
     *
     * @param origin
     * @return
     */
    private String numberLeftToRight(String origin) {
        return TextUtils.isEmpty(origin) ? origin : '\u202D' + origin + '\u202C';
    }
    /// @}

    private final int VIEW_TYPE_CALL_YELLOW = 2;
    private final int VIEW_TYPE_CALL_YELLOW_TITLE = 3;

    private boolean isFreemeYellowPageTitle(int position) {
        Cursor cursor = (Cursor) getItem(position);
        if (cursor != null && !cursor.isNull(SEARCH_PHONE_LABEL_INDEX)) {
            return FreemeYellowPageUtils.FREEME_YELLOWPAGE_TITLE_LABEL.equals(
                    cursor.getString(SEARCH_PHONE_LABEL_INDEX));
        }
        return false;
    }

    private FreemeYellowPageUtils freemeYellowPageUtils = new FreemeYellowPageUtils();

    private void bindFreemeYellowView(View view, Context context, Cursor cursor) {
        int highLightColor = context.getResources().getColor(R.color.freeme_color_accent);
        String name = cursor.getString(NAME_INDEX);
        String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        String fullPin = cursor.getString(DS_MATCHED_DATA_OFFSETS);
        String jianPin = cursor.getString(DS_MATCHED_NAME_OFFSETS);
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
        viewHolder.address.setVisibility(View.GONE);
        viewHolder.detail.setVisibility(View.GONE);
        if (viewHolder.callMark != null) {
            viewHolder.callMark.setVisibility(View.GONE);
        }
        viewHolder.name.setText(name);
        String query = getQueryString().replace(" ", "");
        SpannableString span = new SpannableString(number);
        if (number.startsWith(query)) {
            span.setSpan(new ForegroundColorSpan(highLightColor),
                    0, query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewHolder.labelAndNumber.setText(span);
        } else {
            viewHolder.labelAndNumber.setText(number);
        }
        int span_start = 0;
        int span_end = 0;
        if (fullPin.indexOf(query) >= 0) {
            String[] chineseHighLight = getFullPinQuery(query, name, fullPin);
            span_start = Integer.parseInt(chineseHighLight[0]);
            span_end = span_start + chineseHighLight[1].length();
        } else if (jianPin.indexOf(query) >= 0) {
            String[] chineseHighLight = getJianPinQuery(query, jianPin);
            span_start = Integer.parseInt(chineseHighLight[0]);
            span_end = span_start + chineseHighLight[1].length();
        }
        SpannableStringBuilder hightLightSpan = new SpannableStringBuilder(name);
        hightLightSpan.setSpan(new ForegroundColorSpan(highLightColor),
                span_start, span_end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        viewHolder.name.setText(hightLightSpan);

        freemeYellowPageUtils.showFreemeYellowPageNameAndLogo(context, number,
                null, null);
    }

    private String[] getFullPinQuery(String query, String chinese, String full_pin) {
        int queryLength = query.length();
        int index = full_pin.indexOf(query);
        int currentCount = 0;
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        int start = 0;
        char[] nameChar = chinese.toCharArray();
        for (int i = 0; i < nameChar.length; i++) {
            if (nameChar[i] > 128) {
                try {
                    String s = PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat)[0];
                    currentCount += s.length();
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                currentCount += 1;
            }
            if (currentCount > index) {
                start = i;
                break;
            } else if (currentCount == index) {
                start = i + 1;
                break;
            }
        }

        int currentCount1 = 0;
        String s1 = chinese.substring(start);
        int ll = 0;
        char[] s1Char = s1.toCharArray();
        for (int j = 0; j < s1Char.length; j++) {
            if (s1Char[j] > 128) {
                try {
                    String s = PinyinHelper.toHanyuPinyinStringArray(s1Char[j], defaultFormat)[0];
                    currentCount1 += s.length();
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                currentCount1 += 1;
            }

            if (currentCount1 >= queryLength) {
                ll = j + 1;
                break;
            }
        }

        return new String[]{String.valueOf(start), s1.substring(0, ll)};
    }

    private String[] getJianPinQuery(String query, String jian_pin) {
        int index = jian_pin.indexOf(query);
        return new String[]{String.valueOf(index), query};
    }
}
