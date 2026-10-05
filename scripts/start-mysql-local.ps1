Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Remove-Item Env:LEXHO_DB_URL, Env:LEXHO_DB_DRIVER_CLASS_NAME, Env:LEXHO_DB_USERNAME, Env:LEXHO_DB_PASSWORD, `
    Env:LEXHO_JPA_DIALECT, Env:LEXHO_JPA_HBM2DDL_AUTO, Env:LEXHO_FLYWAY_ENABLED -ErrorAction SilentlyContinue

$env:DB_URL = "jdbc:mysql://localhost:3306/user_servicedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = ""
$env:JPA_DIALECT = "org.hibernate.dialect.MySQLDialect"
$env:JPA_HBM2DDL_AUTO = "validate"
$env:FLYWAY_ENABLED = "true"

& "$repoRoot\mvnw.cmd" -B -ntp "-Dstyle.color=never" -pl application-module spring-boot:run
