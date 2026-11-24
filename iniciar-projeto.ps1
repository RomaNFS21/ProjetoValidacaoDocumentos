# Script de Inicializacao Automatizada - Projeto Validacao de Documentos
Set-Location $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Yellow
Write-Host "   PROJETO VALIDACAO DE DOCUMENTOS - AVANADE 2026.2     " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Yellow
Write-Host ""

# Garante o .env se nao existir
if (-not (Test-Path "$PSScriptRoot\.env") -and (Test-Path "$PSScriptRoot\.env.example")) {
    Copy-Item "$PSScriptRoot\.env.example" "$PSScriptRoot\.env"
    Write-Host "[INFO] Arquivo .env criado a partir de .env.example." -ForegroundColor Green
}

# Verifica dependencias do frontend
$frontendDir = Join-Path $PSScriptRoot "frontend"
if (-not (Test-Path "$frontendDir\node_modules")) {
    Write-Host "[INFO] Instalando dependencias do Frontend (npm install)..." -ForegroundColor Cyan
    Push-Location $frontendDir
    npm install
    Pop-Location
}

# 1. Backend Spring Boot (H2 Local)
Write-Host "[1/2] Iniciando Backend Spring Boot (H2 Local)..." -ForegroundColor Cyan
Start-Process cmd.exe -WorkingDirectory $PSScriptRoot -ArgumentList '/k', 'mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local'
Write-Host "   -> Backend aberto na porta 8080." -ForegroundColor Green

Write-Host ""

# 2. Frontend React / Vite
Write-Host "[2/2] Iniciando Frontend React..." -ForegroundColor Cyan
Start-Process cmd.exe -WorkingDirectory $frontendDir -ArgumentList '/k', 'npm run dev'
Write-Host "   -> Frontend aberto na porta 3000." -ForegroundColor Green

Write-Host ""
Write-Host "Aguardando inicializacao e abrindo o navegador..." -ForegroundColor Gray
Start-Sleep -Seconds 8
Start-Process "http://localhost:3000"
