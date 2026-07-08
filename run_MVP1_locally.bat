@echo off
echo === Starting backend ===
cd 5-backend
start "Backend" mvn spring-boot:run -pl business-service
timeout /t 30 /nobreak >nul

echo === Starting frontend ===
cd ..\4-frontend
if not exist "node_modules" npm install
start "Frontend" npm run dev

echo Open http://localhost:5173 in your browser
