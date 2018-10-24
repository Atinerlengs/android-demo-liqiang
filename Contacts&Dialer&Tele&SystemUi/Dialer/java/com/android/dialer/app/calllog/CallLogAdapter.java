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

import android.app.Activity;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Trace;
import android.provider.CallLog;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.telecom.PhoneAccountHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.compat.PhoneNumberUtilsCompat;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.dialer.app.DialtactsActivity;
import com.android.dialer.app.R;
import com.android.dialer.app.calllog.CallLogGroupBuilder.GroupCreator;
import com.android.dialer.app.calllog.calllogcache.CallLogCache;
import com.android.dialer.app.contactinfo.ContactInfoCache;
import com.android.dialer.app.voicemail.VoicemailPlaybackPresenter;
import com.android.dialer.app.voicemail.VoicemailPlaybackPresenter.OnVoicemailDeletedListener;
import com.android.dialer.blocking.FilteredNumberAsyncQueryHandler;
import com.android.dialer.calldetails.CallDetailsEntries;
import com.android.dialer.calldetails.CallDetailsEntries.CallDetailsEntry;
import com.android.dialer.callintent.CallIntentBuilder;
import com.android.dialer.calllogutils.PhoneAccountUtils;
import com.android.dialer.calllogutils.PhoneCallDetails;
import com.android.dialer.calllogutils.PhoneNumberDisplayUtil;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.common.concurrent.AsyncTaskExecutor;
import com.android.dialer.common.concurrent.AsyncTaskExecutors;
import com.android.dialer.configprovider.ConfigProviderBindings;
import com.android.dialer.compat.CompatUtils;
import com.android.dialer.enrichedcall.EnrichedCallCapabilities;
import com.android.dialer.enrichedcall.EnrichedCallComponent;
import com.android.dialer.enrichedcall.EnrichedCallManager;
import com.android.dialer.enrichedcall.historyquery.proto.HistoryResult;
import com.android.dialer.lightbringer.Lightbringer;
import com.android.dialer.lightbringer.LightbringerComponent;
import com.android.dialer.lightbringer.LightbringerListener;
import com.android.dialer.logging.ContactSource;
import com.android.dialer.logging.DialerImpression;
import com.android.dialer.logging.Logger;
import com.android.dialer.logging.UiAction;
import com.android.dialer.performancereport.PerformanceReport;
import com.android.dialer.phonenumbercache.CallLogQuery;
import com.android.dialer.phonenumbercache.ContactInfo;
import com.android.dialer.phonenumbercache.ContactInfoHelper;
import com.android.dialer.phonenumberutil.PhoneNumberHelper;
import com.android.dialer.spam.Spam;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.PermissionsUtil;

import com.mediatek.dialer.compat.ContactsCompat.PhoneCompat;
import com.mediatek.dialer.calllog.ConfCallLogInfo;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.util.DialerFeatureOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
//*/ freeme.zhaozehong, 20180126. for freemeOS, UI redesign
import com.android.dialer.database.CallLogQueryHandler;

import com.freeme.contacts.common.utils.FreemeDateTimeUtils;
import com.freeme.dialer.app.FreemeDialtactsActivity;
import com.freeme.dialer.callback.IFreemeMultiSelectCallBack;

import java.io.Serializable;
import java.util.HashMap;
//*/

//*/freeme.zhaozehong, 20180627. for freemeOS, replace ContextMenu with AlertDialog
import android.app.Dialog;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.view.ContextMenu;

import com.android.contacts.common.util.UriUtils;
import com.android.dialer.logging.ScreenEvent;

import com.android.internal.view.menu.ContextMenuBuilder;
//*/

/** Adapter class to fill in data for the Call Log. */
public class CallLogAdapter extends GroupingListAdapter
    implements GroupCreator, OnVoicemailDeletedListener, LightbringerListener {

  // Types of activities the call log adapter is used for
  public static final int ACTIVITY_TYPE_CALL_LOG = 1;
  public static final int ACTIVITY_TYPE_DIALTACTS = 2;
  private static final int NO_EXPANDED_LIST_ITEM = -1;
  public static final int ALERT_POSITION = 0;
  private static final int VIEW_TYPE_ALERT = 1;
  private static final int VIEW_TYPE_CALLLOG = 2;

  private static final String KEY_EXPANDED_POSITION = "expanded_position";
  private static final String KEY_EXPANDED_ROW_ID = "expanded_row_id";
  private static final String KEY_ACTION_MODE = "action_mode_selected_items";

  public static final String LOAD_DATA_TASK_IDENTIFIER = "load_data";

  public static final String ENABLE_CALL_LOG_MULTI_SELECT = "enable_call_log_multiselect";
  public static final boolean ENABLE_CALL_LOG_MULTI_SELECT_FLAG = true;

  protected final Activity mActivity;
  protected final VoicemailPlaybackPresenter mVoicemailPlaybackPresenter;
  /** Cache for repeated requests to Telecom/Telephony. */
  protected final CallLogCache mCallLogCache;

  private final CallFetcher mCallFetcher;
  private final OnActionModeStateChangedListener mActionModeStateChangedListener;
  private final MultiSelectRemoveView mMultiSelectRemoveView;
  @NonNull private final FilteredNumberAsyncQueryHandler mFilteredNumberAsyncQueryHandler;
  private final int mActivityType;

  /** Instance of helper class for managing views. */
  private final CallLogListItemHelper mCallLogListItemHelper;
  /** Helper to group call log entries. */
  private final CallLogGroupBuilder mCallLogGroupBuilder;

  private final AsyncTaskExecutor mAsyncTaskExecutor = AsyncTaskExecutors.createAsyncTaskExecutor();
  private ContactInfoCache mContactInfoCache;
  // Tracks the position of the currently expanded list item.
  private int mCurrentlyExpandedPosition = RecyclerView.NO_POSITION;
  // Tracks the rowId of the currently expanded list item, so the position can be updated if there
  // are any changes to the call log entries, such as additions or removals.
  private long mCurrentlyExpandedRowId = NO_EXPANDED_LIST_ITEM;

  private final CallLogAlertManager mCallLogAlertManager;

  public ActionMode mActionMode = null;
  public boolean selectAllMode = false;
  public boolean deselectAllMode = false;
  private final SparseArray<String> selectedItems = new SparseArray<>();

  private final ActionMode.Callback mActionModeCallback =
      new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
          if (mActivity != null) {
            announceforAccessibility(
                mActivity.getCurrentFocus(),
                mActivity.getString(R.string.description_entering_bulk_action_mode));
          }
          mActionMode = mode;
          // Inflate a menu resource providing context menu items
          MenuInflater inflater = mode.getMenuInflater();
          inflater.inflate(R.menu.actionbar_delete, menu);
          mMultiSelectRemoveView.showMultiSelectRemoveView(true);
          mActionModeStateChangedListener.onActionModeStateChanged(true);
          return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
          return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
          if (item.getItemId() == R.id.action_bar_delete_menu_item) {
            Logger.get(mActivity).logImpression(DialerImpression.Type.MULTISELECT_TAP_DELETE_ICON);
            if (selectedItems.size() > 0) {
              showDeleteSelectedItemsDialog();
            }
            return true;
          } else {
            return false;
          }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
          if (mActivity != null) {
            announceforAccessibility(
                mActivity.getCurrentFocus(),
                mActivity.getString(R.string.description_leaving_bulk_action_mode));
          }
          selectedItems.clear();
          mActionMode = null;
          selectAllMode = false;
          deselectAllMode = false;
          mMultiSelectRemoveView.showMultiSelectRemoveView(false);
          mActionModeStateChangedListener.onActionModeStateChanged(false);
          notifyDataSetChanged();
        }
      };

  private void showDeleteSelectedItemsDialog() {
    SparseArray<String> voicemailsToDeleteOnConfirmation = selectedItems.clone();
    new AlertDialog.Builder(mActivity, R.style.AlertDialogCustom)
        .setCancelable(true)
        .setTitle(
            mActivity
                .getResources()
                .getQuantityString(
                    R.plurals.delete_voicemails_confirmation_dialog_title, selectedItems.size()))
        .setPositiveButton(
            R.string.voicemailMultiSelectDeleteConfirm,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, final int button) {
                LogUtil.i(
                    "CallLogAdapter.showDeleteSelectedItemsDialog",
                    "onClick, these items to delete " + voicemailsToDeleteOnConfirmation);
                deleteSelectedItems(voicemailsToDeleteOnConfirmation);
                mActionMode.finish();
                dialog.cancel();
                Logger.get(mActivity)
                    .logImpression(
                        DialerImpression.Type.MULTISELECT_DELETE_ENTRY_VIA_CONFIRMATION_DIALOG);
              }
            })
        .setOnCancelListener(
            new OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialogInterface) {
                Logger.get(mActivity)
                    .logImpression(
                        DialerImpression.Type
                            .MULTISELECT_CANCEL_CONFIRMATION_DIALOG_VIA_CANCEL_TOUCH);
                dialogInterface.cancel();
              }
            })
        .setNegativeButton(
            R.string.voicemailMultiSelectDeleteCancel,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, final int button) {
                Logger.get(mActivity)
                    .logImpression(
                        DialerImpression.Type
                            .MULTISELECT_CANCEL_CONFIRMATION_DIALOG_VIA_CANCEL_BUTTON);
                dialog.cancel();
              }
            })
        .show();
    Logger.get(mActivity)
        .logImpression(DialerImpression.Type.MULTISELECT_DISPLAY_DELETE_CONFIRMATION_DIALOG);
  }

  private void deleteSelectedItems(SparseArray<String> voicemailsToDelete) {
    for (int i = 0; i < voicemailsToDelete.size(); i++) {
      String voicemailUri = voicemailsToDelete.get(voicemailsToDelete.keyAt(i));
      LogUtil.i("CallLogAdapter.deleteSelectedItems", "deleting uri:" + voicemailUri);
      CallLogAsyncTaskUtil.deleteVoicemail(mActivity, Uri.parse(voicemailUri), null);
    }
  }

  private final View.OnLongClickListener mLongPressListener =
      new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
          if (ConfigProviderBindings.get(v.getContext())
                  .getBoolean(ENABLE_CALL_LOG_MULTI_SELECT, ENABLE_CALL_LOG_MULTI_SELECT_FLAG)
              && mVoicemailPlaybackPresenter != null) {
            if (v.getId() == R.id.primary_action_view || v.getId() == R.id.quick_contact_photo) {
              if (mActionMode == null) {
                Logger.get(mActivity)
                    .logImpression(
                        DialerImpression.Type.MULTISELECT_LONG_PRESS_ENTER_MULTI_SELECT_MODE);
                mActionMode = v.startActionMode(mActionModeCallback);
              }
              Logger.get(mActivity)
                  .logImpression(DialerImpression.Type.MULTISELECT_LONG_PRESS_TAP_ENTRY);
              CallLogListItemViewHolder viewHolder = (CallLogListItemViewHolder) v.getTag();
              viewHolder.quickContactView.setVisibility(View.GONE);
              /*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
              viewHolder.checkBoxView.setVisibility(View.VISIBLE);
              /*/
              viewHolder.setCheckBoxViewVisiable(View.VISIBLE);
              //*/
              mExpandCollapseListener.onClick(v);
              return true;
            }
          }
          //*/ freeme.zhaozehong, 20180627. for freemeOS, replace ContextMenu with AlertDialog
          else {
              CallLogListItemViewHolder viewHolder = (CallLogListItemViewHolder) v.getTag();
              showBottomMenu(viewHolder);
          }
          //*/
          return true;
        }
      };

  @VisibleForTesting
  public View.OnClickListener getExpandCollapseListener() {
    return mExpandCollapseListener;
  }

  /** The OnClickListener used to expand or collapse the action buttons of a call log entry. */
  private final View.OnClickListener mExpandCollapseListener =
      new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          PerformanceReport.recordClick(UiAction.Type.CLICK_CALL_LOG_ITEM);

          CallLogListItemViewHolder viewHolder = (CallLogListItemViewHolder) v.getTag();
          if (viewHolder == null) {
            return;
          }

          //*/ freeme.zhaozehong, 20180130. for freemeOS, UI redesign
          if (true) {
              if (isMultiSelected) {
                  if (mSelectedCallIds.containsKey(viewHolder.rowId)) {
                      mSelectedCallIds.remove(viewHolder.rowId);
                      viewHolder.updateCheckBoxCheckStatus(false);
                  } else {
                      mSelectedCallIds.put(viewHolder.rowId, viewHolder.callIds);
                      viewHolder.updateCheckBoxCheckStatus(true);
                  }
                  if (mFreemeMultiSelectCallBack != null) {
                      mFreemeMultiSelectCallBack.onSelectedCount(mSelectedCallIds.size());
                  }
                  return;
              }

              if (viewHolder.callType == CallLog.Calls.MISSED_TYPE) {
                  CallLogAsyncTaskUtil.markCallAsRead(mActivity, viewHolder.callIds);
                  if (mActivityType == ACTIVITY_TYPE_DIALTACTS) {
                      if (v.getContext() instanceof FreemeDialtactsActivity) {
                          ((FreemeDialtactsActivity) v.getContext()).updateTabUnreadCounts();
                      }
                  }
              }
              viewHolder.call();
              DialerUtils.disableViewClickableInDuration(v, 200);
              return;
          }
          //*/

          if (mActionMode != null && viewHolder.voicemailUri != null) {
            selectAllMode = false;
            deselectAllMode = false;
            mMultiSelectRemoveView.setSelectAllModeToFalse();
            int id = getVoicemailId(viewHolder.voicemailUri);
            if (selectedItems.get(id) != null) {
              Logger.get(mActivity)
                  .logImpression(DialerImpression.Type.MULTISELECT_SINGLE_PRESS_UNSELECT_ENTRY);
              uncheckMarkCallLogEntry(viewHolder, id);
            } else {
              Logger.get(mActivity)
                  .logImpression(DialerImpression.Type.MULTISELECT_SINGLE_PRESS_SELECT_ENTRY);
              checkMarkCallLogEntry(viewHolder);
              // select all check box logic
              if (getItemCount() == selectedItems.size()) {
                LogUtil.i(
                    "mExpandCollapseListener.onClick",
                    "getitem count %d is equal to items select count %d, check select all box",
                    getItemCount(),
                    selectedItems.size());
                mMultiSelectRemoveView.tapSelectAll();
              }
            }
            return;
          }

          if (mVoicemailPlaybackPresenter != null) {
            // Always reset the voicemail playback state on expand or collapse.
            mVoicemailPlaybackPresenter.resetAll();
          }

          // If enriched call capabilities were unknown on the initial load,
          // viewHolder.isCallComposerCapable may be unset. Check here if we have the capabilities
          // as a last attempt at getting them before showing the expanded view to the user
          EnrichedCallCapabilities capabilities =
              getEnrichedCallManager().getCapabilities(viewHolder.number);
          viewHolder.isCallComposerCapable =
              capabilities != null && capabilities.supportsCallComposer();
          generateAndMapNewCallDetailsEntriesHistoryResults(
              viewHolder.number,
              viewHolder.getDetailedPhoneDetails(),
              getAllHistoricalData(viewHolder.number, viewHolder.getDetailedPhoneDetails()));

          if (viewHolder.rowId == mCurrentlyExpandedRowId) {
            // Hide actions, if the clicked item is the expanded item.
            viewHolder.showActions(false);

            mCurrentlyExpandedPosition = RecyclerView.NO_POSITION;
            mCurrentlyExpandedRowId = NO_EXPANDED_LIST_ITEM;
          } else {
            if (viewHolder.callType == CallLog.Calls.MISSED_TYPE) {
              CallLogAsyncTaskUtil.markCallAsRead(mActivity, viewHolder.callIds);
              if (mActivityType == ACTIVITY_TYPE_DIALTACTS) {
                ((DialtactsActivity) v.getContext()).updateTabUnreadCounts();
              }
            }
            expandViewHolderActions(viewHolder);

            if (isLightbringerCallButtonVisible(viewHolder.videoCallButtonView)) {
              CallIntentBuilder.increaseLightbringerCallButtonAppearInExpandedCallLogItemCount();
            }
          }
        }

        private boolean isLightbringerCallButtonVisible(View videoCallButtonView) {
          if (videoCallButtonView == null) {
            return false;
          }
          if (videoCallButtonView.getVisibility() != View.VISIBLE) {
            return false;
          }
          IntentProvider intentProvider = (IntentProvider) videoCallButtonView.getTag();
          if (intentProvider == null) {
            return false;
          }
          String packageName =
              LightbringerComponent.get(mActivity).getLightbringer().getPackageName();
          if (packageName == null) {
            return false;
          }
          return packageName.equals(intentProvider.getIntent(mActivity).getPackage());
        }
      };

  private void checkMarkCallLogEntry(CallLogListItemViewHolder viewHolder) {
    announceforAccessibility(
        mActivity.getCurrentFocus(),
        mActivity.getString(
            R.string.description_selecting_bulk_action_mode, viewHolder.nameOrNumber));
    viewHolder.quickContactView.setVisibility(View.GONE);
    /*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
    viewHolder.checkBoxView.setVisibility(View.VISIBLE);
    /*/
    viewHolder.setCheckBoxViewVisiable(View.VISIBLE);
    //*/
    selectedItems.put(getVoicemailId(viewHolder.voicemailUri), viewHolder.voicemailUri);
    updateActionBar();
  }

  private void announceforAccessibility(View view, String announcement) {
    if (view != null) {
      view.announceForAccessibility(announcement);
    }
  }

  private void updateActionBar() {
    if (mActionMode == null && selectedItems.size() > 0) {
      Logger.get(mActivity)
          .logImpression(DialerImpression.Type.MULTISELECT_ROTATE_AND_SHOW_ACTION_MODE);
      mActivity.startActionMode(mActionModeCallback);
    }
    if (mActionMode != null) {
      mActionMode.setTitle(
          mActivity
              .getResources()
              .getString(
                  R.string.voicemailMultiSelectActionBarTitle,
                  Integer.toString(selectedItems.size())));
    }
  }

  private void uncheckMarkCallLogEntry(CallLogListItemViewHolder viewHolder, int id) {
    announceforAccessibility(
        mActivity.getCurrentFocus(),
        mActivity.getString(
            R.string.description_unselecting_bulk_action_mode, viewHolder.nameOrNumber));
    selectedItems.delete(id);
    /*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
    viewHolder.checkBoxView.setVisibility(View.GONE);
    /*/
    viewHolder.setCheckBoxViewVisiable(View.GONE);
    //*/
    viewHolder.quickContactView.setVisibility(View.VISIBLE);
    updateActionBar();
  }

  private static int getVoicemailId(String voicemailUri) {
    Assert.checkArgument(voicemailUri != null);
    Assert.checkArgument(voicemailUri.length() > 0);
    return (int) ContentUris.parseId(Uri.parse(voicemailUri));
  }

  /**
   * A list of {@link CallLogQuery#ID} that will be hidden. The hide might be temporary so instead
   * if removing an item, it will be shown as an invisible view. This simplifies the calculation of
   * item position.
   */
  @NonNull private Set<Long> mHiddenRowIds = new ArraySet<>();
  /**
   * Holds a list of URIs that are pending deletion or undo. If the activity ends before the undo
   * timeout, all of the pending URIs will be deleted.
   *
   * <p>TODO: move this and OnVoicemailDeletedListener to somewhere like {@link
   * VisualVoicemailCallLogFragment}. The CallLogAdapter does not need to know about what to do with
   * hidden item or what to hide.
   */
  @NonNull private final Set<Uri> mHiddenItemUris = new ArraySet<>();

  private CallLogListItemViewHolder.OnClickListener mBlockReportSpamListener;
  /**
   * Map, keyed by call Id, used to track the day group for a call. As call log entries are put into
   * the primary call groups in {@link com.android.dialer.app.calllog.CallLogGroupBuilder}, they are
   * also assigned a secondary "day group". This map tracks the day group assigned to all calls in
   * the call log. This information is used to trigger the display of a day group header above the
   * call log entry at the start of a day group. Note: Multiple calls are grouped into a single
   * primary "call group" in the call log, and the cursor used to bind rows includes all of these
   * calls. When determining if a day group change has occurred it is necessary to look at the last
   * entry in the call log to determine its day group. This map provides a means of determining the
   * previous day group without having to reverse the cursor to the start of the previous day call
   * log entry.
   */
  private Map<Long, Integer> mDayGroups = new ArrayMap<>();

  private boolean mLoading = true;
  private ContactsPreferences mContactsPreferences;

  private boolean mIsSpamEnabled;

  public CallLogAdapter(
      Activity activity,
      ViewGroup alertContainer,
      CallFetcher callFetcher,
      MultiSelectRemoveView multiSelectRemoveView,
      OnActionModeStateChangedListener actionModeStateChangedListener,
      CallLogCache callLogCache,
      ContactInfoCache contactInfoCache,
      VoicemailPlaybackPresenter voicemailPlaybackPresenter,
      @NonNull FilteredNumberAsyncQueryHandler filteredNumberAsyncQueryHandler,
      int activityType) {
    super();

    mActivity = activity;
    mCallFetcher = callFetcher;
    mActionModeStateChangedListener = actionModeStateChangedListener;
    mMultiSelectRemoveView = multiSelectRemoveView;
    mVoicemailPlaybackPresenter = voicemailPlaybackPresenter;
    if (mVoicemailPlaybackPresenter != null) {
      mVoicemailPlaybackPresenter.setOnVoicemailDeletedListener(this);
    }

    mActivityType = activityType;

    mContactInfoCache = contactInfoCache;

    if (!PermissionsUtil.hasContactsReadPermissions(activity)) {
      mContactInfoCache.disableRequestProcessing(true);
    }

    Resources resources = mActivity.getResources();

    mCallLogCache = callLogCache;

    PhoneCallDetailsHelper phoneCallDetailsHelper =
        new PhoneCallDetailsHelper(mActivity, resources, mCallLogCache);
    mCallLogListItemHelper =
        new CallLogListItemHelper(phoneCallDetailsHelper, resources, mCallLogCache);
    mCallLogGroupBuilder = new CallLogGroupBuilder(this);
    mFilteredNumberAsyncQueryHandler = Assert.isNotNull(filteredNumberAsyncQueryHandler);

    mContactsPreferences = new ContactsPreferences(mActivity);

    mBlockReportSpamListener =
        new BlockReportSpamListener(
            mActivity,
            ((Activity) mActivity).getFragmentManager(),
            this,
            mFilteredNumberAsyncQueryHandler);
    setHasStableIds(true);

    mCallLogAlertManager =
        new CallLogAlertManager(this, LayoutInflater.from(mActivity), alertContainer);
  }

  private void expandViewHolderActions(CallLogListItemViewHolder viewHolder) {
    if (!TextUtils.isEmpty(viewHolder.voicemailUri)) {
      Logger.get(mActivity).logImpression(DialerImpression.Type.VOICEMAIL_EXPAND_ENTRY);
    }

    int lastExpandedPosition = mCurrentlyExpandedPosition;
    // Show the actions for the clicked list item.
    viewHolder.showActions(true);
    mCurrentlyExpandedPosition = viewHolder.getAdapterPosition();
    mCurrentlyExpandedRowId = viewHolder.rowId;

    // If another item is expanded, notify it that it has changed. Its actions will be
    // hidden when it is re-binded because we change mCurrentlyExpandedRowId above.
    if (lastExpandedPosition != RecyclerView.NO_POSITION) {
      notifyItemChanged(lastExpandedPosition);
    }
    // M: Add for Presence
    ExtensionManager.getCallLogExtension().onExpandViewHolderActions(viewHolder.number);
  }

  public void onSaveInstanceState(Bundle outState) {
    outState.putInt(KEY_EXPANDED_POSITION, mCurrentlyExpandedPosition);
    outState.putLong(KEY_EXPANDED_ROW_ID, mCurrentlyExpandedRowId);

    ArrayList<String> listOfSelectedItems = new ArrayList<>();

    if (selectedItems.size() > 0) {
      for (int i = 0; i < selectedItems.size(); i++) {
        int id = selectedItems.keyAt(i);
        String voicemailUri = selectedItems.valueAt(i);
        LogUtil.i(
            "CallLogAdapter.onSaveInstanceState", "index %d, id=%d, uri=%s ", i, id, voicemailUri);
        listOfSelectedItems.add(voicemailUri);
      }
    }
    outState.putStringArrayList(KEY_ACTION_MODE, listOfSelectedItems);

      //*/ freeme.zhaozehong, 20180223. for freemeOS, UI redesign
      outState.putBoolean(KEY_MULTI_MODE, isMultiSelected);
      outState.putSerializable(KEY_MULTI_SELECTED_DATA, mSelectedCallIds);
      //*/

    LogUtil.i(
        "CallLogAdapter.onSaveInstanceState",
        "saved: %d, selectedItemsSize:%d",
        listOfSelectedItems.size(),
        selectedItems.size());
  }

  public void onRestoreInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
        //*/ freeme.zhaozehong, 20180223. for freemeOS, UI redesign
        isMultiSelected = savedInstanceState.getBoolean(KEY_MULTI_MODE);
        if (isMultiSelected) {
            mSelectedCallIds.clear();
            Serializable data = savedInstanceState.getSerializable(KEY_MULTI_SELECTED_DATA);
            if (data != null) {
                mSelectedCallIds.putAll((HashMap) data);
            }
        }
        if (mFreemeMultiSelectCallBack != null) {
            mFreemeMultiSelectCallBack.isInMulitMode(isMultiSelected);
            mFreemeMultiSelectCallBack.onSelectedCount(mSelectedCallIds.size());
        }
        //*/

      mCurrentlyExpandedPosition =
          savedInstanceState.getInt(KEY_EXPANDED_POSITION, RecyclerView.NO_POSITION);
      mCurrentlyExpandedRowId =
          savedInstanceState.getLong(KEY_EXPANDED_ROW_ID, NO_EXPANDED_LIST_ITEM);
      // Restoring multi selected entries
      ArrayList<String> listOfSelectedItems =
          savedInstanceState.getStringArrayList(KEY_ACTION_MODE);

      //*/ freeme.zhaozehong, 20180131. for freemeOS, UI redesign
      if (listOfSelectedItems != null)
      //*/
      LogUtil.i(
          "CallLogAdapter.onRestoreInstanceState",
          "restored selectedItemsList:%d",
          listOfSelectedItems.size());

      //*/ freeme.zhaozehong, 20180131. for freemeOS, UI redesign
      if (listOfSelectedItems != null)
      //*/
      if (!listOfSelectedItems.isEmpty()) {
        for (int i = 0; i < listOfSelectedItems.size(); i++) {
          String voicemailUri = listOfSelectedItems.get(i);
          int id = getVoicemailId(voicemailUri);
          LogUtil.i(
              "CallLogAdapter.onRestoreInstanceState",
              "restoring selected index %d, id=%d, uri=%s ",
              i,
              id,
              voicemailUri);
          selectedItems.put(id, voicemailUri);
        }

        LogUtil.i(
            "CallLogAdapter.onRestoreInstance",
            "restored selectedItems %s",
            selectedItems.toString());
        updateActionBar();
      }
    }
  }

  /** Requery on background thread when {@link Cursor} changes. */
  @Override
  protected void onContentChanged() {
    mCallFetcher.fetchCalls();
  }

  public void setLoading(boolean loading) {
    mLoading = loading;
  }

  public boolean isEmpty() {
    if (mLoading) {
      // We don't want the empty state to show when loading.
      return false;
    } else {
      return getItemCount() == 0;
    }
  }

  public void clearFilteredNumbersCache() {
    mFilteredNumberAsyncQueryHandler.clearCache();
  }

  public void onResume() {
    if (PermissionsUtil.hasPermission(mActivity, android.Manifest.permission.READ_CONTACTS)) {
      /// M: enable request process if permission is enable
      mContactInfoCache.disableRequestProcessing(false);
      mContactInfoCache.start();
    }
    mContactsPreferences.refreshValue(ContactsPreferences.DISPLAY_ORDER_KEY);
    mIsSpamEnabled = Spam.get(mActivity).isSpamEnabled();
    getLightbringer().registerListener(this);
    notifyDataSetChanged();
  }

  public void onPause() {
    getLightbringer().unregisterListener(this);
    pauseCache();
    for (Uri uri : mHiddenItemUris) {
      CallLogAsyncTaskUtil.deleteVoicemail(mActivity, uri, null);
    }
  }

  public void onStop() {
    //*/ freeme.zhaozehong, 20180627. for freemeOS, replace ContextMenu with AlertDialog
    hideButtomMenu();
    //*/
    getEnrichedCallManager().clearCachedData();
  }

  public CallLogAlertManager getAlertManager() {
    return mCallLogAlertManager;
  }

  @VisibleForTesting
  /* package */ void pauseCache() {
    mContactInfoCache.stop();
    mCallLogCache.reset();
  }

  @Override
  protected void addGroups(Cursor cursor) {
    mCallLogGroupBuilder.addGroups(cursor);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_ALERT) {
      return mCallLogAlertManager.createViewHolder(parent);
    }
    return createCallLogEntryViewHolder(parent);
  }

  /**
   * Creates a new call log entry {@link ViewHolder}.
   *
   * @param parent the parent view.
   * @return The {@link ViewHolder}.
   */
  private ViewHolder createCallLogEntryViewHolder(ViewGroup parent) {
    LayoutInflater inflater = LayoutInflater.from(mActivity);
    /*/ freeme.zhaozehong, 20180130. for freemeOS, UI redesign
    View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
    CallLogListItemViewHolder viewHolder =
        CallLogListItemViewHolder.create(
            view,
            mActivity,
            mBlockReportSpamListener,
            mExpandCollapseListener,
            mLongPressListener,
            mActionModeStateChangedListener,
            mCallLogCache,
            mCallLogListItemHelper,
            mVoicemailPlaybackPresenter);
    /*/
    View view = inflater.inflate(R.layout.freeme_call_log_list_item, parent, false);
    CallLogListItemViewHolder viewHolder =
            CallLogListItemViewHolder.newCreate(
                    view,
                    mActivity,
                    mBlockReportSpamListener,
                    mExpandCollapseListener,
                    mLongPressListener,
                    mActionModeStateChangedListener,
                    mCallLogCache,
                    mCallLogListItemHelper,
                    mVoicemailPlaybackPresenter);
    viewHolder.setMultiSelectCallBack(mMultiSelectCallBack);
    //*/

    viewHolder.callLogEntryView.setTag(viewHolder);

    viewHolder.primaryActionView.setTag(viewHolder);
    viewHolder.quickContactView.setTag(viewHolder);

    return viewHolder;
  }

  /**
   * Binds the views in the entry to the data in the call log. TODO: This gets called 20-30 times
   * when Dialer starts up for a single call log entry and should not. It invokes cross-process
   * methods and the repeat execution can get costly.
   *
   * @param viewHolder The view corresponding to this entry.
   * @param position The position of the entry.
   */
  @Override
  public void onBindViewHolder(ViewHolder viewHolder, int position) {
    Trace.beginSection("onBindViewHolder: " + position);
    switch (getItemViewType(position)) {
      case VIEW_TYPE_ALERT:
        // Do nothing
        break;
      default:
        bindCallLogListViewHolder(viewHolder, position);
        break;
    }
    Trace.endSection();
  }

  @Override
  public void onViewRecycled(ViewHolder viewHolder) {
    if (viewHolder.getItemViewType() == VIEW_TYPE_CALLLOG) {
      CallLogListItemViewHolder views = (CallLogListItemViewHolder) viewHolder;
      updateCheckMarkedStatusOfEntry(views);
      //*/ freeme.zhaozehong, 20180226. for freemeOS, UI redesign
      holderMap.remove(views);
      //*/

      if (views.asyncTask != null) {
        views.asyncTask.cancel(true);
        LogUtil.d("CallLogAdapter.onViewRecycled", "views.asyncTask.cancel true.");
      }
    }
  }

  @Override
  public void onViewAttachedToWindow(ViewHolder viewHolder) {
    if (viewHolder.getItemViewType() == VIEW_TYPE_CALLLOG) {
      ((CallLogListItemViewHolder) viewHolder).isAttachedToWindow = true;
    }
  }

  @Override
  public void onViewDetachedFromWindow(ViewHolder viewHolder) {
    if (viewHolder.getItemViewType() == VIEW_TYPE_CALLLOG) {
      ((CallLogListItemViewHolder) viewHolder).isAttachedToWindow = false;
    }
  }

  /**
   * Binds the view holder for the call log list item view.
   *
   * @param viewHolder The call log list item view holder.
   * @param position The position of the list item.
   */
  protected void bindCallLogListViewHolder(final ViewHolder viewHolder, final int position) {
    Cursor c = (Cursor) getItem(position);
    if (c == null) {
      return;
    }
    CallLogListItemViewHolder views = (CallLogListItemViewHolder) viewHolder;
    updateCheckMarkedStatusOfEntry(views);
    //*/ freeme.zhaozehong, 20180226. for freemeOS, UI redesign
    holderMap.put(views, position);
    views.setCallTypeFilter(mCallTypeFilter);
    //*/

    views.isLoaded = false;
    int groupSize = getGroupSize(position);
    CallDetailsEntries callDetailsEntries = createCallDetailsEntries(c, groupSize);
    PhoneCallDetails details = createPhoneCallDetails(c, groupSize, views);
    if (mHiddenRowIds.contains(c.getLong(CallLogQuery.ID))) {
      views.callLogEntryView.setVisibility(View.GONE);
      views.dayGroupHeader.setVisibility(View.GONE);
      return;
    } else {
      views.callLogEntryView.setVisibility(View.VISIBLE);
      // dayGroupHeader will be restored after loadAndRender() if it is needed.
    }
    /// M: ALPS03413423  cant inflate action view here,
    //it will inflated  by showAction(true) in methord of render()@{
    /*if (mCurrentlyExpandedRowId == views.rowId) {
             views.inflateActionViewStub();
       }*/
    /// @}
    loadAndRender(views, views.rowId, details, callDetailsEntries);
  }

  private void updateCheckMarkedStatusOfEntry(CallLogListItemViewHolder views) {
    if (selectedItems.size() > 0 && views.voicemailUri != null) {
      int id = getVoicemailId(views.voicemailUri);
      if (selectedItems.get(id) != null) {
        checkMarkCallLogEntry(views);
      } else {
        uncheckMarkCallLogEntry(views, id);
      }
    }
  }

  private void loadAndRender(
      final CallLogListItemViewHolder views,
      final long rowId,
      final PhoneCallDetails details,
      final CallDetailsEntries callDetailsEntries) {
    LogUtil.d("CallLogAdapter.loadAndRender", "position: %d", views.getAdapterPosition());
    /// M: ALPS03446184  call log load perfemance{@
    loadData(views, rowId, details);
    ExtensionManager.getRCSeCallLogExtension().bindPluginViewForCallLogList(
         views.primaryActionView.getContext(),
         (ViewGroup) views.primaryActionView, views.number);

    int currentGroup = getDayGroupForCall(views.rowId);
    if (currentGroup != details.previousGroup) {
         views.dayGroupHeaderVisibility = View.VISIBLE;
         views.dayGroupHeaderText = getGroupDescription(currentGroup);
    } else {
         views.dayGroupHeaderVisibility = View.GONE;
    }

    //*/ freeme.zhaozehong, 20180223. for freemeOS, UI redesign
    views.isMultiSelected = isMultiSelected;
    //*/

    render(views, details, rowId);
    /// @}

    // Reset block and spam information since this view could be reused which may contain
    // outdated data.
    views.isSpam = false;
    views.blockId = null;
    views.isSpamFeatureEnabled = false;

    // Attempt to set the isCallComposerCapable field. If capabilities are unknown for this number,
    // the value will be false while capabilities are requested. mExpandCollapseListener will
    // attempt to set the field properly in that case
    views.isCallComposerCapable = isCallComposerCapable(views.number);
    CallDetailsEntries updatedCallDetailsEntries =
        generateAndMapNewCallDetailsEntriesHistoryResults(
            views.number,
            callDetailsEntries,
            getAllHistoricalData(views.number, callDetailsEntries));
    views.setDetailedPhoneDetails(updatedCallDetailsEntries);
    views.lightbringerReady = getLightbringer().isReachable(mActivity, views.number);
    final AsyncTask<Void, Void, Boolean> loadDataTask =
        new AsyncTask<Void, Void, Boolean>() {
          @Override
          protected Boolean doInBackground(Void... params) {
            if (isCancelled()) {
                return false;
            }
            views.blockId =
                mFilteredNumberAsyncQueryHandler.getBlockedIdSynchronous(
                    views.number, views.countryIso);
            details.isBlocked = views.blockId != null;

            if (mIsSpamEnabled) {
              views.isSpamFeatureEnabled = true;
              // Only display the call as a spam call if there are incoming calls in the list.
              // Call log cards with only outgoing calls should never be displayed as spam.
              views.isSpam =
                  details.hasIncomingCalls()
                      && Spam.get(mActivity)
                          .checkSpamStatusSynchronous(views.number, views.countryIso);
              details.isSpam = views.isSpam;
            }
            //return !isCancelled() && loadData(views, rowId, details);
            return !isCancelled();
          }

          @Override
          protected void onPostExecute(Boolean success) {
            views.isLoaded = true;
            /// M: ALPS03446184  call log load perfemance{@
            if (success) {
              /*int currentGroup = getDayGroupForCall(views.rowId);
              if (currentGroup != details.previousGroup) {
                views.dayGroupHeaderVisibility = View.VISIBLE;
                views.dayGroupHeaderText = getGroupDescription(currentGroup);
              } else {
                views.dayGroupHeaderVisibility = View.GONE;
              }
              render(views, details, rowId);*/
              LogUtil.d(TAG,  "onPostExecute", "views.showActions");
              /// M: ALPS03581720 only update action
              ///is shown after got block id info. or block button will
              ///not become unblock. call button would not hide in delete
              ///activity when update hide action view, so dont update.
              if (mCurrentlyExpandedRowId == views.rowId) {
                mCurrentlyExpandedPosition = views.getAdapterPosition();
                views.showActions(true);
              }
             }
            /// @}
          }
        };

    views.asyncTask = loadDataTask;
    LogUtil.d("CallLogAdapter.loadAndRender", "AsyncTaskExecutor.submit.");
    mAsyncTaskExecutor.submit(LOAD_DATA_TASK_IDENTIFIER, loadDataTask);
  }

  @MainThread
  private boolean isCallComposerCapable(@Nullable String number) {
    if (number == null) {
      return false;
    }

    EnrichedCallCapabilities capabilities = getEnrichedCallManager().getCapabilities(number);
    if (capabilities == null) {
      getEnrichedCallManager().requestCapabilities(number);
      return false;
    }
    return capabilities.supportsCallComposer();
  }

  @NonNull
  private Map<CallDetailsEntry, List<HistoryResult>> getAllHistoricalData(
      @Nullable String number, @NonNull CallDetailsEntries entries) {
    if (number == null) {
      return Collections.emptyMap();
    }

    Map<CallDetailsEntry, List<HistoryResult>> historicalData =
        getEnrichedCallManager().getAllHistoricalData(number, entries);
    if (historicalData == null) {
      getEnrichedCallManager().requestAllHistoricalData(number, entries);
      return Collections.emptyMap();
    }
    return historicalData;
  }

  private static CallDetailsEntries generateAndMapNewCallDetailsEntriesHistoryResults(
      @Nullable String number,
      @NonNull CallDetailsEntries callDetailsEntries,
      @NonNull Map<CallDetailsEntry, List<HistoryResult>> mappedResults) {
    if (number == null) {
      return callDetailsEntries;
    }
    CallDetailsEntries.Builder mutableCallDetailsEntries = CallDetailsEntries.newBuilder();
    for (CallDetailsEntry entry : callDetailsEntries.getEntriesList()) {
      CallDetailsEntry.Builder newEntry = CallDetailsEntry.newBuilder().mergeFrom(entry);
      List<HistoryResult> results = mappedResults.get(entry);
      if (results != null) {
        newEntry.addAllHistoryResults(mappedResults.get(entry));
        LogUtil.v(
            "CallLogAdapter.generateAndMapNewCallDetailsEntriesHistoryResults",
            "mapped %d results",
            newEntry.getHistoryResultsList().size());
      }
      mutableCallDetailsEntries.addEntries(newEntry.build());
    }
    return mutableCallDetailsEntries.build();
  }

  /**
   * Initialize PhoneCallDetails by reading all data from cursor. This method must be run on main
   * thread since cursor is not thread safe.
   */
  @MainThread
  private PhoneCallDetails createPhoneCallDetails(
      Cursor cursor, int count, final CallLogListItemViewHolder views) {
    Assert.isMainThread();
    final String number = cursor.getString(CallLogQuery.NUMBER);
    final String postDialDigits =
        (VERSION.SDK_INT >= VERSION_CODES.N) ? cursor.getString(CallLogQuery.POST_DIAL_DIGITS) : "";
    final String viaNumber =
        (VERSION.SDK_INT >= VERSION_CODES.N) ? cursor.getString(CallLogQuery.VIA_NUMBER) : "";
    final int numberPresentation = cursor.getInt(CallLogQuery.NUMBER_PRESENTATION);
    final ContactInfo cachedContactInfo = ContactInfoHelper.getContactInfo(cursor);
    final PhoneCallDetails details =
        new PhoneCallDetails(number, numberPresentation, postDialDigits);
    details.viaNumber = viaNumber;
    details.countryIso = cursor.getString(CallLogQuery.COUNTRY_ISO);
    details.date = cursor.getLong(CallLogQuery.DATE);
    details.duration = cursor.getLong(CallLogQuery.DURATION);
    details.features = getCallFeatures(cursor, count);
    details.geocode = cursor.getString(CallLogQuery.GEOCODED_LOCATION);
    details.transcription = cursor.getString(CallLogQuery.TRANSCRIPTION);
    details.callTypes = getCallTypes(cursor, count);

    details.accountComponentName = cursor.getString(CallLogQuery.ACCOUNT_COMPONENT_NAME);
    details.accountId = cursor.getString(CallLogQuery.ACCOUNT_ID);
    details.cachedContactInfo = cachedContactInfo;

    if (!cursor.isNull(CallLogQuery.DATA_USAGE)) {
      details.dataUsage = cursor.getLong(CallLogQuery.DATA_USAGE);
    }

    views.rowId = cursor.getLong(CallLogQuery.ID);
    // Stash away the Ids of the calls so that we can support deleting a row in the call log.
    views.callIds = getCallIds(cursor, count);
    details.previousGroup = getPreviousDayGroup(cursor);

    // Store values used when the actions ViewStub is inflated on expansion.
    views.number = number;
    views.countryIso = details.countryIso;
    views.postDialDigits = details.postDialDigits;
    views.numberPresentation = numberPresentation;

    if (details.callTypes[0] == CallLog.Calls.VOICEMAIL_TYPE
        /*/ freeme.liqiang, 20180331. set text color red when rejected or missed
        || details.callTypes[0] == CallLog.Calls.MISSED_TYPE) {
        /*/
        || details.callTypes[0] == CallLog.Calls.MISSED_TYPE
        || details.callTypes[0] == CallLog.Calls.REJECTED_TYPE) {
        //*/
      details.isRead = cursor.getInt(CallLogQuery.IS_READ) == 1;
    }
    views.callType = cursor.getInt(CallLogQuery.CALL_TYPE);
    views.voicemailUri = cursor.getString(CallLogQuery.VOICEMAIL_URI);

    /// M: [VoLTE ConfCallLog] For Volte Conference CallLog @{
    long confCallId = -1;
    if (DialerFeatureOptions.isVolteConfCallLogSupport()) {
        confCallId = cursor.getLong(CallLogQuery.CONFERENCE_CALL_ID);
        views.confCallId = confCallId;
        details.conferenceId = confCallId;
    }
    if (confCallId > 0 && !mIsConfCallMemberList) {
      ArrayList<String> numbers = getConferenceCallNumbers(cursor, count);
      details.date = getConferenceCallDate(cursor, count);
      views.confCallLogInfos = getConfCallLogInfos(cursor, count, details);
      views.confCallNumbers = numbers;
      int firstCallType = details.callTypes[0];
      details.callTypes = new int[1];
      details.callTypes[0] = firstCallType;
      LogUtil.d(TAG, "Volte ConfCall numbers= " + LogUtil.sanitizePii(numbers) + ", date="
              + details.date + ", name=" + details.namePrimary);
    } else {
      views.confCallLogInfos =null;
      views.confCallNumbers = null;
    }
    /// @}

    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    if (CallLogQuery.FREEME_YELLOW_FLAG_IDX != -1) {
        details.yellowPageFlag = cursor.getInt(CallLogQuery.FREEME_YELLOW_FLAG_IDX);
    }
    if (CallLogQuery.FREEME_CALL_MARK_IDX != -1) {
        details.callMark = cursor.getString(CallLogQuery.FREEME_CALL_MARK_IDX);
    }
    //*/

    return details;
  }

  @MainThread
  private static CallDetailsEntries createCallDetailsEntries(Cursor cursor, int count) {
    Assert.isMainThread();
    int position = cursor.getPosition();
    CallDetailsEntries.Builder entries = CallDetailsEntries.newBuilder();
    for (int i = 0; i < count; i++) {
      CallDetailsEntry.Builder entry =
          CallDetailsEntry.newBuilder()
              .setCallId(cursor.getLong(CallLogQuery.ID))
              .setCallType(cursor.getInt(CallLogQuery.CALL_TYPE))
              .setDataUsage(cursor.getLong(CallLogQuery.DATA_USAGE))
              .setDate(cursor.getLong(CallLogQuery.DATE))
              .setDuration(cursor.getLong(CallLogQuery.DURATION))
              .setFeatures(cursor.getInt(CallLogQuery.FEATURES));
      entries.addEntries(entry.build());
      cursor.moveToNext();
    }
    cursor.moveToPosition(position);
    return entries.build();
  }

  /**
   * Load data for call log. Any expensive operation should be put here to avoid blocking main
   * thread. Do NOT put any cursor operation here since it's not thread safe.
   */
  @WorkerThread
  private boolean loadData(CallLogListItemViewHolder views, long rowId, PhoneCallDetails details) {
    /// M: ALPS03446184  call log load perfemance
    //Assert.isWorkerThread();
    if (rowId != views.rowId) {
      LogUtil.i(
          "CallLogAdapter.loadData",
          "rowId of viewHolder changed after load task is issued, aborting load");
      return false;
    }

    final PhoneAccountHandle accountHandle =
        PhoneAccountUtils.getAccount(details.accountComponentName, details.accountId);

    final boolean isVoicemailNumber =
        mCallLogCache.isVoicemailNumber(accountHandle, details.number);

    // Note: Binding of the action buttons is done as required in configureActionViews when the
    // user expands the actions ViewStub.

    ContactInfo info = ContactInfo.EMPTY;
    if (PhoneNumberHelper.canPlaceCallsTo(details.number, details.numberPresentation)
        && !isVoicemailNumber
        /**M:[VoLTE ConfCallLog]*/
        && (views.confCallId <= 0 || mIsConfCallMemberList) ) {
      // Lookup contacts with this number
      // Only do remote lookup in first 5 rows.
      int position = views.getAdapterPosition();
      info =
          mContactInfoCache.getValue(
              details.number + details.postDialDigits,
              details.countryIso,
              details.cachedContactInfo,
              position
                  < ConfigProviderBindings.get(mActivity)
                      .getLong("number_of_call_to_do_remote_lookup", 5L));
    /**M:[VoLTE ConfCallLog] @{*/
    } else if (DialerFeatureOptions.isVolteConfCallLogSupport()
        && views.confCallId > 0 && !mIsConfCallMemberList) {
      details.namePrimary = getConferenceCallName(views.confCallLogInfos);
    }
    /**@}*/
    CharSequence formattedNumber =
        info.formattedNumber == null
            ? null
            : PhoneNumberUtilsCompat.createTtsSpannable(info.formattedNumber);
    details.updateDisplayNumber(mActivity, formattedNumber, isVoicemailNumber);

    views.displayNumber = details.displayNumber;
    views.accountHandle = accountHandle;
    details.accountHandle = accountHandle;

    if ((!TextUtils.isEmpty(info.name) || !TextUtils.isEmpty(info.nameAlternative))
            /**M:[VoLTE ConfCallLog]*/
            && (views.confCallId <= 0 || mIsConfCallMemberList) ) {
      details.contactUri = info.lookupUri;
      details.namePrimary = info.name;
      details.nameAlternative = info.nameAlternative;
      details.nameDisplayOrder = mContactsPreferences.getDisplayOrder();
      details.numberType = info.type;
      details.numberLabel = info.label;
      details.photoUri = info.photoUri;
      details.sourceType = info.sourceType;
      details.objectId = info.objectId;
      details.contactUserType = info.userType;
    }
    LogUtil.d(
        "CallLogAdapter.loadData",
        "position:%d, update geo info: %s, cequint caller id geo: %s, photo uri: %s <- %s",
        views.getAdapterPosition(),
        details.geocode,
        info.geoDescription,
        details.photoUri,
        info.photoUri);
    if (!TextUtils.isEmpty(info.geoDescription)) {
      details.geocode = info.geoDescription;
    }

    views.info = info;
    views.numberType = getNumberType(mActivity.getResources(), details);

    mCallLogListItemHelper.updatePhoneCallDetails(details);

    //*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
    details.callDate = FreemeDateTimeUtils.formatCallLogsTime(mActivity, details.date);
    //*/

    return true;
  }

  private String getNumberType(Resources res, PhoneCallDetails details) {
    // Label doesn't make much sense if the information is coming from CNAP or Cequint Caller ID.
    if (details.sourceType == ContactSource.Type.SOURCE_TYPE_CNAP
        || details.sourceType == ContactSource.Type.SOURCE_TYPE_CEQUINT_CALLER_ID) {
      return "";
    }
    // Returns empty label instead of "custom" if the custom label is empty.
    if (details.numberType == Phone.TYPE_CUSTOM && TextUtils.isEmpty(details.numberLabel)) {
      return "";
    }
    //return (String) Phone.getTypeLabel(res, details.numberType, details.numberLabel);
    /// M:Using new API for AAS phone number label lookup
    return (String)PhoneCompat.getTypeLabel(mActivity, details.numberType, details.numberLabel);
  }

  /**
   * Render item view given position. This is running on UI thread so DO NOT put any expensive
   * operation into it.
   */
  @MainThread
  protected void render(CallLogListItemViewHolder views, PhoneCallDetails details, long rowId) {
    Assert.isMainThread();
    if (rowId != views.rowId) {
      LogUtil.i(
          "CallLogAdapter.render",
          "rowId of viewHolder changed after load task is issued, aborting render");
      return;
    }

    // Default case: an item in the call log.
    views.primaryActionView.setVisibility(View.VISIBLE);
    views.workIconView.setVisibility(
        details.contactUserType == ContactsUtils.USER_TYPE_WORK ? View.VISIBLE : View.GONE);

    if (selectAllMode && views.voicemailUri != null) {
      selectedItems.put(getVoicemailId(views.voicemailUri), views.voicemailUri);
    }
    if (deselectAllMode && views.voicemailUri != null) {
      selectedItems.delete(getVoicemailId(views.voicemailUri));
    }
    if (views.voicemailUri != null
        && selectedItems.get(getVoicemailId(views.voicemailUri)) != null) {
      /*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
      views.checkBoxView.setVisibility(View.VISIBLE);
      /*/
      views.setCheckBoxViewVisiable(View.VISIBLE);
      //*/
      views.quickContactView.setVisibility(View.GONE);
    } else if (views.voicemailUri != null) {
      /*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
      views.checkBoxView.setVisibility(View.GONE);
      /*/
      views.setCheckBoxViewVisiable(View.GONE);
      //*/
      views.quickContactView.setVisibility(View.VISIBLE);
    }

    /// M: [Dialer Global Search] Highlight the search text @{
    if (DialerFeatureOptions.DIALER_GLOBAL_SEARCH && mUpperCaseQueryString != null
        && mUpperCaseQueryString.length > 0) {
      mCallLogListItemHelper.setHighlightedText(mUpperCaseQueryString);
    }
    /// @}

    mCallLogListItemHelper.setPhoneCallDetails(views, details);
    if (mCurrentlyExpandedRowId == views.rowId) {
      // In case ViewHolders were added/removed, update the expanded position if the rowIds
      // match so that we can restore the correct expanded state on rebind.
      mCurrentlyExpandedPosition = views.getAdapterPosition();
      views.showActions(true);
    } else {
      views.showActions(false);
    }
    views.dayGroupHeader.setVisibility(views.dayGroupHeaderVisibility);
    views.dayGroupHeader.setText(views.dayGroupHeaderText);

    /// M: for Plug-in @{
    ExtensionManager.getCallLogExtension()
            .setCallAccountForCallLogList(views.primaryActionView.getContext(),
                views.primaryActionView, views.accountHandle);
    /// @}

      //*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
      if (views.isMultiSelected) {
          views.setCheckBoxViewVisiable(View.VISIBLE);
          views.updateCheckBoxCheckStatus(mSelectedCallIds.containsKey(views.rowId));
      } else {
          views.setCheckBoxViewVisiable(View.GONE);
      }
      //*/
  }

  @Override
  public int getItemCount() {
    return super.getItemCount() + (mCallLogAlertManager.isEmpty() ? 0 : 1);
  }

  @Override
  public int getItemViewType(int position) {
    if (position == ALERT_POSITION && !mCallLogAlertManager.isEmpty()) {
      return VIEW_TYPE_ALERT;
    }
    return VIEW_TYPE_CALLLOG;
  }

  /**
   * Retrieves an item at the specified position, taking into account the presence of a promo card.
   *
   * @param position The position to retrieve.
   * @return The item at that position.
   */
  @Override
  public Object getItem(int position) {
    return super.getItem(position - (mCallLogAlertManager.isEmpty() ? 0 : 1));
  }

  @Override
  public long getItemId(int position) {
    Cursor cursor = (Cursor) getItem(position);
    if (cursor != null) {
      return cursor.getLong(CallLogQuery.ID);
    } else {
      return 0;
    }
  }

  @Override
  public int getGroupSize(int position) {
    return super.getGroupSize(position - (mCallLogAlertManager.isEmpty() ? 0 : 1));
  }

  protected boolean isCallLogActivity() {
    return mActivityType == ACTIVITY_TYPE_CALL_LOG;
  }

  /**
   * In order to implement the "undo" function, when a voicemail is "deleted" i.e. when the user
   * clicks the delete button, the deleted item is temporarily hidden from the list. If a user
   * clicks delete on a second item before the first item's undo option has expired, the first item
   * is immediately deleted so that only one item can be "undoed" at a time.
   */
  @Override
  public void onVoicemailDeleted(CallLogListItemViewHolder viewHolder, Uri uri) {
    mHiddenRowIds.add(viewHolder.rowId);
    // Save the new hidden item uri in case the activity is suspend before the undo has timed out.
    mHiddenItemUris.add(uri);

    collapseExpandedCard();
    notifyItemChanged(viewHolder.getAdapterPosition());
    // The next item might have to update its day group label
    notifyItemChanged(viewHolder.getAdapterPosition() + 1);
  }

  private void collapseExpandedCard() {
    mCurrentlyExpandedRowId = NO_EXPANDED_LIST_ITEM;
    mCurrentlyExpandedPosition = RecyclerView.NO_POSITION;
  }

  /** When the list is changing all stored position is no longer valid. */
  public void invalidatePositions() {
    mCurrentlyExpandedPosition = RecyclerView.NO_POSITION;
  }

  /** When the user clicks "undo", the hidden item is unhidden. */
  @Override
  public void onVoicemailDeleteUndo(long rowId, int adapterPosition, Uri uri) {
    mHiddenItemUris.remove(uri);
    mHiddenRowIds.remove(rowId);
    notifyItemChanged(adapterPosition);
    // The next item might have to update its day group label
    notifyItemChanged(adapterPosition + 1);
  }

  /** This callback signifies that a database deletion has completed. */
  @Override
  public void onVoicemailDeletedInDatabase(long rowId, Uri uri) {
    mHiddenItemUris.remove(uri);
  }

  /**
   * Retrieves the day group of the previous call in the call log. Used to determine if the day
   * group has changed and to trigger display of the day group text.
   *
   * @param cursor The call log cursor.
   * @return The previous day group, or DAY_GROUP_NONE if this is the first call.
   */
  private int getPreviousDayGroup(Cursor cursor) {
    // We want to restore the position in the cursor at the end.
    int startingPosition = cursor.getPosition();
    moveToPreviousNonHiddenRow(cursor);
    if (cursor.isBeforeFirst()) {
      cursor.moveToPosition(startingPosition);
      return CallLogGroupBuilder.DAY_GROUP_NONE;
    }
    int result = getDayGroupForCall(cursor.getLong(CallLogQuery.ID));
    cursor.moveToPosition(startingPosition);
    return result;
  }

  private void moveToPreviousNonHiddenRow(Cursor cursor) {
    while (cursor.moveToPrevious() && mHiddenRowIds.contains(cursor.getLong(CallLogQuery.ID))) {}
  }

  /**
   * Given a call Id, look up the day group that the call belongs to. The day group data is
   * populated in {@link com.android.dialer.app.calllog.CallLogGroupBuilder}.
   *
   * @param callId The call to retrieve the day group for.
   * @return The day group for the call.
   */
  @MainThread
  private int getDayGroupForCall(long callId) {
    Integer result = mDayGroups.get(callId);
    if (result != null) {
      return result;
    }
    return CallLogGroupBuilder.DAY_GROUP_NONE;
  }

  /**
   * Returns the call types for the given number of items in the cursor.
   *
   * <p>It uses the next {@code count} rows in the cursor to extract the types.
   *
   * <p>It position in the cursor is unchanged by this function.
   */
  private static int[] getCallTypes(Cursor cursor, int count) {
    int position = cursor.getPosition();
    int[] callTypes = new int[count];
    for (int index = 0; index < count; ++index) {
      callTypes[index] = cursor.getInt(CallLogQuery.CALL_TYPE);
      cursor.moveToNext();
    }
    cursor.moveToPosition(position);
    return callTypes;
  }

  /**
   * Determine the features which were enabled for any of the calls that make up a call log entry.
   *
   * @param cursor The cursor.
   * @param count The number of calls for the current call log entry.
   * @return The features.
   */
  private int getCallFeatures(Cursor cursor, int count) {
    int features = 0;
    int position = cursor.getPosition();
    for (int index = 0; index < count; ++index) {
      features |= cursor.getInt(CallLogQuery.FEATURES);
      cursor.moveToNext();
    }
    cursor.moveToPosition(position);
    return features;
  }

  /**
   * Sets whether processing of requests for contact details should be enabled.
   *
   * <p>This method should be called in tests to disable such processing of requests when not
   * needed.
   */
  @VisibleForTesting
  void disableRequestProcessingForTest() {
    // TODO: Remove this and test the cache directly.
    mContactInfoCache.disableRequestProcessing(true);
  }

  @VisibleForTesting
  void injectContactInfoForTest(String number, String countryIso, ContactInfo contactInfo) {
    // TODO: Remove this and test the cache directly.
    mContactInfoCache.injectContactInfoForTest(number, countryIso, contactInfo);
  }

  /**
   * Stores the day group associated with a call in the call log.
   *
   * @param rowId The row Id of the current call.
   * @param dayGroup The day group the call belongs in.
   */
  @Override
  @MainThread
  public void setDayGroup(long rowId, int dayGroup) {
    if (!mDayGroups.containsKey(rowId)) {
      mDayGroups.put(rowId, dayGroup);
    }
  }

  /** Clears the day group associations on re-bind of the call log. */
  @Override
  @MainThread
  public void clearDayGroups() {
    mDayGroups.clear();
  }

  /**
   * Retrieves the call Ids represented by the current call log row.
   *
   * @param cursor Call log cursor to retrieve call Ids from.
   * @param groupSize Number of calls associated with the current call log row.
   * @return Array of call Ids.
   */
  private long[] getCallIds(final Cursor cursor, final int groupSize) {
    // We want to restore the position in the cursor at the end.
    int startingPosition = cursor.getPosition();
    long[] ids = new long[groupSize];
    // Copy the ids of the rows in the group.
    for (int index = 0; index < groupSize; ++index) {
      ids[index] = cursor.getLong(CallLogQuery.ID);
      cursor.moveToNext();
    }
    cursor.moveToPosition(startingPosition);
    return ids;
  }

  /**
   * Determines the description for a day group.
   *
   * @param group The day group to retrieve the description for.
   * @return The day group description.
   */
  private CharSequence getGroupDescription(int group) {
    if (group == CallLogGroupBuilder.DAY_GROUP_TODAY) {
      return mActivity.getResources().getString(R.string.call_log_header_today);
    } else if (group == CallLogGroupBuilder.DAY_GROUP_YESTERDAY) {
      return mActivity.getResources().getString(R.string.call_log_header_yesterday);
    } else {
      return mActivity.getResources().getString(R.string.call_log_header_other);
    }
  }

  @NonNull
  private EnrichedCallManager getEnrichedCallManager() {
    return EnrichedCallComponent.get(mActivity).getEnrichedCallManager();
  }

  @NonNull
  private Lightbringer getLightbringer() {
    return LightbringerComponent.get(mActivity).getLightbringer();
  }

  @Override
  public void onLightbringerStateChanged() {
    notifyDataSetChanged();
  }

  public void onAllSelected() {
    selectAllMode = true;
    deselectAllMode = false;
    selectedItems.clear();
    for (int i = 0; i < getItemCount(); i++) {
      Cursor c = (Cursor) getItem(i);
      if (c != null) {
        Assert.checkArgument(CallLogQuery.VOICEMAIL_URI == c.getColumnIndex("voicemail_uri"));
        String voicemailUri = c.getString(CallLogQuery.VOICEMAIL_URI);
        selectedItems.put(getVoicemailId(voicemailUri), voicemailUri);
      }
    }
    updateActionBar();
    notifyDataSetChanged();
  }

  public void onAllDeselected() {
    selectAllMode = false;
    deselectAllMode = true;
    selectedItems.clear();
    updateActionBar();
    notifyDataSetChanged();
  }

  /** Interface used to initiate a refresh of the content. */
  public interface CallFetcher {

    void fetchCalls();
  }

  /** Interface used to allow single tap multi select for contact photos. */
  public interface OnActionModeStateChangedListener {

    void onActionModeStateChanged(boolean isEnabled);

    boolean isActionModeStateEnabled();
  }

  /** Interface used to hide the fragments. */
  public interface MultiSelectRemoveView {

    void showMultiSelectRemoveView(boolean show);

    void setSelectAllModeToFalse();

    void tapSelectAll();
  }

  /// M: [VoLTE ConfCallLog] For volte conference callLog @{
  private static String TAG = "CallLogAdapter";

  private boolean mIsConfCallMemberList = false;

  /**
   * Is this adapter used to show the conference call member list
   */
  public void setIsConfCallMemberList(boolean isConfCallMemberList) {
    mIsConfCallMemberList = isConfCallMemberList;
  }

  private ArrayList<String> getConferenceCallNumbers(Cursor cursor, int count) {
    int position = cursor.getPosition();
    ArrayList<String> numbers = new ArrayList<String>(count);
    for (int index = 0; index < count; ++index) {
      // add postDialDigits when get numbers
      final String postDialDigits = CompatUtils.isNCompatible() ?
              cursor.getString(CallLogQuery.POST_DIAL_DIGITS) : "";
      numbers.add(cursor.getString(CallLogQuery.NUMBER) + postDialDigits);
      cursor.moveToNext();
    }
    cursor.moveToPosition(position);
    return numbers;
  }

  private long getConferenceCallDate(Cursor cursor, int count) {
    int position = cursor.getPosition();
    long minDate = cursor.getLong(CallLogQuery.DATE);
    for (int index = 1; index < count; ++index) {
      cursor.moveToNext();
      long date = cursor.getLong(CallLogQuery.DATE);
      if (minDate > date) {
        minDate = date;
      }
    }
    cursor.moveToPosition(position);
    return minDate;
  }

  /**
   * copy cursor info to ConfCallLogInfo
   * avoid concurrent read calllog cursor
   */
  private ArrayList<ConfCallLogInfo> getConfCallLogInfos(Cursor cursor, int count,
      final PhoneCallDetails details) {
    int position = cursor.getPosition();

    final ArrayList<ConfCallLogInfo> confCallLogs = new ArrayList<ConfCallLogInfo>();
    for (int index = 0; index < count; ++index) {
      ConfCallLogInfo cc = new ConfCallLogInfo(cursor);
      confCallLogs.add(cc);
      cursor.moveToNext();
    }
    cursor.moveToPosition(position);
    return confCallLogs;
  }

  private String getConferenceCallName(ArrayList<ConfCallLogInfo> confCallLogs) {
    if(confCallLogs == null) {
      return "";
    }
    final ArrayList<CharSequence> names = new ArrayList<CharSequence>(confCallLogs.size());
    for(final ConfCallLogInfo cc:confCallLogs) {
      ContactInfo info = ContactInfo.EMPTY;
      if (PhoneNumberHelper.canPlaceCallsTo(cc.number, cc.numberPresentation)) {
        // Lookup contacts with this number
        info = mContactInfoCache.getValue(cc.number + cc.postDialDigits,
              cc.countryIso, cc.cachedContactInfo, false);
      }
      if (TextUtils.isEmpty(info.name)) {
        CharSequence formattedNumber = info.formattedNumber == null
                ? null : PhoneNumberUtilsCompat.createTtsSpannable(info.formattedNumber);
                CharSequence displayNumber = PhoneNumberDisplayUtil.getDisplayNumber(
                mActivity,
                cc.number,
                cc.numberPresentation,
                formattedNumber,
                cc.postDialDigits,
                false).toString();
                names.add(displayNumber);
      } else {
        names.add(info.name);
      }
    }
    return DialerUtils.join(names).toString();
  }

  /// M: [Dialer Global Search] New Feature CallLogSearch @{
  private char[] mUpperCaseQueryString;

  // Add for call log search feature
  public void setQueryString(String queryString) {
    if (TextUtils.isEmpty(queryString)) {
      mUpperCaseQueryString = null;
    } else {
      mUpperCaseQueryString = queryString.toUpperCase().toCharArray();
    }
  }
  /// @}

    //*/ freeme.zhaozehong, 20180222. for freemeOS, UI redesign
    private static final String KEY_MULTI_MODE = "KEY_MULTI_MODE";
    private static final String KEY_MULTI_SELECTED_DATA = "KEY_MULTI_SELECTED_DATA";

    private IFreemeMultiSelectCallBack mMultiSelectCallBack = new IFreemeMultiSelectCallBack() {
        @Override
        public void isInMulitMode(boolean isMultiMode) {
            setMultiSelected(isMultiMode);
            notifyDataSetChanged();
        }

        @Override
        public void onSelectedCount(int count) {

        }
    };

    private IFreemeMultiSelectCallBack mFreemeMultiSelectCallBack;
    public void setFreemeMultiSelectCallBack(IFreemeMultiSelectCallBack multiSelectCallBack) {
        this.mFreemeMultiSelectCallBack = multiSelectCallBack;
    }

    private boolean isMultiSelected;
    public void setMultiSelected(boolean multiSelected) {
        if(isMultiSelected != multiSelected) {
            isMultiSelected = multiSelected;
            if (mFreemeMultiSelectCallBack != null) {
                mFreemeMultiSelectCallBack.isInMulitMode(isMultiSelected);
                mFreemeMultiSelectCallBack.onSelectedCount(0);
            }
            mSelectedCallIds.clear();
            for (CallLogListItemViewHolder holder : holderMap.keySet()) {
                holder.updateCheckBoxCheckStatus(false);
            }
        }
    }

    public boolean isMultiSelected() {
        return isMultiSelected;
    }

    private HashMap<Long, long[]> mSelectedCallIds = new HashMap<Long, long[]>();
    private final ArrayMap<CallLogListItemViewHolder, Integer> holderMap = new ArrayMap<>();

    public void allSelecteOrNot() {
        if (isMultiSelected) {
            final int selectCount = mSelectedCallIds.size();
            final int size = getItemCount();
            mSelectedCallIds.clear();
            if (selectCount != size) {
                Cursor c;
                for (int i = 0; i < size; i++) {
                    c = (Cursor) getItem(i);
                    mSelectedCallIds.put(c.getLong(CallLogQuery.ID) /* rowId */,
                            getCallIds(c, getGroupSize(i)) /* callIds */);
                }
            }
            notifyDataSetChanged();

            mFreemeMultiSelectCallBack.onSelectedCount(mSelectedCallIds.size());
        }
    }

    public HashMap<Long, long[]> getSelectedCallIds() {
        return mSelectedCallIds;
    }

    private int mCallTypeFilter = CallLogQueryHandler.CALL_TYPE_ALL;

    public void setCallTypeFilter(int filter) {
        this.mCallTypeFilter = filter;
    }
    //*/

    //*/ freeme.zhaozehong, 20180627. for freemeOS, replace ContextMenu with AlertDialog
    private Dialog mContextMenuDialog;

    private void showBottomMenu(CallLogListItemViewHolder viewHolder) {
        if (viewHolder == null) {
            return;
        }
        if (isMultiSelected) {
            return;
        }

        /** M: [VoLTE ConfCallLog] For Volte Conference callLog
         conference call will not show context menu//@{ */
        if (viewHolder.confCallNumbers != null || !viewHolder.isLoaded) {
            return;
        }/** @} */

        if (TextUtils.isEmpty(viewHolder.number)) {
            return;
        }

        CharSequence title = PhoneNumberUtilsCompat.createTtsSpannable(
                BidiFormatter.getInstance().unicodeWrap(viewHolder.nameOrNumber, TextDirectionHeuristics.LTR));
        if (viewHolder.callType == CallLog.Calls.VOICEMAIL_TYPE) {
            title = mActivity.getResources().getString(R.string.voicemail);
        }

        final ArrayList<String> resList = new ArrayList<>();
        final ContextMenu menu = new ContextMenuBuilder(mActivity);

        boolean isVoicemailNumber = mCallLogCache.isVoicemailNumber(viewHolder.accountHandle, viewHolder.number);
        if (PhoneNumberHelper.canPlaceCallsTo(viewHolder.number, viewHolder.numberPresentation)
                && !isVoicemailNumber
                && !PhoneNumberHelper.isSipNumber(viewHolder.number)) {
            createMenuData(resList, menu, R.id.freeme_menu_item_send_sms,
                    R.string.call_log_action_send_message);
        }

        String lookup = UriUtils.getLookupKeyFromUri(viewHolder.info.lookupUri);
        if (TextUtils.isEmpty(lookup) && !isVoicemailNumber) {
            createMenuData(resList, menu, R.id.freeme_menu_item_add_new_contact,
                    R.string.search_shortcut_create_new_contact);

            createMenuData(resList, menu, R.id.freeme_menu_item_add_exist_contact,
                    R.string.search_shortcut_add_to_contact);
        }

        createMenuData(resList, menu, R.id.context_menu_copy_to_clipboard,
                R.string.action_copy_number_text);

        if (viewHolder.callType == CallLog.Calls.VOICEMAIL_TYPE
                && viewHolder.phoneCallDetailsViews.voicemailTranscriptionView.length() > 0) {
            createMenuData(resList, menu, R.id.context_menu_copy_transcript_to_clipboard,
                    R.string.copy_transcript_text);
        }

        createMenuData(resList, menu, R.id.freeme_menu_item_delete_calllog,
                R.string.delete_from_call_log_list);

        createMenuData(resList, menu, R.id.freeme_menu_item_mulit_delete_calllog,
                R.string.freeme_call_logs_mulit_delete);

        Logger.get(mActivity).logScreenView(ScreenEvent.Type.CALL_LOG_CONTEXT_MENU, mActivity);

        String[] menuItems = resList.toArray(new String[0]);
        mContextMenuDialog = new android.app.AlertDialog.Builder(mActivity)
                .setTitle(title)
                .setItems(menuItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        MenuItem item = menu.getItem(which);
                        if (item != null) {
                            viewHolder.onMenuItemClick(item);
                        }
                    }
                })
                .setFreemeDialogOption(DialogInterface.FREEME_DIALOG_OPTION_BOTTOM)
                .setCancelable(true)
                .create();
        mContextMenuDialog.show();
    }

    private void createMenuData(final ArrayList<String> resList, final ContextMenu menu,
                                final int menuIdRes, final int menuTextRes) {
        resList.add(mActivity.getString(menuTextRes));
        menu.add(ContextMenu.NONE, menuIdRes, ContextMenu.NONE, menuTextRes);
    }

    private void hideButtomMenu() {
        if (mContextMenuDialog != null) {
            mContextMenuDialog.dismiss();
        }
    }
    //*/
}
