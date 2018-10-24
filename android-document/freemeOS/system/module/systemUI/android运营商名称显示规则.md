  Plmn的全称是Public Land Mobile
  Network（公共陆地移动网络），而在运营商显示方面主要是指当前SIM所驻留的网络，比如当中国移动的SIM（46000）如果漫游到联通的网络（46001），那么虽然当前的SIM是中国移动，但是他的Plmn就应该是中国联通。
  也就是说，Plmn的名称与当前驻留的网络相关
  SPN(Service Provider Name)就是当前发行SIM卡的运营商的名称，可以从以下两个路径获取：

  1、从SIM文件系统读取

  2、从配置文件读取

具体流程梳理

一、入口或衔接作用之UiccController

UiccController是整个UICC相关信息的控制接口，UiccController的实例化就是在RIL与UiccController 之间建立监听关系，这样的话，当SIM卡状态发生变化时，UiccController就可以马上知道并且做出相应的操作。
UiccController对象是在PhoneFacotry.Java中的makeDefaultPhone()方法中初始化的，有个细节值得注意的是sCommandsInterfaces数组的i对应的是PhoneId。先进入PhoneFactory.java的makeDefaultPhone(Context context)：

```
    /**
     * FIXME replace this with some other way of making these
     * instances
     */
    public static void makeDefaultPhone(Context context) {
        synchronized (sLockProxyPhones) {
            if (!sMadeDefaults) {
                sContext = context;
                、、、、、、
                sCommandsInterfaces = new RIL[numPhones];
                sTelephonyNetworkFactories = new TelephonyNetworkFactory[numPhones];

                for (int i = 0; i < numPhones; i++) {
                    // reads the system properties and makes commandsinterface
                    // Get preferred network type.
                    networkModes[i] = RILConstants.PREFERRED_NETWORK_MODE;

                    Rlog.i(LOG_TAG, "Network Mode set to " + Integer.toString(networkModes[i]));
                    sCommandsInterfaces[i] = new RIL(context, networkModes[i],
                            cdmaSubscription, i);
                }
                Rlog.i(LOG_TAG, "Creating SubscriptionController");
                SubscriptionController.init(context, sCommandsInterfaces);
                RadioManager.init(context, numPhones, sCommandsInterfaces);

                // Instantiate UiccController so that all other classes can just
                // call getInstance()
                sUiccController = UiccController.make(context, sCommandsInterfaces);

                for (int i = 0; i < numPhones; i++) {
                    Phone phone = null;
                    int phoneType = TelephonyManager.getPhoneType(networkModes[i]);
                    if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                        phone = new GsmCdmaPhone(context,
                                sCommandsInterfaces[i], sPhoneNotifier, i,
                                PhoneConstants.PHONE_TYPE_GSM,
                                TelephonyComponentFactory.getInstance());
                    } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        phone = new GsmCdmaPhone(context,
                                sCommandsInterfaces[i], sPhoneNotifier, i,
                                PhoneConstants.PHONE_TYPE_CDMA_LTE,
                                TelephonyComponentFactory.getInstance());
                    }
                    Rlog.i(LOG_TAG, "Creating Phone with type = " + phoneType + " sub = " + i);

                    sPhones[i] = phone;
                }
                、、、、、、

                sProxyController = ProxyController.getInstance(context, sPhones,
                        sUiccController, sCommandsInterfaces, sPhoneSwitcher);

                sTelephonyNetworkFactories = new TelephonyNetworkFactory[numPhones];
                for (int i = 0; i < numPhones; i++) {
                    sTelephonyNetworkFactories[i] = new TelephonyNetworkFactory(
                            sPhoneSwitcher, sc, sSubscriptionMonitor, Looper.myLooper(),
                            sContext, i, sPhones[i].mDcTracker);
                }

                // M: Data connection helper class.
                DataConnectionHelper.makeDataConnectionHelper(context, sPhones, sPhoneSwitcher);

                //[WorldMode]
                if (WorldPhoneUtil.isWorldModeSupport() && WorldPhoneUtil.isWorldPhoneSupport()) {
                    Rlog.i(LOG_TAG, "World mode support");
                    WorldMode.init();
                } else if (WorldPhoneUtil.isWorldPhoneSupport()) {
                    Rlog.i(LOG_TAG, "World phone support");
                    sWorldPhone = WorldPhoneWrapper.getWorldPhoneInstance();
                } else {
                    Rlog.i(LOG_TAG, "World phone not support");
                }

            }
        }
    }
```

在UiccController.java的make()方法中new了一个UiccController对象：

```
    public static UiccController make(Context c, CommandsInterface[] ci) {
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("MSimUiccController.make() should only be called once");
            }
            //实例化UiccController对象
            mInstance = new UiccController(c, ci);
            return (UiccController)mInstance;
        }
    }

    private UiccController(Context c, CommandsInterface []ci) {
        if (DBG) log("Creating UiccController");
        mContext = c;
        mCis = ci;
        for (int i = 0; i < mCis.length; i++) {
            //index对应的是PhoneId
            Integer index = new Integer(i);
            //主要注册监听四种事件
            mCis[i].registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, index);
            // TODO remove this once modem correctly notifies the unsols
            // MTK-START
            if (SystemProperties.get("ro.crypto.state").equals("unencrypted")
                    || SystemProperties.get("ro.crypto.state").equals("unsupported")
                    || SystemProperties.get("ro.crypto.type").equals("file")
                    || DECRYPT_STATE.equals(SystemProperties.get("vold.decrypt"))) {
            //if (DECRYPT_STATE.equals(SystemProperties.get("vold.decrypt"))) {
            // MTK-END
                mCis[i].registerForAvailable(this, EVENT_ICC_STATUS_CHANGED, index);
            } else {
                mCis[i].registerForOn(this, EVENT_ICC_STATUS_CHANGED, index);
            }
            mCis[i].registerForNotAvailable(this, EVENT_RADIO_UNAVAILABLE, index);
            mCis[i].registerForIccRefresh(this, EVENT_SIM_REFRESH, index);
            // MTK-START

            mCis[i].registerForVirtualSimOn(this, EVENT_VIRTUAL_SIM_ON, index);
            mCis[i].registerForVirtualSimOff(this, EVENT_VIRTUAL_SIM_OFF, index);
            mCis[i].registerForSimMissing(this, EVENT_SIM_MISSING, index);
            mCis[i].registerForSimRecovery(this, EVENT_SIM_RECOVERY, index);
            mCis[i].registerForSimPlugOut(this, EVENT_SIM_PLUG_OUT, index);
            mCis[i].registerForSimPlugIn(this, EVENT_SIM_PLUG_IN, index);
            mCis[i].registerForCommonSlotNoChanged(this, EVENT_COMMON_SLOT_NO_CHANGED, index);
            // MTK-END
        }

        // MTK-START
        try {
            mUiccControllerExt = MPlugin.createInstance(
                    IUiccControllerExt.class.getName(), mContext);
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Fail to create plug-in");
            e.printStackTrace();
        }
        // MTK-END
    }
```

在上面UiccController的构造方法中可以看到，注册了三个事件EVENT_ICC_STATUS_CHANGED（监听SIM卡的状态变化），EVENT_RADIO_UNAVAILABLE（一旦radio变成不可用状态，就清空SIM卡的信息），EVENT_SIM_REFRESH。index对应的是PhoneId，当上面这三种消息上来时，就知道对应哪个Phone对象，也就对应那张卡。 
当接收到EVENT_ICC_STATUS_CHANGED消息后，UiccController调用RIL.java的getIccCardStatus()方法给MODEM发送RIL_REQUEST_GET_SIM_STATUS消息，查询SIM卡的状态，看下与RIL.java交互的过程：

UiccController.java本身UiccController extends Handler，所以会默认实现handleMessage (Message msg)：

```
    @Override
    public void handleMessage (Message msg) {
        synchronized (mLock) {
            Integer index = getCiIndex(msg);

            if (index < 0 || index >= mCis.length) {
                Rlog.e(LOG_TAG, "Invalid index : " + index + " received with event " + msg.what);
                return;
            }

            AsyncResult ar = (AsyncResult)msg.obj;
            switch (msg.what) {
                case EVENT_ICC_STATUS_CHANGED:
                    // MTK-START
                    //if (DBG) log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus");
                    if (DBG) {
                        log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus, index: "
                            + index);
                    }

                    //mCis[index].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE, index));
                    if (ignoreGetSimStatus()) {
                        if (DBG) log("FlightMode ON, Modem OFF: ignore get sim status");
                    } else {
                    // MTK-END
                        mCis[index].getIccCardStatus(obtainMessage(
                                EVENT_GET_ICC_STATUS_DONE, index));
                    // MTK-START
                    }
                    // MTK-END
                    break;
```

mCis为PhoneFacotry.Java里方法makeDefaultPhone(Context context)定义的new RIL[numPhones]，所以为RIL实例：

```
    @Override
    public void
    getIccCardStatus(Message result) {
        //Note: This RIL request has not been renamed to ICC,
        //       but this request is also valid for SIM and RUIM
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_GET_SIM_STATUS, result);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        send(rr);
    }
```

这里要注意的是RILRequest.java对消息进行了处理，看下如何处理的：

```
    static RILRequest obtain(int request, Message result) {
        RILRequest rr = null;

        synchronized(sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }

        if (rr == null) {
            rr = new RILRequest();
        }

        rr.mSerial = sNextSerial.getAndIncrement();

        rr.mRequest = request;
        //这里是关键，把Message类型的result赋值给将要返回的RILRequest
        rr.mResult = result;
        rr.mParcel = Parcel.obtain();

        rr.mWakeLockType = RIL.INVALID_WAKELOCK;
        if (result != null && result.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }

        // first elements in any RIL Parcel
        rr.mParcel.writeInt(request);
        rr.mParcel.writeInt(rr.mSerial);

        return rr;
    }
```

再看下RIL.java如何接收处理消息RIL_REQUEST_GET_SIM_STATUS：
