/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.calllogutils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.PersistableBundle;
import android.provider.CallLog.Calls;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.android.dialer.app.calllog.CallLogActivity;
import com.android.dialer.compat.AppCompatConstants;
import com.mediatek.dialer.ext.ExtensionManager;
import java.util.ArrayList;
import java.util.List;

import mediatek.telephony.MtkCarrierConfigManager;
//*/ freeme.zhaozehong, 20180202. for freemeOS, UI redesign
import com.freeme.contacts.common.utils.FreemeBitmapUtils;
//*/

/**
 * View that draws one or more symbols for different types of calls (missed calls, outgoing etc).
 * The symbols are set up horizontally. If {@code useLargeIcons} is set in the xml attributes,
 * alternatively this view will only render one icon (Call Type, HD or Video).
 *
 * <p>As this view doesn't create subviews, it is better suited for ListView-recycling than a
 * regular LinearLayout using ImageViews.
 */
public class CallTypeIconsView extends View {

  private final boolean useLargeIcons;

  private static Resources sResources;
  private static Resources sLargeResouces;
  private List<Integer> mCallTypes = new ArrayList<>(3);
  private boolean mShowVideo;
  private boolean mShowHd;
  private boolean mShowWifi;
  public int mWidth;
  public int mHeight;


 ///:M For Plugin to show various icons as per call type
  public boolean mShowvowifi = false;
  public boolean mShowvolte = false;
  public boolean mShowViWifi = false;

  ///M: For customization using carrier config
  private boolean mIsSupportCallPull = false;
  /// M: for plugin to get call type icon
  private Context mContext = null;

  public CallTypeIconsView(Context context) {
    this(context, null);
  }

  public CallTypeIconsView(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray typedArray =
        context.getTheme().obtainStyledAttributes(attrs, R.styleable.CallTypeIconsView, 0, 0);
    useLargeIcons = typedArray.getBoolean(R.styleable.CallTypeIconsView_useLargeIcons, false);
    typedArray.recycle();
    if (sResources == null) {
      sResources = new Resources(context, false);
    }
    if (sLargeResouces == null && useLargeIcons) {
      sLargeResouces = new Resources(context, true);
    }
    ///M: For customization using carrier config
    CarrierConfigManager configMgr = (CarrierConfigManager) context
                          .getSystemService(Context.CARRIER_CONFIG_SERVICE);
    PersistableBundle carrierConfig =
            configMgr.getConfigForSubId(SubscriptionManager.getDefaultVoiceSubscriptionId());
    if (carrierConfig != null) {
        mIsSupportCallPull = carrierConfig.getBoolean(
          MtkCarrierConfigManager.MTK_KEY_DIALER_CALL_PULL_BOOL);
    }
    mContext = context;
  }

  public void clear() {
    mCallTypes.clear();
    mWidth = 0;
    mHeight = 0;
    invalidate();
  }

  public void add(int callType) {
    mCallTypes.add(callType);

    /*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign. do not show call type
    final Drawable drawable = getCallTypeDrawable(callType);
    mWidth += drawable.getIntrinsicWidth() + sResources.iconMargin;
    mHeight = Math.max(mHeight, drawable.getIntrinsicWidth());
    //*/
    invalidate();
  }

  /**
   * Determines whether the video call icon will be shown.
   *
   * @param showVideo True where the video icon should be shown.
   */
  public void setShowVideo(boolean showVideo) {
    mShowVideo = showVideo;
    if (showVideo) {
      mWidth += sResources.videoCall.getIntrinsicWidth() + sResources.iconMargin;
      mHeight = Math.max(mHeight, sResources.videoCall.getIntrinsicHeight());
      invalidate();
    }
  }

  /**
   * Determines if the video icon should be shown.
   *
   * @return True if the video icon should be shown.
   */
  public boolean isVideoShown() {
    return mShowVideo;
  }

  public void setShowHd(boolean showHd) {
    mShowHd = showHd;
    if (showHd) {
      mWidth += sResources.hdCall.getIntrinsicWidth() + sResources.iconMargin;
      mHeight = Math.max(mHeight, sResources.hdCall.getIntrinsicHeight());
      invalidate();
    }
  }

  public void setShowWifi(boolean showWifi) {
    mShowWifi = showWifi;
    if (showWifi) {
      mWidth += sResources.wifiCall.getIntrinsicWidth() + sResources.iconMargin;
      mHeight = Math.max(mHeight, sResources.wifiCall.getIntrinsicHeight());
      invalidate();
    }
  }

  public int getCount() {
    return mCallTypes.size();
  }

  public int getCallType(int index) {
    return mCallTypes.get(index);
  }

  private Drawable getCallTypeDrawable(int callType) {
    Resources resources = useLargeIcons ? sLargeResouces : sResources;

    /// M: for plugin in to get call type icon@{
    Drawable drawable = ExtensionManager.getCallLogExtension()
        .getCallTypeDrawable(callType, useLargeIcons, mContext);
    if (drawable != null) {
      return drawable;
    }/// @}

    ///M: For customization using carrier config
    boolean isCallPulledType = true;
    if (mIsSupportCallPull) {
      Log.d("CallTypeIconsView", "Call pull supported");
      if (callType == CallLogActivity.DECLINED_EXTERNAL_TYPE) {
        return sResources.missed;
      } else if (callType == CallLogActivity.INCOMING_PULLED_AWAY_TYPE) {
        return sResources.incoming;
      } else if (callType == CallLogActivity.OUTGOING_PULLED_AWAY_TYPE) {
        return sResources.outgoing;
      }
    }
    ///}@
    switch (callType) {
      case AppCompatConstants.CALLS_INCOMING_TYPE:
      case AppCompatConstants.CALLS_ANSWERED_EXTERNALLY_TYPE:
        return resources.incoming;
      case AppCompatConstants.CALLS_OUTGOING_TYPE:
        return resources.outgoing;
      case AppCompatConstants.CALLS_MISSED_TYPE:
        return resources.missed;
      case AppCompatConstants.CALLS_VOICEMAIL_TYPE:
        return resources.voicemail;
      case AppCompatConstants.CALLS_BLOCKED_TYPE:
        return resources.blocked;
      default:
        // It is possible for users to end up with calls with unknown call types in their
        // call history, possibly due to 3rd party call log implementations (e.g. to
        // distinguish between rejected and missed calls). Instead of crashing, just
        // assume that all unknown call types are missed calls.
        return resources.missed;
    }
  }


  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(mWidth, mHeight);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    Resources resources = useLargeIcons ? sLargeResouces : sResources;
    int left = 0;
    /*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign, do not show call type
    // If we are using large icons, we should only show one icon (video, hd or call type) with
    // priority give to HD or Video. So we skip the call type icon if we plan to show them.
    if (!useLargeIcons || !(mShowHd || mShowVideo || mShowWifi)) {
      for (Integer callType : mCallTypes) {
        final Drawable drawable = getCallTypeDrawable(callType);
        final int right = left + drawable.getIntrinsicWidth();
        drawable.setBounds(left, 0, right, drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        left = right + resources.iconMargin;
      }
    }
    //*/

    // If showing the video call icon, draw it scaled appropriately.
    if (mShowVideo) {
      left = addDrawable(canvas, resources.videoCall, left) + resources.iconMargin;
    }
    // If showing HD call icon, draw it scaled appropriately.
    if (mShowHd) {
      left = addDrawable(canvas, resources.hdCall, left) + resources.iconMargin;
    }
    // If showing HD call icon, draw it scaled appropriately.
    if (mShowWifi) {
      left = addDrawable(canvas, resources.wifiCall, left) + resources.iconMargin;
    }
    ///M: Plug-in call to draw Canvas for different icons ViWifi, VoLTE, VoWifi
    ExtensionManager.getCallLogExtension().drawWifiVolteCanvas(
                        left, canvas, this);
  }

  private int addDrawable(Canvas canvas, Drawable drawable, int left) {
    int right = left + drawable.getIntrinsicWidth();
    drawable.setBounds(left, 0, right, drawable.getIntrinsicHeight());
    drawable.draw(canvas);
    return right;
  }

  /*M: change modifier to public*/
  /*private*/public static class Resources {

    // Drawable representing an incoming answered call.
    public final Drawable incoming;

    // Drawable respresenting an outgoing call.
    public final Drawable outgoing;

    // Drawable representing an incoming missed call.
    public final Drawable missed;

    // Drawable representing a voicemail.
    public final Drawable voicemail;

    // Drawable representing a blocked call.
    public final Drawable blocked;

    // Drawable repesenting a video call.
    final Drawable videoCall;

    // Drawable represeting a hd call.
    final Drawable hdCall;

    // Drawable representing a WiFi call.
    final Drawable wifiCall;

    /** The margin to use for icons. */
    final int iconMargin;

    /**
     * Configures the call icon drawables. A single white call arrow which points down and left is
     * used as a basis for all of the call arrow icons, applying rotation and colors as needed.
     *
     * <p>For each drawable we call mutate so that a new instance of the drawable is created. This
     * is done so that when we apply a color filter to the drawables, they are recolored across
     * dialer.
     *
     * @param context The current context.
     */
    public Resources(Context context, boolean largeIcons) {
      final android.content.res.Resources r = context.getResources();

      int iconId = R.drawable.quantum_ic_call_received_white_24;
      Drawable drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      incoming = drawable.mutate();
      incoming.setColorFilter(r.getColor(R.color.answered_call), PorterDuff.Mode.MULTIPLY);

      // Create a rotated instance of the call arrow for outgoing calls.
      iconId = R.drawable.quantum_ic_call_made_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      outgoing = drawable.mutate();
      outgoing.setColorFilter(r.getColor(R.color.answered_call), PorterDuff.Mode.MULTIPLY);

      // Need to make a copy of the arrow drawable, otherwise the same instance colored
      // above will be recolored here.
      iconId = R.drawable.quantum_ic_call_missed_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      missed = drawable.mutate();
      missed.setColorFilter(r.getColor(R.color.missed_call), PorterDuff.Mode.MULTIPLY);

      iconId = R.drawable.quantum_ic_voicemail_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      voicemail = drawable.mutate();
      voicemail.setColorFilter(r.getColor(R.color.call_type_icon_color), PorterDuff.Mode.MULTIPLY);

      iconId = R.drawable.quantum_ic_block_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      blocked = drawable.mutate();
      blocked.setColorFilter(r.getColor(R.color.blocked_call), PorterDuff.Mode.MULTIPLY);

      /*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
      iconId = R.drawable.quantum_ic_videocam_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      videoCall = drawable.mutate();
      videoCall.setColorFilter(r.getColor(R.color.call_type_icon_color), PorterDuff.Mode.MULTIPLY);

      iconId = R.drawable.quantum_ic_hd_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      hdCall = drawable.mutate();
      hdCall.setColorFilter(r.getColor(R.color.call_type_icon_color), PorterDuff.Mode.MULTIPLY);

      iconId = R.drawable.quantum_ic_signal_wifi_4_bar_white_24;
      drawable = largeIcons ? r.getDrawable(iconId) : getScaledBitmap(context, iconId);
      wifiCall = drawable.mutate();
      wifiCall.setColorFilter(r.getColor(R.color.call_type_icon_color), PorterDuff.Mode.MULTIPLY);
      /*/
      drawable = r.getDrawable(R.drawable.freeme_call_log_type_video);
      videoCall = drawable.mutate();

      drawable = r.getDrawable(R.drawable.freeme_call_log_type_hd);
      hdCall = drawable.mutate();

      drawable = r.getDrawable(R.drawable.freeme_call_log_type_wifi);
      wifiCall = drawable.mutate();
      //*/

      ///M: Plug-in call to draw different icons ViWifi, VoLTE, VoWifi
      ExtensionManager.getCallLogExtension().drawWifiVolteCallIcon(
                       context.getResources().getDimensionPixelSize(R.dimen.call_type_icon_size));
      /*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
      iconMargin = largeIcons ? 0 : r.getDimensionPixelSize(R.dimen.call_log_icon_margin);
      /*/
      iconMargin = r.getDimensionPixelSize(R.dimen.call_log_icon_margin);
      //*/
    }

    // Gets the icon, scaled to the height of the call type icons. This helps display all the
    // icons to be the same height, while preserving their width aspect ratio.
    private Drawable getScaledBitmap(Context context, int resourceId) {
      /*/ freeme.zhaozehong, 20180201. for freemeOS, UI redesign
      Bitmap icon = BitmapFactory.decodeResource(context.getResources(), resourceId);
      /*/
      Bitmap icon = FreemeBitmapUtils.getBitmapFromDrawableRes(context.getResources(), resourceId);
      //*/
      int scaledHeight = context.getResources().getDimensionPixelSize(R.dimen.call_type_icon_size);
      int scaledWidth =
          (int) ((float) icon.getWidth() * ((float) scaledHeight / (float) icon.getHeight()));
      Bitmap scaledIcon = Bitmap.createScaledBitmap(icon, scaledWidth, scaledHeight, false);
      return new BitmapDrawable(context.getResources(), scaledIcon);
    }
  }

    //*/ freeme.zhaozehong, 20180329. for freemeOS, UI redesign
    public boolean isNeedShow() {
        return mShowVideo || mShowHd || mShowWifi
                || mShowvowifi || mShowvolte || mShowViWifi;
    }
    //*/
}
