package application.android.com.zhaozehong.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import application.android.com.zhaozehong.database.DatabaseConstant.Columns;
import application.android.com.zhaozehong.database.DatabaseConstant.Tables;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, "game_app.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.TAB_GAME_APPS + "("
                + Columns._ID + " INTEGER PRIMARY KEY, "
                + Columns.COLUMN_APP_PACKAGE_NAME + " VARCHAR NOT NULL, "
                + Columns.COLUMN_APP_SELECTED + " INTEGER DEFAULT 1"
                + ");");

        insertPresetData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            insertPresetData(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void insertPresetData(SQLiteDatabase db) {
        String[] arr = new String[]{"acacsac", "acc", "cascasc", "aaaaaaaa"};
        if (arr.length > 0) {
            List<String> appList = new ArrayList<>(Arrays.asList(arr));
            appList.remove("acacsac");

            db.beginTransaction();
            StringBuffer buffer = new StringBuffer("'");
            buffer.append(TextUtils.join("', '", arr));
            buffer.append("'");
            // buffer = 'com.xxx.xxx', 'com.xxx.xxx', 'com.xxx.xxx'

            Cursor cursor = db.query(Tables.TAB_GAME_APPS,
                    new String[]{Columns.COLUMN_APP_PACKAGE_NAME},
                    Columns.COLUMN_APP_PACKAGE_NAME + " IN (" + buffer.toString() + ")",
                    null, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String pkg = cursor.getString(0);
                    Log.e("zhaozehong", "[DatabaseHelper][insertPresetData] "+pkg);
                    appList.remove(pkg);
                }
                cursor.close();
            }

            if (!appList.isEmpty()) {
                ContentValues values = new ContentValues();
                for (String pkgName : appList) {
                    values.clear();
                    values.put(Columns.COLUMN_APP_PACKAGE_NAME, pkgName);
                    db.insert(Tables.TAB_GAME_APPS, Columns._ID, values);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }
}
