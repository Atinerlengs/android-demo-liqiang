package application.android.com.zhaozehong.utils;

import android.util.Log;

public class PublicInfoData {

    String number;
    String name;
    String pinyin;
    String fullpyDigit;
    String jianpyDigit;

    public PublicInfoData(String number, String name) {
        setNumber(number);
        setName(name);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;

        String[] arr = HanziToPinyinUtils.onHanyu2Pinyin(name);
        this.pinyin = arr[0];
        this.fullpyDigit = arr[1];
        this.jianpyDigit = arr[2];

        Log.e("PublicInfoData", "name: " + name);
        Log.e("PublicInfoData", "fullpyDigit: " + fullpyDigit);
        Log.e("PublicInfoData", "jianpyDigit: " + jianpyDigit);
    }

    public String getPinyin() {
        return pinyin;
    }

    public String getFullpyDigit() {
        return fullpyDigit;
    }

    public String getJianpyDigit() {
        return jianpyDigit;
    }
}
