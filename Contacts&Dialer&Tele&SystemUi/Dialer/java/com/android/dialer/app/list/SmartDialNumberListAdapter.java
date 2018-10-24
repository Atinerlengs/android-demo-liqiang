/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.common.list.ContactListItemView;
import com.android.dialer.app.dialpad.SmartDialCursorLoader;
import com.android.dialer.common.LogUtil;
import com.android.dialer.smartdial.SmartDialMatchPosition;
import com.android.dialer.smartdial.SmartDialNameMatcher;
import com.android.dialer.smartdial.SmartDialPrefix;
import com.android.dialer.util.CallUtil;
import com.mediatek.dialer.search.DialerSearchCursorLoader;
import com.mediatek.dialer.util.DialerFeatureOptions;
//*/ freeme.zhaozehong, 20180205. for freemeOS, UI redesign
import com.freeme.dialer.app.list.FreemeDialerPhoneNumberListAdapter;
//*/

import java.util.ArrayList;

/** List adapter to display the SmartDial search results. */
/*/ freeme.zhaozehong, 20180205. for freemeOS, UI redesign
public class SmartDialNumberListAdapter extends DialerPhoneNumberListAdapter {
/*/
public class SmartDialNumberListAdapter extends FreemeDialerPhoneNumberListAdapter {
//*/

  private static final String TAG = SmartDialNumberListAdapter.class.getSimpleName();
  private static final boolean DEBUG = false;

  @NonNull private final SmartDialNameMatcher mNameMatcher;

  public SmartDialNumberListAdapter(Context context) {
    super(context);
    mNameMatcher = new SmartDialNameMatcher("", SmartDialPrefix.getMap());
    setShortcutEnabled(SmartDialNumberListAdapter.SHORTCUT_DIRECT_CALL, false);

    if (DEBUG) {
      Log.v(TAG, "Constructing List Adapter");
    }
  }

  /** Sets query for the SmartDialCursorLoader. */
  public void configureLoader(SmartDialCursorLoader loader) {
    if (DEBUG) {
      Log.v(TAG, "Configure Loader with query" + getQueryString());
    }

    if (getQueryString() == null) {
      loader.configureQuery("");
      mNameMatcher.setQuery("");
    } else {
      loader.configureQuery(getQueryString());
      mNameMatcher.setQuery(PhoneNumberUtils.normalizeNumber(getQueryString()));
    }
  }

  /**
   * Sets highlight options for a List item in the SmartDial search results.
   *
   * @param view ContactListItemView where the result will be displayed.
   * @param cursor Object containing information of the associated List item.
   */
  @Override
  protected void setHighlight(ContactListItemView view, Cursor cursor) {
    /// M: [MTK Dialer Search] @{
    if (DialerFeatureOptions.isDialerSearchEnabled()) {
      super.setHighlight(view, cursor);
      return;
    }
    /// @}
    view.clearHighlightSequences();

    if (mNameMatcher.matches(cursor.getString(PhoneQuery.DISPLAY_NAME))) {
      final ArrayList<SmartDialMatchPosition> nameMatches = mNameMatcher.getMatchPositions();
      for (SmartDialMatchPosition match : nameMatches) {
        view.addNameHighlightSequence(match.start, match.end);
        if (DEBUG) {
          Log.v(
              TAG,
              cursor.getString(PhoneQuery.DISPLAY_NAME)
                  + " "
                  + mNameMatcher.getQuery()
                  + " "
                  + String.valueOf(match.start));
        }
      }
    }

    final SmartDialMatchPosition numberMatch =
        mNameMatcher.matchesNumber(cursor.getString(PhoneQuery.PHONE_NUMBER));
    if (numberMatch != null) {
      view.addNumberHighlightSequence(numberMatch.start, numberMatch.end);
    }
  }

  @Override
  public void setQueryString(String queryString) {
    //*/ freeme.zhaozehong, 20180205. for freemeOS, UI redesign
    if (true) {
        super.setQueryString(queryString);
        return;
    }
    //*/
    final boolean showNumberShortcuts = !TextUtils.isEmpty(getFormattedQueryString());
    boolean changed = false;
    changed |= setShortcutEnabled(SHORTCUT_CREATE_NEW_CONTACT, showNumberShortcuts);
    changed |= setShortcutEnabled(SHORTCUT_ADD_TO_EXISTING_CONTACT, showNumberShortcuts);
    changed |= setShortcutEnabled(SHORTCUT_SEND_SMS_MESSAGE, showNumberShortcuts);
    changed |=
        setShortcutEnabled(
            SHORTCUT_MAKE_VIDEO_CALL, showNumberShortcuts && CallUtil.isVideoEnabled(getContext()));
    if (changed) {
      notifyDataSetChanged();
    }
    super.setQueryString(queryString);
  }

  public void setShowEmptyListForNullQuery(boolean show) {
    mNameMatcher.setShouldMatchEmptyQuery(!show);
  }

  /// M: Mediatek start.
  /**
   * M: [MTK Dialer Search] Sets query for the DialerSearchCursorLoader
   * @param loader
   */
  public void configureLoader(DialerSearchCursorLoader loader) {
    Log.d(TAG, "MTK-DialerSearch, configureLoader, getQueryString: " + getQueryString()
        + " ,loader: " + loader);

    if (getQueryString() == null) {
      loader.configureQuery("", true);
      mNameMatcher.setQuery("");
    } else {
      loader.configureQuery(getQueryString(), true);
      mNameMatcher.setQuery(PhoneNumberUtils.normalizeNumber(getQueryString()));
    }
  }

  /**
   *  M: [MTK Dialer Search] phone number column index changed due to dialer search
   * @see com.android.contacts.common.list.PhoneNumberListAdapter#getPhoneNumber(int)
   */
  @Override
  public String getPhoneNumber(int position) {
    if (!DialerFeatureOptions.isDialerSearchEnabled()) {
      return super.getPhoneNumber(position);
    }

    Cursor cursor = ((Cursor) getItem(position));
    if (cursor != null) {
      String phoneNumber = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
      Log.d(TAG,
          "SmartDialNumberListAdatper: phoneNumber:" + LogUtil.sanitizePhoneNumber(phoneNumber));
      return phoneNumber;
    } else {
      Log.w(TAG, "Cursor was null in getPhoneNumber() call. Returning null instead.");
      return null;
    }
  }
  /// M: Mediatek end.

    //*/ freeme.zhaozehong, 20170809. to refresh shortcut item
    @Override
    public void refreshShortcutItem(String queryString) {
        final boolean showNumberShortcuts = !TextUtils.isEmpty(queryString);
        boolean changed = false;
        boolean isContacts = isContact(queryString);
        changed |= setShortcutEnabled(SHORTCUT_CREATE_NEW_CONTACT,
                !isContacts && showNumberShortcuts);
        changed |= setShortcutEnabled(SHORTCUT_ADD_TO_EXISTING_CONTACT,
                !isContacts && showNumberShortcuts);
        changed |= setShortcutEnabled(SHORTCUT_SEND_SMS_MESSAGE, showNumberShortcuts);
        changed |= setShortcutEnabled(SHORTCUT_MAKE_VIDEO_CALL,
                showNumberShortcuts &&  CallUtil.isVideoEnabled(getContext()));
        if (changed) {
            notifyDataSetChanged();
        }
    }

    private boolean isContact(String number) {
        if (!TextUtils.isEmpty(number)) {
            int count = getCount();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    Object obj = getItem(i);
                    if (obj != null && obj instanceof Cursor) {
                        int contactsId = ((Cursor) obj).getInt(CONTACT_ID_INDEX);
                        String phoneNumber = ((Cursor) obj).getString(SEARCH_PHONE_NUMBER_INDEX);
                        if (contactsId > 0 && number.equals(phoneNumber)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    //*/
}
