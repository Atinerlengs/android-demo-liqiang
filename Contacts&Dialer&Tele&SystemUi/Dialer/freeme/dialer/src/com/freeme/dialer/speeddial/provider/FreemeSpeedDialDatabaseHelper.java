package com.freeme.dialer.speeddial.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class FreemeSpeedDialDatabaseHelper extends SQLiteOpenHelper {

    private final static String TAG = "FreemeSpeedDialDatabaseHelper";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "speeddial.db";
    private static FreemeSpeedDialDatabaseHelper mSpeedDialDatabaseHelper;

    public interface Tables {
        static final String SPEEDDIAL = "speeddials";
    }

    public static synchronized FreemeSpeedDialDatabaseHelper getInstance(Context context) {
        if (mSpeedDialDatabaseHelper == null) {
            mSpeedDialDatabaseHelper = new FreemeSpeedDialDatabaseHelper(context, DATABASE_NAME, true);
        }
        return mSpeedDialDatabaseHelper;
    }

    protected FreemeSpeedDialDatabaseHelper(Context context, String databaseName,
                                            boolean optimizationEnabled) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.SPEEDDIAL + " (" +
                FreemeSpeedDial.Numbers._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                FreemeSpeedDial.Numbers.NUMBER + " TEXT" +
                ");");
        initSpeedDialTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private void initSpeedDialTable(SQLiteDatabase db) {
        for (int i = 0; i < 10; i++) {
            db.execSQL("INSERT INTO " + Tables.SPEEDDIAL + " (" +
                    FreemeSpeedDial.Numbers.NUMBER + ") " +
                    "VALUES('" + "" + "'" +
                    ");");
        }
    }
}

