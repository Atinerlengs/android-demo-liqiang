package com.freeme.game.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.freeme.game.R;
import com.freeme.game.database.GmDatabaseConstant.Columns;
import com.freeme.game.database.GmDatabaseConstant.Tables;

public class GmDatabaseHelper extends SQLiteOpenHelper {

    private static final int VERSION = 1;
    private static final String NAME = "game_app.db";

    private Context mContext;

    public GmDatabaseHelper(Context context) {
        super(context, NAME, null, VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createGameAppTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion < 2) {
//            insertPresetData(db);
//        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void createGameAppTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.TAB_GAME_APPS + "("
                + Columns._ID + " INTEGER PRIMARY KEY, "
                + Columns.COLUMN_APP_PACKAGE_NAME + " VARCHAR NOT NULL, "
                + Columns.COLUMN_APP_SELECTED + " INTEGER DEFAULT 1, "
                + Columns.COLUMN_APP_PRESET_FLAG + " INTEGER  DEFAULT 0"
                + ");");

        insertPresetData(db);
    }

    private void insertPresetData(SQLiteDatabase db) {
        String[] arr = mContext.getResources().getStringArray(R.array.gm_recommend_app_list);
        if (arr.length > 0) {
            List<String> appList = new ArrayList<>(Arrays.asList(arr));

            db.beginTransaction();
            StringBuffer buffer = new StringBuffer();
            buffer.append("'");
            buffer.append(TextUtils.join("', '", arr));
            buffer.append("'");
            // buffer = 'com.xxx.xxx', 'com.xxx.xxx', 'com.xxx.xxx'

            Cursor cursor = db.query(Tables.TAB_GAME_APPS,
                    new String[]{Columns.COLUMN_APP_PACKAGE_NAME},
                    Columns.COLUMN_APP_PACKAGE_NAME + " IN (" + buffer.toString() + ")",
                    null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    appList.remove(cursor.getString(0));
                }
                cursor.close();
            }

            if (!appList.isEmpty()) {
                ContentValues values = new ContentValues();
                for (String pkgName : appList) {
                    values.clear();
                    values.put(Columns.COLUMN_APP_PACKAGE_NAME, pkgName);
                    values.put(Columns.COLUMN_APP_PRESET_FLAG, 1);
                    db.insert(Tables.TAB_GAME_APPS, Columns._ID, values);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }
}
