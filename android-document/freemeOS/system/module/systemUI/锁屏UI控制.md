此文档只介绍锁屏UI的控制，以介绍其中涉及的主要类的介绍来展开。
* KeyguardService

  SystemUI对Framenwork层提供的锁屏服务，该服务提供的方法就是写对Keyguard UI的操作

* KeyguardViewMediator

  此类就是一个中转站，分发处理各种与keyguard相关的事件（他不实际处理，真正做事的主要是StatusBarKeyguardViewManager.java）

  那么到底有多少种Keyguard相关事件呢？查看mHandler

* StatusBarKeyguardViewManager

  Keyguard的show，hide都是在此类中实现

Keyguard相关的主要控制类就以上几个，具体的view上的设计，视图上很清楚。

至于，一次解锁过程是怎样的，打出方法调用栈一看便知。

OK
