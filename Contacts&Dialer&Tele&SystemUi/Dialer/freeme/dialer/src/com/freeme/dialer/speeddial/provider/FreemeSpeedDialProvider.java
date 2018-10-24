package com.freeme.dialer.speeddial.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.freeme.dialer.speeddial.provider.FreemeSpeedDialDatabaseHelper.Tables;

public class FreemeSpeedDialProvider extends ContentProvider {

    private static final String TAG = "FreemeSpeedDialProvider";
    private static final int SPEEDDIAL = 1;
    private FreemeSpeedDialDatabaseHelper mDbHelper;

    private static final UriMatcher URIMATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URIMATCHER.addURI(FreemeSpeedDial.AUTHORITY, "numbers", SPEEDDIAL);
    }

    protected FreemeSpeedDialDatabaseHelper getDatabaseHelper(Context context) {
        FreemeSpeedDialDatabaseHelper dbHelper = FreemeSpeedDialDatabaseHelper.getInstance(context);
        return dbHelper;
    }

    @Override
    public boolean onCreate() {
        mDbHelper = getDatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        int match = URIMATCHER.match(uri);
        switch (match) {
            case SPEEDDIAL:
                return FreemeSpeedDial.Numbers.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int match = URIMATCHER.match(uri);
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor;
        switch (match) {
            case SPEEDDIAL:
                cursor = db.query(Tables.SPEEDDIAL, projection, selection, selectionArgs, null, null, sortOrder, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(Tables.SPEEDDIAL, null, values);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = URIMATCHER.match(uri);
        int result = -1;
        switch (match) {
            case SPEEDDIAL:
                result = db.update(Tables.SPEEDDIAL, values, selection, selectionArgs);
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int match = URIMATCHER.match(uri);
        int result = -1;
        switch (match) {
            case SPEEDDIAL:
                result = db.delete(Tables.SPEEDDIAL, selection, selectionArgs);
                break;
            default:
                break;
        }
        return result;
    }
}
