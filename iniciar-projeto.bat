@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
title Validação de Documentos - Inicializador

:: Garante que o diretório atual seja a raiz do projeto
cd /d "%~dp0"

echo ========================================================
echo    PROJETO VALIDAÇÃO DE DOCUMENTOS - AVANADE 2026.2
echo ========================================================
echo.

:: 1. Verificação do Docker / Banco de Dados
where docker >nul 2>&1
if %errorlevel% equ 0 (
    echo [1/3] Docker detectado. Iniciando container PostgreSQL...
    docker compose up -d
    set SPRING_PROFILE=
) else (
    echo [1/3] Docker não encontrado no computador.
    echo       -> Usando banco de dados embutido (H2) automaticamente.
    echo       -> Nenhuma instalação adicional de banco é necessária!
    set SPRING_PROFILE=-Dspring-boot.run.profiles=local
)

echo.
:: 2. Backend Spring Boot
echo [2/3] Iniciando Backend Spring Boot em uma nova janela...
start "Backend Spring Boot (Porta 8080)" cmd /k "cd /d "%~dp0" && echo [BACKEND] Iniciando Spring Boot... && mvnw.cmd spring-boot:run %SPRING_PROFILE%"

echo.
:: 3. Frontend React / Vite
echo [3/3] Iniciando Frontend React em uma nova janela...
start "Frontend React (Porta 3000)" cmd /k "cd /d "%~dp0frontend" && echo [FRONTEND] Iniciando Vite... && npm run dev"

echo.
echo ========================================================
echo    Serviços em inicialização!
echo.
echo    - Frontend Web: http://localhost:3000
echo    - Backend API:  http://localhost:8080
echo    - Swagger Docs: http://localhost:8080/swagger-ui.html
echo ========================================================
echo.
echo Aguardando 6 segundos para os serviços subirem e abrindo o navegador...
timeout /t 6 > nul
start http://localhost:3000
