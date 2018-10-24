package com.freeme.systemui.utils;

public class WeatherData {

    private String mCityId;
    private String mCityName;
    private String mCurTemper;
    private String mAqiValue;
    private String mAqiLevel;
    private String mHighTemper;
    private String mLowTemper;
    private String mWeatherType;

    public WeatherData(String cityId, String cityName, String curTemper, String aqiValue,
                       String aqiLevel, String highTemper, String lowTemper, String weatherType) {
        this.mCityId = cityId;
        this.mCityName = cityName;
        this.mCurTemper = curTemper;
        this.mAqiValue = aqiValue;
        this.mAqiLevel = aqiLevel;
        this.mHighTemper = highTemper;
        this.mLowTemper = lowTemper;
        this.mWeatherType = weatherType;
    }

    public void setCityId(String cityId) {
        this.mCityId = cityId;
    }

    public void setCityName(String cityName) {
        this.mCityName = cityName;
    }

    public void setCurTemper(String curTemper) {
        this.mCurTemper = curTemper;
    }

    public void setAqiValue(String aqiValue) {
        this.mAqiValue = aqiValue;
    }

    public void setAqiLevel(String aqiLevel) {
        this.mAqiLevel = aqiLevel;
    }

    public void setHighTemper(String highTemper) {
        this.mHighTemper = highTemper;
    }

    public String getCityId() {
        return mCityId;
    }

    public String getCityName() {
        return mCityName;
    }

    public String getCurTemper() {
        return mCurTemper;
    }

    public String getAqiValue() {
        return mAqiValue;
    }

    public String getAqiLevel() {
        return mAqiLevel;
    }

    public String getHighTemper() {
        return mHighTemper;
    }

    public String getLowTemper() {
        return mLowTemper;
    }

    public String getWeatherType() {
        return mWeatherType;
    }

    public void setLowTemper(String lowTemper) {
        this.mLowTemper = lowTemper;
    }

    public void setWeatherType(String weatherType) {
        this.mWeatherType = weatherType;
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "mCityId='" + mCityId + '\'' +
                ", mCityName='" + mCityName + '\'' +
                ", mCurTemper='" + mCurTemper + '\'' +
                ", mAqiValue='" + mAqiValue + '\'' +
                ", mAqiLevel='" + mAqiLevel + '\'' +
                ", mHighTemper='" + mHighTemper + '\'' +
                ", mLowTemper='" + mLowTemper + '\'' +
                ", mWeatherType='" + mWeatherType + '\'' +
                '}';
    }

}
