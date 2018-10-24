package application.android.com.zhaozehong.action;

import android.app.Activity;

import application.android.com.zhaozehong.database.DatabaseHelper;

public class DatabaseAction extends Action {

    public DatabaseAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Create DB";
    }

    @Override
    public void doAction() {
        DatabaseHelper helper = new DatabaseHelper(mActivity);
        helper.getReadableDatabase();
    }
}
