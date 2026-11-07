# qml4j 使用约束与方案修正（阶段 0/1 实测结论）

> 环境：Windows 11 / Zulu JDK 21.0.8 / Gradle 8.10.2 / qml4j-core v0.2.38（JitPack）
> Skija 0.143.17（skija-windows-x64）/ LWJGL 3.3.3 / JUnit 5.10.2
> 实测显卡 NVIDIA RTX 4060，OpenGL 3.2 core context 正常。

本文记录**已经跑通验证**的事实，以及它们对原实施计划的修正。
详细 API 取证见 [qml4j-api.md](./qml4j-api.md)（每条结论都附源码行号或 jar 字节码依据）。

## 1. 引擎本质：qml4j 不创建窗口

`QmlView` 只是把 QML 渲染进一块 Skia surface，**没有文件 API**：

```java
QmlView view = QmlView.withStockTypes(new QmlEngine());
view.resources(loader)            // 单方法接口 byte[] load(String)
    .context("theme", bridge)     // 必须在 load 之前
    .load(qmlSourceText);         // 参数是「源码文本」，不是路径
// 自己开窗 + 自己驱动：
view.renderFrame(surfaceBackend);
```

因此 **GLFW 窗口、OpenGL context、事件循环、帧循环都属于宿主**（`selkie.host`）。
原计划的 `QmlView.withStockTypes(...).load("qml/main.qml")` 不足以弹出窗口。

**硬约束：一个 `QmlView` 只能配一个 `QmlEngine`**，两个 view 共用一个 engine 会
`LinkageError: duplicate class definition Component$0`。

## 2. Java → QML：只认 public 字段

| 项目 | 事实 |
|---|---|
| 可读成员 | **只有 public 字段**（实现走 `Class.getField`）。getter、record 访问器、bean 反射**全部不可用** |
| 响应式 | `public Property<T>` 字段自动解包，且 `Property.set()` 会触发依赖重算——这是**唯一**的变更通知机制 |
| 变更监听 | **没有** `PropertyChangeListener`、没有 `notify()`、没有注解路径 |
| 方法调用 | public 方法可被 QML 调用，按**精确参数个数**匹配（无重载排序） |
| 信号 | `public final Signal` 字段，双向可连（`Connections { target: x; function onFoo() {} }`） |
| 只读 | `field.setInterceptor(...)`；宿主写入用 `setBypassInterceptor()` |
| 注入点 | `view.context(name, obj)`，**必须在 `load()` 之前** |

> **对原计划的影响**：`ThemeBridge` / `PluginBridge` / `TokenBridge` 不能用
> record 或 getter 暴露数据，必须用 `public final Property<T>` 字段。
> 另外，QML 改了主题状态宿主无法感知，所以「切换深色」要通过
> `setDark(boolean)` 这类**方法**由 QML 调用，宿主动态重算并写回 Property。

## 3. 单例：两种都支持

1. `pragma Singleton` + `qmldir`：`singleton Theme 1.0 Theme.qml`，由 `ResourceLoader` 提供；
2. Java 类注册为 QML 单例类型：`TypeRegistry.registerSingleton(name, cls)` +
   手写 `public static Object __instance()`。

单例是 **JVM 级（整个应用一份）**，不是每窗口一份；每窗口独立状态请用 `context(name, obj)`。

## 4. 动效：**没有 SpringAnimation** ⚠️

qml4j v0.2.38 的动画类共 21 个：Property/Number/Color/Rotation/Opacity/Parallel/
Sequential/Pause/ScriptAction/Behavior/State/PropertyChanges/Transition/Timer 等。
**没有 `SpringAnimation`**（实测报 `unknown QML type: SpringAnimation`），
也没有 `SmoothedAnimation` / `PathAnimation`。缓动表**不完整**：只有
Quad / Cubic / Quart / Sine / Back，Elastic / Bounce / Expo / Circ 会**退化成线性**。

可动画对象 = 任何 `public Property` 字段（包括我们自己的 Java 桥字段）。

> **对原计划的修正**：阶段 2 的「开关和标签页切换必须用 `SpringAnimation`」
> 在本引擎上无法实现。可行路线二选一：
> 1. **宿主驱动弹簧**：Java 侧用 `MotionTokens` 的 dampingRatio/stiffness 积分，
>    逐帧 `Property.set()`（qplayer 的 `SpringAnim` 就是这么做）；
> 2. **降级近似**：`Behavior` + `OutBack` 缓动，能做出类似 overshoot 的观感，
>    但不是真弹簧（不可中断、不自适应）。
>
> 推荐 1（保真且能复用阶段 1 的 MotionTokens）；2 只作为过渡。

## 5. 控件层缺失

共 **69 个内置类型**。有：anchors（除 baseline）、Row/Column/Flow、
Row/Column/Grid/StackLayout + 完整 `Layout.*`、Repeater、ListView/GridView
（模型可直接吃 `java.util.List<POJO>`，delegate 读 `modelData.field`）、
MouseArea、Flickable、Text、Canvas、Image、Rectangle、Gradient、阴影/发光效果。

**没有**：`Grid` 定位器，以及整个 Controls 控件层——
Popup / Dialog / Menu / ScrollView / SplitView / Page / StackView / TableView /
BorderImage / AnimatedImage / ShaderEffect。

> **对原计划的影响**：`M3Toast`、`M3Tabs`、下拉菜单等必须用 Rectangle+MouseArea+
> Behavior 自己搭（qplayer 也是把 MD3 控件全部写成 .qml）；滚动只能用 `Flickable`。

## 6. 线程模型

create / context / load / tickAnimations / renderFrame / dispatch / dispose
**必须在同一个线程**（GL 渲染线程）。跨线程 `Property.set()` 是真实数据竞争
（`Property.changeVersion` 与 `Property.evaluating` 是全局的，只有 DirtyQueue 是
ThreadLocal）。`QmlEngine` 本身可以不绑定线程。
宿主桥接若要从工作线程更新数据，必须用「任务队列 + 帧内排空」的方式。

## 7. 字体由宿主提供

qml4j 不做系统字体查找，需要宿主喂字节：
`QmlView.uiTypefaces(byte[] regular, byte[] medium)` + `iconTypeface(byte[])`。
中文界面需要宿主打包一套可商用的中文字体（如 Noto Sans SC / 思源黑体）。

## 8. Java 21 兼容性：无阻碍

qml4j 字节码 major 52（Java 8），build `--release 8`，ASM 只写 V1_8 类；
200 个源文件里零 `setAccessible` / `Unsafe` / `sun.*` / `--add-opens`；
类定义走合法的 `ClassLoader` 子类。Zulu 21.0.8 下 29/30 项检查通过。

## 9. Native Image（阶段 6）**高风险，需改路线**

qml4j 的核心机制是**运行时**把 QML 编译成字节码再 `ClassLoader.defineClass`
（`engine/classloader/JvmClassLoaderBackend$DynamicClassLoader`、`ClassLoaderBackend`、
`compiler/bytecode/QmlCompiler`）。而 GraalVM Native Image 是封闭世界假设，
**运行期定义新类不可用**，所以「直接上 native-image」这条路走不通。

参考项目 QPlayer 的做法证实了这一点：它确实发布 Native Image 可执行文件，
但 **QML 在构建期由 `qml-aot-tool` 预编译**成 scene/类，其 AGENTS.md 明确写
"Production QML is compiled by qml-aot-tool at build time … do not rely on
runtime compilation"。该工具依赖 qml4j 的包内 API（甚至把类放进
`io.github.timer_err.qml4j.render` 包以访问包私有成员），属于**非公开接口**，
版本升级易碎。

**结论 / 建议**：
- 阶段 6 不要按原计划「配 reflection 就完事」，那不够；
- 可行路径 A：自研/移植一个 QML→scene 构建期 AOT 步骤（对齐 `CompilationRecorder`
  → `CompiledScene`），才能进 Native Image；
- 可行路径 B（更稳）：**jpackage** 打包自带 JRE，先保证可交付；
  代价是冷启动约 0.5–1s、体积 ~50MB+，需要重新裁定验收指标；
- 建议：阶段 5 结束前做一次 spike 再定，而不是等到阶段 6。

## 10. 已知待复验风险

- 一次**不可复现**的 headless 光栅崩溃（`ACCESS_VIOLATION`）：在 pre-layout 阶段
  dispatch 输入后，raster 渲染带 `anchors.centerIn` 的 Text 时 JVM 崩溃。
  纯 Text 渲染正常。**需要在真实 GL 路径下持续关注**。
- 单例 + 多窗口：主题单例是 JVM 级，多窗口各自深色状态需改用 context 对象。
