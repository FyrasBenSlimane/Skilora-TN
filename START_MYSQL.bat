@echo off
REM =========================================
REM SKILORA - MySQL Server Startup Script
REM =========================================
REM This script helps start MySQL for the Skilora application

echo.
echo ======================================
echo   SKILORA MySQL Server Startup
echo ======================================
echo.

REM Check if XAMPP is installed
if exist "C:\xampp\mysql\bin\mysqld.exe" (
    echo [1] Starting XAMPP MySQL...
    echo.
    "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --console
    goto :end
)

REM Check if MySQL service is installed
sc query MySQL80 > nul 2>&1
if %errorlevel% == 0 (
    echo [2] Starting MySQL80 service...
    echo.
    net start MySQL80
    if %errorlevel% == 0 (
        echo.
        echo =======================================
        echo   MySQL Service Started Successfully!
        echo =======================================
        echo.
        echo Connection String: localhost:3306
        echo Username: root
        echo Password: (empty by default)
        echo.
        echo You can now run the Skilora application.
        echo Press any key to exit...
        pause
    ) else (
        echo.
        echo ERROR: Failed to start MySQL service
        echo Please check the error message above
        echo.
    )
    goto :end
)

REM Check if MySQL is installed in default location
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" (
    echo [3] Starting MySQL Server 8.0...
    echo.
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --defaults-file="C:\Program Files\MySQL\MySQL Server 8.0\my.ini" --console
    goto :end
)

REM No MySQL found
echo.
echo =========================================
echo   ERROR: MySQL NOT FOUND
echo =========================================
echo.
echo MySQL is not installed on your system.
echo.
echo Please install one of the following:
echo   1. XAMPP (includes MySQL)
echo      Download from: https://www.apachefriends.org/
echo.
echo   2. MySQL Server standalone
echo      Download from: https://dev.mysql.com/downloads/
echo.
echo   3. Use Docker
echo      docker run --name skilora-mysql ^
echo        -e MYSQL_ROOT_PASSWORD=password ^
echo        -p 3306:3306 ^
echo        mysql:8.0
echo.
echo Press any key to exit...
pause
goto :end

:end
echo.
echo Exiting...
exit /b
