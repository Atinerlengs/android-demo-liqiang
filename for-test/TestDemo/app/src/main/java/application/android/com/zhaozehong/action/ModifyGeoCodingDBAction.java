package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.activities.ModifyGeoCodingActivity;

public class ModifyGeoCodingDBAction extends Action {

    public ModifyGeoCodingDBAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Modify GeoCoding";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, ModifyGeoCodingActivity.class));
    }
}
