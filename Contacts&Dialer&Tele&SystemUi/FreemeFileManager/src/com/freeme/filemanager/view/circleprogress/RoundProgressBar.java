package com.freeme.filemanager.view.circleprogress;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
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
import android.view.animation.AccelerateDecelerateInterpolator;

import com.freeme.filemanager.R;

public class RoundProgressBar extends View {
    private final int STEP_TXT0 = 0;
    private final int STEP_TXT1 = 25;
    private final int STEP_TXT2 = 50;
    private final int STEP_TXT3 = 75;
    private final int STEP_TXT4 = 100;
    private final int STEP_COLOR0 = 70;
    private final int STEP_COLOR1 = 90;

    private Paint paint;
    protected Paint textPaint;

    private RectF rectF = new RectF();
    private RectF mCleanRectF = new RectF();

    private float strokeWidth;
    private float suffixTextSize;
    private float bottomTextSize;
    private String bottomText;
    private float bottomText2Size;
    private String bottomText2;
    private int bottomText2Color;
    private float textSize;
    private float mCleanTextSize;
    private float mCleanTextSizeLong;
    private int textColor;
    private int progress;
    private int max;
    private int finishedStrokeColor;
    private int unfinishedStrokeColor;
    private float arcAngle;
    private String suffixText = "%";
    private float suffixTextPadding;

    private float arcBottomHeight;

    private final int default_finished_color = Color.WHITE;
    private final int default_unfinished_color = Color.rgb(72, 106, 176);
    private final int default_text_color = Color.rgb(66, 145, 241);
    private final float default_suffix_text_size;
    private final float default_suffix_padding;
    private final float default_bottom_text_size;
    private final float default_stroke_width;
    private final String default_suffix_text;
    private final int default_max = 100;
    private final float default_arc_angle = 360 * 1.0f;
    private float default_text_size;
    private final int min_size;
    private Context mContext;
    private int[] allTypeProgress = {0, 0, 0, 0, 0, 0};

    private static final String INSTANCE_STATE = "saved_instance";
    private static final String INSTANCE_STROKE_WIDTH = "stroke_width";
    private static final String INSTANCE_SUFFIX_TEXT_SIZE = "suffix_text_size";
    private static final String INSTANCE_SUFFIX_TEXT_PADDING = "suffix_text_padding";
    private static final String INSTANCE_BOTTOM_TEXT_SIZE = "bottom_text_size";
    private static final String INSTANCE_BOTTOM_TEXT = "bottom_text";
    private static final String INSTANCE_BOTTOM_TEXT2_SIZE = "bottom_text2_size";
    private static final String INSTANCE_BOTTOM_TEXT2 = "bottom_text2";
    private static final String INSTANCE_TEXT_SIZE = "text_size";
    private static final String INSTANCE_TEXT_COLOR = "text_color";
    private static final String INSTANCE_PROGRESS = "progress";
    private static final String INSTANCE_MAX = "max";
    private static final String INSTANCE_FINISHED_STROKE_COLOR = "finished_stroke_color";
    private static final String INSTANCE_UNFINISHED_STROKE_COLOR = "unfinished_stroke_color";
    private static final String INSTANCE_ARC_ANGLE = "arc_angle";
    private static final String INSTANCE_SUFFIX = "suffix";
    public static final int  DURATION = 700;
    private boolean mOnlyColor;

    private String mCenterText;

    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        min_size = (int) Utils.dp2px(getResources(), 100);
        default_text_size = Utils.sp2px(getResources(), 63);
        default_suffix_text_size = Utils.sp2px(getResources(), 63);
        default_suffix_padding = Utils.dp2px(getResources(), 4);
        default_suffix_text = "%";
        default_bottom_text_size = Utils.sp2px(getResources(), 10);
        default_stroke_width = Utils.dp2px(getResources(), 6);

        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RoundProgressBar, defStyleAttr, 0);
        initByAttributes(attributes);
        attributes.recycle();

        initPainters();
    }

    protected void initByAttributes(TypedArray attributes) {
        finishedStrokeColor = attributes.getColor(R.styleable.RoundProgressBar_arc_finished_color, default_finished_color);
        unfinishedStrokeColor = attributes.getColor(R.styleable.RoundProgressBar_arc_unfinished_color, default_unfinished_color);

        textColor = attributes.getColor(R.styleable.RoundProgressBar_arc_text_color, default_text_color);
        textSize = attributes.getDimension(R.styleable.RoundProgressBar_arc_text_size, default_text_size);
        arcAngle = attributes.getFloat(R.styleable.RoundProgressBar_arc_angle, default_arc_angle);
        setMax(attributes.getInt(R.styleable.RoundProgressBar_arc_max, default_max));
        setProgress(attributes.getInt(R.styleable.RoundProgressBar_arc_progress, 0));
        strokeWidth = attributes.getDimension(R.styleable.RoundProgressBar_arc_stroke_width, default_stroke_width);
        suffixTextSize = attributes.getDimension(R.styleable.RoundProgressBar_arc_suffix_text_size, default_suffix_text_size);
        suffixText = TextUtils.isEmpty(attributes.getString(R.styleable.RoundProgressBar_arc_suffix_text)) ? default_suffix_text : attributes.getString(R.styleable.RoundProgressBar_arc_suffix_text);
        suffixTextPadding = attributes.getDimension(R.styleable.RoundProgressBar_arc_suffix_text_padding, default_suffix_padding);
        bottomTextSize = attributes.getDimension(R.styleable.RoundProgressBar_arc_bottom_text_size, default_bottom_text_size);
        bottomText = attributes.getString(R.styleable.RoundProgressBar_arc_bottom_text);
        bottomText2Color = attributes.getColor(R.styleable.RoundProgressBar_arc_text2_color, default_text_color);
        bottomText2Size = attributes.getDimension(R.styleable.RoundProgressBar_arc_bottom_text2_size, default_bottom_text_size);
        bottomText2 = attributes.getString(R.styleable.RoundProgressBar_arc_bottom_text2);
        mCleanTextSize = getResources().getDimensionPixelSize(R.dimen.arc_clean_text_size);
        mCleanTextSizeLong = getResources().getDimensionPixelSize(R.dimen.arc_clean_text_size_long);
    }

    protected void initPainters() {
        textPaint = new TextPaint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);

        paint = new Paint();
        paint.setColor(default_unfinished_color);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public void invalidate() {
        initPainters();
        super.invalidate();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
        this.invalidate();
    }

    public float getSuffixTextSize() {
        return suffixTextSize;
    }

    public void setSuffixTextSize(float suffixTextSize) {
        this.suffixTextSize = suffixTextSize;
        this.invalidate();
    }

    public String getBottomText() {
        return bottomText;
    }

    public void setBottomText(String bottomText) {
        this.bottomText = bottomText;
        this.invalidate();
    }

    public String getBottomText2() {
        return bottomText2;
    }

    public void setBottomText2(String bottomText) {
        this.bottomText2 = bottomText;
        this.invalidate();
    }

    public void setOnlyColor(boolean onlycolor) {
        mOnlyColor = onlycolor;
    }

    public void setCenterText(String centerText) {
        mCenterText = centerText;
        if (centerText.equals(getResources().getString(R.string.no_garbage_result))) {
            mCleanTextSize = mCleanTextSizeLong;
            invalidate();
        }
    }
    public String getCenterText() {
        return mCenterText;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        if (this.progress > getMax()) {
            this.progress %= getMax();
        }
        invalidate();
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        if (max > 0) {
            this.max = max;
            invalidate();
        }
    }

    public float getBottomTextSize() {
        return bottomTextSize;
    }

    public void setBottomTextSize(float bottomTextSize) {
        this.bottomTextSize = bottomTextSize;
        this.invalidate();
    }

    public float getBottomText2Size() {
        return bottomText2Size;
    }

    public void setBottomText2Size(float bottomTextSize) {
        this.bottomText2Size = bottomTextSize;
        this.invalidate();
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
        this.invalidate();
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        this.invalidate();
    }

    public int getFinishedStrokeColor() {
        return finishedStrokeColor;
    }

    public void setFinishedStrokeColor(int finishedStrokeColor) {
        this.finishedStrokeColor = finishedStrokeColor;
        this.invalidate();
    }

    public int getUnfinishedStrokeColor() {
        return unfinishedStrokeColor;
    }

    public void setUnfinishedStrokeColor(int unfinishedStrokeColor) {
        this.unfinishedStrokeColor = unfinishedStrokeColor;
        this.invalidate();
    }

    public float getArcAngle() {
        return arcAngle;
    }

    public void setArcAngle(float arcAngle) {
        this.arcAngle = arcAngle;
        this.invalidate();
    }

    public String getSuffixText() {
        return suffixText;
    }

    public void setSuffixText(String suffixText) {
        this.suffixText = suffixText;
        this.invalidate();
    }

    public float getSuffixTextPadding() {
        return suffixTextPadding;
    }

    public void setSuffixTextPadding(float suffixTextPadding) {
        this.suffixTextPadding = suffixTextPadding;
        this.invalidate();
    }

    public void setAllTypeProgress(int[] allTypeProgress) {
        this.allTypeProgress = allTypeProgress;
        this.invalidate();
    }

    public void doAnimation(int progress) {
        this.progress = progress;
        AnimatorSet animation = new AnimatorSet();
        ObjectAnimator progressAnimation = ObjectAnimator.ofInt(this,"progress", 0, progress);
        progressAnimation.setDuration(DURATION);
        progressAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.playTogether(progressAnimation);
        animation.start();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return min_size;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return min_size;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        rectF.set(strokeWidth / 2f, strokeWidth / 2f, width - strokeWidth / 2f, MeasureSpec.getSize(heightMeasureSpec) - strokeWidth / 2f);
        //5dp padding to background
        float padding = getResources().getDimension(R.dimen.server_info_margin_left);
        mCleanRectF.set(rectF.left + padding, rectF.top + padding, rectF.right - padding, rectF.bottom - padding);
        float radius = width / 2f;
        float angle = (360 - arcAngle) / 2f;
        arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float startAngle = 270 - arcAngle / 2f;

        float finishedStartAngle = startAngle;
        String centerText = null;
        if (mOnlyColor) {
            if (!TextUtils.isEmpty(getCenterText())) {
                centerText = getCenterText();
                textPaint.setTextSize(mCleanTextSize);
            } else {
                if (progress < STEP_TXT1) {
                    centerText = String.valueOf(STEP_TXT0);
                } else if (progress >= STEP_TXT1 && progress < STEP_TXT2) {
                    centerText = String.valueOf(STEP_TXT1);
                } else if (progress >= STEP_TXT2 && progress < STEP_TXT3) {
                    centerText = String.valueOf(STEP_TXT2);
                } else if (progress >= STEP_TXT3 && progress < STEP_TXT4) {
                    centerText = String.valueOf(STEP_TXT3);
                } else {
                    centerText = String.valueOf(STEP_TXT4);
                }
                centerText = centerText + suffixText;
                textPaint.setTextSize(textSize);
            }

            float finishedSweepAngle = progress / (float) getMax() * arcAngle;
            if (progress == 0) finishedStartAngle = 0.01f;
            paint.setColor(finishedStrokeColor);
            canvas.drawArc(mCleanRectF, finishedStartAngle, finishedSweepAngle, false, paint);

        } else  {
            centerText = String.valueOf(progress) + suffixText;
            float finishedSweepAngle = allTypeProgress[0] / (float) getMax() * arcAngle;
            if (allTypeProgress[0] > 0) {
                paint.setColor(mContext.getResources().getColor(R.color.grid_horizontal_music_color));
                canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);
            }

            finishedStartAngle = finishedStartAngle + finishedSweepAngle;
            finishedSweepAngle = allTypeProgress[1] / (float) getMax() * arcAngle;
            if (allTypeProgress[1] > 0) {
                paint.setColor(mContext.getResources().getColor(R.color.grid_horizontal_video_color));
                canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);
            }

            finishedStartAngle = finishedStartAngle + finishedSweepAngle;
            finishedSweepAngle = allTypeProgress[2] / (float) getMax() * arcAngle;
            if (allTypeProgress[2] > 0) {
                paint.setColor(mContext.getResources().getColor(R.color.grid_horizontal_image_color));
                canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);
            }
            finishedStartAngle = finishedStartAngle + finishedSweepAngle;
            finishedSweepAngle = allTypeProgress[3] / (float) getMax() * arcAngle;
            if (allTypeProgress[3] > 0) {
                paint.setColor(mContext.getResources().getColor(R.color.grid_horizontal_document_color));
                canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);
            }
            finishedStartAngle = finishedStartAngle + finishedSweepAngle;
            finishedSweepAngle = allTypeProgress[4] / (float) getMax() * arcAngle;
            if (allTypeProgress[4] > 0) {
                paint.setColor(mContext.getResources().getColor(R.color.grid_horizontal_apk_color));
                canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);
            }

            finishedStartAngle = finishedStartAngle + finishedSweepAngle;
            finishedSweepAngle = allTypeProgress[5] / (float) getMax() * arcAngle;
            if (allTypeProgress[5] > 0) {
                paint.setColor(mContext.getResources().getColor(R.color.grid_horizontal_other_color));
                canvas.drawArc(rectF, finishedStartAngle, finishedSweepAngle, false, paint);
            }
            textPaint.setTextSize(textSize);
        }

        if (arcBottomHeight == 0) {
            float radius = getWidth() / 2f;
            float angle = (360 - arcAngle) / 2f;
            arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
        }

        float textBaseline = mContext.getResources().getDimensionPixelSize(R.dimen.arc_progress_text_baseline);
        if (!TextUtils.isEmpty(centerText)) {
            if (!mOnlyColor) {
                if (progress > STEP_COLOR0 && progress <= STEP_COLOR1) {
                    textColor = mContext.getResources().getColor(R.color.arc_progress_text_color1);
                } else if (progress > STEP_COLOR1) {
                    textColor = mContext.getResources().getColor(R.color.arc_progress_text_color2);
                }
                textPaint.setColor(textColor);
            } else {
                textColor = mContext.getResources().getColor(R.color.app_theme_color_accent);
                textPaint.setColor(textColor);
            }
            canvas.drawText(centerText, (getWidth() - textPaint.measureText(centerText)) / 2.0f, textBaseline, textPaint);
        }

        /*if (!TextUtils.isEmpty(getBottomText())) {
            textPaint.setTextSize(bottomTextSize);
            float textHeight = textPaint.descent() + textPaint.ascent();
            float bottomTextBaseline = (getHeight() - textHeight) / 2 + textHeight * 2; // above text1 1/2 textheight.
            canvas.drawText(getBottomText(), (getWidth() - textPaint.measureText(getBottomText())) / 2.0f, bottomTextBaseline, textPaint);
        }*/

        if (!TextUtils.isEmpty(getBottomText2())) {
            textPaint.setColor(bottomText2Color);
            textPaint.setTextSize(bottomText2Size);
            if (mOnlyColor) {
                textBaseline = mContext.getResources().getDimensionPixelSize(R.dimen.arc_progress_text_baseline_top);
            }
            float bottomTextBaseline = textBaseline + (textPaint.descent() - textPaint.ascent())
                    + mContext.getResources().getDimensionPixelSize(R.dimen.arc_bottom_text2_top);
            canvas.drawText(getBottomText2(), (getWidth() - textPaint.measureText(getBottomText2())) / 2.0f, bottomTextBaseline, textPaint);

        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloat(INSTANCE_STROKE_WIDTH, getStrokeWidth());
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_SIZE, getSuffixTextSize());
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_PADDING, getSuffixTextPadding());
        bundle.putFloat(INSTANCE_BOTTOM_TEXT_SIZE, getBottomTextSize());
        bundle.putFloat(INSTANCE_BOTTOM_TEXT2_SIZE, getBottomText2Size());
        bundle.putString(INSTANCE_BOTTOM_TEXT, getBottomText());
        bundle.putString(INSTANCE_BOTTOM_TEXT2, getBottomText2());
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize());
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor());
        bundle.putInt(INSTANCE_PROGRESS, getProgress());
        bundle.putInt(INSTANCE_MAX, getMax());
        bundle.putInt(INSTANCE_FINISHED_STROKE_COLOR, getFinishedStrokeColor());
        bundle.putInt(INSTANCE_UNFINISHED_STROKE_COLOR, getUnfinishedStrokeColor());
        bundle.putFloat(INSTANCE_ARC_ANGLE, getArcAngle());
        bundle.putString(INSTANCE_SUFFIX, getSuffixText());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            strokeWidth = bundle.getFloat(INSTANCE_STROKE_WIDTH);
            suffixTextSize = bundle.getFloat(INSTANCE_SUFFIX_TEXT_SIZE);
            suffixTextPadding = bundle.getFloat(INSTANCE_SUFFIX_TEXT_PADDING);
            bottomTextSize = bundle.getFloat(INSTANCE_BOTTOM_TEXT_SIZE);
            bottomText2Size = bundle.getFloat(INSTANCE_BOTTOM_TEXT2_SIZE);
            bottomText = bundle.getString(INSTANCE_BOTTOM_TEXT);
            bottomText2 = bundle.getString(INSTANCE_BOTTOM_TEXT2);
            textSize = bundle.getFloat(INSTANCE_TEXT_SIZE);
            textColor = bundle.getInt(INSTANCE_TEXT_COLOR);
            setMax(bundle.getInt(INSTANCE_MAX));
            setProgress(bundle.getInt(INSTANCE_PROGRESS));
            finishedStrokeColor = bundle.getInt(INSTANCE_FINISHED_STROKE_COLOR);
            unfinishedStrokeColor = bundle.getInt(INSTANCE_UNFINISHED_STROKE_COLOR);
            suffixText = bundle.getString(INSTANCE_SUFFIX);
            initPainters();
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }
}
