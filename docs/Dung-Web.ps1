# Chỉ dừng Tomcat do Chay-Web.ps1 tạo; không dừng server IntelliJ hoặc Java khác.
#Requires -Version 5.1
[CmdletBinding()]
param([ValidateRange(1024, 65535)][int]$Port = 8081)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$serverBase = Join-Path $projectRoot "tool/tomcat-local-$Port"
$statePath = Join-Path $serverBase 'server.json'
if (-not (Test-Path -LiteralPath $statePath)) {
    Write-Output 'Khong co web do script nay khoi dong.';
    return
}
$saved = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$running = Get-CimInstance Win32_Process -Filter "ProcessId = $($saved.processId)" -ErrorAction SilentlyContinue
if ($null -eq $running) {
    Write-Output 'Web da dung.';
    return
}
if (-not $running.CommandLine.Contains("-Dcatalina.base=$serverBase") -or $running.Name -ne 'java.exe') {
    throw 'PID khong con thuoc Tomcat nay. Khong dung tien trinh.'
}
Stop-Process -Id $saved.processId
Write-Output 'Da dung web.'
