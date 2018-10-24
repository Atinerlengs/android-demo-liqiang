package com.freeme.contacts.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.android.contacts.R;

/**
 * freeme.linqingwei, 20180321.
 * <p>
 * just for start ContactsActivity in the Dialer.
 */

public class FreemeBootActivity extends Activity {
    private static final String FREEME_ACTION_LIST_CONTACTS = "freeme.intent.action.LIST_DEFAULT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterContacts();
    }

    private void enterContacts() {
        finish();
        try {
            startActivity(new Intent(FREEME_ACTION_LIST_CONTACTS));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
