## 1.现象

- 通知面板中，QQ消息显示一条黑条，无任何内容显示
- 长按此消息，会弹出QQ扩展选项

## 2.分析

- 视图树上看，QQ此条通知的Expand视图结构正常，含自己的扩展Layout(NotificationContentView)，但是其中的ImageView和Textview均布局为width 0，无内容显示
- 反编译QQ apk，发现其中含有qapp_center_notification.xml，代码如下

```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@id/notification_root" android:layout_width="fill_parent"
    android:layout_height="fill_parent" android:gravity="center_vertical" android:padding="2.0dip">
    <ImageView android:id="@id/notification_icon" android:layout_width="36.0dip"
        android:layout_height="36.0dip" android:layout_marginLeft="5.0dip"
        android:scaleType="centerInside" />
    <LinearLayout android:id="@id/name_APKTOOL_DUPLICATENAME_0x7f0a0aac"
        android:layout_width="fill_parent" android:layout_height="fill_parent"
        android:layout_marginLeft="10.0dip" android:gravity="center_vertical"
        android:orientation="vertical">
        <RelativeLayout android:layout_width="fill_parent" android:layout_height="wrap_content"
            android:gravity="center_vertical" android:paddingRight="5.0dip">
            <TextView android:id="@id/notification_title" android:layout_width="wrap_content"
                android:layout_height="wrap_content" android:layout_alignParentLeft="true"
                android:layout_centerVertical="true" android:ellipsize="marquee"
                android:fadingEdge="horizontal" />
            <TextView android:id="@id/notification_progress" android:layout_width="wrap_content"
                android:layout_height="wrap_content" android:layout_alignParentRight="true"
                android:layout_centerVertical="true" android:ellipsize="marquee"
                android:fadingEdge="horizontal" android:singleLine="true" />
        </RelativeLayout>
        <TextView android:id="@id/notification_content" android:layout_width="wrap_content"
            android:layout_height="wrap_content" android:ellipsize="marquee"
            android:fadingEdge="horizontal" android:gravity="center_vertical"
            android:singleLine="true" />
        <LinearLayout android:id="@id/notif_pro_bar_layout" android:layout_width="fill_parent"
            android:layout_height="wrap_content" android:layout_marginTop="5.0dip"
            android:paddingRight="5.0dip" android:visibility="gone">
            <ProgressBar android:id="@id/notif_pro_bar" style="@style/progress_horizontal"
                android:layout_width="fill_parent" android:layout_height="5.0dip"
                android:layout_gravity="center_vertical" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- 代码结构所显示的布局和视图树一致
- 断点调试发现，该条通知的解析正常，所Load的视图也正常
- QQ本身未发任何Action出来
- 外面问过，还没做Android N，未遇到这个问题

## 继续分析

- 查看Google源码更新补丁
- 查为毛QQ不去更新这几个textview
- MTK提个Case？
