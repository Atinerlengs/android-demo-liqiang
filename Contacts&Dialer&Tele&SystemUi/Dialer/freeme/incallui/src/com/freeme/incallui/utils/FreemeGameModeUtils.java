package com.freeme.incallui.utils;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.freeme.provider.FreemeSettings;
import com.freeme.service.game.GameManager;

public class FreemeGameModeUtils {

    private Context mContext;
    private GameManager mGameManager;

    public FreemeGameModeUtils(Context context) {
        mContext = context;
        mGameManager = GameManager.from(context);
    }

    public boolean isGameModeActive() {
        return mGameManager != null && mGameManager.isGameModeActive();
    }

    private boolean supportAnswerCallViaSpeaker() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                FreemeSettings.System.FREEME_GAMEMODE_SETS_ANSWER_CALL,
                0, UserHandle.USER_CURRENT_OR_SELF) != 0;
    }

    public boolean answerCallViaSpeaker() {
        return isGameModeActive() && supportAnswerCallViaSpeaker();
    }
}
