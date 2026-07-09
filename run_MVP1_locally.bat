@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

echo ============================================
echo   Gimme Vetting Solution - Local MVP (Clean)
echo   This will clean, build, and start services.
echo   Estimated time: 1-4 minutes.
echo ============================================
echo.

:: Step 1: Clean
echo === Step 1: Cleaning build artifacts ===

:: Kill stale backend process on port 8082
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":8082.*LISTENING"') do (
    echo WARNING: Killing stale process on port 8082 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

:: Kill stale frontend process on port 5173
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":5173.*LISTENING"') do (
    echo WARNING: Killing stale process on port 5173 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

cd /d "%SCRIPT_DIR%\5-backend"
call mvn clean -q 2>nul || echo WARNING: mvn clean returned an error, continuing anyway.
cd /d "%SCRIPT_DIR%\4-frontend"
if exist dist rmdir /S /Q dist 2>nul
if exist node_modules\.vite rmdir /S /Q node_modules\.vite 2>nul
echo Cleanup complete.
echo.

:: Step 2: Backend Build
echo === Step 2: Building backend ===
echo (may take 1-3 minutes)...
cd /d "%SCRIPT_DIR%\5-backend"
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Backend build failed. Aborting.
    goto :end
)
echo Backend build complete.
echo.

:: Step 3: Frontend Build
echo === Step 3: Building frontend ===
echo (may take 30-60 seconds)...
cd /d "%SCRIPT_DIR%\4-frontend"
call npm run build
if errorlevel 1 (
    echo.
    echo WARNING: Frontend build failed. Backend will still start.
    echo Starting backend without frontend validation.
    echo.
) else (
    echo Frontend build complete.
)
echo.

:: Step 4: Start Backend
echo === Step 4: Starting backend ===
cd /d "%SCRIPT_DIR%\5-backend"
start "Backend" cmd /c "mvn spring-boot:run -pl business-service"

echo Waiting for backend to start...
set /a max_wait=60
set /a wait_count=0
:wait_loop
if !wait_count! geq !max_wait! (
    echo ERROR: Backend did not respond after 60 seconds. Aborting.
    goto :end
)
curl -s -o nul http://localhost:8082/api/v1/intake >nul 2>&1
if !errorlevel! equ 0 (
    echo Backend is ready.
    goto :start_frontend
)
timeout /t 1 /nobreak >nul
set /a wait_count+=1
goto :wait_loop

:start_frontend
:: Step 5: Start Frontend
echo.
echo === Step 5: Starting frontend ===
cd /d "%SCRIPT_DIR%\4-frontend"
start "Frontend" cmd /c "npm run dev"
echo Frontend started.
echo.

echo ============================================
echo   Open http://localhost:5173 in your browser
echo   Close this window to stop both services
echo ============================================

goto :end

:end
echo.
pause
