package application.android.com.zhaozehong.utils;

import android.content.Context;

import jxl.Sheet;

public class LocationInfoExcelFileLoader extends ExcelFileLoader {

    public LocationInfoExcelFileLoader(Context context, CallBack callBack) {
        super(context, "insert_location.xls", 0, callBack);
    }

    @Override
    public XlsData getXlsData(Sheet sheet, int row) {
        LocationInfoData data = new LocationInfoData();
        if (sheet != null && sheet.getColumns() >= 2 && row > 0) {
            data.setEmpty(false);
            data.setNumber(sheet.getCell(0, row).getContents());
            data.setCityName(sheet.getCell(1, row).getContents());
        }
        return data;
    }
}
