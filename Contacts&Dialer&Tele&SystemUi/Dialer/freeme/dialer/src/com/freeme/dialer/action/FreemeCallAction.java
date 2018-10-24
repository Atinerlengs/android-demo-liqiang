package com.freeme.dialer.action;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.support.annotation.IntDef;

public class FreemeCallAction {

    public static final int TYPE_ACTION_UNKNOWN = 0;
    public static final int TYPE_ACTION_REJECT = 1;
    public static final int TYPE_ACTION_ANSWER_AUDIO = 2;
    public static final int TYPE_ACTION_ANSWER_VIDEO = 3;
    @IntDef({TYPE_ACTION_UNKNOWN, TYPE_ACTION_REJECT, TYPE_ACTION_ANSWER_AUDIO,
            TYPE_ACTION_ANSWER_VIDEO})
    @interface ActionType {}

    public static final int TEXT_COLOR_DEFAULT = 0XFF8D8D8D;
    public static final int TEXT_COLOR_ANSWER = 0XFF29BD68;
    public static final int TEXT_COLOR_REJECT = 0XFFDE2D03;
    public static final int TEXT_COLOR_ANSWER_VIDEO = TEXT_COLOR_ANSWER;
    public static final int TEXT_COLOR_ANSWER_AUDIO = TEXT_COLOR_DEFAULT;
    @SuppressLint("UniqueConstants")
    @IntDef({TEXT_COLOR_DEFAULT, TEXT_COLOR_ANSWER, TEXT_COLOR_ANSWER_VIDEO,
            TEXT_COLOR_ANSWER_AUDIO, TEXT_COLOR_REJECT})
    @interface ActionTextColor {}

    private PendingIntent mPendingIntent;
    private CharSequence mText;
    private @ActionTextColor int mTextColor;
    private int mIcon;
    private boolean isEmpty;
    private @ActionType int mActionType;

    public FreemeCallAction() {
        this(0, null, null);
        isEmpty = true;
    }

    public FreemeCallAction(int icon, PendingIntent intent) {
        this(icon, null, intent);
    }

    public FreemeCallAction(CharSequence text, PendingIntent intent) {
        this(0, text, intent);
    }

    public FreemeCallAction(int icon, CharSequence text, PendingIntent intent) {
        this.mIcon = icon;
        this.mText = text;
        this.mPendingIntent = intent;

        mTextColor = TEXT_COLOR_DEFAULT;
        mActionType = TYPE_ACTION_UNKNOWN;
        isEmpty = false;
    }

    public int getIcon() {
        return mIcon;
    }

    public CharSequence getText() {
        return mText;
    }

    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public FreemeCallAction setTextColor(@ActionTextColor int color) {
        this.mTextColor = color;
        return this;
    }

    public @ActionTextColor int getTextColor() {
        return mTextColor;
    }

    public FreemeCallAction setActionType(@ActionType int type) {
        this.mActionType = type;
        return this;
    }

    public @ActionType int getActionType() {
        return mActionType;
    }
}
