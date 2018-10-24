package com.freeme.filemanager.fragment;

import com.freeme.filemanager.activity.FileExplorerTabActivity;
import com.freeme.filemanager.activity.FileExplorerTabActivity.IBackPressedListener;
import com.freeme.filemanager.R;
import com.freeme.filemanager.util.Util;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class FastCategoryContainerFragment extends BaseFragment implements IBackPressedListener {
    private static final String TAG = "FastCategoryContainerFragment";

    private static final int OPERATION_MENU_CLEAN = 20;

    private static final int MSG_INIT = 1;

    private boolean isInit;

    private View mRootView;
    private FragmentManager mFragmentManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_INIT:
                    if (isFragmentDetached()) {
                        return;
                    }

                    FastCategroyFragment fragment = new FastCategroyFragment();
                    mFragmentManager = getFragmentManager();
                    FragmentTransaction transaction = mFragmentManager
                            .beginTransaction();
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.commitAllowingStateLoss();
                    isInit = true;
                    break;
                default:
                    break;
            }
        };
    };

    @Override
    public View onFragmentCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        isInit = false;
        mRootView = inflater.inflate(R.layout.layout_file_explorer_container, null);
        setHasOptionsMenu(true);
        fragmentShow();
        return mRootView;
    }

    @Override
    public void fragmentShow() {
        super.fragmentShow();
        init();
    }

    @Override
    protected void pagerUserHide() {
        if(mFragmentManager == null){
            return;
        }
        Fragment fragment = mFragmentManager
                .findFragmentById(R.id.fragment_container);
        if (fragment != null && (fragment instanceof BaseCategoryFragment)) {
            ((BaseCategoryFragment) fragment).pagerUserHide();
        }
    }

    @Override
    protected void pagerUserVisible() {
        if(mFragmentManager == null){
            return;
        }
        Fragment fragment = mFragmentManager
                .findFragmentById(R.id.fragment_container);
        if (fragment != null && (fragment instanceof BaseCategoryFragment)) {
            ((BaseCategoryFragment) fragment).pagerUserVisible();
        }
    }

    @Override
    public boolean onBack() {
        if (mFragmentManager == null) {
            return false;
        }

        int back_stack_cnt = mFragmentManager.getBackStackEntryCount();
        if (back_stack_cnt == 0) {
            return false;
        } else {
            mFragmentManager.popBackStack();
            String back_tag = mFragmentManager.getBackStackEntryAt(back_stack_cnt -1).getName();

            Activity activity = (FileExplorerTabActivity) getActivity();
            if (back_tag.equals(FastCategroyFragment.BACKSTACK_TAG)) {
                activity.getActionBar().setTitle(R.string.app_name);
                activity.getActionBar().setDisplayHomeAsUpEnabled(false);
            } else {
                activity.getActionBar().setTitle(R.string.category_picture);
                Util.setBackTitle(activity.getActionBar(), getString(R.string.app_name));
            }
            return true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void setConfigurationChanged(boolean isChange) {

    }

    public boolean isHomePage() {
        return false;
    }

    public void init() {
        if (!isInit) {
            mHandler.sendEmptyMessageDelayed(MSG_INIT, 0);
        }
    }
}
