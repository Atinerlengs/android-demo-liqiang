# 一、功能描述
###### 1、Android自带可滚动界面的Edge Effect，但效果一直不尽如人意，故提出对List View的效果美化
###### 2、当列表处于上下边缘拉伸状态时，加入弹性和缩放效果，取代边缘的半弧状动画
# 二、概要描述
### 1、功能点
##### A、上下边缘
##### B、弹性动画
##### C、画布缩放
### 2、注入点
##### A、onTouchEvent
###### a) TouchDown 信息记录，标记开始
###### b) TouchMove 设置相应的Scale值
###### c) TouchUp or TouchCancel 状态重置
##### B、dispatchDraw
###### a) 注入Canvas处理，如果必要，使用Scale进行缩放，并开始弹性动画
# 三、详细实现
### 1、Add AbsListViewInjector
#### A、描述
###### 注入类，嵌入到AbsListView和ListView中，处理Canvas
#### B、动画
###### a) BackEase
倒退缓冲动画
###### b) CircEase
环状缓冲动画，在倒退缓冲动画之前播放
#### C、Public method
###### a) getPanSpeed
获取弹性(惯性)的速度
###### b) initOnTouchDown
记录初始点击状态
###### c) setListScaleIfNeeded
设置缩放因子
###### d) needFinishActionMode
判别是否结束Action mode
###### e) resetScale
重置缩放因子
#### D、Private method
###### a) edgeReached
是否到达边缘
###### b) isAnimating
是否正在做动画
###### c) isSpringOverscrollEnabled
是否开启该Feature
###### d) setListScale
设置缩放因子
### 2、使用AbsListViewInjector注入touch信息标记
#### A、AbsListView onTouchEvent
###### a) ACTION_POINTER_DOWN 标记DownY
###### b) ACTION_DOWN 初始化Down信息
###### c) ACTION_MOVE 获取弹性速度, 并设置缩放因子
###### d) ACTION_UP 重置所有状态
###### e) ACTION_CANCEL 重置所有状态
### 3、使用AbsListViewInjector注入Canvas处理
#### A、ListView dispatchDraw
###### Draw Divider之前，预先对canvas进行Scale，以达到分割线与内容的效果同步
#### B、AbsListView dispatchDraw
###### Draw Child之前，预先对canvas进行Scale，使每一个子项具有缩放和动画效果
### 4、对MultiChoiceModeWrapper特殊处理
###### 如果当前应用使用弹性效果的ListView，则忽略选中项清零的行为，不退出Action Mode
# 四、遗留问题
### 1、效率问题
###### 多次频繁滑动，会出现卡顿现象，正在尝试优化动画和缩放注入后的频繁刷新
### 2、使用开关
###### 根据应用的主题，判断是否使用具有弹性效果的List View，此点在主题成熟时，修改加入
# 五、总结
###### 1、重点在于对触屏滑动和控件绘图的理解
###### 2、难点在于对绘图效率的优化

###### 附录
Porting文档请见ListView_Scrollable_OverScroll_porting_guide.pdf
