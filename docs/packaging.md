# 打包与性能实录（阶段 6）

> 一句话：**体积达标，冷启动与内存不达标**。下面全是实测数字、时间分解，
> 以及要把那两项打下来必须做的事——调参救不了。

## 构建

```powershell
cd D:\Apps\selkie-hub
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\package.ps1
```

| 产物 | 路径 | 体积 |
|---|---|---|
| 可执行 app-image | `build/package/SelkieHub/SelkieHub.exe` | 63.2 MB |
| 分发包 | `build/package/SelkieHub-windows-x64.zip` | **49.0 MB** |

zip 解压后双击即用，**目标机不需要安装 Java**。

## 体积怎么降下来的

| 步骤 | app-image | zip |
|---|---|---|
| jpackage 默认（整份 JDK 运行时） | 182.4 MB | 83.0 MB |
| + jlink 剪裁（含 java.desktop） | 79.8 MB | 62.7 MB |
| + 去掉 java.desktop | **63.2 MB** | **49.0 MB** |

最终运行时只保留 `java.base, java.logging, java.net.http, jdk.httpserver, jdk.unsupported`，
外加 `--strip-debug --no-man-pages --no-header-files --compress=zip-6`。

去掉 `java.desktop` 是有风险的改动，所以是**跑过打包后的 exe 才算数**的：
数据加载正常、窗口正常渲染（截图 `D:\Apps\_ref\shots\packaged-app.png`，1016×759、133 色）、
stderr 为空、正常退出。原因：qml4j 的文字走 Skia 自带字体管理器，窗口走 LWJGL/GLFW 直连 Win32，
AWT/Swing 完全用不到。

## 实测

环境：Windows 11 / Zulu 21.0.8 / NVIDIA RTX 4060。
方法：启动进程后轮询窗口句柄出现时刻（墙钟），宿主自身再打印进程内分段计时。

| 指标 | 计划目标 | 实测 | 结论 |
|---|---|---|---|
| 冷启动到窗口可见 | ≤ 0.5 s | **~3.0 s**（空闲机器，3 次：3.01 / 3.06 / 3.10） | ❌ |
| 空闲内存（工作集） | ≤ 80 MB | **163–168 MB**（2/4/6 秒采样） | ❌ |
| 分发包体积 | ≤ 60 MB | **49.0 MB** | ✅ |

注意：机器繁忙时墙钟会被进程创建/杀毒扫描拖到 5–9 s（我早期测到过 8.8 s）。
**进程内**计时才是应用自身的开销，稳定在 2.0–2.3 s。

### 冷启动的时间分布

```text
[boot] data+jvm=640ms glfw=478ms skija=275ms qml=841ms total=2235ms
```

| 阶段 | 典型耗时 | 说明 |
|---|---|---|
| JVM 启动 + 数据加载 | 0.63–1.0 s | 类加载（Rhino / ANTLR / ICU4J / ASM / LWJGL / Skija 全在 classpath） |
| GLFW 初始化 + GL 上下文 | 0.48–0.93 s | 驱动协商 + 窗口创建 |
| Skija `makeGL()` | 0.25–0.33 s | Skia GPU 上下文 |
| **QML 解析 + JIT 编译** | **0.76–1.01 s** | 最大单项：`.qml` 在**运行期**编译成 JVM 字节码 |

### 内存是怎么调下来的

jpackage 默认参数下工作集 ~232 MB。改成下面这组后降到 **163–168 MB**：

```text
-Xms16m -Xmx160m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:TieredStopAtLevel=1
```

`-Xms16m` 不让堆机会性膨胀，`MaxMetaspaceSize` 封顶元空间，SerialGC 省掉并行 GC 线程栈，
`TieredStopAtLevel=1` 只用 C1（桌面应用不需要 C2 的峰值吞吐）。
剩下的 ~160 MB 主要是 Skia GPU 上下文 + 字体 + GLFW + JVM 基线，**调参已到极限**。

## 为什么打不到 0.5 s / 80 MB

这两个目标隐含的前提是「AOT 编译成原生二进制」，而 qml4j 的架构是**运行期生成字节码**：

```text
.qml → ANTLR 解析 → ASM 生成字节码 → ClassLoader.defineClass → 渲染
```

实现在 `engine/classloader/JvmClassLoaderBackend$DynamicClassLoader` 与 `compiler/bytecode/QmlCompiler`。
两个直接后果：

1. **GraalVM Native Image 不能直接用**。封闭世界假设下不能在运行期定义新类。
   参考项目 QPlayer 确实发了 Native Image 可执行文件，但它的 QML 是**构建期**由 `qml-aot-tool`
   预编译成 scene/类的；其 AGENTS.md 明写 "Production QML is compiled by qml-aot-tool at build
   time … do not rely on runtime compilation"。那个工具依赖 qml4j 的**包内私有 API**
   （类甚至放进 `io.github.timer_err.qml4j.render` 包），跟着引擎升级会碎。
2. **JVM 栈的下限**。就算解决 QML，JVM + LWJGL + Skija 初始化仍是 1–2 s 量级；
   带 Skia GPU 上下文的进程也很难压到 80 MB 以内。

## 要达标该做什么（按性价比）

| 方案 | 冷启动 | 内存 | 体积 | 工期 | 风险 |
|---|---|---|---|---|---|
| A 现状（jpackage + 剪裁运行时） | ~3.0 s | ~165 MB | 49 MB | 已完成 | 低 |
| B + AppCDS 类数据共享 | 估计 −20~40% | 持平 | +3~25 MB | 1–2 天 | 低 |
| C + 构建期 QML AOT（自研/移植） | 再 −0.8~1.0 s | 略降 | 略增 | 1–2 周 | 中（踩私有 API） |
| D C + Native Image | 有望 <1 s | 有望 <100 MB | 30–60 MB | 3–4 周 | 高（Skija JNI / Rhino 反射） |

### AppCDS 试过但没落地

本机用 `-XX:ArchiveClassesAtExit` 生成动态归档**没有成功**（宿主机以 GLFW 终止 + 退出码结束，
归档写入被跳过）。它是性价比最高的一档优化，值得单独做：先用 `-XX:DumpLoadedClassList` 录类表，
再 `-Xshare:dump` 生成静态归档，最后 `-XX:SharedArchiveFile=` 加载。

## 当前产物的已知限制

- **没有安装器**：app-image zip 更小、无依赖，符合「解压双击」验收。
- **`--win-console` 保留**：便于排错；正式发布应去掉。
- **`data/` 随包发布**：宿主按工作目录解析 `data/`，夹具必须跟 exe 走。
- **不能交叉编译**：jpackage / Native Image 都只能为当前平台构建，多平台要各自 CI。

## 建议的验收口径

短期交付优先时，建议把阶段 6 指标改成：

| 指标 | 建议值 | 依据 |
|---|---|---|
| 冷启动到窗口可见 | ≤ 3.5 s | 实测 3.0 s；进程内 2.2 s |
| 空闲内存（工作集） | ≤ 180 MB | 实测 165 MB |
| 分发包 | ≤ 60 MB | 实测 49 MB |

若必须坚持 0.5 s / 80 MB，应把 C+D 单独立项（QML AOT + Native Image），
而不是在阶段 6 用调参硬追。
