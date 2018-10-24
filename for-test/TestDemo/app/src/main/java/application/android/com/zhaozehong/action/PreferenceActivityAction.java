package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.activities.TestPreferenceActivity;

public class PreferenceActivityAction extends Action {

    public PreferenceActivityAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "PreferenceActivity";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, TestPreferenceActivity.class));
    }
}
