Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  STARTING HUST ONLINE EXAM C TCP SOCKET SERVER" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

Set-Location server_c

if (-not (Test-Path "server.exe")) {
    Write-Host "Compiling C source files with GCC..." -ForegroundColor Yellow
    gcc -O2 -Wall -Ilib -I. server.c codec.c db.c session.c room.c dispatcher.c sha256.c lib/cJSON.c lib/sqlite3.c -o server.exe -lws2_32
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
        Set-Location ..
        exit $LASTEXITCODE
    }
}

Write-Host "Starting C Server on Port 8888..." -ForegroundColor Green
.\server.exe 8888 ../exam_system.db
Set-Location ..
