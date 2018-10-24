#### 1.问题简述：
在Settings里搜索“Notification access”，点击进入，出现SwitchButton条目，点击SwitchButton，出现弹出框，此时将手机屏幕横屏，Settings发生崩溃。
#### 2.查看log

```
java.lang.RuntimeException: Unable to start activity ComponentInfo{com.android.settings/com.android.settings.Settings$NotificationAccessSettingsActivity}: android.app.Fragment$InstantiationException: Unable to instantiate fragment com.android.settings.utils.ManagedServiceSettings$ScaryWarningDialogFragment: make sure class name exists, is public, and has an empty constructor that is public
    at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2724)
    at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2789)
    at android.app.ActivityThread.-wrap12(ActivityThread.java)
    at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1527)
    at android.os.Handler.dispatchMessage(Handler.java:110)
    at android.os.Looper.loop(Looper.java:203)
    at android.app.ActivityThread.main(ActivityThread.java:6256)
    at java.lang.reflect.Method.invoke(Native Method)
    at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1071)
    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)
Caused by: android.app.Fragment$InstantiationException: Unable to instantiate fragment com.android.settings.utils.ManagedServiceSettings$ScaryWarningDialogFragment: make sure class name exists, is public, and has an empty constructor that is public
    at android.app.Fragment.instantiate(Fragment.java:633)
    at android.app.FragmentState.instantiate(Fragment.java:111)
    at android.app.FragmentManagerImpl.restoreAllState(FragmentManager.java:1942)
    at android.app.FragmentController.restoreAllState(FragmentController.java:135)
    at android.app.Activity.onCreate(Activity.java:959)
    at com.android.settingslib.drawer.SettingsDrawerActivity.onCreate(SettingsDrawerActivity.java:76)
    at com.android.settings.SettingsActivity.onCreate(SettingsActivity.java:569)
    at android.app.Activity.performCreate(Activity.java:6666)
    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1118)
    at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2677)
    ... 9 more
Caused by: java.lang.InstantiationException: java.lang.Class<com.android.settings.utils.ManagedServiceSettings$ScaryWarningDialogFragment> has no zero argument constructor
    at java.lang.Class.newInstance(Native Method)
    at android.app.Fragment.instantiate(Fragment.java:622)
    ... 18 more
```
#### 3.简单分析

- 我们知道当手机横屏时，当横屏时Activity的生命周期会重新调用一次，详见：https://developer.android.google.cn/guide/topics/resources/runtime-changes.html
- 从log来看异常是无法创建NotificationAccessSettingsActivity，原因是Fragment.instantiate方法抛出了java.lang.InstantiationException，代码如下：

```
    public static Fragment instantiate(Context context, String fname, @Nullable Bundle args) {
        try {
            Class<?> clazz = sClassMap.get(fname);
            if (clazz == null) {
                // Class not found in the cache, see if it's real, and try to add it
                clazz = context.getClassLoader().loadClass(fname);
                if (!Fragment.class.isAssignableFrom(clazz)) {
                    throw new InstantiationException("Trying to instantiate a class " + fname
                            + " that is not a Fragment", new ClassCastException());
                }
                sClassMap.put(fname, clazz);
            }
            Fragment f = (Fragment)clazz.newInstance();
            if (args != null) {
                args.setClassLoader(f.getClass().getClassLoader());
                f.mArguments = args;
            }
            return f;
        } catch (ClassNotFoundException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (java.lang.InstantiationException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new InstantiationException("Unable to instantiate fragment " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        }
    }
```

在622行也就是“Fragment f = (Fragment)clazz.newInstance()”发生了异常，进一步看l

```
java.lang.Class<com.android.settings.utils.ManagedServiceSettings$ScaryWarningDialogFragment> has no zero argument constructor
```

也就是说当Fragment去实例化ScaryWarningDialogFragment类时发生了异常，注意ScaryWarningDialogFragment是一个内部类，其外部类为ManagedServiceSettings，注意，也是一个Fragment。

- 我们看看ManagedServiceSettings和ScaryWarningDialogFragment代码

```
public abstract class ManagedServiceSettings extends EmptyTextSettings {
    ...
    protected boolean setEnabled(ComponentName service, String title, boolean enable) {
        if (!enable) {
            // the simple version: disabling
            mServiceListing.setEnabled(service, false);
            return true;
        } else {
            if (mServiceListing.isEnabled(service)) {
                return true; // already enabled
            }
            // show a scary dialog
            new ScaryWarningDialogFragment()
                    .setServiceInfo(service, title)
                    .show(getFragmentManager(), "dialog");
            return false;
        }
    }
    public class ScaryWarningDialogFragment extends DialogFragment {
        ...
    }
}
```

这里ScaryWarningDialogFragment直接继于DialogFragment，并且没有重写其他构造方法，那为什么Fragment通过反射newInstance方法无法实例化呢？通过搜索，找到了一篇文章：http://blog.csdn.net/xplee0576/article/details/43057633 上面的log和我们的很相似，文章分析的原因是没有public的empty constructor从而导致了发射实例化失败，于是加上了空的构造函数

```
public ScaryWarningDialogFragment() {}
```

然后编译，push到手机运行，结果一样，崩溃，所以不是没有public的empty constructor的原因。

- 再一次审视代码，发现IDE提示ScaryWarningDialogFragment是一个内部类，需要添加static。于是猜想可能和内部类有关系，通过google搜索，找到了一篇文章：http://www.jianshu.com/p/6a362ea4dfd8， 文章提到一个非静态内部类的默认构造函数是持有外部类的引用，也是说默认的不是无参的构造函数，既然不是无参构造函数，那么就能解释为什么Fragment通过newInstance方法反射发生失败了。
- 为了进一步验证，自己写了一个纯JavaProject的Damo，验证通过发射来实例化非静态内部类，结果确实抛出了java.lang.InstantiationException，相关用法可以参考：http://blog.csdn.net/id19870510/article/details/4965623
- 至此找到了发生异常的原因，当我们点击按钮的时候，外部类通过new ScaryWarningDialogFragment()方式来初始化没有问题，但是当很手机横屏的时候，Fragment要经历销毁到重新创建的过程，走到了Fragment.instantiate方法，从而导致了crash

#### 3.方案调研：
将ScaryWarningDialogFragment变成静态内部类，这样当Fragment通过反射实例化就没有问题了。但是ScaryWarningDialogFragment引用了外部类的一些变量，如果变成静态内部类，那么则无法访问。我们看看ScaryWarningDialogFragment引用了外部类哪些变量

```
    public class ScaryWarningDialogFragment extends DialogFragment {
        static final String KEY_COMPONENT = "c";
        static final String KEY_LABEL = "l";

        public ScaryWarningDialogFragment setServiceInfo(ComponentName cn, String label) {
            Bundle args = new Bundle();
            args.putString(KEY_COMPONENT, cn.flattenToString());
            args.putString(KEY_LABEL, label);
            setArguments(args);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            final String label = args.getString(KEY_LABEL);
            final ComponentName cn = ComponentName.unflattenFromString(args
                    .getString(KEY_COMPONENT));

            final String title = getResources().getString(mConfig.warningDialogTitle, label);
            final String summary = getResources().getString(mConfig.warningDialogSummary, label);
            return new AlertDialog.Builder(mContext)
                    .setMessage(summary)
                    .setTitle(title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.allow,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mServiceListing.setEnabled(cn, true);
                                }
                            })
                    .setNegativeButton(R.string.deny,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // pass
                                }
                            })
                    .create();
        }
    }
```

其中mConfig，mServiceListing和mContext都是外部类的变量，mContext可以用getActivity代替，但是mConfig和mServiceListing怎么办？先看看官方关于Fragment的介绍：https://developer.android.google.cn/reference/android/app/Fragment.html， 其中有个方法setArguments，官方文档是这样说的：

```
Supply the construction arguments for this fragment. The arguments supplied here will be retained across fragment destroy and creation.
```

#### 解决方案：

1. 既然有setArguments方法为我们保存数据，那么我们在初始化ScaryWarningDialogFragment时将mConfig和mServiceListing保存到arguments中，这样在onCreateDialog时再次获取，就可以得到mConfig和mServiceListing
2. 通过Fragment的setTargetFragment和onActivityResult来实现，将ComponentName实例传递到外部类的Fragment去处理。

