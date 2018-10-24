package com.freeme.dialer.dialpadview;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.animation.AnimUtils;
import com.android.dialer.dialpadview.DialpadKeyButton;
import com.android.dialer.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;


public class FreemeDialpadView extends LinearLayout {
    private static final String TAG = FreemeDialpadView.class.getSimpleName();

    private static final double DELAY_MULTIPLIER = 0.66;
    private static final double DURATION_MULTIPLIER = 0.8;
    // For animation.
    private static final int KEY_FRAME_DURATION = 33;
    /**
     * {@code True} if the dialpad is in landscape orientation.
     */
    private final boolean mIsLandscape;
    /**
     * {@code True} if the dialpad is showing in a right-to-left locale.
     */
    private final boolean mIsRtl;

    private final int[] mButtonIds =
            new int[]{
                    R.id.zero,
                    R.id.one,
                    R.id.two,
                    R.id.three,
                    R.id.four,
                    R.id.five,
                    R.id.six,
                    R.id.seven,
                    R.id.eight,
                    R.id.nine,
                    R.id.star,
                    R.id.pound
            };
    private EditText mDigits;
    private ImageButton mDelete;
    private ColorStateList mRippleColor;
    private ViewGroup mRateContainer;
    private TextView mIldCountry;
    private TextView mIldRate;
    private boolean mCanDigitsBeEdited;
    private int mTranslateDistance;
    private TextView mGeoLocation;
    private View mDigitsViews;

    public FreemeDialpadView(Context context) {
        this(context, null);
    }

    public FreemeDialpadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FreemeDialpadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Dialpad);
        mRippleColor = a.getColorStateList(R.styleable.Dialpad_dialpad_key_button_touch_tint);
        a.recycle();

        mTranslateDistance =
                getResources().getDimensionPixelSize(R.dimen.dialpad_key_button_translate_y);

        mIsLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        mIsRtl =
                TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;

        mDigitsViews = View.inflate(context, R.layout.freeme_dialpad_digits_container, null);
    }

    @Override
    protected void onFinishInflate() {
        setupKeypad();
        mDigits = mDigitsViews.findViewById(R.id.digits);
        mDelete = mDigitsViews.findViewById(R.id.deleteButton);
        mRateContainer = findViewById(R.id.rate_container);
        mIldCountry = mRateContainer.findViewById(R.id.ild_country);
        mIldRate = mRateContainer.findViewById(R.id.ild_rate);

        mGeoLocation = mDigitsViews.findViewById(R.id.geo_location);

        AccessibilityManager accessibilityManager =
                (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            // The text view must be selected to send accessibility events.
            mDigits.setSelected(true);
        }
    }

    private int[] numberIcons = new int[]{R.drawable.freeme_dial_number_0,
            R.drawable.freeme_dial_number_1, R.drawable.freeme_dial_number_2,
            R.drawable.freeme_dial_number_3, R.drawable.freeme_dial_number_4,
            R.drawable.freeme_dial_number_5, R.drawable.freeme_dial_number_6,
            R.drawable.freeme_dial_number_7, R.drawable.freeme_dial_number_8,
            R.drawable.freeme_dial_number_9, R.drawable.freeme_dial_number_star,
            R.drawable.freeme_dial_number_pound};

    public void setNumberIcons(int[] numberIcons) {
        this.numberIcons = numberIcons;
        setupKeypad();
    }

    private void setupKeypad() {
        DialpadKeyButton dialpadKey;
        ImageView numberView;

        for (int i = 0; i < mButtonIds.length; i++) {
            dialpadKey = findViewById(mButtonIds[i]);
            numberView = dialpadKey.findViewById(R.id.dialpad_key_number);
            numberView.setBackgroundResource(numberIcons[i]);

            final RippleDrawable rippleBackground =
                    (RippleDrawable) getDrawableCompat(getContext(), R.drawable.btn_dialpad_key);
            if (mRippleColor != null) {
                rippleBackground.setColor(mRippleColor);
            }
            dialpadKey.setBackground(rippleBackground);
        }
    }

    private Drawable getDrawableCompat(Context context, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(id);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    /**
     * Whether or not the digits above the dialer can be edited.
     *
     * @param canBeEdited If true, the backspace button will be shown and the digits EditText will be
     *                    configured to allow text manipulation.
     */
    public void setCanDigitsBeEdited(boolean canBeEdited) {
        View deleteButton = mDigitsViews.findViewById(R.id.deleteButton);
        deleteButton.setVisibility(canBeEdited ? View.VISIBLE : View.INVISIBLE);

        EditText digits = mDigitsViews.findViewById(R.id.digits);
        digits.setClickable(canBeEdited);
        digits.setLongClickable(canBeEdited);
        digits.setFocusableInTouchMode(canBeEdited);
        digits.setCursorVisible(false);

        mCanDigitsBeEdited = canBeEdited;
    }

    public void setCallRateInformation(String countryName, String displayRate) {
        if (TextUtils.isEmpty(countryName) && TextUtils.isEmpty(displayRate)) {
            mRateContainer.setVisibility(View.GONE);
            return;
        }
        mRateContainer.setVisibility(View.VISIBLE);
        mIldCountry.setText(countryName);
        mIldRate.setText(displayRate);
    }

    public boolean canDigitsBeEdited() {
        return mCanDigitsBeEdited;
    }

    /**
     * Always returns true for onHoverEvent callbacks, to fix problems with accessibility due to the
     * dialpad overlaying other fragments.
     */
    @Override
    public boolean onHoverEvent(MotionEvent event) {
        return true;
    }

    public void animateShow() {
        // This is a hack; without this, the setTranslationY is delayed in being applied, and the
        // numbers appear at their original position (0) momentarily before animating.
        final AnimatorListenerAdapter showListener = new AnimatorListenerAdapter() {
        };

        for (int i = 0; i < mButtonIds.length; i++) {
            int delay = (int) (getKeyButtonAnimationDelay(mButtonIds[i]) * DELAY_MULTIPLIER);
            int duration = (int) (getKeyButtonAnimationDuration(mButtonIds[i]) * DURATION_MULTIPLIER);
            final DialpadKeyButton dialpadKey = (DialpadKeyButton) findViewById(mButtonIds[i]);

            ViewPropertyAnimator animator = dialpadKey.animate();
            // Portrait orientation requires translation along the Y axis.
            dialpadKey.setTranslationY(mTranslateDistance);
            animator.translationY(0);

            animator.setInterpolator(AnimUtils.EASE_OUT_EASE_IN)
                    .setStartDelay(delay)
                    .setDuration(duration)
                    .setListener(showListener)
                    .start();
        }
    }

    public EditText getDigits() {
        return mDigits;
    }

    public ImageButton getDeleteButton() {
        return mDelete;
    }

    /**
     * Get the animation delay for the buttons, taking into account whether the dialpad is in
     * landscape left-to-right, landscape right-to-left, or portrait.
     *
     * @param buttonId The button ID.
     * @return The animation delay.
     */
    private int getKeyButtonAnimationDelay(int buttonId) {
        if (buttonId == R.id.one) {
            return KEY_FRAME_DURATION * 1;
        } else if (buttonId == R.id.two) {
            return KEY_FRAME_DURATION * 2;
        } else if (buttonId == R.id.three) {
            return KEY_FRAME_DURATION * 3;
        } else if (buttonId == R.id.four) {
            return KEY_FRAME_DURATION * 4;
        } else if (buttonId == R.id.five) {
            return KEY_FRAME_DURATION * 5;
        } else if (buttonId == R.id.six) {
            return KEY_FRAME_DURATION * 6;
        } else if (buttonId == R.id.seven) {
            return KEY_FRAME_DURATION * 7;
        } else if (buttonId == R.id.eight) {
            return KEY_FRAME_DURATION * 8;
        } else if (buttonId == R.id.nine) {
            return KEY_FRAME_DURATION * 9;
        } else if (buttonId == R.id.star) {
            return KEY_FRAME_DURATION * 10;
        } else if (buttonId == R.id.zero || buttonId == R.id.pound) {
            return KEY_FRAME_DURATION * 11;
        }

        Log.wtf(TAG, "Attempted to get animation delay for invalid key button id.");
        return 0;
    }

    /**
     * Get the button animation duration, taking into account whether the dialpad is in landscape
     * left-to-right, landscape right-to-left, or portrait.
     *
     * @param buttonId The button ID.
     * @return The animation duration.
     */
    private int getKeyButtonAnimationDuration(int buttonId) {
        if (buttonId == R.id.one
                || buttonId == R.id.two
                || buttonId == R.id.three
                || buttonId == R.id.four
                || buttonId == R.id.five
                || buttonId == R.id.six) {
            return KEY_FRAME_DURATION * 10;
        } else if (buttonId == R.id.seven || buttonId == R.id.eight || buttonId == R.id.nine) {
            return KEY_FRAME_DURATION * 9;
        } else if (buttonId == R.id.star || buttonId == R.id.zero || buttonId == R.id.pound) {
            return KEY_FRAME_DURATION * 8;
        }
        Log.wtf(TAG, "Attempted to get animation duration for invalid key button id.");
        return 0;
    }

    public void setGeoLocation(String location) {
        if (location == null || location.trim().length() <= 0) {
            mGeoLocation.setVisibility(GONE);
        } else {
            mGeoLocation.setText(location);
            mGeoLocation.setVisibility(VISIBLE);
        }
    }

    public View getDigitsViews() {
        return mDigitsViews;
    }
}
