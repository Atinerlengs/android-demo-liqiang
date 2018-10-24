package application.android.com.zhaozehong.action;

import android.app.Activity;

import application.android.com.zhaozehong.utils.CityDataExcelFileLoader;

public class ParseXlsAction extends Action {

    public ParseXlsAction(Activity activity) {
        super(activity);
    }

    @Override
    public String getName() {
        return "Parse excel file";
    }

    @Override
    public void doAction() {
        new CityDataExcelFileLoader(mActivity, null).execute();
    }
}
