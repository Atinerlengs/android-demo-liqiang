
[toc]

# Android 基础知识

## 简介
具体参考[Android官方简介](https://developer.android.google.cn/guide/index.html)

## 应用组件

利用 Android 应用框架，您可以使用一组可重复使用的组件创建丰富的创新应用。此部分阐述您可以如何构建用于定义应用构建基块的组件，以及如何使用 Intent 将这些组件连接在一起。

### Intent和Intent过滤器

Intent是一种消息传递对象，可以使用它从其它应用组件请求操作，可以通过多种方式促进组件之间通信，主要的是以下三中:

- 启动Activity

    通过startActivity(intent)s来启动activity 。如果需要返回值，则可以调用startActivityForResult()

- 启动Service

    通过startService(intent)，如果服务旨在使用客户端-服务器接口， bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

- 传递Broadcasts

    通过给sendBroadcast(intent)启动广播

#### Intent类型

Intent分为两种类型，如下:

- 显示intent

    按名称（完全限定类名）指定要启动的组件。 通常，您会在自己的应用中使用显式 Intent 来启动组件，这是因为您知道要启动的 Activity 或服务的类名。例如，启动新 Activity 以响应用户操作，或者启动服务以在后台下载文件。

    举例：

    第一个Activity启动

    ```
    //显示启动
    Intent intent = new Intent(MainActivity.this,SecondActivity.class);
    intent.putExtra("name","第二个Activity");
    startActivity(intent);
    ```

    第二个Activity接收处理：

    ```
    Intent intent = getIntent();
    String name = intent.getStringExtra("name");
    ```

- 隐式intent

    ```
    //隐式启动
    // Create the text message with a string
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.putExtra(Intent.EXTRA_TEXT, "for test");
    sendIntent.setType("text/plain");

    // Verify that the intent will resolve to an activity
    if (sendIntent.resolveActivity(getPackageManager()) !=null) {
            startActivity(sendIntent);
    }

    Intent i = new Intent();
    //里面action需要在AndroidManifest.xml里面注册
    i.setAction("com.seconddemo.otheractivity");
    i.putExtra("test","xinxin");
    startActivity(i);
    ```

> 注意：用户可能没有任何应用处理您发送到 startActivity() 的隐式 Intent。如果出现这种情况，则调用将会失败，且应用会崩溃。要验证 Activity 是否会接收 Intent，请对 Intent 对象调用 resolveActivity()。如果结果为非空，则至少有一个应用能够处理该 Intent，且可以安全调用 startActivity()。 如果结果为空，则不应使用该 Intent。如有可能，您应停用发出该 Intent 的功能。


> 注意：为了确保应用的安全性，启动 Service 时，请始终使用显式 Intent，且不要为服务声明 Intent 过滤器。使用隐式 Intent 启动服务存在安全隐患，因为您无法确定哪些服务将响应 Intent，且用户无法看到哪些服务已启动。从 Android 5.0（API 级别 21）开始，如果使用隐式 Intent 调用 bindService()，系统会引发异常。


#### 强制使用应用的选择器

要显示选择器，可使用createChooser()创建Intent，并将其传递给StartActivity()

```
Intent sendIntent = new Intent();
sendIntent.setAction(Intent.ACTION_SEND);
//设置类型
sendIntent.setType("text/plain");
sendIntent.putExtra(Intent.EXTRA_TEXT, text);
Intent chooser = Intent.createChooser(sendIntent, "share");
// Verify the original intent will resolve to at least one activity
if (sendIntent.resolveActivity(getPackageManager()) != null) {
    startActivity(chooser);
}
```

#### 接受隐式Intent

要公布应用可以接受哪些隐式intent，需要在AndroidManifest.xml文件里面使用<intent-filter>元素为每一个应用组件申请一个或者多个intent过滤器。每个 Intent 过滤器均根据 Intent 的操作、数据和类别指定自身接受的 Intent 类型。

> 注：显示Intent始终会传递给其他目标，无论组件声明的Intent过滤器如果均是如此

一般的我们按照下面添加

```
<activity android:name=".OtherActivity">
    <intent-filter>
        <action android:name="com.seconddemo.otheractivity"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
```

可以创建一个包括多个 <action>、<data> 或 <category> 实例的过滤器。创建时，仅需确定组件能够处理这些过滤器元素的任何及所有组合即可。

>注意：为了避免无意中运行不同应用的 Service，请始终使用显式 Intent 启动您自己的服务，且不必为该服务声明 Intent 过滤器。

>对于所有 Activity，您必须在清单文件中声明 Intent 过滤器。但是，广播接收器的过滤器可以通过调用 registerReceiver() 动态注册。 稍后，您可以使用 unregisterReceiver() 注销该接收器。这样一来，应用便可仅在应用运行时的某一指定时间段内侦听特定的广播。

##### 过滤器实例
为了更好地了解一些 Intent 过滤器的行为，我们一起来看看从社交共享应用的清单文件中截取的以下片段。

```
<activity android:name="ShareActivity">
    <!-- This activity handles "SEND" actions with text data -->
    <intent-filter>
        <action android:name="android.intent.action.SEND"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="text/plain"/>
    </intent-filter>
    <!-- This activity also handles "SEND" and "SEND_MULTIPLE" with media data -->
    <intent-filter>
        <action android:name="android.intent.action.SEND"/>
        <action android:name="android.intent.action.SEND_MULTIPLE"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="application/vnd.google.panorama360+jpg"/>
        <data android:mimeType="image/*"/>
        <data android:mimeType="video/*"/>
    </intent-filter>
</activity>
```

#### 通用Intent
这个就不多做说明了，主要是几种可用于执行的常见操作的隐式Intent，参考[通用Intennt](https://developer.android.google.cn/guide/components/intents-common.html)

下面就举一个联系人例子

```
static final int REQUEST_SELECT_CONTACT = 1;

public void selectContact() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    //通过CibtactsContract.Contacts.CONTENT_TYPE去访问联系人信息
    intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
    if (intent.resolveActivity(getPackageManager()) != null) {
        startActivityForResult(intent, REQUEST_SELECT_CONTACT);
    }
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SELECT_CONTACT && resultCode == RESULT_OK) {
        //可以从这个data里面获取联系人的数据
        Uri contactUri = data.getData();
        // Do something with the selected contact at contactUri
        ...
    }
}
```


### Activity

Activity 是一个应用组件，用户可与其提供的屏幕进行交互，以执行拨打电话、拍摄照片、发送电子邮件或查看地图等操作。 每个 Activity 都会获得一个用于绘制其用户界面的窗口。窗口通常会充满屏幕，但也可小于屏幕并浮动在其他窗口之上。

#### Ativity

##### 创建Activity

需要继承Activity或者是继承Activiy子类，然后在其中实现Activity的生命周期各种状态转变时系统调用的回调方法。

- onCreate()

您必须实现此方法。系统会在创建您的 Activity 时调用此方法。您应该在实现内初始化 Activity 的必需组件。 最重要的是，您必须在此方法内调用 setContentView()，以定义 Activity 用户界面的布局。

- onPause()

系统将此方法作为用户离开 Activity 的第一个信号（但并不总是意味着 Activity 会被销毁）进行调用。 您通常应该在此方法内确认在当前用户会话结束后仍然有效的任何更改（因为用户可能不会返回）。


###### 实现用户界面

在onCreate()方法里面使用setContentView(R.layout.activity_main).也可以在Activity里面创建新的View，然后将新的View插入ViewGroup来创建视图层次。

###### 在清单文件中声明Activity

```
<activity   android:name=".MainActivity"
            android:label="@string/launcherActivityLabel"
            android:theme="@style/DialtactsActivityTheme"
            android:launchMode="singleTask"
            android:clearTaskOnLaunch="true"
            android:icon="@mipmap/ic_launcher_phone"
            android:windowSoftInputMode="stateAlwaysHidden|adjustNothing"
            android:resizeableActivity="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

可以在<intent-filter>使用Intentn过滤器，
<action>元素指定这个是程序的“主”入口，<category>元素指定此 Activity 应列入系统的应用启动器内（以便用户启动该 Activity）。

##### 启动Activity
调用StartActivity()启动Activity，下面例子是传值并且启动Activity

```
Intent intent = new Intent(this, SecondClass.this);
intent.putExtra("name","jack");
startActivity(intent);
```

###### startActivityForResult()
举例说明：

```
//启动SecondActivity
Intent intent = new Intent(MainActivity.this,SecondActivity.class);
intent.putExtra("name","第二个Activity");
startActivityForResult(intent,REQUEST_CODE);

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //resultCode判断请求是否成功，requestCode与上面startActivityForResult发送的第二个参数匹配
    if (resultCode == 2 && requestCode == REQUEST_CODE) {
        Log.v(TAG,data.getStringExtra("SecondName"));
    }
    super.onActivityResult(requestCode, resultCode, data);
    }

```

SecondActivity调用setResult()方法

```
//SecondActiviy里面setResult
getIntent().putExtra("SecondName","第二个Activity返回");
setResult(2,intent);
```

##### 结束Activity

调用finish()方法去结束Activity

> PS:官方文档说还可以通过finishActivity(requestCode)方法去结束Activity，验证无效果,查资料也没有说清楚这个方法是咋用.......

##### Activity生命周期

Activity一般以以下三种状态存在：

- OnResume()

    此Activity位于屏幕前台，并且具有用户焦点

- onPause()

    另一个Activity位于屏幕前台并具有用户焦点，但此Activity仍可见。也就是说，另一个Activity显示在此Activity上方，并且该Activity部分透明或未覆盖整个屏幕。暂停的Activity处于完全活动状态(Activity对象保留在内存中，它保留了所有状态和成员信息，并与窗口管理器保持连接)，但是在内存极度不足的情况下，会被系统终止。

- onStop()

    该Activity被另一个Activity完全遮盖(该Activity目前位于“后台”)。已停止的Activity同样仍处于活动状态(Activity对象保留在内存中，它保留了所有状态和成员信息，但未与窗口管理器连接)。不过，它对用户不再可见，在其他需要内存是可能会被系统终止。

下图说明了Activity再状态变化期间可能经过的路径，矩形表示回调方法，当Activity在不同的状态之间转换，可以实现这些方法来执行操作
![image](https://developer.android.google.cn/images/activity_lifecycle.png)

详细的Activity生命周期参考官方文档[Activity生命周期](https://developer.android.google.cn/guide/components/activities.html#Lifecycle)

#####  onSaveInstanceState()

管理 Activity 生命周期的引言部分简要提及，当 Activity 暂停或停止时，Activity 的状态会得到保留。 确实如此，因为当 Activity 暂停或停止时，Activity 对象仍保留在内存中 — 有关其成员和当前状态的所有信息仍处于活动状态。 因此，用户在 Activity 内所做的任何更改都会得到保留，这样一来，当 Activity 返回前台（当它“继续”）时，这些更改仍然存在。

不过，当系统为了恢复内存而销毁某项 Activity 时，Activity 对象也会被销毁，因此系统在继续 Activity 时根本无法让其状态保持完好，而是必须在用户返回 Activity 时重建 Activity 对象。但用户并不知道系统销毁 Activity 后又对其进行了重建，因此他们很可能认为 Activity 状态毫无变化。 在这种情况下，您可以实现另一个回调方法对有关 Activity 状态的信息进行保存，以确保有关 Activity 状态的重要信息得到保留：onSaveInstanceState()。

系统会先调用 onSaveInstanceState()，然后再使 Activity 变得易于销毁。系统会向该方法传递一个 Bundle，您可以在其中使用 putString() 和 putInt() 等方法以名称-值对形式保存有关 Activity 状态的信息。然后，如果系统终止您的应用进程，并且用户返回您的 Activity，则系统会重建该 Activity，并将 Bundle 同时传递给 onCreate() 和 onRestoreInstanceState()。您可以使用上述任一方法从 Bundle 提取您保存的状态并恢复该 Activity 状态。如果没有状态信息需要恢复，则传递给您的 Bundle 是空值（如果是首次创建该 Activity，就会出现这种情况）。

```
   @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
        outState.putBoolean(KEY_IN_REGULAR_SEARCH_UI, mInRegularSearch);
        outState.putBoolean(KEY_IN_DIALPAD_SEARCH_UI, mInDialpadSearch);
        outState.putBoolean(KEY_FIRST_LAUNCH, mFirstLaunch);
        outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mIsDialpadShown);
        /// M: Save and restore the mPendingSearchViewQuery
        outState.putString(KEY_PENDING_SEARCH_QUERY, mPendingSearchViewQuery);
        mActionBarController.saveInstanceState(outState);

        mIsDialpadShown = false;
        if (mListsFragment != null) {
            mIsDialpadShown = mListsFragment.isDialpadShow();
        }
        outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mIsDialpadShown);
        mPendingSearchViewQuery = null;
        if (mListsFragment != null) {
            mPendingSearchViewQuery = mListsFragment.getDialpadQuery();
        }
        outState.putString(KEY_PENDING_SEARCH_QUERY, mPendingSearchViewQuery);

        mStateSaved = true;
    }
```

![image](https://developer.android.google.cn/images/fundamentals/restore_instance.png)

>注：无法保证系统会在销毁您的 Activity 前调用 onSaveInstanceState()，因为存在不需要保存状态的情况（例如用户使用“返回”按钮离开您的 Activity 时，因为用户的行为是在显式关闭 Activity）。 如果系统调用 onSaveInstanceState()，它会在调用 onStop() 之前，并且可能会在调用 onPause() 之前进行调用。



不过，即使您什么都不做，也不实现 onSaveInstanceState()，Activity 类的 onSaveInstanceState() 默认实现也会恢复部分 Activity 状态。具体地讲，默认实现会为布局中的每个 View 调用相应的 onSaveInstanceState() 方法，让每个视图都能提供有关自身的应保存信息。Android 框架中几乎每个小部件都会根据需要实现此方法，以便在重建 Activity 时自动保存和恢复对 UI 所做的任何可见更改。例如，EditText 小部件保存用户输入的任何文本，CheckBox 小部件保存复选框的选中或未选中状态。您只需为想要保存其状态的每个小部件提供一个唯一的 ID（通过 android:id 属性）。如果小部件没有 ID，则系统无法保存其状态。

##### Configuration Changes

有些设备配置可能会在运行时发生变化（例如屏幕方向、键盘可用性及语言）。 发生这种变化时，Android 会重启正在运行的 Activity（先后调用 onDestroy() 和 onCreate()）。重启行为旨在通过利用与新设备配置匹配的备用资源自动重新加载您的应用，来帮助它适应新配置。

但是，可能会遇到这种情况：重启应用并恢复大量数据不仅成本高昂，而且给用户留下糟糕的使用体验。 在这种情况下，有两个其他选择：

a.在配置变更期间保留对象

允许 Activity 在配置变更时重启，但是要将有状态对象传递给 Activity 的新实例。

b.自行处理配置变更

阻止系统在某些配置变更期间重启 Activity，但要在配置确实发生变化时接收回调，这样，您就能够根据需要手动更新 Activity。


###### 处理变更期间保留对象

如果重启 Activity 需要恢复大量数据、重新建立网络连接或执行其他密集操作，那么因配置变更而引起的完全重启可能会给用户留下应用运行缓慢的体验。 此外，依靠系统通过onSaveInstanceState() 回调为您保存的 Bundle，可能无法完全恢复 Activity 状态，因为它并非设计用于携带大型对象（例如位图），而且其中的数据必须先序列化，再进行反序列化，这可能会消耗大量内存并使得配置变更速度缓慢。 在这种情况下，如果 Activity 因配置变更而重启，则可通过保留 Fragment 来减轻重新初始化 Activity 的负担。此片段可能包含对您要保留的有状态对象的引用。

要在运行时配置变更期间将有状态的对象保留在片段中，请执行以下操作：

- 扩展 Fragment 类并声明对有状态对象的引用。
- 在创建片段后调用 setRetainInstance(boolean)。
- 将片段添加到 Activity。
- 重启 Activity 后，使用 FragmentManager 检索片段。

例如，按如下方式定义片段：

```
public class RetainedFragment extends Fragment {

    // data object we want to retain
    private MyDataObject data;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setData(MyDataObject data) {
        this.data = data;
    }

    public MyDataObject getData() {
        return data;
    }
}
```

>注意：尽管您可以存储任何对象，但是切勿传递与 Activity 绑定的对象，例如，Drawable、Adapter、View 或其他任何与 Context 关联的对象。否则，它将泄漏原始 Activity 实例的所有视图和资源。 （泄漏资源意味着应用将继续持有这些资源，但是无法对其进行垃圾回收，因此可能会丢失大量内存。）

然后，使用 FragmentManager 将片段添加到 Activity。在运行时配置变更期间再次启动 Activity 时，您可以获得片段中的数据对象。 例如，按如下方式定义 Activity：

```
public class MyActivity extends Activity {

    private RetainedFragment dataFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // find the retained fragment on activity restarts
        FragmentManager fm = getFragmentManager();
        dataFragment = (DataFragment) fm.findFragmentByTag(“data”);

        // create the fragment and data the first time
        if (dataFragment == null) {
            // add the fragment
            dataFragment = new DataFragment();
            fm.beginTransaction().add(dataFragment, “data”).commit();
            // load the data from the web
            dataFragment.setData(loadMyData());
        }

        // the data is available in dataFragment.getData()
        ...
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // store the data in the fragment
        dataFragment.setData(collectMyLoadedData());
    }
}
```

在此示例中，onCreate() 添加了一个片段或恢复了对它的引用。此外，onCreate() 还将有状态的对象存储在片段实例内部。onDestroy() 对所保留的片段实例内的有状态对象进行更新。




###### 自行处理配置

如果应用在特定配置变更期间无需更新资源，并且因性能限制您需要尽量避免重启，则可声明 Activity 将自行处理配置变更，这样可以阻止系统重启 Activity。

>注：自行处理配置变更可能导致备用资源的使用更为困难，因为系统不会为您自动应用这些资源。 只能在您必须避免 Activity 因配置变更而重启这一万般无奈的情况下，才考虑采用自行处理配置变更这种方法，而且对于大多数应用并不建议使用此方法。


例如，以下清单文件代码声明的 Activity 可同时处理屏幕方向变更和键盘可用性变更：

```
        <activity android:name="com.android.incallui.InCallActivity"
                  android:theme="@style/Theme.InCallScreen"
                  android:label="@string/phoneAppLabel"
                  android:excludeFromRecents="true"
                  android:launchMode="singleInstance"
                  android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|keyboardHidden|keyboard|navigation|mnc|mcc"
                  android:exported="false"
                  android:screenOrientation="nosensor"
                  android:directBootAware="true"
                  android:resizeableActivity="@bool/enable_multi_window">
        </activity>
```

现在，当其中一个配置发生变化时，MyActivity 不会重启。相反，MyActivity 会收到对 onConfigurationChanged() 的调用。向此方法传递 Configuration 对象指定新设备配置。您可以通过读取 Configuration 中的字段，确定新配置，然后通过更新界面中使用的资源进行适当的更改。调用此方法时，Activity 的 Resources 对象会相应地进行更新，以根据新配置返回资源，这样，您就能够在系统不重启 Activity 的情况下轻松重置 UI 的元素。

>注意：从 Android 3.2（API 级别 13）开始，当设备在纵向和横向之间切换时，“屏幕尺寸”也会发生变化。因此，在开发针对 API 级别 13 或更高版本（正如 minSdkVersion 和 targetSdkVersion 属性中所声明）的应用时，若要避免由于设备方向改变而导致运行时重启，则除了 "orientation" 值以外，您还必须添加 "screenSize" 值。 也就是说，您必须声明 android:configChanges="orientation|screenSize"。但是，如果您的应用面向 API 级别 12 或更低版本，则 Activity 始终会自行处理此配置变更（即便是在 Android 3.2 或更高版本的设备上运行，此配置变更也不会重启 Activity）。

```
        <activity android:name=".MainActivity"
            android:configChanges="orientation|screenSize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
```

以下 onConfigurationChanged() 实现检查当前设备方向：

```
@Override
public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    // Checks the orientation of the screen
    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
    }
}
```

不加android:configChanges横竖屏切换生命周期

```
//第一次进入
12-26 21:27:58.957 18323-18323/com.configurationchangesdemo D/liqiang: onCreate:
12-26 21:27:58.964 18323-18323/com.configurationchangesdemo D/liqiang: onStart:
12-26 21:27:58.968 18323-18323/com.configurationchangesdemo D/liqiang: onResume:
//切换成横屏
12-26 21:34:53.794 18323-18323/com.configurationchangesdemo D/liqiang: onPause:
12-26 21:34:53.796 18323-18323/com.configurationchangesdemo D/liqiang: onSaveInstanceState: name = jack
12-26 21:34:53.801 18323-18323/com.configurationchangesdemo D/liqiang: onStop:
12-26 21:34:53.802 18323-18323/com.configurationchangesdemo D/liqiang: onDestroy:
12-26 21:34:53.930 18323-18323/com.configurationchangesdemo D/liqiang: onCreate:
12-26 21:34:53.932 18323-18323/com.configurationchangesdemo D/liqiang: onStart:
12-26 21:34:53.933 18323-18323/com.configurationchangesdemo D/liqiang: onRestoreInstanceState: name = jack
12-26 21:34:53.935 18323-18323/com.configurationchangesdemo D/liqiang: onResume:
//切换成竖屏
12-26 21:35:44.087 18323-18323/com.configurationchangesdemo D/liqiang: onPause:
12-26 21:35:44.088 18323-18323/com.configurationchangesdemo D/liqiang: onSaveInstanceState: name = jack
12-26 21:35:44.094 18323-18323/com.configurationchangesdemo D/liqiang: onStop:
12-26 21:35:44.094 18323-18323/com.configurationchangesdemo D/liqiang: onDestroy:
12-26 21:35:44.157 18323-18323/com.configurationchangesdemo D/liqiang: onCreate:
12-26 21:35:44.160 18323-18323/com.configurationchangesdemo D/liqiang: onStart:
12-26 21:35:44.160 18323-18323/com.configurationchangesdemo D/liqiang: onRestoreInstanceState: name = jack
12-26 21:35:44.166 18323-18323/com.configurationchangesdemo D/liqiang: onResume:
```

添加android:configChanges="orientation|screenSize"横竖屏切换生命周期

```
//第一次进入
12-26 21:36:49.237 18534-18534/? D/liqiang: onCreate:
12-26 21:36:49.238 18534-18534/? D/liqiang: onStart:
12-26 21:36:49.241 18534-18534/? D/liqiang: onResume:
//切换成横屏
12-26 21:41:38.632 18534-18534/com.configurationchangesdemo D/liqiang: onConfigurationChanged:
//切换成竖屏
12-26 21:42:10.048 18534-18534/com.configurationchangesdemo D/liqiang: onConfigurationChanged:
```


如果需要在Activity里面处理哪些配置变更的详细信息，则可以查询[android:configChanges](https://developer.android.google.cn/guide/topics/manifest/activity-element.html#config)

---

#### Fragment

##### Fragment 原理

参考[Fragment设计原理](https://developer.android.google.cn/guide/components/fragments.html#Design)

##### 如何创建Fragment

要想创建片段，必须创建Fragment子类(或者已有的子类)，通常我们需要是想Fragment以下的生命周期:

- onCreate()

    系统会在创建Fragment的时候调用这个方法，

- onCreateView()

    系统会在片段首次绘制其用户界面时调用此方法，要想为您的片段绘制 UI，您从此方法中返回的 View必须是片段布局的根视图。如果片段未提供 UI，您可以返回 null。

- onPause()

    系统将此方法作为用户离开片段的第一个信号（但并不总是意味着此片段会被销毁）进行调用。 您通常应该在此方法内确认在当前用户会话结束后仍然有效的任何更改（因为用户可能不会返回）

下面是几个扩展的子类：

- DialogFragment

    显示浮动对话框。使用此类创建对话框可有效地替代使用 Activity 类中的对话框帮助程序方法，因为您可以将片段对话框纳入由 Activity 管理的片段返回栈，从而使用户能够返回清除的片段。

- ListFragment

    显示由适配器（如 SimpleCursorAdapter）管理的一系列项目，类似于 ListActivity。它提供了几种管理列表视图的方法，用于处理点击事件的  onListItemClick() 回调。

- PreferenceFragment

    以列表形式显示 Preference 对象的层次结构，类似于 PreferenceActivity。这在为您的应用创建“设置” Activity 时很有用处。

##### Activity中添加Fragment

- 静态添加Fragment

    在布局文件里面添加方法如下

```
<fragment
    android:id="@+id/first_fragment"
    android:name="com.fragment.FirstFragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
</fragment>
```

写demo验证的时候，没有添加id，一直报错如下

```
 Caused by: java.lang.IllegalArgumentException: Binary XML file line #9: Must specify unique android:id, android:tag, or have a parent with an id for com.fragment.FirstFragment
                                                                    at android.support.v4.app.FragmentManagerImpl.onCreateView(FragmentManager.java:3484)
```

查看源码，可以看在FragmentManager里面有，这里面id,tag至少有一项不能为空，所以我们静态添加fragment的时候要加上id。（虽然大部分时候都是添加的........）

```
if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
    throw new IllegalArgumentException(attrs.getPositionDescription()
            + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
}
```

>注：每个片段都需要一个唯一的标识符，重启 Activity 时，系统可以使用该标识符来恢复片段（您也可以使用该标识符来捕获片段以执行某些事务，如将其移除）。 可以通过三种方式为片段提供 ID：

> - 为 android:id 属性提供唯一 ID。
> - 为 android:tag 属性提供唯一字符串。
> - 如果您未给以上两个属性提供值，系统会使用容器视图的 ID。

- 动态添加Fragment

    可以在Activity运行期间随时将fragment添加到Activity布局中去，只需要指定将fragment放入哪个ViewGroup。

    具体如何在Activity里面添加移除或者替换fragment，需要使用FragmentTransaction中的API 。通过以下方法获取FragmentTransaction实例

```
//获取FragmentTransaction实例
FragmentManager fragmentManager = getFragmentManager();
FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
```

    然后可以用add方法去添加一个fragment

```
FragmentManager manager = getSupportFragmentManager();
FragmentTransaction transaction = manager.beginTransaction();
DynamicFragment fragment1 = new DynamicFragment();
transaction.add(R.id.fragment_container, fragment1);
transaction.commit();
```

传递到add()的第一个参数是ViewGroup，即应该放置片段的位置，由资源ID指定，第二个参数是要添加的片段，一旦通过FragmentTransaction做出了更改，必须用commit以使更改生效。

##### 执行Fragment

在Activity里面使用fragment最大的优点就是可以根据用户行为执行添加，移除替换。
您可以像下面这样从 FragmentManager 获取一个 FragmentTransaction 实例：

```
FragmentManager fragmentManager = getFragmentManager();
FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
```

每个事务都是您想要同时执行的一组更改。您可以使用 add()、remove() 和 replace() 等方法为给定事务设置您想要执行的所有更改。然后，要想将事务应用到 Activity，您必须调用 commit()。

不过，在您调用 commit() 之前，您可能想调用 addToBackStack()，以将事务添加到片段事务返回栈。 该返回栈由 Activity 管理，允许用户通过按返回按钮返回上一片段状态。

通过调用 addToBackStack() 可将替换事务保存到返回栈，以便用户能够通过按返回按钮撤消事务并回退到上一片段。

如果您向事务添加了多个更改（如又一个 add() 或 remove()），并且调用了 addToBackStack()，则在调用 commit() 前应用的所有更改都将作为单一事务添加到返回栈，并且返回按钮会将它们一并撤消。

向 FragmentTransaction 添加更改的顺序无关紧要，不过：

您必须最后调用 commit()
如果您要向同一容器添加多个片段，则您添加片段的顺序将决定它们在视图层次结构中的出现顺序
如果您没有在执行移除片段的事务时调用 addToBackStack()，则事务提交时该片段会被销毁，用户将无法回退到该片段。 不过，如果您在删除片段时调用了 addToBackStack()，则系统会停止该片段，并在用户回退时将其恢复。

>提示：对于每个片段事务，您都可以通过在提交前调用 setTransition() 来应用过渡动画。

调用 commit() 不会立即执行事务，而是在 Activity 的 UI 线程（“主”线程）可以执行该操作时再安排其在线程上运行。不过，如有必要，您也可以从 UI 线程调用 executePendingTransactions() 以立即执行 commit() 提交的事务。通常不必这样做，除非其他线程中的作业依赖该事务。

>注意：您只能在 Activity 保存其状态（用户离开 Activity）之前使用 commit() 提交事务。如果您试图在该时间点后提交，则会引发异常。 这是因为如需恢复 Activity，则提交后的状态可能会丢失。 对于丢失提交无关紧要的情况，请使用 commitAllowingStateLoss()。


##### 与Activity通信

Fragment可以通过getActivity()访问Activity实例，如下

```
View view = getActivity().findViewById(R.id.list);
```

同样的，Activity也可以通过findFragentById()或findFragmentByTag(),通过FragmentManager获取对Fragment的引用来调用fragmetn方法。

```
DynamicFragment fragment = (DynamicFragment) getFragmentManager().findFragmentById(R.id.dynamic_fragment);

```

下面一个例子是Activity获取fragment里面的值(通过回调)

Fragment里面

```
//接口回调
public void getEditText(CallBack callBack){
    //获取文本框信息
    String msg = editText.getText().toString();
    callBack.getResult(msg);
}
//接口
public interface CallBack{
    //定义一个获取方法
    public void getResult(String result);
}
```

Activity里面：

```
//使用接口回调的方法获取数据
leftFragment.getEditText(new LeftFragment.CallBack() {
    @Override
    public void getResult(String result) {
        Toast.makeText(CallbackActivity.this, "-->>" + result, Toast.LENGTH_SHORT).show();                    }
    });
```

[Fragment Demo](https://github.com/Atinerlengs/StudyNotes/tree/master/fragmentdemo)

##### Fragment生命周期

参考官方文档[Fragment生命周期](https://developer.android.google.cn/guide/components/fragments.html#Lifecycle)


#### Loaders

Android3.0开始引入了Loaders，支持轻松在Activity或fragment中异步加载数据。主要有以下特征:

- 可用于每一个Fragment和Activity

- 支持异步加载数据

- 监控其数据源并且在其改变时传递结果

- 在某一配置更改后重建加载器时，会自动重新连接上一个加载器的游标。 因此，它们无需重新查询其数据

[Loader API](https://developer.android.google.cn/guide/components/loaders.html#summary)

##### 在应用中使用加载器

此部分描述如何在 Android 应用中使用加载器。使用加载器的应用通常包括：

- Activity 或 Fragment。

- LoaderManager 的实例。

- 一个 CursorLoader，用于加载由 ContentProvider 支持的数据。您也可以实现自己的 Loader 或 AsyncTaskLoader 子类，从其他源中加载数据。

- 一个 LoaderManager.LoaderCallbacks 实现。您可以使用它来创建新加载器，并管理对现有加载器的引用

- 一种显示加载器数据的方法，如 SimpleCursorAdapter。

- 使用 CursorLoader 时的数据源，如 ContentProvider。

###### Starting a Loader

```
getLoaderManager().initLoader(0,null,this);
```

initLoader()方法采用以下参数:

- 用于标示加载器唯一ID

- 在构建时候提供给加载器可选参数

- LoaderManager.LoaderCallbacks 实现， LoaderManager 将调用此实现来报告加载器事件。在此示例中，本地类实现 LoaderManager.LoaderCallbacks 接口，因此它会传递对自身的引用 this。

initLoader() 调用确保加载器已初始化且处于活动状态。这可能会出现两种结果：

- 如果 ID 指定的加载器已存在，则将重复使用上次创建的加载器。

- 如果 ID 指定的加载器不存在，则 initLoader() 将触发 LoaderManager.LoaderCallbacks方法onCreateLoader()。在此方法中，您可以实现代码以实例化并返回新加载器。有关详细介绍，请参阅 onCreateLoader 部分。

无论何种情况，给定的 LoaderManager.LoaderCallbacks 实现均与加载器相关联，且将在加载器状态变化时调用。如果在调用时，调用程序处于启动状态，且请求的加载器已存在并生成了数据，则系统将立即调用 onLoadFinished()（在 initLoader() 期间），因此您必须为此做好准备。 有关此回调的详细介绍，请参阅 onLoadFinished。

请注意，initLoader() 方法将返回已创建的 Loader，但您不必捕获其引用。LoaderManager 将自动管理加载器的生命周期。LoaderManager 将根据需要启动和停止加载，并维护加载器的状态及其相关内容。 这意味着您很少直接与加载器进行交互（有关使用加载器方法调整加载器行为的示例，请参阅 LoaderThrottle 示例）。当特定事件发生时，您通常会使用 LoaderManager.LoaderCallbacks 方法干预加载进程。有关此主题的详细介绍，请参阅使用 LoaderManager 回调


###### ReStarting a Loader

当您使用 initLoader() 时（如上所述），它将使用含有指定 ID 的现有加载器（如有）。如果没有，则它会创建一个。但有时，您想舍弃这些旧数据并重新开始。

要舍弃旧数据，请使用 restartLoader()。例如，当用户的查询更改时，此 SearchView.OnQueryTextListener 实现将重启加载器。 加载器需要重启，以便它能够使用修订后的搜索过滤器执行新查询：

```
public boolean onQueryTextChanged(String newText) {
    // Called when the action bar search text has changed.  Update
    // the search filter, and restart the loader to do a new query
    // with this filter.
    mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
    getLoaderManager().restartLoader(0, null, this);
    return true;
}
```


##### Using the LoaderManager Callbacks


LoaderManager.LoaderCallbacks是一个支持客户端与LoaderManager交互的回调接口。

Loaer(特别是CursorLoader)在停止运行后，仍需保留其数据。这样，应用即可保留Activity或fragment的onStop()和onStart()方法中的数据。当用户返回应用时，无需等待它重新加载这些数据。可使用LoaderManager.LoaderCallbacks方法了解何时创建加载器，并告知应用何时停止使用加载器数据


###### onCreateLoader()：针对指定的 ID 进行实例化并返回新的 Loader

当您尝试访问加载器时（例如，通过 initLoader()），该方法将检查是否已存在由该 ID 指定的加载器。 如果没有，它将触发 LoaderManager.LoaderCallbacks 方法 onCreateLoader()。在此方法中，您可以创建新加载器。 通常，这将是 CursorLoader，但您也可以实现自己的 Loader 子类。

在此示例中，onCreateLoader() 回调方法创建了 CursorLoader。您必须使用其构造函数方法来构建 CursorLoader。该方法需要对 ContentProvider 执行查询时所需的一系列完整信息。具体地说，它需要：

- uri：用于检索内容的 URI
- projection：要返回的列的列表。传递 null 时，将返回所有列，这样会导致效率低下
- selection：一种用于声明要返回哪些行的过滤器，采用 SQL WHERE 子句格式（WHERE 本身除外）。传递 null 时，将为指定的 URI 返回所有行
- selectionArgs：您可以在 selection 中包含 ?s，它将按照在 selection 中显示的顺序替换为 selectionArgs 中的值。该值将绑定为字串符
- sortOrder：行的排序依据，采用 SQL ORDER BY 子句格式（ORDER BY 自身除外）。传递 null 时，将使用默认排序顺序（可能并未排序）

```
// If non-null, this is the current filter the user has provided.
String mCurFilter;
...
public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    // This is called when a new Loader needs to be created.  This
    // sample only has one Loader, so we don't care about the ID.
    // First, pick the base URI to use depending on whether we are
    // currently filtering.
    Uri baseUri;
    if (mCurFilter != null) {
        baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                  Uri.encode(mCurFilter));
    } else {
        baseUri = Contacts.CONTENT_URI;
    }

    // Now create and return a CursorLoader that will take care of
    // creating a Cursor for the data being displayed.
    String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND ("
            + Contacts.HAS_PHONE_NUMBER + "=1) AND ("
            + Contacts.DISPLAY_NAME + " != '' ))";
    return new CursorLoader(getActivity(), baseUri,
            CONTACTS_SUMMARY_PROJECTION, select, null,
            Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
}
```




###### onLoadFinished() ：将在先前创建的加载器完成加载时调用

当先前创建的加载器完成加载时，将调用此方法。该方法必须在为此加载器提供的最后一个数据释放之前调用。 此时，您应移除所有使用的旧数据（因为它们很快会被释放），但不要自行释放这些数据，因为这些数据归其加载器所有，其加载器会处理它们。

当加载器发现应用不再使用这些数据时，即会释放它们。 例如，如果数据是来自 CursorLoader 的一个游标，则您不应手动对其调用 close()。如果游标放置在 CursorAdapter 中，则应使用 swapCursor() 方法，使旧 Cursor 不会关闭。例如：

```
// This is the Adapter being used to display the list's data.
SimpleCursorAdapter mAdapter;
...

public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    // Swap the new cursor in.  (The framework will take care of closing the
    // old cursor once we return.)
    mAdapter.swapCursor(data);
}
```




###### onLoaderReset()：将在先前创建的加载器重置且其数据因此不可用时调用
此方法将在先前创建的加载器重置且其数据因此不可用时调用。 通过此回调，您可以了解何时将释放数据，因而能够及时移除其引用。

此实现调用值为 null 的swapCursor()：

```
// This is the Adapter being used to display the list's data.
SimpleCursorAdapter mAdapter;
...

public void onLoaderReset(Loader<Cursor> loader) {
    // This is called when the last Cursor provided to onLoadFinished()
    // above is about to be closed.  We need to make sure we are no
    // longer using it.
    mAdapter.swapCursor(null);
}
```


##### Loaders实例

参考Android Loaders api里面，以下是一个Fragment实例，它展示了一个ListView，其中包含针对联系人内容提供程序的查询结果，它使用CursorLoader管理提供程序的查询。

Android联系人其实很多地方用到了Loaders。

[完整LoaderDemo](https://github.com/Atinerlengs/AndroidDemo/tree/master/loaderdemo)

##### Loaders参考
可以参考[Android Loader机制全面详解及源码浅析](http://blog.csdn.net/axi295309066/article/details/52536960)

#### 返回栈
这一节主要将一下任务和返回栈，一个应用一般都包含多个Activity，遵循以下"后进先出"，如下图
![image](https://developer.android.google.cn/images/fundamentals/diagram_backstack.png)

下面我们介绍下Activtiy的四种启动模式:

在清单文件种声明Activity的启动模式，使用<activity>元素的launchMode属性去指定Activity应该如何与任务关联

- standard (默认模式)

    默认，系统在启动Activity的任务中创建Activity的新实例并向其传送Intent，Activity可以多次实例化，而每个实例均可属于不用任务，并且一个任务可以拥有多个实例。

- singleTop

    如果当前任务的顶部已存在 Activity 的一个实例，则系统会通过调用该实例的 onNewIntent() 方法向其传送 Intent，而不是创建 Activity 的新实例。Activity 可以多次实例化，而每个实例均可属于不同的任务，并且一个任务可以拥有多个实例（但前提是位于返回栈顶部的 Activity 并不是 Activity 的现有实例。

    例如，假设任务的返回栈包含根 Activity A 以及 Activity B、C 和位于顶部的 D（堆栈是 A-B-C-D；D 位于顶部）。收到针对 D 类 Activity 的 Intent。如果 D 具有默认的 "standard" 启动模式，则会启动该类的新实例，且堆栈会变成 A-B-C-D-D。但是，如果 D 的启动模式是 "singleTop"，则 D 的现有实例会通过 onNewIntent() 接收 Intent，因为它位于堆栈的顶部；而堆栈仍为 A-B-C-D。但是，如果收到针对 B 类 Activity 的 Intent，则会向堆栈添加 B 的新实例，即便其启动模式为 "singleTop" 也是如此。

    > 注：为某个 Activity创建新实例时，用户可以按“返回”按钮返回到前一个 Activity。 但是，当 Activity 的现有实例处理新 Intent 时，则在新 Intent 到达 onNewIntent() 之前，用户无法按“返回”按钮返回到 Activity 的状态。


- singleTask

    系统创建新任务并实例化位于新任务底部的 Activity。但是，如果该 Activity 的一个实例已存在于一个单独的任务中，则系统会通过调用现有实例的 onNewIntent() 方法向其传送 Intent，而不是创建新实例。一次只能存在 Activity 的一个实例。

    singleTask模式下，Task栈中只能有一个对应Activity的实例。例如：现在栈的结构为：A B C D。此时D通过Intent跳转到B，则栈的结构变成了：A B C D B

- singleInstance

    与 "singleTask" 相同，只是系统不会将任何其他 Activity 启动到包含实例的任务中。该 Activity 始终是其任务唯一仅有的成员；由此 Activity 启动的任何 Activity 均在单独的任务中打开

    singleInstance模式下，会将打开的Activity压入一个新建的任务栈中。例如：Task栈1中结构为：A B C ，C通过Intent跳转到了D（D的模式为singleInstance），那么则会新建一个Task 栈2，栈1中结构依旧为A B C，栈2中结构为D，此时屏幕中显示D，之后D通过Intent跳转到D，栈2中不会压入新的D，所以2个栈中的情况没发生改变。如果D跳转到了C，那么就会根据C对应的launchMode的在栈1中进行对应的操作，C如果为standard，那么D跳转到C，栈1的结构为A B C C ，此时点击返回按钮，还是在C，栈1的结构变为A B C，而不会回到D。

### Service

#### Service


Service是一个可以在后台执行长时间运行操作而不提供用户界面的应用组件。服务可以由其他应用组件启动，而且即使用户切换到其他应用，服务仍在后台继续运行。此外，组件可以绑定到服务以与之进行交互，甚至执行进程间通信。
服务基本分为两种形式：

- 启动服务

当应用组件（如 Activity）通过调用 startService() 启动服务时，服务即处于“启动”状态。一旦启动，服务即可在后台无限期运行，即使启动服务的组件已被销毁也不受影响。 已启动的服务通常是执行单一操作，而且不会将结果返回给调用方。例如，它可能通过网络下载或上传文件。 操作完成后，服务会自行停止运行。

- 绑定服务

当应用组件通过调用 bindService() 绑定到服务时，服务即处于“绑定”状态。绑定服务提供了一个客户端-服务器接口，允许组件与服务进行交互、发送请求、获取结果，甚至是利用进程间通信 (IPC) 跨进程执行这些操作。 仅当与另一个应用组件绑定时，绑定服务才会运行。 多个组件可以同时绑定到该服务，但全部取消绑定后，该服务即会被销毁。


>注意：服务在其托管进程的主线程中运行，它既不创建自己的线程，也不在单独的进程中运行（除非另行指定）。 这意味着，如果服务将执行任何 CPU 密集型工作或阻止性操作（例如 MP3 播放或联网），则应在服务内创建新线程来完成这项工作。通过使用单独的线程，可以降低发生“应用无响应”(ANR) 错误的风险，而应用的主线程仍可继续专注于运行用户与 Activity 之间的交互


##### 基础知识

创建Service子类(或者是一个现有的子类)，在Service里面需要写的回调方法有如下：

- onStartCommand()

    当另一个组件(如Activity)通过调用startService请求启动服务时，系统将会调用此方法，服务会启动并在后台无限期运行。如果实现此方法，则在服务工作完成后，需要通过调用stopSelf()或者stopService()方法来停止服务。

- onBind()

    当另一个组件想通过调用bindService()与服务绑定时，系统将调用此方法。在此方法的实现中，必须通过返回的IBinder提供一个接口，供客户端用来与服务进行通信。

- onCreate()

    首次创建服务时，系统将调用此方法来执行一次性设置程序(在调用onStartCommand 或 onBind()之后)如果服务已运行，则不会调用该方法。

- onDestory()

    当服务不在使用且将被销毁时，系统将调用此方法。服务应该实现此方法来清理所有资源，如线程、注册的监听器、接收器等等。这个是服务接受的最后一个调用。


清单文件中需要声明服务

```
<manifest ... >
  ...
  <application ... >
      <service android:name=".ExampleService" />
      ...
  </application>
</manifest>

```

>Caution: To ensure that your app is secure, always use an explicit intent when starting a Service and do not declare intent filters for your services. Using an implicit intent to start a service is a security hazard because you cannot be certain of the service that will respond to the intent, and the user cannot see which service starts. Beginning with Android 5.0 (API level 21), the system throws an exception if you call bindService() with an implicit intent.

##### Create StartService

通过startService启动，然后会调用onStartCommand()方法，启动之后可通过stopSelf()或者是stopService()方法去停止服务。

可以扩展两个类来创建服务:

- Service

    这是适用于所有服务的基类。扩展此类时，必须创建一个用于执行所有服务工作的新线程，因为默认情况下，服务将使用应用的主线程，这会降低应用正在运行的所有 Activity 的性能。

- IntentService

    这是 Service 的子类，它使用工作线程逐一处理所有启动请求。如果您不要求服务同时处理多个请求，这是最好的选择。 您只需实现 onHandleIntent() 方法即可，该方法会接收每个启动请求的 Intent，使您能够执行后台工作。

###### 继承IntentService

继承IntentService需要实现onHandleIntent(),还需要加一个构造函数,以下就是实例

```
public class PtService extends IntentService{
    public PtService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

###### 继承Service

正如上一部分中所述，使用 IntentService 显著简化了启动服务的实现。但是，若要求服务执行多线程（而不是通过工作队列处理启动请求），则可继承 Service 类来处理每个 Intent。

```
public class PlaysService extends Service{
    private static final String TAG = "PlaysService";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind: ");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
    }
}
```

```
注意：IntentService 和 Service的区别：

1. 首先IntentService是继承自Service;
2. Service不是一个单独的进程，它和应用程序在同一个进程中；
3. Service也不是一个线程，所以我们要避免在Service中进行耗时的操作；
4. IntentService使用队列的方式将请求的Intent加入队列，然后开启了一个Worker Thread（工作线程）在处理队列中的Intent,对于异步的startService请求，
IntentService会处理完成一个之后在处理第二个，每一个请求都会在一个单独的Worker Thread中处理，不会阻塞应用程序的主线程。
因此，如果我们如果要在Service里面处理一个耗时的操作，我们可以用IntentService来代替使用。
5. 使用IntentService 必须首先继承IntentService并实现onHandleIntent()方法，将耗时的任务放在这个方法执行，其他方面，IntentService和Service一样。

```

##### Create BindService

绑定服务允许应用组件通过bindService()与其绑定，以便创建长期连接(通常不允许组件通过调用startService()来启动)

如需与Activity和其他应用组件中的服务进行交互，或者需要通过进程间通信向其他应用公开某些应用功能，则应创建绑定服务。

创建绑定服务，必须实现onBind()回调方法以返回Ibinder，用于定义与服务通信的接口。然后，其他组件可以通过调用bindService()来检索该接口，并开始对服务调用方法。服务只用于与其绑定的应用组件，如果没有组件绑定到服务，则系统会销毁服务。

##### 管理服务生命周期

服务生命周期(从创建到销毁)可以遵循两条不同的路径：

- 启动服务

    该服务在其他组件调用startService时创建，然后无限期的运行，且必须通过stopSelf()来自行停止运行。此外，其他组件也可以通过调用stopService()来停止服务，服务停止后，系统会将其销毁

- 绑定服务

    该服务在另一个组件(客户端)调用bindService()时创建，然后客户端通过Ibinder接口与服务进行通信。客户端可以通过调用unbindservice()来关闭服务。多个客户端可以绑定到相同的客户顿，当所有绑定取消之后，系统即会销毁该服务。


这两条路劲并非完全独立，也就是说，你可以绑定到已经使用startService()启动的服务。 例如，可以通过使用intent(标示要播放的音乐)调用startService()来启动后台音乐服务，随后，可能在用户需要稍加控制播放器或获取有关当前播放歌曲的信息时，Activity可以通过调用bindService()绑定到服务。在这种情况下，除非所有客户端均取消绑定，否则stopService()或stopSelf()不实际停止服务。


###### 实现生命周期回调


```
public class ExampleService extends Service {

    //创建服务
    @Override
    public void onCreate() {
        super.onCreate();
    }

    //调用startService()方法回调onStartCommand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //客户端通过bindService()方法绑定到Service
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //客户端通过unbindService()解绑
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //客户端通过bindService()去再次绑定到Service
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    //销毁
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
```


> 注： 与Activity生命周期回调方法不同，不需要调用这些回调方法的超类实现。

![image](https://developer.android.google.cn/images/service_lifecycle.png)

左边图显示了使用startService()使用Service生命周期，右边使用了bindSercice来创建服务生命周期。

下面log是多次调用startservice和bindservice，serivce回调的方法

```
//第一次startService
12-27 10:47:22.404 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onCreate:
12-27 10:47:22.404 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStartCommand:
12-27 10:47:22.404 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStart:
//连续启动startService
12-27 10:48:02.810 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStart:
12-27 10:48:06.341 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStartCommand:
12-27 10:48:06.341 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStart:
12-27 10:48:06.728 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStartCommand:
12-27 10:48:06.728 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStart:
12-27 10:48:06.991 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStartCommand:
12-27 10:48:06.991 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onStart:
//stopservice
12-27 10:48:50.240 26624-26624/com.clinetservicedemo D/liqiang: UseStartService onDestroy:

//第一次bindservice
12-27 10:49:56.007 26624-26624/com.clinetservicedemo D/liqiang: UseBindService onCreate:
12-27 10:49:56.008 26624-26624/com.clinetservicedemo D/liqiang: UseBindService onBind:
//继续调用bindservice不会回调任何方法
```

#### Bound Services

BoundService服务是客户端-服务器接口中的服务器。BoundService可以让组件(例如Activity)绑定到服务、发送请求、接受响应，甚至执行进程通信(IPC)。绑定服务通常只在为其他应用组件服务时处于活动状态，不会无限期的在后台运行。


##### Create a Bound Service

创建提供绑定服务时候，必须提供Ibinder，用以提供客户端用来与服务进行交互的编程接口。可以通过以下三种方式去定义接口：

- 继承Binder类

    如果服务是供自由应用专用，并且在与客户端相同的进程中运行(常见情况)，则应通过继承Binder类并从onBind()返回他的一个实例来创建接口。客户端接受到Binder后，可利用它直接访问Binder实现以及Service里面的公共方法。

- 使用Messenger

如需要让接口跨进程工作，可以使用Messenger为服务创建接口。服务可以以这种方式定义对应于不同类型Message对象的Handler。此Handler时Messenger的基础，后者随后可与客户端分享一个IBinder，从而让客户端能利用Message对象向服务发送命令。此外，客户端还可以自定义由Messenger，以便服务回传消息。

这是执行进程通信的最简单方法，因为Messenger会在单一线程中创建包含所有请求的队列，这样就不必对服务进行线程安全设计

- 使用AIDL

AIDL（Android 接口定义语言）执行所有将对象分解成原语的工作，操作系统可以识别这些原语并将它们编组到各进程中，以执行 IPC。 之前采用 Messenger 的方法实际上是以 AIDL 作为其底层结构。 如上所述，Messenger 会在单一线程中创建包含所有客户端请求的队列，以便服务一次接收一个请求。 不过，如果您想让服务同时处理多个请求，则可直接使用 AIDL。 在此情况下，您的服务必须具备多线程处理能力，并采用线程安全式设计。
如需直接使用 AIDL，您必须创建一个定义编程接口的 .aidl 文件。Android SDK 工具利用该文件生成一个实现接口并处理 IPC 的抽象类，您随后可在服务内对其进行扩展。

>注：大多数应用“都不会”使用 AIDL 来创建绑定服务，因为它可能要求具备多线程处理能力，并可能导致实现的复杂性增加。因此，AIDL 并不适合大多数应用。


###### 继承Binder类

如果您的服务仅供本地应用使用，不需要跨进程工作，则可以实现自有 Binder 类，让您的客户端通过该类直接访问服务中的公共方法。

>注：此方法只有在客户端和服务位于同一应用和进程内这一最常见的情况下方才有效。 例如，对于需要将 Activity 绑定到在后台播放音乐的自有服务的音乐应用，此方法非常有效。

具体设置方法：

1.在您的服务中，创建一个可满足下列任一要求的Binder实例：

- 包含客户端可调用的公共方法
- 返回当前Service实例，其中包含客户端调用的公共方法
- 或返回由承载的其他类的实例，其中包含客户端调用的公共方法

2.从Onbind()回调分方法返回此Binder实例

3.在客户端中，从onServiceConnection()回调方法中接受Binder，并使用提供的方法调用绑定服务。

> 注：之所以要求服务和客户端必须在同一应用内，是为了便于客户端转换返回的对象和正确调用其 API。服务和客户端还必须在同一进程内，因为此方法不执行任何跨进程编组。

以下这个服务可让客户端通过 Binder 实现访问服务中的方法：

```
public class LocalService extends Service{
    //Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private final Random mRandom = new Random();

    public class LocalBinder extends Binder{
        public LocalService getService(){
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return this instance of LocalService so clients can call public methods
        return mBinder;
    }

    /** method for clients */
    public int getRandomNumber() {
        return mRandom.nextInt(100);
    }
}
```

LocalBinder 为客户端提供 getService() 方法，返回 LocalService 的当前实例。这样，客户端便可调用服务中的公共方法。 例如，客户端可调用服务中的 getRandomNumber()。

点击按钮时，以下这个 Activity 会绑定到 LocalService 并调用 getRandomNumber() ：

```
public class BindActivity extends AppCompatActivity{
    LocalService mService;
    boolean mBound;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bind_layout);
        Button getNumber = (Button) findViewById(R.id.btn_get);
        getNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBound) {
                    int num = mService.getRandomNumber();
                    Toast.makeText(BindActivity.this, "number: " + num, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this,LocalService.class);
        bindService(intent,mConntection, Context.BIND_AUTO_CREATE);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConntection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalService.LocalBinder binder = (LocalService.LocalBinder)iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBound = false;
        }
    };
}

```

###### 使用Messenger

以下是Messenger具体实现：

- 服务实现一个Handler,由其接受来自客户端的每个调用回调
- Handler用于创建Messenger对象(对Hnadler的引用)
- Messenger创建一个IBinder，服务通过onBind()使其返回客户端
- 客户端使用IBinder将Messenger(引用服务的Handler)实例化，然后使用后者将Message对象发送给服务
- 服务在其Handler中(具体的讲是在handleMessage()方法中)接受每个Message。

这样，客户端并没有调用服务的“方法”。而客户端传递的“消息”（Message 对象）是服务在其 Handler 中接收的。

下面是使用Messenger的简单服务实例：

服务端：

```
public class MessengerService extends Service {
    private static final String TAG = "liqiang";
    /** Command to the service to display a message */
    static final int MSG_SAY_HELLO = 1;

    class IncomingHandler extends Handler{
        @Override
        public void handleMessage(Message msgFromClient) {
            Message msgToClient = Message.obtain(msgFromClient);//返回给客户端的消息
            switch (msgFromClient.what){
                case MSG_SAY_HELLO:

                    msgToClient.what = MSG_SAY_HELLO;
                    try {
                        Thread.sleep(1000);
                        msgToClient.arg2 = msgFromClient.arg1 + msgFromClient.arg2;
                        msgFromClient.replyTo.send(msgToClient);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "handleMessage: say hello ~");
                    break;
                default:
                    super.handleMessage(msgFromClient);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: binding");
        return mMessenger.getBinder();
    }
}
```

服务端就一个Service，可以看到代码相当的简单，只需要去声明一个Messenger对象，然后onBind方法返回mMessenger.getBinder()；

然后坐等客户端将消息发送到handleMessage方法，根据message.what去判断进行什么操作，然后做对应的操作，最终将结果通过 msgfromClient.replyTo.send(msgToClient);返回。

可以看到我们这里主要是取出客户端传来的两个数字，然后求和返回，这里添加了sleep(1000)模拟耗时。

客户端：

```
public class MessengerActivity extends AppCompatActivity{
    private static final String TAG = "liqiang";
    /** Messenger for communicating with the service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: sucesseful");
            mService = new Messenger(service);
            mBound = true;
            mTv_connection_status.setText("Service Connected Successful.......");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: failed");
            mBound = false;
            mTv_connection_status.setText("Service Connected UnSuccessful..........");
        }
    };

    private Messenger mMessenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msgFromServer) {
            switch (msgFromServer.what){
                case 1:
                    TextView tv = (TextView) mLyContainer.findViewById(msgFromServer.arg1);
                    tv.setText(tv.getText() + " ==> " + msgFromServer.arg2);
                    break;
            }
            super.handleMessage(msgFromServer);
        }
    });

    private int mA;
    TextView mTv_connection_status;
    LinearLayout mLyContainer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messenger_layout);
        mTv_connection_status = (TextView) findViewById(R.id.tv_connect_status);
        mLyContainer = (LinearLayout) findViewById(R.id.ll_add);
    }

    public void sayHello(View view){
        Log.d(TAG, "mBound: " + mBound);
        if (!mBound) return;

        int a = mA++;
        int b = (int)(Math.random()*100);

        TextView tv = new TextView(MessengerActivity.this);
        tv.setText(a + " + " + b + " = caculating....");
        tv.setId(a);
        mLyContainer.addView(tv);

        // Create and send a message to the service, using a supported 'what' value
        Message msgFromClient = Message.obtain(null, 1, a, b);
        msgFromClient.replyTo = mMessenger;

        try {
            Log.d(TAG, "sayHello: send");
            mService.send(msgFromClient);
        }catch (RemoteException e) {
            Log.d(TAG, "sayHello: exception");
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        String packageName = "com.messengerserver";
        String serviceClassName = "com.messengerserver.MessengerService";
        // Bind to the service
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName,serviceClassName));
        Log.d(TAG, "onStart: intent : " + intent);
        bindService(intent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }
}
```

首先bindService，然后在onServiceConnected中拿到回调的service（IBinder）对象，通过service对象去构造一个mService =new Messenger(service);然后就可以使用mService.send(msg)给服务端了。

点击之后，服务端会收到消息，处理完成会将结果返回，传到Client端的mMessenger中的Handler的handleMessage方法中。

##### 绑定到服务
应用组件（客户端）可通过调用 bindService() 绑定到服务。Android 系统随后调用服务的 onBind() 方法，该方法返回用于与服务交互的 IBinder。

绑定是异步的。bindService() 会立即返回，“不会”使 IBinder 返回客户端。要接收 IBinder，客户端必须创建一个 ServiceConnection 实例，并将其传递给 bindService()。ServiceConnection 包括一个回调方法，系统通过调用它来传递 IBinder

应用组件(客户端)可以通过调用bindService（）绑定到服务。Android系统随后调用服务的onBind()方法，该方法返回用于与服务交互的IBinder。


>注：只有Activity、服务和内容提供可以绑定到服务， 无法从广播接收器绑定到服务。

因此，总结一下从客户端绑定到服务端所需步骤：

1.实现ServiceConnection：

    实现这个类然后重新两个回调方法，

    onServiceConnected()

    系统会调用该方法以传递服务的onBind()方法返回的IBinder。

    onServiceDisConnected()

    Android 系统会在与服务的连接意外中断时（例如当服务崩溃或被终止时）调用该方法。当客户端取消绑定时，系统“不会”调用该方法。

2.调用 bindService()，传递 ServiceConnection 实现。
3.当系统调用您的 onServiceConnected() 回调方法时，您可以使用接口定义的方法开始调用服务。
4.要断开与服务的连接，请调用 unbindService()。
如果应用在客户端仍绑定到服务时销毁客户端，则销毁会导致客户端取消绑定。 更好的做法是在客户端与服务交互完成后立即取消绑定客户端。 这样可以关闭空闲服务。

#### AIDL

AIDL（Android 接口定义语言）与您可能使用过的其他 IDL 类似。 您可以利用它定义客户端与服务使用进程间通信 (IPC) 进行相互通信时都认可的编程接口。 在 Android 上，一个进程通常无法访问另一个进程的内存。 尽管如此，进程需要将其对象分解成操作系统能够识别的原语，并将对象编组成跨越边界的对象。 编写执行这一编组操作的代码是一项繁琐的工作，因此 Android 会使用 AIDL 来处理。

>只有允许不同应用的客户端用 IPC 方式访问服务，并且想要在服务中处理多线程时，才有必要使用 AIDL。 如果您不需要执行跨越不同应用的并发 IPC，就应该通过实现一个 Binder 创建接口；或者，如果您想执行 IPC，但根本不需要处理多线程，则使用 Messenger 类来实现接口。无论如何，在实现 AIDL 之前，请您务必理解绑定服务。

#####  定义AIDL接口

使用ADIL创建绑定服务，需要执行以下步骤：

1.创建.aidl文件

此文件定义带有方法签名的编程接口。

2.实现接口

Android SDK 工具基于您的 .aidl 文件，使用 Java 编程语言生成一个接口。此接口具有一个名为 Stub 的内部抽象类，用于扩展 Binder 类并实现 AIDL 接口中的方法。您必须扩展 Stub 类并实现方法。

3.向客户端公开该接口

实现 Service 并重写 onBind() 以返回 Stub 类的实现。

>注意：在 AIDL 接口首次发布后对其进行的任何更改都必须保持向后兼容性，以避免中断其他应用对您的服务的使用。 也就是说，因为必须将您的 .aidl 文件复制到其他应用，才能让这些应用访问您的服务的接口，因此您必须保留对原始接口的支持


###### 创建.aidl文件

AIDL 使用简单语法，使您能通过可带参数和返回值的一个或多个方法来声明接口。 参数和返回值可以是任意类型，甚至可以是其他 AIDL 生成的接口。

您必须使用 Java 编程语言构建 .aidl 文件。每个 .aidl 文件都必须定义单个接口，并且只需包含接口声明和方法签名。

默认情况下，AIDL支持多种数据类型。以下是AIDL示例：

```
// IRemoteService.aidl
package com.example.android;

// Declare any non-default types here with import statements

/** Example service interface */
interface IRemoteService {
    /** Request the process ID of this service, to do evil things with it. */
    int getPid();

    /** Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
}

```

###### 实现接口

当您开发应用时，Android SDK 工具会生成一个以 .aidl 文件命名的 .java 接口文件。生成的接口包括一个名为 Stub 的子类，这个子类是其父接口（例如，YourInterface.Stub）的抽象实现，用于声明 .aidl 文件中的所有方法。

> Stub 还定义了几个帮助程序方法，其中最引人关注的是 asInterface()，该方法带 IBinder（通常便是传递给客户端 onServiceConnected() 回调方法的参数）并返回存根接口实例。 如需了解如何进行这种转换的更多详细信息，请参见调用 IPC 方法一节。

如需实现 .aidl 生成的接口，请扩展生成的 Binder 接口（例如，YourInterface.Stub）并实现从 .aidl 文件继承的方法。以下是一个使用匿名实例实现名为 IRemoteService 的接口（由以上 IRemoteService.aidl 示例定义）的示例：

```
private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
    public int getPid(){
        return Process.myPid();
    }
    public void basicTypes(int anInt, long aLong, boolean aBoolean,
        float aFloat, double aDouble, String aString) {
        // Does nothing
    }
};
```

现在，mBinder 是 Stub 类的一个实例（一个 Binder），用于定义服务的 RPC 接口。 在下一步中，将向客户端公开该实例，以便客户端能与服务进行交互。

在实现 AIDL 接口时应注意遵守以下这几个规则：

- 由于不能保证在主线程上执行传入调用，因此您一开始就需要做好多线程处理准备，并将您的服务正确地编译为线程安全服务。

- 默认情况下，RPC 调用是同步调用。如果您明知服务完成请求的时间不止几毫秒，就不应该从 Activity 的主线程调用服务，因为这样做可能会使应用挂起（Android 可能会显示“Application is Not Responding”对话框）— 您通常应该从客户端内的单独线程调用服务。

- 您引发的任何异常都不会回传给调用方。

###### 向客户端公开该接口

您为服务实现该接口后，就需要向客户端公开该接口，以便客户端进行绑定。 要为您的服务公开该接口，请扩展 Service 并实现 onBind()，以返回一个类实例，这个类实现了生成的 Stub（见前文所述）。以下是一个向客户端公开 IRemoteService 示例接口的服务示例。

```
public class RemoteService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the interface
        return mBinder;
    }

    private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
        public int getPid(){
            return Process.myPid();
        }
        public void basicTypes(int anInt, long aLong, boolean aBoolean,
            float aFloat, double aDouble, String aString) {
            // Does nothing
        }
    };
}
```

当客户端在 onServiceConnected() 回调中收到 IBinder 时，它必须调用 YourServiceInterface.Stub.asInterface(service) 以将返回的参数转换成 YourServiceInterface 类型。例如：

```
IRemoteService mIRemoteService;
private ServiceConnection mConnection = new ServiceConnection() {
    // Called when the connection with the service is established
    public void onServiceConnected(ComponentName className, IBinder service) {
        // Following the example above for an AIDL interface,
        // this gets an instance of the IRemoteInterface, which we can use to call on the service
        mIRemoteService = IRemoteService.Stub.asInterface(service);
    }

    // Called when the connection with the service disconnects unexpectedly
    public void onServiceDisconnected(ComponentName className) {
        Log.e(TAG, "Service has unexpectedly disconnected");
        mIRemoteService = null;
    }
};
```

aidl的demo就不一一去写了，后续会把demo上传



### ContentProvider

内容提供程序管理对结构化数据集的访问。它们封装数据，并提供用于定义数据安全性的机制。 内容提供程序是连接一个进程中的数据与另一个进程中运行的代码的标准界面。

如果您想要访问内容提供程序中的数据，可以将应用的 Context 中的 ContentResolver 对象用作客户端来与提供程序通信。 ContentResolver 对象会与提供程序对象（即实现 ContentProvider 的类实例）通信。 提供程序对象从客户端接收数据请求，执行请求的操作并返回结果。

如果您不打算与其他应用共享数据，则无需开发自己的提供程序。 不过，您需要通过自己的提供程序在您自己的应用中提供自定义搜索建议。 如果您想将复杂的数据或文件从您的应用复制并粘贴到其他应用中，也需要创建您自己的提供程序。

Android 本身包括的内容提供程序可管理音频、视频、图像和个人联系信息等数据。 android.provider 软件包参考文档中列出了部分提供程序。 任何 Android 应用都可以访问这些提供程序，但会受到某些限制。


#### ContentProvider基础知识

内容提供程序管理对中央数据存储区的访问。提供程序是 Android 应用的一部分，通常提供自己的 UI 来使用数据。 但是，内容提供程序主要旨在供其他应用使用，这些应用使用提供程序客户端对象来访问提供程序。 提供程序与提供程序客户端共同提供一致的标准数据界面，该界面还可处理跨进程通信并保护数据访问的安全性。


##### 概览

内容提供程序以一个或多个表（与在关系型数据库中找到的表类似）的形式将数据呈现给外部应用。 行表示提供程序收集的某种数据类型的实例，行中的每个列表示为实例收集的每条数据。

##### 访问Provider

应用从具有 ContentResolver 客户端对象的内容提供程序访问数据。 此对象具有调用提供程序对象（ContentProvider 的某个具体子类的实例）中同名方法的方法。 ContentResolver 方法可提供持续存储的基本“CRUD”（创建、检索、更新和删除）功能。

客户端应用进程中的 ContentResolver 对象和拥有提供程序的应用中的 ContentProvider 对象可自动处理跨进程通信。 ContentProvider 还可充当其数据存储区和表格形式的数据外部显示之间的抽象层。

>注：要访问提供程序，您的应用通常需要在其清单文件中请求特定权限。 内容提供程序权限部分详细介绍了此内容。

要从用户字典提供程序中获取字词及其语言区域的列表

要从ContentProvider程序中获取字词以及其列表，需要调用ContentResolver.query()方法，具体调用如下：

```
// Queries the user dictionary and returns results
mCursor = getContentResolver().query(
    UserDictionary.Words.CONTENT_URI,   // The content URI of the words table
    mProjection,                        // The columns to return for each row
    mSelectionClause                    // Selection criteria
    mSelectionArgs,                     // Selection criteria
    mSortOrder);                        // The sort order for the returned rows
```

###### 内容URI

内容 URI 是用于在提供程序中标识数据的 URI。内容 URI 包括整个提供程序的符号名称（其授权）和一个指向表的名称（路径）。 当您调用客户端方法来访问提供程序中的表时，该表的内容 URI 将是其参数之一。

在前面的代码行中，常量 CONTENT_URI 包含用户字典的“字词”表的内容 URI。 ContentResolver 对象会分析出 URI 的授权，并通过将该授权与已知提供程序的系统表进行比较，来“解析”提供程序。 然后， ContentResolver 可以将查询参数分派给正确的提供程序。

ContentProvider 使用内容 URI 的路径部分来选择要访问的表。 提供程序通常会为其公开的每个表显示一条路径。

在前面的代码行中，“字词”表的完整 URI 是：

```
content://user_dictionary/words
```

user_dictionary 字符串是提供程序的授权, words 字符串是表的路径。 字符串 content://（架构）始终显示，并将此标识为内容 URI。

许多提供程序都允许您通过将 ID 值追加到 URI 末尾来访问表中的单个行。 例如，要从用户字典中检索 _ID 为 4 的行，则可使用此内容 URI：

```
Uri singleUri = ContentUris.withAppendedId(UserDictionary.Words.CONTENT_URI,4);
```

在检索到一组行后想要更新或删除其中某一行时通常会用到 ID 值。

>注：Uri 和 Uri.Builder 类包含根据字符串构建格式规范的 URI 对象的便利方法。 ContentUris 包含一些可以将 ID 值轻松追加到 URI 后的方法。 前面的代码段就是使用 withAppendedId() 将 ID 追加到 UserDictionary 内容 URI 后。

##### 从Provider里面检索数据

要从提供程序中检索数据，请按照以下基本步骤执行操作：

1.请求对提供程序的读取访问权限。

2.定义将查询发送至提供程序的代码。


###### 请求读取访问权限

在AndroidManifest.xml；里面添加相应的权限

###### 构建查询

从提供程序中检索数据的下一步是构建查询。第一个代码段定义某些用于访问用户字典提供程序的变量：

```
//
A "projection" defines the columns that will be returned for each row
String[] mProjection =
{
    UserDictionary.Words._ID,    // Contract class constant for the _ID column name
    UserDictionary.Words.WORD,   // Contract class constant for the word column name
    UserDictionary.Words.LOCALE  // Contract class constant for the locale column name
};

// Defines a string to contain the selection clause
String mSelectionClause = null;

// Initializes an array to contain selection arguments
String[] mSelectionArgs = {""};
```

查询应该返回的列集被称为投影（变量 mProjection）。

在下一个代码段中，如果用户未输入字词，则选择子句将设置为 null，而且查询会返回提供程序中的所有字词。 如果用户输入了字词，选择子句将设置为 UserDictionary.Words.WORD + " = ?" 且选择参数数组的第一个元素将设置为用户输入的字词。

```
/*
 * This defines a one-element String array to contain the selection argument.
 */
String[] mSelectionArgs = {""};

// Gets a word from the UI
mSearchString = mSearchWord.getText().toString();

// Remember to insert code here to check for invalid or malicious input.

// If the word is the empty string, gets everything
if (TextUtils.isEmpty(mSearchString)) {
    // Setting the selection clause to null will return all words
    mSelectionClause = null;
    mSelectionArgs[0] = "";

} else {
    // Constructs a selection clause that matches the word that the user entered.
    mSelectionClause = UserDictionary.Words.WORD + " = ?";

    // Moves the user's input string to the selection arguments.
    mSelectionArgs[0] = mSearchString;

}

// Does a query against the table and returns a Cursor object
mCursor = getContentResolver().query(
    UserDictionary.Words.CONTENT_URI,  // The content URI of the words table
    mProjection,                       // The columns to return for each row
    mSelectionClause                   // Either null, or the word the user entered
    mSelectionArgs,                    // Either empty, or the string the user entered
    mSortOrder);                       // The sort order for the returned rows

// Some providers return null if an error occurs, others throw an exception
if (null == mCursor) {
    /*
     * Insert code here to handle the error. Be sure not to use the cursor! You may want to
     * call android.util.Log.e() to log this error.
     *
     */
// If the Cursor is empty, the provider found no matches
} else if (mCursor.getCount() < 1) {

    /*
     * Insert code here to notify the user that the search was unsuccessful. This isn't necessarily
     * an error. You may want to offer the user the option to insert a new row, or re-type the
     * search term.
     */

} else {
    // Insert code here to do something with the results

}
```

此查询与 SQL 语句相似：

```
SELECT _ID, word, locale FROM words WHERE word = <userinput> ORDER BY word ASC;

```

###### 显示查询结果

ContentResolver.query() 客户端方法始终会返回符合以下条件的 Cursor：包含查询的投影为匹配查询选择条件的行指定的列。 Cursor 对象为其包含的行和列提供随机读取访问权限。 通过使用 Cursor 方法，您可以循环访问结果中的行、确定每个列的数据类型、从列中获取数据，并检查结果的其他属性。 某些 Cursor 实现会在提供程序的数据发生更改时自动更新对象和/或在 Cursor 更改时触发观察程序对象中的方法。

>注：提供程序可能会根据发出查询的对象的性质来限制对列的访问。 例如，联系人提供程序会限定只有同步适配器才能访问某些列，因此不会将它们返回至 Activity 或服务。

如果没有与选择条件匹配的行，则提供程序会返回 Cursor.getCount() 为 0（空游标）的 Cursor 对象。

如果出现内部错误，查询结果将取决于具体的提供程序。它可能会选择返回 null，或引发 Exception。

由于 Cursor 是行“列表”，因此显示 Cursor 内容的良好方式是通过 SimpleCursorAdapter 将其与 ListView 关联。

以下代码段将延续上一代码段的代码。它会创建一个包含由查询检索到的 Cursor 的 SimpleCursorAdapter 对象，并将此对象设置为 ListView 的适配器：

```
// Defines a list of columns to retrieve from the Cursor and load into an output row
String[] mWordListColumns =
{
    UserDictionary.Words.WORD,   // Contract class constant containing the word column name
    UserDictionary.Words.LOCALE  // Contract class constant containing the locale column name
};

// Defines a list of View IDs that will receive the Cursor columns for each row
int[] mWordListItems = { R.id.dictWord, R.id.locale};

// Creates a new SimpleCursorAdapter
mCursorAdapter = new SimpleCursorAdapter(
    getApplicationContext(),               // The application's Context object
    R.layout.wordlistrow,                  // A layout in XML for one row in the ListView
    mCursor,                               // The result from the query
    mWordListColumns,                      // A string array of column names in the cursor
    mWordListItems,                        // An integer array of view IDs in the row layout
    0);                                    // Flags (usually none are needed)

// Sets the adapter for the ListView
mWordList.setAdapter(mCursorAdapter);
```

>注：要通过 Cursor 支持 ListView，游标必需包含名为 _ID 的列。 正因如此，前文显示的查询会为“字词”表检索 _ID 列，即使 ListView 未显示该列。 此限制也解释了为什么大多数提供程序的每个表都具有 _ID 列。

###### 从查询结果中获取数据

您可以将查询结果用于其他任务，而不是仅显示它们。例如，您可以从用户字典中检索拼写，然后在其他提供程序中查找它们。 要执行此操作，您需要在 Cursor 中循环访问行：

```
// Determine the column index of the column named "word"
int index = mCursor.getColumnIndex(UserDictionary.Words.WORD);

/*
 * Only executes if the cursor is valid. The User Dictionary Provider returns null if
 * an internal error occurs. Other providers may throw an Exception instead of returning null.
 */

if (mCursor != null) {
    /*
     * Moves to the next row in the cursor. Before the first movement in the cursor, the
     * "row pointer" is -1, and if you try to retrieve data at that position you will get an
     * exception.
     */
    while (mCursor.moveToNext()) {

        // Gets the value from the column.
        newWord = mCursor.getString(index);

        // Insert code here to process the retrieved word.

        ...

        // end of while loop
    }
} else {

    // Insert code here to report an error if the cursor is null or the provider threw an exception.
}
```

##### 内容提供程序权限

提供程序的应用可以指定其他应用访问提供程序的数据所必需的权限。 这些权限可确保用户了解应用将尝试访问的数据。 根据提供程序的要求，其他应用会请求它们访问提供程序所需的权限。 最终用户会在安装应用时看到所请求的权限。

权限写法如下

```
    <uses-permission android:name="android.permission.READ_USER_DICTIONARY">
```

##### 插入、更新、删除数据

与从提供程序检索数据的方式相同，也可以通过提供程序客户端和提供程序 ContentProvider 之间的交互来修改数据。 您通过传递到 ContentProvider 的对应方法的参数来调用 ContentResolver 方法。 提供程序和提供程序客户端会自动处理安全性和跨进程通信。

###### 插入数据

要将数据插入提供程序，可调用 ContentResolver.insert() 方法。此方法会在提供程序中插入新行并为该行返回内容 URI。 此代码段显示如何将新字词插入用户字典提供程序：

```
// Defines a new Uri object that receives the result of the insertion
Uri mNewUri;
...

// Defines an object to contain the new values to insert
ContentValues mNewValues = new ContentValues();

/*
 * Sets the values of each column and inserts the word. The arguments to the "put"
 * method are "column name" and "value"
 */
mNewValues.put(UserDictionary.Words.APP_ID, "example.user");
mNewValues.put(UserDictionary.Words.LOCALE, "en_US");
mNewValues.put(UserDictionary.Words.WORD, "insert");
mNewValues.put(UserDictionary.Words.FREQUENCY, "100");

mNewUri = getContentResolver().insert(
    UserDictionary.Word.CONTENT_URI,   // the user dictionary content URI
    mNewValues                          // the values to insert
);
```

新行的数据会进入单个 ContentValues 对象中，该对象在形式上与单行游标类似。 此对象中的列不需要具有相同的数据类型，如果您不想指定值，则可以使用 ContentValues.putNull() 将列设置为 null。

###### 更新数据

要更新行，请按照执行插入的方式使用具有更新值的 ContentValues 对象，并按照执行查询的方式使用选择条件。 您使用的客户端方法是 ContentResolver.update()。您只需将值添加至您要更新的列的 ContentValues 对象。 如果您要清除列的内容，请将值设置为 null。

以下代码段会将语言区域具有语言“en”的所有行的语言区域更改为 null。 返回值是已更新的行数

```
// Defines an object to contain the updated values
ContentValues mUpdateValues = new ContentValues();

// Defines selection criteria for the rows you want to update
String mSelectionClause = UserDictionary.Words.LOCALE +  "LIKE ?";
String[] mSelectionArgs = {"en_%"};

// Defines a variable to contain the number of updated rows
int mRowsUpdated = 0;

...

/*
 * Sets the updated value and updates the selected words.
 */
mUpdateValues.putNull(UserDictionary.Words.LOCALE);

mRowsUpdated = getContentResolver().update(
    UserDictionary.Words.CONTENT_URI,   // the user dictionary content URI
    mUpdateValues                       // the columns to update
    mSelectionClause                    // the column to select on
    mSelectionArgs                      // the value to compare to
);
```

######　删除数据

删除行与检索行数据类似：为要删除的行指定选择条件，客户端方法会返回已删除的行数。 以下代码段会删除应用 ID 与“用户”匹配的行。该方法会返回已删除的行数。

```

// Defines selection criteria for the rows you want to delete
String mSelectionClause = UserDictionary.Words.APP_ID + " LIKE ?";
String[] mSelectionArgs = {"user"};

// Defines a variable to contain the number of rows deleted
int mRowsDeleted = 0;

...

// Deletes the words that match the selection criteria
mRowsDeleted = getContentResolver().delete(
    UserDictionary.Words.CONTENT_URI,   // the user dictionary content URI
    mSelectionClause                    // the column to select on
    mSelectionArgs                      // the value to compare to
);
```

##### Provider数据类型

内容提供程序可以提供多种不同的数据类型。用户字典提供程序仅提供文本，但提供程序也能提供以下格式：

- 整型
- 长整型（长）
- 浮点型
- 长浮点型（双倍）

######　提供程序访问的替代形式

提供程序访问的三种替代形式在应用开发过程中十分重要：

- 批量访问：您可以通过 ContentProviderOperation 类中的方法创建一批访问调用，然后通过 ContentResolver.applyBatch() 应用它们。
- 异步查询：您应该在单独线程中执行查询。执行此操作的方式之一是使用 CursorLoader 对象。 加载器指南中的示例展示了如何执行此操作。
- 通过 Intent 访问数据：尽管您无法直接向提供程序发送 Intent，但可以向提供程序的应用发送 Intent，后者通常具有修改提供程序数据的最佳配置。




#### 创建ContentProvider程序

内容提供程序管理对中央数据存储区的访问。您将提供程序作为 Android 应用中的一个或多个类（连同清单文件中的元素）实现。 其中一个类会实现子类 ContentProvider，即您的提供程序与其他应用之间的接口。 尽管内容提供程序旨在向其他应用提供数据，但您的应用中必定有这样一些 Activity，它们允许用户查询和修改由提供程序管理的数据。

##### 设计数据存储

内容提供程序是用于访问以结构化格式保存的数据的接口。在您创建该接口之前，必须决定如何存储数据。 您可以按自己的喜好以任何形式存储数据，然后根据需要设计读写数据的接口。

具体几种存储方法可以去查看[数据存储](https://developer.android.google.cn/guide/topics/data/index.html) 


##### 设计内容 URI

内容 URI 是用于在提供程序中标识数据的 URI。内容 URI 包括整个提供程序的符号名称（其授权）和一个指向表或文件的名称（路径）。 可选 ID 部分指向表中的单个行。 ContentProvider 的每一个数据访问方法都将内容 URI 作为参数；您可以利用这一点确定要访问的表、行或文件。



#### 联系人Provider & 日历Provider

参考 [联系人](https://developer.android.google.cn/guide/topics/providers/contacts-provider.html#RawContactBasics) & [日历](https://developer.android.google.cn/guide/topics/providers/calendar-provider.html)

#### 存储访问框架
Android 4.4（API 级别 19）引入了存储访问框架 (SAF)。SAF 让用户能够在其所有首选文档存储提供程序中方便地浏览并打开文档、图像以及其他文件。 用户可以通过易用的标准 UI，以统一方式在所有应用和提供程序中浏览文件和访问最近使用的文件。

[存储访问框架详情参考](https://developer.android.google.cn/guide/topics/providers/document-provider.html)


### BroadCasts
Android应用程序可以发送或接收来自Android系统和其他Android应用程序的广播消息，类似于 发布 - 订阅 设计模式。这些广播是在感兴趣的事件发生时发送的。例如，Android系统在发生各种系统事件时发送广播，例如当系统启动或设备开始充电时。应用程序也可以发送自定义广播，例如，通知其他应用程序他们可能感兴趣的东西（例如，一些新的数据已被下载）。

应用程序可以注册以接收特定的广播。当广播被发送时，系统自动将广播路由到已经预订接收该特定类型的广播的应用。

一般来说，广播可以用作应用程序之间和正常用户流程之外的消息传递系统。

#### 广播分类：

##### (1)按照发送的方式分类

- 标准广播

    标准广播是一种异步的方式来进行传播的，广播发出去之后，所有的广播接收者几乎是同一时间收到消息的。他们之间没有先后顺序可言，而且这种广播是没法被截断的。

- 有序广播

    是一种同步执行的广播，在广播发出去之后，同一时刻只有一个广播接收器可以收到消息。当广播中的逻辑执行完成后，广播才会继续传播。

##### (2)按照注册的方式分类

- 动态注册广播

    顾名思义，就是在代码中注册的。

- 静态注册广播

    动态注册要求程序必须在运行时才能进行，有一定的局限性，如果我们需要在程序还没启动的时候就可以接收到注册的广播，就需要静态注册了。主要是在AndroidManifest中进行注册。

##### (3)按照定义的方式分类

- 系统广播

    Android系统中内置了多个系统广播，每个系统广播都具有特定的intent-filter，其中主要包括具体的action，系统广播发出后，将被相应的BroadcastReceiver接收。系统广播在系统内部当特定事件发生时，由系统自动发出。

- 自定义广播

    由应用程序开发者自己定义的广播

#### 动态注册广播的实现

##### (1) 实现一个广播接收器

```
public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "received in MyBroadcastReceiver", Toast.LENGTH_SHORT).show();
        abortBroadcast();
    }
}
```

主要就是继承一个BroadcastReceiver，实现onReceive方法，在其中实现自己的业务逻辑就可以了。

##### (2)注册广播

```
public class MainActivity extends AppCompatActivity {

    private IntentFilter intentFilter;
    private MyBroadcastReceiver myBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        myBroadcastReceiver = new MyBroadcastReceiver();
        registerReceiver(myBroadcastReceiver, intentFilter);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
                sendBroadcast(intent); // 发送广播
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }
}
```

这样MyBroadcastReceiver就可以收到相应的广播消息了。

#### 静态注册广播的实现
还是用上面的按个MyBroadcastReceiver，只不过这次采用静态注册的方式
在manifest文件中增加如下的代码：

```
<receiver
    android:name=".MyBroadcastReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
    </intent-filter>
</receiver>
```

相关解释：

- android:exported

此BroadcastReceiver能否接收其他App发出的广播(其默认值是由receiver中有无intent-filter决定的，如果有intent-filter，默认值为true，否则为false);

- android:name

此broadcastReceiver类名;

- android:permission

如果设置，具有相应权限的广播发送方发送的广播才能被此broadcastReceiver所接收;

- android:process

broadcastReceiver运行所处的进程。默认为App的进程。可以指定独立的进程(Android四大组件都可以通过此属性指定自己的独立进程);


#### 静态注册广播与动态注册广播的区别

- 生存期，静态广播的生存期可以比动态广播的长很多，因为静态广播很多都是用来对系统时间进行监听，比如我们可以监听手机开机。而动态广播会随着context的终止而终止

- 优先级动态广播的优先级比静态广播高

- 动态广播无需在AndroidManifest.xml中声明即可直接使用，也即动态；而静态广播则需要，有时候还要在AndroidManifest.xml中加上一些权限的声明

#### 有序广播

有序广播是异步方式传播的。指的是发送出去的广播被BroadcastReceiver按照先后循序接收。有序广播的定义过程与普通广播无异，只是其的主要发送方式变为：

```
/**
     * Broadcast the given intent to all interested BroadcastReceivers, delivering
     * them one at a time to allow more preferred receivers to consume the
     * broadcast before it is delivered to less preferred receivers.  This
     * call is asynchronous; it returns immediately, and you will continue
     * executing while the receivers are run.
     * @param intent The Intent to broadcast; all receivers matching this
     *               Intent will receive the broadcast.
     * @param receiverPermission (optional) String naming a permissions that
     *               a receiver must hold in order to receive your broadcast.
     *               If null, no permission is required.
     */
    public abstract void sendOrderedBroadcast(Intent intent,
            @Nullable String receiverPermission);
```

有序广播的主要特点：

- 同级别接收是随机的(结合下一条)
- 同级别动态注册（代码中注册）高于静态注册（AndroidManifest中注册）
- 排序规则为：将当前系统中所有有效的动态注册和静态注册的BroadcastReceiver按照priority属性值从大到小排序
- 先接收的BroadcastReceiver可以对此有序广播进行截断，使后面的BroadcastReceiver不再接收到此广播，也可以对广播进行修改，使后面的BroadcastReceiver接收到广播后解析得到错误的参数值。当然，一般情况下，不建议对有序广播进行此类操作，尤其是针对系统中的有序广播。实现截断的代码为：

```
abortBroadcast();
```

#### 标准广播
标准广播的主要特点：

- 同级别接收先后是随机的（无序的）
- 级别低的后接收到广播
- 接收器不能截断广播的继续传播，也不能处理广播
- 同级别动态注册（代码中注册）高于静态注册（AndroidManifest中注册）


#### 广播的安全性问题
Android中的广播可以跨进程甚至跨App直接通信，且exported属性在有intent-filter的情况下默认值是true，由此将可能出现的安全隐患如下：

- 其他App可能会针对性的发出与当前App intent-filter相匹配的广播，由此导致当前App不断接收到广播并处理；

- 其他App可以注册与当前App一致的intent-filter用于接收广播，获取广播具体信息。

增加安全性的方案包括：

- 对于同一App内部发送和接收广播，将exported属性人为设置成false，使得非本App内部发出的此广播不被接收；

- 在广播发送和接收时，都增加上相应的permission，用于权限验证；

- 发送广播时，指定特定广播接收器所在的包名，具体是通过intent.setPackage(packageName)指定，这样此广播将只会发送到此包中的App内与之相匹配的有效广播接收器中。

- 采用LocalBroadcastManager的方式

#### 本地广播LocalBroadcastManager

##### (1) LoacalroadcastMananger 概念

为了解决安全性问题，Android在android.support.v4.content包中引入了LocalBroadcastManager。按照官方文档的描述,使用LocalBroadcastManager有如下的优势：

```
Helper to register for and send broadcasts of Intents to local objects within your process. This has a number of advantages over sending global broadcasts with sendBroadcast(Intent):
(1)You know that the data you are broadcasting won't leave your app, so don't need to worry about leaking private data.
(2)It is not possible for other applications to send these broadcasts to your app, so you don't need to worry about having security holes they can exploit.
(3)It is more efficient than sending a global broadcast through the system.
```

也就是说，使用该机制发出的广播只能够在应用程序内部进行传递，并且广播接收器也只能接收来自本地应用程序发出的广播，这样所有的安全性问题都不存在了。

##### (2)LocalBroadcastManager使用范例

```
public class MainActivity extends AppCompatActivity {
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        localBroadcastManager = LocalBroadcastManager.getInstance(this); // 获取实例
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.example.broadcasttest.LOCAL_BROADCAST");
                localBroadcastManager.sendBroadcast(intent); // 发送本地广播
            }
        });
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.broadcasttest.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter); // 注册本地广播监听器
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(localReceiver);
    }

    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "received local broadcast", Toast.LENGTH_SHORT).show();
        }
    }
}
```

[LocalBroadcastManager官方文档](https://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager.html)


##  应用清单

每个应用的根目录中都必须包含一个 AndroidManifest.xml 文件（且文件名精确无误）。 清单文件向 Android 系统提供应用的必要信息，系统必须具有这些信息方可运行应用的任何代码。

清单文件结构

```
<?xml version="1.0" encoding="utf-8"?>

<manifest>

    <uses-permission android:name="android.permission.READ_CONTACTS"/>

    <permission android:name="com.example.project.DEBIT_ACCT" . . . />

    <permission-tree />
    <permission-group />


    <instrumentation />

    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="25" />

    <!-- 指明应用程序的软硬件需求 -->
    <uses-configuration />

    <!-- 声明应用使用的单一硬件或软件功能 -->
    <uses-feature android:name="android.hardware.bluetooth" />
    <uses-feature android:name="android.hardware.camera" />

    <!-- 定义应用程序支持的屏幕尺寸，并针对更大的屏幕启用 屏幕兼容模式 -->
    <supports-screens />
    <!-- 指定应用程序所兼容的屏幕参数 -->
    <compatible-screens />
    <!-- 声明本应用程序支持的一种 GL 纹理压缩格式 -->
    <supports-gl-texture />

    <application
        android:name="com.android.contacts.ContactsApplication"
        android:label="@string/applicationLabel"
        android:icon="@mipmap/ic_contacts_launcher"
        android:roundIcon="@mipmap/ic_contacts_launcher"
        android:hardwareAccelerated="true"
        android:supportsRtl="true"
    >

        <activity
            android:name=".activities.ContactEditorActivity"
            android:configChanges="mcc|mnc|orientation|screenSize"
            android:label="@string/editContactActivityLabel"
            android:theme="@style/EditorActivityTheme"
            android:resizeableActivity="true">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <intent-filter android:label="@string/editContactDescription">
                <action android:name="android.intent.action.EDIT" />
                <category android:name="android.intent.category.DEFAULT" />

                <data android:mimeType="vnd.android.cursor.item/person" />
                <data android:mimeType="vnd.android.cursor.item/contact" />
                <data android:mimeType="vnd.android.cursor.item/raw_contact" />
            </intent-filter>

            <meta-data android:name="android.app.searchable"
                android:resource="@xml/searchable"
            />
        </activity>


        <service android:name=".ViewNotificationService"
                 android:permission="android.permission.WRITE_CONTACTS"
                 android:exported="true">
            <intent-filter>
                <action android:name="com.android.contacts.VIEW_NOTIFICATION"/>
                <data android:mimeType="vnd.android.cursor.item/contact"/>
            </intent-filter>
        </service>

        <receiver android:name="LocaleChangeReceiver">
            <intent-filter>
                <action android:name="android.intent.action.LOCALE_CHANGED"/>
            </intent-filter>
        </receiver>

        <provider
            android:name="android.support.v4.content.FileProvider"
            android:authorities="@string/contacts_file_provider_authority"
            android:grantUriPermissions="true"
            android:exported="false">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
```

## 支持多种屏幕
Android 可在各种具有不同屏幕尺寸和密度的设备上运行。对于应用，Android 系统在不同设备中提供一致的开发环境，可以处理大多数工作，将每个应用的用户界面调整为适应其显示的屏幕。 同时，系统提供 API，可用于控制应用适用于特定屏幕尺寸和密度的 UI，以针对不同屏幕配置优化 UI 设计。 例如，您可能想要不同于手机 UI 的平板电脑 UI。

虽然系统为使您的应用适用于不同的屏幕，会进行缩放和大小调整，但您应针对不同的屏幕尺寸和密度优化应用。 这样可以最大程度优化所有设备上的用户体验，用户会 认为您的应用实际上是专为他们的设备而设计，而不是 简单地拉伸以适应其设备屏幕。

### 重要概念：

- 什么是屏幕尺寸、屏幕分辨率、屏幕像素密度
- 什么事dp、dip、dpi、sp、px ？ 他们之间关系是什么
- 什么是mdpi、hdpi、xdpi、xxdpi？如何计算和区分？

#### 屏幕尺寸
屏幕尺寸指屏幕的对角线的长度，单位是英寸，1英寸=2.54厘米

比如常见的屏幕尺寸有2.4、2.8、3.5、3.7、4.2、5.0、5.5、6.0等

#### 屏幕分辨率

屏幕分辨率是指在横纵向上的像素点数，单位是px，1px=1个像素点。一般以纵向像素*横向像素，如1960 *1080。

#### 屏幕像素密度

屏幕像素密度是指每英寸上的像素点数，单位是dpi，即“dot per inch”的缩写。屏幕像素密度与屏幕尺寸和屏幕分辨率有关，在单一变化条件下，屏幕尺寸越小、分辨率越高，像素密度越大，反之越小。

#### dp、dip、dpi、sp、px

px我们应该是比较熟悉的，前面的分辨率就是用的像素为单位，大多数情况下，比如UI设计、Android原生API都会以px作为统一的计量单位，像是获取屏幕宽高等。

dip和dp是一个意思，都是Density Independent Pixels的缩写，即密度无关像素，上面我们说过，dpi是屏幕像素密度，假如一英寸里面有160个像素，这个屏幕的像素密度就是160dpi，那么在这种情况下，dp和px如何换算呢？在Android中，规定以160dpi为基准，1dip=1px，如果密度是320dpi，则1dip=2px，以此类推。

假如同样都是画一条320px的线，在480*800分辨率手机上显示为2/3屏幕宽度，在320 *480的手机上则占满了全屏，如果使用dp为单位，在这两种分辨率下，160dp都显示为屏幕一半的长度。这也是为什么在Android开发中，写布局的时候要尽量使用dp而不是px的原因。

而sp，即scale-independent pixels，与dp类似，但是可以根据文字大小首选项进行放缩，是设置字体大小的御用单位。

mdpi、hdpi、xdpi、xxdpi

其实之前还有个ldpi，但是随着移动设备配置的不断升级，这个像素密度的设备已经很罕见了，所在现在适配时不需考虑。


mdpi、hdpi、xdpi、xxdpi用来修饰Android中的drawable文件夹及values文件夹，用来区分不同像素密度下的图片和dimen值。

### 支持的屏幕范围

Android 支持多种屏幕尺寸和密度，反映设备可能具有的多种不同屏幕配置。 您可以使用 Android 系统的功能优化应用在各种屏幕配置下的用户界面 ，确保应用不仅正常渲染，而且在每个屏幕上提供 最佳的用户体验。

为简化您为多种屏幕设计用户界面的方式，Android 将实际屏幕尺寸和密度的范围 分为：

- 四种通用尺寸：小、正常、 大 和超大

>注：从 Android 3.2（API 级别 13）开始，这些尺寸组 已弃用，而采用根据可用屏幕宽度管理屏幕尺寸的 新技术。如果为 Android 3.2 和更高版本开发，请参阅声明适用于 Android 3.2 的平板电脑布局以了解更多信息。

- 六种通用的密度：

```
ldpi（低）~120dpi
mdpi（中）~160dpi
hdpi（高）~240dpi
xhdpi（超高）~320dpi
xxhdpi（超超高）~480dpi
xxxhdpi（超超超高）~640dpi
```

通用的尺寸和密度按照基线配置（即正常尺寸和 mdpi（中）密度）排列。 此基线基于第一代 Android 设备 (T-Mobile G1) 的屏幕配置，该设备采用 HVGA 屏幕（在 Android 1.6 之前，这是 Android 支持的唯一屏幕配置）。

每种通用的尺寸和密度都涵盖一个实际屏幕尺寸和密度范围。例如， 两部都报告正常屏幕尺寸的设备在手动测量时，实际屏幕尺寸和 高宽比可能略有不同。类似地，对于两台报告 hdpi 屏幕密度的设备，其实际像素密度可能略有不同。 Android 将这些差异抽象概括到应用，使您可以提供为通用尺寸和密度设计的 UI，让系统按需要处理任何最终调整。 图 1 说明不同的尺寸和密度如何粗略归类为不同的尺寸 和密度组。

![image](
https://developer.android.google.cn/images/screens_support/screens-ranges.png)


在为不同的屏幕尺寸设计 UI 时，您会发现每种设计都需要 最小空间。因此，上述每种通用的屏幕尺寸都关联了系统定义的最低 分辨率。这些最小尺寸以“dp”单位表示 — 在定义布局时应使用相同的单位 — 这样系统无需担心屏幕密度的变化。


- 超大屏幕至少为 960dp x 720dp
- 大屏幕至少为 640dp x 480dp
- 正常屏幕至少为 470dp x 320dp
- 小屏幕至少为 426dp x 320dp


### 密度独立性


应用显示在密度不同的屏幕上时，如果它保持用户界面元素的物理尺寸（从 用户的视角），便可实现“密度独立性” 。

保持密度独立性很重要，因为如果没有此功能，UI 元素（例如 按钮）在低密度屏幕上看起来较大，在高密度屏幕上看起来较小。这些 密度相关的大小变化可能给应用布局和易用性带来问题。图 2 和 3 分别显示了应用不提供密度独立性和 提供密度独立性时的差异。

![image](https://developer.android.google.cn/images/screens_support/density-test-bad.png)
图 2. 不支持不同密度的示例应用在低、中、高密度屏幕上的显示情况。

![iamge](https://developer.android.google.cn/images/screens_support/density-test-good.png)
图 3. 良好支持不同密度（密度独立）的示例应用在低、中、高密度屏幕上的显示情况。


Android 系统可帮助您的应用以两种方式实现密度独立性：

- 系统根据当前屏幕密度扩展 dp 单位数
- 系统在必要时可根据当前屏幕 密度将可绘制对象资源扩展到适当的大小

在图 2 中，文本视图和位图可绘制对象具有以像素（px 单位）指定的尺寸，因此视图的物理尺寸在低密度屏幕上更大，在高密度 屏幕上更小。这是因为，虽然实际屏幕尺寸可能相同，但高密度屏幕 的每英寸像素更多（同样多的像素在一个更小的区域内）。在图 3 中，布局 尺寸以密度独立的像素（dp 单位）指定。由于 密度独立像素的基线是中密度屏幕，因此具有中密度屏幕的设备看起来 与图 2 一样。但对于低密度和高密度屏幕，系统 将分别增加和减少密度独立像素值，以适应 屏幕。


大多数情况下，确保应用中的屏幕独立性很简单，只需以适当的密度独立像素（dp 单位）或 "wrap_content" 指定所有 布局尺寸值。系统然后根据适用于当前屏幕密度的缩放比例适当地缩放位图可绘制对象，以 适当的大小显示。



### 如何支持多屏

Android 支持多种屏幕的基础是它能够管理针对当前屏幕配置 以适当方式渲染应用的布局和位图 可绘制对象。系统可处理大多数工作，通过适当地 缩放布局以适应屏幕尺寸/密度和根据屏幕密度缩放位图可绘制对象 ，在每种屏幕配置中渲染您的应用。但是，为了更适当地处理不同的屏幕配置 ，还应该：

- 为不同屏幕尺寸提供不同的布局

    通过声明您的应用支持哪些屏幕尺寸，可确保只有 其屏幕受支持的设备才能下载您的应用。声明对 不同屏幕尺寸的支持也可影响系统如何在较大 屏幕上绘制您的应用 — 特别是，您的应用是否在屏幕兼容模式中运行。
要声明应用支持的屏幕尺寸，应在清单文件中包含 <supports-screens> 元素。

- 为不同屏幕尺寸提供不同的布局

    默认情况下，Android 会调整应用布局的大小以适应当前设备屏幕。大多数 情况下效果很好。但有时 UI 可能看起来不太好，需要针对 不同的屏幕尺寸进行调整。例如，在较大屏幕上，您可能要调整 某些元素的位置和大小，以利用其他屏幕空间，或者在较小屏幕上， 可能需要调整大小以使所有内容纳入屏幕。
可用于提供尺寸特定资源的配置限定符包括 small、normal、large 和 xlarge。例如，超大屏幕的布局应使用 layout-xlarge/。
从 Android 3.2（API 级别 13）开始，以上尺寸组已弃用，您 应改为使用 sw<N>dp 配置限定符来定义布局资源 可用的最小宽度。例如，如果多窗格平板电脑布局 需要至少 600dp 的屏幕宽度，应将其放在 layout-sw600dp/ 中。声明适用于 Android 3.2 的平板电脑布局一节将进一步讨论如何使用新技术声明布局资源。

- 为不同屏幕密度提供不同的位图可绘制对象

    默认情况下，Android 会缩放位图可绘制对象（.png、.jpg 和 .gif 文件）和九宫格可绘制对象（.9.png 文件），使它们以适当的 物理尺寸显示在每部设备上。例如，如果您的应用只为 基线中密度屏幕 (mdpi) 提供位图可绘制对象，则在高密度 屏幕上会增大位图，在低密度屏幕上会缩小位图。这种缩放可能在 位图中造成伪影。为确保位图的最佳显示效果，应针对 不同屏幕密度加入不同分辨率的替代版本。
可用于密度特定资源的配置限定符（在下面详述） 包括 ldpi（低）、mdpi（中）、 hdpi（高）、xhdpi（超高）、xxhdpi （超超高）和 xxxhdpi（超超超高）。例如，高密度屏幕的位图应使用 drawable-hdpi/。

>注：仅当要在 xxhdpi 设备上提供比正常位图大的启动器图标时才需要提供 mipmap-xxxhdpi 限定符。无需为所有应用的图像提供 xxxhdpi 资源。

>将您的所有启动器图标放在 res/mipmap-[density]/ 文件夹中，而非 res/drawable-[density]/ 文件夹中。无论安装应用的设备屏幕分辨率如何，Android 系统都会将资源保留在这些密度特定的文件夹中，例如 mipmap-xxxhdpi。此 行为可让启动器应用为您的应用选择要显示在主 屏幕上的最佳分辨率图标。如需了解有关使用 mipmap 文件夹的详细信息，请参阅管理项目概览。


在运行时，系统通过 以下程序确保任何给定资源在当前屏幕上都能保持尽可能最佳的显示效果：

1.系统使用适当的备用资源

根据当前屏幕的尺寸和密度，系统将使用您的应用中提供的任何尺寸和 密度特定资源。例如，如果设备有 高密度屏幕，并且应用请求可绘制对象资源，系统将查找 与设备配置最匹配的可绘制对象资源目录。根据可用的其他 备用资源，包含 hdpi 限定符（例如 drawable-hdpi/）的资源目录可能是最佳匹配项，因此系统将使用此 目录中的可绘制对象资源。


2.如果没有匹配的资源，系统将使用默认资源，并按需要向上 或向下扩展，以匹配当前的屏幕尺寸和密度。

“默认”资源是指未标记配置限定符的资源。例如，drawable/ 中的资源是默认可绘制资源。 系统假设默认资源设计用于基线屏幕尺寸和密度，即 正常屏幕尺寸和中密度。 因此，系统对于高密度屏幕向上扩展默认密度 资源，对于低密度屏幕向下扩展。
当系统查找密度特定的资源但在 密度特定目录中未找到时，不一定会使用默认资源。系统在缩放时可能 改用其他密度特定资源提供更好的 效果。例如，查找低密度资源但该资源不可用时， 系统会缩小资源的高密度版本，因为 系统可轻松以 0.5 为系数将高密度资源缩小至低密度资源，与以 0.75 为系数 缩小中密度资源相比，伪影更少。

#### 使用配置限定符

要使用配置限定符：

1.在项目的 res/ 目录中新建一个目录，并使用以下 格式命名： <resources_name>-<qualifier>

- <resources_name> 是标准资源名称（例如 drawable 或 layout）。
- <qualifier> 是下表 1 中的配置限定符，用于指定 要使用这些资源的屏幕配置（例如 hdpi 或 xlarge）。

2.将适当的配置特定资源保存在此新目录下。这些资源 文件的名称必须与默认资源文件完全一样。

例如，xlarge 是超大屏幕的配置限定符。将 此字符串附加到资源目录名称（例如 layout-xlarge）时，它指向 要在具有超大屏幕的设备上使用这些资源的系统。

例如，以下应用资源目录 为不同屏幕尺寸和不同可绘制对象提供不同的布局设计。使用 mipmap/ 文件夹放置 启动器图标。

```
res/layout/my_layout.xml              // layout for normal screen size ("default")
res/layout-large/my_layout.xml        // layout for large screen size
res/layout-xlarge/my_layout.xml       // layout for extra-large screen size
res/layout-xlarge-land/my_layout.xml  // layout for extra-large in landscape orientation

res/drawable-mdpi/graphic.png         // bitmap for medium-density
res/drawable-hdpi/graphic.png         // bitmap for high-density
res/drawable-xhdpi/graphic.png        // bitmap for extra-high-density
res/drawable-xxhdpi/graphic.png       // bitmap for extra-extra-high-density

res/mipmap-mdpi/my_icon.png         // launcher icon for medium-density
res/mipmap-hdpi/my_icon.png         // launcher icon for high-density
res/mipmap-xhdpi/my_icon.png        // launcher icon for extra-high-density
res/mipmap-xxhdpi/my_icon.png       // launcher icon for extra-extra-high-density
res/mipmap-xxxhdpi/my_icon.png      // launcher icon for extra-extra-extra-high-density
```

#### 设计替代布局和可绘制对象

您应该创建的备用资源类型取决于应用的需求。 通常，您应该使用尺寸和方向限定符提供替代布局资源 ，并且使用密度限定符提供替代位图可绘制对象资源。

##### 替代布局

一般而言，在不同的屏幕配置上测试应用后，您会知道 是否需要用于不同屏幕尺寸的替代布局。例如：

- 在小屏幕上测试时，可能会发现您的布局不太适合 屏幕。例如，小屏幕设备的屏幕宽度可能无法容纳一排 按钮。在此情况下，您应该为小屏幕提供调整 按钮大小或位置的替代布局。
- 在超大屏幕上测试时，可能会发现您的布局无法 有效地利用大屏幕，并且明显拉伸填满屏幕。 在此情况下，您应该为超大屏幕提供替代布局，以提供 针对大屏幕（例如平板电脑）优化、重新设计的 UI。
虽然您的应用不使用替代布局也能在大屏幕上正常运行，但 必须让用户感觉您的应用看起来像是专为其 设备而设计。如果 UI 明显拉伸，用户很可能对 应用体验不满意。
- 而且，对比横屏测试和竖屏测试时 可能会发现，竖屏时置于底部的 UI 在横屏时应位于屏幕右侧。
简而言之，您应确保应用布局：

- 适应小屏幕（让用户能实际使用您的应用）
- 已针对大屏幕优化，可以利用其他屏幕空间
- 已同时针对横屏和竖屏方向优化


如果 UI 使用的位图即使在系统缩放 布局后也需要适应视图大小（例如按钮的背景图片），则应使用九宫格位图文件。九宫格文件基本上是一个指定可拉伸的二维区域的 PNG 文件。 当系统需要缩放使用位图的视图时，系统 会拉伸九宫格位图，但只拉伸指定的区域。因此，您无 需为不同的屏幕尺寸提供不同的可绘制对象，因为九宫格位图可 调整至任何大小。但您应该为不同的屏幕密度提供 九宫格文件的替代版本。

##### 替代可绘制对象

基本上每个应用都应该具有不同密度的替代可绘制对象 资源，因为基本上每个应用都有启动器图标，而且该图标应该在 所有屏幕密度中看起来都很好。同样，如果您的应用中包含其他位图可绘制对象（例如 应用中的菜单图标或其他图形），则应该为不同密度提供替代版本或 每种密度一个版本

>注：您只需要为 位图文件（.png、.jpg 或 .gif）和九宫格文件 (.9.png) 提供密度特定的可绘制对象。如果您使用 XML 文件定义形状、颜色或其他可绘制对象资源，应该 将一个副本放在默认可绘制对象目录中 (drawable/)。


要为不同的密度创建替代位图可绘制对象，应遵循六种通用密度之间的 3:4:6:8:12:16 缩放比率。例如，如果您的 位图可绘制对象是对中密度屏幕使用 48x48 像素，则所有不同的尺寸应为：

- 36x36 (0.75x) 用于低密度
- 48x48（1.0x 基线）用于中密度
- 72x72 (1.5x) 用于高密度
- 96x96 (2.0x) 用于超高密度
- 144x144 (3.0x) 用于超超高密度
- 192x192 (4.0x) 用于超超超高密度（仅限启动器图标；请参阅上面的 注）



### 最佳做法
支持多种屏幕的目标是创建一款在 Android 系统支持的通用屏幕尺寸上都可以 正常运行且显示良好的应用。本文档 前面各节内容介绍了 Android 系统如何使您的 应用适应屏幕配置，以及如何在不同的 屏幕配置上自定义应用的外观。本节提供另外一些提示以及有助于 确保应用针对不同屏幕配置正确缩放的 技巧概览。

下面是有关如何确保应用在 不同屏幕上正常显示的快速检查清单：

1.在 XML 布局文件中指定尺寸时使用 wrap_content、match_parent 或 dp 单位 。

2.不要在应用代码中使用硬编码的像素值

3.不要使用 AbsoluteLayout（已弃用）

4.为不同屏幕密度提供替代位图可绘制对象

#### 1. 对布局尺寸使用 wrap_content、match_parent 或 dp 单位

为 XML 布局文件中的视图定义 android:layout_width 和 android:layout_height 时，使用 "wrap_content"、 "match_parent" 或 dp 单位可确保在当前设备屏幕上为 视图提供适当的尺寸。

例如，layout_width="100dp" 的视图在 中密度屏幕上测出宽度为 100 像素，在高密度屏幕上系统会将其扩展至 150 像素宽， 因此视图在屏幕上占用的物理空间大约相同。

类似地，您应选择 sp（缩放独立的像素）来定义文本 大小。sp 缩放系数取决于用户设置，系统 会像处理 dp 一样缩放大小。

#### 2. 不要在应用代码中使用硬编码的像素值

由于性能的原因和简化代码的需要，Android 系统使用像素作为 表示尺寸或坐标值的标准单位。这意味着， 视图的尺寸在代码中始终以像素表示，但始终基于当前的屏幕密度。 例如，如果 myView.getWidth() 返回 10，则表示视图在 当前屏幕上为 10 像素宽，但在更高密度的屏幕上，返回的值可能是 15。如果 在应用代码中使用像素值来处理预先未针对 当前屏幕密度缩放的位图，您可能需要缩放代码中使用的像素值，以与 未缩放的位图来源匹配。

#### 3. 不要使用 AbsoluteLayout

与其他布局小工具不同，AbsoluteLayout 会强制 使用固定位置放置其子视图，很容易导致 在不同显示屏上显示效果不好的用户界面。因此，AbsoluteLayout 在 Android 1.5（API 级别 3）上便已弃用。

#### 4. 使用尺寸和密度特定资源


### 注意事项

资源（例如位图可绘制对象）的预缩放

根据当前屏幕的密度，系统将使用您的应用中提供的任何尺寸或 密度特定资源，并且不加缩放而显示它们。如果没有可用于正确密度 的资源，系统将加载默认资源，并按需要向上或向下扩展，以 匹配当前屏幕的密度。系统假设默认资源（ 没有配置限定符的目录中的资源）针对基线屏幕密度 (mdpi) 而设计， 除非它们加载自密度特定的资源目录。因此，系统 会执行预缩放，以将位图调整至适应当前屏幕 密度的大小。
如果您请求预缩放的资源的尺寸，系统将返回 代表缩放后尺寸的值。例如，针对 mdpi 屏幕以 50x50 像素 设计的位图在 hdpi 屏幕上将扩展至 75x75 像素（如果没有 用于 hdpi 的备用资源），并且系统会这样报告大小。
有时您可能不希望 Android 预缩放 资源。避免预缩放最简单的方法是将资源放在 有 nodpi 配置限定符的资源目录中。例如：
res/drawable-nodpi/icon.png
当系统使用此文件夹中的 icon.png 位图时，不会 根据当前设备密度缩放。

# 参考
[android developer](https://developer.android.google.cn/guide/)
