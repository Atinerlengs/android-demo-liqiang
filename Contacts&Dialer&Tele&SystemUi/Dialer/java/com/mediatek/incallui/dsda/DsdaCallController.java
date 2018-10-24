package com.mediatek.incallui.dsda;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.incallui.incall.protocol.InCallScreenDelegate;
import com.android.incallui.incall.protocol.SecondaryInfo;

/** Manages DSDA two incoming banner. */
public class DsdaCallController implements OnClickListener {

  @NonNull private InCallScreenDelegate inCallScreenDelegate;

  @NonNull private View dsdaCallBar;

  private boolean isVisible;

  private boolean isEnabled;

  @Nullable private SecondaryInfo secondaryInfo;

  public DsdaCallController(
      @NonNull View dsdaBanner,
      @NonNull InCallScreenDelegate inCallScreenDelegate) {
    this.dsdaCallBar = Assert.isNotNull(dsdaBanner);
    this.dsdaCallBar.setOnClickListener(this);
    this.inCallScreenDelegate = Assert.isNotNull(inCallScreenDelegate);
  }

  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
    updateButtonState();
  }

  public void setVisible(boolean isVisible) {
    this.isVisible = isVisible;
    updateButtonState();
  }

  public void setOnScreen() {
    isVisible = hasSecondaryInfo();
    updateButtonState();
  }

  public void setSecondaryInfo(@Nullable SecondaryInfo secondaryInfo) {
    this.secondaryInfo = secondaryInfo;
    isVisible = hasSecondaryInfo();
  }

  private boolean hasSecondaryInfo() {
    return secondaryInfo != null && secondaryInfo.shouldShow;
  }

  public void updateButtonState() {
    dsdaCallBar.setEnabled(isEnabled);
    dsdaCallBar.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
  }

  @Override
  public void onClick(View view) {
    LogUtil.d("AnswerFragment.onClick", "DsdaCallController");
    inCallScreenDelegate.onSecondaryInfoClicked();
  }
}
