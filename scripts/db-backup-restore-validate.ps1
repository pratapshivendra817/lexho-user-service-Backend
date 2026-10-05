param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUsername = $(if ($env:IT_DB_USERNAME) { $env:IT_DB_USERNAME } else { "root" }),
    [string]$MysqlPassword = $(if ($env:IT_DB_PASSWORD) { $env:IT_DB_PASSWORD } else { "" }),
    [string]$MysqlExe = "mysql",
    [string]$MysqlDumpExe = "mysqldump",
    [string]$SourceDatabase = "lexho_application_production_readiness_test",
    [string]$RestoreDatabase = "lexho_backup_restore_restored"
)

$ErrorActionPreference = "Stop"
$backupFile = Join-Path $env:TEMP "lexho-backup-restore.sql"
$start = Get-Date

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
    param(
        [string]$Sql,
        [string]$Database
    )

    $args = Get-MysqlArgs -Database $Database
    $args += @("-e", $Sql)
    Write-Host ("RUN $MysqlExe " + ($args -join " "))
    & $MysqlExe @args | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed."
    }
}

function Get-ScalarValue {
    param(
        [string]$Database,
        [string]$Sql
    )

    $args = Get-MysqlArgs -Database $Database
    $args += @("-N", "-e", $Sql)
    & $MysqlExe @args
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL scalar query failed."
    }
}

function New-JdbcUrl {
    param([string]$Database)

    return "jdbc:mysql://$MysqlHost`:$MysqlPort/${Database}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
}

function Invoke-Maven {
    param([string[]]$MavenArgs)

    Write-Host ("RUN .\mvnw.cmd " + ($MavenArgs -join " "))
    $process = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $MavenArgs -Wait -NoNewWindow -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Command failed: .\mvnw.cmd $($MavenArgs -join ' ')"
    }
}

$MysqlExe = Resolve-ExecutablePath -CommandName $MysqlExe
$MysqlDumpExe = Resolve-ExecutablePath -CommandName $MysqlDumpExe

if ($SourceDatabase -ne "lexho_application_production_readiness_test") {
    throw "SourceDatabase must remain lexho_application_production_readiness_test because the readiness integration test enforces that isolated database name."
}

Invoke-MysqlStatement -Database "" -Sql "DROP DATABASE IF EXISTS $SourceDatabase; CREATE DATABASE $SourceDatabase;"
Invoke-MysqlStatement -Database "" -Sql "DROP DATABASE IF EXISTS $RestoreDatabase; CREATE DATABASE $RestoreDatabase;"

$jdbcUrl = New-JdbcUrl -Database $SourceDatabase
Invoke-Maven -MavenArgs @(
    "-pl", "application-module",
    "-Dlexho.it.mysql.production.readiness=true",
    "-DIT_DB_URL=""$jdbcUrl""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=ApplicationMysqlProductionReadinessIT",
    "test"
)

Invoke-MysqlStatement -Database $SourceDatabase -Sql @"
INSERT INTO orders (
    user_id, total_amount, status, payment_status, address, scheduled_at, created_at, updated_at,
    customer_name, customer_phone, latitude, longitude, service_type, amount, payment_mode, booking_type
) VALUES (
    501, 999.00, 'COMPLETED', 'SUCCESS', 'Backup restore address', NOW(), NOW(), NOW(),
    'Backup Customer', '9999999999', 28.61, 77.20, 'Cleaning', 999.00, 'ONLINE', 'INSTANT'
);
"@

$dumpArgs = @("-h", $MysqlHost, "-P", "$MysqlPort", "-u", $MysqlUsername)
if ($MysqlPassword) {
    $dumpArgs += "-p$MysqlPassword"
}
$dumpArgs += @("--no-create-db", $SourceDatabase, "--result-file=$backupFile")
Write-Host ("RUN $MysqlDumpExe " + ($dumpArgs -join " "))
& $MysqlDumpExe @dumpArgs
if ($LASTEXITCODE -ne 0) {
    throw "mysqldump failed."
}

Invoke-MysqlStatement -Database "" -Sql "DROP DATABASE IF EXISTS $RestoreDatabase; CREATE DATABASE $RestoreDatabase;"

$restoreArgs = Get-MysqlArgs -Database $RestoreDatabase
Write-Host ("RUN cmd /c type `"$backupFile`" | $MysqlExe " + ($restoreArgs -join " "))
Get-Content $backupFile | & $MysqlExe @restoreArgs
if ($LASTEXITCODE -ne 0) {
    throw "mysql restore failed."
}

$sourceOrderCount = (Get-ScalarValue -Database $SourceDatabase -Sql "SELECT COUNT(*) FROM orders;").Trim()
$restoredOrderCount = (Get-ScalarValue -Database $RestoreDatabase -Sql "SELECT COUNT(*) FROM orders;").Trim()

$duration = (Get-Date) - $start
Write-Host "backup_file=$backupFile"
Write-Host "source_orders=$sourceOrderCount"
Write-Host "restored_orders=$restoredOrderCount"
Write-Host "duration_seconds=$([int]$duration.TotalSeconds)"

if ($sourceOrderCount -ne $restoredOrderCount) {
    throw "Backup/restore validation failed: row counts differ."
}

Write-Host "PASS backup-restore-validation"
