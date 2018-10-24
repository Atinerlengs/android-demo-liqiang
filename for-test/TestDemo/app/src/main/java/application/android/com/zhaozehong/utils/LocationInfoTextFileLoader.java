package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

public class LocationInfoTextFileLoader extends TextFileLoader {

    private ArrayList<String> tempNumList = new ArrayList<>();
    private ArrayList<String> repeatNumList = new ArrayList<>();

    public LocationInfoTextFileLoader(Context context, CallBack callBack) {
        super(context, "loc.txt", callBack);
    }

    @Override
    public XlsData getXlsData(String lineTxt) {
        LocationInfoData data = new LocationInfoData();
        if (lineTxt != null) {
            String[] values = lineTxt.split(",");
            if (values.length >= 2) {
                data.setEmpty(false);
                data.setNumber(values[0]);
                data.setCityName(values[1]);

                String num = data.getNumber();
                if (tempNumList.contains(num)) {
                    repeatNumList.add(num);
                } else {
                    tempNumList.add(data.getNumber());
                }
            }
        }
        return data;
    }

    @Override
    public void onPostRet(ArrayList<XlsData> list) {
        super.onPostRet(list);
        Log.e(TAG, "onPostRet: repeat Num List -> " + repeatNumList.toString());
    }
}
