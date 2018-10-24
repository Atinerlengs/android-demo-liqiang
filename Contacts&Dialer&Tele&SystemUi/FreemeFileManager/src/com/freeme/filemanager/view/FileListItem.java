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
package com.freeme.filemanager.view;

import java.util.ArrayList;

import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FileViewInteractionHub;
import com.freeme.filemanager.controller.IActionModeCtr;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.Util;

import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileListItem extends LinearLayout {
    private static ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();

    public FileListItem(Context context) {
        super(context);
    }

    public FileListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void bind(Context context, FileInfo fileInfo, FileViewInteractionHub fileViewInteractionHub, FileIconHelper fileIconHelper) {
        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        mCheckedFileNameList = fileViewInteractionHub.getSelectedFileList();
        if (mCheckedFileNameList.size() > 0) {
            for (FileInfo f : mCheckedFileNameList) {
                if (f.filePath.equals(fileInfo.filePath)) {
                    fileInfo.Selected = true;
                }
            }
        }
        FileViewInteractionHub.Mode mode = fileViewInteractionHub.getMode();
        // if in moving mode, show selected file always
        /*if (fileViewInteractionHub.isMoveState()) {
            fileInfo.Selected = fileViewInteractionHub.isFileSelected(fileInfo.filePath);
        }*/

        ActionMode actionMode;
        try {
            actionMode = ((IActionModeCtr) context).getActionMode();
        } catch (ClassCastException e) {
            actionMode = null;
        }

        boolean hasActionMode  = actionMode != null || mode.equals(FileViewInteractionHub.Mode.Pick);

        CheckBox checkbox = (CheckBox) findViewById(R.id.file_checkbox);
        checkbox.setChecked(hasActionMode && fileInfo.Selected);
        checkbox.setVisibility(hasActionMode ? VISIBLE : GONE);
        checkbox.setTag(fileInfo);
        ImageView dir_enter = (ImageView) findViewById(R.id.dir_arrow);
        dir_enter.setVisibility(hasActionMode || !fileInfo.IsDir ? GONE : VISIBLE);

        Util.setText(this, R.id.file_name, fileInfo.fileName);
        TextView file_count_view = (TextView) findViewById(R.id.file_count);
        TextView modified_time_view = (TextView) findViewById(R.id.modified_time);
        if (fileViewInteractionHub.mTabIndex == Util.PAGE_EXPLORER) {
            TextView file_owner_view = (TextView) findViewById(R.id.file_owner);
            file_owner_view.setVisibility(View.VISIBLE);
            Util.setText(this, R.id.file_owner, fileInfo.owner == null ? "" : " | " + fileInfo.owner);
            int childCount = fileInfo.Count;
            String childItem = childCount == 0 ?
                    getResources().getString(R.string.empty_folder) :
                    (childCount == 1 ?
                            context.getResources().getString(R.string.child_count, childCount) :
                            childCount + " " + context.getResources().getString(R.string.child_item_count));
            Util.setText(this, R.id.file_count, fileInfo.IsDir ? childItem : (Util.convertStorage(fileInfo.fileSize)));
        } else {
            if (fileInfo.bucketId != null) {
                Util.setText(this, R.id.file_count, "(" + fileInfo.Count + ")");
            } else {
                Util.setText(this, R.id.file_count, Util.convertStorage(fileInfo.fileSize));
            }
        }

        String modifyDateTime = DateUtils.formatDateRange(context, fileInfo.ModifiedDate, fileInfo.ModifiedDate,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_NUMERIC_DATE);
        Util.setText(this, R.id.modified_time, modifyDateTime);
        ImageView lFileImage = (ImageView) findViewById(R.id.file_image);
        ImageView lFavTagImage = (ImageView) findViewById(R.id.favorite_tag);

        lFileImage.setTag(fileInfo.filePath);
        lFavTagImage.setTag(fileInfo.filePath);
        if (fileInfo.IsDir && fileInfo.bucketId == null) {
            lFileImage.setImageResource(R.drawable.folder);
            if (databaseHelper != null) {
                if (databaseHelper.isFavorite(fileInfo.filePath.toString())) {
                    if (lFavTagImage.getTag().equals(fileInfo.filePath)) {
                        lFavTagImage.setVisibility(View.VISIBLE);
                    }
                } else {
                    lFavTagImage.setVisibility(View.GONE);
                }
            } else {
                lFavTagImage.setVisibility(View.GONE);
            }
        } else {
            fileIconHelper.setIcon(context,fileInfo, lFileImage);

            if (fileInfo.IsDir && fileInfo.bucketId != null) {
                lFavTagImage.setVisibility(View.GONE);
            } else if (databaseHelper != null) {
                if (databaseHelper.isFavorite(fileInfo.filePath.toString())) {
                    if (lFavTagImage.getTag().equals(fileInfo.filePath)) {
                        lFavTagImage.setVisibility(View.VISIBLE);
                    }
                } else {
                    lFavTagImage.setVisibility(View.GONE);
                }
            } else {
                lFavTagImage.setVisibility(View.GONE);
            }
        }
    }

    private static String getFilePath(String filePath) {
        int sepIndex = filePath.lastIndexOf("/");
        if (sepIndex >= 0) {
            return filePath.substring(sepIndex + 1);
        }
        return "";
    }

    public static class ModeCallback implements ActionMode.Callback {
        private Menu mMenu;
        private Context mContext;
        private FileViewInteractionHub mFileViewInteractionHub;

        private static void setItemVisible(Menu menu, int id, boolean visible) {
            if (menu.findItem(id) != null) {
                menu.findItem(id).setVisible(visible);
            }
        }

        private Menu getPrepareMenu(Menu menu) {
            MenuInflater inflater = ((Activity) mContext).getMenuInflater();

            int N = mFileViewInteractionHub.getSelectedFileList().size();
            if (N < 0) {
                return menu;
            }

            menu.clear();
            inflater.inflate(R.menu.file_explorer_menu, menu);

            if (N == 0) {
                setItemVisible(menu, R.id.action_select_all, true);

                setItemVisible(menu, R.id.action_cancel, false);
                setItemVisible(menu, R.id.action_move, false);
                setItemVisible(menu, R.id.action_faverite, false);
                setItemVisible(menu, R.id.action_unfaverite, false);
                setItemVisible(menu, R.id.action_delete, false);
                setItemVisible(menu, R.id.action_copy, false);
                setItemVisible(menu, R.id.action_compress, false);
                setItemVisible(menu, R.id.rename, false);
                setItemVisible(menu, R.id.encryption, false);
                setItemVisible(menu, R.id.details, false);
                setItemVisible(menu, R.id.share, false);
                setItemVisible(menu, R.id.action_more, false);
                return menu;
            }

            final boolean isSingleFile = N == 1;
            setItemVisible(menu, R.id.action_faverite, !mFileViewInteractionHub.isFavoritedFile());
            setItemVisible(menu, R.id.action_unfaverite, mFileViewInteractionHub.isFavoritedFile());
            setItemVisible(menu, R.id.action_cancel, mFileViewInteractionHub.isSelectedAll());
            setItemVisible(menu, R.id.action_select_all, !mFileViewInteractionHub.isSelectedAll());

            if (mFileViewInteractionHub.mTabIndex == Util.PAGE_DETAILS // magic 0 is FastCategoryDetailsFragment
                    || mFileViewInteractionHub.mTabIndex == Util.PAGE_STORAGE) { // magic 3 is StorageFileListActivity
                setItemVisible(menu, R.id.action_compress, false);
                setItemVisible(menu, R.id.action_move, false);
                setItemVisible(menu, R.id.action_copy, false);
                setItemVisible(menu, R.id.details, false);

                setItemVisible(menu, R.id.rename, isSingleFile);
                setItemVisible(menu, R.id.action_more, isSingleFile);
                setItemVisible(menu, R.id.encryption, !isSingleFile);
                setItemVisible(menu, R.id.share, !isSingleFile);

            } else if (mFileViewInteractionHub.mTabIndex == Util.PAGE_EXPLORER) { //magic 1 is FileExplorerViewFragment
                setItemVisible(menu, R.id.action_copy, false);
                setItemVisible(menu, R.id.action_compress, false);
                setItemVisible(menu, R.id.rename, false);
                setItemVisible(menu, R.id.encryption, false);
                setItemVisible(menu, R.id.details, false);
                setItemVisible(menu, R.id.share, false);
            } else if (mFileViewInteractionHub.mTabIndex == Util.PAGE_PICFOLDER) { //magic 4 is FastCategoryPictureDetailsFragment
                setItemVisible(menu, R.id.action_move, false);
                setItemVisible(menu, R.id.action_faverite, false);
                setItemVisible(menu, R.id.action_unfaverite, false);
                setItemVisible(menu, R.id.action_copy, false);
                setItemVisible(menu, R.id.action_compress, false);
                setItemVisible(menu, R.id.rename, false);
                setItemVisible(menu, R.id.encryption, false);
                setItemVisible(menu, R.id.details, false);
                setItemVisible(menu, R.id.share, false);
                setItemVisible(menu, R.id.action_more, false);
            }

            return menu;
        }

        public ModeCallback(Context context,
                            FileViewInteractionHub fileViewInteractionHub) {
            mContext = context;
            mFileViewInteractionHub = fileViewInteractionHub;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mMenu = getPrepareMenu(menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int N = mFileViewInteractionHub.getSelectedFileList().size();
            switch (item.getItemId()) {
                case R.id.action_more:
                    mFileViewInteractionHub.onOperationMoreMenu();
                    break;
                case R.id.action_delete:
                    if (mFileViewInteractionHub.mDeleteFlag) {
                        mFileViewInteractionHub.onOperationDelete();
                    }
                    break;
                case R.id.action_copy:
                    if (N > 1) {
                        setItemVisible(mMenu, R.id.action_compress, false);
                        setItemVisible(mMenu, R.id.action_move, false);
                        setItemVisible(mMenu, R.id.action_copy, false);
                        mode.getMenu().clear();
                        mode.getMenu().close();
                        item.setVisible(false);
                        ((IActionModeCtr) mContext).setActionMode(null);
                    }
                    mFileViewInteractionHub.onOperationCopy();
                    mode.finish();
                    break;
                case R.id.action_faverite:
                    mFileViewInteractionHub.onOperationFavoriteNew(true);
                    mode.finish();
                    break;
                case R.id.action_unfaverite:
                    mFileViewInteractionHub.onOperationFavoriteNew(false);
                    mode.finish();
                    break;
                case R.id.action_move:
                    mFileViewInteractionHub.onOperationMove();
                    mode.finish();
                    break;
                case R.id.action_compress:
                    mFileViewInteractionHub.onOperationCompress();
                    mode.finish();
                    break;
                case R.id.action_cancel:
                    mFileViewInteractionHub.actionModeClearSelection();
                    mode.invalidate();
                    break;
                case R.id.action_select_all:
                    mFileViewInteractionHub.onOperationSelectAllOrCancel();
                    break;
                case R.id.rename:
                    mFileViewInteractionHub.onOperationRenameSingle();
                    break;
                case R.id.share:
                    mFileViewInteractionHub.onOperationSend();
                    break;
                case R.id.details:
                    mFileViewInteractionHub.onOperationInfo();
                    break;
                case R.id.encryption:
                    mFileViewInteractionHub.onActionEntrySafe();
                    break;
            }
            Util.updateActionModeTitle(mode, mContext, mFileViewInteractionHub
                    .getSelectedFileList().size());
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (mContext instanceof IActionModeCtr) {
                ((IActionModeCtr) mContext).setActionMode(null);
            }
            mFileViewInteractionHub.exitActionMode();
        }
    }
}
