package application.android.com.zhaozehong.action;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import application.android.com.zhaozehong.fragments.TestFragmentActivity;

public class FragmentActivityAction extends Action {

    private final String[] mPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    public FragmentActivityAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "FragmentActivity";
    }

    @Override
    public void onClick() {
        if (!requestPermissions(mPermissions,
                REQUEST_CODE_REQUEST_PERMISSIONS_FRAGMENTACTIVITYACTION)) {
            doAction();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUEST_PERMISSIONS_FRAGMENTACTIVITYACTION
                && isAllGranted(grantResults)) {
            doAction();
        }
    }

    @Override
    public void doAction() {
        mActivity.startActivity(new Intent(mActivity, TestFragmentActivity.class));
        mActivity.overridePendingTransition(0, 0);
    }
}
