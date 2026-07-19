Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port
    )
    try {
        $r = Test-NetConnection -ComputerName $HostName -Port $Port -WarningAction SilentlyContinue
        return [bool]$r.TcpTestSucceeded
    } catch {
        return $false
    }
}

$checks = @(
    @{ Name = "MySQL"; Host = "localhost"; Port = 3306 },
    @{ Name = "Redis"; Host = "localhost"; Port = 6379 },
    @{ Name = "RabbitMQ AMQP"; Host = "localhost"; Port = 5672 },
    @{ Name = "RabbitMQ Mgmt"; Host = "localhost"; Port = 15672 }
)

foreach ($c in $checks) {
    $ok = Test-TcpPort -HostName $c.Host -Port $c.Port
    if ($ok) {
        Write-Host "[OK] $($c.Name) reachable at $($c.Host):$($c.Port)"
    } else {
        Write-Host "[WARN] $($c.Name) not reachable at $($c.Host):$($c.Port)"
    }
}
