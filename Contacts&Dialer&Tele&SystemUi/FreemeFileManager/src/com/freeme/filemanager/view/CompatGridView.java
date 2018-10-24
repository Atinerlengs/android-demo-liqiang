package com.freeme.filemanager.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.GridView;

public class CompatGridView extends GridView {

    public CompatGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompatGridView(Context context) {
        super(context);
    }

    public CompatGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
              MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
            //forbidden gridview scroll
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
}