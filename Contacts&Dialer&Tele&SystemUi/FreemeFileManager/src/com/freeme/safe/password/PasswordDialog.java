package com.freeme.safe.password;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.utils.SafeConstants;

class PasswordDialog {

    private boolean mIsFirst;
    private boolean mIsModify;
    private boolean mIsNeedResult;
    private boolean mIsOpenSafe;

    private AlertDialog mDialog;

    void createPasswordDialog(final Activity activity, final int password_mode) {
        final Builder builder = new Builder(activity);
        int arrayId = getTitleArrayId(password_mode);
        final String[] itemArraryId = activity.getResources().getStringArray(arrayId);
        builder.setItems(arrayId, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String clickItemString = itemArraryId[which];
                if (clickItemString.equals(getStringFormId(activity, R.string.set_unlock_pattern_title))) {
                    startPatternActivity(activity);
                } else if (clickItemString.equals(getStringFormId(activity, R.string.set_unlock_pin_title))) {
                    startPasswordActivity(activity);
                } else if (clickItemString.equals(getStringFormId(activity, R.string.set_unlock_password_title))) {
                    startComplexActivity(activity);
                }
            }
        });
        Util.setFreemeDialogOption(builder, Util.FREEME_DIALOG_OPTION_BOTTOM);
        mDialog = builder.create();
    }
    private String getStringFormId(Activity activity, int resId) {
        return activity.getResources().getString(resId);
    }

    void showDialog() {
        if (mDialog != null) {
            mDialog.show();
        }
    }

    void setState(boolean first, boolean modify, boolean openSafe, boolean needResult) {
        mIsFirst = first;
        mIsModify = modify;
        mIsOpenSafe = openSafe;
        mIsNeedResult = needResult;
    }

    private void startComplexActivity(Activity activity) {
        Intent intent = new Intent();
        intent.setClass(activity, UnlockComplexActivity.class);
        intent.setAction(SafeConstants.NEW_APP_PROTECT_COMPLEX);
        intent.putExtra(SafeConstants.IS_FIRST_SET, mIsFirst);
        intent.putExtra(SafeConstants.IS_MODIFY_PASSWORD, mIsModify);
        intent.putExtra(SafeConstants.MODIFY_PASSWORD, mIsModify);
        intent.putExtra(SafeConstants.IS_NEED_OPEN_SAFE, mIsOpenSafe);
        intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
        if (mIsNeedResult) {
            activity.startActivityForResult(intent, SafeConstants.REQUEST_ENTRY_SAFE);
        } else {
            activity.startActivity(intent);
        }
        if (!mIsNeedResult) {
            activity.finish();
        }
    }

    private void startPasswordActivity(Activity activity) {
        Intent intent = new Intent();
        intent.setClass(activity, UnlockPasswordActivity.class);
        intent.setAction(SafeConstants.NEW_APP_PROTECT_PASSWORD);
        intent.putExtra(SafeConstants.IS_FIRST_SET, mIsFirst);
        intent.putExtra(SafeConstants.IS_MODIFY_PASSWORD, mIsModify);
        intent.putExtra(SafeConstants.MODIFY_PASSWORD, mIsModify);
        intent.putExtra(SafeConstants.IS_NEED_OPEN_SAFE, mIsOpenSafe);
        intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
        if (mIsNeedResult) {
            activity.startActivityForResult(intent, SafeConstants.REQUEST_ENTRY_SAFE);
        } else {
            activity.startActivity(intent);
        }
        if (!mIsNeedResult) {
            activity.finish();
        }
    }

    private void startPatternActivity(Activity activity) {
        Intent intent = new Intent();
        intent.setClass(activity, UnlockPatternActivity.class);
        intent.setAction(SafeConstants.NEW_APP_PROTECT_PATTERN);
        intent.putExtra(SafeConstants.IS_FIRST_SET, mIsFirst);
        intent.putExtra(SafeConstants.IS_MODIFY_PASSWORD, mIsModify);
        intent.putExtra(SafeConstants.MODIFY_PASSWORD, mIsModify);
        intent.putExtra(SafeConstants.IS_NEED_OPEN_SAFE, mIsOpenSafe);
        intent.putExtra(SafeConstants.IS_NEED_RESULT, mIsNeedResult);
        if (mIsNeedResult) {
            activity.startActivityForResult(intent, SafeConstants.REQUEST_ENTRY_SAFE);
        } else {
            activity.startActivity(intent);
        }
        if (!mIsNeedResult) {
            activity.finish();
        }
    }

    private int getTitleArrayId(int password_mode) {
        int dialogArrayId;
        switch (password_mode) {
            case SafeConstants.LOCK_MODE_PATTERN:
                dialogArrayId = R.array.bottom_dialog_safe_mode_pattern;
                break;
            case SafeConstants.LOCK_MODE_PASSWORD:
                dialogArrayId = R.array.bottom_dialog_safe_mode_pin;
                break;
            case SafeConstants.LOCK_MODE_COMPLEX:
                dialogArrayId = R.array.bottom_dialog_safe_mode_password;
                break;
            default:
                dialogArrayId = R.array.bottom_dialog_safe_mode_pattern;
                break;
        }
        return dialogArrayId;
    }
}
