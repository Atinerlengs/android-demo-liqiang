package com.freeme.dialer.speeddial;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;


/**
 * Special class to to allow the parent to be pressed without being pressed itself.
 * This way the line of a tab can be pressed, but the image itself is not.
 */
public class FreemeDontPressWithParentImageView extends ImageView {

    public FreemeDontPressWithParentImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setPressed(boolean pressed) {
        // If the parent is pressed, do not set to pressed.
        if (pressed && ((View) getParent()).isPressed()) {
            super.setPressed(false);
            return;
        }
        super.setPressed(pressed);
    }
}
