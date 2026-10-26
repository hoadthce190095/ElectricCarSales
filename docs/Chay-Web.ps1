# Chạy web bằng Tomcat đã cài trên máy, không thay đổi cấu hình IntelliJ.
#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$TomcatHome = $env:CATALINA_HOME,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenHome = $env:MAVEN_HOME,
    [ValidateRange(1024, 65535)][int]$Port = 8081,
    [switch]$Build,
    [switch]$Restart,
    [switch]$OpenBrowser
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    throw 'Dat JAVA_HOME hoac truyen -JavaHome toi JDK 24.'
}
if ([string]::IsNullOrWhiteSpace($TomcatHome)) {
    throw 'Dat CATALINA_HOME hoac truyen -TomcatHome toi Tomcat 10.1.'
}
$javaExe = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    throw 'Khong tim thay Java. Truyen -JavaHome toi JDK 24.'
}
if (-not (Test-Path -LiteralPath (Join-Path $TomcatHome 'bin/bootstrap.jar'))) {
    throw 'Khong tim thay Tomcat. Truyen -TomcatHome toi Tomcat 10.1.'
}
$release = Get-Content -LiteralPath (Join-Path $JavaHome 'release') -Raw
if ($release -notmatch '(?m)^JAVA_VERSION="24(?:\.|-|\")') {
    throw 'Ban build nay dung JDK 24.'
}
if ($Restart) {
    & (Join-Path $PSScriptRoot 'Dung-Web.ps1') -Port $Port
}
$listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($null -ne $listener -and ($Build -or $Restart)) {
    throw "Cong $Port da duoc dung. Dung server dang dung cong nay hoac them -Port 8082."
}
$webRoot = Join-Path $projectRoot 'ElectricCarSales/target/ElectricCarSales'
if ($Build -or -not (Test-Path -LiteralPath (Join-Path $webRoot 'WEB-INF/classes'))) {
    $mavenExe = $null
    if (-not [string]::IsNullOrWhiteSpace($MavenHome)) {
        $mavenExe = Join-Path $MavenHome 'bin/mvn.cmd'
        if (-not (Test-Path -LiteralPath $mavenExe -PathType Leaf)) {
            throw 'Khong tim thay Maven trong -MavenHome/MAVEN_HOME.'
        }
    }
    if ($null -eq $mavenExe) {
        $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
        if ($null -ne $mavenCommand) {
            $mavenExe = $mavenCommand.Source
        }
    }
    if ($null -eq $mavenExe) {
        $wrapperRoot = Join-Path $env:USERPROFILE '.m2/wrapper/dists'
        if (Test-Path -LiteralPath $wrapperRoot) {
            $mavenExe = Get-ChildItem -LiteralPath $wrapperRoot -Filter 'mvn.cmd' -File -Recurse |
                Select-Object -First 1 -ExpandProperty FullName
        }
    }
    if ([string]::IsNullOrWhiteSpace($mavenExe)) {
        throw 'Chay Maven verify trong IntelliJ truoc, hoac cai Maven vao PATH.'
    }
    $previousJavaHome = $env:JAVA_HOME
    try {
        $env:JAVA_HOME = $JavaHome
        & $mavenExe -f (Join-Path $projectRoot 'ElectricCarSales/pom.xml') clean verify
        if ($LASTEXITCODE -ne 0) {
            throw 'Maven verify khong thanh cong.'
        }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
    }
}
$serverBase = Join-Path $projectRoot "tool/tomcat-local-$Port"
$statePath = Join-Path $serverBase 'server.json'
$url = "http://localhost:$Port/ElectricCarSales/"
$healthUrl = "http://127.0.0.1:$Port/ElectricCarSales/"
$listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($null -ne $listener) {
    if (Test-Path -LiteralPath $statePath) {
        $saved = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
        $running = Get-CimInstance Win32_Process -Filter "ProcessId = $($saved.processId)" -ErrorAction SilentlyContinue
        if ($null -ne $running -and $running.CommandLine.Contains("-Dcatalina.base=$serverBase") -and
            (@($listener.OwningProcess) -contains [int]$saved.processId)) {
            Write-Output "Web dang chay: $url"
            return
        }
    }
    throw "Cong $Port da duoc dung. Dung server dang dung cong nay hoac them -Port 8082."
}
foreach ($folder in @('conf', 'conf/Catalina/localhost', 'logs', 'temp', 'work', 'webapps')) {
    New-Item -ItemType Directory -Path (Join-Path $serverBase $folder) -Force | Out-Null
}
foreach ($file in @('web.xml', 'context.xml', 'catalina.properties', 'logging.properties')) {
    Copy-Item -LiteralPath (Join-Path $TomcatHome "conf/$file") -Destination (Join-Path $serverBase "conf/$file") -Force
}
$serverConfig = @"
<?xml version="1.0" encoding="UTF-8"?>
<Server port="-1">
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
  <Service name="Catalina">
    <Connector address="127.0.0.1" port="$Port" protocol="HTTP/1.1" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false" />
    </Engine>
  </Service>
</Server>
"@
$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText((Join-Path $serverBase 'conf/server.xml'), $serverConfig, $utf8)
$escapedRoot = [System.Security.SecurityElement]::Escape($webRoot)
[IO.File]::WriteAllText((Join-Path $serverBase 'conf/Catalina/localhost/ElectricCarSales.xml'),
    "<?xml version=`"1.0`" encoding=`"UTF-8`"?><Context docBase=`"$escapedRoot`" reloadable=`"false`" />", $utf8)
$arguments = @('--enable-native-access=ALL-UNNAMED', '-Dapp.adminSetup.enabled=true',
    "`"-Dcatalina.home=$TomcatHome`"", "`"-Dcatalina.base=$serverBase`"",
    "`"-Djava.io.tmpdir=$(Join-Path $serverBase 'temp')`"",
    '-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager',
    "`"-Djava.util.logging.config.file=$(Join-Path $serverBase 'conf/logging.properties')`"")
$authDirectory = Join-Path $projectRoot 'tool/jdbc-auth'
if (Test-Path -LiteralPath (Join-Path $authDirectory 'mssql-jdbc_auth-13.4.0.x64.dll')) {
    $arguments += "`"-Djava.library.path=$authDirectory`""
}
$arguments += @('-cp', "`"$(Join-Path $TomcatHome 'bin/bootstrap.jar');$(Join-Path $TomcatHome 'bin/tomcat-juli.jar')`"",
    'org.apache.catalina.startup.Bootstrap', 'start')
$process = Start-Process -FilePath $javaExe -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $serverBase 'logs/stdout.log') -RedirectStandardError (Join-Path $serverBase 'logs/stderr.log')
@{
    processId = $process.Id;
    serverBase = $serverBase;
    url = $url
} | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8
$ready = $false
for ($attempt = 0; $attempt -lt 10; $attempt++) {
    if ($process.HasExited) {
        break
    }
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -eq 200) {
            $ready = $true;
            break
        }
    } catch {
        Start-Sleep -Milliseconds 500
    }
}
if (-not $ready) {
    throw "Web chua san sang. Xem $serverBase\logs\stderr.log"
}
try {
    $catalogue = Invoke-RestMethod -Uri ($healthUrl + 'api/ai') -TimeoutSec 10
} catch {
    throw "Tomcat da chay nhung chua doc duoc SQL Server. Kiem tra db.properties/DB_* va log $serverBase\logs\stderr.log"
}
Write-Output "Web da chay: $url"
Write-Output "Danh muc SQL Server: $(@($catalogue.models).Count) phien ban xe."
Write-Output 'Lan dau: chon Tao Admin dau tien, tu dat mat khau, sau do dang nhap de xem bao cao.'
if ($OpenBrowser) {
    Start-Process -FilePath $url
}
