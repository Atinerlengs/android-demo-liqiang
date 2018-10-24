package com.freeme.safe.view;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.freeme.filemanager.model.MediaFile;
import com.freeme.filemanager.model.MediaFile.MediaFileType;
import com.freeme.safe.encryption.service.EncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService.Stub;
import com.freeme.filemanager.R;
import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.util.Util;
import com.freeme.safe.controller.EncryptionListCursorAdapter;
import com.freeme.safe.dialog.DialogFactory;
import com.freeme.safe.encryption.provider.EncryptionColumns;
import com.freeme.safe.encryption.thread.DeleteThread;
import com.freeme.safe.encryption.thread.DeleteThread.OnDeleteCompleteListener;
import com.freeme.safe.encryption.thread.DecryptionThread;
import com.freeme.safe.encryption.thread.DecryptionThread.OnDecryptionCompleteListener;
import com.freeme.safe.helper.HomeBroadcastListener;
import com.freeme.safe.helper.HomeBroadcastReceiver;
import com.freeme.safe.model.EncryptionFileInfo;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import static com.freeme.safe.encryption.provider.EncryptionColumns.FILE_URI;
import static com.freeme.safe.utils.SafeConstants.DECRYPTION_PATH;
import static com.freeme.safe.utils.SafeConstants.FROM_SAFE;
import static com.freeme.safe.utils.SafeConstants.REQUEST_GET_ENCRYPT_PATH;

public class EncryptionFileBrowserFragment extends Fragment implements OnItemClickListener, OnItemLongClickListener {

    private final int MENU_EDIT = 0;

    private EncryptionListCursorAdapter mAdapter;
    private ArrayList<EncryptionFileInfo> mCheckedFileList = new ArrayList<>();

    private int mCurrentFileType;
    private boolean mIsDoubleClick;

    private ActionMode mActionMode;
    private Activity mActivity;
    private LinearLayout mEmptyView;
    private ListView mListView;

    private Dialog mDecryptionTaskDialog;
    private AlertDialog mDeleteFileDialog;
    private ProgressDialog mProgressDialog;

    private DeleteThread mDeleteThread;

    private HomeBroadcastReceiver mHomeBroadcastReceiver;

    private MenuItem mEditMenu;

    private IEncryptionService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mCurrentFileType = bundle.getInt(SafeConstants.SELECTED_SAFE_FILE_TYPE, -1);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        bindService();
        registerHomeBroadcastReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout lRootView = (LinearLayout) inflater.inflate(R.layout.activity_category_encryption_file, null);
        setHasOptionsMenu(true);
        mListView = lRootView.findViewById(R.id.safe_list);
        mEmptyView = lRootView.findViewById(R.id.empty_view);
        initListView();
        createDeleteFileDialog(mActivity);
        super.onCreateView(inflater, container, savedInstanceState);
        return lRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mEditMenu = menu.add(0, MENU_EDIT, 0, R.string.edit);
        mEditMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mEditMenu.setVisible(mListView != null && mListView.getCount() > 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case MENU_EDIT:
                setSelectionMode();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        SafeUtils.setPrivatePathPermition();
        mIsDoubleClick = false;
        loadData();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String linkPath = null;
        EncryptionFileInfo encrypFileInfo = mAdapter.getFileItem(position);

        if (encrypFileInfo == null) {
            mAdapter.notifyDataSetChanged();
            return;
        }

        if (isInSelection()) {
            CheckBox checkBox = view.findViewById(R.id.file_checkbox);
            boolean selected = encrypFileInfo.getSelected() || checkBox.isChecked();
            if (selected) {
                mCheckedFileList.remove(encrypFileInfo);
                checkBox.setChecked(false);
            } else {
                mCheckedFileList.add(encrypFileInfo);
                checkBox.setChecked(true);
            }
            Util.updateActionModeTitle(mActionMode, mActivity, mCheckedFileList.size());
            mActionMode.invalidate();
            encrypFileInfo.setSelected(!selected);
            return;
        }

        String newPath = SafeUtils.getEncryptionPath(encrypFileInfo.getEncryptionName());
        try {
            if (!TextUtils.isEmpty(newPath)) {
                File file = new File(newPath);
                if (file.exists()) {
                    openSelectedFile(encrypFileInfo, newPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String savePath;
            if (requestCode == REQUEST_GET_ENCRYPT_PATH) {
                savePath = data.getStringExtra(DECRYPTION_PATH);
                if (savePath != null) {
                    mDecryptionTaskDialog = DialogFactory.getProgressDialog(mActivity, mActivity.getResources().getString(R.string.dencryp_progress_text));
                    DecryptionThread decryptionThread = new DecryptionThread(mActivity, mDecryptionTaskDialog, mService);
                    decryptionThread.setOnDecryptionCompleteListener(new OnDecryptionCompleteListener() {
                        @Override
                        public void onDecryptionComplete() {
                            if (mCheckedFileList != null) {
                                mCheckedFileList.clear();
                            }
                            onOperationDeleteComplete();
                        }
                    });
                    if (decryptionThread.addDecryptionTask(mCheckedFileList, savePath)) {
                        decryptionThread.start();
                    }
                }
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unbindService();
        unregisterHomeBroadcastReceiver();
    }

    @Override
    public void onDestroy() {
        SafeUtils.setPrivatePathPermition();
        super.onDestroy();
        if (mDeleteThread != null) {
            mDeleteThread.stopDeleteTask();
        }
        if (mDecryptionTaskDialog != null && mDecryptionTaskDialog.isShowing()) {
            mDecryptionTaskDialog.dismiss();
        }
        if (mDeleteFileDialog != null && mDeleteFileDialog.isShowing()) {
            mDeleteFileDialog.dismiss();
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void registerHomeBroadcastReceiver() {
        mHomeBroadcastReceiver = new HomeBroadcastReceiver();
        mHomeBroadcastReceiver.setOnHomeBroadcastListener(new HomeBroadcastListener() {
            @Override
            public void onReceiveListener() {
                mActivity.finish();
            }
        });
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        mActivity.registerReceiver(mHomeBroadcastReceiver, iFilter);
    }

    private void unregisterHomeBroadcastReceiver() {
        if (mHomeBroadcastReceiver != null) {
            mActivity.unregisterReceiver(mHomeBroadcastReceiver);
            mHomeBroadcastReceiver = null;
        }
    }

    private void bindService() {
        if (mConnection != null) {
            Intent intent = new Intent(mActivity, EncryptionService.class);
            mActivity.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbindService() {
        if (mConnection != null) {
            mActivity.unbindService(mConnection);
        }
    }

    private void initListView() {
        mAdapter = new EncryptionListCursorAdapter(mActivity, null);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
    }

    private void createDeleteFileDialog(Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                mDeleteThread = new DeleteThread(mActivity, mService);
                mDeleteThread.setOnDeleteCompleteListener(new OnDeleteCompleteListener() {
                    @Override
                    public void onDeleteComplete() {
                        onOperationDeleteComplete();
                    }
                });
                if (mDeleteThread.addDeleteTask(mCheckedFileList)) {
                    mDeleteThread.start();
                }
                dialogInterface.cancel();
            }
        }).setNegativeButton(R.string.cancel, null);

        Util.setFreemeDialogOption(builder, Util.FREEME_DIALOG_OPTION_BOTTOM);
        mDeleteFileDialog = builder.create();
    }

    private void setSelectionMode() {
        if (isInSelection()) {
            return;
        }
        mAdapter.setInCheck(true);
        mAdapter.notifyDataSetChanged();
        startActionMode();
    }

    private void openSelectedFile(EncryptionFileInfo info, String file) {
        String originalPath = info.getOriginalPath();
        String originName = SafeUtils.getOriginalFileName(originalPath);
        int mediaType = info.getMediaType();
        if (!mIsDoubleClick) {
            mIsDoubleClick = true;
            Intent intent = new Intent();
            intent.setAction(SafeConstants.ACTION_SAFE_FILE_VIEW);
            switch (mediaType) {
                case MediaFile.AUDIO_TYPE:
                    intent.setType("audio/*");
                    break;
                case MediaFile.VIDEO_TYPE:
                    intent.setType("video/*");
                    break;
                case MediaFile.IMAGE_TYPE:
                    intent.setType("image/*");
                    break;
                case MediaFile.DOC_TYPE:
                    MediaFileType mediaFileType = MediaFile.getFileType(originalPath);
                    intent.setType(mediaFileType == null ? "text/*" : mediaFileType.mimeType);
                    break;
                default:
                    break;
            }

            intent.putExtra(SafeConstants.SAFE_FILE_PATH, file);
            intent.putExtra(Intent.EXTRA_TITLE, originName);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                mIsDoubleClick = false;
                Toast.makeText(mActivity, R.string.open_fail, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isInSelection() {
        return mActionMode != null;
    }

    private void onOperationDeleteComplete() {
        stopActionMode();
        loadData();
    }

    public void startActionMode() {
        if (mActionMode == null) {
            ActionMode.Callback cb = new ModeCallback();
            mActionMode = mActivity.startActionMode(cb);
            Util.updateActionModeTitle(mActionMode, mActivity, mCheckedFileList.size());
        }
    }

    public void stopActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    private void loadData() {
        AsyncFetchData asyncFetchData = new AsyncFetchData(mCurrentFileType);
        asyncFetchData.execute();
    }

    private void showEmptyView(boolean show) {
        mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (mEditMenu != null) {
            mEditMenu.setVisible(!show);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncFetchData extends AsyncTask<Void, Void, Cursor> {
        private int mCurrentFileType;

        private AsyncFetchData(int curCategory) {
            mCurrentFileType = curCategory;
        }

        @Override
        protected void onPreExecute() {
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(mActivity);
                mProgressDialog.setTitle(getString(R.string.operation_load));
                mProgressDialog.setMessage(getString(R.string.operation_loading));
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setCancelable(false);
            }
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            return mActivity.getContentResolver().query(FILE_URI, null,
                    EncryptionColumns.MEDIA_TYPE + " = " + mCurrentFileType, null, null);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor != null) {
                showEmptyView(cursor.getCount() == 0);
                mAdapter.setSelectionMode(SafeConstants.FRAGMENT_ALL_DEFAULT);
                mAdapter.changeCursor(cursor);
            } else {
                showEmptyView(true);
            }

            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    private class ModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mActivity.getMenuInflater();
            inflater.inflate(R.menu.safe_file_browser_option, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            final int checkSize = mCheckedFileList.size();
            final boolean isSelectAll = checkSize == mAdapter.getCount();
            menu.findItem(R.id.action_select_all).setVisible(!isSelectAll);
            menu.findItem(R.id.action_deselect_all).setVisible(isSelectAll);
            menu.findItem(R.id.action_delete).setEnabled(checkSize > 0);
            menu.findItem(R.id.action_decrypt).setEnabled(checkSize > 0);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_select_all:
                    onOperationSelectAll();
                    break;
                case R.id.action_deselect_all:
                    clearSelection();
                    break;
                case R.id.action_delete:
                    onOperationDelete();
                    break;
                case R.id.action_decrypt:
                    onOperationDecryption();
                    break;
                default:
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.setInCheck(false);
            clearSelection();
            mActionMode = null;
        }
    }

    private void onOperationSelectAll() {
        mCheckedFileList.clear();
        for (EncryptionFileInfo f : mAdapter.getAllFiles()) {
            f.setSelected(true);
            mCheckedFileList.add(f);
        }
        Util.updateActionModeTitle(mActionMode, mActivity, mCheckedFileList.size());
        mActionMode.invalidate();
        mAdapter.setSelectionMode(SafeConstants.FRAGMENT_ALL_SELECT);
        mAdapter.notifyDataSetChanged();
    }

    private void onOperationDelete() {
        if (mCheckedFileList != null) {
            mDeleteFileDialog.show();
        }
    }

    private void onOperationDecryption() {
        Intent intent = new Intent(mActivity, FileExplorerTabActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(FROM_SAFE, true);
        startActivityForResult(intent, REQUEST_GET_ENCRYPT_PATH);
    }

    private void clearSelection() {
        if (mActionMode != null && mCheckedFileList.size() > 0) {
            for (EncryptionFileInfo f : mCheckedFileList) {
                if (f == null) {
                    continue;
                }
                f.setSelected(false);
            }
            mCheckedFileList.clear();
            Util.updateActionModeTitle(mActionMode, mActivity, 0);
            mActionMode.invalidate();
            mAdapter.setSelectionMode(SafeConstants.FRAGMENT_ALL_DESELECT);
        }
        mAdapter.notifyDataSetChanged();
    }
}