package com.freeme.onehand;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.WindowManager;

final class OneHandWindowInfo {
    private static final String TAG = "OneHandWindowInfo";
    private static final boolean DBG = OneHandConstants.DEBUG;

    private static final OneHandWindowInfo mSingleton = new OneHandWindowInfo();
    static OneHandWindowInfo getInstance() {
        return mSingleton;
    }

    private Context mContext;
    private WindowManager mWindowManager;
    private Resources mResources;

    private final DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private final Point mScreenSize = new Point();

    int mOffsetX;
    int mOffsetY;

    private float mUserScale;
    private float mDefaultScale;
    private float mMinScale;
    float mScale = 1.0f;

    private int mDiagonalDist;
    private boolean mGestureEnabled = true;
    private boolean mIsSwitchPositionRunning;
    private boolean mLaunchedByHomeTriple;
    private boolean mLeftHandMode;

    final Rect mMagnifyRect = new Rect();
    final Rect mMainRect = new Rect();

    private int mSoftkeyMode;
    private float mPhysicalInch;

    private int mReduceScreenButtonCount;
    private int mReduceScreenGestureCount;
    private int mReduceScreenLaunchCount;
    private boolean mReturnToFullScreen;

    private boolean mSupportNavigationBar;
    private float mSwipeScaleFactor;
    private int mTouchableOffset = -1;

    private OneHandWindowInfo() {
    }

    void init(Context context) {
        mContext = context.getApplicationContext();
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mResources = context.getResources();

        loadSettingValue();
        loadSharedPreference();

        updateDisplayMatrix();
        getTouchableWindowOffset();

        if (DBG) {
            Log.d(TAG, (new StringBuilder())
                    .append("init() ")
                    .append("mPhysicalInch=").append(mPhysicalInch).append(", ")
                    .append("mDiagonalDist=").append(mDiagonalDist).append(", ")
                    .append("mScreenSize=").append(mScreenSize).append(", ")
                    .append("mSwipeScaleFactor=").append(mSwipeScaleFactor).append(", ")
                    .append("mTouchableOffset=").append(mTouchableOffset).append(", ")
                    .append("mDisplayMetrics=").append(mDisplayMetrics)
                    .toString());

        }
    }

    void updateDisplayMatrix() {
        if (DBG) {
            Log.d(TAG, "updateDisplayMatrix()");
        }
        Display display;
        if (mWindowManager != null && (display = mWindowManager.getDefaultDisplay()) != null) {
            display.getMetrics(mDisplayMetrics);
            display.getRealSize(mScreenSize);
            mMagnifyRect.set(0, 0, getScreenWidth(), getScreenHeight());
            mMainRect.set(0, 0, getScreenWidth(), getScreenHeight());

            DisplayInfo displayInfo = new DisplayInfo();
            display.getDisplayInfo(displayInfo);

            float physicalInch = (float) Math.sqrt(
                    Math.pow(mDisplayMetrics.widthPixels / mDisplayMetrics.xdpi, 2)
                    + Math.pow(mDisplayMetrics.heightPixels / mDisplayMetrics.ydpi, 2));
            mSwipeScaleFactor = physicalInch > 5.7f ? 1.3f :
                                physicalInch > 5.4f ? 1.2f :
                                        1.0f;

            mMinScale = OneHandConstants.ONEHAND_INCH_MIN / physicalInch;
            mDefaultScale = OneHandConstants.ONEHAND_INCH_DEF / physicalInch;
            if (Math.abs(mUserScale - OneHandConstants.ONEHAND_SCALE_UNDEF) < 0.0000001f) {
                mUserScale = mDefaultScale;
            }
            mDiagonalDist = (int) Math.hypot(getScreenWidth(), getScreenHeight());
            mPhysicalInch = physicalInch;

            if (DBG) {
                Log.d(TAG, (new StringBuilder(128))
                        .append("updateDisplayMatrix() ")
                        .append("physicalInch=").append(physicalInch).append(", ")
                        .append("diagonalDist=").append(mDiagonalDist).append(", ")
                        .append("screenSize=").append(mScreenSize).append(", ")
                        .append("swipeScaleFactor=").append(mSwipeScaleFactor).append(", ")
                        .append("displayMetrics=").append(mDisplayMetrics).append(", ")
                        .append("displayInfo=").append(displayInfo)
                        .toString());
            }
        }
    }

    void screenTurnedOn() {
        setWindowChanged(1.0f, 0, 0);
        mReturnToFullScreen = false;
    }

    void setWindowChanged(float scale, int offsetX, int offsetY) {
        mScale = scale;
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mMagnifyRect.set(offsetX, offsetY,
                (int) (offsetX + (mScale * getScreenWidth()) + 0.5f),
                (int) (offsetY + (mScale * getScreenHeight()) + 0.5f));
    }

    void setNavigationBarSupportState(boolean support) {
        mSupportNavigationBar = support;
    }

    boolean isSupportNavigationBar() {
        return mSupportNavigationBar;
    }

    int getSideMargin() {
        return (int) mResources.getDimension(R.dimen.onehand_side_margin);
    }

    int getBottomMargin() {
        if (mSupportNavigationBar) {
            return 0;
        }
        if (isSoftkeyMode()) {
            return (int) mResources.getDimension(R.dimen.onehand_bottom_icon_size);
        }
        return (int) mResources.getDimension(R.dimen.onehand_bottom_margin);
    }

    int getScreenWidth() {
        return mScreenSize.x;
    }

    int getScreenHeight() {
        return mScreenSize.y;
    }

    float getWindowScale() {
        return mScale;
    }

    boolean isTripleHomeStyle() {
        return mLaunchedByHomeTriple;
    }

    void setTripleHomeStyle(boolean tripleHomeStyle) {
        mLaunchedByHomeTriple = tripleHomeStyle;
    }

    boolean isLeftHandMode() {
        return mLeftHandMode;
    }

    void setLeftHandMode(boolean left) {
        mLeftHandMode = left;
    }

    int getDiagonalDist() {
        return mDiagonalDist;
    }

    int getTriggerDistance() {
        return (int) mContext.getResources().getDimension(R.dimen.onehand_trigger_distance);
    }

    int getTouchableAreaHeight() {
        return (int) mResources.getDimension(R.dimen.onehand_touchable_area_height);
    }

    int getTouchableAreaWidth() {
        return (int) mResources.getDimension(R.dimen.onehand_touchable_area_width);
    }

    int getTouchableAreaBreadth() {
        return (int) mResources.getDimension(R.dimen.onehand_touchable_area_breadth);
    }

    int getSideWindowGap() {
        return (int) mResources.getDimension(R.dimen.onehand_side_window_gap);
    }

    int getIconSize() {
        return (int) mResources.getDimension(R.dimen.onehand_default_icon_size);
    }

    int getResizeStrokeThickness() {
        return (int) mResources.getDimension(R.dimen.onehand_resize_queue_border_tickness);
    }

    int getResizeHandleSide() {
        return (int) mResources.getDimension(R.dimen.onehand_resize_handle_size);
    }

    int getTouchableWindowOffset() {
        if (mTouchableOffset < 0) {
            mTouchableOffset = getScreenHeight() - getTouchableAreaHeight();
        }
        return mTouchableOffset;
    }

    boolean isSoftkeyMode() {
        return mSoftkeyMode == 1;
    }

    void setSoftkeyMode(int mode) {
        if (DBG) {
            Log.d(TAG, "setSoftkeyMode() mode=" + mode);
        }
        mSoftkeyMode = mode;
    }

    void loadSettingValue() {
        ContentResolver resolver = mContext.getContentResolver();
        mSoftkeyMode = Settings.System.getIntForUser(resolver,
                OneHandConstants.ONEHAND_SHOW_HARD_KEYS, 0,
                UserHandle.USER_CURRENT);
        mLaunchedByHomeTriple = Settings.System.getIntForUser(resolver,
                OneHandConstants.ONEHAND_WAKEUP_TYPE, 0,
                UserHandle.USER_CURRENT) != 0;
    }

    private static final String SETTINGS_SP_ITEM_SCALE = "onehand_default_scale";
    private static final String SETTINGS_SP_ITEM_REDUCE_COUNT = "onehand_reduce_count";
    private static final String SETTINGS_SP_ITEM_REDUCE_COUNT_BYGESTURE = "onehand_reduce_gesture_count";
    private static final String SETTINGS_SP_ITEM_REDUCE_COUNT_BYBUTTON = "onehand_reduce_button_count";
    private SharedPreferences mSharedPreference;
    private SharedPreferences getSharedPreference() {
        if (mSharedPreference == null) {
            mSharedPreference = mContext.getSharedPreferences(
                    "onehandwindowInfo_sharedpreference", Context.MODE_PRIVATE);
        }
        return mSharedPreference;
    }
    private void loadSharedPreference() {
        SharedPreferences sp = getSharedPreference();
        mUserScale = sp.getFloat(SETTINGS_SP_ITEM_SCALE, OneHandConstants.ONEHAND_SCALE_UNDEF);
        mReduceScreenLaunchCount = sp.getInt(SETTINGS_SP_ITEM_REDUCE_COUNT, 0);
        mReduceScreenGestureCount = sp.getInt(SETTINGS_SP_ITEM_REDUCE_COUNT_BYGESTURE, 0);
        mReduceScreenButtonCount = sp.getInt(SETTINGS_SP_ITEM_REDUCE_COUNT_BYBUTTON, 0);
        if (DBG) {
            Log.d(TAG, (new StringBuilder(128))
                    .append("loadSharedPreference() ")
                    .append("UserScale=").append(mUserScale).append(", ")
                    .append("LaunchCount=").append(mReduceScreenLaunchCount).append(", ")
                    .append("GestureCount=").append(mReduceScreenGestureCount).append(", ")
                    .append("ButtonCount=").append(mReduceScreenButtonCount)
                    .toString());
        }
    }
    private void saveSharedPreference() {
        getSharedPreference().edit()
                .putFloat(SETTINGS_SP_ITEM_SCALE, mUserScale)
                .putInt(SETTINGS_SP_ITEM_REDUCE_COUNT, mReduceScreenLaunchCount)
                .putInt(SETTINGS_SP_ITEM_REDUCE_COUNT_BYGESTURE, mReduceScreenGestureCount)
                .putInt(SETTINGS_SP_ITEM_REDUCE_COUNT_BYBUTTON, mReduceScreenButtonCount)
                .apply();
        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("saveSharedPreference() ")
                    .append("mUserScale=").append(mUserScale).append(", ")
                    .append("mReduceScreenLaunchCount=").append(mReduceScreenLaunchCount)
                    .toString());
        }
    }

    void changeDefaultScale(float scale) {
        if (DBG) {
            Log.d(TAG, (new StringBuilder(64))
                    .append("changeDefaultScale() ")
                    .append("scale=").append(scale).append(", ")
                    .append("mScale=").append(mScale).append(", ")
                    .append("mUserScale=").append(mUserScale)
                    .toString());
        }
        mUserScale = scale;
        saveSharedPreference();
    }

    void increaseReduceScreenLaunchCount() {
        if (DBG) {
            Log.d(TAG, "ReduceScreen Launched. count=" + mReduceScreenLaunchCount);
        }
        mReduceScreenLaunchCount++;
        if (isTripleHomeStyle()) {
            mReduceScreenButtonCount++;
        } else {
            mReduceScreenGestureCount++;
        }
        saveSharedPreference();
    }

    int getReduceScreenLaunchButtonCount() {
        return mReduceScreenButtonCount;
    }

    float getDefaultScale() {
        return mUserScale;
    }

    float getMinScale() {
        return mMinScale;
    }

    float getMaxScale() {
        int h = getScreenHeight();
        int w = getScreenWidth();
        return Math.min((h - getBottomMargin()) / (float) h,
                (((w - getSideMargin()) - getSideWindowGap()) - getIconSize()) / (float) w);
    }

    float getFullScale() {
        int h = getScreenHeight();
        int w = getScreenWidth();
        return Math.min((h - getBottomMargin() - 2) / (float) h,
                (w - getSideMargin() - 2) / (float) w);
    }

    boolean isPortraitMode() {
        return getScreenHeight() > getScreenWidth();
    }

    void setReturnToFullScreen(boolean exit) {
        mReturnToFullScreen = exit;
    }

    boolean isReturnToFullScreen() {
        return mReturnToFullScreen;
    }

    void setSwitchPositionRunning(boolean running) {
        mIsSwitchPositionRunning = running;
    }

    boolean isSwitchAnimationRunning() {
        return mIsSwitchPositionRunning;
    }

    void setGestureByProximitySensor(boolean enable) {
        mGestureEnabled = enable;
    }

    boolean isGestureEnabledByProximitySensor() {
        return mGestureEnabled;
    }
}
