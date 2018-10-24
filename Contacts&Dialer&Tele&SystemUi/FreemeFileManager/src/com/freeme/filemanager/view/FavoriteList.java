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

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.FavoriteListAdapter;
import com.freeme.filemanager.controller.IntentBuilder;
import com.freeme.filemanager.fragment.FileExplorerViewFragment;
import com.freeme.filemanager.util.FavoriteDatabaseHelper;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.util.FavoriteDatabaseHelper.FavoriteDatabaseListener;

import java.io.File;
import java.util.ArrayList;

public class FavoriteList implements FavoriteDatabaseListener {
    private static final String LOG_TAG = "FavoriteList";

    private ArrayList<FavoriteItem> mFavoriteList = new ArrayList<FavoriteItem>();

    private ArrayAdapter<FavoriteItem> mFavoriteListAdapter;

    private FavoriteDatabaseHelper mFavoriteDatabase;

    private ListView mListView;

    private FavoriteDatabaseListener mListener;

    private Context mContext;

    public FavoriteList(Context context, ListView list, FavoriteDatabaseListener listener,
            FileIconHelper iconHelper) {
        mContext = context;

        mFavoriteDatabase = new FavoriteDatabaseHelper(context, this);
        mFavoriteListAdapter = new FavoriteListAdapter(context, R.layout.layout_file_list_item,
                mFavoriteList, iconHelper);
        setupFavoriteListView(list);
        mListener = listener;
    }
    
    public FavoriteList(Context context, FavoriteDatabaseListener listener) {
        mContext = context;
        mFavoriteDatabase = new FavoriteDatabaseHelper(context, this);
        mListener = listener;
    }

    public ArrayAdapter<FavoriteItem> getArrayAdapter() {
        return mFavoriteListAdapter;
    }

    public void update() {
        mFavoriteList.clear();

        Cursor c = mFavoriteDatabase.query();
        if (c != null) {
            while (c.moveToNext()) {
                FavoriteItem item = new FavoriteItem(c.getLong(0),
                        c.getString(1), c.getString(2));
                item.fileInfo = Util.GetFileInfo(item.location);
                mFavoriteList.add(item);
            }
            c.close();
        }

        // remove not existing items
        // if (Util.isSDCardReady()) {
        for (int i = mFavoriteList.size() - 1; i >= 0; i--) {
            File file = new File(mFavoriteList.get(i).location);
            if (file.exists())
                continue;

            FavoriteItem favorite = mFavoriteList.get(i);
            mFavoriteDatabase.delete(favorite.id, false);
            mFavoriteList.remove(i);
        }
        // }
        if (mListView != null) {
            mFavoriteListAdapter.notifyDataSetChanged();
        }
    }

    public void initList() {
        mFavoriteList.clear();
        Cursor c = mFavoriteDatabase.query();
        if (c != null)
            c.close();

        if (mFavoriteDatabase.isFirstCreate()) {
            for (FavoriteItem fi : Util.getDefaultFavorites(mContext)) {
                mFavoriteDatabase.insert(fi.title, fi.location);
            }
        }

        update();
    }

    public long getCount() {
        return mFavoriteList.size();
    }

    public void show(boolean show) {
        mListView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setupFavoriteListView(ListView list) {
        mListView = list;
        mListView.setAdapter(mFavoriteListAdapter);
        mListView.setLongClickable(true);
        mListView.setOnCreateContextMenuListener(mListViewContextMenuListener);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onFavoriteListItemClick(parent, view, position, id);
            }
        });
    }

    public void onFavoriteListItemClick(AdapterView<?> parent, View view, int position, long id) {
        FavoriteItem favorite = mFavoriteList.get(position);
        File file = new File(favorite.location);
        if((favorite.fileInfo == null) || !file.exists()){
            deleteFavorite(position);
            Toast.makeText(mContext, R.string.favorite_file_not_exist, Toast.LENGTH_LONG).show();
            return ;
        }

        //Favorite item is dir, need to skip to FileExplorerViewFragment with location
        if (favorite.fileInfo.IsDir) {
            FileExplorerTabActivity activity = (FileExplorerTabActivity) mContext;

            FileExplorerViewFragment fragment = new FileExplorerViewFragment();
            FragmentManager fm = activity.getFragmentManager();
            Bundle bundle = new Bundle();
            bundle.putSerializable("current_favorite_location", favorite.location);
            fragment.setArguments(bundle);
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.first_page, fragment);
            ft.addToBackStack(null);
            ft.commitAllowingStateLoss();

        } else {
            try {
                IntentBuilder.viewFile(mContext, favorite.fileInfo.filePath);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
    }

    private static final int MENU_UNFAVORITE = 100;

    // context menu
    private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            menu.add(0, MENU_UNFAVORITE, 0, R.string.operation_unfavorite)
                    .setOnMenuItemClickListener(menuItemClick);
        }
    };

    private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int itemId = item.getItemId();
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            int position = info != null ? info.position : -1;

            switch (itemId) {
                case MENU_UNFAVORITE:
                    if (position != -1) {
                        deleteFavorite(position);
                    }
                    break;

                default:
                    return false;
            }

            return true;
        }
    };

    private void deleteFavorite(int position) {
        FavoriteItem favorite = mFavoriteList.get(position);
        mFavoriteDatabase.delete(favorite.id, false);
        mFavoriteList.remove(position);
        mFavoriteListAdapter.notifyDataSetChanged();
        mListener.onFavoriteDatabaseChanged();
    }

    @Override
    public void onFavoriteDatabaseChanged() {
        update();
        mListener.onFavoriteDatabaseChanged();
    }
}
