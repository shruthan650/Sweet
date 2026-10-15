$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repositoryRoot ".env"
if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Missing .env at $envFile. Copy .env.example to .env and set SWEET_API_ACCESS_TOKEN."
}

foreach ($line in Get-Content -LiteralPath $envFile) {
    if ($line -match '^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') {
        $key = $matches[1]
        if ($key -notin @("SWEET_API_BASE_URL", "SWEET_API_ACCESS_TOKEN")) {
            continue
        }

        $value = $matches[2]
        if ($value.Length -ge 2 -and
            (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        Set-Item -Path "Env:$key" -Value $value
    }
}

if ([string]::IsNullOrWhiteSpace($env:SWEET_API_BASE_URL)) {
    $env:SWEET_API_BASE_URL = "https://sweet-yqdu.onrender.com"
}
if ([string]::IsNullOrWhiteSpace($env:SWEET_API_ACCESS_TOKEN)) {
    throw "SWEET_API_ACCESS_TOKEN is missing or empty in .env. Add the same client token configured on Render."
}

Push-Location $repositoryRoot
try {
    & (Join-Path $repositoryRoot "mvnw.cmd") -f "desktop\pom.xml" javafx:run
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
