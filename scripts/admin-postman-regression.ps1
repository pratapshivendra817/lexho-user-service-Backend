param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$AdminToken = $env:POSTMAN_ADMIN_TOKEN,
    [string]$OutDir = "target/postman-regression",
    [switch]$IncludeMutations
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AdminToken)) {
    throw "Admin token is required. Pass -AdminToken or set POSTMAN_ADMIN_TOKEN."
}

$outFull = if ([System.IO.Path]::IsPathRooted($OutDir)) { $OutDir } else { Join-Path (Get-Location) $OutDir }
New-Item -ItemType Directory -Force -Path $outFull | Out-Null

$headers = @{
    Authorization = "Bearer $AdminToken"
}

$tests = @(
    @{ Name = "Dashboard Stats"; Method = "GET"; Path = "/api/admin/dashboard/stats"; Expected = @(200) },
    @{ Name = "Dashboard Revenue"; Method = "GET"; Path = "/api/admin/dashboard/revenue"; Expected = @(200) },
    @{ Name = "Services Distribution"; Method = "GET"; Path = "/api/admin/dashboard/services-distribution"; Expected = @(200) },
    @{ Name = "Service Summary"; Method = "GET"; Path = "/api/admin/dashboard/service-summary"; Expected = @(200) },
    @{ Name = "Top Services"; Method = "GET"; Path = "/api/admin/dashboard/top-services"; Expected = @(200) },
    @{ Name = "Service Analytics"; Method = "GET"; Path = "/api/admin/dashboard/service-analytics"; Expected = @(200) },
    @{ Name = "List Customers"; Method = "GET"; Path = "/api/admin/customers?page=0&size=10&search="; Expected = @(200) },
    @{ Name = "Get Customer"; Method = "GET"; Path = "/api/admin/customers/1"; Expected = @(200,404) },
    @{ Name = "Account Delete Requests"; Method = "GET"; Path = "/api/admin/customers/account-delete-requests?page=0&size=10"; Expected = @(200) },
    @{ Name = "List Bookings"; Method = "GET"; Path = "/api/v1/admin/booking-management?page=0&size=10&search=&status="; Expected = @(200) },
    @{ Name = "Get Booking"; Method = "GET"; Path = "/api/v1/admin/booking-management/1"; Expected = @(200,404) },
    @{ Name = "Booking Live Tracking"; Method = "GET"; Path = "/api/v1/admin/booking-management/1/tracking/live"; Expected = @(200,404) },
    @{ Name = "Booking Navigation"; Method = "GET"; Path = "/api/v1/admin/booking-management/1/navigation"; Expected = @(200,404) },
    @{ Name = "Payment Reconciliation"; Method = "GET"; Path = "/api/v1/admin/booking-management/1/payment/reconciliation"; Expected = @(200,404) },
    @{ Name = "Partner Settlements"; Method = "GET"; Path = "/api/admin/partners/settlements?page=0&size=10"; Expected = @(200) },
    @{ Name = "Partner Withdrawals"; Method = "GET"; Path = "/api/admin/partners/withdrawals?page=0&size=10"; Expected = @(200) },
    @{ Name = "Warning Disputes"; Method = "GET"; Path = "/api/admin/partners/warnings/disputes?page=0&size=10"; Expected = @(200) },
    @{ Name = "List Services"; Method = "GET"; Path = "/api/admin/services?page=1&limit=10&search=&status="; Expected = @(200) },
    @{ Name = "List Categories"; Method = "GET"; Path = "/api/admin/categories?page=0&size=10"; Expected = @(200) },
    @{ Name = "List Banners"; Method = "GET"; Path = "/api/admin/banners?page=1&limit=10&search=&status="; Expected = @(200) },
    @{ Name = "Popular Services"; Method = "GET"; Path = "/api/admin/popular-services?categoryId=29"; Expected = @(200) },
    @{ Name = "Cart Config"; Method = "GET"; Path = "/api/admin/cart-config"; Expected = @(200) },
    @{ Name = "Cook Family Sizes"; Method = "GET"; Path = "/api/admin/cook-family-sizes"; Expected = @(200) },
    @{ Name = "Serviceable Areas"; Method = "GET"; Path = "/api/admin/serviceable-areas?page=0&size=10"; Expected = @(200) },
    @{ Name = "Serviceable Area Search"; Method = "GET"; Path = "/api/admin/serviceable-areas/search?q=delhi&page=0&size=10"; Expected = @(200) },
    @{ Name = "Geo Cities"; Method = "GET"; Path = "/api/admin/geo/cities"; Expected = @(200) },
    @{ Name = "Geo Zones"; Method = "GET"; Path = "/api/admin/geo/zones"; Expected = @(200) },
    @{ Name = "Emergency Incidents"; Method = "GET"; Path = "/api/admin/emergency/incidents?page=0&size=10"; Expected = @(200) },
    @{ Name = "Profile Content"; Method = "GET"; Path = "/api/admin/profile-content"; Expected = @(200) },
    @{ Name = "Settings"; Method = "GET"; Path = "/api/admin/settings"; Expected = @(200) },
    @{ Name = "Monitoring Dashboard"; Method = "GET"; Path = "/api/admin/monitoring/dashboard"; Expected = @(200) },
    @{ Name = "Razorpay Webhooks"; Method = "GET"; Path = "/api/admin/razorpay/webhooks?page=0&size=10"; Expected = @(200) },
    @{ Name = "List Reviews"; Method = "GET"; Path = "/api/admin/reviews?page=1&limit=10&search=&status="; Expected = @(200) },
    @{ Name = "List Notifications"; Method = "GET"; Path = "/api/admin/notifications?page=1&limit=10&search=&status="; Expected = @(200) },
    @{ Name = "List Payouts"; Method = "GET"; Path = "/api/admin/payouts?page=1&limit=10&search=&status="; Expected = @(200) },
    @{ Name = "Activity Logs"; Method = "GET"; Path = "/api/admin/activity-logs?page=1&limit=10&search="; Expected = @(200) },
    @{ Name = "Audit Logs"; Method = "GET"; Path = "/api/admin/audit-logs?page=0&size=10"; Expected = @(200) },
    @{ Name = "Support Tickets"; Method = "GET"; Path = "/api/admin/support/tickets?page=1&limit=10&search=&status="; Expected = @(200) },
    @{ Name = "Customer Support Tickets"; Method = "GET"; Path = "/api/admin/support/customer/tickets?page=1&limit=10"; Expected = @(200) },
    @{ Name = "Partner Support Tickets"; Method = "GET"; Path = "/api/admin/support/partner/tickets?page=1&limit=10"; Expected = @(200) },
    @{ Name = "Support FAQ Categories"; Method = "GET"; Path = "/api/admin/support/faq-categories"; Expected = @(200) },
    @{ Name = "Admin Inbox Activity"; Method = "GET"; Path = "/api/admin/inbox/activity?page=0&size=20"; Expected = @(200) }
)

if ($IncludeMutations) {
    $tests += @(
        @{ Name = "Create Notification"; Method = "POST"; Path = "/api/admin/notifications/send"; Expected = @(200); Body = @{ title = "Service Update"; message = "Regression notification"; notificationType = "SYSTEM"; priority = "MEDIUM"; channels = @("IN_APP"); targetUserType = "CUSTOMER"; userIds = @(1); referenceId = 1; referenceType = "BOOKING"; redirectType = "NONE" } },
        @{ Name = "Update Cart Config"; Method = "PUT"; Path = "/api/admin/cart-config"; Expected = @(200); Body = @{ maxCartItems = 25; minServiceQuantity = 1; maxServiceQuantity = 10; minCleaningDurationHours = 1.0; maxCleaningDurationHours = 8.0; instantBookingWindowMinutes = 60; scheduledBookingWindowDays = 14; slotIntervalMinutes = 30; instantBookingEnabled = $true; scheduledBookingEnabled = $true; subscriptionBookingEnabled = $true; requireSlotRecheck = $true; rejectFullSlots = $true; maxBookingsPerSlot = 5; subscriptionCheckoutEnabled = $true; subscriptionPaymentRequired = $true; subscriptionAutoRenewEnabled = $true } },
        @{ Name = "Update Dispatch Radius"; Method = "PUT"; Path = "/api/admin/settings/dispatch-radius"; Expected = @(200); Body = @{ radiusKm = 2.0 } }
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
$jsonReport = Join-Path $outFull "admin-app-regression-$timestamp.json"
$txtReport = Join-Path $outFull "admin-app-regression-$timestamp.txt"

$results | ConvertTo-Json -Depth 10 | Set-Content -Path $jsonReport -Encoding UTF8
$results | Format-Table Name, Method, StatusCode, Expected, DurationMs, Passed -AutoSize | Out-String | Set-Content -Path $txtReport -Encoding UTF8

Write-Host "===== Admin App Regression Summary ====="
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
