package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import java.util.ArrayList;

public abstract class Action {

    public static final int REQUEST_CODE_REQUEST_PERMISSIONS = 1000;
    public static final int REQUEST_CODE_REQUEST_PERMISSIONS_DIALACTION
            = REQUEST_CODE_REQUEST_PERMISSIONS + 1;
    public static final int REQUEST_CODE_REQUEST_PERMISSIONS_FRAGMENTACTIVITYACTION
            = REQUEST_CODE_REQUEST_PERMISSIONS_DIALACTION + 1;

    protected Activity mActivity;

    public Action(Activity activity) {
        mActivity = activity;
    }

    public abstract String getName();

    public void onClick() {
        doAction();
    }

    public abstract void doAction();

    public boolean onBackPress() {
        return false;
    }

    public void onDestroy() {

    }

    public boolean requestPermissions(String[] permissions, int requestCode) {
        ArrayList<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (mActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                list.add(permission);
            }
        }
        if (list.size() > 0) {
            mActivity.requestPermissions(list.toArray(new String[0]), requestCode);
            return true;
        }
        return false;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
    }

    public boolean isAllGranted(@NonNull int[] grantResults) {
        boolean allGrant = true;
        for (int flag : grantResults) {
            if (flag != PackageManager.PERMISSION_GRANTED) {
                allGrant = false;
            }
        }
        return allGrant;
    }
}
