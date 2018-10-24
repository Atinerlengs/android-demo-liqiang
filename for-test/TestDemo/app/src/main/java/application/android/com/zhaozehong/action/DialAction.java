package application.android.com.zhaozehong.action;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.telecom.PhoneAccount;
import android.widget.EditText;

public class DialAction extends Action {

    private final String[] mPermissions = new String[]{
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.CALL_PHONE
    };

    public DialAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Dial or Call";
    }

    @Override
    public void onClick() {
        if (!requestPermissions(mPermissions, REQUEST_CODE_REQUEST_PERMISSIONS_DIALACTION)) {
            doAction();
        }
    }

    @Override
    public void doAction() {
        final EditText et = new EditText(mActivity);
        et.setHint("请输入电话号码");
        new AlertDialog.Builder(mActivity)
                .setTitle("拨号")
                .setView(et)
                .setPositiveButton("拨打", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.fromParts(PhoneAccount.SCHEME_TEL, et.getText().toString(),
                                null));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mActivity.startActivity(intent);
                    }
                })
                .setNeutralButton("拨号盘", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.fromParts(PhoneAccount.SCHEME_TEL, et.getText().toString(),
                                null));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mActivity.startActivity(intent);
                    }
                })
                .setNegativeButton("取消", null)
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUEST_PERMISSIONS_DIALACTION
                && isAllGranted(grantResults)) {
            doAction();
        }
    }
}
