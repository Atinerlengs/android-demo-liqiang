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

import java.io.File;

import com.freeme.filemanager.R;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.Util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

public class InformationDialog extends AlertDialog {
    protected static final int ID_USER = 100;
    private FileInfo mFileInfo;
    private FileIconHelper mFileIconHelper;
    private Context mContext;
    private View mView;

    public InformationDialog(Context context, FileInfo f) {
        super(context);
        mFileInfo = f;
        mContext = context;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.dialog_information, null);

        //*start
        // note by droi heqianqian for not show icon when show details on 20151215/
        if (mFileInfo.IsDir) {
            asyncGetSize();
        }

        setTitle(mContext.getString(R.string.details));
        ((TextView) mView.findViewById(R.id.information_name))
        .setText(mFileInfo.fileName);
        ((TextView) mView.findViewById(R.id.information_size))
                .setText(formatFileSizeString(mFileInfo.fileSize));
        ((TextView) mView.findViewById(R.id.information_location))
                .setText(mFileInfo.filePath);
        //*/modified by tyd wulianghuan 20130522 for fix bug[tyd00479821]
        String modifyDateTime = DateUtils.formatDateRange(mContext, mFileInfo.ModifiedDate, mFileInfo.ModifiedDate,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | 
                DateUtils.FORMAT_NUMERIC_DATE );
        ((TextView) mView.findViewById(R.id.information_modified)).setText(modifyDateTime);
        ((TextView) mView.findViewById(R.id.information_canread))
                .setText(mFileInfo.canRead ? R.string.yes : R.string.no);
        ((TextView) mView.findViewById(R.id.information_canwrite))
                .setText(mFileInfo.canWrite ? R.string.yes : R.string.no);
        ((TextView) mView.findViewById(R.id.information_ishidden))
                .setText(mFileInfo.isHidden ? R.string.yes : R.string.no);

        setView(mView);
        setButton(BUTTON_NEGATIVE, mContext.getString(R.string.confirm_know), (DialogInterface.OnClickListener) null);

        super.onCreate(savedInstanceState);
    }

    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ID_USER:
                    Bundle data = msg.getData();
                    long size = data.getLong("SIZE");
                    ((TextView) mView.findViewById(R.id.information_size)).setText(formatFileSizeString(size));
            }
        };
    };

    private AsyncTask task;

    @SuppressWarnings("unchecked")
    private void asyncGetSize() {
        task = new AsyncTask() {
            private long size;

            @Override
            protected Object doInBackground(Object... params) {
                String path = (String) params[0];
                size = 0;
                getSize(path);
                task = null;
                return null;
            }

            private void getSize(String path) {
                if (isCancelled())
                    return;
                File file = new File(path);
                if (file.isDirectory()) {
                    File[] listFiles = file.listFiles();
                    if (listFiles == null)
                        return;

                    for (File f : listFiles) {
                        if (isCancelled())
                            return;

                        getSize(f.getPath());
                    }
                } else {
                    size += file.length();
                    onSize(size);
                }
            }

        }.execute(mFileInfo.filePath);
    }

    private void onSize(final long size) {
        Message msg = new Message();
        msg.what = ID_USER;
        Bundle bd = new Bundle();
        bd.putLong("SIZE", size);
        msg.setData(bd);
        mHandler.sendMessage(msg); 
    }

    private String formatFileSizeString(long size) {
        String ret = "";
        if (size >= 1024) {
            ret = Util.convertStorage(size);
            ret += (" (" + mContext.getResources().getString(R.string.file_sizes, size) + ")");
        } else {
            if(size>1){
            ret = mContext.getResources().getString(R.string.file_sizes, size);
        }else{
            ret = mContext.getResources().getString(R.string.file_size, size);
        }}

        return ret;
    }
}
