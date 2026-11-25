# Usage: .\run_three_times.ps1 -Browser chrome -Headless false
param(
  [string]$Browser = "chrome",
  [string]$Headless = "false"
)
foreach ($i in 1..3) {
  Write-Host "=== Run $i of 3 on $Browser ==="
  mvn -q clean test "-Dbrowser=$Browser" "-Dheadless=$Headless"
  $dest = "run-reports\${Browser}_run$i"
  New-Item -ItemType Directory -Force -Path $dest | Out-Null
  Copy-Item -Recurse -Force "target\surefire-reports\*" $dest
  if (Test-Path "target\screenshots") { Copy-Item -Recurse -Force "target\screenshots" $dest }
}
Write-Host "Done. Open run-reports\${Browser}_run1\index.html (and run2, run3)."
