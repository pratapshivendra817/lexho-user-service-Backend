param(
    [int]$Port = 6380,
    [string]$ExtractDir = "target/memurai-extract",
    [string]$DataDir = "target/memurai-data",
    [string]$LogFile = "target/memurai-6380.log"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-RepoPath {
    param([string]$Path)
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path (Get-Location) $Path)
}

function Test-TcpPort {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue |
            Where-Object { $_.State -eq "Listen" } |
            Select-Object -First 1
    return $null -ne $connection
}

function Get-RedisCli {
    $candidates = @(
        "C:\Program Files\Redis\redis-cli.exe",
        "C:\Program Files\Memurai\redis-cli.exe"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }
    return $null
}

function Test-GeoAddSupport {
    param([int]$Port)
    $redisCli = Get-RedisCli
    if ($null -eq $redisCli) {
        return $false
    }
    $commandInfo = & $redisCli -h localhost -p $Port COMMAND INFO GEOADD 2>$null
    return ($LASTEXITCODE -eq 0 -and (($commandInfo | Out-String).Trim().Length -gt 0))
}

function Expand-Memurai {
    param([string]$ExtractDir)

    $memuraiExe = Join-Path $ExtractDir "Memurai\memurai.exe"
    if (Test-Path $memuraiExe) {
        return $memuraiExe
    }

    $cachedMsi = Get-ChildItem "$env:LOCALAPPDATA\Temp\WinGet" -Recurse -Filter "Memurai-Developer-v*.msi" -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
    if ($null -eq $cachedMsi) {
        throw "Memurai MSI not found. Run: winget download --id Memurai.MemuraiDeveloper"
    }

    New-Item -ItemType Directory -Force -Path $ExtractDir | Out-Null
    $logPath = Join-Path (Split-Path $ExtractDir -Parent) "memurai-extract.log"
    $process = Start-Process -FilePath "msiexec.exe" `
            -ArgumentList @("/a", $cachedMsi.FullName, "/qn", "TARGETDIR=$ExtractDir", "/L*v", $logPath) `
            -Wait `
            -PassThru `
            -WindowStyle Hidden
    if ($process.ExitCode -ne 0 -or -not (Test-Path $memuraiExe)) {
        throw "Memurai extraction failed. See $logPath"
    }

    return $memuraiExe
}

$resolvedExtractDir = Resolve-RepoPath $ExtractDir
$resolvedDataDir = Resolve-RepoPath $DataDir
$resolvedLogFile = Resolve-RepoPath $LogFile
$memuraiExe = Expand-Memurai -ExtractDir $resolvedExtractDir

if (Test-TcpPort -Port $Port) {
    if (Test-GeoAddSupport -Port $Port) {
        Write-Host "[OK] Redis-compatible GEO server already listening on localhost:$Port"
        exit 0
    }
    throw "Port $Port is already in use, but GEOADD is not supported. Use another -Port or stop that service."
}

New-Item -ItemType Directory -Force -Path $resolvedDataDir | Out-Null
$arguments = @(
    "--port", "$Port",
    "--dir", $resolvedDataDir,
    "--logfile", $resolvedLogFile
)

Start-Process -FilePath $memuraiExe -ArgumentList $arguments -WindowStyle Hidden

$deadline = (Get-Date).AddSeconds(15)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 500
    if ((Test-TcpPort -Port $Port) -and (Test-GeoAddSupport -Port $Port)) {
        Write-Host "[OK] Redis-compatible GEO server started on localhost:$Port"
        exit 0
    }
}

throw "Memurai did not become ready on localhost:$Port. See $resolvedLogFile"
