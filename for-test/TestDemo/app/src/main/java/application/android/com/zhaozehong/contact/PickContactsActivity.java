package application.android.com.zhaozehong.contact;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;

import application.android.com.zhaozehong.demoapplication.R;

public class PickContactsActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pick_contacts);
    }

    public void onPickContacts(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.Contacts.CONTENT_TYPE));
    }

    public void onPickPeople(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(Contacts.People.CONTENT_TYPE));
    }

    public void onPickPhone(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE));
    }

    public void onPickPhones(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(Contacts.Phones.CONTENT_TYPE));
    }

    public void onPickStructuredPostal(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_TYPE));
    }

    public void onPickContactMethods(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(Contacts.ContactMethods.CONTENT_POSTAL_TYPE));
    }

    public void onPickEmail(View view) {
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE));
    }

    public void onPickGroups(View view) {
        ArrayList<String> arrayList= new ArrayList<>();
        startActivity(new Intent(Intent.ACTION_PICK)
                .setType(ContactsContract.Groups.CONTENT_TYPE)
                .putExtra("com.android.contacts.extra.GROUP_ACCOUNT_NAME",
                        "Phone")
                .putExtra("com.android.contacts.extra.GROUP_ACCOUNT_DATA_SET",
                        "")
                .putExtra("com.android.contacts.extra.GROUP_ACCOUNT_TYPE",
                        "Local Phone Account")
                .putStringArrayListExtra("com.android.contacts.extra.GROUP_CONTACT_IDS",
                        arrayList));
    }
}
