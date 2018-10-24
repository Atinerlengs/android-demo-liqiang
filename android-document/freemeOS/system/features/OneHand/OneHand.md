### 一、思路

#### 1、Add Window

##### A、OneHandBG

- 用于显示高斯模糊背景，且承载控制按钮

```
mBaseLayer=291000 mSubLayer=0 mAnimLayer=291000+0=291000 mLastLayer=291000
Frames: containing=[0,0][1080,1920] parent=[0,0][1080,1920]
    display=[-10000,-10000][10000,10000] overscan=[-10000,-10000][10000,10000]
    content=[0,0][1080,1920] visible=[0,0][1080,1920]
    decor=[0,0][1080,1920]
    outset=[-10000,-10000][10000,10000]
```

##### B、OneHandTouch

- 新增InputFilter，监听屏幕左右角滑动事件

```
mBaseLayer=251000 mSubLayer=0 mAnimLayer=251000+0=251000 mLastLayer=251000
Frames: containing=[0,0][1080,1920] parent=[0,0][1080,1920]
    display=[0,0][1080,1920] overscan=[0,0][1080,1920]
    content=[0,1710][1080,1920] visible=[0,1710][1080,1920]
    decor=[0,0][1080,1920]
    outset=[0,0][1080,1920]
```

#### 2、Change Magnification Spec

- Scale
- xOffset
- yOffset

```
mGlobalScale=1.0 mDsDx=0.74083924 mDtDx=0.0 mDsDy=0.0 mDtDy=0.74083924
```

#### 3、Filter Window's inputEvent

- 过滤OneHandBG透明区域事件，直接往下面的窗口分发
- 过滤OneHandTouch弃用的事件，直接往下面的窗口分发

```
if ((windowHandle->getName().find("OneHandBG") != -1)) {
    if (! (entry->flags & AMOTION_EVENT_FLAG_PREDISPATCH)) {
        // Pass through when click_point is in small single_hand window
        continue;
    }
} else if ((windowHandle->getName().find("OneHandTouch") != -1)) {
    if (entry->flags & AMOTION_EVENT_FLAG_PREDISPATCH) {
        // Pass through when click_point is bypassed by one hand touch window
        continue;
    }
}
```

### 二、代码改动

#### 1、FreemeOneHand（com.freeme.onehand）

##### services

 .OneHandService

##### receiver

 .EOHBroadcastReceiver

##### activity

.settings.OneHandSettingsActivity

#### 2、FrameWork

##### Base

##### Native

#### 3、Vendor/Freeme

### 三、遗留问题

##### 1、Screen Capture
截下来的是整屏的图，华为有修改为截小屏

##### 2、性能测试

- 内存
- CPU

##### 3、稳定性测试

### 四、代码记录
http://10.20.40.17:8080/#/q/topic:OneHand
