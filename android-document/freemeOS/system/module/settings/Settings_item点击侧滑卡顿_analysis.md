ViewDragHelper.java

```
    /**
     * Interpolator defining the animation curve for mScroller
     */
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            log 打印 t 值
            return t * t * t * t * t + 1.0f;
        }
    };
```

正常滑动 log 打印：

```
open 差值点 t 打印:

t:-0.9765625
t:-0.9453125
t:-0.849609375
t:-0.72265625
t:-0.564453125
t:-0.46875
t:-0.375
t:-0.248046875
t:-0.15234375


close 差值点 t 打印：

t:-0.978515625
t:-0.89453125
t:-0.806640625
t:-0.7109375
t:-0.615234375
t:-0.48828125
t:-0.361328125
t:-0.267578125
t:-0.171875
```

非正常打印（点击设置侧滑item） log 打印：

```
t:-0.96875
t:-0.873046875
```

特殊(通知栏设置item) 属正常滑动关闭 log 打印：

```
t:-0.990234375
t:-0.86328125
t:-0.736328125
t:-0.609375
```

分析

```
以上正常滑动和关闭滑动 无可比性(只执行了属性动画打开和关闭)
卡顿的 都打开了新的带侧滑栏的activity
特殊的 (通知栏设置) 并无侧滑栏


SettingsDrawerActivity.java
openTile()方法
activity启动
tile.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
startActivity（。。）
flag： FLAG_ACTIVITY_CLEAR_TASK
清除关联的task
```

验证

```
1.首先 阻塞 100ms 再开启activity 与之前相比 卡顿也相对延迟出现
2.再次 阻塞 200ms 操作同上       与之前相比 卡顿现象消失(其实就是把卡顿推迟了)

注： 阻塞是为了 让新的activity 延迟跳出来

差值器 log 打印：
t:-0.59765625
t:-0.501953125
```

结论

```
综合上面所有的信息
可以得出差值器打印的t个数 与 滑动速度有关
速度越慢 t的个数越多
相反 速度越快 t的个数越少

所以：
差值器的t打印个数并不是造成卡顿的直接原因

得出：
动画没执行完 当前activity的信息就被清除 新的activity就跳出来了
导致的视觉卡顿
```

建议

```
虽然 阻塞能把卡顿推迟 完全消除卡顿 但是 感觉并不能这么玩

openTile中
去掉closeDrawer（去除点击item的close动画效果）
注：展讯源码 也是这么修改的(参考)

或者 不修改
```

