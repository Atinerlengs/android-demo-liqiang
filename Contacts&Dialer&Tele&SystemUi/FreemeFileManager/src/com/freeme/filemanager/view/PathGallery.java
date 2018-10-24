package com.freeme.filemanager.view;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.freeme.filemanager.R;

public class PathGallery extends LinearLayout {
    private LayoutInflater mInflater = LayoutInflater.from(getContext());
    private String mCurrentPath;
    private IPathItemClickListener mPathItemClickListener;
    private ArrayList<Pair<String, String>> mPathSegments = new ArrayList();
    private int mPathStartIndex = 1;
    private View.OnClickListener pathItemClickListener = new View.OnClickListener() {
        public void onClick(View paramView) {
            String str = (String) paramView.getTag();
            if (PathGallery.this.mPathItemClickListener != null){
                PathGallery.this.mPathItemClickListener.onPathItemClickListener(str);
            }
        }
    };

    public PathGallery(Context context) {
        super(context, null);
    }

    public PathGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void addPathSegmentViews() {
        LinearLayout localLinearLayout = (LinearLayout) findViewById(R.id.scroll_container);
        localLinearLayout.removeAllViews();
        final HorizontalScrollView localHorizontalScrollView = (HorizontalScrollView) findViewById(R.id.path_scroll_view);
        if(mPathSegments.size() < mPathStartIndex){
            return;
        }
        for (int i = this.mPathStartIndex; i <this.mPathSegments.size(); ++i) {
            Pair localPair = (Pair) this.mPathSegments.get(i);

            LinearLayout view = (LinearLayout) this.mInflater.inflate(R.layout.layout_path_gallery_item, null, false);
            TextView localTextView = (TextView) view.findViewById(R.id.path_item);
            localTextView.setText((CharSequence) localPair.first);
            localTextView.setTag(localPair.second);
            localTextView.setOnClickListener(this.pathItemClickListener);
            localLinearLayout.addView(view);
            postDelayed(new Runnable() {
                public void run() {
                    if (localHorizontalScrollView != null){
                        localHorizontalScrollView.fullScroll(FOCUS_RIGHT);
                    }
                }
            }, 100L);
        }
    }

    private void initFirstPathView() {
        TextView localTextView = (TextView) findViewById(R.id.first_path);
        if ((this.mPathSegments.size() == 0) || (localTextView == null))
            return;
        Pair localPair = (Pair) this.mPathSegments.get(0);
        localTextView.setText((CharSequence) localPair.first);
        localTextView.setTag(localPair.second);
        localTextView.setOnClickListener(this.pathItemClickListener);
    }

    private void parsePathSegments() {
        if (!TextUtils.isEmpty(mCurrentPath)){
            this.mPathSegments.clear();
            
            int j;
            for(int i=0; i<mCurrentPath.length(); i=j+1){
                j = mCurrentPath.indexOf("/", i);
                if(j<0){
                    return;
                }else{
                    String str1 = mCurrentPath.substring(i, j);
                    String str2 = mCurrentPath.substring(0, j);
                    mPathSegments.add(new Pair(str1, str2));
                }
            }
        }
    }

    public void setPath(String paramString) {
        this.mCurrentPath = paramString+"/";
        parsePathSegments();
        initFirstPathView();
        addPathSegmentViews();
    }

    public void setPathItemClickListener(
            IPathItemClickListener paramIPathItemClickListener) {
        this.mPathItemClickListener = paramIPathItemClickListener;
    }

    public void setPathStartIndex(int paramInt) {
        this.mPathStartIndex = paramInt;
    }

    public static abstract interface IPathItemClickListener {
        public abstract void onPathItemClickListener(String paramString);
    }
}
