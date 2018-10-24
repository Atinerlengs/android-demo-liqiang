/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.dialer.app.list;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.format.TextHighlighter;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.PhoneNumberListAdapter;
import com.android.contacts.common.util.ContactDisplayUtils;
import com.android.dialer.app.R;
import com.android.dialer.location.GeoUtil;
import com.android.dialer.app.calllog.calllogcache.CallLogCache;
import com.android.dialer.calllogutils.CallTypeIconsView;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.calllogutils.PhoneNumberDisplayUtil;
import com.android.dialer.phonenumbercache.ContactInfoHelper;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.util.CallUtil;
import com.mediatek.dialer.compat.ContactsCompat.PhoneCompat;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.search.DialerSearchHelper.DialerSearch;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.DialerSearchUtils;

/**
 * {@link PhoneNumberListAdapter} with the following added shortcuts, that are displayed as list
 * items: 1) Directly calling the phone number query 2) Adding the phone number query to a contact
 *
 * <p>These shortcuts can be enabled or disabled to toggle whether or not they show up in the list.
 */
public class DialerPhoneNumberListAdapter extends PhoneNumberListAdapter {

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

  public DialerPhoneNumberListAdapter(Context context) {
    super(context);

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

  /** @return The number of enabled shortcuts. Ranges from 0 to a maximum of SHORTCUT_COUNT */
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
  @Override
  protected ContactListItemView newView(
      Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
    final ContactListItemView view = super.newView(context, partition, cursor, position, parent);

    view.setSupportVideoCallIcon(mIsImsVideoEnabled);
    return view;
  }*/

  /**
   * @param position The position of the item
   * @return The enabled shortcut type matching the given position if the item is a shortcut, -1
   *     otherwise
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
    final int shortcutType = getShortcutTypeFromPosition(position);
    if (shortcutType >= 0) {
      return true;
    } else {
      return super.isEnabled(position);
    }
  }

  private void assignShortcutToView(ContactListItemView v, int shortcutType) {
    final CharSequence text;
    final Drawable drawable;
    final Resources resources = getContext().getResources();
    final String number = getFormattedQueryString();
    switch (shortcutType) {
      case SHORTCUT_DIRECT_CALL:
        text =
            ContactDisplayUtils.getTtsSpannedPhoneNumber(
                resources,
                R.string.search_shortcut_call_number,
                mBidiFormatter.unicodeWrap(number, TextDirectionHeuristics.LTR));
        drawable = ContextCompat.getDrawable(getContext(), R.drawable.quantum_ic_call_vd_theme_24);
        break;
      case SHORTCUT_CREATE_NEW_CONTACT:
        text = resources.getString(R.string.search_shortcut_create_new_contact);
        drawable =
            ContextCompat.getDrawable(getContext(), R.drawable.quantum_ic_person_add_vd_theme_24);
        drawable.setAutoMirrored(true);
        break;
      case SHORTCUT_ADD_TO_EXISTING_CONTACT:
        text = resources.getString(R.string.search_shortcut_add_to_contact);
        drawable =
            ContextCompat.getDrawable(getContext(), R.drawable.quantum_ic_person_add_vd_theme_24);
        break;
      case SHORTCUT_SEND_SMS_MESSAGE:
        text = resources.getString(R.string.search_shortcut_send_sms_message);
        drawable =
            ContextCompat.getDrawable(getContext(), R.drawable.quantum_ic_message_vd_theme_24);
        break;
      case SHORTCUT_MAKE_VIDEO_CALL:
        text = resources.getString(R.string.search_shortcut_make_video_call);
        drawable =
            ContextCompat.getDrawable(getContext(), R.drawable.quantum_ic_videocam_vd_theme_24);
        break;
      case SHORTCUT_BLOCK_NUMBER:
        text = resources.getString(R.string.search_shortcut_block_number);
        drawable =
            ContextCompat.getDrawable(getContext(), R.drawable.ic_not_interested_googblue_24dp);
        break;
      default:
        throw new IllegalArgumentException("Invalid shortcut type");
    }
    v.setDrawable(drawable);
    v.setDisplayName(text);
    v.setAdjustSelectionBoundsEnabled(false);

    /// M: for Plug-in to customize the view. @{
    ExtensionManager.getDialPadExtension()
          .customizeDialerOptions(v, shortcutType, number);
    ///@}

  }

  /** @return True if the shortcut state (disabled vs enabled) was changed by this operation */
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
  private final String TAG = "DialerPhoneNumberListAdapter";

  private final int VIEW_TYPE_UNKNOWN = -1;
  private final int VIEW_TYPE_CONTACT = 0;
  private final int VIEW_TYPE_CALL_LOG = 1;

  private final int NUMBER_TYPE_NORMAL = 0;
  private final int NUMBER_TYPE_UNKNOWN = 1;
  private final int NUMBER_TYPE_VOICEMAIL = 2;
  private final int NUMBER_TYPE_PRIVATE = 3;
  private final int NUMBER_TYPE_PAYPHONE = 4;
  private final int NUMBER_TYPE_EMERGENCY = 5;

  private final int DS_MATCHED_DATA_INIT_POS    = 3;
  private final int DS_MATCHED_DATA_DIVIDER     = 3;

  public final int NAME_LOOKUP_ID_INDEX        = 0;
  public final int CONTACT_ID_INDEX            = 1;
  public final int DATA_ID_INDEX               = 2;
  public final int CALL_LOG_DATE_INDEX         = 3;
  public final int CALL_LOG_ID_INDEX           = 4;
  public final int CALL_TYPE_INDEX             = 5;
  public final int CALL_GEOCODED_LOCATION_INDEX = 6;
  public final int PHONE_ACCOUNT_ID_INDEX                = 7;
  public final int PHONE_ACCOUNT_COMPONENT_NAME_INDEX     = 8;
  public final int PRESENTATION_INDEX          = 9;
  public final int INDICATE_PHONE_SIM_INDEX    = 10;
  public final int CONTACT_STARRED_INDEX       = 11;
  public final int PHOTO_ID_INDEX              = 12;
  public final int SEARCH_PHONE_TYPE_INDEX     = 13;
  public final int SEARCH_PHONE_LABEL_INDEX    = 14;
  public final int NAME_INDEX                  = 15;
  public final int SEARCH_PHONE_NUMBER_INDEX   = 16;
  public final int CONTACT_NAME_LOOKUP_INDEX   = 17;
  public final int IS_SDN_CONTACT              = 18;
  public final int DS_MATCHED_DATA_OFFSETS     = 19;
  public final int DS_MATCHED_NAME_OFFSETS     = 20;

  private ContactPhotoManager mContactPhotoManager;
  private final CallLogCache mPhoneNumberUtils;
  private PhoneNumberDisplayUtil mPhoneNumberHelper;

  private String mUnknownNumber;
  private String mPrivateNumber;
  private String mPayphoneNumber;

  private String mVoiceMail;

  private HashMap<Integer, Drawable> mCallTypeDrawables = new HashMap<Integer, Drawable>();

  private TextHighlighter mTextHighlighter;

  /**
   * M: bind view for mediatek's search UI.
   * @see com.android.contacts.common.list.PhoneNumberListAdapter
   * #bindView(android.view.View, int, android.database.Cursor, int)
   */
  @Override
  protected void bindView(View itemView, int partition, Cursor cursor, int position) {
    /*/ freeme.zhaozehong, 20180205. for freemeOS, UI redesign
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
      default:
        break;
    }
    //*/
  }

  /**
   * M: create item view for this feature
   * @see com.android.contacts.common.list.PhoneNumberListAdapter
   * #newView(android.content.Context, int, android.database.Cursor, int, android.view.ViewGroup)
   */
  @Override
  protected View newView(Context context, int partition, Cursor cursor, int position,
      ViewGroup parent) {
    /*/ freeme.zhaozehong, 20180205. for freemeOS, UI redesign
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

    View view = View.inflate(context, R.layout.mtk_dialer_search_item_view, null);
    TypedArray a = context.obtainStyledAttributes(null, R.styleable.ContactListItemView);

    view.setPadding(
        a.getDimensionPixelOffset(R.styleable.ContactListItemView_list_item_padding_left, 0),
        a.getDimensionPixelOffset(R.styleable.ContactListItemView_list_item_padding_top, 0),
        a.getDimensionPixelOffset(R.styleable.ContactListItemView_list_item_padding_right, 0),
        a.getDimensionPixelOffset(R.styleable.ContactListItemView_list_item_padding_bottom, 0));

    ViewHolder viewHolder = new ViewHolder();

    viewHolder.quickContactBadge = (QuickContactBadge) view.findViewById(R.id.quick_contact_photo);
    viewHolder.name = (TextView) view.findViewById(R.id.name);
    viewHolder.labelAndNumber = (TextView) view.findViewById(R.id.labelAndNumber);
    viewHolder.callInfo = (View) view.findViewById(R.id.call_info);
    viewHolder.callType = (ImageView) view.findViewById(R.id.callType);
    viewHolder.address = (TextView) view.findViewById(R.id.address);
    viewHolder.date = (TextView) view.findViewById(R.id.date);

    viewHolder.accountLabel = (TextView) view.findViewById(R.id.call_account_label);

    view.setTag(viewHolder);
    ///M: fix for ALPS03385579 (coding defect){
    a.recycle();
    /// @}
    return view;
    /*/
    return null;
    //*/
  }

  /**
   * M: init UI resources
   * @param context
   */
  private void initResources(Context context) {
    mContactPhotoManager = ContactPhotoManager.getInstance(context);
    mPhoneNumberHelper = new PhoneNumberDisplayUtil();

    mVoiceMail = context.getResources().getString(R.string.voicemail);
    mPrivateNumber = context.getResources().getString(R.string.private_num_non_verizon);
    mPayphoneNumber = context.getResources().getString(R.string.payphone);
    mUnknownNumber = context.getResources().getString(R.string.unknown);

    // 1. incoming 2. outgoing 3. missed 4.voicemail
    // Align drawables of result items in dialer search to AOSP style.
    CallTypeIconsView.Resources resources = new CallTypeIconsView.Resources(context, false);
    mCallTypeDrawables.put(Calls.INCOMING_TYPE, resources.incoming);
    mCallTypeDrawables.put(Calls.OUTGOING_TYPE, resources.outgoing);
    mCallTypeDrawables.put(Calls.MISSED_TYPE, resources.missed);
    mCallTypeDrawables.put(Calls.VOICEMAIL_TYPE, resources.voicemail);
    /// M: Add reject icon
    mCallTypeDrawables.put(Calls.REJECTED_TYPE, resources.missed);
    /// M: for Plug-in add auto reject icon for dialer search @{
    ExtensionManager.getCallLogExtension().addResourceForDialerSearch(
            mCallTypeDrawables, false, context);
    /// @}
  }

  /**
   * M: calculate view's type from cursor
   * @param cursor
   * @return type number
   */
  private int getViewType(Cursor cursor) {
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
   * @param view
   * @param context
   * @param cursor
   */
  private void bindContactView(View view, Context context, Cursor cursor) {

    final ViewHolder viewHolder = (ViewHolder) view.getTag();

    viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
    viewHolder.callInfo.setVisibility(View.GONE);
    viewHolder.accountLabel.setVisibility(View.GONE);

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
    if (!(labelType == Phone.TYPE_CUSTOM && TextUtils.isEmpty(label))) {
      /// M: Using new API for AAS phone number label lookup
      label = PhoneCompat.getTypeLabel(mContext, labelType, label);
    }
    final CharSequence displayName = cursor.getString(NAME_INDEX);

    Uri contactUri = getContactUri(cursor);
    Log.d(TAG, "bindContactView, contactUri: " + contactUri);

    long photoId = cursor.getLong(PHOTO_ID_INDEX);

    if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
      photoId = 0;
      viewHolder.quickContactBadge.assignContactUri(null);
    } else {
      viewHolder.quickContactBadge.assignContactUri(contactUri);
    }
    viewHolder.quickContactBadge.setOverlay(null);

    if (photoId > 0) {
      mContactPhotoManager.loadThumbnail(viewHolder.quickContactBadge, photoId, false, true, null);
    } else {
      String identifier = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
      DefaultImageRequest request = new DefaultImageRequest((String) displayName, identifier, true);
      if (subId > 0) {
        request.subId = subId;
        request.photoId = cursor.getInt(IS_SDN_CONTACT);
      }
      mContactPhotoManager.loadThumbnail(viewHolder.quickContactBadge, photoId, false, true,
          request);
    }

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

      /// M: add for plug-in @{
      ExtensionManager.getDialerSearchExtension()
              .removeCallAccountForDialerSearch(context, view);
      /// @}

  }

  /**
   * M: Bind call log view by cursor data
   * @param view
   * @param context
   * @param cursor
   */
  private void bindCallLogView(View view, Context context, Cursor cursor) {
    final ViewHolder viewHolder = (ViewHolder) view.getTag();

    viewHolder.callInfo.setVisibility(View.VISIBLE);
    viewHolder.labelAndNumber.setVisibility(View.GONE);

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

    final int type = cursor.getInt(CALL_TYPE_INDEX);
    final long date = cursor.getLong(CALL_LOG_DATE_INDEX);
    final int indicate = cursor.getInt(INDICATE_PHONE_SIM_INDEX);
    String geocode = cursor.getString(CALL_GEOCODED_LOCATION_INDEX);

    // create a temp contact uri for quick contact view.
    Uri contactUri = null;
    if (!TextUtils.isEmpty(number)) {
      contactUri = ContactInfoHelper.createTemporaryContactUri(number);
    }

    int contactType = ContactPhotoManager.TYPE_DEFAULT;
    if (numberType == NUMBER_TYPE_VOICEMAIL) {
      contactType = ContactPhotoManager.TYPE_VOICEMAIL;
      contactUri = null;
    }

    viewHolder.quickContactBadge.assignContactUri(contactUri);
    viewHolder.quickContactBadge.setOverlay(null);

    /// M: [ALPS01963857] keep call log and smart search's avatar in same color. @{
    boolean isVoiceNumber = mPhoneNumberUtils.isVoicemailNumber(accountHandle, number);
    String nameForDefaultImage = mPhoneNumberHelper.getDisplayNumber(context, number,
    /// M: [N Conflict Change]TODO:: POST_DIAL_DIGITS colum maybe need add in
    // DialerSearch table. here just set "" for build pass.
        presentation, number, "", isVoiceNumber).toString();
    /// @}

    String identifier = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
    DefaultImageRequest request = new DefaultImageRequest(nameForDefaultImage, identifier,
        contactType, true);
    mContactPhotoManager.loadThumbnail(viewHolder.quickContactBadge, 0, false, true, request);

    viewHolder.address.setText(geocode);

    if (isSpecialNumber(numberType)) {
      if (numberType == NUMBER_TYPE_VOICEMAIL) {
        viewHolder.name.setText(mVoiceMail);
        String highlight = getNumberHighlight(cursor);
        if (!TextUtils.isEmpty(highlight)) {
          SpannableStringBuilder style = highlightHyphen(highlight, formattedNumber, number);
          viewHolder.address.setText(style);
        } else {
          viewHolder.address.setText(formattedNumber);
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

    java.text.DateFormat dateFormat = DateFormat.getTimeFormat(context);
    String dateString = dateFormat.format(date);
    viewHolder.date.setText(dateString);

    viewHolder.callType.setImageDrawable(mCallTypeDrawables.get(type));

    final String accountLabel = PhoneAccountUtils.getAccountLabel(context, accountHandle);

    if (!TextUtils.isEmpty(accountLabel)) {
      viewHolder.accountLabel.setText(accountLabel);
      /// M: [ALPS02038899] set visible in case of gone
      viewHolder.accountLabel.setVisibility(View.VISIBLE);
      // Set text color for the corresponding account.
      int color = PhoneAccountUtils.getAccountColor(context, accountHandle);
      if (color == PhoneAccount.NO_HIGHLIGHT_COLOR) {
        int defaultColor = R.color.dialtacts_secondary_text_color;
        viewHolder.accountLabel.setTextColor(context.getResources().getColor(defaultColor));
      } else {
        viewHolder.accountLabel.setTextColor(color);
      }
    } else {
      viewHolder.accountLabel.setVisibility(View.GONE);
    }

    /// M: add for plug-in @{
    ExtensionManager.getDialerSearchExtension()
            .setCallAccountForDialerSearch(context, view, accountHandle);
    /// @}

  }

  private int getNumberType(PhoneAccountHandle accountHandle, CharSequence number,
          int presentation) {
    int type = NUMBER_TYPE_NORMAL;
    if (presentation == Calls.PRESENTATION_UNKNOWN) {
      type = NUMBER_TYPE_UNKNOWN;
    } else if (presentation == Calls.PRESENTATION_RESTRICTED) {
      type = NUMBER_TYPE_PRIVATE;
    } else if (presentation == Calls.PRESENTATION_PAYPHONE) {
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
    return Contacts.getLookupUri(contactId, lookup);
  }

  private boolean isSpecialNumber(int type) {
    return type != NUMBER_TYPE_NORMAL;
  }

  /**
   * M: highlight search result string
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
      style.setSpan(new StyleSpan(Typeface.BOLD), numberHighlightOffset.get(0),
          numberHighlightOffset.get(1) + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return style;
  }

  private String getNameHighlight(Cursor cursor) {
    final int index = cursor.getColumnIndex(DialerSearch.MATCHED_NAME_OFFSET);
    return index != -1 ? cursor.getString(index) : null;
  }

  private boolean isRegularSearch(Cursor cursor) {
    final int index = cursor.getColumnIndex(DialerSearch.MATCHED_DATA_OFFSET);
    String regularSearch = (index != -1 ? cursor.getString(index) : null);
    Log.d(TAG, "" + regularSearch);

    return Boolean.valueOf(regularSearch);
  }

  private String getNumberHighlight(Cursor cursor) {
    final int index = cursor.getColumnIndex(DialerSearch.MATCHED_DATA_OFFSET);
    return index != -1 ? cursor.getString(index) : null;
  }

  /**
   * M: set label and number to view
   * @param view
   * @param label
   * @param number
   */
  private void setLabelAndNumber(TextView view, CharSequence label,
          SpannableStringBuilder number) {
    if (PhoneNumberHelper.isUriNumber(number.toString())) {
      view.setText(number);
      return;
    }
    label = mBidiFormatter.unicodeWrap(label,
                      TextDirectionHeuristics.FIRSTSTRONG_LTR);
    if (TextUtils.isEmpty(label)) {
      view.setText(number);
    } else if (TextUtils.isEmpty(number)) {
      view.setText(label);
    } else {
      number.insert(0, label + " ");
      view.setText(number);
    }
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
    public QuickContactBadge quickContactBadge;
    public TextView name;
    public TextView labelAndNumber;
    public View callInfo;
    public ImageView callType;
    public TextView address;
    public TextView date;
    public TextView accountLabel;
  }

  /**
   * M: Fix ALPS01398152, Support RTL display for Arabic/Hebrew/Urdu
   * @param origin
   * @return
   */
  private String numberLeftToRight(String origin) {
    return TextUtils.isEmpty(origin) ? origin : '\u202D' + origin + '\u202C';
  }
  /// @}

//    /**
//     * M: use this method instead of CallUtil.isVideoEnable, give a chance to modify
//     * the value. such as plug-in customization.
//     * @return
//     */
//    boolean isVideoEnabled(String query) {
//        return ExtensionManager.getInstance().getDialPadExtension().isVideoButtonEnabled(
//                CallUtil.isVideoEnabled(getContext()), PhoneNumberUtils.normalizeNumber(query),
//                null);
//  }
  /// M: Mediatek end.
}
