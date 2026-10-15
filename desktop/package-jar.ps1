param(
    [string]$Maven
)

$ErrorActionPreference = "Stop"
$desktopRoot = $PSScriptRoot
$repositoryRoot = Split-Path -Parent $desktopRoot
$target = Join-Path $desktopRoot "target"
$pom = Join-Path $desktopRoot "pom.xml"
$stage = Join-Path $target "portable-jar"
$destination = Join-Path $target "dist"

if ([string]::IsNullOrWhiteSpace($Maven)) {
    $Maven = Join-Path $repositoryRoot "mvnw.cmd"
}

Push-Location $repositoryRoot
try {
    & $Maven -f $pom -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed while packaging the desktop JAR."
    }
} finally {
    Pop-Location
}

$applicationJar = Get-ChildItem $target -Filter "sweet-desktop-*.jar" -File |
    Where-Object { $_.Name -notlike "*.original" } |
    Select-Object -First 1
if (-not $applicationJar) {
    throw "The desktop application JAR was not created."
}

$targetPrefix = [IO.Path]::GetFullPath($target).TrimEnd([IO.Path]::DirectorySeparatorChar) +
    [IO.Path]::DirectorySeparatorChar
$resolvedStage = [IO.Path]::GetFullPath($stage)
if (-not $resolvedStage.StartsWith($targetPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "The JAR staging path must remain under the desktop target directory."
}
if (Test-Path -LiteralPath $stage) {
    Remove-Item -LiteralPath $stage -Recurse -Force
}
New-Item -ItemType Directory -Force (Join-Path $stage "lib") | Out-Null
Copy-Item $applicationJar.FullName (Join-Path $stage "sweet-desktop.jar")
Copy-Item (Join-Path $target "package-input\*.jar") (Join-Path $stage "lib")

$launcher = @'
$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($env:SWEET_API_BASE_URL) -or
    [string]::IsNullOrWhiteSpace($env:SWEET_API_ACCESS_TOKEN)) {
    $envFile = Join-Path $PSScriptRoot ".env"
    if (Test-Path -LiteralPath $envFile) {
        foreach ($line in Get-Content -LiteralPath $envFile) {
            if ($line -match '^\s*(?:export\s+)?(SWEET_API_BASE_URL|SWEET_API_ACCESS_TOKEN)\s*=\s*(.*?)\s*$') {
                $key = $matches[1]
                $value = $matches[2]
                if ($value.Length -ge 2 -and
                    (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                        ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
                if ([string]::IsNullOrWhiteSpace((Get-Item "Env:$key" -ErrorAction SilentlyContinue).Value)) {
                    Set-Item -Path "Env:$key" -Value $value
                }
            }
        }
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java 25 is required. Install a Java 25 JDK and add java to PATH."
}
if ([string]::IsNullOrWhiteSpace($env:SWEET_API_BASE_URL)) {
    $env:SWEET_API_BASE_URL = "https://sweet-yqdu.onrender.com"
}
if ([string]::IsNullOrWhiteSpace($env:SWEET_API_ACCESS_TOKEN)) {
    throw "Set SWEET_API_ACCESS_TOKEN to the API token issued for the hosted Sweet service."
}
$appDirectory = $PSScriptRoot
$modulePath = Join-Path $appDirectory "lib"
$classPath = (Join-Path $appDirectory "sweet-desktop.jar") + ";" + (Join-Path $appDirectory "lib\*")
& java --enable-native-access=ALL-UNNAMED --module-path $modulePath --add-modules javafx.controls `
    --class-path $classPath com.shruthan.sweet.desktop.SweetDesktopApplication
exit $LASTEXITCODE
'@
Set-Content -LiteralPath (Join-Path $stage "run.ps1") -Value $launcher -Encoding utf8
Set-Content -LiteralPath (Join-Path $stage "env.example") `
    -Value "SWEET_API_BASE_URL=https://sweet-yqdu.onrender.com`nSWEET_API_ACCESS_TOKEN=" -Encoding utf8

New-Item -ItemType Directory -Force $destination | Out-Null
$archive = Join-Path $destination "sweet-desktop-windows-x64-jar.zip"
if (Test-Path -LiteralPath $archive) {
    Remove-Item -LiteralPath $archive -Force
}
Compress-Archive -Path (Join-Path $stage "*") -DestinationPath $archive
Write-Output "Portable JAR bundle created at $archive"
