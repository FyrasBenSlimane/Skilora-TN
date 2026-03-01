@echo off
REM Maven wrapper that fixes repository path issue
REM This script removes the problematic -Dmaven.repo.local parameter
REM and replaces it with a path that doesn't have special characters

setlocal enabledelayedexpansion
set MAVEN_REPO=C:/m2-repo
set MAVEN_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3

REM Build new argument list without the problematic repo parameter
set NEW_ARGS=
set FOUND_REPO=0

for %%a in (%*) do (
    set "ARG=%%a"
    echo !ARG! | findstr /B /C:"-Dmaven.repo.local" >nul 2>&1
    if errorlevel 1 (
        set "NEW_ARGS=!NEW_ARGS! %%a"
    ) else (
        set FOUND_REPO=1
    )
)

REM Add the correct repository path
if !FOUND_REPO!==1 (
    set "NEW_ARGS=!NEW_ARGS! -Dmaven.repo.local=%MAVEN_REPO%"
) else (
    REM If not found, add it anyway to be safe
    set "NEW_ARGS=!NEW_ARGS! -Dmaven.repo.local=%MAVEN_REPO%"
)

REM Execute Maven with corrected arguments
"%MAVEN_HOME%\bin\mvn.cmd" %NEW_ARGS%
