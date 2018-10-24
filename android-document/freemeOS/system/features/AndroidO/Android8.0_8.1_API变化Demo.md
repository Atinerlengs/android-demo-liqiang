[toc]
# API变化
### findViewById(int id)
**api25**

|Return|Public Methods|
|-- |--|
|View | findViewById(int id)|

```
findViewById(int id)
Finds a view that was identified by the android:id XML attribute that was processed in onCreate(Bundle).
```

举个栗子:

```
//need cast to "android.widget.Button"
Button btn = (Button)findViewById(R.id.btn_test);
```

---
**api26&27**

|Return|Public Methods|
|-- |--|
|<T extends View> T | findViewById(int id)|

```
findViewById(int id)
Finds a view that was identified by the android:id XML attribute that was processed in onCreate(Bundle).
```

举个栗子:

```
Button btn = findViewById(R.id.btn_test);
```

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

### Autosizing TextViews

**api26&27**
效果图展示
|first picture |second picture| third picture|
|-- |-- |-- |
|<img src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171205-112839.png" /> |<img src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171205-113006.png" /> |<img src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171205-113126.png" /> |

有三种方式设置自动大小的TextView如下：


- **Default**

在java中使用默认的自动大小的text

```
TextViewCompat.setAutoSizeTextTypeWithDefaults(TextView textview, int autoSizeTextType)
//TextView widget and one of the text types, such as TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE or TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM.
```

在xml中使用

```
<?xml version="1.0" encoding="utf-8"?>
<TextView
    android:layout_width="match_parent"
    android:layout_height="200dp"
    android:autoSizeTextType="uniform" />
```

**note** : The default dimensions for uniform scaling are minTextSize = 12sp, maxTextSize = 112sp, and granularity = 1px.

- **Granularity**

在java中定义text sizes的范围

```
TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(int autoSizeMinTextSize, int autoSizeMaxTextSize, int autoSizeStepGranularity, int unit)
```

maximum value, the minimum value, the granularity value, and any TypedValue dimension unit.

在xml中定义text sizes的范围

```
<?xml version="1.0" encoding="utf-8"?>
<TextView
    android:layout_width="match_parent"
    android:layout_height="200dp"
    android:autoSizeTextType="uniform"
    android:autoSizeMinTextSize="12sp"
    android:autoSizeMaxTextSize="100sp"
    android:autoSizeStepGranularity="2sp" />
```

- **Preset Sizes**

在res/values/arrays.xml中定义资源数组

```
<resources>
  <array name="autosize_text_sizes">
    <item>10sp</item>
    <item>12sp</item>
    <item>20sp</item>
    <item>40sp</item>
    <item>100sp</item>
  </array>
</resources>
```

在autoSizePresetSizes属性中添加上面的资源数组

```
<?xml version="1.0" encoding="utf-8"?>
<TextView
    android:layout_width="match_parent"
    android:layout_height="200dp"
    android:autoSizeTextType="uniform"
    android:autoSizePresetSizes="@array/autosize_text_sizes />
```

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

### Fonts in XML
**Api26&27**

新特性，将fonts作为资源使用

在java代码中:

```
Typeface typeface = getResources().getFont(R.font.dancing_script);
textView.setTypeface(typeface);
```

在xml中使用

```
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:fontFamily="@font/dancing_script"
    android:textSize="50sp"
    android:text="@string/xinsichen" />
```

效果图：

|效果图1|
|--|
| <img width="300px" src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/font_xinsichen.png"  /> |

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/EditText_Demo)**

### Shortcuts
**api25**
增加了app shortcuts

**api26**
增加了pinned shortcuts

|展示图|Demo效果图|
|-- |--|
|  <img width="300px" src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/pinned-shortcuts.png"  /> |<img width="300px" src="https://raw.githubusercontent.com/chenxinsi/Pictures/master/demo_pinned_shortcut.png"  />|

使用 Pinned Shortcuts

1.isRequestPinShortcutSupported()
验证设备的默认启动支持程序快捷启动方式

2.两种创建ShortcutInfo的方式

a. 如果该快捷方式已经存在，则创建一个ShortcutInfo只包含新快捷方式的ID的对象。系统会自动查找并锁定与快捷方式有关的所有其他信息。

b.如果要固定新的快捷方式，请创建一个ShortcutInfo新快捷方式包含 ID，Intent， short label

注意：如果用户不允许将快捷方式固定到启动器，则你的app不会收到callback

```
ShortcutManager mShortcutManager =
        context.getSystemService(ShortcutManager.class);

if (mShortcutManager.isRequestPinShortcutSupported()) {
    // Assumes there's already a shortcut with the ID "my-shortcut".
    // The shortcut must be enabled.
    ShortcutInfo pinShortcutInfo =
            new ShortcutInfo.Builder(context, "my-shortcut").build();

    // Create the PendingIntent object only if your app needs to be notified
    // that the user allowed the shortcut to be pinned. Note that, if the
    // pinning operation fails, your app isn't notified. We assume here that the
    // app has implemented a method called createShortcutResultIntent() that
    // returns a broadcast intent.
    Intent pinnedShortcutCallbackIntent =
            mShortcutManager.createShortcutResultIntent(pinShortcutInfo);

    // Configure the intent so that your app's broadcast receiver gets
    // the callback successfully.
    PendingIntent successCallback = PendingIntent.getBroadcast(context, 0,
            pinnedShortcutCallbackIntent, 0);

    mShortcutManager.requestPinShortcut(pinShortcutInfo,
            successCallback.getIntentSender());
}
```

**[Demo地址](https://github.com/chenxinsi/Android8.x_demo/tree/master/Demo)**

### Picture in Picture


展示图

|picture1|picture2|picture3|
|--|--|--|
|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171211-154222.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171211-154144.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171211-154129.png)|

[ **Demo地址** ](https://github.com/googlesamples/android-PictureInPicture)

### AutofillFramework

首先我们要选择我们的自动填充服务

Settings > System > Languages & Input > Advanced > Auto-fill service

展示图

|picture1|picture2|picture3|picture4|picture5|
|-- |-- |-- |--|--|
| ![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144444.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144637.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144651.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144729.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Screenshot_20171214-144736.png) |

[ **Demo地址** ](https://github.com/googlesamples/android-PictureInPicture)

### JobScheduler

展示图
|picture1|picture2|picture3|
|-- |-- |-- |
| ![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/JobScheduler1.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/JobScheduler2.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/JobScheduler3.png) |

log：

```
12-18 11:50:21.828  1113  1113 I MyJobService: Service destroyed
12-18 11:51:32.414  1113  1113 I MyJobService: Service created
12-18 11:51:41.596  1113  1113 I MyJobService: on start job: 8
12-18 11:51:51.051  1113  1113 I MyJobService: Service destroyed
12-18 11:51:52.785  1113  1113 I MyJobService: Service created
12-18 11:51:54.810  1113  1113 I MyJobService: on start job: 0
12-18 11:52:24.838  1113  1113 I MyJobService: Service destroyed
12-18 11:52:41.473  1113  1113 I MyJobService: Service created
12-18 11:53:39.287  1113  1113 I MyJobService: Service destroyed
12-18 11:54:47.581  1113  1113 I MyJobService: Service created
12-18 11:54:55.080  1113  1113 I MyJobService: on start job: 1
12-18 11:55:34.551  1113  1113 I MyJobService: on start job: 2
12-18 11:56:04.552  1113  1113 I MyJobService: Service destroyed
12-18 11:56:07.900  1113  1113 I MyJobService: Service created
12-18 11:56:21.473  1113  1113 I MyJobService: on start job: 3
12-18 11:56:24.603  1113  1113 I MyJobService: Service destroyed
```

[ **Demo地址** ](https://github.com/googlesamples/android-JobScheduler)

### Notifications

展示图

|picture1|picture2|picture3|picture4|
|-- |-- |-- |--|
| ![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification.png)|![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification1.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification3.png) |![image](https://raw.githubusercontent.com/chenxinsi/Pictures/master/Notification4.png) |

[ **Demo地址** ](https://github.com/googlesamples/android-NotificationChannels)

## 参考
1. shortcuts:

https://developer.android.com/guide/topics/ui/shortcuts.html

## 代码地址
官方Samples：

https://github.com/googlesamples

集成demo：

https://github.com/chenxinsi/Android8.x_demo
