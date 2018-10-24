### 1.Bug现象：
第一次打开talkback时，会进入talkback教程界面，点击“关闭 TALKBACK”按钮，会弹出“TalkBack已停止运

### 2.Bug日志：

```
02-01 00:02:33.460  5833  5833 E SpeechController: Attempted to speak before TTS was initialized.
02-01 00:02:33.463  5833  5833 D AndroidRuntime: Shutting down VM
--------- beginning of crash
02-01 00:02:33.464  5833  5833 E AndroidRuntime: FATAL EXCEPTION: main
02-01 00:02:33.464  5833  5833 E AndroidRuntime: Process: com.google.android.marvin.talkback, PID: 5833
02-01 00:02:33.464  5833  5833 E AndroidRuntime: java.lang.IllegalStateException: Activity has been destroyed
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.app.FragmentManagerImpl.enqueueAction(FragmentManager.java:1456)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.app.BackStackRecord.commitInternal(BackStackRecord.java:707)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.app.BackStackRecord.commit(BackStackRecord.java:671)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at com.android.talkback.tutorial.AccessibilityTutorialActivity.switchFragment(AccessibilityTutorialActivity.java:213)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at com.android.talkback.tutorial.AccessibilityTutorialActivity.showLesson(AccessibilityTutorialActivity.java:261)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at com.android.talkback.tutorial.AccessibilityTutorialActivity.access$100(AccessibilityTutorialActivity.java:44)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at com.android.talkback.tutorial.AccessibilityTutorialActivity$2.run(AccessibilityTutorialActivity.java:179)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.os.Handler.handleCallback(Handler.java:836)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.os.Handler.dispatchMessage(Handler.java:103)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.os.Looper.loop(Looper.java:203)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at android.app.ActivityThread.main(ActivityThread.java:6245)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at java.lang.reflect.Method.invoke(Native Method)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1071)
02-01 00:02:33.464  5833  5833 E AndroidRuntime:    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:932)
```

### 3.分析日志过程

1. 查看日志后，直接定位FragmentManager.java，试图从中找到一些原因，但是没有头绪
2. 打开百度，搜索“Activity has been destroyed”，查看相关帖子，依旧没有头绪
3. 再次仔细查看日志，但是焦点始终在“beginning of crash”之后的log，没有跳出思维狭隘区。
4. 在prife的指导下，开始关注到“SpeechController: Attempted to speak before TTS was initialized”，通过nexus 6p的log日志和我们日志的对比分析，发现6p在点击关闭按钮之后没有再次打印这句log，我们的手机在点击之后会立刻打印。怀疑是不是由于这个不同点导致了异常，焦点开始聚集到talkback本身。
5. 由于talkback属于GMS包，所以开始搜索和gms包相关的信息，在了解相应的基本概念之后，对gms包有个最基础的了解
6. 在搜索过程中，发现了talkback在github上面已经开源，于是clone代码到本地
7. 在调试了代码之后发现了问题产生的原因[bug_007671](http://bug.droi.com/mantis/view.php?id=7671)，通过分析知道在点击“关闭 TALKBACK”按钮之后，一个Runnable还在执行，但是此时Activity已经销毁。
8. 分析代码发现：

```
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
            mHandler.removeCallbacks(mRunnable);
            TalkBackService service = TalkBackService.getInstance();
            if (service != null) {
                service.postRemoveEventListener(this);
            }
        }
    }
```

于是猜测是不是在点击关闭按钮之后，这个方法会被回调，从而将remove掉这个Runnable，通过log，发现这个方法会被多次回调，但是都不满足if条件。

9. 这个时候我准备从view的点击事情跟起，看看当点击关闭按钮之后产生了哪些事件，是不是什么Event事件漏发了
10. 被prife制止，原因很明确，系统框架层代码太多，如大海捞针一般，并且没有一个正确的对比验证。
11. 回到正确路上，先找到一个正确的，并且能够调试的rom，然后在对比验证，于是从Google官网下载Nexus 6的rom（和华为 Nexus 6p版本相近），同时下载对应版本Branch的Android源代码
12. 再次调试talkback，发现nexus 6在回调onAccessibilityEvent的时候也不满足if条件，所以这个猜测over
13. 周末prife调试talkback代码之后，发现了在英文状态下会先播放语音，然后才会起Runnable，更进一步的找到了问题发生的原因
14. 周一在仔细调试代码之后发现，确实和系统播放语音有很大关系，并且把我们自己的手机语言设置成英文之后，也没有问题
15. 在talkback设置界面发现了“Google TTS”语音引擎，并且下载了中文语音包之后，再次打开talkback，系统播放中文语音，没有问题
16. 但是在nexus 6手机上面，设置成中文后，打开talkback，虽然没有播放语音，但是也没有崩溃，查看起语音引擎，为“Pico TTS”,于是猜测可能和语音引擎有关系，于是做交叉验证，发现"Pico TTS"语音引擎在我们手机上面确实也不会发生异常
17. 拿到公司对比机Nexus 6p，查看其talkback语音引擎，一样为"Google TTS"，但是Nexus 6P却没有发生异常，但是当打开talkback时，却能够播放中文语音，查看其语音列表，没有中文语音。并且Nexus 6p开始的语言为“中文（中国）”，切换成“简体中文（中国）”之后，在添加语言栏里面找不到“中文（中国）”这一项，怀疑Nexus 6p是经过OTA升级到Android N的，而Android N对多语言这块也是有新变化的
18. 清除Nexus 6p手机的数据，重启手机，设置“Google TTS”默认语言为“中文（中国）”，打开talkback，点击关闭按钮，弹出“talkback已停止运行”。

### 4.反思：
#### 第一种（有源码）：

- 我们仔细查看日志，和talkback相关的几句log里面都有“AccessibilityTutorialActivity”，顾名思义，这是一个Activity，而log里面明确显示了异常出现的原因：Activity has been destroyed。这个时候是不是可以思考下一个Activity会在什么情况下被destroy？第一种调用finish方法，第二种点击返回键，第三种在内存低的情况下，系统销毁了Activity。但是AccessibilityTutorialActivity处在前台显示，被系统销毁几乎不可能，我们也没有点击返回键，而是点击了“关闭按钮”，所以推测应该是调用了自身的finish方法。再次仔细查看log，发现前面有Handler.dispatchMessage，而Handler是跨线程，异步处理消息的，那么一个合理的推测就是当我们点击关闭按钮，Activity销毁了，但是Handler跨线程调用了showLesson方法，导致了问题的出现。
- 在我们拿到源码之后，带着之前的推测应该很快就能定位问题出现的原因：mHandler.postDelayed(mRunnable, AUTO_NAVIGATION_TIMEOUT)，仔细阅读代码，应该能发现要post这个线程是要满足一个case条件的：SpeechController.STATUS_SPOKEN，那么这个时候是不是应该先搞清楚在什么情况下会满足这个case，通过AS调试，设置断点，立刻就能发现问题所在，一个很快调用，一个要等待一段时间，而等待这段时间里，手机是一直不停的播放语音，稍微细心点，就能发现和语音有一点关系。
- 既然和语音有关系，在设置为英文时可以播放，而中文时没有声音，应该能猜测和语音库有关系，所以可以尝试摸索talkback里面和语音相关的信息。在talkback设置里面发现了语音引擎，在引擎设置里面有下载语音库选项，点击下载中文语音，测试没有问题，所以进一步验证了语音库的关系，对比Nexus 6P,虽然没有中文语音库，但是在打开talkback时，能播放中文语音，所以系统某个地方肯定存在语音库，并且在增加“简体中文（中国）”语言后，原本的“中文（中国）”找不到选项，推测可能是OTA升级，那么这个时候如果做对比验证，就需要清楚数据，保持变量相同。
- 经过这样的思路，应该能够较快的定位到问题的根本。
#### 第二种（无源码）：
- 如果拿到一个gms包没有源码的，这种情况下日志应该是尤其重要的，所以我们要对日志做全面细致的分析。如果talkback没有源码，那么我们在拿到log日志后，就应该重点分析关于talkback的几句log，Handler之后执行了AccessibilityTutorialActivity的内部类run方法，猜测可能是一个线程，然后调用了switchFragment方法，导致了crash。
- 那么在Activity中，调用switchFragment方法应该是用来切换不同Fragment来展示不同的页面，但是这里要注意的是Fragment和Activity之间的关系,Fragment是依赖于Activity的，如果Activity已经destroy了，Fragment也就无法显示了，官方介绍：https://developer.android.google.cn/reference/android/app/Fragment.html
- 在分析log之后，应该也是能大致看出，再点击“关闭”按钮之后，Activity销毁了，但是一个线程却要切换Fragment，导致了问题出现的原因。
- 但是这个时候不能确实是应用自身的问题还是系统的问题，那怎么办？正确做法应该是找出一个没有此问题的系统，当然系统尽可能的保持和出问题一致，这样控制单一变量，作对比，在对比中找出不同的地方，并在不同的地方分析log，或者不同的现象产生的原因。
- 如果对比的系统没有问题，可以做个交叉验证，将两个应用互换到不同的系统，记录是否会发生异常。

### 5.流程：
那么在遇到一个bug的时候，如果快速的定位问题？

1. 细致全面的分析日志
### 6.相关知识
1. 关于Handler的官网介绍：https://developer.android.google.cn/reference/android/os/Handler.html
2. csdn上面从源码角度介绍Handler：http://blog.csdn.net/lmj623565791/article/details/38377229/
3. 关于Activity的生命周期：https://developer.android.google.cn/guide/components/activities/activity-lifecycle.html
4. 关于AccssibilityService可以实现微信抢红包：http://blog.csdn.net/jwzhangjie/article/details/47205299
5. 简书上面对于AccessibilityService的介绍：http://www.jianshu.com/p/4cd8c109cdfb
