package com.freeme.dialer.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;

import com.android.dialer.R;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;

public class FreemeQuickContactBadge extends QuickContactBadge implements View.OnClickListener {


    public FreemeQuickContactBadge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(this);
    }

    public FreemeQuickContactBadge(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FreemeQuickContactBadge(Context context) {
        this(context, null);
    }

    private String mPhoneNumber;

    public void assignPhoneNumber(String number) {
        mPhoneNumber = number;
    }

    @Override
    public void onClick(View v) {
        if (mPhoneNumber != null) {
            showDialog();
        } else {
            super.onClick(v);
        }
    }


    private void showDialog() {
        CharSequence[] items = {mContext.getString(R.string.search_shortcut_create_new_contact),
                mContext.getString(R.string.search_shortcut_add_to_contact)};
        String title = mContext.getString(R.string.dialer_shortcut_add_contact_short) + ":" + mPhoneNumber;
        AlertDialog dialog = new AlertDialog.Builder(mContext).setTitle(title).setItems(items,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        if (which == 0) {
                            intent = IntentUtil.getNewContactIntent(mPhoneNumber);
                        } else {
                            intent = IntentUtil.getAddToExistingContactIntent(mPhoneNumber);
                        }
                        if (intent != null) {
                            DialerUtils.startActivityWithErrorToast(mContext, intent);
                        }
                    }
                }).create();
        dialog.show();
        return;
    }
}
