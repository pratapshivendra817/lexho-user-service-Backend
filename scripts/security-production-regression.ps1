param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$OutDir = "target/security-regression",
    [switch]$IncludeOtpLive,
    [string]$CustomerOtp,
    [string]$PartnerOtp
)

$ErrorActionPreference = "Stop"

$outFull = if ([System.IO.Path]::IsPathRooted($OutDir)) { $OutDir } else { Join-Path (Get-Location) $OutDir }
New-Item -ItemType Directory -Force -Path $outFull | Out-Null

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)
    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-TestJwt {
    param(
        [string]$Role,
        [long]$UserId,
        [string]$Subject,
        [string]$Email,
        [int]$ExpiresInSeconds = 86400,
        [string]$Secret = $env:JWT_SECRET,
        [string]$Issuer = $env:JWT_ISSUER,
        [string]$Audience = $env:JWT_AUDIENCE
    )

    if ([string]::IsNullOrWhiteSpace($Secret)) {
        throw "JWT_SECRET is required for security regression token generation."
    }
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $claims = [ordered]@{
        role = $Role
        userId = $UserId
        sub = $Subject
        iss = $(if ($Issuer) { $Issuer } else { "lexho-user-service" })
        aud = $(if ($Audience) { $Audience } else { "lexho-clients" })
        iat = $now
        exp = $now + $ExpiresInSeconds
    }
    if (-not [string]::IsNullOrWhiteSpace($Email)) {
        $claims.email = $Email
    }

    $headerJson = @{ alg = "HS256"; typ = "JWT" } | ConvertTo-Json -Compress
    $payloadJson = $claims | ConvertTo-Json -Compress
    $header = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($headerJson))
    $payload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payloadJson))
    $data = "$header.$payload"
    $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
    $signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data)))
    return "$data.$signature"
}

function Invoke-SecurityRequest {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [int[]]$Expected,
        [object]$Body,
        [hashtable]$ExtraHeaders
    )

    $headers = @{}
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }
    if ($ExtraHeaders) {
        foreach ($key in $ExtraHeaders.Keys) {
            $headers[$key] = $ExtraHeaders[$key]
        }
    }

    $statusCode = $null
    $errorMessage = $null
    $responseText = $null
    $started = Get-Date
    try {
        $params = @{
            Uri = "$BaseUrl$Path"
            Method = $Method
            Headers = $headers
            TimeoutSec = 30
            UseBasicParsing = $true
        }
        if ($null -ne $Body) {
            $params.ContentType = "application/json"
            $params.Body = ($Body | ConvertTo-Json -Depth 12)
        }
        $response = Invoke-WebRequest @params
        $statusCode = [int]$response.StatusCode
        $responseText = [string]$response.Content
    } catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseText = $reader.ReadToEnd()
            $errorMessage = $responseText
        } else {
            $errorMessage = $_.Exception.Message
        }
    }

    [pscustomobject]@{
        Name = $Name
        Method = $Method
        Path = $Path
        StatusCode = $statusCode
        Expected = ($Expected -join ",")
        DurationMs = [int]((Get-Date) - $started).TotalMilliseconds
        Passed = ($Expected -contains $statusCode)
        Error = $errorMessage
        ReflectedProbe = (($responseText -match "<script") -or ($responseText -match "onerror="))
    }
}

function Invoke-OtpLiveCheck {
    param(
        [string]$Name,
        [string]$Mobile,
        [string]$Role,
        [string]$Otp
    )

    $send = Invoke-SecurityRequest -Name "$Name OTP Send" -Method "POST" -Path "/otp/send" `
        -Expected @(200, 400) -Body @{ mobile = $Mobile; role = $Role }
    if ([string]::IsNullOrWhiteSpace($Otp)) {
        return @(
            $send,
            [pscustomobject]@{
                Name = "$Name OTP Verify"
                Method = "POST"
                Path = "/otp/verify"
                StatusCode = $null
                Expected = "200"
                DurationMs = 0
                Passed = $false
                Error = "OTP value not supplied. Pass -CustomerOtp/-PartnerOtp from the running app delivery channel."
                ReflectedProbe = $false
            }
        )
    }

    $verify = Invoke-SecurityRequest -Name "$Name OTP Verify" -Method "POST" -Path "/otp/verify" `
        -Expected @(200) -Body @{ mobile = $Mobile; role = $Role; otp = $Otp }
    return @($send, $verify)
}

function Invoke-SecretScan {
    $patterns = @(
        @{ Name = "Private key"; Regex = "-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----" },
        @{ Name = "AWS access key"; Regex = "AKIA[0-9A-Z]{16}" },
        @{ Name = "GitHub token"; Regex = "gh[pousr]_[A-Za-z0-9_]{36,255}" },
        @{ Name = "Stripe live key"; Regex = "sk_live_[A-Za-z0-9]{16,}" },
        @{ Name = "Razorpay live key"; Regex = "rzp_live_[A-Za-z0-9]{10,}" },
        @{ Name = "Hardcoded password assignment"; Regex = "(?i)(password|secret|token)\s*[:=]\s*['""](?![\$%<{])[^'""]{12,}['""]" }
    )
    $trackedFiles = @(git ls-files)
    $untrackedFiles = @(git ls-files --others --exclude-standard)
    $files = @($trackedFiles + $untrackedFiles | Sort-Object -Unique)
    $findings = New-Object System.Collections.Generic.List[object]
    foreach ($file in $files) {
        $normalized = $file.Replace("\", "/")
        if (-not (Test-Path $file) -or $normalized -match "(^|/)target/|(^|/)\.git/|(^|/)\.idea/|\.class$|\.png$|\.jpg$|\.jpeg$|\.gif$|\.ico$|\.jar$") {
            continue
        }
        if ($normalized -match "src/test/") {
            continue
        }
        $lineNumber = 0
        foreach ($line in Get-Content $file -ErrorAction SilentlyContinue) {
            $lineNumber++
            foreach ($pattern in $patterns) {
                if ($line -match $pattern.Regex) {
                    $findings.Add([pscustomobject]@{
                        Rule = $pattern.Name
                        File = $file
                        Line = $lineNumber
                    })
                }
            }
        }
    }
    return $findings.ToArray()
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

$customerToken = New-TestJwt -Role "CUSTOMER" -UserId 4 -Subject "9876543210"
$partnerToken = New-TestJwt -Role "PARTNER" -UserId 4 -Subject "9876543210"
$adminToken = New-TestJwt -Role "ADMIN" -UserId 2 -Subject $env:ADMIN_SEED_EMAIL -Email $env:ADMIN_SEED_EMAIL
$expiredCustomerToken = New-TestJwt -Role "CUSTOMER" -UserId 4 -Subject "9876543210" -ExpiresInSeconds -60
$tamperedToken = $customerToken.Substring(0, $customerToken.Length - 2) + "xx"
$wrongSecretToken = New-TestJwt -Role "CUSTOMER" -UserId 4 -Subject "9876543210" -Secret "wrong-secret-wrong-secret-wrong-secret-32"

$checks = New-Object System.Collections.Generic.List[object]

$checks.Add((Invoke-SecurityRequest -Name "Customer token blocked from admin dashboard" -Method "GET" -Path "/api/admin/dashboard/stats" -Token $customerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Partner token blocked from admin dashboard" -Method "GET" -Path "/api/admin/dashboard/stats" -Token $partnerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Partner token blocked from customer profile" -Method "GET" -Path "/api/customer/profile" -Token $partnerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Admin token blocked from partner mutation" -Method "PUT" -Path "/api/partner/availability" -Token $adminToken -Expected @(401, 403) -Body @{ online = $true }))
$checks.Add((Invoke-SecurityRequest -Name "Customer token blocked from v1 admin partners" -Method "GET" -Path "/api/v1/admin/partners" -Token $customerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Partner token blocked from v1 admin partners" -Method "GET" -Path "/api/v1/admin/partners" -Token $partnerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Partner token blocked from v1 customer addresses" -Method "GET" -Path "/api/v1/customer/location/addresses" -Token $partnerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Customer token blocked from v1 partner dashboard" -Method "GET" -Path "/api/v1/partner/dashboard/stats" -Token $customerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Admin token blocked from v1 partner dashboard" -Method "GET" -Path "/api/v1/partner/dashboard/stats" -Token $adminToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Missing token blocked from admin" -Method "GET" -Path "/api/admin/dashboard/stats" -Expected @(401)))
$checks.Add((Invoke-SecurityRequest -Name "Missing token blocked from v1 customer" -Method "GET" -Path "/api/v1/customer/location/addresses" -Expected @(401)))
$checks.Add((Invoke-SecurityRequest -Name "Expired JWT blocked" -Method "GET" -Path "/api/customer/profile" -Token $expiredCustomerToken -Expected @(401)))
$checks.Add((Invoke-SecurityRequest -Name "Tampered JWT blocked" -Method "GET" -Path "/api/customer/profile" -Token $tamperedToken -Expected @(401)))
$checks.Add((Invoke-SecurityRequest -Name "Wrong signature JWT blocked" -Method "GET" -Path "/api/customer/profile" -Token $wrongSecretToken -Expected @(401)))

$xss = [uri]::EscapeDataString("<script>alert(1)</script>")
$sqli = [uri]::EscapeDataString("' OR '1'='1' --")
$pathTraversal = [uri]::EscapeDataString("../../../../windows/win.ini")
$checks.Add((Invoke-SecurityRequest -Name "XSS probe does not crash search" -Method "GET" -Path "/api/search/services?keyword=$xss&page=0&size=20" -Expected @(200)))
$checks.Add((Invoke-SecurityRequest -Name "SQLi probe does not crash search" -Method "GET" -Path "/api/search/services?keyword=$sqli&page=0&size=20" -Expected @(200)))
$checks.Add((Invoke-SecurityRequest -Name "Path traversal upload path blocked" -Method "GET" -Path "/uploads/$pathTraversal" -Expected @(400, 401, 403, 404)))
$checks.Add((Invoke-SecurityRequest -Name "Customer IDOR probe on admin customer detail blocked" -Method "GET" -Path "/api/admin/customers/1" -Token $customerToken -Expected @(401, 403)))
$checks.Add((Invoke-SecurityRequest -Name "Partner mass-assignment probe constrained" -Method "PUT" -Path "/api/partner/availability" -Token $partnerToken -Expected @(200, 400) -Body @{ online = $true; role = "ADMIN"; userId = 1; balance = 999999 }))
$checks.Add((Invoke-SecurityRequest -Name "Admin mass-assignment probe constrained" -Method "PUT" -Path "/api/admin/settings/dispatch-radius" -Token $adminToken -Expected @(200, 400) -Body @{ radiusKm = 2.0; role = "SUPER_ADMIN"; active = $true }))

if ($IncludeOtpLive) {
    foreach ($result in Invoke-OtpLiveCheck -Name "Customer" -Mobile "9876500011" -Role "CUSTOMER" -Otp $CustomerOtp) {
        $checks.Add($result)
    }
    foreach ($result in Invoke-OtpLiveCheck -Name "Partner" -Mobile "9876500012" -Role "PARTNER" -Otp $PartnerOtp) {
        $checks.Add($result)
    }
}

$secretFindings = @(Invoke-SecretScan)
$secretResult = [pscustomobject]@{
    Name = "Tracked file secret scan"
    Method = "LOCAL"
    Path = "git ls-files"
    StatusCode = if ($secretFindings.Count -eq 0) { 0 } else { 1 }
    Expected = "0"
    DurationMs = 0
    Passed = ($secretFindings.Count -eq 0)
    Error = if ($secretFindings.Count -eq 0) { $null } else { "$($secretFindings.Count) possible tracked secret findings" }
    ReflectedProbe = $false
}
$checks.Add($secretResult)

$jsonReport = Join-Path $outFull "security-production-regression-$timestamp.json"
$txtReport = Join-Path $outFull "security-production-regression-$timestamp.txt"
$secretReport = Join-Path $outFull "security-secret-findings-$timestamp.json"

$checks | ConvertTo-Json -Depth 10 | Set-Content -Path $jsonReport -Encoding UTF8
$secretFindings | ConvertTo-Json -Depth 5 | Set-Content -Path $secretReport -Encoding UTF8
$checks | Format-Table Name, Method, StatusCode, Expected, Passed, ReflectedProbe -AutoSize | Out-String | Set-Content -Path $txtReport -Encoding UTF8

Write-Host "===== Security Production Regression Summary ====="
$checks | Format-Table Name, Method, StatusCode, Expected, Passed, ReflectedProbe -AutoSize
Write-Host "JSON report: $jsonReport"
Write-Host "Text report: $txtReport"
Write-Host "Secret findings report: $secretReport"

$failed = @($checks | Where-Object { -not $_.Passed -or $_.ReflectedProbe })
if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Host "Failed checks:"
    $failed | Format-Table Name, Method, Path, StatusCode, Expected, Error, ReflectedProbe -AutoSize
    exit 1
}
