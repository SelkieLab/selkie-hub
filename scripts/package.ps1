<#
.SYNOPSIS
  Build a self-contained Windows app-image for Selkie Hub with jpackage.

.DESCRIPTION
  Produces build/package/SelkieHub/SelkieHub.exe plus a trimmed runtime, so the app
  runs on a machine with no Java installed.

  Two things are deliberately NOT done here:
    * no GraalVM native-image - see docs/packaging.md: qml4j defines QML classes at
      runtime through a ClassLoader, which a closed-world native image cannot host
      without precompiling every scene at build time.
    * no installer (.msi/.exe) - an app-image zip is the smaller, dependency-free
      artefact and matches the unzip-and-double-click acceptance.
#>
[CmdletBinding()]
param(
    [string]$OutDir
)

$ErrorActionPreference = 'Stop'
# $PSScriptRoot is not populated while parameter defaults are evaluated.
$scriptDir = $PSScriptRoot
if (-not $scriptDir) { $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path }
$repo = (Resolve-Path (Join-Path $scriptDir '..')).Path
if (-not $OutDir) { $OutDir = Join-Path $repo 'build\package' }

Push-Location $repo
try {
    Write-Host '[package] 1/5 building the runtime classpath...'
    & .\gradlew.bat installDist --console=plain -q
    if ($LASTEXITCODE -ne 0) { throw 'gradlew installDist failed' }

    $lib = Join-Path $repo 'build\install\selkie-hub\lib'
    if (-not (Test-Path $lib)) { throw "missing $lib" }

    $javaHome = $env:JAVA_HOME
    if (-not $javaHome) { throw 'JAVA_HOME must point at a JDK 21' }
    $jpackage = Join-Path $javaHome 'bin\jpackage.exe'
    $jlink = Join-Path $javaHome 'bin\jlink.exe'
    if (-not (Test-Path $jpackage)) { throw "jpackage not found under $javaHome" }

    $stage = Join-Path $repo 'build\package\stage'
    if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
    New-Item -ItemType Directory -Force -Path $stage | Out-Null
    Copy-Item (Join-Path $lib '*.jar') $stage
    # The shipped data fixtures must sit next to the executable: the host resolves
    # data/ relative to the working directory, so bundle the whole tree.
    Copy-Item (Join-Path $repo 'data') (Join-Path $stage 'data') -Recurse

    # Trim the runtime: the full JDK is ~150 MB and most of it (compiler, ct.sym,
    # debug attributes, unused modules) never runs. jlink keeps only what is needed.
    Write-Host '[package] 2/5 building a trimmed runtime with jlink...'
    $runtime = Join-Path $repo 'build\package\runtime'
    if (Test-Path $runtime) { Remove-Item -Recurse -Force $runtime }
    $jlinkArgs = @(
        # No java.desktop: qml4j's text goes through Skia's own font manager and LWJGL/GLFW
        # talks straight to Win32, so AWT/Swing are dead weight here. Verified by running
        # the packaged exe (see docs/packaging.md).
        '--add-modules', 'java.base,java.logging,java.net.http,jdk.httpserver,jdk.unsupported',
        '--strip-debug', '--no-man-pages', '--no-header-files', '--compress=zip-6',
        '--output', $runtime
    )
    & $jlink @jlinkArgs
    if ($LASTEXITCODE -ne 0) { throw 'jlink failed' }
    $runtimeMb = [Math]::Round(((Get-ChildItem $runtime -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
    Write-Host ("[package] trimmed runtime: {0} MB" -f $runtimeMb)

    Write-Host '[package] 3/5 running jpackage...'
    $appDir = Join-Path $repo 'build\package\SelkieHub'
    if (Test-Path $appDir) { Remove-Item -Recurse -Force $appDir }
    $mainJar = (Get-ChildItem $stage -Filter '*.jar' | Select-Object -First 1).Name

    $pkgArgs = @(
        '--type', 'app-image',
        '--name', 'SelkieHub',
        '--input', $stage,
        '--main-jar', $mainJar,
        '--main-class', 'selkie.host.Main',
        '--dest', (Join-Path $repo 'build\package'),
        '--runtime-image', $runtime,
        '--java-options', '-Xmx256m',
        '--java-options', '-XX:+UseSerialGC',
        '--java-options', '-XX:TieredStopAtLevel=1',
        '--win-console'
    )
    & $jpackage @pkgArgs
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }

    Write-Host '[package] 4/5 measuring the app-image...'
    $exe = Join-Path $appDir 'SelkieHub.exe'
    if (-not (Test-Path $exe)) { throw "missing $exe" }
    $appSizeMb = [Math]::Round(((Get-ChildItem $appDir -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
    Write-Host ("[package] app-image size: {0} MB" -f $appSizeMb)

    Write-Host '[package] 5/5 zipping...'
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    $zip = Join-Path $OutDir 'SelkieHub-windows-x64.zip'
    if (Test-Path $zip) { Remove-Item -Force $zip }
    Compress-Archive -Path $appDir -DestinationPath $zip -CompressionLevel Optimal
    $zipMb = [Math]::Round((Get-Item $zip).Length / 1MB, 1)

    Write-Host '[package] done.'
    Write-Host ("[package] zip: {0} ({1} MB)" -f $zip, $zipMb)
    Write-Host ("[package] run: {0}" -f $exe)
}
finally {
    Pop-Location
}
