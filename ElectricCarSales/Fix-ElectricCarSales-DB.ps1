param(
    [string]$ProjectRoot = (Get-Location).Path
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path -LiteralPath $ProjectRoot).Path
$javaRoot = Join-Path $ProjectRoot 'src\main\java'
$pom = Join-Path $ProjectRoot 'pom.xml'
$financeFile = Join-Path $javaRoot 'com\electriccarsales\dao\FinanceConnection.java'
$dbContextFile = Join-Path $javaRoot 'util\DBContext.java'

if (-not (Test-Path -LiteralPath $pom)) {
    throw 'pom.xml was not found. Run from the ElectricCarSales project root.'
}
foreach ($path in @($financeFile, $dbContextFile)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing required Java file: $path"
    }
}

$finance = [System.IO.File]::ReadAllText($financeFile)
$db = [System.IO.File]::ReadAllText($dbContextFile)
$pattern = 'if\s*\(\s*url\s*==\s*null\s*\|\|\s*url\.isBlank\(\)\s*\)\s*\{\s*return\s+DBContext\.openConnection\(\);\s*\}'
$matches = [regex]::Matches($finance, $pattern)
if ($matches.Count -gt 1) {
    throw 'Ambiguous FinanceConnection.open() fallback. No files changed.'
}
$needsFinance = $matches.Count -eq 1
if (-not $needsFinance -and $finance -notmatch 'Missing ELECTRIC_CAR_SALES_JDBC_URL') {
    throw 'FinanceConnection.java differs from the reviewed version. No files changed to avoid overwriting your code.'
}

$oldLog = 'System.out.println("Khong ket noi duoc roi em oi!!!!!");'
$needsLog = $db.Contains($oldLog)
if ($needsLog -and -not ($db.Contains('import java.util.logging.Logger;') -and $db.Contains('import java.util.logging.Level;'))) {
    throw 'DBContext.java is missing logging imports; no files changed.'
}
if (-not $needsFinance -and -not $needsLog) {
    Write-Host 'Already fixed: no source code changes required.'
    exit 0
}

$backup = Join-Path $ProjectRoot ('db-login-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backup -Force | Out-Null
$encoding = New-Object System.Text.UTF8Encoding($false)

if ($needsFinance) {
    Copy-Item -LiteralPath $financeFile -Destination (Join-Path $backup 'FinanceConnection.java')
    $replacement = @'
if (url == null || url.isBlank()) {
            throw new SQLException("Missing ELECTRIC_CAR_SALES_JDBC_URL in Tomcat environment variables.");
        }
'@
    $finance = [regex]::Replace($finance, $pattern, $replacement)
    $finance = $finance.Replace('import util.DBContext;' + "`r`n", '').Replace('import util.DBContext;' + "`n", '')
    [System.IO.File]::WriteAllText($financeFile, $finance, $encoding)
    Write-Host 'Fixed: FinanceConnection.open() no longer calls DBContext.openConnection() recursively.'
}
if ($needsLog) {
    Copy-Item -LiteralPath $dbContextFile -Destination (Join-Path $backup 'DBContext.java')
    $db = $db.Replace($oldLog, 'Logger.getLogger(DBContext.class.getName()).log(Level.SEVERE, "SQL Server connection failed", ex);')
    [System.IO.File]::WriteAllText($dbContextFile, $db, $encoding)
    Write-Host 'Fixed: DBContext now prints the original SQLException and stack trace.'
}
Write-Host "Backups: $backup"
Write-Host 'Next: Set the 3 SQL Server environment variables in the Tomcat configuration; restart Tomcat and retry login/register.'
