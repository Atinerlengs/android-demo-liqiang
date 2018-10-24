package com.freeme.onehand.settings;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.freeme.onehand.R;

public final class OneHandViewPagerAdapter extends PagerAdapter {
    private Context mContext;
    private LayoutInflater mInflater;

    private static final class Description {
        String key;
        int title;
        int summary;
        int animation;
    }
    private final ArrayList<Description> mDescriptions = new ArrayList<>();

    public OneHandViewPagerAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);

        setDescriptions();
    }

    @Override
    public int getCount() {
        return mDescriptions.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mInflater.inflate(R.layout.pager_adapter, null);
        view.setTag(position);

        TextView titleView = (TextView) view.findViewById(R.id.description_title);
        TextView summaryView = (TextView) view.findViewById(R.id.description_summary);
        ImageView animView = (ImageView) view.findViewById(R.id.animation);

        if (Utils.isRTL(mContext)) {
            position = (mDescriptions.size() - 1) - position;
        }
        final Description description = mDescriptions.get(position);
        if (description.title == 0) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setVisibility(View.VISIBLE);
            titleView.setText(description.title);
        }
        if (description.summary == 0) {
            summaryView.setVisibility(View.GONE);
        } else {
            summaryView.setVisibility(View.VISIBLE);
            summaryView.setText(description.summary);
        }
        if (description.animation == 0) {
            animView.setVisibility(View.GONE);
        } else {
            animView.setVisibility(View.VISIBLE);
            animView.setImageResource(description.animation);
            if (OneHandSettingsFragment.PREF_GESTURE_TYPE.equals(description.key)) {
                AnimationDrawable anim = (AnimationDrawable) animView.getDrawable();
                if (anim != null) {
                    anim.start();
                }
            }
        }

        container.addView(view, 0);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(Object object) {
        if (object == null) {
            return -1;
        }
        String key = (String) object;
        final int size = mDescriptions.size();
        for (int i = 0; i < size; i++) {
            if (key.equals(mDescriptions.get(i).key)) {
                if (Utils.isRTL(mContext)) {
                    return (size - 1) - i;
                }
                return i;
            }
        }
        return -1;
    }

    private void setDescriptions() {
        Description description = new Description();
        description.key = OneHandSettingsFragment.PREF_GESTURE_TYPE;
        description.animation = R.drawable.oho_help_gesture_animation;
        description.summary = R.string.onehand_settings_text_1;
        mDescriptions.add(description);

        if (OneHandSettingsFragment.HAS_TRIPLY_HOMEKEY) {
            description = new Description();
            description.key = OneHandSettingsFragment.PREF_BUTTON_TYPE;
            description.animation = R.drawable.oho_help_button_img;
            description.summary = OneHandSettingsFragment.HAS_NAVIGATION_BAR
                    ? R.string.onehand_settings_text_2
                    : R.string.onehand_settings_text_2_hardkey;
            mDescriptions.add(description);
        }
    }
}
