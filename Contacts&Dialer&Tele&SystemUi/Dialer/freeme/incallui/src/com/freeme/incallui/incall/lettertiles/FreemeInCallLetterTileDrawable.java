package com.freeme.incallui.incall.lettertiles;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.android.incallui.R;
import com.android.contacts.common.lettertiles.LetterTileDrawable;

public class FreemeInCallLetterTileDrawable extends LetterTileDrawable {
    private final Drawable mDefaultPersonAvatar;

    public FreemeInCallLetterTileDrawable(Resources res) {
        super(res);
        mDefaultPersonAvatar = res.getDrawable(R.drawable.freeme_incall_avatar_default, null);
    }

    @Override
    public void drawLetterTile(Canvas canvas) {
        drawAvatarWithOutLetter(canvas);
    }

    @Override
    public Drawable getDefaultPersonAvatar() {
        return mDefaultPersonAvatar;
    }
}
