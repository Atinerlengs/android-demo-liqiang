package com.freeme.game.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.freeme.game.database.GmDatabaseConstant.Columns;
import com.freeme.game.database.GmDatabaseConstant.Tables;

public class GmProvider extends ContentProvider {

    private static final int GAME_APP = 1000;

    private GmDatabaseHelper mHelper;

    private static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mUriMatcher.addURI(GmDatabaseConstant.AUTHORITY, "game_app", GAME_APP);
    }

    @Override
    public boolean onCreate() {
        mHelper = new GmDatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case GAME_APP:
                return db.query(Tables.TAB_GAME_APPS, projection, selection, selectionArgs,
                        null, null, sortOrder);
            default:
                break;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        long id = 0;
        switch (match) {
            case GAME_APP:
                id = db.insert(Tables.TAB_GAME_APPS, Columns._ID, values);
                break;
            default:
                break;
        }

        if (id <= 0) {
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case GAME_APP:
                count = db.delete(Tables.TAB_GAME_APPS, selection, selectionArgs);
                break;
            default:
                break;
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = 0;
        SQLiteDatabase db = mHelper.getWritableDatabase();
        final int match = mUriMatcher.match(uri);
        switch (match) {
            case GAME_APP:
                count = db.update(Tables.TAB_GAME_APPS, values, selection, selectionArgs);
                break;
            default:
                break;
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        switch (method) {
            case GmDatabaseConstant.Methods.METHOD_UPDATE: {
                if (extras != null) {
                    String pkg = extras.getString(Columns.COLUMN_APP_PACKAGE_NAME);
                    int selected = extras.getInt(Columns.COLUMN_APP_SELECTED);
                    long ret;
                    if (selected == 1) {
                        ret = addGameAppToDB(pkg);
                    } else {
                        ret = removeGameAppFromDB(pkg);
                    }
                    if (ret > 0) {
                        getContext().getContentResolver().notifyChange(
                                GmDatabaseConstant.CONTENT_URI, null);
                    }
                }
                break;
            }
        }
        return extras;
    }

    private long addGameAppToDB(String pkgName) {
        long ret;

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String selection = Columns.COLUMN_APP_PACKAGE_NAME + " = ?";
        String[] selectionArgs = new String[]{pkgName};

        db.beginTransaction();

        Cursor cursor = db.query(Tables.TAB_GAME_APPS,
                new String[]{Columns.COLUMN_APP_PACKAGE_NAME},
                selection, selectionArgs,
                null, null, null);
        boolean has = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }

        ContentValues values = new ContentValues();
        values.put(Columns.COLUMN_APP_SELECTED, 1);
        values.put(Columns.COLUMN_APP_PACKAGE_NAME, pkgName);
        if (has) {
            ret = db.update(Tables.TAB_GAME_APPS, values, selection, selectionArgs);
        } else {
            ret = db.insert(Tables.TAB_GAME_APPS, Columns._ID, values);
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        return ret;
    }

    private long removeGameAppFromDB(String pkgName) {
        long ret;

        SQLiteDatabase db = mHelper.getWritableDatabase();
        String whereClause = Columns.COLUMN_APP_PACKAGE_NAME + " = ?";
        String[] whereArgs = new String[]{pkgName};

        db.beginTransaction();

        Cursor cursor = db.query(Tables.TAB_GAME_APPS,
                new String[]{Columns.COLUMN_APP_PRESET_FLAG},
                whereClause, whereArgs,
                null, null, null);
        int presetFlag = 0;
        if (cursor != null && cursor.moveToFirst()) {
            presetFlag = cursor.getInt(0);
            cursor.close();
        }

        if (presetFlag == 1) { // preset data
            ContentValues values = new ContentValues();
            values.put(Columns.COLUMN_APP_SELECTED, 0);
            ret = db.update(Tables.TAB_GAME_APPS, values, whereClause, whereArgs);
        } else {
            ret = db.delete(Tables.TAB_GAME_APPS, whereClause, whereArgs);
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        return ret;
    }
}
