param(
    [Parameter(Mandatory = $true)]
    [string]$BaseUrl,

    [Parameter(Mandatory = $true)]
    [string]$AdminToken,

    [Parameter(Mandatory = $true)]
    [string]$PartnerToken,

    [Parameter(Mandatory = $true)]
    [long]$PartnerId
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $params = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }
    Invoke-RestMethod @params
}

$base = $BaseUrl.TrimEnd("/")
$warningReason = "Staging warning smoke " + (Get-Date -Format "yyyyMMddHHmmss")

Write-Host "Checking liveness..."
Invoke-Json -Method GET -Uri "$base/health/liveness" | Out-Null

Write-Host "Creating warning for partner $PartnerId..."
$createResponse = Invoke-Json `
    -Method POST `
    -Uri "$base/api/v1/admin/partners/$PartnerId/warnings" `
    -Headers @{ Authorization = "Bearer $AdminToken" } `
    -Body @{ reason = $warningReason; sendNotification = $true }

$warning = $createResponse.data
if ($null -eq $warning) {
    throw "Warning create response did not contain data"
}

$warningId = if ($warning.warningId) { $warning.warningId } else { $warning.id }
if (-not $warningId) {
    throw "Warning create response did not contain id or warningId"
}
if ($warning.redirectType -ne "WARNING") {
    throw "Warning response redirectType was '$($warning.redirectType)', expected WARNING"
}

Write-Host "Fetching partner warnings..."
$warningsResponse = Invoke-Json `
    -Method GET `
    -Uri "$base/api/v1/partner/warnings" `
    -Headers @{ Authorization = "Bearer $PartnerToken" }

$warnings = if ($warningsResponse.data) { $warningsResponse.data } else { $warningsResponse }
$matchedWarning = $warnings | Where-Object {
    ($_.warningId -eq $warningId) -or ($_.id -eq $warningId)
} | Select-Object -First 1

if ($null -eq $matchedWarning) {
    throw "Created warning $warningId was not returned by /api/v1/partner/warnings"
}
if ($matchedWarning.redirectType -ne "WARNING") {
    throw "Partner warning redirectType was '$($matchedWarning.redirectType)', expected WARNING"
}

Write-Host "Fetching partner notifications..."
$notificationsResponse = Invoke-Json `
    -Method GET `
    -Uri "$base/api/v1/partner/notifications?page=0&size=20" `
    -Headers @{ Authorization = "Bearer $PartnerToken" }

$notifications = if ($notificationsResponse.data.content) {
    $notificationsResponse.data.content
} elseif ($notificationsResponse.data) {
    $notificationsResponse.data
} else {
    $notificationsResponse
}

$matchedNotification = $notifications | Where-Object {
    ($_.warningId -eq $warningId) -or ($_.redirectType -eq "WARNING" -and "$($_.redirectValue)" -eq "$warningId")
} | Select-Object -First 1

if ($null -eq $matchedNotification) {
    throw "Warning notification for warning $warningId was not found"
}

Write-Host "Warning smoke passed for warningId=$warningId"
