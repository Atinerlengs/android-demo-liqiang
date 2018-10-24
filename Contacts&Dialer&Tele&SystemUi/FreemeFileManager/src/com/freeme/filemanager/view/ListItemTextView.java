package com.freeme.filemanager.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.freeme.filemanager.R;

public class ListItemTextView extends RelativeLayout {
    private int mBackground_res;
    private int mLeft_text;
    private int mRight_text;
    private int mRight_img;
    private int mRight_tx_visible;
    private int mLeft_image_visible;
    private int mRight_image_visible;

    private RelativeLayout mListeItem;
    private TextView mLeftTv;
    private TextView mRightTv;
    private ProgressBar mRightProgressBar;
    private ImageView mLeftImageView;
    private ImageView mRightImageView;

    public ListItemTextView(Context context) {
        super(context);
    }

    public ListItemTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.list_item_textview, this);

        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.ListItemTextView);
        mBackground_res = typeArray.getResourceId(R.styleable.ListItemTextView_background_res, R.drawable.grid_item_line1_background);

        mLeft_text = typeArray.getResourceId(R.styleable.ListItemTextView_left_text, R.string.app_name);
        mRight_text = typeArray.getResourceId(R.styleable.ListItemTextView_right_text, R.string.app_name);
        mRight_img = typeArray.getResourceId(R.styleable.ListItemTextView_right_image, R.drawable.ic_right_arrow);
        mRight_tx_visible = typeArray.getInt(R.styleable.ListItemTextView_right_text_visible, GONE);
        mLeft_image_visible = typeArray.getInt(R.styleable.ListItemTextView_left_image_visible, GONE);
        mRight_image_visible = typeArray.getInt(R.styleable.ListItemTextView_right_image_visible, VISIBLE);
        typeArray.recycle();

        mListeItem = (RelativeLayout) findViewById(R.id.list_item);
        mLeftTv = (TextView) findViewById(R.id.left_tv);
        mRightTv = (TextView) findViewById(R.id.right_tv);
        mRightProgressBar = (ProgressBar) findViewById(R.id.right_group_progress);
        mLeftImageView = (ImageView) findViewById(R.id.left_img);
        mRightImageView = (ImageView) findViewById(R.id.right_img);

        initListItem();
    }

    private void initListItem() {
        mListeItem.setBackgroundResource(mBackground_res);
        mLeftImageView.setVisibility(mLeft_image_visible);
        mLeftTv.setText(mLeft_text);
        mRightTv.setText(mRight_text);
        mRightImageView.setImageResource(mRight_img);
        mRightTv.setVisibility(mRight_tx_visible);
        mRightImageView.setVisibility(mRight_image_visible);
    }

    public void setRightTvListener(OnClickListener listener) {
        mRightTv.setOnClickListener(listener);
    }

    public void setRightTvVisible(int visible) {
        mRightTv.setVisibility(visible);
    }

    public void setProgressBarVisible(int visible) {
        mRightProgressBar.setVisibility(visible);
    }

    public void refreshRightTv(int stringId, boolean enable) {
        if (enable) {
            mRightTv.setTextColor(this.getResources().getColor(R.color.app_theme_color_accent));
        } else {
            mRightTv.setTextColor(this.getResources().getColor(R.color.textColorSecondary));
        }
        mRightTv.setText(stringId);
        mRightTv.setEnabled(enable);
        mRightTv.setVisibility(VISIBLE);
    }
}
