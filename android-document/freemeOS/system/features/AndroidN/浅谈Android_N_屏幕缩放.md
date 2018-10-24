[TOC]

# 一、浅谈Android N 屏幕缩放
### 1、官方描述的Android N 屏幕缩放
- Android 7.0 支持用户设置显示尺寸，以放大或缩小屏幕上的所有元素，从而提升设备对视力不佳用户的可访问性。

### 2、还是官方，特别强调了dp
- 避免用像素单位指定尺寸，因为像素不会随屏幕密度缩放。应改为使用与密度无关像素 (dp) 单位指定尺寸。

### 3、我，老生常谈下dp
#### 屏幕密度，官方是这么说的
- 屏幕物理区域中的像素量；通常称为 dpi（每英寸 点数）。例如， 与“正常”或“高”密度屏幕相比，“低”密度屏幕在给定物理区域的像素较少。
为简便起见，Android 将所有屏幕密度分组为六种通用密度： 低、中、高、超高、超超高和超超超高。

#### 对比下Windows和Android

##### A、Windows
用了这么多年的Windows，大家都知道，分辨率是可调的，分辨率越高，图标显示越小，反之，越大

为毛？

因为Windows的应用是用像素来设计的，分辨率大了，自然所需要显示的空间就小了

##### B、Android
目前为止，Android只有一个固定的分辨率，也不能像Windows一样调来调去，以至于我们在N之前，只会觉得分辨率高的机器，清晰些，分辨率低的机器，模糊些，有木有

然而到了N，问题来了，可以缩放了，怎么缩放呢？

举个栗子，QHD(960 X 540)的机器，只有三级缩放，默认当然是标准的、我们写在SystemProp中的240dpi；往下调一级，屏幕图标变小了，往上调一级，图标变大了，发生了什么？

好吧，调来调去调的是Density，往下，Density变小，比如240 X 0.85 = 204，反之，240 X 1.15 = 276，变大

#### 那么，这乱七八糟的对比说明了什么
楼下Android Density的大小，是和图标的大小正向对应的，而楼上Windows哥哥的分辨率是和图标的大小反向对应的，千万别被Windows带沟里去了

### 4、对了，还特别强调了SW320dp
用户无法将屏幕缩放至低于最小屏幕宽度 sw320dp，该宽度是 Nexus 4 的宽度，也是常规中等大小手机的宽度。

为毛要强调？

原来不支持动态缩放的时候，SW < 320的机器基本已经绝种了，但是现在可调了，Google老人家估计是操心，调着调着又调到320下面去了

还是举个栗子

还是那个QHD的机器，设置240dpi，当然没问题，540 / (240 / 160) 还有360dpi，咱不怕，但是要是手贱调成了271dpi，计算器算下，问题来了，变成308了，< 320了，完蛋了，应用要适配不了了，测试要提BUG了

### 5、说点正紧的，官方兼容性文档是这么说的

Device implementations are STRONGLY RECOMMENDED to provide users a setting to change the display size. If there is an implementation to change the display size of the device, it MUST align with the AOSP implementation as indicated below:

- The display size MUST NOT be scaled any larger than 1.5 times the native density or produce an effective minimum screen dimension smaller than 320dp (equivalent to resource qualifier sw320dp), whichever comes first.
- Display size MUST NOT be scaled any smaller than 0.85 times the native density.
- To ensure good usability and consistent font sizes, it is RECOMMENDED that the following scaling of Native Display options be provided (while complying with the limits specified above)
- Small: 0.85x
- Default: 1x (Native display scale)
- Large: 1.15x
- Larger: 1.3x
- Largest 1.45x

### 6、总结一下
调可以，别超过1.5，别小于0.85，更别让屏幕最小宽度小于320dp，不然不让你过CTS

# 二、参考资料
https://developer.android.com/about/versions/nougat/android-7.0-changes.html

android-7.0-cdd.pdf
