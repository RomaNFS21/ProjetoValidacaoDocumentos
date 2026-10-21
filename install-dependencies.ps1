param(
    [switch]$SkipPostgres = $false
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   INSTALADOR DE AMBIENTE - PROJETO VALIDAÇÃO DOCUMENTOS " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# -------------------------------------------------------------
# 1. DOWNLOAD DO MODELO DE PORTUGUÊS DO TESSERACT (por.traineddata)
# -------------------------------------------------------------
Write-Host "`n[Passo 1/4] Configurando tessdata e baixando por.traineddata..." -ForegroundColor Yellow
$projectDir = $PSScriptRoot
$tessdataDir = Join-Path $projectDir "tessdata"

if (-not (Test-Path $tessdataDir)) {
    New-Item -ItemType Directory -Path $tessdataDir -Force | Out-Null
}

$porFile = Join-Path $tessdataDir "por.traineddata"
if (-not (Test-Path $porFile)) {
    Write-Host "Baixando modelo em Português de tesseract-ocr/tessdata_fast..." -ForegroundColor Cyan
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $webClient = New-Object System.Net.WebClient
        $webClient.DownloadFile("https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/por.traineddata", $porFile)
        $size = (Get-Item $porFile).Length
        Write-Host " [OK] por.traineddata baixado com sucesso! ($size bytes)" -ForegroundColor Green
    } catch {
        Write-Host " [!] Falha no download automático: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host " [OK] por.traineddata já existe em $tessdataDir" -ForegroundColor Green
}

# Configura variável TESSDATA_PREFIX para o projeto
[System.Environment]::SetEnvironmentVariable("TESSDATA_PREFIX", $tessdataDir, "User")
Write-Host " [OK] TESSDATA_PREFIX configurado para: $tessdataDir" -ForegroundColor Green

# -------------------------------------------------------------
# 2. INSTALAÇÃO DO TESSERACT OCR NO WINDOWS
# -------------------------------------------------------------
Write-Host "`n[Passo 2/4] Verificando instalador do Tesseract OCR..." -ForegroundColor Yellow
$tessInstalled = (Get-Command tesseract -ErrorAction SilentlyContinue) -or (Test-Path "C:\Program Files\Tesseract-OCR\tesseract.exe")

if (-not $tessInstalled) {
    Write-Host "Iniciando a instalação do Tesseract OCR pelo Winget..." -ForegroundColor Cyan
    Write-Host "Se o Windows solicitar permissão de Administrador (UAC), clique em 'SIM'." -ForegroundColor Magenta
    
    $wingetArgs = "install UB-Mannheim.TesseractOCR --accept-package-agreements --accept-source-agreements"
    Start-Process -FilePath "winget" -ArgumentList $wingetArgs -Wait -NoNewWindow
    
    # Atualiza PATH com o binário se instalado
    $tessBin = "C:\Program Files\Tesseract-OCR"
    if (Test-Path "$tessBin\tesseract.exe") {
        $currUserPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
        if ($currUserPath -notlike "*$tessBin*") {
            [System.Environment]::SetEnvironmentVariable("Path", "$tessBin;$currUserPath", "User")
        }
        # Copia também o por.traineddata para a pasta oficial caso não exista lá
        $officialTessdata = "$tessBin\tessdata\por.traineddata"
        if ((Test-Path $porFile) -and (-not (Test-Path $officialTessdata))) {
            try {
                Copy-Item -Path $porFile -Destination $officialTessdata -Force -ErrorAction SilentlyContinue
            } catch {}
        }
        Write-Host " [OK] Tesseract OCR instalado com sucesso!" -ForegroundColor Green
    } else {
        Write-Host " [!] Instalação pendente de confirmação pelo usuário." -ForegroundColor Yellow
    }
} else {
    Write-Host " [OK] Tesseract OCR já está instalado no sistema." -ForegroundColor Green
}

# -------------------------------------------------------------
# 3. BANCO DE DADOS (POSTGRESQL)
# -------------------------------------------------------------
Write-Host "`n[Passo 3/4] Verificando PostgreSQL..." -ForegroundColor Yellow
$pgInstalled = (Get-Command psql -ErrorAction SilentlyContinue) -or (Get-Service -Name '*postgres*' -ErrorAction SilentlyContinue)

if (-not $pgInstalled -and -not $SkipPostgres) {
    Write-Host "Deseja instalar o PostgreSQL 17 agora via Winget?" -ForegroundColor Cyan
    Write-Host "Comando: winget install PostgreSQL.PostgreSQL.17" -ForegroundColor White
    Write-Host "(Nota: Para desenvolvimento, o projeto também roda automaticamente com perfil H2 em memória sem precisar de instalação)." -ForegroundColor Gray
} elseif ($pgInstalled) {
    Write-Host " [OK] PostgreSQL detectado no sistema." -ForegroundColor Green
}

# -------------------------------------------------------------
# 4. CRIANDO ARQUIVO DE VARIÁVEIS LOCAL (.env)
# -------------------------------------------------------------
Write-Host "`n[Passo 4/4] Gerando arquivo .env local..." -ForegroundColor Yellow
$envFile = Join-Path $projectDir ".env"
$envExample = Join-Path $projectDir ".env.example"

if (-not (Test-Path $envFile) -and (Test-Path $envExample)) {
    Copy-Item -Path $envExample -Destination $envFile
    Write-Host " [OK] Arquivo .env criado a partir de .env.example." -ForegroundColor Green
    Write-Host "     Edite o arquivo .env para adicionar sua chave de IA (OPENAI_API_KEY ou GEMINI_API_KEY)." -ForegroundColor Cyan
} else {
    Write-Host " [OK] Arquivo .env já existe." -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   Instalação e configuração de passos finalizados!      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
