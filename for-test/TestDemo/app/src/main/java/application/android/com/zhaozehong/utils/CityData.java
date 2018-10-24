package application.android.com.zhaozehong.utils;

public class CityData extends XlsData {
    String cityId;
    String cityName;
    String cityNameEn;

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCityNameEn() {
        return cityNameEn;
    }

    public void setCityNameEn(String cityNameEn) {
        this.cityNameEn = cityNameEn;
    }

    @Override
    public String toString() {
        return "cityId: " + cityId + ", cityName: " + cityName + ", cityNameEn: " + cityNameEn;
    }
}
