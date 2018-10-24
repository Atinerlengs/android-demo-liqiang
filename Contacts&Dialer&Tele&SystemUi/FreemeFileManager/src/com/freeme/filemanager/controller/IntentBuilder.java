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

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.EditUtility;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.ArchiveHelper;
import com.freeme.filemanager.util.MimeUtils;
import com.freeme.filemanager.util.Util;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class IntentBuilder {

    public static void viewFile(Context context, String filePath) {
        viewFile(context, filePath, null, false, null);
    }

    public static void viewFile(Context context, String filePath, boolean fromEncryption, String originPath) {
        viewFile(context, filePath, null, fromEncryption, originPath);
    }

    public static void viewFile(Context context, String filePath, FileViewInteractionHub fileViewInteractionHub) {
        viewFile(context, filePath, fileViewInteractionHub, false, null);
    }

    public static void viewFile(final Context context, final String filePath,
                                FileViewInteractionHub fileViewInteractionHub, boolean fromEncryption, String originPath) {
        if (ArchiveHelper.checkIfArchive(filePath)) {
            if (fileViewInteractionHub != null) {
                viewArchive(context, filePath, fileViewInteractionHub);
            }
            return;
        }

        String type = getMimeType(context, filePath);
        if (fromEncryption) {
            type = getMimeType(context, originPath);
        }
        String fileRealPath = filePath;
        if (!TextUtils.isEmpty(type) && !TextUtils.equals(type, "*/*")) {
            /* 设置intent的file与MimeType */
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(fileRealPath)), type);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e("IntentBuilder", "fail to view file, type: " + type);
                Toast.makeText(context, R.string.msg_unable_open_file, Toast.LENGTH_SHORT).show();
            }
        } else {
            // unknown MimeType
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle(R.string.dialog_select_type);

            CharSequence[] menuItemArray = new CharSequence[] {
                    context.getString(R.string.dialog_type_text),
                    context.getString(R.string.dialog_type_audio),
                    context.getString(R.string.dialog_type_video),
                    context.getString(R.string.dialog_type_image) };
            dialogBuilder.setItems(menuItemArray,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selectType = "*/*";
                            switch (which) {
                            case 0:
                                selectType = "text/plain";
                                break;
                            case 1:
                                selectType = "audio/*";
                                break;
                            case 2:
                                selectType = "video/*";
                                break;
                            case 3:
                                selectType = "image/*";
                                break;
                            }
                            Intent intent = new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(new File(filePath)), selectType);
                            try {
                            context.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Log.e("IntentBuilder", "fail to view file, type: " + intent.getType());
                                Toast.makeText(context, R.string.msg_unable_open_file, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            dialogBuilder.show();
        }
    }

    private static void viewArchive(Context context, final String filePath, final FileViewInteractionHub fileViewInteractionHub){
        if(fileViewInteractionHub == null || fileViewInteractionHub.mTabIndex != Util.PAGE_EXPLORER){
            return;
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
        dialogBuilder.setTitle(fileName);
        CharSequence[] menuItemArray = new CharSequence[] {
                context.getString(R.string.decompress_to_current_dir),
                context.getString(R.string.decompress_to)};
        dialogBuilder.setItems(menuItemArray,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fileViewInteractionHub.mDeComPressFilePath = filePath;
                        switch (which) {
                        case 0:
                            fileViewInteractionHub.onOperationDeCompress();
                            break;
                        case 1:
                            fileViewInteractionHub.updateBarForDeCompress();
                            break;
                        }
                    }
                });
        dialogBuilder.show();
    }

    public static Intent buildSendFile(Context context, ArrayList<FileInfo> files) {
        ArrayList<Uri> uris = new ArrayList<Uri>();

        String mimeType = null;
        String preMineType = null;
        boolean isSameMineType = true;
        int sameStringleng = 0;
        for (FileInfo file : files) {
            if (file.IsDir)
                continue;

            File fileIn = new File(file.filePath);
            mimeType = getSendMimeType(context, file.fileName);

            if(preMineType == null){
                sameStringleng = mimeType.indexOf("/");
                preMineType = mimeType.substring(0,sameStringleng + 1);
            }else {
                if(mimeType.startsWith(preMineType) && isSameMineType){
                    preMineType = mimeType;
                }else {
                    isSameMineType = false;
                }
            }
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }

        if (uris.size() == 0)
            return null;
        boolean multiple = uris.size() > 1;
        Intent intent = new Intent(multiple ? android.content.Intent.ACTION_SEND_MULTIPLE
                : android.content.Intent.ACTION_SEND);
        if (multiple) {
            if(isSameMineType){
                intent.setType(mimeType);
            }else {
                intent.setType("*/*");
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        } else {
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        }

        return intent;
    }

    private static String getMimeType(Context context, String filePath) {
        int dotPosition = filePath.lastIndexOf('.');
        if (dotPosition == -1)
            return "*/*";

        String ext = filePath.substring(dotPosition + 1, filePath.length()).toLowerCase();
        String mimeType;
        if (MimeUtils.is3GPPExtension(ext)) {
            mimeType = EditUtility.getMimeTypeForFile(context, new File(filePath));
        } else {
            mimeType = MimeUtils.guessMimeTypeFromExtension(ext);
        }
        return mimeType != null ? mimeType : "*/*";
    }


    private static String getSendMimeType(Context context, String filePath) {
        int dotPosition = filePath.lastIndexOf('.');
        if (dotPosition == -1)
            return "application/*";
        String ext = filePath.substring(dotPosition + 1, filePath.length()).toLowerCase();
        String mimeType ;
        if (MimeUtils.is3GPPExtension(ext)) {
            mimeType = EditUtility.getMimeTypeForFile(context, new File(filePath));
        } else {
            mimeType = MimeUtils.guessMimeTypeFromExtension(ext);
        }
        return mimeType != null ? mimeType : "application/*";
    }

}
