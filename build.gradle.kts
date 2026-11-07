// Selkie Hub - Stage 0 host build.
//
// Layout notes (deliberate, keep in sync with other stages):
//   host/src/main/java  -> Java sources (sourceSet "main")
//   host/src/test/java  -> JUnit 5 tests (sourceSet "test")
//   qml/                -> QML sources; this directory IS a resources root, so
//                          qml/main.qml is the classpath resource "main.qml".

plugins {
    application
}

group = "selkie"
version = "0.1.0-SNAPSHOT"

val lwjglVersion = "3.3.3"
val skijaVersion = "0.143.17"
val qml4jVersion = "v0.2.38"
val junitVersion = "5.10.2"
// JSON for the data layer (plugin manifests, token sample, dev-backend payloads).
// Stage 4 only - the UI runtime itself stays dependency-free.
val jacksonVersion = "2.17.2"

// Native classifiers for the host OS. Windows is the shipping target; the other
// branches keep the build honest on a Linux/macOS dev box.
val hostOs = System.getProperty("os.name").lowercase()
val hostArch = System.getProperty("os.arch").lowercase()
val macArm = hostOs.contains("mac") && (hostArch == "aarch64" || hostArch == "arm64")
val lwjglNatives: String = when {
    hostOs.contains("win") -> "natives-windows"
    macArm -> "natives-macos-arm64"
    hostOs.contains("mac") -> "natives-macos"
    else -> "natives-linux"
}
val skijaPlatform: String = when {
    hostOs.contains("win") -> "windows-x64"
    macArm -> "macos-arm64"
    hostOs.contains("mac") -> "macos-x64"
    else -> "linux-x64"
}

repositories {
    mavenCentral()
    // qml4j is published only through JitPack.
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
        content { includeGroupByRegex("com\\.github\\..*") }
    }
}

java {
    // Hard lock: the whole host compiles and runs on JDK 21.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

sourceSets {
    main {
        java.srcDir("host/src/main/java")
        resources.srcDir("qml")
    }
    test {
        java.srcDir("host/src/test/java")
    }
}

dependencies {
    // qml4j: QML parsing, JIT bytecode compilation, binding/signal runtime, Skia rendering.
    implementation("com.github.TIMER-err.qml4j:qml4j-core:$qml4jVersion")

    // Skija: the Skia binding qml4j renders through (qml4j keeps skija-shared "provided").
    implementation("io.github.humbleui:skija-shared:$skijaVersion")
    implementation("io.github.humbleui:skija-$skijaPlatform:$skijaVersion")

    // LWJGL provides the OS window + GL context that Skija draws into.
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")

    // JSON parsing/writing for the data layer: data/plugins/*/manifest.json,
    // data/tokens.json and the dev backend's responses.
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    // Silences the "missing @Contract annotation" javac lint noise raised by Skija's
    // JetBrains-annotated bytecode; compile-time only.
    compileOnly("org.jetbrains:annotations:24.1.0")

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "selkie.host.Main"
    // QML is read from the classpath; nothing else is needed at runtime.
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-processing"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
