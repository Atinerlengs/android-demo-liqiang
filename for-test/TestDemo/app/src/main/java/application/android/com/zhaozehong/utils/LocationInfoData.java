package application.android.com.zhaozehong.utils;

public class LocationInfoData extends XlsData {

    String cityId;
    String cityName;
    String number;

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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "number: " + number + ", cityName: " + cityName + ", cityId: " + cityId;
    }
}
