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
      artefact and matches the "unzip and double-click" acceptance.

.PARAMETER OutDir
  Output directory for the finished zip. Defaults to build/package.
#>
[CmdletBinding()]
param(
    [string]$OutDir = (Join-Path $PSScriptRoot '..\build\package')
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
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
    if (-not (Test-Path $jpackage)) { throw "jpackage not found under $javaHome" }

    $stage = Join-Path $repo 'build\package\stage'
    if (Test-Path $stage) { Remove-Item -Recurse -Force $stage }
    New-Item -ItemType Directory -Force -Path $stage | Out-Null

    # jpackage wants one input directory; the app jar plus its runtime dependencies.
    Copy-Item (Join-Path $lib '*.jar') $stage
    # The shipped data fixtures must sit next to the executable: the host resolves
    # data/ relative to the working directory, so bundle the whole tree.
    Copy-Item (Join-Path $repo 'data') (Join-Path $stage 'data') -Recurse

    Write-Host '[package] 2/5 running jpackage...'
    $appDir = Join-Path $repo 'build\package\SelkieHub'
    if (Test-Path $appDir) { Remove-Item -Recurse -Force $appDir }

    $mainJar = Get-ChildItem $stage -Filter 'host-*.jar' | Select-Object -First 1
    $mainJarName = if ($mainJar) { $mainJar.Name } else { (Get-ChildItem $stage -Filter '*.jar' | Select-Object -First 1).Name }

    & $jpackage `
        --type app-image `
        --name SelkieHub `
        --input $stage `
        --main-jar $mainJarName `
        --main-class selkie.host.Main `
        --dest (Join-Path $repo 'build\package') `
        --java-options '-Xmx256m' `
        --java-options '-XX:+UseSerialGC' `
        --java-options '-XX:TieredStopAtLevel=1' `
        --win-console
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }

    Write-Host '[package] 3/5 measuring the app-image...'
    $exe = Join-Path $appDir 'SelkieHub.exe'
    if (-not (Test-Path $exe)) { throw "missing $exe" }
    $appSizeMb = [Math]::Round(((Get-ChildItem $appDir -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
    Write-Host ("[package] app-image size: {0} MB" -f $appSizeMb)

    Write-Host '[package] 4/5 zipping...'
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
    $zip = Join-Path $OutDir 'SelkieHub-windows-x64.zip'
    if (Test-Path $zip) { Remove-Item -Force $zip }
    Compress-Archive -Path $appDir -DestinationPath $zip -CompressionLevel Optimal
    $zipMb = [Math]::Round((Get-Item $zip).Length / 1MB, 1)

    Write-Host '[package] 5/5 done.'
    Write-Host ("[package] zip: {0} ({1} MB)" -f $zip, $zipMb)
    Write-Host ("[package] run: {0}" -f $exe)
}
finally {
    Pop-Location
}
