package com.freeme.filemanager.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class CleanUpDatabaseHelper {
    private final static String LOG_TAG = "CleanUpDatabaseHelper";

    private final static String DATABASE_PREFIX = ".db";

    private static final String PACK_NAME = "com.freeme.filemanager";
    
    public static final String TABLE_DIR_NAME = "dir_name";
    
    public static final String TABLE_DIR_COLUMN_PATH = "path";
    
    public static final String TABLE_DIR_COLUMN_NAME = "name";
    
    public static final String CACHE = "cache";

    private static final int BUFFER_SIZE = 1024 * 1024;

    private Context mContext;
    
    private static CleanUpDatabaseHelper mInstance = null;

    public CleanUpDatabaseHelper(Context context) {
        super();
        this.mContext = context;
    }
    
    public static CleanUpDatabaseHelper getDatabaseHelperInstance(Context context){
        if(mInstance == null){
            mInstance = new CleanUpDatabaseHelper(context);
        }
        return mInstance;
    }

    public SQLiteDatabase openDatabase() {
        String assetDbFileName = getAssetFileName();
        if (assetDbFileName == null) {
            return null;
        }
        String databasePath = getPackagePath() + assetDbFileName;
        File file = new File(databasePath);
        if (!file.exists() && !file.isDirectory()) {
            try {
                copyDbFile(getPackagePath(), assetDbFileName);
            } catch (IOException e) {
                Log.i(LOG_TAG, "openDatabase catch IOException: " + e.toString());
            }
        }
        int currentUserId = android.os.Process.myUserHandle().hashCode();
        if (currentUserId != 0) {
            String userDatabasePath = "/data/user/" + currentUserId+ "/com.freeme.filemanager/databases/";
            databasePath = userDatabasePath + assetDbFileName;
            try {
                copyDbFile(userDatabasePath, assetDbFileName);
            } catch (IOException e) {
                Log.i(LOG_TAG, "openDatabase catch IOException: " + e.toString());
            }
            
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY);
        
        return database;
    }

    /**
     * Get *.db file name in assets folder
     * @return db file name
     */
    private String getAssetFileName() {
        String dbFileName = null;
        AssetManager assetManager = mContext.getAssets();
        try {
            String[] files = assetManager.list("");
            for (int i = 0; i < files.length; i++) {
                if (files[i].contains(DATABASE_PREFIX)) {
                    dbFileName = files[i];
                    break;
                }
            }
        } catch (IOException e) {
            Log.i(LOG_TAG, "getAssetFileName catch IOException: " + e.toString());
        }
        return dbFileName;
    }

    /**
     * Get the absolute path of this app's database folder
     * @return database folder path
     */
    private String getPackagePath() {
        String str = Environment.getDataDirectory().getAbsolutePath();
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(str).append("/data/").append(PACK_NAME).append(File.separator).append("databases").append(File.separator);
        return stringBuffer.toString();
    }

    /**
     * Cpoy db file from assets folder to app's database folder
     * @param packagePath
     * @param assetdbFileName
     * @throws IOException
     */
    private void copyDbFile(String packagePath, String assetdbFileName) throws IOException {
        InputStream inputStream;

        if (!new File(packagePath + assetdbFileName).exists()) {
            File file = new File(packagePath);
            file.mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(packagePath + assetdbFileName);
        inputStream = mContext.getAssets().open(assetdbFileName);

        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int readCount = 0;
            while ((readCount = inputStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
                fileOutputStream.write(buffer, 0, readCount);
            }
        } finally {
            inputStream.close();
            fileOutputStream.close();
        }
    }
}
