Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

param(
    [string]$DbUrl = "jdbc:mysql://localhost:3306/user_servicedb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
    [string]$DbUsername = "root",
    [string]$DbPassword = "",
    [string]$RedisHost = "localhost",
    [string]$RedisPort = "6380",
    [switch]$EnableRabbit
)

$env:DB_URL = $DbUrl
$env:DB_USERNAME = $DbUsername
$env:DB_PASSWORD = $DbPassword
$env:REDIS_HOST = $RedisHost
$env:REDIS_PORT = $RedisPort
$env:IT_REDIS_PORT = $RedisPort

$env:APP_GEO_EVENTS_RABBIT_ENABLED = $EnableRabbit.IsPresent.ToString().ToLower()
$env:APP_GEO_EVENTS_RABBIT_CONSUMER_ENABLED = $EnableRabbit.IsPresent.ToString().ToLower()
$env:APP_GEO_EVENTS_RABBIT_DLQ_CONSUMER_ENABLED = $EnableRabbit.IsPresent.ToString().ToLower()
$env:APP_GEO_EVENTS_RABBIT_EXCHANGE = "geo.events.exchange"
$env:APP_GEO_EVENTS_RABBIT_QUEUE = "geo.events.queue"
$env:APP_GEO_EVENTS_RABBIT_DLQ = "geo.events.dlq"
$env:APP_GEO_EVENTS_RABBIT_ROUTING_KEY = "geo.events"

Write-Host "Non-docker env configured for current shell."
Write-Host "Rabbit enabled: $($env:APP_GEO_EVENTS_RABBIT_ENABLED)"
