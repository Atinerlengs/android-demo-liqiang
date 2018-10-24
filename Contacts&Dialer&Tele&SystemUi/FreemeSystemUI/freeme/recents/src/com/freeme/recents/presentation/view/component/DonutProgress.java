package com.freeme.recents.presentation.view.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.android.systemui.R;
import com.freeme.recents.RecentsUtils;

public class DonutProgress extends View {

    private Paint mfinishedPaint;
    private Paint munfinishedPaint;
    private Paint minnerCirclePaint;

    protected Paint mtextPaint;
    protected Paint minnerBottomTextPaint;

    private RectF mfinishedOuterRect = new RectF();
    private RectF munfinishedOuterRect = new RectF();

    private int mattributeResourceId = 0;
    private boolean mshowText;
    private float mtextSize;
    private int mtextColor;
    private int minnerBottomTextColor;
    private float mprogress = 0;
    private int mmax;
    private int mfinishedStrokeColor;
    private int munfinishedStrokeColor;
    private int munfinishedStrokeAlpha;
    private int mstartingDegree;
    private float mfinishedStrokeWidth;
    private float munfinishedStrokeWidth;
    private int minnerBackgroundColor;
    private String mprefixText = "";
    private String msuffixText = "%";
    private String mtext = null;
    private float minnerBottomTextSize;
    private String minnerBottomText;
    private float minnerBottomTextHeight;

    private final float default_stroke_width;
    private final int default_finished_color = Color.rgb(66, 145, 241);
    private final int default_unfinished_color = Color.rgb(204, 204, 204);
    private final int default_text_color = Color.rgb(66, 145, 241);
    private final int default_inner_bottom_text_color = Color.rgb(66, 145, 241);
    private final int default_inner_background_color = Color.TRANSPARENT;
    private final int default_max = 100;
    private final int default_startingDegree = 270;
    private final int default_unfinished_alpha = 58;
    private final float default_text_size;
    private final float default_inner_bottom_text_size;
    private final int min_size;


    private static final String INSTANCE_STATE = "saved_instance";
    private static final String INSTANCE_TEXT_COLOR = "text_color";
    private static final String INSTANCE_TEXT_SIZE = "text_size";
    private static final String INSTANCE_TEXT = "text";
    private static final String INSTANCE_INNER_BOTTOM_TEXT_SIZE = "inner_bottom_text_size";
    private static final String INSTANCE_INNER_BOTTOM_TEXT = "inner_bottom_text";
    private static final String INSTANCE_INNER_BOTTOM_TEXT_COLOR = "inner_bottom_text_color";
    private static final String INSTANCE_FINISHED_STROKE_COLOR = "finished_stroke_color";
    private static final String INSTANCE_UNFINISHED_STROKE_COLOR = "unfinished_stroke_color";
    private static final String INSTANCE_MAX = "max";
    private static final String INSTANCE_PROGRESS = "progress";
    private static final String INSTANCE_SUFFIX = "suffix";
    private static final String INSTANCE_PREFIX = "prefix";
    private static final String INSTANCE_FINISHED_STROKE_WIDTH = "finished_stroke_width";
    private static final String INSTANCE_UNFINISHED_STROKE_WIDTH = "unfinished_stroke_width";
    private static final String INSTANCE_BACKGROUND_COLOR = "inner_background_color";
    private static final String INSTANCE_STARTING_DEGREE = "starting_degree";
    private static final String INSTANCE_INNER_DRAWABLE = "inner_drawable";

    public DonutProgress(Context context) {
        this(context, null);
    }

    public DonutProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DonutProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        default_text_size = RecentsUtils.sp2px(getResources(), 18);
        min_size = (int) RecentsUtils.dp2px(getResources(), 100);
        default_stroke_width = RecentsUtils.dp2px(getResources(), 10);
        default_inner_bottom_text_size = RecentsUtils.sp2px(getResources(), 18);

        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DonutProgress, defStyleAttr, 0);
        initByAttributes(attributes);
        attributes.recycle();

        initPainters();
    }

    protected void initPainters() {
        if (mshowText) {
            mtextPaint = new TextPaint();
            mtextPaint.setColor(mtextColor);
            mtextPaint.setTextSize(mtextSize);
            mtextPaint.setAntiAlias(true);

            minnerBottomTextPaint = new TextPaint();
            minnerBottomTextPaint.setColor(minnerBottomTextColor);
            minnerBottomTextPaint.setTextSize(minnerBottomTextSize);
            minnerBottomTextPaint.setAntiAlias(true);
        }

        mfinishedPaint = new Paint();
        mfinishedPaint.setColor(mfinishedStrokeColor);
        mfinishedPaint.setStyle(Paint.Style.STROKE);
        mfinishedPaint.setAntiAlias(true);
        mfinishedPaint.setStrokeWidth(mfinishedStrokeWidth);

        munfinishedPaint = new Paint();
        munfinishedPaint.setColor(munfinishedStrokeColor);
        munfinishedPaint.setStyle(Paint.Style.STROKE);
        munfinishedPaint.setAntiAlias(true);
        munfinishedPaint.setAlpha (munfinishedStrokeAlpha);
        munfinishedPaint.setStrokeWidth(munfinishedStrokeWidth);

        minnerCirclePaint = new Paint();
        minnerCirclePaint.setColor(minnerBackgroundColor);
        minnerCirclePaint.setAntiAlias(true);
    }

    protected void initByAttributes(TypedArray attributes) {
        mfinishedStrokeColor = attributes.getColor(R.styleable.DonutProgress_donut_finished_color, default_finished_color);
        munfinishedStrokeAlpha = attributes.getColor(R.styleable.DonutProgress_donut_unfinished_alpha, default_unfinished_alpha);
        munfinishedStrokeColor = attributes.getColor(R.styleable.DonutProgress_donut_unfinished_color, default_unfinished_color);
        mshowText = attributes.getBoolean(R.styleable.DonutProgress_donut_show_text, true);
        mattributeResourceId = attributes.getResourceId(R.styleable.DonutProgress_donut_inner_drawable, 0);

        setMax(attributes.getInt(R.styleable.DonutProgress_donut_max, default_max));
        setProgress(attributes.getFloat(R.styleable.DonutProgress_donut_progress, 0));
        mfinishedStrokeWidth = attributes.getDimension(R.styleable.DonutProgress_donut_finished_stroke_width, default_stroke_width);
        munfinishedStrokeWidth = attributes.getDimension(R.styleable.DonutProgress_donut_unfinished_stroke_width, default_stroke_width);

        if (mshowText) {
            if (attributes.getString(R.styleable.DonutProgress_donut_prefix_text) != null) {
                mprefixText = attributes.getString(R.styleable.DonutProgress_donut_prefix_text);
            }
            if (attributes.getString(R.styleable.DonutProgress_donut_suffix_text) != null) {
                msuffixText = attributes.getString(R.styleable.DonutProgress_donut_suffix_text);
            }
            if (attributes.getString(R.styleable.DonutProgress_donut_text) != null) {
                mtext = attributes.getString(R.styleable.DonutProgress_donut_text);
            }

            mtextColor = attributes.getColor(R.styleable.DonutProgress_donut_text_color, default_text_color);
            mtextSize = attributes.getDimension(R.styleable.DonutProgress_donut_text_size, default_text_size);
            minnerBottomTextSize = attributes.getDimension(R.styleable.DonutProgress_donut_inner_bottom_text_size, default_inner_bottom_text_size);
            minnerBottomTextColor = attributes.getColor(R.styleable.DonutProgress_donut_inner_bottom_text_color, default_inner_bottom_text_color);
            minnerBottomText = attributes.getString(R.styleable.DonutProgress_donut_inner_bottom_text);
        }

        minnerBottomTextSize = attributes.getDimension(R.styleable.DonutProgress_donut_inner_bottom_text_size, default_inner_bottom_text_size);
        minnerBottomTextColor = attributes.getColor(R.styleable.DonutProgress_donut_inner_bottom_text_color, default_inner_bottom_text_color);
        minnerBottomText = attributes.getString(R.styleable.DonutProgress_donut_inner_bottom_text);

        mstartingDegree = attributes.getInt(R.styleable.DonutProgress_donut_circle_starting_degree, default_startingDegree);
        minnerBackgroundColor = attributes.getColor(R.styleable.DonutProgress_donut_background_color, default_inner_background_color);
    }

    @Override
    public void invalidate() {
        initPainters();
        super.invalidate();
    }

    public boolean isShowText() {
        return mshowText;
    }

    public void setShowText(boolean showText) {
        this.mshowText = showText;
    }

    public float getFinishedStrokeWidth() {
        return mfinishedStrokeWidth;
    }

    public void setFinishedStrokeWidth(float finishedStrokeWidth) {
        this.mfinishedStrokeWidth = finishedStrokeWidth;
        this.invalidate();
    }

    public float getUnfinishedStrokeWidth() {
        return munfinishedStrokeWidth;
    }

    public void setUnfinishedStrokeWidth(float unfinishedStrokeWidth) {
        this.munfinishedStrokeWidth = unfinishedStrokeWidth;
        this.invalidate();
    }

    private float getProgressAngle() {
        return getProgress() / (float) mmax * 360f;
    }

    public float getProgress() {
        return mprogress;
    }

    public void setProgress(float progress) {
        this.mprogress = progress;
        if (this.mprogress > getMax()) {
            this.mprogress %= getMax();
        }
        invalidate();
    }

    public int getMax() {
        return mmax;
    }

    public void setMax(int max) {
        if (max > 0) {
            this.mmax = max;
            invalidate();
        }
    }

    public float getTextSize() {
        return mtextSize;
    }

    public void setTextSize(float textSize) {
        this.mtextSize = textSize;
        this.invalidate();
    }

    public int getTextColor() {
        return mtextColor;
    }

    public void setTextColor(int textColor) {
        this.mtextColor = textColor;
        this.invalidate();
    }

    public int getFinishedStrokeColor() {
        return mfinishedStrokeColor;
    }

    public void setFinishedStrokeColor(int finishedStrokeColor) {
        this.mfinishedStrokeColor = finishedStrokeColor;
        this.invalidate();
    }

    public int getUnfinishedStrokeColor() {
        return munfinishedStrokeColor;
    }

    public void setUnfinishedStrokeColor(int unfinishedStrokeColor) {
        this.munfinishedStrokeColor = unfinishedStrokeColor;
        this.invalidate();
    }

    public String getText() {
        return mtext;
    }

    public void setText(String text) {
        this.mtext = text;
        this.invalidate();
    }

    public String getSuffixText() {
        return msuffixText;
    }

    public void setSuffixText(String suffixText) {
        this.msuffixText = suffixText;
        this.invalidate();
    }

    public String getPrefixText() {
        return mprefixText;
    }

    public void setPrefixText(String prefixText) {
        this.mprefixText = prefixText;
        this.invalidate();
    }

    public int getInnerBackgroundColor() {
        return minnerBackgroundColor;
    }

    public void setInnerBackgroundColor(int innerBackgroundColor) {
        this.minnerBackgroundColor = innerBackgroundColor;
        this.invalidate();
    }


    public String getInnerBottomText() {
        return minnerBottomText;
    }

    public void setInnerBottomText(String innerBottomText) {
        this.minnerBottomText = innerBottomText;
        this.invalidate();
    }


    public float getInnerBottomTextSize() {
        return minnerBottomTextSize;
    }

    public void setInnerBottomTextSize(float innerBottomTextSize) {
        this.minnerBottomTextSize = innerBottomTextSize;
        this.invalidate();
    }

    public int getInnerBottomTextColor() {
        return minnerBottomTextColor;
    }

    public void setInnerBottomTextColor(int innerBottomTextColor) {
        this.minnerBottomTextColor = innerBottomTextColor;
        this.invalidate();
    }

    public int getStartingDegree() {
        return mstartingDegree;
    }

    public void setStartingDegree(int startingDegree) {
        this.mstartingDegree = startingDegree;
        this.invalidate();
    }

    public int getAttributeResourceId() {
        return mattributeResourceId;
    }

    public void setAttributeResourceId(int attributeResourceId) {
        this.mattributeResourceId = attributeResourceId;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));

        //TODO calculate inner circle height and then position bottom text at the bottom (3/4)
        minnerBottomTextHeight = getHeight() - (getHeight() * 3) / 4;
    }

    private int measure(int measureSpec) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = min_size;
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.min(result, size);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float delta = Math.max(mfinishedStrokeWidth, munfinishedStrokeWidth);
        mfinishedOuterRect.set(delta,
                delta,
                getWidth() - delta,
                getHeight() - delta);
        munfinishedOuterRect.set(delta,
                delta,
                getWidth() - delta,
                getHeight() - delta);

        float innerCircleRadius = (getWidth() - Math.min(mfinishedStrokeWidth, munfinishedStrokeWidth) + Math.abs(mfinishedStrokeWidth - munfinishedStrokeWidth)) / 2f;
        canvas.drawCircle(getWidth() / 2.0f, getHeight() / 2.0f, innerCircleRadius, minnerCirclePaint);
        canvas.drawArc(mfinishedOuterRect, getStartingDegree(), getProgressAngle(), false, mfinishedPaint);
        canvas.drawArc(munfinishedOuterRect, getStartingDegree() + getProgressAngle(), 360 - getProgressAngle(), false, munfinishedPaint);

        if (mshowText) {
            String text = this.mtext != null ? this.mtext : mprefixText + mprogress + msuffixText;
            if (!TextUtils.isEmpty(text)) {
                float textHeight = mtextPaint.descent() + mtextPaint.ascent();
                canvas.drawText(text, (getWidth() - mtextPaint.measureText(text)) / 2.0f, (getWidth() - textHeight) / 2.0f, mtextPaint);
            }

            if (!TextUtils.isEmpty(getInnerBottomText())) {
                minnerBottomTextPaint.setTextSize(minnerBottomTextSize);
                float bottomTextBaseline = getHeight() - minnerBottomTextHeight - (mtextPaint.descent() + mtextPaint.ascent()) / 2;
                canvas.drawText(getInnerBottomText(), (getWidth() - minnerBottomTextPaint.measureText(getInnerBottomText())) / 2.0f, bottomTextBaseline, minnerBottomTextPaint);
            }
        }

        if (mattributeResourceId != 0) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mattributeResourceId);
            canvas.drawBitmap(bitmap, (getWidth() - bitmap.getWidth()) / 2.0f, (getHeight() - bitmap.getHeight()) / 2.0f, null);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor());
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize());
        bundle.putFloat(INSTANCE_INNER_BOTTOM_TEXT_SIZE, getInnerBottomTextSize());
        bundle.putFloat(INSTANCE_INNER_BOTTOM_TEXT_COLOR, getInnerBottomTextColor());
        bundle.putString(INSTANCE_INNER_BOTTOM_TEXT, getInnerBottomText());
        bundle.putInt(INSTANCE_INNER_BOTTOM_TEXT_COLOR, getInnerBottomTextColor());
        bundle.putInt(INSTANCE_FINISHED_STROKE_COLOR, getFinishedStrokeColor());
        bundle.putInt(INSTANCE_UNFINISHED_STROKE_COLOR, getUnfinishedStrokeColor());
        bundle.putInt(INSTANCE_MAX, getMax());
        bundle.putInt(INSTANCE_STARTING_DEGREE, getStartingDegree());
        bundle.putFloat(INSTANCE_PROGRESS, getProgress());
        bundle.putString(INSTANCE_SUFFIX, getSuffixText());
        bundle.putString(INSTANCE_PREFIX, getPrefixText());
        bundle.putString(INSTANCE_TEXT, getText());
        bundle.putFloat(INSTANCE_FINISHED_STROKE_WIDTH, getFinishedStrokeWidth());
        bundle.putFloat(INSTANCE_UNFINISHED_STROKE_WIDTH, getUnfinishedStrokeWidth());
        bundle.putInt(INSTANCE_BACKGROUND_COLOR, getInnerBackgroundColor());
        bundle.putInt(INSTANCE_INNER_DRAWABLE, getAttributeResourceId());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            mtextColor = bundle.getInt(INSTANCE_TEXT_COLOR);
            mtextSize = bundle.getFloat(INSTANCE_TEXT_SIZE);
            minnerBottomTextSize = bundle.getFloat(INSTANCE_INNER_BOTTOM_TEXT_SIZE);
            minnerBottomText = bundle.getString(INSTANCE_INNER_BOTTOM_TEXT);
            minnerBottomTextColor = bundle.getInt(INSTANCE_INNER_BOTTOM_TEXT_COLOR);
            mfinishedStrokeColor = bundle.getInt(INSTANCE_FINISHED_STROKE_COLOR);
            munfinishedStrokeColor = bundle.getInt(INSTANCE_UNFINISHED_STROKE_COLOR);
            mfinishedStrokeWidth = bundle.getFloat(INSTANCE_FINISHED_STROKE_WIDTH);
            munfinishedStrokeWidth = bundle.getFloat(INSTANCE_UNFINISHED_STROKE_WIDTH);
            minnerBackgroundColor = bundle.getInt(INSTANCE_BACKGROUND_COLOR);
            mattributeResourceId = bundle.getInt(INSTANCE_INNER_DRAWABLE);
            initPainters();
            setMax(bundle.getInt(INSTANCE_MAX));
            setStartingDegree(bundle.getInt(INSTANCE_STARTING_DEGREE));
            setProgress(bundle.getFloat(INSTANCE_PROGRESS));
            mprefixText = bundle.getString(INSTANCE_PREFIX);
            msuffixText = bundle.getString(INSTANCE_SUFFIX);
            mtext = bundle.getString(INSTANCE_TEXT);
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }
    public void setDonut_progress(String percent){
        if(!TextUtils.isEmpty(percent)){
            setProgress(Integer.parseInt(percent));
        }
    }
}
