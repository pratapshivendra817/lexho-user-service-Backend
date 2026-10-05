param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$PartnerToken = $env:POSTMAN_PARTNER_TOKEN,
    [string]$OutDir = "target/postman-regression",
    [switch]$IncludeMutations
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($PartnerToken)) {
    throw "Partner token is required. Pass -PartnerToken or set POSTMAN_PARTNER_TOKEN."
}

$outFull = if ([System.IO.Path]::IsPathRooted($OutDir)) { $OutDir } else { Join-Path (Get-Location) $OutDir }
New-Item -ItemType Directory -Force -Path $outFull | Out-Null

$headers = @{
    Authorization = "Bearer $PartnerToken"
}

$tests = @(
    @{ Name = "Dashboard Stats"; Method = "GET"; Path = "/api/partner/dashboard/stats"; Expected = @(200) },
    @{ Name = "Partner Banners"; Method = "GET"; Path = "/api/partner/banners?page=0&size=5"; Expected = @(200) },
    @{ Name = "Earnings Summary"; Method = "GET"; Path = "/api/partner/earnings/summary"; Expected = @(200) },
    @{ Name = "Earning Transactions"; Method = "GET"; Path = "/api/partner/earnings/transactions?page=0&size=20"; Expected = @(200) },
    @{ Name = "Payouts"; Method = "GET"; Path = "/api/partner/payouts?page=0&size=20"; Expected = @(200) },
    @{ Name = "Performance"; Method = "GET"; Path = "/api/partner/performance"; Expected = @(200) },
    @{ Name = "Ratings"; Method = "GET"; Path = "/api/partner/ratings?page=0&size=20"; Expected = @(200) },
    @{ Name = "Leave"; Method = "GET"; Path = "/api/partner/leave"; Expected = @(200) },
    @{ Name = "Service Areas"; Method = "GET"; Path = "/api/partner/service-areas"; Expected = @(200) },
    @{ Name = "Warnings"; Method = "GET"; Path = "/api/partner/warnings"; Expected = @(200) },
    @{ Name = "Bank Details"; Method = "GET"; Path = "/api/partner/bank-details"; Expected = @(200) },
    @{ Name = "Withdrawals"; Method = "GET"; Path = "/api/partner/withdrawals?page=0&size=20"; Expected = @(200) },
    @{ Name = "Withdrawal Balance"; Method = "GET"; Path = "/api/partner/withdrawals/balance"; Expected = @(200) },
    @{ Name = "Activity Inbox"; Method = "GET"; Path = "/api/partner/inbox/activity?page=0&size=20"; Expected = @(200) },
    @{ Name = "Notifications"; Method = "GET"; Path = "/api/partner/notifications?page=0&size=20"; Expected = @(200) },
    @{ Name = "Notification Unread Count"; Method = "GET"; Path = "/api/partner/notifications/unread-count"; Expected = @(200) },
    @{ Name = "New Orders"; Method = "GET"; Path = "/api/orders/new?page=0&size=20"; Expected = @(200) },
    @{ Name = "Order History"; Method = "GET"; Path = "/api/orders/history?page=0&size=20"; Expected = @(200) },
    @{ Name = "Order Dashboard"; Method = "GET"; Path = "/api/orders/dashboard"; Expected = @(200) },
    @{ Name = "Partner New Orders Compatibility"; Method = "GET"; Path = "/api/partner/orders/new?page=0&size=20"; Expected = @(200) },
    @{ Name = "Partner Dispatch Requests"; Method = "GET"; Path = "/api/partner/dispatch/requests/new?page=0&size=20"; Expected = @(200) },
    @{ Name = "Support FAQs"; Method = "GET"; Path = "/api/partner/support/faqs"; Expected = @(200) },
    @{ Name = "Support Tickets"; Method = "GET"; Path = "/api/partner/support/tickets?page=0&size=20"; Expected = @(200) }
)

if ($IncludeMutations) {
    $deviceToken = "postman-" + [guid]::NewGuid().ToString("N")
    $tests += @(
        @{ Name = "Update Availability"; Method = "PUT"; Path = "/api/partner/availability"; Expected = @(200); Body = @{ online = $true; available = $true } },
        @{ Name = "Register Device Token"; Method = "POST"; Path = "/api/partner/device-token"; Expected = @(200); Body = @{ token = $deviceToken; deviceType = "ANDROID" } },
        @{ Name = "Create Support Ticket"; Method = "POST"; Path = "/api/partner/support/tickets"; Expected = @(200); Body = @{ subject = "Partner regression ticket"; message = "Created by partner regression runner"; priority = "LOW" } }
    )
}

$results = New-Object System.Collections.Generic.List[object]

foreach ($test in $tests) {
    $uri = "$BaseUrl$($test.Path)"
    $body = $null
    if ($test.ContainsKey("Body")) {
        $body = ($test.Body | ConvertTo-Json -Depth 8)
    }

    $started = Get-Date
    $statusCode = $null
    $errorMessage = $null
    try {
        $params = @{
            Uri = $uri
            Method = $test.Method
            Headers = $headers
            TimeoutSec = 30
        }
        if ($body -ne $null) {
            $params.ContentType = "application/json"
            $params.Body = $body
        }
        $response = Invoke-WebRequest @params -UseBasicParsing
        $statusCode = [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorMessage = $reader.ReadToEnd()
        } else {
            $errorMessage = $_.Exception.Message
        }
    }

    $elapsedMs = [int]((Get-Date) - $started).TotalMilliseconds
    $results.Add([pscustomobject]@{
        Name = $test.Name
        Method = $test.Method
        Path = $test.Path
        StatusCode = $statusCode
        Expected = ($test.Expected -join ",")
        DurationMs = $elapsedMs
        Passed = ($test.Expected -contains $statusCode)
        Error = $errorMessage
    })
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonReport = Join-Path $outFull "partner-app-regression-$timestamp.json"
$txtReport = Join-Path $outFull "partner-app-regression-$timestamp.txt"

$results | ConvertTo-Json -Depth 10 | Set-Content -Path $jsonReport -Encoding UTF8
$results | Format-Table Name, Method, StatusCode, Expected, DurationMs, Passed -AutoSize | Out-String | Set-Content -Path $txtReport -Encoding UTF8

Write-Host "===== Partner App Regression Summary ====="
$results | Format-Table Name, Method, StatusCode, Expected, DurationMs, Passed -AutoSize
Write-Host "JSON report: $jsonReport"
Write-Host "Text report: $txtReport"

$failed = @($results | Where-Object { -not $_.Passed })
if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Host "Failed checks:"
    $failed | Format-Table Name, Method, Path, StatusCode, Expected -AutoSize
    exit 1
}
