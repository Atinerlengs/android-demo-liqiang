package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.activities.TabActivity;

public class TabActivityAction extends Action {

    public TabActivityAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Tab Activity";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, TabActivity.class));
    }
}
