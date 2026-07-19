Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

param(
    [switch]$WithRabbit
)

if ($WithRabbit) {
    $env:APP_GEO_EVENTS_RABBIT_ENABLED = "true"
    $env:APP_GEO_EVENTS_RABBIT_CONSUMER_ENABLED = "true"
    $env:APP_GEO_EVENTS_RABBIT_DLQ_CONSUMER_ENABLED = "true"
}

# Reliable startup via Maven so classpath is always correct.
# Batch/no-transfer-progress keeps IDE terminals from repainting/flickering.
& "$PSScriptRoot\mvnw.cmd" -B -ntp -Dstyle.color=never -pl application-module spring-boot:run
