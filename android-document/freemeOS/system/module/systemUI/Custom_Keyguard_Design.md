# 第三方锁屏实现设计

锁屏分为：普通锁屏和安全锁屏，第三方锁屏就是一个普通锁屏（与普通锁屏同等级）
第三方锁屏以插件（至于如何实现第三方的插件话，这是一个独立问题）的形式被加载到SystemUI进程中。如果无需功能实现，实质上他只是个view，被加载到SystemUI的视图树上。但，既然是锁屏，最起码你要有解锁的功能。而，解锁的权限肯定不会交给你一个第三方插件的，所以我们要设计一个相关接口提供插件使用。如下（只提供解锁）

```
public interface IKeyguardSercurityCallback {
    //只是为了简单说个过程，接口肯定不可能如此简单设计（稍微想想，解锁你有直接解锁进入，也有进入安全锁屏等等）
    void unlock();
}
```

然后我们再让view实现一个接口，通过接口里面的set方法传递IKeyguardSecurityCallback的具体实现类的对象。

```
public interface ICustomView {
    //实际上这个接口类可以设置很多限制，比如view是否允许弹出输入法（这样输入法的控制权就可以被下发到插件了）等等
    void setKeyguardSecurityCallback();
}
```

这样，一个解锁小闭环就设计好了。

ps：至于如何实现锁屏view的插件化，简单来说
他是一个独立的apk，通过shareuid和平台签名保证他和systemui运行在同一个进程。那么我们就可以通过apk包命获取其中的view资源了，实际上SystemUI也只想获取view！

OK
