package com.freeme.safe.encryption.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;

import static com.freeme.safe.encryption.provider.EncryptionColumns.MEDIA_TYPE;

public class EncryptionContentProvider extends ContentProvider {

    public static final String DB_NAME = "encryption.db";
    public static final String ENCRYPTION_TABLE_NAME = "file";

    private static final int FILE = 1;
    private static final int FILE_ID = 2;

    private static final UriMatcher sUriMatcher = new UriMatcher(-1);

    private SQLiteDatabase mDb;
    private SQLiteOpenHelper mOpenHelper;

    static {
        sUriMatcher.addURI(EncryptionColumns.AUTHORITY, ENCRYPTION_TABLE_NAME, FILE);
        sUriMatcher.addURI(EncryptionColumns.AUTHORITY, "file/#", FILE_ID);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext(), DB_NAME);
        mDb = mOpenHelper.getWritableDatabase();
        return true;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        if (mOpenHelper == null) {
            return new ContentProviderResult[]{};
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int size = operations.size();
            ContentProviderResult[] result = new ContentProviderResult[size];
            for (int i = 0; i < size; i++) {
                result[i] = operations.get(i).apply(this, result, i);
            }
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (mOpenHelper == null) {
            return null;
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case FILE:
                qb.setTables(ENCRYPTION_TABLE_NAME);
                break;
            case FILE_ID:
                qb.setTables(ENCRYPTION_TABLE_NAME);
                //append ID with FILE_ID data
                qb.appendWhere(EncryptionColumns.ID + "=" + (uri.getPathSegments().get(1)));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Cursor ret = qb.query(mOpenHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
        Context context = getContext();
        if (context != null) {
            ret.setNotificationUri(context.getContentResolver(), uri);
        }
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = mOpenHelper.getWritableDatabase().update(ENCRYPTION_TABLE_NAME, values, selection, selectionArgs);
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (mOpenHelper == null) {
            return null;
        }
        if (initialValues == null) {
            initialValues = new ContentValues();
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case FILE:
                return ContentUris.withAppendedId(EncryptionColumns.FILE_URI,
                        db.insert(ENCRYPTION_TABLE_NAME, null,
                                ensureFile(initialValues, FILE)));
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (mOpenHelper == null) {
            return -1;
        }
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case FILE: {
                try {
                    count = mDb.delete(ENCRYPTION_TABLE_NAME, selection, selectionArgs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case FILE_ID: {
                String segment = uri.getPathSegments().get(1);
                if (TextUtils.isEmpty(selection)) {
                    selection = EncryptionColumns.ID + "=" + segment;
                } else {
                    selection = EncryptionColumns.ID + "=" + segment + " AND (" + selection + ")";
                }
                try {
                    count = mDb.delete(ENCRYPTION_TABLE_NAME, selection, selectionArgs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + uri);
        }
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    private ContentValues ensureFile(ContentValues initialValues, int tableType) {
        ContentValues values;
        if (initialValues == null) {
            values = new ContentValues();
        } else {
            values = new ContentValues(initialValues);
        }
        switch (tableType) {
            case FILE: {
                if (!values.containsKey(EncryptionColumns.ORIGINAL_PATH)) {
                    values.put(EncryptionColumns.ORIGINAL_PATH, "");
                }
                if (!values.containsKey(EncryptionColumns.ORIGINAL_SIZE)) {
                    values.put(EncryptionColumns.ORIGINAL_SIZE, 0);
                }
                if (!values.containsKey(EncryptionColumns.ORIGINAL_TYPE)) {
                    values.put(EncryptionColumns.ORIGINAL_TYPE, 0);
                }
                if (!values.containsKey(EncryptionColumns.ORIGINAL_COUNT)) {
                    values.put(EncryptionColumns.ORIGINAL_COUNT, -1);
                }
                if (!values.containsKey(MEDIA_TYPE)) {
                    values.put(MEDIA_TYPE, -1);
                }
                if (!values.containsKey(EncryptionColumns.ROOT_PATH)) {
                    values.put(EncryptionColumns.ROOT_PATH, "");
                }
                if (!values.containsKey(EncryptionColumns.ENCRYPTION_NAME)) {
                    values.put(EncryptionColumns.ENCRYPTION_NAME, "");
                }
                break;
            }
        }
        return values;
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        DatabaseHelper(Context context, String name) {
            super(context, name, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDatabase(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS file");
            onCreate(db);
        }
    }

    public static void createDatabase(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + ENCRYPTION_TABLE_NAME
                    + "("
                    + EncryptionColumns.ORIGINAL_PATH + " TEXT,"
                    + EncryptionColumns.ROOT_PATH + " TEXT,"
                    + EncryptionColumns.ORIGINAL_SIZE + " LONG,"
                    + EncryptionColumns.ORIGINAL_TYPE + " INTEGER,"
                    + EncryptionColumns.ORIGINAL_COUNT + " INTEGER DEFAULT -1,"
                    + MEDIA_TYPE + " INTEGER DEFAULT -1,"
                    + EncryptionColumns.ENCRYPTION_NAME + " TEXT,"
                    + EncryptionColumns.ID + " INTEGER PRIMARY KEY)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
