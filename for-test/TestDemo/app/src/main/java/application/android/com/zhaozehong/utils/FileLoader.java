package application.android.com.zhaozehong.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

public abstract class FileLoader extends AsyncTask<String, Void, ArrayList<XlsData>> {

    public static final String TAG = "FileLoader";

    public Context mContext;
    private String mFileName;

    public FileLoader(Context context, String fileName) {
        mContext = context;
        mFileName = fileName;
    }

    @Override
    protected ArrayList<XlsData> doInBackground(String... strings) {
        return parseFileData(mFileName);
    }

    public abstract ArrayList<XlsData> parseFileData(String fileName);

    public abstract void onPostRet(ArrayList<XlsData> list);

    @Override
    protected void onPostExecute(ArrayList<XlsData> list) {
        Log.d(TAG, "FileLoader.onPostExecute....");
        Log.d(TAG, "list.size: " + list.size());
        if (list != null && list.size() > 0) {
            Log.d(TAG, list.toString());
        }
        onPostRet(list);
    }

    public interface CallBack {
        void onRet(ArrayList<XlsData> list);
    }
}
