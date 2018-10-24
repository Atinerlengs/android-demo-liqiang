package com.freeme.safe.encryption.provider;

import android.net.Uri;

public class EncryptionColumns {
    static final String AUTHORITY = "com.freeme.safe.encryption";

    public static final Uri FILE_URI = Uri.parse("content://" + AUTHORITY + "/file");

    public static final String ID = "_id";

    public static final String MEDIA_TYPE = "media_type";

    public static final String ORIGINAL_COUNT = "original_count";
    public static final String ORIGINAL_PATH = "original_path";
    public static final String ORIGINAL_SIZE = "original_size";
    public static final String ORIGINAL_TYPE = "original_type";

    public static final String ROOT_PATH = "root_path";

    public static final String ENCRYPTION_NAME = "encryption_name";
}
