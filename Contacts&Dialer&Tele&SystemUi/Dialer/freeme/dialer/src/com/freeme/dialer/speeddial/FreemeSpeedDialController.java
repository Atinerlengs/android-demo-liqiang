package com.freeme.dialer.speeddial;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.dialer.R;
import com.android.dialer.util.DialerUtils;

import com.freeme.dialer.speeddial.provider.FreemeSpeedDial;

public class FreemeSpeedDialController {
    private static final String TAG = "FreemeSpeedDialController";
    private static FreemeSpeedDialController sMe;
    private Context mContext;
    private static final String FREEME_SPEED_DIAL = "com.freeme.intent.ACTION_FREEME_SPEEDDIAL";

    private void FreemeSpeedDialController() {
    }

    public static FreemeSpeedDialController getInstance() {
        if (sMe == null) {
            sMe = new FreemeSpeedDialController();
        }
        return sMe;
    }

    public void handleKeyLongProcess(Activity activity, int key) {
        mContext = activity;
        Cursor cursor = mContext.getContentResolver().query(FreemeSpeedDial.Numbers.CONTENT_URI,
                new String[]{FreemeSpeedDial.Numbers._ID, FreemeSpeedDial.Numbers.NUMBER},
                FreemeSpeedDial.Numbers._ID + " = " + key, null, null);

        String number = "";
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                number = cursor.getString(1);
            }
            cursor.close();
        }
        if (TextUtils.isEmpty(number)) {
            showSpeedDialConfirmDialog();
        } else {
            final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts("tel",
                    number, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            DialerUtils.startActivityWithErrorToast(mContext, intent);
        }
    }

    public void enterSpeedDial(Context fcnx) {
        final Intent intent = new Intent();
        intent.setAction(FREEME_SPEED_DIAL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        fcnx.startActivity(intent);
    }

    public void showSpeedDialConfirmDialog() {
        AlertDialog confirmDialog = new AlertDialog.Builder(mContext)
                .setTitle(mContext.getString(R.string.freeme_speeddial_call_speed_dial))
                .setMessage(mContext.getString(R.string.freeme_speeddial_dialog_no_speed_dial_number_message))
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                enterSpeedDial(mContext);
                            }
                        }).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create();
        confirmDialog.show();
    }
}


