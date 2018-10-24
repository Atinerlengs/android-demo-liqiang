package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import jxl.Sheet;
import jxl.Workbook;

public class CityDataExcelFileLoader extends ExcelFileLoader {

    public CityDataExcelFileLoader(Context context, CallBack callBack) {
        super(context, "city.xls", 0, callBack);
    }

    @Override
    public XlsData getXlsData(Sheet sheet, int row) {
        CityData data = new CityData();
        int count = sheet.getColumns();
        if (count >= 3) {
            data.setEmpty(false);
            data.setCityId(sheet.getCell(0, row).getContents());
            data.setCityName(sheet.getCell(1, row).getContents());
            data.setCityNameEn(sheet.getCell(2, row).getContents());
        } else if (count >= 2) {
            data.setEmpty(false);
            data.setCityName(sheet.getCell(0, row).getContents());
            data.setCityNameEn(sheet.getCell(1, row).getContents());
        }
        return data;
    }
}
