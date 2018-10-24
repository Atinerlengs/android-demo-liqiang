package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.activities.TranslucentActivity;

public class TranslucentActivityAction extends Action {

    public TranslucentActivityAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "TranslucentActivity";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, TranslucentActivity.class));
    }
}
