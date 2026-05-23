param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$AdminEmail = "admin@lexho.com",
    [string]$AdminPassword = "",
    [string]$InternalApiToken = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
    $AdminPassword = Read-Host "Enter admin password for $AdminEmail"
}
if ([string]::IsNullOrWhiteSpace($InternalApiToken)) {
    $InternalApiToken = Read-Host "Enter INTERNAL_API_TOKEN for strict mode"
}

Write-Host "1) Running critical smoke (strict-friendly)"
& "$PSScriptRoot\critical-smoke.ps1" -BaseUrl $BaseUrl -AdminEmail $AdminEmail -AdminPassword $AdminPassword
if ($LASTEXITCODE -ne 0) {
    throw "critical-smoke failed"
}

Write-Host "2) Verifying /internal endpoint without token should be 401"
try {
    Invoke-WebRequest -Method Get -Uri "$BaseUrl/internal/bookings/users/1/summary" -ErrorAction Stop | Out-Null
    throw "Expected 401, but request succeeded without internal token"
} catch {
    $code = 0
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
        $code = [int]$_.Exception.Response.StatusCode
    }
    if ($code -ne 401) {
        throw "Expected 401 for internal endpoint without token, got $code"
    }
}

Write-Host "3) Verifying /internal endpoint with token should pass auth layer"
$headers = @{ "X-Internal-Token" = $InternalApiToken }
try {
    $resp = Invoke-WebRequest -Method Get -Uri "$BaseUrl/internal/bookings/users/1/summary" -Headers $headers -ErrorAction Stop
    $code = [int]$resp.StatusCode
    if ($code -ne 200) {
        throw "Expected 200 with internal token, got $code"
    }
} catch {
    $code = 0
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
        $code = [int]$_.Exception.Response.StatusCode
    }
    if ($code -ne 404) {
        throw "Expected 200 or 404 with internal token, got $code"
    }
}

Write-Host "Staging dry-run completed successfully."
