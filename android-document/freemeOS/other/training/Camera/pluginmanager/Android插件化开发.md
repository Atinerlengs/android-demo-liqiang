## 题记

### 背景

当时的情况是这样的，我们一直在mtk相机上做二次开发，开发了例如水印、儿童等模式，还有一些基础功能。但每次mtk大版本迭代的时候，mtk都会调整框架，移植就成了世界上最痛苦的事情。我们的解决方案就是要开发一个独立相机app,能在所有平台上运行。当时，我们有4个兄弟参于这次开发。那么问题来了，我们如何分工和并发工作呢？我们协商决定把所的模式都设计成插件式。只要把插件化的框架搭建出来，其它的兄弟就可以关注具体插件的开发了。这样就可以独立并行的进行开发了，不再担心代码冲突，功能依赖的问题了。

## 开始工作

我们研究了一下插件开发，当时网上大多数都是给予app安装的方式实现，这样做一大好处就是不需要管理资源。使用Android自带的PathClassLoader可以在安装目录动态加载字节码。并且能返回插件的Context，由于插件是安装的，拿到了插件的Context，就是可以使用Android方式操作插件的资源了。也就是可以通过R引用访问资源。具体代码如下：

```  Plugin.java
package com.freeme.pluginmanager;

import java.lang.reflect.Constructor;
import dalvik.system.PathClassLoader;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

public class Plugin {
    private static final String TAG = "[FREEME_DBG]Plugin";
    private Context mHostContext;
    private ActivityInfo mActivityInfo;
    private String mName; // the name of plugin
    private String mPackageName;
    private String mType;
    private BasePlugin mInstance;

    public Plugin(Context hostContext, String pkgName, ActivityInfo info) {
        mHostContext = hostContext;
        mActivityInfo = info;
        mPackageName = pkgName;
        initialize();
    }

    public final BasePlugin getInstance() {
        return mInstance;
    }

    public final String getType() {
        return mType;
    }

    public final String getName() {
        return mName;
    }

    public final String getPackageName() {
        return mPackageName;
    }

    private void initialize() {
        Bundle metaDatas = mActivityInfo.metaData;
        if (metaDatas != null) {
            mType = metaDatas.getString(PluginUtil.KEY_PLUGIN_TYPE);
        }

        // create the instance of plugin
        createPluginObject();

        if (mInstance != null) {
            mName = mInstance.mContext.getResources().getString(mActivityInfo.labelRes);
            Log.i(TAG, "initialize(): plugin name = " + mName);
        }
    }

    private void createPluginObject() {
        Log.i(TAG, "createPluginObject(): Enter ");
        ClassLoader cl = new PathClassLoader(mActivityInfo.applicationInfo.sourceDir,
                mHostContext.getClassLoader());
        try {
            Constructor constructor = cl.loadClass(mActivityInfo.name).getConstructor(
                    Context.class, String.class);
            mInstance = (BasePlugin) constructor.newInstance(mHostContext, mPackageName);
        } catch (Exception e) {
            Log.i(TAG, "createPluginObject(): error! " + e.getMessage());
            e.printStackTrace();
        }
    }

    // TODO: can configure plugin properties
}

```
具体可以参考我们目前相机的插件管理代码PluginInterface。

## 主仆通讯

### 相机调用插件接口

通过事先定义的接口IPluginModuleEntry，让宿主调用，接口如下：

```
package com.freeme.camera;

import com.freeme.camera.data.PictureSizeInfo;
import com.freeme.camera.data.Size;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.net.Uri;

public interface IPluginModuleEntry {
    int getModuleID();

    String getPkgName();

    Drawable getModuleIcon();

    String getModuleTitle();

    void showPanel(ViewGroup root);

    void hidePanel();

    void switchPanel(ViewGroup root);

    byte[] blendOutput(byte[] jpegData);

    void mediaSaved(Uri uri);
    
    PictureSizeInfo getPictureSizeInfo();
    
    boolean isInterceptCapture();
    
    void setVisible(int visible);
    
}

```

但我们开发插件都是继承BasePluginImpl的，主要是解决后面接口的扩展，如果直接实现IPluginModuleEntry，哪天需要添加一个接口，就需要所有插件去实现，把所有代码修改一遍。

### 插件调用相机的接口

刚开始，插件没有调用相机接口需求，只有开发大片的时候，发现插件也要调用相机的接口了。其实最常用的一种方法，就是在IPluginModuleEntry中加入callback接口，让插件需要的时候，自己注册，然后宿主相机程序去实现。但是由于时间紧，薛大神就用静态接口对象实现了。

具体接口请参考 ICameraExt.java

```
package com.freeme.camera;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;

import com.freeme.camera.data.Size;

public interface ICameraExt {

    public int getOrientationCompensation();

    public Object getGLRoot();

    public void setSwipingEnabled(boolean b);

    public void runOnUiThread(Runnable runnable);

    public Size getPictureSize();

    public Context getHostContext();

    public int getOrientation();

    public void setTopBarVisible(int visible);

    public void setBottomBarVisible(int visible);

    public void onShutter();

    public void setTopBarBackgroudcolor(int color);

    public AlertDialog.Builder getAlertDialog();

    public void sendCommand(int command);

    public Camera getApi1Camera();

}

```

## 目前相机插件问题

-  apk需要安装。
- 由于和宿主共享userId,安装和卸载plugin都会引起相机退出。
- 每次安装和卸载都要通过packageManager遍历一遍手机中安装的应用，判断是不是插件。如果手机中安装的应用比较多时很耗时。
- 设计的插件调用宿主相机的接口，明显是为了快速的解决问题。有点背离此框架设计的思想，不过你们看其它本地插件开发，也会发现添加接口，根本就不在IPluginModuleEntry中添加，而是转型为具体插件类直接调用。当然这事是我干的，主要也是为了快速实现。
- scanPlugins没有设计成在单独的线程执行，所以会阻塞主线程，具体表现是第一次预览出来后，加载插件时，预览会卡一下。

## android插件开发第二种方式

要解决上述的问题，如果apk不需要安装就可以运行就好了。当然这也是可以实现的，可以通过一个宿主程序来运行一些未安装的apk。通过ClassLoader可以动态加载类，这样代码层面的加载是没有问题的，主要是资源，apk没有安装，就是不能使用R去访问插件的资源，这是一个棘手的问题。插件中Activity就是一个普通的类，没有安装，系统根本就不认识，也不会维护它的activity的生命周期。activity的生命周期需要宿主去模拟。这是另一个比较棘手的问题。

## 思路

如果让宿主程序直接加载apk文件，PathClassLoader就没有办法完成这个工作，PathClassLoader 只会加载/data/app目录下的apk，这也意味着，它只能加载已经安装的apk。幸好android提供了DexClassLoader,可以加载文件系统上的jar、dex、apk。讲ClassLoader原理时，说到的URLClassLoader是可以加载java中的jar,但是android修改了jvm，目前提供dalvik不能直接识别标准的字节码，所以在android系统中URLClassLoader是无法使用的。

顺便说一下，标准的jar，可以通过android sdk中platform-tools目录下的dx工具转换成dalvik所能识别的字节码文件。

转换命令 : dx -- dex -- output = dest.jar src.jar

关于此方式的实现,github有一个开源的项目，地址如下：
https://github.com/singwhatiwanna/dynamic-load-apk

mtk o版本也有插件管理代码：
vendor/mediatek/proprietary/frameworks/opt/appluginmanager

实现核心思想和github上的差不多。github的代码设计更简单。

## 代码讲解

github上的代码自己看就好了，我们就讲mtk的实现，mtk实现做了很抽象，我个人认为代码质量比较github上的这篇要好得多。

首先看preloadAllPlugins接口：

```
public void preloadAllPlugins(final boolean signatureCheckEnabled,
                                  final boolean xmlValidateEnabled,
                                  final boolean preloadPluginClassEnabled,
                                  final PreloaderListener listener) {
        final ArrayList<String> archivePaths = getAllArchivePath();
        if (archivePaths == null || archivePaths.size() == 0) {
            Log.d(TAG, "<preloadAllPlugins> archivePaths empty, call onPreloadFinished directly");
            listener.onPreloadFinished();
            return;
        }
        int pluginCount = archivePaths.size();
        final CountDownLatch latch = new CountDownLatch(pluginCount);
        for (int i = 0; i < pluginCount; i++) {
            final int index = i;
            Job<Void> job = new Job<Void>() {
                @Override
                public Void run(JobContext jc) {
                    Log.d(TAG, "<preloadAllPlugins> plugin path " + archivePaths.get(index));
                    PluginDescriptor pluginDescriptor =
                            Preloader.getInstance().preloadPlugin(mContext,
                                    archivePaths.get(index), mNativeLibDir,
                                    signatureCheckEnabled, xmlValidateEnabled);
                    Log.d(TAG, "<preloadAllPlugins> pluginDescriptor " + pluginDescriptor);
                    if (pluginDescriptor != null) {
                        mRegistry.addPluginDescriptor(pluginDescriptor);
                    }
                    return null;
                }
            };
            ThreadPool.getInstance().submit(job, new FutureListener<Void>() {
                @Override
                public synchronized void onFutureDone(Future<Void> future) {
                    latch.countDown();
                    Log.d(TAG, "<preloadAllPlugins.onFutureDone> latch count " + latch.getCount());
                    if (latch.getCount() != 0) {
                        return;
                    }
                    mRegistry.generateRelationship();
                    listener.onPreloadFinished();
                    Log.d(TAG, "<preloadAllPlugins.onFutureDone> onPreloadFinished done!");
                    if (!preloadPluginClassEnabled) {
                        return;
                    }
                    Set<String> pluginsId = mRegistry.getAllPluginsId();
                    for (String pluginId : pluginsId) {
                        ThreadPool.getInstance().submit(new Job<Void>() {
                            @Override
                            public Void run(JobContext jc) {
                                getPlugin(pluginId);
                                return null;
                            }
                        });
                    }
                }
            });
        }
    }

```

主要是使用线程池去加载，使用CountDownLatch计数判断一个插件属性(为什么是属性？可以理解为这个过程就是apk的安装过程，只是填充PluginDescriptor,这个是对插件数据与操作的一种抽象，可以理解为和linux系统的FileDescriptor抽象类似)是否加载完成，如果latch.getCount() == 0表示所有的任务都完成了，也就是所有的插件属性都加载完成。然后通知主调线程完成状态。

不难看出真正加载的接口是
```
Preloader.getInstance().preloadPlugin(mContext,
                                    archivePaths.get(index), mNativeLibDir,
                                    signatureCheckEnabled, xmlValidateEnabled);
```

具体实现如下：

```
public PluginDescriptor preloadPlugin(Context context, String archivePath,
                                          String nativeLibDir, boolean signatureCheckEnabled,
                                          boolean xmlValidateEnabled) {
        TraceHelper.beginSection(">>>>Preloader-preloadPlugin");

        // Do some initial operation here, these operations only do when preload the first plugin
        initPreloadEnviorment(context, signatureCheckEnabled);

        // File the ZipFile to process this plugin
        ZipFile zipFile = ZipCenter.createZipFile(archivePath);
        if (zipFile == null) {
            Log.d(TAG, "<preloadPlugin> Cannot find the ZipFile to process, return null");
            TraceHelper.endSection();
            return null;
        }

        // Get and check signature of plugin
        if (signatureCheckEnabled) {
            Signature[] targetFileSig = zipFile.getSignature();
            if (mHostSignature == null || targetFileSig ==PluginDescriptor null
                    || !ArrayUtils.areExactMatch(mHostSignature, targetFileSig)) {
                Log.d(TAG, "<preloadPlugin> Signature not match, return null");
                zipFile.recycle();
                TraceHelper.endSection();
                return null;
            }
        }

        // Schema validate
        if (xmlValidateEnabled && !zipFile.validateXML(getXsdInputStream(context))) {
            Log.e(TAG, "<preloadPlugin> Schema validate fail, return null");
            zipFile.recycle();
            TraceHelper.endSection();
            return null;
        }

        // Parse plugin.xml and get PluginDescriptor
        IResource resource = zipFile.getResource(context);
        XMLParser xmlfile = new XMLParser(zipFile.getXmlInputStream(), resource);
        PluginDescriptor descriptor = (PluginDescriptor) xmlfile.parserXML();
        if (descriptor == null) {
            Log.e(TAG, "<preloadPlugin> parserXML return null, return null");
            zipFile.recycle();
            TraceHelper.endSection();
            return null;
        }
        if (!isMatchHostVersion(descriptor)) {
            Log.e(TAG, "<preloadPlugin> Version is not match with host, return null");
            zipFile.recycle();
            TraceHelper.endSection();
            return null;
        }
        descriptor.setArchivePath(archivePath);

        // Init Element for apk
        if (zipFile instanceof ApkFile) {
            descriptor.setAssetManager(((ApkResource) resource).getAssetManager());
            descriptor.setResource(((ApkResource) resource).getResources());
            descriptor.setPackageInfo(((ApkFile) zipFile).getPackageInfo(context));
        }

        // Copy so lib to native lib dir
        zipFile.copySoLib(context, nativeLibDir);

        // Print all elements for debug
        // descriptor.printf();

        zipFile.recycle();
        TraceHelper.endSection();
        return descriptor;
    }

```
可以看出首先是设置环境，签名和认证检查。然后检查 xml是否缓存PluginDescriptor。后面是设置plugin最重要的三个数据：

-  Asset
-  Resource
-  PackageInfo

这是仿照android PackageManager的实现的。想一下，其实android应用也就是资源和代码。资源主要也就是Asset和Resource两种，代码是通过PackageInfo封装的。

### 第一个棘手问题解决方法

上面我们提到了未安装的方式实现插件化开发，最棘手就是资源管理，如果让我开发，第一感觉就是把Resource和Asset目前解压出来，通过操作文件方式获取资源，但是立马也会想到如何做分辨适配。再一想，还是算了吧，这不是让我把Android的资源处理从头到脚实现一遍，代价太高了，也是我们当初选择通过安装方式实现相机插件开发主要原因。

现在插件开发方式是如何解决这个问题的呢？要不想通过上述方式自己实现，肯定要找人来帮忙啊。找谁？当然是Android系统，如果能实例化Android的AssetManager和Resource，然后把指定的资源路径传进去就好了。

现在的实现：

```
private Resources getResource() {
        if (mResources == null) {
            TraceHelper.beginSection(">>>>ApkResource-getResource");
            Constructor<?> con = ReflectUtils.getConstructor(AssetManager.class);
            mAssetManager = (AssetManager) ReflectUtils.createInstance(con);
            Method addAssertPath =
                    ReflectUtils.getMethod(mAssetManager.getClass(), "addAssetPath",
                            String.class);
            Log.d(TAG, "<getResource> addAssertPath " + mFilePath);
            ReflectUtils.callMethodOnObject(mAssetManager, addAssertPath, mFilePath);
            Resources resources = new Resources(mAssetManager, mMetrics, mConfiguration);
            Log.d(TAG, "<getResource> resources " + resources);
            mResources = resources;
            TraceHelper.endSection();
        }
        return mResources;
    }

```

上面的代码就清楚了，通过反射new一个AssetManager实例，然后调用addAssetPath方法把指定的路径传进入。至于Resouce就更好解决了，可以直接new，android framework关于Resouce是对开发者可见的，当然为什么AssetManager不能直接new？显然它是对开发者不可见的。到此资源操作这个棘手的问题解决了。顺便说一下，熟悉android源码对一个android开发者来说是非常重要的。一个android开发如果去转做java，他们只会把你当一个初学者。我们android开发者最大优势也就是对系统和sdk的熟悉程度了。当然，每一个程序员都有一个架构师的梦想。但是对android程序员，特别是手机开发的来说，我们也只能看别人设计架构了。

### PackageInfo的获取方法

PackageInfo也是借助系统，代码如下：

```
mInfo =           context.getPackageManager().getPackageArchiveInfo(mFilPath,
                        PackageManager.GET_CONFIGURATIONS);

```

这个接口可以通过插件apk的路径，解析出它的PackageInfo，可以去看一下sdk源码，PackageInfo就是对apk代码一种解析描述。

### DexClassLoader登场

到此PluginDescriptor的抽象和加载已经讲完了，下面就看怎么使用了，对于资源的使用没有什么好讲的，就和我们开发android程序一样，只不过获取AssetManager和Resource的句柄是通过PluginDescriptor得到的。DexClassLoader加载PackageInfo中的类需要看一下：

```
private Plugin doActivePlugin(String pluginId, PluginDescriptor pluginDescriptor) {
        TraceHelper.beginSection(">>>>PluginManager-doActivePlugin");
        Log.d(TAG, "<doActivePlugin> begin, pluginId " + pluginId);
        PluginClassLoader pluginClassLoader =
                new PluginClassLoader(pluginDescriptor.getArchivePath(), mDexDir,
                        mNativeLibDir, getClass().getClassLoader());
        pluginClassLoader.setRequiredClassLoader(getRequiredClassLoader(pluginDescriptor));
        try {
            Log.d(TAG, "<doActivePlugin> pluginDescriptor.className "
                    + pluginDescriptor.className);
            Class<?> pluginClass = pluginClassLoader.loadClass(pluginDescriptor.className);
            Constructor<?> pluginCons =
                    ReflectUtils.getConstructor(pluginClass, PluginDescriptor.class,
                            ClassLoader.class);
            Plugin plugin =
                    (Plugin) ReflectUtils.createInstance(pluginCons, pluginDescriptor,
                            pluginClassLoader);
            plugin.start();
            mRegistry.setPlugin(pluginId, plugin);
            TraceHelper.endSection();
            return plugin;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "<doActivePlugin> ClassNotFoundException, pluginId " + pluginId);
            e.printStackTrace();
            TraceHelper.endSection();
            return null;
        }
    }

```

其中pluginClassLoader就是继承DexClassLoader的，后面就是loadClass，反射调用相应的方法了，和上一次讲的ClassLoader原理，操作class是一样的。

### 第二次棘手问题解决方法

上面说了，DexClassLoader可以Load Activity,但是此时的Acivity就是一个普通类，Activity的生命周期如何解决。目前的解决方案是使用一个正常android Activity的生命周期去模拟插件中的Activity。就是启动插件中Activity,首先启动正常android Activity,然后调用插件中Activity的生命周期的方法,onCreate、onStart、onRestart、onResume、onPause、onStop、onDestroy等方法。

## 后记

具体代码可以参考mtk插件管理代码，上面说的都是一些核心思想，具体流程还是需要自己看代码去理的，mtk关于插件管理的代码写的还是不错的，至少有抽象，加载的时候也用了线程池，是有设计架构在里面的。不像github上的dynamic-load-apk,感觉就是为了演示核心思想，就像我们开发程序一样，知道了业务逻辑，撸起袖子就干，直奔主题。先实现了再说，毕竟老板催得急。

如果想深入学习插件开发，可以看看这篇文档：https://www.jianshu.com/p/353514d315a7





































