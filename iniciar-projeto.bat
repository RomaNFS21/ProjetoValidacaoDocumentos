@echo off
cd /d "%~dp0"
cls
echo ========================================================
echo    PROJETO VALIDACAO DE DOCUMENTOS - AVANADE 2026.2
echo ========================================================
echo.
echo [1/2] Iniciando Backend Spring Boot com banco H2 embutido...
start "Backend Spring Boot" /D "%~dp0" cmd /k "mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local"
echo.
echo [2/2] Iniciando Frontend React...
start "Frontend React" /D "%~dp0frontend" cmd /k "npm run dev"
echo.
echo ========================================================
echo    Backend:  http://localhost:8080
echo    Frontend: http://localhost:3000
echo ========================================================
echo.
echo Aguarde alguns instantes enquanto os servicos iniciam...
timeout /t 5 > nul
start http://localhost:3000
