RemoteView表示的是一种View结构，它可以在其他进程中显示（具体来讲是SystemServer进程），由于它是在其他进程中显示，为了更新它的界面，我们不能简单地使用普通View的那一套方法，RemoteView提供了一系列Set方法用于更新界面

```
private void sendNotification(){

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification =new Notification();

        notification.icon=R.mipmap.ic_launcher;

        notification.when=System.currentTimeMillis();

        notification.flags=Notification.FLAG_AUTO_CANCEL;

        //跳转意图

        Intent intent = new Intent(this,SettingsActivity.class);

        //建立一个RemoteView的布局，并通过RemoteView加载这个布局

        RemoteViews remoteViews = new RemoteViews(getPackageName(),R.layout.layout_notification);

        //为remoteView设置图片和文本

        remoteViews.setTextViewText(R.id.message,"第一条通知");

        remoteViews.setImageViewResource(R.id.image,R.mipmap.ic_launcher_round);

        //设置PendingIntent

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        //为id为openActivity的view设置单击事件

        remoteViews.setOnClickPendingIntent(R.id.openActivity,pendingIntent);

        //将RemoteView作为Notification的布局

        notification.contentView =remoteViews;

        //将pendingIntent作为Notification的intent，这样当点击其他部分时，也能实现跳转

        notification.contentIntent=pendingIntent;

        notificationManager.notify(1,notification);

    }
```

特殊Activity的处理
仅当从通知中启动时，用户才会看到此 Activity。 从某种意义上说，Activity 是通过提供很难显示在通知本身中的信息来扩展通知。对于这种情况，请将 PendingIntent 设置为在全新任务中启动。但是，由于启动的 Activity 不是应用 Activity 流程的一部分，因此无需创建返回栈。点击“返回”仍会将用户带到主屏幕。

设置常规 Activity PendingIntent

要设置可启动直接进入 Activity 的 PendingIntent，请执行以下步骤：

在清单文件中定义应用的 Activity 层次结构。

添加对 Android 4.0.3 及更低版本的支持。为此，请通过添加 meta-data 元素作为 activity的子项来指定正在启动的 Activity 的父项。
对于此元素，请设置 android:name="android.support.PARENT_ACTIVITY"。 设置 android:value="<parent_activity_name>"，其中，<parent_activity_name> 是父 <activity> 元素的 android:name 值。请参阅下面的 XML 示例。
同样添加对 Android 4.1 及更高版本的支持。为此，请将 android:parentActivityName 属性添加到正在启动的 Activity 的 activity 元素中。
最终的 XML 应如下所示：

```
<activity

    android:name=".MainActivity"
    android:label="@string/app_name" >
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
<activity

    android:name=".ResultActivity"
    android:parentActivityName=".MainActivity">
    <meta-data
        android:name="android.support.PARENT_ACTIVITY"
        android:value=".MainActivity"/>
</activity>
```

根据可启动 Activity 的 Intent 创建返回栈：

创建 Intent 以启动 Activity。

通过调用 TaskStackBuilder.create() 创建堆栈生成器。

通过调用 addParentStack() 将返回栈添加到堆栈生成器。 对于在清单文件中所定义层次结构内的每个 Activity，返回栈均包含可启动 Activity 的 Intent 对象。此方法还会添加一些可在全新任务中启动堆栈的标志。

注：尽管 addParentStack() 的参数是对已启动 Activity 的引用，但是方法调用不会添加可启动 Activity 的 Intent，而是留待下一步进行处理。

通过调用 addNextIntent()，添加可从通知中启动 Activity 的 Intent。 将在第一步中创建的 Intent 作为 addNextIntent() 的参数传递。

如需，请通过调用 TaskStackBuilder.editIntentAt() 向堆栈中的 Intent 对象添加参数。有时，需要确保目标 Activity 在用户使用“返回”导航回它时会显示有意义的数据。

通过调用 getPendingIntent() 获得此返回栈的 PendingIntent。 然后，您可以使用此 PendingIntent 作为 setContentIntent() 的参数。

以下代码段演示了该流程：

```
...
Intent resultIntent = new Intent(this, ResultActivity.class);

TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

// Adds the back stack

stackBuilder.addParentStack(ResultActivity.class);

// Adds the Intent to the top of the stack

stackBuilder.addNextIntent(resultIntent);

// Gets a PendingIntent containing the entire back stack

PendingIntent resultPendingIntent =
        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

...

NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

builder.setContentIntent(resultPendingIntent);

NotificationManager mNotificationManager =
    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

mNotificationManager.notify(id, builder.build());
```

其实整个流程就是，在XML文件中设置parentActivityName属性，创建一个新栈，pendingIntent所启动的Activity在最上面，new builder封装后发送通知

设置特殊 Activity PendingIntent

特殊 Activity 无需返回栈，因此您不必在清单文件中定义其 Activity 层次结构，也不必调用 addParentStack() 来构建返回栈。取而代之的是，您可使用清单文件设置 Activity 任务选项，并通过调用 getActivity() 创建 PendingIntent：

在清单文件中，将以下属性添加到 Activity 的 <activity> 元素

```
android:name="activityclass"
```

Activity 的完全限定类名。

```
android:taskAffinity=""
```

与您在代码中设置的 FLAG_ACTIVITY_NEW_TASK 标志相结合，这可确保此 Activity

不会进入应用的默认任务。任何具有应用默认关联的现有任务均不受影响。

```
android:excludeFromRecents="true"
```

将新任务从“最新动态”中排除，这样用户就不会在无意中导航回它。

以下代码段显示了该元素：

```
<activity

    android:name=".ResultActivity"
...

    android:launchMode="singleTask"
    android:taskAffinity=""
    android:excludeFromRecents="true">

</activity>
...
```

构建并发出通知：

创建可启动 Activity的 Intent。

通过使用 FLAG_ACTIVITY_NEW_TASK 和 FLAG_ACTIVITY_CLEAR_TASK 标志调用 setFlags()，将 Activity 设置为在新的空任务中启动。

为 Intent 设置所需的任何其他选项。

通过调用 getActivity() 从 Intent 中创建 PendingIntent。 然后，您可以使用此 PendingIntent 作为 setContentIntent() 的参数。

以下代码段演示了该流程：

```
// Instantiate a Builder object.

NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

// Creates an Intent for the Activity

Intent notifyIntent =
        new Intent(this, ResultActivity.class);

// Sets the Activity to start in a new, empty task

notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
// Creates the PendingIntent

PendingIntent notifyPendingIntent =

        PendingIntent.getActivity(
        this,
        0,
        notifyIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
);

// Puts the PendingIntent into the notification builder

builder.setContentIntent(notifyPendingIntent);

// Notifications are issued by sending them to the
// NotificationManager system service.

NotificationManager mNotificationManager =
    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

// Builds an anonymous Notification object from the builder, and
// passes it to the NotificationManager

mNotificationManager.notify(id, builder.build())
```

在通知中显示进度

通知可能包括动画形式的进度指示器，向用户显示正在进行的操作状态。 如果您可以估计操作所需的时间以及任意时刻的完成进度，则使用“限定”形式的指示器（进度栏）。 如果无法估计操作的时长，则使用“非限定”形式的指示器（Activity 指示器）。

平台的 ProgressBar 类实现中显示有进度指示器。

要在 Android 4.0 及更高版本的平台上使用进度指示器，需调用 setProgress()。对于早期版本，您必须创建包括 ProgressBar 视图的自定义通知布局。

下文介绍如何使用 setProgress() 在通知中显示进度。

显示持续时间固定的进度指示器

要显示限定形式的进度栏，请通过调用 setProgress(max, progress, false) 将进度栏添加到通知，然后发出通知。随着操作继续进行，递增 progress 并更新通知。操作结束时， progress 应该等于 max。调用 setProgress() 的常见方法是将 max 设置为 100，然后将 progress 作为操作的“完成百分比”值递增。

您可以在操作完成后仍保留显示进度栏，也可以将其删除。无论哪种情况，都请记住更新通知文本以显示操作已完成。 要删除进度栏，请调用 setProgress(0, 0, false)。例如：

```
...

mNotifyManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

mBuilder = new NotificationCompat.Builder(this);

mBuilder.setContentTitle("Picture Download")

    .setContentText("Download in progress")
    .setSmallIcon(R.drawable.ic_notification);
// Start a lengthy operation in a background thread

new Thread(

    new Runnable() {
        @Override
        public void run() {
            int incr;
            // Do the "lengthy" operation 20 times
            for (incr = 0; incr <= 100; incr+=5) {
                    // Sets the progress indicator to a max value, the
                    // current completion percentage, and "determinate"
                    // state
                    mBuilder.setProgress(100, incr, false);
                    // Displays the progress bar for the first time.
                    mNotifyManager.notify(0, mBuilder.build());
                        // Sleeps the thread, simulating an operation
                        // that takes time
                        try {
                            // Sleep for 5 seconds
                            Thread.sleep(5*1000);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "sleep failure");
                        }
            }
            // When the loop is finished, updates the notification
            mBuilder.setContentText("Download complete")
            // Removes the progress bar
                    .setProgress(0,0,false);
            mNotifyManager.notify(ID, mBuilder.build());
        }
    }
// Starts the thread by calling the run() method in its Runnable
).start();
```

显示持续 Activity 指示器

要显示非限定形式的 Activity 指示器，请使用 setProgress(0, 0, true) 将其添加到通知（忽略前两个参数），然后发出通知。这样一来，指示器的样式就与进度栏相同，只是其动画还在继续。

在操作开始之际发出通知。除非您修改通知，否则动画将一直运行。 操作完成后，调用 setProgress(0, 0, false)，然后更新通知以删除 Activity 指示器。 请务必这样做；否则，即使操作完成，动画仍将运行。同时，请记得更改通知文本，以表明操作已完成。

要了解 Activity 指示器的工作方式，请参阅上述代码段。找到以下几行：

```
// Sets the progress indicator to a max value, the current completion
// percentage, and "determinate" state

mBuilder.setProgress(100, incr, false);

// Issues the notification

mNotifyManager.notify(0, mBuilder.build());

将找到的这几行替换为以下几行：

 // Sets an activity indicator for an operation of indeterminate length

mBuilder.setProgress(0, 0, true);

// Issues the notification

mNotifyManager.notify(0, mBuilder.build());
```

通知元数据

通知可根据您使用以下 NotificationCompat.Builder 方法分配的元数据进行排序：

当设备处于“优先”模式时，setCategory() 会告知系统如何处理应用通知（例如，通知代表传入呼叫、即时消息还是闹铃）。

如果优先级字段设置为 PRIORITY_MAX 或 PRIORITY_HIGH 的通知还有声音或振动，则 setPriority() 会将其显示在小型浮动窗口中。

addPerson() 允许您向通知添加人员名单。您的应用可以使用此名单指示系统将指定人员发出的通知归成一组，或者将这些人员发出的通知视为更重要的通知。
浮动通知

对于 Android 5.0（API 级别 21），当设备处于活动状态时（即，设备未锁定且其屏幕已打开），通知可以显示在小型浮动窗口中（也称为“浮动通知”）。 这些通知看上去类似于精简版的通知​​，只是浮动通知还显示操作按钮。 用户可以在不离开当前应用的情况下处理或清除浮动通知。

可能触发浮动通知的条件示例包括：

用户的 Activity 处于全屏模式中（应用使用 fullScreenIntent），或者通知具有较高的优先级并使用铃声或振动

锁定屏幕通知

随着 Android 5.0（API 级别 21）的发布，通知现在还可显示在锁定屏幕上。您的应用可以使用此功能提供媒体播放控件以及其他常用操作。 用户可以通过“设置”选择是否将通知显示在锁定屏幕上，并且您可以指定您应用中的通知在锁定屏幕上是否可见。

设置可见性

您的应用可以控制在安全锁定屏幕上显示的通知中可见的详细级别。 调用 setVisibility() 并指定以下值之一：


- VISIBILITY_PUBLIC 显示通知的完整内容。

- VISIBILITY_SECRET 不会在锁定屏幕上显示此通知的任何部分。

- VISIBILITY_PRIVATE 显示通知图标和内容标题等基本信息，但是隐藏通知的完整内容。

设置 VISIBILITY_PRIVATE 后，您还可以提供其中隐藏了某些详细信息的替换版本通知内容。例如，短信 应用可能会显示一条通知，指出“您有 3 条新短信”，但是隐藏了短信内容和发件人。要提供此替换版本的通知，请先使用 NotificationCompat.Builder 创建替换通知。创建专用通知对象时，请通过 setPublicVersion() 方法为其附加替换通知。

在锁定屏幕上控制媒体播放

在 Android 5.0（API 级别 21）中，锁定屏幕不再基于 RemoteControlClient（现已弃用）显示媒体控件。取而代之的是，将 Notification.MediaStyle 模板与 addAction() 方法结合使用，后者可将操作转换为可点击的图标。

注：该模板和 addAction() 方法未包含在支持库中，因此这些功能只能在 Android 5.0 及更高版本的系统上运行。

要在 Android 5.0 系统的锁定屏幕上显示媒体播放控件，请将可见性设置为 VISIBILITY_PUBLIC，如上文所述。然后，添加操作并设置 Notification.MediaStyle 模板，如以下示例代码中所述：

```
Notification notification = new Notification.Builder(context)

    // Show controls on lock screen even when user hides sensitive content.
    .setVisibility(Notification.VISIBILITY_PUBLIC)
    .setSmallIcon(R.drawable.ic_stat_player)
    // Add media control buttons that invoke intents in your media service
    .addAction(R.drawable.ic_prev, "Previous", prevPendingIntent) // #0
    .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)  // #1
    .addAction(R.drawable.ic_next, "Next", nextPendingIntent)     // #2
    // Apply the media style template
    .setStyle(new Notification.MediaStyle()
    .setShowActionsInCompactView(1 /* #1: pause button */)
    .setMediaSession(mMediaSession.getSessionToken())
    .setContentTitle("Wonderful music")
    .setContentText("My Awesome Band")
    .setLargeIcon(albumArtBitmap)
    .build();
```

注：弃用 RemoteControlClient 会对控制媒体产生进一步的影响

自定义通知布局

您可以利用通知框架定义自定义通知布局，由该布局定义通知在 RemoteViews 对象中的外观。 自定义布局通知类似于常规通知，但是它们是基于 XML 布局文件中所定义的 RemoteViews。

自定义通知布局的可用高度取决于通知视图。普通视图布局限制为 64 dp，扩展视图布局限制为 256 dp。

要定义自定义通知布局，请首先实例化 RemoteViews 对象来扩充 XML 布局文件。然后，调用 setContent()，而不是调用 setContentTitle() 等方法。要在自定义通知中设置内容详细信息，请使用 RemoteViews 中的方法设置视图子项的值：

在单独的文件中为通知创建 XML 布局。您可以根据需要使用任何文件名，但必须使用扩展名 .xml。

在您的应用中，使用 RemoteViews 方法定义通知的图标和文本。通过调用 setContent() 将此 RemoteViews 对象放入 NotificationCompat.Builder 中。避免在 RemoteViews 对象上设置背景 Drawable，因为文本颜色可能使文本变得难以阅读。

此外，RemoteViews 类中还有一些方法可供您轻松将 Chronometer 或 ProgressBar 添加到通知布局。如需了解有关为通知创建自定义布局的详细信息，请参阅 RemoteViews 参考文档。

注意

使用自定义通知布局时，要特别注意确保自定义布局适用于不同的设备方向和分辨率。 尽管这条建议适用于所有“视图”布局，但对通知尤为重要，因为抽屉式通知栏中的空间非常有限。 不要让自定义布局过于复杂，同时确保在各种配置中对其进行测试。

对自定义通知文本使用样式资源

始终对自定义通知的文本使用样式资源。通知的背景颜色可能因设备和系统版本的不同而异，使用样式资源有助于您充分考虑到这一点。 从 Android 2.3 开始，系统定义了标准通知布局文本的样式。若要在面向 Android 2.3 或更高版本系统的多个应用中使用相同样式，则应确保文本在显示背景上可见。


取消通知事件

通知被用户取消时发送(清除所有，右滑删除)
“自动取消(FLAG_AUTO_CANCEL)”不会产生该事件

```
Intent intent = new Intent(ACTION);

intent.putExtra("op", op);

PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, 0);

builder.setDeleteIntent(pi);
```

提醒通知到达

提供了 铃声/振动/呼吸灯 三种提醒方式，可以使用一种或同时使用多种

使用默认提醒

FLAG | 描述
---|---
Notification.DEFAULT_SOUND | 添加默认声音提醒
Notification.DEFAULT_VIBRATE | 添加默认震动提醒
Notification.DEFAULT_LIGHTS | 添加默认呼吸灯提醒
Notification.DEFAULT_ALL | 同时添加以上三种默认提醒

// 添加默认声音提醒

```
builder.setDefaults(Notification.DEFAULT_SOUND);
```

// 添加默认呼吸灯提醒，自动添加FLAG_SHOW_LIGHTS

```
builder.setDefaults(Notification.DEFAULT_LIGHTS);
```

添加自定义提醒

// 添加自定义声音提醒

```
builder.setSound(Uri.parse("path/to/sound"));
```

// 添加自定义震动提醒

// 延迟200ms后震动300ms，再延迟400ms后震动500ms

```
long[] pattern = new long[]{200,300,400,500};
builder.setVibrate(pattern);
```

// 添加自定义呼吸灯提醒，自动添加FLAG_SHOW_LIGHTS

```
int argb = 0xffff0000;  // led灯光颜色

int onMs = 300;         // led亮灯持续时间

int offMs = 100;        // led熄灯持续时间

builder.setLights(argb, onMs, offMs);
```


对Builder进行配置示例：

```
mBuilder.setContentTitle("测试标题")//设置通知栏标题

    .setContentText("测试内容") /<span style="font-family: Arial;">/设置通知栏显示内容</span>
    .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL)) //设置通知栏点击意图
//  .setNumber(number) //设置通知集合的数量
    .setTicker("测试通知来啦") //通知首次出现在通知栏，带上升动画效果的
    .setWhen(System.currentTimeMillis())//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
    .setPriority(Notification.PRIORITY_DEFAULT) //设置该通知优先级
//  .setAutoCancel(true)//设置这个标志当用户单击面板就可以让通知将自动取消
    .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
    .setDefaults(Notification.DEFAULT_VIBRATE)//向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合
    //Notification.DEFAULT_ALL  Notification.DEFAULT_SOUND 添加声音 // requires VIBRATE permission
    .setSmallIcon(R.drawable.ic_launcher);//设置通知小ICON
```

RemoteView的使用
android:使用RemoteView自定义Notification，在service中的情况
step1 准备自定义layout

常规的实现方式,并不会因为是用于notification的而在实现上有所不同.

```
<?xml version="1.0" encoding="utf-8"?>

<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"

              android:orientation="horizontal"
              android:layout_margin="10dp"
              android:layout_width="match_parent"
              android:layout_height="match_parent">
    <LinearLayout
        android:layout_gravity="center"
        android:layout_marginLeft="10dp"
        android:orientation="vertical"
        android:layout_width="170dp"
        android:layout_height="wrap_content">
        <TextView
            android:layout_marginLeft="10dp"
            android:id="@+id/music_name"
            android:textSize="20dp"
            android:text="要怎么办"
            android:maxLines="1"
            android:ellipsize="end"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>
        <TextView
            android:layout_marginLeft="10dp"
            android:layout_marginTop="6dp"
            android:id="@+id/singer_name"
            android:textSize="16dp"
            android:text="李柏凝"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>

    </LinearLayout>

    <LinearLayout
        android:layout_gravity="center"
        android:layout_marginTop="5dp"
        android:orientation="horizontal"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center">
        <ImageButton
            android:id="@+id/btn_prev"
            android:background="@drawable/desk_pre"
            android:layout_width="40dp"
            android:layout_height="40dp"/>
        <ImageButton
            android:layout_marginLeft="10dp"
            android:id="@+id/btn_play"
            android:src="@drawable/note_btn_play"
            android:background="#00ffffff"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>
        <ImageButton
            android:layout_marginLeft="10dp"
            android:id="@+id/btn_next"
            android:background="@drawable/desk_next"
            android:layout_width="40dp"
            android:layout_height="40dp"/>
    </LinearLayout>
</LinearLayout>
```

//以下内容均为service中的实现

step2 使用以上layout文件创建一个RemoteView实例

```
    private void initRemoteView() {

        //创建一个RemoteView实例
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.music_notification);
        mRemoteViews.setTextViewText(R.id.music_name, mMusicDatas.get(i).getName());
        mRemoteViews.setTextViewText(R.id.singer_name, mMusicDatas.get(i).getSinger());

        //实例化一个指向MusicService的intent
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(ACTION_NOTIFICATION);

        //设置play按钮的点击事件
        intent.putExtra(BUTTON_INDEX, BUTTON_PLAY);
        PendingIntent pendingIntent = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.btn_play, pendingIntent);

        //设置next按钮的点击事件
        intent.putExtra(BUTTON_INDEX, BUTTON_NEXT);
        pendingIntent = PendingIntent.getService(this, 3, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.btn_next, pendingIntent);

        //设置prev按钮的点击事件
        intent.putExtra(BUTTON_INDEX, BUTTON_PREV);
        pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.btn_prev, pendingIntent);
    }
```

step3 使用RemoteView实例创建Nitification

```
    private void initNotification() {

        //实例化一个Builder
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setSmallIcon(R.drawable.default_pic);
        //将remoteView设置进去
        mBuilder.setContent(mRemoteViews);
        //获取NotificationManager实例
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
```
step4 重写onStartCommand()用于处理Notification中按钮的点击事件,举例如下:

```
 @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        String stringExtra = intent.getStringExtra(BUTTON_INDEX);

        //校验action
        if(TextUtils.equals(action, ACTION_NOTIFICATION)) {
            //校验stringExtra
            if (TextUtils.equals(stringExtra, BUTTON_NEXT)) {
                i = (i+1)>=mMusicDatas.size()? 0 : i+1;
                mMediaPlayer.stop();
                mMediaPlayer = MediaPlayer.create(MusicService.this, mMusicDatas.get(i).getSrc());
                if(isPlaying) {
                    mMediaPlayer.start();
                }

                //重置Notification显示的内容
                mRemoteViews.setTextViewText(R.id.music_name, mMusicDatas.get(i).getName());
                mRemoteViews.setTextViewText(R.id.singer_name, mMusicDatas.get(i).getSinger());
                mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            } else if(TextUtils.equals(stringExtra, BUTTON_PLAY)) {
               //...
            } else {
               //...
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }
```

