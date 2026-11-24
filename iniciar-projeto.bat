@echo off
cd /d "%~dp0"
cls
echo ========================================================
echo    PROJETO VALIDACAO DE DOCUMENTOS - AVANADE 2026.2
echo ========================================================
echo.

:: Cria o .env a partir de .env.example se nao existir
if not exist ".env" (
    if exist ".env.example" (
        echo [INFO] Criando .env local a partir de .env.example...
        copy ".env.example" ".env" > nul
    )
)

:: Verifica dependencias do frontend
if not exist "frontend\node_modules" (
    echo [INFO] Instalando dependencias do Frontend via npm install...
    pushd frontend
    call npm install
    popd
    echo.
)

echo [1/2] Iniciando Backend Spring Boot - H2 Local...
start "Backend Spring Boot" cmd /k "mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local"
echo.
echo [2/2] Iniciando Frontend React...
start "Frontend React" /D "frontend" cmd /k "npm run dev"
echo.
echo ========================================================
echo    Backend:  http://localhost:8080
echo    Frontend: http://localhost:3000
echo ========================================================
echo.
echo Aguarde alguns instantes enquanto os servicos iniciam...
timeout /t 8 > nul 2>&1 || ping -n 9 127.0.0.1 > nul
start http://localhost:3000
