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

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import java.util.Collection;

import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.FileSortHelper;

public interface IFileInteractionListener {

    View getViewById(int id);

    Context getContext();

    FragmentManager getFragmentM();

    void startActivity(Intent intent);

    void startActivityForResult(Intent intent, int requestCode);

    void onDataChanged();

    void onPick(FileInfo f);

    boolean shouldShowOperationPane();

    boolean onOperation(int id);

    String getDisplayPath(String path);

    String getRealPath(String displayPath);

    void runOnUiThread(Runnable r);

    boolean shouldHideMenu(int menu);

    void showPathGalleryNavbar(boolean show);

    FileIconHelper getFileIconHelper();

    FileInfo getItem(int pos);

    void sortCurrentList(FileSortHelper sort);

    Collection<FileInfo> getAllFiles();

    void addSingleFile(FileInfo file);

    void deleteSingleFile(FileInfo file);

    boolean onRefreshFileList(String path, FileSortHelper sort);

    void onRefreshMenu(boolean visible);

    int getItemCount();

    void hideVolumesList();

    void finish();
}
