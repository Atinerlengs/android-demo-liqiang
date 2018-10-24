package com.freeme.filemanager.view.memoryprogress;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.freeme.filemanager.R;

public class MemoryProgressLayout extends LinearLayout {
    private LinearLayout storageLayout;

    public MemoryProgressLayout(Context context) {
        super(context);
    }

    public MemoryProgressLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.storageLayout = (LinearLayout) findViewById(R.id.phnoe_memory_progress_bar_layout);
    }
}
