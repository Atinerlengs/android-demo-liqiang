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

package com.android.server.telecom.components;

import com.android.server.telecom.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.telecom.Log;
import android.telephony.SubscriptionManager;
import android.view.WindowManager;

// TODO: Needed for move to system service: import com.android.internal.R;

/**
 * Used to display an error dialog from within the Telecom service when an outgoing call fails
 */
public class ErrorDialogActivity extends Activity {
    private static final String TAG = ErrorDialogActivity.class.getSimpleName();

    public static final String SHOW_MISSING_VOICEMAIL_NO_DIALOG_EXTRA = "show_missing_voicemail";
    public static final String ERROR_MESSAGE_ID_EXTRA = "error_message_id";
    public static final String ERROR_MESSAGE_STRING_EXTRA = "error_message_string";
    ///M: make call in roaming @{
    public static final String SHOW_ROAMING_ALERT_DIALOG = "show_roaming_alert_dialog";
    public static final String ACTION_MAKE_CALL = "com.mediatek.telecom.plugin.MAKE_CALL";
    public static final String CONTACT_NUMBER = "contact_number";
    public static final String IS_VIDEO_CALL = "is_video_call";
    public static final String IS_DOMESTIC_ROAMING = "is_domestic_roaming";
    public static final String SUB_ID_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
    /// @}

    /**
     * Intent action to bring up Voicemail Provider settings.
     */
    public static final String ACTION_ADD_VOICEMAIL =
            "com.android.phone.CallFeaturesSetting.ADD_VOICEMAIL";

    ///M: add a field to manage the different dialogs
    private AlertDialog mGeneralErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean showVoicemailDialog = getIntent().getBooleanExtra(
                SHOW_MISSING_VOICEMAIL_NO_DIALOG_EXTRA, false);

        if (showVoicemailDialog) {
            showMissingVoicemailErrorDialog();
        }  else if (getIntent().getCharSequenceExtra(ERROR_MESSAGE_STRING_EXTRA) != null) {
            final CharSequence error = getIntent().getCharSequenceExtra(
                    ERROR_MESSAGE_STRING_EXTRA);
            showGenericErrorDialog(error);
        } else {
            final int error = getIntent().getIntExtra(ERROR_MESSAGE_ID_EXTRA, -1);
            if (error == -1) {
                Log.w(TAG, "ErrorDialogActivity called with no error type extra.");
                finish();
            } else {
                showGenericErrorDialog(error);
            }
        }
    }

    private void showGenericErrorDialog(CharSequence msg) {
        final DialogInterface.OnClickListener clickListener;
        final DialogInterface.OnCancelListener cancelListener;

        clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        };

        final AlertDialog errorDialog = new AlertDialog.Builder(this)
                .setMessage(msg).setPositiveButton(android.R.string.ok, clickListener)
                        .setOnCancelListener(cancelListener).create();

        errorDialog.show();
    }

    private void showGenericErrorDialog(int resid) {
        final CharSequence msg = getResources().getText(resid);
        showGenericErrorDialog(msg);
    }

    private void showMissingVoicemailErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_vm_number)
                .setMessage(R.string.no_vm_number_msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }})
                .setNegativeButton(R.string.add_vm_number_str,
                        new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    addVoiceMailNumberPanel(dialog);
                                }})
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }}).show();
    }


    private void addVoiceMailNumberPanel(DialogInterface dialog) {
        if (dialog != null) {
            dialog.dismiss();
        }

        // Navigate to the Voicemail setting in the Call Settings activity.
        Intent intent = new Intent(ACTION_ADD_VOICEMAIL);
        startActivity(intent);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        // Don't show the return to previous task animation to avoid showing a black screen.
        // Just dismiss the dialog and undim the previous activity immediately.
        overridePendingTransition(0, 0);
    }

    ///M: MTK SPRINT roaming feature @{
    @Override
    protected void onResume() {
        super.onResume();
        if (mGeneralErrorDialog != null) {
            mGeneralErrorDialog.dismiss();
        }
        if (getIntent().getBooleanExtra(SHOW_ROAMING_ALERT_DIALOG, false)) {
            showRoamingAlertDialog();
            return;
        }
    }

    private void showRoamingAlertDialog() {
        Log.d(TAG, "showRoamingAlertDialog");
        AlertDialog.Builder dialogBuild = new AlertDialog.Builder(this);
        if (getIntent().getBooleanExtra(IS_DOMESTIC_ROAMING, false)) {
            dialogBuild.setMessage(R.string.roaming_network_dom);
        } else {
            dialogBuild.setMessage(R.string.roaming_network_int);
        }
        dialogBuild.setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.modify_roaming_settings, new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "positive button clicked, show roaming settings");
                        showRoamingSettings();
                        onDialogDismissed();
                        finish();
                    }
                })
                .setNegativeButton(R.string.continue_roaming, new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "negative button clicked, make call in roaming");
                        makeCall();
                        onDialogDismissed();
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onDialogDismissed();
                        finish();
                    }
                })
                .create();
        mGeneralErrorDialog = dialogBuild.show();
        mGeneralErrorDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mGeneralErrorDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    private void onDialogDismissed() {
        if (mGeneralErrorDialog != null) {
            mGeneralErrorDialog.dismiss();
            mGeneralErrorDialog = null;
        }
    }

    private void showRoamingSettings() {
        Intent roamingSettingsIntent = new Intent();
        roamingSettingsIntent.setAction(
                "com.mediatek.services.telephony.ACTION_SHOW_ROAMING_SETTINGS");
        roamingSettingsIntent.putExtra(SUB_ID_EXTRA,
                SubscriptionManager.getDefaultSubscriptionId());
        startActivity(roamingSettingsIntent);
    }

    private void makeCall() {
        Intent broadcastIntent = new Intent(ACTION_MAKE_CALL);
        getApplicationContext().sendBroadcast(broadcastIntent);
        String contactNumber = getIntent().getStringExtra(CONTACT_NUMBER);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(android.net.Uri.parse("tel:" + contactNumber));
        if (getIntent().getBooleanExtra(IS_VIDEO_CALL, false)) {
            callIntent.putExtra(android.telecom.TelecomManager.EXTRA_START_CALL_WITH_VIDEO_STATE,
                    android.telecom.VideoProfile.STATE_BIDIRECTIONAL);
        }
        startActivity(callIntent);
    }
    /// @}
}
