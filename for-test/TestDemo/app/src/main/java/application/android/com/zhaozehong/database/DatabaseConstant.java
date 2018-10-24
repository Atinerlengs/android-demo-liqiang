package application.android.com.zhaozehong.database;

import android.net.Uri;

public class DatabaseConstant {
    /**
     * The authority of the game mode content provider
     */
    public static final String AUTHORITY = "com.freeme.game";

    /**
     * A content:// style uri to the authority for the game mode provider
     */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * uri to get game app for the game mode provider
     */
    public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "game_app");

    public static class Tables {
        public static final String TAB_GAME_APPS = "game_apps";
    }

    public static class BaseColumns {
        public static final String _ID = "_id";
    }

    public static class Columns extends BaseColumns {
        public static final String COLUMN_APP_PACKAGE_NAME = "pkg_name";
        public static final String COLUMN_APP_SELECTED = "selected";
    }
}
