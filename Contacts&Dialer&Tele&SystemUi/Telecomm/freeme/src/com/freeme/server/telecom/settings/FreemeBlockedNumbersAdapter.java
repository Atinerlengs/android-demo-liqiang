package com.freeme.server.telecom.settings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.BlockedNumberContract;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.server.telecom.R;
import com.android.server.telecom.settings.BlockedNumbersUtil;

public class FreemeBlockedNumbersAdapter extends SimpleCursorAdapter {
    public FreemeBlockedNumbersAdapter(Context context, int layout, Cursor c,
                                       String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        super.bindView(view, context, cursor);
        final String rawNumber = cursor.getString(cursor.getColumnIndex(
                BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER));
        final String formattedNumber = BlockedNumbersUtil.formatNumber(rawNumber);
        TextView numberView = (TextView) view.findViewById(R.id.blocked_number);
        Spannable numberSpannable = new SpannableString(formattedNumber);
        PhoneNumberUtils.addTtsSpan(numberSpannable, 0, numberSpannable.length());
        numberView.setText(numberSpannable);

        final String rawName = cursor.getString(cursor.getColumnIndex(
                BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NAME));
        TextView nameView = (TextView) view.findViewById(R.id.blocked_name);
        if (rawName == null) {
            nameView.setVisibility(View.GONE);
        } else {
            nameView.setVisibility(View.VISIBLE);
            nameView.setText(rawName);
        }

        View deleteButton = view.findViewById(R.id.delete_blocked_number);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteBlockedNumberDialog(context, rawNumber, formattedNumber);
            }
        });
    }

    private void showDeleteBlockedNumberDialog(final Context context, final String rawNumber,
                                               final String formattedNumber) {
        String message = context.getString(R.string.unblock_dialog_body, formattedNumber);
        int startingPosition = message.indexOf(formattedNumber);
        Spannable messageSpannable = new SpannableString(message);
        PhoneNumberUtils.addTtsSpan(messageSpannable, startingPosition,
                startingPosition + formattedNumber.length());
        new AlertDialog.Builder(context)
                .setMessage(messageSpannable)
                .setPositiveButton(R.string.unblock_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteBlockedNumber(context, rawNumber);
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }
                )
                .create()
                .show();
    }

    private void deleteBlockedNumber(Context context, String number) {
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.delete(BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + "=?",
                new String[]{number});
        BlockedNumbersUtil.showToastWithFormattedNumber(mContext,
                R.string.blocked_numbers_number_unblocked_message, number);
    }
}
