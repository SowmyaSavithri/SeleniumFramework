<#
.SYNOPSIS
  Replaces plain-text secrets in config.properties with ENC(...) values (AES-256-GCM).

.DESCRIPTION
  Needs your own passphrase in the CONFIG_KEY environment variable (never stored in the repo).
  At test time ConfigReader decrypts ENC(...) values using the same CONFIG_KEY.
  Values that are already ENC(...) are left alone, so the script is safe to re-run.

.EXAMPLE
  $env:CONFIG_KEY = "your-own-passphrase"
  .\scripts\encrypt-config.ps1
  .\scripts\encrypt-config.ps1 -Keys valid.password,invalid.password
#>
param(
    [string[]]$Keys = @("valid.password", "invalid.password"),
    [string]$ConfigPath = (Join-Path $PSScriptRoot "..\src\test\resources\config.properties")
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrEmpty($env:CONFIG_KEY)) {
    throw "CONFIG_KEY is not set. Run:  `$env:CONFIG_KEY = 'your-own-passphrase'  and try again."
}

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ConfigPath = (Resolve-Path $ConfigPath).Path

Push-Location $projectRoot
try {
    mvn -q test-compile
    if ($LASTEXITCODE -ne 0) { throw "mvn test-compile failed" }

    $lines = [System.IO.File]::ReadAllLines($ConfigPath)
    $changed = @()
    for ($i = 0; $i -lt $lines.Length; $i++) {
        foreach ($key in $Keys) {
            $prefix = "$key="
            if ($lines[$i].StartsWith($prefix)) {
                $value = $lines[$i].Substring($prefix.Length).Trim()
                if ($value -and -not ($value.StartsWith("ENC(") -and $value.EndsWith(")"))) {
                    $enc = (java -cp target\test-classes com.iap.utils.SecretCrypto $value).Trim()
                    if ($LASTEXITCODE -ne 0 -or -not $enc.StartsWith("ENC(")) { throw "Encryption failed for $key" }
                    $lines[$i] = "$prefix$enc"
                    $changed += $key
                }
            }
        }
    }

    [System.IO.File]::WriteAllLines($ConfigPath, $lines, (New-Object System.Text.UTF8Encoding($false)))
    if ($changed.Count -gt 0) { "Encrypted: " + ($changed -join ", ") } else { "Nothing to encrypt (already ENC or key not found)." }
}
finally {
    Pop-Location
}
