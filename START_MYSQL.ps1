# =========================================
# SKILORA - MySQL Server Startup Script
# PowerShell version for Windows
# =========================================

# Run as Administrator
#Requires -RunAsAdministrator

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   SKILORA MySQL Server Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check if XAMPP MySQL is installed
if (Test-Path "C:\xampp\mysql\bin\mysqld.exe") {
    Write-Host "[1] Starting XAMPP MySQL..." -ForegroundColor Yellow
    Write-Host ""
    & "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --console
    Exit
}

# Check if MySQL80 service exists
$mysql_service = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue

if ($mysql_service) {
    Write-Host "[2] Starting MySQL80 service..." -ForegroundColor Yellow
    Write-Host ""
    
    try {
        Start-Service -Name "MySQL80" -ErrorAction Stop
        Start-Sleep -Seconds 2
        
        Write-Host ""
        Write-Host "======================================" -ForegroundColor Green
        Write-Host "   MySQL Service Started!" -ForegroundColor Green
        Write-Host "======================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Connection String: localhost:3306" -ForegroundColor Cyan
        Write-Host "Username: root" -ForegroundColor Cyan
        Write-Host "Password: (empty by default)" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "You can now run the Skilora application." -ForegroundColor Green
        Write-Host ""
        
        Read-Host "Press Enter to exit"
    } catch {
        Write-Host ""
        Write-Host "ERROR: Failed to start MySQL service" -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
        Write-Host ""
        Read-Host "Press Enter to exit"
        Exit 1
    }
}

# Check default MySQL installation
if (Test-Path "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe") {
    Write-Host "[3] Starting MySQL Server 8.0..." -ForegroundColor Yellow
    Write-Host ""
    & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --defaults-file="C:\Program Files\MySQL\MySQL Server 8.0\my.ini" --console
    Exit
}

# MySQL not found
Write-Host ""
Write-Host "==========================================" -ForegroundColor Red
Write-Host "   ERROR: MySQL NOT FOUND" -ForegroundColor Red
Write-Host "==========================================" -ForegroundColor Red
Write-Host ""
Write-Host "MySQL is not installed on your system." -ForegroundColor Yellow
Write-Host ""
Write-Host "Please install one of the following:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. XAMPP (includes MySQL)" -ForegroundColor Green
Write-Host "   Download: https://www.apachefriends.org/" -ForegroundColor Green
Write-Host ""
Write-Host "2. MySQL Server standalone" -ForegroundColor Green
Write-Host "   Download: https://dev.mysql.com/downloads/" -ForegroundColor Green
Write-Host ""
Write-Host "3. Use Docker" -ForegroundColor Green
Write-Host "   docker run --name skilora-mysql \" -ForegroundColor Green
Write-Host "     -e MYSQL_ROOT_PASSWORD=password \" -ForegroundColor Green
Write-Host "     -p 3306:3306 \" -ForegroundColor Green
Write-Host "     mysql:8.0" -ForegroundColor Green
Write-Host ""
Write-Host "After installation, run this script again." -ForegroundColor Yellow
Write-Host ""

Read-Host "Press Enter to exit"
Exit 1
