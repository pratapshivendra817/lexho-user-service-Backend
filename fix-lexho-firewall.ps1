Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$port = 8082
$ruleNames = @(
    "Lexho Backend 8082",
    "Lexho Backend 8082 TCP",
    "Lexho Backend Java"
)

Write-Host "Applying Lexho backend firewall fix..."

$profiles = @(
    "HKLM:\SOFTWARE\Policies\Microsoft\WindowsFirewall\StandardProfile",
    "HKLM:\SOFTWARE\Policies\Microsoft\WindowsFirewall\DomainProfile",
    "HKLM:\SOFTWARE\Policies\Microsoft\WindowsFirewall\PublicProfile"
)

foreach ($profile in $profiles) {
    if (-not (Test-Path $profile)) {
        New-Item -Path $profile -Force | Out-Null
    }
    New-ItemProperty -Path $profile -Name "DoNotAllowExceptions" -PropertyType DWord -Value 0 -Force | Out-Null
    New-ItemProperty -Path $profile -Name "AllowLocalPolicyMerge" -PropertyType DWord -Value 1 -Force | Out-Null
    New-ItemProperty -Path $profile -Name "EnableFirewall" -PropertyType DWord -Value 1 -Force | Out-Null
}

foreach ($ruleName in $ruleNames) {
    Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule
}

New-NetFirewallRule `
    -DisplayName "Lexho Backend 8082 TCP" `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort $port `
    -Profile Private,Public,Domain `
    -Enabled True | Out-Null

$javaProcess = Get-Process java -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -and $_.Path -like "*\jdk-17\bin\java.exe" } |
    Select-Object -First 1

if ($javaProcess) {
    New-NetFirewallRule `
        -DisplayName "Lexho Backend Java" `
        -Direction Inbound `
        -Action Allow `
        -Program $javaProcess.Path `
        -Profile Private,Public,Domain `
        -Enabled True | Out-Null
}

Get-NetConnectionProfile -ErrorAction SilentlyContinue |
    Where-Object { $_.InterfaceAlias -eq "Wi-Fi" } |
    Set-NetConnectionProfile -NetworkCategory Private

Set-NetFirewallProfile -Profile Public,Private,Domain -Enabled True
gpupdate /force | Out-Host

Write-Host ""
Write-Host "Done. Current firewall rules:"
Get-NetFirewallRule -DisplayName "Lexho Backend 8082 TCP","Lexho Backend Java" -ErrorAction SilentlyContinue |
    Format-Table DisplayName,Enabled,Direction,Action,Profile -AutoSize

Write-Host ""
Write-Host "Press Enter to close this window."
Read-Host | Out-Null
