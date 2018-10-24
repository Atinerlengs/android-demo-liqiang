package com.freeme.filemanager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class CompatListView extends ListView {

    public CompatListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompatListView(Context context) {
        super(context);
    }

    public CompatListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
              MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}