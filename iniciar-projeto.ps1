# Script de Inicialização Automatizada - Projeto Validação de Documentos
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================================" -ForegroundColor DarkYellow
Write-Host "   PROJETO VALIDAÇÃO DE DOCUMENTOS - AVANADE 2026.2   " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor DarkYellow
Write-Host ""

# 1. Banco de Dados Docker
Write-Host "[1/3] Iniciando Banco de Dados PostgreSQL..." -ForegroundColor Cyan
try {
    docker compose up -d
    Write-Host "   -> PostgreSQL ativo na porta 5432." -ForegroundColor Green
} catch {
    Write-Host "   -> [AVISO] Verifique se o Docker Desktop está em execução." -ForegroundColor Yellow
}

Write-Host ""

# 2. Backend Spring Boot
Write-Host "[2/3] Iniciando Backend Spring Boot em nova janela..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Write-Host '=== Backend Spring Boot ===' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run"
Write-Host "   -> Processo do Backend disparado." -ForegroundColor Green

Write-Host ""

# 3. Frontend React / Vite
Write-Host "[3/3] Iniciando Frontend React em nova janela..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Write-Host '=== Frontend React (Vite) ===' -ForegroundColor Yellow; Set-Location frontend; npm run dev"
Write-Host "   -> Processo do Frontend disparado." -ForegroundColor Green

Write-Host ""
Write-Host "========================================================" -ForegroundColor DarkYellow
Write-Host "   Serviços em execução!" -ForegroundColor Green
Write-Host "   * Frontend: http://localhost:3000" -ForegroundColor White
Write-Host "   * Backend:  http://localhost:8080" -ForegroundColor White
Write-Host "   * Swagger:  http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "========================================================" -ForegroundColor DarkYellow
Write-Host ""

# Aguarda breves segundos e abre a aplicação no navegador padrão
Start-Sleep -Seconds 4
Start-Process "http://localhost:3000"

