$pgBin = "C:\Program Files\PostgreSQL\17\bin"
$currUserPath = [System.Environment]::GetEnvironmentVariable("Path", "User")
if ($currUserPath -notlike "*$pgBin*") {
    [System.Environment]::SetEnvironmentVariable("Path", "$pgBin;$currUserPath", "User")
    Write-Host "PostgreSQL adicionado ao PATH do Usuário." -ForegroundColor Green
}

# Criar banco de dados se não existir
$env:PGPASSWORD = "postgres"
$createdb = Join-Path $pgBin "createdb.exe"
if (Test-Path $createdb) {
    try {
        & $createdb -h localhost -p 5432 -U postgres doc_validation_db 2>&1 | Out-Null
        Write-Host "Banco 'doc_validation_db' criado com sucesso!" -ForegroundColor Green
    } catch {
        Write-Host "Banco já existe ou aviso: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}
