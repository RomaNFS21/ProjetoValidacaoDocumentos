@echo off
chcp 65001 > nul
title Validação de Documentos - Encerrar Serviços

echo Encerrando container do PostgreSQL...
docker compose down

echo.
echo Para fechar o Backend e o Frontend, basta fechar as janelas de terminal correspondentes.
echo.
pause

