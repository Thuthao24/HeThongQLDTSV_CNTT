<#
.SYNOPSIS
.DESCRIPTION
.PARAMETER ResetCredentials
.PARAMETER NoBrowser
.PARAMETER Username
.PARAMETER Port
#>
[CmdletBinding()]
param(
    [switch]$ResetCredentials,
    [switch]$NoBrowser,
    [string]$Username = '',
    [int]$Port = 8081
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
$projectDirectory = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($projectDirectory)) {
    $projectDirectory = (Get-Location).Path
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   KHOI DONG HE THONG QUAN LY DE TAI SINH VIEN HCMUTE    " -ForegroundColor Cyan
Write-Host "                 CHE DO DEMO AN TOAN                     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Cau hinh tu dong:" -ForegroundColor White
Write-Host "  * Profile:            demo" -ForegroundColor Gray
Write-Host "  * Database:           quanlydetaisinhvien_demo (127.0.0.1:3306)" -ForegroundColor Gray
Write-Host "  * Database goc:       quanlydetaisinhvien (KHONG CHAM TOI)" -ForegroundColor Green
Write-Host "  * Port:               $Port" -ForegroundColor Gray
Write-Host "  * Email Delivery:     FALSE (Khong tu gui email)" -ForegroundColor Green
Write-Host "  * Email Scheduler:    FALSE (Tat scheduler ngam)" -ForegroundColor Green
Write-Host "  * Email Ket qua:      FALSE (Tat worker Module 16)" -ForegroundColor Green
Write-Host "  * DDL Auto / Init:    NONE / NEVER (Khong chay migration)" -ForegroundColor Green
Write-Host "----------------------------------------------------------" -ForegroundColor DarkGray

# 1. Kiem tra Port
$listeners = @([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -eq $Port })
if ($listeners.Count -gt 0) {
    Write-Host "Phat hien port $Port dang hoat dong." -ForegroundColor Yellow
    try {
        $testResp = Invoke-WebRequest -Uri "http://localhost:$Port/" -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        Write-Host "Ung dung Demo HCMUTE da dang chay tren port $Port!" -ForegroundColor Green
        if (-not $NoBrowser) {
            Write-Host "Dang mo trinh duyet den trang Giam sat Email..." -ForegroundColor Cyan
            Start-Process "http://localhost:$Port/morong/email-thongbao/quanly"
        }
        return
    } catch {
        Write-Host "Port $Port dang bi chiem boi tien trinh khac. Vui long dong tien trinh do truoc." -ForegroundColor Red
        return
    }
}

# 2. Quan ly thong tin dang nhap MySQL (An toan voi Windows DPAPI, khong plaintext)
$credFile = Join-Path $env:USERPROFILE '.quanlydetaisinhvien_demo_creds.clixml'

if ($ResetCredentials -and (Test-Path -LiteralPath $credFile)) {
    Remove-Item -LiteralPath $credFile -Force
    Write-Host "Da xoa thong tin dang nhap MySQL da luu truoc do." -ForegroundColor Yellow
}

$mysqlUser = $null
$mysqlPass = $null

# Kiem tra xem bien moi truong da co san chua
if (-not [string]::IsNullOrWhiteSpace($env:DEMO_DB_USERNAME) -and -not [string]::IsNullOrWhiteSpace($env:DEMO_DB_PASSWORD)) {
    $mysqlUser = $env:DEMO_DB_USERNAME
    $mysqlPass = $env:DEMO_DB_PASSWORD
    Write-Host "Su dung tai khoan MySQL tu bien moi truong hien tai ($mysqlUser)." -ForegroundColor Cyan
}
# Neu da co file credential ma hoa DPAPI
elseif (Test-Path -LiteralPath $credFile) {
    try {
        $storedCred = Import-Clixml -LiteralPath $credFile
        $mysqlUser = $storedCred.UserName
        $mysqlPass = $storedCred.GetNetworkCredential().Password
        Write-Host "Da tai thong tin MySQL cho user '$mysqlUser' (Ma hoa Windows DPAPI an toan)." -ForegroundColor Green
    } catch {
        Write-Warning "Khong the doc file dang nhap da luu ($($_.Exception.Message)). Vui long nhap lai."
        Remove-Item -LiteralPath $credFile -Force -ErrorAction SilentlyContinue
    }
}

# Neu chua co thong tin, yeu cau nguoi dung nhap 1 lan
if ([string]::IsNullOrWhiteSpace($mysqlUser) -or [string]::IsNullOrWhiteSpace($mysqlPass)) {
    $defaultUser = if (-not [string]::IsNullOrWhiteSpace($Username)) { $Username } else { 'root' }
    $inputUser = Read-Host "Nhap MySQL username [$defaultUser]"
    $mysqlUser = if ([string]::IsNullOrWhiteSpace($inputUser)) { $defaultUser } else { $inputUser.Trim() }
    
    $securePass = Read-Host "Nhap MySQL password (an)" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePass)
    try {
        $mysqlPass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        if ($ptr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
    }
    
    if ([string]::IsNullOrWhiteSpace($mysqlPass)) {
        Write-Host "Loi: Mat khau MySQL khong duoc de trong." -ForegroundColor Red
        return
    }
    
    # Hoi luu DPAPI
    $askSave = Read-Host "Ban co muon luu an toan mat khau nay (ma hoa bang Windows DPAPI) cho cac lan sau? (Y/n) [Mac dinh: Y]"
    if ([string]::IsNullOrWhiteSpace($askSave) -or $askSave.Trim().ToUpper() -eq 'Y') {
        try {
            $psCred = New-Object System.Management.Automation.PSCredential($mysqlUser, $securePass)
            $psCred | Export-Clixml -LiteralPath $credFile
            Write-Host "Da luu thong tin dang nhap tai: $credFile" -ForegroundColor Cyan
            Write-Host "(Luu y: Mat khau duoc ma hoa bang Windows Data Protection API cua tai khoan ban, khong luu plaintext va khong nam trong Git)." -ForegroundColor DarkGray
        } catch {
            Write-Warning "Khong the luu file credential: $($_.Exception.Message)"
        }
    }
    $securePass.Dispose()
}

# 3. Kiem tra ket noi MySQL den database quanlydetaisinhvien_demo
Write-Host "Kiem tra ket noi den MySQL Database 'quanlydetaisinhvien_demo'..." -NoNewline
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (-not (Test-Path -LiteralPath $mysqlBin)) {
    $found = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($found) { $mysqlBin = $found.Source }
}

if (Test-Path -LiteralPath $mysqlBin) {
    $testProcInfo = New-Object Diagnostics.ProcessStartInfo
    $testProcInfo.FileName = $mysqlBin
    $testProcInfo.Arguments = "--protocol=TCP --host=127.0.0.1 --port=3306 --user=$mysqlUser --default-character-set=utf8mb4 quanlydetaisinhvien_demo -e ""SELECT 1;"""
    $testProcInfo.UseShellExecute = $false
    $testProcInfo.CreateNoWindow = $true
    $testProcInfo.RedirectStandardError = $true
    $testProcInfo.RedirectStandardOutput = $true
    $testProcInfo.EnvironmentVariables['MYSQL_PWD'] = $mysqlPass
    
    $testProc = [Diagnostics.Process]::Start($testProcInfo)
    $testErr = $testProc.StandardError.ReadToEnd()
    [void]$testProc.WaitForExit(10000)
    if ($testProc.ExitCode -ne 0) {
        Write-Host " [THAT BAI]" -ForegroundColor Red
        Write-Host "Loi ket noi MySQL: $testErr" -ForegroundColor Red
        Write-Host "Goi y: Kiem tra MySQL server co dang bat khong, hoac chay lai voi tham so -ResetCredentials de doi mat khau." -ForegroundColor Yellow
        return
    }
    Write-Host " [OK]" -ForegroundColor Green
} else {
    Write-Host " [Bo qua kiem tra truc tiep mysql.exe, tiep tuc khoi dong]" -ForegroundColor Yellow
}

# 4. Thiet lap cac bien moi truong cho Profile Demo
$env:DEMO_DB_USERNAME = $mysqlUser
$env:DEMO_DB_PASSWORD = $mysqlPass
$env:DEMO_DB_HOST = '127.0.0.1'
$env:DEMO_DB_PORT = '3306'
$env:DEMO_PORT = [string]$Port
$env:SPRING_PROFILES_ACTIVE = 'demo'

# 5. Background Monitor de tu dong mo trinh duyet khi server san sang
if (-not $NoBrowser) {
    $monitorScript = {
        param($TargetPort)
        $url = "http://localhost:$TargetPort"
        $targetPage = "http://localhost:$TargetPort/morong/email-thongbao/quanly"
        for ($i = 0; $i -lt 120; $i++) {
            Start-Sleep -Seconds 1
            try {
                $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
                if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) {
                    Start-Sleep -Milliseconds 800
                    Start-Process $targetPage
                    break
                }
            } catch {}
        }
    }
    Start-Job -ScriptBlock $monitorScript -ArgumentList $Port | Out-Null
    Write-Host "Trinh duyet se tu dong mo trang Giam sat Email khi ung dung khoi dong xong." -ForegroundColor Cyan
}

# 6. Tim lenh Maven
$mvnCmd = 'mvn'
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    if (Test-Path 'C:\apache-maven-3.9.16\bin\mvn.cmd') {
        $mvnCmd = 'C:\apache-maven-3.9.16\bin\mvn.cmd'
    }
}

# 7. Cac tham so Spring Boot chac chan bao ve an toan tuyet doi
$springArgs = @(
    '--spring.profiles.active=demo',
    "--server.port=$Port",
    '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien_demo',
    '--spring.jpa.hibernate.ddl-auto=none',
    '--spring.jpa.generate-ddl=false',
    '--spring.sql.init.mode=never',
    '--app.thongbao.email.outbox-enabled=true',
    '--app.thongbao.email.delivery-enabled=false',
    '--app.thongbao.email.schedule-enabled=false',
    '--app.thongbao.email.max-recipients-per-notice=1000',
    "--app.thongbao.email.base-url=http://localhost:$Port",
    '--app.thongbao.email.smtp.host=',
    '--app.thongbao.email.smtp.username=',
    '--app.thongbao.email.smtp.password=',
    '--app.thongbao.email.smtp.from=',
    '--app.ketqua.email.enabled=false',
    '--app.ketqua.email.scheduling-enabled=false',
    '--spring.mail.test-connection=false',
    '--spring.mail.host=',
    '--spring.mail.username=',
    '--spring.mail.password='
)
$argLine = ($springArgs -join ' ')

Write-Host "Dang khoi dong Spring Boot (Profile: demo, Port: $Port)..." -ForegroundColor Green
Write-Host "Nhan Ctrl+C de dung ung dung.`n" -ForegroundColor Yellow

Push-Location $projectDirectory
try {
    & $mvnCmd spring-boot:run "-Dspring-boot.run.profiles=demo" "-Dspring-boot.run.arguments=$argLine"
} finally {
    Pop-Location
    # Don dep bien moi truong chua mat khau sau khi tat
    $env:DEMO_DB_PASSWORD = $null
}
