package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import jxl.Sheet;
import jxl.Workbook;

public abstract class ExcelFileLoader extends FileLoader {

    private CallBack mCallBack;
    private int mSheetIndex;

    public ExcelFileLoader(Context context, String xlsName) {
        this(context, xlsName, 0, null);
    }

    public ExcelFileLoader(Context context, String xlsName, int sheetIndex, CallBack callBack) {
        super(context, xlsName);
        mSheetIndex = sheetIndex;
        mCallBack = callBack;
    }

    @Override
    public final ArrayList<XlsData> parseFileData(String fileName) {

        ArrayList<XlsData> cityList = new ArrayList<>();

        AssetManager assetManager = mContext.getAssets();

        try {
            Workbook workbook = Workbook.getWorkbook(assetManager.open(fileName));

            cityList.addAll(parseData(workbook));

            workbook.close();

        } catch (Exception e) {
            Log.e(TAG, "read error=" + e, e);
        }

        return cityList;
    }

    private ArrayList<XlsData> parseData(Workbook workbook) {

        ArrayList<XlsData> cityList = new ArrayList<>();

        Sheet sheet = workbook.getSheet(mSheetIndex);
        String currentSheetName = sheet.getName();
        int sheetRows = sheet.getRows();
        int sheetColumns = sheet.getColumns();

        Log.d(TAG, "sheet name: " + currentSheetName
                + " rows: " + sheetRows
                + " cols: " + sheetColumns);

        for (int i = 0; i < sheetRows; i++) {
            XlsData data = getXlsData(sheet, i);
            cityList.add(data);
            Log.d(TAG, "sheet raw data: " + data.toString());
        }

        return cityList;
    }

    public abstract XlsData getXlsData(Sheet sheet, int row);

    @Override
    public void onPostRet(ArrayList<XlsData> list) {
        if (mCallBack != null) {
            mCallBack.onRet(list);
        }
    }
}
