package com.freeme.game.floatingui;

import android.content.Context;
import android.util.ArraySet;
import android.view.WindowManager;

import com.freeme.game.utils.GmLogUtils;
import com.freeme.game.utils.GmNavBarUtils;

public class GmFloatingUIManager {
    private static final String TAG = "GmFloatingUIManager";

    private Context mContext;
    private GmNavBarUtils mNavBarUtils;
    private GmFloatingButtonView mButtonView;
    private GmFloatingSettingView mSettingView;
    private GmFloatingViewHelper mViewHelper;

    private ArraySet<IGmFloatingUIConfig> mListeners;

    public enum ViewType {
        VIEW_TYPE_FLOAT_BUTTON,
        VIEW_TYPE_SETTING_PANEL,
    }

    public GmFloatingUIManager(Context context) {
        mContext = context;

        mListeners = new ArraySet<>();

        WindowManager wm = getWindowManager();
        mViewHelper = new GmFloatingViewHelper(wm);
        mNavBarUtils = new GmNavBarUtils(mContext, wm);
        mNavBarUtils.start();
    }

    Context getContext() {
        return mContext;
    }

    WindowManager getWindowManager() {
        return (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    GmNavBarUtils getNavBarUtils() {
        return mNavBarUtils;
    }

    GmFloatingViewHelper getViewHelper() {
        return mViewHelper;
    }

    public void onDestroy() {
        mViewHelper.removeAllView();
        mNavBarUtils.destroy();
        for (IGmFloatingUIConfig l : mListeners) {
            unregisterGmFloatingUIAdapter(l);
        }
    }

    public void showFloatingView(ViewType viewType) {
        AbsGmFloatingView absView;
        switch (viewType) {
            case VIEW_TYPE_FLOAT_BUTTON:
                if (mButtonView == null) {
                    mButtonView = new GmFloatingButtonView(this);
                }
                absView = mButtonView;
                break;
            case VIEW_TYPE_SETTING_PANEL:
                if (mSettingView == null) {
                    mSettingView = new GmFloatingSettingView(this);
                }
                absView = mSettingView;
                break;
            default:
                GmLogUtils.loge(TAG, "showFloatingView. viewType is invalid");
                return;
        }
        absView.show();
    }

    public void setFloatingViewVisible(ViewType viewType) {
        switch (viewType) {
            case VIEW_TYPE_FLOAT_BUTTON:
                if (mButtonView != null) {
                    mButtonView.setFloatingViewVisible();
                }
                break;
            default:
                GmLogUtils.loge(TAG, "setFloatingViewVisible. viewType is invalid");
                break;
        }
    }

    void registerGmFloatingUIAdapter(IGmFloatingUIConfig adapter) {
        if (adapter != null) {
            mListeners.add(adapter);
            adapter.start();
        }
    }

    void unregisterGmFloatingUIAdapter(IGmFloatingUIConfig adapter) {
        if (adapter != null && mListeners.contains(adapter)) {
            adapter.stop();
            mListeners.remove(adapter);
        }
    }
}
