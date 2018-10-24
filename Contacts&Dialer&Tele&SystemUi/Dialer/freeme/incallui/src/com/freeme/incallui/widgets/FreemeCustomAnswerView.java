package com.freeme.incallui.widgets;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.incallui.R;

public class FreemeCustomAnswerView extends ViewGroup implements View.OnTouchListener {

    private int mActionBtnMargin = 0;
    private int mActionBtnSize = 0;
    private int mHandleViewSize = 0;
    private int mArrowHeight = 0;
    private int mArrowWidth = 0;
    private int mArrowAnimWidth = 0;
    private float mArrowAnimWidthStep = 0;
    private final int ANIMATION_LAYOUT_COUNT = 50;
    private final int ANIMATION_DURATION = 30;
    private final int ANIMATION_AFTER_DELAY = 750;
    private final float ALPHA_INIT = 0.6f;
    private final float ALPHA_STEP = ALPHA_INIT / ANIMATION_LAYOUT_COUNT;

    private ImageView mLeftArrow;
    private ImageView mRightArrow;
    private ImageView mHandleView;
    private ImageView mAnswerView;
    private ImageView mDeclineView;

    private float x, y;
    private boolean mArrowAnimationStart;
    private int mHandleViewLeftBound;
    private int mAnswerViewLeftBound;
    private int mDeclineViewLeftBound;

    public FreemeCustomAnswerView(Context context) {
        this(context, null);
    }

    public FreemeCustomAnswerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FreemeCustomAnswerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FreemeCustomAnswerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mActionBtnMargin = getResources().getDimensionPixelSize(R.dimen.freeme_incall_answer_padding_horizontal_3);
        mActionBtnSize = getResources().getDimensionPixelSize(R.dimen.freeme_incall_answer_widget_circle_size);
        mHandleViewSize = getResources().getDimensionPixelSize(R.dimen.freeme_incall_answer_controller_size);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        mLeftArrow = new ImageView(getContext());
        mLeftArrow.setLayoutParams(params);
        mLeftArrow.setImageResource(R.drawable.freeme_incall_glow_pad_left_arrow);
        addView(mLeftArrow);

        mRightArrow = new ImageView(getContext());
        mRightArrow.setLayoutParams(params);
        mRightArrow.setImageResource(R.drawable.freeme_incall_glow_pad_right_arrow);
        addView(mRightArrow);

        mArrowWidth = mRightArrow.getDrawable().getIntrinsicWidth();
        mArrowHeight = mRightArrow.getDrawable().getIntrinsicHeight();

        mHandleView = new ImageView(getContext());
        mHandleView.setLayoutParams(params);
        mHandleView.setImageResource(R.drawable.freeme_incall_glow_pad_handle);
        addView(mHandleView);

        mAnswerView = new ImageView(getContext());
        mAnswerView.setLayoutParams(params);
        mAnswerView.setImageResource(R.drawable.freeme_incall_glow_pad_answer);
        addView(mAnswerView);

        mDeclineView = new ImageView(getContext());
        mDeclineView.setLayoutParams(params);
        mDeclineView.setImageResource(R.drawable.freeme_incall_glow_pad_decline);
        addView(mDeclineView);

        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float ix = event.getX();
                float iy = event.getY();
                if (ix > mHandleViewLeftBound && ix < (mHandleViewLeftBound + mHandleViewSize)) {
                    mHandleView.setSelected(true);
                    stopAnimation();
                    mLeftArrow.setVisibility(INVISIBLE);
                    mRightArrow.setVisibility(INVISIBLE);
                    x = ix;
                    y = iy;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeMessages(WHAT_HANDLE);
                requestLayout();
                if (mAnswerView.isSelected()) {
                    if (mCallBack != null) {
                        mCallBack.onAnswer();
                    }
                } else if (mDeclineView.isSelected()) {
                    if (mCallBack != null) {
                        mCallBack.onDecline();
                    }
                } else {
                    reset();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mHandleView.isSelected()) {
                    break;
                }
                float cx = event.getX();
                float cy = event.getY();
                if (Math.abs(cy - y) > 50) {
                    mHandleView.setSelected(false);
                    handler.removeMessages(WHAT_HANDLE);
                    requestLayout();
                } else {
                    handler.sendMessage(handler.obtainMessage(
                            WHAT_HANDLE, (int) (cx - x), 0));
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getRight() - getLeft();
        mHandleViewLeftBound = w / 2 - mHandleViewSize / 2;
        mArrowAnimWidth = mHandleViewLeftBound - mActionBtnMargin - mActionBtnSize;
        mArrowAnimWidthStep = (mArrowAnimWidth * 1.0f) / ANIMATION_LAYOUT_COUNT;

        mHandleView.layout(mHandleViewLeftBound, 0,
                mHandleViewLeftBound + mHandleViewSize, mHandleViewSize);

        int top = (mHandleViewSize - mActionBtnSize) / 2;
        int bottom = top + mActionBtnSize;
        mDeclineViewLeftBound = mActionBtnMargin;
        mDeclineView.layout(mDeclineViewLeftBound, top, mDeclineViewLeftBound + mActionBtnSize, bottom);

        mAnswerViewLeftBound = w - mActionBtnMargin - mActionBtnSize;
        mAnswerView.layout(mAnswerViewLeftBound, top, mAnswerViewLeftBound + mActionBtnSize, bottom);

        if (!mArrowAnimationStart) {
            int at = (mHandleViewSize - mArrowHeight) / 2;
            if (isViewVisibility(mLeftArrow)) {
                int al = mHandleViewLeftBound - 2 * mArrowWidth;
                mLeftArrow.layout(al, at, al + mArrowWidth, at + mArrowHeight);
            }

            if (isViewVisibility(mRightArrow)) {
                int al = mHandleViewLeftBound + mHandleViewSize + mArrowWidth;
                mRightArrow.layout(al, at, al + mArrowWidth, at + mArrowHeight);
            }
        }
    }

    private boolean isViewVisibility(View view) {
        return view == null ? false : view.getVisibility() == View.VISIBLE;
    }

    public void startAnimation() {
        if (mArrowAnimationStart) {
            return;
        }
        mArrowAnimationStart = true;
        layoutCount++;
        handler.sendEmptyMessageDelayed(WHAT_ARROW, ANIMATION_DURATION);
    }

    public void stopAnimation() {
        mArrowAnimationStart = false;
        handler.removeMessages(WHAT_ARROW);
        layoutCount = 0;
        requestLayout();
    }

    private int layoutCount = 0;
    private final int WHAT_ARROW = 1111;
    private final int WHAT_HANDLE = 1112;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == WHAT_ARROW) {
                int count = layoutCount % ANIMATION_LAYOUT_COUNT;
                int step = (int) (count * mArrowAnimWidthStep);
                int at = (mHandleViewSize - mArrowHeight) / 2;
                if (isViewVisibility(mLeftArrow)) {
                    int al = mHandleViewLeftBound - mArrowWidth;
                    al = al - step;
                    mLeftArrow.layout(al, at, al + mArrowWidth, at + mArrowHeight);
                    mLeftArrow.setAlpha(ALPHA_INIT - count * ALPHA_STEP);
                }

                if (isViewVisibility(mRightArrow)) {
                    int al = mHandleViewLeftBound + mHandleViewSize;
                    al = al + step;
                    mRightArrow.layout(al, at, al + mArrowWidth, at + mArrowHeight);
                    mRightArrow.setAlpha(ALPHA_INIT - count * ALPHA_STEP);
                }
                layoutCount++;
                handler.sendEmptyMessageDelayed(WHAT_ARROW, count == (ANIMATION_LAYOUT_COUNT - 1)
                        ? ANIMATION_AFTER_DELAY : ANIMATION_DURATION);
            } else if (msg.what == WHAT_HANDLE) {
                int left = mHandleViewLeftBound + msg.arg1;
                int answerCenter = mAnswerViewLeftBound + mActionBtnSize / 2;
                int declineCenter = mDeclineViewLeftBound + mActionBtnSize / 2;
                if ((left + mHandleViewSize / 2) >= answerCenter) {
                    left = answerCenter - mHandleViewSize / 2;
                    mHandleView.layout(left, 0,
                            left + mHandleViewSize, mHandleViewSize);
                    mAnswerView.setSelected(true);
                    mHandleView.setVisibility(INVISIBLE);
                } else if ((left + mHandleViewSize / 2) <= declineCenter) {
                    left = declineCenter - mHandleViewSize / 2;
                    mHandleView.layout(left, 0,
                            left + mHandleViewSize, mHandleViewSize);
                    mDeclineView.setSelected(true);
                    mHandleView.setVisibility(INVISIBLE);
                } else {
                    mHandleView.setVisibility(VISIBLE);
                    mHandleView.layout(left, 0,
                            left + mHandleViewSize, mHandleViewSize);
                    mAnswerView.setSelected(false);
                    mDeclineView.setSelected(false);
                }
            }
        }
    };

    public void reset() {
        startAnimation();
        mRightArrow.setVisibility(VISIBLE);
        mLeftArrow.setVisibility(VISIBLE);
        mHandleView.setVisibility(VISIBLE);
        mHandleView.setSelected(false);
        mAnswerView.setSelected(false);
        mDeclineView.setSelected(false);
    }


    private IControllerCallBack mCallBack;

    public void setControllerCallBack(IControllerCallBack callBack) {
        this.mCallBack = callBack;
    }

    public interface IControllerCallBack {
        void onAnswer();

        void onDecline();
    }
}
