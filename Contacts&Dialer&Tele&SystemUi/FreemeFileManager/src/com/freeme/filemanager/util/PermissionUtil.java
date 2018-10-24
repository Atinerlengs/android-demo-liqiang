package com.freeme.filemanager.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import com.freeme.filemanager.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {
    private static final int CHECK_PERMISSIONS_REQUEST = 10010;

    private static String[] PERMISSIONS_STORAGE = new String[]{
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static String[] PERMISSIONS = new String[]{
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public static void checkSecurityPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissionForM(activity);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void checkPermissionForM(Activity activity) {
        List<String> perList = new ArrayList<>();
        int length = PERMISSIONS.length;
        for (int i = 0; i < length; i++) {
            if (activity.getApplicationContext().checkPermission(PERMISSIONS[i],
                    android.os.Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
                perList.add(PERMISSIONS[i]);
            }
        }

        int size = perList.size();
        if (size > 0) {
            String[] permisGroup = (String[]) perList.toArray(new String[size]);
            activity.requestPermissions(permisGroup, CHECK_PERMISSIONS_REQUEST);
        }
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CHECK_PERMISSIONS_REQUEST:
                String str = "";
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)  {
                            if (!activity.shouldShowRequestPermissionRationale(permissions[i])) {
                                showDialog(activity);
                                return;
                            }
                        } else {
                            showDialog(activity);
                            return;
                        }
                        activity.finish();
                    }
                }
                break;
        }
    }

    public static boolean hasSecurityPermissions(Context context) {
        for (String permission : PERMISSIONS) {
            if (context.checkPermission(permission, android.os.Process.myPid(), Process.myUid())
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasStoragePermissions(Context context) {
        for (String permission : PERMISSIONS_STORAGE) {
            if (context.checkPermission(permission, android.os.Process.myPid(), Process.myUid())
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void showDialog(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.need_permission);
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        });
        builder.show();
        builder.show().setCanceledOnTouchOutside(false);
    }

}
