package com.mediatek.services.telephony;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * RoamingSwitchPreference accumulates UI to show roaming prefernce.
 */
public class RoamingSwitchPreference extends SwitchPreference {


    /**
     * RoamingSwitchPreference accumulates UI to show roaming prefernce.
     * @param context context of UI
     * @param attrs UI attributes
     */
    public RoamingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    protected void onBindView(View view) {
        super.onBindView(view);
        adjustViews(view);
    }

    protected void adjustViews(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                 adjustViews(group.getChildAt(index));
            }
        } else if (view instanceof TextView) {
            // adjust your textViews here
            TextView t = (TextView) view;
            t.setSingleLine(false);
            t.setEllipsize(null);
        }
    }
}
