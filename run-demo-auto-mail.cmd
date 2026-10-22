@echo off
setlocal
cd /d "%~dp0"
title HCMUTE - Quan Ly De Tai Sinh Vien (DEMO AUTO MAIL)
echo ==========================================================
echo    KHOI DONG HE THONG QUAN LY DE TAI SINH VIEN HCMUTE
echo         CHE DO TU DONG GUI GMAIL THAT (PORTAL)
echo ==========================================================
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-demo-auto-mail.ps1" %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [THONG BAO] Tien trinh da ket thuc hoac co loi xay ra.
    pause
)
