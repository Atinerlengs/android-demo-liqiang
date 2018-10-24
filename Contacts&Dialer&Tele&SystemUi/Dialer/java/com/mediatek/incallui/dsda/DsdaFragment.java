/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.mediatek.incallui.dsda;


import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.incallui.R;
import com.android.incallui.incall.protocol.SecondaryInfo;
import com.mediatek.incallui.plugin.ExtensionManager;

/** Shows banner UI for another incoming call */
public class DsdaFragment extends Fragment{

  private static final String ARG_INFO = "info";
  private boolean padTopInset = true;
  private int topInset;

  public static DsdaFragment newInstance(@NonNull SecondaryInfo info) {
    DsdaFragment fragment = new DsdaFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_INFO, info);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
    final View view = layoutInflater.inflate(R.layout.incall_dsda_banner, viewGroup, false);

    SecondaryInfo secondaryInfo = getArguments().getParcelable(ARG_INFO);
    secondaryInfo = Assert.isNotNull(secondaryInfo);

    ((TextView) view.findViewById(R.id.hold_contact_name))
        .setText(
            secondaryInfo.nameIsNumber
                ? PhoneNumberUtils.createTtsSpannable(
                    BidiFormatter.getInstance()
                        .unicodeWrap(secondaryInfo.name, TextDirectionHeuristics.LTR))
                : secondaryInfo.name);
    ((ImageView) view.findViewById(R.id.hold_phone_icon))
        .setImageResource(
            secondaryInfo.isVideoCall
                ? R.drawable.quantum_ic_videocam_white_18
                : R.drawable.quantum_ic_call_white_18);

    TextView providerName = (TextView) view.findViewById(R.id.call_provider_name);
    if (!TextUtils.isEmpty(secondaryInfo.providerLabel)) {
      providerName.setText(
              PhoneNumberUtils.createTtsSpannable(
                BidiFormatter.getInstance()
                    .unicodeWrap(secondaryInfo.providerLabel, TextDirectionHeuristics.LTR)));
      providerName.setTextColor(secondaryInfo.color);
    } else {
      providerName.setVisibility(View.GONE);
    }

    view.addOnAttachStateChangeListener(
        new OnAttachStateChangeListener() {
          @Override
          public void onViewAttachedToWindow(View v) {
            topInset = v.getRootWindowInsets().getSystemWindowInsetTop();
            applyInset();
          }

          @Override
          public void onViewDetachedFromWindow(View v) {}
        });

    /// M: add for OP09 plugin. @{
    ExtensionManager.getCallCardExt().onHoldViewCreated(view);
    /// @}
    return view;
  }

  public void setPadTopInset(boolean padTopInset) {
    this.padTopInset = padTopInset;
    applyInset();
  }

  private void applyInset() {
    if (getView() == null) {
      return;
    }

    int newPadding = padTopInset ? topInset : 0;
    if (newPadding != getView().getPaddingTop()) {
      TransitionManager.beginDelayedTransition(((ViewGroup) getView().getParent()));
      getView().setPadding(0, newPadding, 0, 0);
    }
  }
}
