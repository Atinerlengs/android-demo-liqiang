package com.freeme.safe.view;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.freeme.safe.encryption.service.EncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.controller.PrivateSpaceAdapter;
import com.freeme.safe.dialog.DialogFactory;
import com.freeme.safe.encryption.thread.EncryptionThread;
import com.freeme.safe.encryption.thread.EncryptionThread.OnEncryptionCompleteListener;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;
import com.freeme.safe.password.UnlockComplexActivity;
import com.freeme.safe.password.UnlockPasswordActivity;
import com.freeme.safe.password.UnlockPatternActivity;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import java.util.ArrayList;

public class PrivateSpaceActivity extends Activity implements OnItemClickListener {
    private static final String TAG = "PrivateSpaceActivity";

    private static final int REQUEST_MODIFY_PASSWORD = 1;
    private static final int REQUEST_ADD_FILE = 2;

    private int mCurrentFileType;

    private HomeBroadcastReceiver mHomeBroadcastReceiver;
    private PrivateSpaceAdapter mPrivateSpaceAdapter;

    private IEncryptionService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IEncryptionService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_space);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            Util.setBackTitle(actionBar, getString(R.string.app_name));
        }

        bindService();

        ListView listView = findViewById(R.id.private_main_list_view);
        mPrivateSpaceAdapter = new PrivateSpaceAdapter(this,
                getResources().getStringArray(R.array.safe_category_list));
        listView.setAdapter(mPrivateSpaceAdapter);
        listView.setOnItemClickListener(this);

        registerHomeBroadcastReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPrivateSpaceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        switch (position) {
            case MediaFile.AUDIO_TYPE:
            case MediaFile.VIDEO_TYPE:
            case MediaFile.IMAGE_TYPE:
            case MediaFile.DOC_TYPE:
            case MediaFile.OTHER_TYPE:
                openSelectedItem(position);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.private_space_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_file:
                //FIXME
                return true;
            case R.id.action_modify_password:
                actionModifyPassword(this, REQUEST_MODIFY_PASSWORD);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (REQUEST_MODIFY_PASSWORD == requestCode) {
                startModifyPassword(data);
            } else if (REQUEST_ADD_FILE == requestCode && SafeUtils.getEncrypFileList() != null) {
                onActionEncryp(SafeUtils.getEncrypFileList());
            }
        }
        super.onActivityResult(requestCode, requestCode, data);
    }

    @Override
    protected void onDestroy() {
        unbindService();
        unregisterHomeBroadcastReceiver();
        super.onDestroy();
    }

    private void bindService() {
        if (mConnection != null) {
            Intent intent = new Intent(this, EncryptionService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    private void registerHomeBroadcastReceiver() {
        mHomeBroadcastReceiver = new HomeBroadcastReceiver();
        mHomeBroadcastReceiver.setOnHomeBroadcastListener(new HomeBroadcastListener() {
            @Override
            public void onReceiveListener() {
                finish();
            }
        });
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeBroadcastReceiver, iFilter);
    }

    private void unregisterHomeBroadcastReceiver() {
        if (mHomeBroadcastReceiver != null) {
            unregisterReceiver(mHomeBroadcastReceiver);
            mHomeBroadcastReceiver = null;
        }
    }

    public void actionModifyPassword(Activity activity, int requestCode) {
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
        Intent intent = new Intent();
        final int mode = SafeUtils.getLockMode(activity, SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_MODE_PATH));
        switch (mode) {
            case SafeConstants.LOCK_MODE_PATTERN: {
                intent.setClass(activity, UnlockPatternActivity.class);
                intent.setAction(SafeConstants.APP_UNLOCK_PATTERN_ACTIVITY);
                break;
            }
            case SafeConstants.LOCK_MODE_PASSWORD: {
                intent.setClass(activity, UnlockPasswordActivity.class);
                intent.setAction(SafeConstants.APP_UNLOCK_PASSWORD_ACTIVITY);
                break;
            }
            case SafeConstants.LOCK_MODE_COMPLEX: {
                intent.setClass(activity, UnlockComplexActivity.class);
                intent.setAction(SafeConstants.APP_UNLOCK_COMPLEX_ACTIVITY);
                break;
            }
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SafeConstants.IS_MODIFY_PASSWORD, true);
        activity.startActivityForResult(intent, requestCode);
    }

    private void startModifyPassword(Intent data) {
        final int from = data.getIntExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, 0);
        final String header = data.getStringExtra(SafeConstants.HEADER_TIP);
        final String sub = data.getStringExtra(SafeConstants.SUB_TIP);
        Intent intent = new Intent(this, UnlockPasswordActivity.class);
        intent.setAction(SafeConstants.NEW_APP_PROTECT_PASSWORD);
        intent.putExtra(SafeConstants.IS_MODIFY_PASSWORD, true);
        intent.putExtra(SafeConstants.MODIFY_PASSWORD, true);
        intent.putExtra(SafeConstants.IS_NEED_OPEN_SAFE, false);
        intent.putExtra(SafeConstants.FROM_LOCK_MODE_ACTIVITY, from);
        intent.putExtra(SafeConstants.HEADER_TIP, header);
        intent.putExtra(SafeConstants.SUB_TIP, sub);
        startActivity(intent);
    }

    private void onActionEncryp(ArrayList<FileInfo> list) {
        Dialog encryptionTaskDialog = DialogFactory.getProgressDialog(this, getString(R.string.encryp_progress_text));
        EncryptionThread encryptionThread = new EncryptionThread(this, encryptionTaskDialog, mCurrentFileType, mService);
        encryptionThread.setOnEncryptionCompleteListener(new OnEncryptionCompleteListener() {
            @Override
            public void onEncryptionComplete() {
                if (SafeUtils.getEncrypFileList() != null) {
                    SafeUtils.getEncrypFileList().clear();
                }
            }
        });
        if (encryptionThread.addEncryptionTask(list)) {
            encryptionThread.start();
            return;
        }
        Log.e(TAG, "no select file or file not in sd or phone.");
    }

    private void openSelectedItem(int position) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setClass(this, EncryptionFileBrowserActivity.class);
        intent.putExtra(SafeConstants.SELECTED_SAFE_FILE_TYPE, position);
        startActivity(intent);
    }
}