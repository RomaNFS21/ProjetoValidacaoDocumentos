# Script de instalacao do PostgreSQL 17
Write-Host "Iniciando instalacao do PostgreSQL 17 via Winget..." -ForegroundColor Cyan
Write-Host "Se uma janela do Windows (Controle de Conta de Usuario - UAC) solicitar permissao, clique em 'SIM'." -ForegroundColor Magenta

$args = "install PostgreSQL.PostgreSQL.17 --override `"--mode unattended --superpassword postgres --servicepassword postgres --serverport 5432`" --accept-package-agreements --accept-source-agreements"
Write-Host "Comando: winget $args"

Start-Process -FilePath "winget" -ArgumentList $args -Wait -NoNewWindow
Write-Host "Instalação finalizada ou aguardando conclusão do serviço..." -ForegroundColor Green
