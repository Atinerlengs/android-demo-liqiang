分几个方面进行阐述

一、加载布局

二、恢复副本信息


一 加载布局

系统启动SystemUIService：

    @Override
    public void onCreate() {
        super.onCreate();
        ((SystemUIApplication) getApplication()).startServicesIfNeeded();
    }
调用startServicesIfNeeded中mServices[i].start();，看下mServices：

        final int N = services.length;
        for (int i=0; i<N; i++) {
            Class<?> cl = services[i];
            if (DEBUG) Log.d(TAG, "loading: " + cl);
            try {
                Object newService = SystemUIFactory.getInstance().createInstance(cl);    ###×××有待弄清SystemUIFactory生成相应service的原理×××###
                mServices[i] = (SystemUI) ((newService == null) ? cl.newInstance() : newService);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            }

            mServices[i].mContext = this;
            mServices[i].mComponents = mComponents;
            if (DEBUG) Log.d(TAG, "running: " + mServices[i]);
            mServices[i].start();
            、、、、、、
数组mServices[i]中有一个com.android.systemui.statusbar.SystemBars.class：

    @Override
    public void start() {
        if (DEBUG) Log.d(TAG, "start");
        mServiceMonitor = new ServiceMonitor(TAG, DEBUG,
                mContext, Settings.Secure.BAR_SERVICE_COMPONENT, this);
        mServiceMonitor.start();  // will call onNoService if no remote service is found
    }
ServiceMonitor.java中：

    public void start() {
        // listen for setting changes
        ContentResolver cr = mContext.getContentResolver();
        cr.registerContentObserver(Settings.Secure.getUriFor(mSettingKey),
                false /*notifyForDescendents*/, mSettingObserver, UserHandle.USER_ALL);

        // listen for package/component changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        mContext.registerReceiver(mBroadcastReceiver, filter);

        mHandler.sendEmptyMessage(MSG_START_SERVICE);
    }
自己接收：

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_START_SERVICE:
                    startService();
                    break;
                case MSG_CONTINUE_START_SERVICE:
                    continueStartService();
                    break;
                case MSG_STOP_SERVICE:
                    stopService();
                    break;
                case MSG_PACKAGE_INTENT:
                    packageIntent((Intent)msg.obj);
                    break;
                case MSG_CHECK_BOUND:
                    checkBound();
                    break;
                case MSG_SERVICE_DISCONNECTED:
                    serviceDisconnected((ComponentName)msg.obj);
                    break;
            }
        }
    };
发消息MSG_START_SERVICE，调用startService();，如下：

    private void startService() {
        mServiceName = getComponentNameFromSetting();
        if (mDebug) Log.d(mTag, "startService mServiceName=" + mServiceName);
        if (mServiceName == null) {
            mBound = false;
            mCallbacks.onNoService();
        } else {
            long delay = mCallbacks.onServiceStartAttempt();
            mHandler.sendEmptyMessageDelayed(MSG_CONTINUE_START_SERVICE, delay);
        }
    }
回调mCallbacks.onNoService();，看下mCallbacks的定义：

    public ServiceMonitor(String ownerTag, boolean debug,
            Context context, String settingKey, Callbacks callbacks) {
        mTag = ownerTag + ".ServiceMonitor";
        mDebug = debug;
        mContext = context;
        mSettingKey = settingKey;
        mCallbacks = callbacks;
    }
在构造方法中，接着看在哪里实例化了ServiceMonitor，发现：

在SystemBars.java中
    @Override
    public void start() {
        if (DEBUG) Log.d(TAG, "start");
        mServiceMonitor = new ServiceMonitor(TAG, DEBUG,
                mContext, Settings.Secure.BAR_SERVICE_COMPONENT, this);
        mServiceMonitor.start();  // will call onNoService if no remote service is found
    }
并且public class SystemBars extends SystemUI implements ServiceMonitor.Callbacks {、、、
直接看：

    @Override
    public void onNoService() {
        if (DEBUG) Log.d(TAG, "onNoService");
        createStatusBarFromConfig();  // fallback to using an in-process implementation
    }
接着：

    private void createStatusBarFromConfig() {
        if (DEBUG) Log.d(TAG, "createStatusBarFromConfig");
        final String clsName = mContext.getString(R.string.config_statusBarComponent);
        if (clsName == null || clsName.length() == 0) {
            throw andLog("No status bar component configured", null);
        }
        Class<?> cls = null;
        try {
            cls = mContext.getClassLoader().loadClass(clsName);
        } catch (Throwable t) {
            throw andLog("Error loading status bar component: " + clsName, t);
        }
        try {
            mStatusBar = (BaseStatusBar) cls.newInstance();
        } catch (Throwable t) {
            throw andLog("Error creating status bar component: " + clsName, t);
        }
        mStatusBar.mContext = mContext;
        mStatusBar.mComponents = mComponents;
        mStatusBar.start();
        if (DEBUG) Log.d(TAG, "started " + mStatusBar.getClass().getSimpleName());
    }
这里mStatusBar.start();，说明开始调用到类BaseStatusBar.java：

    public void start() {
        /* 由于状态栏的窗口不属于任何一个Activity，所以需要使用WindowManager
          进行窗口的创建 */
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        /* 状态栏的存在对窗口布局有着重要的影响。因此状态栏中所发生的变化有必要通知给WMS */
        mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        mDisplay = mWindowManager.getDefaultDisplay();
        mDevicePolicyManager = (DevicePolicyManager)mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        mNotificationData = new NotificationData(this);

        mAccessibilityManager = (AccessibilityManager)
                mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);

        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.checkService(DreamService.DREAM_SERVICE));
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

    /*mProvisioningOberver是一个ContentObserver。
      它负责监听Settings.Global.DEVICE_PROVISIONED设置的变化。这一设置表示此设备是否已经
      归属于某一个用户。比如当用户打开一个新购买的设备时，初始化设置向导将会引导用户阅读使用条款、
      设置帐户等一系列的初始化操作。在初始化设置向导完成之前，
      Settings.Global.DEVICE_PROVISIONED的值为false，表示这台设备并未归属于某
      一个用户。
      当设备并未归属于某以用户时，状态栏会禁用一些功能以避免信息的泄露。mProvisioningObserver
      即是用来监听设备归属状态的变化，以禁用或启用某些功能 */
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED), true,
                mSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ZEN_MODE), false,
                mSettingsObserver);
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS), false,
                mSettingsObserver,
                UserHandle.USER_ALL);
        if (ENABLE_LOCK_SCREEN_ALLOW_REMOTE_INPUT) {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_ALLOW_REMOTE_INPUT),
                    false,
                    mSettingsObserver,
                    UserHandle.USER_ALL);
        }

        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS),
                true,
                mLockscreenSettingsObserver,
                UserHandle.USER_ALL);

  /* ① 获取IStatusBarService的实例。IStatusBarService是一个系统服务，由ServerThread
      启动并常驻system_server进程中。IStatusBarService为那些对状态栏感兴趣的其他系统服务定
      义了一系列API，然而对SystemUI而言，它更像是一个客户端。因为IStatusBarService会将操作
      状态栏的请求发送给SystemUI，并由后者完成请求 */
        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));

        mRecents = getComponent(Recents.class);

        final Configuration currentConfig = mContext.getResources().getConfiguration();
        mLocale = currentConfig.locale;
        mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(mLocale);
        mFontScale = currentConfig.fontScale;
        mDensity = currentConfig.densityDpi;

        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mLockPatternUtils = new LockPatternUtils(mContext);

        //和IStatusBarService进行交互的IBinder
        // Connect in to the status bar manager service
        mCommandQueue = new CommandQueue(this);

        int[] switches = new int[9];
        ArrayList<IBinder> binders = new ArrayList<IBinder>();
        ArrayList<String> iconSlots = new ArrayList<>();
        ArrayList<StatusBarIcon> icons = new ArrayList<>();
        Rect fullscreenStackBounds = new Rect();
        Rect dockedStackBounds = new Rect();
   /* 随后BaseStatusBar将自己注册到IStatusBarService之中。以此声明本实例才是状态栏的真正
      实现者，IStatusBarService会将其所接受到的请求转发给本实例。
      “天有不测风云”，SystemUI难免会因为某些原因使得其意外终止。而状态栏中所显示的信息并不属于状态
      栏自己，而是属于其他的应用程序或是其他的系统服务。因此当SystemUI重新启动时，便需要恢复其
      终止前所显示的信息以避免信息的丢失。为此，IStatusBarService中保存了所有的需要状态栏进行显
      示的信息的副本，并在新的状态栏实例启动后，这些副本将会伴随着注册的过程传递给状态栏并进行显示，
      从而避免了信息的丢失。
      从代码分析的角度来看，这一从IstatusBarService中取回信息副本的过程正好完整地体现了状态栏
      所能显示的信息的类型*/
 
    /*iconList是向IStatusBarService进行注册的参数之一。它保存了用于显示在状态栏的系统状态
      区中的状态图标列表。在完成注册之后，IStatusBarService将会在其中填充两个数组，一个字符串
      数组用于表示状态的名称，一个StatusBarIcon类型的数组用于存储需要显示的图标资源。
    */
    
   StatusBarIconList iconList = new StatusBarIconList();
   
    /*notificationKeys和StatusBarNotification则存储了需要显示在状态栏的通知区中通知信息。
      前者存储了一个用Binder表示的通知发送者的ID列表。而notifications则存储了通知列表。二者
      通过索引号一一对应。关于通知的工作原理将在7.2.2节介绍 */
   ArrayList<IBinder> notificationKeys = newArrayList<IBinder>();
   ArrayList<StatusBarNotification> notifications
                                    = newArrayList<StatusBarNotification>();
                                    
    /*mCommandQueue是CommandQueue类的一个实例。CommandQueue继承自IStatusBar.Stub。
      因此它是IStatusBar的Bn端。在完成注册后，这一Binder对象的Bp端将会保存在
     IStatusBarService之中。因此它是IStatusBarService与BaseStatusBar进行通信的桥梁。
      */
    mCommandQueue= new CommandQueue(this, iconList);
    /*switches则存储了一些杂项：禁用功能列表，SystemUIVisiblity，是否在导航栏中显示虚拟的
      菜单键，输入法窗口是否可见、输入法窗口是否消费BACK键、是否接入了实体键盘、实体键盘是否被启用。
      在后文中将会介绍它们的具体影响 */
      
    int[]switches = new int[7];
   ArrayList<IBinder> binders = new ArrayList<IBinder>();
        try {
            //这一步实际上就是将PhoneStatusBar的实现的CommandQueue其中包含
            //callbacks传递给StatusbarManagerService使用
            mBarService.registerStatusBar(mCommandQueue, iconSlots, icons, switches, binders,
                    fullscreenStackBounds, dockedStackBounds);
        } catch (RemoteException ex) {
            // If the system process isn't there we're doomed anyway.
        }

  // 创建状态栏与导航栏的窗口。由于创建状态栏与导航栏的窗口涉及到控件树的创建，因此它由子类
    PhoneStatusBar或TabletStatusBar实现，以根据不同的布局方案选择创建不同的窗口与控件树 */
        createAndAddWindows();

        mSettingsObserver.onChange(false); // set up
    /*应用来自IStatusBarService中所获取的信息
      mCommandQueue已经注册到IStatusBarService中，状态栏与导航栏的窗口与控件树也都创建完毕
      因此接下来的任务就是应用从IStatusBarService中所获取的信息 */
        disable(switches[0], switches[6], false /* animate */);     // 禁用某些功能
        setSystemUiVisibility(switches[1], switches[7], switches[8], 0xffffffff,
                fullscreenStackBounds, dockedStackBounds);    // 设置SystemUIVisibility
        topAppWindowChanged(switches[2] != 0);     // 设置菜单键的可见性
        // 根据输入法窗口的可见性调整导航栏的样式
        // StatusBarManagerService has a back up of IME token and it's restored here.
        setImeWindowStatus(binders.get(0), switches[3], switches[4], switches[5] != 0);

        // 依次向系统状态区添加状态图标
        //iconSlots是通过StatusBarManagerService.java的setIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription)进行修改
        // Set up the initial icon state
        int N = iconSlots.size();
        int viewIndex = 0;
        for (int i=0; i < N; i++) {
            setIcon(iconSlots.get(i), icons.get(i));
        }

        // Set up the initial notification state.
        try {
            mNotificationListener.registerAsSystemService(mContext,
                    new ComponentName(mContext.getPackageName(), getClass().getCanonicalName()),
                    UserHandle.USER_ALL);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to register notification listener", e);
        }


        if (DEBUG) {
            Log.d(TAG, String.format(
                    "init: icons=%d disabled=0x%08x lights=0x%08x menu=0x%08x imeButton=0x%08x",
                   icons.size(),
                   switches[0],
                   switches[1],
                   switches[2],
                   switches[3]
                   ));
        }

        mCurrentUserId = ActivityManager.getCurrentUser();
        setHeadsUpUser(mCurrentUserId);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mBroadcastReceiver, filter);

        IntentFilter internalFilter = new IntentFilter();
        internalFilter.addAction(WORK_CHALLENGE_UNLOCKED_NOTIFICATION_ACTION);
        internalFilter.addAction(BANNER_ACTION_CANCEL);
        internalFilter.addAction(BANNER_ACTION_SETUP);
        mContext.registerReceiver(mBroadcastReceiver, internalFilter, PERMISSION_SELF, null);

        IntentFilter allUsersFilter = new IntentFilter();
        allUsersFilter.addAction(
                DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED);
        mContext.registerReceiverAsUser(mAllUsersReceiver, UserHandle.ALL, allUsersFilter,
                null, null);
        updateCurrentProfilesCache();

        IVrManager vrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
        try {
            vrManager.registerListener(mVrStateCallbacks);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register VR mode state listener: " + e);
        }
   /* 至此，与IStatusBarService的连接已建立，状态栏与导航栏的窗口也已完成创建与显示，并且
      保存在IStatusBarService中的信息都已完成了显示或设置。状态栏与导航栏的启动正式完成 */

    }
看下这句            mBarService.registerStatusBar(mCommandQueue, iconSlots, icons, switches, binders,
                    fullscreenStackBounds, dockedStackBounds);
进入StatusBarManagerService.java的registerStatusBar：
    @Override
    public void registerStatusBar(IStatusBar bar, List<String> iconSlots,
            List<StatusBarIcon> iconList, int switches[], List<IBinder> binders,
            Rect fullscreenStackBounds, Rect dockedStackBounds) {
        enforceStatusBarService();

        Slog.i(TAG, "registerStatusBar bar=" + bar);
        mBar = bar;
        synchronized (mIcons) {
            for (String slot : mIcons.keySet()) {
                iconSlots.add(slot);
                iconList.add(mIcons.get(slot));
            }
        }
        synchronized (mLock) {
            switches[0] = gatherDisableActionsLocked(mCurrentUserId, 1);
            switches[1] = mSystemUiVisibility;
            switches[2] = mMenuVisible ? 1 : 0;
            switches[3] = mImeWindowVis;
            switches[4] = mImeBackDisposition;
            switches[5] = mShowImeSwitcher ? 1 : 0;
            switches[6] = gatherDisableActionsLocked(mCurrentUserId, 2);
            switches[7] = mFullscreenStackSysUiVisibility;
            switches[8] = mDockedStackSysUiVisibility;
            binders.add(mImeToken);
            fullscreenStackBounds.set(mFullscreenStackBounds);
            dockedStackBounds.set(mDockedStackBounds);
        }
    }
主要看mIcons怎样获得的：
    @Override
    public void setIcon(String slot, String iconPackage, int iconId, int iconLevel,
            String contentDescription) {
        enforceStatusBar();

        synchronized (mIcons) {
            StatusBarIcon icon = new StatusBarIcon(iconPackage, UserHandle.SYSTEM, iconId,
                    iconLevel, 0, contentDescription);
            //Slog.d(TAG, "setIcon slot=" + slot + " index=" + index + " icon=" + icon);
            mIcons.put(slot, icon);

            if (mBar != null) {
                try {
                    mBar.setIcon(slot, icon);
                } catch (RemoteException ex) {
                }
            }
        }
    }
发现在PhoneStatusBarPolicy.java中进行自行调用
经过registerStatusBar后iconSlots已经被赋值
所以调用setIcon(iconSlots.get(i), icons.get(i));时不会为空，那么问题来了，setIcon是从谁的？
经过分析发现BaseStatusBar.java“实现”了CommandQueue.java的CommandQueue.Callbacks的接口，奇怪的是并未实现CommandQueue.Callbacks的void setIcon(String slot, StatusBarIcon icon);，结果发现原因在于
BaseStatusBar.java为抽象类，可不实现接口的方法，所以现在应该去找BaseStatusBar.java的之类，很容易发现为PhoneStatusBar.java，看下实现：

    @Override
    public void setIcon(String slot, StatusBarIcon icon) {
        mIconController.setIcon(slot, icon);
    }
下面流程就不难了，现在会有一个疑问，从流程上来看，iconSlots是在registerStatusBar后被赋值，那为什么状态栏都还没有加载完毕，StatusBarManagerService.java中的mIcons是从在哪个时间节点赋值的呢？
其实不用纠结这个问题，前面可知，IStatusBarService是一个系统服务，IStatusBarService为那些对状态栏感兴趣的其他系统服务定义了一系列API，通过调用StatusBarManagerService.javasetIcon(String slot, String iconPackage, int iconId, int iconLevel, String contentDescription)或setIconVisibility(String slot, boolean visibility)，本质上调用的是PhoneStatusBar.java的setIcon(String slot, StatusBarIcon icon)，如下：

    @Override
    public void setIcon(String slot, StatusBarIcon icon) {
        mIconController.setIcon(slot, icon);
    }
现在会发现这个方法最终调用的也是mIconController.setIcon(slot, icon);，与PhoneStatusBarPolicy.java添加icons所用的方法一样，看下对icons的控制：

    /*private*/ BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                updateAlarm();
            } else if 、、、、、、
很容易发现是通过接收广播进行更新
更直接的说明状态栏是被动显示，这些config_statusBarIcons相关的icons是在mIconController.setIcon(进行赋值以及显示，对于外来的icons是通过调用StatusBarManagerService.java的setIcon(或setIconVisibity(最终毁掉到
PhoneStatusbar.java的setIcon(，同样也是用到mIconController.setIcon(，可以看出，所谓的保存副本信息只是在StatusBarManagerService.java不死掉的情况下，保留mIcons的值，在SystemUI启动的过程中注册registerStatusBar后
赋值并显示的操作（恢复副本信息）
还是要补充一下icons的初始化过程：
先看PhoneStatusBar.java中：
    protected PhoneStatusBarView makeStatusBarView() {
           、、、、、、
           createIconController();
           、、、、、

    protected void createIconController() {
        mIconController = new StatusBarIconController(
                mContext, mStatusBarView, mKeyguardStatusBar, this);
        //*/ freeme.zhiwei.zhang, 20170609. NavigationBar Icon Tint.
        mIconController.setNavigationBarView( mNavigationBarView);
        //*/
    }
再到StatusBarIconController.java的构造方法中：

    public StatusBarIconController(Context context, View statusBar, View keyguardStatusBar,
            PhoneStatusBar phoneStatusBar) {
        super(context.getResources().getStringArray(
                com.android.internal.R.array.config_statusBarIcons));
        、、、、、、
其中        super(context.getResources().getStringArray(com.android.internal.R.array.config_statusBarIcons));，跟到父类StatusBarIconList.java中去：

    public StatusBarIconList(String[] slots) {
        final int N = slots.length;
        for (int i=0; i < N; i++) {
            mSlots.add(slots[i]);
            mIcons.add(null);
        }
    }
这里的mSlots会被赋config_statusBarIcons的值，可令人好奇的是mIcons.add(null);，那在StatusBarIconController.java里调用getIcon(int index)时候不是返回null了吗？是的，所以才会等待接收广播或api接口被调用

接着看createAndAddWindows();，实现在PhoneStatusBar.java中：

    @Override
    public void createAndAddWindows() {
        addStatusBarWindow();
    }

    private void addStatusBarWindow() {
        //创建状态栏的控件树
        makeStatusBarView();
        mStatusBarWindowManager = new StatusBarWindowManager(mContext);
        mRemoteInputController = new RemoteInputController(mStatusBarWindowManager,
                mHeadsUpManager);
         //通过WindowManager.addView()创建状态栏的窗口
        mStatusBarWindowManager.add(mStatusBarWindow, getStatusBarHeight());
    }
最后注册一些广播

根据类的多态，public class PhoneStatusBar extends BaseStatusBar，调用的是PhoneStatusBar的方法start()

    @Override
    public void start() {
        mDisplay = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        updateDisplaySize();
        mScrimSrcModeEnabled = mContext.getResources().getBoolean(
                R.bool.config_status_bar_scrim_behind_use_src);

        super.start(); // calls createAndAddWindows()

        mMediaSessionManager
                = (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        // TODO: use MediaSessionManager.SessionListener to hook us up to future updates
        // in session state

        addNavigationBar();

        // Lastly, call to the icon policy to install/update all the icons.
        mIconPolicy = new PhoneStatusBarPolicy(mContext, mIconController, mCastController,
                mHotspotController, mUserInfoController, mBluetoothController,
                mRotationLockController, mNetworkController.getDataSaverController());
        mIconPolicy.setCurrentUserSetup(mUserSetup);
        mSettingsObserver.onChange(false); // set up

        mHeadsUpObserver.onChange(true); // set up
        if (ENABLE_HEADS_UP) {
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED), true,
                    mHeadsUpObserver);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(SETTING_HEADS_UP_TICKER), true,
                    mHeadsUpObserver);
        }
        mUnlockMethodCache = UnlockMethodCache.getInstance(mContext);
        mUnlockMethodCache.addListener(this);
        startKeyguard();

        mDozeServiceHost = new DozeServiceHost();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mDozeServiceHost);
        putComponent(DozeHost.class, mDozeServiceHost);
        putComponent(PhoneStatusBar.class, this);

        setControllerUsers();

        notifyUserAboutHiddenNotifications();

        mScreenPinningRequest = new ScreenPinningRequest(mContext);
        mFalsingManager = FalsingManager.getInstance(mContext);

        //*/ freeme, gouzhouping, 20161221, for show notifications switch.
        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SHOW_NOTIFICATIONS_ICON),
                false, mNotificationObserver);
        updateShowNotifications();
        //*/

        //*/ freeme. gouzhouping, 20170724. for show network speed.
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(FreemeStatusbarStateToolKit.SHOW_NETWORK_SPEED_SWITCH), false, mNetworkObserver);
        //*/

        //*/ freeme. chenming, 20170731. Freeme Battery.
        mFreemeBatteryManager = new FreemeBatteryManager(mContext, mBatteryController, mStatusBarWindow, mKeyguardStatusBar);
        //*/
    }
里面包含super.start();，调用到上面的父类的start()，我们在子类PhoneStatusBar.java中可以依次分析加载布局相关的流程

二 恢复副本信息
已在前面分析
