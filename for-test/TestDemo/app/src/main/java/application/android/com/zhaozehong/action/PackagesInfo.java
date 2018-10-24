package application.android.com.zhaozehong.action;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackagesInfo extends Action {

    private static final String TAG = "PackagesInfo";
    private final static String ACTION = "android.intent.action.MAIN";
    private final static String CATEGORY = "android.intent.category.LAUNCHER";

    private PackageManager mPackageManager;
    private UserManager mUserManager;

    public PackagesInfo(Activity activity) {
        super(activity);
        mPackageManager = activity.getPackageManager();
        mUserManager = UserManager.get(activity);
    }

    @Override
    public String getName() {
        return "PackagesInfo";
    }

    @Override
    public void doAction() {
        try {
            List<ResolveInfo> infos = new ArrayList();
            Intent mainIntent = new Intent(ACTION);
            mainIntent.addCategory(CATEGORY);
            for (UserInfo user : mUserManager.getProfiles(UserHandle.myUserId())) {
                infos.addAll(mPackageManager.queryIntentActivitiesAsUser(
                        mainIntent, 0, user.id));
            }

            if (!infos.isEmpty()) {
                for (ResolveInfo info : infos) {
                    String pkgName = info.activityInfo.packageName;
                    String appName = (String) info.loadLabel(mPackageManager);
                    Log.e("zhaozehong", "[PackagesInfo] appName: " + appName + ", pkgName: " + pkgName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "some unknown error happened.");
            e.printStackTrace();
        }
    }
}
