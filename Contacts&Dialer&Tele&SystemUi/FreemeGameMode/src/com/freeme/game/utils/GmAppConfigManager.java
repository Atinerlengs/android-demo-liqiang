package com.freeme.game.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

import com.freeme.game.R;
import com.freeme.game.apppicker.loader.GmAppModel;

public class GmAppConfigManager {

    private Context mContext;

    private List<String> mExceptAppList = new ArrayList<>();
    private List<String> mRecommendAppList = new ArrayList<>();

    public GmAppConfigManager(Context context) {
        mContext = context;
        initExceptAppList();
        initRecommendAppList();
    }

    private void initExceptAppList() {
        String[] gmExceptApps = mContext.getResources()
                .getStringArray(R.array.gm_except_app_list);
        if (gmExceptApps.length > 0) {
            mExceptAppList.clear();
            mExceptAppList.addAll(Arrays.asList(gmExceptApps));
        }
    }

    private void initRecommendAppList() {
        String[] gmRecommendApps = mContext.getResources()
                .getStringArray(R.array.gm_recommend_app_list);
        if (gmRecommendApps.length > 0) {
            mRecommendAppList.clear();
            mRecommendAppList.addAll(Arrays.asList(gmRecommendApps));
        }
    }

    public boolean packageExcludeFilter(String pkgName) {
        return mExceptAppList.contains(pkgName);
    }

    public List<GmAppModel> resortGmAppListByRecommend(List<GmAppModel> list) {
        if (list == null || list.isEmpty() || mRecommendAppList.size() <= 0) {
            return list;
        }
        List<GmAppModel> recommendList = new ArrayList<>();
        for (GmAppModel data : list) {
            String pkg = data.getPkgName();
            for (String name : mRecommendAppList) {
                if (pkg.startsWith(name)) {
                    recommendList.add(data);
                    break;
                }
            }
        }
        if (recommendList.isEmpty()) {
            return list;
        }
        for (GmAppModel data : recommendList) {
            list.remove(data);
        }
        if (!list.isEmpty()) {
            list.get(0).setTitle(mContext.getString(R.string.gm_add_apps_list_other));
        }
        if (!recommendList.isEmpty()) {
            recommendList.get(0).setTitle(mContext.getString(R.string.gm_add_apps_list_recommended));
        }
        list.addAll(0, recommendList);
        return list;
    }
}
