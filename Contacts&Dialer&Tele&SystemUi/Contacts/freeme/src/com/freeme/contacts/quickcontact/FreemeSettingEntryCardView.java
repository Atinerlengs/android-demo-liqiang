package com.freeme.contacts.quickcontact;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.CallUtil;
import com.android.contacts.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FreemeSettingEntryCardView extends FrameLayout {

    private LinkedHashMap<Integer, SettingsEntryView> mSettingViews = new LinkedHashMap<>();
    private LinkedHashMap<Integer, SettingsEntry> mSettingEntries = new LinkedHashMap<>();
    private LinearLayout mEntriesViewGroup;
    private int mDividerLineHeightPixels;

    public FreemeSettingEntryCardView(@NonNull Context context) {
        this(context, null);
    }

    public FreemeSettingEntryCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = LayoutInflater.from(context);
        View settingEntryCardView = inflater.inflate(R.layout.freeme_quick_contact_settings, this);
        mEntriesViewGroup = settingEntryCardView.findViewById(R.id.content_area_linear_layout);
        mDividerLineHeightPixels = getResources().getDimensionPixelSize(R.dimen.divider_line_height);
    }

    public void initialize(LinkedHashMap<Integer, SettingsEntry> settingEntries) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        mSettingEntries.clear();
        mSettingEntries = new LinkedHashMap<>(settingEntries);
        insertEntriesIntoViewGroup(layoutInflater);
    }

    private void insertEntriesIntoViewGroup(LayoutInflater layoutInflater) {
        mEntriesViewGroup.removeAllViews();
        mSettingViews.clear();
        int size = mSettingEntries.size();
        int index = 0;
        for (SettingsEntry entry : mSettingEntries.values()) {
            SettingsEntryView view = createEntryView(layoutInflater, entry, size, index++);
            mEntriesViewGroup.addView(view);
            mSettingViews.put(entry.getId(), view);
        }
    }

    private SettingsEntryView createEntryView(LayoutInflater layoutInflater,
                                              final SettingsEntry entry, int size, int position) {
        final SettingsEntryView entryView = (SettingsEntryView) layoutInflater.inflate(R.layout.freeme_quick_contact_settings_item,
                this, false);

        entryView.setOnClickListener(entry.getOnClickListener());
        entryView.setTag(entry.getId());

        String title = entry.getHeader();
        TextView header = entryView.getHeaderTv();
        View bottomLine = entryView.getBottomLine();
        if (!TextUtils.isEmpty(title)) {
            header.setText(title);
        }

        String subTitle = entry.getSubHeader();
        if (!TextUtils.isEmpty(subTitle)) {
            TextView subHeader = entryView.getSubTv();
            subHeader.setText(subTitle);
            subHeader.setVisibility(VISIBLE);
        }
        if (size == 1) {//full
            entryView.setBackgroundResource(R.drawable.freeme_content_full_bg_selector);
            bottomLine.setVisibility(View.INVISIBLE);
        } else {
            if (position == 0) {//top
                entryView.setBackgroundResource(R.drawable.freeme_content_top_bg_selector);
            } else if (position == size - 1) {//bottom
                entryView.setBackgroundResource(R.drawable.freeme_content_bottom_bg_selector);
                bottomLine.setVisibility(View.INVISIBLE);
            } else {//center
                entryView.setBackgroundResource(R.drawable.freeme_content_center_bg_selector);
            }
        }

        return entryView;
    }

    private View generateSeparator() {
        View separator = new View(getContext());
        Resources res = getResources();

        separator.setBackgroundColor(res.getColor(
                R.color.freeme_list_divider_color));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, mDividerLineHeightPixels);
        // The separator is aligned with the text in the entry. This is offset by a default
        // margin. If there is an icon present, the icon's width and margin are added
        int marginStart = res.getDimensionPixelSize(
                R.dimen.expanding_entry_card_item_padding_start);

        layoutParams.setMarginStart(marginStart);
        separator.setLayoutParams(layoutParams);
        return separator;
    }

    public void updateEntryView(int id, String header, String sub) {
        SettingsEntryView entryView = mSettingViews.get(id);
        if (entryView != null) {
            if (!TextUtils.isEmpty(header)) {
                entryView.getHeaderTv().setText(header);
            }
            if (!TextUtils.isEmpty(sub)) {
                TextView subTv = entryView.getSubTv();
                subTv.setVisibility(VISIBLE);
                subTv.setText(sub);
            }
        }
    }

    public void displayOrHideEntryView(int id) {
        SettingsEntryView entryView = mSettingViews.get(id);
        if (entryView != null) {
            entryView.setVisibility(videoCallEnable() ? View.VISIBLE : View.GONE);
        }
    }

    public boolean videoCallEnable() {
        int videoCapability = CallUtil.getVideoCallingAvailability(getContext());
        boolean isVideoEnabled = (videoCapability & CallUtil.VIDEO_CALLING_ENABLED) != 0;
        return isVideoEnabled;
    }

    public static final class SettingsEntryView extends LinearLayout {
        private TextView mHeaderTv;
        private TextView mSubTv;
        private View mBottomView;

        public SettingsEntryView(Context context) {
            super(context);
        }

        public SettingsEntryView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            mHeaderTv = (TextView) findViewById(R.id.header);
            mSubTv = (TextView) findViewById(R.id.sub_header);
            mBottomView = findViewById(R.id.contact_settings_bottom_line);
        }

        public TextView getHeaderTv() {
            return mHeaderTv;
        }

        public TextView getSubTv() {
            return mSubTv;
        }

        public View getBottomLine(){
            return mBottomView;
        }
    }

    public static final class SettingsEntry {
        private int mId;
        private String mHeader;
        private String mSubHeader;
        private OnClickListener mOnClickListener;

        public SettingsEntry(int id, String header, String subHeader, OnClickListener onClickListener) {
            mId = id;
            mHeader = header;
            mSubHeader = subHeader;
            mOnClickListener = onClickListener;
        }

        public int getId() {
            return mId;
        }

        public void setId(int id) {
            mId = id;
        }

        public String getHeader() {
            return mHeader;
        }

        public void setHeader(String header) {
            mHeader = header;
        }

        public String getSubHeader() {
            return mSubHeader;
        }

        public void setSubHeader(String subHeader) {
            mSubHeader = subHeader;
        }

        public OnClickListener getOnClickListener() {
            return mOnClickListener;
        }

        public void setOnClickListener(OnClickListener onClickListener) {
            mOnClickListener = onClickListener;
        }
    }
}
