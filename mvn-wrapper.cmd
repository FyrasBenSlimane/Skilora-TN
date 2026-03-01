@echo off
setlocal enabledelayedexpansion

REM Override Maven repository path to avoid special character issues
set MAVEN_REPO=C:/m2-repo

REM Get all arguments passed to this script
set ARGS=
for %%a in (%*) do (
    set ARG=%%a
    REM Remove any existing maven.repo.local parameter
    echo !ARG! | findstr /C:"-Dmaven.repo.local" >nul
    if errorlevel 1 (
        set ARGS=!ARGS! %%a
    )
)

REM Add our custom repository path
set ARGS=%ARGS% -Dmaven.repo.local=%MAVEN_REPO%

REM Call the actual Maven command
"C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd" %ARGS%
