param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUsername = $(if ($env:IT_DB_USERNAME) { $env:IT_DB_USERNAME } else { "root" }),
    [string]$MysqlPassword = $(if ($env:IT_DB_PASSWORD) { $env:IT_DB_PASSWORD } else { "" }),
    [string]$MysqlExe = "mysql",
    [string]$MysqlAdminExe = "mysqladmin",
    [string]$DatabaseName = "lexho_outage_recovery_test",
    [ValidateSet("manual", "service")]
    [string]$OutageMode = "manual",
    [string]$MysqlServiceName = ""
)

$ErrorActionPreference = "Stop"

function Resolve-ExecutablePath {
    param([string]$CommandName)

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($null -ne $command) {
        return $command.Source
    }

    $fileName = if ([System.IO.Path]::GetExtension($CommandName)) { $CommandName } else { "$CommandName.exe" }
    $candidates = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\$fileName",
        "C:\Program Files\MySQL\MySQL Workbench 8.0\$fileName"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "Required command not found: $CommandName"
}

function Get-MysqlArgs {
    param([string]$Database)

    $args = @("-h", $MysqlHost, "-P", "$MysqlPort", "-u", $MysqlUsername)
    if ($MysqlPassword) {
        $args += "-p$MysqlPassword"
    }
    if ($Database) {
        $args += $Database
    }
    return $args
}

function Invoke-MysqlStatement {
    param([string]$Sql)

    $args = Get-MysqlArgs
    $args += @("-e", $Sql)
    Write-Host ("RUN $MysqlExe " + ($args -join " "))
    & $MysqlExe @args | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed."
    }
}

function Test-MysqlPing {
    $args = @("-h", $MysqlHost, "-P", "$MysqlPort", "-u", $MysqlUsername)
    if ($MysqlPassword) {
        $args += "-p$MysqlPassword"
    }
    $args += @("ping", "--silent")
    $previousErrorActionPreference = $ErrorActionPreference
    $previousNativeCommandUseErrorActionPreference = $PSNativeCommandUseErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $PSNativeCommandUseErrorActionPreference = $false
        & $MysqlAdminExe @args 1>$null 2>$null
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
        $PSNativeCommandUseErrorActionPreference = $previousNativeCommandUseErrorActionPreference
    }
    return $LASTEXITCODE -eq 0
}

function Wait-ForPingState {
    param(
        [bool]$ExpectedAvailable,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $available = Test-MysqlPing
        if ($available -eq $ExpectedAvailable) {
            return
        }
        Start-Sleep -Seconds 2
    }
    if ($ExpectedAvailable) {
        throw "MySQL did not recover within $TimeoutSeconds seconds."
    }
    throw "MySQL did not become unavailable within $TimeoutSeconds seconds."
}

function Invoke-Maven {
    param([string[]]$MavenArgs)

    Write-Host ("RUN .\mvnw.cmd " + ($MavenArgs -join " "))
    $process = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $MavenArgs -Wait -NoNewWindow -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Command failed: .\mvnw.cmd $($MavenArgs -join ' ')"
    }
}

function Get-MySqlServiceDefinition {
    param([string]$ServiceName)

    if (-not $ServiceName) {
        return $null
    }

    return Get-CimInstance Win32_Service -Filter "Name='$ServiceName'" -ErrorAction SilentlyContinue
}

function Stop-MySqlForOutage {
    param([string]$ServiceName)

    $service = Get-MySqlServiceDefinition -ServiceName $ServiceName

    if ($service) {
        try {
            Write-Host "Stopping Windows service $ServiceName"
            Stop-Service -Name $ServiceName -ErrorAction Stop
            return @{
                RecoveryMode = "service"
                ServiceName = $ServiceName
            }
        } catch {
            Write-Host "Service stop failed, falling back to mysqladmin shutdown: $($_.Exception.Message)"
        }
    }

    $shutdownArgs = @("-h", $MysqlHost, "-P", "$MysqlPort", "-u", $MysqlUsername)
    if ($MysqlPassword) {
        $shutdownArgs += "-p$MysqlPassword"
    }
    $shutdownArgs += "shutdown"

    Write-Host ("RUN $MysqlAdminExe " + ($shutdownArgs -join " "))
    & $MysqlAdminExe @shutdownArgs
    if ($LASTEXITCODE -ne 0) {
        throw "mysqladmin shutdown failed."
    }

    return @{
        RecoveryMode = "service-definition"
        ServiceName = $ServiceName
        Service = $service
    }
}

function Start-MySqlAfterOutage {
    param([hashtable]$RecoveryPlan)

    if ($RecoveryPlan.RecoveryMode -eq "service") {
        Write-Host "Starting Windows service $($RecoveryPlan.ServiceName)"
        Start-Service -Name $RecoveryPlan.ServiceName -ErrorAction Stop
        return
    }

    $service = $RecoveryPlan.Service
    if ($null -eq $service) {
        throw "No MySQL service definition available for recovery."
    }

    try {
        Write-Host "Starting Windows service $($service.Name)"
        Start-Service -Name $service.Name -ErrorAction Stop
        return
    } catch {
        Write-Host "Service start failed, falling back to direct mysqld launch: $($_.Exception.Message)"
    }

    $pathName = $service.PathName.Trim()
    $exePath = ""
    $arguments = ""

    if ($pathName.StartsWith('"')) {
        $closingQuote = $pathName.IndexOf('"', 1)
        if ($closingQuote -lt 0) {
            throw "Unable to parse MySQL service executable path."
        }
        $exePath = $pathName.Substring(1, $closingQuote - 1)
        $arguments = $pathName.Substring($closingQuote + 1).Trim()
    } else {
        $firstSpace = $pathName.IndexOf(' ')
        if ($firstSpace -lt 0) {
            $exePath = $pathName
        } else {
            $exePath = $pathName.Substring(0, $firstSpace)
            $arguments = $pathName.Substring($firstSpace + 1).Trim()
        }
    }

    if (-not (Test-Path $exePath)) {
        throw "MySQL service executable not found at $exePath"
    }

    Write-Host ("RUN " + $exePath + " " + $arguments)
    Start-Process -FilePath $exePath -ArgumentList $arguments -WindowStyle Hidden | Out-Null
}

$MysqlExe = Resolve-ExecutablePath -CommandName $MysqlExe
$MysqlAdminExe = Resolve-ExecutablePath -CommandName $MysqlAdminExe

if (-not (Test-MysqlPing)) {
    throw "MySQL is not reachable before outage simulation."
}

Invoke-MysqlStatement -Sql "DROP DATABASE IF EXISTS $DatabaseName; CREATE DATABASE $DatabaseName;"

$jdbcUrl = "jdbc:mysql://$MysqlHost`:$MysqlPort/${DatabaseName}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
Write-Host "Starting isolated production-readiness app startup validation"
Invoke-Maven -MavenArgs @(
    "-pl", "application-module",
    "-Dlexho.it.mysql.production.readiness=true",
    "-DIT_DB_URL=""$jdbcUrl""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=ApplicationMysqlProductionReadinessIT",
    "test"
)

if ($OutageMode -eq "service") {
    if (-not $MysqlServiceName) {
        throw "MysqlServiceName is required when OutageMode=service."
    }
    $recoveryPlan = Stop-MySqlForOutage -ServiceName $MysqlServiceName
} else {
    Write-Host "Manual outage required: stop MySQL now for $MysqlHost`:$MysqlPort, then press Enter."
    Read-Host | Out-Null
}

Wait-ForPingState -ExpectedAvailable $false
Write-Host "Observed expected DB outage"

if ($OutageMode -eq "service") {
    Start-MySqlAfterOutage -RecoveryPlan $recoveryPlan
} else {
    Write-Host "Manual recovery required: start MySQL now for $MysqlHost`:$MysqlPort, then press Enter."
    Read-Host | Out-Null
}

Wait-ForPingState -ExpectedAvailable $true
Write-Host "PASS outage-recovery-validation"
