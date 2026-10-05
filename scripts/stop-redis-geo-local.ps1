param(
    [int]$Port = 6380
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
        Where-Object { $_.State -eq "Listen" }

if ($null -eq $connections) {
    Write-Host "[OK] No Redis-compatible GEO server is listening on localhost:$Port"
    exit 0
}

$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($processId in $processIds) {
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($null -eq $process) {
        continue
    }
    if ($process.ProcessName -notmatch "memurai") {
        throw "Port $Port is owned by $($process.ProcessName) ($processId), not Memurai. Refusing to stop it."
    }
    Stop-Process -Id $processId -Force
    Write-Host "[OK] Stopped Memurai process $processId on localhost:$Port"
}
