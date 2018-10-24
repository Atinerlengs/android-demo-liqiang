package com.freeme.game.apppicker;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.freeme.game.R;
import com.freeme.game.apppicker.loader.GmAppModel;
import com.freeme.game.apppicker.loader.GmAppLoader;
import com.freeme.game.database.GmDatabaseConstant;
import com.freeme.game.receiver.GmAppInstallReceiver;
import com.freeme.game.utils.GmAppConfigManager;
import com.freeme.actionbar.app.FreemeActionBarUtil;

public class GmAppPickerActivity extends ListActivity implements
        AdapterView.OnItemClickListener, GmAppInstallReceiver.IAppChangedCallBack {

    private ListView mListView;

    private GmAppLoader mLoader;
    private GmAppPickerAdapter mAdapter;
    private GmAppConfigManager mAppConfigManager;

    private List<GmAppModel> mAppList = new ArrayList<>();

    private class PickResultHandler extends Handler {
        private PickResultHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mAdapter != null && mLoader != null) {
                mAppList.clear();
                mAppList.addAll(mLoader.getAppListByType(msg.what));
                mAppList = mAppConfigManager.resortGmAppListByRecommend(mAppList);
                mAdapter.setData(mAppList);
                if (mAppList.isEmpty()) {
                    View emptyView = findViewById(R.id.list_tips_view);
                    emptyView.setVisibility(View.VISIBLE);
                    mListView.setEmptyView(emptyView);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        FreemeActionBarUtil.setNavigateTitle(this, getIntent());

        setContentView(R.layout.gm_activity_apps_picker);

        Handler handler = new PickResultHandler(Looper.myLooper());
        mAppConfigManager = new GmAppConfigManager(this);
        mLoader = new GmAppLoader(this, getPackageManager(), handler, getLoaderManager());
        GmAppInstallReceiver.registerCallBack(this);

        init();
    }

    @Override
    protected void onDestroy() {
        GmAppInstallReceiver.unregisterCallBack(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        GmAppModel appModel = (GmAppModel) mListView.getItemAtPosition(position);
        boolean isSelected = !mAdapter.isSelected(position);
        mAdapter.setSelected(position, isSelected);
        String pkgName = appModel.getPkgName();

        Bundle bundle = new Bundle();
        bundle.putString(GmDatabaseConstant.Columns.COLUMN_APP_PACKAGE_NAME, pkgName);
        bundle.putInt(GmDatabaseConstant.Columns.COLUMN_APP_SELECTED, isSelected ? 1 : 0);
        getContentResolver().call(GmDatabaseConstant.CONTENT_URI,
                GmDatabaseConstant.Methods.METHOD_UPDATE, null, bundle);
    }

    @Override
    public void onAppChanged() {
        updateListData();
    }

    private void init() {
        mListView = getListView();
        mAdapter = new GmAppPickerAdapter(this, mAppList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        View mLoadingContainer = findViewById(R.id.loading_container);
        mLoader.setLoadingContainer(mLoadingContainer);
        updateListData();
    }

    private void updateListData() {
        if (!mLoader.isLoading()) {
            mLoader.initData(GmAppLoader.LOAD_TYPE_UNSELECTED_APP);
        }
    }
}
