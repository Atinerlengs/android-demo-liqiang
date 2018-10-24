# 综述

Navigtaion Bar有两个效果，分别是：

- Translucent，在该效果下，导航栏上会有一层浅灰色背景
- 设置颜色，可以实现透明（Transparent）或者其他纯色颜色效果

上面两个效果分别由几个属性控制，并且这两组属性之间互斥。

## 1. Translucent

可在style.xml中添加如下属性：

```xml
<item name="android:windowTranslucentNavigation">true</item>
<item name="android:windowTranslucentStatus">true</item>
```

若应用设置了windowTranslucentStatus/windowTranslucentNavigation属性为true后，Android系统布局会设定应用窗口无状态栏/导航栏，也就是说应用会以整屏幕方式布局，此时导航栏会以半透明的效果悬浮在应用窗口上（注意:状态栏则是全透明效果）。
此时可以配合如下属性（添加到layout xml文件中），系统自动为StatusBar/NavigationBar留出空间。

```
android:fitsSystemWindows="true"
```

## 2. 设置颜色

实例1：设置状态栏为绿色不透明、导航栏为红色不透明

可在style中添加如下效果，

```xml
<item name="android:windowTranslucentStatus">false</item>
<item name="android:statusBarColor">#FF00FF00</item>
<item name="android:windowTranslucentNavigation">false</item>
<item name="android:navigationBarColor">#FFFF0000</item>
```

**注意** ，当设置了 `windowTranslucentNavigation` 为true时，设置 `navigationBarColor` 将失效。

实例2：设置导航栏透明

```xml
<item name="android:windowTranslucentNavigation">false</item>
<item name="android:navigationBarColor">#00000000</item>
```

其次在代码中（onCreate函数或其他合理位置）添加代码

```java
getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
```

这句代码设置Android的窗口系统假定不存在NavigationBar的方式布局，也就是窗口会将NavigationBar的空间也用于自身布局，此时NavigationBar就覆盖到了应用窗口上了。

PS. 同理，如果需要实现StatusBar透明，则添加View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN。

## demo

参考资料[1]中给出的Translucent与Transparent效果可以通过上面的代码实现（已经在华为Nexus6)上验证。

参考资料[4]提供了一个测试代码，github地址为

```
https://github.com/D-clock/AndroidSystemUiTraining
```

经测试，该应用全部使用Translucent属性实现，因此该应用并不能实现真正的全透明效果，如有疑问请阅读参考资料[2]或下载代码验证。

**我们可以基于该代码编写我们自己的演示demo。**

# 遗留问题

是否有办法实现参考资料1中演示的Light效果？

**个人猜测** : 目前Android系统中并不能编写一个应用NavigationBar为背景为白色同时三个icon为灰色的效果。

# 参考资料

1. https://material.google.com/layout/structure.html#structure-system-bars
2. http://stackoverflow.com/questions/26474125/android-4-4-translucent-status-and-navigation-bars-style-on-android-5-0
3. http://stackoverflow.com/questions/29069070/completely-transparent-status-bar-and-navigation-bar-on-lollipop
4. http://www.jianshu.com/p/0acc12c29c1b#
