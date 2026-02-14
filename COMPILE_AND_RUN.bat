@echo off
echo.
echo ========================================
echo  COMPILATION DU MODULE FINANCE
echo ========================================
echo.
echo Compilation en cours...
echo.

cd /d "%~dp0"
call mvn clean compile

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo  COMPILATION REUSSIE !
    echo ========================================
    echo.
    echo Voulez-vous lancer l'application ?
    echo.
    choice /C YN /M "Lancer maintenant (Y/N)"
    if %ERRORLEVEL% EQU 1 (
        echo.
        echo Lancement de l'application...
        call mvn javafx:run
    )
) else (
    echo.
    echo ========================================
    echo  ERREUR DE COMPILATION
    echo ========================================
    echo.
    echo Verifiez les erreurs ci-dessus.
    echo.
)

pause
