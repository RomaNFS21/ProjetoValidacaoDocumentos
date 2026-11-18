# Script de Inicializacao Automatizada - Projeto Validacao de Documentos
Set-Location $PSScriptRoot

Write-Host "========================================================" -ForegroundColor Yellow
Write-Host "   PROJETO VALIDACAO DE DOCUMENTOS - AVANADE 2026.2     " -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Yellow
Write-Host ""

# 1. Backend Spring Boot (H2 Local)
Write-Host "[1/2] Iniciando Backend Spring Boot (H2 Local)..." -ForegroundColor Cyan
Start-Process cmd.exe -ArgumentList '/k', "cd /d `"$PSScriptRoot`" && mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local"
Write-Host "   -> Backend aberto na porta 8080." -ForegroundColor Green

Write-Host ""

# 2. Frontend React / Vite
Write-Host "[2/2] Iniciando Frontend React..." -ForegroundColor Cyan
$frontendDir = Join-Path $PSScriptRoot "frontend"
Start-Process cmd.exe -ArgumentList '/k', "cd /d `"$frontendDir`" && npm run dev"
Write-Host "   -> Frontend aberto na porta 3000." -ForegroundColor Green

Write-Host ""
Write-Host "Aguardando inicializacao e abrindo o navegador..." -ForegroundColor Gray
Start-Sleep -Seconds 5
Start-Process "http://localhost:3000"
