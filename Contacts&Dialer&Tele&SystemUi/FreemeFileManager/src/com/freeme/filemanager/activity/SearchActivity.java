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
package com.freeme.filemanager.activity;

import java.io.File;

import com.freeme.filemanager.R;
import com.freeme.filemanager.controller.IntentBuilder;
import com.freeme.filemanager.model.FileInfo;
import com.freeme.filemanager.util.FileIconHelper;
import com.freeme.filemanager.util.PermissionUtil;
import com.freeme.filemanager.util.Util;
import com.freeme.filemanager.view.Settings;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Files.FileColumns;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

public class SearchActivity extends Activity implements
        AdapterView.OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private EditText mSearchEditText;
    private ImageButton mbtnClear;
    private ListView mSearchListView;
    private TextView mNoMatch;
    private static final String[] SEARCH_COLUMNS = { "_id", "_data",
            "mime_type" };
    private static final Uri FILE_URI = MediaStore.Files.getContentUri("external");
    private Cursor mCursor = null;
    private String mQuery;
    private SearchFileAdapter mAdapter;
    private String selection;
    Uri pathUri;
    private String categoryselection;
    private String mSelectString = FileColumns.DISPLAY_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!PermissionUtil.hasSecurityPermissions(this)) {
            //finish activity if has no permission
            finish();
        }
        setContentView(R.layout.activity_search);
        pathUri = getIntent().getData();
        categoryselection = getIntent().getStringExtra("category_selection");

        getActionBar().setDisplayShowTitleEnabled(false);
        getActionBar().setDisplayShowCustomEnabled(true);

        View actionbarLayout = LayoutInflater.from(this).inflate(
                R.layout.layout_actionar_searchview, null);
        SearchView searchView = (SearchView) actionbarLayout.findViewById(R.id.searchView);

        int imgId = searchView.getContext().getResources().getIdentifier("android:id/search_mag_icon",null,null);
        ImageView searchButton = (ImageView)searchView.findViewById(imgId);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(searchButton.getLayoutParams());
        int margin = getResources().getDimensionPixelSize(R.dimen.memory_info_layout_top);
        lp.setMarginStart(margin);
        searchButton.setLayoutParams(lp);

        imgId = searchView.getContext().getResources().getIdentifier("android:id/search_close_btn",null,null);
        ImageView cancelButton = (ImageView)searchView.findViewById(imgId);
        lp = new LinearLayout.LayoutParams(cancelButton.getLayoutParams());
        margin = getResources().getDimensionPixelSize(R.dimen.separate_menu_padding_right);
        lp.setMarginEnd(margin);

        TextView cancelView = (TextView) actionbarLayout.findViewById(R.id.search_cancel);
        cancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getActionBar().setCustomView(actionbarLayout);

        SpannableString ss = new SpannableString(getString(R.string.search_hint));
        AbsoluteSizeSpan ass = new AbsoluteSizeSpan(12,true);
        ss.setSpan(ass, 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        searchView.setQueryHint(new SpannedString(ss));

        searchView.onActionViewExpanded();
        getActionBar().setDisplayHomeAsUpEnabled(false);
        searchView.setOnQueryTextListener(mOnQueryTextListener);

        mSearchListView = (ListView) findViewById(R.id.search_list_view);
        this.mAdapter = new SearchFileAdapter(this, R.layout.layout_search_item, null);
        mSearchListView.setAdapter(this.mAdapter);

        mSearchListView.setOnItemClickListener(this);
        mNoMatch = (TextView) findViewById(R.id.no_match);
        getLoaderManager().initLoader(0, null, this);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public Loader<Cursor> onCreateLoader(int paramInt, Bundle paramBundle) {
        if (paramInt == 1) {
            Uri baseUri;
            if (pathUri!= null) {
                baseUri = pathUri;
            } else {
                baseUri = FILE_URI;
            }
            //FIXME: adapter later
            if (baseUri.equals(FILE_URI)) {
                mSelectString = " file_name";
            }

            String selections = mSelectString + " like '%" + sqliteEscape(mQuery) + "%' ";

            if (categoryselection != null && categoryselection.length() > 0) {
                selections = "(" + selections + ") and (" + categoryselection + ")";
            }

            if (Settings.instance().getShowDotAndHiddenFiles()) {
                selection = selections;
            } else {
                selection = "(" + selections+") AND "+ (mSelectString  +" NOT LIKE '.%'");
            }

            String[] selectionArgs = new String[] { this.mQuery };
            return new CursorLoader(this, baseUri, SEARCH_COLUMNS, selection,
                    null, " date_modified DESC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> paramLoader, Cursor paramCursor) {
        this.mAdapter.swapCursor(paramCursor);
        if (paramCursor != null && paramCursor.getCount() > 0) {
            mNoMatch.setVisibility(View.GONE);
        } else {
            mNoMatch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> paramLoader) {
        this.mAdapter.swapCursor(null);
    }

    public boolean onQueryTextSubmit(String paramString) {
        this.mQuery = paramString;
        if (mQuery == null || TextUtils.isEmpty(mQuery)) {
            return true;
        }
        getLoaderManager().restartLoader(1, null, this);
        return true;
    }

    private OnQueryTextListener mOnQueryTextListener = new OnQueryTextListener() {
        @Override
        public boolean onQueryTextChange(String newText) {
            SearchActivity.this.onQueryTextSubmit(newText);
            if(TextUtils.isEmpty(newText)){
                mAdapter.swapCursor(null);
                mNoMatch.setVisibility(View.VISIBLE);
            }
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return false;
        }
    };


    @Override
    public void onItemClick(AdapterView<?> paramAdapterView, View paramView,
            int paramInt, long paramLong) {
        View view = this.getWindow().peekDecorView();
        if (view != null) {
            InputMethodManager inputmanger = (InputMethodManager)getSystemService(SearchActivity.INPUT_METHOD_SERVICE);
            inputmanger.hideSoftInputFromWindow(view.getWindowToken(),
                    0);
        }
        FileInfo fileInfo = Util.GetFileInfo(((Cursor) this.mSearchListView
                .getAdapter().getItem(paramInt)).getString(1));
        if(fileInfo!=null){
            if ((TextUtils.isEmpty(fileInfo.filePath))
                    || (!fileInfo.IsDir)) {
                try {
                    if(fileInfo != null) {
                        IntentBuilder.viewFile(this, fileInfo.filePath);
                        return;
                    }
                } catch (ActivityNotFoundException localActivityNotFoundException) {

                }
                return;
            }
            Intent intent = new Intent(this, FileExplorerTabActivity.class);
            String folderPath = fileInfo.filePath;
            intent.setData(Uri.parse(folderPath));
            intent.putExtra("isSearch", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    private class SearchFileAdapter extends ResourceCursorAdapter {

        public SearchFileAdapter(Context context, int layout, Cursor c) {
            super(context, layout, c);
        }

        public void bindView(View paramView, Context paramContext,
                Cursor paramCursor) {
            SearchActivity.SearchFileView localSearchFileView = (SearchActivity.SearchFileView) paramView
                    .getTag();
            String str = paramCursor.getString(1);
            localSearchFileView.title.setText(Util.getNameFromFilepath(str));
            localSearchFileView.path.setText(str);
            if (new File(str).isDirectory()) {
                localSearchFileView.icon.setImageResource(R.drawable.folder);
                return;
            }
            FileInfo fileInfo = new FileInfo();
            fileInfo.filePath = str;
            localSearchFileView.icon.setTag(str);
            FileIconHelper fileIconHelper = new FileIconHelper(paramContext);
            fileIconHelper.setIcon(paramContext, fileInfo, localSearchFileView.icon);
        }

        public View newView(Context paramContext, Cursor paramCursor,
                ViewGroup paramViewGroup) {
            View view = super
                    .newView(paramContext, paramCursor, paramViewGroup);
            view.setTag(new SearchActivity.SearchFileView(view));
            return view;
        }
    }

    private static final class SearchFileView {
        final ImageView icon;
        final TextView path;
        final TextView title;

        public SearchFileView(View paramView) {
            this.icon = ((ImageView) paramView
                    .findViewById(R.id.search_file_image));
            this.title = ((TextView) paramView
                    .findViewById(R.id.search_file_name));
            this.path = ((TextView) paramView
                    .findViewById(R.id.search_file_path));
        }
    }
    public static String sqliteEscape(String keyWord){
        keyWord = keyWord.replace("/", "//");
        keyWord = keyWord.replace("'", "''");
        keyWord = keyWord.replace("[", "/[");
        keyWord = keyWord.replace("]", "/]");
        keyWord = keyWord.replace("%", "/%");
        keyWord = keyWord.replace("&","/&");
        keyWord = keyWord.replace("_", "/_");
        keyWord = keyWord.replace("(", "/(");
        keyWord = keyWord.replace(")", "/)");
        return keyWord;
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
    }
}
