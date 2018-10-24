[TOC]

| 版本 | 修改日期 | 作者 | 修改内容 |
| :---| ----------| ---- | ---- |
| v1.0 | 2018.06.30 | 李秋月 | 初版 |

## 问题
进到设置里面，屏保-时钟-样式，选择之后，长按桌面 “时钟” 图标，点击 “启动屏保”，结果样式切换并不生效。

## 踩坑历程

查看代码，发现设置里，点击样式切换的时候只是更新了 summry，并没有执行写值的操作，于是乎，我自己手动添加了如下代码

```
public boolean onPreferenceChange(Preference pref, Object newValue) {

    if (KEY_CLOCK_STYLE.equals(pref.getKey())) {
                final ListPreference clockStylePref = (ListPreference) pref;
                final int index = clockStylePref.findIndexOfValue((String) newValue);
                clockStylePref.setSummary(clockStylePref.getEntries()[index]);
            }

            // Add start
            SharedPreferences sp = mContext.getSharedPreferences(PreferenceManager.getDefaultSharedPreferencesName(mContext), mContext.MODE_PRIVATE);                SharedPreferences.Editor edit = sp.edit();
            if (newValue.equals("analog")) {
                edit.putString("clock_style","analog");
            } else {
                edit.putString("clock_style","digital");
            }
           edit.commit();
           // Add end

    return true;
}
```

手动往 SharedPreferences 写值。因为取值的时候就是通过这种方式 getString() 的，看起来毫无破绽吧，是吧，我也这么认为的。但是这种改法并不生效。为什么呢？

接下来，就去看手机里应用下面的 xml 文件，写进去的值是什么？这里以时钟为例， adb shell 进去之后，进到如下的路径：

```
/data/user_de/0/com.android.deskclock/shared_prefs/com.android.deskclock_preferences.xml
```

内容如下：

```
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="screensaver_night_mode" value="true" />
    <string name="screensaver_clock_style">digital</string>
    <boolean name="display_clock_seconds" value="false" />
    <int name="intent.extra.alarm.global.id" value="1" />
    <string name="clock_style">digital</string>
</map>
```

发现 clock_style 这个值至始至终都没有改变过，这就奇怪了。并且我通过 debug 模式，断点，发现我添加的代码确实也走了，并且成功了，那么为什么还是没有生效呢？

最后通过请教小明同学，发现我写进去的值，改的是另一个路径下的文件

```
data/data/com.android.deskclock/shared_prefs/com.android.deskclock_preferences.xml
```

对，问题就出在这里，我其实改动的是 data/data/ 下面的 xml 文件，但是却从 data/user_de/0/ 下面的 xml 读取的 ，那为什么会有两个 xml 文件呢？

于是把代码稍稍修改了一下，问题就解决了。

```
SharedPreferences sp = mContext.createDeviceProtectedStorageContext().getSharedPreferences(PreferenceManager.getDefaultSharedPreferencesName(mContext), mContext.MODE_PRIVATE);
```

是的，我只添加了 createDeviceProtectedStorageContext() 这个方法。

细心的人不难发现，我是用了两个不同的 Context 操作了这个 SharedPreferences

- 1、getApplicationContext() 操作的是默认的 data/data 下面的 pref

- 2、getApplicationContext().createDeviceProtectedStorageContext() 操作的是 data/user_de/0 下面的 pref

7.0 之后，出于安全考虑，使用了 Context.moveSharedPreferencesFrom() 和 Context.moveDatabaseFrom() ，对原来的数据进行转移。其中这里用到的 Context，就是 createDeviceProtectedStorageContext() 得到的 Context。
所以 存 pref 的路径有两个，获取哪一个路径，用相应的 context 就可以了。

可以这样理解：

- 1、且当只有配置了 “直接启动” 模式的应用，SharedPreference 自动保存的数据，会在 data/user_de/0 下面。

- 2、自己创建的 SharedPreferences，可以是 data/data下面，也可以是 data/user_de/0 下面，取决于使用的是哪种 Context 去 getSharedPreferences()。

android 7.0 推出了 Direct Boot 新特性，其中也用到了这个方法，这里就不详细讲解了。为何 AOSP 要这样设计？具体详情请查看 [直接启动](https://developer.android.com/training/articles/direct-boot)。
