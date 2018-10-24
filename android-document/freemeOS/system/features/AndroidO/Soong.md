[toc]
# Soong

Soong是基于Android构建系统的替代品。它用Android.bp文件替换Android.mk文件，这些文件是类似JSON的简单声明性描述。

## Android.bp文件格式

按照设计，Android.bp文件非常简单。没有条件或控制流程语句 - 在Go中编写的构建逻辑中处理任何复杂性。在可能的情况下，Android.bp文件的语法和语义有意与Bazel BUILD文件类似。
 [Bazel BUILD files](https://www.bazel.io/versions/master/docs/be/overview.html)连接

### 模块

Android.bp文件中的模块以模块类型开头，后面是一组name: value,格式的属性：

```
cc_binary {
    name: "gzip",
    srcs: ["src/test/minigzip.c"],
    shared_libs: ["libz"],
    stl: "none",
}
```

每个模块都必须有一个name属性，并且该值在所有的Android.bp文件中必须是唯一的。

有关有效模块类型及其属性的列表，请参阅
[$OUT_DIR/soong/.bootstrap/docs/soong_build.html](https://go/Android.bp).

### 变量

一个Android.bp文件可能包含顶级变量赋值：

```
gzip_srcs = ["src/test/minigzip.c"],

cc_binary {
    name: "gzip",
    srcs: gzip_srcs,
    shared_libs: ["libz"],
    stl: "none",
}
```

变量被限定在声明文件的其余部分，以及任何子蓝图文件中。变量是不可变的，只有一个例外 - 它们可以附加到+ =赋值，但是只有在它们被引用之前。

### 注释
Android.bp文件可以包含C风格的多行/* */和C ++风格的单行//注释。

### 类型

变量和属性是强类型的，变量是基于第一个赋值而动态变化的，而属性由模块类型静态地赋值。支持的类型是:

* Bool (`true` or `false`)
* Integers (`int`)
* Strings (`"string"`)
* Lists of strings (`["string1", "string2"]`)
* Maps (`{key1: "value1", key2: ["value2"]}`)

Maps 可以是任何类型的值，list和maps可能在最后一个值之后有逗号。

### 操作符

字符串，字符串lists和Maps可以使用+操作符来连接。

整数求和通过`+`操作符

### 默认模块

默认模块可用于在多个模块中重复相同的属性。例如：

```
cc_defaults {
    name: "gzip_defaults",
    shared_libs: ["libz"],
    stl: "none",
}

cc_binary {
    name: "gzip",
    defaults: ["gzip_defaults"],
    srcs: ["src/test/minigzip.c"],
}
```

### 名称解析

只要每个模块在一个单独的名称空间中声明，Soong可以为不同目录中的模块指定相同的名称。命名空间可以这样声明：

```
soong_namespace {
    imports: ["path/to/otherNamespace1", "path/to/otherNamespace2"],
}
```

每个Soong模块都会根据其在树中的位置分配一个名称空间。除非找不到soong_namespace模块，否则每个Soong模块都被认为位于当前目录或最接近的祖先目录中的Android.bp中的soong_namespace所定义的名称空间中，在这种情况下，该模块被认为处于隐式根目录命名空间。

当Soong试图解决依赖关系时，D在声明命名空间N中声明我的模块M，它导入命名空间I1，I2，I3 ...，那么如果D是“//命名空间：模块”形式的完全限定名，那么只有指定的命名空间将搜索指定的模块名称。否则，宋将首先在名字空间N中寻找一个名为D的模块。如果该模块不存在，宋将在名字空间I1，I2，I3中寻找一个名为D的模块。最后，宋将查找根名字空间。

在我们完全转换成Make到Soong之前，Make产品配置需要指定一个值PRODUCT_SOONG_NAMESPACES。它的值应该是由m命令建立的Soong导出到Make的空格分隔的名字空间列表。在Make to Soong完全转换之后，启用命名空间的细节可能会发生变化。

### 格式化

Soong包括一个blueprint的规范格式，类似于[gofmt](https://golang.org/cmd/gofmt/).。要递归地重新格式化当前目录中的所有Android.bp文件：

```
bpfmt -w .
```

规范格式包括4个空格缩进，在多元素列表的每个元素之后都包含换行符，并且总是在list和map中包含尾随逗号。

### 转换Android.mk文件

Soong 包括一个工具执行第一过程转换Android.mk文件到Android.bp文件：

```
androidmk Android.mk > Android.bp
```

该工具转换变量，模块，注释和一些条件，但任何自定义的Makefile规则，复杂的条件或额外的包括必须手动转换。

#### Android.mk和Android.bp之间的区别

* Android.mk文件通常具有多个具有相同名称的模块（例如，静态和共享版本的库，或主机和设备版本）。Android.bp文件需要为每个模块使用唯一的名称，但是单个模块可以构建多个变体，例如添加host_supported: true。androidmk转换器将产生多个冲突的模块，这些模块必须手动解决到单个模块中，而模块内部有任何差异target: { android: { }, host: { } }。


## 建立逻辑

构建逻辑是使用Blueprint框架在Go中编写的。构建逻辑接收使用反射解析为Go结构的模块定义并生成构建规则。构建规则由Blueprint收集并写入到[ninja](http://ninja-build.org)构建文件中。



## 常问问题

### 我如何编写条件？

Soong 有意不支持Android.bp文件中的条件。相反，在Go中处理需要条件的构建规则的复杂性，其中可以使用高级语言特征，并且可以跟踪由条件引入的隐式依赖性。大多数条件都被转换为map属性，其中map中的一个值将被选中并附加到顶层属性。

例如，要支持体系结构特定的文件：

```
cc_library {
    ...
    srcs: ["generic.cpp"],
    arch: {
        arm: {
            srcs: ["arm.cpp"],
        },
        x86: {
            srcs: ["x86.cpp"],
        },
    },
}
```

有关产品变量或环境变量的更复杂条件的示例，请参阅[art/build/art.go](https://android.googlesource.com/platform/art/+/master/build/art.go)或[external/llvm/soong/llvm.go](https://android.googlesource.com/platform/external/llvm/+/master/soong/llvm.go)

## 参考

* [Android中的Ninja简介](http://note.qidong.name/2017/08/android-ninja/)

* [Android中的Kati](http://note.qidong.name/2017/08/android-kati/)

* [Android.mk的深入介绍](http://note.qidong.name/2017/08/android-mk/)

* [Android 6.0中的Makefile](http://note.qidong.name/2017/08/android-6.0-makefile/)

*  [Android编译系统中的Android.bp、Blueprint与Soong](http://note.qidong.name/2017/08/android-blueprint/)

* [Soong的详细解析](https://android.googlesource.com/platform/build/soong/)
