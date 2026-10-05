param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$CustomerToken = $env:POSTMAN_CUSTOMER_TOKEN,
    [string]$OutDir = "target/postman-regression",
    [switch]$IncludeMutations
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($CustomerToken)) {
    throw "Customer token is required. Pass -CustomerToken or set POSTMAN_CUSTOMER_TOKEN."
}

$outFull = if ([System.IO.Path]::IsPathRooted($OutDir)) { $OutDir } else { Join-Path (Get-Location) $OutDir }
New-Item -ItemType Directory -Force -Path $outFull | Out-Null

$headers = @{
    Authorization = "Bearer $CustomerToken"
}

$tests = @(
    @{ Name = "Public Home Dashboard"; Method = "GET"; Path = "/api/home/dashboard"; Auth = $false; Expected = @(200) },
    @{ Name = "Public Banners"; Method = "GET"; Path = "/api/banners?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Public Categories"; Method = "GET"; Path = "/api/categories?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Public Popular Services"; Method = "GET"; Path = "/api/services/popular?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Public Recommended Services"; Method = "GET"; Path = "/api/services/recommended?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Public Quick Services"; Method = "GET"; Path = "/api/services/quick?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Public Search Services"; Method = "GET"; Path = "/api/search/services?keyword=clean&page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Banners"; Method = "GET"; Path = "/api/customer/banners"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Service Catalog"; Method = "GET"; Path = "/api/customer/services/catalog?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Services By Category"; Method = "GET"; Path = "/api/customer/services?categoryId=29&page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Popular By Category"; Method = "GET"; Path = "/api/customer/services/popular?categoryId=29&page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Popular Cook"; Method = "GET"; Path = "/api/customer/services/popular/cook"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Popular Cleaning"; Method = "GET"; Path = "/api/customer/services/popular/cleaning"; Auth = $false; Expected = @(200) },
    @{ Name = "Cart Config"; Method = "GET"; Path = "/api/customer/cart/config"; Auth = $true; Expected = @(200) },
    @{ Name = "Cook Family Sizes"; Method = "GET"; Path = "/api/customer/cook-family-sizes"; Auth = $true; Expected = @(200) },
    @{ Name = "Coupon List"; Method = "GET"; Path = "/api/coupon/list?page=0&size=20"; Auth = $false; Expected = @(200) },
    @{ Name = "Customer Profile"; Method = "GET"; Path = "/api/customer/profile"; Auth = $true; Expected = @(200) },
    @{ Name = "Legacy User Profile"; Method = "GET"; Path = "/api/user/profile"; Auth = $true; Expected = @(200) },
    @{ Name = "Profile Content"; Method = "GET"; Path = "/api/customer/profile-content"; Auth = $false; Expected = @(200) },
    @{ Name = "Profile Addresses"; Method = "GET"; Path = "/api/customer/addresses"; Auth = $true; Expected = @(200) },
    @{ Name = "Customer Cart"; Method = "GET"; Path = "/api/customer/cart"; Auth = $true; Expected = @(200) },
    @{ Name = "Legacy Cart Summary"; Method = "GET"; Path = "/api/cart/summary"; Auth = $true; Expected = @(200) },
    @{ Name = "Booking History"; Method = "GET"; Path = "/api/bookings/history?page=1&size=20"; Auth = $true; Expected = @(200) },
    @{ Name = "Notifications"; Method = "GET"; Path = "/api/notifications?page=0&size=20"; Auth = $true; Expected = @(200) },
    @{ Name = "Unread Notification Count"; Method = "GET"; Path = "/api/customer/notifications/unread-count"; Auth = $true; Expected = @(200) },
    @{ Name = "Activity Inbox"; Method = "GET"; Path = "/api/customer/inbox/activity?page=1&limit=20"; Auth = $true; Expected = @(200) },
    @{ Name = "Favorites"; Method = "GET"; Path = "/api/favorites/list?page=0&size=20"; Auth = $true; Expected = @(200) },
    @{ Name = "Support FAQs"; Method = "GET"; Path = "/api/support/faqs"; Auth = $false; Expected = @(200) },
    @{ Name = "Support History"; Method = "GET"; Path = "/api/support/history?page=1&limit=20"; Auth = $true; Expected = @(200) },
    @{ Name = "All Addresses"; Method = "GET"; Path = "/api/customer/location/all?page=0&size=10"; Auth = $true; Expected = @(200) },
    @{ Name = "Reverse Geocode"; Method = "POST"; Path = "/api/customer/location/reverse-geocode"; Auth = $true; Expected = @(200); Body = @{ latitude = 28.6123; longitude = 77.2290 } },
    @{ Name = "Serviceability Check"; Method = "POST"; Path = "/api/customer/location/serviceability/check"; Auth = $true; Expected = @(200); Body = @{ pincode = "110001"; latitude = 28.6123; longitude = 77.2290 } }
)

if ($IncludeMutations) {
    $tests += @(
        @{ Name = "Update Location"; Method = "POST"; Path = "/api/customer/location/update"; Auth = $true; Expected = @(200); Body = @{ latitude = 28.6123; longitude = 77.2290; accuracyMeters = 18.4; detectionSource = "GPS_AUTO" } },
        @{ Name = "Current Location"; Method = "GET"; Path = "/api/customer/location/current"; Auth = $true; Expected = @(200) },
        @{ Name = "Generate Referral"; Method = "POST"; Path = "/api/referral/generate"; Auth = $true; Expected = @(200) },
        @{ Name = "Create Support Ticket"; Method = "POST"; Path = "/api/support/create"; Auth = $true; Expected = @(200); Body = @{ subject = "Postman regression ticket"; message = "Created by customer regression runner"; priority = "LOW" } }
    )
}

$results = New-Object System.Collections.Generic.List[object]

foreach ($test in $tests) {
    $uri = "$BaseUrl$($test.Path)"
    $requestHeaders = if ($test.Auth) { $headers } else { @{} }
    $body = $null
    if ($test.ContainsKey("Body")) {
        $body = ($test.Body | ConvertTo-Json -Depth 8)
    }

    $started = Get-Date
    $statusCode = $null
    $ok = $false
    $errorMessage = $null
    try {
        $params = @{
            Uri = $uri
            Method = $test.Method
            Headers = $requestHeaders
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
    $ok = $test.Expected -contains $statusCode

    $results.Add([pscustomobject]@{
        Name = $test.Name
        Method = $test.Method
        Path = $test.Path
        StatusCode = $statusCode
        Expected = ($test.Expected -join ",")
        DurationMs = $elapsedMs
        Passed = $ok
        Error = $errorMessage
    })
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$jsonReport = Join-Path $outFull "customer-app-regression-$timestamp.json"
$txtReport = Join-Path $outFull "customer-app-regression-$timestamp.txt"

$results | ConvertTo-Json -Depth 10 | Set-Content -Path $jsonReport -Encoding UTF8
$results | Format-Table Name, Method, StatusCode, Expected, DurationMs, Passed -AutoSize | Out-String | Set-Content -Path $txtReport -Encoding UTF8

Write-Host "===== Customer App Regression Summary ====="
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
