# Run this helper manually in the IntelliJ PowerShell terminal.
# It never writes a password to a file or passes it as a Java argument.
#Requires -Version 5.1
[CmdletBinding()]
param(
    [ValidatePattern('^[A-Za-z0-9_.-]{3,50}$')]
    [string]$Username = 'admin',
    [string]$JavaHome
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$webInf = Join-Path $projectRoot 'ElectricCarSales/target/ElectricCarSales/WEB-INF'
$classesPath = Join-Path $webInf 'classes'
$libPath = Join-Path $webInf 'lib'
$bootstrapClass = Join-Path $classesPath 'vn/edu/fpt/electriccarsales/util/BootstrapAdmin.class'
if (-not (Test-Path -LiteralPath $bootstrapClass -PathType Leaf) -or
    -not (Test-Path -LiteralPath $libPath -PathType Container)) {
    throw 'Chua co ban build WAR. Chay Maven verify trong ElectricCarSales, sau do chay lai script.'
}
if (@(Get-ChildItem -LiteralPath $libPath -Filter 'mssql-jdbc-*.jar' -File).Count -eq 0) {
    throw 'Ban build thieu SQL Server JDBC driver. Chay Maven verify va thu lai.'
}

$javaCandidates = if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    @($env:JAVA_HOME, 'C:\Program Files\Java\jdk-24\New Folder')
} else {
    @($JavaHome)
}
$javaExe = $null
foreach ($candidate in $javaCandidates) {
    if ([string]::IsNullOrWhiteSpace($candidate)) {
        continue
    }
    $candidateExe = Join-Path $candidate 'bin/java.exe'
    $releaseFile = Join-Path $candidate 'release'
    if (-not (Test-Path -LiteralPath $candidateExe -PathType Leaf) -or
        -not (Test-Path -LiteralPath $releaseFile -PathType Leaf)) {
        continue
    }
    $releaseText = Get-Content -LiteralPath $releaseFile -Raw
    $version = [regex]::Match($releaseText, '(?m)^JAVA_VERSION="(?<version>[^"]+)"').Groups['version'].Value
    if ($version -match '^24(?:\.|-|$)') {
        $javaExe = (Resolve-Path -LiteralPath $candidateExe).Path
        break
    }
}
if ($null -eq $javaExe) {
    throw 'Khong tim thay JDK 24. Dat JAVA_HOME hoac truyen -JavaHome voi duong dan JDK 24.'
}

$javaArguments = @('--enable-native-access=ALL-UNNAMED')
$authDllDirectory = Join-Path $projectRoot '.codex-tools/jdbc-auth'
if (Test-Path -LiteralPath $authDllDirectory -PathType Container) {
    if (@(Get-ChildItem -LiteralPath $authDllDirectory -Filter 'mssql-jdbc_auth-13.4.0.x64.dll' -File).Count -gt 0) {
        $javaArguments += "-Djava.library.path=$authDllDirectory"
    }
}
$javaArguments += @('-cp', "$classesPath;$libPath/*", 'vn.edu.fpt.electriccarsales.util.BootstrapAdmin')

$previousUsername = [Environment]::GetEnvironmentVariable('ADMIN_USERNAME', 'Process')
$previousPassword = [Environment]::GetEnvironmentVariable('ADMIN_PASSWORD', 'Process')
$passwordSecure = $null
$confirmationSecure = $null
$passwordBstr = [IntPtr]::Zero
$confirmationBstr = [IntPtr]::Zero
$passwordText = $null
$confirmationText = $null
try {
    $passwordSecure = Read-Host 'Nhap mat khau Admin (10-128 ky tu)' -AsSecureString
    if ($passwordSecure.Length -lt 10 -or $passwordSecure.Length -gt 128) {
        throw 'Mat khau phai dai tu 10 den 128 ky tu. Chay lai script de nhap lai.'
    }
    $confirmationSecure = Read-Host 'Nhap lai mat khau Admin' -AsSecureString
    try {
        $passwordBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwordSecure)
        $confirmationBstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirmationSecure)
        $passwordText = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordBstr)
        $confirmationText = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($confirmationBstr)
    } finally {
        if ($passwordBstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordBstr)
            $passwordBstr = [IntPtr]::Zero
        }
        if ($confirmationBstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($confirmationBstr)
            $confirmationBstr = [IntPtr]::Zero
        }
    }
    if (-not [string]::Equals($passwordText, $confirmationText, [StringComparison]::Ordinal)) {
        throw 'Hai mat khau khong trung nhau. Chay lai script de nhap lai.'
    }
    $confirmationText = $null
    [Environment]::SetEnvironmentVariable('ADMIN_USERNAME', $Username, 'Process')
    [Environment]::SetEnvironmentVariable('ADMIN_PASSWORD', $passwordText, 'Process')
    & $javaExe @javaArguments
    if ($LASTEXITCODE -ne 0) {
        throw "BootstrapAdmin khong thanh cong (exit code $LASTEXITCODE). Kiem tra ket noi DB va migration 01."
    }
} finally {
    [Environment]::SetEnvironmentVariable('ADMIN_USERNAME', $previousUsername, 'Process')
    [Environment]::SetEnvironmentVariable('ADMIN_PASSWORD', $previousPassword, 'Process')
    $passwordText = $null
    $confirmationText = $null
    $previousPassword = $null
    if ($null -ne $passwordSecure) {
        $passwordSecure.Dispose()
    }
    if ($null -ne $confirmationSecure) {
        $confirmationSecure.Dispose()
    }
}
