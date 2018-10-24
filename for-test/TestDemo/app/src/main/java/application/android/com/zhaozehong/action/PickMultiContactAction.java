package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.contact.PickMutilContactsActivity;

public class PickMultiContactAction extends Action {

    public PickMultiContactAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Pick Multi Contact";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, PickMutilContactsActivity.class));
    }
}
