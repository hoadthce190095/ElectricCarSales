[CmdletBinding()]
param(
    [string]$ConfigDirectory = (Join-Path ([Environment]::GetFolderPath('UserProfile')) '.electriccarsales')
)

$ErrorActionPreference = 'Stop'
$source = Join-Path $PSScriptRoot 'ElectricCarSales/src/main/resources/db.properties.example'
$destination = Join-Path $ConfigDirectory 'db.properties'
New-Item -ItemType Directory -Path $ConfigDirectory -Force | Out-Null

if (Test-Path -LiteralPath $destination) {
    Write-Output "Kept existing database configuration at $destination"
} else {
    Copy-Item -LiteralPath $source -Destination $destination
    Write-Output "Created $destination"
}

Write-Output 'Edit db.properties with the settings for your SQL Server, then restart Tomcat.'
