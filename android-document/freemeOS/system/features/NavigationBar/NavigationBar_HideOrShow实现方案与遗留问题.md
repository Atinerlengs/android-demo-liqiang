
## Androi代码NavigatioBar隐藏/唤出实现

### 原理简介

[未完待续]

### 代码实现

patch文件见共享中的minibar_prife_3_remove_log.patch文件。

### 遗留问题
附加：

4.无法兼容SYSTEM_UI_FLAG_FULLSCREEN

原理上，禁用了fakewindow，当应用设置了SYSTEM_UI_FLAG_FULLSCREEN风格时，无法恢复

1. 隐藏导航栏后，在任一编辑框中点击唤出输入法软，软键盘不能布满全屏。

   **说明** ：华为手机无此问题

   **进展** ：该问题已经解决，代码如下：

   ```
     @@ -2228,7 +2252,7 @@ public class PhoneWindowManager implements WindowManagerPolicy {
             if (mHasNavigationBar) {
                 // For a basic navigation bar, when we are in landscape mode we place
                 // the navigation bar to the side.
    -            if (mNavigationBarCanMove && fullWidth > fullHeight) {
    +            if (mFreemeosNavBarMin == false && mNavigationBarCanMove && fullWidth > fullHeight) {
                     return fullWidth - mNavigationBarWidthForRotation[rotation];
                 }
             }
    @@ -3468,6 +3492,27 @@ public class PhoneWindowManager implements WindowManagerPolicy {
             }
         };
   ```

1. 横屏模式下，隐藏/显示导航栏后输入法不能自动布局。

   **说明** ：该问题在华为手机上安装的第三方输入法也存在，但是华为内置的中文输入法（百度输入法定制版）无此问题，经反编译后发现，该输入法接收隐藏/显示导航栏inten后重新初始化输入法窗口解决该问题。

1. 唤出输入法后，切换屏幕方向（横屏/竖屏切换），输入法窗口消失

   **说明** ：该问题并非导航栏导致，安卓原生ROM也有此问题，华为手机可以保持输入法窗口横竖屏切换。

```diff
diff --git a/packages/SettingsProvider/res/values/defaults.xml b/packages/SettingsProvider/res/values/defaults.xml
index 1cd2908..cb07c52 100644
--- a/packages/SettingsProvider/res/values/defaults.xml
+++ b/packages/SettingsProvider/res/values/defaults.xml
@@ -17,6 +17,12 @@
  */
 -->
 <resources>
+    <!-- freeme.Linguanrong, 20150707. for navigationbar show/hide. -->
+    <bool name="def_show_navigationbar" translatable="false">true</bool>
+    <bool name="def_can_hide_navigationbar" translatable="false">true</bool>
+    <bool name="def_fullscreen_nvbar_show" translatable="false">true</bool>
+    <!-- freeme.Linguanrong, 20150707. end -->
+
     <bool name="def_dim_screen">true</bool>
     <integer name="def_screen_off_timeout">60000</integer>
     <integer name="def_sleep_timeout">-1</integer>
diff --git a/packages/SystemUI/res/drawable-hdpi/ic_sysbar_hide_bar_land.png b/packages/SystemUI/res/drawable-hdpi/ic_sysbar_hide_bar_land.png
new file mode 100755
index 0000000..1074f72
Binary files /dev/null and b/packages/SystemUI/res/drawable-hdpi/ic_sysbar_hide_bar_land.png differ
diff --git a/packages/SystemUI/res/drawable-hdpi/ic_sysbar_hide_bar_port.png b/packages/SystemUI/res/drawable-hdpi/ic_sysbar_hide_bar_port.png
new file mode 100755
index 0000000..c7dac86
Binary files /dev/null and b/packages/SystemUI/res/drawable-hdpi/ic_sysbar_hide_bar_port.png differ
diff --git a/packages/SystemUI/res/drawable-xhdpi/ic_sysbar_hide_bar_land.png b/packages/SystemUI/res/drawable-xhdpi/ic_sysbar_hide_bar_land.png
new file mode 100755
index 0000000..4e26117
Binary files /dev/null and b/packages/SystemUI/res/drawable-xhdpi/ic_sysbar_hide_bar_land.png differ
diff --git a/packages/SystemUI/res/drawable-xhdpi/ic_sysbar_hide_bar_port.png b/packages/SystemUI/res/drawable-xhdpi/ic_sysbar_hide_bar_port.png
new file mode 100755
index 0000000..313fc6c
Binary files /dev/null and b/packages/SystemUI/res/drawable-xhdpi/ic_sysbar_hide_bar_port.png differ
diff --git a/packages/SystemUI/res/layout/navigation_bar.xml b/packages/SystemUI/res/layout/navigation_bar.xml
index c92ba45..f03b720 100644
--- a/packages/SystemUI/res/layout/navigation_bar.xml
+++ b/packages/SystemUI/res/layout/navigation_bar.xml
@@ -40,6 +40,19 @@
             android:id="@+id/nav_buttons"
             android:animateLayoutChanges="true"
             >
+            <!-- Modified begin by tyd xupeng, add for navigation bar show or hide, 2016-01-13 -->
+            <!-- Modified by Linguanrong for navigationbar show/hide, 2015-7-6 -->
+            <View
+                android:layout_width="0dp"
+                android:layout_height="match_parent"
+                android:layout_weight="1"
+                android:visibility="invisible"
+                />
+            <com.android.systemui.statusbar.policy.KeyButtonView android:id="@+id/hide_bar"
+                android:layout_width="wrap_content"
+                android:layout_height="match_parent"
+                android:src="@drawable/ic_sysbar_hide_bar_port"
+                />

             <!-- navigation controls -->
             <View
@@ -48,6 +61,7 @@
                 android:layout_weight="0"
                 android:visibility="invisible"
                 />
+
             <com.android.systemui.statusbar.policy.KeyButtonView android:id="@+id/back"
                 android:layout_width="@dimen/navigation_key_width"
                 android:layout_height="match_parent"
@@ -265,6 +279,20 @@
                 android:layout_weight="0"
                 android:visibility="invisible"
                 />
+            <!-- Modified begin by tyd xupeng, add for navigation bar show or hide, 2016-01-13 -->
+            <!-- Modified by Linguanrong for navigationbar show/hide, 2015-7-6 -->
+            <com.android.systemui.statusbar.policy.KeyButtonView android:id="@+id/hide_bar"
+                android:layout_height="wrap_content"
+                android:layout_width="match_parent"
+                android:src="@drawable/ic_sysbar_hide_bar_land"
+                />
+            <View
+                android:layout_height="@dimen/navigation_side_padding"
+                android:layout_width="match_parent"
+                android:layout_weight="0"
+                android:visibility="invisible"
+                />
+            <!-- Modified end -->
         </LinearLayout>

         <!-- lights out layout to match exactly -->
diff --git a/packages/SystemUI/src/com/android/systemui/statusbar/phone/NavigationBarView.java b/packages/SystemUI/src/com/android/systemui/statusbar/phone/NavigationBarView.java
index 7d2805d..e85f79e 100644
--- a/packages/SystemUI/src/com/android/systemui/statusbar/phone/NavigationBarView.java
+++ b/packages/SystemUI/src/com/android/systemui/statusbar/phone/NavigationBarView.java
@@ -333,6 +333,10 @@ public class NavigationBarView extends LinearLayout {
         return mCurrentView.findViewById(R.id.ime_switcher);
     }

+    public View getHideBarButton() {
+        return mCurrentView.findViewById(R.id.hide_bar);
+    }
+
     private void getIcons(Resources res) {
         mBackIcon = mNavBarPlugin.getBackImage(res.getDrawable(R.drawable.ic_sysbar_back));

diff --git a/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBar.java b/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBar.java
index cd322b9..db71b1e 100644
--- a/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBar.java
+++ b/packages/SystemUI/src/com/android/systemui/statusbar/phone/PhoneStatusBar.java
@@ -1122,6 +1122,55 @@ public class PhoneStatusBar extends BaseStatusBar implements DemoMode,
         return mNaturalBarHeight;
     }

+    //prife
+    private View.OnClickListener mHideClickListener = new View.OnClickListener() {
+        public void onClick(View v) {
+            Log.i(TAG, "hide on nvigationbar is pressed");
+            if((mSystemUiVisibility & (View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)) != 0
+                    && (mSystemUiVisibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0) {
+                Log.i(TAG, "IMMERSIVE pressed");
+                //cancelAutohide();
+                //mHandler.post(mAutohide);
+            } else {
+                Log.i(TAG, "send intent to hide navigation bar");
+                Intent intent = new Intent("com.freeme.navigationbar.statuschange");
+                intent.putExtra("minNavigationBar", true);
+                mContext.sendBroadcast(intent);
+                //setNavigationBarHiddenbyButton(true);
+                //
+                intent = new Intent("com.huawei.navigationbar.statuschange");
+                intent.putExtra("minNavigationBar", true);
+                mContext.sendBroadcast(intent);
+            }
+        }
+    };
+
+
+    private BroadcastReceiver mNavigationBarBCR = new BroadcastReceiver() {
+        public void onReceive(Context context, Intent intent) {
+            if(intent != null && intent.getAction() != null &&
+               ("com.freeme.navigationbar.statuschange".equals(intent.getAction()))) {
+                boolean barState = intent.getBooleanExtra("minNavigationBar", false);
+                updateNavigationBar(barState);
+            }
+        }
+    };
+
+    public void updateNavigationBar(boolean miniNavigationbar) {
+        if (mNavigationBarView == null)
+            return;
+        //TODO: huawei code here called changeNaviBarStatus
+        if (miniNavigationbar) {
+            Log.i(TAG, "setVisibility GONE");
+            mNavigationBarView.setVisibility(View.GONE);
+            //mWindowManagerService.updateRotation(true, true);
+            //setSystemUiVisibility(0, View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
+        } else {
+            Log.i(TAG, "setVisibility VISIBLE");
+            mNavigationBarView.setVisibility(View.VISIBLE);
+        }
+    }
+
     private View.OnClickListener mRecentsClickListener = new View.OnClickListener() {
         public void onClick(View v) {
             awakenDreams();
@@ -1189,6 +1238,13 @@ public class PhoneStatusBar extends BaseStatusBar implements DemoMode,
         mNavigationBarView.getBackButton().setOnLongClickListener(mLongPressBackRecentsListener);
         mNavigationBarView.getHomeButton().setOnTouchListener(mHomeActionListener);
         mNavigationBarView.getHomeButton().setOnLongClickListener(mLongPressHomeListener);
+
+        //prife
+        mNavigationBarView.getHideBarButton().setOnClickListener(mHideClickListener);
+        IntentFilter intent = new IntentFilter();
+        intent.addAction("com.freeme.navigationbar.statuschange");
+        mContext.registerReceiver(mNavigationBarBCR, intent);
+
         mAssistManager.onConfigurationChanged();
         /// M: add for multi window @{
         if(MultiWindowProxy.isSupported()){
diff --git a/services/core/java/com/android/server/policy/PhoneWindowManager.java b/services/core/java/com/android/server/policy/PhoneWindowManager.java
index c683688..7d42990 100644
--- a/services/core/java/com/android/server/policy/PhoneWindowManager.java
+++ b/services/core/java/com/android/server/policy/PhoneWindowManager.java
@@ -153,7 +153,7 @@ import com.mediatek.multiwindow.MultiWindowProxy;
  * of both of those when held.
  */
 public class PhoneWindowManager implements WindowManagerPolicy {
-    static final String TAG = "WindowManager";
+    static final String TAG = "PhoneWindowManager";
     /// M: runtime switch debug flags @{
     static boolean DEBUG = false;
     static boolean localLOGV = false;
@@ -484,6 +484,9 @@ public class PhoneWindowManager implements WindowManagerPolicy {
     // If nonzero, a panic gesture was performed at that time in uptime millis and is still pending.
     private long mPendingPanicGestureUptime;

+    //prife
+    boolean mFreemeosNavBarMin = false;
+
     InputConsumer mInputConsumer = null;

     static final Rect mTmpParentFrame = new Rect();
@@ -1496,6 +1499,11 @@ public class PhoneWindowManager implements WindowManagerPolicy {
         context.registerReceiver(mStkUserActivityEnReceiver, stkUserActivityFilter);
         /// M: @}

+        //prife
+        IntentFilter navbarFilter = new IntentFilter();
+        navbarFilter.addAction("com.freeme.navigationbar.statuschange");
+        context.registerReceiver(mNavigationBarBCR, navbarFilter);
+
         // register for dream-related broadcasts
         filter = new IntentFilter();
         filter.addAction(Intent.ACTION_DREAMING_STARTED);
@@ -1524,6 +1532,14 @@ public class PhoneWindowManager implements WindowManagerPolicy {
                         if (isGestureIsolated())
                             return;
                         if (mNavigationBar != null && mNavigationBarOnBottom) {
+                            Log.i(TAG, "onSwipeFromBottom pass1");
+
+                            //prife
+                            mFreemeosNavBarMin = false;
+                            Intent intent = new Intent("com.freeme.navigationbar.statuschange");
+                            intent.putExtra("minNavigationBar", false);
+                            mContext.sendBroadcast(intent);
+
                             requestTransientBars(mNavigationBar);
                         }
                     }
@@ -1533,6 +1549,14 @@ public class PhoneWindowManager implements WindowManagerPolicy {
                         if (isGestureIsolated())
                             return;
                         if (mNavigationBar != null && !mNavigationBarOnBottom) {
+                            Log.i(TAG, "onSwipeFromRight pass1");
+
+                            //prife
+                            mFreemeosNavBarMin = false;
+                            Intent intent = new Intent("com.freeme.navigationbar.statuschange");
+                            intent.putExtra("minNavigationBar", false);
+                            mContext.sendBroadcast(intent);
+
                             requestTransientBars(mNavigationBar);
                         }
                     }
@@ -3468,6 +3492,27 @@ public class PhoneWindowManager implements WindowManagerPolicy {
         }
     };

+    //prife
+    private BroadcastReceiver mNavigationBarBCR = new BroadcastReceiver() {
+        public void onReceive(Context context, Intent intent) {
+            if(intent != null && intent.getAction() != null &&
+               ("com.freeme.navigationbar.statuschange".equals(intent.getAction()))) {
+                mFreemeosNavBarMin = intent.getBooleanExtra("minNavigationBar", false);
+                android.os.SystemProperties.set("persist.sys.custom.navbar_min", mFreemeosNavBarMin ? "1" : "0");
+                Log.i(TAG, "3492 mFreemeosNavBarMin=" + mFreemeosNavBarMin);
+                if (mFreemeosNavBarMin) {
+                    try {
+                        mWindowManager.statusBarVisibilityChanged(mLastSystemUiFlags |
+                           View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
+                    } catch (RemoteException e) {
+                    }
+                } else {
+                    //should not be here
+                }
+            }
+        }
+    };
+
     @Override
     public int adjustSystemUiVisibilityLw(int visibility) {
         mStatusBarController.adjustSystemUiVisibilityLw(mLastSystemUiFlags, visibility);
@@ -3645,11 +3690,16 @@ public class PhoneWindowManager implements WindowManagerPolicy {
                 navTranslucent &= areTranslucentBarsAllowed();
             }

+            //prife fix navVisible state with
+            if (mFreemeosNavBarMin) {
+                navVisible = false;
+            }
+
             // When the navigation bar isn't visible, we put up a fake
             // input window to catch all touch events.  This way we can
             // detect when the user presses anywhere to bring back the nav
             // bar and ensure the application doesn't see the event.
-            if (navVisible || navAllowedHidden) {
+            if (mFreemeosNavBarMin || navVisible || navAllowedHidden) {
                 if (mInputConsumer != null) {
                     mInputConsumer.dismiss();
                     mInputConsumer = null;
@@ -3690,8 +3740,11 @@ public class PhoneWindowManager implements WindowManagerPolicy {
                                     = mDockBottom - mRestrictedOverscanScreenTop;
                         }
                     } else {
+                        if (DEBUG) Log.i(TAG, "beginLayout: navVisible is false");
                         // We currently want to hide the navigation UI.
                         mNavigationBarController.setBarShowingLw(false);
+                        //prife
+                        mStableBottom = mStableFullscreenBottom = mDockBottom;
                     }
                     if (navVisible && !navTranslucent && !navAllowedHidden
                             && !mNavigationBar.isAnimatingLw()
@@ -3721,6 +3774,8 @@ public class PhoneWindowManager implements WindowManagerPolicy {
                     } else {
                         // We currently want to hide the navigation UI.
                         mNavigationBarController.setBarShowingLw(false);
+                        //prife
+                        mStableRight = mStableFullscreenRight = mDockRight;
                     }
                     if (navVisible && !navTranslucent && !navAllowedHidden
                             && !mNavigationBar.isAnimatingLw()

```

