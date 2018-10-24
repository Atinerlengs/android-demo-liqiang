# Android/freemeos编译方法

## 便捷命令

大家知道，Android项目里，可以使用`mmm`命令命令进行模块编译。实际上Android还提供了其他不少命令，这些命令都定义在Android代码的`build/envsetup.sh`文件中。

这里简单介绍最常用的命令，完整命令请阅读`build/envsetup.sh`代码。要使用这些命令，首先需要在Android代码主目录下执行

```
$ cd ~/workplace/aosp/android6.0/
$ . ./build/envsetup.sh
```

### 路径切换
```
croot 快速切换到Android代码主目录
```

### 编译相关

```
mmm  编译模块
mmma  编译模块以及模块的所有依赖库
```
**技巧**：添加`showcommands`参数会打印完整的编译信息。如
```bash
mmm showcommands frameworks/base
```

### 代码检索

命令 | 功能
---|---
cgrep | 检索所有C/C++代码，文件后缀为c/cpp/h/hpp
jgrep | 检索所有java代码，文件后缀为java
resgrep| 检索所有xml文件
mangrep| 检索所有AndroidManifest.xml文件
ggrep | 检索所有gradle文件，后缀为gradle
sgrep | 检索所有代码文件，文件后缀为c/cpp/h/hpp/S/java/xml/sh/mk/aidl

以上这些命令都有类似的使用方法。譬如要搜索frameworks/base目录下所有调用`getService`的java代码：

```
$ cd frameworks/base
$ jgrep getService 
./libs/usb/src/com/android/future/usb/UsbManager.java:81:        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
./tools/layoutlib/bridge/src/com/android/layoutlib/bridge/android/BridgePackageManager.java:138:    public 
...
```

PS. 当需要手动搜索代码时，强烈建议大家使用上述命令而非手动grep搜索，因为这些命令对grep打开了颜色高亮、行号、递归目录，还会提过无关目录(如out/.git等目录）

## build错误

当修改了framework.jar的源代码（位于framework/base目录下），改动了API接口，并且其他模块（如 如service.jar，frameworks/base/services/中的代码）引用了frameworks中改动的API，直接使用mmm编译该模块会报错找不到新修改的符号错误，即使已经使用`mmm`重新编译了framework.jar。

解决方法是：删除`out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar`后，使用再对改用的模块使用`mmma`编译，即可编译成功。

原因解释：Android 5.0之后在编译系统引入了jack（google开发的java编译器），并且在Android6.0中启用，目前jack仍在开发中，build系统同时使用javac和jack。从实际测试来看，对`mmm frameworks/base`时会使用jack编译，不会更新`out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar`文件；而编译`mmm frameworks/base/services/`时则使用javac编译，因此依赖framework的classes.jar文件。