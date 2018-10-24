package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import jxl.Sheet;
import jxl.Workbook;

public class PrefixInfoDataExcelFileLoader extends ExcelFileLoader {

    public PrefixInfoDataExcelFileLoader(Context context, CallBack callBack) {
        super(context, "prefix_info.xls", 0, callBack);
    }

    @Override
    public XlsData getXlsData(Sheet sheet, int row) {
        PrefixInfoData data = new PrefixInfoData();
        if (sheet.getColumns() >= 3) {
            data.setEmpty(false);
            data.setPrefix(sheet.getCell(0, row).getContents());
            data.setInfo(sheet.getCell(1, row).getContents());
            data.setInfo_en(sheet.getCell(2, row).getContents());
        }
        return data;
    }
}
