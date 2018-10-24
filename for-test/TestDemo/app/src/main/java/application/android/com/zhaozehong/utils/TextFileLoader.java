package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class TextFileLoader extends FileLoader {

    private CallBack mCallBack;

    public TextFileLoader(Context context, String fileName, CallBack callBack) {
        super(context, fileName);
        mCallBack = callBack;
    }

    @Override
    public final ArrayList<XlsData> parseFileData(String fileName) {
        ArrayList<XlsData> list = new ArrayList<>();
        try {
            InputStream is = mContext.getAssets().open(fileName);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String lineTxt = null;
            while ((lineTxt = br.readLine()) != null) {
                XlsData data = getXlsData(lineTxt);
                if (!data.isEmpty()) {
                    list.add(data);
                    Log.d(TAG, "text file raw data: " + data.toString());
                }
            }
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "read error=" + e, e);
        }
        return list;
    }

    public abstract XlsData getXlsData(String lineTxt);

    @Override
    public void onPostRet(ArrayList<XlsData> list) {
        if (mCallBack != null) {
            mCallBack.onRet(list);
        }
    }
}
