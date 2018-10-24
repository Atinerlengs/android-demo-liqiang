package com.freeme.recents;

import android.annotation.NonNull;
import android.app.ActivityManager;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Messenger;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.MutableBoolean;

import com.android.systemui.R;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.model.ThumbnailData;

import com.freeme.internal.app.AppLockPolicy;
import com.freeme.recents.recentslib.data.model.Task;
import com.freeme.recents.recentslib.data.repository.SystemProxy;

import com.mediatek.systemui.statusbar.util.FeatureOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.app.ActivityManager.StackId.DOCKED_STACK_ID;
import static android.app.ActivityManager.StackId.FULLSCREEN_WORKSPACE_STACK_ID;
import static android.app.ActivityManager.StackId.HOME_STACK_ID;
import static android.app.ActivityManager.StackId.RECENTS_STACK_ID;

public class SystemServicesProxy implements SystemProxy {

    final static String TAG = "SystemServicesProxy";
    private static SystemServicesProxy mSystemServicesProxy;
    final static BitmapFactory.Options sBitmapOptions;
    private Messenger mService = null;
    static {
        sBitmapOptions = new BitmapFactory.Options();
        sBitmapOptions.inMutable = true;
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
    }
    protected final ActivityManager mAm;
    private final IActivityManager mIam;
    private final PackageManager mPm;
    Canvas mBgProtectionCanvas;
    Paint mBgProtectionPaint;

    private final int NUMLASTTASKS = 30;

    private SystemServicesProxy(Context context) {
        mContext = context;
        mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mIam = ActivityManagerNative.getDefault();
        mPm = context.getPackageManager();
        bindFreemeRecentsService(context);
        mBgProtectionPaint = new Paint();
        mBgProtectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        mBgProtectionPaint.setColor(0xFFffffff);
        mBgProtectionCanvas = new Canvas();

        //*/ freeme.gouzhouping. 20180604. applock
        if (AppLockPolicy.isSupportAppLock()) {
            Resources res = context.getResources();
            BitmapFactory.Options appLockBmpOpts = new BitmapFactory.Options();
            int defTextSize = res.getDimensionPixelSize(R.dimen.app_locked_text_size);
            int[] attr = new int[]{android.R.attr.textAppearanceLarge,
                    android.R.attr.textColorSecondary,
                    android.R.attr.colorPrimary};
            TypedArray array = context.getTheme().obtainStyledAttributes(attr);

            int largeResId = array.getResourceId(0, -1);
            mAppLockTextColor = array.getColor(1, Color.GRAY);
            mAppLockBgColor = array.getColor(2, Color.WHITE);
            array.recycle();

            if (largeResId > 0) {
                array = context.getTheme().obtainStyledAttributes(largeResId, new int[]{android.R.attr.textSize});
                mAppLockTestSize = array.getDimension(0, defTextSize);
                array.recycle();
            } else {
                mAppLockTestSize = defTextSize;
            }

            appLockBmpOpts.inJustDecodeBounds = false;
            mAppLockFgBmp = BitmapFactory.decodeResource(res,
                    R.drawable.ic_taskmanager_app_lock, appLockBmpOpts);
            mAppLockText = res.getString(R.string.app_locked);
            mAppLockTextTopPadding = res.getDimensionPixelSize(R.dimen.app_locked_text_top_padding);
        }
        //*/
    }

    public static SystemServicesProxy getInstance(Context context) {
        if (mSystemServicesProxy == null) {
            mSystemServicesProxy = new SystemServicesProxy(context);
        }
        return mSystemServicesProxy;
    }

    public void proxyAction(int action) {
        if (mService == null) return;

        Message msg = Message.obtain(null, action, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void proxyAction(int action, int value) {
        if (mService == null) return;

        Message msg = Message.obtain(null, action, 0, 0);
        msg.arg1 = value;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    private void bindFreemeRecentsService(Context context) {
        Intent i = new Intent();
        i.setComponent(new ComponentName("com.android.systemui",
                    "com.android.systemui.SystemUIService"));
        context.bindServiceAsUser(i, mConnection, Context.BIND_AUTO_CREATE, UserHandle.SYSTEM);
    }

    public ActivityManager.MemoryInfo getMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        mAm.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    public boolean isRecentsActivityVisible() {
        return isRecentsActivityVisible(null);
    }

    public boolean isRecentsActivityVisible(MutableBoolean isHomeStackVisible) {
        if (mIam == null) return false;
        try {
            List<StackInfo> stackInfos = mIam.getAllStackInfos();
            ActivityManager.StackInfo homeStackInfo = null;
            ActivityManager.StackInfo fullscreenStackInfo = null;
            ActivityManager.StackInfo recentsStackInfo = null;
            for (int i = 0; i < stackInfos.size(); i++) {
                StackInfo stackInfo = stackInfos.get(i);
                if (stackInfo.stackId == HOME_STACK_ID) {
                    homeStackInfo = stackInfo;
                } else if (stackInfo.stackId == FULLSCREEN_WORKSPACE_STACK_ID) {
                    fullscreenStackInfo = stackInfo;
                } else if (stackInfo.stackId == RECENTS_STACK_ID) {
                    recentsStackInfo = stackInfo;
                }
            }
            boolean homeStackVisibleNotOccluded = isStackNotOccluded(homeStackInfo,
                    fullscreenStackInfo);
            boolean recentsStackVisibleNotOccluded = isStackNotOccluded(recentsStackInfo,
                    fullscreenStackInfo);
            if (isHomeStackVisible != null) {
                isHomeStackVisible.value = homeStackVisibleNotOccluded;
            }
            ComponentName topActivity = recentsStackInfo != null ?
                    recentsStackInfo.topActivity : null;
            return (recentsStackVisibleNotOccluded && topActivity != null
                    && topActivity.getPackageName().equals(RecentsImpl.RECENTS_PACKAGE)
                    && Recents.RECENTS_ACTIVITIES.contains(topActivity.getClassName()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isStackNotOccluded(ActivityManager.StackInfo stackInfo,
                                       ActivityManager.StackInfo fullscreenStackInfo) {
        boolean stackVisibleNotOccluded = stackInfo == null || stackInfo.visible;
        if (fullscreenStackInfo != null && stackInfo != null) {
            boolean isFullscreenStackOccludingg = fullscreenStackInfo.visible &&
                    fullscreenStackInfo.position > stackInfo.position;
            stackVisibleNotOccluded &= !isFullscreenStackOccludingg;
        }
        return stackVisibleNotOccluded;
    }

    public void removeTask(int taskId) {
        if (mAm == null) return;

        mAm.removeTask(taskId);
    }

    public void removeAllTasks() {
        int currentUserId = UserHandle.USER_CURRENT;
        int flags = ActivityManager.RECENT_IGNORE_HOME_AND_RECENTS_STACK_TASKS |
                ActivityManager.RECENT_INGORE_DOCKED_STACK_TOP_TASK |
                        ActivityManager.RECENT_INGORE_PINNED_STACK_TASKS |
                        ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                        ActivityManager.RECENT_INCLUDE_PROFILES;
        flags |= ActivityManager.RECENT_WITH_EXCLUDED;
        final List<ActivityManager.RecentTaskInfo> recentTasks = mAm.getRecentTasksForUser(100, flags, currentUserId);
        for (int i = 0; i < recentTasks.size(); ++i) {
            final ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);
            mAm.removeTask(recentInfo.persistentId);
        }
        mAm.removeTask(-1);
    }

    public Drawable getTaskIcon(ActivityManager.TaskDescription taskDescription, ComponentName cn,
                                int userId, Resources res) {
        Bitmap tdIcon = taskDescription.getInMemoryIcon();
        if (tdIcon == null && isNapp()) {
            tdIcon = ActivityManager.TaskDescription.loadTaskDescriptionIcon(
                    taskDescription.getIconFilename(), userId);
        }
        if (tdIcon != null) {
            return getBadgedIcon(new BitmapDrawable(res, tdIcon), userId);
        }

        ActivityInfo activityInfo = getActivityInfo(cn);
        if (activityInfo != null) {
            Drawable icon = activityInfo.loadIcon(mPm);
            if (icon != null) {
                if (!isNapp()) {
                    return icon;
                } else {
                    return getBadgedIcon(icon, userId);
                }
            }
        }

        return null;
    }

    /*/ freeme.gouzhouping, 20180328. Recents, blank view.
    public Drawable getTaskThumbnail(int taskId, Resources res) {
        if (mAm == null) {
            return null;
        }
        if (ActivityManager.ENABLE_TASK_SNAPSHOTS) {
            ActivityManager.TaskSnapshot snapshot = null;
            try {
                snapshot = ActivityManager.getService().getTaskSnapshot(taskId, true);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to retrieve snapshot", e);
            }
            if (snapshot != null) {
                return new BitmapDrawable(res,(Bitmap.createHardwareBitmap(snapshot.getSnapshot())));
            } else {
                return null;
            }
        } else {
            ActivityManager.TaskThumbnail taskThumbnail = mAm.getTaskThumbnail(taskId);
            if (taskThumbnail == null) {
                return null;
            }
            Bitmap thumbnail = taskThumbnail.mainThumbnail;
            ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
            if (thumbnail == null && descriptor != null) {
                thumbnail = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(),
                        null, sBitmapOptions);
            }
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException e) {
                }
            }

            if (thumbnail != null) {
                thumbnail.setHasAlpha(false);
                // We use a dumb heuristic for now, if the thumbnail is purely transparent in the top
                // left pixel, then assume the whole thumbnail is transparent. Generally, proper
                // screenshots are always composed onto a bitmap that has no alpha.
                if (Color.alpha(thumbnail.getPixel(0, 0)) == 0) {
                    mBgProtectionCanvas.setBitmap(thumbnail);
                    mBgProtectionCanvas.drawRect(0, 0, thumbnail.getWidth(),
                            thumbnail.getHeight(), mBgProtectionPaint);
                    mBgProtectionCanvas.setBitmap(null);
                    Log.e(TAG, "Invalid screenshot detected from getTaskThumbnail()");
                }
                return new BitmapDrawable(res, thumbnail);
            }
            return null;
        }
    }
    //*/

    public String getTaskLable(ActivityManager.TaskDescription description, ComponentName cn) {
        if (description != null && description.getLabel() != null) {
            return description.getLabel();
        }
        ActivityInfo activityInfo = getActivityInfo(cn);
        if (activityInfo != null) {
            return activityInfo.loadLabel(mPm).toString();
        }
        return "";
    }

    public List<ActivityManager.RecentTaskInfo> getTasks() {
        int currentUserId = UserHandle.USER_CURRENT;
        int flags = ActivityManager.RECENT_IGNORE_HOME_AND_RECENTS_STACK_TASKS |
                ActivityManager.RECENT_INGORE_DOCKED_STACK_TOP_TASK |
                ActivityManager.RECENT_INGORE_PINNED_STACK_TASKS |
                ActivityManager.RECENT_IGNORE_UNAVAILABLE |
                ActivityManager.RECENT_INCLUDE_PROFILES;
        flags |= ActivityManager.RECENT_WITH_EXCLUDED;
        List<ActivityManager.RecentTaskInfo> tasks = null;
        try {
            tasks = mAm.getRecentTasksForUser(NUMLASTTASKS, flags, currentUserId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get recent tasks", e);
        }

        if (tasks == null) {
            return new ArrayList<>();
        }

        Iterator<ActivityManager.RecentTaskInfo> iter = tasks.iterator();
        while (iter.hasNext()) {
            ActivityManager.RecentTaskInfo t = iter.next();

            // Remove the task if it is marked as excluded, unless it is the first most task and we
            // are requested to include it
            boolean isExcluded = (t.baseIntent.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    == Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
            if (isExcluded) {
                iter.remove();
            }
        }
        return tasks.subList(0, Math.min(tasks.size(), NUMLASTTASKS));
    }

    public ActivityInfo getActivityInfo(ComponentName cn) {
        if (mPm == null) return null;

        try {
            return mPm.getActivityInfo(cn, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean startActivityFromRecents(Context context, Task task, CharSequence taskName,
                                            ActivityOptions options) {
        if (mIam != null) {
            try {
                if (task.stackId == DOCKED_STACK_ID) {
                    // We show non-visible docked tasks in Recents, but we always want to launch
                    // them in the fullscreen stack.
                    if (options == null) {
                        options = ActivityOptions.makeBasic();
                    }
                    if (isNapp()) options.setLaunchStackId(FULLSCREEN_WORKSPACE_STACK_ID);
                }
                mIam.startActivityFromRecents(
                        task.taskId, options == null ? null : options.toBundle());
                return true;
            } catch (Exception e) {
                Log.e(TAG, context.getString(R.string.recents_launch_error_message, taskName), e);
            }
        }
        return false;
    }

    public int getSystemSetting(Context context, String setting) {
        ContentResolver cr = context.getContentResolver();
        return Settings.System.getInt(cr, setting, 0);
    }

    /* compatibility for android M */
    private List<ActivityManager.RunningTaskInfo> getRunningTasks(int numTasks) {
        if (mAm == null) return null;
        return mAm.getRunningTasks(numTasks);
    }

    /**
     * Returns the given icon for a user, badging if necessary.
     */
    public Drawable getBadgedIcon(Drawable icon, int userId) {
        if (userId != UserHandle.myUserId()) {
            icon = mPm.getUserBadgedIcon(icon, new UserHandle(userId));
        }
        return icon;
    }

    public boolean isNapp() {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    //*/ freeme.gouzhouping, 20180328. Recents, blank view.
    public Drawable getTaskThumbnail(int taskId, Resources res) {
        if (mAm == null) return null;
        ThumbnailData thumbnailData = getThumbnail(taskId, true);
        if (thumbnailData.thumbnail != null && !ActivityManager.ENABLE_TASK_SNAPSHOTS) {
            thumbnailData.thumbnail.setHasAlpha(false);
            // We use a dumb heuristic for now, if the thumbnail is purely transparent in the top
            // left pixel, then assume the whole thumbnail is transparent. Generally, proper
            // screenshots are always composed onto a bitmap that has no alpha.
            if (Color.alpha(thumbnailData.thumbnail.getPixel(0, 0)) == 0) {
                mBgProtectionCanvas.setBitmap(thumbnailData.thumbnail);
                mBgProtectionCanvas.drawRect(0, 0, thumbnailData.thumbnail.getWidth(),
                    + thumbnailData.thumbnail.getHeight(), mBgProtectionPaint);
                mBgProtectionCanvas.setBitmap(null);
                Log.e(TAG, "Invalid screenshot detected from getTaskThumbnail()");
            }
        }
        return new BitmapDrawable(thumbnailData.thumbnail);
    }

    public @NonNull
    ThumbnailData getThumbnail(int taskId, boolean reducedResolution) {
        if (mAm == null) {
            return new ThumbnailData();
        }
        // M: Slim thumbnail's size for GMO
        if (!reducedResolution && FeatureOptions.LOW_RAM_SUPPORT) reducedResolution = true;
        final ThumbnailData thumbnailData;
        if (ActivityManager.ENABLE_TASK_SNAPSHOTS) {
            ActivityManager.TaskSnapshot snapshot = null;
            try {
                snapshot = ActivityManager.getService().getTaskSnapshot(taskId, reducedResolution);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to retrieve snapshot", e);
            }
            if (snapshot != null) {
                thumbnailData = ThumbnailData.createFromTaskSnapshot(snapshot);
            } else {
                return new ThumbnailData();
            }
        } else {
            ActivityManager.TaskThumbnail taskThumbnail = mAm.getTaskThumbnail(taskId);
            if (taskThumbnail == null) {
                return new ThumbnailData();
            }

            Bitmap thumbnail = taskThumbnail.mainThumbnail;
            ParcelFileDescriptor descriptor = taskThumbnail.thumbnailFileDescriptor;
            if (thumbnail == null && descriptor != null) {
                thumbnail = BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(),
                        null, sBitmapOptions);
            }
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException e) {
                }
            }
            thumbnailData = new ThumbnailData();
            thumbnailData.thumbnail = thumbnail;
            thumbnailData.orientation = taskThumbnail.thumbnailInfo.screenOrientation;
            thumbnailData.insets.setEmpty();
        }
        return thumbnailData;
    }
    //*/

    //*/ freeme.gouzhouping. 20180604. applock
    private Bitmap mAppLockFgBmp;
    private float mAppLockTestSize;
    private String mAppLockText;
    private int mAppLockTextColor;
    private int mAppLockTextTopPadding;
    private int mAppLockBgColor;

    public Bitmap getAppLockedTaskThumbnail(int width, int height) {
        final int fgBmpWidth = mAppLockFgBmp.getWidth();
        final int fgBmpHeight = mAppLockFgBmp.getHeight();

        if (fgBmpWidth > width || fgBmpHeight > height) {
            Bitmap thumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            thumbnail.eraseColor(0xff333333);
            return thumbnail;
        }

        Rect appLockTextBounds = new Rect();
        Paint appLockPaint = new Paint();
        appLockPaint.setAntiAlias(true);
        appLockPaint.setFilterBitmap(true);
        appLockPaint.setFakeBoldText(true);
        appLockPaint.setTextSize(mAppLockTestSize);
        appLockPaint.getTextBounds(mAppLockText, 0, mAppLockText.length(), appLockTextBounds);
        final float fgBmpLeft = (width - fgBmpWidth) / 2.0f;
        final float fgBmpTop = (height - fgBmpHeight - appLockTextBounds.height() - mAppLockTextTopPadding) / 2.0f;
        final float appLockTextLeft = (width - appLockTextBounds.width()) / 2.0f;
        final float appLockTextBottom = fgBmpHeight + fgBmpTop + mAppLockTextTopPadding + appLockTextBounds.height();

        Bitmap appLockBgBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        appLockBgBmp.eraseColor(mAppLockBgColor);
        Canvas appLockCanvas = new Canvas(appLockBgBmp);
        appLockCanvas.drawBitmap(mAppLockFgBmp, fgBmpLeft, fgBmpTop, appLockPaint);
        appLockPaint.setColor(mAppLockTextColor);
        appLockCanvas.drawText(mAppLockText, appLockTextLeft, appLockTextBottom, appLockPaint);
        return appLockBgBmp;
    }

    public boolean isAppLockedPackage(String packageName) {
        return (mAm != null) && mAm.isAppLockedPackage(packageName);
    }
    //*/

    private Context mContext;
    public void removeTask(int taskId, String pkg) {
        removeTask(taskId);
        if (FreemeWhiteListHelper.hasAltas()) {
            removeTaskByForceStop(pkg);
        }
    }
    private void removeTaskByForceStop(String pkg) {
        if (mAm == null) return;
        if (FreemeWhiteListHelper.getInstance(mContext).isNeedForceStop(pkg)) {
            mAm.forceStopPackageAsUser(pkg, UserHandle.USER_CURRENT);
        }
    }

}
