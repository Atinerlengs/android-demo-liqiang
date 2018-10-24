package com.freeme.onehand;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;

public final class OneHandViewUnbindHelper {

    public static void unbindReferences(View view) {
        if (view != null) {
            try {
                unbindViewReferences(view);
                if (view instanceof ViewGroup) {
                    unbindViewGroupReferences((ViewGroup) view);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void unbindViewGroupReferences(ViewGroup group) {
        int size = group.getChildCount();
        for (int i = 0; i < size; i++) {
            View view = group.getChildAt(i);
            unbindViewReferences(view);
            if (view instanceof ViewGroup) {
                unbindViewGroupReferences((ViewGroup) view);
            }
        }
        try {
            group.removeAllViews();
        } catch (Exception ignored) {
        }
    }

    private static void unbindViewReferences(View view) {
        try {
            view.setOnClickListener(null);
            view.setOnCreateContextMenuListener(null);
            view.setOnFocusChangeListener(null);
            view.setOnKeyListener(null);
            view.setOnLongClickListener(null);
            view.setOnClickListener(null);
            view.setTouchDelegate(null);
        } catch (Exception ignored) {
        }

        Drawable d = view.getBackground();
        if (d != null) {
            try {
                d.setCallback(null);
            } catch (Exception ignored) {
            }
        }

        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            d = imageView.getDrawable();
            if (d != null) {
                d.setCallback(null);
            }
            imageView.setImageDrawable(null);
        } else if (view instanceof WebView) {
            ((WebView) view).destroyDrawingCache();
            ((WebView) view).destroy();
        }

        try {
            view.setBackground(null);
            view.setAnimation(null);
            view.setContentDescription(null);
            view.setTag(null);
        } catch (Exception ignored) {
        }
    }
}
