@echo off
chcp 65001 > nul
title Validação de Documentos - Inicializador

echo ========================================================
echo    PROJETO VALIDAÇÃO DE DOCUMENTOS - AVANADE 2026.2
echo ========================================================
echo.

echo [1/3] Iniciando Banco de Dados PostgreSQL via Docker...
docker compose up -d
if %errorlevel% neq 0 (
    echo [AVISO] Falha ao iniciar o Docker. Verifique se o Docker Desktop está aberto.
)

echo.
echo [2/3] Iniciando Backend Spring Boot em uma nova janela...
start "Backend Spring Boot" cmd /k "mvnw.cmd spring-boot:run"

echo.
echo [3/3] Iniciando Frontend React em uma nova janela...
start "Frontend Vite React" cmd /k "cd frontend && npm run dev"

echo.
echo ========================================================
echo    Serviços em inicialização!
echo.
echo    - Frontend: http://localhost:3000
echo    - Backend:  http://localhost:8080
echo    - Swagger:  http://localhost:8080/swagger-ui.html
echo ========================================================
echo.
echo Aguardando inicialização para abrir o navegador...
timeout /t 5 > nul
start http://localhost:3000

