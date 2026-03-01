@echo off
:: This script must be run as Administrator.
:: It opens TCP port 8080 in Windows Firewall so mobile phones on the same
:: WiFi network can reach the Skilora certificate verification server.

echo ========================================================
echo  Skilora - Opening Firewall Port 8080
echo ========================================================
echo.

netsh advfirewall firewall delete rule name="Skilora Certificate Verification (8080)" >nul 2>&1

netsh advfirewall firewall add rule ^
  name="Skilora Certificate Verification (8080)" ^
  dir=in ^
  action=allow ^
  protocol=TCP ^
  localport=8080

if %ERRORLEVEL% == 0 (
    echo.
    echo  SUCCESS: Port 8080 is now open.
    echo  Your phone can now scan QR codes and verify certificates.
) else (
    echo.
    echo  ERROR: Could not open port. Make sure you ran this as Administrator.
    echo  Right-click the file and select "Run as administrator".
)

echo.
echo ========================================================
echo  Your local IP addresses:
ipconfig | findstr /i "IPv4"
echo ========================================================
echo.
pause
