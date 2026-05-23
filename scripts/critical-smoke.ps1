param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$AdminEmail = "admin@lexho.com",
    [string]$AdminPassword = "",
    [string]$AdminToken = "",
    [string]$CustomerToken = "",
    [long]$CategoryId = 1,
    [switch]$SkipAdminOtpPrompt
)

$ErrorActionPreference = "Stop"

$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [int]$StatusCode,
        [string]$Details
    )
    $results.Add([PSCustomObject]@{
            Name = $Name
            Passed = $Passed
            StatusCode = $StatusCode
            Details = $Details
        })
}

function Invoke-Check {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = $null,
        [string]$Body = $null,
        [int[]]$ExpectedStatus = @(200),
        [string]$ContentType = "application/json"
    )

    try {
        $params = @{
            Method = $Method
            Uri = $Url
            ErrorAction = "Stop"
        }
        if ($Headers) { $params.Headers = $Headers }
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = $ContentType
        }

        $response = Invoke-WebRequest @params
        $code = [int]$response.StatusCode
        $ok = $ExpectedStatus -contains $code
        Add-Result -Name $Name -Passed $ok -StatusCode $code -Details "OK"
        return $response
    } catch {
        $code = 0
        $msg = $_.Exception.Message
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $code = [int]$_.Exception.Response.StatusCode
            $msg = "HTTP $code"
        }
        $ok = $ExpectedStatus -contains $code
        Add-Result -Name $Name -Passed $ok -StatusCode $code -Details $msg
        return $null
    }
}

Write-Host "Running critical smoke checks against $BaseUrl"

# Public/customer-open critical reads
Invoke-Check -Name "Public: GET /api/customer/profile-content" -Method "GET" -Url "$BaseUrl/api/customer/profile-content"
Invoke-Check -Name "Public: GET /api/customer/home/sections" -Method "GET" -Url "$BaseUrl/api/customer/home/sections"
Invoke-Check -Name "Public: GET /api/customer/services/catalog" -Method "GET" -Url "$BaseUrl/api/customer/services/catalog?page=0&size=5"
Invoke-Check -Name "Public: GET /api/customer/services?categoryId=$CategoryId" -Method "GET" -Url "$BaseUrl/api/customer/services?categoryId=$CategoryId&page=0&size=5"
Invoke-Check -Name "Public: GET /api/customer/services/popular?categoryId=$CategoryId" -Method "GET" -Url "$BaseUrl/api/customer/services/popular?categoryId=$CategoryId"

# Admin auth + protected admin checks
if ([string]::IsNullOrWhiteSpace($AdminToken)) {
    if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
        $AdminPassword = Read-Host "Enter admin password for $AdminEmail"
    }
    $loginBody = @{
        email = $AdminEmail
        password = $AdminPassword
    } | ConvertTo-Json
    $null = Invoke-Check -Name "Admin: POST /api/admin/login" -Method "POST" -Url "$BaseUrl/api/admin/login" -Body $loginBody

    $shouldPromptOtp = -not $SkipAdminOtpPrompt.IsPresent
    if ($env:CI -eq "true") {
        $shouldPromptOtp = $false
    }

    $otp = ""
    if ($shouldPromptOtp) {
        $otp = Read-Host "Enter OTP for $AdminEmail (leave blank to skip admin protected checks)"
    }
    if (-not [string]::IsNullOrWhiteSpace($otp)) {
        $verifyBody = @{
            email = $AdminEmail
            otp = $otp
        } | ConvertTo-Json
        $verify = Invoke-Check -Name "Admin: POST /api/admin/verify-otp" -Method "POST" -Url "$BaseUrl/api/admin/verify-otp" -Body $verifyBody
        if ($verify) {
            try {
                $verifyJson = $verify.Content | ConvertFrom-Json
                if ($verifyJson.accessToken) {
                    $AdminToken = [string]$verifyJson.accessToken
                }
            } catch {
                Add-Result -Name "Admin: parse verify-otp token" -Passed $false -StatusCode 0 -Details "Failed to parse response JSON"
            }
        }
    } else {
        Add-Result -Name "Admin: protected checks skipped (OTP not provided)" -Passed $true -StatusCode 0 -Details "Skipped by mode"
    }
}

if (-not [string]::IsNullOrWhiteSpace($AdminToken)) {
    $adminHeaders = @{ Authorization = "Bearer $AdminToken" }
    Invoke-Check -Name "Admin: GET /api/admin/dashboard/stats" -Method "GET" -Url "$BaseUrl/api/admin/dashboard/stats" -Headers $adminHeaders
    Invoke-Check -Name "Admin: GET /api/admin/services?page=1&limit=5" -Method "GET" -Url "$BaseUrl/api/admin/services?page=1&limit=5" -Headers $adminHeaders
    Invoke-Check -Name "Admin: GET /api/ops/summary" -Method "GET" -Url "$BaseUrl/api/ops/summary" -Headers $adminHeaders
}

# Optional customer-protected checks (if token available)
if (-not [string]::IsNullOrWhiteSpace($CustomerToken)) {
    $customerHeaders = @{ Authorization = "Bearer $CustomerToken" }
    Invoke-Check -Name "Customer: GET /api/customer/profile" -Method "GET" -Url "$BaseUrl/api/customer/profile" -Headers $customerHeaders
    Invoke-Check -Name "Customer: GET /api/customer/addresses" -Method "GET" -Url "$BaseUrl/api/customer/addresses" -Headers $customerHeaders
}

$passCount = ($results | Where-Object { $_.Passed }).Count
$failCount = $results.Count - $passCount

Write-Host ""
Write-Host "===== Critical Smoke Summary ====="
$results | Format-Table Name, Passed, StatusCode, Details -AutoSize
Write-Host "Total: $($results.Count), Passed: $passCount, Failed: $failCount"

if ($failCount -gt 0) {
    exit 1
}
