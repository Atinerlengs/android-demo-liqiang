package com.freeme.incallui.floating;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;

import com.freeme.service.game.GameManager;
import com.freeme.service.game.IGameStatusWatcher;

import static com.freeme.provider.FreemeSettings.System.FREEME_GAMEMODE;

public class FreemeInCallFloatingService extends Service {

    private final IGameStatusWatcher mWatcher = new IGameStatusWatcher.Stub() {

        private boolean mLastActive;

        @Override
        public void onGameModeActive(boolean active) {
            if (mLastActive != active) {
                toggleFloatingView(!active, MSG_DELAY_MILLIS);

                mLastActive = active;
            }
        }
    };

    private final class Constants extends ContentObserver {
        final Uri kUriGameModeMaster = Settings.System.getUriFor(FREEME_GAMEMODE);

        private ContentResolver mResolver;

        public Constants(Context context, Handler handler) {
            super(handler);
            mResolver = context.getContentResolver();

            mResolver.registerContentObserver(kUriGameModeMaster, false, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }

            if (kUriGameModeMaster.equals(uri)) {
                boolean mGameMaster = Settings.System.getIntForUser(mResolver,
                        FREEME_GAMEMODE, 0, UserHandle.USER_CURRENT_OR_SELF) != 0;
                if (mGameMaster) {
                    registerGameStatsWatcher();
                } else {
                    unregisterGameStatsWatcher();

                    toggleFloatingView(true, MSG_DELAY_MILLIS);
                }
            }
        }

        void unregisterContentObserver(){
            mResolver.unregisterContentObserver(this);
        }
    }
    private Constants mConstants;

    private static final int MSG_COMPONENT_RESUMED = 1;

    private static final int MSG_DELAY_MILLIS = 500;

    /**
     * Handler for asynchronous operations performed by the game manager.
     */
    private final class ViewManagerHandler extends Handler {
        ViewManagerHandler(Looper looper) {
            super(looper, null, true /*async*/);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_COMPONENT_RESUMED:
                    boolean showFloating = (boolean) msg.obj;
                    if (showFloating) {
                        mFloatingManager.createView();
                    } else {
                        mFloatingManager.removeView();
                    }
                    break;
            }
        }
    }
    private ViewManagerHandler mHandler;

    private FreemeInCallFloatingManager mFloatingManager;
    private GameManager mGameManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();

        mHandler = new ViewManagerHandler(Looper.getMainLooper());

        mGameManager = GameManager.from(context);

        mConstants = new Constants(context, mHandler);
        mConstants.onChange(false, mConstants.kUriGameModeMaster);

        mFloatingManager = new FreemeInCallFloatingManager(getApplicationContext());
        toggleFloatingView(true, MSG_DELAY_MILLIS);
    }

    @Override
    public void onDestroy() {
        mConstants.unregisterContentObserver();

        unregisterGameStatsWatcher();

        toggleFloatingView(false, 0);

        super.onDestroy();
    }

    private void registerGameStatsWatcher() {
        if (mGameManager != null) {
            mGameManager.registerGameStatsWatcher(mWatcher);
        }
    }

    private void unregisterGameStatsWatcher() {
        if (mGameManager != null) {
            mGameManager.unregisterGameStatsWatcher(mWatcher);
        }
    }

    private void toggleFloatingView(boolean show, long delayMillis) {
        Message msg = mHandler.obtainMessage(MSG_COMPONENT_RESUMED, show);
        mHandler.removeMessages(MSG_COMPONENT_RESUMED);
        mHandler.sendMessageDelayed(msg, delayMillis);
    }

    public static void start(Context context) {
        if (context != null) {
            if (FreemeInCallFloatingUtils.getPowerManager(context).isScreenOn()) {
                context.startService(new Intent(context, FreemeInCallFloatingService.class));
            }
        }
    }

    public static void stop(Context context) {
        if (context != null) {
            context.stopService(new Intent(context, FreemeInCallFloatingService.class));
        }
    }
}
