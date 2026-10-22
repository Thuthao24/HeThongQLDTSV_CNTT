<#
.SYNOPSIS
    Khởi động ứng dụng Quản lý đề tài sinh viên HCMUTE ở chế độ DEMO TỰ ĐỘNG GỬI GMAIL THẬT.
.DESCRIPTION
    Script tự động:
    - Kích hoạt profile 'demo-auto', database 'quanlydetaisinhvien_demo', cổng 8081.
    - Tự động bật Worker và Scheduler (polling mỗi 3s) để tự động gửi Gmail thật.
    - CƠ CHẾ KỸ THUẬT BẢO VỆ 23 EMAIL CŨ:
      Tự động tính minNoticeId = MAX(Id hiện tại) + 1. Loại trừ 100% tất cả thông báo cũ (như đợt thử notice 5).
      Chỉ những thông báo MỚI được tạo sau khi script chạy mới được phép gửi.
    - MẶC ĐỊNH AN TOÀN:
      Chỉ gửi cho sinh viên ID 1 (hnghi.dong01@gmail.com). Hỗ trợ ID thông báo mới tạo từ website.
    - CHẾ ĐỘ TOÀN BỘ SINH VIÊN (-AllStudents):
      Bắt buộc hiển thị bảng danh sách người nhận và yêu cầu người dùng phê duyệt trước khi kích hoạt.
    - Tắt hoàn toàn Emailketqua worker (Module 16) và bảo vệ database gốc 'quanlydetaisinhvien'.
    - Quản lý mật khẩu MySQL và Gmail App Password an toàn qua Windows DPAPI (không plaintext, không vào Git).
    - Tự động mở trình duyệt tới trang Giám sát Email khi server sẵn sàng.
.PARAMETER AllStudents
    Chế độ gửi cho toàn bộ sinh viên có email hợp lệ (yêu cầu phê duyệt danh sách trước).
.PARAMETER ResetCredentials
    Xóa thông tin đăng nhập MySQL và Gmail đã lưu để nhập lại.
.PARAMETER NoBrowser
    Không tự động mở trình duyệt.
.PARAMETER InitBaseline
    Khởi tạo lại ngưỡng bảo vệ minNoticeId từ database hiện tại và lưu vào vị trí bền vững.
.PARAMETER Port
    Cổng chạy ứng dụng (mặc định: 8081).
#>
[CmdletBinding()]
param(
    [switch]$AllStudents,
    [switch]$ResetCredentials,
    [switch]$NoBrowser,
    [switch]$InitBaseline,
    [int]$Port = 8081
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
$projectDirectory = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($projectDirectory)) {
    $projectDirectory = (Get-Location).Path
}

Write-Host "==========================================================" -ForegroundColor Magenta
Write-Host "   KHOI DONG HE THONG QUAN LY DE TAI SINH VIEN HCMUTE    " -ForegroundColor Magenta
Write-Host "        CHE DO DEMO TU DONG GUI GMAIL THAT               " -ForegroundColor Magenta
Write-Host "==========================================================" -ForegroundColor Magenta

# 1. Kiem tra Port
$listeners = @([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -eq $Port })
if ($listeners.Count -gt 0) {
    Write-Host "Phat hien port $Port dang hoat dong. Vui long dong tien trinh cu truoc khi chay che do gui tu dong." -ForegroundColor Red
    return
}

# 2. Quan ly thong tin MySQL an toan (Windows DPAPI)
$dbCredFile = Join-Path $env:USERPROFILE '.quanlydetaisinhvien_demo_creds.clixml'
$smtpCredFile = Join-Path $env:USERPROFILE '.quanlydetaisinhvien_demo_smtp.clixml'

if ($ResetCredentials) {
    if (Test-Path -LiteralPath $dbCredFile) { Remove-Item -LiteralPath $dbCredFile -Force; Write-Host "Da xoa thong tin MySQL da luu." -ForegroundColor Yellow }
    if (Test-Path -LiteralPath $smtpCredFile) { Remove-Item -LiteralPath $smtpCredFile -Force; Write-Host "Da xoa thong tin Gmail SMTP da luu." -ForegroundColor Yellow }
}

$mysqlUser = $null; $mysqlPass = $null
if (-not [string]::IsNullOrWhiteSpace($env:DEMO_DB_USERNAME) -and -not [string]::IsNullOrWhiteSpace($env:DEMO_DB_PASSWORD)) {
    $mysqlUser = $env:DEMO_DB_USERNAME; $mysqlPass = $env:DEMO_DB_PASSWORD
} elseif (Test-Path -LiteralPath $dbCredFile) {
    try {
        $storedDb = Import-Clixml -LiteralPath $dbCredFile
        $mysqlUser = $storedDb.UserName; $mysqlPass = $storedDb.GetNetworkCredential().Password
        Write-Host "Da tai thong tin MySQL cho user '$mysqlUser' (Windows DPAPI)." -ForegroundColor Green
    } catch {
        Remove-Item -LiteralPath $dbCredFile -Force -ErrorAction SilentlyContinue
    }
}

if ([string]::IsNullOrWhiteSpace($mysqlUser) -or [string]::IsNullOrWhiteSpace($mysqlPass)) {
    $inputUser = Read-Host "Nhap MySQL username [root]"
    $mysqlUser = if ([string]::IsNullOrWhiteSpace($inputUser)) { 'root' } else { $inputUser.Trim() }
    $securePass = Read-Host "Nhap MySQL password (an)" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePass)
    try { $mysqlPass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { if ($ptr -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) } }
    if ([string]::IsNullOrWhiteSpace($mysqlPass)) { Write-Host "Loi: Mat khau MySQL khong duoc de trong." -ForegroundColor Red; return }
    $askSaveDb = Read-Host "Luu an toan mat khau MySQL (Windows DPAPI) cho lan sau? (Y/n) [Mac dinh: Y]"
    if ([string]::IsNullOrWhiteSpace($askSaveDb) -or $askSaveDb.Trim().ToUpper() -eq 'Y') {
        try { (New-Object System.Management.Automation.PSCredential($mysqlUser, $securePass)) | Export-Clixml -LiteralPath $dbCredFile } catch {}
    }
    $securePass.Dispose()
}

# 3. Quan ly thong tin Gmail SMTP an toan (Windows DPAPI)
$smtpSender = $null; $smtpPassword = $null
if (-not [string]::IsNullOrWhiteSpace($env:THONGBAO_SMTP_USERNAME) -and -not [string]::IsNullOrWhiteSpace($env:THONGBAO_SMTP_PASSWORD)) {
    $smtpSender = $env:THONGBAO_SMTP_USERNAME; $smtpPassword = $env:THONGBAO_SMTP_PASSWORD
} elseif (Test-Path -LiteralPath $smtpCredFile) {
    try {
        $storedSmtp = Import-Clixml -LiteralPath $smtpCredFile
        $smtpSender = $storedSmtp.UserName; $smtpPassword = $storedSmtp.GetNetworkCredential().Password
        Write-Host "Da tai thong tin Gmail SMTP cho '$smtpSender' (Windows DPAPI)." -ForegroundColor Green
    } catch {
        Remove-Item -LiteralPath $smtpCredFile -Force -ErrorAction SilentlyContinue
    }
}

if ([string]::IsNullOrWhiteSpace($smtpSender) -or [string]::IsNullOrWhiteSpace($smtpPassword)) {
    $inputSender = (Read-Host "Nhap dia chi Gmail NGUOI GUI (co App Password, vi du: yourname@gmail.com)").Trim()
    if ($inputSender -notmatch '^[A-Za-z0-9][A-Za-z0-9._%+\-]{0,63}@gmail\.com$') {
        Write-Host "Loi: Dia chi Gmail nguoi gui khong hop le." -ForegroundColor Red; return
    }
    $smtpSender = $inputSender
    $secureSmtp = Read-Host "Nhap Gmail App Password 16 ky tu (an, KHONG phai mat khau email chinh)" -AsSecureString
    $ptrSmtp = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureSmtp)
    try { $smtpPassword = ([Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptrSmtp)).Replace(' ', '') } finally { if ($ptrSmtp -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptrSmtp) } }
    if ($smtpPassword.Length -ne 16) { Write-Host "Loi: Gmail App Password phai dung 16 ky tu." -ForegroundColor Red; return }
    $askSaveSmtp = Read-Host "Luu an toan Gmail App Password (Windows DPAPI) cho lan sau? (Y/n) [Mac dinh: Y]"
    if ([string]::IsNullOrWhiteSpace($askSaveSmtp) -or $askSaveSmtp.Trim().ToUpper() -eq 'Y') {
        try { (New-Object System.Management.Automation.PSCredential($smtpSender, $secureSmtp)) | Export-Clixml -LiteralPath $smtpCredFile } catch {}
    }
    $secureSmtp.Dispose()
}

# 4. Kiem tra MySQL va tinh toan CO CHE KY THUAT CHAN EMAIL CU
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
if (-not (Test-Path -LiteralPath $mysqlBin)) {
    $found = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($found) { $mysqlBin = $found.Source }
}

function Invoke-Sql([string]$Query) {
    if (-not (Test-Path -LiteralPath $mysqlBin)) {
        throw "Khong tim thay mysql.exe tai: $mysqlBin"
    }
    $pInfo = New-Object Diagnostics.ProcessStartInfo
    $pInfo.FileName = $mysqlBin
    $pInfo.Arguments = "--protocol=TCP --host=127.0.0.1 --port=3306 --user=$mysqlUser --default-character-set=utf8mb4 -s -N -r quanlydetaisinhvien_demo -e ""$Query"""
    $pInfo.UseShellExecute = $false
    $pInfo.CreateNoWindow = $true
    $pInfo.RedirectStandardOutput = $true
    $pInfo.RedirectStandardError = $true
    $pInfo.EnvironmentVariables['MYSQL_PWD'] = $mysqlPass
    
    $proc = [Diagnostics.Process]::Start($pInfo)
    try {
        $stdoutTask = $proc.StandardOutput.ReadToEndAsync()
        $stderrTask = $proc.StandardError.ReadToEndAsync()
        $exited = $proc.WaitForExit(15000)
        if (-not $exited) {
            try { $proc.Kill() } catch {}
            throw "Truy van MySQL qua thoi gian cho (15s): $Query"
        }
        $out = $stdoutTask.GetAwaiter().GetResult()
        $err = $stderrTask.GetAwaiter().GetResult()
        if ($proc.ExitCode -ne 0) {
            throw "Loi MySQL (ExitCode $($proc.ExitCode)): $err"
        }
        return [string]($out.Trim())
    } finally {
        $proc.Dispose()
    }
}

Write-Host "Kiem tra ket noi MySQL den database quanlydetaisinhvien_demo..." -NoNewline
try {
    $dbName = Invoke-Sql "SELECT DATABASE();"
    if ($dbName -ne 'quanlydetaisinhvien_demo') { throw "Sai database: $dbName (yeu cau quanlydetaisinhvien_demo)" }
    Write-Host " [OK]" -ForegroundColor Green
} catch {
    Write-Host " [THAT BAI]" -ForegroundColor Red
    Write-Host "Loi: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Goi y: Chay lai voi -ResetCredentials de nhap lai mat khau." -ForegroundColor Yellow
    return
}

# 4.2. Tinh toan va co dinh nguong bao ve 23 email cu (minNoticeId)
# Vi tri luu tru ben vung ngoai target/ (khong bi mat khi chay mvn clean):
$appDataDir = if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    Join-Path $env:LOCALAPPDATA 'Quanlydetaisinhvien'
} else {
    Join-Path $env:USERPROFILE 'AppData\Local\Quanlydetaisinhvien'
}
if (-not (Test-Path -LiteralPath $appDataDir)) {
    try { New-Item -ItemType Directory -Path $appDataDir -Force | Out-Null } catch {}
}
$baselineFile = Join-Path $appDataDir 'email-auto-mail-baseline.json'
$legacyTargetBaseline = Join-Path $projectDirectory 'target\email-auto-mail-baseline.json'

# Tu dong di chuyen tu target/ sang LocalAppData neu da co san tu truoc
if (-not (Test-Path -LiteralPath $baselineFile) -and (Test-Path -LiteralPath $legacyTargetBaseline)) {
    try {
        Copy-Item -LiteralPath $legacyTargetBaseline -Destination $baselineFile -Force
        Write-Host "Da di chuyen file baseline tu target/ sang vi tri ben vung: $baselineFile" -ForegroundColor Cyan
    } catch {
        Write-Warning "Khong the sao chep baseline tu target/: $($_.Exception.Message)"
    }
}

$minNoticeId = 0
$baseNoticeId = 0
$baselineLoaded = $false

# Neu khong yeu cau khoi tao lai (-InitBaseline) va da co file baseline hop le: doc lai truc tiep
if ((-not $InitBaseline) -and (Test-Path -LiteralPath $baselineFile)) {
    try {
        $rawJson = Get-Content -LiteralPath $baselineFile -Raw
        $saved = $rawJson | ConvertFrom-Json
        
        # Kiem tra tinh hop le va khop database
        if (-not $saved -or -not $saved.database) {
            throw "File baseline bi loi hoac thieu truong database."
        }
        if ($saved.database -ne 'quanlydetaisinhvien_demo') {
            throw "Database trong baseline ('$($saved.database)') khong khop voi database dang ket noi ('quanlydetaisinhvien_demo')."
        }
        if (-not $saved.minNoticeId -or [string]$saved.minNoticeId -notmatch '^\d+$' -or [long]$saved.minNoticeId -le 0) {
            throw "minNoticeId trong baseline khong hop le: '$($saved.minNoticeId)'."
        }

        $minNoticeId = [long]$saved.minNoticeId
        $baseNoticeId = if ($saved.baseNoticeId) { [long]$saved.baseNoticeId } else { $minNoticeId - 1 }
        $baselineLoaded = $true
        Write-Host "Su dung nguong minNoticeId da co dinh tu truoc: $minNoticeId (Notice <= $baseNoticeId bi loai tru 100%)" -ForegroundColor Cyan
        Write-Host "File baseline ben vung: $baselineFile" -ForegroundColor DarkGray
    } catch {
        Write-Host "`n[CANH BAO AN TOAN BASELINE]" -ForegroundColor Red
        Write-Host "Loi file baseline: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "DUNG KHOI DONG: He thong TU CHOI kich hoat SMTP de ngan ngua rui ro gui nham 23 email cu." -ForegroundColor Red
        Write-Host "Tuyet doi khong tu dong su dung minNoticeId=0 hoac tiep tuc khi baseline bi loi." -ForegroundColor Red
        Write-Host "Goi y: Kiem tra file '$baselineFile' hoac chay lai voi tham so '-InitBaseline' de khoi tao lai." -ForegroundColor Yellow
        return
    }
}

# Truong hop chua co baseline hoac yeu cau -InitBaseline
if (-not $baselineLoaded) {
    if ($InitBaseline) {
        Write-Host "Yeu cau khoi tao lai baseline bao ve (-InitBaseline)..." -ForegroundColor Yellow
    } else {
        Write-Host "`nCANH BAO AN TOAN: Chua tim thay file baseline bao ve tai: $baselineFile" -ForegroundColor Yellow
        $confirmInit = Read-Host "Ban co muon khoi tao baseline bao ve an toan lan dau cho database 'quanlydetaisinhvien_demo' khong? (Y/n) [Mac dinh: Y]"
        if (-not [string]::IsNullOrWhiteSpace($confirmInit) -and $confirmInit.Trim().ToUpper() -ne 'Y') {
            Write-Host "DUNG KHOI DONG: He thong khong the bat che do tu dong gui khi chua co baseline bao ve." -ForegroundColor Red
            return
        }
    }

    try {
        Write-Host "Dang phan tich database de tinh toan nguong bao ve an toan minNoticeId..." -ForegroundColor Cyan

        # 1. Thong bao cu chua 23 email PENDING cu (Notice ID tuong ung cac email co Id <= 24 trong emailthongbao)
        $legacyText = Invoke-Sql "SELECT COALESCE(MAX(Thongbaosnapshotid), 0) FROM quanlydetaisinhvien_demo.emailthongbao WHERE Id <= 24;"
        $cleanLegacy = $legacyText.Trim()
        if ($cleanLegacy -notmatch '^\d+$') {
            throw "Ket qua MAX Thongbaosnapshotid legacy khong hop le: '$cleanLegacy'"
        }
        $legacyNoticeId = [long]$cleanLegacy

        # 2. MAX(Id) thongbao hien co trong database
        $currentMaxText = Invoke-Sql "SELECT COALESCE(MAX(Id), 0) FROM quanlydetaisinhvien_demo.thongbao;"
        $cleanMax = $currentMaxText.Trim()
        if ($cleanMax -notmatch '^\d+$') {
            throw "Ket qua MAX(Id) thongbao khong hop le: '$cleanMax'"
        }
        $currentMaxId = [long]$cleanMax

        # 3. Nguong chan: phai lon hon thong bao legacy va tat ca thong bao cu hien co
        $baseNoticeId = [Math]::Max($legacyNoticeId, $currentMaxId)
        $minNoticeId = $baseNoticeId + 1

        if ($minNoticeId -le 0) {
            throw "Khong the xac dinh nguong minNoticeId hop le (> 0)."
        }

        $baselineData = [ordered]@{
            database = 'quanlydetaisinhvien_demo'
            legacyNoticeId = $legacyNoticeId
            initialMaxNoticeId = $currentMaxId
            baseNoticeId = $baseNoticeId
            minNoticeId = $minNoticeId
            createdAt = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
            description = "Co dinh nguong: chi gui thong bao co Id >= $minNoticeId. Cac thong bao cu (Id <= $baseNoticeId) bi loai tru 100%."
        }
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [IO.File]::WriteAllText($baselineFile, ($baselineData | ConvertTo-Json -Depth 5), $utf8NoBom)
        Write-Host "Da thiet lap va co dinh nguong bao ve moi vao file ben vung:" -ForegroundColor Green
        Write-Host "  * File:         $baselineFile" -ForegroundColor White
        Write-Host "  * minNoticeId:  $minNoticeId (Chan toan bo Notice ID <= $baseNoticeId)" -ForegroundColor Green
    } catch {
        Write-Host "Loi khoi tao minNoticeId: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "DUNG KHOI DONG: He thong tu choi chay voi minNoticeId=0." -ForegroundColor Red
        return
    }
}

# Kiem tra bao ve cuoi cung: tuyet doi khong cho phep minNoticeId <= 0
if ($minNoticeId -le 0) {
    Write-Host "LOI AN TOAN: minNoticeId=$minNoticeId khong hop le. TU CHOI khoi dong SMTP!" -ForegroundColor Red
    return
}

Write-Host "----------------------------------------------------------" -ForegroundColor DarkGray
Write-Host "CO CHE KY THUAT BAO VE 23 EMAIL CU:" -ForegroundColor Yellow
Write-Host "  * Thong bao cu bi loai tru:   Notice ID <= $baseNoticeId" -ForegroundColor Gray
Write-Host "  * Nguong thong bao moi:       minNoticeId = $minNoticeId" -ForegroundColor Green
Write-Host "  => Tat ca cac email cua thong bao cu bi CHAN 100% o cap Database SQL va Safety permitted()." -ForegroundColor Green
Write-Host "  => Worker chi xu ly thong bao MUA MOI tao tu giao dien website (ID >= $minNoticeId)." -ForegroundColor Green
Write-Host "----------------------------------------------------------" -ForegroundColor DarkGray

# 5. Xac dinh che do gui (Allowlist vs AllStudents)
$targetMode = 'ALLOWLIST'
$targetStudentId = 1
$targetAllowlist = 'hnghi.dong01@gmail.com'

if ($AllStudents) {
    Write-Host "`nCANH BAO: Ban dang yeu cau CHE DO GUI CHO TOAN BO SINH VIEN!" -ForegroundColor Red
    Write-Host "Danh sach nguoi nhan hop le hien co trong he thong:" -ForegroundColor Cyan
    try {
        $studentListRaw = Invoke-Sql "SELECT s.Id, s.Masinhvien, tk.Hoten, tk.Email FROM sinhvien s JOIN taikhoan tk ON tk.Id=s.Taikhoanid WHERE tk.Vaitro='SINHVIEN' AND tk.Kichhoat=TRUE AND tk.Email IS NOT NULL AND tk.Email LIKE '%@%';"
        $lines = $studentListRaw -split "`r?`n"
        Write-Host ("{0,-6} {1,-12} {2,-25} {3}" -f "ID", "MSSV", "HO TEN", "EMAIL") -ForegroundColor White
        Write-Host ("-" * 65) -ForegroundColor DarkGray
        foreach ($l in $lines) {
            $parts = $l -split "`t"
            if ($parts.Count -ge 4) {
                Write-Host ("{0,-6} {1,-12} {2,-25} {3}" -f $parts[0], $parts[1], $parts[2], $parts[3])
            }
        }
        Write-Host ("-" * 65) -ForegroundColor DarkGray
        Write-Host "Tong so sinh vien du dieu kien: $($lines.Count)" -ForegroundColor Cyan
    } catch {
        Write-Host "Loi truy van danh sach sinh vien: $($_.Exception.Message)" -ForegroundColor Red; return
    }

    $confirm = Read-Host "Ban co dong y PHE DUYET danh sach nguoi nhan tren de bat che do gui hang loat khong? (Y/n)"
    if ($confirm.Trim().ToUpper() -ne 'Y') {
        Write-Host "Da huy kich hoat gui hang loat. He thong khong khoi dong." -ForegroundColor Yellow
        return
    }
    $targetMode = 'SYSTEM'
    $targetStudentId = 0
    $targetAllowlist = ''
    Write-Host "DA PHE DUYET: Chế độ gửi cho toàn bộ sinh viên hợp lệ." -ForegroundColor Green
} else {
    Write-Host "CHE DO GUI: GIOI HAN KIEM THU AN TOAN" -ForegroundColor Cyan
    Write-Host "  * Nguoi nhan duy nhat: Sinh vien ID 1 (hnghi.dong01@gmail.com)" -ForegroundColor White
    Write-Host "  * Che do:              ALLOWLIST" -ForegroundColor White
    Write-Host "  (De chuyen sang gui tat ca sinh vien sau khi test, chay: .\run-demo-auto-mail.ps1 -AllStudents)" -ForegroundColor DarkGray
}

# 6. Thiet lap bien moi truong
$env:DEMO_DB_USERNAME = $mysqlUser
$env:DEMO_DB_PASSWORD = $mysqlPass
$env:DEMO_DB_HOST = '127.0.0.1'
$env:DEMO_DB_PORT = '3306'
$env:DEMO_PORT = [string]$Port
$env:SPRING_PROFILES_ACTIVE = 'demo-auto'
$env:THONGBAO_SMTP_USERNAME = $smtpSender
$env:THONGBAO_SMTP_PASSWORD = $smtpPassword
$env:THONGBAO_SMTP_FROM = $smtpSender

# 7. Background Monitor de tu dong mo trinh duyet
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
}

# 8. Maven Command & Arguments
$mvnCmd = 'mvn'
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    if (Test-Path 'C:\apache-maven-3.9.16\bin\mvn.cmd') { $mvnCmd = 'C:\apache-maven-3.9.16\bin\mvn.cmd' }
}

$springArgs = @(
    '--spring.profiles.active=demo-auto',
    "--server.port=$Port",
    '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien_demo',
    '--spring.jpa.hibernate.ddl-auto=none',
    '--spring.jpa.generate-ddl=false',
    '--spring.sql.init.mode=never',
    '--app.thongbao.email.outbox-enabled=true',
    '--app.thongbao.email.delivery-enabled=true',
    '--app.thongbao.email.schedule-enabled=true',
    '--app.thongbao.email.poll-ms=3000',
    '--app.thongbao.email.min-interval-ms=1000',
    "--app.thongbao.email.min-notice-id=$minNoticeId",
    "--app.thongbao.email.student-id=$targetStudentId",
    "--app.thongbao.email.mode=$targetMode",
    "--app.thongbao.email.allowlist=$targetAllowlist",
    '--app.thongbao.email.batch-size=1',
    "--app.thongbao.email.base-url=http://localhost:$Port",
    '--app.thongbao.email.smtp.host=smtp.gmail.com',
    '--app.thongbao.email.smtp.port=587',
    "--app.thongbao.email.smtp.username=$smtpSender",
    "--app.thongbao.email.smtp.from=$smtpSender",
    '--app.thongbao.email.smtp.auth=true',
    '--app.thongbao.email.smtp.starttls=true',
    '--app.ketqua.email.enabled=false',
    '--app.ketqua.email.scheduling-enabled=false',
    '--spring.mail.test-connection=false',
    '--spring.mail.host=',
    '--spring.mail.username=',
    '--spring.mail.password='
)
$argLine = ($springArgs -join ' ')

Write-Host "`nDang khoi dong Website voi Worker tu dong gui Gmail (Port: $Port)..." -ForegroundColor Green
Write-Host "Huong dan kiem thu tu dong:" -ForegroundColor Cyan
Write-Host "  1. Dang nhap tai khoan Truong khoa: TK001" -ForegroundColor White
Write-Host "  2. Vao menu 'Thong bao' -> 'Them thong bao moi'" -ForegroundColor White
Write-Host "  3. Bam Luu -> He thong tu dong tao outbox va worker tu gui Gmail trong vong vai giay!" -ForegroundColor White
Write-Host "  4. Khong can chay bat ky script nao khac. Kiem tra hop thu den Gmail va trang Giam sat Email." -ForegroundColor White
Write-Host "`nNhan Ctrl+C de dung ung dung.`n" -ForegroundColor Yellow

Push-Location $projectDirectory
try {
    & $mvnCmd spring-boot:run "-Dspring-boot.run.profiles=demo-auto" "-Dspring-boot.run.arguments=$argLine"
} finally {
    Pop-Location
    $env:DEMO_DB_PASSWORD = $null
    $env:THONGBAO_SMTP_PASSWORD = $null
}
