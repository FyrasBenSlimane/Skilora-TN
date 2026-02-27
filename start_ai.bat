@echo off
echo ========================================
echo Skilora AI Recruitment API
echo ========================================
echo.

echo [INFO] Starting API...
echo [INFO] API will be available at: http://localhost:8000
echo [INFO] Documentation: http://localhost:8000/docs
echo.
echo Press Ctrl+C to stop the API
echo.

cd python\recruitment_api
pip install -r requirements.txt
python main.py

echo.
echo ========================================
echo Skilora AI Recruitment API has stopped.
echo ========================================
pause

