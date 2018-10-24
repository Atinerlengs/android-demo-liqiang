/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui.videotech.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import com.android.dialer.util.PermissionsUtil;




public class VideoUtils {

  public static boolean hasSentVideoUpgradeRequest(@SessionModificationState int state) {
    return state == SessionModificationState.WAITING_FOR_UPGRADE_TO_VIDEO_RESPONSE
        || state == SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_FAILED
        || state == SessionModificationState.REQUEST_REJECTED
        || state == SessionModificationState.UPGRADE_TO_VIDEO_REQUEST_TIMED_OUT
        /// M: cancel upgrade
        || state == SessionModificationState.WAITING_FOR_CANCEL_UPGRADE_RESPONSE
        /// M: ALPS03523330 show failed title @{
        || state == SessionModificationState.REQUEST_FAILED;
        /// @}
  }

  public static boolean hasReceivedVideoUpgradeRequest(@SessionModificationState int state) {
    return state == SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST;
  }

  public static boolean hasCameraPermissionAndShownPrivacyToast(@NonNull Context context) {
    /// M:ALPS03473303 only check camera permission on insturment test.incallui will show dialog to
    ///choose whether allow camera to use or not in video call,but on insturment test can't make
    ///choose. so incallui will set camera is null to vtservice.VTservice will get stuck. @{
    return isTestSim() ? hasCameraPermission(context) :
    ///@}
           (PermissionsUtil.hasCameraPrivacyToastShown(context) && hasCameraPermission(context));
  }

  public static boolean hasCameraPermission(@NonNull Context context) {
    return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED;
  }

  /// M:ALPS03473303 only check camera permission on insturment test. @{
  public static boolean isTestSim() {
        boolean isTestSim = false;
        isTestSim = SystemProperties.get("gsm.sim.ril.testsim").equals("1") ||
                   SystemProperties.get("gsm.sim.ril.testsim.2").equals("1") ||
                   SystemProperties.get("gsm.sim.ril.testsim.3").equals("1") ||
                   SystemProperties.get("gsm.sim.ril.testsim.4").equals("1");
        return isTestSim;
  }
  ///@}
}
