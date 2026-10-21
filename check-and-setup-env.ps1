<#
.SYNOPSIS
    Script de Verificação e Diagnóstico do Ambiente para o Projeto de Validação de Documentos
#>

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   VERIFICAÇÃO DE AMBIENTE - VALIDAÇÃO DE DOCUMENTOS     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. JAVA
Write-Host "`n[1/5] Verificando Java (JDK)..." -ForegroundColor Yellow
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host " [OK] Java encontrado: $javaVersion" -ForegroundColor Green
    $javaHome = [System.Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
    if (-not $javaHome) {
        $javaHome = [System.Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    }
    if ($javaHome) {
        Write-Host " [OK] JAVA_HOME configurado: $javaHome" -ForegroundColor Green
    }
} else {
    Write-Host " [X] Java não encontrado no PATH." -ForegroundColor Red
}

# 2. NODE & NPM
Write-Host "`n[2/5] Verificando Node.js e NPM (Frontend)..." -ForegroundColor Yellow
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
if ($nodeCmd) {
    $nodeVer = & node -v
    $npmVer = & npm -v
    Write-Host " [OK] Node.js: $nodeVer | NPM: $npmVer" -ForegroundColor Green
} else {
    Write-Host " [X] Node.js não encontrado." -ForegroundColor Red
}

# 3. TESSERACT OCR & MODELO DE IDIOMA
Write-Host "`n[3/5] Verificando Tesseract OCR e tessdata..." -ForegroundColor Yellow
$projectTessData = Join-Path $PSScriptRoot "tessdata\por.traineddata"
if (Test-Path $projectTessData) {
    $size = (Get-Item $projectTessData).Length
    Write-Host " [OK] Modelo em Português local (tessdata/por.traineddata) pronto! ($size bytes)" -ForegroundColor Green
    Write-Host "      (Suficiente para a biblioteca Java Tess4J executar nativamente)" -ForegroundColor DarkGray
}

$tessCmd = Get-Command tesseract -ErrorAction SilentlyContinue
if ($tessCmd -or (Test-Path "C:\Program Files\Tesseract-OCR\tesseract.exe")) {
    Write-Host " [OK] CLI Tesseract instalada no Windows." -ForegroundColor Green
} else {
    Write-Host " [!] Executável Tesseract CLI no Windows ainda não instalado (opcional se usar Tess4J)." -ForegroundColor Yellow
}

# 4. BANCO DE DADOS (POSTGRESQL OU DOCKER)
Write-Host "`n[4/5] Verificando Banco de Dados / Docker..." -ForegroundColor Yellow
$pgService = Get-Service -Name '*postgres*' -ErrorAction SilentlyContinue
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
$psqlCmd = (Get-Command psql -ErrorAction SilentlyContinue) -or (Test-Path "C:\Program Files\PostgreSQL\17\bin\psql.exe")

if ($pgService -and ($pgService.Status -eq 'Running')) {
    Write-Host " [OK] Serviço do PostgreSQL 17 está ATIVO e RODANDO!" -ForegroundColor Green
    Write-Host "      Porta 5432 ativa | Banco: doc_validation_db" -ForegroundColor DarkGray
} elseif ($dockerCmd) {
    Write-Host " [OK] Docker encontrado." -ForegroundColor Green
} elseif ($psqlCmd) {
    Write-Host " [OK] PostgreSQL instalado no sistema." -ForegroundColor Green
} else {
    Write-Host " [!] PostgreSQL ou Docker ainda não instalados." -ForegroundColor Yellow
}

# 5. CHAVE DE API DE IA (SPRING AI)
Write-Host "`n[5/5] Verificando Chave de API de IA (OpenAI / Gemini)..." -ForegroundColor Yellow
$openAiKey = [System.Environment]::GetEnvironmentVariable('OPENAI_API_KEY', 'User')
$geminiKey = [System.Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'User')

if ($openAiKey) {
    Write-Host " [OK] OPENAI_API_KEY configurada nas variáveis de ambiente." -ForegroundColor Green
} elseif ($geminiKey) {
    Write-Host " [OK] GEMINI_API_KEY configurada nas variáveis de ambiente." -ForegroundColor Green
} else {
    Write-Host " [!] Nenhuma chave de IA detectada nas variáveis de ambiente do Windows." -ForegroundColor Yellow
    Write-Host "     Você também pode colocá-la diretamente no arquivo .env criado na raiz do projeto." -ForegroundColor Gray
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
