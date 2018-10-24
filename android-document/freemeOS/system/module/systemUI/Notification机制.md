NotificationManager.java中：

    public void notify(int id, Notification notification)
    {
        notify(null, id, notification);
    }
###--->>

    public void notify(String tag, int id, Notification notification)
    {
        notifyAsUser(tag, id, notification, new UserHandle(UserHandle.myUserId()));
    }
###--->>

    public void notifyAsUser(String tag, int id, Notification notification, UserHandle user)
    {
        、、、、、、
        try {
            service.enqueueNotificationWithTag(pkg, mContext.getOpPackageName(), tag, id,
                    copy, idOut, user.getIdentifier());
            if (id != idOut[0]) {
                Log.w(TAG, "notify: id corrupted: sent " + id + ", got back " + idOut[0]);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
NotificationManagerService.java中：

        @Override
        public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id,
                Notification notification, int[] idOut, int userId) throws RemoteException {
            enqueueNotificationInternal(pkg, opPkg, Binder.getCallingUid(),
                    Binder.getCallingPid(), tag, id, notification, idOut, userId);
        }
###--->>

    void enqueueNotificationInternal(final String pkg, final String opPkg, final int callingUid,
            final int callingPid, final String tag, final int id, final Notification notification,
            int[] idOut, int incomingUserId) {
        if (DBG) {
            Slog.v(TAG, "enqueueNotificationInternal: pkg=" + pkg + " id=" + id
                    + " notification=" + notification);
        }
        /// M: Just filter for special notification flow, normal can ignore it. @{
        boolean foundTarget = false;
        if (pkg != null && pkg.contains(".stub") && notification != null) {
            String contentTitle = notification.extras != null ?
                    notification.extras.getString(Notification.EXTRA_TITLE) : " ";
            if (contentTitle != null && contentTitle.startsWith("notify#")) {
                foundTarget = true;
                Slog.d(TAG, "enqueueNotification, found notification, callingUid: " + callingUid
                        + ", callingPid: " + callingPid + ", pkg: " + pkg
                        + ", id: " + id + ", tag: " + tag);
            }
        }
        /// @}
        checkCallerIsSystemOrSameApp(pkg);
        // 校验UID
        final boolean isSystemNotification = isUidSystem(callingUid) || ("android".equals(pkg));
        final boolean isNotificationFromListener = mListeners.isListenerPackage(pkg);

        final int userId = ActivityManager.handleIncomingUser(callingPid,
                callingUid, incomingUserId, true, false, "enqueueNotification", pkg);
        final UserHandle user = new UserHandle(userId);

        // Fix the notification as best we can.
        try {
            final ApplicationInfo ai = getContext().getPackageManager().getApplicationInfoAsUser(
                    pkg, PackageManager.MATCH_DEBUG_TRIAGED_MISSING,
                    (userId == UserHandle.USER_ALL) ? UserHandle.USER_SYSTEM : userId);
            Notification.addFieldsFromContext(ai, userId, notification);
        } catch (NameNotFoundException e) {
            Slog.e(TAG, "Cannot create a context for sending app", e);
            return;
        }

        mUsageStats.registerEnqueuedByApp(pkg);

        // Limit the number of notifications that any given package except the android
        // package or a registered listener can enqueue.  Prevents DOS attacks and deals with leaks.
        // 这里会做一个限制，除了系统级别的应用之外，其他应用的notification数量会做限制,用来防止DOS攻击导致的泄露
        if (!isSystemNotification && !isNotificationFromListener) {
            synchronized (mNotificationList) {
                final float appEnqueueRate = mUsageStats.getAppEnqueueRate(pkg);
                if (appEnqueueRate > mMaxPackageEnqueueRate) {
                    mUsageStats.registerOverRateQuota(pkg);
                    final long now = SystemClock.elapsedRealtime();
                    if ((now - mLastOverRateLogTime) > MIN_PACKAGE_OVERRATE_LOG_INTERVAL) {
                        Slog.e(TAG, "Package enqueue rate is " + appEnqueueRate
                                + ". Shedding events. package=" + pkg);
                        mLastOverRateLogTime = now;
                    }
                    return;
                }

                int count = 0;
                final int N = mNotificationList.size();
                for (int i=0; i<N; i++) {
                    final NotificationRecord r = mNotificationList.get(i);
                    if (r.sbn.getPackageName().equals(pkg) && r.sbn.getUserId() == userId) {
                        if (r.sbn.getId() == id && TextUtils.equals(r.sbn.getTag(), tag)) {
                            break;  // Allow updating existing notification
                        }
                        count++;
                        if (count >= MAX_PACKAGE_NOTIFICATIONS) {    //同一个应用发送notification数量不能超过50
                            mUsageStats.registerOverCountQuota(pkg);
                            Slog.e(TAG, "Package has already posted " + count
                                    + " notifications.  Not showing more.  package=" + pkg);
                            return;
                        }
                    }
                }
            }
        }

        if (pkg == null || notification == null) {    //通知不能为空
            throw new IllegalArgumentException("null not allowed: pkg=" + pkg
                    + " id=" + id + " notification=" + notification);
        }

        // Whitelist pending intents.
        if (notification.allPendingIntents != null) {
            final int intentCount = notification.allPendingIntents.size();
            if (intentCount > 0) {
                final ActivityManagerInternal am = LocalServices
                        .getService(ActivityManagerInternal.class);
                final long duration = LocalServices.getService(
                        DeviceIdleController.LocalService.class).getNotificationWhitelistDuration();
                for (int i = 0; i < intentCount; i++) {
                    PendingIntent pendingIntent = notification.allPendingIntents.valueAt(i);
                    if (pendingIntent != null) {
                        am.setPendingIntentWhitelistDuration(pendingIntent.getTarget(), duration);
                    }
                }
            }
        }

        // Sanitize inputs
        notification.priority = clamp(notification.priority, Notification.PRIORITY_MIN,
                Notification.PRIORITY_MAX);
         
        // 验证完条件后，将前面传递进来的Notification封装成一个StatusBarNotification对象
        // setup local book-keeping
        final StatusBarNotification n = new StatusBarNotification(
                pkg, opPkg, id, tag, callingUid, callingPid, 0, notification,
                user);
        // 封装NotificationRecord对象
        final NotificationRecord r = new NotificationRecord(getContext(), n);
        //开启线程，异步处理
        mHandler.post(new EnqueueNotificationRunnable(userId, r));

        idOut[0] = id;

        /// M: Just filter for special notification flow, normal can ignore it. @{
        if (foundTarget) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                // ignore it.
            }
        }
        /// @}
    }
###--->>

    private class EnqueueNotificationRunnable implements Runnable {
        private final NotificationRecord r;
        private final int userId;

        EnqueueNotificationRunnable(int userId, NotificationRecord r) {
            this.userId = userId;
            this.r = r;
        };

        @Override
        public void run() {

            synchronized (mNotificationList) {
                final StatusBarNotification n = r.sbn;
                if (DBG) Slog.d(TAG, "EnqueueNotificationRunnable.run for: " + n.getKey());
                NotificationRecord old = mNotificationsByKey.get(n.getKey());
                if (old != null) {
                    //保留上一记录的排名信息
                    // Retain ranking information from previous record
                    r.copyRankingInformation(old);
                }

                final int callingUid = n.getUid();
                final int callingPid = n.getInitialPid();
                final Notification notification = n.getNotification();
                final String pkg = n.getPackageName();
                final int id = n.getId();
                final String tag = n.getTag();
                final boolean isSystemNotification = isUidSystem(callingUid) ||
                        ("android".equals(pkg));
                //如果我们可以避免提取信号，请处理分组通知并提前保释
                // Handle grouped notifications and bail out early if we
                // can to avoid extracting signals.
                handleGroupedNotificationLocked(r, old, callingUid, callingPid);

                ////这个条件是一个肮脏的黑客，可以代替下载管理器限制日志记录，而不影响其他应用
                // This conditional is a dirty hack to limit the logging done on
                //     behalf of the download manager without affecting other apps.
                if (!pkg.equals("com.android.providers.downloads")
                        || Log.isLoggable("DownloadManager", Log.VERBOSE)) {
                    int enqueueStatus = EVENTLOG_ENQUEUE_STATUS_NEW;
                    if (old != null) {
                        enqueueStatus = EVENTLOG_ENQUEUE_STATUS_UPDATE;
                    }
                    EventLogTags.writeNotificationEnqueue(callingUid, callingPid,
                            pkg, id, tag, userId, notification.toString(),
                            enqueueStatus);
                }

                mRankingHelper.extractSignals(r);

                final boolean isPackageSuspended = isPackageSuspendedForUser(pkg, callingUid);

                // blocked apps 判断pkg是否可以显示通知
                // blocked apps
                if (r.getImportance() == NotificationListenerService.Ranking.IMPORTANCE_NONE
                        || !noteNotificationOp(pkg, callingUid) || isPackageSuspended) {
                    if (!isSystemNotification) {    //不拦截系统通知
                        if (isPackageSuspended) {
                            Slog.e(TAG, "Suppressing notification from package due to package "
                                    + "suspended by administrator.");
                            mUsageStats.registerSuspendedByAdmin(r);
                        } else {
                            Slog.e(TAG, "Suppressing notification from package by user request.");
                            mUsageStats.registerBlocked(r);
                        }
                        return;
                    }
                }

                //*/ freeme.tangxiaohui, 20170321. notification manager in Security.apk. ZYSE050501
                if (SystemProperties.getBoolean("ro.fo_security_notifi",false)) {
                    if (!getDroiSecurityNoteNotificationOp(pkg, callingUid) && (!isSystemNotification)) {
                        if (isPackageSuspended) {
                            mUsageStats.registerSuspendedByAdmin(r);
                            Slog.e("antipush", "DroiSecurity Suppressing notification from package due to package " + pkg + " suspended by administrator.");
                        } else {
                            mUsageStats.registerBlocked(r);
                            Slog.e("antipush", "DroiSecurity Suppressing notification from package " + pkg + " by user request.");
                        }
                        return;
                    }
                }
                //*/

                //告诉排序器服务有关通知
                // tell the ranker service about the notification
                if (mRankerServices.isEnabled()) {
                    mRankerServices.onNotificationEnqueued(r);
                    // TODO delay the code below here for 100ms or until there is an answer
                }


                // 获取是否已经发送过此notification
                //对于新来通知和已经存在的通知进行区别处理
                int index = indexOfNotificationLocked(n.getKey());
                if (index < 0) {
                    // 如果是新发送的notification,就走新增流程
                    mNotificationList.add(r);
                    mUsageStats.registerPostedByApp(r);
                } else {
                    //如果有发送过，就获取oldNtificationRecord，后面走更新流程 mStatusBar.updateNotification(r.statusBarKey, n)
                    old = mNotificationList.get(index);
                    mNotificationList.set(index, r);
                    mUsageStats.registerUpdatedByApp(r, old);
                    // Make sure we don't lose the foreground service state.
                    notification.flags |=
                            old.getNotification().flags & Notification.FLAG_FOREGROUND_SERVICE;
                    r.isUpdate = true;
                }

                mNotificationsByKey.put(n.getKey(), r);

                //确保这是一个前台服务，设置正确的附加标志
                // Ensure if this is a foreground service that the proper additional
                // flags are set.
                if ((notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) {
                    notification.flags |= Notification.FLAG_ONGOING_EVENT
                            | Notification.FLAG_NO_CLEAR;
                }

                applyZenModeLocked(r);
                mRankingHelper.sort(mNotificationList);    //将mNotificationList排序

                // 如果notification设置了smallIcon，调用所有NotificationListeners的notifyPostedLocked方法，
                // 通知有新的notification，传入的参数为上面封装成的StatusBarNotification对象
                if (notification.getSmallIcon() != null
                        /// M: Do not show notifications if FLAG_HIDE_NOTIFICATION is on
                        && (notification.flags & Notification.FLAG_HIDE_NOTIFICATION) == 0) {
                    StatusBarNotification oldSbn = (old != null) ? old.sbn : null;
                    mListeners.notifyPostedLocked(n, oldSbn);
                } else {
                    Slog.e(TAG, "Not posting notification without small icon: " + notification);
                    if (old != null && !old.isCanceled) {
                        mListeners.notifyRemovedLocked(n);
                    }
                    //注意：在将来的版本中，我们将在此保释使我们不播放声音，显示灯等无效通知
                    // ATTENTION: in a future release we will bail out here
                    // so that we do not play sounds, show lights, etc. for invalid
                    // notifications
                    Slog.e(TAG, "WARNING: In a future release this will crash the app: "
                            + n.getPackageName());
                }

                // buzzBeepBlinkLocked方法负责对消息进行处理
                // 通知status bar显示该notification,确认是否需要声音，震动和闪光，如果需要，那么就发出声音，震动和闪光
                buzzBeepBlinkLocked(r);
            }
        }
    }
进入RankingHelper.java类中，看看如何进行排序：

    public void sort(ArrayList<NotificationRecord> notificationList) {
        final int N = notificationList.size();
        // clear global sort keys
        for (int i = N - 1; i >= 0; i--) {
            notificationList.get(i).setGlobalSortKey(null);
        }

        // rank each record individually
        Collections.sort(notificationList, mPreliminaryComparator);

        synchronized (mProxyByGroupTmp) {
            // record individual ranking result and nominate proxies for each group
            for (int i = N - 1; i >= 0; i--) {
                final NotificationRecord record = notificationList.get(i);
                record.setAuthoritativeRank(i);
                final String groupKey = record.getGroupKey();
                boolean isGroupSummary = record.getNotification().isGroupSummary();
                if (isGroupSummary || !mProxyByGroupTmp.containsKey(groupKey)) {
                    mProxyByGroupTmp.put(groupKey, record);
                }
            }
            // assign global sort key:
            //   is_recently_intrusive:group_rank:is_group_summary:group_sort_key:rank
            for (int i = 0; i < N; i++) {
                final NotificationRecord record = notificationList.get(i);
                NotificationRecord groupProxy = mProxyByGroupTmp.get(record.getGroupKey());
                String groupSortKey = record.getNotification().getSortKey();

                // We need to make sure the developer provided group sort key (gsk) is handled
                // correctly:
                //   gsk="" < gsk=non-null-string < gsk=null
                //
                // We enforce this by using different prefixes for these three cases.
                String groupSortKeyPortion;
                if (groupSortKey == null) {
                    groupSortKeyPortion = "nsk";
                } else if (groupSortKey.equals("")) {
                    groupSortKeyPortion = "esk";
                } else {
                    groupSortKeyPortion = "gsk=" + groupSortKey;
                }

                boolean isGroupSummary = record.getNotification().isGroupSummary();
                record.setGlobalSortKey(
                        String.format("intrsv=%c:grnk=0x%04x:gsmry=%c:%s:rnk=0x%04x",
                        record.isRecentlyIntrusive() ? '0' : '1',
                        groupProxy.getAuthoritativeRank(),
                        isGroupSummary ? '0' : '1',
                        groupSortKeyPortion,
                        record.getAuthoritativeRank()));
            }
            mProxyByGroupTmp.clear();
        }

        // Do a second ranking pass, using group proxies
        Collections.sort(notificationList, mFinalComparator);
    }
###--->>查看Collections.sort(notificationList, mPreliminaryComparator);

重点在于比较器mPreliminaryComparator，我们来看下代码

路径：frameworks/base/services/core/java/com/android/server/notification/NotificationComparator.java

进到里面的compare（）方法来查看，以下就是具体的排序规则

    @Override
    public int compare(NotificationRecord left, NotificationRecord right) {
        final int leftImportance = left.getImportance();
        final int rightImportance = right.getImportance();
        if (leftImportance != rightImportance) {
            // by importance, high to low
            return -1 * Integer.compare(leftImportance, rightImportance);
        }

        // Whether or not the notification can bypass DND.
        final int leftPackagePriority = left.getPackagePriority();
        final int rightPackagePriority = right.getPackagePriority();
        if (leftPackagePriority != rightPackagePriority) {
            // by priority, high to low
            return -1 * Integer.compare(leftPackagePriority, rightPackagePriority);
        }

        final int leftPriority = left.sbn.getNotification().priority;
        final int rightPriority = right.sbn.getNotification().priority;
        if (leftPriority != rightPriority) {
            // by priority, high to low
            return -1 * Integer.compare(leftPriority, rightPriority);
        }

        final float leftPeople = left.getContactAffinity();
        final float rightPeople = right.getContactAffinity();
        if (leftPeople != rightPeople) {
            // by contact proximity, close to far
            return -1 * Float.compare(leftPeople, rightPeople);
        }

        // then break ties by time, most recent first
        return -1 * Long.compare(left.getRankingTimeMs(), right.getRankingTimeMs());
    }
可以看到，比较器根据leftImportance ，leftPackagePriority ，leftPriority ，leftPeople 这四个参数做出排序，工作基本上算完成了

先看下mListeners的定义mListeners = new NotificationListeners();
再回到SytemUI当中去，发现在BaseStatusBar.java中有监听listen：

    private final NotificationListenerService mNotificationListener =
            new NotificationListenerService() {
        、、、、、、
        @Override
        public void onNotificationPosted(final StatusBarNotification sbn,
                final RankingMap rankingMap) {
            /// M: Enable this log for unusual case debug.
            /*if (DEBUG)*/ Log.d(TAG, "onNotificationPosted: " + sbn);
            if (sbn != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        processForRemoteInput(sbn.getNotification());
                        String key = sbn.getKey();
                        mKeysKeptForRemoteInput.remove(key);
                        boolean isUpdate = mNotificationData.get(key) != null;
                        // In case we don't allow child notifications, we ignore children of
                        // notifications that have a summary, since we're not going to show them
                        // anyway. This is true also when the summary is canceled,
                        // because children are automatically canceled by NoMan in that case.
                        if (!ENABLE_CHILD_NOTIFICATIONS
                            && mGroupManager.isChildInGroupWithSummary(sbn)) {
                            if (DEBUG) {
                                Log.d(TAG, "Ignoring group child due to existing summary: " + sbn);
                            }

                            // Remove existing notification to avoid stale data.
                            if (isUpdate) {
                                removeNotification(key, rankingMap);
                            } else {
                                mNotificationData.updateRanking(rankingMap);
                            }
                            return;
                        }
                        if (isUpdate) {
                            updateNotification(sbn, rankingMap);
                        } else {
                            addNotification(sbn, rankingMap, null /* oldEntry */);
                        }
                    }
                });
            }
        }
        、、、、、、
调用到addNotification(sbn, rankingMap, null /* oldEntry */);
为抽象方法：

    public abstract void addNotification(StatusBarNotification notification,
            RankingMap ranking, Entry oldEntry);
直接去PhoneStatusBar.java中看实现：

    @Override
    public void addNotification(StatusBarNotification notification, RankingMap ranking,
            Entry oldEntry) {
        、、、、、、
        addNotificationViews(shadeEntry, ranking);
        // Recalculate the position of the sliding windows and the titles.
        setAreThereNotifications();
    }
接着又回调到BaseStatusBar.java

    protected void addNotificationViews(Entry entry, RankingMap ranking) {
        if (entry == null) {
            return;
        }
        // Add the expanded view and icon.
        mNotificationData.add(entry, ranking);
        updateNotifications();
    }
接着再来到PhoneStatusBar.java中：

    @Override
    protected void updateNotifications() {
        mNotificationData.filterAndSort();

        updateNotificationShade();
        mIconController.updateNotificationIcons(mNotificationData);
    }
来到StatusBarIconController.java中：

    public void updateNotificationIcons(NotificationData notificationData) {
        mNotificationIconAreaController.updateNotificationIcons(notificationData);
    }
看看mNotificationIconAreaController的定义：
    private NotificationIconAreaController mNotificationIconAreaController;

        mNotificationIconAreaController = SystemUIFactory.getInstance()
                .createNotificationIconAreaController(context, phoneStatusBar);    /×××又是工厂模式？！×××/
最后到NotificationIconAreaController.java中：

    /**
     * Updates the notifications with the given list of notifications to display.
     */
    public void updateNotificationIcons(NotificationData notificationData) {
        final LinearLayout.LayoutParams params = generateIconLayoutParams();

        ArrayList<NotificationData.Entry> activeNotifications =
                notificationData.getActiveNotifications();
        final int size = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(size);

        // Filter out ambient notifications and notification children.
        for (int i = 0; i < size; i++) {
            NotificationData.Entry ent = activeNotifications.get(i);
            if (shouldShowNotification(ent, notificationData)) {
                toShow.add(ent.icon);
            }
        }

        ArrayList<View> toRemove = new ArrayList<>();
        for (int i = 0; i < mNotificationIcons.getChildCount(); i++) {
            View child = mNotificationIcons.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }

        final int toRemoveCount = toRemove.size();
        for (int i = 0; i < toRemoveCount; i++) {
            mNotificationIcons.removeView(toRemove.get(i));
        }

        for (int i = 0; i < toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mNotificationIcons.addView(v, i, params);
            }
        }

        // Re-sort notification icons
        final int childCount = mNotificationIcons.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View actual = mNotificationIcons.getChildAt(i);
            StatusBarIconView expected = toShow.get(i);
            if (actual == expected) {
                continue;
            }
            mNotificationIcons.removeView(expected);
            mNotificationIcons.addView(expected, i);
        }

        applyNotificationIconsTint();
    }
注：mNotificationIcons的定义

        mNotificationIcons =
                (IconMerger) mNotificationIconArea.findViewById(R.id.notificationIcons);
id/notificationIcons定义在布局文件notification_icon_area.xml中
看看NotificationIconAreaController.java在怎样处理这个布局文件：

    protected View mNotificationIconArea;

    /**
     * Initializes the views that will represent the notification area.
     */
    protected void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        mNotificationIconArea = inflateIconArea(layoutInflater);
    、、、、、、
看下方法inflateIconArea(layoutInflater)：
    protected View inflateIconArea(LayoutInflater inflater) {
        return inflater.inflate(R.layout.notification_icon_area, null);
    }
通过这里对mNotificationIconArea进行了赋值
再看下怎么把它添加到布局里面的：
在StatusBarIconController.java里面：

    public StatusBarIconController(Context context, View statusBar, View keyguardStatusBar,
            PhoneStatusBar phoneStatusBar) {
        、、、、、、
        mNotificationIconAreaInner =
                mNotificationIconAreaController.getNotificationInnerAreaView();

        ViewGroup notificationIconArea =
                (ViewGroup) statusBar.findViewById(R.id.notification_icon_area);
        notificationIconArea.addView(mNotificationIconAreaInner);
        、、、、、、
注：notification_icon_area在布局status_bar.xml中
可以看出通过notificationIconArea.addView(mNotificationIconAreaInner)把通知mNotificationIconAreaInner添加到下拉状态栏中，这些都是在StatusBarIconController.java的构造方法中事先完成的


 先来区分以下状态栏和状态条的区别：
 
    1、状态条就是手机屏幕最上方的一个条形状的区域；
          在状态条有好多信息量：比如usb连接图标，手机信号图标，电池电量图标，时间图标等等；
    2、状态栏就是手从状态条滑下来的可以伸缩的view；
          在状态栏中一般有两类（使用FLAG_标记）：
          （1）正在进行的程序；
          （2）是通知事件；
 
     大概来描述创建一个Notification传送的信息有：
     
    1、一个状态条图标；
    2、在拉伸的状态栏窗口中显示带有大标题，小标题，图标的信息，并且有处理该点击事件：比如调用该程序的入口类；
    3、闪光，LED，或者震动；
 
      快速创建一个Notification的步骤简单可以分为以下四步：
      第一步：通过getSystemService（）方法得到NotificationManager对象；
      第二步：对Notification的一些属性进行设置比如：内容，图标，标题，相应notification的动作进行处理等等；
      第三步：通过NotificationManager对象的notify（）方法来执行一个notification的快讯；
      第四步：通过NotificationManager对象的cancel（）方法来取消一个notificatioin的快讯；
 
     下面对Notification类中的一些常量，字段，方法简单介绍一下：
     常量：
        DEFAULT_ALL                  使用所有默认值，比如声音，震动，闪屏等等
        DEFAULT_LIGHTS            使用默认闪光提示
        DEFAULT_SOUNDS         使用默认提示声音
        DEFAULT_VIBRATE         使用默认手机震动 
      【说明】：加入手机震动，一定要在manifest.xml中加入权限：
                         <uses-permission android:name="android.permission.VIBRATE" />
        以上的效果常量可以叠加,即通过
                mNotifaction.defaults =DEFAULT_SOUND  |  DEFAULT_VIBRATE ;  
            或mNotifaction.defaults |=DEFAULT_SOUND   (最好在真机上测试，震动效果模拟器上没有)
 
        //设置flag位
           FLAG_AUTO_CANCEL          该通知能被状态栏的清除按钮给清除掉
        FLAG_NO_CLEAR                  该通知能被状态栏的清除按钮给清除掉
        FLAG_ONGOING_EVENT      通知放置在正在运行
        FLAG_INSISTENT                    是否一直进行，比如音乐一直播放，知道用户响应
 
      常用字段：
           contentIntent                  设置PendingIntent对象，点击时发送该Intent
           defaults                             添加默认效果
           flags                                  设置flag位，例如FLAG_NO_CLEAR等
           icon                                  设置图标
           sound                                设置声音
           tickerText                        显示在状态栏中的文字
           when                                发送此通知的时间戳
 
Notification.build构造Notification方法介绍：   
     void setLatestEventInfo(Context context , CharSequencecontentTitle,CharSequence  contentText,PendingIntent contentIntent)  
          
        功能： 显示在拉伸状态栏中的Notification属性，点击后将发送PendingIntent对象
        参数： context             上下文环境
                      contentTitle      状态栏中的大标题
                      contentText      状态栏中的小标题
                      contentIntent    点击后将发送PendingIntent对象
      说明：要是在Notification中加入图标，在状态栏和状态条中显示图标一定要用这个方法，否则报错！
 
      最后说一下NotificationManager类的常用方法：
             通过获取系统服务来获取该对象：           
                NotificationManager mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE) ;
 
      NotificationManager常用方法介绍：
               public  void cancelAll()                                                          移除所有通知 (只是针对当前Context下的Notification)
               public  void cancel(int id)                                                      移除标记为id的通知 (只是针对当前Context下的所有Notification)
               public  void notify(String tag ,int id, Notification notification) 将通知加入状态栏, 标签为tag，标记为id
               public  void notify(int id, Notification notification)                   将通知加入状态栏,，标记为id
 

