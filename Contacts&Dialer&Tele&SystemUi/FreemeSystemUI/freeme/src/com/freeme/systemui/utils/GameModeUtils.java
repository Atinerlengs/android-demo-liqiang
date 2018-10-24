package com.freeme.systemui.utils;

import java.util.Arrays;
import java.util.List;

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;

import com.android.systemui.R;

import com.freeme.service.game.GameManager;

public class GameModeUtils {

    public static final String KEY_SHOULD_PEEK_IN_GAMEMODE = "shouldPeekInGameMode";

    private GameManager mGameManager;
    private List<String> mWhiteList;

    private static GameModeUtils sUtils;

    public static synchronized GameModeUtils getInstance(Context context) {
        if (sUtils == null) {
            sUtils = new GameModeUtils(context);
        }
        return sUtils;
    }

    private GameModeUtils(Context context) {
        mGameManager = GameManager.from(context);
        if (mGameManager == null) {
            return;
        }

        String[] arr = context.getResources().getStringArray(
                R.array.gm_heads_up_notification_white_list);
        mWhiteList = Arrays.asList(arr);
    }

    private boolean isGameModeActive() {
        return mGameManager != null && mGameManager.isGameModeActive();
    }

    public boolean isLockRecentKey() {
        return isGameModeActive() && mGameManager.lockedKeys(KeyEvent.KEYCODE_APP_SWITCH);
    }

    public boolean isBlockedNotification(StatusBarNotification statusBarNotification) {
        return isGameModeActive()
                && mGameManager.blockedNotification()
                && statusBarNotification != null
                && !shouldPeekInGameMode(statusBarNotification.getNotification())
                && !isInWhiteList(statusBarNotification.getPackageName());
    }

    private boolean isInWhiteList(String pkgName) {
        return mWhiteList != null && mWhiteList.contains(pkgName);
    }

    private boolean shouldPeekInGameMode(Notification n) {
        return n != null
                && n.extras != null
                && n.extras.getBoolean(KEY_SHOULD_PEEK_IN_GAMEMODE, false);
    }
}
