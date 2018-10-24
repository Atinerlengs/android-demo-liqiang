package application.android.com.zhaozehong.contact;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import application.android.com.zhaozehong.demoapplication.R;

public class PickMutilContactsActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_multi_pick_contacts);
    }

    public void onPickContacts(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(ContactsContract.Contacts.CONTENT_TYPE));
    }

    public void onPickPeople(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(Contacts.People.CONTENT_TYPE));
    }

    public void onPickPhone(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE));
    }

    public void onPickPhone2(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType("vnd.android.cursor.item/phone_v2"));
    }

    public void onPickPhones(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(Contacts.Phones.CONTENT_TYPE));
    }

    public void onPickStructuredPostal(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_TYPE));
    }

    public void onPickContactMethods(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(Contacts.ContactMethods.CONTENT_POSTAL_TYPE));
    }

    public void onPickEmail(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE));
    }

    public void onPickRcseCapabilities(View view) {
        startActivity(new Intent("mediatek.intent.action.contacts.list.PICKMULTICONTACTS")
                .setType("vnd.android.cursor.item/com.orangelabs.rcse.capabilities"));
    }
}
