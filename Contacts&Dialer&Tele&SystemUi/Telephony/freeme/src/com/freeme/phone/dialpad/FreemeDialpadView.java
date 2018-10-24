package com.freeme.phone.dialpad;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.phone.common.R;
import com.android.phone.common.animation.AnimUtils;
import com.android.phone.common.dialpad.DialpadKeyButton;

import java.util.Locale;

/**
 * View that displays a twelve-key phone dialpad.
 */
public class FreemeDialpadView extends LinearLayout {
    private static final String TAG = FreemeDialpadView.class.getSimpleName();

    private static final double DELAY_MULTIPLIER = 0.66;
    private static final double DURATION_MULTIPLIER = 0.8;

    /**
     * {@code True} if the dialpad is in landscape orientation.
     */
    private final boolean mIsLandscape;

    /**
     * {@code True} if the dialpad is showing in a right-to-left locale.
     */
    private final boolean mIsRtl;

    private EditText mDigits;
    private ImageButton mDelete;
    private View mOverflowMenuButton;
    private ColorStateList mRippleColor;

    private ViewGroup mRateContainer;
    private TextView mIldCountry;
    private TextView mIldRate;

    private boolean mCanDigitsBeEdited;

    private final int[] mButtonIds = new int[]{R.id.zero, R.id.one, R.id.two, R.id.three,
            R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star,
            R.id.pound, R.id.cancel, R.id.dial, R.id.deleteNumer};

    // For animation.
    private static final int KEY_FRAME_DURATION = 33;

    private int mTranslateDistance;

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

        mTranslateDistance = getResources().getDimensionPixelSize(
                R.dimen.dialpad_key_button_translate_y);

        mIsLandscape = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        mIsRtl = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                View.LAYOUT_DIRECTION_RTL;
    }

    @Override
    protected void onFinishInflate() {
        setupKeypad();
        mDigits = (EditText) findViewById(R.id.digits);
        mDelete = (ImageButton) findViewById(R.id.deleteButton);
        mOverflowMenuButton = findViewById(R.id.dialpad_overflow);
        mRateContainer = (ViewGroup) findViewById(R.id.rate_container);
        mIldCountry = (TextView) mRateContainer.findViewById(R.id.ild_country);
        mIldRate = (TextView) mRateContainer.findViewById(R.id.ild_rate);

        AccessibilityManager accessibilityManager = (AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            // The text view must be selected to send accessibility events.
            mDigits.setSelected(true);
        }
    }

    private void setupKeypad() {
        final int[] picIds = new int[]{
                R.drawable.freeme_dial_number_new_0,
                R.drawable.freeme_dial_number_new_1,
                R.drawable.freeme_dial_number_new_2,
                R.drawable.freeme_dial_number_new_3,
                R.drawable.freeme_dial_number_new_4,
                R.drawable.freeme_dial_number_new_5,
                R.drawable.freeme_dial_number_new_6,
                R.drawable.freeme_dial_number_new_7,
                R.drawable.freeme_dial_number_new_8,
                R.drawable.freeme_dial_number_new_9,
                R.drawable.freeme_dial_number_new_star,
                R.drawable.freeme_dial_number_new_pound,
                R.drawable.freeme_dial_delete,
                R.drawable.freeme_dial_new_dial,
                R.drawable.freeme_dial_delete,
        };

        final Resources resources = getContext().getResources();

        DialpadKeyButton dialpadKey;
        TextView picView;

        for (int i = 0; i < mButtonIds.length; i++) {
            dialpadKey = (DialpadKeyButton) findViewById(mButtonIds[i]);

            picView = dialpadKey.findViewById(R.id.dialpad_key_pic);

            final RippleDrawable rippleBackground = (RippleDrawable)
                    getDrawableCompat(getContext(), R.drawable.btn_dialpad_key);
            if (mRippleColor != null) {
                rippleBackground.setColor(mRippleColor);
            }

            dialpadKey.setBackground(rippleBackground);

            if (picView != null) {
                picView.setBackground(resources.getDrawable(picIds[i]));
            }
            if (mButtonIds[i] == R.id.cancel) {
                picView.setText(android.R.string.cancel);
                picView.setTextSize(getResources().getInteger(R.integer.freeme_dial_emergency_cancel));
                picView.setGravity(Gravity.CENTER);
                picView.setBackground(null);
            }
        }

        final DialpadKeyButton one = (DialpadKeyButton) findViewById(R.id.one);
        one.setLongHoverContentDescription(
                resources.getText(R.string.description_voicemail_button));

        final DialpadKeyButton zero = (DialpadKeyButton) findViewById(R.id.zero);
        zero.setLongHoverContentDescription(
                resources.getText(R.string.description_image_button_plus));

    }

    private Drawable getDrawableCompat(Context context, int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(id);
        } else {
            return context.getResources().getDrawable(id);
        }
    }

    public void setShowVoicemailButton(boolean show) {
        View view = findViewById(R.id.dialpad_key_voicemail);
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * Whether or not the digits above the dialer can be edited.
     *
     * @param canBeEdited If true, the backspace button will be shown and the digits EditText
     *                    will be configured to allow text manipulation.
     */
    public void setCanDigitsBeEdited(boolean canBeEdited) {
        View deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setVisibility(canBeEdited ? View.VISIBLE : View.GONE);
        View overflowMenuButton = findViewById(R.id.dialpad_overflow);
        overflowMenuButton.setVisibility(canBeEdited ? View.VISIBLE : View.GONE);

        EditText digits = (EditText) findViewById(R.id.digits);
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
     * Always returns true for onHoverEvent callbacks, to fix problems with accessibility due to
     * the dialpad overlaying other fragments.
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
            int duration =
                    (int) (getKeyButtonAnimationDuration(mButtonIds[i]) * DURATION_MULTIPLIER);
            final DialpadKeyButton dialpadKey = (DialpadKeyButton) findViewById(mButtonIds[i]);

            ViewPropertyAnimator animator = dialpadKey.animate();
            if (mIsLandscape) {
                // Landscape orientation requires translation along the X axis.
                // For RTL locales, ensure we translate negative on the X axis.
                dialpadKey.setTranslationX((mIsRtl ? -1 : 1) * mTranslateDistance);
                animator.translationX(0);
            } else {
                // Portrait orientation requires translation along the Y axis.
                dialpadKey.setTranslationY(mTranslateDistance);
                animator.translationY(0);
            }
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

    public View getOverflowMenuButton() {
        return mOverflowMenuButton;
    }

    /**
     * Get the animation delay for the buttons, taking into account whether the dialpad is in
     * landscape left-to-right, landscape right-to-left, or portrait.
     *
     * @param buttonId The button ID.
     * @return The animation delay.
     */
    private int getKeyButtonAnimationDelay(int buttonId) {
        if (mIsLandscape) {
            if (mIsRtl) {
                if (buttonId == R.id.three) {
                    return KEY_FRAME_DURATION * 1;
                } else if (buttonId == R.id.six) {
                    return KEY_FRAME_DURATION * 2;
                } else if (buttonId == R.id.nine) {
                    return KEY_FRAME_DURATION * 3;
                } else if (buttonId == R.id.pound) {
                    return KEY_FRAME_DURATION * 4;
                } else if (buttonId == R.id.two) {
                    return KEY_FRAME_DURATION * 5;
                } else if (buttonId == R.id.five) {
                    return KEY_FRAME_DURATION * 6;
                } else if (buttonId == R.id.eight) {
                    return KEY_FRAME_DURATION * 7;
                } else if (buttonId == R.id.zero) {
                    return KEY_FRAME_DURATION * 8;
                } else if (buttonId == R.id.one) {
                    return KEY_FRAME_DURATION * 9;
                } else if (buttonId == R.id.four) {
                    return KEY_FRAME_DURATION * 10;
                } else if (buttonId == R.id.seven || buttonId == R.id.star) {
                    return KEY_FRAME_DURATION * 11;
                }
            } else {
                if (buttonId == R.id.one) {
                    return KEY_FRAME_DURATION * 1;
                } else if (buttonId == R.id.four) {
                    return KEY_FRAME_DURATION * 2;
                } else if (buttonId == R.id.seven) {
                    return KEY_FRAME_DURATION * 3;
                } else if (buttonId == R.id.star) {
                    return KEY_FRAME_DURATION * 4;
                } else if (buttonId == R.id.two) {
                    return KEY_FRAME_DURATION * 5;
                } else if (buttonId == R.id.five) {
                    return KEY_FRAME_DURATION * 6;
                } else if (buttonId == R.id.eight) {
                    return KEY_FRAME_DURATION * 7;
                } else if (buttonId == R.id.zero) {
                    return KEY_FRAME_DURATION * 8;
                } else if (buttonId == R.id.three) {
                    return KEY_FRAME_DURATION * 9;
                } else if (buttonId == R.id.six) {
                    return KEY_FRAME_DURATION * 10;
                } else if (buttonId == R.id.nine || buttonId == R.id.pound) {
                    return KEY_FRAME_DURATION * 11;
                }
            }
        } else {
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
        if (mIsLandscape) {
            if (mIsRtl) {
                if (buttonId == R.id.one || buttonId == R.id.four || buttonId == R.id.seven
                        || buttonId == R.id.star) {
                    return KEY_FRAME_DURATION * 8;
                } else if (buttonId == R.id.two || buttonId == R.id.five || buttonId == R.id.eight
                        || buttonId == R.id.zero) {
                    return KEY_FRAME_DURATION * 9;
                } else if (buttonId == R.id.three || buttonId == R.id.six || buttonId == R.id.nine
                        || buttonId == R.id.pound) {
                    return KEY_FRAME_DURATION * 10;
                }
            } else {
                if (buttonId == R.id.one || buttonId == R.id.four || buttonId == R.id.seven
                        || buttonId == R.id.star) {
                    return KEY_FRAME_DURATION * 10;
                } else if (buttonId == R.id.two || buttonId == R.id.five || buttonId == R.id.eight
                        || buttonId == R.id.zero) {
                    return KEY_FRAME_DURATION * 9;
                } else if (buttonId == R.id.three || buttonId == R.id.six || buttonId == R.id.nine
                        || buttonId == R.id.pound) {
                    return KEY_FRAME_DURATION * 8;
                }
            }
        } else {
            if (buttonId == R.id.one || buttonId == R.id.two || buttonId == R.id.three
                    || buttonId == R.id.four || buttonId == R.id.five || buttonId == R.id.six) {
                return KEY_FRAME_DURATION * 10;
            } else if (buttonId == R.id.seven || buttonId == R.id.eight || buttonId == R.id.nine) {
                return KEY_FRAME_DURATION * 9;
            } else if (buttonId == R.id.star || buttonId == R.id.zero || buttonId == R.id.pound) {
                return KEY_FRAME_DURATION * 8;
            }
        }

        Log.wtf(TAG, "Attempted to get animation duration for invalid key button id.");
        return 0;
    }
}
