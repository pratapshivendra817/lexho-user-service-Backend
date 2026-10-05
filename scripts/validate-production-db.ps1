param(
    [switch]$SkipLocal,
    [switch]$SkipMysql,
    [switch]$SkipBackupRestore,
    [switch]$SkipOutageRecovery,
    [switch]$SkipLoad,
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUsername = $(if ($env:IT_DB_USERNAME) { $env:IT_DB_USERNAME } else { "root" }),
    [string]$MysqlPassword = $(if ($env:IT_DB_PASSWORD) { $env:IT_DB_PASSWORD } else { "" }),
    [string]$MysqlExe = "mysql",
    [string]$MysqlDumpExe = "mysqldump",
    [string]$MysqlAdminExe = "mysqladmin",
    [ValidateSet("manual", "service")]
    [string]$OutageMode = "manual",
    [string]$MysqlServiceName = ""
)

$ErrorActionPreference = "Stop"
$results = [System.Collections.Generic.List[object]]::new()

function Add-Result {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Detail
    )
    $results.Add([pscustomobject]@{
        Step = $Name
        Status = $Status
        Detail = $Detail
    })
}

function Invoke-CheckedCommand {
    param(
        [string]$Name,
        [string]$Executable,
        [string[]]$Arguments
    )

    $display = ($Executable + " " + ($Arguments -join " ")).Trim()
    Write-Host "RUN $display"
    & $Executable @Arguments
    if ($LASTEXITCODE -eq 0) {
        Add-Result -Name $Name -Status "PASS" -Detail $display
    } else {
        Add-Result -Name $Name -Status "FAIL" -Detail $display
        throw "Validation failed: $Name"
    }
}

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

    return $null
}

try {
    if (-not $SkipLocal) {
        Invoke-CheckedCommand -Name "local-application-db-tests" -Executable ".\mvnw.cmd" -Arguments @("-pl", "application-module", "-Dtest=InfrastructureSupportTest,DispatchOrchestrationServiceTest,PartnerSettlementServiceTest,ApplicationModuleFlywayMigrationTest", "test")
        Invoke-CheckedCommand -Name "local-payment-db-tests" -Executable ".\mvnw.cmd" -Arguments @("-pl", "payment-service", "-Dtest=PaymentPersistenceIntegrityTest", "test")
    } else {
        Add-Result -Name "local-tests" -Status "SKIPPED" -Detail "SkipLocal specified"
    }

    $resolvedMysqlExe = Resolve-ExecutablePath -CommandName $MysqlExe
    $resolvedMysqlAdminExe = Resolve-ExecutablePath -CommandName $MysqlAdminExe
    $resolvedMysqlDumpExe = Resolve-ExecutablePath -CommandName $MysqlDumpExe
    $mysqlCliReady = $null -ne $resolvedMysqlExe
    $mysqlAdminReady = $null -ne $resolvedMysqlAdminExe
    $backupCliReady = $mysqlCliReady -and ($null -ne $resolvedMysqlDumpExe)

    if (-not $SkipMysql) {
        if ($mysqlCliReady) {
            Invoke-CheckedCommand -Name "mysql-validation-suite" -Executable ".\scripts\run-mysql-validation-suite.ps1" -Arguments @(
                "-MysqlHost", $MysqlHost,
                "-MysqlPort", "$MysqlPort",
                "-MysqlUsername", $MysqlUsername,
                "-MysqlPassword", $MysqlPassword,
                "-MysqlExe", $resolvedMysqlExe
            )
        } else {
            Add-Result -Name "mysql-validation-suite" -Status "SKIPPED" -Detail "mysql not available in PATH"
        }
    } else {
        Add-Result -Name "mysql-validation-suite" -Status "SKIPPED" -Detail "SkipMysql specified"
    }

    if (-not $SkipBackupRestore) {
        if ($backupCliReady) {
            Invoke-CheckedCommand -Name "backup-restore-validation" -Executable ".\scripts\db-backup-restore-validate.ps1" -Arguments @(
                "-MysqlHost", $MysqlHost,
                "-MysqlPort", "$MysqlPort",
                "-MysqlUsername", $MysqlUsername,
                "-MysqlPassword", $MysqlPassword,
                "-MysqlExe", $resolvedMysqlExe,
                "-MysqlDumpExe", $resolvedMysqlDumpExe
            )
        } else {
            Add-Result -Name "backup-restore-validation" -Status "SKIPPED" -Detail "mysql/mysqldump not available in PATH"
        }
    } else {
        Add-Result -Name "backup-restore-validation" -Status "SKIPPED" -Detail "SkipBackupRestore specified"
    }

    if (-not $SkipOutageRecovery) {
        if ($mysqlCliReady -and $mysqlAdminReady) {
            $outageArguments = @(
                "-MysqlHost", $MysqlHost,
                "-MysqlPort", "$MysqlPort",
                "-MysqlUsername", $MysqlUsername,
                "-MysqlPassword", $MysqlPassword,
                "-MysqlExe", $resolvedMysqlExe,
                "-MysqlAdminExe", $resolvedMysqlAdminExe,
                "-OutageMode", $OutageMode
            )
            if ($MysqlServiceName) {
                $outageArguments += @("-MysqlServiceName", $MysqlServiceName)
            }
            Invoke-CheckedCommand -Name "outage-recovery-validation" -Executable ".\scripts\db-outage-recovery-validate.ps1" -Arguments $outageArguments
        } else {
            Add-Result -Name "outage-recovery-validation" -Status "SKIPPED" -Detail "mysql/mysqladmin not available in PATH"
        }
    } else {
        Add-Result -Name "outage-recovery-validation" -Status "SKIPPED" -Detail "SkipOutageRecovery specified"
    }

    if (-not $SkipLoad) {
        $k6 = Get-Command k6 -ErrorAction SilentlyContinue
        if ($null -eq $k6) {
            Add-Result -Name "load-validation" -Status "SKIPPED" -Detail "k6 is not installed"
        } else {
            Add-Result -Name "load-validation" -Status "SKIPPED" -Detail "Set BASE_URL and auth tokens, then run k6 scripts\db-load-validation.js"
        }
    } else {
        Add-Result -Name "load-validation" -Status "SKIPPED" -Detail "SkipLoad specified"
    }
}
finally {
    $results | Format-Table -AutoSize
    $failed = $results | Where-Object { $_.Status -eq "FAIL" }
    if ($failed.Count -gt 0) {
        Write-Host "FAIL production-db-validation"
        exit 1
    }
    $passed = $results | Where-Object { $_.Status -eq "PASS" }
    if ($passed.Count -gt 0) {
        Write-Host "PASS production-db-validation (with explicit skipped items shown above if any)"
        exit 0
    }
    Write-Host "SKIPPED production-db-validation"
    exit 0
}
