# Selkie Hub

A cross-platform desktop hub whose entire UI is described in QML and rendered by
[qml4j](https://github.com/TIMER-err/qml4j) — a pure-Java QML engine (QML → JVM
bytecode → Skia). Design system: **Material Design 3 Expressive (MD3E)**.

## Stack

| Concern | Choice |
|---|---|
| Host language | JDK 21 (LTS) — Zulu 21.0.8 |
| UI engine | qml4j `qml4j-core:v0.2.38` via JitPack |
| Rendering | Skija `skija-windows-x64` (Skia JNI) |
| Windowing | LWJGL 3 / GLFW + OpenGL |
| Build | Gradle (Kotlin DSL), JDK 21 toolchain |
| Tests | JUnit 5 |

> **Note on the engine.** qml4j is *not* on Maven Central; it is published through
> JitPack as `com.github.TIMER-err.qml4j:qml4j-core:v0.2.38`. It is a UI engine
> only — it renders into a Skia surface. Creating an OS window, owning the render
> loop and feeding frames is the **host's** job, which is what `selkie.host` does.

## Layout

```text
host/src/main/java/selkie/host/     entry point, window + render loop
host/src/main/java/selkie/theme/    MD3E color roles and motion tokens (Java side)
host/src/test/java/                 unit tests
qml/                                all UI, loaded from the classpath
```

## Run

```bash
# JAVA_HOME must point at a JDK 21
./gradlew run
```

## Status

Work in progress, built stage by stage. See `docs/` for stage notes.
