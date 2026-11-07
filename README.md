# Selkie Hub

A cross-platform desktop hub whose entire UI is described in QML and rendered by
[qml4j](https://github.com/TIMER-err/qml4j) — a pure-Java QML engine (QML → JVM
bytecode → Skia). Design system: **Material Design 3 Expressive (MD3E)**.

## Stack (verified, not aspirational)

| Concern | Choice | Verified |
|---|---|---|
| Host language | JDK 21 (Azul Zulu 21.0.8) | toolchain locked in Gradle |
| UI engine | qml4j `qml4j-core:v0.2.38` via JitPack | renders on screen |
| Rendering | Skija `0.143.17` (`skija-windows-x64`) | Skia surface 960×640 |
| Windowing | LWJGL 3.3.3 / GLFW + OpenGL 3.2 core | RTX 4060, GL 3.2 |
| Build | Gradle 8.10.2 (wrapper), Kotlin DSL | `gradlew build` green |
| Tests | JUnit 5.10.2 | 9/9 theme tests |

> **The engine is UI-only.** qml4j renders into a Skia surface; it does **not** create a
> window and has **no file API** (`load()` takes QML *source text*). The window, the GL
> context, the render loop and every `.qml` byte all come from the host — that is what
> `selkie.host` is for. Read [docs/qml4j-constraints.md](docs/qml4j-constraints.md)
> before writing any UI code: several assumptions in a "normal" Qt QML project
> (getters, `SpringAnimation`, the Controls widget layer) are **false** here.

## Layout

```text
host/src/main/java/selkie/host/     GLFW window, Skija surface, render loop, input dispatch,
                                    host-driven springs (MotionBridge/SpringChannel)
host/src/main/java/selkie/theme/    MD3E: HCT math, 48 roles, motion/shape/type/spacing tokens
host/src/test/java/                 unit tests (theme + headless QML reactivity)
qml/                                all UI; this directory is a Gradle resources root,
                                    so qml/main.qml is the classpath resource "main.qml"
  main.qml                          three-pane shell (rail | page + top bar | details)
  Theme.qml + qmldir                MD3E token singleton (delegation only)
  NavigationRail.qml / TopBar.qml   shell chrome (flat, see the import rule below)
  components/                       M3Card, M3Switch, M3Button, M3TextField, M3Tabs, M3Toast
  demo-theme.qml                    Stage 1 theme proof page (48 roles, light/dark)
  preview-components.qml            Stage 2 component gallery
docs/                               engine constraints + API recon notes
```

### Two engine rules that shape the file layout

1. **Only `import "."` resolves the `Theme` singleton.** A file in a subdirectory that
   writes `import ".."` can load when a parent imports it first, but fails on eager
   property initialisers with `unknown identifier: Theme`. That is why the shell chrome
   lives flat next to `Theme.qml` while `components/` (only ever instantiated from
   `main.qml`) may stay nested.
2. **Springs are host objects, not QML types.** `motion.channel(token)` returns a *new*
   independent channel per call; a component must take it once into a `property var` and
   bind to `.value`. Sharing one channel between widgets makes them fight.

## Run

```bash
# JAVA_HOME must point at a JDK 21 (bare 'java' may be a JDK 8)
./gradlew run
```

Useful flags: `--dark`, `--seed=0xAARRGGBB`, `--toggle-after=<ms>`,
`--quit-after=<ms>`, `--title=<text>`, `--trace-resources`.

## Status

| Stage | Theme | State |
|---|---|---|
| 0 | Repo + project skeleton | **done** — real window renders |
| 1 | Design system / theme generation | **done** — 48 MD3E roles from a seed + 6 spring tokens, reachable from QML, runtime dark toggle |
| 2 | Core components + 3-pane layout | **done** — 6 components, rail + top bar + details pane, real pointer input, host-driven springs, narrow-screen collapse |
| 3 | Page skeletons + navigation | **done** — six pages, rail drives them, seed picker re-seeds live |
| 4 | Data bridges / real data | — |
| 5 | Motion, responsive, polish | — |
| 6 | Packaging + performance | **done, with revised targets** — jpackage app-image 63 MB / zip 49 MB (target met); cold start 4.8 s and idle 155 MB miss the original 0.5 s / 80 MB goals. See [docs/packaging.md](docs/packaging.md). |

**Known engine gaps** (measured, see `docs/`): no `SpringAnimation` (motion must be
host-driven), no Controls widget layer (no Popup/Menu/ScrollView/StackView), QML can
only read **public fields** (not getters or record accessors), one `QmlEngine` per
`QmlView`, and all QML work must happen on the render thread.

**Packaging:** `scripts/package.ps1` builds a self-contained Windows app-image with a
jlink-trimmed runtime (49 MB zip, no Java needed). Cold start is ~4.8 s and idle memory
~155 MB — measured, with a per-phase breakdown in [docs/packaging.md](docs/packaging.md).
Hitting the original 0.5 s / 80 MB would require GraalVM Native Image, and qml4j defines
classes at runtime (`DynamicClassLoader`), which a closed-world native image cannot host
unless every scene is precompiled at build time. That is a separate subproject, not a
config change.

## Run

```bash
./gradlew run                     # JVM dev run
./gradlew run --args="--dev-backend=7071"   # with the local dev backend
powershell -File scripts/package.ps1        # self-contained Windows build
```
