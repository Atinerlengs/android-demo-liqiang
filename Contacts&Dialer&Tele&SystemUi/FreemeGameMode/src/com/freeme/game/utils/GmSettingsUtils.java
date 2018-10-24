package com.freeme.game.utils;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.KeyEvent;

import com.freeme.provider.FreemeSettings;
import com.freeme.util.FreemeOption;

public class GmSettingsUtils {

    /**
     * Game mode control switcher
     *
     * @param context Context
     * @return true: Game mode is turned on
     */
    public boolean isGameModeTurnedOn(Context context) {
        return FreemeOption.FREEME_GAMEMODE_SUPPORT
                && Settings.System.getIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE,
                0, UserHandle.USER_CURRENT_OR_SELF) != 0;
    }

    /**
     * Game mode control switcher
     *
     * @param context Context
     * @param on      true, turn on
     */
    public void turnGameModeOn(Context context, boolean on) {
        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE,
                on ? 1 : 0, UserHandle.USER_CURRENT_OR_SELF);
    }

    /**
     * Game tool control switcher
     *
     * @param context Context
     * @return true: Game tool is turned on
     */
    public boolean isGameToolTurnedOn(Context context) {
        return FreemeOption.FREEME_GAMEMODE_TOOL_SUPPORT
                && isGameModeTurnedOn(context)
                && (Settings.System.getIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_TOOLS,
                0, UserHandle.USER_CURRENT_OR_SELF) != 0);
    }

    /**
     * Game tool control switcher
     *
     * @param context Context
     * @param on      true, turn on
     */
    public void turnGameToolOn(Context context, boolean on) {
        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_TOOLS,
                on ? 1 : 0, UserHandle.USER_CURRENT_OR_SELF);
    }

    /**
     * Answer incoming calls in the background through speaker when Game mode is turned on
     *
     * @param context Context
     * @return true, Answer incoming calls in the background through speaker
     */
    public boolean isAnswerCallViaSpeaker(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_ANSWER_CALL,
                0, UserHandle.USER_CURRENT_OR_SELF) != 0;
    }

    /**
     * Answer incoming calls in the background through speaker when Game mode is turned on
     *
     * @param context    Context
     * @param viaSpeaker true, Answer incoming calls in the background through speaker
     */
    public void setAnswerCallViaSpeaker(Context context, boolean viaSpeaker) {
        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_ANSWER_CALL,
                viaSpeaker ? 1 : 0, UserHandle.USER_CURRENT_OR_SELF);
    }

    /**
     * Block heads-up notifications when Game mode is turned on
     *
     * @param context Context
     * @return true, block heads-up notifications
     */
    public boolean isBlockedNotification(Context context) {
        return isGameModeTurnedOn(context)
                && (Settings.System.getIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_BLOCK_NOTIFICATIONS,
                0, UserHandle.USER_CURRENT_OR_SELF) != 0);
    }

    /**
     * Block heads-up notifications when Game mode is turned on
     *
     * @param context Context
     * @param block   true, block heads-up notifications
     */
    public void setBlockNotification(Context context, boolean block) {
        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_BLOCK_NOTIFICATIONS,
                block ? 1 : 0, UserHandle.USER_CURRENT_OR_SELF);
    }

    private static final int LOCKED_KEY_BACK = 0;
    private static final int LOCKED_KEY_RECENT = 1;

    private int getLockedKeysValue(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_LOCK_KEYS, 0,
                UserHandle.USER_CURRENT_OR_SELF);
    }

    /**
     * Lock the recent and back keys on the navigation bar
     *
     * @param context Context
     * @param keyCode
     * @return true, lock the recent and back keys
     */
    public boolean isLockedKeys(Context context, int keyCode) {
        int value = getLockedKeysValue(context);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return (value & (1 << LOCKED_KEY_BACK)) != 0;
            case KeyEvent.KEYCODE_APP_SWITCH:
                return (value & (1 << LOCKED_KEY_RECENT)) != 0;
        }
        return false;
    }

    /**
     * Lock the recent and back keys on the navigation bar
     *
     * @param context Context
     * @param lock    true, lock the recent and back keys
     */
    public void setLockKeys(Context context, boolean lock) {
        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_LOCK_KEYS,
                lock ? -1 : 0, UserHandle.USER_CURRENT_OR_SELF);
    }

    /**
     * Lock the recent and back keys on the navigation bar
     *
     * @param context Context
     * @param keyCode
     * @param lock    true, lock the recent and back keys
     */
    public void setLockKeys(Context context, int keyCode, boolean lock) {
        int old_value = getLockedKeysValue(context);
        int new_value;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                new_value = 1 << LOCKED_KEY_BACK;
                break;
            case KeyEvent.KEYCODE_APP_SWITCH:
                new_value = 1 << LOCKED_KEY_RECENT;
                break;
            default:
                return;
        }

        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_LOCK_KEYS,
                lock ? old_value | new_value : old_value & ~new_value,
                UserHandle.USER_CURRENT_OR_SELF);
    }

    /**
     * Lock screen automatic brightness when Game mode is turned on
     *
     * @param context Context
     * @return true: block
     */
    public boolean isBlockedAutoBrightness(Context context) {
        return isGameModeTurnedOn(context)
                && (Settings.System.getIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_BLOCK_AUTO_BRIGHTNESS,
                0, UserHandle.USER_CURRENT_OR_SELF) != 0);
    }

    /**
     * Lock screen automatic brightness when Game mode is turned on
     *
     * @param context Context
     * @param block   true, block automatic brightness
     */
    public void setBlockAutoBrightness(Context context, boolean block) {
        Settings.System.putIntForUser(context.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_BLOCK_AUTO_BRIGHTNESS,
                block ? 1 : 0, UserHandle.USER_CURRENT_OR_SELF);
    }
}
