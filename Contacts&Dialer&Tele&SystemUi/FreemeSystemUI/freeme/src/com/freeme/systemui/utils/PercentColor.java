package com.freeme.systemui.utils;

import android.graphics.Color;

public class PercentColor {
    int mColor;
    int mLevel;

    public PercentColor(int level, int color) {
        mLevel = level;
        mColor = color;
    }

    public int getLevel() {
        return mLevel;
    }

    public int getColor() {
        return mColor;
    }

    public static PercentColor createFromColorStr(String colorStr) {
        String[] items = colorStr.split(":");
        if (items == null || items.length < 2) {
            return null;
        }
        return new PercentColor(Integer.parseInt(items[0]), Color.parseColor(items[1]));
    }

    public static PercentColor[] createFromColorStrs(String colorStrs) {
        String[] items = colorStrs.split(";");
        PercentColor[] colors = new PercentColor[items.length];
        for (int i = 0; i < items.length; i++) {
            colors[i] = createFromColorStr(items[i]);
        }
        return colors;
    }

    public static int getColor(PercentColor[] colors, int level) {
        for (int i = 0; i < colors.length; i++) {
            if (colors[i].getLevel() >= level) {
                return colors[i].getColor();
            }
        }
        return colors[colors.length - 1].getColor();
    }
}
