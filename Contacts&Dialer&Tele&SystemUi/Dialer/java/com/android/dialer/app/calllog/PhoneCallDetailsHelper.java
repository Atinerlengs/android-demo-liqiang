/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.dialer.app.calllog;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.PersistableBundle;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.content.ContextCompat;
import android.telecom.PhoneAccount;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.dialer.app.R;
import com.android.dialer.app.calllog.calllogcache.CallLogCache;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.compat.AppCompatConstants;
import com.android.dialer.logging.ContactSource;
import com.android.dialer.oem.MotorolaUtils;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.util.DialerUtils;
import com.mediatek.dialer.compat.ContactsCompat.PhoneCompat;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.util.CallLogHighlighter;
import com.mediatek.dialer.util.DialerFeatureOptions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import mediatek.telephony.MtkCarrierConfigManager;

//*/ freeme.zhaozehong, 20180315. for freemeOS, UI redesign
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
//*/

/** Helper class to fill in the views in {@link PhoneCallDetailsViews}. */
public class PhoneCallDetailsHelper {

  /** The maximum number of icons will be shown to represent the call types in a group. */
  /*/ freeme.zhaozehong, 20180130. for freemeOS, UI redesign
  private static final int MAX_CALL_TYPE_ICONS = 3;
  /*/
  private static final int MAX_CALL_TYPE_ICONS = 1;
  //*/

  private final Context mContext;
  private final Resources mResources;
  private final CallLogCache mCallLogCache;
  /** Calendar used to construct dates */
  private final Calendar mCalendar;
  /** The injected current time in milliseconds since the epoch. Used only by tests. */
  private Long mCurrentTimeMillisForTest;

  private CharSequence mPhoneTypeLabelForTest;
  /** List of items to be concatenated together for accessibility descriptions */
  private ArrayList<CharSequence> mDescriptionItems = new ArrayList<>();
  ///M: For customization using carrier config
  private boolean mIsSupportCallPull = false;

  /**
   * Creates a new instance of the helper.
   *
   * <p>Generally you should have a single instance of this helper in any context.
   *
   * @param resources used to look up strings
   */
  public PhoneCallDetailsHelper(Context context, Resources resources, CallLogCache callLogCache) {
    mContext = context;
    mResources = resources;
    mCallLogCache = callLogCache;
    mCalendar = Calendar.getInstance();
    /// M: [Dialer Global Search] for CallLogSearch @{
    if (DialerFeatureOptions.DIALER_GLOBAL_SEARCH) {
      initHighlighter();
    }
    /// @}

    ///M: For customization using carrier config
    CarrierConfigManager configMgr =
        (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
    PersistableBundle carrierConfig =
         configMgr.getConfigForSubId(SubscriptionManager.getDefaultVoiceSubscriptionId());
    if (carrierConfig != null) {
        mIsSupportCallPull = carrierConfig.getBoolean(
          MtkCarrierConfigManager.MTK_KEY_DIALER_CALL_PULL_BOOL);
    }
    // Combine the count (if present) and the date.
  }

  /** Fills the call details views with content. */
  public void setPhoneCallDetails(PhoneCallDetailsViews views, PhoneCallDetails details) {
    // Display up to a given number of icons.
    views.callTypeIcons.clear();
    int count = details.callTypes.length;
    boolean isVoicemail = false;
    for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
      views.callTypeIcons.add(details.callTypes[index]);
      if (index == 0) {
        isVoicemail = details.callTypes[index] == Calls.VOICEMAIL_TYPE;
      }
    }

    // Show the video icon if the call had video enabled.
    views.callTypeIcons.setShowVideo(
        (details.features & Calls.FEATURES_VIDEO) == Calls.FEATURES_VIDEO);
    views.callTypeIcons.setShowHd(
        MotorolaUtils.shouldShowHdIconInCallLog(mContext, details.features));
    views.callTypeIcons.setShowWifi(
        MotorolaUtils.shouldShowWifiIconInCallLog(mContext, details.features));

    ///M: Plug-in call to show different icons VoLTE, VoWifi, ViWifi in call logs
    ExtensionManager.getCallLogExtension().setShowVolteWifi(views.callTypeIcons,
                                 details.features);

    /*/ freeme.zhaozehong, 20180329. for freemeOS, UI redesign
    views.callTypeIcons.requestLayout();
    views.callTypeIcons.setVisibility(View.VISIBLE);
    /*/
    if (views.callTypeIcons.isNeedShow()){
        views.callTypeIcons.requestLayout();
        views.callTypeIcons.setVisibility(View.VISIBLE);
    } else {
        views.callTypeIcons.setVisibility(View.GONE);
    }
    //*/

    // Show the total call count only if there are more than the maximum number of icons.
    final Integer callCount;
    if (count > MAX_CALL_TYPE_ICONS) {
      callCount = count;
    } else {
      callCount = null;
    }

    // Set the call count, location, date and if voicemail, set the duration.
    setDetailText(views, callCount, details);

    // Set the account label if it exists.
    String accountLabel = mCallLogCache.getAccountLabel(details.accountHandle);
    if (!TextUtils.isEmpty(details.viaNumber)) {
      if (!TextUtils.isEmpty(accountLabel)) {
        accountLabel =
            mResources.getString(
                R.string.call_log_via_number_phone_account, accountLabel, details.viaNumber);
      } else {
        accountLabel = mResources.getString(R.string.call_log_via_number, details.viaNumber);
      }
    }
    if (!TextUtils.isEmpty(accountLabel)) {
      views.callAccountLabel.setVisibility(View.VISIBLE);
      views.callAccountLabel.setText(accountLabel);
      int color = mCallLogCache.getAccountColor(details.accountHandle);
      if (color == PhoneAccount.NO_HIGHLIGHT_COLOR) {
        int defaultColor = R.color.dialer_secondary_text_color;
        views.callAccountLabel.setTextColor(mContext.getResources().getColor(defaultColor));
      } else {
        views.callAccountLabel.setTextColor(color);
      }
    } else {
      views.callAccountLabel.setVisibility(View.GONE);
    }

    CharSequence nameText;
    final CharSequence displayNumber = details.displayNumber;
    if (TextUtils.isEmpty(details.getPreferredName())) {
      nameText = displayNumber;
      // We have a real phone number as "nameView" so make it always LTR
      views.nameView.setTextDirection(View.TEXT_DIRECTION_LTR);
    } else {
      nameText = details.getPreferredName();
    }

    /// M: [Dialer Global Search]for CallLog Search @{
    if (DialerFeatureOptions.DIALER_GLOBAL_SEARCH && mHighlightString != null
        && mHighlightString.length > 0) {
      boolean onlyNumber = TextUtils.isEmpty(details.getPreferredName());
      nameText = getHightlightedCallLogName(nameText.toString(),
          mHighlightString, onlyNumber);
    }
    /// @}

    views.nameView.setText(nameText);

    if (isVoicemail) {
      int relevantLinkTypes = Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS | Linkify.WEB_URLS;
      views.voicemailTranscriptionView.setAutoLinkMask(relevantLinkTypes);
      views.voicemailTranscriptionView.setText(
          TextUtils.isEmpty(details.transcription) ? null : details.transcription);
    }

    // Bold if not read
    /*/ freeme.liqiang, 20180331. set text color red when rejected or missed
    Typeface typeface = details.isRead ? Typeface.SANS_SERIF : Typeface.DEFAULT_BOLD;
    views.nameView.setTypeface(typeface);
    views.voicemailTranscriptionView.setTypeface(typeface);
    views.callLocationAndDate.setTypeface(typeface);
    views.callLocationAndDate.setTextColor(
        ContextCompat.getColor(
            mContext,
            details.isRead ? R.color.call_log_detail_color : R.color.call_log_unread_text_color));
    /*/
    int textColor = ContextCompat.getColor(mContext,
            details.isRead ? R.color.freeme_list_item_title_text_color : R.color.freeme_missed_call_font_color);
    views.voicemailTranscriptionView.setTextColor(textColor);
    views.nameView.setTextColor(textColor);
    //*/

    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    setPhoneCallDetailsExtra(views, details, callCount, accountLabel,
            getSimSlot(details.accountHandle));
    //*/
  }

  /**
   * Builds a string containing the call location and date. For voicemail logs only the call date is
   * returned because location information is displayed in the call action button
   *
   * @param details The call details.
   * @return The call location and date string.
   */
  public CharSequence getCallLocationAndDate(PhoneCallDetails details) {
    mDescriptionItems.clear();

    if (details.callTypes[0] != Calls.VOICEMAIL_TYPE) {
      // Get type of call (ie mobile, home, etc) if known, or the caller's location.
      CharSequence callTypeOrLocation = getCallTypeOrLocation(details);

      // Only add the call type or location if its not empty.  It will be empty for unknown
      // callers.
      /** M: [VoLTE ConfCallLog] the conference call only show date not show location or label  */
      if (!TextUtils.isEmpty(callTypeOrLocation) && details.conferenceId <= 0) {
        mDescriptionItems.add(callTypeOrLocation);
      }
    }

    // The date of this call
    mDescriptionItems.add(getCallDate(details));

    // Create a comma separated list from the call type or location, and call date.
    return DialerUtils.join(mDescriptionItems);
  }

  /**
   * For a call, if there is an associated contact for the caller, return the known call type (e.g.
   * mobile, home, work). If there is no associated contact, attempt to use the caller's location if
   * known.
   *
   * @param details Call details to use.
   * @return Type of call (mobile/home) if known, or the location of the caller (if known).
   */
  public CharSequence getCallTypeOrLocation(PhoneCallDetails details) {
    if (details.isSpam) {
      return mResources.getString(R.string.spam_number_call_log_label);
    } else if (details.isBlocked) {
      return mResources.getString(R.string.blocked_number_call_log_label);
    }

    CharSequence numberFormattedLabel = null;
    // Only show a label if the number is shown and it is not a SIP address.
    if (!TextUtils.isEmpty(details.number)
        && !PhoneNumberHelper.isUriNumber(details.number.toString())
        && !mCallLogCache.isVoicemailNumber(details.accountHandle, details.number)) {

      if (shouldShowLocation(details)) {
        numberFormattedLabel = details.geocode;
      } else if (!(details.numberType == Phone.TYPE_CUSTOM
          && TextUtils.isEmpty(details.numberLabel))) {
        // Get type label only if it will not be "Custom" because of an empty number label.
        numberFormattedLabel =
            mPhoneTypeLabelForTest != null
                ? mPhoneTypeLabelForTest
                : PhoneCompat.getTypeLabel(mContext, details.numberType, details.numberLabel);
      }
    }

    if (!TextUtils.isEmpty(details.namePrimary) && TextUtils.isEmpty(numberFormattedLabel)) {
      numberFormattedLabel = details.displayNumber;
    }
    return numberFormattedLabel;
  }

  /** Returns true if primary name is empty or the data is from Cequint Caller ID. */
  private static boolean shouldShowLocation(PhoneCallDetails details) {
    if (TextUtils.isEmpty(details.geocode)) {
      return false;
    }
    // For caller ID provided by Cequint we want to show the geo location.
    if (details.sourceType == ContactSource.Type.SOURCE_TYPE_CEQUINT_CALLER_ID) {
      return true;
    }
    // Don't bother showing geo location for contacts.
    if (!TextUtils.isEmpty(details.namePrimary)) {
      return false;
    }
    return true;
  }

  public void setPhoneTypeLabelForTest(CharSequence phoneTypeLabel) {
    this.mPhoneTypeLabelForTest = phoneTypeLabel;
  }

  /**
   * Get the call date/time of the call. For the call log this is relative to the current time. e.g.
   * 3 minutes ago. For voicemail, see {@link #getGranularDateTime(PhoneCallDetails)}
   *
   * @param details Call details to use.
   * @return String representing when the call occurred.
   */
  public CharSequence getCallDate(PhoneCallDetails details) {
    if (details.callTypes[0] == Calls.VOICEMAIL_TYPE) {
      return getGranularDateTime(details);
    }

    return DateUtils.getRelativeTimeSpanString(
        details.date,
        getCurrentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE);
  }

  /**
   * Get the granular version of the call date/time of the call. The result is always in the form
   * 'DATE at TIME'. The date value changes based on when the call was created.
   *
   * <p>If created today, DATE is 'Today' If created this year, DATE is 'MMM dd' Otherwise, DATE is
   * 'MMM dd, yyyy'
   *
   * <p>TIME is the localized time format, e.g. 'hh:mm a' or 'HH:mm'
   *
   * @param details Call details to use
   * @return String representing when the call occurred
   */
  public CharSequence getGranularDateTime(PhoneCallDetails details) {
    return mResources.getString(
        R.string.voicemailCallLogDateTimeFormat,
        getGranularDate(details.date),
        DateUtils.formatDateTime(mContext, details.date, DateUtils.FORMAT_SHOW_TIME));
  }

  /**
   * Get the granular version of the call date. See {@link #getGranularDateTime(PhoneCallDetails)}
   */
  private String getGranularDate(long date) {
    if (DateUtils.isToday(date)) {
      return mResources.getString(R.string.voicemailCallLogToday);
    }
    return DateUtils.formatDateTime(
        mContext,
        date,
        DateUtils.FORMAT_SHOW_DATE
            | DateUtils.FORMAT_ABBREV_MONTH
            | (shouldShowYear(date) ? DateUtils.FORMAT_SHOW_YEAR : DateUtils.FORMAT_NO_YEAR));
  }

  /**
   * Determines whether the year should be shown for the given date
   *
   * @return {@code true} if date is within the current year, {@code false} otherwise
   */
  private boolean shouldShowYear(long date) {
    mCalendar.setTimeInMillis(getCurrentTimeMillis());
    int currentYear = mCalendar.get(Calendar.YEAR);
    mCalendar.setTimeInMillis(date);
    return currentYear != mCalendar.get(Calendar.YEAR);
  }

  /** Sets the text of the header view for the details page of a phone call. */
  public void setCallDetailsHeader(TextView nameView, PhoneCallDetails details) {
    final CharSequence nameText;
    if (!TextUtils.isEmpty(details.namePrimary)) {
      nameText = details.namePrimary;
    } else if (!TextUtils.isEmpty(details.displayNumber)) {
      nameText = details.displayNumber;
    } else {
      nameText = mResources.getString(R.string.unknown);
    }

    nameView.setText(nameText);
  }

  public void setCurrentTimeForTest(long currentTimeMillis) {
    mCurrentTimeMillisForTest = currentTimeMillis;
  }

  /**
   * Returns the current time in milliseconds since the epoch.
   *
   * <p>It can be injected in tests using {@link #setCurrentTimeForTest(long)}.
   */
  private long getCurrentTimeMillis() {
    if (mCurrentTimeMillisForTest == null) {
      return System.currentTimeMillis();
    } else {
      return mCurrentTimeMillisForTest;
    }
  }

  /** Sets the call count, date, and if it is a voicemail, sets the duration. */
  private void setDetailText(
      PhoneCallDetailsViews views, Integer callCount, PhoneCallDetails details) {
    /*/ freeme.zhaozehong, 20180130. for freemeOS, UI redesign
    CharSequence dateText = details.callLocationAndDate;
    final CharSequence text;
    if (callCount != null) {
      text = mResources.getString(R.string.call_log_item_count_and_date, callCount, dateText);
    } else {
      text = dateText;
    }
    /*/
    String geoCode = details.geocode;
    if (TextUtils.isEmpty(geoCode)) {
      geoCode = mResources.getString(R.string.freeme_geo_unknown_city);
    }
    final CharSequence text = geoCode;
    //*/

    if (details.callTypes[0] == Calls.VOICEMAIL_TYPE && details.duration > 0) {
      views.callLocationAndDate.setText(
          mResources.getString(
              R.string.voicemailCallLogDateTimeFormatWithDuration,
              text,
              getVoicemailDuration(details)));
    } else {
      views.callLocationAndDate.setText(text);
      /// M: For operator, to check for call pull @{
      if (mIsSupportCallPull) {
          CharSequence detailText = null;
          Log.d("PhoneCallDetailsHelper", "Call pull supported");
          if (details.callTypes[0] == CallLogActivity.DECLINED_EXTERNAL_TYPE) {
                detailText = mContext.getString(R.string.declined) + ", " + text;
                views.callLocationAndDate.setText(detailText);
          } else if (details.callTypes[0] == Calls.ANSWERED_EXTERNALLY_TYPE) {
                detailText = mContext.getString(R.string.answered_remotely) + ", " + text;
                views.callLocationAndDate.setText(detailText);
          } else if (details.callTypes[0] == CallLogActivity.INCOMING_PULLED_AWAY_TYPE
                        || details.callTypes[0] == CallLogActivity.OUTGOING_PULLED_AWAY_TYPE) {
                detailText = mContext.getString(R.string.call_pulled_away) + ", " + text;
                views.callLocationAndDate.setText(detailText);
          } else if (details.callTypes[0] == AppCompatConstants.CALLS_REJECTED_TYPE) {
                detailText = mContext.getString(R.string.call_rejected) + ", " + text;
                views.callLocationAndDate.setText(detailText);
          }
       }
       ///}@
    }
  }

  private String getVoicemailDuration(PhoneCallDetails details) {
    long minutes = TimeUnit.SECONDS.toMinutes(details.duration);
    long seconds = details.duration - TimeUnit.MINUTES.toSeconds(minutes);
    if (minutes > 99) {
      minutes = 99;
    }
    return mResources.getString(R.string.voicemailDurationFormat, minutes, seconds);
  }

  /// M: [Dialer Global Search] for CallLog search @{
  private CallLogHighlighter mHighlighter;
  private char[] mHighlightString;

  private void initHighlighter() {
    mHighlighter = new CallLogHighlighter(Color.GREEN);
  }

  public void setHighlightedText(char[] highlightedText) {
    mHighlightString = highlightedText;
  }

  private String getHightlightedCallLogName(String text, char[] highlightText,
      boolean isOnlyNumber) {
    String name = text;
    if (isOnlyNumber) {
      name = mHighlighter.applyNumber(text, highlightText).toString();
    } else {
      name = mHighlighter.applyName(text, highlightText).toString();
    }
    return name;
  }
  /// @}

    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    private void setPhoneCallDetailsExtra(PhoneCallDetailsViews views, PhoneCallDetails details,
                                          Integer callCount, String accountLabel, int simSlot) {
        if (callCount != null && callCount > 1) {
            views.mCallCount.setVisibility(View.VISIBLE);
            views.mCallCount.setText(mResources.getString(R.string.call_log_item_count_and_date,
                    callCount, ""));
        } else {
            views.mCallCount.setVisibility(View.GONE);
        }

        int callTypeDrawable;
        switch (details.callTypes[0]) {
            case Calls.INCOMING_TYPE:
            case Calls.ANSWERED_EXTERNALLY_TYPE:
                callTypeDrawable = R.drawable.freeme_call_log_type_in;
                break;
            case Calls.OUTGOING_TYPE:
                callTypeDrawable = R.drawable.freeme_call_log_type_out;
                break;
            case Calls.MISSED_TYPE:
                callTypeDrawable = R.drawable.freeme_call_log_type_missed;
                break;
            case Calls.VOICEMAIL_TYPE:
                callTypeDrawable = R.drawable.freeme_call_log_type_voicemail;
                break;
            case Calls.BLOCKED_TYPE:
                callTypeDrawable = R.drawable.freeme_call_log_type_block;
                break;
            default:
                // It is possible for users to end up with calls with unknown call types in their
                // call history, possibly due to 3rd party call log implementations (e.g. to
                // distinguish between rejected and missed calls). Instead of crashing, just
                // assume that all unknown call types are missed calls.
                callTypeDrawable = R.drawable.freeme_call_log_type_missed;
                break;
        }
        views.mCallType.setImageResource(callTypeDrawable);

        if (!TextUtils.isEmpty(accountLabel)) {
            views.mSimIcon.setVisibility(View.VISIBLE);
            if (simSlot <= 0) {
                views.mSimIcon.setImageResource(R.drawable.freeme_call_log_sim1_icon);
            } else {
                views.mSimIcon.setImageResource(R.drawable.freeme_call_log_sim2_icon);
            }
        } else {
            views.mSimIcon.setVisibility(View.GONE);
        }

        boolean hasMark = !TextUtils.isEmpty(details.callMark);
        if (hasMark) {
            if (views.mCallMark == null && views.mCallMarkStub != null) {
                View markView = views.mCallMarkStub.inflate();
                if (markView instanceof TextView) {
                    views.mCallMark = (TextView) markView;
                }
            }
            if (views.mCallMark != null) {
                views.mCallMark.setVisibility(View.VISIBLE);
                views.mCallMark.setText(details.callMark);
            }
        } else {
            if (views.mCallMark != null) {
                views.mCallMark.setVisibility(View.GONE);
            }
        }

        views.mCallDate.setText(details.callDate);
    }

    private int getSimSlot(PhoneAccountHandle accountHandle) {
        TelecomManager telecomManager = TelecomManager.from(mContext);
        TelephonyManager telephonyManager = TelephonyManager.from(mContext);
        PhoneAccount phoneAccount = telecomManager.getPhoneAccount(accountHandle);
        int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
        SubscriptionInfo info = SubscriptionManager.from(mContext).getActiveSubscriptionInfo(subId);
        return info == null ? SubscriptionManager.INVALID_SIM_SLOT_INDEX : info.getSimSlotIndex();
    }
    //*/
}
