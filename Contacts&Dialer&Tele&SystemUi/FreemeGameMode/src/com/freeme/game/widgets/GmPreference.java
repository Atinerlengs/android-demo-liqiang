package com.freeme.game.widgets;

import android.content.Context;
import android.preference.Preference;
import android.view.View;

import com.freeme.game.R;

public class GmPreference extends Preference implements View.OnClickListener {

    public interface OnWidgetLayoutClickListener {
        void onClick(Preference pref);
    }

    private OnWidgetLayoutClickListener mListener;

    public GmPreference(Context context, OnWidgetLayoutClickListener listener) {
        super(context);
        mListener = listener;
        setWidgetLayoutResource(R.layout.gm_pref_widget_layout);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View remove = view.findViewById(R.id.remove);
        if (remove != null) {
            remove.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        if (mListener != null) {
            mListener.onClick(this);
        }
    }
}
