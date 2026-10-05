param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$Environment = "application-module/postman/newman.tokens.env.json",
    [string[]]$Collections = @(
        "application-module/postman/Lexho-Admin.postman_collection.json",
        "application-module/postman/Lexho-Location-Dispatch-Transition.postman_collection.json",
        "docs/lexho-partner-postman-collection.json"
    ),
    [string]$OutDir = "target/postman-regression",
    [string]$Newman = "newman",
    [string]$AdminToken = $env:POSTMAN_ADMIN_TOKEN,
    [string]$AccessToken = $env:POSTMAN_ACCESS_TOKEN,
    [string]$CustomerToken = $env:POSTMAN_CUSTOMER_TOKEN,
    [string]$PartnerToken = $env:POSTMAN_PARTNER_TOKEN,
    [int]$TimeoutRequestMs = 30000,
    [switch]$ReadOnly,
    [switch]$SkipAuthWhenToken,
    [switch]$AllowKnownStatefulStatuses,
    [switch]$SkipMissingCollections,
    [switch]$NoInstallHint
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path (Get-Location) $Path)
}

function Get-NewmanCommand {
    param([string]$Preferred)

    $cmd = Get-Command $Preferred -ErrorAction SilentlyContinue
    if ($cmd) {
        return @($cmd.Source)
    }

    $npx = Get-Command "npx.cmd" -ErrorAction SilentlyContinue
    if (-not $npx) {
        $npx = Get-Command "npx" -ErrorAction SilentlyContinue
    }
    if ($npx) {
        return @($npx.Source, "--yes", "newman")
    }

    if (-not $NoInstallHint) {
        Write-Host "Newman is not installed. Install it with: npm install -g newman"
    }
    throw "Newman executable not found"
}

function Convert-CollectionForRegression {
    param(
        [string]$CollectionPath,
        [string]$OutputPath,
        [bool]$ReadOnlyMode,
        [bool]$SkipAuthRequests,
        [bool]$HasAuthToken
    )

    $collection = Get-Content $CollectionPath -Raw | ConvertFrom-Json
    $stats = [ordered]@{
        requests = 0
        skippedRequests = 0
        generatedStatusAssertions = 0
        existingTestEvents = 0
    }

    $defaultTest = @(
        "pm.test('endpoint returns a successful HTTP status', function () {",
        "    const allowKnownStatefulStatuses = String(pm.environment.get('allowKnownStatefulStatuses') || '').toLowerCase() === 'true';",
        "    const expectedByRequest = {",
        "        'Login': [200, 401, 429],",
        "        'Verify OTP': [200, 400],",
        "        'Update Verification': [200, 400]",
        "    };",
        "    const allowed = expectedByRequest[pm.info.requestName] || [];",
        "    if (allowKnownStatefulStatuses && allowed.includes(pm.response.code)) {",
        "        return;",
        "    }",
        "    pm.expect(pm.response.code).to.be.below(400);",
        "});",
        "pm.test('endpoint does not return a server error', function () {",
        "    pm.expect(pm.response.code).to.be.below(500);",
        "});",
        "pm.test('response completes within regression timeout', function () {",
        "    pm.expect(pm.response.responseTime).to.be.below(Number(pm.environment.get('responseTimeBudgetMs') || 5000));",
        "});"
    )

    function Update-Items {
        param(
            [object[]]$Items,
            [string]$ParentName = ""
        )

        $filtered = New-Object System.Collections.Generic.List[object]
        foreach ($item in $Items) {
            $itemPath = if ([string]::IsNullOrWhiteSpace($ParentName)) { $item.name } else { "$ParentName / $($item.name)" }
            if ($item.request) {
                $method = [string]$item.request.method
                if (($ReadOnlyMode -and $method.ToUpperInvariant() -ne "GET") -or
                    ($SkipAuthRequests -and $HasAuthToken -and $itemPath.StartsWith("Auth /"))) {
                    $stats.skippedRequests++
                    continue
                }

                $stats.requests++
                $testEvents = @($item.event | Where-Object { $_.listen -eq "test" })
                $stats.existingTestEvents += $testEvents.Count

                $generatedEvent = [pscustomobject]@{
                    listen = "test"
                    script = [pscustomobject]@{
                        type = "text/javascript"
                        exec = $defaultTest
                    }
                }

                if ($item.PSObject.Properties.Name -contains "event" -and $item.event) {
                    $item.event = @($item.event) + $generatedEvent
                } else {
                    Add-Member -InputObject $item -MemberType NoteProperty -Name "event" -Value @($generatedEvent)
                }
                $stats.generatedStatusAssertions++
            }

            if ($item.item) {
                $children = @(Update-Items -Items @($item.item) -ParentName $itemPath)
                if ($children.Count -eq 0) {
                    continue
                }
                $item.item = $children
            }

            $filtered.Add($item)
        }

        return $filtered.ToArray()
    }

    $collection.item = @(Update-Items -Items @($collection.item))
    $collection | ConvertTo-Json -Depth 100 | Set-Content -Path $OutputPath -Encoding UTF8
    return [pscustomobject]$stats
}

$outFull = Resolve-RepoPath $OutDir
New-Item -ItemType Directory -Force -Path $outFull | Out-Null

$newmanCmd = Get-NewmanCommand -Preferred $Newman
$runResults = New-Object System.Collections.Generic.List[object]

foreach ($collection in $Collections) {
    $collectionFull = Resolve-RepoPath $collection
    if (-not (Test-Path $collectionFull)) {
        if ($SkipMissingCollections) {
            Write-Warning "Skipping missing collection: $collection"
            continue
        }
        throw "Collection not found: $collection"
    }

    $name = [System.IO.Path]::GetFileNameWithoutExtension($collectionFull)
    $normalizedCollection = Join-Path $outFull "$name.regression.postman_collection.json"
    $jsonReport = Join-Path $outFull "$name.newman-report.json"

    $hasAuthToken = -not [string]::IsNullOrWhiteSpace($AdminToken) -or -not [string]::IsNullOrWhiteSpace($AccessToken)
    $stats = Convert-CollectionForRegression `
        -CollectionPath $collectionFull `
        -OutputPath $normalizedCollection `
        -ReadOnlyMode $ReadOnly.IsPresent `
        -SkipAuthRequests $SkipAuthWhenToken.IsPresent `
        -HasAuthToken $hasAuthToken
    Write-Host ""
    Write-Host "Running Postman regression: $collection"
    Write-Host "Requests: $($stats.requests), skipped: $($stats.skippedRequests), existing test events: $($stats.existingTestEvents), generated assertions: $($stats.generatedStatusAssertions)"

    $args = @(
        "run", $normalizedCollection,
        "--env-var", "baseUrl=$BaseUrl",
        "--env-var", "responseTimeBudgetMs=5000",
        "--env-var", "allowKnownStatefulStatuses=$($AllowKnownStatefulStatuses.IsPresent.ToString().ToLowerInvariant())",
        "--timeout-request", "$TimeoutRequestMs",
        "--reporters", "cli,json",
        "--reporter-json-export", $jsonReport
    )
    if ($AllowKnownStatefulStatuses) {
        $args += @("--env-var", "strictMode=false")
    }

    $tokenOverrides = [ordered]@{
        adminToken = $AdminToken
        accessToken = $AccessToken
        customerToken = $CustomerToken
        partnerToken = $PartnerToken
    }
    foreach ($entry in $tokenOverrides.GetEnumerator()) {
        if (-not [string]::IsNullOrWhiteSpace($entry.Value)) {
            $args += @("--env-var", "$($entry.Key)=$($entry.Value)")
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($Environment)) {
        $environmentFull = Resolve-RepoPath $Environment
        if (Test-Path $environmentFull) {
            $args = @("run", $normalizedCollection, "-e", $environmentFull) + $args[2..($args.Count - 1)]
        } else {
            Write-Warning "Environment file not found, continuing without it: $Environment"
        }
    }

    $executable = $newmanCmd[0]
    $newmanPrefixArgs = @()
    if ($newmanCmd.Count -gt 1) {
        $newmanPrefixArgs = $newmanCmd[1..($newmanCmd.Count - 1)]
    }

    $previousErrorActionPreference = $ErrorActionPreference
    $previousNodeOptions = $env:NODE_OPTIONS
    if ([string]::IsNullOrWhiteSpace($env:NODE_OPTIONS)) {
        $env:NODE_OPTIONS = "--no-deprecation"
    } elseif ($env:NODE_OPTIONS -notmatch "(^|\s)--no-deprecation(\s|$)") {
        $env:NODE_OPTIONS = "$env:NODE_OPTIONS --no-deprecation"
    }

    $ErrorActionPreference = "Continue"
    $output = & $executable @($newmanPrefixArgs + $args) 2>&1
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    $env:NODE_OPTIONS = $previousNodeOptions
    $output | Tee-Object -FilePath (Join-Path $outFull "$name.newman-output.txt")

    $runResults.Add([pscustomobject]@{
            Collection = $collection
            Requests = $stats.requests
            Skipped = $stats.skippedRequests
            ExistingTestEvents = $stats.existingTestEvents
            GeneratedAssertions = $stats.generatedStatusAssertions
            Report = $jsonReport
            ExitCode = $exitCode
            Passed = ($exitCode -eq 0)
        })
}

Write-Host ""
Write-Host "===== Postman Regression Summary ====="
$runResults | Format-Table Collection, Requests, Skipped, ExistingTestEvents, GeneratedAssertions, Passed, ExitCode -AutoSize

$failed = @($runResults | Where-Object { -not $_.Passed })
if ($failed.Count -gt 0) {
    exit 1
}
