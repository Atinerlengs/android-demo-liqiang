package com.freeme.incallui.contactgrid;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateYAnimation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.compat.PhoneNumberUtilsCompat;
import com.android.contacts.common.lettertiles.LetterTileDrawable;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.animation.AnimationListenerAdapter;
import com.android.dialer.util.DrawableConverter;
import com.android.incallui.InCallPresenter;
import com.android.incallui.contactgrid.BottomRow;
import com.android.incallui.contactgrid.R;
import com.android.incallui.contactgrid.TopRow;
import com.android.incallui.incall.protocol.ContactPhotoType;
import com.android.incallui.incall.protocol.PrimaryCallState;
import com.android.incallui.incall.protocol.PrimaryInfo;
import com.freeme.incallui.incall.lettertiles.FreemeInCallLetterTileDrawable;
import com.mediatek.incallui.plugin.ExtensionManager;

import java.util.List;

public class FreemeContactGridManager {
    private final Context context;
    private final View contactGridLayout;

    // Row 0: Captain Holt        ON HOLD
    // Row 0: Calling...
    // Row 0: [Wi-Fi icon] Calling via Starbucks Wi-Fi
    // Row 0: [Wi-Fi icon] Starbucks Wi-Fi
    // Row 0: Hey Jake, pick up!
    private final ImageView connectionIconImageView;
    private final TextView statusTextView;

    // Row 1: Jake Peralta        [Contact photo]
    // Row 1: Walgreens
    // Row 1: +1 (650) 253-0000
    private final TextView contactNameTextView;
    @Nullable
    private ImageView avatarImageView;

    // Row 2: Mobile +1 (650) 253-0000
    // Row 2: [HD attempting icon]/[HD icon] 00:15
    // Row 2: Call ended
    // Row 2: Hanging up
    // Row 2: [Alert sign] Suspected spam caller
    // Row 2: Your emergency callback number: +1 (650) 253-0000
    private final ImageView workIconImageView;
    private final ImageView hdIconImageView;
    private final ImageView spamIconImageView;
    private final ImageView simIconImageView;
    private final TextView bottomTextView;
    private final TextView bottomGeoTextView;
    private final Chronometer bottomTimerView;
    private ViewStub mFreemeForwardStub;
    private View mFreemeForward;
    private TextView forwardedNumberView;
    private int avatarSize;
    private boolean hideAvatar;
    private boolean showAnonymousAvatar;
    private boolean middleRowVisible = true;
    private boolean isTimerStarted;

    private PrimaryInfo primaryInfo = PrimaryInfo.createEmptyPrimaryInfo();
    private PrimaryCallState primaryCallState = PrimaryCallState.createEmptyPrimaryCallState();
    private final LetterTileDrawable letterTile;

    public FreemeContactGridManager(
            View view, @Nullable ImageView avatarImageView, int avatarSize, boolean showAnonymousAvatar) {
        context = view.getContext();
        Assert.isNotNull(context);

        this.avatarImageView = avatarImageView;
        this.avatarSize = avatarSize;
        this.showAnonymousAvatar = showAnonymousAvatar;
        connectionIconImageView = view.findViewById(R.id.contactgrid_connection_icon);
        statusTextView = view.findViewById(R.id.contactgrid_status_text);
        contactNameTextView = view.findViewById(R.id.contactgrid_contact_name);
        workIconImageView = view.findViewById(R.id.contactgrid_workIcon);
        hdIconImageView = view.findViewById(R.id.contactgrid_hdIcon);
        spamIconImageView = view.findViewById(R.id.contactgrid_spamIcon);
        simIconImageView = view.findViewById(R.id.contactgrid_simIcon);
        bottomTextView = view.findViewById(R.id.contactgrid_bottom_text);
        bottomGeoTextView = view.findViewById(R.id.contactgrid_geo_text);
        bottomTimerView = view.findViewById(R.id.contactgrid_bottom_timer);
        mFreemeForwardStub = view.findViewById(R.id.freeme_forward);

        mFreemeCallerInfoContainer = view.findViewById(R.id.incall_contact_info);

        contactGridLayout = (View) contactNameTextView.getParent();
        letterTile = new FreemeInCallLetterTileDrawable(context.getResources());
        isTimerStarted = false;

        /// M: add for OP02 plugin. @{
        ExtensionManager.getCallCardExt().onViewCreated(context, view);
        /// @}
    }

    public void show() {
        contactGridLayout.setVisibility(View.VISIBLE);
    }

    public void hide() {
        contactGridLayout.setVisibility(View.GONE);
    }

    public void setAvatarHidden(boolean hide) {
        if (hide != hideAvatar) {
            hideAvatar = hide;
            updatePrimaryNameAndPhoto();
        }
    }

    public boolean isAvatarHidden() {
        return hideAvatar;
    }

    public View getContainerView() {
        return contactGridLayout;
    }

    public void setIsMiddleRowVisible(boolean isMiddleRowVisible) {
        if (middleRowVisible == isMiddleRowVisible) {
            return;
        }
        middleRowVisible = isMiddleRowVisible;

        contactNameTextView.setVisibility(isMiddleRowVisible ? View.VISIBLE : View.GONE);
        updateAvatarVisibility();
    }

    public void setPrimary(PrimaryInfo primaryInfo) {
        this.primaryInfo = primaryInfo;
        updatePrimaryNameAndPhoto();
        updateBottomRow();
    }

    public void setCallState(PrimaryCallState primaryCallState) {
        this.primaryCallState = primaryCallState;
        updatePrimaryNameAndPhoto();
        updateBottomRow();
        updateTopRow();
    }

    public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        dispatchPopulateAccessibilityEvent(event, statusTextView);
        dispatchPopulateAccessibilityEvent(event, contactNameTextView);
        BottomRow.Info info = BottomRow.getInfo(context, primaryCallState, primaryInfo);
        if (info.shouldPopulateAccessibilityEvent) {
            dispatchPopulateAccessibilityEvent(event, bottomTextView);
        }
    }

    public void setAvatarImageView(
            @Nullable ImageView avatarImageView, int avatarSize, boolean showAnonymousAvatar) {
        this.avatarImageView = avatarImageView;
        this.avatarSize = avatarSize;
        this.showAnonymousAvatar = showAnonymousAvatar;
        updatePrimaryNameAndPhoto();
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        final List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // If no text added write null to keep relative position.
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }

    private boolean updateAvatarVisibility() {
        if (avatarImageView == null) {
            return false;
        }

        if (!middleRowVisible) {
            avatarImageView.setVisibility(View.GONE);
            return false;
        }

        boolean hasPhoto = primaryInfo.photo != null;
        if (!hasPhoto && !showAnonymousAvatar) {
            avatarImageView.setVisibility(View.GONE);
            return false;
        }

        avatarImageView.setVisibility(View.VISIBLE);
        return true;
    }

    /**
     * Updates row 0. For example:
     * <p>
     * <ul>
     * <li>Captain Holt ON HOLD
     * <li>Calling...
     * <li>[Wi-Fi icon] Calling via Starbucks Wi-Fi
     * <li>[Wi-Fi icon] Starbucks Wi-Fi
     * <li>Call from
     * </ul>
     */
    private void updateTopRow() {
        TopRow.Info info = TopRow.getInfo(context, primaryCallState, primaryInfo);
        if (TextUtils.isEmpty(info.label)) {
            // Use INVISIBLE here to prevent the rows below this one from moving up and down.
            statusTextView.setVisibility(View.INVISIBLE);
            statusTextView.setText(null);
        } else {
            /// M: Show timer for video upgrade request.@{
            CharSequence countTimer = InCallPresenter.getInstance().appendCountdown(
                    info.label);
            statusTextView.setText(countTimer);
            /// @}
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setSingleLine(info.labelIsSingleLine);
        }

        if (info.icon == null) {
            connectionIconImageView.setVisibility(View.GONE);
        } else {
            connectionIconImageView.setVisibility(View.VISIBLE);
            connectionIconImageView.setImageDrawable(info.icon);
        }
    }

    /**
     * Updates row 1. For example:
     * <p>
     * <ul>
     * <li>Jake Peralta [Contact photo]
     * <li>Walgreens
     * <li>+1 (650) 253-0000
     * </ul>
     */
    private void updatePrimaryNameAndPhoto() {
        if (TextUtils.isEmpty(primaryInfo.name)) {
            contactNameTextView.setText(null);
        } else {
            contactNameTextView.setText(primaryInfo.nameIsNumber
                    ? PhoneNumberUtilsCompat.createTtsSpannable(primaryInfo.number)
                    : primaryInfo.name);

            // Set direction of the name field
            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (primaryInfo.nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            contactNameTextView.setTextDirection(nameDirection);
        }

        if (avatarImageView != null) {
            if (hideAvatar) {
                avatarImageView.setVisibility(View.GONE);
            } else if (avatarSize > 0 && updateAvatarVisibility()) {
                boolean hasPhoto = primaryInfo.photo != null && primaryInfo.photoType == ContactPhotoType.CONTACT;
                // Contact has a photo, don't render a letter tile.
                if (hasPhoto) {
                    avatarImageView.setImageDrawable(
                            DrawableConverter.getRoundedDrawable(
                                    context, primaryInfo.photo, avatarSize, avatarSize));
                    // Contact has a name, that isn't a number.
                } else {
                    letterTile.setCanonicalDialerLetterTileDetails(
                            primaryInfo.name,
                            primaryInfo.contactInfoLookupKey,
                            LetterTileDrawable.SHAPE_RECTANGLE,
                            LetterTileDrawable.getContactTypeFromPrimitives(
                                    primaryCallState.isVoiceMailNumber,
                                    primaryInfo.isSpam,
                                    primaryCallState.isBusinessNumber,
                                    primaryInfo.numberPresentation,
                                    primaryCallState.isConference));
                    // By invalidating the avatarImageView we force a redraw of the letter tile.
                    // This is required to properly display the updated letter tile iconography based on the
                    // contact type, because the background drawable reference cached in the view, and the
                    // view is not aware of the mutations made to the background.
                    avatarImageView.invalidate();
                    avatarImageView.setImageDrawable(letterTile);
                }
            }
        }
    }

    /**
     * Updates row 2. For example:
     * <p>
     * <ul>
     * <li>Mobile +1 (650) 253-0000
     * <li>[HD attempting icon]/[HD icon] 00:15
     * <li>Call ended
     * <li>Hanging up
     * </ul>
     */
    private void updateBottomRow() {
        BottomRow.Info info = BottomRow.getInfo(context, primaryCallState, primaryInfo);

        showforwardedNumber(info.isForwardIconVisible, info.label);

        bottomTextView.setText(primaryInfo.number);

        if (!TextUtils.isEmpty(primaryInfo.location)) {
            bottomGeoTextView.setVisibility(View.VISIBLE);
            bottomGeoTextView.setText(primaryInfo.location);
        } else {
            bottomGeoTextView.setVisibility(View.VISIBLE);
            bottomGeoTextView.setText(null);
        }

        workIconImageView.setVisibility(info.isWorkIconVisible ? View.VISIBLE : View.GONE);
        /*/
        if (hdIconImageView.getVisibility() == View.GONE) {
            if (info.isHdAttemptingIconVisible) {
                hdIconImageView.setVisibility(View.VISIBLE);
                hdIconImageView.setActivated(false);
                Drawable drawableCurrent = hdIconImageView.getDrawable().getCurrent();
                if (drawableCurrent instanceof Animatable && !((Animatable) drawableCurrent).isRunning()) {
                    ((Animatable) drawableCurrent).start();
                }
            } else if (info.isHdIconVisible) {
                hdIconImageView.setVisibility(View.VISIBLE);
                hdIconImageView.setActivated(true);
            }
        } else if (info.isHdIconVisible) {
            hdIconImageView.setActivated(true);
        } else if (!info.isHdAttemptingIconVisible) {
            hdIconImageView.setVisibility(View.GONE);
        }
        //*/
        spamIconImageView.setVisibility(info.isSpamIconVisible ? View.VISIBLE : View.GONE);

        simIconImageView.setVisibility(info.mIsShowSim ? View.VISIBLE : View.GONE);
        simIconImageView.setImageResource(info.mSimIconRes);

        /// M: Show connect time only when primaryCallState.connectTimeMillis > 0 @{
        if (info.isTimerVisible && primaryCallState.connectTimeMillis > 0) {
        /// @}
            bottomTimerView.setVisibility(View.VISIBLE);
            if (primaryCallState.connectTimeMillis > 0) {
                bottomTimerView.setBase(
                        primaryCallState.connectTimeMillis
                                - System.currentTimeMillis()
                                + SystemClock.elapsedRealtime());
            }
            if (!isTimerStarted) {
                LogUtil.i(
                        "FreemeContactGridManager.updateBottomRow",
                        "starting timer with base: %d",
                        bottomTimerView.getBase());
                bottomTimerView.start();

                /// M: ALPS03536309, need to refresh time when CDMA accept @ {
                /**
                 * Google code:
                 isTimerStarted = true;
                 */
                /// @}
            }
        } else {
            bottomTimerView.setVisibility(View.GONE);
            bottomTimerView.stop();
            isTimerStarted = false;
        }
    }

    /**
     * M: add for auto decline timer for video upgrade request.
     */
    public void updateDeclineTimer() {
        updateTopRow();
    }

    private void showforwardedNumber(boolean show, CharSequence number) {
        if (show) {
            if (mFreemeForward == null && mFreemeForwardStub != null) {
                mFreemeForward = mFreemeForwardStub.inflate();
                forwardedNumberView = mFreemeForward.findViewById(R.id.contactgrid_forwardNumber);
            }
            mFreemeForward.setVisibility(View.VISIBLE);
            forwardedNumberView.setText(number);
        } else {
            if (mFreemeForward != null) {
                mFreemeForward.setVisibility(View.GONE);
            }
        }
    }

    private String mCallerMark;

    private void showCallerMarkInfo() {
        if (primaryInfo != null) {
            if (TextUtils.isEmpty(primaryInfo.contactInfoLookupKey)
                    && !TextUtils.isEmpty(mCallerMark)) {
                contactNameTextView.setText(mCallerMark);
            }
        }
    }

    private boolean mIsMultiCall;
    private AnimationSet mCallerInfoShowAnim;
    private AnimationSet mCallerInfoHideAnim;
    private View mFreemeCallerInfoContainer;

    public void performShowOrHideCallerInfo(boolean isMultiCall, boolean isDialpadShown,
                                            boolean show, boolean animate) {
        boolean realShow = show && !isMultiCall && !isDialpadShown;

        if (realShow && isMultiCall) {
            mIsMultiCall = isMultiCall;
            return;
        }

        performShowOrHideInfoAll(realShow, animate);

        mIsMultiCall = isMultiCall;
    }


    private void initCallerInforAnimate() {
        long duration = context.getResources().getInteger(R.integer.dialpad_slide_in_duration);

        mCallerInfoShowAnim = new AnimationSet(true);
        TranslateYAnimation tya_in = new TranslateYAnimation(
                Animation.RELATIVE_TO_SELF, -0.5f, Animation.RELATIVE_TO_SELF, 0f);
        tya_in.setDuration(duration);
        AlphaAnimation aa_in = new AlphaAnimation(0f, 1f);
        aa_in.setDuration(duration);
        mCallerInfoShowAnim.addAnimation(tya_in);
        mCallerInfoShowAnim.addAnimation(aa_in);
        mCallerInfoShowAnim.setDuration(duration);

        mCallerInfoHideAnim = new AnimationSet(true);
        TranslateYAnimation tya_out = new TranslateYAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -0.5f);
        tya_in.setDuration(duration);
        AlphaAnimation aa_out = new AlphaAnimation(1f, 0f);
        aa_in.setDuration(duration);
        mCallerInfoHideAnim.addAnimation(tya_out);
        mCallerInfoHideAnim.addAnimation(aa_out);
        mCallerInfoHideAnim.setDuration(duration);
        mCallerInfoHideAnim.setAnimationListener(mCallerInfoHideAnimListener);
    }

    private AnimationListenerAdapter mCallerInfoHideAnimListener =
            new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mFreemeCallerInfoContainer != null) {
                        mFreemeCallerInfoContainer.setVisibility(View.GONE);
                    }
                    mFreemeCallerInfoContainer.clearAnimation();
                }
            };

    private void performShowOrHideInfoAll(boolean show, boolean animate) {
        if (mFreemeCallerInfoContainer == null || mIsMultiCall) {
            return;
        }
        if (animate) {
            if (mCallerInfoShowAnim == null || mCallerInfoHideAnim == null) {
                initCallerInforAnimate();
            }
            mFreemeCallerInfoContainer.setVisibility(View.VISIBLE);
            mFreemeCallerInfoContainer.startAnimation(
                    show ? mCallerInfoShowAnim : mCallerInfoHideAnim);
        } else {
            mFreemeCallerInfoContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

}
