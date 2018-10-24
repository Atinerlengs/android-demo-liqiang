package com.freeme.systemui.utils;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;

public class WeatherUtils {

    public interface WeatherCallBack {
        void updateWeatherInfo(WeatherData weather);
    }

    public static final String TEMPURTURE_POSTFIX = "\u2103";
    public static final String NO_WEATHER = "NA";

    private static final String CONTENT = "content://com.icoolme.android.weather.ExternalProvider/LocCityWeather";
    private static final  Uri WEATHER_CONTENT_URI = Uri.parse(CONTENT);
    private static final int QUERY_TOKEN = 0;
    private static final String TAG = "WeatherUtils";

    private static WeatherUtils sInstance;

    private String mCityId, mCityName, mWeatherType, mAqiValue, mAqiLevel, mCurTemper, mHighTemper, mLowTemper;
    private String[] mWeatherStyle;

    private ContentResolver mResolver;

    private WeatherData mWeather;
    private QueryHandler mQueryHandler;
    private List<WeatherCallBack> mWeatherCallBacks;

    private WeatherUtils(Context context) {
        mWeatherStyle = context.getResources().getStringArray(R.array.weather_style);
        mResolver = context.getContentResolver();
        mQueryHandler = new QueryHandler(mResolver);
        mWeatherCallBacks = new ArrayList<>();
        try {
            mResolver.registerContentObserver(WEATHER_CONTENT_URI, true, new WeatherObserver(mQueryHandler));
        } catch (SecurityException exception) {
            Log.d(TAG,"WeatherUtils error state no zuimei app");
        }
    }

    public static WeatherUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WeatherUtils(context);
        }
        return sInstance;
    }

    public void registerView(WeatherCallBack weatherCallback) {
        if (mWeatherCallBacks != null) {
            mWeatherCallBacks.add(weatherCallback);
        }
    }

    public void updateWeather(Cursor cursor) {
        if (cursor == null) {
            mWeather = null;
            updateWeatehrViews(mWeather);
            return;
        }

        while (cursor.moveToNext()) {
            mCityId = cursor.getString(cursor.getColumnIndex("city_id"));
            mCityName = cursor.getString(cursor.getColumnIndex("city_name"));
            mCurTemper = cursor.getString(cursor.getColumnIndex("current_temper"));
            mAqiValue = cursor.getString(cursor.getColumnIndex("aqi_value"));
            mAqiLevel = cursor.getString(cursor.getColumnIndex("aqi_level"));
            mHighTemper = cursor.getString(cursor.getColumnIndex("high_temper"));
            mLowTemper = cursor.getString(cursor.getColumnIndex("low_temper"));
            mWeatherType = cursor.getString(cursor.getColumnIndex("weather_type"));
        }
        mWeather = new WeatherData(mCityId,
                mCityName,
                mCurTemper,
                mAqiValue,
                mAqiLevel,
                mHighTemper,
                mLowTemper,
                mWeatherType);
        cursor.close();

        updateWeatehrViews(mWeather);
    }

    public String getWeatherType(int type) {
        if (type >= mWeatherStyle.length) {
            return NO_WEATHER;
        }
        return mWeatherStyle[type];
    }

    private class WeatherObserver extends ContentObserver {

        public WeatherObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            query();
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
                updateWeather(cursor);
        }
    }

    private void updateWeatehrViews(WeatherData weatherData) {
        for (WeatherCallBack callback : mWeatherCallBacks) {
            callback.updateWeatherInfo(weatherData);
        }
    }

    public void query() {
        mQueryHandler.startQuery(QUERY_TOKEN, null,
                WEATHER_CONTENT_URI, null, null, null, null);
    }

    @Override
    public String toString() {
        return mWeather != null ? mWeather.toString() : null;
    }
}
