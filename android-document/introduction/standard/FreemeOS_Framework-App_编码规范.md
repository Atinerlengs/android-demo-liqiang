# FreemeOS Framework/App 编码规范

| 版次   | 修改日期       | 作者   | 修改内容                       |
| :--- | :--------- | :--- | :------------------------- |
| V0.1 | 2016.05.15 | 卞涛   | Init                       |
| V0.2 | 2016.05.16 | 卞涛   | 新增 SystemProperty约定        |
| V0.3 | 2016.05.19 | 卞涛   | 新增 配置宏命名                   |
| V0.4 | 2016.05.26 | 卞涛   | 新增 部分注释案例                  |
| V0.5 | 2016.05.31 | 卞涛   | 补充完善约定(2)和freeme-framework |
| V0.6 | 2017.08.31 | 卞涛   | 新增更多实例                     |

------

[TOC]

------

# (0) 概述

本文档是FreemeOS Framework开发规范的完整定义：

- 第(1)篇描述Java编程风格规范
- 第(2)篇描述FreemeOS工程实践规范

本文档如有约定与《Android编码规范》冲突，请以此为准。

------

# (1) Java编程风格规范

## (1)1 前言
本篇是FreemeOS Java编程风格规范的完整定义。当且仅当一个Java源文件符合此文档中的规则，我们才认为它符合FreemeOS的Java编程风格。

与其它的编程风格指南一样，这里所讨论的不仅仅是编码格式美不美观的问题，同时也讨论一些约定及编码标准。然而，这份文档主要侧重于我们所普遍遵循的规则，对于那些不是明确强制要求的，我们尽量避免提供意见。

### 1.1 术语说明
在本文档中，除非另有说明：

1. 术语class可表示一个普通类，枚举类，接口或是annotation类型(`@interface`)
2. 术语comment只用来指代实现的注释(implementation comments)，我们不使用“documentation comments”一词，而是用Javadoc。

其他的术语说明会偶尔在后面的文档出现。

### 1.2 规范说明
本文档中的示例代码并不作为规范。也就是说，虽然示例代码是遵循FreemeOS编程风格，但并不意味着这是展现这些代码的唯一方式。示例中的格式选择不应该被强制定为规则。

## (1)2 源文件基础
### 2.1 文件名
源文件以其最顶层的类名来命名，大小写敏感，文件扩展名为`.java`。

### 2.2 文件编码：UTF-8
源文件编码格式为UTF-8。

### 2.3 特殊字符
#### 2.3.1 空白字符
除了行结束符序列，ASCII水平空格字符(0x20，即空格)是源文件中唯一允许出现的空白字符，这意味着：
1. 所有其它字符串中的空白字符都要进行转义。
2. 制表符不用于缩进。

#### 2.3.2 特殊转义序列
对于具有特殊转义序列的任何字符(\b, \t, \n, \f, \r, ", '及\\)，我们使用它的转义序列，而不是相应的八进制(比如`\012`)或Unicode(比如`\u000a`)转义。

#### 2.3.3 非ASCII字符
对于剩余的非ASCII字符，是使用实际的Unicode字符(比如∞)，还是使用等价的Unicode转义符(比如\u221e)，取决于哪个能让代码更易于阅读和理解。

> Tip: 在使用Unicode转义符或是一些实际的Unicode字符时，建议做些注释给出解释，这有助于别人阅读和理解。

例如：

``` java
String unitAbbrev = "μs";                                 | 赞，即使没有注释也非常清晰
String unitAbbrev = "\u03bcs"; // "μs"                    | 允许，但没有理由要这样做
String unitAbbrev = "\u03bcs"; // Greek letter mu, "s"    | 允许，但这样做显得笨拙还容易出错
String unitAbbrev = "\u03bcs";                            | 很糟，读者根本看不出这是什么
return '\ufeff' + content; // byte order mark             | Good，对于非打印字符，使用转义，并在必要时写上注释
```

> Tip: 永远不要由于害怕某些程序可能无法正确处理非ASCII字符而让你的代码可读性变差。当程序无法正确处理非ASCII字符时，它自然无法正确运行，你就会去fix这些问题的了。(言下之意就是大胆去用非ASCII字符，如果真的有需要的话)

## (1)3 源文件结构
一个源文件包含(按顺序地)：

1. 许可证或版权信息(如有需要)
2. package语句
3. import语句
4. 一个顶级类(**只有一个**)

以上每个部分之间用一个空行隔开。

### 3.1 许可证或版权信息
如果一个文件包含许可证或版权信息，那么它应当被放在文件最前面。

### 3.2 package语句
package语句不换行，列限制(4.4节)并不适用于package语句。(即package语句写在一行里)

### 3.3 import语句
#### 3.3.1 import不要使用通配符
即，不要出现类似这样的import语句：`import java.util.*;`

#### 3.3.2 不要换行
import语句不换行，列限制(4.4节)并不适用于import语句。(每个import语句独立成行)

#### 3.3.3 顺序和间距
import语句可分为以下几组，按照这个顺序，每组由一个空行分隔：

1. 所有的静态导入独立成组
2. 第三方的包。每个顶级包为一组，字典序。例如：android, com, junit, org, sun
3. `java` imports
4. `javax` imports

组内不空行，按字典序排列。

### 3.4 类声明
#### 3.4.1 只有一个顶级类声明
每个顶级类都在一个与它同名的源文件中(当然，还包含`.java`后缀)。

例外：`package-info.java`，该文件中可没有`package-info`类。

#### 3.4.2 类成员顺序
类的成员顺序对易学性有很大的影响，但这也不存在唯一的通用法则。不同的类对成员的排序可能是不同的。最重要的一点，每个类应该以某种逻辑去排序它的成员，维护者应该要能解释这种排序逻辑。比如，新的方法不能总是习惯性地添加到类的结尾，因为这样就是按时间顺序而非某种逻辑来排序的。

##### **3.4.2.1 重载：永不分离**
当一个类有多个构造函数，或是多个同名方法，这些函数/方法应该按顺序出现在一起，中间不要放进其它函数/方法。

## (1)4 格式
**术语说明**：块状结构(block-like construct)指的是一个类，方法或构造函数的主体。需要注意的是，数组初始化中的初始值可被选择性地视为块状结构(4.8.3.1节)。

### 4.1 大括号
#### 4.1.1 使用大括号(即使是可选的)
大括号与`if, else, for, do, while`语句一起使用，即使只有一条语句(或是空)，也应该把大括号写上。

#### 4.1.2 非空块：K&R 风格
对于非空块和块状结构，大括号遵循Kernighan和Ritchie风格 (Egyptian brackets):

- 左大括号前不换行
- 左大括号后换行
- 右大括号前换行
- 如果右大括号是一个语句、函数体或类的终止，则右大括号后换行; 否则不换行。例如，如果右大括号后面是else或逗号，则不换行。

示例：

``` java
return new MyClass() {
    @Override public void method() {
        if (condition()) {
            try {
                something();
            } catch (ProblemException e) {
                recover();
            }
        }
    }
};
```

4.8.1节给出了enum类的一些例外。

#### 4.1.3 空块：可以用简洁版本
一个空的块状结构里什么也不包含，大括号可以简洁地写成{}，不需要换行。例外：如果它是一个多块语句的一部分(if/else 或 try/catch/finally) ，即使大括号内没内容，右大括号也要换行。

示例：

``` java
void doNothing() {}
```

### 4.2 块缩进：4个空格
每当开始一个新的块，缩进增加4个空格，当块结束时，缩进返回先前的缩进级别。缩进级别适用于代码和注释。(见4.1.2节中的代码示例)

### 4.3 一行一个语句
每个语句后要换行。

### 4.4 列限制：80或100
一个项目可以选择一行80个字符或100个字符的列限制，除了下述例外，任何一行如果超过这个字符数限制，必须自动换行。

例外：

1. 不可能满足列限制的行(例如，Javadoc中的一个长URL，或是一个长的JSNI方法参考)。
2. `package`和`import`语句(见3.2节和3.3节)。
3. 注释中那些可能被剪切并粘贴到shell中的命令行。

### 4.5 自动换行
**术语说明**：一般情况下，一行长代码为了避免超出列限制(80或100个字符)而被分为多行，我们称之为自动换行(line-wrapping)。

我们并没有全面，确定性的准则来决定在每一种情况下如何自动换行。很多时候，对于同一段代码会有好几种有效的自动换行方式。

> Tip: 提取方法或局部变量可以在不换行的情况下解决代码过长的问题(是合理缩短命名长度吧)

#### 4.5.1 从哪里断开
自动换行的基本准则是：更倾向于在更高的语法级别处断开。

1. 如果在`非赋值运算符`处断开，那么在该符号前断开(比如+，它将位于下一行)。注意：这一点与其它语言的编程风格不同(如C++和JavaScript)。这条规则也适用于以下“类运算符”符号：点分隔符(.)，类型界限中的&（`<T extends Foo & Bar>`)，catch块中的管道符号(`catch (FooException | BarException e`)
2. 如果在`赋值运算符`处断开，通常的做法是在该符号后断开(比如=，它与前面的内容留在同一行)。这条规则也适用于`foreach`语句中的分号。
3. 方法名或构造函数名与左括号留在同一行。
4. 逗号(,)与其前面的内容留在同一行。

#### 4.5.2 自动换行时缩进至少+4个空格
自动换行时，第一行后的每一行至少比第一行多缩进4个空格(注意：制表符不用于缩进。见2.3.1节)。

当存在连续自动换行时，缩进可能会多缩进不只4个空格(语法元素存在多级时)。一般而言，两个连续行使用相同的缩进当且仅当它们开始于同级语法元素。

第4.6.3水平对齐一节中指出，不鼓励使用可变数目的空格来对齐前面行的符号。

### 4.6 空白
#### 4.6.1 垂直空白
以下情况需要使用一个空行：

1. 类内连续的成员之间：字段，构造函数，方法，嵌套类，静态初始化块，实例初始化块。 
    - **例外**：两个连续字段之间的空行是可选的，用于字段的空行主要用来对字段进行逻辑分组。
2. 在函数体内，语句的逻辑分组间使用空行。
3. 类内的第一个成员前或最后一个成员后的空行是可选的(既不鼓励也不反对这样做，视个人喜好而定)。
4. 要满足本文档中其他节的空行要求(比如3.3节：import语句)

多个连续的空行是允许的，但没有必要这样做(我们也不鼓励这样做)。

#### 4.6.2 水平空白
除了语言需求和其它规则，并且除了文字，注释和Javadoc用到单个空格，单个ASCII空格也出现在以下几个地方：

1. 分隔任何保留字与紧随其后的左括号(`(`)(如`if, for catch`等)。
2. 分隔任何保留字与其前面的右大括号(`}`)(如`else, catch`)。
3. 在任何左大括号前(`{`)，两个例外： 
    - `@SomeAnnotation({a, b})`(不使用空格)。
    - `String[][] x = foo;`(大括号间没有空格，见下面的Note)。
4. 在任何二元或三元运算符的两侧。这也适用于以下“类运算符”符号： 
    - 类型界限中的&(`<T extends Foo & Bar>`)。
    - catch块中的管道符号(`catch (FooException | BarException e`)。
    - `foreach`语句中的分号。
5. 在`, : ;`及右括号(`)`)后
6. 如果在一条语句后做注释，则双斜杠(//)两边都要空格。这里可以允许多个空格，但没有必要。
7. 类型和变量之间：List list。
8. 数组初始化中，大括号内的空格是可选的，即`new int[] {5, 6}`和`new int[] { 5, 6 }`都是可以的。

> **Note**：这个规则并不要求或禁止一行的开关或结尾需要额外的空格，只对内部空格做要求。

#### 4.6.3 水平对齐：不做要求
**术语说明**：水平对齐指的是通过增加可变数量的空格来使某一行的字符与上一行的相应字符对齐。

这是允许的(而且在不少地方可以看到这样的代码)，但Google编程风格对此不做要求。即使对于已经使用水平对齐的代码，我们也不需要去保持这种风格。

以下示例先展示未对齐的代码，然后是对齐的代码：

``` java
private int x; // this is fine
private Color color; // this too

private int   x;      // permitted, but future edits
private Color color;  // may leave it unaligned
```

> Tip：对齐可增加代码可读性，但它为日后的维护带来问题。考虑未来某个时候，我们需要修改一堆对齐的代码中的一行。这可能导致原本很漂亮的对齐代码变得错位。很可能它会提示你调整周围代码的空白来使这一堆代码重新水平对齐(比如程序员想保持这种水平对齐的风格)，这就会让你做许多的无用功，增加了**reviewer**的工作并且可能导致更多的合并冲突。

### 4.7 用小括号来限定组：推荐
除非作者和reviewer都认为去掉小括号也不会使代码被误解，或是去掉小括号能让代码更易于阅读，否则我们不应该去掉小括号。我们没有理由假设读者能记住整个Java运算符优先级表。

### 4.8 具体结构
#### 4.8.1 枚举类
枚举常量间用逗号隔开，换行可选。

没有方法和文档的枚举类可写成数组初始化的格式：

``` java
private enum Suit { CLUBS, HEARTS, SPADES, DIAMONDS }
```

由于枚举类也是一个类，因此所有适用于其它类的格式规则也适用于枚举类。

#### 4.8.2 变量声明
##### 4.8.2.1 每次只声明一个变量
不要使用组合声明，比如`int a, b;`。

##### 4.8.2.2 需要时才声明，并尽快进行初始化
不要在一个代码块的开头把局部变量一次性都声明了(这是c语言的做法)，而是在第一次需要使用它时才声明。局部变量在声明时最好就进行初始化，或者声明后尽快进行初始化。

#### 4.8.3 数组
##### 4.8.3.1 数组初始化：可写成块状结构
数组初始化可以写成块状结构，比如，下面的写法都是OK的：

``` java
new int[] {
    0, 1, 2, 3
}

new int[] {
    0,
    1,
    2,
    3
}

new int[] {
    0, 1,
    2, 3
}

new int[]{0, 1, 2, 3}
```

##### 4.8.3.2 非C风格的数组声明
中括号是类型的一部分：`String[] args`， 而非`String args[]`。

#### 4.8.4 switch语句
**术语说明**：switch块的大括号内是一个或多个语句组。每个语句组包含一个或多个switch标签(`case FOO:`或`default:`)，后面跟着一条或多条语句。

##### 4.8.4.1 缩进
与其它块状结构一致，switch块中的内容缩进为2个空格。

每个switch标签后新起一行，再缩进2个空格，写下一条或多条语句。

##### 4.8.4.2 Fall-through：注释
在一个switch块内，每个语句组要么通过`break, continue, return`或抛出异常来终止，要么通过一条注释来说明程序将继续执行到下一个语句组，任何能表达这个意思的注释都是OK的(典型的是用`// fall through`)。这个特殊的注释并不需要在最后一个语句组(一般是`default`)中出现。示例：

``` java
switch (input) {
    case 1:
    case 2:
        prepareOneOrTwo();
        // fall through
    case 3:
        handleOneTwoOrThree();
        break;
    default:
        handleLargeNumber(input);
}
```

##### 4.8.4.3 default的情况要写出来
每个switch语句都包含一个`default`语句组，即使它什么代码也不包含。

#### 4.8.5 注解(Annotations)
注解紧跟在文档块后面，应用于类、方法和构造函数，一个注解独占一行。这些换行不属于自动换行(第4.5节，自动换行)，因此缩进级别不变。例如：

``` java
@Override
@Nullable
public String getNameIfPresent() { ... }
```

**例外**：单个的注解可以和签名的第一行出现在同一行。例如：

``` java
@Override public int hashCode() { ... }
```

应用于字段的注解紧随文档块出现，应用于字段的多个注解允许与字段出现在同一行。例如：

``` java
@Partial @Mock DataLoader loader;
```

参数和局部变量注解没有特定规则。

#### 4.8.6 注释
#####4.8.6.1 块注释风格
块注释与其周围的代码在同一缩进级别。它们可以是`/* ... */`风格，也可以是`// ...`风格。对于多行的`/* ... */`注释，后续行必须从`*`开始，并且与前一行的`*`对齐。以下示例注释都是OK的。

``` java
/*
 * This is          // And so           /* Or you can
 * okay.            // is this.          * even do this. */
 */
```

注释不要封闭在由星号或其它字符绘制的框架里。

> Tip：在写多行注释时，如果你希望在必要时能重新换行(即注释像段落风格一样)，那么使用`/* ... */`。

#### 4.8.7 Modifiers
类和成员的modifiers如果存在，则按Java语言规范中推荐的顺序出现。

``` java
public protected private abstract static final transient volatile synchronized native strictfp
```

## (1)5 命名约定
### 5.1 对所有标识符都通用的规则
标识符只能使用ASCII字母和数字，因此每个有效的标识符名称都能匹配正则表达式`\w+`。

在其它编程语言风格中使用的特殊前缀或后缀，如`name_`, `mName`, `s_name`和`kName`，在Java编程风格中都不再使用。

### 5.2 标识符类型的规则
#### 5.2.1 包名
包名全部小写，连续的单词只是简单地连接起来，不使用下划线。

#### 5.2.2 类名
类名都以`UpperCamelCase`风格编写。

类名通常是名词或名词短语，接口名称有时可能是形容词或形容词短语。现在还没有特定的规则或行之有效的约定来命名注解类型。

测试类的命名以它要测试的类的名称开始，以`Test`结束。例如，`HashTest`或`HashIntegrationTest`。

#### 5.2.3 方法名
方法名都以`lowerCamelCase`风格编写。

方法名通常是动词或动词短语。

下划线可能出现在JUnit测试方法名称中用以分隔名称的逻辑组件。一个典型的模式是：`test<MethodUnderTest>_<state>`，例如`testPop_emptyStack`。并不存在唯一正确的方式来命名测试方法。

#### 5.2.4 常量名
常量名命名模式为`CONSTANT_CASE`，全部字母大写，用下划线分隔单词。那，到底什么算是一个常量？

每个常量都是一个静态final字段，但不是所有静态final字段都是常量。在决定一个字段是否是一个常量时，考虑它是否真的感觉像是一个常量。例如，如果任何一个该实例的观测状态是可变的，则它几乎肯定不会是一个常量。只是永远不`打算`改变对象一般是不够的，它要真的一直不变才能将它示为常量。

``` java
// Constants
static final int NUMBER = 5;
static final ImmutableList<String> NAMES = ImmutableList.of("Ed", "Ann");
static final Joiner COMMA_JOINER = Joiner.on(',');  // because Joiner is immutable
static final SomeMutableType[] EMPTY_ARRAY = {};
enum SomeEnum { ENUM_CONSTANT }

// Not constants
static String nonFinal = "non-final";
final String nonStatic = "non-static";
static final Set<String> mutableCollection = new HashSet<String>();
static final ImmutableSet<SomeMutableType> mutableElements = ImmutableSet.of(mutable);
static final Logger logger = Logger.getLogger(MyClass.getName());
static final String[] nonEmptyArray = {"these", "can", "change"};
```

这些名字通常是名词或名词短语。

#### 5.2.5 非常量字段名
非常量字段名以`mUpperCamelCase`风格编写。

这些名字通常是名词或名词短语。

#### 5.2.6 参数名
参数名以`mUpperCamelCase`风格编写。

参数应该避免用单个字符命名。

#### 5.2.7 局部变量名
局部变量名以`lowerCamelCase`风格编写，比起其它类型的名称，局部变量名可以有更为宽松的缩写。

虽然缩写更宽松，但还是要避免用单字符进行命名，除了临时变量和循环变量。

即使局部变量是final和不可改变的，也不应该把它示为常量，自然也不能用常量的规则去命名它。

#### 5.2.8 类型变量名
类型变量可用以下两种风格之一进行命名：

- 单个的大写字母，后面可以跟一个数字(如：E, T, X, T2)。
- 以类命名方式(5.2.2节)，后面加个大写的T(如：RequestT, FooBarT)。

### 5.3 驼峰式命名法(CamelCase)
驼峰式命名法分大驼峰式命名法(`UpperCamelCase`)和小驼峰式命名法(`lowerCamelCase`)。有时，我们有不只一种合理的方式将一个英语词组转换成驼峰形式，如缩略语或不寻常的结构(例如”IPv6”或”iOS”)。Google指定了以下的转换方案。

名字从`散文形式`(prose form)开始:

1. 把短语转换为纯ASCII码，并且移除任何单引号。例如：”Müller’s algorithm”将变成”Muellers algorithm”。
2. 把这个结果切分成单词，在空格或其它标点符号(通常是连字符)处分割开。 
    - 推荐：如果某个单词已经有了常用的驼峰表示形式，按它的组成将它分割开(如”AdWords”将分割成”ad words”)。 需要注意的是”iOS”并不是一个真正的驼峰表示形式，因此该推荐对它并不适用。
3. 现在将所有字母都小写(包括缩写)，然后将单词的第一个字母大写： 
    - 每个单词的第一个字母都大写，来得到大驼峰式命名。
    - 除了第一个单词，每个单词的第一个字母都大写，来得到小驼峰式命名。
4. 最后将所有的单词连接起来得到一个标识符。

示例：

``` java
Prose form                Correct               Incorrect
------------------------------------------------------------------
"XML HTTP request"        XmlHttpRequest        XMLHTTPRequest
"new customer ID"         newCustomerId         newCustomerID
"inner stopwatch"         innerStopwatch        innerStopWatch
"supports IPv6 on iOS?"   supportsIpv6OnIos     supportsIPv6OnIOS
"YouTube importer"        YouTubeImporter
                          YoutubeImporter*
```

加星号处表示可以，但不推荐。

> **Note**：在英语中，某些带有连字符的单词形式不唯一。例如：”nonempty”和”non-empty”都是正确的，因此方法名`checkNonempty`和`checkNonEmpty`也都是正确的。

## (1)6 编程实践
### 6.1 @Override：能用则用
只要是合法的，就把`@Override`注解给用上。

### 6.2 捕获的异常：不能忽视
除了下面的例子，对捕获的异常不做响应是极少正确的。(典型的响应方式是打印日志，或者如果它被认为是不可能的，则把它当作一个`AssertionError`重新抛出。)

如果它确实是不需要在catch块中做任何响应，需要做注释加以说明(如下面的例子)。

``` java
try {
    int i = Integer.parseInt(response);
    return handleNumericResponse(i);
} catch (NumberFormatException ok) {
    // it's not numeric; that's fine, just continue
}
return handleTextResponse(response);
```

**例外**：在测试中，如果一个捕获的异常被命名为`expected`，则它可以被不加注释地忽略。下面是一种非常常见的情形，用以确保所测试的方法会抛出一个期望中的异常，因此在这里就没有必要加注释。

``` java
try {
    emptyStack.pop();
    fail();
} catch (NoSuchElementException expected) {
}
```

### 6.3 静态成员：使用类进行调用
使用类名调用静态的类成员，而不是具体某个对象或表达式。

``` java
Foo aFoo = ...;
Foo.aStaticMethod(); // good
aFoo.aStaticMethod(); // bad
somethingThatYieldsAFoo().aStaticMethod(); // very bad
```

### 6.4 Finalizers: 禁用
极少会去重写`Object.finalize`。

> Tip：不要使用finalize。如果你非要使用它，请先仔细阅读和理解Effective Java 第7条款：“Avoid Finalizers”，然后不要使用它。

## (1)7 Javadoc
### 7.1 格式
#### 7.1.1 一般形式
Javadoc块的基本格式如下所示：

``` java
/**
 * Multiple lines of Javadoc text are written here,
 * wrapped normally...
 */
public int method(String p1) { ... }
```

或者是以下单行形式：

``` java
/** An especially short bit of Javadoc. */
```

基本格式总是OK的。当整个Javadoc块能容纳于一行时(且没有Javadoc标记@XXX)，可以使用单行形式。

#### 7.1.2 段落
空行(即，只包含最左侧星号的行)会出现在段落之间和Javadoc标记(@XXX)之前(如果有的话)。除了第一个段落，每个段落第一个单词前都有标签`<p>`，并且它和第一个单词间没有空格。

#### 7.1.3 Javadoc标记
标准的Javadoc标记按以下顺序出现：`@param`, `@return`,` @throws`, `@deprecated`, 前面这4种标记如果出现，描述都不能为空。当描述无法在一行中容纳，连续行需要至少再缩进4个空格。

### 7.2 摘要片段
每个类或成员的Javadoc以一个简短的摘要片段开始。这个片段是非常重要的，在某些情况下，它是唯一出现的文本，比如在类和方法索引中。

这只是一个小片段，可以是一个名词短语或动词短语，但不是一个完整的句子。它不会以`A {@code Foo} is a...`或`This method returns...`开头, 它也不会是一个完整的祈使句，如`Save the record...`。然而，由于开头大写及被加了标点，它看起来就像是个完整的句子。

> Tip：一个常见的错误是把简单的Javadoc写成`/** @return the customer ID */`，这是不正确的。它应该写成`/** Returns the customer ID. */`。

### 7.3 哪里需要使用Javadoc
至少在每个public类及它的每个public和protected成员处使用Javadoc，以下是一些例外：

#### 7.3.1 例外：不言自明的方法
对于简单明显的方法如`getFoo`，Javadoc是可选的(即，是可以不写的)。这种情况下除了写“Returns the foo”，确实也没有什么值得写了。

单元测试类中的测试方法可能是不言自明的最常见例子了，我们通常可以从这些方法的描述性命名中知道它是干什么的，因此不需要额外的文档说明。

> Tip：如果有一些相关信息是需要读者了解的，那么以上的例外不应作为忽视这些信息的理由。例如，对于方法名`getCanonicalName`，就不应该忽视文档说明，因为读者很可能不知道词语`canonical name`指的是什么。

#### 7.3.2 例外：重写
如果一个方法重写了超类中的方法，那么Javadoc并非必需的。

#### 7.3.3 可选的Javadoc
对于包外不可见的类和方法，如有需要，也是要使用Javadoc的。如果一个注释是用来定义一个类，方法，字段的整体目的或行为，那么这个注释应该写成Javadoc，这样更统一更友好。

-----

# (2) FreemeOS工程实践规范

## (2)1 前言
本篇是FreemeOS在工程实施中一些规范。

> **Note:**  
>
> 1. *(1)*节的各种约定，须严格执行，但不必拘泥——新场景/文件中按照此约定，但原生场景下，请遵循**上下文**约定。
> 2. *(1)*节的各种约定，如无专项规范，同样(可选择性/可参考性)适用于其他编程语言/工程：
>     - 缩进，可适用于C/C++/Makefile/Shell/Python等；
>     - 大括号/风格，不强制适用于C/C++/Makefile等；
>     - 不一而足；

## (2)2 源码(Code)
### 2.1 缩进
使用**4个空格（Space）**替代**1个制表符（Tab）**。
相关IDE/编辑器环境中皆有偏好设置。

### 2.2 文件
#### 2.2.1 文件命名
代码(`java, c/cpp`等)文件的命名除按照各自代码规范约束外，FreemeOS新添加的文件须使用`Freeme/freeme_`前缀：

``` markdown
FreemeFingerprint.java
IFreemeFingetCallback.aidl
FreemeInputDispatcher.cpp
freeme_lights.c
```

#### 2.2.2 文件组织
对于FreemeOS专有功能/模块，*如可以*须将相关代码文件按照一定策略集中放置。

``` markdown
# 例举：
# 集中放置FreemeOS自有的公共泛化控件
com.freeme.widget;
# 集中放置FreemeOS自有的手势相关组件
com.freeme.internal.kinect;
# 集中放置FreemeOS自有的一些系统服务组件
com.freeme.server;
```

如相关源码文件放置在FreemeOS自有包(`package`)内，**按需**使用上节约定的`Freeme/freeme_`前缀：

``` markdown
# 目录：vendor/freeme/frameworks/
# 何谓“按需”？即可以不用，但如果FreemeOS自建的文件（特别是Java类），如果与原生系统中的Api类名冲突，在引用时出现麻烦，还是需要前缀“Freeme/freeme_”。
```

### 2.3 SystemProperty命名
Android SystemProperty命名约定：**`<prefix>.freeme.<module>_<feature>`**。

*案例：*

``` markdown
# 一般需求
ro.freeme.camera_eis             | Camera-EIS
persist.freeme.pmirror_disables  | 应用分身-不适用列表
ro.freeme.factory_forestrobe     | 工厂模式-前闪光灯
# 特殊需求(遵从系统约定)
ro.build.ota.product             | 系统更新版本适配属性
ro.config.message_sound          | 短信声音路径
```

> Tips:
>
> - 命名串长度限制为31，灵活缩减各字段长度；

### 2.4 控制宏命名
约定：**FREEME\_<module>\_<feature>**。

``` markdown
FREEME_FACTORYMODE_SUPPORT    | 工厂模式App模块
FREEME_FACTORYMODE_FORESTROBE | 工厂模式App内特性-前闪光灯
FREEME_FACTORYMODE_GSENSOR    | 工厂模式App内特性-加速度传感器
```

### 2.5 Android API生成约束
对于上述新添加的源码中涉及到Javadoc的内容(java)部分，须使用`@hide`进行隐藏。

``` markdown
# 到底哪些涉及到呢？
#   framework/base/[core|media|*]等中涉及新Java API导出(修饰符为public,protected等)。
```

*案例：*

``` java
// Sample 1
/** @hide */
public class Xxx { }

// Sample 2
public class Context {
    ...
    
    /** @hide */
    protected int mXxx;
    /** @hide */
    public void xxx() { }
    /** @hide */
    public class Xxx {
    }
}
```

### 2.6 变量命名约定
除基本遵从 *(1)5 命名约定* 节外，在既有的源代码中，请遵循**上下文**的约定。

### 2.7 注释
除了`Javadoc`类注释需要块注释（`/* */`），全部使用行注释（`//...`）。

在既有系统源码文件中修改和插入新代码时，须使用特定注释和标签：

- 原则：保留原始实现（**代码行**），凸显**最小**修改变动。
- **标签：`freeme.<name>, <date(yyyyMMdd)>. <purpose>`** 。
- 注释：结合既有源码/编程语言，灵活使用块/行注释

``` java
// Java/C/C++

// Style 1
/*/
[既有代码]
//*/

// Style 2
//*/ <标签>
[插入新]
//*/

// Style 3
/*/ <标签>
[修改前(维持原有代码实现)]
/*/
[修改后]
//*/

// Style 4
//*/ <标签>
[修改后]
/*/
[修改前(维持原有代码实现)]
//*/
```

``` markdown
# 探讨：为什么要使用以上注释形式？
```

*案例：*

``` java
// Java
public class StatusBarNotification {
    ...
    
    public StatusBarNotification(Parcel in) {
        ...
        //*/ freeme.biantao, 20151222. ProjectMirror.
        if (!android.util.ProjectMirrorConfig.WITHOUT) {
        	this.instanceId = in.readInt();
        }
        //*/
        ...
    }
    
    private String groupKey() {
        final String group = getNotification().getGroup();
        final String sortKey = getNotification().getSortKey();
        if (group == null && sortKey == null) {
            // a group of one
            return key;
        }
        return user.getIdentifier() + "|" + pkg + "|" +
                (group == null
                        ? "p:" + notification.priority
                        //*/ freeme.biantao, 20151222. ProjectMirror.
                        : "g:" + group) + "|" + instanceId;
                        /*/
                        : "g:" + group);
                        //*/
    }
  
    @Override
    public StatusBarNotification clone() {
        return new StatusBarNotification(this.pkg, this.opPkg,
                this.id, this.tag, this.uid, this.initialPid,
                //*/ freeme.biantao, 20151222. ProjectMirror.
                this.score, this.notification.clone(), this.user, this.postTime, this.instanceId);
                /*/
                this.score, this.notification.clone(), this.user, this.postTime);
                //*/
    }

    //*/ freeme.biantao, 20151222. ProjectMirror.
    private int instanceId;
  
    /** @hide */
    public int instanceIdSample;
    //*/
}
```

> Tips：既有源码中遇到有块注释(`/*...*/`)的情形，在设计修改代码时，尽量考虑需变动代码的设计形式，最小化修改。极端情况，可使用行注释进行实施。

``` xml
<!-- XML -->
<application ...>
    ...
    
    <activity android:name="com.android.internal.app.ResolverActivity"
        android:theme="@style/Theme.DeviceDefault.Resolver"
        android:finishOnCloseSystemDialogs="true"
        android:excludeFromRecents="true"
        android:documentLaunchMode="never"
        android:relinquishTaskIdentity="true"
        android:process=":ui" >
  
        <!-- Sample 1.1: 移除 -->
        <!-- freeme.biantao, 20151230. ProjectMirror.
        <action android:name="android.intent.action.LAUNCHER" />
        -->
  
        <!-- Sample 1.2: 新增 -->
        <!-- freeme.biantao, 20160526. ProjectMirror. -->
        <action android:name="com.freeme.intent.action.LAUNCHER" />
        <!-- 或者 -->
        <!-- @{ freeme.biantao, 20160526. ProjectMirror. -->
        <action android:name="com.freeme.intent.action.LAUNCHER" />
        <!-- @} -->
       
        <!-- Sample 1.3: 修改 -->
        <!-- @{ freeme.biantao, 20160531. ProjectMirror.
        <category android:name="android.intent.category.HOME" />
        -->
        <category android:name="com.freeme.intent.category.HOME" />
        <!-- @} -->
    </activity>
    
    <!-- Sample 2: 新增 -->
    <!-- @{ freeme.biantao, 20151230. ProjectMirror. -->
    <activity android:name="com.android.internal.app.ProjectMirrorResolverActivity"
        android:theme="@style/Theme.DeviceDefault.Resolver"
        android:finishOnCloseSystemDialogs="true"
        android:excludeFromRecents="true"
        android:documentLaunchMode="never"
        android:relinquishTaskIdentity="true"
        android:process=":ui" >
    </activity>
    <!-- @} -->
    <!-- 或者 -->
    <!-- freeme.biantao, 20160526. Topic 1 -->
    <service android:name=".Sample1Service" />
    <!-- 自然空行隔开 -->
    <!-- freeme.biantao, 20160526. Topic 2 -->
    <service android:name=".Sample2Service" />
    <activity android:name=".Sample2Activity" />
    
    <!-- 分割线 -->
    
    <!-- Sample 3: 移除 -->
    <!-- freeme.biantao, 20151230. ProjectMirror.
    <activity android:name="com.android.internal.app.ProjectMirrorResolverActivityAlias"
        android:theme="@style/Theme.DeviceDefault.Resolver"
        android:finishOnCloseSystemDialogs="true"
        android:excludeFromRecents="true"
        android:documentLaunchMode="never"
        android:relinquishTaskIdentity="true"
        android:process=":ui" >
    </activity>
    -->
</application>
```

> Tips：如`xml`文件修改过多，可以考虑：
>
> 1. 将原始内容完全注释掉，另行添加新实现；
> 2. 另行添加新文件（`freeme_`前缀）进行置换；

``` makefile
# makefile

# Sample 1: 新增
# @{ freeme.biantao, 20160328. ProjectMirror.
LOCAL_SHARED_LIBRARIES += \
    libproject_mirror
# @}

# Sample 2: 移除(整体)
# freeme.biantao, 20160328. ProjectMirror.
#LOCAL_JAVA_LIBRARIES := \
    framework \
    base-telephony \
    freeme-framework

# Sample 3: 移除(部分)
# freeme.biantao, 20160526. Useless.
#   base-telephony
LOCAL_JAVA_LIBRARIES := \
    framework \
    freeme-framework

# Sample 4: 移除(整体)
# freeme.biantao, 20170831. Useless.
#ifeq ($(strip $(PRODUCT_BUILD_TARGET_SAMPLE)),true)
#    LOCAL_STATIC_JAVA_LIBRARIES += android-common-sample
#endif

# Sample 5: 新增
# $(1): output file
define build-systemimage-target
  @# freeme.jack, 20141009. LicenseVerity.
  $(call generate-system-verity-fingerprint)
  @#
  @echo "Target system fs image: $(1)"
  ...
endef
```

``` python
# Python
# Sample 1: 移除
''' freeme.biantao, 20160526. Ota.
install_from_ota()
'''

# Sample 2: 替换
# @{ freeme.biantao, 20160526. Ota.
'''
install_from_ota()
'''
install_from_ota_new()
# @}

# Sample 3: 新增
# @{ freeme.biantao, 20160526. Ota.
install_from_ota_new()
# @}
```

``` java
// Java
// 特殊情况
void killAppAtUsersRequest(ProcessRecord app, Dialog fromDialog) {
    synchronized (this) {
        app.crashing = false;
        app.crashingReport = null;
        app.notResponding = false;
        app.notRespondingReport = null;
        if (app.anrDialog == fromDialog) {
            app.anrDialog = null;
        }
        if (app.waitDialog == fromDialog) {
            app.waitDialog = null;
        }
        // 修改前：
        if (app.pid > 0 && app.pid != MY_PID) {
            handleAppCrashLocked(app, "user-terminated" /*reason*/,
                    null /*shortMsg*/, null /*longMsg*/, null /*stackTrace*/);
            app.kill("user request after error", true);
        }
        // 修改后：
        /*/
        if (app.pid > 0 && app.pid != MY_PID) {
            handleAppCrashLocked(app, "user-terminated" /*reason* /,
                    null /*shortMsg* /, null /*longMsg* /, null /*stackTrace* /);
            app.kill("user request after error", true);
        }
        /*/
        if (app.pid > 0 && app.pid != MY_PID && app.pid != 1000) {
            app.kill("system default request after error", true);
        }
        //*/
    }
}
```

> **Note:** 
>
> - 约定适用于Java/C/C++/Python/Makefile/Python/Shell/Xml/**sepolicy**等，灵活适用；
> - 约定只限定于修改原生工程源码。如修改FreemeOS内部开发/修改的源码，可以忽略此约定——直接在原有代码上进行修改，如果需要可添加__行注释__和__标签__说明缘由。

### 2.8 内容组织
只要是新增的内容，如无顺序需求，在同级中尽量集中放置在文件/类尾部。

*案例：*

``` java
public class ActivityManagerService {
    ...

    //*/ freeme.biantao, 20160531. ProjectMirror.
    private static final String TAG_AM = "ActivityManagerServer-PM";

    public static final int FUNC_AM = 1;

    private Handler mPMHander = new Handler();

    private int mInstanceId;

    public int getInstanceId() {
        return mInstanceId;
    }
    //*/
}
```

### 2.9 废弃源码
对于`FreemeOS`废弃源码，无论是否本人所为，遇到后都应该主动确认，然后移除。

### 2.10 Makefile
`Android.mk`中禁止包含`PRODUCT_COPY_FILES`，当用`BUILD_PREBUILT`预构建。

### 2.11 freeme-framework

``` markdown
# freeme-framework Map
vendor/freeme/frameworks/base
  ├ core                 [产物freeme-framework.jar]
  ├ core-export          [合入framework.jar中编译]
  ├ services/core-export [合入services.core.jar中编译]
  └ data
      ├ bootanimation    [开机动画]
      ├ fonts            [字体]
      ├ presets          [内置多媒体资源]
      ├ wallpapers       [壁纸]
      ├ prebuilts        [预置的应用、等预构建目标]
      └ sounds           [声音]
# Q: 为什么会有core和core-export，差别在哪？
# A: 一句话，编译循环引用导致。core独立提供给'app/services/etc'使用，core-export被'framework'引用。
```

## (2)3 资源(Resources)
### 3.1 文件命名
资源(`layout, drawable, color, xml`等)文件的命名必须以小写字母开头，只包含小写字母[a-z]、数字[0-9]以及下划线[\_]。单词间使用下划线连接，单词尽量使用符合上下文的有意义名词。

``` markdown
drawable/ic_launcher.png
layout/activity_main.xml
color/tab_indicator_text.xml
xml/wifi_settings.xml
```

如该文件为某个功能/控件独占，当使用同义前缀：

``` markdown
progress_text_color.xml
progress_state_background.xml
progress_layout.xml
```

### 3.2 标识符命名
标识符(`id, color, dimen, integer, style`等)使用小写字母开头，只包含字母[a-z]、数字[0-9]以及下划线[\_]。使用符合上下文有意义的单词，并以下划线连接。

``` markdown
id/btn_version_text
dimen/progress_title_text_size
color/progress_title_text_color
```

如该标识符为某个功能/控件独占，当使用同义前缀：

``` markdown
progress_title_text
progress_title_text_color
progress_title_text_size
```

### 3.3 命名前缀
为提高新添加内容的可移植性和可维护性，**在既有源生资源环境中**，新加入的文件/标识符须使用`freeme_`前缀（但对于诸如`[strings/themes/symbols/styles/configs/ids/dimens/colors/publics/...].xml`须使用`_freeme`后缀）：

``` markdown
values/strings_freeme.xml
values/themes_freeme.xml
values/styles_freeme.xml
values/colors_freeme.xml
values/dimens_freeme.xml

layout/freeme_progress.xml
drawable/freeme_ic_launcher.png

string/freeme_network_err
id/freeme_icon
color/freeme_primary_text_size
```

### 3.4 Android SDK生成约束
对于上述新添加的资源文件/标识符，如需在`Java`代码中作为符号引用，请使用非公开方式，在`sysmbols_freeme.xml`中添加。

### 3.5 废弃资源
对于`FreemeOS`废弃资源，无论是否本人所为，遇到后都应该主动确认，然后移除。

-----

# (3) 后记

## (3)1 备注
1. 前文规范中，凡有关使用“freeme/Freeme”组织名，仅部分强制适用于`FreemeOS`组织内部。其他工作人员请使用“droi/Droi”组织名。
2. 前文规范前提下，我等工程师当怀“为团队服务为先”之心、弃“闭门造车”之行。

## (3)2 其他
编辑器（`markdown`）：`Typora`
