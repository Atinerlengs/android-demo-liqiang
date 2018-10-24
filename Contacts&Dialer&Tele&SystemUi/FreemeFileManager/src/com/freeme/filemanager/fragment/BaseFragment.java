package com.freeme.filemanager.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freeme.filemanager.controller.IBackHandledInterface;

import java.lang.reflect.Field;

public class BaseFragment extends Fragment {

    private final static String TAG = "BaseFragment";

    private boolean isInit;
    private boolean isCreated;
    private boolean isVisiBle;
    private boolean isCreateView;
    private boolean isDetached;
    protected IBackHandledInterface mBackHandledInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCreated = true;

        if(!(getActivity() instanceof IBackHandledInterface)){
            throw new ClassCastException("Hosting activity must implement IBackHandledInterface");
        }else{
            this.mBackHandledInterface = (IBackHandledInterface)getActivity();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mBackHandledInterface.setSelectedFragment(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View view = onFragmentCreateView(inflater, container,
                savedInstanceState);
        if (view != null) {
            isCreateView = true;
           /* if (isVisiBle) {
                initFragementData();
            }*/
            return view;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public View onFragmentCreateView(LayoutInflater inflater,
                                     ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onFragmentCreateView");
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (isVisiBle) {
            fragmentShow();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isVisiBle) {
            fragmentHint();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint" + "" + isVisibleToUser);
//        if (!isCreated) {
//            return;
//        }
        if (isVisibleToUser) {
            isVisiBle = true;
//            if (isCreateView) {
//                fragmentShow();
            pagerUserVisible();
//            }
        } else {
            isVisiBle = false;
            fragmentHint();
            pagerUserHide();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        isDetached = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isDetached = true;
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void fragmentHint() {
        Log.i(TAG, "fragmentHint");
    }

    public void fragmentShow() {
        Log.i(TAG, "fragmentShow");
        if (isCreateView) {
            initFragementData();
        }
    }

    private void initFragementData() {
        Log.i(TAG, "initFragementData isInit=" + isInit);
        if (isInit || !isVisiBle) {
            return;
        }

        isInit = true;
        initUserData();
    }

    public void initUserData() {
        Log.i(TAG, "initUserData");
    }

    protected void pagerUserVisible() {
        fragmentShow();
    }

    protected void pagerUserHide() {

    }

    public boolean isVisiBle() {
        return isVisiBle;
    }

    public void setVisiBle(boolean isVisiBle) {
        this.isVisiBle = isVisiBle;
    }


    public boolean isFragmentDetached() {
        return isDetached;
    }

    public boolean setPath(String path) {
        return false;
    }
}
