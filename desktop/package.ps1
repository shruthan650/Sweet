param(
    [string]$Maven = "mvn"
)

$ErrorActionPreference = "Stop"
$desktopRoot = $PSScriptRoot
$repositoryRoot = Split-Path -Parent $desktopRoot
$pom = Join-Path $desktopRoot "pom.xml"
$target = Join-Path $desktopRoot "target"
$inputDirectory = Join-Path $target "package-input"
$destination = Join-Path $target "dist"

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage was not found. Install a JDK 25 and add its bin directory to PATH."
}

Push-Location $repositoryRoot
try {
    & $Maven -f $pom -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed while building the desktop application."
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

Copy-Item $applicationJar.FullName $inputDirectory -Force
New-Item -ItemType Directory -Force $destination | Out-Null
$applicationVersion = ([xml](Get-Content $pom -Raw)).project.version -replace "-SNAPSHOT$", ""
$applicationImage = Join-Path $destination "Sweet"
$targetPrefix = [IO.Path]::GetFullPath($target).TrimEnd([IO.Path]::DirectorySeparatorChar) +
    [IO.Path]::DirectorySeparatorChar
$resolvedImage = [IO.Path]::GetFullPath($applicationImage)
if (-not $resolvedImage.StartsWith($targetPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "The application image path must remain under the desktop target directory."
}
if (Test-Path -LiteralPath $applicationImage) {
    Remove-Item -LiteralPath $applicationImage -Recurse -Force
}

$jpackageArguments = @(
    "--type", "app-image",
    "--name", "Sweet",
    "--input", $inputDirectory,
    "--main-jar", $applicationJar.Name,
    "--main-class", "com.shruthan.sweet.desktop.SweetDesktopApplication",
    "--java-options", "--enable-native-access=ALL-UNNAMED",
    "--dest", $destination
)
# macOS rejects 0.x app versions. Since --app-version is optional, keep
# jpackage's valid default on macOS and use the Maven version elsewhere.
if (-not [Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
    [Runtime.InteropServices.OSPlatform]::OSX
)) {
    $jpackageArguments += @("--app-version", $applicationVersion)
}

& jpackage @jpackageArguments
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed to create the desktop application image."
}

Write-Output "Desktop app image created at $(Join-Path $destination 'Sweet')"
