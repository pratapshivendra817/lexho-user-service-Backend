param(
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUsername = $(if ($env:IT_DB_USERNAME) { $env:IT_DB_USERNAME } else { "root" }),
    [string]$MysqlPassword = $(if ($env:IT_DB_PASSWORD) { $env:IT_DB_PASSWORD } else { "" }),
    [string]$MysqlExe = "mysql"
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
    $displayArgs = $args | ForEach-Object {
        if ($_ -like "-p*") {
            "-p****"
        } else {
            $_
        }
    }
    Write-Host ("RUN $MysqlExe " + ($displayArgs -join " "))
    & $MysqlExe @args | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL command failed."
    }
}

function Invoke-Maven {
    param([string[]]$MavenArgs)

    Write-Host ("RUN .\mvnw.cmd " + ($MavenArgs -join " "))
    $process = Start-Process -FilePath ".\mvnw.cmd" -ArgumentList $MavenArgs -Wait -NoNewWindow -PassThru
    if ($process.ExitCode -ne 0) {
        throw "Command failed: .\mvnw.cmd $($MavenArgs -join ' ')"
    }
}

function New-JdbcUrl {
    param([string]$Database)

    return "jdbc:mysql://$MysqlHost`:$MysqlPort/${Database}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
}

$MysqlExe = Resolve-ExecutablePath -CommandName $MysqlExe

$databases = @(
    "lexho_application_flyway_test",
    "lexho_application_explain_test",
    "lexho_application_upgrade_test",
    "lexho_application_production_readiness_test",
    "lexho_order_test",
    "lexho_user_flyway_test",
    "lexho_backup_restore_source",
    "lexho_backup_restore_restored",
    "lexho_outage_recovery_test"
)

foreach ($db in $databases) {
    Invoke-MysqlStatement -Sql "DROP DATABASE IF EXISTS $db; CREATE DATABASE $db;"
}

Invoke-Maven -MavenArgs @(
    "-pl", "user-service",
    "-Dlexho.it.mysql.flyway=true",
    "-DIT_DB_URL=""$(New-JdbcUrl -Database 'lexho_user_flyway_test')""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=CallingFlywayMigrationTest",
    "test"
)

Invoke-Maven -MavenArgs @(
    "-pl", "order-service",
    "-Dlexho.it.mysql.order=true",
    "-DIT_DB_URL=""$(New-JdbcUrl -Database 'lexho_order_test')""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=OrderRepositoryTest",
    "test"
)

Invoke-Maven -MavenArgs @(
    "-pl", "application-module",
    "-Dlexho.it.mysql.application.flyway=true",
    "-DIT_DB_URL=""$(New-JdbcUrl -Database 'lexho_application_flyway_test')""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=ApplicationMysqlFlywayMigrationIT",
    "test"
)

Invoke-Maven -MavenArgs @(
    "-pl", "application-module",
    "-Dlexho.it.mysql.explain=true",
    "-DIT_DB_URL=""$(New-JdbcUrl -Database 'lexho_application_explain_test')""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=ApplicationMysqlExplainPlanIT",
    "test"
)

Invoke-Maven -MavenArgs @(
    "-pl", "application-module",
    "-Dlexho.it.mysql.application.upgrade=true",
    "-DIT_DB_URL=""$(New-JdbcUrl -Database 'lexho_application_upgrade_test')""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=ApplicationMysqlUpgradeMigrationIT",
    "test"
)

Invoke-Maven -MavenArgs @(
    "-pl", "application-module",
    "-Dlexho.it.mysql.production.readiness=true",
    "-DIT_DB_URL=""$(New-JdbcUrl -Database 'lexho_application_production_readiness_test')""",
    "-DIT_DB_USERNAME=$MysqlUsername",
    "-DIT_DB_PASSWORD=$MysqlPassword",
    "-Dtest=ApplicationMysqlProductionReadinessIT",
    "test"
)

Write-Host "PASS mysql-validation-suite"
