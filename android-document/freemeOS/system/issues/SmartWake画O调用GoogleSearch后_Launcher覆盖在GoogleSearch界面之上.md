### 现象

- 灭屏状态下，SmartWake画O调用GoogleSearch，Launcher覆盖在GoogleSearch界面之上

### 原因分析:
#### 1.1 Z序

- Window的显示层级由Z序控制，Launcher此时的Z序高于GoogleSearch，故打开WMS LOG开关从Log追踪Z序构成(打开Log请在WindowManagerDebugConfig.java中将DEBUG开关置为True)

```
03-01 19:41:49.638 26315-26597/system_process V/WindowManager: Assigning layers based on windows=[Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}, Window{a64d07c u0 DockedStackDivider}, Window{d7fd93a u0 AssistPreviewPanel}, Window{2f1b225 u0 com.android.systemui.ImageWallpaper}, Window{aff266c u0 KeyguardScrim}, Window{217dcd7 u0 StatusBar}, Window{74155e1 u0 NavigationBar}, Window{2d8e7c3 u0 SmartWake}]
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: mBase=21000 mLayer=21000 mAppLayer=0 =mAnimLayer=21000
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{a64d07c u0 DockedStackDivider}: mBase=21000 mLayer=21005 =mAnimLayer=21005
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{d7fd93a u0 AssistPreviewPanel}: mBase=41000 mLayer=41000 =mAnimLayer=41000
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{2f1b225 u0 com.android.systemui.ImageWallpaper}: mBase=21000 mLayer=41005 =mAnimLayer=41005
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{aff266c u0 KeyguardScrim}: mBase=141000 mLayer=141000 =mAnimLayer=141000
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{217dcd7 u0 StatusBar}: mBase=161000 mLayer=161000 =mAnimLayer=161000
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{74155e1 u0 NavigationBar}: mBase=211000 mLayer=211000 =mAnimLayer=211000
03-01 19:41:49.639 26315-26597/system_process V/WindowManager: Assign layer Window{2d8e7c3 u0 SmartWake}: mBase=291000 mLayer=291000 =mAnimLayer=291000
03-01 19:41:49.660 26315-26608/system_process V/WindowManager: Window{2d8e7c3 u0 SmartWake}: wasAnimating=false, nowAnimating=false
03-01 19:41:49.660 26315-26608/system_process V/WindowManager: Window{217dcd7 u0 StatusBar}: wasAnimating=false, nowAnimating=false
03-01 19:41:49.660 26315-26608/system_process V/WindowManager: Window{2f1b225 u0 com.android.systemui.ImageWallpaper}: wasAnimating=false, nowAnimating=false
03-01 19:41:49.660 26315-26608/system_process V/WindowManager: Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: wasAnimating=false, nowAnimating=false
```

- 如上初始排序状态，Wallpaper Target为StatusBar，ImageWallpaper id 为3

```
03-01 19:41:49.719 26315-27019/system_process D/WindowManager: addWindowToListInOrderLocked: win=Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity} Callers=com.android.server.wm.WindowManagerService.addWindow:2265 com.android.server.wm.Session.addToDisplay:177 android.view.IWindowSession$Stub.onTransact:124 com.android.server.wm.Session.onTransact:139 
03-01 19:41:49.719 26315-27019/system_process V/WindowManager: Based on layer: Adding window Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity} at 4 of 8
03-01 19:41:49.720 26315-27019/system_process V/WindowManager: Adding Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity} to AppWindowToken{53347b3 token=Token{6df4122 ActivityRecord{e88eed u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity t34}}}
03-01 19:41:49.721 26315-27019/system_process V/WindowManager: Assigning layers based on windows=[Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}, Window{a64d07c u0 DockedStackDivider}, Window{d7fd93a u0 AssistPreviewPanel}, Window{2f1b225 u0 com.android.systemui.ImageWallpaper}, Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}, Window{aff266c u0 KeyguardScrim}, Window{217dcd7 u0 StatusBar}, Window{74155e1 u0 NavigationBar}, Window{2d8e7c3 u0 SmartWake}]
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: mBase=21000 mLayer=21000 mAppLayer=0 =mAnimLayer=21000
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{a64d07c u0 DockedStackDivider}: mBase=21000 mLayer=21005 =mAnimLayer=21005
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{d7fd93a u0 AssistPreviewPanel}: mBase=41000 mLayer=41000 =mAnimLayer=41000
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{2f1b225 u0 com.android.systemui.ImageWallpaper}: mBase=21000 mLayer=41005 =mAnimLayer=41005
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}: mBase=21000 mLayer=21000 mAppLayer=0 =mAnimLayer=21000
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{aff266c u0 KeyguardScrim}: mBase=141000 mLayer=141000 =mAnimLayer=141000
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{217dcd7 u0 StatusBar}: mBase=161000 mLayer=161000 =mAnimLayer=161000
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{74155e1 u0 NavigationBar}: mBase=211000 mLayer=211000 =mAnimLayer=211000
03-01 19:41:49.722 26315-27019/system_process V/WindowManager: Assign layer Window{2d8e7c3 u0 SmartWake}: mBase=291000 mLayer=291000 =mAnimLayer=291000
```

- 此时插入googlequicksearchbox后再排序，根据Z序逻辑被插入到了ImageWallpaper前面，id为4，且mLayer为21000

```
03-01 19:41:49.725 26315-27019/system_process V/WindowManager: addWindow: New client android.os.BinderProxy@43e36d2: window=Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity} Callers=com.android.server.wm.Session.addToDisplay:177 android.view.IWindowSession$Stub.onTransact:124 com.android.server.wm.Session.onTransact:139 android.os.Binder.execTransact:570 <bottom of call stack> 
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Win #8 Window{2d8e7c3 u0 SmartWake}: isOnScreen=true mDrawState=4
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Win #7 Window{74155e1 u0 NavigationBar}: isOnScreen=false mDrawState=0
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Win #6 Window{217dcd7 u0 StatusBar}: isOnScreen=true mDrawState=4
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Found wallpaper target: #6=Window{217dcd7 u0 StatusBar}
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Wallpaper vis: target Window{217dcd7 u0 StatusBar}, obscured=false anim=null upper=null lower=null
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Wallpaper visibility: true
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: adjustWallpaper win Window{2f1b225 u0 com.android.systemui.ImageWallpaper} anim layer: 41005
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Wallpaper removing at 3: Window{2f1b225 u0 com.android.systemui.ImageWallpaper}
03-01 19:41:49.727 26315-26608/system_process V/WindowManager: Moving wallpaper Window{2f1b225 u0 com.android.systemui.ImageWallpaper} from 3 to 4
03-01 19:41:49.729 26315-26608/system_process V/WindowManager: Assigning layers based on windows=[Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}, Window{a64d07c u0 DockedStackDivider}, Window{d7fd93a u0 AssistPreviewPanel}, Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}, Window{2f1b225 u0 com.android.systemui.ImageWallpaper}, Window{aff266c u0 KeyguardScrim}, Window{217dcd7 u0 StatusBar}, Window{74155e1 u0 NavigationBar}, Window{2d8e7c3 u0 SmartWake}]
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: mBase=21000 mLayer=21000 mAppLayer=0 =mAnimLayer=21000
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{a64d07c u0 DockedStackDivider}: mBase=21000 mLayer=21005 =mAnimLayer=21005
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{d7fd93a u0 AssistPreviewPanel}: mBase=41000 mLayer=41000 =mAnimLayer=41000
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}: mBase=21000 mLayer=21000 mAppLayer=0 =mAnimLayer=21000
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{2f1b225 u0 com.android.systemui.ImageWallpaper}: mBase=21000 mLayer=21005 =mAnimLayer=21005
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{aff266c u0 KeyguardScrim}: mBase=141000 mLayer=141000 =mAnimLayer=141000
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{217dcd7 u0 StatusBar}: mBase=161000 mLayer=161000 =mAnimLayer=161000
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{74155e1 u0 NavigationBar}: mBase=211000 mLayer=211000 =mAnimLayer=211000
03-01 19:41:49.730 26315-26608/system_process V/WindowManager: Assign layer Window{2d8e7c3 u0 SmartWake}: mBase=291000 mLayer=291000 =mAnimLayer=291000
03-01 19:41:49.751 26315-26608/system_process V/WindowManager: Window{2d8e7c3 u0 SmartWake}: wasAnimating=false, nowAnimating=false
03-01 19:41:49.751 26315-26608/system_process V/WindowManager: Window{217dcd7 u0 StatusBar}: wasAnimating=false, nowAnimating=true
03-01 19:41:49.751 26315-26608/system_process V/WindowManager: Window{2f1b225 u0 com.android.systemui.ImageWallpaper}: wasAnimating=false, nowAnimating=false
03-01 19:41:49.751 26315-26608/system_process V/WindowManager: Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: wasAnimating=false, nowAnimating=false
03-01 19:41:49.752 26315-26608/system_process V/WindowManager: WP target attached xform: Transformation{alpha=1.0 matrix=[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]}
03-01 19:41:49.755 26315-26608/system_process V/WindowManager: Looking for focus: 8 = Window{2d8e7c3 u0 SmartWake}, flags=17302808, canReceive=false
03-01 19:41:49.755 26315-26608/system_process V/WindowManager: Looking for focus: 7 = Window{74155e1 u0 NavigationBar}, flags=25428072, canReceive=false
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Looking for focus: 6 = Window{217dcd7 u0 StatusBar}, flags=-2120875968, canReceive=true
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #8 Window{2d8e7c3 u0 SmartWake}: isOnScreen=true mDrawState=4
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #7 Window{74155e1 u0 NavigationBar}: isOnScreen=false mDrawState=0
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #6 Window{217dcd7 u0 StatusBar}: isOnScreen=true mDrawState=4
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Found wallpaper target: #6=Window{217dcd7 u0 StatusBar}
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win Window{217dcd7 u0 StatusBar}: token animating, looking behind.
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #5 Window{aff266c u0 KeyguardScrim}: isOnScreen=false mDrawState=0
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #3 Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}: isOnScreen=false mDrawState=0
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #2 Window{d7fd93a u0 AssistPreviewPanel}: isOnScreen=false mDrawState=0
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #1 Window{a64d07c u0 DockedStackDivider}: isOnScreen=false mDrawState=0
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Win #0 Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: isOnScreen=true mDrawState=4
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: Found wallpaper target: #0=Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: New wallpaper target: Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher} oldTarget: Window{217dcd7 u0 StatusBar}
```

- 直到此时Launcher被作为Wallpaper Target, 即Launcher被显示出来
- 重新排序，ImageWallpaper拉到最底，Launcher叠于之上，+5Z序，成了21005

```
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: New animation: true old animation: true
03-01 19:41:49.756 26315-26608/system_process V/WindowManager: New i: 0 old i: 6
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: Animating wallpapers: old#6=Window{217dcd7 u0 StatusBar}; new#0=Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: Found target below old target.
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: Wallpaper vis: target Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}, obscured=true anim=null upper=Window{217dcd7 u0 StatusBar} lower=Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: Wallpaper visibility: true
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: adjustWallpaper win Window{2f1b225 u0 com.android.systemui.ImageWallpaper} anim layer: 21005
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: Wallpaper removing at 4: Window{2f1b225 u0 com.android.systemui.ImageWallpaper}
03-01 19:41:49.762 26315-26608/system_process V/WindowManager: Moving wallpaper Window{2f1b225 u0 com.android.systemui.ImageWallpaper} from 4 to 0
03-01 19:41:49.762 26315-26608/system_process D/WindowManager: New wallpaper: target=Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher} lower=Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher} upper=Window{217dcd7 u0 StatusBar}
03-01 19:41:49.764 26315-26608/system_process V/WindowManager: Assigning layers based on windows=[Window{2f1b225 u0 com.android.systemui.ImageWallpaper}, Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}, Window{a64d07c u0 DockedStackDivider}, Window{d7fd93a u0 AssistPreviewPanel}, Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}, Window{aff266c u0 KeyguardScrim}, Window{217dcd7 u0 StatusBar}, Window{74155e1 u0 NavigationBar}, Window{2d8e7c3 u0 SmartWake}]
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{2f1b225 u0 com.android.systemui.ImageWallpaper}: mBase=21000 mLayer=21000 =mAnimLayer=21000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}: mBase=21000 mLayer=21005 mAppLayer=0 =mAnimLayer=21005
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{a64d07c u0 DockedStackDivider}: mBase=21000 mLayer=21010 =mAnimLayer=21010
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{d7fd93a u0 AssistPreviewPanel}: mBase=41000 mLayer=41000 =mAnimLayer=41000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{b9d83a3 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}: mBase=21000 mLayer=21000 mAppLayer=0 =mAnimLayer=21000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{aff266c u0 KeyguardScrim}: mBase=141000 mLayer=141000 =mAnimLayer=141000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{217dcd7 u0 StatusBar}: mBase=161000 mLayer=161000 =mAnimLayer=161000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{74155e1 u0 NavigationBar}: mBase=211000 mLayer=211000 =mAnimLayer=211000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Assign layer Window{2d8e7c3 u0 SmartWake}: mBase=291000 mLayer=291000 =mAnimLayer=291000
03-01 19:41:49.765 26315-26608/system_process V/WindowManager: Wallpaper vis: target Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}, obscured=false anim=null upper=Window{217dcd7 u0 StatusBar} lower=Window{e7eedd0 u0 com.android.launcher3/com.android.launcher3.Launcher}
```

- 至此，Launcher的Z序高于GoogleQuickSearchbox，自然显示于之上，而Launcher为全透的背景，所以看到了两个界面重叠

#### 1.2 Rebuild

- 如上种种，只能说明现象所对应的Z序问题，并不能解释导致此Z序的原因，对于窗口的插入和绘制，我们关注到还有一个关键点－Rebuild　Window

```
03-30 13:25:54.138 6899-7193/system_process V/WindowManager: WP target attached xform: Transformation{alpha=1.0 matrix=[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]}
03-30 13:25:54.151 6899-10973/system_process D/WindowManager: finishDrawingWindow: Window{70d85a0 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity} mDrawState=DRAW_PENDING
03-30 13:25:54.154 6899-7193/system_process V/WindowManager: WP target attached xform: Transformation{alpha=1.0 matrix=[1.0, 0.0, 0.0][0.0, 1.0, 0.0][0.0, 0.0, 1.0]}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Wallpaper vis: target Window{13f8190 u0 StatusBar}, obscured=false anim=null upper=Window{13f8190 u0 StatusBar} lower=Window{130c638 u0 com.android.launcher3/com.android.launcher3.Launcher}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Rebuild removing window: Window{f38049e u0 com.android.systemui/com.android.systemui.recents.RecentsActivity}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Rebuild removing window: Window{130c638 u0 com.android.launcher3/com.android.launcher3.Launcher}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Rebuild removing window: Window{70d85a0 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Re-adding window at 1: Window{f38049e u0 com.android.systemui/com.android.systemui.recents.RecentsActivity}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Re-adding window at 2: Window{130c638 u0 com.android.launcher3/com.android.launcher3.Launcher}
03-30 13:25:54.155 6899-7193/system_process V/WindowManager: Re-adding window at 3: Window{70d85a0 u0 com.google.android.googlequicksearchbox/com.google.android.apps.gsa.queryentry.QueryEntryActivity}
```

如下代码，两种情况会Rebuild, Ready时或者动画结束且Running时

```
// "Something has changed!  Let's make it correct now."
private void performSurfacePlacementInner(boolean recoveringMemory) {
    ...
    // If we are ready to perform an app transition, check through
    // all of the app tokens to be shown and see if they are ready
    // to go.
    if (mService.mAppTransition.isReady()) {
        defaultDisplay.pendingLayoutChanges |= handleAppTransitionReadyLocked(defaultWindows);
        if (DEBUG_LAYOUT_REPEATS)
            debugLayoutRepeats("after handleAppTransitionReadyLocked",
                    defaultDisplay.pendingLayoutChanges);
    }


    if (!mService.mAnimator.mAppWindowAnimating && mService.mAppTransition.isRunning()) {
        // We have finished the animation of an app transition.  To do
        // this, we have delayed a lot of operations like showing and
        // hiding apps, moving apps in Z-order, etc.  The app token list
        // reflects the correct Z-order, but the window list may now
        // be out of sync with it.  So here we will just rebuild the
        // entire app window list.  Fun!
        defaultDisplay.pendingLayoutChanges |=
                mService.handleAnimatingStoppedAndTransitionLocked();
        if (DEBUG_LAYOUT_REPEATS)
            debugLayoutRepeats("after handleAnimStopAndXitionLock",
                    defaultDisplay.pendingLayoutChanges);
    }
    ...
}

/**
 * @return bitmap indicating if another pass through layout must be made.
 */
int handleAnimatingStoppedAndTransitionLocked() {
    ……
    rebuildAppWindowListLocked();
    ……
}
```

跟踪这条Path我们发现，是否Ready取决于inPendingTransaction是否为true，即okToDisplay是否为true，即isScreenOn是否为true

```
boolean okToDisplay() {
    return !mDisplayFrozen && mDisplayEnabled && mPolicy.isScreenOn();
}
```

```
// If we are preparing an app transition, then delay changing
// the visibility of this token until we execute that transition.
if (okToDisplay() && mAppTransition.isTransitionSet()) {
    ……
    wtoken.inPendingTransaction = true;
    ……
}
```

综上，如果在抬起quick search窗口时，如果当前屏幕没有亮，则不会进行动画，且当前状态会被置为Idle，这也是不会进行Rebuild Window的原因

#### 1.3 总结

结合1.1和1.2，如果插入过早，有可能会导致Z序错乱，且如果此时没有亮屏，则不会进行Rebuild

### 解决方式

- 1.Delay 启动GoogleQuickSearchbox的时机为亮屏之后
