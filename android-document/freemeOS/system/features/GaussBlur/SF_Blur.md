# 一、背景
##### 1.高斯模糊
###### 所谓"模糊"，可以理解成每一个像素都取周边像素的平均值，只是因为图像是连续的，越靠近的点关系越密切，所以也并不是简单的平均，而是采用一种加权平均的方式，而“高斯"函数正被用于计算模糊所需要的正态分布的权重矩阵，故称之为高斯模糊
##### 2.历史
###### Android2.3默认支持Blur Layer，但网传会造成黑屏问题，在4.0被废弃，后续的版本，RenderScript基本上已经完全替代了BlurLayer的地位
##### 3.主流模糊方式
###### A、RenderScript + AsyncTask，API 17 (4.2)以上支持，直接调用RenderScript包即可
###### B、FastBlur / Advanced FastBlur，缩小模糊，开源算法，不适合于大图，且效率低
###### C、Java/Native，Native效率优于Java

# 二、需求
###### 简化应用模糊场景使用，提供除RenderScript之外的平台级高斯模糊方案，力求在效率和动态帧数上有所突破

# 三、应用场景
##### 1.静态
##### A.Dialog
###### 类EMUI效果，关机菜单之下整个窗口进行模糊
##### B.Lock Screen
###### Freeme7.0新需求，下半屏局部模糊
##### C.Launcher
###### 类IOS效果，文件夹打开后，文件夹下的页面进行模糊
##### 2.动态
##### A.Camera
###### 类Samsung效果，Camera预览时菜单页滑动覆盖的部分进行模糊
##### B.SystemUI
###### 通知面板下拉的部分局部模糊

# 四、概要设计
###### 1.还原2.3 BLUR Layer，重新添加BlurLayer流程
###### 2.SurfaceFlinger中添加LayerBlur，LayerBlur中截图后进行模糊操作，然后将模糊过的纹理绘出

# 五、具体实现
##### 1.宏开关
###### FREEME_SF_BLUR_SUPPORT对应ro.freeme.sf.blur_support

##### 2.java侧
###### 与DimLayer不同的是，Blur添加setBlur方法，用于应用层对模糊因子的控制，其余与Dim一致

##### 3.SF侧
###### 流程上，除setBlur的方法贯通之外，与Dim无区别
##### LayerBlur的实现解析：
###### A、使用两个Texture，分别用于截屏和模糊
###### B、onDraw时，先进行截屏
###### C、将截屏纹理使用so库进行模糊，吐出模糊纹理
###### D、将模糊纹理使用OpenGL ES绘出
##### 4.使用
###### 给Window添加FLAG_BLUR_BEHIND，且设置blurAmount因子，0无变化，1最模糊

```
void setWindowStyle(Window window) {
    window.setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
    WindowManager.LayoutParams attrs = window.getAttributes();
    attrs.blurAmount = 0.5f;
    window.setAttributes(attrs);
}

```

# 六、验证
## 1.内存
在720P机器上验证，每次绘模糊层时，会多使用一块共享内存中的Buffer，BlurLayer为1280 X 720 X 4，大小约3.6M
Dump Log如下：
```
Hardware Composer state (version 01040000):
type   |  handle  | hint | flag | tr | blnd |   format    |     source crop (l,t,r,b)      |          frame         | name
  GLES | 00000000 | 0000 | 0001 | 00 | 0105 | ? ffffffff  |    0.0,    0.0,   -1.0,   -1.0 |    0,    0,  720, 1280 | BlurLayer

Allocated buffers:
0xb87db2b0:  140.62 KiB |  720 ( 720) x   50 |        1 | 0x00000b00
0xb87db368: 3680.00 KiB |  720 ( 736) x 1280 |        1 | 0x00001a00
【0xb88126b0: 3600.00 KiB |  720 ( 720) x 1280 |        2 | 0x00000b00】
0xb882fd58:  270.00 KiB |  720 ( 720) x   96 |        1 | 0x00000b00
0xb8830540:  270.00 KiB |  720 ( 720) x   96 |        1 | 0x00000b00
0xb8850af0:  270.00 KiB |  720 ( 720) x   96 |        1 | 0x00000b00
0xb8856ab0: 1609.69 KiB |  812 ( 816) x  505 |        1 | 0x00000b00
0xb8856d30: 3600.00 KiB |  720 ( 720) x 1280 |        1 | 0x00000900
0xb892a808: 3680.00 KiB |  720 ( 736) x 1280 |        1 | 0x00001a00
0xb893ad18: 3600.00 KiB |  720 ( 720) x 1280 |        1 | 0x00000b00
0xb89494e0: 1609.69 KiB |  812 ( 816) x  505 |        1 | 0x00000b00
0xb8965dc0:  140.62 KiB |  720 ( 720) x   50 |        1 | 0x00000b00
0xb897c108:  140.62 KiB |  720 ( 720) x   50 |        1 | 0x00000b00
0xb8982c70: 3600.00 KiB |  720 ( 720) x 1280 |        1 | 0x00000900
0xb8b114c8: 1609.69 KiB |  812 ( 816) x  505 |        1 | 0x00000b00
Total allocated (estimate): 27820.94 KB
```

## 2.性能
###### 使用Camera实时预览，进行动态模糊，观察秒表的延迟，目测大概再200ms左右

## 3.功耗
##### A.静态
###### 使用Global Action来测试，10组数据10分钟，平均电流和正常一样，不会导致增长

##### B.动态
###### a).播放视频，在视频上进行实时模糊，视频帧数30帧，测试数据发现，平均电流增长200+ mA

###### b).Camera预览，在预览上进行实时模糊，预览帧数20帧，测试数据发现，平均电流增长100 mA

###### c).上升的功耗中，截屏动作占比80%，模糊动作则相对较少

###### 测试过程为手机运行时测试，相对底电流测试来说，主观性较强，稍有偏差

# 七、遗留问题
###### 1. 导航栏背景被模糊

###### 2. Amount为0-0.4，0.7-1.0的模糊效果不好，会有轻微竖线产生

###### 3. 实现一个控件模型，考虑用Surfaceview实现，便于应用使用此控件来局部模糊

# 八、总结
##### 1、本功能开发，持续的时间很长，走的弯路较多，分为3个阶段
###### A.流程理解与贯通，添加BlurLayer的上下层框架

###### B.OpenGL绘图理解，最终决定使用CM方案

###### C.CM方案移植与验证

##### 2、过程中暴露的问题
###### A.前期评估不够到位，低估了OpenGL的专业复杂度

###### B.资源整合不够到位，未能及早摸清其他厂家的实现方式

###### C.知识深度不够到位，未能在每一个点上都有透彻的理解

##### 3.后续需要改正的地方
###### A.牵涉到相对专业知识领域的功能点，需更加谨慎评估，充分计入专业知识熟悉的时间长度

###### B.调查透彻其他厂家对当前功能的实现程度与方式

###### C.对牵涉到的知识点需要有个深度认识，不能浮于模糊的理解、报侥幸心理

###### D.难点问题及时抛出讨论，缩短工程周期


###### 文档持续更新，任何疑问和见解，欢迎大家随时提出讨论，感谢

###### 附录

Porting文档请见sf_blur_porting_guide.pdf
