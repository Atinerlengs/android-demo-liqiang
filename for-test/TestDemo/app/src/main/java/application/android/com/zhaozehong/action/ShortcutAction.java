package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;

import application.android.com.zhaozehong.activities.ShortcutActivity;

public class ShortcutAction extends Action {

    public ShortcutAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Shortcut";
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, ShortcutActivity.class));
    }
}
