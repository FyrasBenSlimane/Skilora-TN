@echo off
echo.
echo ========================================
echo  COMPILATION ET LANCEMENT
echo ========================================
echo.

cd /d "%~dp0"

echo [1/2] Compilation...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERREUR DE COMPILATION
    echo.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation réussie !
echo.
echo [2/2] Lancement de l'application...
echo.

call mvn javafx:run

pause
