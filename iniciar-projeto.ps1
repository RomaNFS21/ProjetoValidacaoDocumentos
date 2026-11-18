# Script de Inicialização Automatizada - Projeto Validação de Documentos
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Garante a execução na raiz do projeto
Set-Location $PSScriptRoot

Write-Host "========================================================" -ForegroundColor DarkYellow
Write-Host "   PROJETO VALIDAÇÃO DE DOCUMENTOS - AVANADE 2026.2   " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor DarkYellow
Write-Host ""

    # 1. Verificação do Docker / Banco
    $dockerExists = Get-Command docker -ErrorAction SilentlyContinue
    $springProfile = ""

    if ($dockerExists) {
        Write-Host "[1/3] Docker detectado. Subindo container PostgreSQL..." -ForegroundColor Cyan
        try {
            docker compose up -d
            Write-Host "   -> PostgreSQL ativo na porta 5432." -ForegroundColor Green
        }
        catch {
            Write-Host "   -> Falha ao rodar Docker Compose. Alternando para banco em memória (H2)." -ForegroundColor Yellow
            $springProfile = "-Dspring-boot.run.profiles=local"
        }
    }
    else {
        Write-Host "[1/3] Docker não detectado no sistema." -ForegroundColor Cyan
        Write-Host "   -> Usando banco de dados embutido (H2) em modo PostgreSQL." -ForegroundColor Green
        Write-Host "   -> Nenhuma instalação externa necessária!" -ForegroundColor Gray
        $springProfile = "-Dspring-boot.run.profiles=local"
    }

    Write-Host ""

    # 2. Backend Spring Boot
    Write-Host "[2/3] Iniciando Backend Spring Boot em nova janela..." -ForegroundColor Cyan
    $backendCommand = "Write-Host '=== Backend Spring Boot (Porta 8080) ===' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run $springProfile"
    Start-Process powershell -WorkingDirectory $PSScriptRoot -ArgumentList "-NoExit", "-Command", $backendCommand
    Write-Host "   -> Janela do Backend aberta." -ForegroundColor Green

    Write-Host ""

    # 3. Frontend React / Vite
    Write-Host "[3/3] Iniciando Frontend React em nova janela..." -ForegroundColor Cyan
    $frontendPath = Join-Path $PSScriptRoot "frontend"
    $frontendCommand = "Write-Host '=== Frontend React (Porta 3000) ===' -ForegroundColor Yellow; npm run dev"
    Start-Process powershell -WorkingDirectory $frontendPath -ArgumentList "-NoExit", "-Command", $frontendCommand
    Write-Host "   -> Janela do Frontend aberta." -ForegroundColor Green

    Write-Host ""
    Write-Host "========================================================" -ForegroundColor DarkYellow
    Write-Host "   Serviços em inicialização!" -ForegroundColor Green
    Write-Host "   * Frontend Web: http://localhost:3000" -ForegroundColor White
    Write-Host "   * Backend API:  http://localhost:8080" -ForegroundColor White
    Write-Host "   * Swagger Docs: http://localhost:8080/swagger-ui.html" -ForegroundColor White
    Write-Host "========================================================" -ForegroundColor DarkYellow
    Write-Host ""

    # Aguarda inicialização e abre o navegador
    Start-Sleep -Seconds 6
    Start-Process "http://localhost:3000"
