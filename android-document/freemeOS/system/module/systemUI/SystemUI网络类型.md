##SystemUI网络类型

###功能定义
网络类型的选择，打开该宏可以实现的功能是，单击下拉状态栏中的“手机网络图标”出现一个网络类型（2G/3G/4G）的选择对话框，可以选择网络类型，完成之后手机中的网络类型都会随之改变，如若没有打开则没有该功能。

###实现清单
1.开关行为值设置
[ProjectOption.ini]
device/droi/common/ProjectOption.ini

    [com.android.systemui]
    feature.cellular.network.switch = no

2.开关行为值读取
[QSTileHost.java]
frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/QSTileHost.java

```
    FreemeFeature.isLocalSupported("feature.cellular.network.switch")
```

### 备注
开关行为值的可选项：** no/0/false/n/off（关闭）、yes/1/true/y/on（打开）**
默认关闭该功能

```
    feature.cellular.network.switch = no
```

Debug调试时可以修改 **feature.cellular.network.switch** 的值，以便打开或关闭该功能。
