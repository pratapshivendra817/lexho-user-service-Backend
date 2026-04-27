param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$Email = "admin@lexho.com",
    [string]$Password = "Admin@12345",
    [long]$ProviderId = 1
)

$ErrorActionPreference = "Stop"

Write-Host "1) Admin login (OTP trigger)"
$loginBody = @{
    email = $Email
    password = $Password
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/login" -ContentType "application/json" -Body $loginBody
$loginResponse | ConvertTo-Json -Depth 5 | Write-Host

$otp = Read-Host "Enter OTP for $Email"
if ([string]::IsNullOrWhiteSpace($otp)) {
    throw "OTP is required"
}

Write-Host "2) Verify OTP and get access token"
$verifyBody = @{
    email = $Email
    otp = $otp
} | ConvertTo-Json

$verifyResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/verify-otp" -ContentType "application/json" -Body $verifyBody
$verifyResponse | ConvertTo-Json -Depth 5 | Write-Host

$token = $verifyResponse.accessToken
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Access token not found in verify-otp response"
}

$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "3) Dashboard stats"
$statsResponse = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/dashboard/stats" -Headers $headers
$statsResponse | ConvertTo-Json -Depth 10 | Write-Host

Write-Host "4) PATCH provider verification (CORS-sensitive endpoint)"
$patchBody = @{
    status = "VERIFIED"
} | ConvertTo-Json

$providerPatchResponse = Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/admin/providers/$ProviderId/verification" -Headers $headers -ContentType "application/json" -Body $patchBody
$providerPatchResponse | ConvertTo-Json -Depth 10 | Write-Host

Write-Host "Admin smoke flow completed successfully."
