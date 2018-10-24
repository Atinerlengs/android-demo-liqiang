package com.freeme.filemanager.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class CleanupUtil {
    private static final String LOG_TAG = "CleanupUtil";

    public static final int WECHAT_TEMP_CACHE = 0;
    public static final int WECHAT_THROLD_FILES = 1;
    public static final int WECHAT_CHAT_MEDIA = 2;
    public static final int WECHAT_DOWNLOADS = 3;
    public static final int WECHAT_SAVED_MEDIA = 4;
    public static final int MONTHS_AGO = 3;
    public static final int MEMORY_THRESHOLD = 70;

    private static final int WECHAT_USER_PATH_MIN_LENGTH = 22;
    private static Locale sLocale = Locale.getDefault();
    private static HashMap<String, String> sPathAppNames;//path, name

    public static List<String> FilterPath(String str) {
        List arrayList = new ArrayList();
        if (TextUtils.isEmpty(str)) {
            return null;
        }

        int indexOf = str.indexOf("*");
        if (indexOf == -1) {
            arrayList.add(str);
        } else {
            File file = new File(str.substring(0, indexOf));
            if (!file.exists() || file.isFile()) {
                return  null;
            }

            String[] list = file.list();
            if (list == null ) {
                return  null;
            }

            int length = list.length;
            if (length <= 0) {
                return null;
            }

            int list_pos = 0;
            while (list_pos < length) {
                CharSequence charSequence = list[list_pos];
                if (charSequence.length() <= WECHAT_USER_PATH_MIN_LENGTH) {
                    list_pos++;
                    continue;
                }

                String realpath = str.replace("*", charSequence);
                if (new File(realpath).exists()) {
                    arrayList.add(realpath);
                }
                list_pos++;
            }
        }
        return arrayList;
    }

    /**
     * Get the app name which the relativeaPath belong to
     * @param context
     * @param relativePath
     * @return appName
     */
    public static String getAppNameByPath(Context context, String relativePath) {
        String appName = null;
        if(!isSupportedLauguage())
        {
            return appName;
        } else {
            if (sPathAppNames == null) {
                sPathAppNames = new HashMap();
            }
            // if the relativePath contained in sPathAppNames, then return the value
            if(sPathAppNames.containsKey(relativePath)){
                appName = sPathAppNames.get(relativePath);
                return appName;
            }
            // get the appName by query package_data table
            CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(context);
            SQLiteDatabase db = dbHelper.openDatabase();
            Cursor cursor = null;
            try{
                cursor = db.rawQuery("select name from folder_name where path = '"+relativePath+"'", null); 
                while(cursor.moveToNext()){
                    appName = cursor.getString(0);
                    sPathAppNames.put(relativePath, appName);
                }
            }catch(Exception e){
                Log.i(LOG_TAG, "getAppNameByPath() catch exception: "+e.toString());
            }finally{
                if(cursor != null && cursor.isClosed()){
                    cursor.close();
                    db.close();
                }
                return appName;
            }
            
        }
    }
    
    private static boolean isSupportedLauguage()
    {
        boolean isSupported;
        if(Locale.CHINA.equals(sLocale) || Locale.US.equals(sLocale))
            isSupported = true;
        else
            isSupported = false;
        return isSupported;
    }
    
    private static void initiatePathAppNames(Context context)
    {
        sPathAppNames = new HashMap();
        loadNamesFromWhiteList(context);
    }
    
    private static void loadNamesFromWhiteList(Context context)
    {
        CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(context);
        SQLiteDatabase db = dbHelper.openDatabase();
        
        Cursor cursor = null;
        try{
            cursor = db.rawQuery("select (path,names) from white_list", null); 
            while(cursor.moveToNext()){
                HashMap<String, String> hashmap = new HashMap();
                String path = cursor.getString(0);
                String[] names = cursor.getString(1).split(":");
                for(int i=0; i<names.length; i++){
                    String[] langName = names[i].split("=");
                    hashmap.put(langName[0], langName[1]);
                }
                String localeName = sLocale.toString().toLowerCase();
                String appName = null;
                if(hashmap.containsKey(localeName)){
                    appName = hashmap.get(localeName);
                }
                if(appName != null){
                    sPathAppNames.put(path, appName);
                }
            }
            
            
        }catch(Exception e){
            Log.i(LOG_TAG, "loadNamesFromWhiteList() catch exception: "+e.toString());
        }finally{
            if(cursor != null){
                cursor.close();
                db.close();
            }
        }
    }
    
    public static ArrayList<String> getWhiteList(Context context){
        ArrayList<String> whiteList = new ArrayList<String>();
        CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(context);
        SQLiteDatabase db = dbHelper.openDatabase();
        
        Cursor cursor = null;
        try{
            cursor = db.rawQuery("select path from white_list", null); 
            while(cursor.moveToNext()){
                whiteList.add(cursor.getString(0));
            }
        }catch(Exception e){
            Log.i(LOG_TAG, "getWhiteList() catch exception: "+e.toString());
        }finally{
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
                db.close();
            }
            return whiteList;
        }
    }
    
    
    /**
     * Get a list of folder path which belong to the given packageName
     * @param context
     * @param packageName
     * @return packagePathList
     */
    public static ArrayList<String> getPackagePath(Context context, String packageName){
        ArrayList<String> packagePathList = new ArrayList<String>();
        CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(context);
        SQLiteDatabase db = dbHelper.openDatabase();
        Cursor cursor = null;
        try{
            cursor = db.rawQuery("select folder_path from package_path where package_name='"+packageName+"'", null); 
            while(cursor.moveToNext()){
                packagePathList.add(cursor.getString(0));
            }
        }catch(Exception e){
            Log.i(LOG_TAG, "getPackagePath() catch exception: "+e.toString());
        }finally{
            if(cursor != null){
                cursor.close();
                db.close();
            }
            return packagePathList;
        }
    }
    
    /**
     * Get temp folder path list
     * @param context
     * @return tempFolderList
     */
    public static ArrayList<String> getTempFolderList(Context context){
        ArrayList<String> tempFolderList = new ArrayList<String>();
        CleanUpDatabaseHelper dbHelper = CleanUpDatabaseHelper.getDatabaseHelperInstance(context);
        SQLiteDatabase db = dbHelper.openDatabase();
        Cursor cursor = null;
        try{
            cursor = db.rawQuery("select path from temp_folder", null); 
            while(cursor.moveToNext()){
                tempFolderList.add(cursor.getString(0));
            }
        }catch(Exception e){
            Log.i(LOG_TAG, "getPackagePath() catch exception: "+e.toString());
        }finally{
            if(cursor != null){
                cursor.close();
                db.close();
            }
            return tempFolderList;
        }
    }
}
