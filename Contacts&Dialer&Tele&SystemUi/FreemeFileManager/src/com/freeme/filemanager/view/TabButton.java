package com.freeme.filemanager.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Button;

import com.freeme.filemanager.R;

public class TabButton extends Button {
    private int normal_bg_res;
    private int selected_bg_res;

    public TabButton(Context context) {
        super(context);
    }

    public TabButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.TabButton);
        normal_bg_res = typeArray.getResourceId(R.styleable.TabButton_normal_bg_res, 0);
        selected_bg_res = typeArray.getResourceId(R.styleable.TabButton_selected_bg_res, 0);

        typeArray.recycle();
    }

    public void setSelected(boolean selected) {
        if (selected) {
            setBackgroundResource(selected_bg_res);
            setTextColor(mContext.getResources().getColor(R.color.white));
        } else {
            setBackgroundResource(normal_bg_res);
            setTextColor(mContext.getResources().getColor(R.color.textColorSecondary));
        }
    }
}
