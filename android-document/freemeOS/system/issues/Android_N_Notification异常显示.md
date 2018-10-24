### Android N Nofication

针对前段时间出现的QQ/wechat，登录时出现一条全黑色，没有任何内容的Notification Bug的分析:
首先考虑到此条Notification的出现应该为不合理的，那么Notification的消失隐藏机制是怎么样的呢，通过查看资料知道，Notification的隐藏消失实现的方法为 **stopForground(boolean removeNotification)** 方法。


参考文章：

>http://www.jianshu.com/p/0929c4012347
>http://blog.csdn.net/zhangweiwtmdbf/article/details/52369276

Notification 的隐藏实现(参考以上资料)
位置：frameworks/base/core/java/android/app/Service.java

```
    public final void stopForeground(boolean removeNotification) {
        stopForeground(removeNotification ? STOP_FOREGROUND_REMOVE : 0);
    }
```

可以看到主要的实现是在 stopForeground(int )中实现的，继续跟进该方法中

```
    public final void stopForeground(@StopForegroundFlags int flags) {
        try {
            mActivityManager.setServiceForeground(
                    new ComponentName(this, mClassName), mToken, 0, null, flags);
        } catch (RemoteException ex) {
        }
    }
```

mActivityManager 变量的定义类型为 IActivityManager
此时可以看到已经进入到 **IActivityManager** 中，继续进入到其 **BN** 端的 **ActivityManagerService** 中
位置：frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java

```
    @Override
    public void setServiceForeground(ComponentName className, IBinder token,
            int id, Notification notification, int flags) {
        synchronized(this) {
            mServices.setServiceForegroundLocked(className, token, id, notification, flags);
        }
    }
```

mServices 变量的定义类型为 ActiveServices
接着我们需要跟着进入 **ActiveServices** 中的 **setServiceForegroundLocked**
位置：frameworks/base/services/core/java/com/android/server/am/ActiveServices.java

```
    public void setServiceForegroundLocked(ComponentName className, IBinder token, int id, Notification notification, boolean removeNotification) {
        ...
            if(notification == null) {
                throw new IllegalArgumentException("null notification");
            }
            if(r.foregroundId != id) {
                cancelForegroudNotificationLocked(r);
                r.foregroundId = id;
            }
            ...
            } else {
            ...
                if(removeNotification) {
                    cancelForegroudNotificationLocked(r);
                    r.foregroundId = 0;
                    r.foregroundNoti = null;
                }
            }
        ...
    }
```

真正实现隐藏消失Notification的操作地方为 cancelForegroudNotificationLocked(r)
接着进入到 **cancelForegroudNotificationLocked(r)**

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
                    /// M: ALPS02870971 Avoid showing notification after
                    /// stopForegroundService @{
                    if (r == other) {
                        continue;
                    }
                    /// @}
                    if (other.foregroundId == r.foregroundId
                            && other.packageName.equals(r.packageName)) {
                        // Found one!  Abort the cancel.
                        return;
                    }
                }
            }
            r.cancelNotification();
        }
    }
```

发现在这里，系统会进行判断如果有相同的 NotificationId 就会直接返回，不会有隐藏消失的效果出现.
我们的处理方式为:

```
     public void setServiceForegroundLocked(ComponentName className, IBinder token, int id, Notification notification, boolean removeNotification) {
        ...
            if(r.foregroundId != id) {
                /*/ modified
                cancelForegroudNotificationLocked(r);
                /*/
                r.cancelNotification()
                //*/
                r.foregroundId = id;
            }
            ...
            } else {
                ...
                if(removeNotification) {
                    /*/ modified
                    cancelForegroudNotificationLocked(r);
                    /*/
                    r.cancelNotification()
                    //*/
                    r.foregroundId = 0;
                    r.foregroundNoti = null;
                }
            }
        ...
    }
```

以前的处理为 **ServiceRecord.java** 中的 **cancelNotification()** 方法
位置: frameworks/base/services/core/java/com/android/server/am/ServiceRecord.java

```
    public void cancelNotification() {
        if (foregroundId != 0) {
            // Do asynchronous communication with notification manager to
            // avoid deadlocks.
            final String localPackageName = packageName;
            final int localForegroundId = foregroundId;
            ams.mHandler.post(new Runnable() {
                public void run() {
                    INotificationManager inm = NotificationManager.getService();
                    if (inm == null) {
                        return;
                    }
                    try {
                        inm.cancelNotificationWithTag(localPackageName, null,
                                localForegroundId, userId);
                    } catch (RuntimeException e) {
                        Slog.w(TAG, "Error canceling notification for service", e);
                    } catch (RemoteException e) {
                    }
                }
            });
        }
    }
```

针对这个问题，我们的修改方式，修改原先的 **cancelNotification()** 为：

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
                     inm.cancelNotificationWithTag(localPackageName, null,
                             localForegroundId, userId);
                 } catch (RuntimeException e) {
                     Slog.w(TAG, "Error canceling notification for service", e);
                 } catch (RemoteException e) {
                 }
             }
         });
     }
```

#### 总结
这个问题的出现是因为在应用中的[保活方案](http://blog.csdn.net/zhangweiwtmdbf/article/details/52369276)引起的，在Android N 之后对以前实现的保活方案所依赖的 **API** 有所改变，这也就影响到相应应用出现一些不合适的显示问题。

