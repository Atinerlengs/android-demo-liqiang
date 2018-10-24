/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 *
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.freeme.safe.encryption.service.EncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService;
import com.freeme.safe.encryption.service.IEncryptionService.Stub;
import com.freeme.filemanager.R;
import com.freeme.filemanager.fragment.FastCategoryDetailsFragment;
import com.freeme.filemanager.fragment.FileExplorerViewFragment.SelectFilesCallback;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.model.GlobalConsts;
import com.freeme.filemanager.util.ArchiveHelper;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.FileCategoryHelper;
import com.freeme.filemanager.util.FileOperationHelper;
import com.freeme.filemanager.util.FileSortHelper;
import com.freeme.filemanager.util.StorageHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.FileOperationHelper.IOperationProgressListener;
import com.freeme.filemanager.util.FileSortHelper.SortMethod;
import com.freeme.filemanager.util.Util.MemoryCardInfo;
import com.freeme.filemanager.util.Util.SDCardInfo;
import com.freeme.filemanager.util.Util.UsbStorageInfo;
import com.freeme.filemanager.activity.FileExplorerPreferenceActivity;
import com.freeme.filemanager.view.InformationDialog;
import com.freeme.filemanager.view.PathGallery;
import com.freeme.filemanager.activity.SearchActivity;
import com.freeme.filemanager.view.Settings;
import com.freeme.filemanager.view.TextInputDialog;
import com.freeme.filemanager.view.FileListItem.ModeCallback;
import com.freeme.filemanager.view.PathGallery.IPathItemClickListener;
import com.freeme.filemanager.view.TextInputDialog.OnFinishListener;

import com.freeme.filemanager.custom.FeatureOption;
import com.freeme.safe.dialog.DialogFactory;
import com.freeme.safe.encryption.thread.EncryptionThread;
import com.freeme.safe.password.UnlockComplexActivity;
import com.freeme.safe.password.UnlockPasswordActivity;
import com.freeme.safe.password.UnlockPatternActivity;
import com.freeme.safe.utils.SafeConstants;
import com.freeme.safe.utils.SafeUtils;

import static com.freeme.filemanager.FMIntent.EXTRA_BUCKETID_INFO;
import static com.freeme.filemanager.FMIntent.EXTRA_BUCKET_NAME;
import static com.freeme.filemanager.FMIntent.EXTRA_CATEGORY_TAG;

public class FileViewInteractionHub implements IOperationProgressListener, IPathItemClickListener {
    private static final String LOG_TAG = "FileViewInteractionHub";

    private IFileInteractionListener mFileViewListener;

    private ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();

    private FileOperationHelper mFileOperationHelper;

    private FileSortHelper mFileSortHelper;

    private View mConfirmOperationBar;

    private ProgressDialog progressDialog;

    private ProgressBar mRefreshProgressBar;

    private View mDropdownNavigation;

    private Context mContext;

    private IEncryptionService mService;

    private PathGallery mPathGallery;

    private MenuItem mEditMenu;
    private boolean mEditLastState;

    private static final String ROOT_DIR = "/mnt";

    public int mTabIndex;

    private static final int FILE_NAME_LENGTH = 85;

    private static final int SEND_MAX_FILE_SIZE = 127;

    public String mDeComPressFilePath = null;

    public boolean mDeleteFlag = true;
    private boolean mIsCopy;
    private String mToastMsg;
    private boolean mInSelection;

    private boolean mFromSafe;
    private boolean mInDecryption;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    public enum Mode {
        View, Pick
    }

    public FileViewInteractionHub(IFileInteractionListener fileViewListener, int tabIndex, SortMethod sort) {
        assert (fileViewListener != null);
        mFileViewListener = fileViewListener;
        mTabIndex = tabIndex;
        setup();
        mContext = mFileViewListener.getContext();

        switch (mTabIndex) {
            case Util.PAGE_DETAILS:
            case Util.PAGE_EXPLORER:
            case Util.PAGE_STORAGE: {
                if (mConnection != null) {
                    try {
                        Intent intent = new Intent(mContext, EncryptionService.class);
                        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                break;
            }
            default:
                break;
        }

        mFileOperationHelper = new FileOperationHelper(this, mContext);
        mFileSortHelper = new FileSortHelper(sort);
    }

    public void unbindTaskService() {
        if (this.mConnection != null) {
            this.mContext.unbindService(this.mConnection);
        }
    }

    public void setFromSafe(boolean fromSafe) {
        mFromSafe = fromSafe;
        showConfirmOperationBar(mFromSafe);
        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setText(mContext.getString(R.string.decryption_path));
    }

    public void setDecryption(boolean indecryption) {
        mInDecryption = indecryption;
    }

    public boolean getDecryption() {
        return mInDecryption;
    }

    public void showProgress(String str_title, String str_msg, boolean showCancelButton) {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle(str_title);
        progressDialog.setMessage(str_msg);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        if (showCancelButton) {
            progressDialog.setButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int i) {
                    mDeleteFlag = false;
                    mFileOperationHelper.cancelFileOperation();
                    refreshFileList();
                    dialog.cancel();
                    mToastMsg = "";
                    showConfirmOperationBar(false);
                    mDeleteFlag = true;
                }
            });
        }
        progressDialog.show();
    }

    public void showLoadingProgress(String str_title, String str_msg, boolean showCancelButton) {
        if (showCancelButton) {
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setTitle(str_title);
            progressDialog.setMessage(str_msg);
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        } else {
            progressDialog.dismiss();
        }
    }

    public void sortCurrentList() {
        mFileViewListener.sortCurrentList(mFileSortHelper);
    }

    public boolean canShowCheckBox(String file_path) {
        if (file_path != null && file_path.equals(ROOT_DIR)) {
            return false;
        } else {
            return mConfirmOperationBar.getVisibility() != View.VISIBLE;
        }
    }

    public void showConfirmOperationBar(boolean show) {
        mIsCopy = show;
        if (mEditMenu != null && show) {
            mEditLastState = mEditMenu.isVisible();
            mEditMenu.setVisible(false);
        }
        mFileViewListener.onRefreshMenu(show);
        mConfirmOperationBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void addContextMenuSelectedItem() {
        if (mCheckedFileNameList.size() == 0) {
            int pos = mListViewContextMenuSelectedItem;
            if (pos != -1) {
                FileInfo fileInfo = mFileViewListener.getItem(pos);
                if (fileInfo != null) {
                    mCheckedFileNameList.add(fileInfo);
                }
            }
        }
    }

    public ArrayList<FileInfo> getSelectedFileList() {
        return mCheckedFileNameList;
    }

    public boolean canPaste() {
        return mFileOperationHelper.canPaste();
    }

    // operation finish notification
    @Override
    public void onFinish() {
        DismissProgressDialog();
        if (!TextUtils.isEmpty(mToastMsg)) {
            Toast.makeText(mContext, mToastMsg, Toast.LENGTH_SHORT).show();
            mToastMsg = "";
        }
        mFileViewListener.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showConfirmOperationBar(false);
                refreshFileList();
            }
        });
    }

    public void DismissProgressDialog() {
        if (isLoadingShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public FileInfo getItem(int pos) {
        return mFileViewListener.getItem(pos);
    }

    public boolean isInSelection() {
        if (mCheckedFileNameList != null) {
            if (mInSelection) {
                return true;
            }
            if (Mode.Pick.equals(getMode())) {
                return true;
            }
        }
        return false;
    }

    public boolean isMoveState() {
        return mFileOperationHelper.isMoveState() || mFileOperationHelper.canPaste();
    }

    public boolean inMoveState() {
        return mFileOperationHelper.isMoveState() && mFileOperationHelper.canPaste();
    }

    private void setup() {
        setupFileListView();
        setupPathGallery();
        setupOperationPane();
    }

    // buttons
    private void setupOperationPane() {
        mConfirmOperationBar = mFileViewListener.getViewById(R.id.moving_operation_bar);
        setupClick(mConfirmOperationBar, R.id.button_moving_confirm);
        setupClick(mConfirmOperationBar, R.id.button_moving_cancel);
    }

    private void setupClick(View v, int id) {
        View button = (v != null ? v.findViewById(id) : mFileViewListener.getViewById(id));
        if (button != null)
            button.setOnClickListener(buttonClick);
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_moving_confirm:
                    if (mFromSafe) {
                        mFromSafe = false;
                        setDecryption(true);
                        mFileViewListener.finish();
                    } else if (TextUtils.isEmpty(mDeComPressFilePath)) {
                        onOperationButtonConfirm();
                    } else {
                        onOperationDeCompress();
                    }
                    break;
                case R.id.button_moving_cancel:
                    if (mFromSafe) {
                        mFromSafe = false;
                        setDecryption(false);
                        mFileViewListener.finish();
                    } else {
                        onOperationButtonCancel();
                    }
                    break;
            }
        }

    };

    public void onOperationMoreMenu() {
        if ((mFileGridView != null && mFileGridView.getVisibility() == View.VISIBLE)
            || (mFileListView != null && mFileListView.getVisibility() == View.VISIBLE)) {
            showBottomDialog();
        }
    }

    private void showBottomDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        int arrayId = R.array.bottom_dialog_items_tab0_singlefile;
        int selectSize = getSelectedFileList().size();
        boolean isSingleFile = selectSize == 1;
        switch (mTabIndex) {
            case Util.PAGE_DETAILS:   // 0 is FastCategoryDetailsFragment
            case Util.PAGE_STORAGE: { // 3 is StorageFileListActivity
                if (isSingleFile) {
                    arrayId = R.array.bottom_dialog_items_tab0_singlefile;
                }
            }
                break;
            case Util.PAGE_EXPLORER: { // 1 is FileExplorerViewFragment
                if (isSingleFile) {
                    FileInfo fileInfo = getSelectedFileList().get(0);
                    if (fileInfo == null) {
                        break;
                    }
                    if (fileInfo.IsDir) {
                        arrayId = R.array.bottom_dialog_items_tab1_singledir;
                    } else if (ArchiveHelper.checkIfArchive(fileInfo.filePath)) {
                        arrayId = R.array.bottom_dialog_items_tab1_singlezip;
                    } else {
                        arrayId = R.array.bottom_dialog_items_tab1_singlefile;
                    }
                } else {
                    ArrayList<FileInfo> checkedFileNameList = getSelectedFileList();
                    if (selectSize > 1) {
                        int zipFileCount = 0;
                        int dirCount = 0;

                        for (FileInfo fileInfo : checkedFileNameList) {
                            if (ArchiveHelper.checkIfArchive(fileInfo.filePath))  {
                                zipFileCount = zipFileCount + 1;
                            }
                            FileInfo info = Util.GetFileInfo(fileInfo.filePath);
                            if (info != null && info.IsDir) {
                                dirCount = dirCount + 1;
                            }
                        }
                        if (zipFileCount == selectSize) {
                            arrayId = R.array.bottom_dialog_items_tab1_multizip;
                        } else if (dirCount > 0) {
                            arrayId = R.array.bottom_dialog_items_tab1_multihasdir;
                        } else {
                            arrayId = R.array.bottom_dialog_items_tab1_multinodir;
                        }
                    }
                }
            }
                break;
            default:
                break;
        }

        final int finalArrayId = arrayId;
        final String[] itemArraryId = mContext.getResources().getStringArray(finalArrayId);

        builder.setItems(arrayId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String clickItemString = itemArraryId[which];
                if (clickItemString.equals(getStringFormId(R.string.operation_copy))) {
                    onOperationCopy();
                } else if (clickItemString.equals(getStringFormId(R.string.operation_compress))) {
                    onOperationCompress();
                } else if (clickItemString.equals(getStringFormId(R.string.rename))) {
                    onOperationRenameSingle();
                } else if (clickItemString.equals(getStringFormId(R.string.details))) {
                    onOperationInfo();
                } else if (clickItemString.equals(getStringFormId(R.string.share))) {
                    onOperationSend();
                } else if (clickItemString.equals(getStringFormId(R.string.encryption_menu))) {
                    onActionEntrySafe();
                }
            }
        });

        Util.setFreemeDialogOption(builder, Util.FREEME_DIALOG_OPTION_BOTTOM);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getStringFormId(int stringId) {
        return mContext.getResources().getString(stringId);
    }

    public void onOperationReferesh() {
        if (mCurrentPath != null && !mCurrentPath.contains(ROOT_DIR)) {
            Util.scanAllFile(mContext, new String[]{mCurrentPath});
        } else {
            Util.scanAllFile(mContext, null);
        }

        refreshFileList();
        if (getRefresh()) {
            Toast.makeText(mContext, mContext.getString(R.string.refresh_over), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean refresh = false;

    public boolean getRefresh() {
        return refresh;
    }

    public void setRefresh(boolean flag) {
        refresh = flag;
    }

    private void onOperationFavorite() {
        String path = mCurrentPath;

        if (mListViewContextMenuSelectedItem != -1) {
            path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
        }

        onOperationFavorite(path);
        refreshFileList();
    }

    private void onOperationSetting() {
        Intent intent = new Intent(mContext, FileExplorerPreferenceActivity.class);
        if (intent != null) {
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to start setting: " + e.toString());
            }
        }
    }

    private void onOperationFavorite(String path) {
        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        if (databaseHelper != null) {
            int stringId = 0;
            if (databaseHelper.isFavorite(path)) {
                databaseHelper.delete(path);
                stringId = R.string.removed_favorite;
            } else {
                databaseHelper.insert(Util.getNameFromFilepath(path), path);
                stringId = R.string.added_favorite;
            }
            Toast.makeText(mContext, stringId, Toast.LENGTH_SHORT).show();
        }
    }

    public void onOperationFavoriteNew(boolean favorited) {
        int N = mCheckedFileNameList.size();
        String[] fileLists = new String[N];
        for (int i = 0; i < N; i++) {
            fileLists[i] = mCheckedFileNameList.get(i).filePath;
        }

        FavoriteFileListTask refreshFileListTask = new FavoriteFileListTask(fileLists, favorited);
        if (refreshFileListTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        } else {
            refreshFileListTask.execute();
        }
    }

    private class FavoriteFileListTask extends AsyncTask<Void, Void, Boolean> {
        String[] fileInfos;
        boolean favorited;
        private FavoriteFileListTask(String[] fileInfos, boolean favorite) {
            this.fileInfos = fileInfos;
            favorited = favorite;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();

            synchronized (fileInfos) {
                for (int i = 0; i < fileInfos.length; i++) {
                    String filePath = fileInfos[i];
                    if (databaseHelper != null) {
                        if (databaseHelper.isFavorite(filePath) && !favorited) {
                            databaseHelper.delete(filePath);
                        } else if (!databaseHelper.isFavorite(filePath) && favorited) {
                            databaseHelper.insert(Util.getNameFromFilepath(filePath), filePath);
                        }
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mCheckedFileNameList.clear();
            if (success) {
                Toast.makeText(mContext, favorited ?
                        R.string.added_favorite : R.string.removed_favorite, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onOperationShowSysFiles() {
        Settings.instance().setShowDotAndHiddenFiles(!Settings.instance().getShowDotAndHiddenFiles());
        notifyRefreshViewInfo();
        refreshFileList();
    }

    public void onOperationSelectAllOrCancel() {
        if (mFileViewListener.getItemCount() == 0) {
            return;
        }

        //protect file option
        if (mFileOperationHelper.mCancelfileoperation) {
            Toast.makeText(mContext, R.string.cancel_option_note, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isSelectedAll()) {
            onOperationSelectAll();
        } else {
            clearSelection();
        }
    }

    public void onOperationSelectAll() {
        mCheckedFileNameList.clear();
        if (mSeparateMenu != null && mSeparateMenu.getVisibility() != View.GONE) {
            mSeparateMenu.setVisibility(View.INVISIBLE);
        }

        for (FileInfo f : mFileViewListener.getAllFiles()) {
            f.Selected = true;
            mCheckedFileNameList.add(f);
        }

        if (mContext instanceof IActionModeCtr) {
            IActionModeCtr actionmodeCtr = ((IActionModeCtr) mContext);
            ActionMode mode = actionmodeCtr.getActionMode();
            if (mode == null) {
                mode = actionmodeCtr.startActionMode(new ModeCallback(mContext, this));
                actionmodeCtr.setActionMode(mode);
                Util.updateActionModeTitle(mode, mContext, getSelectedFileList().size());
            }
            mode.invalidate();
        }
        mFileViewListener.onDataChanged();
    }

    public boolean onOperationUpLevel() {
        if (mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL)) {
            return true;
        }
        if (!TextUtils.isEmpty(mRoot) && !mRoot.equals(mCurrentPath)) {
            String parentPath = new File(mCurrentPath).getParent();
            if (!ROOT_DIR.equals(parentPath)) {
                mCurrentPath = parentPath;
                refreshFileList();
                return true;
            }
        }
        return false;
    }

    public void onOperationCreateFolder() {
        if (getCurMemoryFreeSize(mCurrentPath) == 0) {
            Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_SHORT).show();
            return;
        }
        TextInputDialog dialog = new TextInputDialog(mContext,
                mContext.getString(R.string.operation_create_folder),
                mContext.getString(R.string.operation_create_folder_message),
                mContext.getString(R.string.new_folder_name),
                mContext.getString(R.string.new_folder_name).length(),
                new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        if (text == null || TextUtils.isEmpty(text.trim())) {
                            Toast.makeText(mContext, R.string.invalid_empty_name, Toast.LENGTH_SHORT).show();
                        } else if (text.matches(".*[/\\\\:*?\"<>|].*")) { // characters not allowed
                            Toast.makeText(mContext, R.string.warn_invalid_forlder_name, Toast.LENGTH_SHORT).show();
                        } else {
                            return doCreateFolder(text);
                        }
                        return false;
                    }
                });
        dialog.show();
    }

    private boolean doCreateFolder(String text) {
        if (TextUtils.isEmpty(text))
            return false;
        int textLength = text.length();
        if (textLength >= FILE_NAME_LENGTH) {
            Toast.makeText(mContext, R.string.invalid_file_name, Toast.LENGTH_LONG).show();
            return false;
        }

        // if free is 0 bit
        if (mFileOperationHelper.CreateFolder(mCurrentPath, text.trim())) {
            Toast.makeText(mContext, mContext.getString(R.string.succeed_to_create_folder), Toast.LENGTH_SHORT).show();
            mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(mCurrentPath, text)));
            mFileListView.setSelection(mFileListView.getCount() - 1);
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_create_folder))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }
        refreshFileList();
        return true;
    }

    public void onOperationSearch() {
        Intent intent = new Intent(mContext, SearchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mContext.startActivity(intent);
    }

    public void onSortChanged(SortMethod s) {
        if (mFileSortHelper.getSortMethod() != s) {
            mFileSortHelper.setSortMethod(s);
            sortCurrentList();
        }
    }

    public void onOperationCopy() {
        onOperationCopy(getSelectedFileList());
    }

    public void onOperationCopy(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
        clearSelection();

        showConfirmOperationBar(true);
        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void onOperationCompress() {
        if (mCheckedFileNameList.size() == 0) {
            return;
        }
        if (getCurMemoryFreeSize(mCurrentPath) == 0) {
            Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_SHORT).show();
            return;
        }
        FileInfo fileinfo = getSelectedFileList().get(0);
        mFileOperationHelper.Copy(getSelectedFileList());
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.dialog_compress, null);
        EditText compressNameEdit = (EditText) view.findViewById(R.id.compress_name);
        String compressName = "";
        if (mCheckedFileNameList.size() == 1) {
            compressName = Util.getFormatedFileName(mCheckedFileNameList.get(0).fileName);
        } else {
            compressName = mCurrentPath.substring(mCurrentPath.lastIndexOf("/") + 1);
        }
        compressNameEdit.setText(compressName);
        compressNameEdit.setSelection(compressName.length());
        final String saveDir = mCurrentPath;

        TextInputDialog dialog = new TextInputDialog(mContext,
                mContext.getString(R.string.operation_compress),
                mContext.getString(R.string.compress_save_name) + "(" + mContext.getString(R.string.compress_save_format) + ")",
                compressName,
                compressName.length(),
                new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        exitActionMode();
                        if (TextUtils.isEmpty(text)) {
                            Toast.makeText(mContext, R.string.operation_compress_invalid_name, Toast.LENGTH_LONG).show();
                            return true;
                        } else {
                            String savePath = saveDir + "/" + text + ".zip";
                            if ((new File(savePath)).exists()) {
                                Toast.makeText(mContext, R.string.compress_failed_for_exists, Toast.LENGTH_LONG).show();
                                return true;
                            } else {
                                if (mFileOperationHelper.compress(text)) {
                                    showProgress(mContext.getString(R.string.operation_compress),
                                            mContext.getString(R.string.operation_compressing), true);
                                }
                                return false;
                            }
                        }
                    }
                });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (progressDialog == null || !progressDialog.isShowing()) {
                    mFileOperationHelper.clear();
                }
            }
        });
        dialog.show();
    }

    public void updateBarForDeCompress() {
        showConfirmOperationBar(true);
        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setText(mContext.getString(R.string.operation_decompress));
//        confirmButton.setEnabled(false);
    }

    public void onOperationDeCompress() {
        if (!TextUtils.isEmpty(mDeComPressFilePath)) {
            if (mFileOperationHelper.deCompress(mDeComPressFilePath, mCurrentPath)) {
                showProgress(mContext.getString(R.string.operation_decompress), mContext.getString(R.string.operation_decompressing), true);
            }
            mDeComPressFilePath = null;
        }
    }

    public long getFileListsSize(String path) {
        long fileSize = 0;
        File file = new File(path);
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                return fileSize;
            }
            for (File f : listFiles) {
                fileSize = fileSize + getFileListsSize(f.getPath());
            }
        } else {
            fileSize = file.length();
        }
        return fileSize;
    }

    public boolean isCopyFreeMemorySizeEnough(ArrayList<FileInfo> files, String path) {
        long filesSize = 0;
        SDCardInfo sdCardInfo = Util.getSDCardInfo();
        MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();
        for (FileInfo f : files) {
            if (f != null) {
                filesSize = filesSize + getFileListsSize(f.filePath);
            }
        }
        //for kk storage
        if (FeatureOption.MTK_MULTI_STORAGE_SUPPORT) {
            if (Util.isSdcardExist()) {
                if (path.startsWith(Util.MEMORY_DIR)) {
                    if (filesSize < memoryCardInfo.free) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (path.startsWith(Util.SD_DIR)) {
                    if (filesSize < sdCardInfo.free) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (path.startsWith(Util.USBOTG_DIR)) {
                    if (filesSize < usbStorageInfo.free) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                if (path.startsWith(Util.MEMORY_DIR)) {
                    if (filesSize < memoryCardInfo.free) {
                        return true;
                    } else {
                        return false;
                    }
                } else if (path.startsWith(Util.USBOTG_DIR)) {
                    if (filesSize < usbStorageInfo.free) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {

            if (path.startsWith(Util.MEMORY_DIR)) {
                if (filesSize < memoryCardInfo.free) {
                    return true;
                } else {
                    return false;
                }
            } else if (path.startsWith(Util.USBOTG_DIR)) {
                if (filesSize < usbStorageInfo.free) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private long getCurMemoryFreeSize(String currentPath) {
        long freeSize = 0;
        SDCardInfo sdCardInfo = Util.getSDCardInfo();
        MemoryCardInfo memoryCardInfo = Util.getMemoryCardInfo();
        UsbStorageInfo usbStorageInfo = Util.getUsbStorageInfo();

        if (currentPath.startsWith(Util.MEMORY_DIR)) {
            freeSize = memoryCardInfo.free;
        } else if (currentPath.startsWith(Util.SD_DIR)) {
            freeSize = sdCardInfo.free;
        } else if (currentPath.startsWith(Util.USBOTG_DIR)) {
            freeSize = usbStorageInfo.free;
        }

        return freeSize;
    }

    private void onOperationPaste() {
        if (!isCopyFreeMemorySizeEnough(mFileOperationHelper.mCurFileNameList, mCurrentPath)) {
            Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_LONG).show();
            onOperationButtonCancel();
            return;
        }
        if (mFileOperationHelper.Paste(mCurrentPath)) {
            showProgress(mContext.getString(R.string.operation_copy), mContext.getString(R.string.operation_pasting), true);
        }
    }

    public void onOperationMove() {
        if (isFavoritedFile()) {
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            return;
        }
        mFileOperationHelper.StartMove(getSelectedFileList());
        clearSelection();
        showConfirmOperationBar(true);
        View confirmButton = mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void refreshFileList() {
        clearSelection();
        mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
        updateConfirmButtons();
    }

    private void updateConfirmButtons() {
        if (mConfirmOperationBar == null || mConfirmOperationBar.getVisibility() == View.GONE) {
            return;
        }

        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        int text = R.string.operation_paste;
        if (isSelectingFiles()) {
            confirmButton.setEnabled(mCheckedFileNameList.size() != 0);
            text = R.string.operation_send;
        } else if (!TextUtils.isEmpty(mDeComPressFilePath)) {
            text = R.string.operation_decompress;
            confirmButton.setEnabled(true);
        } else if (isMoveState()) {
            confirmButton.setEnabled(mFileOperationHelper.canMove(mCurrentPath));
        } else if (mFromSafe) {
            text = R.string.decryption_path;
            confirmButton.setEnabled(true);
        }
        confirmButton.setText(text);
    }

    public void onOperationSend() {
        ArrayList<FileInfo> selectedFileList = getSelectedFileList();
        int listSize = selectedFileList.size();
        if (listSize > SEND_MAX_FILE_SIZE) {
            Toast.makeText(mContext, R.string.send_file_max_size, Toast.LENGTH_LONG).show();
            clearSelection();
            return;
        }
        for (FileInfo f : selectedFileList) {
            if (f.IsDir) {
                AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage(
                        R.string.error_info_cant_send_folder).setPositiveButton(R.string.confirm, null).create();
                dialog.show();
                clearSelection();
                return;
            }
        }

        Intent intent = IntentBuilder.buildSendFile(mContext, selectedFileList);
        if (intent != null) {
            try {
                mFileViewListener.startActivity(Intent.createChooser(intent, mContext.getString(R.string.send_file)));
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
    }

    public void onActionEntrySafe() {
        String safeFilePath = SafeConstants.SAFE_ROOT_PATH;
        String unlockModePath = SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_MODE_PATH);
        String passWordPath = SafeUtils.getSafeFilePath(safeFilePath, SafeConstants.LOCK_PASSWORD_PATH);
        int unlockMode = SafeUtils.getLockMode(mContext, unlockModePath);
        String password = SafeUtils.getPassword(mContext, passWordPath);
        Intent intent = new Intent();
        intent.putExtra(SafeConstants.IS_NEED_RESULT, true);
        if (unlockMode == SafeConstants.LOCK_MODE_DEFAULT || password == null) {
            intent.setClass(mContext, UnlockPasswordActivity.class);
            intent.setAction(SafeConstants.NEW_APP_PROTECT_PASSWORD);
            intent.putExtra(SafeConstants.IS_FIRST_SET, true);
        } else if (unlockMode == SafeConstants.LOCK_MODE_PATTERN) {
            intent.setAction(SafeConstants.APP_UNLOCK_PATTERN_ACTIVITY);
            intent.setClass(mContext, UnlockPatternActivity.class);
        } else if (unlockMode == SafeConstants.LOCK_MODE_PASSWORD) {
            intent.setAction(SafeConstants.APP_UNLOCK_PASSWORD_ACTIVITY);
            intent.setClass(mContext, UnlockPasswordActivity.class);
        } else if (unlockMode == SafeConstants.LOCK_MODE_COMPLEX) {
            intent.setAction(SafeConstants.APP_UNLOCK_COMPLEX_ACTIVITY);
            intent.setClass(mContext, UnlockComplexActivity.class);
        }
        startActivityForResult(intent, SafeConstants.REQUEST_ENTRY_SAFE);
    }

    private void startActivityForResult(Intent intent, int requestCode) {
        mFileViewListener.startActivityForResult(intent, requestCode);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SafeConstants.REQUEST_ENTRY_SAFE) {
                onActionEncryp();
            }
        }
    }

    private void onActionEncryp() {
        EncryptionThread encryptionThread = new EncryptionThread(mContext, DialogFactory.getProgressDialog(mContext, mContext.getResources().getString(R.string.encryption_menu)), -1, mService);
        if (encryptionThread != null) {
            encryptionThread.setOnEncryptionCompleteListener(new EncryptionThread.OnEncryptionCompleteListener() {
                @Override
                public void onEncryptionComplete() {
                    deleteComplete();
                }
            });
            if (encryptionThread.addEncryptionTask(mCheckedFileNameList)) {
                encryptionThread.start();
            }
        }
    }

    private void deleteComplete() {
        exitActionMode();
        refreshFileList();
    }

    public boolean isFavoritedFile() {
        String path = mCurrentPath;
        ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(getSelectedFileList());
        if (mFileViewListener == null) {
            return false;
        }
        if (mListViewContextMenuSelectedItem != -1) {
            if ((mFileViewListener.getItem(mListViewContextMenuSelectedItem) != null)) {
                path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
            } else {
                return false;
            }
        }

        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        if (databaseHelper != null) {
            Log.i(LOG_TAG, "isFavoritedFile-->path is " + path);
            if (databaseHelper.isFavorite(path)) {
                return true;
            }
        }

        if ((databaseHelper != null) && selectedFiles != null) {
            for (FileInfo f : selectedFiles) {
                Log.i(LOG_TAG, "isFavoritedFile-->f.filePath is " + f.filePath);
                if (databaseHelper.isFavorite(f.filePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onOperationRename() {
        int pos = mListViewContextMenuSelectedItem;
        if (isFavoritedFile()) {
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            return;
        }
        if (pos == -1)
            return;

        if (getSelectedFileList().size() == 0)
            return;

        final FileInfo f = getSelectedFileList().get(0);
        clearSelection();
        //if the filename is too long,it can't know  the type of file when you rename.
        final String extFromFilename = Util.getExtFromFilename(f.filePath);
        String selectFileName;
        if (f.IsDir || "".equals(extFromFilename)) {
            selectFileName = f.fileName.toString();
        } else {
            selectFileName = Util.getNameFromFilename(f.fileName);
        }

        TextInputDialog dialog = new TextInputDialog(mContext,
                mContext.getString(R.string.operation_rename),
                mContext.getString(R.string.operation_rename_message),
                f.fileName,
                selectFileName.length(),
                new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        return doRename(f, text);
                    }
                });
        dialog.show();

        exitActionMode();
    }

    public void onOperationRenameSingle() {
        if (isFavoritedFile()) {
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            return;
        }

        if (getSelectedFileList().size() == 0)
            return;

        final FileInfo f = getSelectedFileList().get(0);
        //if the filename is too long,it can't know  the type of file when you rename.
        final String extFromFilename = Util.getExtFromFilename(f.filePath);
        String selectFileName;
        if (f.IsDir || "".equals(extFromFilename)) {
            selectFileName = f.fileName.toString();
        } else {
            selectFileName = Util.getNameFromFilename(f.fileName);
        }

        TextInputDialog dialog = new TextInputDialog(mContext,
                mContext.getString(R.string.operation_rename),
                mContext.getString(R.string.operation_rename_message),
                f.fileName,
                selectFileName.length(),
                new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        exitActionMode();
                        return doRename(f, text);
                    }
                });
        dialog.show();
    }

    private boolean doRename(final FileInfo f, String text) {
        if (TextUtils.isEmpty(text))
            return false;

        int textLength = text.length();
        if (textLength >= FILE_NAME_LENGTH) {
            Toast.makeText(mContext, R.string.invalid_file_rename, Toast.LENGTH_LONG).show();
            return false;
        }

        if (text.matches(".*[/\\\\:*?\"<>|].*")) {
            Toast.makeText(mContext, R.string.warn_invalid_rename, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (mFileOperationHelper.Rename(f, text)) {
            f.fileName = text;
            mFileViewListener.onDataChanged();
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_rename))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }
        refreshFileList();
        return true;
    }

    public void onOperationDelete() {
        if (isFavoritedFile()) {
            Toast.makeText(mContext, R.string.removed_favorite_first, Toast.LENGTH_LONG).show();
            refreshFileList();
            if (mCleanBtn != null) {
                mCleanBtn.setEnabled(false);
            }
            exitActionMode();
            return;
        }
        doOperationDelete(getSelectedFileList());
    }

    private void doOperationDelete(final ArrayList<FileInfo> selectedFileList) {
        final ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(selectedFileList);
        int fileCnt;
        int folderCnt = 0;
        int selectCnt = selectedFiles.size();
        String message;
        for (int i = 0; i < selectCnt; i++) {
            if (selectedFiles.get(i).IsDir) {
                folderCnt++;
            }
        }

        fileCnt = selectCnt - folderCnt;
        switch (folderCnt) {
            case 0:
                if (fileCnt == 1) {
                    message = mContext.getString(R.string.operation_delete_file_confirm_message);
                }  else {
                    message = mContext.getString(R.string.operation_delete_files_confirm_message, fileCnt);
                }
                break;
            case 1:
                if (fileCnt == 0) {
                    message = mContext.getString(R.string.operation_delete_folder_confirm_message);
                } else {
                    message = mContext.getString(R.string.operation_delete_folder_file_confirm_message,
                            folderCnt, fileCnt);
                }
                break;
            default:
                if (fileCnt == 0) {
                    message = mContext.getString(R.string.operation_delete_folders_confirm_message, folderCnt);
                } else {
                    message = mContext.getString(R.string.operation_delete_folder_file_confirm_message,
                                folderCnt, fileCnt);
                }
                break;
        }

        Dialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mCleanBtn != null) {
                            mCleanBtn.setEnabled(false);
                        }
                        exitActionMode();
                        if (mFileOperationHelper.Delete(selectedFiles)) {
                            for (FileInfo fileInfo : selectedFiles) {
                                mFileViewListener.deleteSingleFile(fileInfo);
                            }
                            showProgress(mContext.getString(R.string.operation_delete), mContext.getString(R.string.operation_deleting), true);
                            mToastMsg = mContext.getString(R.string.delete) + " " +
                                    selectedFiles.size() + " " +
                                    mContext.getString(R.string.item);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
    }

    public void onOperationInfo() {
        if (getSelectedFileList().size() == 0) {
            return;
        }

        FileInfo file = getSelectedFileList().get(0);
        if (file == null) {
            return;
        }

        InformationDialog dialog = new InformationDialog(mContext, file);
        dialog.show();
    }

    public void onOperationButtonConfirm() {
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(mCheckedFileNameList);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (mFileOperationHelper.isMoveState()) {
            if (!isCopyFreeMemorySizeEnough(mFileOperationHelper.mCurFileNameList, mCurrentPath)) {
                Toast.makeText(mContext, R.string.insufficient_memory, Toast.LENGTH_LONG).show();
                onOperationButtonCancel();
                return;
            }
            if (mFileOperationHelper.EndMove(mCurrentPath)) {
                showProgress(mContext.getString(R.string.operation_move), mContext.getString(R.string.operation_moving), true);
            }
        } else {
            onOperationPaste();
        }
    }

    public void onOperationButtonCancel() {
        mDeComPressFilePath = null;
        mFileOperationHelper.clear();
        showConfirmOperationBar(false);
        if (mEditMenu != null) {
            mEditMenu.setVisible(mEditLastState);
        }
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(null);
            mSelectFilesCallback = null;
            clearSelection();
            refreshFileList();
        } else if (mFileOperationHelper.isMoveState()) {
            // refresh to show previously selected hidden files
            mFileOperationHelper.EndMove(null);
            refreshFileList();
        } else {
            refreshFileList();
        }
    }

    // context menu
    private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (isInSelection() || isMoveState()) {
                addMenuItem(menu, GlobalConsts.MENU_COPY, 0, R.string.operation_copy);
                addMenuItem(menu, GlobalConsts.MENU_COMPRESS, 0, R.string.operation_compress);
                addMenuItem(menu, MENU_RENAME, 0, R.string.rename);
                addMenuItem(menu, MENU_INFO, 0, R.string.details);
                addMenuItem(menu, MENU_SEND, 0, R.string.share);

                boolean isSingleFile = getSelectedFileList().size() == 1;
                switch (mTabIndex) {
                    case Util.PAGE_DETAILS:
                    case Util.PAGE_STORAGE:
                        setItemVisible(menu, GlobalConsts.MENU_COPY, false);
                        setItemVisible(menu, GlobalConsts.MENU_COMPRESS, false);
                        setItemVisible(menu, MENU_RENAME, false);
                        setItemVisible(menu, MENU_INFO, isSingleFile);
                        break;
                    case Util.PAGE_EXPLORER:
                        setItemVisible(menu, MENU_RENAME, isSingleFile);
                        setItemVisible(menu, MENU_INFO, isSingleFile);

                        ArrayList<FileInfo> checkedFileNameList = getSelectedFileList();
                        if (checkedFileNameList.size() > 0) {
                            int zipFileCount = 0;
                            for (FileInfo fileInfo : checkedFileNameList) {
                                if (ArchiveHelper.checkIfArchive(fileInfo.filePath)) {
                                    zipFileCount = zipFileCount + 1;
                                }
                                //.thumbnails folder maybe del when actionmode
                                FileInfo info = Util.GetFileInfo(fileInfo.filePath);
                                if (info != null && info.IsDir) {
                                    setItemVisible(menu, MENU_SEND, false);
                                    break;
                                }
                            }
                            setItemVisible(menu, GlobalConsts.MENU_COMPRESS, zipFileCount != checkedFileNameList.size());
                        } else {
                            setItemVisible(menu, GlobalConsts.MENU_COMPRESS, false);
                        }
                        break;
                    case Util.PAGE_PICFOLDER:
                        menu.clear();
                        break;
                    default:
                        break;
                }

                return;
            }

            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

            FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
            FileInfo file = mFileViewListener.getItem(info.position);
            if (file == null) {
                mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
                return;
            }

            if (databaseHelper != null && file != null) {
                int stringId = databaseHelper.isFavorite(file.filePath) ? R.string.operation_unfavorite
                        : R.string.operation_favorite;
                addMenuItem(menu, GlobalConsts.MENU_FAVORITE, 0, stringId);
            }
            //delete the copy and move function when mTabIndex=0/
            if (mTabIndex != Util.PAGE_DETAILS || mTabIndex != Util.PAGE_PICFOLDER) {
                addMenuItem(menu, GlobalConsts.MENU_COPY, 0, R.string.operation_copy);
                addMenuItem(menu, GlobalConsts.MENU_MOVE, 0, R.string.operation_move);
            }
            if (mTabIndex == Util.PAGE_EXPLORER) {
                if (!ArchiveHelper.checkIfArchive(file.filePath)) {
                    addMenuItem(menu, GlobalConsts.MENU_COMPRESS, 0, R.string.operation_compress);
                } else {
                    addMenuItem(menu, GlobalConsts.MENU_DECOMPRESS, 0, R.string.operation_decompress);
                }
            }
            addMenuItem(menu, MENU_SEND, 0, R.string.operation_send);
            addMenuItem(menu, MENU_RENAME, 0, R.string.operation_rename);
            addMenuItem(menu, MENU_DELETE, 0, R.string.operation_delete);
            addMenuItem(menu, MENU_INFO, 0, R.string.operation_info);

            if (!canPaste()) {
                MenuItem menuItem = menu.findItem(GlobalConsts.MENU_PASTE);
                if (menuItem != null)
                    menuItem.setEnabled(false);
            }
        }
    };
    private static void setItemVisible(Menu menu, int id, boolean visible) {
        if (menu.findItem(id) != null) {
            menu.findItem(id).setVisible(visible);
        }
    }

    // File List view setup
    private ListView mFileListView;
    private GridView mFileGridView;
    private int mListViewContextMenuSelectedItem = -1;
    private LinearLayout mSeparateMenu;
    private Button mCleanBtn;

    private void setupPathGallery() {
        mDropdownNavigation = mFileViewListener.getViewById(R.id.dropdown_navigation);
        mPathGallery = ((PathGallery) this.mFileViewListener.getViewById(R.id.path_gallery_nav));
        mRefreshProgressBar = ((ProgressBar) this.mFileViewListener.getViewById(R.id.refresh_progress));
        if (mPathGallery != null) {
            mPathGallery.setPathItemClickListener(this);
        }
    }

    private void setupFileListView() {
        mSeparateMenu = (LinearLayout) mFileViewListener.getViewById(R.id.separate_menu_view);
        mFileListView = (ListView) mFileViewListener.getViewById(R.id.file_path_list);
        if (mFileListView != null) {
            mFileListView.setLongClickable(true);
            //mFileListView.setOnCreateContextMenuListener(mListViewContextMenuListener);
            mFileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    return onListItemLongClick(parent, view, position, id);
                }
            });
            mFileListView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onListItemClick(parent, view, position, id);
                }
            });
        }

        mFileGridView = (GridView) mFileViewListener.getViewById(R.id.file_folder_grid);
        if (mFileGridView != null) {
            mFileGridView.setLongClickable(true);
            //mFileGridView.setOnCreateContextMenuListener(mListViewContextMenuListener);
            mFileGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    return onListItemLongClick(parent, view, position, id);
                }
            });
            mFileGridView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onListItemClick(parent, view, position, id);
                }
            });
        }

        mCleanBtn = (Button) mFileViewListener.getViewById(R.id.cleanup_button);
        if (mCleanBtn != null) {
            mCleanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onOperationDelete();
                }
            });
        }
    }

    // menu
    private static final int MENU_SORT = 3;

    private static final int MENU_SEND = 7;

    private static final int MENU_RENAME = 8;

    private static final int MENU_DELETE = 9;

    private static final int MENU_INFO = 10;

/*    private static final int MENU_SORT_NAME = 11;

    private static final int MENU_SORT_SIZE = 12;

    private static final int MENU_SORT_DATE = 13;

    private static final int MENU_SORT_TYPE = 14;*/

    private static final int MENU_REFRESH = 15;

    private static final int MENU_SELECTALL = 16;

    private static final int MENU_EXIT = 18;

    private static final int MENU_UPDATE = 30;

    private static final int MENU_APPABOUT = 31;

    private static final int MENU_SETTING = 32;

    private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            mListViewContextMenuSelectedItem = info != null ? info.position : -1;

            int itemId = item.getItemId();
            if (mFileViewListener.onOperation(itemId)) {
                return true;
            }
            addContextMenuSelectedItem();

            switch (itemId) {
                case GlobalConsts.MENU_SEARCH:
                    onOperationSearch();
                    break;
                case GlobalConsts.MENU_NEW_FOLDER:
                    onOperationCreateFolder();
                    break;
                case GlobalConsts.MENU_EDIT:
                    setSelectionMode(true);
                    break;
                case MENU_REFRESH:
                    onOperationReferesh();
                    break;
                case MENU_SELECTALL:
                    onOperationSelectAllOrCancel();
                    break;
                case GlobalConsts.MENU_SHOWHIDE:
                    onOperationShowSysFiles();
                    break;
                case GlobalConsts.MENU_FAVORITE:
                    onOperationFavorite();
                    break;
                case MENU_EXIT:
                    mFileViewListener.finish();
                    break;
                case GlobalConsts.MENU_COPY:
                    onOperationCopy();
                    break;
                case GlobalConsts.MENU_COMPRESS:
                    onOperationCompress();
                    break;
                case GlobalConsts.MENU_DECOMPRESS:
                    FileInfo fileInfo = getSelectedFileList().get(0);
                    refreshFileList();
                    viewFile(fileInfo);
                    break;
                case GlobalConsts.MENU_PASTE:
                    onOperationPaste();
                    break;
                case GlobalConsts.MENU_MOVE:
                    onOperationMove();
                    break;
                case MENU_SEND:
                    onOperationSend();
                    break;
                case MENU_RENAME:
                    onOperationRename();
                    break;
                case MENU_DELETE:
                    onOperationDelete();
                    break;
                case MENU_INFO:
                    onOperationInfo();
                    break;
                case MENU_UPDATE:
                    onOperationUpdate(mContext);
                    break;
                case MENU_APPABOUT:
                    onOperationAppAbout();
                    break;
                case MENU_SETTING:
                    onOperationSettingRing();
                    break;
                default:
                    return false;
            }
            mListViewContextMenuSelectedItem = -1;
            return true;
        }

    };

    /* popwindow menu item click function */
    public void newPopupMenuResponse(int itemId) {
        if(itemId == R.id.sort_by_name) {
            onSortChanged(SortMethod.name);
        } else if (itemId == R.id.sort_by_size) {
            onSortChanged(SortMethod.size);
        } else if (itemId == R.id.sort_by_date) {
            onSortChanged(SortMethod.date);
        } else {
            onSortChanged(SortMethod.type);
        }
    }

    private void onOperationSettingRing() {
        String path = mCurrentPath;

        if (mListViewContextMenuSelectedItem != -1) {
            path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
            Log.i(LOG_TAG, "path=" + path);
        }

        setVoice(path, AppConstant.RINGTONE);
        Toast.makeText(mContext, R.string.menu_setting_ring_ok, Toast.LENGTH_LONG).show();
    }

    public interface AppConstant {
        public static final int RINGTONE = 0;                   //铃声
        public static final int NOTIFICATION = 1;               //通知音
        public static final int ALARM = 2;                      //闹钟
        public static final int ALL = 3;                        //所有声音
    }

    private void setVoice(String path, int id) {
        ContentValues cv = new ContentValues();
        Uri newUri = null;
//        Uri uri = MediaStore.Audio.Media.getContentUriForPath(path);
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(LOG_TAG, "path = " + path);
        Log.i(LOG_TAG, "uri = " + uri);
        // 查询音乐文件在媒体库是否存在
        Cursor cursor = mContext.getContentResolver().query(uri, null, MediaStore.MediaColumns.DATA + "=? ", new String[]{path}, null);
        if (cursor.moveToFirst() && cursor.getCount() > 0) {
            String _id = cursor.getString(0);
            switch (id) {
                case AppConstant.RINGTONE:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                case AppConstant.NOTIFICATION:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, false);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                case AppConstant.ALARM:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, false);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, true);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                case AppConstant.ALL:
                    cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    cv.put(MediaStore.Audio.Media.IS_ALARM, true);
                    cv.put(MediaStore.Audio.Media.IS_MUSIC, true);
                    break;
                default:
                    break;
            }
            // 把需要设为铃声的歌曲更新铃声库
            mContext.getContentResolver().update(uri, cv, MediaStore.MediaColumns.DATA + "=?", new String[]{path});
            newUri = ContentUris.withAppendedId(uri, Long.valueOf(_id));
            Log.i(LOG_TAG, "newUri = " + newUri);
            // 一下为关键代码：
            switch (id) {
                case AppConstant.RINGTONE:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, newUri);
                    break;
                case AppConstant.NOTIFICATION:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION, newUri);
                    break;
                case AppConstant.ALARM:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM, newUri);
                    break;
                case AppConstant.ALL:
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALL, newUri);
                    break;
                default:
                    break;

            }
        }
    }

    private void onOperationAppAbout() {
        Intent intent = new Intent();
        intent.setClassName(mContext, "com.freeme.filemanager.activity.AboutActivity");
        mContext.startActivity(intent);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void onOperationUpdate(Context context) {
        checkUpdate(context);
    }

    private void checkUpdate(Context context) {
        Toast.makeText(context, R.string.newest_version, Toast.LENGTH_LONG).show();
    }

    private com.freeme.filemanager.controller.FileViewInteractionHub.Mode mCurrentMode;

    public String mCurrentPath;

    private String mRoot;

    private SelectFilesCallback mSelectFilesCallback;

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();

        addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall);
        addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 1, R.string.operation_create_folder, R.drawable.ic_menu_new_folder);
        addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 2, R.string.operation_show_sysfile);
        addMenuItem(menu, MENU_REFRESH, 3, R.string.operation_refresh);
        addMenuItem(menu, MENU_APPABOUT, 4, R.string.app_about);
        addMenuItem(menu, GlobalConsts.MENU_EDIT, 5, R.string.edit);
        mEditMenu = menu.findItem(GlobalConsts.MENU_EDIT);
        mEditMenu.setVisible(mFileViewListener.getItemCount() > 0);
        return true;
    }

    public void showEditMenuOrNot(boolean show) {
        if (mEditMenu != null) {
            mEditMenu.setVisible(!mIsCopy && show);
        }
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string) {
        addMenuItem(menu, itemId, order, string, -1);
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string, int iconRes) {
        if (!mFileViewListener.shouldHideMenu(itemId)) {
            switch (itemId) {
                case MENU_SELECTALL:
                case MENU_REFRESH:
                case MENU_APPABOUT:
                case GlobalConsts.MENU_NEW_FOLDER:
                case GlobalConsts.MENU_SHOWHIDE:
                    return;
            }

            MenuItem item = menu.add(0, itemId, order, string).setOnMenuItemClickListener(menuItemClick);
            switch (itemId) {
                case GlobalConsts.MENU_EDIT:
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    break;
                default:
                    break;
            }

            if (iconRes > 0) {
                item.setIcon(iconRes);
            }
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            menu.clear();
            return false;
        }
        if (isInSelection()) {
            return false;
        }
        updateMenuItems(menu);
        return true;
    }

    public void addMenuItems(Menu menu) {
        menu.clear();
        addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall);
        addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 1, R.string.operation_create_folder, R.drawable.ic_menu_new_folder);
        addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 2, R.string.operation_show_sysfile);
        addMenuItem(menu, MENU_REFRESH, 3, R.string.operation_refresh);
        addMenuItem(menu, MENU_APPABOUT, 4, R.string.app_about);
        addMenuItem(menu, GlobalConsts.MENU_EDIT, 5, R.string.edit);
    }

    private void updateMenuItems(Menu menu) {
        if (menu.size() <= 0) {
            addMenuItems(menu);
        }

        if (menu.size() > 0) {
            if (mEditMenu != null && mIsCopy) {
                mEditMenu.setVisible(false);
            }
            MenuItem menuItem = menu.findItem(MENU_SELECTALL);
            if (menuItem != null) {
                menuItem.setTitle(
                        isSelectedAll() ? R.string.operation_cancel_selectall : R.string.operation_selectall);
                menuItem.setEnabled(mCurrentMode != Mode.Pick);

                menuItem = menu.findItem(GlobalConsts.MENU_SHOWHIDE);
                if (menuItem != null) {
                    menuItem.setTitle(Settings.instance().getShowDotAndHiddenFiles() ? R.string.operation_hide_sysfile
                            : R.string.operation_show_sysfile);
                }
            }
        }
    }

    public boolean isFileSelected(String filePath) {
        return mFileOperationHelper.isFileSelected(filePath);
    }

    public void setMode(Mode m) {
        mCurrentMode = m;
    }

    public Mode getMode() {
        return mCurrentMode;
    }

    public void setSelectionMode(boolean selected) {
        if (mInSelection == selected) {
            return;
        }

        mInSelection = selected;
        if (mInSelection) {
            if (mContext instanceof IActionModeCtr) {
                ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
                if (actionMode == null) {
                    actionMode = ((IActionModeCtr) mContext).startActionMode(new ModeCallback(mContext, FileViewInteractionHub.this));
                    ((IActionModeCtr) mContext).setActionMode(actionMode);
                } else {
                    actionMode.invalidate();
                }
                Util.updateActionModeTitle(actionMode, mContext, getSelectedFileList().size());
            }

            if (mSeparateMenu != null && mSeparateMenu.getVisibility() != View.GONE) {
                mSeparateMenu.setVisibility(View.INVISIBLE);
            }
        }
        mFileViewListener.onDataChanged();
    }

    public boolean getSelectionMode() {
        return mInSelection;
    }

    public boolean onListItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!Util.isOneStepMode(mContext)) {
            return false;
        }

        final int N = mCheckedFileNameList.size();
        if (N >= 1) {
            boolean isAllImage = true;
            File[] checkFiles = new File[N];
            String[] mimeTypes = new String[N];
            for (int i = 0; i < N; i++) {
                FileInfo fileInfo = mCheckedFileNameList.get(i);
                if (fileInfo.IsDir) {
                    return false;
                }
                checkFiles[i] = new File(fileInfo.filePath);
                mimeTypes[i] = fileInfo.mFileMimeType;
                if (isAllImage) {
                    if (!mimeTypes[i].startsWith("image/")) {
                        isAllImage = false;
                    }
                }
            }

            if (isAllImage) {
                Util.dragMultipleImage(view, mContext, 0, checkFiles, mimeTypes, 0);
            } else {
                FileInfo lFileInfo = mCheckedFileNameList.get(N - 1);
                int iconId = lFileInfo.mFileIconResId;
                if (iconId == 0) {
                    iconId = R.drawable.file_icon_onestep_default;
                }
                Util.dragMultipleFile(view, mContext, checkFiles, mimeTypes, null, iconId);
            }
        } else {
            FileInfo lFileInfo = mFileViewListener.getItem(position);
            File file = new File(lFileInfo.filePath);
            boolean isDir = lFileInfo.IsDir;
            if (isDir) {
                return false;
            }
            String mimeType = lFileInfo.mFileMimeType;
            int iconId = lFileInfo.mFileIconResId;
            if (mimeType.startsWith("image/") && iconId == 0) {
                Util.dragImage(view, mContext, null, file, mimeType);
            } else if (iconId != 0) {
                Util.dragFile(view, mContext, file, mimeType, iconId);
            }
        }

        return true;
    }

    public void onListItemClick(AdapterView<?> parent, View view, int position, long id) {
        View drop = view.getRootView().findViewById(R.id.dropdown_navigation);
        if (drop != null && drop.getVisibility() != View.GONE) {
            drop.setVisibility(View.GONE);
        }

        FileInfo lFileInfo = mFileViewListener.getItem(position);

        if (lFileInfo == null) {
            mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);
            Log.e(LOG_TAG, "file does not exist on position:" + position);
            return;
        }

        if (isInSelection()) {
            boolean selected = lFileInfo.Selected;
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.file_checkbox);
            if (selected) {
                mCheckedFileNameList.remove(lFileInfo);
                checkBox.setChecked(false);
            } else {
                mCheckedFileNameList.add(lFileInfo);
                checkBox.setChecked(true);
            }

            if (mContext instanceof IActionModeCtr) {
                ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
                if (actionMode != null) {
                    if (mCheckedFileNameList.size() == 0) {
                        Util.updateActionModeTitle(actionMode, mContext, mCheckedFileNameList.size());
                        actionMode.invalidate();
                        //mFileViewListener.onDataChanged();
                    } else {
                        Util.updateActionModeTitle(actionMode, mContext, mCheckedFileNameList.size());
                        actionMode.invalidate();
                    }
                }
            }
            lFileInfo.Selected = !selected;
            if (mCleanBtn != null) {
                mCleanBtn.setEnabled(mCheckedFileNameList.size() > 0);
            }
            return;
        }

        if (!lFileInfo.IsDir) {
            if (mCurrentMode == Mode.Pick) {
                mFileViewListener.onPick(lFileInfo);
            } else {
                viewFile(lFileInfo);
            }
            return;
        } else if (lFileInfo.IsDir && lFileInfo.bucketId!= null) {
            Fragment fragment = new FastCategoryDetailsFragment();
            FragmentManager fm = mFileViewListener.getFragmentM();
            Bundle bundle = new Bundle();
            bundle.putSerializable(EXTRA_CATEGORY_TAG, FileCategoryHelper.FileCategory.Picture);
            bundle.putString(EXTRA_BUCKETID_INFO, lFileInfo.bucketId);
            bundle.putString(EXTRA_BUCKET_NAME, lFileInfo.fileName);
            fragment.setArguments(bundle);
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment_container, fragment);
            ft.addToBackStack("picture_folder_list");
            ft.commitAllowingStateLoss();
            return;
        }

        setCurrentPath(getAbsoluteName(mCurrentPath, lFileInfo.fileName));

        //function about the page of internal and sd can click
        if (mContext instanceof IActionModeCtr) {
            ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
            if (actionMode != null) {
                actionMode.finish();
                refreshFileList();
            }
        }
    }

    public void setRootPath(String path) {
        mRoot = path;
        if (Util.PAGE_EXPLORER == this.mTabIndex) {
            StorageHelper.getInstance(this.mContext).setCurrentMountPoint(path);
        }
        setCurrentPath(path);
    }

    public String getRootPath() {
        return mRoot;
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    public void setCurrentPath(String path) {
        mCurrentPath = path;
        mPathGallery.setPath(mFileViewListener.getDisplayPath(mCurrentPath));
        refreshFileList();
    }

    private String getAbsoluteName(String currentPath, String fielName) {
        if (currentPath.equals("/")) {
            return currentPath + fielName;
        }
        return currentPath + "/" + fielName;
    }

    // check or uncheck
    public boolean onCheckItem(FileInfo f, View v) {
        if (mTabIndex == Util.PAGE_EXPLORER) {
            mFileViewListener.hideVolumesList();
        }

        if (inMoveState())
            return false;

        if (isSelectingFiles() && f.IsDir)
            return false;

        if (f.Selected) {
            mCheckedFileNameList.add(f);
        } else {
            mCheckedFileNameList.remove(f);
        }
        return true;
    }

    private boolean isSelectingFiles() {
        return mSelectFilesCallback != null;
    }

    public boolean isSelectedAll() {
        return mFileViewListener.getItemCount() != 0
                && mCheckedFileNameList.size() != 0
                && mCheckedFileNameList.size() == mFileViewListener.getItemCount();
    }

    public boolean isSelected() {
        return mCheckedFileNameList.size() != 0;
    }

    public void clearSelection() {
        //function about the page of internal and sd can click
        if (mContext instanceof IActionModeCtr) {
            ActionMode actionMode = ((IActionModeCtr) mContext).getActionMode();
            if (actionMode != null) {
                exitActionMode();
                if (mCheckedFileNameList.size() > 0) {
                    for (FileInfo f : mCheckedFileNameList) {
                        if (f == null) {
                            continue;
                        }
                        f.Selected = false;
                    }
                    mCheckedFileNameList.clear();
                    mFileViewListener.onDataChanged();
                }
            }
        }

    }

    public void actionModeClearSelection() {
        if (mCheckedFileNameList.size() >= 0) {
            for (FileInfo f : mCheckedFileNameList) {
                if (f == null) {
                    continue;
                }
                f.Selected = false;
            }
            mCheckedFileNameList.clear();
            mFileViewListener.onDataChanged();
        }
    }

    public void exitActionMode() {
        setSelectionMode(false);
        actionModeClearSelection();
        //function about the page of internal and sd can click
        if (mContext instanceof IActionModeCtr) {
            ActionMode mode = ((IActionModeCtr) mContext).getActionMode();
            if (mode != null) {
                mode.finish();
            }
        }

        if (mSeparateMenu != null && mSeparateMenu.getVisibility() != View.GONE) {
            mSeparateMenu.setVisibility(View.VISIBLE);
        }
    }

    private void viewFile(FileInfo lFileInfo) {
        if (!mIsCopy) {
            try {
                IntentBuilder.viewFile(mContext, lFileInfo.filePath, FileViewInteractionHub.this);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
    }

    public int getTabIndex() {
        return this.mTabIndex;
    }

    public boolean isRootPath() {
        return this.mRoot.equals(this.mCurrentPath);
    }

    public boolean onBackPressed() {
        if (mDropdownNavigation.getVisibility() == View.VISIBLE) {
            mDropdownNavigation.setVisibility(View.GONE);
        } else if (isInSelection()) {
            clearSelection();
        } else if (!onOperationUpLevel()) {
            return false;
        }
        if (mTabIndex == Util.PAGE_DETAILS || mTabIndex == Util.PAGE_PICFOLDER) {
            mFileViewListener.showPathGalleryNavbar(false);
        } else {
            this.mPathGallery.setPath(this.mFileViewListener.getDisplayPath(this.mCurrentPath));
        }
        return true;
    }

    public void copyFile(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
    }

    public void moveFileFrom(ArrayList<FileInfo> files) {
        mFileOperationHelper.StartMove(files);
        showConfirmOperationBar(true);
        updateConfirmButtons();
        // refresh to hide selected files
        refreshFileList();
    }

    private static final int SHOW_NAVIGATION_TIMER = 1111;
    private static final int SHOW_NAVIGATION_WAITING_TIMER = 80;
    private Timer clickWaitTimer;
    boolean isShowNavigation = false;
    private Handler navigationhandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_NAVIGATION_TIMER:
                    isTimerShowDropdownNavigation();
                    break;
            }
            super.handleMessage(msg);
        }

    };

    public void isTimerShowDropdownNavigation() {
        mDropdownNavigation.setVisibility(isShowNavigation ? View.VISIBLE : View.GONE);
    }

    private void showDropdownNavigation(boolean show) {
        isShowNavigation = show;
        if (clickWaitTimer != null) {
            clickWaitTimer.cancel();
        }
        clickWaitTimer = new Timer();
        clickWaitTimer.schedule(new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = SHOW_NAVIGATION_TIMER;
                navigationhandler.sendMessage(message);
            }
        }, SHOW_NAVIGATION_WAITING_TIMER);
    }

    @Override
    public void onFileChanged(int type) {
        if (type == GlobalConsts.TYPE_NOTIFY_REFRESH) {
            refreshFileList();
            notifyRefreshViewInfo();
        } else if (type == GlobalConsts.TYPE_NOTIFY_SCAN) {
            notifyFileChanged();
        } else if (type == GlobalConsts.TYPE_MOVE_NOTIFY_SCAN) {
            moveNotifyRefreshViewInfo();
        }
    }

    public void notifyRefreshViewInfo() {
        Intent intent = new Intent(GlobalConsts.BROADCAST_REFRESH, Uri.fromFile(new File(mCurrentPath)));
        Log.i(LOG_TAG, "mCurrentPath=" + mCurrentPath);
        if (mTabIndex == Util.PAGE_DETAILS || mTabIndex == Util.PAGE_PICFOLDER) {
            intent.putExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, GlobalConsts.BROADCAST_REFRESH_TABCATEGORY);
        } else if (mTabIndex == Util.PAGE_EXPLORER) {
            intent.putExtra(GlobalConsts.BROADCAST_REFRESH_EXTRA, GlobalConsts.BROADCAST_REFRESH_TABVIEW);
        }
        mContext.sendBroadcast(intent);
    }

    //notify scan when move
    public void moveNotifyRefreshViewInfo() {
        if (TextUtils.isEmpty(mCurrentPath)) {
            return;
        }
        Util.scanAllFile(mContext, new String[]{mCurrentPath});
    }

    private void notifyFileChanged() {
        if (TextUtils.isEmpty(mCurrentPath)) {
            return;
        }
        Util.scanAllFile(mContext, new String[]{mCurrentPath});
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        mSelectFilesCallback = callback;
        showConfirmOperationBar(true);
        updateConfirmButtons();
    }

    public void showPathGallery(boolean paramBoolean) {
        View localView = this.mFileViewListener.getViewById(R.id.path_gallery_nav);
        if (paramBoolean) {
            localView.setVisibility(View.VISIBLE);
        } else {
            localView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPathItemClickListener(String paramString) {
        String clickPath = this.mFileViewListener.getRealPath(paramString);
        if (mTabIndex == Util.PAGE_DETAILS || mTabIndex == Util.PAGE_PICFOLDER) {
            if (ROOT_DIR.equals(clickPath)) {
                exitActionMode();
                mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL);
            }
            return;
        } else if (mTabIndex == Util.PAGE_EXPLORER) {
            if (this.mCurrentPath.equals(clickPath))
                return;
            exitActionMode();
            setCurrentPath(clickPath);
        } else if (mTabIndex == Util.PAGE_STORAGE) {
            mFileViewListener.finish();
        }
    }

    public boolean isLoadingShowing() {
        if (progressDialog != null && progressDialog.isShowing()) {
            return true;
        } else {
            return false;
        }
    }

    public void showRefreshProgress(boolean show) {
        if (show) {
            mRefreshProgressBar.setVisibility(View.VISIBLE);
        } else {
            mRefreshProgressBar.setVisibility(View.GONE);
        }
    }
}
