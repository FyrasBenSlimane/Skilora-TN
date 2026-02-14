@echo off
echo.
echo ========================================
echo  MODULE FINANCE - LANCEMENT
echo ========================================
echo.
echo Demarrage de l'application...
echo.

cd /d "%~dp0"
mvn javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo  ERREUR DE LANCEMENT
    echo ========================================
    echo.
    echo Verifiez que Maven est installe :
    echo   mvn -version
    echo.
    echo Si Maven n'est pas installe, installez-le depuis :
    echo   https://maven.apache.org/download.cgi
    echo.
    pause
)
