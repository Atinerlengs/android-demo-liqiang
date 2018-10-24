三星启动图标标记实现简述

三星设计了一套数据库，每当app增加未读数据的时候，都会向数据库中写入数据。Launcher中通过BadgeMonitor进行数据监听，实现界面更新。
相关数据库：

```
    public static final Uri BADGE_INTERNAL_URI = Uri.parse("content://com.sec.badge/internal");


    public static final Uri BADGE_URI = Uri.parse("content://com.sec.badge/apps");
```

接下来的工作是：
找到数据库实现的地方，在launcher中没有搜到
找到数据库后，了解其字段设计（数据是如何携带的）。

接下来，给出印证的具体代码

主要涉及类：

BadgeMonitor（**ContentProvider**）

LauncherConnector（**更新标记**）


先看BadgeMonitor的onChange（）

```
            pendingBadgeUpdate = false;
            uri = updateBadgeCounts();
            if(mBadgeListener != null)
            {
                // mBadgeListener的实现就是LauncherConnector。在LauncherConnector中onBadgeUpdated（）的代码中我们可以得到更清晰的印证
                mBadgeListener.onBadgeUpdated(uri);
                return;
            }
```

再看LauncherConnector的onBadgeUpdated()

```
if(launcheritem.getComponentName().equals(entry.getKey()) && launcheritem.getBadgeCount() != badgecount.getBadgeCount())
                        {
                            launcheritem.setBadgeCount(badgecount.getBadgeCount());
                            notifyBadgeUpdated(item);
                        }
```

Launcher监听数据库变化

```
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.samsung.android.email.providerclass:com.samsung.android.email.ui.activity.MessageListXLbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.android.mmsclass:com.android.mms.ui.ConversationComposerbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.samsung.android.smclass:com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivitybadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.android.settingsclass:com.android.settings.Settingsbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.android.settingsclass:com.android.settings.GridSettingsbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.sec.android.app.samsungappsclass:com.sec.android.app.samsungapps.SamsungAppsMainActivitybadgecount:15
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.tencent.mmclass:com.tencent.mm.ui.LauncherUIbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.tencent.mobileqqclass:com.tencent.mobileqq.activity.SplashActivitybadgecount:14
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.myzaker.ZAKER_Phoneclass:com.myzaker.ZAKER_Phone.view.LogoActivitybadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.lionmobi.powercleanclass:com.lionmobi.powerclean.activity.SplashActivitybadgecount:1
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.eg.android.AlipayGphoneclass:com.eg.android.AlipayGphone.AlipayLoginbadgecount:1
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.android.contactsclass:com.android.dialer.DialtactsActivitybadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.lbe.parallel.intlclass:com.lbe.parallel.ui.tour.SplashActivitybadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.baidu.BaiduMapclass:com.baidu.baidumaps.WelcomeScreenbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.infraware.office.link.chinaclass:com.infraware.service.ActLauncherbadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:ctrip.android.viewclass:ctrip.android.view.splash.CtripSplashActivitybadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.kingsoft.emailclass:com.kingsoft.email.activity.Welcomebadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.zhuoyi.marketclass:com.zhuoyi.market.Splashbadgecount:11
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.jingdong.app.mallclass:com.jingdong.app.mall.main.MainActivitybadgecount:0
02-20 16:50:16.551 25590-25590/com.app.badgetest I/shanjibing: package:com.netease.cloudmusicclass:com.netease.cloudmusic.activity.LoadingActivitybadgecount:0
```

app发广播   action：android.intent.action.BADGE_COUNT_UPDATE

```
02-21 11:54:06.433 26187-26187/com.app.badgetest I/shanjibing: count =4
                                                               package name =com.tencent.mobileqq
02-21 11:54:23.803 26187-26187/com.app.badgetest I/shanjibing: count =3
                                                               package name =com.tencent.mobileqq
02-21 11:54:43.033 26187-26187/com.app.badgetest I/shanjibing: count =0
                                                               package name =com.tencent.mobileqq
02-21 11:54:43.053 26187-26187/com.app.badgetest I/shanjibing: count =0
                                                               package name =com.tencent.mobileqq
02-21 11:54:52.353 26187-26187/com.app.badgetest I/shanjibing: count =1
                                                               package name =com.tencent.mobileqq
```

Launcher没有接受广播，而是监听数据库的改变更新数据

最后，更新标记图标概述：
app发广播并携带标记相关数据。系统会有数据哭保存相关数据，Launcher监听数据库并依据数据库中的数据更新界面。

