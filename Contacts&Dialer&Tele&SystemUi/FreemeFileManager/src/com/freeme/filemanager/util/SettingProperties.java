package com.freeme.filemanager.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.text.TextUtils;

public class SettingProperties extends Properties {

    private static SettingProperties sInstance;

    public static SettingProperties build(String commonFromResource, String... specificFromFiles)
            throws IOException {
        SettingProperties instance = sInstance;
        if (instance == null) {
            synchronized (SettingProperties.class) {
                if (instance == null) {
                    instance = new SettingProperties();
                    sInstance = instance;

                    String specificFromFile = null;
                    if (specificFromFiles != null) {
                        for (String file : specificFromFiles) {
                            if ((new File(file)).exists()) {
                                specificFromFile = file;
                                break;
                            }
                        }
                    }
                    // If failed, throw exception
                    instance.load(commonFromResource, specificFromFile);
                }
            }
        }
        return instance;
    }

    public static SettingProperties getInstance() {
        SettingProperties instance = sInstance;
        if (instance == null) {
            throw new RuntimeException("SettingProperties has not be initialized.");
        }
        return instance;
    }

    private static final long serialVersionUID = -4800785546075953211L;

    public SettingProperties() {
    }

    /**
     * @param resourceName Base setting file from resource assets
     * @param fileName     Spec setting file from file system
     */
    public void load(String resourceName, String fileName) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourceName);
        if (in != null) {
            try {
                load(in);
            } finally {
                CloseUtils.closeQuietly(in);
            }
        }

        if (!TextUtils.isEmpty(fileName) && (new File(fileName)).exists()) {
            FileReader reader = null;
            try {
                reader = new FileReader(fileName);
                load(reader);
            } finally {
                CloseUtils.closeQuietly(reader);
            }
        }
    }

    public String get(String name, String defaultValue) {
        return getProperty(name, defaultValue);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        String value = getProperty(name, "");
        return TextUtils.isEmpty(value) ? defaultValue : value.equals("true");
    }

    public boolean getBoolean(String name) {
        return getBoolean(name, false);
    }

    public int getInt(String name, int defaultValue) {
        String value = getProperty(name, "");
        return TextUtils.isEmpty(value) ? defaultValue : Integer.parseInt(value);
    }

    public int getInt(String name) {
        return getInt(name, 0);
    }

    public float getFloat(String name, float defaultValue) {
        String value = getProperty(name, "");
        return TextUtils.isEmpty(value) ? defaultValue : Float.parseFloat(value);
    }

    public float getFloat(String name) {
        return getFloat(name, 0);
    }
}