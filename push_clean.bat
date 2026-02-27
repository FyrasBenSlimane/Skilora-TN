@echo off
REM Push to GitHub with a CLEAN history (no secrets in any commit).
REM This creates a new branch with a single commit containing current files,
REM then force-pushes it as Gestion_Rect so GitHub never sees the old commits.

set BRANCH=Gestion_Rect
set REMOTE=origin
set REPO=https://github.com/FyrasBenSlimane/Skilora-TN.git

echo.
echo [1/5] Making sure .env is not staged...
git reset HEAD python/recruitment_api/.env 2>nul
git reset HEAD .env 2>nul
git checkout -- python/recruitment_api/.env 2>nul
git checkout -- .env 2>nul

echo [2/5] Creating backup branch Gestion_Rect_backup...
git branch -f Gestion_Rect_backup %BRANCH% 2>nul

echo [3/5] Creating orphan branch (no parent = no old history)...
git checkout --orphan temp_clean

echo [4/5] Adding all files (except .env) and committing (single clean commit)...
git reset
git add -A
git rm --cached python/recruitment_api/.env 2>nul
git rm --cached .env 2>nul
git commit -m "Clean push: API et metier avance (no secrets in history)"

echo [5/5] Replacing %BRANCH% with clean history and pushing...
git branch -D %BRANCH% 2>nul
git branch -m %BRANCH%
git remote remove %REMOTE% 2>nul
git remote add %REMOTE% %REPO%
git push %REMOTE% %BRANCH% --force

echo.
echo Done. If push succeeded, your repo now has a single commit with no secrets.
echo Old branch backed up as Gestion_Rect_backup (local only).
pause
