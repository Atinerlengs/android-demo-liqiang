package application.android.com.zhaozehong.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import application.android.com.zhaozehong.demoapplication.R;
import application.android.com.zhaozehong.utils.CityData;
import application.android.com.zhaozehong.utils.CityDataExcelFileLoader;
import application.android.com.zhaozehong.utils.FileLoader;
import application.android.com.zhaozehong.utils.LocationInfoData;
import application.android.com.zhaozehong.utils.LocationInfoExcelFileLoader;
import application.android.com.zhaozehong.utils.LocationInfoTextFileLoader;
import application.android.com.zhaozehong.utils.PrefixInfoData;
import application.android.com.zhaozehong.utils.PrefixInfoDataExcelFileLoader;
import application.android.com.zhaozehong.utils.PublicInfoData;
import application.android.com.zhaozehong.utils.XlsData;

public class ModifyGeoCodingActivity extends Activity {

    private static final String TAG = "ModifyGeoCodingActivity";
    private static final String NAME = "geocoding.db";

    private SQLiteOpenHelper mDBHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_geocoding);

        if (resolveDatabaseFile(this)) {
            mDBHelper = new DatabaseHelper(this);
        }
    }

    //============================
    public void onReInsertCityData(View view) {
        new CityDataExcelFileLoader(this, new FileLoader.CallBack() {
            @Override
            public void onRet(ArrayList<XlsData> list) {
                if (list != null) {
                    ContentValues cv = new ContentValues();
                    SQLiteDatabase db = mDBHelper.getWritableDatabase();
                    db.beginTransaction();
                    db.delete("NumberCity", null, null);
                    db.delete("city", null, null);

                    int id = 1;
                    for (XlsData data : list) {
                        cv.clear();
                        CityData cd = (CityData) data;
                        cv.put("_id", id);
                        cv.put("city_name", cd.getCityName());
                        cv.put("city_name_en", cd.getCityNameEn());
                        db.insert("city", null, cv);

                        id++;
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    Log.i(TAG, "onUpdateCityNameFromXls: update end .....");
                }
            }
        }).execute();
    }
    //============================

    //======通过文件插入/更新号码归属地信息=======
    public void onInsertLocationFromXls(View view) {
        new LocationInfoExcelFileLoader(this, new FileLoader.CallBack() {
            @Override
            public void onRet(ArrayList<XlsData> list) {
                new InsertTask().execute(list);
            }
        }).execute();
    }

    public void onInsertLocationFromTxt(View view) {
        new LocationInfoTextFileLoader(ModifyGeoCodingActivity.this,
                new FileLoader.CallBack() {
                    @Override
                    public void onRet(ArrayList<XlsData> list) {
                        Log.d(TAG, "text file onRet: " + list.size());
                        new InsertTask().execute(list);
                    }
                }).execute();
    }

    class InsertTask extends AsyncTask<ArrayList<XlsData>, Void, Void> {

        @Override
        protected Void doInBackground(ArrayList<XlsData>... lists) {

            if (lists != null && lists.length > 0) {
                ArrayList<XlsData> list = lists[0];
                SQLiteDatabase db = mDBHelper.getWritableDatabase();

                if (list != null && !list.isEmpty()) {
                    ContentValues cv = new ContentValues();
                    db.beginTransaction();

                    for (XlsData data : list) {
                        cv.clear();
                        LocationInfoData locData = (LocationInfoData) data;

                        String loc = locData.getCityName();
                        String id = locData.getCityId();
                        if (TextUtils.isEmpty(id)) {
                            if (TextUtils.isEmpty(loc)) {
                                continue;
                            }
                            id = onQueryCityID(db, loc);
                        }

                        if (UNKNOWN_CITY.equals(id)) {
                            Cursor cursor = db.rawQuery("select count(*) from city", null);
                            if (cursor != null && cursor.getCount() > 0) {
                                cursor.moveToFirst();
                                long count = cursor.getLong(0);
                                id = String.valueOf(count + 1);
                                Log.i(TAG, "onInsertLocation: insert new city, CityID: " + id + " , CityName: " + loc);
                                cv.put("_id", id);
                                cv.put("city_name", loc);
                                db.insert("city", null, cv);
                                cursor.close();
                            }
                        }

                        if (UNKNOWN_CITY.equals(id)) {
                            Log.i(TAG, "onInsertLocation: has no city");
                            continue;
                        }

                        String number = locData.getNumber();
                        cv.put("CityID", id);
                        cv.put("NumberHead", number);
                        Log.i(TAG, "onInsertLocation: CityID: " + id + " , NumberHead: " + number);
                        String id_old = onCheckNumber(db, number);
                        if (UNKNOWN_CITY.equals(id_old)) {
                            db.insert("NumberCity", null, cv);
                        } else {
                            db.update("NumberCity", cv, "NumberHead = ?",
                                    new String[]{number});
                        }
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    Log.i(TAG, "onInsertLocation: insert end .....");
                }
            }
            return null;
        }
    }
    //=======================================

    public void onUpdateCityNameFromXls(View view) {
        new CityDataExcelFileLoader(this, new FileLoader.CallBack() {
            @Override
            public void onRet(ArrayList<XlsData> list) {
                if (list != null) {
                    ContentValues cv = new ContentValues();
                    SQLiteDatabase db = mDBHelper.getWritableDatabase();
                    db.beginTransaction();

                    for (XlsData data : list) {
                        cv.clear();
                        CityData cd = (CityData) data;
                        cv.put("_id", cd.getCityId());
                        cv.put("city_name", cd.getCityName());
                        cv.put("city_name_en", cd.getCityNameEn());
                        db.update("city", cv, "_id = " + cd.getCityId(), null);
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    Log.i(TAG, "onUpdateCityNameFromXls: update end .....");
                }
            }
        }).execute();
    }

    public void onUpdatePrefixInfo(View view) {
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();

        Cursor cursor = db.query("prefix_info", new String[]{"prefix", "info"},
                null, null,
                null, null, null);
        if (cursor != null) {
            ArrayList<PrefixInfoData> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                String prefix = cursor.getString(0);
                String info = cursor.getString(1);
                if (info != null) {
                    info = info.trim();
                }
                PrefixInfoData data = new PrefixInfoData();
                data.setPrefix(prefix);
                data.setInfo(info);
                list.add(data);
            }
            cursor.close();

            ContentValues cv = new ContentValues();
            for (PrefixInfoData data : list) {
                cv.clear();
                cv.put("info", data.getInfo());

                db.update("prefix_info", cv, "prefix = ?",
                        new String[]{data.getPrefix()});
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        Log.i(TAG, "onUpdatePrefixInfo: update end .....");
    }

    public void onUpdatePrefixInfoFromXls(View view) {
        new PrefixInfoDataExcelFileLoader(this, new FileLoader.CallBack() {
            @Override
            public void onRet(ArrayList<XlsData> list) {
                if (list != null) {
                    ContentValues cv = new ContentValues();
                    SQLiteDatabase db = mDBHelper.getWritableDatabase();
                    db.beginTransaction();

                    for (XlsData data : list) {
                        cv.clear();
                        PrefixInfoData cd = (PrefixInfoData) data;
                        cv.put("info_en", cd.getInfo_en());
                        db.update("prefix_info", cv, "prefix = ?",
                                new String[]{cd.getPrefix()});
                    }

                    db.setTransactionSuccessful();
                    db.endTransaction();

                    Log.i(TAG, "onUpdateCityNameFromXls: update end .....");
                }
            }
        }).execute();
    }

    private static final String UNKNOWN_CITY = "unknown";

    private String onQueryCityID(SQLiteDatabase db, String cityName) {
        String cityId = UNKNOWN_CITY;
        Cursor cursor = db.rawQuery("select _id from city where city_name like ? or city_name_en like ?",
                new String[]{'%' + cityName + '%', '%' + cityName + '%'});
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                cityId = cursor.getString(0);
            }
            cursor.close();
        }

        return cityId;
    }

    private String onCheckNumber(SQLiteDatabase db, String number) {
        String cityId = UNKNOWN_CITY;
        Cursor cursor = db.query("NumberCity", new String[]{"CityId"},
                "NumberHead = ?",
                new String[]{number}, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                cityId = cursor.getString(0);
            }
            cursor.close();
        }
        return cityId;
    }

    public void onQueryCityID(View view) {
        EditText city_name_query = findViewById(R.id.city_name_query);
        TextView city_id_query = findViewById(R.id.city_id_query);

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        String cityName = city_name_query.getText().toString();
        city_id_query.setText(onQueryCityID(db, cityName));
    }

    public void onUpdateLocation(View view) {
        EditText city_id_add = findViewById(R.id.city_id_add);
        EditText phone_number = findViewById(R.id.phone_number);

        String id = city_id_add.getText().toString();
        String num = phone_number.getText().toString();

        if (num.length() != 7) {
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("cityid", id);
        cv.put("numberhead", num);

        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        Cursor cursor = db.query("numbercity", null, "numberhead = ?",
                new String[]{num}, null,
                null, null);
        if (cursor != null && cursor.getCount() > 0) {
            db.update("numbercity", cv, "numberhead = ?",
                    new String[]{num});
        } else {
            db.insert("numbercity", null, cv);
        }

        if (cursor != null) {
            cursor.close();
        }

        Log.i(TAG, "onUpdateLocation: update end .....");
    }

    public void onUpdatePublicInfo(View view) {
        ArrayList<PublicInfoData> list = new ArrayList<>();
        list.add(new PublicInfoData("95501", "深发银行"));

        ContentValues cv = new ContentValues();
        SQLiteDatabase db = mDBHelper.getWritableDatabase();
        db.beginTransaction();
        for (PublicInfoData data : list) {
            cv.clear();
            cv.put("phone_num", data.getNumber());
            cv.put("phone_name", data.getName());
            cv.put("flag", "y");
            cv.put("full_pin", data.getFullpyDigit());
            cv.put("jian_pin", data.getJianpyDigit());
            db.insert("phone_logo", null, cv);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        Log.e("zhaozehong", "[ModifyGeoCodingActivity][onUpdatePublicInfo] end ...");
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        private static final String NAME = "geocoding.db";
        private static final int VERSION = 3;

        DatabaseHelper(Context context) {
            super(context, NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // do nothing.
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.v(TAG, "onUpgarde :: oldVersion:" + oldVersion
                    + " newVersion:" + newVersion);
        }
    }

    private boolean resolveDatabaseFile(Context context) {
        final File dbfile = context.getDatabasePath(NAME);
        if (!dbfile.exists()) {
            try {
                writeAssetFileToDisk(context, NAME, dbfile);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    private static void writeAssetFileToDisk(Context context, String assetName, File diskPath)
            throws IOException {
        try (final InputStream input = context.getAssets().open(assetName)) {
            try (final OutputStream output = new FileOutputStream(diskPath)) {
                final byte[] BUF = new byte[1024 * 32];
                int len;
                while ((len = input.read(BUF)) != -1) {
                    output.write(BUF, 0, len);
                }
            }
        }
    }
}
