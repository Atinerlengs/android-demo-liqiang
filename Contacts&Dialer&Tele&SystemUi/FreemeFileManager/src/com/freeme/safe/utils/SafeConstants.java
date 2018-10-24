package com.freeme.safe.utils;

import android.os.Environment;

public class SafeConstants {
    public static final int FRAGMENT_ALL_DEFAULT  = -1;
    public static final int FRAGMENT_ALL_SELECT   = 0;
    public static final int FRAGMENT_ALL_DESELECT = 1;

    public static final int LOCK_MODE_DEFAULT  = -1;
    public static final int LOCK_MODE_PATTERN  = 0;
    public static final int LOCK_MODE_PASSWORD = 1;
    public static final int LOCK_MODE_COMPLEX  = 2;

    public static final int REQUEST_GET_ENCRYPT_PATH  = 1;
    public static final int REQUEST_RESET_COMPLEX     = 5;
    public static final int REQUEST_RESET_PATTERN     = 6;
    public static final int REQUEST_RESET_PASSWORD    = 7;
    public static final int REQUEST_RESET_PASSCODE    = 9;
    public static final int REQUEST_ENTRY_SAFE        = 10;

    private static final String ACTIVITY_UNLOCK_ROOT        = "com.freeme.safe.password";
    public static final String APP_UNLOCK_COMPLEX_ACTIVITY  = ACTIVITY_UNLOCK_ROOT + ".UnlockComplexActivity";
    public static final String APP_UNLOCK_PASSWORD_ACTIVITY = ACTIVITY_UNLOCK_ROOT + ".UnlockPasswordActivity";
    public static final String APP_UNLOCK_PATTERN_ACTIVITY  = ACTIVITY_UNLOCK_ROOT + ".UnlockPatternActivity";
    public static final String PASSWORD = "password";

    public static final String SAFE_ROOT_PATH     = "/data/data/com.freeme.filemanager";
    public static final String PRIVATE_FILE_PATH  = SAFE_ROOT_PATH + "/private";
    public static final String TEMPS_FILE_PATH    = SAFE_ROOT_PATH + "/temps";
    public static final String LOCK_MODE_PATH     = "abcmodefas";
    public static final String LOCK_PASSWORD_PATH = "abcunlockpsd";

    public static final String NEW_APP_PROTECT_PASSWORD = "com.freeme.intent.action.NEW_APP_PROTECT_PASSWORD";
    public static final String NEW_APP_PROTECT_PATTERN  = "com.freeme.intent.action.NEW_APP_PROTECT_PATTERN";
    public static final String NEW_APP_PROTECT_COMPLEX  = "com.freeme.intent.action.NEW_APP_PROTECT_COMPLEX";

    public static final String IS_NEED_RESULT          = "is_need_result";
    public static final String IS_FIRST_SET            = "is_first_set";
    public static final String IS_MODIFY_PASSWORD      = "is_modify_password";
    public static final String IS_NEED_OPEN_SAFE       = "is_need_open_safe";
    public static final String IS_RESET_PASSWORD       = "is_reset_password";
    public static final String MODIFY_PASSWORD         = "modify_password";
    public static final String HEADER_TIP              = "header_tip";
    public static final String SUB_TIP                 = "sub_tip";
    public static final String FROM_LOCK_MODE_ACTIVITY = "from_lock_mode";

    public static final String FROM_SAFE = "from_safe_activity";
    public static final String DECRYPTION_PATH = "decryption_path";

    public static final String ENCRYPTION_KEY = "freeme safe welcome you";
    public static final String SELECTED_SAFE_FILE_TYPE = "selected_safe_file_type";

    public static final String SAFE_RECORD_NAME     = "safe_record";
    public static final String SAFE_UNLOCK_PASSWORD = "unlock_password";
    public static final String SAFE_LOCK_MODE_KEY   = "lock_mode";

    public static final String NEW_INTERNAL_PATH_START = "/data/media/0";

    public static final int SAME_NAME_MAX   = 10000;
    public static final int ENCRYPTION_SIZE = 3 * 1024;
    public static final long ENCRYPTION_SIZE_MAX = 2L * 1024 * 1024 * 1024;
    public static final long THRESHOLD = 5 * 1024 * 1024;

    public static final String ACTION_SAFE_FILE_VIEW = "com.freeme.intent.action.filemanager.VIEW";
    public static final String SAFE_FILE_PATH = "safe_file_path";
}