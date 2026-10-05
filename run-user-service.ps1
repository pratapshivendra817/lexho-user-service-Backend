param(
    [switch]$WithRabbit
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($WithRabbit) {
    $env:APP_GEO_EVENTS_RABBIT_ENABLED = "true"
    $env:APP_GEO_EVENTS_RABBIT_CONSUMER_ENABLED = "true"
    $env:APP_GEO_EVENTS_RABBIT_DLQ_CONSUMER_ENABLED = "true"
}

if ([string]::IsNullOrWhiteSpace($env:SPRING_PROFILES_ACTIVE)) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}
if ([string]::IsNullOrWhiteSpace($env:LEXHO_AUTH_OTP_LOG_TO_CONSOLE)) {
    $env:LEXHO_AUTH_OTP_LOG_TO_CONSOLE = "true"
}

# Reliable startup via Maven so classpath is always correct.
# Batch/no-transfer-progress keeps IDE terminals from repainting/flickering.
& "$PSScriptRoot\mvnw.cmd" -B -ntp -pl application-module -am "-Dmaven.test.skip=true" "-Dcheckstyle.skip=true" "-Dspotbugs.skip=true" install
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& "$PSScriptRoot\mvnw.cmd" -B -ntp -pl application-module "-Dmaven.test.skip=true" spring-boot:run
exit $LASTEXITCODE
