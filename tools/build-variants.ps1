param(
    [string]$JavaHome = 'C:\Users\ABOBUS\.jdks\ms-17.0.17',
    [switch]$Offline
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
Push-Location -LiteralPath $projectRoot
try {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;$previousPath"
    foreach ($variant in @('true', 'false')) {
        $gradleArguments = @('build', "-PwithVeil=$variant", '--no-daemon')
        if ($Offline) { $gradleArguments += '--offline' }
        & .\gradlew.bat @gradleArguments
        if ($LASTEXITCODE -ne 0) { throw "Build failed: withVeil=$variant (exit $LASTEXITCODE)" }
    }
    Get-ChildItem -LiteralPath (Join-Path $projectRoot 'build/libs') -Filter '*.jar' |
        Where-Object { $_.Name -match '-(veil|no-veil)\.jar$' }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
    Pop-Location
}
