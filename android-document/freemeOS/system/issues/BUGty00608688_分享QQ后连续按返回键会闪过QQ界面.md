1.必现路径

QQ应用存活的条件下，进入搜索界面，再进入网页内容界面，最后，连续点击返回键3次

2.Window Stack如下：

Window #5 Window{7520cb7 u0 com.freeme.widget.newspage/com.freeme.widget.newspage.browser.WebViewActivity}:
mBaseLayer=21000 mSubLayer=0 mAnimLayer=21025+0=21025 mLastLayer=21025

Window #4 Window{ca891c3 u0 com.freeme.widget.newspage/com.freeme.widget.newspage.SearchActivity}:
mBaseLayer=21000 mSubLayer=0 mAnimLayer=21020+0=21020 mLastLayer=21025

Window #3 Window{ed2c770 u0 com.tencent.mobileqq/com.tencent.mobileqq.activity.SplashActivity}:
mBaseLayer=21000 mSubLayer=0 mAnimLayer=21015+0=21015 mLastLayer=22005

Window #1 Window{39e6752 u0 com.freeme.home/com.freeme.home.Launcher}:
mBaseLayer=21000 mSubLayer=0 mAnimLayer=21005+0=21005 mLastLayer=21010

3.WebViewActivity和SearchActivity的Launcher-mode均为SingleInstance

4.从Log分析来看，有几个与AMS相关的点需要澄清，如下：

A、WebViewActivity早于SearchActivity进入Finsh, 但是走入OnDestroy的时间却晚于后者，即从activity record中移除的时间也晚于后者

B、WebViewActivity没有立即从ActivityRecord中移除，导致了SplashActivity被误认为是FocusWindow，而被抬起

C、理想情况是，adjustFocusedActivityLocked时，满足条件（r.frontOfTask && task == topTask() && task.isOverHomeStack())，回到Launcher, 但是正如B中所说，当Task为SearchActivity时，WebViewActivity仍然处于top Task，task != topTask()，无法进入此条件

请帮忙从AMS角度澄清一下，多谢