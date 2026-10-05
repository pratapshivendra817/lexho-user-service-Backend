Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:DB_URL = "jdbc:mysql://127.0.0.1:3307/lexho_application_production_readiness_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "change-me"
$env:JWT_SECRET = [guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")
$env:INTERNAL_API_TOKEN = "internal-" + [guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N")
$env:APP_ALLOWED_CORS_ORIGINS = "http://localhost:3000"
$env:APP_PUSH_FCM_ENABLED = "false"
$env:APP_NOTIFICATION_EMAIL_ENABLED = "false"
$env:APP_NOTIFICATION_SMS_ENABLED = "false"
$env:SOCKET_IO_ENABLED = "false"

& "$repoRoot\mvnw.cmd" -pl application-module spring-boot:run
