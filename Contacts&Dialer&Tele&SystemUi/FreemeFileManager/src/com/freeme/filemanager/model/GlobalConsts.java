/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 *
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.model;

public abstract class GlobalConsts {
    public static final String KEY_BASE_SD = "key_base_sd";

    public static final String KEY_SHOW_CATEGORY = "key_show_category";

    public static final String INTENT_EXTRA_TAB = "TAB";

    public static final String ROOT_PATH = "/";

    public static final String SDCARD_PATH = ROOT_PATH + "sdcard2";

    // Menu id
    public static final int MENU_EDIT = 90;

    public static final int MENU_NEW_FOLDER = 100;

    public static final int MENU_FAVORITE = 101;

    public static final int MENU_SEARCH = 102;

    public static final int MENU_COPY = 104;

    public static final int MENU_PASTE = 105;

    public static final int MENU_MOVE = 106;

    public static final int MENU_SHOWHIDE = 117;

    public static final int MENU_COMPRESS = 118;

    public static final int MENU_DECOMPRESS = 119;

    public static final int OPERATION_UP_LEVEL = 3;

    // BroadCast string
    public static final String BROADCAST_REFRESH = "com.mediatek.filemanager.broadcast.refresh";

    public static final String BROADCAST_REFRESH_EXTRA = "refreshTabIndex";

    public static final int BROADCAST_REFRESH_TABCATEGORY = 120;

    public static final int BROADCAST_REFRESH_TABVIEW = 121;

    public static final int DECOMPRESS_ZIP_STATE_SUCCESS = 1001;

    public static final int DECOMPRESS_ZIP_STATE_FILE_EXISTS = 1002;

    public static final int DECOMPRESS_ZIP_STATE_NO_FREE = 1003;

    public static final int DECOMPRESS_ZIP_STATE_CANCEL = 1004;

    public static final int TYPE_NOTIFY_SCAN = 1005;

    public static final int TYPE_NOTIFY_REFRESH = 1006;
    //add by tyd liuyong 20140723
    public static final int DECOMPRESS_ZIP_STATE_NO_FILE = 1007;
    //add by tyd sxp 20141218
    public static final int TYPE_MOVE_NOTIFY_SCAN = 1008;
    //add by mingjun 20151207
    public static final int IS_CATEGORY_FRAGMENT = 0;

    public static final int IS_MEMORY_CARD = 1;

    public static final int IS_SD_CARD = 2;

    public static final int IS_USBOTG_CARD = 3;

}
