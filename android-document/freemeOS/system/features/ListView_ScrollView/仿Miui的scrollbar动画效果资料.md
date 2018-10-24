# 资料搜集

- [仿 MIUI 弹性拉伸 view](http://blog.csdn.net/wxy318/article/details/53246322)
- [github](https://github.com/prife/elasticity)

# 实现分析

### 1、动画效果

参考 [属性动画官网文档](https://developer.android.com/guide/topics/graphics/prop-animation.html#listeners)
代码可ApiDemos。

PS. 可以使用google的图形绘制语言学习动画系统的差值器。

### 2、View移动/变形的原理

参考

- http://blog.csdn.net/eieihihi/article/details/45668189
- https://developer.android.com/reference/android/view/View.html


利用下面的API可以实现IOS的弹性效果

```java
setTranslationX 实现水平位移
setTranslationY 实现竖直位移
```

利用下面的API则可以实现小米的弹性拉伸效果

```java
setPivotX
setPivotY
setScaleX 实现水平拉伸
setScaleY 实现竖直拉伸
```

## 小米方案

### 刷入开发版软件包

公司小米版本为 红米NOTE(1代) LTE版本

- 小米官方MIUI8开发包下载，http://www.miui.com/zt/miui8/dev.html
- 小米官方卡刷方法，http://www.miui.com/shuaji-329.html

### 反编译分析

**小米资料**

dump出来的小米系统

```
\\prifepc\sharedir\projects\xiaomi\eng 目录下
```

**测试方法**

安装ApiDemo，测试来看，Listview并没有弹性拉伸的效果

**实现猜测**

小米的改法没有影响原生Listview系统控件效果，小米的实现有两种可能：

1. 小米没有修改ListView控件，那么可能的实现方式包括：1）类似demo代码中的方法，使用辅助类外挂实现 2）通过布局到一个具有该功能的特殊View上，3）其他暂时未知方法
2. 小米修改了ListView控件代码，那么可能的方法包括 1）

目前对小米手机反编译的步骤如下：

1. 反编译具有ListView控件动效的应用，包括`联系人`和`设置应用`，配合`hierarchyviewer`工具查看布局。

没有发现调用setScale的语句，也没有发现特殊的View，因为**怀疑，小米没有采用类似demo控件的方法**

2. 下一步调查方向

直接反编译小米ListView所在系统库（framework.jar），分析小米是否直接在其中修改了呢？

需要进一步深入细致的分析。

## Freeme实现

初步考虑，有以下几种方法：

1. 类似`elasticity`代码的实现方式，
2. 继承Listview/Scrollview控件，实现一个新类
3. 给原生的ListView类增加一个动画特效属性
4. 其他思路，如分析出的小米的方式

