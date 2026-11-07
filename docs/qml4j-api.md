# qml4j v0.2.38 — API recon for Selkie Hub host/bridge layer

Owner: qml4j-recon (shared task-3). Date: 2026-11-07.

## 0. Evidence base (how to re-check any claim here)

| Artifact | Path | Use |
|---|---|---|
| Release jar | D:\Apps\_ref\libs\qml4j-core.jar | jar tf, javap -p -c (class inventory, bytecode) |
| **Exact v0.2.38 sources** | D:\Apps\_ref\libs\qml4j-src\ (from jitpack.io/.../qml4j-core-v0.2.38-sources.jar) | authoritative line citations |
| Compiled deps for the proof build | D:\Apps\_ref\libs\deps\ (skija-shared, rhino-1.9.1, antlr4-runtime-4.13.2, asm-9.7, icu4j-77.1, types-0.2.0, skija-windows-x64) | javac / java classpath |
| Working reference host | D:\Apps\_ref\q\qplayer-master (Maven, JDK 21, real qml4j + Skija + LWJGL) | host topology, threading, fonts |
| My verification program | D:\Apps\_ref\notes\_scratch\probe\Probe.java (+ FontProbe.java, SkiaSmoke.java) | 29 runtime checks, results in section 8 |
| Deliverable snippet | D:\Apps\_ref\notes\snippets\MiniHost.java | compiled clean (section 8.1) |

Citation prefixes:

* src/... = D:\Apps\_ref\libs\qml4j-src\io\github\timer_err\qml4j\...
* qplayer/... = D:\Apps\_ref\q\qplayer-master\...
* "javap" = bytecode dump of the release jar (D:\Apps\_ref\notes\_scratch\StockTypes.txt, TypeRegistry.txt)

---

## 1. Bootstrap: exact API surface

### 1.1 The four types you need

src/render/QmlView.java:21-52, src/engine/QmlEngine.java:7-39, src/render/StockTypes.java,
src/render/ResourceLoader.java:5.

~~~java
// qml4j-core v0.2.38 - signatures exactly as in the jar (javap)
public final class QmlView {
    public QmlView(QmlEngine engine, TypeRegistry types);                    // :37
    public static QmlView withStockTypes(QmlEngine engine);                  // :44  = new QmlView(engine, StockTypes.registry())
    public QmlView resources(ResourceLoader res);                            // :48
    public QmlView context(String name, Object value);                       // :94
    public QmlView compilationCache(CompiledSceneCache cache, String key);   // :59  AOT bundle path, optional
    public QmlView networkPolicy(NetworkResourcePolicy policy);              // :65
    public QmlView uiTypefaces(byte[] regular, byte[] medium);               // :73  raw font-file BYTES
    public QmlView cjkTypeface(byte[] bytes);                                // :80
    public QmlView iconTypeface(byte[] bytes);                               // :87
    public Item load(String qml);                                            // :100 -> load(qml, "")
    public Item load(String qml, String baseDir);                            // :106
    public Item root();                                                      // :116
    public Item findByObjectName(String name);                               // :123
    public void renderFrame(SurfaceBackend backend);                         // :309
    public void tickAnimations(long nowNanos);                               // :405
    public DirtyQueue dirtyQueue();                                          // :460
    public Renderer renderer();                                              // :464
    public void dispose();                                                   // :468
    // + dispatchKey / dispatchClick / dispatchPointerDown|Move|Up|Cancel / dispatchWheel,
    //   setClipboard, focus API: :207-303
}

public final class QmlEngine {
    public QmlEngine();                                                    // :13  JvmClassLoaderBackend, safeJavaScript=false
    public QmlEngine(ClassLoaderBackend backend);                           // :17
    public QmlEngine(ClassLoaderBackend backend, boolean safeJavaScript);   // :23  third-party QML -> true
    public ClassLoaderBackend backend();  public Context rootContext();  public JsRuntime.Realm jsRealm();
}

public final class StockTypes {
    public static TypeRegistry registry();      // 69 QML types (includes FileDialog/Window/ApplicationWindow/StyleManager)
    public static TypeRegistry safeRegistry();  // registry() minus FileDialog, Window, ApplicationWindow, StyleManager
}

public interface ResourceLoader { byte[] load(String path); }   // resource-root-relative path, UTF-8 bytes, null = not found
~~~

QmlView is a pure facade - Loader (compile + instantiate), FocusManager, EventDispatcher do the work
(src/render/QmlView.java:29-42; qml4j CLAUDE.md section "Dispatch & polymorphism").

### 1.2 "Load a .qml file" - there is no file API: you pass SOURCE TEXT + a resource root

load(String qml[, String baseDir]) takes QML source, not a path (src/render/QmlView.java:100-114).
Everything a document imports is resolved through your ResourceLoader (src/render/Loader.java:294-335).

~~~java
// classpath root - qplayer's desktop loader
// qplayer/desktop-host/src/main/java/dev/t1m3/qplayer/desktop/resources/ClasspathResourceLoader.java:16-50
public byte[] load(String source) {
    File f = source.startsWith("file:") ? new File(URI.create(source)) : new File(source);
    if (f.isAbsolute() && f.isFile()) return Files.readAllBytes(f.toPath());   // disk override for absolute paths
    try (InputStream in = ClasspathResourceLoader.class.getResourceAsStream("/" + source)) { ... }
}

// directory root - same contract, nothing else (see D:\Apps\_ref\notes\_scratch\probe\Probe.java MapResourceLoader)
ResourceLoader dir = path -> {
    Path p = root.resolve(path);
    return Files.isReadable(p) ? Files.readAllBytes(p) : null;
};
~~~

Resolution rules the Loader applies to whatever the loader returns (src/render/Loader.java):

| QML construct | Resolution | Source |
|---|---|---|
| import QtQuick / import md3.Core | module URI -> directory md3/Core under the resource root ('.' -> '/'); built-ins resolve to nothing | :426-444 |
| import "pages" / import "../widgets" | joined onto the importing document's baseDir, then ../ collapsed | :426-444, :448-459 |
| type Card { } in prefix pages | pages/<qmldir entry or Card.qml> via resources.load | :294-309 |
| qmldir | "<prefix>/qmldir", parsed for "[singleton] Name [1.0] File.qml" | :344-375 |
| import "x.js" as X | evaluated once, bound to global X | :402-417 |
| Loader { source: "Menu.qml" } | document dir first, then resource root | :167-197 |
| pragma Singleton in a document | detected via subDoc.hasPragma("Singleton") | :315-328 |

So: **classpath vs directory is entirely your ResourceLoader**. qml4j ships none and never touches java.io
for QML. For the root document, read the file yourself and pass the string:
view.load(new String(Files.readAllBytes(entry), UTF_8), "") - the second arg is the entry's directory
relative to the resource root (pass "pages" when loading pages/Main.qml).

Verified (probe T10a/T10b): import "pages" -> pages/Page.qml -> import '../components' -> components/Card.qml
-> Card { } instantiates and keeps its declared "property string label".

### 1.3 Hard constraint found by test: ONE QmlEngine PER QmlView

QmlEngine owns one ClassLoaderBackend; the view's Loader defines every compiled document into it with names
io.github.timer_err.qml4j.generated.Component$<n>, n from a per-QmlCompiler counter
(src/compiler/bytecode/QmlCompiler.java:132,166). Two views on one engine both start at Component$0 and the
JVM refuses the second definition:

~~~
T7b  LinkageError: loader ...DynamicClassLoader@... attempted duplicate class definition for
     io.github.timer_err.qml4j.generated.Component$0
T7a  two views, two "new QmlEngine()"   -> both load fine
T7c  second load() on the SAME view     -> works (the counter keeps advancing)
~~~

Recommended topology: one QmlEngine + one QmlView per window; share Java bridge objects across windows
(they are plain objects). qplayer does the same (engine per window: qplayer/desktop-host/.../DesktopWindow.java:139,489-516).
---

## 2. Java -> QML: the one supported mechanism, and its exact rules

### 2.1 Registration: view.context(name, object) == QML's setContextProperty

src/render/QmlView.java:92-98:

~~~java
public QmlView context(String name, Object value) {
    RhinoScope.registerContextProperty(name);            // compiler must accept the identifier
    JsRuntime.putGlobal(engine.jsRealm(), name, value);   // binding resolves it as a free identifier
    return this;
}
~~~

* Accepts ANY Object - not a QObject. qplayer passes a plain final class:
  qplayer/player-core/src/main/java/dev/t1m3/qplayer/bridge/WindowChromeStub.java:12-27
  (public Property fields + public methods, no QObject base).
* **Must be called before load()** (:92-93 comment; the compiler rejects an undeclared free identifier at
  compile time - src/compiler/bytecode/rhino/RhinoScope.java:26-50).
* Name registration is process-global (RhinoScope.CONTEXT_PROPERTIES static set, :46-50); the VALUE is
  per-realm (QmlEngine.jsRealm()), so two windows with two engines can expose the same name to different objects.
* There is no setContextProperty alias, no registerSingletonInstance(instance), and QmlSafeBridge does not
  register anything - it only gates methods (2.7).

### 2.2 What QML can READ: public Java FIELDS only (no getters, no bean introspection)

Read path: JsWrap.JavaMember.get -> MemberAccess.readMember -> Class.getField(name)
(src/engine/js/JsWrap.java:168-180; src/runtime/member/MemberAccess.java:45-56,115-164):

~~~java
// MemberAccess.readMember - the entire rule set
Field f = type.getField(name);              // PUBLIC field, direct or inherited. No getX(), no isX().
if (f == null) throw new IllegalArgumentException("no member '" + name + "' on " + type.getName());
Object val = f.get(target);
if (val instanceof Property) return ((Property<?>) val).get();   // reactive unwrap + dependency tracking
if (val instanceof ObservableList) ((ObservableList<?>) val).trackRead();
return val;
~~~

| Java-side shape | QML sees | Reactive? |
|---|---|---|
| public final Property<String> accent | the VALUE (Property.get()), dependency recorded | YES - every binding that read it re-evaluates on set() |
| public final String appName | the string | no (plain value, read per evaluation) |
| public final List<Row> rows | JS Array (JsWrap.toJs converts List -> NativeArray, JsWrap.java:63-76) | array identity only |
| public int counter | number; writable from QML (Field.set) | no |
| public static ... | not reachable as a member (needs an instance) | - |
| getters / records / private fields | invisible - "no member 'x' on ..." | - |

A Map target is read as a key/value bag (MemberAccess.java:141-143); length and r/g/b/a are virtual on
String/List/Map/color (:122-140).

### 2.3 What QML can CALL: public methods, by exact arity (no overload ranking)

src/runtime/invoke/MethodInvocation.java:31-112:

~~~java
public static Object callMethod(Object receiver, String name, Object[] args)
  Resolved r = resolve(cls, name, args.length);        // cached per (class, name, argc)
  if (r.exact != null)   return r.exact.invoke(receiver, Coercion.coerceArgs(args, r.exactParams));
  if (r.varargs != null) return r.varargs.invoke(receiver, reshaped);
  if (receiver instanceof QObject && __getFunction(name) != null) return ...;   // a QML function
  Field f = cls.getField(name);
  if (Signal.class.isAssignableFrom(f.getType())) { ((Signal) f.get(receiver)).emit(args); return null; }
  throw new IllegalArgumentException("no method '" + name + "' with " + n + " args on " + cls.getName());
~~~

* Class.getMethods() = public methods (inherited included). scan() takes the FIRST public method whose
  parameter count matches exactly; same-arity overloads are NOT disambiguated. Keep bridge method names
  unique and arguments simple (String, int, long, double, boolean, Object).
* Arguments are coerced (Coercion): JS number -> long/double/int, string -> String, etc.
* Verified: QML "theme.add(2, 3)" returned 5 and "theme.bump()" bumped a Java counter (probe T1g).

### 2.4 Signals: a public Signal field, emittable AND connectable from either side

src/engine/Signal.java:6-33; QML side src/engine/js/JsWrap.java:168-180,402-428:

~~~java
public final class Signal {
    public void connect(SignalHandler h);   // SignalHandler: void invoke(Object[] args)
    public void connect(Runnable h);
    public void disconnect(SignalHandler h);
    public void emit(Object... args);
}
~~~

* Java -> QML: a public Signal field read by a binding becomes JsWrap.SignalRef, so QML can do
  "theme.pinged.emit(1, 'x')" AND the bare call "theme.pinged()" (JsWrap.java:420-423); invoking a method
  name that is a Signal field also emits it (MethodInvocation.java:64-72).
  Handlers attach with "Connections { target: theme; function onPinged(a) { ... } }" - Connections is stock
  (render/items/view/Connections.java, target is Property<QObject>; live example of the handler syntax:
  qplayer/shared-qml/miuix/Core/CanvasLineChart.qml:275-277).
* QML -> Java: verified - "onClicked: theme.pinged()" ran the Java handler exactly once (probe T1h).
* Java-side emit() runs handlers synchronously on the emitting thread; from a worker thread that means QML
  code runs off the render thread -> section 6.

### 2.5 Change notification: Property.set(); there is NO PropertyChangeListener

src/engine/binding/Property.java:

~~~java
public T get()  { BindingEvaluationContext.recordRead(this); return value; }      // :41-44  dependency capture
public void set(T v) { clearBinding(); interceptor != null ? interceptor.write(this, v) : setInternal(v); } // :65-76
private void setInternal(T v) { if (unchanged(value, v)) return; changeVersion++; value = v; fireListeners(); } // :265-270
public void setBypassInterceptor(T v);                                  // :78-80   host write, skips interceptor
public void setPaintOnly(T v);                                          // :88-92   no changeVersion bump
public void setInterceptor(WriteInterceptor<T> i);                      // :127-129 intercept ALL writes
public void addListener(Consumer<T> l);                                 // :222-224
public void addChangeHandler(SignalHandler h);                          // :228-230
public void notifyChanged();                                            // :243-246 re-fire dependents, value unchanged
public static long changeVersion();                                     // :261-263 process-global
~~~

* **The notification mechanism is Property.set() bumping its listener list.** There is no
  java.beans.PropertyChangeListener, no Object.notify(), no annotation processor, no observer registry on
  host objects. (grep PropertyChangeListener|notify() over the engine sources: only Property.notifyChanged()
  and Object.notify in a safety denylist.)
* A host object therefore exposes Property fields; Java writes them; QML bindings re-run. That is the whole
  bridge contract for theming/tokens.
* PropertyChangeSink (src/engine/PropertyChangeSink.java:5-9) is NOT the host notification API - it is the
  internal hook QML's PropertyChanges (state system) implements.
* Bindings re-evaluate synchronously when no DirtyQueue is installed, or are queued and flushed inside
  renderFrame / input dispatch (Property.reevaluate :292-331; DirtyQueue install/flush, QmlView.java:194-205,309-331).
  From a background thread this is unsafe - go through the render thread (section 6).
* Idiom for a "reactive method": read a Property inside the method. qplayer's I18n.t(String) reads
  "public final Property<Long> revision" so every binding calling i18n.t(...) re-runs when the language
  changes (qplayer/player-core/src/main/java/dev/t1m3/qplayer/i18n/I18n.java:26-40). Same trick for
  PluginBridge.tokenCount().

### 2.6 Read-only properties - the supported way

There is no read-only flag. Options, in order of preference:

~~~java
// (a) interceptor = "QML may read, QML may not write" (verified: probe T1i, write dropped and counted)
public final Property<String> accent = new Property<>("#6750A4");
public ThemeBridge() { accent.setInterceptor((p, v) -> { /* ignore: host owns this */ }); }
public void setAccent(String hex) { accent.setBypassInterceptor(hex); }   // host write, still notifies QML

// (b) plain final field = value with no notification (a QML write throws IllegalArgumentException,
//     because Field.set on a final field fails; MemberAccess never calls setAccessible)
public final String appName = "Selkie Hub";

// (c) keep the Property private and expose a method (reactive if called from a binding)
private final Property<Integer> count = new Property<>(0);
public int count() { return count.get(); }
~~~

setInterceptor can also transform a write (clamp a slider, ignore NaN) - which is exactly how
Behavior animates (section 4.4).

### 2.7 Third-party QML (plugin windows): QmlSafeBridge

src/engine/QmlSafeBridge.java:11-13; enforcement src/engine/js/JsWrap.java:332-350:

~~~java
public interface QmlSafeBridge { boolean allowsQmlMethod(String name); }
~~~

With new QmlEngine(backend, true) + StockTypes.safeRegistry():

* **fields remain readable** ("Safe QML realms may read public data fields", QmlSafeBridge.java:6-8);
* methods on a non-qml4j object are callable only if it implements QmlSafeBridge and approves the exact name;
  getClass/wait/notify/notifyAll/clone/finalize are always denied (JsWrap.java:335-339);
* Qt.binding must use the arrow form; eval has a deadline (-Dqml4j.safeEvalMs, default 100 ms,
  src/engine/js/JsRuntime.java:33-41);
* safeRegistry() removes FileDialog, Window, ApplicationWindow, StyleManager (javap of StockTypes.safeRegistry).

So PluginBridge should implement QmlSafeBridge, allow-list methods, expose state as Property fields, and run
in a view built with QmlEngine(..., true) + safeRegistry().

---

## 3. QML singletons: BOTH forms are supported

### 3.1 "pragma Singleton" documents (+ qmldir)

* Detected by the Loader: "boolean singleton = (entry != null && entry.singleton) || subDoc.hasPragma(\"Singleton\")"
  (src/render/Loader.java:315-328).
* The compiler emits the accessor on the generated component class
  (src/compiler/bytecode/QmlCompiler.java:192-194,223-245):

~~~java
private static Object __singleton;
public static synchronized Object __instance() {      // exactly one instance per class (per loader)
    if (__singleton == null) __singleton = new Component$N();
    return __singleton;
}
~~~

* qmldir form: "singleton Theme 1.0 Theme.qml" (src/render/Loader.java:359-375).
* Bindings reference it by bare type name; QmlScope.singleton(name) materialises it lazily via
  c.getMethod("__instance").invoke(null) (src/engine/js/QmlScope.java:87-100).
* VERIFIED with no setup (probe T6a): qmldir = "singleton Theme 1.0 Theme.qml", Theme.qml = "pragma Singleton
  ... QtObject { property string accent: '#445566' }", main QML "import '.'" then "color: Theme.accent" -> #445566.

### 3.2 A JAVA CLASS as a QML singleton type

src/compiler/TypeRegistry.java:30-34 plus the special static accessor:

~~~java
registry.registerSingleton("Theme", ThemeBridge.class);   // signature: Class<? extends QObject>

// The class must provide (StyleManager is the in-tree reference implementation):
public final class ThemeBridge extends QObject {                     // src/runtime/color/StyleManager.java:13-31
    public static Object __instance() { ... }                        // :17-22  lazy singleton, static field
    public final Property<Object> currentScheme = new Property<>();   // reactive, readable from QML
}
~~~

* StockTypes.registry() itself does this for one type: registerSingleton("StyleManager", StyleManager.class)
  (javap StockTypes.registry, constant-pool offsets 498-504; StyleManager.__instance() at
  src/runtime/color/StyleManager.java:17). StyleManager is a Java class, not a .qml file - so this is a
  first-class, supported path, not a hack.
* VERIFIED (probe T5a/T5b): hand-written "public static Object __instance()" class registered with
  registerSingleton, used from QML as "color: Theme.accent", instantiated exactly once.
* Scope/limits that matter for the host layer:
  * one instance per class, i.e. per JVM (static field) - a global, NOT per window;
  * visibility is per importing document (singletonsByPrefix, src/render/Loader.java:42-44,284-292);
  * QmlView.dispose() unbinds items from long-lived singletons (src/render/QmlView.java:468-489), and
    qplayer clears __singleton on scene close (qplayer/.../qml/BundledQml.java:121-160).
* For per-window theme state (two windows, different schemes) use context(name, object) with one bridge
  instance per window instead of a singleton type: singleton = "one per app", context = "one per view".

---

## 4. Animations: what exists, what is missing, what is animatable

### 4.1 Complete inventory - src/render/items/animation/ (21 classes in the jar)

~~~
AbstractAnimation (running, loops, finished)   Animatable (tick(long))          PropertyAnimation
NumberAnimation      ColorAnimation           RotationAnimation (direction)     OpacityAnimation
ParallelAnimation    SequentialAnimation      PauseAnimation (duration)         ScriptAction (trigger)
GroupAnimation       Behavior                 Easing (type/overshoot/amplitude/period)   Easings (table)
Timer (interval/running/repeat/triggeredOnStart/triggered)
State (name/when)    PropertyChanges           Transition (running/from/to)     StateController   TargetKey
~~~

**SpringAnimation does not exist** - not in the jar and not in StockTypes (javap registry = 69 types, none
spring-related). Loading it fails with IllegalArgumentException: "unknown QML type: SpringAnimation"
(probe T3a). Same for SmoothedAnimation, PathAnimation, AnimationController, KeyframeGroup.

### 4.2 What is animatable: ANY public Property field on the target object

PropertyAnimation.tick writes through MemberAccess.writeMember(target, property, value)
(src/render/items/animation/PropertyAnimation.java:64-98,104-112) - the same reflective path as 2.2:

* every Item property: x, y, width, height, implicitWidth/Height, opacity, scale, rotation, transformOrigin,
  z, clip, visible, enabled, focus, activeFocus (src/render/items/core/Item.java:30-107);
* every type-specific property (Rectangle.radius, color, Text.color/fontSize, Flickable.contentX/Y,
  grouped paths like border.color via Behavior's dotted lookup);
* **and your Java bridge's Property fields** - "target: theme; property: 'scale';" animates
  ThemeBridge.scale (write goes through the interceptor, 2.6a);
* values: numbers (NumberAnimation / PropertyAnimation), colors (ColorAnimation, HSV lerp,
  ColorAnimation.java:6-25), rotation with direction (RotationAnimation.java:5-41). Anything else snaps at
  t >= 1 (PropertyAnimation.interpolate:134-141).

Semantics: omitted "from" = current value (:104-112); duration 0 writes the end value; loops -1 =
Animation.Infinite; finished fires only on natural completion, not on stop() (AbstractAnimation.java:9-22,
PropertyAnimation.java:88-97); starting an animation stops any other animation on the same (target, property)
(PropertyAnimation.java:36-62).

### 4.3 Easing: a PARTIAL QEasingCurve table - the real motion constraint

src/render/items/animation/Easings.java:8-34 maps only these ordinals; everything else falls through to LINEAR:

| ordinals | curves |
|---|---|
| 0 | Linear |
| 1-3 | In / Out / InOut Quad |
| 5-7 | In / Out / InOut Cubic |
| 9-11 | In / Out / InOut Quart |
| 13-15 | In / Out / InOut Sine |
| 29-31 | In / Out / InOut Back (overshoot, c1 = 1.70158) |
| everything else | In/Out/InOut Quint, Circ, Expo, Elastic, Bounce -> linear fallback |

Easing.overshoot/amplitude/period are stored but unused (Easing.java:8-15).

=> **Spring-like motion must be produced by the host, not the engine.** Two workable routes:

1. Host-side spring integrator writing a Property - qplayer already has one:
   qplayer/player-core/src/main/java/dev/t1m3/qplayer/lyric/skia/SpringAnim.java:42-51
   (SpringAnim(stiffness, damping), SpringAnim(mass, damping, stiffness)), stepped from the frame loop.
   The same object can drive ThemeBridge.motionProgress (Property<Double>) that QML binds to.
2. Multi-stage approximation: SequentialAnimation / ParallelAnimation of
   NumberAnimation { easing.type: Easing.OutBack; duration: ... } chained with decreasing deltas
   (OutBack is the springiest available curve).

The engine ticks animations only from QmlView.tickAnimations(nowNanos) (QmlView.java:405-424), which the host
calls every frame - so a host spring and QML animations share one clock and one frame (verified: NumberAnimation
on width 0 -> 100 over 200 ms reads exactly 50.0 at t+100 ms, probe T2a/T2b).

### 4.4 Behavior works on any Property, including a Java-side write

src/render/items/animation/Behavior.java:9,41-52,74-115: it implements Property.WriteInterceptor, finds the
owner's Property by public field (dotted paths supported, :163-175), reads duration/easing from its child
PropertyAnimation, and tweens instead of assigning. It is armed only after construction completes
(:54-60), so initial bindings do not animate.

VERIFIED (probe T2c/T2d): "Behavior on opacity { NumberAnimation { duration: 200 } }" plus a Java-side
beh.opacity.set(1.0) -> 0.5 after 100 ms, 1.0 once settled.

### 4.5 Two documentation corrections (measured, not read)

* README says "property is a reserved keyword - NumberAnimation { property: \"width\" } won't parse".
  **False in v0.2.38**: it parses and targets correctly - NumberAnimation { target: box; property: 'width';
  from: 0; to: 80; duration: 100; running: true } gave width 40 at t+50 ms (probe T3b/T9a).
* A PropertyAnimation created IN JAVA only ticks when it is part of the item tree (QmlView.collectAnimations
  walks children/resources, :446-458): root.children.add(anim); anim.running.set(true); then it animates a
  QML property (probe T9b). Without children.add it silently never ticks.

---

## 5. Item + layout coverage (authoritative list from the jar)

StockTypes.registry() registers exactly **69** QML type names (bytecode dump, registration order):

~~~
Item Rectangle Text Column Row RowLayout ColumnLayout StackLayout GridLayout Flow
MultiEffect ShaderEffectSource Translate Rotation Scale FileDialog TapHandler DragHandler
Timer MouseArea Image Loader PropertyAnimation NumberAnimation ColorAnimation RotationAnimation
OpacityAnimation ParallelAnimation PauseAnimation ScriptAction SequentialAnimation State
PropertyChanges Transition Behavior Repeater ListModel ListElement ListView GridView Component
Connections Flickable Canvas Binding FocusScope Shape ShapePath PathMove PathLine PathQuad
PathCubic PathArc DropShadow Glow ColorOverlay Gradient GradientStop TextInput TextEdit
Window ApplicationWindow Control AbstractButton Button Label TextField QtObject StyleManager
~~~

safeRegistry() = the same list minus FileDialog, Window, ApplicationWindow, StyleManager.

Package grouping of the render/items tree (jar tf):
core/ (Item, Rectangle, Text, TextWrap, Image, MouseArea, MouseEvent, Flickable, Canvas, Gradient,
GradientStop, Border, Font, Drag, Size, ColorMath, ChildrenRect, Binding), layout/, view/, animation/,
transform/ (Translate, Scale, Rotation, Axis, Origin), effect/, input/ (TextInput, TextEdit, TextField,
TextEditable, Keys, KeyEvent, FocusScope), window/, shape/, dialog/ (FileDialog only), handler/
(TapHandler, DragHandler).

### 5.1 Anchors - real, complete for the common cases

src/render/Anchors.java:7-24 (grouped anchors.*) + Item.left/right/top/bottom/horizontalCenter/verticalCenter
(Item.java:82-93), AnchorLine edges in src/render/AnchorLine.java:

~~~
fill, centerIn, margins, leftMargin, rightMargin, topMargin, bottomMargin,
left, right, top, bottom, horizontalCenter, verticalCenter,
horizontalCenterOffset, verticalCenterOffset
~~~

**Gap:** no baseline / baselineOffset. VERIFIED working: anchors.fill: parent laid a MouseArea out to the
parent box (200x100) during layout (probe T8b).

### 5.2 Positioners and QtQuick.Layouts

* Positioners: Row, Column, Flow (+ spacing, Flow.flow, Flow.padding). **The Grid POSITIONER is missing**
  (only GridLayout and GridView exist).
* Layouts: RowLayout, ColumnLayout, GridLayout (columns, rows, rowSpacing, columnSpacing, flow),
  StackLayout (currentIndex), plus spacing on Row/ColumnLayout.
* Attached Layout.* on EVERY Item (src/render/items/layout/LayoutAttached.java:10-32; field Item.Layout at
  Item.java:79): fillWidth, fillHeight, preferredWidth/Height, minimumWidth/Height, maximumWidth/Height,
  alignment, margins, leftMargin, rightMargin, topMargin, bottomMargin, row, column, rowSpan, columnSpan
  (min/max default -1 = unset).
* Sizing helpers: LayoutSizing, PositionerSizing (jar).

### 5.3 Views, models, delegates - and how a Java model plugs in

* Repeater (model, windowStart, windowCount, instances()), ListView (model, spacing, orientation,
  cacheBuffer, currentIndex, highlightMoveDuration, snapMode, highlightRangeMode, preferredHighlightBegin/End,
  count), GridView (model, cellWidth, cellHeight, flow), ListModel (rows, count, rowsInserted/Removed/Changed),
  ListElement, Component, Loader (source, sourceComponent, item, active, asynchronous, loaded), Connections,
  Flickable (contentX/Y, contentWidth/Height, flickableDirection, interactive, moving, boundsBehavior,
  margins, pressDelay), Timer.
* Java model from a plain java.util.List: sizeOf/dataAt accept ListModel, Number (literal count) and List
  (src/render/items/view/ListView.java:139-159); a List is converted to a JS array on read
  (JsWrap.java:63-76). Delegate rows: a Map row exposes roles by bare name
  (src/runtime/member/DelegateScope.java:106-116); a POJO row is reached as modelData.field
  (JavaMember -> public field). VERIFIED (probe T4a/T4b): model: rows with List<Row>,
  delegate Text { text: modelData.title } -> 2 delegates, text "alpha".
* No incremental notification for a plain List: only ListModel has rowsInserted/Removed/Changed and
  ListView subscribes to those (ListView.java:75-90). To refresh from Java either set the model property
  again (model.set(newList) -> rebuild, :92-110) or use ListModel + its signals. There is no
  beginInsertRows-style delta API for host lists.
* ListView gaps vs Qt: no header/footer/section/highlight/currentItem/positionViewAtEnd, no delegate
  PROPERTY (the delegate is compiled into a DelegateFactory and installed via setDelegate, :57-61);
  only positionViewAtIndex.

### 5.4 Input, text, canvas, shapes, effects

* MouseArea (clicked, doubleClicked, pressed, released, positionChanged, entered, exited, canceled, wheel;
  pressed, hoverEnabled, acceptedButtons, containsMouse, mouseX/Y, cursorShape, drag.*), TapHandler,
  DragHandler, Keys, FocusScope, Item.focus/activeFocus/activeFocusOnTab.
* Text (text, color, fontSize, font.*, style/styleColor/styleWidth, wrapMode, horizontalAlignment,
  verticalAlignment, elide, maximumLineCount, lineHeight, lineHeightMode, contentWidth/Height), TextInput,
  TextEdit (multi-line), TextField (placeholderText, radius, borderColor, padding, ...), Label.
* Canvas (onPaint signal, contextType, renderStrategy, renderTarget, available; Context2D ops; offscreen
  cache with -Dqml4j.canvasCache).
* Shape / ShapePath / PathMove|Line|Quad|Cubic|Arc / ImageFill; effects DropShadow, Glow, ColorOverlay,
  MultiEffect (blur/shadow/mask/brightness/contrast/saturation/colorization), ShaderEffectSource, Item.layer.
* Window, ApplicationWindow, Control, AbstractButton, Button (real clicked/pressed/released/toggled
  signals), FileDialog.

### 5.5 Gaps vs Qt QML/Controls that affect Selkie Hub

Absent (unknown type -> compile error at load): Grid positioner, Popup, Dialog, Menu, ScrollView, SplitView,
Page, StackView, SwipeView, PathView, TableView, TreeView, BorderImage, AnimatedImage, ShaderEffect,
DropArea, MultiPointTouchArea, PinchArea, Switch, Checkbox, Slider, ComboBox, SpinBox, TabBar, ToolTip -
i.e. the whole QtQuick.Controls widget layer except Button/Label/Control/AbstractButton/TextField.
qplayer builds every MD3 widget as .qml files instead (qplayer/shared-qml/miuix/Core/*.qml).
Also missing: Item.baseline anchors; no font letterSpacing/wordSpacing/capitalization beyond what
src/render/items/core/Font.java declares; no Component.createObject convenience beyond compiled factories.

---

## 6. Threading: one thread owns the view; everything else must be marshalled

### 6.1 Rules

1. **QmlView (create -> context() -> load() -> tickAnimations() -> renderFrame() -> dispose()) must stay on
   one thread.** qml4j README, "Known limitations": "Renderer is not thread-safe. All render() / dispatch
   calls must come from one thread (the GL thread)."
2. Input dispatch (dispatchClick / dispatchPointer* / dispatchKey) belongs on that same thread.
3. QmlEngine construction is not thread-bound, but the QmlView built on it is. qplayer creates the engine in
   the window constructor and builds the view on the render thread
   (qplayer/.../DesktopWindow.java:139,489-516; RenderThread.java:79).
4. **Property writes from another thread are a real race**, even though they appear to work: DirtyQueue and
   the binding-evaluation stack are ThreadLocal (src/engine/binding/DirtyQueue.java:8,
   src/engine/binding/BindingEvaluationContext.java:9-10, src/engine/binding/Property.java:147-151) while
   Property.changeVersion and Property.evaluating are process-global statics (Property.java:259-263,293-331).
   qplayer wraps every frame's mutation + flush in a monitor and states why:

~~~java
// qplayer/player-core/src/main/java/dev/t1m3/qplayer/qml/QmlRuntimeLock.java:3-18
/** Dirty queues are thread-local, but Property.evaluating is not. A Property.set on the GLFW/Android
 *  main thread therefore reevaluates dependents immediately, and that reenter races a render-thread
 *  flush of the same property: qml4j reports "cycle detected in binding evaluation" and the desktop
 *  render thread dies. Hold this monitor around every flush and around host-side writes that are
 *  not already on that render thread. */
public static final Object MONITOR = new Object();

// user: qplayer/desktop-host/.../RenderThread.java:106-146
synchronized (QmlRuntimeLock.MONITOR) {
    dq.install();
    try { win.drainRenderTasks(); win.tickInput(); ... view.tickAnimations(System.nanoTime()); dq.flush(); ... }
    finally { dq.uninstall(); }
}
~~~

5. **Recommended for Selkie Hub:** one render thread; a BlockingQueue<Runnable> of host->QML tasks drained at
   the top of each frame (qplayer: drainRenderTasks(), DesktopLyricWindow.postInput),
   plus the QmlRuntimeLock-style monitor if any window ever writes from the UI thread. Never let a Java
   bridge call Property.set from an audio/HTTP thread without posting.
6. dispose() must run on the scene thread, after the last frame, before the GPU context dies
   (qplayer RenderThread.java:182-212; BundledQml javadoc :27-28).

### 6.2 Multi-window

Each window needs its own QmlEngine + QmlView (1.3) and, in qplayer's design, its own render thread sharing
the QmlRuntimeLock monitor, because Property.changeVersion is global
(qplayer/.../DesktopLyricRenderThread.java:61-70). Two windows on two threads is safe only if they never write
each other's Property objects; the simplest correct topology is one render thread for all windows.

---

## 7. Java 21 compatibility: no blockers found (evidence)

| Check | Result | Evidence |
|---|---|---|
| Published bytecode target | **major 52 = Java 8** | raw class header of render/QmlView.class and engine/QObject.class (bytes 6-7) |
| Source release | maven.compiler.release=8 (tests 11) | JitPack parent pom qml4j-parent/v0.2.38 |
| Generated-at-runtime classes | cw.visit(Opcodes.V1_8, ...) | src/compiler/bytecode/QmlCompiler.java:181 |
| Class definition mechanism | subclass of ClassLoader calling the PROTECTED defineClass - legal on 21 | src/engine/classloader/JvmClassLoaderBackend.java:18-39 |
| setAccessible / Unsafe / sun.* / --add-opens in core | none (grep over all 200 sources: 0 hits) | - |
| Reflection used | Class.getField, getMethods, Class.getMethod("__instance") on app classes only | MemberAccess.java:52, MethodInvocation.java:105, QmlScope.java:94 |
| ASM | 9.7 (writes up to class-file 23); used as writer only | parent pom asm.version=9.7 |
| Rhino | 1.9.1; interpreter opt level configurable (-Dqml4j.rhino.opt=-1 forces interpreter, what qplayer sets for Android/native-image) | src/engine/js/JsRuntime.java:33-41; qplayer BundledQml.java:36-40 |
| ICU4J | pinned 77.1, with the comment "78+ requires Java 11; the engine's public runtime floor is Java 8" | jar META-INF/maven/.../pom.xml |
| Jar build JDK | Build-Jdk-Spec: 17 | META-INF/MANIFEST.MF |
| **Runtime proof** | 29/30 behavioural checks pass on **Zulu 21.0.8 (Windows x64)** with the Java-8-target jar | section 8.2 |

Nothing to add to JAVA_TOOL_OPTIONS / --add-opens. The only Java-21-adjacent caveats are ours, not the engine's:
compile the host with --release 21 (qml4j types are Java-8 bytecode, forward compatible), and never share a
QmlEngine between views (1.3).

One observed anomaly (not Java-21 related, not reproducible in a plain render): the very first headless probe
run crashed the JVM (EXCEPTION_ACCESS_VIOLATION) while raster-rendering a scene that contained
"Text { anchors.centerIn: parent }" after input had been dispatched before any layout. A later run of the same
scene shape rendered fine, and Text at explicit x/y renders with AND without uiTypefaces (FontProbe: 1199 vs
1267 non-white pixels; raw Skija raster is healthy - SkiaSmoke draws and reads back FF00FF00). Treat as
"verify in the real GL path", not as a rule. Note also that fonts are HOST-supplied: QmlView.uiTypefaces takes
raw font-file bytes and qml4j has no installed-font lookup - qplayer resolves the bytes itself
(qplayer/.../DesktopWindow.java:524-548,533 uiTypefaces, 546 iconTypeface).

---

## 8. Minimal complete snippet (compiles clean) + what was verified

### 8.1 D:\Apps\_ref\notes\snippets\MiniHost.java - load a QML string, expose one Java object, read a read-only property

~~~java
import io.github.timer_err.qml4j.engine.QObject;
import io.github.timer_err.qml4j.engine.QmlEngine;
import io.github.timer_err.qml4j.engine.binding.Property;
import io.github.timer_err.qml4j.render.QmlView;
import io.github.timer_err.qml4j.render.items.core.Item;

public final class MiniHost {

    /** Host state QML reads. A public Property field is REACTIVE; a public field is a plain value. */
    public static final class Theme extends QObject {

        public final Property<String> accent = new Property<>("#6750A4"); // read-only from QML
        public final String appName = "Selkie Hub";                       // plain field, read-only

        public Theme() {
            // QML may read accent; a QML-side write is dropped (the host owns it).
            accent.setInterceptor((property, value) -> { });
        }

        /** Host-side mutation: bypasses the interceptor and notifies every QML binding. */
        public void setAccent(String hex) { accent.setBypassInterceptor(hex); }
    }

    public static void main(String[] args) {
        Theme theme = new Theme();

        QmlView view = QmlView.withStockTypes(new QmlEngine())  // 69 stock QtQuick types
                .context("theme", theme)                        // QML setContextProperty (before load!)
                .resources(path -> null);                       // resource root: nothing imported here

        Item root = view.load(
                "import QtQuick\n" +
                "Rectangle {\n" +
                "    width: 320; height: 96\n" +
                "    color: theme.accent\n" +
                "    Text { anchors.centerIn: parent; text: theme.appName; color: \"#ffffff\" }\n" +
                "}");

        theme.setAccent("#006A6A");   // re-runs the "color: theme.accent" binding

        // Host render loop (your window owns this thread):
        //   view.tickAnimations(System.nanoTime());
        //   view.renderFrame(surfaceBackend);

        System.out.println(root.width.peek() + "x" + root.height.peek() + " ready");
    }
}
~~~

Exact commands used (JDK 21, no window, no --add-opens):

~~~
set J=%JAVA_HOME%\bin
set CP=D:\Apps\_ref\libs\qml4j-core.jar;D:\Apps\_ref\libs\deps\skija-shared.jar;D:\Apps\_ref\libs\deps\rhino.jar;D:\Apps\_ref\libs\deps\antlr4-runtime.jar;D:\Apps\_ref\libs\deps\asm.jar;D:\Apps\_ref\libs\deps\icu4j.jar

%J%\javac -d out -cp %CP% MiniHost.java                         -> exit 0, no warnings
%J%\java -cp out;%CP%;D:\Apps\_ref\libs\deps\skija-windows-x64.jar;D:\Apps\_ref\libs\deps\types.jar MiniHost
   -> 320x96 ready     (root.width.peek() is a Long, hence the integer form)
~~~

(skija-windows-x64 + types.jar are RUNTIME only; skija-shared alone compiles.)

### 8.2 Behavioural verification - D:\Apps\_ref\notes\_scratch\probe\Probe.java (29 PASS / 1 expected FAIL)

Run: java -cp out;... probe.Probe [render]

| Check | Observed | Proves |
|---|---|---|
| T1a / T1b | root Item width 200; findByObjectName works | 1 bootstrap, load(), findByObjectName |
| T1c | color=#6750A4 | 2.2 QML reads a Java Property field |
| T1d | text="Selkie Hub" | 2.2 QML reads a plain public final String |
| T1e | color=#006A6A after setAccent | 2.5 Property.set re-evaluates the binding |
| T1f / T1g | click handled; lastSum=5 | 2.3 dispatchClick -> QML calls a Java method with args |
| T1h | pings=1 | 2.4 QML emitted a Java Signal |
| T1i | rejected=1, accent unchanged | 2.6 read-only Property drops the QML write |
| T2a / T2b | 50.0 at 100 ms, 100.0 at 300 ms | 4.2 NumberAnimation on width + host-driven clock |
| T2c / T2d | 0.5 at 100 ms, 1.0 settled | 4.4 Behavior intercepts a Java-side write |
| T3a | IllegalArgumentException: unknown QML type: SpringAnimation | 4.1 no spring animation |
| T3b | parses | 4.5 "property:" is NOT reserved in 0.2.38 |
| T4a / T4b | count=2, text=alpha | 5.3 List<POJO> model, modelData.field roles |
| T5a / T5b | #112233, instantiations=1 | 3.2 Java class as singleton type |
| T6a | #445566 | 3.1 pragma Singleton + qmldir |
| T7a / T7c | two engines OK; reload on same view OK | 1.3 engine topology |
| T7b | FAIL (expected): LinkageError duplicate class definition Component$0 | 1.3 one engine per view - the failure IS the finding |
| T8a / T8b / T8c | argb=FF6750A4; ma=200x100; click-through emits | 5.1 anchors layout, 6 headless render, input |
| T9a / T9b | width 40.0; opacity 0.5 | 4.5 property: targeting; Java-created animation needs tree membership |
| T10a / T10b | inner resolved, label=card | 1.2 directory + module imports via ResourceLoader |

(Probe.exit code 1 only because T7b is an intentionally asserted engine-sharing constraint.)

### 8.3 Recommended shapes for ThemeBridge / PluginBridge / TokenBridge

~~~java
// ThemeBridge - per-window context object (NOT a singleton type): one per QmlView.
public final class ThemeBridge extends QObject {                   // QObject base only needed if also registerSingleton'd
    public final Property<String> primary   = ro("#6750A4");       // MD3 role hexes as colors
    public final Property<String> onPrimary = ro("#FFFFFF");
    public final Property<Boolean> dark     = ro(Boolean.FALSE);
    public final Property<Double> springProgress = new Property<>(0.0);  // host spring writes this
    public final Signal schemeChanged = new Signal();

    private static <T> Property<T> ro(T initial) {                 // read-only from QML (2.6a)
        Property<T> p = new Property<>(initial);
        p.setInterceptor((prop, value) -> { });
        return p;
    }

    public void applyScheme(ColorScheme scheme) {                   // Java -> QML: one write per role
        primary.setBypassInterceptor(scheme.primary());
        onPrimary.setBypassInterceptor(scheme.onPrimary());
        dark.setBypassInterceptor(scheme.dark());
        schemeChanged.emit();
    }

    public int roleCount() { return 48; }                           // callable from QML (2.3)
}

// PluginBridge - third-party QML window: implements QmlSafeBridge, built with
// new QmlEngine(new JvmClassLoaderBackend(), true) + StockTypes.safeRegistry()
public final class PluginBridge extends QObject implements QmlSafeBridge {
    public final Property<Boolean> loading = new Property<>(Boolean.TRUE);
    private static final java.util.Set<String> ALLOWED = java.util.Set.of("invoke", "log");
    @Override public boolean allowsQmlMethod(String name) { return ALLOWED.contains(name); }
    public String invoke(String action, String json) { ... }        // unique name, simple args
}

// TokenBridge - same shape; expose motion tokens as numbers/strings QML can bind, and expose the
// host spring's progress as a Property advanced from the frame loop (SpringAnimation does not exist, 4.1/4.3).
~~~

---

## 9. One-line answers

1. **Bootstrap** - QmlView.withStockTypes(new QmlEngine()) (or new QmlView(engine, StockTypes.registry())),
   optional .resources(ResourceLoader).context(name, obj), then load(qmlSource[, baseDir]); imports resolve
   through your ResourceLoader (classpath or directory - same one-method interface); ONE engine per view.
2. **Java->QML** - view.context(name, obj) BEFORE load(); QML reads public FIELDS only (a Property field is
   unwrapped and made reactive), calls public METHODS by exact arity, reads/emits/connects public SIGNAL
   fields; change notification is Property.set() - there is no PropertyChangeListener; read-only via
   setInterceptor; safe realms via QmlSafeBridge.
3. **Singletons** - both forms: "pragma Singleton" documents / qmldir "singleton Name 1.0 File.qml", and
   TypeRegistry.registerSingleton(name, SomeJavaClass.class) with a hand-written
   "public static Object __instance()" (in-tree example StyleManager); one instance per JVM, per-app not per-window.
4. **Animations** - 21 classes (Property/Number/Color/Rotation/Opacity/Parallel/Sequential/Pause/ScriptAction
   + Behavior/State/PropertyChanges/Transition/Timer); animatable = any public Property field; NO SpringAnimation
   and only Quad/Cubic/Quart/Sine/Back easings (others fall back to linear) -> spring motion must be host-driven
   (qplayer SpringAnim) or staged OutBack.
5. **Items/layout** - 69 stock types; anchors (no baseline), Row/Column/Flow, Row/Column/Grid/StackLayout with
   full Layout.*, Repeater/ListView/GridView + Java List / ListModel models, MouseArea/Flickable/Text/Canvas/
   Shape/effects; MISSING: Grid positioner, the whole Controls widget layer, Popup/Dialog/Menu/ScrollView/
   SplitView/Page/StackView/TableView/BorderImage/AnimatedImage/ShaderEffect.
6. **Threading** - build, load, tick, render, dispatch and dispose a QmlView on ONE thread (the GL/render thread);
   engine creation is free; cross-thread Property writes are a real race (Property.evaluating and changeVersion
   are globals) -> post tasks to the render thread and/or hold qplayer's QmlRuntimeLock monitor.
7. **Java 21** - fine: Java-8 bytecode (major 52), --release 8 build, ASM writes V1_8 classes, no setAccessible /
   Unsafe / JDK-internal reflection, class definition via a legal ClassLoader subclass; 29/30 checks pass on
   Zulu 21.0.8 with Rhino 1.9.1 + ICU4J 77.1.




