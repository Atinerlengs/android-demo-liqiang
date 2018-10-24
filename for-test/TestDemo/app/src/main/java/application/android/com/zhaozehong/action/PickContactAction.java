package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.contact.PickContactsActivity;

public class PickContactAction extends Action {

    public PickContactAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Pick Signal Contact";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, PickContactsActivity.class));
    }
}
