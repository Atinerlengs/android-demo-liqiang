[TOC]

# 文章问题

1. 章节设置不合理，标题混乱，观察TOC字段可知。
2. 标点符号格式混乱，半角全角混用，部分中文句子不通。
3. 部分表达要么残缺不全，要么用词不准确
3. 参考文献写法改进

# Android N Nofication Bug

## `bug` 现象

`QQ/wechat` 打开时出现一条没有任何 `action` 的 `Notification` . ( `Android 7.0_r1` ， `QQ` 版本 `8.9.20026` ，相同版本 `QQ` 在 `Android 7.0_r1` 之前没有该现象， `Android 7.1` 没有该现象)

【评注：这里重写下，用列表形式描述】

Bug 现象如图所示

![Bug](notify.png)

## Notification

通知是可以在应用的常规`UI`外部向用户显示的消息. 告知系统发出通知时，它将先以图标的形式显示在通知区域中. 用户可以下拉状态栏查 看通知的详细信息.

显示一条 `Notification` ，至少需要设置：

- `setSmallIcon(@DrawableRes int icon)` ，设置小图标
- `setContentTitle(CharSequence title)` ，设置标题
- `setContentText(CharSequence text)` ，设置通知内容

## 显示通知

【评注：通知的介绍显然应该单独列一节，然后根据需要分为小节，而不是多节并列。发送通知和删除通知建议合并。还有标题还是不要放api原型了，太长太难看了。把原型和所在java文件以代码形式贴。】

### `notify(int id, Notification notification)`

应用中一般发送通知所使用的方法为 `notify` 方法，该方法中有一个参 数为所构建出的 `notification` ，可以通过 `cancel` 方法来移除该条通知.

### `startForeground(int id, Notification notification)`

`Android SDK` 中对该方法的描述如下:

> Make this service run in the foreground, supplying the ongoing notification to be shown to the user while in this state.

该 `API` 将服务提到前台运行，在该状态过程中，将正在运行的(`ongoing`)通知显示给用户.

>该通知只有在该服务被终止或者从前台主动移除才能被解除，可以通过 `stopForeground` 方法来移除该条通知.

`Android SDK` 中对 `ongoing` 的描述如下:

> Ongoing notifications cannot be dismissed by the user, so your application or service must take care of canceling them.


## 删除通知


### `setAutoCancel(boolean autoCancel)` 、 `cancel(int id)` 、 `cancelAll()`

- 用户手动通过点击 `清除全部` 按钮
- 在用户点击通知时，执行 `setAutoCancel(boolean autoCancel)`
- 调用 `cancel(int id)` ，删除指定通知，注意，该 `API` 还可以清除

 `ongoing` 的通知
- 调用 `cancelAll()` ，删除所有的通知

注意： `setAutoCancel` 方法是在创建通知时使用的，如果设置  `setAutoCancel(true)` 则在发出通知后，该条通知在发出后会自动删除

### `stopForeground(boolean removeNotification)`

>Remove this service from foreground state, allowing it to be killed if more memory is needed.

从前台进程中移除该服务，在内存紧张的时候会被杀死


## Bug 产生

打开 `QQ` ，在使用 `stopForeground` 方法却没能将相同 `notification id` 的通知移除.

【评注：描述不清、后面描述也不全，没有说明黑色通知是startForegroud产生的】

### 原因分析

[frameworks/base/core/java/android/app/Service.java]

```
public final void stopForeground(boolean removeNotificatio) {
   stopForeground(removeNotification ? STOP_FOREGROUND_REMOVE : 0);
}
```

```
public final void stopForeground(@StopForegroundFlags int flags) {
    try {
        mActivityManager.setServiceForeground(
            new ComponentName(this, mClassName), mToken, 0, null, flags);
    } catch (RemoteException ex) {
    }
}
```

[frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java]

```
public void setServiceForeground(ComponentName className, IBinder token,int id, Notification notification, int  flags) {
    synchronized(this) {
        mServices.setServiceForegroundLocked(className, token, id, notification, flags);
    }
}
```

[frameworks/base/services/core/java/com/android/server/am/ActiveServices.java]

```
public void setServiceForegroundLocked(ComponentName className, IBinder token,int id, Notification notification, int flags) {
    ...
    if (r.foregroundId != id) {
        cancelForegroudNotificationLocked(r);
        r.foregroundId = id;
    ...
    } else {
        ...
        if ((flags & Service.STOP_FOREGROUND_REMOVE) != 0) {
            cancelForegroudNotificationLocked(r);
            r.foregroundId = 0;
            r.foregroundNoti = null;
        }
        ...
    }
}
```

```
private void cancelForegroudNotificationLocked(ServiceRecord r) {
    if (r.foregroundId != 0) {
        // First check to see if this app has any other active foreground services
        // with the same notification ID.  If so, we shouldn't actually cancel it,
        // because that would wipe away the notification that still needs to be shown
        // due the other service.
        ServiceMap sm = getServiceMap(r.userId);
        if (sm != null) {
            for (int i = sm.mServicesByName.size()-1; i >= 0; i--) {
                ServiceRecord other = sm.mServicesByName.valueAt(i);
                if (other != r && other.foregroundId == r.foregroundId && other.packageName.equals(r.packageName)) {
                    // Found one!  Abort the cancel.
                    return;
                }
            }
        }
        r.cancelNotification();
    }
}
```

可以看到在移除通知时，发现有相同 `notification id` 的通知会直接 退出.

【评注：为什么`直接`和`退出`中间有个空格？？？这是什么写法？】

## 问题

1. 在 `Android M` 中没有该现象， `Android 7.1` 中也没有这个现象,
我们的版本却有该问题?

【评注：为什么这里逗号后面有断行？？描述也不清楚】

2. 使用 `startForeground` 方法将通知提到前台服务，却还要用 `stopForeground` 将该通知移除?

3. 使用 `startForeground` 方法将通知提到前台，意义何在?

【评注：问题3可以删掉，问题2描述改写下】

## 问题回答

问题1，查找 `Google` 源码提交记录发现在 `2016/08/01` 有一个提交改动，如下

```
    commit 0ba4c710c6f8805175bde2dbd85d7d8788a15ee0
    Author: Dianne Hackborn <hackbod@google.com>
    Date:   Mon Aug 1 17:49:41 2016 -0700

    Fix issue #29506774: Foreground Service Can Avoid Notification Requirement

    Don't cancel the notification if there are other foreground
    services using the same notification ID.

    Change-Id: I02a49d9a07af0203e59e70be2dc6773f3cefee47
```

也就是说`Android M`中没有这个改变，还沿用以前的方案，
frameworks/base/services/core/java/com/android/server/am/ServiceRecord.java

```
public void cancelNotification() {
    final String localPackageName = packageName;
    final int localForegroundId = foregroundId;
    ams.mHandler.post(new Runnable() {
        public void run() {
            INotificationManager inm = NotificationManager.getService();
            if (inm == null) {
                return;
            }
            try {
                inm.cancelNotificationWithTag(localPackageName, null,localForegroundId, userId);
            } catch (RuntimeException e) {
                Slog.w(TAG, "Error canceling notification for service", e);
            } catch (RemoteException e) {
            }
        }
    });
}
```


修改之后的`patch`如下:

![patch](diff.png)

第二点，`Android 7.0`的`SDK Level`为24，`Android 7.1`的`SDK Level`为25，对相同`notification id`的修改是在`2016/08/01`完成的，而我们的代码处在八月份之后，已经合并了该修改，`QQ`中应该用`API Level`的判断，所以才导致了在`Android 7.1`中没有出现，在我们的代码中出现该现象.

问题2，使用`startForeground`方法将服务提到前台，带有一条`notification`，可以提高进程的优先级，使得该进程在内存紧张的情况下，不容易被杀死，但是这样一来`notification`会被用户看到，不利于体验，所以需要通过`stopForeground`方法将该通知移除，`stopForeground`会设置一个`null`的通知在前台，这样即可以移除掉原先的通知，也会将一个`null`的通知放在前台，优化了用户体验，提高了进程优先级.

问题3，问题2中以讲到使用`startForeground`可以提高进程优先级，不容易被系统杀死，这样做的意义在于，在内存允许的情况下该进程可以始终存在，这样`QQ`就始终运行在前台，提高用户体验度，这种设计思想可以使所在意的应用一直存活在前台，达到一种保活应用的目的，但是如果大量使用这种保活思想的话，会导致设备的操作性能降低，导致设备使用起来会很卡.


【评注：问题和问题解答建议合并在一起。在抛出问题之后，立刻解答该问题。】

## 总结
这个问题的出现是因为在`Android N`之后对以前实现的保活方案所依 赖的`API`有所改变.

【评注：这总结太笼统，模棱两可，AndroidN之后？7.0算不算AndroidN？还有为什么中间又有莫名其妙的空格？】

## 保活
`Android`系统将尽量长时间地保持应用进程，但为了新建进程或运行更重要的进程，最终需要清除旧进程来回收内存. 

【评注：这一句话不清不楚，完全没有言简意赅的说明为什么要保活】

![process](process.png)

需要更多了解请参考[保活文档](http://blog.csdn.net/zhangweiwtmdbf/article/details/52369276)

`QQ`保活思想，主要是通过`startForeground`提升优先级，方案的具体实现(简单模拟)，如下:

![Live](live.jpg)

【评注：这一段建议在报保活文档的基础上，自己整理下，锻炼下自己的表达能力，不要贴图了】

## 参考文献

1. http://blog.csdn.net/zhangweiwtmdbf/article/details/52369276[保活]
2. http://www.jianshu.com/p/0929c4012347[黑条分析]
3. https://www.diycode.cc/topics/45[保活]
4. https://developer.android.com/guide/components/processes-and-threads.html?hl=zh-cn[进程介绍]
5. https://developer.android.com/guide/topics/ui/notifiers/notifications.html?hl=zh-cn[通知介绍]

【评注：这是什么格式？？简直闻所未闻】

## 分析过程

1. 看到这个`Bug`时，在自己的手机中安装了`QQ`测试确实有该现象， 必现，查看视图树发现其中的`ImageView`与`TextView`的宽度均为0，没有显示内容
2. 相同的`QQ`放在驱动版本中验证也存在这个现象，查看对比机发现没有该现象
3. 使用`adb shell dumpsys notification`，查看到 `contentIntent`、`tickerText`均为空
4. 根据这个现象，还有一般通知的发送为`notify`方法，跟踪`notify`方法，添加断点，在跟踪的过程中发现对该条`notification` 的解析过程正常
5. 将`QQ`反编译查看其关于`notification`的布局，发现有和此条 `notification`相同布局结构的`xml`文件，并没有大的发现
6. 查看到此还是没有结论，放假搁置
7. 接着在网上找相关资料，最新版本`QQ`没有该现象
8. 使用`adb shell dumpsys activity services com.tencent.mobileqq`输出`QQ`的`Services`相应信息，发现有 `isForeground=true`说明`QQ`使用了前台服务提供的通知保活方案，参考文献[3]
9. 接着了解到`startForeground`方法与`notification`实现的保活思想
10. 了解到移除该条通知的方法`stopForeground`,追踪该方法调用栈信息得到问题根本原因

notification

![Notification](QQ_Notification.png)

service

![Service](QQ_Service.png)

