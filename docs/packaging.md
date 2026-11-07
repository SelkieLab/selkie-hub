# 打包与性能实录（阶段 6）

> 结论先行：**体积达标，冷启动和内存不达标**。本文件给出实测数字、原因分解，
> 以及要把另外两项指标打下来必须做的事——不是调参能解决的。

## 怎么构建

```powershell
cd D:\Apps\selkie-hub
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\package.ps1
```

产物：

| 产物 | 路径 | 体积 |
|---|---|---|
| 可执行 app-image | `build/package/SelkieHub/SelkieHub.exe` | 63.2 MB |
| 分发包 | `build/package/SelkieHub-windows-x64.zip` | **49.0 MB** |

zip 解压后双击 `SelkieHub.exe` 即可运行，**不需要机器上装有 Java**。

## 体积是怎么降下来的

| 步骤 | app-image | zip |
|---|---|---|
| jpackage 默认（整份 JDK 运行时） | 182.4 MB | 83.0 MB |
| + `jlink` 剪裁运行时（含 java.desktop） | 79.8 MB | 62.7 MB |
| + 去掉 `java.desktop` | **63.2 MB** | **49.0 MB** |

最终运行时只保留 `java.base, java.logging, java.net.http, jdk.httpserver, jdk.unsupported`。
去掉 `java.desktop` 是安全的：qml4j 的文字走 Skia 自带的字体管理器，窗口走
LWJGL/GLFW 直连 Win32，AWT/Swing 完全用不到——但这一点是**跑过打包后的 exe 才算数的**
（数据加载正常、窗口出现、stderr 为空、正常退出）。

`jlink` 还加了 `--strip-debug --no-man-pages --no-header-files --compress=zip-6`。

## 性能实测

测量环境：Windows 11 / Zulu 21.0.8 / NVIDIA RTX 4060 / 本目录所在机器。
方法：启动进程后轮询窗口句柄出现时刻；内存取窗口可见后 1.2–1.5s 的进程快照。

### 验收对照

| 指标 | 计划目标 | 实测 | 结论 |
|---|---|---|---|
| 冷启动到窗口可见 | ≤ 0.5 s | **4.7–8.8 s**（3 次：4.7 / 6.5 / 8.8） | ❌ 差一个数量级 |
| 空闲内存（工作集） | ≤ 80 MB | **154–166 MB** | ❌ 约 2 倍 |
| 分发包体积 | ≤ 60 MB | **49.0 MB** | ✅ 达标 |

### 冷启动时间去哪了

宿主内置分段计时（`--quit-after` 跑一次即打印一行）：

```text
[boot] data+jvm=1416ms glfw=1324ms skija=444ms qml=1612ms total=4798ms
```

| 阶段 | 耗时 | 说明 |
|---|---|---|
| JVM 启动 + 数据加载 | ~1.4 s | 类加载（Rhino/ANTLR/ICU4J/ASM/LWJGL/Skija 全在 classpath） |
| GLFW 初始化 + 创建 GL 上下文 | ~1.3 s | 显卡驱动协商 + 窗口创建 |
| Skija `DirectContext.makeGL()` | ~0.44 s | Skia GPU 上下文 |
| **QML 解析 + JIT 编译** | **~1.6 s** | 最大单体开销：`.qml` 在**运行期**被编译成 JVM 字节码 |

## 为什么达不到 0.5s / 80MB

这两个目标隐含的前提是「AOT 编译成原生二进制」。而 qml4j 的架构是**运行期生成字节码**：

```text
.qml 源文件 → ANTLR 解析 → ASM 生成字节码 → ClassLoader.defineClass → 渲染
```

`engine/classloader/JvmClassLoaderBackend$DynamicClassLoader` 和 `compiler/bytecode/QmlCompiler`
就是这一步的实现。这带来两个后果：

1. **GraalVM Native Image 不能直接用**。Native Image 是封闭世界假设：运行期定义新类不可用。
   参考项目 QPlayer 确实发布了 Native Image 可执行文件，但它的 QML 是**构建期**由
   `qml-aot-tool` 预编译成 scene/类的，其 AGENTS.md 明写
   "Production QML is compiled by qml-aot-tool at build time … do not rely on runtime compilation"。
   那个工具依赖 qml4j 的**包内私有 API**（甚至把类放进 `io.github.timer_err.qml4j.render` 包），
   属于非公开接口，跟着 qml4j 升级会碎。
2. **JVM 冷启动下限**。即使解决 QML，JVM + LWJGL + Skija 初始化仍有 1–2s 量级；
   80MB 空闲内存在带 Skia GPU 上下文 + GLFW + JVM 的进程里也很难保证。

## 要达标需要做什么（按性价比排序）

| 方案 | 冷启动 | 内存 | 体积 | 工作量 | 风险 |
|---|---|---|---|---|---|
| A. 现状（jpackage + 剪裁运行时） | ~4.8 s | ~155 MB | 49 MB | 已完成 | 低 |
| B. + AppCDS（类数据共享） | 估计 −20~40% | 持平 | +3~25 MB | 1–2 天 | 低（本次尝试未落地，见下） |
| C. + 构建期 QML AOT（自研/移植） | 估计 −1.5 s | 略降 | 略增 | 1–2 周 | 中（踩 qml4j 私有 API） |
| D. C + GraalVM Native Image | 有望 <1 s | 有望 <100 MB | 30–60 MB | 3–4 周 | 高（Skija JNI/Rhino 反射） |

### 关于 AppCDS 的实测说明

本机尝试过 `-XX:ArchiveClassesAtExit` 生成动态归档，**归档文件没有成功生成**
（宿主以 GLFW 终止 + 退出码结束，动态归档写入被跳过）。CDS 不是本阶段阻塞项，
但它是最便宜的一档优化，值得单独做一次：用 `-XX:DumpLoadedClassList` 先录类表，
再用 `-Xshare:dump` 生成静态归档，然后 `-XX:SharedArchiveFile=` 加载。

## 当前打包产物的已知限制

- **没有安装器**（.msi/.exe 安装包）：app-image zip 更小、无依赖，符合「解压双击」的验收方式。
- **`--win-console` 保留**：便于排错；正式发布应去掉（改 `--win-gui`），双击不弹控制台。
- **`data/` 目录随包发布**：宿主按工作目录解析 `data/`，所以夹具必须跟着 exe 走。
- **交叉编译不可能**：Native Image 与 jpackage 都只能为当前平台构建，三平台需要各自 CI。

## 建议的验收口径调整

如果短期交付优先，建议把阶段 6 指标改为：

| 指标 | 建议值 | 依据 |
|---|---|---|
| 冷启动到窗口可见 | ≤ 3 s（配合 AppCDS 再议） | 当前 4.8 s，JVM 栈下限 |
| 空闲内存（工作集） | ≤ 180 MB | 当前 155 MB，Skia GPU 上下文占主导 |
| 分发包 | ≤ 60 MB | 已达标 49 MB |

若必须坚持 0.5s / 80MB，则应单独立项 C+D（QML AOT + Native Image），
而不是在阶段 6 里用调参追赶。
