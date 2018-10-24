package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.activities.GridLayoutActivity;

public class GridLayoutAction extends Action {

    public GridLayoutAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "GridLayout";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, GridLayoutActivity.class));
    }
}
