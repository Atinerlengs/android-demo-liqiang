package com.android.settings.preference;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;

public class UnclickablePreference extends Preference {

    public UnclickablePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UnclickablePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_unclickable_layout);
    }

    @Override
    protected void onBindView(View view) {
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
        if (text1 != null && TextUtils.isEmpty(getSummary())) {
            text1.setText(getTitle());
        } else {
            if (text1 != null) {
                text1.setVisibility(View.GONE);
            }
            super.onBindView(view);
        }
    }
}
